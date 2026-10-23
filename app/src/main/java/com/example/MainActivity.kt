package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import com.example.ui.screens.AuditFormScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MapViewScreen
import com.example.ui.screens.SavedRecordsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuditViewModel

sealed interface AppScreen {
    data object Home : AppScreen
    data object Form : AppScreen
    data object Records : AppScreen
    data object MapView : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CoolerAuditApp()
                }
            }
        }
    }
}

@Composable
fun CoolerAuditApp(
    viewModel: AuditViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var previousScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    when (currentScreen) {
        is AppScreen.Home -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToWithCoolerForm = {
                    viewModel.initNewForm(AuditFormType.WITH_COOLER)
                    previousScreen = AppScreen.Home
                    currentScreen = AppScreen.Form
                },
                onNavigateToWithoutCoolerForm = {
                    viewModel.initNewForm(AuditFormType.WITHOUT_COOLER)
                    previousScreen = AppScreen.Home
                    currentScreen = AppScreen.Form
                },
                onNavigateToSavedRecords = {
                    previousScreen = AppScreen.Home
                    currentScreen = AppScreen.Records
                },
                onNavigateToMapView = {
                    previousScreen = AppScreen.Home
                    currentScreen = AppScreen.MapView
                },
                onEditRecord = { record ->
                    viewModel.loadRecordForEdit(record)
                    previousScreen = AppScreen.Home
                    currentScreen = AppScreen.Form
                }
            )
        }

        is AppScreen.Form -> {
            AuditFormScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = previousScreen
                }
            )
        }

        is AppScreen.Records -> {
            SavedRecordsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = AppScreen.Home
                },
                onEditRecord = { record ->
                    viewModel.loadRecordForEdit(record)
                    previousScreen = AppScreen.Records
                    currentScreen = AppScreen.Form
                }
            )
        }

        is AppScreen.MapView -> {
            MapViewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = AppScreen.Home
                },
                onEditRecord = { record ->
                    viewModel.loadRecordForEdit(record)
                    previousScreen = AppScreen.MapView
                    currentScreen = AppScreen.Form
                }
            )
        }
    }
}
