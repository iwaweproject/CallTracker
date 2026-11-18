package com.example.calltracker.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * Listens for call state changes and starts/stops the recording service.
 *
 *   RINGING  -> an incoming call is arriving
 *   OFFHOOK  -> a call is active (answered, or an outgoing call connected)
 *   IDLE     -> no call -> stop any recording
 *
 * The last dialled/incoming number is tracked so we can label the recording.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        if (!number.isNullOrBlank()) lastNumber = number

        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call is live -> try to start recording.
                val svc = Intent(context, CallRecordingService::class.java).apply {
                    action = CallRecordingService.ACTION_START
                    putExtra(CallRecordingService.EXTRA_NUMBER, lastNumber ?: "unknown")
                }
                context.startForegroundService(svc)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val svc = Intent(context, CallRecordingService::class.java).apply {
                    action = CallRecordingService.ACTION_STOP
                }
                context.startService(svc)
            }
            // RINGING: nothing to do yet; we wait until the call is answered.
        }
    }

    companion object {
        @Volatile var lastNumber: String? = null
    }
}
