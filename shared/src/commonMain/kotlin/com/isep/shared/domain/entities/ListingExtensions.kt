package com.isep.shared.domain.entities

fun Listing.describePhoto(): String {
    return photoUrl?.let { "Photo available: $it" } ?: "No photo"
}