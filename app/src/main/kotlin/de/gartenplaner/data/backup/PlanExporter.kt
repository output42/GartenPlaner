package de.gartenplaner.data.backup

import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.SectionWithPlants
import org.json.JSONArray
import org.json.JSONObject

object PlanExporter {

    private const val FORMAT_VERSION = 1

    fun export(
        plan             : Plan,
        sections         : List<SectionWithPlants>,
        monthEntries     : Map<Int, List<MonthEntry>>,
        unsectionedPlants: List<Plant> = emptyList(),
    ): String = JSONObject().apply {
        put("version", FORMAT_VERSION)
        put("plan", planJson(plan))
        put("sections", sectionsJson(sections, monthEntries))
        if (unsectionedPlants.isNotEmpty()) {
            put("unsectioned_plants", plantsJson(unsectionedPlants, monthEntries))
        }
    }.toString(2)

    private fun planJson(plan: Plan) = JSONObject().apply {
        put("title",          plan.title)
        put("year",           plan.year)
        put("frostInfoLast",  plan.frostInfoLast)
        put("frostInfoFirst", plan.frostInfoFirst)
        put("climateZone",    plan.climateZone)
    }

    private fun sectionsJson(
        sections    : List<SectionWithPlants>,
        monthEntries: Map<Int, List<MonthEntry>>,
    ) = JSONArray().apply {
        sections.forEach { sw ->
            put(JSONObject().apply {
                put("title",  sw.section.title)
                put("order",  sw.section.order)
                put("plants", plantsJson(sw.plants, monthEntries))
            })
        }
    }

    private fun plantsJson(
        plants      : List<Plant>,
        monthEntries: Map<Int, List<MonthEntry>>,
    ) = JSONArray().apply {
        plants.forEach { plant ->
            put(JSONObject().apply {
                put("name",     plant.name)
                put("subtitle", plant.subtitle)
                put("order",    plant.order)
                put("months",   monthsJson(monthEntries[plant.id] ?: emptyList()))
            })
        }
    }

    private fun monthsJson(entries: List<MonthEntry>) = JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().apply {
                put("month", entry.month)
                put("type",  entry.type.name)
                put("label", entry.label)
            })
        }
    }
}
