# Prusa CORE One+ (Gen 2) gegenüber CORE One (Gen 1): Verbesserungen, Probleme, Lautstärke, Filtration

Stand: 19.11.2026. Methode: WebSearch/WebFetch, nur Prusa-Seiten, Prusa-Forum, GitHub, Tests. Jede Aussage mit URL. "Belegt" = auf der Seite gelesen. "Unbelegt" = nicht gefunden.

## 1. Was Gen 2 verbessert (Herstellerangaben)

- Gen 2 wurde im August 2026 vorgestellt (Firmware 6.8.1 vom 14.08.2026). Der Preis bleibt gleich. Neu sind GT1.5-Riemen, ein Nozzle Wiper, eine neu montierte Heizbett-Aufnahme (Expansion Joints Gen 2) und ein werkzeuglos abnehmbares Top-Panel.
  - https://blog.prusa3d.com/better-prints-easier-use-prusa-xl-core-one-l-and-core-one-gen-2-our-big-product-update_137539/
  - https://www.prusa3d.com/p/core-one-plus/index.md
- GT1.5-Riemen: Wiederkehrende Muster (Riemenzahn-Artefakte) werden feiner und fallen auf glatten Flächen weniger auf. Das ist der gleiche Riemen wie bei XL+ und CORE One L+.
  - https://www.prusa3d.com/product/prusa-core-one-to-prusa-core-one-gen-2/
- Expansion Joints Gen 2 / Heizbett-Montage: Der Schritt "Absorbing heat" entfällt bei Bett-Temperaturen bis 85 °C (PLA, PETG). Bei höheren Temperaturen verkürzt er sich. In den Einstellungen gibt es einen Schalter unter Settings, Hardware, Expansion Joints Gen 2.
  - https://www.prusa3d.com/product/prusa-core-one-to-prusa-core-one-gen-2/
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/6-8-1-firmware-for-prusa-core-one-gen-2-core-one/
  - Das Prusa-Changelog nennt für 6.8.1 "deutlich kürzere Aufheizphase": https://help.prusa3d.com/changelog/core-one-plus
  - Warum die Joints das bewirken (Rahmen erreicht schneller stabile Temperatur), steht nur in einer Zusammenfassung der Suchmaschine, nicht auf einer geprüften Prusa-Seite. Der Mechanismus ist daher unbelegt.
- Nozzle Wiper (Silikon, neben dem Bett): Er ersetzt die bisherige Düsenreinigung am Bett und soll Fäden und Tropfen vermeiden.
  - https://www.prusa3d.com/p/core-one-plus/index.md
  - https://help.prusa3d.com/changelog/core-one-plus (6.8.1: "ersetzt die bisherige Düsenreinigung beim Druckstart")
- Top-Panel: Es lässt sich in Sekunden ohne Werkzeug abnehmen. Es ist laut Produktseite nicht im Upgrade-Kit enthalten (nur als Ersatzteil).
  - https://www.prusa3d.com/product/prusa-core-one-to-prusa-core-one-gen-2/
- Die Top-Vent-Mechanik wurde laut Blog robuster gemacht (nach einem Kommentator "more robust"). Ob damit ein Defekt gemeint ist, bleibt offen.
  - https://blog.prusa3d.com/better-prints-easier-use-prusa-xl-core-one-l-and-core-one-gen-2-our-big-product-update_137539/
- Zu Kammer, Lüftern, Lärm und Rahmen nennt Prusa KEINE Änderung. Die Gen-2-Produktseite nennt die gleiche max. Kammertemperatur (55 °C), die Maße 415 x 444 x 555 mm und 22,5 kg. Es gibt keine dB-Angabe.
  - https://www.prusa3d.com/p/core-one-plus/index.md
- Upgrade-Kit für Gen 1 (CORE One+): GT1.5-Riemen und Riemenscheiben, Expansion Joints Gen 2, Wiper, Ersatz-Nylonnieten. Preis 54,99 EUR inkl. MwSt.
  - https://www.prusa3d.com/product/prusa-core-one-to-prusa-core-one-gen-2/
  - Hinweis: Das Prusa-Forum nennt für den Umbau etwa 6 bis 8 Stunden (Nutzerangaben): https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/gen-2-upgrade-results/

