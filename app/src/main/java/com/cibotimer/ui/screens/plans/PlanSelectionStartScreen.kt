package com.cibotimer.ui.screens.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.R
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.PlanRatings
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.ratings
import com.cibotimer.ui.components.PlanRatingsRow
import com.cibotimer.ui.theme.*

private val CoachCardBg = Color(0xFF0F1B30)
private val CoachCardBorder = Color(0xFF1C2D4A)
private val CoachBlue = Color(0xFF2A8FD0)
private val CoachGreen = Color(0xFF1F9D72)
private val CoachAzure = Color(0xFF22B8E8)
private val CoachDarkText = Color(0xFF04202B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSelectionStartScreen(
    repository: AppRepository,
    onPlanChosen: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val userProfile by repository.userProfile.collectAsState()

    fun selectAndProceed(plan: FastingPlan) {
        repository.setActivePlan(plan)
        repository.updateUserProfile(userProfile.copy(hasSelectedInitialPlan = true))
        onPlanChosen()
    }

    val recommendedPlan = FastingPlanTemplates.PLAN_16_8
    val compactPlans = listOf(
        FastingPlanTemplates.PLAN_20_4,
        FastingPlanTemplates.PLAN_18_6,
        FastingPlanTemplates.PLAN_AUTOPHAGY_BOOSTER
    )

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTabletLandscape = screenWidthDp >= 840
    val isTabletPortrait = screenWidthDp in 600..839

    Scaffold(
        topBar = {
            if (onBack != null || userProfile.hasSelectedInitialPlan) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { onBack?.invoke() ?: onPlanChosen() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                                tint = CiboTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CiboBg)
                )
            }
        },
        containerColor = CiboBg,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header: Titolo "Programmi Coach" e sottotitolo "Il Coach ti guida lungo il tuo percorso"
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.program_coach_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CiboTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.program_coach_subtitle),
                        fontSize = 14.sp,
                        color = CiboTextSecondary
                    )
                }
            }

            // SEZIONE PROGRAMMI COACH (ADATTIVA)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when {
                        // 1. Tablet in orizzontale (>= 840dp): Due colonne affiancate
                        isTabletLandscape -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Colonna Sinistra: Card in evidenza (16-8 Classico)
                                CoachFeaturedCard(
                                    plan = recommendedPlan,
                                    onStart = { selectAndProceed(recommendedPlan) },
                                    modifier = Modifier.weight(1.1f)
                                )

                                // Colonna Destra: Tre card compatte in colonna
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    compactPlans.forEach { plan ->
                                        CoachCompactCard(
                                            plan = plan,
                                            onStart = { selectAndProceed(plan) }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Tablet in verticale e schermi medi (600–840dp): Colonna centrata max 600dp
                        isTabletPortrait -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CoachFeaturedCard(
                                    plan = recommendedPlan,
                                    onStart = { selectAndProceed(recommendedPlan) }
                                )

                                compactPlans.forEach { plan ->
                                    CoachCompactCard(
                                        plan = plan,
                                        onStart = { selectAndProceed(plan) }
                                    )
                                }
                            }
                        }

                        // 3. Telefono (< 600dp): Colonna unica a tutta larghezza
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CoachFeaturedCard(
                                    plan = recommendedPlan,
                                    onStart = { selectAndProceed(recommendedPlan) }
                                )

                                compactPlans.forEach { plan ->
                                    CoachCompactCard(
                                        plan = plan,
                                        onStart = { selectAndProceed(plan) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SEZIONE PIANI SINGOLI: Scegli tu il metodo di digiuno
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isTabletPortrait) Modifier.widthIn(max = 600.dp) else Modifier),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Titolo sezione
                        Text(
                            text = stringResource(R.string.single_plans_header_title) + " — " + stringResource(R.string.single_plans_header_subtitle),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboTextPrimary
                        )

                        // Due tile compatte (16-8 e Autophagy Booster)
                        if (isTabletLandscape) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SinglePlanTile(
                                    title = FastingPlanTemplates.PLAN_16_8.localizedName,
                                    subtitle = stringResource(R.string.single_plan_16_8_subtitle),
                                    icon = Icons.Default.Schedule,
                                    onClick = { selectAndProceed(FastingPlanTemplates.PLAN_16_8) },
                                    modifier = Modifier.weight(1f)
                                )
                                SinglePlanTile(
                                    title = FastingPlanTemplates.PLAN_AUTOPHAGY_BOOSTER.localizedName,
                                    subtitle = stringResource(R.string.single_plan_autophagy_subtitle),
                                    icon = Icons.Default.AutoAwesome,
                                    onClick = { selectAndProceed(FastingPlanTemplates.PLAN_AUTOPHAGY_BOOSTER) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            SinglePlanTile(
                                title = FastingPlanTemplates.PLAN_16_8.localizedName,
                                subtitle = stringResource(R.string.single_plan_16_8_subtitle),
                                icon = Icons.Default.Schedule,
                                onClick = { selectAndProceed(FastingPlanTemplates.PLAN_16_8) }
                            )
                            SinglePlanTile(
                                title = FastingPlanTemplates.PLAN_AUTOPHAGY_BOOSTER.localizedName,
                                subtitle = stringResource(R.string.single_plan_autophagy_subtitle),
                                icon = Icons.Default.AutoAwesome,
                                onClick = { selectAndProceed(FastingPlanTemplates.PLAN_AUTOPHAGY_BOOSTER) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card in evidenza per il programma consigliato (16-8 Classico)
 */
@Composable
fun CoachFeaturedCard(
    plan: FastingPlan,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratings = plan.ratings ?: PlanRatings(weightLoss = 3, health = 4)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CoachCardBg,
        border = BorderStroke(1.dp, CoachCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Chip verde "Consigliato per iniziare"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CoachGreen.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, CoachGreen.copy(alpha = 0.5f))
            ) {
                Text(
                    text = stringResource(R.string.coach_badge_recommended_start),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Titolo grande
            Text(
                text = plan.localizedName,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Descrizione
            Text(
                text = plan.localizedDescription,
                fontSize = 13.sp,
                color = CiboTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Barra delle 24 ore (16h digiuno blu #2A8FD0, 8h pasto verde #1F9D72)
            Coach24hBar(
                fastingHours = 16,
                eatingHours = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Legenda barra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CoachBlue)
                    )
                    Text(
                        text = stringResource(R.string.legend_hours_fasting, 16),
                        fontSize = 12.sp,
                        color = CiboTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CoachGreen)
                    )
                    Text(
                        text = stringResource(R.string.legend_hours_eating, 8),
                        fontSize = 12.sp,
                        color = CiboTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Misuratori Dimagrimento e Salute (formato grande)
            PlanRatingsRow(
                ratings = ratings,
                isLarge = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Pulsante pieno azzurro #22B8E8 "Inizia settimana 1" (testo scuro #04202B)
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoachAzure,
                    contentColor = CoachDarkText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.button_start_week_1_short),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoachDarkText
                )
            }
        }
    }
}

/**
 * Card compatta per gli altri programmi Coach (20-4, 18-6, Autophagy Booster)
 */
@Composable
fun CoachCompactCard(
    plan: FastingPlan,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratings = plan.ratings ?: PlanRatings(weightLoss = 4, health = 4)
    val isAutophagy = plan.id == "autophagy-booster"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CoachCardBg,
        border = BorderStroke(1.dp, CoachCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Titolo
            Text(
                text = plan.localizedName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Descrizione su una riga con ellipsize
            Text(
                text = plan.localizedDescription,
                fontSize = 12.sp,
                color = CiboTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mini barra 24h con pasto in verde (o 7 pillole giornaliere per Autophagy Booster)
            if (isAutophagy) {
                AutophagyBoosterPills(modifier = Modifier.fillMaxWidth())
            } else {
                val (fastingH, eatingH) = when (plan.id) {
                    "20-4" -> Pair(20, 4)
                    "18-6" -> Pair(18, 6)
                    else -> Pair(16, 8)
                }
                Coach24hBar(
                    fastingHours = fastingH,
                    eatingHours = eatingH,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Riga inferiore: Misuratori piccoli a sinistra, Pulsante a contorno a destra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Misuratori piccoli
                PlanRatingsRow(
                    ratings = ratings,
                    barWidth = 14.dp,
                    barHeight = 5.dp,
                    labelFontSize = 11.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Pulsante "Inizia" a contorno azzurro #22B8E8 con freccia
                OutlinedButton(
                    onClick = onStart,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CoachAzure),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CoachAzure
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.button_start_plan),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoachAzure
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = CoachAzure,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tile compatta per i piani singoli
 */
@Composable
fun SinglePlanTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CoachCardBg,
        border = BorderStroke(1.dp, CoachCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF14233D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CoachAzure,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = CiboTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = CoachAzure,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Barra 24h: finestra pasto in verde #1F9D72 e digiuno in blu #2A8FD0
 */
@Composable
fun Coach24hBar(
    fastingHours: Int,
    eatingHours: Int,
    modifier: Modifier = Modifier
) {
    // Normalizzazione standard sulle 24h
    // Per piani tipo 16-8, 18-6, 20-4: il pasto tipico è a metà/tardo giorno
    val fastRatio = fastingHours.toFloat() / 24f
    val eatRatio = eatingHours.toFloat() / 24f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF091422))
    ) {
        // Segmento digiuno mattutino
        val morningFast = (fastRatio * 0.75f).coerceIn(0.1f, 0.9f)
        val eveningFast = (fastRatio - morningFast).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .weight(morningFast)
                .fillMaxHeight()
                .background(CoachBlue)
        )
        // Finestra pasto (Verde #1F9D72)
        Box(
            modifier = Modifier
                .weight(eatRatio)
                .fillMaxHeight()
                .background(CoachGreen)
        )
        // Segmento digiuno serale
        if (eveningFast > 0f) {
            Box(
                modifier = Modifier
                    .weight(eveningFast)
                    .fillMaxHeight()
                    .background(CoachBlue)
            )
        }
    }
}

/**
 * 7 pillole giornaliere verdi/blu per Autophagy Booster che alterna i giorni
 */
@Composable
fun AutophagyBoosterPills(modifier: Modifier = Modifier) {
    // Autophagy Booster 7 giorni: Lun..Dom
    // Lun: 16h fast / 8h eat (67% / 33%)
    // Mar: 18h fast / 6h eat (75% / 25%)
    // Mer: 16h fast / 8h eat (67% / 33%)
    // Gio: 22h fast / 2h eat (92% / 8% booster peak)
    // Ven: 16h fast / 8h eat (67% / 33%)
    // Sab: 18h fast / 6h eat (75% / 25%)
    // Dom: 16h fast / 8h eat (67% / 33%)
    val dailyRatios = listOf(
        Pair(16f / 24f, 8f / 24f),
        Pair(18f / 24f, 6f / 24f),
        Pair(16f / 24f, 8f / 24f),
        Pair(22f / 24f, 2f / 24f),
        Pair(16f / 24f, 8f / 24f),
        Pair(18f / 24f, 6f / 24f),
        Pair(16f / 24f, 8f / 24f)
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        dailyRatios.forEach { (fastR, eatR) ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF091422))
            ) {
                Box(
                    modifier = Modifier
                        .weight(fastR)
                        .fillMaxHeight()
                        .background(CoachBlue)
                )
                Box(
                    modifier = Modifier
                        .weight(eatR)
                        .fillMaxHeight()
                        .background(CoachGreen)
                )
            }
        }
    }
}
