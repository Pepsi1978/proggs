# SunoDownload

Sichert deine **komplette Suno-Bibliothek** als MP3-Dateien — durchnummeriert, mit dem Songtitel,
mit eingebettetem Cover, sortiert vom **ältesten zum neuesten** Song.

```
001 - Erster Song.mp3
002 - Zweiter Song.mp3
003 - Dritter Song.mp3
...
```

Zielordner: **`C:\Suno Backup`**

---

## Bedienung: ein Doppelklick genügt

Doppelklick auf **„Suno Backup"** auf dem Desktop. Das Programm startet, legt einen
**Einzeiler** in die Zwischenablage und öffnet Suno im Browser.

1. Im Browser: `F12` → Reiter **Console** → **Strg+V** → **Enter**
   *(meckert Chrome, tippe `allow pasting`, Enter, dann nochmal Strg+V)*
2. Fertig. Songliste, Download-Links, Herunterladen, Cover und Titel laufen von allein.

Kein Speichern-Dialog, keine Datei hin- und herschieben: Der Browser reicht die Songliste
direkt an das laufende Programm weiter.

> Warum überhaupt der Browser? Suno lässt keine Anmeldung von außerhalb zu. Der
> Anmelde-Nachweis bleibt deshalb im Browser — an das Programm gehen nur die Songliste
> und die von Suno ausgestellten Download-Links.

### Warum es jetzt schnell geht

Früher wurde jeder Download-Link einzeln angefordert: 600 ms Pause, dazu bis zu 2,5 s
Warten, bis Suno den Link aufbereitet hatte. Bei 2400 Songs sind das Stunden.

Die Songliste wird darum über **acht Seiten gleichzeitig** gelesen (gemessen: über 170
Seiten kein einziger Fehlschlag), und geladen wird mit **acht Downloads parallel** — dort
bremst nichts.

Bei den Download-Links ist es seit September 2026 umgekehrt: Sie müssen **nacheinander**
geholt werden. Gemessen ergaben 4 gleichzeitige Abrufe 3-mal `rate_limited`; nacheinander
mit 1,5 s Abstand ging jeder durch. Die Brücke regelt die Pause selbst nach — sie verlängert
sie, sobald Suno bremst, und verkürzt sie wieder, wenn es glatt läuft.

**`rate_limited` ist keine Absage.** Suno antwortet darauf mit `ok:false` — genauso wie bei
`not_authorized`. Wer die beiden nicht auseinanderhält, wirft freigeschaltete Songs
stillschweigend weg. Ein gebremster Song wird deshalb erneut gefragt, ein gesperrter nicht.

### Aus der Kommandozeile

```cmd
node downloader.ts                    ... Zielordner C:\Suno Backup, nur Neues
node downloader.ts "D:\Musik"
node downloader.ts --limit 15 "D:\Test"   ... Probelauf mit 15 Songs
node downloader.ts --freischalten         ... zusätzlich selbst freischalten (verbraucht Kontingent)
node downloader.ts --still                ... ohne Zwischenablage und ohne Browser zu öffnen
node downloader.ts --alle-pruefen         ... jeden Song einzeln bei Suno nachfragen (langsam)
```

---

## Aufs Handy übertragen

Doppelklick auf **„Suno Handy-Abgleich"** auf dem Desktop. Das Skript vergleicht alle
MP3-Dateien in `C:\Suno Backup` mit dem Ordner `Suno Backup` im internen Speicher des
Handys und kopiert alles hinüber, was dort fehlt. Gedacht für die Musik im Auto.

```cmd
Handy-Abgleich.cmd          ... der Normalfall, mit Nachfrage am Ende
handy-abgleich.ps1 -Still   ... ohne Nachfrage, für automatische Läufe
```

**Einbahnstraße: PC → Handy.** Auf dem Handy wird **nie** etwas gelöscht. Dateien, die
nur dort liegen, bleiben unangetastet und werden nur gezählt.

