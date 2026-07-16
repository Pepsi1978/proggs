# Projekt 1: Bezahlter Android-Spezialrechner

**Version:** v1.0 - 16.07.2026 11:32 Uhr  
**Status:** Projektgrundlage und sämtliche noch verfügbaren Recherchedaten im Repo gebündelt  
**Entscheidung:** Keine App zur Umsetzung freigegeben

## Zweck dieses Ordners

Dieser Ordner ist die vollständige Arbeitsgrundlage für eine spätere Fortsetzung der Projektsuche. Er enthält die verbindliche Projektdefinition, den konsolidierten Abschlussbericht, die Rechercheaufträge beider Stufen und sämtliche noch vorhandenen Rohartefakte der zweiten Recherche.

## Hauptdokumente

- `bezahlte-android-spezialrechner-projektgrundlage.md`: verbindliche Produkt-, Markt-, Rechts- und Technikleitplanken.
- `bezahlte-android-spezialrechner-recherche-2026-07-16.md`: konsolidierte Auswertung beider Recherchewellen mit Kandidaten, Matrix, Quellen und offenen Punkten.

## Recherchebestand

### Stufe A: Firecrawl und MiniMax

- Engine: Firecrawl und MiniMax.
- Umfang: acht Researcher, höchstens zwei gleichzeitig.
- Exakte Aufträge: `recherche/stufe-a/rechercheauftraege.md`.
- Ergebnis: Die Rohdateien wurden vom Research-Swarm vor Stufe B automatisch überschrieben. Sie sind nicht mehr wiederherstellbar.
- Erhaltener Inhalt: Alle vor dem Überschreiben extrahierten Befunde, URLs, Kennzahlen, Kandidaten und Einschränkungen stehen im konsolidierten Abschlussbericht.

### Stufe B: OpenRouter MiniMax M3 `:online`

- Engine: OpenRouter mit `minimax/minimax-m3:online`.
- Umfang: zehn Researcher, höchstens sieben gleichzeitig.
- Exakte Aufträge: `recherche/stufe-b/rechercheauftraege.md`.
- Lesbare Modellantworten: `recherche/stufe-b/antworten/answer-1.txt` bis `answer-10.txt`.
- Ausführungslogs mit Provider, Tokenverbrauch, Einzelkosten, Retry und Rohdateipfad: `recherche/stufe-b/logs/log-1.txt` bis `log-10.txt`.
- Vollständige OpenRouter-Antwortobjekte einschließlich Metadaten und Quellenannotation: `recherche/stufe-b/json/answer-1.json` bis `answer-10.json`.

## Integrität und Grenzen

- Die zehn Antworten, Logs und JSON-Dateien der Stufe B wurden vor der Übernahme anhand ihrer eingebetteten Projektprompts geprüft.
- Die Datei `C:\Users\barwa\.research-swarm\themen.txt` wurde bewusst nicht übernommen. Sie war am 16.07.2026 bereits durch eine fremde, spätere Recherche zu OpenAI-Modellpreisen überschrieben und gehört nicht zu Projekt 1.
- `done.flag` wurde nicht übernommen, da es nur ein flüchtiger Runner-Sentinel ohne Rechercheinhalt ist.
- Die Stufe-B-Logs enthalten teilweise fehlerhaft dargestellte Umlaute aus der damaligen Konsolenausgabe. Die unverfälschten deutschen Aufträge stehen deshalb zusätzlich in `rechercheauftraege.md`.
- Fehlende Suchtreffer sind kein Beleg für eine Marktlücke. Der Abschlussbericht behandelt diese Grenze ausdrücklich.

## Nächster sinnvoller Schritt

Vor einer Implementierung muss zuerst die Hypothese `Modellmaß` mit direkten Play-Store-Listings, öffentlichen Rezensionen, Web-Rechner-Vergleichen, Community-Fragen und frei zugänglichen Formelquellen validiert werden. Bis dahin bleibt die Zahl ausreichend marktvalidierter Kandidaten bei null.
