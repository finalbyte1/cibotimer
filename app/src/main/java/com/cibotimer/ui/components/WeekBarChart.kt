package com.cibotimer.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingDayFormatter
import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.domain.DateTimeFormatters
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.domain.TimelineSegment
import com.cibotimer.ui.theme.*
import java.util.Calendar
import java.util.Locale

/**
 * Visualizzazione a 7 barre orizzontali (Lun..Dom) 0-24h con diciture sotto ogni giorno (es. "Lun 20:00 → Mar 12:00").
 * Compatta e ottimizzata per garantire la piena visibilità di tutti i 7 giorni (Lun..Dom) su qualsiasi schermo.
 */
@Composable
fun WeekBarChart(
    days: List<DayPlan>,
    selectedDayOfWeek: Int,
    periods: List<FastingPeriod>? = null,
    onDaySelected: (Int) -> Unit = {},
    fillMaxHeight: Boolean = false,
    currentTimeFraction: Float? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orderedDays = remember(days) {
        days.sortedBy { if (it.dayOfWeek == 7) 7 else it.dayOfWeek }
    }

    // Calcolo dinamico in tempo reale dell'orario corrente (aggiornato ogni secondo con precisione assoluta)
    val internalTimeFraction = produceState(
        initialValue = {
            val cal = Calendar.getInstance()
            (cal.get(Calendar.HOUR_OF_DAY) * 3600f + cal.get(Calendar.MINUTE) * 60f + cal.get(Calendar.SECOND).toFloat()) / 86400f
        }()
    ) {
        while (true) {
            delay(1000L)
            val cal = Calendar.getInstance()
            value = (cal.get(Calendar.HOUR_OF_DAY) * 3600f + cal.get(Calendar.MINUTE) * 60f + cal.get(Calendar.SECOND).toFloat()) / 86400f
        }
    }
    val effectiveTimeFraction = currentTimeFraction ?: internalTimeFraction.value

    val todayDayOfWeek = run {
        val cal = Calendar.getInstance()
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        if (calDay == Calendar.SUNDAY) 7 else calDay - 1
    }

    data class RowDayInfo(
        val dayIndex: Int,
        val label: String,
        val isToday: Boolean,
        val isSelected: Boolean
    )

    val locale = LocalConfiguration.current.locales[0]

    val rowItems = remember(orderedDays, todayDayOfWeek, selectedDayOfWeek, locale) {
        (1..7).map { dayIdx ->
            RowDayInfo(
                dayIndex = dayIdx,
                label = DateTimeFormatters.formatShortDay(dayIdx, locale, capitalize = true),
                isToday = todayDayOfWeek == dayIdx,
                isSelected = selectedDayOfWeek == dayIdx
            )
        }
    }

    Surface(
        color = CiboCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CiboCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillMaxHeight) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = if (fillMaxHeight) Arrangement.SpaceBetween else Arrangement.spacedBy(2.dp)
        ) {
            // 1. Header: Titolo a sinistra + Legenda a destra
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.weekly_plan_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = CiboTextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pasto (#FBBF24)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CiboAmber)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.legend_eating),
                            fontSize = 11.sp,
                            color = CiboTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Digiuno (#38BDF8 / CiboAccent)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CiboAccent)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.legend_fasting),
                            fontSize = 11.sp,
                            color = CiboTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Griglia a 7 righe orizzontali (Lun - Dom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillMaxHeight) Modifier.weight(1f, fill = true) else Modifier.wrapContentHeight()),
                verticalArrangement = if (fillMaxHeight) Arrangement.SpaceEvenly else Arrangement.spacedBy(1.dp)
            ) {
                rowItems.forEach { item ->
                    val segments = remember(item.dayIndex, periods, orderedDays) {
                        if (!periods.isNullOrEmpty()) {
                            FastingCalculator.computePeriods24hSegments(periods, item.dayIndex)
                        } else {
                            val day = orderedDays.find { it.dayOfWeek == item.dayIndex } ?: orderedDays.first()
                            val prevDay = orderedDays.find {
                                it.dayOfWeek == (if (item.dayIndex == 1) 7 else item.dayIndex - 1)
                            } ?: day
                            FastingCalculator.compute24hSegments(day, prevDay)
                        }
                    }

                    val dayPlan = orderedDays.find { it.dayOfWeek == item.dayIndex } ?: orderedDays.first()
                    val subtitle = remember(item.dayIndex, periods, dayPlan, locale) {
                        getSubtitleForDay(context, item.dayIndex, item.label, periods, dayPlan, locale)
                    }

                    val isHighlighted = item.isSelected || item.isToday

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onDaySelected(item.dayIndex) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                color = if (item.isSelected) CiboAccent else if (item.isToday) Color(0xFF93C5FD) else CiboTextSecondary,
                                modifier = Modifier.width(30.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (item.isToday) 23.dp else (if (fillMaxHeight) 12.dp else 8.dp))
                            ) {
                                MainHorizontalBar24h(
                                    segments = segments,
                                    isToday = item.isToday,
                                    isSelected = item.isSelected,
                                    currentTimeFraction = if (item.isToday) effectiveTimeFraction else null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (dayPlan.isFastingDay) "${dayPlan.fastingDurationMinutes / 60} h" else stringResource(R.string.week_chart_free),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayPlan.isFastingDay) CiboAccent else CiboAmber,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        // Dicitura orari sotto la barra (es: "Lun 20:00 → Mar 12:00")
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            color = if (item.isSelected) CiboAccent.copy(alpha = 0.85f) else CiboTextSecondary.copy(alpha = 0.75f),
                            fontWeight = if (item.isSelected) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.padding(start = 34.dp)
                        )
                    }
                }
            }

            // 3. Asse 0h, 6h, 12h, 18h, 24h in basso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 34.dp, end = 38.dp, top = 2.dp),
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
}

