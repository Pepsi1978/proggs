# JARVIS Sprachsteuerung MCP Agentenschleifen und Projektwissen

Gesprächsdatum: 07.10.2026

Aktualisiert und in die Datenbank-Struktur überführt: 07.10.2026, 22:09 Uhr

Frank entwickelt JARVIS als persönlichen, erweiterbaren KI-Assistenten. Im Gespräch vom 7. Oktober 2026 wurden die Sprachoberfläche, die Anbindung eigener Android-Apps, Werkzeugaufrufe und Agentenschleifen besprochen. Bevorzugter erster Aufbau: JARVIS auf dem Handy, zunächst mit einer Verbindung zur Aufgaben-App. Die Anbindung an ChatGPT soll zuerst mit wenigen Funktionen getestet werden.

## Sprachoberfläche und vollständiger Gesprächskontext

Frank möchte längere Gespräche und Recherchen weiterhin über die in seinem ChatGPT-Vertrag enthaltene Sprachfunktion führen. Eine eigene kostenpflichtige Sprachverbindung in JARVIS soll möglichst vermieden oder auf nötige Nutzung beschränkt werden. Als mögliche Planungsgröße nannte er 30 Minuten täglich; das ist kein gemessener Bedarf. Die im Gespräch genannten Minutenpreise und Monatskosten sind keine gesicherte Budgetgrundlage und müssen vor einer Implementierung aktuell geprüft werden.

Franks zentraler Wunsch ist, sämtliche besprochenen Informationen einschließlich Nutzerbeiträgen, Antworten und Korrekturen vollständig an JARVIS zu übertragen. JARVIS soll selbst relevante Informationen, Erinnerungen und Handlungsaufträge erkennen. Ein allgemeines Kontext- oder Auftragswerkzeug kann übergebene Texte verarbeiten, garantiert aber keine lückenlose Übertragung des gesamten ChatGPT-Live-Gesprächs. Für kontrollierte Erfassung des Gesprächsverlaufs ist eine eigene Sprachoberfläche mit Zugriff auf die Gesprächsereignisse der belastbarere Ansatz.

## Geplanter Anschluss an die Handy Apps

ChatGPT soll als Gesprächsoberfläche dienen; ein persönliches JARVIS-Plugin soll Aufträge an JARVIS weiterleiten. JARVIS soll die eigenen Apps über programmierte Schnittstellen anbinden. Zunächst ist die Aufgaben-App vorgesehen, später Ideen-App, Journal und Entropie-Reduktor. Die Apps werden durch ihre Installation auf demselben Handy nicht automatisch für ChatGPT oder JARVIS zugänglich.

MCP bedeutet Model Context Protocol, ein standardisiertes Verfahren zum Bereitstellen und Aufrufen externer Werkzeuge. Der MCP-Dienst kann grundsätzlich auf dem Handy laufen; in ChatGPT wird die Verbindung eingerichtet. Ein Tunnel könnte den lokalen Dienst aus der Cloud erreichbar machen. Der konkrete Android-Tunnelbetrieb, die Kontoverfügbarkeit und Lese- und Schreibaufrufe über den gewünschten ChatGPT-Sprachmodus müssen praktisch geprüft werden. Die frühere Aussage zur mobilen Plugin-Unterstützung ist kein Nachweis, dass Franks konkrete Verbindung funktioniert.

Frank möchte JARVIS möglichst direkt auf dem Handy betreiben und einen zusätzlichen eigenen Server vermeiden. Seine WireGuard-Verbindung empfand er teilweise als langsam. Eine bessere Geschwindigkeit eines MCP-Tunnels wurde nicht belegt. Für kurze Aufgabenaufträge sind die Datenmengen klein; große Datenauswertungen sollten mit bedarfsgerechten Ausschnitten, lokalem Abruf und gegebenenfalls lokaler Verdichtung arbeiten.

## Werkzeuge und Regeln

Frank bevorzugt natürliche Sprache statt auswendig gelernter Werkzeugnamen. Es gibt keinen verbindlichen weltweiten Werkzeugkatalog. Übliche Bezeichnungen sind search, read, get, list, create, update und delete; eine verständliche Benennung verbindet Handlung und Gegenstand. Das offizielle MCP Registry verzeichnet öffentliche MCP-Server, nicht sämtliche Werkzeuge oder private Dienste.

Bei der Entwicklung reicht die Beschreibung gewünschter Fähigkeiten als Auftrag an den Programmierassistenten. Technisch müssen diese Fähigkeiten durch ausführbaren Code oder angebundene Dienste umgesetzt werden. Die KI erhält Werkzeugname, Beschreibung und Eingabefelder und kann natürliche Nutzeraufträge passenden Werkzeugen zuordnen. Ein Plugin kann mehrere Werkzeuge enthalten; ein allgemeines Werkzeug zur Übergabe freier Aufträge an JARVIS ist ebenfalls möglich.

