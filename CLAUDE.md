# CLAUDE.md - Persönliche Richtlinien

## Terminal-Ausführung

Wenn Terminal-Befehle ausgeführt werden sollen, führe diese **immer direkt selbst aus** (über das Bash-Tool), anstatt dem Benutzer Zeilen zum Kopieren zu geben. Claude Code hat direkten Zugriff auf das Terminal und soll diesen auch nutzen.

- **MacOS**: Terminal verwenden
- **Windows**: Terminal / PowerShell verwenden

Der Benutzer sieht jede Ausführung und kann sie über das Berechtigungssystem genehmigen oder ablehnen.

## Installationsanleitung in der README

Bei jeder Aufgabe, die ein neues Projekt, Tool oder Skript erstellt oder verändert, muss eine **Schritt-für-Schritt-Installationsanleitung** in der zugehörigen `README.md` erstellt oder aktualisiert werden. Dabei gelten folgende Regeln:

- **Zielgruppe**: Windows-Benutzer, die Anfänger bei der Installation von KI-programmierten Programmen sind
- **Voraussetzungen**: Alle nötigen Programme und Tools auflisten (z.B. Python, Node.js, Git, etc.), mit Erklärung **warum** jede Voraussetzung benötigt wird
- **Download-Links**: Direkte Links zu den offiziellen Download-Seiten jeder Voraussetzung angeben
- **Schritt-für-Schritt**: Jeden Installations- und Einrichtungsschritt einzeln erklären, mit konkreten Befehlen für Windows (PowerShell/Terminal)
- **Erklärungen**: Bei jedem Schritt erklären, **warum** dieser Schritt wichtig ist und was er bewirkt
- **Reihenfolge**: Die Anleitung muss in der richtigen Reihenfolge aufgebaut sein – erst Voraussetzungen, dann Installation, dann Konfiguration, dann Start
- **Fehlerbehebung**: Häufige Probleme und deren Lösungen am Ende der Anleitung auflisten
