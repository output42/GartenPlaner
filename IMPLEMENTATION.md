# GartenPlaner — Implementierungsplan

> 11 Sessions à ~2 Stunden · Ziel: releasefähiges v1.0-APK für F-Droid
>
> Jede Session endet mit einem **konkreten, testbaren Ergebnis**.
> Abhängigkeiten fließen immer von oben nach unten — keine Session setzt
> etwas voraus das noch nicht fertig ist.

**Entschiedene Design-Fragen:**
- Mehrere Pläne (ein Plan pro Jahr), PlanListScreen als Start-Destination
- JSON-Backup/Restore via Android SAF (in Session 11)
- v1.0 nur Deutsch — alle Strings in `strings.xml`, kein Hardcoding in Compose

---

## Übersicht

| # | Session | Ergebnis | Status |
|---|---|---|---|
| 1 | Gradle-Setup + Theme | App baut, Farben und Fonts korrekt | ✅ abgeschlossen |
| 2 | Datenmodell + Room | DAO-Unit-Tests grün | ✅ abgeschlossen |
| 3 | Repositories + Pflanzenbibliothek | ~40 Pflanzen, Repository-Flows testbar | ✅ abgeschlossen |
| 4 | Navigation + Screen-Shells | Alle Screens erreichbar inkl. PlanListScreen | ✅ abgeschlossen |
| 5 | PlanListScreen + PlanScreen Anzeige | Pläne auflistbar, Demo-Plan sichtbar | ✅ abgeschlossen |
| 6 | EditPlantScreen + BottomSheet | Pflanzen anlegen und bearbeiten, persistiert | |
| 7 | PlanScreen — CRUD + Drag & Drop | Vollständiges Bearbeiten/Löschen/Umsortieren | |
| 8 | PlantPickerScreen + EditSectionScreen | Bibliothek durchsuchbar, Sections verwaltbar | ✅ abgeschlossen |
| 9 | SettingsScreen + Planverwaltung | Metadaten editierbar, neuer Plan aus Einstellungen | |
| 10 | HtmlExporter + PDF-Export | Drucktaste → A4-PDF im System-Dialog | |
| 11 | JSON-Backup + Polish + Release | Backup/Restore, Release-APK, F-Droid-Metadaten | |

---

## Session 1 — Gradle-Setup + Theme

**Ziel:** Das Android-Projekt baut fehlerfrei. Fonts und Farben sind exakt wie im Mockup.

### Gradle-Abhängigkeiten (`build.gradle.kts`)

```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.x")
implementation("androidx.room:room-ktx:2.6.x")
ksp("androidx.room:room-compiler:2.6.x")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.x"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-text-google-fonts")  // NICHT für Produktion
implementation("androidx.activity:activity-compose:1.9.x")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.x")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.x")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.x")
```

> **F-Droid-Hinweis:** `ui-text-google-fonts` lädt Fonts zur Laufzeit über Google APIs —
> nicht F-Droid-kompatibel. Nunito und DM Mono als `.ttf`-Dateien unter
> `res/font/` bündeln und via `FontFamily` einbinden.

### Dateien

```
app/src/main/
├── res/font/
│   ├── nunito_regular.ttf
│   ├── nunito_semibold.ttf
│   ├── nunito_bold.ttf
│   ├── nunito_extrabold.ttf
│   ├── dm_mono_regular.ttf
│   └── dm_mono_medium.ttf
└── ui/theme/
    ├── Color.kt       — alle Farb-Tokens (Light + Dark + ActivityType-Farben)
    ├── Theme.kt       — GartenPlanerTheme, MaterialTheme-Setup
    └── Typography.kt  — Nunito + DM Mono als FontFamily
```

### `Color.kt` — Schlüsselwerte

```kotlin
// App-UI
val GreenPrimary   = Color(0xFF2A6A2A)
val GreenAccent    = Color(0xFF6DCC6D)
val BackgroundLight = Color(0xFFF2F4F0)
val SurfaceLight    = Color(0xFFFFFFFF)
val BackgroundDark  = Color(0xFF111111)
val SurfaceDark     = Color(0xFF1E1E1E)

// ActivityType → UI-Chips
val AmberChip   = Color(0xFFE09A00)   // VORANZUCHT
val GreenChip   = Color(0xFF3D9E3D)   // DIREKTSAAT
val BlueChip    = Color(0xFF3A8FD4)   // AUSPFLANZEN
val RedChip     = Color(0xFFD44A4A)   // ERNTE
val PurpleChip  = Color(0xFF8A5CD0)   // PFLEGE

// ActivityType → PDF-CSS (für HtmlExporter, Session 10)
// Amber:  bg=#fff0c0 text=#7a4f00
// Grün:   bg=#c8ecd0 text=#145220
// Blau:   bg=#c5e0ff text=#00387a
// Rot:    bg=#fad0d3 text=#6e1219
// Lila:   bg=#e8d0f8 text=#3d1060
```

### Testbares Ergebnis

`./gradlew assembleDebug` baut ohne Fehler.
`MainActivity` zeigt leere Scaffold-Shell mit korrekter grüner TopAppBar.
Fonts sind in Preview und auf Gerät sichtbar (Nunito, nicht Roboto).

---

## Session 2 — Datenmodell + Room

**Ziel:** Alle Entities, DAOs und die Datenbank sind definiert. Unit-Tests für
die wichtigsten Queries laufen grün.

### Dateien

```
data/model/
├── Plan.kt
├── Section.kt
├── Plant.kt
├── MonthEntry.kt
├── ActivityType.kt
└── PlantTemplate.kt

data/db/
├── GardenDatabase.kt
├── PlanDao.kt
├── SectionDao.kt
├── PlantDao.kt
└── MonthEntryDao.kt
```

### Entity-Details

