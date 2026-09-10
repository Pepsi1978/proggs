# Werbeaussagen-Korrektur-Liste — BestJournalAndroid

**Datum:** 2026-05-01
**Basis:** `app-roentgen-AUDIT-2026-05-01.md`
**Quelle der Strings:** `app/src/main/res/values/strings.xml`

---

## Anleitung

Jede problematische Werbeaussage hat eine **A-Nummer** (A1, A2, A3, ...).
Pro Aussage gibt es **2-3 Korrektur-Optionen** (O1, O2, O3).

So entscheidest du:
- "Fuer A1 nehmen wir O2" → ich aendere `dashboard_premium_upsell_body` mit Option 2
- "A2-O1 + A3-O3" → ich nehme bei A2 Option 1, bei A3 Option 3

Wenn du zustimmst, **uebersetze ich die Aenderungen automatisch in alle 27 Sprachen** ueber den `uebersetzung`-Skill.

---

## TEIL 1 — KRITISCH (UWG §5 / EU UCPD Art. 6)

> Diese Aussagen muessen vor dem naechsten Release korrigiert werden. Sie behaupten "Unbegrenzte KI", obwohl die App in Wirklichkeit ein Tageslimit von 150 KI-Aufrufen hat (Cooldown ab dem 101., Hard-Block ab dem 151.).

### A1 — Dashboard Premium-Upsell

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Mit Premium bekommst du unbegrenzte Analysen aus 5 verschiedenen Perspektiven." |
| **String-Key + Pfad** | `dashboard_premium_upsell_body` — `strings.xml:252` |
| **Wo der Nutzer das sieht** | Dashboard-Karte (Tab 1), Upsell-Banner fuer Free-Nutzer |
| **Code-Realitaet** | Premium-Nutzer haben kein Wochen-Limit, ABER: ab dem 101. KI-Aufruf am Tag → 30-Min-Cooldown, ab dem 151. → komplett gesperrt fuer den Tag. Quelle: `Constants.kt:99-102` (`SUB_HARD_LIMIT = 151`), `AiRateLimiter.kt:27-29` |
| **Warum problematisch** | "Unbegrenzt" ist nachweislich falsch — ein aktiver Nutzer trifft das Limit von 150/Tag bei intensiver Nutzung |

**Korrektur-Optionen:**

- **A1-O1:** "Mit Premium bekommst du grosszuegige taegliche Analysen aus 5 verschiedenen Perspektiven."
- **A1-O2:** "Mit Premium bekommst du taeglich bis zu 150 KI-Analysen aus 5 verschiedenen Perspektiven."
- **A1-O3:** "Mit Premium analysierst du Tag fuer Tag deine Eintraege aus 5 verschiedenen Perspektiven — so viel du brauchst."

---

### A2 — Settings + Paywall + Onboarding: KI-Textverbesserung

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Unbegrenzte KI-Textverbesserung [fuer jeden Eintrag]" |
| **String-Keys + Pfad** | `settings_premium_feature_improve` (strings.xml:491) — "Unbegrenzte KI-Textverbesserung"<br>`paywall_feature_improve` (strings.xml:1078) — "Unbegrenzte KI-Textverbesserung fuer jeden Eintrag"<br>`onboarding_premium_feature_improve` (strings.xml:1029) — "Unbegrenzte KI-Textverbesserung, jeder Eintrag wird klarer und ausdrucksstaerker" |
| **Wo der Nutzer das sieht** | Settings-Premium-Block, Paywall-Feature-Liste, Onboarding-Premium-Vorschau |
| **Code-Realitaet** | Gleiches Tages-Hard-Limit 150 wie A1 — Textverbesserung zaehlt zum gleichen Pool wie Dashboard-Analysen |
| **Warum problematisch** | "Unbegrenzt" trifft wieder nicht zu — gleicher Hard-Block bei 151/Tag |

**Korrektur-Optionen (gelten fuer alle 3 Strings gleichzeitig):**

- **A2-O1:** Kurz: "Grosszuegige taegliche KI-Textverbesserung" / Lang (Onboarding): "Grosszuegige taegliche KI-Textverbesserung — jeder Eintrag wird klarer und ausdrucksstaerker"
- **A2-O2:** Kurz: "Bis zu 150 KI-Textverbesserungen pro Tag" / Lang: "Bis zu 150 KI-Textverbesserungen pro Tag — jeder Eintrag wird klarer und ausdrucksstaerker"
- **A2-O3:** Kurz: "KI-Textverbesserung fuer jeden Eintrag, Tag fuer Tag" / Lang: "KI-Textverbesserung fuer jeden Eintrag, Tag fuer Tag — fuer klarere und ausdrucksstaerkere Texte"

