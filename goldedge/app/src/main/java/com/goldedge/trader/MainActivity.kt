package com.goldedge.trader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            GoldEdgeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    GoldEdgeRoot()
                }
            }
        }
    }
}

internal enum class AppTab(val label: String, val icon: String) {
    DASHBOARD("الرئيسية", "⌂"),
    NEWS("الأخبار", "◷"),
    CHECKLIST("فلتر الصفقة", "✓"),
    RISK("المخاطرة", "%"),
    JOURNAL("السجل", "▤")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldEdgeRoot(vm: GoldEdgeViewModel = viewModel()) {
    var selected by remember { mutableStateOf(AppTab.DASHBOARD) }
    var settingsOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.error) {
        vm.error?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = AppBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
                title = {
                    Column {
                        Text("GoldEdge", fontWeight = FontWeight.ExtraBold, color = AccentGold, fontSize = 20.sp)
                        Text("مرصد قرار تداول الذهب", color = MutedText, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    Text("◎", modifier = Modifier.padding(start = 16.dp, end = 12.dp), color = AccentGold, fontSize = 28.sp)
                },
                actions = {
                    if (vm.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = AccentGold)
                    } else {
                        TextButton(onClick = vm::refresh) { Text("↻", fontSize = 24.sp, color = AccentGold) }
                    }
                    TextButton(onClick = { settingsOpen = true }) { Text("⚙", fontSize = 20.sp, color = MutedText) }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceOne) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Text(tab.icon, fontSize = 18.sp) },
                        label = { Text(tab.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(selected, label = "goldedge-tabs") { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(vm)
                    AppTab.NEWS -> NewsScreen(vm)
                    AppTab.CHECKLIST -> ChecklistScreen(vm)
                    AppTab.RISK -> RiskScreen(vm)
                    AppTab.JOURNAL -> JournalScreen(vm)
                }
            }
        }
    }

    if (settingsOpen) SettingsDialog(vm) { settingsOpen = false }
}
