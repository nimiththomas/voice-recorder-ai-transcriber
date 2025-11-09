package com.nimith.echonote.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    object Dashboard : Screen()

    @Serializable
    data class Recording(val recordingId: String? = null) : Screen()
}
