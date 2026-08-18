# Funktions-Spec — Denknotiz
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

## 1. Überblick der Funktionen

| Kennung | Funktion | Bildschirm(e) | Stufe |
|---------|----------|---------------|-------|
| F-01 | Sitzungen anlegen und automatisch betiteln | B-01, B-02 | Kern |
| F-02 | Sitzungen organisieren und wiederherstellen | B-02, B-13 | Kern |
| F-03 | Sitzung wechseln und durchsuchen | B-01, B-02, B-12 | Kern |
| F-04 | Entwurf tippen, diktieren und bestätigen | B-01, B-03 | Kern |
| F-05 | Aufnahme im Vorder- und Hintergrund | B-01, B-03 | Kern |
| F-06 | Groq-Transkription mit vier Cortex-Filtern | B-03, B-04 | Kern |
| F-07 | Notiz speichern und betiteln | B-01 | Kern |
| F-08 | Notizen bearbeiten, kopieren, duplizieren und löschen | B-01, B-05 | Kern |
| F-09 | Notiztext per KI verbessern und Original wiederherstellen | B-01, B-05 | Kern |
| F-10 | Auswertungsgrenze und Snapshot bilden | B-01, B-06 | Kern |
| F-11 | Fokus, Profil und Webmodus wählen | B-06 | Kern |
| F-12 | KI-Auswertung vollständig und wiederholbar ausführen | B-01, B-06 | Kern |
| F-13 | Alte Auswertungsgrundlagen markieren | B-01 | Kern |
| F-14 | KI-Antwort verwenden und löschen | B-01 | Kern |
| F-15 | Auswertungsprofile verwalten | B-07, B-09 | Kern |
| F-16 | Codex verbinden, Modell und Reasoning wählen | B-07, B-08 | Kern |
| F-17 | TTS wiedergeben und Anbieter wechseln | B-01, B-10 | Kern |
| F-18 | Qwen-Stimmklone verwalten | B-10, B-11 | Kern |
| F-19 | JSON-Sicherung exportieren und mergen | B-07, B-14 | Kern |
| F-20 | Themes und Einstellungen verwalten | B-07 | Kern |
| F-21 | Offline-, Fehler- und Geheimnisbehandlung | B-01, B-07 | Kern |
| F-22 | Fold-, Prozess- und Hintergrundzustand erhalten | alle | Kern |

## 2. Funktionen im Einzelnen

### F-01 — Sitzungen anlegen und automatisch betiteln

- **Auslöser** — Frank tippt in B-01 oder B-02 auf „Neue Sitzung“.
- **Ablauf** — 1. Room erzeugt sofort eine Sitzung mit UUID, Titel „Neue Sitzung“ und aktuellem Änderungszeitpunkt. 2. Die Sitzung wird geöffnet und der Eingabeentwurf fokussiert. 3. Nach der ersten gespeicherten Notiz erhält die Sitzung sofort lokal einen Titel aus den ersten höchstens sechs sinntragenden Wörtern. 4. Ist Codex online und der Titel nicht manuell geändert, wird einmalig ein kurzer KI-Sitzungstitel angefordert und bei Erfolg ersetzt.
- **Daten** — Schreibt `Sitzung`; liest für den Titel die erste `Notiz`.
- **Ergebnis** — Eine sofort nutzbare, dauerhaft gespeicherte und editierbar betitelte Sitzung.
- **Fehlerfall** — Schlägt der KI-Titel fehl, bleibt der lokale Titel bestehen; es gibt keinen automatischen zweiten KI-Versuch.
- **Regeln/Grenzen** — Sitzungstitel höchstens 60 Zeichen. Manuell geänderte Titel werden weder lokal noch durch KI überschrieben. **Abgeleitete technische Entscheidung:** UUID v4 dient als geräteübergreifend stabile ID.

### F-02 — Sitzungen organisieren und wiederherstellen

- **Auslöser** — Frank öffnet das Kontextmenü einer Sitzung oder zieht eine Sitzung nach links.
- **Ablauf** — „Anheften“ fixiert die Sitzung im angehefteten Abschnitt; erneutes Antippen löst sie. „Umbenennen“ speichert einen manuellen Titel. „Archivieren“ verschiebt sie aus der aktiven in die archivierte Liste, ohne Inhalte zu verändern. „Löschen“ verlangt Bestätigung, markiert Sitzung samt Notizen und Antworten als `pendingDelete` und zeigt 8 Sekunden Undo; danach werden sie endgültig entfernt. Undo entfernt die Markierung vollständig.
- **Daten** — Ändert `Sitzung.istAngeheftet`, `.titel`, `.titelManuell`, `.archiviertAm`, `.pendingDeleteBis` sowie abhängige Löschmarken.
- **Ergebnis** — Sitzungen sind gezielt priorisiert, benannt, archiviert oder sicher gelöscht.
- **Fehlerfall** — Prozessende während der Undo-Frist: Beim nächsten Start wird anhand `pendingDeleteBis` entweder wiederhergestellt oder endgültig gelöscht. Scheitert eine Room-Transaktion, bleibt der vorherige Zustand bestehen.
- **Regeln/Grenzen** — Aktive und archivierte Sitzungen sind je Abschnitt sortiert: angeheftet zuerst, innerhalb der Gruppe `geaendertAm` absteigend. Löschen einer geöffneten Sitzung öffnet die nächste aktive Sitzung; fehlt eine, wird F-01 ausgeführt.

### F-03 — Sitzung wechseln und durchsuchen

