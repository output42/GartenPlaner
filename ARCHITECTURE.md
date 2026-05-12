# GartenPlaner — Technische Architektur

## Überblick

GartenPlaner ist eine offline-first Android-App für Hobbygärtner und Selbstversorger.
Kernfunktion: einen jahresbasierten Aussaat- und Ernteplaner erstellen, bearbeiten und als druckfertiges A4-PDF exportieren.

**Prinzipien:** Kein Server. Kein Account. Keine externen Libraries. GPL-3.0. F-Droid-kompatibel.

---

## Tech-Stack

| Schicht | Technologie | Begründung |
|---|---|---|
| Sprache | Kotlin | Standardsprache auf Android |
| UI | Jetpack Compose | Deklarativ, wartbar, Material You |
| Datenbank | Room (SQLite) | Lokal, typsicher, kein Internet |
| PDF-Export | Android `PrintManager` + `WebView` | Keine externe Lib, Chromium-Qualität |
| HTML-Generierung | Kotlin String-Templates | Direkte Kontrolle, keine Abhängigkeit |
| Min SDK | 26 (Android 8.0) | `PrintManager` stabil, breite Gerätebasis |
| Lizenz | GPL-3.0 | F-Droid-Anforderung erfüllt |

---

## Datenmodell

```
Plan
├── id: Int (PrimaryKey, autoGenerate)
├── year: Int                      // z.B. 2026
├── title: String                  // z.B. "Garten Brandenburg"
├── frostInfoLast: String          // z.B. "~15. April"
├── frostInfoFirst: String         // z.B. "~15. Oktober"
└── climateZone: String            // z.B. "7a · Lehmboden"

Section
├── id: Int (PrimaryKey, autoGenerate)
├── planId: Int (ForeignKey → Plan.id)
├── title: String                  // z.B. "🥬 Gemüse & Kräuter"
└── order: Int

Plant
├── id: Int (PrimaryKey, autoGenerate)
├── sectionId: Int (ForeignKey → Section.id)
├── name: String                   // z.B. "Tomaten"
├── subtitle: String               // z.B. "Voranzucht" (optional)
├── order: Int
└── fromLibrary: Boolean

MonthEntry
├── id: Int (PrimaryKey, autoGenerate)
├── plantId: Int (ForeignKey → Plant.id)
├── month: Int                     // 0=Jan … 11=Dez
├── type: ActivityType             // Enum, gespeichert als String
└── label: String                  // z.B. "Voranz.", "Ernte ↑"

ActivityType (Enum)
├── VORANZUCHT   → Farbe: Amber   #f0a500 / #fff0c0
├── DIREKTSAAT   → Farbe: Grün    #3a8c3f / #c8ecd0
├── AUSPFLANZEN  → Farbe: Blau    #2196f3 / #c5e0ff
├── ERNTE        → Farbe: Rot     #c0392b / #fad0d3
└── PFLEGE       → Farbe: Lila    #8e44ad / #e8d0f8

PlantTemplate (read-only Pflanzenbibliothek)
├── id: Int
├── name: String
├── subtitle: String
├── category: String               // "Fruchtgemüse", "Wurzelgemüse", …
└── months: List<MonthEntry?>      // Standardwerte, null = leer
```

### Datenbankrelationen

```
Plan 1──n Section 1──n Plant 1──n MonthEntry
```

Room-Database: `GardenDatabase` mit einer einzigen Datenbankdatei `gartenplaner.db`.
Migrationen ab v1.0 versioniert, kein Datenverlust bei Updates.

---

## Projektstruktur (Android-Modul)

```
app/src/main/
├── data/
│   ├── db/
│   │   ├── GardenDatabase.kt          Room-Database-Klasse
│   │   ├── PlanDao.kt
│   │   ├── SectionDao.kt
│   │   ├── PlantDao.kt
│   │   └── MonthEntryDao.kt
│   ├── model/
│   │   ├── Plan.kt
│   │   ├── Section.kt
│   │   ├── Plant.kt
│   │   ├── MonthEntry.kt
│   │   ├── ActivityType.kt            Enum mit Farb- und Label-Defaults
│   │   └── PlantTemplate.kt
│   ├── library/
│   │   └── PlantLibrary.kt            Statische Liste von PlantTemplate-Objekten
│   └── repository/
│       ├── PlanRepository.kt          Aggregiert DAOs, Flow-basierte API
│       └── LibraryRepository.kt       Filtert/sucht PlantTemplates
├── ui/
│   ├── plan/
│   │   ├── PlanScreen.kt
│   │   └── PlanViewModel.kt
│   ├── editplant/
│   │   ├── EditPlantScreen.kt
│   │   └── EditPlantViewModel.kt
│   ├── plantpicker/
│   │   ├── PlantPickerScreen.kt
│   │   └── PlantPickerViewModel.kt
│   ├── editsection/
│   │   └── EditSectionScreen.kt       (kein eigenes ViewModel nötig)
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Typography.kt
├── export/
│   └── HtmlExporter.kt                planToHtml(plan, sections, plants)
└── MainActivity.kt
```

