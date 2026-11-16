package com.isep.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isep.composeapp.ui.i18n.*
import com.isep.shared.domain.entities.Listing

@Composable
fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(listing.title, style = MaterialTheme.typography.titleMedium)
            Text(formatPrice(listing.price, currentLanguageTag()), style = MaterialTheme.typography.titleLarge)
            Text(listing.category.text())
            Text(listing.status.text(), style = MaterialTheme.typography.labelMedium)
        }
    }
}
