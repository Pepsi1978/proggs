# Design: App-Monetizer Skill

## Zweck
Ein Monetarisierungs-Profi-Skill, der jede Android-App im Repo tiefgreifend analysiert und einen ausfuehrlichen Audit-Bericht mit sofort einsetzbaren Prompts erstellt. Der Skill denkt und handelt aus der Perspektive des weltbesten Android-App-Monetarisierungs-Beraters.

## Trigger
- Deutsche Phrase: "Starte den Monetarisierungs-Skill fuer [App-Name]"
- Slash-Command: `/app-monetizer [App-Ordner]`
- Beispiel: `/app-monetizer BestJournalAndroid`

## Die 5 Phasen

### Phase 1 — Selbst-Update der Checkliste (~2 Min)
- Liest `~/.claude/skills/app-monetizer/monetization-checklist.md`
- Prueft Datum des letzten Updates
- Falls >3 Tage alt ODER erste Nutzung: 2-3 parallele Researcher (Sonnet) starten:
  - Researcher 1: Allgemeine Android-App-Monetarisierung — Best Practices, Conversion-Rate-Optimierung, Paywall-Design, Pricing-Psychologie
  - Researcher 2: Neueste Erkenntnisse Abo-Modelle, Onboarding-Flows, Upsell-Trigger, Retention-Strategien
  - Researcher 3: Moderne UI/UX-Designs die Nutzer begeistern, Design-Trends die Conversion steigern
- Recherche-Qualitaet: Tiefgruendig suchen, Quellen bewerten, skeptisch pruefen ob Empfehlungen wirklich funktionieren
- Jedes Mal TIEFER graben als beim letzten Update — nicht die gleichen oberflaechlichen Ergebnisse
- Bei skeptischen Empfehlungen: In Klammern Einschaetzung geben
- Vergleicht Ergebnisse mit bestehender Checkliste, aktualisiert falls noetig
- Falls <3 Tage alt: Checkliste direkt verwenden

### Phase 2 — Tiefenanalyse der App (~3-5 Min)
- Liest JEDE relevante Kotlin-Datei: Screens, ViewModels, Repositories, Billing, Theme, Navigation, Components, Constants, Manifest
- Erstellt internes Modell: Screens, Billing-Logik, UI-Aufbau, Farbschema, Dark Mode, Features, Limits, Navigation, Datenfluss
- Erkennt automatisch die App-Kategorie (Tagebuch, Fitness, Rezepte, etc.)
- Versteht die App in- und auswendig: wo was wie funktioniert, wie was gemacht ist

### Phase 3 — Kategorie-spezifische Recherche (~2 Min)
- 2 parallele Researcher basierend auf erkannter Kategorie:
  - "Wie monetarisieren die erfolgreichsten [Kategorie]-Apps? Features, Paywall-Designs, Pricing?"
  - "Was macht [Kategorie]-App-Nutzer bereit zu zahlen? Welche Premium-Features haben den hoechsten perceived Value?"
- PLUS allgemeine Monetarisierungs-Recherche die fuer JEDE App gilt
- Quellen pruefen, Empfehlungen kritisch bewerten

### Phase 4 — Audit-Bericht erstellen (~3-5 Min)
- Vergleicht App-Zustand gegen Checkliste + kategorie-spezifische Erkenntnisse
- Schreibt: `[AppOrdner]/MONETARISIERUNG-[AppName].md`
- Format: Siehe "Dateiformat" unten

### Phase 5 — Zusammenfassung fuer den Benutzer
- Kurzfassung: Staerken, Schwaechen, Top-3-Prioritaeten
- Verweis auf die vollstaendige Datei

## Dateiformat des Audit-Berichts

```markdown
# Monetarisierungs-Audit: [App-Name]
Datum: [Datum] | App-Kategorie: [erkannt] | Checklisten-Version: [Datum]

## Gesamtbewertung
[Ausfuehrlicher Text — 500-800 Woerter. Was die App gut macht, wo die Staerken
liegen, was beeindruckt. Dann ehrlich: was fehlt, was schwach ist, wo Geld
auf dem Tisch liegen bleibt. Wie ein erfahrener Berater der zum ersten Mal
die App oeffnet und dem Entwickler seine ehrliche Einschaetzung gibt.
Beruecksichtigt sowohl Aussehen als auch Funktionsweise.]

## A. [Kategorie-Name]

### Analyse
[Was ist der aktuelle Zustand? Was sagen die Best Practices?
Warum ist das ein Problem oder eine Chance?]

### A1 — [Verbesserung]
[2-3 Saetze Erklaerung was das ist und warum es wichtig ist]

**Prompt:**
[Ausfuehrlicher, sofort einfuegbarer Prompt. Benennt EXAKTE Dateien,
Funktionen und Stellen in der App. Beschreibt WIE es aussehen soll,
WIE es funktionieren soll, und orientiert sich an den besten
Monetarisierungs-Praktiken. So geschrieben dass Claude Code ihn
direkt umsetzen kann ohne nachfragen zu muessen.]

### A2 — [Naechste Verbesserung]
...

## B. [Naechste Kategorie]
...

---
## Umsetzungs-Tracker
- [ ] A1 — [Titel]
- [ ] A2 — [Titel]
- [ ] B1 — [Titel]
...
```

