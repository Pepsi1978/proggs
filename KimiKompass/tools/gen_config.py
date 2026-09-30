# -*- coding: utf-8 -*-
from gen_slash import E, write  # re-uses helpers (also regenerates slash file)

C = []
CT, TT, UV = "config.toml", "tui.toml", "Umgebungsvariable"
def c(name, kat, kurz, eng, erk, art=CT, **kw):
    C.append(E(name, kat, art, kurz, eng, erk, **kw))

# ================= config.toml: oberste Ebene =================
c("default_model", "Modellauswahl", "Legt fest, welches Modell beim Start benutzt wird.",
  "LLM model alias to use for this invocation. Defaults to default_model in config.toml.",
  "Jedes Modell hat in der `config.toml` einen Kurznamen, den Alias. Beispiel: `default_model = \"kimi-code/k3\"`.\n\nDieser Alias wird bei jedem Start benutzt. Für einen einzelnen Aufruf überschreibst du ihn mit `kimi -m <alias>`.\n\nWählst du in `/model` ein Modell mit Enter, schreibt Kimi den neuen Wert selbst hierher.")
c("default_provider", "Modellauswahl", "Legt den Standard-Anbieter fest.",
  "defaultProvider",
  "Hast du mehrere Anbieter eingerichtet, bestimmt dieser Schlüssel, welcher als Standard gilt.\n\nDer Wert ist der Name eines Eintrags unter `[providers]`. Kimi pflegt ihn selbst, wenn du über `/provider` oder `/login` arbeitest.")
c("default_permission_mode", "Berechtigungen", "Freigabe-Modus, mit dem jede Sitzung startet.",
  "defaultPermissionMode: enum([\"manual\", \"auto\", \"yolo\"])",
  "Erlaubt sind drei Werte. `manual` heißt „Always Ask“: Kimi liest selbst, fragt aber vor allem anderen. `yolo` heißt „Ask When Needed“: Alltägliches läuft allein, Riskantes wird gefragt. `auto` heißt „Never Ask“: Kimi fragt nie.\n\nBeispiel: `default_permission_mode = \"yolo\"`, wenn dich die vielen Rückfragen stören.\n\nIn der laufenden Sitzung wechselst du mit `/permission`, `/yolo` oder `/auto`.")
c("default_plan_mode", "Arbeitsweise", "Startet jede Sitzung im Planungsmodus.",
  "defaultPlanMode: boolean (default false)",
  "Steht hier `true`, beginnt jede neue Sitzung im Planungsmodus. Kimi darf dann erst nur lesen und einen Plan schreiben.\n\nDas passt, wenn du dir angewöhnen willst, vor jeder Änderung einen Plan zu sehen. Standard ist `false`. Einmalig geht das auch mit `kimi --plan`.")
c("extra_skill_dirs", "Skills", "Zusätzliche Ordner, in denen Kimi nach Skills sucht.",
  "extraSkillDirs: array(string) (default [])",
  "Kimi Code sucht Skills von selbst in `~/.kimi-code/skills`, `~/.agents/skills` und im Projekt unter `.kimi-code/skills` und `.agents/skills`.\n\nLiegen deine Skills woanders, etwa in einem gemeinsamen Team-Ordner, trägst du ihn hier ein. Beispiel: `extra_skill_dirs = [\"D:/team/skills\"]`.")
c("extra_agent_dirs", "Agenten", "Zusätzliche Ordner mit Agenten-Profilen.",
  "extraAgentDirs: array(string) (default [])",
  "Agenten-Profile sind Markdown-Dateien, die eine Rolle beschreiben, etwa „Prüfer“ oder „Übersetzer“. Beim Start wählst du eins mit `kimi --agent <name>`.\n\nHier trägst du weitere Ordner ein, in denen Kimi nach solchen Profilen suchen soll.")
c("merge_all_available_skills", "Skills", "Ob Kimi alle eigenen Skill-Ordner zusammenführt.",
  "mergeAllAvailableSkills: boolean (default true)",
  "Bei `true` (Standard) nimmt Kimi alle vorhandenen Kimi-eigenen Skill-Ordner zusammen. Bei `false` nimmt es nur den ersten, den es findet.\n\nDie meisten Nutzer müssen hier nichts ändern.")
c("builtin_product_skills", "Skills", "Schaltet die eingebauten Produkt-Skills ein oder aus.",
  "builtinProductSkills: boolean (default true) — env KIMI_CODE_BUILTIN_PRODUCT_SKILLS",
  "Kimi Code bringt Skills mit, die sich um das Programm selbst drehen, zum Beispiel `/check-kimi-code-docs`, `/update-config`, `/mcp-config`, `/custom-theme` und `/import-from-cc-codex`.\n\nMit `builtin_product_skills = false` blendest du sie aus. Die Umgebungsvariable `KIMI_CODE_BUILTIN_PRODUCT_SKILLS` hat Vorrang.")

# ================= providers =================
P = "Anbieter"
c("providers.<name>.type", P, "Art des Anbieters, zum Beispiel \"kimi\".",
  "type: string",
  "Jeder Anbieter steht in einem eigenen Abschnitt, etwa `[providers.\"managed:kimi-code\"]`. `type` sagt, welche Schnittstelle er spricht.\n\nBei der Anmeldung über Kimi steht hier `type = \"kimi\"`. Einträge legst du am einfachsten über `/provider` an, nicht von Hand.")
c("providers.<name>.base_url", P, "Adresse der Schnittstelle des Anbieters.",
  "baseUrl: string",
  "Die Basisadresse (Base URL) ist die Internetadresse, an die Kimi seine Anfragen schickt. Beim Kimi-Abo ist das zum Beispiel `https://api.kimi.ai/coding/v1`.\n\nÄndern musst du sie nur, wenn du einen eigenen oder einen Firmen-Zugang nutzt.")
c("providers.<name>.api_key", P, "API-Schlüssel des Anbieters.",
  "apiKey: string",
  "Ein API-Schlüssel ist ein geheimes Passwort für die Schnittstelle. Wer ihn hat, kann auf deine Kosten Anfragen stellen.\n\nTeile die `config.toml` deshalb nie mit anderen und lege sie nicht in ein öffentliches Git-Repo. Sicherer ist `api_key_env`: Dann steht der Schlüssel nur in einer Umgebungsvariablen.")
c("providers.<name>.api_key_env", P, "Name der Umgebungsvariablen, aus der der API-Schlüssel kommt.",
  "apiKeyEnv: string",
  "Statt den Schlüssel direkt in die Datei zu schreiben, nennst du hier nur den Namen einer Umgebungsvariablen. Beispiel: `api_key_env = \"MEIN_ANBIETER_KEY\"`.\n\nSo bleibt der Schlüssel aus der Datei heraus, und du kannst die Konfiguration gefahrloser weitergeben.")
c("providers.<name>.oauth", P, "Verweis auf gespeicherte Anmeldedaten statt eines festen Schlüssels.",
  "oauth: { storage: \"file\" | \"keyring\", key, oauth_host }",
  "Meldest du dich mit `/login` an, speichert Kimi keinen festen Schlüssel. Stattdessen steht hier ein Verweis auf die gespeicherte Anmeldung.\n\n`storage` sagt, wo sie liegt: `file` (Datei) oder `keyring` (Schlüsselbund des Betriebssystems). `oauth_host` ist die Anmeldeadresse, etwa `https://auth.kimi.ai`. Diese Werte pflegt Kimi selbst.")
c("providers.<name>.model_source", P, "Woher die Modellliste des Anbieters kommt.",
  "modelSource: enum([\"static\", \"discover\", \"oauth-catalog\"])",
  "Erlaubt sind `static`, `discover` und `oauth-catalog`. Bei `static` stehen die Modelle fest in der Datei. Bei `discover` fragt Kimi den Anbieter nach seiner Liste. Bei `oauth-catalog` kommt die Liste aus dem Katalog des angemeldeten Kontos.\n\nNormalerweise setzt Kimi den Wert selbst, wenn du einen Anbieter über `/provider` anlegst.")
