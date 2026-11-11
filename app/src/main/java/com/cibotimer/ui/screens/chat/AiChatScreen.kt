package com.cibotimer.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.*
import com.cibotimer.data.remote.OpenAiApiClient
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.ui.components.MarkdownText
import com.cibotimer.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

private val aiChatJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    repository: AppRepository,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aiConfig by repository.aiConfig.collectAsState()
    val chatMessages by repository.chatMessages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputMessage by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamingJob by remember { mutableStateOf<Job?>(null) }
    var planAppliedSnackbarMessage by remember { mutableStateOf<String?>(null) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showModelPickerModal by remember { mutableStateOf(false) }

    // Auto-scroll to bottom on new messages or stream chunks
    LaunchedEffect(chatMessages.size, isStreaming) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // AI Coach Sparkle Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CiboTimerAzureContainer)
                                .border(1.dp, CiboTimerAzure.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CiboTimerAzureGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Coach AI CiboTimer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            // Quick Model / Provider chip (Tappable!)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showModelPickerModal = true }
                                    .padding(vertical = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (aiConfig.isEnabled) CiboTimerAzureGlow else Color(0xFF64748B))
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (aiConfig.isEnabled) "${aiConfig.modelName} · ${aiConfig.providerName}" else stringResource(R.string.ai_chat_subtitle_disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = if (aiConfig.isEnabled) CiboTimerAzureLight else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Clear conversation button
                    if (chatMessages.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.ai_dialog_clear_title),
                                tint = TextSecondary
                            )
                        }
                    }

                    // Settings shortcut
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.ai_config_button),
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        if (!aiConfig.isEnabled) {
            // Disabled State Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CiboTimerAzureContainer)
                                .border(1.5.dp, CiboTimerAzureGlow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CiboTimerAzureGlow,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Coach Nutrizionale AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Il Coach AI è il tuo assistente personale per creare piani di digiuno su misura, analizzare i pasti e comprendere la chetosi.\n\nSupporta qualsiasi provider OpenAI-compatibile (OpenAI, DeepSeek, Groq, Google Gemini, Ollama locale).\n\nPer preservare la tua privacy è disattivato finché non configuri la tua chiave API.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CiboTimerBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.ai_open_settings), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            // Main Chat Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                // Banner avviso piano applicato
                AnimatedVisibility(visible = planAppliedSnackbarMessage != null) {
                    Surface(
                        color = CiboTimerAzureDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = planAppliedSnackbarMessage ?: "",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = { planAppliedSnackbarMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                            }
                        }
                    }
                }

                // Messaggi della chat
                if (chatMessages.isEmpty()) {
                    EmptyChatState(
                        onSelectPrompt = { prompt ->
                            startSendMessage(
                                text = prompt,
                                repository = repository,
                                scope = scope,
                                onStreamingChange = { isStreaming = it },
                                setJob = { streamingJob = it }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { msg ->
                            val isLast = msg.id == chatMessages.lastOrNull()?.id
                            ChatMessageItem(
                                message = msg,
                                isLast = isLast,
                                isStreaming = isStreaming && isLast,
                                onApplyPlan = { planJson, customName, activate ->
                                    val success = repository.applyProposedPlan(planJson, customName, activate = activate)
                                    if (success) {
                                        val appliedName = customName?.takeIf { it.isNotBlank() } ?: "personalizzato"
                                        planAppliedSnackbarMessage = if (activate) {
                                            "Piano '$appliedName' applicato e attivato con successo!"
                                        } else {
                                            "Piano '$appliedName' salvato nei tuoi piani personalizzati!"
                                        }
                                    }
                                },
                                onRegenerate = {
                                    val lastUserMsg = chatMessages.findLast { it.sender == MessageSender.USER }
                                    if (lastUserMsg != null && !isStreaming) {
                                        startSendMessage(
                                            text = lastUserMsg.text,
                                            repository = repository,
                                            scope = scope,
                                            onStreamingChange = { isStreaming = it },
                                            setJob = { streamingJob = it }
                                        )
                                    }
                                },
                                onDelete = { repository.deleteChatMessage(msg.id) }
                            )
                        }
                    }
                }

                // Quick Prompt Chips (se la chat non è vuota)
                if (chatMessages.isNotEmpty() && !isStreaming) {
                    QuickPromptsRow(
                        onSelectPrompt = { prompt ->
                            inputMessage = prompt
                        }
                    )
                }

                // Input Bar con Send / Stop button
                ChatInputBar(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    isStreaming = isStreaming,
                    onSend = {
                        val text = inputMessage.trim()
                        if (text.isNotBlank() && !isStreaming) {
                            inputMessage = ""
                            startSendMessage(
                                text = text,
                                repository = repository,
                                scope = scope,
                                onStreamingChange = { isStreaming = it },
                                setJob = { streamingJob = it }
                            )
                        }
                    },
                    onStop = {
                        streamingJob?.cancel()
                        isStreaming = false
                    }
                )
            }
        }
    }

    // Modal di conferma cancellazione cronologia
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.ai_dialog_clear_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.ai_dialog_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.clearChatHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ai_dialog_clear_confirm), color = CiboStopRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.button_cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            textContentColor = TextSecondary,
            titleContentColor = TextPrimary
        )
    }

    // Modal di selezione rapida del modello / provider (stile MiniiChat)
    if (showModelPickerModal) {
        ModelPickerBottomSheet(
            currentConfig = aiConfig,
            onDismiss = { showModelPickerModal = false },
            onSelectPreset = { preset ->
                repository.updateAiConfig(
                    aiConfig.copy(
                        providerName = preset.name,
                        baseUrl = preset.baseUrl,
                        modelName = preset.sampleModel.ifBlank { aiConfig.modelName }
                    )
                )
                showModelPickerModal = false
            },
            onOpenSettings = {
                showModelPickerModal = false
                onNavigateToSettings()
            }
        )
    }
}

