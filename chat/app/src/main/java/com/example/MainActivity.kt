package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.SecretChatMainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    companion object {
        var isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firestore/Firebase for secret chatting app
        com.example.data.firebase.FirestoreService.initialize(applicationContext)

        // Launch background notification monitoring if logged in
        val prefs = getSharedPreferences("secret_chat_prefs", MODE_PRIVATE)
        val activeUid = prefs.getString("unique_id", "") ?: ""
        if (activeUid.isNotEmpty() && activeUid.lowercase() != "admin") {
            try {
                val intentService = android.content.Intent(this, com.example.data.firebase.ChatNotificationService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intentService)
                } else {
                    startService(intentService)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed starting notification service: ${e.message}")
            }
        }

        // Set Jetpack Compose View Surface
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecretChatMainScreen()
                }
            }
        }
    }
}
