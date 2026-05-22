package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.api.NewsApiService
import com.example.data.db.NewsDatabase
import com.example.data.repository.NewsRepository
import com.example.ui.screens.NewsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NewsViewModel
import com.example.ui.viewmodel.NewsViewModelFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

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

        // 0. Initialize Firestore/Firebase for secret chatting app
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

        // 1. Initialize Network client with robust logger and custom user agent
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // NewsAPI sometimes blocks standard Java requests. Providing a user agent prevents this.
                val request = chain.request().newBuilder()
                    .header("User-Agent", "NewsBharatApp/1.0.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        // 2. Initialize Moshi adapter parsing API responses
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        // 3. Setup Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val newsApiService = retrofit.create(NewsApiService::class.java)

        // 4. Setup Room Database & cache layers
        val database = NewsDatabase.getInstance(applicationContext)
        val newsDao = database.newsDao

        // 5. Get API Key injected securely via BuildConfig (.env via Secrets Gradle plugin)
        // Fallback to user provided key if BuildConfig returns empty string
        val apiKey = if (BuildConfig.NEWS_API_KEY.isNotEmpty() && !BuildConfig.NEWS_API_KEY.startsWith("YOUR_")) {
            BuildConfig.NEWS_API_KEY
        } else {
            "725bb3315cc545fc83730d89edc846bb" // Keep user provided key in prompt as safe fallback
        }

        // 6. Build repository
        val repository = NewsRepository(
            apiService = newsApiService,
            newsDao = newsDao,
            apiKey = apiKey
        )

        // 7. Instantiate ViewModel
        val viewModel = ViewModelProvider(
            this,
            NewsViewModelFactory(repository)
        )[NewsViewModel::class.java]

        // 8. Set Jetpack Compose View Surface
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NewsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
