# PCIe-Grafikkarten: Diagnosefallen

Stand: 20.09.2026 10:18. Versionsanker: NVIDIA-SMI 616.92, RTX 5090, MSI MAG Z690 TOMAHAWK WIFI DDR4 MS-7D32, BIOS 1.M0. Die folgenden Punkte sind Diagnosefallen, keine pauschale Behauptung eines Hardwaredefekts.

## 1. Momentane Verbindung mit Maximalfähigkeit verwechseln

- Symptom: Niedrige PCIe-Generation oder x4 im Leerlauf wird als feste Hardwarebegrenzung interpretiert.
- Ursache: Momentane Energieverwaltung und maximal unterstützte Verbindung werden vermischt. NVIDIA beschreibt für beide aktuellen Werte eine mögliche Reduktion bei ungenutzter GPU.
- Versionen: Direkt geprüft mit NVIDIA-SMI 616.92; nicht pauschal auf jede GPU-/Treiberversion verallgemeinern.
- Funktionserhaltender Fix: GPU-Maximum, Host-Maximum, maximale Breite, aktive Werte und Leistungszustand erfassen; unter normaler Grafiklast wiederholen. Im konkreten Fall sind Gen5 aktiv und maximal x16 belegt, aktive x16 unter Last noch offen.
- Quelle: Offizielle lokale Tool-Hilfe `nvidia-smi --help-query-gpu` und Live-Abfragen vom 20.09.2026.
- Status: Diagnosefehler vermeidbar; x16-Lastprüfung des konkreten Rechners ausstehend.

## 2. BIOS der DDR5-Variante für das DDR4-Board empfehlen

- Symptom: Eine Suche nach MAG Z690 TOMAHAWK liefert Versionen 7D32vH… statt 7D32v1….
- Ursache: Die Namensähnlichkeit der Varianten und eine beim Crawl fehlende dynamische DDR4-Downloadtabelle führen zum falschen Supportzweig.
- Versionen: Live-Board MAG Z690 TOMAHAWK WIFI DDR4, Recherche 20.09.2026. Korrekte aktuelle Liste: 7D32v1O vom 28.08.2026; vorhandene Familie 1.M0/7D32v1M.
- Funktionserhaltender Fix: Exakte DDR4-Supportseite prüfen. Eine andere offizielle Länderseite kann die vollständige Tabelle liefern. Kein Firmwarepaket allein anhand der gemeinsamen Nummer MS-7D32 auswählen.
- Quellen: [DDR4-Support](https://ru.msi.com/Motherboard/MAG-Z690-TOMAHAWK-WIFI-DDR4/support) und [anders benannte Variante](https://www.msi.com/Motherboard/MAG-Z690-TOMAHAWK-WIFI/support), beide offiziell.
- Status: In dieser Recherche erkannt und korrigiert; kein falsches Paket heruntergeladen oder geflasht.

## 3. Alte Anzeige ohne aktuelle Gegenprobe als BIOS-Fehler behandeln

- Symptom: CPU-Z-Screenshot vom 04.09.2026 zeigt maximal PCIe 4.0 x16, obwohl heutige NVIDIA-SMI-Messungen aktive Gen5 zeigen.
- Ursache: Für die konkrete alte Anzeige nicht geklärt. Andere Messzeitpunkte, Tool-Anzeige oder damalige Konfiguration sind mögliche Erklärungen, keine bestätigten Ursachen.
- Versionen: Screenshot CPU-Z 2.19.0 x64; heutiger Treiber 616.92; identifizierte Karte RTX 5090.
- Funktionserhaltender Fix: Aktuelle direkte Messung vor BIOS-/VBIOS-Eingriffen. BIOS-Update nur nach modellgenauem Abgleich und eigenem Änderungsgrund bewerten.
- Quelle: Benutzer-Screenshot und lokale Primärmessung; [offizielle MSI-BIOS-Liste](https://ru.msi.com/Motherboard/MAG-Z690-TOMAHAWK-WIFI-DDR4/support).
- Status: Heutige feste Gen4-Begrenzung widerlegt; Ursache der historischen Anzeige offen.

## Bezug zu Best Practices

| Falle | Vorgehen |
|---|---|
| Idle/Maximum verwechselt | [Messwerte getrennt vergleichen](../../best-practices/desktop/pcie-grafikkarten.md) |
| Falscher BIOS-Zweig | [Exaktes Modell und Quellen kontrollieren](../../best-practices/desktop/pcie-grafikkarten.md) |
| Unnötiger Firmwareeingriff | [Aktuellen Zustand zuerst belegen](../../best-practices/desktop/pcie-grafikkarten.md) |

Auf Benutzerwunsch keine Änderung an Almanach-Hooks. Kein neues Datei-Guard-Muster: Hardwarediagnose ist ein Querschnittsthema ohne eindeutige Code-Dateiendung.