- **Auslöser** — Tipp auf eine Sitzung in B-02, Sucheingabe in B-02/B-12 oder Tipp auf einen Treffer.
- **Ablauf** — Die lokale Suche startet ab zwei Zeichen und durchsucht Sitzungstitel, Notiztitel, Notiztexte und KI-Antworten per Room FTS. Ein Sitzungstreffer öffnet die Sitzung an der letzten Scrollposition; ein Inhaltstreffer öffnet sie und scrollt zum Treffer. Der zuletzt geöffnete Zustand wird je Sitzung gesichert.
- **Daten** — Liest Room-FTS; schreibt `Sitzung.zuletztGeoeffnetAm`, `SitzungsUiZustand`.
- **Ergebnis** — Gesuchte Inhalte und Sitzungen sind offline erreichbar.
- **Fehlerfall** — Kein Treffer zeigt einen Leerzustand; ein inzwischen gelöschter Treffer wird aus der Ergebnisliste entfernt.
- **Regeln/Grenzen** — Groß-/Kleinschreibung und deutsche Diakritika werden tolerant gesucht. Ein Wechsel stoppt keine Aufnahme, Transkription, Auswertung oder TTS; die jeweilige Aufgabe bleibt ihrer Ursprungssitzung zugeordnet.

### F-04 — Entwurf tippen, diktieren und bestätigen

- **Auslöser** — Frank tippt in B-01 oder startet dort ein Diktat.
- **Ablauf** — 1. Jeder Entwurf wird sitzungsbezogen lokal persistiert. 2. Getippter Text und bestätigte Transkriptteile werden in zeitlicher Reihenfolge an denselben Entwurf angehängt; zwischen nichtleerem Bestand und neuem Teil stehen zwei Zeilenumbrüche. 3. Beliebig viele Diktate können vor dem Speichern folgen. 4. Nach jeder Transkription öffnet B-03 mit Textvorschau und „Übernehmen“, „Erneut transkribieren“ und „Verwerfen“. 5. „Übernehmen“ hängt den Text an, „Notiz speichern“ erzeugt erst dann die Notiz über F-07.
- **Daten** — Schreibt `Entwurf.text`, `Entwurf.geaendertAm`, temporäre `Diktat`-Datensätze.
- **Ergebnis** — Ein bestätigter, editierbarer Entwurf ohne unbeabsichtigt gespeicherte Transkription.
- **Fehlerfall** — Leerer Entwurf kann nicht gespeichert werden. App-Abbruch lässt den Entwurf erhalten. Ein verworfenes Diktat verändert den Entwurf nicht.
- **Regeln/Grenzen** — Keine Zeichenbegrenzung. Vor dem Speichern ist immer eine sichtbare Bestätigung erforderlich, sobald der Entwurf mindestens einen Diktatteil enthält.

### F-05 — Aufnahme im Vorder- und Hintergrund

- **Auslöser** — Erster Tipp auf die runde Aufnahmeaktion startet, zweiter Tipp stoppt; „Abbrechen“ verwirft.
- **Ablauf** — 1. Bei Bedarf werden `RECORD_AUDIO` und ab Android 13 Benachrichtigungen erklärt und angefordert. 2. Ein Foreground-Service vom Typ Mikrofon startet und zeigt Dauer, Stoppen und Abbrechen in der Benachrichtigung. 3. Aufnahme erfolgt als 16-kHz-Mono-WAV im privaten temporären Speicher. 4. Bei 09:30 erscheint ein Hinweis; bei exakt 10:00 stoppt die Aufnahme automatisch. 5. Stoppen übergibt die vollständige Datei an F-06; Abbrechen löscht sie sofort.
- **Daten** — Temporäre WAV-Datei, `Diktat.aufnahmeBegonnenAm`, Dauer und Zustand.
- **Ergebnis** — Eine vollständige Aufnahme zur Transkription, auch bei gesperrtem Bildschirm oder App im Hintergrund.
- **Fehlerfall** — Fehlendes Mikrofonrecht zeigt einen Weg zu den Systemeinstellungen. Audiofokusverlust durch Anruf pausiert nicht: die Aufnahme stoppt und der bisherige Inhalt geht zur Bestätigung. Zu wenig Speicher beendet die Aufnahme mit verständlichem Fehler und löscht unvollständige Daten.
- **Regeln/Grenzen** — Genau eine Aufnahme gleichzeitig; Mindestdauer 0,4 Sekunden; Start einer Aufnahme pausiert TTS. Die Benachrichtigung bleibt, solange Aufnahme oder deren Übergabe aktiv ist.

### F-06 — Groq-Transkription mit vier Cortex-Filtern

