# UpdateZentrale: „Nicht verifiziert“ nach `claude update`, weil die neue claude.exe einige Sekunden lang nicht startbar war (28.11.2026)

> Behoben in UpdateZentrale 1.3.98 (`Services/UpdateKette.cs`) und UpdaterZentrale macOS 1.0.62 (`Services/UpdateKette.swift`).

## Symptom

Karte „Claude Code CLI“ (claude-cli, Art cli) im Sammellauf 28.11.2026, 11:34: `claude update` meldet „Successfully updated from 2.1.318 to version 2.1.332“, Exit 0. Danach scheitern alle vier Nachprüfungen (`claude.cmd --version`, je 3 s Abstand, ~9 s) mit Exit 1:
`Die Version von …\npm\node_modules\@anthropic-ai\claude-code\bin\claude.exe ist mit der ausgeführten Windows-Version nicht kompatibel.`
Ergebnis: „Nicht verifiziert – Der Stand ließ sich nach dem Update nicht ermitteln“. Um 11:55 las die Prüfung ohne Zutun 2.1.332. Das Update war also angekommen.

## Ursache

- Das npm-Paket `@anthropic-ai/claude-code` legt die native `bin\claude.exe` erst in seinem `postinstall` (`node install.cjs`) ab. Direkt nach dem Update ist die Datei für kurze Zeit unvollständig oder noch nicht freigegeben, etwa weil sie noch geschrieben oder vom Virenscanner geprüft wird. Windows meldet für so eine Datei ERROR_BAD_EXE_FORMAT („nicht kompatibel“).
- Die Nachprüfung der Update-Kette gab nach `MaxNachpruefungen` × `Pause` (4 × 3 s) auf. Diese Frist reicht, solange der Stand lesbar ist, aber nicht, solange die neue Programmdatei noch gar nicht startet.

## Behebung (für die ganze Fehlerart: Stand direkt nach dem Update vorübergehend unlesbar)

Sind alle regulären Nachprüfungen unlesbar, obwohl der Stand vor dem Lauf lesbar war, liest die Kette weiter: alle 10 s, höchstens 90 s lang (`UnlesbarNachfrist`, `UnlesbarPause`). Begrenzt wird die Nachfrist doppelt, über die Zahl der Runden und über die Uhr, damit ein langsamer Dry-Run die Wartezeit nicht vervielfacht. Im Protokoll steht „Der Stand ist gleich nach dem Update nicht lesbar … Warte bis zu 90 s“ und danach „Stand nach N s lesbar“ oder „blieb N s lang unlesbar“. Bleibt er unlesbar, ist das Urteil wie bisher „Nicht verifiziert“. Ein unlesbarer Stand wird also nie als Erfolg gewertet.

Gilt für alle Arten (cli, npm, winget, reposkript …), nicht nur für Claude Code. Hatte ein Programm schon vorher keinen lesbaren Stand, kostet die Nachfrist keine Zeit.

## Merksatz

Nach einem Selbst-Update kann die neue Programmdatei einige Sekunden lang unbrauchbar sein, wenn ein postinstall sie noch ablegt oder ein Virenscanner sie prüft. Ein Fehler beim Lesen der Version direkt danach heißt nicht „Update kaputt“. Es braucht eine eigene, längere Nachfrist, bevor das Urteil fällt.

## macOS

Dieselbe Nachprüfschleife (4 × 3 s) gibt es in `UpdaterZentrale-macOS`. Ein npm-postinstall, das die native Datei ablegt, oder eine Gatekeeper-Prüfung kann dort genauso kurz blockieren. Die Nachfrist wurde deshalb mitgezogen.
