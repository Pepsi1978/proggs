# FORMAT — verbindlicher Aufbau der Spec-Dateien

Version 1.0.0 (09.08.2026)

Diese Datei ist der Vertrag zwischen den drei Stufen. `spec-schmiede` schreibt danach,
`spec-rueckimport` liest und fortschreibt danach, `design-umsetzer` baut danach.
Wer eine Datei anders aufbaut, macht sie für die nächste Stufe unlesbar.

**Grundregeln für jede Spec-Datei:**

1. Jede Datei beginnt mit `# <Titel> — <Projekt>` und darunter einer Kopfzeile:
   `Stand: <Datum> · Stufe: <v1|v2> · Plattform(en): <…>`
2. Werte stehen **exakt** da, wie sie gemessen wurden. Kein Runden, kein „ca.", kein „ungefähr".
3. Was nicht feststeht, kommt unter **Offene Fragen** — es wird nicht geraten und nicht
   stillschweigend erfunden.
4. Jede Funktion, jeder Bildschirm und jede Bewegung hat eine **stabile Kennung**
   (`F-01`, `B-01`, `M-01`). Die Kennungen bleiben über v1 → v2 → Quellcode gleich.
   Nur so lässt sich am Ende nachweisen, dass nichts verlorengegangen ist.

---

## 00-PROJEKT.md

```
# Projekt — <Name>
Stand: … · Stufe: … · Plattform(en): …

## 1. Zweck in drei Sätzen
## 2. Zielplattform(en)
   Tabelle: Plattform | Zielgerät / Auflösung | Technik-Weg (Compose / WPF / SwiftUI) | Pflicht oder später
## 3. Rahmenbedingungen
   Sprache der Oberfläche · Offline/Online · Konten/Anmeldung · Berechtigungen ·
   externe Dienste · Datenhaltung · Verteilung (Store, Installer, privat)
## 4. Ausdrücklich NICHT enthalten
## 5. Abnahme — wann ist es fertig
   Nummerierte, prüfbare Kriterien (A-01, A-02 …). Jedes muss beobachtbar sein.
## 6. Offene Fragen
```

---

## 01-FUNKTIONS-SPEC.md

Beschreibt **Verhalten**, nicht Aussehen. Die Quelle für das Backend.

```
# Funktions-Spec — <Name>
Stand: … · Stufe: … · Plattform(en): …

## 1. Überblick der Funktionen
   Tabelle: Kennung | Funktion | Bildschirm(e) | Stufe (Kern / später)

## 2. Funktionen im Einzelnen
   Je Funktion ein Abschnitt `### F-01 — <Name>`:
   - **Auslöser** — was der Nutzer tut oder was das Programm auslöst
   - **Ablauf** — Schritt für Schritt, nummeriert
   - **Daten** — was gelesen, was geschrieben wird
   - **Ergebnis** — was danach anders ist, sichtbar und gespeichert
   - **Fehlerfall** — was passiert, wenn es schiefgeht (nur reale Fälle)
   - **Regeln/Grenzen** — Pflichtfelder, Höchstwerte, Berechtigungen

## 3. Datenmodell
   Je Einheit: Felder, Typ, Pflicht/optional, Standardwert, wo gespeichert.

## 4. Zustände und Übergänge
   Nur wo es mehr als an/aus gibt (z. B. Sitzung: bereit → läuft → pausiert → beendet).

## 5. Externe Dienste
   Dienst | wofür | Schlüssel/Anmeldung | Verhalten ohne Netz

## 6. Hintergrund und Lebenszyklus
   Was läuft weiter, wenn das Fenster/die App in den Hintergrund geht oder geschlossen wird.

## 7. Offene Fragen
```

---

## 02-UI-SPEC.md

Beschreibt **Aussehen**. In v1 als Absicht, in v2 als gemessene Wahrheit aus dem Design.

```
# UI-Spec — <Name>
Stand: … · Stufe: … · Plattform(en): …

## 1. Gestalterische Grundhaltung
   Drei bis fünf Sätze. Woran sich jede spätere Entscheidung messen lässt.

