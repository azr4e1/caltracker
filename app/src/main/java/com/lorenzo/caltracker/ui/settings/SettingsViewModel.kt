package com.lorenzo.caltracker.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.backup.BackupRepository
import com.lorenzo.caltracker.data.db.entity.ActivityLevel
import com.lorenzo.caltracker.data.db.entity.GoalMode
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.data.db.entity.Sex
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.domain.GoalCalculator
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.toDecimalOrNull
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    // Profile
    var sex by mutableStateOf(Sex.MALE)
    var birthDate by mutableStateOf<LocalDate?>(null)
    var height by mutableStateOf("")
    var weight by mutableStateOf("")
    var activity by mutableStateOf(ActivityLevel.MODERATE)

    // Goals
    var mode by mutableStateOf(GoalMode.AUTO)
    var kcal by mutableStateOf("")
    var proteinPerKg by mutableStateOf("2")
    var fatPercent by mutableStateOf("25")
    var protein by mutableStateOf("")
    var carbs by mutableStateOf("")
    var fat by mutableStateOf("")

    /** One-off message to show in a snackbar (save and backup results). */
    var message by mutableStateOf<String?>(null)

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            settingsRepository.profile.first()?.let { p ->
                sex = p.sex
                birthDate = p.birthDate
                height = p.heightCm.formatAmount()
                weight = p.weightKg.formatAmount()
                activity = p.activityLevel
            }
            settingsRepository.goals.first()?.let { g ->
                mode = g.mode
                kcal = g.kcal.formatAmount()
                proteinPerKg = g.proteinPerKg.formatAmount()
                fatPercent = (g.fatShare * 100).formatAmount()
                protein = g.protein.formatAmount()
                carbs = g.carbs.formatAmount()
                fat = g.fat.formatAmount()
            }
        }
    }

    private fun positive(value: String) = value.toDecimalOrNull()?.takeIf { it > 0 }
    private fun nonNegative(value: String) = value.toDecimalOrNull()?.takeIf { it >= 0 }

    val profile: Profile?
        get() {
            val birth = birthDate ?: return null
            return Profile(
                sex = sex,
                birthDate = birth,
                heightCm = positive(height) ?: return null,
                weightKg = positive(weight) ?: return null,
                activityLevel = activity,
            )
        }

    /** Maintenance calories from the profile, or null if the profile is incomplete. */
    val maintenanceKcal: Double?
        get() = profile?.let {
            GoalCalculator.tdee(it.sex, GoalCalculator.age(it.birthDate), it.heightCm, it.weightKg, it.activityLevel)
        }

    val bmr: Double?
        get() = profile?.let { GoalCalculator.bmr(it.sex, GoalCalculator.age(it.birthDate), it.heightCm, it.weightKg) }

    /** The targets that would be saved with the current form values, or null if something is invalid. */
    val targets: Nutrients?
        get() {
            val calories = positive(kcal) ?: return null
            return when (mode) {
                GoalMode.AUTO -> {
                    val weightKg = profile?.weightKg ?: return null
                    val perKg = nonNegative(proteinPerKg) ?: return null
                    val fatShare = nonNegative(fatPercent)?.takeIf { it <= 100 }?.div(100) ?: return null
                    GoalCalculator.autoMacros(calories, weightKg, perKg, fatShare)
                }
                GoalMode.MANUAL -> Nutrients(
                    kcal = calories,
                    protein = nonNegative(protein) ?: return null,
                    carbs = nonNegative(carbs) ?: return null,
                    fat = nonNegative(fat) ?: return null,
                )
            }
        }

    fun useMaintenance() {
        maintenanceKcal?.let { kcal = it.toInt().toString() }
    }

    fun setGoalMode(newMode: GoalMode) {
        // Switching to manual starts from the calculated values, so they can be tweaked.
        if (newMode == GoalMode.MANUAL && mode == GoalMode.AUTO) {
            targets?.let {
                protein = it.protein.toInt().toString()
                carbs = it.carbs.toInt().toString()
                fat = it.fat.toInt().toString()
            }
        }
        mode = newMode
    }

    fun save() {
        val t = targets ?: return
        viewModelScope.launch {
            profile?.let { settingsRepository.saveProfile(it) }
            settingsRepository.saveGoals(
                Goals(
                    mode = mode,
                    kcal = t.kcal,
                    protein = t.protein,
                    carbs = t.carbs,
                    fat = t.fat,
                    proteinPerKg = nonNegative(proteinPerKg) ?: 2.0,
                    fatShare = (nonNegative(fatPercent) ?: 25.0) / 100,
                ),
            )
            message = "Goals saved"
        }
    }

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
            load()
        }
    }

    companion object {
        val Factory = appViewModelFactory { SettingsViewModel(it.settingsRepository, it.backupRepository) }
    }
}
