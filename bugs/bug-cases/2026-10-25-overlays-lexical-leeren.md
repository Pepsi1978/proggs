# Overlays: X markiert Kimi-Text nur statt ihn zu löschen (25.10.2026)

> Behoben in Overlays 0.6.18. Auf Kimi.ai direkt im Editor verifiziert.

## Symptom

Der X-Knopf der Chrome-Erweiterung markiert den gesamten Text im Kimi-Eingabefeld, entfernt ihn aber nicht.

## Ursache

Das Leeren verwendet denselben Lexical-Schreibweg wie das Einfügen. Ein `ClipboardEvent("paste")` mit leerem Text ersetzt die zuvor markierte Auswahl nicht; die Auswahl bleibt lediglich bestehen.

## Behebung

- Bei leerem Zieltext wird nach dem Markieren ein `beforeinput` mit `inputType: "deleteContentBackward"` ausgelöst.
- Danach wird geprüft, ob das Feld tatsächlich leer ist.
- Natives `document.execCommand("delete")` dient als Fallback.
- Nichtleere Texte verwenden weiterhin das einmalige Paste-Ereignis aus Version 0.6.17.

## Dateien

- `overlays/src/editable.js`
- `overlays/manifest.json`
