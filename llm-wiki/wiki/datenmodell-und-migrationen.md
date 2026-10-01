# Datenmodell und Migrationen

## Hierarchie und aktueller Schema-Stand (Version 3)

Logische Hierarchie `Plan → Section → Plant → MonthEntry`. **Aktueller Code-Stand ist Version 3**
(`@Database(version = 3, exportSchema = true)`, `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)`, **kein**
`fallbackToDestructiveMigration`). Jede künftige Schema-Änderung braucht Version 4 + eigene Migration +
Migrationstest + neu exportiertes `4.json` — ohne Migration crasht die App beim Öffnen
(`IllegalStateException`).
Quelle: `app/.../data/db/GardenDatabase.kt`.

- **v1** (`schemas/.../1.json`): `plans(id, year, title, frost_info_last, frost_info_first,
  climate_zone)`; `sections(id, plan_id→plans CASCADE, title, order)`; `plants(id, section_id NOT
  NULL→sections CASCADE, name, subtitle, order, from_library)`; `month_entries(id, plant_id→plants
  CASCADE, month, type, label)`. Reine Kaskade — entspricht dem CLAUDE.md-Bild, gilt aber nur für v1.
- **v2**: `plants` bekommt `plan_id NOT NULL` (FK→plans, CASCADE, eigener Index); `section_id` wird
  **nullable** mit `ON DELETE SET NULL` statt CASCADE. **Seit v2 löscht das Löschen einer Section ihre
  Pflanzen nicht mehr** — sie bleiben ohne Section im Plan erhalten („ungruppierte Pflanzen",
  `PlantDao.getUnsectionedPlants(planId)`). Migration 1→2 baut `plants` neu (SQLite kennt kein `ALTER
  COLUMN`): `plants_new` anlegen, per `INSERT…SELECT…JOIN sections` befüllen (ein `INNER JOIN` verwirft
  dabei Pflanzen ohne gültige Section!), alte Tabelle droppen, umbenennen, Indizes neu anlegen.
- **v3**: `month_entries` bekommt `plan_id` (`ADD COLUMN … NOT NULL DEFAULT 0`, Backfill per
  `UPDATE … SELECT plan_id FROM plants WHERE plants.id = month_entries.plant_id`, eigener Index). Kein
  FK von `month_entries.plan_id` auf `plans` — Löschung läuft über die `plants`-Kaskade. Grund: eine
  direkte `plan_id`-Abfrage auf `month_entries` ohne JOIN über `plants`.

**CLAUDE.md und Teile von IMPLEMENTATION beschreiben weiterhin v1 (volle Kaskade überall)** — bei
Widerspruch gilt der Schema-Export + Code. Siehe [[offene-fragen]] für die zugehörigen
Widerspruchskarten.
Quelle: `schemas/de.gartenplaner.data.db.GardenDatabase/{1,2,3}.json`,
`app/.../data/db/GardenDatabase.kt#MIGRATION_1_2/#MIGRATION_2_3`, `app/.../data/model/{Plant,MonthEntry}.kt`.

## Wichtige Fallstricke im Datenmodell

- `month` ist **0-basiert** (0=Jan … 11=Dez); ActivityType wird **als String** (Enum-Name) gespeichert
  (`Converters`: `type.name` ↔ `ActivityType.valueOf(name)`) — ein umbenannter Enum-Wert lässt
  `valueOf()` beim Lesen alter Zeilen/Backups mit `IllegalArgumentException` abstürzen.
- `order` als Spaltenname ist ein SQL-Schlüsselwort — Queries brauchen Backticks (`` ORDER BY `order` ``).
- R8/ProGuard braucht `keep`-Regeln für `RoomDatabase`-Subklassen, `@Entity`-Klassen und
  `values()/valueOf()` der Enums, sonst bricht Minifizierung den Name↔Enum-Weg im Release-Build.
