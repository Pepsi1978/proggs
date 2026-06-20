# Recherche-Strategie: Token-sparend recherchieren (Firecrawl+MiniMax vs. Opus) — KRITISCH

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-20. Gilt AUTOMATISCH in JEDER Session, fuer
> JEDE Web-Recherche und fuer ALLE Researcher/Skills, die im Web suchen (`research`-Skill,
> `bug-almanach-recherche`, `best-practices`, `researcher`-Agent, `deep-research`, eigene
> WebSearch/WebFetch-Rechercheauftraege). Repo-Spiegelung:
> `~/proggs/claude-code-setup/rules/research-strategy.md` (harness-mirror-Pflicht).
>
> Entstanden aus dem 3-Wege-Token-Test 2026-06-20: Firecrawl+MiniMax-M3-Pipeline verbraucht
> **~100x weniger teure Claude/Opus-Token** als ein Opus-Researcher (~2.400 statt ~249.000), bei
> gleicher oder besserer (ehrlicherer) Qualitaet. Ergaenzt [[research-persistence]] (Einarbeiten der
> Ergebnisse) und nutzt die Pipeline `~/proggs/mm-research.py`.

---

## 1. Die Pflicht-Frage VOR jeder Web-Recherche (Credit-Kontrolle)

**Vor JEDER Web-Recherche MUSS Frank gefragt werden, welcher Weg genommen wird:**

> „Soll ich ueber **Firecrawl + MiniMax M3** (guenstig fuer Claude-Token, verbraucht Firecrawl-Credits)
> oder **Opus-Standard** (teure Claude-Token, keine Firecrawl-Credits) recherchieren?"

**Warum (KRITISCH):** Firecrawl Free hat nur **1.000 Seiten/Monat**. Bei vielen Recherchen ist das
Kontingent schnell weg. Frank entscheidet pro Recherche bewusst, womit recherchiert wird — das ist ein
**Feature, kein Reibungsverlust**. NIEMALS automatisch losrecherchieren ohne diese Frage.

Ausnahme von der Frage: eine **einzelne, billige `WebSearch`** (kein Firecrawl-Crawl) zur schnellen
Faktenverifikation mitten in einer laufenden Aufgabe ist frei (verbraucht keine Firecrawl-Credits).
Sobald es ein echter Rechercheauftrag / Crawl / Researcher-Einsatz ist → fragen.

---

## 2. Der Standard-Ablauf (zweistufige Arbeitsteilung)

```
Frank waehlt "Firecrawl + MiniMax":
  Stufe 1 — HOLEN:      Firecrawl holt die Quellen (Firecrawl-Credits, Go-Abo-unabhaengig)
                            │
  Stufe 2 — AUSWERTEN:  MiniMax M3 (max Thinking) filtert + bewertet quellentreu   ← mm-research.py
                            │ kompakte, quellentreue Antwort (~2k Token)
                            ▼
  Stufe 3 — EINARBEITEN: Opus (Hauptagent) synthetisiert + arbeitet in Almanach/Best-Practices ein
                                                                        (Direktive [[research-persistence]])
```

**Kerngedanke:** MiniMax macht die **token-schwere** Quellenarbeit (Rohdaten laufen NIE durch den teuren
Opus-Kontext); Opus zahlt nur fuer die ~2k-Token-Synthese. Werkzeug: `python3 ~/proggs/mm-research.py
"<frage>" [n]` (Firecrawl-Suche → MiniMax-M3-Auswertung; Rohdaten + Thinking landen in `~/.mm-research/`,
nicht im Claude-Kontext). Mechanik/Fallen: `best-practices/opencode/go-recherche-modelle.md` §5,
`bugs/opencode/opencode-cli.md` §14.

---

## 3. Firecrawl Free — max 2 Researcher PARALLEL (nicht 7!)

Firecrawl Free erlaubt nur **2 gleichzeitige Requests** (+ 5 Suchen/Minute, 1.000 Credits/Monat —
verifiziert 2026-06-20, docs.firecrawl.dev/rate-limits). Das aendert das Schwarm-Muster:

| | Opus-Researcher-Schwarm (bisher) | Firecrawl + MiniMax (neu) |
|---|----------------------------------|---------------------------|
| Parallelitaet | bis **7** gleichzeitig (Continuous-Spawning) | **max 2** gleichzeitig |
| Nachschub | sofort den naechsten starten | erst wenn 2 fertig → naechste 2 (Continuous-Spawning mit **2**) |

