package com.isep.shared.data

import com.isep.shared.domain.entities.Available
import com.isep.shared.domain.entities.Category
import com.isep.shared.domain.entities.Listing
import com.isep.shared.domain.repositories.AppError
import com.isep.shared.domain.repositories.AppResult
import com.isep.shared.domain.repositories.ListingFilter
import com.isep.shared.domain.repositories.ListingInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.isep.shared.domain.entities.Reserved
import com.isep.shared.domain.entities.Sold
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.isep.shared.domain.entities.ListingTarget
import com.isep.shared.domain.entities.ModerationAction
import com.isep.shared.domain.entities.ModerationActionType

class InMemoryListingRepositoryTest {

    private fun input(
        title: String = "Kotlin textbook",
        price: Double = 15.0,
        category: Category = Category.BOOKS
    ) = ListingInput(
        title = title,
        description = "Used textbook in good condition",
        price = price,
        category = category
    )

    @Test
    fun guestCannotCreateListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)

        val result = repository.createListing(input())

        assertEquals(
            AppResult.Failure(AppError.LOGIN_REQUIRED),
            result
        )
        assertTrue(repository.listings.value.isEmpty())
    }

    @Test
    fun studentCanCreateAndReadListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val result = repository.createListing(
            input(title = "  Kotlin textbook  ")
        )
        val listing = assertIs<AppResult.Success<Listing>>(result).data

        assertEquals("Kotlin textbook", listing.title)
        assertEquals(1, listing.author.id)
        assertEquals(Available, listing.status)
        assertEquals(listing, repository.getListing(listing.id))
        assertEquals(listOf(listing), repository.listings.value)
    }

    @Test
    fun invalidInputDoesNotCreateListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        assertEquals(
            AppResult.Failure(AppError.EMPTY_TITLE),
            repository.createListing(input(title = "   "))
        )

        for (price in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(
                AppResult.Failure(AppError.INVALID_PRICE),
                repository.createListing(input(price = price))
            )
        }

        assertTrue(repository.listings.value.isEmpty())
    }

    @Test
    fun administratorCannotCreateListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("admin@swapcampus.test", "admin123")

        assertEquals(
            AppResult.Failure(AppError.PERMISSION_DENIED),
            repository.createListing(input())
        )
        assertTrue(repository.listings.value.isEmpty())
    }

    @Test
    fun searchCombinesFiltersAndIncludesPriceBoundaries() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        repository.createListing(input())
        repository.createListing(
            input(title = "Kotlin advanced book", price = 30.0)
        )
        repository.createListing(
            input(
                title = "Kotlin tablet",
                price = 15.0,
                category = Category.ELECTRONICS
            )
        )

        auth.logout()

        val result = repository.searchListings(
            ListingFilter(
                keyword = "  KOTLIN  ",
                category = Category.BOOKS,
                minPrice = 15.0,
                maxPrice = 15.0
            )
        )
        val matches =
            assertIs<AppResult.Success<List<Listing>>>(result).data

        assertEquals(1, matches.size)
        assertEquals("Kotlin textbook", matches.single().title)
    }

    @Test
    fun invalidPriceRangeReturnsFailure() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)

        assertEquals(
            AppResult.Failure(AppError.INVALID_PRICE_RANGE),
            repository.searchListings(
                ListingFilter(minPrice = 20.0, maxPrice = 10.0)
            )
        )
    }

    @Test
    fun myListingsBelongOnlyToCurrentStudent() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)

        auth.login("alice@swapcampus.test", "alice123")
        repository.createListing(input(title = "Alice book"))

        auth.login("bob@swapcampus.test", "bob123")
        repository.createListing(input(title = "Bob book"))

        val result = repository.getMyListings()
        val mine =
            assertIs<AppResult.Success<List<Listing>>>(result).data

        assertEquals(1, mine.size)
        assertEquals("Bob book", mine.single().title)
        assertEquals(2, mine.single().author.id)
    }

    @Test
    fun missingListingReturnsNull() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)

        assertNull(repository.getListing(999))
    }
    @Test
    fun ownerCanEditListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        val updated = assertIs<AppResult.Success<Listing>>(
            repository.updateListing(
                created.id,
                input(title = "  Updated textbook  ", price = 20.0)
            )
        ).data

        assertEquals("Updated textbook", updated.title)
        assertEquals(20.0, updated.price)
        assertEquals(created.id, updated.id)
        assertEquals(created.author, updated.author)
        assertEquals(created.creationDate, updated.creationDate)
        assertEquals(created.status, updated.status)
        assertEquals(updated, repository.getListing(created.id))
        assertEquals(listOf(updated), repository.listings.value)
    }

    @Test
    fun otherStudentCannotEditListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        auth.login("bob@swapcampus.test", "bob123")

        val result = repository.updateListing(
            created.id,
            input(title = "Changed by Bob")
        )

        assertEquals(
            AppResult.Failure(AppError.PERMISSION_DENIED),
            result
        )
        assertEquals(created, repository.getListing(created.id))
        assertEquals(listOf(created), repository.listings.value)
    }

    @Test
    fun invalidEditPreservesOriginalListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        assertEquals(
            AppResult.Failure(AppError.EMPTY_TITLE),
            repository.updateListing(
                created.id,
                input(title = "   ")
            )
        )

        assertEquals(
            AppResult.Failure(AppError.INVALID_PRICE),
            repository.updateListing(
                created.id,
                input(price = -5.0)
            )
        )

        assertEquals(created, repository.getListing(created.id))
        assertEquals(listOf(created), repository.listings.value)
    }
    @Test
    fun ownerCanReserveListingForAnotherStudent() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        val reserved = assertIs<AppResult.Success<Listing>>(
            repository.reserveListing(created.id, buyerId = 2)
        ).data

        assertEquals(Reserved(buyer = "2"), reserved.status)
        assertEquals(created.id, reserved.id)
        assertEquals(created.author, reserved.author)
        assertEquals(reserved, repository.getListing(created.id))
        assertEquals(listOf(reserved), repository.listings.value)

        val searchResults = assertIs<AppResult.Success<List<Listing>>>(
            repository.searchListings(ListingFilter())
        ).data
        assertTrue(searchResults.isEmpty())

        val myListings = assertIs<AppResult.Success<List<Listing>>>(
            repository.getMyListings()
        ).data
        assertEquals(listOf(reserved), myListings)
    }

    @Test
    fun invalidBuyerDoesNotChangeListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        // Reject the seller, an administrator, and a nonexistent account.
        for (buyerId in listOf(1, 3, 999)) {
            assertEquals(
                AppResult.Failure(AppError.INVALID_BUYER),
                repository.reserveListing(created.id, buyerId)
            )
            assertEquals(created, repository.getListing(created.id))
            assertEquals(listOf(created), repository.listings.value)
        }
    }

    @Test
    fun otherStudentCannotReserveOwnersListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        auth.login("bob@swapcampus.test", "bob123")

        assertEquals(
            AppResult.Failure(AppError.PERMISSION_DENIED),
            repository.reserveListing(created.id, buyerId = 2)
        )
        assertEquals(created, repository.getListing(created.id))
        assertEquals(listOf(created), repository.listings.value)
    }
    @Test
    fun ownerCanMarkReservedListingAsSold() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        repository.reserveListing(created.id, buyerId = 2)

        val zone = TimeZone.currentSystemDefault()
        val dateBefore = Clock.System.now().toLocalDateTime(zone).date

        val sold = assertIs<AppResult.Success<Listing>>(
            repository.markListingSold(created.id)
        ).data

        val dateAfter = Clock.System.now().toLocalDateTime(zone).date
        val status = assertIs<Sold>(sold.status)

        assertTrue(
            status.saleDate == dateBefore || status.saleDate == dateAfter
        )
        assertEquals(created.id, sold.id)
        assertEquals(created.author, sold.author)
        assertEquals(sold, repository.getListing(created.id))
        assertEquals(listOf(sold), repository.listings.value)

        val results = assertIs<AppResult.Success<List<Listing>>>(
            repository.searchListings(ListingFilter())
        ).data
        assertTrue(results.isEmpty())

        val mine = assertIs<AppResult.Success<List<Listing>>>(
            repository.getMyListings()
        ).data
        assertEquals(listOf(sold), mine)
    }

    @Test
    fun availableListingCannotBeMarkedAsSold() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        assertEquals(
            AppResult.Failure(AppError.INVALID_STATUS),
            repository.markListingSold(created.id)
        )
        assertEquals(created, repository.getListing(created.id))
        assertEquals(listOf(created), repository.listings.value)
    }

    @Test
    fun soldListingCannotBeEdited() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        repository.reserveListing(created.id, buyerId = 2)

        val sold = assertIs<AppResult.Success<Listing>>(
            repository.markListingSold(created.id)
        ).data

        assertEquals(
            AppResult.Failure(AppError.INVALID_STATUS),
            repository.updateListing(
                created.id,
                input(title = "Changed after sale")
            )
        )
        assertEquals(sold, repository.getListing(created.id))
        assertEquals(listOf(sold), repository.listings.value)
    }
    @Test
    fun ownerCanRestoreReservedListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        repository.reserveListing(created.id, buyerId = 2)

        val restored = assertIs<AppResult.Success<Listing>>(
            repository.restoreListing(created.id)
        ).data

        assertEquals(created, restored)
        assertEquals(listOf(restored), repository.listings.value)

        val results = assertIs<AppResult.Success<List<Listing>>>(
            repository.searchListings(ListingFilter())
        ).data

        assertEquals(listOf(restored), results)
    }

    @Test
    fun ownerCanRestoreSoldListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        repository.reserveListing(created.id, buyerId = 2)
        repository.markListingSold(created.id)

        val restored = assertIs<AppResult.Success<Listing>>(
            repository.restoreListing(created.id)
        ).data

        assertEquals(created, restored)
        assertEquals(restored, repository.getListing(created.id))
        assertEquals(listOf(restored), repository.listings.value)
    }

    @Test
    fun invalidRestoreDoesNotChangeListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        // An available listing cannot be restored again.
        assertEquals(
            AppResult.Failure(AppError.INVALID_STATUS),
            repository.restoreListing(created.id)
        )
        assertEquals(created, repository.getListing(created.id))

        val reserved = assertIs<AppResult.Success<Listing>>(
            repository.reserveListing(created.id, buyerId = 2)
        ).data

        // Another student cannot restore the owner's listing.
        auth.login("bob@swapcampus.test", "bob123")

        assertEquals(
            AppResult.Failure(AppError.PERMISSION_DENIED),
            repository.restoreListing(created.id)
        )
        assertEquals(reserved, repository.getListing(created.id))
        assertEquals(listOf(reserved), repository.listings.value)
    }
    @Test
    fun ownerCanRepostSoldListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        repository.reserveListing(created.id, buyerId = 2)

        val sold = assertIs<AppResult.Success<Listing>>(
            repository.markListingSold(created.id)
        ).data

        val zone = TimeZone.currentSystemDefault()
        val dateBefore = Clock.System.now().toLocalDateTime(zone).date

        val reposted = assertIs<AppResult.Success<Listing>>(
            repository.repostListing(created.id)
        ).data

        val dateAfter = Clock.System.now().toLocalDateTime(zone).date

        assertTrue(reposted.id != sold.id)
        assertEquals(Available, reposted.status)
        assertEquals(sold.title, reposted.title)
        assertEquals(sold.description, reposted.description)
        assertEquals(sold.price, reposted.price)
        assertEquals(sold.category, reposted.category)
        assertEquals(sold.photoUrl, reposted.photoUrl)
        assertEquals(sold.author, reposted.author)
        assertTrue(
            reposted.creationDate == dateBefore ||
                    reposted.creationDate == dateAfter
        )

        assertEquals(sold, repository.getListing(sold.id))
        assertEquals(reposted, repository.getListing(reposted.id))
        assertEquals(2, repository.listings.value.size)

        val results = assertIs<AppResult.Success<List<Listing>>>(
            repository.searchListings(ListingFilter())
        ).data

        assertEquals(listOf(reposted), results)
    }

    @Test
    fun unsoldListingCannotBeReposted() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        assertEquals(
            AppResult.Failure(AppError.INVALID_STATUS),
            repository.repostListing(created.id)
        )

        val reserved = assertIs<AppResult.Success<Listing>>(
            repository.reserveListing(created.id, buyerId = 2)
        ).data

        assertEquals(
            AppResult.Failure(AppError.INVALID_STATUS),
            repository.repostListing(created.id)
        )

        assertEquals(listOf(reserved), repository.listings.value)
    }

    @Test
    fun otherStudentCannotRepostSoldListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        repository.reserveListing(created.id, buyerId = 2)

        val sold = assertIs<AppResult.Success<Listing>>(
            repository.markListingSold(created.id)
        ).data

        auth.login("bob@swapcampus.test", "bob123")

        assertEquals(
            AppResult.Failure(AppError.PERMISSION_DENIED),
            repository.repostListing(created.id)
        )
        assertEquals(listOf(sold), repository.listings.value)
    }
    @Test
    fun ownerCanDeleteListingWithoutReason() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        assertEquals(
            AppResult.Success(Unit),
            repository.deleteListing(created.id)
        )
        assertNull(repository.getListing(created.id))
        assertTrue(repository.listings.value.isEmpty())

        val mine = assertIs<AppResult.Success<List<Listing>>>(
            repository.getMyListings()
        ).data
        assertTrue(mine.isEmpty())

        auth.login("admin@swapcampus.test", "admin123")
        val logs = assertIs<AppResult.Success<List<ModerationAction>>>(
            repository.getModerationActions()
        ).data
        assertTrue(logs.isEmpty())
    }

    @Test
    fun otherStudentCannotDeleteListing() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        auth.login("bob@swapcampus.test", "bob123")

        assertEquals(
            AppResult.Failure(AppError.PERMISSION_DENIED),
            repository.deleteListing(created.id)
        )
        assertEquals(created, repository.getListing(created.id))
        assertEquals(listOf(created), repository.listings.value)
    }

    @Test
    fun administratorMustProvideReasonAndDeletionIsLogged() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)
        auth.login("alice@swapcampus.test", "alice123")

        val created = assertIs<AppResult.Success<Listing>>(
            repository.createListing(input())
        ).data

        auth.login("admin@swapcampus.test", "admin123")

        for (reason in listOf(null, "", "   ")) {
            assertEquals(
                AppResult.Failure(AppError.REASON_REQUIRED),
                repository.deleteListing(created.id, reason)
            )
            assertEquals(created, repository.getListing(created.id))

            val logs = assertIs<AppResult.Success<List<ModerationAction>>>(
                repository.getModerationActions()
            ).data
            assertTrue(logs.isEmpty())
        }

        assertEquals(
            AppResult.Success(Unit),
            repository.deleteListing(
                created.id,
                reason = "  Prohibited item  "
            )
        )
        assertNull(repository.getListing(created.id))
        assertTrue(repository.listings.value.isEmpty())

        val logs = assertIs<AppResult.Success<List<ModerationAction>>>(
            repository.getModerationActions()
        ).data

        assertEquals(1, logs.size)
        val action = logs.single()
        assertEquals("Prohibited item", action.reason)
        assertEquals(3, action.administrator.id)
        assertEquals(ModerationActionType.LISTING_REMOVAL, action.type)
        assertEquals(created, assertIs<ListingTarget>(action.target).listing)

        // Repeated deletion must not create another audit record.
        assertEquals(
            AppResult.Failure(AppError.LISTING_NOT_FOUND),
            repository.deleteListing(created.id, reason = "Repeated deletion")
        )
        val logsAfter = assertIs<AppResult.Success<List<ModerationAction>>>(
            repository.getModerationActions()
        ).data
        assertEquals(logs, logsAfter)
    }

    @Test
    fun guestsAndStudentsCannotReadModerationLogs() {
        val auth = InMemoryAuthRepository()
        val repository = InMemoryListingRepository(auth)

        assertEquals(
            AppResult.Failure(AppError.LOGIN_REQUIRED),
            repository.getModerationActions()
        )

        auth.login("alice@swapcampus.test", "alice123")

        assertEquals(
            AppResult.Failure(AppError.PERMISSION_DENIED),
            repository.getModerationActions()
        )
    }
}