### Nutzer-Erfahrung mit Gen 2 (nur Prusa-Forum, anekdotisch)
- Aufheizen kürzer: bestätigt (KlinWer, 21.09.2026).
- Druckqualität: Mehrere berichten weniger VFA ("significantly reduced", "much better"). Einer (stevo1957) sah nur eine kleine Verbesserung gegenüber den Marketingbildern.
- Lärm: Im Upgrade-Thread berichtet niemand über Lärm. Ein Nutzer (Nailig) schreibt am 11.10.2026, die Resonanz bei etwa 45 mm/s bestehe auch mit den Gen-2-Riemen weiter.
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/gen-2-upgrade-results/
  - https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/core-one-resonance-noise-issue

## 2. Kinderkrankheiten Gen 1 (CORE One, seit 2025) und Status

| Problem | Was ist belegt | Status |
|---|---|---|
| Phase-Stepping fehlt, Motor-/Frame-Lärm | März 2025: "Prusa has acknowledged that the lack of phase stepping calibration is a bug". Phase-Stepping-Kalibrierung kam mit FW 6.3.4 (18.07.2025), erfordert ein Accelerometer-Set (Tuning-Funktion, kein Pauschal-Fix). Nutzer berichten teils starke, teils keine Wirkung. | Teilweise gelöst (Option da, Wirkung je Gerät unterschiedlich) |
| Resonanzgeräusche 40 bis 80 mm/s | Thread vom 13.06.2025 bis 11.10.2026, keine Prusa-Antwort, kein offizieller Fix. Bleibt laut Nailig auch mit Gen-2-Riemen bestehen. | Offen |
| Dröhnen über Motor-Schrauben (Rahmenübertragung) | Thread 26.01.2026 "[Solved]": Lockern der Motorschrauben um eine halbe Umdrehung half dem Ersteller. Keine bestätigte Dauerlösung. | Workaround, kein offizieller Fix |
| Nozzle cleaning failed (Load-Cell bei hohen Temperaturen) | Support (Juni 2025): liegt an den Load-Cell-Messwerten bei hoher Temperatur. FW 6.4.0 brachte "Fortsetzen nach Fehlschlag". CORE One L: FW 6.5.2 (Januar 2026) behob zu strenge Prüfung, danach teils Wiederkehr. Für die normale CORE One kein bestätigter Fix. Mit Gen 2 ersetzt der Wiper die Reinigung (FW 6.8.1), Verbesserung in FW 7.0.0. | Gen 1 offen/teilweise, bei Gen 2 durch Wiper umgangen |
| Absturz/Neustart beim Türöffnen | Threads 2025. Workaround: Türsensor in Settings, Hardware, Door Sensor abschalten. FW 7.0.0 (05.10.2026) behebt einen Absturz beim Öffnen der Tür während des Homings. | Teilweise gelöst (nur Homing-Fall belegt) |
| Unzuverlässiges Homing, Bett-Probing/Z-Drift | FW 7.0.0: unzuverlässiges Homing behoben (ab 6.9.1), neue automatische Z-Ausrichtung bei unebenem Bett. | Laut Changelog gelöst, kaum Nutzerbestätigung gefunden |
| Riemenspannung, Y-Kalibrierung scheitert, Layershift, Spannrolle bricht | Mehrere Threads 2025 und 2026 (Belt cut loose, Tension pulley broken, Failed Y calibration). | Offen (Montage-/Hardware-Streuung) |
| Düsenverstopfung/Heat-Creep | Nutzerthreads ("nozzle clogging after 10 days"), keine offizielle Aussage gefunden. | Offen/unklar |
| Kammer-Hysterese (PETG bleibt bei ~42 °C trotz 35 °C Ziel) | Nutzer Steve, 19.08.2025, "als GitHub-Bug geführt". FW 6.5.3: Ziel höchstens 55 °C. | Unklar |

