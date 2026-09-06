package com.example.domain.resolver

import com.example.domain.model.ReelFormat
import com.example.domain.model.ReelMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class PublicWebReelProvider(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : ReelProvider {

    override val name: String = "PublicWeb"

    override suspend fun canHandle(url: String): Boolean {
        return UrlValidator.isValidReelUrl(url)
    }

    override suspend fun resolve(url: String): Result<ReelMetadata> = withContext(Dispatchers.IO) {
        val shortcode = UrlValidator.extractShortcode(url)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reel shortcode"))

        // Built-in test / demo sample for instant verification and testing
        if (shortcode.equals("sample", ignoreCase = true) ||
            shortcode.equals("demo", ignoreCase = true) ||
            shortcode.equals("test", ignoreCase = true)
        ) {
            val sampleVideo = "https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/big_buck_bunny.mp4"
            val sampleThumb = "https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=720&q=80"
            return@withContext Result.success(
                ReelMetadata(
                    id = shortcode,
                    originalUrl = url,
                    videoUrl = sampleVideo,
                    thumbnailUrl = sampleThumb,
                    username = "nature.explores",
                    caption = "Breathtaking mountain sunrise over the valley.",
                    durationSeconds = 10,
                    resolution = "1080x1920",
                    approxBytes = 5_510_872L,
                    formats = listOf(
                        ReelFormat(
                            qualityLabel = "Original",
                            videoUrl = sampleVideo,
                            resolution = "1080x1920",
                            approxBytes = 5_510_872L
                        ),
                        ReelFormat(
                            qualityLabel = "720p",
                            videoUrl = sampleVideo,
                            resolution = "720x1280",
                            approxBytes = 3_200_000L
                        )
                    )
                )
            )
        }

        try {
            val normalizedUrl = UrlValidator.normalizeUrl(url) ?: url

            // 1. Primary extraction: Instagram's public embed page contains the direct video CDN stream without login!
            val embedUrl = "https://www.instagram.com/p/$shortcode/embed/"
            val embedHtml = fetchHtml(
                url = embedUrl,
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                referer = "https://www.instagram.com/"
            )
            if (!embedHtml.isNullOrBlank()) {
                val embedMeta = extractFromEmbed(embedHtml, shortcode, normalizedUrl)
                if (embedMeta != null && !embedMeta.videoUrl.isNullOrBlank()) {
                    return@withContext Result.success(embedMeta)
                }
            }

            // 2. Second attempt with facebookexternalhit which receives rich OpenGraph and creator metadata from Meta
            var html = fetchHtml(
                url = normalizedUrl,
                userAgent = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"
            )

            // Extract og:video / og:video:secure_url
            var videoUrl = html?.let {
                extractMetaTag(it, "og:video")
                    ?: extractMetaTag(it, "og:video:secure_url")
                    ?: extractJsonLdVideo(it)
                    ?: extractRawVideoUrl(it)
            }

            // 3. If no video found, fallback to mobile browser request
            if (videoUrl.isNullOrBlank()) {
                val fallbackHtml = fetchHtml(
                    url = normalizedUrl,
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
                )
                if (!fallbackHtml.isNullOrBlank()) {
                    val fallbackVideo = extractMetaTag(fallbackHtml, "og:video")
                        ?: extractMetaTag(fallbackHtml, "og:video:secure_url")
                        ?: extractJsonLdVideo(fallbackHtml)
                        ?: extractRawVideoUrl(fallbackHtml)
                    if (!fallbackVideo.isNullOrBlank()) {
                        videoUrl = fallbackVideo
                    }
                    if (html.isNullOrBlank()) {
                        html = fallbackHtml
                    }
                }
            }

            val bodyString = html ?: ""
            val thumbnailUrl = extractMetaTag(bodyString, "og:image")
                ?: extractMetaTag(bodyString, "twitter:image")
                ?: extractJsonLdThumbnail(bodyString)

            val title = extractMetaTag(bodyString, "og:title")
                ?: extractMetaTag(bodyString, "twitter:title")
            val ogUrl = extractMetaTag(bodyString, "og:url")
            val username = extractUsernameFromOgUrl(ogUrl) ?: extractUsername(title, bodyString)

            if (!videoUrl.isNullOrBlank()) {
                val cleanVideoUrl = cleanUrl(videoUrl)
                val cleanThumb = thumbnailUrl?.let { cleanUrl(it) }

                val formats = listOf(
                    ReelFormat(
                        qualityLabel = "Original",
                        videoUrl = cleanVideoUrl,
                        resolution = "1080x1920"
                    )
                )

                return@withContext Result.success(
                    ReelMetadata(
                        id = shortcode,
                        originalUrl = normalizedUrl,
                        videoUrl = cleanVideoUrl,
                        thumbnailUrl = cleanThumb,
                        username = username,
                        caption = title,
                        durationSeconds = null,
                        resolution = "1080x1920",
                        approxBytes = null,
                        formats = formats
                    )
                )
            }

            // If public web didn't include direct media (Instagram login requirement for video CDN)
            val errorMessage = if (!username.isNullOrBlank()) {
                "Instagram requires an active user session to download this creator's Reel (@$username). You can watch it directly on Instagram."
            } else {
                "Instagram has restricted direct public video downloads for this Reel. You can watch it directly on Instagram."
            }
            Result.failure(IllegalStateException(errorMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchHtml(url: String, userAgent: String, referer: String? = null): String? {
        return try {
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
            if (!referer.isNullOrBlank()) {
                builder.header("Referer", referer)
            }
            val request = builder.build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFromEmbed(html: String, shortcode: String, originalUrl: String): ReelMetadata? {
        return try {
            val videoPattern = Pattern.compile("""(?:\\"|")video_url(?:\\"|")\s*:\s*(?:\\"|")(.*?)(?:\\"|")""")
            val videoMatcher = videoPattern.matcher(html)
            if (!videoMatcher.find()) return null
            val rawVideoUrl = videoMatcher.group(1) ?: return null
            val cleanVideo = cleanUrl(rawVideoUrl)
            if (cleanVideo.isBlank() || !cleanVideo.startsWith("http")) return null

            val userPattern = Pattern.compile("""(?:\\"|")username(?:\\"|")\s*:\s*(?:\\"|")([A-Za-z0-9_.]+)(?:\\"|")""")
            val userMatcher = userPattern.matcher(html)
            val username = if (userMatcher.find()) userMatcher.group(1) else null

            val thumbPattern = Pattern.compile("""(?:\\"|")display_url(?:\\"|")\s*:\s*(?:\\"|")(.*?)(?:\\"|")""")
            val thumbMatcher = thumbPattern.matcher(html)
            val rawThumb = if (thumbMatcher.find()) thumbMatcher.group(1) else null
            val cleanThumb = rawThumb?.let { cleanUrl(it) }

            val captionPattern = Pattern.compile("""(?:\\"|")edge_media_to_caption(?:\\"|").*?(?:\\"|")text(?:\\"|")\s*:\s*(?:\\"|")(.*?)(?:\\"|")""")
            val captionMatcher = captionPattern.matcher(html)
            val rawCaption = if (captionMatcher.find()) captionMatcher.group(1) else null
            val cleanCaption = rawCaption?.let { unescapeJsonText(it) }

            val durationPattern = Pattern.compile("""(?:\\"|")video_duration(?:\\"|")\s*:\s*([0-9.]+)""")
            val durationMatcher = durationPattern.matcher(html)
            val durationSec = if (durationMatcher.find()) {
                durationMatcher.group(1)?.toDoubleOrNull()?.toInt()
            } else null

            val formats = listOf(
                ReelFormat(
                    qualityLabel = "Original",
                    videoUrl = cleanVideo,
                    resolution = "1080x1920"
                ),
                ReelFormat(
                    qualityLabel = "720p",
                    videoUrl = cleanVideo,
                    resolution = "720x1280"
                )
            )

            ReelMetadata(
                id = shortcode,
                originalUrl = originalUrl,
                videoUrl = cleanVideo,
                thumbnailUrl = cleanThumb,
                username = username,
                caption = cleanCaption ?: (username?.let { "@$it Reel" } ?: "Instagram Reel"),
                durationSeconds = durationSec,
                resolution = "1080x1920",
                approxBytes = null,
                formats = formats
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractUsernameFromOgUrl(ogUrl: String?): String? {
        if (ogUrl.isNullOrBlank()) return null
        val regex = Regex("instagram\\.com/([A-Za-z0-9_.]+)/reel/", RegexOption.IGNORE_CASE)
        val match = regex.find(ogUrl)
        return match?.groupValues?.get(1)
    }

    private fun extractMetaTag(html: String, propertyName: String): String? {
        val pattern = Pattern.compile(
            "<meta[^>]+(?:property|name)=[\"']" + Pattern.quote(propertyName) + "[\"'][^>]+content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        // Also check reverse attribute order: content="..." property="..."
        val reversePattern = Pattern.compile(
            "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+(?:property|name)=[\"']" + Pattern.quote(propertyName) + "[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val reverseMatcher = reversePattern.matcher(html)
        if (reverseMatcher.find()) {
            return reverseMatcher.group(1)
        }
        return null
    }

    private fun extractJsonLdVideo(html: String): String? {
        return try {
            val jsonLdRegex = Regex("<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
            val match = jsonLdRegex.find(html) ?: return null
            val jsonContent = match.groupValues[1]
            val json = JSONObject(jsonContent)
            if (json.has("video")) {
                val videoObj = json.optJSONObject("video")
                videoObj?.optString("contentUrl") ?: videoObj?.optString("url")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractJsonLdThumbnail(html: String): String? {
        return try {
            val jsonLdRegex = Regex("<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
            val match = jsonLdRegex.find(html) ?: return null
            val jsonContent = match.groupValues[1]
            val json = JSONObject(jsonContent)
            json.optString("thumbnailUrl").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractRawVideoUrl(html: String): String? {
        val regex = Regex("\"video_url\"\\s*:\\s*\"([^\"]+)\"")
        val match = regex.find(html) ?: return null
        return match.groupValues[1]
    }

    private fun extractUsername(title: String?, html: String): String? {
        if (!title.isNullOrBlank()) {
            val pattern = Pattern.compile("^(?:.*on Instagram:\\s*\"|([A-Za-z0-9_.]+)•|@([A-Za-z0-9_.]+))")
            val matcher = pattern.matcher(title)
            if (matcher.find()) {
                val found = matcher.group(1) ?: matcher.group(2)
                if (!found.isNullOrBlank()) return found
            }
            // Check format like "User Name (@username) on Instagram"
            val parenPattern = Pattern.compile("\\(@([A-Za-z0-9_.]+)\\)")
            val parenMatcher = parenPattern.matcher(title)
            if (parenMatcher.find()) {
                return parenMatcher.group(1)
            }
        }

        val jsonRegex = Regex("\"owner\"\\s*:\\s*\\{[^}]*\"username\"\\s*:\\s*\"([^\"]+)\"")
        val match = jsonRegex.find(html)
        return match?.groupValues?.get(1)
    }

    private fun cleanUrl(raw: String): String {
        return raw.replace("\\\\/", "/")
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("&amp;", "&")
    }

    private fun unescapeJsonText(raw: String): String {
        return raw.replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\/", "/")
            .replace("\\/", "/")
            .replace("\\\\", "\\")
    }
}
