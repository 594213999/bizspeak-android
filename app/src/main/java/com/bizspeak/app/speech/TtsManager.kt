package com.bizspeak.app.speech

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * 文本转语音封装，用于朗读 AI 导师的回复。
 * 默认美式英语；可设置语速。朗读前会先去掉 [CORRECTION]/[VOCAB] 标记行，只念正文。
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    var enabled = true
    var speed = 1.0f
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.setSpeechRate(speed)
                }
                ready = true
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
    }

    fun setSpeed(rate: Float) {
        speed = rate
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String) {
        if (!enabled || !ready) return
        val body = text.lines()
            .filter { !it.trim().startsWith("[CORRECTION]") && !it.trim().startsWith("[VOCAB]") }
            .joinToString("\n")
            .trim()
        if (body.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(body, TextToSpeech.QUEUE_FLUSH, null, "bizspeak_${System.nanoTime()}")
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(body, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun stop() = tts?.stop()

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
