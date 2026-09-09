package com.ben.emberr.domain.ai.external

sealed class ExternalAiException(message: String) : Exception(message) {

    class NotConfigured(provider: String) : ExternalAiException(
        "No API key configured for $provider. Add one in AI Settings."
    )

    class InvalidApiKey(provider: String) : ExternalAiException(
        "Your $provider API key was rejected. Check it in AI Settings and try again."
    )

    class RateLimited(provider: String, retryAfterSeconds: Int?) : ExternalAiException(
        if (retryAfterSeconds != null)
            "$provider is rate-limiting requests. Try again in ${retryAfterSeconds}s."
        else
            "$provider is rate-limiting requests right now. Try again shortly."
    )

    class RequestTooLarge(provider: String) : ExternalAiException(
        "That request was too large for $provider. Try a shorter question, or fewer notes in context."
    )

    class ProviderUnavailable(provider: String, statusCode: Int) : ExternalAiException(
        "$provider is having issues right now (HTTP $statusCode). Try again in a moment, or switch to Local AI."
    )

    class ProviderError(provider: String, statusCode: Int, detail: String) : ExternalAiException(
        "$provider returned an error (HTTP $statusCode): $detail"
    )

    class TimedOut(provider: String) : ExternalAiException(
        "$provider took too long to respond. Try again."
    )

    class NoConnection(provider: String) : ExternalAiException(
        "Couldn't reach $provider — check your internet connection."
    )
}
