# Windows-CLIs über WSL/tmux Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Pfade/Argumente an WSL | `wsl --exec` + getrennte Argumente, nie `sh -c` | §1 |
| 2 | Windows-Programm im tmux-Pane | `-e WSL_INTEROP=/run/WSL/1_interop`, nie am kurzlebigen `wsl.exe` hängen | §2 |
| 3 | Start dauert mehrere Sekunden | versteckter Halteprozess mit `tmux wait-for`, danach freigeben | §3 |
| 4 | Erfolg melden | CIM auf exakten inneren Skriptnamen + Stabilität, erst dann `attach` | §4 |
| 5 | Start scheitert | Sitzung beenden, Backoff, max. 3 Versuche, dann sichtbarer Fehler | §5 |
| 6 | Tests | eigener frischer tmux-Socket, Mutationsprobe | §6 |