**Nur MP3.** `_bestand.json`, die Protokolldateien und alles andere bleiben auf dem PC —
im Auto stören sie nur.

**Abgebrochene Übertragungen heilen sich.** Verglichen wird nicht nur der Dateiname,
sondern auch die Größe: Liegt auf dem Handy ein halber Song, wird er beim nächsten Lauf
neu übertragen. Meldet das Handy keine Größen, wird nur nach Namen verglichen — das sagt
das Skript dann auch.

**Voraussetzung:** Handy per USB angeschlossen, entsperrt, USB-Debugging eingeschaltet
(Einstellungen → Entwickleroptionen). Fehlt etwas davon, sagt das Skript genau was.

Nach dem Kopieren wird das Medienverzeichnis aufgefrischt, damit die neuen Lieder sofort
in der Musik-App auftauchen und nicht erst nach einem Neustart des Handys.

---

## Die Nummerierung bleibt stabil

Welche Nummer zu welchem Song gehört, steht in **`_bestand.json`** im Zielordner. Einmal vergebene
Nummern ändern sich nie — neue Songs bekommen die nächsten freien. Auch wenn Suno die Reihenfolge
ändert oder ein älterer Song erst später auftaucht, wird **keine** deiner Dateien umbenannt.

Nebeneffekt: Ein Song, der nachträglich auftaucht, aber älter ist, landet trotzdem hinten. Das ist
der Preis dafür, dass deine Sammlung nie durcheinandergerät.

Wird eine Datei versehentlich gelöscht, merkt das Programm das beim nächsten Lauf und lädt genau
diese eine Datei wieder — unter demselben Namen, egal wie alt der Song ist. Dafür sucht es so
lange weiter, bis es sie in der Bibliothek gefunden hat, auch wenn sonst nichts Neues da ist.

---

## Was drinsteckt

| Datei | Aufgabe |
|-------|---------|
| `Neue-Songs-holen.cmd` | **Der Normalfall.** Hinter dem Desktop-Symbol „Suno Backup" |
| `downloader.ts` | Das Programm: nimmt die Songliste entgegen, lädt, benennt, taggt |
| `bruecke.js` | Der Teil, der im Browser läuft — Songliste und Download-Links |
| `downloader.ps1` | Startet den Downloader mit Protokoll |
| `gemeinsam.ts` | Geteilte Bausteine — Namen, Nummern, Download, Cover, Titel |
| `Handy-Abgleich.cmd` | **Musik aufs Handy.** Hinter dem Desktop-Symbol „Suno Handy-Abgleich" |
| `handy-abgleich.ps1` | Der Abgleich selbst — vergleicht und kopiert per `adb push` |
| `icon-erzeugen.py` | Zeichnet `suno-backup.ico` neu |
| `icon-handy-erzeugen.py` | Zeichnet `handy-abgleich.ico` neu |
| `Alter-Weg-Songliste.cmd` | Rückfallweg über eine gespeicherte `suno-liste.json` |
| `aktualisieren.ts`, `bibliothek-holen.js` | Der alte Weg — bleibt als Rückfallebene |
| `suno-download.ts`, `cover-nachtragen.ts`, `titel-nachtragen.ts` | Einzelwerkzeuge |

#### Der alte Weg

Er bleibt vollständig erhalten: `Alter-Weg-Songliste.cmd` speichert die Songliste als
Datei und lädt daraus. Nötig ist er nur, wenn der neue Weg einmal nicht durchkommt.

---

## Gut zu wissen

**Abbruch ist kein Problem.** Einfach neu starten — Fertiges wird übersprungen. Halb geladene
Dateien heißen `.teil` und werden nie als fertige MP3 gewertet.

**Suno bremst.** Bei zu schnellen Abfragen antwortet Suno mit `429`. Das Auslese-Skript wartet dann
und wiederholt dieselbe Seite, statt sie zu überspringen — sonst fehlen Songs.

