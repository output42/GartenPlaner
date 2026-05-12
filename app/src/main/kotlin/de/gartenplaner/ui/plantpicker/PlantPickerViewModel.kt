package de.gartenplaner.ui.plantpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.gartenplaner.data.model.PlantTemplate
import de.gartenplaner.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlantPickerUiState(
    val query           : String            = "",
    val selectedCategory: String?           = null,
    val results         : List<PlantTemplate> = emptyList(),
    val categories      : List<String>      = emptyList(),
)

class PlantPickerViewModel(private val repo: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlantPickerUiState(
            results    = repo.getAll(),
            categories = repo.getCategories(),
        )
    )
    val uiState: StateFlow<PlantPickerUiState> = _uiState.asStateFlow()

    fun updateQuery(q: String) {
        _uiState.update { it.copy(query = q, results = repo.search(q, it.selectedCategory)) }
    }

    fun selectCategory(cat: String?) {
        _uiState.update { it.copy(selectedCategory = cat, results = repo.search(it.query, cat)) }
    }

    class Factory(private val repo: LibraryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlantPickerViewModel(repo) as T
    }
}
