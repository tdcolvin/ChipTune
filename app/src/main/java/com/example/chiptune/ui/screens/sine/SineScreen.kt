package com.example.chiptune.ui.screens.sine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SineScreen(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") viewModel: SineViewModel = viewModel(),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Sine Screen",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
