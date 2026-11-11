package com.cibotimer.ui.screens.coaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.data.models.CoachingTip
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.R
import com.cibotimer.ui.theme.*
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachingScreen(
    repository: AppRepository,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coachingTips by repository.coachingTips.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) } // null = Tutti
    var activeTipForReading by remember { mutableStateOf<CoachingTip?>(null) }

    val categories = remember(coachingTips) {
        listOf("Tutti") + coachingTips.map { it.category }.distinct()
    }

    val filteredTips = remember(selectedCategory, coachingTips) {
        if (selectedCategory == null || selectedCategory == "Tutti") {
            coachingTips
        } else {
            coachingTips.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.coaching_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.nav_chat), tint = CiboTimerAzure)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.content_desc_settings), tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = (cat == "Tutti" && selectedCategory == null) || (cat == selectedCategory)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = if (cat == "Tutti") null else cat
                        },
                        label = { Text(if (cat == "Tutti") stringResource(R.string.category_all) else cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CiboTimerAzure,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Article Cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CiboTimerAzureContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToChat() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CiboTimerAzure,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.coaching_ai_card_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CiboTimerAzure
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.coaching_ai_card_desc),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                items(filteredTips, key = { it.id }) { tip ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeTipForReading = tip }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CiboTimerAzureContainer
                                ) {
                                    Text(
                                        text = tip.localizedCategory,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CiboTimerAzure,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${tip.readTimeMinutes} min",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = tip.localizedTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tip.localizedSubtitle,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.coaching_read_full_article),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CiboTimerAzure
                                )
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = CiboTimerAzure,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Article Reader Bottom Sheet
    if (activeTipForReading != null) {
        val tip = activeTipForReading!!
        ModalBottomSheet(
            onDismissRequest = { activeTipForReading = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CiboTimerAzureContainer
                    ) {
                        Text(
                            text = tip.localizedCategory,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CiboTimerAzure,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = { activeTipForReading = null }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_close))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tip.localizedTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tip.localizedSubtitle,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = DividerColor)
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = tip.localizedContentMarkdown,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { activeTipForReading = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CiboTimerAzure),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.coaching_finish_reading), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
