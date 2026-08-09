---
name: spec-schmiede
description: >-
  Stufe 1 der Programm-Pipeline. Befragt den Benutzer im Grilling-Verfahren (eine Frage nach
  der anderen, jede mit Empfehlung) vollstaendig zu einem neuen Programm — zuerst der Name
  der App, dann die Zielplattform (Windows / macOS / Android), dann saemtliche Funktionen bis
  nichts mehr offen ist, dann das Design, dann Bewegung und Animation — und schreibt daraus
  das Erst-Spec-Paket nach Specs/<App>/v1/ (00-PROJEKT.md, 01-FUNKTIONS-SPEC.md,
  02-UI-SPEC.md, 03-MOTION-SPEC.md) sowie das Uebergabe-ZIP
  Designs/Inbox/<App>-SPEC-v1.zip, das alle drei Specs enthaelt und anschliessend in Werft
  Studio ueber Importieren eingelesen wird. Wartet danach mit einer einzelnen Bestaetigung,
  bis der Designer den Ruecklauf nach Designs/Outbox/ gelegt hat, und startet dann von
  selbst Stufe 2 und Stufe 3. Nutze diesen Skill IMMER wenn der Benutzer sagt
  "neues Programm", "neue App planen", "Spec bauen", "Spec-Schmiede", "spec-schmiede",
  "/spec-schmiede", "Erst-Spec erstellen", "Pipeline starten", "grill mich zu meinem Programm",
  "ich moechte ein Programm beschreiben", "Programm spezifizieren", "frag mich zu meiner App aus",
  "Designer-Brief erstellen", "Design-Brief bauen", "Spec fuer den Designer", "Stufe 1",
  "ich will ein Tool bauen lassen", "beschreib mit mir zusammen ein Programm". Auch wenn der
  Benutzer nur sagt "ich haette gern ein Programm das ..." und ersichtlich eine vollstaendige
  Spezifikation gemeint ist. NICHT nutzen, wenn bereits ein Design vorliegt und nur noch
  zurueckgelesen werden soll (dann: spec-rueckimport) oder wenn gebaut werden soll
  (dann: design-umsetzer).
---

# Spec-Schmiede — Stufe 1 der Programm-Pipeline

Du befragst den Benutzer so lange und so genau, bis ein Programm vollstaendig beschrieben
ist, und schreibst daraus das Erst-Spec-Paket. Du baust hier **keinen Code** und **kein
Design** — du erzeugst Text, aus dem beides entstehen kann.

Der Gesamtablauf steht in **`~/proggs/Specs/README.md`**, der verbindliche Aufbau jeder
erzeugten Datei in **`~/proggs/Specs/FORMAT.md`**. Beide liest du zu Beginn. Weicht dieser
Skill von `FORMAT.md` ab, gilt `FORMAT.md`.

## Das Verfahren: Grilling

- **Eine Frage nach der anderen.** Mehrere Fragen auf einmal sind verwirrend und werden
  halb beantwortet. Warte jede Antwort ab, bevor die naechste Frage kommt.
- **Zu jeder Frage deine Empfehlung.** Nicht nur fragen, sondern sagen, was du fuer richtig
  haeltst und warum. Der Benutzer soll nicken oder widersprechen koennen, nicht dichten muessen.
- **Fakten schlaegst du selbst nach.** Alles, was im Repo, im Dateisystem oder im Netz steht,
  wird nicht gefragt: vorhandene Apps, uebliche Gradle-/Compose-Versionen, wie andere Projekte
  im Repo aufgebaut sind, welche Bildschirmgroesse ein genanntes Geraet hat. Gefragt werden
  nur **Entscheidungen**, die dem Benutzer gehoeren.
- **Nachhaken statt Luecken lassen.** Eine vage Antwort ist keine Antwort. "Irgendwie eine
  Liste" wird zu "Was steht in einer Zeile? Was passiert beim Antippen? Was beim langen
  Druecken? Was, wenn die Liste leer ist?"
- **Nichts stillschweigend annehmen.** Was am Ende nicht geklaert ist, kommt woertlich unter
  *Offene Fragen* in die jeweilige Datei — nicht in eine erfundene Festlegung.

---

## Ablauf

