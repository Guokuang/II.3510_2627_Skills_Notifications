package com.isep.shared.data

import com.isep.shared.domain.entities.*
import com.isep.shared.domain.repositories.*
import kotlin.test.*

class BuyerReservationTest {
    private val auth = InMemoryAuthRepository()
    private val repository = InMemoryListingRepository(auth)
    private fun create(): Listing {
        auth.login("alice@swapcampus.test", "alice123")
        return assertIs<AppResult.Success<Listing>>(repository.createListing(
            ListingInput("Book", "Used", 12.5, Category.BOOKS))).data
    }

    @Test fun guestCannotReserve() {
        val listing = create()
        auth.logout()
        assertEquals(AppResult.Failure(AppError.LOGIN_REQUIRED), repository.requestReservation(listing.id))
        assertEquals(Available, repository.getListing(listing.id)?.status)
    }

    @Test fun ownerCannotReserveOwnListing() {
        val listing = create()
        assertEquals(AppResult.Failure(AppError.INVALID_BUYER), repository.requestReservation(listing.id))
        assertEquals(Available, repository.getListing(listing.id)?.status)
    }

    @Test fun buyerCanReserveAndOwnerCanCancelThenSell() {
        val listing = create()
        auth.login("bob@swapcampus.test", "bob123")
        val reserved = assertIs<AppResult.Success<Listing>>(repository.requestReservation(listing.id)).data
        assertEquals(Reserved("2"), reserved.status)
        assertEquals(AppResult.Failure(AppError.INVALID_STATUS), repository.requestReservation(listing.id))
        assertEquals(AppResult.Failure(AppError.PERMISSION_DENIED), repository.markListingSold(listing.id))
        assertEquals(AppResult.Failure(AppError.PERMISSION_DENIED), repository.restoreListing(listing.id))
        auth.login("alice@swapcampus.test", "alice123")
        assertIs<AppResult.Success<Listing>>(repository.restoreListing(listing.id))
        assertEquals(Available, repository.getListing(listing.id)?.status)
        auth.login("bob@swapcampus.test", "bob123")
        assertIs<AppResult.Success<Listing>>(repository.requestReservation(listing.id))
        auth.login("alice@swapcampus.test", "alice123")
        assertIs<AppResult.Success<Listing>>(repository.markListingSold(listing.id))
        assertIs<Sold>(repository.getListing(listing.id)?.status)
        assertIs<AppResult.Success<Listing>>(repository.restoreListing(listing.id))
        assertEquals(Available, repository.getListing(listing.id)?.status)
    }

    @Test fun administratorCannotReserveAndMissingListingIsReported() {
        val listing = create()
        auth.login("admin@swapcampus.test", "admin123")
        assertEquals(AppResult.Failure(AppError.PERMISSION_DENIED), repository.requestReservation(listing.id))
        auth.login("bob@swapcampus.test", "bob123")
        assertEquals(AppResult.Failure(AppError.LISTING_NOT_FOUND), repository.requestReservation(-1))
    }
}
