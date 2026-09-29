# Kimi Code (Kimi for Coding) in OpenCode — Bug-Almanach

> **Lesen vor Arbeit an Kimi-Code-Abo/Kimi-Provider in OpenCode oder anderen Drittanbieter-Clients.**
> **Stand:** 29.09.2026, 12:37 Uhr, ergänzt durch Engine C mit sieben Researchern. **Anker:** Kimi Code Docs
> Stand 29.09.2026 (`kimi-for-coding` = K2.8 Preview, `k3`, `k3-256k`, `kimi-for-coding-highspeed`);
> ältere opencode-Issues betreffen 1.15.3; neue Befunde Sidebar 1.15.0→1.16.0 / OpenCode 1.18.33.
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
- **Symptom:** Gleiche ID liefert seit 11.09.2026 K2.8 Preview statt K2.7. Die Sidebar 1.15.0 ordnete ihr trotzdem K2.7-Preise zu.
- **Ursache:** Eine veränderliche Coding-ID wurde wie eine feste API-Modellgeneration behandelt.
- **Fix:** In 1.16.0 ohne belegten K2.8-USD-Tarif `nicht verfügbar` statt falschem K2.7-/Nullpreis. `k3`/`k3-256k` explizit wählen; auch diese werden laut Doku bei Thinking off durch K2.8 Preview bedient.
- **Quellen [offiziell]:** https://www.kimi.com/code/docs/en/kimi-code/models.html · https://www.kimi.com/code/docs/en/kimi-code/whats-new.html#k2-8-preview-september-11-2026

### 7. Halber Abo-Verbrauch wird mit halbem Dollarpreis verwechselt
- **Symptom:** K3-256K und K3 zeigen denselben Preis, obwohl Kimi ungefähr ½ Kontingentverbrauch nennt.
- **Ursache:** `withKimiPricing` in 1.15.0 setzte API-Vergleichspreise ein, ohne deren Unterschied zum Abo-Verbrauch im TUI zu erklären.
- **Fix:** 1.16.0 kennzeichnet Vergleichspreise, zeigt den ungefähren Abo-Faktor separat und liest den tatsächlichen Monatsanteil direkt vom Anbieter. Keine Halbierung von Tokenzahlen oder erfundener USD-Tarif.
- **Quelle [offiziell]:** https://www.kimi.com/code/docs/en/kimi-code/models.html · https://platform.kimi.ai/docs/guide/kimi-k3-quickstart (API-Preis ohne Kontextstaffelung).

### 8. Falsche Batch- und Fremdanbietertarife
- **Symptom:** Sidebar 1.15.0 halbierte jedes Moonshot-`:batch` und ergänzte Moonshot-Write-Preise auch bei OpenRouter.
- **Ursache:** Pauschaler Multiplikator und Modellname wurden anstelle produkt-/providerspezifischer Preistabellen verwendet.
- **Fix:** 1.16.0 kennt nur belegte K2.6/K2.7-Batchtarife (60 %), K2.6-Cache-read explizit 0,10; keine K3-Batch-Erfindung. OpenRouter behält eigene Preise.
- **Quellen [offiziell]:** https://platform.kimi.ai/docs/pricing/batch · https://platform.kimi.ai/docs/guide/use-batch-api

### 9. Monatsquote fehlt oder wird aus der falschen Größe erzeugt
- **Symptom:** Parser zeigt nur Woche, 5h oder lokale 30-Tage-Tokens; fehlende Felder erscheinen fälschlich als 0 %.
- **Ursache:** Altes `usage`/`limits[]`-Schema oder `limit_month_code` wird für das neue gemeinsame Monatskontingent verwendet.
- **Fix:** `usages.limit_month_total.used_ratio` und `.reset_time` separat parsen. Zahlenstrings zulassen, ungültige Werte als unbekannt anzeigen, Reset niemals berechnen. 1.16.0 nutzt regionstreue Coding-Credentials, Timeout, Redirect-Sperre, zusammengefasste Refreshes und sichtbares `n/v` bei Fehlern.
- **Quellen [offiziell]:** https://github.com/MoonshotAI/kimi-code/blob/06ebfc821e304bd4ca3f828ae739954166463d65/packages/oauth/src/managed-usage.ts · https://www.kimi.com/code/docs/en/kimi-code/membership.html

### 10. Doppelte Tokenabrechnung oder falscher Cache-Default
- **Symptom:** Input/Reasoning/Cache-Summen oder Write-Kosten zu hoch; Messages schreibt erwarteten Cache nicht.
- **Ursache:** Messages schließt Cache aus `input_tokens` aus, Responses/Chat schließen ihn ein. Reasoning und Cache-TTL-Aufteilung sind Unterzähler. Messages ohne Top-Level `cache_control` schreibt nicht.
- **Fix:** Vor Normalisierung API-Format unterscheiden; OpenCode-normalisierte Usage nicht erneut zerlegen. Write-Tokens nicht zusätzlich als normalen Input abrechnen. Ohne 1h-Telemetrie die Sidebar-K3-Kosten als 5min-Schätzung markieren.
- **Quellen [offiziell]:** https://platform.kimi.ai/docs/api/messages · https://platform.kimi.ai/docs/api/responses · https://platform.kimi.ai/docs/guide/context-caching

### 11. TUI verwendet SDK-v1-Aufrufe mit einem SDK-v2-Client
- **Symptom:** Historischer Session-Verlauf/Fehlerlogging wird nicht korrekt angefragt; Typdiagnose beanstandet `path`/`body`.
- **Ursache:** `TuiPluginApi.client` importiert `@opencode-ai/sdk/v2`; Sidebar übergab `{path, query}` beziehungsweise `{body, query}`.
- **Fix:** 1.16.0 verwendet `session.messages({sessionID, directory})` und `app.log({service, level, message, extra, directory})`. Loggingfehler dürfen keine unbehandelten TUI-Rejections erzeugen.
- **Beleg:** lokale installierte `@opencode-ai/plugin/dist/tui.d.ts` und `@opencode-ai/sdk/dist/v2/gen/sdk.gen.d.ts` (Plugin 1.17.15).

## Ursachen- und Absicherungsnotiz

Warum falsche Preise? Feste API-Tabelle auf Coding-Aliase übertragen → Alias-/Produktunterschiede ignoriert → kein getrennter Datenbegriff für Abo-Quote und Dollarvergleich.
Verwandte Stellen: Tarifanzeige UND historische Kosten verwenden denselben korrigierten Resolver; Batch/Fremdanbieter ebenfalls berücksichtigt. GPT-Wochenanzeige bleibt verbleibend, Kimi-Monat wird ausdrücklich verbraucht beschriftet. Netzwerkfehler ergeben sichtbar `n/v`; ein späterer Poll heilt den Zustand. Gemeinsamer TS-Code und Home-/XDG-Pfadauflösung für Windows/macOS.
Build mit Bun/OpenTUI-Solid erfolgreich, authentifizierter Monatsabruf erfolgreich; keine Testsuite oder visuelle TUI-Abnahme im Schnellmodus.
Hook-Einordnung: rekursiver Index und vorhandener `opencode/kimi-code-gateway`-Hint erfassen diesen Bereich bereits. Der Providerbereich ist dateiübergreifend; installierte Config/Plugins bleiben unter dem vorhandenen allgemeinen OpenCode-Guard. Keine neuen Hook-Muster nötig.
