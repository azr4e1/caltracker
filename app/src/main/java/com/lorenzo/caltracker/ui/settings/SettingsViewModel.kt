package com.lorenzo.caltracker.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.backup.BackupRepository
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.export.ExportRepository
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.data.repository.OnlineFoodRepository
import com.lorenzo.caltracker.data.repository.PreferencesRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.data.repository.UsdaKeyCheck
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
    private val exportRepository: ExportRepository,
    private val preferences: PreferencesRepository,
    private val onlineFoods: OnlineFoodRepository,
) : ViewModel() {
    /** The user's own USDA key, or null when the shared one is used. */
    val usdaApiKey: StateFlow<String?> = preferences.usdaApiKey

    var editingUsdaKey by mutableStateOf(false)
    var checkingUsdaKey by mutableStateOf(false)
        private set
    var usdaKeyError by mutableStateOf<String?>(null)
        private set

    fun openUsdaKey() {
        usdaKeyError = null
        editingUsdaKey = true
    }

    /** Checks the key with USDA, then saves it unless USDA rejects it. */
    fun saveUsdaKey(key: String) {
        checkingUsdaKey = true
        usdaKeyError = null
        viewModelScope.launch {
            val check = onlineFoods.checkUsdaKey(key)
            checkingUsdaKey = false
            if (check == UsdaKeyCheck.Invalid) {
                usdaKeyError = "USDA didn't accept this key. Check it was copied completely."
                return@launch
            }
            preferences.setUsdaApiKey(key)
            editingUsdaKey = false
            message = when (check) {
                is UsdaKeyCheck.Unverified -> "USDA key saved, but not checked yet: ${check.reason}"
                else -> "USDA key saved and working"
            }
        }
    }

    fun useSharedUsdaKey() {
        preferences.setUsdaApiKey(null)
        editingUsdaKey = false
        message = "Using the shared USDA key"
    }

    val uiState: StateFlow<SettingsUiState> =
        combine(settingsRepository.profile, settingsRepository.goals, ::SettingsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** One-off message to show in a snackbar (backup and export results). */
    var message by mutableStateOf<String?>(null)

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            message = runCatching { backupRepository.export(uri) }
                .fold({ "Backup saved ($it entries)" }, { "Backup failed: ${it.message}" })
        }
    }

    fun exportExcel(uri: Uri) {
        viewModelScope.launch {
            message = runCatching { exportRepository.exportExcel(uri) }
                .fold({ "Excel file saved ($it entries)" }, { "Export failed: ${it.message}" })
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            message = runCatching { backupRepository.import(uri) }
                .fold({ "Restored $it entries" }, { "Restore failed: ${it.message}" })
        }
    }

    fun deleteAllData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            backupRepository.deleteAll()
            onDeleted()
        }
    }

    companion object {
        val Factory = appViewModelFactory { SettingsViewModel(
                it.settingsRepository, it.backupRepository, it.exportRepository, it.preferencesRepository, it.onlineFoodRepository,
            ) }
    }
}