c("providers.<name>.custom_headers", P, "Zusätzliche HTTP-Kopfzeilen für jede Anfrage.",
  "customHeaders: record(string, string)",
  "Manche Firmen-Zugänge verlangen besondere Kopfzeilen (Header), zum Beispiel eine Team-Kennung.\n\nDie trägst du hier als Tabelle ein, etwa `custom_headers = { \"X-Team\" = \"app\" }`.")
c("providers.<name>.env", P, "Umgebungsvariablen nur für diesen Anbieter.",
  "env: record(string, string)",
  "Hier legst du Umgebungsvariablen fest, die nur für diesen Anbieter gelten. Im Code wird zum Beispiel `env.KIMI_API_KEY` als dokumentierter Weg für einen Kimi-Schlüssel genannt.")
c("providers.<name>.default_model", P, "Standard-Modell dieses Anbieters.",
  "defaultModel: string",
  "Wählst du einen Anbieter, nimmt Kimi dieses Modell, sofern du kein anderes angibst.")

# ================= models =================
M = "Modellkonfiguration"
c("models.<alias>.provider", M, "Zu welchem Anbieter das Modell gehört.",
  "provider: string",
  "Jedes Modell steht in einem eigenen Abschnitt, etwa `[models.\"kimi-code/k3\"]`. `provider` verweist auf einen Eintrag unter `[providers]`.\n\nWichtig: Enthält der Alias Punkte oder Schrägstriche, muss er in Anführungszeichen stehen. Sonst meldet Kimi, dass der Eintrag kein Modell ist.")
c("models.<alias>.model", M, "Der echte Modellname beim Anbieter.",
  "model: string",
  "Der Alias ist dein Kurzname, `model` der Name, den der Anbieter erwartet. Beispiel: Alias `kimi-code/k3`, `model = \"k3\"`.\n\nFehlt `model`, warnt Kimi: Der Eintrag kann dann nicht als Modell benutzt werden.")
c("models.<alias>.display_name", M, "Anzeigename in der Modellauswahl.",
  "displayName: string",
  "So erscheint das Modell in `/model`. Beispiel: `display_name = \"K3\"`.\n\nDas ändert nichts an der Technik, nur an der Beschriftung.")
c("models.<alias>.max_context_size", M, "Größe des Kontextfensters in Tokens.",
  "maxContextSize: int >= 1",
  "Das Kontextfenster ist das Kurzzeitgedächtnis des Modells. Es wird in Tokens gemessen, das sind kleine Textbausteine. Beispiel: `max_context_size = 1048576` sind rund eine Million Tokens.\n\nKimi nutzt die Zahl, um zu entscheiden, wann es das Gespräch verdichtet.")
c("models.<alias>.max_output_size", M, "Höchstlänge einer Antwort in Tokens.",
  "maxOutputSize: int >= 1",
  "Begrenzt, wie lang eine einzelne Antwort des Modells werden darf. Dazu gibt es passend `max_input_size` für die Eingabe.\n\nMeist musst du hier nichts eintragen.")
c("models.<alias>.capabilities", M, "Liste der Fähigkeiten des Modells.",
  "capabilities: array(string)",
  "Hier steht, was das Modell kann. In der mitgelieferten Konfiguration stehen zum Beispiel `thinking`, `always_thinking`, `image_in`, `video_in`, `tool_use` und `dynamically_loaded_tools`.\n\nKimi schaltet danach Funktionen frei. Ohne `image_in` kannst du dem Modell zum Beispiel keine Bilder zeigen.")
c("models.<alias>.support_efforts", M, "Welche Denkstufen das Modell kennt.",
  "supportEfforts: array(string)",
  "Beispiel: `support_efforts = [\"low\", \"high\", \"max\"]`. Die Reihenfolge geht von schwach nach stark.\n\nGenau diese Stufen bietet dir `/effort` dann zur Auswahl an.")
c("models.<alias>.default_effort", M, "Standard-Denkstufe des Modells.",
  "defaultEffort: string",
  "Mit dieser Denkstufe startet das Modell, wenn du nichts anderes wählst. Beispiel: `default_effort = \"high\"`.\n\nWählst du in `/effort` eine teurere Stufe als diesen Standard, merkt sich Kimi sie nur für die Sitzung. So wird sie nicht aus Versehen zum Dauerzustand.")
c("models.<alias>.overrides", M, "Überschreibt einzelne Werte eines Modells aus dem Katalog.",
  "overrides: ModelBaseSchema.omit(providerId, baseUrl, apiKey, oauth, protocol, name, aliases, provider, model, betaApi).partial()",
  "Kommt ein Modell aus dem Katalog eines Anbieters, kannst du einzelne Werte trotzdem ändern. Dafür gibt es den Unterabschnitt `overrides`.\n\nBeispiel: `[models.\"kimi-code/k3\".overrides]` mit `default_effort = \"low\"`. Adresse, Schlüssel und Modellname kannst du hier nicht ändern.")

# ================= thinking / catalog =================
c("thinking.enabled", "Modellverhalten", "Schaltet das Nachdenken vor der Antwort ein oder aus.",
  "enabled: boolean",
  "Viele Modelle denken vor der Antwort nach. Mit `enabled = false` schaltest du das ab. Die Antworten kommen dann schneller, sind bei schweren Aufgaben aber oft schlechter.\n\nKimi schreibt den Wert selbst, wenn du in `/effort` oder `/model` die Stufe „off“ wählst.")
c("thinking.effort", "Modellverhalten", "Gewählte Denkstufe als Standard.",
  "effort: string",
  "Hier speichert Kimi die Denkstufe, die du in `/effort` mit Enter übernommen hast. Beispiel: `effort = \"high\"`.\n\nFür einen einzelnen Lauf erzwingst du eine Stufe mit der Umgebungsvariablen `KIMI_MODEL_THINKING_EFFORT`.")
c("model_catalog.refresh_on_start", "Modellauswahl", "Lädt den Modellkatalog beim Start neu.",
  "refreshOnStart: boolean — env KIMI_CODE_MODEL_CATALOG_REFRESH_ON_START",
  "Der Modellkatalog ist die Liste der Modelle, die dein Anbieter gerade anbietet. Mit `true` holt Kimi sie bei jedem Start frisch.\n\nDazu gehört `refresh_interval_ms`: Abstand in Millisekunden zwischen zwei automatischen Aktualisierungen.")
c("model_catalog.refresh_interval_ms", "Modellauswahl", "Abstand zwischen zwei Katalog-Aktualisierungen.",
  "refreshIntervalMs: int >= 0 — env KIMI_CODE_MODEL_CATALOG_REFRESH_INTERVAL_MS",
  "Gibt in Millisekunden an, wie oft Kimi die Modellliste neu lädt. 60000 Millisekunden sind eine Minute.")

# ================= Unteragenten =================
UA = "Unteragenten"
c("secondary_model.default_model", UA, "Modell, das Unteragenten standardmäßig benutzen.",
  "defaultModel: string",
  "Unteragenten sind Helfer für Teilaufgaben. Hier legst du fest, welches Modell sie nutzen. Beispiel: ein schnelles Modell wie `kimi-code/kimi-for-coding-highspeed`.\n\nAm bequemsten stellst du das mit `/secondary-model` ein. Für einen einzelnen Lauf gibt es `KIMI_SECONDARY_MODEL`.")
c("secondary_model.models", UA, "Auswahl an Modellen, aus der der Hauptagent für Helfer wählen darf.",
  "models: record(string, string)",
  "Statt eines festen Modells kannst du eine kleine Auswahl anbieten. Der Hauptagent entscheidet dann je Aufgabe, welches passt.\n\nDiese Tabelle verträgt sich nicht mit `force = true`.")
