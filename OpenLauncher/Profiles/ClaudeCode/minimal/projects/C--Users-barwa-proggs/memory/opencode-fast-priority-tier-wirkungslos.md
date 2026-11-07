---
name: opencode-fast-priority-tier-wirkungslos
description: "OpenCode-Fast-Modelle (Sol/Terra/Luna) über ChatGPT-OAuth: ob Fast/Priority wirkt, ist über das service_tier-Response-Feld NICHT entscheidbar (frühere \"läuft nur default\"-Behauptung war Überbehauptung); A/B-Messung: nur Luna ~1,5× schneller"
metadata: 
  node_type: memory
  type: project
  originSessionId: 9aef8aa9-8d29-4c45-b603-e04456507d19
  modified: 2026-11-07T13:54:19.578Z
---

**Aufbau:** „Fast"-Modelle im OpenCodeLauncher (`gpt-5.6-sol-fast`, `-terra-fast`, `-luna-fast`) = lokaler Alias (`id` aufs Basismodell) + `serviceTier:"priority"`. Provider `openai` per **OAuth** (ChatGPT-/Codex-Zugang, `~/.local/share/opencode/auth.json` → `type:"oauth"`), NICHT per API-Key → Requests über Codex-Backend `chatgpt.com/backend-api/codex/responses`, nicht `api.openai.com/v1`.

**Kern (Sonnet-5-Schwarm + Web-Recherche 18.07.2026):** `response.service_tier` ist auf dieser OAuth-Schiene KEIN Beweis, in KEINE Richtung:
- Codex/ChatGPT hat einen EIGENEN Fast-Mechanismus (`service_tier:"fast"` + `[features].fast_mode=true`, serverseitiges Routing für „eligible ChatGPT plans"), NICHT das API-Platform-`priority`. Belege: developers.openai.com/codex/speed (Fast = 1,5×, GPT-5.6/5.5/5.4, per ChatGPT-Login); OpenAI-PR openai/codex#19053 („Default Fast service tier for eligible ChatGPT plans", Autor shijie-oai = OpenAI).
- WebSocket-/app-server-Schiene normalisiert den Tier oft VOR dem Request auf `null` bzw. lässt ihn im Payload weg; direktes Senden von `fast` gab „Unsupported service_tier: fast" (openai/codex#14204). Response meldet dann `default`/`null`, ohne Aussage übers echte Backend-Routing (openai/codex#32191).
- Von beiden Seiten übersehen (Mechanismus-Mismatch): OpenCode sendet `priority`, Codex-Fast erwartet `fast`/`fast_mode`. Ob `priority` am Codex-Endpoint überhaupt Fast auslöst, ist offen.

**Falsche frühere Behauptungen:**
1. „Es sind die schnellen Modelle" (nur aus Config geschlossen).
2. „379/379 = bewiesen NICHT Priority" (aus dem Response-Feld). Die 379/379 `default` sind echt (Schwarm-verifiziert), beweisen auf OAuth aber NICHTS über Fast.
3. Gegen-These eines anderen Researchers, teils richtig: Feld auf OAuth kein Beweis; Sidebar übertrug API-Key-Semantik fälschlich auf OAuth. Kronzeuge falsch: „OpenAI-Contributor bestätigt in #32191 serverseitiges Fast-Routing" hält nicht. #32191 ist User-Report (colindotfun, „Fast mode silently falls back to standard") + Kommentar `riverpilot` (association: none = KEIN OpenAI), keine Maintainer-Bestätigung. Die harte Evidenz dort (Katalog ohne tiers für Sol/Terra/Luna, Tier→null, identische Streaming-Kadenz) spricht eher gegen wirksames Fast. „Fast lief in Wahrheit" ist EBENSO unbewiesen.

**Client sauber (Schwarm, Binary-verifiziert opencode 1.18.3):** `service_tier:"priority"` geht korrekt raus (id-Alias verschluckt nichts; `serviceTier`→`service_tier`-Mapping ok; keine lokale Löschung, da `gpt-5.6-sol` `supportsPriorityProcessing=true`). Fehler liegt NICHT im OpenCode-Client.

**Verlässliche Verifikation (statt Response-Feld):** (a) ChatGPT-Plan-/Credit-Verbrauch messen (Fast ~2,5× Credits, müsste im Kontingent sichtbar sein); (b) Codex-nativ `fast_mode=true`+`service_tier="fast"` statt OpenCodes `priority` fahren, Plan-Verbrauch/`response.created`-Rohmetadaten beobachten; (c) Gegentest über echten API-Key-Provider (`api.openai.com`) mit gebuchtem Priority, dort IST das Feld verlässlich.

