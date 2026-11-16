package com.isep.composeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun SwapCampusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = platformColorScheme(isSystemInDarkTheme()),
        content = content
    )
}
