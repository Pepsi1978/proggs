# Cortex — Komplett-Backup & Wiederherstellung (lokal, auf Franks PC)

**Ziel:** Ein **server-unabhängiges Komplett-Backup** des zweiten Gehirns auf **diesem PC** — mit
**einem Klick**. Geht der Server (VPS) komplett kaputt, kann er mit diesem Backup **schnell wieder
genau wie vorher** aufgesetzt werden. Kein kostenpflichtiges Hostinger-Snapshot nötig.

> **Warum „auf dem PC ziehen" statt Dashboard-Knopf?** Ein Knopf im Dashboard läuft *auf dem Server*
> und könnte nur auf das Z-Laufwerk schreiben — das ist weg, sobald der Server tot ist. Deshalb
> **zieht dieser PC aktiv** (per SSH). So liegt das Backup auf deiner lokalen Platte, unabhängig vom
> Server-Zustand.

---

## Was wird gesichert? (alles Unersetzliche)

| Inhalt | Was es ist |
|--------|-----------|
| **Qdrant-Gehirn-Snapshot** | Alle Einträge + Vektoren der Collection `brain` (konsistenter Snapshot über die API) |
| **`.env`** | Alle Secrets (Gemini-, Qdrant-, SB-, OpenCode-, Groq-Key) |
| **`agent-data/`** | Die 3 System-Prompts (Haupt/Speicher/Abfrage), Modell-Wahl, Kategorie-Registry |
| **`compose.yaml`, `Caddyfile`, Code** | Der komplette Bauplan (`/opt/second-brain` ohne qdrant-data/.git/Logs) |
| **Caddy-Volumes** | Interne CA + Zertifikate (HTTPS fürs Cockpit) |
| **`wg0.conf`** | WireGuard-Tunnel-Konfiguration (falls als root lesbar) |

Das Ubuntu-System selbst wird **nicht** gesichert (ist in ~20 Min frisch aufgesetzt). Der komplette
*Aufbau* steckt im Bauplan oben + im GitHub-Repo `Pepsi1978/proggs`.

---

## Einmalige Einrichtung (einmal pro PC)

### 1. OpenSSH-Client (meist schon da)
Windows: *Einstellungen → Apps → Optionale Features* → **„OpenSSH-Client"** installieren, falls nicht
vorhanden. (Test: in PowerShell `ssh -V` eingeben.)

### 2. SSH-Schlüssel (empfohlen — dann kein Passwort beim Backup)
In PowerShell:
```powershell
ssh-keygen -t ed25519 -f "$env:USERPROFILE\SK\second-brain\id_cortex" -C "cortex-backup"
# (Bei "Passphrase" einfach Enter drücken für passwortlos.)
# Öffentlichen Schlüssel auf den Server bringen (einmalig, fragt nach dem Server-Passwort):
type "$env:USERPROFILE\SK\second-brain\id_cortex.pub" | ssh ROOT@SERVER_IP "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
# Danach die Windows-ACL idempotent absichern und den echten SSH-Zugriff testen
pwsh -NoProfile -File .\set-cortex-ssh-key-acl.ps1 `
  -KeyPath "$env:USERPROFILE\SK\second-brain\id_cortex" -Server SERVER_IP
```
(`ROOT@SERVER_IP` und `SERVER_IP` durch deinen echten Nutzer beziehungsweise die echte IP ersetzen.
Den Block aus dem `windows`-Ordner des Repos ausführen.)

### 3. Konfig-Datei `backup.env` anlegen
Datei: `%USERPROFILE%\SK\second-brain\backup.env` (liegt **außerhalb** des Repos — Secrets-Regel).
Inhalt (Werte anpassen):
```
SERVER_HOST=DEINE_VPS_IP        # öffentliche VPS-IP  ODER  10.8.0.1 (wenn SSH über WireGuard läuft)
SSH_USER=root
SSH_PORT=22
SSH_KEY=%USERPROFILE%\SK\second-brain\id_cortex
REMOTE_APP_DIR=/opt/second-brain
LOCAL_DEST=C:\CortexBackup
```

### 4. Sicherstellen, dass die Skripte auf dem Server liegen
`scripts/full-backup-create.sh` + `scripts/full-restore.sh` liegen im Repo. Beim nächsten Server-Deploy
(`git pull` in `/opt/second-brain`) sind sie automatisch da. Einmal ausführbar machen:
```bash
chmod +x /opt/second-brain/scripts/full-backup-create.sh /opt/second-brain/scripts/full-restore.sh
```

---

## Backup machen (der Klick)

**Doppelklick auf `Cortex-Backup.cmd`** (oder als Stream-Deck-Knopf: *System → Öffnen* auf diese Datei).

Es öffnet sich ein Fenster, das zeigt: Tunnel-Check → Server-Backup anstoßen → Archiv ziehen. Danach
liegt das Backup unter `C:\CortexBackup\JJJJ-MM-TT_HHMMSS\cortex-full-….tar.gz` und der Ordner öffnet
sich automatisch. Fertig.

> Tipp: Mach das regelmäßig (z. B. wöchentlich) und bewahre **mindestens eine Kopie auf einer externen
> Platte** auf — ein Backup nur auf dem PC ist bei PC-Defekt auch weg.

---

## Wiederherstellen (Server ist tot → neu aufsetzen)

1. **Frisches Ubuntu** auf dem (neuen) Server. Docker + Compose installieren:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```
2. **Backup-Archiv hochladen** (von diesem PC):
   ```powershell
   scp -i "$env:USERPROFILE\SK\second-brain\id_cortex" "C:\CortexBackup\…\cortex-full-….tar.gz" root@NEUE_IP:/root/
   ```
3. **Restore-Skript holen** (aus dem Repo) und ausführen:
   ```bash
   # Repo klonen (enthält full-restore.sh) ODER das Skript aus dem Archiv nutzen:
   git clone https://github.com/Pepsi1978/proggs.git
   sudo bash proggs/second-brain-server/scripts/full-restore.sh /root/cortex-full-….tar.gz
   ```
   Das Skript stellt `/opt/second-brain` wieder her, startet den Stack (`docker compose up -d --build`)
   und spielt das **Qdrant-Gehirn** aus dem Snapshot ein. Mit „JA" bestätigen.
4. **WireGuard** auf dem neuen Server einrichten (aus der gesicherten `wg0.conf`), damit der Tunnel
   und damit Dashboard/Agent wieder erreichbar sind.

Danach ist alles wieder **genau wie vorher** — inklusive aller Einträge, Kategorien und der 3 Prompts.

---

## Fehlersuche

| Problem | Lösung |
|---------|--------|
| „Konfig fehlt: …backup.env" | Schritt 3 oben — Datei anlegen |
| „WireGuard-Tunnel nicht erreichbar" | Tunnel verbinden (sind Z:/Y: im Explorer da?), dann erneut |
| Fragt nach Passwort | SSH-Key nicht eingerichtet/falscher Pfad (Schritt 2) — funktioniert trotzdem mit Passwort |
| „Server lieferte keinen Archiv-Pfad" | Auf dem Server `chmod +x …/scripts/full-backup-create.sh`; Qdrant/Docker laufen? |
| OpenSSH fehlt | Schritt 1 — OpenSSH-Client-Feature installieren |
