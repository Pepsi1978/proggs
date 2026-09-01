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

Doppelklick auf **„Suno Backup"** auf dem Desktop (oder im Startmenü suchen). Das übernimmt alles:

1. Legt das Auslese-Skript in die Zwischenablage und öffnet Suno im Browser
2. Du machst **einmal**: `F12` → Reiter **Console** → **Strg+V** → **Enter**
   *(meckert Chrome, tippe `allow pasting`, Enter, dann nochmal Strg+V)*
3. Am Ende erscheint der Speichern-Dialog — Downloads-Ordner genügt
4. Der Rest läuft von allein: neue Songs werden geladen, mit Cover und Titel versehen

Beim **ersten Mal** holt es alles, danach **nur noch das Neue**. Das Programm wartet geduldig,
bis die Songliste da ist.

> Warum der Browser-Schritt? Google lässt keine Anmeldung in einem ferngesteuerten Browser zu.
> Deshalb liest dein eigener, angemeldeter Chrome die Liste aus. Das Herunterladen selbst
> braucht keine Anmeldung und macht das Programm allein.

---

## Die Nummerierung bleibt stabil

Welche Nummer zu welchem Song gehört, steht in **`_bestand.json`** im Zielordner. Einmal vergebene
Nummern ändern sich nie — neue Songs bekommen die nächsten freien. Auch wenn Suno die Reihenfolge
ändert oder ein älterer Song erst später auftaucht, wird **keine** deiner Dateien umbenannt.

Nebeneffekt: Ein Song, der nachträglich auftaucht, aber älter ist, landet trotzdem hinten. Das ist
der Preis dafür, dass deine Sammlung nie durcheinandergerät.

Wird eine Datei versehentlich gelöscht, merkt das Programm das beim nächsten Lauf und lädt genau
diese eine Datei wieder — unter demselben Namen.

---

## Was drinsteckt

| Datei | Aufgabe |
|-------|---------|
| `Neue-Songs-holen.cmd` | **Der Normalfall.** Erstlauf und Nachladen in einem — hinter dem Desktop-Symbol „Suno Backup" |
| `icon-erzeugen.py` | Zeichnet `suno-backup.ico` neu (nur nötig, wenn das Symbol geändert werden soll) |
| `bibliothek-holen.js` | Das Skript für die Chrome-Konsole (liest die Songliste aus) |
| `aktualisieren.ts` | Erkennt Neues, lädt es, setzt Cover und Titel, pflegt den Bestand |
| `gemeinsam.ts` | Geteilte Bausteine — Namen, Download, Cover, Titel |
| `suno-download.ts` | Reiner Download einer Songliste, ohne Bestandsführung |
| `cover-nachtragen.ts` | Trägt Cover und Titel in vorhandene Dateien nach |
| `titel-nachtragen.ts` | Holt fehlende Songtitel und benennt Dateien um |
| `Songs-laden.cmd` | Startet den reinen Download |

### Aus der Kommandozeile

```cmd
node aktualisieren.ts                                  ... findet Liste und Ordner selbst
node aktualisieren.ts "C:\Sono Backup\suno-liste.json" "D:\Musik"
```

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

### Private Songs (HTTP 403)

Songs, die in Suno nicht veröffentlicht sind (`is_public = false`), sperrt das Suno-CDN aus —
ein direkter Ladeversuch endet in `HTTP 403`. Für genau diese Songs holt das Konsolen-Skript
beim Erstellen der Liste einen signierten Download-Link (`download_url`) über den offiziellen
Endpunkt `/api/download/clip/<id>` mit, und der Nachlade-Lauf benutzt ihn.

Praktische Folge: Die Liste sollte **kurz vor** dem Ladelauf geholt werden — die signierten
Links sind zeitlich begrenzt. Meldet ein Lauf
`HTTP 403 — Song ist privat, es fehlt der Download-Link`, einfach das Konsolen-Skript noch
einmal ausführen und den Lauf wiederholen.

Nur die Links nachholen, ohne die ganze Bibliothek neu zu lesen:

```js
await sunoLinks(); sunoSpeichern();
```

---

Version 1.4.0 (01.09.2026, 06:25 Uhr)
