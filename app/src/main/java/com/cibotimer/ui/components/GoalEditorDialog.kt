package com.cibotimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.WeightUnit
import com.cibotimer.domain.BmiCalculator
import com.cibotimer.domain.WeightUnitConverter
import com.cibotimer.ui.theme.*
import java.util.Locale

@Composable
fun GoalEditorDialog(
    initialStartingWeightKg: Double,
    initialTargetWeightKg: Double,
    initialHeightCm: Double,
    weightUnit: WeightUnit = WeightUnit.KG,
    onDismiss: () -> Unit,
    onSave: (startingWeight: Double, targetWeight: Double, height: Double) -> Unit
) {
    var startingWeightText by remember {
        mutableStateOf(WeightUnitConverter.formatWeight(initialStartingWeightKg, weightUnit, showUnit = false))
    }
    var targetWeightText by remember {
        mutableStateOf(WeightUnitConverter.formatWeight(initialTargetWeightKg, weightUnit, showUnit = false))
    }
    var heightText by remember { mutableStateOf(initialHeightCm.toInt().toString()) }

    var startingError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }
    var heightError by remember { mutableStateOf(false) }

    val (minBound, maxBound) = remember(weightUnit) {
        WeightUnitConverter.getFormattedBounds(weightUnit)
    }
    val weightErrorMessage = stringResource(R.string.weight_validation_error, minBound, maxBound)

    val targetDisplay = targetWeightText.replace(',', '.').toDoubleOrNull()
    val targetKg = if (targetDisplay != null && WeightUnitConverter.isValidWeight(targetDisplay, weightUnit)) {
        WeightUnitConverter.displayToKg(targetDisplay, weightUnit)
    } else {
        initialTargetWeightKg
    }
    val height = heightText.replace(',', '.').toDoubleOrNull() ?: initialHeightCm
    val targetBmi = remember(targetKg, height) {
        BmiCalculator.calculate(targetKg, height)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CiboSheetBg,
        titleContentColor = CiboTextPrimary,
        textContentColor = CiboTextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = CiboTimerAzureGlow,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.goal_editor_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = CiboTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.goal_editor_desc),
                    fontSize = 12.sp,
                    color = CiboTextSecondary
                )

                OutlinedTextField(
                    value = startingWeightText,
                    onValueChange = {
                        startingWeightText = it
                        startingError = false
                    },
                    label = { Text(stringResource(R.string.goal_editor_starting_weight, weightUnit.symbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = startingError,
                    supportingText = if (startingError) {
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

                OutlinedTextField(
                    value = targetWeightText,
                    onValueChange = {
                        targetWeightText = it
                        targetError = false
                    },
                    label = { Text(stringResource(R.string.goal_editor_target_weight, weightUnit.symbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = targetError,
                    supportingText = if (targetError) {
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

                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        heightText = it
                        heightError = false
                    },
                    label = { Text(stringResource(R.string.goal_editor_height)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = heightError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CiboTimerAzure,
                        unfocusedBorderColor = CiboCardBorder,
                        focusedTextColor = CiboTextPrimary,
                        unfocusedTextColor = CiboTextPrimary,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // BMI Preview at Target
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CiboCard,
                    border = BorderStroke(1.dp, CiboCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.goal_editor_target_bmi, targetBmi.bmiValue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CiboTextPrimary
                        )
                        Text(
                            text = targetBmi.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(targetBmi.colorHex)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sWVal = startingWeightText.replace(',', '.').toDoubleOrNull()
                    val tWVal = targetWeightText.replace(',', '.').toDoubleOrNull()
                    val hVal = heightText.replace(',', '.').toDoubleOrNull()

                    val isStartingValid = sWVal != null && WeightUnitConverter.isValidWeight(sWVal, weightUnit)
                    val isTargetValid = tWVal != null && WeightUnitConverter.isValidWeight(tWVal, weightUnit)
                    val isHeightValid = hVal != null && hVal in 50.0..280.0

                    startingError = !isStartingValid
                    targetError = !isTargetValid
                    heightError = !isHeightValid

                    if (isStartingValid && isTargetValid && isHeightValid) {
                        val sWKg = WeightUnitConverter.displayToKg(sWVal!!, weightUnit)
                        val tWKg = WeightUnitConverter.displayToKg(tWVal!!, weightUnit)
                        onSave(sWKg, tWKg, hVal!!)
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
