package com.cibotimer.ui.screens.timer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.zIndex
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cibotimer.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingState
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.notifications.NotificationHelper
import com.cibotimer.ui.components.AppPrimaryButton
import com.cibotimer.ui.components.AppSecondaryButton
import com.cibotimer.ui.components.FastingCircularProgress
import com.cibotimer.ui.components.FastingStagesCard
import com.cibotimer.ui.components.FastingStagesSheet
import com.cibotimer.ui.components.PlanPickerModal
import com.cibotimer.ui.components.TimeEditorModal
import com.cibotimer.ui.components.WeekBarChart
import com.cibotimer.ui.theme.*
import com.cibotimer.update.UpdateManager
import com.cibotimer.update.UpdateState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimerScreen(
    repository: AppRepository,
    updateManager: UpdateManager? = null,
    onNavigateToPlanSelection: () -> Unit = {},
    onNavigateToWater: () -> Unit = {},
    onNavigateToCoaching: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activePlan by repository.activePlan.collectAsState()
    val currentSession by repository.currentSession.collectAsState()
    val updateState = updateManager?.updateState?.collectAsState()?.value ?: UpdateState.Idle

    // Controllo automatico aggiornamenti all'apertura del Timer
    LaunchedEffect(Unit) {
        if (updateManager?.autoCheckEnabled == true) {
            updateManager.checkForUpdates(manual = false)
        }
    }

    // Real-time ticker
    var currentEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentEpochMillis = System.currentTimeMillis()
        }
    }

    var showEndConfirmDialog by remember { mutableStateOf(false) }
    var showTimeEditorModal by remember { mutableStateOf(false) }
    var selectedDayToEdit by remember { mutableStateOf<Int?>(null) }
    var showPlanPickerModal by remember { mutableStateOf(false) }
    var showStagesSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )

    val isFasting = currentSession != null && currentSession?.state == FastingState.FASTING
    val isEating = currentSession != null && currentSession?.state == FastingState.EATING
    val isRunning = isFasting || isEating

    val scheduled = remember(activePlan, currentEpochMillis) {
        FastingCalculator.resolveCurrentScheduledPeriod(activePlan, currentEpochMillis)
    }

    val currentDayOfWeek = remember(currentEpochMillis) {
        val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (calDay == Calendar.SUNDAY) 7 else calDay - 1
    }

    val todayPlan = activePlan.days.find { it.dayOfWeek == currentDayOfWeek }
        ?: activePlan.days.firstOrNull()
        ?: DayPlan(1, true, 20, 0, 12, 0, 960)

    val totalPlannedSeconds = if (isFasting && currentSession != null) {
        FastingCalculator.calculateTotalPlannedSeconds(currentSession!!)
    } else if (isEating && currentSession != null) {
        ((currentSession!!.plannedEndTimeEpochMillis - currentSession!!.startTimeEpochMillis) / 1000L).coerceAtLeast(1L)
    } else {
        scheduled.durationSeconds
    }

    val remainingSeconds = if (isFasting && currentSession != null) {
        FastingCalculator.calculateRemainingSeconds(currentSession!!, currentEpochMillis)
    } else if (isEating && currentSession != null) {
        ((currentSession!!.plannedEndTimeEpochMillis - currentEpochMillis) / 1000L).coerceAtLeast(0L)
    } else {
        scheduled.remainingSeconds
    }

    val elapsedSeconds = if (isFasting && currentSession != null) {
        FastingCalculator.calculateElapsedSeconds(currentSession!!, currentEpochMillis)
    } else if (isEating && currentSession != null) {
        ((currentEpochMillis - currentSession!!.startTimeEpochMillis) / 1000L).coerceAtLeast(0L)
    } else {
        scheduled.eatingElapsedSeconds
    }

    val progressFraction = if (isFasting && currentSession != null) {
        FastingCalculator.calculateProgressFraction(currentSession!!, currentEpochMillis)
    } else if (isEating && currentSession != null) {
        (elapsedSeconds.toFloat() / totalPlannedSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        scheduled.progressFraction
    }

    // Transizione continua automatica: il timer non deve MAI fermarsi tra finestra pasto e digiuno o viceversa
    LaunchedEffect(currentEpochMillis) {
        if (isRunning) {
            repository.syncOrAdvanceSession(currentEpochMillis)
        }
    }

    // Formattazione orari Inizio e Fine identici al periodo della popup
    val timeFormat = remember { SimpleDateFormat("EEE HH:mm", Locale.ITALIAN) }

    val activeStartMillis = if (currentSession != null) {
        currentSession!!.startTimeEpochMillis
    } else {
        scheduled.periodStartEpochMillis
    }

    val activeEndMillis = if (currentSession != null) {
        currentSession!!.plannedEndTimeEpochMillis
    } else {
        scheduled.periodEndEpochMillis
    }

    val startTimeFormatted = remember(activeStartMillis) {
        timeFormat.format(Date(activeStartMillis)).lowercase()
    }

    val endTimeFormatted = remember(activeEndMillis) {
        timeFormat.format(Date(activeEndMillis)).lowercase()
    }

    CompactTimerContent(
        isFasting = isFasting,
        isEating = isEating,
        remainingSeconds = remainingSeconds,
        totalPlannedSeconds = totalPlannedSeconds,
        elapsedSeconds = elapsedSeconds,
        progressFraction = progressFraction,
        startTimeFormatted = startTimeFormatted,
        endTimeFormatted = endTimeFormatted,
        planName = activePlan.name,
        planDays = activePlan.days,
        planPeriods = activePlan.periods,
        currentDayOfWeek = currentDayOfWeek,
        onStopClick = { showEndConfirmDialog = true },
        onChangePlanClick = { showPlanPickerModal = true },
        onFastingPhaseClick = { showStagesSheet = true },
        onInizioClick = {
            selectedDayToEdit = scheduled.period.startDayOfWeek
            showTimeEditorModal = true
        },
        onFineClick = {
            selectedDayToEdit = scheduled.period.startDayOfWeek
            showTimeEditorModal = true
        },
        onStartClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            repository.startFasting()
        },
        onDaySelected = { dayOfWeek ->
            selectedDayToEdit = dayOfWeek
            showTimeEditorModal = true
        },
        onNotificationsClick = {
            NotificationHelper.openChannelSettings(context)
        },
        onSettingsClick = onNavigateToSettings,
        modifier = modifier
    )

    // Modal fasi del digiuno
    if (showStagesSheet) {
        FastingStagesSheet(
            currentElapsedMinutes = elapsedSeconds / 60,
            onDismiss = { showStagesSheet = false }
        )
    }

    // Dialog conferma Stop / Gestione ciclo
    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            containerColor = CiboCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = if (isFasting) stringResource(R.string.dialog_stop_title) else "Finestra Pasto in corso",
                    color = CiboTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (isFasting) {
                        "Sei in digiuno. Desideri passare subito alla finestra pasto oppure fermare completamente il timer?"
                    } else {
                        "Sei nella finestra di alimentazione. Desideri iniziare il digiuno adesso oppure fermare completamente il timer?"
                    },
                    color = CiboTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isFasting) {
                        Button(
                            onClick = {
                                repository.transitionToEating()
                                showEndConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CiboAccent)
                        ) {
                            Text(text = stringResource(R.string.timer_button_switch_eating), color = CiboButtonDarkText, fontWeight = FontWeight.Bold)
                        }
                    } else if (isEating) {
                        Button(
                            onClick = {
                                repository.startFastingNow()
                                showEndConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CiboAccent)
                        ) {
                            Text(text = stringResource(R.string.timer_button_start_fast), color = CiboButtonDarkText, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            repository.stopTimer()
                            showEndConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CiboStopRed)
                    ) {
                        Text(text = stringResource(R.string.timer_button_stop_timer), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) {
                    Text(
                        text = stringResource(R.string.dialog_stop_cancel),
                        color = CiboTextSecondary
                    )
                }
            }
        )
    }

    // Modal regolazione orari
    if (showTimeEditorModal) {
        TimeEditorModal(
            dayPlan = todayPlan,
            plan = activePlan,
            initialDayOfWeek = selectedDayToEdit,
            onSavePlan = { updatedPlan ->
                repository.updateActivePlan(updatedPlan)
                android.widget.Toast.makeText(context, context.getString(R.string.planner_plan_saved_toast), android.widget.Toast.LENGTH_SHORT).show()
            },
            onSaveCustomPlan = { newPlan, activate ->
                repository.saveCustomPlan(newPlan, activate = activate)
                val msg = if (activate) "Piano \"${newPlan.name}\" salvato e attivato!" else "Piano \"${newPlan.name}\" salvato!"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showTimeEditorModal = false
                selectedDayToEdit = null
            },
            onShiftSingleDay = { deltaHours, deltaMinutes ->
                val targetDay = selectedDayToEdit ?: todayPlan.dayOfWeek
                repository.shiftPlanDay(targetDay, deltaHours, deltaMinutes)
            },
            onShiftAllDays = { deltaHours ->
                repository.shiftPlanAllDays(deltaHours)
            },
            onAdjustDuration = { deltaMinutes ->
                val targetDay = selectedDayToEdit ?: todayPlan.dayOfWeek
                repository.adjustPlanDuration(targetDay, deltaMinutes)
            }
        )
    }

    // Modal selezione rapida piano di digiuno
    if (showPlanPickerModal) {
        val customPlans by repository.customPlans.collectAsState()
        PlanPickerModal(
            currentPlan = activePlan,
            customPlans = customPlans,
            onPlanSelected = { selectedPlan ->
                repository.setActivePlan(selectedPlan)
                showPlanPickerModal = false
            },
            onSaveCustomPlan = { newPlan ->
                repository.saveCustomPlan(newPlan)
                showPlanPickerModal = false
            },
            onRenamePlan = { renamedPlan ->
                repository.renamePlan(renamedPlan.id, renamedPlan.name)
            },
            onDeletePlan = { planToDelete ->
                repository.deletePlan(planToDelete.id)
            },
            onDismiss = { showPlanPickerModal = false }
        )
    }

    // ==========================================
    // NOTIFICHE & DIALOG AGGIORNAMENTO OTA (HTTPS)
    // ==========================================
    when (updateState) {
        is UpdateState.Available -> {
            val info = updateState.info
            AlertDialog(
                onDismissRequest = { updateManager?.dismissBanner() },
                containerColor = CiboCard,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚀", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.update_available_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = CiboTextPrimary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.update_version_info, info.versionName, info.size),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboAccent
                        )
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.releaseNotes,
                                fontSize = 12.sp,
                                color = CiboTextSecondary,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                confirmButton = {
                    AppPrimaryButton(
                        text = if (com.cibotimer.BuildConfig.IS_PLAY_STORE_BUILD) stringResource(R.string.update_button_play) else stringResource(R.string.update_button_direct),
                        onClick = { updateManager?.downloadAndInstall(info) }
                    )
                },
                dismissButton = {
                    AppSecondaryButton(
                        text = stringResource(R.string.update_button_later),
                        onClick = { updateManager?.dismissBanner() }
                    )
                }
            )
        }

        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = CiboCard,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = stringResource(R.string.update_downloading_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CiboTextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CiboAccent,
                            trackColor = CiboTrack
                        )
                        Text(
                            text = stringResource(R.string.update_progress_percent, updateState.progressPercent),
                            fontSize = 13.sp,
                            color = CiboTextSecondary
                        )
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = { updateManager?.dismissBanner() },
                containerColor = CiboCard,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = stringResource(R.string.update_ready_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CiboTextPrimary
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.update_ready_message),
                        fontSize = 14.sp,
                        color = CiboTextSecondary
                    )
                },
                confirmButton = {
                    AppPrimaryButton(
                        text = stringResource(R.string.update_button_install),
                        onClick = { updateManager?.triggerInstall(updateState.apkUri) }
                    )
                },
                dismissButton = {
                    AppSecondaryButton(
                        text = stringResource(R.string.button_cancel),
                        onClick = { updateManager?.dismissBanner() }
                    )
                }
            )
        }

        else -> {}
    }
}

