package com.bizspeak.app.speech

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/**
 * 离线语音识别（Vosk）。完全本地运行，不依赖 Google 云端服务——
 * 系统自带的 SpeechRecognizer 在模拟器/国内网络下会因连不上 Google 而报网络错误。
 *
 * 模型随 APK 放在 assets 中，首次启动时解压到 filesDir（Vosk 需要真实文件路径）。
 */
class SpeechManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private var listening = false

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial.asStateFlow()

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    /** Vosk 模型就绪后即可用；模型解压失败才算不可用。 */
    val isAvailable: Boolean get() = modelReady || !modelMissing()

    private var modelReady = false

    private fun modelMissing(): Boolean =
        !File(context.filesDir, MODEL_DIR).exists() && !assetExists()

    private fun assetExists(): Boolean = try {
        context.assets.list(MODEL_DIR)?.isNotEmpty() == true
    } catch (_: Exception) {
        false
    }

    /** 首次使用前调用：优先加载外部大模型，否则解压 assets 小模型。 */
    private suspend fun ensureModel(): Model? = withContext(Dispatchers.IO) {
        model?.let { return@withContext it }
        // 1) 外部大模型：扫描 App 外部专属目录下所有合法 Vosk 模型目录，
        //    优先选模型名在优先级列表中的（gigaspeech 精度最高），否则取任意一个。
        val extRoot = context.getExternalFilesDir(null)
        val candidates = extRoot?.listFiles()
            ?.filter { it.isDirectory && File(it, "am/final.mdl").exists() }
            .orEmpty()
            .sortedByDescending { dir -> PREFERRED_MODELS.indexOfFirst { dir.name.startsWith(it) } }
        val bigModelDir = candidates.firstOrNull()

        val target: File = if (bigModelDir != null) {
            android.util.Log.i("BizSpeakSpeech", "使用外部大模型: ${bigModelDir.absolutePath}")
            bigModelDir
        } else {
            // 2) 内置小模型：assets 解压到 filesDir
            val small = File(context.filesDir, MODEL_DIR)
            if (!small.exists()) copyAssets(MODEL_DIR, small)
            small
        }
        val m = Model(target.absolutePath)
        model = m
        modelReady = true
        m
    }

    private fun copyAssets(assetPath: String, destDir: File) {
        val children = context.assets.list(assetPath) ?: return
        if (children.isEmpty()) {
            // 叶子节点：是文件
            destDir.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                destDir.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        destDir.mkdirs()
        for (child in children) {
            copyAssets("$assetPath/$child", File(destDir, child))
        }
    }

    fun start(onFinal: (String) -> Unit, onError: (String) -> Unit) {
        if (listening) return
        listening = true
        _partial.value = ""
        _state.value = SpeechState.Loading

        recordJob = scope.launch {
            try {
                val m = ensureModel()
                if (m == null) {
                    fail("语音模型加载失败", onError)
                    return@launch
                }
                val rec = Recognizer(m, SAMPLE_RATE.toFloat())
                recognizer = rec

                val minBuf = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                val bufSize = maxOf(minBuf, 4096)
                val ar = AudioRecord(
                    // MIC 比 VOICE_RECOGNITION 更通用：部分模拟器/设备上后者会静音
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize,
                )
                if (ar.state != AudioRecord.STATE_INITIALIZED) {
                    ar.release()
                    fail("无法初始化麦克风（可能缺少录音权限）", onError)
                    return@launch
                }
                audioRecord = ar
                ar.startRecording()
                _state.value = SpeechState.Listening

                val buffer = ShortArray(bufSize)
                val sb = StringBuilder()
                var chunks = 0
                var maxAmp = 0
                while (listening) {
                    val n = ar.read(buffer, 0, buffer.size)
                    if (n <= 0) continue
                    // 诊断：统计采样块数与峰值振幅，用于判断麦克风是否采到声音
                    chunks++
                    for (i in 0 until n) {
                        val a = kotlin.math.abs(buffer[i].toInt())
                        if (a > maxAmp) maxAmp = a
                    }
                    if (chunks % 50 == 0) {
                        android.util.Log.i("BizSpeakSpeech", "chunks=$chunks maxAmp=$maxAmp partial=${_partial.value}")
                    }
                    if (rec.acceptWaveForm(buffer, n)) {
                        val text = parseText(rec.result, "text")
                        android.util.Log.i("BizSpeakSpeech", "final-chunk text=$text")
                        if (text.isNotBlank()) {
                            sb.append(text).append(' ')
                            _partial.value = sb.toString().trim()
                        }
                    } else {
                        val partial = parseText(rec.partialResult, "partial")
                        if (partial.isNotBlank()) _partial.value = (sb.toString() + partial).trim()
                    }
                }
                android.util.Log.i("BizSpeakSpeech", "loop-end chunks=$chunks maxAmp=$maxAmp")

                // 停止后取最后一段
                val tail = parseText(rec.finalResult, "text")
                val full = (sb.toString() + " " + tail).trim().replace(Regex("\\s+"), " ")
                _state.value = SpeechState.Idle
                onFinal(full)
            } catch (e: Exception) {
                fail(e.message ?: "语音识别失败", onError)
            } finally {
                stopInternal()
            }
        }
    }

    private fun fail(msg: String, onError: (String) -> Unit) {
        listening = false
        _state.value = SpeechState.Error(msg)
        onError(msg)
    }

    private fun parseText(json: String?, key: String): String = try {
        if (json.isNullOrBlank()) "" else JSONObject(json).optString(key, "")
    } catch (_: Exception) {
        ""
    }

    fun stop() {
        if (!listening) return
        listening = false
        _state.value = SpeechState.Processing
    }

    private fun stopInternal() {
        listening = false
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { recognizer?.close() }
        recognizer = null
    }

    fun destroy() {
        stopInternal()
        runCatching { model?.close() }
        model = null
        scope.cancel()
    }

    private companion object {
        const val MODEL_DIR = "vosk-model-en-us-0.22-lgraph"
        const val SAMPLE_RATE = 16000

        /** 外部模型优先级：越靠前越优先（gigaspeech 精度最高） */
        val PREFERRED_MODELS = listOf(
            "vosk-model-en-us-0.42-gigaspeech",
            "vosk-model-en-us-0.22",
        )
    }
}

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Loading : SpeechState
    data object Listening : SpeechState
    data object Processing : SpeechState
    data class Error(val message: String) : SpeechState
}
