---
name: modul-einbauen
description: Baut ein fertiges Modul aus ~/proggs/Module/ in eine App ein — Funktionen 1:1, Aussehen an die Ziel-App angepasst — und verteilt Modul-Änderungen an alle Apps, die das Modul bereits benutzen. Nutze diesen Skill IMMER wenn der Benutzer sagt "bau M1.1 ein", "baue das Modul X ein", "Modul einbauen", "nimm M1.1 bis M1.7 und bau sie ein", "setz das Drag-and-Drop-Modul in App Y ein", "das Modul aus der Bibliothek in die neue App", "benutze Modul Mx.y", "zieh M1.1 nach", "zieh das Modul überall nach", "verteile die Modul-Änderung", "bring App Y auf den neuen Modulstand", "welche Apps hinken beim Modul hinterher". Der Benutzer darf das Modul über die Nummer ODER über seinen Anzeigenamen nennen ("bau das Drag & Drop Modul ein") — beides nachschlagen in Module/INDEX.md. Ebenso wenn ein vorhandener Eigenbau abgelöst werden soll — "ersetz die alte Sortierung durch das Modul", "die App hat sowas schon, ersetz es", "tausch das gegen das Modul aus", "bau das Modul ein und wirf die alte Lösung raus". Der Skill sucht dann aktiv nach einer ähnlichen Eigenumsetzung, prüft VOR dem Ersetzen, ob dabei Nutzerdaten verloren gehen (Datenbank-Schema, Einstellungsschlüssel, Sicherungsdateien, Sortierfelder), und hält an, wenn die Datenform nicht passt. Der Skill prüft Versionsverträglichkeit, kopiert das Modul byte-identisch, erzeugt eine app-eigene Anbindungsdatei für Aussehen und app-spezifische Inhalte, legt vor, was 1:1 übernommen wird und was geklärt werden muss, und nimmt erst nach grünem Build ab. NICHT nutzen, wenn aus bestehendem App-Code erst ein neues Modul entstehen soll ("mach daraus ein Modul", "als Modul abspeichern", "extrahiere X als Modul") — dafür ist der Skill modul-erstellen zuständig.
---

# Modul einbauen

Zwei Aufgaben, ein Skill — beide bewegen Code **aus** der Bibliothek **in** Apps:

| Modus | Auslöser | Was passiert |
|---|---|---|
| **Einbauen** | „bau M1.1 ein" | Modul kommt neu in eine App, Anbindung wird erzeugt |
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

Der Gewinn: `diff` zwischen Bibliothek und App-Kopie bleibt aussagekräftig,
Nachziehen ist reines Überschreiben — und die App sieht trotzdem aus wie sie
selbst. Würde stattdessen die Modulkopie angepasst, wäre nach dem zweiten
Einbau nicht mehr feststellbar, welche App welchen Stand hat.

> **Beim Nachziehen wird die Anbindung nie überschrieben.** Sie ist das
> Eigentum der App. Nur die Modulkopie wird ersetzt.

## Was in jedem Arbeitsmodus gilt

Dieser Skill stellt genau **zwei** Fragen, bei denen angehalten und gewartet
wird — die Datenfrage (Phase 1b, Fall C) und die Abweichungsfrage beim
Nachziehen. Beide gelten **auch im Schnellmodus**.

Der Schnellmodus streicht Rückfragen zur *Arbeitsweise* — er streicht keine
Freigabe für etwas Unwiederbringliches. Bei allem anderen gilt er
uneingeschränkt: keine Zwischenberichte, keine Bestätigungen, direkt bauen,
installieren, committen, pushen.

### Wenn es schiefgeht

Bleibt die App nach einer Ablösung rot und der Fehler ist nicht in ein, zwei
Schritten behoben: **nicht weiterprobieren.** Der Commit aus Schritt 1 der
Ablösung ist der Rückweg — melde den roten Stand mit der Fehlermeldung und
sag, dass ein `git revert` dieses Commits den alten Zustand zurückholt. Die
Entscheidung, ob zurückgegangen oder weitergesucht wird, gehört dem Benutzer.

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
   ist das kein Einbau, sondern ein Nachziehen — wechsle den Modus.
   Gibt es stattdessen eine **eigene, ähnliche Umsetzung** in der App, geht es
   in Phase 1b weiter.
4. **Braucht das Modul andere Module?** Das Feld `Braucht Module:` im Manifest
   nennt sie. Fehlt eines davon in der Ziel-App, baue es **zuerst** ein —
   sonst bricht der Build mit „unresolved reference", und die Ursache steht in
   einer Datei, die noch gar nicht da ist. Nennt das Manifest nichts, ist das
   Modul eigenständig.
