package com.jexpop.appkotlininggas.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.ui.screens.importcsv.ImportScreen
import androidx.compose.ui.unit.sp

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val screenLabels = mapOf(
        Screen.Home.route to stringResource(R.string.nav_home),
        Screen.Transactions.route to stringResource(R.string.nav_transactions),
        Screen.Categories.route to stringResource(R.string.nav_categories),
        Screen.Banks.route to stringResource(R.string.nav_banks),
        Screen.Import.route to stringResource(R.string.nav_import),
        Screen.Settings.route to stringResource(R.string.nav_settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screenLabels[screen.route]) },
                        label = {
                            Text(
                                text = screenLabels[screen.route] ?: "",
                                fontSize = 8.sp
                            )
                        },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                Text(stringResource(R.string.placeholder_home))
            }
            composable(Screen.Transactions.route) {
                Text(stringResource(R.string.placeholder_transactions))
            }
            composable(Screen.Categories.route) {
                Text(stringResource(R.string.placeholder_categories))
            }
            composable(Screen.Banks.route) {
                Text(stringResource(R.string.placeholder_banks))
            }
            composable(Screen.Import.route) {
                ImportScreen()
            }
            composable(Screen.Settings.route) {
                Text(stringResource(R.string.placeholder_settings))
            }
        }
    }
}