---
name: sound-search
description: "Sucht kostenlose CC0-Sounds auf Freesound und baut IMMER eine anklickbare HTML-Galerie mit 10 nummerierten Vorschlaegen im Browser. Trigger: suche einen Sound, finde einen Klang, Sound-Effekt fuer."
---

# Sound Search (Cowork-Fassung) — kostenlose CC0-Sounds finden & vergleichen

Findet kostenlose CC0-Sound-Effekte auf Freesound und baut IMMER eine anklickbare HTML-Galerie mit
standardmaessig 10 nummerierten Vorschlaegen, die im Browser geoeffnet wird — damit man die Sounds
vergleichen und per Nummer auswaehlen kann. Laeuft in der **Claude-Cowork-Desktop-App**.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Alle Dateien werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert** (ueblicherweise der
gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad:

| Was | Relativer Pfad |
|-----|----------------|
| Temp-Galerie + MP3-Vorschauen | `sound-search/<thema>/` (Galerie `auswahl.html` + `01_...mp3` … `10_...mp3`) |
| Finaler Sound ins Projekt (nach Auswahl) | `<projektordner>/app/src/main/res/raw/<name>.ogg` bzw. `.wav` |

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt ein Ziel-/Zwischenordner → ERST anlegen
(Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfuegbar), DANN schreiben. NIEMALS abbrechen, weil ein
Ordner fehlt. Anderer Basis-Ordner vom Benutzer genannt → dorthin (gleiche Struktur).

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Mount-Bruecke kann das **Dateiende abschneiden**. Nach JEDEM Schreiben
  (besonders der `auswahl.html`) das Dateiende pruefen (`tail -1`, `wc -l`).
- **~45s-Shell-Limit:** Jeder curl-/Schreib-/Git-Schritt muss in EINEM Shell-Aufruf durchlaufen;
  Hintergrundprozesse ueberleben den Aufruf-Wechsel nicht.
- **Git NIEMALS nackt:** IMMER ueber `bash ~/proggs/cowork-git.sh` (Datenverlust-Waechter). NIE direktes `git`.

---

## Voraussetzung: Freesound-Konto + API-Key

**Frank hat bereits ein Freesound-Konto + API-Key.** NIEMALS eine Neu-Registrierung vorschlagen.
Key pruefen:
```bash
cat ~/.config/freesound/api_key 2>/dev/null
```
- **Key da** → SOFORT suchen (kein Account-Schritt, keine Rueckfrage).
- **Datei fehlt mal** (neuer Rechner / geloescht) → NICHT registrieren lassen. Sagen, dass nur die
  lokale Key-Datei fehlt, und Frank um seinen vorhandenen Key bitten, dann speichern:
  ```bash
  mkdir -p ~/.config/freesound && printf '%s' "THE_KEY" > ~/.config/freesound/api_key
  ```

## Schritt 1: Verstehen, welcher Sound gebraucht wird

Den Benutzer fragen (oder aus dem Kontext ableiten):
- **Was soll der Sound ausloesen?** (Button-Click, Timer-Tick, Erfolg, Fehler, Benachrichtigung, Uebergang)
- **Welche Stimmung?** (spielerisch, ernst, minimalistisch, retro, modern)
- **Wie lang?** (kurz < 1s, mittel 1-3s, lang > 3s)

## Schritt 2: Freesound durchsuchen

```bash
API_KEY=$(cat ~/.config/freesound/api_key)
curl -s "https://freesound.org/apiv2/search/text/?query=SUCHBEGRIFF&filter=duration:[0+TO+5]+license:\"Creative+Commons+0\"&fields=id,name,duration,avg_rating,num_ratings,previews,tags,license&page_size=8&sort=rating_desc&token=$API_KEY"
```

**Suchtipps fuer gute Treffer (Suchbegriffe IMMER Englisch):**
| App-Kontext | Suchbegriffe |
|-------------|--------------|
| Button-Click | `click ui`, `button tap`, `interface click` |
| Erfolg/Richtig | `success chime`, `correct answer`, `achievement` |
| Fehler/Falsch | `error buzz`, `wrong answer`, `negative beep` |
| Timer/Countdown | `tick tock`, `clock tick`, `timer beep` |
| Benachrichtigung | `notification chime`, `alert tone`, `ping` |
| Uebergang/Swipe | `whoosh`, `swipe`, `transition swoosh` |
| Muenzen/Punkte | `coin collect`, `point score`, `reward` |
| Levelaufstieg | `level up`, `fanfare short`, `upgrade` |

## Schritt 3: HTML-Galerie bauen + im Browser oeffnen (PFLICHT)

> **Bei JEDER Sound-Suche IMMER eine anklickbare HTML-Galerie bauen und im Browser oeffnen —
> NICHT die Sounds einzeln im Terminal vorspielen.** Grund: kurzes Anhoeren erlaubt keine Zuordnung
> ("welcher Sound war das?"). Der Benutzer will klicken, vergleichen, dann sagen "Sound 3 einbauen".

Ablauf:
1. **Standardmaessig 10 Vorschlaege** sammeln (mehrere Suchbegriffe kombinieren, falls eine Query zu
   wenig CC0-Treffer liefert — `page_size` hoch genug, Duplikate per ID raus).
2. Die `preview-hq-mp3`-Dateien aller 10 in den relativen Galerie-Ordner laden
   (`sound-search/<thema>/`), Dateinamen nummeriert (`01_...mp3` … `10_...mp3`).
3. Eine `auswahl.html` daneben schreiben — **eine Karte pro Sound** mit: grosser Nummer ("Sound N"),
   Titel, 1-Satz-Beschreibung, `<audio controls preload="none" src="NN_...mp3">`, Freesound-Link
   (`https://freesound.org/s/ID/`) und dem Hinweis 'sag: "Sound N einbauen"'.
   HTML **UTF-8 ohne BOM** schreiben (sonst kaputte Umlaute). Danach Dateiende pruefen (Mount-Falle).
4. Im Browser oeffnen (plattformrichtig): macOS `open`, Linux `xdg-open`, Windows `start`.
5. Der Benutzer waehlt per Nummer ("Sound 3 einbauen") → dann Schritt 4 (HQ laden + ins Projekt).

Optionales Zusatz-Vorspielen im Terminal (nur auf Wunsch "spiel Nummer X nochmal"), plattformrichtig:
```bash
# macOS: afplay datei.mp3   |   Linux: ffplay -nodisp -autoexit datei.mp3
```

## Schritt 4: Ausgewaehlten Sound ins Projekt uebernehmen

```bash
# HQ-Version laden (OAuth fuer manche Lizenzen noetig)
curl -sL "https://freesound.org/apiv2/sounds/ID/download/?token=$API_KEY" -o /tmp/sound_hq.wav
# Fuer Android nach OGG Vorbis konvertieren (falls ffmpeg verfuegbar) — Ziel RELATIV:
ffmpeg -i /tmp/sound_hq.wav -c:a libvorbis -q:a 5 <projektordner>/app/src/main/res/raw/<name>.ogg 2>/dev/null
# Oder WAV direkt kopieren:
cp /tmp/sound_hq.wav <projektordner>/app/src/main/res/raw/<name>.wav
```

## Schritt 5: Zapsplat-Vorschlaege (kein API)

Zapsplat hat keine oeffentliche API → Browse-Links generieren:
```
Zapsplat-Suche: https://www.zapsplat.com/sound-effect-category/CATEGORY/
```
| Kategorie | Zapsplat-URL |
|-----------|--------------|
| UI/Interface | https://www.zapsplat.com/sound-effect-category/button-and-interface/ |
| Games | https://www.zapsplat.com/sound-effect-category/game-sounds/ |
| Alerts | https://www.zapsplat.com/sound-effect-category/alarms-and-alerts/ |
| Transitions | https://www.zapsplat.com/sound-effect-category/whoosh-and-swoosh/ |
| Comedy | https://www.zapsplat.com/sound-effect-category/comedy-and-cartoon/ |

## Schritt 6: Lizenz-Check

**DEFAULT: Nur CC0-Sounds** (kommerziell nutzbar, keine Attribution, Play Store OK). Harte Regel.
- **CC0**: frei, keine Attribution — DEFAULT, immer diesen Filter.
- **CC-BY**: nur wenn der Benutzer ausdruecklich mehr Optionen will (Credits-Screen noetig).
- **CC-BY-NC**: NIEMALS verwenden — nicht mit kommerziellen Play-Store-Apps vereinbar.

## Proaktive Sound-Vorschlaege

Bei JEDER App mit UI-Interaktionen proaktiv vorschlagen: Button-Taps (kurzer Click 50-100ms),
Erfolgs-Sounds (aufsteigende Tonfolge), Fehler-Sounds (kurzer Buzz/absteigender Ton), Uebergaenge
(sanfter Whoosh), dezente Ambient-Loops. Danach: 'Soll ich passende Sounds auf Freesound suchen?'

## SONNISS Free Pack (Bonus)

Jaehrliche kostenlose Packs von `https://sonniss.com/gameaudiogdc/` — Studio-Qualitaet, royalty-free,
keine Attribution, kommerzielle Nutzung erlaubt, WAV. Gut fuer: Game-Effekte, cinematic, ambient.

---

## Sichern (Cowork-Git)

Falls ein Git-Repo verbunden ist und etwas Bleibendes ins Projekt uebernommen wurde:
```bash
bash ~/proggs/cowork-git.sh setup        # auf "Push-Zugang OK" warten
bash ~/proggs/cowork-git.sh push-files "#NNN - sound: <name> eingebaut" <projektordner>/app/src/main/res/raw/<name>.ogg
```
Nur die eigenen, eben geaenderten relativen Pfade nennen. **Kein Git-Repo verbunden → nur speichern
und den Ablage-Pfad nennen.** Die Temp-Galerie (`sound-search/<thema>/`) wird NICHT committet.

## Was NIEMALS passieren darf

- **Keine Neu-Registrierung** bei Freesound vorschlagen — der Key existiert (fehlt die Datei: Key erfragen).
- **CC-BY-NC-Sounds** vorschlagen — nicht Play-Store-kompatibel. DEFAULT bleibt CC0.
- Sounds nur einzeln im Terminal vorspielen statt die HTML-Galerie zu bauen (Auswahl waere nicht moeglich).
- macOS-`afplay` auf einer Nicht-macOS-Plattform aufrufen (plattformrichtig bleiben).
- Feste `~/proggs`-Pfade fuers Speichern verwenden (ausser `~/proggs/cowork-git.sh`) — relativ schreiben.
- Nacktes `git commit`/`git push` aus Cowork — immer `cowork-git.sh`.
- Dateiende der `auswahl.html` nach dem Schreiben nicht pruefen (Mount-Truncation-Gefahr).

## Referenzen

- Galerie + Vorschauen: `sound-search/<thema>/auswahl.html`
- Cowork-Regeln: `bugs/claude-tooling/cowork.md`, `bugs/claude-tooling/cowork-git-push.md` (im Arbeitsordner)
