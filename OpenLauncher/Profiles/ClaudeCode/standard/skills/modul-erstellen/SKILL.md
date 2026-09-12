---
name: modul-erstellen
description: Löst einen gut funktionierenden Bereich aus einer fertigen App heraus und legt ihn als wiederverwendbares Modul unter ~/proggs/Module/<Plattform>/<Ordnername>/ ab — plattformübergreifend für Android (Kotlin/Compose), Windows (C#/WPF), macOS und iOS (Swift/SwiftUI). Nutze diesen Skill IMMER wenn der Benutzer sagt "mach daraus ein Modul", "als Modul abspeichern", "speicher das als Modul", "extrahiere X als Modul", "bau aus App Y das X-Modul", "das gefällt mir, das will ich wiederverwenden", "als M1.x speichern", "neues Modul anlegen", "Modul erstellen", "Modul-Skill", "in die Modul-Bibliothek aufnehmen", "das Drag-and-Drop als Modul", "herauslösen und ablegen". Ebenso bei Sätzen, die gleich einen Namen mitgeben — "bau daraus ein Modul und nenn es X", "erstelle ein Modul mit dem Namen X", "mach daraus das X-Modul", "speicher das als X ab". Auch bei Spracherkennungs-Varianten wie "Zwift" (= Swift), "Dreck-and-Drop" (= Drag-and-Drop) oder "Modul abspeichern aus der App". Dieser Skill pflegt AUSSERDEM bestehende Module — nutze ihn ebenso bei "fixe M1.1", "ändere das Modul X", "M1.1 soll jetzt auch Y können", "bessere das Modul nach", "hebe den Fix aus App Z ins Modul", "neue Modulversion", "Modul-Version anheben", "das Modul hat einen Fehler". Änderungen an einem Modul passieren IMMER in der Bibliothek, nie in einer App-Kopie. Dieser Skill prüft AUSSERDEM die Bibliothek auf stille Drift — nutze ihn bei "prüf die Modul-Bibliothek", "ist bei den Modulen alles sauber", "Modul-Check", "prüf M1.1 durch", "welche Apps hinken bei den Modulen hinterher", "stimmen die Modulstände noch": er meldet abweichende Dateiköpfe, einen veralteten INDEX.md, App-Kopien mit Direktänderungen und Modulteile ohne Aufrufer, ändert dabei aber nichts von allein. Der Skill übernimmt den genannten Namen wörtlich als Anzeigenamen und fragt danach, wenn keiner genannt wurde, vergibt die nächste freie M-Nummer, schneidet die Abhängigkeiten zur Ursprungs-App sauber durch, schreibt das Manifest MODUL.md, verdrahtet die Ursprungs-App auf die Kopie um und baut sie zur Abnahme durch. NICHT nutzen, wenn ein bestehendes Modul in eine App eingebaut werden soll ("bau M1.1 ein", "nimm M1.1 bis M1.7", "Modul einbauen") oder wenn eine Änderung an alle Konsumenten verteilt werden soll ("zieh M1.1 nach") — dafür ist der Skill modul-einbauen zuständig.
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

### Datenformat — die zweite Treueachse

**Schreibt oder liest der Bereich etwas, das den Neustart überlebt?** Dateien,
Datenbankspalten, Einstellungsschlüssel, ein Exportformat. Dann hat das Modul
nicht nur Code-Treue zu wahren, sondern auch **Treue zum gespeicherten Zustand**:
Was auf dem Gerät liegt, muss danach noch lesbar sein.

Das ist eine **Halt-Frage**, keine Mitteilung:

> „Das Sicherungsmodul schreibt Dateien. Liegen auf dem Gerät welche, die
>  danach noch einspielbar sein müssen?"

Warte auf die Antwort. Lautet sie ja, ist das Format ab hier unantastbar —
Feldnamen, Reihenfolge, Schlüsselnamen — und der Abnahmetest heißt nicht „baut
grün", sondern „alte Datei lässt sich einspielen".

Trag die betroffenen Namen ins Manifest unter **Gespeicherter Zustand** ein, mit
dem Hinweis, dass sie unverändert zu übernehmen sind. Ein umbenannter
Einstellungsschlüssel löscht die Einstellung des Benutzers, ohne dass ihm etwas
angezeigt wird.

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

### Den Schnitt finden, nicht Importe zählen

Die Importe zeigen, **dass** gekoppelt ist — nicht, **wo** die Grenze liegt. Wer
nur Importe abarbeitet, zieht am Ende die Datentypen der App ins Modul und hat
eine verschobene App-Datei statt eines Moduls.

Die bessere Frage: **Wo beginnt die app-eigene Form der Daten?**

> Alles davor ist **Umschlag** und gehört ins Modul.
> Alles ab dort ist **Inhalt** und bleibt in der App.

Beim Sicherungsmodul lag die Grenze mitten in einer Datei: Kopf, Prüfsumme und
Fußzeile sind Umschlag; welche Feldnamen ein Satz hat, ist Inhalt. Der erste
Anlauf hatte die Grenze eine Ebene zu tief gezogen und wollte die
Datenbank-Entitäten mitnehmen — das wäre kein Modul geworden.

### Liegt der Bereich in geteiltem Code?

Prüf, ob der Ordner von mehreren Apps über `sourceSets.srcDir` eingebunden wird
(bei diesem Benutzer: `KompassKern` für die drei Kompass-Apps). Dann gilt:

| | |
|---|---|
| Modulkopie und Anbindung | **einmal** in den geteilten Ordner, nicht je App |
| Konsumententabelle | **eine Zeile je App**, alle mit demselben Pfad |
| Abnahme | **alle** beteiligten Apps bauen |

Und sag es dem Benutzer vorher: Ein Fehler trifft hier **alle Apps
gleichzeitig**. Deshalb gilt in Phase 5 zwingend erst bauen, dann den alten Code
entfernen.

### Gibt es schon Tests für den Bereich?

Such danach, bevor du schneidest. Ein vorhandener Test ist der Abnahmetest, den
niemand erst schreiben muss — und bei einem Datenformat der einzige, der die
Treue wirklich beweist. Er wird mitgezogen und auf das Modul umgestellt.

Findest du keinen, vermerk das im Manifest: „Abnahme nur Build, kein Test." Dann
weiß der nächste, worauf er sich nicht verlassen kann.

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

**Halt den Quellstand fest, bevor du die erste Datei anfasst:**

```
git rev-parse --short HEAD
```

Der Hash kommt ins Manifest (Phase 4, Feld `Quellstand`). Er muss **jetzt**
genommen werden, nicht später: Läuft während der Arbeit ein fremder Commit ein
und du notierst den Hash erst hinterher, zeigt der Vergleich in Phase 5 nichts
an — der Schutz wäre genau in dem Fall blind, für den es ihn gibt.

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

**Verschieben, nicht umschreiben.** Das ist die wichtigste Regel dieser Phase.
Der Modulcode entsteht durch **Ausschneiden** aus der Quelldatei. Neue Zeilen
gibt es nur für den Herkunfts-Kopf, für Konstruktorparameter und für den Aufruf
der Rückruf-Grenze. Jede andere Zeile muss sich im Vergleich als *bewegt*
wiederfinden lassen, nicht als *neu*.

Der Grund ist eine Erfahrung aus dem ersten echten Lauf: Beim Sicherungsmodul
wurde eine Leseschleife neu geschrieben, statt den bestehenden Verzweigungsblock
zu behalten und nur die Nutzlast-Zweige nach außen zu geben. Dabei rutschte die
Kopfprüfung ans Ende — die App hätte Sätze einer **fremden** App erst in die
Datenbank geschrieben und danach abgelehnt. Der Build war grün, alle Tests
liefen. Ein Build prüft, **ob** etwas passiert, nicht **wann**.

Muss ein Block wirklich neu geschrieben werden, ist das keine Fleißarbeit,
sondern eine **Frage an den Benutzer** — mit dem Vorschlag, was sich dabei
ändert.

**Reihenfolge der Wirkungen ist Verhalten.** Prüfungen, Abbrüche und
Schreibzugriffe müssen danach in derselben Reihenfolge stehen wie vorher. Geh
die verschobenen Stellen einmal ausdrücklich daraufhin durch: *Was passiert
zuerst, was danach?* Ein Build beantwortet diese Frage nie.

**Die Testbarkeit prüft den Schnitt.** Lässt sich die Anbindung ohne Datenbank
und ohne Android-`Context` bauen? Wenn nicht, ist der Schnitt zu grob — dann
nimmt die Anbindung die Bausteine einzeln entgegen (Quelle, Zähler, Senke)
statt das ganze Repository. Das ist keine Schönheit: Ohne diesen Konstruktor
lässt sich das Dateiformat nicht prüfen, und beim Sicherungsmodul war genau
dieser Test der einzige echte Beweis der Treue.

**Kein Gestaltungssystem im Modul.** Benutzt der Bereich die eigenen Bausteine
der App — `Block`, `Schalterzeile`, eigene Farb-Objekte, ein eigenes Theme —
bleiben die draußen. Das Modul liefert **Zustand und Aktionen**, gezeichnet wird
in der App. Genau das erfüllt die Zusage „gleiche Funktionen, eigenes Aussehen".

### Phase 4 — Manifest

Schreib `MODUL.md` nach der Vorlage `assets/MODUL.md.template`. Die Felder, die
wirklich zählen:

- **Herkunft** — App, Dateipfade, Commit-Hash. Damit ist später nachvollziehbar,
  gegen welchen Stand das Modul geschnitten wurde.
- **Quellstand** — der Hash aus Phase 3, *vor* dem ersten Kopieren genommen.
  Phase 5 prüft damit, ob unterwegs jemand anders dieselben Dateien geändert hat.
- **Gespeicherter Zustand** — Dateinamen, Einstellungsschlüssel und Spalten aus
  der Datenformat-Prüfung in Phase 1. Sie werden in einer bestehenden App
  unverändert übernommen.
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

**0. Hat sich die Quelle unterwegs geändert?** Vergleich den zu Beginn von
Phase 3 genommenen Quellstand mit dem jetzigen:

```
git diff <Quellstand>..HEAD -- <die Pfade aus der Herkunft>
```

Kommt etwas zurück, hat jemand anders — womöglich eine parallel laufende
Sitzung — dieselben Dateien angefasst. Dann **erst klären**, ob deine Kopie den
Fix schon enthält, bevor du den alten Code löschst. Genau das ist im ersten
echten Lauf passiert: Acht Leistungsverbesserungen liefen mitten in der
Herauslösung ein. Nach dem Löschen wäre nicht mehr feststellbar gewesen, ob sie
in der Kopie stecken.

1. Die Moduldateien **byte-identisch** in den App-Baum kopieren, an den in der
   Referenzdatei genannten Ort.
2. **Anbindungsdatei anlegen**, sobald es überhaupt etwas App-Eigenes gibt —
   `Anbindung.<ext>` neben der Kopie, mit den Theme-Werten und Texten, die der
   Bereich vorher fest verdrahtet hatte. Vorlage:
   `modul-einbauen/assets/anbindung-vorlage.md`.
   Dadurch ist die Quell-App ein ganz normaler Konsument und beim Nachziehen
   kein Sonderfall. Deckt das Modul alles über Vorgabewerte ab, bleibt die
   Datei weg — eine leere Anbindung anzulegen wäre nur Ballast.
3. Importe an den Aufrufstellen auf den neuen Namensraum ziehen.
4. **Durchbauen** — und *erst dann* den alten Code in der Quell-App löschen.
   Nie umgekehrt. Bei geteiltem Quellordner (siehe Phase 2) ist das zwingend:
   Ein roter Zwischenstand legt dort alle beteiligten Apps gleichzeitig lahm.
5. **Vorhandene Tests mitziehen, nicht wegwerfen.** Der Test aus Phase 2 wird
   auf das Modul umgestellt und **muss laufen**, bevor abgenommen wird. Er ist
   der einzige Beweis, dass ein Datenformat unverändert geblieben ist. Ihn zu
   löschen, weil er nicht mehr kompiliert, macht die Abnahme wertlos.
6. Bei geteiltem Quellordner: **alle** beteiligten Apps bauen, nicht nur eine.

**Jeder Teil des Moduls muss benutzt werden — von der App oder vom Modul
selbst.** Geh am Ende jede öffentliche Klasse und Funktion durch und such die
Aufrufstelle:

```bash
# Alle Top-Level-Deklarationen des Moduls einsammeln …
grep -hoE "^(class|data class|interface|object|enum class|fun|val) [A-Za-z]+"   <Modulordner>/*.kt | awk '{print $NF}' | sort -u > /tmp/decls.txt

# … und je Name fragen: ruft irgendwer außerhalb der Moduldateien auf?
while read n; do
  c=$(grep -rn --include=*.kt "$n" <App-Quellwurzel>       | grep -v "<Modulordner>/" | grep -c "")
  a=$(grep -c "$n" <Modulordner>/Anbindung.kt)
  [ "$c" -eq 0 ] && [ "$a" -eq 0 ] && echo "OHNE AUFRUFER: $n"
done < /tmp/decls.txt
```

Zwei Dinge dabei beachten, sonst meldet der Lauf lauter Fehlalarme:

- Die **Anbindungsdatei** liegt im Modulordner, gehört aber der App — sie zählt
  als Aufrufer, darf also nicht mit ausgeschlossen werden.
- Die **Testquellen** liegen in einem eigenen Quellbaum; nimm die App-Wurzel
  weit genug, dass sie mit durchsucht werden.
- **Modulintern benutzt zählt auch.** Ein Baustein, den nur der Dienst aufruft,
  ist über den Ablauf mit bewiesen. Gemeint sind Teile, die **niemand** aufruft.

Was übrig bleibt, ist **unbewiesen** — es hat nie laufen müssen. Dann gilt:
entweder die Quell-App darauf umstellen, oder den Teil aus dem Modul nehmen und
beim zweiten Konsumenten bauen, wenn er wirklich gebraucht wird. Etwas
Ungeprüftes liegen zu lassen ist die schlechteste der drei Möglichkeiten — der
nächste Einbau verlässt sich darauf.

> Im ersten echten Lauf fiel genau hier eine ganze Datei durch: Eine
> Steuerungsklasse war beim Herauslösen **neu geschrieben** statt verschoben
> worden, blieb hinter dem ViewModel der App zurück (ein Knopf fehlte, die
> Meldungstexte fehlten) und wurde deshalb nie angeschlossen. Sie sah brauchbar
> aus und wäre beim nächsten Einbau als fertig genommen worden.

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

Den Commit-Kurzhash kannst du danach eintragen, musst es aber nicht — die
Version allein genügt, weil die Commit-Nachricht in Schritt 7 fest vorgegeben
ist und den Stand auffindbar macht.

### 6. Abschluss — und warum hier nicht gebaut wird

Die Bibliothek hat keinen Build. **Die Änderung ist deshalb erst geprüft, wenn
mindestens ein Konsument nachgezogen wurde.** Sag das klar:

> „v4 steht. 3 Konsumenten sind noch auf v3 — sag ‚zieh M1.1 nach', dann wird
>  es gebaut und geprüft."

Zieh **nicht selbst** nach. Das ist `modul-einbauen`, und der Benutzer
entscheidet, wann seine Apps angefasst werden.

**Die Zeile in `INDEX.md` mitziehen.** Der fette Modulstand dort ist jetzt `vN+1`,
und der Klammerzusatz hinter den Konsumenten zeigt an, wer hinterherhinkt:

```
- **M1.1** Sicherung **v4** — … Konsumenten: 3 (3 auf v3)
```

Solange niemand nachgezogen hat, stehen also alle drei auf dem alten Stand — und
genau das soll man beim Überfliegen des Index sehen. Bei geänderter Beschreibung
oder Mindestversion auch diese anpassen. Den Konsumenten**zähler** dagegen
**nicht** anfassen — die Zahl ändert sich nur beim Ein- und Ausbau, nur der
Klammerzusatz wandert.

### 7. Committen und pushen

Die Änderung liegt bis hierher nur auf der Platte. **Ein Commit** über alles,
was zum Modul gehört — Quelldateien, `MODUL.md`, gegebenenfalls `INDEX.md` —
und anschließend pushen.

**Die erste Zeile der Commit-Nachricht hat eine feste Form:**

```
Module: M1.1 v4 — Ruckler beim Randscrollen behoben
```

Also `Module: <Nummer> v<N> — <was>`. Das ist keine Kosmetik: `modul-einbauen`
muss beim Nachziehen den **alten** Bibliotheksstand aus der Historie holen, um
die Abweichungsprüfung zu machen. Mit dieser Form findet er ihn in einem Griff:

```
git log --oneline -- Module/<Plattform>/<Ordnername>/ | grep "M1.1 v3"
```

Ohne die feste Form stehen dort nur fünf gleich aussehende Commits, und niemand
kann sagen, welcher v3 war — dann hält die Abweichungsprüfung an und das
Nachziehen kommt nicht voran. Dasselbe gilt sinngemäß für Phase 6 beim Anlegen:
dort lautet die Zeile `Module: M1.1 v1 — aus GenialeIdeen herausgelöst`,
zusammen mit der Quell-App im selben Commit.

Keine App-Version wird dabei gebumpt: Es hat sich kein App-Code geändert, und
die Bibliothek hat keine eigene App-Version. Der Modulstand `vN` **ist** hier
die Version. Sag das ausdrücklich, statt den Bump stillschweigend wegzulassen.

Ohne diesen Schritt steht die neue Fassung auf keinem anderen Rechner zur
Verfügung, und das erste Nachziehen dort zöge auf einen Stand, den es im Repo
gar nicht gibt.

---

# Modus „Bibliothek prüfen"

Auslöser: „prüf die Modul-Bibliothek", „ist bei den Modulen alles sauber",
„welche Apps hinken hinterher", „Modul-Check", „prüf M1.1 durch".

Die Bibliothek hat keinen Build und keine Tests. Nichts fällt also von allein
auf, wenn etwas auseinanderläuft — ein Dateikopf, der auf einem Stand stehen
blieb, ein Index, der eine alte Zahl nennt, eine App-Kopie, in der jemand direkt
gearbeitet hat. Dieser Modus sucht genau danach.

```bash
bash assets/bibliothek-pruefen.sh          # alle Module
bash assets/bibliothek-pruefen.sh M1.1     # nur eines
```

Das Skript **ändert nichts**. Es meldet vier Arten von Befund:

| Befund | Was dahintersteckt |
|---|---|
| Dateiköpfe weichen vom Manifest ab | eine Version wurde gehoben und eine Datei vergessen |
| `INDEX.md` nennt anderen Stand oder andere Zahl | der Index wurde beim letzten Mal nicht mitgezogen |
| App steht auf dem Stand, weicht aber ab | jemand hat **direkt in der Modulkopie** gearbeitet |
| im Modul selbst nie benutzt | ein Teil ohne Aufrufer — der Sweep aus Phase 5 |

Der dritte ist der wichtigste: Er ist derselbe Fall, den die Abweichungsprüfung
beim Nachziehen abfängt — nur findet ihn dieser Lauf, **bevor** jemand
überschreibt. Dann gilt dasselbe Vorgehen: den Unterschied zeigen, nicht
stillschweigend verwerfen, und den Fix nach Weg 1 zuerst ins Modul heben.

Den vierten Befund nicht blind glauben: Das Skript sieht nur die Moduldateien.
Ein gemeldeter Name kann sehr wohl in einer App aufgerufen werden — such die
Aufrufstelle in den Konsumenten, bevor du etwas entfernst. Findet sich keine,
greift die Regel aus Phase 5.

Zeilen mit `·` statt `⚠` sind kein Befund, sondern die Arbeitsanzeige: Die App
hinkt hinterher, und das ist erlaubt. Wenn der Benutzer sie heben will, sagt er
„zieh nach" — das ist `modul-einbauen`, nicht dieser Skill.

**Reparieren nur auf Ansage.** Der Lauf meldet, der Benutzer entscheidet. Ein
Befund kann der Rest einer bewussten Entscheidung sein, und ein Skript, das
ungefragt Dateiköpfe umschreibt, wäre genau die stille Drift, gegen die es
gebaut wurde.

## Was dieser Skill nicht tut

Ein bestehendes Modul in eine weitere App einbauen, oder eine Modul-Änderung an
alle Konsumenten verteilen. Beides ist Sache des Skills `modul-einbauen`. Kommt so eine
Bitte herein, sag das und bau nichts Halbes.
