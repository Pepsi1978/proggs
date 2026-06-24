# Session Handoff — 2026-06-24 17:51

## Ziel (1-3 Saetze)
Franks "zweites Gehirn" (second-brain-server, Cortex-Cockpit) um 8 eingesprochene Features erweitern
(OpenCode-Go-Provider/minimax, System-Prompt-Feld groesser, Eintrags-Editor mit Vektor-Ersatz,
Mikrofon-STT, Logbuch-Fix, Gemini-Modell-Liste, G-Verbesserungs-Button + Auto-Grow) PLUS zwei
Folgeaufgaben aus Frank-Feedback: Logbuch-Gespraeche nicht in Uebersicht zaehlen + HTTPS fuers Mikrofon.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)
- **Welche Aufgabe lief gerade:** Mikrofon-Button im Cockpit zum Laufen bringen. Root-Cause war NICHT
  der Groq-Key, sondern: Chrome erlaubt getUserMedia/Mikrofon nicht ueber http://10.8.0.1 (LAN-IP
  ueber HTTP = kein "secure context"). Loesung: HTTPS via Caddy (deployt) + Root-CA in Windows-Trust.
- **Wo genau unterbrochen — der allerletzte Schritt:** Ich habe gerade die Caddy-Root-CA per
  `certutil -addstore -user -f Root "C:\Users\barwa\Downloads\cortex-root-ca.crt"` in Franks
  Windows-Zertifikatsspeicher importiert (ERFOLG bestaetigt). Frank wurde aufgefordert, **Chrome
  komplett neu zu starten**, dann `https://10.8.0.1` zu oeffnen (sollte kein "nicht sicher" mehr
  zeigen) und den Mikrofon-Button zu testen.
