package com.example.data.github

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

object GitHubStorageService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    data class UploadResult(
        val isImage: Boolean,
        val url: String
    )

    fun isConfigured(): Boolean {
        val token = BuildConfig.GITHUB_TOKEN
        val owner = BuildConfig.GITHUB_REPO_OWNER
        val repo = BuildConfig.GITHUB_REPO_NAME

        return token.isNotEmpty() && token != "YOUR_GITHUB_TOKEN_PLACEHOLDER" &&
               owner.isNotEmpty() && owner != "YOUR_GITHUB_USERNAME_PLACEHOLDER" &&
               repo.isNotEmpty() && repo != "YOUR_GITHUB_REPO_NAME_PLACEHOLDER"
    }

    suspend fun uploadMedia(context: Context, uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            throw IllegalStateException("GitHub configuration is missing. Please fill GITHUB_TOKEN, GITHUB_REPO_OWNER, and GITHUB_REPO_NAME in the Secrets Panel in AI Studio.")
        }

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val isImage = mimeType.startsWith("image/")
        val isVideo = mimeType.startsWith("video/")

        if (!isImage && !isVideo) {
            throw IllegalArgumentException("Selected file is neither a photo nor a video.")
        }

        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("3gp") -> "3gp"
            mimeType.contains("mov") -> "mov"
            isImage -> "jpg"
            else -> "mp4"
        }

        val fileName = "${UUID.randomUUID()}.$ext"
        
        val bytes = contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: throw IOException("Could not pull content from your device.")

        // Enforce 25MB maximum upload size
        val maxSizeBytes = 25 * 1024 * 1024
        if (bytes.size > maxSizeBytes) {
            throw IllegalArgumentException("Size limit exceeded. Videos and photos must be smaller than 25MB.")
        }

        val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val owner = BuildConfig.GITHUB_REPO_OWNER
        val repo = BuildConfig.GITHUB_REPO_NAME
        val token = BuildConfig.GITHUB_TOKEN
        val branch = if (BuildConfig.GITHUB_BRANCH.isNotEmpty()) BuildConfig.GITHUB_BRANCH else "main"

        val path = "chat_media/$fileName"
        val url = "https://api.github.com/repos/$owner/$repo/contents/$path"

        val json = JSONObject().apply {
            put("message", "Upload chat media: $fileName")
            put("content", base64Content)
            put("branch", branch)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader("Authorization", "token $token")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IOException("GitHub responded with an error (Code ${response.code}): $errorBody")
            }

            // Raw direct content URL
            val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/chat_media/$fileName"
            UploadResult(isImage = isImage, url = rawUrl)
        }
    }
}
