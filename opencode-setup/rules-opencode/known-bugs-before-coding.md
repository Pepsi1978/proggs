Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird

Pro Technologie-Bereich gibt es einen kuratierten Bug-Almanach: `~/proggs/bugs/<kategorie>/<bereich>.md` (Index: `~/proggs/bugs/README.md`). Er wird PROAKTIV vor der Arbeit gelesen — damit bekannte Fehler gar nicht erst passieren. (Unterschied zu `bug-cases.jsonl`: die wird REAKTIV nach einem Fehler durchsucht.)

## Digest-Modell — 3 Stufen
- **A (vor JEDER echten Arbeit im Bereich):** NUR den Kurzcheck lesen (`Read` mit `limit=80`) — zuerst den Almanach, DANN die zugehoerige `best-practices/.../<bereich>.md` (auch `limit=80`). Erst dann coden.
- **B (ab dem ERSTEN Fehler im Bereich):** SOFORT den VOLLTEXT des Almanachs lesen (`Read` ohne limit). Der Kurzcheck reicht ab jetzt nicht mehr. Zuerst pruefen, ob es ein bekannter Bug ist -> dokumentierte Loesung anwenden.
- **C (Hochrisiko-Bereiche r8, firebase-billing, claude-hooks, claude-config):** schon VORAB den VOLLTEXT lesen.

## Reihenfolge
Immer erst Almanach, dann Best Practices, dann arbeiten.

## Kein Almanach fuer den Bereich
Bei echter Bereichsarbeit: Frank melden ("neuer Bereich X, kein Almanach"), sein OK abwarten, dann den Skill `bug-almanach-recherche` STARTEN (NICHT selbst ad hoc recherchieren — der Skill ist der vollstaendige Weg). Nur bei trivialem Kleinkram ODER wenn Frank gegen die Recherche entscheidet: bewusst die ack-Flag setzen.

## Gilt AUCH fuer die eigene Harness-Arbeit
- Hook (.ps1/.sh) -> `bugs/claude-tooling/claude-hooks.md` (Volltext, Stufe C)
- CLAUDE.md/Regel/Settings/Skill/Command/Agent -> `bugs/claude-tooling/claude-config.md` (Volltext, Stufe C) + Best Practices
- MCP-Server -> `bugs/claude-tooling/mcp-server.md`
- Python-Hilfsskript -> `bugs/claude-tooling/python-windows.md`

## Nach der Aufgabe
Jeden NEU erlebten Bug im passenden Almanach ergaenzen (Bug + Loesung + Versionen).

## Kein Almanach noetig bei
Einzelnem String, Doku, Kommentar, Versions-Bump (trivialer Kleinkram).

## NIEMALS
- An einem Bereich mit Almanach arbeiten, ohne mindestens dessen Kurzcheck zu lesen (Stufe A).
- Nach einem Fehler im Bereich weiterarbeiten ohne den Volltext (Stufe B).
- In einem Hochrisiko-Bereich nur den Kurzcheck lesen (Stufe C verlangt Volltext).
- Bei neuem Bereich selbst ad hoc recherchieren statt den Skill `bug-almanach-recherche` zu starten.
- Einen Bug "loesen", indem Funktionalitaet entfernt wird (Loesungen sind funktionserhaltend).
