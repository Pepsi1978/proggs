# Suno-API: Feed-Endpunkt liefert 21 statt 3247 Songs — Bibliothek liegt woanders

## Symptom

Der SunoDownload-Downloader fand freigeschaltete, noch nicht gesicherte Songs nicht. Ein vom
Benutzer namentlich genanntes Lied („Waldhütte im Morgenlicht (Fade Out)", freigeschaltet am
03.09.2026) tauchte in keinem Lauf auf. Zwei aufeinanderfolgende Reparaturversuche schlugen
fehl, weil beide an der falschen Stelle ansetzten: an der Erkennung der Freischaltung statt an
der Herkunft der Songliste.

## Ursache

`GET /api/feed/v2?page=N&page_size=20` meldete `num_total_results: 21`. Der Endpunkt zeigt nur
die letzten Erzeugungen, **nicht** die Bibliothek. Von 3247 Songs sah das Programm also 21 —
alles Ältere war unerreichbar, unabhängig vom Freischaltungs-Status. Der Fehler war nicht
sichtbar, weil `has_more: true` gemeldet wurde und die Paginierung scheinbar normal lief.

Zwei Folgefehler kamen dazu:

1. **`rate_limited` wurde als Absage gewertet.** `/api/download/clip/<id>` antwortet auf
   Bremsung mit `{ok:false, reason:"rate_limited"}` — syntaktisch identisch zu
   `{ok:false, reason:"not_authorized"}`. Eine Prüfung auf `d.ok === false` wirft damit
   freigeschaltete Songs stillschweigend weg.
2. **Die Handle-Erkennung war ein Wettlauf.** `ich` wurde aus `clips[0]` derjenigen von fünf
   parallel gelesenen Seiten gesetzt, die zuerst antwortete. Gewinnt dabei ein Fremdstück
   (Vorlage einer Coverversion), filtert der Handle-Vergleich anschließend die gesamte eigene
   Bibliothek weg.

## Fix

- Songliste aus `GET /api/project/default?page=N`. `clip_count` nennt die Gesamtzahl, die Songs
  stehen als `project_clips[].clip`.
- **Seiten sind eins-basiert**: `page=0` liefert dieselbe Seite wie `page=1`. `page_size` wird
  ignoriert — es sind immer 20 pro Seite.
- `is_download_unlocked` als Vorfilter, aber nur wenn das Feld als eigene Eigenschaft auf einem
  Clip existiert; fehlt es ganz, wird für jeden Song einzeln beim Link-Endpunkt nachgefragt.
- `rate_limited` (`/rate_?limit|too_?many|slow_?down|throttl/i`) führt zu Wiederholung mit
  längerer Pause, `not_authorized` zu endgültigem Ausschluss.
- Link-Abrufe sequenziell mit selbst nachregelnder Pause.
- Seite 1 wird zuerst allein gelesen, um Handle und Gesamtzahl deterministisch zu bestimmen.

## Messwerte (05.09.2026, echte Bibliothek mit 3247 Songs)

| Messung | Ergebnis |
|---|---|
| `/api/feed/v2` → `num_total_results` | **21** |
| `/api/project/default` → `clip_count` | **3247** |
| `is_download_unlocked === true` | 10 von 3247 |
| Gegenprobe: 20 gesperrte Songs am Link-Endpunkt | 20× `not_authorized` (Feld stimmt) |
| 8 Bibliotheks-Seiten gleichzeitig, 170 Seiten | 0 Fehlschläge |
| 4 gleichzeitige `/api/download/clip`-Abrufe | 3× `rate_limited` |
| Sequenziell mit 1,5 s Abstand | jeder Abruf erfolgreich |
| Erholung nach `rate_limited` | nach ~12 s wieder frei |

Antwortformen von `/api/download/clip/<id>` (alle mit HTTP 200):

```
{"ok":true,  "download_url":"…", "status":"ready"}                        freigeschaltet
{"ok":false, "reason":"not_authorized", "message":"Seems like you don't…"} gesperrt, endgültig
{"ok":false, "reason":"rate_limited",   "message":"Woah, too many downl…"} zu schnell, wiederholen
```

## Verifikation

Vollständiger Lauf am 05.09.2026: 3218 Songs gelesen, 188 fehlend, davon 3 freigeschaltet —
alle drei geladen, mit Cover und Titel, darunter das genannte Beispiellied.

## Übertragbare Lehre

**Wenn eine Suche nichts findet, zuerst die Grundgesamtheit prüfen, nicht den Filter.** Zwei
Reparaturversuche gingen an der Freischaltungs-Erkennung vorbei, weil niemand `num_total_results`
gegen die erwartete Bibliotheksgröße gehalten hat. Eine Zeile — Gesamtzahl der Quelle gegen
Gesamtzahl auf der Platte — hätte den Fehler sofort gezeigt.

**Zwei verschiedene Fehler mit derselben Hülle sind eine Falle.** `ok:false` bedeutete hier
sowohl „nie" als auch „gleich nochmal". Wo eine API Absage und Bremsung im selben Feld
ausdrückt, muss der Grund ausgewertet werden — sonst wird aus einer Bremsung stiller Datenverlust.
