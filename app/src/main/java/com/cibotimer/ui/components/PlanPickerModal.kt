package com.cibotimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cibotimer.R
import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingDayFormatter
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.PlanCategory
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.PlanShiftEngine
import com.cibotimer.ui.theme.*

/**
 * Stati di navigazione interna del selettore del piano.
 */
sealed interface PlanPickerStep {
    data object List : PlanPickerStep
    data class PreviewAndConfirm(val plan: FastingPlan) : PlanPickerStep
    data class CreateCustom(val initialPlan: FastingPlan? = null) : PlanPickerStep
}

/**
 * Modal Bottom Sheet per la selezione, anteprima, conferma e creazione personalizzata del piano di digiuno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanPickerModal(
    currentPlan: FastingPlan,
    customPlans: List<FastingPlan> = emptyList(),
    onPlanSelected: (FastingPlan) -> Unit,
    onSaveCustomPlan: ((FastingPlan) -> Unit)? = null,
    onRenamePlan: ((FastingPlan) -> Unit)? = null,
    onDeletePlan: ((FastingPlan) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val allPredefined = remember { FastingPlanTemplates.ALL_PREDEFINED_PLANS }

    Dialog(
        onDismissRequest = onDismiss,
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
            PlanPickerSheetContent(
                predefinedPlans = allPredefined,
                customPlans = customPlans,
                activePlanId = currentPlan.id,
                onPlanConfirmed = { plan ->
                    onPlanSelected(plan)
                    onDismiss()
                },
                onSaveCustomPlan = { plan ->
                    if (onSaveCustomPlan != null) {
                        onSaveCustomPlan(plan)
                    } else {
                        onPlanSelected(plan)
                    }
                    onDismiss()
                },
                onRenamePlan = onRenamePlan,
                onDeletePlan = onDeletePlan,
                onClose = onDismiss
            )
        }
    }
}

/**
 * Contenitore dei passi (Elenco, Anteprima & Conferma, Crea Personalizzato).
 */
@Composable
fun PlanPickerSheetContent(
    predefinedPlans: List<FastingPlan>,
    customPlans: List<FastingPlan> = emptyList(),
    activePlanId: String,
    onPlanConfirmed: (FastingPlan) -> Unit,
    onSaveCustomPlan: (FastingPlan) -> Unit,
    onRenamePlan: ((FastingPlan) -> Unit)? = null,
    onDeletePlan: ((FastingPlan) -> Unit)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf<PlanPickerStep>(PlanPickerStep.List) }

    when (val currentStep = step) {
        is PlanPickerStep.List -> {
            PlanPickerListStep(
                predefinedPlans = predefinedPlans,
                customPlans = customPlans,
                activePlanId = activePlanId,
                onCreateCustomClick = { step = PlanPickerStep.CreateCustom(null) },
                onPlanClick = { plan -> step = PlanPickerStep.PreviewAndConfirm(plan) },
                onDeletePlan = onDeletePlan,
                onClose = onClose,
                modifier = modifier
            )
        }

        is PlanPickerStep.PreviewAndConfirm -> {
            PlanPickerPreviewStep(
                plan = currentStep.plan,
                customPlans = customPlans,
                onConfirm = onPlanConfirmed,
                onEditAdvanced = { step = PlanPickerStep.CreateCustom(currentStep.plan) },
                onRenamePlan = { renamedPlan ->
                    step = PlanPickerStep.PreviewAndConfirm(renamedPlan)
                    onRenamePlan?.invoke(renamedPlan)
                },
                onDeletePlan = onDeletePlan,
                onBack = { step = PlanPickerStep.List },
                onClose = onClose,
                modifier = modifier
            )
        }

        is PlanPickerStep.CreateCustom -> {
            PlanPickerCustomStep(
                initialPlan = currentStep.initialPlan,
                onSave = onSaveCustomPlan,
                onBack = { step = PlanPickerStep.List },
                onClose = onClose,
                modifier = modifier
            )
        }
    }
}

/**
 * PASSO 1: Elenco dei piani con pulsante in evidenza per creare un piano personalizzato.
 */
