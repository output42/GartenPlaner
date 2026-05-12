package de.gartenplaner.ui.planlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.repository.PlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlanListUiState {
    data object Loading : PlanListUiState
    data object Empty   : PlanListUiState
    data class  Success(val plans: List<Plan>) : PlanListUiState
}

class PlanListViewModel(private val repo: PlanRepository) : ViewModel() {

    val uiState: StateFlow<PlanListUiState> = repo.getAllPlans()
        .map { plans ->
            if (plans.isEmpty()) PlanListUiState.Empty
            else PlanListUiState.Success(plans)
        }
        .stateIn(
            scope             = viewModelScope,
            started           = SharingStarted.WhileSubscribed(5_000),
            initialValue      = PlanListUiState.Loading,
        )

    fun createPlan(year: Int, title: String, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val planId = repo.upsertPlan(Plan(year = year, title = title))
            onCreated(planId)
        }
    }

    fun deletePlan(plan: Plan) {
        viewModelScope.launch { repo.deletePlan(plan) }
    }

    class Factory(private val repo: PlanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlanListViewModel(repo) as T
    }
}
