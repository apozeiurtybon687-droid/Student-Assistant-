package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FolderEntity
import com.example.data.local.LectureEntity
import com.example.data.remote.GeminiService
import com.example.data.remote.LectureAnalysisResult
import kotlinx.coroutines.flow.Flow
import java.io.File

class LectureRepository(
    private val database: AppDatabase,
    val geminiService: GeminiService
) {
    private val lectureDao = database.lectureDao()
    private val folderDao = database.folderDao()
    private val chatMessageDao = database.chatMessageDao()

    val allLectures: Flow<List<LectureEntity>> = lectureDao.getAllLectures()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    fun getLecturesByFolder(folderId: Long): Flow<List<LectureEntity>> =
        lectureDao.getLecturesByFolder(folderId)

    suspend fun getLectureById(id: Long): LectureEntity? =
        lectureDao.getLectureById(id)

    fun searchLectures(query: String): Flow<List<LectureEntity>> =
        lectureDao.searchLectures(query)

    suspend fun saveLecture(lecture: LectureEntity): Long =
        lectureDao.insertLecture(lecture)

    suspend fun updateLecture(lecture: LectureEntity) =
        lectureDao.updateLecture(lecture)

    suspend fun deleteLecture(lecture: LectureEntity) {
        // Delete audio file if exists
        lecture.audioPath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
        lectureDao.deleteLecture(lecture)
    }

    suspend fun deleteLectureById(id: Long) =
        lectureDao.deleteLectureById(id)

    // Folders
    suspend fun createFolder(name: String, colorHex: String = "#3B82F6", description: String = ""): Long {
        return folderDao.insertFolder(FolderEntity(name = name, colorHex = colorHex, description = description))
    }

    suspend fun deleteFolder(folder: FolderEntity) =
        folderDao.deleteFolder(folder)

    // Chat
    fun getChatMessages(lectureId: Long): Flow<List<ChatMessageEntity>> =
        if (lectureId == 0L) chatMessageDao.getGlobalMessages()
        else chatMessageDao.getMessagesForLecture(lectureId)

    suspend fun saveChatMessage(message: ChatMessageEntity): Long =
        chatMessageDao.insertMessage(message)

    suspend fun clearChatMessages(lectureId: Long) =
        chatMessageDao.clearMessagesForLecture(lectureId)

    // Gemini operations
    suspend fun transcribeAudio(audioFile: File): String {
        return geminiService.transcribeAudioWithGemini(audioFile)
    }

    suspend fun analyzeAudioFile(audioFile: File, userHint: String): LectureAnalysisResult {
        return geminiService.analyzeLectureAudio(audioFile, userHint)
    }

    suspend fun analyzeTextContent(content: String): LectureAnalysisResult {
        return geminiService.analyzeLectureText(content)
    }

    suspend fun translateContent(text: String, targetLanguage: String): String {
        return geminiService.translateText(text, targetLanguage)
    }

    suspend fun askChatbot(
        modelName: String,
        history: List<Pair<String, String>>,
        roleInstruction: String
    ): String {
        return geminiService.sendChatMessage(modelName, history, roleInstruction)
    }
}
