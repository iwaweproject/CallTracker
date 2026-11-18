package com.example.calltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per call. We key on the system call-log id so re-syncing never
 * creates duplicates.
 */
@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey val systemLogId: Long,
    val number: String,
    val contactName: String?,
    /** INCOMING / OUTGOING / MISSED / REJECTED / VOICEMAIL / BLOCKED / UNKNOWN */
    val type: String,
    /** When the call happened, epoch millis. */
    val dateMillis: Long,
    /** Duration in seconds (0 for missed/rejected). */
    val durationSec: Long,
    /** Absolute path to the audio file, or null if nothing was recorded. */
    val recordingPath: String? = null
)

/** Aggregated view: one entry per distinct number. */
data class NumberSummary(
    val number: String,
    val contactName: String?,
    val callCount: Int,
    val lastDateMillis: Long,
    val totalDurationSec: Long
)
