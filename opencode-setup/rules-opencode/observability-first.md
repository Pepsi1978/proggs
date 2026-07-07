Observability-First: Sonden-, Logging- & Live-Monitoring-Standard

Bei JEDEM qualifizierten Software-Projekt ist der ERSTE Schritt — vor jeder Feature-Arbeit — die Beobachtungsschicht. Qualifiziert = mehr als ein Mini-Fix: mehr als 1 Datei, App mit Oberflaeche, eigene Logik/Zustand/Persistenz/IO, oder ueber ~150 Zeilen / mehr als 1 Sitzung. Sonden weglassen nur bei Wegwerf-Skript oder Mini-Fix (dann in 1 Satz begruenden). Im Zweifel: Sonden einbauen.

## Die drei Bausteine
1. **Strukturiertes Logging (JSON-Lines):** je Eintrag `ts, level, module, fn, msg, ctx`, bei Fehlern zusaetzlich `trace`. Fester Log-Pfad, beim Start EINMAL ausgeben (`Log: <Pfad>`). Log-Rotation. Auf stdout spiegeln. Log-Level umschaltbar (Default INFO).
2. **Globaler Fehler-Faenger:** zentraler Handler faengt unbehandelte Crashes und loggt vollen Kontext, BEVOR etwas stirbt. Nichts stirbt still.
3. **Logik-Sonden (Herzstueck — fangen STILLE Fehler, nicht nur Crashes):** Hilfsfunktion `probe(bedingung, meldung, kontext)` prueft eine Annahme und loggt deren Verletzung (WARN/ERROR mit Kontext), ohne im Normalbetrieb zu crashen. Pruefe: Vor-/Nachbedingungen an Kernfunktionen, Invarianten, Zustandsuebergaenge (A->B), Sanity-/Range-Checks (NaN/negativ/ausserhalb Grenzen), Entscheidungs-Logging an wichtigen Verzweigungen.

## Live-Monitoring
- Android: `adb logcat -s <TAG>`
- Windows: `Get-Content <log> -Wait -Tail 20`
- macOS/Linux: `tail -f <log>`

Frank bedient die App, Claude liest die frisch aufgelaufenen Zeilen mit und korreliert Anomalien direkt mit der Aktion.

## Live-Logik-Sonden (Intent-Verifikation)
Aus einem Bau-Prompt mit klarer Verhaltensabsicht jede beabsichtigte Handlung als benannten Checkpoint verdrahten, der zur Laufzeit "erwartet vs. tatsaechlich" in einen EIGENEN Kanal schreibt (`kind:CHECKPOINT`, Felder `step, intent, expected, actual, ok, ctx`). Frank startet die App, Claude liest den Kanal live mit und bestaetigt Schritt fuer Schritt, ob die Logik so angekommen ist wie gemeint. `ok:false` SOFORT melden + an der Wurzel fixen. Zuruf: "starte den Live-Logik-Check".

## Lebende Sonden (Co-Evolution)
Jeder Commit, der Logik aendert/hinzufuegt, zieht die Sonden MIT: neue Logik -> neue Sonden; geaenderte Logik -> Sonde anpassen; geloeschte Logik -> tote Sonde entfernen. Veraltete Sonden erzeugen Fehlalarme (Stale-Probe-Schutz). Ziel: jeder fachliche Schritt ist im Nachhinein per Log debuggbar; eine uninstrumentierte Bestandsstelle wird beim Anfassen SOFORT nachgeruestet.

## Zwei Zuruf-Hebel
- "durchsuche das Log und fixe": Log am bekannten Pfad einlesen, Fehler nach Typ/Haeufigkeit gruppieren+priorisieren, je Fehler Root-Cause-Fix, bis 2 Durchlaeufe sauber sind.
- "auditiere die Sondenabdeckung": Logikpfade ohne Sonde + tote Sonden finden und melden.

## Sicherheit
Keine Secrets/PII roh ins Log (maskieren/redacten). Log-Pfad in `.gitignore`.

## Selbst-Check vor "fertig" (pro qualifiziertem Commit)
Logschicht existiert; ein absichtlich provozierter Fehler landet mit Kontext im Log; Live-Tail geht; neue Logik dieses Commits ist instrumentiert; betroffene Bestands-Sonden sind aktualisiert; keine toten Sonden uebrig.
