---
name: sound-search
description: "Sucht kostenlose CC0-Sound-Effekte von Freesound und baut IMMER eine anklickbare HTML-Galerie mit standardmaessig 10 nummerierten Vorschlaegen, die im Browser geoeffnet wird. Nutze IMMER wenn der Benutzer sagt: 'suche einen Sound', 'suche mir einen kostenlosen Sound', 'finde einen Klang', 'Sound-Effekt fuer', 'find me a sound', 'search for a click/notification sound', oder wenn eine App Audio braucht."
---

# Sound Search Skill — Find & Preview Audio for Apps

## Prerequisites

**Frank hat bereits ein Freesound-Konto + API-Key** (siehe Memory `reference_freesound_account_exists`).
NIEMALS eine Neu-Registrierung vorschlagen. Key pruefen (Windows-Pfad):
```bash
cat ~/.config/freesound/api_key 2>/dev/null   # Windows: %USERPROFILE%\.config\freesound\api_key
```

- **Key da** → SOFORT suchen (kein Account-Schritt, keine Rueckfrage).
- **Datei fehlt mal** (neuer Rechner / geloescht) → NICHT registrieren lassen. Sagen, dass nur
  die lokale Key-Datei fehlt, und Frank um seinen vorhandenen Key bitten, dann speichern:
  ```powershell
  $d = Join-Path $env:USERPROFILE ".config\freesound"; New-Item -ItemType Directory -Force -Path $d | Out-Null
  Set-Content -Path (Join-Path $d "api_key") -Value "THE_KEY" -NoNewline -Encoding ascii
  ```

## Step 1: Understand What Sound Is Needed

Ask the user (or infer from context):
- **Was soll der Sound auslösen?** (Button-Click, Timer-Tick, Erfolg, Fehler, Benachrichtigung, Übergang)
- **Welche Stimmung?** (spielerisch, ernst, minimalistisch, retro, modern)
- **Wie lang?** (kurz < 1s, mittel 1-3s, lang > 3s)

## Step 2: Search Freesound

```bash
API_KEY=$(cat ~/.config/freesound/api_key)
# Search with filters for short sounds suitable for apps
curl -s "https://freesound.org/apiv2/search/text/?query=SUCHBEGRIFF&filter=duration:[0+TO+5]+license:\"Creative+Commons+0\"&fields=id,name,duration,avg_rating,num_ratings,previews,tags,license&page_size=8&sort=rating_desc&token=$API_KEY"
```

**Search tips for good results:**
| App-Kontext | Suchbegriffe (Englisch!) |
|-------------|------------------------|
| Button-Click | `click ui`, `button tap`, `interface click` |
| Erfolg/Richtig | `success chime`, `correct answer`, `achievement` |
| Fehler/Falsch | `error buzz`, `wrong answer`, `negative beep` |
| Timer/Countdown | `tick tock`, `clock tick`, `timer beep` |
| Benachrichtigung | `notification chime`, `alert tone`, `ping` |
| Übergang/Swipe | `whoosh`, `swipe`, `transition swoosh` |
| Münzen/Punkte | `coin collect`, `point score`, `reward` |
| Levelaufstieg | `level up`, `fanfare short`, `upgrade` |

## Step 3: HTML-Galerie bauen + im Browser oeffnen (PFLICHT — Frank-Regel 2026-06-07)

> **Bei JEDER Sound-Suche IMMER eine anklickbare HTML-Galerie bauen und im Browser oeffnen —
> NICHT die Sounds einzeln im Terminal vorspielen.** Grund (Frank woertlich): "nur kurz anhoeren
> weiss ich ja gar nicht, welcher Sound war das, wie ordnen wir den jetzt zu. Es geht ja darum,
> dass ich mir einen aussuche." Er will klicken, vergleichen, dann sagen "Sound 3 einbauen".

Ablauf:
1. **Standardmaessig 10 Vorschlaege** sammeln (mehrere Suchbegriffe kombinieren, falls eine
   Query zu wenig CC0-Treffer liefert — `page_size` hoch genug, Duplikate per ID raus).
