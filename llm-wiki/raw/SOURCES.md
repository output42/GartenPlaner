# Quellen (SOURCES)

Liste der Quelldokumente im GartenPlaner-Repo, auf die `wiki/*.md` verweist. Keine Kopien — nur Pfade.
Pfade relativ zur Repo-Wurzel `/home/output42/CursorProjekte/GartenPlaner`.

## Planungsdokumente (unterschiedlicher Aktualitätsgrad — siehe wiki/offene-fragen.md)

- `CLAUDE.md`
- `ARCHITECTURE.md`
- `IMPLEMENTATION.md`
- `GartenPlaner_Konzept.md`
- `V1_technische_schulden.md`
- `performance_fix.md`
- `performance_fix_2.md`
- `CHANGELOG.md`

## Code (zentrale Dateien, laut Wissenskarten geprüft)

- `app/build.gradle.kts`, `gradle/libs.versions.toml`
- `app/src/main/kotlin/de/gartenplaner/data/model/` (`Plan.kt`, `Plant.kt`, `MonthEntry.kt`, `ActivityType.kt`)
- `app/src/main/kotlin/de/gartenplaner/data/db/` (`GardenDatabase.kt`, `Converters.kt`, `PlantDao.kt`, `MonthEntryDao.kt`)
- `app/src/main/kotlin/de/gartenplaner/data/repository/PlanRepository.kt`
- `app/src/main/kotlin/de/gartenplaner/data/backup/PlanImporter.kt`
- `app/src/main/kotlin/de/gartenplaner/data/library/PlantLibrary.kt`
- `app/src/main/kotlin/de/gartenplaner/data/seed/DemoSeed.kt`
- `app/src/main/kotlin/de/gartenplaner/navigation/Screen.kt`
- `app/src/main/kotlin/de/gartenplaner/ui/plan/PlanViewModel.kt`
- `app/src/main/kotlin/de/gartenplaner/ui/planlist/PlanListViewModel.kt`
- `app/src/main/kotlin/de/gartenplaner/ui/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/de/gartenplaner/ui/editplant/EditPlantViewModel.kt`
- `app/src/main/kotlin/de/gartenplaner/ui/editsection/EditSectionScreen.kt`
- `app/src/main/kotlin/de/gartenplaner/ui/components/PlanBottomBar.kt`
- `app/src/main/kotlin/de/gartenplaner/MainActivity.kt`
- `app/src/main/kotlin/de/gartenplaner/export/HtmlExporter.kt`
- `app/src/test/.../PlantLibraryTest.kt`
- `schemas/de.gartenplaner.data.db.GardenDatabase/{1,2,3}.json`

## Geheimnisse — nicht öffnen, nicht zitieren

- `keystore.propertys` (im Repo-Root, untracked — Signierungsdaten)
- jede weitere `*.properties`-Datei mit Schlüsseln/Zugangsdaten

## Hinweis zum Arbeitsbaum (Stand Erstanlage, nicht Teil dieses Wikis)

Bei Anlage dieses Wikis lagen im Arbeitsbaum zusätzlich unversionierte/geänderte Dateien
(`.claude/settings.local.json` geändert, `.mcp.json`, `GartenPlaner-1.0.0.apk`, `app/build/`,
`performance_fix.md`, `performance_fix_2.md` als untracked) — diese wurden für dieses Wiki nur
gelesen (zwei `performance_fix*.md` als Quelle, siehe oben), nicht verändert oder committet.
