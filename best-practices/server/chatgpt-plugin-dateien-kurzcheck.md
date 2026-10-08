# Dateien und Bilder aus einem eigenen MCP-Server in ChatGPT anzeigen Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

> Stand: 08.10.2026, 22:46 Uhr. Volltext: `chatgpt-plugin-dateien.md`.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Regel | Detail |
|---|-------|--------|
| 1 | **Bilder nur über eine Karte** | ChatGPT zeigt Bilder aus Werkzeug-Ergebnissen verlässlich nur über ein MCP-Apps-Widget, nicht über MCP-Bildblöcke oder Bildlinks. Volltext §2 |
| 2 | **Verweis am Werkzeug doppelt** | `_meta.ui.resourceUri` und der Alias `_meta["openai/outputTemplate"]`. Volltext §2 |
| 3 | **Ressource richtig ausweisen** | `ui://…`, mimeType `text/html;profile=mcp-app`, Server-Fähigkeit `resources` mit `resources/list` und `resources/read`. Volltext §2 |
| 4 | **Ohne Karte brauchbar bleiben** | Text in `content`, Daten in `structuredContent`. Volltext §1 |
| 5 | **Große Daten in `_meta`** | Sieht nur die Karte, nicht das Modell (kein Token-Verbrauch). Volltext §1 |
| 6 | **Bilder klein halten** | Verkleinertes JPEG, einige hundert kB; große Ergebnisse verschwinden still. Volltext §2 |
| 7 | **Adresse der Karte versionieren** | Neues HTML = neue Adresse (`…-v2.html`). Volltext §2 |
| 8 | **Nach Änderungen: Refresh + neues Gespräch** | Sonst arbeitet ChatGPT mit dem alten Stand. Volltext §2 |
| 9 | **Dateien sonst als Link** | Der Support empfiehlt klickbare Links; unbekannte Adressen lösen einen Sicherheitsdialog aus. Volltext §3 |
| 10 | **Sprachmodus am Gerät testen** | Karten eigener Server im Live-Modus sind nicht belegt. Volltext §4 |
