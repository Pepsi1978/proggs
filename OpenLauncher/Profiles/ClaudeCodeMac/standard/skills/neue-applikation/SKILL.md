---
name: neue-applikation
description: >-
  Die Klammer um die gesamte Programm-Pipeline — von der ersten Frage bis zum installierten
  Programm. Faehrt nacheinander: Grilling (spec-schmiede) → Uebergabe-ZIP nach
  Designs/Inbox/ → Warten auf den Designer (eine Bestaetigung) → Rueckimport aus
  Designs/Outbox/ (spec-rueckimport) → Bau (design-umsetzer) → Build, Installation und
  Deployment auf der Zielplattform (Windows, macOS oder das angeschlossene Android-Geraet).
  Committet und pusht nach jedem Zwischenschritt. Nutze diesen Skill IMMER wenn der Benutzer
  sagt "neue Applikation", "starte neue Applikation", "neue-applikation",
  "/neue-applikation", "neue App bauen", "starte das Neue-Applikation-Plugin",
  "komplette Pipeline starten", "Pipeline von vorn", "bau mir ein neues Programm",
  "ich moechte ein neues Programm von Anfang bis Ende", "ganze Kette starten",
  "von der Idee bis zur fertigen App". Fuer einzelne Stufen stattdessen direkt
  spec-schmiede (Stufe 1), spec-rueckimport (Stufe 2) oder design-umsetzer (Stufe 3) nutzen.
---

# Neue Applikation — die ganze Kette

Ein Aufruf, ein Ergebnis: aus einer Idee wird ein Programm, das auf dem Zielsystem laeuft.
Du fuehrst hier **keine** eigene Arbeit aus, die es schon als Stufe gibt — du rufst die
Stufen auf, sorgst fuer die Uebergaenge und dafuer, dass unterwegs nichts verlorengeht.

Der Gesamtablauf steht in `~/proggs/Specs/README.md`, das Dateiformat in
`~/proggs/Specs/FORMAT.md`.

## Die Kette

| Schritt | Wer | Ergebnis |
|---------|-----|----------|
| 1 | `spec-schmiede` | `Specs/<App>/v1/` + `Designs/Inbox/<App>-SPEC-v1.zip` |
| 2 | **Frank** (Handgriff) | In Werft Studio importieren, Design bauen, ZIP nach `Designs/Outbox/` |
| 3 | dieser Skill | Warten mit **einer** Bestaetigung |
| 4 | `spec-rueckimport` | `Specs/<App>/v2/` inkl. `BAU-AUFTRAG.md` |
| 5 | `design-umsetzer` | Der Quellcode, gebaut und per Screenshot gegen das Design geprueft |
| 6 | dieser Skill | Build, Installation, Deployment auf der Zielplattform |

Melde zu Beginn: "Neue Applikation gestartet. Ich fuehre dich durch die ganze Kette —
erst das Grilling, dann dein Handgriff im Designer, danach baue ich alles fertig und
installiere es. Committet und gepusht wird nach jedem Schritt."

---

## Schritt 1 — Grilling

`spec-schmiede` aufrufen. Der Skill nimmt auf, was der Benutzer erzaehlt hat, spiegelt sein
Verstaendnis zurueck und hinterfragt es danach Durchgang fuer Durchgang — mit Pruef-Agenten
je Bereich und einem Operator, der die Querverbindungen im Blick behaelt. Nach jedem
Durchgang fragt er, ob noch einer folgen soll.

**Du greifst hier nicht ein.** Keine Frage abkuerzen, keine Antwort vorwegnehmen, keinen
Durchgang ueberspringen. Der Skill endet erst, wenn nichts mehr unentschieden ist.

Danach: **committen und pushen** (siehe *Sichern nach jedem Schritt*).

---

## Schritt 2 und 3 — Handgriff und Warten

Nach dem Schreiben des Uebergabe-ZIP sagst du den Handgriff woertlich:

