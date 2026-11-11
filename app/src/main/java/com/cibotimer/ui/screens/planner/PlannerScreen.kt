package com.cibotimer.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.PlanCategory
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.ui.components.TimeEditorModal
import com.cibotimer.ui.components.PlanPickerModal
import com.cibotimer.ui.components.WeekBarChart
import com.cibotimer.R
import com.cibotimer.ui.theme.*
import androidx.compose.ui.res.stringResource
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activePlan by repository.activePlan.collectAsState()

    // Determine current day of week (1=Monday .. 7=Sunday)
    val currentDayOfWeek = remember {
        val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        // Calendar: Sunday=1, Monday=2 -> map to Monday=1..Sunday=7
        if (calDay == Calendar.SUNDAY) 7 else calDay - 1
    }

    var selectedDayOfWeek by remember { mutableIntStateOf(currentDayOfWeek) }
    var showTimeEditor by remember { mutableStateOf(false) }
    var showDeleteActiveDialog by remember { mutableStateOf(false) }
    var showPlanPickerModal by remember { mutableStateOf(false) }

    val selectedDayPlan: DayPlan = activePlan.days.find { it.dayOfWeek == selectedDayOfWeek }
        ?: activePlan.days.firstOrNull()
        ?: DayPlan(selectedDayOfWeek, true, 20, 0, 12, 0, 16 * 60)

    val totalFastingHoursWeek = remember(activePlan) {
        activePlan.days.sumOf { it.fastingDurationMinutes } / 60
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pianificatore settimanale",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = activePlan.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = CiboTimerAzure
                        )
                    }
                },
                actions = {
                    if (activePlan.isCustom || activePlan.category == PlanCategory.CUSTOM) {
                        IconButton(onClick = { showDeleteActiveDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = text = stringResource(R.string.dialog_delete_plan_title),,
                                tint = CiboStopRed
                            )
                        }
                    }
                    TextButton(onClick = { showPlanPickerModal = true }) {
                        Text(stringResource(R.string.planner_change_plan), color = CiboTimerAzure, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plan summary banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activePlan.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activePlan.descriptionItalian,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CiboTimerAzureContainer
                        ) {
                            Text(
                                text = "$totalFastingHoursWeek ore/sett.",
                                color = CiboTimerAzure,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Visual 7-day Bar Chart
            item {
                WeekBarChart(
                    days = activePlan.days,
                    selectedDayOfWeek = selectedDayOfWeek,
                    periods = activePlan.periods,
                    onDaySelected = { selectedDayOfWeek = it }
                )
            }

            // Action button: Modifica orari piano
            item {
                Button(
                    onClick = { showTimeEditor = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CiboTimerAzure,
                        contentColor = Color(0xFF031019)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF031019))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MODIFICA PIANO (${selectedDayPlan.dayName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF031019)
                    )
                }
            }

            // List of the 7 days with detailed intervals
            item {
                Text(
                    text = "Dettaglio giorni della settimana",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(activePlan.days) { _, day ->
                val isSelected = day.dayOfWeek == selectedDayOfWeek
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SurfaceVariantDark else SurfaceDark,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CiboTimerAzure) else androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedDayOfWeek = day.dayOfWeek
                            showTimeEditor = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day badge
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CiboTimerAzure else CiboTimerAzureContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayNameShort,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else CiboTimerAzure,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = day.dayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = CiboTimerAzure,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Digiuno: ${day.startTimeFormatted} - ${day.endTimeFormatted} (${day.fastingDurationMinutes / 60}h)",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = CiboTimerOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pasti: ${day.endTimeFormatted} - ${day.startTimeFormatted} (${(24 * 60 - day.fastingDurationMinutes) / 60}h)",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica giorno",
                            tint = if (isSelected) CiboTimerAzure else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Time Editor Bottom Sheet Modal
    if (showTimeEditor) {
        TimeEditorModal(
            dayPlan = selectedDayPlan,
            plan = activePlan,
            initialDayOfWeek = selectedDayOfWeek,
            onSavePlan = { updatedPlan ->
                repository.updateActivePlan(updatedPlan)
                android.widget.Toast.makeText(context, context.getString(R.string.planner_plan_saved_toast), android.widget.Toast.LENGTH_SHORT).show()
            },
            onSaveCustomPlan = { newPlan, activate ->
                repository.saveCustomPlan(newPlan, activate = activate)
                val msg = if (activate) "Piano \"${newPlan.name}\" salvato e attivato!" else "Piano \"${newPlan.name}\" salvato!"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            },
            onShiftAllDays = { deltaHours ->
                repository.shiftPlanAllDays(deltaHours)
            },
            onShiftSingleDay = { deltaHours, deltaMinutes ->
                repository.shiftPlanDay(selectedDayOfWeek, deltaHours, deltaMinutes)
            },
            onAdjustDuration = { deltaMinutes ->
                repository.adjustPlanDuration(selectedDayOfWeek, deltaMinutes)
            },
            onDismiss = { showTimeEditor = false }
        )
    }

    if (showDeleteActiveDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteActiveDialog = false },
            title = {
                Text(
                    text = "Elimina Piano",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Sei sicuro di voler eliminare definitivamente il piano \"${activePlan.name}\"? Verrà automaticamente ripristinato il piano 16-8 Classico.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deletePlan(activePlan.id)
                        showDeleteActiveDialog = false
                    }
                ) {
                    Text(stringResource(R.string.button_delete), color = CiboStopRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteActiveDialog = false }) {
                    Text(stringResource(R.string.button_cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

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
}