## 2. Erscheinungen (Themes)
   Je Erscheinung eine vollständige Farbtabelle: Rolle | Wert (Hex/RGBA) | Verwendung.
   ALLE Erscheinungen, nicht nur die Standard-Erscheinung.

## 3. Typografie
   Schriftfamilie(n) + alle Schnitte. Skala: Rolle | Größe | Gewicht | Zeilenhöhe | Laufweite.

## 4. Maße und Raster
   Grundraster, alle vorkommenden Abstände, feste Breiten/Höhen.

## 5. Formen und Tiefe
   Eckenradien je Bauteil, Ränder, Schatten, Verläufe (mit exakten Stops), Weichzeichner.

## 6. Bildschirme
   Tabelle: Kennung | Bildschirm | Zweck | Startbildschirm? | führt zu (Kennungen)
   Danach je Bildschirm `### B-01 — <Name>`:
   - Aufbau von oben nach unten, in genau der Reihenfolge des Designs
   - Bauteile mit Maßen und Farbrollen
   - Zustände: leer, lädt, Fehler, aktiv, ausgewählt
   - Bedienelemente: was ist antippbar und wohin führt es (Kennung des Ziels oder F-Kennung)

## 7. Ikonografie und Bilder
   Welche Symbole, welcher Satz, welche Größe. Assets mit Pfad.

## 8. Texte
   Alle festen Beschriftungen wörtlich. Platzhalter als solche gekennzeichnet.

## 9. Barrierefreiheit
   Kontrastverhältnisse, Mindest-Tippfläche, Verhalten bei großer Systemschrift.

## 10. Offene Fragen
```

---

## 03-MOTION-SPEC.md

Beschreibt **Bewegung**. Gehört zum Design, steht aber getrennt, weil es getrennt geprüft wird.

```
# Motion-Spec — <Name>
Stand: … · Stufe: … · Plattform(en): …

## 1. Bewegungs-Grundhaltung
   Grundtempo, Charakter (ruhig/straff/verspielt), was sich NIE bewegen darf.

## 2. Kurven und Dauern
   Tabelle: Name | Dauer (ms) | Kurve (cubic-bezier exakt) | wofür
   Diese Namen werden überall sonst nur noch referenziert, nie neu erfunden.

## 3. Bewegungen im Einzelnen
   Je Bewegung ein Abschnitt `### M-01 — <Name>`:
   - **Wo** — Bildschirm/Bauteil (B-Kennung)
   - **Auslöser** — Erscheinen, Antippen, Wechsel, dauerhaft
   - **Was sich ändert** — Eigenschaft, Von-Wert → Zu-Wert (exakt)
   - **Dauer / Kurve / Verzögerung / Wiederholung**
   - **Quelle** (ab v2) — `@keyframes`-Name bzw. Zeile in `design.css`

## 4. Bildschirmwechsel
   Je Übergang: von B-xx nach B-yy, Art (Überblenden/Schieben/Aufziehen), Dauer, Kurve.

## 5. Rückmeldung auf Bedienung
   Druckzustand, Wellen-/Ripple-Effekt, Halten, Wischen, Vibration.

## 6. Dauerbewegung
   Alles, was ohne Zutun läuft (Atmen, Pulsieren, Kreisen) — mit Periodendauer.

## 7. Lade- und Wartezustände
   Was zeigt sich ab wann, wie lange, und wie geht es wieder weg.

## 8. Reduzierte Bewegung
   Was passiert, wenn das System „Bewegung reduzieren" meldet. Pflichtabschnitt.

