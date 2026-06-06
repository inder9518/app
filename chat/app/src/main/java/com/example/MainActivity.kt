package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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

        // Launch background notification monitoring if logged in, requesting permission first if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                val permissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    Log.d("MainActivity", "Notification permission granted: $isGranted")
                    startBackgroundServiceIfLoggedIn()
                }
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startBackgroundServiceIfLoggedIn()
            }
        } else {
            startBackgroundServiceIfLoggedIn()
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

    private fun startBackgroundServiceIfLoggedIn() {
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
                Log.d("MainActivity", "ChatNotificationService started dynamically")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed starting notification service: ${e.message}")
            }
        }
    }
}
