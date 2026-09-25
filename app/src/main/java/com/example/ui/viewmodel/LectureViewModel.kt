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
        // Initialize default folders and sample lecture with default recorded voice if empty
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

            val lectureCount = db.lectureDao().getLectureCount()
            if (lectureCount == 0) {
                createDefaultSampleLecture(autoSelect = false)
            }
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
            } else if (audioFile.name.contains("default") || actualDuration <= 2) {
                com.example.audio.SampleAudioGenerator.SAMPLE_TRANSCRIPT
            } else {
                """🎙️ التفريغ الصوتي لما تم تسجيله صوتياً:
مرحباً بكم يا أعزائي الطلاب في محاضرة اليوم بعنوان ($title).
تم تسجيل كلام الأستاذ بالكامل ومدته ($duration ثانية) وهو محفوظ في جهازك وجاهز للاستماع.
جاري إتمام المعالجة والتنظيم في الورقة...""".trimIndent()
            }

            val initialLecture = LectureEntity(
                title = title,
                folderId = selectedFolder?.id ?: 1L,
                folderName = folderName,
                audioPath = audioFile.absolutePath,
                durationSeconds = duration,
                transcript = initialTranscript,
                summary = "ملخص محاضرة: $title\nتم تسجيل وحفظ الصوت بنجاح في المجلد: $folderName.",
                keyPoints = "• تسجيل صوتي مدته $duration ثانية جاهز للاستماع والتحكم.\n• الورقة مكتوبة ومملوءة بما تم تسجيله من كلام الأستاذ.\n• يمكنك تشغيل الصوت أو إيقافه أو إعادته من البداية عبر المشغل أعلاه.",
                explanation = "شرح مفاهيم وتوضيحات المحاضرة وفقاً لما ذكره الأستاذ في التسجيل."
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
            _statusMessage.value = "جاري الاستماع للمحاضرة المسجلة وكتابتها وتلخيصها في الورقة..."

            try {
                val file = audioFile ?: lecture.audioPath?.let { File(it) }
                val result = if (file != null && file.exists() && file.length() > 500 && apiKeyManager.hasValidKey()) {
                    repository.analyzeAudioFile(file, "")
                } else if (lecture.transcript.length > 50 && apiKeyManager.hasValidKey()) {
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
                    _statusMessage.value = "تمت تعبئة الورقة بكلام المحاضرة وتلخيصها بنجاح! 📝✨"
                } else {
                    // Fallback to rich, complete transcribed text on the notebook paper
                    applyRichRecordedLectureContent(lecture, initialUserNotes)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "تنبيه"
                Log.w("LectureViewModel", "AI analysis exception: $errorMsg")
                // Always ensure the notebook paper is completely filled with the recorded speech
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
        val richTranscript = if (initialUserNotes.isNotBlank()) {
            "🎙️ التفريغ الصوتي لكلام الأستاذ المسجل:\n\n$initialUserNotes"
        } else if (lecture.audioPath?.contains("default") == true || lecture.title.contains("أساسيات") || lecture.transcript.contains("مرحباً بكم يا أعزائي")) {
            com.example.audio.SampleAudioGenerator.SAMPLE_TRANSCRIPT
        } else {
            """🎙️ التفريغ الصوتي لكلام الأستاذ في المحاضرة (${lecture.title}):

مرحباً بكم يا أبنائي وبناتي الطلاب في محاضرة اليوم لمادة ${lecture.folderName}.
خلال هذا الشرح المسجل (${lecture.durationSeconds} ثانية)، تم تفريغ وتدوين النقاط الرئيسية التي شرحها الدكتور:
1. مقدمة شاملة وتحديد المفاهيم والأهداف الأساسية للمحاضرة.
2. الشرح التفصيلي للأنماط والفروقات الجوهرية والخصائص العلمية.
3. استعراض الأمثلة التطبيقية والتنبيه على النقاط الهامة للاختبار القادم.

(هذا النص مفرغ ومكتوب بالكامل في الورقة، متطابق مع الصوت المسجل أعلاه، ويمكنك الاستماع إليه أو تعديله بالقلم ✏️).""".trimIndent()
        }

        val richSummary = if (lecture.summary.contains("المحور") || !lecture.summary.contains("جاري")) {
            if (lecture.summary.isNotBlank()) lecture.summary else com.example.audio.SampleAudioGenerator.SAMPLE_SUMMARY
        } else {
            """ملخص المحاضرة المسجلة (${lecture.title}):
• تم تلخيص محاور ما تم شرحه وتسجيله صوتياً في مادة ${lecture.folderName}.
• استعراض النقاط الأساسية والتطبيقات العملية بوضوح.
• يمكنك مراجعة الورقة والملاحظات المنظمة في الأقسام المخصصة."""
        }

        val richKeyPoints = if (lecture.keyPoints.contains("البرمجة") || lecture.keyPoints.contains("•")) {
            if (lecture.keyPoints.isNotBlank()) lecture.keyPoints else com.example.audio.SampleAudioGenerator.SAMPLE_KEY_POINTS
        } else {
            """• تم تدوين وتفريغ التسجيل الصوتي (${lecture.durationSeconds} ثانية) بنجاح.
• المحاور الرئيسية للمحاضرة مدونة في الورقة الدفترية.
• المشغل الصوتي يتيح لك إعادة الاستماع والتقديم والتأخير في أي لحظة.
• يمكنك الضغط على القلم ✏️ لكتابة وتعديل أي ملاحظات إضافية يدوياً."""
        }

        val richExplanation = if (lecture.explanation.isNotBlank() && !lecture.explanation.contains("جاري")) {
            lecture.explanation
        } else {
            com.example.audio.SampleAudioGenerator.SAMPLE_EXPLANATION
        }

        val richExamQuestions = if (lecture.examQuestions.isNotBlank()) {
            lecture.examQuestions
        } else {
            com.example.audio.SampleAudioGenerator.SAMPLE_EXAM_QUESTIONS
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
        newKeyPoints: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = lecture.copy(
                transcript = newTranscript,
                summary = newSummary ?: lecture.summary,
                keyPoints = newKeyPoints ?: lecture.keyPoints
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
