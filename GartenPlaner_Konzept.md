# GartenPlaner — App-Konzept

> Jahresplaner für Hobbygärtner und Selbstversorger.
> Pflanzenbibliothek, druckfertige PDF-Pläne, Erinnerungen — direkt auf Android.

---

## Vision

Eine minimalistische Android-App, mit der Hobbygärtner und Selbstversorger —
besonders Einsteiger — einen übersichtlichen Jahresplan für Aussaat, Pflege und
Ernte erstellen. Die App nimmt die größte Hürde ab: den Anfang. Wer weiß wann
was zu tun ist, hat mehr Erfolge, bleibt motiviert, und kann sich auf das
Wesentliche konzentrieren: beobachten, lernen, verstehen.

Mit einem Tipp wird der Plan als schöne PDF exportiert oder ausgedruckt —
zum Aufhängen, zum Teilen mit dem Partner, zum Abhaken.

Kein Account, kein Server, keine Cloud. Offline-first, Open Source, F-Droid-kompatibel.

---

## Zielgruppe

- Hobbygärtner und Selbstversorger, besonders Einsteiger
- Schrebergärtner
- Alle die ernsthaft Gemüse anbauen wollen — nicht nur ein paar Töpfe
- F-Droid-Nutzer: technikaffin, Open-Source-bewusst, jünger
- Menschen die ein Planungsbuch kennen, aber lieber ihr Handy nutzen

---

## Das Problem das gelöst wird

Der Anfang ist die größte Hürde im Gemüseanbau. Wer nicht weiß wann was zu tun
ist, macht Fehler, hat weniger Erfolge und hört auf. Ein guter Plan am
Jahresanfang nimmt diese Unsicherheit raus. Die App ersetzt das Planungsbuch —
aber mit vorausgefüllten Standardwerten, Benachrichtigungen, und einem
ausdruckbaren Ergebnis das an die Wand gehängt werden kann.

---

## Tech-Stack

| Bereich | Entscheidung | Begründung |
|---|---|---|
| Sprache | Kotlin | Standard-Android |
| UI | Jetpack Compose | Moderner Ansatz, passt zu Flowwalker-Stack |
| Datenspeicherung | Room (SQLite) | Lokal, robust, kein Internet nötig |
| PDF-Export | WebView + Android PrintManager | Keine externe Abhängigkeit, Chromium-Qualität |
| HTML-Generierung | Kotlin String-Template | Direkte Kontrolle über Print-Output |
| Min SDK | 26 (Android 8) | Breite Kompatibilität, PrintManager stabil |
| Lizenz | GPL-3.0 | F-Droid-kompatibel |
| Distribution | F-Droid → Play Store | Open Source zuerst, Play Store als Vorbereitung für Flowwalker |

---

## Datenmodell

```
Plan
├── id: Int
├── year: Int                        // z.B. 2026
├── title: String                    // z.B. "Garten Brandenburg"
├── frostInfoLast: String            // z.B. "~15. April"
├── frostInfoFirst: String           // z.B. "~15. Oktober"
├── climateZone: String              // z.B. "7a · Lehmboden"
└── sections: List<Section>

Section
├── id: Int
├── title: String                    // z.B. "🥬 Gemüse & Kräuter"
├── order: Int
└── plants: List<Plant>

Plant
├── id: Int
├── name: String                     // z.B. "Tomaten"
├── subtitle: String                 // z.B. "Voranzucht" (optional)
├── order: Int
├── fromLibrary: Boolean             // aus Pflanzenbibliothek oder manuell?
└── months: List<MonthEntry?>        // 12 Einträge, null = leer

MonthEntry
├── type: ActivityType               // Enum
└── label: String                    // z.B. "Voranz." oder "Ernte ↑"

ActivityType (Enum, fix v1.0)
├── VORANZUCHT      // gelb
├── DIREKTSAAT      // grün
├── AUSPFLANZEN     // blau
├── ERNTE           // rot
└── PFLEGE          // lila

PlantTemplate (Pflanzenbibliothek, read-only)
├── id: Int
├── name: String
├── subtitle: String
├── category: String                 // z.B. "Fruchtgemüse", "Wurzelgemüse"
└── months: List<MonthEntry?>        // vorausgefüllte Standardwerte
```

---

## Screens & Navigation

