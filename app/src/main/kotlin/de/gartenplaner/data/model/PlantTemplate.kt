package de.gartenplaner.data.model

/**
 * Read-only Vorlage aus der Pflanzenbibliothek.
 * Wird nicht in Room gespeichert — nur im Speicher als statische Liste.
 */
data class PlantTemplate(
    val id: Int,
    val name: String,
    val subtitle: String = "",
    val category: String,
    /** 12 Einträge (Index = Monat 0–11), null = kein Eintrag für diesen Monat */
    val months: List<MonthEntryTemplate?>,
)

/** Monats-Eintrag innerhalb eines Templates (ohne plantId / id) */
data class MonthEntryTemplate(
    val month: Int,
    val type: ActivityType,
    val label: String = type.defaultLabel,
)
