# macOS — Swift / SwiftUI (Kreis M3)

## Namensraum

Swift kennt keine Namensräume innerhalb eines Ziels — alle Typen eines Ziels
liegen flach nebeneinander. Deshalb wird die Abgrenzung über **Typnamen** statt
über Pakete gemacht: Modultypen bekommen ein knappes Präfix, z. B.
`MDragReorderZustand` statt `DragReorderZustand`.

Grund: Ohne Präfix kollidiert ein Modultyp früher oder später mit einem
gleichnamigen Typ der Zielapp, und der Fehler taucht erst beim Einbau in die
dritte App auf.

> **Beim ersten macOS-Modul:** Am bestehenden Swift-Projekt
> (`~/proggs/TerminalVoiceOverlay-macOS`, `~/proggs/OpenLauncherMac`) ablesen,
> wie dort benannt wird, die Konvention hier eintragen und diesen Hinweis
> entfernen. Nicht raten.

## Ablage

| | Pfad |
|---|---|
| Modul | `Module/macOS/M3.y-Name/src/` |
| Kopie in der App | `<App>/Module/<Kurzname>/` |

Bei einem Xcode-Projekt müssen neu hinzugefügte Dateien in die
Zielmitgliedschaft aufgenommen werden — reines Kopieren in den Ordner reicht
dort nicht. Bei einem Swift Package genügt der Ordner.

## Typische Nabelschnüre

| Was | Woran erkennbar | Empfohlener Schnitt |
|---|---|---|
| `@Environment` der App | eigener `EnvironmentKey` | Parameter |
| `@EnvironmentObject` | App-eigenes `ObservableObject` | Parameter oder Protokoll |
| Singletons | `Verwalter.shared` | Protokoll hereinreichen |
| `UserDefaults.standard` | direkter Zugriff | Protokoll `EinstellungenSpeicher` |
| Farben und Bilder | `Color("AkzentFarbe")`, `Image("x")` | Parameter — `xcassets` fehlen in fremden Apps |
| Lokalisierung | `LocalizedStringKey`, `NSLocalizedString` | Parameter `String` |
| `Bundle.main` | Zugriff auf das App-Bündel | Parameter, oder `Bundle.module` im Package |

## Mindestversion feststellen

```
grep -rn "MACOSX_DEPLOYMENT_TARGET\|swift-tools-version\|platforms:" <App>
```

Trag die gefundene Zielversion als `Mindestens:` ins Manifest.

## Abnahme

Die App durchbauen — bei einem Xcode-Projekt über `xcodebuild` mit dem Schema
der App, bei einem Swift Package über `swift build`. Grün heißt: Modul steht.

Danach Regel 9 für die App.
