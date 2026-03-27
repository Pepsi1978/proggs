# clawi-setup - Portable Identity & Memory for Clawi (OpenClaw)

Dieses Verzeichnis enthält die "Seele", Identität und das Gedächtnis von **Clawi**. Es dient dazu, Clawi über verschiedene Rechner hinweg synchron zu halten.

## 🏛️ Inhaltsübersicht
- **SOUL.md**: Die Grundwerte und der Vibe von Clawi (Deutsch, professionell, erklärend).
- **IDENTITY.md**: Name, Emoji (🦖) und Identitätsmerkmale.
- **DIREKTIVEN.md**: Die drei obersten Direktiven (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing).
- **memory/**: Das tägliche Gedächtnis (logs) und kuratierte Langzeit-Erinnerungen.

## 🔄 Synchronisation
Nutze die bereitgestellten Skripte, um deine lokale OpenClaw-Instanz mit diesem Repository zu synchronisieren:

### Automatisierung (Neu)
Ich habe soeben **Cron-Jobs** eingerichtet, die sicherstellen, dass meine Identität täglich gesichert wird. Zusätzlich habe ich Hooks vorbereitet, die du manuell oder über deinen System-Scheduler aufrufen kannst.

### Manuelle Synchronisation
#### Windows (PowerShell)
```powershell
powershell -NoProfile -File clawi-setup/sync-clawi.ps1 -Mode pull  # Holt Identität vom Repo
powershell -NoProfile -File clawi-setup/sync-clawi.ps1 -Mode push  # Sichert Identität ins Repo
```

### macOS / Linux (Bash)
```bash
bash clawi-setup/sync-clawi.sh pull  # Holt Identität vom Repo
bash clawi-setup/sync-clawi.sh push  # Sichert Identität ins Repo
```

---
*Erstellt am 2026-03-27 von Clawi.*
