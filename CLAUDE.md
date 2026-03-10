# CLAUDE.md - Persönliche Richtlinien

## Terminal-Ausführung

Wenn Terminal-Befehle ausgeführt werden sollen, führe diese **immer direkt selbst aus** (über das Bash-Tool), anstatt dem Benutzer Zeilen zum Kopieren zu geben. Claude Code hat direkten Zugriff auf das Terminal und soll diesen auch nutzen.

- **MacOS**: Terminal verwenden
- **Windows**: Terminal / PowerShell verwenden

Der Benutzer sieht jede Ausführung und kann sie über das Berechtigungssystem genehmigen oder ablehnen.

## README-Dateien

Jedes Projekt muss eine ausfuehrliche `README.md` enthalten. Diese besteht aus zwei Hauptteilen:

### 1. Programmbeschreibung

- **Was macht das Programm?** — Ausfuehrliche Erklaerung der Funktionalitaet, des Zwecks und der Features
- **Fuer wen ist es gedacht?** — Zielgruppe und Anwendungsfaelle beschreiben
- **Wie funktioniert es?** — Technische Uebersicht (welche APIs, Frameworks, Technologien werden verwendet)
- **Screenshots** — Wenn moeglich, Screenshots oder GIFs der Benutzeroberflaeche beifuegen

### 2. Installation und Einrichtung

Die Installationsanleitung muss **fuer jedes unterstuetzte System** (macOS, Windows, ggf. Linux) separat beschrieben werden. Dabei gelten folgende Regeln:

- **Zielgruppe**: Anfaenger, die mit der Installation von KI-programmierten Programmen nicht vertraut sind
- **Plattform-spezifische Abschnitte**: Getrennte Anleitungen fuer macOS und Windows mit den jeweiligen Befehlen (Terminal bzw. PowerShell)
- **Voraussetzungen pro Plattform**: Alle noetigen Programme und Tools auflisten (z.B. Xcode CLI Tools, .NET SDK, Node.js, Git, etc.), mit Erklaerung **warum** jede Voraussetzung benoetigt wird
- **Download-Links**: Direkte Links zu den offiziellen Download-Seiten jeder Voraussetzung angeben
- **Schritt-fuer-Schritt**: Jeden Installations- und Einrichtungsschritt einzeln erklaeren — Befehle duerfen logisch nach Arbeitsschritten gruppiert werden (z.B. drei Befehle fuer eine Tool-Installation in einem Block)
- **Erklaerungen**: Bei jedem Schritt erklaeren, **warum** dieser Schritt wichtig ist und was er bewirkt
- **Reihenfolge**: Erst Voraussetzungen, dann Installation, dann Konfiguration, dann Start
- **Berechtigungen und Sicherheit**: Hinweise zu noetigen Systemberechtigungen (z.B. Accessibility-Berechtigung auf macOS, Smart App Control auf Windows)
- **Fehlerbehebung**: Haeufige Probleme und deren Loesungen am Ende der Anleitung auflisten, getrennt nach Plattform

## Synchronisation der CLAUDE.md

Die CLAUDE.md existiert sowohl **lokal** im Repository als auch **auf GitHub** (via git push). Bei jeder Änderung an der CLAUDE.md müssen **immer beide Versionen synchron gehalten werden**:

- Nach jeder Bearbeitung der CLAUDE.md muss die Änderung **committet und gepusht** werden, damit die GitHub-Version aktuell bleibt
- Neue Anweisungen oder Regeländerungen dürfen **niemals nur lokal** gespeichert werden
- Der Workflow ist: Datei bearbeiten → committen → pushen

## Commit-Nachrichten

Jede Commit-Nachricht muss mit einer **fortlaufenden Nummer** beginnen, gefolgt von einer **ausführlichen Beschreibung** der Änderung. Das Format ist:

```
#NNN - Ausführliche Beschreibung was geändert wurde
```

**Regeln:**

- Die Nummerierung beginnt bei **#001** und wird fortlaufend hochgezählt
- Die Nummer wird **automatisch** von Claude Code vergeben – der Benutzer muss sie nicht selbst angeben
- Die Beschreibung nach der Nummer erklärt **was geändert wurde und warum**
- Wenn der Benutzer einen eigenen Namen für den Commit angibt (z.B. "Committe mit dem Namen Wassermelone"), wird dieser Name **nach der Nummer** eingefügt: `#NNN - Wassermelone`
- Wenn der Benutzer keinen Namen angibt, erstellt Claude Code automatisch eine aussagekräftige Beschreibung
- Die aktuelle Zählung wird anhand der bestehenden Commits im Repository ermittelt, um Doppelungen zu vermeiden

**Beispiele:**

```
#001 - Initiales Projekt erstellt mit Grundstruktur
#002 - Login-Seite mit E-Mail- und Passwort-Feld hinzugefügt
#003 - Wassermelone
#004 - Farbschema von blau auf dunkelgrün geändert
```

## Claude Code Setup-Pruefung (Automatisch)

Beim **ersten Start** von Claude Code in diesem Repository soll automatisch geprueft werden, ob alle benoetigten Plugins und Skills installiert sind. Dazu wird die Datei `claude-code-setup/manifest.json` gelesen.

**Ablauf:**

1. Pruefe ob `claude-code-setup/manifest.json` existiert
2. Vergleiche die dort gelisteten Plugins mit den aktuell installierten
3. Wenn Plugins oder Skills fehlen:
   - Dem Benutzer mitteilen: "Es fehlen X Plugins und Y Skills. Soll ich diese jetzt installieren?"
   - Nach Bestaetigung: Fehlende Plugins mit `claude plugins install` nachinstallieren
   - Custom Skills aus `claude-code-setup/skills/` nach `~/.claude/skills/` kopieren
4. Wenn alles vorhanden ist: Nichts tun, normal weiterarbeiten

**Alternativ** kann das Setup manuell ausgefuehrt werden:
- macOS/Linux: `bash claude-code-setup/setup.sh`
- Windows: `powershell claude-code-setup/setup.ps1`

## Automatisierung und Transparenz

Claude Code soll Aufgaben **vollstaendig automatisiert** ausfuehren — inklusive Testen, Verifizieren, Committen und Pushen wenn die CLAUDE.md-Regeln es erfordern. Dabei gilt:

- **Immer mitteilen was getan wurde**: Nach jeder automatischen Aktion (Commit, Push, Plugin-Installation, Build, etc.) dem Benutzer kurz berichten, was passiert ist
- **Nicht vorher fragen, aber nachher informieren**: Automatisierung bedeutet, dass Claude Code eigenstaendig handelt — aber der Benutzer muss immer wissen, was getan wurde
- **Keine stille Ausfuehrung**: Niemals Aenderungen vornehmen, ohne sie dem Benutzer mitzuteilen

## Sprache

Sämtliche Inhalte in der CLAUDE.md müssen **immer auf Deutsch** verfasst werden. Das gilt auch für:

- Alle Anweisungen und Regeln
- Commit-Nachrichten
- Pull-Request-Titel und -Beschreibungen
- Kommentare in Dokumentationen

Es darf **niemals** eine andere Sprache als Deutsch verwendet werden.