**Songs ohne Titel.** Hast du einen Song nie benannt, steht auf der Songseite die erste Textzeile.
Die wird als Name verwendet. Mehrere Dateien können dadurch gleich heißen — eindeutig bleiben sie
durch die vorangestellte Nummer.

**Ohne Cover.** Für einige ältere Songs hat Suno nie ein Cover erzeugt (nur ein rot-lila
Platzhalterbild). Diese Dateien bleiben ohne Bild.

**Protokolle.** Jeder Lauf schreibt nach `logs/*.jsonl` — dort steht bei einem Fehler die genaue
Ursache samt Adresse.

**Es wird nie mehr ewig gewartet.** Stolpert das Skript im Browser, meldet es den Fehler an
den Downloader, der ihn anzeigt und sich beendet. Und solange die Brücke arbeitet, schickt sie
bei jedem Seitenblock ein Lebenszeichen; bleibt es länger als drei Minuten aus, bricht der
Downloader mit einem Hinweis auf die Chrome-Konsole ab, statt stumm hängen zu bleiben.

---

## Qualität

Die Dateien sind **MP3 mit 192 kbit/s, 48 kHz, Stereo** — genau das, was Sunos eigener Knopf
„Download → MP3 Audio" liefert. Höher ginge nur die WAV-Fassung: rund zehnmal so groß, pro Song
einzeln anzufordern, hörbar praktisch kein Unterschied. Lohnend nur zum Weiterbearbeiten.

---

## Technisches

- **Node.js 24+** — führt TypeScript direkt aus, kein Build-Schritt nötig
- **node-id3** für die MP3-Informationen, **playwright-core** nur für den alten Browser-Weg
- **adb** (Android platform-tools) für den Handy-Abgleich — wird selbst gesucht, muss nicht im PATH stehen
- Keine Zugangsdaten im Code, kein gespeichertes Passwort, kein Token auf der Festplatte
- Das Konsolen-Skript liest ausschließlich und sendet nichts an Dritte

### Nicht im Repository

`node_modules/`, `logs/`, `.browser-profil/`, `suno-liste*.json` und alle MP3-Dateien sind per
`.gitignore` ausgeschlossen.

---

### Desktop-Symbol neu anlegen

Falls die Verknüpfung einmal verloren geht:

```powershell
$wsh = New-Object -ComObject WScript.Shell
$lnk = $wsh.CreateShortcut("$env:USERPROFILE\Desktop\Suno Backup.lnk")
$lnk.TargetPath = "C:\Users\barwa\proggs\SunoDownload\Neue-Songs-holen.cmd"
$lnk.WorkingDirectory = "C:\Users\barwa\proggs\SunoDownload"
$lnk.IconLocation = "C:\Users\barwa\proggs\SunoDownload\suno-backup.ico,0"
$lnk.Save()

$lnk = $wsh.CreateShortcut("$env:USERPROFILE\Desktop\Suno Handy-Abgleich.lnk")
$lnk.TargetPath = "C:\Users\barwa\proggs\SunoDownload\Handy-Abgleich.cmd"
$lnk.WorkingDirectory = "C:\Users\barwa\proggs\SunoDownload"
$lnk.IconLocation = "C:\Users\barwa\proggs\SunoDownload\handy-abgleich.ico,0"
$lnk.Save()
```

---

### Warum es ohne Download-Links nicht geht (HTTP 403)

Suno liefert Songs nicht mehr über `cdn1.suno.ai` aus — jeder direkte Ladeversuch endet in
`HTTP 403`, bei veröffentlichten Stücken genauso wie bei privaten. In der Songliste steht
als `audio_url` nur noch ein Platzhalter.

Der einzige tragfähige Weg ist der offizielle Endpunkt `/api/download/clip/<id>`: er stellt
einen zeitlich begrenzten, signierten Link aus. Genau die holt der Browser-Teil — und weil
sie ablaufen können, bleibt der Tab während des Ladens in Bereitschaft: Fällt ein Link um,
fordert das Programm von selbst einen frischen an. Der Handgriff `sunoLinks(true)` von
früher ist damit überflüssig.

