# Kimi Code (Kimi for Coding) in OpenCode — Bug-Almanach

> **Lesen vor Arbeit an Kimi-Code-Abo/Kimi-Provider in OpenCode oder anderen Drittanbieter-Clients.**
> **Stand:** recherchiert am 29.09.2026 (Engine C, 4 Sonnet-5.5-Researcher). **Anker:** Kimi Code Docs
> Stand 29.09.2026 (`kimi-for-coding` = K2.8 Preview, `k3`, `k3-256k`, `kimi-for-coding-highspeed`);
> opencode-Issues betreffen 1.15.3. Seiten teils nur per WebFetch-Kurzfassung gelesen → Zahlen gegenprüfen.
> Best-Practices-Gegenseite: `best-practices/opencode/kimi-code-abo.md`. Kurzcheck: `kimi-code-gateway-kurzcheck.md`.

## Einträge

### 1. Gateway lehnt Client ab: 403 „only available for Coding Agents" / 429 „engine overloaded"
- **Symptom:** Anfrage an `api.kimi.com/coding/v1` (bzw. `api.kimi.ai/coding/`) liefert 403 „Kimi For Coding is currently only available for Coding Agents such as Kimi CLI, Claude Code, Roo Code, Kilo Code, etc." oder 429 „engine overloaded".
- **Ursache (Nutzerberichte, nicht offiziell bestätigt):** Das Gateway prüft die Client-Kennung (User-Agent). Nicht gelistete Clients (nanobot, Cline, OpenClaw) werden abgewiesen. OpenCode 1.15.3 sendete mit dem eingebauten Provider „Anthropic/JS".
- **Fix:** Offiziell dokumentierte Clients nutzen (Claude Code, OpenCode, Codex, Hermes Agent; OpenCode: `opencode auth login` → Provider „Kimi For Coding"). **User-Agent NIE fälschen** — laut Help Center ein Verstoß, kann zur Aussetzung der Abo-Vorteile führen.
- **Quellen:** https://github.com/anomalyco/opencode/issues/27902 · https://github.com/HKUDS/nanobot/issues/354 · https://github.com/cline/cline/issues/10307 · https://github.com/openclaw/openclaw/issues/42499 · https://www.kimi.com/en/help/kimi-code/benefits

### 2. OpenCode wendet Header/User-Agent aus `opencode.json` nicht an
- **Symptom:** Konfigurierte `headers` (User-Agent) kommen beim Provider nicht an.
- **Ursache:** OpenCode-Issue #22608 (Stand April 2026, „not planned" geschlossen); heutiger Stand ungeprüft. Ohnehin kein empfohlener Weg (siehe #1).
- **Fix:** Nicht per Header tricksen; offiziellen Provider `kimi-for-coding` nutzen.
- **Quelle:** https://github.com/anomalyco/opencode/issues/22608

### 3. Quota erschöpft = HTTP 403 (kein Auth-Fehler)
- **Symptom:** 403 trotz gültigem Key; manche Clients behandeln es als Auth-Fehler und rotieren/verwerfen den Key.
- **Ursache:** 403 steht für 5-h-Limit, 7-Tage-Limit (Legacy) und Monatslimit; 402 = „unable to verify your membership benefits"; 429 = zu viele parallele Anfragen.
- **Fix:** Fehlerreferenz lesen, Fenster abwarten oder Extra Usage aufladen (umgeht laut Changelog 09.07.2026 die Limits, Abrechnung ≈ API-Preise, Minimum 25 CNY).
- **Quellen:** https://www.kimi.com/code/docs/en/kimi-code/error-reference.html · https://github.com/openclaw/openclaw/issues/142524 · https://www.kimi.com/code/docs/en/kimi-code/whats-new.html

### 4. `kimi-for-coding-highspeed` → HTTP 401 bei zu niedrigem Plan
- **Ursache:** High Speed (= K2.7 Code HighSpeed) und 1M-Kontext erst ab Pro (Legacy: Allegretto). K3 ab Plus.
- **Fix:** Plan prüfen oder `k3-256k` / `kimi-for-coding` wählen.
- **Quellen:** https://www.kimi.com/en/help/kimi-code/membership-guide · https://www.kimi.com/code/docs/en/third-party-tools/opencode.html

### 5. Tooling erkennt `k3-256k` nicht
- **Symptom:** Regex/Modellkatalog kennt die ID `k3-256k` nicht (endet auf „k").
- **Betroffen:** oh-my-openagent (#8469), CLIProxyAPI (#4612, Feature-Request).
- **Quellen:** https://github.com/code-yeongyu/oh-my-openagent/issues/8469 · https://github.com/router-for-me/CLIProxyAPI/issues/4612

### 6. `kimi-for-coding` wandert still auf neue Modelle
- **Symptom:** Gleicher Modellname liefert anderes Verhalten (K2.7 → K2.8 Preview am 11.09.2026, laut Suchsnippet, Docs-Seite nicht abrufbar). Modellwechsel invalidiert außerdem den Kontext-Cache.
- **Fix:** Für Reproduzierbarkeit `k3` oder `k3-256k` explizit wählen. Unsicher: bei ausgeschaltetem Thinking soll K3 laut Drittsnippet von K2.8 Preview bedient werden.
- **Quelle:** https://www.kimi.com/code/docs/en/kimi-code/models.html
