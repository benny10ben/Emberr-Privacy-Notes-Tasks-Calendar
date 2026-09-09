package com.ben.emberr.domain.ai.external

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

internal suspend fun throwForFailedResponse(response: HttpResponse, providerDisplayName: String): Nothing {
    val statusCode = response.status.value
    val bodySnippet = response.bodyAsText().take(300)

    when {
        statusCode == 401 || statusCode == 403 ->
            throw ExternalAiException.InvalidApiKey(providerDisplayName)

        statusCode == 429 -> {
            val retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.toIntOrNull()
            throw ExternalAiException.RateLimited(providerDisplayName, retryAfterSeconds)
        }

        statusCode == 413 || (statusCode == 400 && bodySnippet.looksLikeContextLengthError()) ->
            throw ExternalAiException.RequestTooLarge(providerDisplayName)

        statusCode in 500..599 ->
            throw ExternalAiException.ProviderUnavailable(providerDisplayName, statusCode)

        else ->
            throw ExternalAiException.ProviderError(providerDisplayName, statusCode, bodySnippet)
    }
}

private fun String.looksLikeContextLengthError(): Boolean {
    val lower = lowercase()
    return lower.contains("context_length_exceeded") ||
        lower.contains("maximum context length") ||
        lower.contains("too many tokens") ||
        lower.contains("input is too long") ||
        lower.contains("exceeds the maximum")
}