**Der Browser-Tab muss offen bleiben**, bis das Programm fertig meldet.

**Stand September 2026 — nur freigeschaltete Songs:** Suno gibt den signierten Link nur noch
für *freigeschaltete* Songs aus; für alle anderen antwortet `/api/download/clip/<id>` mit
`not_authorized`.

**Woran erkennt das Programm, ob ein Song freigeschaltet ist?** Am Feld
`is_download_unlocked` in der Songliste — nachgemessen am 05.09.2026: 10 von 3247 Songs
standen auf `true`, und eine Gegenprobe an 20 gesperrten Songs quer durch die Bibliothek
ergab 20-mal `not_authorized`. Das Feld stimmt also.

Es entscheidet aber nicht allein: Das letzte Wort hat der Link-Abruf
`/api/download/clip/<id>`. Kommt ein Link, wird geladen; kommt `not_authorized`, bleibt der
Song liegen. Und sollte Suno das Feld einmal wieder umbenennen — das ist schon zweimal
passiert —, merkt die Brücke, dass gar kein Freischalt-Feld mehr da ist, und fragt
stattdessen für jeden Song einzeln nach. Erzwingen lässt sich das mit `--alle-pruefen`.
Der Link-Abruf kostet **nichts**; Kontingent verbraucht allein das Freischalten.

**Freigeschaltet wird von Hand auf suno.com.** Der Downloader schaltet von sich aus **nie**
etwas frei. Er lädt genau die Songs, die du selbst freigeschaltet hast und die noch nicht auf
der Platte liegen. Was gesperrt ist, wird am Ende namentlich aufgezählt (die ersten zehn) —
so siehst du, was du bei Suno noch freischalten müsstest.

Nur mit `--freischalten` schaltet die Brücke zusätzlich selbst frei: `POST
/api/download/authorize` (`item_id`, `item_type: "clip"`), das verbraucht je einen Download
aus dem Monatskontingent des Abos (Premier: 60 pro Monat plus gekaufte Zusatz-Downloads;
Stand in `/api/billing/info/` → `download_usage`). Auch dann wird kein Kontingent verschenkt:
freigeschaltet wird erst, nachdem der Link-Abruf gelaufen ist, und nur für die Songs, die
dabei wirklich abgelehnt wurden — älteste zuerst, bis das Kontingent erschöpft ist.

**Die ganze Bibliothek wird jedes Mal gelesen, ohne Frühstopp.** Ein Song, den du gestern
von Hand freigeschaltet hast, kann drei Jahre alt sein und liegt dann tief in der Liste. Ein
Durchlauf über gut 160 Seiten dauert eine knappe halbe Minute — der Preis dafür, nichts zu
verpassen.

**Woher die Songliste kommt — und warum das der eigentliche Fehler war.** Bis Version 1.6.7
las die Brücke `/api/feed/v2`. Dieser Endpunkt meldete zuletzt `num_total_results: 21`: Er
zeigt nur die letzten Erzeugungen, nicht die Bibliothek. Von 3247 Songs kannte das Programm
also 21 — freigeschaltete Songs, die tiefer lagen, konnte es gar nicht finden. Die
vollständige Bibliothek steht in `/api/project/default`: `clip_count` nennt die Gesamtzahl,
die Songs stehen als `project_clips[].clip`. Zwei Fallstricke dort: Die Seiten sind
**eins-basiert** (`page=0` liefert dieselbe Seite wie `page=1`), und `page_size` wird
ignoriert — es sind immer 20 pro Seite.

Die m4a aus `media_urls` (CloudFront) ist zwar ohne Anmeldung erreichbar, aber verschlüsselt —
sie taugt nicht als Quelle.

---

Version 1.8.0 (05.09.2026, 13:06 Uhr)
