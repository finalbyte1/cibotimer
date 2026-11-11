package com.cibotimer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.cibotimer.data.repository.AppRepository
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
    data object PlanSelection : Screen("plan_selection", "Programmi", Icons.Default.FormatListBulleted, R.string.nav_plan_selection)
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

    val showBottomBar = currentRoute != Screen.PlanSelection.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = CiboBg,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(screen.titleRes),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
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
}

@Deprecated("Usa CiboTimerNavigation", ReplaceWith("CiboTimerNavigation(repository, updateManager, modifier)"))
@Composable
fun BodyFastNavigation(
    repository: AppRepository,
    updateManager: UpdateManager? = null,
    modifier: Modifier = Modifier
) = CiboTimerNavigation(repository, updateManager, modifier)

