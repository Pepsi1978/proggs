# Codex Desktop mit Administratorrechten starten

Version 1.0.0 — 20.09.2026, 11:44 Uhr

Startet **Codex Desktop** (MSIX-Paket `OpenAI.Codex`) dauerhaft mit Administratorrechten:
beim Anmelden automatisch im System-Tray und per Desktop-Verknüpfung sichtbar.

## Warum das vorher nicht funktioniert hat

Codex Desktop ist eine Electron/Chromium-Anwendung. Chromium hat unter Windows einen
eingebauten Schutz: Wird der Browser-Prozess **erhöht** gestartet, beendet er sich selbst
und startet sich sofort **unerhöht** neu ("de-elevation"). Der alte Launcher sah davon nur
den sterbenden Prozess und meldete:

> Der erhöhte Codex-Prozess wurde beendet. Möglicherweise hat eine andere Instanz den Start übernommen.

Die Meldung war korrekt — die "andere Instanz" war Codex selbst, unerhöht neu gestartet.

Chromium kennt dafür genau einen Schalter: **`--do-not-de-elevate`**. Damit bleibt der
erhöhte Prozess bestehen. Genau dieser Schalter hat gefehlt.

## Zweite Hürde: MSIX

Das Paketmanifest deklariert `runFullTrust`, aber **kein `allowElevation`**. Deshalb kann
Codex nicht über die normale Paket-Aktivierung (`shell:AppsFolder\OpenAI.Codex…!App`)
erhöht gestartet werden — Windows fällt dabei immer auf normale Rechte zurück.

Der Launcher startet die Programmdatei deshalb direkt:

```
C:\Program Files\WindowsApps\OpenAI.Codex_<Version>_x64__2p2nqsd0c76g0\app\ChatGPT.exe --do-not-de-elevate
```

Der Pfad wird zur Laufzeit über `Get-AppxPackage` ermittelt, überlebt also jedes
Codex-Update.

## Dateien

| Datei | Zweck |
|---|---|
| `Start-CodexAdmin.ps1` | Der Launcher. Ohne Schalter: sichtbares Fenster. Mit `-Background`: versteckt im Tray. |
| `Install-CodexAdminAutostart.ps1` | Richtet Autostart-Aufgabe und Desktop-Verknüpfung ein. Einmal ausführen. |
| `codex.ico` | Symbol für die Desktop-Verknüpfung. |

## Einrichtung

```powershell
& "$HOME\proggs\Werkzeuge\codex-autostart\Install-CodexAdminAutostart.ps1"
```

Das Skript holt sich die Adminrechte selbst (eine UAC-Abfrage) und legt an:

- **Aufgabenplanung:** `Codex Desktop - Start im System-Tray`
  Auslöser: Anmeldung + 20 s Verzögerung · Rechte: *Höchste* (dadurch **keine** UAC-Abfrage
  beim Hochfahren) · Aktion: `Start-CodexAdmin.ps1 -Background`
- **Desktop-Verknüpfung:** `Codex.lnk` → `Start-CodexAdmin.ps1`

## Verhalten des Launchers

1. Erhöht sich selbst, falls nötig (UAC).
2. Ermittelt Paketpfad über `Get-AppxPackage -Name OpenAI.Codex`.
3. Prüft laufende Instanzen:
   - läuft bereits **erhöht** → nichts tun;
   - läuft **unerhöht** → beim manuellen Start Rückfrage "beenden und erhöht neu starten?",
     im Autostart-Modus wird nichts beendet.
   Hintergrund: Electron lässt nur eine Instanz zu, eine unerhöhte blockiert jeden
   erhöhten Start.
4. Startet `ChatGPT.exe --do-not-de-elevate` per `CreateProcess` (`UseShellExecute=false`),
   damit das erhöhte Token vererbt wird.
5. Prüft nach 5 s, dass der Prozess lebt **und** wirklich erhöht ist.
6. Im `-Background`-Modus: versteckt das Fenster mehrfach (`SW_HIDE`), Codex bleibt über
   das Tray-Symbol erreichbar.

Ergebnis jedes Starts landet in `%LOCALAPPDATA%\CodexAutostart\last-launch.json`.

## Prüfen, ob es wirkt

```powershell
Get-CimInstance Win32_Process -Filter "Name='ChatGPT.exe'" |
    Where-Object { $_.CommandLine -notmatch '--type=' } |
    Select-Object ProcessId, CommandLine
```

Ist die **Kommandozeile leer**, obwohl die Prozesse laufen, ist das der Beweis: Ein
normaler Benutzerprozess darf die Kommandozeile erhöhter Prozesse nicht lesen.

## Nach einem Codex-Update

Nichts zu tun — der Paketpfad wird bei jedem Start neu ermittelt.

Sollte OpenAI eines Tages `allowElevation` ins Manifest aufnehmen, ließe sich wieder der
saubere Weg über `shell:AppsFolder` nutzen; nötig ist das nicht.
