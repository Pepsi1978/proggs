# SunoDownload

Sichert deine **komplette Suno-Bibliothek** als MP3-Dateien — durchnummeriert, mit dem Songtitel,
mit eingebettetem Cover, sortiert vom **ältesten zum neuesten** Song.

```
001 - Erster Song.mp3
002 - Zweiter Song.mp3
003 - Dritter Song.mp3
...
```

Zielordner: **`C:\Sono Backup`**

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

Gemessen verträgt Suno aber **25 gleichzeitige** Link-Anfragen ohne zu bremsen, und ein
Link, der beim ersten Abruf noch „wird vorbereitet" meldet, ist beim zweiten Abruf sofort
da. Daraus wurde: **erst alle anstoßen, dann alle einsammeln** — statt bei jedem Song
einzeln zu warten. Die Songliste wird über fünf Seiten gleichzeitig gelesen, geladen wird
mit acht Downloads parallel.

Gemessen: **12 Songs in 14 Sekunden**, samt Cover und Titel.

### Aus der Kommandozeile

```cmd
node downloader.ts                    ... Zielordner C:\Sono Backup, nur Neues
node downloader.ts "D:\Musik"
node downloader.ts --alles            ... ganze Bibliothek prüfen statt nur Neues
node downloader.ts --limit 15 "D:\Test"   ... Probelauf mit 15 Songs
```

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
| `icon-erzeugen.py` | Zeichnet `suno-backup.ico` neu |
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

---

## Qualität

Die Dateien sind **MP3 mit 192 kbit/s, 48 kHz, Stereo** — genau das, was Sunos eigener Knopf
„Download → MP3 Audio" liefert. Höher ginge nur die WAV-Fassung: rund zehnmal so groß, pro Song
einzeln anzufordern, hörbar praktisch kein Unterschied. Lohnend nur zum Weiterbearbeiten.

---

## Technisches

- **Node.js 24+** — führt TypeScript direkt aus, kein Build-Schritt nötig
- **node-id3** für die MP3-Informationen, **playwright-core** nur für den alten Browser-Weg
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

**Stand September 2026:** `/api/download/clip/<id>` antwortet für die meisten Songs nur noch
mit `ok:false / not_authorized` — es gibt keinen signierten Link mehr. Dafür steht in
`media_urls` eine m4a-Datei auf CloudFront (`d2lwuy8qc234o3.cloudfront.net`), die ohne
Anmeldung erreichbar ist. Der Downloader nimmt sie als reguläre Quelle und wandelt sie mit
`ffmpeg` in MP3 um. Ohne `ffmpeg` im Pfad schlagen diese Downloads fehl.

---

Version 1.6.1 (01.09.2026, 18:52 Uhr)
