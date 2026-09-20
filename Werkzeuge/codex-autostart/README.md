# Codex Desktop mit Administratorrechten starten

Version 1.2.0 — 20.09.2026, 12:24 Uhr

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
6. Im `-Background`-Modus: wartet auf das Fenster und postet **`WM_CLOSE`**. Codex geht
   daraufhin selbst ins Benachrichtigungsfeld und bleibt dort voll bedienbar.

Läuft Codex bereits erhöht und wird die Desktop-Verknüpfung geklickt, **startet der Launcher
die `.exe` ein zweites Mal**. Electrons Single-Instance-Sperre meldet das der laufenden
Instanz, die ihr Fenster daraufhin selbst zeigt; der zweite Prozess beendet sich.

Mit `-Neustart` beendet der Launcher eine laufende Instanz ohne Rückfrage und startet
sichtbar neu — für den Fall, dass Codex hängt oder unerhöht läuft.

Ergebnis jedes Starts landet in `%LOCALAPPDATA%\CodexAutostart\last-launch.json`.

## Prüfen, ob es wirkt

```powershell
Get-CimInstance Win32_Process -Filter "Name='ChatGPT.exe'" |
    Where-Object { $_.CommandLine -notmatch '--type=' } |
    Select-Object ProcessId, CommandLine
```

Ist die **Kommandozeile leer**, obwohl die Prozesse laufen, ist das der Beweis: Ein
normaler Benutzerprozess darf die Kommandozeile erhöhter Prozesse nicht lesen.

## Die wichtigste Regel: Finger weg vom Fenster

**Niemals `ShowWindow` oder `SetForegroundWindow` auf das Electron-Fenster anwenden.**

Electron verwaltet Sichtbarkeit und Eingabe-Routing selbst. Ein per Win32 versteckt oder
sichtbar gemachtes Fenster ist für Electron unsichtbar geblieben: Das Fenster steht auf dem
Bildschirm, aber der Renderer zeichnet nicht und nimmt keine Mausklicks an — **die App sieht
offen aus und ist komplett tot**. Genau das ist in Version 1.0.x passiert (`SW_HIDE` beim
Tray-Start, danach `SW_SHOW` beim Klick auf die Verknüpfung).

Richtig ist der Weg über die App selbst:

| Ziel | Falsch | Richtig |
|---|---|---|
| Fenster zeigen | `ShowWindow(SW_SHOW)` | `.exe` erneut starten → Single-Instance-Signal, die App zeigt sich selbst |
| Fenster ins Tray | `ShowWindow(SW_HIDE)` | `WM_CLOSE` posten → die App geht selbst ins Tray und bleibt resident |

Win32 darf nur **lesen**: `EnumWindows` zum Finden, `IsWindowVisible`/`IsIconic` zum Prüfen.

## Fallstricke, die hier schon Blut gekostet haben

- **`--do-not-de-elevate` weglassen** → Codex startet unerhöht neu, der Launcher meldet
  einen Fehler, obwohl die App läuft.
- **Start über `shell:AppsFolder`** → Windows aktiviert das Paket und fällt dabei auf
  normale Rechte zurück. Nur `CreateProcess` (`UseShellExecute=false`) vererbt das Token.
- **Hauptprozess über die Kommandozeile erkennen** (`--type=` herausfiltern) → die
  Kommandozeile erhöhter Prozesse ist für normale Prozesse nicht lesbar und kommt leer
  zurück. Der Launcher erkennt den Hauptprozess deshalb daran, dass sein Elternprozess
  kein `ChatGPT.exe` ist.
- **Fenstersuche ohne Klassenprüfung** → Codex hält mehrere `Chrome_WidgetWin_1`-Fenster,
  darunter transparente Overlays (`WS_EX_TOOLWINDOW`) und ein titelloses Zweitfenster.
  Overlays, Eigentümer-Fenster und fremde Fensterklassen aussortieren, sonst erwischt man
  das falsche.
- **`$liste = Funktion-Die-Ein-Array-Liefert`** → PowerShell entrollt das Array; bei genau
  einem Treffer bleibt ein `CimInstance` übrig, das keine `.Count`-Eigenschaft hat. Die
  Prüfung läuft still ins Leere. Immer `@(...)` um den Aufruf.

## Updates

**Updates funktionieren unverändert weiter.** Codex Desktop ist ein Store-signiertes
MSIX-Paket (`SignatureKind: Store`) und wird über den Paketdienst von Windows aktualisiert —
per Store im Hintergrund oder über die UpdateZentrale, die
`winget upgrade --id 9PLM9XGG6VKS --source msstore --silent` ausführt.

Das ist völlig unabhängig davon, wie die App gestartet wurde. Der Launcher greift in den
Update-Weg an keiner Stelle ein: Er ersetzt nur den **Start**, nicht die Installation.

Nach einem Update ist nichts von Hand nachzuziehen:

| Was | Warum es das Update überlebt |
|---|---|
| Autostart-Aufgabe | zeigt auf dieses Skript, nicht auf Codex |
| Desktop-Verknüpfung | ebenso |
| Paketpfad (enthält die Versionsnummer) | wird bei **jedem** Start neu über `Get-AppxPackage` ermittelt |

**Der eine Haken, den der Launcher abfängt:** Ein MSIX-Update kann nicht in einen laufenden
Paketordner geschrieben werden. Windows legt die neue Version daneben — die laufende Instanz
arbeitet weiter aus dem alten Ordner. Da Codex per Autostart durchgehend läuft, bliebe das
Update sonst bis zum nächsten Hochfahren wirkungslos.

Deshalb vergleicht der Launcher bei jedem Start den Pfad der laufenden Instanz mit dem
aktuellen Paketpfad. Weichen sie ab, fragt er beim Klick auf die Verknüpfung:

> Codex wurde aktualisiert, es läuft aber noch die alte Fassung. Soll Codex jetzt neu
> gestartet werden, damit das Update wirksam wird?

Im Autostart-Modus (`-Background`) wird nie ungefragt beendet.

### Was die UpdateZentrale betrifft

Der Katalogeintrag `codex-desktop` steht auf `beendenVorUpdate: false` und
`neuStartenNachUpdate: false` — die UpdateZentrale fasst die laufende App also nicht an und
startet sie auch nicht neu. Das ist gut so: Ihr Neustart liefe über
`shell:AppsFolder\…` und damit **ohne** Administratorrechte.

Zwei Dinge dort deshalb nicht einschalten:

- **„Autostart als geplante Aufgabe"** für Codex. Die UpdateZentrale legt eine eigene Aufgabe
  `UpdateZentrale - codex-desktop` an — es gäbe zwei Autostart-Einträge nebeneinander.
- **„Nach dem Update neu starten"** für Codex, solange der Neustart über `shell:AppsFolder`
  läuft. Codex liefe danach unerhöht.

## Wenn OpenAI das Manifest ändert

Sollte eines Tages `allowElevation` im Manifest stehen, ließe sich wieder der saubere Weg über
`shell:AppsFolder` nutzen; nötig ist das nicht.