c("secondary_model.force", UA, "Zwingt alle Helfer auf das Standard-Helfermodell.",
  "[secondary_model].force cannot be combined with [secondary_model.models]: the pool table only exists to offer the main agent a choice, and force removes that choice",
  "Mit `force = true` dürfen Unteragenten nur noch `default_model` benutzen. Der Hauptagent hat dann keine Wahl mehr.\n\nDafür muss `default_model` gesetzt sein, und die Tabelle `models` darf nicht gleichzeitig existieren. Sonst meldet Kimi einen Fehler.")
c("subagent.timeout_ms", UA, "Wie lange ein Unteragent höchstens arbeiten darf.",
  "timeoutMs: int >= 0 (default 7200000) — env KIMI_SUBAGENT_TIMEOUT_MS",
  "Die Zeit wird in Millisekunden angegeben. Der Standard 7200000 entspricht zwei Stunden.\n\nDauert ein Helfer länger, bricht Kimi ihn ab. Das schützt vor Helfern, die sich festfahren.")
c("swarm.timeout_ms", UA, "Zeitgrenze für Aufgaben im Schwarm-Modus.",
  "timeoutMs: int >= 0 (default 7200000) — env KIMI_CODE_SWARM_TIMEOUT_MS",
  "Gilt für Aufgaben, die mit `/swarm` auf mehrere Agenten verteilt werden. Standard sind 7200000 Millisekunden, also zwei Stunden.")

# ================= loop_control =================
LC = "Ausführungslimits"
c("loop_control.max_steps_per_turn", LC, "Höchstzahl an Arbeitsschritten pro Runde.",
  "maxStepsPerTurn: int >= 0 — env KIMI_LOOP_MAX_STEPS_PER_TURN",
  "Eine Runde beginnt mit deiner Nachricht und endet mit Kimis Antwort. Dazwischen können viele Schritte liegen: Datei lesen, Befehl ausführen, wieder lesen.\n\nDieser Wert begrenzt die Zahl der Schritte. So kann sich Kimi nicht endlos im Kreis drehen.")
c("loop_control.max_attempts_per_step", LC, "Wie oft ein Schritt bei Fehlern wiederholt wird.",
  "maxAttemptsPerStep: int >= 0 — env KIMI_LOOP_MAX_ATTEMPTS_PER_STEP",
  "Schlägt eine Anfrage an das Modell fehl, etwa wegen einer Netzstörung, versucht Kimi es erneut. Der Code nimmt 10 Versuche, wenn nichts eingestellt ist.\n\nDer frühere Name `max_retries_per_step` wird noch erkannt, ist aber veraltet.")
c("loop_control.max_retries_per_step", LC, "Veraltet: alter Name für max_attempts_per_step.",
  "deprecations: [{ key: \"max_retries_per_step\", replacement: \"max_attempts_per_step\" }]",
  "Dieser Schlüssel wurde umbenannt. Kimi erkennt ihn noch, weist aber auf den neuen Namen hin.\n\nSchreib stattdessen `max_attempts_per_step`.",
  entfernt=False, entferntIn="", ersatz="loop_control.max_attempts_per_step")
c("loop_control.compaction_trigger_ratio", LC, "Ab welchem Füllstand das Gespräch automatisch verdichtet wird.",
  "compactionTriggerRatio: number 0.5–0.99",
  "Der Wert ist ein Anteil zwischen 0,5 und 0,99. Im Code ist 0,85 voreingestellt: Ist das Kontextfenster zu 85 Prozent voll, fasst Kimi das Gespräch zusammen.\n\nEin kleinerer Wert verdichtet früher. Das hält das Gespräch schlank, dafür gehen öfter Einzelheiten verloren.")
c("loop_control.reserved_context_size", LC, "Platz im Kontextfenster, der immer frei bleibt.",
  "reservedContextSize: int >= 0",
  "Kimi hält einen Teil des Gedächtnisses frei, damit die nächste Antwort sicher hineinpasst. Im Code sind 50000 Tokens voreingestellt.")
c("loop_control.compaction_max_attempts", LC, "Wie oft Kimi eine fehlgeschlagene Verdichtung neu versucht.",
  "compactionMaxAttempts: int >= 1",
  "Das Zusammenfassen selbst kann schiefgehen. Dieser Wert begrenzt die Zahl der Versuche.")

# ================= tools / permission / hooks =================
c("tools.enabled", "Werkzeuge", "Liste der Werkzeuge, die ausdrücklich erlaubt sind.",
  "enabled: array(string)",
  "Werkzeuge sind die Fähigkeiten von Kimi: Dateien lesen, schreiben, Befehle ausführen, im Web suchen.\n\nMit `enabled` und dem Gegenstück `disabled` steuerst du, welche davon überhaupt zur Verfügung stehen.")
c("tools.disabled", "Werkzeuge", "Liste der Werkzeuge, die abgeschaltet sind.",
  "disabled: array(string)",
  "Was hier steht, kann Kimi gar nicht benutzen. Das ist strenger als eine Freigaberegel: Das Werkzeug fehlt dann einfach.")
PR = "Berechtigungen"
c("permission.rules", PR, "Feste Regeln: erlauben, verbieten oder nachfragen.",
  "rules: array({ decision: \"allow\"|\"deny\"|\"ask\", scope: \"turn-override\"|\"session-runtime\"|\"project\"|\"user\" (default \"user\"), pattern, reason })",
  "Mit Regeln legst du fest, wie Kimi mit bestimmten Aktionen umgeht. Jede Regel hat eine Entscheidung (`allow`, `deny` oder `ask`), ein Muster und optional einen Grund.\n\nDas Muster hat die Form `Werkzeug(Argument)`. So kannst du zum Beispiel einen bestimmten Befehl immer erlauben und einen gefährlichen immer verbieten.\n\nUngültige Muster lehnt Kimi mit „Invalid permission rule pattern“ ab.")
c("permission.allow", PR, "Kurzschreibweise für Regeln (ebenso permission.deny und permission.ask).",
  "appendPermissionRules(rules, raw[\"deny\"], \"deny\"); … raw[\"allow\"] … raw[\"ask\"]",
  "Statt jede Regel mit `decision` zu schreiben, kannst du sie unter `allow`, `deny` oder `ask` sammeln. Jeder Eintrag hat dann `tool` und `match` (oder `pattern`).\n\nKimi baut daraus intern das Muster `tool(match)`. Beim Speichern schreibt es die Regeln wieder als `rules`.")
c("permission.dangerous_command_guard", PR, "Schutz, der bei gefährlichen Befehlen immer nachfragt.",
  "dangerousCommandGuard: boolean (default true) — env KIMI_CODE_DANGEROUS_COMMAND_GUARD",
  "Der Schutz ist standardmäßig eingeschaltet. Er untersucht Shell-Befehle und fragt bei gefährlichen Befehlen nach, zum Beispiel beim Löschen von Ordnern außerhalb der Temp-Verzeichnisse.\n\nLass ihn eingeschaltet, gerade wenn du mit `/yolo` arbeitest. Er ist dann oft die letzte Bremse vor einem teuren Fehler.")
H = "Hooks"
c("hooks", H, "Eigene Befehle, die bei bestimmten Ereignissen automatisch laufen.",
  "HookDefSchema = { event, matcher?, command, timeout? (1–600) }",
  "Ein Hook (Haken) ist ein kleines Programm, das Kimi bei einem Ereignis selbst startet. Du schreibst ihn als Tabellenliste `[[hooks]]` mit `event`, optional `matcher`, `command` und `timeout`.\n\nMögliche Ereignisse sind: PreToolUse, PostToolUse, PostToolUseFailure, PermissionRequest, PermissionResult, UserPromptSubmit, UserPromptQueued, TurnStarted, Stop, StopFailure, Interrupt, SessionStart, SessionEnd, SessionHeartbeat, SubagentStart, SubagentStop, TaskStarted, PreCompact, PostCompact und Notification.\n\nBeispiel: Nach jeder Dateiänderung (`PostToolUse`) läuft automatisch ein Formatierer. `timeout` begrenzt die Laufzeit des Hooks; erlaubt ist eine ganze Zahl von 1 bis 600.")

