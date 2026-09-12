---
name: modul-erstellen
description: Löst einen gut funktionierenden Bereich aus einer fertigen App heraus und legt ihn als wiederverwendbares Modul unter ~/proggs/Module/<Plattform>/Mx.y-Name/ ab — plattformübergreifend für Android (Kotlin/Compose), Windows (C#/WPF), macOS und iOS (Swift/SwiftUI). Nutze diesen Skill IMMER wenn der Benutzer sagt "mach daraus ein Modul", "als Modul abspeichern", "speicher das als Modul", "extrahiere X als Modul", "bau aus App Y das X-Modul", "das gefällt mir, das will ich wiederverwenden", "als M1.x speichern", "neues Modul anlegen", "Modul erstellen", "Modul-Skill", "in die Modul-Bibliothek aufnehmen", "das Drag-and-Drop als Modul", "herauslösen und ablegen". Ebenso bei Sätzen, die gleich einen Namen mitgeben — "bau daraus ein Modul und nenn es X", "erstelle ein Modul mit dem Namen X", "mach daraus das X-Modul", "speicher das als X ab". Auch bei Spracherkennungs-Varianten wie "Zwift" (= Swift), "Dreck-and-Drop" (= Drag-and-Drop) oder "Modul abspeichern aus der App". Der Skill übernimmt den genannten Namen wörtlich als Anzeigenamen und fragt danach, wenn keiner genannt wurde, vergibt die nächste freie M-Nummer, schneidet die Abhängigkeiten zur Ursprungs-App sauber durch, schreibt das Manifest MODUL.md, verdrahtet die Ursprungs-App auf die Kopie um und baut sie zur Abnahme durch. NICHT nutzen, wenn ein bestehendes Modul in eine App eingebaut werden soll ("bau M1.1 ein", "nimm M1.1 bis M1.7", "Modul einbauen") oder wenn eine Änderung an alle Konsumenten verteilt werden soll ("zieh M1.1 nach") — dafür ist der Skill modul-einbauen zuständig.
---

# Modul erstellen

Ein Modul entsteht nicht aus dem Nichts, sondern aus Code, der sich in einer
echten App bereits bewährt hat. Dieser Skill hebt so einen Bereich heraus,
ohne ihn dabei zu verschlimmbessern.

**Ablageort:** `~/proggs/Module/<Plattform>/Mx.y-Kurzname/`
**Index:** `~/proggs/Module/INDEX.md` — Nummernkreise und Modulliste stehen dort.

## Die drei Grundsätze

Sie erklären die meisten Einzelentscheidungen weiter unten.

**1 — Kopie, nicht Verdrahtung.** Jede App bekommt eine eigene Abschrift der
Moduldateien. Kein `srcDir`, kein Paket-Server, keine Projektreferenz auf den
Modulordner. Grund: Jeder App-Ordner bleibt allein baubar — auch wenn der
Modulordner fehlt, das Netz weg ist oder nur diese eine App ausgecheckt wurde.
Der Preis ist, dass ein Fix mehrfach verteilt werden muss; das erledigt später
der Skill `modul-einbauen` auf Ansage.

**2 — Die Kopie ist byte-identisch mit der Quelle.** Deshalb steht der
Herkunfts-Kopf in der *Moduldatei selbst*, nicht erst in der App-Kopie. Dann
beantwortet ein `diff` die Frage „ist diese App aktuell?" ohne Nachdenken, und
Nachziehen ist schlichtes Überschreiben.

**3 — Die Modulgrenze ist die Schnittstelle, nicht die Schicht.** Nicht
„Frontend oder Backend" fragen, sondern: Alles oberhalb der Rückruf-Grenze
gehört ins Modul, alles darunter bleibt in der App. Braucht ein Modul
Persistenz, definiert es eine Schnittstelle und die App erfüllt sie. Ein Modul
legt niemals eigene Datenbanktabellen an — das würde sich in jede App
hineinfressen.

## Ablauf

### Phase 1 — Nummer und Name

**Die Nummer kommt von dir, der Name vom Benutzer.** Das ist die Arbeitsteilung,
auf die der Skill ausgelegt ist: Der Benutzer sagt „bau daraus ein Modul und
nenn es Drag & Drop Modul", und alles Weitere ergibt sich daraus.

**Nummer.** Lies `~/proggs/Module/INDEX.md`, bestimme den Nummernkreis aus der
Plattform (M1 Android, M2 Windows, M3 macOS, M4 iOS) und nimm die nächste freie
Nummer im Kreis — also die höchste vorhandene plus eins. Ist M1.7 die höchste,
wird das neue Modul M1.8. Lücken werden **nicht** nachbelegt: Eine Nummer, die
einmal vergeben war, bleibt verbraucht, sonst zeigen alte Notizen und
Commit-Nachrichten irgendwann auf das falsche Modul.

**Name.** Nimm den Namen, den der Benutzer nennt, wörtlich als Anzeigenamen —
auch wenn er lang ist, Leerzeichen hat oder auf „Modul" endet. Er hat ihn sich
so gemerkt, und in einem halben Jahr sucht er danach.

Nennt er **keinen** Namen, frag nach, bevor du irgendetwas anlegst:

> „Wie soll das Modul heißen? Die Nummer wird M1.8."

Erfinde keinen Namen aus dem Code heraus. Ein selbst ausgedachter Name findet
sich später nicht wieder, weil der Benutzer nach seinem eigenen Wort sucht.

**Aus dem Anzeigenamen werden drei Dinge abgeleitet:**

| | Beispiel | Regel |
|---|---|---|
| Anzeigename | `Drag & Drop Modul` | wörtlich, kommt in `MODUL.md` und `INDEX.md` |
| Ordnername | `M1.8-Drag-und-Drop-Modul` | Nummer, Bindestrich, Name werkzeugfest gemacht |
| Technischer Kurzname | `dragunddropmodul` | für Namensraum und Ordner in der App |

Die Umformung zum Ordnernamen ist bewusst winzig und vorhersehbar:

1. `&` wird zu `und`
2. Leerzeichen werden zu `-`
3. Zeichen, die in Pfaden nicht vorkommen dürfen (`/ \ : * ? " < > |`), fallen weg

Bei einem Namen ohne Leerzeichen und Sonderzeichen passiert dadurch gar nichts —
aus `DragReorder` wird schlicht `M1.1-DragReorder`.

Der Grund für Schritt 1 und 2: Ein `&` im Pfad ist in PowerShell der
Aufrufoperator und in `cmd` ein Befehlstrenner. Ein Ordner mit `&` oder
Leerzeichen zwingt jeden späteren Befehl in Anführungszeichen, und genau das
wird irgendwann vergessen. Der Anzeigename bleibt davon unberührt — verloren
geht nichts, es steht nur an zwei Stellen leicht verschieden.

Der technische Kurzname ist kleingeschrieben, nur ASCII (Umlaute werden
umschrieben), ohne Trennzeichen — Namensräume vertragen nichts anderes. Zeig ihn
dem Benutzer zusammen mit der Nummer, damit er ihn in einem Satz korrigieren
kann, falls er zu sperrig gerät:

> „Wird M1.8-Drag-und-Drop-Modul, Namensraum `de.frank.module.dragunddropmodul`."

**Namensdopplung.** Gibt es den Namen im Kreis schon: abbrechen und nachfragen,
ob das bestehende Modul erweitert werden soll. Eine zweite Nummer für dieselbe
Sache macht den Index unbrauchbar.

### Phase 2 — Bereich finden und Nabelschnüre vorlegen

Finde die beteiligten Dateien in der Quell-App. Liste dann **jede Verbindung
zur App** auf, die im Modul nicht bestehen bleiben kann. Die plattformtypischen
Verdächtigen stehen in der passenden Referenzdatei:

- Android → `references/android.md`
- Windows → `references/windows.md`
- macOS → `references/macos.md`
- iOS → `references/ios.md`

Für jede Nabelschnur gibt es drei Wege — **Parameter** (kleinste Kopplung,
Standardwahl), **eigener Mechanismus im Modul**, oder **dokumentierte
Host-Anforderung**. Leg dem Benutzer die Liste mit deiner Empfehlung vor und
warte auf sein Ja, bevor du schneidest. Was er hier entscheidet, bestimmt, wie
angenehm sich das Modul in der nächsten App anfühlt.

Ressourcen zählen immer als Nabelschnur: Ein Modul darf keine `R.string`,
`R.color`, kein `ResourceDictionary` und keine `xcassets` der App benutzen — in
einer fremden App gibt es die nicht. Texte und Farben kommen als Parameter
herein oder stehen unter „Host muss liefern".

### Phase 3 — Herauslösen

Lege `Module/<Plattform>/Mx.y-Name/src/` an und verschiebe die Dateien dorthin.
Dabei:

- **Namensraum umbenennen** auf den Modul-Namensraum der Plattform (siehe
  Referenzdatei). Ein Modul, das noch `de.frank.genialeideen.ui` heißt, ist
  kein Modul, sondern eine verschobene App-Datei.
- **Herkunfts-Kopf** aus `assets/dateikopf.txt` an den Anfang jeder Datei
  setzen, mit Nummer, Name und `Stand v1`.
- **Nicht generalisieren.** Wenn der Code `Long` als ID benutzt, bleibt es
  `Long`. Der Bereich funktioniert heute; eine Verallgemeinerung auf Verdacht
  fügt Fehler hinzu, ohne einen echten zweiten Anwendungsfall zu kennen.
  Verallgemeinert wird, wenn der zweite Konsument es wirklich braucht.

### Phase 4 — Manifest

Schreib `MODUL.md` nach der Vorlage `assets/MODUL.md.template`. Die Felder, die
wirklich zählen:

- **Herkunft** — App, Dateipfade, Commit-Hash. Damit ist später nachvollziehbar,
  gegen welchen Stand das Modul geschnitten wurde.
- **Host muss liefern** — die Nabelschnüre aus Phase 2, die nicht zu Parametern
  wurden. Ohne diese Liste scheitert der Einbau in die nächste App.
- **Mindestens** — Compose-, .NET- oder Swift-Version. Ein Modul, das eine neue
  API benutzt, bricht sonst stillschweigend in einer älteren App.
- **Konsumenten** — App, Stand und **Pfad der Kopie**. Das ist die Liste, die
  der Skill `modul-einbauen` später abarbeitet.

Schreib zusätzlich `BEISPIEL.md` mit dem echten Aufrufcode aus der Quell-App —
meist nur wenige Zeilen. Das ist beim nächsten Einbau mehr wert als jede
Prosa-Beschreibung.

### Phase 5 — Quell-App umverdrahten und abnehmen

Ein Modul, das nur herauskopiert wurde, ist ungetestet. Deshalb:

1. Alten Code in der Quell-App **löschen**.
2. Die Moduldateien **byte-identisch** in den App-Baum kopieren, an den in der
   Referenzdatei genannten Ort.
3. Importe an den Aufrufstellen auf den neuen Namensraum ziehen.
4. **Durchbauen.** Erst wenn die Quell-App grün baut, existiert das Modul.

Bricht der Build, ist das kein Rückschlag, sondern genau die Information, für
die dieser Schritt da ist: Eine Nabelschnur wurde übersehen. Zurück zu Phase 2.

### Phase 6 — Abschluss

- Modulzeile in `Module/INDEX.md` eintragen (Format: `assets/INDEX-zeile.md`).
- Für die **Quell-App** gilt Regel 9 vollständig: bauen, Version bumpen mit
  echter Systemzeit, committen, pushen, auf dem Gerät installieren.
- Das Modul selbst hat keinen eigenen Build und keine App-Version — sag das
  ausdrücklich, statt den Schritt stillschweigend zu überspringen.
- **Ein Commit** für beides, Modul und Quell-App. Sie gehören zusammen; getrennt
  committet gäbe es einen Stand, in dem die App auf ein Modul verweist, das es
  noch nicht gibt.

## Modul-Versionen

Die Version ist eine ganze Zahl: `v1` beim Anlegen, `+1` bei jeder Änderung am
Modul. Sie steht an zwei Stellen — im Dateikopf und in `MODUL.md`. Ohne sie ist
der Eintrag `Konsumenten: GenialeIdeen (Stand v3)` wertlos, und niemand kann
sagen, welche App hinterherhinkt.

## Was dieser Skill nicht tut

Ein bestehendes Modul in eine weitere App einbauen, oder eine Modul-Änderung an
alle Konsumenten verteilen. Beides ist Sache des Skills `modul-einbauen`. Kommt so eine
Bitte herein, sag das und bau nichts Halbes.