- **Auslöser** — Eine gestoppte Aufnahme aus F-05 oder „Erneut transkribieren“ in B-03/B-04.
- **Ablauf** — 1. **RMS vor API:** Die lokale RMS-/Sprachaktivitätsprüfung verwirft reine Stille und sendet sie nicht. 2. Groq verarbeitet die Datei mit `whisper-large-v3-turbo`, Deutsch, Segmentzeitstempeln und ausführlichen Segmentwerten. 3. **Confidence/Repetition/Mini-Noise:** Segmente mit unzureichender Log-Wahrscheinlichkeit, hoher `no_speech`-Wahrscheinlichkeit, auffälliger Kompression/Wiederholung oder nur minimalem Geräuschfenster werden entfernt. 4. **Segment-vs-Audiofenster:** Jedes Segment bleibt nur, wenn sein Zeitfenster mit lokal erkannter Sprachaktivität überlappt. 5. **Floskelblocklist:** bekannte Whisper-Halluzinationsfloskeln werden blockiert. 6. Bleibt Text, zeigt B-03 die vollständige Transkription zur Bearbeitung und Bestätigung. 7. Verwerfen alle Filter den Text, zeigt B-04 die Vollfilter-Werkzeuge „Audio anhören“, „Erneut transkribieren“ und „Verwerfen“.
- **Daten** — Liest temporäres Audio und verschlüsselten Groq-Schlüssel; schreibt Transkript, Filterentscheidungen und Status in `Diktat`.
- **Ergebnis** — Ein überprüfbares Transkript oder ein ausdrücklich leerer Vollfilter-Zustand.
- **Fehlerfall** — Kein Netz/Schlüssel: Audio bleibt temporär erhalten und B-03 zeigt Wiederholen sowie Verwerfen. HTTP-, Rate-Limit- und Timeoutfehler zeigen ihren Typ ohne Text zu erfinden. Nach drei automatischen Versuchen erfolgen weitere nur manuell.
- **Regeln/Grenzen** — Alle vier Filter sind verpflichtend und nicht abschaltbar. „Erneut transkribieren“ verwendet dieselbe Audiodatei. Nach „Übernehmen“ oder bestätigtem „Verwerfen“ wird die Rohaufnahme sofort sicher gelöscht; unbestätigtes Audio wird spätestens nach sieben Tagen beim App-Start bereinigt. **Abgeleitete technische Entscheidung:** maximal zwei parallele Transkriptionen, FIFO je Sitzung.

### F-07 — Notiz speichern und betiteln

- **Auslöser** — „Notiz speichern“ bei nichtleerem Entwurf.
- **Ablauf** — Room speichert Notiz, Zeitstempel, Quelle und sofort einen lokalen Titel aus dem ersten sinntragenden Satz. Der Entwurf wird erst nach erfolgreicher Transaktion geleert. Ist Codex verfügbar, wird genau einmal ein KI-Titel angefordert; er ersetzt den lokalen Titel nur, wenn `titelManuell=false` und der Titel seit Auftragserstellung unverändert ist.
- **Daten** — Schreibt `Notiz`, leert `Entwurf`; optional schreibt KI-Titelstatus.
- **Ergebnis** — Eine fertige Notizkarte mit Titel und Zeitstempel.
- **Fehlerfall** — Room-Fehler lässt den Entwurf unverändert. KI-Fehler belässt den lokalen Titel und markiert den einmaligen Versuch als beendet.
- **Regeln/Grenzen** — Lokaler/KI-Titel maximal 80 Zeichen. Ein manueller Titel wird nie überschrieben. Der KI-Titelversuch ist pro Notiz exakt einmalig, auch nach Neustart.

### F-08 — Notizen bearbeiten, kopieren, duplizieren und löschen

- **Auslöser** — Kontextmenü einer Notiz oder B-05.
- **Ablauf** — Bearbeiten ändert Titel und/oder Text in einer Transaktion und setzt beim Titel `titelManuell=true`. Kopieren legt Titel plus Text in die Zwischenablage. Duplizieren erzeugt direkt unter dem Original eine neue Notiz mit neuer UUID, aktuellem Zeitstempel, gleichem Titel/Text und `duplikatVonId`. Löschen markiert die Notiz 8 Sekunden für Undo und entfernt sie sichtbar; Undo stellt sie an ihrer ursprünglichen Position wieder her.
- **Daten** — Ändert oder erzeugt `Notiz`; schreibt Löschmarke und Änderungsereignis.
- **Ergebnis** — Notizen lassen sich sicher pflegen, wiederverwenden und entfernen.
- **Fehlerfall** — Zwischenablagefehler zeigt „Kopieren fehlgeschlagen“. Leerer Text kann nicht gespeichert werden. Prozessende während Undo folgt derselben Regel wie F-02.
- **Regeln/Grenzen** — Zeitstempel des Originals bleibt beim Bearbeiten unverändert, `geaendertAm` wird aktualisiert. Auswirkungen auf alte Auswertungen behandelt F-13.

### F-09 — Notiztext per KI verbessern und Original wiederherstellen

- **Auslöser** — „Mit KI verbessern“ im Notizmenü.
- **Ablauf** — Codex erhält nur den festen Notizsnapshot und den Auftrag, Rechtschreibung, Zeichensetzung, Satzbau und Absätze zu verbessern, ohne Inhalt hinzuzufügen oder zu entfernen. Vor dem Ersetzen wird der aktuelle Text als unveränderliche Revision gespeichert. „Original wiederherstellen“ setzt exakt diese Revision zurück; erneutes Verbessern erzeugt eine weitere Revision.
- **Daten** — Liest `Notiz.text`; schreibt `NotizRevision` und neuen Text.
- **Ergebnis** — Verbesserter Text mit sicherer Wiederherstellung des vorherigen Originals.
- **Fehlerfall** — Kein Netz/Codex, Abbruch oder unvollständige Antwort verändert die Notiz nicht. Wurde die Notiz parallel bearbeitet, wird das Ergebnis nicht automatisch übernommen, sondern als Vorschau mit „Übernehmen“ angeboten.
- **Regeln/Grenzen** — Nur vollständige finale Antwort kann übernommen werden. Manuelle Titel bleiben unberührt. Alte Grundlage wird nach Übernahme durch F-13 markiert.

### F-10 — Auswertungsgrenze und Snapshot bilden

