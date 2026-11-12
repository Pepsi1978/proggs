# UpdateZentrale: winget ordnet ein installiertes Paket nicht zu, `upgrade` endet mit 0x8A150014 (12.11.2026)

> Behoben in UpdateZentrale 1.3.84. macOS ist nicht betroffen: brew kennt keine Zuordnung über die Deinstallationsliste, und die macOS-Kette prüft nur über `brew list`.

## Symptom

Karte „Windows SDK (10.0.26100)“ (`art: winget`, `Microsoft.WindowsSDK.10.0.26100`) stand auf „Nicht installiert“. Ein Klick auf Aktualisieren lief trotzdem `winget upgrade --id Microsoft.WindowsSDK.10.0.26100 --exact --silent` und endete mit `0x8A150014` („Es wurde kein installiertes Paket gefunden …“), Stand nachher „(nicht lesbar)“.

## Ursache

- Das SDK **ist installiert**, sogar neuer als winget: Deinstallationsliste (HKLM, 32 Bit) `Windows Software Development Kit - Windows 10.0.26100.9457`, DisplayVersion `10.1.26100.9457` (kam über den Visual-Studio-Installer). winget bietet höchstens `10.0.26100.7705`.
- winget ordnet ARP-Einträge über die ProductCodes seiner Manifeste zu. Für 9457 gibt es kein Manifest, also findet `winget list/upgrade --id` nichts. `winget list --name "Windows Software Development Kit"` zeigt nur eine `ARP\Machine\X86\{…}`-Zeile ohne Paket-Id.
- Die UpdateZentrale ließ `winget upgrade` laufen, obwohl die Vorprüfung „nicht installiert“ gemeldet hatte. Das kann nur mit 0x8A150014 enden.
- Versionsfalle: ARP schreibt `10.1.26100.x`, winget `10.0.26100.x`. Ein voller Vergleich hielte die Karte für immer aktuell; erst ab der **dritten** Komponente (Build.Revision) ist der Vergleich richtig.

## Behebung

- Katalog-Felder `arpName` (Anfang des DisplayName in der Deinstallationsliste, HKLM 64/32 Bit und HKCU) und `arpVergleichAb` (ab welcher Versionskomponente verglichen wird, 0 = alle). Für das SDK: `"arpName": "Windows Software Development Kit - Windows 10.0.26100"`, `"arpVergleichAb": 2`. Der Namensanfang trifft keine anderen SDK-Bänder (22621) und keine Teil-MSIs („Windows SDK Desktop Tools“).
- `WingetAktualisierer.PruefenAsync`: Meldet winget „nicht installiert“/„unbekannt“ und ist `arpName` gesetzt, liest die Prüfung die höchste ARP-Version und die neueste Version aus `winget show` („Version:“, deutsch und englisch). Installiert ≥ Angebot → „Aktuell“; älter → „Update verfügbar“; unlesbar → „Unbekannt“, nie „Aktuell“.
- `FingerabdruckAsync` nimmt dann ebenfalls die ARP-Version, damit „Stand vorher/nachher“ lesbar ist und die Kette verifizieren kann.
- `AktualisierenAsync`: Bei nicht zugeordnetem Paket mit `arpName` entscheidet die ARP-Prüfung neu. Aktuell → nichts installieren; nicht sicher lesbar → Fehler ohne Installation; nur nachweislich älter → `winget install --id … --exact` statt `upgrade` (das ältere Bündel wird nie über ein neueres gelegt).
- Für die ganze Fehlerart: `UpdateKette.WingetNichtInstalliert` beendet jede winget-Kette mit Vorprüfung „nicht installiert“ vor dem Aufruf (Ergebnis „Abgebrochen“, kein Fehler) und nennt den Ausweg `arpName`/`wingetName`. Ausgenommen sind `wingetUpdatePerInstall` und andere Arten (Repo-Skripte bauen legitim, was „noch nicht gebaut“ ist).

## Erkennen

winget-Karte „Nicht installiert“, das Programm ist aber da und ein Update endet mit `0x8A150014`: `winget list --name "<Anzeigename>"` zeigt eine `ARP\…`-Zeile statt der Paket-Id. Dann im Katalog `arpName` (bei abweichendem Versionsschema zusätzlich `arpVergleichAb`) setzen. Zeigt `--name` die Paket-Id, reicht `wingetName`.