# ================= mcp / task / read / media / watch / services =================
c("mcp.startup_timeout_ms", "MCP-Verbindungen", "Wie lange Kimi auf den Start eines MCP-Servers wartet.",
  "startupTimeoutMs — env KIMI_MCP_STARTUP_TIMEOUT_MS",
  "MCP-Server sind Zusatzprogramme mit weiteren Werkzeugen. Manche brauchen lange zum Starten. Hier legst du in Millisekunden fest, wie lange Kimi wartet.")
c("mcp.tool_timeout_ms", "MCP-Verbindungen", "Wie lange ein einzelner MCP-Werkzeugaufruf dauern darf.",
  "toolTimeoutMs — env KIMI_MCP_TOOL_TIMEOUT_MS",
  "Braucht ein MCP-Werkzeug länger als diese Zeit in Millisekunden, bricht Kimi den Aufruf ab.")
HA = "Hintergrundaufgaben"
c("task.max_running_tasks", HA, "Höchstzahl gleichzeitig laufender Hintergrundaufgaben.",
  "maxRunningTasks: int >= 1 — env KIMI_CODE_BACKGROUND_MAX_RUNNING_TASKS",
  "Hintergrundaufgaben sind zum Beispiel lange Builds oder Server, die nebenher laufen. Dieser Wert begrenzt, wie viele es gleichzeitig sein dürfen. Überblick gibt `/tasks`.")
c("task.keep_alive_on_exit", HA, "Lässt Hintergrundaufgaben beim Beenden weiterlaufen.",
  "keepAliveOnExit: boolean — env KIMI_CODE_BACKGROUND_KEEP_ALIVE_ON_EXIT",
  "Normalerweise enden Hintergrundaufgaben mit Kimi. Mit `true` laufen sie weiter.\n\nIm Einmal-Modus `kimi -p` ändert das außerdem den Standard von `print_background_mode` auf `drain`.")
c("task.bash_auto_background_on_timeout", HA, "Schiebt zu lange Shell-Befehle automatisch in den Hintergrund.",
  "bashAutoBackgroundOnTimeout: boolean",
  "Dauert ein Shell-Befehl länger als erlaubt, wird er nicht abgebrochen, sondern als Hintergrundaufgabe weitergeführt.")
c("task.bash_task_timeout_s", HA, "Zeitgrenze für Shell-Befehle in Sekunden.",
  "bashTaskTimeoutS: int >= 0 — env KIMI_CODE_BACKGROUND_BASH_TASK_TIMEOUT_S",
  "Legt fest, wie viele Sekunden ein Shell-Befehl laufen darf.")
c("task.kill_grace_period_ms", HA, "Schonfrist beim Beenden einer Aufgabe.",
  "killGracePeriodMs: int >= 0",
  "Soll eine Aufgabe enden, bekommt sie erst diese Zeit in Millisekunden, um sauber aufzuräumen. Danach wird sie hart beendet.")
c("task.print_background_mode", HA, "Umgang mit Hintergrundaufgaben im Einmal-Modus kimi -p.",
  "printBackgroundMode: enum([\"exit\", \"drain\", \"steer\"]) — env KIMI_CODE_BACKGROUND_PRINT_BACKGROUND_MODE",
  "Im Einmal-Modus `kimi -p` beantwortet Kimi eine Frage und endet. Dieser Schlüssel regelt, was dabei mit laufenden Hintergrundaufgaben passiert.\n\nErlaubt sind `exit`, `drain` und `steer`. Standard ist `steer`, bei `keep_alive_on_exit = true` dagegen `drain`. Dazu gehören `print_wait_ceiling_s` (Wartezeit-Obergrenze) und `print_max_turns` (Höchstzahl an Runden).")
c("task.print_wait_ceiling_s", HA, "Längste Wartezeit im Einmal-Modus in Sekunden.",
  "printWaitCeilingS: int >= 1 — env KIMI_CODE_BACKGROUND_PRINT_WAIT_CEILING_S",
  "Obergrenze, wie lange `kimi -p` auf Hintergrundaufgaben wartet.")
c("task.print_max_turns", HA, "Höchstzahl an Runden im Einmal-Modus.",
  "printMaxTurns: int >= 1 — env KIMI_CODE_BACKGROUND_PRINT_MAX_TURNS",
  "Begrenzt, wie viele Runden `kimi -p` insgesamt dreht, bevor es endet.")
c("background", HA, "Veraltet: alter Name des Abschnitts [task].",
  "LEGACY_BACKGROUND_SECTION = \"background\"",
  "Früher hieß der Abschnitt für Hintergrundaufgaben `[background]`. Kimi liest ihn noch und mischt ihn mit `[task]`. Bei doppelten Werten gewinnt `[task]`.\n\nNeue Einträge gehören nach `[task]`.",
  entfernt=False, ersatz="task")
c("read.default_max_chars", "Dateien lesen", "Standardlänge, die das Lese-Werkzeug auf einmal liefert.",
  "defaultMaxChars: int > 0",
  "Liest Kimi eine große Datei, bekommt es nicht alles auf einmal. Dieser Wert legt fest, wie viele Zeichen standardmäßig kommen. `max_chars` ist die absolute Obergrenze.")
c("read.max_chars", "Dateien lesen", "Höchstzahl an Zeichen pro Lesevorgang.",
  "maxChars: int > 0",
  "Mehr als diese Zahl an Zeichen liefert ein einzelner Lesevorgang nie. Das schützt das Kontextfenster vor riesigen Dateien.")
c("image.max_edge_px", "Dateianhänge", "Längste Bildkante in Pixeln, bevor Kimi verkleinert.",
  "maxEdgePx: int >= 1 — env KIMI_IMAGE_MAX_EDGE_PX",
  "Große Bilder kosten viele Tokens. Kimi verkleinert sie deshalb, bis die längste Seite höchstens diese Pixelzahl hat.")
c("image.read_byte_budget", "Dateianhänge", "Größenbudget in Bytes beim Einlesen von Bildern.",
  "readByteBudget: int >= 1 — env KIMI_IMAGE_READ_BYTE_BUDGET",
  "Begrenzt, wie viele Bytes Bilddaten Kimi beim Lesen verarbeitet.")
c("watch.enabled", "Arbeitsumgebung", "Beobachtet Konfigurations- und Projektdateien auf Änderungen.",
  "Revert filesystem watchers for config and workspace files to on by default. Set `[watch] enabled` to `false` or `KIMI_CODE_WATCH=0` to keep them off.",
  "Kimi beobachtet Konfigurationsdateien und Dateien wie `AGENTS.md`. Ändern sie sich, merkt Kimi das von selbst.\n\nLaut Änderungsprotokoll von 2.1.1 ist das wieder standardmäßig eingeschaltet. Mit `enabled = false` oder `KIMI_CODE_WATCH=0` schaltest du es ab.")
c("database.search", "Sitzungsverwaltung", "Schaltet den Suchindex für Sitzungen ein oder aus.",
  "search: boolean — env KIMI_CODE_SEARCH_WORKER",
  "Kimi baut im Hintergrund einen Suchindex über deine Sitzungen auf. Mit `false` schaltest du das ab.\n\nIm selben Abschnitt gibt es noch technische Stellschrauben (`base`, `search_sync_session_cap`, `search_sync_debounce_ms`), die man normalerweise nicht braucht.")