- **Auslöser** — Öffnen von B-06 und abschließend „Auswertung starten“.
- **Ablauf** — Die persistente Grenze einer Sitzung liegt bei der höchsten Ordnungsposition der letzten vollständig erfolgreichen Auswertung. B-01 zeigt dort die Trennlinie „Bis hier zuletzt ausgewertet“. Für eine neue Auswertung werden ausschließlich fertige, nicht gelöschte Notizen hinter der Grenze nach stabiler Sitzungsreihenfolge erfasst. Beim Absenden werden IDs, Titel, vollständige Texte, Zeitstempel, Reihenfolge und Inhalts-Hashes in einen unveränderlichen `AuswertungsSnapshot` kopiert.
- **Daten** — Liest Notizen und `Sitzung.auswertungsGrenze`; schreibt Snapshot samt Snapshot-Elementen.
- **Ergebnis** — Ein fester, später reproduzierbarer Kontext.
- **Fehlerfall** — Keine neuen Notizen: Start bleibt deaktiviert und erklärt „Keine neuen Notizen hinter der Grenze“. Unfertige Transkriptionen werden genannt, aber nicht aufgenommen; Frank kann warten oder ohne sie absenden.
- **Regeln/Grenzen** — Fokusfrage allein verschiebt keine Grenze. Nachträgliche neue, bearbeitete oder gelöschte Notizen ändern den Snapshot nie. Alte Notizen vor der Grenze werden nie erneut aufgenommen.

### F-11 — Fokus, Profil und Webmodus wählen

- **Auslöser** — KI-Aktion in B-01 öffnet B-06.
- **Ablauf** — B-06 zeigt unveränderlich die lokale Frage „Worauf soll ich mich konzentrieren?“. Frank gibt eine Antwort ein, wählt eines der sechs Profile und je Auftrag „Nur Notizen“ oder „Notizen + Web“. Modell und Reasoning werden aus den Einstellungen angezeigt und können für diesen Auftrag überschrieben werden. „Auswertung starten“ validiert alles und ruft F-10/F-12 auf.
- **Daten** — Liest Profile und Codex-Standard; schreibt die gewählten Werte in den Snapshot.
- **Ergebnis** — Eindeutiger, vollständig parametrisierter Auftrag.
- **Fehlerfall** — Leere Fokusantwort oder leeres Profil deaktiviert den Start mit Erklärung. Offline kann der Dialog ausgefüllt, aber nicht abgesendet werden.
- **Regeln/Grenzen** — Die Fokusfrage selbst wird nie generiert, geändert oder gespeichert; nur Franks Antwort wird im Snapshot gespeichert. Webmodus gilt ausschließlich für diesen Auftrag.

### F-12 — KI-Auswertung vollständig und wiederholbar ausführen

- **Auslöser** — Bestätigtes Absenden aus B-06 oder „Exakt wiederholen“ an einem Fehler.
- **Ablauf** — 1. Ein foreground-fähiger Hintergrundauftrag startet mit Notification. 2. Passt der vollständige Snapshot ins Modelllimit, wird er als Ganzes gesendet. 3. Passt er nicht, teilt die App ausschließlich an Notizgrenzen in Blöcke; jede Notiz bleibt vollständig. 4. Jeder Block wird mit derselben Fokusantwort und Profilfassung ausgewertet und als Zwischenresultat gespeichert. 5. Danach erzeugt Codex aus allen Zwischenresultaten und einer vollständigen Notiz-Inventarliste eine Gesamtauswertung. 6. Die finale Antwort verlangt Absätze mit 4–8 Sätzen; inhaltlich kurze Antworten dürfen weniger haben. 7. Bei „Notizen + Web“ werden Quellen ausschließlich als Linkliste am Ende ausgegeben. 8. Erst nach vollständiger, validierter Finalantwort werden `KiAntwort` gespeichert und die Grenze atomar auf die letzte Snapshot-Notiz verschoben.
- **Daten** — Liest Snapshot, Profilfassung, Modell/Reasoning/Auth; schreibt Auftrag, Blockstatus, Finalantwort und neue Grenze.
- **Ergebnis** — Eine vollständige, nachvollziehbare Auswertung ohne stille Kürzung.
- **Fehlerfall** — Netz, Auth, Rate-Limit, Kontextlimit, Prozess- oder Validierungsfehler lassen die Grenze unverändert und zeigen „Exakt wiederholen“. Unvollständiges Streaming wird verworfen, nicht als Antwort gespeichert. Ist selbst eine einzelne vollständige Notiz zu groß, meldet die App das offen und startet nicht mit gekürztem Text.
- **Regeln/Grenzen** — Genau eine Auswertung gleichzeitig je Sitzung; mehrere Sitzungen dürfen in der globalen FIFO-Schlange warten. Retry nutzt exakt den gespeicherten Snapshot und alle Parameter, niemals den aktuellen Datenbestand. **Abgeleitete technische Entscheidung:** Ein Auftrag gilt nur bei terminalem Status `erfolgreich` als vollständig.

### F-13 — Alte Auswertungsgrundlagen markieren

- **Auslöser** — Bearbeiten, Verbessern, Wiederherstellen oder Löschen einer Notiz, deren ID in einem erfolgreichen Snapshot enthalten ist.
- **Ablauf** — Inhaltsänderung vergleicht den aktuellen Hash mit dem Snapshot-Hash und markiert jede betroffene Antwort „Grundlage nachträglich bearbeitet“. Löschung markiert „Grundlage teilweise gelöscht“. Wiederherstellung exakt zum Snapshot-Hash entfernt die Bearbeitungsmarke; Wiederherstellung einer gelöschten Notiz per Undo entfernt die Löschmarke.
- **Daten** — Liest Snapshot-Elemente; schreibt abgeleitete `KiAntwortGrundlageStatus`-Einträge.
- **Ergebnis** — Alte Antworten bleiben unverändert, ihre historische Datenlage ist sichtbar korrekt gekennzeichnet.
- **Fehlerfall** — Kann der Status nicht neu berechnet werden, bleibt die bestehende Warnung sichtbar und wird beim nächsten Start repariert.
- **Regeln/Grenzen** — Markierungen verändern weder Antworttext noch Auswertungsgrenze und führen nie zur erneuten Aufnahme alter Notizen.

