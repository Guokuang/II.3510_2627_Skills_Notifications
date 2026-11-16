package com.isep.composeapp.ui

import androidx.lifecycle.ViewModel
import com.isep.shared.domain.repositories.AppResult
import com.isep.shared.domain.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class LoginError { EMPTY_FIELDS, INVALID_CREDENTIALS }

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    val currentUser = repository.currentUser

    private val _error = MutableStateFlow<LoginError?>(null)
    val error: StateFlow<LoginError?> = _error

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = LoginError.EMPTY_FIELDS
            return
        }

        val result = repository.login(email, password)
        if (result is AppResult.Success) {
            _error.value = null
        } else {
            _error.value = LoginError.INVALID_CREDENTIALS
        }
    }

    fun logout() {
        repository.logout()
        _error.value = null
    }
}
