# OpenLauncher unter Windows

## Optional mit tmux starten

Neben den Startbuttons lässt sich **tmux** einschalten. Diese gespeicherte Auswahl
gilt für **alle Profile und Modi**, für Anthropic-Modelle in Claude Code, OpenAI-Modelle
in Codex CLI oder OpenCode sowie alle übrigen von OpenCode unterstützten Anbieter und
Modelle. Modell, Anbieter, Profil und Denkstufe werden weiterhin wie bisher gewählt.

Voraussetzung: WSL mit einer Benutzer-Distribution als Standard (hier Ubuntu), tmux
in dieser Distribution und aktivierte Windows-Interop. Die CLIs selbst bleiben
Windows-Programme: bestehende Anmeldungen, Profile, Hooks und Build-Werkzeuge werden
weiterverwendet. Eine zusätzliche Linux-Installation der CLIs ist nicht nötig.

- **Start:** öffnet die gewählte CLI im Windows-Terminal, optional innerhalb von tmux.
- **Start (Codex):** kopiert weiterhin den Claude-Startbefehl für eine freie Shell im
  eingebauten Codex-Terminal; bei aktivierter Option enthält er den tmux-Start.
- Jeder neue Start erzeugt eine eigene Sitzung. Derselbe kopierte tmux-Befehl hängt
  eine noch laufende Sitzung wieder an. `Strg+B`, danach `D`, trennt die Anzeige.
- Ohne Häkchen gilt der bisherige Standard-Terminalstart. Bei fehlender WSL-/tmux-
  Voraussetzung erscheint ein Fehler; es wird nicht unbemerkt ohne tmux gestartet.

Der Socket heißt `openlauncher`; die Distribution und die eindeutige Sitzung stehen
im erzeugten temporären Wrapper. Diese Dateien bleiben zum Wiederanhängen erhalten.
Endet die CLI, endet auch ihre tmux-Sitzung. Ein alter Befehl ersetzt keine beendete
Sitzung: für einen neuen Start den Launcher verwenden.

Die macOS-Version bietet dieselbe unabhängige Terminalwahl und nutzt dort native
tmux-Prozesse. Unter Windows läuft tmux in WSL, die CLI über Windows-Interop.
