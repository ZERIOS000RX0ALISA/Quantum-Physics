package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ActiveExperimentScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NewProjectScreen
import com.example.ui.screens.ProjectDetailsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val language by viewModel.language.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            // Handle RTL direction when Arabic is selected
            val layoutDirection = if (language == com.example.ui.translation.AppLanguage.AR) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val screenModifier = Modifier.padding(innerPadding)
                        when (currentScreen) {
                            is Screen.Home -> HomeScreen(
                                viewModel = viewModel,
                                lang = language,
                                modifier = screenModifier
                            )
                            is Screen.NewProject -> NewProjectScreen(
                                viewModel = viewModel,
                                lang = language,
                                modifier = screenModifier
                            )
                            is Screen.ActiveExperiment -> ActiveExperimentScreen(
                                viewModel = viewModel,
                                lang = language,
                                modifier = screenModifier
                            )
                            is Screen.ProjectDetails -> ProjectDetailsScreen(
                                viewModel = viewModel,
                                lang = language,
                                modifier = screenModifier
                            )
                            is Screen.Library -> LibraryScreen(
                                viewModel = viewModel,
                                lang = language,
                                modifier = screenModifier
                            )
                            is Screen.Settings -> SettingsScreen(
                                viewModel = viewModel,
                                lang = language,
                                modifier = screenModifier
                            )
                            is Screen.About -> AboutScreen(
                                viewModel = viewModel,
                                lang = language,
                                modifier = screenModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
