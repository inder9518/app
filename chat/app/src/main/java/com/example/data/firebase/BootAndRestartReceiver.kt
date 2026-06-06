package com.example.data.firebase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootAndRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootAndRestartReceiver", "Received intent action: ${intent.action}")
        
        val prefs = context.getSharedPreferences("secret_chat_prefs", Context.MODE_PRIVATE)
        val myUniqueId = prefs.getString("unique_id", "") ?: ""
        val isAdmin = prefs.getBoolean("is_admin", false)
        
        if (myUniqueId.isNotEmpty() && !isAdmin) {
            try {
                val serviceIntent = Intent(context, ChatNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d("BootAndRestartReceiver", "Successfully restarted ChatNotificationService")
            } catch (e: Exception) {
                Log.e("BootAndRestartReceiver", "Failed to start service: ${e.message}")
            }
        }
    }
}