### F-14 — KI-Antwort verwenden und löschen

- **Auslöser** — Kontextmenü einer KI-Antwortkarte.
- **Ablauf** — „Kopieren“ kopiert den unveränderten Originaltext. „Vorlesen“ startet F-17. „Als neue Notiz“ erzeugt hinter der aktuellen Grenze eine neue Notiz mit lokalem Titel „Auswertung: {Datum}“ und identischem Antworttext. „Löschen“ markiert Antwort und zugehörige technische Zwischenresultate 8 Sekunden für Undo.
- **Daten** — Liest `KiAntwort.text`; erzeugt `Notiz` oder Löschmarke.
- **Ergebnis** — Antwort ist nutzbar, aber niemals direkt editierbar.
- **Fehlerfall** — Schlägt „Als neue Notiz“ fehl, bleibt die Antwort unverändert. Undo stellt die Antwort wieder her.
- **Regeln/Grenzen** — Löschen einer Antwort setzt die bereits verschobene Grenze nicht zurück. Der Originaltext wird in keinem Ablauf verändert.

### F-15 — Auswertungsprofile verwalten

- **Auslöser** — B-07 oder Profilauswahl in B-06 öffnet B-09.
- **Ablauf** — Es existieren genau sechs Profile. Kurz, Normal und Ausführlich besitzen feste Namen, editierbare Prompts und „Auf Standard zurücksetzen“. Drei freie Profile besitzen editierbaren Namen und Prompt; Reset leert beides auf „Freies Profil 1–3“ plus leeren Prompt. Ein Profil mit nichtleerem Prompt kann aktiviert werden; die Aktivierung deaktiviert das bisherige in derselben Transaktion.
- **Daten** — Schreibt `Auswertungsprofil`.
- **Ergebnis** — Genau ein wirksames Profil ist aktiv und kann im Fokusdialog auftragsbezogen gewählt werden.
- **Fehlerfall** — Leerer Prompt kann nicht aktiviert oder für eine Auswertung verwendet werden. Reset verlangt Bestätigung, wenn Änderungen verloren gehen.
- **Regeln/Grenzen** — Genau eines ist aktiv; Standard Normal. Standardprompts: Kurz „Antworte knapp, priorisiere das Wesentliche und nenne konkrete nächste Schritte.“; Normal „Ordne die Notizen, erkenne Zusammenhänge, beantworte den Fokus ausgewogen und konkret.“; Ausführlich „Analysiere gründlich, benenne Muster, Spannungen, Alternativen, Risiken und konkrete nächste Schritte.“ Snapshot speichert Name und vollständigen Prompt, damit spätere Änderungen alte Retries nicht verändern.

### F-16 — Codex verbinden, Modell und Reasoning wählen

- **Auslöser** — „Codex verbinden“ in B-07/B-08 oder Cloudfunktion ohne Anmeldung.
- **Ablauf** — Der Device-Code-Ablauf, Pollingrhythmus, Tokenrefresh, Fehlerbehandlung und sichere Speicherung werden exakt aus PerfectMoment übernommen. B-08 zeigt Verifizierungsadresse, Gerätecode, Kopieren, Browser öffnen, Ablaufzeit und Verbindungsstatus. In B-07 sind GPT-5.6 Sol, Terra und Luna sowie Reasoning `low`, `medium`, `high`, `xhigh`, `max` wählbar.
- **Daten** — Tokens verschlüsselt per Android Keystore; Modell und Reasoning in Room/DataStore.
- **Ergebnis** — Codex-Funktionen sind authentifiziert und parametrisiert.
- **Fehlerfall** — Abgelaufener Code kann erneuert werden; Ablehnung, Netz- oder Pollingfehler speichern keine unvollständige Anmeldung. Trennen löscht alle Codex-Tokens.
- **Regeln/Grenzen** — Standard GPT-5.6 Terra / `medium`. Authentifizierungsdetails werden nicht neu erfunden, sondern exakt aus PerfectMoment übernommen.

### F-17 — TTS wiedergeben und Anbieter wechseln

- **Auslöser** — TTS-Aktion an Notiz/KI-Antwort oder B-10.
- **Ablauf** — Text wird an Absatzgrenzen, bei überlangen Absätzen zusätzlich an Satzgrenzen geteilt. Wiedergabe bietet Start/Pause, Stopp, Absatz zurück und Absatz vor. Der aktive Absatz wird markiert; während er läuft, werden gemäß Cortex/Experimente die nächsten zwei Absätze vorab synthetisiert. Im Hintergrund steuert eine MediaSession-Benachrichtigung dieselben Aktionen. Chirp, Edge und Qwen stehen als Anbieter bereit. Anbieterwechsel während einer laufenden Wiedergabe zeigt eine Bestätigung; bei Zustimmung stoppt der alte Anbieter, leert seinen Cache und startet den aktuellen Absatz mit dem neuen.
- **Daten** — Liest Text und TTS-Einstellungen; temporäre Audiodateien nur im Cache, Position in `TtsZustand`.
- **Ergebnis** — Lückenarmes absatzweises Vorlesen im Vorder- und Hintergrund.
- **Fehlerfall** — Netz-/Anbieter-/Schlüsselfehler pausiert an der aktuellen Absatznummer und bietet Wiederholen oder Anbieterwechsel. Ein fehlgeschlagener Prefetch verwirft nicht die laufende Wiedergabe.
- **Regeln/Grenzen** — Nur eine globale TTS-Wiedergabe. Stopp löscht Position und Cache; Pause erhält sie. Start einer Aufnahme pausiert. Anbieterwechsel niemals ohne Bestätigung.

