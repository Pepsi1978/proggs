---
name: modul-einbauen
description: Baut ein fertiges Modul aus ~/proggs/Module/ in eine App ein — Funktionen 1:1, Aussehen an die Ziel-App angepasst — und verteilt Modul-Änderungen an alle Apps, die das Modul bereits benutzen. Nutze diesen Skill IMMER wenn der Benutzer sagt "bau M1.1 ein", "baue das Modul X ein", "Modul einbauen", "nimm M1.1 bis M1.7 und bau sie ein", "setz das Drag-and-Drop-Modul in App Y ein", "das Modul aus der Bibliothek in die neue App", "benutze Modul Mx.y", "zieh M1.1 nach", "zieh das Modul überall nach", "verteile die Modul-Änderung", "bring App Y auf den neuen Modulstand", "welche Apps hinken beim Modul hinterher". Der Benutzer darf das Modul über die Nummer ODER über seinen Anzeigenamen nennen ("bau das Drag & Drop Modul ein") — beides nachschlagen in Module/INDEX.md. Ebenso wenn ein vorhandener Eigenbau abgelöst werden soll — "ersetz die alte Sortierung durch das Modul", "die App hat sowas schon, ersetz es", "tausch das gegen das Modul aus", "bau das Modul ein und wirf die alte Lösung raus". Der Skill sucht dann aktiv nach einer ähnlichen Eigenumsetzung, prüft VOR dem Ersetzen, ob dabei Nutzerdaten verloren gehen (Datenbank-Schema, Einstellungsschlüssel, Sicherungsdateien, Sortierfelder), und hält an, wenn die Datenform nicht passt. Der Skill prüft Versionsverträglichkeit, kopiert das Modul byte-identisch, erzeugt eine app-eigene Anbindungsdatei für Aussehen und app-spezifische Inhalte, legt vor, was 1:1 übernommen wird und was geklärt werden muss, und nimmt erst nach grünem Build ab. NICHT nutzen, wenn aus bestehendem App-Code erst ein neues Modul entstehen soll ("mach daraus ein Modul", "als Modul abspeichern", "extrahiere X als Modul") oder wenn das Modul selbst geändert werden soll ("fixe M1.1", "ändere das Modul", "M1.1 soll auch X können", "neue Modulversion") — beides gehört zum Skill modul-erstellen, der die Bibliothek besitzt. Dieser Skill verteilt nur, was dort steht.
---

# Modul einbauen

Zwei Aufgaben, ein Skill — beide bewegen Code **aus** der Bibliothek **in** Apps:

| Modus | Auslöser | Was passiert |
|---|---|---|
| **Einbauen** | „bau M1.1 ein" | Modul kommt neu in eine App, Anbindung wird erzeugt |
| **Ablösen** | „ersetz die alte Sortierung durch M1.1" | Eigenbau raus, Modul rein — **erst die Datenprüfung** (Phase 1b) |
| **Nachziehen** | „zieh M1.1 nach" | Bestehende Kopien werden auf den neuen Stand gehoben |

**Bibliothek:** `~/proggs/Module/` · **Index:** `~/proggs/Module/INDEX.md`
Das Gegenstück, das Module *anlegt*, ist `modul-erstellen`.

## Der zentrale Trick: zwei Schichten

Der Benutzer will zwei Dinge, die sich zu widersprechen scheinen — Funktionen
**1:1 identisch**, Aussehen **an die Ziel-App angepasst**. Beides geht, wenn
strikt getrennt wird:

| Schicht | Datei | Wird angefasst? |
|---|---|---|
| **Modulkopie** | `…/module/<kurzname>/*` | **Niemals.** Byte-identisch zur Bibliothek |
| **Anbindung** | `…/module/<kurzname>/Anbindung.<ext>` | Gehört der App, wird frei angepasst |

In die Anbindung kommt alles, was von App zu App verschieden ist: Farben,
Schriften, Abstände, Texte, die Umsetzung von Schnittstellen und die
Aufrufstelle. Die Modulkopie bleibt unberührt.

Der Gewinn: `diff` zwischen Bibliothek und App-Kopie bleibt aussagekräftig —
darauf beruht die Abweichungsprüfung beim Nachziehen — und die App sieht
trotzdem aus wie sie selbst. Würde stattdessen die Modulkopie angepasst, wäre nach dem zweiten
Einbau nicht mehr feststellbar, welche App welchen Stand hat.

> **Beim Nachziehen wird die Anbindung nie überschrieben.** Sie ist das
> Eigentum der App. Nur die Modulkopie wird ersetzt.

## Was in jedem Arbeitsmodus gilt

Der Skill kennt drei Arten von Rückfragen. Nur die erste hält an.

| Art | Beispiele | Im Schnellmodus |
|---|---|---|
| **Halt-Frage** — etwas ist unwiederbringlich | Datenfrage (Phase 1b, Fall C), Abweichungsfrage beim Nachziehen | **wird gestellt, es wird gewartet** |
| **Sachfrage** — es gibt keinen Vorgabewert | Spalte 3 der Klassifikation, mehrdeutiger Modulname | wird gestellt, kurz und mit Vorschlag |
| **Mitteilung** — nur Information | Abhängigkeitsliste, „bricht Anbindung" | sagen und weiterarbeiten |

