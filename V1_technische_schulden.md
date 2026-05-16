# Technische Schulden — GartenPlaner v1.0

Gefunden beim Code-Audit vor dem Release. Die drei kritischen Punkte wurden
bereits in der gleichen Session behoben (commit nach diesem Dokument). Alles
hier ist noch offen.

---

## Moderat — Kann seltsames Verhalten erzeugen

### M1 · SharedFlow ohne Buffer — Events können verloren gehen
**Datei:** `ui/plan/PlanViewModel.kt:41`  
`MutableSharedFlow<PlanEvent>()` hat Standardpuffer 0. Events (z.B. Undo-Snackbar),
die vor dem ersten UI-Subscriber emittiert werden, gehen lautlos verloren.  
**Fix:** `MutableSharedFlow(replay = 1)` oder `extraBufferCapacity = 1`.

### M2 · `copyPlanForYear` ohne Transaktion — Absturz hinterlässt halbfertigen Plan
**Datei:** `data/repository/PlanRepository.kt:81`  
Die Funktion schreibt Dutzende einzelne DB-Operationen ohne umschließende
Transaktion. Ein Absturz mittendrin hinterlässt einen Plan mit fehlenden
Bereichen oder Pflanzen.  
**Fix:** Den gesamten Block in `db.withTransaction { … }` einwickeln.

### M3 · DB-Flow-Collection ohne Ende — Coroutine lebt nach Screen-Abbau weiter
**Datei:** `ui/editplant/EditPlantViewModel.kt:49`  
`loadSections()` startet eine Coroutine, die `getSectionsWithPlants()` unbegrenzt
sammelt. Da `viewModelScope` daran hängt, lebt die Collection bis das ViewModel
klar geräumt wird — kein Error-Handling wenn die DB kurz fehlschlägt.  
**Fix:** Scope ist korrekt (`viewModelScope`), aber Exception-Handling fehlt.
Entweder `.catch { }` auf dem Flow oder `try/catch` im Launch-Block.

### M4 · State-Synchronisations-Race beim Drag & Drop
**Datei:** `ui/plan/PlanScreen.kt:56`  
`LaunchedEffect(dbSections)` aktualisiert `localSections` auch während ein
aktiver Drag läuft. Ein DB-Emit während des Ziehens kann den UI-Zustand unter
dem Finger verändern.  
**Fix:** Update von `localSections` nur ausführen wenn gerade kein Drag aktiv
ist (`draggingPlantId == null`).

### M5 · `onPlantClick`-Logik dupliziert in Composable statt im ViewModel
**Datei:** `ui/plan/PlanScreen.kt:276`  
Navigationslogik (welche Route für welchen Klick) ist an zwei Stellen im
Composable statt zentral im ViewModel definiert.  
**Fix:** Navigation als ViewModel-Event kapseln (`PlanEvent.NavigateTo`).

---

## Minor — Wartbarkeit und Performance

### N1 · Bibliothekssuche O(n) statt O(1)
**Datei:** `data/repository/LibraryRepository.kt:12`  
`getById()` ruft `.find()` auf der gesamten Liste auf. Bei 37 Einträgen kein
Problem; bei wachsender Bibliothek verlangsamt es jeden Klick im PlantPicker.  
**Fix:** Interne `Map<Int, PlantTemplate>` beim Initialisieren aufbauen.

### N2 · Drag-Zustand nicht konfigurationsänderungssicher
**Datei:** `ui/plan/PlanScreen.kt:57`  
`localSections`, `draggingPlantId`, `dragOffsetY`, `itemHeights` sind
Composable-State — bei Gerätedrehung während eines aktiven Drags wird der
Zustand zurückgesetzt.  
**Fix:** In ViewModel verschieben oder `rememberSaveable` verwenden.

### N3 · `.value =` und `.update {}` gemischt in EditPlantViewModel
**Datei:** `ui/editplant/EditPlantViewModel.kt:95`  
`updateName()`, `updateSubtitle()`, `updateSection()` nutzen `.value =`-Zuweisung,
während andere Stellen (z.B. Zeile 103) `.update {}` verwenden. Inkonsistent,
kein Fehler, aber schwerer lesbar.  
**Fix:** Einheitlich auf `.update { it.copy(…) }` umstellen.

### N4 · Ineffizienter JOIN in MonthEntryDao
**Datei:** `data/db/MonthEntryDao.kt:19`  
Die Query nutzt einen INNER JOIN zu `plants` nur um nach `plan_id` zu filtern.
Eine direkte `plan_id`-Spalte auf `month_entries` würde den JOIN überflüssig
machen und die Abfrage beschleunigen.  
**Fix:** Migration: `plan_id`-Spalte zu `month_entries` hinzufügen, Index setzen,
JOIN entfernen. (Breaking change — braucht Room-Migration.)

### N5 · Route-Strings werden ohne URL-Encoding gebaut
**Datei:** `navigation/Screen.kt:27`  
Query-Parameter für EditPlant werden per String-Konkatenation zusammengebaut.
Pflanzennamen mit Sonderzeichen (z.B. `&`, `=`) würden die Route brechen.  
**Fix:** `Uri.encode()` auf alle Parameterwerte anwenden, oder auf
Navigation-Safe-Args migrieren.

### N6 · Hardcodierter Rücknavigations-Route-String
**Datei:** `ui/editplant/EditPlantScreen.kt:74`  
`navController.popBackStack(Screen.Plan.route(planId), false)` konstruiert die
Route als Rohstring. Ändert sich das Route-Format in `Screen.kt`, bricht die
Navigation lautlos.  
**Fix:** Route immer über `Screen.Plan.route(planId)` beziehen (bereits so) —
aber sicherstellen dass nirgendwo ein manuell zusammengesetzter String steht.
