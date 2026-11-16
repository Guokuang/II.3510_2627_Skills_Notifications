package com.isep.shared.data

import com.isep.shared.domain.entities.UserRole
import com.isep.shared.domain.repositories.AppError
import com.isep.shared.domain.repositories.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryAuthRepositoryTest {

    @Test
    fun initialUserIsNull() {
        val repository = InMemoryAuthRepository()

        assertNull(repository.currentUser.value)
    }

    @Test
    fun correctCredentialsLogIn() {
        val repository = InMemoryAuthRepository()

        val result = repository.login(
            "alice@swapcampus.test",
            "alice123"
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, repository.currentUser.value?.id)
        assertEquals(
            UserRole.STUDENT,
            repository.currentUser.value?.role
        )
    }

    @Test
    fun wrongPasswordDoesNotLogIn() {
        val repository = InMemoryAuthRepository()

        val result = repository.login(
            "alice@swapcampus.test",
            "wrong-password"
        )

        assertEquals(
            AppResult.Failure(AppError.INVALID_CREDENTIALS),
            result
        )
        assertNull(repository.currentUser.value)
    }

    @Test
    fun failedLoginPreservesExistingSession() {
        val repository = InMemoryAuthRepository()
        repository.login("alice@swapcampus.test", "alice123")

        val result = repository.login(
            "bob@swapcampus.test",
            "wrong-password"
        )

        assertEquals(
            AppResult.Failure(AppError.INVALID_CREDENTIALS),
            result
        )
        assertEquals(1, repository.currentUser.value?.id)
    }

    @Test
    fun adminCanLogIn() {
        val repository = InMemoryAuthRepository()

        val result = repository.login(
            "admin@swapcampus.test",
            "admin123"
        )

        assertTrue(result is AppResult.Success)
        assertEquals(
            UserRole.ADMINISTRATOR,
            repository.currentUser.value?.role
        )
    }

    @Test
    fun logoutClearsCurrentUser() {
        val repository = InMemoryAuthRepository()
        repository.login("alice@swapcampus.test", "alice123")

        repository.logout()

        assertNull(repository.currentUser.value)
    }
}