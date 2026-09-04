# Codex-Kompass: Fallen bei Dokumentationsimport und App-Kopie

Stand: 04.09.2026. Quellenanker: Codex CLI 0.153.3.

1. Nur der erste Name einer Tabellenzelle wird gelesen: Aliasnamen fehlen. Alle Rückstrich-Namen auswerten; 59 dokumentierte Einträge ergeben sich aus Tabelle, Alias /clean und fünf Release-Ergänzungen.
2. Release-Ergänzungen fehlen in der Tabelle: Ein automatischer Abgleich würde sie fälschlich als entfernt markieren. Ergänzungsbestand getrennt behandeln; Abwesenheit in der Übersicht ist kein Entfernungsbeleg.
3. Alte Claude-Quellen in einer umbenannten App: Der Aktualisieren-Knopf importiert wieder den falschen Katalog. Quellen, Modell-Anweisungen, Assets und Produkttexte gemeinsam umstellen.
4. Die Release-Seite /docs/changelog.md lieferte beim Abruf 404, während /docs/changelog als HTML verfügbar war. Nicht annehmen, dass jede Seite eine nutzbare Markdown-Variante hat; CLI-Version gezielt aus HTML lesen und bei fehlender Version abbrechen.
5. Sicherungen der Schwester-App würden bei gleichem Schema akzeptiert. Vor dem Einlesen die App-Kennung prüfen, damit Claude-Inhalte nicht den Codex-Katalog überschreiben.

Quellen: https://learn.chatgpt.com/docs/developer-commands?surface=cli ; https://learn.chatgpt.com/docs/changelog