- `Plan`-Frostdaten (`frostInfoLast/First`) sind **Freitext-Strings** („~15. April"), keine Datumswerte
  — keine Berechnung möglich.
Quelle: `CLAUDE.md#ActivityType`, `app/.../data/db/Converters.kt`, `IMPLEMENTATION.md#ProGuard/R8`.

## JSON-Backup

Format: `{"version":1, "plan":{...}, "sections":[{title, order, plants:[{name, subtitle, order,
months:[{month,type,label}]}]}]}`. IDs werden nie exportiert; Import erzeugt **immer einen neuen Plan**
(kein Überschreiben) und neue IDs auf jeder Ebene. SAF (`ACTION_CREATE_DOCUMENT` /
`ACTION_OPEN_DOCUMENT`), keine Storage-Permissions nötig. `PlanImporter.import()` nutzt
`runCatching { ... }` für `Result<Int>`, prüft `version == 1` — **aber läuft nicht in einer
DB-Transaktion**: scheitert z. B. die dritte Pflanze an einem unbekannten `type`
(`ActivityType.valueOf`), bleiben Plan und die bisher eingefügten Sections/Pflanzen als **halber Plan**
in der DB, obwohl `Result.failure` zurückkommt. `fromLibrary` wird beim Import nie gesetzt (immer
`false`); `month` wird nicht auf den Bereich 0–11 geprüft; ein optionales Array
`unsectioned_plants` erweitert das Format ohne Versionssprung.
Quelle: `app/.../data/backup/PlanImporter.kt`.

`copyPlanForYear(sourcePlanId, newYear)` (Plan fürs nächste Jahr kopieren) läuft im Code dagegen
**bereits in `db.withTransaction { … }`** — ein älteres Schulden-Dokument listet das Fehlen dieser
Transaktion noch als offen (siehe [[offene-fragen]]).
Quelle: `app/.../data/repository/PlanRepository.kt#copyPlanForYear`.

## Demo-Seed

Seeding läuft in `RoomDatabase.Callback.onCreate` (nur bei **Neuanlage** der DB, nicht nach
Migrationen) und startet asynchron auf `Dispatchers.IO` — fire-and-forget ohne Fehlerbehandlung; ist
`INSTANCE` beim Aufruf noch `null`, wird stillschweigend nicht geseedet. Demo-Plan „Mein Garten · 2026"
(Klimazone 7a) mit 3 Sections, 10 Pflanzen. Nicht-Typ-Tätigkeiten wie „Setzen", „Düngung", „Ausbringen"
sind **Labels auf einem der 5 festen ActivityTypes** (z. B. Knoblauch „Setzen" = `DIREKTSAAT`-Label,
Kompost „Ausbringen" = `AUSPFLANZEN`-Label) — die Farbe folgt dem Typ, die fachliche Bedeutung dem
Label. Bambus/Kompost stammen nicht aus `PlantLibrary` (Widerspruch zur Doku-Aussage „10 Pflanzen aus
PlantLibrary").
Quelle: `app/.../data/db/GardenDatabase.kt#SeedCallback`, `app/.../data/seed/DemoSeed.kt`.

`PlantLibrary` ist eine statische Kotlin-Liste (kein Asset, keine DB) mit **37 Einträgen** in 6
Kategorien (Fruchtgemüse 8, Wurzelgemüse 6, Blattgemüse 6, Kräuter 8, Hülsenfrüchte 4, Zwiebeln & Lauch
5); `PlantLibraryTest` prüft `size >= 37` — die Bibliothek liegt also genau auf der Testuntergrenze,
jede Entfernung eines Eintrags lässt den Test scheitern. Zwei unterschiedliche Suchsemantiken:
`PlantLibrary.search` matcht Name ODER Kategorie, `LibraryRepository.search(query, category)` filtert
erst nach Kategorie, dann nur nach Name.
Quelle: `app/.../data/library/PlantLibrary.kt`, `app/src/test/.../PlantLibraryTest.kt`.

## Verweise

[[architektur]] · [[konventionen-und-entscheidungen]] · [[offene-fragen]]
