# Projekt — Gedankenspeicher
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

## 0. Das Wichtigste

Der **KI-Knopf** ist der Kern der App. Alles andere — Aufnehmen, Tippen, Überschriften,
Vorlesen, Seitenleiste — dient nur dazu, dass beim Druck auf diesen Knopf ein guter,
zusammenhängender Notiz-Kontext bereitliegt. Geht beim Bauen etwas verloren, darf es
niemals dieser Knopf sein.

## 1. Zweck in drei Sätzen

Gedankenspeicher ist ein Ort, an dem Frank über Tage oder Wochen Gedanken zu einem Thema
sammelt — eingesprochen oder getippt, so wie sie ihm einfallen, ohne sie sofort ordnen zu
müssen. Jede Notiz landet mit Zeitstempel und einer von der KI vergebenen Überschrift als
Karte in einem chatartigen Verlauf; jede Notiz lässt sich vorlesen, im Text verbessern und
nachträglich ändern. Erst wenn genug beisammen ist, schaltet er die KI zu: sie nimmt die
gesammelten Notizen als Kontext, fragt zuerst zurück, worauf sie sich konzentrieren soll,
und liefert danach eine Auswertung in der Länge und Machart des aktiven Auswertungsprofils.

## 2. Zielplattform(en)

| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht oder später |
|-----------|----------------------|-------------|--------------------|
| Android | Galaxy Z Fold 8 (SM-F971B) — Cover 1248 × 1972 px @ 420 dpi (297 × 469 dp), Innen 1848 × 2448 px @ 420 dpi (440 × 583 dp) | Kotlin + Jetpack Compose | Pflicht |

**Beide Displays sind zu schmal für eine dauerhaft danebenstehende Seitenleiste.** Die
Sitzungsliste ist deshalb auf beiden Displays eine Schublade, die von links hereinfährt —
auf dem Innendisplay breiter (320 dp) als auf dem Cover (280 dp).

Paketname: `de.frank.gedankenspeicher` · Quellcode-Ordner: `~/proggs/Gedankenspeicher/`

## 3. Rahmenbedingungen

| Punkt | Festlegung |
|-------|-----------|
| **Sprache der Oberfläche** | Deutsch, einsprachig. Umlaute echt (ä ö ü ß). |
| **Offline / Online** | Teilweise offline. Lesen, Tippen, Bearbeiten, Suchen, Löschen und Vorlesen mit der Gerätestimme gehen ohne Netz. Aufnehmen geht ebenfalls ohne Netz — die Aufnahme wird gepuffert und automatisch nachtranskribiert, sobald Netz da ist (F-04). Transkription, Überschriften, Textverbesserung, KI-Auswertung und die drei Netz-Stimmen brauchen Netz. |
| **Konten / Anmeldung** | Keine App-eigene Anmeldung. Zwei fremde Anmeldungen in den Einstellungen: Codex per Gerätecode (F-11) und Google Drive für die Sicherung (F-17). |
| **Berechtigungen** | `RECORD_AUDIO` — wird beim ersten Druck auf den Aufnahmeknopf abgefragt, nicht beim Start. Bei Ablehnung bleibt die App voll benutzbar, nur der Aufnahmeknopf ist ausgegraut und erklärt beim Antippen, was fehlt. `INTERNET` und `ACCESS_NETWORK_STATE` sind Installationsrechte ohne Abfrage. |
| **Externe Dienste** | Groq (Transkription, `whisper-large-v3-turbo`) · ChatGPT Codex (Überschriften, Textverbesserung, Auswertung) · Microsoft Edge TTS · Google Cloud TTS (Chirp 3 HD) · Qwen (Stimmklon) · Google Drive (Sicherung). Schlüssel und Anmeldungen siehe `01-FUNKTIONS-SPEC.md` §5. |
| **Datenhaltung** | Alles auf dem Gerät in einer Room-Datenbank. Zusätzlich: Export je Sitzung als Markdown-Datei (F-16) und automatische Sicherung nach Google Drive (F-17). |
| **Verteilung** | Privat. Die App wird per `adb install -r` auf Franks Gerät installiert, nicht veröffentlicht. Deshalb: kein Onboarding, keine Datenschutzerklärung, keine Store-Pflichten — `04-ONBOARDING-SPEC.md` und `05-RECHT-SPEC.md` entfallen. |

## 4. Ausdrücklich NICHT enthalten

1. **Kein freies Chatten mit der KI.** Die KI meldet sich ausschließlich über den KI-Knopf
   (F-09) und ausschließlich zu den Notizen, die als Kontext übergeben wurden. Es gibt
   keinen offenen Chat-Eingabeschlitz und keinen Weg, die KI ohne Notiz-Kontext etwas zu
   fragen. Der einzige Text, den Frank direkt an die KI schickt, ist seine Antwort auf
   deren Rückfrage.
