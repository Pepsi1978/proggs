---
name: deploy-guard-port-unterscheidung
description: "Bei Voice-Overlay-Deploys ist \"antwortet nicht\" NICHT \"nimmt gerade auf\" — der offene Port trennt die Fälle"
metadata:
  node_type: memory
  type: feedback
  originSessionId: b7db77d8-d4f1-438f-b516-b06f24346713
  modified: 2026-11-07T13:55:32.939Z
---

Wartet ein Guard/Health-Check auf einen Status-Endpunkt: **immer** erst prüfen, ob jemand auf dem Port lauscht, bevor fail-closed gewartet wird. Kein Listener = Dienst bietet den Schutz gar nicht an → sofort ohne Reservierung weiter. Port offen, aber stumm = hängt → fail-closed, aber nach Sekunden, nicht Minuten.

**Why:** 30.08.2026 stand `rebuild-overlay.ps1` zehn Minuten still ohne Ausgabe: CVO hat keinen Listener auf 5724, weil der Status-Server der Overlays bewusst still ausfällt, wenn der `HttpListener` nicht binden darf (App startet trotzdem). "Kein Listener" ist vorgesehener Dauerzustand, kein Ausnahmefall; der Guard hielt ihn für "könnte gerade aufnehmen" und pollte 600 s. Dieselbe Fehlerklasse auch in der Startup-Verifikation (`AddMinutes(10)` × 2 Versuche). Nach dem Fix: 35 s statt 10 min.

**How to apply:** Fix in `voice-overlay-deploy-guard.ps1`/`.sh` (PortProbe) und `rebuild-overlay.ps1` (`PortOpen`, Verifikation über die Prozess-Startzeit statt über den Endpunkt). Kontrakt-Test "no deploy path can stall silently for minutes" in `voice-overlay-deploy-contract.test.mjs` erzwingt: keine Wartefrist über 3 Minuten, Port-Prüfung vor jedem fail-closed, Fortschrittsausgabe in jeder Warteschleife. Sagt Frank, ein Deploy-Skript "macht 10 Minuten nix": hier zuerst schauen. Siehe auch [[http-timeouts-fallback-wartezeit]].
