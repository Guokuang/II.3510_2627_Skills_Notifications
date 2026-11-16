package com.isep.composeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isep.composeapp.resources.*
import com.isep.composeapp.ui.components.*
import com.isep.composeapp.ui.i18n.messageRes
import com.isep.shared.domain.entities.Category
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListingsScreen(uiState: ListingsUiState, onFilterChange: (String, Category?) -> Unit,
                   onOpen: (Int) -> Unit, onRetry: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedName?.let(Category::valueOf)
    LaunchedEffect(query, selected) { onFilterChange(query, selected) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, { query = it }, label = { Text(stringResource(Res.string.search_label)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                keyboard?.hide()
            }))
        CategoryChips(selected, includeAll = true) { selectedName = it?.name }
        when (uiState) {
            ListingsUiState.Loading -> LoadingView()
            is ListingsUiState.Error -> {
                Text(stringResource(uiState.error.messageRes()), color = MaterialTheme.colorScheme.error)
                Button(onRetry) { Text(stringResource(Res.string.action_retry)) }
            }
            is ListingsUiState.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.listings.isEmpty()) item { Text(stringResource(Res.string.listings_empty)) }
                items(uiState.listings, key = { it.id }) { listing ->
                    ListingCard(listing) { onOpen(listing.id) }
                }
            }
        }
    }
}