**`ActivityType.kt`**
```kotlin
enum class ActivityType(
    val defaultLabel: String,
    val chipColor: Color,      // App-UI
    val cssBackground: String, // PDF
    val cssText: String        // PDF
) {
    VORANZUCHT("Voranz.", AmberChip, "#fff0c0", "#7a4f00"),
    DIREKTSAAT("Direktsaat", GreenChip, "#c8ecd0", "#145220"),
    AUSPFLANZEN("Auspfl.", BlueChip, "#c5e0ff", "#00387a"),
    ERNTE("Ernte", RedChip, "#fad0d3", "#6e1219"),
    PFLEGE("Pflege", PurpleChip, "#e8d0f8", "#3d1060")
}
```

> ActivityType trägt alle kontextspezifischen Werte — keine switch-Statements
> in UI oder Exporter nötig.

**`MonthEntry.kt`** — eigene Tabelle, da 1:12-Relation zu Plant:

```kotlin
@Entity(
    tableName = "month_entries",
    foreignKeys = [ForeignKey(
        entity = Plant::class,
        parentColumns = ["id"],
        childColumns = ["plant_id"],
        onDelete = CASCADE
    )],
    indices = [Index("plant_id")]
)
data class MonthEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "plant_id") val plantId: Int,
    val month: Int,          // 0–11
    val type: ActivityType,
    val label: String
)
```

**`GardenDatabase.kt`**

```kotlin
@Database(
    entities = [Plan::class, Section::class, Plant::class, MonthEntry::class],
    version = 1,
    exportSchema = true    // Schema-Export für Migrations-Diff
)
@TypeConverters(Converters::class)
abstract class GardenDatabase : RoomDatabase() { ... }
```

Schema-Export-Pfad in Gradle konfigurieren → `schemas/` ins Git committen.
`ActivityType` als `@TypeConverter` String ↔ Enum.

### DAO-Queries (Auswahl)

```kotlin
// PlanDao
@Query("SELECT * FROM plans LIMIT 1")
fun getActivePlan(): Flow<Plan?>

// SectionDao
@Query("SELECT * FROM sections WHERE plan_id = :planId ORDER BY `order`")
fun getSectionsForPlan(planId: Int): Flow<List<Section>>

// PlantDao
@Query("SELECT * FROM plants WHERE section_id = :sectionId ORDER BY `order`")
fun getPlantsForSection(sectionId: Int): Flow<List<Plant>>

// MonthEntryDao
@Query("SELECT * FROM month_entries WHERE plant_id = :plantId ORDER BY month")
fun getEntriesForPlant(plantId: Int): Flow<List<MonthEntry>>

// Batch-Upsert für Monats-Updates
@Upsert
suspend fun upsertEntries(entries: List<MonthEntry>)
```

### Unit-Tests

```
androidTest/
└── data/db/
    ├── PlanDaoTest.kt        — insert/read/update Plan
    ├── SectionDaoTest.kt     — cascade-delete bei Plan-Delete
    ├── PlantDaoTest.kt       — order-Update, cascade-delete
    └── MonthEntryDaoTest.kt  — upsert-Logik, cascade-delete
```

In-Memory-DB (`Room.inMemoryDatabaseBuilder`) für Tests.

### Testbares Ergebnis

`./gradlew connectedAndroidTest` (oder Robolectric-Setup):
alle 4 DAO-Testklassen grün. Kein Laufzeitfehler bei DB-Initialisierung.

---

## Session 3 — Repositories + Pflanzenbibliothek

**Ziel:** Die Repository-Schicht abstrahiert die DAOs. Die Pflanzenbibliothek
enthält ~40 realistische Pflanzenvorlagen in 6 Kategorien.

### Dateien

```
data/repository/
├── PlanRepository.kt
└── LibraryRepository.kt

data/library/
└── PlantLibrary.kt
```

### `PlanRepository.kt` — API-Übersicht

```kotlin
class PlanRepository(private val db: GardenDatabase) {

    // Reads (Flow → StateFlow in ViewModel)
    fun getActivePlan(): Flow<Plan?>
    fun getSectionsWithPlants(planId: Int): Flow<List<SectionWithPlants>>
    fun getMonthEntries(plantId: Int): Flow<List<MonthEntry>>

    // Writes (suspend — in viewModelScope aufrufen)
    suspend fun upsertPlan(plan: Plan): Int      // gibt ID zurück
    suspend fun upsertSection(section: Section): Int
    suspend fun upsertPlant(plant: Plant): Int
    suspend fun upsertMonthEntries(plantId: Int, entries: List<MonthEntry?>)

    suspend fun deleteSection(section: Section)
    suspend fun deletePlant(plant: Plant)
    suspend fun deleteMonthEntry(entry: MonthEntry)

    suspend fun reorderSections(sections: List<Section>)  // batch update order
    suspend fun reorderPlants(plants: List<Plant>)
}
```

`SectionWithPlants` ist ein Room-`@Relation`-Wrapper:

```kotlin
data class SectionWithPlants(
    @Embedded val section: Section,
    @Relation(parentColumn = "id", entityColumn = "section_id")
    val plants: List<Plant>
)
```

Die Monats-Entries werden separat geladen (verhindert zu tiefe Verschachtelung).

### `PlantLibrary.kt` — Struktur

