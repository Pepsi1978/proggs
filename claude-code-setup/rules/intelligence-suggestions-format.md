# Intelligenz-Vorschlaege: Ausfuehrlich und verstaendlich (KRITISCH)

## Regel

Intelligenz-Vorschlaege duerfen NIEMALS als technische Einzeiler formuliert werden.
Jeder Vorschlag muss 6-7 Zeilen lang sein, in einfacher Sprache die auch ein
Nichtprogrammierer versteht.

## Format (PFLICHT)

```
💡 **Intelligenz-Vorschlag N**: [Titel — kurz und klar]

**Was ist passiert:** [In einfachen Worten beschreiben was aufgefallen ist]
**Warum ist das ein Problem:** [Erklaeren welche Auswirkung das hat — Zeit, Fehler, Risiko]
**Was schlage ich vor:** [Konkreter Vorschlag, verstaendlich formuliert]
**Warum wird das System dadurch intelligenter:** [Erklaeren wie das den Compound Intelligence Effect staerkt]
Soll ich das umsetzen?
```

## Beispiel — FALSCH (zu kurz, zu technisch)

```
💡 **Intelligenz-Vorschlag 1**: Race Condition bei Service-Start → Service-Registry implementieren — Soll ich das umsetzen?
```

## Beispiel — RICHTIG (verstaendlich, begruendet)

```
💡 **Intelligenz-Vorschlag 1**: Zwei Programme starten gleichzeitig den gleichen Dienst

**Was ist passiert:** Beim Starten der Session haben ein Hook und ein Plugin beide versucht,
den gleichen Hintergrund-Dienst zu starten. Das fuehrt zu einem Konflikt weil beide glauben,
sie waeren dafuer zustaendig.
**Warum ist das ein Problem:** Der Dienst startet manchmal doppelt, manchmal gar nicht.
Das fuehrt zu zufaelligen Fehlern die schwer zu finden sind weil sie nicht jedes Mal auftreten.
**Was schlage ich vor:** Eine zentrale Stelle einrichten die entscheidet wer den Dienst startet,
so dass es nie mehr zu einem Konflikt kommen kann.
**Warum wird das System dadurch intelligenter:** Eine ganze Kategorie von zufaelligen Fehlern
faellt weg. Das System wird zuverlaessiger und braucht weniger Debugging-Zeit.
Soll ich das umsetzen?
```

## Warum

Der Benutzer ist kein Programmierer. Technische Einzeiler wie "Race Condition bei
Service-Start fixen" sagen ihm nichts. Er will verstehen WAS das Problem ist, WARUM
es ein Problem ist, und WIE das System dadurch intelligenter wird. Erst dann kann er
sinnvoll entscheiden ob er "Ja, umsetzen" sagt.
