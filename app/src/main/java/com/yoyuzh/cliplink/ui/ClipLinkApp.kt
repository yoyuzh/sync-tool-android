package com.yoyuzh.cliplink.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.yoyuzh.cliplink.ui.history.HistoryScreen
import com.yoyuzh.cliplink.ui.settings.SettingsScreen
import com.yoyuzh.cliplink.ui.theme.ClipLinkTheme

private enum class AppTab(val label: String, val icon: String) {
    HISTORY("历史记录", "📋"),
    SETTINGS("设置", "⚙")
}

@Composable
fun ClipLinkApp() {
    var selectedTab by remember { mutableStateOf(AppTab.HISTORY) }

    ClipLinkTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            icon = { Text(tab.icon) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            when (selectedTab) {
                AppTab.HISTORY -> HistoryScreen(modifier = Modifier.padding(paddingValues))
                AppTab.SETTINGS -> SettingsScreen(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}
