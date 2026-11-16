package com.isep.composeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.isep.composeapp.resources.*
import com.isep.composeapp.ui.components.ListingCard
import com.isep.shared.domain.entities.Listing
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyListingsScreen(listings: List<Listing>, onPost: () -> Unit, onOpen: (Int) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Button(onPost) { Text(stringResource(Res.string.post_title)) } }
        if (listings.isEmpty()) item { Text(stringResource(Res.string.my_listings_empty)) }
        items(listings, key = { it.id }) { listing -> ListingCard(listing) { onOpen(listing.id) } }
    }
}
