package com.kapi.ledgerroast.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    Ledger("ledger", "记账", Icons.Filled.Wallet),
    Budget("budget", "预算", Icons.Filled.Home),
    Goals("goals", "目标", Icons.Filled.Flag),
    Report("report", "报表", Icons.Filled.Assessment),
    Settings("settings", "设置", Icons.Filled.Settings)
}
