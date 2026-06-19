# Session Handoff — 2026-06-20

## Ziel (1-3 Saetze)
Lange Session: (1) OpenCode-Plugins eingerichtet (notifier/plannotator/firecrawl), (2) ausfuehrlich
die GUENSTIGE Recherche-Pipeline getestet (Firecrawl scrape/search + billiges OpenRouter-Modell statt
teurem Opus-Schwarm) und (3) mehrere Recherchen als neue Almanache/Best-Practices persistiert.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Alles committed bis #46966, Working Tree
hat keine eigenen uncommitteten Dateien (nur fremde Parallel-Session-/Cache-Dateien — NICHT anfassen).

## Aktueller Status
- Erledigt diese Session:
  - OpenCode-Plugins notifier + plannotator (workflow:manual) + firecrawl-MCP eingerichtet (in
    ~/.config/opencode/opencode.jsonc; Firecrawl-Key in ~/SK/OpenCode/firecrawl-api-key.txt via {file:}).
    notifier-Toene (3 WAV) in ~/.config/opencode/sounds/. ffmpeg via winget installiert (+ ~/bin-Shims).
  - Firecrawl-Doku korrigiert (= MCP, kein npm-Plugin): #46941.
  - Research-Fallen (github-WebFetch blockiert + Researcher-Rate-Limit) in agent-knowledge-system.md §11/§12: #46942.
  - Neue Almanache+Best-Practices: android/app-widgets (#46962), AGP-9-Deltas §2.6 (#46961),
    server/wireguard + server/vps-hosting (NEUE Kategorie server/, #46966).
  - A/B-Tests der Recherche-Pipeline (siehe "Wichtige Recherche-Ergebnisse").
- In Arbeit: nichts.
- Blockiert: OpenRouter-Go-Abo (Frank macht es 2026-06-20; danach Key + Re-Test).

## Relevante Dateien
- best-practices/opencode/self-hosted-memory-server.md — Memory-Server-Bauplan (supermemory self-host, Hetzner/Hostinger, WireGuard-Verweis).
- bugs/server/wireguard.md + best-practices/server/wireguard.md — WireGuard (inkl. Opus-Korrektur IP-Forwarding-Mythos).
- bugs/server/vps-hosting.md + best-practices/server/vps-hosting.md — Hostinger/Hetzner VPS-Wahl.
- bugs/android/app-widgets.md + best-practices/ — Glance + klassische RemoteViews.
- Memory: project_research_pipeline_and_openrouter_go.md — die validierte Pipeline + OpenRouter-Go-Plan.

## Getroffene Entscheidungen
- VALIDIERT als Standard-Recherche: Firecrawl + deepseek/deepseek-v4-pro (1M ctx, ~2 Cent/Recherche,
  ~120x weniger Opus-Token, ~Opus-Qualitaet, zuverlaessig). Opus nur fuer haerteste/heikelste Faelle.
- VOR jeder Recherche zuerst lokalen Almanach/Best-Practices pruefen (Lehre aus AGP-Doppel-Recherche).
- OpenRouter-Key liegt in ~/SK/ClaudeCodeOpenRouter/openrouter.key (Datei hat Kommentar-Header → Key-Zeile per Regex sk-or-v1-... extrahieren).

## Fehlgeschlagene Ansaetze (WICHTIG — nicht wiederholen)
- Gratis-OpenRouter-Modelle sind unzuverlaessig: 429-Rate-Limit (llama-3.3-70b:free, qwen3-coder:free);
  Nemotron 3 Ultra 550B :free = 2 Tok/s -> 504-Timeout. Nemotron 3 Super 120B :free (30 Tok/s) lief,
  uebernahm aber einen Sachfehler (IP-Forwarding-Mythos). -> fuer Pipeline ein BEZAHL-Modell nutzen.
- Firecrawl /v2/extract ist deprecated; fuer Synthese stattdessen /v2/scrape oder /v2/search + eigenes Modell.
- git commit -- <pfade> nimmt NEUE (untracked) Dateien NICHT mit -> bei neuen Dateien git add nutzen.

## Wichtige Recherche-Ergebnisse
- A/B-Test 1 (AGP 9.0): war schon im Almanach -> Doppel-Recherche, Lehre gezogen.
- A/B-Test 2 (WireGuard): Opus fing den IP-Forwarding-Mythos (Forwarding nur fuer Full-Tunnel, NICHT
  fuer Dienst-auf-dem-Host), den Nemotron 120B uebernahm.
- A/B-Test 3 (Hostinger Memory-DB): deepseek-v4-pro = exzellent + ehrlich ueber Quellenluecken,
  prompt 13013 + completion 4918 = $0.0197, Antwort ~1969 Token an mich. Opus-Researcher 235.536 Token.
  -> Pipeline validiert.

## Naechste Schritte (priorisiert)
1. OpenRouter-Go-Key (kommt 2026-06-20 von Frank): in SK ablegen + DAUERHAFT nutzen; Pipeline mit den
   staerkeren Go-Modellen erneut testen.
2. Offen (Frank-OK abwarten): "Firecrawl+deepseek-v4-pro zuerst, Opus auf Eskalation" + "Almanach-Check
   vor Recherche" als feste Regel verankern.
3. guard-coverage-Allowlist (bugs/check-guard-coverage.py) um die neuen Konzept-Almanache ergaenzen:
   server/wireguard, server/vps-hosting, android/app-widgets (sonst als LUECKEN gezaehlt, 5 aktuell).
4. supermemory-Server-Projekt starten (Bauplan + Anbieter + WireGuard sind komplett dokumentiert).
5. Festplatte aufraeumen (98% belegt).

## Offene Fragen
- 2 Intelligenz-Vorschlaege offen: (a) Pipeline-Regel verankern? (b) kompletter feedback_*-Memory ->
  Almanach-Abgleich? Beide auf Franks "ja" warten.

## Anker
- Branch: main
- Letzte Commits:
264d123b6 #46966 - docs(server): new 'server/' category — WireGuard + VPS-hosting almanacs+best-practices from A/B research
f7b09a403 #46965 - session restore: clear handoff backup
72a482681 #46964 - session backup: ID-architecture stages 1/2a/2b done+verified
51aa7ad07 #46963 - feat(EntropieReductor): ID-architecture stage 2b
d9bc2759f #46962 - feat(EntropieReductor): ID-architecture stage 2a
