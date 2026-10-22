# Codex-Modellkataloge und Denkstufen

- Ursache: Viele unabhängige Modelllisten enthielten noch Sol 5.6, Sol 6 und Luna 5.6; dynamische Kontokataloge liefern diese weiter als sichtbar. Aktualisierte Startlisten allein bereinigen keine gespeicherten Auswahlen und Caches.
- Maßgeblich: lokaler Codex-Kontokatalog, geladen am 22.10.2026 um 19:41 UTC. Neueste Familien: gpt-6-astra, gpt-6.1-sol, gpt-5.6-terra, gpt-6-luna. Astra, Sol und Terra: low, medium, high, xhigh, max, ultra; Luna ohne ultra.
- Absicherung: sichtbare Kataloge auf die vier aktuellen Kennungen begrenzen; gespeicherte Kennungen beim Lesen innerhalb ihrer Familie migrieren; Denkstufen modellabhängig anzeigen und ausgehende Werte normalisieren. Claude- und sonstige Anbieter bleiben getrennt.
- Verwandte Stellen: Android-Enums, Kompass-Einstellungen mit eigenen Modellen, NewsKompass-Livekatalog, Cortex-App/Server/Dashboard, Desktop-Kataloge, Recherche-Modellwahl und WerftStudio-Providerkatalog.
- Werkzeughinweis: Eine einzeilige JSON-Datei wie models_cache.json kann die 65.536-Byte-Grenze des Grep-Werkzeugs überschreiten. Für die gezielte JSON-Projektion einen Parser verwenden, statt die vollständige Zeile zu suchen.