ChatGPT erhält nicht automatisch den gesamten Plugin-Code oder alle internen JARVIS-Regeln. Werkzeugbeschreibungen und tatsächlich bereitgestellte Anweisungen steuern die Auswahl; detaillierte Verarbeitung kann in JARVIS liegen. Eine lokale cloud.md-Datei wird nicht allein wegen ihres Namens automatisch vom Android-Sprachmodus gelesen. Zugriffsbeschränkungen müssen im Programm durchgesetzt werden. Das Wort JARVIS kann die gewünschte Zuordnung im Gespräch verdeutlichen, ist aber dadurch noch kein ständig aktives technisches Wake Word.

## Agentenschleife auf Android

Ein Agent arbeitet mit Ziel, Modellentscheidung, Werkzeugaufruf, Ergebnis und anschließender Prüfung weiter. Der Verlauf wird normalerweise fortgeführt statt bei jedem Schritt vollständig neu gestartet. Zielerreichung braucht konkrete Prüfkriterien und darf nicht allein auf der Selbstauskunft des Modells beruhen. Grenzen für Schritte, Laufzeit oder Kosten verhindern endlose Schleifen.

Die Steuerung kann in der JARVIS-App auf dem Handy laufen, während das Sprachmodell über eine externe Programmierschnittstelle rechnet. Ein eigener Server ist dafür nicht zwingend nötig. Ein lokales Modell wäre eine zusätzliche Option mit eigenen Leistungs- und Energiegrenzen.

Ein gesperrter Bildschirm schließt Hintergrundarbeit nicht aus. Das Beibehalten der App im Arbeitsspeicher und das Ausschalten der Akku-Optimierung garantieren jedoch keine ununterbrochene Ausführung. Je nach Aufgabe kommen Android-Dienste mit sichtbarer Statusbenachrichtigung oder geplante Hintergrundarbeit infrage. Dienstkategorien, Startregeln, Ruhemodus und versionsabhängige Laufzeitgrenzen sind zu beachten. Begrenzte gestartete Aufträge sind einfacher umzusetzen als garantierter Dauerbetrieb rund um die Uhr.

JARVIS soll Ziel, Zwischenergebnisse, nächsten Schritt und bereits ausgeführte Änderungen dauerhaft speichern. Nach Prozessende, Netzunterbrechung oder Neustart soll eine kontrollierte Wiederaufnahme möglich sein, ohne Aufgaben oder andere Änderungen doppelt auszuführen.

## Erster Funktionstest

Zuerst wird geprüft, ob JARVIS auf dem Handy erreichbar ist. Danach werden Aufgaben lesen und eine Aufgabe anlegen angebunden. Anschließend werden Textaufruf und Sprachaufruf aus ChatGPT getrennt getestet. Für die lokale Agentenschleife folgen Tests bei gesperrtem Bildschirm, Netzunterbrechung und Wiederaufnahme. Weitere Apps und komplexe Auswertungen werden erst nach erfolgreichem Grundtest ergänzt.

Diese Datei hält Projektwünsche und technische Grenzen fest. Sie muss bei Bedarf gelesen werden und ist keine automatisch in jedem Gespräch geladene persönliche ChatGPT-Erinnerung.

## Externes Projektwissen im Repository

Frank möchte wichtige Informationen längerer Gespräche dauerhaft im eigenen Repository speichern und später gezielt wieder einlesen lassen. Dieses Repository dient damit als externes, versioniertes Wissensarchiv. Der Inhalt wird beim Abruf in den aktuellen Arbeitskontext geladen; das Modell wird durch die Dateiablage nicht neu trainiert. Der Zugriff auf Pepsi1978/proggs wurde in dieser Sitzung praktisch bestätigt: Die ursprüngliche JARVIS-Notiz wurde erstellt, über einen Pull Request nach main übernommen und anschließend wieder ausgelesen.

Eine zuvor angelegte private ChatGPT-Projektnotiz existiert zusätzlich. Sie ersetzt weder die Repository-Datei noch eine persönliche ChatGPT-Erinnerung. Der Repository-Stand soll die ausdrücklich gewünschte Ablage für das Projektwissen sein. Ein späterer Auftrag kann das Lesen der gespeicherten Projektinformationen anfordern; zuverlässiger Abruf setzt voraus, dass die verwendete Anwendung die nötigen Werkzeuge und Zugriffsrechte besitzt.

## Verbindlich definierter Datenbank Skill

Frank wollte zuerst nur die Machbarkeit eines solchen Skills klären und anschließend dessen Regeln selbst definieren. Die erste Installation erfolgte vor dieser Definition. Anschließend wurden die Anweisungen an seine konkreten Vorgaben angepasst. Es gibt ausschließlich einen Skill mit dem Namen Datenbank. Die zusätzliche Bezeichnung Gedächtnis-Skill wurde verworfen und aus den vorgesehenen Aufrufen entfernt.

