package com.cibotimer.ui.screens.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.ui.theme.*
import androidx.compose.ui.res.stringResource

data class CoachProgramItem(
    val plan: FastingPlan,
    val subtitle: String,
    val weightLossRating: Int, // 1..4
    val healthRating: Int,     // 1..4
    val gradientColors: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSelectionStartScreen(
    repository: AppRepository,
    onPlanChosen: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val userProfile by repository.userProfile.collectAsState()

    val coachPrograms = listOf(
        CoachProgramItem(
            plan = FastingPlanTemplates.PLAN_20_4,
            subtitle = stringResource(R.string.plan_warrior_subtitle),
            weightLossRating = 4,
            healthRating = 3,
            gradientColors = listOf(Color(0xFF0C4A6E), Color(0xFF0369A1))
        ),
        CoachProgramItem(
            plan = FastingPlanTemplates.PLAN_16_8,
            subtitle = stringResource(R.string.plan_16_8_subtitle),
            weightLossRating = 3,
            healthRating = 4,
            gradientColors = listOf(Color(0xFF0A2E3D), Color(0xFF0077B6))
        ),
        CoachProgramItem(
            plan = FastingPlanTemplates.PLAN_AUTOPHAGY_BOOSTER,
            subtitle = stringResource(R.string.plan_dynamic_subtitle),
            weightLossRating = 4,
            healthRating = 4,
            gradientColors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
        ),
        CoachProgramItem(
            plan = FastingPlanTemplates.PLAN_18_6,
            subtitle = stringResource(R.string.plan_ketosis_subtitle),
            weightLossRating = 4,
            healthRating = 3,
            gradientColors = listOf(Color(0xFF082F49), Color(0xFF0284C7))
        )
    )

    val singlePlans = listOf(
        Pair(FastingPlanTemplates.PLAN_16_8, "Saltare la colazione"),
        Pair(FastingPlanTemplates.PLAN_AUTOPHAGY_BOOSTER, "Settimana di potenza • Perdi peso"),
        Pair(FastingPlanTemplates.PLAN_23_1_OMAD, "Un pasto al giorno (cena)"),
        Pair(FastingPlanTemplates.PLAN_14_10, "Principianti • Dolce"),
        Pair(FastingPlanTemplates.PLAN_18_6, "Intermedio avanzato • Chetosi"),
        Pair(FastingPlanTemplates.PLAN_5_2, "Due giorni completi (24h)")
    )

    fun selectAndProceed(plan: FastingPlan) {
        repository.setActivePlan(plan)
        repository.updateUserProfile(userProfile.copy(hasSelectedInitialPlan = true))
        onPlanChosen()
    }

    Scaffold(
        topBar = {
            if (onBack != null || userProfile.hasSelectedInitialPlan) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { onBack?.invoke() ?: onPlanChosen() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Indietro",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
                )
            }
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                // Top Header: Programmi Coach
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.program_coach_title),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.program_coach_subtitle),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Horizontal Carousel of Coach Programs
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(coachPrograms) { item ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderDark),
                            modifier = Modifier
                                .width(300.dp)
                                .height(380.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(item.gradientColors))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = item.plan.name,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Column {
                                        // Ratings (Perdita di peso / Salute)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(R.string.rating_weight_loss),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.9f)
                                                )
                                                RatingDots(rating = item.weightLossRating)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(R.string.rating_health),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.9f)
                                                )
                                                RatingDots(rating = item.healthRating)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Action Button: Inizia la settimana 1
                                        Button(
                                            onClick = { selectAndProceed(item.plan) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CiboTimerOrange,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.button_start_week_1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Piani singoli di digiuno
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.single_plans_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.single_plans_subtitle),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.plans_suggestions_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // 2-column Grid of Fasting Plans
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (i in singlePlans.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val plan1 = singlePlans[i]
                            PlanCardButton(
                                plan = plan1.first,
                                subtitle = plan1.second,
                                onClick = { selectAndProceed(plan1.first) },
                                modifier = Modifier.weight(1f)
                            )

                            if (i + 1 < singlePlans.size) {
                                val plan2 = singlePlans[i + 1]
                                PlanCardButton(
                                    plan = plan2.first,
                                    subtitle = plan2.second,
                                    onClick = { selectAndProceed(plan2.first) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDots(rating: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (i <= rating) Color.White else Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun PlanCardButton(
    plan: FastingPlan,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CiboTimerAzure.copy(alpha = 0.5f)),
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = plan.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CiboTimerAzure
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    lineHeight = 15.sp
                )
            }

            // Mini progress indicator bar
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfaceVariantDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(CiboTimerAzure)
                )
            }
        }
    }
}
