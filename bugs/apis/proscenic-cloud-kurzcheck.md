# Proscenic-Cloud — Bug-Kurzcheck (Stand 2026-11-14)
1. Login "success" trotz falschem Passwort → `equipcount` prüfen.
2. Nur eine Anmeldung je Konto: `code 102` = andere Anmeldung aktiv; für Parallelbetrieb Zweitkonto + „Gerät teilen“.
3. Gateway-Socket nur kurz offen halten (verdrängt die App).
4. App 4.x (`com.proscenic.smart.robot`) = andere Plattform, `getEquips` leer.
