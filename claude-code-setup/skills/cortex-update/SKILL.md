---
name: cortex-update
description: >
  Synchronisiert Wissensdateien aus dem Repo ins zweite Gehirn (Cortex / Qdrant-Server) — neue UND
  geaenderte Bug-Almanache (bugs/), Best Practices (best-practices/) und Regeln
  (opencode-setup/rules-opencode/) landen unter Titel + passender Kategorie/Unterkategorie im Brain,
  genau wie der bestehende Bestand. Erkennt per Scan, was im Brain noch fehlt oder sich geaendert
  hat (inkl. veralteter "Gespeichert am:"-Praefix-Eintraege), zeigt eine Uebersicht mit Titel- und
  Kategorie-Vorschlaegen zum Freigeben, legt die bestaetigten Dateien ueber die brain-api ab und
  verifiziert grosse Dateien stichprobenartig auf VOLLSTAENDIGKEIT (kein abgeschnittener Text).
  Nutze diesen Skill IMMER wenn der Benutzer sagt: "Cortex Update", "starte Cortex Update",
  "synchronisiere ins Gehirn", "spiegle die Almanache/Best Practices/Regeln ins Cortex", "was ist
  neu fuers Gehirn", "aktualisiere das zweite Gehirn", "lade neue/geaenderte Dateien ins Gehirn",
  "Gehirn-Sync", "Cortex synchronisieren", "leg die neuen Almanache/Regeln ins Gehirn". Auch bei
  Varianten wie "haelt die Regeln mit dem Gehirn synchron" oder "ins Qdrant/Vektor-Gehirn ablegen".
  Aendert nur das Brain (per Bestaetigung) — niemals die Repo-Dateien.
invocation: user
---

# Cortex Update — Repo-Wissen ins zweite Gehirn (Cortex/Qdrant) synchronisieren

## Zweck

Im Repo entstehen laufend neue und geaenderte Wissensdateien (Almanache, Best Practices, Regeln).
Das zweite Gehirn (Cortex, `second-brain-server` / Qdrant) soll denselben Stand haben — damit alle
CLIs und Apps per semantischer Suche darauf zugreifen koennen. Dieser Skill findet die Luecken
(neu / geaendert), schlaegt Titel + Kategorie vor, legt nach Freigabe ab und prueft die Ablage.

**Drei Quellen — bewusst ERWEITERBAR** (in `scripts/cortex_sync.py` → `SOURCES`; eine neue Datenart
ist ein neuer Eintrag, mehr nicht):

| Quelle | Ordner / Datei | Brain-Kategorie | Titel |
|--------|----------------|-----------------|-------|
| **Almanache** | `bugs/<bereich>/<thema>.md` | `Programmierung/Almanache/<Label>` | Ueberschrift **+ `" (Almanach)"`** |
| **Almanach-Kurzchecks** | `bugs/<bereich>/<thema>-codecheck.md` (auch `-kurzcheck.md`) | `Programmierung/Almanache/Kurzchecks` | Ueberschrift **+ `" (Almanach Kurzcheck)"`** |
| **Best Practices** | `best-practices/<bereich>/<thema>.md` | `Programmierung/Best Practices/<Label>` | Ueberschrift **+ `" (Best Practices)"`** |
| **BP-Kurzchecks** | `best-practices/<bereich>/<thema>-codecheck.md` (auch `-kurzcheck.md`) | `Programmierung/Best Practices/Kurzchecks` | Ueberschrift **+ `" (Best Practices Kurzcheck)"`** |
| **Regeln** | `opencode-setup/rules-opencode/` | `Programmierung/Rules` | **erste Zeile der Datei, 1:1** (kein Zusatz) |

**Check-Versionen (Frank, 2026-06-27):** Zu jedem Almanach/jeder Best-Practice gibt es kuenftig eine
Check-Datei im SELBEN Bereichs-Ordner, erkennbar am Suffix `-codecheck` ODER `-kurzcheck` vor `.md`
(beide Schreibweisen werden erkannt). Diese landen in einer EIGENEN Unterkategorie `…/Kurzchecks`
(nicht nach Bereich getrennt) und bekommen ihren eigenen Titel-Zusatz, damit sie nie mit dem
Voll-Almanach/der Voll-Best-Practice kollidieren. Eine neue Datenart = ein neuer `SOURCES`-Eintrag
(`file_kind: "check"`, `category_tmpl` ohne `{label}`).

