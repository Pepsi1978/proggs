# Effort-Auswahl ohne Netzblockade

Stand: 05.09.2026, 10:44 Uhr.

## Ursache

`LoadThinkingOptionsAsync` zeigte erst nach dem Netzaufruf Optionen an. Der letzte
erfolgreiche Stand wurde nicht gespeichert. Die Logs meldeten bei GPT-6 Astra und
Claude Opus 5[1m] einen HttpClient-Timeout nach 30 Sekunden. Der grosse models.dev-
Katalog wurde ohne automatische HTTP-Dekompression angefordert. Ein komprimierter
Abruf lieferte im selben Netz sofort die Modellinformationen.

## Fix

- Optionen und Auswahl pro Provider/Modell dauerhaft speichern, atomar unter Windows.
- Gespeicherten Stand oder lokale Vorgaben vor dem ersten await anzeigen.
- Nutzerwahl erst nach dem await erfassen, damit das Update sie nicht zuruecksetzt.
- Nur eindeutige Katalogangaben ersetzen Optionen; unbekannt ist nicht leer.
- OpenRouter direkt abfragen, nicht von der Erreichbarkeit von models.dev abhaengig machen.
- HTTP-Kompression, begrenzter Verbindungsaufbau und regelmaessige DNS-Erneuerung.
- Lokale Astra-Stufen an die empfangene Katalogantwort angleichen.
- Gleiches Cache-/Auswahlverhalten im macOS-Gegenstueck.

## Verwandte Fehlerquellen

OpenRouter: fehlende Modelle/Parameter waren bisher eine leere Stufenliste.
models.dev: unbekannte reasoning_options konnten vorhandene Stufen loeschen.
Beide Plattformen: Hintergrundantworten konnten eine inzwischen geaenderte Auswahl
ueberschreiben. Abbruch bei Modellwechsel bleibt erhalten. Explizit leere Listen
duerfen weiterhin Stufen entfernen. Profil- und Modellstandards bleiben erhalten.

## Absicherung und Grenzen

Fehler lassen die sofort angezeigte Auswahl stehen und werden protokolliert.
Unbekannte Antworten koennen den letzten Stand nicht mehr als leere Liste ersetzen
(Poka-Yoke durch Unterscheidung unbekannt/leer). Ein defekter Cache faellt auf lokale
Vorgaben zurueck. Der Cache bestaetigt Katalogdaten bzw. Auswahl, nicht erfolgreiche
Modell-Inferenz. Kein automatisierter Funktionstest im Schnellmodus; Windows wird
mit dem vorgeschriebenen Updatescript gebaut/installiert. macOS kann auf diesem
Windows-Rechner nicht gebaut oder installiert werden.
