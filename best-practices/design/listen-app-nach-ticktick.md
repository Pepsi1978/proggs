# Listen-App nach dem Vorbild von TickTick: Funktionsumfang als Vorlage

> Stand 12.11.2026. Quelle: offizielles TickTick-Hilfecenter (help.ticktick.com, 97 Artikel aus dem eingebetteten Seiten-JSON), Open-API-Doku,
> Changelog. Markierung: **offiziell** = Hilfecenter/API, **extern** = Drittquellen. Umgesetzt in Jarvis 1.8.638 (`Jarvis/.../listen/`).

## Kurzcheck (Stufe A)

| # | Bereich | Was TickTick hat (offiziell) | Übernahme-Empfehlung |
|---|---------|------------------------------|----------------------|
| 1 | Hierarchie | Ordner → Liste → Abschnitt (Section) → Aufgabe → Unteraufgabe (max. 5 Ebenen) | Liste, Abschnitt, Unterpunkte bis 5 Ebenen; Ordner nur bei vielen Listen |
| 2 | Prioritäten | 4 Stufen: hoch, mittel, niedrig, keine (API 5/3/1/0) | 0–3 speichern; Farben rot/gelb/blau/grau sind Konvention (extern) |
| 3 | Gruppieren | Custom (Abschnitte), Liste, Zeit (Überfällig/Heute/Morgen/7 Tage/Später), Priorität, Tag, Erstellzeit, Assignee | je Liste speichern |
| 4 | Sortieren | innerhalb der Gruppe: Datum, Priorität, Erstellzeit, Änderungszeit (neu/alt), dazu manuell per Drag & Drop | Unterpunkte nur auf ihrer Ebene sortieren, angeheftete immer oben |
| 5 | Abhaken | Alle Check Items erledigt → Aufgabe erledigt; „Won't Do“ mit „Restart“ | Eltern nimmt Kinder mit, letztes Kind schließt Eltern, ein geöffnetes Kind öffnet Eltern |
| 6 | Schnelleingabe | `!` Priorität, `#` Tag, `~`/`^` Liste (offiziell); `*` Datum (extern) | `!hoch`/`!1–3` und `#tag` reichen für den Anfang |
| 7 | Listen | Task- und Note-Listen, Ansichten Liste/Kanban/Timeline, Farbe, Emoji, Smart Lists (Heute, 7 Tage, Alle, Erledigt) | Art mit Symbol und Farbe; Kanban, Timeline und Smart Lists optional |
| 8 | Limits | Free 9 Listen/99 Aufgaben je Liste, Premium 299/999 (extern bestätigt) | keine künstlichen Limits |

## Volltext

### Datenmodell, das sich bewährt hat
- Liste: id, Name, Art, Farbe, Sortierung, Gruppierung, „Erledigte zeigen“, Abschnitte (Reihenfolge), Position, erstellt/geändert, Grabstein.
- Eintrag: id, Liste, Eltern-id (0 = oben), Titel, Notiz, Priorität 0–3, Fälligkeit (ISO-Datum), Tags, erledigt + Zeitpunkt, Position (Double),
  Anheften, Abschnitt, erstellt/geändert, Grabstein.
- Je Liste und je Eintrag eine eigene Datei. Ein Datei-Abgleich zwischen Geräten löst Konflikte dann je Eintrag statt je ganzer Liste.
  Gelöscht wird nur als Grabstein in der Datei, sonst holt der Abgleich gelöschte Einträge zurück.

### Sprache (Assistent)
- Das Werkzeug löst den Listennamen selbst auf: gleicher Name, Teilwort, dann die einzige Liste der erkennbaren Art („Einkaufsliste“ → Art
  Einkauf). Genau ein Treffer schreibt sofort, mehrere geben die Kandidaten als Rückfrage zurück. Fehlt die Liste ganz, legt das Hinzufügen sie an.
- Hinweis an das Modell: Listen gegen Aufgaben-Apps abgrenzen („Dinge auf einer Liste“ vs. „Aufgabe mit Termin“).

### Quellen
- help.ticktick.com (Hilfecenter, u. a. Subtasks, Sort & Group, Smart Lists, Quick Add), developer.ticktick.com (Open API: `priority`, `sortOrder`, `completedTime`)
- ticktick.com/blog (Premium 101, 2020; Sortier-Upgrade 18.04.2023), Changelog 15.09.2026 (Sortieren nach Erstell- und Änderungszeit)
- Rohberichte der Recherche: Sitzung 12.11.2026 (4 Sonnet-Researcher)

### Offen / unbelegt
- Ob das Abhaken der Eltern-Aufgabe die Unteraufgaben mit abhakt, steht in keiner offiziellen Quelle; die Hilfe widerspricht sich bei der Auto-Erledigung (Check Items vs. Subtasks).
- Eine Funktion „Erledigte löschen“ und eine kompakte Ansicht sind offiziell nicht dokumentiert.
