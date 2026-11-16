package com.isep.composeapp

import com.isep.composeapp.resources.*
import org.jetbrains.compose.resources.stringResource
import com.isep.composeapp.ui.i18n.currentLanguageTag
import com.isep.composeapp.ui.i18n.formatPrice
import com.isep.composeapp.ui.i18n.formatDate
import com.isep.composeapp.ui.i18n.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.isep.composeapp.ui.AuthViewModel
import com.isep.composeapp.ui.LoginScreen
import com.isep.shared.domain.entities.UserAccount
import com.isep.shared.domain.entities.AppNotification

@Composable
fun ProfileScreen(currentUser: UserAccount?, authViewModel: AuthViewModel, notifications: List<AppNotification>) {
    if (currentUser == null) {
        LoginScreen(authViewModel)
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.profile_name, currentUser.name))
        Text(stringResource(Res.string.profile_email, currentUser.email))
        Text(stringResource(Res.string.profile_role, currentUser.role.text()))
        Text(stringResource(Res.string.member_since, formatDate(currentUser.creationDate, currentLanguageTag())))
        com.isep.composeapp.ui.screens.LanguageSelector()
        Text(stringResource(Res.string.alerts_title), style = MaterialTheme.typography.titleMedium)
        if (notifications.isEmpty()) {
            Text(stringResource(Res.string.alerts_empty))
        } else {
            notifications.asReversed().forEach { notification ->
                Text(notification.message)
            }
        }
        Button(onClick = authViewModel::logout) {
            Text(stringResource(Res.string.action_logout))
        }
    }
}
