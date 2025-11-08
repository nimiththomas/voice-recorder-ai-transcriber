package com.nimith.echonote.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    object Dashboard : Screen()
}