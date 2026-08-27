# Keystore-Sync — gemeinsamer Debug-Keystore über alle Rechner

**Ziel:** Auf Windows **und** macOS dieselbe Debug-Signatur, damit jede App auf beiden
Rechnern gebaut und über bestehende Installationen hinweg aufs Handy gebracht werden kann.

## Das Prinzip

Alle Android-Apps nutzen **einen gemeinsamen Debug-Keystore** (`debug-shared.keystore`,
SHA256 beginnt mit `012d8ce4…`). Dieser ist identisch mit dem ursprünglichen
Windows-Standard-Debug-Keystore — deshalb mussten die meisten Apps **nicht** neu signiert
werden.

- Der Keystore liegt **nie im Git** (nur Build-Dateien sind versioniert).
- Quelle der Wahrheit pro Rechner: `~/SK/<App>/`
- Transport/Backup zwischen Rechnern: **Laufwerk Y** (`\\10.8.0.1\daten\Keystore`,
  über WireGuard), auf macOS gemountet als `/Volumes/daten`.

## Ordnerstruktur auf Y (`Y:\Keystore\`)

| Ordner | Inhalt |
|--------|--------|
| `<App>/` | `debug-shared.keystore` (gemeinsam) + app-spezifische Secrets |
| `BestJournalAndroid/` | zusätzlich `release.keystore` + `keystore.properties` (**kritisch, unwiederbringbar**) |
| `_GEMEINSAM/` | der eine gemeinsame Debug-Keystore (Referenz) |
| `_WindowsStandard/` | Kopie des Windows-Standard-Debug-Keystores |
| `_backup-alte-einzelschluessel/` | die früheren App-eigenen Debug-Keystores (vor der Vereinheitlichung) |

## Welche App nutzt welche Datei

| App | Keystore-Datei in `~/SK/<App>/` | Code-Änderung nötig war |
|-----|----------------------------------|--------------------------|
| BestJournalAndroid | `debug-shared.keystore` | nein |
| BestJournalFrank | `debug-shared.keystore` | nein |
| EntropieReductor | `entropiereductor.debug.keystore` | nein (nur Datei-Inhalt) |
| CortexAndroid | `debug-shared.keystore` | **ja** (Debug-Signing ergänzt) |
| NEMS | `debug-shared.keystore` | **ja** (Debug-Signing ergänzt) |
| VoiceKey | `debug-shared.keystore` | nein (Datei fehlte nur) |
| Gedankenspeicher | `debug-shared.keystore` | **ja** (Debug-Signing ergänzt, 27.08.2026) |

## Auf macOS einrichten

```bash
# 1. Y mounten (Finder: smb://10.8.0.1/daten) — oder:
mkdir -p /Volumes/daten && mount_smbfs //10.8.0.1/daten /Volumes/daten
# 2. Skript ausführen:
bash ~/proggs/keystore-sync/setup-keystores-macos.sh
```

## Wichtig: einmalige Neuinstallation auf den Handys

Drei Apps hatten vorher einen **anderen** Debug-Schlüssel und wurden umgestellt:
**BestJournalAndroid-Testversion**, **EntropieReductor** und **Gedankenspeicher**.

Bei **Gedankenspeicher** war der alte Einzelschlüssel nirgends gesichert — weder in `~/SK`
noch in `_backup-alte-einzelschluessel/`. Die Version auf dem Handy trägt
`SHA-256 171034c5…` / `SHA-1 7c:51:08:7f:01:e0:a4:90:43:eb:2f:8b:55:d5:d6:71:74:b3:d3:1e`.
Taucht diese Datei auf einem Rechner noch auf, gehört sie nach
`_backup-alte-einzelschluessel/`; sonst hilft nur die einmalige Neuinstallation — **vorher
in der App sichern** (Einstellungen → Sicherung, F-17), die Notizen kommen nur darüber zurück. Kommt beim Installieren
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` / „Signatures do not match", die App auf dem Gerät
**einmal deinstallieren** und neu installieren. Daten kommen über Geräte-Sync (EntropieReductor)
bzw. Backup (BestJournal) zurück. Der **Release-/Play-Store-Schlüssel ist nicht betroffen.**
