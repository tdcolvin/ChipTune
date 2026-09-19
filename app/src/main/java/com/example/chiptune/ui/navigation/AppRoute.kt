package com.example.chiptune.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    val title: String

    @Serializable
    data object Tetris : AppRoute {
        override val title: String = "Tetris"
    }

    @Serializable
    data object Sine : AppRoute {
        override val title: String = "Sine"
    }
}
