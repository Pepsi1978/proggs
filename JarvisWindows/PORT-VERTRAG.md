# Port JarvisMac → JarvisWindows: Vertrag für alle Teilbereiche

Ziel: **Jarvis Windows HP**, 1:1 wie `~/proggs/JarvisMac` (Swift) in Funktion, Aussehen und Texten, als .NET-10-WPF-App. Jede Swift-Datei wird zu einer C#-Datei mit gleichem Namen im gleichen Ordner (`Sources/<Bereich>/<Name>.cs`). Nichts weglassen, nichts vereinfachen, keine TODO-Attrappen: Geht etwas unter Windows wirklich nicht, baue das nächstbeste echte Gegenstück und nenne es am Ende.

## Namensregeln (damit die Bereiche ohne Absprache zusammenpassen)
- Namespace überall `JarvisWindows` (Datei-Namespace). Kein XAML: Oberfläche nur in C# gebaut.
- Typnamen wie in Swift. Ausnahme: `Mac…` wird `Win…` (`MacWerkzeuge` → `WinWerkzeuge`, `MacBedienung` → `WinBedienung`, `MacDialoge` → `WinDialoge`, `MacRechte` → `WinRechte`, `MacEinstellungenWerkzeug` → `WinEinstellungenWerkzeug`).
- Eigenschaften und Methoden: Swift-Name mit großem Anfangsbuchstaben (`frageHandy` → `FrageHandy`, `shared` → `Shared`, `zustand` → `Zustand`). Enum-Fälle ebenso (`.hoert` → `.Hoert`). Benannte Parameter bleiben klein wie in Swift.
- `async` in Swift → `Task`/`Task<T>` in C#, Name unverändert, **ohne** Async-Endung. Swift-`var x: T { get async }` → Methode `Task<T> X()`.
- `@MainActor` → läuft auf dem WPF-Hauptfaden (`Haupt.Post`, `Haupt.Auf`). Hintergrund: `Task.Run`.
- `Task.isCancelled`/Abbruch → optionaler letzter Parameter `CancellationToken ct = default`.
- `@Observable` → Klasse erbt `Beobachtbar`, Eigenschaften mit `Setze(ref f, v)`. Swift-Arrays, die die Oberfläche anzeigt → `ObservableCollection<T>`. Structs mit Änderungen in der Liste (`Nachricht`, `Schritt`) → Klassen, die `Beobachtbar` erben.
- Werkzeugnamen fürs Modell: `mac_…` → `windows_…` (`mac_befehl` → `windows_befehl`, `mac_bedienen` → `windows_bedienen`). Alle anderen bleiben (`terminal_befehl`, `openlauncher_sitzung`, `updater_zentrale`, `chatgpt_app`, `repo_lesen`, `cli_frage`, `handy_jarvis_sprechen`, `screenshot_ins_cli` …), damit das Handy sie gleich versteht.
- Texte fürs Modell und die Oberfläche: wörtlich übernehmen, nur Mac-Wörter ersetzen: „Mac“ → „Laptop“ oder „PC“, „Jarvis Mac OS“ → `AppInfo.Name` („Jarvis Windows HP“), ⌘ → Strg, ⌥ → Alt, Touch ID → Windows Hello, Finder → Explorer, „MacBook Air“ → „HP-Laptop (OmniBook Ultra Flip)“.

## Fertiger Kern (nicht ändern, nur benutzen): `Sources/Kern/Kern.cs`, `Sources/Kern/Tresor.cs`
`JSONObjekt` (= `Dictionary<string,object?>`), `JSON.Daten/Text/Objekt/Wert`, Lesehelfer `o.Text("k")`, `.Ganz`, `.Zahl`, `.Wahr`, `.Obj`, `.Liste`, `.Objekte`, `.Texte`; `Pfade` (`Heim`, `Proggs`, `Unterstuetzung`, `Gespraeche`, `Temp`, `Sk`, `Ressource`, `Mitgeliefert`); `AppInfo` (`Geraet`, `Geraetename`, `Name`, `Version`, `Stand`); `Haupt`; `Beobachtbar`; `Zugang`; `Logbuch` und global `Log(art, text, wo:, werkzeug:, eingabe:, ergebnis:, fehler:, dauerMs:)`; `ModellWahl`; `Vorgaben` (statt UserDefaults); `Einstellungen.Shared`; `Tresor` (`Lade<T>(name)`, `Sichere(wert, name)`, `Loesche`, `Sperre(...)`, `Versiegeln/Oeffnen`); `Geheimnisse`; `WerkzeugDefinition(Name, Beschreibung, Schema)`; `WerkzeugErgebnis(Text, Fehler, BildPNG)`; `LokalesWerkzeug(Definition, Ausfuehren)`. Fehlt dir eine Eigenschaft im Kern: in deiner eigenen Datei als `partial`-freie Hilfsklasse lösen, Kern nicht anfassen.

