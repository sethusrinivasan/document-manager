package com.app.paperstow.presentation.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.paperstow.data.demo.DemoDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DemoUiState(
    val busy: Boolean = false,
    val loaded: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class DemoDataViewModel @Inject constructor(
    private val repository: DemoDataRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DemoUiState())
    val state: StateFlow<DemoUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loaded = repository.isLoaded())
        }
    }

    fun importSampleTrip() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val result = repository.importSampleTrip()
            _state.value = result.fold(
                onSuccess = { count ->
                    DemoUiState(busy = false, loaded = true, message = "Loaded $count sample papers. Open a folder to preview, search, or share. Remove the sample trip from the menu when you are done.")
                },
                onFailure = { e ->
                    _state.value.copy(busy = false, message = e.message ?: "Could not load sample trip")
                }
            )
        }
    }

    fun removeSampleTrip() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val result = repository.removeSampleTrip()
            _state.value = result.fold(
                onSuccess = { count ->
                    DemoUiState(busy = false, loaded = false, message = "Removed $count sample papers. Your own documents were not touched.")
                },
                onFailure = { e ->
                    _state.value.copy(busy = false, loaded = _state.value.loaded, message = e.message ?: "Could not remove sample trip")
                }
            )
        }
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
