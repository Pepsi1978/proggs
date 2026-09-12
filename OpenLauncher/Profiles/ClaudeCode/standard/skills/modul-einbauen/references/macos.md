# Einbau auf macOS und iOS — Swift / SwiftUI (M3.x, M4.x)

## Wohin die Dateien kommen

```
<App>/Module/<Kurzname>/
├── <Modul>.swift     ← byte-identische Kopie
└── Anbindung.swift   ← app-eigen
```

## Der wichtigste Schritt: Zielmitgliedschaft

Bei einem **Xcode-Projekt** reicht Kopieren in den Ordner **nicht**. Die
Dateien müssen dem Ziel (Target) zugeordnet werden, sonst werden sie
kommentarlos nicht kompiliert — und der Fehler sieht aus wie „Typ nicht
gefunden", nicht wie „Datei fehlt".

Prüfen, ob die Dateien in der `project.pbxproj` unter den Build-Phasen des
Ziels auftauchen. Bei einem **Swift Package** genügt der Ordner, weil dort das
Dateisystem die Quelle ist.

## Namenskollisionen

Swift kennt keine Namensräume innerhalb eines Ziels — alle Typen liegen flach
nebeneinander. Vor dem Kopieren prüfen, ob ein Typname des Moduls in der
Ziel-App schon existiert. Kollidiert etwas, ist das ein Fall für
`modul-erstellen` (Modul mit Präfix versehen, Version +1), keine stille
Umbenennung in der Kopie — die würde die Byte-Identität zerstören.

## Mindestversion prüfen (Phase 1)

```
grep -rn "MACOSX_DEPLOYMENT_TARGET\|IPHONEOS_DEPLOYMENT_TARGET\|swift-tools-version\|platforms:" <App>
```

## Aussehen aus der Ziel-App holen

In die Anbindung, nie ins Modul:

| Was | Woher |
|---|---|
| Farben | die `Color`-Werte bzw. `xcassets` der Ziel-App |
| Schrift | `Font`-Definitionen der Ziel-App |
| Texte | `LocalizedStringKey` der Ziel-App |
| Dienste und Speicher | Protokoll-Umsetzungen in der Anbindung |

## Zusätzlich bei iOS

Braucht das Modul Berechtigungen oder `Info.plist`-Schlüssel, stehen die unter
„Host muss liefern" im Manifest. Die App trägt sie ein — das Modul fragt nie
selbst nach einer Berechtigung.

## Abnahme

`diff -r` (Anbindung ausgenommen) muss leer sein. Danach die App durchbauen —
`xcodebuild` mit dem Schema der App, bei einem Swift Package `swift build`.
Dann Regel 9 für die App.
