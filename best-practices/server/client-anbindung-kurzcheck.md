# Endgeraet-zu-self-hosted-Server-Anbindung Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
