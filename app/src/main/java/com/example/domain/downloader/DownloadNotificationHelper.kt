package com.example.domain.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object DownloadNotificationHelper {

    private const val CHANNEL_ID = "reelsave_downloads"
    private const val CHANNEL_NAME = "Reel Downloads"
    private const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Reel download progress and completion notifications"
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(
        context: Context,
        id: Long,
        filename: String,
        progress: Float,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        createNotificationChannel(context)

        val progressPercent = (progress * 100).toInt().coerceIn(0, 100)
        val indeterminate = totalBytes <= 0

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading Reel")
            .setContentText(filename)
            .setProgress(100, progressPercent, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)

        try {
            NotificationManagerCompat.from(context).notify(
                (NOTIFICATION_ID_BASE + id).toInt(),
                builder.build()
            )
        } catch (_: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun showCompletedNotification(
        context: Context,
        id: Long,
        filename: String,
        videoUri: String
    ) {
        createNotificationChannel(context)

        val openIntent = MediaStoreHelper.createViewIntent(context, videoUri)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (id * 2).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shareIntent = MediaStoreHelper.createShareIntent(context, videoUri)
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            (id * 2 + 1).toInt(),
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reel saved")
            .setContentText(filename)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open Reel", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel((NOTIFICATION_ID_BASE + id).toInt())
            notificationManager.notify((NOTIFICATION_ID_BASE + id).toInt(), builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun cancelNotification(context: Context, id: Long) {
        try {
            NotificationManagerCompat.from(context).cancel((NOTIFICATION_ID_BASE + id).toInt())
        } catch (_: Exception) {
            // Ignore
        }
    }
}
