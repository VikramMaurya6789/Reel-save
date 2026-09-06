package com.example

import com.example.domain.resolver.UrlValidator
import com.example.util.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelSaveUnitTest {

    @Test
    fun testValidReelUrls() {
        val validUrls = listOf(
            "https://www.instagram.com/reel/C8_ABC123/",
            "https://instagram.com/reel/C8_ABC123",
            "https://www.instagram.com/reels/C8_ABC123/",
            "https://instagram.com/reels/C8_ABC123",
            "https://www.instagram.com/p/C8_ABC123/",
            "http://www.instagram.com/reel/C8_ABC123/",
            "https://www.instagram.com/reel/C8_ABC123/?utm_source=ig_web_copy_link&igsh=ABC123xyz",
            "Check this out https://www.instagram.com/reel/C8_ABC123/ from IG!"
        )

        for (url in validUrls) {
            assertTrue("Expected URL to be valid: $url", UrlValidator.isValidReelUrl(url))
            val shortcode = UrlValidator.extractShortcode(url)
            assertEquals("C8_ABC123", shortcode)
        }
    }

    @Test
    fun testInvalidReelUrls() {
        val invalidUrls = listOf(
            "",
            "   ",
            "https://facebook.com/reel/123456",
            "https://www.tiktok.com/@user/video/123456",
            "https://www.google.com",
            "https://instagram.com/explore/",
            "https://instagram.com/direct/t/123",
            "file:///sdcard/download.mp4"
        )

        for (url in invalidUrls) {
            assertFalse("Expected URL to be invalid: $url", UrlValidator.isValidReelUrl(url))
            assertNull("Expected null shortcode for: $url", UrlValidator.extractShortcode(url))
        }
    }

    @Test
    fun testUrlNormalizationStripsTrackingParams() {
        val input = "https://www.instagram.com/reel/C8_ABC123/?utm_source=ig_web_copy_link&igsh=MzRlODBiNWFlZA=="
        val normalized = UrlValidator.normalizeUrl(input)
        assertEquals("https://www.instagram.com/reel/C8_ABC123/", normalized)
    }

    @Test
    fun testFilenameSanitization() {
        val dangerous = "user/name:with*illegal?chars\"and<brackets>|test"
        val sanitized = FileUtils.sanitizeFilename(dangerous)
        assertEquals("user_name_with_illegal_chars_and_brackets__test", sanitized)
    }

    @Test
    fun testFilenameGeneration() {
        val filenameWithUser = FileUtils.generateReelFilename("creativestudio", 1725624000000L)
        assertTrue(filenameWithUser.startsWith("reel_creativestudio_"))
        assertTrue(filenameWithUser.endsWith(".mp4"))

        val filenameWithoutUser = FileUtils.generateReelFilename(null, 1725624000000L)
        assertTrue(filenameWithoutUser.startsWith("reel_"))
        assertTrue(filenameWithoutUser.endsWith(".mp4"))
    }

    @Test
    fun testFileSizeFormatting() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("512 B", FileUtils.formatFileSize(512))
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
        assertEquals("10.0 MB", FileUtils.formatFileSize(10 * 1024 * 1024))
        assertEquals("1.5 GB", FileUtils.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("", FileUtils.formatDuration(null))
        assertEquals("", FileUtils.formatDuration(0))
        assertEquals("0:15", FileUtils.formatDuration(15))
        assertEquals("1:00", FileUtils.formatDuration(60))
        assertEquals("2:34", FileUtils.formatDuration(154))
    }
}
