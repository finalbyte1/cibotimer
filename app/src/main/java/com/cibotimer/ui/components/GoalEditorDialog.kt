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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.domain.BmiCalculator
import com.cibotimer.ui.theme.*
import java.util.Locale

@Composable
fun GoalEditorDialog(
    initialStartingWeightKg: Double,
    initialTargetWeightKg: Double,
    initialHeightCm: Double,
    onDismiss: () -> Unit,
    onSave: (startingWeight: Double, targetWeight: Double, height: Double) -> Unit
) {
    var startingWeightText by remember { mutableStateOf(initialStartingWeightKg.toString()) }
    var targetWeightText by remember { mutableStateOf(initialTargetWeightKg.toString()) }
    var heightText by remember { mutableStateOf(initialHeightCm.toInt().toString()) }

    val targetWeight = targetWeightText.toDoubleOrNull() ?: initialTargetWeightKg
    val height = heightText.toDoubleOrNull() ?: initialHeightCm
    val targetBmi = remember(targetWeight, height) {
        BmiCalculator.calculate(targetWeight, height)
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
                    text = "Modifica Obiettivi e Peso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
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
                    text = "Configura il tuo peso di partenza, l'obiettivo da raggiungere e la tua altezza per monitorare i tuoi progressi.",
                    fontSize = 12.sp,
                    color = CiboTextSecondary
                )

                OutlinedTextField(
                    value = startingWeightText,
                    onValueChange = { startingWeightText = it },
                    label = { Text("Peso di Partenza (kg)") },
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

                OutlinedTextField(
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it },
                    label = { Text("Peso Obiettivo (kg)") },
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

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Altezza (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CiboTimerAzure,
                        unfocusedBorderColor = CiboCardBorder,
                        focusedTextColor = CiboTextPrimary,
                        unfocusedTextColor = CiboTextPrimary
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
                            text = "BMI all'obiettivo: %.1f".format(Locale.US, targetBmi.bmiValue),
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
                    val sW = startingWeightText.toDoubleOrNull()
                    val tW = targetWeightText.toDoubleOrNull()
                    val h = heightText.toDoubleOrNull()
                    if (sW != null && sW > 0 && tW != null && tW > 0 && h != null && h > 0) {
                        onSave(sW, tW, h)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CiboTimerAzure,
                    contentColor = Color(0xFF031019)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Salva", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = CiboTextSecondary)
            }
        }
    )
}
