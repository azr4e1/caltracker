package com.lorenzo.caltracker.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.backup.BackupRepository
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val profile: Profile? = null, val goals: Goals? = null)

class SettingsViewModel(
    settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(settingsRepository.profile, settingsRepository.goals, ::SettingsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** One-off message to show in a snackbar (backup results). */
    var message by mutableStateOf<String?>(null)

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            message = runCatching { backupRepository.export(uri) }
                .fold({ "Backup saved ($it entries)" }, { "Backup failed: ${it.message}" })
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            message = runCatching { backupRepository.import(uri) }
                .fold({ "Restored $it entries" }, { "Restore failed: ${it.message}" })
        }
    }

    companion object {
        val Factory = appViewModelFactory { SettingsViewModel(it.settingsRepository, it.backupRepository) }
    }
}
