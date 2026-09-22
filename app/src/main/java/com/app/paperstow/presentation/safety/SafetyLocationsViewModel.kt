package com.app.paperstow.presentation.safety

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.paperstow.data.safety.SafetyLocationRepository
import com.app.paperstow.data.safety.SafetyTrailPreferences
import com.app.paperstow.data.safety.SafetyTrailService
import com.app.paperstow.domain.safety.SafetyPlace
import com.app.paperstow.domain.safety.UniqueLocations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyLocationsViewModel @Inject constructor(
    private val repository: SafetyLocationRepository
) : ViewModel() {

    val places: StateFlow<List<SafetyPlace>> = repository.observeLast24Hours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val running: StateFlow<Boolean> = SafetyTrailPreferences.running

    init {
        viewModelScope.launch { repository.pruneExpired() }
    }

    fun startTrail(context: Context) {
        SafetyTrailService.start(context.applicationContext)
    }

    fun stopTrail(context: Context) {
        SafetyTrailService.stop(context.applicationContext)
    }

    fun clearPlaces() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun shareAllText(): String = UniqueLocations.shareAll(places.value)

    fun shareLastKnownText(): String? {
        val last = places.value.firstOrNull() ?: return null
        return UniqueLocations.shareLastKnown(last)
    }
}
