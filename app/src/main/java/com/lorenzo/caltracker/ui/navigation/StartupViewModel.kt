package com.lorenzo.caltracker.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** Decides once, at launch, whether to show first-time setup. Null while still checking. */
class StartupViewModel(settingsRepository: SettingsRepository) : ViewModel() {
    val needsSetup: StateFlow<Boolean?> = flow {
        emit(settingsRepository.profile.first() == null || settingsRepository.goals.first() == null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory = appViewModelFactory { StartupViewModel(it.settingsRepository) }
    }
}
