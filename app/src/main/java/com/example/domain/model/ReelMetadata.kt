package com.example.domain.model

data class ReelFormat(
    val qualityLabel: String,
    val videoUrl: String,
    val resolution: String? = null,
    val approxBytes: Long? = null
)

data class ReelMetadata(
    val id: String,
    val originalUrl: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val username: String? = null,
    val caption: String? = null,
    val durationSeconds: Int? = null,
    val resolution: String? = null,
    val approxBytes: Long? = null,
    val formats: List<ReelFormat> = emptyList()
)
