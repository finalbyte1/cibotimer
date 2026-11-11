package com.cibotimer.data.models

data class ProviderPreset(
    val name: String,
    val baseUrl: String,
    val sampleModel: String,
    val description: String
)

object ProviderPresets {
    val all: List<ProviderPreset> = listOf(
        ProviderPreset(
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            sampleModel = "gpt-4o-mini",
            description = "API Ufficiale OpenAI (gpt-4o, gpt-4o-mini, o3-mini)"
        ),
        ProviderPreset(
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            sampleModel = "deepseek-chat",
            description = "DeepSeek V3 e DeepSeek R1 (reasoning)"
        ),
        ProviderPreset(
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            sampleModel = "openrouter/auto",
            description = "Aggregatore universale multi-modello"
        ),
        ProviderPreset(
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            sampleModel = "llama-3.3-70b-versatile",
            description = "Inference ad altissima velocità"
        ),
        ProviderPreset(
            name = "Gemini (OpenAI Shim)",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            sampleModel = "gemini-1.5-flash",
            description = "Google Gemini via endpoint OpenAI-compatibile"
        ),
        ProviderPreset(
            name = "Mistral AI",
            baseUrl = "https://api.mistral.ai/v1",
            sampleModel = "mistral-small-latest",
            description = "Modelli Mistral e Codestral"
        ),
        ProviderPreset(
            name = "Together AI",
            baseUrl = "https://api.together.xyz/v1",
            sampleModel = "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo",
            description = "Hosting modelli open-source ad alte prestazioni"
        ),
        ProviderPreset(
            name = "Ollama (Locale)",
            baseUrl = "http://10.0.2.2:11434/v1",
            sampleModel = "llama3.2",
            description = "Server Ollama locale (10.0.2.2 o IP LAN)"
        ),
        ProviderPreset(
            name = "LM Studio (Locale)",
            baseUrl = "http://10.0.2.2:1234/v1",
            sampleModel = "local-model",
            description = "Server LM Studio in locale"
        ),
        ProviderPreset(
            name = "Personalizzato",
            baseUrl = "https://",
            sampleModel = "",
            description = "Qualsiasi endpoint compatibile OpenAI"
        )
    )
}
