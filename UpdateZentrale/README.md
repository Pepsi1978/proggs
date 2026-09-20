# UpdateZentrale

Ein kleines Windows-Programm, das alle wichtigen Werkzeuge an einer Stelle prüft und aktualisiert:
LM Studio (inklusive Engines/Runtimes), Claude Desktop, Codex Desktop, Claude Code CLI, Codex CLI,
Stream Deck sowie die eigenen Werkzeuge OpenLauncher, TVO und CVO.

Start über die Desktop-Verknüpfung **UpdateZentrale** (`create_shortcut.ps1` legt sie an).

---

## Was das Programm macht

| Bereich | Verhalten |
|---|---|
| **Prüfen** | Pro Programm oder alle auf einmal. Zeigt installierte Version → verfügbare Version. |
| **Aktualisieren** | Einzeln oder alle offenen Updates nacheinander. |
| **Laufende Programme** | Wird erkannt (inklusive Helferprogramme). Muss ein Programm für das Update beendet werden, fragt die App vorher und startet es danach wieder, wenn es vorher lief. |
| **Autostart** | Wird pro Programm angezeigt, damit klar ist, was beim Hochfahren mitstartet. |
| **Als Administrator** | Schiebeschalter pro Programm. Setzt den Windows-Kompatibilitätsschalter, so dass Windows das Programm bei **jedem** Start erhöht ausführt – auch wenn es außerhalb der UpdateZentrale gestartet wird. |
| **Protokoll** | Rechts steht die vollständige Ausgabe des jeweiligen Update-Werkzeugs. |

### Wichtiger Hinweis zum Administratormodus

Programme mit gesetztem Administratormodus startet Windows **nicht mehr über den Autostart**
(`HKCU\...\Run`) – die Benutzerkontensteuerung blockiert das. Betrifft hier vor allem TVO und CVO.
Die App zeigt in dem Fall eine orange Warnung direkt auf der Karte.

---

## Neue Programme hinzufügen (ohne Neubau)

Alles steht in **`programs.json`** im Projektordner. Die App liest die Datei bei jedem Start und
über die Schaltfläche **Neu laden**. Ein neues Programm ist also ein weiteres Objekt in
`programme` – kein Code, kein Build.

```jsonc
{
  "id": "beispiel",                    // eindeutig, dient als Schlüssel für die Einstellungen
  "name": "Beispielprogramm",
  "gruppe": "KI-Anwendungen",          // Überschrift in der Liste; freie Wahl
  "beschreibung": "Kurztext auf der Karte.",
  "art": "winget",                     // siehe Tabelle unten
  "wingetId": "Hersteller.Paket",
  "exePfad": "%LOCALAPPDATA%/Programs/Beispiel/Beispiel.exe",
  "prozesse": ["Beispiel"],            // Hauptprozess(e), ohne .exe
  "helferProzesse": ["BeispielTray"],  // Helfer, die den Installer sonst blockieren
  "beendenVorUpdate": true,
  "neuStartenNachUpdate": true,
  "akzent": "#3FA9F5"                  // Farbe des Kennzeichens
}
```

Pfade werden mit **Schrägstrichen** geschrieben (kein Escaping nötig); `%VARIABLEN%` werden
aufgelöst und Windows bekommt intern die gewohnte Schreibweise.

### Die vier Update-Arten

| `art` | Wofür | Pflichtfelder | Prüfung | Update |
|---|---|---|---|---|
| `winget` | Alles, was winget kennt | `wingetId` | `winget list --id … ` | `winget upgrade --id … --silent` |
| `cli` | Werkzeuge, die sich selbst aktualisieren | `exePfad`, `updateArgumente` | `pruefArgumente` (Trockenlauf) **oder** `npmPaket` | `exePfad updateArgumente` |
| `msstore` | MSIX-/Store-Apps | `appxName`, `packageFamilyName` | `Get-AppxPackage` | öffnet die Store-Seite |
| `reposkript` | Eigene Werkzeuge in `~/proggs` | `skript`, `statusPraefix` | `git rev-list HEAD..origin/main` + csproj-Version gegen gebaute exe | ruft das vorhandene PowerShell-Skript auf |

Weitere optionale Felder:

| Feld | Bedeutung |
|---|---|
| `versionsArgumente` | Argumente, die die installierte Version ausgeben (z. B. `--version`) |
| `pruefArgumente` | Trockenlauf-Befehl; Zeilen mit `→` bzw. `->` gelten als geplante Updates |
| `npmPaket` | Vergleichsquelle für die neueste Version, wenn das Werkzeug auf npm liegt |
| `startArgumente` | Argumente beim Start über die Schaltfläche „Starten“ |
| `zeitlimitMinuten` | Zeitlimit des Update-Laufs (Standard 20) |
| `dialogWartezeitSekunden` | Nur `reposkript`: wie lange auf den Ja/Nein-Klick gewartet wird |
| `hinweis` | Kursive Zeile auf der Karte |
| `adminStandard` | Vorschlag für den Administrator-Schalter beim ersten Start |

---

## Besonderheiten der eingetragenen Programme

* **LM Studio Engines & Runtimes** – eigener Eintrag. Geprüft wird mit
  `lms runtime update --all --dry-run`, aktualisiert mit `lms runtime update --all --yes`. Deckt
  llama.cpp für CPU/AVX2, NVIDIA CUDA, CUDA12 und Vulkan ab. Lädt teils mehrere Gigabyte;
  LM Studio darf dabei laufen.
* **Codex Desktop vs. Codex CLI** – zwei getrennte Installationen: die Store-App (MSIX) und der
  Standalone-Build unter `~/.codex/packages`. Deshalb zwei Karten mit unterschiedlichen Wegen.
* **Claude Code CLI** – laufende Sessions werden nie beendet; das Update greift beim nächsten Start.
* **Stream Deck** – die Helferdienste werden beim Update mitbeendet, sonst hängt der Installer.
* **OpenLauncher / TVO / CVO** – die App ruft ausschließlich die vorhandenen Skripte auf
  (`update-launcher.ps1`, `rebuild-overlay.ps1`), **nie mit `-Force`**, und beendet deren Prozesse
  niemals selbst. Jedes Skript zeigt sein eigenes Ja/Nein-Fenster; die Antwort (`started`,
  `already-current`, `cancelled`, `no-answer`) wird ausgelesen und im Klartext angezeigt.

---

## Dateien

| Datei | Zweck |
|---|---|
| `programs.json` | Der Katalog. Hier werden Programme hinzugefügt. |
| `Models/` | Katalog-Eintrag und Prüfergebnis |
| `Services/` | Pfade, Prozesse, Registry (Admin/Autostart), Kommandozeile, Einstellungen, Konverter |
| `Providers/` | Je eine Implementierung pro Update-Art |
| `ViewModels/` | Karten- und Fensterlogik |
| `HauptFenster.xaml`, `Theme.xaml` | Oberfläche und Gestaltung |
| `create_shortcut.ps1` | Desktop-Verknüpfung |
| `make_icon.ps1` | Erzeugt `app.ico` (einmalig) |

Benutzereinstellungen (Administrator-Schalter) liegen außerhalb des Repos in
`%LOCALAPPDATA%\UpdateZentrale\settings.json`, damit `programs.json` sauber bleibt.

## Bauen

```powershell
cd ~\proggs\UpdateZentrale
dotnet build -c Release
pwsh -File .\create_shortcut.ps1
```
