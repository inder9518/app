package com.example.data.firebase

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChatNotificationService : Service() {
    private var listenerRegistration: ListenerRegistration? = null
    private val TAG = "ChatNotificationService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ChatNotificationService Created")
        createNotificationChannel()
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
            .setSmallIcon(android.R.drawable.stat_notify_chat)
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
            .setSmallIcon(android.R.drawable.stat_notify_chat)
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

    override fun onDestroy() {
        Log.d(TAG, "ChatNotificationService Destroyed")
        listenerRegistration?.remove()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
