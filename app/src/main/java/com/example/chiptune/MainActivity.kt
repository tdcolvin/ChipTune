package com.example.chiptune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.chiptune.ui.navigation.AppRoute
import com.example.chiptune.ui.screens.sine.SineScreen
import com.example.chiptune.ui.screens.smb.SmbScreen
import com.example.chiptune.ui.screens.tetris.TetrisScreen
import com.example.chiptune.ui.screens.waves.WavesScreen
import com.example.chiptune.ui.theme.ChipTuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChipTuneTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val backStack = rememberNavBackStack(AppRoute.Tetris)
    val currentRoute = backStack.lastOrNull() as? AppRoute ?: AppRoute.Tetris

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopNavBar(
                currentRoute = currentRoute,
                onNavigateTo = { route ->
                    if (currentRoute != route) {
                        backStack.clear()
                        backStack.add(route)
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.size - 1)
                }
            },
            entryProvider = entryProvider {
                entry<AppRoute.Tetris> {
                    TetrisScreen(modifier = Modifier.padding(innerPadding))
                }
                entry<AppRoute.Sine> {
                    SineScreen(modifier = Modifier.padding(innerPadding))
                }
                entry<AppRoute.Smb> {
                    SmbScreen(modifier = Modifier.padding(innerPadding))
                }
                entry<AppRoute.Waves> {
                    WavesScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        )
    }
}

@Composable
fun TopNavBar(
    currentRoute: AppRoute,
    onNavigateTo: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.statusBars
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val routes = listOf(AppRoute.Tetris, AppRoute.Sine, AppRoute.Smb, AppRoute.Waves)
                routes.forEach { route ->
                    val isSelected = currentRoute == route
                    if (isSelected) {
                        Button(
                            onClick = { onNavigateTo(route) }
                        ) {
                            Text(text = route.title)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onNavigateTo(route) }
                        ) {
                            Text(text = route.title)
                        }
                    }
                }
            }
        }
    }
}
