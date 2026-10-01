# SCHEMA — Konventionen für dieses LLM-Wiki (GartenPlaner)

Karpathy-Muster: `raw/` (Verweise auf Quellen im Repo, keine Kopien), `wiki/` (gepflegtes,
verlinktes Projektwissen), diese Schema-Datei. Zielgruppe: ein LLM (oder Mensch), das sich schnell in
GartenPlaner orientieren muss, ohne alle Planungsdokumente und den Code gegeneinander zu lesen.

## Besonderheit dieses Projekts: Doku und Code sind auseinandergelaufen

GartenPlaner hat mehrere Planungsdokumente (`CLAUDE.md`, `ARCHITECTURE.md`, `IMPLEMENTATION.md`,
`GartenPlaner_Konzept.md`, `V1_technische_schulden.md`, `performance_fix.md`, `performance_fix_2.md`)
aus verschiedenen Projektphasen, die sich an vielen Stellen widersprechen, weil nur ein Teil nach
Code-Änderungen nachgezogen wurde. **Bei Widerspruch zwischen Doku und tatsächlichem Code gilt der
Code** — Wiki-Seiten kennzeichnen das explizit und verweisen auf `wiki/offene-fragen.md`, statt den
Widerspruch stillschweigend aufzulösen.

## Seitentypen

Wie in den Ausgangs-Wissenskarten: **Architektur**, **Konvention**, **Entscheidung**, **Datenmodell**,
**Performance**, **Technische Schuld**, **Offene Frage / Widerspruch**.

## Verlinkung

`[[Dateiname-ohne-Endung]]` (Obsidian-Stil). Jede neue Seite kommt mit Einzeiler in `index.md`.

## Quellenangaben

Repo-Pfad + Abschnitt/Datei relativ zur Repo-Wurzel `/home/output42/CursorProjekte/GartenPlaner`, z. B.
`CLAUDE.md#Unveränderliche Invarianten`, `app/src/main/.../PlanRepository.kt#copyPlanForYear`,
`schemas/de.gartenplaner.data.db.GardenDatabase/3.json`. Eine Aussage ohne Quelle ist ein Lint-Fehler.
`raw/SOURCES.md` listet die Quelldokumente.

## Arbeitsablauf

1. **ingest** — neue/geänderte Quelle (Doku oder Code) → betroffene Seite aktualisieren, dabei
   kennzeichnen, ob die Aussage aus Doku oder aus tatsächlichem Code stammt. Widerspricht ein neuer Fund
   einer bestehenden Aussage, bleibt die alte mit Datum stehen, der Widerspruch geht nach
   `wiki/offene-fragen.md`. `index.md`/`log.md` fortschreiben.
2. **query** — nur mit Wiki + den in `raw/SOURCES.md` verlinkten Repo-Dateien beantworten; unbelegt
   heißt „nicht belegt“.
3. **lint** — tote Links, Seiten ohne Quelle, Aussagen, die der Code inzwischen widerlegt (so gefunden:
   mehrere GP-C*-Befunde in `wissen.md` lösten ältere GP-W*-Widersprüche bereits auf — nachziehen statt
   doppelt führen).

## Abgrenzung

- Nur Projektwissen, keine Inhalte aus Testaufgaben/Prüffragen der TestSuite.
- Keine Geheimnisse: `keystore.propertys`/`*.properties` mit Schlüsseln, `local.properties`,
  Signierungsschlüssel werden nie zitiert — auch nicht als Dateiname mit Inhalt, höchstens als Hinweis
  „existiert, nicht lesen“.
