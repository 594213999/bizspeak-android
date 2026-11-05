package com.bizspeak.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * 对话与纠错的业务逻辑层。
 * - 持有 Ollama 客户端与设置存储
 * - 维护内存中的最近对话历史（用于上下文）
 * - 发送用户消息并以流式方式更新 UI，最后解析 [CORRECTION]/[VOCAB] 标记
 */
class TutorRepository(private val context: Context) {

    private val settingsStore = SettingsStore(context)
    private val ollama = OllamaClient()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val settings: Flow<AppSettings> = settingsStore.settings

    private val history = mutableListOf<ChatTurn>()

    suspend fun currentSettings(): AppSettings = settingsStore.current()

    suspend fun saveSettings(s: AppSettings) = settingsStore.save(s)

    fun clear() {
        history.clear()
        _messages.value = emptyList()
    }

    suspend fun send(userText: String) {
        val s = settingsStore.current()
        val system = PromptBuilder.buildSystemPrompt(s.level, s.scenario, s.accent)

        val assistantId = UUID.randomUUID().toString()
        val userMsg = ChatMessage(UUID.randomUUID().toString(), "user", userText)
        _messages.value = _messages.value + userMsg + ChatMessage(assistantId, "assistant", "…")

        val full = try {
            ollama.streamChat(
                baseUrl = s.ollamaUrl,
                model = s.ollamaModel,
                system = system,
                history = history.toList(),
                userText = userText,
            ) { chunk ->
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantId) {
                        if (msg.text == "…") msg.copy(text = chunk) else msg.copy(text = msg.text + chunk)
                    } else {
                        msg
                    }
                }
            }
        } catch (e: Exception) {
            _messages.value = _messages.value.map {
                if (it.id == assistantId) {
                    it.copy(text = "⚠️ 连接失败：${e.message}\n请确认：①Ollama 已启动并拉取模型；②手机/模拟器能访问该地址（真机用电脑局域网 IP，模拟器用 10.0.2.2）。")
                } else {
                    it
                }
            }
            return
        }

        val (correction, vocab) = parseTags(full)
        val cleanText = stripTags(full)
        _messages.value = _messages.value.map {
            if (it.id == assistantId) it.copy(text = cleanText, correction = correction, vocabulary = vocab) else it
        }

        history.add(ChatTurn("user", userText))
        history.add(ChatTurn("assistant", cleanText))
        while (history.size > 20) history.removeAt(0)
    }

    private fun parseTags(full: String): Pair<String?, String?> {
        var correction: String? = null
        var vocab: String? = null
        // 兼容三种写法：整行标记、行内标记（AI 常把标记混进正文同一行）
        val regex = Regex("""\[CORRECTION]""")
        val vocabRegex = Regex("""\[VOCAB]""")
        for (raw in full.lines()) {
            val line = raw.trim()
            when {
                regex.containsMatchIn(line) -> {
                    val v = line.substringAfter("[CORRECTION]").trim()
                    if (v.isNotBlank()) correction = v
                }
                vocabRegex.containsMatchIn(line) -> {
                    val v = line.substringAfter("[VOCAB]").trim()
                    if (v.isNotBlank()) vocab = v
                }
            }
        }
        return correction to vocab
    }

    /** 从正文中剔除 [CORRECTION]/[VOCAB] 标记及其后内容，只保留对话正文 */
    private fun stripTags(full: String): String {
        val cutIdx = listOf(
            full.indexOf("[CORRECTION]"),
            full.indexOf("[VOCAB]"),
        ).filter { it >= 0 }.minOrNull() ?: return full.trim()
        return full.substring(0, cutIdx).trim()
    }
}