2. Die `preview-hq-mp3`-Dateien aller 10 in einen Temp-Ordner laden
   (`$env:TEMP\<thema>_sounds\`), Dateinamen nummeriert (`01_...mp3` … `10_...mp3`).
3. Eine `auswahl.html` daneben schreiben — **eine Karte pro Sound** mit: grosser Nummer
   ("Sound N"), Titel, 1-Satz-Beschreibung, `<audio controls preload="none" src="NN_...mp3">`,
   Freesound-Link (`https://freesound.org/s/<id>/`) und dem Hinweis 'sag: "Sound N einbauen"'.
   HTML **UTF-8 ohne BOM** schreiben (`[System.IO.File]::WriteAllText($p,$html,(New-Object System.Text.UTF8Encoding $false))`),
   sonst kaputte Umlaute.
4. Im Browser oeffnen: `Start-Process <pfad\auswahl.html>` (Windows) bzw. `open` (macOS).
5. Frank waehlt per Nummer ("Sound 3 einbauen") → dann Step 4 (HQ laden + ins Projekt).

Optionales Zusatz-Vorspielen im Terminal (nur auf Wunsch "spiel Nummer X nochmal") —
plattformrichtig, **kein** macOS-`afplay` auf Windows:
```powershell
# Windows: MP3 hoerbar abspielen
Add-Type -AssemblyName PresentationCore
$p = New-Object System.Windows.Media.MediaPlayer
$p.Open([Uri]"C:\...\03_....mp3"); $p.Play(); Start-Sleep 3; $p.Close()
```
```bash
# macOS: afplay datei.mp3   |   Linux: ffplay -nodisp -autoexit datei.mp3
```

If the user likes it:
```bash
# Download high-quality version (requires OAuth for some licenses)
curl -sL "https://freesound.org/apiv2/sounds/[ID]/download/?token=$API_KEY" -o /tmp/sound_hq.wav
# Convert to OGG Vorbis for Android (if ffmpeg available)
ffmpeg -i /tmp/sound_hq.wav -c:a libvorbis -q:a 5 ~/proggs/[Project]/app/src/main/res/raw/[name].ogg 2>/dev/null
# Or copy WAV directly
cp /tmp/sound_hq.wav ~/proggs/[Project]/app/src/main/res/raw/[name].wav
```

## Step 5: Zapsplat Suggestions

Zapsplat has no public API. Generate browse links instead:
```
Zapsplat-Suche: https://www.zapsplat.com/sound-effect-category/[category]/
```

| Kategorie | Zapsplat-URL |
|-----------|-------------|
| UI/Interface | https://www.zapsplat.com/sound-effect-category/button-and-interface/ |
| Games | https://www.zapsplat.com/sound-effect-category/game-sounds/ |
| Alerts | https://www.zapsplat.com/sound-effect-category/alarms-and-alerts/ |
| Transitions | https://www.zapsplat.com/sound-effect-category/whoosh-and-swoosh/ |
| Comedy | https://www.zapsplat.com/sound-effect-category/comedy-and-cartoon/ |

## Step 6: License Check

**DEFAULT: Nur CC0-Sounds** (kommerziell nutzbar, keine Attribution, Play Store OK).
This is a hard rule — NEVER suggest CC-BY-NC sounds for this user's projects.

- **CC0**: Free, no attribution — DEFAULT, always use this filter
- **CC-BY**: Only if user explicitly asks for more options (must add credits screen)
- **CC-BY-NC**: NEVER use — not compatible with Play Store commercial apps

## Proactive Sound Suggestions

When working on ANY app with UI interactions, proactively suggest:

> **Sound-Möglichkeiten für dein Projekt:**
> - Button-Taps: Kurzer Click (50-100ms) für taktiles Feedback
> - Erfolgs-Sounds: Aufsteigende Tonfolge bei richtigem Ergebnis
> - Fehler-Sounds: Kurzer Buzz oder absteigender Ton
> - Übergänge: Sanfter Whoosh beim Seitenwechsel
> - Hintergrund: Dezente Ambient-Loops für Atmosphäre
>
> Soll ich passende Sounds auf Freesound suchen? Sag einfach z.B. "suche einen Erfolgs-Sound"

## SONNISS Free Pack (Bonus)

Annual free packs from https://sonniss.com/gameaudiogdc/ — professional quality, royalty-free:
- No attribution needed
- Commercial use allowed
- WAV format, studio quality
- Good for: game effects, cinematic, ambient