5. **Ist das Modul selbst sauber?** Durchsuch den Modulcode nach direkten
   Theme- und Ressourcenzugriffen (`MaterialTheme.`, `R.string`, `R.color`,
   `StaticResource`, `Color("…")`, ein CompositionLocal der Ursprungs-App).
   Findest du welche, **brich ab**: Das Modul lässt sich nicht per Anbindung
   anpassen, weil das Aussehen fest verdrahtet ist. Das ist kein
   Einbau-Problem, sondern eine übersehene Nabelschnur — zurück zu
   `modul-erstellen`, Modul-Version +1, danach einbauen.

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
| Exakte Modulkopie | Nachziehen, nicht Einbauen |
| **Eigene, ähnliche Umsetzung** | **Ablösung — erst die Datenprüfung unten** |

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
2. Modul einbauen (Phase 2 bis 4).
3. **Alten Code restlos entfernen** — Datei, Aufrufstellen, jetzt unbenutzte
   Hilfsfunktionen, verwaiste Zeichenketten. Eine zurückgelassene zweite
   Fassung wird später versehentlich weitergepflegt.
4. Im Commit ausdrücklich nennen, was abgelöst wurde und was mit den Daten
   geschehen ist.

## Phase 2 — Klassifikation vorlegen

Das ist der Kern. Teile alles, was das Modul braucht, in drei Spalten und **leg
die Tabelle dem Benutzer vor, bevor du schreibst**:

| Einstufung | Woran erkennbar | Behandlung |
|---|---|---|
| **1:1 übernehmen** | die gesamte Logik; Parameter mit sinnvollem Vorgabewert | ohne Rückfrage übernehmen |
| **Aussehen aus der App** | Farbe, Schrift, Abstand, Radius, Animationsdauer | ohne Rückfrage aus dem Theme der Ziel-App setzen |
| **App-eigen — klären** | Schnittstellen, Listen, Datentypen ohne sinnvollen Vorgabewert; alles unter „Host muss liefern" | **nachfragen**, mit Vorschlag |

Die Trennlinie zwischen Spalte 2 und 3 ist praktisch: **Aussehen hat immer einen
guten Vorgabewert** — das Theme der Ziel-App. Danach muss niemand gefragt
werden. **Inhalt hat keinen.**

**Beispiel Sicherungsmodul.** Der Ablauf ist überall gleich: Menü aufziehen,
Einträge anhaken, Fortschritt zeigen, Fehler behandeln → Spalte 1. Kartenfarbe
und Schrift → Spalte 2. Aber *welche Daten* eine App überhaupt sichern kann,
weiß nur diese App → Spalte 3, und das Modul nimmt sie über eine Schnittstelle
entgegen, die in der Anbindung umgesetzt wird.

**Teile, die diese App nicht braucht, bleiben trotzdem liegen.** Nichts aus der
Modulkopie herauskürzen — der Compiler entfernt ungenutzten Code ohnehin, und
jede Kürzung würde die Byte-Identität zerstören.

Frag nur nach Spalte 3 — und nicht als offene Frage, sondern mit Vorschlag:

> „Was soll die App sichern? Ich sehe Notizen, Kategorien und Einstellungen —
>  nehme ich alle drei?"

## Phase 3 — Kopieren und anbinden

1. **Modulkopie** byte-identisch an den Ort aus der Plattform-Referenz legen.
   Nichts umbenennen, nichts kürzen, den Herkunfts-Kopf mitnehmen.
2. **Anbindung** nach `assets/anbindung-vorlage.md` schreiben: Theme-Werte der
   **Ziel-App** einsetzen, Schnittstellen aus Spalte 3 umsetzen, Texte in der
   Sprache und Tonlage der Ziel-App.
3. **Aufrufstelle** einbauen — `BEISPIEL.md` des Moduls ist die Vorlage, aber
   der Aufruf gehört an die Stelle, an der er in *dieser* App Sinn ergibt.

Plattform-Eigenheiten stehen in `references/android.md`, `windows.md`,
`macos.md`, `ios.md` — vor dem Kopieren die passende lesen. Bei Xcode ist die
Zielmitgliedschaft der häufigste Stolperstein, bei WPF ein ausdrückliches
`<Compile Include=…>`.

## Phase 4 — Abnahme

Zwei Prüfungen, beide zwingend:

1. **`diff` zwischen Bibliothek und App-Kopie ist leer.** Ist er es nicht, hast
   du am falschen Ort angepasst — die Änderung gehört in die Anbindung.
2. **Die Ziel-App baut grün.** Vorher gilt der Einbau nicht als erledigt.

## Phase 5 — Buchführung und Abschluss

- **`MODUL.md`**: Zeile in der Konsumententabelle ergänzen — App, Stand, Pfad
  der Kopie. Ohne diesen Eintrag findet das spätere Nachziehen die App nicht.
- **`INDEX.md`**: Konsumentenzähler erhöhen.
- **Regel 9 für die Ziel-App**: bauen, Version bumpen mit echter Systemzeit,
  committen, pushen, installieren.
- **Ein Commit** für App und `MODUL.md` zusammen — die Buchführung darf nicht
  getrennt von dem stehen, was sie beschreibt.