```kotlin
object PlantLibrary {
    val all: List<PlantTemplate> = listOf(
        // Fruchtgemüse (8)
        tomaten(), zucchini(), kueRbis(), paprika(), aubergine(),
        gurke(), mais(), fenchel(),
        // Wurzelgemüse (6)
        moehren(), roteBeete(), pastinaken(), petersilienwurzel(),
        rettich(), sellerie(),
        // Blattgemüse (6)
        salat(), spinat(), mangold(), gruenkohl(), kohlrabi(), pak_choi(),
        // Kräuter (8)
        basilikum(), petersilie(), dill(), schnittlauch(),
        koriander(), thymian(), rosmarin(), minze(),
        // Hülsenfrüchte (4)
        buschbohnen(), stangenbohnen(), erbsen(), dicke_bohnen(),
        // Zwiebeln & Lauch (5)
        knoblauch(), zwiebeln(), porree(), schalotten(), bärlauch()
    )

    fun byCategory(category: String) = all.filter { it.category == category }
    fun search(query: String) = all.filter {
        it.name.contains(query, ignoreCase = true) ||
        it.category.contains(query, ignoreCase = true)
    }
    val categories = listOf(
        "Fruchtgemüse", "Wurzelgemüse", "Blattgemüse",
        "Kräuter", "Hülsenfrüchte", "Zwiebeln & Lauch"
    )
}
```

Jede private Builder-Funktion (z.B. `fun tomaten(): PlantTemplate`) trägt
die Monats-Standardwerte hardcoded — kein JSON-Parsing, kein Asset-File.

**Beispiel Tomaten:**
- Voranzucht: März, April
- Auspflanzen: Mai
- Ernte: Juli, August, September

### `LibraryRepository.kt`

```kotlin
class LibraryRepository {
    fun getAll(): List<PlantTemplate> = PlantLibrary.all
    fun search(query: String, category: String?): List<PlantTemplate> {
        val filtered = if (category != null) PlantLibrary.byCategory(category)
                       else PlantLibrary.all
        return if (query.isBlank()) filtered
               else filtered.filter { it.name.contains(query, ignoreCase = true) }
    }
}
```

### Testbares Ergebnis

Unit-Test (JVM, kein Gerät nötig):
- `PlantLibrary.all` hat genau 37–42 Einträge
- Alle 6 Kategorien nicht leer
- Jede Pflanze hat mind. 2 belegte Monate
- `LibraryRepository.search("tom")` → enthält Tomaten, kein false positive

---

## Session 4 — Navigation + Screen-Shells

**Ziel:** Alle 6 Screens sind erreichbar. PlanListScreen ist Start-Destination.
Bottom-Nav erscheint nur innerhalb des PlanScreen-Kontexts. Kein Crash.

### Dateien

```
├── MainActivity.kt                   — NavHost + WebView-Holder
└── ui/
    ├── planlist/PlanListScreen.kt    — Placeholder: "PlanListScreen"
    ├── plan/PlanScreen.kt            — Placeholder mit BottomNav-Shell
    ├── editplant/EditPlantScreen.kt
    ├── plantpicker/PlantPickerScreen.kt
    ├── editsection/EditSectionScreen.kt
    └── settings/SettingsScreen.kt
```

### Route-Definitionen

```kotlin
sealed class Screen(val route: String) {
    // Root
    data object PlanList    : Screen("plan_list")

    // Bottom-Nav-Tabs innerhalb eines Plans (tragen planId)
    data class Plan(val planId: Int)     : Screen("plan/{planId}") {
        companion object { const val ROUTE = "plan/{planId}" }
    }
    data class Library(val planId: Int)  : Screen("library/{planId}") {
        companion object { const val ROUTE = "library/{planId}" }
    }
    data class Settings(val planId: Int) : Screen("settings/{planId}") {
        companion object { const val ROUTE = "settings/{planId}" }
    }

    // Pushed Screens
    data object EditPlant   : Screen("edit_plant?planId={planId}&plantId={plantId}") {
        companion object { const val ROUTE = "edit_plant?planId={planId}&plantId={plantId}" }
    }
    data object EditSection : Screen("edit_section?planId={planId}&sectionId={sectionId}") {
        companion object { const val ROUTE = "edit_section?planId={planId}&sectionId={sectionId}" }
    }
    data object PlantPicker : Screen("plant_picker/{planId}") {
        companion object { const val ROUTE = "plant_picker/{planId}" }
    }
}
```

### NavGraph-Struktur

```kotlin
NavHost(navController, startDestination = Screen.PlanList.route) {

    composable(Screen.PlanList.route) { PlanListScreen(navController) }

    // Plan-Kontext: Bottom-Nav-Shell mit 3 Tabs
    composable(Screen.Plan.ROUTE)     { PlanScreen(navController, planId) }
    composable(Screen.Library.ROUTE)  { PlantPickerScreen(navController, planId, standalone = true) }
    composable(Screen.Settings.ROUTE) { SettingsScreen(navController, planId) }

    // Pushed (kein Bottom-Nav)
    composable(Screen.EditPlant.ROUTE)   { EditPlantScreen(navController, planId, plantId) }
    composable(Screen.PlantPicker.ROUTE) { PlantPickerScreen(navController, planId, standalone = false) }
    composable(Screen.EditSection.ROUTE) { EditSectionScreen(navController, planId, sectionId) }
}
```

Die Bottom-Navigation ist in einem gemeinsamen `PlanScaffold`-Composable
gekapselt das alle drei Tab-Screens wrappen — so erscheint sie nur innerhalb
des Plan-Kontexts, nicht auf PlanListScreen oder pushed Screens.

### Bottom-Navigation (Plan-Kontext)

Drei Tabs: **Plan · Bibliothek · Einstellungen** — alle mit `planId` im Argument.
`saveState = true` + `restoreState = true` beim Tabwechsel.

### WebView-Holder in `MainActivity`

```kotlin
class MainActivity : ComponentActivity() {
    lateinit var printWebView: WebView
        private set

    override fun onCreate(...) {
        printWebView = WebView(this).apply {
            settings.javaScriptEnabled = false
        }
        // Nicht in den View-Tree eingefügt — nur für PrintManager genutzt
    }
}
```

Zugriff im Exporter (Session 10) über `LocalContext.current as MainActivity`.

### Testbares Ergebnis

