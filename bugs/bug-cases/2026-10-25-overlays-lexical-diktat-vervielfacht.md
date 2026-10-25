# Overlays: Diktat im Kimi-Lexical-Editor vervielfacht (25.10.2026)

> Behoben in Overlays 0.6.17. Auf Kimi.ai direkt im Editor reproduziert.

## Symptom

Jede Spracheingabe der Chrome-Erweiterung erschien im Kimi-Eingabefeld etwa zwanzigmal statt einmal.

## Ursache

`setViaPaste` schrieb den vollständigen Text zuerst per `innerHTML` in den Editor und feuerte danach künstliche `insertReplacementText`-Ereignisse. Kimis Lexical-Editor übernahm denselben Text dadurch erneut. `reactNudge` und der Clipboard-Fallback wiederholten das mit dem bereits vervielfachten Feldinhalt.

Die direkte Reproduktion auf Kimi.ai zeigte: Nach `innerHTML` stand „Probe einmal“ im Feld; nach dem künstlichen Input-Ereignis stand „Probe einmalProbe einmal“ darin.

## Behebung

- Lexical-Editoren (`data-lexical-editor="true"`) erhalten den Text durch genau ein `ClipboardEvent("paste")` mit `DataTransfer`; kein `insertReplacementText` und kein `reactNudge` mehr.
- Ein nativer Clipboard-Paste bleibt als Fallback erhalten.
- Schreibvorgänge auf dasselbe Feld werden seriell ausgeführt. Ein bereits exakt erreichtes Ziel wird nicht erneut geschrieben, damit doppelt ausgelöste Abschlüsse nichts vervielfachen.
- Andere Eingabefeldtypen und alle Overlay-Funktionen bleiben unverändert.

## Dateien

- `overlays/src/editable.js`
- `overlays/manifest.json`
