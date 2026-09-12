# iOS — Swift / SwiftUI (Kreis M4)

**Stand: noch kein Gerät, noch kein iOS-Projekt im Bestand.** Dieser
Nummernkreis ist reserviert, aber unbenutzt.

Die Sprache ist dieselbe wie bei macOS, deshalb gilt bis auf Weiteres
`references/macos.md` — Abgrenzung über Typpräfix, Ablage unter
`Module/iOS/M4.y-Name/src/`, Kopie in die App unter `<App>/Module/<Kurzname>/`.

## Zusätzliche Nabelschnüre gegenüber macOS

| Was | Empfohlener Schnitt |
|---|---|
| `UIApplication.shared` | Rückruf oder Protokoll |
| Berechtigungen (Kamera, Mikrofon, Standort) | Modul fragt nie selbst — die App reicht das Ergebnis herein |
| `Info.plist`-Einträge | unter „Host muss liefern" ins Manifest, mit genauem Schlüsselnamen |
| Sicherheitsbereich und Notch | Parameter statt fester Abstände |
| Haptik (`UIFeedbackGenerator`) | darf im Modul bleiben, aber abschaltbar über Parameter |

## Beim ersten iOS-Modul

Vor dem Herauslösen am dann vorhandenen Projekt ablesen: Namenskonvention,
Mindest-Zielversion, Ablageort im Xcode-Projekt, Baubefehl. Anschließend diese
Datei damit füllen und diesen Abschnitt entfernen.

Nicht raten — falsch geratene Konventionen fallen erst beim zweiten Einbau auf,
und dann müssen alle bereits verteilten Kopien nachgezogen werden.

## Abnahme

Gegen den Simulator durchbauen (`xcodebuild` mit dem Schema der App und der
`iphonesimulator`-SDK). Grün heißt: Modul steht. Danach Regel 9 für die App.
