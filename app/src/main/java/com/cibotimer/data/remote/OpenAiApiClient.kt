package com.cibotimer.data.remote

import com.cibotimer.data.models.AiConfig
import com.cibotimer.data.models.ChatMessage
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class StreamChunk(
    val contentDelta: String? = null,
    val thinkingDelta: String? = null,
    val isDone: Boolean = false
)

class OpenAiApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) {

    /**
     * Non-streaming API call (backward-compatible fallback).
     */
    suspend fun sendMessage(
        config: AiConfig,
        conversationHistory: List<ChatMessage>,
        userMessage: String
    ): Result<Pair<String, FastingPlan?>> = withContext(Dispatchers.IO) {
        if (!config.isEnabled) {
            return@withContext Result.failure(IllegalStateException("L'assistente AI è disabilitato nelle impostazioni."))
        }
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Chiave API mancante. Inseriscila nelle impostazioni AI."))
        }

        try {
            val messagesArray = buildMessagesArray(config.systemPrompt, conversationHistory, userMessage)

            val requestJson = buildJsonObject {
                put("model", JsonPrimitive(config.modelName.ifBlank { "gpt-4o-mini" }))
                put("messages", messagesArray)
                put("temperature", JsonPrimitive(0.7))
                put("stream", JsonPrimitive(false))
            }

            val url = resolveChatUrl(config.baseUrl)
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.apiKey.trim()}")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyString)
                    return@withContext Result.failure(IOException("Errore API (${response.code}): $errorMsg"))
                }

                val responseJson = json.parseToJsonElement(bodyString).jsonObject
                val choices = responseJson["choices"]?.jsonArray
                val firstChoice = choices?.firstOrNull()?.jsonObject
                val assistantContent = firstChoice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                    ?: "Nessuna risposta ricevuta."

                val extractedPlan = extractPlanFromJsonBlock(assistantContent)
                Result.success(Pair(assistantContent, extractedPlan))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Server-Sent Events (SSE) streaming API call.
     * Emits token deltas in real time. Handles both standard content and reasoning_content (DeepSeek R1 / o1).
     */
    fun sendMessageStream(
        config: AiConfig,
        conversationHistory: List<ChatMessage>,
        userMessage: String
    ): Flow<StreamChunk> = flow {
        if (!config.isEnabled) {
            throw IllegalStateException("L'assistente AI è disabilitato nelle impostazioni.")
        }
        if (config.apiKey.isBlank()) {
            throw IllegalStateException("Chiave API mancante. Inseriscila nelle impostazioni AI.")
        }

        val messagesArray = buildMessagesArray(config.systemPrompt, conversationHistory, userMessage)

        val requestJson = buildJsonObject {
            put("model", JsonPrimitive(config.modelName.ifBlank { "gpt-4o-mini" }))
            put("messages", messagesArray)
            put("temperature", JsonPrimitive(0.7))
            put("stream", JsonPrimitive(true))
        }

        val url = resolveChatUrl(config.baseUrl)
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        call.execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                val errorMsg = parseErrorMessage(errorBody)
                throw IOException("Errore API (${response.code}): $errorMsg")
            }

            val source = response.body?.byteStream() ?: throw IOException("Corpo risposta vuoto")
            source.bufferedReader(Charsets.UTF_8).use { reader ->
                var insideThinkTag = false

                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) continue
                    if (!line.startsWith("data:")) continue

                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") {
                        emit(StreamChunk(isDone = true))
                        break
                    }
                    if (payload.isEmpty()) continue

                    try {
                        val element = json.parseToJsonElement(payload).jsonObject
                        val choices = element["choices"]?.jsonArray
                        val firstChoice = choices?.firstOrNull()?.jsonObject
                        val delta = firstChoice?.get("delta")?.jsonObject ?: continue

                        // Check reasoning_content (DeepSeek R1 / thinking models)
                        val reasoning = delta["reasoning_content"]?.jsonPrimitive?.contentOrNull
                        if (!reasoning.isNullOrEmpty()) {
                            emit(StreamChunk(thinkingDelta = reasoning))
                        }

                        // Check standard content
                        val content = delta["content"]?.jsonPrimitive?.contentOrNull
                        if (!content.isNullOrEmpty()) {
                            // Detect inline <think>...</think> blocks
                            if (content.contains("<think>")) {
                                insideThinkTag = true
                                val parts = content.split("<think>")
                                if (parts[0].isNotEmpty()) emit(StreamChunk(contentDelta = parts[0]))
                                if (parts.size > 1) {
                                    val afterThink = parts[1]
                                    if (afterThink.contains("</think>")) {
                                        insideThinkTag = false
                                        val subParts = afterThink.split("</think>")
                                        if (subParts[0].isNotEmpty()) emit(StreamChunk(thinkingDelta = subParts[0]))
                                        if (subParts.size > 1 && subParts[1].isNotEmpty()) emit(StreamChunk(contentDelta = subParts[1]))
                                    } else {
                                        emit(StreamChunk(thinkingDelta = afterThink))
                                    }
                                }
                            } else if (insideThinkTag) {
                                if (content.contains("</think>")) {
                                    insideThinkTag = false
                                    val parts = content.split("</think>")
                                    if (parts[0].isNotEmpty()) emit(StreamChunk(thinkingDelta = parts[0]))
                                    if (parts.size > 1 && parts[1].isNotEmpty()) emit(StreamChunk(contentDelta = parts[1]))
                                } else {
                                    emit(StreamChunk(thinkingDelta = content))
                                }
                            } else {
                                emit(StreamChunk(contentDelta = content))
                            }
                        }
                    } catch (e: Exception) {
                        // ignore malformed SSE chunk and continue
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Queries /models endpoint to retrieve available models from the provider.
     */
    suspend fun fetchAvailableModels(config: AiConfig): Result<List<String>> = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Chiave API mancante"))
        }

        try {
            val cleanBase = config.baseUrl.trim().removeSuffix("/").removeSuffix("/chat/completions")
            val url = "$cleanBase/models"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.apiKey.trim()}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${parseErrorMessage(body)}"))
                }

                val models = try {
                    val root = json.parseToJsonElement(body).jsonObject
                    val data = root["data"]?.jsonArray
                    data?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content } ?: emptyList()
                } catch (e: Exception) {
                    Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.toList()
                }

                Result.success(models.distinct().sorted())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveChatUrl(baseUrl: String): String {
        val clean = baseUrl.trim().removeSuffix("/")
        return if (clean.endsWith("/chat/completions")) {
            clean
        } else {
            "$clean/chat/completions"
        }
    }

    private fun buildMessagesArray(
        systemPrompt: String,
        conversationHistory: List<ChatMessage>,
        userMessage: String
    ): JsonArray = buildJsonArray {
        // System message
        add(buildJsonObject {
            put("role", JsonPrimitive("system"))
            put("content", JsonPrimitive(systemPrompt))
        })

        // Recent conversation context (last 12 messages)
        val recentHistory = conversationHistory.takeLast(12)
        for (msg in recentHistory) {
            val role = when (msg.sender) {
                MessageSender.USER -> "user"
                MessageSender.AI -> "assistant"
                MessageSender.SYSTEM -> "system"
            }
            add(buildJsonObject {
                put("role", JsonPrimitive(role))
                put("content", JsonPrimitive(msg.text))
            })
        }

        // Current user message
        add(buildJsonObject {
            put("role", JsonPrimitive("user"))
            put("content", JsonPrimitive(userMessage))
        })
    }

    private fun parseErrorMessage(body: String): String {
        return try {
            val parsed = json.parseToJsonElement(body).jsonObject
            parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: body
        } catch (e: Exception) {
            body.take(300)
        }
    }

    fun extractPlanFromJsonBlock(aiText: String): FastingPlan? {
        return Companion.extractPlanFromJsonBlock(aiText, json)
    }

    companion object {
        private val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        /**
         * Estrae la stringa JSON del piano sia da blocchi markdown (```fasting_plan o ```json o ```)
         * sia da JSON grezzo inline nel messaggio.
         */
        fun extractJsonCandidate(aiText: String): String? {
            // 1. Cerca nei blocchi di codice markdown (```fasting_plan ... ``` o ```json ... ``` o ``` ... ```)
            val fenceRegex = Regex("""```(?:fasting_plan|json)?\s*([\s\S]*?)```""", RegexOption.MULTILINE)
            val fenceMatches = fenceRegex.findAll(aiText)
            for (m in fenceMatches) {
                val content = m.groupValues[1].trim()
                val firstBrace = content.indexOf('{')
                val lastBrace = content.lastIndexOf('}')
                if (firstBrace != -1 && lastBrace > firstBrace) {
                    val candidate = content.substring(firstBrace, lastBrace + 1)
                    if (candidate.contains("\"days\"") || candidate.contains("dayOfWeek") || candidate.contains("fastingDurationMinutes")) {
                        return candidate
                    }
                }
            }

            // 2. Fallback: Cerca l'oggetto JSON nel testo grezzo analizzando le parentesi bilanciate
            val markerIndex = aiText.indexOf("\"days\"").takeIf { it != -1 }
                ?: aiText.indexOf("fastingDurationMinutes").takeIf { it != -1 }
                ?: aiText.indexOf("\"schedule\"").takeIf { it != -1 }

            if (markerIndex != null) {
                val startBrace = aiText.lastIndexOf('{', markerIndex)
                if (startBrace != -1) {
                    var depth = 0
                    var inString = false
                    var escape = false
                    for (i in startBrace until aiText.length) {
                        val c = aiText[i]
                        if (escape) {
                            escape = false
                            continue
                        }
                        if (c == '\\') {
                            escape = true
                            continue
                        }
                        if (c == '"') {
                            inString = !inString
                            continue
                        }
                        if (!inString) {
                            if (c == '{') depth++
                            else if (c == '}') {
                                depth--
                                if (depth == 0) {
                                    return aiText.substring(startBrace, i + 1)
                                }
                            }
                        }
                    }
                }
            }

            return null
        }

        /**
         * Pulisce la stringa JSON rimuovendo commenti // o /* */ e virgole finali (trailing commas)
         * che impedirebbero la deserializzazione standard.
         */
        fun cleanJsonString(raw: String): String {
            val sb = StringBuilder(raw.length)
            var inString = false
            var escape = false
            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                if (escape) {
                    sb.append(c)
                    escape = false
                    i++
                    continue
                }
                if (c == '\\') {
                    escape = true
                    sb.append(c)
                    i++
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    sb.append(c)
                    i++
                    continue
                }
                if (!inString && c == '/' && i + 1 < raw.length) {
                    if (raw[i + 1] == '/') {
                        // Salta il commento su singola riga // ...
                        i += 2
                        while (i < raw.length && raw[i] != '\n' && raw[i] != '\r') {
                            i++
                        }
                        continue
                    } else if (raw[i + 1] == '*') {
                        // Salta il commento su più righe /* ... */
                        i += 2
                        while (i + 1 < raw.length && !(raw[i] == '*' && raw[i + 1] == '/')) {
                            i++
                        }
                        i += 2
                        continue
                    }
                }
                sb.append(c)
                i++
            }
            var cleaned = sb.toString()
            // Rimuove eventuali virgole finali prima di } o ]
            cleaned = cleaned.replace(Regex(""",\s*([}\]])"""), "$1")
            return cleaned.trim()
        }

        fun extractPlanFromJsonBlock(aiText: String, jsonParser: Json = defaultJson): FastingPlan? {
            val rawCandidate = extractJsonCandidate(aiText) ?: return null
            val cleaned = cleanJsonString(rawCandidate)

            val plan = try {
                jsonParser.decodeFromString<FastingPlan>(cleaned)
            } catch (e: Exception) {
                try {
                    val startIdx = cleaned.indexOf('{')
                    val endIdx = cleaned.lastIndexOf('}')
                    if (startIdx >= 0 && endIdx > startIdx) {
                        jsonParser.decodeFromString<FastingPlan>(cleaned.substring(startIdx, endIdx + 1))
                    } else null
                } catch (e2: Exception) {
                    null
                }
            } ?: return null

            return sanitizePlan(plan)
        }

        /**
         * Sanifica, valida e corregge rigorosamente orari e proprietà dei piani generati dall'AI:
         * 1. Converte la durata da ore a minuti se l'AI ha specificato 1..24 invece di minuti totali.
         * 2. Rileva e corregge automaticamente l'inversione della finestra pasto vs finestra digiuno
         *    (quando l'AI mette per sbaglio l'orario dei pasti in startHour/endHour).
         * 3. Garantisce che (end - start + 1440) % 1440 == fastingDurationMinutes.
         * 4. Per i giorni di riposo/pasti liberi (isFastingDay == false), azzera la durata e orari.
         * 5. Assicura che la settimana contenga tutti e 7 i giorni (Lunedì = 1 .. Domenica = 7).
         */
        fun sanitizePlan(plan: FastingPlan): FastingPlan {
            val finalId = plan.id.ifBlank { "custom-ai-${java.util.UUID.randomUUID().toString().take(8)}" }
            val finalName = plan.name.ifBlank { "Piano Personalizzato" }

            val templateDay = plan.days.firstOrNull { it.isFastingDay }
                ?: com.cibotimer.data.models.DayPlan(
                    dayOfWeek = 1,
                    isFastingDay = true,
                    startHour = 20,
                    startMinute = 0,
                    endHour = 12,
                    endMinute = 0,
                    fastingDurationMinutes = 960
                )

            val sanitizedDays = (1..7).map { dayNum ->
                val existing = plan.days.find { it.dayOfWeek == dayNum }
                    ?: plan.days.getOrNull(dayNum - 1)
                    ?: templateDay.copy(dayOfWeek = dayNum)

                val isFasting = existing.isFastingDay
                if (!isFasting) {
                    com.cibotimer.data.models.DayPlan(
                        dayOfWeek = dayNum,
                        isFastingDay = false,
                        startHour = 0,
                        startMinute = 0,
                        endHour = 0,
                        endMinute = 0,
                        fastingDurationMinutes = 0
                    )
                } else {
                    var startH = existing.startHour.coerceIn(0, 23)
                    var startM = existing.startMinute.coerceIn(0, 59)
                    var endH = existing.endHour.coerceIn(0, 23)
                    var endM = existing.endMinute.coerceIn(0, 59)
                    var duration = existing.fastingDurationMinutes

                    // 1. Se espresso in ore (1..24), converte in minuti
                    if (duration in 1..24) {
                        duration *= 60
                    }

                    // 2. Calcola differenza tra inizio e fine
                    val startTotal = startH * 60 + startM
                    val endTotal = endH * 60 + endM
                    val rawDiff = (endTotal - startTotal + 1440) % 1440
                    val diff = if (rawDiff == 0 && (startH != 0 || startM != 0 || endH != 0 || endM != 0)) 1440 else rawDiff

                    // 3. Controllo inversione finestra pasto vs finestra digiuno:
                    // Se diff + duration == 1440 e diff != duration, l'AI ha specificato la finestra di alimentazione
                    // invece della finestra di digiuno (es. 12:00 -> 20:00 [480m = 8h] con durata 960m = 16h).
                    // Invertiamo start ed end: il digiuno diventa 20:00 -> 12:00 (durata 960m = 16h).
                    if (duration > 0 && diff > 0 && diff + duration == 1440 && diff != duration) {
                        val tempH = startH
                        val tempM = startM
                        startH = endH
                        startM = endM
                        endH = tempH
                        endM = tempM
                    }

                    // 4. Se duration <= 0, calcola la durata dalla differenza
                    if (duration <= 0) {
                        val curStart = startH * 60 + startM
                        val curEnd = endH * 60 + endM
                        val curDiff = (curEnd - curStart + 1440) % 1440
                        duration = if (curDiff == 0) 1440 else curDiff
                    }

                    // 5. Coerenza matematica tra orario di inizio, fine e durata
                    val finalStartTotal = startH * 60 + startM
                    val finalEndTotal = endH * 60 + endM
                    val finalDiff = (finalEndTotal - finalStartTotal + 1440) % 1440

                    if (finalDiff != duration && !(finalDiff == 0 && duration == 1440)) {
                        val planText = "${plan.name} ${plan.description}".lowercase()
                        val diffHours = finalDiff / 60
                        if (finalDiff > 0 && (planText.contains("${diffHours}h") || planText.contains("${diffHours} ore") || planText.contains("$diffHours:"))) {
                            // La differenza oraria esplicita corrisponde a quanto indicato nel titolo/descrizione
                            duration = finalDiff
                        } else {
                            // Sincronizza l'orario di fine da inizio + durata
                            val expectedEnd = (finalStartTotal + duration + 14400) % 1440
                            endH = expectedEnd / 60
                            endM = expectedEnd % 60
                        }
                    }

                    com.cibotimer.data.models.DayPlan(
                        dayOfWeek = dayNum,
                        isFastingDay = true,
                        startHour = startH,
                        startMinute = startM,
                        endHour = endH,
                        endMinute = endM,
                        fastingDurationMinutes = duration
                    )
                }
            }

            val sanitizedPeriods = plan.periods?.mapIndexed { index, p ->
                var pDuration = p.fastingDurationMinutes
                if (p.isFasting) {
                    if (pDuration in 1..24) pDuration *= 60
                    if (pDuration <= 0) {
                        val sTotal = p.startHour * 60 + p.startMinute
                        val eTotal = p.endHour * 60 + p.endMinute
                        val diff = (eTotal - sTotal + 1440) % 1440
                        pDuration = if (diff == 0) 1440 else diff
                    }
                } else {
                    pDuration = 0
                }

                val startDay = if (p.startDayOfWeek in 1..7) p.startDayOfWeek else 1
                val expectedEndTotal = (p.startHour * 60 + p.startMinute + pDuration + 1440 * 10) % 1440
                val endDayOffset = (p.startHour * 60 + p.startMinute + pDuration) / 1440
                val calculatedEndDay = (startDay - 1 + endDayOffset) % 7 + 1

                p.copy(
                    periodNumber = index + 1,
                    startDayOfWeek = startDay,
                    startHour = p.startHour.coerceIn(0, 23),
                    startMinute = p.startMinute.coerceIn(0, 59),
                    endDayOfWeek = calculatedEndDay,
                    endHour = expectedEndTotal / 60,
                    endMinute = expectedEndTotal % 60,
                    fastingDurationMinutes = pDuration
                )
            }

            return plan.copy(
                id = finalId,
                name = finalName,
                days = sanitizedDays,
                periods = sanitizedPeriods
            )
        }
    }
}
