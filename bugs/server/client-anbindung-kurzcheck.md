# Endgeraet-zu-self-hosted-Server-Anbindung (VPN/REST, Android/iOS/macOS/Windows) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`).
> Volltext = Pflicht bei JEDEM Fehler in diesem Bereich (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ Android: `CLEARTEXT communication to 10.8.0.1 not permitted` (ab targetSdk 28) | Network Security Config: `domain-config cleartextTrafficPermitted="true"` mit `<domain>10.8.0.1</domain>` + Manifest-Verweis | §1.1 |
| 2 | Android NSC: IP-Regel greift nicht | KEIN CIDR/Wildcard in `<domain>` (jede IP einzeln); `includeSubdomains="false"` bei IP; Cleartext-Erlaubnis in `domain-config` (NICHT nur `debug-overrides`, sonst Release blockt) | §1.2-1.4 |
| 3 | ⭐ iOS/macOS: `App Transport Security has blocked a cleartext HTTP` | `NSAllowsLocalNetworking=YES` (korrekter Hebel fuer private IPs), NICHT global `NSAllowsArbitraryLoads`. IP als Exception-Domain matcht NICHT zuverlaessig | §2.1-2.3 |
| 4 | ⭐ iOS 14+: Verbindung zu 10.x schlaegt STILL fehl (-1009 / PolicyDenied) | `NSLocalNetworkUsageDescription` setzen (ab iOS 18 sonst KEIN Prompt) + Zugriff anstossen. Zwei getrennte Sperren: erst Local-Network, dann ATS | §2.4 |
| 5 | ⭐ Geraet erreicht 10.8.0.1 NUR bei aktivem Tunnel | Vor Request Tunnel-Status pruefen + Retry/verstaendliche Meldung; Client-`AllowedIPs` muss 10.8.0.0/24 enthalten | §3.1-3.2 |
| 6 | Android Always-on/Lockdown blockt App wenn Tunnel kurz down | `PersistentKeepalive=25` im CLIENT-Peer; Reconnect bei `NetworkCallback` aktiv ausloesen; Lockdown bei instabilem Netz aus | §3.3 |
| 7 | ⭐ Tunnel/Calls sterben im Standby (Doze), erste Anfrage nach Wake haengt | High-Priority-FCM als Wake-Trigger + Foreground-Service (korrekter `foregroundServiceType` ab A14); Battery-Optimization-Ausnahme | §4.1-4.3 |
| 8 | Samsung/Xiaomi/Huawei killen App+VPN trotz System-Einstellung | Hersteller-Killer (dontkillmyapp): App in „nicht schlafen"/Autostart/Protected Apps; nach App-Update erneut pruefen | §4.4 |
| 9 | ⭐ Android: interne CA „Trust anchor ... not found" (ab API 24/28) | CA in `res/raw/` + NSC `<trust-anchors><certificates src="@raw/myca"/>`; NIE auf user-store/Default verlassen | §5.1 |
| 10 | ⭐ TLS auf IP: `Hostname/IP does not match certificate's altnames` | Cert braucht **IP im SAN** (CN reicht nicht); besser DNS-Name + Split-Horizon-DNS auf die VPN-IP. Let's Encrypt geht NICHT fuer private IP | §5.2-5.3 |
| 11 | ⭐ Certificate-Pinning bricht bei Cert-Wechsel → App tot bis Update | IMMER Backup-Pin; bei eigener CA aufs CA-Cert pinnen (ueberlebt Leaf-Renewal); NIE Pinning ersatzlos abschalten | §5.4 |
| 12 | iOS: CA-Profil installiert, trotzdem nicht vertraut | „Vollstaendiges Vertrauen fuer Stammzertifikate aktivieren" (Zertifikatsvertrauenseinstellungen) | §5.5 |
| 13 | ⭐ POST-Retry nach Tunnel-Abriss → Doppel-Speicherung | `Idempotency-Key` (UUID pro Nutzer-Intent, vor 1. Versuch persistiert); Server dedupliziert. Offline-Outbox fuer verlorene Writes | §6.1 |
| 14 | Erster Request nach Standby/VPN-Reconnect: ECONNRESET/„unexpected end of stream" | `callTimeout`/`PooledConnectionLifetime` kurz; `evictAll()` bei Netzwechsel; `retryOnConnectionFailure(true)`; `waitsForConnectivity=true` | §6.2 |
| 15 | Web-Client: `No 'Access-Control-Allow-Origin'` / Preflight failt | `CORSMiddleware` als ERSTE Middleware, explizite `allow_origins`; bei `allow_credentials=True` KEIN Wildcard `*` | §6.3 |
| 16 | nginx vor Backend: 413 / Abbruch nach ~60s | `client_max_body_size` (Default 1 MB) + `proxy_read_timeout` (Default 60s) erhoehen; chunked + langer writeTimeout | §6.4 |
| 17 | macOS sandboxed App erreicht Netz gar nicht | Entitlement `com.apple.security.network.client=YES` (Outgoing Connections); Sequoia 15: zusaetzlich Local-Network-Permission | §7.1-7.2 |
| 18 | .NET: `SocketException Only one usage of each socket address` | Singleton-`HttpClient`/`IHttpClientFactory` (nie pro Request neu); `SocketsHttpHandler.PooledConnectionLifetime` gegen DNS-stale | §7.3 |
