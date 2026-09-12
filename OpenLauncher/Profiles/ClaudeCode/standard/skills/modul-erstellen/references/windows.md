# Windows — C# / .NET WPF (Kreis M2)

## Namensraum

`Module.<Kurzname>` — z. B. `Module.SchwebendesFenster`.

Abgelesen am Bestand: Apps benutzen ihren eigenen Projektnamen als Wurzel
(`TerminalVoiceOverlay`, `TerminalVoiceOverlay.Models`, `.Views`). Der
Modul-Namensraum bleibt bewusst außerhalb dieser Wurzeln, damit eine Kopie in
einer fremden App nie mit deren Typen kollidiert.

In C# muss der Namensraum **nicht** dem Ordner entsprechen, die Kopie bleibt
also byte-identisch. Der Ordner spiegelt ihn trotzdem, weil sich das leichter
liest.

## Ablage

| | Pfad |
|---|---|
| Modul | `Module/Windows/M2.y-Name/src/` |
| Kopie in der App | `<App>/Module/<Kurzname>/` |

Die Dateien werden über den üblichen Glob des SDK-Projekts automatisch
mitkompiliert — nur wenn die `.csproj` ein ausdrückliches `<Compile Include=…>`
benutzt, muss dort ein Eintrag ergänzt werden. Erst nachsehen, dann handeln.

## Typische Nabelschnüre

| Was | Woran erkennbar | Empfohlener Schnitt |
|---|---|---|
| App-Ressourcen | `{StaticResource X}`, `ResourceDictionary` der App | `DependencyProperty` bzw. Parameter |
| Zugriff auf `App.Current` | `((App)Application.Current).X` | Parameter im Konstruktor |
| Singletons / Dienste | `Dienst.Instanz` | Schnittstelle hereinreichen |
| Einstellungen | `Properties.Settings.Default` | Schnittstelle `IEinstellungen` |
| Lokalisierte Texte | `Resx`, `.resources` | Parameter `string` |
| Fest verdrahtete Pfade | `%APPDATA%\<AppName>` | Pfad als Parameter |
| P/Invoke | `[DllImport]` | Darf im Modul bleiben — gehört zur Sache, nicht zur App |

## Mindestversion feststellen

Das Zielframework aus der `.csproj` ablesen:

```
grep -n "TargetFramework\|<UseWPF>\|LangVersion" <App>/*.csproj
```

Trag das gefundene Zielframework als `Mindestens:` ins Manifest.

## Abnahme

Die App im Debug-Profil durchbauen (`dotnet`-CLI auf die `.csproj` der App).
Grün heißt: Modul steht.

Danach Regel 9 für die App — `<Version>` in der `.csproj` bumpen, committen,
pushen, deployen.

> **Achtung bei TVO und CVO:** Dort läuft Bauen und Deployen ausschließlich über
> `~/proggs/rebuild-overlay.ps1` (Regel 17). Niemals von Hand bauen und den
> laufenden Prozess abschießen — das Skript prüft den Exit-Code, erkennt einen
> Sofortabsturz der neuen Fassung und fragt vorher per Dialog nach.
