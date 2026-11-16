package com.isep.composeapp

import com.isep.composeapp.resources.*
import org.jetbrains.compose.resources.stringResource
import com.isep.composeapp.ui.i18n.currentLanguageTag
import com.isep.composeapp.ui.i18n.formatPrice
import com.isep.composeapp.ui.i18n.formatDate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.isep.shared.domain.entities.Category
import com.isep.composeapp.ui.i18n.text

@Composable
fun PostListingScreen(
    onSubmit: (String, String, String, Category, String) -> Unit,
    initialTitle: String = "",
    initialDescription: String = "",
    initialPrice: String = "",
    initialCategory: Category = Category.BOOKS,
    initialPhotoUrl: String = "",
    isEditing: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val title = remember(initialTitle) { mutableStateOf(initialTitle) }
    val description = remember(initialDescription) { mutableStateOf(initialDescription) }
    val price = remember(initialPrice) { mutableStateOf(initialPrice) }
    val category = remember(initialCategory) { mutableStateOf(initialCategory) }
    val photoUrl = remember(initialPhotoUrl) { mutableStateOf(initialPhotoUrl) }
    val emptyTitleMessage = stringResource(Res.string.error_empty_title)
    val invalidPriceMessage = stringResource(Res.string.error_invalid_price)
    val errorMessage = remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (isEditing) stringResource(Res.string.edit_title) else stringResource(Res.string.post_title),
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = title.value,
            onValueChange = { title.value = it },
            label = { Text(stringResource(Res.string.field_title)) },
            modifier = Modifier.fillMaxWidth().testTag("listing-title"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboard?.hide()
            })
        )
        OutlinedTextField(
            value = description.value,
            onValueChange = { description.value = it },
            label = { Text(stringResource(Res.string.field_description)) },
            modifier = Modifier.fillMaxWidth().testTag("listing-description"),
            minLines = 3
        )
        OutlinedTextField(
            value = price.value,
            onValueChange = { price.value = it },
            label = { Text(stringResource(Res.string.field_price)) },
            modifier = Modifier.fillMaxWidth().testTag("listing-price"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboard?.hide()
            })
        )

        Text(stringResource(Res.string.field_category))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.entries.forEach { item ->
                FilterChip(
                    selected = category.value == item,
                    onClick = { category.value = item },
                    label = { Text(item.text()) }
                )
            }
        }

        OutlinedTextField(
            value = photoUrl.value,
            onValueChange = { photoUrl.value = it },
            label = { Text(stringResource(Res.string.field_photo)) },
            modifier = Modifier.fillMaxWidth().testTag("listing-photo"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboard?.hide()
            })
        )
        errorMessage.value?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                val parsedPrice = price.value.trim()
                    .replace(',', '.')
                    .toDoubleOrNull()

                when {
                    title.value.isBlank() ->
                        errorMessage.value = emptyTitleMessage

                    parsedPrice == null || !parsedPrice.isFinite() || parsedPrice <= 0.0 ->
                        errorMessage.value = invalidPriceMessage

                    else -> {
                        errorMessage.value = null
                        onSubmit(
                            title.value.trim(),
                            description.value,
                            parsedPrice.toString(),
                            category.value,
                            photoUrl.value.trim()
                        )
                    }
                }
            }
        ) {
            Text(if (isEditing) stringResource(Res.string.action_save) else stringResource(Res.string.action_post))
        }

    }
}
