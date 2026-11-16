package com.isep.shared.domain.repositories

import com.isep.shared.domain.entities.Category

data class ListingInput(
    val title: String,
    val description: String,
    val price: Double,
    val category: Category,
    val photoUrl: String? = null
)