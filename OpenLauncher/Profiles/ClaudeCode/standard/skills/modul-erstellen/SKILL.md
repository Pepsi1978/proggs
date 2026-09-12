---
name: modul-erstellen
description: Löst einen gut funktionierenden Bereich aus einer fertigen App heraus und legt ihn als wiederverwendbares Modul unter ~/proggs/Module/<Plattform>/<Ordnername>/ ab — plattformübergreifend für Android (Kotlin/Compose), Windows (C#/WPF), macOS und iOS (Swift/SwiftUI). Nutze diesen Skill IMMER wenn der Benutzer sagt "mach daraus ein Modul", "als Modul abspeichern", "speicher das als Modul", "extrahiere X als Modul", "bau aus App Y das X-Modul", "das gefällt mir, das will ich wiederverwenden", "als M1.x speichern", "neues Modul anlegen", "Modul erstellen", "Modul-Skill", "in die Modul-Bibliothek aufnehmen", "das Drag-and-Drop als Modul", "herauslösen und ablegen". Ebenso bei Sätzen, die gleich einen Namen mitgeben — "bau daraus ein Modul und nenn es X", "erstelle ein Modul mit dem Namen X", "mach daraus das X-Modul", "speicher das als X ab". Auch bei Spracherkennungs-Varianten wie "Zwift" (= Swift), "Dreck-and-Drop" (= Drag-and-Drop) oder "Modul abspeichern aus der App". Dieser Skill pflegt AUSSERDEM bestehende Module — nutze ihn ebenso bei "fixe M1.1", "ändere das Modul X", "M1.1 soll jetzt auch Y können", "bessere das Modul nach", "hebe den Fix aus App Z ins Modul", "neue Modulversion", "Modul-Version anheben", "das Modul hat einen Fehler". Änderungen an einem Modul passieren IMMER in der Bibliothek, nie in einer App-Kopie. Der Skill übernimmt den genannten Namen wörtlich als Anzeigenamen und fragt danach, wenn keiner genannt wurde, vergibt die nächste freie M-Nummer, schneidet die Abhängigkeiten zur Ursprungs-App sauber durch, schreibt das Manifest MODUL.md, verdrahtet die Ursprungs-App auf die Kopie um und baut sie zur Abnahme durch. NICHT nutzen, wenn ein bestehendes Modul in eine App eingebaut werden soll ("bau M1.1 ein", "nimm M1.1 bis M1.7", "Modul einbauen") oder wenn eine Änderung an alle Konsumenten verteilt werden soll ("zieh M1.1 nach") — dafür ist der Skill modul-einbauen zuständig.
---

# Modul erstellen

Ein Modul entsteht nicht aus dem Nichts, sondern aus Code, der sich in einer
echten App bereits bewährt hat. Dieser Skill hebt so einen Bereich heraus,
ohne ihn dabei zu verschlimmbessern.

**Ablageort:** `~/proggs/Module/<Plattform>/<Ordnername>/src/`
(`<Ordnername>` = `Mx.y-Anzeigename`, werkzeugfest gemacht — siehe Phase 1.)
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
beantwortet ein `diff` die Frage „ist diese App aktuell?" ohne Nachdenken.
Genau darauf baut `modul-einbauen` beim Nachziehen auf: Es vergleicht zuerst und
überschreibt nur, wenn die Kopie unverändert ist — sonst hält es an. Ohne die
Byte-Identität wäre dieser Schutz nicht möglich.

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

**Ein Sonderfall ist keine Nabelschnur:** Zeigt ein Import auf
`de.frank.module.*`, benutzt der Bereich ein **anderes Modul** aus der
Bibliothek. Das wird nicht durchgeschnitten, sondern unter `Braucht Module:`
ins Manifest eingetragen. `modul-einbauen` sorgt dann dafür, dass es vorher in
der Ziel-App liegt. Bleibt das Feld leer, ist das Modul eigenständig — der
Normalfall und das Ziel.

### Phase 3 — Herauslösen

Lege `Module/<Plattform>/<Ordnername>/src/` an und **kopiere** die Dateien
dorthin — noch nicht verschieben. Der alte Code in der Quell-App bleibt
vorerst stehen und wird erst in Phase 5 entfernt, wenn die Bibliotheksfassung
vollständig ist. So gibt es zu keinem Zeitpunkt einen Stand ohne beides.
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
- **Braucht Module** — andere Module aus der Bibliothek, auf die dieses
  aufbaut. Meist leer, und das ist auch das Ziel: Ein eigenständiges Modul
  lässt sich überall einsetzen. Baut es doch auf einem anderen auf, muss das
  hier stehen, sonst bricht der Einbau mit „unresolved reference".
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
3. **Anbindungsdatei anlegen**, sobald es überhaupt etwas App-Eigenes gibt —
   `Anbindung.<ext>` neben der Kopie, mit den Theme-Werten und Texten, die der
   Bereich vorher fest verdrahtet hatte. Vorlage:
   `modul-einbauen/assets/anbindung-vorlage.md`.
   Dadurch ist die Quell-App ein ganz normaler Konsument und beim Nachziehen
   kein Sonderfall. Deckt das Modul alles über Vorgabewerte ab, bleibt die
   Datei weg — eine leere Anbindung anzulegen wäre nur Ballast.
4. Importe an den Aufrufstellen auf den neuen Namensraum ziehen.
5. **Durchbauen.** Erst wenn die Quell-App grün baut, existiert das Modul.

Bricht der Build, ist das kein Rückschlag, sondern genau die Information, für
die dieser Schritt da ist: Eine Nabelschnur wurde übersehen. Zurück zu Phase 2.

### Phase 6 — Abschluss

- Modulzeile in `Module/INDEX.md` eintragen (Format: `assets/INDEX-zeile.md`).
- Für die **Quell-App** gilt Regel 9 vollständig: bauen, Version bumpen mit
  echter Systemzeit, committen, pushen, auf dem Gerät installieren.
- Das Modul selbst hat keinen eigenen Build und keine App-Version — sag das
  ausdrücklich, statt den Schritt stillschweigend zu überspringen.
- **Ein Commit** über alles: Modulordner, `INDEX.md` und Quell-App. Sie gehören
  zusammen; getrennt committet gäbe es einen Stand, in dem die App auf ein Modul
  verweist, das es noch nicht gibt — oder einen Index, der ein Modul listet, das
  im Repo fehlt.

## Modul-Versionen

Die Version ist eine ganze Zahl: `v1` beim Anlegen, `+1` bei jeder Änderung am
Modul.

Zwei Angaben werden leicht verwechselt:

| Angabe | Wo | Bedeutet |
|---|---|---|
| **Modulstand** | Kopf jeder Moduldatei, Kopf von `MODUL.md` | wie weit die Bibliothek ist |
| **Konsumentenstand** | Konsumententabelle, je Zeile | wie weit *diese App* ist |

Beide dürfen auseinanderlaufen — genau das heißt „die App hinkt hinterher".
Steht die Bibliothek auf v4 und eine App auf v3, ist das kein Fehler, sondern
die Arbeitsanzeige für das nächste Nachziehen.

Maßgeblich für den Stand einer App ist immer die **Konsumententabelle**, nie
der Kopf in ihrer Kopie.

---

# Modus „Modul ändern"

Auslöser: „fixe M1.1", „ändere das Modul", „M1.1 soll jetzt auch X können",
„hebe den Fix aus GenialeIdeen ins Modul".

**Jede Änderung an einem Modul passiert in der Bibliothek.** Niemals in einer
App-Kopie — die ist eine Abschrift und wird beim nächsten Nachziehen ersetzt.

### 1. Lage feststellen und nennen

Modul über Nummer oder Anzeigenamen finden, `MODUL.md` lesen und die
Konsumenten **zählen und aussprechen**:

> „M1.1 steht bei v3 und hat 3 Konsumenten — GenialeIdeen, Denknotiz,
>  KarteikartenLernen. Die Änderung betrifft alle drei."

Das ist keine Höflichkeit: Wer nicht weiß, dass er drei Apps anfasst, ändert
leichtfertiger als nötig.

Hat das Modul **keine** Konsumenten, sag auch das — dann ist die Änderung
folgenlos und darf mutiger ausfallen. Das ist der einzige Fall, in dem auch
die Signatur ohne Weiteres umgebaut werden kann.

### 2. Ändern — in der Bibliothek

Kommt der Fix aus einer App (der Abweichungsfall aus `modul-einbauen`),
übernimm den **Unterschied**. Mach nicht die App-Kopie zur neuen Quelle — sie
enthält womöglich noch mehr, das nur für diese App gilt.

### 3. Signaturprüfung — bricht die Änderung die Anbindungen?

Ändern sich öffentliche Funktionen, Parameter, Schnittstellen oder Datentypen?

| | Folge |
|---|---|
| Nur innen geändert | Nachziehen kommt ohne Anbindungsarbeit aus (die Abweichungsprüfung läuft trotzdem) |
| **Signatur geändert** | Jede Anbindung muss angepasst werden |

Im zweiten Fall im Änderungsprotokoll ausdrücklich **„bricht Anbindung"**
vermerken. Genau diese Angabe braucht `modul-einbauen` später, um nicht
blind loszulaufen.

**Abhängige Module mitdenken.** Andere Module können auf diesem aufbauen:

```
grep -lF "M1.1" ~/proggs/Module/*/*/MODUL.md
```

Zwei Stolpersteine dabei:

- **`-F` nicht vergessen.** Ohne das ist der Punkt ein Platzhalter, und `M1.1`
  findet auch `M111`.
- **Das eigene Manifest steht immer mit drin**, weil dort die eigene Nummer in
  der Überschrift steht. Dieser Treffer wird übergangen — gemeint sind nur die
  *anderen* Module.

Echte Treffer gehören ins Änderungsprotokoll: Sie müssen bei einer
Signaturänderung ebenfalls angepasst werden, und zwar **hier**, in diesem
Modus — ein Modul zieht kein anderes Modul nach, das wäre wieder eine
Bibliotheksänderung.

### 4. Version anheben

`vN` → `vN+1`, **gleichzeitig in allen Dateiköpfen und in `MODUL.md`**. Bleibt
eine Datei zurück, zeigt ihr Kopf einen Stand an, den es nicht gibt, und die
Konsumententabelle wird wertlos.

### 5. Änderungsprotokoll

Eine Zeile in `MODUL.md` unter **Änderungen**: Version, echtes Datum, was
geändert wurde, und ob es die Anbindung bricht.

**Trag den Commit-Kurzhash nach**, sobald Schritt 7 committet hat. Das ist
keine Formsache: `modul-einbauen` braucht beim Nachziehen den alten Stand aus
der Historie, um die Abweichungsprüfung zu machen. Ohne den Hash muss er ihn
über die Ordner-Historie suchen — mit ihm ist er in einem Griff da.

### 6. Abschluss — und warum hier nicht gebaut wird

Die Bibliothek hat keinen Build. **Die Änderung ist deshalb erst geprüft, wenn
mindestens ein Konsument nachgezogen wurde.** Sag das klar:

> „v4 steht. 3 Konsumenten sind noch auf v3 — sag ‚zieh M1.1 nach', dann wird
>  es gebaut und geprüft."

Zieh **nicht selbst** nach. Das ist `modul-einbauen`, und der Benutzer
entscheidet, wann seine Apps angefasst werden.

Bei geänderter Beschreibung oder Mindestversion auch die Zeile in `INDEX.md`
anpassen. Den Konsumentenzähler dort **nicht** anfassen — der ändert sich nur
beim Ein- und Ausbau.

### 7. Committen und pushen

Die Änderung liegt bis hierher nur auf der Platte. **Ein Commit** über alles,
was zum Modul gehört — Quelldateien, `MODUL.md`, gegebenenfalls `INDEX.md` —
und anschließend pushen.

Keine App-Version wird dabei gebumpt: Es hat sich kein App-Code geändert, und
die Bibliothek hat keine eigene App-Version. Der Modulstand `vN` **ist** hier
die Version. Sag das ausdrücklich, statt den Bump stillschweigend wegzulassen.

Ohne diesen Schritt steht die neue Fassung auf keinem anderen Rechner zur
Verfügung, und das erste Nachziehen dort zöge auf einen Stand, den es im Repo
gar nicht gibt.

## Was dieser Skill nicht tut

Ein bestehendes Modul in eine weitere App einbauen, oder eine Modul-Änderung an
alle Konsumenten verteilen. Beides ist Sache des Skills `modul-einbauen`. Kommt so eine
Bitte herein, sag das und bau nichts Halbes.
