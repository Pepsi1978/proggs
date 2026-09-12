# Format der Zeile in Module/INDEX.md

Eine Zeile pro Modul, unter der Überschrift `## Module`, nach Nummer sortiert.
Das Muster ist bewusst dasselbe wie in `MEMORY.md`: kurz genug, dass die ganze
Bibliothek auf einen Blick lesbar bleibt.

```
- **M1.1** DragReorder **v3** — Umsortieren per Langdruck in einer LazyColumn, mit Randscrollen und weicher Ablege-Animation. Braucht: Compose BOM 2025.04. Konsumenten: 1 (alle auf v3)
- **M1.8** Drag & Drop Modul **v5** — Karten per Ziehen umsortieren. Braucht: Compose BOM 2025.04. Konsumenten: 2 (1 auf v4)
```

## Bestandteile

| Teil | Regel |
|---|---|
| `**M1.1**` | Nummer, fett, genau wie im Ordnernamen |
| `Drag & Drop Modul` | der **Anzeigename**, wörtlich wie vom Benutzer vergeben |
| `**v5**` | der **Modulstand der Bibliothek**, fett. Wandert bei jeder neuen Version mit |
| Beschreibung | ein Satz, was es tut — nicht wie es gebaut ist |
| `Braucht:` | die Mindestversion aus dem Manifest |
| `Konsumenten:` | Anzahl der Apps, nicht ihre Namen (die stehen im Manifest) |
| `(alle auf v5)` | sind alle Apps auf dem Bibliotheksstand. Sonst: `(1 auf v4)` — so viele hinken hinterher, auf welchem Stand sie stehen |

## Anzeigename, nicht Ordnername

Im Index steht der Name so, wie der Benutzer ihn gesprochen hat — mit
Leerzeichen und `&`, falls er ihn so genannt hat. Der bereinigte Ordnername
(`M1.8-Drag-und-Drop-Modul`) steht im Manifest unter **Ordner**.

Grund: Der Index wird gelesen, nicht ausgeführt. Wer nach einem Modul sucht,
sucht nach seinem eigenen Wort, nicht nach der werkzeugfesten Fassung.

Der Platzhaltersatz „_Noch keine Module angelegt._" wird beim ersten Eintrag
entfernt.

## Warum der Stand mit in die Zeile gehört

Ohne ihn beantwortet der Index nur „was gibt es?", nicht „wo liegt Arbeit?".
Man müsste jedes Manifest einzeln öffnen, um zu sehen, ob eine App hinterherhinkt
— und genau das tut niemand. Mit dem Stand in der Zeile sieht man die ganze
Bibliothek auf einen Blick und weiß, wo ein „zieh nach" fällig ist.

Zwei Stellen, eine Wahrheit: Verbindlich bleibt die **Konsumententabelle** im
Manifest. Der Index ist die Übersicht, die daraus abgeschrieben wird —
widersprechen sie sich, gilt das Manifest und der Index wird berichtigt.

## Warum die Anzahl und nicht die Namen

Die Zeile soll beim Lesen die Frage „was gibt es überhaupt?" beantworten. Wer
vor einer Änderung wissen muss, **welche** Apps betroffen sind, schlägt im
Manifest nach — dort steht die Tabelle mit Stand und Pfad. Stünden die Namen
auch im Index, müssten sie an zwei Stellen gepflegt werden und würden
auseinanderlaufen.
