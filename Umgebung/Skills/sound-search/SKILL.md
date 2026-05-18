---
name: sound-search
description: >
  Search and preview sound effects from Freesound.org and Zapsplat. Use when: the user
  needs sounds for an app, asks "find me a sound for...", "search for a click sound",
  "what sounds could I use for...", or when building Android/iOS apps that need audio.
  Also triggers on: "suche einen Sound", "finde einen Klang", "Sound-Effekt fuer".
---

# Sound Search Skill — Find & Preview Audio for Apps

## Prerequisites

Freesound API key must be set. Check with:
```bash
cat ~/.config/freesound/api_key 2>/dev/null
```

If missing, tell the user:
> Du brauchst einen kostenlosen Freesound API-Key. Einmal registrieren:
> 1. Gehe zu https://freesound.org/apiv2/apply/
> 2. Erstelle ein Konto (oder logge dich ein)
> 3. Erstelle eine "Application" (Name egal, z.B. "Claude Audio")
> 4. Kopiere den "Client secret / API key"
> 5. Sag mir den Key und ich speichere ihn sicher.

Save the key:
```bash
mkdir -p ~/.config/freesound
echo "THE_KEY" > ~/.config/freesound/api_key
chmod 600 ~/.config/freesound/api_key
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

## Step 3: Present Results

For each result, show:
```
🔊 [Name] — [Duration]s — ⭐ [Rating] — Lizenz: [License]
   Tags: [top 5 tags]
   Preview: [preview URL]
   Freesound: https://freesound.org/people/[user]/sounds/[id]/
```

## Step 4: Preview Sound

Download and play the preview:
```bash
# Download low-quality preview (always available, no special auth needed)
curl -sL "[preview-hq-ogg URL from API]" -o /tmp/sound_preview.ogg
afplay /tmp/sound_preview.ogg
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
