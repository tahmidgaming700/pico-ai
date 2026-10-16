package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppThemeMode
import com.example.ui.MainViewModel
import com.example.ui.components.AboutPrivacyDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ModelManagerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PicoAiTheme
import com.example.ui.theme.PicoCyan
import com.example.ui.theme.PicoNavyBorder
import com.example.ui.theme.PicoNavyCard
import com.example.ui.theme.PicoNavyDarkest
import com.example.ui.theme.PicoNavySurface
import com.example.ui.theme.PicoTextMuted
import com.example.ui.theme.PicoTextPrimary

enum class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CHAT("chat", "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    MODELS("models", "Models", Icons.Filled.Storage, Icons.Outlined.Storage),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val themeMode by mainViewModel.themeMode.collectAsState()

            val isDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            PicoAiTheme(darkTheme = isDark) {
                PicoAiApp(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun PicoAiApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.CHAT) }
    var showAboutPrivacy by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val uiNotice by viewModel.uiNotice.collectAsState()

    // Handle notices
    LaunchedEffect(uiNotice) {
        uiNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    // BackHandler: if on any other screen, Back takes user to Chat
    BackHandler(enabled = currentScreen != Screen.CHAT) {
        currentScreen = Screen.CHAT
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = PicoNavySurface,
                modifier = Modifier
                    .background(PicoNavyBorder)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PicoNavyDarkest,
                            selectedTextColor = PicoCyan,
                            indicatorColor = PicoCyan,
                            unselectedIconColor = PicoTextMuted,
                            unselectedTextColor = PicoTextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.CHAT -> {
                    ChatScreen(
                        viewModel = viewModel,
                        onNavigateToModels = { currentScreen = Screen.MODELS }
                    )
                }
                Screen.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onSelectConversation = { convId ->
                            viewModel.selectConversation(convId)
                            currentScreen = Screen.CHAT
                        }
                    )
                }
                Screen.MODELS -> {
                    ModelManagerScreen(viewModel = viewModel)
                }
                Screen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onShowAboutPrivacy = { showAboutPrivacy = true }
                    )
                }
            }
        }
    }

    if (showAboutPrivacy) {
        AboutPrivacyDialog(onDismiss = { showAboutPrivacy = false })
    }
}
