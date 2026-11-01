package com.rvishal.tvcalendar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // Undated open todos match every window: they re-anchor to today at read time.
    @Query("SELECT * FROM events WHERE (startMillis < :windowEnd AND endMillis > :windowStart) OR (kind = 'TODO' AND undated = 1 AND completed = 0) ORDER BY startMillis")
    fun observe(windowStart: Long, windowEnd: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE (startMillis < :windowEnd AND endMillis > :windowStart) OR (kind = 'TODO' AND undated = 1 AND completed = 0) ORDER BY startMillis")
    suspend fun inWindow(windowStart: Long, windowEnd: Long): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE source = :source")
    suspend fun deleteSource(source: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: EventEntity)

    @Query("UPDATE events SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM events")
    suspend fun clear()
}
