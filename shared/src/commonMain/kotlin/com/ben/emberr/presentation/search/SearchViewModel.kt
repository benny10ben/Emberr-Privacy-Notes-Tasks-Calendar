package com.ben.emberr.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.emberr.domain.model.NoteSearchResult
import com.ben.emberr.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the cross-note search screen.
 *
 * [query] is updated on every keystroke (so the UI can highlight matches instantly), while
 * [results] is derived from a *debounced* copy of that same value - re-querying Room on every
 * character would mean firing (and mostly discarding) a query per keystroke while the user is
 * still typing. flatMapLatest cancels any in-flight search the moment a newer query arrives,
 * so a slow search for "meet" never race-overwrites the result of a fast-typed "meeting".
 */
class SearchViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<NoteSearchResult>> = _query
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else flow { emit(repository.searchNotes(q)) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }
}
