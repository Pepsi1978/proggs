# Bibliothekar-Agent (Schicht 3) — Bauplan

> Der "gute Bibliothekar" des zweiten Gehirns: ein **Gespraechs-Agent**, der Wissen **einordnet**
> (speichert) und **rausgibt** (abruft). Er liest nur und schreibt **nie eigenmaechtig** am
> 1:1-Speicher — er benutzt dessen Werkzeuge. **Text rein, Text raus.**
>
> **Stand: 2026-06-23** (Design mit Frank festgelegt). Phase 4 des Flugplans
> [[project_second_brain_flugplan]]. Speicher-Schema: [[speicher-schema-1zu1]].
>
> **Reihenfolge (Frank): ZUERST die Speicher-Seite (Phase 4a) — komplett, NICHT mit der Abfrage-Seite
> vermischt.** Die Abfrage-Seite (4b), STT/TTS (Frontends) und das Dashboard kommen spaeter.

---

## 1. Architektur-Einordnung

| Schicht | Wo | Aufgabe |
|---------|-----|---------|
| **Frontends** | auf jedem Geraet (Handy, PC, Mac, …) | Sprache↔Text (STT/TTS), Chat-Oberflaeche. Schicken/empfangen **NUR Text** |
| **🆕 Agent (Bibliothekar)** | **auf dem Server (VPS)** | Versteht Frank, ordnet ein, erkennt Dubletten, fragt zurueck, holt raus, antwortet als Text. Nutzt ein Cloud-LLM zum *Denken* — veraendert NIE den 1:1-Inhalt |
| **Speicher** | auf dem Server | Bleibt dumm + 1:1 (`brain-api` + Qdrant). Der Agent ruft dessen 8 Werkzeuge auf |

**Warum der Agent auf dem Server liegt:** Dubletten-Erkennung braucht den Blick auf den GANZEN Speicher;
das Kategorien-Gedaechtnis muss EINE Wahrheit sein; die Geraete bleiben dumm (neues Geraet = nur
WireGuard-Anbindung). Alle Geraete sprechen denselben Agenten ueber den Tunnel an.

---

## 2. Festgelegte Entscheidungen (Frank, 2026-06-23)

- **Erst die Speicher-Seite** bauen, Abfrage-Seite spaeter — nicht vermischen.
- **Modell: Gemini 3.1 Flash Lite** (gut mehrsprachig, vernuenftige Intelligenz, 1-Mio-Kontext), aber
  **austauschbar** per Config (Modell-Wahl spaeter im Dashboard). Modellname beim Einrichten an der
  Live-Doku verifizieren.
- **Gespraech: 30-Minuten-Fenster** (Inaktivitaet). Danach Kontext-Reset; der Chat wird **gespeichert, NIE
  geloescht** — **ZWEIFACH**: (1) 1:1 ins Gehirn (Kategorie `gespraeche`), (2) als **einfache .txt-Datei** auf
  der Samba-Platte *Gedanken* unter `Logbuch/<Jahr>/<Monat>/`. = ewiges UND browsbares Gespraechs-Gedaechtnis.
- **Agent auf dem Server**, Text rein/raus, gesteuert ueber einen **System-Prompt** (Rolle / Aufgabe /
  Kontext / Ausgabeformat).
- **Ton: ganz normales, menschliches Deutsch — wie Smalltalk.** Natuerlich, nicht steif/technisch.
- **Dashboard spaeter** (von jedem Rechner erreichbar, grafische Uebersicht, Einstellungen fuer Server +
  Brain, inkl. Modell-Wahl).

---

## 3. Phase 4a — die Speicher-Seite (IN SCOPE)

### 3.1 Schnittstelle
Neuer Dienst **`sb-agent`** (Docker-Container auf dem VPS), gebunden an die WireGuard-IP, z.B.
`10.8.0.1:8002`, ein Text-Endpunkt (z.B. `POST /chat`), Bearer-Auth (wie brain-api/mcp). **Text rein,
Text raus.** STT/TTS macht das Frontend, NICHT der Agent.

