# Globale OpenCode-Regeln (jede Session, jedes Projekt)

> Wird bei jedem OpenCode-Start aus `~/.config/opencode/AGENTS.md` geladen. Diese kompakte Datei
> enthält die immer geltenden Kernregeln; die ausführlichen Arbeitsregeln liegen im zweiten Gehirn.

## ZUERST: Repository mit origin/main synchronisieren (PFLICHT)

**Allererste Handlung jeder Session, sofern das Arbeitsverzeichnis in einem Git-Repository liegt:**
1. `git fetch origin` ausführen.
2. Mit `git rev-list --count HEAD..origin/main` prüfen, ob `origin/main` voraus ist.
3. Ist der Wert größer als 0, sofort `git rebase --autostash origin/main` ausführen.
4. Bei einem Rebase-Konflikt `git rebase --abort` ausführen und Frank fragen; nie Konflikte raten.

So beginnt jede Session auf dem neuesten Remote-Stand. Existiert kein `origin/main`, normal fortfahren.

## DANACH: Arbeitsregeln einzeln aus dem zweiten Gehirn laden (PFLICHT)

**Zweite Handlung jeder Session, vor der eigentlichen Aufgabe:**
1. `second-brain_get_category_item(category='Programmierung/Rules', index=1)` aufrufen.
2. Aus „Eintrag 1 von N“ die Gesamtzahl N ablesen und danach Index 2, 3, ... bis N einzeln laden.
3. Jeden Eintrag vollständig lesen und erst danach bestätigen: „N Regeln aus dem zweiten Gehirn eingelesen.“

**P1:** Ohne vollständigen Abruf 1 bis N ist die Session nicht arbeitsbereit. Scheitert der MCP-Abruf,
gelten die Kernregeln unten trotzdem vollständig.

**P2:** Second-Brain-Daten immer einzeln holen: ganze Kategorien per `get_category_item` nummernweise,
bekannte Einträge per `get_by_title`. Niemals eine große Kategorie mit `get_by_category` gesammelt laden;
sie kann abgeschnitten werden. `recall` ist für gezielte Themensuche erlaubt.

## Kernregeln (gelten immer, auch nach Komprimierung)

### 1. Sprache
Alle Ausgaben und sichtbares Reasoning vollständig auf Deutsch mit echten Umlauten (`ä ö ü Ä Ö Ü ß`),
nie ASCII-Ersatz. Ausnahmen: Code, Pfade, Commit-Messages und etablierte englische Fachbegriffe.

### 2. Multi-Task: ` ; ` trennt Aufgaben (KRITISCH)
` ; ` (Leerzeichen-Semikolon-Leerzeichen) trennt eigenständige Aufgaben; ein abschließender leerer Teil
zählt nicht, Semikola in Code/SQL/URLs ebenfalls nicht. Aufgaben nach Abhängigkeiten ordnen, bei
Widerspruch nachfragen, bei mindestens zwei Aufgaben kurz nummeriert anzeigen, jede vollständig
erledigen und nach jeder einzeln committen+pushen. App-Build/Install nur einmal ganz am Ende. Abschließend
prüfen, dass keine Teilaufgabe vergessen wurde. Voller Ablauf: `Programmierung/Rules`.

### 3. Nach jeder Aufgabe automatisch committen und pushen (KRITISCH)
Jede fertige Änderung (Code, Doku, Config, Regel, Fix) sofort ohne weitere Aufforderung committen und
pushen; bei Apps vor Build/Install/Deploy. Diese Regel ist die ausdrückliche dauerhafte Git-Freigabe.
Nur eigene Dateien namentlich stagen, niemals `git add -A` oder `git add .`. Format:
`git commit -m "#NNN - Description"` → `git fetch origin && git rebase origin/main` → `git push`.
Nie force-pushen, nie `git reset --hard` ohne Freigabe. Fremde Änderungen weder stagen noch verändern.

### 4. Secrets
Keine API-Keys, Tokens oder Passwörter ins Repo, auch nicht in Doku, Kommentaren oder Tests. Secrets
liegen in `~/SK/<projekt>/`; im Repo nur redaktierte Templates.

### 5. Komprimierung
Diese Kernregeln gelten nach Compact/Compress unverändert. Zusammenfassungen müssen auf Deutsch mit
echten Umlauten sein und alle offenen ` ; `-Aufgaben bewahren.

