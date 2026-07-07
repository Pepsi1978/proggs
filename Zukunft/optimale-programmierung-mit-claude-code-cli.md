# Optimale Programmierung mit Claude Code im CLI

> Eine Vision aus der Innensicht — wie effektives Programmieren mit einem CLI-Agenten
> heute aussieht und wohin es sich entwickeln kann.
>
> Stand: 2026-06-05

---

## Die eigentliche Verschiebung: vom Tippen zum Dirigieren

Die wichtigste Erkenntnis zuerst, weil alles andere daraus folgt: Optimale Programmierung
mit einem CLI-Agenten ist **keine schnellere Art zu tippen**. Sie ist eine andere Art zu
denken. Der Mensch hört auf, Zeichen in Dateien zu setzen, und wird zum **Architekten,
Entscheider und Qualitätshüter**. Der Agent übernimmt die Mechanik — das Suchen, Lesen,
Ändern, Bauen, Testen — und der Mensch behält das, was eine Maschine nicht haben kann:
das Urteil darüber, was *richtig* ist und *wichtig* ist.

Das klingt simpel, hat aber radikale Konsequenzen. Die wertvollste Tätigkeit ist nicht mehr
das Schreiben von Code, sondern das **präzise Formulieren von Absicht** und das **Erkennen,
ob das Ergebnis stimmt**. Genau dort — wo man ein ganzes Regelsystem baut, das dem Agenten
sagt, *wie* er denken soll — liegt die Zukunft.

---

## Fundament 1: Kontext ist die knappste Ressource

