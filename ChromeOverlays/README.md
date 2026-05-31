# Overlays — Chrome-Erweiterung (Voice + Tools)

Eine **einzige** Chrome-Erweiterung, die auf verschiedenen Webseiten unterschiedliche
Overlays (Button-Leisten) einblendet. Loest die einzelnen Tampermonkey-Skripte ab.

Aktuell als Prototyp enthalten:

| Seite | Buttons |
|-------|---------|
| **Google Uebersetzer** (`translate.google.com`) | 🎙️ Mic · 📋 Einfuegen · 📎 Kopieren · ❌ Leeren (4) |
| **ChatGPT** (`chatgpt.com`) | 🎙️ Mic · ⏎ Auto-Send · 📋 · 📎 · ❌ · G (Gemini-Korrektur) · ✨ Prompt (Frank) · 🪄 Prompt (allgemein) · 💾 Memory (9) |

Welche Buttons auf welcher Seite erscheinen, steht in **`src/registry.js`** — eine Liste pro Seite.

---

## Installation (entpackte Erweiterung)

1. Chrome oeffnen → Adresse `chrome://extensions` eingeben.
2. Oben rechts **Entwicklermodus** einschalten.
3. **"Entpackte Erweiterung laden"** klicken.
4. Diesen Ordner auswaehlen: `…/proggs/ChromeOverlays`.
5. Die Erweiterung erscheint in der Leiste. Auf das Icon klicken → **API-Keys eintragen**:
   - **Groq-Key** (kostenlos auf console.groq.com) — fuer die Spracheingabe (Mic).
   - **Gemini-Key** (kostenlos auf aistudio.google.com/apikey) — nur fuer ChatGPT (Prompt-Builder, Korrektur).
6. Die Ziel-Seite (ChatGPT / Translate) neu laden. Das Overlay erscheint unten rechts.

Nach jeder Aenderung am Code: in `chrome://extensions` bei der Erweiterung auf **Neu laden** (↻) klicken,
danach die Webseite neu laden.

---

## Eine neue Seite hinzufuegen

1. In **`src/registry.js`** ein neues Profil ergaenzen:
   ```js
   {
     id: "meineSeite",
     label: "Meine Seite",
     match: (host) => host === "beispiel.de",
     uiPos: { right: 16, bottom: 96 },
     buttons: ["mic", "paste", "copy", "clear"],   // beliebige Auswahl
   }
   ```
2. In **`manifest.json`** unter `content_scripts[0].matches` die URL ergaenzen
   (z.B. `"https://beispiel.de/*"`).
3. Erweiterung neu laden. Fertig — kein neues Plugin noetig.

Verfuegbare Button-Schluessel (definiert in `src/ui.js` → `CATALOG`):
`mic`, `enter`, `paste`, `copy`, `clear`, `gemini`, `promptFrank`, `promptGeneral`, `memory`.

---

## Architektur

```
manifest.json        Manifest V3: welche Seiten, welche Skripte, Berechtigungen
background.js        Service Worker: Groq- + Gemini-fetch (umgeht CORS, ersetzt GM_xmlhttpRequest)
options.html/.js     Einstellungsseite fuer API-Keys (ersetzt GM_registerMenuCommand)
src/
  storage.js         chrome.storage statt GM_getValue/GM_setValue (synchroner Cache)
  toast.js           Hinweis-Einblendungen
  editable.js        Eingabefeld finden + React-sicher Text setzen (setViaPaste)
  stt.js             Mic-Aufnahme + Whisper-Spracheingabe
  gemini.js          Grammatik-Korrektur + Domaenen-Finder
  actions.js         Logik hinter jedem Button
  ui.js              CATALOG (Button-Definitionen) + Renderer + Watchdog
  registry.js        ←── zentrale Liste: welche Buttons pro Seite
  content.js         Einstieg: Seite erkennen → Profil waehlen → Overlay bauen
  overlay.css        Mic-Button-Animationen
```

Alle `src/*.js` teilen sich ueber das Objekt `window.__chromeOverlays__` (Kurz: `OV`)
einen gemeinsamen Namespace — kein Bundler noetig.

---

## Unterschiede zu Tampermonkey (Migration)

| Tampermonkey | Hier (Manifest V3) |
|--------------|--------------------|
| `GM_getValue` / `GM_setValue` | `chrome.storage.local` (`src/storage.js`) |
| `GM_xmlhttpRequest` | fetch im Service Worker (`background.js`) |
| `GM_registerMenuCommand` | Einstellungsseite (`options.html`) |
| `@match` Header | `manifest.json` → `content_scripts.matches` + Profil-`match` in der Registry |

Die alten Tampermonkey-Skripte unter `../Tampermonkey/` bleiben unveraendert und funktionieren
weiter, bis du den Wechsel final vollziehst.

---

## Status

Prototyp mit zwei Seiten (ChatGPT + Translate) zum Testen des Umschalt-Prinzips.
Weitere Seiten (claude, gemini, grok, …) koennen nach demselben Muster ergaenzt werden.
