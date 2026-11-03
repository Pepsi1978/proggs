# Bug-Almanach: Laptop-Helligkeit wirkt nicht (ASUS ROG, NVIDIA, Windows 11)

> Stand: 03.11.2026. Versions-Anker: ASUS G834JZ (Scar 18, 2023), Panel BOE NE180QDM-NZ2, RTX 4080 Laptop, NVIDIA 32.0.16.1656 (vermutlich 616.56), BIOS 333, Windows 11 Build 26200.
> Kurzcheck: [laptop-helligkeit-asus-nvidia-kurzcheck.md](laptop-helligkeit-asus-nvidia-kurzcheck.md). Gegenseite: `best-practices/desktop/laptop-helligkeit-asus-nvidia.md`.
> Quellenlage: ROG- und NVIDIA-Foren waren beim Abruf gesperrt (403 oder nur per JavaScript). Mit [S] markierte Einträge stammen nur aus Suchzusammenfassungen. Es gibt keine von ASUS oder NVIDIA bestätigte Ursache für das G834JZ.

## Fall G834JZ (03.11.2026, offen)

**Symptom:** Tasten bewegen nur den OSD-Balken, WMI meldet 100 %, das Panel wirkt zu dunkel. Ein vor dem Neustart dunkler gestellter Wert gilt laut Nutzer auch nach dem Neustart.

**Geprüft ohne Befund:** HDR aus (Registry), adaptive Helligkeit aus, Nachtlicht aus, Gamma-Kurve normal, Netzbetrieb, NPCF-Gerät OK, beide Grafikadapter OK, keine Display-Fehler im Ereignisprotokoll der letzten 24 Stunden.

**Ohne Wirkung:**
- `Win+Strg+Umschalt+B` mit gesetztem Wert 100 %: kein Helligkeitsgewinn. Nach dem ersten Kürzel reagierte ein WMI-Test auf 10 % sichtbar, ein späterer Test auf 20 % nicht mehr.
- Neustart des Dienstes `NVDisplay.ContainerLocalSystem` als Administrator: Ein WMI-Test auf 20 % danach zeigte keine Änderung.

**Stand bei Abschluss der Recherche:** nicht behoben. Ungetestet: Neustart mit gespeichertem Wert 100 %, BIOS-Vergleich, GPU-Modus Standard, EC-Reset, ASUS-Grafiktreiber, Rollback, BIOS 334.

## Einträge

### 1. Regler oder Balken bewegen sich, das Panel nicht
- **Symptom:** Der Wert ändert sich in Windows und lässt sich zurücklesen, die Beleuchtung bleibt gleich.
- **Ursache:** Uneinheitlich. Monitor.sys nimmt den Wert an und gibt ihn an Grafiktreiber oder ACPI weiter. Der WMI-Wert beweist daher nicht, dass das Panel ihn umsetzt (Schlussfolgerung aus der Microsoft-Doku).
- **Betroffen:** Mehrere Hersteller. Bei ROG mehrfach mit NVIDIA-GPU gemeldet.
- **Fix:** Pfad eingrenzen, siehe Best Practices. Keine allgemeingültige Lösung.
- **Quellen:** https://learn.microsoft.com/en-ca/windows-hardware/drivers/display/supporting-brightness-controls-on-integrated-display-panels ; https://github.com/seerge/g-helper/issues/660 ; https://github.com/seerge/g-helper/issues/4632

### 2. ROG mit NVIDIA: Steuerung fällt nach Shutdown mit abgeschalteter dGPU aus
- **Symptom:** Regler und Tasten ohne Wirkung nach Neustart oder Shutdown im Eco- oder Optimized-Modus.
- **Ursache:** Laut G-Helper-Maintainer NVIDIA-Treiber oder Windows, tritt auch mit Armoury Crate auf. Kein NVIDIA-Beleg.
- **Betroffen:** ROG-Modelle 2022 bis 2025, darunter G14, G16, G17 G713PV. G18 2023 nur als Einzelbericht.
- **Fix:** Dienst „NVIDIA Display Container LS“ neu starten (mehrfach bestätigt, meist nur zeitweise). G-Helper „Enable GPU on shutdown“ oder ab v0.231 die Option zum Stoppen der NVIDIA-Dienste. DDU ist unzuverlässig. **Beim G834JZ-Fall half der Dienst-Neustart nicht.**
- **Quellen:** https://github.com/seerge/g-helper/discussions/1042 ; https://github.com/seerge/g-helper/issues/660

### 3. Helligkeit hängt im dGPU-Direktmodus (Ultimate, MUX) [S]
- **Symptom:** Nur im dGPU-Modus: Regler ohne Wirkung oder ausgegraut, teils erst nach externem Monitor.
- **Ursache:** Nicht bestätigt.
- **Betroffen (Einzelberichte):** Scar 18 2025, Zephyrus G16, G16 RTX 5050, FA608PM nach VBIOS-Update, G834JZR, G733PY.
- **Fix:** GPU-Modus auf Standard, Neustart, Helligkeit auf Maximum, zurück auf Ultimate, Neustart. Alternativ Moduswechsel im BIOS. ASUS-Support nennt den modelleigenen NVIDIA-Treiber.
- **Quellen:** https://rog-forum.asus.com/t5/rog-strix-series/the-screen-brightness/td-p/1111388 ; https://rog-forum.asus.com/t5/rog-zephyrus-series/brightness-adjustment-disappears-when-switching-to-discrete/m-p/1073318 ; https://rog-forum.asus.com/t5/gaming-notebooks/rog-strix-g16-amd-rtx-5050-brightness-bug-in-ultimate-gpu-mode/td-p/1132692 ; https://rog-forum.asus.com/t5/gaming-notebooks/display-brightness-contrast-issue-in-ultimate-gpu-mode-after/td-p/1160605 ; https://www.justanswer.com/laptop/rsqgs-issue-laptop-screen-rog-strix-18-model.html

