package de.gartenplaner.data.seed

import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.model.ActivityType
import de.gartenplaner.data.model.ActivityType.*
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section

/**
 * Wird einmalig beim ersten App-Start ausgeführt (GardenDatabase.Callback.onCreate).
 * Erzeugt einen vollständigen Demo-Plan der die App-Stärken zeigt.
 */
object DemoSeed {

    suspend fun seed(db: GardenDatabase) {
        val planId = db.planDao().upsert(
            Plan(
                year           = 2026,
                title          = "Mein Garten",
                frostInfoLast  = "~15. April",
                frostInfoFirst = "~15. Oktober",
                climateZone    = "7a",
            )
        ).toInt()

        seedSection1(db, planId)
        seedSection2(db, planId)
        seedSection3(db, planId)
    }

    // ── Section 1: Gemüse & Kräuter ──────────────────────────────────────────

    private suspend fun seedSection1(db: GardenDatabase, planId: Int) {
        val secId = db.sectionDao().upsert(
            Section(planId = planId, title = "🥬 Gemüse & Kräuter", order = 0)
        ).toInt()

        addPlant(db, planId, secId, 0, "Tomaten", "Voranzucht", true) {
            m(2, VORANZUCHT); m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE)
        }
        addPlant(db, planId, secId, 1, "Zucchini", "Direktsaat") {
            m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE); m(8, ERNTE)
        }
        addPlant(db, planId, secId, 2, "Möhren", "Direktsaat", true) {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(7, ERNTE); m(8, ERNTE, "Ernte ↑"); m(9, ERNTE, "Lager")
        }
        addPlant(db, planId, secId, 3, "Knoblauch", "Überwinternd", true) {
            m(0, PFLEGE); m(1, PFLEGE); m(2, PFLEGE); m(3, PFLEGE); m(4, PFLEGE)
            m(5, ERNTE, "Ernte ↑")
            m(9, DIREKTSAAT, "Setzen"); m(10, DIREKTSAAT, "Setzen")
        }
        addPlant(db, planId, secId, 4, "Bohnen", "Three Sisters") {
            m(4, DIREKTSAAT); m(5, DIREKTSAAT)
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE)
        }
    }

    // ── Section 2: Kräuter ───────────────────────────────────────────────────

    private suspend fun seedSection2(db: GardenDatabase, planId: Int) {
        val secId = db.sectionDao().upsert(
            Section(planId = planId, title = "🌿 Kräuter", order = 1)
        ).toInt()

        addPlant(db, planId, secId, 0, "Basilikum", "Voranzucht", true) {
            m(2, VORANZUCHT); m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE)
        }
        addPlant(db, planId, secId, 1, "Petersilie", "Direktsaat", true) {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT); m(6, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(6, ERNTE); m(7, ERNTE); m(8, ERNTE); m(9, ERNTE)
        }
        addPlant(db, planId, secId, 2, "Schnittlauch", "Direktsaat", true) {
            m(2, DIREKTSAAT)
            m(3, ERNTE); m(4, ERNTE); m(5, ERNTE)
            m(6, ERNTE); m(7, ERNTE); m(8, ERNTE)
        }
    }

    // ── Section 3: Dauerprojekte ─────────────────────────────────────────────

    private suspend fun seedSection3(db: GardenDatabase, planId: Int) {
        val secId = db.sectionDao().upsert(
            Section(planId = planId, title = "🎋 Dauerprojekte", order = 2)
        ).toInt()

        addPlant(db, planId, secId, 0, "Bambus", "Ph. atrovaginata") {
            m(2, PFLEGE, "Rhizom"); m(3, PFLEGE, "Rhizom")
            m(4, PFLEGE, "Halme ↑"); m(5, PFLEGE); m(6, PFLEGE)
            m(9, PFLEGE, "Düngen")
        }
        addPlant(db, planId, secId, 1, "Kompost", "Trockentrennklo") {
            m(0, PFLEGE, "Wenden"); m(1, PFLEGE, "Kontrolle"); m(2, PFLEGE, "Sieben")
            m(3, AUSPFLANZEN, "Ausbringen")
            m(8, AUSPFLANZEN, "Ausbringen"); m(9, PFLEGE, "Neue Charge")
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun addPlant(
        db: GardenDatabase,
        planId: Int,
        sectionId: Int,
        order: Int,
        name: String,
        subtitle: String = "",
        fromLibrary: Boolean = false,
        months: MonthBuilder.() -> Unit,
    ) {
        val plantId = db.plantDao().upsert(
            Plant(planId = planId, sectionId = sectionId, name = name, subtitle = subtitle,
                  order = order, fromLibrary = fromLibrary)
        ).toInt()
        val entries = MonthBuilder(plantId, planId).apply(months).build()
        db.monthEntryDao().upsertAll(entries)
    }

    private class MonthBuilder(private val plantId: Int, private val planId: Int) {
        private val entries = mutableListOf<MonthEntry>()

        fun m(month: Int, type: ActivityType, label: String = type.defaultLabel) {
            entries += MonthEntry(plantId = plantId, planId = planId, month = month, type = type, label = label)
        }

        fun build(): List<MonthEntry> = entries
    }
}