Der Schnellmodus streicht Rückfragen zur *Arbeitsweise* — er streicht weder
eine Freigabe für etwas Unwiederbringliches noch eine Tatsache, die niemand
erraten kann. Ansonsten gilt er uneingeschränkt: keine Zwischenberichte, keine
Bestätigungen, direkt bauen, installieren, committen, pushen.

### Wenn es schiefgeht

Bleibt die App rot und der Fehler ist nicht in ein, zwei Schritten behoben:
**nicht weiterprobieren.** Melde den roten Stand mit der Fehlermeldung und nenn
den Rückweg:

| Lage | Rückweg |
|---|---|
| Ablösung | `git revert` des Commits aus Schritt 1 der Ablösung |
| Einbau ohne Ablösung | die hinzugefügten Dateien wieder entfernen — es wurde nichts überschrieben |
| Nachziehen | `git revert` des Commits dieser App; die anderen Apps sind nicht betroffen |
| Sammelbestellung | der Sicherungscommit vom Anfang — alle Module fallen gemeinsam zurück; einzelne Ordner von Hand zu entfernen reicht hier nicht |

Die Entscheidung, ob zurückgegangen oder weitergesucht wird, gehört dem
Benutzer.

## Phase 1 — Modul finden und Vorabprüfung

Lies `Module/INDEX.md`. Der Benutzer nennt das Modul über die **Nummer**
(`M1.1`) oder über den **Anzeigenamen** („das Drag & Drop Modul") — beides muss
zum Ziel führen. Ist die Angabe mehrdeutig oder nicht auffindbar, frag nach,
statt das nächstbeste Modul zu nehmen.

Dann `MODUL.md` und `BEISPIEL.md` des Moduls lesen und **fünf Dinge prüfen,
bevor irgendeine Datei kopiert wird**:

1. **Plattform passt?** Ein M1-Modul (Kotlin) kann nicht in eine WPF-App. Wenn
   der Benutzer das will, ist das eine Portierung, kein Einbau — sag es und
   halt an.
2. **Mindestversion erfüllt?** `Mindestens:` aus dem Manifest gegen die
   Ziel-App prüfen (Compose-BOM, Zielframework, Deployment-Target). Liegt die
   App darunter, bricht der Build erst beim Kompilieren mit einer
   irreführenden Meldung — lieber jetzt melden.
3. **Kollision?** Existiert `…/module/<kurzname>/` in der Ziel-App schon? Dann
   ist das kein Einbau, sondern ein Nachziehen — wechsle den Modus, aber **nur
   für diese eine App**. Der Benutzer hat nach einem Einbau gefragt, nicht nach
   einer Verteilung an alle Konsumenten; sag ihm, dass das Modul schon liegt
   und du es stattdessen auf den aktuellen Stand hebst.
   Liegt dort **keine** Modulkopie, geht es nach diesen Prüfungen in Phase 1b
   weiter — dort wird gesucht, ob die App die Sache bereits selbstgebaut hat.
4. **Braucht das Modul andere Module?** Siehe den eigenen Abschnitt direkt
   unter dieser Liste — das ist mehr als ein Blick ins Manifest.
5. **Ist das Modul selbst sauber?** Durchsuch den Modulcode nach Theme- und
   Ressourcenzugriffen — aber unterscheide dabei genau, **wo** sie stehen:

   | Fundstelle | Urteil |
   |---|---|
   | Als **Vorgabewert eines Parameters**, z. B. `farbe: Color = MaterialTheme.colorScheme.surface` | **in Ordnung** — die App kann ihn überschreiben, genau so ist es gedacht |
   | **Fest im Code**, mitten in der Logik oder im Aufbau der Oberfläche | **Abbruch** |
   | `R.string`, `R.color`, `StaticResource`, `Color("…")`, ein CompositionLocal der Ursprungs-App — an **jeder** Stelle | **Abbruch**, diese Bezeichner gibt es in einer fremden App nicht |

   Der Unterschied ist entscheidend: Ein Vorgabewert aus dem Theme ist gutes
   Modulhandwerk und die Grundlage für Spalte 2 der Klassifikation. Würde
   jeder Fund von `MaterialTheme.` zum Abbruch führen, scheiterten genau die
   Module, die richtig gebaut wurden.

   Beim Abbruch: Das ist kein Einbau-Problem, sondern eine übersehene
   Nabelschnur — zurück zu `modul-erstellen`, Modul-Version +1, danach
   einbauen.

### Abhängigkeiten auflösen

Ein Modul kann auf anderen aufbauen. Das Feld `Braucht Module:` im Manifest
nennt sie — aber ein einzelner Blick reicht nicht, denn die genannten Module
können ihrerseits welche brauchen.

**Folge der Kette bis zum Ende.** M1.5 braucht M1.2, M1.2 braucht M1.0 → alle
drei gehören eingebaut. Prüf für jedes, ob es in der Ziel-App schon liegt; nur
die fehlenden kommen dazu.

**Reihenfolge: das Benötigte zuerst.** Also M1.0, dann M1.2, dann M1.5 — nie
nach Nummer, nie in der Reihenfolge, in der der Benutzer sie aufgezählt hat.
Andersherum bricht der Build mit „unresolved reference", und die Ursache steht
in einer Datei, die noch gar nicht existiert.

**Leg die Liste vor, bevor du anfängst:**

> „M1.5 braucht M1.2, und das fehlt in Gedankenspeicher. Ich baue beide ein, M1.2
>  zuerst."

Das ist eine Mitteilung, keine Freigabe — im Schnellmodus also sagen und
weiterarbeiten, nicht warten.

**Kreis in der Kette?** Braucht M1.5 das Modul M1.2 und M1.2 wiederum M1.5,
**brich ab**. Das ist ein Fehler in der Bibliothek, nicht im Einbau: Zwei
Module, die sich gegenseitig brauchen, sind in Wahrheit eines. Melde es und
verweise auf `modul-erstellen` zum Zusammenlegen.

Nennt ein Manifest nichts, ist das Modul eigenständig — der Normalfall.

## Phase 1b — Ähnliches in der App ablösen

Häufiger Fall: Die App hat die Sache **schon**, nur selbstgebaut — eine eigene
Sortierung, eine eigene Sicherung. Der Benutzer will sie dann durch das Modul
ersetzt haben, nicht daneben gestellt. Zwei Umsetzungen desselben nebeneinander
sind der schlechteste aller Zustände.

**Such aktiv danach**, bevor du kopierst. Nicht nach dem Namen des Moduls
suchen — die App nennt ihre Fassung anders. Such nach der *Fähigkeit*: bei
einem Sortiermodul nach Ziehgesten und Reihenfolge-Feldern, bei einem
Sicherungsmodul nach Export, Sicherung, Zip, Datei schreiben.

| Gefunden | Vorgehen |
|---|---|
| Nichts | normaler Einbau, weiter mit Phase 2 |
| **Eigene, ähnliche Umsetzung** | **Ablösung — erst die Datenprüfung unten** |

(Eine exakte Modulkopie ist hier nicht mehr möglich — die hätte schon Prüfung 3
abgefangen.)

### Die Datenprüfung — vor jeder Ablösung

**Verlorener Code ist kein Problem: Git holt ihn zurück. Verlorene Nutzerdaten
sind endgültig** — die liegen auf dem Gerät, nicht im Repo. Genau darum geht es
hier.

Stell fest, welchen gespeicherten Zustand die alte Umsetzung besitzt:

| Wo | Woran erkennbar | Gefahr |
|---|---|---|
| Datenbank | `@Entity`-Felder, Migrationen, Spaltennamen | Schema passt nicht → Daten weg |
| Einstellungen | `SharedPreferences`, `UserDefaults`, `Settings` | Anderer Schlüsselname → Wert stillschweigend verloren |
| Dateien | Export-, Sicherungs- oder Zwischenstandsdateien | Altes Format nicht mehr lesbar |
| Sortierung | Reihenfolge-Feld (`position`, `sortIndex`) | Anderer Name oder Zählbeginn → Reihenfolge zerschossen |

Daraus folgt genau eines von drei Ergebnissen:

**A — Kein gespeicherter Zustand.** Ablösen ohne Umstände. Sag es kurz dazu,
damit klar ist, dass geprüft wurde.

**B — Zustand vorhanden, Form passt.** Gleiche Schlüssel, gleiche Spalten,
gleiche Bedeutung. Ablösen, Daten laufen weiter. Sag ausdrücklich, was
weiterläuft.

**C — Zustand vorhanden, Form passt nicht.** **Halt an.** Nicht ersetzen, nicht
„erstmal probieren". Melde genau das:

> ⚠️ In BestJournal liegt die Reihenfolge als `position` (ab 1) in der
> Datenbank, das Modul erwartet `sortIndex` (ab 0). Ohne Umstellung ist die
> Sortierung aller 340 Einträge nach dem Update verloren.
>
> Drei Wege:
> 1. Umstellung mitschreiben (empfohlen) — Daten bleiben, etwas mehr Aufwand
> 2. Vorher sichern und danach neu sortieren
> 3. Abbrechen und die eigene Umsetzung behalten
>
> Wie soll ich vorgehen?

Fällt die Wahl auf Weg 1: **Die Umstellung gehört der App, nicht dem Modul.**
Eine Datenbank-Migration, ein Umschreiben von Einstellungsschlüsseln, ein
Umrechnen alter Werte — all das kommt in den App-Code oder in die Anbindung,
niemals in die Modulkopie. Das Modul kennt die Vergangenheit dieser einen App
nicht und darf sie auch nicht kennen, sonst schleppt es sie in jede weitere
App mit.

**Warte auf die Antwort.** Das ist die eine Stelle in diesem Skill, an der
nicht weitergearbeitet werden darf, weil ein falscher Schritt nicht
zurückholbar ist.

### Zwei Fallen, die dazugehören

- **Zerstörende Datenbank-Umstellung.** Ein geändertes Schema ohne Migration
  löscht auf Android die Datenbank restlos, wenn
  `fallbackToDestructiveMigration` gesetzt ist — ohne Fehlermeldung, ohne
  Nachfrage. Vor jeder Ablösung prüfen, ob das in der App steht.
- **Neu installieren ist keine Lösung.** Meldet `adb` nach dem Einbau
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, wird **nie** deinstalliert — das
  löscht alle Daten des Benutzers. Stattdessen die Signatur rotieren, siehe
  `best-practices/android/debug-signing.md`.

### Ablösen

Ist die Datenfrage geklärt:

1. **Zuerst committen**, was da ist — der alte Stand muss in der Historie
   liegen, bevor er verschwindet. Ein Rückzieher braucht keinen Mut, wenn der
   Commit existiert.
2. Modul einbauen: Phase 2 und 3 (Klassifikation samt Gerüstliste, Kopieren,
   Anbinden, Aufrufstelle, Oberfläche) — **noch nicht bauen**.
3. **Alten Code restlos entfernen** — Datei, Aufrufstellen, jetzt unbenutzte
   Hilfsfunktionen, verwaiste Zeichenketten. Eine zurückgelassene zweite
   Fassung wird später versehentlich weitergepflegt.
4. **Erst jetzt Phase 4** — bauen. Die Reihenfolge ist wichtig: Solange beide
   Fassungen nebeneinander liegen, kann der Build an doppelten Bezeichnern
   oder mehrdeutigen Aufrufen scheitern, und man sucht den Fehler im neuen
   Modul statt im alten Rest.
5. **Weiter mit Phase 5** — Buchführung und Abschluss. Es gibt nur *einen*
   Abschluss-Commit, den aus Phase 5; hier wird keiner zusätzlich gemacht.
   In dessen Nachricht ausdrücklich nennen, was abgelöst wurde und was mit den
   Daten geschehen ist.

## Phase 2 — Klassifikation vorlegen

Das ist der Kern. Teile alles, was das Modul braucht, in drei Spalten und **leg
die Tabelle dem Benutzer vor, bevor du schreibst**:

| Einstufung | Woran erkennbar | Behandlung |
|---|---|---|
| **1:1 übernehmen** | die gesamte Logik; **der Aufbau der Bedienung**; Parameter mit sinnvollem Vorgabewert | ohne Rückfrage übernehmen |
| **Aussehen aus der App** | Farbe, Schrift, Größe, Abstand, Radius, Animationsdauer — und **womit** gezeichnet wird | ohne Rückfrage aus dem Theme und den Bausteinen der Ziel-App |
| **App-eigen — klären** | Schnittstellen, Listen, Datentypen ohne sinnvollen Vorgabewert; alles unter „Host muss liefern" | **nachfragen**, mit Vorschlag |

Die Trennlinie zwischen Spalte 2 und 3 ist praktisch: **Aussehen hat immer einen
guten Vorgabewert** — das Theme der Ziel-App. Danach muss niemand gefragt
werden. **Inhalt hat keinen.**

**Beispiel Sicherungsmodul.** Der Ablauf ist überall gleich: Menü aufziehen,
Einträge anhaken, Fortschritt zeigen, Fehler behandeln → Spalte 1. Kartenfarbe
und Schrift → Spalte 2. Aber *welche Daten* eine App überhaupt sichern kann,
weiß nur diese App → Spalte 3, und das Modul nimmt sie über eine Schnittstelle
entgegen, die in der Anbindung umgesetzt wird.

**Code der Modulkopie, den diese App nicht braucht, bleibt trotzdem liegen.**
Nichts herauskürzen — der Compiler entfernt ungenutzten Code ohnehin, und jede
Kürzung würde die Byte-Identität zerstören.

Frag nur nach Spalte 3 — und nicht als offene Frage, sondern mit Vorschlag:

> „Was soll die App sichern? Ich sehe Notizen, Kategorien und Einstellungen —
>  nehme ich alle drei?"

### Der Aufbau gehört zur Funktion, nicht zum Aussehen

Das ist die Stelle, an der die Klassifikation am häufigsten falsch gelesen
wird. „Aussehen aus der App" verführt dazu, die Oberfläche frei neu zu
erfinden — und heraus kommt ein Bereich, der zwar dieselben Knöpfe hat, aber
anders aufgebaut ist als überall sonst. Genau das will der Benutzer nicht: Er
will das Modul **wiedererkennen** und trotzdem seine App sehen.

**Gleiches Gerüst, eigene Bausteine.**

| Gehört zum Gerüst → Spalte 1 | Gehört zum Baustein → Spalte 2 |
|---|---|
| **welche** Bedienelemente es gibt | mit welcher Komponente sie gezeichnet werden |
| ihre **Reihenfolge** von oben nach unten | Schriftgröße, Farbe, Radius, Höhe |
| was **nebeneinander** in einer Zeile steht | wie breit die Zeile ist, welcher Abstand dazwischen |
| was nur **unter einer Bedingung** erscheint | wie es ein- und ausblendet |
| was **von allein** dasteht statt auf Knopfdruck | ob es fett, gedämpft oder farbig dasteht |
| welcher Text **welche Rolle** hat (Erklärung am Schalter, Warnung, Stand) | wie dieser Text formuliert und gesetzt ist |

Daraus folgen drei harte Regeln:

1. **Was in der Vorlage steht, steht auch hier — an derselben Stelle, in
   derselben Zeile.** Stehen dort „Jetzt sichern", „Ordner wählen" und „Ordner
   vergessen" nebeneinander in einer umbrechenden Zeile, dann stehen sie auch
   in der Ziel-App nebeneinander in einer umbrechenden Zeile, nicht
   untereinander.
   **Zusätzliches darf dazu**, wenn diese App es wirklich braucht — ans Ende
   derjenigen Zeile, zu der es sachlich gehört, sonst hinter das Gerüst. Nie vor
   ein Element der Vorlage und nie an dessen Stelle. Ein Extraknopf am Ende
   einer Zeile stört das Wiedererkennen nicht; einer zwischen zwei Knöpfen der
   Vorlage schon.
2. **Was die Vorlage von allein anzeigt, bekommt keinen Knopf.** Steht dort
   „Nächste Sicherung: 12 Einträge" einfach als Zeile, dann ist ein Knopf
   „Umfang zeigen" kein gleichwertiger Ersatz, sondern ein anderer Aufbau.
3. **Erklärtexte haben Anzahl, Platz und Rolle aus der Vorlage — es kommt
   keiner dazu.** Der Wortlaut darf in die Tonlage der Ziel-App, das ist
   Spalte 2. Aber was dort am Schalter steht, bleibt am Schalter, und ein
   einleitender Absatz, den die Vorlage nicht hat, kommt nicht dazu: Er bläht
   den Bereich auf und erklärt, was der Schalter daneben schon sagt.

**Ein Knopf der Ziel-App bleibt derselbe Knopf.** Er wird mit deren Komponente
gezeichnet und wächst oder schrumpft mit deren Schriftgröße, Abständen und
Tippflächen — aber er behält Beschriftung, Platz und Bedingung aus der Vorlage.

### Wo steht das Gerüst, wenn das Modul keine Oberfläche hat?

Viele Module enthalten bewusst kein Compose, kein XAML, kein SwiftUI — sie
bieten nur den Dienst an und lassen die Bedienung der App. Dann ist die in
`BEISPIEL.md` genannte **Referenz-Umsetzung der Quell-App verbindlich**, nicht
bloß eine Anregung. Sie ist der einzige Ort, an dem das Gerüst überhaupt steht.

Nennt `BEISPIEL.md` keine Referenz-Umsetzung, ist die Ursprungs-App aus dem
Herkunfts-Kopf der Modulkopie die Vorlage — aus ihr wurde das Modul
herausgelöst, dort steht die Bedienung noch. Gibt es auch die nicht, gibt es
kein Gerüst: Das ist eine Sachfrage, kurz und mit Vorschlag, bevor eine Zeile
Oberfläche entsteht.

**Lies sie und schreib sie als Gerüstliste ab, bevor du eine Zeile Oberfläche
baust** — Element für Element, in der Reihenfolge der Vorlage. Die Liste gehört
zusammen mit der Klassifikationstabelle vorgelegt; sie ist eine Mitteilung, kein
Warten. Für M1.1 sieht sie so aus — für jedes andere Modul entsteht sie neu aus
dessen eigener Vorlage:

```
1. Mehrfachauswahl "Was gesichert wird" — je Punkt Titel + Erklärung
2. Hinweiszeile, was NICHT in der Sicherung steckt
3. Schalter "Von allein sichern" + Erklärung darunter
   (Erklärung wechselt, solange kein Ordner gewählt ist)
4. Ordnername, gedämpft solange keiner gewählt ist
5. Zeile: Haken (nur wenn geprüft) + Stand der letzten Sicherung
6. Zeile "Nächste Sicherung: …" — steht von allein da, kein Knopf
7. Knopfzeile, umbrechend: Jetzt sichern · Ordner wählen · Ordner vergessen
   (der letzte nur, wenn ein Ordner gemerkt ist)
8. Knopfzeile, umbrechend: Wiederherstellen · Neueste wiederherstellen
9. Rückgängig-Zeile — nur nach einem Einspielen
10. Auswahlliste "Welche Sicherung?" + Abbrechen — nur wenn aufgezogen
```

Diese Liste ist danach die Prüfliste aus Phase 4. Ohne sie merkt niemand, dass
Punkt 6 fehlt und dafür ein Knopf dasteht, den es nirgends sonst gibt.

**Etwas weglassen ist erlaubt — aber nur bewusst und mit Ansage.** Kann die
Ziel-App einen Punkt nicht (es gibt keine Rücknahme, also auch keine
Rückgängig-Zeile), fällt er weg, und das wird in der Abschlussmeldung genannt.
Still verschwinden darf nichts.

### Aufklappen, Listen und andere Behälter

Zeigt die Vorlage eine Auswahl **eingebettet** — die Liste erscheint im selben
Bereich, sobald man sie aufzieht, und verschwindet wieder —, dann wird sie auch
in der Ziel-App eingebettet gezeigt. Kein Dialog, kein zweites Klappmenü im
Klappmenü, kein Dateiwähler des Betriebssystems **an dieser Stelle**: Das sind
andere Aufbauten mit anderem Verhalten beim Zurückgehen, und genau daran merkt
der Benutzer, dass hier etwas nachgebaut statt übernommen wurde. Was die Vorlage
selbst über einen Systemdialog löst — etwa „Ordner wählen" —, bleibt
selbstverständlich ein Systemdialog.

Umgekehrt gilt das auch für den Rahmen: Steckt der ganze Bereich in der
Ziel-App schon in einem Klappblock, während er in der Vorlage offen liegt, ist
das **kein** Verstoß — der Rahmen gehört der App, das Innere der Vorlage. Nur
darf der App-Rahmen dieses Innere nicht umsortieren.

## Phase 3 — Kopieren und anbinden

1. **Modulkopie** byte-identisch an den Ort aus der Plattform-Referenz legen.
   Nichts umbenennen, nichts kürzen, den Herkunfts-Kopf mitnehmen.
2. **Anbindung** nach `assets/anbindung-vorlage.md` schreiben: Theme-Werte der
   **Ziel-App** einsetzen, Schnittstellen aus Spalte 3 umsetzen, Texte in der
   Sprache und Tonlage der Ziel-App.
3. **Aufrufstelle** einbauen — `BEISPIEL.md` des Moduls ist die Vorlage, aber
   der Aufruf gehört an die Stelle, an der er in *dieser* App Sinn ergibt.
4. **Oberfläche nach der Gerüstliste bauen**, Punkt für Punkt in der Reihenfolge
   der Vorlage — gezeichnet mit den Bausteinen der Ziel-App. Fehlt der Ziel-App
   eine Entsprechung (kein Schalter, keine umbrechende Knopfzeile), nimm den
   nächstliegenden eigenen Baustein; erfinde keinen anderen Aufbau.

Plattform-Eigenheiten stehen in `references/` — vor dem Kopieren die passende
lesen:

| Kreis | Datei |
|---|---|
| M1 Android | `references/android.md` |
| M2 Windows | `references/windows.md` |
| M3 macOS und M4 iOS | `references/macos.md` (deckt beide ab) |

Bei Xcode ist die Zielmitgliedschaft der häufigste Stolperstein, bei WPF ein
ausdrückliches `<Compile Include=…>`.

## Phase 4 — Abnahme

Drei Prüfungen, alle zwingend:

1. **`diff` zwischen Bibliothek und App-Kopie ist leer** — **ohne** die
   Anbindungsdatei, die es in der Bibliothek gar nicht gibt:

   ```
   diff -r <Bibliothek>/src/… <App>/…/module/<kurzname> --exclude=Anbindung.*
   ```

   Ohne diese Ausnahme meldet der Vergleich immer einen Unterschied, und die
   Prüfung wäre wertlos. Ist er darüber hinaus nicht leer, hast du am falschen
   Ort angepasst — die Änderung gehört in die Anbindung.
2. **Die Gerüstliste aus Phase 2 stimmt** — sofern die Oberfläche hier entstand;
   bringt das Modul seine eigene mit, gibt es keine Liste. Geh sie Punkt für Punkt durch und
   halte jeden gegen das, was du gebaut hast: vorhanden, an derselben Stelle,
   in derselben Zeile, unter derselben Bedingung. Ein grüner Build sagt darüber
   nichts — eine Oberfläche, die anders aufgebaut ist, kompiliert tadellos.
   Weicht etwas ab, ist das zu beheben und nicht zu begründen; einzige Ausnahme
   ist ein bewusst weggelassener Punkt, und der gehört in die Abschlussmeldung.
3. **Die Ziel-App baut grün.** Vorher gilt der Einbau nicht als erledigt.

## Phase 5 — Buchführung und Abschluss

- **`MODUL.md`**: Zeile in der Konsumententabelle ergänzen — App, Stand, Pfad
  der Kopie. Ohne diesen Eintrag findet das spätere Nachziehen die App nicht.
- **`INDEX.md`**: Konsumentenzähler erhöhen und den Klammerzusatz nachführen —
  stehen danach alle Apps auf dem Bibliotheksstand, heißt er `(alle auf vN)`,
  sonst `(2 auf v3)` mit der Zahl der Nachzügler. Beim **Nachziehen** ändert
  sich nur der Klammerzusatz, nicht der Zähler.
- **Regel 9 für die Ziel-App**: bauen, Version bumpen mit echter Systemzeit,
  committen, pushen, installieren.
- **Ein Commit** für App, `MODUL.md` **und** `INDEX.md` zusammen — die
  Buchführung darf nicht getrennt von dem stehen, was sie beschreibt.

**Die Commit-Regel in einem Satz:** Die Buchführung wandert mit der App. Beim
Einbau ist das ein Commit (App + neue Zeile in der Konsumententabelle), beim
Nachziehen einer pro App (App + die gehobene Zeile *dieser* App). So ist jeder
Commit für sich zurücknehmbar, und `MODUL.md` sagt zu keinem Zeitpunkt etwas
Falsches.

### Wer sagt die Wahrheit, wenn zwei Angaben sich widersprechen

Denselben Sachverhalt gibt es an mehreren Stellen. Damit nie geraten werden
muss, gilt eine feste Rangfolge:

| Frage | Maßgeblich | Nachrangig |
|---|---|---|
| Auf welchem Stand ist App X? | **Konsumententabelle** in `MODUL.md` | der `Stand vN` im Dateikopf |
| Wie viele Konsumenten hat das Modul? | **Konsumententabelle** | der Zähler in `INDEX.md` |
| Hinkt eine App hinterher? | **Konsumententabelle** | der Klammerzusatz in `INDEX.md` |

Der Dateikopf ist Bequemlichkeit für den Lesenden, nicht die Buchführung.
Weichen beide voneinander ab, wird die **Tabelle** korrigiert und nachgezogen —
nie umgekehrt.

Der Zähler in `INDEX.md` wird aus der Tabelle abgeleitet und nur beim **Ein-
und Ausbau** angefasst. **Nachziehen ändert ihn nie** — die Anzahl der
Konsumenten bleibt dabei ja gleich.

---

# Modus „Nachziehen"

Auslöser: „zieh M1.1 nach", „zieh M1.1 nach in GenialeIdeen", „verteile die
Änderung". Ohne Zusatz sind **alle** Konsumenten gemeint.

### Ablauf

1. **Konsumententabelle lesen** — sie ist die verbindliche Liste, wer das Modul
   hat und auf welchem Stand.
2. **Änderungsprotokoll lesen.** Steht bei einer der übersprungenen Versionen
   **„bricht Anbindung"**, muss jede Anbindung angepasst werden — das ist dann
   die eigentliche Arbeit, nicht das Kopieren. Ebenso prüfen, ob
   `Host muss liefern` gegenüber dem Stand der App länger geworden ist. Nenn
   beides **vorher**, statt es später als Baufehler zu melden.
   Und prüf, ob **`Braucht Module` gewachsen** ist: Stützt sich das Modul seit
   der neuen Fassung auf ein weiteres Modul, muss dieses in jeder Konsumenten-App
   zuerst eingebaut werden — sonst bricht der Build dort mit „unresolved
   reference", obwohl an der App nichts falsch ist.
3. **Abhängige Module prüfen** (siehe unten) — sie kommen vor den Apps.
4. **Je App:** Abweichungsprüfung (siehe unten) → nur die Modulkopie
   überschreiben, Anbindung unangetastet lassen → bauen → Regel 9 → **ein
   Commit**, der auch die Zeile *dieser* App in der Konsumententabelle hebt.
   Nicht am Ende gesammelt, sonst behauptet `MODUL.md` zwischendurch etwas
   Falsches und ein einzelner Rückzieher nimmt die Buchführung nicht mit.
5. **Abschließend melden**, welche Apps gehoben wurden, welche auf eine
   Rückfrage warten und welche unverändert blieben. Eine still übersprungene
   App hinkt sonst monatelang hinterher, ohne dass es jemand merkt.

### Mehrere Zeilen mit demselben Pfad

Stehen in der Konsumententabelle mehrere Apps mit **identischem Pfad**, teilen
sie sich den Quellordner (`sourceSets.srcDir`; bei diesem Benutzer `KompassKern`
für die drei Kompass-Apps). Dann gilt Schritt 4 **je Pfad**, nicht je App:

| | |
|---|---|
| Abweichungsprüfung | einmal |
| Überschreiben | einmal |
| Bauen und installieren | **jede** dieser Apps |
| Konsumententabelle | **alle** diese Zeilen heben |
| Commit | **einer** für die ganze Gruppe |

Sonst wird dieselbe Datei dreimal überschrieben — die zweite Abweichungsprüfung
schlägt an, weil sie die eigene Arbeit von eben sieht — oder es wird nur eine
App gebaut, während die anderen mit demselben Code ungeprüft bleiben.

Die Regel „die Buchführung wandert mit der App" bleibt damit unangetastet: Sie
gilt je **Kopie**, und hier gibt es nur eine.

### Abhängige Module kommen zuerst

Bricht die Signatur, sind womöglich nicht nur Apps betroffen, sondern auch
**andere Module**, die auf diesem aufbauen. Das Änderungsprotokoll nennt sie.

Diese Module kann dieser Skill **nicht** reparieren — eine Änderung an einem
Modul gehört in die Bibliothek und damit zu `modul-erstellen`. Also:

> „M1.2 bricht die Anbindung, und M1.5 baut darauf auf. M1.5 muss zuerst in der
>  Bibliothek nachgezogen werden — das macht `modul-erstellen`. Erst danach
>  ergibt das Verteilen an die Apps Sinn."

Wird trotzdem zuerst an die Apps verteilt, bauen genau die Apps nicht mehr, die
beide Module haben — und die Ursache liegt in einem dritten Ort.

### Abweichungsprüfung — der Code-Zwilling der Datenprüfung

Die Regel sagt, dass niemand direkt in einer Modulkopie arbeitet. Es wird
trotzdem passieren — jemand fixt schnell etwas an Ort und Stelle. Blindes
Überschreiben wirft diesen Fix weg, und niemand merkt es.

Deshalb **vor** dem Überschreiben vergleichen: App-Kopie gegen den
Bibliotheksstand **der Version, die die App laut Konsumententabelle hat** (nicht
gegen den neuen — sonst siehst du nur die Modul-Änderung selbst). Die
Anbindungsdatei bleibt dabei außen vor, wie in Phase 4.

Den alten Stand holst du aus der Historie — die Bibliothek liegt im selben
Repo, er ist also immer greifbar. `modul-erstellen` schreibt jeden Modul-Commit
in der festen Form `Module: <Nummer> v<N> — <was>`, deshalb ist der Stand
eindeutig auffindbar:

```bash
git log --oneline -- Module/<Plattform>/<Ordnername>/ | grep "M1.1 v3"

# Den alten Stand NEBEN den Baum legen, nie in ihn hinein:
ALT=$(mktemp -d)
git ls-tree --name-only <Commit>:Module/<Plattform>/<Ordnername>/src/<Paketpfad>   | while read f; do
      git show <Commit>:Module/<Plattform>/<Ordnername>/src/<Paketpfad>/$f > "$ALT/$f"
    done

diff -r --exclude="Anbindung.*" "$ALT" <Pfad der App-Kopie>
```

Steht im Änderungsprotokoll schon ein Commit-Hash, nimm ihn direkt — dann
sparst du dir die Suche.

> ⚠️ **Niemals `git stash`, `git checkout <commit>` oder `git restore` benutzen,
> um den alten Stand herzustellen.** Alle drei schreiben in den Arbeitsbaum: Sie
> machen deine noch nicht committete Arbeit unsichtbar, und beim Zurückholen
> normalisiert Git die Zeilenenden — danach meldet `diff` **jede Zeile jeder
> Datei** als geändert, und die Prüfung ist wertlos. `git show` schreibt nichts
> und lässt den Arbeitsbaum in Ruhe. (Genau dieser Fehler ist beim ersten echten
> Nachziehen passiert.)

Kommt der Vergleich trotz `git show` mit lauter Vollzeilen-Unterschieden zurück,
sind es fast immer die Zeilenenden. Dann `diff --strip-trailing-cr` nehmen und
das Ergebnis erneut ansehen, statt einen Fix zu vermuten, den es nicht gibt.

Findest du den Stand ausnahmsweise nicht zweifelsfrei — etwa bei einem Modul
aus der Zeit vor dieser Regel —, **rate nicht**. Dann ist die
Abweichungsprüfung nicht durchführbar, und das ist selbst ein Grund
anzuhalten und nachzufragen.

| Ergebnis | Vorgehen |
|---|---|
| Kein Unterschied | überschreiben, alles gut |
| **Unterschied** | **anhalten** |

Bei einem Unterschied den Unterschied **zeigen** und zur Wahl stellen:

> ⚠️ Die Kopie in GenialeIdeen weicht vom Stand v3 ab — dort wurde direkt in
> der Modulkopie etwas geändert (3 Zeilen in `settle()`).
>
> 1. Diese Änderung zuerst ins Modul heben (empfohlen) — `modul-erstellen`
>    macht daraus v5, danach ziehe ich überall nach
> 2. Verwerfen und mit dem Bibliotheksstand überschreiben
>
> Wie soll ich vorgehen?

**Niemals stillschweigend verwerfen.** Vorher zu committen schützt den Text,
aber nicht das Wissen, dass es diesen Fix überhaupt gab — und danach sucht ihn
niemand mehr.

Der Fall „App hat lokal gefixt **und** die Bibliothek ist weitergezogen" ist
kein Sonderfall, sondern genau Weg 1: erst den Fix hochheben, dann nachziehen.
Ein Drei-Wege-Abgleich ist dafür nicht nötig, nur das Anhalten.

**Wenn eine App nach dem Überschreiben nicht mehr baut**, ist das die
Information, für die dieser Ablauf existiert: Die Modulsignatur hat sich
geändert und die Anbindung dieser App passt nicht mehr. Repariere die
**Anbindung**, niemals die Modulkopie.

## Wenn der Benutzer mehrere Module nennt

**Erst alle prüfen, dann bauen.** Führ Phase 1 und 1b für **alle** genannten
Module durch, sammle sämtliche Rückfragen und stell sie **in einem Zug**. Sonst
muss der Benutzer siebenmal hintereinander etwas entscheiden, jedes Mal mit
Wartezeit dazwischen.

**Der Sicherungscommit vor einer Ablösung wird nur einmal gemacht**, ganz am
Anfang, auch wenn mehrere der Module einen Eigenbau ablösen. Er sichert den
Stand der App vor dem ganzen Arbeitsgang — ein Commit je Ablösung würde der
Regel „ein Commit für die App" widersprechen und die Historie zerfasern.

**Reihenfolge nach Abhängigkeit, nicht nach Nummer.** Braucht M1.5 das Modul
M1.2, kommt M1.2 zuerst — auch wenn der Benutzer sie anders aufgezählt hat. Das
Feld `Braucht Module:` in den Manifesten gibt die Reihenfolge vor.

Danach jedes Modul durch Phase 2 und 3. Phase 4 und 5 laufen **einmal
gemeinsam** am Ende: ein Build, ein Versions-Bump, ein Commit für die App —
in dem dann alle Konsumentenzeilen und alle Zählerstände auf einmal stehen.
Sieben Bumps für einen Arbeitsgang wären Lärm.

Die `diff`-Prüfung aus Phase 4 gilt dabei **je Modul**, nicht einmal pauschal:
Jede Kopie wird einzeln gegen ihre Bibliotheksfassung gehalten.

Bricht ein Modul die Prüfung in Phase 1, überspring **nur dieses** und bau die
übrigen fertig — außer ein anderes Modul hängt davon ab, dann fällt es mit.
Sag am Ende klar, welche fehlen und warum.
