# Projekt — Denknotiz
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

## 1. Zweck in drei Sätzen

Denknotiz ist Franks privater, chatartig organisierter Denkraum für getippte und diktierte Notizen in thematischen Sitzungen. Die App sammelt neue Notizen bis zu einer sichtbaren Auswertungsgrenze und lässt Codex daraus auf eine feste Fokusfrage hin eine nachvollziehbare Auswertung erstellen, ohne alte Inhalte still erneut einzubeziehen oder wegen Modellgrenzen zu kürzen. Aufnahme, Transkription, KI-Auswertung, Vorlesen, Sicherung und der Wechsel zwischen Außen- und Innendisplay des Galaxy Z Fold 8 bilden einen durchgängigen, ausfallsicheren Arbeitsfluss.

## 2. Zielplattform(en)

| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht oder später |
|-----------|------------------------|-------------|----------------------|
| Android | Galaxy Z Fold 8 außen: 1248 × 1972 px, adaptiver kompakter Zielraum | Kotlin + Jetpack Compose | Pflicht |
| Android | Galaxy Z Fold 8 innen: 1848 × 2448 px, adaptiver erweiterter Zielraum | Kotlin + Jetpack Compose mit adaptivem Zwei-Spalten-Aufbau | Pflicht |

Paketname: `de.frank.denknotiz`. **Abgeleitete technische Entscheidung:** Mindestversion Android 8 (API 26) wie die Referenz-Apps, Zielversion Android 16 (API 36); Hochformat ist der primäre Zielzustand, Querformat bleibt vollständig bedienbar.

## 3. Rahmenbedingungen

| Punkt | Festlegung |
|-------|-----------|
| Sprache | Oberfläche und feste Texte ausschließlich Deutsch mit echten Umlauten. |
| Nutzer | Privat, genau ein lokaler Nutzer: Frank. Keine Mehrbenutzerfähigkeit und kein App-Konto. |
| Offline / Online | Teilweise offline. Sitzungen und Notizen anzeigen, suchen, anlegen, tippen, bearbeiten, kopieren, duplizieren, löschen/Undo sowie JSON-Export und -Import funktionieren offline. Groq-Transkription, Codex-Titel, Textverbesserung, KI-Auswertung und Chirp/Edge/Qwen-TTS brauchen Netz und zeigen klare Netzfehler. |
| Anmeldung | Nur Codex Device-Code Auth, exakt nach dem in PerfectMoment eingesetzten Ablauf. Sonstige Dienste erhalten Schlüssel in den Einstellungen. |
| Berechtigungen | `RECORD_AUDIO` bedarfsgerecht beim ersten Aufnahmestart; Foreground-Service für Aufnahme mit Mikrofontyp und dauerhafter Benachrichtigung; Benachrichtigungsrecht ab Android 13 bedarfsgerecht vor dem ersten Hintergrundvorgang; Medienwiedergabe für TTS; Dateiauswahl über Android Storage Access Framework ohne pauschale Speicherberechtigung. |
| Externe Dienste | Groq `whisper-large-v3-turbo`; Codex mit GPT-5.6 Sol/Terra/Luna; Chirp, Microsoft Edge und Qwen für TTS. |
| Datenhaltung | Strukturierte App-Daten lokal in Room. Temporäre Aufnahme- und TTS-Dateien im privaten App-Speicher/Cache. Secrets verschlüsselt über Android Keystore; keine Secrets in Room oder Sicherungen. |
| Sicherung | Manuelle, unverschlüsselte, versionierte JSON-Datei nach PerfectMoment-Muster. Geräteübergreifend importierbar; Import führt einen Merge aus, lokale Datensätze mit gleicher ID gewinnen. Keine Schlüssel, Tokens, Audioaufnahmen oder TTS-Caches. |
| Verteilung | Privat per direkter Installation auf Franks Galaxy Z Fold 8; keine Store-Veröffentlichung. Daher keine separaten Onboarding- oder Rechts-Specs. |
| Designstatus | **Werft-Designer auf aktuelle Nutzeranweisung übersprungen. v1 ist direkte Baugrundlage ohne Messpaket.** Alle als „abgeleitete technische Entscheidung“ markierten Kleindetails und alle exakten UI-Werte sind deshalb verbindliche Bauwerte, keine Werft-Messwerte. |

