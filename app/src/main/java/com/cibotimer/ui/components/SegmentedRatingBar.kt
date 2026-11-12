package com.cibotimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.PlanRatings
import com.cibotimer.ui.theme.CiboTextSecondary

val SegmentedRatingActiveColor = Color(0xFF22B8E8)
val SegmentedRatingInactiveColor = Color(0xFF1F2F4B)

/**
 * Componente "misuratore a segmenti": 4 barrette arrotondate affiancate,
 * con quelle piene in azzurro #22B8E8 e quelle vuote in #1F2F4B,
 * con etichetta testuale a sinistra.
 */
@Composable
fun SegmentedRatingBar(
    label: String,
    rating: Int,
    maxRating: Int = 4,
    barWidth: Dp = 14.dp,
    barHeight: Dp = 6.dp,
    barSpacing: Dp = 3.dp,
    barCornerRadius: Dp = 2.dp,
    labelFontSize: TextUnit = 11.sp,
    labelColor: Color = CiboTextSecondary,
    activeColor: Color = SegmentedRatingActiveColor,
    inactiveColor: Color = SegmentedRatingInactiveColor,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = labelFontSize,
                fontWeight = FontWeight.Medium,
                color = labelColor
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(barSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..maxRating) {
                Box(
                    modifier = Modifier
                        .size(width = barWidth, height = barHeight)
                        .clip(RoundedCornerShape(barCornerRadius))
                        .background(if (i <= rating) activeColor else inactiveColor)
                )
            }
        }
    }
}

/**
 * Riga completa con misuratori affiancati:
 * "Dimagrimento ▰▰▰▰ · Salute ▰▰▰▰"
 */
@Composable
fun PlanRatingsRow(
    ratings: PlanRatings,
    modifier: Modifier = Modifier,
    barWidth: Dp = 14.dp,
    barHeight: Dp = 6.dp,
    labelFontSize: TextUnit = 11.sp,
    isLarge: Boolean = false,
    labelColor: Color = CiboTextSecondary
) {
    val bWidth = if (isLarge) 18.dp else barWidth
    val bHeight = if (isLarge) 7.dp else barHeight
    val fSize = if (isLarge) 12.sp else labelFontSize
    val spacing = if (isLarge) 12.dp else 8.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        SegmentedRatingBar(
            label = stringResource(R.string.rating_weight_loss_label),
            rating = ratings.weightLoss,
            barWidth = bWidth,
            barHeight = bHeight,
            labelFontSize = fSize,
            labelColor = labelColor
        )
        Text(
            text = "·",
            color = Color(0xFF4A5A75),
            fontSize = if (isLarge) 14.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
        SegmentedRatingBar(
            label = stringResource(R.string.rating_health_label),
            rating = ratings.health,
            barWidth = bWidth,
            barHeight = bHeight,
            labelFontSize = fSize,
            labelColor = labelColor
        )
    }
}
