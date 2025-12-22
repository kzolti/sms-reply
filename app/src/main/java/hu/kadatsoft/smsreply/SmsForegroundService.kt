package hu.kadatsoft.smsreply

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SmsForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "SmsReplyChannelPersistent"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_DISMISSED = "ACTION_DISMISSED"
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("SmsService", "Service onCreate", this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                AppLogger.i("SmsService", "Action START received", this)
                ServiceState.setRunning(true)
                try {
                    val notification = createNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    AppLogger.e("SmsService", "Error starting foreground", e, this)
                }
            }
            ACTION_STOP -> {
                AppLogger.i("SmsService", "Action STOP received", this)
                ServiceState.setRunning(false)
                // Stop foreground and remove notification
                stopForeground(STOP_FOREGROUND_REMOVE)
                // Stop the service completely
                stopSelf()
            }
            ACTION_DISMISSED -> {
                AppLogger.i("SmsService", "Notification dismissed by user, restoring...", this)
                if (ServiceState.isServiceRunning) {
                    val notification = createNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
            }
            else -> {
                // Default start (e.g. from App launch or restart)
                AppLogger.i("SmsService", "Service started with default action: ${intent?.action}", this)
                ServiceState.setRunning(true)
                try {
                    val notification = createNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    AppLogger.e("SmsService", "Error in default start", e, this)
                }
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i("SmsService", "Service onDestroy", this)
        ServiceState.setRunning(false)
        // Make sure notification is removed
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        // Also manually cancel the notification to be extra sure
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }



    private fun createNotification(): Notification {
        val isRunning = ServiceState.isServiceRunning
        
        val statusText = if (isRunning) getString(R.string.notification_active) else getString(R.string.notification_stopped)
        val icon = R.drawable.ic_stat_running
        val actionTitle = if (isRunning) getString(R.string.action_stop) else getString(R.string.action_start)
        val actionIntentAction = if (isRunning) ACTION_STOP else ACTION_START

        // Action Button Intent
        val actionIntent = Intent(this, SmsForegroundService::class.java).apply {
            action = actionIntentAction
        }
        val actionPendingIntent = PendingIntent.getService(
            this, 
            0, 
            actionIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Open App Intent
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Dismissed Intent
        val deleteIntent = Intent(this, SmsForegroundService::class.java).apply {
            action = ACTION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setSmallIcon(icon)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setOngoing(true) // Persistent
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, 
                actionTitle, 
                actionPendingIntent
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
            
        // Explicitly set flags to prevent dismissal on some device manufacturers
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        
        return notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH 
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