Manuell:
- App-Start → PlanListScreen (Placeholder)
- Plan antippen → PlanScreen mit Bottom-Nav sichtbar
- Alle 3 Tabs schalten korrekt
- EditPlantScreen öffnen → Bottom-Nav verschwindet
- Back → Bottom-Nav wieder sichtbar
- Zurück von PlanScreen → PlanListScreen, keine Bottom-Nav

---

## Session 5 — PlanListScreen + PlanScreen Anzeige

**Ziel:** Pläne können aufgelistet und geöffnet werden. Ein realer Plan wird
aus Room geladen und korrekt dargestellt. Demo-Daten werden beim ersten
App-Start automatisch eingefügt.

### Dateien

```
ui/planlist/
├── PlanListScreen.kt
└── PlanListViewModel.kt

ui/plan/
├── PlanScreen.kt
├── PlanViewModel.kt
└── components/
    ├── MonthChipRow.kt
    ├── PlantRow.kt
    └── SectionHeader.kt
```

### `PlanListScreen.kt`

```kotlin
// Layout:
// TopAppBar: "Meine Gartenpläne" + "+" rechts (→ neuen Plan anlegen)
// LazyColumn der Pläne, sortiert nach year descending:
//   PlanCard: Jahr (groß) + Titel + Anzahl Pflanzen
//   Tap → navController.navigate(Screen.Plan(plan.id))
// Empty State: "Noch kein Plan. Tippe auf + um zu beginnen."
```

```kotlin
data class PlanListUiState(
    val plans: List<Plan> = emptyList(),
    val isLoading: Boolean = true
)

class PlanListViewModel(private val repo: PlanRepository) : ViewModel() {
    val uiState: StateFlow<PlanListUiState> = repo.getAllPlans()
        .map { PlanListUiState(plans = it.sortedByDescending { p -> p.year }, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanListUiState())

    suspend fun createPlan(year: Int, title: String): Int  // gibt planId zurück
    suspend fun deletePlan(plan: Plan)
}
```

Neuer Plan anlegen: `AlertDialog` mit Jahr-Feld und Titel-Feld → `createPlan()` →
direkt zu `PlanScreen(newPlanId)` navigieren.

Plan löschen: Swipe-to-dismiss auf PlanCard + Bestätigungsdialog
("Plan und alle Pflanzen unwiderruflich löschen?").

### `PlanViewModel.kt`

```kotlin
sealed interface PlanUiState {
    data object Loading : PlanUiState
    data object Empty   : PlanUiState    // Kein Plan in DB
    data class  Success(
        val plan: Plan,
        val sections: List<SectionWithPlants>,
        val monthEntries: Map<Int, List<MonthEntry>>  // plantId → entries
    ) : PlanUiState
}

class PlanViewModel(private val repo: PlanRepository) : ViewModel() {
    val uiState: StateFlow<PlanUiState> = ...
    val events: SharedFlow<PlanEvent> = ...  // NavigateTo, StartPrint
}
```

### First-Launch-Seeding

```kotlin
// In GardenDatabase.Builder:
.addCallback(object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        // Seed-Plan + 3 Sections + 10 Demo-Pflanzen aus PlantLibrary
        // Wird genau einmal ausgeführt
    }
})
```

Demo-Plan: "Mein Garten · 2026", Frost ~15. April / ~15. Oktober,
3 Sections (Gemüse & Kräuter, Kräuter, Dauerprojekte),
10 Pflanzen aus PlantLibrary mit realistischen Monats-Daten.

### `MonthChipRow.kt`

```kotlin
@Composable
fun MonthChipRow(
    entries: List<MonthEntry?>,  // 12 Einträge, null = leer
    onMonthClick: (month: Int) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        repeat(12) { month ->
            val entry = entries[month]
            MonthChip(
                modifier = Modifier.weight(1f),
                entry = entry,
                onClick = { onMonthClick(month) }
            )
        }
    }
}
```

Belegter Monat: Hintergrundfarbe aus `ActivityType.chipColor`, Label-Text.
Leerer Monat: Grauer Background, nur Monatsinitialen.

### Testbares Ergebnis

App-Start (frische Installation) → Demo-Plan mit Sections und Pflanzen sichtbar.
Monats-Chips haben korrekte Farben (Voranzucht=Amber, Ernte=Rot, etc.).
Scrollen durch lange Pflanzenliste performant (kein Jank).
`PlanUiState.Empty` → Placeholder-Text mit Aufforderung zum Anlegen.

---

## Session 6 — EditPlantScreen + BottomSheet

**Ziel:** Pflanzen können angelegt und bearbeitet werden. Alle Änderungen
persistieren nach Navigation zurück in den PlanScreen.

### Dateien

```
ui/editplant/
├── EditPlantScreen.kt
├── EditPlantViewModel.kt
└── components/
    ├── MonthGrid.kt
    └── ActivityTypeSheet.kt
```

### `EditPlantViewModel.kt`

```kotlin
data class EditPlantUiState(
    val name: String = "",
    val subtitle: String = "",
    val sectionId: Int? = null,
    val availableSections: List<Section> = emptyList(),
    val months: List<MonthEntry?> = List(12) { null },   // Index = Monat
    val isNew: Boolean = true,
    val isSaving: Boolean = false
)

class EditPlantViewModel(
    private val repo: PlanRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Lädt Plant + MonthEntries wenn plantId != null
    // Arbeitet auf lokalem State — kein Live-Write während Bearbeitung

    fun updateMonth(month: Int, entry: MonthEntry?)
    fun save(onSuccess: () -> Unit)  // upsert plant + upsert 12 entries
}
```

### `MonthGrid.kt` — 6×2 Grid