```
MainActivity
└── NavHost
    ├── PlanScreen           (Hauptansicht)
    ├── EditPlantScreen      (Pflanze anlegen / bearbeiten)
    ├── PlantPickerScreen    (Aus Bibliothek wählen)
    ├── EditSectionScreen    (Section anlegen / umbenennen)
    └── SettingsScreen       (Plan-Metadaten: Titel, Jahr, Frostdaten)
```

---

## Screen 1: PlanScreen (Hauptansicht)

**Layout:** Scrollbare Liste der Sections mit ihren Pflanzenzeilen.
Die Monatsübersicht wird als horizontaler Chip-Strip pro Zeile dargestellt —
kein zweidimensionales Scrollen, das auf Mobile eine UX-Katastrophe wäre.

**TopAppBar:**
- Links: `[⚙]` → SettingsScreen
- Mitte: Plan-Titel + Jahr
- Rechts: `[✏]` Bearbeitungsmodus | `[🖨]` Export/Drucken

**Bearbeitungsmodus:**
- Drag-Handle erscheint links → Drag & Drop zum Umsortieren
- Löschen-Icon erscheint rechts neben jeder Pflanze
- Section-Header zeigen Löschen-Icon (mit Bestätigungsdialog)
- FAB wechselt zu "+ Section"

**Normalmodus:**
- Tap auf Pflanzennamen → EditPlantScreen
- FAB (+) unten rechts → Auswahl: "Aus Bibliothek wählen" | "Manuell anlegen" | "Section hinzufügen"

**Monatsdarstellung pro Pflanze:**
```
[Jan][Feb][Mär][Apr Voranz.][Mai Auspfl.][Jun][Jul Ernte][Aug Ernte↑][Sep][Okt][Nov][Dez]
```
Belegte Monate: Farbe + Label. Leere Monate: kleines graues Kästchen.
Tap auf Monat-Chip öffnet direkt diesen Monat im EditPlantScreen.

---

## Screen 2: PlantPickerScreen (Pflanzenbibliothek)

Öffnet sich wenn der User "Aus Bibliothek wählen" tippt.

- Suchfeld oben
- Kategorien als horizontale Filter-Chips (Fruchtgemüse, Wurzelgemüse, Blattgemüse, Kräuter, Hülsenfrüchte, Zwiebeln...)
- Liste der Pflanzen mit Name und Vorschau der belegten Monate
- Tap auf Pflanze → Standardwerte werden übernommen, EditPlantScreen öffnet sich zur Kontrolle/Anpassung

---

## Screen 3: EditPlantScreen

Öffnet sich beim manuellen Anlegen, nach Bibliotheksauswahl, oder beim Tap auf Pflanzennamen.

- Textfeld: Pflanzname
- Textfeld: Untertitel (optional)
- Dropdown: Section zuweisen
- 12 Monats-Buttons in zwei Reihen (Jan–Jun, Jul–Dez)
  - Belegt: farbig mit Kürzel
  - Leer: grau
- Tap auf Monats-Button → Bottom Sheet:
  - Radio-Buttons für Aktivitätstyp (mit Farb-Dot)
  - Textfeld für Label (vorausgefüllt mit Default)
  - Option "Leer / nichts"
  - Button "Übernehmen"

Default-Labels:
- VORANZUCHT → "Voranz."
- DIREKTSAAT → "Direktsaat"
- AUSPFLANZEN → "Auspfl."
- ERNTE → "Ernte"
- PFLEGE → "Pflege"

---

## Screen 4: EditSectionScreen

Nur ein Textfeld für den Section-Namen (inkl. Emoji). Speichern oben rechts.

---

## Screen 5: SettingsScreen

- Plan-Titel
- Jahr
- Letzter Spätfrost
- Erster Herbstfrost
- Klimazone / Bodeninfo (optional)

Diese Werte fließen in den PDF-Header.

---

## PDF-Export

1. User tippt Drucker-Icon in der TopAppBar
2. App generiert HTML-String aus aktuellem Plan-State
3. Unsichtbarer WebView lädt das HTML
4. `webView.createPrintDocumentAdapter()` wird aufgerufen
5. Android-Druckdialog öffnet sich
6. User wählt: Drucker oder "Als PDF speichern"