### 3.2 System-Prompt (das "Wesen" des Agenten)
Enthaelt **Rolle** (freundlicher Bibliothekar von Frank), **Aufgabe** (Wissen 1:1 einordnen + speichern),
**Kontext** (die feste Kategorienliste, das 1:1-Prinzip, dass er nie Inhalt umschreibt), **Ausgabeformat**
(kurze Klartext-Antwort an den Sender) und **Ton** (normales, menschliches Deutsch, smalltalk-artig).

### 3.3 Ablauf (Schritt fuer Schritt)
1. Frontend → Text ueber WireGuard an `sb-agent`.
2. Agent (Gemini Flash Lite + System-Prompt) liest den Text → bestimmt **Kategorie** + formuliert **Titel**.
3. Agent macht **Aehnlichkeits-Check** (Dubletten, siehe 3.5).
4. **Rueckmeldung an den Sender** falls noetig (neue Kategorie / Dublette — siehe 3.4/3.5), sonst direkt weiter.
5. Speichert den Text **1:1** ueber `brain-api /store` (text, title, category) — keine KI-Bearbeitung des Inhalts.
6. Antwortet dem Sender in normalem Deutsch: was abgelegt wurde (Kategorie/Titel) + Hinweise.
7. Haelt den Gespraechskontext **30 min**; bei Inaktivitaets-Timeout → Logbuch (3.6).

