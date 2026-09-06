package com.example.domain.resolver

import com.example.domain.model.ReelMetadata

class DefaultReelResolver(
    private val providers: List<ReelProvider> = listOf(
        PublicWebReelProvider()
    )
) : ReelResolver {

    override fun validateUrl(url: String): Boolean {
        return UrlValidator.isValidReelUrl(url)
    }

    override suspend fun resolveReel(url: String): Result<ReelMetadata> {
        val normalized = UrlValidator.normalizeUrl(url)
            ?: return Result.failure(IllegalArgumentException("Enter a valid Instagram Reel link."))

        for (provider in providers) {
            if (provider.canHandle(normalized)) {
                val result = provider.resolve(normalized)
                if (result.isSuccess) {
                    return result
                }
            }
        }

        return Result.failure(
            IllegalStateException("The Reel is unavailable or cannot be accessed.")
        )
    }
}
