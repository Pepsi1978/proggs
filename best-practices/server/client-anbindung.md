# Endgeraet-zu-self-hosted-Server-Anbindung — Best Practices (Praeventions-Seite)

> **Zweite Seite der Medaille zum Bug-Almanach** [`bugs/server/client-anbindung.md`](../../bugs/server/client-anbindung.md):
> dort steht *was schiefgeht*, hier *wie man eine App von vornherein richtig an ein eigenes Backend anbindet*.
> Gilt fuer die **Client-/Geraete-Seite**: Android-Apps, iOS/Auto-Sprach-App, macOS-Overlays (Swift/AppKit),
> Windows-Overlays (C#/.NET) — Zugriff auf brain-api unter `http://10.8.0.1:8000` (WireGuard), spaeter Reverse-Proxy/TLS.
>
> **Stand:** 2026-06-24 (recherchiert: Firecrawl+MiniMax-Schwarm; je Empfehlung Quelle + offiziell/extern-Flag).
> **Anker:** Android targetSdk 36/minSdk 26-29 · iOS 14+/18 · macOS 15 Sequoia · .NET (HttpClient) · WireGuard-Client.
> Multi-Plattform/Theme — kein Live-Anker.
>
> **Abgrenzung:** `best-practices/server/wireguard.md` = Server-/Tunnel-Seite · `best-practices/android/…` (falls vorhanden) =
> allgemeiner HTTP-Client. HIER: die Geraet-zu-eigenem-Server-Anbindung. **Ehrlichkeit:** Punkte ohne frische Quelle sind
> als `kanonisch` markiert (etablierte, offizielle Doku — keine erfundenen URLs).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Thema | Best Practice (Kurzform) | Almanach |
|---|-------|--------------------------|----------|
| 1 | EIN API-Contract fuer alle Clients | Contract-First OpenAPI (`/v1`, `/openapi.json`); konsistentes JSON, RFC 9457 Fehler, Cursor-Pagination; ein generiertes Client-SDK je Plattform | §1, Almanach §6.3 |
| 2 | Schreib-Calls | `Idempotency-Key`-Header pro Nutzer-Intent (UUID, vor 1. Versuch persistiert); PUT/DELETE idempotent | §2, Almanach §6.1 |
| 3 | Auth | Bearer + kurzlebige Access-Tokens + Refresh-Token-Rotation; Least-Privilege-Scopes; Token wie Passwort behandeln | §3 |
| 4 | Token-Storage Android | Android Keystore (StrongBox wenn da) + DataStore+Tink (NICHT mehr EncryptedSharedPreferences, deprecatet) | §3.1 |
| 5 | Token-Storage iOS/macOS | Keychain + Secure Enclave + ACL (`kSecAttrAccessibleAfterFirstUnlock`/`…ThisDeviceOnly`), nie `UserDefaults` | §3.2 |
| 6 | Token-Storage Windows | DPAPI / Credential Manager / .NET DataProtection — nie Klartext, nie im Log (kanonisch) | §3.3 |
| 7 | Zugriffsweg | Privat = WireGuard/Tailscale (nur eigene Geraete); oeffentlich = Reverse-Proxy+TLS+Auth. Nie HTTP-Port roh ins Internet | §4 |
| 8 | Offline-First | Lokale DB = Source of Truth; Outbox/Queue fuer Writes; optimistic UI; nie auf synchrone Antwort im UI-Pfad warten | §5, Almanach §6.1 |
| 9 | Konfliktloesung | LWW wenn Konflikte unkritisch; Versionierung/CRDT wenn wichtig — bewusst waehlen | §5 |
| 10 | Timeouts | Connect/Read/Write/Call getrennt dimensionieren; `callTimeout` (OkHttp) gegen Haenger; nie OS-Default | §6 |
| 11 | Retry | Exponential Backoff + FULL Jitter, `Retry-After` respektieren, nur idempotente Requests, Circuit-Breaker | §6 |
| 12 | Connection-Mgmt | Singleton-Client + Pool/Keep-Alive; `PooledConnectionLifetime` (.NET); `evictAll()` bei Netzwechsel | §6, Almanach §6.2/§7.3 |
| 13 | Interne CA | step-ca: Root offline, Intermediate online; Root-CA auf Clients vertrauen; kurze TTL + ACME-Renewal | §7, Almanach §5 |
| 14 | TLS-Identitaet | DNS-SAN (+ Split-Horizon-DNS auf die VPN-IP) statt nackter IP-SAN; LE geht NICHT fuer private IP | §7, Almanach §5.2/§5.3 |
| 15 | Pinning | Nur MIT Backup-Pin; auf die interne CA pinnen (ueberlebt Leaf-Renewal); nie ersatzlos abschalten | §7, Almanach §5.4 |
| 16 | Hintergrund-Sync | Android WorkManager (+ Constraints, Backoff) + High-Prio-FCM-Wake + FGS (korrekter Type ab A14); iOS BGTaskScheduler + Background-URLSession + Push-to-wake | §8, Almanach §4 |
| 17 | Resilienz im BG | Idempotent + Checkpoints + Backoff (BG-Slot kann jederzeit entzogen werden); Push = nur "neue Daten da"-Signal | §8 |
| 18 | Voice-Pipeline | Kaskadiert STT→klassifizieren→REST async→TTS; <500 ms Ziel; Streaming/Pipelining; Outbox bei Netzabriss; kurze TTS-Bestaetigung; im Auto kein visuelles UI | §9 |

---

## TL;DR — die 7 Leitsaetze

1. **Ein Contract, viele Clients:** OpenAPI Contract-First, daraus pro Plattform ein SDK generieren — nie pro App eigenes Schema.
2. **Schreiben ist idempotent:** Idempotency-Key pro Nutzer-Intent + Offline-Outbox; das Netz ist nur Sync-Mechanismus, die lokale DB ist die Wahrheit.
3. **Token wie Passwort:** kurzlebig + Refresh-Rotation, plattform-sicher gespeichert (Keystore/Keychain/DPAPI), nie im Code/Log.
4. **Zugriffsweg bewusst:** privat → VPN (Tailscale/WireGuard); oeffentlich → Reverse-Proxy+TLS+Auth. Beides kombinierbar, nie roh.
5. **Resilient by default:** getrennte Timeouts, Backoff+Jitter, nur idempotente Retries, Pool-Recycling bei Netzwechsel/Standby.
6. **Interne CA richtig:** Root offline, kurze TTL, DNS-SAN, Pinning nur mit Backup — Rotation ohne App-Update.
7. **Voice im Auto:** Streaming-Pipeline mit <500 ms, asynchrone REST-Calls + Outbox, kurze gesprochene Quittung, keine Blicke von der Strasse.

---

## §1 Einheitlicher REST-API-Contract fuer alle Clients

- **Do — Contract-First (OpenAPI):** OpenAPI-Spec zuerst schreiben, Server-Stubs + Client-SDKs daraus generieren, Contract-Tests; Spec unter `/openapi.json` ausliefern und mit ALLEN Konsumenten (Android/iOS/macOS/Windows/Voice) reviewen. **Don't:** Code-First mit nachtraeglicher Doku; inkonsistente Feldnamen je Endpoint. `offiziell/extern` (psenger, Fern, Postman).
- **Do — URL-Versionierung:** nur Major im Pfad (`/v1/...`), Deprecation via `Sunset`-Header (RFC 8594). **Don't:** Verben in URLs, Minor/Patch im Pfad.
- **Do — Einheitliches Fehler-Format RFC 9457** (Problem Details: `type`/`title`/`status`/`detail`/`instance`); ALLE Validierungsfehler in einer Response; stabile maschinenlesbare Error-Codes. **Don't:** je Endpoint andere Error-Shape; Internals/Secrets im Error-Body. (Fern; RFC 9457 = offiziell.)
- **Do — Cursor-Pagination** fuer grosse/sich aendernde Datensets (Mobile/Voice), Pagination-Metadaten mitliefern. **Don't:** Offset-Pagination bei >10 000 Eintraegen. (SBB, Fern.)
- **Do — Content-Negotiation:** `Content-Type: application/json; charset=utf-8`; optional `ETag`+`If-Match` fuer Konsistenz. **Don't:** XML als Default.
- **`store/recall/search`-Endpunkt-Design (kanonisch, Quellen schwiegen):** Ressourcen als Nomen, Aktion via HTTP-Methode. Empfohlen fuer das Gehirn: `POST /v1/memories` (store, mit `Idempotency-Key`), `GET /v1/memories?query=…` bzw. `POST /v1/search` (recall/search, lesend), konsistent ueber alle Clients. Ein einziger Contract — der Voice-Client nutzt dieselben Endpunkte wie die Desktop-App.

## §2 Idempotente Schreib-Calls (Contract-Ebene)

- **Do:** `Idempotency-Key`-Header (UUID) fuer POST/PATCH; EIN Key pro Nutzer-Intent/Tap (nicht pro HTTP-Versuch), vor dem ersten Versuch persistiert; Server speichert Key+Ergebnis und gibt bei Wiederholung das gespeicherte Ergebnis. PUT/DELETE per Definition idempotent. **Don't:** „create"-Retries ohne Duplikatschutz. (SBB, Postman; IETF idempotency-key-header = kanonisch.) Koppelt direkt an Almanach §6.1.

## §3 Auth + sicheres Token-Storage pro Plattform

- **§3.0 Allgemein:** Tokens wie Passwoerter behandeln; KEINE hardcoded Tokens/Client-Secrets in verteiltem App-Code; immer HTTPS; JWTs streng validieren (alg/exp/iss/aud). Kurzlebige Access-Tokens (Minuten) + **Refresh-Token-Rotation**; Least-Privilege-Scopes; abgelaufene Tokens loeschen. **Don't:** langlebige Allmacht-Tokens. (42crunch, capgo — extern, aber konsolidiert.)
- **§3.1 Android:** Schluessel im **Android Keystore** (non-exportable; StrongBox via `setIsStrongBoxBacked()` wenn `FEATURE_STRONGBOX_KEYSTORE`). Token-Persistenz: **DataStore + Tink (AES256_GCM) + Keystore**; AAD pro Namespace binden. **Don't:** neue Implementierungen auf `EncryptedSharedPreferences` (in androidx.security-crypto 1.1.0 **deprecatet**); Schluessel im App-Sandbox/Code. Schluessel alle 90-180 Tage rotieren. (capgo, KMP-Credential-Store.)
- **§3.2 iOS/macOS:** **Keychain Services** (AES-256-GCM, Secure-Enclave-geschuetzte Metadaten-Keys); ACL mit Face/Touch ID/Passcode; `kSecAttrAccessibleAfterFirstUnlock` fuer Hintergrund-Sync-Tokens bzw. `…WhenPasscodeSetThisDeviceOnly` fuer geraetegebundene; Edge-Cases (Biometrie-Lockout/Reset) behandeln. **Don't:** Tokens in `UserDefaults`. (capgo, Apple Keychain = offiziell.)
- **§3.3 Windows/.NET (kanonisch — Quellen schwiegen):** Tokens via **DPAPI** (`ProtectedData`, CurrentUser-Scope) ODER **Windows Credential Manager** ODER **.NET Data Protection API** (`IDataProtector`) verschluesseln; nie Klartext in Datei/Registry/Log. Entspricht OWASP MASVS-STORAGE (keine Secrets im Klartext) — als kanonische Plattform-Empfehlung uebernommen.
- **§3.4 Querschnitt:** Secrets NIE in Prompt/Kontext/Log (auch Voice-App). Browser-Client: **Backend-for-Frontend/Token-Handler** statt Token im `localStorage`. (curity.)

## §4 Zugriffsweg: WireGuard-VPN vs. Reverse-Proxy+TLS (Entscheidungsmatrix)

> **Hinweis:** Dieser Abschnitt ist **kanonisch** (etablierte Architektur; der Firecrawl-Lauf lief in einen Timeout). Quellen: WireGuard/Tailscale-Doku, Caddy-Doku, sowie die lokalen Almanache `bugs/server/wireguard.md` und `bugs/server/self-hosted-ai-agent-server.md` §1.2 (Cloudflare-Tunnel strippt TLS).

| Zugriffsfall | Empfehlung | Begruendung |
|--------------|-----------|-------------|
| Nur eigene, vertrauenswuerdige Geraete (Frank's Handy/Laptop/Auto) | **WireGuard/Tailscale-VPN**, Dienst nur an `10.8.0.1` gebunden | Kleinste Angriffsflaeche: Dienst gar nicht oeffentlich; HTTP intern akzeptabel (Tunnel verschluesselt). Privacy: kein Dritter im Pfad |
| Geteilt / Dritte / Web-Client / kein VPN moeglich | **Reverse-Proxy (Caddy/nginx) mit TLS + Auth** | OEffentlich erreichbar braucht echte TLS-Terminierung + Authentifizierung; Caddy = Auto-Let's-Encrypt |
| Maximale Sicherheit fuer sensible Daten | **VPN + TLS + Auth kombiniert** (Defense-in-Depth), ODER **mTLS** | Zwei unabhaengige Schichten; mTLS bindet Client-Identitaet |
| Auto-Sprach-App unterwegs | **WireGuard-Client always-on** (eigenes Geraet) ODER Reverse-Proxy+TLS, wenn VPN im Auto unpraktisch | Abwaegung Erreichbarkeit vs. Tunnel-Stabilitaet (Almanach §3.3/§3.4) |

- **Do:** Zero-Trust — auch hinter VPN authentifizieren (Bearer), nicht „im VPN = vertraut". Dienst an die VPN-IP binden, nur UDP-`ListenPort` oeffentlich (siehe `wireguard.md`). **Don't:** rohen HTTP-Port ins Internet; **Cloudflare-Tunnel fuer persoenliche Daten** (strippt TLS im CF-Netz — self-hosted-Almanach §1.2) → eigener TLS-Proxy/WireGuard.

## §5 Offline-First + Outbox + Konfliktloesung

- **Do — Lokale DB als Source of Truth** (Android Room, iOS Core Data/Realm); UI beobachtet reaktive Streams (Flow). Netz = nur Sync-Mechanismus. **Don't:** `SharedPreferences`/`UserDefaults` fuer komplexe Daten. (extern, konsolidiert.)
- **Do — Outbox/Queue:** Schreibvorgang sofort lokal persistieren + „pending sync" markieren; eigentlichen Request via WorkManager/persistente Queue; optimistic UI. **Don't:** im UI-Pfad auf synchrone Antwort warten. Koppelt an Idempotency (§2).
- **Do — Konfliktloesung bewusst:** LWW wenn Konflikte unkritisch (Firebase/Trello); Versionierung (`(id, version)` + parent_version, Merge in Transaktion) wenn wichtig; CRDT fuer kollaborative Operationen. **Don't:** ohne Strategie ueberschreiben (Datenverlust).
- **Do — Sync-Trigger kombinieren:** Connectivity-Observer + WorkManager (Constraints) + Pull-to-Refresh + App-Start; bidirektional erst Push (Outbox) dann Pull; Alters-/Prioritaets-Strategie. Backoff bei Fehlversuchen.

## §6 Timeouts / Backoff / Resilienz / Connection-Management

- **Do — Timeouts getrennt** (Connect/Read/Write/Gesamt) an der Latenz-Verteilung (p99.9) ausrichten; Verbindungen beim Start vorwaermen. **Don't:** OS-Default als End-to-End-Timeout; alle Timeouts gleich/zu niedrig. (AWS Builders' Library.)
  - OkHttp (kanonisch + Almanach §6.2): `callTimeout(...)` (begrenzt den GESAMTEN Call; Default 0=unendlich) + getrennte connect/read/write; Richtwert connect 15 s / read 30 s / write 60 s mobil.
  - URLSession (kanonisch): `timeoutIntervalForRequest`/`forResource`; `waitsForConnectivity=true`.
  - .NET: `client.Timeout` passend; Timeout kommt als `TaskCanceledException` (Token pruefen).
- **Do — Exponential Backoff + FULL Jitter** (AWS-Style), `Retry-After`-Header respektieren, `maxRetries`/Cap, `isRetryable` (429+5xx retry, 4xx ausser 429/408 nicht). **Don't:** Sofort-/Fixed-Retry, kein Jitter (Thundering Herd), 4xx retryen. (AWS, Polly/MS Learn.)
- **Do — Circuit-Breaker** (Polly: FailureRatio 0.1, MinimumThroughput 100, SamplingDuration 30 s, BreakDuration 5 s); .NET: `Microsoft.Extensions.Http.Resilience` (Nachfolger von `Polly.Extensions.Http`, deprecatet). Swift: Retry-Wrapper.
- **Do — Connection-Mgmt:** Singleton-Client; Pool/Keep-Alive; .NET `SocketsHttpHandler.PooledConnectionLifetime` 2-15 min (DNS-Refresh, Almanach §7.3); OkHttp `connectionPool().evictAll()` bei Netzwechsel (Almanach §6.2). **Don't:** `new HttpClient()` pro Request (Socket-Exhaustion).
- **Do — Nur idempotente Requests retryen** (Timeout ≠ „nicht passiert") → koppelt an §2.

## §7 Cert-Handling: interne CA / Pinning mit Rotation

- **Do — Eigene PKI:** Root-CA **offline** (HSM/air-gapped), **Intermediate online** (step-ca); Passwoerter via `--password-file`, nie in CLI/Env; Backups an 2 Orten. **Don't:** Root-Key im Tagesbetrieb auf dem CA-Server; Default-Provisioner ungesichert. (smallstep — offiziell.)
- **Do — Root-CA auf Clients vertrauen** (nicht nur Intermediate); Android: CA in `res/raw/` + NSC `@raw`-trust-anchor (Almanach §5.1); iOS: Profil + „Full Trust" (§5.5); .NET/Windows: LocalMachine\Root.
- **Do — TLS-Identitaet:** **DNS-SAN** (z.B. `brain.lan`) + Split-Horizon-DNS auf die VPN-IP — robuster als nackte IP-SAN. **Don't:** auf Let's Encrypt fuer private IP setzen (geht nicht; nur oeffentliche IP seit 2025-07). (letsencrypt, community.)
- **Do — Kurze TTL + ACME-Renewal** automatisieren (step-ca Default 24 h; Host-Certs ≤ 1 Monat); Renewal im Hintergrund. **Don't:** langlebige Certs ohne Revocation.
- **Do — Pinning richtig (Almanach §5.4):** IMMER Backup-Pin; auf die **interne CA** pinnen (ueberlebt Leaf-Renewal); NSC `<pin-set>` mit `expiration`. **Don't:** Pinning ersatzlos abschalten; ohne Backup-Pin = App tot bei Renewal.

## §8 Hintergrund- / Sync-Strategie pro Plattform

- **Do — Android:** WorkManager fuer deferrable/periodic Sync (Constraints: NetworkType/charging; exponential Backoff; respektiert Doze automatisch). `setExpedited()` fuer kurze wichtige Aufgaben. **High-Priority-FCM** als Wake-Trigger fuer „neue Daten da". Foreground-Service nur fuer nutzersichtbare Daueraufgaben — ab Android 14 **korrekter `foregroundServiceType` Pflicht** (Almanach §4.3). **Don't:** Dauer-Polling im Hintergrund; FGS als generischer „keep alive". (Android Developers — offiziell.)
- **Do — iOS:** `BGAppRefreshTask` (fetch vor App-Start), `BGProcessingTask` (schwere Arbeit, nur am Charger+Netz), Tasks beim Launch registrieren + `BGTaskSchedulerPermittedIdentifiers` in Info.plist; **Background-URLSession** fuer grosse Transfers; **Background-Push** nur als „neue Daten"-Signal (discretionary). **Don't:** garantierte BG-Ausfuehrung annehmen; Push fuer Echtzeit. (WWDC25 — offiziell.)
- **Do — Resilienz (beide):** idempotent, Checkpoints frueh+oft, Backoff, Queue-State persistieren — der BG-Slot kann jederzeit entzogen werden. Hersteller-Killer (Samsung/Xiaomi/Huawei, Almanach §4.4) per User-Anleitung adressieren. Testen mit Screen-locked/Low-Power/BG-Refresh-aus/Airplane-Toggle.
- **Desktop (kanonisch):** Windows-Dienst / macOS LaunchAgent; bei Standby Connection-Pool recyceln (Almanach §7.3).
- **Push vs. Poll:** regelmaessige frische Daten → Poll (BGAppRefresh/PeriodicWork); seltene Updates → Push-to-wake; nutzerangestossen → Foreground.

## §9 End-to-End-Voice-Pipeline (Auto-Sprach-App)

- **Do — Kaskadierte Streaming-Pipeline** STT → (clientseitig klassifizieren/aufbereiten) → REST `store/recall` **async** → TTS-Bestaetigung. Bewaehrte Orchestratoren (LiveKit Agents/Pipecat) bringen VAD/Turn-Detection/WebRTC mit. **Don't:** native Speech-to-Speech fuer Realtime (kein Function-Calling, ~13 s TTFA). (arxiv, LiveKit — extern.)
- **Do — Latenz <500 ms** (ideal <400 ms TTFB) durch **Streaming + Pipelining** ueber alle Stufen; sentence-aware TTS-Streaming; System-Prompt per Prefix/KV-Cache cachen. **Don't:** auf vollstaendiges Transkript/komplette LLM-Antwort warten, bevor TTS startet. (Simplismart, arxiv.)
- **Do — Transport:** WebRTC/Opus (UDP, Jitter-Buffer) fuer Live-Audio. **Don't:** PCM ueber TCP/WebSocket fuer Live-Sprache. (LiveKit.)
- **Do — Barge-in:** VAD + semantische Turn-Detection + Background-Voice-Cancellation. **Do — Security:** ephemere, serverseitig ausgestellte Tokens, nie statische Keys in der App. (brain.co.)
- **Auto-spezifisch (kanonisch — Quellen schwiegen, mit den Projekt-Direktiven konsolidiert):**
  - **Fehler-Recovery bei Netzabriss:** Outbox-Pattern (§5) — Sprachnotiz wird lokal persistiert + bei Tunnel-Rueckkehr mit Idempotency-Key gesendet; nie still verloren.
  - **Kurze TTS-Bestaetigungen** statt langer Vorlesungen (kognitive Last im Auto niedrig halten).
  - **Freisprech/Sicherheit:** Android Auto / CarPlay Template-/Voice-Constraints einhalten (kein freies visuelles UI waehrend der Fahrt, Audio-Focus/MediaSession korrekt); on-device-STT senkt Latenz + funktioniert bei Funkloch, Cloud-STT bei Bedarf.
  - **Erreichbarkeit:** WireGuard-Client-Doze/Zombie-State (Almanach §3.3/§4.5) beachten — Reconnect bei Netzwechsel aktiv ausloesen, sonst erste Sprachnotiz nach Standby verloren.

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei) | Bug-Gegenpart in `bugs/server/client-anbindung.md` |
|---|---|
| §2 Idempotente Schreib-Calls | §6.1 POST-Retry → Doppel-Speicherung |
| §3 Token-Storage (Android/iOS/Win) | §5 interne CA/Trust (verwandt), §6.3 Secrets-Querschnitt |
| §4 VPN vs. Reverse-Proxy | §3 WireGuard-Client (Erreichbarkeit), `bugs/server/wireguard.md` (Tunnel) |
| §5 Offline-First/Outbox | §6.1 Idempotenz, §6.2 stale connection |
| §6 Timeouts/Backoff/Connection | §6.2 stale connection, §7.3 .NET Socket/DNS, §6.4 Reverse-Proxy 413/60s |
| §7 Cert/CA/Pinning | §5.1 Trust-Anchor, §5.2 IP-SAN, §5.4 Pinning-Renewal |
| §8 Hintergrund-Sync | §4 Doze/FGS/Hersteller-Killer |
| §9 Voice-Pipeline | §1/§2 Cleartext/ATS, §3 WireGuard-Client, §6.1 Outbox |
| §1 API-Contract | §6.3 CORS (Web-Client), §6.4 Body-Limit |

---

## Pflicht-Checkliste (vor der Anbindung)

```
□ OpenAPI-Contract-First, /v1, RFC 9457, Cursor-Pagination — ein SDK je Plattform? (§1)
□ Idempotency-Key fuer alle Schreib-Calls? (§2)
□ Kurzlebige Tokens + Refresh-Rotation + Least-Privilege? (§3)
□ Token plattform-sicher: Keystore+DataStore/Tink (Android), Keychain+ACL (iOS/macOS), DPAPI/.NET DataProtection (Win)? (§3)
□ Zugriffsweg bewusst gewaehlt (VPN privat / Reverse-Proxy+TLS oeffentlich), Zero-Trust auch hinter VPN? (§4)
□ Lokale DB = Source of Truth + Outbox + Konfliktstrategie? (§5)
□ Getrennte Timeouts + Backoff+Jitter + Retry-After + nur idempotente Retries + Pool-Recycling? (§6)
□ Interne CA (Root offline, kurze TTL, DNS-SAN), Pinning mit Backup + Rotation? (§7)
□ Hintergrund: WorkManager/FCM/FGS (A14-Type) bzw. BGTaskScheduler/Background-URLSession + Resilienz? (§8)
□ Voice: Streaming-Pipeline <500ms, async REST, Outbox bei Netzabriss, kurze TTS, Auto-Sicherheit? (§9)
```