### 6. Arbeitsmodi und proportionale Quality Gates (KRITISCH)
Ohne Modusangabe gilt **Schnellmodus**. Frank kann einen Modus für eine Aufgabe oder bis auf Widerruf
setzen. Ein Gate-Durchlauf ist genau ein Aufruf eines read-only Reviewers. Findings blockieren nur bei
Auftragsbezug, fehlschlagendem Build/Test, Sicherheit, Datenintegrität oder reproduzierbarer Regression;
entfernte Randfälle werden notiert, erweitern den Auftrag aber nicht automatisch.

- **Schnellmodus (Standard):** kleinster korrekter Fix, keine allgemeine Härtung, fokussierte Tests, KEIN Gate.
  Auf externe read-only Reviews vollständig verzichten, damit die Aufgabe schnell abgeschlossen wird.
- **Normalmodus:** proportionaler Fix und relevante Regressionstests; breites, strikt
  auftragsbezogenes Gate, höchstens 2 Durchläufe, bei Grün sofort stoppen. Nach dem zweiten roten Lauf
  offene Punkte zusammenfassen und Frank fragen.
- **Gründlichkeitsmodus:** verwandte Fehlerklassen und sinnvolle Härtung mitprüfen; Quality-Gate-
  Durchläufe ohne feste Obergrenze. Auftragsbezogene Findings beheben, deterministisch testen und das
  Gate wiederholen, bis es grün ist. Bei einer objektiv nicht auflösbaren externen Blockade Frank fragen.

Immer: Schnell- und Normalmodus halten ihre festen Gate-Grenzen ein; nur der Gründlichkeitsmodus läuft
bewusst bis Grün. Keine unbeauftragte zweite Großaufgabe, keine falsche PASS-Meldung. Deterministische
Tests nach Korrekturen müssen grün sein; Restunsicherheit ehrlich nennen.
Geschwindigkeit ist eine Qualitätsdimension und wird gegen Absicherung balanciert.

### 7. Bei jedem Bug/Fehler: Direktive 3 (KRITISCH)
Bei jedem Bug, Fehler, fehlgeschlagenen Befehl oder Zuruf „Direktive 3“ den Skill
`resilient-bugfixing` laden; er lädt den vollständigen Originaltext. Eine Kurzfassung nie als vollständig
oder wörtlich ausgeben. Root Cause finden, Funktionalität erhalten (nichts entfernen,
auskommentieren oder in leerem Catch schlucken), verifizieren und dokumentieren. Die Direktive ist
proportionale Orientierung, keine pauschale Blockerliste oder Scope-Erlaubnis. Ihre Prüfpunkte nach dem
aktiven Modus gewichten; nur auftragsbezogene Risiken, Build-/Testfehler, Sicherheit, Datenverlust und
echte Regressionen blockieren, alles andere höchstens vorschlagen.

Nach objektiv bestätigtem Bugfix zusätzlich einzeln per `second-brain_remember` dokumentieren: Titel
`Bugfix <App> <Bereich> <YYYY-MM-DD HH:MM>`, passende Kategorie `bugfixes/<unterkategorie>`; bei nur
subjektiv prüfbaren Fixes vorher einmal „Hat der Fix funktioniert?“ fragen.

### 8. Anti-Halluzination (KRITISCH)
1. Bei Unsicherheit „Ich weiß es nicht“ statt raten.
2. Tool-first: Datei, API, Config und Projektzustand vor Aussagen mit `read`/`grep`/`list` prüfen.
3. Nach einer unverifizierten Vermutung stoppen; keine Kette darauf aufbauen.
4. Erkannte Irrtümer sofort zurückziehen.
5. Quellen als Datei/Zeile oder Tool-Ausgabe nennen; keine Funktionen, Imports, Pakete oder Keys erfinden.
Das Plugin `tool-first-guard` warnt bei Änderungen ohne vorherigen Read; Volltext:
`best-practices/agents/anti-halluzination-regeln.md`.

### 9. Bekannte Bugs und Best Practices zuerst (KRITISCH)
Vor echter Arbeit an einem technischen Bereich (nicht bei Planung oder Kleinkram) just-in-time einzeln:
1. `second-brain_recall("<Bereich> Almanach Kurzcheck")` lesen.
2. `second-brain_recall("<Bereich> Best Practices Kurzcheck")` lesen.
3. Tritt ein Fehler auf, den passenden Volltext einzeln per `get_by_title`/`recall` lesen.
Gibt es keinen passenden Eintrag, Frank kurz melden. Nie große Kategorien gesammelt laden. Hintergrund:
`bugs/SYSTEM.md` und `best-practices/`.
