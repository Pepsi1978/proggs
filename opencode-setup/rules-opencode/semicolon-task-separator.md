Semikolon-Trenner für mehrere Aufgaben in einem Prompt

Die exakte Zeichenfolge ` ; ` (Leerzeichen-Semikolon-Leerzeichen) im Prompt bedeutet: der Prompt enthaelt MEHRERE eigenstaendige Aufgaben, die NACHEINANDER und VOLLSTAENDIG abgearbeitet werden — keine darf vergessen werden. (Entsteht durch Franks Voice-Overlay, das nach jedem Einsprechen ` ; ` anhaengt.)

## Erkennen
Am ` ; ` splitten. Anzahl Aufgaben = Anzahl NICHT-leerer Teile. Ein ` ; ` nur am Ende (kein Text danach) zaehlt NICHT mit. Semikola in Code/SQL/URLs OHNE beidseitige Leerzeichen sind KEINE Trenner.

## Die 7-Schritte-Pipeline
1. **Erkennen:** splitten, leere Teile verwerfen, Pre-/Post-Prompt-Marker aussortieren (sind keine Aufgaben).
2. **Sortieren (Pre-Flight):** gruppieren (gleiche Datei/Feature zusammen), Abhaengigkeiten beachten (A vor B), optimale Reihenfolge (nicht stur Einsprech-Reihenfolge). Bei GEGENSAETZLICHEN Aufgaben (gleiche Stelle, widerspruechlich): STOP und nachfragen, nicht blind beides bauen.
3. **Anzeigen:** ab 2 Aufgaben kurze Uebersicht + sichtbare TaskCreate-Liste (Pflicht).
4. **Abarbeiten:** sichtbar im Hauptchat, eine nach der anderen, KEINE Subagents. Pro Aufgabe: Task auf in_progress -> umsetzen -> committen+pushen (nur eigene Dateien namentlich) -> sichtbaren Commit-Marker ausgeben -> Task abhaken (in Echtzeit, nicht erst am Ende).
5. **Bauen:** NUR EINMAL nach der letzten Aufgabe (nicht nach jeder).
6. **Installieren:** NUR EINMAL nach dem Build (aufs Handy + App starten).
7. **Verifizieren:** Original-Liste durchgehen — sind WIRKLICH alle erledigt (auch die in der Mitte)? Untergegangenes nachholen, dann erst Status-Meldung.

Schritt 5+6 nur bei baubarer/installierbarer App (Android usw.); bei reinen Regel-/Doku-/Config-/Web-Aufgaben uebersprungen. Schritte 1-4 und 7 laufen IMMER.

## Commit-Marker pro Aufgabe (direkt nach Commit+Push)
80 × `━` als Linie ueber und unter der Marker-Zeile:
`💾 Aufgabe N: [kurze Beschreibung in leichtem Deutsch] — committed und gepusht`
Nur bei Aufgaben mit Commit; eine reine Frage/Erklaerung hat keinen Marker.

## Pre-Prompt / Post-Prompt (Marker, keine Aufgaben)
`Pre-Prompt: "..."` = Kontext/Setup VOR den Aufgaben. `Post-Prompt: "..."` = Constraint WAEHREND/NACH jeder Aufgabe. Tolerant erkennen (PrePrompt / Pre Prompt / pre-prompt ... egal). Enthaelt der Prompt mindestens einen Marker: ZUERST eine Tabelle (Typ | Inhalt) ausgeben, Inhalt WORTWOERTLICH 1:1 (nicht zusammenfassen), Reihenfolge: erst alle Pre-Prompts, dann Aufgaben, dann Post-Prompts.

## NIEMALS
- Eine Aufgabe (besonders in der Mitte einer langen Liste) vergessen oder nur halb erledigen.
- Bei 2+ Aufgaben keine TaskCreate-Liste anlegen.
- Subagents fuer die Abarbeitung nutzen (Frank will alles sichtbar im Hauptchat).
- Nach JEDER Aufgabe bauen/installieren — Build+Install nur EINMAL am Ende.
- `git add -A`/`.` statt nur eigene Dateien namentlich.
- Den Commit-Marker weglassen oder gesammelt am Ende ausgeben.
- Semikola in Code faelschlich als Trenner werten.
- Pre-/Post-Prompt-Inhalt in der Tabelle zusammenfassen statt 1:1.
