---
name: jarvis-relay-und-chatgpt-plugin
description: "Jarvis läuft über den Hostinger-Relay; ChatGPT-Plugin wird über „Benutzerdefinierten MCP-Server erstellen\" angelegt; keine Fremdanbieter-Konten"
metadata:
  node_type: memory
  type: project
  originSessionId: e79775e1-35fa-4ac6-972c-82b8f09a1290
  modified: 2026-10-08T14:20:59.301Z
---

Jarvis (`~/proggs/Jarvis`, Paket `de.frank.jarvis`) ist seit 08.10.2026 als ChatGPT-Plugin in Betrieb. Der MCP-Server läuft auf dem Handy, die öffentliche Gegenstelle ist der Relay unter `/opt/jarvis` auf dem Hostinger-Server (`https://srv1774016.hstgr.cloud`, Port 443 öffentlich, IPv4 und IPv6). Zugangsdaten: `~/SK/Jarvis/`. Erste Anbindung: Geniale Aufgaben über `JarvisBruecke`.

**Why:** Frank wollte keinen fremden Tunnel-Anbieter mit eigener Anmeldung (ngrok hat er abgelehnt, er kannte es nicht und es war nicht abgesprochen). Der eigene Server läuft ohnehin rund um die Uhr.

**How to apply:**
- Neue Dienste, die ein Konto bei einem Fremdanbieter brauchen, vorher ansprechen und begründen, nicht als Schritt in die Abschlussmeldung schreiben.
- Fehlersuche bei Plugin-Aufrufen: `docker logs jarvis-relay` auf dem Server zeigt je Anfrage eine Zeile (ohne Geheimnis). ChatGPT meldet sich als `openai-mcp/1.0.0` und ruft zuerst `server/discover` auf (Jarvis antwortet darauf mit „unbekannte Methode", danach folgt `initialize`).
- In ChatGPT (Web und Desktop-App): Plugins → Hinzufügen → „Benutzerdefinierten MCP-Server erstellen", Verbindung „Server-URL", Authentifizierung „Keine Authentifizierung". „Plugin erstellen" (Plugin Creator) ist der falsche Einstieg.
- Stand 08.10.2026: Aufrufe funktionieren, in der Handy-App erscheint Jarvis aber nicht unter „Installiert" („Plugin konnte nicht aktualisiert werden"). Sprachmodus noch nicht bestätigt. Geplanter Ausweg: GPT Live direkt in Jarvis.
- Von diesem PC aus immer mit IPv4 testen (`curl -4`), siehe [[http-timeouts-fallback-wartezeit]]. Handy nicht blind fernsteuern: [[handy-nicht-blind-fernsteuern]].
- Stand 08.10.2026 abends (Jarvis 1.4.0): angebunden sind Geniale Aufgaben, Entropie Reductor (Biomarker), Kalender mit Dienstplan, Geniale Ideen; dazu Tagesauswertung (4:25, 12:00, 16:25), Tagesdatenbank, Ablage, Agenten mit Tavily-Suche, Gmail per App-Passwort (von Frank noch einzutragen). Jede neue App: ContentProvider `JarvisBruecke` in der App mit Erlaubnis `de.frank.jarvis.permission.BRUECKE`, in Jarvis eine `Faehigkeit` plus `<queries>`-Eintrag. Nach neuen Werkzeugen muss Frank das Plugin in ChatGPT aktualisieren.
- Testen ohne Handy-Bedienung: Aufrufe von außen über den Relay, Skripte dafür lagen im Sitzungs-Scratchpad (nicht dauerhaft); Geheimnis aus `/data/zustand.json` im Container `jarvis-relay`.
- Stand 08.10.2026, 18 Uhr (Jarvis 1.6.0): zusätzlich Tagebuch (Drive-Ordner über Nur-Lese-rclone-Zugang auf dem Relay, `~/SK/rclone/gdrive-lesen.conf`), Wissens-Datenbank (Ordner `Datenbank` im öffentlichen Repo, ohne Schlüssel), Mikrofon mit Groq, absatzweises Vorlesen (Modul aus GenialeIdeen kopiert), Ablage-Reiter, Fingerabdruck-Sperre. Die Oberfläche dieser Teile hat Frank noch nicht bestätigt.
- Stand 08.10.2026, 18:30 Uhr (Jarvis 1.8.0, 31 Werkzeuge): zusätzlich Genialer Wecker (Brücke in `GenialerWecker`, nicht GenialerWeckerAndroid), Wetter (Open-Meteo), Kalender schreiben (`WRITE_CALENDAR`), Mitdenken (`faehigkeit/Mitdenken.kt`), Knopf „Berechtigung erneuern" für den Drive-Lesezugang. Verwandte Werkzeuge sind gebündelt (Helfer `als`/`mit`); bei neuen Fähigkeiten zuerst prüfen, ob sie in ein vorhandenes Werkzeug passen. Offene Annahme: Tagdienst belegt 4:00 bis etwa 18:30 Uhr, Frank hat die Rückkehrzeit nicht genannt.
