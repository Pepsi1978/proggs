# ChatGPT-Plugin (eigener MCP-Server): Dateien, Bilder und Karten – Bug-Almanach

> Stand: 08.10.2026, 22:46 Uhr. Versions-Anker: ChatGPT-Plugins (developers.openai.com/plugins), MCP Apps 2026-01-26,
> Live-Sprachmodus GPT-Live-1 (ab 23.09.2026). Gegenseite (Vorgehen): `best-practices/server/chatgpt-plugin-dateien.md`.
> Alle Einträge stammen aus Forums- und GitHub-Berichten; ein Fix ist bei keinem bestätigt.

## ⚡ Kurzcheck (Stufe A)

| # | Signal | Sofort-Regel | § |
|---|--------|--------------|---|
| 1 | Werkzeug liefert ein Bild als MCP-Bildblock, ChatGPT zeigt nichts oder `{}` | Karte (MCP-Apps-Widget) verwenden | 1 |
| 2 | Karte erscheint bei kleinem Bild, bei großem nur Text, ohne Fehler | Bild verkleinern | 2 |
| 3 | Bildlink aus dem Werkzeug wird nicht als Bild angezeigt | Klickbaren Link oder Karte verwenden | 3 |
| 4 | Karte lädt in der mobilen App nicht, im Web schon | Am echten Gerät prüfen, Text-Ergebnis als Rückfall | 4 |
| 5 | `app.downloadFile()` meldet „Method not found“ | In ChatGPT nicht vorhanden | 5 |
| 6 | Hochgeladene Datei kommt am Handy als Text statt als Objekt an | Wert defensiv auswerten | 6 |
| 7 | „Response output was truncated … tool response budget“ | Ergebnis kürzen, Großes in `_meta` | 7 |

## 1. MCP-Bildblöcke und resource_link werden nicht angezeigt

- **Symptom:** `content: [{type:"image", data, mimeType}]` ergibt in ChatGPT „returned no result“, ein leeres `{}` oder nur Text.
  Im MCP Inspector und in Claude funktioniert dasselbe Werkzeug. `resource_link` ebenso.
- **Ursache:** Kein zugesicherter Weg; der OpenAI-Support nennt es am 22.04.2026 ausdrücklich nicht garantiert.
- **Betroffen:** ChatGPT-Connectors und -Plugins, 2025 bis mindestens 09/2026. Fall am 09.08.2026 ohne Lösung geschlossen.
- **Fix (funktionserhaltend):** Bild über eine Karte zeigen (Best Practices §2), Text und `structuredContent` zusätzlich liefern.
- **Quellen:** https://community.openai.com/t/image-responses-from-mcp-tools-do-not-work/1360430 ·
  https://community.openai.com/t/chatgpt-connector-returns-empty-for-mcp-tool-results-with-type-image-while-same-tool-works-in-mcp-inspector/1375446

## 2. Großes Bild: Karte bleibt stumm aus

- **Symptom:** Ein PNG mit etwa 3 kB erscheint in der Karte, eines mit etwa 3 MiB nicht: nur Text, keine Karte, kein Fehler.
- **Ursache:** Unbekannt; eine Größengrenze ist nicht dokumentiert.
- **Betroffen:** gemeldet am 22.09.2026, keine Antwort von OpenAI.
- **Fix:** Bild verkleinern und als JPEG liefern (in Jarvis: längste Kante 1600 Pixel, Zielgröße unter etwa 450 kB).
- **Quelle:** https://community.openai.com/t/mcp-tool-returns-an-image-successfully-but-chatgpt-shows-only-text-no-image-or-widget/1399928

## 3. Bildlinks werden nicht mehr als Bild angezeigt

- **Symptom:** `![titel](https://…)` aus einem Werkzeug-Ergebnis erscheint als Text oder gar nicht; `url_safe` meldet `safe:false`.
- **Ursache:** ChatGPT prüft Adressen vor dem Anzeigen als Schutz gegen Daten-Abfluss; unbekannte oder nur einmal gültige
  Adressen fallen durch. Regeln nicht öffentlich.
- **Betroffen:** Custom-GPT-Actions seit 12/2025; für MCP ein unbeantworteter Bericht vom 12.05.2026.
- **Fix:** Klickbaren Link ausgeben (Empfehlung des Supports) oder das Bild über eine Karte zeigen.
- **Quellen:** https://community.openai.com/t/did-something-change-images-from-gpt-actions-don-t-render-anymore/1369828 ·
  https://community.openai.com/t/output-markdown-with-image/1380737

## 4. Karten laden in der mobilen App zeitweise nicht

- **Symptom:** Karte erscheint im Web, in der App auf iOS und Android nicht oder bleibt im Ladezustand. Mehrere Karten in
  einer Antwort erscheinen in der mobilen App teils gar nicht.
- **Betroffen:** gemeldet 08/2026, zwischenzeitlich behoben, danach erneut gemeldet; keine Stellungnahme.
- **Fix:** Ergebnis so bauen, dass es ohne Karte verständlich ist; am echten Gerät prüfen; je Antwort nur eine Karte.
- **Quellen:** https://community.openai.com/t/widgets-are-not-loading-in-the-chatgpt-mobile-apps-for-launched-apps-plugins/1388831 ·
  https://community.openai.com/t/apps-sdk-multiple-widgets-in-a-single-chat-turn-fail-to-render-inside-the-native-chatgpt-mobile-app/1373881

## 5. `app.downloadFile()` fehlt in ChatGPT

- **Symptom:** Der Standard-Aufruf der MCP-Apps-Brücke meldet „Method not found“ (-32601).
- **Ursache:** Lücke auf der Seite von ChatGPT, vom Support am 09.04.2026 bestätigt.
- **Fix:** `window.openai.getFileDownloadUrl` und einen gewöhnlichen Link verwenden (nur für Dateien aus ChatGPTs Speicher belegt).
- **Quelle:** https://community.openai.com/t/app-downloadfile-returns-method-not-found-in-chatgpt-host/1378708/2

## 6. Datei-Eingaben vom Handy kommen als Text an

- **Symptom:** Bei `openai/fileParams` liefert die mobile App statt des Objekts mit `download_url` einen Text wie
  `chat_upload://image_0`.
- **Betroffen:** offen seit 20.01.2026. Betrifft in Jarvis `ablage_datei_speichern` (`chatgpt_dateien`).
- **Fix:** Wert prüfen; ist es kein Objekt mit `download_url`, ehrlich melden, dass die Datei nicht übergeben wurde.
- **Quelle:** https://github.com/openai/openai-apps-sdk-examples/issues/185

## 7. Ergebnis wird abgeschnitten

- **Symptom:** „Response output was truncated … tool response budget“ bei Ergebnissen, die früher durchgingen.
- **Betroffen:** gemeldet 06/2026; eine Grenze ist nicht dokumentiert.
- **Fix:** `content` und `structuredContent` knapp halten; Großes, das nur die Karte braucht, in `_meta`.
- **Quelle:** https://community.openai.com/t/tool-response-truncation-on-mcp-connector-responses-that-previously-worked/1383071

## 8. Weitere

- Ressourcen aus privaten Netzen (10.x, 192.168.x) werden in der Karte trotz Freigabe blockiert (ChatGPT Desktop).
  https://github.com/openai/mcp-extensions/issues/18
- Mit `uploadFile` hochgeladene Bilder sind für das Modell nicht lesbar; offen seit 29.12.2025.
  https://github.com/openai/openai-apps-sdk-examples/issues/170
