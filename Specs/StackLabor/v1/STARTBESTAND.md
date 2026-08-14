# Startbestand — StackLabor
Stand: 14.08.2026 · Stufe: v1

Quelle: `C:\Users\barwa\Meine Ablage\Dokumente\KI\Backup\Stack.docx` (deckungsgleich mit
`~/proggs/NEMS/app/src/main/java/com/nems/app/data/local/SeedDataProvider.kt`, aber mit mehr
Angaben als dort übernommen wurden).

**6 Stacks, 72 Einträge, 63 verschiedene Mittel.** Der Bestand liegt als `startbestand.json` in
den Assets der App und wird über F-21 eingelesen — er ist **nicht einkompiliert**, damit er sich
aktualisieren lässt, ohne die App neu zu bauen.

Regel aus der Quelle, die für jeden Eintrag gilt:
> *„Alle Mengenangaben beziehen sich immer auf 1 Kapsel; wenn in Klammern 2 Kapseln steht, nimmt
> Frank die doppelte Menge der Mengenangabe — beachte dies konsequent bei jedem gelisteten NEM."*

Deshalb werden Stückzahl und Menge je Stück **getrennt** geführt (F-03, Datenmodell) und als
`2 × 80 mg = 160 mg` angezeigt.

Legende: 🟡 = mittleres Durchfallrisiko · 🟢 = Pulver · W = wasserlöslich · F = fettlöslich

## Die sechs Stacks

| id | Name | Zeitpunkt | Einnahme-Hinweis |
|---|---|---|---|
| `morning1` | Morgen-Stack Teil 1 | Direkt nach dem Aufstehen | nur mit Wasser |
| `morning2` | Morgen-Stack Teil 2 | 60 Minuten nach dem Aufstehen | mit Olivenöl und Wasser |
| `presport` | Pre-Sport-Stack | 45 Minuten vor dem Sport (später im Laufe des Tages) | mit Olivenöl und Wasser |
| `evening1` | Abend-Stack Teil 1 | 2 Stunden vor dem Schlafen | mit Wasser |
| `evening2` | Abend-Stack Teil 2 | 60 Minuten vor dem Schlafen | mit 1 EL Olivenöl und Wasser |
| `evening3` | Abend-Stack Teil 3 | Direkt vor dem Schlafen | mit Wasser |

Die Reihenfolge innerhalb jedes Stacks ist die **Einnahme-Reihenfolge** — die Quelle sagt
ausdrücklich „nimmt er … in dieser Reihenfolge ein". Sie wird als `reihenfolge` gespeichert
(F-07); die Löslichkeits-Ansicht (F-06) ist nur eine andere Anzeige derselben Daten.

## morning1 — Morgen-Stack Teil 1

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | Vitamin C | 2 | 80 mg | Kapsel | W | täglich | |
| 2 | Eisen (Bisglycinat) | 1 | 14 mg | Kapsel | W | täglich | enthält zusätzlich 80 mg Vitamin C |
| 3 | L-Theanin | 1 | 500 mg | Kapsel | W | täglich | |
| 4 | Venlafaxin | 1 | 50 mg / **75 mg** | Tablette | W | täglich | 🟡 · **Dosis-Variante**: 50 mg im Frei, 75 mg im Dienst |
| 5 | Hyaluronsäure | 2 | 600 mg | Kapsel | W | täglich | |
| 6 | Vitamin-B Komplex | 1 | — | Kapsel | W | täglich | 🟡 · Greenfood „B100" |
| 7 | Bor | 1 | 3 mg | Kapsel | W | alle 5 Tage | |
| 8 | Selen | 1 | 200 µg | Kapsel | W | alle 3 Tage | |
| 9 | Löwenmähne | 2 | 650 mg | Kapsel | W | täglich | |
| 10 | Uridin Monophosphat | 1 | 300 mg | Kapsel | W | alle 2 Tage | alterniert mit Citicolin |
| 11 | Citicolin | 1 | 250 mg (davon 50 mg Cholin) | Kapsel | W | alle 2 Tage | alterniert mit Uridin + Phosphatidylserin |
| 12 | EAAs | 1 | 10 g | Löffel | W | täglich | 🟢 · **Kombi**: zusammen mit Kollagen |
| 13 | Kollagen | 1 | 10 g | Löffel | W | täglich | 🟢 · **Kombi**: zusammen mit EAAs |
| 14 | Kreatin | 1 | 3 g | Löffel | W | täglich | 🟡 🟢 |
| 15 | Acetyl-L-Carnitin (ALCAR) | 1 | 750 mg | Löffel | W | täglich | 🟡 🟢 |
| 16 | Kaffee (Koffein) | 1 | 1 Tasse | Tasse | W | täglich | |

