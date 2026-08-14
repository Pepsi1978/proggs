# KI-Designstudio (Spec → Design) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Trifft ein Punkt auf deine Aufgabe zu, lies den gleichnamigen
> Abschnitt im VOLLTEXT (`ki-designstudio.md`). Gegenseite: `bugs/design/design-export-und-messung.md`.

Stand: 14.08.2026 · Bereich: KI-gestützte Design-Erzeugung, Multi-Screen-Konsistenz, Vorschau-Rendering

## ⚡ Kurzcheck (vor der Arbeit lesen)

| # | Situation | Sofort-Regel | Volltext |
|---|-----------|--------------|----------|
| 1 | Mehrere Bildschirme aus einer Beschreibung erzeugen | Erst Design-Kontrakt (Tokens **und** Leisten-Maße), dann Screens — nie umgekehrt aus fertigen Screens ableiten | §1 |
| 2 | Wiederkehrende Navigation je Screen neu erzeugt | Chrome EINMAL bauen, per Vorlage in jeden Screen einsetzen; nie pro Screen frei texten lassen | §2 |
| 3 | Ein Mega-Aufruf für alle Screens erwogen | Nein — Einzelaufruf je Screen mit **gemeinsamem festem Kontext** ist die Praxis aller großen Werkzeuge | §3 |
| 4 | Konsistenz nur per Prompt-Anweisung gesichert | Reicht nicht: Kein Aufruf sieht die anderen. Nach dem Zusammensetzen **programmatisch** vergleichen | §4 |
| 5 | Golden-Screen als Beispiel weggelassen | Entfernen senkt die Konsistenz-Metriken um 38–42 % | §1 |
| 6 | Fertige Bildschirmdateien liegen im Paket bei | Als Quelle lesen statt aus der Prosa neu erfinden — abschreiben schlägt raten | §5 |
| 7 | Effekt im Faktenblatt ohne Ortsangabe | Name muss den Ort tragen (`.karte (shadow)`), sonst wird der Effekt lautlos weggelassen | §5 |
| 8 | `backdrop-filter` wirkt nicht | Kein `filter`/`opacity<1`/`mask`/`mix-blend-mode`/`will-change` auf einem Vorfahren; `-webkit-`-Präfix | §6 |
| 9 | Generiertes HTML in Vorschau sanitisiert | CSSOM-basiertes Property-Whitelisting statt Regex; `srcdoc`+`csp`-Attribut kann die Eltern-CSP **nicht** lockern → eigene Preview-Origin | §6 |
| 10 | Skalierte Bühne zeigt helle Randlinien | Fläche darunter in Designfarbe + gleichfarbiger 1px-Saum (Chromium 600120) | §6 |
| 11 | Gleicher Kontext geht bei jedem Aufruf neu raus | Stabilen Teil ans **Prompt-Ende? Nein — an den ANFANG**, wörtlich identisch; Cache-Read ist ~90 % günstiger | §7 |
| 12 | Ganze Datei neu erzeugen lassen | Fast-Apply/Diff statt Vollausgabe: 10–13× schneller, >90 % Latenzersparnis | §7 |
| 13 | Cache-Breakpoint auf wechselndem Block (Zeitstempel) | Dauerhafter Cache-Miss trotz gleichem Präfix — Breakpoint auf den letzten **stabilen** Block | §7 |
| 14 | Mehrere KI-Aufträge auf EIN Dokument | Parallel generieren, **seriell anwenden**; Sperre + frisches Lesen innerhalb der Transaktion | §8 |
| 15 | Konflikte automatisch mergen lassen | Nie — syntaktisch korrekt, semantisch falsch. Konflikt anzeigen oder neu erzeugen | §8 |
| 16 | Bereich markieren + Änderung beschreiben | Screenshot **und** struktureller Code-Pfad kombinieren — reiner CSS-Selektor ist zu brüchig | §9 |