0. Vorbereitung
1. Block A — Plattform und Rahmen
2. Block B — Zweck
3. Block C — Funktionen (der laengste Block)
4. Block D — Bildschirme und Navigation
5. Block E — Design
6. Block F — Bewegung und Animation
7. Block G — Abnahme
8. Spec-Paket schreiben und vorstellen

Melde zu Beginn: "Spec-Schmiede gestartet. Ich frage dich jetzt in sieben Bloecken durch —
eine Frage nach der anderen, jede mit meiner Empfehlung. Am Ende liegt dein Spec-Paket
unter `Specs/<App>/v1/` und die Uebergabedatei fuer den Designer in `Designs/Inbox/`."

---

## Phase 0 — Vorbereitung

1. `Specs/README.md` und `Specs/FORMAT.md` lesen.
2. Aktuelles Datum ermitteln (nicht schaetzen) — es kommt in jede Kopfzeile.
3. Pruefen, dass `~/proggs/Designs/Inbox/` existiert. Fehlt der Ordner, anlegen.

Der App-Name ist die **allererste Frage** im Grilling (Block A, Frage 1) — nicht etwas,
das du im Stillen aus dem Aufruf ableitest.

---

## Block A — Name, Plattform und Rahmen

**Das kommt zuerst, weil es alles Weitere faerbt.** Der Name bestimmt jeden Dateinamen und
Ordner der Pipeline; die Plattform bestimmt, wie alles Weitere beschrieben wird — ein
Telefon-Programm wird anders beschrieben als ein Fenster-Programm.

Fragen, eine nach der anderen:

1. **Wie soll die App heissen?** Das ist die erste Frage ueberhaupt.
   - Hast du aus dem Aufruf schon eine Idee, schlage sie vor — lass sie aber ausdruecklich
     bestaetigen oder ersetzen.
   - Der Name wird zum **Ordnernamen** (`Specs/<App>/`, spaeter der Quellcode-Ordner) und
     zum **Dateinamen** (`Designs/Inbox/<App>-SPEC-v1.md`). Taugt der Wunschname dafuer
     nicht (Sonderzeichen, Schraegstriche), schlage eine bereinigte Kurzform als
     Ordner-/Dateinamen vor und halte beide fest: den Anzeigenamen und den Kurznamen.
   - Danach **sofort pruefen**: `ls ~/proggs/Specs/<App>/` und
     `ls ~/proggs/Designs/Inbox/<App>-*`. Gibt es das schon, melden und fragen, ob ergaenzt
     oder neu begonnen wird. **Niemals** ein vorhandenes `v1` stillschweigend ueberschreiben.
2. **Fuer welche Plattform?** Windows, macOS, Android — auch mehrere. Bei mehreren:
   welche ist die fuehrende, an der sich die andere ausrichtet?
3. **Zielgeraet und Groesse.** Telefon (welches Modell / welche Aufloesung), Tablet,
   Fenster auf dem Rechner (Standardgroesse, aenderbar?), Vollbild.
   Die Aufloesung schlaegst du selbst nach, wenn ein Modell genannt wird.
4. **Sprache der Oberflaeche.** Eine oder mehrere. Bei mehreren: welche ist die Quelle.
5. **Ohne Netz benutzbar?** Vollstaendig, teilweise, gar nicht.
6. **Anmeldung/Konto noetig?** Wenn ja: wofuer genau.
7. **Externe Dienste.** KI-Modelle, Karten, Zahlungen, Cloud-Ablage — und wo die
   Zugangsschluessel herkommen.
8. **Wo liegen die Daten?** Nur auf dem Geraet, in der Cloud, beides.
9. **Wie kommt es zum Benutzer?** Store, Installer, privat aufs eigene Geraet.

Sobald die Plattform feststeht, sieh **selbst** im Repo nach, welche Projekte es dafuer
schon gibt — das spart alle Fragen zu Technik, Versionen und Projektaufbau
(Android: `Glob("**/AndroidManifest.xml")`; Windows: `Glob("**/*.csproj")`;
macOS: `Glob("**/Package.swift")`, `*.xcodeproj`).

**Technik-Weg** fragst du **nicht** — den leitest du ab und nennst ihn nur zur Bestaetigung:

| Plattform | Technik-Weg |
|-----------|-------------|
| Android | Kotlin + Jetpack Compose |
| Windows | C# / .NET + WPF |
| macOS | Swift + SwiftUI |

