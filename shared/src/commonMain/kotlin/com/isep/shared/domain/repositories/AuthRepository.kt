package com.isep.shared.domain.repositories

import com.isep.shared.domain.entities.UserAccount
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    val currentUser: StateFlow<UserAccount?>

    fun login(
        email: String,
        password: String
    ): AppResult<UserAccount>

    fun logout()
    /** Finds a mock user by ID, or returns null if absent. */
    fun findUserById(id: Int): UserAccount?
}