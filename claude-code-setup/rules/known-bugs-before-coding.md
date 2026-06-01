# Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-01.
> Ausloeser: Chrome-Extension-Verschwind-Bug — ~1h verschwendet, weil der bekannte
> Workaround (Ordner wechseln) nicht vorher nachgeschlagen wurde. Frank: "Wenn du
> vor der Arbeit an Chrome-Erweiterungen immer erst alle bekannten Bugs durchliest,
> waere der Fehler nie aufgefallen." Poka-Yoke Stufe 3: Fehler gar nicht erst machen.

---

## Grundregel

Es gibt pro Technologie einen **kuratierten Bug-Almanach** unter:

```
~/proggs/.claude/known-bugs/<technologie>.md
```

**BEVOR** mit einer Technologie gearbeitet wird, fuer die eine solche Datei
existiert, MUSS diese Datei ZUERST komplett gelesen werden — als Pflicht-Vorstufe,
noch vor der ersten Code-Aenderung. So werden bekannte Fehler gar nicht erst
gemacht, statt sie hinterher zu debuggen.

Das ist der Unterschied zu `bug-cases.jsonl`: jene wird REAKTIV nach einem Fehler
durchsucht; der Almanach wird PROAKTIV vor der Arbeit gelesen.

---

## Wann diese Regel greift (Beispiele)

| Arbeit an … | ZUERST lesen |
|-------------|--------------|
| Chrome-/Browser-Erweiterung (manifest.json, content scripts, service worker, chrome.storage) | `~/proggs/.claude/known-bugs/chrome-extensions.md` |
| (weitere Almanache nach Bedarf anlegen) | `~/proggs/.claude/known-bugs/<tech>.md` |

Erkennungssignale fuer "Chrome-Extension-Arbeit": eine `manifest.json` mit
`manifest_version`, Dateien in einem Extension-Ordner, `chrome.*`-APIs,
`*.user.js`, oder der Nutzer nennt Erweiterung/Extension/Overlay-Plugin.

---

## Pflicht-Ablauf

1. Technologie der anstehenden Aufgabe erkennen.
2. Pruefen ob `~/proggs/.claude/known-bugs/<tech>.md` existiert.
3. Wenn ja: **komplett lesen**, BEVOR die erste Datei geaendert wird.
4. Die Almanach-Checkliste auf die konkrete Aufgabe anwenden.
5. **Nach** der Aufgabe: jeden NEU aufgetretenen Bug dieser Technologie als
   Eintrag im Almanach ergaenzen (Compound Intelligence — der Almanach waechst).
6. Existiert noch kein Almanach fuer eine fehleranfaellige Technologie an der
   wir wiederholt arbeiten: einen anlegen.

---

## Zusammenspiel

| System | Rolle |
|--------|-------|
| `~/proggs/.claude/known-bugs/*.md` | Proaktiver Almanach pro Technologie (VOR der Arbeit) |
| `bug-cases.jsonl` | Reaktive Fall-Datenbank mit Auto-Match (NACH einem Fehler) |
| Direktive #3 (Resilient Bugfixing) | Jeder neue Bug → Almanach-Eintrag + bug-case |
| Direktive #1 (Superintelligenz/Harness) | Der Almanach ist Harness-Wissen, das jede Session erbt |

---

## Was NIEMALS passieren darf

- ❌ An einer Technologie arbeiten, fuer die ein Almanach existiert, ohne ihn vorher zu lesen
- ❌ Einen erlebten Bug fixen, ohne ihn anschliessend im passenden Almanach zu ergaenzen
- ❌ Den Almanach als "nice to have" behandeln — das Vorab-Lesen ist Pflicht
