package com.phonehardware.diagnostic.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val deviceName: String,
    val androidVersion: String,
    val startedAt: Long,
    val finishedAt: Long? = null
)

@Entity(tableName = "results", indices = [Index("sessionId")],
    foreignKeys = [ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)])
data class ResultEntity(
    @PrimaryKey(autoGenerate = true) val key: Long = 0,
    val sessionId: String,
    val testId: String,
    val testName: String,
    val category: String,
    val status: String,
    val json: String,
    val createdAt: Long
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun sessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM results WHERE sessionId = :session ORDER BY createdAt, key")
    fun results(session: String): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE sessionId = :session ORDER BY createdAt, key")
    suspend fun resultsOnce(session: String): List<ResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSession(value: SessionEntity)

    @Insert
    suspend fun putResult(value: ResultEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM sessions")
    suspend fun clear()
}

@Database(entities = [SessionEntity::class, ResultEntity::class], version = 1, exportSchema = false)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun dao(): SessionDao
}
