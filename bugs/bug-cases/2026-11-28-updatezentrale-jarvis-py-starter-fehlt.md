# UpdateZentrale: Jarvis-Verteilung startet nicht, weil der Python-Starter „py“ fehlt (28.11.2026)

> Behoben in UpdateZentrale 1.3.97 (`Providers/JarvisVerteilungAktualisierer.cs`). macOS nicht betroffen (siehe unten).

## Symptom

Karte „Jarvis (HP, Power-PC, Mac)“ rot bei der Prüfung (28.11.2026, 11:55):
`Jarvis-Verteilung ohne Ergebnis: Start fehlgeschlagen: An error occurred trying to start process 'py' with working directory 'C:\Users\barwa\proggs'. Das System kann die angegebene Datei nicht finden.`

## Ursache

- Python ist auf diesem Rechner nur über den **Python Install Manager** (Store-Paket `PythonSoftwareFoundation.PythonManager`) installiert. Die Laufzeit liegt unter `%LOCALAPPDATA%\Python\pythoncore-3.14-64\python.exe`, Verknüpfungen unter `%LOCALAPPDATA%\Python\bin\`.
- Den klassischen Launcher `py.exe` (winget `Python.Launcher`) gibt es nicht: Die Karte „Python Launcher“ steht auf „nicht installiert“. Der Install Manager legt nur `pymanager.exe`, `pyw.exe` und `python*.exe` als App-Ausführungsaliase in `WindowsApps` an.
- `python.exe` in `WindowsApps` war zum Zeitpunkt der Prüfung der **Store-Platzhalter** („Python wurde nicht gefunden; ohne Argumente ausführen, um aus dem Microsoft Store zu installieren“, Exit 49). Der Aufruf von `pymanager list` meldete dabei „Python install manager was successfully updated to 26.3“. Der Manager hatte sich also gerade selbst aktualisiert, und die Aliase waren dabei offenbar ausgefallen.
- `JarvisVerteilungAktualisierer.Python()` suchte nur `%LOCALAPPDATA%\Programs\Python\Python3*` (klassischer Installer) und fiel sonst blind auf den Namen `py` zurück. Mit dem Install Manager traf keiner der beiden Wege.

## Behebung (Vorbeugung für die ganze Fehlerart: Interpreter nur über einen Starter im PATH erreichbar)

`Python()` liefert jetzt ein echtes `python.exe` mit festem Pfad:
1. Ordnersuche über alle drei Wurzeln **gemeinsam** nach Nebenversion sortiert (als Zahl: 3.14 vor 3.9; ein altes `Python38` schlägt kein neueres `pythoncore-3.14`). Wurzeln: klassischer Installer `%LOCALAPPDATA%\Programs\Python\Python3xx`, Install Manager `%LOCALAPPDATA%\Python\pythoncore-3.xx-64|-arm64|-32` (ohne Free-Threading `3.xxt`), systemweit `Program Files\Python3xx`. Bei gleicher Version zuerst 64 Bit, 32 Bit zuletzt. Unlesbare Ordner und überlange Ziffern werden übersprungen, statt die Prüfung abstürzen zu lassen.
2. `%LOCALAPPDATA%\Python\bin\python.exe`
3. PATH (Einträge ohne Anführungszeichen, Umgebungsvariablen aufgelöst): zuerst `py.exe` in allen Einträgen, auch als Alias in `WindowsApps`; danach `python3.exe`/`python.exe` nur nach einem Probelauf (`sys.version_info>=(3,8)` und `win32`, höchstens 10 s). So fallen der Store-Platzhalter, ein Python 2 oder ein MSYS-Python durch, und ein echtes Store-Python bleibt nutzbar.

Der Treffer wird zwischengespeichert, solange die Datei existiert. Dadurch nutzen `einrichten` und `pruefen` denselben Interpreter. Wird nichts gefunden, meldet die Karte „Python nicht gefunden (gesucht: …)“ statt der nichtssagenden Startausnahme. Die Ordnersuche ist mit Tests abgedeckt (`Tests/JarvisPythonSucheTests.cs`, die Muster sind Konstanten, die die Tests direkt nutzen).

Prüfung danach (28.11.2026, 12:01): `Python()` → `pythoncore-3.14-64\python.exe`; `einrichten` legt die Hintergrundaufgabe „JarvisVerteilung“ mit `pythonw.exe` an (sie fehlte bis dahin, weil `einrichten` nie lief); `pruefen` endet mit `JARVIS_VERTEILUNG_STATUS=geprueft`, Exit 0.

## Merksatz

Für Hintergrundprozesse unter Windows nie `py` oder `python` als nackten Namen starten: `py` fehlt beim Install Manager, `python` in `WindowsApps` kann der Store-Platzhalter sein, und Aliase fallen bei Updates des Managers kurzzeitig aus. Immer den Pfad zur `python.exe` auflösen.

## macOS

Die Mac-Zentrale ruft fest `/usr/bin/python3` auf (`UpdaterZentrale-macOS/UpdaterZentrale/Providers/JarvisVerteilungAktualisierer.swift`). Das gehört zum System. Dieselbe Ursache gibt es dort nicht.