@Composable
fun PlanPickerListStep(
    predefinedPlans: List<FastingPlan>,
    customPlans: List<FastingPlan>,
    activePlanId: String,
    onCreateCustomClick: () -> Unit,
    onPlanClick: (FastingPlan) -> Unit,
    onDeletePlan: ((FastingPlan) -> Unit)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var planToDelete by remember { mutableStateOf<FastingPlan?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Header (48dp): X Chiudi | Titolo "Piani di digiuno" | Spacer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.plan_editor_close),
                    tint = CiboTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.plan_picker_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CiboTextPrimary
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Pulsante "+ Crea piano personalizzato" in alto
            item {
                Surface(
                    onClick = onCreateCustomClick,
                    color = Color(0xFF0F2B45),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, CiboAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CiboAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CiboAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.plan_picker_create_custom),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CiboAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.plan_picker_create_custom_desc),
                                fontSize = 12.sp,
                                color = CiboTextSecondary
                            )
                        }
                    }
                }
            }

            // Sezione piani personalizzati esistenti
            if (customPlans.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.plan_picker_section_custom),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CiboTextSecondary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                items(customPlans, key = { it.id }) { plan ->
                    PlanItemCard(
                        plan = plan,
                        isActive = plan.id == activePlanId,
                        onClick = { onPlanClick(plan) },
                        onDelete = if (onDeletePlan != null) {
                            { planToDelete = plan }
                        } else null
                    )
                }
            }

            // Sezione piani consigliati
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.plan_picker_section_predefined),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboTextSecondary,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            items(predefinedPlans, key = { it.id }) { plan ->
                PlanItemCard(
                    plan = plan,
                    isActive = plan.id == activePlanId,
                    onClick = { onPlanClick(plan) }
                )
            }
        }
    }

    if (planToDelete != null) {
        val targetPlan = planToDelete!!
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_plan_title),
                    fontWeight = FontWeight.Bold,
                    color = CiboTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_delete_plan_message, targetPlan.name),
                        color = CiboTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    if (targetPlan.id == activePlanId) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.dialog_delete_active_plan_warning),
                            color = CiboAmber,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlan?.invoke(targetPlan)
                        planToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.button_delete), color = CiboStopRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text(stringResource(R.string.button_cancel), color = CiboTextSecondary)
                }
            },
            containerColor = CiboSheetCard,
            titleContentColor = CiboTextPrimary,
            textContentColor = CiboTextSecondary
        )
    }
}

/**
 * PASSO 2: Anteprima prima di accettare il piano, con regolazione e conferma inizio e fine.
 */
