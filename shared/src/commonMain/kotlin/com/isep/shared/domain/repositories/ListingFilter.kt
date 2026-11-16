package com.isep.shared.domain.repositories

import com.isep.shared.domain.entities.Category

data class ListingFilter(
    val keyword: String = "",
    val category: Category? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null
)