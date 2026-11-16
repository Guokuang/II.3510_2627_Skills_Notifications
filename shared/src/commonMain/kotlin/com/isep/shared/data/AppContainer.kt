package com.isep.shared.data

import com.isep.shared.domain.entities.Category
import com.isep.shared.domain.entities.ListingCatalog
import com.isep.shared.domain.repositories.AuthRepository
import com.isep.shared.domain.repositories.ListingRepository

object AppContainer {
    val authRepository: AuthRepository = InMemoryAuthRepository()
    private val catalog = ListingCatalog().apply {
        val alice = requireNotNull(authRepository.findUserById(1))
        val bob = requireNotNull(authRepository.findUserById(2))
        postListing("Kotlin handbook", "A used book in good condition.", 12.50, Category.BOOKS, alice)
        postListing("Desk lamp", "LED lamp, available on campus.", 8.0, Category.FURNITURE, bob)
        postListing("Sports bag", "A small bag for the gym.", 15.0, Category.SPORTS_LEISURE, alice)
    }
    val listingRepository: ListingRepository = InMemoryListingRepository(authRepository, catalog)
}
