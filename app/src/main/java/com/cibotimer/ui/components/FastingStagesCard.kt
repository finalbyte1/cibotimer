package com.cibotimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.cibotimer.R
import com.cibotimer.data.models.FastingState
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.ui.theme.*

class NotchedCardShape(
    private val cornerRadius: androidx.compose.ui.unit.Dp = 14.dp,
    private val notchWidth: androidx.compose.ui.unit.Dp = 152.dp,
    private val notchHeight: androidx.compose.ui.unit.Dp = 52.dp,
    private val notchRadius: androidx.compose.ui.unit.Dp = 26.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val notchWidthPx = with(density) { notchWidth.toPx() }
        val notchHeightPx = with(density) { notchHeight.toPx() }
        val notchRadiusPx = with(density) { notchRadius.toPx() }

        val baseRect = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
        }

        val notchRect = Path().apply {
            val left = (size.width - notchWidthPx) / 2f
            val top = -notchHeightPx / 2f
            val right = left + notchWidthPx
            val bottom = notchHeightPx / 2f
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(notchRadiusPx, notchRadiusPx)
                )
            )
        }

        val combinedPath = Path().apply {
            op(baseRect, notchRect, PathOperation.Difference)
        }

        return Outline.Generic(combinedPath)
    }
}

@Composable
fun FastingStagesCard(
    elapsedSeconds: Long,
    state: FastingState,
    modifier: Modifier = Modifier,
    shape: Shape = remember { NotchedCardShape() },
    startTimeFormatted: String = "",
    endTimeFormatted: String = "",
    onInizioClick: () -> Unit = {},
    onFineClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isFasting = state == FastingState.FASTING
    val safeElapsedSec = elapsedSeconds.coerceAtLeast(0L)
    val elapsedMinutes = safeElapsedSec / 60
    val elapHours = safeElapsedSec / 3600
    val elapMinsRemainder = (safeElapsedSec % 3600) / 60

    val currentStage = remember(elapsedMinutes) {
        FastingCalculator.getCurrentStage(elapsedMinutes)
    }
    val stageColor = Color(currentStage.colorHex)

    // Segmenti metabolici basati sulle tappe fisiologiche ufficiali di FastingCalculator.STAGES:
    // 1. 0-4h (Digestione), 2. 4-8h (Calo Insulina), 3. 8-12h (Combustione Grassi),
    // 4. 12-16h (Chetosi), 5. 16-24h (Autofagia), 6. 24h+ (Ormone Crescita & Rigenerazione)
    val metabolicMilestones = listOf(
        0 to 240,     // 1. Digestione & Glicemia (0-4h)
        240 to 480,   // 2. Calo Insulina (4-8h)
        480 to 720,   // 3. Combustione Grassi (8-12h)
        720 to 960,   // 4. Chetosi (12-16h)
        960 to 1440,  // 5. Autofagia (16-24h)
        1440 to 2880  // 6. Ormone Crescita & Rigenerazione (24h+)
    )

    val stageProgresses = metabolicMilestones.map { (startMin, endMin) ->
        if (!isFasting) {
            0f
        } else if (elapsedMinutes >= endMin) {
            1f
        } else if (elapsedMinutes < startMin) {
            0f
        } else {
            val span = (endMin - startMin).toFloat()
            ((elapsedMinutes - startMin) / span).coerceIn(0f, 1f)
        }
    }

    // Titolo e descrizione fase corrente tenendo conto rigorosamente delle ORE PASSATE
    val elapsedHoursFormatted = if (elapHours > 0) {
        stringResource(R.string.elapsed_time_hours_mins, elapHours, elapMinsRemainder)
    } else {
        stringResource(R.string.elapsed_time_mins, elapMinsRemainder)
    }

    val phasePrefix = stringResource(R.string.stage_phase_prefix)
    val eatingWindowLabel = stringResource(R.string.header_subtitle_eating)
    val (stageTitle, stageDesc) = if (!isFasting) {
        Pair(
            if (elapHours > 0 || elapMinsRemainder > 0) "$eatingWindowLabel ($elapsedHoursFormatted)" else stringResource(R.string.stage_eating_title),
            stringResource(R.string.stage_eating_desc)
        )
    } else {
        Pair(
            "$phasePrefix ${currentStage.stageIndex}: ${currentStage.localizedTitle} ($elapsedHoursFormatted)",
            "${currentStage.localizedSubtitle}. ${currentStage.localizedDescription}"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CiboCard)
            .border(1.dp, if (isFasting) stageColor.copy(alpha = 0.45f) else CiboCardBorder, shape)
            .clickable(onClick = onClick)
    ) {
        // Blocco Inizio (Alto Sinistra)
        if (startTimeFormatted.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(bottomEnd = 12.dp))
                    .clickable(onClick = onInizioClick)
                    .padding(start = 12.dp, top = 12.dp, end = 2.dp, bottom = 8.dp)
                    .defaultMinSize(minHeight = 44.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state == FastingState.EATING) stringResource(R.string.timer_label_start_eating) else stringResource(R.string.timer_label_start),
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        color = CiboTextSecondary,
                        maxLines = 2,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.content_desc_edit_start_time),
                        tint = CiboAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = startTimeFormatted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Blocco Fine (Alto Destra)
        if (endTimeFormatted.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                    .clickable(onClick = onFineClick)
                    .padding(end = 12.dp, top = 12.dp, start = 2.dp, bottom = 8.dp)
                    .defaultMinSize(minHeight = 44.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (state == FastingState.FASTING) {
                            stringResource(R.string.timer_label_end_fasting)
                        } else if (state == FastingState.EATING) {
                            stringResource(R.string.timer_label_end_eating)
                        } else {
                            stringResource(R.string.timer_label_next_short)
                        },
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        color = CiboTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.content_desc_edit_end_time),
                        tint = CiboAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = endTimeFormatted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CiboTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Linea del tempo (Barra segmenti) centrata sotto l'incavo, tra Inizio e Fine
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 86.dp, end = 86.dp, top = 38.dp)
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stageProgresses.forEach { progress ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(CiboTrack)
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isFasting) stageColor else CiboAccent)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 48.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Icona Fase del Digiuno grande ed a colori nativi con badge fase
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isFasting) stageColor.copy(alpha = 0.16f) else CiboTrack)
                .border(1.dp, if (isFasting) stageColor.copy(alpha = 0.75f) else CiboCardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isFasting) "🔥" else "🍽️",
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )

            if (isFasting) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(stageColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentStage.stageIndex}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contenuto destro: Testo fase
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            val annotatedText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = CiboTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(stageTitle)
                }
                append(" ")
                withStyle(
                    style = SpanStyle(
                        color = CiboTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                ) {
                    append(stageDesc)
                }
            }

            Text(
                text = annotatedText,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
}