c("services.moonshot_search", "Websuche", "Zugang zum Websuche-Dienst.",
  "moonshotSearch: { base_url, api_key, oauth, custom_headers } — env KIMI_WEB_SEARCH_BASE_URL / KIMI_WEB_SEARCH_API_KEY",
  "Wenn Kimi im Web sucht, benutzt es diesen Dienst. Nach `/login` trägt Kimi Adresse und Anmeldung selbst ein, etwa `https://api.kimi.ai/coding/v1/search`.\n\nÄndern musst du hier normalerweise nichts.")
c("services.moonshot_fetch", "Websuche", "Zugang zum Dienst, der Webseiten abruft.",
  "moonshotFetch: { base_url, api_key, oauth, custom_headers } — env KIMI_WEB_FETCH_BASE_URL / KIMI_WEB_FETCH_API_KEY",
  "Mit diesem Dienst holt Kimi den Inhalt einer Webseite, zum Beispiel eine Doku-Seite. Auch hier trägt `/login` die Werte selbst ein.")
c("token_counting.strategy", "Kosten", "Wie Kimi Tokens zählt.",
  "strategy: enum([\"measured+estimated\", \"measured\", \"estimated\"]) (default \"measured+estimated\") — env KIMI_TOKEN_COUNTING_STRATEGY",
  "`measured` nimmt die Zahlen, die der Anbieter meldet. `estimated` schätzt selbst. Der Standard `measured+estimated` verbindet beides.\n\nDie Zahlen siehst du unter `/usage`.")

# ================= experimental =================
EX = "Experimentelle Funktionen"
for key, title, desc, env, default, erk in [
 ("notify_user", "NotifyUser tool", "Show live progress updates from the main agent and subagents in the TUI Updates panel.", "KIMI_CODE_EXPERIMENTAL_NOTIFY_USER", False,
  "Zeigt Fortschrittsmeldungen vom Hauptagenten und von Helfern live in einem eigenen Bereich „Updates“. Nach dem Einschalten brauchst du eine neue Sitzung."),
 ("subagent_fork", "Fork context for subagents", "Let the Agent and AgentSwarm tools start a subagent with a snapshot of the calling agent's conversation history via the fork parameter.", "KIMI_CODE_EXPERIMENTAL_SUBAGENT_FORK", False,
  "Helfer können dann mit einer Kopie des bisherigen Gesprächs starten, statt bei null anzufangen."),
 ("wait_for", "WaitFor tool", "Give the model the WaitFor tool so it can wait for background tasks inside the current turn instead of ending the turn and being re-invoked.", "KIMI_CODE_EXPERIMENTAL_WAIT_FOR", True,
  "Das Modell kann damit innerhalb einer Runde auf Hintergrundaufgaben warten. Diese Funktion ist in 2.1.1 standardmäßig eingeschaltet."),
 ("tower", "Tower mode", "Enable tower mode: coordinate multiple agents on a shared objective, toggled with the /tower command.", "KIMI_CODE_EXPERIMENTAL_TOWER", False,
  "Mehrere Agenten arbeiten gemeinsam an einem Ziel. Gesteuert wird das mit `/tower`."),
 ("tool-select", "Tool select (progressive tool disclosure)", "Keep MCP tool schemas out of the immutable top-level tools[]; the model loads them on demand via the select_tools tool. Only takes effect on models whose capability catalog declares dynamically loaded tools.", "KIMI_CODE_EXPERIMENTAL_TOOL_SELECT", False,
  "Die Beschreibungen der MCP-Werkzeuge werden erst geladen, wenn das Modell sie braucht. Das spart Platz im Kontextfenster. Es wirkt nur bei Modellen mit der Fähigkeit `dynamically_loaded_tools`."),
]:
    nm = f'experimental."{key}"' if '-' in key else f"experimental.{key}"
    c(nm, EX, f"Schaltet die experimentelle Funktion „{title}“ ein oder aus.", f"{title}: {desc}",
      f"Im Abschnitt `[experimental]` stehen Schalter für Funktionen in Erprobung. Standard ist hier `{str(default).lower()}`.\n\n{erk}\n\nAm einfachsten schaltest du das über `/experiments`. Ist die Umgebungsvariable `{env}` oder der Hauptschalter `KIMI_CODE_EXPERIMENTAL_FLAG` gesetzt, ist der Schalter dort gesperrt.")

# ================= tui.toml =================
D = "Darstellung"
c("theme", D, "Farbschema der Terminal-Oberfläche.",
  "theme = \"auto\" # \"auto\" | \"dark\" | \"light\" | custom theme name",
  "Erlaubt sind `auto`, `dark`, `light` oder der Name eines eigenen Schemas. `auto` passt sich an das Terminal an.\n\nBequemer geht es mit `/theme` oder unter `/settings` → Theme.", art=TT)
c("tui_mode", D, "Normale oder Vollbild-Darstellung.",
  "tui_mode = \"regular\" # \"regular\" | \"fullscreen\" (\"fullscreen\" is experimental)",
  "`regular` schreibt in den normalen Verlauf des Terminals. `fullscreen` nutzt einen eigenen Bildschirm mit Scrollen, Markieren und Suche im Gesprächsverlauf. Vollbild ist noch experimentell.\n\nDie Änderung wirkt erst nach einem Neustart von Kimi Code.", art=TT)
c("render_latex", D, "Stellt mathematische Formeln schön dar.",
  "render_latex = true # false keeps LaTeX math in assistant messages as raw source",
  "LaTeX ist eine Schreibweise für Formeln, etwa `\\frac{a}{b}` für einen Bruch. Bei `true` zeigt Kimi die Formel lesbar an. Bei `false` siehst du den rohen Quelltext.", art=TT)
c("disable_paste_burst", "Eingabe", "Schaltet die Erkennung schnellen Einfügens ab.",
  "disable_paste_burst = false # true disables non-bracketed paste-burst fallback",
  "Manche Terminals melden nicht sauber, dass gerade Text eingefügt wird. Kimi erkennt Einfügen dann an sehr schnell eintreffenden Zeichen. So wird ein Zeilenumbruch im eingefügten Text nicht als „Absenden“ verstanden.\n\nMit `true` schaltest du diese Ersatz-Erkennung ab. Das brauchst du nur, wenn sie bei dir stört.", art=TT)
c("cache_expiry_hint", "Kosten", "Warnt, wenn der Zwischenspeicher abgelaufen ist.",
  "cache_expiry_hint = true # false disables the \"cache expired\" dialog on resume / idle submit",
  "Anbieter speichern den Anfang eines Gesprächs kurz zwischen. Das macht weitere Anfragen günstiger. Nach einer längeren Pause ist dieser Zwischenspeicher abgelaufen.\n\nKimi zeigt dann beim Fortsetzen einen Hinweis. Mit `false` schaltest du ihn ab.", art=TT)
c("disable_feedback_survey", "Rückmeldung", "Blendet die gelegentliche Bewertungsfrage aus.",
  "disable_feedback_survey = false # true hides the occasional session rating prompt",
  "Ab und zu bittet Kimi dich, eine Sitzung zu bewerten. Mit `true` erscheint diese Frage nicht mehr. Im Menü heißt das `/settings` → Feedback survey.", art=TT)
c("editor.command", "Eingabe", "Externer Editor für Strg+G.",
  "command = \"\" # Empty uses $VISUAL / $EDITOR",
  "Beispiel: `command = \"code --wait\"` öffnet VS Code. Bleibt der Wert leer, nimmt Kimi `KIMI_CODE_EDITOR`, `$VISUAL` oder `$EDITOR`.\n\nEinstellen geht auch mit `/editor`.", art=TT)
