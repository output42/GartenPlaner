package de.gartenplaner.ui.editplant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.gartenplaner.data.model.ActivityType
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.repository.LibraryRepository
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
    private val repo      : PlanRepository,
    private val libRepo   : LibraryRepository,
    private val planId    : Int,
    private val plantId   : Int?,
    private val templateId: Int?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditPlantUiState())
    val uiState: StateFlow<EditPlantUiState> = _uiState.asStateFlow()

    private var originalPlant: de.gartenplaner.data.model.Plant? = null

    init {
        loadSections()
        when {
            plantId    != null -> loadPlant(plantId)
            templateId != null -> loadTemplate(templateId)
        }
    }

    private fun loadSections() {
        viewModelScope.launch {
            repo.getSectionsWithPlants(planId)
                .catch { }
                .collect { sw ->
                    _uiState.update { it.copy(availableSections = sw.map { s -> s.section }) }
                }
        }
    }

    private fun loadTemplate(id: Int) {
        val template = libRepo.getById(id) ?: return
        val months = MutableList<MonthEntry?>(12) { null }
        template.months.forEachIndexed { month, entry ->
            if (entry != null) {
                months[month] = MonthEntry(plantId = 0, month = month, type = entry.type, label = entry.label)
            }
        }
        _uiState.update {
            it.copy(
                name     = template.name,
                subtitle = template.subtitle,
                months   = months,
                isNew    = true,
            )
        }
    }

    private fun loadPlant(id: Int) {
        viewModelScope.launch {
            val plant = repo.getPlantById(id) ?: return@launch
            originalPlant = plant
            val entries = repo.getMonthEntriesOnce(id)
            val months = MutableList<MonthEntry?>(12) { null }
            entries.forEach { months[it.month] = it }
            _uiState.update {
                it.copy(
                    name      = plant.name,
                    subtitle  = plant.subtitle,
                    sectionId = plant.sectionId,
                    months    = months,
                    isNew     = false,
                )
            }
        }
    }

    fun updateName(v: String)     = _uiState.update { it.copy(name = v) }
    fun updateSubtitle(v: String) = _uiState.update { it.copy(subtitle = v) }
    fun updateSection(id: Int?)   = _uiState.update { it.copy(sectionId = id) }

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
        if (state.name.isBlank()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val savedPlantId = repo.upsertPlant(
                    Plant(
                        id          = plantId ?: 0,
                        planId      = planId,
                        sectionId   = state.sectionId,
                        name        = state.name.trim(),
                        subtitle    = state.subtitle.trim(),
                        order       = originalPlant?.order ?: 0,
                        fromLibrary = originalPlant?.fromLibrary ?: false,
                    )
                )
                repo.replaceMonthEntries(savedPlantId, state.months)
                onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    class Factory(
        private val repo      : PlanRepository,
        private val planId    : Int,
        private val plantId   : Int?,
        private val templateId: Int? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditPlantViewModel(repo, LibraryRepository(), planId, plantId, templateId) as T
    }
}
