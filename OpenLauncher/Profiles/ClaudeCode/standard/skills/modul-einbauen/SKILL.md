---
name: modul-einbauen
description: Baut ein fertiges Modul aus ~/proggs/Module/ in eine App ein — Funktionen 1:1, Aussehen an die Ziel-App angepasst — und verteilt Modul-Änderungen an alle Apps, die das Modul bereits benutzen. Nutze diesen Skill IMMER wenn der Benutzer sagt "bau M1.1 ein", "baue das Modul X ein", "Modul einbauen", "nimm M1.1 bis M1.7 und bau sie ein", "setz das Drag-and-Drop-Modul in App Y ein", "das Modul aus der Bibliothek in die neue App", "benutze Modul Mx.y", "zieh M1.1 nach", "zieh das Modul überall nach", "verteile die Modul-Änderung", "bring App Y auf den neuen Modulstand", "welche Apps hinken beim Modul hinterher". Der Benutzer darf das Modul über die Nummer ODER über seinen Anzeigenamen nennen ("bau das Drag & Drop Modul ein") — beides nachschlagen in Module/INDEX.md. Der Skill prüft Versionsverträglichkeit, kopiert das Modul byte-identisch, erzeugt eine app-eigene Anbindungsdatei für Aussehen und app-spezifische Inhalte, legt vor, was 1:1 übernommen wird und was geklärt werden muss, und nimmt erst nach grünem Build ab. NICHT nutzen, wenn aus bestehendem App-Code erst ein neues Modul entstehen soll ("mach daraus ein Modul", "als Modul abspeichern", "extrahiere X als Modul") — dafür ist der Skill modul-erstellen zuständig.
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

## Phase 1 — Modul finden und Vorabprüfung

Lies `Module/INDEX.md`. Der Benutzer nennt das Modul über die **Nummer**
(`M1.1`) oder über den **Anzeigenamen** („das Drag & Drop Modul") — beides muss
zum Ziel führen. Ist die Angabe mehrdeutig oder nicht auffindbar, frag nach,
statt das nächstbeste Modul zu nehmen.

Dann `MODUL.md` und `BEISPIEL.md` des Moduls lesen und **vier Dinge prüfen,
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
4. **Ist das Modul selbst sauber?** Durchsuch den Modulcode nach direkten
   Theme- und Ressourcenzugriffen (`MaterialTheme.`, `R.string`, `R.color`,
   `StaticResource`, `Color("…")`, ein CompositionLocal der Ursprungs-App).
   Findest du welche, **brich ab**: Das Modul lässt sich nicht per Anbindung
   anpassen, weil das Aussehen fest verdrahtet ist. Das ist kein
   Einbau-Problem, sondern eine übersehene Nabelschnur — zurück zu
   `modul-erstellen`, Modul-Version +1, danach einbauen.

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

---

# Modus „Nachziehen"

Auslöser: „zieh M1.1 nach", „zieh M1.1 nach in GenialeIdeen", „verteile die
Änderung". Ohne Zusatz sind **alle** Konsumenten gemeint.

1. Konsumententabelle in `MODUL.md` lesen — sie ist die verbindliche Liste.
2. Pro App: **nur die Modulkopie** überschreiben, Anbindung unangetastet lassen.
3. Pro App bauen.
4. Pro App Regel 9 und **ein eigener Commit** — eine App pro Commit, damit ein
   einzelner Rückzieher möglich bleibt.
5. Am Ende alle Stände in der Konsumententabelle hochsetzen.

**Wenn eine App nicht mehr baut**, ist das die Information, für die dieser
Schritt existiert: Die Modulsignatur hat sich geändert und die Anbindung dieser
App passt nicht mehr. Repariere die **Anbindung**, niemals die Modulkopie.

Melde am Ende ausdrücklich, welche Apps gehoben wurden und welche nicht — eine
still übersprungene App hinkt sonst monatelang hinterher, ohne dass es jemand
merkt.

## Wenn der Benutzer mehrere Module nennt

„Nimm M1.1 bis M1.7" heißt: nacheinander, jedes vollständig durch Phase 1–4,
bevor das nächste beginnt. Gemeinsam ist nur der Abschluss — **ein** Build,
**ein** Versions-Bump, **ein** Commit für die App. Sieben Bumps für einen
Arbeitsgang wären Lärm.

Bricht ein Modul die Prüfung in Phase 1, überspring **nur dieses** und bau die
übrigen fertig. Sag am Ende klar, welches fehlt und warum.
