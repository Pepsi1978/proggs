# ChatGPT-Plugin (eigener MCP-Server): Dateien, Bilder und Karten Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`).
> Volltext = Pflicht bei JEDEM Fehler in diesem Bereich (Stufe B). Stand: 08.10.2026, 22:46 Uhr.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Werkzeug liefert MCP-Bildblock oder `resource_link`, ChatGPT zeigt nichts, `{}` oder nur Text | Bild über eine Karte (MCP-Apps-Widget) zeigen | §1 |
| 2 | Karte erscheint bei kleinem Bild, bei großem nur Text, ohne Fehlermeldung | Bild verkleinern (JPEG, einige hundert kB) | §2 |
| 3 | Bildlink `![…](https://…)` aus dem Werkzeug wird nicht als Bild angezeigt | Klickbaren Link oder Karte nehmen | §3 |
| 4 | Karte lädt in der mobilen App nicht, im Web schon; mehrere Karten je Antwort fehlen | Am echten Gerät prüfen, Text als Rückfall, eine Karte je Antwort | §4 |
| 5 | `app.downloadFile()` meldet „Method not found“ (-32601) | In ChatGPT nicht vorhanden; `getFileDownloadUrl` + Link | §5 |
| 6 | Vom Handy hochgeladene Datei kommt als Text `chat_upload://…` statt als Objekt an | Wert prüfen, ehrlich melden | §6 |
| 7 | „Response output was truncated … tool response budget“ | Ergebnis kürzen, Großes in `_meta` | §7 |
