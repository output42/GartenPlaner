# Architektur

## Produkt und Invarianten

Minimalistischer Jahresplaner für Aussaat/Pflege/Ernte (Zielgruppe: Einsteiger, Selbstversorger,
Schrebergärtner, F-Droid-Nutzer); Ergebnis ist ein ausdruckbarer Plan „zum Aufhängen". Kein
2D-Scrollen der Monatsmatrix auf Mobile — horizontaler Chip-Strip pro Zeile statt Grid.
Quelle: `GartenPlaner_Konzept.md#Vision/#Screen 1`.

Unveränderliche Invarianten (CLAUDE.md): offline-first, kein Server, kein Account, **keine externen
Libraries**, GPL-3.0, F-Droid-kompatibel — keine `INTERNET`-Permission, keine Google Play Services.
JSON (Backup) nur über `org.json` (AOSP), nie Gson/Moshi/kotlinx-serialization. Fonts als `.ttf` in
`res/font/` (kein `ui-text-google-fonts` zur Laufzeit — Hinweis dazu stand sogar im
IMPLEMENTATION-Gradle-Beispiel, siehe [[offene-fragen]]). DAOs nutzen `@Upsert`, kein manuelles
Löschen von Kindern im Repository (Cascade/Room-FK übernimmt das). Keine
`androidx.compose.ui.graphics.Color` in `data/model`/`data/db` — stattdessen `colorArgb: Long`, die
UI macht `Color(entry.type.colorArgb)`. **Im Code bereits so umgesetzt** (`ActivityType` hat
`colorArgb`/`textArgb`, `chipColor` ist eine Extension in `ui/theme`).
Quelle: `CLAUDE.md#Unveränderliche Invarianten`, `app/.../data/model/ActivityType.kt`.

## Tech-Stack

Kotlin, Compose, Room, PDF via Android `PrintManager` + unsichtbare `WebView` (keine externe PDF-Lib —
würde GPL/F-Droid verletzen), HTML über Kotlin-String-Templates. minSdk 26 (PrintManager stabil),
targetSdk laut Code **35** (Release-Checkliste in der Doku nennt noch 34 — Doku veraltet). Room 2.6.1,
Navigation 2.8.3, KSP mit `room.schemaLocation` auf `../schemas`.
Quelle: `ARCHITECTURE.md#Tech-Stack`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.

## Navigation

Start `plan_list` (immer, auch bei genau einem Plan — kein Auto-Sprung in den PlanScreen). Tabs
(Bottom-Nav in `PlanScaffold`): `plan/{planId}`, `library/{planId}`, `settings/{planId}`. Gepusht:
`edit_plant/{planId}?plantId=&templateId=`, `edit_section/{planId}?sectionId=`,
`plant_picker/{planId}`. `PlantPickerScreen` läuft in zwei Modi: `standalone = true` als Library-Tab,
`standalone = false` als gepushter Picker (nur dieser liefert eine Vorlage an EditPlant).
Tab-Wechsel: Plan-Tab nutzt `popBackStack(Screen.Plan.route(planId), inclusive = false)` (holt Plan
wieder nach oben, kein Neu-Laden); Library/Settings nutzen `navigate { popUpTo(Plan) { saveState =
true }; restoreState = true; launchSingleTop = true }`. Library und Settings liegen damit immer direkt
über Plan im Stack.
Quelle: `app/.../navigation/Screen.kt`, `app/.../ui/components/PlanBottomBar.kt`.

`templateId` (Picker → EditPlant) ist im **Code** ein optionales Route-Argument
(`edit_plant/{planId}?...&templateId={templateId}`) — das widerspricht sowohl CLAUDE.md
(`savedStateHandle`-Key `template_id`) als auch IMPLEMENTATION (`selected_template` über
`previousBackStackEntry`). Drei verschiedene Mechanismen dokumentiert; **Code gilt**. Siehe
[[offene-fragen]].
Quelle: `app/.../navigation/Screen.kt` vs. `CLAUDE.md#Navigationsfluss`.

## State-Management-Pattern

Screen → `collectAsStateWithLifecycle(vm.uiState)` → ViewModel (`StateFlow<UiState>`) → Repository
(Flow) → DAO. ViewModels halten keinen Navigation-State; Side-Effects (Speichern, PDF, Navigation)
laufen über `events: SharedFlow`. `PlanUiState` ist `Loading | Empty | Success(plan, sections,
monthEntries) | ...` — **`Empty`, nicht `Error`** wie es die ARCHITECTURE-Doku beschreibt.
`EditSectionScreen` hat bewusst **kein eigenes ViewModel** (direkter Repository-Aufruf über
`rememberCoroutineScope()` — Scope stirbt mit dem Composable, Rotation verliert ungespeicherte
Eingabe; akzeptierte Vereinfachung).
Quelle: `ARCHITECTURE.md#State-Management`, `app/.../ui/plan/PlanViewModel.kt`,
`app/.../ui/editsection/EditSectionScreen.kt`.

## Screens im Überblick

- **PlanListScreen** — Pläne nach `year` absteigend, Anlegen per Dialog, Löschen per Swipe +
  Bestätigung (Cascade erledigt den Rest).
- **PlanScreen** — `LazyColumn` mit Section-Headern, je Pflanze eine Zeile mit 12 gleich breiten
  Monats-Chips (`MonthChipRow`, `weight(1f)`, Index = Monat 0–11); Edit-Modus mit Drag-Handle +
  Löschen (optimistisches lokales Reorder, DB-Write bei `onDragStopped`, nicht bei jedem Drag-Schritt).
- **EditPlantScreen** — BottomSheet pro Monat: Typ-Radio, „Leer/nichts", Notizfeld nur bei PFLEGE
  sichtbar (Konzept-Doku sagte: Textfeld bei jedem Typ — Code/ARCHITECTURE gelten). Arbeitet auf
  lokalem State (12er-Liste), kein Live-Write; `save()` upsertet Plant + ersetzt alle Monatseinträge in
  einer DB-Transaktion (siehe [[datenmodell-und-migrationen]]).
- **SettingsScreen** — schreibt sofort (kein Speichern-Button): Plan-Felder, Klimazone, Planverwaltung
  (Kopieren fürs nächste Jahr, Löschen → zurück zur Liste), JSON-Backup (SAF), App-Info.
- **Druck/PDF**: `MainActivity` hält eine `WebView` **außerhalb** des Compose-Trees, nur für den Druck,
  `javaScriptEnabled = false`. ViewModel sendet ein Event mit dem fertigen HTML; `MainActivity` lädt es
  und ruft `createPrintDocumentAdapter()`. Im Code wird `webViewClient` **vor** `loadDataWithBaseURL`
  gesetzt (Race aus einem älteren Doku-Beispiel ist damit vermieden); `createPrintDocumentAdapter`
  läuft erst in `onPageFinished`. `isPrinting`-Guard gegen Doppeltipp, bleibt aber hängen, wenn das
  Laden nie fertig wird.
Quelle: `ARCHITECTURE.md#Screens`, `app/.../MainActivity.kt#startPrint`.

## Verweise

[[datenmodell-und-migrationen]] · [[konventionen-und-entscheidungen]] ·
[[performance-und-technische-schulden]] · [[offene-fragen]]
