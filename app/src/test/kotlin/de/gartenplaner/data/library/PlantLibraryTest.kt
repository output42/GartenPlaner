package de.gartenplaner.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantLibraryTest {

    @Test
    fun all_containsAtLeast37Plants() {
        assertTrue(
            "Expected at least 37 plants, got ${PlantLibrary.all.size}",
            PlantLibrary.all.size >= 37,
        )
    }

    @Test
    fun categories_nonEmpty() {
        assertTrue(PlantLibrary.categories.isNotEmpty())
    }

    @Test
    fun byCategory_returnsCorrectSubset() {
        for (cat in PlantLibrary.categories) {
            val subset = PlantLibrary.byCategory(cat)
            assertTrue("Category '$cat' must be non-empty", subset.isNotEmpty())
            assertTrue(
                "All plants in category '$cat' must have that category",
                subset.all { it.category == cat },
            )
        }
    }

    @Test
    fun eachTemplate_hasTwelveMonthSlots() {
        for (template in PlantLibrary.all) {
            assertEquals(
                "Template '${template.name}' must have exactly 12 month slots",
                12,
                template.months.size,
            )
        }
    }

    @Test
    fun eachTemplate_hasNonBlankNameAndCategory() {
        for (template in PlantLibrary.all) {
            assertTrue("Template name must not be blank", template.name.isNotBlank())
            assertTrue("Template '${template.name}' category must not be blank", template.category.isNotBlank())
        }
    }

    @Test
    fun search_emptyQuery_returnsAllPlants() {
        val repo = LibraryRepository()
        val results = repo.search("", null)
        assertEquals(PlantLibrary.all.size, results.size)
    }

    @Test
    fun search_byName_returnsMatchingPlants() {
        val repo = LibraryRepository()
        val results = repo.search("Tomate", null)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.name.contains("Tomate", ignoreCase = true) ||
                                 it.subtitle.contains("Tomate", ignoreCase = true) ||
                                 it.category.contains("Tomate", ignoreCase = true) })
    }

    @Test
    fun search_byCategory_returnsOnlyThatCategory() {
        val repo    = LibraryRepository()
        val firstCat = PlantLibrary.categories.first()
        val results  = repo.search("", firstCat)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == firstCat })
    }

    @Test
    fun allTemplateIds_unique() {
        val ids = PlantLibrary.all.map { it.id }
        assertEquals("All template IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun allTemplateIds_positive() {
        for (template in PlantLibrary.all) {
            assertTrue("Template '${template.name}' must have positive id", template.id > 0)
        }
    }

    @Test
    fun atLeastOneNonNullMonthPerTemplate() {
        for (template in PlantLibrary.all) {
            assertNotNull(
                "Template '${template.name}' must have at least one active month",
                template.months.firstOrNull { it != null },
            )
        }
    }
}
