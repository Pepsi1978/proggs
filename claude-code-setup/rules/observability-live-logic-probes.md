# Live-Logik-Sonden: Intent-Verifikation in Echtzeit (KRITISCH)

> Zusatz-Direktive und Erweiterung von [[observability-first]] (Abschnitt 2.3 Logik-Sonden +
> Abschnitt 4 Live-Monitoring). Gilt fuer jedes qualifizierte Software-Projekt. Adressat: Claude Code.
> Fokus: live pruefen, ob die Logik genau das tut, was im Bau-Prompt gemeint war.

## Der Unterschied zu "normalen" Sonden

Logik-Sonden (observability-first) sind **defensiv** (schlagen bei Annahme-Verletzung an).
Live-Logik-Sonden sind **bestaetigend**: sie zeichnen die Logik selbst auf, waehrend die Software
laeuft, und melden live, ob jeder fachliche Schritt so umgesetzt wurde wie beschrieben. Nicht "ist
etwas kaputt?", sondern "ist die Logik richtig angekommen?" — in Echtzeit beim ersten Start.

## Kernmechanik — Intent-gebundene Checkpoints

Beim Bauen aus einem Bau-Prompt mit klarer Verhaltensabsicht: jede beabsichtigte Verhaltensweise /
jedes Akzeptanzkriterium ("die App soll …") als benannten **Live-Logik-Checkpoint** an genau der
Stelle verdrahten, wo der Schritt passiert. Jeder Checkpoint gibt zur Laufzeit **erwartet vs.
tatsaechlich** aus, in einen EIGENEN Kanal (getrennt vom Fehler-Log):

```json
{"ts":"…","kind":"CHECKPOINT","step":"Rabatt berechnen","intent":"10% ab 3 Artikeln","expected":"0.10","actual":"0.00","ok":false,"ctx":{"items":4}}
```

`step` (fachlicher Schritt), `intent` (was gemeint war, Klartext), `expected`/`actual`, `ok`, `ctx`.
Eigener TAG/Kanal (z.B. `LOGIC` bzw. `CHECKPOINT`) → live verfolgbar ohne Fehler-Rauschen.

## Der Live-Verifikations-Loop

1. Frank startet die Software. 2. Checkpoint-Kanal streamen: Android `adb logcat -s LOGIC`,
Windows `Get-Content <log> -Wait`, macOS/Linux `tail -f`. 3. Frank bedient die App normal.
4. Claude liest mit und prueft jeden Checkpoint gegen die Absicht: `ok:true` → "Schritt korrekt
angekommen ✓"; `ok:false` → sofort melden ("erwartet …, tatsaechlich … → Logik nicht wie gemeint").
5. Bei Abweichung: Root-Cause (Direktive #3), fixen, beim naechsten Lauf erneut verifizieren.

## Was aufgezeichnet wird

Die logische Substanz: Entscheidungen (welcher Zweig, war es der beabsichtigte?), Berechnungen
(Ergebnis vs. erwartet), Ablauf-Schritte (Reihenfolge/uebersprungen), Zustandsuebergaenge gegen Spec,
Ein-/Ausgaben an fachlichen Grenzen.

## Verankerung

Checkpoints werden mitgebaut, sobald Software aus einem Prompt mit klarer Verhaltensabsicht entsteht.
Co-Evolution (wie observability-first): neuer/geaenderter Intent → Checkpoint mitziehen; weggefallener
Intent → Checkpoint entfernen (Stale-Probe-Schutz). Zuruf: **"starte den Live-Logik-Check"**.

## Was NIEMALS passieren darf

- Aus Bau-Prompt mit klarer Absicht bauen OHNE Intent-Checkpoints
- `ok:false` im Stream sehen und nicht sofort melden + an der Wurzel fixen
- Checkpoints in denselben Kanal wie das Fehler-Log mischen
- Geaenderte Absicht committen ohne Checkpoint mitzuziehen
