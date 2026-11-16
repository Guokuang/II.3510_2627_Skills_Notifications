package com.isep.composeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.isep.composeapp.resources.*
import org.jetbrains.compose.resources.stringResource
import com.isep.composeapp.ui.i18n.text
@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val email = rememberSaveable { mutableStateOf("") }
    val password = rememberSaveable { mutableStateOf("") }
    val error = authViewModel.error.collectAsState().value

    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(Res.string.login_title), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text(stringResource(Res.string.login_email)) },
            modifier = Modifier.fillMaxWidth().testTag("login-email"),
            singleLine = true
        )
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(stringResource(Res.string.login_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("login-password"),
            singleLine = true
        )
        if (error != null) {
            Text(error.text(), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { authViewModel.login(email.value, password.value) }) {
            Text(stringResource(Res.string.action_login))
        }
        Text(stringResource(Res.string.login_demo_accounts))
    }
}
