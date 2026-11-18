package com.example.calltracker.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.calltracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground service that attempts to record the current call.
 *
 * IMPORTANT — platform reality:
 * On stock Android 10+ a third-party app CANNOT capture the call's voice
 * stream (both parties). The audio sources below are tried in order of
 * preference; on a locked-down device the recorder will either fail to start
 * (caught here) or capture only silence / the local mic. Where it DOES work
 * (rooted devices, older Android, or OEMs that still permit it) you get a
 * proper recording. The result path is written back to the call record.
 */
class CallRecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputPath: String? = null
    private var number: String = "unknown"
    private var startedAt: Long = 0L
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                number = intent.getStringExtra(EXTRA_NUMBER) ?: "unknown"
                startForeground(NOTIF_ID, buildNotification())
                startRecording()
            }
            ACTION_STOP -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (recorder != null) return // already recording

        val dir = File(getExternalFilesDir(null), "recordings").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeNumber = number.replace(Regex("[^0-9+]"), "")
        val file = File(dir, "call_${safeNumber}_$stamp.m4a")
        outputPath = file.absolutePath
        startedAt = System.currentTimeMillis()

        // Audio sources to try, best first. VOICE_CALL captures both sides but
        // is blocked for non-system apps on modern Android; the others are
        // progressively more likely to be allowed (but capture less).
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in sources) {
            if (tryStart(source, file)) {
                Log.i(TAG, "Recording started with audio source $source")
                return
            }
        }
        Log.w(TAG, "Could not start recording with any audio source on this device.")
        outputPath = null
    }

    private fun tryStart(source: Int, file: File): Boolean {
        return try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            rec.setAudioSource(source)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(128_000)
            rec.setAudioSamplingRate(44_100)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            true
        } catch (e: Exception) {
            Log.d(TAG, "Audio source $source rejected: ${e.message}")
            false
        }
    }

    private fun stopRecording() {
        val rec = recorder ?: return
        try {
            rec.stop()
        } catch (e: Exception) {
            Log.d(TAG, "stop() failed (call may have been too short): ${e.message}")
        } finally {
            rec.reset()
            rec.release()
            recorder = null
        }

        val path = outputPath ?: return
        val file = File(path)
        // Discard empty/broken captures so the log isn't full of 0-byte files.
        if (!file.exists() || file.length() < 1024) {
            file.delete()
            return
        }
        // Link this recording to the matching call row (by number + start time).
        val num = number
        val at = startedAt
        scope.launch {
            AppDatabase.get(applicationContext).callDao()
                .attachRecording(num, at, path)
        }
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Call recording",
                NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Tracker")
            .setContentText("Recording the current call")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "CallRecordingService"
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_NUMBER = "number"
        private const val CHANNEL_ID = "call_recording"
        private const val NOTIF_ID = 42
    }
}
