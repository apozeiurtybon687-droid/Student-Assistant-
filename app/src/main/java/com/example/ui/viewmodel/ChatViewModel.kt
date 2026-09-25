package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.LectureEntity
import com.example.data.remote.GeminiService
import com.example.data.repository.LectureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChatRolePreset(val title: String, val instruction: String) {
    PROFESSOR(
        "أستاذ المادة والمدقق الأكاديمي",
        "أنت أستاذ جامعي ومدرس ذكي لطالب جامعي. تشرح المفاهيم بعمق أكاديمي، وتبسط الأمور الصعبة، وتقدم أمثلة عملية من واقع التخصص."
    ),
    EXAM_PREP(
        "مراجع الامتحانات والاختبارات",
        "أنت متخصص في تحضير الطلاب للامتحانات الجامعية. تقوم باختبار الطالب بأسئلة ذكية، وتصحيح إجاباته، وتوضيح الأسئلة المتكررة وتوقعات الاختبار."
    ),
    QUICK_TUTOR(
        "مساعد المذاكرة السريع",
        "أنت مساعد مذاكرة يركز على الإيجاز الشديد، والنقاط المباشرة، وتلخيص الأفكار الطويلة في أسطر معدودة."
    )
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val geminiService = GeminiService(application)
    val repository = LectureRepository(db, geminiService)

    private val _lectureId = MutableStateFlow<Long>(0L)
    val lectureId: StateFlow<Long> = _lectureId.asStateFlow()

    private val _associatedLecture = MutableStateFlow<LectureEntity?>(null)
    val associatedLecture: StateFlow<LectureEntity?> = _associatedLecture.asStateFlow()

    // Available models according to feature specifications:
    // gemini-3.1-pro-preview for complex tasks, gemini-3.5-flash for general tasks, and gemini-3.1-flash-lite for fast tasks.
    val availableModels = listOf(
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (للمهام والتحليلات المعقدة)",
        "gemini-3.5-flash" to "Gemini 3.5 Flash (للمهام العامة والتفصيلية)",
        "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite (للمهام السريعة والفورية)"
    )

    private val _selectedModel = MutableStateFlow("gemini-3.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedRole = MutableStateFlow(ChatRolePreset.PROFESSOR)
    val selectedRole: StateFlow<ChatRolePreset> = _selectedRole.asStateFlow()

    private val _customRoleInstruction = MutableStateFlow("")
    val customRoleInstruction: StateFlow<String> = _customRoleInstruction.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = _lectureId.flatMapLatest { id ->
        repository.getChatMessages(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setLectureContext(lecture: LectureEntity?) {
        _associatedLecture.value = lecture
        _lectureId.value = lecture?.id ?: 0L
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
    }

    fun selectRole(preset: ChatRolePreset) {
        _selectedRole.value = preset
    }

    fun setCustomRoleInstruction(instruction: String) {
        _customRoleInstruction.value = instruction
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return
        val currentLecId = _lectureId.value
        val model = _selectedModel.value

        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            _errorMessage.value = null

            // Save user message in local DB
            val userMsg = ChatMessageEntity(
                lectureId = currentLecId,
                role = "user",
                content = userText.trim(),
                modelUsed = model
            )
            repository.saveChatMessage(userMsg)

            // Prepare conversation history
            val history = messages.value.map { it.role to it.content }.toMutableList()
            history.add("user" to userText.trim())

            // Prepare system instruction
            val baseRole = if (_customRoleInstruction.value.isNotBlank()) {
                _customRoleInstruction.value
            } else {
                _selectedRole.value.instruction
            }

            val lectureContext = _associatedLecture.value?.let { lec ->
                """
                معلومات المحاضرة الحالية للطالب:
                عنوان المحاضرة: ${lec.title}
                المادة / المجلد: ${lec.folderName}
                ملخص المحاضرة: ${lec.summary}
                الملاحظات والنقاط الرئيسية: ${lec.keyPoints}
                شرح وتوضيح المحاضرة: ${lec.explanation}
                """.trimIndent()
            } ?: ""

            val fullSystemInstruction = if (lectureContext.isNotBlank()) {
                "$baseRole\n\n$lectureContext"
            } else {
                baseRole
            }

            try {
                val assistantReply = repository.askChatbot(
                    modelName = model,
                    history = history,
                    roleInstruction = fullSystemInstruction
                )

                val modelMsg = ChatMessageEntity(
                    lectureId = currentLecId,
                    role = "model",
                    content = assistantReply,
                    modelUsed = model
                )
                repository.saveChatMessage(modelMsg)
            } catch (e: Exception) {
                _errorMessage.value = "خطأ في استلام الرد: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearChatMessages(_lectureId.value)
        }
    }
}