```kotlin
@Composable
fun MonthGrid(
    months: List<MonthEntry?>,
    onMonthClick: (month: Int) -> Unit
) {
    val monthNames = listOf("Jan","Feb","Mär","Apr","Mai","Jun",
                             "Jul","Aug","Sep","Okt","Nov","Dez")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (col in 0..5) {
                    val month = row * 6 + col
                    MonthButton(
                        modifier = Modifier.weight(1f),
                        name = monthNames[month],
                        entry = months[month],
                        onClick = { onMonthClick(month) }
                    )
                }
            }
        }
    }
}
```

### `ActivityTypeSheet.kt` — ModalBottomSheet

Inhalt des Bottom Sheets wenn ein Monat angetippt wird:
- Titel: Monatsname (z.B. "März")
- 5 Radio-Rows für ActivityType (Farbpunkt + Name)
- Notiz-Textfeld: **nur sichtbar wenn PFLEGE ausgewählt** (`AnimatedVisibility`)
- "Leer / nichts"-Option (löscht den Eintrag)
- Übernehmen-Button

Zustand des Sheets in `EditPlantViewModel` (welcher Monat offen,
welcher Typ ausgewählt, Label-Text).

### Testbares Ergebnis

Manuell: Neue Pflanze "Tomaten" anlegen.
- Name, Untertitel eingeben
- März als VORANZUCHT markieren → Chip wird gelb
- Mai als AUSPFLANZEN → blau
- Juli, August als ERNTE → rot
- Speichern → zurück auf PlanScreen → Pflanze mit korrekten Chips sichtbar
- Pflanze antippen → Daten korrekt vorausgefüllt
- Änderungen erneut speichern → persistiert nach App-Neustart

---

## Session 7 — PlanScreen CRUD + Drag & Drop

**Ziel:** Der Bearbeitungsmodus ist vollständig. Pflanzen und Sections können
gelöscht und umsortiert werden. FAB öffnet das Hinzufügen-Menü.

### Neue Elemente in `PlanScreen.kt`

**Bearbeitungsmodus-Toggle:**
```kotlin
var editMode by remember { mutableStateOf(false) }
// TopAppBar rechts: ✏-Icon toggled editMode
```

**Drag & Drop Reordering:**

Jetpack Compose bietet ab 1.7 `LazyColumn` mit
`androidx.compose.foundation.lazy.layout` Reorder-Unterstützung.
Alternative: `Modifier.draggable` + `onDragStopped` manuell.

Empfohlene Strategie:
1. In `editMode`: Drag-Handle (≡) links neben jeder Pflanze sichtbar
2. `LongPressDraggable` auf dem Handle
3. Optimistisches Update: lokale Liste sofort umsortieren, DB-Write
   via `viewModelScope.launch { repo.reorderPlants(newOrder) }`

**Löschen:**
- Pflanze: Löschen-Icon rechts → direkt löschen (kein Dialog; Undo-Snackbar)
- Section: Löschen-Icon im Header → `AlertDialog` "Section + alle Pflanzen löschen?"

**FAB-Menü (Normalmodus):**
```kotlin
// FAB tippt → kleines BottomSheet mit 3 Optionen:
// [🌿 Aus Bibliothek] [✏ Manuell anlegen] [📁 Section hinzufügen]
```

**FAB (Bearbeitungsmodus):**
Direkt "+ Section hinzufügen" → navigiert zu `EditSectionScreen`.

### `PlanViewModel` — neue Events

```kotlin
sealed interface PlanEvent {
    data class NavigateTo(val screen: Screen) : PlanEvent
    data class ShowSnackbar(val message: String) : PlanEvent
    data object StartPrint : PlanEvent
}
```

### Testbares Ergebnis

Manuell:
- Bearbeitungsmodus aktivieren → Drag-Handles und Löschen-Icons erscheinen
- Pflanze per Drag verschieben → neue Position bleibt nach App-Neustart
- Pflanze löschen → verschwindet sofort, Undo-Snackbar
- Section löschen → Dialog erscheint, nach Bestätigung verschwindet Section + alle Pflanzen
- FAB → Menü → "Manuell anlegen" → EditPlantScreen
- FAB → Menü → "Section hinzufügen" → EditSectionScreen

---

## Session 8 — PlantPickerScreen + EditSectionScreen

**Ziel:** Die Pflanzenbibliothek ist vollständig durchsuchbar.
Sections können angelegt und umbenannt werden.

### Dateien

```
ui/plantpicker/
├── PlantPickerScreen.kt
├── PlantPickerViewModel.kt
└── components/
    └── MiniMonthDots.kt

ui/editsection/
└── EditSectionScreen.kt
```

### `PlantPickerViewModel.kt`

```kotlin
data class PlantPickerUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val results: List<PlantTemplate> = PlantLibrary.all
)

class PlantPickerViewModel(private val libRepo: LibraryRepository) : ViewModel() {
    fun updateQuery(q: String)
    fun selectCategory(cat: String?)
    // Keine DB-Schreiboperation — Navigation zurück mit gewähltem Template
}
```

### `PlantPickerScreen.kt` — Layout

```
TopAppBar: "Pflanzenbibliothek" + Back-Button
Suchfeld (SearchBar oder OutlinedTextField)
Horizontale Chip-Leiste: [Alle] [Fruchtgemüse] [Wurzelgemüse] [Blattgemüse] [Kräuter] [Hülsenfrüchte] [Zwiebeln & Lauch]
LazyColumn:
  ├── Anbieterlabel + Kategorie
  ├── MiniMonthDots (12 × 5dp-Dot)
  └── Pfeil-Icon rechts
```

### `MiniMonthDots.kt`

```kotlin
@Composable
fun MiniMonthDots(months: List<MonthEntry?>) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        months.forEach { entry ->
            Box(
                Modifier
                    .size(width = 12.dp, height = 5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (entry != null) entry.type.chipColor
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
```

### Übergabe zur EditPlantScreen

