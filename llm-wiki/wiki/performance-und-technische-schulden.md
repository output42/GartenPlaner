# Performance und technische Schulden

Hinweis: Diese Seite fasst mehrere Doku-Generationen zusammen (`performance_fix.md`,
`performance_fix_2.md`, `V1_technische_schulden.md`). Viele dort gelistete Probleme sind im
**aktuellen Code bereits gelöst** — das ist explizit vermerkt, weil die Schulden-Dokumente selbst nicht
nachgezogen wurden.

## Tab-Navigation lud den Plan bei jedem Tab-Wechsel neu (gelöst)

Ursache: `PlanBottomBar` navigierte mit `navigate(...) { popUpTo(Plan) { inclusive = true } }` und warf
damit den bestehenden Plan-BackStackEntry raus → neues `PlanViewModel` → alle 4 Room-Queries von vorn.
**Fix A** (umgesetzt): Plan-Tab-Klick nutzt `popBackStack(Screen.Plan.route(planId), inclusive =
false)` — poppt nur Library/Settings darüber weg, der Plan-Entry bleibt samt Scroll-Position/Edit-Mode
erhalten.
Quelle: `performance_fix.md#Kontext/#Fix A`, `app/.../ui/components/PlanBottomBar.kt`.

## BottomBar-Layout-Jump bei Nicht-Tab-Navigation (laut Code weiterhin offen)

Ursache: Der äußere `Scaffold` in `MainActivity` liest `currentBackStackEntryAsState()` und zeigt/
versteckt die `PlanBottomBar` exakt im Navigationsmoment — Scaffold-Höhe ändert sich, beide
Screens (alter/neuer) layouten während der Animation neu → Frame-Drop, bei jeder Navigation außer
Tab-Wechsel. Vorgeschlagener Fix 2 aus `performance_fix_2.md` (BottomBar in die einzelnen Screens
verschieben statt im äußeren Scaffold) ist laut Code-Kommentar **nicht umgesetzt** — die BottomBar
sitzt weiterhin im äußeren Scaffold („bewegt sich nie während Tab-Transitionen", ironischerweise die
Ursache des Jumps bei anderen Übergängen). Siehe [[offene-fragen]].
Quelle: `performance_fix_2.md#Verdächtiger 1/2`, `app/.../MainActivity.kt`.

## Weitere umgesetzte Performance-Fixes

- **Skeleton statt Spinner** (Fix B): `PlanUiState.Loading` zeigt 5 Platzhalter-Zeilen statt
  `CircularProgressIndicator` — verbessert nur die wahrgenommene Ladezeit, nicht die echte.
- **SideEffect → LaunchedEffect** (Fix 3, umgesetzt): StatusBar-Setup lief vorher in `SideEffect {}`
  (bei jeder Rekomposition, potenziell jedem Animationsframe, mit synchronen View-Calls) und steht jetzt
  in `LaunchedEffect(Unit)` (einmalig) — reagiert dafür nicht mehr auf spätere Theme-Wechsel.
