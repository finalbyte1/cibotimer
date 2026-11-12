package com.cibotimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.data.models.UserProfile
import com.cibotimer.update.UpdateManager
import com.cibotimer.ui.screens.chat.AiChatScreen
import com.cibotimer.ui.screens.coaching.CoachingScreen
import com.cibotimer.ui.screens.notifications.NotificationsScreen
import com.cibotimer.ui.screens.planner.PlannerScreen
import com.cibotimer.ui.screens.plans.PlanSelectionStartScreen
import com.cibotimer.ui.screens.progress.ProgressScreen
import com.cibotimer.ui.screens.settings.SettingsScreen
import com.cibotimer.ui.screens.timer.TimerScreen
import com.cibotimer.ui.screens.water.WaterTrackerScreen
import com.cibotimer.ui.theme.*
import com.cibotimer.R

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val titleRes: Int) {
    data object Timer : Screen("timer", "Digiuno", Icons.Default.Timer, R.string.nav_timer)
    data object Progress : Screen("progress", "Io", Icons.Default.Person, R.string.nav_progress)
    data object Coach : Screen("coach", "Coach", Icons.Default.Favorite, R.string.nav_coach)
    data object Notifications : Screen("notifications", "Notifiche", Icons.Default.Notifications, R.string.nav_notifications)
    data object Planner : Screen("planner", "Piani", Icons.Default.CalendarMonth, R.string.nav_planner)
    data object Settings : Screen("settings", "Altro", Icons.Default.MoreHoriz, R.string.nav_settings)

    // Secondary / Deep screens
    data object PlanSelection : Screen("plan_selection", "Programmi", Icons.AutoMirrored.Filled.FormatListBulleted, R.string.nav_plan_selection)
    data object Chat : Screen("chat", "Chat AI", Icons.Default.AutoAwesome, R.string.nav_chat)
    data object Water : Screen("water", "Acqua", Icons.Default.WaterDrop, R.string.nav_water)
}

val bottomNavItems = listOf(
    Screen.Timer,
    Screen.Progress,
    Screen.Coach,
    Screen.Notifications,
    Screen.Settings
)

@Composable
fun CiboTimerNavigation(
    repository: AppRepository,
    updateManager: UpdateManager? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val userProfile by repository.userProfile.collectAsState()

    // Everything starts from plan selection if user hasn't selected a plan yet!
    val startDestination = if (userProfile.hasSelectedInitialPlan) Screen.Timer.route else Screen.PlanSelection.route

    val showNav = currentRoute != Screen.PlanSelection.route

    val configuration = LocalConfiguration.current
    val isTabletOrExpanded = configuration.screenWidthDp >= 600

    if (isTabletOrExpanded) {
        // Layout Tablet (>= 600dp): NavigationRail laterale a sinistra
        Scaffold(
            containerColor = CiboBg,
            modifier = modifier
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(CiboBg)
            ) {
                if (showNav) {
                    NavigationRail(
                        containerColor = CiboBg,
                        contentColor = CiboTextSecondary,
                        header = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp, bottom = 12.dp)
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CiboCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🍽️", fontSize = 20.sp)
                            }
                        },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val labelText = androidx.compose.ui.res.stringResource(screen.titleRes)
                            NavigationRailItem(
                                alwaysShowLabel = true,
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = labelText,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = labelText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CiboAccent,
                                    selectedTextColor = CiboAccent,
                                    unselectedIconColor = CiboTextSecondary,
                                    unselectedTextColor = CiboTextSecondary,
                                    indicatorColor = CiboAccent.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CiboTimerNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        repository = repository,
                        updateManager = updateManager,
                        userProfile = userProfile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    } else {
        // Layout Telefono (< 600dp): identico a oggi, nessuna modifica
        Scaffold(
            bottomBar = {
                if (showNav) {
                    NavigationBar(
                        containerColor = CiboBg,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val labelText = androidx.compose.ui.res.stringResource(screen.titleRes)
                            NavigationBarItem(
                                alwaysShowLabel = true,
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = labelText,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = labelText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CiboAccent,
                                    selectedTextColor = CiboAccent,
                                    unselectedIconColor = CiboTextSecondary,
                                    unselectedTextColor = CiboTextSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            CiboTimerNavHost(
                navController = navController,
                startDestination = startDestination,
                repository = repository,
                updateManager = updateManager,
                userProfile = userProfile,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CiboTimerNavHost(
    navController: NavHostController,
    startDestination: String,
    repository: AppRepository,
    updateManager: UpdateManager?,
    userProfile: UserProfile,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.PlanSelection.route) {
            PlanSelectionStartScreen(
                repository = repository,
                onPlanChosen = {
                    navController.navigate(Screen.Timer.route) {
                        popUpTo(Screen.PlanSelection.route) { inclusive = true }
                    }
                },
                onBack = if (userProfile.hasSelectedInitialPlan) {
                    { navController.popBackStack() }
                } else null
            )
        }

        composable(Screen.Timer.route) {
            TimerScreen(
                repository = repository,
                updateManager = updateManager,
                onNavigateToPlanSelection = { navController.navigate(Screen.PlanSelection.route) },
                onNavigateToWater = { navController.navigate(Screen.Water.route) },
                onNavigateToCoaching = { navController.navigate(Screen.Coach.route) },
                onNavigateToProgress = { navController.navigate(Screen.Progress.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Planner.route) {
            PlannerScreen(
                repository = repository
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                repository = repository
            )
        }

        composable(Screen.Progress.route) {
            ProgressScreen(
                repository = repository
            )
        }

        composable(Screen.Coach.route) {
            CoachingScreen(
                repository = repository,
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Chat.route) {
            AiChatScreen(
                repository = repository,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Water.route) {
            WaterTrackerScreen(
                repository = repository
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                repository = repository,
                updateManager = updateManager
            )
        }
    }
}

@Deprecated("Usa CiboTimerNavigation", ReplaceWith("CiboTimerNavigation(repository, updateManager, modifier)"))
@Composable
fun BodyFastNavigation(
    repository: AppRepository,
    updateManager: UpdateManager? = null,
    modifier: Modifier = Modifier
) = CiboTimerNavigation(repository, updateManager, modifier)