**A/B-Tests 18.07.2026 (Tokenverbrauch.jsonl, eigenständig verifiziert):** je 3 Paare Normal vs. Fast abwechselnd, IDENTISCHER Prompt (userPromptHash `c1a9ba99…`) und IDENTISCHE Output-Länge (1504 Tokens).
- **Sol (Zeilen 381–386): kein Vorteil.** Model-Step-Dauer (startedAt→step-finish) Normal Ø 31,37 s (29,66/33,87/30,60), Fast Ø 30,31 s (29,82/29,51/31,61) → ~3,4 %, im Rauschen; ~50–56 tok/s bei BEIDEN. Echter 1,5×-Effekt (~20 s / ~84 tok/s) tritt NICHT ein. `provider.serviceTier` bei den A/B-Läufen `null` (Feld fehlte ganz), nur der frühere E2E (Zeile 380) hatte `default`+responseID. Position kippte damals von „unentscheidbar" zu „Evidenz klar GEGEN praktischen Fast-Vorteil auf OAuth" (3 Indizien: kein A/B-Durchsatzvorteil + kein bestätigender Tier + leerer Fast-Katalog #32191). Einschränkung: 1,5×-Durchsatz widerlegt; reiner Queue-/Latenz-Vorteil UNTER HOCHLAST nicht getestet (Läufe bei geringer Last). n=3 reicht gegen 50-%-Effekt, nicht gegen 5 %.
- **Luna (Z488–495): klarer Fast-Vorteil.** Step-Dauer (startedAt→timestamp) Normal Ø 29,71 s vs. Fast Ø 20,72 s (−9 s); Durchsatz Normal 49,6–51,2 vs. Fast 71,8–73,7 tok/s, Verteilungen überlappen NICHT (3 vs. 3). Faktor ~1,43× (ganze Step-Dauer) bzw. ~1,52× (OpenCodes reine-Generierungs-Isolierung: 56,15 vs. 85,17 tok/s) ≈ OpenAIs beworbenes 1,5×. Integritätscheck bestanden: genau 6 Läufe (keine abgebrochenen, alle `reason=stop`), 6 getrennte Sessions, Cache als Ursache widerlegt (2 der 3 Fast-Läufe mit `cacheRead=0` gleich schnell), Reasoning-Tokens gleichverteilt. Entscheidend: Unterschied liegt in der GENERIERUNGSRATE (tok/s), nicht in Wartezeit/TTFT (Fast sogar leicht höher: 7,91 s vs. 7,34 s); ein reiner Queue-Effekt würde nur TTFT senken → deutet auf echtes Fast-Compute-Routing.
- **Terra (Z515–521): KEIN Vorteil.** Normal Ø 30,16 s / 49,9 tok/s vs. Fast Ø 30,40 s / 49,6 tok/s → 0,994× (Fast minimal langsamer), wie Sol.

**Stand:** Die pauschale Position „Evidenz gegen Fast-Vorteil" gilt NICHT für alle Modelle; Effekt ist modell-/lastabhängig. Bewiesen: der Fast-Pfad KANN wirken (Client-Config korrekt, Backend liefert bei Luna 1,5×), aber NICHT durchgängig für alle Fast-Aliase. Deutung (plausibel, nicht serverseitig bewiesen): Auslastungs-/Kapazitäts-Hypothese; Luna (schwächstes, selten genutztes Modell) hat freie Fast-Kapazität, Sol/Terra (stärker ausgelastet) fallen still auf Standard zurück (konsistent mit #32191). Grenzen: `response.service_tier` bei ALLEN Läufen leer/`null` → Nachweis nur per Laufzeitmessung, keine Server-Bestätigung; n=3 je Zelle; harter Beweis (Credit-Verbrauch ~2,5×) nicht gemessen. Nutzer-Formulierung „fast 30 s schneller" war ein Missverständnis (30,4 s = absolute Fast-GESAMTzeit; Differenz ~8–9 s bzw. 21,6 %).

**Commits (main, gepusht):** `fde7dba11abb435cb8b68e5f6382e9504f148531` „OpenCode: OAuth-Fast-Semantik korrigieren" (fügt Plugin `openai-fast-service-tier.js` hinzu, erzwingt `serviceTier=priority` für alle Fast-Aliase; nimmt im bug-case an, aus `default` sei fälschlich auf „Fast funktioniert nicht" geschlossen worden, diese optimistische Annahme stützt der A/B-Test NICHT) + `7cd5c978345259ddf207b22eea1331ac51f42145` „Provider-Tier und Audit-Zustand korrigieren" (Sidebar-Anzeige-Fix). Empfehlung der Test-Session (korrekt): Sidebar auf „Priority angefordert · nicht bestätigt" stellen.
