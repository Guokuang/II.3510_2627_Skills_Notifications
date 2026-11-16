package com.isep.composeapp
import com.isep.shared.domain.entities.Listing
import com.isep.shared.domain.repositories.AppError
sealed interface ListingsUiState {
    data object Loading : ListingsUiState
    data class Success(val listings: List<Listing>) : ListingsUiState
    data class Error(val error: AppError) : ListingsUiState
}
