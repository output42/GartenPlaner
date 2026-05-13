package de.gartenplaner.data.repository

import de.gartenplaner.data.library.PlantLibrary
import de.gartenplaner.data.model.PlantTemplate

class LibraryRepository {

    fun getAll(): List<PlantTemplate> = PlantLibrary.all

    fun getCategories(): List<String> = PlantLibrary.categories

    fun search(query: String, category: String?): List<PlantTemplate> {
        val pool = if (category != null) PlantLibrary.byCategory(category) else PlantLibrary.all
        return if (query.isBlank()) pool
               else pool.filter {
                   it.name.contains(query, ignoreCase = true) ||
                   it.subtitle.contains(query, ignoreCase = true) ||
                   it.category.contains(query, ignoreCase = true)
               }
    }
}