> In Werft Studio oben auf **Importieren**. Ist der Designs-Ordner freigegeben, steht
> `<App>-SPEC-v1.zip` direkt unter „Aus der Inbox" — sonst ueber **ZIP- oder Designdatei
> auswaehlen**. Werft **erzeugt daraus von selbst das vollstaendige Design**; du gestaltest
> es um, loeschst und ergaenzt. Zum Aendern einzelner Stellen das **Kommentarwerkzeug**
> (Taste `c`): Bereich markieren, beschreiben was anders soll, absenden — und **sofort den
> naechsten** markieren. Bis zu **fuenf Auftraege arbeiten gleichzeitig**; warten musst du auf
> keinen. Unter **Kommentare** steht je Auftrag, was gerade geschieht. Danach **Projekt als ZIP
> herunterladen**, dort **das Zielsystem waehlen** (Android, Windows oder macOS) und
> **In die Outbox legen**.

Dann legst du **eine** Bestaetigung vor (`AskUserQuestion`, erste Option vorausgewaehlt):

> **Ist das fertige Spec vom Designer in der Outbox?**
> - **Ja, weiter** — es geht sofort weiter
> - Noch nicht — nochmal nachsehen
> - Abbrechen — spaeter weitermachen

Bei "Noch nicht" den Outbox-Inhalt zeigen und dieselbe Bestaetigung erneut vorlegen.
**Nicht** von selbst in einer Schleife nachsehen und **nicht** ohne Frage warten.

Bei "Abbrechen" den Wiedereinstieg nennen: "Sag spaeter `Rueckimport <App>`."

---

## Schritt 4 — Rueckimport

`spec-rueckimport` aufrufen. Er packt das ZIP aus, gleicht gegen v1 ab, fragt zu allem
Neuen nach und schreibt `Specs/<App>/v2/`.

Ist der Dateiname in der Outbox nicht eindeutig, fragt er — das ist richtig so, greif
nicht ein.

Danach: **committen und pushen**.

---

## Schritt 5 — Bauen

`design-umsetzer` aufrufen. Er laeuft in Betriebsart P (Spec-Paket) und baut auf der im
Spec festgelegten Plattform. Fertig ist er erst, wenn alle drei Nachweise stehen:
Aussehen nach UI-Spec, Bewegung nach Motion-Spec, Verhalten nach Funktions-Spec — und
**kein toter Knopf**.

Danach: **committen und pushen**.

---

## Schritt 6 — Installieren und ausliefern

Erst wenn der Bau sauber durchlaeuft. Der Weg haengt an der Zielplattform aus
`Specs/<App>/v2/00-PROJEKT.md` §2:

| Plattform | Bauen | Aufs System bringen |
|-----------|-------|---------------------|
| **Android** | `gradlew assembleDebug` (bzw. der im Projekt uebliche Task) | `adb devices` pruefen, dann `adb install -r <apk>` auf das angeschlossene Geraet |
| **Windows** | `dotnet build` / `dotnet publish` bzw. das `publish.ps1` des Projekts | Die veroeffentlichte Fassung an ihren Zielort legen und einmal starten |
| **macOS** | `swift build` bzw. das `build.sh` des Projekts | Die App an ihren Zielort legen und einmal starten |

**Vor der Installation die sichtbare Version bumpen**, wenn das Projekt das vorsieht —
ohne Bump wirkt eine geglueckte Installation wie eine fehlgeschlagene.

Ist kein Geraet angeschlossen oder die Zielplattform nicht die, auf der du gerade laeufst,
baust du trotzdem fertig, meldest das klar und nennst den einen Befehl, der noch fehlt.
Du taeuschst keine erfolgreiche Installation vor.

Danach: **committen und pushen**.

---

## Ein Handgriff, dann laeuft es durch

Die Kette hat **genau einen** Punkt, an dem ein Mensch arbeitet: das Bearbeiten des Entwurfs
in Werft Studio. Davor wird gefragt (Grilling), danach wird **nicht mehr gefragt**.

Sobald der Ruecklauf in `Designs/Outbox/` liegt und der Benutzer das bestaetigt hat, laeuft
ohne weitere Rueckfrage durch:

```
Ruecklauf da
  → messe-design.ps1            (jeder Bildschirm, jede Erscheinung, vollstaendig)
  → Specs/<App>/v2/ schreiben   (Messung + lesbares Spec + Bau-Auftrag)
  → bauen                       (Bildschirm fuer Bildschirm aus der Messung)
  → Abnahme je Bildschirm       (Bild gegen Bild, beide Erscheinungen)
  → Build, Version, Installation
  → Bericht
```