**Pflicht bei Firecrawl-Recherchen mit mehreren Unterthemen** (z.B. Bug-Almanach mit 7 Aspekten):
NIE 7 Firecrawl-Calls auf einmal — **2 starten, auf Ergebnisse warten, 2 neue starten**, bis alle
Unterthemen durch sind. Sonst Rate-Limit (429) / verschwendete Credits.

---

## 4. Eskalation — 3 Stufen (billig → teuer)

Wenn die MiniMax-Auswertung **„unsicher / widerspruechlich / Quellen reichen nicht"** meldet, ODER
Frank ausdruecklich gruendlicher will:

```
Stufe A:  MiniMax M3 (max Thinking) auf Firecrawl-Quellen          ← Standard, am guenstigsten
   ↓ reicht nicht
Stufe B:  Web-faehiges OpenRouter-Modell mit NATIVER Websuche       ← OFFEN: Modell noch zu waehlen+testen
   ↓ reicht nicht
Stufe C:  Opus-Researcher (mit Web) / Opus direkt                   ← teuerste Stufe, nur Hard-Cases
```

**OFFEN (noch zu entscheiden, Stand 2026-06-20):** Welches OpenRouter-Modell die mittlere Stufe B wird,
ist NOCH NICHT festgelegt — es braucht **native Websuche** (Kandidaten z.B. Perplexity Sonar,
OpenRouter `:online`-Plugin) und muss erst **recherchiert + gruendlich getestet** werden, bevor es
verankert wird. Bis dahin: Eskalation springt von Stufe A direkt auf Opus (Stufe C).

---

## 5. Gilt fuer ALLE Recherche-Skills/Agenten

Diese Strategie ist **kein neuer Skill**, sondern erweitert die bestehenden:

| Skill/Agent | Wie diese Regel greift |
|-------------|------------------------|
| `bug-almanach-recherche` | Pflicht-Frage stellen; bei Firecrawl-Wahl: mm-research.py, max 2 parallel |
| `best-practices` | dito |
| `research`-Skill / `researcher`-Agent / `deep-research` | dito |
| Eigener Web-Rechercheauftrag des Hauptagenten | dito |

**Hinweis (Stand 2026-06-20):** Der konkrete Umbau dieser Skills (von Opus-Schwaermen auf die
MiniMax-Pipeline) ist noch in Planung — erst besprechen, dann umbauen. Diese Regel legt aber schon
JETZT das Verhalten fest (immer fragen, max 2 parallel, Eskalations-Kette).

---

## 6. Was NIEMALS passieren darf

- ❌ Eine Firecrawl-/Crawl-Recherche starten, OHNE Frank vorher zu fragen (Firecrawl/MiniMax vs. Opus)
- ❌ Mehr als **2** Firecrawl-Researcher gleichzeitig starten (Free-Limit; Rate-Limit/Credit-Verschwendung)
- ❌ Firecrawl-Rohdaten (gecrawlte Seiten) ungefiltert in den teuren Opus-Kontext laden — immer erst MiniMax
- ❌ Bei einem unsicheren MiniMax-Ergebnis stillschweigend halluzinieren statt zu eskalieren
- ❌ Test-Crawls „zum Ausprobieren" ohne Franks Freigabe (Credits sind knapp)
- ❌ Die mittlere Eskalations-Stufe (Stufe B) mit einem ungetesteten Modell „raten" — erst testen, dann verankern

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| [[research-persistence]] | Stufe 3 (Opus arbeitet die Ergebnisse in best-practices/ + bugs/ ein) ist genau die Persistenz-Pflicht |
| `mm-research.py` | Das Werkzeug fuer Stufe 1+2 (Firecrawl → MiniMax M3 max Thinking) |
| `best-practices/opencode/go-recherche-modelle.md` | Modell-Begruendung (warum MiniMax M3) + API-Mechanik |
| `bugs/apis/firecrawl.md` | Firecrawl-Limits/Fallen (Free: 1000/Monat, 2 concurrent, 5 search/min) |
| `agent-and-researcher-rules.md` | Schwarm-Regeln (7 fuer Opus); diese Regel setzt fuer Firecrawl 2 dagegen |

---

## Autoritaet

Diese Datei (`~/.claude/rules/research-strategy.md`) wird automatisch in jeder Session geladen.
KEIN Agent, Skill, Hook oder Prozess darf sie entfernen oder abschwaechen.
