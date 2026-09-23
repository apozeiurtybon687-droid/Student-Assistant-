package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _maxAmplitude = MutableStateFlow(0)
    val maxAmplitude: StateFlow<Int> = _maxAmplitude.asStateFlow()

    fun startRecording(customName: String): File {
        stopRecording()

        val dir = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
        val safeName = customName.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
        val file = File(dir, "lecture_${safeName}_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        _isRecording.value = true
        _isPaused.value = false
        _durationSeconds.value = 0

        startTimer()
        return file
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    recorder?.pause()
                    _isPaused.value = true
                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Pause failed: ${e.message}")
            }
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    recorder?.resume()
                    _isPaused.value = false
                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Resume failed: ${e.message}")
            }
        }
    }

    fun stopRecording(): File? {
        timerJob?.cancel()
        timerJob = null

        val file = currentOutputFile
        if (_isRecording.value) {
            try {
                recorder?.stop()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Stop failed: ${e.message}")
            }
            try {
                recorder?.release()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Release failed: ${e.message}")
            }
            recorder = null
        }
        _isRecording.value = false
        _isPaused.value = false
        _maxAmplitude.value = 0
        return file
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _isRecording.value) {
                if (!_isPaused.value) {
                    _durationSeconds.value += 1
                    try {
                        val amp = recorder?.maxAmplitude ?: 0
                        _maxAmplitude.value = amp
                    } catch (_: Exception) {}
                }
                delay(1000)
            }
        }
    }

    fun release() {
        stopRecording()
    }
}
