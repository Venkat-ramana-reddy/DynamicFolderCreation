package com.factoryattendance.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.factoryattendance.ui.screen.attendance.AttendanceScreen
import com.factoryattendance.ui.screen.dashboard.DashboardScreen
import com.factoryattendance.ui.screen.geo.GeoScreen
import com.factoryattendance.ui.screen.report.ReportScreen
import com.factoryattendance.ui.screen.setup.SetupScreen
import com.factoryattendance.ui.screen.who.WhoScreen
import com.factoryattendance.ui.screen.who.WhoViewModel

sealed class Screen(val route: String) {
    object Who        : Screen("who")
    object Dashboard  : Screen("dashboard")
    object Attendance : Screen("attendance")
    object Geo        : Screen("geo")
    object Report     : Screen("report")
    object Setup      : Screen("setup")
}

data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    NavItem(Screen.Dashboard,  "Dashboard",  Icons.Default.Dashboard),
    NavItem(Screen.Attendance, "Attendance", Icons.Default.People),
    NavItem(Screen.Geo,        "Geo-fence",  Icons.Default.LocationOn),
    NavItem(Screen.Report,     "Hours",      Icons.Default.AccessTime),
    NavItem(Screen.Setup,      "Setup",      Icons.Default.Settings)
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val whoVm: WhoViewModel = hiltViewModel()
    val showWho by whoVm.showWhoScreen.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route)  { DashboardScreen() }
            composable(Screen.Attendance.route) { AttendanceScreen() }
            composable(Screen.Geo.route)        { GeoScreen() }
            composable(Screen.Report.route)     { ReportScreen() }
            composable(Screen.Setup.route)      { SetupScreen() }
        }

        // Who-are-you overlay on top of everything
        if (showWho) {
            WhoScreen(viewModel = whoVm)
        }
    }
}
