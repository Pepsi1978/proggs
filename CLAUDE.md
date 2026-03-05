# CLAUDE.md - Persönliche Richtlinien

## Terminal-Ausführung

Wenn Terminal-Befehle ausgeführt werden sollen, führe diese **immer direkt selbst aus** (über das Bash-Tool), anstatt dem Benutzer Zeilen zum Kopieren zu geben. Claude Code hat direkten Zugriff auf das Terminal und soll diesen auch nutzen.

- **MacOS**: Terminal verwenden
- **Windows**: Terminal / PowerShell verwenden

Der Benutzer sieht jede Ausführung und kann sie über das Berechtigungssystem genehmigen oder ablehnen.

## Formatierung von Terminal-/PowerShell-Anweisungen

Wenn Terminal- oder PowerShell-Befehle in README-Dateien oder anderen Dokumentationen angegeben werden, gelten folgende Regeln:

- **Ein Befehl pro Code-Block**: Niemals mehrere Befehle in einen mehrzeiligen Code-Block zusammenfassen, da das Einfügen mehrzeiliger Blöcke im Terminal zu Problemen führt
- **Erläuterung vor jedem Befehl**: Vor jedem Code-Block eine kurze Erklärung schreiben, was der Befehl tut und warum er nötig ist
- **Separater Kopier-Button**: Jeder Befehl steht in seinem eigenen Code-Block, damit ein eigener Kopier-Button verfügbar ist

**Beispiel für das korrekte Format:**

Ins Projektverzeichnis wechseln:

```
cd mein-projekt
```

Abhängigkeiten installieren:

```
npm install
```

Projekt starten:

```
npm start
```

**Falsch** (nicht verwenden):

```
cd mein-projekt
npm install
npm start
```

## Installationsanleitung in der README

Bei jeder Aufgabe, die ein neues Projekt, Tool oder Skript erstellt oder verändert, muss eine **Schritt-für-Schritt-Installationsanleitung** in der zugehörigen `README.md` erstellt oder aktualisiert werden. Dabei gelten folgende Regeln:

- **Zielgruppe**: Windows-Benutzer, die Anfänger bei der Installation von KI-programmierten Programmen sind
- **Voraussetzungen**: Alle nötigen Programme und Tools auflisten (z.B. Python, Node.js, Git, etc.), mit Erklärung **warum** jede Voraussetzung benötigt wird
- **Download-Links**: Direkte Links zu den offiziellen Download-Seiten jeder Voraussetzung angeben
- **Schritt-für-Schritt**: Jeden Installations- und Einrichtungsschritt einzeln erklären, mit konkreten Befehlen für Windows (PowerShell/Terminal)
- **Erklärungen**: Bei jedem Schritt erklären, **warum** dieser Schritt wichtig ist und was er bewirkt
- **Reihenfolge**: Die Anleitung muss in der richtigen Reihenfolge aufgebaut sein – erst Voraussetzungen, dann Installation, dann Konfiguration, dann Start
- **Fehlerbehebung**: Häufige Probleme und deren Lösungen am Ende der Anleitung auflisten

## Synchronisation der CLAUDE.md

Die CLAUDE.md existiert sowohl **lokal** im Repository als auch **auf GitHub** (via git push). Bei jeder Änderung an der CLAUDE.md müssen **immer beide Versionen synchron gehalten werden**:

- Nach jeder Bearbeitung der CLAUDE.md muss die Änderung **committet und gepusht** werden, damit die GitHub-Version aktuell bleibt
- Neue Anweisungen oder Regeländerungen dürfen **niemals nur lokal** gespeichert werden
- Der Workflow ist: Datei bearbeiten → committen → pushen

## Sprache

Sämtliche Inhalte in der CLAUDE.md müssen **immer auf Deutsch** verfasst werden. Das gilt auch für:

- Alle Anweisungen und Regeln
- Commit-Nachrichten
- Pull-Request-Titel und -Beschreibungen
- Kommentare in Dokumentationen

Es darf **niemals** eine andere Sprache als Deutsch verwendet werden.