Wenn Picker als Sub-Screen von PlanScreen aufgerufen:
```kotlin
// Navigation mit savedStateHandle-Ergebnis
navController.previousBackStackEntry
    ?.savedStateHandle
    ?.set("selected_template", templateId)
navController.popBackStack()
```

PlanViewModel liest das Ergebnis und navigiert weiter zu EditPlantScreen
mit vorausgefüllten Daten aus dem Template.

### `EditSectionScreen.kt`

```kotlin
@Composable
fun EditSectionScreen(navController: NavController, sectionId: Int?) {
    // Lädt Section wenn sectionId != null (Umbenennen)
    // Neuer Section wenn null
    // Einziges Element: OutlinedTextField für Titel
    // TopAppBar: "Section" + Speichern-Button rechts
    // Tastatur öffnet sich automatisch (FocusRequester)
    // Speichern → upsert → popBackStack()
}
```

Kein eigenes ViewModel nötig — direkter Repository-Aufruf via
`rememberCoroutineScope()` reicht für den simplen Use-Case.

### Testbares Ergebnis

Manuell:
- Bibliothek öffnen → 37+ Einträge sichtbar
- "tom" eingeben → nur Tomaten + ähnliche sichtbar
- Kategorie "Kräuter" → nur Kräuter
- Basilikum antippen → EditPlantScreen mit vorausgefüllten Daten öffnet sich
- Speichern → Pflanze im Plan
- Neue Section anlegen → Section erscheint in PlanScreen
- Section umbenennen → neuer Name sichtbar

---

## Session 9 — SettingsScreen + Planverwaltung

**Ziel:** Plan-Metadaten sind editierbar und fließen sofort in TopAppBar und
PDF-Export. Der SettingsScreen ermöglicht auch Plan-Kopie (Jahreswechsel)
und Löschen. Der Demo-Plan beim ersten Start ist vollständig.

### Dateien

```
ui/settings/
├── SettingsScreen.kt
└── SettingsViewModel.kt
```

### `SettingsViewModel.kt`

```kotlin
data class SettingsUiState(
    val plan: Plan? = null,
    val appVersion: String = ""
)

class SettingsViewModel(private val repo: PlanRepository) : ViewModel() {
    val uiState: StateFlow<SettingsUiState>
    suspend fun updatePlanTitle(title: String)
    suspend fun updatePlanYear(year: Int)
    suspend fun updateFrostLast(value: String)
    suspend fun updateFrostFirst(value: String)
    suspend fun updateClimateZone(value: String)
}
```

### `SettingsScreen.kt` — Layout

```
TopAppBar: "Einstellungen"
ListItems (gruppiert):

Gruppe "Dieser Plan"
  ├── Plan-Titel  [Garten Brandenburg 2026]  →  Dialog: TextField + Bestätigen
  └── Planjahr    [2026]                     →  Dialog: NumberPicker o. TextField

Gruppe "Klima & Standort"
  ├── Letzter Spätfrost   [~15. April]       →  Dialog: TextField
  ├── Erster Herbstfrost  [~15. Oktober]     →  Dialog: TextField
  └── Klimazone / Boden   [7a · Lehmboden]   →  Dialog: TextField (optional)

Gruppe "Planverwaltung"
  ├── Plan kopieren für nächstes Jahr  →  Dialog: neues Jahr eingeben → Plan kopieren
  │   (Struktur: Sections + Pflanzen + Monats-Einträge werden 1:1 übernommen)
  └── Diesen Plan löschen              →  AlertDialog mit Warnung → PlanListScreen

Gruppe "Datensicherung"                        (Implementierung in Session 11)
  ├── Als JSON exportieren  →  SAF Intent ACTION_CREATE_DOCUMENT
  └── Aus JSON importieren  →  SAF Intent ACTION_OPEN_DOCUMENT

Gruppe "Export"
  └── Plan drucken / Als PDF  →  Aufruf des PDF-Exports (Session 10)

Gruppe "App"
  └── Version  [1.0.0 · GPL-3.0 · Open Source]  (read-only)
```

Änderungen werden sofort in die DB geschrieben (kein "Speichern"-Button auf Screen-Ebene).

### Plan kopieren (Jahreswechsel)

```kotlin
suspend fun copyPlanForYear(sourcePlanId: Int, newYear: Int): Int {
    // 1. Neuen Plan anlegen (gleiches title/frost/climate, neues year)
    // 2. Sections kopieren (neue IDs)
    // 3. Plants kopieren (neue IDs, neue sectionId-Referenzen)
    // 4. MonthEntries kopieren (neue IDs, neue plantId-Referenzen)
    // 5. Neue planId zurückgeben → direkt zu PlanScreen(newPlanId) navigieren
}
```

Alle Monats-Einträge werden übernommen — der User passt Abweichungen danach an.
Das entspricht dem Konzept "Planungsbuch für das neue Jahr aus dem Vorjahr kopieren".

### Verbesserter Demo-Plan

Beim ersten App-Start (`GardenDatabase.Callback.onCreate`):

```
Plan: "Mein Garten · 2026"
Frost: "~15. April" / "~15. Oktober"
Klimazone: "7a"

Section 1: 🥬 Gemüse & Kräuter
  - Tomaten (Voranzucht: Mär-Apr, Auspfl: Mai, Ernte: Jul-Sep)
  - Zucchini (Voranz: Apr, Auspfl: Mai, Ernte: Jun-Sep)
  - Möhren (Direktsaat: Mär-Apr, Ernte: Aug-Okt)
  - Knoblauch (Pflege: Jan-Mai, Ernte: Jun, Setzen: Okt-Nov)
  - Bohnen (Direktsaat: Mai-Jun, Ernte: Jul-Sep)

Section 2: 🌿 Kräuter
  - Basilikum (Voranz: Mär-Apr, Auspfl: Mai, Ernte: Jun-Aug)
  - Petersilie (Direktsaat: Mär-Apr+Jul, Ernte: Mai-Okt)
  - Schnittlauch (Direktsaat: Mär, Ernte: Apr-Sep)

Section 3: 🎋 Dauerprojekte
  - Bambus (Pflege: Mär-Jul, Düngung: Okt)
  - Kompost (Pflege: Jan-Mär, Ausbringen: Apr+Sep)
```