Quellen:
- https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/when-can-we-realistically-expect-more-fw-addressing-these-teething-pains (acht Kinderkrankheiten, Juni 2025)
- https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/prusa-core-one-not-usable-inhouse-sorry-but-the-sound-was-to-intensiv
- https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/6-3-4-firmware-for-prusa-core-one
- https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/core-one-resonance-noise-issue
- https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/reason-for-core-one-rumbling-found
- https://forum.prusa3d.com/forum/prusa-core-one-l-general-discussion-announcements-and-releases/6-5-2-firmware-for-prusa-core-one-l (Fix "nozzle cleaning failed", CORE One L)
- https://cdn.help.prusa3d.com/changelog/core-one-l
- https://help.prusa3d.com/changelog/core-one (neueste FW 8.0.2 vom 16.11.2026; 7.0.0 vom 05.10.2026)
- https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/core-one-crashing-when-opening-the-door
- https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/printer-reboots-when-i-open-the-door-during-a-print
- https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/nozzle-cleaning-consistently-fails-with-petg/paged/3
- https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/whats-your-experience-with-prusa-advance-filteration-for-prusa-core-one/ (Kammer-Hysterese)

Einschränkung: Der Changelog-Abruf war abgeschnitten. Die Einträge 6.2 bis 6.7 und 6.9/6.10 liegen nur als Verweise vor. Der CORE-One-Changelog zeigt in sichtbaren Versionen keine Einträge zu Load Cell oder Phase Stepping/Stealth. Eine Suchzusammenfassung nennt FW 6.6.0 (25.06.2026) mit Phase-Stepping-/Input-Shaper-Kalibrierungen, das habe ich nicht an der Quelle bestätigt.

## 3. Neue Probleme von Gen 2 (Stand Nov. 2026)

- Wiper trifft nicht die Mitte (Y etwa 2 bis 3 mm zu weit vorne): GitHub-Issue #5496 vom 22.09.2026, Label bug, zugewiesen, kein Milestone, keine Fix-Version. Laut Ersteller hilft auch FW 8.0.0 nicht. Möglicher Auslöser sind die GT1.5-Riemen (Schalter Menü, Einstellungen, Hardware, 1.5GT Belts prüfen), nicht bestätigt. Status: OFFEN.
  - https://github.com/prusa3d/Prusa-Firmware-Buddy/issues/5496
- Düse kratzt nach Gen-2-Upgrade am Druckblech statt am Wiper, teils ungleichmäßige Z-Bewegung. Thread 05. bis 26.09.2026, mehrere Betroffene, keine Prusa-Antwort. Status: OFFEN.
  - https://forum.prusa3d.com/forum/prusa-core-one-assembly-and-first-prints-troubleshooting/upgrade-to-gen2-nozzle-scratches-build-sheet-instead-of-using-the-wiper/
  - https://forum.prusa3d.com/forum/prusa-core-one-assembly-and-first-prints-troubleshooting/core-one-gen-2-nozzle-missing-the-center-of-the-wiper/
- Belt-Pulleys passen nicht richtig auf die Motorwelle (ovale Bohrung, Welle feilen, Scheibe falsch herum, Prusa ersetzte in einem Fall den Motor): Hardwarefall beim Umbau.
  - https://forum.prusa3d.com/forum/prusa-core-one-assembly-and-first-prints-troubleshooting/issues-with-pulleys-on-core-one-gen-2-upgrade/
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/gen-2-upgrade-results/
- Homing schlägt öfter an (sechs statt vier Anschläge), laute Phase-Stepping-/Input-Shaper-Kalibrierung, Y-Kalibrierung scheitert nach Wechsel auf GT1.5, nach dem Firmware-Update war das falsche Profil aktiv (JBinFL). Datum 19. bis 20.08.2026.
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/6-8-1-firmware-for-prusa-core-one-gen-2-core-one/
- Firmware 6.8.1: Sofort-Stopp bei Filamentende führt XY-Homing aus, ein Druck ging verloren (whitestormy). Virtuelle Extruder und MMU3 funktionieren mit 6.8.1 nicht, mit 6.5.7 ja. Nicht kompatibel mit INDX. Status ungeklärt, ob in 7.x/8.x behoben (kein Changelog-Eintrag gefunden).
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/6-8-1-firmware-for-prusa-core-one-gen-2-core-one/
- Frische Gen-2-Threads (Liste, 19.11.2026, Titel gelesen, Inhalt nicht gelesen): "Troubles with Gen 2 belts", "Issues with pulley in Gen2 upgrade", "Core One+ Gen 2 potentially partially clogged nozzle", "Posun vrstev po upgrade Core One+ Gen.2" (Layershift nach Upgrade, 18.10.2026).
  - https://forum.prusa3d.com/forum/prusa-core-one-assembly-and-first-prints-troubleshooting/
