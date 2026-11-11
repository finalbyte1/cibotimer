package com.cibotimer.ui.screens.water

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTrackerScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val userProfile by repository.userProfile.collectAsState()
    val waterEntries by repository.waterEntries.collectAsState()

    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayEntries = remember(waterEntries) {
        waterEntries.filter { it.epochMillis >= startOfToday }.sortedByDescending { it.epochMillis }
    }

    val todayWaterMl = remember(todayEntries) {
        todayEntries.sumOf { it.amountMl }
    }

    val targetMl = userProfile.waterTargetMl.coerceAtLeast(500)
    val progressFraction = (todayWaterMl.toFloat() / targetMl).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "waterProgress"
    )

    var showCustomDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("") }
    var showTargetDialog by remember { mutableStateOf(false) }
    var targetInputText by remember { mutableStateOf(targetMl.toString()) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.ITALIAN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.water_tracker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { showTargetDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.water_content_desc_tune), tint = CiboTimerAzure)
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big Circular Gauge
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Indicator
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(200.dp),
                        color = CiboTimerAzure,
                        strokeWidth = 14.dp,
                        trackColor = CiboTimerAzure.copy(alpha = 0.15f)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = CiboTimerAzure,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$todayWaterMl ml",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.water_target_format, targetMl),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.water_progress_format, (progressFraction * 100).toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboTimerAzure
                        )
                    }
                }
            }

            // Quick Add Buttons Row
            item {
                Text(
                    text = stringResource(R.string.water_quick_add_header),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WaterPresetButton(label = stringResource(R.string.water_preset_150), sub = stringResource(R.string.water_sub_cup), onClick = { repository.addWater(150) }, modifier = Modifier.weight(1f))
                    WaterPresetButton(label = stringResource(R.string.water_preset_250), sub = stringResource(R.string.water_sub_glass), onClick = { repository.addWater(250) }, modifier = Modifier.weight(1f))
                    WaterPresetButton(label = stringResource(R.string.water_preset_500), sub = stringResource(R.string.water_sub_bottle), onClick = { repository.addWater(500) }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WaterPresetButton(label = stringResource(R.string.water_preset_750), sub = stringResource(R.string.water_sub_large_bottle), onClick = { repository.addWater(750) }, modifier = Modifier.weight(1f))
                    WaterPresetButton(label = stringResource(R.string.water_preset_1000), sub = stringResource(R.string.water_sub_1_liter), onClick = { repository.addWater(1000) }, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { showCustomDialog = true },
                        modifier = Modifier.weight(1f).height(62.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CiboTimerAzure.copy(alpha = 0.5f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CiboTimerAzure, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.water_preset_other), fontSize = 11.sp, color = CiboTimerAzure, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Undo Button if entries exist
            if (todayEntries.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { repository.removeLastWater() },
                        colors = ButtonDefaults.textButtonColors(contentColor = CiboTimerOrange)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.water_undo_last))
                    }
                }
            }

            // Today's History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.water_today_history_header, todayEntries.size),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }
            }

            if (todayEntries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.water_empty_state),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(todayEntries, key = { it.id }) { entry ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(CiboTimerAzure.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = CiboTimerAzure,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "+${entry.amountMl} ml",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = timeFormat.format(Date(entry.epochMillis)),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: Custom Water Amount
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(stringResource(R.string.water_dialog_custom_title)) },
            text = {
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.water_dialog_custom_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = customAmountText.toIntOrNull()
                        if (amount != null && amount > 0) {
                            repository.addWater(amount)
                        }
                        showCustomDialog = false
                        customAmountText = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CiboTimerAzure,
                        contentColor = Color(0xFF031019)
                    )
                ) {
                    Text(stringResource(R.string.water_dialog_add), color = Color(0xFF031019), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    // Dialog: Adjust Daily Water Target
    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text(stringResource(R.string.water_dialog_target_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.water_dialog_target_text))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetInputText,
                        onValueChange = { targetInputText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.water_dialog_target_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTarget = targetInputText.toIntOrNull()
                        if (newTarget != null && newTarget >= 500) {
                            repository.updateUserProfile(userProfile.copy(waterTargetMl = newTarget))
                        }
                        showTargetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CiboTimerAzure,
                        contentColor = Color(0xFF031019)
                    )
                ) {
                    Text(stringResource(R.string.button_save), color = Color(0xFF031019), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}

@Composable
fun WaterPresetButton(
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        shadowElevation = 1.dp,
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CiboTimerAzure
            )
            Text(
                text = sub,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}
