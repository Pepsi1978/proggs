# Prompt Board (Chrome-Erweiterung)

Ein Prompt-Board als **Seitenpanel** (Side Panel) für Google Chrome. Ein Klick auf
das Symbol oben in der Erweiterungsleiste öffnet das Panel rechts. Ein zweiter Klick
schließt es wieder. Im Panel klickst du auf einen Prompt — er wird sofort in das
**zuletzt benutzte Textfeld** der aktiven Webseite eingefügt.

## Was es kann

- **Seitenpanel statt Overlay** — nativ rechts angedockt, verdeckt die Seite nicht.
- **Klick-Toggle** — Symbol anklicken öffnet/schließt das Panel (übernimmt Chrome selbst).
- **Zuverlässiges Einfügen** — funktioniert in normalen Feldern (`<input>`, `<textarea>`)
  **und** in Rich-Editoren wie ChatGPT oder Gmail (`contenteditable`), auch in iframes.
- **Fokus-sicher** — die Erweiterung merkt sich das zuletzt benutzte Textfeld, bevor
  der Klick ins Panel den Fokus verschiebt.
- **Eigene Prompts** — über „Bearbeiten" Prompts hinzufügen oder löschen. Gespeichert
  in `chrome.storage.sync`, also auf deinen angemeldeten Chrome-Geräten synchronisiert.

## Installation (Entwickler-Modus, kein Store nötig)

1. Icons erzeugen (einmalig):
   ```
   python .tools/generate_icons.py
   ```
   Das legt `icons/icon16.png`, `icon48.png`, `icon128.png` an.
   (Das Skript liegt bewusst in `.tools/` — Chrome ignoriert `.`-Ordner, und so
   kann das Python-`__pycache__` die Erweiterung nie blockieren.)
2. In Chrome `chrome://extensions` öffnen.
3. Oben rechts **Entwicklermodus** einschalten.
4. **Entpackte Erweiterung laden** klicken und den Ordner `PromptBoardChrome` auswählen.
5. Das Prompt-Board-Symbol erscheint in der Werkzeugleiste (ggf. über das Puzzle-Symbol
   anpinnen). Klick darauf öffnet das Panel.

> Hinweis: Bereits geöffnete Tabs erst **neu laden**, damit das Einfüge-Script aktiv wird.
> Auf internen Seiten (`chrome://…`, Chrome Web Store) funktioniert das Einfügen nicht —
> das ist eine Chrome-Sicherheitsregel, kein Fehler der Erweiterung.

## Aufbau

| Datei | Aufgabe |
|-------|---------|
| `manifest.json` | Manifest V3, Berechtigungen, Side-Panel- und Content-Script-Registrierung |
| `background.js` | Service Worker — aktiviert das Klick-Toggle des Symbols |
| `sidepanel.html/.css/.js` | Oberfläche des Panels, Prompt-Verwaltung |
| `content.js` | Merkt sich das letzte Textfeld und fügt den Prompt ein |
| `.tools/generate_icons.py` | Erzeugt die Toolbar-Icons (nur Standardbibliothek, außerhalb des Chrome-Scans) |