### F-18 — Qwen-Stimmklone verwalten

- **Auslöser** — B-10 „Qwen-Stimmen verwalten“ öffnet B-11.
- **Ablauf** — Frank kann Sprachproben aufnehmen oder über SAF auswählen, anhören, benennen und zur Qwen-Klonerstellung hochladen. Nach Erfolg wird die externe Voice-ID verschlüsselt gespeichert; eine Probe kann synthetisiert werden. Klone lassen sich umbenennen, auswählen und nach Bestätigung lokal und beim Anbieter löschen. Der gewählte Klon ist sofort in B-10 verfügbar.
- **Daten** — Temporäre Sprachprobe, verschlüsselter Qwen-Schlüssel und Voice-ID; sichtbarer Name lokal.
- **Ergebnis** — Vollständig verwaltbare eigene Qwen-Stimmen.
- **Fehlerfall** — Zu kurze/ungültige Probe, Upload-, Netz- oder Anbieterfehler lassen keinen halben Klon zurück. Scheitert Remote-Löschen, bleibt der Eintrag mit „Löschen ausstehend“ und Wiederholen erhalten.
- **Regeln/Grenzen** — Aufnahme nutzt F-05 ohne 10-Minuten-Ausreizung; **abgeleitete technische Entscheidung:** zulässige Klonprobe 10–120 Sekunden. Rohprobe wird nach erfolgreichem Klonen oder Verwerfen gelöscht und nie gesichert.

### F-19 — JSON-Sicherung exportieren und mergen

- **Auslöser** — B-14 „Sicherung exportieren“ oder „Sicherung importieren“.
- **Ablauf** — Export erzeugt über SAF eine UTF-8-JSON-Datei mit `format`, `schemaVersion`, `exportedAt` und allen zulässigen Room-Datensätzen einschließlich IDs, Snapshots, Profile und Einstellungen. Import validiert Format/Version, zeigt eine Vorschau der Mengen und merged nach Bestätigung transaktional. Bei gleicher Entitätsart und ID gewinnt immer lokal; nur unbekannte IDs werden eingefügt. Referenzen auf lokal gewinnende Eltern werden korrekt verbunden.
- **Daten** — Liest/schreibt Room; JSON enthält keine Secrets, Tokens, Audio, Sprachproben, Voice-IDs oder TTS-Caches.
- **Ergebnis** — Manuelle, unverschlüsselte, versionierte und geräteübergreifende Sicherung ohne Bestandsersetzung.
- **Fehlerfall** — Ungültiges JSON, unbekannte neuere Version, fehlende Referenz oder Schreibfehler bricht vor Commit ab und verändert nichts. Ältere unterstützte Versionen werden beim Einlesen migriert.
- **Regeln/Grenzen** — Dateiname `Denknotiz-Backup-YYYYMMDD-HHmm-v{schemaVersion}.json`. **Abgeleitete technische Entscheidung:** `schemaVersion=1` für diese v1; der Export weist sichtbar auf unverschlüsselten Inhalt hin.

### F-20 — Themes und Einstellungen verwalten

- **Auslöser** — Änderungen in B-07.
- **Ablauf** — Theme, Codex-Modell/Reasoning, Schlüssel, TTS-Anbieter/Stimme, Profile, Backup und Berechtigungsstatus sind gruppiert. Theme-Wechsel gilt sofort vollständig. Änderungen an Cloudschlüsseln werden verschlüsselt gespeichert; „Testen“ prüft den jeweiligen Dienst ohne Nutzdaten.
- **Daten** — Nichtgeheime Einstellungen lokal; Geheimnisse im Keystore-gestützten verschlüsselten Speicher.
- **Ergebnis** — Persistente, nachvollziehbare Konfiguration.
- **Fehlerfall** — Ungültiger Schlüssel wird nicht als „getestet“ markiert; bestehender Schlüssel bleibt bis zum bestätigten Speichern erhalten.
- **Regeln/Grenzen** — Themes: Hell, Dunkel, Gold-Hell, Gold-Dunkel; Standard Gold-Dunkel. Kein automatisches Systemtheme. Versionsnummer und Bump-Zeitpunkt stehen im Bereich „Über“.

### F-21 — Offline-, Fehler- und Geheimnisbehandlung

