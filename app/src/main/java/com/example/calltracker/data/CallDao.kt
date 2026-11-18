package com.example.calltracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<CallRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: CallRecordEntity)

    /** All calls, newest first. */
    @Query("SELECT * FROM call_records ORDER BY dateMillis DESC")
    fun allCalls(): Flow<List<CallRecordEntity>>

    /** Every call to/from one number, newest first. */
    @Query("SELECT * FROM call_records WHERE number = :number ORDER BY dateMillis DESC")
    fun callsForNumber(number: String): Flow<List<CallRecordEntity>>

    /** One row per number with counts, last-seen date and total talk time. */
    @Query(
        """
        SELECT number,
               MAX(contactName) AS contactName,
               COUNT(*) AS callCount,
               MAX(dateMillis) AS lastDateMillis,
               SUM(durationSec) AS totalDurationSec
        FROM call_records
        GROUP BY number
        ORDER BY lastDateMillis DESC
        """
    )
    fun numberSummaries(): Flow<List<NumberSummary>>

    @Query("SELECT MAX(systemLogId) FROM call_records")
    suspend fun maxSystemLogId(): Long?

    /** Attach a recording path to an already-stored call (matched by time + number). */
    @Query(
        """
        UPDATE call_records SET recordingPath = :path
        WHERE number = :number
        AND ABS(dateMillis - :approxDateMillis) < 60000
        """
    )
    suspend fun attachRecording(number: String, approxDateMillis: Long, path: String)
}