---

### A3 — Settings: Dashboard-Analysen

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Unbegrenzte Dashboard-Analysen" |
| **String-Key + Pfad** | `settings_premium_feature_dashboard` — `strings.xml:493` |
| **Wo der Nutzer das sieht** | Settings → Premium-Bereich — Feature-Bullet |
| **Code-Realitaet** | Gleicher Tages-Hard-Limit 150 — Dashboard-Analysen + Textverbesserung teilen sich denselben Tages-Pool |
| **Warum problematisch** | "Unbegrenzt" stimmt nicht — Block bei 151/Tag |

**Korrektur-Optionen:**

- **A3-O1:** "Grosszuegige taegliche Dashboard-Analysen"
- **A3-O2:** "Dashboard-Analysen Tag fuer Tag"
- **A3-O3:** "Dashboard-Analyse so oft du willst — taeglich"

---

### A4 — Settings: Premium-Beschreibung Gesamttext

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Alle Features freigeschaltet, unbegrenzte KI, PDF-Export und mehr." |
| **String-Key + Pfad** | `settings_premium_desc` — `strings.xml:485` |
| **Wo der Nutzer das sieht** | Settings → Premium-Bereich — Hauptbeschreibungs-Text |
| **Code-Realitaet** | "unbegrenzte KI" wieder mit dem 150/Tag-Hard-Limit — alle KI-Features teilen sich diesen Pool |
| **Warum problematisch** | "Unbegrenzte KI" als Hauptverkaufsargument im Settings-Hauptblock |

**Korrektur-Optionen:**

- **A4-O1:** "Alle Features freigeschaltet, grosszuegiges taegliches KI-Kontingent, PDF-Export und mehr."
- **A4-O2:** "Alle Features freigeschaltet, taegliche KI-Analyse und Textverbesserung, PDF-Export und mehr."
- **A4-O3:** "Alle Features freigeschaltet, KI-Analysen Tag fuer Tag, PDF-Export und mehr."

---

### A5 — Churn-Dialog: KI-Analysen

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Unbegrenzte KI-Analysen" |
| **String-Key + Pfad** | `churn_offer_feature_ai` — `strings.xml:1134` |
| **Wo der Nutzer das sieht** | ChurnFlowDialog Step 2 — Retention-Angebot beim Versuch zu kuendigen |
| **Code-Realitaet** | Gleicher Tages-Hard-Limit 150 |
| **Warum besonders sensibel** | Im Cancel-Flow gilt strenge Massstaebe — der Nutzer ist im Kuendigungs-Modus, jede irrefuehrende Aussage hier ist UWG-relevanter |

**Korrektur-Optionen:**

- **A5-O1:** "Grosszuegige taegliche KI-Analysen"
- **A5-O2:** "KI-Analysen Tag fuer Tag"
- **A5-O3:** "Bis zu 150 KI-Analysen pro Tag"

---

## TEIL 2 — HOCH (UWG §5 / §5a)

### A6 — Settings: Lifetime "fuer immer"

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Einmalkauf, alle Features fuer immer freigeschaltet." |
| **String-Key + Pfad** | `settings_premium_lifetime_desc` — `strings.xml:484` |
| **Wo der Nutzer das sieht** | Settings → Premium-Bereich (nur fuer Lifetime-Kaeufer sichtbar) |
| **Code-Realitaet** | Lifetime ist an die App-Verfuegbarkeit gebunden. Wenn die App eingestellt wird, kein Rueckgabeanspruch. BGH-Rechtsprechung verlangt Vorbehalt bei dauerhaften digitalen Lizenzen |
| **Warum problematisch** | "Fuer immer" suggeriert ewige Verfuegbarkeit unabhaengig vom App-Bestand |

**Korrektur-Optionen:**

- **A6-O1:** "Einmalkauf, alle Features dauerhaft freigeschaltet, solange die App verfuegbar ist."
- **A6-O2:** "Einmalkauf, alle Premium-Features ohne Ablaufdatum (an die App gebunden)."
- **A6-O3:** "Einmalkauf, alle Features langfristig freigeschaltet — solange Best Journal verfuegbar ist."