- **Auslöser** — Netzstatuswechsel, Cloudaufruf, App-Start oder Schlüsseländerung.
- **Ablauf** — Lokale Funktionen bleiben unabhängig vom Netz aktiv. Cloudaktionen prüfen Erreichbarkeit erst beim Start und zeigen bei Fehlen „Kein Netz“ plus „Erneut versuchen“, ohne lokale Eingaben zu verwerfen. Geheimnisse werden ausschließlich verschlüsselt gelesen/geschrieben und in Logs redigiert. Temporäre Cloudaufträge speichern nur fachliche IDs und Status, keine Tokens.
- **Daten** — Netzstatus, verschlüsselte Secrets, Auftragsstatus.
- **Ergebnis** — Vorhersehbares Offlineverhalten und keine Klartextgeheimnisse.
- **Fehlerfall** — Keystore-Verlust meldet, welcher Dienst neu verbunden werden muss; lokale Notizen bleiben zugänglich. HTTP-Fehler werden in Auth, Rate-Limit, Server, Timeout und Netz unterschieden.
- **Regeln/Grenzen** — Keine automatische Übertragung lokaler Inhalte außer durch eine ausdrücklich gestartete Cloudfunktion. Keine Secrets in Backup, Room-Dumps, Notifications oder Diagnosetexten.

### F-22 — Fold-, Prozess- und Hintergrundzustand erhalten

- **Auslöser** — Auf-/Zuklappen, Größenklasse, Rotation, Hintergrund, Prozessneustart oder Rückkehr.
- **Ablauf** — Sitzung, sitzungsbezogener Entwurf, Scrollanker/Offset, Aufnahme-ID/Dauer, Transkriptionsauftrag, Auswertungsauftrag, Fokusdialogdaten und TTS-Absatz/Position liegen außerhalb kurzlebiger Composables. Adaptive UI wechselt ohne Navigation zwischen Außen-Schublade und innerer persistenter Seitenleiste. Foreground-Service/WorkManager/MediaSession tragen Aufnahme, Auswertung und TTS im Hintergrund; idempotente Auftrags-IDs verhindern Doppelstarts.
- **Daten** — `SitzungsUiZustand`, `Entwurf`, Auftrags- und Wiedergabestatus in Room/SavedState/Services.
- **Ergebnis** — Fold-Wechsel und Lebenszyklusunterbrechungen verlieren keinen Arbeitsstand.
- **Fehlerfall** — Nach Prozessabbruch rekonstruiert die App laufende persistente Jobs oder markiert sie mit Wiederholen. Eine nicht mehr aktive Aufnahme wird als gestoppt zur Transkriptionsbestätigung angeboten, nicht fortgesetzt vorgespiegelt.
- **Regeln/Grenzen** — Kein Fold-Wechsel startet Netzwerk-, Aufnahme- oder TTS-Aufträge neu. Scrollposition wird als stabile Element-ID plus Offset, nicht als bloßer Listenindex gespeichert.

## 3. Datenmodell

Alle IDs sind UUID-Strings. Zeitwerte sind UTC-Epoch-Millis; die UI formatiert sie lokal deutsch.

### Sitzung

| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id` | String UUID | ja | UUID v4 | Room |
| `titel` | String | ja | „Neue Sitzung“ | Room |
| `titelManuell` | Boolean | ja | false | Room |
| `kiTitelVersucht` | Boolean | ja | false | Room |
| `erstelltAm`, `geaendertAm` | Long | ja | jetzt | Room |
| `istAngeheftet` | Boolean | ja | false | Room |
| `archiviertAm` | Long? | nein | null | Room |
| `auswertungsGrenzeNotizId` | String? | nein | null | Room |
| `pendingDeleteBis` | Long? | nein | null | Room |

### Notiz

| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id`, `sitzungId` | String UUID | ja | UUID / Referenz | Room |
| `titel`, `text` | String | ja | lokal erzeugt / Entwurf | Room |
| `titelManuell` | Boolean | ja | false | Room |
| `kiTitelVersucht` | Boolean | ja | false | Room |
| `quelle` | Enum `getippt`, `diktiert`, `gemischt`, `kiAntwort` | ja | abgeleitet | Room |
| `erstelltAm`, `geaendertAm` | Long | ja | jetzt | Room |
| `sortierPosition` | Long | ja | nächste Position | Room |
| `duplikatVonId` | String? | nein | null | Room |
| `pendingDeleteBis` | Long? | nein | null | Room |

### Entwurf und Diktat

| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `Entwurf.sitzungId` | String UUID | ja | offene Sitzung | Room |
| `Entwurf.text` | String | ja | leer | Room |
| `Entwurf.hatDiktat` | Boolean | ja | false | Room |
| `Diktat.id`, `sitzungId` | String UUID | ja | UUID / Referenz | Room |
| `Diktat.audioPfad` | String? | nein | null | privates Dateisystem |
| `Diktat.transkript` | String? | nein | null | Room |
| `Diktat.filterStatus` | JSON/String | ja | leer | Room |
| `Diktat.zustand` | Enum | ja | `aufnahme` | Room |
| `Diktat.dauerMs` | Long | ja | 0 | Room |

### NotizRevision

| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id`, `notizId` | String UUID | ja | UUID / Referenz | Room |
| `text` | String | ja | vorheriger Text | Room |
| `grund` | Enum `manuell`, `kiVerbesserung`, `wiederherstellung` | ja | — | Room |
| `erstelltAm` | Long | ja | jetzt | Room |

### AuswertungsSnapshot und Element

| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `Snapshot.id`, `sitzungId` | String UUID | ja | UUID / Referenz | Room |
| `fokusAntwort` | String | ja | — | Room |
| `profilName`, `profilPrompt` | String | ja | gewählte Fassung | Room |
| `modell` | Enum Sol/Terra/Luna | ja | Terra | Room |
| `reasoning` | Enum low/medium/high/xhigh/max | ja | medium | Room |
| `webmodus` | Enum `nurNotizen`, `notizenUndWeb` | ja | `nurNotizen` | Room |
| `erstelltAm` | Long | ja | jetzt | Room |
| `status` | Enum | ja | `bereit` | Room |
| `Element.notizId`, `titel`, `text`, `inhaltHash` | String | ja | Snapshotwerte | Room |
| `Element.erstelltAm`, `sortierPosition` | Long | ja | Snapshotwerte | Room |

### KiAntwort

| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id`, `sitzungId`, `snapshotId` | String UUID | ja | UUID / Referenzen | Room |
| `textOriginal` | String | ja | finale Antwort | Room |
| `quellenLinks` | Liste String | ja | leer | Room |
| `erstelltAm` | Long | ja | jetzt | Room |
| `grundlageBearbeitet`, `grundlageGeloescht` | Boolean | ja | false | Room/abgeleitet |
| `pendingDeleteBis` | Long? | nein | null | Room |