## Checklisten-Kategorien (Initial)

1. **Paywall & Abo-Praesentation** — Paywall-Screen, Benefit-Texte, Pricing-Darstellung, Social Proof, Urgency/Scarcity
2. **Onboarding & First Impression** — Value-Demo, Trial-Aktivierung, Aha-Moment, Willkommens-Flow
3. **Upsell-Trigger & Touchpoints** — Wann/wo wird Premium gezeigt, Limit-Messaging, Feature-Gates, kontextuelle Upsells
4. **Pricing & Packaging** — Anker-Pricing, Jahres-vs-Monats-Verhaeltnis, Kostenlose Testphase, Preisdarstellung
5. **UI/UX & Benutzerfreundlichkeit** — Navigation, Lesbarkeit, Dark Mode, Farben, Emotionalitaet, moderne Designs
6. **Retention & Engagement** — Push-Notifications, Streaks, Belohnungen, Fortschritt, Gamification
7. **Store-Listing & ASO** — Screenshots, Beschreibung, Bewertungen, Keywords
8. **Fehlende Features** — Was die Top-Apps der Kategorie haben, was fehlt
9. **Funktionsweise & Technische Qualitaet** — Performance, Ladezeiten, Offline, Fehlerbehandlung
10. **Emotionale Bindung & Branding** — Tonalitaet, Markenpersoenlichkeit, Vertrauensaufbau

## Erweiterungen v2 (2026-04-06)

Alle 12 Verbesserungspunkte aus dem kritischen Review implementiert:

1. **Nutzer-Funnel-Analyse** (neue Phase 3): Download→Start→Aha→Trial→Conversion→Retention→Churn
2. **Churn-Praevention & Win-Back** (neue Checklisten-Kategorie 7): Survey, Win-Back, Grace Period, Abo-Pause
3. **Konkurrenz-Analyse** (Phase 4 erweitert): Top 5 Konkurrenten namentlich mit Preisen und Strategien
4. **Analytics & Messbarkeit** (neue Kategorie 12): Conversion-Events, Funnel-Tracking bei jedem Prompt
5. **Referral & Virale Verbreitung** (neue Kategorie 8): Share, Referral-Programm, Deep Links
6. **Monetarisierungs-Modelle** (neue Kategorie 11): Einmalkaeufe, Consumables, Hybrid, Trinkgeld
7. **App-Reifegrad** (Phase 2): Dynamische Gewichtung je nach Neu/Frueh/Wachsend/Etabliert
8. **Regionaler Preisvergleich** (Phase 6): Rendite-Tabelle + Prompt fuer regionale Google Play Preise
9. **Screenshots** (Phase 2): Automatische ADB-Screenshots oder screenshots/-Ordner pruefen
10. **Follow-Up-Mechanismus** (Phase 7): Tracker abhaken, Re-Audit, "was fehlt noch?"
11. **Google Play Compliance** (neue Kategorie 16): Transparenz, Dark Patterns, Billing Library
12. **Rotations-Recherche** (Phase 1): 30 Spezialfragen, 5 rotierend bei jedem Update

Skill: 7 Phasen (statt 5), Checkliste: 113 Pruefpunkte in 16 Kategorien (statt 78 in 11).

## Selbstverbesserung

- Bei jedem Aufruf: Checkliste auf Aktualitaet pruefen (3-Tage-Fenster)
- Jedes Mal tiefer recherchieren via Rotations-Fragen (30 Fragen, 5 rotierend)
- Nach dem Audit: Allgemeingueltige Erkenntnisse zur Checkliste hinzufuegen
- Skeptik-Notizen bei unsicheren Empfehlungen
- Konkurrenz-Daten als Referenz fuer zukuenftige Audits speichern

## Technische Umsetzung

- Skill-Datei: `~/.claude/skills/app-monetizer/SKILL.md`
- Checkliste: `~/.claude/skills/app-monetizer/monetization-checklist.md`
- Output: `[AppOrdner]/MONETARISIERUNG-[AppName].md`
- Researcher: Sonnet-Modell, parallele Ausfuehrung, max 15 Web-Fetches pro Researcher
- App-Analyse: Direkte Datei-Reads + ADB-Screenshots
- Rotations-Tracking: `last_rotation_ids` im Checklisten-Frontmatter