## 9. Offene Fragen
```

---

## Zusatzteile — nur wenn die App sie braucht

Nicht jede App braucht dieselben Teile. Geht sie in den Store oder benutzen andere Menschen sie,
kommen zwei weitere Dateien dazu; eine App nur für den eigenen Rechner hat beides nicht.
Der Schalter dafür ist die Frage „Für wen ist die App?" in Stufe 1 — dieselbe, die dort die
Prüf-Brillen zuschaltet.

| Datei | Wann | Inhalt |
|-------|------|--------|
| `04-ONBOARDING-SPEC.md` | Andere Menschen starten die App | Erststart-Ablauf, was erklärt wird, was übersprungen werden darf, Berechtigungs-Abfragen mit ihrem Zeitpunkt |
| `05-RECHT-SPEC.md` | Veröffentlichung, personenbezogene Daten oder externe Dienste | Pflichttexte, Einwilligungen, Datenverarbeitung, Löschung, Store-Vorgaben |

**Ihre Bildschirme stehen trotzdem im UI-Spec.** Ein Onboarding-Bildschirm ist ein Bildschirm wie
jeder andere und gehört mit `B-`Kennung in `02-UI-SPEC.md` §6 — sonst baut der Designer ihn nicht.
Die Zusatzdatei beschreibt den *Ablauf* und die *Pflichten*, nicht das Aussehen.

Werft misst diese Teile nicht — es reicht sie unverändert durch, damit sie beim Umsetzer ankommen.

---

## Das Übergabe-ZIP: `Designs/Inbox/<App>-SPEC-v1.zip`

Das Paket, das den Rechner verlässt und in Werft Studio über *Importieren → ZIP- oder
Designdatei auswählen* eingelesen wird. Es liegt **nicht** in `Specs/`, sondern in
`~/proggs/Designs/Inbox/`. Der Dateiname trägt den App-Namen, weil im Inbox-Ordner mehrere
Projekte nebeneinander liegen können — bei einem zweiten Durchlauf `<App>-SPEC-v3.zip`.

Inhalt, flach ohne Unterordner:

| Eintrag | Inhalt |
|---------|--------|
| `SPEC.md` | Die **Zusammenstellung aller drei Specs** in einem Dokument (Aufbau unten). Die Datei, die der Designer liest |
| `00-PROJEKT.md`, `01-FUNKTIONS-SPEC.md`, `02-UI-SPEC.md`, `03-MOTION-SPEC.md` | Dieselben Einzeldateien wie in `Specs/<App>/v1/`, unverändert |
| `04-ONBOARDING-SPEC.md`, `05-RECHT-SPEC.md` | Nur wenn die App sie braucht (siehe oben) |
| `LIESMICH.md` | Zielplattform, Auftrag an den Designer, Regeln für den Rücklauf (Kennungen erhalten, `NEU`/`ENTFALLEN` kennzeichnen, jedes neue Bedienelement bekommt eine Aufgabe) |

`SPEC.md` muss **allein verständlich** sein: der Designer sieht sonst keine Datei.

```
# <App> — Spec v1
Stand: … · Plattform: … · Zielgerät: … · Sprache der Oberfläche: …
Herkunft: Specs/<App>/v1/ · Erzeugt von: spec-schmiede

## 0. Was dieses Programm ist — in drei Sätzen
## 1. Auftrag an den Designer
   Wörtlich: jeden Bildschirm aus Teil B §6 in jeder Erscheinung aufbauen.
   Zusatz: Wer ein Bedienelement ergänzt, das in Teil A keine Aufgabe hat,
   beschreibt kurz, was es tun soll — sonst muss beim Rückimport nachgefragt werden.

## Teil A — Funktions-Spec      (vollständiger Inhalt von 01-FUNKTIONS-SPEC.md)
## Teil B — UI-Spec             (vollständiger Inhalt von 02-UI-SPEC.md)
## Teil C — Motion-Spec         (vollständiger Inhalt von 03-MOTION-SPEC.md)
## Teil D — Rahmen und Abnahme  (§2–5 aus 00-PROJEKT.md)

