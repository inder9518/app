package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiGenerator {
    suspend fun generateSatire(topic: String, style: String): SatireResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            Log.w("GeminiGenerator", "API key is placeholder or empty. Rolling back to offline generator.")
            return@withContext runOfflineGenerator(topic, style)
        }

        val prompt = """
            Write a hilarious, highly professional satirical fake news story about the topic: "$topic".
            The chosen parody humor style is: "$style".
            
            Return the result strict in JSON format with exactly these fields:
            - title: A catchy, extremely professional-sounding news headline (e.g. "Cat Appointed CEO of major Bank").
            - body: A comprehensive 3-paragraph news report complete with funny quotes from experts.
            - category: One short humorous word or phrase (e.g. "Finance Satire", "Feline Takeover", "Pizza Emergency").
            - status: A witty 'fact check status' label (e.g., "Status: 110% Sarcastic", "Fact Check: Debunked by physics").
            
            Double check that you return ONLY valid JSON and no backticks or markdown markers.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.9f,
                responseMimeType = "application/json"
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a senior satirical journalist at a comedy award-winning fake news organization. You write hyper-realistic news structures that are laugh-out-loud funny."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse manually or using Moshi to be safe
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(SatireResponseJson::class.java)
                val responseJson = adapter.fromJson(jsonText)
                if (responseJson != null) {
                    return@withContext SatireResult(
                        title = responseJson.title ?: "Silly Event Shock",
                        body = responseJson.body ?: "Details remain mysterious.",
                        category = responseJson.category ?: "Satire",
                        status = responseJson.status ?: "100% PARODY",
                        isAiGenerated = true
                    )
                }
            }
            // Fallback parse if json starts with markdown or similar
            Log.e("GeminiGenerator", "Failed to parse JSON response. Text was: $jsonText")
            return@withContext runOfflineGenerator(topic, style)
        } catch (e: Exception) {
            Log.e("GeminiGenerator", "Gemini API call failed: ${e.localizedMessage}. Falling back.", e)
            return@withContext runOfflineGenerator(topic, style)
        }
    }

    private fun runOfflineGenerator(topic: String, style: String): SatireResult {
        // High quality offline helper implementing the requested theme
        val cleanTopic = topic.trim().ifEmpty { "Pajama Day in Office" }
        
        val funnyPostTitle = when (style.lowercase()) {
            "sarcastic" -> "Urgent: Studies Show $cleanTopic May Be Too Intelligent for Human Comprehension"
            "dramatic" -> "Breaking: World Paralyzed by Suddenly Popular '$cleanTopic' Craze"
            "conspiracy" -> "The Truth is Out: How '$cleanTopic' is Secretly Governed by Ancient Ferrets"
            else -> "Breaking News Bulletin: Global Consensus Reached Regarding '$cleanTopic'"
        }

        val bodyParagraphs = """
            In an unexpected shift in contemporary affairs, leaders from twelve major global coalitions have gathered in Paris to address the sudden rise of '$cleanTopic'. Citizens represent this trend as a 'monumental change of heart for society'. 
            
            'We did not see this coming,' remarked legendary researcher Dr. Oliver Gimmick, while organizing his spreadsheet of funny hats. 'Our sensors show that if this continues for another seventy-two hours, the density of pure amusement in the ozone layer will double.' Many advocates of the movement have declared that it has solved at least three small arguments regarding whose turn it was to wash the dishes.
            
            Critics claim that this is simply a distracter designed to keep people from noticing that cats have secretly discovered how to unlock high-capacity treat cupboards. Nonetheless, grocery stores report that sales of related commodities have increased by a whopping 700% as humanity prepares for the official '$cleanTopic' celebration.
        """.trimIndent()

        val category = "Offline ${style.replaceFirstChar { it.uppercase() }}"
        val status = "LOCAL GENERATION - 100% Mock"

        return SatireResult(
            title = funnyPostTitle,
            body = bodyParagraphs,
            category = category,
            status = status,
            isAiGenerated = false
        )
    }
}

@JsonClass(generateAdapter = true)
data class SatireResponseJson(
    val title: String?,
    val body: String?,
    val category: String?,
    val status: String?
)

data class SatireResult(
    val title: String,
    val body: String,
    val category: String,
    val status: String,
    val isAiGenerated: Boolean
)