2. **Kein automatisches Auswerten.** Die KI läuft niemals von selbst los, weder nach einer
   bestimmten Anzahl Notizen noch nach Zeit. Sammeln und Auswerten sind zwei getrennte
   Vorgänge, und den zweiten löst immer Frank aus.
3. **Keine zweite Sortierebene über den Sitzungen.** Es gibt Sitzungen und darin Notizen —
   keine Ordner über den Sitzungen.

*(Was darüber hinaus nicht hineingehört — Bilder und Anhänge, Erinnerungen und
Benachrichtigungen, Tags — steht bewusst unter §6 Offene Fragen und ist nicht entschieden.)*

## 5. Abnahme — wann ist es fertig

| Kennung | Kriterium |
|---------|-----------|
| A-01 | Ich lege eine neue Sitzung an, spreche eine Notiz ein und sehe binnen weniger Sekunden eine Karte mit Zeitstempel, KI-Überschrift und dem transkribierten Text. |
| A-02 | Ich spreche fünf Notizen hintereinander ein. Alle fünf stehen untereinander im Verlauf, in der Reihenfolge, in der ich sie gesprochen habe. |
| A-03 | Ich schalte den Flugzeugmodus ein, spreche eine Notiz und sehe eine Karte mit „wartet auf Transkription". Schalte ich das Netz wieder ein, füllt sich die Karte binnen einer Minute von selbst mit dem Text. |
| A-04 | Ich tippe den Lautsprecher an einer langen Notiz an. Sie wird vorgelesen, Absatz für Absatz, ohne Aussetzer zwischen den Absätzen. Tippe ich denselben Knopf erneut an, hört sie auf. |
| A-05 | Ich tippe den KI-Knopf an. Die KI stellt eine Rückfrage, die erkennbar auf meine Notizen Bezug nimmt — keine allgemeine Standardfrage. |
| A-06 | Nach meiner Antwort erscheint eine KI-Antwortkarte im Verlauf, in Absätzen, deren Länge zum aktiven Auswertungsprofil passt. |
| A-07 | Ich spreche danach drei weitere Notizen ein und drücke erneut den KI-Knopf. Die zweite Auswertung bezieht sich nur auf diese drei Notizen, nicht auf die davor. |
| A-08 | Im KI-Dialog schalte ich „ganze Sitzung" ein. Nun bezieht sich die Auswertung nachweislich auch auf die früheren Notizen. |
| A-09 | Ich wechsle in den Einstellungen zwischen den vier Erscheinungen. Jede ist vollständig durchgefärbt — kein Bildschirm, kein Dialog, keine Schublade bleibt in der alten Erscheinung stehen. |
| A-10 | Ich ändere ein Auswertungsprofil, setze das Häkchen darauf und werte aus. Die Antwort folgt sichtbar dem geänderten Text. Es ist zu keinem Zeitpunkt möglich, zwei Häkchen gleichzeitig zu setzen. |
| A-11 | Ich drücke an einer Notiz den Verbessern-Knopf. Der Text wird sauberer. Der Rückgängig-Knopf stellt exakt den ursprünglichen Wortlaut wieder her. |
| A-12 | Ich suche nach einem Wort, das ich vor Wochen in einer anderen Sitzung gesagt habe, und finde die Notiz über das Suchfeld. |
| A-13 | Ich schließe die App während einer laufenden Aufnahme und öffne sie wieder. Es steht keine halbe, kaputte Notiz im Verlauf. |
| A-14 | Die App läuft auf dem Fold 8 zugeklappt und aufgeklappt, ohne dass Text abgeschnitten wird oder Knöpfe aus dem Bild rutschen. |
| A-15 | Kein Knopf in der ganzen App ist ohne Wirkung. |

## 6. Offene Fragen

| Nr. | Frage | Warum sie offen ist |
|-----|-------|--------------------|
| O-01 | Sollen Notizen Bilder, Dateien oder Anhänge aufnehmen können? | Frank hat die Frage bewusst nicht beantwortet. Bis zu einer Entscheidung wird **nichts davon gebaut**: Notizen sind reiner Text. Der Designer soll dafür auch keine Fläche vorsehen. |
| O-02 | Soll es Tags oder Kategorien innerhalb einer Sitzung geben? | Ebenso offengelassen. Bis zu einer Entscheidung nicht gebaut — die Sitzung ist die einzige Ordnungsebene. |
| O-03 | Soll die App Erinnerungen oder Benachrichtigungen schicken? | Ebenso offengelassen. Bis zu einer Entscheidung wird **keine** Benachrichtigung gesendet und kein Benachrichtigungsrecht angefordert. |
| O-04 | Wie viele Notizen darf eine Auswertung höchstens umfassen, bevor der Kontext zu groß wird? | Hängt vom Token-Fenster des gewählten Codex-Modells ab und lässt sich erst am laufenden System messen. Bis dahin gilt: es wird nicht gekürzt, und läuft der Aufruf in ein Kontext-Limit, meldet die App das offen (F-09, Fehlerfall) statt stillschweigend Notizen wegzulassen. |
