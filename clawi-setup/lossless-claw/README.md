# lossless-claw Backup für Clawi

Dieses Verzeichnis enthält **nur sanitisierte Metadaten** von `lossless-claw` für die Wiederherstellung von Clawi.

## Inhalt

- `manifest.json` — Metadaten zur lokalen LCM-Datenbank und zur aktiven `lossless-claw`-Konfiguration
- `.gitignore` — verhindert, dass rohe Datenbanken versehentlich ins Repo gelangen

## Sicherheitsregel

Die rohe Datei `~/.openclaw/lcm.db` wird **nicht** nach GitHub gesichert.
Grund: Sie kann Gesprächsinhalte, Tool-Ausgaben oder Konfigurationsfragmente enthalten und damit sensible Daten leaken.

## Wiederherstellung

1. `lossless-claw` per npm/OpenClaw wieder installieren
2. OpenClaw-Konfiguration laut `manifest.json` und `RECOVERY-OPENCLAW.md` prüfen
3. Falls eine lokale LCM-Datenbank gesichert werden soll, nur außerhalb von GitHub/backups mit Zugriffsschutz