---

### A7 — Paywall: Lifetime-Beschreibung

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Einmal zahlen, alles fuer immer nutzen" |
| **String-Key + Pfad** | `paywall_lifetime_desc` — `strings.xml:1071` |
| **Wo der Nutzer das sieht** | Paywall-Bildschirm — Lifetime-Surface (Amber-Rahmen) |
| **Code-Realitaet** | Wie A6 — App-bezogen |
| **Warum problematisch** | "Fuer immer" auf Paywall ist besonders sensibel weil hier die Kaufentscheidung getroffen wird |

**Korrektur-Optionen:**

- **A7-O1:** "Einmal zahlen, dauerhaft nutzen, solange die App verfuegbar ist"
- **A7-O2:** "Einmalkauf ohne Ablaufdatum (an die App-Verfuegbarkeit gebunden)"
- **A7-O3:** "Einmal zahlen, langfristig alle Features nutzen"

---

### A8 — Paywall: "Unbegrenzte Tagebucheintraege"

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Unbegrenzte Tagebucheintraege" |
| **String-Key + Pfad** | `paywall_feature_unlimited_entries` — `strings.xml:1396` |
| **Wo der Nutzer das sieht** | Paywall-Bildschirm — Feature-Bullet |
| **Code-Realitaet** | Es gibt **fuer Free-Nutzer kein Eintrags-Limit**. Free kann genauso viele Tagebucheintraege schreiben wie Premium. Diese Aussage als **Premium-Vorteil** beworben ist faktisch falsch |
| **Warum problematisch** | UWG §5 — Eigenschaft als Premium beworben, die kostenlos verfuegbar ist. Google Play Review koennte das beanstanden |

**Korrektur-Optionen (Empfehlung: ENTFERNEN):**

- **A8-O1 (ENTFERNEN):** Den Bullet komplett aus der Paywall-Feature-Liste entfernen — er ist kein Premium-Differentiator
- **A8-O2 (UMFORMULIEREN):** "Alle Eintraege ohne Datenlimit gespeichert" (kein Differentiator-Anspruch)
- **A8-O3 (UMFORMULIEREN auf etwas das WIRKLICH Premium ist):** Bullet ersetzen durch z.B. "Volltextsuche ueber alle Eintraege" oder anderes echtes Premium-Feature

---

## TEIL 3 — MITTEL (Hyperbel grenzwertig)

### A9 — Onboarding-Headline / Paywall-Headline: "ohne Grenzen"

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussagen** | "Dein persoenlicher KI-Begleiter ohne Grenzen"<br>"Dein kreativer Begleiter ohne Grenzen" |
| **String-Keys + Pfad** | `paywall_headline_default_sub` (strings.xml:1095)<br>`paywall_headline_thoughts_sub` (strings.xml:1093) |
| **Wo der Nutzer das sieht** | Paywall-Untertitel (Default + Goal "Gedanken ordnen") |
| **Code-Realitaet** | "Ohne Grenzen" passt nicht zum 150/Tag-Hard-Limit. Hyperbel grenzwertig — Gerichte tolerieren Werbe-Uebertreibung, aber bei nachweisbarem Hard-Limit angreifbar |
| **Warum problematisch** | UWG §5 Abs. 3 (Hyperbel-Ausnahme) gilt nur wenn Verbraucher den Uebertreibungs-Charakter erkennt — bei "ohne Grenzen" + tatsaechlichem Limit fraglich |

**Korrektur-Optionen:**

- **A9-O1:** "Dein persoenlicher KI-Begleiter, jeden Tag" / "Dein kreativer Begleiter, Tag fuer Tag"
- **A9-O2:** "Dein persoenlicher KI-Begleiter" / "Dein kreativer Begleiter" (komplett ohne Zusatz)
- **A9-O3 (mutigere Variante):** "KI-Unterstuetzung wann immer du sie brauchst" / "Mit dir, wann immer du schreiben willst"

---

### A10 — Profile: "Unendlich"

