package com.cibotimer.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.Trophy
import com.cibotimer.data.models.WeightEntry
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.domain.BmiCalculator
import com.cibotimer.domain.BodyStatsCalculator
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.ui.components.*
import com.cibotimer.ui.theme.*
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val userProfile by repository.userProfile.collectAsState()
    val weightEntries by repository.weightEntries.collectAsState()
    val sessionsHistory by repository.sessionsHistory.collectAsState()
    val trophies by repository.trophies.collectAsState()

    var showAddWeightDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WeightEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<WeightEntry?>(null) }
    var showGoalEditorDialog by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Peso & Misure, 1: Digiuno, 2: Trofei

    // Calcolo progressi peso & obiettivi
    val currentEffectiveWeight = remember(weightEntries, userProfile.currentWeightKg) {
        weightEntries.firstOrNull()?.weightKg ?: userProfile.currentWeightKg
    }

    val weightProgressInfo = remember(userProfile.startingWeightKg, currentEffectiveWeight, userProfile.targetWeightKg) {
        BodyStatsCalculator.calculateWeightProgress(
            startingWeightKg = userProfile.startingWeightKg,
            currentWeightKg = currentEffectiveWeight,
            targetWeightKg = userProfile.targetWeightKg
        )
    }

    val bmiResult = remember(currentEffectiveWeight, userProfile.heightCm) {
        BmiCalculator.calculate(currentEffectiveWeight, userProfile.heightCm)
    }

    val circumferences = remember(weightEntries) {
        BodyStatsCalculator.calculateCircumferences(weightEntries)
    }

    val totalCircumferenceInfo = remember(weightEntries) {
        BodyStatsCalculator.calculateTotalCircumference(weightEntries)
    }

    // WHR calcolato sull'ultima pesata che ha sia vita che fianchi
    val latestWithWaistAndHips = remember(weightEntries) {
        weightEntries.firstOrNull { it.waistCm != null && it.hipsCm != null }
    }
    val whrResult = remember(latestWithWaistAndHips) {
        BodyStatsCalculator.calculateWhr(
            waistCm = latestWithWaistAndHips?.waistCm,
            hipsCm = latestWithWaistAndHips?.hipsCm
        )
    }

    val totalFastingHours = remember(sessionsHistory) {
        sessionsHistory.sumOf { FastingCalculator.calculateElapsedMinutes(it) / 60 }.toInt()
    }

    val longestFastingHours = remember(sessionsHistory) {
        sessionsHistory.maxOfOrNull { (FastingCalculator.calculateElapsedMinutes(it) / 60).toInt() } ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.nav_progress),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CiboTextPrimary
                        )
                    }
                },
                actions = {
                    // Pulsante "Condividi Progresso"
                    FilledTonalButton(
                        onClick = { showShareModal = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CiboTimerAzureContainer,
                            contentColor = CiboTimerAzureGlow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CiboTimerAzureGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.button_share),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CiboBg)
            )
        },
        containerColor = CiboBg,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CiboBg,
                contentColor = CiboTimerAzure,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CiboTimerAzureGlow
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Peso & Misure",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) CiboTextPrimary else CiboTextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Statistiche",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) CiboTextPrimary else CiboTextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Trofei (${trophies.count { it.isUnlocked }}/${trophies.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) CiboTextPrimary else CiboTextSecondary
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> WeightAndMeasurementsTab(
                    userProfile = userProfile,
                    weightEntries = weightEntries,
                    weightProgress = weightProgressInfo,
                    circumferences = circumferences,
                    whrResult = whrResult,
                    totalCircumference = totalCircumferenceInfo,
                    bmiResult = bmiResult,
                    onOpenAddWeight = { showAddWeightDialog = true },
                    onOpenEditGoals = { showGoalEditorDialog = true },
                    onOpenShareModal = { showShareModal = true },
                    onEditWeight = { entryToEdit = it },
                    onDeleteWeight = { entryToDelete = it }
                )
                1 -> FastingStatsTab(
                    completedFastsCount = sessionsHistory.size,
                    totalFastingHours = totalFastingHours,
                    longestFastingHours = longestFastingHours
                )
                2 -> TrophiesTab(trophies = trophies)
            }
        }
    }

    // Modal Inserimento Misura
    if (showAddWeightDialog) {
        MeasurementEntryDialog(
            defaultWeight = currentEffectiveWeight,
            onDismiss = { showAddWeightDialog = false },
            onSave = { weight, waist, hips, chest, thighs, arms, notes, epoch ->
                repository.addWeightEntry(
                    weightKg = weight,
                    waistCm = waist,
                    hipsCm = hips,
                    chestCm = chest,
                    thighsCm = thighs,
                    armsCm = arms,
                    notes = notes,
                    epochMillis = epoch
                )
                showAddWeightDialog = false
            }
        )
    }

    // Modal Modifica Misura
    if (entryToEdit != null) {
        MeasurementEntryDialog(
            initialEntry = entryToEdit,
            onDismiss = { entryToEdit = null },
            onSave = { weight, waist, hips, chest, thighs, arms, notes, epoch ->
                repository.updateWeightEntry(
                    id = entryToEdit!!.id,
                    newWeightKg = weight,
                    newWaistCm = waist,
                    newHipsCm = hips,
                    newChestCm = chest,
                    newThighsCm = thighs,
                    newArmsCm = arms,
                    newNotes = notes,
                    newEpochMillis = epoch
                )
                entryToEdit = null
            },
            onDelete = {
                entryToDelete = entryToEdit
                entryToEdit = null
            }
        )
    }

    // Dialog Conferma Eliminazione
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            containerColor = CiboSheetBg,
            titleContentColor = CiboTextPrimary,
            textContentColor = CiboTextSecondary,
            title = { Text(stringResource(R.string.dialog_delete_measurement_title)) },
            text = { Text(stringResource(R.string.dialog_delete_measurement_message, entryToDelete!!.weightKg)) },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteWeightEntry(entryToDelete!!.id)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.button_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.button_cancel), color = CiboTextSecondary)
                }
            }
        )
    }

    // Dialog Modifica Obiettivi
    if (showGoalEditorDialog) {
        GoalEditorDialog(
            initialStartingWeightKg = userProfile.startingWeightKg,
            initialTargetWeightKg = userProfile.targetWeightKg,
            initialHeightCm = userProfile.heightCm,
            onDismiss = { showGoalEditorDialog = false },
            onSave = { starting, target, height ->
                repository.updateGoals(starting, target, height)
                showGoalEditorDialog = false
            }
        )
    }

    // Modal Condividi Progresso
    if (showShareModal) {
        ShareProgressModal(
            progressInfo = weightProgressInfo,
            totalFastingHours = totalFastingHours,
            completedSessionsCount = sessionsHistory.size,
            circumferenceInfo = totalCircumferenceInfo,
            onDismiss = { showShareModal = false }
        )
    }
}

