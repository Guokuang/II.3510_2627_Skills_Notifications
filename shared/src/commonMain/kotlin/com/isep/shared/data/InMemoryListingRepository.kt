package com.isep.shared.data

import com.isep.shared.domain.entities.Listing
import com.isep.shared.domain.entities.ListingCatalog
import com.isep.shared.domain.entities.ModerationAction
import com.isep.shared.domain.entities.UserRole
import com.isep.shared.domain.repositories.AppError
import com.isep.shared.domain.repositories.AppResult
import com.isep.shared.domain.repositories.AuthRepository
import com.isep.shared.domain.repositories.ListingFilter
import com.isep.shared.domain.repositories.ListingInput
import com.isep.shared.domain.repositories.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.isep.shared.domain.entities.Sold
import com.isep.shared.domain.entities.Available
import com.isep.shared.domain.entities.Reserved
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.isep.shared.domain.entities.ListingTarget
import com.isep.shared.domain.entities.ModerationActionType
import com.isep.shared.domain.entities.AppNotification

class InMemoryListingRepository(
    private val authRepository: AuthRepository,
    private val catalog: ListingCatalog = ListingCatalog()
) : ListingRepository {
    private val moderationActions = mutableListOf<ModerationAction>()
    private var nextModerationId = 1
    private val _listings =
        MutableStateFlow<List<Listing>>(catalog.allListings())

    override val listings: StateFlow<List<Listing>> =
        _listings.asStateFlow()

    private val _notifications =
        MutableStateFlow<List<AppNotification>>(emptyList())

    override val notifications: StateFlow<List<AppNotification>> =
        _notifications.asStateFlow()

    private var nextNotificationId = 1

    private fun publishListings() {
        _listings.value = catalog.allListings()
    }

    override fun getListing(id: Int): Listing? {
        return catalog.findById(id)
    }

    override fun searchListings(
        filter: ListingFilter
    ): AppResult<List<Listing>> {
        val min = filter.minPrice
        val max = filter.maxPrice

        if (
            (min != null && (!min.isFinite() || min < 0)) ||
            (max != null && (!max.isFinite() || max < 0)) ||
            (min != null && max != null && min > max)
        ) {
            return AppResult.Failure(AppError.INVALID_PRICE_RANGE)
        }

        val result = catalog.searchListings(
            keyword = filter.keyword,
            category = filter.category,
            minPrice = min,
            maxPrice = max
        )

        return AppResult.Success(result)
    }

    override fun getMyListings(): AppResult<List<Listing>> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (user.role != UserRole.STUDENT) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        val result = catalog.allListings().filter {
            it.author.id == user.id
        }

        return AppResult.Success(result)
    }

    override fun createListing(
        input: ListingInput
    ): AppResult<Listing> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        if (user.role != UserRole.STUDENT) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        if (input.title.isBlank()) {
            return AppResult.Failure(AppError.EMPTY_TITLE)
        }

        if (!input.price.isFinite() || input.price <= 0) {
            return AppResult.Failure(AppError.INVALID_PRICE)
        }

        val listing = catalog.postListing(
            title = input.title.trim(),
            description = input.description,
            price = input.price,
            category = input.category,
            author = user,
            photoUrl = input.photoUrl
        )

        publishListings()

        return AppResult.Success(listing)
    }

    override fun updateListing(
        id: Int,
        input: ListingInput
    ): AppResult<Listing> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        val existing = catalog.findById(id)
            ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)

        if (
            user.role != UserRole.STUDENT ||
            existing.author.id != user.id
        ) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        if (existing.status is Sold) {
            return AppResult.Failure(AppError.INVALID_STATUS)
        }

        if (input.title.isBlank()) {
            return AppResult.Failure(AppError.EMPTY_TITLE)
        }

        if (!input.price.isFinite() || input.price <= 0) {
            return AppResult.Failure(AppError.INVALID_PRICE)
        }

        val updated = catalog.updateListing(
            id = id,
            title = input.title.trim(),
            description = input.description,
            price = input.price,
            category = input.category,
            photoUrl = input.photoUrl
        ) ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)

        publishListings()

        return AppResult.Success(updated)
    }

    override fun requestReservation(id: Int): AppResult<Listing> {
        val buyer = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)
        if (!buyer.active) return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        if (buyer.role != UserRole.STUDENT) return AppResult.Failure(AppError.PERMISSION_DENIED)
        val listing = catalog.findById(id)
            ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)
        if (listing.author.id == buyer.id) return AppResult.Failure(AppError.INVALID_BUYER)
        if (listing.status != Available) return AppResult.Failure(AppError.INVALID_STATUS)
        val updated = catalog.changeStatus(id, Reserved(buyer.id.toString()))
            ?: return AppResult.Failure(AppError.INVALID_STATUS)
        publishListings()

        // TODO: Notify the listing owner when a buyer reserves the listing.
        // Hint: markListingSold() below contains a notification example.

        return AppResult.Success(updated)
    }

    override fun reserveListing(
        id: Int,
        buyerId: Int
    ): AppResult<Listing> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        val existing = catalog.findById(id)
            ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)

        if (
            user.role != UserRole.STUDENT ||
            existing.author.id != user.id
        ) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        if (existing.status != Available) {
            return AppResult.Failure(AppError.INVALID_STATUS)
        }

        val buyer = authRepository.findUserById(buyerId)
            ?: return AppResult.Failure(AppError.INVALID_BUYER)

        if (
            !buyer.active ||
            buyer.role != UserRole.STUDENT ||
            buyer.id == user.id
        ) {
            return AppResult.Failure(AppError.INVALID_BUYER)
        }

        val updated = catalog.changeStatus(
            id = id,
            newStatus = Reserved(buyer = buyer.id.toString())
        ) ?: return AppResult.Failure(AppError.INVALID_STATUS)

        publishListings()

        return AppResult.Success(updated)
    }

    override fun markListingSold(id: Int): AppResult<Listing> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        val existing = catalog.findById(id)
            ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)

        if (
            user.role != UserRole.STUDENT ||
            existing.author.id != user.id
        ) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        if (existing.status !is Reserved) {
            return AppResult.Failure(AppError.INVALID_STATUS)
        }

        val buyerId = (existing.status as Reserved).buyer.toInt()

        val saleDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        val updated = catalog.changeStatus(
            id = id,
            newStatus = Sold(saleDate = saleDate)
        ) ?: return AppResult.Failure(AppError.INVALID_STATUS)

        _notifications.value = _notifications.value + AppNotification(
            id = nextNotificationId++,
            recipientId = buyerId,
            listingId = existing.id,
            message = "Your reserved listing was marked as sold: ${existing.title}"
        )

        publishListings()

        return AppResult.Success(updated)
    }

    override fun restoreListing(id: Int): AppResult<Listing> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        val existing = catalog.findById(id)
            ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)

        if (
            user.role != UserRole.STUDENT ||
            existing.author.id != user.id
        ) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        if (
            existing.status !is Reserved &&
            existing.status !is Sold
        ) {
            return AppResult.Failure(AppError.INVALID_STATUS)
        }

        val updated = catalog.changeStatus(
            id = id,
            newStatus = Available
        ) ?: return AppResult.Failure(AppError.INVALID_STATUS)

        publishListings()

        return AppResult.Success(updated)
    }

    override fun repostListing(id: Int): AppResult<Listing> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        val existing = catalog.findById(id)
            ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)

        if (
            user.role != UserRole.STUDENT ||
            existing.author.id != user.id
        ) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        if (existing.status !is Sold) {
            return AppResult.Failure(AppError.INVALID_STATUS)
        }

        val newListing = catalog.postListing(
            title = existing.title,
            description = existing.description,
            price = existing.price,
            category = existing.category,
            author = user,
            photoUrl = existing.photoUrl
        )

        publishListings()

        return AppResult.Success(newListing)
    }
    /** Deletes a listing; administrator deletion requires an audit reason. */
    override fun deleteListing(
        id: Int,
        reason: String?
    ): AppResult<Unit> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        val existing = catalog.findById(id)
            ?: return AppResult.Failure(AppError.LISTING_NOT_FOUND)

        val auditAction = when (user.role) {
            UserRole.STUDENT -> {
                if (existing.author.id != user.id) {
                    return AppResult.Failure(AppError.PERMISSION_DENIED)
                }
                null
            }

            UserRole.ADMINISTRATOR -> {
                val trimmedReason = reason?.trim().orEmpty()

                if (trimmedReason.isEmpty()) {
                    return AppResult.Failure(AppError.REASON_REQUIRED)
                }

                ModerationAction(
                    id = nextModerationId,
                    target = ListingTarget(existing),
                    type = ModerationActionType.LISTING_REMOVAL,
                    reason = trimmedReason,
                    administrator = user,
                    date = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                )
            }
        }

        if (!catalog.removeListing(id)) {
            return AppResult.Failure(AppError.LISTING_NOT_FOUND)
        }

        if (auditAction != null) {
            moderationActions.add(auditAction)
            nextModerationId++
        }

        publishListings()

        return AppResult.Success(Unit)
    }

    /** Returns deletion audit records to an authenticated administrator. */
    override fun getModerationActions():
            AppResult<List<ModerationAction>> {
        val user = authRepository.currentUser.value
            ?: return AppResult.Failure(AppError.LOGIN_REQUIRED)

        if (!user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        if (user.role != UserRole.ADMINISTRATOR) {
            return AppResult.Failure(AppError.PERMISSION_DENIED)
        }

        return AppResult.Success(moderationActions.toList())
    }
}