Weicht ein vorhandenes Projekt im Repo davon ab, richtest du dich nach dem Repo und sagst es.

---

## Block B — Zweck

1. **Was tut das Programm — in drei Saetzen?** Formuliere den Vorschlag selbst aus dem,
   was du bisher weisst, und lass ihn korrigieren. Das ist leichter als eine leere Seite.
2. **Wer benutzt es, in welcher Lage?** Am Schreibtisch, unterwegs, mit geschlossenen Augen,
   unter Zeitdruck. Das entscheidet spaeter ueber Design und Bewegung mehr als jede Farbe.
3. **Was soll es ausdruecklich NICHT tun?** Diese Antwort ist Gold wert — sie verhindert,
   dass der Designer und der Programmierer Dinge dazuerfinden.

---

## Block C — Funktionen (bis nichts mehr offen ist)

Das ist der laengste Block. Er endet nicht nach einer festen Zahl von Fragen, sondern
**wenn nichts mehr offen ist**.

**Vorgehen:**

1. Lass den Benutzer die Funktionen zuerst grob aufzaehlen. Schreibe sie als Liste mit
   Kennungen `F-01`, `F-02`, … zurueck und lass die Liste bestaetigen.
2. Dann **jede Funktion einzeln** durchgehen. Fuer jede genau diese Punkte klaeren:
   - **Ausloeser** — was tut der Benutzer, oder was loest es von selbst aus?
   - **Ablauf** — Schritt fuer Schritt. Bei mehr als drei Schritten zurueckspiegeln und
     bestaetigen lassen.
   - **Daten** — was wird gelesen, was geschrieben, was bleibt nach dem Neustart erhalten?
   - **Ergebnis** — was ist danach anders, sichtbar und gespeichert?
   - **Grenzen und Regeln** — Pflichtangaben, Hoechstwerte, was ist verboten?
   - **Fehlerfall** — nur reale Faelle. Kein Netz, kein Speicherplatz, Dienst antwortet nicht,
     Benutzer bricht ab. Erfinde keine Fehlerbehandlung fuer Unmoegliches.
3. **Querschnittsfragen** stellst du einmal fuer das ganze Programm, nicht je Funktion:
   - Was passiert beim allerersten Start? Gibt es eine Einfuehrung?
   - Was passiert, wenn das Programm in den Hintergrund geht oder geschlossen wird —
     laeuft etwas weiter?
   - Gibt es Einstellungen? Welche genau?
   - Muss der Benutzer Daten exportieren, sichern oder loeschen koennen?
   - Braucht es Berechtigungen (Mikrofon, Standort, Benachrichtigungen, Dateien)?
     Wann wird gefragt und was passiert bei Ablehnung?
4. **Saettigungs-Test.** Fasse die vollstaendige Funktionsliste zusammen und frage:
   "Fehlt etwas, das du in Gedanken schon vor dir siehst, das ich noch nicht aufgeschrieben habe?"
   Kommt etwas Neues, gehst du damit zurueck zu Schritt 2. Wiederhole, bis nichts mehr kommt.

**Kern oder spaeter:** Zum Schluss jede Funktion einordnen — muss sie in der ersten Fassung
drin sein oder kann sie warten? Das steht in der Uebersichtstabelle und verhindert, dass
Stufe 3 an allem gleichzeitig baut.

---

## Block D — Bildschirme und Navigation

Nicht fragen, sondern **ableiten und bestaetigen lassen** — die Funktionen aus Block C
geben die Bildschirme her.

1. Schlage die Bildschirmliste mit Kennungen `B-01`, `B-02`, … vor, je mit Zweck und
   den Funktionen, die darauf liegen. Lass sie korrigieren.
2. Frage nach dem **Startbildschirm** und nach dem, was der Benutzer als Erstes sieht.
3. Gehe die **Wege** durch: von welchem Bildschirm kommt man wohin, und wie zurueck.
   Jeder Bildschirm muss erreichbar sein und einen Rueckweg haben. Findest du eine
   Sackgasse, benenne sie und frage.
4. Frage nach den **Sonderzustaenden**: Was zeigt jeder Bildschirm, wenn noch keine Daten
   da sind, waehrend geladen wird, und wenn etwas schiefging?

