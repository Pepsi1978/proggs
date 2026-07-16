---
name: sound-search
description: "Sucht kostenlose CC0-Sound-Effekte auf Freesound und baut IMMER eine anklickbare HTML-Galerie mit 10 Vorschlaegen. Trigger: suche einen Sound, finde einen Klang, Sound-Effekt fuer, find me a sound."
---

# Sound Search (Cowork-Fassung) — kostenlose Sounds finden & anhoeren

Diese Cowork-Fassung sucht kostenlose CC0-Sound-Effekte auf Freesound, baut IMMER eine anklickbare
HTML-Galerie mit standardmaessig 10 nummerierten Vorschlaegen (Frank klickt, vergleicht, sagt dann
"Sound N einbauen") und laedt den gewaehlten Sound ins Projekt. Läuft in der **Claude-Cowork-Desktop-App**.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Galerie und geladene Sounds werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert**
(üblicherweise der gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad.
Wenn Cowork einen Temp-Ordner anbietet, kann die Vorschau-Galerie auch dort liegen; das Endergebnis
(der gewaehlte Sound) gehoert ins Projekt:

| Was | Relativer Pfad |
|-----|----------------|
| Vorschau-Galerie (10 MP3-Previews + auswahl.html) | `sound-search/<thema>/` (oder ein angebotener Temp-Ordner) |
| Gewaehlter Sound (Android) | `<projekt>/app/src/main/res/raw/<name>.ogg` bzw. `.wav` |

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt ein Ziel-/Zwischenordner → ERST anlegen
(Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfügbar), DANN schreiben. NIEMALS abbrechen, weil ein
Ordner fehlt. Anderer Basis-Ordner vom Benutzer genannt → dorthin.

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Mount-Brücke kann das **Dateiende abschneiden**. Nach JEDEM Schreiben
  (besonders der `auswahl.html`) das Dateiende prüfen (`tail -1`, `wc -l`).
- **~45s-Shell-Limit:** Jeder Such-/Download-/Schreib-Schritt muss in EINEM Shell-Aufruf durchlaufen;
  10 Previews am Stueck laden, nicht in vielen Einzelaufrufen.
- **Git NIEMALS nackt:** IMMER über `bash ~/proggs/cowork-git.sh` (Datenverlust-Wächter). NIE direktes `git`.
- **Browser/Audio plattformrichtig:** Cowork laeuft in einer Linux-VM. Kann die Galerie nicht
  automatisch im Browser geoeffnet werden, dem Benutzer den `auswahl.html`-Pfad nennen, damit er sie
  selbst oeffnet. Kein macOS-`afplay` auf fremder Plattform.

---

## Voraussetzung: Freesound-Key

**Frank hat bereits ein Freesound-Konto + API-Key.** NIEMALS eine Neu-Registrierung vorschlagen.
Key pruefen (liegt unter `~/.config/freesound/api_key`):
```bash
cat ~/.config/freesound/api_key 2>/dev/null
```
- **Key da** → SOFORT suchen (kein Account-Schritt, keine Rueckfrage).
- **Datei fehlt mal** (neuer Rechner/geloescht) → NICHT registrieren lassen. Sagen, dass nur die
  lokale Key-Datei fehlt, und Frank um seinen vorhandenen Key bitten, dann speichern:
  ```bash
  mkdir -p ~/.config/freesound && printf '%s' "THE_KEY" > ~/.config/freesound/api_key
  ```

## Schritt 1: Klaeren, welcher Sound gebraucht wird

Aus Kontext ableiten oder fragen:
- **Was soll der Sound ausloesen?** (Button-Click, Timer-Tick, Erfolg, Fehler, Benachrichtigung, Übergang)
- **Welche Stimmung?** (spielerisch, ernst, minimalistisch, retro, modern)
- **Wie lang?** (kurz < 1s, mittel 1-3s, lang > 3s)

## Schritt 2: Freesound durchsuchen

```bash
API_KEY=$(cat ~/.config/freesound/api_key)
curl -s "https://freesound.org/apiv2/search/text/?query=SUCHBEGRIFF&filter=duration:[0+TO+5]+license:\"Creative+Commons+0\"&fields=id,name,duration,avg_rating,num_ratings,previews,tags,license&page_size=8&sort=rating_desc&token=$API_KEY"
```

**Suchbegriffe (Englisch!) je App-Kontext:**
| App-Kontext | Suchbegriffe |
|-------------|--------------|
| Button-Click | `click ui`, `button tap`, `interface click` |
| Erfolg/Richtig | `success chime`, `correct answer`, `achievement` |
| Fehler/Falsch | `error buzz`, `wrong answer`, `negative beep` |
| Timer/Countdown | `tick tock`, `clock tick`, `timer beep` |
| Benachrichtigung | `notification chime`, `alert tone`, `ping` |
| Übergang/Swipe | `whoosh`, `swipe`, `transition swoosh` |
| Münzen/Punkte | `coin collect`, `point score`, `reward` |
| Levelaufstieg | `level up`, `fanfare short`, `upgrade` |

## Schritt 3: HTML-Galerie bauen + im Browser oeffnen (PFLICHT — Frank-Regel)

> **Bei JEDER Sound-Suche IMMER eine anklickbare HTML-Galerie bauen — NICHT die Sounds einzeln
> vorspielen.** Grund (Frank woertlich): "nur kurz anhoeren weiss ich ja gar nicht, welcher Sound war
> das, wie ordnen wir den jetzt zu. Es geht ja darum, dass ich mir einen aussuche." Er will klicken,
> vergleichen, dann sagen "Sound 3 einbauen".

Ablauf:
1. **Standardmaessig 10 Vorschlaege** sammeln (mehrere Suchbegriffe kombinieren, falls eine Query zu
   wenig CC0-Treffer liefert; `page_size` hoch genug, Duplikate per ID raus).
2. Die `preview-hq-mp3`-Dateien aller 10 in den Galerie-Ordner laden (`sound-search/<thema>/` relativ),
   Dateinamen nummeriert (`01_...mp3` … `10_...mp3`).
3. Eine `auswahl.html` daneben schreiben — **eine Karte pro Sound** mit: grosser Nummer ("Sound N"),
   Titel, 1-Satz-Beschreibung, `<audio controls preload="none" src="NN_...mp3">`, Freesound-Link
   (`https://freesound.org/s/<id>/`) und dem Hinweis 'sag: "Sound N einbauen"'. HTML **UTF-8 ohne BOM**
   schreiben (sonst kaputte Umlaute); Dateiende danach pruefen (Mount-Schreibfalle).
4. Im Browser oeffnen (plattformrichtig): `xdg-open <pfad/auswahl.html>` (Linux) bzw. `open` (macOS).
   Geht das in Cowork nicht automatisch → dem Benutzer den `auswahl.html`-Pfad nennen.
5. Frank waehlt per Nummer ("Sound 3 einbauen") → dann Schritt 4 (HQ laden + ins Projekt).

Optionales Zusatz-Vorspielen (nur auf Wunsch "spiel Nummer X nochmal"), plattformrichtig:
```bash
# macOS: afplay datei.mp3   |   Linux: ffplay -nodisp -autoexit datei.mp3   |   mpv datei.mp3
```

Wenn Frank einen waehlt — HQ laden + fuer Android konvertieren:
```bash
curl -sL "https://freesound.org/apiv2/sounds/[ID]/download/?token=$API_KEY" -o /tmp/sound_hq.wav
# Nach OGG Vorbis fuer Android (falls ffmpeg vorhanden):
ffmpeg -i /tmp/sound_hq.wav -c:a libvorbis -q:a 5 <projekt>/app/src/main/res/raw/<name>.ogg 2>/dev/null
# Oder WAV direkt kopieren:
cp /tmp/sound_hq.wav <projekt>/app/src/main/res/raw/<name>.wav
```

## Schritt 4: Zapsplat-Browse-Links (kein API)

Zapsplat hat keine oeffentliche API. Stattdessen Browse-Links nennen:
| Kategorie | Zapsplat-Pfad |
|-----------|---------------|
| UI/Interface | zapsplat.com/sound-effect-category/button-and-interface/ |
| Games | zapsplat.com/sound-effect-category/game-sounds/ |
| Alerts | zapsplat.com/sound-effect-category/alarms-and-alerts/ |
| Transitions | zapsplat.com/sound-effect-category/whoosh-and-swoosh/ |
| Comedy | zapsplat.com/sound-effect-category/comedy-and-cartoon/ |

## Schritt 5: Lizenz-Check

**DEFAULT: Nur CC0-Sounds** (kommerziell nutzbar, keine Attribution, Play Store OK). Harte Regel —
NIEMALS CC-BY-NC fuer Franks Projekte.
- **CC0**: frei, keine Attribution — DEFAULT, immer dieser Filter.
- **CC-BY**: nur wenn der Benutzer explizit mehr Optionen will (braucht Credits-Screen).
- **CC-BY-NC**: NIE verwenden — nicht kompatibel mit kommerziellen Play-Store-Apps.

## Proaktive Sound-Vorschlaege

Bei JEDER App mit UI-Interaktionen proaktiv anbieten:
> **Sound-Möglichkeiten für dein Projekt:**
> - Button-Taps: kurzer Click (50-100ms) für taktiles Feedback
> - Erfolgs-Sounds: aufsteigende Tonfolge bei richtigem Ergebnis
> - Fehler-Sounds: kurzer Buzz oder absteigender Ton
> - Übergänge: sanfter Whoosh beim Seitenwechsel
> - Hintergrund: dezente Ambient-Loops für Atmosphäre
>
> Soll ich passende Sounds auf Freesound suchen? Sag z.B. "suche einen Erfolgs-Sound".

## Bonus: SONNISS Free Pack

Jaehrliche kostenlose Packs von `sonniss.com/gameaudiogdc/` — professionelle Qualitaet,
royalty-free, keine Attribution, kommerzielle Nutzung erlaubt, WAV/Studio-Qualitaet. Gut fuer
Game-Effekte, Cinematic, Ambient.

---

## Sichern (Cowork-Git)

Wurde ein Sound ins Projekt gelegt → committen + pushen über das Cowork-Skript (nur eigene Pfade):
```bash
bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push-files "#NNN - sound-search: <name> Sound eingebaut" \
  <projekt>/app/src/main/res/raw/<name>.ogg
```
Kein Git-Repo verbunden → nur speichern und dem Benutzer den Ablage-Pfad nennen. Reine
Vorschau-Galerien ohne gewaehlten Sound muessen nicht committet werden.

## Was NIEMALS passieren darf

- CC-BY-NC-Sounds fuer Franks (kommerzielle) Projekte vorschlagen — nur CC0 (DEFAULT), CC-BY nur auf Wunsch.
- Eine Neu-Registrierung bei Freesound vorschlagen — der Key existiert; bei fehlender Datei nur den Key erfragen.
- Sounds einzeln vorspielen statt der anklickbaren 10er-HTML-Galerie (Frank will auswaehlen).
- HTML mit BOM schreiben (kaputte Umlaute) oder das Dateiende nach dem Schreiben nicht pruefen (Mount-Schreibfalle).
- Aus Cowork mit nacktem `git commit`/`git push` arbeiten — immer `cowork-git.sh`.
- macOS-`afplay` auf fremder Plattform aufrufen — plattformrichtigen Player nutzen (`ffplay`/`mpv`/`open`).

## Referenzen
- Freesound-API-Doku (offiziell): `freesound.org/docs/api/`.
- `bugs/claude-tooling/cowork-git-push.md` — die Cowork-Git-/Mount-Regeln im Arbeitsordner.
