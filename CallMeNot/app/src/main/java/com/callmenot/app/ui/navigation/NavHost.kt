package com.callmenot.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.callmenot.app.data.local.entity.CallAction
import com.callmenot.app.ui.screens.activity.ActivityScreen
import com.callmenot.app.ui.screens.home.HomeScreen
import com.callmenot.app.ui.screens.onboarding.OnboardingScreen
import com.callmenot.app.ui.screens.onboarding.OnboardingViewModel
import com.callmenot.app.ui.screens.paywall.PaywallScreen
import com.callmenot.app.ui.screens.settings.SettingsScreen
import com.callmenot.app.ui.screens.whitelist.WhitelistScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Whitelist : Screen("whitelist")
    object Activity : Screen("activity")
    object ActivityFiltered : Screen("activity/{filter}")
    object Settings : Screen("settings")
    object Paywall : Screen("paywall")
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Whitelist.route, "Whitelist", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(Screen.Activity.route, "Activity", Icons.Filled.CallReceived, Icons.Outlined.CallReceived),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun CallMeNotNavHost() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsState()
    
    val startDestination = if (isOnboardingComplete) Screen.Home.route else Screen.Onboarding.route
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route } ||
                        currentDestination?.route?.startsWith("activity") == true
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { 
                            it.route == item.route || 
                            (item.route == Screen.Activity.route && it.route?.startsWith("activity") == true)
                        } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    },
                    onNavigateToBlockedCalls = {
                        navController.navigate("activity/BLOCKED")
                    },
                    onNavigateToAllowedCalls = {
                        navController.navigate("activity/ALLOWED")
                    },
                    onNavigateToWhitelist = {
                        navController.navigate(Screen.Whitelist.route)
                    }
                )
            }
            
            composable(Screen.Whitelist.route) {
                WhitelistScreen()
            }
            
            composable(Screen.Activity.route) {
                ActivityScreen()
            }
            
            composable(
                route = "activity/{filter}",
                arguments = listOf(navArgument("filter") { type = NavType.StringType })
            ) { backStackEntry ->
                val filterArg = backStackEntry.arguments?.getString("filter")
                val filter = when (filterArg) {
                    "BLOCKED" -> CallAction.BLOCKED
                    "ALLOWED" -> CallAction.ALLOWED
                    else -> null
                }
                ActivityScreen(initialFilter = filter)
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    }
                )
            }
            
            composable(Screen.Paywall.route) {
                PaywallScreen(
                    onDismiss = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
