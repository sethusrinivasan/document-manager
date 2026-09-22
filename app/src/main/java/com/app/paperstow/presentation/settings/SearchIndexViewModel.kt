package com.app.paperstow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.paperstow.data.local.SearchIndexRebuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchIndexViewModel @Inject constructor(
    private val rebuilder: SearchIndexRebuilder
) : ViewModel() {
    val state = rebuilder.state

    fun rebuild() {
        viewModelScope.launch { rebuilder.rebuild() }
    }
}
