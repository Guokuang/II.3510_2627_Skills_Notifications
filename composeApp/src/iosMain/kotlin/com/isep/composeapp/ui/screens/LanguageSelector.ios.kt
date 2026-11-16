package com.isep.composeapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.isep.composeapp.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun LanguageSelector() {
    Column {
        Text(stringResource(Res.string.language_title))
        Text(stringResource(Res.string.language_ios_hint))
    }
}
