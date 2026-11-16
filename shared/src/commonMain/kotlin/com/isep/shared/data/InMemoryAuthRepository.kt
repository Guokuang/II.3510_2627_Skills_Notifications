package com.isep.shared.data

import com.isep.shared.domain.entities.UserAccount
import com.isep.shared.domain.entities.UserRole
import com.isep.shared.domain.repositories.AppError
import com.isep.shared.domain.repositories.AppResult
import com.isep.shared.domain.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class InMemoryAuthRepository : AuthRepository {

    private data class MockAccount(
        val user: UserAccount,
        val password: String
    )

    private val accounts = listOf(
        MockAccount(
            user = UserAccount(
                id = 1,
                name = "Alice",
                email = "alice@swapcampus.test",
                passwordHash = "",
                creationDate = LocalDate(2026, 10, 7),
                role = UserRole.STUDENT
            ),
            password = "alice123"
        ),
        MockAccount(
            user = UserAccount(
                id = 2,
                name = "Bob",
                email = "bob@swapcampus.test",
                passwordHash = "",
                creationDate = LocalDate(2026, 10, 7),
                role = UserRole.STUDENT
            ),
            password = "bob123"
        ),
        MockAccount(
            user = UserAccount(
                id = 3,
                name = "Admin",
                email = "admin@swapcampus.test",
                passwordHash = "",
                creationDate = LocalDate(2026, 10, 7),
                role = UserRole.ADMINISTRATOR
            ),
            password = "admin123"
        )
    )

    private val _currentUser = MutableStateFlow<UserAccount?>(null)

    override val currentUser: StateFlow<UserAccount?> =
        _currentUser.asStateFlow()

    override fun login(
        email: String,
        password: String
    ): AppResult<UserAccount> {
        val account = accounts.firstOrNull {
            it.user.email.equals(
                email.trim(),
                ignoreCase = true
            ) && it.password == password
        } ?: return AppResult.Failure(AppError.INVALID_CREDENTIALS)

        if (!account.user.active) {
            return AppResult.Failure(AppError.ACCOUNT_INACTIVE)
        }

        _currentUser.value = account.user
        return AppResult.Success(account.user)
    }

    override fun logout() {
        _currentUser.value = null
    }

    override fun findUserById(id: Int): UserAccount? {
        return accounts.firstOrNull {
            it.user.id == id
        }?.user
    }
}