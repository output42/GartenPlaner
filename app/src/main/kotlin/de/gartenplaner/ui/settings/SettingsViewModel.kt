package de.gartenplaner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.repository.PlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val plan      : Plan?  = null,
    val isLoading : Boolean = true,
)

class SettingsViewModel(
    private val repo  : PlanRepository,
    private val planId: Int,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repo.getPlanById(planId)
        .map { SettingsUiState(plan = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateTitle(title: String) = updatePlan { it.copy(title = title.trim()) }
    fun updateYear(year: Int)      = updatePlan { it.copy(year = year) }
    fun updateFrostLast(v: String) = updatePlan { it.copy(frostInfoLast = v.trim()) }
    fun updateFrostFirst(v: String)= updatePlan { it.copy(frostInfoFirst = v.trim()) }
    fun updateClimateZone(v: String) = updatePlan { it.copy(climateZone = v.trim()) }

    fun copyPlanForYear(newYear: Int, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val newPlanId = repo.copyPlanForYear(planId, newYear)
            onCreated(newPlanId)
        }
    }

    fun deletePlan(onDeleted: () -> Unit) {
        viewModelScope.launch {
            uiState.value.plan?.let { repo.deletePlan(it) }
            onDeleted()
        }
    }

    private fun updatePlan(transform: (Plan) -> Plan) {
        val plan = uiState.value.plan ?: return
        viewModelScope.launch { repo.upsertPlan(transform(plan)) }
    }

    class Factory(private val repo: PlanRepository, private val planId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo, planId) as T
    }
}