/**
 * Triggers sending a message with real-time SSE streaming.
 */
private fun startSendMessage(
    text: String,
    repository: AppRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onStreamingChange: (Boolean) -> Unit,
    setJob: (Job?) -> Unit
) {
    val userMsg = ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = MessageSender.USER,
        text = text,
        timestampEpochMillis = System.currentTimeMillis()
    )
    repository.addChatMessage(userMsg)

    val assistantId = UUID.randomUUID().toString()
    val initialAiMsg = ChatMessage(
        id = assistantId,
        sender = MessageSender.AI,
        text = "",
        timestampEpochMillis = System.currentTimeMillis()
    )
    repository.addChatMessage(initialAiMsg)

    onStreamingChange(true)

    val job = scope.launch {
        val client = repository.getOpenAiApiClient()
        val currentConfig = repository.aiConfig.value.copy(
            systemPrompt = repository.buildCurrentAiCoachPrompt()
        )
        val history = repository.chatMessages.value

        val accumulatedContent = StringBuilder()
        val accumulatedThinking = StringBuilder()

        try {
            client.sendMessageStream(currentConfig, history, text).collect { chunk ->
                if (!chunk.thinkingDelta.isNullOrEmpty()) {
                    accumulatedThinking.append(chunk.thinkingDelta)
                }
                if (!chunk.contentDelta.isNullOrEmpty()) {
                    accumulatedContent.append(chunk.contentDelta)
                }

                // Aggiorna messaggio in tempo reale
                repository.updateChatMessage(
                    id = assistantId,
                    text = accumulatedContent.toString(),
                    thinking = if (accumulatedThinking.isNotEmpty()) accumulatedThinking.toString() else null
                )
            }

            // A fine stream: estrai eventuale piano nutrizionale proposto
            val finalContent = accumulatedContent.toString()
            val extractedPlan = client.extractPlanFromJsonBlock(finalContent)
            val planJsonString = extractedPlan?.let { aiChatJson.encodeToString(FastingPlan.serializer(), it) }

            repository.updateChatMessage(
                id = assistantId,
                text = finalContent,
                thinking = if (accumulatedThinking.isNotEmpty()) accumulatedThinking.toString() else null,
                planJson = planJsonString
            )
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                repository.updateChatMessage(
                    id = assistantId,
                    text = accumulatedContent.toString().ifBlank {
                        "⚠️ Si è verificato un errore durante la risposta: ${e.localizedMessage ?: "Errore di connessione."}"
                    }
                )
            }
        } finally {
            onStreamingChange(false)
        }
    }

    setJob(job)
}

