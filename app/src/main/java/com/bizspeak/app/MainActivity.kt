package com.bizspeak.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.bizspeak.app.data.Level
import com.bizspeak.app.data.Scenario
import com.bizspeak.app.data.SettingsStore
import com.bizspeak.app.data.TutorRepository
import com.bizspeak.app.speech.SpeechManager
import com.bizspeak.app.speech.TtsManager
import com.bizspeak.app.ui.screens.ChatScreen
import com.bizspeak.app.ui.screens.HomeScreen
import com.bizspeak.app.ui.screens.SettingsScreen
import com.bizspeak.app.ui.theme.BizSpeakTheme
import com.bizspeak.app.viewmodel.ChatViewModel
import com.bizspeak.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsVm: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(SettingsStore(applicationContext)) as T
            }
        }
    }

    private val chatVm: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(
                    TutorRepository(applicationContext),
                    SpeechManager(this@MainActivity),
                    TtsManager(applicationContext),
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BizSpeakTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                when (val s = screen) {
                    Screen.Home -> {
                        val settings by settingsVm.settings.collectAsState(initial = null)
                        val cur = settings ?: return@BizSpeakTheme
                        HomeScreen(
                            level = cur.level,
                            scenario = cur.scenario,
                            onLevelChange = { settingsVm.update(level = it) },
                            onScenarioChange = { settingsVm.update(scenario = it) },
                            onStart = {
                                chatVm.startSession()
                                screen = Screen.Chat
                            },
                            onOpenSettings = { screen = Screen.Settings },
                        )
                    }
                    Screen.Chat -> {
                        val messages by chatVm.messages.collectAsState(initial = emptyList())
                        val speechState by chatVm.speechState.collectAsState()
                        val partial by chatVm.partialSpeech.collectAsState()
                        val isListening = speechState is com.bizspeak.app.speech.SpeechState.Listening ||
                            speechState is com.bizspeak.app.speech.SpeechState.Loading ||
                            speechState is com.bizspeak.app.speech.SpeechState.Processing
                        val ctx = this@MainActivity

                        fun startVoice() {
                            chatVm.startListening(
                                onFinal = { text -> if (text.isNotBlank()) chatVm.send(text) },
                                onError = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() },
                            )
                        }

                        // RECORD_AUDIO 是运行时权限：没申请过就直接调 SpeechRecognizer 会静默失败
                        val micPermission = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission(),
                        ) { granted ->
                            if (granted) startVoice()
                            else Toast.makeText(ctx, "需要录音权限才能使用语音输入", Toast.LENGTH_LONG).show()
                        }

                        ChatScreen(
                            messages = messages,
                            isListening = isListening,
                            canUseVoice = chatVm.canUseVoice,
                            partialText = partial,
                            onSend = { chatVm.send(it) },
                            onMicClick = {
                                // 录音中再点一次 = 停止并返回识别结果
                                if (isListening) {
                                    chatVm.stopListening()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        ctx, Manifest.permission.RECORD_AUDIO,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) startVoice()
                                    else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onSpeak = { chatVm.speak(it) },
                            onBack = { screen = Screen.Home },
                        )
                    }
                    Screen.Settings -> {
                        val settings by settingsVm.settings.collectAsState(initial = null)
                        val cur = settings ?: return@BizSpeakTheme
                        SettingsScreen(
                            settings = cur,
                            onBack = { screen = Screen.Home },
                            onSave = {
                                settingsVm.save(it)
                                screen = Screen.Home
                            },
                        )
                    }
                }
            }
        }
    }
}

private enum class Screen { Home, Chat, Settings }