### 3.4 Kategorie-Verhalten (Frank bestaetigt)
- Passt der Eintrag **eindeutig in eine bestehende** Kategorie → **direkt einordnen**, nur kurz mitteilen (kein Stopp).
- Will der Agent eine **neue Kategorie** anlegen (gibt's noch nicht) → **Vorschlag zurueck an Frank**, Frank
  antwortet: Vorschlag uebernehmen ODER eine andere nennen ("nimm lieber Freizeit") — der Agent versteht
  das natuerlichsprachlich und legt entsprechend an.
- Der Agent **kennt immer alle** Kategorien und pflegt eine **mitwachsende Liste** (eigenes Kategorien-
  Gedaechtnis, getrennt vom Inhalt).

### 3.5 Dubletten-Erkennung
Ein Mechanismus (semantischer Aehnlichkeits-Check), zwei Pruefrichtungen:
- **(b) Hauptfall:** neue Info vs. **schon im Gehirn** Gespeichertes (semantische Suche vor dem Ablegen).
- **(a) Zusatz:** Dubletten **innerhalb derselben Eingabe-Sitzung** (gegen die gerade angenommenen Punkte) —
  wie bei den 177 (mehrere fast gleiche Saetze).
- Bei Treffer: **Rueckfrage an den Sender** — "aehnlich zu 'X' — ersetzen / als neu speichern / abbrechen?",
  dann auf Antwort warten. (Frank will hier die Kontrolle.)
- **WICHTIG — zwei getrennte Spuren:** Der Fakten-Dublettencheck prueft NUR gegen **Fakten**, NIE gegen die
  **Gespraechs-Logs** (sonst verschmutzt das die Sortierung).

### 3.6 Gespraech & Logbuch (ewiges Gedaechtnis — Franks Idee, verfeinert 2026-06-23)
- Der Agent hat ein **Kurzzeit-Gedaechtnis pro Gespraech** (Rueckfragen/Bezuege funktionieren), Fenster **30 min**.
- Nach **30 min ohne Aktivitaet**: Kontext-Reset, der ganze Chat wird **ZWEIFACH** gesichert (nichts geloescht):

  **(1) 1:1 ins Gehirn** — Kategorie `gespraeche`, Titel = Datum/Uhrzeit, voller Verlauf → semantisch
  durchsuchbar (per `by-date`/`recall`: "worueber haben wir am 23.06. gesprochen").

  **(2) Als einfache `.txt`-Sicherheitskopie auf der Samba-Platte *Gedanken*** (Server-Pfad
  `/srv/samba/gedanken/Logbuch/`, fuer Frank = `Z:\Logbuch\`). Einfache, wiederverwendbare Struktur:
  - **Sortiert in Unterordner** `Logbuch/<Jahr>/<Monat 2-stellig>/` — z.B. `Logbuch/2026/06/`. (Ueber Jahre =
    perfekter Rueckblick statt tausender loser Dateien.)
  - **Dateiname** gut leserlich: `<TT.MM.JJJJ> - <H.MM> Uhr.txt` — z.B. `23.06.2026 - 7.05 Uhr.txt`.
  - **Inhalt** als reiner Text — erste Zeile Kategorie, dann Datum/Uhrzeit, darunter der GANZE Verlauf 1:1,
    klar getrennt zwischen **Frank:** und **Agent:**:
    ```
    Kategorie: Gespraeche
    Datum/Uhrzeit: 23.06.2026 - 7.05 Uhr

    Frank: <…>
    Agent: <…>
    Frank: <…>
    Agent: <…>
    ```
- Der Agent (Container auf dem VPS) schreibt die `.txt` direkt in `/srv/samba/gedanken/Logbuch/…`; ueber Samba
  erscheint sie automatisch auf Franks `Z:` — kein extra Mount auf dem PC noetig.
- **Trennung bleibt:** Gespraechs-Logs sind eine eigene Spur; der Fakten-Dublettencheck prueft NIE gegen sie.

### 3.7 Modell & Ton
- **Gemini 3.1 Flash Lite**, per Config austauschbar; das LLM **routet/kategorisiert/versteht**, schreibt aber
  **nie** den gespeicherten Inhalt um (1:1 bleibt unangetastet).
- **Ton:** normales, menschliches Deutsch wie im Smalltalk.

---

## 4. Zu bauende Komponenten (4a)
1. **`sb-agent`-Dienst** (Container, compose-Service), Text-Endpunkt an `10.8.0.1:8002`, Bearer-Auth.
2. **System-Prompt** (Rolle/Aufgabe/Kontext/Ausgabeformat/Ton).
3. **Kategorien-Gedaechtnis** (kennt alle, legt neue an, mitwachsende Liste) — Quelle: bestehende Kategorien
   aus dem Gehirn ableiten + eigene kanonische Liste fuehren.
4. **Sitzungs-Speicher** (1-h-Fenster) + **Logbuch-Schreiber** (Chat → `gespraeche`-Eintrag ins Gehirn).
5. **Observability-First** von Anfang an (JSON-Log, Fehler-Faenger, Logik-Sonden + Intent-Checkpoints).
6. **LLM austauschbar** verdrahtet (Config: `model=gemini-3.1-flash-lite`).

---

## 5. Bewusst NICHT in Phase 4a (spaeter)
- **Abfrage-Seite (4b):** Frage verstehen → richtigen Abruf waehlen (by-title/by-category/by-date/search+Filter)
  → im Stil der Frage antworten. (Erst nach der Speicher-Seite.)
- **STT/TTS:** im Frontend, nicht im Agenten.
- **Dashboard:** spaeter (Modell-Wahl, Server-/Brain-Einstellungen, von jedem Rechner).

---

## 6. Offene Detail-Entscheidungen (beim Bau zu klaeren)
- Wie genau das **Kategorien-Gedaechtnis** persistiert wird (aus Qdrant ableiten vs. eigene Mini-Collection/Eintrag).
- Wie der **Sitzungs-Speicher** umgesetzt wird (In-Memory mit 1-h-TTL vs. kleine Datei/DB).
- Auth-Token fuer `sb-agent` (eigener Bearer ODER derselbe wie brain-api).
- Wie/ob der **Speicher-Modus** vs. spaeterer Abfrage-Modus markiert wird (in 4a ist alles "speichern").

---

## 7. Verweise
- [[speicher-schema-1zu1]] — das as-built 1:1-Schema (Datenmodell, Payload, Kategorien, gefilterte Suche)
- [[project_second_brain_flugplan]] — Flugplan (Phase 1-6), [[project_second_brain_memory_server]] — voller Stand
- `second-brain-server/` — der Stack (brain-api, mcp; hier kommt `sb-agent` dazu)
- `~/.claude/rules/observability-first.md` + `observability-live-logic-probes.md` — Sonden-Pflicht beim Bau
