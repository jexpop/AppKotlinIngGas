package com.jexpop.appkotlininggas.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home : Screen("home", Icons.Filled.Home)
    object Transactions : Screen("transactions", Icons.AutoMirrored.Filled.List)
    object Categories : Screen("categories", Icons.Filled.Category)
    object Banks : Screen("banks", Icons.Filled.AccountBalance)
    object Import : Screen("import", Icons.Filled.Upload)
    object Settings : Screen("settings", Icons.Filled.Settings)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Categories,
    Screen.Banks,
    Screen.Import,
    Screen.Settings
)