package com.cibotimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.WeightEntry
import com.cibotimer.data.models.WeightUnit
import com.cibotimer.domain.DateTimeFormatters
import com.cibotimer.domain.WeightUnitConverter
import com.cibotimer.ui.theme.*
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ChartTimeRange(val labelResId: Int, val days: Int) {
    ONE_MONTH(R.string.chart_range_1_month, 30),
    THREE_MONTHS(R.string.chart_range_3_months, 90),
    ALL_TIME(R.string.chart_range_all, 0)
}

enum class ChartMetric(val labelResId: Int, val isWeight: Boolean = false, val fallbackUnit: String = "cm") {
    WEIGHT(R.string.metric_weight, isWeight = true, fallbackUnit = "kg"),
    WAIST(R.string.metric_waist, fallbackUnit = "cm"),
    HIPS(R.string.metric_hips, fallbackUnit = "cm"),
    CHEST(R.string.metric_chest, fallbackUnit = "cm"),
    THIGHS(R.string.metric_thighs, fallbackUnit = "cm"),
    ARMS(R.string.metric_arms, fallbackUnit = "cm");

    fun getUnit(weightUnit: WeightUnit): String = if (isWeight) weightUnit.symbol else fallbackUnit
}

data class ChartPoint(
    val epochMillis: Long,
    val value: Double,
    val formattedDate: String,
    val originalEntry: WeightEntry
)

