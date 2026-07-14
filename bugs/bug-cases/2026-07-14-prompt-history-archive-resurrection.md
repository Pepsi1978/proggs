# Prompt-Historie: archivierte Einträge kehren aus Drive zurück

## Symptom

Bei mehr als 100 Einträgen wurden ältere Prompts in Markdown archiviert. Die aktive JSON-Datei
vergaß diese IDs anschließend vollständig. Eine ältere Drive-Kopie konnte dieselben Einträge beim
nächsten Start wieder als aktiv liefern und erneut ins Markdown-Archiv schreiben.

## Ursache

Der Merge war additiv, aber das Archivieren hatte keinen synchronisierten Tombstone. Abwesenheit
in der aktiven JSON-Datei konnte deshalb nicht von einem noch nie gesehenen Cloud-Eintrag
unterschieden werden. Die Archive konnten dadurch wachsen, obwohl keine neuen Prompts entstanden.

## Fix

- `archivedAt` als monotonen Tombstone in das gemeinsame Windows-/macOS-JSON-Schema aufgenommen.
- Archivierte Datensätze bleiben für Cloud-Wiederherstellung im JSON, werden aber aus der aktiven
  UI ausgeblendet.
- Merge erhält einen Tombstone auch dann, wenn eine neuere Inhaltsrevision gewinnt.
- Start-Merge lädt aktive und archivierte Datensätze und schreibt neue Tombstones zurück zu Drive.
- Markdown-Blöcke tragen künftig eine ID; vorhandene Legacy-Blöcke werden per Inhalt dedupliziert.
- .NET-Minimaldatum `0001-01-01` wird auf macOS kompatibel als Legacyrevision behandelt.

## Verifikation

- TVO und CVO mit `TreatWarningsAsErrors=true`: `0 Warnungen, 0 Fehler`.
- Temporärer Merge-Test: aktive/archivierte Konflikte, Groß-/Kleinschreibung und kaputtes Cloud-JSON bestanden.
- TVO `1.4.79` und CVO `2.1.64` neu deployed und versionsgeprüft.
- Reale History auf das neue Schema migriert; anschließender Drive-Upload erfolgreich.
- Swift-Quellen statisch auf Parität geprüft; macOS-Build war unter Windows nicht verfügbar.

## Referenz

- Fix-Commit: `fc232ec7f #47912 - Preserve cross-platform history archives`