/**
 * Layout compatto della schermata Digiuno (zero scroll, visualizzazione 1 schermata)
 */
@Composable
fun CompactTimerContent(
    isFasting: Boolean,
    isEating: Boolean = false,
    remainingSeconds: Long,
    totalPlannedSeconds: Long,
    elapsedSeconds: Long,
    progressFraction: Float,
    startTimeFormatted: String,
    endTimeFormatted: String,
    planName: String = "Luigi 1",
    planDays: List<DayPlan>,
    planPeriods: List<com.cibotimer.data.models.FastingPeriod>? = null,
    currentDayOfWeek: Int,
    onStopClick: () -> Unit,
    onStartClick: () -> Unit,
    onChangePlanClick: () -> Unit = {},
    onFastingPhaseClick: () -> Unit = {},
    onInizioClick: () -> Unit = {},
    onFineClick: () -> Unit = {},
    onDaySelected: (Int) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRunning = isFasting || isEating

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CiboBg)
    ) {
        val availableHeight = maxHeight
        val isVeryShort = availableHeight < 670.dp
        val isShort = availableHeight < 740.dp

        val ringSize = when {
            isVeryShort -> 172.dp
            isShort -> 192.dp
            else -> 216.dp
        }
        val timerRowHeight = ringSize
        val planBarPaddingV = if (isVeryShort) 5.dp else 6.dp

        // Margine visivo sopra al pulsante Start/Stop rispetto al cerchio del timer:
        // Il pulsante Start/Stop ha altezza 40dp con offset(-20.dp) rispetto al bordo superiore della card.
        // Con Column(spacedBy = 8.dp), impostando stagesBoxPaddingTop = buttonTopMargin + 12.dp si garantisce
        // esattamente buttonTopMargin di spazio libero e pulito sopra al pulsante Start/Stop.
        val buttonTopMargin = when {
            isVeryShort -> 14.dp
            isShort -> 16.dp
            else -> 20.dp
        }
        val stagesBoxPaddingTop = buttonTopMargin + 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // 1. RIGA CENTRALE TIMER (Titolo a sinistra, Anello al centro, Stato a destra)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(timerRowHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // A SINISTRA: Titolo "Digiuno" allineato verticalmente in alto
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp, top = 6.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = stringResource(R.string.header_title_fasting),
                        fontSize = if (isVeryShort) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CiboTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // AL CENTRO: Anello allargato di circa il 20%
                FastingCircularProgress(
                    remainingSeconds = remainingSeconds,
                    totalPlannedSeconds = totalPlannedSeconds,
                    elapsedSeconds = elapsedSeconds,
                    progressFraction = progressFraction,
                    state = if (isFasting) FastingState.FASTING else if (isEating) FastingState.EATING else FastingState.NOT_STARTED,
                    modifier = Modifier.size(ringSize)
                )

                // A DESTRA: "Adesso: Digiunare" allineato verticalmente in alto
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, top = 6.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = stringResource(R.string.timer_label_now),
                            fontSize = if (isVeryShort) 10.5.sp else 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = CiboTextSecondary,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = if (isFasting) stringResource(R.string.legend_fasting) else if (isEating) stringResource(R.string.legend_eating) else stringResource(R.string.timer_status_stopped_short),
                            fontSize = if (isVeryShort) 12.5.sp else 13.5.sp,
                            fontWeight = if (isRunning) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isFasting) CiboAccent else if (isEating) CiboTimerAzureGlow else CiboTextSecondary,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // ==========================================
            // 2. PULSANTE START/STOP & FASI (Card Inizio/Fine con pulsante agganciato al bordo superiore)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = stagesBoxPaddingTop),
                contentAlignment = Alignment.TopCenter
            ) {
                FastingStagesCard(
                    elapsedSeconds = elapsedSeconds,
                    state = if (isFasting) FastingState.FASTING else if (isEating) FastingState.EATING else FastingState.NOT_STARTED,
                    startTimeFormatted = startTimeFormatted,
                    endTimeFormatted = endTimeFormatted,
                    onInizioClick = onInizioClick,
                    onFineClick = onFineClick,
                    onClick = onFastingPhaseClick
                )

                val buttonInteraction = remember { MutableInteractionSource() }
                val buttonPressed by buttonInteraction.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (buttonPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "buttonScale"
                )
                val buttonShape = RoundedCornerShape(20.dp)

                Box(
                    modifier = Modifier
                        .zIndex(1f)
                        .offset(y = (-20).dp)
                        .width(140.dp)
                        .height(40.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .clip(buttonShape)
                        .let {
                            if (isRunning) it.background(CiboStopRed)
                            else it.background(Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0288D1))))
                        }
                        .clickable(
                            interactionSource = buttonInteraction,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClick = if (isRunning) onStopClick else onStartClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(CiboButtonDarkText.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(CiboStopRed)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.button_stop),
                                color = Color(0xFF2A0D0F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(CiboButtonDarkText.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.content_desc_start),
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.button_start),
                                color = Color(0xFF04202B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. BARRA NOME DEL PIANO (spazio costante 8dp via Column spacedBy)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CiboCard.copy(alpha = 0.7f))
                    .border(1.dp, CiboCardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onChangePlanClick)
                    .padding(horizontal = 12.dp, vertical = planBarPaddingV),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.content_desc_change_plan),
                        tint = CiboAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = CiboTextSecondary, fontWeight = FontWeight.Normal)) {
                                append("Piano: ")
                            }
                            withStyle(SpanStyle(color = CiboTextPrimary, fontWeight = FontWeight.SemiBold)) {
                                append(planName)
                            }
                        },
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.button_change_plan),
                    color = CiboAccent,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // ==========================================
            // 4. PIANO SETTIMANALE (spazio costante 8dp via Column spacedBy, asse 0-24h sempre visibile)
            // ==========================================
            WeekBarChart(
                days = planDays,
                selectedDayOfWeek = currentDayOfWeek,
                periods = planPeriods,
                onDaySelected = onDaySelected,
                fillMaxHeight = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

private val previewDays = listOf(
    DayPlan(dayOfWeek = 1, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960),
    DayPlan(dayOfWeek = 2, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960),
    DayPlan(dayOfWeek = 3, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960),
    DayPlan(dayOfWeek = 4, isFastingDay = true, startHour = 18, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 1080),
    DayPlan(dayOfWeek = 5, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960),
    DayPlan(dayOfWeek = 6, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960),
    DayPlan(dayOfWeek = 7, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960)
)

@Preview(name = "Stato in digiuno (Pixel)", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun PreviewTimerScreenFasting() {
    CiboTimerTheme {
        CompactTimerContent(
            isFasting = true,
            remainingSeconds = 39041L, // 10h 50m 41s
            totalPlannedSeconds = 72000L, // 20h
            elapsedSeconds = 32959L, // 9h 10m
            progressFraction = 0.46f,
            startTimeFormatted = "Lun 00:41",
            endTimeFormatted = "Lun 20:41",
            planDays = previewDays,
            currentDayOfWeek = 1,
            onStopClick = {},
            onStartClick = {}
        )
    }
}

@Preview(name = "Stato nessun digiuno attivo", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun PreviewTimerScreenEating() {
    CiboTimerTheme {
        CompactTimerContent(
            isFasting = false,
            remainingSeconds = 14400L, // 4h
            totalPlannedSeconds = 28800L, // 8h
            elapsedSeconds = 14400L,
            progressFraction = 0.50f,
            startTimeFormatted = "Oggi 12:00",
            endTimeFormatted = "Oggi 20:00",
            planDays = previewDays,
            currentDayOfWeek = 1,
            onStopClick = {},
            onStartClick = {}
        )
    }
}

@Preview(name = "Schermo Piccolo 360x640", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun PreviewTimerScreenSmall() {
    CiboTimerTheme {
        CompactTimerContent(
            isFasting = true,
            remainingSeconds = 39041L,
            totalPlannedSeconds = 72000L,
            elapsedSeconds = 32959L,
            progressFraction = 0.46f,
            startTimeFormatted = "Lun 00:41",
            endTimeFormatted = "Lun 20:41",
            planDays = previewDays,
            currentDayOfWeek = 1,
            onStopClick = {},
            onStartClick = {}
        )
    }
}
