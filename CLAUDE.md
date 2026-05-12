# GartenPlaner — Claude Code Instructions

Offline-first Android-App für jahresbasierte Gartenplanung mit PDF-Export.
**Kein Server. Kein Account. Keine externen Libraries. GPL-3.0. F-Droid-kompatibel.**

## Befehle

```bash
# Projekt bauen
./gradlew assembleDebug

# Release-Build
./gradlew assembleRelease

# JVM-Unit-Tests (kein Gerät nötig)
./gradlew test

# Instrumentierungstests (braucht Emulator/Gerät)
./gradlew connectedAndroidTest

# Bestimmte Testklasse
./gradlew test --tests "de.gartenplaner.data.library.PlantLibraryTest"

# Lint
./gradlew lint
```

> Gradle-Wrapper-JAR fehlt im Repo (Binärdatei). Beim ersten Import in
> Android Studio automatisch erzeugt, oder manuell: `gradle wrapper --gradle-version 8.7`

## Projektstruktur (Kurzreferenz)

```
app/src/main/kotlin/de/gartenplaner/
├── data/
│   ├── model/          — Room-Entities + PlantTemplate (kein Compose-Import hier)
│   ├── db/             — DAOs, GardenDatabase, Converters
│   ├── repository/     — PlanRepository, LibraryRepository
│   ├── library/        — PlantLibrary (statisches Kotlin-Objekt, 37+ Einträge)
│   ├── backup/         — PlanExporter, PlanImporter (org.json, kein Extra-Dep)
│   ├── prefs/          — AppPreferences (SharedPreferences, activePlanId)
│   └── seed/           — DemoSeed (läuft einmalig bei onCreate)
├── export/             — HtmlExporter (→ WebView → PrintManager)
├── navigation/         — Screen.kt (alle Routen als sealed class)
├── ui/
│   ├── theme/          — Color.kt, Theme.kt, Typography.kt
│   ├── planlist/       — PlanListScreen + ViewModel
│   ├── plan/           — PlanScreen + ViewModel + components/
│   ├── editplant/      — EditPlantScreen + ViewModel + components/
│   ├── editsection/    — EditSectionScreen (kein eigenes ViewModel)
│   ├── plantpicker/    — PlantPickerScreen + ViewModel + components/
│   └── settings/       — SettingsScreen + ViewModel
└── MainActivity.kt     — NavHost + WebView-Holder für PrintManager
```

## Unveränderliche Invarianten

- **Kein INTERNET-Permission** in AndroidManifest.xml — nie hinzufügen
- **Kein `androidx.compose.ui.graphics.Color`** in der Datenschicht (`data/model/`, `data/db/`) — stattdessen `colorArgb: Long`; UI macht `Color(entry.type.colorArgb)`
- **Keine externen Libs** (kein Retrofit, kein Gson, kein Coil, kein Hilt) — `org.json` für JSON (AOSP, immer verfügbar)
- **Keine Google Play Services** — F-Droid verbietet sie
- **Fonts als .ttf** in `res/font/` — nicht über `ui-text-google-fonts` laden
- **Alle Strings in `strings.xml`** — kein Hardcoding in Compose-Dateien
- **`@Upsert`** statt separater Insert+Update-Methoden in DAOs
- **Cascade-Delete** läuft über Room ForeignKey — kein manuelles Löschen in Repositories
- **`SharingStarted.WhileSubscribed(5_000)`** in allen StateFlow-Definitionen

## Datenmodell (Überblick)

```
Plan → Section → Plant → MonthEntry
```
Alle Fremdschlüssel mit `CASCADE DELETE`. `planId` ist Pflicht-Argument für alle plan-kontextuellen Screens (kein globaler "aktiver Plan" in der UI — nur in SharedPreferences für Schnellstart).

## ActivityType — CSS-Klassen für HtmlExporter

| Enum | cssClass | Hintergrund | Text |
|---|---|---|---|
| VORANZUCHT | `av` | `#fff0c0` | `#7a4f00` |
| DIREKTSAAT | `ad` | `#c8ecd0` | `#145220` |
| AUSPFLANZEN | `ap` | `#c5e0ff` | `#00387a` |
| ERNTE | `ae` | `#fad0d3` | `#6e1219` |
| PFLEGE | `apg` | `#e8d0f8` | `#3d1060` |

## Implementierungssessions (TODO-Tracker)

| Session | Status | Was fehlt noch |
|---|---|---|
| S1 Gradle + Theme | Struktur ✓, Fonts fehlen | `.ttf`-Dateien in `res/font/` ablegen (siehe `FONTS.md`) |
| S2 Room | Struktur ✓ | Gradle-Wrapper-JAR → DAO-Tests grün |
| S3 Repositories | Struktur ✓ | PlantLibraryTest grün |
| S4 Navigation | Struktur ✓ | Alle Screens im Emulator erreichbar |
| S5 PlanListScreen | TODO-Marker | `PlanCard`, `CreatePlanDialog` vollständig implementieren |
| S6 EditPlantScreen | TODO-Marker | Section-Dropdown, Template-Übernahme via savedStateHandle |
| S7 PlanScreen CRUD | TODO-Marker | Edit-Mode, Delete, Drag & Drop (ReorderableLazyColumn) |
| S8 PlantPicker + EditSection | Struktur ✓ | Integration testen |
| S9 SettingsScreen | TODO-Marker | Alle Dialoge (Titel, Jahr, Frost, ClimateZone, CopyPlan, DeletePlan) |
| S10 HtmlExporter + PDF | Struktur ✓ | `MainActivity.triggerPrint()` verdrahten |
| S11 JSON-Backup + Release | TODO-Marker | SAF-Intent, PlanExporter/Importer verdrahten, F-Droid-Metadaten |

## Navigationsfluss

```
PlanListScreen (Start)
  └─ Plan/{planId}
       ├─ Library/{planId}          (Tab)
       ├─ Settings/{planId}         (Tab)
       ├─ EditPlant/{planId}?plantId (aus Plan oder PlantPicker)
       └─ EditSection/{planId}?sectionId
```

`template_id` wird via `savedStateHandle` von PlantPickerScreen zu EditPlantScreen übergeben (kein Route-Argument, weil optionaler Pfad).

## WebView / PDF-Export

`MainActivity` hält eine `WebView`-Instanz außerhalb des Compose-View-Trees. Zugriff aus ViewModel via Event `PlanEvent.StartPrint(html)` → `MainActivity` lädt HTML und ruft `createPrintDocumentAdapter()` auf.

## Wichtige Referenzdateien

- `ARCHITECTURE.md` — vollständige technische Architektur
- `IMPLEMENTATION.md` — detaillierter Sessionsplan mit Code-Beispielen
- `gartenplaner_2026.html` — Referenz-PDF-Template (CSS-Klassen, Tabellenstruktur)
- `GartenPlaner_Mockups.html` — UI-Mockups aller Screens (Light + Dark)
