# Konventionen und Produktentscheidungen

## Coding-Konventionen

- `StateFlow`/`stateIn` nutzt projektweit `SharingStarted.WhileSubscribed(5_000)` als Invariante
  (CLAUDE.md) — `PlanViewModel`/`SettingsViewModel` wurden performance-bedingt teils auf `30_000`
  umgestellt, siehe [[performance-und-technische-schulden]] und [[offene-fragen]] für den
  dokumentierten Widerspruch dazu.
- `@Upsert` statt getrennter Insert/Update-DAO-Methoden; Repositories löschen Kinder nicht manuell
  (Cascade/FK übernimmt das).
- Farben: zwei bewusst getrennte Farbsätze je `ActivityType` — PDF/CSS-Farben (heller Zellhintergrund,
  dunkler Text) vs. App-Chip-Farben (z. B. VORANZUCHT als einziger Chip mit dunklem statt weißem Text).
  Datenmodell-Kommentare nannten zeitweise noch einen dritten Satz Werte — veraltet.
- `HtmlExporter` escaped `&`, `<`, `>`, `"` für Nutzertexte in der HTML-Tabelle, **`&` zuerst**, sonst
  werden erzeugte Entities doppelt escaped; leeres Label fällt auf `type.defaultLabel` zurück.
- Spaltennamen in Room durchgängig `snake_case` (`plant_id`, `section_id`, `plan_id`).
- DAO-Tests laufen als Instrumentierungstests (`androidTest/`, `Room.inMemoryDatabaseBuilder`) —
  `./gradlew test` (JVM) führt sie **nicht** aus.
Quelle: `CLAUDE.md#Unveränderliche Invarianten`, `IMPLEMENTATION.md#Farbwerte`,
`app/.../export/HtmlExporter.kt`, `IMPLEMENTATION.md#Unit-Tests`.

## Aktivitätstypen (fix, genau 5)

VORANZUCHT („Voranz."), DIREKTSAAT („Direktsaat"), AUSPFLANZEN („Auspfl."), ERNTE („Ernte"), PFLEGE
(„Pflege", als einziger Typ mit Notizfeld im Editor). Label ist frei wählbar, der Typ bestimmt nur die
Farbe — „Ernte ↑" ist ein Label, kein sechster Typ. Eigene/benutzerdefinierte Aktivitätstypen sind für
v1.0 bewusst **nicht** vorgesehen.
Quelle: `CLAUDE.md#ActivityType`, `GartenPlaner_Konzept.md#Screen 3`, `ARCHITECTURE.md#Entschiedene Design-Fragen`.

## Produktentscheidungen

- **Mehrere Pläne, ein Plan pro Jahr** (nicht: ein einziger aktiver Plan). `activePlanId` liegt in
  SharedPreferences (reiner UI-State für den Schnellstart), nicht in Room — ältere Versionsplan-Zeilen
  („ein aktiver Plan pro App") sind veraltet, maßgeblich sind Entscheidungstabelle, CHANGELOG und Code.
- **Start immer über die Planliste**, auch bei genau einem Plan — bewusst kein automatischer Sprung in
  den PlanScreen, aus Konsistenzgründen.
- **PDF statt externer Lib**: Android `PrintManager` + unsichtbare `WebView` erzeugt Chromium-Qualität
  ohne eine GPL-/F-Droid-unverträgliche PDF-Bibliothek (iText wäre AGPL/kommerziell).
- **Pflanzenbibliothek ist statischer Kotlin-Code**, kein Asset/keine DB — Compile-Time-Sicherheit und
  einfache F-Droid-Reproduzierbarkeit. Vorlagen sind read-only; Übernahme kopiert Werte in eine neue
  `Plant` mit `fromLibrary=true`.
- **Versionsplan**: v1.0 (GitHub + F-Droid) ohne Beetplaner/Fruchtfolge; v1.1 Benachrichtigungen; v2.0
  Beetplaner (Raster, Fruchtfolge, Mischkultur, dann auch Play Store); v3.0 Zuchtplaner.
- **Plan-Löschen**: Bestätigungsdialog (Cascade löscht den ganzen Baum). **Pflanze löschen**: sofort,
  kein Dialog, dafür Undo-Snackbar (muss beim Rückgängigmachen auch die kaskadierend gelöschten
  Monatseinträge wiederherstellen). **Section löschen**: Bestätigungsdialog. Kriterium für
  Dialog-vs.-Undo ist, wie teuer/unvollständig ein Rückgängigmachen wäre.
Quelle: `ARCHITECTURE.md#Entschiedene Design-Fragen`, `#Pflanzenbibliothek`, `#Versionierungsplan`,
`IMPLEMENTATION.md#PlanListScreen.kt`, `#Session 7`.

## Release-Eckdaten (Code-Stand)

minSdk 26, targetSdk 35, versionCode 1 / versionName 1.0.0, keine `INTERNET`-Permission, keine
Storage-Permissions (alles über SAF), `exportSchema = true` + `schemas/` committet, Lizenz GPL-3.0,
Predictive Back nur über das Manifest-Flag `android:enableOnBackInvokedCallback="true"` (Navigation
Compose ≥ 2.7 reicht aus).
Quelle: `IMPLEMENTATION.md#Release-Checkliste`, `app/build.gradle.kts`.

## Verweise

[[architektur]] · [[datenmodell-und-migrationen]] · [[performance-und-technische-schulden]] ·
[[offene-fragen]]