@Composable
fun WeightAndMeasurementsTab(
    userProfile: com.cibotimer.data.models.UserProfile,
    weightEntries: List<WeightEntry>,
    weightProgress: com.cibotimer.domain.WeightProgressInfo,
    circumferences: List<com.cibotimer.domain.CircumferenceMetric>,
    whrResult: com.cibotimer.domain.WhrResult?,
    totalCircumference: com.cibotimer.domain.TotalCircumferenceInfo?,
    bmiResult: com.cibotimer.domain.BmiResult,
    onOpenAddWeight: () -> Unit,
    onOpenEditGoals: () -> Unit,
    onOpenShareModal: () -> Unit,
    onEditWeight: (WeightEntry) -> Unit,
    onDeleteWeight: (WeightEntry) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CARD OBIETTIVI & PANORAMICA PESO
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CiboCard,
                border = BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.progress_goals_weight_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = CiboTextPrimary
                        )

                        TextButton(
                            onClick = onOpenEditGoals,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CiboTimerAzureGlow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.button_edit),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CiboTimerAzureGlow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Colonne: Iniziale | Attuale | Obiettivo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.progress_initial_label), fontSize = 11.sp, color = CiboTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%.1f kg".format(Locale.US, weightProgress.startingWeightKg),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CiboTextPrimary
                            )
                        }

                        // Attuale Hero
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CiboTimerAzureContainer,
                            border = BorderStroke(1.dp, CiboTimerAzureGlow.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(stringResource(R.string.progress_current_label), fontSize = 11.sp, color = CiboTimerAzureLight)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "%.1f kg".format(Locale.US, weightProgress.currentWeightKg),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = CiboTimerAzureGlow
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.progress_target_label), fontSize = 11.sp, color = CiboTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%.1f kg".format(Locale.US, weightProgress.targetWeightKg),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CiboTimerAzure
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Due badge: Kg Persi / Aumentati e Kg Rimanenti
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CiboBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (weightProgress.lostKg >= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (weightProgress.lostKg >= 0) CiboTimerAzureGlow else CiboTimerOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (weightProgress.lostKg >= 0) stringResource(R.string.progress_lost_label) else stringResource(R.string.progress_gained_label),
                                        fontSize = 10.sp,
                                        color = CiboTextSecondary
                                    )
                                    Text(
                                        text = "%+.1f kg".format(Locale.US, -weightProgress.lostKg),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (weightProgress.lostKg >= 0) CiboTimerAzureGlow else CiboTimerOrange
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CiboBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = CiboTimerAzure,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.progress_to_target_label),
                                        fontSize = 10.sp,
                                        color = CiboTextSecondary
                                    )
                                    Text(
                                        text = if (weightProgress.isGoalReached) stringResource(R.string.progress_goal_reached)
                                        else "%.1f kg".format(Locale.US, weightProgress.remainingKg),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (weightProgress.isGoalReached) CiboTimerAzureGlow else CiboTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Linear Progress Bar verso l'obiettivo
                    LinearProgressIndicator(
                        progress = { weightProgress.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CiboTimerAzureGlow,
                        trackColor = CiboTrack
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (weightProgress.isGoalReached) "🎉 Obiettivo Raggiunto!"
                            else "${weightProgress.progressPercent}% verso l'obiettivo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (weightProgress.isGoalReached) CiboTimerAzureGlow else CiboTextSecondary
                        )

                        Text(
                            text = stringResource(R.string.share_target_format, weightProgress.targetWeightKg),
                            fontSize = 11.sp,
                            color = CiboTextSecondary
                        )
                    }
                }
            }
        }

        // 2. GRAFICO EVOLUZIONE (WEIGHT & CIRCONFERENZE)
        item {
            WeightProgressChart(
                entries = weightEntries,
                targetWeightKg = userProfile.targetWeightKg
            )
        }

        // 3. MISURE CORPOREE (CIRCONFERENZE & WHR)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CiboCard,
                border = BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = CiboTimerAzureGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.progress_measurements_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CiboTextPrimary
                            )
                        }

                        if (totalCircumference?.totalDiffCm != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CiboTimerAzureContainer,
                                border = BorderStroke(1.dp, CiboTimerAzureGlow.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = stringResource(R.string.progress_total_diff_format, totalCircumference.totalDiffCm),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalCircumference.totalDiffCm <= 0) CiboTimerAzureGlow else CiboTimerOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Griglia Circonferenze (Vita, Fianchi, Torace, Cosce, Braccia)
                    val activeCircumferences = circumferences.filter { it.currentCm != null }
                    if (activeCircumferences.isEmpty()) {
                        Text(
                            text = stringResource(R.string.progress_no_circumferences_desc),
                            fontSize = 12.sp,
                            color = CiboTextSecondary,
                            lineHeight = 17.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeCircumferences.chunked(2).forEach { rowMetrics ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowMetrics.forEach { metric ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CiboBg,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(metric.label, fontSize = 11.sp, color = CiboTextSecondary)
                                                    Text(
                                                        text = "%.0f cm".format(Locale.US, metric.currentCm),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = CiboTextPrimary
                                                    )
                                                }

                                                if (metric.diffCm != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (metric.diffCm <= 0) CiboTimerAzureContainer else CiboTimerOrangeContainer
                                                    ) {
                                                        Text(
                                                            text = "%+.1f cm".format(Locale.US, metric.diffCm),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (metric.diffCm <= 0) CiboTimerAzureGlow else CiboTimerOrange,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (rowMetrics.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // WHR Card (Rapporto Vita / Fianchi)
                    if (whrResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CiboBg,
                            border = BorderStroke(1.dp, Color(whrResult.colorHex).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = stringResource(R.string.measurement_whr_prefix), fontSize = 12.sp, color = CiboTextSecondary)
                                        Text(
                                            text = "%.2f".format(Locale.US, whrResult.ratio),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = CiboTextPrimary
                                        )
                                    }
                                    Text(
                                        text = whrResult.category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(whrResult.colorHex)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = whrResult.description,
                                    fontSize = 11.sp,
                                    color = CiboTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. BMI CARD
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CiboCard,
                border = BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(bmiResult.colorHex).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%.1f".format(Locale.US, bmiResult.bmiValue),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(bmiResult.colorHex),
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bmi_title),
                            fontSize = 11.sp,
                            color = CiboTextSecondary
                        )
                        Text(
                            text = bmiResult.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(bmiResult.colorHex)
                        )
                        Text(
                            text = stringResource(R.string.bmi_height_format, userProfile.heightCm),
                            fontSize = 11.sp,
                            color = CiboTextSecondary
                        )
                    }
                }
            }
        }

        // 5. PULSANTE REGISTRA NUOVA MISURA
        item {
            Button(
                onClick = onOpenAddWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CiboTimerAzure,
                    contentColor = Color(0xFF031019)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF031019))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.progress_record_button), fontWeight = FontWeight.ExtraBold, color = Color(0xFF031019))
            }
        }

        // 6. STORICO MISURAZIONI
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_history_count_format, weightEntries.size),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CiboTextPrimary
                )
            }
        }

        if (weightEntries.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.progress_no_weight_desc),
                    fontSize = 13.sp,
                    color = CiboTextSecondary
                )
            }
        } else {
            items(weightEntries, key = { it.id }) { entry ->
                // Calcola delta rispetto alla precedente cronologica se disponibile
                val entryIndex = weightEntries.indexOf(entry)
                val prevEntry = if (entryIndex < weightEntries.size - 1) weightEntries[entryIndex + 1] else null
                val weightDelta = if (prevEntry != null) entry.weightKg - prevEntry.weightKg else null

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CiboCard,
                    border = BorderStroke(1.dp, CiboCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%.1f kg".format(Locale.US, entry.weightKg),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = CiboTextPrimary
                                )

                                if (weightDelta != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "%+.1f kg".format(Locale.US, weightDelta),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (weightDelta <= 0) CiboTimerAzureGlow else CiboTimerOrange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateFormat.format(Date(entry.epochMillis)),
                                fontSize = 11.sp,
                                color = CiboTextSecondary
                            )

                            // Dettaglio Misure registrate (Vita, Fianchi, Torace, Cosce, Braccia)
                            val measurementsText = buildString {
                                entry.waistCm?.let { append("Vita: %.0f cm  ".format(Locale.US, it)) }
                                entry.hipsCm?.let { append("Fianchi: %.0f cm  ".format(Locale.US, it)) }
                                entry.chestCm?.let { append("Torace: %.0f cm  ".format(Locale.US, it)) }
                                entry.thighsCm?.let { append("Cosce: %.0f cm  ".format(Locale.US, it)) }
                                entry.armsCm?.let { append("Braccia: %.0f cm".format(Locale.US, it)) }
                            }.trim()

                            if (measurementsText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = measurementsText,
                                    fontSize = 11.sp,
                                    color = CiboTimerAzureLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (entry.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.notes,
                                    fontSize = 11.sp,
                                    color = CiboTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onEditWeight(entry) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifica",
                                    tint = CiboTimerAzure,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteWeight(entry) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Elimina",
                                    tint = Color(0xFFEF4444),
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

@Composable
fun FastingStatsTab(
    completedFastsCount: Int,
    totalFastingHours: Int,
    longestFastingHours: Int
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.progress_stats_section_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CiboTextPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.progress_stat_fasts),
                    value = "$completedFastsCount",
                    icon = Icons.Default.CheckCircle,
                    color = CiboTimerAzureGlow,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.progress_stat_hours),
                    value = "$totalFastingHours h",
                    icon = Icons.Default.Schedule,
                    color = CiboTimerAzure,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatCard(
                title = stringResource(R.string.progress_stat_longest),
                value = stringResource(R.string.progress_stat_consecutive_hours, longestFastingHours),
                icon = Icons.Default.EmojiEvents,
                color = CiboTimerOrange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CiboCard,
                border = BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = CiboTimerAzureGlow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.progress_consistency_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CiboTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.progress_consistency_desc),
                        fontSize = 13.sp,
                        color = CiboTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CiboCard,
        border = BorderStroke(1.dp, CiboCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontSize = 12.sp, color = CiboTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CiboTextPrimary)
        }
    }
}

@Composable
fun TrophiesTab(trophies: List<Trophy>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.progress_trophies_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CiboTextPrimary
            )
            Text(
                text = stringResource(R.string.progress_trophies_desc),
                fontSize = 13.sp,
                color = CiboTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(trophies, key = { it.id }) { trophy ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (trophy.isUnlocked) CiboCard else CiboBg,
                border = if (trophy.isUnlocked) BorderStroke(1.5.dp, CiboTimerAzureGlow) else BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (trophy.isUnlocked) CiboTimerAzureContainer else CiboTrack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (trophy.isUnlocked) CiboTimerAzureGlow else CiboTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trophy.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (trophy.isUnlocked) CiboTextPrimary else CiboTextSecondary
                            )
                            if (trophy.isUnlocked) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CiboTimerAzureContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.progress_trophy_unlocked_badge),
                                        color = CiboTimerAzureGlow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = trophy.description,
                            fontSize = 12.sp,
                            color = CiboTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { trophy.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CiboTimerAzureGlow,
                            trackColor = CiboTrack
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${trophy.currentProgress} / ${trophy.maxProgress}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CiboTextSecondary
                        )
                    }
                }
            }
        }
    }
}
