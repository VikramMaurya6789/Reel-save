package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private val ILLEGAL_CHARS = Regex("[/\\\\:*?\"<>«»|]")

    /**
     * Sanitizes a string for safe use in file names on all Android file systems.
     */
    fun sanitizeFilename(input: String): String {
        val sanitized = input.replace(ILLEGAL_CHARS, "_").trim()
        return sanitized.ifEmpty { "reel" }
    }

    /**
     * Generates a standard filename according to specifications:
     * reel_username_yyyyMMdd_HHmm.mp4 or reel_yyyyMMdd_HHmm.mp4
     */
    fun generateReelFilename(username: String?, timestamp: Long = System.currentTimeMillis()): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        val formattedDate = dateFormat.format(Date(timestamp))

        val cleanUser = username?.let { sanitizeFilename(it) }?.takeIf { it.isNotBlank() }
        return if (cleanUser != null) {
            "reel_${cleanUser}_$formattedDate.mp4"
        } else {
            "reel_$formattedDate.mp4"
        }
    }

    /**
     * Formats bytes to human-readable string (e.g. 8.4 MB, 540 KB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Formats speed in bytes/sec to human readable format (e.g. 1.8 MB/s).
     */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return ""
        return "${formatFileSize(bytesPerSec)}/s"
    }

    /**
     * Formats seconds to mm:ss format.
     */
    fun formatDuration(seconds: Int?): String {
        if (seconds == null || seconds <= 0) return ""
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }
}
