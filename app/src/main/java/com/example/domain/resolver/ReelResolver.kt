package com.example.domain.resolver

import com.example.domain.model.ReelMetadata

interface ReelResolver {
    fun validateUrl(url: String): Boolean
    suspend fun resolveReel(url: String): Result<ReelMetadata>
}

interface ReelProvider {
    val name: String
    suspend fun canHandle(url: String): Boolean
    suspend fun resolve(url: String): Result<ReelMetadata>
}
