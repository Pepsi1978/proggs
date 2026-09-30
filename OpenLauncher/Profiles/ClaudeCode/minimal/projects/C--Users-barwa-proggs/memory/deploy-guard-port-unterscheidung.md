---
name: deploy-guard-port-unterscheidung
description: Bei Voice-Overlay-Deploys ist "antwortet nicht" NICHT "nimmt gerade auf" — der offene Port trennt die Fälle
metadata:
  type: feedback
---

Wenn ein Guard oder Health-Check auf einen Status-Endpunkt wartet: prüfe **immer**, ob überhaupt jemand auf dem Port lauscht, bevor du fail-closed wartest. Kein Listener = der Dienst bietet den Schutz gar nicht an → sofort ohne Reservierung fortfahren. Port offen, aber stumm = er hängt → fail-closed, aber nach Sekunden, nicht Minuten.

**Why:** Am 30.08.2026 stand `rebuild-overlay.ps1` zehn Minuten still und lieferte nichts, weil CVO keinen Listener auf 5724 hat — der Status-Server der Overlays fällt bewusst still aus, wenn der `HttpListener` nicht binden darf (die App startet trotzdem). "Kein Listener" ist also ein vorgesehener Dauerzustand, kein Ausnahmefall. Der Guard behandelte ihn wie "könnte gerade aufnehmen" und pollte 600 s. Dieselbe Fehlerklasse steckte ein zweites Mal in der Startup-Verifikation (`AddMinutes(10)` × 2 Versuche). Nach dem Fix: 35 s statt 10 min.

**How to apply:** Der Fix sitzt in `voice-overlay-deploy-guard.ps1`/`.sh` (PortProbe) und `rebuild-overlay.ps1` (`PortOpen`, Verifikation über die Prozess-Startzeit statt über den Endpunkt). Der Kontrakt-Test "no deploy path can stall silently for minutes" in `voice-overlay-deploy-contract.test.mjs` erzwingt das künftig: keine Wartefrist über 3 Minuten, Port-Prüfung vor jedem fail-closed, Fortschrittsausgabe in jeder Warteschleife. Wenn Frank sagt, ein Deploy-Skript "macht 10 Minuten nix" — hier zuerst schauen. Siehe auch [[http-timeouts-fallback-wartezeit]].
