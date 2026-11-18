package com.example.calltracker.data

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls the system call log into our Room database.
 *
 * This is the part that is fully supported on every Android version: the OS
 * records the number, direction, timestamp and duration of every call, and
 * READ_CALL_LOG lets us read it.
 */
class CallLogRepository(private val context: Context) {

    private val dao = AppDatabase.get(context).callDao()

    val numberSummaries get() = dao.numberSummaries()
    val allCalls get() = dao.allCalls()
    fun callsForNumber(number: String) = dao.callsForNumber(number)

    /**
     * Reads the system call log and stores anything new.
     * Returns how many new rows were added.
     */
    suspend fun sync(): Int = withContext(Dispatchers.IO) {
        val since = dao.maxSystemLogId() ?: -1L

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        val records = mutableListOf<CallRecordEntity>()
        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            "${CallLog.Calls._ID} > ?",
            arrayOf(since.toString()),
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numCol = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameCol = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeCol = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateCol = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durCol = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (c.moveToNext()) {
                records += CallRecordEntity(
                    systemLogId = c.getLong(idCol),
                    number = c.getString(numCol) ?: "Unknown",
                    contactName = c.getString(nameCol),
                    type = typeToString(c.getInt(typeCol)),
                    dateMillis = c.getLong(dateCol),
                    durationSec = c.getLong(durCol)
                )
            }
        }

        if (records.isNotEmpty()) dao.upsertAll(records)
        records.size
    }

    private fun typeToString(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
        CallLog.Calls.MISSED_TYPE -> "MISSED"
        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
        CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
        CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
        else -> "UNKNOWN"
    }
}
