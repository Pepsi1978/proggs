# Proscenic-Cloud — Kurzcheck (Stand 2026-11-14)
1. Fremd-Saugroboter unsichtbar steuern: Cloud-API statt UI-Automation (Volltext `proscenic-cloud.md`).
2. Login-Erfolg an `data.token` und `equipcount` >= 1 prüfen, nicht an `code 0`.
3. Token speichern, nur bei `code 102`/401/403 neu einloggen.
4. Befehle: Form-POST `/instructions/…` mit Header `token`; Status: TCP-Gateway, AES-128-ECB mit Token.