- Hinweis: Viele Gen-2-Fälle sind Upgrade-Fälle (Umbau durch Nutzer), nicht ab Werk gelieferte Geräte. Für ab Werk montierte Gen-2-Geräte fand ich kaum eigene Berichte; die Datenlage ist dünn.

## 4. Lautstärke (alles Gen 1, für Gen 2 keine Messung gefunden)

- Prusa nennt keine dB-Zahl: https://www.prusa3d.com/p/core-one-plus/index.md
- Messung Biomech, 01.04.2025, Smartphone-App, 50 cm: Normal 50,0 dB Mittel / 57,4 dB Spitze. Silent 47,7 dB Mittel / 55,0 dB Spitze. Dämm-Schaumstoff ohne Wirkung. Spätere Nutzerangabe 2026: Serverrack-Gehäuse senkt 3 bis 6 dB.
  - https://forum.prusa3d.com/forum/user-mods/i-tested-noise-and-heat-insulation-so-you-dont-have-to/
- Weitere Nutzerwerte (Phone-App, Suchzusammenfassung, nicht an der Quelle geprüft): Normal 55 bis 61 dB, Stealth 53 bis 60 dB bei 100 % Geschwindigkeit; ein Nutzer rund 47 dBA Stealth, unter 52 dBA normal in 1 m.
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/prusa-core-one-not-usable-inhouse-sorry-but-the-sound-was-to-intensiv/paged/5
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/prusa-core-one-not-usable-inhouse-sorry-but-the-sound-was-to-intensiv/paged/10
- Ein Review habe 62 dB bei geschlossener Tür gemessen (nur indirekt zitiert, Quelle unbekannt): https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/prusa-core-one-not-usable-inhouse-sorry-but-the-sound-was-to-intensiv
- Tom's Hardware: Antrieb etwas lauter als MK4S, Lüfter nur leises Brummen: https://www.tomshardware.com/3d-printing/prusa-core-one-review. 3DPrint.com: "one of the quietest CoreXY printers": https://3dprint.com/316927/prusa-core-one-innovating-or-catching-up/
- Stealth Mode begrenzt Beschleunigung und Geschwindigkeit, bringt nur wenige dB, ein Nutzer fand ihn "praktisch nutzlos": https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/stealth-mode-more-than-just-reduced-speed-acceleration
- Dominante Quellen: Kammerlüfter (bei Structural-PLA), bei hoher Geschwindigkeit Motoren und Y-Lager, Blechpaneele können klappern: https://forum.prusa3d.com/forum/user-mods/i-tested-noise-and-heat-insulation-so-you-dont-have-to/
- Exemplarstreuung: Es gibt Wohnungs-Beschwerden ("not usable inhouse", Gerät in die Garage verlegt) neben Nutzern, die zufrieden sind (derselbe Thread).

## 5. Stromverbrauch

- Herstellerangabe: PLA-Profil 90 W, ABS-Profil 110 W (Durchschnitt laut Produktseite): https://www.prusa3d.com/p/core-one-plus/index.md. Netzteil 240 W laut Händlern.
- Nutzermessung (Forum, nur als Grafik, 01.04.2025): Peaks von etwa 260 W (ABS) und etwa 300 W (PETG). Leerlauf und Dauerbetrieb der Kammerheizung nicht gemessen.
  - https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/power-requirement
