package com.example.domain.resolver

import com.example.domain.model.ReelFormat
import com.example.domain.model.ReelMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class BackendApiReelProvider(
    private val backendEndpoint: String? = null, // Configurable endpoint e.g. from BuildConfig or Settings
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : ReelProvider {

    override val name: String = "BackendApi"

    override suspend fun canHandle(url: String): Boolean {
        return !backendEndpoint.isNullOrBlank() && UrlValidator.isValidReelUrl(url)
    }

    override suspend fun resolve(url: String): Result<ReelMetadata> = withContext(Dispatchers.IO) {
        val endpoint = backendEndpoint
        if (endpoint.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No backend endpoint configured"))
        }

        try {
            val jsonBody = JSONObject().apply {
                put("url", url)
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("The download service is temporarily unavailable."))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(
                IOException("The download service is temporarily unavailable.")
            )

            val json = JSONObject(body)
            val success = json.optBoolean("success", false)
            if (!success) {
                return@withContext Result.failure(
                    IOException(json.optString("error", "The Reel is unavailable or cannot be accessed."))
                )
            }

            val shortcode = UrlValidator.extractShortcode(url) ?: "reel_${System.currentTimeMillis()}"
            val thumbnail = json.optString("thumbnail").takeIf { it.isNotBlank() }
            val username = json.optString("username").takeIf { it.isNotBlank() }
            val duration = if (json.has("duration")) json.optInt("duration") else null
            val videoUrl = json.optString("video_url").takeIf { it.isNotBlank() }
                ?: json.optString("videoUrl").takeIf { it.isNotBlank() }

            val formatsList = mutableListOf<ReelFormat>()
            val formatsArray = json.optJSONArray("formats")
            if (formatsArray != null) {
                for (i in 0 until formatsArray.length()) {
                    val fObj = formatsArray.getJSONObject(i)
                    formatsList.add(
                        ReelFormat(
                            qualityLabel = fObj.optString("quality", "Original"),
                            videoUrl = fObj.getString("url"),
                            resolution = fObj.optString("resolution").takeIf { it.isNotBlank() },
                            approxBytes = if (fObj.has("size")) fObj.optLong("size") else null
                        )
                    )
                }
            } else if (!videoUrl.isNullOrBlank()) {
                formatsList.add(ReelFormat("Original", videoUrl))
            }

            val chosenVideoUrl = formatsList.firstOrNull()?.videoUrl ?: videoUrl
                ?: return@withContext Result.failure(IOException("The Reel is unavailable or cannot be accessed."))

            Result.success(
                ReelMetadata(
                    id = shortcode,
                    originalUrl = url,
                    videoUrl = chosenVideoUrl,
                    thumbnailUrl = thumbnail,
                    username = username,
                    durationSeconds = duration,
                    formats = formatsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
