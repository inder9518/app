package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.NewsEntity
import com.example.data.repository.NewsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface NewsUiState {
    object Loading : NewsUiState
    data class Success(val articles: List<NewsEntity>, val isSearchActive: Boolean = false) : NewsUiState
    data class Error(val message: String, val cachedArticles: List<NewsEntity>) : NewsUiState
}

class NewsViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("general")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _customSearchArticles = MutableStateFlow<List<NewsEntity>?>(null)

    // Reactive StateFlow merging room stream and pull-refresh events
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NewsUiState> = combine(
        _selectedCategory,
        _searchQuery,
        _customSearchArticles
    ) { category, query, searchResults ->
        Triple(category, query, searchResults)
    }.flatMapLatest { (category, query, searchResults) ->
        if (query.isNotBlank()) {
            if (searchResults != null) {
                flowOf(NewsUiState.Success(searchResults, isSearchActive = true))
            } else {
                flowOf(NewsUiState.Loading)
            }
        } else {
            repository.getCachedArticles(category).map<List<NewsEntity>, NewsUiState> { entities ->
                if (entities.isEmpty() && !_isRefreshing.value) {
                    // Force refresh if database is empty to fetch initial news
                    refreshCategory(category)
                }
                NewsUiState.Success(entities)
            }.catch { e ->
                emit(NewsUiState.Error(e.localizedMessage ?: "Unknown Database Error", emptyList()))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewsUiState.Loading
    )

    init {
        // Trigger initial General feed load
        refreshCurrentCategory()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = "" // Clear search when category changes
        _customSearchArticles.value = null
        refreshCategory(category)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _customSearchArticles.value = null
        }
    }

    fun performSearch() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isRefreshing.value = true
            val results = repository.searchOnlineArticles(query)
            _customSearchArticles.value = results
            _isRefreshing.value = false
        }
    }

    fun refreshCurrentCategory() {
        if (_searchQuery.value.isNotBlank()) {
            performSearch()
        } else {
            refreshCategory(_selectedCategory.value)
        }
    }

    private fun refreshCategory(category: String) {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                repository.refreshArticles(category)
            } catch (e: Exception) {
                // Keep pre-existing cache; UI will flowSuccess or view error toast
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

class NewsViewModelFactory(private val repository: NewsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
