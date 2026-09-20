# PCIe-Grafikkarten: Verbindung und BIOS prüfen

Stand: 20.09.2026 10:18 (Systemzeit). Bereichsversion: Dokumentationsstand, keine Softwareänderung.

## Versionsanker und konkreter Befund

MSI MAG Z690 TOMAHAWK WIFI DDR4 (MS-7D32), Revision 1.0; Intel Core i9-13900K; NVIDIA GeForce RTX 5090, MSI-Subsystem 53031462; Treiber 616.92; VBIOS 98.02.2e.40.a3. Mainboard-BIOS laut SMBIOS: 1.M0, Build-Datum 05.08.2025. Grafikkarte laut Benutzer direkt im obersten langen Steckplatz ohne Riser.

Am 20.09.2026 lieferte NVIDIA-SMI GPU-Maximum Gen5, Host-Maximum Gen5 und maximale Breite x16. Im Leerlauf wurden Gen1 x4/P8, zwischenzeitlich auch aktive Gen5 x4/P0 gemessen. Gen5 ist damit tatsächlich beobachtet; ein erfolgreicher Gen5-x16-Lasttest ist NICHT belegt. Der CPU-Z-Screenshot vom 04.09.2026 zeigt abweichend maximal Gen4 x16; die Ursache dieser älteren Anzeige ist nicht ermittelt.

## Diagnosefolge

1. Exakte Mainboard-Variante, CPU, GPU, Treiber und BIOS live auslesen. DDR4-/DDR5-Namenszusätze dürfen bei Firmware nicht entfallen.
2. Maximalfähigkeit und momentane Verbindung getrennt erfassen. `nvidia-smi --help-query-gpu` erklärt für Treiber 616.92 ausdrücklich, dass sowohl aktuelle PCIe-Generation als auch aktuelle Link-Breite bei ungenutzter GPU reduziert sein können.
3. Unter einer kurzen, normalen 3D-Last erneut messen. Ein Leerlaufwert allein ist weder ein Defektnachweis noch ein ausreichender Nachweis der vollen Anbindung. Bleibt die Verbindung unter geeigneter anhaltender Last x4, ist das separat zu untersuchen.
4. Erst danach BIOS-Konfiguration, tatsächlichen Steckplatz, Riser und Kartenkontakt untersuchen. Aus maximal Gen5 folgt allein noch nicht aktive Gen5; in dieser Sitzung wurde aktive Gen5 zusätzlich gemessen.
5. BIOS-Versionen nur von der Supportseite des exakten Modells zuordnen. Fehlende dynamische Tabellen sind keine Aussage darüber, dass es kein Update gibt. Offizielle Länderseite oder Herstellerunterlagen gegenprüfen.

Lesender Diagnosebefehl:

```powershell
nvidia-smi --query-gpu=pcie.link.gen.gpucurrent,pcie.link.gen.gpumax,pcie.link.gen.hostmax,pcie.link.width.current,pcie.link.width.max,pstate,utilization.gpu --format=csv
```

## MSI-BIOS-Abgleich zum Versionsanker

| MSI-Version | Veröffentlichung | Relevante Angaben des Herstellers |
|---|---|---|
| 7D32v1M | 12.08.2025 | Bestehende Versionsfamilie 1.M0; ME 16.1.38.2676, Änderungen an Sicherheitsvorgaben und Lüftereinstellungen |
| 7D32v1N | 17.03.2026 | GOP und CPU-Microcode aktualisiert; ME 16.1.40.2765 |
| 7D32v1O | 28.08.2026 | Neuester ausgelesener Eintrag ohne Beta-Kennzeichnung; Speicherkompatibilität verbessert; ME 16.1.42.2872 |

Die MSI-Veröffentlichung und das SMBIOS-Build-Datum bezeichnen verschiedene Zeitpunkte. Die Versionsfamilie passt; ein binärer Vergleich der installierten Firmware wurde nicht durchgeführt. Keine der hier ausgewerteten neueren Versionsnotizen nennt eine spezielle RTX-5090-Freischaltung für PCIe 5.0. Das vorhandene BIOS ließ bereits aktive Gen5-Verbindungen zu.

Mainboard-BIOS und Grafikkarten-VBIOS sind getrennte Firmware. Die PCI-Subsystemkennung belegt noch nicht zuverlässig die genaue MSI-Verkaufsvariante und deren geeignete VBIOS-Datei. Keine pauschale VBIOS-Empfehlung ohne passendes Modell und konkretes Fehlerbild.

## Quellen und Grenzen

- [MSI DDR4-Produktseite](https://www.msi.com/Motherboard/MAG-Z690-TOMAHAWK-WIFI-DDR4), offiziell: Gen5-Unterstützung. Abruf über Firecrawl.
- [MSI DDR4-Support mit vollständiger BIOS-Liste](https://ru.msi.com/Motherboard/MAG-Z690-TOMAHAWK-WIFI-DDR4/support), offiziell: Versionen und Changelogs. Abruf über Tavily-Rückfall, weil der globale Crawl keine Tabelle lieferte.
- [MSI BIOS 7D32v1O](https://download.msi.com/bos_exe/mb/7D32v1O.zip), offiziell verlinktes Paket; nicht heruntergeladen oder geflasht. Auf der Supportseite veröffentlichter SHA-256: `19474932d867ef393fd9c693b8a54b55f18e5f4ff78892c0891b6e69f245ffef`.
- Lokale Primärquelle: NVIDIA-SMI 616.92, `--help-query-gpu`, `-q` und obiger Abfragebefehl; SMBIOS/PnP über Windows-CIM. Kein synthetischer Lasttest: WinSAT-Start erforderte erhöhte Rechte und schlug vor Ausführung fehl.
- Nicht verifiziert: genaue BIOS-Menübezeichnung dieser Version, gespeicherter Auto-/Gen-Wert, Gen5 x16 unter Last, passendes GPU-VBIOS-Update.

## Bezug zum Bug-Almanach

| Best Practice | Gegenstück |
|---|---|
| Maximalfähigkeit, Idle und Last trennen | [Fehlinterpretation der PCIe-Anzeige](../../bugs/desktop/pcie-grafikkarten.md) |
| DDR4-Firmware eindeutig zuordnen | [Verwechslung ähnlich benannter Mainboards](../../bugs/desktop/pcie-grafikkarten.md) |

Auf ausdrücklichen Benutzerwunsch nur Dokumentation: keine Hook-Änderung. Die rekursive Dateierfassung ist vorhanden; eine zusätzliche kuratierte Hook-Registrierung wurde nicht vorgenommen.
