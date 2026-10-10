package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.DeckState

class PlaybackService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "tedoki_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.tedoki.ACTION_START"
        const val ACTION_STOP = "com.example.tedoki.ACTION_STOP"
        const val ACTION_TOGGLE_PLAY = "com.example.tedoki.ACTION_TOGGLE_PLAY"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun start(context: Context, title: String, subtitle: String = "", isPlaying: Boolean = true) {
            try {
                // If notification permission is required and not granted, skip foreground notification to avoid crash
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                }

                val intent = Intent(context, PlaybackService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_SUBTITLE, subtitle)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        context.startForegroundService(intent)
                    } catch (t: Throwable) {
                        try {
                            context.startService(intent)
                        } catch (e: Throwable) {
                            Log.w("PlaybackService", "Non-fatal: could not start service", e)
                        }
                    }
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.w("PlaybackService", "Non-fatal start error", t)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, PlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (t: Throwable) {
                // Non-fatal
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START -> {
                    try {
                        acquireWakeLock()
                        createNotificationChannel()
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Tedoki Mixtape"
                        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: "Wiedergabe aktiv"
                        val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                        val notification = buildNotification(title, subtitle, isPlaying)

                        var foregroundStarted = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                ServiceCompat.startForeground(
                                    this,
                                    NOTIFICATION_ID,
                                    notification,
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                                )
                                foregroundStarted = true
                            } catch (t: Throwable) {
                                Log.w("PlaybackService", "startForeground with mediaPlayback type failed, trying plain", t)
                            }
                        }

                        if (!foregroundStarted) {
                            try {
                                startForeground(NOTIFICATION_ID, notification)
                                foregroundStarted = true
                            } catch (t: Throwable) {
                                Log.w("PlaybackService", "Plain startForeground failed", t)
                            }
                        }

                        if (!foregroundStarted) {
                            // If startForeground failed, stop immediately to avoid Android OS ForegroundServiceDidNotStartInTimeException
                            stopSelf()
                        }
                    } catch (t: Throwable) {
                        Log.e("PlaybackService", "Error during ACTION_START", t)
                        stopSelf()
                    }
                }
                ACTION_TOGGLE_PLAY -> {
                    val engine = AudioEngine.instance
                    if (engine != null) {
                        if (engine.deckState.value == DeckState.PLAYING) {
                            engine.pause()
                        } else {
                            engine.play()
                        }
                    }
                }
                ACTION_STOP -> {
                    val engine = AudioEngine.instance
                    engine?.stop()
                    releaseWakeLock()
                    try {
                        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    } catch (t: Throwable) {
                        // ignore
                    }
                    stopSelf()
                }
            }
        } catch (t: Throwable) {
            Log.e("PlaybackService", "Unhandled error in onStartCommand", t)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "TedokiMixtape::AudioPlaybackWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(4 * 60 * 60 * 1000L) // 4 hours maximum
                }
            } catch (e: Exception) {
                // WakeLock acquisition non-fatal
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        wakeLock = null
    }

    private fun buildNotification(title: String, subtitle: String, isPlaying: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = Intent(this, PlaybackService::class.java).apply {
            action = ACTION_TOGGLE_PLAY
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, PlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (subtitle.isNotBlank()) subtitle else "Tedoki Boombox")
            .setContentIntent(openPendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseIcon, playPauseTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Tedoki Wiedergabe",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Hintergrund-Audiowiedergabe und Radio-Streaming"
                    setShowBadge(false)
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            } catch (t: Throwable) {
                Log.w("PlaybackService", "Could not create notification channel", t)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
