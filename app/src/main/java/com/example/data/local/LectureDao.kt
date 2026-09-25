package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY createdAt DESC")
    fun getAllLectures(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getLecturesByFolder(folderId: Long): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE id = :id LIMIT 1")
    suspend fun getLectureById(id: Long): LectureEntity?

    @Query("SELECT COUNT(*) FROM lectures")
    suspend fun getLectureCount(): Int

    @Query("SELECT * FROM lectures ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestLecture(): LectureEntity?

    @Query("""
        SELECT * FROM lectures 
        WHERE title LIKE '%' || :query || '%' 
           OR transcript LIKE '%' || :query || '%' 
           OR summary LIKE '%' || :query || '%' 
           OR explanation LIKE '%' || :query || '%'
           OR folderName LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchLectures(query: String): Flow<List<LectureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity): Long

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Delete
    suspend fun deleteLecture(lecture: LectureEntity)

    @Query("DELETE FROM lectures WHERE id = :id")
    suspend fun deleteLectureById(id: Long)

    @Query("DELETE FROM lectures WHERE title LIKE '%أساسيات%' OR audioPath LIKE '%default%' OR title LIKE '%تجريبي%'")
    suspend fun deleteDemoLectures()
}
