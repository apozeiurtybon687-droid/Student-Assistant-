package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long? = null,
    val folderName: String = "عام",
    val title: String,
    val audioPath: String? = null,
    val durationSeconds: Int = 0,
    val transcript: String = "",
    val summary: String = "",
    val keyPoints: String = "",
    val explanation: String = "",
    val examQuestions: String = "",
    val originalLanguage: String = "ar",
    val translatedText: String? = null,
    val translatedLanguage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
