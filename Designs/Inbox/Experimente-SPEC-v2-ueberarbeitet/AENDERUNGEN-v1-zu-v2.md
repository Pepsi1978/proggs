# Was sich von v1 (überarbeitet) zu v2 (überarbeitet) geändert hat

Stand: 14.08.2026, 11.36 Uhr
Gegenüberstellung: `Designs/Inbox/Experimente-SPEC-v1-ueberarbeitet.zip` (12.08.2026, 12.06 Uhr)
gegen die App, wie sie am 14.08.2026 läuft.

---

## Warum es diese Fassung gibt

Das v1-Spec war ein **Auftrag**: Der Monitor `B-10` war beschrieben, aber im Design noch nicht
aufgebaut, und die Effektschicht `E-01` bis `E-24` stand als Forderung da. Zwischen dem 12.08.
und dem 13.08.2026 wurde die App in **siebzehn Schritten** weitergebaut — beginnend mit einem
vollständigen Neuaufbau aus dem **Fold-Außendisplay-Entwurf**. Dabei sind Funktionen entstanden,
die im Spec fehlten, und Regeln, die dem Spec widersprechen, weil sie sich beim Benutzen als
falsch erwiesen haben.

**Diese Fassung beschreibt die App, wie sie ist.** Sie erfindet nichts hinzu.

| | v1 (überarbeitet) | v2 (überarbeitet) |
|---|---|---|
| Funktionen | F-01 … F-41 | F-01 … **F-58** |
| davon beschrieben | 34 (F-27 … F-33 fehlten) | **58** |
| Bildschirme | B-01 … B-10, `B-10` **nicht gebaut** | B-01 … B-10, **alle gebaut** |
| Effekte | E-01 … E-24 | E-01 … **E-29** |
| Bewegungen | M-01 … M-95 | M-01 … **M-100** |
| Abnahme | A-01 … A-30 | A-01 … **A-45** |
| Schriftrollen | 10 | **22** |
| Farbrollen | 13 | **14** |
| Datenbank | (nicht genannt) | **Room Version 4**, drei Wanderungen |

**Nächste freie Nummern für den Rücklauf:** `B-11` · `F-59` · `M-101` · `E-30` · `A-46`.

---

## 1. Die siebzehn neuen Funktionen

| Kennung | Funktion | Warum sie entstanden ist |
|---------|----------|--------------------------|
| **F-42** | Dauer eines Experiments nachträglich ändern | Die KI schätzte die Dauer allein, und sie ließ sich **nirgends** berichtigen — aus „die nächsten sechs, sieben Tage" wurden zwei, und dabei blieb es |
| **F-43** | Dauer beim Anlegen selbst bestimmen | dieselbe Ursache, an der Stelle, an der sie entsteht |
| **F-44** | Ein Experiment weiterführen statt abzuschließen | Die Auswertung am letzten Tag beendete das Experiment **stillschweigend**. Wer bei „Tag 2 von 2" erzählte, wie es lief, hatte es damit beendet |
| **F-45** | Der Verlauf: jede Auswertung bleibt einzeln erhalten | Eine zweite Aufnahme am selben Kalendertag **überschrieb** die erste samt Einschätzung. Wer nachts um halb eins erzählte und am folgenden Abend noch einmal, verlor den ganzen Vortag |
| **F-46** | Logbuch-Reiter *Auswertungen* | Mit dem Abschluss verschwindet die Karte aus dem Monitor — und über sie führte der einzige Weg zu ihren Auswertungen |
| **F-47** | Vorlesen an jeder Stelle | Vorlesen gab es an genau **einer** Stelle, obwohl die App auf Sprache gebaut ist und der Vorleser längst bereitstand |
| **F-48** | Stimme des Geräts als vierter Weg und Rückfallebene | Drei der vier Wege brauchen Netz, zwei zusätzlich einen Schlüssel — fehlte eines, blieb jeder Lautsprecher **stumm** |
| **F-49** | Zwischen Morgen und Abend umschalten | Der Abend-Zustand wurde gesetzt, aber niemand leitete daraus den Zustand ab — er erschien **nie**, auch nicht über die Erinnerung |
| **F-50** | Mikrofon-Erlaubnis erfragen und fortsetzen | Die Erlaubnis wurde nur **geprüft**, nie **angefragt**. Damit war jede Spracheingabe tot: F-01, F-09, F-10, F-18, F-20, F-21, F-23 |
| **F-51** | Zurück: Stapel, Zurück-Taste, Wischgeste | Ein einziger Besuch im Selbstbild machte den Zurück-Pfeil der Einstellungen zur Sackgasse; die Zurück-Geste wurde gar nicht behandelt |
| **F-52** | Selbstbild ausdrücklich sichern | Es wurde einzig beim Druck auf den Zurück-Pfeil gespeichert — und die Sackgasse zwang dazu, die App wegzuwischen |
| **F-53** | Eigene Stimmen verwalten | Die 46 Zeichen lange Stimmkennung hätte von Hand auf dem Telefon eingetippt werden müssen |
| **F-54** | Gerätecode anzeigen, kopieren, abbrechen | Ohne sichtbaren Code kann auf der OpenAI-Seite nichts eingetippt werden — die Anmeldung war nicht durchführbar |
| **F-55** | Weckzeit an der Uhr stellen | Die Zeiten ließen sich überhaupt nicht einstellen |
| **F-56** | Nachlauf: Liegengebliebenes wird nachgeholt | An fünf Stellen stand „läuft beim nächsten Start nach". **Nachgeholt hat es nie jemand** |
| **F-57** | Tageswechsel im laufenden Betrieb | Wer die App abends offen liegen ließ, schrieb morgens noch immer in den **Vortag** |
| **F-58** | Anlegefläche beiseitelegen | Ein Druck neben die Fläche verwarf eine fertige Transkription |

