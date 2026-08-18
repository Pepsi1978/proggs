# Auftrag an den Designer — Gedankenspeicher

Stand: 18.08.2026 · Stufe: v1 · **Vorgesehene Zielplattform: Android**

Beim Herunterladen fragt Werft Studio die Zielplattform noch einmal ausdrücklich ab und
übersetzt die Specs in die Sprache des dort gewählten Systems. **Weicht die Wahl beim
Herunterladen von der Angabe hier ab, gilt die Wahl beim Herunterladen.**

## Was gebaut werden soll

**Jeden Bildschirm in jeder Erscheinung.** Acht Bildschirme (B-01 bis B-08), vier
Erscheinungen (Hell, Dunkel, Gold-Hell, Gold-Dunkel) — das sind 32 Ansichten. Die vier
Erscheinungen sind **gleichrangig**: keine ist eine abgedunkelte Kopie einer anderen,
jede wird vollständig gestaltet.

Zielgerät ist ein Galaxy Z Fold 8: zugeklappt 1248 × 1972 px @ 420 dpi (297 × 469 dp),
aufgeklappt 1848 × 2448 px @ 420 dpi (440 × 583 dp). Beide Displays sind zu schmal für eine
dauerhaft danebenstehende Seitenleiste — die Sitzungsliste ist eine Schublade.

Auch die Zustände gehören dazu, nicht nur der Normalfall: leer, lädt, Fehler, Aufnahme
läuft, Auswertung läuft, Vorlesen läuft. Sie stehen je Bildschirm im UI-Spec unter
*Zustände*.

## Die Regel für Ergänzungen

**Jedes neue Bedienelement braucht eine Aufgabe.** Fügst du einen Knopf, einen Schalter oder
eine Zeile hinzu, schreib in einem Satz dazu, was er tun soll. Ohne diesen Satz entsteht beim
Bauen ein toter Knopf — ein Element, das da ist und nichts kann.

## Die Regel für Kennungen

- `B-` (Bildschirme), `F-` (Funktionen), `M-` (Bewegungen) und `A-` (Abnahme) **bleiben
  erhalten**. Sie sind der Faden von hier bis in den fertigen Quellcode.
- Neues bekommt die nächste freie Nummer und die Marke **`NEU`**.
- Weggelassenes wird als **`ENTFALLEN`** gekennzeichnet, nicht stillschweigend gelöscht.

## Was nicht dazuerfunden werden darf

Drei Dinge sind ausdrücklich ausgeschlossen (siehe `00-PROJEKT.md` §4): kein freies Chatten
mit der KI, kein automatisches Auswerten, keine Ordner über den Sitzungen.

Drei weitere sind **offen und deshalb nicht zu bauen** (siehe `00-PROJEKT.md` §6): keine
Bilder oder Anhänge an Notizen, keine Tags, keine Benachrichtigungen. Bitte auch keine
Flächen dafür vorsehen.

## Was am wichtigsten ist

Der **KI-Knopf** in der Fußleiste von B-01 ist der Kern der App. Alles andere dient nur
dazu, dass beim Druck auf diesen Knopf ein guter Notiz-Kontext bereitliegt. Er darf im
Entwurf nicht untergehen.

## Der Rücklauf

Nach dem Gestalten: **Projekt als ZIP herunterladen**, dort **Android** als Zielsystem
wählen und **In die Outbox legen**. Die Datei landet als

```
~/proggs/Designs/Outbox/Gedankenspeicher-SPEC-v2.zip
```

Danach macht Stufe 2 (`spec-rueckimport`) weiter.

## Der Inhalt dieses Pakets

| Datei | Inhalt |
|-------|--------|
| `SPEC.md` | Alle drei Specs in einem Dokument — **das ist die Datei zum Lesen** |
| `00-PROJEKT.md` | Name, Plattform, Zweck, Rahmen, Abnahme |
| `01-FUNKTIONS-SPEC.md` | Was die App kann (18 Funktionen) |
| `02-UI-SPEC.md` | Wie sie aussieht (4 Erscheinungen, 8 Bildschirme) |
| `03-MOTION-SPEC.md` | Wie sie sich bewegt (12 Bewegungen) |
| `LIESMICH.md` | Diese Datei |

`04-ONBOARDING-SPEC.md` und `05-RECHT-SPEC.md` entfallen: die App ist privat, sie geht nicht
in den Store und niemand außer dem Besitzer benutzt sie.
