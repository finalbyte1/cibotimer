package com.cibotimer.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.domain.TotalCircumferenceInfo
import com.cibotimer.domain.WeightProgressInfo
import com.cibotimer.ui.theme.*
import androidx.compose.ui.res.stringResource
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProgressModal(
    progressInfo: WeightProgressInfo,
    totalFastingHours: Int,
    completedSessionsCount: Int,
    circumferenceInfo: TotalCircumferenceInfo?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val shareText = buildString {
        append(context.getString(R.string.share_text_header))
        if (progressInfo.lostKg > 0) {
            append(context.getString(R.string.share_text_weight_lost, progressInfo.lostKg, progressInfo.startingWeightKg, progressInfo.currentWeightKg))
        } else {
            append(context.getString(R.string.share_text_current_weight, progressInfo.currentWeightKg, progressInfo.startingWeightKg))
        }
        append(context.getString(R.string.share_text_goal, progressInfo.targetWeightKg, progressInfo.progressPercent, progressInfo.remainingKg))
        append(context.getString(R.string.share_text_fasting_hours, totalFastingHours, completedSessionsCount))
        if (circumferenceInfo?.totalDiffCm != null) {
            append(context.getString(R.string.share_text_circumference, circumferenceInfo.totalDiffCm))
        }
        append(context.getString(R.string.share_text_footer))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CiboSheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CiboTextSecondary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.share_progress_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CiboTextPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.content_desc_close),
                        tint = CiboTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Anteprima Condivisione
            Surface(
                color = CiboCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, CiboTimerAzureGlow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⏱️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.share_progress_brand),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = CiboTimerAzureGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hero Stat
                    if (progressInfo.lostKg > 0) {
                        Text(
                            text = "-%.1f kg".format(Locale.US, progressInfo.lostKg),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = CiboTimerAzureGlow
                        )
                        Text(
                            text = stringResource(R.string.share_weight_lost),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboTextSecondary,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = "%.1f kg".format(Locale.US, progressInfo.currentWeightKg),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = CiboTimerAzureGlow
                        )
                        Text(
                            text = stringResource(R.string.share_current_weight),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboTextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Goal & Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.share_starting_format, progressInfo.startingWeightKg),
                                fontSize = 11.sp,
                                color = CiboTextSecondary
                            )
                            Text(
                                text = stringResource(R.string.share_target_format, progressInfo.targetWeightKg),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CiboTimerAzure
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progressInfo.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CiboTimerAzureGlow,
                            trackColor = CiboTrack
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (progressInfo.isGoalReached) stringResource(R.string.share_goal_reached)
                            else stringResource(R.string.share_goal_progress, progressInfo.progressPercent, progressInfo.remainingKg),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (progressInfo.isGoalReached) CiboTimerAzureGlow else CiboTextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Badges (Fasting Hours & Measurements)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CiboBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$totalFastingHours h",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = CiboTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.share_total_fast),
                                    fontSize = 10.sp,
                                    color = CiboTextSecondary
                                )
                            }
                        }

                        if (circumferenceInfo?.totalDiffCm != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CiboBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "%+.1f cm".format(Locale.US, circumferenceInfo.totalDiffCm),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (circumferenceInfo.totalDiffCm <= 0) CiboTimerAzureGlow else CiboTimerOrange
                                    )
                                    Text(
                                        text = stringResource(R.string.share_body_measurements),
                                        fontSize = 10.sp,
                                        color = CiboTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Condividi
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title))
                    context.startActivity(shareIntent)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CiboTimerAzure,
                    contentColor = Color(0xFF031019)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color(0xFF031019)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.share_button),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color(0xFF031019)
                )
            }
        }
    }
}
