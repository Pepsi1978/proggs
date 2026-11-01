# Sonnenanimation – 01.11.2026

- Symptom: ruckartige Sonne beim Sprechen und Zuhören.
- Ursache in `JarvisKugel.Zeichne`: ungeglättete Audiopegel, hochfrequente Schwingungen bei nur 30 Timer-Bildern pro Sekunde und absolute Zeit mal wechselndes Tempo (Phasensprung). Zuhör-Ringe erschienen beim Phasenumlauf unmittelbar mit hoher Deckkraft.
- Änderung: bildschirmsynchroner Takt für aktive Kugeln, zeitabhängige Pegel- und Zustandsfilter, integrierte Bewegungsphase, langsamere Schwingungen und an beiden Enden ausgeblendete Ringe.
- Absicherung: ungültige Pegel fallen auf null zurück, die Bildzeit ist nach Pausen begrenzt; Rendering- und App-Events werden bei Unsichtbarkeit beziehungsweise Entladen abgemeldet. Timer-Animationen anderer Komponenten bleiben beim bisherigen Takt.
- Verwandte Stellen: `KugelLive` und `LiveKugel` verwenden dieselbe Grundlage und erhalten die Glättung. Hintergrund und Sprachpille haben eigene Takte; keine Änderung daran erforderlich. Windows-spezifische Rendering-Korrektur, keine neue plattformübergreifende Funktion.
- Funktionserhalt: Zustandserkennung, Farben, Pegelreaktion, Mikrofon, Wellen und Antippen bleiben erhalten. Der Startbereich wird per Layout-Abstand um 76 geräteunabhängige Punkte versetzt und bleibt scrollbar. Die visuelle Abnahme erfolgt durch Frank im Schnellmodus.