@Composable
fun WeightProgressChart(
    entries: List<WeightEntry>,
    targetWeightKg: Double?,
    weightUnit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(ChartTimeRange.ONE_MONTH) }
    var selectedMetric by remember { mutableStateOf(ChartMetric.WEIGHT) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]

    // 1. Filtra i punti in base alla metrica e all'intervallo temporale
    val now = remember { System.currentTimeMillis() }
    val minEpoch = remember(selectedRange, now) {
        if (selectedRange.days > 0) {
            now - (selectedRange.days.toLong() * 24 * 3600 * 1000L)
        } else 0L
    }

    val chartPoints = remember(entries, selectedMetric, minEpoch, weightUnit, locale) {
        val filtered = entries.filter { entry ->
            val value = when (selectedMetric) {
                ChartMetric.WEIGHT -> entry.weightKg
                ChartMetric.WAIST -> entry.waistCm
                ChartMetric.HIPS -> entry.hipsCm
                ChartMetric.CHEST -> entry.chestCm
                ChartMetric.THIGHS -> entry.thighsCm
                ChartMetric.ARMS -> entry.armsCm
            }
            value != null && value > 0.0 && (minEpoch == 0L || entry.epochMillis >= minEpoch)
        }.sortedBy { it.epochMillis }

        filtered.map { entry ->
            val v = when (selectedMetric) {
                ChartMetric.WEIGHT -> WeightUnitConverter.kgToDisplay(entry.weightKg, weightUnit)
                ChartMetric.WAIST -> entry.waistCm ?: 0.0
                ChartMetric.HIPS -> entry.hipsCm ?: 0.0
                ChartMetric.CHEST -> entry.chestCm ?: 0.0
                ChartMetric.THIGHS -> entry.thighsCm ?: 0.0
                ChartMetric.ARMS -> entry.armsCm ?: 0.0
            }
            ChartPoint(
                epochMillis = entry.epochMillis,
                value = v,
                formattedDate = DateTimeFormatters.formatShortDate(entry.epochMillis, locale),
                originalEntry = entry
            )
        }
    }

    Surface(
        color = CiboCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CiboCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Titolo + Range Selector (1 Mese | 3 Mesi | Tutto)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = CiboTimerAzureGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.chart_evolution_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CiboTextPrimary
                    )
                }

                // Range Selector Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CiboBg)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ChartTimeRange.values().forEach { range ->
                        val isSelected = selectedRange == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CiboTimerAzure else Color.Transparent)
                                .clickable {
                                    selectedRange = range
                                    selectedPointIndex = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(range.labelResId),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF031019) else CiboTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Selector Chips (Peso | Vita | Fianchi | Torace | Cosce | Braccia)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    ChartMetric.WEIGHT,
                    ChartMetric.WAIST,
                    ChartMetric.HIPS,
                    ChartMetric.CHEST,
                    ChartMetric.THIGHS
                ).forEach { metric ->
                    val isSelected = selectedMetric == metric
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CiboTimerAzureContainer else CiboBg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CiboTimerAzure else CiboCardBorder
                        ),
                        modifier = Modifier
                            .clickable {
                                selectedMetric = metric
                                selectedPointIndex = null
                            }
                    ) {
                        Text(
                            text = stringResource(metric.labelResId),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CiboTimerAzureGlow else CiboTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tooltip or Active Selection Info
            val activePoint = selectedPointIndex?.let { chartPoints.getOrNull(it) }
                ?: chartPoints.lastOrNull()

            if (activePoint != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CiboBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = DateTimeFormatters.formatFullDateTime(activePoint.epochMillis, context, locale, includeDayOfWeek = true),
                            fontSize = 11.sp,
                            color = CiboTextSecondary
                        )
                        if (activePoint.originalEntry.notes.isNotBlank()) {
                            Text(
                                text = activePoint.originalEntry.notes,
                                fontSize = 11.sp,
                                color = CiboTimerAzureLight,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.1f".format(Locale.US, activePoint.value),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CiboTimerAzureGlow
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = selectedMetric.getUnit(weightUnit),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CiboTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Canvas Chart
            if (chartPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CiboBg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.chart_empty_measurement, stringResource(selectedMetric.labelResId).lowercase()),
                            fontSize = 13.sp,
                            color = CiboTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.chart_tap_to_record),
                            fontSize = 11.sp,
                            color = CiboTimerAzure
                        )
                    }
                }
            } else {
                // Determine min and max for chart Y scale
                val values = chartPoints.map { it.value }
                val target = if (selectedMetric == ChartMetric.WEIGHT && targetWeightKg != null) {
                    WeightUnitConverter.kgToDisplay(targetWeightKg, weightUnit)
                } else null

                val rawMin = min(values.minOrNull() ?: 0.0, target ?: Double.MAX_VALUE)
                val rawMax = max(values.maxOrNull() ?: 100.0, target ?: Double.MIN_VALUE)
                val paddingY = max(1.5, (rawMax - rawMin) * 0.15)
                val minY = (rawMin - paddingY).coerceAtLeast(0.0)
                val maxY = rawMax + paddingY

                val firstPoint = chartPoints.first()
                val lastPoint = chartPoints.last()
                val minTime = firstPoint.epochMillis
                val maxTime = lastPoint.epochMillis
                val timeSpan = max(1L, maxTime - minTime)

                var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(chartPoints) {
                                detectTapGestures { tapOffset ->
                                    if (chartPoints.isEmpty() || canvasSize.width <= 0) return@detectTapGestures
                                    val paddingLeft = 35.dp.toPx()
                                    val paddingRight = 15.dp.toPx()
                                    val availableWidth = canvasSize.width - paddingLeft - paddingRight

                                    // Find closest point by X coordinate
                                    var closestIndex = 0
                                    var closestDist = Float.MAX_VALUE

                                    chartPoints.forEachIndexed { idx, pt ->
                                        val xFraction = if (chartPoints.size == 1) 0.5f
                                        else (pt.epochMillis - minTime).toFloat() / timeSpan.toFloat()
                                        val ptX = paddingLeft + (xFraction * availableWidth)
                                        val dist = kotlin.math.abs(tapOffset.x - ptX)
                                        if (dist < closestDist) {
                                            closestDist = dist
                                            closestIndex = idx
                                        }
                                    }
                                    selectedPointIndex = closestIndex
                                }
                            }
                    ) {
                        canvasSize = size
                        val paddingLeft = 35.dp.toPx()
                        val paddingRight = 15.dp.toPx()
                        val paddingTop = 15.dp.toPx()
                        val paddingBottom = 25.dp.toPx()

                        val chartW = size.width - paddingLeft - paddingRight
                        val chartH = size.height - paddingTop - paddingBottom

                        fun getX(epochMillis: Long): Float {
                            if (chartPoints.size == 1) return paddingLeft + (chartW / 2f)
                            val fraction = (epochMillis - minTime).toFloat() / timeSpan.toFloat()
                            return paddingLeft + (fraction * chartW)
                        }

                        fun getY(v: Double): Float {
                            val range = maxY - minY
                            if (range <= 0.0) return paddingTop + (chartH / 2f)
                            val fraction = ((v - minY) / range).toFloat()
                            return paddingTop + chartH - (fraction * chartH)
                        }

                        // 1. Horizontal dashed grid lines
                        val steps = 3
                        for (i in 0..steps) {
                            val stepVal = minY + (i * (maxY - minY) / steps)
                            val yPos = getY(stepVal)

                            // Grid line
                            drawLine(
                                color = CiboCardBorder.copy(alpha = 0.5f),
                                start = Offset(paddingLeft, yPos),
                                end = Offset(size.width - paddingRight, yPos),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                        }

                        // 2. Horizontal Target Line (if Peso & target is set)
                        if (target != null && target in minY..maxY) {
                            val targetY = getY(target)
                            drawLine(
                                color = CiboTimerOrange.copy(alpha = 0.85f),
                                start = Offset(paddingLeft, targetY),
                                end = Offset(size.width - paddingRight, targetY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                            )
                        }

                        if (chartPoints.size >= 2) {
                            // 3. Area gradient fill
                            val fillPath = Path()
                            val linePath = Path()

                            val firstX = getX(chartPoints.first().epochMillis)
                            val firstY = getY(chartPoints.first().value)

                            linePath.moveTo(firstX, firstY)
                            fillPath.moveTo(firstX, paddingTop + chartH)
                            fillPath.lineTo(firstX, firstY)

                            for (i in 1 until chartPoints.size) {
                                val prev = chartPoints[i - 1]
                                val curr = chartPoints[i]

                                val p0X = getX(prev.epochMillis)
                                val p0Y = getY(prev.value)
                                val p1X = getX(curr.epochMillis)
                                val p1Y = getY(curr.value)

                                val midX = (p0X + p1X) / 2f
                                linePath.cubicTo(midX, p0Y, midX, p1Y, p1X, p1Y)
                                fillPath.cubicTo(midX, p0Y, midX, p1Y, p1X, p1Y)
                            }

                            val lastX = getX(chartPoints.last().epochMillis)
                            fillPath.lineTo(lastX, paddingTop + chartH)
                            fillPath.close()

                            // Fill gradient
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        CiboTimerAzure.copy(alpha = 0.28f),
                                        CiboTimerAzure.copy(alpha = 0.02f),
                                        Color.Transparent
                                    ),
                                    startY = paddingTop,
                                    endY = paddingTop + chartH
                                )
                            )

                            // Stroke line
                            drawPath(
                                path = linePath,
                                color = CiboTimerAzureGlow,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // 4. Draw points
                        chartPoints.forEachIndexed { idx, pt ->
                            val ptX = getX(pt.epochMillis)
                            val ptY = getY(pt.value)
                            val isSelected = selectedPointIndex == idx

                            if (isSelected) {
                                // Vertical selection line
                                drawLine(
                                    color = CiboTimerAzureGlow.copy(alpha = 0.6f),
                                    start = Offset(ptX, paddingTop),
                                    end = Offset(ptX, paddingTop + chartH),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                                )

                                // Outer glow circle
                                drawCircle(
                                    color = CiboTimerAzureGlow.copy(alpha = 0.35f),
                                    radius = 9.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                            } else {
                                drawCircle(
                                    color = CiboCard,
                                    radius = 5.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                                drawCircle(
                                    color = CiboTimerAzure,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                            }
                        }
                    }

                    // Static Target Badge (right side)
                    if (target != null && target in minY..maxY) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp, top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CiboTimerOrangeContainer,
                                border = BorderStroke(1.dp, CiboTimerOrange.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = stringResource(R.string.share_target_format, target, selectedMetric.getUnit(weightUnit)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CiboTimerOrange,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Period Summary Delta
                if (chartPoints.size >= 2) {
                    val delta = chartPoints.last().value - chartPoints.first().value
                    val isLoss = delta <= 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${firstPoint.formattedDate} ➔ ${lastPoint.formattedDate}",
                            fontSize = 11.sp,
                            color = CiboTextSecondary
                        )

                        Text(
                            text = if (isLoss) stringResource(R.string.chart_variation_format, delta, selectedMetric.getUnit(weightUnit))
                            else stringResource(R.string.chart_variation_plus_format, delta, selectedMetric.getUnit(weightUnit)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLoss) CiboTimerAzureGlow else CiboTimerOrange
                        )
                    }
                }
            }
        }
    }
}
