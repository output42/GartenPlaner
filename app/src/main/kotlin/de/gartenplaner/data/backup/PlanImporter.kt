package de.gartenplaner.data.backup

import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.model.ActivityType
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section
import org.json.JSONObject

object PlanImporter {

    private const val SUPPORTED_VERSION = 1

    /** Legt einen neuen Plan an (kein Überschreiben). Gibt die neue planId zurück. */
    suspend fun import(json: String, db: GardenDatabase): Result<Int> = runCatching {
        val root = JSONObject(json)

        val version = root.optInt("version", -1)
        require(version == SUPPORTED_VERSION) {
            "Nicht unterstützte Backup-Version: $version (erwartet: $SUPPORTED_VERSION)"
        }

        val planObj = root.getJSONObject("plan")
        val planId = db.planDao().upsert(
            Plan(
                title          = planObj.getString("title"),
                year           = planObj.getInt("year"),
                frostInfoLast  = planObj.optString("frostInfoLast"),
                frostInfoFirst = planObj.optString("frostInfoFirst"),
                climateZone    = planObj.optString("climateZone"),
            )
        ).toInt()

        val sectionsArr = root.getJSONArray("sections")
        for (si in 0 until sectionsArr.length()) {
            val secObj    = sectionsArr.getJSONObject(si)
            val sectionId = db.sectionDao().upsert(
                Section(
                    planId = planId,
                    title  = secObj.getString("title"),
                    order  = secObj.optInt("order", si),
                )
            ).toInt()

            val plantsArr = secObj.getJSONArray("plants")
            for (pi in 0 until plantsArr.length()) {
                importPlant(plantsArr.getJSONObject(pi), planId, sectionId, pi, db)
            }
        }

        val unsectionedArr = root.optJSONArray("unsectioned_plants")
        if (unsectionedArr != null) {
            for (pi in 0 until unsectionedArr.length()) {
                importPlant(unsectionedArr.getJSONObject(pi), planId, sectionId = null, pi, db)
            }
        }

        planId
    }

    private suspend fun importPlant(
        plantObj : org.json.JSONObject,
        planId   : Int,
        sectionId: Int?,
        order    : Int,
        db       : GardenDatabase,
    ) {
        val plantId = db.plantDao().upsert(
            Plant(
                planId    = planId,
                sectionId = sectionId,
                name      = plantObj.getString("name"),
                subtitle  = plantObj.optString("subtitle"),
                order     = plantObj.optInt("order", order),
            )
        ).toInt()

        val monthsArr = plantObj.getJSONArray("months")
        val entries = buildList {
            for (mi in 0 until monthsArr.length()) {
                val mo = monthsArr.getJSONObject(mi)
                add(MonthEntry(
                    plantId = plantId,
                    planId  = planId,
                    month   = mo.getInt("month"),
                    type    = ActivityType.valueOf(mo.getString("type")),
                    label   = mo.getString("label"),
                ))
            }
        }
        db.monthEntryDao().upsertAll(entries)
    }
}