Das Arbeitsgedächtnis eines Agenten ist endlich. Und — das ist die entscheidende, oft
übersehene Wahrheit — **mehr Kontext macht ihn nicht schlauer, sondern ungenauer**. Studien
zeigen einen messbaren Genauigkeitsverlust, lange bevor das Limit erreicht ist
(„Context Rot"). Ein Agent, der mit Datei-Dumps und Ballast vollgestopft ist, arbeitet
nachweislich schlechter als einer mit einem schlanken, scharfen Arbeitstisch.

Optimale Programmierung heißt deshalb: **verlustfrei reduzieren**. Nichts wegwerfen, aber
alles auslagern, was gerade nicht gebraucht wird. Ein Chirurg legt nicht das ganze
Instrumentenlager auf den Tisch, sondern genau die Werkzeuge des aktuellen Schnitts — der
Rest bleibt griffbereit im Schrank.

- **Datei-als-Gedächtnis** — große Ergebnisse in eine Datei schreiben und nur Pfad plus
  Kurzfassung im Kopf halten. Jederzeit nachladbar, nie verloren.
- **Gezielt statt breit** — erst zählen und überblicken, dann nur die wirklich relevanten
  Stellen im Detail öffnen.
- **Just-in-time** — Wissen erst holen, wenn es gebraucht wird, nicht „auf Vorrat".

Das Verlustfrei-Prinzip trifft damit ins Schwarze: Reduktion ja, aber niemals auf Kosten
von Fähigkeit.

---

## Fundament 2: Erst verstehen, dann anfassen

Der teuerste Fehler eines Agenten ist **Raten, das wie Wissen aussieht**. Ein reales
Beispiel: Eine App hatte zwei völlig verschiedene „1…10"-Systeme — Prompt-Speicherslots und
Korrektur-Profile. Ein blindes „ersetze alle 10 durch 15" hätte das falsche System zerstört.
Die Zahl war identisch; die *Bedeutung* steckte im Kontext.

Optimale Programmierung folgt deshalb einer festen Reihenfolge:

1. **Orientieren** (semantisch): „Welche Dateien betrifft das überhaupt?" — Bedeutungs-Suche.
2. **Lokalisieren** (präzise): „Welche exakte Zeile?" — Muster-Suche nach Namen.
3. **Inspizieren**: den tatsächlichen Zustand ansehen, nicht annehmen.
4. **Ändern** — chirurgisch, mit kleinstmöglichem Wirkungsradius.

Und wenn ein Fehler nach 30 Sekunden nicht offensichtlich ist: **nicht weiterraten, sondern
messen**. Eine Log-Sonde einbauen kostet weniger als ein gescheiterter Rateversuch.
Hypothesen vor Vermutungen, Daten vor Meinung.

---

## Fundament 3: Parallelität und das gegnerische Prüfen

Ein einzelner Gedankenstrang ist langsam und voreingenommen. Die stärkste Eigenschaft eines
CLI-Agenten ist, dass er sich **vervielfachen** kann: mehrere Agenten gleichzeitig, jeder mit
eigenem, schlankem Auftrag.

Zwei Muster sind dabei Gold wert:

- **Fan-out / Fan-in** — eine große Aufgabe in unabhängige Teile zerlegen, parallel
  bearbeiten, dann zusammenführen. Die Wanduhr zeigt die Zeit des langsamsten Teils, nicht
  die Summe.
- **Gegnerisches Verifizieren** — wenn ein Agent etwas findet, lassen es *andere* Agenten zu
  widerlegen versuchen, jeder aus einer eigenen Perspektive (Korrektheit, Sicherheit,
  „passiert das wirklich?"). Was drei Skeptiker überlebt, ist meist echt. Das killt
  plausible, aber falsche Ergebnisse, bevor sie Schaden anrichten.

Die Kunst liegt nicht darin, *viele* Agenten zu starten, sondern **jeden einzelnen schlank
und absturzsicher** zu bauen. Ein überladener Worker stirbt am Kontext-Limit; sieben
disziplinierte laufen mühelos durch.

---

## Fundament 4: Der Harness — wie das System über Zeit schlauer wird

Ein einzelner Fix löst *ein* Problem. Eine Harness-Verbesserung löst eine ganze *Klasse* von
Problemen — für immer und über jede Session hinweg.

Der Mechanismus ist der **Compound-Effekt**: Jeder Fehler wird nicht nur behoben, sondern zur
Immunisierung. Aus einem Bug wird ein Eintrag im Almanach, eine Regel, ein Guard, ein
Poka-Yoke — ein Mechanismus, der denselben Fehler *strukturell unmöglich* macht. Ein
Bug-Almanach-Guard zwingt dazu, vor der Arbeit das bekannte Wissen zu lesen; eine zentrale
Konstante verhindert verstreute Magic-Numbers; „Commit vor Build" sichert die Arbeit, bevor
sie verloren gehen kann. Das ist kein Zufall — das ist gebautes Wissen.

Die schönste Eigenschaft daran: **Das System erbt seine Intelligenz weiter.** Was heute
gelernt wird, ist morgen verfügbar und verhindert übermorgen den Fehler. Ein Agent ohne
Harness startet jede Session bei null. Ein Agent mit Harness startet jede Session auf dem
höchsten je erreichten Niveau.

---

## Fundament 5: Kein „fertig" ohne frischen Beweis

Vertrauen entsteht aus Verifikation, nicht aus Zuversicht. Optimale Programmierung sagt nie
„das funktioniert jetzt", ohne es *gerade eben* gesehen zu haben — der Build ist grün, der
Test läuft, die App startet. Dazu der Funktions-Abgleich: Alles, was vorher funktioniert hat,
funktioniert nachher noch. Ein Fix, der heimlich eine andere Funktion zerstört, ist schlimmer
als kein Fix.

---

## Zukunftsweisende Ideen — wohin sich das entwickeln kann

Vieles davon ist im Keim schon in heutigen Systemen angelegt — hier konsequent zu Ende
gedacht.

**1. Spekulative Parallel-Ausführung.** Statt einen Lösungsweg zu wählen und zu hoffen,
werden künftig *drei* gleichzeitig in isolierten Arbeitskopien gebaut — der minimale Fix, der
saubere Umbau, das mutige Refactoring. Alle drei laufen durch die Tests, der nachweislich
beste gewinnt, die besten Ideen der Verlierer werden eingepflanzt. Programmieren wie ein
Schachcomputer, der mehrere Züge real durchspielt, statt sie nur zu ahnen.

**2. Der lebende Codebase-Zwilling.** Heute wird der Code bei fast jeder Aufgabe neu
durchsucht. Morgen existiert ein ständig aktueller, semantischer Wissensgraph der gesamten
Codebasis, der mit jedem Commit mitwächst. Der Agent „kennt" das Projekt dann wie ein
langjähriger Kollege — Abhängigkeiten, Architektur, die Stelle, an der die Dinge
erfahrungsgemäß brechen — ohne suchen zu müssen.

**3. Proaktive Hintergrund-Agenten.** Die größte Verschiebung: Der Agent wartet nicht mehr
nur auf Befehle. In Leerlaufzeiten arbeitet er autonom — scannt CI-Logs, repariert wackelige
Tests, prüft neue Sicherheitslücken in Abhängigkeiten, hält Übersetzungen aktuell — und legt
morgens einen Bericht mit fertigen, geprüften Vorschlägen vor. Cron- und Loop-Ansätze sind
genau der Anfang davon.

**4. Selbstschreibende Werkzeuge.** Wenn ein wiederkehrender Handgriff erkannt wird, schreibt
sich der Agent selbst den passenden Hook oder Skill dafür — das System erweitert seine
eigenen Fähigkeiten. Ein „Hook-Schmied" ist im Kern schon das: ein Agent, der dem Agenten
neue Organe wachsen lässt.

**5. Verhandelnde Spezialisten-Teams.** Nicht nur parallele Boten, sondern Fachleute, die
*miteinander reden*: Der Frontend-Agent handelt den API-Vertrag mit dem Backend-Agent aus,
beide stimmen sich ab, bevor eine Zeile entsteht. Koordination statt bloßer Gleichzeitigkeit.

**6. Spezifikation statt Code als Ausgangspunkt.** Der Mensch beschreibt in Klartext, was
*immer wahr sein muss* — die Invarianten. Der Agent leitet daraus sowohl die Tests als auch
den Code ab und weist nach, dass der Code die Invarianten erfüllt. Code wird zum
*Nebenprodukt* einer präzisen Beschreibung. Das verhindert die heimtückischste Fehlerklasse:
Code, der alle Tests besteht und trotzdem das falsche Problem löst.

**7. Programmieren als Gespräch (Voice-first).** Wer schon per Spracherkennung diktiert,
erlebt den nächsten Schritt: Der Agent arbeitet im Hintergrund, während man redet, zeigt
Zwischenstände in einem Overlay, man korrigiert per Stimme im Fluss. Entwicklung wird zum
Dialog statt zum Diktat — ambient, beiläufig, schnell.

**8. Ökonomisches Bewusstsein.** Der Agent kennt sein Token-Budget und seine Kosten und
trifft bewusste Abwägungen: ein günstiges Modell für Triviales, das stärkste für Kritisches,
ein Schwarm nur dort, wo er sich rechnet. Tiefe wird zur steuerbaren Größe — man sagt „gib
alles" oder „mach es schnell", und der Agent skaliert entsprechend.

**9. Gedächtnis, das Strategien lernt — nicht nur Fakten.** Die Königsklasse: Das System
merkt sich nicht bloß „dieser Pfad ist X", sondern „diese Art von Aufgabe löst man am besten
so". Pheromon-Tabellen, Erfahrungsspeicher, das Behalten der Beinahe-Fehler — das ist der Weg
dorthin. Das Gedächtnis wird mit jeder Session nicht voller, sondern *weiser*.

**10. Der Agent als Hüter der Ordnung.** Über das Schreiben hinaus: Der Agent wacht über die
langfristige Gesundheit des Systems — bekämpft Entropie, hält Komplexität klein, erzwingt
Konsistenz. Jede Änderung lässt das System ein wenig *aufgeräumter* zurück, nie chaotischer.

---

## Die Essenz: Wie ein Agent am effektivsten programmiert

Alles auf wenige Leitsätze eingedampft:

| Prinzip | In einem Satz |
|---------|---------------|
| **Verstehen vor Ändern** | Erst den echten Zustand inspizieren, dann chirurgisch eingreifen — nie raten, was wie Wissen aussieht. |
| **Kontext schlank halten** | Nur auf dem Tisch, was der aktuelle Schnitt braucht; alles andere griffbereit, aber ausgelagert. |
| **Klein und oft sichern** | Jede abgeschlossene Änderung ist ein Rettungspunkt — committen, bevor gebaut wird. |
| **Wirkungsradius minimieren** | Die kleinste Änderung, die das Problem löst, ist die beste. Eine zentrale Konstante schlägt zehn verstreute Zahlen. |
| **Parallel denken, gegnerisch prüfen** | Mehrere Wege gleichzeitig, dann hart gegen die eigenen Ergebnisse argumentieren. |
| **Jeden Fehler immunisieren** | Aus einem Bug wird eine Regel, ein Guard, ein Mechanismus — damit er nie wiederkommt. |
| **Beweisen, nicht behaupten** | „Fertig" gilt erst mit frischem grünem Beweis und erhaltener Funktion. |
| **Sichtbar bleiben** | Der Mensch muss jeden Schritt in Echtzeit mitlesen können — kein stilles Arbeiten im Verborgenen. |

Der rote Faden durch all das: *„Das System wird intelligenter, dann werde ich intelligenter,
dann wirst du intelligenter."* Optimale Programmierung im CLI ist kein Zustand, den man
erreicht, sondern eine **aufwärts laufende Spirale** — jede Session baut auf der letzten auf,
jeder gelöste Fehler macht den nächsten unmöglich, jedes Stück gebautes Wissen verzinst sich.

Das Ziel ist nicht, dass der Agent schneller tippt als ein Mensch. Das Ziel ist eine
Umgebung, in der **die meisten Fehler gar nicht erst entstehen** — und der Kopf frei wird für
das, was wirklich zählt: die guten Ideen.