---

## Navigation

```
MainActivity
└── NavHost (Scaffold mit BottomNavigation)
    ├── Tab: Plan
    │   └── PlanScreen
    │       ├── → EditPlantScreen (plantId oder neu)
    │       ├── → PlantPickerScreen
    │       └── → EditSectionScreen (sectionId oder neu)
    ├── Tab: Bibliothek
    │   └── PlantPickerScreen (standalone)
    └── Tab: Einstellungen
        └── SettingsScreen
```

Bottom-Navigation-Tabs: **Plan · Bibliothek · Einstellungen**

Compose Navigation mit `NavController` und typed Route-Klassen (Kotlin Serialization oder sealed class).
Back-Stack wird von Compose Navigation verwaltet — kein manuelles Stack-Management.

---

## State-Management

Jedes Feature-Paket folgt dem Pattern:

```
Screen (Composable)
  └── collectAsStateWithLifecycle(ViewModel.uiState)
        └── ViewModel (StateFlow<UiState>)
              └── Repository (Flow<Entity>)
                    └── DAO (Room)
```

- ViewModels halten **keinen** Navigation-State
- Side-Effects (Speichern, PDF-Export, Navigation) über `ViewModel.events: SharedFlow`
- `UiState` ist eine sealed interface mit `Loading`, `Success`, `Error`

---

## PDF-Export-Pipeline

```
1. User tippt 🖨-Icon (TopAppBar in PlanScreen)
2. PlanViewModel.exportPdf() wird aufgerufen
3. HtmlExporter.planToHtml(plan) → String (vollständiges HTML-Dokument)
4. Unsichtbarer WebView (in MainActivity gehalten) lädt den HTML-String
5. webView.createPrintDocumentAdapter("GartenPlan_${plan.year}.pdf") wird aufgerufen
6. Android PrintManager.print() öffnet System-Druckdialog
7. User wählt: physischer Drucker oder „Als PDF speichern" → beliebiger Speicherort
```

### HtmlExporter

`fun planToHtml(plan: Plan, sections: List<SectionWithPlants>): String`

Das Referenz-Template `gartenplaner_2026.html` definiert die Ziel-Struktur:
- `@page { size: 297mm 210mm; margin: 0; }` — A4 Querformat
- `print-color-adjust: exact` global — Farben werden nicht vom Browser wegoptimiert
- HTML `<table>` mit `table-layout: fixed` und expliziten `col`-Breiten — stabiler als CSS Grid im Print-Engine
- Explizite `height`-Werte in mm für alle Zeilen (Druckpräzision)
- Fußzeile mit Plan-Metadaten (Frostdaten, Klimazone)

### Farbwerte (CSS ↔ ActivityType)

| ActivityType | Zell-Hintergrund | Text |
|---|---|---|
| VORANZUCHT | `#fff0c0` | `#7a4f00` |
| DIREKTSAAT | `#c8ecd0` | `#145220` |
| AUSPFLANZEN | `#c5e0ff` | `#00387a` |
| ERNTE | `#fad0d3` | `#6e1219` |
| PFLEGE | `#e8d0f8` | `#3d1060` |

---

## Pflanzenbibliothek

`PlantLibrary.kt` ist eine **statische Kotlin-Datei** mit `List<PlantTemplate>`.
Keine externe Datenbank, kein Asset-File — direkt im Code für maximale Compile-Time-Sicherheit und einfache F-Droid-Builds.

### Kategorien

- Fruchtgemüse (Tomaten, Zucchini, Kürbis, Mais, ...)
- Wurzelgemüse (Möhren, Rote Bete, Pastinaken, ...)
- Blattgemüse (Salat, Spinat, Mangold, ...)
- Kräuter (Basilikum, Petersilie, Dill, ...)
- Hülsenfrüchte (Bohnen, Erbsen, ...)
- Zwiebeln & Lauch (Knoblauch, Zwiebeln, Porree, ...)

Jedes `PlantTemplate` enthält vorausgefüllte `MonthEntry`-Standardwerte für mitteleuropäische Gärten (Klimazone 7a als Baseline).

Der User kann nach dem Hinzufügen alle Monate beliebig überschreiben.

---

## UI-Design-System

Basis: **Material You** (Material 3) mit angepasster Farbpalette.

### Farbpalette (App-UI)

| Token | Light | Dark |
|---|---|---|
| Primary | `#2a6a2a` | `#6dcc6d` |
| Background | `#f2f4f0` | `#111111` |
| Surface | `#ffffff` | `#1e1e1e` |
| Border | `#e0e4dc` | `#2e2e2e` |
| Text primary | `#111a11` | `#f0f0f0` |
| Text secondary | `#4a5c4a` | `#a0a0a0` |

### Typografie

