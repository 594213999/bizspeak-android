package com.bizspeak.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 与本地 Ollama 的 /api/chat 端点通信，逐块（流式）返回内容。
 * Ollama 的流式响应是 NDJSON：每一行一个 JSON 对象，字段 message.content 为增量文本。
 */
data class ChatTurn(val role: String, val content: String)

class OllamaClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        // 本地 CPU 推理首个 token 可能要 30~60 秒，默认 10 秒必然超时
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // 流式响应：不限制读超时
        .callTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
) {

    suspend fun streamChat(
        baseUrl: String,
        model: String,
        system: String,
        history: List<ChatTurn>,
        userText: String,
        onChunk: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + "/api/chat"

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", system))
        for (m in history) {
            messages.put(JSONObject().put("role", m.role).put("content", m.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userText))

        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("stream", true)
            // Qwen3.5 默认开启思考模式：CPU 上会把 40+ 秒耗在 thinking 上，
            // 且正文 content 为空。关掉后响应从 ~47s 降到 <1s。
            .put("think", false)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder().url(url).post(body).build()

        val sb = StringBuilder()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw IllegalStateException("Ollama 请求失败 (${response.code}): $err")
            }
            val source = response.body!!.source()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                try {
                    val obj = JSONObject(line)
                    val content = obj.optJSONObject("message")?.optString("content") ?: ""
                    if (content.isNotEmpty()) {
                        sb.append(content)
                        onChunk(content)
                    }
                    if (obj.optBoolean("done", false)) break
                } catch (_: Exception) {
                    // 跳过无法解析的行（如心跳/注释行）
                }
            }
        }
        sb.toString()
    }
}