Der Skill soll bei einem ausdrücklichen Speicherauftrag möglichst viele wichtige Informationen aus dem gesamten verfügbaren Gespräch ausführlich aufbereiten. Eine knappe oder grobe Zusammenfassung genügt Frank nicht. Der Text soll übersichtlich gegliedert sein, Zusammenhänge und Gründe erhalten und Wiederholungen oder sprachliche Füllwörter entfernen. Zu bewahren sind insbesondere konkrete Wünsche, Entscheidungen, Begründungen, Alternativen, technische Anforderungen, Zahlen, Beispiele, Einschränkungen, offene Fragen und nächste Schritte. Vorschläge der Assistenz dürfen nicht als Entscheidungen des Nutzers dargestellt werden. Fehlende Gesprächsteile dürfen nicht erfunden werden.

Der feste Hauptordner ist Datenbank. Darunter liegen Themen- oder Projektordner wie JARVIS, Philosophie, Zitate oder konkrete Apps. Existiert ein passender Ordner bereits, soll er wiederverwendet werden. Für ein neues Thema darf nach der Freigabe ein neuer Unterordner entstehen. Mehrere Gespräche über JARVIS werden im selben Projektordner gesammelt. Die heutige Migration wurde ausdrücklich nach Datenbank/JARVIS beauftragt.

## Dateinamen und zweistufige Inhaltsverzeichnisse

Dateinamen müssen den konkreten Inhalt erkennbar machen. Frank kritisierte die frühere Bezeichnung Gespraech_2026-10-07.md, weil sie das Thema nicht erkennen lässt. Vereinbart ist eine Benennung mit Thema und Unterthemen sowie dem Datum als Zusatz. Die heutige Datei beschreibt deshalb Sprachsteuerung, MCP, Agentenschleifen und Projektwissen im Namen.

Im Hauptordner Datenbank liegt INHALTSVERZEICHNIS.md als Projektübersicht. Zu jedem Unterordner enthält es dessen Namen, eine kurze Beschreibung des Projekts oder Themengebiets und einen Link zum jeweiligen Inhaltsverzeichnis. Der JARVIS-Eintrag erklärt die Idee eines persönlichen KI-Assistenten mit Sprachsteuerung, App-Anbindungen und Agentenabläufen. Die Hauptübersicht soll die Einordnung ermöglichen, ohne alle Unterordner und Dateien vollständig zu durchsuchen.

Jeder Themenordner besitzt ebenfalls INHALTSVERZEICHNIS.md. Dort werden die einzelnen Wissensdateien mit aussagekräftigem Titel, Datum, Link und kurzer konkreter Inhaltsbeschreibung aufgeführt. Beim späteren Abruf wird zunächst die Übersicht verwendet, dann das Inhaltsverzeichnis des passenden Projekts und schließlich nur die für die Frage relevanten Dateien. Die Inhaltsverzeichnisse ersetzen den ausführlichen Inhalt nicht, sondern helfen bei dessen gezielter Auswahl.

## Gewünschter Speicherablauf und Bestätigung

Der Aufruf kann lauten: „Starte den Datenbank-Skill, speichere die Informationen ab.“ Danach werden Gespräch, Projektzuordnung, ausführliche Zusammenfassung, Dateiname und Inhaltsverzeichniseinträge vorbereitet. Vor dem Schreiben soll die Assistenz den konkreten Ordner vorschlagen und fragen, ob dieser passend ist. Franks Beispiel: „Ich würde die Informationen jetzt in Datenbank im Ordner Philosophie abspeichern. Ist das okay?“

Ein Ja gibt den vorgeschlagenen Speicherort frei. Eine Antwort wie „Nein, speichere das lieber in Zitate“ bestimmt stattdessen den korrigierten Ordner. Ohne eindeutige Freigabe wird nicht gespeichert. Anschließend werden Dateien und Inhaltsverzeichnisse konsistent geschrieben und aus dem tatsächlichen Repository-Ziel erneut geprüft. Eine bloße Frage nach der Machbarkeit ist ausdrücklich kein Auftrag, den Skill anzulegen oder ein Gespräch zu speichern.

Der Skill beschreibt den Arbeitsablauf und verwendet vorhandene Werkzeuge zum Lesen und Schreiben. Er erzeugt selbst weder einen neuen Repository-Zugang noch einen vollständigen automatischen Live-Mitschnitt. Die Verfügbarkeit aller Anweisungen und Werkzeuge in einer zukünftigen Sprachsitzung ist von deren Umgebung abhängig; in der aktuellen Sitzung wurde Repository-Zugriff nachgewiesen.

## Offene Umsetzungspunkte

Die Datenbank-Struktur und der Skill dienen zunächst dem Speichern und Abrufen von Wissen. Sie sind noch keine fertig gebaute JARVIS-App und kein getesteter MCP-Tunnel zum Handy. Für JARVIS bleiben die Erreichbarkeit auf Android, die Schnittstelle zur Aufgaben-App, Lese- und Schreibzugriffe im gewünschten Sprachmodus sowie Hintergrundbetrieb und Wiederaufnahme praktisch zu testen. Die gewünschte vollständige Übergabe aller Live-Gesprächsinformationen bleibt eine gesonderte technische Anforderung.