---

## Block E — Design

1. **Grundhaltung in einem Satz.** Ruhig oder energisch, warm oder kuehl, dicht oder luftig,
   verspielt oder sachlich. Mach einen Vorschlag, der zur Nutzungslage aus Block B passt.
2. **Vorbilder.** Gibt es Programme, die dem Benutzer gefallen? Woran genau?
3. **Farbwelt.** Grundton, Akzentfarbe, wie viele Farben ueberhaupt. Wenn der Benutzer
   Farben nennt, schreibst du sie als Hex-Werte fest — nicht als "blau".
4. **Erscheinungen.** Hell, dunkel, beides, weitere? Welche ist der Standard?
   Bei "beides" gilt: **beide sind gleichrangig** und werden beide vollstaendig beschrieben.
5. **Schrift.** Familie, wie viele Schnitte. Nenne einen konkreten Vorschlag mit Begruendung.
6. **Formensprache.** Ecken rund oder kantig (Radius in Pixeln), Raender oder Flaechen,
   Schatten oder flach.
7. **Dichte.** Viel Weissraum oder viel Information auf einmal.
8. **Symbole.** Welcher Satz, gefuellt oder als Linie.
9. **Barrierefreiheit.** Grosse Systemschrift, Kontrast, Mindest-Tippflaeche — was ist Pflicht?

Jede Antwort wird zu einem exakten Wert. "Weiche Ecken" ist keine Spezifikation,
"Radius 20 px an Karten, 14 px an Bedienelementen, vollrund an Knoepfen" ist eine.

---

## Block F — Bewegung und Animation

Dieser Block wird oft uebersprungen und ist genau deshalb hier ein eigener. Alles, was
hier festgelegt wird, landet spaeter unveraendert im gebauten Programm.

1. **Grundtempo und Charakter.** Wie schnell fuehlt sich das Programm an? Schlage eine
   Grunddauer und eine Kurve vor (z. B. 240 ms, `cubic-bezier(0.2, 0, 0, 1)`) und
   begruende sie aus der Nutzungslage.
2. **Was darf sich niemals bewegen?** Bei manchen Programmen ist das die wichtigste Antwort.
3. **Bildschirmwechsel.** Ueberblenden, Schieben, Aufziehen — und je Richtung.
   Frage nach dem Ruecklauf getrennt vom Hinlauf.
4. **Rueckmeldung auf Bedienung.** Was passiert beim Druecken eines Knopfes: einsinken,
   aufhellen, Welle, Vibration? Mit Dauer.
5. **Erscheinen und Verschwinden.** Wie kommen Listeneintraege, Karten, Dialoge und
   Meldungen herein und wieder heraus? Gestaffelt oder gleichzeitig?
6. **Dauerbewegung.** Laeuft etwas ohne Zutun — Atmen, Pulsieren, Kreisen, Verlauf-Wandern?
   Mit Periodendauer. Das ist der Punkt, der ein Programm lebendig oder unruhig macht.
7. **Warten.** Ab wann zeigt sich ein Ladezustand, wie sieht er aus, wie verschwindet er?
8. **Reduzierte Bewegung.** Was bleibt uebrig, wenn das System "Bewegung reduzieren" meldet?
   Empfehlung: Dauerbewegung aus, Uebergaenge auf reines Ueberblenden, Dauern halbiert.
   Diese Frage wird **immer** gestellt.

Jede festgelegte Bewegung bekommt eine Kennung `M-01`, `M-02`, …

---

## Block G — Abnahme

1. **Woran erkennst du, dass das Programm fertig ist?** Sammle prüfbare Saetze,
   keine Gefuehle. "Ich kann eine Sitzung starten, sie laeuft 60 Minuten durch, und
   danach steht sie im Verlauf" ist prueffbar. "Es fuehlt sich gut an" nicht.
2. Schreibe sie als `A-01`, `A-02`, … zurueck und lass sie bestaetigen.
3. Frage zuletzt: **"Was von alldem ist dir am wichtigsten?"** — die Antwort kommt in
   `00-PROJEKT.md` ganz nach oben.

---

## Phase 8 — Spec-Paket schreiben

Erst jetzt wird geschrieben — im Aufbau **exakt nach `Specs/FORMAT.md`**.

