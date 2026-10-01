# Offene Fragen und Widersprüche

Diese Liste ist ungewöhnlich lang, weil GartenPlaner mehrere, nie konsolidierte Planungsdokumente hat
(`CLAUDE.md`, `ARCHITECTURE.md`, `IMPLEMENTATION.md`, `GartenPlaner_Konzept.md`,
`V1_technische_schulden.md`, zwei `performance_fix*.md`). **Grundregel: der tatsächliche Code gewinnt**;
wo der Code nicht gelesen/geprüft wurde, ist das vermerkt.

## Braucht eine Bestätigung des Projektinhabers (aus Code abgeleitet, nicht am Gerät geprüft)

- **`@Upsert` liefert `-1` bei Update → gehen Monatsänderungen beim Bearbeiten verloren?**
  `EditPlantViewModel.save()` reicht den Rückgabewert von `upsertPlant()` direkt als `plantId` an
  `replaceMonthEntries()` weiter; bei Room-Updates ist der Upsert-Rückgabewert typischerweise `-1`.
  Siehe [[performance-und-technische-schulden]].
- **Section löschen: `SET NULL` statt Kaskade — wirklich gewollt?** CLAUDE.md/IMPLEMENTATION
  beschreiben noch volle Kaskade; seit Schema v2 bleiben Pflanzen ohne Section erhalten
  (`ON DELETE SET NULL`). Siehe [[datenmodell-und-migrationen]].
- **PDF-Export ignoriert Pflanzen ohne Section** — Folge der SET-NULL-Entscheidung, im Exporter nicht
  nachgezogen.
- **`WhileSubscribed`-Werte vertauscht?** Geplant (Fix D) vs. Code vs. CLAUDE.md-Invariante — drei
  unterschiedliche Stände, siehe [[performance-und-technische-schulden]].
- **BottomBar-Fix (`performance_fix_2`, Fix 2) offen oder verworfen?** Im Code steht die BottomBar
  weiterhin im äußeren Scaffold von `MainActivity`.
- **`LaunchedEffect(Unit)` für StatusBar reagiert nicht mehr auf Theme-Wechsel** (Dark/Light) — Folge
  des SideEffect-Fixes, als Nebenwirkung nicht ausdrücklich entschieden.

## Widersprüche zwischen Planungsdokumenten (nach Thema)

**Pläne/Startverhalten**
- „Mehrere Pläne" (Entscheidungstabelle, CHANGELOG) vs. „ein aktiver Plan pro App"
  (Versionsplan-Zeile, veraltet) — siehe [[konventionen-und-entscheidungen]].
- `PlanDao.getActivePlan() = SELECT * FROM plans LIMIT 1` (IMPLEMENTATION S2, aus einer
  Ein-Plan-Annahme) — der tatsächliche aktive Plan kommt aus SharedPreferences/Route-Argument, nicht
  aus dieser Query (technische Schuld, Relevanz im aktuellen Code nicht geprüft).

**Datenmodell**
- `MonthEntry` als eingebettetes Feld (Konzept) vs. eigene Room-Tabelle (ARCHITECTURE/Code) — Code
  gilt.
- `ActivityType` mit Compose-`Color` im Entity (IMPLEMENTATION-Beispiel) vs. CLAUDE.md-Invariante
  (`colorArgb: Long`) — **im Code zugunsten der Invariante gelöst** (`colorArgb`/`textArgb`).
- Kaskade „überall" (CLAUDE.md, IMPLEMENTATION S7) vs. `SET NULL` für `plants.section_id` seit Schema
  v2 — siehe oben.
- `V1_technische_schulden.md` nennt N4 (JOIN in `MonthEntryDao`) und M2 (`copyPlanForYear` ohne
  Transaktion) als offen; beide sind im Code bereits gelöst (Schema v3 bzw. `withTransaction`) — das
  Schulden-Dokument wurde nicht nachgezogen.

**Navigation**
- `template_id`-Übergabe Picker → EditPlant: CLAUDE.md sagt `savedStateHandle`-Key `template_id`,
  IMPLEMENTATION sagt `selected_template` über `previousBackStackEntry`, **Code** macht es als
  optionales Route-Argument `templateId` — Code gilt.
- Bibliotheks-Tab: ARCHITECTURE beschreibt eine eigenständige `PlantPickerScreen`, CLAUDE.md eine
  Tab-Route `Library/{planId}` — ebenso unterschiedliche Bibliotheksgrößen „~40" vs. „37+" in
  verschiedenen Dokumenten (Code: exakt 37).
- `saveState`/`restoreState` (IMPLEMENTATION, klassisches Bottom-Nav-Muster) vs. der tatsächliche
  Push-Stack, den `performance_fix.md` beschreibt (Library/Settings liegen über Plan) — Code/Fix A
  gelten.

**Session-Status / Projektfortschritt**
- Drei sich widersprechende Session-Tracker: IMPLEMENTATION-Übersicht (S1–S5, S8 fertig), CLAUDE.md
  (S5–S8, S11 fertig, S1/S9/S10 offen/TODO), CHANGELOG (v1.0.0 bereits veröffentlicht inkl. PDF-Export
  und Backup). Kein Dokument ist allein verlässlich — bei Zweifel am Code prüfen, was tatsächlich
  existiert.
- `PlanEvent.StartPrint`: drei Fassungen in drei Dokumenten (`data object` ohne Payload, Event mit
  `plan/sections/monthEntries`, `StartPrint(html)`) — Code entscheidet, welche gilt.
- `PlanUiState`: ARCHITECTURE nennt `Loading/Success/Error`, der Code hat `Loading/Empty/Success`
  (kein `Error`-Zweig).

**Build/Release**
- Release-Checkliste nennt `targetSdk 34`, der Code hat `targetSdk 35`.

## Nicht gelesen / nicht ausgewertet

`docs/projects/apps/REVIEW.md` listet weitere Prüfpunkte (z. B. Autor-Schlussfolgerungen in
Testaufgaben-Referenzen, konkrete Beispiel-Aufgaben) — dieses Wiki wertet nur `wissen.md` aus, nicht die
Testaufgaben selbst (siehe [[SCHEMA]], Abgrenzung).

## Verweise

[[architektur]] · [[datenmodell-und-migrationen]] · [[konventionen-und-entscheidungen]] ·
[[performance-und-technische-schulden]]