HTML-Generierung via `fun planToHtml(plan: Plan): String`:
- Print-CSS mit `print-color-adjust: exact` global
- HTML `<table>` statt CSS Grid (stabiler im Print-Engine)
- `@page { size: 297mm 210mm; margin: 0; }`
- Explizite Zeilenhöhen in mm

**Referenz-Template:** Die Datei `gartenplaner_2026.html` im Projektordner ist das
fertige, getestete HTML-Template das als direkte Vorlage für `HtmlExporter.kt` dient.
Struktur, CSS-Regeln, Farbwerte und Tabellenaufbau sollen exakt übernommen und
dynamisch mit den Plan-Daten befüllt werden. Das Template ist bereits print-getestet
und produziert korrekte A4-Querformat-PDFs über den Android PrintManager.

Keine externen Libraries. Vollständig F-Droid-kompatibel.

---

## Pflanzenbibliothek

Vorinstallierte Datenbank mit typischen Gemüsesorten für mitteleuropäische Gärten.
Kategorien: Fruchtgemüse, Wurzelgemüse, Blattgemüse, Kräuter, Hülsenfrüchte, Zwiebeln/Lauch.

Jede Pflanze hat Standardwerte für alle relevanten Monate — Voranzucht, Direktsaat,
Auspflanzen, Ernte. Der User kann diese nach dem Hinzufügen beliebig anpassen.

Die Bibliothek ist read-only in v1.0. Community-Erweiterungen möglich in späteren Versionen.

---

## Versionierung

### v1.0 — Jahresplaner (Kern)
- Ein aktiver Plan
- Sections frei erstell- und löschbar
- Pflanzenbibliothek mit Standardwerten
- Pflanzen manuell anlegen oder aus Bibliothek wählen
- Bearbeiten, löschen, umsortieren per Drag & Drop
- 5 fix definierte Aktivitätstypen
- PDF-Export via Android PrintManager
- Lokale Speicherung (Room)
- Deutsch, Dark Mode (Material You)

### v1.1 — Benachrichtigungen
- Tägliche Erinnerung zu frei wählbarer Uhrzeit
- Wöchentliche Zusammenfassung: "Diese Woche steht an..."
- Benachrichtigung wenn ein Aktivitätszeitraum beginnt

### v2.0 — Beetplaner
- Visuelles Raster das das Beet abbildet
- Fruchtfolge-Hinweise
- Mischkultur-Empfehlungen
- Inspiration: klassische Planungsbücher für Selbstversorger

### v3.0 — Zuchtplaner
- Dokumentation von Zuchtprojekten (Sortenkreuzungen, Generationen)
- Entwicklungsfotos und Notizen pro Pflanze
- Zeitstrahl pro Zuchtlinie
- Generisch einsetzbar, nicht auf eine Pflanzenart beschränkt

---

## Distribution

| Kanal | Version | Anforderungen |
|---|---|---|
| GitHub | v1.0 | Quellcode + Releases |
| F-Droid | v1.0 | GPL-3.0, keine proprietären Libs ✓ |
| Play Store | v1.1 | Signed APK, Store-Listing, Screenshots |

F-Droid zuerst: niedrigere Hürde, früheres Community-Feedback.
Play Store als zweiter Schritt — Vorbereitung für Flowwalker-Launch.

---

## Projektstruktur (Kotlin/Compose)

```
gartenplaner/
├── data/
│   ├── db/           Room-Database, DAOs
│   ├── model/        Plan, Section, Plant, MonthEntry, ActivityType, PlantTemplate
│   ├── library/      Pflanzenbibliothek (statische Daten)
│   └── repository/   PlanRepository, LibraryRepository
├── ui/
│   ├── plan/         PlanScreen + ViewModel
│   ├── editplant/    EditPlantScreen + ViewModel
│   ├── plantpicker/  PlantPickerScreen + ViewModel
│   ├── editsection/  EditSectionScreen
│   ├── settings/     SettingsScreen
│   └── theme/        MaterialTheme, Colors, Typography
├── export/
│   └── HtmlExporter.kt
└── MainActivity.kt
```

---

## Offene Fragen / Community-Feedback-Punkte

- Mehrere Pläne / Jahreswechsel mit Plan-Kopie?
- JSON-Backup und Restore?
- Englische Lokalisierung?
- Landscape-Ansatz auf Tablet?
- Pflanzenbibliothek durch Community erweiterbar?
- Eigene Aktivitätstypen definieren?
