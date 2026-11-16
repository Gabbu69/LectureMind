package com.lecturemind.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled lecture",
    val subject: String = "General",
    val created: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val status: String = "saved",
    val notes: String = "",
    val reviewer: String = "",
    val quiz: String = "",
    val message: String = "",
    val generatedProvider: String = "",
    val generatedLanguage: String = "",
    val contentRevision: Long = 0
)
@Entity(tableName = "audio", foreignKeys = [ForeignKey(entity = Lecture::class, parentColumns = ["id"], childColumns = ["lectureId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lectureId")])
data class AudioPart(@PrimaryKey val id: String, val lectureId: String, val position: Int, val path: String, val startMs: Long, val durationMs: Long = 0, val finalized: Boolean = false, val transcript: String = "", val transcriptionProvider: String = "")
@Entity(tableName = "bookmarks", foreignKeys = [ForeignKey(entity = Lecture::class, parentColumns = ["id"], childColumns = ["lectureId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lectureId")])
data class Bookmark(@PrimaryKey val id: String = UUID.randomUUID().toString(), val lectureId: String, val positionMs: Long, val label: String = "Important moment")
@Entity(tableName = "attempts", foreignKeys = [ForeignKey(entity = Lecture::class, parentColumns = ["id"], childColumns = ["lectureId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lectureId")])
data class Attempt(@PrimaryKey val id: String = UUID.randomUUID().toString(), val lectureId: String, val created: Long = System.currentTimeMillis(), val correct: Int, val total: Int, val answers: String, val questions: String)

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY created DESC") fun observeLectures(): Flow<List<Lecture>>
    @Query("SELECT * FROM lectures WHERE id = :id") fun observeLecture(id: String): Flow<Lecture?>
    @Query("SELECT * FROM lectures WHERE id = :id") suspend fun lecture(id: String): Lecture?
    @Query("SELECT * FROM lectures") suspend fun all(): List<Lecture>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(lecture: Lecture)
    @Query("UPDATE lectures SET status = :status, message = :message WHERE id = :id") suspend fun status(id: String, status: String, message: String = "")
    @Query("UPDATE lectures SET title = :title, subject = :subject WHERE id = :id") suspend fun rename(id: String, title: String, subject: String)
    @Query("UPDATE lectures SET durationMs = :duration WHERE id = :id") suspend fun duration(id: String, duration: Long)
    @Query("UPDATE lectures SET notes = :notes, contentRevision = contentRevision + 1 WHERE id = :id") suspend fun notes(id: String, notes: String)
    @Query("UPDATE lectures SET notes = :notes, reviewer = :reviewer, quiz = :quiz, generatedProvider = :provider, generatedLanguage = :language, status = 'ready', message = '', contentRevision = contentRevision + 1 WHERE id = :id") suspend fun materials(id: String, notes: String, reviewer: String, quiz: String, provider: String, language: String)
    @Query("DELETE FROM lectures WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT * FROM audio WHERE lectureId = :id ORDER BY position") fun observeAudio(id: String): Flow<List<AudioPart>>
    @Query("SELECT * FROM audio WHERE lectureId = :id ORDER BY position") suspend fun audio(id: String): List<AudioPart>
    @Upsert suspend fun putAudio(part: AudioPart)
    @Query("SELECT * FROM bookmarks WHERE lectureId = :id ORDER BY positionMs") fun observeBookmarks(id: String): Flow<List<Bookmark>>
    @Insert suspend fun bookmark(bookmark: Bookmark)
    @Query("SELECT * FROM attempts WHERE lectureId = :id ORDER BY created DESC") fun observeAttempts(id: String): Flow<List<Attempt>>
    @Insert suspend fun attempt(attempt: Attempt)
}
@Database(entities = [Lecture::class, AudioPart::class, Bookmark::class, Attempt::class], version = 1, exportSchema = true)
abstract class LectureDatabase : RoomDatabase() {
    abstract fun dao(): LectureDao
    companion object {
        @Volatile private var instance: LectureDatabase? = null
        fun get(context: Context): LectureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LectureDatabase::class.java, "lecturemind.db").build().also { instance = it }
        }
    }
}