### Testbares Ergebnis

Manuell:
- Einstellungen öffnen → alle Felder sichtbar mit aktuellen Werten
- Plan-Titel ändern → TopAppBar in PlanScreen aktualisiert sofort (Flow)
- Frostdaten ändern → nach Session 10 im PDF sichtbar
- App schließen + öffnen → alle Einstellungen persistiert
- Frische Installation → Demo-Plan sichtbar und vollständig

---

## Session 10 — HtmlExporter + PDF-Export

**Ziel:** Tipp auf das Drucker-Icon öffnet einen korrekt formatierten A4-PDF
im Android-Druckdialog. "Als PDF speichern" funktioniert ohne Fehler.

### Dateien

```
export/
└── HtmlExporter.kt
```

### `HtmlExporter.kt`

```kotlin
object HtmlExporter {

    fun planToHtml(
        plan: Plan,
        sections: List<SectionWithPlants>,
        monthEntries: Map<Int, List<MonthEntry>>  // plantId → entries
    ): String = buildString {
        append(htmlHead())          // DOCTYPE, <style> mit @page, print-CSS
        append(header(plan))        // Titel, Frost, Klimazone
        append(legend())            // Farblegende
        append(table(sections, monthEntries))
        append(footer(plan))        // Fußzeile mit Metadaten
        append("</body></html>")
    }

    private fun htmlHead(): String  // CSS direkt aus gartenplaner_2026.html portiert
    private fun header(plan: Plan): String
    private fun legend(): String

    private fun table(
        sections: List<SectionWithPlants>,
        monthEntries: Map<Int, List<MonthEntry>>
    ): String = buildString {
        append("<table>")
        append(colgroup())          // col.lc 9.5% + 12x col.mc 7%
        append(tableHeader())       // Monatskopfzeile mit Emoji-Jahreszeiten
        sections.forEach { sec ->
            append(sectionRow(sec.section.title))
            sec.plants.forEach { plant ->
                append(plantRow(plant, monthEntries[plant.id] ?: emptyList()))
            }
        }
        append("</table>")
    }

    private fun plantRow(plant: Plant, entries: List<MonthEntry>): String {
        val byMonth = entries.associateBy { it.month }
        return buildString {
            append("<tr>")
            append("""<td class="lbl">${plant.name.escapeHtml()}""")
            if (plant.subtitle.isNotBlank())
                append("""<em>${plant.subtitle.escapeHtml()}</em>""")
            append("</td>")
            for (m in 0..11) {
                val entry = byMonth[m]
                if (entry != null) {
                    val cssClass = entry.type.toCssClass()
                    append("""<td class="$cssClass">${entry.label.escapeHtml()}</td>""")
                } else {
                    append("<td></td>")
                }
            }
            append("</tr>")
        }
    }

    private fun ActivityType.toCssClass() = when (this) {
        VORANZUCHT  -> "av"
        DIREKTSAAT  -> "ad"
        AUSPFLANZEN -> "ap"
        ERNTE       -> "ae"
        PFLEGE      -> "apg"
    }

    private fun String.escapeHtml() =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
```

CSS-Klassen und Farbwerte exakt aus `gartenplaner_2026.html` übernehmen —
das Template ist bereits print-getestet.

### Integration in `MainActivity`

```kotlin
// Event aus PlanViewModel
lifecycleScope.launch {
    planViewModel.events.collect { event ->
        when (event) {
            is PlanEvent.StartPrint -> {
                val html = HtmlExporter.planToHtml(
                    event.plan, event.sections, event.monthEntries
                )
                printWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                printWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val adapter = view.createPrintDocumentAdapter(
                            "GartenPlan_${event.plan.year}"
                        )
                        val pm = getSystemService(PrintManager::class.java)
                        pm.print("GartenPlan_${event.plan.year}", adapter,
                                 PrintAttributes.Builder().build())
                    }
                }
            }
        }
    }
}
```

> WebView muss auf den `onPageFinished`-Callback warten bevor
> `createPrintDocumentAdapter` aufgerufen wird — sonst leere PDF.

### Testbares Ergebnis

Manuell (Gerät mit physischem oder virtuellem Drucker):
- Drucker-Icon tippen → Android-Druckdialog öffnet sich
- Vorschau zeigt A4-Querformat
- Alle Sections und Pflanzen sichtbar
- Farben korrekt (Hintergrundfarben erscheinen, kein Grau statt Farbe)
- "Als PDF speichern" → valides PDF in Downloads
- Plan-Metadaten (Titel, Frost, Klimazone) im PDF-Header sichtbar

---

## Session 11 — JSON-Backup + Polish + Release

**Ziel:** JSON-Backup/Restore funktioniert. Release-fähiges APK. F-Droid-Metadaten
vorhanden. Keine Abstürze bei Edge-Cases. App-Icon gesetzt.

### JSON-Backup (PlanExporter + PlanImporter)

```
data/backup/
├── PlanExporter.kt    — Plan → JSON-String (org.json.JSONObject, keine externe Lib)
└── PlanImporter.kt    — JSON-String → Room (neue IDs, kein Überschreiben)
```

**Export-Flow:**
```kotlin
// In SettingsViewModel:
fun exportPlan(planId: Int, uri: Uri) {
    val json = PlanExporter.export(plan, sections, monthEntries)
    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
}
```

