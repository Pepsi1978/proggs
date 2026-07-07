# Bekannte Bugs & Fallen: Endgeraet-zu-self-hosted-Server-Anbindung (VPN/REST, Android/iOS/macOS/Windows)

> **PFLICHT-LESEN vor Arbeit an der Anbindung eines Endgeraets an ein selbst gehostetes Backend** —
> brain-api unter `http://10.8.0.1:8000` (WireGuard), spaeter Reverse-Proxy/TLS; geplante Sprach-App im Auto.
> Gilt fuer die **Client-/Geraete-Seite**: Android-Apps, iOS-App, macOS-Overlays (Swift/AppKit),
> Windows-Overlays (C#/.NET) — wie sie den eigenen Server ueber VPN/REST erreichen.
>
> **Stand:** recherchiert am 2026-06-24 (Opus-Schwarm, 7 Researcher; Issue-Status am 2026-06-24 hart per `gh`
> geprueft). **Anker:** Android `minSdk 26-29`/`targetSdk 36`/`compileSdk 36-37` (Cleartext-Block ab API 28) ·
> iOS 14+/18 (ATS + Local Network Privacy) · macOS 15 Sequoia (Local Network Privacy) · .NET (HttpClient) ·
> WireGuard-Client → `10.8.0.1:8000`. Multi-Plattform/Theme-gebunden — kein Live-Anker.
> **Gegenseite (Best Practices, wie man es richtig macht):** [`best-practices/server/client-anbindung.md`](../../best-practices/server/client-anbindung.md) — mit wechselseitiger Bug↔Best-Practice-Bezugs-Tabelle.
>
> **Abgrenzung (wichtig — nicht verwechseln):**
> - `bugs/server/wireguard.md` = **Server-/Tunnel-Seite** (wg0.conf auf dem VPS, AllowedIPs/Keepalive/DNS/Autostart serverseitig).
> - `bugs/android/retrofit-okhttp-moshi.md` = **allgemeiner HTTP-Client** (Retrofit/OkHttp/Moshi, R8-Keep-Regeln, Adapter).
> - **DIESE Datei** = die **Geraet-zu-eigenem-Server-Huerden**: Cleartext/ATS-Block, VPN-Erreichbarkeit AUF dem Geraet,
>   Doze killt Tunnel, interne-CA-Trust, Offline-Queue, CORS. Ueberschneidungen (OkHttp-Timeouts, WireGuard-Keepalive)
>   hier aus dem **Client-Anbindungs-Blickwinkel** + Querverweis.

---

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

---

## TL;DR — die 7 wichtigsten Regeln

1. **Zwei getrennte Plattform-Sperren bei „private IP geht nicht":** Android Cleartext-Block (klare Meldung) bzw.
   iOS ATS (klare Meldung) UND iOS/macOS Local-Network-Permission (STILL, wirkt wie „offline"). Diagnose-Reihenfolge beachten.
2. **`http://10.8.0.x` braucht aktive Freigabe:** Android NSC `cleartextTrafficPermitted` pro IP; iOS/macOS `NSAllowsLocalNetworking=YES`.
3. **Das Geraet erreicht den Server nur bei aktivem Tunnel** — vor jedem Request Status pruefen, Retry statt stillem Crash.
4. **Doze/Standby + Hersteller-Killer toeten Tunnel und Calls** — nie auf passives Polling/Keepalive bauen; FCM-Wake + Foreground-Service.
5. **Interne CA korrekt vertrauen** (NSC `@raw`, iOS Full-Trust) und **IP ins SAN** — Pinning nur MIT Backup-Pin.
6. **Schreib-Requests idempotent** (Idempotency-Key) + Offline-Outbox — sonst Doppel-Speicherung/Verlust bei Tunnel-Abriss.
7. **Stale Connections nach Standby/Reconnect** abfangen (`callTimeout`, `evictAll`, kurze Pool-Lifetime, Retry).

---

## 1. Android — Cleartext-HTTP-Block & Network Security Config

### 1.1 Cleartext blockiert per Default ab targetSdk 28  [⭐ HAEUFIG]
**Symptom:** `java.net.UnknownServiceException: CLEARTEXT communication to 10.8.0.1 not permitted by network security policy` (WebView: `net::ERR_CLEARTEXT_NOT_PERMITTED`). Jeder `http://`-Request auf die private IP scheitert.
**Ursache:** Ab Android 9 / API 28 ist die implizite Default-NSC `base-config cleartextTrafficPermitted="false"`. Betrifft Apps mit `targetSdk >= 28` (hier 36), geraeteunabhaengig.
**Versionen:** per Design ab API 28; <28 war Default `true`. Kein Fix geplant (gewollt).
**FIX (funktionserhaltend):** `res/xml/network_security_config.xml` mit `domain-config cleartextTrafficPermitted="true"` fuer die konkrete IP, im Manifest `android:networkSecurityConfig="@xml/network_security_config"`. HTTP zur privaten IP erlaubt, Rest bleibt HTTPS-only:
```xml
<network-security-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="false">10.8.0.1</domain>
  </domain-config>
</network-security-config>
```
**Quelle:** developer.android.com/privacy-and-security/security-config, github square/okhttp #6196.

### 1.2 Kein CIDR / Subnetz / Wildcard in `<domain>`
**Symptom:** `<domain>10.8.0.0/8</domain>` oder `0.0.0.0/0` matcht nichts, HTTP bleibt blockiert.
**Ursache:** `<domain>` akzeptiert nur exakte Hostnamen oder einzelne IPs — keine CIDR-Notation/Wildcards.
**FIX:** Jede benoetigte IP einzeln als eigenes `<domain>`. Nur wenn wirklich alle Cleartext-Ziele gewollt: bewusst `base-config cleartextTrafficPermitted="true"` (oeffnet alles — sicherheitlich schlechter, Funktion bleibt).
**Quelle:** developer.android.com/privacy-and-security/security-config, devblogs.microsoft.com/xamarin.

### 1.3 `includeSubdomains="true"` bei IP ist sinnlos/Fehlerquelle
**Symptom:** Regel greift nicht wie erwartet; manche Tools werfen „invalid domain".
**Ursache:** `includeSubdomains` ist ein DNS-Konzept — bei einer numerischen IP gibt es keine Subdomains.
**FIX:** Bei IP-Eintraegen immer `includeSubdomains="false"`.
**Quelle:** developer.android.com/privacy-and-security/security-config, TrustKit-Android #49.

### 1.4 `debug-overrides` greift nur bei debuggable=true → Release blockt erneut  [⭐ HAEUFIG]
**Symptom:** HTTP zur IP geht im Debug-Build, im Release-Build/AAB ploetzlich `CLEARTEXT not permitted`.
**Ursache:** `<debug-overrides>` wird nur bei `android:debuggable="true"` ausgewertet; im Release ignoriert.
**FIX:** Produktiv noetige Cleartext-Erlaubnis in `domain-config` eintragen (nicht nur in `debug-overrides`).
**Quelle:** developer.android.com/privacy-and-security/security-config.

### 1.5 `usesCleartextTraffic`-Manifest-Attribut wird von NSC ueberschrieben
**Symptom:** `usesCleartextTraffic="true"` gesetzt, HTTP scheitert trotzdem — oder umgekehrt.
**Ursache:** Ist eine NSC vorhanden, wird das Manifest-Attribut ab API 24 ignoriert (NSC hat Vorrang); ab Ziel-API 38 ganz deprecatet.
**FIX:** Entweder ganz auf NSC setzen (empfohlen, granular) ODER nur das Manifest-Attribut ohne NSC — nicht mischen.
**Quelle:** developer.android.com/guide/topics/manifest/application-element, OWASP MASTG-TEST-0235.

### 1.6 NSC greift nicht in jeder HTTP-Library / verschachtelte Configs
**Symptom:** Gleicher `http://10.8.0.x`-Call geht mit einer Library, scheitert mit anderer; oder breite Erlaubnis wird von spezifischerer `domain-config` ueberstimmt.
**Ursache:** OkHttp/Retrofit/Cronet/URLConnection/WebView ehren NSC; **Ktor (reine-Socket-Clients) NICHT zuverlaessig**. Bei mehreren `domain-config` gewinnt die laengste/spezifischste Regel. base-config + IP-domain koennen kollidieren (Flutter #65841 OPEN: Crash/Nicht-Honorierung).
**FIX:** NSC-Eintrag fuer die IP setzen (deckt OkHttp/WebView ab); bei Ktor nicht auf NSC-Schutz verlassen. Regeln entschachteln, eindeutige `domain-config` ohne widersprechende Eltern.
**Quelle:** OWASP MASTG-TEST-0235, github square/okhttp #6196, flutter/flutter #65841 (OPEN).

---

## 2. iOS / macOS — App Transport Security & Local Network Privacy

### 2.1 ATS blockt jeden cleartext-HTTP-Zugriff  [⭐ HAEUFIG]
**Symptom:** `App Transport Security has blocked a cleartext HTTP (http://) resource load since it is insecure.`
**Ursache:** ATS ist standardmaessig aktiv; HTTP ohne TLS verboten.
**Versionen:** iOS 9+, macOS 10.11+.
**FIX (funktionserhaltend, NICHT global):** `Info.plist` → `NSAppTransportSecurity` (Dictionary) → fuer privaten Host entweder `NSAllowsLocalNetworking=YES` (siehe 2.2) ODER `NSExceptionDomains` → `<hostname>` → `NSExceptionAllowsInsecureHTTPLoads=YES`.
**Quelle:** developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity.

### 2.2 `NSAllowsLocalNetworking=YES` ist der korrekte Hebel fuer private IPs  [⭐ HAEUFIG]
**Symptom/Kontext:** HTTP zu privater IP (10.x/172.16-31.x/192.168.x) erlauben, ohne ATS global abzuschalten. Eine nackte IP als `NSExceptionDomains`-Key **matcht NICHT zuverlaessig** (Keys sind fuer Hostnamen gedacht).
**FIX:** `NSAppTransportSecurity` → `NSAllowsLocalNetworking=YES` (deaktiviert ATS gezielt fuer lokales Netz). Alternativ echten Hostnamen vergeben (lokales DNS) und als Exception-Domain eintragen.
**Versionen:** iOS 10+, macOS 10.12+.
**Quelle:** nowsecure.com (ATS-Exceptions-Guide).

### 2.3 `NSAllowsArbitraryLoads=YES` ueberschreibt gelistete Domains NICHT + App-Store-Pflicht
**Symptom:** Trotz `NSAllowsArbitraryLoads=YES` bleibt eine unter `NSExceptionDomains` gelistete Domain ATS-geschuetzt; ausserdem zieht globales Abschalten App-Review-Begruendungspflicht nach sich.
**Ursache:** Sobald eine Domain unter `NSExceptionDomains` steht, gilt fuer SIE die Domain-Konfig; der globale Schalter wird fuer sie ignoriert. `NSAllowsArbitraryLoads` muss bei Apple begruendet werden.
**FIX:** Bei gelisteter Domain IMMER `NSExceptionAllowsInsecureHTTPLoads=YES` explizit setzen. Lieber `NSAllowsLocalNetworking` (gilt nicht als arbitrary loads, review-freundlich) statt global. self-signed/interne CA + ATS: CA ins Trust bringen (siehe §5) ODER pro Domain `NSExceptionRequiresForwardSecrecy=NO`/`NSExceptionMinimumTLSVersion`.
**Quelle:** nowsecure.com, developer.apple.com (NSAppTransportSecurity).

### 2.4 iOS 14+ Local Network Privacy: Verbindung zu privater IP schlaegt STILL fehl  [⭐ HAEUFIG]
**Symptom:** Verbindung zu 10.x haengt/timeout; `NSURLErrorDomain Code=-1009` („Internet connection appears offline") bzw. DNS `PolicyDenied(-65570)`. Wirkt wie „kein Netz", obwohl Server erreichbar.
**Ursache:** Seit iOS 14 braucht JEDE lokale Verbindung (auch direkte IP) die Local-Network-Erlaubnis; ohne erteilte Permission blockt iOS still. Ab iOS 18 erscheint OHNE `NSLocalNetworkUsageDescription` KEIN Prompt. iOS-18-Bug (FB14321888): Permission-State desynchronisiert nach Toggle → Neustart noetig. iOS 17↔18 inkonsistent (17 blockt hart, 18 funktioniert teils trotz Verweigerung).
**FIX:** `Info.plist` → `NSLocalNetworkUsageDescription` = "<Begruendung>" setzen + Zugriff anstossen (Prompt erscheint); ggf. `NSBonjourServices` zum Triggern. Auf echtem Geraet testen (Simulator zeigt teils keinen Prompt). Defensiv: Timeout + `URLSessionConfiguration.waitsForConnectivity=true`.
**Versionen:** iOS/iPadOS 14+, verschaerft 18.
**Quelle:** developer.apple.com/documentation/technotes/tn3179, developer.apple.com/forums/thread/766133, nilcoalescing.com.

### 2.5 VPN/utun-Grenzfall: zaehlt 10.8.0.1 ueber WireGuard als „local network"?  [Luecke — Live-Test noetig]
**Symptom/Faktenlage:** Unklar/inkonsistent, ob die Local-Network-Permission fuer eine NUR ueber den VPN-Tunnel erreichbare private IP gilt.
**Ursache:** TN3179 definiert „local network" als direkt verbundenes Subnetz/link-local/Bonjour. Ueber das utun-Interface GEROUTETER Verkehr ist konzeptionell getunnelt — Apple dokumentiert den VPN-Fall NICHT explizit. Praxis-Indiz: ueber utun geroutetes 10.8.0.1 wird oft NICHT als local network behandelt (kein Prompt); liegt die VPN-IP im direkt verbundenen Subnetz, greift die Permission.
**FIX:** Auf echtem Geraet mit aktivem WireGuard testen; vorsorglich `NSLocalNetworkUsageDescription` setzen (schadet nicht). Bei stillem Fehlschlag erst Local-Network, dann ATS pruefen.
**Quelle:** developer.apple.com/documentation/technotes/tn3179 (Tunnel-Fall nicht dokumentiert) — **ehrliche Luecke, nur per Geraetetest/Apple-DTS klaerbar.**

---

## 3. WireGuard-Client auf dem Geraet (Erreichbarkeit nur bei aktivem Tunnel)

> Server-/Tunnel-Config (wg0.conf auf dem VPS) steht in `bugs/server/wireguard.md`. Hier nur die Geraete-Seite.

### 3.1 Handshake OK, Bytes fliessen, App erreicht 10.8.0.1 trotzdem nicht
**Symptom:** `wg show` zeigt Handshake + Bytes, Ping/App schlagen fehl.
**Ursache:** Client-`AllowedIPs` deckt das Ziel nicht ab (AllowedIPs = Routing-Tabelle + ACL); ODER MTU-Mismatch → stiller Paketverlust.
**FIX:** Client-`AllowedIPs` muss `10.8.0.0/24` (bzw. `10.8.0.1/32`) enthalten; MTU per DF-Ping testen, ggf. auf 1280 senken.
**Quelle:** cr0x.net (handshake-did-not-complete), Manjaro-Forum.

### 3.2 DNS-IP fehlt in Client-`AllowedIPs` → Hostname loest nicht auf (Split-Tunnel-Falle)
**Symptom:** Tunnel verbunden, 10.8.0.1 per IP erreichbar, per Hostname nicht (DNS-Query geht am Tunnel vorbei / Timeout).
**Ursache:** `AllowedIPs = 10.8.0.0/24` routet nur das Dienst-Subnetz; liegt der DNS-Server ausserhalb, wird er lokal versucht.
**FIX:** DNS-Server-IP zu Client-`AllowedIPs` hinzufuegen; `DNS =` im Client-`[Interface]` auf den Tunnel-DNS. (Siehe auch §6.5.)
**Quelle:** cr0x.net (wireguard-dns-not-working), procustodibus.com.

### 3.3 Always-on/Lockdown blockt App wenn Tunnel kurz down  [⭐ HAEUFIG]
**Symptom:** App-Calls schlagen voellig fehl wenn Tunnel nach Wake/Roaming nicht sofort steht; bei „Block connections without VPN" ist das Geraet komplett offline statt nur ungeschuetzt; erst Lockdown-Toggle hilft.
**Ursache:** Always-on + Lockdown blockt JEGLICHEN Traffic solange der Tunnel nicht steht (Deadlock beim ersten Connect nach Doze). Verschaerft durch Android-16-VPN-Bug (netfilter-State divergiert → stille Disconnects).
**FIX:** `PersistentKeepalive=25` im CLIENT-Peer (schnellerer Re-Handshake); Tunnel-Handshake bei `ConnectivityManager.NetworkCallback` aktiv neu ausloesen (nicht nur passives Keepalive); Lockdown bei instabilem Netz aus.
**Versionen:** Android (alle), bes. Android 16.
**Quelle:** github celzero/rethink-app #2105/#1367, Android-16-VPN-Bug (gadgethacks).

### 3.4 iOS on-demand: „verbunden" aber kein Traffic nach WLAN↔Mobilfunk-Wechsel
**Symptom:** Beim Netzwechsel zeigt der Tunnel „verbunden", leitet aber keinen Traffic; 10.8.0.1 unerreichbar bis manueller Reconnect.
**Ursache:** Meist MTU- oder IPv6-Mismatch zwischen den Netzen; teils „Disconnect on demand"; NAT-Endpoint-IP wechselt mid-session.
**FIX:** Client-MTU 1280; „On-Demand" fuer alle Netze aktiv, „Disconnect on demand" AUS; bei mehreren Tunneln On-Demand nur auf EINEM; PersistentKeepalive.
**Quelle:** github trailofbits/algo #1385, forums.lawrencesystems.com.

### 3.5 iOS/macOS Network-Extension 15-MB-Memory-Limit → Tunnel-Crash
**Symptom:** WireGuard-Tunnel wird unter Last (hoher Upload) oder bei sehr vielen Routen vom System gekillt (jetsam); danach kein Traffic.
**Ursache:** iOS Network Extensions haben ~15 MB harte Memory-Grenze; Spikes/grosse Route-Zahl ueberschreiten sie (Crash beim Erhoehen 1024→2048 Routen).
**FIX:** Routenzahl niedrig halten; fuer den Dienst-Zugang `10.8.0.0/24` statt Default-Route 0.0.0.0/0 mit vielen Excludes (spart Memory); keine riesigen Peer-Listen auf iOS.
**Quelle:** developer.apple.com/forums/thread/723156, SagerNet/sing-box #3976.

---

## 4. Hintergrund / Doze / Standby killt Calls + Tunnel

### 4.1 Doze setzt Netzwerkzugriff im Hintergrund aus  [⭐ HAEUFIG]
**Symptom:** Hintergrund-Request haengt/timeout bis Screen-on; Notification verspaetet.
**Ursache:** Doze (ab API 23) suspendiert Netzzugriff, ignoriert Wakelocks, deferred Alarme, blockt JobScheduler/WorkManager. Aktiv wenn: unplugged, stationaer, Screen off, idle.
**FIX (funktionserhaltend):** Daten per High-Priority-FCM-Push (weckt App temporaer + Netz + Wakelock). Echte Dauerverbindung → Foreground-Service mit Notification. Periodisches Polling via WorkManager (laeuft im Maintenance-Window).
**Quelle:** developer.android.com/training/monitoring-device-state/doze-standby.

### 4.2 App-eigene Alarme/Keepalive feuern nicht im Doze
**Symptom:** Reconnect-/Keepalive-Timer feuert im Standby gar nicht; Verbindung erst nach Wake.
**Ursache:** `setExact()`/`setWindow()` werden verschoben; nur `setAndAllowWhileIdle()` feuert im Doze — max. 1×/9 min/App. `setAlarmClock()` weckt zuverlaessig.
**FIX:** Reconnect auf `setAndAllowWhileIdle()` (9-min-Grenze einplanen); zeitkritisch `setAlarmClock()`. Besser: High-Priority-FCM als Wake-Trigger.
**Quelle:** developer.android.com/.../doze-standby.

### 4.3 Android 14+: Foreground-Service crasht ohne korrekten Type
**Symptom:** `MissingForegroundServiceTypeException` / `foregroundServiceType ... is not a subset` — Dauerverbindungs-Service startet nicht.
**Ursache:** Ab API 34 muss jeder FGS einen Typ im Manifest deklarieren (+ Permission). Ab API 35 hat `dataSync` max. 6 h Laufzeit.
**FIX:** Korrekten `foregroundServiceType` (z.B. `dataSync`/`connectedDevice`) im Manifest UND Code + Permission; `dataSync` nicht als Dauerlaeufer (>6 h via Reschedule/FCM).
**Quelle:** developer.android.com/about/versions/14/changes/fgs-types-required.

### 4.4 Hersteller-Killer (Samsung/Xiaomi/Huawei) toeten App + VPN  [⭐ HAEUFIG]
**Symptom:** App/VPN nach Nichtnutzung gekillt, keine Reconnects/Notifications — teils selbst mit deaktivierter System-Battery-Optimization.
**Ursache:** Samsung „Put unused apps to sleep"/Deep-Sleeping (Default 3 Tage); Xiaomi MIUI Autostart + reaktiviert Battery-Optimization nach App-Update selbst; Huawei PowerGenie killt nicht-gewhitelistete Apps.
**FIX (User-Anleitung, dontkillmyapp.com):** Samsung: App in „Apps that won't be put to sleep", Adaptive Battery aus. Xiaomi: Autostart an, Battery-Saver „No restrictions", nach Updates erneut. Huawei: „Protected Apps". Plus `ignoreBatteryOptimizations` (ACHTUNG: befreit NICHT voll — Alarme bleiben deferred; Play-Store erlaubt den direkten Request nur bei echtem Zwang).
**Quelle:** dontkillmyapp.com/samsung, protonvpn.com/support/android-killing-proton-vpn, vpnhouse.net (Huawei).

### 4.5 WireGuard-Zombie-State nach Doze/Netzwechsel
**Symptom:** Nach langem Sperren „connected", aber kein Traffic; erst Off/On hilft; Verbindung bricht nachts ab.
**Ursache:** Doze unterbricht Keepalive; nach Wake/Netzwechsel resynct die Routing-/Socket-Schicht den internen State nicht mit dem neuen Socket → Pakete ignoriert, DNS-Timeout.
**FIX:** `PersistentKeepalive=25`, App „Unrestricted"; bei `CONNECTIVITY`/Netzwechsel-Event automatischen Re-Bind/Tunnel-Restart triggern statt nur Keepalive.
**Quelle:** discuss.grapheneos.org (terminates at night), github celzero/rethink-app #2602.

### 4.6 iOS: App suspendiert → Netz-Call bricht ab; erster Request nach Wake failt
**Symptom:** Request bricht ab/verloren wenn App in den Hintergrund geht; direkt nach Wake schlaegt der erste Request fehl, Sekunden spaeter geht es.
**Ursache:** iOS suspendiert die App; normale `URLSession`-Tasks werden unterbrochen. Default `waitsForConnectivity=false` → Foreground-Session failt sofort statt zu warten.
**FIX:** `URLSession.background(withIdentifier:)` fuer Hintergrund-Transfers (System setzt fort/relauncht); `URLSessionConfiguration.waitsForConnectivity=true`; `timeoutIntervalForResource` ausreichend; statt NWPathMonitor-Preflight direkt requesten.
**Quelle:** developer.apple.com/forums/thread/97950, useyourloaf.com (urlsession-waiting-for-connectivity).

---

## 5. Self-Signed / interne CA & Certificate Pinning

### 5.1 Android: user-installierte CA wird ab targetSdk 24/28 nicht mehr vertraut  [⭐ HAEUFIG]
**Symptom:** `SSLHandshakeException: ... CertPathValidatorException: Trust anchor for certification path not found` — obwohl die CA im Geraet installiert ist.
**Ursache:** Apps mit `targetSdk >= 24` vertrauen user-/admin-installierten CAs nicht mehr (nur System-Trust); ab API 28 Default ohne Ausweg. Default-base-config: API 24-27 implizit `src="user"`, API 28+ nur `src="system"` (Migrations-Falle beim targetSdk-Heben).
**FIX (funktionserhaltend):** Eigene CA als PEM/DER in `res/raw/` buendeln + NSC:
```xml
<domain-config>
  <domain includeSubdomains="false">10.8.0.1</domain>
  <trust-anchors>
    <certificates src="system" />
    <certificates src="@raw/myca" />
  </trust-anchors>
</domain-config>
```
`src="user"` nur fuer Debug. Trust deklarativ verankern (nicht nur in-memory im SSLContext — geht beim App-Kill verloren).
**Versionen:** Android 7.0+ (API 24+), Default ab API 28.
**Quelle:** developer.android.com/privacy-and-security/security-config, android-developers.googleblog.com (2016 Nougat CA-Changes).

### 5.2 TLS auf eine IP-Adresse: Zertifikat braucht IP im SAN (CN reicht nicht)  [⭐ HAEUFIG]
**Symptom:** `ERR_TLS_CERT_ALTNAME_INVALID: Hostname/IP does not match certificate's altnames` bzw. Hostname-Mismatch-Handshake-Fehler bei `https://10.8.0.1`.
**Ursache:** Moderne TLS-Clients pruefen NUR das **SAN**-Feld, nicht den CN. Cert mit nur CN=10.8.0.1 wird abgelehnt; IP muss als `IP Address`-SAN-Eintrag vorhanden sein.
**FIX (funktionserhaltend):** Cert mit IP-SAN ausstellen (OpenSSL `subjectAltName = IP:10.8.0.1`). **Robuster:** DNS-Namen verwenden (`DNS:mein-server.lan`) + Split-Horizon-DNS auf die VPN-IP — ueberlebt IP-Wechsel.
**Quelle:** community.letsencrypt.org (hostname/ip does not match altnames), agirlamonggeeks.com.

### 5.3 Let's Encrypt geht NICHT fuer private/interne IP
**Symptom:** ACME-Ausstellung fuer interne Domain/private IP schlaegt fehl.
**Ursache:** LE braucht Kontroll-Nachweis ueber eine oeffentliche Domain; seit 2025-07-01 gibt es zwar IP-Zertifikate, aber nur fuer **oeffentliche** IPs (kurzlebig). RFC-1918 (10.x) ausgeschlossen.
**FIX:** Eigene interne CA betreiben (mkcert/step-ca/OpenSSL) + Root-CA auf Clients vertrauen (§5.1/5.5). Alternativ echte Domain + DNS-01 + Split-Horizon-DNS auf die VPN-IP.
**Quelle:** letsencrypt.org/2025/07/01 (first IP cert), community.letsencrypt.org.

### 5.4 Certificate Pinning bricht beim Cert-Renewal → App tot bis Update  [⭐ HAEUFIG]
**Symptom:** Nach Zertifikatswechsel `SSLPeerUnverifiedException: Certificate pinning failure!` — App fuer ALLE Nutzer tot, bis ein Update kommt.
**Ursache:** Gepinnter SPKI-Hash veraltet, wenn das neue Cert einen neuen Key hat. Ohne Backup-Pin kein Ausweg. Android NSC `<pin-set>` ohne `expiration`/Backup erzwingt das alte Cert dauerhaft; `overridePins`/custom-trust-anchor hebeln Pinning still aus.
**FIX (funktionserhaltend — NIE Pinning ersatzlos abschalten):** IMMER Backup-Pin (zweiter SPKI-Hash). Bei eigener interner CA aufs **CA-Cert** pinnen statt aufs Leaf (ueberlebt Leaf-Renewals). Beim Renewal Key wiederverwenden ODER Reserve-Key rechtzeitig aktivieren.
```kotlin
CertificatePinner.Builder()
  .add("mein-server.lan", "sha256/AAAA…")  // aktuell
  .add("mein-server.lan", "sha256/BBBB…")  // Backup-Pin (Pflicht!)
  .build()
```
**Quelle:** square.github.io/okhttp (CertificatePinner), approov.io/blog/the-problem-with-pinning, developer.android.com/privacy-and-security/security-config.

### 5.5 iOS/macOS: CA-Profil installiert, aber nicht fuer TLS vertraut
**Symptom:** Root-CA-Profil installiert, `URLSession`/Safari brechen mit `NSURLErrorServerCertificateUntrusted (-1202)` ab.
**Ursache:** Manuell installierte CA-Profile sind NICHT automatisch fuer SSL/TLS vertraut — der „Full Trust"-Schalter fehlt.
**FIX:** Einstellungen → Allgemein → Info → **Zertifikatsvertrauenseinstellungen** → „Vollstaendiges Vertrauen fuer Stammzertifikate aktivieren" fuer die eigene CA. Flotten: per MDM/Apple Configurator (dann automatisch SSL-vertraut). Server-Trust im `URLSessionDelegate` bewusst evaluieren (`SecTrustEvaluateWithError`), nicht blind `.useCredential`.
**Quelle:** support.apple.com/en-us/102390, developer.apple.com/forums/thread/124056.

### 5.6 .NET/Windows: self-signed Cert → AuthenticationException; blindes Deaktivieren = MITM-offen
**Symptom:** `AuthenticationException: The remote certificate is invalid ...` (auch `PartialChain`).
**Ursache:** Interne CA nicht im Windows-Trust-Store, oder Intermediate-Kette fehlt. Bekannte .NET-Falle: `ServerCertificateCustomValidationCallback` wird NICHT aufgerufen, wenn ein Client-Cert genutzt wird UND der Server ein self-signed Cert praesentiert (dotnet/runtime #75595, CLOSED COMPLETED).
**FIX (funktionserhaltend — NICHT global akzeptieren):** Interne Root-CA in den Windows-Trust-Store (LocalMachine\Root) importieren. App-lokal: Callback so, dass NUR der bekannte Thumbprint/Root akzeptiert wird (Pinning-artig), bei `PartialChain` Intermediates in `chain.ChainPolicy.ExtraStore` + `chain.Build()`. NIE `DangerousAcceptAnyServerCertificateValidator`.
**Quelle:** learn.microsoft.com (remote certificate invalid), github dotnet/runtime #75595.

---

## 6. Client-Resilienz, CORS, grosse Requests

### 6.1 POST-Retry nach Tunnel-Abriss erzeugt Duplikate  [⭐ HAEUFIG]
**Symptom:** Nach kurzem Tunnel-Abriss wird ein POST wiederholt; Server fuehrt ihn ZWEIMAL aus (doppelte Eintraege im Gehirn). Auf Mobilfunk failen 5-10 % der Requests (>20 % in schwachen Netzen) — der Client weiss nicht, ob der Server ihn erhielt.
**Ursache:** Nicht-idempotenter POST + blinder Retry; der erste Request kam an, nur die Antwort ging im Abriss verloren.
**FIX (funktionserhaltend):** Client schickt `Idempotency-Key` (UUID, EIN Key pro Nutzer-Intent/Tap — NICHT pro HTTP-Versuch), VOR dem ersten Versuch persistiert (ueberlebt App-Neustart). Server speichert Key+Ergebnis in derselben DB-Transaktion und gibt bei Wiederholung das gespeicherte Ergebnis zurueck. Key nach ~24 h ablaufen. Verlorene Writes: **Outbox-Pattern** (Event + Outbox-Record in EINER lokalen Transaktion, Worker sendet bei Netz-Verfuegbarkeit mit Idempotency-Key).
**Quelle:** mvpfactory.io (idempotent APIs for mobile), docs.stripe.com/api/idempotent_requests, backendbytes.com (outbox).

### 6.2 Stale Connection nach Standby/VPN-Reconnect  [⭐ HAEUFIG]
**Symptom:** `unexpected end of stream` / `EOFException` / `SocketTimeoutException` / ECONNRESET beim ERSTEN Request nach Standby/Idle/Reconnect; Folge-Request geht. OkHttp scheitert dauerhaft nach WLAN→Mobilfunk-Wechsel (toter Pool).
**Ursache:** OkHttp behandelt eine <10 s idle Verbindung als „healthy" (`IDLE_CONNECTION_HEALTHY`), auch wenn Server/Tunnel sie laengst per FIN/RST geschlossen haben; nach Netzwechsel haelt der Pool tote Routen. .NET: Pool haelt toten Socket nach Sleep/VPN-Toggle.
**FIX (funktionserhaltend):** OkHttp `retryOnConnectionFailure(true)` + `callTimeout(...)` (begrenzt den GESAMTEN Call; Default 0 = unendlich) + bei `NetworkCallback`-Netzwechsel `connectionPool().evictAll()`. .NET: `SocketsHttpHandler.PooledConnectionLifetime`/`PooledConnectionIdleTimeout` kurz (1-2 min) + Retry. iOS: `waitsForConnectivity=true`. Requests idempotent halten (§6.1).
**Versionen:** OkHttp-Issues #7007/#8466/#7045/#2328/#4789/#5170/#1747 alle CLOSED COMPLETED — Verhalten dokumentiert, Workaround bleibt aktiv.
**Quelle:** github square/okhttp #7007/#8466/#2328, baeldung.com/okhttp-timeouts.

### 6.3 CORS bei Web-Client gegen self-hosted REST  [⭐ HAEUFIG]
**Symptom:** `No 'Access-Control-Allow-Origin' header is present`; oder GET geht, POST/PUT mit `Authorization`/JSON blockt (fehlschlagende OPTIONS davor); oder Credentials + Wildcard-Origin wird still verworfen.
**Ursache:** Server liefert keine/falsche CORS-Header; Middleware fehlt/falsch eingehaengt; Browser verbietet `Access-Control-Allow-Origin: *` zusammen mit Credentials; Preflight OPTIONS unbeantwortet.
**FIX:** FastAPI `CORSMiddleware` als ERSTE Middleware mit expliziter `allow_origins`-Liste; bei `allow_credentials=True` KEIN `*` (Origins/Methods/Headers explizit). `allow_methods`/`allow_headers` passend; sicherstellen, dass OPTIONS am Proxy nicht zum Backend-Auth umgeleitet wird.
**Quelle:** fastapi.tiangolo.com/tutorial/cors, stackhawk.com (FastAPI CORS), github fastapi/fastapi discussions #7319.

### 6.4 Reverse-Proxy: 413 / Abbruch nach ~60 s  [⭐ HAEUFIG bei TLS-Phase]
**Symptom:** `413 Request Entity Too Large` (Backend sieht den Request NIE); oder langer/langsamer Upload bricht nach ~60 s mit 502/504 ab — verschaerft auf Mobilfunk.
**Ursache:** nginx `client_max_body_size` Default 1 MB; `proxy_read_timeout`/`proxy_send_timeout` Default 60 s.
**FIX:** `client_max_body_size` im passenden Kontext erhoehen; fuer Streaming `proxy_request_buffering off` (ABER: dann kein Upstream-Retry des Bodys); `proxy_read_timeout`/`proxy_send_timeout`/`client_body_timeout` erhoehen; client-seitig chunked + langer writeTimeout.
**Quelle:** nginx-Doku/getpagespeed.com (client_max_body_size), netdata.cloud (nginx 413).

### 6.5 DNS/IPv6/Captive-Portal ueber VPN
**Symptom:** Hostname loest ueber den Tunnel nicht auf (per IP geht's); Verbindung geht via IPv6 am Tunnel vorbei; in fremdem WLAN verbindet WireGuard nicht, bis das Captive-Portal durchlaufen ist.
**Ursache:** `DNS =` greift nicht / falscher Resolver; `AllowedIPs` deckt nur `0.0.0.0/0` ab, nicht `::/0` (IPv6-Leak); Always-on + „Block connections without VPN" blockt genau den HTTP-Zugriff zum Portal-Login.
**FIX:** `DNS =` im Client korrekt setzen; `AllowedIPs` um `::/0` erweitern ODER IPv6 deaktivieren; vor Tunnelaufbau Captive-Portal durchlaufen (Always-on kurz aus), danach `evictAll`.
**Quelle:** procustodibus.com (wireguard-dns), github pivpn/pivpn #1061.

---

## 7. Desktop-Clients (Windows .NET / macOS)

### 7.1 macOS sandboxed App: ohne `com.apple.security.network.client` keine Outbound-Verbindung  [⭐ HAEUFIG]
**Symptom:** Jede `URLSession`/Outbound-Verbindung schlaegt still fehl; „Host name resolution failed" — noch vor ATS; `sandboxd`-Violation in der Konsole.
**Ursache:** App Sandbox verbietet Outbound ohne das Entitlement.
**FIX:** `.entitlements`: `com.apple.security.network.client = true` (Xcode: App Sandbox → „Outgoing Connections (Client)"). ATS (§2) und ab Sequoia Local-Network (§7.2) gelten zusaetzlich.
**Quelle:** developer.apple.com/.../com.apple.security.network.client, developer.apple.com/forums/thread/744961.

### 7.2 macOS Sequoia 15: Local Network Privacy auch auf dem Mac
**Symptom:** macOS-App (auch AppKit) bekommt Local-Network-Prompt; lokale Verbindung blockiert; Grant ueberlebt Power-Cycle nicht; App-Rename/Bundle-ID-Aenderung invalidiert Grant STILL.
**Ursache:** Local Network Privacy mit macOS 15 auf den Mac gebracht (Packet-Filter, kein TCC-Eintrag).
**FIX:** `NSLocalNetworkUsageDescription` auch in der macOS-Info.plist; stabile Bundle-ID; bei „stuck" `tccutil reset` + alte Build-Kopien loeschen. Auf Sequoia testen.
**Quelle:** developer.apple.com/.../tn3179, mjtsai.com (local-network-privacy-on-sequoia), eclecticlight.co.

### 7.3 .NET HttpClient: Socket-Exhaustion + DNS-stale + 100s-Timeout
**Symptom:** `SocketException: Only one usage of each socket address ...` (Port-Exhaustion); ODER Client verbindet zur alten IP nach Server-Umzug; ODER `TaskCanceledException: ... Timeout of 100 seconds`.
**Ursache:** `new HttpClient()` pro Request laesst TCP-Ports im TIME_WAIT haengen; static HttpClient loest DNS nur beim Connect auf (ignoriert TTL); Default-Timeout 100 s, als `TaskCanceledException` geworfen (nicht von Cancellation unterscheidbar ohne Token-Check).
**FIX:** Singleton-`HttpClient`/`IHttpClientFactory` (nie pro Request neu); `SocketsHttpHandler.PooledConnectionLifetime = 2-15 min` (DNS-Refresh beim Recycling); `client.Timeout` passend; im `catch` `CancellationToken.IsCancellationRequested` pruefen.
```csharp
var handler = new SocketsHttpHandler { PooledConnectionLifetime = TimeSpan.FromMinutes(2) };
var client = new HttpClient(handler);
```
**Quelle:** learn.microsoft.com/.../httpclient-guidelines, meziantou.net (avoid DNS issues), github dotnet/runtime #21965.

### 7.4 Windows: Firewall / System-Proxy / VPN-Erreichbarkeit
**Symptom:** `No connection could be made because the target machine actively refused it` / Timeout — sporadisch je VPN-Status; oder Requests gehen an einen Firmen-Proxy ins Leere.
**Ursache:** Defender-Firewall blockt ausgehend; `10.8.0.1` nur bei aktivem WireGuard-Tunnel erreichbar; `HttpClient` nutzt ohne Konfig den System-Proxy (RFC1918-VPN-IP zaehlt NICHT automatisch als „lokal" fuer Bypass).
**FIX:** Outbound-Firewall-Regel; vor Requests Tunnel-Status pruefen (Tunnel-Interface) + verstaendliche Meldung + Retry; `HttpClientHandler.UseProxy=false` ODER `WebProxy` mit Bypass fuer die VPN-IP.
**Quelle:** learn.microsoft.com Q&A (disable system proxy), socketlabs.com (socket-address-error).

### 7.5 Plattformuebergreifend: keine Retry-Logik bei VPN-Reconnect
**Symptom:** Ein einzelner Netzwerk-Glitch (VPN-Reconnect, kurzer Tunnel-Drop) → harter Fehler in der UI statt transparenter Wiederholung.
**FIX:** Resilience-Pipeline mit Exponential-Backoff-Retry fuer transiente HTTP-/Socket-Fehler (.NET: `Microsoft.Extensions.Http.Resilience`/Polly; Swift: Retry-Wrapper um URLSession). Idempotente Requests automatisch wiederholen (§6.1).
**Quelle:** learn.microsoft.com/.../httpclient-guidelines (Resilience).

---

## Fix-Status (hart per `gh` am 2026-06-24)

| Bug | Issue | Status (gh) | Bedeutung |
|-----|-------|-------------|-----------|
| OkHttp stale connection (IDLE_CONNECTION_HEALTHY) | square/okhttp #7007 | CLOSED **COMPLETED** | Verhalten dokumentiert — Workaround (`callTimeout`/`evictAll`) bleibt aktiv |
| OkHttp „unexpected end of stream" | square/okhttp #8466 | CLOSED **COMPLETED** | dito |
| OkHttp FIN,ACK nicht geschlossen | square/okhttp #7045 | CLOSED **COMPLETED** | dito |
| OkHttp keine Exception bei WiFi-down | square/okhttp #2328 | CLOSED **COMPLETED** | `callTimeout` bleibt Pflicht |
| OkHttp Netzwechsel wifi→cellular | square/okhttp #4789, #5170 | CLOSED **COMPLETED** | `evictAll` bleibt aktiv |
| OkHttp erster Request nach VPN verzoegert | square/okhttp #1747 | CLOSED **COMPLETED** | dito |
| .NET Callback nicht bei Client-Cert+self-signed | dotnet/runtime #75595 | CLOSED **COMPLETED** | in neuerer .NET-Version adressiert |
| .NET Timeout=TaskCanceledException | dotnet/runtime #21965 | CLOSED **COMPLETED** | by-design + klarere Message |

### Noch NICHT gefixt / per Design (Workaround bleibt aktiv)

- **Android Cleartext-Block (API 28+), iOS ATS, iOS/macOS Local Network Privacy, Doze, Hersteller-Killer** — **per Design**, kein Fix; NSC/ATS-Keys/FCM/Battery-Ausnahme sind die dauerhaften Loesungen.
- **flutter/flutter #65841 — OPEN** (NSC base-config + IP-domain Kollision).
- **TLS-auf-IP ohne SAN, Let's-Encrypt-nicht-fuer-private-IP, Pinning-Renewal-Bruch** — per Design/TLS-Spec; eigene CA + IP-SAN/DNS-SAN + Backup-Pin bleiben Pflicht.
- **iOS-18 Local-Network-State-Desync** — System-Bug (FB14321888), Neustart als Workaround.

**Ehrlichkeits-Hinweis:** „gefixt" nur wo `gh` CLOSED COMPLETED zeigt. Die meisten Eintraege hier sind
**per-Design-Plattformverhalten** (kein Issue) — der „Fix" ist die korrekte Konfiguration. **Offene Luecke:**
ob 10.8.0.1 ueber WireGuard als iOS/macOS „local network" zaehlt (§2.5), ist offiziell nicht dokumentiert → Live-Test.

---

## Pflicht-Checkliste vor der Anbindung eines Geraets an den eigenen Server

```
□ Android: NSC fuer http://10.8.0.x (domain-config cleartextTrafficPermitted, Manifest-Verweis, Release nicht nur debug-overrides)? (§1)
□ iOS/macOS: NSAllowsLocalNetworking=YES (nicht global ArbitraryLoads) + NSLocalNetworkUsageDescription gesetzt? (§2)
□ Diagnose-Reihenfolge bei „IP geht nicht": erst Local-Network-Permission (still), dann ATS/Cleartext? (§2.4)
□ Client-AllowedIPs deckt 10.8.0.0/24 + DNS-IP ab; PersistentKeepalive=25 im Client-Peer? (§3)
□ Reconnect bei NetworkCallback aktiv ausgeloest (nicht nur passives Keepalive)? (§3.3/§4.5)
□ Doze: FCM-Wake + Foreground-Service (korrekter Type ab A14) statt Hintergrund-Polling? (§4)
□ Hersteller-Killer (Samsung/Xiaomi/Huawei) per User-Anleitung + Battery-Ausnahme adressiert? (§4.4)
□ Interne CA korrekt vertraut (Android @raw-NSC, iOS Full-Trust, .NET/Windows-Store) + IP im SAN? (§5)
□ Certificate-Pinning nur MIT Backup-Pin / aufs CA-Cert gepinnt? (§5.4)
□ Schreib-Tools idempotent (Idempotency-Key) + Offline-Outbox? (§6.1)
□ Stale-Connection abgefangen (callTimeout/evictAll/PooledConnectionLifetime/waitsForConnectivity + Retry)? (§6.2)
□ Web-Client: CORSMiddleware (erste Middleware, kein Wildcard bei Credentials)? (§6.3)
□ Reverse-Proxy: client_max_body_size + proxy_read_timeout erhoeht? (§6.4)
□ macOS: com.apple.security.network.client Entitlement + Sequoia-Local-Network? (§7.1/§7.2)
□ .NET: Singleton-HttpClient/IHttpClientFactory + PooledConnectionLifetime? (§7.3)
```

---

## Bezugs-Tabelle (Abgrenzung zu verwandten Almanachen)

| Thema | DIESE Datei (Client-Anbindung) | Verwandter Almanach |
|-------|--------------------------------|---------------------|
| WireGuard AllowedIPs/Keepalive/DNS | §3 (CLIENT-/Geraete-Seite) | `server/wireguard.md` (Server-/Tunnel-Seite, wg0.conf auf VPS) |
| OkHttp Timeouts/Pinning/Adapter | §5.4/§6.2 (Anbindungs-Blickwinkel) | `android/retrofit-okhttp-moshi.md` (allgemeiner HTTP-Client, R8) |
| CORS/Body-Limit am Backend | §6.3/§6.4 (Client-Sicht) | `server/fastapi.md` (Server-Sicht, falls vorhanden) |
| Server-Infra/Cap/Trifecta | — | `server/self-hosted-ai-agent-server.md` |
| Doze/Foreground-Service-Runtime | §4 (Netz-Fokus) | `android/android-platform.md` (allg. Lifecycle/FGS) |
