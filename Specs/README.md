# Specs — die Programm-Pipeline

Version 1.1.0 (09.08.2026)

Hier liegen die Spezifikationen, aus denen Programme gebaut werden. Ein Projekt durchläuft
drei Stufen. Zwischen Stufe 1 und Stufe 2 liegt der einzige Handgriff von Frank: das Design
im Designer bauen.

```
  ┌─ Stufe 1 ───────────┐   ┌─ Hand ────────┐   ┌─ Stufe 2 ────────┐   ┌─ Stufe 3 ───────┐
  │ spec-schmiede       │   │ Werft Studio  │   │ spec-rueckimport │   │ design-umsetzer │
  │ Grilling → Spec v1  │──▶│ Design bauen  │──▶│ Design → Spec v2 │──▶│ Programm bauen  │
  └─────────────────────┘   └───────────────┘   └──────────────────┘   └─────────────────┘
      Specs/<App>/v1/                              Specs/<App>/v2/         <App>/ (Quellcode)
            │                      │                      ▲
            └─▶ Designs/Inbox/ ────┘   Designs/Outbox/ ───┘
                <App>-SPEC-v1.md        <App>-SPEC-v2.md + <App>/WERFT-DESIGN/
```

## Die beiden Briefkästen

`~/proggs/Designs/` enthält nur noch zwei Ordner. Sie sind die Übergabestellen zwischen
Rechner und Designer:

| Ordner | Richtung | Inhalt |
|--------|----------|--------|
| `Designs/Inbox/` | **hin** zum Designer | `<App>-SPEC-v1.zip` — alle drei Specs, allein verständlich. In Werft Studio über *Importieren → ZIP- oder Designdatei auswählen* |
| `Designs/Outbox/` | **zurück** vom Designer | `<App>-SPEC-v2.zip` — von Werft über *Projekt als ZIP herunterladen* abgelegt |

**Es wird immer als ZIP übergeben — in beide Richtungen.** Alles, was aus dem Designer
zurückkommt, landet in `Outbox/`; nichts liegt mehr direkt in `Designs/`.

## Der eine Einstieg

Wer die ganze Kette will, ruft den Skill **`neue-applikation`** auf ("starte neue
Applikation"). Er fährt Stufe 1 bis 3, wartet dazwischen auf den einen Handgriff im
Designer, baut am Ende auf der Zielplattform und installiert — und committet und pusht nach
jedem Schritt. Die drei Stufen lassen sich aber auch einzeln aufrufen.

## Die drei Stufen

| Stufe | Skill | Eingang | Ausgang |
|-------|-------|---------|---------|
| 1 | `spec-schmiede` | Ein Gespräch (Grilling) | `Specs/<App>/v1/` **und** `Designs/Inbox/<App>-SPEC-v1.md` |
| — | *(Hand)* | Die Inbox-Datei in Werft Studio | Rücklauf nach `Designs/Outbox/` |
| 2 | `spec-rueckimport` | `Designs/Outbox/…` + `Specs/<App>/v1/` | `Specs/<App>/v2/` inkl. `BAU-AUFTRAG.md` |
| 3 | `design-umsetzer` | `Specs/<App>/v2/BAU-AUFTRAG.md` | Lauffähiges Programm |

## Ordner-Konvention

```
Designs/
  Inbox/
    <App>-SPEC-v1.zip           ← Stufe 1 legt es hier ab, Frank importiert es im Designer
      SPEC.md                     alle drei Specs in einem Dokument
      00-PROJEKT.md … 03-MOTION-SPEC.md
      LIESMICH.md                 Auftrag an den Designer + Regeln für den Rücklauf
  Outbox/
    <App>-SPEC-v2.zip           ← der Designer legt es hier ab
    <App>/                      ← Stufe 2 packt es hierhin aus
      01-FUNKTIONS-SPEC.md …      vom Designer fortgeschrieben, für die Zielplattform übersetzt
      WERFT-DESIGN/               das gemessene Design

Specs/
  README.md                     ← diese Datei
  FORMAT.md                     ← verbindlicher Aufbau aller Spec-Dateien
  <App>/
    v1/
      00-PROJEKT.md             Name, Plattform, Zweck, Rahmenbedingungen, Abnahme
      01-FUNKTIONS-SPEC.md      Was das Programm kann (Backend/Verhalten)
      02-UI-SPEC.md             Wie es aussieht (Farben, Schrift, Maße, Bildschirme)
      03-MOTION-SPEC.md         Wie es sich bewegt (Übergänge, Rückmeldungen, Dauerbewegung)
    v2/
      00-PROJEKT.md             unverändert aus v1, sofern nichts umgeworfen wurde
      01-FUNKTIONS-SPEC.md      v1 + die Funktionen, die im Design dazugekommen sind
      02-UI-SPEC.md             aus dem Rücklauf — gemessene Werte, nichts geschätzt
      03-MOTION-SPEC.md         aus dem Rücklauf — gemessene Werte, nichts geschätzt
      AENDERUNGEN.md            Was der Designer gegenüber v1 geändert/ergänzt/entfernt hat
      BAU-AUFTRAG.md            Einstiegsdatei für Stufe 3
```

Der App-Name ist überall derselbe — in `Specs/`, in den Dateinamen in `Inbox/` und `Outbox/`
und im Quellcode-Ordner. Sonst findet keine Stufe die nächste. Er wird in Stufe 1 als
allererstes festgelegt.

## Warum v1 stehen bleibt

`v1` wird von Stufe 2 **nie überschrieben**. Nur so lässt sich nachweisen, was der Designer
verändert hat — genau das ist der Inhalt von `AENDERUNGEN.md`. Ein zweiter Designer-Durchlauf
erzeugt `v3` und `Designs/Inbox/<App>-SPEC-v3.md`, nicht ein zweites `v2`.

## Welche Spec gewinnt bei Widerspruch

| Thema | Verbindlich ab v2 |
|-------|-------------------|
| Farben, Maße, Schrift, Formen, Bildschirm-Aufbau | `02-UI-SPEC.md` (aus dem Design gemessen) |
| Dauer, Kurve, Wiederholung jeder Bewegung | `03-MOTION-SPEC.md` (aus dem Design gemessen) |
| Was beim Antippen tatsächlich passiert, Daten, Regeln | `01-FUNKTIONS-SPEC.md` |
| App-Name, Zielplattform, Sprache, Abnahmekriterien | `00-PROJEKT.md` |

Das Design gewinnt beim **Aussehen und Bewegen**. Das Funktions-Spec gewinnt beim **Verhalten**.
Widersprechen sie sich im selben Punkt, wird gefragt — nicht geraten.

Liegt im Rücklauf sowohl `<App>-SPEC-v2.md` als auch ein `WERFT-DESIGN/`-Paket, gilt für
**Zahlenwerte** das Paket (dort sind sie maschinell gemessen) und für **Absicht und neue
Funktionen** die Spec-Datei.