## Z. Was ausdrücklich nicht ins Design gehört
```

Die Teile A bis C werden **wörtlich** aus den Einzeldateien übernommen, nicht gekürzt und
nicht umformuliert — sonst weichen Übergabedatei und `Specs/<App>/v1/` voneinander ab und
der Rückimport vergleicht gegen den falschen Stand. Die Kennungen (`F-`, `B-`, `M-`, `A-`)
bleiben dabei erhalten; sie sind der Faden durch die ganze Pipeline.

---

## Das Rücklauf-ZIP: `Designs/Outbox/<App>-SPEC-v2.zip`

Was Werft Studio bei *Projekt als ZIP herunterladen* zurückschreibt — nachdem beim
Herunterladen ausdrücklich das **Zielsystem** gewählt wurde (Android, Windows oder macOS).
Es enthält das gemessene Design (`WERFT-DESIGN/`) **und** die fortgeschriebenen Specs — `SPEC.md` im
gleichen Aufbau wie die Übergabedatei (Teil A, B, C, D), damit sich beide Fassungen
Abschnitt für Abschnitt vergleichen lassen, sowie die drei Einzeldateien, bereits für die
im Spec genannte Zielplattform übersetzt (Bewegungen als Compose / WPF / SwiftUI statt als
CSS). Zusätzlich erwartet `spec-rueckimport` darin:

- **erhaltene Kennungen.** Ein Bildschirm, der in v1 `B-03` hieß, heißt auch im Rücklauf
  `B-03`. Nur wirklich Neues bekommt eine neue Nummer, fortlaufend hinter der höchsten
  bisherigen.
- **markiertes Neues.** Jeder Bildschirm, jedes Bedienelement und jede Bewegung, die es in
  v1 nicht gab, ist als `NEU` gekennzeichnet — mit einem Satz, was es tun soll.
- **markiertes Entfallenes.** Was der Designer weggelassen hat, steht als `ENTFALLEN` mit
  Begründung da, statt einfach zu fehlen.

Fehlen diese Markierungen, ist das **kein Abbruch**: `spec-rueckimport` ermittelt die
Unterschiede dann durch Vergleich gegen `Specs/<App>/v1/`.

**Gefragt wird nur zu Neuem.** Ein Bedienelement, das im Design dazugekommen ist und noch
keine Aufgabe hat, wird vorgelegt — sonst entstünde beim Bauen ein toter Knopf. Über
**Streichungen wird nicht gefragt**: was der Designer weggelassen hat, fällt aus v2 heraus
und wird nur in `AENDERUNGEN.md` festgehalten.

Liegt neben der Rücklaufdatei ein ausgepacktes Werft-Paket
(`Designs/Outbox/<App>/WERFT-DESIGN/`), ist dieses für alle **gemessenen Zahlenwerte** die
verbindliche Quelle — insbesondere `design-tokens.json` und `bildschirme/design.css`.
Die Rücklaufdatei ist dann die Quelle für **Absicht, neue Funktionen und Begründungen**.

---

## AENDERUNGEN.md (nur v2)

```
# Änderungen durch den Designer — <Name>
Stand: … · Design-Paket: <Pfad> · Verglichen mit: Specs/<App>/v1/

## 1. Bildschirme
   Tabelle: Kennung | v1 | im Design | Bewertung (unverändert / geändert / NEU / entfallen)
## 2. Neue Bedienelemente ohne Funktion in v1
   Tabelle: Bildschirm | Element | Was es laut Rückfrage tun soll | neue F-Kennung
## 3. Geänderte Gestaltung
   Was sich gegenüber der v1-Absicht bei Farbe/Schrift/Maß/Form verschoben hat.
## 4. Geänderte Bewegung
## 5. Entfallenes
   Was in v1 stand und im Design fehlt — mit Entscheidung: bleibt raus / wird nachgebaut.
```

---

## BAU-AUFTRAG.md (nur v2)

Die Einstiegsdatei für Stufe 3. Kurz — sie verweist, sie wiederholt nicht.

```
# Bau-Auftrag — <Name>
Stand: … · Stufe: v2

## 1. Was gebaut wird
   Ein Absatz.
## 2. Zielplattform(en) und Technik-Weg
   Tabelle aus 00-PROJEKT §2.
## 3. Verbindliche Quellen
   Tabelle: Datei | wofür verbindlich
   — 01-FUNKTIONS-SPEC.md  → Verhalten, Daten, Regeln
   — 02-UI-SPEC.md         → jede Farbe, jedes Maß, jeder Bildschirm
   — 03-MOTION-SPEC.md     → jede Bewegung
   — <Design-Ordner>/WERFT-DESIGN/  → das gebaute Design als Augenschein
## 4. Abhakliste
   Alle Kennungen: B-01…, F-01…, M-01…, A-01… als Liste.
   Fertig ist der Bau erst, wenn jede Kennung im Quellcode nachweisbar ist.
## 5. Offene Fragen, die vor dem Bau geklärt sein müssen
```
