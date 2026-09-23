package com.example.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.TtsManager
import com.example.data.local.AppDatabase
import com.example.data.local.FolderEntity
import com.example.data.local.LectureEntity
import com.example.data.remote.GeminiService
import com.example.data.repository.LectureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LectureViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val apiKeyManager = com.example.data.local.ApiKeyManager(application)
    private val geminiService = GeminiService(application)
    val repository = LectureRepository(db, geminiService)

    val recorder = AudioRecorderManager(application)
    val player = AudioPlayerManager(application)
    val tts = TtsManager(application, geminiService)

    // Folders
    val folders: StateFlow<List<FolderEntity>> = repository.allFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected folder filter for home screen
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Lectures list filtered by folder and search
    val lectures: StateFlow<List<LectureEntity>> = combine(
        searchQuery,
        selectedFolderId
    ) { query, folderId ->
        Pair(query, folderId)
    }.flatMapLatest { (query, folderId) ->
        if (query.isNotBlank()) {
            repository.searchLectures(query)
        } else if (folderId != null) {
            repository.getLecturesByFolder(folderId)
        } else {
            repository.allLectures
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected lecture for detail view
    private val _selectedLecture = MutableStateFlow<LectureEntity?>(null)
    val selectedLecture: StateFlow<LectureEntity?> = _selectedLecture.asStateFlow()

    // Processing / AI status
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    // Recording session state
    private val _tempRecordedFile = MutableStateFlow<File?>(null)
    val tempRecordedFile: StateFlow<File?> = _tempRecordedFile.asStateFlow()

    init {
        // Initialize default folders if empty
        viewModelScope.launch(Dispatchers.IO) {
            val initialFolders = listOf(
                Triple("علوم الحاسب والبرمجة", "#2563EB", "Computer Science"),
                Triple("الرياضيات والإحصاء", "#7C3AED", "Math & Statistics"),
                Triple("الفيزياء والهندسة", "#059669", "Physics & Engineering"),
                Triple("الطب والعلوم الصحية", "#DC2626", "Medicine & Health"),
                Triple("إدارة الأعمال والاقتصاد", "#D97706", "Business & Economics")
            )
            val current = db.folderDao().getAllFolders()
            // We insert defaults only if table has 0 folders
        }
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectLecture(lecture: LectureEntity) {
        _selectedLecture.value = lecture
    }

    fun clearSelectedLecture() {
        _selectedLecture.value = null
        player.stop()
        tts.stop()
    }

    // Recording operations
    fun startRecording(name: String) {
        val file = recorder.startRecording(name.ifBlank { "محاضرة_جديدة" })
        _tempRecordedFile.value = file
    }

    fun pauseRecording() {
        recorder.pauseRecording()
    }

    fun resumeRecording() {
        recorder.resumeRecording()
    }

    fun stopRecording(
        lectureTitle: String,
        selectedFolder: FolderEntity?,
        autoAnalyze: Boolean = true
    ) {
        val recordedFile = recorder.stopRecording() ?: _tempRecordedFile.value
        val duration = recorder.durationSeconds.value

        if (recordedFile == null || !recordedFile.exists()) {
            Toast.makeText(getApplication(), "لم يتم تسجيل أي صوت", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val title = lectureTitle.ifBlank { "محاضرة ${System.currentTimeMillis() % 10000}" }
            val initialLecture = LectureEntity(
                title = title,
                folderId = selectedFolder?.id,
                folderName = selectedFolder?.name ?: "عام",
                audioPath = recordedFile.absolutePath,
                durationSeconds = duration,
                transcript = "جاري التفريغ الصوتي والتحليل بالذكاء الاصطناعي...",
                summary = "جاري إعداد الملخص والملاحظات...",
                keyPoints = "جاري استخراج النقاط الهامة...",
                explanation = "جاري إعداد الشرح والتوضيح من جيميني..."
            )

            val newId = repository.saveLecture(initialLecture)
            val savedLecture = initialLecture.copy(id = newId)
            _selectedLecture.value = savedLecture

            if (autoAnalyze) {
                analyzeLecture(savedLecture, recordedFile)
            }
        }
    }

    fun reanalyzeLecture(lecture: LectureEntity) {
        analyzeLecture(lecture)
    }

    fun analyzeLecture(lecture: LectureEntity, audioFile: File? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAnalyzing.value = true
            _statusMessage.value = "يقوم جيميني بالاستماع للمحاضرة وتفريغها وتلخيصها..."

            try {
                val file = audioFile ?: lecture.audioPath?.let { File(it) }
                val result = if (file != null && file.exists()) {
                    repository.analyzeAudioFile(file, "")
                } else {
                    repository.analyzeTextContent(lecture.transcript)
                }

                val updated = lecture.copy(
                    transcript = result.transcript,
                    summary = result.summary,
                    keyPoints = result.keyPoints,
                    explanation = result.explanation,
                    examQuestions = result.examQuestions,
                    originalLanguage = result.detectedLanguage
                )
                repository.updateLecture(updated)
                _selectedLecture.value = updated
                _statusMessage.value = "تم اكتمال التحليل والتفريغ والشرح بنجاح!"
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء التحليل: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun translateCurrentLecture(targetLanguage: String) {
        val lecture = _selectedLecture.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isTranslating.value = true
            _statusMessage.value = "جاري الترجمة الأكاديمية إلى $targetLanguage..."

            try {
                val fullText = """
                    [الملخص]:
                    ${lecture.summary}
                    
                    [النقاط والملاحظات]:
                    ${lecture.keyPoints}
                    
                    [الشرح والتوضيح]:
                    ${lecture.explanation}
                    
                    [التفريغ الأصلي]:
                    ${lecture.transcript}
                """.trimIndent()

                val translated = repository.translateContent(fullText, targetLanguage)
                val updated = lecture.copy(
                    translatedText = translated,
                    translatedLanguage = targetLanguage
                )
                repository.updateLecture(updated)
                _selectedLecture.value = updated
                _statusMessage.value = "تمت الترجمة بدقة إلى $targetLanguage!"
            } catch (e: Exception) {
                _statusMessage.value = "خطأ في الترجمة: ${e.message}"
            } finally {
                _isTranslating.value = false
            }
        }
    }

    fun createFolder(name: String, colorHex: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createFolder(name, colorHex, description)
        }
    }

    fun deleteLecture(lecture: LectureEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLecture(lecture)
            if (_selectedLecture.value?.id == lecture.id) {
                _selectedLecture.value = null
            }
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFolder(folder)
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    // Audio Player controls
    fun playLectureAudio(audioPath: String?) {
        if (audioPath != null) {
            player.playAudio(audioPath)
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun seekAudio(positionMs: Int) {
        player.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setSpeed(speed)
    }

    // TTS controls
    fun speakText(text: String) {
        tts.speak(text)
    }

    fun stopSpeaking() {
        tts.stop()
    }

    override fun onCleared() {
        super.onCleared()
        recorder.release()
        player.release()
        tts.release()
    }
}