/**
 * Singolo messaggio nella cronologia: User o Assistant con Markdown, Thinking accordion e Card piano.
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isLast: Boolean,
    isStreaming: Boolean,
    onApplyPlan: (planJson: String, customName: String?, activate: Boolean) -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // Bolla Utente (Destra - CiboTimerBlue)
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = CiboTimerBlue,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        fontSize = 14.5.sp,
                        color = Color.White,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            // Bolla Assistente AI o Sistema (Sinistra)
            Column(modifier = Modifier.fillMaxWidth(0.96f)) {
                // Header Coach
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(CiboTimerAzureContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CiboTimerAzureGlow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSystem) "Sistema" else "Coach AI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CiboTextPrimary
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Thinking block (se presente, stile MiniiChat / DeepSeek R1)
                if (!message.thinking.isNullOrBlank()) {
                    ThinkingAccordionBlock(thinking = message.thinking)
                    Spacer(Modifier.height(8.dp))
                }

                // Contenuto del messaggio
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    color = if (isSystem) Color(0xFF23160A) else Color(0xFF0F1826),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (message.text.isEmpty() && isStreaming) {
                            TypingDotsIndicator()
                        } else {
                            SelectionContainer {
                                MarkdownText(
                                    text = message.text,
                                    color = if (isSystem) CiboTimerOrange else CiboTextPrimary
                                )
                            }
                        }

                        // Se contiene un piano personalizzato generato
                        if (!message.proposedPlanJson.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ProposedPlanCard(
                                planJson = message.proposedPlanJson,
                                onApply = { customName, activate -> onApplyPlan(message.proposedPlanJson, customName, activate) }
                            )
                        }
                    }
                }

                // Barra azioni del messaggio (Copia, Rigenera, Elimina)
                if (message.text.isNotEmpty() && (!isLast || !isStreaming)) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        // Copia
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboard.setText(AnnotatedString(message.text))
                                    copied = true
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copia",
                                tint = if (copied) CiboTimerAzureGlow else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (copied) "Copiato!" else "Copia",
                                fontSize = 11.5.sp,
                                color = if (copied) CiboTimerAzureGlow else TextSecondary
                            )
                        }

                        // Rigenera (solo per l'ultimo assistente)
                        if (isLast) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onRegenerate)
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Rigenera",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Rigenera",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Elimina
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Elimina",
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thinking / Reasoning Block per modelli come DeepSeek R1 e o1 (stile MiniiChat).
 */
@Composable
private fun ThinkingAccordionBlock(thinking: String) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF09121E),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = CiboTimerAzureLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Pensiero del modello (${thinking.length} car.)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CiboTimerAzureLight
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardBorderGlow.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = thinking,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

/**
 * Tre puntini pulsanti con animazione fluida in attesa del primo token.
 */
@Composable
private fun TypingDotsIndicator() {
    val infinite = rememberInfiniteTransition(label = "typingDots")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotsAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        repeat(3) { i ->
            val dotAlpha = when (i) {
                0 -> alpha
                1 -> (1.3f - alpha).coerceIn(0.2f, 1f)
                else -> alpha * 0.7f + 0.3f
            }
            Box(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(CiboTimerAzureGlow.copy(alpha = dotAlpha))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Il coach sta rispondendo...",
            fontSize = 12.sp,
            color = CiboTimerAzureLight
        )
    }
}

