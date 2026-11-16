package com.isep.composeapp.ui.i18n

import androidx.compose.runtime.Composable
import com.isep.composeapp.resources.*
import com.isep.shared.domain.entities.*
import com.isep.shared.domain.repositories.AppError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Category.text(): String = stringResource(when (this) {
    Category.BOOKS -> Res.string.category_books
    Category.COURSES -> Res.string.category_courses
    Category.ELECTRONICS -> Res.string.category_electronics
    Category.SPORTS_LEISURE -> Res.string.category_sports_leisure
    Category.FURNITURE -> Res.string.category_furniture
    Category.CLOTHING -> Res.string.category_clothing
    Category.OTHER -> Res.string.category_other
})

@Composable
fun ListingStatus.text(): String = stringResource(when (this) {
    Available -> Res.string.status_available
    is Reserved -> Res.string.status_reserved
    is Sold -> Res.string.status_sold
})

@Composable
fun UserRole.text(): String = stringResource(when (this) {
    UserRole.STUDENT -> Res.string.role_student
    UserRole.ADMINISTRATOR -> Res.string.role_admin
})

fun AppError.messageRes(): StringResource = when (this) {
    AppError.INVALID_CREDENTIALS -> Res.string.error_invalid_credentials
    AppError.ACCOUNT_INACTIVE -> Res.string.error_account_inactive
    AppError.LOGIN_REQUIRED -> Res.string.error_login_required
    AppError.PERMISSION_DENIED -> Res.string.error_permission_denied
    AppError.EMPTY_TITLE -> Res.string.error_empty_title
    AppError.INVALID_PRICE -> Res.string.error_invalid_price
    AppError.INVALID_PRICE_RANGE -> Res.string.error_invalid_price_range
    AppError.LISTING_NOT_FOUND -> Res.string.error_listing_not_found
    AppError.INVALID_STATUS -> Res.string.error_invalid_status
    AppError.INVALID_BUYER -> Res.string.error_invalid_buyer
    AppError.REASON_REQUIRED -> Res.string.error_reason_required
}