- UI-Schrift: **Nunito** (eingebettet als Compose-Font-Resource)
- Monospace: **DM Mono** (für Monatsküzel in der Monatsleiste)

### Aktivitätsfarben (App-UI, Chips)

| Typ | Chip-Farbe | Text |
|---|---|---|
| VORANZUCHT | `#e09a00` (amber) | `#2a1800` |
| DIREKTSAAT | `#3d9e3d` (green-500) | `#ffffff` |
| AUSPFLANZEN | `#3a8fd4` (blue) | `#ffffff` |
| ERNTE | `#d44a4a` (red) | `#ffffff` |
| PFLEGE | `#8a5cd0` (purple) | `#ffffff` |

---

## Screens — Zusammenfassung

### PlanScreen
- `LazyColumn` mit Section-Headern und Pflanzenzeilen
- Pro Pflanze: horizontaler `Row` mit 12 gleichbreiten Monats-Chips (`weight(1f)`)
- TopAppBar: Titel/Jahr links, Edit-Icon + Druck-Icon rechts
- Bearbeitungsmodus: Drag-Handle (Reorder) + Löschen-Icon per Pflanze
- FAB: kontextsensitiv (Normal: Pflanze/Section hinzufügen; Edit: Section hinzufügen)

### EditPlantScreen
- Textfeld Pflanzname, Textfeld Untertitel, Dropdown Section
- 6×2 Monats-Grid (Jan–Jun / Jul–Dez)
- Tap auf Monat → `ModalBottomSheet`:
  - Radio-Buttons für Aktivitätstyp (Farbpunkt + Label)
  - Optionales Notiz-Textfeld (nur bei PFLEGE sichtbar)
  - „Leer / nichts"-Option
  - Übernehmen-Button

### PlantPickerScreen
- Suchfeld + horizontale Kategorie-Filter-Chips
- Liste: Pflanzenname + Untertitel + Mini-Monatsleiste (5px-Dots)
- Tap → übernimmt Standardwerte, öffnet EditPlantScreen zur Kontrolle

### EditSectionScreen
- Einzelnes Textfeld mit Emoji-Tastatur-Unterstützung
- Speichern in TopAppBar

### SettingsScreen
- Gruppierte Einstellungsliste (Material 3 `ListItem`)
- Gruppen: Mein Plan (Titel, Jahr), Klima & Standort (Frostdaten, Klimazone), Export, App-Info

---

## Versionierungsplan

### v1.0 — Kern (Ziel: F-Droid-Release)
- Ein aktiver Plan pro App
- Sections und Pflanzen frei erstellbar, löschbar, umsortierbar (Drag & Drop)
- Pflanzenbibliothek (statisch, ~40 Einträge)
- 5 Aktivitätstypen (fix)
- PDF-Export via Android PrintManager
- Room-Datenbank, vollständig offline
- Deutsch, Light + Dark Mode (Material You)

### v1.1 — Benachrichtigungen
- `WorkManager`-basierte Hintergrundaufgaben
- Tägliche Erinnerung (konfigurierbare Uhrzeit)
- Wöchentliche Zusammenfassung: "Diese Woche steht an..."
- Push wenn ein Aktivitätszeitraum beginnt

### v2.0 — Beetplaner
- Visuelles Raster (Gitterplan) für Beetflächen
- Fruchtfolge-Hinweise basierend auf Pflanzenfamilien
- Mischkultur-Empfehlungen

### v3.0 — Zuchtplaner
- Generationsdokumentation für Zuchtprojekte
- Fotos und Notizen pro Pflanze und Generation
- Zeitstrahl pro Zuchtlinie

---

## Verteilung

| Kanal | Version | Voraussetzungen |
|---|---|---|
| GitHub Releases | v1.0 | Signiertes APK + Quellcode |
| F-Droid | v1.0 | GPL-3.0, keine proprietären Libs ✓ |
| Google Play Store | v1.1 | Store-Listing, Screenshots, Privacy Policy |

F-Droid-Priorität: niedrigere Einstiegshürde, schnelles Community-Feedback.
Play Store als zweiter Schritt — parallel zur Vorbereitung des Flowwalker-Launches.

---

## Referenz-Dateien

| Datei | Zweck |
|---|---|
| `gartenplaner_2026.html` | Fertiges, print-getestetes PDF-Template → direkte Vorlage für `HtmlExporter.kt` |
| `GartenPlaner_Mockups.html` | Visuelle Mockups aller Screens (Light + Dark) |
| `GartenPlaner_Konzept.md` | Produkt-Konzept, Vision, Zielgruppe, offene Fragen |

---

## Offene Entscheidungen (v1.0)

- Mehrere Pläne pro App oder strikt ein aktiver Plan?
- JSON-Backup/Restore über Android SAF?
- Englische Lokalisierung ab v1.0 oder erst v1.1?
- Pflanzenbibliothek erweiterbar durch User (manuell importierbare JSON-Templates)?
