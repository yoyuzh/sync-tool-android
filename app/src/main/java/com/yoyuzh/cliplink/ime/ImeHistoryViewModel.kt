package com.yoyuzh.cliplink.ime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.usecase.GetLocalHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ImeHistoryViewModel @Inject constructor(
    getLocalHistory: GetLocalHistoryUseCase
) : ViewModel() {
    val records: StateFlow<List<ClipboardRecord>> = getLocalHistory(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
