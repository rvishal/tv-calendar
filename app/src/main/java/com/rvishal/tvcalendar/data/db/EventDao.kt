package com.rvishal.tvcalendar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE startMillis < :windowEnd AND endMillis > :windowStart ORDER BY startMillis")
    fun observe(windowStart: Long, windowEnd: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startMillis < :windowEnd AND endMillis > :windowStart ORDER BY startMillis")
    suspend fun inWindow(windowStart: Long, windowEnd: Long): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE source = :source")
    suspend fun deleteSource(source: String)

    @Query("DELETE FROM events")
    suspend fun clear()
}