| Spalte | Inhalt |
|--------|--------|
| **Aktuelle Aussage** | "Unendlich viele individuelle Profile anlegen" |
| **String-Key + Pfad** | `profiles_benefit_many` — `strings.xml:405` |
| **Wo der Nutzer das sieht** | Profile-Premium-Beschreibung im Settings/Profile-Sheet |
| **Code-Realitaet** | Premium hat tatsaechlich kein Profil-Limit. Free: max. 2 Profile (`SettingsScreen.kt:1608, 1718`) |
| **Warum problematisch** | "Unendlich" ist Hyperbel — faktisch stimmt's, aber stilistisch wirkt's wie eine Werbe-Uebertreibung |

**Korrektur-Optionen (NIEDRIG-Prioritaet, optional):**

- **A10-O1:** "Beliebig viele individuelle Profile anlegen"
- **A10-O2:** "So viele individuelle Profile wie du brauchst"
- **A10-O3:** Behalten — "Unendlich" ist akzeptabel als Werbesprache

---

## TEIL 4 — Werbeaussagen die OK SIND (NICHT aendern)

Diese Aussagen sind technisch korrekt und brauchen KEINE Korrektur:

| String-Key | Aussage | Warum OK |
|-----------|---------|----------|
| `follow_up_premium_title` | "Unbegrenzte Nachtraege" | Premium hat tatsaechlich kein Nachtrags-Limit pro Eintrag. Free: 1 Nachtrag |
| `paywall_feature_followups` | "Unbegrenzte Nachtraege zu jedem Tagebucheintrag" | Wie oben |
| `settings_premium_feature_followups` | "Unbegrenzte Nachtraege" | Wie oben |
| `paywall_feature_profiles` | "Unbegrenzte individuelle Analyse-Profile" | Premium hat kein Profil-Limit |
| `settings_premium_feature_profiles` | "Unbegrenzte individuelle Profile" | Wie oben |
| `retro_benefit_weekly` | "Unbegrenzte Wochenrueckblicke, nicht nur die ersten 2 Wochen" | Premium kann beliebig viele Wochenrueckblicke generieren — Beschraenkung "2 Wochen" wird im selben String korrekt fuer Free kommuniziert |
| `paywall_lifetime_title` | "Einmalkauf" | Reine Bezeichnung, OK |
| `paywall_lifetime_note` | "Kein Abo, keine Verlaengerung" | Stimmt, Lifetime ist INAPP nicht Subscription |
| `settings_premium_feature_5_perspectives` | "5 KI-Perspektiven" | Stimmt — 5 Profile (Summary, Entropy, Insight, Goals, Custom) |
| `settings_premium_lifetime` | "Lifetime-Zugang aktiv" | Reines Status-Label |

---

## Zusammenfassung — Fix-Reihenfolge

| Prio | Nr | Anzahl Strings | Aufwand mit `uebersetzung`-Skill |
|------|-----|---------------|----------------------------------|
| KRITISCH | A1 | 1 String × 27 Sprachen | ~5 Min |
| KRITISCH | A2 | 3 Strings × 27 Sprachen | ~10 Min |
| KRITISCH | A3 | 1 String × 27 Sprachen | ~5 Min |
| KRITISCH | A4 | 1 String × 27 Sprachen | ~5 Min |
| KRITISCH | A5 | 1 String × 27 Sprachen | ~5 Min |
| HOCH | A6 | 1 String × 27 Sprachen | ~5 Min |
| HOCH | A7 | 1 String × 27 Sprachen | ~5 Min |
| HOCH | A8 | 1 String × 27 Sprachen (oder ENTFERNEN) | ~5 Min |
| MITTEL | A9 | 2 Strings × 27 Sprachen | ~5 Min |
| NIEDRIG | A10 | 1 String × 27 Sprachen (optional) | ~5 Min |

**Gesamt-Aufwand bei voller Umsetzung:** 13 Strings × 27 Sprachen = **351 Aenderungen**, mit `uebersetzung`-Skill ~60-90 Minuten.

---

## Wie es weitergeht

Sage mir einfach welche Optionen du bevorzugst, z.B.:

> "A1-O2, A2-O1, A3-O1, A4-O1, A5-O1, A6-O1, A7-O1, A8-O1 (entfernen), A9-O2, A10 ueberspringen"

Dann mache ich:
1. Aenderungen in der deutschen `values/strings.xml`
2. `uebersetzung`-Skill aufrufen → alle 26 weiteren Sprachen
3. Verifikation per Diff
4. Commit + Push + APK-Install auf dein Geraet
