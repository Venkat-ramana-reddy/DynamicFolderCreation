package com.imageserver

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ImageServerService : Service() {

    private var server: HttpServer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val folderUriStr = intent?.getStringExtra(EXTRA_FOLDER_URI)
        val port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080
        activePort = port

        server?.stop()
        server = HttpServer(port, applicationContext).also { s ->
            folderUriStr?.let { s.folderUri = Uri.parse(it) }
        }

        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        try {
            server!!.start()
            isRunning = true
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Image Server", NotificationManager.IMPORTANCE_LOW)
                    .also { it.description = "Local HTTP image server is running" }
            )
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_server)
            .setContentTitle("Image Server Active")
            .setContentText("Serving on port $activePort  •  tap to open")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID    = "img_server"
        const val NOTIF_ID      = 101
        const val EXTRA_FOLDER_URI = "folder_uri"
        const val EXTRA_PORT    = "port"

        @Volatile var isRunning  = false
        @Volatile var activePort = 8080
    }
}