**Import-Flow:**
```kotlin
// PlanImporter.import() gibt Result<Int> zurück (neue planId oder Fehler)
// Bei Erfolg: direkt zu PlanScreen(newPlanId) navigieren
// Bei Fehler: Snackbar mit lesbarer Fehlermeldung
```

**JSON-Format** (self-contained, portierbar — keine IDs):
```json
{
  "version": 1,
  "plan": { "title": "...", "year": 2026, "frostInfoLast": "...",
            "frostInfoFirst": "...", "climateZone": "..." },
  "sections": [
    { "title": "🥬 Gemüse & Kräuter", "order": 0,
      "plants": [
        { "name": "Tomaten", "subtitle": "Voranzucht", "order": 0,
          "months": [
            { "month": 2, "type": "VORANZUCHT", "label": "Voranz." }
          ]
        }
      ]
    }
  ]
}
```

Parser: `org.json.JSONObject` (AOSP, kein Gradle-Dependency nötig).
Validierung: `version`-Feld prüfen, Pflichtfelder auf null checken,
bei Fehler `Result.failure(IllegalArgumentException("Ungültige Backup-Datei: ..."))`.

SAF-Integration in `MainActivity`:
```kotlin
val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    uri?.let { settingsViewModel.exportPlan(activePlanId, it) }
}
val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let { settingsViewModel.importPlan(it) }
}
```

### Empty States

```kotlin
// PlanScreen — kein Plan
EmptyState(
    icon = Icons.Outlined.Yard,
    title = "Noch kein Gartenplan",
    body = "Tippe auf + um deinen ersten Plan zu starten.",
    action = "Plan erstellen"
)

// PlanScreen — Plan vorhanden, aber keine Sections
EmptyState(
    icon = Icons.Outlined.GridOn,
    title = "Keine Sections",
    body = "Füge eine Section hinzu (z.B. \"🥬 Gemüse\").",
    action = "Section hinzufügen"
)

// PlantPickerScreen — kein Suchergebnis
EmptyState(
    icon = Icons.Outlined.SearchOff,
    title = "Keine Treffer",
    body = "Versuche einen anderen Suchbegriff."
)
```

### Animationen

- Screen-Übergänge: `EnterTransition` / `ExitTransition` in NavGraph
  (Slide für pushed Screens, Fade für Tab-Wechsel)
- Edit-Mode-Toggle: `AnimatedVisibility` für Drag-Handles und Löschen-Icons
- Bottom-Sheet-Öffnen: Material 3 Standard (schon eingebaut)

### App-Icon

`res/mipmap-anydpi-v26/ic_launcher.xml` — Adaptive Icon:
- Foreground: stilisiertes Blatt-SVG in Weiß
- Background: `#2A6A2A` (GreenPrimary)
- Monochrom-Variante für Android 13+

### Predictive Back Support

```kotlin
// In AndroidManifest.xml
android:enableOnBackInvokedCallback="true"

// Navigation-Compose unterstützt Predictive Back ab 2.7 automatisch
// Kein manueller Code nötig
```

### ProGuard / R8

```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Enum (ActivityType als TypeConverter)
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
```

### F-Droid Fastlane-Struktur

```
fastlane/metadata/android/
└── de-DE/
    ├── title.txt              "GartenPlaner"
    ├── short_description.txt  (80 Zeichen max)
    ├── full_description.txt   (4000 Zeichen max)
    ├── changelogs/
    │   └── 1.txt              "Erste Version: Jahresplaner, Bibliothek, PDF-Export."
    └── images/
        ├── icon.png           (512×512)
        ├── featureGraphic.png (1024×500)
        └── phoneScreenshots/  (mind. 2, max. 8)
```

### Release-Checkliste

```
[ ] build.gradle: versionCode=1, versionName="1.0.0"
[ ] minSdk=26, targetSdk=34
[ ] Keine proprietären Google-Libraries (nur AOSP/Jetpack)
[ ] Fonts gebündelt (nicht downloadable)
[ ] Keine INTERNET-Permission in AndroidManifest.xml
[ ] READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE nicht nötig (SAF verwendet URIs)
[ ] exportSchema=true, schemas/ committed
[ ] GPL-3.0 LICENSE-Datei im Root
[ ] CHANGELOG.md mit v1.0-Eintrag
[ ] JSON-Backup/Restore manuell getestet (Export → Neuinstallation → Import)
[ ] Release-APK signiert (Keystore sicher verwahrt)
[ ] APK mit apksigner verifiziert
[ ] F-Droid-Metadaten vollständig
```

### Testbares Ergebnis

- Frische Installation auf Gerät: kein Crash beim Demo-Plan-Seeding
- Alle Edge-Cases ohne Crash: leerer Plan, Section ohne Pflanzen, Pflanze ohne Monate
- App-Icon korrekt auf Home-Screen (adaptiv auf Android 8+)
- `./gradlew assembleRelease` mit signierten APK erfolgreich
- APK-Größe < 8 MB (keine aufgeblähten Abhängigkeiten)
- Predictive Back auf Android 13+ flüssig

---

## Abhängigkeitsdiagramm

```
S1 (Setup/Theme)
  └── S2 (Datenmodell/Room)
        └── S3 (Repositories/Bibliothek)
              └── S4 (Navigation/Shells)
                    └── S5 (PlanListScreen + PlanScreen Anzeige)
                          ├── S6 (EditPlantScreen)
                          │     └── S7 (PlanScreen CRUD)
                          ├── S8 (PlantPicker + EditSection)   ← parallel zu S6/S7 möglich
                          └── S9 (SettingsScreen + Planverwaltung)

S7 + S8 + S9 → S10 (PDF-Export)
S10          → S11 (JSON-Backup + Polish + Release)
```

Sessions 8 und 9 können **parallel** zu 6/7 bearbeitet werden.
Sessions 5 → 6 → 7 müssen **sequenziell** bleiben.
