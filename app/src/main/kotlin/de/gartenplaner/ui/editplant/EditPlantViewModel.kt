package de.gartenplaner.ui.editplant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.gartenplaner.data.model.ActivityType
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.repository.PlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EditPlantUiState(
    val name              : String          = "",
    val subtitle          : String          = "",
    val sectionId         : Int?            = null,
    val availableSections : List<Section>   = emptyList(),
    /** Index = Monat (0–11), null = kein Eintrag */
    val months            : List<MonthEntry?> = List(12) { null },
    val isNew             : Boolean         = true,
    val isSaving          : Boolean         = false,
    /** Welcher Monat hat das BottomSheet offen (-1 = keiner) */
    val openSheetMonth    : Int             = -1,
)

class EditPlantViewModel(
    private val repo   : PlanRepository,
    private val planId : Int,
    private val plantId: Int?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditPlantUiState())
    val uiState: StateFlow<EditPlantUiState> = _uiState.asStateFlow()

    init {
        loadSections()
        if (plantId != null) loadPlant(plantId)
    }

    private fun loadSections() {
        viewModelScope.launch {
            repo.getSectionsWithPlants(planId)
                .collect { sw ->
                    _uiState.update { it.copy(availableSections = sw.map { s -> s.section }) }
                }
        }
    }

    private fun loadPlant(id: Int) {
        // TODO Session 6: Plant + MonthEntries laden und State befüllen
    }

    fun updateName(v: String)     = _uiState.update { it.copy(name = v) }
    fun updateSubtitle(v: String) = _uiState.update { it.copy(subtitle = v) }
    fun updateSection(id: Int)    = _uiState.update { it.copy(sectionId = id) }

    fun openMonthSheet(month: Int) = _uiState.update { it.copy(openSheetMonth = month) }
    fun closeMonthSheet()          = _uiState.update { it.copy(openSheetMonth = -1) }

    fun setMonthEntry(month: Int, type: ActivityType?, label: String) {
        val newMonths = _uiState.value.months.toMutableList()
        newMonths[month] = if (type == null) null
                           else MonthEntry(plantId = plantId ?: 0, month = month, type = type, label = label)
        _uiState.update { it.copy(months = newMonths) }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank() || state.sectionId == null) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val savedPlantId = repo.upsertPlant(
                Plant(
                    id        = plantId ?: 0,
                    sectionId = state.sectionId,
                    name      = state.name.trim(),
                    subtitle  = state.subtitle.trim(),
                    fromLibrary = plantId == null,
                )
            )
            repo.replaceMonthEntries(savedPlantId, state.months)
            onSuccess()
        }
    }

    class Factory(
        private val repo   : PlanRepository,
        private val planId : Int,
        private val plantId: Int?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditPlantViewModel(repo, planId, plantId) as T
    }
}
