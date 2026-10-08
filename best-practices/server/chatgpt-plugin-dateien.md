# Dateien und Bilder aus einem eigenen MCP-Server in ChatGPT anzeigen

> Stand: 08.10.2026, 22:46 Uhr. Versions-Anker: ChatGPT-Plugins (früher „Apps SDK“, developers.openai.com/plugins),
> MCP-Apps-Spezifikation 2026-01-26, MCP-Spezifikation 2025-06-18 / 2025-11-25, Live-Sprachmodus GPT-Live-1 (ab 23.09.2026).
> Quelle der Recherche: Sonnet-Schwarm, 4 Researcher. Umgesetzt in `Jarvis` (Werkzeug `ablage_zeigen`,
> `Jarvis/app/src/main/java/de/frank/jarvis/faehigkeit/AblageKarte.kt`), dort am 08.10.2026 noch NICHT am Gerät bestätigt.
> Gegenseite (Fallen): `bugs/server/chatgpt-plugin-dateien.md`.

## ⚡ Kurzcheck (Stufe A)

| # | Regel | Quelle |
|---|-------|--------|
| 1 | Bilder zeigt ChatGPT nur über eine Karte (MCP-Apps-Widget) verlässlich an, nicht über MCP-Bildblöcke und nicht über Bildlinks | offiziell |
| 2 | Werkzeug nennt die Karte in `_meta.ui.resourceUri` und zusätzlich im Alias `_meta["openai/outputTemplate"]` | offiziell |
| 3 | Karte ist eine `ui://`-Ressource mit mimeType `text/html;profile=mcp-app`; der Server braucht die Fähigkeit `resources` samt `resources/list` und `resources/read` | offiziell |
| 4 | Ergebnis immer auch ohne Karte brauchbar: Text in `content`, Daten in `structuredContent` | offiziell |
| 5 | Große Daten für die Karte gehören in `_meta` des Ergebnisses: Das sieht nur die Karte, nicht das Modell | offiziell |
| 6 | Bilder klein halten (verkleinertes JPEG, einige hundert kB): große Ergebnisse verschwinden ohne Fehlermeldung | extern |
| 7 | Adresse der Karte wie einen Cache-Schlüssel behandeln: bei jeder Änderung am HTML neue Adresse (`…-v2.html`) | offiziell |
| 8 | Nach Änderungen an Werkzeugen oder Karte: in ChatGPT das Plugin aktualisieren („Refresh“) und ein neues Gespräch beginnen | offiziell |
| 9 | Auf dem echten Handy testen, nicht in der Geräte-Simulation des Browsers | extern |

## 1. Was ChatGPT aus einem Werkzeug-Ergebnis verwendet

- Ein Ergebnis besteht für ChatGPT aus `content`, `structuredContent` und `_meta`. `content` und `structuredContent`
  sieht das Modell (und die Karte), `_meta` sieht nur die Karte. Nur die ersten beiden landen im Gesprächsverlauf.
  offiziell: https://developers.openai.com/plugins/reference
- Ein Gegenstück zu `openai/fileParams` (Datei-EINGABEN) für Datei-AUSGABEN ist nicht dokumentiert. „Tool result file
  references“ werden beiläufig erwähnt, ein Format dazu gibt es nicht. offiziell: https://developers.openai.com/plugins/reference
- Deep Research nutzt nur die Werkzeuge `search` und `fetch`. offiziell: https://developers.openai.com/api/docs/guides/deep-research

## 2. Der dokumentierte Weg: eine Karte (MCP Apps)

- Werkzeug: `_meta: {"ui": {"resourceUri": "ui://…"}, "openai/outputTemplate": "ui://…"}`. Nur das Werkzeug, das etwas
  anzeigt, trägt den Verweis. offiziell: https://developers.openai.com/plugins/build/chatgpt-ui
- Ressource: `contents[0]` mit `uri`, `mimeType: "text/html;profile=mcp-app"`, `text` (das HTML) und
  `_meta.ui.csp` mit `resourceDomains` (Bilder, Schriften, Skripte, Stile) und `connectDomains` (fetch).
  Das offizielle Beispielrepo nutzt noch `text/html+skybridge`; ob ChatGPT das weiter annimmt, ist unklar.
  offiziell: https://developers.openai.com/plugins/build/chatgpt-ui · extern: https://github.com/openai/openai-apps-sdk-examples
- Daten in der Karte: `window.openai.toolOutput` (= `structuredContent`) und `window.openai.toolResponseMetadata`
  (= `_meta`), Ereignis `openai:set_globals`; nach dem Standard die Nachricht `ui/notifications/tool-result` per
  `postMessage`, davor `ui/initialize`. Beide Wege bedienen. offiziell: https://developers.openai.com/plugins/build/chatgpt-ui
- Bild ohne öffentliche Adresse: als `data:`-Adresse in `_meta` mitgeben, Freigabelisten leer lassen.
  extern (Bericht, ohne Antwort von OpenAI): https://github.com/openai/mcp-extensions/issues/18
- Bild von der eigenen https-Domain: Domain in `resourceDomains` eintragen. Ob das auch für PDF, Video und Audio gilt,
  steht nirgends. offiziell: https://developers.openai.com/plugins/reference
- `_meta.ui.domain` ist nur für die Einreichung ins Verzeichnis Pflicht; zum eigenen Gebrauch genügt die Vorgabe.
- ChatGPT steht in der Unterstützungs-Tabelle für MCP Apps. offiziell: https://modelcontextprotocol.io/extensions/client-matrix

## 3. Links statt Karte

- Der OpenAI-Support empfiehlt für Dateien einen klickbaren Link statt eines eingebetteten Bildes.
  extern: https://community.openai.com/t/did-something-change-images-from-gpt-actions-don-t-render-anymore/1369828
- ChatGPT prüft Adressen vor dem Anzeigen (`url_safe`); unbekannte Adressen lädt es nicht von selbst, sondern zeigt
  einen Sicherheitsdialog. Die Regeln sind nicht öffentlich.
  extern: https://embracethered.com/blog/posts/2023/openai-data-exfiltration-first-mitigations-implemented/ ·
  offiziell (nur über Zweitquelle gelesen): https://openai.com/index/ai-agent-link-safety
- Für Downloads aus der Karte gibt es nur `window.openai.getFileDownloadUrl` für Dateien aus ChatGPTs eigenem Speicher;
  ob das für vom Server erzeugte Dateien geht, ist ungeklärt.

## 4. Sprachmodus (Live)

- Seit 23.09.2026 kann der Live-Modus Plugins des Kontos nutzen; Antworten erscheinen als Text im Gespräch, und er kann
  „visuelle Ergebnisse über unterstützte Widgets“ zeigen. Genannt werden nur Wetter, Aktien und Sport.
  offiziell (über Spiegel gelesen): https://help.openai.com/en/articles/20001274-chatgpt-voice
- Ob Karten eines eigenen MCP-Servers im Live-Modus erscheinen, ist nirgends belegt: am Gerät testen.
- Freigaben für Aktionen gehen nur am Bildschirm, nicht per Sprache.

## 5. Offen

- Größengrenzen für Ergebnisse und für `_meta` sind nicht dokumentiert.
- Welche Abos Karten eigener Plugins in der mobilen App zeigen, war nicht zu klären (Hilfeseite nicht abrufbar).