c("notifications.enabled", "Benachrichtigungen", "Schaltet Desktop-Benachrichtigungen ein oder aus.",
  "enabled = true # true | false",
  "Kimi kann dir Bescheid geben, wenn es fertig ist oder auf dich wartet. Mit `false` bleibt es still.", art=TT)
c("notifications.notification_condition", "Benachrichtigungen", "Wann Benachrichtigungen kommen.",
  "notification_condition = \"unfocused\" # \"unfocused\" | \"always\"",
  "`unfocused` (Standard) meldet sich nur, wenn das Terminalfenster gerade nicht im Vordergrund ist. `always` meldet sich immer.", art=TT)
c("upgrade.auto_install", "Aktualisierung", "Installiert neue Versionen automatisch im Hintergrund.",
  "auto_install = true # true | false",
  "Bei `true` lädt und installiert Kimi Updates selbst. Bei `false` zeigt es stattdessen eine Frage zur Installation.\n\nIm Menü: `/settings` → Automatic updates. Ganz abschalten lässt sich die Update-Prüfung mit `KIMI_CODE_NO_AUTO_UPDATE`.", art=TT)
c("markdown.mermaid", D, "Zeichnet Mermaid-Diagramme im Terminal.",
  "# Draw mermaid code blocks as diagrams in the terminal; \"off\" keeps highlighted source.\n# mermaid = \"final\" # \"final\" | \"off\"",
  "Mermaid ist eine Textsprache für Diagramme, etwa Ablaufpläne. Bei `final` (Standard) zeichnet Kimi solche Blöcke als Bild aus Zeichen. Bei `off` siehst du den Quelltext mit Farben.\n\nIm Menü: `/settings` → Mermaid diagrams.", art=TT)
c("status_line.items", D, "Welche Bausteine die Fußzeile zeigt, in welcher Reihenfolge.",
  "# Pick and order the built-in footer slots: mode, goal, model, tasks, cwd, git, tips",
  "Die Fußzeile unten im Terminal besteht aus Bausteinen. Zur Wahl stehen `mode`, `goal`, `model`, `tasks`, `cwd`, `git` und `tips`.\n\nBeispiel: `items = [\"model\", \"git\"]` zeigt nur Modell und Git-Zweig. Unbekannte Namen ignoriert Kimi mit einer Warnung.", art=TT)
c("status_line.command", D, "Eigenes Programm für die erste Zeile der Fußzeile.",
  "# Or render your own: a command whose first stdout line replaces footer line 1.\n# It receives a JSON snapshot (model, cwd, git, usage, mode) on stdin.",
  "Statt der Bausteine kannst du ein eigenes Skript angeben, etwa `~/.kimi-code/statusline.sh`. Kimi schickt ihm Modell, Ordner, Git-Stand, Verbrauch und Modus als JSON.\n\nDie erste Ausgabezeile des Skripts ersetzt dann die erste Zeile der Fußzeile.", art=TT)

# ================= Umgebungsvariablen =================
def u(name, kat, kurz, eng, erk, **kw):
    c(name, kat, kurz, eng, erk, art=UV, **kw)

u("KIMI_CODE_HOME", "Arbeitsumgebung", "Ordner, in dem Kimi Code seine Daten ablegt.",
  "Kimi home follows `$KIMI_CODE_HOME` before `~/.kimi-code`.",
  "Normalerweise liegen Konfiguration, Sitzungen und Skills in `~/.kimi-code`. Mit dieser Variablen legst du einen anderen Ordner fest.\n\nDas ist nützlich, wenn du zwei getrennte Einrichtungen brauchst, etwa privat und Arbeit.")
u("KIMI_CODE_NO_AUTO_UPDATE", "Aktualisierung", "Schaltet automatische Updates ab.",
  "`KIMI_CODE_NO_AUTO_UPDATE` still wins: disabling updates beats opting in.",
  "Ist die Variable gesetzt, zum Beispiel auf `1`, installiert Kimi keine Updates selbst. Sie hat Vorrang vor `upgrade.auto_install` in der `tui.toml`.\n\nDer alte Name `KIMI_CLI_NO_AUTO_UPDATE` wird noch erkannt.")
u("KIMI_CLI_NO_AUTO_UPDATE", "Aktualisierung", "Veraltet: alter Name von KIMI_CODE_NO_AUTO_UPDATE.",
  "`KIMI_CODE_NO_AUTO_UPDATE` (or the legacy `KIMI_CLI_NO_AUTO_UPDATE` alias)",
  "Stammt aus dem Vorgänger kimi-cli. Er wirkt noch, neu solltest du aber `KIMI_CODE_NO_AUTO_UPDATE` benutzen.",
  entfernt=False, ersatz="KIMI_CODE_NO_AUTO_UPDATE")
u("KIMI_MODEL_NAME", "Modellauswahl", "Modell direkt per Umgebungsvariable festlegen.",
  "KIMI_MODEL_NAME is set but KIMI_MODEL_API_KEY is missing.",
  "Mit `KIMI_MODEL_NAME` und `KIMI_MODEL_API_KEY` richtest du ein Modell ganz ohne `config.toml` ein. Das ist praktisch auf Servern oder in automatischen Abläufen.\n\nFehlt der Schlüssel, bricht Kimi mit einer klaren Meldung ab. Dazu passen `KIMI_MODEL_BASE_URL`, `KIMI_MODEL_PROVIDER_TYPE`, `KIMI_MODEL_MAX_CONTEXT_SIZE`, `KIMI_MODEL_MAX_OUTPUT_SIZE`, `KIMI_MODEL_CAPABILITIES` und `KIMI_MODEL_DISPLAY_NAME`.")
u("KIMI_MODEL_API_KEY", "Modellauswahl", "API-Schlüssel für das per Umgebung festgelegte Modell.",
  "apiKey: \"KIMI_MODEL_API_KEY\"",
  "Gehört zu `KIMI_MODEL_NAME`. Der Schlüssel steht so nie in einer Datei. Gib ihn trotzdem nie in Skripten weiter, die andere sehen.")
u("KIMI_MODEL_BASE_URL", "Modellauswahl", "Adresse der Schnittstelle für das per Umgebung festgelegte Modell.",
  "baseUrl: \"KIMI_MODEL_BASE_URL\"",
  "Fehlt sie, nimmt Kimi die Standardadresse der gewählten Anbieterart.")
u("KIMI_MODEL_PROVIDER_TYPE", "Modellauswahl", "Art des Anbieters für das per Umgebung festgelegte Modell.",
  "type: \"KIMI_MODEL_PROVIDER_TYPE\"",
  "Entspricht `type` bei einem Anbieter in der `config.toml`.")
u("KIMI_MODEL_MAX_CONTEXT_SIZE", "Modellauswahl", "Kontextfenster des per Umgebung festgelegten Modells.",
  "parsePositiveInt(maxContextRaw, \"KIMI_MODEL_MAX_CONTEXT_SIZE\")",
  "Größe des Kurzzeitgedächtnisses in Tokens. Muss eine positive ganze Zahl sein.")
u("KIMI_MODEL_THINKING_EFFORT", "Modellverhalten", "Erzwingt eine Denkstufe.",
  "thinkingEnvBindings = { forcedEffort: \"KIMI_MODEL_THINKING_EFFORT\" }",
  "Setzt die Denkstufe fest, zum Beispiel auf `low`. Der Wert wird nie in die `config.toml` geschrieben. Er gilt nur, solange die Variable gesetzt ist.")
u("KIMI_SECONDARY_MODEL", "Unteragenten", "Modell für Unteragenten per Umgebungsvariable.",
  "SECONDARY_MODEL_ENV = \"KIMI_SECONDARY_MODEL\"",
  "Legt das Modell für Helfer fest, ohne `[secondary_model]` zu ändern. Die passende Denkstufe setzt `KIMI_SECONDARY_EFFORT`.")
