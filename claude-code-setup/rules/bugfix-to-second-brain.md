# Funktionierende Bugfixes ins zweite Gehirn (Cortex) speichern (KRITISCH)

> Ergaenzt Direktive #3 um einen festen Ablage-Schritt: jeder BESTAETIGT funktionierende Bugfix wird
> zusaetzlich als strukturierter Fall ins zentrale Second Brain (Cortex, `second-brain`-MCP) geschrieben —
> EIN gemeinsames Fehler-Gedaechtnis ueber alle Werkzeuge (Claude Code UND OpenCode/Codex).

## Die eine Regel

Sobald ein Bugfix als FUNKTIONIEREND gilt, wird er als ein Eintrag ins Second Brain geschrieben — im
festen Format, unter `bugfixes/<unterkategorie>`, mit Titel inkl. Datum UND Uhrzeit. Nur funktionierende
Fixes, nie ein unbestaetigter. Ersetzt NICHT die lokale Bug-Doku (`~/proggs/bugs/` = proaktiver Almanach),
sondern ergaenzt sie (das Gehirn = reaktiv abrufbare Fall-Akte).

## Wann gilt ein Fix als FUNKTIONIEREND

| Situation | Vorgehen |
|-----------|----------|
| Objektiv verifizierbar (Build gruen, Tests bestanden, Deploy `healthy`, Symptom reproduzierbar WEG) | selbst verifizieren → direkt speichern |
| Nur der Benutzer kann es beurteilen (Optik/UI) ODER unsicher | EINMAL fragen "Hat der Fix funktioniert?" → erst bei Ja speichern |

Default ist NICHT speichern bis bestaetigt. Sagt Frank spaeter "hat nicht funktioniert": den Eintrag
per `forget` entfernen, erst nach dem echten Fix neu schreiben.

## Format

**Titel:** `Bugfix <App> <Bereich> <YYYY-MM-DD HH:MM>` (Uhrzeit = lokale Uhr Europe/Berlin im Moment des
Speicherns; Titel fuer Menschen sofort verstaendlich).
**Kategorie:** `bugfixes/<unterkategorie>` — ZUERST pruefen ob eine sinnvolle existiert (MCP `list_memories`/
`get_by_category`), dort einordnen; nur sonst eine neue sprechende anlegen (klein-mit-Bindestrich).
**Inhalt (Klartext):** `Bugfix <YYYY-MM-DD HH:MM>: <App> <Bereich>. Symptom: … Root Cause: <konkret,
Datei/Funktion/CSS-Klasse>. Fix: <was genau, mit Stelle>. Verwandte Pruefung: … Verifikation: <wie
bestaetigt>. Funktionalitaets-Diff: <was bleibt erhalten>. [Poka-Yoke: …].`

## Ablauf

1. Fix nach Direktive #3 (Root Cause, funktionserhaltend, verifizieren). 2. Pruefen: funktioniert er?
(objektiv ODER Benutzer-Ja; sonst fragen). 3. Passende `bugfixes/<unterkategorie>` finden/anlegen.
4. Ueber `second-brain`-MCP `remember` speichern (Titel + Kategorie + Text). 5. Frank in EINEM Satz
melden: "Im Gehirn dokumentiert: <Titel> [<Kategorie>]."

> Ist der `second-brain`-MCP gerade NICHT verbunden: Eintrag nicht verlieren — kurz melden, auf den
> naechsten Moment / die lokale Bug-Doku ausweichen.

## Was NIEMALS passieren darf

- Einen unbestaetigten Bugfix speichern · den Doku-Schritt nach funktionierendem Fix weglassen
- Ein abweichendes Format/Titel-Schema nutzen · blind eine neue Unterkategorie anlegen (obwohl passende existiert)
- Einen kryptischen Titel waehlen (Mensch weiss nicht mehr worum es ging)