## Windows-Gegenstücke (verbindlich)
| Mac | Windows |
|---|---|
| Secure Enclave, Touch ID | DPAPI (`Tresor`), `Windows.Security.Credentials.UI.UserConsentVerifier` (Windows Hello) |
| zsh, `Process` | `pwsh` (Rückfall `powershell`), `System.Diagnostics.Process`, CreateNoWindow; Handles nicht vererben |
| AppleScript | PowerShell + `System.Windows.Automation` (Fenster, Dialoge, Knöpfe); Werkzeug `windows_powershell` statt `mac_applescript` |
| `mac_bedienen`, CGEvent | `SendInput`, Koordinaten in Bildpixeln des letzten Fotos über alle Monitore, DPI beachten |
| `mac_bildschirm`, `screencapture` | `System.Drawing` `CopyFromScreen` (PerMonitorV2), PNG |
| `mac_aufnahme` | `ffmpeg` falls da, sonst Bildfolge + Hinweis; Ziel `%USERPROFILE%\Videos\Jarvis` |
| Terminal.app | Windows Terminal (`wt.exe`), Rückfall `pwsh`-Fenster |
| OpenLauncher `--jarvis-auftrag` | `%LOCALAPPDATA%`/OpenLauncher-Exe, siehe `Jarvis/werkzeuge/pc-bruecke/jarvis-pc-bruecke.ps1` `Finde-OpenLauncher` und `Starte-Sitzung` |
| Menüleiste | `System.Windows.Forms.NotifyIcon` |
| Globale Kürzel ⌥⌘J, ⌥⌘K | `RegisterHotKey` Strg+Alt+J, Strg+Alt+K |
| Sprachpille (NSPanel, nie Fokus, nicht auf Fotos) | Fenster `WS_EX_NOACTIVATE|WS_EX_TOOLWINDOW|WS_EX_TOPMOST`, `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` |
| AVAudioEngine | NAudio (`WasapiCapture`, `WasapiOut`), 16 kHz mono für Erkennung |
| Echounterdrückung | WASAPI-Kategorie Communications mit AEC-Effekt, wenn verfügbar; sonst die vorhandene Echosperre |
| Apple Speech, SpeechAnalyzer | `Windows.Media.SpeechRecognition` als `windows`, Live-Vorschau wie bei Apple; Parakeet bleibt Vorgabe |
| sherpa-onnx C-API | NuGet `org.k2fsa.sherpa.onnx`; Modellort wie `TerminalVoiceOverlay-Windows/Services/ParakeetLocalClient.cs` |
| Systemstimme | `System.Speech.Synthesis` |
| Mitteilung | `NotifyIcon.ShowBalloonTip` über `Mitteilungen`-Helfer in `WinWerkzeuge` (`Shell.Mitteilung(titel, text)` wie Swift) |
| Liquid Glass | halbtransparente Fläche + Verlauf-Rand wie `GlasKarte`, Fenster-Hintergrund Mica/Acrylic optional |
| SF Symbols | Segoe Fluent Icons; Abbildung zentral `Sym.Zeichen(name)` in `Oberflaeche/Design.cs` |
| SwiftUI `Font.jarvis` | `Segoe UI Variable`, gleiche Größen und Gewichte |
| `~/Library/Logs/JarvisPcBruecke` | `%LOCALAPPDATA%\JarvisPcBruecke\bruecke.log` |

## Relay
`Einstellungen.Shared.Zugang.Relay` endet auf `/mcp`. Kanal dieser App: statt `<basis>/kanal` immer `<basis>/hp/kanal` und `<basis>/hp/kanal/quittung`; alles andere (MCP, `/ablage/...`) wie am Mac. User-Agent `JarvisWindows/<Version>`. Ablage-Quelle heißt `AppInfo.Name`.

## Prüfen
Nicht in `JarvisWindows/obj` bauen, andere arbeiten parallel. Eigener Lauf:
`dotnet build JarvisWindows/JarvisWindows.csproj -p:BaseIntermediateOutputPath=<eigener-scratch>/obj/ -p:OutputPath=<eigener-scratch>/bin/ --nologo -v q`
Fehler in Dateien anderer Bereiche (noch fehlende Typen) ignorieren; die eigenen Dateien müssen frei von allem außer fehlenden Fremdtypen sein. Nicht committen, das macht die Hauptsitzung. Nur eigene Dateien anlegen. Lies vorab `best-practices/desktop/dotnet-csharp-kurzcheck.md`.
