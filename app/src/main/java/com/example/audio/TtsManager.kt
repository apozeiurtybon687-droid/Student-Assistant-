package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.remote.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val geminiService: GeminiService
) : TextToSpeech.OnInitListener {

    private var androidTts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var ttsMediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsMode = MutableStateFlow("gemini-3.1-flash-tts-preview")
    val ttsMode: StateFlow<String> = _ttsMode.asStateFlow()

    init {
        androidTts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            androidTts?.language = Locale.forLanguageTag("ar")
            androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    /**
     * Speaks text aloud using gemini-3.1-flash-tts-preview with automatic fallback to Android TTS.
     */
    fun speak(text: String) {
        stop()
        _isSpeaking.value = true

        scope.launch(Dispatchers.IO) {
            var handledByGemini = false
            try {
                // Try Gemini TTS model gemini-3.1-flash-tts-preview
                val audioBytes = geminiService.generateSpeech(text.take(800))
                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    val tempFile = File.createTempFile("tts_gemini_", ".mp3", context.cacheDir)
                    FileOutputStream(tempFile).use { it.write(audioBytes) }

                    scope.launch(Dispatchers.Main) {
                        try {
                            ttsMediaPlayer = MediaPlayer().apply {
                                setDataSource(tempFile.absolutePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    _isSpeaking.value = false
                                    tempFile.delete()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TtsManager", "Gemini TTS player failed: ${e.message}")
                            fallbackAndroidTts(text)
                        }
                    }
                    handledByGemini = true
                }
            } catch (e: Exception) {
                Log.w("TtsManager", "Gemini TTS error: ${e.message}, falling back to Android TTS")
            }

            if (!handledByGemini) {
                scope.launch(Dispatchers.Main) {
                    fallbackAndroidTts(text)
                }
            }
        }
    }

    private fun fallbackAndroidTts(text: String) {
        if (isTtsInitialized && androidTts != null) {
            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_${System.currentTimeMillis()}")
            }
            androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utterance_id")
            _isSpeaking.value = true
        } else {
            _isSpeaking.value = false
        }
    }

    fun stop() {
        try {
            ttsMediaPlayer?.stop()
            ttsMediaPlayer?.release()
        } catch (_: Exception) {}
        ttsMediaPlayer = null

        try {
            androidTts?.stop()
        } catch (_: Exception) {}

        _isSpeaking.value = false
    }

    fun release() {
        stop()
        try {
            androidTts?.shutdown()
        } catch (_: Exception) {}
    }
}