/**
 * Calcola la dicitura sotto ogni giorno (es: "Lun 20:00 → Mar 12:00").
 */
private fun getSubtitleForDay(
    context: Context,
    dayIndex: Int,
    dayLabel: String,
    periods: List<FastingPeriod>?,
    dayPlan: DayPlan,
    locale: Locale = Locale.getDefault()
): String {
    val dayNames = (1..7).map { DateTimeFormatters.formatShortDay(it, locale, capitalize = true) }

    if (!periods.isNullOrEmpty()) {
        val p = periods.find { it.startDayOfWeek == dayIndex && it.isFasting }
        if (p != null) {
            val endDayName = dayNames.getOrElse(p.endDayOfWeek - 1) { dayLabel }
            val startT = DateTimeFormatters.formatTime(p.startHour, p.startMinute, context, locale)
            val endT = DateTimeFormatters.formatTime(p.endHour, p.endMinute, context, locale)
            return "$dayLabel $startT → $endDayName $endT"
        }
        val through = periods.find { it.endDayOfWeek == dayIndex && it.isFasting }
        if (through != null) {
            val startDayName = dayNames.getOrElse(through.startDayOfWeek - 1) { dayLabel }
            val endT = DateTimeFormatters.formatTime(through.endHour, through.endMinute, context, locale)
            return context.getString(R.string.week_chart_fasting_from, startDayName, endT)
        }
    }

    if (dayPlan.isFastingDay) {
        val startTotal = dayPlan.startHour * 60 + dayPlan.startMinute
        val isNextDay = (startTotal + dayPlan.fastingDurationMinutes) >= 24 * 60
        val nextDayIdx = if (dayPlan.dayOfWeek == 7) 1 else dayPlan.dayOfWeek + 1
        val endDayName = if (isNextDay) dayNames[nextDayIdx - 1] else dayLabel
        val startT = DateTimeFormatters.formatTime(dayPlan.startHour, dayPlan.startMinute, context, locale)
        val endT = DateTimeFormatters.formatTime(dayPlan.endHour, dayPlan.endMinute, context, locale)
        return "$dayLabel $startT → $endDayName $endT"
    } else {
        return context.getString(R.string.week_chart_no_fast)
    }
}

