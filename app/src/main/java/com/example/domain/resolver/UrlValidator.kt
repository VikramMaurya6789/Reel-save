package com.example.domain.resolver

import java.net.URI

object UrlValidator {

    private val INSTAGRAM_HOST_REGEX = Regex(
        "^(www\\.)?instagram\\.com$",
        RegexOption.IGNORE_CASE
    )

    private val REEL_PATH_REGEX = Regex(
        "^/(reel|reels|p)/([A-Za-z0-9_-]+)/?.*$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Validates whether the given string is a valid Instagram Reel URL.
     */
    fun isValidReelUrl(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        val normalized = normalizeUrl(input) ?: return false

        return try {
            val uri = URI(normalized)
            val host = uri.host ?: return false
            if (!INSTAGRAM_HOST_REGEX.matches(host)) return false

            val path = uri.path ?: return false
            REEL_PATH_REGEX.matches(path)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts the shortcode (e.g. "C8_ABC123") from a supported Reel URL.
     */
    fun extractShortcode(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeUrl(input) ?: return null

        return try {
            val uri = URI(normalized)
            val path = uri.path ?: return null
            val match = REEL_PATH_REGEX.matchEntire(path)
            match?.groupValues?.get(2)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Normalizes input: ensures https:// scheme, strips tracking query parameters,
     * extracts URL if input contains leading or trailing text from share sheets.
     */
    fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // If the shared text contains extra words (e.g., "Check out this reel https://www.instagram.com/reel/XYZ"),
        // extract the actual URL
        val urlRegex = Regex("https?://[\\w.-]*instagram\\.com/(reel|reels|p)/[A-Za-z0-9_-]+/?(\\?[^\\s]*)?")
        val foundMatch = urlRegex.find(trimmed)?.value ?: trimmed

        return try {
            var raw = foundMatch
            if (raw.startsWith("http://", ignoreCase = true)) {
                raw = "https://" + raw.substring(7)
            } else if (!raw.startsWith("https://", ignoreCase = true)) {
                raw = "https://$raw"
            }

            val uri = URI(raw)
            val host = uri.host?.lowercase() ?: return null
            if (!INSTAGRAM_HOST_REGEX.matches(host)) return null

            val path = uri.path ?: return null
            val match = REEL_PATH_REGEX.matchEntire(path) ?: return null
            val shortcode = match.groupValues[2]

            // Clean, normalized canonical Reel URL with tracking parameters stripped
            "https://www.instagram.com/reel/$shortcode/"
        } catch (_: Exception) {
            null
        }
    }
}
