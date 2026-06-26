# Globale OpenCode-Regeln (gelten in JEDER Session, JEDEM Projekt)

> Diese Datei wird bei jedem OpenCode-Start automatisch geladen
> (`~/.config/opencode/AGENTS.md`). Sie gilt unabhaengig vom Arbeitsverzeichnis.

---

## 1. Sprache: IMMER komplett Deutsch mit echten Umlauten (KRITISCH)

- **Alle Ausgaben auf Deutsch.** Antworten, Erklaerungen, Statusmeldungen,
  Zusammenfassungen, Code-Kommentare auf Deutsch — ausnahmslos.
- **Der gesamte sichtbare Denkvorgang / das Reasoning ist IMMER auf Deutsch.**
  Das ist genauso wichtig wie die Antwort selbst. Jeder laufende Gedanke, jede
  Zwischenueberlegung, jeder Planungsschritt ("Ich mache jetzt dies, dann teste
  ich das, danach pruefe ich jenes") wird auf Deutsch mit Umlauten formuliert —
  niemals auf Englisch. Es darf NIE vorkommen, dass die Antwort deutsch ist, der
  Denkvorgang darueber aber englisch. Beide sind durchgehend deutsch.
- **Echte deutsche Umlaute verwenden:** `ä ö ü Ä Ö Ü ß` — niemals die
  ASCII-Ersatzschreibweisen `ae oe ue ss`. Also "für" statt "fuer",
  "größer" statt "groesser", "löschen" statt "loeschen".
- Diese Regel gilt fuer den gesamten Text. Auch bei langen Antworten,
  Tabellen und Listen niemals in ASCII-Umlaute zurueckfallen.

### Einzige Ausnahmen (hier bleibt Englisch/ASCII erlaubt)
- Code-Variablen, Funktionsnamen, technische Bezeichner (API, JSON, MCP …)
- Dateinamen und Verzeichnispfade (Cross-Platform-Kompatibilitaet)
- Git-Commit-Messages (englische Repo-Konvention)
- Echte englische Fachbegriffe ohne sinnvolle Uebersetzung

---

## 2. Mehrere Aufgaben pro Eingabe: Semikolon-Trenner ` ; ` (KRITISCH)

Wenn in einer Eingabe die exakte Zeichenfolge **Leerzeichen-Semikolon-Leerzeichen**
( ` ; ` ) vorkommt, signalisiert das: die Eingabe enthaelt **mehrere eigenstaendige
Aufgaben**, die nacheinander und vollstaendig abgearbeitet werden muessen.

### Schritt 1 — Erkennen
- Eingabe am Muster ` ; ` aufteilen. Jeder nicht-leere Teil ist eine Aufgabe.
- Ein abschliessendes ` ; ` am Ende (ohne Text danach) erzeugt nur einen leeren
  Teil und zaehlt **nicht** als Aufgabe.
  - `Mach X ;` = 1 Aufgabe.   `Mach X ; Mach Y ;` = 2 Aufgaben.
- **Wichtig:** Nur ` ; ` mit Leerzeichen auf beiden Seiten ist ein Trenner.
  Semikola in Code, SQL oder URLs (z. B. `const x = 5;`) sind **kein** Trenner.

### Schritt 2 — Sortieren (Pre-Flight, vor der ersten Aenderung)
Aufgaben werden **nicht** stur in Eingabe-Reihenfolge abgearbeitet. Vorher kurz
pruefen (dauert Sekunden):
- **Gruppieren:** Betreffen mehrere Aufgaben dieselbe Datei / dasselbe Feature?
  → zusammen erledigen, damit sie sich nicht gegenseitig ueberschreiben.
- **Abhaengigkeiten:** Baut Aufgabe B auf Aufgabe A auf? → A zuerst.
- **Reihenfolge festlegen:** erst die Grundlagen, dann was darauf aufbaut.
- **Konflikt erkennen:** Widersprechen sich zwei Aufgaben an derselben Stelle
  (z. B. "Header blau" vs. "Header gruen")? → **kurz nachfragen**, nicht blind
  beides nacheinander bauen.
- Weicht die sinnvolle Reihenfolge von der Eingabe-Reihenfolge ab, das in
  **einem Satz** mitteilen ("Ich mache Aufgabe 3 zuerst, weil Aufgabe 1 darauf aufbaut").

### Schritt 3 — Anzeigen
Bei 2 oder mehr Aufgaben zu Beginn der Antwort eine kurze, nummerierte Uebersicht
in der geplanten Abarbeitungs-Reihenfolge zeigen:

```
Ich habe N Aufgaben erkannt:
1. <Kurzbeschreibung>
2. <Kurzbeschreibung>
3. <Kurzbeschreibung>
Ich arbeite sie der Reihe nach ab.
```

### Schritt 4 — Abarbeiten (eine nach der anderen, gruendlich)
- Aufgaben **sequenziell** erledigen — eine vollstaendig fertig, dann die naechste.
- Jede Aufgabe **gruendlich** umsetzen (nicht oberflaechlich abhaken).
- **Nach JEDER abgeschlossenen Teilaufgabe sofort committen und pushen**
  (`git commit` + `git push`). Jede fertige Teilaufgabe ist ein eigener Commit.
  Niemals alle Aufgaben am Ende in einen Sammel-Commit werfen.
- So ist jede Teilaufgabe sofort gesichert, bevor die naechste beginnt.

### Schritt 5 — Bauen & Installieren (nur bei Apps, einmal am Ende)
Wenn die Aufgaben eine App betreffen (z. B. eine Android-App auf dem Handy oder
eine laufende Desktop-App), wird **erst nach der letzten Teilaufgabe EINMAL**
gebaut und installiert — nicht nach jeder einzelnen Aufgabe.

Standard-Ablauf:
1. Neue App bauen (z. B. die APK erzeugen).
2. Falls die App auf dem Geraet/Rechner gerade laeuft: kurz **stoppen**.
3. Die neue Version **installieren** (z. B. APK aufs Handy via ADB).
4. Die App danach **automatisch neu starten**.

Bei reinen Nicht-App-Aufgaben (Texte, Configs, Skripte) entfaellt dieser Schritt.

### Schritt 6 — Verifizieren (ganz am Ende)
- Die urspruengliche Aufgabenliste durchgehen: Ist **wirklich jede** Aufgabe
  erledigt — besonders die in der Mitte einer langen Liste?
- Uebergangene oder nur halb erledigte Aufgabe jetzt noch fertig machen,
  bevor "fertig" gemeldet wird.

### Was niemals passieren darf
- Einen Multi-Task-Prompt als nur eine Aufgabe missverstehen.
- Nur die erste oder nur die letzte Aufgabe erledigen, den Rest "vergessen".
- Eine Aufgabe in der Mitte ueberspringen.
- Zwei sich widersprechende Aufgaben blind nacheinander bauen, statt nachzufragen.

---

## 3. Nach jeder Komprimierung / Zusammenfassung (Compact / Compress) (KRITISCH)

Wenn der Gespraechsverlauf komprimiert oder zusammengefasst wird (Compact /
Compress / Summarize), MUESSEN diese globalen Regeln **danach weiterhin
vollstaendig gelten** — sie werden durch die Komprimierung nicht aufgehoben.

- **Als Allererstes nach einer Komprimierung** diese AGENTS.md-Regeln erneut
  bewusst beachten, BEVOR die Arbeit fortgesetzt wird.
- Die Komprimierung darf diese Regeln nicht "vergessen" lassen. Konkret bleibt
  ununterbrochen aktiv:
  1. **Sprache UND Denkvorgang:** weiterhin komplett Deutsch mit echten Umlauten
     (`ä ö ü ß`). Das gilt ausdruecklich fuer den gesamten sichtbaren Denkvorgang
     (Abschnitt 1) — jeder Gedanke, jede Zwischenueberlegung, jeder "Ich mache
     jetzt …"-Schritt bleibt auch nach der Komprimierung auf Deutsch. Das Modell
     darf nach einem Compress NICHT ins englische Denken zurueckfallen.
  2. **Multi-Task:** weiterhin den ` ; `-Trenner erkennen, sortieren und
     Aufgaben einzeln gruendlich abarbeiten (Abschnitt 2).
- **Die Zusammenfassung selbst MUSS komplett auf Deutsch mit echten Umlauten
  verfasst werden** — niemals auf Englisch. Das ist KRITISCH: Die Sprache der
  juengsten Zusammenfassung bestimmt, in welcher Sprache danach weitergearbeitet
  und gedacht wird. Eine englische Zusammenfassung laesst das Modell faelschlich
  ins Englische kippen. Darum ist die Zusammenfassung IMMER deutsch.
- **An den Anfang der Zusammenfassung** gehoert ein kurzer Hinweis:
  "Globale Regeln gelten weiter: (1) komplett Deutsch mit Umlauten inkl. Denkvorgang,
  (2) ` ; `-Multi-Task-Erkennung mit Sortierung." Damit ueberleben die Kern-Regeln
  die Komprimierung sichtbar im verdichteten Kontext.
- Offene, noch nicht erledigte Aufgaben aus einem ` ; `-Multi-Task-Prompt
  duerfen durch die Komprimierung nicht verloren gehen — sie bleiben in der
  Zusammenfassung erhalten und werden danach fertig abgearbeitet.

---

## 4. Direktive #3 — Resilient Bugfixing (bei JEDEM Bug/Fehler, KRITISCH)

Kernprinzip: Ein Bugfix ist nicht fertig, bis er zukunftssicher ist. Jeder Fehler
wird zur Immunisierung — er darf nie zweimal auftreten.

- **Root Cause statt Symptom:** die tiefste Ursache finden (mind. 3x "warum?"),
  nicht nur das sichtbare Problem zukleistern.
- **Funktionalitaet ERHALTEN:** ein Fix darf NIEMALS Funktionen entfernen,
  auskommentieren oder mit leerem `try/catch` verschlucken, um Fehler zu
  unterdruecken. Reparieren, nicht wegnehmen.
- **Dokumentieren:** jeden gefixten Bug festhalten (Ursache + Loesung), damit das
  Wissen erhalten bleibt.

**Bei JEDEM Fehler/Bug — ODER wenn der Benutzer sinngemaess "fixe das nach
Direktive 3", "Direktive 3", "nach Direktive 3" oder "Direktive #3" sagt — den
Skill `resilient-bugfixing` ueber das `skill`-Tool laden**
(`skill({ name: "resilient-bugfixing" })`). Bei der ausdruecklichen Phrase ist das
Laden PFLICHT, immer, ausnahmslos.

Der Skill laedt beim Aktivieren selbst den **vollstaendigen Originaltext** der
Direktive #3 (`~/.claude/rules/resilient-bugfixing.md`, 617 Zeilen) und arbeitet
danach. So wird der Volltext NUR bei Skill-Nutzung geladen (token-sparend) —
aber dann VOLLSTAENDIG.

- **NIEMALS eine verkuerzte Fassung verwenden, zitieren oder als "vollstaendig"
  bzw. "woertlich" ausgeben.** Wird die Direktive zitiert, gilt der vollstaendige
  617-Zeilen-Originaltext woertlich — niemals eine Kurzfassung.
