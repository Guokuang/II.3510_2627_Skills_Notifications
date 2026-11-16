package com.isep.shared.domain.entities

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ListingCatalog {
    private val listings = mutableListOf<Listing>()
    private var nextId = 1

    fun allListings(): List<Listing> {
        return listings.toList()
    }

    fun findById(id: Int): Listing? {
        return listings.firstOrNull { it.id == id }
    }

    fun postListing(
        title: String,
        description: String,
        price: Double,
        category: Category,
        author: UserAccount,
        photoUrl: String? = null
    ): Listing {
        require(title.isNotBlank()) { "Title cannot be empty" }
        require(price.isFinite() && price > 0) {
            "Price must be finite and positive"
        }

        val listing = Listing(
            id = nextId,
            title = title.trim(),
            description = description,
            price = price,
            category = category,
            photoUrl = photoUrl,
            author = author,
            status = Available,
            creationDate = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        )

        listings.add(listing)
        nextId++
        return listing
    }

    fun searchListings(
        keyword: String? = null,
        category: Category? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null
    ): List<Listing> {
        require(
            minPrice == null ||
                    (minPrice.isFinite() && minPrice >= 0)
        ) { "Minimum price must be finite and nonnegative" }

        require(
            maxPrice == null ||
                    (maxPrice.isFinite() && maxPrice >= 0)
        ) { "Maximum price must be finite and nonnegative" }

        require(
            minPrice == null ||
                    maxPrice == null ||
                    minPrice <= maxPrice
        ) { "Minimum price cannot exceed maximum price" }

        val query = keyword?.trim().orEmpty()

        return listings.filter { listing ->
            val matchesKeyword =
                query.isEmpty() ||
                        listing.title.contains(query, ignoreCase = true) ||
                        listing.description.contains(query, ignoreCase = true)

            val matchesCategory =
                category == null || listing.category == category

            val matchesMinPrice =
                minPrice == null || listing.price >= minPrice

            val matchesMaxPrice =
                maxPrice == null || listing.price <= maxPrice

            listing.status == Available &&
                    matchesKeyword &&
                    matchesCategory &&
                    matchesMinPrice &&
                    matchesMaxPrice
        }
    }

    fun updateListing(
        id: Int,
        title: String,
        description: String,
        price: Double,
        category: Category,
        photoUrl: String?
    ): Listing? {
        require(title.isNotBlank()) { "Title cannot be empty" }
        require(price.isFinite() && price > 0) {
            "Price must be finite and positive"
        }

        for (i in listings.indices) {
            if (listings[i].id == id) {
                val updated = listings[i].copy(
                    title = title.trim(),
                    description = description,
                    price = price,
                    category = category,
                    photoUrl = photoUrl
                )
                listings[i] = updated
                return updated
            }
        }

        return null
    }

    fun changeStatus(id: Int, newStatus: ListingStatus): Listing? {
        for (i in listings.indices) {
            if (listings[i].id == id) {
                val allowed = when (listings[i].status) {
                    Available -> newStatus is Reserved
                    is Reserved -> newStatus is Sold || newStatus == Available
                    is Sold -> newStatus == Available
                }

                if (!allowed) return null

                val updated = listings[i].copy(status = newStatus)
                listings[i] = updated
                return updated
            }
        }

        return null
    }

    fun removeListing(id: Int): Boolean {
        for (i in listings.indices) {
            if (listings[i].id == id) {
                listings.removeAt(i)
                return true
            }
        }

        return false
    }
}