- Gen 2: keine eigene Messung gefunden. Wegen kürzerer "Absorbing heat"-Phase könnte die Heizzeit sinken, belegt ist das nicht.

## 6. Wohnungstauglichkeit

- Pro: Gehäuse mit Tür und Kammer. Mit Advanced Filtration praktisch geruchsarm für PLA/PETG (siehe 7). Leiser Normalbetrieb von etwa 47 bis 52 dB bei sauberer Einstellung.
- Contra: Nutzerberichte, dass einzelne Geräte stark dröhnen und im Haus laut sind. Spitzen um 55 bis 58 dB. Resonanzen bei 40 bis 80 mm/s. Stealth Mode wenig wirksam. Eigenes Tuning (Phase Stepping, Motorschrauben, Geschwindigkeiten) nötig.
- Eigene Einschätzung (nicht aus einer Quelle): Mit gutem Exemplar tragbar im Arbeitszimmer. Schlafzimmer eher nein. ABS/ASA nur mit Filtration und Lüftung. Lärmverhalten sollte man im Rückgabezeitraum prüfen.

## 7. Advanced Filtration (Zubehör für CORE One, Gen 1 und Gen 2)

- Aufbau: HEPA für Ultrafeinstaub (UFP) plus Aktivkohle für VOC. Bei aktivierter Filtration übernimmt sie die Kammerregelung, die Kühllüfter bleiben aus (außer bei Überhitzung).
  - https://help.prusa3d.com/glossary/advanced-filtration-system_1034273
- Unabhängige Labormessung (UFP/VOC) fand ich NICHT. Prusa belegt die Wirkung nur mit dem eigenen Printfarm-Test (Herstellerangabe).
  - https://www.prusa3d.com/product/advanced-filtration-for-prusa-core-one/
- Nutzer (Forum, 11.08.2025): "praktisch geruchlos" mit Filter, früher Geruch nach heißem PLA verschwunden. Filter sehr leise, leiser als die Kammerlüfter, die er ersetzt. Keine Auswirkung auf die Druckqualität. Wechsel nach etwa 600 Betriebsstunden. Luft wird aus der Kammer durch den Filter nach außen geführt.
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/whats-your-experience-with-prusa-advance-filteration-for-prusa-core-one/
- ASA/ABS (Suchzusammenfassung eines Posts): reduziert "oppressive smoke/smell", aber nicht vollständig, weil die Kammer nicht dicht ist.
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/advanced-filtration-for-prusa-core-one
- Probleme: Bei ASA laufen die Filterlüfter im Druck nicht auf voller Leistung (sonst Warping), Filter kühlt nach Nutzermeinung nicht ausreichend (User-Mod "Dumptruck"). Ein Nutzer fand nach unter 100 Stunden Plastikgeruch vom Filter und vermutete gesättigte Kohle, unbestätigt.
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/whats-your-experience-with-prusa-advance-filteration-for-prusa-core-one/
  - https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/is-advanced-filtration-supposed-to-smell-weird
- Firmware 7.0.0: Kammerlüftungen schließen sich am Druckende automatisch (Quelle: Changelog, Zuordnung zur Filtration nicht eindeutig): https://help.prusa3d.com/changelog/core-one-plus
- Filter und Gen-2-Wechselwirkung: Nicht untersucht. Keine Hinweise gefunden.

## 8. Fazit: Ist Gen 2 ausgereift?

