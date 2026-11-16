package com.isep.composeapp.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.isep.composeapp.resources.*
import com.isep.composeapp.ui.i18n.AppLanguage
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun LanguageSelector() {
    // Reading Configuration makes the selection update after an Activity recreation.
    LocalConfiguration.current
    val selected = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    Column {
        Text(stringResource(Res.string.language_title))
        Row {
            AppLanguage.entries.forEach { language ->
                val label = when (language) {
                    AppLanguage.SYSTEM -> Res.string.language_system
                    AppLanguage.ENGLISH -> Res.string.language_english
                    AppLanguage.FRENCH -> Res.string.language_french
                }
                FilterChip(selected = selected == language.tag,
                    onClick = { language.apply() },
                    label = { Text(stringResource(label)) })
            }
        }
    }
}
