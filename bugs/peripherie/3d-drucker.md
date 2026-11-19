# 3D-Drucker (FDM) – Bug-Almanach (Stand 19.11.2026)

> Versions-Anker: Marktstand 19.11.2026. Quellen und Rohberichte: `best-practices/peripherie/recherchen/3d-drucker-2026-11/`.
> Gegenstück: `best-practices/peripherie/3d-drucker.md`. Fast alles [extern] aus Foren/Tests, keine systematischen Ausfallraten.

## B1 „Filter eingebaut“ ≠ HEPA
- Symptom: Partikelbelastung trotz „Filter“. Ursache: Bambu P1S/P2S/X1C und QIDI Plus4 haben nur Aktivkohle.
- Fix: Gerät mit H12-HEPA + Kohle wählen (H2D/H2S/H2C, QIDI Q2, Prusa Advanced Filtration) oder HEPA nachrüsten. https://bambuhub.net/articles/printer-air-quality

## B2 Gesättigte Kohle / undichtes Gehäuse
- Symptom: Geruch trotz Filter. Ursache: Kohle gesättigt, Spalten/Lüfter umgehen den Filter (Gehäuse 67–95 % wirksam).
- Fix: Wechsel nach Stunden (Herstellerangaben 500–1.440 h widersprüchlich → früher), Nachlüften. https://stacks.cdc.gov/view/cdc/258855
- Raum-Luftreiniger mit HEPA entfernt kein Styrol/Caprolactam.

## B3 Weiches TPU im Mehrfarbsystem
- Symptom: Stau/Verstopfung, Filament plattgedrückt. Betroffen: Bambu AMS/AMS 2 Pro/AMS Lite (95A/85A), QIDI Box, Creality CFS.
- Fix: externe Spule direkt in Direktextruder, 15–25 mm/s, Retraktion minimal; nur „TPU for AMS“ (68D) ins AMS. https://wiki.bambulab.com/en/knowledge-sharing/tpu-printing-guide
- H2D/H2C: 85A nur über die rechte Düse. QIDI Q2: < 95A klemmt im Extruder. Prusa CORE One: 85A nur mit Magnet-Mod am Filamentsensor.

## B4 Nasses Filament
- Symptom: Blasen, Fäden, Stau, Festigkeitsverlust bei TPU/PA/PC/PET-CF. Fix: 4–8 h trocknen, aus Trockenbox drucken.

## B5 Messingdüse + CF/GF
- Symptom: Düsendurchmesser wächst, Qualität sinkt. Fix: gehärtete Düse.

## B6 Kammerheizung erreicht Sollwert nicht
- Creality K2 Pro: statt 60 °C nur 45 °C (Heizung allein) / 57 °C (mit Bett, 45 min). https://www.igorslab.de/creality-k2-pro-combo-3d-drucker-im-test/
- QIDI Q2/Plus 5: Kammertemperatur wird vom Slicer nicht automatisch gesetzt → manuell + vorheizen.

## B7 Heat Creep bei PLA in beheizter Kammer
- QIDI Q2/Plus und andere: bei PLA/PETG Kammerheizung aus bzw. Tür/Deckel offen. https://printer-hub.com/posts/qidi-q2-known-issues

## B8 Modellspezifische Verschleiß-/Serienthemen
- Bambu H2D: Hotend-Lüfter ~1.600 h, Part-Cooling ~1.000 h (PAHT-CF), Spindelmutter ~2.000 h, Vent-Door-Servo ab Werk defekt. https://stack-archive.com/blog/bambu-lab-h2d-reliability-4000-hours-owner-repairs-2026/
- Bambu H2D/H2S: Filamentsensor-/AMS2-RFID-Fehler, Bambu-Studio-Bug Filament-Düsen-Zuordnung (Fix ohne Termin, Mai/Juni 2026). https://forum.bambulab.com/t/h2s-downtime-due-to-failures/252622
- Bambu X2D: Foren-Berichte zu Zuführung, AMS HT, WLAN-Abbrüchen (anekdotisch).
- Prusa CORE One: Spannrolle gerissen/Schraube festgefressen (Erstauslieferung 2025) → Schraube fetten, Rolle aus PC-CF. https://forum.prusa3d.com/forum/prusa-core-one-assembly-and-first-prints-troubleshooting/tension-pulley-broken
- QIDI Box: Reibung, Staus, nicht TPU-tauglich („skip the box“). QIDI Q2-Firmware V01.01.02.01 (06/2026): Downgrade erzeugt Fehler.
- Creality K2 Plus: CFS-/Extruder-Verstopfungen, Firmware-Update löschte CFS-cfg. https://forum.creality.com/t/k2-plus-long-term-reliability/50845
- Prusa INDX: HT-Hotend nicht kompatibel; Lieferung neuer Bestellungen erst nach 02/2027.

## B9 Kauf-Fallen Amazon.de
- 110-V-/Import-Listings (Bambu P2S Combo ASIN B0FT818HCH = 110 V; B0G87M7V8Z mit falsch geschriebenem Markennamen). Fix: „Verkauf durch“, EU-Modellnummer, 230 V prüfen; Bambu-Garantie nur mit Beleg aus EU-Store/autorisiertem Händler.
- Recherche-Falle: amazon.de liefert WebFetch/curl HTTP 503; Bambu-Wiki WebFetch 402/403 (curl mit Browser-User-Agent geht).

## B10 Prusa CORE One+ Gen 2 Kinderkrankheiten
- Nozzle-Wiper sitzt ~2–3 mm zu weit vorn (GitHub-Issue 5496, offen auch mit Firmware 8.0.0); Gen 1: „Nozzle cleaning failed“ ohne bestätigten Fix, Resonanz bei 40–80 mm/s offen.
- CORE One Kammer nur über das Bett beheizt → Warping großer ABS/PC-Teile; ASA-Geruch trotz Advanced Filtration nicht ganz weg (Kammer nicht dicht).
- Wartungs-Kit (199 €) passt nicht zu Gen 2 / L+. Quelle: Rohberichte r11, r13 in best-practices/peripherie/recherchen/3d-drucker-2026-11/.
