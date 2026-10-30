# Jarvis für macOS (JarvisMac) – Projektregeln

Native Swift/SwiftUI-App, gebaut mit `swiftc` ohne Xcode-Projekt (wie OpenLauncherMac). Bundle-ID `de.frank.jarvismac`,
installiert als `/Applications/Jarvis.app`, signiert mit „Frank Local Dev“ (sonst fragt macOS nach jedem Build die Rechte neu).

## Version (Regel 6)
Die Version steht in `Info.plist` in **zwei** Schlüsseln, die immer gleich sein müssen:
`CFBundleVersion` und `CFBundleShortVersionString`, Form `1.0.0`. Pro Commit genau einmal die letzte Stelle erhöhen.
Den Zeitstempel nie von Hand eintragen: `build.sh` setzt `BuildTimestamp` aus der echten Systemzeit
(Platzhalter `__BUILD_TIMESTAMP__` bleibt in der Quelle). Die App zeigt beides in der Seitenleiste und unter Einstellungen → Über.

## Bauen und installieren
```bash
cd ~/proggs/JarvisMac
bash build.sh check     # nur Typprüfung
bash build.sh install   # bauen, nach /Applications/Jarvis.app, starten
```
- `DEVELOPER_DIR=/Library/Developer/CommandLineTools` (Xcode-Lizenz ist nicht akzeptiert, siehe bugs/desktop/openlauncher-mac-xcode-lizenz.md).
- SwiftUI-Makros (`@State` …) gibt es nur in Xcode: `build.sh` lädt sie per `-plugin-path` aus Xcode.app – nicht entfernen.
- Neue Swift-Dateien unter `Sources/` werden automatisch mitgebaut.

## Architektur
- **Gehirn** (`Sources/Gehirn`): Werkzeugschleife über Claude (Messages API, `anthropic-version: 2026-10-21`, Prompt-Cache) oder
  OpenAI (ChatGPT-Konto mit eigener Anmeldung am Mac oder API-Schlüssel). Verlauf kanonisch im Claude-Messages-Format.
- **Gemeinsames Gedächtnis**: Alle Handy-Werkzeuge kommen per MCP über das Relay des Handy-Jarvis (`zugang.relay`, gleiche Adresse
  wie das ChatGPT-Plugin). Kein eigener Gedächtnis-Speicher am Mac: `jarvis_kontext` zu Gesprächsbeginn, `jarvis_merken` für
  Neues, `Lernen.swift` gibt nach 10 Minuten Ruhe neue Angaben aus Mac-Gesprächen zurück.
- **Mac-Werkzeuge** (`Sources/Werkzeuge`): Shell, AppleScript, Programme, Tastatur, Bildschirm, Dateien, Terminal, OpenLauncher
  (`--jarvis-auftrag`), Updater-Zentrale, ChatGPT-App. Neue Mac-Werkzeuge dort ergänzen und in `funktionen.md` eintragen.
- **Sprache** (`Sources/Sprache`), **Live** (`Sources/Live`), **Oberfläche** (`Sources/Oberflaeche`).
- Zugangsschlüssel nur verschlüsselt in `~/Library/Application Support/JarvisMac/Tresor` (`Sources/Kern/Tresor.swift`, Secure Enclave), beim ersten Start aus `~/SK` übernommen. Nie ins Git, nie im Klartext. Keine Keychain: Sie sperrt bei lokaler Signatur fremde Programme nicht aus (getestet).
- Logbuch: `Application Support/JarvisMac/Logbuch/JJJJ-MM-TT.jsonl`, höchstens 30 Tage. Neue Abläufe mit `log(art, was, wo:, werkzeug:, ergebnis:, fehler:, dauerMs:)` eintragen.

## Live-Gespräch
Vor Änderungen an `Sources/Live` `best-practices/apis/openai-live-realtime.md` §14 lesen (wie bei der Android-App).

## Funktionsdatei
`funktionen.md` nach jeder Änderung knapp nachziehen: nur tatsächlich Eingebautes. `jarvis-wissen.md` beschreibt die Handy-Reiter, die CLI-Zusammenarbeit und die Regeln für ChatGPT Live; `build.sh` legt dazu die aktuelle `Jarvis/app/src/main/assets/funktionen.md` als `handy-funktionen.md` ins Bundle.

## Farben
Nur über `Farbe` und `DesignStand` (Hell, Automatik, Dunkel), keine festen Weiß- oder Schwarztöne für Flächen: `Farbe.glas(a)` und `Farbe.tiefe(a)` passen sich an. Nicht in `Canvas`-Blöcken lesen, sondern davor. Einzige Ausnahme: die Sprachpille (`SprachPille.swift`) bleibt wie am Handy in jedem Design dunkel mit Orange.
