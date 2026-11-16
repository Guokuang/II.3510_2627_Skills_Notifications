package com.isep.composeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isep.composeapp.resources.*
import com.isep.composeapp.ui.i18n.*
import com.isep.shared.domain.entities.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListingDetailScreen(listing: Listing?, user: UserAccount?, onEdit: () -> Unit,
    onReserve: () -> Unit, onSold: () -> Unit, onRestore: () -> Unit, onDelete: () -> Unit) {
    if (listing == null) { Text(stringResource(Res.string.error_listing_not_found)); return }
    var confirmDelete by remember(listing.id) { mutableStateOf(false) }
    val language = currentLanguageTag()
    val owner = user?.id == listing.author.id && user.role == UserRole.STUDENT
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(listing.title, style = MaterialTheme.typography.headlineSmall)
        Text(formatPrice(listing.price, language), style = MaterialTheme.typography.titleLarge)
        Text(listing.category.text())
        Text(listing.description)
        Text(stringResource(Res.string.seller, listing.author.name))
        Text(stringResource(Res.string.created, formatDate(listing.creationDate, language)))
        Text(listing.status.text())
        if (listing.status == Available) Button(onReserve) { Text(stringResource(Res.string.action_reserve)) }
        if (owner) {
            if (listing.status !is Sold) OutlinedButton(onEdit) { Text(stringResource(Res.string.action_edit)) }
            if (listing.status is Reserved) {
                Button(onSold) { Text(stringResource(Res.string.action_sold)) }
                OutlinedButton(onRestore) { Text(stringResource(Res.string.action_cancel_reservation)) }
            }
            if (listing.status is Sold) Button(onRestore) { Text(stringResource(Res.string.action_restore)) }
            OutlinedButton({ confirmDelete = true }) { Text(stringResource(Res.string.action_delete)) }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(Res.string.delete_title)) },
        text = { Text(stringResource(Res.string.delete_message, listing.title)) },
        confirmButton = { TextButton({ confirmDelete = false; onDelete() }) { Text(stringResource(Res.string.action_delete)) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) } }
    )
}
