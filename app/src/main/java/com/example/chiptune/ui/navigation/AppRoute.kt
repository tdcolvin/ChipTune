package com.example.chiptune.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    val title: String

    @Serializable
    data object Tetris : AppRoute {
        override val title: String = "Polyphonics"
    }

    @Serializable
    data object Sine : AppRoute {
        override val title: String = "Sine"
    }

    @Serializable
    data object Smb : AppRoute {
        override val title: String = "Melody"
    }

    @Serializable
    data object Monkey : AppRoute {
        override val title: String = "Monkey"
    }

    @Serializable
    data object Waves : AppRoute {
        override val title: String = "Waves"
    }

    @Serializable
    data object Explosion : AppRoute {
        override val title: String = "Percussion"
    }
}
