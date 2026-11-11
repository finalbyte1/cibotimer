package com.cibotimer.ui.components

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cibotimer.R
import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.PlanCategory
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.PlanShiftEngine
import com.cibotimer.domain.TimelineSegment
import com.cibotimer.ui.theme.*

private const val MIN_DURATION_MINUTES = 60 // 1 ora minimo
private const val MAX_DURATION_MINUTES = 24 * 60 // 24 ore massimo

/**
 * Bottom sheet per la modifica del piano di digiuno (Pianificatore)
 * con istogramma a capsule verticali 24 ore disposte orizzontalmente per i 7 giorni,
 * colori della home (Digiuno = Azzurro, Pasto = Ambra),
 * lista compatta dei periodi e scheda inferiore con orari modificabili e steppers.
 * Dimensionato per stare comodamente nello schermo in stile CiboTimer con grafica OLED accattivante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeEditorModal(
    dayPlan: DayPlan,
    plan: FastingPlan? = null,
    initialDayOfWeek: Int? = null,
    onSavePlan: ((FastingPlan) -> Unit)? = null,
    onSaveCustomPlan: ((FastingPlan, Boolean) -> Unit)? = null,
    onShiftAllDays: (Int) -> Unit = {},
    onShiftSingleDay: (Int, Int) -> Unit = { _, _ -> },
    onAdjustDuration: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val originalPlan = remember(plan, dayPlan) {
        plan ?: FastingPlan(
            id = "custom_plan",
            name = "Personalizzato",
            days = (1..7).map { d ->
                dayPlan.copy(dayOfWeek = d)
            }
        )
    }

    var workingPlan by remember(originalPlan) { mutableStateOf(originalPlan) }
    val effectivePeriods = workingPlan.getEffectivePeriods()

    // Indice periodo selezionato: default = 1 (oppure matching del giorno iniziale)
    var selectedPeriodNumber by remember {
        val initialMatch = if (initialDayOfWeek != null) {
            effectivePeriods.find { it.startDayOfWeek == initialDayOfWeek }?.periodNumber ?: 1
        } else {
            1
        }
        mutableStateOf<Int?>(initialMatch)
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }

    val defaultSaveAsName = remember(workingPlan, originalPlan) {
        if (originalPlan.isCustom) {
            originalPlan.name
        } else {
            "${originalPlan.name} Personalizzato"
        }
    }
    var saveAsName by remember(workingPlan, originalPlan) { mutableStateOf(defaultSaveAsName) }
    var saveAsDescription by remember(workingPlan) { mutableStateOf(workingPlan.description) }

    val hasUnsavedChanges = workingPlan != originalPlan

    Dialog(
        onDismissRequest = {
            if (hasUnsavedChanges) {
                showDiscardDialog = true
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = CiboBg
        ) {
            val currentPeriods = workingPlan.getEffectivePeriods()
            val previewPeriod = remember(currentPeriods, selectedPeriodNumber) {
                if (selectedPeriodNumber != null) {
                    currentPeriods.find { it.periodNumber == selectedPeriodNumber } ?: currentPeriods.first()
                } else {
                    currentPeriods.first()
                }
            }

            TimeEditorSheetContent(
                workingPlan = workingPlan,
                selectedPeriodNumber = selectedPeriodNumber,
                previewPeriod = previewPeriod,
                onSelectPeriod = { selectedPeriodNumber = it },
                onShiftTimes = { deltaHours, deltaMinutes ->
                    workingPlan = if (selectedPeriodNumber == null) {
                        PlanShiftEngine.shiftAllDays(workingPlan, deltaHours, deltaMinutes)
                    } else {
                        PlanShiftEngine.shiftPeriod(workingPlan, selectedPeriodNumber!!, deltaHours, deltaMinutes, cascadeNext = false)
                    }
                },
                onAdjustDuration = { deltaMinutes ->
                    workingPlan = if (selectedPeriodNumber == null) {
                        PlanShiftEngine.adjustDurationAllDays(workingPlan, deltaMinutes)
                    } else {
                        PlanShiftEngine.adjustPeriodDuration(workingPlan, selectedPeriodNumber!!, deltaMinutes, cascadeNext = false)
                    }
                },
                onUpdateStartOrEndTime = { newHour, newMinute, isStart ->
                    workingPlan = if (selectedPeriodNumber != null) {
                        PlanShiftEngine.updatePeriodTime(workingPlan, selectedPeriodNumber!!, newHour, newMinute, isStart, cascadeNext = false)
                    } else {
                        val currentTotal = if (isStart) previewPeriod.startHour * 60 + previewPeriod.startMinute else previewPeriod.endHour * 60 + previewPeriod.endMinute
                        val newTotal = newHour * 60 + newMinute
                        val deltaMinutes = (newTotal - currentTotal + 24 * 60 * 10) % (24 * 60)
                        val signedDelta = if (deltaMinutes > 12 * 60) deltaMinutes - 24 * 60 else deltaMinutes
                        PlanShiftEngine.shiftAllDays(workingPlan, 0, signedDelta)
                    }
                },
                onSave = {
                    showSaveAsDialog = true
                },
                onClose = {
                    if (hasUnsavedChanges) {
                        showDiscardDialog = true
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Modal Salva con Nome (Save As)
    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            containerColor = CiboSheetCard,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = CiboAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salva con nome",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CiboTextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Assegna un nuovo nome a questo piano per salvarlo nei tuoi piani personalizzati:",
                        fontSize = 13.sp,
                        color = CiboTextSecondary,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = saveAsName,
                        onValueChange = { saveAsName = it },
                        label = { Text("Nome del piano", color = CiboAccent) },
                        placeholder = { Text("es. 16-8 Personalizzato", color = CiboTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CiboAccent,
                            unfocusedBorderColor = CiboCardBorder,
                            focusedTextColor = CiboTextPrimary,
                            unfocusedTextColor = CiboTextPrimary,
                            cursorColor = CiboAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = saveAsDescription,
                        onValueChange = { saveAsDescription = it },
                        label = { Text("Descrizione (opzionale)", color = CiboTextSecondary) },
                        placeholder = { Text("Note o orari personalizzati", color = CiboTextSecondary) },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CiboAccent,
                            unfocusedBorderColor = CiboCardBorder,
                            focusedTextColor = CiboTextPrimary,
                            unfocusedTextColor = CiboTextPrimary,
                            cursorColor = CiboAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val uniqueId = "custom-${java.util.UUID.randomUUID().toString().take(8)}"
                            val finalName = saveAsName.trim().ifBlank { defaultSaveAsName }
                            val newPlan = workingPlan.copy(
                                id = uniqueId,
                                name = finalName,
                                description = saveAsDescription.trim(),
                                category = PlanCategory.CUSTOM,
                                isCustom = true
                            )
                            if (onSaveCustomPlan != null) {
                                onSaveCustomPlan(newPlan, true)
                            } else {
                                onSavePlan?.invoke(newPlan)
                            }
                            showSaveAsDialog = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CiboAccent,
                            contentColor = CiboButtonDarkText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(
                            text = "Salva e Attiva",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val uniqueId = "custom-${java.util.UUID.randomUUID().toString().take(8)}"
                                val finalName = saveAsName.trim().ifBlank { defaultSaveAsName }
                                val newPlan = workingPlan.copy(
                                    id = uniqueId,
                                    name = finalName,
                                    description = saveAsDescription.trim(),
                                    category = PlanCategory.CUSTOM,
                                    isCustom = true
                                )
                                if (onSaveCustomPlan != null) {
                                    onSaveCustomPlan(newPlan, false)
                                } else {
                                    onSavePlan?.invoke(newPlan)
                                }
                                showSaveAsDialog = false
                                onDismiss()
                            },
                            border = BorderStroke(1.dp, CiboAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(
                                text = "Solo Salva",
                                color = CiboAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (originalPlan.isCustom) {
                            TextButton(
                                onClick = {
                                    val updated = workingPlan.copy(
                                        id = originalPlan.id,
                                        name = saveAsName.trim().ifBlank { originalPlan.name },
                                        description = saveAsDescription.trim(),
                                        category = PlanCategory.CUSTOM,
                                        isCustom = true
                                    )
                                    onSavePlan?.invoke(updated)
                                    showSaveAsDialog = false
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text(
                                    text = "Sovrascrivi",
                                    color = CiboAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveAsDialog = false }
                ) {
                    Text(
                        text = "Annulla",
                        color = CiboTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = CiboCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(R.string.plan_editor_unsaved_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CiboTextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.plan_editor_unsaved_message),
                    fontSize = 14.sp,
                    color = CiboTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.plan_editor_unsaved_discard),
                        color = CiboStopRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            showSaveAsDialog = true
                        }
                    ) {
                        Text(
                            text = "Salva con nome",
                            color = CiboAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { showDiscardDialog = false }
                    ) {
                        Text(
                            text = stringResource(R.string.plan_editor_unsaved_stay),
                            color = CiboTextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        )
    }
}

/**
 * Contenuto principale del bottom sheet con layout compatto (fit screen).
 */