@Composable
fun PlanPickerPreviewStep(
    plan: FastingPlan,
    customPlans: List<FastingPlan> = emptyList(),
    onConfirm: (FastingPlan) -> Unit,
    onEditAdvanced: () -> Unit,
    onRenamePlan: ((FastingPlan) -> Unit)? = null,
    onDeletePlan: ((FastingPlan) -> Unit)? = null,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workingPlan by remember(plan) { mutableStateOf(plan) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isCustomPlan = remember(workingPlan, customPlans) {
        workingPlan.isCustom || workingPlan.category == PlanCategory.CUSTOM || customPlans.any { it.id == workingPlan.id }
    }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(workingPlan.name) { mutableStateOf(workingPlan.name) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Header (48dp): Freccia indietro | Titolo "Anteprima piano" | X Chiudi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.plan_picker_back),
                    tint = CiboTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.plan_picker_preview_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CiboTextPrimary
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.plan_editor_close),
                    tint = CiboTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Info Nome & Descrizione Piano
            Surface(
                color = CiboSheetCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .then(
                                    if (isCustomPlan) {
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                renameText = workingPlan.name
                                                showRenameDialog = true
                                            }
                                    } else Modifier
                                )
                        ) {
                            Text(
                                text = workingPlan.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CiboAccent
                            )

                            if (isCustomPlan) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.content_desc_rename_plan),
                                    tint = CiboAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = CiboAccent.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            val cat = when (workingPlan.category) {
                                PlanCategory.STANDARD -> stringResource(R.string.plan_category_standard)
                                PlanCategory.AUTOPHAGY -> stringResource(R.string.plan_category_autophagy)
                                PlanCategory.SPECIAL -> stringResource(R.string.plan_category_special)
                                PlanCategory.CUSTOM -> stringResource(R.string.plan_category_custom)
                            }
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CiboAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (workingPlan.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = workingPlan.description,
                            fontSize = 13.sp,
                            color = CiboTextSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    if (isCustomPlan) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    renameText = workingPlan.name
                                    showRenameDialog = true
                                }
                                .padding(vertical = 2.dp, horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CiboAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.plan_rename_action),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CiboAccent
                            )
                        }
                    }
                }
            }

            // Programma Settimanale e Modifica Orari
            Surface(
                color = CiboSheetCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.plan_preview_weekly_schedule),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CiboTextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 7 giorni
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        workingPlan.days.forEach { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.dayNameShort,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CiboTextSecondary,
                                    modifier = Modifier.width(34.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                ) {
                                    Fasting24hBar(
                                        dayPlan = day,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Asse 0h, 6h, 12h, 18h, 24h
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("0h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                        Text("6h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(2f))
                        Text("12h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(2f))
                        Text("18h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(2f))
                        Text("24h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pulsante Modifica Avanzata
                    OutlinedButton(
                        onClick = onEditAdvanced,
                        border = BorderStroke(1.dp, CiboAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = CiboAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.plan_preview_advanced_edit),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Pulsante "Conferma ed adotta piano"
            Button(
                onClick = { onConfirm(workingPlan) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CiboAccent,
                    contentColor = CiboButtonDarkText
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CiboButtonDarkText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.plan_picker_confirm_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboButtonDarkText
                )
            }

            if (onDeletePlan != null && isCustomPlan) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    border = BorderStroke(1.dp, CiboStopRed),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = CiboStopRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.plan_delete_action),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CiboStopRed
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CiboAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.plan_rename_title),
                        fontWeight = FontWeight.Bold,
                        color = CiboTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.plan_rename_subtitle),
                        color = CiboTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppOutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.plan_picker_custom_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AppPrimaryButton(
                    text = stringResource(R.string.button_save),
                    enabled = renameText.trim().isNotBlank(),
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isNotBlank()) {
                            val updated = workingPlan.copy(name = trimmed)
                            workingPlan = updated
                            onRenamePlan?.invoke(updated)
                            showRenameDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                AppSecondaryButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { showRenameDialog = false }
                )
            },
            containerColor = CiboSheetCard,
            titleContentColor = CiboTextPrimary,
            textContentColor = CiboTextSecondary
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_plan_title),
                    fontWeight = FontWeight.Bold,
                    color = CiboTextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_delete_plan_message, workingPlan.name),
                    color = CiboTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlan?.invoke(workingPlan)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.button_delete), color = CiboStopRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.button_cancel), color = CiboTextSecondary)
                }
            },
            containerColor = CiboSheetCard,
            titleContentColor = CiboTextPrimary,
            textContentColor = CiboTextSecondary
        )
    }
}

/**
 * PASSO 3: Creazione di un nuovo piano personalizzato su misura, personalizzabile per ogni singolo giorno.
 */