**Angehalten wird nur bei echten Hindernissen**, und dann mit klarer Ansage:
ein neu hinzugekommenes Bedienelement, dessen Zweck das Funktions-Spec nicht hergibt ·
ein Widerspruch, den die Messung nicht aufloest · fehlender Zugang · Git-Fehler.
Gestaltungsfragen sind **kein** Hindernis: die Antwort steht in der Messung.

---

## Das Abnahme-Tor — wann die Kette fertig ist

Die Kette gilt **erst** als durchlaufen, wenn alle vier Punkte stehen. Kein Punkt ist
verhandelbar, und keiner darf durch einen anderen ersetzt werden:

1. **Die Messung liegt vor.** `Specs/<App>/v2/messung/<erscheinung>/<bildschirm>.json` fuer
   **jeden** Bildschirm in **jeder** Erscheinung. Fehlt einer, ist Stufe 2 unvollstaendig
   gelaufen — nachholen, nicht weiterbauen.
2. **Jeder Bildschirm ist einzeln abgenommen.** Screenshot der gebauten App neben dem Bild
   aus `Specs/<App>/v2/bilder/…`, in beiden Erscheinungen. Die Abnahme geschieht **je
   Bildschirm, bevor der naechste gebaut wird** — nicht gesammelt am Ende.
3. **Die Bewegungen laufen.** Jede Kennung aus dem Motion-Spec ist im Code wiederzufinden,
   mit der gemessenen Dauer und der gemessenen `cubic-bezier`. Auch die Zustaende, die erst
   beim Druecken oder Aufnehmen entstehen (`zustaende` in der Messung), und das Verhalten
   bei reduzierter Bewegung.
4. **Die App laeuft auf dem Geraet.** Gebaut, installiert, gestartet, bedient.

**Ein gruener Build ist kein Punkt dieser Liste.** Er beweist, dass der Code uebersetzt —
ueber das Aussehen sagt er nichts.

Solange ein Punkt offen ist, wird das **nicht** als „fertig" gemeldet, sondern als
„Punkt N offen, weil …". Eine geschoente Meldung kostet den naechsten Durchlauf.

---

## Sichern nach jedem Schritt

Nach **jedem** der Schritte 1, 4, 5 und 6 wird gesichert — nicht erst am Ende:

1. `git status` ansehen. Nur die Dateien aufnehmen, die zum Schritt gehoeren.
2. Committen mit einer Nachricht, die den Schritt benennt, z. B.
   `<App>: Spec v1 aus dem Grilling`, `<App>: Spec v2 aus dem Design`,
   `<App>: Bau nach Spec v2`, `<App>: gebaut und installiert`.
3. Pushen.

Bei einem Git-Fehler **sofort anhalten und melden** — nicht selbst reparieren.

---

## Was NIEMALS passieren darf

- ❌ Eine Stufe ueberspringen oder ihre Arbeit selbst erledigen, statt sie aufzurufen.
- ❌ Das Grilling abkuerzen, weil "das meiste schon klar" scheint.
- ❌ Von selbst in einer Schleife nachsehen, ob die Outbox-Datei schon da ist —
  es wird **einmal** gefragt und auf die Antwort gewartet.
- ❌ Bei unklarem Dateinamen in der Outbox raten oder die neueste Datei nehmen.
- ❌ Die Zielplattform wechseln, weil eine andere gerade naeher liegt.
- ❌ Mehrere Zielsysteme in einem Lauf bauen. Soll dieselbe App auf Android UND Windows
  laufen, wird das Design **zweimal** heruntergeladen — je einmal je System — und die Kette
  laeuft je System einmal durch, in einen eigenen Quellcode-Ordner.
- ❌ Installieren, bevor der Bau sauber durchlaeuft.
- ❌ Eine Installation als geglueckt melden, die nicht stattgefunden hat.
- ❌ Das Sichern auf das Ende verschieben — nach jedem Schritt wird committet und gepusht.
- ❌ Einen Git-Fehler selbst zu beheben versuchen.
