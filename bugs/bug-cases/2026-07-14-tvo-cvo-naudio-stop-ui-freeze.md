# TVO/CVO: NAudio-Stop blockiert den WPF-Dispatcher

## Symptom

Nach dem Klick auf Stop fror das Overlay ein. Die Prozesse liefen weiter, aber die Diagnose endete
bei `stop_clicked`; `recording_stop` fehlte und die temporäre WAV-Datei blieb liegen.

## Ursache

`WaveInEvent.StopRecording()` lief synchron auf dem WPF-Dispatcher. WinMM kann bei einem
Geräte- oder Treiberproblem unbegrenzt blockieren. Dadurch blieb nicht nur der Recorder, sondern
die gesamte Oberfläche hängen.

## Fix

- Den nativen Stop außerhalb des UI-Threads ausführen.
- Stop-Aufruf und Wartezeit auf `RecordingStopped` separat auf drei Sekunden begrenzen.
- Den logischen Recorder-Zustand idempotent freigeben.
- Cleanup nach Startfehlern ebenfalls begrenzen und im Hintergrund ausführen.
- Dialoge asynchron stoppen und nur selbst erzeugte temporäre WAV-Dateien löschen.

## Verifikation

- TVO und CVO mit `0 Warnungen, 0 Fehler` gebaut.
- TVO neu deployed; `/recording/status` meldete `busy:false`.
- Fix in TVO und CVO gespiegelt und per `git diff --check` geprüft.

## Referenz

- Fix-Commit: `5f576e794 #47910 - Prevent audio stop from freezing overlays`
- Almanach: `bugs/desktop/voice-pipeline.md`, Abschnitt 3.2
