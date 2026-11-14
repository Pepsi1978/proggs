# Proscenic-Cloud (M7 Pro) — Bug-Almanach (Stand 2026-11-14)

> Gegenstück zu `best-practices/apis/proscenic-cloud.md`. Versions-Anker: Proscenic-App 1.5.5, M7 Pro `811_LDS`, Jarvis 1.8.676.

## 1. Falsches Passwort meldet trotzdem Erfolg
- **Symptom:** `/user/login` antwortet `{"code":0,"msg":"success"}`, später scheitert alles.
- **Ursache:** Bei falschen Daten kommt `equipcount 0` und kein brauchbarer Token.
- **Fix:** Erfolg an `data.token` und `equipcount` >= 1 prüfen.
- **Quelle:** https://community.home-assistant.io/t/proscenic-m7-pro/241732

## 2. Nur eine aktive Anmeldung je Konto (verifiziert 14.11.2026)
- **Symptom:** Befehle oder `getSockAddr` liefern `code 102`.
- **Ursache:** Jeder Login stellt einen neuen Token aus und macht den alten ungültig (`getSockAddr` → `{"code":102,"msg":"APP你的token过期啦"}`). Meldet sich der Fremd-Client neu an, ist die Handy-App abgemeldet (Knöpfe grau, nur Neu-Anmelden hilft).
- **Fix:** Zweitkonto anlegen und den Roboter per „Gerät teilen“ freigeben (App 1.5.5 kann das); Token speichern, bei `code 102` einmal neu einloggen.
- **Quelle:** https://community.home-assistant.io/t/proscenic-m7-pro/241732?page=3 ; JuliusBlueTek `update_sockets_ip`

## 3. Zwei Gateway-Verbindungen verdrängen sich
- **Symptom:** Proscenic-App oder Fremd-Client verliert den Live-Status.
- **Fix:** Socket nur kurz für einen 20001-Status öffnen und schließen; Befehle über REST.
- **Quelle:** https://github.com/JuliusBlueTek/Proscenic-Home-Assistant

## 4. Neuere Konten der App "Proscenic" 4.x (com.proscenic.smart.robot) sind leer
- **Symptom:** `getEquips` liefert keine Geräte.
- **Ursache:** Neue Plattform (Tuya), nicht die Legacy-Cloud. M7 Pro mit `com.proscenic.robot` 1.5.5 ist nicht betroffen (verifiziert 14.11.2026).
- **Quelle:** https://github.com/Hyperion5088/homeassistant-proscenic-air-fryer ; HA-Forum Seite 3

## 5. cmd21005 liefert HTTP 400 auf dem M6 Pro
- **Fix:** Befehlscodes modellabhängig; am Gerät prüfen.
- **Quelle:** https://community.home-assistant.io/t/proscenic-m7-pro/241732?page=2