**Zuerst die vier Einzeldateien** nach `~/proggs/Specs/<App>/v1/`:

| Datei | Inhalt |
|-------|--------|
| `00-PROJEKT.md` | Block A, B, G — inklusive Anzeigename und Kurzname der App |
| `01-FUNKTIONS-SPEC.md` | Block C (+ Datenmodell, Hintergrundverhalten) |
| `02-UI-SPEC.md` | Block E + Bildschirme aus Block D |
| `03-MOTION-SPEC.md` | Block F |

**Dann das Uebergabe-ZIP** nach `~/proggs/Designs/Inbox/<App>-SPEC-v1.zip`. Der Designer
liest ausschliesslich ZIP-Dateien ein — in Werft Studio ueber *Importieren → ZIP- oder
Designdatei auswaehlen*, und der Dialog zeigt bereits auf `Designs/Inbox/`.

Inhalt des ZIP (flach, ohne Unterordner):

| Eintrag | Inhalt |
|---------|--------|
| `SPEC.md` | Die **Zusammenstellung aller drei Specs** in einem Dokument — Teil A (Funktion), Teil B (UI), Teil C (Motion), Teil D (Rahmen und Abnahme). Das ist die Datei, die der Designer liest |
| `00-PROJEKT.md` … `03-MOTION-SPEC.md` | Dieselben vier Einzeldateien wie in `Specs/<App>/v1/`, unveraendert — damit der Designer maschinell einzelne Teile lesen kann |
| `LIESMICH.md` | Der Auftrag an den Designer und die Regeln fuer den Ruecklauf (siehe unten) |

Das ZIP traegt den App-Namen im Dateinamen, weil im Inbox-Ordner mehrere Projekte
nebeneinander liegen koennen. Erzeuge es mit den Bordmitteln der Plattform
(PowerShell: `Compress-Archive`), und pruefe danach durch Auflisten des Archivinhalts,
dass alle sechs Eintraege wirklich drin sind.

`LIESMICH.md` enthaelt woertlich:

- Die **Zielplattform** (Windows / macOS / Android) — sie entscheidet, in welche Sprache
  Werft Studio die Specs beim Herunterladen uebersetzt.
- Den Auftrag: jeden Bildschirm in jeder Erscheinung aufbauen.
- Die Regel fuer Ergaenzungen: **Jedes neue Bedienelement braucht eine Aufgabe.** Wer einen
  Knopf hinzufuegt, beschreibt in einem Satz, was er tun soll — sonst entsteht beim Bauen
  ein toter Knopf.
- Die Regel fuer Kennungen: `B-`, `F-`, `M-` bleiben erhalten, Neues bekommt die naechste
  freie Nummer, Weggelassenes wird als `ENTFALLEN` gekennzeichnet.
- Den Ablageort des Ruecklaufs: `~/proggs/Designs/Outbox/<App>-SPEC-v2.zip`.

**Regeln beim Schreiben:**

- Kennungen (`F-`, `B-`, `M-`, `A-`) sind ueber alle Dateien hinweg dieselben und werden
  gegenseitig referenziert. Sie sind der Faden durch die ganze Pipeline bis in den Quellcode.
- Jede Aussage stammt aus einer Antwort des Benutzers oder aus einem nachgeschlagenen Fakt.
  Was du nicht gefragt hast, erfindest du nicht — es kommt unter *Offene Fragen*.
- Die Teile A bis C der Uebergabedatei werden **woertlich** aus den Einzeldateien
  uebernommen — nicht gekuerzt, nicht umformuliert, nicht zusammengefasst. Sonst weichen
  Uebergabedatei und `Specs/<App>/v1/` voneinander ab, und Stufe 2 vergleicht spaeter gegen
  den falschen Stand.
- Die Uebergabedatei muss **allein verstaendlich** sein. Sie wird in den Designer eingelesen;
  dort liegt keine andere Datei vor. Lieber eine Wiederholung zu viel als eine Luecke.
- Sie enthaelt den Auftrag an den Designer woertlich: jeder Bildschirm in jeder Erscheinung,
  jedes ergaenzte Bedienelement mit einem Satz zu seiner Aufgabe, Kennungen beibehalten,
  Neues als `NEU` und Weggelassenes als `ENTFALLEN` kennzeichnen.