### 4. NVIDIA-Treiberberichte zu Helligkeit [S, Einzelberichte]
| Version | Symptom | Gemeldete Abhilfe | Quelle |
|---|---|---|---|
| 610.47, 610.62 | Regler verschwindet im Discrete-Modus (Legion) | Hybrid oder Rollback 596.49 | https://www.nvidia.com/en-us/geforce/forums/game-ready-drivers/13/587425/screen-brightness-control-completely-disappears-af/ |
| 591.44 bis 596.49 | Flicker-free Dimming ohne Wirkung im dGPU-Modus (GA605WV) | 581.94 oder 591.74 | https://rog-forum.asus.com/t5/rog-zephyrus-series/flicker-free-dimming-and-color-temperature-broken-in-dgpu-mode/td-p/1149297 |
| 591.xx | Über MUX deutlich dunkler (G615LR) | 581.57 | https://rog-forum.asus.com/t5/rog-strix-series/rog-strix-g16-g615lr-too-high-brightness-issue/td-p/1131617 |
| 580.88 | Display dunkel trotz Regler | Rollback 577.0 | https://www.nvidia.com/en-us/geforce/forums/game-ready-drivers/13/572886/display-dimming-issue-version-58088/ |

Bei 616.56 ist nur ein anderes Problem belegt: Flackern und kurzes Abdunkeln an 8-bpc-Anzeigen, Workaround 10 bpc oder automatische Farbverwaltung, Rollback 610.88. Erschienen am 26.08.2026. Quellen: https://www.thefpsreview.com/2026/09/01/geforce-616-56-is-the-fastest-driver-in-months-and-it-broke-two-things-on-the-way/ ; https://www.notebookcheck.net/Nvidia-Game-Ready-Driver-616-56-is-causing-display-problems-for-some-users-including-flickering.1386228.0.html

### 5. Dell G15 5530: NPCF-Gerät fehlerhaft
- **Symptom:** Rücklesewert stimmt, Panel bleibt gleich (Advanced Optimus, Build 26300).
- **Ursache:** Laut Repo-Autor `ACPI\NVDA0820\NPCF` im Zustand „failed install“. Ein Gerät, Eigenaussage.
- **Fix:** Dell-NVIDIA-Treiberpaket 32.0.16.1687.
- **Beim G834JZ-Fall ausgeschlossen:** Das Gerät steht auf OK.
- **Quelle:** https://github.com/morshedmilton/dell-brightness-stuck-fix

### 6. Armoury Crate 6.1.13.0: dunkleres Bild [S]
- **Symptom:** Maximalhelligkeit sinkt, normal nur im Ultimate-Modus (G18).
- **Fix:** Uninstall-Tool und Neuinstallation. Einzelberichte.
- **Quellen:** https://rog-forum.asus.com/t5/rog-strix-series/laptop-brightness-down-with-armory-crate-update/td-p/1081788 ; https://rog-forum.asus.com/t5/gaming-notebooks/armoury-crate-makes-my-screen-look-dimmer-without-changing-the/td-p/1084547

### 7. Reset hält nur kurz
- **Symptom:** HDR ein und aus oder Monitor-Gerät aus und an stellt die Regelung her, später fällt sie wieder aus.
- **Betroffen:** Windows 11 (Nov. 2025), ROG GL503VM unter Windows 10. Einzelberichte.
- **Quellen:** https://learn.microsoft.com/en-us/answers/questions/5613686/brightness-not-changing-in-windows-11 ; https://learn.microsoft.com/en-us/answers/a/5761176

### 8. Falsche Fährten
- **KB5095093, KB5095182, KB5092427:** Kein Helligkeitsbezug in den Release Notes, Microsofts Release-Health-Seite für 25H2 (Stand 02.11.2026) nennt nichts Passendes. Nicht deswegen deinstallieren. https://support.microsoft.com/help/5095093 ; https://support.microsoft.com/help/5092427 ; https://learn.microsoft.com/en-us/windows/release-health/status-windows-11-25h2
- **`Win+Strg+Umschalt+B`:** Microsoft dokumentiert nur „Wake up the device when the screen is blank or black“. Kein belegter Treiber-Reset. https://support.microsoft.com/en-us/windows/keyboard-shortcuts-in-windows-dcc61a57-8ff0-cffe-9796-cb9706c75eec
- **Silent-Profil, Panel Power Saver:** Keine Helligkeitsbegrenzung belegt. https://rog.asus.com/ca-en/articles/guides/armoury-crate-performance-modes-explained-silent-vs-performance-vs-turbo-vs-windows/
- **Monitor-Duplikate im Status Unknown:** Vermutlich Altlasten, kein Kausalbeleg.
- **Panel-Alterung:** Erklärt keinen wirkungslosen Regler. Eigene Rechnung: grob 10 bis 20 % in 3 Jahren, keine Messdaten.
- **ASUS Smart Display Control:** Keine Funktionsbeschreibung gefunden. `FRQ_Config.txt` enthält nur Bildwiederholraten.
- **Live-Linux negativ:** Beweist keinen Defekt, auf ASUS mit NVIDIA ändert sich dort der Wert oft ohne Panel-Wirkung. https://forums.developer.nvidia.com/t/brightness-not-working-asus-laptop-gl503vs-linux/312321 ; https://discuss.cachyos.org/t/asus-rog-strix-brightness/17267