**Die Commit-Regel in einem Satz:** Die Buchführung wandert mit der App. Beim
Einbau ist das ein Commit (App + neue Zeile in der Konsumententabelle), beim
Nachziehen einer pro App (App + die gehobene Zeile *dieser* App). So ist jeder
Commit für sich zurücknehmbar, und `MODUL.md` sagt zu keinem Zeitpunkt etwas
Falsches.

---

# Modus „Nachziehen"

Auslöser: „zieh M1.1 nach", „zieh M1.1 nach in GenialeIdeen", „verteile die
Änderung". Ohne Zusatz sind **alle** Konsumenten gemeint.

1. Konsumententabelle in `MODUL.md` lesen — sie ist die verbindliche Liste.
2. **Gewachsene Anforderungen prüfen.** Ist `Host muss liefern` gegenüber dem
   Stand der App länger geworden, braucht deren Anbindung eine Ergänzung. Das
   ist erwartete Arbeit, kein Baufehler — nenn es vorher, statt es später als
   Fehler zu melden.
3. **Abweichungsprüfung — vor jedem Überschreiben.** Siehe unten.
4. Pro App: **nur die Modulkopie** überschreiben, Anbindung unangetastet lassen.
5. Pro App bauen.
6. Pro App Regel 9 und **ein eigener Commit**, der auch gleich **die Zeile
   dieser App** in der Konsumententabelle hebt. Nicht am Ende gesammelt —
   sonst behauptet `MODUL.md` zwischendurch etwas Falsches, und ein einzelner
   Rückzieher nimmt die Buchführung nicht mit.

### Abweichungsprüfung — der Code-Zwilling der Datenprüfung

Die Regel sagt, dass niemand direkt in einer Modulkopie arbeitet. Es wird
trotzdem passieren — jemand fixt schnell etwas an Ort und Stelle. Blindes
Überschreiben wirft diesen Fix weg, und niemand merkt es.

Deshalb **vor** dem Überschreiben vergleichen: App-Kopie gegen den
Bibliotheksstand **der Version, die die App laut Konsumententabelle hat** (nicht
gegen den neuen — sonst siehst du nur die Modul-Änderung selbst).

| Ergebnis | Vorgehen |
|---|---|
| Kein Unterschied | überschreiben, alles gut |
| **Unterschied** | **anhalten** |

Bei einem Unterschied den Unterschied **zeigen** und zur Wahl stellen:

> ⚠️ Die Kopie in GenialeIdeen weicht vom Stand v3 ab — dort wurde direkt in
> der Modulkopie etwas geändert (3 Zeilen in `settle()`).
>
> 1. Diese Änderung zuerst ins Modul heben (empfohlen) — v4, dann überall nachziehen
> 2. Verwerfen und mit dem Bibliotheksstand überschreiben
>
> Wie soll ich vorgehen?

**Niemals stillschweigend verwerfen.** Vorher zu committen schützt den Text,
aber nicht das Wissen, dass es diesen Fix überhaupt gab — und danach sucht ihn
niemand mehr.

Der Fall „App hat lokal gefixt **und** die Bibliothek ist weitergezogen" ist
kein Sonderfall, sondern genau Weg 1: erst den Fix hochheben, dann nachziehen.
Ein Drei-Wege-Abgleich ist dafür nicht nötig, nur das Anhalten.

7. Am Ende melden, welche Apps gehoben wurden, welche auf Rückfrage warten und
   welche unverändert blieben.

**Wenn eine App nicht mehr baut**, ist das die Information, für die dieser
Schritt existiert: Die Modulsignatur hat sich geändert und die Anbindung dieser
App passt nicht mehr. Repariere die **Anbindung**, niemals die Modulkopie.

Melde am Ende ausdrücklich, welche Apps gehoben wurden und welche nicht — eine
still übersprungene App hinkt sonst monatelang hinterher, ohne dass es jemand
merkt.

## Wenn der Benutzer mehrere Module nennt

**Erst alle prüfen, dann bauen.** Führ Phase 1 und 1b für **alle** genannten
Module durch, sammle sämtliche Rückfragen und stell sie **in einem Zug**. Sonst
muss der Benutzer siebenmal hintereinander etwas entscheiden, jedes Mal mit
Wartezeit dazwischen.

**Reihenfolge nach Abhängigkeit, nicht nach Nummer.** Braucht M1.5 das Modul
M1.2, kommt M1.2 zuerst — auch wenn der Benutzer sie anders aufgezählt hat. Das
Feld `Braucht Module:` in den Manifesten gibt die Reihenfolge vor.

Danach jedes Modul vollständig durch Phase 2–4. Gemeinsam ist nur der
Abschluss — **ein** Build, **ein** Versions-Bump, **ein** Commit für die App.
Sieben Bumps für einen Arbeitsgang wären Lärm.

Bricht ein Modul die Prüfung in Phase 1, überspring **nur dieses** und bau die
übrigen fertig — außer ein anderes Modul hängt davon ab, dann fällt es mit.
Sag am Ende klar, welche fehlen und warum.
