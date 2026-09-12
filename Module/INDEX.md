# Modul-Bibliothek

Wiederverwendbare Code-Bausteine, aus fertigen Apps herausgelöst.
Angelegt und gepflegt vom Skill `modul-erstellen`.

## Nummernkreise

| Kreis | Plattform | Sprache / Technik              | Ordner            |
|-------|-----------|--------------------------------|-------------------|
| M1.x  | Android   | Kotlin / Jetpack Compose       | `Module/Android/` |
| M2.x  | Windows   | C# / .NET WPF                  | `Module/Windows/` |
| M3.x  | macOS     | Swift / SwiftUI                | `Module/macOS/`   |
| M4.x  | iOS       | Swift / SwiftUI                | `Module/iOS/`     |

Ordnername ist autoritativ: `Mx.y-Kurzname`. Die nächste freie Nummer ergibt
sich aus der höchsten vorhandenen im jeweiligen Kreis, plus eins.

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

_Noch keine Module angelegt._