- **Tab-Transitionen als Crossfade** (200/160 ms) statt Slide für die 3 Tab-Screens.
- **Companion-Object-Cache** in `PlanViewModel` (`ConcurrentHashMap<Int, PlanUiState>`, Status
  „implementiert, minimale Wirkung, weil nicht die eigentliche Ursache"): `uiState`-Flow emittiert
  zuerst den Cache-Wert, dann die kombinierten Room-Flows; Cache wird **nie invalidiert** (gelöschte
  Pläne bleiben prozessweit drin) — akzeptierte technische Schuld.
- **`prewarmPlanCache()`** in `MainActivity`: lädt beim Start alle Pläne samt Sections/Monatseinträgen
  und füllt den ViewModel-Cache vor; Kosten wachsen mit der Planzahl, Exceptions werden still
  verschluckt (`catch (_: Exception) {}`).
Quelle: `performance_fix.md#Fix B`, `performance_fix_2.md#Status/#Verdächtiger 4`,
`app/.../ui/plan/PlanViewModel.kt`, `app/.../MainActivity.kt#prewarmPlanCache`.

## `WhileSubscribed`-Timeout: im Code anders als in beiden Fix-Dokumenten geplant

Geplant (Fix D): `PlanViewModel`/`SettingsViewModel` auf `30_000`, `PlanListViewModel` bleibt `5_000`.
**Code tatsächlich**: `PlanViewModel` **5_000** (hat inzwischen einen eigenen Companion-Cache, der den
Zweck von Fix D anders löst), `SettingsViewModel` `30_000`, `PlanListViewModel` **30_000** (mit eigenem
`cachedState` als `initialValue`). Alles davon widerspricht zusätzlich der CLAUDE.md-Invariante
„überall `5_000`". Siehe [[offene-fragen]] — vom Nutzer zu klären, welcher Stand gewollt ist.
Quelle: `performance_fix.md#Fix D` vs. `app/.../ui/plan/PlanViewModel.kt`,
`app/.../ui/planlist/PlanListViewModel.kt`, `app/.../ui/settings/SettingsViewModel.kt`.

## Technische Schulden — Stand im Schulden-Dokument vs. aktueller Code

| Kürzel | Befund (Dokument) | Stand im Code |
|---|---|---|
| M1 | `MutableSharedFlow<PlanEvent>()` ohne Buffer — Events vor dem ersten Subscriber gehen verloren | **Behoben**: `extraBufferCapacity = 1` im Code (Dokument nicht nachgezogen) |
| M2 | `copyPlanForYear` ohne Transaktion | **Behoben**: läuft in `db.withTransaction { … }` (Dokument nicht nachgezogen) |
| M3 | Flow-Collection ohne Fehlerbehandlung (`EditPlantViewModel.loadSections`) | laut gelesenen Quellen weiterhin offen |
| M4 | Drag-&-Drop-Race: `LaunchedEffect(dbSections)` überschreibt `localSections` auch während eines Drags | laut gelesenen Quellen weiterhin offen (Fix: nur aktualisieren, wenn `draggingPlantId == null`) |
| M5 | Navigationslogik doppelt im Composable statt als `PlanEvent` | laut gelesenen Quellen weiterhin offen |
| N4 | JOIN in `MonthEntryDao` nur zum Filtern nach `plan_id` | **Behoben** durch Schema v3 (eigene `plan_id`-Spalte auf `month_entries`, kein JOIN mehr, Code-Kommentar bestätigt „kein JOIN mehr") |
| N1, N2, N3, N5, N6 | O(n)-Suche in `LibraryRepository`, Drag-Zustand nicht rotationsfest, `.value =` statt `.update{}`-Mix, Routen ohne URL-Encoding, Rücknavigation per Route-String | laut gelesenen Quellen nicht gegengeprüft — als offen behandeln |

Quelle: `V1_technische_schulden.md#M1–M5, N1–N6`, `app/.../ui/plan/PlanViewModel.kt`,
`app/.../data/repository/PlanRepository.kt`, `app/.../data/db/MonthEntryDao.kt`.

## Im Code gefundene, nicht in den Schulden-Dokumenten stehende Verdachtsmomente

- **`@Upsert`-Rückgabewert bei Update** (`EditPlantViewModel.save()` → `upsertPlant()` →
  `PlantDao.upsert()`): Room liefert bei einem **Update** üblicherweise `-1` (Insert scheitert am
  Primärschlüssel-Konflikt → Update → `-1`). Wird dieser Rückgabewert direkt als `savedPlantId` an
  `replaceMonthEntries()` weitergereicht, würde `getPlantById(-1)` ins Leere laufen und
  Monatsänderungen beim **Bearbeiten einer bestehenden Pflanze** stillschweigend verwerfen. **Nicht auf
  dem Gerät verifiziert** — höchste Priorität in [[offene-fragen]].
- **PDF-Export ignoriert ungruppierte Pflanzen**: `HtmlExporter.buildHtml()` iteriert nur über
  `sections` und deren `plants` — Pflanzen mit `section_id = NULL` (möglich seit Schema v2, siehe
  [[datenmodell-und-migrationen]]) fehlen im gedruckten Plan, obwohl `PlanScreen` und
  `copyPlanForYear` sie berücksichtigen. Nicht auf dem Gerät geprüft.
Quelle: `app/.../ui/editplant/EditPlantViewModel.kt#save`, `app/.../data/db/PlantDao.kt`,
`app/.../export/HtmlExporter.kt#buildHtml`.

## Verweise

[[architektur]] · [[datenmodell-und-migrationen]] · [[konventionen-und-entscheidungen]] ·
[[offene-fragen]]