## 4. Ausdrücklich NICHT enthalten

1. Kein freier KI-Chat und keine frei formulierbare Fokusfrage; die lokale Fokusfrage lautet fest „Worauf soll ich mich konzentrieren?“.
2. Keine automatische Auswertung ohne Franks ausdrückliches Absenden im Fokusdialog.
3. Keine stille Kürzung, Zusammenfassung oder Auslassung von Notizen wegen Modelllimits.
4. Keine erneute Aufnahme alter, nach der Auswertungsgrenze liegender Notizen in spätere Auswertungen, auch wenn sie nachträglich bearbeitet wurden.
5. Keine Veränderung des Originaltexts einer gespeicherten KI-Antwort.
6. Keine dauerhafte Speicherung bestätigter Rohaufnahmen und keine Aufnahme-, Token-, Schlüssel- oder TTS-Cache-Daten im JSON-Backup.
7. Keine Cloud-Synchronisierung, keine automatische Sicherung und kein Ersetzen des lokalen Bestands beim Import.
8. Keine Bilder, Dateianhänge, Tags, Erinnerungen, Teamfunktionen oder Veröffentlichung.

## 5. Abnahme — wann ist es fertig

| Kennung | Prüfkriterium |
|---------|---------------|
| A-01 | Eine neue Sitzung wird sofort geöffnet, erhält nach der ersten Notiz automatisch einen editierbaren Titel und steht nach letzter Änderung sortiert in der Session-Seitenleiste. |
| A-02 | Sitzungen lassen sich suchen, anheften, umbenennen, archivieren und löschen; nach dem Löschen stellt Undo sie samt Inhalt wieder her. |
| A-03 | Auf dem Außendisplay ist die Seitenleiste eine Schublade; auf dem Innendisplay steht sie dauerhaft neben dem Verlauf. Sitzung, Entwurf und Scrollposition bleiben beim Fold-Wechsel erhalten. |
| A-04 | Ein getippter Entwurf lässt sich direkt speichern; mehrere aufeinanderfolgende Diktate werden an denselben Entwurf angehängt und erst nach Bestätigung gespeichert. |
| A-05 | Aufnahme startet und stoppt per Tipp, lässt sich abbrechen, endet spätestens nach 10:00 Minuten und läuft im Hintergrund mit sichtbarer Foreground-Service-Benachrichtigung weiter. |
| A-06 | Jede Transkription durchläuft RMS-Vorfilter, Confidence/Repetition/Mini-Noise-Filter, Segment-vs-Audiofenster-Prüfung und Floskelblocklist. |
| A-07 | Verwerfen alle Filter den Inhalt, kann Frank die temporäre Aufnahme anhören, erneut transkribieren oder verwerfen; nach bestätigter Transkription ist die Rohaufnahme gelöscht. |
| A-08 | Jede gespeicherte Notiz besitzt Zeitstempel und sofort einen lokalen Titel; online wird höchstens einmal ein Codex-Titel erzeugt, ein manueller Titel wird nie überschrieben. |
| A-09 | Notiztext und -titel lassen sich bearbeiten, kopieren, duplizieren und mit Undo löschen. KI-Verbesserung bewahrt das Original und kann es exakt wiederherstellen. |
| A-10 | Im Verlauf ist die persistente Trennlinie „Bis hier zuletzt ausgewertet“ sichtbar. Eine neue Auswertung enthält nur vollständig gespeicherte Notizen hinter dieser Grenze. |
| A-11 | Der Fokusdialog zeigt fest „Worauf soll ich mich konzentrieren?“ und erlaubt pro Auswertung die Wahl eines Profils sowie „Nur Notizen“ oder „Notizen + Web“. |
| A-12 | Beim Absenden wird ein unveränderlicher Notizsnapshot fixiert. Spätere Bearbeitungen, neue Notizen oder Löschungen verändern den laufenden Auftrag nicht. |
| A-13 | Nur eine vollständig erfolgreiche finale KI-Antwort verschiebt die Auswertungsgrenze; Fokusdialog, Fehler und Abbruch verschieben sie nicht. |
| A-14 | Retry verwendet exakt denselben Snapshot, dieselbe Profilfassung, dasselbe Modell, dieselbe Reasoning-Stufe und denselben Webmodus. |
| A-15 | Überschreitet der Kontext das Modelllimit, werden vollständige Notizen in Teilblöcken verarbeitet und danach vollständig zusammengeführt; keine Notiz wird still gekürzt. |
| A-16 | Alte nachträglich bearbeitete Notizen werden nicht erneut ausgewertet und markieren die zugehörige alte Auswertung sichtbar als „Grundlage nachträglich bearbeitet“. |
| A-17 | Wird eine alte Grundlage gelöscht, bleibt die KI-Antwort erhalten und wird sichtbar als „Grundlage teilweise gelöscht“ markiert. |
| A-18 | KI-Antworten sind im Original unveränderlich und lassen sich kopieren, vorlesen, als neue Notiz übernehmen und mit Undo löschen. |
| A-19 | Im Webmodus „Notizen + Web“ enthält die Antwort keine Inline-URLs; alle Quellen stehen ausschließlich als Linkliste am Ende. |
| A-20 | KI-Antworten bestehen grundsätzlich aus Absätzen mit 4–8 Sätzen; eine inhaltlich kurze Antwort darf weniger Sätze enthalten. |
| A-21 | Codex Device-Code Auth verhält sich exakt wie PerfectMoment; Modelle Sol, Terra und Luna sowie low, medium, high, xhigh und max sind wählbar. Standard ist Terra/medium. |
| A-22 | Die drei festen Profile Kurz, Normal und Ausführlich haben editierbare Prompts und Reset; drei freie Profile haben Name und Prompt; genau ein nichtleeres Profil ist aktiv. |
| A-23 | Eine laufende KI-Auswertung arbeitet im Hintergrund weiter und zeigt Fortschritt sowie Abschluss/Fehler in einer Benachrichtigung. |
| A-24 | Chirp, Edge und Qwen lassen sich verwenden. TTS bietet Start/Pause, Stopp, Absatz zurück/vor, Absatzhighlight und Absatzprefetch; im Hintergrund erscheint eine Medienbenachrichtigung. |
| A-25 | Ein Anbieterwechsel während laufender TTS erfolgt erst nach Bestätigung. Qwen-Stimmklone lassen sich vollständig anlegen, benennen, testen, auswählen und löschen. |
| A-26 | Ein JSON-Export ist versioniert, unverschlüsselt und enthält alle zulässigen lokalen Inhalte; Import merged, wobei bei gleicher ID immer der lokale Datensatz gewinnt. |
| A-27 | Im Flugmodus bleiben lokale Anzeige, Suche, Bearbeitung, Tippnotizen und Backup bedienbar; jede Cloudfunktion meldet verständlich, dass Netz fehlt, ohne lokale Daten zu verändern. |
| A-28 | Fold-Wechsel erhält offene Sitzung, Entwurf, Scrollposition, Aufnahme, Transkription, laufende Auswertung und TTS-Position ohne Neustart oder Doppelauftrag. |
| A-29 | Hell, Dunkel, Gold-Hell und Gold-Dunkel färben jeden Bildschirm vollständig; Gold-Dunkel ist beim Erststart aktiv. |
| A-30 | Bei reduzierter Bewegung gibt es keine Dauerbewegung, Parallaxe oder Rotation; notwendige Zustandswechsel verwenden ausschließlich Fades von 100–160 ms. |
| A-31 | Kein sichtbares Bedienelement ist ohne Funktion, jede destructive Aktion verlangt Bestätigung oder bietet Undo, und alle Fehlerzustände besitzen einen klaren nächsten Schritt. |

## 6. Offene Fragen

Keine. Noch nicht ausdrücklich vorgegebene Kleindetails sind in den Specs als **abgeleitete technische Entscheidung** festgelegt und für v1 verbindlich.