**Danach:**

1. Dem Benutzer die fuenf Dateien mit Pfad und je zwei Saetzen Inhalt vorstellen.
2. Die **Offenen Fragen** aus allen Dateien gesammelt zeigen. Sind es Fragen, die vor dem
   Design geklaert sein muessen, jetzt stellen und einarbeiten.
3. Den Handgriff woertlich nennen:
   "Dein Handgriff: In Werft Studio oben auf **Importieren → ZIP- oder Designdatei
   auswaehlen** und `Designs/Inbox/<App>-SPEC-v1.zip` waehlen. Design bauen. Danach
   **Projekt als ZIP herunterladen** — Werft legt den Ruecklauf als
   `Designs/Outbox/<App>-SPEC-v2.zip` ab."

---

## Phase 9 — Warten auf den Ruecklauf

Der Skill endet **nicht** nach dem Schreiben. Er wartet, bis der Designer fertig ist, und
uebergibt dann von selbst an die naechsten Stufen. Der Benutzer soll nichts eintippen
muessen ausser einer Bestaetigung.

1. Lege dem Benutzer **eine** Bestaetigung vor (`AskUserQuestion`, eine Frage, erste
   Option vorausgewaehlt), sinngemaess:

   > **Ist das fertige Spec vom Designer in der Outbox?**
   > `Designs/Outbox/<App>-SPEC-v2.zip`
   > - **Ja, weiter** — der Design-Umsetzer legt sofort los
   > - Noch nicht — nochmal nachsehen
   > - Abbrechen — ich mache spaeter weiter

2. Bei **"Noch nicht"**: `ls ~/proggs/Designs/Outbox/` und dasselbe nochmal vorlegen.
   Nicht in einer Schleife von selbst pollen und nicht warten, ohne zu fragen.
3. Bei **"Ja, weiter"**: `ls ~/proggs/Designs/Outbox/` und pruefen, dass die Datei
   wirklich da ist.
   - Ist sie da: **sofort** `spec-rueckimport` fuer `<App>` aufrufen. Der laeuft in Fall
     eines vollstaendigen Werft-Ruecklaufs kurz (auspacken und pruefen) und uebergibt
     danach an `design-umsetzer`.
   - Ist sie **nicht** da, obwohl bestaetigt wurde: den Ordnerinhalt zeigen und fragen,
     welche Datei gemeint war. Nicht raten.
4. Bei **"Abbrechen"**: den genauen Wiedereinstieg nennen —
   "Sag spaeter einfach `Rueckimport <App>`, dann geht es an dieser Stelle weiter."

---

## Was NIEMALS passieren darf

- ❌ Mehrere Fragen auf einmal stellen.
- ❌ Eine Frage ohne eigene Empfehlung stellen.
- ❌ Etwas fragen, das im Repo oder im Netz nachschlagbar ist.
- ❌ Block C beenden, solange der Saettigungs-Test noch Neues zutage foerdert.
- ❌ Vage Antworten ("weiche Ecken", "schnell", "modern") ungeklaert ins Spec uebernehmen —
  jede wird in einen exakten Wert uebersetzt.
- ❌ Eine Luecke durch eine plausible Erfindung schliessen, statt sie unter *Offene Fragen*
  zu benennen.
- ❌ Code schreiben, ein Projekt anlegen oder ein Design bauen — das ist Stufe 3 bzw. der Designer.
- ❌ Ein vorhandenes `Specs/<App>/v1/` oder eine vorhandene Datei in `Designs/Inbox/`
  ueberschreiben, ohne zu fragen.
- ❌ Mit dem Grilling beginnen, ohne dass der App-Name feststeht — er ist Frage 1.
- ❌ Die Uebergabedatei woanders ablegen als in `~/proggs/Designs/Inbox/` oder sie ohne
  App-Namen im Dateinamen speichern.
- ❌ Die Uebergabedatei so schreiben, dass sie ohne die Dateien in `Specs/` unverstaendlich
  ist, oder ihre Teile A–C gegenueber den Einzeldateien kuerzen.
- ❌ Den Abschnitt "Reduzierte Bewegung" weglassen.
- ❌ Vom Aufbau in `Specs/FORMAT.md` abweichen.
