# Second Brain — Level-2-Plan: 50 Feature-Vorschläge (Stand 2026-07-04)

> **UMSETZUNGS-STATUS (Stand 2026-07-05, 14.55 Uhr):**
>
> | Gruppe | Punkte | Status |
> |--------|--------|--------|
> | A — Gedächtnis-Architektur | 1-10 | ✅ FERTIG (2026-07-10, #47796–#47802): 1 Kurzzeit/Langzeit-Ebene + Bibliothekar-Beförderung, 2+3 Kern-Blöcke (agent/coreblocks.py, Werkzeug aktualisiere_kernblock, Dashboard-Karte), 4 bi-temporal (nur manuell, Drawer), 6 Episoden-Destillation (Nacht-Task, nur Vorschläge), 7+8 Recall-Verstärkung (/entries/touch + sanfter Boost, KEIN Alters-Malus), 9 Meta-Gedächtnis (Daumen hoch/runter + Nacht-Muster→Regel-Vorschläge), 10 Provenance (source=chat, trust, Drawer-Zeile). **Punkt 5 (ADD/UPDATE/DELETE beim Speichern) auf Franks ausdrücklichen Wunsch GESTRICHEN — Speichern bleibt 1:1 ohne Bewertung/Ersetzung.** |
> | B — Nachtschicht-Bibliothekar | 11-18 | ✅ FERTIG (librarian 0.1.0-0.8.0, 2026-07-05) |
> | C — Proaktivität / Unterbewusstsein | 19-26 | 🔴 offen |
> | D — Mitlernen in Programmier-Sessions | 27-33 | ✅ FERTIG (agent 0.53.0 + SessionEnd-Hook + librarian 0.8.0, 2026-07-05) — Detail: D27 /session-log, D28 Kern-Block, D29 Entscheidungs-Regel, D30 OpenCode-AGENTS.md, D31 Projektstand-Recall, D32 Episoden-Auszug+Schema-Canary, D33 Reibungs-Detektor. Hinweis zu D32: bewusst als verdichtete Episode IM Session-Eintrag umgesetzt (Prompts-Auszug), nicht als separates Voll-Transkript-Archiv |
> | E — Such-Intelligenz | 34-39 | ✅ FERTIG (brain-api 1.21.0 + agent 0.50.0-0.51.2, 2026-07-04) |
> | F — Frontends | 40-46 | 🔴 offen |
> | G — Sprache & Capture | 47-50 | 🔴 offen |

> Ergebnis der Recherche vom 2026-07-04 (Engine A: Firecrawl+MiniMax, 7 Themen + 2 Retries,
> ~44 Quellen). Recherche-Wissen persistiert in `best-practices/second-brain/memory-evolution-2026.md`.
> Franks Vision: Das Second Brain soll wie ein **Unterbewusstsein** funktionieren — immer da,
> immer relevant, mit Gesamtüberblick; Langzeitgedächtnis (Qdrant) + Kurzzeitgedächtnis;
> lernt in jeder (Programmier-)Session automatisch dazu.
>
> **Schwester-Plan:** Eine parallele Cowork-Session hat am selben Tag unabhängig recherchiert und
> `best-practices/second-brain/second-brain-2.0-unterbewusstsein-50-features.md` erstellt (#47463).
> Beide Pläne decken sich im Kern (Kern-Blöcke, Schlaf-Agent, Hybrid-Suche/RRF, Session-Hooks,
> Notify/Question/Review, bi-temporale Fakten, Soft-Decay) — das validiert die Richtung doppelt.
> Nur dort: ntfy-Push-Kanal, ACE-Playbook pro Projekt, Spaced-Repetition-Resurfacing, ColBERT-
> Rescoring, Heat-Score, Blocks-als-Markdown-in-Git, Agent-Inbox. Nur hier: Android-System-Capture
> (Widget/Share-Sheet/Tile), Gehirn-Gesundheits-KPIs, Wissens-Graph-Ansicht, Friction-Detektor,
> Multi-Query-Recall, Wochenrückblick, Morning-Brain-Dump-Ritual, Streaming-TTS. Bei der Umsetzung
> BEIDE Listen als Ideen-Pool nutzen.

## Ist-Stand (worauf aufgebaut wird)

| Baustein | Stand |
|----------|-------|
| brain-api | v1.20 — 1:1-Speicher auf Qdrant, 2-Ebenen-Kategorien, Multi-Kategorie, Papierkorb, by-title/category/date/parent, gefilterte Vektorsuche, reembed-all |
| sb-agent | v0.31 — Intents save/recall/internet(Tavily)/smalltalk, Leseagent-Filter, Dubletten-Check, Logbuch mit Zeitstempeln |
| Dashboard (Cortex) | v0.36 — Chat, Kategorien-Baum, Drawer mit Edit, Vorlesen, Multi-Kategorie |
| CortexAndroid | App mit Chat, Audio, WireGuard-VPN-Anbindung |
| MCP | remember/recall/get_by_*/get_category_item/list/forget/health in Claude Code + OpenCode |
| Backup | Täglicher Qdrant-Snapshot (4 Uhr) + Cortex-Backup-Tool (Disaster Recovery) |

## Die 3 Leitideen aus der Recherche

1. **Kern-Blöcke (Kurzzeitgedächtnis):** kleine, immer präsente Wissens-Blöcke (Wer ist Frank,
   aktuelle Ziele, laufende Projekte) — der „Gesamtüberblick", der bei jeder Antwort im Kontext liegt.
2. **Nachtschicht-Bibliothekar (Konsolidierung):** ein Sleep-Time-Agent, der asynchron (nachts,
   stärkeres Modell) aufräumt: verdichten, Widersprüche finden, Dubletten mergen, Kern-Blöcke pflegen.
3. **Proaktivität mit Drossel:** Kontext-Injektion vor jeder Antwort („der Agent weiß mehr als er
   zeigt"), Briefings/Trigger nach dem Muster Notify/Question/Review, hart gedrosselt gegen Nerv-Faktor.

---

## A. Gedächtnis-Architektur: Kurzzeit + Langzeit (1-10)

1. **Kurzzeitgedächtnis-Schicht:** Neues landet zuerst in einer „Arbeitsgedächtnis"-Ebene
   (Payload-Flag `layer=kurzzeit`); der Nacht-Bibliothekar befördert Bewährtes ins Langzeitgedächtnis.
2. **Kern-Blöcke nach Letta-Vorbild:** 4-6 kleine Blöcke (Profil, Ziele, laufende Projekte, offene
   Aufgaben, Vorlieben), die der Agent bei JEDER Antwort im Kontext hat — der immer präsente Überblick.
3. **Selbst-editierende Kern-Blöcke:** Der Agent darf Kern-Blöcke per Tool aktualisieren
   (mit Änderungs-Log + Zeichen-Limit pro Block), statt dass sie manuell gepflegt werden.
4. **Bi-temporale Fakten:** Felder gültig-ab/gültig-bis pro Eintrag (Zep-Muster) — „Wo wohne ich?"
   liefert den aktuellen Stand, „Wo wohnte ich 2024?" den historischen; nichts geht verloren.
5. **ADD/UPDATE/DELETE/NOOP-Logik beim Speichern (mem0-Muster):** neue Fakten werden gegen den
   Bestand geprüft — aktualisieren statt duplizieren, mit Rückfrage bei echtem Widerspruch.
6. **Episoden→Fakten-Destillation:** Aus dem Gesprächs-Logbuch (episodisch) destilliert der
   Bibliothekar dauerhafte Fakten (semantisch) — die Brücke zwischen den zwei Spuren, die es schon gibt.
7. **Recall-Verstärkung:** Jeder Abruf erhöht den Strength-Score eines Eintrags und setzt seine
   „Vergessens-Uhr" zurück (MemoryBank-Muster) — oft Gebrauchtes bleibt vorn.
8. **Sanftes Vergessen mit Boden:** Decay-Score aus Alter + Abrufhäufigkeit + Relevanz; nie löschen,
   nur nach hinten sortieren — jederzeit reaktivierbar (belegtes Zielske/MemoryBank-Modell).
9. **Meta-Gedächtnis des Agenten:** Der Agent merkt sich, WIE Frank fragt und welche Antworten gut
   ankamen (Daumen hoch/runter in App + Dashboard) — getrennt vom Inhalts-Gehirn.
10. **Provenance-Pflicht:** Jeder Eintrag trägt Quelle (Chat/Session/App/Import) + Vertrauens-Level;
    der Agent nennt die Herkunft auf Wunsch (Befund: Vertrauen entsteht erst durch Provenance).

## B. Der Nachtschicht-Bibliothekar (11-18)

11. **Sleep-Time-Agent als eigener Dienst:** läuft nachts nach dem 4-Uhr-Backup, konsolidiert das
    Kurzzeitgedächtnis, pflegt Kern-Blöcke — darf ein stärkeres Modell nutzen als der Tages-Agent.
12. **Widerspruchs-Suche:** semantisch ähnliche Einträge mit widersprüchlichem Inhalt finden
    (Nachbarschaft + LLM-Prüfung) → morgendliche Klär-Liste für Frank.
13. **Dubletten-Merge-Vorschläge:** ähnliche Einträge clustern, Merge vorschlagen — Frank bestätigt
    im Dashboard mit einem Klick (nie automatisch mergen).
14. **Auto-Verdichtung des Logbuchs:** Gespräche älter als X Wochen zu Monats-Zusammenfassungen
    verdichten — Original bleibt 1:1 erhalten, die Summary ist eine ZUSÄTZLICHE Ebene.
15. **Kategorien-Gärtner:** schlägt neue Unterkategorien vor, wenn eine Kategorie zu groß/heterogen
    wird; erkennt verwaiste und schlecht befüllte Kategorien.
16. **Stale-Entry-Erkennung:** Einträge mit Verfallscharakter (Preise, Versionen, Termine) markieren,
    wenn sie wahrscheinlich veraltet sind → „Bitte prüfen"-Liste.
17. **Wissens-Lücken-Detektor:** erkennt, wonach Frank oft fragt, ohne dass etwas gespeichert ist →
    Vorschlag „soll ich das anlegen/recherchieren?".
18. **Morgen-Report des Bibliothekars:** Karte in Dashboard + App: „Heute Nacht: 3 Dubletten
    zusammengeführt, 1 Widerspruch gefunden, 2 Einträge womöglich veraltet".

## C. Proaktivität / Unterbewusstsein (19-26)

19. **Tagesbriefing:** jeden Morgen automatisch (Push in die App + Dashboard-Karte): offene Aufgaben,
    Termine, relevante Erinnerungen, Bibliothekar-Report (Saner.AI-Vorbild).
20. **Wochenrückblick:** sonntags automatisch — was kam neu ins Gehirn, welche Projekte bewegten
    sich, was blieb liegen.
21. **Per-Turn-Kontext-Injektion:** vor jeder Antwort holt der Agent still passende Einträge
    (Signal-Erkennung auf der Nachricht) und legt sie dem LLM unsichtbar bei — Display-Layer ≠
    System-Layer: „der Agent weiß mehr als er zeigt".
22. **Notify/Question/Review-Muster:** proaktive Aktionen nur in drei Formen — informieren,
    nachfragen, Freigabe einholen (LangChain-Ambient-Muster) — nie eigenmächtig handeln.
23. **Proaktivitäts-Drossel (Temporal Constraints):** max. N proaktive Meldungen/Tag + Ruhezeiten —
    belegt +38,9 % Zufriedenheit durch genau diese Drossel.
24. **Erinnerungs-Trigger:** „Erinnere mich am Montag / beim nächsten Einkauf an X" — Reminder-Engine
    im Agent + Push in die Android-App (Ort optional per Geofence).
25. **Kontext-Feed „Könnte dich jetzt interessieren":** Karte, die abhängig von Tageszeit und
    letzten Themen passende Einträge hochspült — Serendipity statt nur Suche.
26. **Muster-Erkennung über Zeit:** Wiederkehrendes sichtbar machen („jeden Winter fragst du nach X";
    Trainings-/Gesundheits-Muster aus den eigenen Einträgen).

## D. Mitlernen in Programmier-Sessions (27-33)

27. **Session-End-Hook → Gehirn:** Claude-Code-Hook schreibt pro Session ein kompaktes
    „gemacht/entschieden/gelernt" ins Kurzzeitgedächtnis (Kategorie Programmieren/Sessions).
28. **Ziele-Extraktion in Kern-Block:** aus Task-Ledger + Transkript wird der Kern-Block
    „Woran Frank gerade baut" automatisch aktuell gehalten.
29. **Entscheidungs-Rückfluss:** zusätzlich zu Bugfixes (läuft schon) auch Architektur-Entscheidungen
    automatisch als Merk-Kandidaten vorschlagen („soll ich diese Entscheidung merken?").
30. **Cross-CLI-Gedächtnis:** OpenCode/Codex speisen dieselben Session-Zusammenfassungen im gleichen
    Format ein — das Gehirn hat CLI-übergreifend den Überblick.
31. **„Woran habe ich zuletzt gearbeitet?"-Recall:** Agent beantwortet Projektstand-Fragen aus den
    Session-Einträgen + git-Historie.
32. **Transkript-Archiv:** Claude-Code-Transkripte (verdichtet) als durchsuchbare Episoden ins Gehirn
    spiegeln — mit Format-Wächter gegen Log-Format-Änderungen (claude-engram-Muster „Schema-Canary").
33. **Friction-Detektor:** wiederkehrende Fehlversuche/Korrekturen aus Sessions erkennen und als
    Verbesserungs-Kandidaten ablegen (Befund: „Korrekturen fließen nie zurück" — genau das reparieren).

## E. Such-Intelligenz (34-39)

34. **Hybrid-Suche (BM25 + dense, RRF-Fusion):** DER eine belegte Retrieval-Gewinn — nativ in Qdrant,
    trifft exakte Begriffe UND vage Erinnerungen; bei hunderten Einträgen schnell eingebaut.
35. **Zeit-bewusste Suche:** „letzten Monat", „im Winter", „vor dem Umzug" als Datums-Filter im
    Query-Routing (created_at liegt schon im Payload).
36. **Leichtes Entity-Linking statt Voll-Graph:** kleine zweite Collection mit Entitäten (Personen,
    Orte, Projekte, Geräte) als Hub-and-Spoke (mem0-v3-Muster) — GraphRAG ist belegt Overkill.
37. **Multi-Query-Recall:** der Agent formuliert intern 2-3 Suchvarianten und fusioniert die Treffer —
    bessere Quote bei vagen Fragen.
38. **Confidence in Antworten:** „sicher, aus 3 Einträgen" vs. „nur ein schwacher Treffer" —
    Vertrauens-Transparenz in jeder recall-Antwort.
39. **Quellen-Drilldown überall:** jede Agent-Antwort verlinkt die benutzten Einträge (Dashboard-Drawer
    bzw. App-Sheet öffnet den Volltext).

## F. Frontends: Dashboard + Android (40-46)

40. **Gehirn-Gesundheits-Dashboard:** KPIs (Einträge, Wachstum, Dubletten-Kandidaten, veraltete
    Einträge, verwaiste Kategorien, Backup-Status) — gibt es am Markt kaum: Alleinstellungs-Feature.
41. **Wissens-Graph-Ansicht:** interaktive Cluster-Visualisierung (Community-Detection) — Themen
    tauchen von selbst auf, statt starrer Taxonomie.
42. **Android-Widget „Schnell ins Gehirn":** Homescreen-Widget mit Mikro + Textfeld — ein Tipp,
    sprechen, gespeichert.
43. **Android Share-Sheet-Ziel:** aus jeder App Text/Links „An Cortex senden" — mit
    Kategorie-Vorschlag durch den Agent.
44. **Quick-Settings-Tile + Notification-Action:** Capture aus der Statusleiste; Tagesbriefing als
    ausklappbare Benachrichtigung.
45. **Timeline-Ansicht:** „Was kam wann ins Gehirn" chronologisch (Tag/Woche/Monat) — der
    by-date-Endpunkt liefert die Daten schon.
46. **„An diesem Tag"-Karte:** Einträge von vor 1 Monat / 1 Jahr wieder hochspülen — macht das
    Gedächtnis lebendig.

## G. Sprache & Capture (47-50)

47. **Streaming + Vorlesen ab erstem Absatz:** SSE-Streaming im Agent, TTS startet beim ersten
    vollständigen Absatz (steht schon auf der Wunschliste — hier fest eingeplant).
48. **Freihand-Sprachgespräch in der App:** Push-to-talk-Loop (Whisper-STT → Agent → TTS) für
    unterwegs und Auto.
49. **Morning Brain Dump als Ritual:** geführter Capture-Modus („2 Minuten reden"), der Agent zerlegt
    in Aufgaben/Fakten/Gedanken und sortiert ein (Chela-Muster).
50. **Foto-/Dokument-Capture:** Foto in der App → OCR/Beschreibung durch Gemini → durchsuchbarer
    Eintrag (Typenschilder, Zettel, Schrankinhalt — passt zum Inventar-Ziel).

---

## Empfohlene Reihenfolge (Phasen)

| Phase | Inhalt | Vorschläge |
|-------|--------|------------|
| **1 — Fundament „Überblick"** | Kern-Blöcke + Selbst-Edit + Per-Turn-Injektion | 2, 3, 21 |
| **2 — Nachtschicht-Bibliothekar v1** | Dienst + Dubletten + Stale + Morgen-Report | 11, 13, 16, 18 |
| **3 — Session-Mitlernen** | Session-End-Hook + Ziele-Kern-Block + Cross-CLI | 27, 28, 30 |
| **4 — Proaktivität** | Tagesbriefing + Drossel + Reminder | 19, 23, 24 |
| **5 — Such-Intelligenz** | Hybrid-Suche + Zeit-Filter + Confidence | 34, 35, 38 |
| **6 — Frontends** | Widget + Share-Sheet + Gesundheits-Dashboard | 42, 43, 40 |
| danach | alles Weitere nach Lust und Nutzen | Rest |

Faustregel aus der Recherche: erst der **Überblick** (Kern-Blöcke), dann die **Pflege**
(Bibliothekar), dann das **Mitlernen** (Sessions) — Proaktivität wird erst richtig gut, wenn die
ersten drei stehen, weil Briefings und Injektion aus gepflegtem Wissen schöpfen.