**Titel-Zusatz-Regel (Frank, 2026-06-27):** Weil die `doc_id` global nur aus dem Titel gebildet wird
(Kategorie zaehlt nicht), bekommt JEDER Almanach den festen Zusatz `" (Almanach)"` und JEDE
Best-Practice `" (Best Practices)"` an den Titel. So koennen Almanach UND Best-Practice zum selben
Thema (z.B. „Room") nie dieselbe ID bekommen und sich nie gegenseitig ueberschreiben — ohne
Server-Umbau. Der Zusatz wird idempotent gesetzt (nie doppelt). Regeln bleiben ohne Zusatz.

> **Einmalige Bestand-Umstellung:** Die bereits im Gehirn liegenden Almanach-/BP-Eintraege haben den
> Zusatz noch nicht (z.B. „Room" statt „Room (Almanach)"). Beim ersten Sync nach dieser Regel werden
> sie als NEU (mit Zusatz) erkannt; die alten (ohne Zusatz) erscheinen als **verwaist** und muessen
> einmalig geloescht werden (`forget` per Titel). Danach ist alles konsistent.

`<Label>` = Bereichs-Ordner in der Brain-Schreibweise (android → Android, apis → API, agents →
Agenten, android-build → Android Build, claude-tooling → Claude Tooling, second-brain → Second
Brain, …). Meta-Dateien (`README.md`, `SYSTEM.md`, `_*.md`, GROSSBUCHSTABEN-Dateien) werden
ausgelassen.

## Wichtige Technik (warum es so gebaut ist)

- **Brain-Schreiben:** `POST http://10.8.0.1:8000/store` (NUR ueber WireGuard erreichbar, NICHT die
  oeffentliche VPS-IP). Bearer-Key aus `~/SK/second-brain/brain.env` (`SB_API_KEY`).
- **doc_id ist titel-basiert und GLOBAL** (`sha1("frank::"+titel.lower())`): Gleicher Titel
  ueberschreibt — egal in welcher Kategorie. Titel muessen also ueber ALLE Eintraege eindeutig
  sein. Der `replaced`-Wert aus `/store` ist der Waechter: bei NEU + `replaced=true` = Titel-Kollision
  → wird NICHT als neu abgelegt, Titel muss angepasst werden.
- **Kontext-schonend:** Die Skripte lesen die (teils sehr grossen) Dateien per Datei-IO und sprechen
  direkt mit der brain-api — Volltexte laufen NIE durch den LLM-Kontext (kein MCP `remember` fuer
  Massen-Ablage). Der Skill sieht nur kompakte JSON-Berichte.
- **Normalisierung:** Der Server speichert Text `strip()`-getrimmt; die Skripte normalisieren gleich,
  damit die Zeichenzahl ein exakter Fingerabdruck ist (+-1 Toleranz fuer die finale Leerzeile).

## Schritt 0 — Scan (was fehlt / hat sich geaendert?)

```bash
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py scan          # beide + Regeln
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py scan --focus almanache
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py scan --focus "best practices"
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py scan --focus rules
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py scan --focus android   # nur ein Bereich
```

Standard = **alle drei Quellen** (Benutzer-Wahl). Der Scan liest `/list` vom Brain, ordnet jede
Datei ihrem Eintrag zu (exakter Titel → eindeutige Groesse → sonst neu) und schreibt einen Bericht
nach `~/.cortex-sync/scan.json` mit vier Gruppen:

| Status | Bedeutung | Aktion |
|--------|-----------|--------|
| **neu** | kein passender Brain-Eintrag | ablegen (Titel + Kategorie bestaetigen) |
| **geaendert** | Eintrag existiert, Inhalt weicht ab (echte Aenderung ODER alter "Gespeichert am:"-Praefix) | Inhalt aktualisieren (Titel/Kategorie bleiben) |
| **aktuell** | identisch im Brain | nichts tun |
| **verwaist** | im Brain, aber keine Datei mehr | nur anzeigen (evtl. umbenannt/geloescht) — nicht automatisch loeschen |

Bei „neu" kann eine Notiz „aehnelt bestehendem '<X>'" erscheinen — dann ist es vielleicht ein
Update statt neu: in der Uebersicht den Titel auf `<X>` setzen, dann wird es ein sauberes
Ueberschreiben statt eines Duplikats.

## Schritt 1 — Uebersicht zeigen & freigeben lassen

Dem Benutzer die Funde als **kompakte Uebersicht** zeigen (sein gewaehlter Modus: erst Uebersicht,
dann freigeben), getrennt nach **neu** und **geaendert**, je mit vorgeschlagenem **Titel** und
**Kategorie**. Kurz zusammenfassen, wie viele „aktuell" sind und ob „Gespeichert am:"-Praefix-Reste
sauber neu abgelegt werden. Dann:

- **Neue Dateien:** Titel-Vorschlag + Kategorie-Vorschlag pro Datei. Der Benutzer nimmt an oder
  korrigiert (Titel frei eintippbar; Kategorie ist fast immer automatisch korrekt). Bei einem ganz
  neuen Bereich (noch kein `<Label>` im Brain) die neue Unterkategorie kurz bestaetigen lassen.
- **Geaenderte Dateien:** Titel + Kategorie bleiben (Benutzer-Wahl „nur Inhalt aktualisieren") —
  nur eine kurze Sammel-Bestaetigung („diese N Dateien haben sich geaendert — aktualisieren?").
- Auf **Titel-Kollisionen** achten (zwei verschiedene Dateien, gleicher Titel): vorab anpassen
  (z.B. Almanach behaelt den Basis-Titel, die Best-Practice bekommt einen Zusatz wie „(Best
  Practices)"), sonst wuerde eine die andere ueberschreiben.

Nach der Freigabe die **plan.json** bauen — am einfachsten mit dem `plan`-Subcommand (uebernimmt
automatisch alle neu + geaenderten aus `scan.json`, mit den vorgeschlagenen Titeln/Kategorien):

```bash
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py plan                   # alle neu + geaendert
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py plan --status neu      # nur neue
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py plan --only android    # nur rel-Substring
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py plan --exclude rohergebnisse  # ausnehmen
```

Danach `~/.cortex-sync/plan.json` bei Bedarf direkt editieren (Korrekturen des Benutzers aus der
Uebersicht): Titel aendern; einen „neu"-Eintrag, der eigentlich ein Update ist, auf den bestehenden
Brain-Titel setzen UND `status` auf `"geaendert"` aendern; einzelne Eintraege rauswerfen. Format
pro Eintrag — `{rel, title, category, status}`:

```json
[
  {"rel": "opencode-setup/rules-opencode/anti-halluzination.md",
   "title": "Anti-Halluzination: erst pruefen, dann behaupten — nichts erfinden",
   "category": "Programmierung/Rules", "status": "neu"},
  {"rel": "bugs/server/qdrant.md",
   "title": "Qdrant (Vektordatenbank im selbst gehosteten Memory-Stack)",
   "category": "Programmierung/Almanache/Server", "status": "geaendert"}
]
```

`status:"neu"` aktiviert den Kollisions-Waechter (bricht bei bestehendem Titel ab); `status:"geaendert"`
erlaubt das bewusste Ueberschreiben am selben Titel.

## Schritt 2 — Ablegen

```bash
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py upload --plan ~/.cortex-sync/plan.json
```

Legt jede Datei per `/store` ab (Volltext 1:1, getrimmt). Pro Eintrag wird OK / ueberschrieben
gemeldet. **Waechter:** bei `status:"neu"` und `replaced=true` bricht es fuer diese Datei ab und
meldet die Titel-Kollision (kein versehentliches Ueberschreiben). Schreibt `~/.cortex-sync/uploaded.json`.

## Schritt 3 — Verifizieren (Vollstaendigkeit grosser Dateien)

```bash
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py verify --sample 2
```

Holt die **N groessten** gerade abgelegten Dateien per `/by-title` ZURUECK aus dem Gehirn und
vergleicht den Text **1:1** mit der Originaldatei — so faellt auf, wenn eine grosse Datei
abgeschnitten oder der Chunk-Zusammenbau kaputt waere (die `chars`-Zahl allein wuerde das nicht
fangen, weil sie die Eingabe-Laenge spiegelt). Prueft zusaetzlich Kategorie und semantische
Auffindbarkeit (`/search`). Bei vielen grossen Dateien `--sample 3` oder mehr. Ergebnis als Tabelle
(„ALLE STICHPROBEN VOLLSTAENDIG" oder PRUEFEN-Zeilen). Bei Abweichung: betroffene Datei erneut
`upload`-en und erneut verifizieren.

## Schritt 4 — Verwaiste alte Eintraege aufraeumen (prune)

Nach der Titel-Zusatz-Regel liegen die alten Eintraege (ohne Zusatz) nach dem Upload als **verwaist**
im Gehirn. Nach einem frischen `scan` (der die Verwaisten neu bestimmt) koennen sie aufgeraeumt werden:

```bash
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py prune            # Dry-Run: nur anzeigen
python ~/.claude/skills/cortex-update/scripts/cortex_sync.py prune --confirm  # in den Papierkorb verschieben
```

**Soft-Delete:** `prune` verschiebt die Verwaisten in den Papierkorb (`DELETE /entry`, im Dashboard
wiederherstellbar) — es loescht NICHT hart. Reihenfolge ist wichtig: **erst** `upload` (neue Titel
ablegen), **dann** frisch `scan`, **dann** `prune` — sonst wuerde Inhalt kurz fehlen. Nur Eintraege
aus der `orphans`-Liste des letzten Scans werden angefasst.

## Erster Lauf = Komplett-Umzug (einmalig)

Weil die Titel-Zusatz-Regel neu ist, ist der erste vollstaendige Lauf ein **einmaliger Umzug** des
Bestands. Empfohlene Reihenfolge:

1. `scan` — zeigt „neu" (echt neu + Umstellung) und „verwaist" (alte ohne Zusatz).
2. **Backup pruefen** (Cortex Backup / taeglicher Snapshot) — vor dem Massen-Schreiben.
3. `plan` → Uebersicht freigeben → `upload` → `verify` (die neuen Titel mit Zusatz ablegen).
4. `scan` erneut → `prune --confirm` (die alten ohne Zusatz in den Papierkorb).
5. `scan` zur Kontrolle: „neu"/„verwaist" sollten jetzt nur noch echte Neuzugaenge/Reste sein.

Danach sind alle Eintraege im Zusatz-Schema; Folge-Laeufe sind klein (nur echte Aenderungen).

## Statusmeldung am Ende

Kurz: wie viele neu abgelegt, wie viele aktualisiert, Verifikations-Ergebnis, offene verwaiste
Eintraege. Beispiel: „Cortex Update: 49 neu, 35 aktualisiert (inkl. 14 Praefix-Bereinigungen),
Stichprobe vollstaendig. 1 verwaister Eintrag (Room-Persistenz) — bitte pruefen."

## Erweiterbarkeit (kuenftige Datenarten)

Eine neue Quelle (z.B. ein weiterer Ordner mit eigener Kategorie) wird einzig durch einen neuen
Eintrag in `SOURCES` (`scripts/cortex_sync.py`) ergaenzt: `root`, `category_tmpl` (mit `{label}`
fuer Bereichs-Unterordner oder fest), `title_mode` (`h1_clean` | `first_line`), `layout`
(`bereich` | `flach`). Braucht eine neue Datenart eine andere Titel-Logik, kommt ein neuer
`title_mode` in `derive_title` dazu. Sonst aendert sich nichts — Scan/Upload/Verify gelten generisch.

## Abgrenzung

- **Repo-Dateien recherchieren/aktualisieren** (Inhalt schreiben) → `almanach-update` /
  `best-practices-update` / `bug-almanach-recherche`. Cortex Update spiegelt nur den FERTIGEN
  Datei-Stand ins Gehirn, es recherchiert nichts und aendert keine Repo-Datei.
- **Einzelne Erinnerung manuell ablegen** → MCP `second-brain` `remember` direkt (fuer eine kleine
  Notiz). Cortex Update ist fuer den Datei-Bestand (Massen-Sync, kontext-schonend).
- **Regeln im Gehirn lesen/laden** → MCP `get_category_item` (siehe Regel
  `second-brain-load-individually`). Cortex Update SCHREIBT, liest nur zum Vergleich.

## Voraussetzungen / Fehlerfaelle

- **WireGuard-Tunnel muss aktiv sein** (Brain nur ueber `10.8.0.1` erreichbar). Schlaegt `scan` mit
  Timeout fehl → Tunnel pruefen.
- `~/SK/second-brain/brain.env` mit `SB_API_KEY` muss vorhanden sein (Skript meldet sonst klar).
- Python (Standardbibliothek genuegt, keine externen Pakete). Auf Windows `python` bzw. `py -3`.
