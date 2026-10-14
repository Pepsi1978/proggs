# Update-Skript vererbt Admin-Rechte → Overlays fügen nicht mehr ein (UIPI)

Stand: 14.10.2026

## Symptom
TVO/CVO nehmen auf, transkribieren und loggen im `diag.log` jeden Einfüge-Schritt mit `ok`
(`clipboard_set`, `foreground`, `ctrl_v`) — im Terminal/in der CLI kommt aber nichts an.

## Ursache
Windows-UIPI: Ein Prozess mit normaler Integrität (Medium, 0x2000) darf keine Tastendrücke an ein
Fenster mit höherer Integrität (High, 0x3000) schicken. `SendInput` meldet trotzdem Erfolg.
Auslöser war `OpenLauncher/update-launcher.ps1`, aufgerufen aus einem erhöhten Prozess
(UpdateZentrale/Admin-Sitzung): `__COMPAT_LAYER=RunAsInvoker` + `UseShellExecute=false` erbt das
Admin-Token → Launcher erhöht → jedes Terminal + CLI erhöht. Die Overlays laufen über die geplanten
Aufgaben „Spracheingabe - …" mit RunLevel Limited.

## Diagnose (1 Minute)
Integritätsstufe von Zielfenster-Prozess und Overlay vergleichen (OpenProcessToken +
GetTokenInformation(TokenIntegrityLevel)). Ziel höher als Overlay → UIPI.

## Fix / Poka-Yoke
`update-launcher.ps1` erkennt Admin-Rechte und startet den Launcher dann über eine einmalige
geplante Aufgabe mit `RunLevel Limited` (Muster wie `rebuild-overlay.ps1`). Bereits erhöht
gestartete Terminals bleiben erhöht → neue CLI-Sitzung öffnen.