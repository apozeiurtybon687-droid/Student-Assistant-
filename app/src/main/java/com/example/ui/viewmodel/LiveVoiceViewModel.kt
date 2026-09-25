package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecorderManager
import com.example.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class LiveVoiceTurn(
    val speaker: String, // "Student" or "Gemini Live"
    val text: String,
    val hasAudio: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class LiveVoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiService(application)
    val recorder = AudioRecorderManager(application)
    private var mediaPlayer: MediaPlayer? = null

    private val _isLiveConnected = MutableStateFlow(true)
    val isLiveConnected: StateFlow<Boolean> = _isLiveConnected.asStateFlow()

    private val _isSpeakingToLive = MutableStateFlow(false)
    val isSpeakingToLive: StateFlow<Boolean> = _isSpeakingToLive.asStateFlow()

    private val _isLiveResponding = MutableStateFlow(false)
    val isLiveResponding: StateFlow<Boolean> = _isLiveResponding.asStateFlow()

    private val _conversation = MutableStateFlow<List<LiveVoiceTurn>>(emptyList())
    val conversation: StateFlow<List<LiveVoiceTurn>> = _conversation.asStateFlow()

    private val _activeVoiceName = MutableStateFlow("Kore")
    val activeVoiceName: StateFlow<String> = _activeVoiceName.asStateFlow()

    private var currentRecordedVoiceFile: File? = null

    init {
        // Welcome turn
        _conversation.value = listOf(
            LiveVoiceTurn(
                speaker = "Gemini Live",
                text = "مرحباً بك! أنا مساعدك الصوتي الذكي المباشر. اضغط على الميكروفون وتحدث معي عن أي استفسار دراسي أو فكرة تحتاج لتوضيح وسأجيبك فوراً بالصوت والكلام."
            )
        )
    }

    fun startSpeaking() {
        if (_isLiveResponding.value) return
        val file = recorder.startRecording("live_turn")
        currentRecordedVoiceFile = file
        _isSpeakingToLive.value = true
    }

    fun finishSpeakingAndSend() {
        if (!_isSpeakingToLive.value) return
        val recordedFile = recorder.stopRecording() ?: currentRecordedVoiceFile
        _isSpeakingToLive.value = false

        if (recordedFile == null || !recordedFile.exists()) return

        val audioBytes = recordedFile.readBytes()
        if (audioBytes.isEmpty()) return

        val duration = recorder.durationSeconds.value
        val studentTurn = LiveVoiceTurn(
            speaker = "الطالب",
            text = "تسجيل صوتي ($duration ثانية) 🎙️"
        )
        _conversation.value = _conversation.value + studentTurn

        viewModelScope.launch(Dispatchers.IO) {
            _isLiveResponding.value = true

            val (replyText, replyAudioBytes) = geminiService.liveVoiceInteraction(
                userAudioBytes = audioBytes,
                userText = null,
                systemInstruction = "أنت مدرس جامعي رائع متحدث باللغة العربية عبر gemini-3.8-live. تحدث بنبرة مشجعة، وقدم إجابات ذكية وموجزة وواضحة تلائم الطالب الجامعي."
            )

            val geminiTurn = LiveVoiceTurn(
                speaker = "Gemini Live (gemini-3.8-live)",
                text = replyText,
                hasAudio = replyAudioBytes != null
            )
            _conversation.value = _conversation.value + geminiTurn

            // Play response audio if returned
            if (replyAudioBytes != null && replyAudioBytes.isNotEmpty()) {
                playAudioBytes(replyAudioBytes)
            }

            _isLiveResponding.value = false
            try { recordedFile.delete() } catch (_: Exception) {}
        }
    }

    fun sendTextToLive(text: String) {
        if (text.isBlank() || _isLiveResponding.value) return
        val studentTurn = LiveVoiceTurn(speaker = "الطالب", text = text.trim())
        _conversation.value = _conversation.value + studentTurn

        viewModelScope.launch(Dispatchers.IO) {
            _isLiveResponding.value = true

            val (replyText, replyAudioBytes) = geminiService.liveVoiceInteraction(
                userAudioBytes = null,
                userText = text.trim(),
                systemInstruction = "أنت مدرس جامعي رائع متحدث باللغة العربية عبر gemini-3.8-live. تحدث بنبرة مشجعة، وقدم إجابات ذكية وموجزة وواضحة تلائم الطالب الجامعي."
            )

            val geminiTurn = LiveVoiceTurn(
                speaker = "Gemini Live (gemini-3.8-live)",
                text = replyText,
                hasAudio = replyAudioBytes != null
            )
            _conversation.value = _conversation.value + geminiTurn

            if (replyAudioBytes != null && replyAudioBytes.isNotEmpty()) {
                playAudioBytes(replyAudioBytes)
            }

            _isLiveResponding.value = false
        }
    }

    private fun playAudioBytes(bytes: ByteArray) {
        try {
            val tempFile = File.createTempFile("live_reply_", ".mp3", getApplication<Application>().cacheDir)
            FileOutputStream(tempFile).use { it.write(bytes) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    tempFile.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun stopAudioPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        recorder.release()
        stopAudioPlayback()
    }
}
