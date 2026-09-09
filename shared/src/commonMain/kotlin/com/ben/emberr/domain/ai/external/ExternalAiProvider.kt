package com.ben.emberr.domain.ai.external

enum class ExternalAiProvider(val displayName: String, val defaultModel: String?) {
    OPENAI("OpenAI", "gpt-4o-mini"),
    ANTHROPIC("Anthropic", "claude-3-5-haiku-latest"),
    GEMINI("Gemini", "gemini-2.0-flash"),
    CUSTOM("Custom", null)
}
