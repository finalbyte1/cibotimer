package com.cibotimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.WeightEntry
import com.cibotimer.data.models.WeightUnit
import com.cibotimer.domain.BodyStatsCalculator
import com.cibotimer.domain.DateTimeFormatters
import com.cibotimer.domain.WeightUnitConverter
import com.cibotimer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MeasurementEntryDialog(
    initialEntry: WeightEntry? = null,
    defaultWeight: Double = 75.0,
    weightUnit: WeightUnit = WeightUnit.KG,
    onDismiss: () -> Unit,
    onSave: (
        weightKg: Double,
        waistCm: Double?,
        hipsCm: Double?,
        chestCm: Double?,
        thighsCm: Double?,
        armsCm: Double?,
        notes: String,
        epochMillis: Long
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val initialKg = initialEntry?.weightKg ?: defaultWeight
    var weightText by remember {
        mutableStateOf(WeightUnitConverter.formatWeight(initialKg, weightUnit, showUnit = false))
    }
    var weightError by remember { mutableStateOf(false) }

    val (minBound, maxBound) = remember(weightUnit) {
        WeightUnitConverter.getFormattedBounds(weightUnit)
    }
    val weightErrorMessage = stringResource(R.string.weight_validation_error, minBound, maxBound)

    var waistText by remember { mutableStateOf(initialEntry?.waistCm?.toString() ?: "") }
    var hipsText by remember { mutableStateOf(initialEntry?.hipsCm?.toString() ?: "") }
    var chestText by remember { mutableStateOf(initialEntry?.chestCm?.toString() ?: "") }
    var thighsText by remember { mutableStateOf(initialEntry?.thighsCm?.toString() ?: "") }
    var armsText by remember { mutableStateOf(initialEntry?.armsCm?.toString() ?: "") }
    var notesText by remember { mutableStateOf(initialEntry?.notes ?: "") }

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val epochMillis = remember { initialEntry?.epochMillis ?: System.currentTimeMillis() }

    // Live WHR calculation
    val waistVal = waistText.toDoubleOrNull()
    val hipsVal = hipsText.toDoubleOrNull()
    val liveWhr = remember(waistVal, hipsVal) {
        BodyStatsCalculator.calculateWhr(waistVal, hipsVal)
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CiboSheetBg,
        titleContentColor = CiboTextPrimary,
        textContentColor = CiboTextSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = CiboTimerAzureGlow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialEntry == null) stringResource(R.string.measurement_dialog_title_new) else stringResource(R.string.measurement_dialog_title_edit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = CiboTextPrimary
                    )
                }

                if (onDelete != null && initialEntry != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.button_delete),
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.measurement_date_prefix, DateTimeFormatters.formatFullDateTime(epochMillis, context, locale)),
                    fontSize = 11.sp,
                    color = CiboTextSecondary
                )

                // Peso (Obbligatorio)
                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        weightError = false
                    },
                    label = { Text(stringResource(R.string.measurement_weight_label, weightUnit.symbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = weightError,
                    supportingText = if (weightError) {
                        { Text(weightErrorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CiboTimerAzure,
                        unfocusedBorderColor = CiboCardBorder,
                        focusedTextColor = CiboTextPrimary,
                        unfocusedTextColor = CiboTextPrimary,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.measurement_circumferences_header),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboTimerAzureLight,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Vita & Fianchi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = waistText,
                        onValueChange = { waistText = it },
                        label = { Text(stringResource(R.string.measurement_waist_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CiboTimerAzure,
                            unfocusedBorderColor = CiboCardBorder,
                            focusedTextColor = CiboTextPrimary,
                            unfocusedTextColor = CiboTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hipsText,
                        onValueChange = { hipsText = it },
                        label = { Text(stringResource(R.string.measurement_hips_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CiboTimerAzure,
                            unfocusedBorderColor = CiboCardBorder,
                            focusedTextColor = CiboTextPrimary,
                            unfocusedTextColor = CiboTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live WHR badge
                if (liveWhr != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CiboCard,
                        border = BorderStroke(1.dp, Color(liveWhr.colorHex).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.measurement_whr_format, liveWhr.ratio),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CiboTextPrimary
                            )
                            Text(
                                text = liveWhr.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(liveWhr.colorHex)
                            )
                        }
                    }
                }

                // Torace & Cosce
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chestText,
                        onValueChange = { chestText = it },
                        label = { Text(stringResource(R.string.measurement_chest_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CiboTimerAzure,
                            unfocusedBorderColor = CiboCardBorder,
                            focusedTextColor = CiboTextPrimary,
                            unfocusedTextColor = CiboTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thighsText,
                        onValueChange = { thighsText = it },
                        label = { Text(stringResource(R.string.measurement_thighs_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CiboTimerAzure,
                            unfocusedBorderColor = CiboCardBorder,
                            focusedTextColor = CiboTextPrimary,
                            unfocusedTextColor = CiboTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Braccia
                OutlinedTextField(
                    value = armsText,
                    onValueChange = { armsText = it },
                    label = { Text(stringResource(R.string.measurement_arms_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CiboTimerAzure,
                        unfocusedBorderColor = CiboCardBorder,
                        focusedTextColor = CiboTextPrimary,
                        unfocusedTextColor = CiboTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Note
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text(stringResource(R.string.measurement_notes_label)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CiboTimerAzure,
                        unfocusedBorderColor = CiboCardBorder,
                        focusedTextColor = CiboTextPrimary,
                        unfocusedTextColor = CiboTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val wVal = weightText.replace(',', '.').toDoubleOrNull()
                    val isValid = wVal != null && WeightUnitConverter.isValidWeight(wVal, weightUnit)
                    weightError = !isValid
                    if (isValid) {
                        val wKg = WeightUnitConverter.displayToKg(wVal!!, weightUnit)
                        onSave(
                            wKg,
                            waistText.replace(',', '.').toDoubleOrNull(),
                            hipsText.replace(',', '.').toDoubleOrNull(),
                            chestText.replace(',', '.').toDoubleOrNull(),
                            thighsText.replace(',', '.').toDoubleOrNull(),
                            armsText.replace(',', '.').toDoubleOrNull(),
                            notesText,
                            epochMillis
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CiboTimerAzure,
                    contentColor = Color(0xFF031019)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.button_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel), color = CiboTextSecondary)
            }
        }
    )
}
