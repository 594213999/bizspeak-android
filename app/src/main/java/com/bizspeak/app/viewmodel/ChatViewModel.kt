package com.bizspeak.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bizspeak.app.data.ChatMessage
import com.bizspeak.app.data.TutorRepository
import com.bizspeak.app.speech.SpeechManager
import com.bizspeak.app.speech.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: TutorRepository,
    private val speech: SpeechManager,
    private val tts: TtsManager,
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = repo.messages
    val speechState = speech.state
    val partialSpeech = speech.partial
    val canUseVoice: Boolean get() = speech.isAvailable

    init {
        viewModelScope.launch { repo.send("") } // 不发送，仅预加载设置
    }

    fun startSession() = repo.clear()

    fun send(text: String) {
        val t = text.trim()
        if (t.isBlank()) return
        viewModelScope.launch { repo.send(t) }
    }

    fun startListening(onFinal: (String) -> Unit, onError: (String) -> Unit) {
        speech.start(onFinal = { onFinal(it) }, onError = { onError(it) })
    }

    fun stopListening() = speech.stop()

    fun speak(text: String) = tts.speak(text)

    override fun onCleared() {
        speech.destroy()
        tts.destroy()
        super.onCleared()
    }
}
