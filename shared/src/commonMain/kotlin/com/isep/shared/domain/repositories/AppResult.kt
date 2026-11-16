package com.isep.shared.domain.repositories

enum class AppError {
    INVALID_CREDENTIALS,
    ACCOUNT_INACTIVE,
    LOGIN_REQUIRED,
    PERMISSION_DENIED,
    EMPTY_TITLE,
    INVALID_PRICE,
    INVALID_PRICE_RANGE,
    LISTING_NOT_FOUND,
    INVALID_STATUS,
    INVALID_BUYER,
    REASON_REQUIRED
}

sealed class AppResult<out T> {

    data class Success<T>(
        val data: T
    ) : AppResult<T>()

    data class Failure(
        val error: AppError
    ) : AppResult<Nothing>()
}