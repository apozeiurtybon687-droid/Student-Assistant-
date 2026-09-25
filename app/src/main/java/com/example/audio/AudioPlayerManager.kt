package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
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

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0)
    val totalDurationMs: StateFlow<Int> = _totalDurationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun playAudio(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.w("AudioPlayerManager", "Audio file does not exist: $filePath")
            return
        }

        currentFilePath = filePath
        releasePlayerOnly()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                applySpeed(_playbackSpeed.value)
                seekTo(0)
                start()
                _isPlaying.value = true
                _totalDurationMs.value = duration
                _currentPositionMs.value = 0

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0 // Reset to beginning on completion
                    progressJob?.cancel()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    true
                }
            }
            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error starting playback: ${e.message}", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause(fallbackFilePath: String? = null) {
        val targetPath = currentFilePath ?: fallbackFilePath
        val player = mediaPlayer

        if (player == null) {
            if (!targetPath.isNullOrEmpty()) {
                playAudio(targetPath)
            }
            return
        }

        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            } else {
                // If at or near the end, rewind to start first
                val currentPos = player.currentPosition
                val dur = player.duration
                if (dur > 0 && currentPos >= dur - 300) {
                    player.seekTo(0)
                    _currentPositionMs.value = 0
                }
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "togglePlayPause failed: ${e.message}", e)
            if (!targetPath.isNullOrEmpty()) {
                playAudio(targetPath)
            }
        }
    }

    fun replayFromBeginning(fallbackFilePath: String? = null) {
        val targetPath = currentFilePath ?: fallbackFilePath
        if (targetPath.isNullOrEmpty()) return

        val player = mediaPlayer
        if (player == null) {
            playAudio(targetPath)
            return
        }

        try {
            player.seekTo(0)
            _currentPositionMs.value = 0
            if (!player.isPlaying) {
                player.start()
                _isPlaying.value = true
            }
            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "replay failed: ${e.message}", e)
            playAudio(targetPath)
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
                player.seekTo(0)
            }
        } catch (_: Exception) {}
        _isPlaying.value = false
        _currentPositionMs.value = 0
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            try {
                val clamped = positionMs.coerceIn(0, _totalDurationMs.value.coerceAtLeast(0))
                player.seekTo(clamped)
                _currentPositionMs.value = clamped
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "seekTo failed: ${e.message}")
            }
        }
    }

    fun seekForward(seconds: Int = 10) {
        seekTo(_currentPositionMs.value + (seconds * 1000))
    }

    fun seekBackward(seconds: Int = 10) {
        seekTo((_currentPositionMs.value - (seconds * 1000)).coerceAtLeast(0))
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        mediaPlayer?.let { player ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    player.playbackParams = PlaybackParams().apply { this.speed = speed }
                } catch (e: Exception) {
                    Log.e("AudioPlayerManager", "Failed to set playback speed: ${e.message}")
                }
            }
        }
    }

    private fun releasePlayerOnly() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    } catch (_: Exception) {}
                }
                delay(150)
            }
        }
    }

    fun release() {
        stop()
        releasePlayerOnly()
        currentFilePath = null
    }
}
