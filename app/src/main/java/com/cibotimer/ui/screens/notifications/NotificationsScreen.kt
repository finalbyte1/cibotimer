package com.cibotimer.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.data.models.AppNotificationItem
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.notifications.NotificationHelper
import com.cibotimer.ui.components.AppPrimaryButton
import com.cibotimer.ui.components.AppSecondaryButton
import com.cibotimer.ui.theme.*
import com.cibotimer.R
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifications by repository.notifications.collectAsState()
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CiboBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.nav_notifications),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboTextPrimary
                )
                Text(
                    text = if (notifications.isEmpty()) stringResource(R.string.notif_archive_empty) else stringResource(R.string.notif_archive_count, notifications.size),
                    fontSize = 13.sp,
                    color = CiboTextSecondary
                )
            }

            if (notifications.isNotEmpty()) {
                IconButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CiboCard)
                        .border(1.dp, CiboCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Cancella tutte le notifiche",
                        tint = CiboStopRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Info Banner: Spiegazione Notifica Live Timer Fissa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CiboCard)
                .border(1.dp, CiboCardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CiboTimerBlue.copy(alpha = 0.18f))
                        .border(1.dp, CiboTimerAzureGlow.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = CiboTimerAzureGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.notif_live_timer_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CiboTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.notif_live_timer_desc),
                        fontSize = 11.5.sp,
                        color = CiboTextSecondary,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { NotificationHelper.openChannelSettings(context) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.notif_channel_config_desc),
                        tint = CiboAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Lista Notifiche o Stato Vuoto
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CiboCard)
                            .border(1.dp, CiboCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = CiboTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.notif_empty_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CiboTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.notif_empty_desc),
                        fontSize = 13.sp,
                        color = CiboTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onDelete = { repository.deleteNotification(item.id) }
                    )
                }
            }
        }
    }

    // Dialog Conferma "Cancella tutte le notifiche"
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.notif_clear_dialog_title),
                    color = CiboTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.notif_clear_dialog_message),
                    color = CiboTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                AppPrimaryButton(
                    text = stringResource(R.string.notif_clear_dialog_confirm),
                    onClick = {
                        repository.clearAllNotifications()
                        showClearAllDialog = false
                    }
                )
            },
            dismissButton = {
                AppSecondaryButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { showClearAllDialog = false }
                )
            },
            containerColor = CiboSheetCard,
            titleContentColor = CiboTextPrimary,
            textContentColor = CiboTextSecondary
        )
    }
}

@Composable
fun NotificationCard(
    item: AppNotificationItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconEmoji, iconBg, iconBorder) = when (item.type) {
        "FASTING_END" -> Triple("🍽️", CiboTimerAzureGlow.copy(alpha = 0.16f), CiboTimerAzureGlow.copy(alpha = 0.5f))
        "EATING_END" -> Triple("🔥", CiboTimerBlue.copy(alpha = 0.18f), CiboTimerBlue.copy(alpha = 0.6f))
        "EATING_PRE_REMINDER" -> Triple("⏰", CiboTimerAzure.copy(alpha = 0.16f), CiboTimerAzure.copy(alpha = 0.5f))
        "STAGE_REACHED" -> Triple("✨", Color(0xFFC084FC).copy(alpha = 0.18f), Color(0xFFC084FC).copy(alpha = 0.6f))
        "WATER" -> Triple("💧", CiboTimerAzure.copy(alpha = 0.20f), CiboTimerAzure.copy(alpha = 0.6f))
        "WEIGHT" -> Triple("⚖️", Color(0xFF38BDF8).copy(alpha = 0.18f), Color(0xFF38BDF8).copy(alpha = 0.5f))
        else -> Triple("🔔", CiboCardBorder, CiboCardBorder)
    }

    val timeFormatted = remember(item.timestampMillis) {
        val now = System.currentTimeMillis()
        val diff = now - item.timestampMillis
        val calNow = Calendar.getInstance()
        val calItem = Calendar.getInstance().apply { timeInMillis = item.timestampMillis }

        val isToday = calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)

        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = calYesterday.get(Calendar.YEAR) == calItem.get(Calendar.YEAR) &&
                calYesterday.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)

        val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fullFormat = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())

        val timeStr = when {
            diff < 60_000L -> null
            isToday -> hourFormat.format(Date(item.timestampMillis))
            isYesterday -> hourFormat.format(Date(item.timestampMillis))
            else -> fullFormat.format(Date(item.timestampMillis)).replaceFirstChar { it.uppercase() }
        }
        Triple(diff < 60_000L, isToday, if (isYesterday) timeStr else if (diff >= 60_000L && !isToday) timeStr else null)
    }

    val displayTime = when {
        timeFormatted.first -> stringResource(R.string.notif_time_now)
        timeFormatted.second -> stringResource(R.string.notif_time_today, timeFormatted.third ?: "")
        timeFormatted.third != null -> {
            val calItem = Calendar.getInstance().apply { timeInMillis = item.timestampMillis }
            val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = calYesterday.get(Calendar.YEAR) == calItem.get(Calendar.YEAR) &&
                    calYesterday.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)
            if (isYesterday) stringResource(R.string.notif_time_yesterday, timeFormatted.third!!) else timeFormatted.third!!
        }
        else -> ""
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CiboCard)
            .border(1.dp, CiboCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Icona tipo notifica
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg)
                    .border(1.dp, iconBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconEmoji,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Testo Notifica
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.message,
                    fontSize = 12.5.sp,
                    color = CiboTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = displayTime,
                    fontSize = 11.sp,
                    color = CiboAccent,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pulsante Cancella Notifica
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancella notifica",
                    tint = CiboTextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
