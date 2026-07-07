# Claude Code kennen lernen

## Tag 1 – Git & Claude Code Grundlagen

---

### 1. Git vs. GitHub

| | **Git** | **GitHub** |
|---|---------|-----------|
| **Was** | Programm/Werkzeug | Website/Plattform |
| **Wo** | Lokal auf deinem Rechner | Im Internet (github.com) |
| **Zweck** | Versionsverwaltung | Code teilen & zusammenarbeiten |
| **Erfunden** | 2005 von Linus Torvalds | 2008, als "Social Media für Code" |

Git funktioniert komplett ohne GitHub. GitHub ist nur ein Ort, wohin man Code hochladen kann.

---

### 2. Repository (Repo)

- Ein **Projektordner**, der von Git überwacht wird
- Enthält einen versteckten **`.git/`-Ordner** – dort speichert Git die gesamte Geschichte (alle Commits, alle Branches)
- Kann **nur lokal**, **nur auf GitHub** oder **an beiden Orten** existieren
- Wird erstellt durch `git init` (lokal) oder auf GitHub ("New Repository")
- Der `.git/`-Ordner enthält keine lesbaren Dateien – niemals manuell darin Dateien löschen oder ändern!

---

### 3. Branches (Zweige)

- Branches sind ein **Git-Konzept**, kein GitHub-Konzept – sie existieren zuerst lokal
- Du bist **immer** auf genau einem Branch – es gibt keine "branchlose" Situation
- Ein **Feature-Branch** ist einfach ein Branch für eine bestimmte neue Funktion (nur eine Namenskonvention)
- Branch-Namen werden in Claude Code Desktop **automatisch** erstellt (z.B. `claude/learn-claude-code-Nfcsb`)
- Alte, gemergte Branches auf GitHub können gelöscht werden – nach dem Mergen sind alle Änderungen in `main` enthalten
- Der lokale Branch und der GitHub-Branch heißen **immer gleich**
- Beim **ersten Push** wird der Branch auf GitHub **automatisch erstellt**

---

### 4. Commit – der Schnappschuss

- Ein Commit ist wie **"Speichern" in Word** – er speichert den aktuellen Stand lokal
- Commits **stapeln sich** auf demselben Branch – sie überschreiben sich **nicht**
- Jeder Commit speichert **nur die Änderung** (den Unterschied zum Vorgänger), nicht das ganze Projekt
- Die **Dateien auf der Festplatte** sind aber immer der vollständige, aktuelle Stand
- Commits brauchen ihre **gesamte Kette** (alle Vorgänger) – deshalb niemals alte Commits löschen
- Ohne Commit gehen Zwischenschritte **verloren** – regelmäßig committen ist wichtig
- Jeder Commit hat eine **eindeutige ID** (z.B. `f9955ca`) und eine **Nachricht** (z.B. `#001 - Login erstellt`)

**Wichtig:** Es werden nur **Dateiänderungen** gespeichert – Chatverläufe oder Unterhaltungen werden nicht in Git gespeichert!

---

### 5. Push und Pull

```
Push = Hochladen zu GitHub        ⬆️  (lokal → GitHub)
Pull = Herunterladen von GitHub   ⬇️  (GitHub → lokal)
```

- Push geht **immer in den aktuellen Branch** – nie heimlich woanders hin
- Git **verweigert** einen Push, wenn GitHub neuere Commits hat → erst pullen, dann pushen
- Wenn du lokal mehr Commits hast als GitHub → Push funktioniert normal, GitHub bekommt die neuen dazu
- Beim Pullen eines älteren Stands passiert nichts – Git sagt: "Du hast schon alles"

---

### 6. Pull Request (PR)

| Aktion | Bedeutung |
|--------|-----------|
| **PR erstellen** | "Hier sind meine Änderungen, bitte prüfen" |
| **PR reviewen** | "Ich schaue mir die Änderungen an" – das ist **nicht** rückgängig machen! |
| **PR mergen** | "Änderungen sind gut, ich übernehme sie in main" |

- Geht meistens von einem Feature-Branch → nach `main`
- Kann über den **PR-Button** in der App oder durch Claude Code erstellt werden – beides führt zum gleichen Ergebnis

---

### 7. Zu einem früheren Commit zurückkehren

| Option | Beschreibung | Sicherheit |
|--------|-------------|------------|
| **A: Nur anschauen** | Stand eines alten Commits anzeigen, ohne etwas zu ändern | Sicher |
| **B: Zurück + löschen** | Alles nach dem gewählten Commit wird gelöscht | Gefährlich! |
| **C: Zurück ohne löschen** | Neuer Commit wird erstellt, der den alten Stand wiederherstellt – alle bisherigen Commits bleiben erhalten | Sicher (empfohlen) |

Man kann jederzeit sagen: "Zeig mir alle Commits" → dann einen auswählen → "Gehe zu Commit X zurück, ohne zu löschen"

---

### 8. Commit-Nachrichten benennen

- Jeder Commit bekommt eine **fortlaufende Nummer**: `#001`, `#002`, `#003`, ...
- Die Nummerierung wird **automatisch** von Claude Code vergeben
- Man kann einen **eigenen Namen** vergeben: "Committe mit dem Namen Wassermelone" → `#003 - Wassermelone`
- Ohne eigenen Namen erstellt Claude Code **automatisch** eine aussagekräftige Beschreibung
- Diese Regel ist in der **CLAUDE.md** festgehalten und gilt für alle zukünftigen Sitzungen

---

### 9. Claude Code Desktop vs. Terminal

In der Claude Code Desktop App muss man **keine Terminal-Befehle kennen**. Man schreibt in normaler Sprache:

| Statt Terminal-Befehl | Sagt man einfach |
|---|---|
| `git commit -m "..."` | "Committe den aktuellen Stand" |
| `git push` | "Pushe das auf GitHub" |
| `git log` | "Zeig mir alle Commits" |
| `git checkout` | "Gehe zu Commit X zurück" |
| `git status` | "Auf welchem Branch sind wir?" |
| `git init` | "Erstelle ein Git-Repository" |

Claude Code führt die Befehle im Hintergrund aus – man sieht jede Ausführung und kann sie genehmigen oder ablehnen.

---

### 10. Was Git NICHT speichert

```
Was Git speichert:              Was Git NICHT speichert:
✓ Code-Dateien                  ✗ Chatverläufe
✓ CLAUDE.md                     ✗ Unterhaltungen
✓ README.md                     ✗ Erklärungen von Claude
✓ Skripte, Bilder, etc.        ✗ Fragen des Benutzers
```

Damit Wissen dauerhaft erhalten bleibt, muss es in eine **Datei** geschrieben werden (wie diese hier) – dann kann es committet und gepusht werden.
