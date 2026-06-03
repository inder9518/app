package com.example.data.firebase

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChatNotificationService : Service() {
    private var listenerRegistration: ListenerRegistration? = null
    private val TAG = "ChatNotificationService"
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
         Log.d(TAG, "ChatNotificationService Created")
        createNotificationChannel()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SecureChat::MessageListenerWakeLock")
            wakeLock?.acquire(30 * 60 * 1000L) // Safe partial wake lock for continuous listening when network/CPU is asleep
            Log.d(TAG, "WakeLock acquired successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ChatNotificationService Started")
        
        // Standard notification channel for background service
        val notificationChannelId = "CHAT_SERVICE_CHANNEL"
        
        val intentLaunch = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intentLaunch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Secure chat service is active")
            .setContentText("Listening for new messages...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .build()
        
        startForeground(9999, notification)

        startMessageListener()

        return START_STICKY
    }

    private fun startMessageListener() {
        listenerRegistration?.remove()

        val prefs = getSharedPreferences("secret_chat_prefs", Context.MODE_PRIVATE)
        val myUniqueId = prefs.getString("unique_id", "") ?: ""
        if (myUniqueId.isEmpty() || myUniqueId.lowercase() == "admin") {
            Log.d(TAG, "No logged in user, stopping listener.")
            return
        }

        val startTime = System.currentTimeMillis()

        try {
            val db = FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("messages")
                .whereEqualTo("receiverId", myUniqueId)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        Log.e(TAG, "Listen failed: $exception")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED) {
                                val doc = dc.document
                                val timestamp = doc.getLong("timestamp") ?: 0L
                                val senderId = doc.getString("senderId") ?: ""
                                val text = doc.getString("text") ?: ""
                                
                                if (timestamp > startTime && senderId != myUniqueId) {
                                    handleIncomingMessage(senderId, text)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listener: ${e.message}")
        }
    }

    private fun handleIncomingMessage(senderId: String, text: String) {
        // If app is currently in foreground/active, do not trigger system notification
        if (com.example.MainActivity.isAppInForeground) {
            Log.d(TAG, "App is in foreground. Skipping system notification.")
            return
        }

        val prefs = getSharedPreferences("secret_chat_prefs", Context.MODE_PRIVATE)
        
        // Global mute or individual mute check
        val isAllMuted = prefs.getBoolean("mute_all", false)
        val isSenderMuted = prefs.getBoolean("mute_$senderId", false)

        if (isAllMuted || isSenderMuted) {
            Log.d(TAG, "Muted chat alert for sender: $senderId. No notification shown.")
            return
        }

        showMessageNotification(senderId, text)
    }

    private fun showMessageNotification(senderId: String, text: String) {
        val notificationChannelId = "NEW_MESSAGE_CHANNEL"
        
        val intentLaunch = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            senderId.hashCode(),
            intentLaunch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(senderId)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "New Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for new secure messages"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(senderId.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channel = NotificationChannel(
                "CHAT_SERVICE_CHANNEL",
                "Background Chat Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Service monitoring for background incoming chats"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved called - app swiped away. Scheduling restart broadcast...")
        
        // Broadcast to trigger resurrection receiver immediately after task is swiped
        val restartIntent = Intent("com.example.chat.RESTART_SERVICE").apply {
            setPackage(packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            999,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 500, // Trigger in 500ms
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule AlarmManager service restart: ${e.message}")
        }
        
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d(TAG, "ChatNotificationService Destroyed")
        listenerRegistration?.remove()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release WakeLock: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
