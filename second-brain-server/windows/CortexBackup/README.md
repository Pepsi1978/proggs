# Cortex Backup — Windows-Tool (C#/WPF)

Ein kleines Windows-Programm mit Fenster, drei Knöpfen und Ordner-Auswahl, das ein
**server-unabhängiges Komplett-Backup** des zweiten Gehirns auf diesen PC zieht.

## Bauen

```powershell
pwsh -ExecutionPolicy Bypass -File publish.ps1
```

Ergebnis:
- `publish\CortexBackup.exe` — eine self-contained `.exe` (läuft ohne installiertes .NET).
- Eine Verknüpfung **„Cortex Backup"** auf dem Desktop (mit Icon) — Doppelklick startet das Tool.

## Bedienung

| Knopf | Funktion |
|-------|----------|
| **🔒 Backup jetzt** | Zieht alle Server-Daten (Gehirn-Snapshot, .env, Prompts, Configs, Volumes) in den gewählten Ordner |
| **♻ Wiederherstellen** | Zeigt die Restore-Anleitung + öffnet den Backup-Ordner (Restore selbst läuft auf dem Server) |
| **⚙ Einrichtung** | Server-Adresse, Benutzer, Port, SSH-Schlüssel eintragen (+ Schlüssel erzeugen & zum Server kopieren) |
| **Durchsuchen…** | Festplatte/Ordner fürs Backup auswählen |

Die Konfiguration wird in `%USERPROFILE%\SK\second-brain\backup.env` gespeichert (außerhalb des Repos).

## Technik

- .NET 10 / WPF, Code-Behind. Self-contained Single-File (kein Trimming/AOT — WPF-Vorgabe).
- Nutzt die serverseitigen Skripte `scripts/full-backup-create.sh` (Backup) und `scripts/full-restore.sh` (Restore).
- Pfade über `Environment.ProcessPath` (Single-File-sicher), SSH/scp über das eingebaute Windows-OpenSSH.

Build-Artefakte (`bin/`, `obj/`, `publish/`) sind gitignored.
