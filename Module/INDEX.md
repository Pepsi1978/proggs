# Modul-Bibliothek

Wiederverwendbare Code-Bausteine, aus fertigen Apps herausgelöst.

| Skill | Wofür |
|---|---|
| `modul-erstellen` | App-Code → neues Modul in dieser Bibliothek |
| `modul-einbauen` | Modul → App, und Änderungen an alle Konsumenten verteilen |

## Nummernkreise

| Kreis | Plattform | Sprache / Technik              | Ordner            |
|-------|-----------|--------------------------------|-------------------|
| M1.x  | Android   | Kotlin / Jetpack Compose       | `Module/Android/` |
| M2.x  | Windows   | C# / .NET WPF                  | `Module/Windows/` |
| M3.x  | macOS     | Swift / SwiftUI                | `Module/macOS/`   |
| M4.x  | iOS       | Swift / SwiftUI                | `Module/iOS/`     |

Ordnername ist autoritativ: `Mx.y-Anzeigename`. Die nächste freie Nummer ergibt
sich aus der höchsten vorhandenen im jeweiligen Kreis, plus eins — Lücken werden
nicht nachbelegt.

## Namensgebung

**Den Namen vergibt Frank, die Nummer der Skill.** Gesagt wird zum Beispiel
„bau daraus ein Modul und nenn es Drag & Drop Modul"; fällt kein Name, fragt der
Skill nach, statt sich einen auszudenken.

Der gesprochene Name bleibt als **Anzeigename** erhalten und steht in dieser
Liste und im Manifest. Für Ordner und Namensraum wird er werkzeugfest gemacht:
`&` → `und`, Leerzeichen → `-`, Pfad-Sonderzeichen entfallen.

| | |
|---|---|
| Anzeigename | `Drag & Drop Modul` |
| Ordner | `M1.8-Drag-und-Drop-Modul` |
| Namensraum | `de.frank.module.dragunddropmodul` |

Bei einem Namen ohne Leerzeichen und Sonderzeichen passiert dabei nichts — aus
`DragReorder` wird schlicht `M1.1-DragReorder`.

## Grundsatz: Kopie, nicht Verdrahtung

Jede App bekommt eine **1:1-Kopie** der Moduldateien in ihren eigenen Baum.
Kein `srcDir`-Link, keine Paketabhängigkeit — jeder App-Ordner bleibt allein
baubar, auch ohne Netz und ohne diesen Ordner.

Die Kopie ist **byte-identisch** mit der Quelle hier. Dadurch zeigt ein
schlichtes `diff`, ob eine App noch aktuell ist, und ein Nachziehen ist reines
Überschreiben. Verteilt wird nur auf Ansage: *„zieh M1.1 nach"*.

## Module

<!-- Eine Zeile pro Modul. Format:
- **M1.1** DragReorder — Kurzbeschreibung. Braucht: Compose ≥1.7. Konsumenten: 1
-->

- **M1.1** Sicherung — Sicherung des Bestands als Datei in einen selbst gewählten Ordner: Auswahl was gesichert wird, Autosicherung nach Ruhezeit, Vorschau vor dem Einspielen, Zurücknehmen. Braucht: androidx.lifecycle, kotlinx.coroutines. Konsumenten: 3
