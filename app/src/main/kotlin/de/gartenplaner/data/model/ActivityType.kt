package de.gartenplaner.data.model

/**
 * Die fünf fix definierten Aktivitätstypen für v1.0.
 *
 * Trägt alle kontextspezifischen Werte direkt — kein when/switch
 * in UI-Schicht oder HtmlExporter nötig.
 *
 * Farbwerte als Long (ARGB) statt Compose-Color, damit die
 * Datenschicht keine Compose-Abhängigkeit hat. UI-Erweiterung
 * via `ActivityType.chipColor` (Extension in ui/theme).
 */
enum class ActivityType(
    val defaultLabel: String,
    /** ARGB-Farbwert für App-UI-Chips */
    val colorArgb: Long,
    /** ARGB-Farbwert für den Chip-Text */
    val textArgb: Long,
    /** CSS-Klasse im PDF-Export (aus gartenplaner_2026.html) */
    val cssClass: String,
    /** CSS-Hintergrundfarbe für PDF-Zellen */
    val cssBackground: String,
    /** CSS-Textfarbe für PDF-Zellen */
    val cssText: String,
) {
    VORANZUCHT(
        defaultLabel  = "Voranz.",
        colorArgb     = 0xFFE09A00L,
        textArgb      = 0xFF2A1800L,
        cssClass      = "av",
        cssBackground = "#fff0c0",
        cssText       = "#7a4f00",
    ),
    DIREKTSAAT(
        defaultLabel  = "Direktsaat",
        colorArgb     = 0xFF3D9E3DL,
        textArgb      = 0xFFFFFFFFL,
        cssClass      = "ad",
        cssBackground = "#c8ecd0",
        cssText       = "#145220",
    ),
    AUSPFLANZEN(
        defaultLabel  = "Auspfl.",
        colorArgb     = 0xFF3A8FD4L,
        textArgb      = 0xFFFFFFFFL,
        cssClass      = "ap",
        cssBackground = "#c5e0ff",
        cssText       = "#00387a",
    ),
    ERNTE(
        defaultLabel  = "Ernte",
        colorArgb     = 0xFFD44A4AL,
        textArgb      = 0xFFFFFFFFL,
        cssClass      = "ae",
        cssBackground = "#fad0d3",
        cssText       = "#6e1219",
    ),
    PFLEGE(
        defaultLabel  = "Pflege",
        colorArgb     = 0xFF8A5CD0L,
        textArgb      = 0xFFFFFFFFL,
        cssClass      = "apg",
        cssBackground = "#e8d0f8",
        cssText       = "#3d1060",
    ),
}
