package com.example.chiptune.ui.screens.service

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.chiptune.TetrisSong
import com.example.chiptune.service.TetrisAudioService
import java.util.Locale

@Composable
fun ServiceScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isRunning by TetrisAudioService.isRunning.collectAsState()
    val isPlaying by TetrisAudioService.isPlaying.collectAsState()
    val currentPositionSec by TetrisAudioService.currentPositionSeconds.collectAsState()
    val scrollState = rememberScrollState()

    val maxDurationSec = TetrisSong.LOOP_DURATION_SECONDS.toFloat()
    val currentPosFloat = (currentPositionSec % TetrisSong.LOOP_DURATION_SECONDS).toFloat().coerceIn(0f, maxDurationSec)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            TetrisAudioService.start(context)
        }
    }

    fun handleShowNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                TetrisAudioService.start(context)
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            TetrisAudioService.start(context)
        }
    }

    fun handleHideNotification() {
        TetrisAudioService.stop(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Foreground Service Playback",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isRunning) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (isRunning) Color(0xFF00E676) else Color.Gray,
                            shape = CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isRunning) {
                            if (isPlaying) "Service Running (Playing)" else "Service Running (Paused)"
                        } else {
                            "Service Stopped"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (isRunning) {
                            "Tetris theme foreground player service is active."
                        } else {
                            "Tap 'Show Notification' to start background player notification."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Main Show / Hide Service Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { handleShowNotification() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Show Notification")
            }

            OutlinedButton(
                onClick = { handleHideNotification() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Hide Notification")
            }
        }

        // Media Player Controls Card
        if (isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Player Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )

                    // Position Slider / Jump to position
                    Column {
                        Slider(
                            value = currentPosFloat,
                            onValueChange = { newValue ->
                                TetrisAudioService.seekTo(context, newValue.toDouble())
                            },
                            valueRange = 0f..maxDurationSec,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatTime(currentPosFloat.toDouble()),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                text = formatTime(maxDurationSec.toDouble()),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    // Play / Pause control button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    TetrisAudioService.pause(context)
                                } else {
                                    TetrisAudioService.play(context)
                                }
                            },
                        ) {
                            Text(if (isPlaying) "Pause" else "Play")
                        }
                    }
                }
            }
        }

        // Technical Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Android Player Notification Capabilities",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "• Notification MediaStyle & MediaSession integration.\n" +
                            "• Live Play / Pause toggling and seek timeline slider.\n" +
                            "• Full compatibility with Android lock screen media controls, quick settings widget, and Bluetooth media keys.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
