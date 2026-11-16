package com.isep.shared.data

import com.isep.shared.domain.repositories.AppError
import com.isep.shared.domain.repositories.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppContainerTest {

    @Test
    fun repositoriesShareLoginState() {
        val auth = AppContainer.authRepository
        val listings = AppContainer.listingRepository

        auth.logout()

        try {
            // Guests cannot access their personal listings.
            assertEquals(
                AppResult.Failure(AppError.LOGIN_REQUIRED),
                listings.getMyListings()
            )

            // The listing repository observes the shared login session.
            val loginResult = auth.login(
                "alice@swapcampus.test",
                "alice123"
            )

            assertTrue(loginResult is AppResult.Success)
            assertEquals(1, auth.currentUser.value?.id)
            assertTrue(listings.getMyListings() is AppResult.Success)

            // Logging out removes access to personal listings.
            auth.logout()

            assertNull(auth.currentUser.value)
            assertEquals(
                AppResult.Failure(AppError.LOGIN_REQUIRED),
                listings.getMyListings()
            )
        } finally {
            // Leave the shared session logged out after the test.
            auth.logout()
        }
    }
}