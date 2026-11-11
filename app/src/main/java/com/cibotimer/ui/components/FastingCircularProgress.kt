package com.cibotimer.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.FastingState
import com.cibotimer.ui.theme.*

@Composable
fun FastingCircularProgress(
    remainingSeconds: Long,
    totalPlannedSeconds: Long,
    elapsedSeconds: Long,
    progressFraction: Float,
    state: FastingState,
    modifier: Modifier = Modifier.size(216.dp)
) {
    val isFasting = state == FastingState.FASTING
    val isStopped = state == FastingState.NOT_STARTED

    val safeRemaining = remainingSeconds.coerceAtLeast(0L)
    val remHours = safeRemaining / 3600
    val remMinutes = (safeRemaining % 3600) / 60
    val remSecs = safeRemaining % 60

    val safeElapsed = elapsedSeconds.coerceAtLeast(0L)
    val elapHours = safeElapsed / 3600
    val elapMinutes = (safeElapsed % 3600) / 60

    val safePlanned = totalPlannedSeconds.coerceAtLeast(0L)
    val plannedHours = safePlanned / 3600
    val plannedMinutes = (safePlanned % 3600) / 60

    val progressPercent = (progressFraction * 100f).toInt().coerceIn(0, 100)

    val contentDesc = when {
        isStopped -> "Timer fermo, tocca per iniziare il digiuno"
        isFasting -> stringResource(
            R.string.content_desc_timer_ring,
            progressPercent,
            remHours.toInt(),
            remMinutes.toInt()
        )
        else -> stringResource(
            R.string.content_desc_timer_ring_eating,
            progressPercent,
            remHours.toInt(),
            remMinutes.toInt()
        )
    }

    // Animazione di respiro sottile per l'alone del puntino / gemma attiva
    val infiniteTransition = rememberInfiniteTransition(label = "indicatorJewelGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics { contentDescription = contentDesc }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 13.5.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Raggi calibrati per safe-area e allineamento perfetto con l'icona
            val mainRadius = (size.minDimension / 2f) - 16.dp.toPx()
            val orbitRadius = (size.minDimension / 2f) - 4.dp.toPx()

            val topLeft = Offset(center.x - mainRadius, center.y - mainRadius)
            val arcSize = Size(mainRadius * 2f, mainRadius * 2f)

            // 1. AURA AMBIENTALE CENTRALE (Zaffiro / Ciano diffuso)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CiboTimerAzureGlow.copy(alpha = 0.09f),
                        CiboTimerBlue.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = mainRadius * 0.95f
                ),
                radius = mainRadius * 0.95f,
                center = center
            )

            // 2. ORBITA CIRCOLARE ESTERNA SOTTILE (Traccia tratteggiata + 4 punti cardinali ore 12, 3, 6, 9)
            drawCircle(
                color = CardBorderGlow.copy(alpha = 0.45f),
                radius = orbitRadius,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                )
            )

            val cardinalAngles = listOf(-90.0, 0.0, 90.0, 180.0)
            for (angle in cardinalAngles) {
                val rad = Math.toRadians(angle)
                val dotPos = Offset(
                    center.x + orbitRadius * Math.cos(rad).toFloat(),
                    center.y + orbitRadius * Math.sin(rad).toFloat()
                )
                drawCircle(
                    color = CiboTimerAzureGlow.copy(alpha = 0.60f),
                    radius = 2.dp.toPx(),
                    center = dotPos
                )
            }

            // 3. TRACCIA DI SFONDO SCURA (Deep Sapphire Twilight Navy #112338)
            drawCircle(
                color = Color(0xFF112338),
                radius = mainRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 4. ARCHI METABOLICI / TIMER
            if (isStopped) {
                // STATO FERMO: Mostra l'iconico cronometro metabolico 16:8 identico alla nuova icona
                // a) Arco di Digiuno (240°, da ore 13:00 / -30° in senso orario fino a ore 09:00 / 180°)
                // Underlay glow
                drawArc(
                    color = CiboTimerBlue.copy(alpha = 0.25f),
                    startAngle = -30f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 1.55f, cap = StrokeCap.Round)
                )
                // Core arc stroke
                drawArc(
                    color = CiboTimerBlue,
                    startAngle = -30f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Sky azure highlight (arco superiore da -30° a 0°)
                drawArc(
                    color = CiboTimerAzureLight.copy(alpha = 0.90f),
                    startAngle = -30f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // b) Arco Finestra Pasto (120°, da ore 09:00 / 180° in senso orario fino a ore 13:00 / -30°)
                // Outer neon glow
                drawArc(
                    color = CiboTimerAzureGlow.copy(alpha = 0.32f),
                    startAngle = 180f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 1.65f, cap = StrokeCap.Round)
                )
                // Mid glow
                drawArc(
                    color = CiboTimerAzureGlow.copy(alpha = 0.55f),
                    startAngle = 180f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 1.25f, cap = StrokeCap.Round)
                )
                // Core cyan stroke
                drawArc(
                    color = CiboTimerAzureGlow,
                    startAngle = 180f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // c) BAGLIORE INDICATORE ALL'INIZIO DELL'ANELLO A SINISTRA (Ore 9:00, 180°)
                val jewelRad = Math.toRadians(180.0)
                val jewelPos = Offset(
                    center.x + mainRadius * Math.cos(jewelRad).toFloat(),
                    center.y + mainRadius * Math.sin(jewelRad).toFloat()
                )
                // 1. Alone diffuso esterno (pulse halo espanso)
                drawCircle(
                    color = CiboTimerAzureGlow.copy(alpha = pulseAlpha * 0.35f),
                    radius = 15.dp.toPx(),
                    center = jewelPos
                )
                // 2. Alone intermedio vivido ciano
                drawCircle(
                    color = CiboTimerAzureGlow.copy(alpha = 0.65f),
                    radius = 10.dp.toPx(),
                    center = jewelPos
                )
                // 3. Nucleo ciano
                drawCircle(
                    color = CiboTimerAzureGlow,
                    radius = 6.5.dp.toPx(),
                    center = jewelPos
                )
                // 4. Bagliore bianco morbido
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = 5.0.dp.toPx(),
                    center = jewelPos
                )
                // 5. Centro bianco brillante
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = jewelPos
                )

            } else {
                // STATO ATTIVO: Countdown con anello che si svuota progressivamente
                val remainingFraction = if (totalPlannedSeconds > 0) {
                    (safeRemaining.toFloat() / safePlanned.toFloat()).coerceIn(0f, 1f)
                } else {
                    (1f - progressFraction).coerceIn(0f, 1f)
                }

                val elapsedFraction = (1f - remainingFraction).coerceIn(0f, 1f)
                val sweepAngle = remainingFraction * 360f
                val ringStartAngle = -90f + (elapsedFraction * 360f)

                val coreColor = if (isFasting) CiboTimerAzureGlow else CiboTimerAzure
                val glowColor = if (isFasting) CiboTimerBlue else CiboTimerAzureDark

                if (remainingFraction >= 0.999f) {
                    // Pieno al 100%: cerchio perfetto a 360 gradi con glow
                    drawCircle(
                        color = glowColor.copy(alpha = 0.28f),
                        radius = mainRadius,
                        center = center,
                        style = Stroke(width = strokeWidth * 1.55f)
                    )
                    drawCircle(
                        color = coreColor,
                        radius = mainRadius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                } else if (sweepAngle > 0f) {
                    // Multi-layer neon glow dell'arco attivo
                    // 1. Underlay glow
                    drawArc(
                        color = glowColor.copy(alpha = 0.28f),
                        startAngle = ringStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth * 1.55f, cap = StrokeCap.Round)
                    )
                    // 2. Mid glow
                    drawArc(
                        color = coreColor.copy(alpha = 0.50f),
                        startAngle = ringStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth * 1.25f, cap = StrokeCap.Round)
                    )
                    // 3. Core stroke
                    drawArc(
                        color = coreColor,
                        startAngle = ringStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // BAGLIORE INDICATORE ATTIVO: viaggia sulla testa dell'arco di countdown
                val headRad = Math.toRadians(ringStartAngle.toDouble())
                val jewelPos = Offset(
                    center.x + mainRadius * Math.cos(headRad).toFloat(),
                    center.y + mainRadius * Math.sin(headRad).toFloat()
                )
                // 1. Alone diffuso esterno (pulse halo espanso)
                drawCircle(
                    color = coreColor.copy(alpha = pulseAlpha * 0.35f),
                    radius = 15.dp.toPx(),
                    center = jewelPos
                )
                // 2. Alone intermedio vivido
                drawCircle(
                    color = coreColor.copy(alpha = 0.65f),
                    radius = 10.dp.toPx(),
                    center = jewelPos
                )
                // 3. Nucleo colorato
                drawCircle(
                    color = coreColor,
                    radius = 6.5.dp.toPx(),
                    center = jewelPos
                )
                // 4. Bagliore bianco morbido
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = 5.0.dp.toPx(),
                    center = jewelPos
                )
                // 5. Centro bianco brillante
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = jewelPos
                )
            }
        }

        // Testi al centro dell'anello
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // Badge maiuscolo (12sp)
            Text(
                text = when {
                    isStopped -> "FERMO"
                    isFasting -> stringResource(R.string.timer_badge_fasting)
                    else -> stringResource(R.string.timer_badge_eating)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    isStopped -> CiboTextSecondary
                    isFasting -> CiboTimerAzureGlow
                    else -> CiboTimerAzureLight
                },
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Etichetta stato / countdown (13sp)
            Text(
                text = when {
                    isStopped -> "Tocca Inizia per avviare"
                    isFasting -> stringResource(R.string.timer_countdown_label_fasting)
                    else -> stringResource(R.string.timer_countdown_label_eating)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = CiboTextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Countdown o Durata prevista (35sp bold con cifre tabulari, senza andare a capo)
            val digitsText = if (isStopped) {
                "%02d:%02d:00".format(plannedHours, plannedMinutes)
            } else {
                "%02d:%02d:%02d".format(remHours, remMinutes, remSecs)
            }

            Text(
                text = digitsText,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp,
                    color = CiboTextPrimary,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Sotto: "7h 7m · 39%" oppure "Durata prevista: 20h"
            val subText = if (isStopped) {
                "Durata prevista: ${plannedHours}h"
            } else {
                "${elapHours}h ${elapMinutes}m · $progressPercent%"
            }

            Text(
                text = subText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CiboTextSecondary,
                maxLines = 1
            )
        }
    }
}
