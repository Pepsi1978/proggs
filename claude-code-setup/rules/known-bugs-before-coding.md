# Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-01.
> Ausloeser: Chrome-Extension-Verschwind-Bug — ~1h verschwendet, weil der bekannte
> Workaround (Ordner wechseln) nicht vorher nachgeschlagen wurde. Frank: "Wenn du
> vor der Arbeit an Chrome-Erweiterungen immer erst alle bekannten Bugs durchliest,
> waere der Fehler nie aufgefallen." Poka-Yoke Stufe 3: Fehler gar nicht erst machen.
>
> Vollstaendige Systembeschreibung: `~/proggs/bugs/SYSTEM.md`. Index: `~/proggs/bugs/README.md`.

---

## Grundregel

Es gibt pro Technologie-Bereich einen **kuratierten Bug-Almanach** unter:

```
~/proggs/bugs/<bereich>.md
```

Das Inhaltsverzeichnis aller Almanache (sortiert nach Plattform, mit Erkennungs-Triggern)
liegt in `~/proggs/bugs/README.md`.

**BEVOR** mit einem Bereich gearbeitet wird, fuer den ein Almanach existiert, MUSS
dieser ZUERST komplett gelesen werden — als Pflicht-Vorstufe, noch vor der ersten
Code-Aenderung. So werden bekannte Fehler gar nicht erst gemacht, statt sie
hinterher zu debuggen.

Das ist der Unterschied zu `bug-cases.jsonl`: jene wird REAKTIV nach einem Fehler
durchsucht; der Almanach wird PROAKTIV vor der Arbeit gelesen.

---

## Drei Automatik-Schichten (sorgen dafuer, dass es in JEDER Session laeuft)

| Schicht | Mechanismus | Wann |
|---------|-------------|------|
| 1 — Praesenz | `bug-almanac-index` Hook (SessionStart) blendet die Almanach-Liste ein | jeder Session-Start |
| 2 — Sicherheitsnetz | `bug-almanac-guard` Hook (PreToolUse Edit/Write) erinnert bei bereichstypischen Dateien an den Almanach | bei Datei-Beruehrung |
| 3 — Verhalten | diese Regel | immer geladen |

---

## Pflicht-Ablauf

1. Bereich (und, falls relevant, Software-Version) der anstehenden Aufgabe erkennen.
2. Greift es ueberhaupt? Bei trivialem Kleinkram (einzelner String, Doku, Kommentar,
   Versions-Bump) NEIN — weiter ohne Almanach. Sonst:
3. Im Index (`bugs/README.md`) pruefen, ob ein Almanach fuer den Bereich existiert.
4. **Almanach vorhanden** → komplett lesen, das `Versionen:`-Feld pro Bug gegen die
   aktuell benutzte Version abgleichen (Version live ermitteln, z.B. `chrome --version`,
   `./gradlew --version`). Arbeite ich mit einer neueren Version als im Stand-Header
   dokumentiert → Frank melden, OK fuer einen kurzen Re-Check einholen. Dann mit dem
   Bug-Wissen arbeiten.
5. **Kein Almanach** → Frank melden ("neuer Bereich X, kein Almanach"), auf sein
   **OK** warten, dann 3–5 Researcher die bekannten Bugs + bewaehrte (funktionserhaltende!)
   Loesungen recherchieren, daraus einen Almanach in `bugs/` anlegen und in `README.md`
   eintragen (Trigger im `bug-almanac-guard`-Hook ergaenzen).
6. Tritt waehrend der Arbeit ein Fehler auf → ZUERST pruefen, ob es ein bekannter Bug
   aus dem Almanach ist → dokumentierte Loesung sofort anwenden (schnellster Pfad).
7. **Nach** der Aufgabe: jeden NEU erlebten Bug als Eintrag im Almanach ergaenzen
   (Bug + Loesung + Versionen, Stand-Header aktualisieren). Compound Intelligence —
   der Almanach waechst.

Wichtig: Das "erst Franks OK" gilt NUR fuer die gezielte Almanach-Recherche
(Researcher-Schwarm fuer einen neuen Bereich / Re-Check). Ein normales kurzes
Web-Lookup mitten im Debuggen einer laufenden Aufgabe bleibt frei.

---

## Wann diese Regel greift (Beispiele)

| Arbeit an … | Almanach |
|-------------|----------|
| Chrome-/Browser-Erweiterung (manifest.json, content scripts, service worker, chrome.storage, Overlays) | `~/proggs/bugs/chrome-extensions.md` |
| Weitere Bereiche (Android, WPF, Swift, TS, Tampermonkey, Hooks, Gradle …) | `~/proggs/bugs/<bereich>.md` (bei erster echter Arbeit anlegen, siehe README) |

Erkennungssignale fuer "Chrome-Extension-Arbeit": eine `manifest.json` mit
`manifest_version`, Dateien in einem Extension-/Overlay-Ordner, `chrome.*`-APIs,
`*.user.js`, oder der Nutzer nennt Erweiterung/Extension/Overlay-Plugin.

---

## Zusammenspiel

| System | Rolle |
|--------|-------|
| `~/proggs/bugs/*.md` + `README.md` + `SYSTEM.md` | Proaktiver Almanach pro Bereich (VOR der Arbeit) |
| `bug-almanac-index` / `bug-almanac-guard` Hooks | Automatik: Liste einblenden / an Datei erinnern |
| `bug-cases.jsonl` | Reaktive Fall-Datenbank mit Auto-Match (NACH einem Fehler) |
| Direktive #3 (Resilient Bugfixing) | Jeder neue Bug → Almanach-Eintrag + bug-case; Loesungen IMMER funktionserhaltend |
| Direktive #1 (Superintelligenz/Harness) | Der Almanach ist Harness-Wissen, das jede Session erbt und mitwaechst |

---

## Was NIEMALS passieren darf

- ❌ An einem Bereich arbeiten, fuer den ein Almanach existiert, ohne ihn vorher zu lesen
- ❌ Einen erlebten Bug fixen, ohne ihn anschliessend im passenden Almanach zu ergaenzen
- ❌ Den Almanach als "nice to have" behandeln — das Vorab-Lesen ist Pflicht
- ❌ Eine gezielte Almanach-Recherche ohne Franks OK starten
- ❌ Einen Bug "loesen", indem Funktionalitaet entfernt wird (Direktive #3 — funktionserhaltend)