**Alternierungs-Zyklus** (aus der Quelle wörtlich):
> Tag A: Citicolin 250 mg (ohne Uridin, ohne Phosphatidylserin)
> Tag B: Uridin 300 mg + Phosphatidylserin 150 mg (ohne Citicolin)
> Huperzin-Tag: Phosphatidylserin-Variante (kein Citicolin, kein Uridin)

## morning2 — Morgen-Stack Teil 2

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | L-Tyrosin | 1 | 650 mg | Kapsel | W | täglich | |
| 2 | Vitamin E Komplex | 1 | — | Kapsel | F | täglich | ZENement „Natural Vitamin E Complex" |
| 3 | Curcumin Phytosome | 1 | 500 mg | Kapsel | F | täglich | Thorne |
| 4 | DHEA | 1 | 50 mg | Kapsel | F | alle 7 Tage | |
| 5 | Vitamin D3+K2 | 1 | 5000 IE + 100 µg | Kapsel | F | täglich | |
| 6 | Apigenin | 1 | 200 mg | Kapsel | F | täglich | |
| 7 | Nicotinamid-Ribosid (NR) | 1 | 300 mg | Kapsel | W | täglich | 🟡 |
| 8 | CoQ10 (Ubiquinol) | 1 | 200 mg | Kapsel | F | täglich | |
| 9 | Astaxanthin | 1 | 12 mg | Kapsel | F | täglich | |
| 10 | Ashwagandha KSM-66 | 2 | 600 mg | Kapsel | F | täglich | 🟡 |
| 11 | Magnesium (Bisglycinat) | 1 | 155 mg | Kapsel | W | täglich | 🟡 |
| 12 | Magnesium (L-Threonat) | 1 | 48 mg | Kapsel | W | täglich | 🟡 |
| 13 | Kupfer (Bisglycinat) | 1 | 2 mg | Kapsel | W | täglich | |
| 14 | Mangan (Bisglycinat) | 1 | 10 mg | Kapsel | W | alle 6 Tage | |
| 15 | Phosphatidylserin (PS) | 1 | 150 mg | Kapsel | F | alle 2 Tage | Teil des Alternierungs-Zyklus |
| 16 | Huperzin A | 1 | 200 µg | Kapsel | F | alle 4 Tage | |
| 17 | Ginkgo Biloba | 1 | — | Kapsel | F | alle 4 Tage | |
| 18 | TMG (Trimethylglycin) | 1 | 1 g | Löffel | W | täglich | 🟢 |
| 19 | MSM | 1 | 1 g | Löffel | W | täglich | 🟢 |

## presport — Pre-Sport-Stack

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | PQQ | 1 | 20 mg | Kapsel | W | täglich | |
| 2 | R-Alpha Liponsäure | 1 | 300 mg | Kapsel | W | täglich | 🟡 |
| 3 | Fucoxanthin | 1 | 50 mg | Kapsel | F | täglich | |
| 4 | Rhodiola Rosea Extrakt | 1 | 500 mg | Kapsel | W | täglich | 🟡 |
| 5 | Cordyceps Sinensis Extrakt | 1 | 700 mg | Kapsel | W | täglich | |
| 6 | AAKG | 1 | 6 g | Löffel | W | täglich | 🟢 · Verhältnis 4,2 : 1,8 (4,2 g Arginin + 1,8 g AKG) |
| 7 | Acetyl-L-Carnitin (ALCAR) | 1 | 750 mg | Löffel | W | täglich | 🟡 🟢 |
| 8 | Whey-Protein | 1 | 25 g | Löffel | W | täglich | 🟢 · **Kombi** mit Kollagen und Vitamin C |
| 9 | Kollagen | 1 | 10 g | Löffel | W | täglich | 🟢 · **Kombi** |
| 10 | Vitamin C | 1 | 80 mg | Kapsel | W | täglich | **Kombi** |

## evening1 — Abend-Stack Teil 1

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | L-Theanin | 1 | 500 mg | Kapsel | W | täglich | |
| 2 | GABA | 1 | 500 mg | Kapsel | W | täglich | |
| 3 | Melatonin | 1 | 1 mg | Kapsel | W | täglich | |
| 4 | Zink (Bisglycinat) | 1 | 25 mg | Kapsel | W | täglich | |
| 5 | Magnesium (Bisglycinat) | 2 | 155 mg | Kapsel | W | täglich | 🟡 |
| 6 | Magnesium (L-Threonat) | 1 | 48 mg | Kapsel | W | täglich | 🟡 |
| 7 | Glycin | 2 | 1 g | Kapsel | W | täglich | |
| 8 | MSM | 1 | 1 g | Löffel | W | täglich | 🟢 |

