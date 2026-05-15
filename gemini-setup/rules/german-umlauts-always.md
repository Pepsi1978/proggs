# Deutsche Umlaute immer verwenden (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-04-23. Gilt ab SOFORT in JEDER Session
> ab Session-Start — ohne Ausnahme, ohne explizite Aktivierung.

---

## Grundregel

In ALLEN deutschen Ausgaben und Programmierarbeiten MÜSSEN echte deutsche Umlaute
und Sonderzeichen verwendet werden:

- **ä** statt "ae"
- **ö** statt "oe"
- **ü** statt "ue"
- **Ä** statt "Ae"
- **Ö** statt "Oe"
- **Ü** statt "Ue"
- **ß** statt "ss" (wo orthographisch ß korrekt ist)

ASCII-Substitutionen sind in deutschen Texten VERBOTEN.

---

## Wo die Regel gilt

| Bereich | Umlaute Pflicht? |
|---------|-----------------|
| Chat-Antworten an den Benutzer auf Deutsch | **JA** |
| Code-Kommentare auf Deutsch | **JA** |
| UI-Strings (strings.xml, Resources, etc.) | **JA** |
| README-Dateien auf Deutsch | **JA** |
| Dokumentation auf Deutsch | **JA** |
| Neue Agents, Skills, Commands auf Deutsch | **JA** |
| Memory-Einträge | **JA** |
| Commit-Messages auf Deutsch (selten) | **JA** |
| Fehlermeldungen/Logs auf Deutsch | **JA** |

## Ausnahmen (ASCII weiterhin erlaubt)

| Bereich | Grund |
|---------|-------|
| Dateinamen und Verzeichnispfade | Cross-Platform-Kompatibilität |
| Code-Variablen und Funktionsnamen | Konvention: Bezeichner auf Englisch |
| Commit-Messages auf Englisch | Standard-Konvention im Repo |
| URLs, Git-Hashes, technische IDs | Keine natürlichsprachlichen Texte |
| Englische Texte | Haben keine Umlaute |
| Bestehende Regel-Dateien mit "ae/oe/ue" | Werden nicht retroaktiv umgeschrieben (zu invasiv) |

---

## Beispiele

### Richtig

- "Ich führe den Befehl für dich aus."
- "Das Feature wurde geändert und funktioniert jetzt zuverlässig."
- "Größe: 42 MB — schön kompakt."
- "Übersetzung für alle unterstützten Sprachen."

### Falsch

- ❌ "Ich fuehre den Befehl fuer dich aus."
- ❌ "Das Feature wurde geaendert und funktioniert jetzt zuverlaessig."
- ❌ "Groesse: 42 MB — schoen kompakt."
- ❌ "Uebersetzung fuer alle unterstuetzten Sprachen."

---

## Was NIEMALS passieren darf

- ❌ Deutsche Antwort mit "ae/oe/ue/ss" statt Umlauten
- ❌ Neue deutsche Datei (Skill, Agent, Doku) ohne Umlaute erstellen
- ❌ UI-Text auf Deutsch ohne Umlaute schreiben
- ❌ Die Regel "vergessen" bei langen Antworten oder Tabellen

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `communication-and-language.md` | Ergänzt: Dort wird "komplett auf Deutsch" verlangt — diese Regel präzisiert WIE das Deutsch aussehen muss |
| `german-skill-triggers.md` | Skill-Trigger-Texte werden nicht retroaktiv geändert (bestehende Infrastruktur), aber neue Einträge nutzen Umlaute |
| `language-rules.md` | Betrifft Code-Sprachen, nicht Natursprache — kein Konflikt |