- **Schon erledigter Teil DIESES Schritts:** HTTPS laeuft + verifiziert (HTTP 200 ueber
  https://10.8.0.1 von Windows). Root-CA importiert. Caddyfile mit default_sni committet (#47155).
- **Noch offener Teil DIESES Schritts:** Franks Bestaetigung abwarten: (a) ist "nicht sicher" nach
  Chrome-Neustart weg? (b) nimmt der Mikrofon-Button jetzt auf (getUserMedia -> /api/transcribe ->
  Groq whisper-large-v3-turbo -> Text ins Feld)?
- **So geht es EXAKT weiter (allererste Aktion der neuen Session):** Auf Franks Rueckmeldung reagieren.
  Wenn Mikro geht -> fertig, Abschluss-Boxen fuer alle 10 Aufgaben. Wenn "nicht sicher" bleibt: pruefen
  ob Chrome einen eigenen Cert-Store nutzt / Chrome wirklich neu gestartet wurde. Wenn getUserMedia
  trotz Trust noch blockt: secure context per `window.isSecureContext` in DevTools pruefen.
- **Was dafuer alles vorhanden sein muss:** Server 10.8.0.1 (WireGuard MUSS auf sein, sonst nichts
  erreichbar). SSH: `ssh -i ~/.ssh/id_ed25519 root@10.8.0.1` (User=root, NICHT frank). Stack liegt in
  /opt/second-brain (KEIN git-Repo dort -> Deploy per scp + `docker compose up -d --build <service>`,
  CRLF->LF per `sed -i "s/\r$//"`). Dashboard-API ohne Auth ueber WireGuard testbar:
  `curl -sk https://10.8.0.1/api/...`. brain-api/agent brauchen Bearer SB_API_KEY (aus
  /opt/second-brain/.env). Keys GROQ_API_KEY + OPENCODE_API_KEY sind in der Server-.env eingetragen.
- **Uncommitteter Arbeitsstand (halbfertige Edits):** KEINER. Alles committet (#47149-#47155),
  gepusht, auf dem VPS deployt. Repo = Server.
- **Danach:** mit "Naechste Schritte" weiter (offene Frank-Entscheidung Groq vs Web Speech).

## Aktueller Status
- Erledigt (alle 10 Aufgaben, committet+deployt+live verifiziert):
  - #47149 agent 0.4.0: OpenCode Zen Go als 2. Provider (minimax/minimax-m3 ueber Anthropic
    /zen/go/v1/messages, x-api-key, curl-UA), llm_generate-Weiche, Modell-Liste aufgeraeumt
    (3.1-pro/3.1-flash raus), /improve-Endpoint. LIVE getestet: minimax + improve funktionieren.
  - #47150 brain-api 1.2.0 (PUT /entry: alten Vektor per doc_id loeschen + neu 1:1, doc_id in Listen)
    + dashboard 0.4.0 (PUT /api/entry, /api/transcribe Groq, /api/improve, /api/logbook liest .txt
    von Samba/Z mit Gehirn-Fallback). compose: GROQ+OPENCODE env, Logbuch-Mount ro.
  - #47151 Frontend (static/index.html): Editor+Bestaetigungsdialog, Mikro-Button, gruener G-Button,
    Logbuch-Anzeige, Auto-Grow bis 420px Buttons fix oben, System-Prompt-Feld 690px, Modell-Dropdown.
  - #47152 dashboard 0.4.1: gespraeche zaehlt nicht in Uebersicht (Vektoren bleiben). LIVE: total=178.
  - #47153/#47155 Caddy HTTPS-Proxy an 10.8.0.1:443 (nur WireGuard), tls internal, default_sni-Fix.
  - #47154 Almanach bugs/server/reverse-proxy-tls.md §1.10 (IP-HTTPS ohne SNI -> alert 80).
  - LIVE verifiziert: Aufgabe 1,2,4,6,8,9,10. Aufgabe 3,7 = reine UI (nach Deploy da).
- In Arbeit: Frank testet Mikrofon (siehe Wiedereinstiegspunkt).
- Blockiert: nichts.

## Relevante Dateien (alle in ~/proggs/second-brain-server/)
- `agent/app.py` (0.4.0) — Provider-Weiche llm_generate, opencode_generate, /improve
- `brain-api/app.py` (1.2.0) — PUT /entry, doc_id in Antworten
- `dashboard/app.py` (0.4.1) — /api/entry, /api/transcribe, /api/improve, /api/logbook, overview-Filter
- `dashboard/static/index.html` — gesamtes Frontend (Editor, Mikro, G-Button, Logbuch, Auto-Grow)
- `compose.yaml` — caddy-Service, GROQ/OPENCODE env, Logbuch-Mount
- `Caddyfile` — tls internal + default_sni 10.8.0.1

## Getroffene Entscheidungen
- OpenCodeGo = OpenCode Zen Go (NICHT OpenRouter); minimax laeuft ueber Anthropic /messages-Schema.
- Groq behalten (besser, Key sicher im Server) statt Web Speech — aber Frank wollte "kein Key";
  Umstieg auf browser-eigene Web Speech API moeglich falls Frank das nach dem Mikro-Test wuenscht.
- HTTPS-Proxy bewusst an 10.8.0.1 gebunden (kein 0.0.0.0) -> Server bleibt nur ueber WireGuard offen.
- Eintrags-Editor per doc_id (robust auch fuer titellose Eintraege), Titel/Kategorie/created_at bleiben.

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- HTTPS ueber IP `https://10.8.0.1 { tls internal }` OHNE `default_sni` -> TLS internal error / SSL
  alert 80, HTTP 000. Client sendet kein SNI fuer IPs. FIX: global `default_sni 10.8.0.1`. (Almanach §1.10)
- Geaenderte bind-gemountete Caddyfile mit `docker compose up -d caddy` -> laedt NICHT neu (kein
  Recreate bei reiner Mount-Aenderung). FIX: `docker compose restart caddy`.
- PowerShell `Import-Certificate -CertStoreLocation Cert:\CurrentUser\Root` -> "Benutzeroberflaeche
  nicht zulaessig" (Root-Store-GUI-Prompt in non-interaktiver Shell). FIX: `certutil -addstore -user
  -f Root <crt>` (funktioniert ohne Prompt).
- SSH-User `frank` -> Permission denied. RICHTIG: `root@10.8.0.1`.

## Wichtige Recherche-Ergebnisse
- getUserMedia/Mikrofon braucht "secure context" (HTTPS oder localhost) — http://LAN-IP ist blockiert
  (MDN, Chrome M74). Web Speech API ebenso. -> HTTPS war Pflicht, nicht der Key.

## Naechste Schritte (priorisiert)
1. Auf Franks Mikro-Test reagieren (siehe Wiedereinstiegspunkt). Bei Erfolg: Abschluss-Boxen alle 10.
2. Falls Frank Web Speech statt Groq will: Mikro-Button im Frontend auf webkitSpeechRecognition umbauen
   (kein Server-Key, lang='de-DE', onresult -> insertIntoChat). Auch das braucht den secure context (HTTPS).
3. Optionale Verbesserungen aus den VORGESCHLAGENE-Boxen (Whisper no_speech_prob-Filter, G-Button im
   Editor, Logbuch-Suche, Eintrag loeschen im Editor, Server-Swap 0 B -> swapfile).

## Offene Fragen
- Ist nach Chrome-Neustart das "nicht sicher" weg und nimmt das Mikrofon auf? (Frank testet)
- Groq behalten oder auf keylose Web Speech API umstellen? (Franks Praeferenz "kein Key")

## Anker
- Branch: main
- Letzte Commits:
e13ce6dc2 #47154 - docs(bugs): reverse-proxy-tls §1.10 IP-HTTPS ohne SNI
1245b3f06 #47153 - feat(sb): HTTPS-Reverse-Proxy (Caddy) fuers Cockpit
c865aad0b #47152 - feat(sb-dashboard): gespraeche nicht in Uebersicht zaehlen
(danach #47155 Caddyfile default_sni — Repo=Server)
