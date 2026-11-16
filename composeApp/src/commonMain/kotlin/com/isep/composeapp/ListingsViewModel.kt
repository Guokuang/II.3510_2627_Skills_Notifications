package com.isep.composeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isep.shared.domain.entities.Category
import com.isep.shared.domain.repositories.*
import com.isep.composeapp.ui.viewmodel.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ListingsViewModel(private val repository: ListingRepository) : ViewModel() {
    private var keyword = ""
    private var category: Category? = null
    val listings = repository.listings
    private val _uiState = MutableStateFlow<ListingsUiState>(ListingsUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val events = Channel<UiMessage>(Channel.BUFFERED)
    val messages = events.receiveAsFlow()

    init { viewModelScope.launch { repository.listings.collect { refresh() } } }

    fun filter(query: String, selectedCategory: Category?) {
        keyword = query
        category = selectedCategory
        refresh()
    }

    fun refresh() {
        _uiState.value = when (val result = repository.searchListings(ListingFilter(keyword, category))) {
            is AppResult.Success -> ListingsUiState.Success(result.data)
            is AppResult.Failure -> ListingsUiState.Error(result.error)
        }
    }

    fun save(id: Int?, title: String, description: String, price: String,
             category: Category, photoUrl: String): Boolean {
        val parsed = price.trim().replace(',', '.').toDoubleOrNull()
        if (title.isBlank()) return complete(AppResult.Failure(AppError.EMPTY_TITLE), ListingAction.CREATED)
        if (parsed == null || !parsed.isFinite() || parsed <= 0) {
            return complete(AppResult.Failure(AppError.INVALID_PRICE), ListingAction.CREATED)
        }
        val input = ListingInput(title.trim(), description, parsed, category, photoUrl.ifBlank { null })
        return if (id == null) complete(repository.createListing(input), ListingAction.CREATED)
            else complete(repository.updateListing(id, input), ListingAction.UPDATED)
    }

    fun delete(id: Int) = complete(repository.deleteListing(id), ListingAction.DELETED)
    fun reserve(id: Int) = complete(repository.requestReservation(id), ListingAction.RESERVED)
    fun markSold(id: Int) = complete(repository.markListingSold(id), ListingAction.SOLD)
    fun restore(id: Int) = complete(repository.restoreListing(id), ListingAction.RESTORED)

    private fun complete(result: AppResult<*>, action: ListingAction): Boolean {
        viewModelScope.launch {
            events.send(when (result) {
                is AppResult.Failure -> UiMessage.Error(result.error)
                is AppResult.Success -> UiMessage.Success(action)
            })
        }
        return result is AppResult.Success
    }
}
