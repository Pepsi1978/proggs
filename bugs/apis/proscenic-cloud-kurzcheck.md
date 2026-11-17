# Proscenic-Cloud — Bug-Kurzcheck (Stand 2026-11-14)
1. Login "success" trotz falschem Passwort → `equipcount` prüfen.
2. Nur eine Anmeldung je Konto: `code 102` = andere Anmeldung aktiv; für Parallelbetrieb Zweitkonto + „Gerät teilen“.
3. Gateway-Socket nur kurz offen halten (verdrängt die App).
4. App 4.x (`com.proscenic.smart.robot`) = andere Plattform, `getEquips` leer.
5. Offline-Diagnose: `robotOnLine`, `getEquips`, `records` prüfen das Token NICHT (auch `xxx` → code 0); Token-Test nur über `getSockAddr` (ungültig → 102). `-1 failure` bei Befehlen/`cleanRobot/info` = Roboter offline. Im LAN heißt er `m7.lan`; er fällt bei schwachem WLAN minutenweise raus (17.11.2026) – Stabilität über ≥3 min beobachten, nie nach einem Online-Treffer „behoben“ melden.
