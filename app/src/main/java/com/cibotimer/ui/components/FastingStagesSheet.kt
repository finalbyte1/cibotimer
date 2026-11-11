package com.cibotimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cibotimer.R
import com.cibotimer.data.models.FastingStage
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.ui.theme.*
import androidx.compose.ui.res.stringResource

@Composable
fun FastingStagesSheet(
    currentElapsedMinutes: Long,
    onDismiss: () -> Unit
) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.stages_sheet_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = CiboTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.plan_editor_close),
                            tint = CiboTextPrimary
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.stages_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CiboTextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(FastingCalculator.STAGES) { stage ->
                        val isActive = currentElapsedMinutes >= stage.startMinutes && currentElapsedMinutes < stage.endMinutes
                        val isPast = currentElapsedMinutes >= stage.endMinutes

                        StageItemRow(
                            stage = stage,
                            isActive = isActive,
                            isPast = isPast,
                            currentElapsedMinutes = currentElapsedMinutes
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StageItemRow(
    stage: FastingStage,
    isActive: Boolean,
    isPast: Boolean,
    currentElapsedMinutes: Long
) {
    val stageColor = Color(stage.colorHex)

    val statusBadgeText = when {
        isPast -> {
            val passedMinutes = currentElapsedMinutes - stage.endMinutes
            val passedHours = passedMinutes / 60
            if (passedHours > 0) "Completata • Superata da ${passedHours}h" else "Completata"
        }
        isActive -> {
            val elapH = currentElapsedMinutes / 60
            val elapM = currentElapsedMinutes % 60
            val remM = stage.endMinutes - currentElapsedMinutes
            val remH = remM / 60
            if (stage.endMinutes == Int.MAX_VALUE) {
                "In corso • ${elapH}h passate"
            } else {
                "In corso • ${elapH}h ${elapM}m passate (mancano ${remH}h)"
            }
        }
        else -> {
            val startDiffH = (stage.startMinutes - currentElapsedMinutes) / 60
            if (startDiffH > 0) "Tra ${startDiffH}h" else "Prossima"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) stageColor.copy(alpha = 0.08f) else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Step number circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isActive || isPast) stageColor else SurfaceVariantDark)
        ) {
            Text(
                text = "${stage.stageIndex}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stage.localizedTitle,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = if (isActive) stageColor else TextPrimary
                )
                Text(
                    text = stage.durationLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isActive) stageColor else TextSecondary
                )
            }

            // Badge stato e ore passate
            Text(
                text = statusBadgeText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isActive) stageColor else (if (isPast) Color(0xFF4ADE80) else TextSecondary),
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = stage.localizedSubtitle,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = stageColor,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = stage.localizedDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