/**
 * Barra inferiore per digitazione e controlli Send / Stop.
 */
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = Color(0xFF0C1320),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Campo di testo multi-linea espandibile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF131D2D))
                    .border(1.dp, CardBorderGlow, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Chiedi al Coach CiboTimer...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 20.dp, max = 110.dp),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(CiboTimerAzureGlow)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Pulsante dinamico: Stop se in streaming, Send se fermo
            if (isStreaming) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CiboStopRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Ferma generazione",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (value.isNotBlank()) CiboTimerBlue else Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Invia",
                        tint = if (value.isNotBlank()) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * Scheda stato vuoto quando non ci sono messaggi.
 */
@Composable
private fun EmptyChatState(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "⏱️ Crea un piano 18:6 su misura per me" to "Crea un piano di digiuno intermittente 18:6 personalizzato per me, considerando che lavoro dalle 9 alle 18.",
        "🥑 Come gestire la fame serale?" to "Quali sono le strategie scientifiche più efficaci per gestire gli attacchi di fame la sera durante il digiuno?",
        "💧 Cosa posso bere a digiuno?" to "Cosa posso bere durante le ore di digiuno senza interrompere l'autofagia o la chetosi?",
        "⚡ Spiegami i benefici dell'autofagia" to "Spiegami in modo chiaro e scientifico cosa accade nel corpo durante l'autofagia e a partire da quante ore si attiva."
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(CiboTimerAzureContainer)
                .border(1.5.dp, CiboTimerAzureGlow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = CiboTimerAzureGlow,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Coach Nutrizionale",
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = TextPrimary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Chiedi consigli su digiuno, ricette, chetosi e crea piani settimanali personalizzati.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            prompts.forEach { (label, prompt) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1826),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(prompt) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CiboTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = CiboTimerAzure,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Riga orizzontale con suggerimenti rapidi.
 */
@Composable
private fun QuickPromptsRow(onSelectPrompt: (String) -> Unit) {
    val quicks = listOf(
        "Cosa bere?" to "Cosa posso bere a digiuno?",
        "Fame serale" to "Come posso calmare la fame stasera?",
        "Autofagia" to "Come si attiva l'autofagia?",
        "Piano 16:8" to "Consigliami il miglior piano 16:8."
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quicks) { (chip, prompt) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF131D2D),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
                modifier = Modifier.clickable { onSelectPrompt(prompt) }
            ) {
                Text(
                    text = chip,
                    fontSize = 12.sp,
                    color = CiboTimerAzureLight,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Bottom Sheet per la selezione rapida di Provider e Modello (stile MiniiChat).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerBottomSheet(
    currentConfig: AiConfig,
    onDismiss: () -> Unit,
    onSelectPreset: (ProviderPreset) -> Unit,
    onOpenSettings: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1726),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seleziona Provider AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = CiboTimerAzure)
                    Spacer(Modifier.width(4.dp))
                    Text("Configura", color = CiboTimerAzure, fontSize = 13.sp)
                }
            }

            Text(
                text = "Scegli un preset compatibile con le API OpenAI o personalizza endpoint e chiavi.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ProviderPresets.all) { preset ->
                    val isSelected = currentConfig.providerName == preset.name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CiboTimerAzureContainer else Color(0xFF131D2D),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CiboTimerAzureGlow else CardBorderGlow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPreset(preset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) CiboTimerAzureGlow else TextPrimary
                                    )
                                    if (preset.sampleModel.isNotBlank()) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = preset.sampleModel,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = preset.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selezionato",
                                    tint = CiboTimerAzureGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card interattiva per visualizzare, rinominare ed applicare o salvare un piano generato dall'AI.
 */
@Composable
fun ProposedPlanCard(
    planJson: String,
    onApply: (customName: String?, activate: Boolean) -> Unit
) {
    val plan = remember(planJson) {
        try {
            val decoded = aiChatJson.decodeFromString<FastingPlan>(planJson)
            com.cibotimer.data.remote.OpenAiApiClient.sanitizePlan(decoded)
        } catch (e: Exception) {
            null
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember(plan) { mutableStateOf(plan?.name ?: "") }

    if (plan != null) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF09121E),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CiboTimerAzureGlow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CiboTimerAzureGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Piano Nutrizionale Consigliato",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CiboTimerAzureGlow
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CiboTimerAzureContainer
                    ) {
                        Text(
                            text = "COACH AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboTimerAzureGlow,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = plan.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )

                if (plan.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plan.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scheda di riepilogo e verifica oraria
                val fastingDaysCount = plan.days.count { it.isFastingDay }
                val restDaysCount = 7 - fastingDaysCount
                val sampleDay = plan.days.firstOrNull { it.isFastingDay }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0C1929),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B3354)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CiboTimerAzureGlow,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Orari verificati e coerenti",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CiboTimerAzureGlow
                            )
                        }

                        if (sampleDay != null) {
                            val eatingMins = (1440 - sampleDay.fastingDurationMinutes).coerceAtLeast(0)
                            val eatingH = eatingMins / 60
                            val eatingM = eatingMins % 60
                            val eatingDurationStr = if (eatingM == 0) "${eatingH}h" else "${eatingH}h ${eatingM}m"

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🔥 Digiuno: ${sampleDay.startTimeFormatted} → ${sampleDay.endTimeFormatted} (${sampleDay.durationHoursString})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🍽️ Finestra pasti: ${sampleDay.endTimeFormatted} → ${sampleDay.startTimeFormatted} ($eatingDurationStr)",
                                fontSize = 12.sp,
                                color = CiboTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (restDaysCount == 0) {
                                "📅 Frequenza: 7 giorni su 7"
                            } else {
                                "📅 Frequenza: $fastingDaysCount giorni di digiuno · $restDaysCount giorni liberi"
                            },
                            fontSize = 11.sp,
                            color = CiboTimerAzureLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pulsanti azione rapidi: Solo Salva | Applica e Attiva
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsante "Solo Salva" (senza attivare subito)
                    OutlinedButton(
                        onClick = { onApply(null, false) },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CiboTimerAzure),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = CiboTimerAzure)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Solo Salva",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = CiboTimerAzure
                        )
                    }

                    // Pulsante "Applica e Attiva"
                    Button(
                        onClick = { onApply(null, true) },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = CiboTimerBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Applica e Attiva",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Opzione Rinomina / Salva con altro nome
                TextButton(
                    onClick = {
                        renameInput = plan.name
                        showRenameDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = CiboTimerAzureLight)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Rinomina o salva con altro nome...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CiboTimerAzureLight
                    )
                }
            }
        }
    }

    if (showRenameDialog && plan != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = "Salva Piano con Altro Nome",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Inserisci il nome con cui vuoi salvare questo piano nei tuoi piani personalizzati:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.ai_custom_plan_placeholder), color = TextSecondary.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CiboTimerAzureGlow,
                            unfocusedBorderColor = CardBorderGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CiboTimerAzureGlow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Puoi scegliere se salvarlo semplicemente per usarlo in seguito, oppure renderlo subito attivo.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val finalName = renameInput.trim().ifBlank { plan.name }
                            showRenameDialog = false
                            onApply(finalName, false)
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CiboTimerAzure),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.plan_save_only), fontWeight = FontWeight.Bold, color = CiboTimerAzure, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val finalName = renameInput.trim().ifBlank { plan.name }
                            showRenameDialog = false
                            onApply(finalName, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CiboTimerBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.plan_save_and_activate), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.button_cancel), color = TextSecondary)
                }
            },
            containerColor = Color(0xFF0F1726)
        )
    }
}
