# Aufgaben

Eigene Aufgaben-App für Android (Kotlin, Jetpack Compose, Room, Glance). Paket `de.frank.aufgaben`.

## Bedienung

- **Plus unten rechts** öffnet eine neue Seite und startet sofort die Aufnahme (abschaltbar).
  Spracherkennung: Groq **whisper-large-v3-turbo** mit den vier Filtern gegen stille
  Halluzinationen (Stille vorab, Segment-Metriken, Zeitstempel-Abgleich, Floskeln) — alle vier
  in den Einstellungen schaltbar.
- **KI-Korrektur** (ChatGPT per Codex-Anmeldung): macht aus dem Diktat eine Aufgabe in der
  Befehlsform („Verkaufe die Grafikkarte bei Kleinanzeigen.“); jeder weitere Tipp liefert eine neue
  Fassung, **Zurück** stellt das Original wieder her. Modelle: GPT-6 Astra, GPT-6.1 Sol, GPT-6 Luna,
  GPT-5.6 Terra.
- **Überschrift** tippt man selbst; bleibt sie leer, erzeugt die KI einen Titel.
- Tag, Uhrzeit, Dauer, Priorität, Erinnerung mit Vorlauf, Wiederholung und Checkliste direkt
  beim Anlegen. Sprache wie „morgen um 10 Uhr, dringend“ wird erkannt und mit einem Tipp übernommen.
- **Bereiche:** Heute, Morgen, Hoch, Mittel, Gering, Später (Eingang für alles Neue), dazu
  Demnächst und Erledigt. In Heute/Morgen sortiert nach Priorität (Hoch oben), dort gibt es kein
  „Später“ — beim Verschieben in einen Tag wird aus „Später“ „Mittel“.
- **Drag & Drop:** Karte lang drücken und ziehen. Oben erscheinen Ziele (Heute, Morgen,
  Übermorgen, Hoch, Mittel, Gering, Später), unten Erledigt und Löschen. Über der **Zeitleiste
  (5–22 Uhr)** zeigt links eine Uhrzeit mit, die dem Finger folgt (15-Minuten-Schritte);
  Loslassen macht die Aufgabe zum Termin. Zurück in den oberen Teil von Heute/Morgen = ohne Uhrzeit.
- **Erinnerungen** für Termine in Heute/Morgen, pünktlich oder mit Vorlauf; eingebauter Ton,
  Systemton oder eigene MP3 (wird in die App kopiert), eigene Lautstärke, Vibration, Aktionen
  „Erledigt“ und „In 10 Min.“.
- **Vorgelesene Erinnerungen:** Beim Speichern entstehen mit der gewählten Stimme (Edge, Google,
  eigene Qwen-Stimme) sechs Fassungen des Aufgabentextes als Dateien in der App
  (`filesDir/ansagen`). Die Erinnerung spielt Ton und Fassungen offline nacheinander ab, 3 s Pause
  dazwischen. Schalter **Als Wecker**: läuft in Schleife (höchstens 60 Min.), bis man in der
  Benachrichtigung „Ausschalten“ tippt, sie wegwischt, öffnet, „Erledigt“ oder „In 10 Min.“ wählt.
  Abspielen im Vordergrund-Dienst `ErinnerungsDienst`, Wecker per `setAlarmClock`. Ohne fertige
  Dateien spricht die Android-Stimme des Geräts.
- **Bereiche ohne Aufgaben** sind zugeklappt; Löschen im Editor fragt vorher nach.
- **Vorlesen** (Edge, Google Chirp 3 HD, eigene Qwen-Stimme) für einzelne Aufgaben und den ganzen Tag.
- **Widget „Aufgaben · Heute“**: um 0 Uhr werden die Morgen-Aufgaben automatisch zu Heute.
- Fokus-Timer, Serie („x Tage in Folge“), Suche, Konfetti, Rückgängig.
- **Hell / Automatisch / Dunkel** per Knopf oben (Symbol zeigt den aktuellen Modus).
- **Vier Designs** (Aurora, Garten, Abendglut, Kosmos) in Hell und Dunkel, jeweils mit animierter
  Szene: nachdenken → planen → umsetzen → freuen.

## Brücke zum Genialen Wecker (`de.frank.genialerwecker`)

Nur lesender ContentProvider, Authority `de.frank.aufgaben.wecker`:

| Adresse | Inhalt |
|---|---|
| `content://de.frank.aufgaben.wecker/heute` | offene Aufgaben von heute plus Überfälliges |
| `content://de.frank.aufgaben.wecker/morgen` | offene Aufgaben von morgen |
| `content://de.frank.aufgaben.wecker/tag/<epochDay>` | offene Aufgaben eines Tages |

Spalten: `_id, titel, text, uhrzeit (Minuten, -1 = ohne), zeit ("HH:MM"), prioritaet, erledigt, tag, vorlesetext`.
Sortierung: Termine nach Uhrzeit, danach nach Priorität.

Wird der Wecker abends gestellt, sind die Morgen-Aufgaben beim Klingeln die Heute-Aufgaben. Der
Wecker fragt deshalb am besten mit dem Klingeldatum: `/tag/<LocalDate.toEpochDay()>`.
Bei jeder Änderung sendet die App `de.frank.aufgaben.AUFGABEN_GEAENDERT` an `de.frank.genialerwecker`.
Der Wecker braucht dafür in seinem Manifest `<queries><provider android:authorities="de.frank.aufgaben.wecker" /></queries>`.

## Bauen

Version nur über `app/src/main/assets/versionslog.json` (neuester Eintrag unten).
Signatur: gemeinsamer Debug-Key (keine eigene signingConfig). Installation aufs Handy über den
Release-Build aus dem Skill `apk-update` (Debug-Compose ruckelt).
