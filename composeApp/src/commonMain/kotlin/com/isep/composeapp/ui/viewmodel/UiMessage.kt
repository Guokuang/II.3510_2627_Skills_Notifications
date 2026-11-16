package com.isep.composeapp.ui.viewmodel
import com.isep.shared.domain.repositories.AppError

enum class ListingAction { CREATED, UPDATED, DELETED, RESERVED, SOLD, RESTORED }
sealed interface UiMessage {
    data class Error(val error: AppError) : UiMessage
    data class Success(val action: ListingAction) : UiMessage
}
