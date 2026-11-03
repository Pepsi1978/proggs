# Best Practices: Laptop-Helligkeit diagnostizieren (ASUS ROG, NVIDIA, Windows 11)

> Stand: 03.11.2026. Versions-Anker: ASUS G834JZ, BOE NE180QDM-NZ2, NVIDIA 32.0.16.1656, BIOS 333, Windows 11 Build 26200.
> Kurzcheck: [laptop-helligkeit-asus-nvidia-kurzcheck.md](laptop-helligkeit-asus-nvidia-kurzcheck.md). Bugs: `bugs/desktop/laptop-helligkeit-asus-nvidia.md`.
> Markierung: `offiziell` = Hersteller-Doku, `extern` = Foren oder Dritte, [S] = nur aus Suchzusammenfassung. Die Reihenfolge ist eine Schlussfolgerung aus den Quellen, kein Herstellerablauf.

## Grundlagen

- **Helligkeitspfad** (`offiziell`): Monitor.sys stellt WMI bereit und nutzt das Brightness-Interface des Grafiktreibers, sonst ACPI `_BCL`, `_BCM`, `_BQC`. https://learn.microsoft.com/en-ca/windows-hardware/drivers/display/supporting-brightness-controls-on-integrated-display-panels
- **WMI-Aufruf** (`offiziell`): WMI ruft über den Monitortreiber `DxgkDdiSetBrightness` im GPU-Miniport auf. https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/dispmprt/nc-dispmprt-dxgk_brightness_set
- **Folgerung:** Ein zurückgelesener Wert belegt nur die Annahme durch die Software. Wirkung immer am Bild prüfen.
- **Tools:** Twinkle Tray nutzt am internen Display WMI, also denselben Weg (`extern`). ControlMyMonitor setzt DDC/CI voraus. Beide sind dort kein unabhängiger Test. https://github.com/xanderfrangos/twinkle-tray/wiki/Display-Detection-&-Support-Issues ; https://www.nirsoft.net/utils/control_my_monitor.html
- **Referenz** [S]: NE180QDM-NZ2 laut Datenblatt 538 cd/m², gemessen 467 bis 502 cd/m². Die Schwelle von etwa 300 cd/m² für „zu dunkel“ ist eine Schlussfolgerung. https://www.notebookcheck.com/Test-Asus-ROG-Strix-Scar-18-2023-G834JY-Gaming-Notebook-mit-RTX-4090.695714.0.html ; https://laptopmedia.com/screen/ne180qdm-nz2-boe0b79/

## Diagnose-Reihenfolge

1. **Einstellungen ausschließen:** inhaltsbasierte und adaptive Helligkeit, Nachtlicht, HDR. Reversibel. `offiziell`: https://support.microsoft.com/windows/content-adaptive-brightness-control-in-windows-292d1f7f-9e02-4b37-a9c8-dab3e1727e78
2. **WMI-Wert sichtbar testen:** Nacheinander niedrig und 100 % setzen, der Nutzer beurteilt das Bild. Beim Test den Ausgangswert wiederherstellen.
3. **Nur lesen:** NPCF-Gerät, Grafikadapter, Dienste, Ereignisprotokoll.
4. **BIOS-Bild vergleichen** (`offiziell`): Bleibt das Problem im BIOS, ist laut ASUS die Hardware möglicherweise defekt. Normal im BIOS spricht für Software. Kein Beweis, manche Geräte starten im BIOS absichtlich dunkler [S]. https://www.asus.com/support/FAQ/1015072
5. **GPU-Modus wechseln:** Standard oder Eco gegen Ultimate, jeweils mit Neustart. Wirkt der Regler nur in einem Modus, ist das ein Softwarebefund. Kein Panel-Defekt [S].
6. **Dienst „NVIDIA Display Container LS“ neu starten:** Braucht Administratorrechte. `extern`: https://github.com/seerge/g-helper/issues/660
7. **EC-Reset** (`offiziell`): Peripherie ab, ausschalten, Netzteil dran, Einschalter 40 s halten. Keine Quelle verknüpft ihn direkt mit dem Symptom. https://www.asus.com/us/support/faq/1050239/
8. **Treiber:** Erst das ASUS-Paket für das Modell, dann ein Rollback auf einen älteren Zweig. Nur Einzelberichte. Intel und NVIDIA nach DDU beide installieren (`extern`, ein Gerät).
9. **ASUS-Software:** GameVisual testweise auf Standard, Armoury Crate mit dem Uninstall-Tool neu installieren [S]. https://www.asus.com/il/support/FAQ/1043607
10. **BIOS aktualisieren:** Für das G834JZ gibt es 334 vom 22.06.2026, ohne genannten Helligkeitsfix. Nur Netzbetrieb und Hersteller-Tool. https://www.asus.com/supportonly/G834JZ/helpdesk_bios/
11. **Reparatur:** Bei dunklem BIOS und ohne Wirkung aller Modi. ASUS nennt das Reparaturcenter.

## Sicherheit

- Treiberwechsel, DDU und BIOS-Update nur nach Rückfrage und mit Wiederherstellungspunkt.
- Beim Entfernen von Monitor-Geräten nie den aktiven Eintrag und nie `pnputil /force`. https://learn.microsoft.com/en-us/windows-hardware/drivers/devtest/pnputil-command-syntax
- Update-Deinstallation ist kein Diagnoseschritt, solange kein KB als Ursache belegt ist.

## Praktischer Hinweis

Das Gerät läuft als G834JZ unter „ROG Strix“, die Modellreihe heißt Scar 18 (2023). Es hat ein IPS-Panel. Mini-LED kam erst 2024 (NE180QDM-NM2), dessen Helligkeitswerte gelten hier nicht [S].
