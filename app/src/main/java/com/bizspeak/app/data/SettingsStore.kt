package com.bizspeak.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bizspeak_settings")

object SettingsKeys {
    val OLLAMA_URL = stringPreferencesKey("ollama_url")
    val OLLAMA_MODEL = stringPreferencesKey("ollama_model")
    val ACCENT = stringPreferencesKey("accent")
    val LEVEL = stringPreferencesKey("level")
    val SCENARIO = stringPreferencesKey("scenario")
    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val VOICE_SPEED = floatPreferencesKey("voice_speed")
}

/**
 * 应用设置。默认值面向"模拟器 + 本机 Ollama"：
 * 模拟器访问宿主机用 10.0.2.2，真机需改成电脑在局域网中的 IP。
 */
data class AppSettings(
    val ollamaUrl: String = "http://192.168.3.112:11434",
    val ollamaModel: String = "qwen-biz:latest",
    val accent: String = "American",
    val level: Level = Level.ELEMENTARY,
    val scenario: Scenario = Scenario.PHONE,
    val ttsEnabled: Boolean = true,
    val voiceSpeed: Float = 1.0f,
)

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            ollamaUrl = prefs[SettingsKeys.OLLAMA_URL] ?: "http://192.168.3.112:11434",
            ollamaModel = prefs[SettingsKeys.OLLAMA_MODEL] ?: "qwen-biz:latest",
            accent = prefs[SettingsKeys.ACCENT] ?: "American",
            level = Level.fromKey(prefs[SettingsKeys.LEVEL] ?: Level.ELEMENTARY.key),
            scenario = Scenario.fromKey(prefs[SettingsKeys.SCENARIO] ?: Scenario.PHONE.key),
            ttsEnabled = prefs[SettingsKeys.TTS_ENABLED] ?: true,
            voiceSpeed = prefs[SettingsKeys.VOICE_SPEED] ?: 1.0f,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun save(s: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.OLLAMA_URL] = s.ollamaUrl
            prefs[SettingsKeys.OLLAMA_MODEL] = s.ollamaModel
            prefs[SettingsKeys.ACCENT] = s.accent
            prefs[SettingsKeys.LEVEL] = s.level.key
            prefs[SettingsKeys.SCENARIO] = s.scenario.key
            prefs[SettingsKeys.TTS_ENABLED] = s.ttsEnabled
            prefs[SettingsKeys.VOICE_SPEED] = s.voiceSpeed
        }
    }
}