/**
 * Disegna la barra orizzontale 24h a segmenti con angoli arrotondati e indicatore di orario attuale.
 */
@Composable
fun MainHorizontalBar24h(
    segments: List<TimelineSegment>,
    isToday: Boolean,
    isSelected: Boolean = false,
    currentTimeFraction: Float? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val w = size.width
        val barHeight = if (isToday) 12.dp.toPx() else 8.dp.toPx()
        val barTop = if (isToday) 8.dp.toPx() else 0f
        val cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())

        // 1. Sfondo contenitore scuro
        drawRoundRect(
            color = Color(0xFF0F1A2E),
            topLeft = Offset(0f, barTop),
            size = Size(w, barHeight),
            cornerRadius = cornerRadius
        )

        // 2. Segmenti Digiuno (azzurro CiboAccent) e Pasto (ambra CiboAmber)
        val gapPx = 1.5.dp.toPx()
        val n = segments.size

        segments.forEachIndexed { i, seg ->
            var segStartX = (seg.startMin / 1440f) * w
            var segEndX = (seg.endMin / 1440f) * w

            if (i > 0) segStartX += gapPx / 2f
            if (i < n - 1) segEndX -= gapPx / 2f

            val segW = (segEndX - segStartX).coerceAtLeast(0f)
            if (segW > 0f) {
                val color = if (seg.isFasting) {
                    if (isToday) CiboAccent else CiboAccent.copy(alpha = 0.75f)
                } else {
                    if (isToday) CiboAmber else CiboAmber.copy(alpha = 0.65f)
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(segStartX, barTop),
                    size = Size(segW, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        // 3. Bordo di selezione / evidenziazione attorno alla sola barra
        if (isSelected) {
            drawRoundRect(
                color = CiboAccent,
                topLeft = Offset(0f, barTop),
                size = Size(w, barHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )
        } else if (isToday) {
            drawRoundRect(
                color = CiboAccent.copy(alpha = 0.5f),
                topLeft = Offset(0f, barTop),
                size = Size(w, barHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 4. Marcatore orario attuale per il giorno corrente (Oggi)
        if (isToday && currentTimeFraction != null) {
            val x = (currentTimeFraction.coerceIn(0f, 1f) * w)
            val triangleW = 12.dp.toPx()
            val triangleH = 8.dp.toPx()
            val lineOverhang = 3.dp.toPx()

            val lineTop = barTop - lineOverhang
            val lineBottom = barTop + barHeight + lineOverhang

            // Sottile ombra/alone scuro sotto la linea verticale per contrasto impeccabile su ogni segmento
            drawLine(
                color = Color(0xFF070B14),
                start = Offset(x, lineTop),
                end = Offset(x, lineBottom),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Linea verticale bianca spessa 2dp che attraversa la barra (sporge ~3dp sopra e sotto)
            drawLine(
                color = Color.White,
                start = Offset(x, lineTop),
                end = Offset(x, lineBottom),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Triangolo bianco rivolto verso il basso largo ~12dp e alto ~8dp subito sopra la barra
            val trianglePath = Path().apply {
                moveTo(x - triangleW / 2f, barTop - triangleH)
                lineTo(x + triangleW / 2f, barTop - triangleH)
                lineTo(x, barTop)
                close()
            }

            // Sottile contorno scuro per il triangolo per risaltare su ogni sfondo
            drawPath(
                path = trianglePath,
                color = Color(0xFF070B14),
                style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round)
            )

            // Triangolo bianco pieno
            drawPath(
                path = trianglePath,
                color = Color.White
            )
        }
    }
}