u("KIMI_SECONDARY_EFFORT", "Unteragenten", "Denkstufe für Unteragenten per Umgebungsvariable.",
  "SECONDARY_MODEL_EFFORT_ENV = \"KIMI_SECONDARY_EFFORT\"",
  "Gehört zu `KIMI_SECONDARY_MODEL`.")
u("KIMI_CODE_EDITOR", "Eingabe", "Editor für Strg+G, vor $VISUAL und $EDITOR.",
  "resolveEditorCommand: [\"KIMI_CODE_EDITOR\", \"VISUAL\", \"EDITOR\"]",
  "Kimi prüft in dieser Reihenfolge: `KIMI_CODE_EDITOR`, dann `VISUAL`, dann `EDITOR`. Der erste gesetzte Wert gewinnt.")
u("KIMI_SHELL_PATH", "Windows", "Pfad zu bash.exe unter Windows.",
  "Git Bash was not found on this Windows host. Install Git for Windows from https://gitforwindows.org/ or set KIMI_SHELL_PATH to a bash.exe.",
  "Unter Windows führt Kimi Code Befehle in Git Bash aus. Findet es Git Bash nicht, bricht es mit dieser Meldung ab.\n\nIst Git an einem ungewöhnlichen Ort installiert, gibst du hier den Pfad zur `bash.exe` an.")
u("KIMI_DISABLE_TELEMETRY", "Datenschutz", "Schaltet das Senden von Nutzungsdaten ab.",
  "TELEMETRY_DISABLE_ENV = \"KIMI_DISABLE_TELEMETRY\"",
  "Telemetrie sind Nutzungsdaten, die ein Programm an den Hersteller schickt. Mit dieser Variablen schaltest du das ab.")
u("KIMI_LOG_LEVEL", "Fehlersuche", "Wie ausführlich Kimi ins Protokoll schreibt.",
  "level: parseLevel(env[\"KIMI_LOG_LEVEL\"]) ?? \"info\"",
  "Standard ist `info`. Für die Fehlersuche stellst du eine ausführlichere Stufe ein. Das Protokoll liegt in `~/.kimi-code/logs/kimi-code.log`.\n\nDazu gehören `KIMI_LOG_GLOBAL_MAX_BYTES` (Standard 6291456 Bytes, rund 6 MB), `KIMI_LOG_GLOBAL_FILES` (Standard 5), `KIMI_LOG_SESSION_MAX_BYTES` (Standard 5242880) und `KIMI_LOG_SESSION_FILES` (Standard 3).")
u("KIMI_CODE_CUSTOM_HEADERS", "Netzwerk", "Zusätzliche HTTP-Kopfzeilen für Anfragen an Kimi.",
  "parseKimiCodeCustomHeaders: one `Name: value` per line",
  "Jede Zeile hat die Form `Name: Wert`. Das brauchst du etwa hinter einem Firmen-Proxy, der eine Kennung verlangt.")
u("KIMI_CODE_OAUTH_HOST", "Anmeldung", "Andere Adresse für die Anmeldung.",
  "oauthHost: envOverride(\"KIMI_CODE_OAUTH_HOST\") ?? envOverride(\"KIMI_OAUTH_HOST\") ?? \"https://auth.kimi.com\"",
  "Standard ist `https://auth.kimi.com`. Der ältere Name `KIMI_OAUTH_HOST` wirkt ebenfalls. Normale Nutzer brauchen das nicht.")
u("KIMI_CODE_DANGEROUS_COMMAND_GUARD", "Berechtigungen", "Schaltet den Schutz vor gefährlichen Befehlen.",
  "DANGEROUS_COMMAND_GUARD_ENV = \"KIMI_CODE_DANGEROUS_COMMAND_GUARD\"",
  "Überschreibt `permission.dangerous_command_guard`. Der Schutz ist standardmäßig an. Schalte ihn nur ab, wenn du genau weißt, warum.")
u("KIMI_CODE_BUILTIN_PRODUCT_SKILLS", "Skills", "Blendet die eingebauten Produkt-Skills ein oder aus.",
  "BUILTIN_PRODUCT_SKILLS_ENV = \"KIMI_CODE_BUILTIN_PRODUCT_SKILLS\"",
  "Überschreibt `builtin_product_skills` in der `config.toml`.")
u("KIMI_CODE_WATCH", "Arbeitsumgebung", "Schaltet die Dateibeobachtung ein oder aus.",
  "Set `[watch] enabled` to `false` or `KIMI_CODE_WATCH=0` to keep them off.",
  "Mit `KIMI_CODE_WATCH=0` bemerkt Kimi Änderungen an Konfiguration und `AGENTS.md` nicht mehr von selbst. Dann hilft `/reload`.")
u("KIMI_CODE_EXPERIMENTAL_FLAG", EX, "Hauptschalter: schaltet alle experimentellen Funktionen ein.",
  "locked by KIMI_CODE_EXPERIMENTAL_FLAG",
  "Ist diese Variable wahr gesetzt, sind alle experimentellen Funktionen an. In `/experiments` sind die Schalter dann gesperrt.\n\nEinzelne Funktionen schaltest du mit `KIMI_CODE_EXPERIMENTAL_NOTIFY_USER`, `…_SUBAGENT_FORK`, `…_WAIT_FOR`, `…_TOWER` und `…_TOOL_SELECT`.")
u("KIMI_LOOP_MAX_STEPS_PER_TURN", "Ausführungslimits", "Höchstzahl an Schritten pro Runde.",
  "LOOP_MAX_STEPS_PER_TURN_ENV = \"KIMI_LOOP_MAX_STEPS_PER_TURN\"",
  "Überschreibt `loop_control.max_steps_per_turn`.")
u("KIMI_LOOP_MAX_ATTEMPTS_PER_STEP", "Ausführungslimits", "Wiederholungen pro Schritt bei Fehlern.",
  "LOOP_MAX_ATTEMPTS_PER_STEP_ENV = \"KIMI_LOOP_MAX_ATTEMPTS_PER_STEP\"",
  "Überschreibt `loop_control.max_attempts_per_step`.")
u("KIMI_LOOP_MAX_RETRIES_PER_STEP", "Ausführungslimits", "Veraltet: alter Name von KIMI_LOOP_MAX_ATTEMPTS_PER_STEP.",
  "deprecatedEnv: LOOP_MAX_RETRIES_PER_STEP_ENV",
  "Wird noch erkannt, ist aber veraltet.", entfernt=False, ersatz="KIMI_LOOP_MAX_ATTEMPTS_PER_STEP")
u("KIMI_CODE_INFINITE_RETRY", "Fehlerbehandlung", "Wiederholt fehlgeschlagene Modellanfragen ohne Ende.",
  "llm request failed; retrying indefinitely (KIMI_CODE_INFINITE_RETRY)",
  "Normalerweise gibt Kimi nach einigen Fehlversuchen auf. Mit dieser Variablen versucht es immer weiter. Das kann bei einer wackligen Verbindung helfen, bei einem echten Fehler aber endlos hängen.")
u("KIMI_CODE_REPEAT_BREAKER", "Modellverhalten", "Schaltet die Bremse gegen wiederholte Werkzeugaufrufe.",
  "The same tool call has been repeated several times in a row. Before making your next call, write one sentence stating what new information you expect it to produce.",
  "Ruft das Modell mehrmals hintereinander genau dasselbe Werkzeug auf, schiebt Kimi einen Hinweis ein (ab dem 3., 5. und 8. Mal). So soll es aus Schleifen herausfinden. Die Variable steuert diese Bremse.")
u("KIMI_CODE_PERMISSION_MODE_REMINDER", "Berechtigungen", "Erinnert das Modell an den aktuellen Freigabe-Modus.",
  "parseBooleanEnv(getEnv(\"KIMI_CODE_PERMISSION_MODE_REMINDER\")) !== false",
  "Standardmäßig bekommt das Modell einen Hinweis, welcher Freigabe-Modus gilt. Mit `false` schaltest du das ab.")
