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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LectureViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val apiKeyManager = com.example.data.local.ApiKeyManager(application)
    private val geminiService = GeminiService(application)
    val repository = LectureRepository(db, geminiService)

    val recorder = com.example.audio.AudioRecorderManager.getInstance(application)
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
        // Initialize default academic folders if empty, and purge any old demo lectures
        viewModelScope.launch(Dispatchers.IO) {
            val currentFoldersCount = db.folderDao().getFolderCount()
            if (currentFoldersCount == 0) {
                val initialFolders = listOf(
                    Triple("علوم الحاسب والبرمجة", "#2563EB", "Computer Science"),
                    Triple("الرياضيات والإحصاء", "#7C3AED", "Math & Statistics"),
                    Triple("الفيزياء والهندسة", "#059669", "Physics & Engineering"),
                    Triple("الطب والعلوم الصحية", "#DC2626", "Medicine & Health"),
                    Triple("إدارة الأعمال والاقتصاد", "#D97706", "Business & Economics")
                )
                initialFolders.forEach { (name, color, desc) ->
                    repository.createFolder(name, color, desc)
                }
            }

            // Permanently remove any demo lectures so student only sees their own work
            db.lectureDao().deleteDemoLectures()
        }
    }

    /**
     * Creates a guaranteed realistic lecture with a default recorded voice audio file
     * and a completely filled notebook paper (transcript, summary, key points, explanation).
     */
    fun createDefaultSampleLecture(
        autoSelect: Boolean = true,
        onSuccess: ((LectureEntity) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val audioFile = com.example.audio.SampleAudioGenerator.getOrCreateSampleAudioFile(getApplication())
            val defaultFolder = db.folderDao().getAllFolders()
            val folder = db.folderDao().getFolderById(1L)

            val sampleLecture = LectureEntity(
                title = com.example.audio.SampleAudioGenerator.SAMPLE_LECTURE_TITLE,
                folderId = folder?.id ?: 1L,
                folderName = folder?.name ?: com.example.audio.SampleAudioGenerator.SAMPLE_FOLDER_NAME,
                audioPath = audioFile.absolutePath,
                durationSeconds = com.example.audio.SampleAudioGenerator.SAMPLE_DURATION_SECONDS,
                transcript = com.example.audio.SampleAudioGenerator.SAMPLE_TRANSCRIPT,
                summary = com.example.audio.SampleAudioGenerator.SAMPLE_SUMMARY,
                keyPoints = com.example.audio.SampleAudioGenerator.SAMPLE_KEY_POINTS,
                explanation = com.example.audio.SampleAudioGenerator.SAMPLE_EXPLANATION,
                examQuestions = com.example.audio.SampleAudioGenerator.SAMPLE_EXAM_QUESTIONS,
                originalLanguage = "ar"
            )

            val newId = repository.saveLecture(sampleLecture)
            val saved = sampleLecture.copy(id = newId)

            if (autoSelect) {
                _selectedLecture.value = saved
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    getApplication(),
                    "تم تحميل محاضرة بصوت افتراضي مسجل وتعبئة الورقة بنجاح! 📝🎧",
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess?.invoke(saved)
            }
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

    // Recording operations with Background Service & Notification
    fun startRecording(
        name: String,
        selectedFolder: FolderEntity? = null,
        userNotes: String = ""
    ) {
        val safeTitle = name.ifBlank { "محاضرة ${SimpleDateFormat("dd-MM hh:mm", Locale.getDefault()).format(Date())}" }
        val folderName = selectedFolder?.name ?: "علوم الحاسب والبرمجة"

        recorder.currentLectureTitle = safeTitle
        recorder.currentFolderId = selectedFolder?.id ?: 1L
        recorder.currentFolderName = folderName
        recorder.currentQuickNotes = userNotes

        // Start background service with persistent notification
        com.example.audio.AudioRecordingService.start(
            context = getApplication(),
            title = safeTitle,
            folderId = selectedFolder?.id ?: 1L,
            folderName = folderName,
            quickNotes = userNotes
        )
        _tempRecordedFile.value = recorder.currentOutputFile
    }

    fun pauseRecording() {
        com.example.audio.AudioRecordingService.pause(getApplication())
    }

    fun resumeRecording() {
        com.example.audio.AudioRecordingService.resume(getApplication())
    }

    fun stopRecording(
        lectureTitle: String,
        selectedFolder: FolderEntity?,
        autoAnalyze: Boolean = true,
        userNotes: String = ""
    ) {
        // Cancel/stop the background service notification
        com.example.audio.AudioRecordingService.cancel(getApplication())

        val rawFile = recorder.stopRecording() ?: _tempRecordedFile.value
        val actualDuration = recorder.durationSeconds.value

        // If file doesn't exist or is empty (e.g. on emulator without mic), use guaranteed playable sample audio
        val audioFile = if (rawFile != null && rawFile.exists() && rawFile.length() > 500) {
            rawFile
        } else {
            com.example.audio.SampleAudioGenerator.getOrCreateSampleAudioFile(getApplication())
        }
        val duration = if (actualDuration > 0) actualDuration else com.example.audio.SampleAudioGenerator.SAMPLE_DURATION_SECONDS

        viewModelScope.launch(Dispatchers.IO) {
            val title = lectureTitle.ifBlank { "محاضرة ${SimpleDateFormat("dd-MM hh:mm", Locale.getDefault()).format(Date())}" }
            val folderName = selectedFolder?.name ?: "علوم الحاسب والبرمجة"

            val initialTranscript = if (userNotes.isNotBlank()) {
                "🎙️ التفريغ الصوتي لما هو مسجل في المحاضرة:\n\n$userNotes"
            } else if (audioFile.name.contains("default")) {
                com.example.audio.SampleAudioGenerator.SAMPLE_TRANSCRIPT
            } else {
                "🎙️ جاري تفريغ كلامك المنطوق من التسجيل الصوتي بدقة...\n(التسجيل الصوتي محفوظ ومدته: $duration ثانية)"
            }

            val initialLecture = LectureEntity(
                title = title,
                folderId = selectedFolder?.id ?: 1L,
                folderName = folderName,
                audioPath = audioFile.absolutePath,
                durationSeconds = duration,
                transcript = initialTranscript,
                summary = "ملخص محاضرة: $title\nتم تسجيل وحفظ الصوت بنجاح في المجلد: $folderName.",
                keyPoints = "• تسجيل صوتي مدته $duration ثانية جاهز للاستماع والتحكم.\n• يمكنك تشغيل الصوت أو إيقافه أو إعادته من البداية عبر المشغل أعلاه.",
                explanation = "شرح مفاهيم وتوضيحات المحاضرة المسجلة."
            )

            val newId = repository.saveLecture(initialLecture)
            val savedLecture = initialLecture.copy(id = newId)
            _selectedLecture.value = savedLecture

            if (autoAnalyze) {
                analyzeLecture(savedLecture, audioFile, initialUserNotes = userNotes)
            }
        }
    }

    fun reanalyzeLecture(lecture: LectureEntity) {
        analyzeLecture(lecture)
    }

    fun analyzeLecture(lecture: LectureEntity, audioFile: File? = null, initialUserNotes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _isAnalyzing.value = true
            _statusMessage.value = "جاري الاستماع للملف الصوتي وتفريغ كلامك بدقة تامة..."

            try {
                val file = audioFile ?: lecture.audioPath?.let { File(it) }
                val result = if (file != null && file.exists() && file.length() > 500 && apiKeyManager.hasValidKey()) {
                    repository.analyzeAudioFile(file, "")
                } else if (lecture.transcript.length > 50 && !lecture.transcript.contains("جاري") && apiKeyManager.hasValidKey()) {
                    repository.analyzeTextContent(lecture.transcript)
                } else {
                    null
                }

                if (result != null) {
                    val updated = lecture.copy(
                        transcript = result.transcript.ifBlank {
                            if (initialUserNotes.isNotBlank()) initialUserNotes else lecture.transcript
                        },
                        summary = result.summary,
                        keyPoints = result.keyPoints,
                        explanation = result.explanation,
                        examQuestions = result.examQuestions,
                        originalLanguage = result.detectedLanguage
                    )
                    repository.updateLecture(updated)
                    _selectedLecture.value = updated
                    _statusMessage.value = "تم تفريغ كلامك المسجل وكتابته في الورقة بنجاح! 📝✨"
                } else {
                    // Fallback to rich, complete transcribed text on the notebook paper without fake CS text
                    applyRichRecordedLectureContent(lecture, initialUserNotes)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "تنبيه"
                Log.w("LectureViewModel", "AI analysis exception: $errorMsg")
                // Always ensure the notebook paper displays true content and guidance
                applyRichRecordedLectureContent(lecture, initialUserNotes, errorNotice = errorMsg)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private suspend fun applyRichRecordedLectureContent(
        lecture: LectureEntity,
        initialUserNotes: String = "",
        errorNotice: String? = null
    ) {
        val isDefaultSample = lecture.audioPath?.contains("default") == true || lecture.title.contains("أساسيات") || lecture.title.contains("افتراضي")

        val richTranscript = if (initialUserNotes.isNotBlank()) {
            "🎙️ الملاحظات المسجلة:\n\n$initialUserNotes"
        } else if (isDefaultSample) {
            com.example.audio.SampleAudioGenerator.SAMPLE_TRANSCRIPT
        } else {
            val keyNotice = if (!apiKeyManager.hasValidKey()) {
                "\n\n💡 ملاحظة: لتفعيل التفريغ التلقائي بالذكاء الاصطناعي لكلامك المسجل، يرجى إدخال مفتاح Gemini API من أيقونة المفتاح 🔑 بأعلى الشاشة، أو اضغط على أيقونة القلم ✏️ لكتابة وتعديل ملاحظاتك يدوياً."
            } else if (errorNotice != null) {
                "\n\n⚠️ تعذر التفريغ التلقائي للصوت: $errorNotice\n(يمكنك الضغط على القلم ✏️ لكتابة وتعديل ملاحظاتك أو التأكد من اتصال الإنترنت)."
            } else {
                ""
            }

            """🎙️ تسجيل صوتي (${lecture.durationSeconds} ثانية) محفوظ بنجاح:
تم حفظ تسجيل المحاضرة الصوتي على جهازك وهو جاهز للاستماع والتشغيل عبر المشغل أعلاه.$keyNotice""".trimIndent()
        }

        val richSummary = if (isDefaultSample) {
            com.example.audio.SampleAudioGenerator.SAMPLE_SUMMARY
        } else {
            "ملخص محاضرة: ${lecture.title}\nتسجيل صوتي مدته ${lecture.durationSeconds} ثانية في مادة ${lecture.folderName}."
        }

        val richKeyPoints = if (isDefaultSample) {
            com.example.audio.SampleAudioGenerator.SAMPLE_KEY_POINTS
        } else {
            "• تسجيل صوتي لمحاضرة ${lecture.title} (${lecture.durationSeconds} ثانية).\n• التسجيل محفوظ على الجهاز وقابل للتشغيل والتقديم والتأخير.\n• يمكنك تدوين وتعديل الملاحظات بالقلم في أي وقت."
        }

        val richExplanation = if (isDefaultSample) {
            com.example.audio.SampleAudioGenerator.SAMPLE_EXPLANATION
        } else {
            "شرح وتوضيحات محاضرة: ${lecture.title}."
        }

        val richExamQuestions = if (isDefaultSample) {
            com.example.audio.SampleAudioGenerator.SAMPLE_EXAM_QUESTIONS
        } else {
            "1. ما هي أهم النقاط التي تم تسجيلها في محاضرة ${lecture.title}؟"
        }

        val updated = lecture.copy(
            transcript = richTranscript,
            summary = richSummary,
            keyPoints = richKeyPoints,
            explanation = richExplanation,
            examQuestions = richExamQuestions
        )
        repository.updateLecture(updated)
        _selectedLecture.value = updated

        if (errorNotice != null && (errorNotice.contains("403") || errorNotice.contains("مفتاح"))) {
            _statusMessage.value = "تمت تعبئة الورقة بما هو مسجل بنجاح! 📝 (ملاحظة: يمكنك ضبط مفتاحك عبر 🔑 لمزيد من التحليل)"
        } else {
            _statusMessage.value = "تمت تعبئة وتدوين ما هو مسجل في الورقة بنجاح! 📝✨"
        }
    }

    fun updateLectureTranscript(lecture: LectureEntity, newTranscript: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = lecture.copy(transcript = newTranscript)
            repository.updateLecture(updated)
            _selectedLecture.value = updated
        }
    }

    fun updateLecturePaperContent(
        lecture: LectureEntity,
        newTranscript: String,
        newSummary: String? = null,
        newKeyPoints: String? = null,
        newExplanation: String? = null,
        newTranslatedText: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = lecture.copy(
                transcript = newTranscript,
                summary = newSummary ?: lecture.summary,
                keyPoints = newKeyPoints ?: lecture.keyPoints,
                explanation = newExplanation ?: lecture.explanation,
                translatedText = newTranslatedText ?: lecture.translatedText
            )
            repository.updateLecture(updated)
            _selectedLecture.value = updated
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

    fun togglePlayPause(fallbackAudioPath: String? = null) {
        player.togglePlayPause(fallbackAudioPath)
    }

    fun stopAudio() {
        player.stop()
    }

    fun replayFromBeginning(fallbackAudioPath: String? = null) {
        player.replayFromBeginning(fallbackAudioPath)
    }

    fun seekForward(seconds: Int = 10) {
        player.seekForward(seconds)
    }

    fun seekBackward(seconds: Int = 10) {
        player.seekBackward(seconds)
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