- Gen 2 ist ein Update von Teilsystemen (Riemen, Wiper, Bettaufnahme, Top-Panel), kein neuer Rahmen und keine neue Kammer. Der Nutzen (kürzere Aufheizphase, weniger Muster) ist plausibel belegt, aber die Wirkung auf Lärm ist nicht belegt.
- Nur etwa drei Monate im Markt (seit August 2026). Gen 2 hat eigene offene Fehler: Wiper-Position (GitHub #5496, offen, auch mit FW 8.0.0), Düse am Blech, Pulley-Passung, Homing-/Kalibrierprobleme, FW-6.8.1-Probleme mit MMU3/virtuellen Extrudern (Status in 7.x/8.x ungeklärt). Viele Berichte kommen von Upgradern, nicht von Werksgeräten.
- Gen-1-Altlasten (Resonanz bei 40 bis 80 mm/s, Streuung beim Lärm) bleiben offen. Gelöst oder abgemildert: Homing (FW 7.0.0), Phase-Stepping-Option, Türsensor-Absturz im Homing, Nozzle-Cleaning durch Wiper bei Gen 2.
- Bewertung (eigene Einschätzung): Gen 2 ist besser als Gen 1 in Aufheizzeit und Oberfläche, aber noch nicht voll ausgereift. Firmware 8.0.2 (16.11.2026) ist neu. Wer in einer Wohnung druckt, sollte Lautheit und Wiper-Verhalten nach dem Kauf prüfen und die Filtration einplanen.

## Lücken / nicht belegt
- Keine dB-Messung für Gen 2. Keine Prusa-Aussage zu Lüftern/Rahmen/Kammer.
- Kein unabhängiger Filtrationstest.
- Kein Leerlauf-Stromwert.
- Tom's-Hardware-/All3DP-Seiten: Textauszug lieferte nur Fragmente.
- Mehrere Zahlen stammen aus Suchzusammenfassungen und sind als solche markiert.

BEST-PRACTICES-KANDIDATEN:
- Prusa Gen-2-Upgrade (CORE One+): Nach dem Umbau FW 6.8.1 oder neuer einspielen, Modell auf Gen 2 stellen (Settings, Hardware: 1.5GT Belts, Expansion Joints Gen 2), Riemen mit Gantry-Anleitung spannen, Z-Achse kalibrieren, Auto-Home, Phase Stepping/Input Shaper danach neu ausführen. Pulley-Passung und Drehrichtung prüfen (Quelle: https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/gen-2-upgrade-results/).
- CORE One in der Wohnung: Phase Stepping kalibrieren, Perimeter-Geschwindigkeit bei Resonanz (etwa 40 bis 45 mm/s) meiden, Motorschrauben prüfen, Advanced Filtration für ABS/ASA verwenden und nach etwa 600 h wechseln (Quellen oben).
- Türsensor bei Abstürzen testweise abschalten (Settings, Hardware, Door Sensor).

BUG-KANDIDATEN:
- Prusa CORE One+ Gen 2: Nozzle-Wiper-Trajektorie etwa 2 bis 3 mm zu weit in Y, GitHub #5496, offen, auch mit FW 8.0.0 (https://github.com/prusa3d/Prusa-Firmware-Buddy/issues/5496).
- Gen 2 Upgrade: Düse kratzt am Druckblech statt am Wiper (https://forum.prusa3d.com/forum/prusa-core-one-assembly-and-first-prints-troubleshooting/upgrade-to-gen2-nozzle-scratches-build-sheet-instead-of-using-the-wiper/).
- Gen 2 Upgrade: GT1.5-Pulleys passen nicht/werden falsch herum montiert, Y-Kalibrierung scheitert, Homing schlägt öfter an (https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/gen-2-upgrade-results/).
- FW 6.8.1: Sofort-Stopp bei Filamentende mit XY-Homing, virtuelle Extruder/MMU3 nicht erkannt (https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/6-8-1-firmware-for-prusa-core-one-gen-2-core-one/).
- CORE One (Gen 1): Resonanzgeräusch bei 40 bis 80 mm/s, offen seit 06/2025, auch mit Gen-2-Riemen (https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/core-one-resonance-noise-issue).
- CORE One: "Nozzle cleaning failed" (Load Cell), Türsensor-Neustart (https://forum.prusa3d.com/forum/prusa-core-one-hardware-firmware-and-software-help/when-can-we-realistically-expect-more-fw-addressing-these-teething-pains, https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/core-one-crashing-when-opening-the-door).
