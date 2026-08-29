# -*- coding: utf-8 -*-
"""Umgebungsvariablen. Sie gehoeren zum Bereich Config, weil sie dieselbe Aufgabe erfuellen."""

ENV_VARS = [
 ("ANTHROPIC_API_KEY", "Anmeldung",
  "Der Schluessel, mit dem sich Claude Code anmeldet.",
  "API key sent as X-Api-Key header. When set, this key is used instead of your subscription",
  "Ist diese Variable gesetzt, wird sie benutzt — auch dann, wenn du ueber `/login` angemeldet "
  "bist. Dein Abo bleibt also ungenutzt und es wird nach Verbrauch abgerechnet.\n\n"
  "Das ist eine haeufige Ursache fuer unerwartete Kosten: Der Schluessel steht noch aus einem "
  "alten Versuch in der Umgebung, und niemand denkt daran."),

 ("ANTHROPIC_AUTH_TOKEN", "Anmeldung",
  "Eigener Wert fuer die Berechtigungs-Kopfzeile.",
  "Custom value for the Authorization header (prefixed with Bearer)",
  "Fuer Umgebungen, in denen die Anmeldung ueber einen eigenen Zugangspunkt laeuft. Der Wert "
  "wird mit dem Vorsatz Bearer verschickt.\n\n"
  "Braucht man nur, wenn ein Zwischendienst die Anfragen entgegennimmt."),

 ("ANTHROPIC_BASE_URL", "Anmeldung",
  "Leitet alle Anfragen ueber eine andere Adresse.",
  "Override the API endpoint to route requests through a proxy or gateway",
  "Damit gehen die Anfragen nicht direkt zu Anthropic, sondern ueber deinen eigenen "
  "Zwischendienst.\n\n"
  "Ueblich in Unternehmen, die den Verkehr protokollieren oder filtern muessen."),

 ("ANTHROPIC_DEFAULT_MODEL", "Modell und Antworten",
  "Das Modell, mit dem neue Sitzungen starten.",
  "Model that new sessions start on by default. Requires Claude Code v2.1.236 or later",
  "Die Entsprechung zur Einstellung `model`, nur als Umgebungsvariable. Praktisch, wenn "
  "verschiedene Projekte unterschiedliche Modelle brauchen.\n\n"
  "Erst ab Version 2.1.236 vorhanden — in aelteren Fassungen wird sie stillschweigend ignoriert."),

 ("ANTHROPIC_MODEL", "Modell und Antworten",
  "Name der zu benutzenden Modell-Einstellung.",
  "Name of the model setting to use",
  "Der aeltere Weg, das Modell ueber die Umgebung zu setzen.\n\n"
  "Fuer neue Einrichtungen ist `ANTHROPIC_DEFAULT_MODEL` oder die Einstellung `model` die "
  "klarere Wahl."),

 ("ANTHROPIC_BETAS", "Modell und Antworten",
  "Schaltet zusaetzliche, noch nicht endgueltige Funktionen frei.",
  "Comma-separated list of additional anthropic-beta header values",
  "Manche Funktionen sind vorhanden, aber noch nicht allgemein freigegeben. Mit dieser Liste "
  "schaltest du sie fuer deine Anfragen dazu.\n\n"
  "Vorsicht: Erprobungsfunktionen koennen sich ohne Ankuendigung aendern oder verschwinden."),

 ("ANTHROPIC_CUSTOM_HEADERS", "Anmeldung",
  "Fuegt eigene Kopfzeilen an jede Anfrage an.",
  "Custom headers to add to requests (Name: Value format)",
  "Manche Zwischendienste verlangen zusaetzliche Angaben in jeder Anfrage — eine Kostenstelle, "
  "eine Projektkennung.\n\n"
  "Mehrere Kopfzeilen werden durch Zeilenumbrueche getrennt."),

 ("API_TIMEOUT_MS", "Netz",
  "Wie lange auf eine Antwort gewartet wird.",
  "Timeout for API requests in milliseconds (default: 600000, or 10 minutes)",
  "Voreingestellt sind zehn Minuten. Bei sehr langen Ueberlegungen kann das knapp werden.\n\n"
  "Ein zu kleiner Wert bricht gute Antworten ab, ein zu grosser laesst dich bei einer echten "
  "Stoerung lange warten."),

 ("BASH_DEFAULT_TIMEOUT_MS", "Arbeitsumgebung",
  "Standard-Zeitgrenze fuer ausgefuehrte Befehle.",
  "Default timeout for long-running bash commands (default: 120000, or 2 minutes)",
  "Nach dieser Zeit wird ein Befehl abgebrochen. Voreingestellt sind zwei Minuten.\n\n"
  "Bei langen Bauvorgaengen zu wenig — dann laeuft der Build ins Leere, obwohl er in Ordnung war."),

 ("BASH_MAX_TIMEOUT_MS", "Arbeitsumgebung",
  "Die groesste Zeitgrenze, die das Modell selbst setzen darf.",
  "Maximum timeout the model can set for bash commands (default: 600000, or 10 minutes)",
  "Claude kann fuer einen einzelnen Befehl mehr Zeit anfordern. Diese Variable begrenzt, "
  "wie viel.\n\n"
  "So kann ein haengender Befehl die Sitzung nicht unbegrenzt blockieren."),

 ("BASH_MAX_OUTPUT_LENGTH", "Arbeitsumgebung",
  "Wie viele Zeichen Ausgabe ein Befehl liefern darf.",
  "Maximum number of characters of bash output (default: 30000; maximum: 150000)",
  "Laengere Ausgaben werden gekappt. Voreingestellt sind 30 000 Zeichen.\n\n"
  "Ein hoeherer Wert hilft bei ausfuehrlichen Protokollen, fuellt aber auch schneller das "
  "Gedaechtnis — hier steckt der eigentliche Preis."),

 ("CLAUDE_CONFIG_DIR", "Einstellungen",
  "Verlegt den gesamten Einstellungsordner.",
  "Use a different configuration directory",
  "Damit bekommt jede Umgebung ihre eigene Welt: eigene Einstellungen, eigene Skills und — "
  "das ist der Punkt, den man leicht uebersieht — eine eigene Anmeldung.\n\n"
  "Praktisch, um beruflich und privat sauber zu trennen. Am Mac haengt der Schluesselbund-"
  "Eintrag am Pfad, ein Wechsel bedeutet dort also eine neue Anmeldung."),

 ("CLAUDE_PROJECT_DIR", "Arbeitsumgebung",
  "Der Projektordner, den Hooks und Skills vorfinden.",
  "The project directory available to hooks and skills",
  "Wird von Claude Code gesetzt, damit ein Hook weiss, wo das Projekt liegt — unabhaengig "
  "davon, aus welchem Unterordner er gerufen wurde.\n\n"
  "In eigenen Hooks immer diese Variable benutzen, statt den Pfad fest einzutragen."),

 ("CLAUDE_SESSION_ID", "Arbeitsumgebung",
  "Die Kennung der laufenden Sitzung.",
  "Identifier of the current session",
  "Nuetzlich in Hooks und Skripten, um Protokolle je Sitzung zu trennen oder mehrere "
  "gleichzeitig laufende Sitzungen auseinanderzuhalten.\n\n"
  "Bleibt ueber die ganze Sitzung gleich, auch nach einem Verdichtungsschritt."),

 ("CLAUDE_PLUGIN_ROOT", "Erweiterungen",
  "Der Ordner der gerade laufenden Erweiterung.",
  "Root directory of the running plugin",
  "Eine Erweiterung, die eigene Dateien mitbringt, findet sie darueber — ohne zu wissen, wohin "
  "sie installiert wurde.\n\n"
  "In Erweiterungen immer diese Variable benutzen statt eines festen Pfades."),

 ("CLAUDE_SKILL_DIR", "Erweiterungen",
  "Der Ordner des gerade laufenden Skills.",
  "Directory of the running skill",
  "Das Gegenstueck fuer Skills: Damit findet ein Skill seine Vorlagen, Skripte und "
  "Nachschlagedateien.\n\n"
  "Genau dafuer sind Skills als Ordner statt als einzelne Datei gedacht."),

 ("CLAUDECODE", "Arbeitsumgebung",
  "Steht auf 1 in allen Prozessen, die Claude Code startet.",
  "Set to 1 in subprocesses Claude Code spawns",
  "Damit kann ein Skript erkennen, dass es von Claude Code aufgerufen wurde und nicht von "
  "einem Menschen.\n\n"
  "Nuetzlich, um Rueckfragen zu ueberspringen oder die Ausgabe knapper zu halten."),

 ("CLAUDE_CODE_ENABLE_TELEMETRY", "Auswertung",
  "Schaltet die Messdaten-Ausgabe ein.",
  "Enable OpenTelemetry metrics and events",
  "Damit lassen sich Nutzung und Kosten in einem eigenen Auswertungssystem sammeln.\n\n"
  "Vor allem fuer Organisationen interessant, die den Einsatz nachvollziehen wollen."),

 ("CLAUDE_CODE_DISABLE_THINKING", "Modell und Antworten",
  "Schaltet das ausfuehrliche Nachdenken ab.",
  "Disable extended thinking",
  "Antworten kommen schneller und guenstiger, sind bei schwierigen Aufgaben aber deutlich "
  "schwaecher.\n\n"
  "Vorsicht in Verbindung mit hohen Denkstufen — diese Kombination war bei Opus 5 lange ein "
  "Fehlerfall und wird seit Version 2.1.251 selbst abgefangen."),

 ("CLAUDE_CODE_DISABLE_AUTO_MEMORY", "Kontext und Gedaechtnis",
  "Schaltet das automatische Gedaechtnis ab.",
  "Disable auto memory",
  "Claude merkt sich dann nichts mehr von allein. Deine `CLAUDE.md`-Dateien gelten weiterhin.\n\n"
  "Die Entsprechung als Einstellung heisst `autoMemoryEnabled`."),

 ("CLAUDE_CODE_USE_POWERSHELL_TOOL", "Arbeitsumgebung",
  "Laesst Befehle unter Windows ueber PowerShell laufen.",
  "Use PowerShell for shell commands on Windows",
  "Unter Windows gibt es zwei Welten mit vollstaendig verschiedener Schreibweise. Diese "
  "Variable legt fest, welche benutzt wird.\n\n"
  "Wichtig, damit Pfade, Variablen und Umleitungen so geschrieben werden, wie die gewaehlte "
  "Kommandozeile sie versteht."),

 ("CLAUDE_AUTOCOMPACT_PCT_OVERRIDE", "Kontext und Gedaechtnis",
  "Legt fest, ab welchem Fuellstand verdichtet wird.",
  "Set the percentage (1-100) at which auto-compaction triggers",
  "Ein Wert in Prozent. Bei 80 wird verdichtet, sobald das Gedaechtnis zu vier Fuenfteln voll "
  "ist.\n\n"
  "Frueher verdichten heisst schneller und guenstiger, spaeter heisst mehr Einzelheiten behalten."),

 ("CLAUDE_AFK_TIMEOUT_MS", "Arbeitsweise",
  "Wartezeit, nach der eine unbeantwortete Rueckfrage von allein weiterlaeuft.",
  "How many milliseconds of idle time before AskUserQuestion auto-continues",
  "Sitzt niemand am Rechner, kann die Arbeit sonst beliebig lange stehen.\n\n"
  "Der Preis ist, dass eine Entscheidung ohne dich faellt. Fuer wichtige Fragen ist Warten "
  "die bessere Wahl."),

 ("CLAUDE_AFK_COUNTDOWN_MS", "Arbeitsweise",
  "Wie lange vorher der Countdown sichtbar wird.",
  "How many milliseconds before auto-continue the countdown appears (default: 20000)",
  "Die Vorwarnung, bevor von allein weitergemacht wird. So kann man noch eingreifen, wenn man "
  "gerade zurueckkommt.\n\n"
  "Zu kurz gesetzt, sieht man sie nicht mehr rechtzeitig."),

 ("CLAUDE_CODE_SIMPLE", "Arbeitsumgebung",
  "Schaltet Claude Code in einen abgespeckten Zustand.",
  "Set by --bare: skip hooks, LSP, plugin sync, auto-memory and more",
  "Wird vom Schalter `--bare` gesetzt. Hooks, Sprachdienste, Erweiterungs-Abgleich, "
  "automatisches Gedaechtnis und die Suche nach `CLAUDE.md` fallen weg.\n\n"
  "Gedacht fuer Automatisierung und fuer die Fehlersuche: Wenn es abgespeckt laeuft, liegt das "
  "Problem in einem der abgeschalteten Teile."),

 ("MCP_TIMEOUT", "Integrationen",
  "Wie lange auf den Start eines Zusatzdienstes gewartet wird.",
  "Timeout for MCP server startup",
  "Startet ein Dienst langsam, wird er sonst als nicht erreichbar aufgegeben.\n\n"
  "Beim Einrichten eines eigenen Dienstes ist das eine der ersten Stellschrauben."),

 ("MCP_TOOL_TIMEOUT", "Integrationen",
  "Wie lange ein einzelner Aufruf an einen Zusatzdienst dauern darf.",
  "Timeout for MCP tool calls",
  "Fragt Claude einen angeschlossenen Dienst und der antwortet nicht, haengt sonst die ganze "
  "Sitzung.\n\n"
  "Ein knapper Wert haelt die Arbeit fluessig, ein zu knapper bricht langsame, aber gueltige "
  "Abfragen ab."),

 ("DISABLE_AUTOUPDATER", "Aktualisierung",
  "Schaltet die automatische Aktualisierung ab.",
  "Turn off automatic updates",
  "Claude Code haelt sich sonst selbst aktuell. In verwalteten Umgebungen soll das oft die "
  "Softwareverteilung uebernehmen.\n\n"
  "Denk daran, dann selbst fuer Aktualisierungen zu sorgen — sonst bleibst du auf einer alten "
  "Fassung sitzen."),

 ("NO_COLOR", "Darstellung",
  "Schaltet Farben in der Ausgabe ab.",
  "Disable colored output; affects subprocesses from v2.1.143",
  "Ein weit verbreiteter Standard, den viele Programme beachten.\n\n"
  "Wichtige Aenderung: Seit Version 2.1.143 wirkt die Variable nur noch fuer die von Claude "
  "Code gestarteten Programme, nicht mehr fuer dessen eigene Anzeige."),

 ("FORCE_COLOR", "Darstellung",
  "Erzwingt Farben, auch wenn sie sonst weggelassen wuerden.",
  "Force colored output; affects subprocesses from v2.1.143",
  "Nuetzlich, wenn die Ausgabe durch eine Pipe laeuft und die Programme deshalb auf Farbe "
  "verzichten.\n\n"
  "Wie bei `NO_COLOR` gilt seit Version 2.1.143: nur noch fuer die gestarteten Programme."),

 ("HTTP_PROXY", "Netz",
  "Leitet den Verkehr ueber einen Zwischendienst.",
  "Route HTTP traffic through a proxy",
  "In vielen Firmennetzen der einzige Weg nach draussen.\n\n"
  "Zusammen mit `HTTPS_PROXY` und `NO_PROXY` setzen, sonst gehen manche Anfragen am "
  "Zwischendienst vorbei und laufen ins Leere."),

 ("HTTPS_PROXY", "Netz",
  "Leitet den verschluesselten Verkehr ueber einen Zwischendienst.",
  "Route HTTPS traffic through a proxy",
  "Das Gegenstueck zu `HTTP_PROXY` fuer verschluesselte Verbindungen — und damit fuer praktisch "
  "alles, was Claude Code tut.\n\n"
  "Prueft der Zwischendienst den verschluesselten Inhalt, braucht das System zusaetzlich dessen "
  "Wurzelzertifikat."),

 ("NO_PROXY", "Netz",
  "Nennt Adressen, die am Zwischendienst vorbeigehen.",
  "Hosts that bypass the proxy",
  "Typischerweise der eigene Rechner und Adressen im internen Netz.\n\n"
  "Ohne diesen Eintrag versucht Claude Code auch den lokalen Entwicklungsserver ueber den "
  "Zwischendienst zu erreichen — und scheitert."),
]