## 2. Sieben nachgetragene Funktionen

`F-27` bis `F-33` standen in v1 **nur in der Übersichtstabelle** und im Anhang „Neu aus dem
Design" — beschrieben war keine von ihnen. Sie sind gebaut, also stehen sie jetzt mit vollem
Ablauf im Funktions-Spec: Wischen · Lage tippen · Auswertungstext bearbeiten · Überspringen ·
Reiter *15 Tage* · Reiter *Langzeit* · Anlegen abbrechen.

## 3. Dreiundzwanzig geänderte Bestandsfunktionen

Jede trägt im Funktions-Spec einen Kasten **„Geändert in v2"**.

| Kennung | Was sich geändert hat |
|---------|------------------------|
| `F-01` | Groq-Schlüssel wird **vorher** geprüft · Erlaubnis wird angefragt · Wellenform und Sekundenanzeige · neuer Zustand `AUFNAHME` |
| `F-03` | eigener Wartezustand `WARTET` · Zahl der Laufenden direkt aus der Ablage statt aus dem Strom |
| `F-06` | aufsteigendes Haptik-Muster, Sprung auf den Monitor, Funken |
| `F-08` | kurzer Stoß beim Abhaken · Aufgaben auch auf der Abendkarte abhakbar |
| `F-09` | Lautsprecher an jeder Runde · Auswertungen nicht mehr als Blase · Textfeld mit Sende-Knopf |
| `F-10` | **ein** Experiment statt aller der Reihe nach · jede Aufnahme eigene Zeile · Text geht **vor** dem Netzaufruf in den Faden |
| `F-11` | **schließt das Experiment nicht mehr ab** · Einschätzung geht an genau die eben angelegte Zeile |
| `F-12` | überall (`F-47`) · Rückfall auf die Gerätestimme · lesbare Fehlertexte |
| `F-13` | nur auf ausdrücklichen Knopf · Abschluss an die **jüngste** Auswertung · „Nicht umgesetzt" schreibt endlich ins Logbuch |
| `F-16` | war an **keinem Knopf** angeschlossen — jetzt „Ändern" und Papierkorb mit Rückfrage |
| `F-18` | Tagewahl · **ohne Netz wird trotzdem gespeichert** (vorher wurde gar nichts angelegt) |
| `F-19` | Papierkorb ohne Rückfrage statt Wischen/langer Druck |
| `F-20` | Ändern und Löschen waren von keinem Knopf erreichbar · Blatt bleibt nach dem Speichern offen |
| `F-21` | siehe `F-52` |
| `F-23` | vierter Anbieter · **eine** Quelle für Anbieter und Stimmen (vorher zwei Listen mit abweichenden Kennungen → „Meine Stimme" landete stumm bei Edge) · 31 + 6 Stimmen · Vorbelegung Edge |
| `F-24` | siehe `F-54` · Auge an den Schlüsselfeldern |
| `F-25` | Uhr-Dialog · Zeitstellen schaltet ein · ungenaue Weckzeit als Rückfall |
| `F-26` | **Symbol zeigt den aktiven Modus**, nicht den nächsten — hier weicht die App bewusst vom Spec ab |
| `F-27` | kein Pager · Schwelle 90 px · an den Enden passiert nichts |
| `F-30` | Text bleibt beim Zurückkehren zum selben Experiment stehen |
| `F-31` | Reiter heißt kurz „15 Tage" |
| `F-33` | nur der Knopf verwirft; Druck daneben legt beiseite (`F-58`) |
| `F-35` | **Stufe raus, Dauer rein** · Sprung auf den Monitor mit Meldung |
| `F-39` | **keine Rückfrage mehr** — Wischen und Kreuz legen auf die Merkliste · Warnton beim Wischen |
| `F-41` | Stufen schärfer gefasst: *Gedämpft* = still **und sichtbar** (45 %), *Aus* = ausgeblendet, Dauern 0 ms, Haptik stumm |

## 4. Datenmodell

| Einheit | Änderung |
|---------|----------|
| `Evaluation` | **+ `createdAt`** (Zeitpunkt der Aufnahme) · **+ `dayIndex`** (Versuchstag) · **Bedeutung geändert:** jede Aufnahme ist eine eigene Zeile, es gibt keine „Auswertung des Tages" mehr |
| `ChatTurn` | **+ `art`** (`GESPRAECH` / `AUSWERTUNG`) — trennt die Anzeige, ohne den Faden zu zerreißen |
| Einstellungen | **+ `ausstehend`** (Merker des Nachlaufs) · **+ `verdichtet_am`** · **+ `hinweis_benachrichtigung`** · `tts_provider` jetzt auf `edge_tts` vorbelegt, Alt-Kennungen werden beim Lesen geradegezogen |
| Datenbank | **Room Version 4** mit drei Wanderungen (1→2 Monitor-Felder, 2→3 `createdAt`, 3→4 `dayIndex` + `art`) |

## 5. Oberfläche

- **`B-10` ist gebaut** — die Werte stammen aus dem Fold-Entwurf, nicht mehr aus einer Ableitung.
  Neu darin: Zählzeile, Griff und Kreuz an der Wartekarte, antippbare Tagesangabe, Verlaufszeile
  auf der Laufkarte, „Vorschläge holen" im leeren Zustand.
- **`B-01`**: Datumszeile, Morgen/Abend-Umschalter, Zustände `AUFNAHME` und `WARTET`, Abendkarte
  mit Haken und drei Wegen, wischbare Vorschlagskarten.
- **`B-02`**: Lautsprecher an jeder Blase, Eingabeleiste mit Sende-Knopf, Denkpunkte,
  Leerzustandstext.
- **`B-03`**: die Abschlussfrage mit vier Wegen und der Verlauf — der am stärksten veränderte
  Bildschirm.
- **`B-05`**: gemeinsames Anlegeblatt mit Tagewahl, Papierkorb statt Dialog.
- **`B-07`**: **dritter Reiter**, Lautsprecher und Ändern/Löschen an jedem Tag.
- **`B-08`**: sieben Abschnitte, echte Aufklapplisten mit Haken, Gerätecodekarte, Uhr-Dialog,
  Auge an den Schlüsseln, Version am Fuß.
- **`B-09`**: Speichern-Knopf, Standanzeige, Wirkungssatz.
- **Typografie**: zwölf zusätzliche Rollen, alle drei Schriften als Datei eingebettet.
- **Farbe**: 14. Rolle `Auf Aktion` (`#FFF6F1`) und die Glasfarben beider Erscheinungen.
- **Effekte**: `E-25` Wisch-Rückmeldung · `E-26` antippbares Etikett · `E-27` Klapp-Pfeil ·
  `E-28` gestrichelter Rand · `E-29` Filmkorn als Kachel.
- **Bewegungen**: `M-96` Klapp-Pfeil · `M-97` Auswahl-Dreieck · `M-98` Denkpunkte ·
  `M-99` Wellenform-Balken · `M-100` Pulsringe.
- **Nachgetragen**: `M-01` (Druck sinkt ein), `M-03` (Haptik beim Aufnehmen), `M-05`
  (Vorschlagskarten werden ausgetauscht) und `M-06` (der Haken zeichnet sich) waren in v1
  an mehreren Stellen **genannt**, aber nirgends beschrieben. Sie sind gebaut — jetzt stehen
  ihre Werte im Motion-Spec.

## 6. Rahmen

- **Zielgerät** ist jetzt das **Fold-Außendisplay** (der schmalste Fall).
- **Fünf neue Berechtigungen**: `VIBRATE`, `MODIFY_AUDIO_SETTINGS`, `SCHEDULE_EXACT_ALARM`,
  `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`.
- **Vierter externer Dienst**: die Sprachausgabe des Geräts — ohne Netz, ohne Schlüssel.
- **Fünfzehn neue Abnahmekriterien** `A-31` bis `A-45`.

---

## 7. Wo die App dem v1-Spec widerspricht

Fünf Stellen. Überall gewinnt die App — sie ist am Gebrauch gewachsen.

| Stelle | v1 sagte | v2 sagt |
|--------|----------|---------|
| `F-26` | „Sein Symbol **kündigt den nächsten Modus** eindeutig an" | Es zeigt den **aktiven**. Bei hellem Bildschirm stand dort der Mond — das las sich wie eine Falschanzeige |
| `F-39` | „Rückfrage mit zwei Wegen: Auf die Merkliste · Löschen" | **Keine Rückfrage.** Beide Wege legen auf die Merkliste; endgültig gelöscht wird dort (`F-19`) |
| `F-35` | „Optional stellt er **Dauer und Stufe** selbst ein" | Nur die **Dauer**. Die Stufe schätzt die KI — dort lag sie nie daneben |
| `F-10` | „B-03 zeigt **alle offenen Experimente der Reihe nach**" | `B-03` arbeitet **ein** Experiment ab, geöffnet aus seiner Karte |
| Motion §8 | „*Gedämpft* → `M-76`, `M-78`, … **aus**" | Auf *Gedämpft* stehen sie **still und bleiben sichtbar** (45 %). Erst *Aus* blendet sie aus |
