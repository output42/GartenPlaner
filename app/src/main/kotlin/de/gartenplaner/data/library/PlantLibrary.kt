package de.gartenplaner.data.library

import de.gartenplaner.data.model.ActivityType.*
import de.gartenplaner.data.model.MonthEntryTemplate
import de.gartenplaner.data.model.PlantTemplate

/**
 * Statische Pflanzenbibliothek — ~40 Einträge für mitteleuropäische Gärten (Zone 7a).
 * Keine externe Datenbank, kein Asset-File → Compile-Time-Sicherheit, F-Droid-kompatibel.
 */
object PlantLibrary {

    val categories = listOf(
        "Fruchtgemüse",
        "Wurzelgemüse",
        "Blattgemüse",
        "Kräuter",
        "Hülsenfrüchte",
        "Zwiebeln & Lauch",
    )

    val all: List<PlantTemplate> by lazy {
        fruchtgemuese + wurzelgemuese + blattgemuese + kraeuter + huelsenfrueChte + zwiebeln
    }

    fun byCategory(category: String) = all.filter { it.category == category }

    // ── Fruchtgemüse (8) ─────────────────────────────────────────────────────

    private val fruchtgemuese = listOf(
        template(1, "Tomaten", "Voranzucht", "Fruchtgemüse") {
            m(2, VORANZUCHT); m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE)
        },
        template(2, "Zucchini", "Direktsaat", "Fruchtgemüse") {
            m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE); m(8, ERNTE)
        },
        template(3, "Kürbis", "Direktsaat", "Fruchtgemüse") {
            m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(7, ERNTE); m(8, ERNTE, "Ernte ↑"); m(9, ERNTE, "Lager")
        },
        template(4, "Paprika", "Voranzucht", "Fruchtgemüse") {
            m(1, VORANZUCHT); m(2, VORANZUCHT); m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE)
        },
        template(5, "Aubergine", "Voranzucht", "Fruchtgemüse") {
            m(1, VORANZUCHT); m(2, VORANZUCHT); m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE)
        },
        template(6, "Gurke", "Direktsaat", "Fruchtgemüse") {
            m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE); m(8, ERNTE)
        },
        template(7, "Mais", "Voranzucht", "Fruchtgemüse") {
            m(3, VORANZUCHT); m(4, AUSPFLANZEN); m(5, DIREKTSAAT)
            m(7, ERNTE); m(8, ERNTE, "Ernte ↑")
        },
        template(8, "Fenchel", "Direktsaat", "Fruchtgemüse") {
            m(3, DIREKTSAAT); m(4, DIREKTSAAT); m(6, DIREKTSAAT)
            m(6, ERNTE); m(7, ERNTE); m(8, ERNTE, "Ernte ↑"); m(9, ERNTE)
        },
    )

    // ── Wurzelgemüse (6) ─────────────────────────────────────────────────────

    private val wurzelgemuese = listOf(
        template(10, "Möhren", "Direktsaat", "Wurzelgemüse") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(7, ERNTE); m(8, ERNTE, "Ernte ↑"); m(9, ERNTE, "Lager")
        },
        template(11, "Rote Bete", "Direktsaat", "Wurzelgemüse") {
            m(3, DIREKTSAAT); m(4, DIREKTSAAT)
            m(7, ERNTE); m(8, ERNTE, "Ernte ↑"); m(9, ERNTE, "Lager")
        },
        template(12, "Pastinaken", "Direktsaat", "Wurzelgemüse") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(8, ERNTE); m(9, ERNTE, "Ernte ↑"); m(10, ERNTE, "Lager")
        },
        template(13, "Petersilienwurzel", "Direktsaat", "Wurzelgemüse") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(7, ERNTE); m(8, ERNTE, "Ernte ↑"); m(9, ERNTE)
        },
        template(14, "Rettich", "Direktsaat", "Wurzelgemüse") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT); m(7, DIREKTSAAT); m(8, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(9, ERNTE); m(10, ERNTE)
        },
        template(15, "Sellerie", "Voranzucht", "Wurzelgemüse") {
            m(1, VORANZUCHT); m(2, VORANZUCHT); m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(8, ERNTE); m(9, ERNTE, "Ernte ↑"); m(10, ERNTE, "Lager")
        },
    )

    // ── Blattgemüse (6) ──────────────────────────────────────────────────────

    private val blattgemuese = listOf(
        template(20, "Salat", "Direktsaat", "Blattgemüse") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT); m(7, DIREKTSAAT); m(8, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(9, ERNTE); m(10, ERNTE)
        },
        template(21, "Spinat", "Direktsaat", "Blattgemüse") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT); m(7, DIREKTSAAT); m(8, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(9, ERNTE); m(10, ERNTE)
        },
        template(22, "Mangold", "Direktsaat", "Blattgemüse") {
            m(3, DIREKTSAAT); m(4, DIREKTSAAT)
            m(5, ERNTE); m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE); m(9, ERNTE)
        },
        template(23, "Grünkohl", "Direktsaat", "Blattgemüse") {
            m(4, DIREKTSAAT); m(5, DIREKTSAAT)
            m(9, ERNTE); m(10, ERNTE, "Ernte ↑"); m(11, ERNTE); m(0, ERNTE); m(1, ERNTE)
        },
        template(24, "Kohlrabi", "Direktsaat", "Blattgemüse") {
            m(2, VORANZUCHT); m(3, DIREKTSAAT); m(4, DIREKTSAAT); m(6, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(7, ERNTE); m(8, ERNTE)
        },
        template(25, "Pak Choi", "Direktsaat", "Blattgemüse") {
            m(3, DIREKTSAAT); m(7, DIREKTSAAT); m(8, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(9, ERNTE); m(10, ERNTE)
        },
    )

    // ── Kräuter (8) ──────────────────────────────────────────────────────────

    private val kraeuter = listOf(
        template(30, "Basilikum", "Voranzucht", "Kräuter") {
            m(2, VORANZUCHT); m(3, VORANZUCHT); m(4, AUSPFLANZEN)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE); m(8, ERNTE)
        },
        template(31, "Petersilie", "Direktsaat", "Kräuter") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT); m(6, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(6, ERNTE); m(7, ERNTE); m(8, ERNTE); m(9, ERNTE)
        },
        template(32, "Dill", "Direktsaat", "Kräuter") {
            m(3, DIREKTSAAT); m(4, DIREKTSAAT); m(5, DIREKTSAAT)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE)
        },
        template(33, "Schnittlauch", "Direktsaat", "Kräuter") {
            m(2, DIREKTSAAT)
            m(3, ERNTE); m(4, ERNTE); m(5, ERNTE); m(6, ERNTE); m(7, ERNTE); m(8, ERNTE)
        },
        template(34, "Koriander", "Direktsaat", "Kräuter") {
            m(3, DIREKTSAAT); m(4, DIREKTSAAT); m(7, DIREKTSAAT)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(8, ERNTE); m(9, ERNTE)
        },
        template(35, "Thymian", "Direktsaat", "Kräuter") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE)
        },
        template(36, "Rosmarin", "Voranzucht", "Kräuter") {
            m(1, VORANZUCHT); m(2, VORANZUCHT); m(4, AUSPFLANZEN)
            m(5, ERNTE); m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE); m(9, ERNTE)
        },
        template(37, "Minze", "Direktsaat", "Kräuter") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(4, ERNTE); m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE); m(8, ERNTE)
        },
    )

    // ── Hülsenfrüchte (4) ────────────────────────────────────────────────────

    private val huelsenfrueChte = listOf(
        template(40, "Buschbohnen", "Direktsaat", "Hülsenfrüchte") {
            m(4, DIREKTSAAT); m(5, DIREKTSAAT); m(6, DIREKTSAAT)
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE)
        },
        template(41, "Stangenbohnen", "Direktsaat", "Hülsenfrüchte") {
            m(4, DIREKTSAAT); m(5, DIREKTSAAT)
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑"); m(8, ERNTE); m(9, ERNTE)
        },
        template(42, "Erbsen", "Direktsaat", "Hülsenfrüchte") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE)
        },
        template(43, "Dicke Bohnen", "Direktsaat", "Hülsenfrüchte") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(5, ERNTE); m(6, ERNTE, "Ernte ↑"); m(7, ERNTE)
        },
    )

    // ── Zwiebeln & Lauch (5) ─────────────────────────────────────────────────

    private val zwiebeln = listOf(
        template(50, "Knoblauch", "Überwinternd", "Zwiebeln & Lauch") {
            m(0, PFLEGE); m(1, PFLEGE); m(2, PFLEGE); m(3, PFLEGE); m(4, PFLEGE)
            m(5, ERNTE, "Ernte ↑")
            m(9, DIREKTSAAT, "Setzen"); m(10, DIREKTSAAT, "Setzen")
        },
        template(51, "Zwiebeln", "Direktsaat", "Zwiebeln & Lauch") {
            m(2, DIREKTSAAT); m(3, DIREKTSAAT)
            m(7, ERNTE); m(8, ERNTE, "Ernte ↑"); m(9, ERNTE, "Lager")
        },
        template(52, "Porree", "Voranzucht", "Zwiebeln & Lauch") {
            m(1, VORANZUCHT); m(2, VORANZUCHT); m(3, AUSPFLANZEN); m(4, AUSPFLANZEN)
            m(8, ERNTE); m(9, ERNTE); m(10, ERNTE, "Ernte ↑"); m(11, ERNTE); m(0, ERNTE)
        },
        template(53, "Schalotten", "Direktsaat", "Zwiebeln & Lauch") {
            m(2, DIREKTSAAT, "Setzen"); m(3, DIREKTSAAT, "Setzen")
            m(6, ERNTE); m(7, ERNTE, "Ernte ↑")
        },
        template(54, "Bärlauch", "Direktsaat", "Zwiebeln & Lauch") {
            m(8, DIREKTSAAT); m(9, DIREKTSAAT)
            m(2, ERNTE); m(3, ERNTE, "Ernte ↑"); m(4, ERNTE)
        },
    )

    // ── Builder-Helper ────────────────────────────────────────────────────────

    private fun template(
        id: Int,
        name: String,
        subtitle: String,
        category: String,
        block: MutableList<MonthEntryTemplate>.() -> Unit,
    ): PlantTemplate {
        val months = MutableList<MonthEntryTemplate?>(12) { null }
        val entries = mutableListOf<MonthEntryTemplate>().apply(block)
        entries.forEach { months[it.month] = it }
        return PlantTemplate(id = id, name = name, subtitle = subtitle,
                             category = category, months = months)
    }

    private fun MutableList<MonthEntryTemplate>.m(
        month: Int,
        type: ActivityType,
        label: String = type.defaultLabel,
    ) = add(MonthEntryTemplate(month = month, type = type, label = label))
}
