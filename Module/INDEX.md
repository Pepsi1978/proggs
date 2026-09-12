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

Der **fette Modulstand** hinter dem Namen ist der Stand der Bibliothek. Steht
dahinter „alle auf vN", hinkt keine App hinterher; sonst zeigt der Klammerzusatz,
wo noch Arbeit liegt — dann sagt man „zieh M1.1 nach". Die verbindliche Liste je
App steht im Manifest, nicht hier.

Die Kopie ist **byte-identisch** mit der Quelle hier. Dadurch zeigt ein
schlichtes `diff`, ob eine App noch aktuell ist, und ein Nachziehen ist reines
Überschreiben. Verteilt wird nur auf Ansage: *„zieh M1.1 nach"*.

## Module

<!-- Eine Zeile pro Modul. Format:
- **M1.1** DragReorder **v3** — Kurzbeschreibung. Braucht: Compose ≥1.7. Konsumenten: 1 (alle auf v3)
-->

- **M1.1** Sicherung **v7** — Sicherung des Bestands als Datei in einen selbst gewählten Ordner: Auswahl was gesichert wird, Autosicherung nach Ruhezeit, Vorschau vor dem Einspielen, Zurücknehmen. Braucht: androidx.lifecycle, kotlinx.coroutines. Konsumenten: 5 (1 auf v7, 4 auf v3 — Kompass-Trio und Gedankenspeicher brauchen „zieh M1.1 nach")
- **M1.2** Drag-and-Drop **v1** — Karten einer Liste per Langdruck greifen und mit dem Finger umsortieren: Nachbarn weichen weich aus, am Rand scrollt die Liste mit, beim Loslassen legt sich die Karte sanft ab. Braucht: Compose BOM 2025.01.01, Kotlin 2.1.0. Konsumenten: 1 (alle auf v1)