## evening2 — Abend-Stack Teil 2

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | Astragalus-Extrakt | 1 | 600 mg (20:1) | Kapsel | W | alle 4 Tage | |
| 2 | PEA (Palmitoylethanolamid) | 2 | 300 mg | Kapsel | F | täglich | |
| 3 | Pterostilben | 1 | 100 mg | Kapsel | F | täglich | **alterniert mit Trans-Resveratrol** |
| 4 | Trans-Resveratrol | 1 | 500 mg | Kapsel | F | täglich | **alterniert mit Pterostilben** |
| 5 | Liposomales Luteolin | 1 | 250 mg | Kapsel | F | täglich | |
| 6 | Curcumin Phytosome | 1 | 500 mg | Kapsel | F | täglich | 🟡 · Thorne |
| 7 | Spermidin | 1 | 6 mg | Kapsel | W | täglich | |
| 8 | Urolithin A | 1 | 500 mg | Kapsel | F | alle 3 Tage | |
| 9 | Weihrauch Extrakt | 1 | 500 mg (davon 425 mg Boswelliasäure) | Kapsel | F | täglich | |
| 10 | Grüntee-Extrakt | 1 | 700 mg | Kapsel | W | täglich | entkoffeiniert · **alterniert mit Brokkoli-Extrakt** |
| 11 | Brokkoli-Extrakt | 1 | 1000 mg | Kapsel | F | täglich | **alterniert mit Grüntee-Extrakt** |
| 12 | Löwenmähne | 2 | 650 mg | Kapsel | W | täglich | |
| 13 | Gotu Kola Extrakt | 1 | 435 mg | Kapsel | F | täglich | |
| 14 | Bacopa Monnieri Extrakt | 1 | 500 mg | Kapsel | F | täglich | |
| 15 | Ashwagandha KSM-66 | 2 | 600 mg | Kapsel | F | täglich | 🟡 |
| 16 | Omega 3 | 3 | 1 g | Kapsel | F | täglich | 🟡 |
| 17 | NAC | 1 | 800 mg | Kapsel | W | täglich | 🟡 |
| 18 | Glycin | 2 | 1 g | Kapsel | W | täglich | |

## evening3 — Abend-Stack Teil 3

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | Micellar Casein | 1 | 25 g in Wasser | Löffel | W | täglich | 🟢 |

## Mittel, die in mehreren Stacks vorkommen

Diese Zusammenstellung ist der Grund für den **Mittel-Katalog** (F-30) und für „Alle Stacks
zusammen prüfen" (F-13). Ohne stabile Kennung je Mittel wären diese Summen nicht berechenbar.

| Mittel | Stacks | Tagesgesamtmenge |
|---|---|---|
| Magnesium (Bisglycinat) | morning2 (1×155 mg), evening1 (2×155 mg) | **465 mg** |
| Magnesium (L-Threonat) | morning2, evening1 | 96 mg |
| Ashwagandha KSM-66 | morning2 (2×600 mg), evening2 (2×600 mg) | **2400 mg** |
| Curcumin Phytosome | morning2, evening2 | 1000 mg |
| Löwenmähne | morning1 (2×650 mg), evening2 (2×650 mg) | 2600 mg |
| Acetyl-L-Carnitin (ALCAR) | morning1, presport | 1500 mg |
| Vitamin C | morning1 (2×80 mg), presport (1×80 mg), + 80 mg im Eisen-Bisglycinat | 320 mg |
| Kollagen | morning1, presport | 20 g |
| Glycin | evening1 (2×1 g), evening2 (2×1 g) | 4 g |
| MSM | morning2, evening1 | 2 g |
| L-Theanin | morning1, evening1 | 1000 mg |

## Bekannte Gegenspieler im Bestand

Nur als Hinweis für die erste Auswertung — die Bewertung leistet Codex, nicht dieses Dokument:
- **Kaffee und Eisen** stehen beide in `morning1` (Positionen 2 und 16).
- **Zink** (evening1) und **Kupfer** (morning2) sind Gegenspieler an derselben Aufnahme.
- Drei ausdrücklich **alternierende** Paare bzw. Zyklen, die **nie** als Konkurrenz gemeldet
  werden dürfen: Pterostilben ⇄ Trans-Resveratrol · Grüntee-Extrakt ⇄ Brokkoli-Extrakt ·
  Citicolin ↔ Uridin + Phosphatidylserin.