@Composable
fun TimeEditorSheetContent(
    workingPlan: FastingPlan,
    selectedPeriodNumber: Int?,
    previewPeriod: FastingPeriod,
    onSelectPeriod: (Int?) -> Unit,
    onShiftTimes: (Int, Int) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onUpdateStartOrEndTime: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectivePeriods = workingPlan.getEffectivePeriods()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. HEADER (44dp): X Chiudi | Titolo "Modifica piano" | Pulsante "Salva" pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.plan_editor_close),
                    tint = CiboTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = stringResource(R.string.plan_editor_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CiboTextPrimary
            )

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CiboAccent,
                    contentColor = CiboButtonDarkText
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = CiboButtonDarkText,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Salva con nome",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboButtonDarkText
                )
            }
        }

        // 2. PIANO SETTIMANALE A BARRE ORIZZONTALI (Lun..Dom) 0-24h con diciture orari sotto (es: Lun 20:00 → Mar 12:00)
        WeekBarChart(
            days = workingPlan.days,
            selectedDayOfWeek = selectedPeriodNumber ?: 1,
            periods = effectivePeriods,
            onDaySelected = { dayIndex ->
                val matching = effectivePeriods.find { it.startDayOfWeek == dayIndex }
                    ?: effectivePeriods.find { it.endDayOfWeek == dayIndex }
                    ?: effectivePeriods.firstOrNull()
                if (matching != null) {
                    if (selectedPeriodNumber == matching.periodNumber) {
                        onSelectPeriod(null) // Tocco di nuovo sul selezionato = Tutti i periodi
                    } else {
                        onSelectPeriod(matching.periodNumber)
                    }
                }
            },
            fillMaxHeight = false,
            modifier = Modifier.fillMaxWidth()
        )

        val periodBounds = remember(workingPlan, selectedPeriodNumber, previewPeriod) {
            if (selectedPeriodNumber != null) {
                PlanShiftEngine.getPeriodBounds(workingPlan, selectedPeriodNumber)
            } else {
                PlanShiftEngine.PeriodBounds()
            }
        }

        // 3. SCHEDA INFERIORE CONTROLLI & STEPPERS (ancorata sotto, perfettamente visibile)
        BottomEditorControlCard(
            previewPeriod = previewPeriod,
            selectedPeriodNumber = selectedPeriodNumber,
            totalPeriodsCount = effectivePeriods.size,
            periodBounds = periodBounds,
            onSelectPeriod = onSelectPeriod,
            onShiftTimes = onShiftTimes,
            onAdjustDuration = onAdjustDuration,
            onUpdateStartOrEndTime = onUpdateStartOrEndTime,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Scheda di controllo inferiore compatta:
 * - Inizio & Fine editabili (con TimePicker)
 * - Due colonne di stepper: "Sposta i periodi" e "Durata"
 */
@Composable
fun BottomEditorControlCard(
    previewPeriod: FastingPeriod,
    selectedPeriodNumber: Int?,
    totalPeriodsCount: Int,
    periodBounds: PlanShiftEngine.PeriodBounds = PlanShiftEngine.PeriodBounds(),
    onSelectPeriod: (Int?) -> Unit,
    onShiftTimes: (Int, Int) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onUpdateStartOrEndTime: (Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val timePickerDialogStart = remember(previewPeriod, periodBounds) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                var targetH = hour
                var targetM = minute
                if (selectedPeriodNumber != null && periodBounds.hasAnterior && periodBounds.anteriorEndHour != null && periodBounds.anteriorEndMinute != null) {
                    if (previewPeriod.startDayOfWeek == periodBounds.anteriorEndDayOfWeek) {
                        val minTotal = periodBounds.anteriorEndHour * 60 + periodBounds.anteriorEndMinute
                        val chosenTotal = hour * 60 + minute
                        if (chosenTotal < minTotal) {
                            targetH = periodBounds.anteriorEndHour
                            targetM = periodBounds.anteriorEndMinute
                            Toast.makeText(
                                context,
                                "Non puoi superare il periodo anteriore (termina alle %02d:%02d)".format(targetH, targetM),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                onUpdateStartOrEndTime(targetH, targetM, true)
            },
            previewPeriod.startHour,
            previewPeriod.startMinute,
            true
        )
    }

    val timePickerDialogEnd = remember(previewPeriod, periodBounds) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                var targetH = hour
                var targetM = minute
                if (selectedPeriodNumber != null && periodBounds.hasNext && periodBounds.nextStartHour != null && periodBounds.nextStartMinute != null) {
                    if (previewPeriod.endDayOfWeek == periodBounds.nextStartDayOfWeek) {
                        val maxTotal = periodBounds.nextStartHour * 60 + periodBounds.nextStartMinute
                        val chosenTotal = hour * 60 + minute
                        if (chosenTotal > maxTotal) {
                            targetH = periodBounds.nextStartHour
                            targetM = periodBounds.nextStartMinute
                            Toast.makeText(
                                context,
                                "Non puoi superare il periodo successivo (inizia alle %02d:%02d)".format(targetH, targetM),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                onUpdateStartOrEndTime(targetH, targetM, false)
            },
            previewPeriod.endHour,
            previewPeriod.endMinute,
            true
        )
    }

    val currentDuration = previewPeriod.fastingDurationMinutes
    val canMinus1h = currentDuration - 60 >= MIN_DURATION_MINUTES
    val canMinus15m = currentDuration - 15 >= MIN_DURATION_MINUTES
    val canPlus1h = currentDuration + 60 <= MAX_DURATION_MINUTES && (selectedPeriodNumber == null || periodBounds.canDurationPlus(60))
    val canPlus15m = currentDuration + 15 <= MAX_DURATION_MINUTES && (selectedPeriodNumber == null || periodBounds.canDurationPlus(15))

    val canShiftMinus1h = selectedPeriodNumber == null || periodBounds.canShiftMinus1h
    val canShiftMinus15m = selectedPeriodNumber == null || periodBounds.canShiftMinus15m
    val canShiftPlus1h = selectedPeriodNumber == null || periodBounds.canShiftPlus1h
    val canShiftPlus15m = selectedPeriodNumber == null || periodBounds.canShiftPlus15m

    Surface(
        color = CiboSheetCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CiboCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Maniglietta card
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 30.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF26385A))
            )

            // Riga Inizio & Fine con matita (cliccabili)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inizio
                Column {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { timePickerDialogStart.show() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inizio: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CiboTextSecondary
                        )
                        Text(
                            text = "${previewPeriod.startDayShort} ${previewPeriod.startTimeFormatted}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboAccent
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica orario inizio",
                            tint = CiboAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (selectedPeriodNumber != null && periodBounds.hasAnterior && periodBounds.anteriorEndHour != null && periodBounds.anteriorEndMinute != null) {
                        Text(
                            text = "≥ ${periodBounds.anteriorEndDayShort ?: ""} %02d:%02d".format(periodBounds.anteriorEndHour, periodBounds.anteriorEndMinute),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Fine
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { timePickerDialogEnd.show() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fine: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CiboTextSecondary
                        )
                        Text(
                            text = "${previewPeriod.endDayShort} ${previewPeriod.endTimeFormatted}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboAccent
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica orario fine",
                            tint = CiboAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (selectedPeriodNumber != null && periodBounds.hasNext && periodBounds.nextStartHour != null && periodBounds.nextStartMinute != null) {
                        Text(
                            text = "≤ ${periodBounds.nextStartDayShort ?: ""} %02d:%02d".format(periodBounds.nextStartHour, periodBounds.nextStartMinute),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF26385A), thickness = 1.dp)

            // Due colonne di steppers affiancate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // COLONNA SINISTRA: "Sposta i periodi"
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.plan_editor_shift_periods_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CiboTextPrimary
                    )

                    // Stepper 1: Scope (Tutti / Periodo X)
                    val scopeLabel = if (selectedPeriodNumber == null) "Tutti i" else "Periodo ${previewPeriod.periodNumber}"
                    PlanStepperControl(
                        label = scopeLabel,
                        onMinus = {
                            if (selectedPeriodNumber == null) {
                                onSelectPeriod(totalPeriodsCount)
                            } else if (selectedPeriodNumber == 1) {
                                onSelectPeriod(null) // Tutti
                            } else {
                                onSelectPeriod(selectedPeriodNumber - 1)
                            }
                        },
                        onPlus = {
                            if (selectedPeriodNumber == null) {
                                onSelectPeriod(1)
                            } else if (selectedPeriodNumber == totalPeriodsCount) {
                                onSelectPeriod(null) // Tutti
                            } else {
                                onSelectPeriod(selectedPeriodNumber + 1)
                            }
                        },
                        contentDescriptionMinus = "Periodo precedente",
                        contentDescriptionPlus = "Periodo successivo",
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Stepper 2: 1 ora
                    PlanStepperControl(
                        label = stringResource(R.string.plan_editor_stepper_1_hour),
                        onMinus = { onShiftTimes(-1, 0) },
                        onPlus = { onShiftTimes(1, 0) },
                        contentDescriptionMinus = stringResource(R.string.plan_editor_shift_minus_1h),
                        contentDescriptionPlus = stringResource(R.string.plan_editor_shift_plus_1h),
                        minusEnabled = canShiftMinus1h,
                        plusEnabled = canShiftPlus1h,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Stepper 3: 15 min
                    PlanStepperControl(
                        label = stringResource(R.string.plan_editor_stepper_15_min),
                        onMinus = { onShiftTimes(0, -15) },
                        onPlus = { onShiftTimes(0, 15) },
                        contentDescriptionMinus = stringResource(R.string.plan_editor_shift_minus_15m),
                        contentDescriptionPlus = stringResource(R.string.plan_editor_shift_plus_15m),
                        minusEnabled = canShiftMinus15m,
                        plusEnabled = canShiftPlus15m,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // COLONNA DESTRA: "Durata: X ore"
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.plan_editor_duration_title) + ": ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboTextPrimary
                        )
                        Text(
                            text = previewPeriod.durationHoursString,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboAccent
                        )
                    }

                    // Stepper 1: 1 ora
                    PlanStepperControl(
                        label = stringResource(R.string.plan_editor_stepper_1_hour),
                        onMinus = { onAdjustDuration(-60) },
                        onPlus = { onAdjustDuration(60) },
                        contentDescriptionMinus = stringResource(R.string.plan_editor_duration_minus_1h),
                        contentDescriptionPlus = stringResource(R.string.plan_editor_duration_plus_1h),
                        minusEnabled = canMinus1h,
                        plusEnabled = canPlus1h,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Stepper 2: 15 min
                    PlanStepperControl(
                        label = stringResource(R.string.plan_editor_stepper_15_min),
                        onMinus = { onAdjustDuration(-15) },
                        onPlus = { onAdjustDuration(15) },
                        contentDescriptionMinus = stringResource(R.string.plan_editor_duration_minus_15m),
                        contentDescriptionPlus = stringResource(R.string.plan_editor_duration_plus_15m),
                        minusEnabled = canMinus15m,
                        plusEnabled = canPlus15m,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Chip del selettore giorni ("Tutti" + Lun..Dom)
 */
@Composable
fun PlanScopeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF0F2B45) else CiboSheetCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CiboAccent else CiboCardBorder
        ),
        modifier = modifier.fillMaxHeight()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) CiboAccent else CiboTextSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Barra 24h orizzontale con digestione/digiuno e pasto (preservata per compatibilità).
 */
@Composable
fun Fasting24hBar(
    dayPlan: DayPlan,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
    ) {
        val width = size.width
        val height = size.height

        val startTotalMinutes = (dayPlan.startHour * 60 + dayPlan.startMinute).toFloat()
        val duration = dayPlan.fastingDurationMinutes.toFloat()
        val endTotalMinutes = startTotalMinutes + duration

        data class Segment(val startMin: Float, val endMin: Float, val isFasting: Boolean)
        val segments = mutableListOf<Segment>()

        if (duration >= 1440f) {
            segments.add(Segment(0f, 1440f, true))
        } else if (duration <= 0f) {
            segments.add(Segment(0f, 1440f, false))
        } else if (endTotalMinutes >= 1440f) {
            val morningEnd = endTotalMinutes - 1440f
            if (morningEnd > 0f) {
                segments.add(Segment(0f, morningEnd, true))
            }
            if (startTotalMinutes > morningEnd) {
                segments.add(Segment(morningEnd, startTotalMinutes, false))
            }
            if (1440f > startTotalMinutes) {
                segments.add(Segment(startTotalMinutes, 1440f, true))
            }
        } else {
            if (startTotalMinutes > 0f) {
                segments.add(Segment(0f, startTotalMinutes, false))
            }
            segments.add(Segment(startTotalMinutes, endTotalMinutes, true))
            if (1440f > endTotalMinutes) {
                segments.add(Segment(endTotalMinutes, 1440f, false))
            }
        }

        val gapPx = 2.dp.toPx()
        val n = segments.size

        segments.forEachIndexed { i, seg ->
            var segStartX = (seg.startMin / 1440f) * width
            var segEndX = (seg.endMin / 1440f) * width

            if (i > 0) segStartX += gapPx / 2f
            if (i < n - 1) segEndX -= gapPx / 2f

            val segWidth = (segEndX - segStartX).coerceAtLeast(0f)
            if (segWidth > 0f) {
                val color = if (seg.isFasting) CiboAccent else CiboAmber
                drawRoundRect(
                    color = color,
                    topLeft = Offset(segStartX, 0f),
                    size = Size(segWidth, height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

/**
 * Composable Stepper compatto: − a sinistra (tonal #26385A), etichetta al centro, + a destra (accent #38BDF8).
 * Altezza 36dp per garantire che stia interamente nello schermo senza scroll.
 */
@Composable
fun PlanStepperControl(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    contentDescriptionMinus: String,
    contentDescriptionPlus: String,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true
) {
    Surface(
        color = Color(0xFF10192A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CiboCardBorder),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Minus button (−) tonal
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (minusEnabled) CiboStepperTonal else Color(0xFF182236))
                    .clickable(enabled = minusEnabled, onClick = onMinus)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = contentDescriptionMinus,
                    tint = if (minusEnabled) Color.White else Color(0xFF48566E),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Etichetta centrale
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CiboTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
            )

            // Plus button (+) accent pieno
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (plusEnabled) CiboAccent else Color(0xFF133247))
                    .clickable(enabled = plusEnabled, onClick = onPlus)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = contentDescriptionPlus,
                    tint = if (plusEnabled) CiboButtonDarkText else Color(0xFF48566E),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Alias per soddisfare la firma StepperRow richiesta dalla specifica.
 */
@Composable
fun StepperRow(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    contentDescriptionMinus: String,
    contentDescriptionPlus: String,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true
) = PlanStepperControl(
    label = label,
    onMinus = onMinus,
    onPlus = onPlus,
    contentDescriptionMinus = contentDescriptionMinus,
    contentDescriptionPlus = contentDescriptionPlus,
    modifier = modifier,
    minusEnabled = minusEnabled,
    plusEnabled = plusEnabled
)

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Scope Tutti (Pianificatore)", showBackground = true, backgroundColor = 0xFF101A2D)
@Composable
fun PreviewTimeEditorSheetScopeAll() {
    MaterialTheme {
        val plan = FastingPlanTemplates.PLAN_LUIGI_1
        val periods = plan.getEffectivePeriods()
        TimeEditorSheetContent(
            workingPlan = plan,
            selectedPeriodNumber = null,
            previewPeriod = periods.first(),
            onSelectPeriod = {},
            onShiftTimes = { _, _ -> },
            onAdjustDuration = {},
            onSave = {},
            onClose = {}
        )
    }
}

@Preview(name = "Scope Singolo Periodo (Periodo 1)", showBackground = true, backgroundColor = 0xFF101A2D)
@Composable
fun PreviewTimeEditorSheetSingleDay() {
    MaterialTheme {
        val plan = FastingPlanTemplates.PLAN_LUIGI_1
        val periods = plan.getEffectivePeriods()
        TimeEditorSheetContent(
            workingPlan = plan,
            selectedPeriodNumber = 1,
            previewPeriod = periods.first(),
            onSelectPeriod = {},
            onShiftTimes = { _, _ -> },
            onAdjustDuration = {},
            onSave = {},
            onClose = {}
        )
    }
}

@Preview(name = "Schermo Compatto 360x640dp", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFF101A2D)
@Composable
fun PreviewTimeEditorSheetSmallScreen() {
    MaterialTheme {
        val plan = FastingPlanTemplates.PLAN_LUIGI_1
        val periods = plan.getEffectivePeriods()
        TimeEditorSheetContent(
            workingPlan = plan,
            selectedPeriodNumber = 1,
            previewPeriod = periods.first(),
            onSelectPeriod = {},
            onShiftTimes = { _, _ -> },
            onAdjustDuration = {},
            onSave = {},
            onClose = {}
        )
    }
}