@Composable
fun PlanPickerCustomStep(
    initialPlan: FastingPlan? = null,
    onSave: (FastingPlan) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val modifiedSuffix = initialPlan?.name?.let { stringResource(R.string.plan_name_modified_suffix, it) } ?: ""
    var planName by remember { mutableStateOf(modifiedSuffix) }
    var selectedDayScope by remember { mutableStateOf<Int?>(null) } // null = "Tutti", 1..7 = Lun..Dom

    var customDays by remember {
        mutableStateOf(initialPlan?.days ?: FastingPlanTemplates.createStandardDays(20, 0, 12, 0, 960))
    }

    val activeDay = remember(customDays, selectedDayScope) {
        if (selectedDayScope != null) {
            customDays.find { it.dayOfWeek == selectedDayScope } ?: customDays.first()
        } else {
            customDays.first()
        }
    }

    val isFastingActive = activeDay.isFastingDay
    val fastingMinutes = if (isFastingActive) activeDay.fastingDurationMinutes else 0
    val fastingH = fastingMinutes / 60
    val fastingM = fastingMinutes % 60
    val fastingLabel = if (fastingM == 0) "$fastingH h" else "${fastingH}h ${fastingM}m"

    val canMinus1h = fastingMinutes - 60 >= 60
    val canMinus15m = fastingMinutes - 15 >= 60
    val canPlus1h = fastingMinutes + 60 <= 24 * 60
    val canPlus15m = fastingMinutes + 15 <= 24 * 60

    val timePickerDialog = remember(activeDay) {
        android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                val newEndTotal = (hour * 60 + minute + activeDay.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
                customDays = if (selectedDayScope == null) {
                    customDays.map { d ->
                        val endTot = (hour * 60 + minute + d.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
                        d.copy(startHour = hour, startMinute = minute, endHour = endTot / 60, endMinute = endTot % 60)
                    }
                } else {
                    customDays.map { d ->
                        if (d.dayOfWeek == selectedDayScope) {
                            d.copy(startHour = hour, startMinute = minute, endHour = newEndTotal / 60, endMinute = newEndTotal % 60)
                        } else d
                    }
                }
            },
            activeDay.startHour,
            activeDay.startMinute,
            true
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Header (48dp): Freccia indietro | Titolo "Nuovo piano personalizzato" | X Chiudi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.plan_picker_back),
                    tint = CiboTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.plan_picker_custom_title),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = CiboTextPrimary
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.plan_editor_close),
                    tint = CiboTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Campo Nome Piano
            OutlinedTextField(
                value = planName,
                onValueChange = { planName = it },
                label = { Text(stringResource(R.string.plan_picker_custom_name_label)) },
                placeholder = { Text(stringResource(R.string.plan_picker_custom_name_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CiboSheetCard,
                    unfocusedContainerColor = CiboSheetCard,
                    focusedBorderColor = CiboAccent,
                    unfocusedBorderColor = CiboCardBorder,
                    focusedLabelColor = CiboAccent,
                    unfocusedLabelColor = CiboTextSecondary,
                    focusedTextColor = CiboTextPrimary,
                    unfocusedTextColor = CiboTextPrimary,
                    cursorColor = CiboAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Istogramma 7 Giorni (Lun..Dom) 0-24h
            Surface(
                color = CiboSheetCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CiboCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.plan_custom_schedule_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CiboTextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(CiboAmber, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.legend_eating), fontSize = 10.sp, color = CiboTextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.size(8.dp).background(CiboAccent, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.legend_fasting), fontSize = 10.sp, color = CiboTextSecondary)
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        customDays.forEach { day ->
                            val isSelected = selectedDayScope == day.dayOfWeek || selectedDayScope == null
                            val isExplicitlyChosen = selectedDayScope == day.dayOfWeek

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedDayScope = day.dayOfWeek }
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.dayNameShort,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isExplicitlyChosen) CiboAccent else if (isSelected) Color(0xFF93C5FD) else CiboTextSecondary,
                                    modifier = Modifier.width(34.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(11.dp)
                                        .then(
                                            if (isExplicitlyChosen) {
                                                Modifier
                                                    .border(1.5.dp, CiboAccent, RoundedCornerShape(6.dp))
                                                    .padding(1.dp)
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    Fasting24hBar(
                                        dayPlan = day,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Text(
                                    text = if (day.isFastingDay) "${day.fastingDurationMinutes / 60}h" else stringResource(R.string.week_chart_free),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.isFastingDay) CiboAccent else CiboAmber,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // Asse 0h, 6h, 12h, 18h, 24h
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, end = 38.dp, top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("0h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                        Text("6h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(2f))
                        Text("12h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(2f))
                        Text("18h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(2f))
                        Text("24h", fontSize = 11.sp, color = CiboTextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }
            }

            // APPLICA A (Tutti / Singoli giorni)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.plan_editor_apply_to),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CiboTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlanScopeChip(
                        label = stringResource(R.string.plan_editor_scope_all),
                        isSelected = selectedDayScope == null,
                        onClick = { selectedDayScope = null },
                        modifier = Modifier.weight(1.35f)
                    )

                    val daysShort = (1..7).map { FastingDayFormatter.formatShort(it) }
                    daysShort.forEachIndexed { index, name ->
                        val dayOfWeek = index + 1
                        PlanScopeChip(
                            label = name,
                            isSelected = selectedDayScope == dayOfWeek,
                            onClick = { selectedDayScope = dayOfWeek },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Se è selezionato un singolo giorno: Toggle Giorno di Digiuno / Giorno Libero
            if (selectedDayScope != null) {
                Surface(
                    onClick = {
                        customDays = customDays.map { d ->
                            if (d.dayOfWeek == selectedDayScope) {
                                d.copy(isFastingDay = !d.isFastingDay)
                            } else d
                        }
                    },
                    color = if (isFastingActive) Color(0xFF0F2B45) else Color(0xFF2A2012),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isFastingActive) CiboAccent else CiboAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFastingActive) stringResource(R.string.plan_custom_active_day) else stringResource(R.string.plan_custom_free_day),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFastingActive) CiboAccent else CiboAmber
                        )
                        Text(
                            text = stringResource(R.string.plan_custom_tap_to_change),
                            fontSize = 11.sp,
                            color = CiboTextSecondary
                        )
                    }
                }
            }

            if (isFastingActive) {
                // Card Orario Inizio Digiuno
                Surface(
                    color = CiboSheetCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CiboCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.plan_editor_start_fasting),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CiboTextPrimary
                            )
                            Surface(
                                onClick = { timePickerDialog.show() },
                                color = Color.Transparent
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeDay.startTimeFormatted,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CiboAccent
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.content_desc_edit_time),
                                        tint = CiboAccent.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StepperRow(
                                label = stringResource(R.string.plan_editor_stepper_1_hour),
                                onMinus = {
                                    customDays = shiftCustomDays(customDays, selectedDayScope, -1, 0)
                                },
                                onPlus = {
                                    customDays = shiftCustomDays(customDays, selectedDayScope, 1, 0)
                                },
                                contentDescriptionMinus = stringResource(R.string.plan_editor_shift_minus_1h),
                                contentDescriptionPlus = stringResource(R.string.plan_editor_shift_plus_1h),
                                modifier = Modifier.weight(1f)
                            )

                            StepperRow(
                                label = stringResource(R.string.plan_editor_stepper_15_min),
                                onMinus = {
                                    customDays = shiftCustomDays(customDays, selectedDayScope, 0, -15)
                                },
                                onPlus = {
                                    customDays = shiftCustomDays(customDays, selectedDayScope, 0, 15)
                                },
                                contentDescriptionMinus = stringResource(R.string.plan_editor_shift_minus_15m),
                                contentDescriptionPlus = stringResource(R.string.plan_editor_shift_plus_15m),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Card Durata Digiuno
                Surface(
                    color = CiboSheetCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CiboCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.plan_editor_duration_title),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CiboTextPrimary
                            )
                            Text(
                                text = fastingLabel,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CiboAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StepperRow(
                                label = stringResource(R.string.plan_editor_stepper_1_hour),
                                onMinus = {
                                    customDays = adjustCustomDuration(customDays, selectedDayScope, -60)
                                },
                                onPlus = {
                                    customDays = adjustCustomDuration(customDays, selectedDayScope, 60)
                                },
                                contentDescriptionMinus = stringResource(R.string.plan_editor_duration_minus_1h),
                                contentDescriptionPlus = stringResource(R.string.plan_editor_duration_plus_1h),
                                minusEnabled = canMinus1h,
                                plusEnabled = canPlus1h,
                                modifier = Modifier.weight(1f)
                            )

                            StepperRow(
                                label = stringResource(R.string.plan_editor_stepper_15_min),
                                onMinus = {
                                    customDays = adjustCustomDuration(customDays, selectedDayScope, -15)
                                },
                                onPlus = {
                                    customDays = adjustCustomDuration(customDays, selectedDayScope, 15)
                                },
                                contentDescriptionMinus = stringResource(R.string.plan_editor_duration_minus_15m),
                                contentDescriptionPlus = stringResource(R.string.plan_editor_duration_plus_15m),
                                minusEnabled = canMinus15m,
                                plusEnabled = canPlus15m,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val defaultPlanName = stringResource(R.string.plan_custom_default_name)
            val defaultPlanDesc = stringResource(R.string.plan_custom_default_desc)
            // Pulsante "Salva ed adotta piano"
            Button(
                onClick = {
                    val title = planName.trim().ifBlank { defaultPlanName }
                    val customPlan = FastingPlan(
                        id = "custom_${System.currentTimeMillis()}",
                        name = title,
                        category = PlanCategory.CUSTOM,
                        description = defaultPlanDesc,
                        days = customDays,
                        isCustom = true
                    )
                    onSave(customPlan)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CiboAccent,
                    contentColor = CiboButtonDarkText
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CiboButtonDarkText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.plan_picker_custom_save),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboButtonDarkText
                )
            }
        }
    }
}

private fun shiftCustomDays(days: List<DayPlan>, scope: Int?, deltaH: Int, deltaM: Int): List<DayPlan> {
    val deltaTotal = deltaH * 60 + deltaM
    return days.map { d ->
        if (scope == null || d.dayOfWeek == scope) {
            val newStart = (d.startHour * 60 + d.startMinute + deltaTotal + 24 * 60 * 10) % (24 * 60)
            val newEnd = (d.endHour * 60 + d.endMinute + deltaTotal + 24 * 60 * 10) % (24 * 60)
            d.copy(
                startHour = newStart / 60,
                startMinute = newStart % 60,
                endHour = newEnd / 60,
                endMinute = newEnd % 60
            )
        } else d
    }
}

private fun adjustCustomDuration(days: List<DayPlan>, scope: Int?, deltaM: Int): List<DayPlan> {
    return days.map { d ->
        if (scope == null || d.dayOfWeek == scope) {
            val newDur = (d.fastingDurationMinutes + deltaM).coerceIn(60, 24 * 60)
            val newEnd = (d.startHour * 60 + d.startMinute + newDur + 24 * 60 * 10) % (24 * 60)
            d.copy(
                endHour = newEnd / 60,
                endMinute = newEnd % 60,
                fastingDurationMinutes = newDur
            )
        } else d
    }
}

/**
 * Singola card per il piano di digiuno con evidenziazione attiva, durate e descrizione.
 */
@Composable
fun PlanItemCard(
    plan: FastingPlan,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firstDay = plan.days.firstOrNull { it.isFastingDay } ?: plan.days.firstOrNull()
    val fastingHours = (firstDay?.fastingDurationMinutes ?: 960) / 60
    val fastingMinutes = (firstDay?.fastingDurationMinutes ?: 960) % 60
    val eatingHours = 24 - fastingHours

    val durationLabel = if (fastingMinutes == 0) {
        stringResource(R.string.plan_card_duration_hours, fastingHours, eatingHours)
    } else {
        stringResource(R.string.plan_card_duration_mins, fastingHours, fastingMinutes)
    }

    Surface(
        onClick = onClick,
        color = if (isActive) Color(0xFF11263F) else CiboSheetCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) CiboAccent else CiboCardBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isActive) stringResource(R.string.content_desc_selected) else stringResource(R.string.content_desc_not_selected),
                tint = if (isActive) CiboAccent else Color(0xFF4A5A75),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.localizedName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) CiboAccent else CiboTextPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isActive) {
                            Surface(
                                color = CiboAccent.copy(alpha = 0.15f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = stringResource(R.string.plan_picker_active_badge),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CiboAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            val categoryName = when (plan.category) {
                                PlanCategory.STANDARD -> stringResource(R.string.plan_category_standard)
                                PlanCategory.AUTOPHAGY -> stringResource(R.string.plan_category_autophagy)
                                PlanCategory.SPECIAL -> stringResource(R.string.plan_category_special)
                                PlanCategory.CUSTOM -> stringResource(R.string.plan_category_custom)
                            }
                            Text(
                                text = categoryName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = CiboTextSecondary
                            )
                        }

                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.content_desc_delete_plan),
                                    tint = CiboStopRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = durationLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Color(0xFF7DD3FC) else CiboTextSecondary
                )

                if (plan.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plan.description,
                        fontSize = 12.sp,
                        color = CiboTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Selettore Piani - Elenco", showBackground = true, backgroundColor = 0xFF101A2D)
@Composable
fun PreviewPlanPickerList() {
    CiboTimerTheme {
        PlanPickerSheetContent(
            predefinedPlans = FastingPlanTemplates.ALL_PREDEFINED_PLANS,
            customPlans = emptyList(),
            activePlanId = "16-8",
            onPlanConfirmed = {},
            onSaveCustomPlan = {},
            onClose = {}
        )
    }
}

@Preview(name = "Selettore Piani - Anteprima", showBackground = true, backgroundColor = 0xFF101A2D)
@Composable
fun PreviewPlanPickerPreview() {
    CiboTimerTheme {
        PlanPickerPreviewStep(
            plan = FastingPlanTemplates.PLAN_16_8,
            onConfirm = {},
            onEditAdvanced = {},
            onBack = {},
            onClose = {}
        )
    }
}

@Preview(name = "Selettore Piani - Crea Personalizzato", showBackground = true, backgroundColor = 0xFF101A2D)
@Composable
fun PreviewPlanPickerCustom() {
    CiboTimerTheme {
        PlanPickerCustomStep(
            onSave = {},
            onBack = {},
            onClose = {}
        )
    }
}