### Auswertungsprofil

| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id` | Int 1–6 | ja | fest | Room |
| `typ` | Enum `fest`, `frei` | ja | fest nach ID | Room |
| `name`, `prompt` | String | ja | F-15 | Room |
| `aktiv` | Boolean | ja | nur Normal | Room |

### Einstellungen und UI-Zustand

| Feld | Typ | Standard | Gespeichert |
|------|-----|----------|-------------|
| `theme` | Enum | `goldDunkel` | DataStore |
| `codexModell` | Enum | Terra | DataStore |
| `codexReasoning` | Enum | medium | DataStore |
| `ttsAnbieter` | Enum Chirp/Edge/Qwen | Edge | DataStore |
| `ttsStimmeId` | String? | Anbieterstandard | verschlüsselt, falls externe ID |
| `offeneSitzungId` | String? | letzte/neu | DataStore |
| `SitzungsUiZustand.scrollNotizId`, `.scrollOffsetDp` | String?/Int | null/0 | Room |

Secrets liegen ausschließlich in Keystore-gestütztem verschlüsseltem Speicher. Audio und TTS-Caches liegen ausschließlich als temporäre Dateien.

## 4. Zustände und Übergänge

| Einheit | Zustände / Übergänge |
|---------|----------------------|
| Aufnahme | `bereit → aufnehmend → gestoppt → transkribiert`; von `aufnehmend` über Abbrechen zu `verworfen`; bei 10:00 automatisch zu `gestoppt` |
| Transkription | `wartet → sendet → filtert → bestaetigung`; Fehler zu `wiederholbar`; Vollfilter zu `vollfilterPruefung`; Bestätigung zu `uebernommen` und Audio gelöscht |
| Notizlöschung | `sichtbar → pendingDelete(8 s) → geloescht`; Undo zurück zu `sichtbar` |
| Auswertung | `dialog → snapshotFixiert → wartet → blockVerarbeitung → gesamtauswertung → validierung → erfolgreich`; jeder Fehler zu `wiederholbar`, Grenze bleibt |
| TTS | `gestoppt → puffert → spielt → pausiert`; vor/zurück wechselt Absatz; Stopp leert Zustand |
| Codex Auth | `getrennt → codeWirdGeholt → wartetBestaetigung → verbunden`; Ablauf/Fehler zurück zu erneuerbarem Code |
| Qwen-Klon | `entwurf → probeBereit → upload → bereit`; Fehler zu `wiederholbar`; Löschen zu `loeschenAusstehend → geloescht` |

## 5. Externe Dienste

| Dienst | Wofür | Schlüssel / Anmeldung | Verhalten ohne Netz |
|--------|-------|------------------------|--------------------|
| Groq | Transkription mit `whisper-large-v3-turbo` | API-Key verschlüsselt | Aufnahme bleibt temporär, klare Wiederholen/Verwerfen-Auswahl |
| Codex | Titel, Textverbesserung, Auswertung | Device-Code Auth exakt wie PerfectMoment | Lokale Inhalte bleiben; Aktion meldet „Kein Netz“ |
| Microsoft Edge TTS | Vorlesen | dienstabhängig ohne gespeichertes Nutzerkonto | Wiedergabe pausiert und bietet Wiederholen/Wechsel |
| Google Chirp | Vorlesen | API-Key verschlüsselt | wie Edge |
| Qwen | Vorlesen und Stimmklonen | API-Key und Voice-ID verschlüsselt | wie Edge; Klonverwaltung bleibt lesbar |

## 6. Hintergrund und Lebenszyklus

| Lage | Verhalten |
|------|-----------|
| Aufnahme im Hintergrund/Display aus | Foreground-Service zeichnet weiter auf; Notification bietet Stoppen/Abbrechen; 10-Minuten-Limit bleibt verbindlich. |
| Transkription im Hintergrund | Persistenter Auftrag läuft weiter; Notification zeigt Fortschritt/Fehler, temporäres Audio bleibt bis Bestätigung. |
| KI-Auswertung im Hintergrund | Foreground-fähiger WorkManager-Auftrag läuft mit Notification weiter; Prozessneustart setzt idempotent fort. |
| TTS im Hintergrund | MediaSession spielt weiter; Mediennotification steuert Pause, Stopp, Absatz vor/zurück. |
| Fold-Wechsel/Rotation | Kein Auftrag startet neu; Sitzung, Entwurf, Scroll, Aufnahme, Transkription, Dialog, Auswertung und TTS bleiben erhalten. |
| App-Prozess beendet | Room/WorkManager/Services rekonstruieren persistente Zustände; unbestätigte Entwürfe und Audio bleiben erhalten, Caches werden regelgerecht bereinigt. |

## 7. Offene Fragen

Keine. Abgeleitete technische Entscheidungen sind ausdrücklich markiert und für v1 verbindlich.
