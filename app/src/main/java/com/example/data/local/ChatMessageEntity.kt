package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lectureId: Long = 0, // 0 indicates global chat, or lecture-specific
    val role: String, // "user" or "model"
    val content: String,
    val modelUsed: String = "models/gemini-3.8-flash",
    val timestamp: Long = System.currentTimeMillis()
)
