package com.cibotimer.data.repository

import android.app.backup.BackupManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cibotimer.R
import com.cibotimer.data.models.*
import com.cibotimer.data.remote.GoogleDriveSyncClient
import com.cibotimer.data.remote.OpenAiApiClient
import com.cibotimer.domain.DateTimeFormatters
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.PlanShiftEngine
import com.cibotimer.notifications.FastingForegroundService
import com.cibotimer.notifications.FastingPersistence
import com.cibotimer.notifications.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID

class AppRepository(
    private val context: Context,
    private val openAiApiClient: OpenAiApiClient = OpenAiApiClient(),
    private val driveSyncClient: GoogleDriveSyncClient = GoogleDriveSyncClient(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val mutex = Mutex()
    private val stateFile = File(context.filesDir, "cibotimer_app_state.json")
    private val legacyStateFile = File(context.filesDir, "bodyfast_app_state.json")

    // Reactive StateFlows
    private val _userProfile = MutableStateFlow(
        UserProfile(weightUnit = com.cibotimer.domain.WeightUnitConverter.getDefaultWeightUnitForLocale())
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _activePlan = MutableStateFlow(FastingPlanTemplates.PLAN_LUIGI_1)
    val activePlan: StateFlow<FastingPlan> = _activePlan.asStateFlow()

    private val _customPlans = MutableStateFlow<List<FastingPlan>>(emptyList())
    val customPlans: StateFlow<List<FastingPlan>> = _customPlans.asStateFlow()

    private val _currentSession = MutableStateFlow<FastingSession?>(null)
    val currentSession: StateFlow<FastingSession?> = _currentSession.asStateFlow()

    private val _sessionsHistory = MutableStateFlow<List<FastingSession>>(emptyList())
    val sessionsHistory: StateFlow<List<FastingSession>> = _sessionsHistory.asStateFlow()

    private val _waterEntries = MutableStateFlow<List<WaterEntry>>(emptyList())
    val waterEntries: StateFlow<List<WaterEntry>> = _waterEntries.asStateFlow()

    private val _weightEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()

    private val _trophies = MutableStateFlow<List<Trophy>>(initializeDefaultTrophies())
    val trophies: StateFlow<List<Trophy>> = _trophies.asStateFlow()

    private val _aiConfig = MutableStateFlow(AiConfig())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _coachingTips = MutableStateFlow<List<CoachingTip>>(initializeDefaultCoachingTips())
    val coachingTips: StateFlow<List<CoachingTip>> = _coachingTips.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val notifications: StateFlow<List<AppNotificationItem>> = _notifications.asStateFlow()

    init {
        loadStateFromDisk()
    }

    // ==========================================
    // NOTIFICATIONS ACTIONS (STORICO NOTIFICHE)
    // ==========================================

    fun addNotification(title: String, message: String, type: String = "GENERAL") {
        val item = AppNotificationItem(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            isRead = false
        )
        // Conserva le notifiche in ordine decrescente (max 100)
        _notifications.value = (listOf(item) + _notifications.value).take(100)
        saveStateToDisk()
    }

    fun deleteNotification(notificationId: String) {
        _notifications.value = _notifications.value.filter { it.id != notificationId }
        saveStateToDisk()
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
        saveStateToDisk()
    }

    fun markAllNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        saveStateToDisk()
    }

    // ==========================================
    // FASTING SESSION ACTIONS (CICLO CONTINUO SENZA INTERRUZIONI)
    // ==========================================

    /**
     * Avvia il ciclo continuo del timer (Digiuno <-> Alimentazione) tenendo sempre conto dell'ora attuale.
     * Se ci si trova in orario di digiuno programmato, avvia il digiuno;
     * altrimenti avvia la finestra di alimentazione fino al prossimo digiuno.
     */
    fun startFasting(customStartTime: Long? = null) {
        val now = customStartTime ?: System.currentTimeMillis()
        val plan = _activePlan.value
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)

        val (startTime, plannedEnd, durationMinutes, state) = if (scheduled.isNowInsideFasting) {
            val durMin = (scheduled.durationSeconds / 60L).toInt()
            val start = scheduled.periodStartEpochMillis
            val end = scheduled.periodEndEpochMillis
            listOf(start, end, durMin.toLong(), 1L)
        } else {
            val start = scheduled.eatingWindowStartMillis
            val end = scheduled.eatingWindowEndMillis
            val durMin = (scheduled.eatingDurationSeconds / 60L).toInt()
            listOf(start, end, durMin.toLong(), 2L)
        }

        val fastingState = if (state == 1L) FastingState.FASTING else FastingState.EATING

        val newSession = FastingSession(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = startTime,
            plannedEndTimeEpochMillis = plannedEnd,
            targetDurationMinutes = durationMinutes.toInt(),
            state = fastingState
        )

        _currentSession.value = newSession
        FastingForegroundService.start(
            context = context,
            startTime = newSession.startTimeEpochMillis,
            plannedEndTime = newSession.plannedEndTimeEpochMillis,
            planName = newSession.planName,
            isEating = (fastingState == FastingState.EATING)
        )

        if (fastingState == FastingState.FASTING) {
            NotificationHelper.cancelEatingAlarms(context)
            if (_userProfile.value.notificationsFastingEnabled) {
                NotificationHelper.scheduleFastingEndAlarm(context, newSession.plannedEndTimeEpochMillis, newSession.planName)
            }
            scheduleStageAlarmsForSession(newSession)
        } else {
            NotificationHelper.cancelFastingAlarms(context)
            if (_userProfile.value.notificationsFastingEnabled) {
                NotificationHelper.scheduleEatingWindowEndAlarm(context, newSession.plannedEndTimeEpochMillis, plan.name)
                NotificationHelper.scheduleEatingWindowPreAlarm(context, newSession.plannedEndTimeEpochMillis - 15 * 60_000L, plan.name)
            }
        }

        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Avvia immediatamente una sessione di digiuno forzando l'inizio a [now],
     * utile ad esempio se l'utente finisce di mangiare prima del previsto.
     */
    fun startFastingNow(customStartTime: Long? = null) {
        val now = customStartTime ?: System.currentTimeMillis()
        val plan = _activePlan.value
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)

        val durationMinutes = (scheduled.durationSeconds / 60L).toInt()
        val plannedEnd = now + (scheduled.durationSeconds * 1000L)

        val newSession = FastingSession(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = now,
            plannedEndTimeEpochMillis = plannedEnd,
            targetDurationMinutes = durationMinutes,
            state = FastingState.FASTING
        )

        _currentSession.value = newSession
        FastingForegroundService.start(
            context = context,
            startTime = newSession.startTimeEpochMillis,
            plannedEndTime = newSession.plannedEndTimeEpochMillis,
            planName = newSession.planName,
            isEating = false
        )

        NotificationHelper.cancelEatingAlarms(context)
        if (_userProfile.value.notificationsFastingEnabled) {
            NotificationHelper.scheduleFastingEndAlarm(context, newSession.plannedEndTimeEpochMillis, newSession.planName)
        }
        scheduleStageAlarmsForSession(newSession)

        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Transizione automatica dal Digiuno alla Finestra di Alimentazione (il timer non si ferma).
     */
    fun transitionToEating(now: Long = System.currentTimeMillis()) {
        val session = _currentSession.value
        val plan = _activePlan.value
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)

        // Se l'orario reale attuale è già dentro una nuova finestra di digiuno programmata, reindirizza al digiuno
        if (scheduled.isNowInsideFasting) {
            transitionToFasting(now)
            return
        }

        // 1. Archivia la sessione di digiuno terminata
        if (session != null && session.state == FastingState.FASTING && session.startTimeEpochMillis <= now) {
            val completedSession = session.copy(
                actualEndTimeEpochMillis = minOf(now, session.plannedEndTimeEpochMillis),
                state = FastingState.FASTING
            )
            _sessionsHistory.value = listOf(completedSession) + _sessionsHistory.value
            evaluateTrophies()
        }

        // 2. Calcola la finestra di alimentazione
        val eatingStart = if (session != null && session.plannedEndTimeEpochMillis <= now && session.plannedEndTimeEpochMillis >= scheduled.eatingWindowStartMillis) {
            session.plannedEndTimeEpochMillis
        } else {
            scheduled.eatingWindowStartMillis
        }
        val eatingEnd = scheduled.eatingWindowEndMillis
        val diffSec = maxOf(60L, (eatingEnd - eatingStart) / 1000L)
        val durationMinutes = (diffSec / 60L).toInt()

        val eatingSession = FastingSession(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = eatingStart,
            plannedEndTimeEpochMillis = eatingEnd,
            targetDurationMinutes = durationMinutes,
            state = FastingState.EATING
        )

        _currentSession.value = eatingSession
        FastingForegroundService.start(
            context = context,
            startTime = eatingStart,
            plannedEndTime = eatingEnd,
            planName = plan.name,
            isEating = true
        )

        NotificationHelper.cancelFastingAlarms(context)

        val locale = DateTimeFormatters.getActiveLocale(context)
        val eatingEndFormatted = DateTimeFormatters.formatTime(eatingEnd, context, locale)
        NotificationHelper.showNotification(
            context,
            NotificationHelper.CHANNEL_FASTING_ID,
            NotificationHelper.NOTIF_ID_TRANSITION_EATING,
            context.getString(R.string.notif_transition_eating_title),
            context.getString(R.string.notif_transition_eating_msg, eatingEndFormatted),
            type = "FASTING_END",
            isEating = true
        )

        NotificationHelper.scheduleEatingWindowEndAlarm(context, eatingEnd, plan.name)
        NotificationHelper.scheduleEatingWindowPreAlarm(context, eatingEnd - 15 * 60_000L, plan.name)

        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Transizione automatica dalla Finestra di Alimentazione al Digiuno (il timer non si ferma).
     */
    fun transitionToFasting(now: Long = System.currentTimeMillis()) {
        val plan = _activePlan.value
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)

        // Se l'orario reale attuale non è ancora dentro il digiuno programmato, reindirizza alla finestra pasto
        if (!scheduled.isNowInsideFasting) {
            transitionToEating(now)
            return
        }

        val fastingStart = scheduled.periodStartEpochMillis
        val fastingEnd = scheduled.periodEndEpochMillis
        val durationMinutes = (scheduled.durationSeconds / 60L).toInt()

        val fastingSession = FastingSession(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = fastingStart,
            plannedEndTimeEpochMillis = fastingEnd,
            targetDurationMinutes = durationMinutes,
            state = FastingState.FASTING
        )

        _currentSession.value = fastingSession
        FastingForegroundService.start(
            context = context,
            startTime = fastingStart,
            plannedEndTime = fastingEnd,
            planName = plan.name,
            isEating = false
        )

        NotificationHelper.cancelEatingAlarms(context)

        val locale = DateTimeFormatters.getActiveLocale(context)
        val fastingEndFormatted = DateTimeFormatters.formatTime(fastingEnd, context, locale)
        NotificationHelper.showNotification(
            context,
            NotificationHelper.CHANNEL_FASTING_ID,
            NotificationHelper.NOTIF_ID_TRANSITION_FASTING,
            context.getString(R.string.notif_transition_fasting_title),
            context.getString(R.string.notif_transition_fasting_msg, plan.name, fastingEndFormatted),
            type = "EATING_END",
            isEating = false
        )

        if (_userProfile.value.notificationsFastingEnabled) {
            NotificationHelper.scheduleFastingEndAlarm(context, fastingEnd, plan.name)
        }
        scheduleStageAlarmsForSession(fastingSession)

        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Verifica e avanza automaticamente il ciclo del timer se la finestra corrente è giunta al termine,
     * oppure corregge istantaneamente qualsiasi disallineamento dello stato rispetto alla realtà wall-clock
     * (es. sessione creata erroneamente nel futuro o mancata transizione dopo standby prolungato).
     * Garantisce che il timer non si fermi mai e sia sempre allineato al minuto esatto.
     */
    fun syncOrAdvanceSession(now: Long = System.currentTimeMillis()) {
        val session = _currentSession.value ?: return
        val plan = _activePlan.value
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)

        val isExpired = now >= session.plannedEndTimeEpochMillis
        val isFutureStart = session.startTimeEpochMillis > now
        val isMismatchedFuture = !scheduled.isNowInsideFasting && session.state == FastingState.FASTING && session.startTimeEpochMillis >= scheduled.periodStartEpochMillis

        if (isExpired || isFutureStart || isMismatchedFuture) {
            if (scheduled.isNowInsideFasting) {
                transitionToFasting(now)
            } else {
                transitionToEating(now)
            }
        }
    }

    /**
     * Arresta manualmente il ciclo del timer (su esplicita richiesta dell'utente tramite Stop).
     */
    fun stopTimer(customEndTime: Long? = null) {
        val session = _currentSession.value
        val now = customEndTime ?: System.currentTimeMillis()

        if (session != null && session.state == FastingState.FASTING) {
            val completedSession = session.copy(
                actualEndTimeEpochMillis = now,
                state = FastingState.FASTING
            )
            val updatedHistory = listOf(completedSession) + _sessionsHistory.value
            _sessionsHistory.value = updatedHistory
            evaluateTrophies()
        }

        _currentSession.value = null
        FastingForegroundService.stop(context)

        NotificationHelper.cancelFastingAlarms(context)
        NotificationHelper.cancelEatingAlarms(context)

        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Alias retrocompatibile per terminare/arrestare il timer.
     */
    fun endFasting(customEndTime: Long? = null) {
        stopTimer(customEndTime)
    }

    fun updateSessionTimes(newStartTimeEpochMillis: Long, newPlannedEndTimeEpochMillis: Long) {
        val session = _currentSession.value ?: return
        val diffMinutes = ((newPlannedEndTimeEpochMillis - newStartTimeEpochMillis) / (60 * 1000)).toInt()

        _currentSession.value = session.copy(
            startTimeEpochMillis = newStartTimeEpochMillis,
            plannedEndTimeEpochMillis = newPlannedEndTimeEpochMillis,
            targetDurationMinutes = if (diffMinutes > 0) diffMinutes else session.targetDurationMinutes
        )
        FastingForegroundService.start(
            context = context,
            startTime = newStartTimeEpochMillis,
            plannedEndTime = newPlannedEndTimeEpochMillis,
            planName = session.planName,
            isEating = (session.state == FastingState.EATING)
        )
        saveStateToDisk()
    }

    // ==========================================
    // FASTING PLAN ACTIONS
    // ==========================================

    fun setActivePlan(plan: FastingPlan) {
        val repaired = PlanShiftEngine.repairPlan(plan)
        _activePlan.value = repaired
        syncActiveSessionWithPlan(repaired)
        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Sposta tutti i 7 giorni del piano corrente di deltaHours (±1 ora),
     * preservando rigorosamente la durata del digiuno.
     */
    fun shiftPlanAllDays(deltaHours: Int) {
        val updatedPlan = PlanShiftEngine.shiftAllDays(_activePlan.value, deltaHours)
        _activePlan.value = updatedPlan
        syncActiveSessionWithPlan(updatedPlan)
        saveStateToDisk()
    }

    fun shiftPlanDay(dayOfWeek: Int, deltaHours: Int, deltaMinutes: Int = 0) {
        val updatedPlan = PlanShiftEngine.shiftDay(_activePlan.value, dayOfWeek, deltaHours, deltaMinutes)
        _activePlan.value = updatedPlan
        syncActiveSessionWithPlan(updatedPlan)
        saveStateToDisk()
    }

    fun adjustPlanDuration(dayOfWeek: Int, deltaMinutes: Int) {
        val updatedPlan = PlanShiftEngine.adjustDuration(_activePlan.value, dayOfWeek, deltaMinutes)
        _activePlan.value = updatedPlan
        syncActiveSessionWithPlan(updatedPlan)
        saveStateToDisk()
    }

    fun updateActivePlan(plan: FastingPlan) {
        val repaired = PlanShiftEngine.repairPlan(plan)
        _activePlan.value = repaired
        if (repaired.isCustom || _customPlans.value.any { it.id == repaired.id }) {
            val exists = _customPlans.value.any { it.id == repaired.id }
            _customPlans.value = if (exists) {
                _customPlans.value.map { if (it.id == repaired.id) repaired else it }
            } else {
                _customPlans.value + repaired
            }
        }
        syncActiveSessionWithPlan(repaired)
        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    fun saveCustomPlan(plan: FastingPlan, activate: Boolean = true) {
        val repaired = PlanShiftEngine.repairPlan(plan)
        val list = _customPlans.value.filter { it.id != repaired.id } + repaired
        _customPlans.value = list
        if (activate || _activePlan.value.id == repaired.id) {
            _activePlan.value = repaired
            syncActiveSessionWithPlan(repaired)
        }
        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Rinomina un piano (personalizzato o attivo), sincronizzando istantaneamente
     * il nome nelle sessioni correnti, nella notifica ongoing e negli allarmi schedulati.
     */
    fun renamePlan(planId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        _customPlans.value = _customPlans.value.map { plan ->
            if (plan.id == planId) plan.copy(name = trimmed) else plan
        }

        if (_activePlan.value.id == planId) {
            _activePlan.value = _activePlan.value.copy(name = trimmed)
        }

        val session = _currentSession.value
        if (session != null && (session.planId == planId || _activePlan.value.id == planId)) {
            val updatedSession = session.copy(planName = trimmed, planId = planId)
            _currentSession.value = updatedSession

            FastingForegroundService.start(
                context = context,
                startTime = updatedSession.startTimeEpochMillis,
                plannedEndTime = updatedSession.plannedEndTimeEpochMillis,
                planName = trimmed,
                isEating = (updatedSession.state == FastingState.EATING)
            )

            NotificationHelper.rescheduleAlarmsWithNewPlanName(
                context = context,
                session = updatedSession,
                planName = trimmed
            )
        }

        saveStateToDisk()
        triggerAutoBackupIfEnabled()
    }

    /**
     * Elimina un piano personalizzato salvato.
     * Se il piano eliminato è quello attivo, ripristina automaticamente il piano 16-8.
     */
    fun deletePlan(planId: String): Boolean {
        val currentCustom = _customPlans.value
        val toDelete = currentCustom.firstOrNull { it.id == planId } ?: return false
        _customPlans.value = currentCustom.filter { it.id != planId }

        if (_activePlan.value.id == planId) {
            val fallback = FastingPlanTemplates.PLAN_16_8
            _activePlan.value = fallback
            syncActiveSessionWithPlan(fallback)
        }

        saveStateToDisk()
        triggerAutoBackupIfEnabled()
        return true
    }

    private fun syncActiveSessionWithPlan(plan: FastingPlan) {
        val session = _currentSession.value ?: return
        val now = System.currentTimeMillis()
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)

        if (scheduled.isNowInsideFasting) {
            val newStart = scheduled.periodStartEpochMillis
            val newEnd = scheduled.periodEndEpochMillis
            val newDuration = (scheduled.durationSeconds / 60L).toInt()

            val updatedSession = session.copy(
                planId = plan.id,
                planName = plan.name,
                startTimeEpochMillis = newStart,
                plannedEndTimeEpochMillis = newEnd,
                targetDurationMinutes = newDuration,
                state = FastingState.FASTING
            )
            _currentSession.value = updatedSession
            FastingForegroundService.start(context, newStart, newEnd, plan.name, isEating = false)
            NotificationHelper.cancelEatingAlarms(context)
            NotificationHelper.rescheduleAlarmsWithNewPlanName(context, updatedSession, plan.name)
            if (_userProfile.value.notificationsFastingEnabled) {
                NotificationHelper.scheduleFastingEndAlarm(context, newEnd, plan.name)
            }
            scheduleStageAlarmsForSession(updatedSession)
            saveStateToDisk()
        } else {
            // Se la sessione precedente era un digiuno attivo valido, salvalo nello storico
            if (session.state == FastingState.FASTING && session.startTimeEpochMillis <= now) {
                val completedSession = session.copy(
                    actualEndTimeEpochMillis = now,
                    state = FastingState.FASTING
                )
                _sessionsHistory.value = listOf(completedSession) + _sessionsHistory.value
                evaluateTrophies()
            }

            val newStart = scheduled.eatingWindowStartMillis
            val newEnd = scheduled.eatingWindowEndMillis
            val newDuration = (scheduled.eatingDurationSeconds / 60L).toInt()

            val updatedSession = session.copy(
                planId = plan.id,
                planName = plan.name,
                startTimeEpochMillis = newStart,
                plannedEndTimeEpochMillis = newEnd,
                targetDurationMinutes = newDuration,
                state = FastingState.EATING
            )
            _currentSession.value = updatedSession
            FastingForegroundService.start(context, newStart, newEnd, plan.name, isEating = true)
            NotificationHelper.cancelFastingAlarms(context)
            NotificationHelper.rescheduleAlarmsWithNewPlanName(context, updatedSession, plan.name)
            NotificationHelper.scheduleEatingWindowEndAlarm(context, newEnd, plan.name)
            NotificationHelper.scheduleEatingWindowPreAlarm(context, newEnd - 15 * 60_000L, plan.name)
            saveStateToDisk()
        }
    }

    // ==========================================
    // WATER TRACKER ACTIONS
    // ==========================================

    fun addWater(amountMl: Int) {
        val entry = WaterEntry(
            id = UUID.randomUUID().toString(),
            epochMillis = System.currentTimeMillis(),
            amountMl = amountMl
        )
        _waterEntries.value = _waterEntries.value + entry
        evaluateTrophies()
        saveStateToDisk()
    }

    fun removeLastWater() {
        if (_waterEntries.value.isNotEmpty()) {
            _waterEntries.value = _waterEntries.value.dropLast(1)
            saveStateToDisk()
        }
    }

    fun getTodayWaterTotalMl(): Int {
        val startOfDay = getStartOfTodayEpochMillis()
        return _waterEntries.value
            .filter { it.epochMillis >= startOfDay }
            .sumOf { it.amountMl }
    }

    // ==========================================
    // WEIGHT TRACKER & MEASUREMENTS ACTIONS
    // ==========================================

    fun addWeightEntry(
        weightKg: Double,
        waistCm: Double? = null,
        hipsCm: Double? = null,
        chestCm: Double? = null,
        thighsCm: Double? = null,
        armsCm: Double? = null,
        notes: String = "",
        epochMillis: Long = System.currentTimeMillis()
    ) {
        val entry = WeightEntry(
            id = UUID.randomUUID().toString(),
            epochMillis = epochMillis,
            weightKg = weightKg,
            notes = notes,
            waistCm = waistCm,
            hipsCm = hipsCm,
            chestCm = chestCm,
            thighsCm = thighsCm,
            armsCm = armsCm
        )
        val updatedList = (listOf(entry) + _weightEntries.value).sortedByDescending { it.epochMillis }
        _weightEntries.value = updatedList
        val newest = updatedList.firstOrNull()
        if (newest != null) {
            _userProfile.value = _userProfile.value.copy(currentWeightKg = newest.weightKg)
        }

        evaluateTrophies()
        saveStateToDisk()
    }

    fun updateWeightEntry(
        id: String,
        newWeightKg: Double,
        newWaistCm: Double? = null,
        newHipsCm: Double? = null,
        newChestCm: Double? = null,
        newThighsCm: Double? = null,
        newArmsCm: Double? = null,
        newNotes: String = "",
        newEpochMillis: Long? = null
    ) {
        val list = _weightEntries.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = list[index]
            list[index] = old.copy(
                weightKg = newWeightKg,
                waistCm = newWaistCm,
                hipsCm = newHipsCm,
                chestCm = newChestCm,
                thighsCm = newThighsCm,
                armsCm = newArmsCm,
                notes = newNotes,
                epochMillis = newEpochMillis ?: old.epochMillis
            )
            val updatedList = list.sortedByDescending { it.epochMillis }
            _weightEntries.value = updatedList
            val newest = updatedList.firstOrNull()
            if (newest != null) {
                _userProfile.value = _userProfile.value.copy(currentWeightKg = newest.weightKg)
            }
            evaluateTrophies()
            saveStateToDisk()
        }
    }

    fun deleteWeightEntry(id: String) {
        val list = _weightEntries.value.filter { it.id != id }
        _weightEntries.value = list
        val newest = list.maxByOrNull { it.epochMillis }
        if (newest != null) {
            _userProfile.value = _userProfile.value.copy(currentWeightKg = newest.weightKg)
        } else {
            _userProfile.value = _userProfile.value.copy(currentWeightKg = _userProfile.value.startingWeightKg)
        }
        evaluateTrophies()
        saveStateToDisk()
    }

    fun updateGoals(startingWeightKg: Double, targetWeightKg: Double, heightCm: Double) {
        _userProfile.value = _userProfile.value.copy(
            startingWeightKg = startingWeightKg,
            targetWeightKg = targetWeightKg,
            heightCm = heightCm
        )
        evaluateTrophies()
        saveStateToDisk()
    }

    // ==========================================
    // USER PROFILE & SETTINGS
    // ==========================================

    fun scheduleStageAlarmsForSession(session: FastingSession) {
        if (!_userProfile.value.notificationsStagesEnabled) return
        if (session.state != FastingState.FASTING) return
        val locale = com.cibotimer.util.AppLanguageManager.getEffectiveLocale(context)
        for (stage in FastingCalculator.STAGES) {
            if (stage.startMinutes > 0 && stage.startMinutes < session.targetDurationMinutes) {
                val triggerMillis = session.startTimeEpochMillis + (stage.startMinutes * 60_000L)
                val hoursPassed = stage.startMinutes / 60
                val localizedTitle = stage.getLocalizedTitle(locale)
                val localizedDesc = stage.getLocalizedSubtitle(locale)
                val title = if (locale.language == "it") {
                    "Fase ${stage.stageIndex}: $localizedTitle (${hoursPassed}h passate)"
                } else {
                    "Stage ${stage.stageIndex}: $localizedTitle (${hoursPassed}h elapsed)"
                }
                NotificationHelper.scheduleStageAlarm(
                    context,
                    triggerMillis,
                    title,
                    localizedDesc,
                    NotificationHelper.REQUEST_CODE_STAGE_BASE + stage.stageIndex
                )
            }
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        val oldPersistent = _userProfile.value.notificationsPersistentEnabled
        val oldStages = _userProfile.value.notificationsStagesEnabled
        val oldFasting = _userProfile.value.notificationsFastingEnabled
        val oldLanguage = _userProfile.value.appLanguage

        _userProfile.value = profile
        FastingPersistence.setPersistentNotificationEnabled(context, profile.notificationsPersistentEnabled)

        if (oldPersistent != profile.notificationsPersistentEnabled) {
            val session = _currentSession.value
            if (profile.notificationsPersistentEnabled) {
                if (session != null) {
                    FastingForegroundService.start(
                        context = context,
                        startTime = session.startTimeEpochMillis,
                        plannedEndTime = session.plannedEndTimeEpochMillis,
                        planName = session.planName,
                        isEating = (session.state == FastingState.EATING)
                    )
                }
            } else {
                FastingForegroundService.stop(context)
            }
        }

        if (oldStages != profile.notificationsStagesEnabled) {
            if (!profile.notificationsStagesEnabled) {
                NotificationHelper.cancelStageAlarms(context)
            } else {
                val session = _currentSession.value
                if (session != null && session.state == FastingState.FASTING) {
                    scheduleStageAlarmsForSession(session)
                }
            }
        }

        if (oldFasting != profile.notificationsFastingEnabled) {
            if (!profile.notificationsFastingEnabled) {
                NotificationHelper.cancelFastingAlarms(context)
                NotificationHelper.cancelEatingAlarms(context)
            } else {
                val session = _currentSession.value
                if (session != null) {
                    if (session.state == FastingState.FASTING) {
                        NotificationHelper.scheduleFastingEndAlarm(context, session.plannedEndTimeEpochMillis, session.planName)
                    } else if (session.state == FastingState.EATING) {
                        NotificationHelper.scheduleEatingWindowEndAlarm(context, session.plannedEndTimeEpochMillis, session.planName)
                        NotificationHelper.scheduleEatingWindowPreAlarm(context, session.plannedEndTimeEpochMillis - 15 * 60_000L, session.planName)
                    }
                }
            }
        }

        if (oldLanguage != profile.appLanguage) {
            com.cibotimer.util.AppLanguageManager.setLanguage(context, profile.appLanguage)
        }

        saveStateToDisk()
    }

    fun updateAiConfig(config: AiConfig) {
        _aiConfig.value = config
        saveStateToDisk()
    }

    // ==========================================
    // AI CHAT COACH ACTIONS
    // ==========================================

    fun getOpenAiApiClient(): OpenAiApiClient = openAiApiClient

    fun addChatMessage(message: ChatMessage) {
        _chatMessages.value = _chatMessages.value + message
        saveStateToDisk()
    }

    fun updateChatMessage(id: String, text: String, thinking: String? = null, planJson: String? = null) {
        _chatMessages.value = _chatMessages.value.map {
            if (it.id == id) it.copy(text = text, thinking = thinking ?: it.thinking, proposedPlanJson = planJson ?: it.proposedPlanJson)
            else it
        }
        saveStateToDisk()
    }

    fun deleteChatMessage(id: String) {
        _chatMessages.value = _chatMessages.value.filter { it.id != id }
        saveStateToDisk()
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        saveStateToDisk()
    }

    suspend fun sendChatMessage(userText: String): Result<String> {
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = userText,
            timestampEpochMillis = System.currentTimeMillis()
        )
        addChatMessage(userMsg)

        val effectiveConfig = _aiConfig.value.copy(
            systemPrompt = buildCurrentAiCoachPrompt()
        )
        val result = openAiApiClient.sendMessage(effectiveConfig, _chatMessages.value, userText)
        return if (result.isSuccess) {
            val (aiText, proposedPlan) = result.getOrThrow()
            val proposedPlanJsonString = proposedPlan?.let { json.encodeToString(it) }

            val aiMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AI,
                text = aiText,
                timestampEpochMillis = System.currentTimeMillis(),
                proposedPlanJson = proposedPlanJsonString
            )
            addChatMessage(aiMsg)
            Result.success(aiText)
        } else {
            val error = result.exceptionOrNull()
            val errorMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.SYSTEM,
                text = "Errore: ${error?.localizedMessage ?: "Impossibile comunicare con il Coach AI."}",
                timestampEpochMillis = System.currentTimeMillis()
            )
            addChatMessage(errorMsg)
            Result.failure(error ?: Exception("Errore sconosciuto"))
        }
    }

    /**
     * Costruisce il system prompt completo per il Coach AI integrando in tempo reale:
     * - Il profilo corporeo completo (peso iniziale, peso attuale, peso target, altezza, BMI, progressi)
     * - Lo storico delle ultime pesate registrate e le misurazioni corporee
     * - L'idratazione odierna dell'utente
     * - Lo stato della sessione di digiuno in corso e la fase metabolica attuale
     * - Il piano di digiuno attualmente scelto dall'utente e tutti i suoi orari/periodi
     * - Lo storico generale e le statistiche dei digiuni
     * - I trofei sbloccati
     * - Le istruzioni per modificare il piano o salvarlo con un altro nome su richiesta dell'utente
     */
    fun buildCurrentAiCoachPrompt(basePrompt: String? = null): String {
        val base = basePrompt ?: _aiConfig.value.systemPrompt
        return com.cibotimer.domain.AiCoachPromptBuilder.buildPrompt(
            basePrompt = base,
            profile = _userProfile.value,
            activePlan = _activePlan.value,
            currentSession = _currentSession.value,
            weightEntries = _weightEntries.value,
            todayWaterMl = getTodayWaterTotalMl(),
            sessionsHistory = _sessionsHistory.value,
            trophies = _trophies.value,
            context = context,
            locale = DateTimeFormatters.getActiveLocale(context)
        )
    }

    /**
     * Salva o applica un piano proposto dall'AI.
     * Se [activate] è true, lo imposta come piano attivo nell'app.
     * Se [activate] è false, lo salva unicamente nei piani personalizzati per uso futuro.
     */
    fun applyProposedPlan(planJson: String, customName: String? = null, activate: Boolean = true): Boolean {
        return try {
            val decodedPlan = json.decodeFromString<FastingPlan>(planJson)
            val basePlan = com.cibotimer.data.remote.OpenAiApiClient.sanitizePlan(decodedPlan)
            val finalName = customName?.trim()?.takeIf { it.isNotBlank() } ?: basePlan.name
            val uniqueId = if (!customName.isNullOrBlank() || basePlan.id.isBlank() || basePlan.id.startsWith("16-8") || basePlan.id.startsWith("custom-ai")) {
                "custom-${UUID.randomUUID().toString().take(8)}"
            } else {
                basePlan.id
            }
            val planToSave = basePlan.copy(
                id = uniqueId,
                name = finalName,
                category = PlanCategory.CUSTOM,
                isCustom = true
            )
            saveCustomPlan(planToSave, activate = activate)
            if (activate) {
                setActivePlan(planToSave)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // BACKUP & GOOGLE DRIVE INTEGRATION
    // ==========================================

    fun generateBackupData(): BackupData {
        return BackupData(
            version = 1,
            exportTimestampEpochMillis = System.currentTimeMillis(),
            profile = _userProfile.value,
            activePlan = _activePlan.value,
            customPlans = _customPlans.value,
            sessions = _sessionsHistory.value + listOfNotNull(_currentSession.value),
            waterEntries = _waterEntries.value,
            weightEntries = _weightEntries.value,
            trophies = _trophies.value,
            aiConfig = _aiConfig.value,
            chatHistory = _chatMessages.value,
            appNotifications = _notifications.value
        )
    }

    fun exportBackupJson(): String {
        return json.encodeToString(generateBackupData())
    }

    fun importBackupJson(backupJson: String): Result<Unit> {
        return try {
            val backup = json.decodeFromString<BackupData>(backupJson)
            _userProfile.value = backup.profile
            FastingPersistence.setPersistentNotificationEnabled(context, backup.profile.notificationsPersistentEnabled)
            val activeRepaired = if (backup.activePlan.id == FastingPlanTemplates.PLAN_LUIGI_1.id) {
                FastingPlanTemplates.PLAN_LUIGI_1
            } else {
                PlanShiftEngine.repairPlan(backup.activePlan)
            }
            _activePlan.value = activeRepaired
            _customPlans.value = backup.customPlans.map { PlanShiftEngine.repairPlan(it) }
            _sessionsHistory.value = backup.sessions.filter { it.isCompleted }
            _currentSession.value = backup.sessions.firstOrNull { !it.isCompleted }
            _waterEntries.value = backup.waterEntries
            _weightEntries.value = backup.weightEntries
            _trophies.value = backup.trophies
            _aiConfig.value = backup.aiConfig
            _chatMessages.value = backup.chatHistory
            _notifications.value = backup.appNotifications

            val activeSess = _currentSession.value
            if (activeSess != null && !activeSess.isCompleted) {
                syncOrAdvanceSession(System.currentTimeMillis())
                val finalSess = _currentSession.value
                if (finalSess != null) {
                    FastingForegroundService.start(
                        context = context,
                        startTime = finalSess.startTimeEpochMillis,
                        plannedEndTime = finalSess.plannedEndTimeEpochMillis,
                        planName = finalSess.planName,
                        isEating = (finalSess.state == FastingState.EATING)
                    )
                }
            }

            saveStateToDisk()
            Result.success(Unit)
        } catch (e: SerializationException) {
            Result.failure(Exception("Formato file non valido. Assicurati che sia un backup JSON valido di CiboTimer.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ripristina il backup caricando direttamente un file JSON selezionato dall'utente
     * tramite il selettore file di sistema (Storage Access Framework / SAF).
     */
    fun importBackupFromUri(context: Context, uri: Uri): Result<Unit> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: return Result.failure(Exception(context.getString(R.string.settings_local_import_read_error)))

            if (jsonString.isBlank()) {
                return Result.failure(Exception(context.getString(R.string.settings_local_import_empty_error)))
            }

            importBackupJson(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backupToGoogleDrive(accessToken: String): Result<String> {
        val backupJson = exportBackupJson()
        return driveSyncClient.uploadBackupToDrive(accessToken, backupJson)
    }

    suspend fun restoreFromGoogleDrive(accessToken: String): Result<Unit> {
        val downloadResult = driveSyncClient.downloadBackupFromDrive(accessToken)
        return if (downloadResult.isSuccess) {
            importBackupJson(downloadResult.getOrThrow())
        } else {
            Result.failure(downloadResult.exceptionOrNull() ?: Exception("Errore download Google Drive"))
        }
    }

    /**
     * Esporta il backup in formato JSON e apre l'Intent ACTION_SEND nativo di Android
     * per inviare il file JSON verso Google Drive ("Salva su Drive"), Telegram, WhatsApp o altre app.
     */
    fun exportAndShareBackup(context: Context): Result<Unit> {
        return try {
            val jsonString = exportBackupJson()
            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "cibotimer_backup_$timeStamp.json"
            val backupFile = File(backupDir, fileName)
            backupFile.writeText(jsonString, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup CiboTimer - $timeStamp")
                putExtra(Intent.EXTRA_TEXT, jsonString)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Esporta o Condividi Backup CiboTimer").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback con testo semplice se la generazione del file URI non riesce
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Backup CiboTimer")
                    putExtra(Intent.EXTRA_TEXT, exportBackupJson())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Esporta Backup CiboTimer"))
                Result.success(Unit)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
        }
    }

    /**
     * Esegue una pulizia sicura e completa di tutti i dati locali dell'app:
     * 1. Elimina i database Room e SQLite dell'app (context.deleteDatabase(...))
     * 2. Cancella tutte le SharedPreferences locali
     * 3. Svuota la cache interna (context.cacheDir.deleteRecursively()) ed esterna
     * 4. Rimuove i file JSON di stato interno (cibotimer_app_state.json, etc.)
     * 5. Resetta tutti i flussi reattivi in memoria (StateFlow) allo stato iniziale
     * 6. Arresta il Foreground Service e cancella gli allarmi delle notifiche
     * 7. Salva il nuovo stato pulito e notifica i widget della home screen
     */
    fun resetAllLocalData(): Result<Unit> {
        return try {
            // 1. Arresta il servizio di timer in background e cancella gli allarmi
            try {
                FastingForegroundService.stop(context)
                NotificationHelper.cancelFastingAlarms(context)
                NotificationHelper.cancelEatingAlarms(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Elimina tutti i database Room / SQLite
            try {
                context.databaseList()?.forEach { dbName ->
                    context.deleteDatabase(dbName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            context.deleteDatabase("cibotimer.db")
            context.deleteDatabase("cibotimer_database")
            context.deleteDatabase("bodyfast.db")
            context.deleteDatabase("androidx.work.workdatabase")

            // 3. Cancella tutte le SharedPreferences locali
            try {
                val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                    sharedPrefsDir.listFiles()?.forEach { file ->
                        val prefName = file.nameWithoutExtension
                        context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().commit()
                    }
                    sharedPrefsDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            context.getSharedPreferences("cibotimer_update_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("cibotimer_prefs", Context.MODE_PRIVATE).edit().clear().commit()

            // 4. Svuota la cache interna ed esterna
            try {
                context.cacheDir.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 5. Rimuove i file di stato JSON interni
            try {
                if (stateFile.exists()) stateFile.delete()
                if (legacyStateFile.exists()) legacyStateFile.delete()
                val tmpFile = File(context.filesDir, "cibotimer_app_state.tmp")
                if (tmpFile.exists()) tmpFile.delete()
                val backupsDir = File(context.filesDir, "backups")
                if (backupsDir.exists()) backupsDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 6. Resetta lo stato in memoria ai valori predefiniti
            _userProfile.value = UserProfile()
            FastingPersistence.setPersistentNotificationEnabled(context, true)
            _activePlan.value = FastingPlanTemplates.PLAN_LUIGI_1
            _customPlans.value = emptyList()
            _currentSession.value = null
            _sessionsHistory.value = emptyList()
            _waterEntries.value = emptyList()
            _weightEntries.value = emptyList()
            _trophies.value = initializeDefaultTrophies()
            _aiConfig.value = AiConfig()
            _chatMessages.value = emptyList()
            _notifications.value = emptyList()

            // 7. Notifica il BackupManager di Android
            try {
                BackupManager.dataChanged(context.packageName)
            } catch (e: Exception) {
                // Non bloccante se BackupManager non è configurato sul dispositivo
            }

            // 8. Salva il nuovo stato iniziale pulito e aggiorna i widget
            saveStateToDisk()
            try {
                com.cibotimer.widget.CiboTimerWidgetProvider.updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun triggerAutoBackupIfEnabled() {
        val profile = _userProfile.value
        if (profile.googleDriveAutoBackupEnabled) {
            try {
                BackupManager.dataChanged(context.packageName)
            } catch (e: Exception) {
                // Non fatale
            }
        }
    }

    // ==========================================
    // PERSISTENCE (ATOMIC FILE WRITES)
    // ==========================================

    private fun saveStateToDisk() {
        scope.launch {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    try {
                        val backup = generateBackupData()
                        val jsonString = json.encodeToString(backup)
                        val tempFile = File(context.filesDir, "cibotimer_app_state.tmp")
                        tempFile.writeText(jsonString, Charsets.UTF_8)
                        if (stateFile.exists()) {
                            stateFile.delete()
                        }
                        if (!tempFile.renameTo(stateFile)) {
                            stateFile.writeText(jsonString, Charsets.UTF_8)
                            tempFile.delete()
                        }
                        com.cibotimer.widget.CiboTimerWidgetProvider.updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun loadStateFromDisk() {
        if (!stateFile.exists() && legacyStateFile.exists()) {
            try {
                legacyStateFile.copyTo(stateFile, overwrite = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!stateFile.exists()) return
        try {
            val content = stateFile.readText(Charsets.UTF_8)
            val backup = json.decodeFromString<BackupData>(content)
            _userProfile.value = backup.profile
            FastingPersistence.setPersistentNotificationEnabled(context, backup.profile.notificationsPersistentEnabled)
            val activeRepaired = if (backup.activePlan.id == FastingPlanTemplates.PLAN_LUIGI_1.id) {
                FastingPlanTemplates.PLAN_LUIGI_1
            } else {
                PlanShiftEngine.repairPlan(backup.activePlan)
            }
            _activePlan.value = activeRepaired
            _customPlans.value = backup.customPlans.map { PlanShiftEngine.repairPlan(it) }
            _sessionsHistory.value = backup.sessions.filter { it.isCompleted }
            _currentSession.value = backup.sessions.firstOrNull { !it.isCompleted }
            _waterEntries.value = backup.waterEntries
            _weightEntries.value = backup.weightEntries
            _trophies.value = if (backup.trophies.isNotEmpty()) backup.trophies else initializeDefaultTrophies()
            _aiConfig.value = backup.aiConfig
            _chatMessages.value = backup.chatHistory
            _notifications.value = backup.appNotifications

            val activeSess = _currentSession.value
            if (activeSess != null && !activeSess.isCompleted) {
                syncOrAdvanceSession(System.currentTimeMillis())
                val finalSess = _currentSession.value
                if (finalSess != null) {
                    FastingForegroundService.start(
                        context = context,
                        startTime = finalSess.startTimeEpochMillis,
                        plannedEndTime = finalSess.plannedEndTimeEpochMillis,
                        planName = finalSess.planName,
                        isEating = (finalSess.state == FastingState.EATING)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun evaluateTrophies() {
        val history = _sessionsHistory.value
        val completedCount = history.size
        val totalFastedHours = history.sumOf { FastingCalculator.calculateElapsedMinutes(it) / 60 }.toInt()
        val longestFastingHours = history.maxOfOrNull { (FastingCalculator.calculateElapsedMinutes(it) / 60).toInt() } ?: 0
        val weightEntriesCount = _weightEntries.value.size
        val startWeight = _userProfile.value.startingWeightKg
        val currentWeight = _userProfile.value.currentWeightKg
        val weightLossKg = (startWeight - currentWeight).coerceAtLeast(0.0)

        val updated = _trophies.value.map { trophy ->
            when (trophy.id) {
                "first_fast" -> trophy.copy(
                    currentProgress = if (completedCount >= 1) 1 else 0,
                    isUnlocked = completedCount >= 1,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (completedCount >= 1) System.currentTimeMillis() else null)
                )
                "seven_fasts" -> trophy.copy(
                    currentProgress = completedCount.coerceAtMost(7),
                    isUnlocked = completedCount >= 7,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (completedCount >= 7) System.currentTimeMillis() else null)
                )
                "hours_100" -> trophy.copy(
                    currentProgress = totalFastedHours.coerceAtMost(100),
                    isUnlocked = totalFastedHours >= 100,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (totalFastedHours >= 100) System.currentTimeMillis() else null)
                )
                "hours_500" -> trophy.copy(
                    currentProgress = totalFastedHours.coerceAtMost(500),
                    isUnlocked = totalFastedHours >= 500,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (totalFastedHours >= 500) System.currentTimeMillis() else null)
                )
                "hours_1000" -> trophy.copy(
                    currentProgress = totalFastedHours.coerceAtMost(1000),
                    isUnlocked = totalFastedHours >= 1000,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (totalFastedHours >= 1000) System.currentTimeMillis() else null)
                )
                "autophagy_master" -> trophy.copy(
                    currentProgress = if (longestFastingHours >= 16) 1 else 0,
                    isUnlocked = longestFastingHours >= 16,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (longestFastingHours >= 16) System.currentTimeMillis() else null)
                )
                "warrior" -> trophy.copy(
                    currentProgress = if (longestFastingHours >= 20) 1 else 0,
                    isUnlocked = longestFastingHours >= 20,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (longestFastingHours >= 20) System.currentTimeMillis() else null)
                )
                "track_weight" -> trophy.copy(
                    currentProgress = weightEntriesCount.coerceAtMost(3),
                    isUnlocked = weightEntriesCount >= 3,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (weightEntriesCount >= 3) System.currentTimeMillis() else null)
                )
                "weight_loss_1kg" -> trophy.copy(
                    currentProgress = if (weightLossKg >= 1.0) 1 else 0,
                    isUnlocked = weightLossKg >= 1.0,
                    unlockedDateEpochMillis = trophy.unlockedDateEpochMillis ?: (if (weightLossKg >= 1.0) System.currentTimeMillis() else null)
                )
                else -> trophy
            }
        }
        _trophies.value = updated
    }

    private fun getStartOfTodayEpochMillis(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun initializeDefaultTrophies(): List<Trophy> {
        return listOf(
            Trophy("first_fast", "Primo Passo", "Completa il tuo primo periodo di digiuno.", "star", 0, 1, titleEn = "First Step", descriptionEn = "Complete your first fasting period."),
            Trophy("seven_fasts", "Una Settimana", "Completa 7 digiuni con successo.", "calendar_month", 0, 7, titleEn = "One Week", descriptionEn = "Successfully complete 7 fasts."),
            Trophy("hours_100", "Centurione", "Raggiungi 100 ore complessive a digiuno.", "timer", 0, 100, titleEn = "Centurion", descriptionEn = "Reach 100 total fasting hours."),
            Trophy("hours_500", "Costanza d'Acciaio", "Raggiungi 500 ore complessive a digiuno.", "shield", 0, 500, titleEn = "Steel Consistency", descriptionEn = "Reach 500 total fasting hours."),
            Trophy("hours_1000", "Maestro del Digiuno", "Raggiungi 1000 ore complessive a digiuno.", "workspace_premium", 0, 1000, titleEn = "Master of Fasting", descriptionEn = "Reach 1000 total fasting hours."),
            Trophy("autophagy_master", "Autofagia Attiva", "Completa un digiuno di almeno 16 ore.", "biotech", 0, 1, titleEn = "Active Autophagy", descriptionEn = "Complete a fast of at least 16 hours."),
            Trophy("warrior", "Vero Guerriero", "Porta a termine un digiuno di 20 ore o più.", "military_tech", 0, 1, titleEn = "True Warrior", descriptionEn = "Complete a fast of 20 hours or more."),
            Trophy("track_weight", "Monitoraggio Attivo", "Registra il tuo peso per almeno 3 volte.", "monitor_weight", 0, 3, titleEn = "Active Tracking", descriptionEn = "Record your weight at least 3 times."),
            Trophy("weight_loss_1kg", "Traguardo Peso", "Riduci il tuo peso di almeno 1 kg dal punto di partenza.", "trending_down", 0, 1, titleEn = "Weight Milestone", descriptionEn = "Reduce your weight by at least 1 kg from your starting point."),
            Trophy("water_goal", "Idratazione Perfetta", "Bevi almeno 2 litri d'acqua in una giornata.", "water_drop", 0, 1, titleEn = "Perfect Hydration", descriptionEn = "Drink at least 2 liters of water in a day.")
        )
    }

    private fun initializeDefaultCoachingTips(): List<CoachingTip> {
        return listOf(
            CoachingTip(
                id = "tip_1",
                title = "Cos'è il Digiuno Intermittente?",
                subtitle = "I principi base per iniziare al meglio",
                category = "Basi Scientifiche",
                contentMarkdown = """
                    Il digiuno intermittente non è una dieta restrittiva, ma un **modello alimentare** che alterna periodi in cui si mangia a periodi in cui ci si astiene dal cibo.
                    
                    Durante il digiuno:
                    - I livelli di insulina scendono drasticamente, facilitando la combustione del grasso corporeo.
                    - I livelli di ormone della crescita (HGH) aumentano, preservando la massa muscolare magra.
                    - Il corpo attiva l'autofagia, un processo cellulare di pulizia e riciclo dei componenti danneggiati.
                """.trimIndent(),
                titleEn = "What is Intermittent Fasting?",
                subtitleEn = "Basic principles to get started right",
                categoryEn = "Scientific Basics",
                contentMarkdownEn = """
                    Intermittent fasting is not a restrictive diet, but an **eating pattern** cycling between periods of eating and fasting.
                    
                    During fasting:
                    - Insulin levels drop drastically, facilitating body fat burning.
                    - Human growth hormone (HGH) levels increase, preserving lean muscle mass.
                    - The body activates autophagy, a cellular cleanup and recycling process for damaged components.
                """.trimIndent()
            ),
            CoachingTip(
                id = "tip_2",
                title = "Cosa Bere Durante il Digiuno?",
                subtitle = "I liquidi consentiti senza interrompere la chetosi",
                category = "Nutrizione",
                contentMarkdown = """
                    Rimanere idratati è fondamentale per prevenire mal di testa, stanchezza e crampi.
                    
                    **Cosa puoi bere liberamente:**
                    - **Acqua**: naturale o frizzante (puoi aggiungere una fetta di limone o cetriolo).
                    - **Caffè amaro**: senza zucchero, latte o dolcificanti artificiali.
                    - **Tè e tisane**: tè verde, tè nero, camomilla, menta (rigorosamente non zuccherati).
                    - **Brodo di ossa o acqua con sale minerale**: per reintegrare sodio e magnesio nei digiuni lunghi.
                """.trimIndent(),
                titleEn = "What to Drink While Fasting?",
                subtitleEn = "Allowed beverages without breaking ketosis",
                categoryEn = "Nutrition",
                contentMarkdownEn = """
                    Staying hydrated is crucial to avoid headaches, fatigue, and muscle cramps.
                    
                    **What you can drink freely:**
                    - **Water**: still or sparkling (you can add a slice of lemon or cucumber).
                    - **Black coffee**: no sugar, milk, or artificial sweeteners.
                    - **Tea and herbal teas**: green tea, black tea, chamomile, peppermint (strictly unsweetened).
                    - **Bone broth or mineral-salted water**: to replenish sodium and magnesium during long fasts.
                """.trimIndent()
            ),
            CoachingTip(
                id = "tip_3",
                title = "Come Superare gli Attacchi di Fame",
                subtitle = "Strategie mentali e fisiche per resistere",
                category = "Consigli Pratici",
                contentMarkdown = """
                    La fame durante il digiuno non cresce all'infinito: arriva a **ondate** causate dall'ormone grelina, tipicamente negli orari in cui eri abituato a mangiare.
                    
                    **Strategie efficaci:**
                    1. **Bevi un grande bicchiere d'acqua calda o tisana**: distende le pareti dello stomaco.
                    2. **Muoviti o cambia stanza**: fai una passeggiata di 10 minuti per distrarre la mente.
                    3. **Ricordati che la fame dura 15-20 minuti**: passata l'ondata di grelina, la sensazione svanisce completamente.
                """.trimIndent(),
                titleEn = "How to Overcome Hunger Cravings",
                subtitleEn = "Mental and physical strategies to persevere",
                categoryEn = "Practical Tips",
                contentMarkdownEn = """
                    Hunger during a fast doesn't build up forever: it comes in **waves** triggered by the hormone ghrelin, usually around your usual meal times.
                    
                    **Effective strategies:**
                    1. **Drink a large cup of warm water or herbal tea**: gently expands the stomach lining.
                    2. **Move or change rooms**: take a 10-minute walk to distract your brain.
                    3. **Remember that hunger lasts 15-20 minutes**: once the ghrelin wave passes, the sensation disappears completely.
                """.trimIndent()
            ),
            CoachingTip(
                id = "tip_4",
                title = "Autofagia: Il Segreto della Longevità",
                subtitle = "Cosa accade dopo 16 ore di digiuno",
                category = "Scienza & Salute",
                contentMarkdown = """
                    La scoperta dei meccanismi dell'autofagia è valsa il Premio Nobel per la Medicina nel 2016 a Yoshinori Ohsumi.
                    
                    Quando il corpo non riceve nutrienti per oltre 16 ore, attiva un sistema di **"spazzini cellulari"**:
                    - Vengono eliminate proteine mal ripiegate e organelli disfunzionali.
                    - Vengono distrutti batteri e virus intracellulari.
                    - Si riducono l'infiammazione sistemica e lo stress ossidativo.
                """.trimIndent(),
                titleEn = "Autophagy: The Secret of Longevity",
                subtitleEn = "What happens after 16 hours of fasting",
                categoryEn = "Science & Health",
                contentMarkdownEn = """
                    The discovery of autophagy mechanisms earned Yoshinori Ohsumi the Nobel Prize in Medicine in 2016.
                    
                    When the body receives no nutrients for over 16 hours, it activates cellular **"cleanup crews"**:
                    - Misfolded proteins and dysfunctional organelles are dismantled.
                    - Intracellular bacteria and viruses are degraded.
                    - Systemic inflammation and oxidative stress are significantly reduced.
                """.trimIndent()
            ),
            CoachingTip(
                id = "tip_5",
                title = "Come Rompere il Digiuno (Refeeding)",
                subtitle = "Il pasto perfetto per non appesantire lo stomaco",
                category = "Nutrizione",
                contentMarkdown = """
                    Dopo molte ore di riposo dell'apparato digerente, è importante non sovraccaricare subito lo stomaco con carboidrati raffinati o cibi pesanti.
                    
                    **Ideale per rompere il digiuno:**
                    - Una manciata di noci o mandorle.
                    - Un uovo sodo o uova strapazzate con avocado.
                    - Una zuppa di verdure leggera o yogurt greco.
                    - Evita cibi fritti, zuccheri semplici e bibite gassate subito dopo il digiuno.
                """.trimIndent(),
                titleEn = "How to Break Your Fast (Refeeding)",
                subtitleEn = "The ideal meal to ease your digestion",
                categoryEn = "Nutrition",
                contentMarkdownEn = """
                    After hours of digestive rest, it is important not to overwhelm your stomach with refined carbs or heavy meals.
                    
                    **Ideal foods to break your fast:**
                    - A small handful of walnuts or almonds.
                    - A boiled or scrambled egg with avocado.
                    - A light vegetable soup or plain Greek yogurt.
                    - Avoid fried foods, simple sugars, and sodas right after your fast.
                """.trimIndent()
            )
        )
    }
}
