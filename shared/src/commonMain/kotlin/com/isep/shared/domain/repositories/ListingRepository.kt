package com.isep.shared.domain.repositories

import com.isep.shared.domain.entities.Listing
import com.isep.shared.domain.entities.ModerationAction
import kotlinx.coroutines.flow.StateFlow
import com.isep.shared.domain.entities.AppNotification

/** Lab 1 Item Management Interface; the specific implementation is responsible for verifying identities and enforcing business rules.。 */
interface ListingRepository {

    /** All items, including those reserved and sold; interface that notifies when data changes。 */
    val listings: StateFlow<List<Listing>>
    val notifications: StateFlow<List<AppNotification>>

    /** Retrieve an item by ID; return null if it does not exist.。 */
    fun getListing(id: Int): Listing?

    /** Search for items for sale without logging in; an error is returned if the price range is invalid.。 */
    fun searchListings(
        filter: ListingFilter = ListingFilter()
    ): AppResult<List<Listing>>

    /** Retrieve all listings for the current student, including those reserved and sold。 */
    fun getMyListings(): AppResult<List<Listing>>

    /** When a student posts an item, an ID, author, date, and initial status are automatically generated. */
    fun createListing(input: ListingInput): AppResult<Listing>

    /** Authors can edit their own items; sold items cannot be edited directly.。 */
    fun updateListing(
        id: Int,
        input: ListingInput
    ): AppResult<Listing>

    /** The signed-in student reserves another student's available listing. */
    fun requestReservation(id: Int): AppResult<Listing>

    fun reserveListing(
        id: Int,
        buyerId: Int
    ): AppResult<Listing>

    fun markListingSold(id: Int): AppResult<Listing>

    fun restoreListing(id: Int): AppResult<Listing>

    fun repostListing(id: Int): AppResult<Listing>

    fun deleteListing(
        id: Int,
        reason: String? = null
    ): AppResult<Unit>

    fun getModerationActions(): AppResult<List<ModerationAction>>
}