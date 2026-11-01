# Jarvis: Vorlese-Lautstärke bei Erinnerungen

- Fehler: Die Lautstärke des Meldungsprofils „Erinnerungen“ war gespeichert, wurde beim Vorlesen aber nicht angewandt.
- Ursache: `ErinnerungStimme.sprich` spielt vorbereitete Dateien direkt mit MediaPlayer bzw. System-TTS statt über `Ansage` ab. Diese zweite Wiedergabestrecke übernahm nur Stimme und Tempo, nicht die temporäre Stream-Lautstärke.
- Fix: `Ansage.mitLautstaerke` kapselt die bestehende temporäre Lautstärkeverwaltung je Tonweg. Ansagen und Erinnerungen nutzen sie gemeinsam; Erinnerungswecker verwenden den Alarm-, normale Erinnerungen den Medien-Tonweg. Die Systemstimme übernimmt dieselben AudioAttributes wie die vorbereitete Datei.
- Absicherung: 0 („Wie Handy“) ändert keinen Tonweg, Werte werden begrenzt, Aufräumen erfolgt im finally; manuelle Lautstärkeänderungen haben Vorrang. Überlappende Ansagen zählen je Stream, erst die letzte stellt zurück; Android-Fehler werden protokolliert.
- Verwandte Stellen: Gemeinsame Meldungsprofile, `Meldungen.vorlesen`, Update-Ansagen, Installationsfrage und Modellwahl verwenden bereits `Ansage`; Timer-Vorleselautstärke ist vom Weckton getrennt. Beide Erinnerungsformen und beide Stimmenwege sind erfasst.
- Funktionserhalt: Schlüssel und gespeicherte Werte bleiben gleich; Offline-Cache, Systemstimmen-Rückfall und vorhandene Stimmen-/Tempoeinstellungen bleiben erhalten. Abnahme durch Build und Frank im Schnellmodus, keine akustischen Tests in dieser Sitzung.
- Muster: Bei neuen direkten Player-/TTS-Strecken nicht nur das Stimmprofil lesen, sondern auch dessen Lautstärke am tatsächlich verwendeten Tonweg anwenden.

## Nachprüfung im sehr ausführlichen Review

- Die Lautstärkeverwaltung verwendet jetzt einzelne Wahlen je laufender Ansage statt nur eines Zählers. Endet ein Einschub, wird die vorherige Wahl wiederhergestellt; auch „Wie Handy“ wird gegen einen noch laufenden Einschub geschützt. Neue manuelle Änderungen werden zur Ausgangslautstärke und bleiben erhalten.
- Ein Fehler beim Lesen des Android-Tonwegs verhindert das Vorlesen nicht mehr. Nach Fristablauf wird nur die noch laufende eigene Quelle beendet; ein abgebrochener Einschub räumt seinen Player vor dem Zurückstellen auf.
- Die Erinnerungs-Probe nutzt dieselbe Wiedergabe wie fällige Erinnerungen, ohne eine Erinnerung anzulegen. „Wie Handy“ wird im Einstellungskatalog verständlich gelesen und direkt angenommen.
- Vollständiger Review-Bericht: `Jarvis/REVIEW-AQTU6.md`.