u("KIMI_CODE_AGENT_SWARM_MAX_CONCURRENCY", UA, "Wie viele Schwarm-Agenten gleichzeitig laufen.",
  "AGENT_SWARM_MAX_CONCURRENCY_ENV = \"KIMI_CODE_AGENT_SWARM_MAX_CONCURRENCY\"",
  "Begrenzt die Zahl paralleler Agenten bei `/swarm`. Weniger gleichzeitige Agenten laufen seltener in Anfragelimits des Anbieters.")
u("KIMI_SUBAGENT_TIMEOUT_MS", UA, "Zeitgrenze für Unteragenten.",
  "SUBAGENT_TIMEOUT_ENV = \"KIMI_SUBAGENT_TIMEOUT_MS\"", "Überschreibt `subagent.timeout_ms`.")
u("KIMI_CODE_SWARM_TIMEOUT_MS", UA, "Zeitgrenze für Schwarm-Aufgaben.",
  "SWARM_TIMEOUT_ENV = \"KIMI_CODE_SWARM_TIMEOUT_MS\"", "Überschreibt `swarm.timeout_ms`.")
u("KIMI_MCP_STARTUP_TIMEOUT_MS", "MCP-Verbindungen", "Startzeit für MCP-Server.",
  "MCP_STARTUP_TIMEOUT_ENV = \"KIMI_MCP_STARTUP_TIMEOUT_MS\"", "Überschreibt `mcp.startup_timeout_ms`.")
u("KIMI_MCP_TOOL_TIMEOUT_MS", "MCP-Verbindungen", "Zeitgrenze für MCP-Werkzeugaufrufe.",
  "MCP_TOOL_TIMEOUT_ENV = \"KIMI_MCP_TOOL_TIMEOUT_MS\"", "Überschreibt `mcp.tool_timeout_ms`.")
u("KIMI_CODE_BACKGROUND_MAX_RUNNING_TASKS", HA, "Höchstzahl paralleler Hintergrundaufgaben.",
  "MAX_RUNNING_TASKS_ENV = \"KIMI_CODE_BACKGROUND_MAX_RUNNING_TASKS\"",
  "Überschreibt `task.max_running_tasks`. Ebenso gibt es `KIMI_CODE_BACKGROUND_KEEP_ALIVE_ON_EXIT`, `…_BASH_TASK_TIMEOUT_S`, `…_PRINT_WAIT_CEILING_S`, `…_PRINT_BACKGROUND_MODE` und `…_PRINT_MAX_TURNS` für die übrigen Werte aus `[task]`.")
u("KIMI_IMAGE_MAX_EDGE_PX", "Dateianhänge", "Längste Bildkante in Pixeln.",
  "IMAGE_MAX_EDGE_ENV = \"KIMI_IMAGE_MAX_EDGE_PX\"", "Überschreibt `image.max_edge_px`. Passend dazu gibt es `KIMI_IMAGE_READ_BYTE_BUDGET`.")
u("KIMI_TOKEN_COUNTING_STRATEGY", "Kosten", "Art der Token-Zählung.",
  "TOKEN_COUNTING_STRATEGY_ENV = \"KIMI_TOKEN_COUNTING_STRATEGY\"", "Überschreibt `token_counting.strategy`.")
u("KIMI_WEB_SEARCH_API_KEY", "Websuche", "Eigener Zugang für die Websuche.",
  "WEB_SEARCH_API_KEY_ENV = \"KIMI_WEB_SEARCH_API_KEY\"",
  "Zusammen mit `KIMI_WEB_SEARCH_BASE_URL` ersetzt er den Zugang aus `[services.moonshot_search]`. Für den Seitenabruf gibt es entsprechend `KIMI_WEB_FETCH_API_KEY` und `KIMI_WEB_FETCH_BASE_URL`.")
u("KIMI_CODE_MODEL_CATALOG_REFRESH_ON_START", "Modellauswahl", "Katalog beim Start neu laden.",
  "REFRESH_ON_START_ENV = \"KIMI_CODE_MODEL_CATALOG_REFRESH_ON_START\"",
  "Überschreibt `model_catalog.refresh_on_start`. Dazu gehört `KIMI_CODE_MODEL_CATALOG_REFRESH_INTERVAL_MS`.")
u("KIMI_CODE_PLUGIN_MARKETPLACE_URL", "Plugins", "Andere Adresse für den Plugin-Marktplatz.",
  "KIMI_CODE_PLUGIN_MARKETPLACE_URL cannot be empty.",
  "Standardmäßig lädt Kimi die Plugin-Liste aus `…/kimi-code/plugins/marketplace.json` auf dem eigenen Server. Mit dieser Variablen nimmst du eine andere Liste, etwa einen Firmen-Marktplatz.")
u("KIMI_CODE_PASSWORD", "Web-Oberfläche", "Passwort für die Web-Oberfläche.",
  "binding non-loopback host with token-only auth (no KIMI_CODE_PASSWORD)",
  "Startest du `kimi web` so, dass andere Rechner im Netz zugreifen können, sichert diese Variable den Zugang mit einem Passwort ab. Ohne sie warnt Kimi.")
u("KIMI_CODE_ALLOWED_HOSTS", "Web-Oberfläche", "Erlaubte Hostnamen für die Web-Oberfläche.",
  "Invalid Host header: …; allow this host with KIMI_CODE_ALLOWED_HOSTS=… or 'kimi web --allowed-host …'.",
  "Rufst du die Web-Oberfläche über einen anderen Namen als `localhost` auf, lehnt Kimi das zunächst ab. Hier trägst du den erlaubten Namen ein.")
u("KIMI_CODE_TUI_MAX_TURNS", D, "Wie viele Runden das Terminal sichtbar hält.",
  "Keep the most recent N turns. `0` disables trimming.",
  "Standard sind 15 Runden. Ältere Runden werden aus der Anzeige entfernt, damit das Terminal flüssig bleibt. `0` schaltet das Kürzen ab.\n\nVerwandt sind `KIMI_CODE_TUI_EXPAND_TURNS` (wie viele Runden sich mit Strg+O aufklappen lassen, Standard 3) und `KIMI_CODE_TUI_KEEP_RECENT_STEPS` (Standard 30).")
u("KIMI_CODE_TUI_FULL_SCREEN", D, "Veraltet: alter Schalter für den Vollbildmodus.",
  "return process.env[\"KIMI_CODE_TUI_FULL_SCREEN\"] === \"1\" ? \"fullscreen\" : void 0;",
  "Früher schaltete `KIMI_CODE_TUI_FULL_SCREEN=1` den Vollbildmodus ein. Kimi überträgt den Wert heute einmalig nach `tui_mode` in der `tui.toml`.",
  entfernt=False, ersatz="tui_mode")
u("KIMI_SHARE_DIR", "Einrichtung", "Quellordner beim Umzug vom alten kimi-cli.",
  "source: … (KIMI_SHARE_DIR | default ~/.kimi)",
  "Der Befehl `kimi migrate` holt Daten aus dem Vorgänger kimi-cli. Der liegt normalerweise in `~/.kimi`. Hatte der alte Ordner einen anderen Ort, gibst du ihn hier an.")
u("KIMI_CODE_DEBUG", "Fehlersuche", "Zeigt Zeitmessungen der Arbeitsschritte im Verlauf an.",
  "if (process.env[\"KIMI_CODE_DEBUG\"] !== \"1\") return;",
  "Mit `KIMI_CODE_DEBUG=1` zeigt Kimi im Gesprächsverlauf zusätzlich Zeitmessungen zu den einzelnen Arbeitsschritten an. Das ist für die Fehlersuche gedacht, nicht für den Alltag.")

write('config_einstellungen.json', 'config', C)
print('config', len(C))
