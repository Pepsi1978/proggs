# -*- coding: utf-8 -*-
"""Die wichtigsten Config-Einstellungen, ausfuehrlich erklaert."""

CONFIG_KERN = [
 ("permissions", "Berechtigungen", "settings.json",
  "Legt fest, was ohne Rueckfrage laufen darf, was nachgefragt und was verboten wird.",
  "Set allow, ask, and deny rules and the starting permission mode",
  "Das ist die wichtigste Sicherheitseinstellung ueberhaupt. Sie hat drei Listen: `allow` "
  "erlaubt ohne Nachfrage, `ask` fragt jedes Mal, `deny` verbietet grundsaetzlich.\n\n"
  "Ein haeufiges Missverstaendnis: `allow` ist KEINE abschliessende Liste. Was nicht darin "
  "steht, ist trotzdem nicht verboten — es loest nur eine Rueckfrage aus. Verbieten geht "
  "ausschliesslich ueber `deny`.\n\n"
  "Die Regeln koennen Muster enthalten, zum Beispiel `Bash(git *)` fuer alle Git-Befehle oder "
  "`Read(./.env)` fuer eine bestimmte Datei.\n\n"
  "Unter `permissions.defaultMode` legst du fest, in welchem Modus neue Sitzungen starten. Und "
  "anders als die meisten Einstellungen werden Berechtigungsregeln aus verschiedenen Dateien "
  "zusammengefuehrt, nicht ersetzt."),

 ("permissions.defaultMode", "Berechtigungen", "settings.json",
  "Der Berechtigungsmodus, in dem neue Sitzungen starten.",
  "Set the permission mode new sessions start in",
  "Es gibt mehrere Modi. `default` fragt bei allem nach, was nicht ausdruecklich erlaubt ist. "
  "`acceptEdits` laesst Dateiaenderungen ohne Rueckfrage durch, fragt aber bei Befehlen weiter "
  "nach.\n\n"
  "`plan` heisst: erst planen, nichts aendern. `bypassPermissions` schaltet alle Rueckfragen "
  "ab — Claude macht dann alles ohne zu fragen.\n\n"
  "Der letzte Modus spart viel Zeit, nimmt dir aber auch die letzte Kontrolle. Er gehoert "
  "nur auf einen Rechner, auf dem ein Fehlgriff nichts Wichtiges zerstoeren kann.\n\n"
  "Eine Organisation kann diesen Modus ueber `permissions.disableBypassPermissionsMode` ganz "
  "verbieten."),

 ("permissions.deny", "Berechtigungen", "settings.json",
  "Sperrt bestimmte Werkzeugaufrufe und Dateien vollstaendig.",
  "Block listed tool uses, including reads of files that hold secrets",
  "`deny` ist die einzige echte Sperre in Claude Code. Was hier steht, wird abgelehnt — ohne "
  "Rueckfrage, ohne Ausweg.\n\n"
  "Der wichtigste Einsatz sind Dateien mit Zugangsdaten: `Read(./.env)` oder "
  "`Read(**/secrets/**)` verhindert, dass ihr Inhalt ueberhaupt ins Gespraech gelangt.\n\n"
  "Ebenso sinnvoll bei gefaehrlichen Befehlen, etwa allem, was auf ein Produktivsystem "
  "zugreift.\n\n"
  "Weil `allow` nicht sperrt, ist `deny` die Stelle, an der Sicherheit wirklich entsteht. "
  "Regeln aus allen Ebenen werden dabei zusammengefuehrt."),

 ("permissions.allow", "Berechtigungen", "settings.json",
  "Erlaubt bestimmte Werkzeugaufrufe ohne Rueckfrage.",
  "Approve listed tool uses without a prompt",
  "Hier traegst du ein, was ohne Nachfrage laufen darf — typischerweise harmlose, lesende "
  "Befehle wie `Bash(git status)` oder `Bash(ls *)`.\n\n"
  "Das spart viel Klicken. Nimm aber nur auf, was nichts kaputtmachen kann.\n\n"
  "Wichtig: Diese Liste erlaubt, sie verbietet nicht. Alles andere ist deshalb nicht gesperrt, "
  "sondern loest nur eine Rueckfrage aus.\n\n"
  "Der Skill `/fewer-permission-prompts` schlaegt dir aus deinen bisherigen Sitzungen eine "
  "passende Liste vor."),

 ("permissions.additionalDirectories", "Berechtigungen", "settings.json",
  "Erlaubt dauerhaft den Zugriff auf Ordner ausserhalb des Projekts.",
  "Give Claude file access to directories outside the current one",
  "Normalerweise endet der Zugriff an der Grenze des Arbeitsordners. Hier traegst du weitere "
  "Ordner ein, die dauerhaft dazugehoeren sollen.\n\n"
  "Typischer Fall: eine gemeinsam genutzte Bibliothek, die neben dem Projekt liegt, oder ein "
  "Ordner mit Vorlagen.\n\n"
  "Der Unterschied zu `/add-dir`: Der Befehl gilt nur fuer die laufende Sitzung, dieser "
  "Eintrag dauerhaft.\n\n"
  "Setze die Liste so kurz wie moeglich. Jeder zusaetzliche Ordner ist eine Stelle, an der "
  "versehentlich etwas geaendert werden kann."),

 ("hooks", "Automatisierung", "settings.json",
  "Fuehrt eigene Befehle an festgelegten Punkten im Ablauf aus.",
  "Run your own commands as hooks at points in Claude Code's lifecycle",
  "Ein Hook ist der einzige Weg, etwas wirklich zu erzwingen. Claude kann eine Anweisung "
  "vergessen — ein Hook laeuft immer.\n\n"
  "Es gibt Ereignisse fuer viele Punkte: vor und nach einem Werkzeugaufruf, beim Start und "
  "Ende der Sitzung, beim Abschicken einer Eingabe, beim Start eines Unteragenten.\n\n"
  "Damit kannst du zum Beispiel nach jeder Dateiaenderung die Formatierung laufen lassen, "
  "gefaehrliche Befehle abfangen oder zusaetzlichen Kontext einspielen.\n\n"
  "Ein Hook ist ein echter Befehl auf deinem Rechner. Ein Fehler darin kann Claude Code "
  "blockieren — pruefe deshalb immer die Rueckgabewerte. Ueber `disableAllHooks` laesst sich "
  "alles auf einmal abschalten."),

 ("model", "Modell und Antworten", "settings.json",
  "Das Modell, mit dem Claude Code startet.",
  "Change the model Claude Code starts with",
  "Hier legst du fest, welches Modell in neuen Sitzungen benutzt wird. Du kannst einen "
  "Kurznamen wie `opus` oder `sonnet` eintragen oder eine vollstaendige Modellkennung.\n\n"
  "Ein staerkeres Modell denkt gruendlicher und kostet mehr, ein leichteres ist schnell und "
  "guenstig.\n\n"
  "Achtung: Diese Einstellung wird nicht waehrend des Betriebs neu eingelesen. Nach einer "
  "Aenderung musst du Claude Code neu starten.\n\n"
  "Innerhalb einer Sitzung wechselst du mit `/model`. Als Ausweichmodell fuer den Fall der "
  "Ueberlastung dient `fallbackModel`."),

 ("env", "Umgebung", "settings.json",
  "Setzt Umgebungsvariablen fuer jede Sitzung und alle gestarteten Programme.",
  "Set environment variables for every session and its subprocesses",
  "Manches laesst sich nur ueber Umgebungsvariablen einstellen — Zeitgrenzen, Modellkennungen, "
  "Schalter fuer einzelne Funktionen.\n\n"
  "Unter `env` traegst du sie als Paare aus Name und Wert ein. Sie gelten dann in jeder "
  "Sitzung und werden auch an jeden Befehl weitergegeben, den Claude ausfuehrt.\n\n"
  "Achtung bei Zugangsdaten: Alles, was hier steht, wird an Unterprozesse weitergereicht. "
  "Schluessel gehoeren deshalb nicht in eine Datei, die im Projekt liegt.\n\n"
  "Zwei Ausnahmen: `NO_COLOR` und `FORCE_COLOR` wirken ab Version 2.1.143 nur noch fuer die "
  "Unterprozesse, nicht mehr fuer die Anzeige von Claude Code selbst."),

 ("effortLevel", "Modell und Antworten", "settings.json",
  "Legt dauerhaft fest, wie gruendlich Claude nachdenkt.",
  "Save the /effort level so future sessions reason more or less deeply",
  "Was du mit `/effort` fuer eine Sitzung einstellst, machst du hier dauerhaft: `low`, "
  "`medium`, `high`, `xhigh` oder `max`.\n\n"
  "Mehr Denken bringt bessere Ergebnisse bei schweren Aufgaben, kostet aber Zeit und Geld. "
  "Bei einfachen Handgriffen bringt es nichts.\n\n"
  "Wichtig: Die Stufe gehoert in diese Einstellung. Der Weg ueber eine Umgebungsvariable "
  "namens `CLAUDE_CODE_EFFORT_LEVEL` funktioniert nicht zuverlaessig.\n\n"
  "Ein bekannter Stolperstein: Bei Opus 5 fuehrten `xhigh` und `max` zu einem Fehler, wenn "
  "das Denken zugleich abgeschaltet war. Seit 2.1.251 wird in dem Fall automatisch `high` "
  "gesendet."),

 ("outputStyle", "Modell und Antworten", "settings.json",
  "Aendert Rolle, Ton und Form der Antworten.",
  "Change Claude's role, tone, and output format with an output style",
  "Ein Ausgabestil bestimmt, wie Claude auftritt: knapp und handelnd, ausfuehrlich erklaerend, "
  "oder auf ein bestimmtes Vorgehen festgelegt.\n\n"
  "Du kannst einen mitgelieferten Stil waehlen oder einen eigenen schreiben. Eigene Stile "
  "liegen unter `~/.claude/output-styles/`.\n\n"
  "Der Stil wirkt sehr stark — er steht weit oben in den Anweisungen und praegt jede Antwort.\n\n"
  "Wie das Modell wird auch der Stil nicht waehrend des Betriebs neu eingelesen. Nach einer "
  "Aenderung ist ein Neustart noetig."),

 ("autoCompactEnabled", "Kontext und Gedaechtnis", "settings.json",
  "Schaltet das automatische Zusammenfassen ein oder aus.",
  "Turn automatic compaction off or on",
  "Wird das Gedaechtnis voll, fasst Claude Code den bisherigen Verlauf von allein zusammen. "
  "Das haelt die Sitzung am Laufen, kostet aber Einzelheiten.\n\n"
  "Schaltest du es aus, laeuft die Sitzung irgendwann gegen die Grenze und du musst selbst mit "
  "`/compact` oder `/clear` eingreifen.\n\n"
  "Ausschalten lohnt sich, wenn dir wichtig ist, dass nichts still verlorengeht — etwa bei "
  "einer langen Fehlersuche, in der jede Zwischenerkenntnis zaehlt.\n\n"
  "Wann verdichtet wird, steuerst du ueber `autoCompactWindow`."),

 ("autoCompactWindow", "Kontext und Gedaechtnis", "settings.json",
  "Legt fest, wie voll das Gedaechtnis wird, bevor zusammengefasst wird.",
  "Set how full the context gets before Claude Code compacts",
  "Diese Einstellung ist die dauerhafte Fassung des Befehls `/autocompact`. Du gibst entweder "
  "`auto` an oder eine Zahl.\n\n"
  "Ein kleines Fenster bedeutet oefter zusammenfassen: schnellere und guenstigere Antworten, "
  "dafuer mehr Verlust an Einzelheiten.\n\n"
  "Ein grosses Fenster behaelt mehr, kostet aber pro Anfrage mehr und wird gegen Ende "
  "erfahrungsgemaess auch ungenauer.\n\n"
  "`auto` ist fuer die meisten der richtige Wert."),

 ("autoMemoryEnabled", "Kontext und Gedaechtnis", "settings.json",
  "Schaltet das automatische Gedaechtnis ein oder aus.",
  "Turn auto memory off or on",
  "Beim automatischen Gedaechtnis merkt sich Claude von allein Dinge ueber dich und deine "
  "Projekte und liest sie in spaeteren Sitzungen wieder ein.\n\n"
  "Das ist bequem: Du musst deine Vorlieben nicht jedes Mal wiederholen. Es hat aber auch eine "
  "Kehrseite — was einmal gespeichert ist, wirkt weiter, auch wenn es inzwischen falsch ist.\n\n"
  "Die Gedaechtniseintraege liegen als Dateien vor und lassen sich lesen und loeschen. Wo, "
  "steuert `autoMemoryDirectory`.\n\n"
  "Ueber `/memory` siehst du, was gespeichert ist."),

 ("cleanupPeriodDays", "Datenhaltung", "settings.json",
  "Wie viele Tage die Gespraechsaufzeichnungen aufbewahrt werden.",
  "Choose how many days Claude Code keeps transcripts before deleting them",
  "Jedes Gespraech wird auf deinem Rechner gespeichert, damit du es mit `/resume` fortsetzen "
  "kannst. Nach der hier eingestellten Zahl von Tagen wird es geloescht.\n\n"
  "Ein kurzer Zeitraum spart Platz und ist datensparsamer. Ein langer erlaubt es, auch nach "
  "Wochen noch nachzusehen, wie etwas entstanden ist.\n\n"
  "Auch die Auswertung durch `/insights` greift auf diese Aufzeichnungen zurueck — mit einem "
  "kurzen Zeitraum sieht sie entsprechend weniger.\n\n"
  "Auf einem geteilten Rechner ist ein kurzer Zeitraum die vorsichtigere Wahl."),

 ("statusLine", "Darstellung", "settings.json",
  "Laesst eine eigene Zeile unter der Eingabe anzeigen.",
  "Run your own command to render a status line below the prompt",
  "Die Statuszeile ist eine Zeile unter dem Eingabefeld, die du frei befuellen kannst — mit "
  "dem Git-Zweig, dem Modell, den bisherigen Kosten, was du willst.\n\n"
  "Du hinterlegst dafuer einen Befehl. Claude Code ruft ihn auf, gibt ihm Angaben zur Sitzung "
  "als JSON mit und zeigt an, was zurueckkommt.\n\n"
  "Der Befehl laeuft haeufig. Er muss deshalb schnell sein — sonst ruckelt die Anzeige.\n\n"
  "Fuer Unteragenten gibt es die eigene Einstellung `subagentStatusLine`."),

 ("language", "Darstellung", "settings.json",
  "Laesst Claude in einer anderen Sprache als Englisch antworten.",
  "Have Claude respond in a language other than English",
  "Traegst du hier `de` oder `Deutsch` ein, antwortet Claude auf Deutsch — auch wenn du auf "
  "Englisch fragst.\n\n"
  "Das betrifft die Antworten. Code, Variablennamen und Befehle bleiben davon unberuehrt, "
  "denn die gehoeren nicht uebersetzt.\n\n"
  "Bei Commit-Nachrichten und Kommentaren im Code lohnt sich eine eigene Regel — sonst "
  "mischen sich die Sprachen.\n\n"
  "Die Einstellung ist bequemer, als es in jeder `CLAUDE.md` erneut hinzuschreiben."),

 ("sandbox", "Sicherheit", "settings.json",
  "Kapselt ausgefuehrte Befehle vom Dateisystem und vom Netz ab.",
  "Isolate Bash commands from your filesystem and network",
  "Die Sandbox ist ein abgesperrter Bereich. Befehle, die Claude ausfuehrt, laufen darin und "
  "kommen nicht ueberall hin — weder im Dateisystem noch im Netz.\n\n"
  "Das ist die staerkste Schutzschicht, die Claude Code bietet. Selbst ein Befehl, der etwas "
  "Dummes tut, richtet ausserhalb der Sandbox keinen Schaden an.\n\n"
  "Sie hat viele Unterpunkte: welche Pfade lesbar und schreibbar sind, welche Rechnernamen "
  "erreichbar sind, wie mit Zugangsdaten umgegangen wird.\n\n"
  "Verfuegbar auf macOS, Linux und unter Windows mit WSL2. Mit "
  "`sandbox.autoAllowBashIfSandboxed` laufen abgeschottete Befehle sogar ohne Rueckfrage — "
  "sicher und bequem zugleich."),

 ("sandbox.enabled", "Sicherheit", "settings.json",
  "Schaltet die Abschottung von Befehlen ein.",
  "Turn on Bash sandboxing on macOS, Linux, and WSL2",
  "Das ist der Hauptschalter der Sandbox. Steht er auf `true`, laufen ausgefuehrte Befehle im "
  "abgesperrten Bereich.\n\n"
  "Manche Befehle brauchen mehr Zugriff, als die Sandbox erlaubt, und schlagen dann fehl. "
  "Ueber `sandbox.excludedCommands` kannst du einzelne davon ausnehmen.\n\n"
  "Kann die Sandbox auf deinem System nicht starten, laeuft der Befehl standardmaessig "
  "trotzdem — ungeschuetzt. Wenn dir das nicht recht ist, setze "
  "`sandbox.failIfUnavailable` auf `true`.\n\n"
  "Zusammen mit `sandbox.autoAllowBashIfSandboxed` bekommst du beides: kaum Rueckfragen und "
  "trotzdem Schutz."),

 ("enabledPlugins", "Erweiterungen", "settings.json",
  "Schaltet einzelne Erweiterungen je Ebene ein oder aus.",
  "Turn individual plugins on or off per scope",
  "Hier steht, welche Erweiterungen aktiv sind. Du kannst das getrennt fuer dich persoenlich "
  "und fuer ein einzelnes Projekt festlegen.\n\n"
  "Das ist praktisch, wenn eine Erweiterung nur in einem bestimmten Projekt Sinn ergibt — dann "
  "schaltest du sie dort ein und sonst nicht.\n\n"
  "Jede aktive Erweiterung bringt Skills, Agenten und Werkzeuge mit, die Platz im Gedaechtnis "
  "brauchen. Weniger ist hier oft mehr.\n\n"
  "Eingerichtet und geaendert wird das bequemer ueber `/plugin`."),

 ("alwaysThinkingEnabled", "Modell und Antworten", "settings.json",
  "Schaltet das ausfuehrliche Nachdenken fuer alle Sitzungen ab.",
  "Turn extended thinking off for every session",
  "Beim ausfuehrlichen Nachdenken arbeitet das Modell erst fuer sich, bevor es antwortet. Das "
  "verbessert die Qualitaet bei schweren Aufgaben deutlich.\n\n"
  "Es kostet aber Zeit und Einheiten. Bei einfachen Handgriffen ist es reine Verschwendung.\n\n"
  "Setzt du diese Einstellung auf `false`, wird nicht mehr vorgedacht.\n\n"
  "Vorsicht in Verbindung mit `effortLevel`: Bei Opus 5 vertraegt sich abgeschaltetes Denken "
  "nicht mit den hohen Denkstufen. Seit 2.1.251 faengt Claude Code das selbst ab."),

 ("fallbackModel", "Modell und Antworten", "settings.json",
  "Nennt Ausweichmodelle fuer den Fall, dass das Hauptmodell ueberlastet ist.",
  "Name backup models for when the primary is overloaded",
  "Wenn das gewuenschte Modell gerade nicht kann, bleibt die Arbeit sonst stehen. Hier "
  "hinterlegst du eine Reihenfolge von Ausweichmodellen.\n\n"
  "Claude Code nimmt dann automatisch das naechste in der Liste, statt abzubrechen.\n\n"
  "Sinnvoll bei langen, unbeaufsichtigten Laeufen — dort ist ein Abbruch besonders aergerlich.\n\n"
  "Bedenke, dass ein schwaecheres Ausweichmodell auch schwaechere Ergebnisse liefert. Bei "
  "wichtigen Aufgaben ist Warten manchmal besser als Ausweichen."),

 ("attribution", "Git und Herkunft", "settings.json",
  "Bestimmt, welchen Hinweis Claude Code an Commits und Pull Requests anhaengt.",
  "Customize the attribution Claude Code adds to commits and pull requests",
  "Standardmaessig haengt Claude Code an jeden Commit eine Zeile an, die auf seine Mitarbeit "
  "hinweist, und ebenso an die Beschreibung eines Pull Requests.\n\n"
  "Ueber `attribution.commit` und `attribution.pr` kannst du den Text aendern oder ganz "
  "weglassen.\n\n"
  "Mit `attribution.sessionUrl` steuerst du, ob zusaetzlich ein Link auf die Sitzung "
  "mitgeschickt wird — der kann in einem oeffentlichen Projekt unerwuenscht sein.\n\n"
  "Die frueheren Einstellung `includeCoAuthoredBy` gilt als veraltet; nimm diese hier."),

 ("editorMode", "Darstellung", "settings.json",
  "Schaltet die Eingabezeile auf Vim-Tastenbelegung um.",
  "Use vim key bindings in the input prompt",
  "Wer Vim gewohnt ist, will auch beim Schreiben einer Eingabe mit `h`, `j`, `k` und `l` "
  "navigieren koennen. Setzt du diese Einstellung auf `vim`, geht das.\n\n"
  "Es gibt dann einen Einfuege- und einen Befehlsmodus, wie in Vim ueblich.\n\n"
  "Wer Vim nicht kennt, sollte die Finger davon lassen — man kommt sonst schnell in einen "
  "Zustand, in dem das Tippen nichts mehr bewirkt.\n\n"
  "Mit der Escape-Taste kommst du zurueck in den Befehlsmodus, mit `i` wieder ins Schreiben."),

 ("disableAllHooks", "Automatisierung", "settings.json",
  "Schaltet Hooks, eigene Statuszeile und eigene Dateivorschlaege auf einmal ab.",
  "Turn off hooks, a custom status line, and a custom file suggestion command at once",
  "Wenn du vermutest, dass ein eigener Hook Probleme macht, kannst du hier alles auf einmal "
  "stilllegen, statt jeden Eintrag einzeln zu suchen.\n\n"
  "Betroffen sind die Hooks, die eigene Statuszeile und der eigene Befehl fuer die "
  "Dateivorschlaege.\n\n"
  "Das ist die schnellste Eingrenzung bei einem Problem: Verschwindet der Fehler, lag es an "
  "einem dieser drei.\n\n"
  "Denk daran, den Schalter danach wieder zurueckzunehmen — sonst laeuft deine Automatik "
  "dauerhaft nicht mehr."),

 ("apiKeyHelper", "Anmeldung", "settings.json",
  "Laesst einen eigenen Befehl die Zugangsdaten erzeugen.",
  "Generate the API credential with your own command",
  "In manchen Umgebungen kommen Zugangsdaten nicht aus einer festen Variable, sondern aus "
  "einem Tresor oder werden regelmaessig neu ausgestellt.\n\n"
  "Hier hinterlegst du einen Befehl, den Claude Code aufruft, um den jeweils gueltigen Zugang "
  "zu bekommen.\n\n"
  "Der Vorteil: Der Schluessel steht nirgends fest in einer Datei und laeuft nicht ab.\n\n"
  "Der Befehl wird oft aufgerufen — er sollte deshalb schnell antworten und nichts ins "
  "Terminal schreiben."),

 ("respectGitignore", "Projekt", "settings.json",
  "Haelt ignorierte Dateien aus der Dateiauswahl heraus.",
  "Keep gitignored files out of the @ file picker",
  "In der Datei `.gitignore` steht, was nicht ins Versionsverzeichnis gehoert — Bauergebnisse, "
  "Zwischenstaende, Abhaengigkeiten.\n\n"
  "Steht diese Einstellung auf `true`, tauchen genau diese Dateien auch nicht mehr auf, wenn "
  "du mit dem Klammeraffen eine Datei suchst.\n\n"
  "Das macht die Auswahl viel uebersichtlicher: Ohne sie stehen dort tausende Dateien aus dem "
  "Abhaengigkeitsordner.\n\n"
  "Brauchst du ausnahmsweise doch eine davon, kannst du den Pfad immer noch von Hand "
  "eintippen."),

 ("autoUpdatesChannel", "Aktualisierung", "settings.json",
  "Bestimmt, ob du die neuesten oder nur die stabilen Fassungen bekommst.",
  "Follow the stable release channel instead of latest",
  "Claude Code aktualisiert sich selbst. Ueber diese Einstellung waehlst du, welchem Strang "
  "du folgst.\n\n"
  "`latest` bringt neue Funktionen sofort, manchmal aber auch neue Fehler. `stable` hinkt "
  "etwas hinterher und ist dafuer zuverlaessiger.\n\n"
  "In einem Team lohnt sich `stable`: Sonst arbeiten alle mit unterschiedlichen Fassungen und "
  "Fehler lassen sich schwer nachstellen.\n\n"
  "Mit `minimumVersion`, `requiredMinimumVersion` und `requiredMaximumVersion` laesst sich "
  "der Rahmen zusaetzlich eingrenzen."),

 ("fileCheckpointingEnabled", "Datenhaltung", "settings.json",
  "Schaltet die Sicherungspunkte fuer Dateien ein oder aus.",
  "Turn off or on the file snapshots that /rewind restores",
  "Claude Code legt vor Aenderungen Sicherungspunkte an. Genau die holt `/rewind` zurueck, "
  "wenn etwas schiefgegangen ist.\n\n"
  "Schaltest du sie ab, sparst du Plattenplatz — verlierst aber die Moeglichkeit, Dateien "
  "auf einen frueheren Stand zurueckzusetzen.\n\n"
  "In einem Projekt mit Git ist der Verlust ueberschaubar: Dort ist ohnehin alles "
  "nachvollziehbar, was eingecheckt wurde.\n\n"
  "Ohne Git wuerde ich sie unbedingt anlassen."),

 ("skillListingBudgetFraction", "Kontext und Gedaechtnis", "settings.json",
  "Bestimmt, wie viel Platz die Liste der Skills im Gedaechtnis bekommt.",
  "Reserve more or less context for the skill listing",
  "Damit Claude einen Skill von allein benutzen kann, muss dessen Beschreibung im Gedaechtnis "
  "stehen. Bei vielen Skills wird das viel Text.\n\n"
  "Diese Einstellung legt fest, welcher Anteil des Gedaechtnisses dafuer reserviert wird. "
  "Passt nicht alles hinein, fallen Beschreibungen weg — und die betroffenen Skills werden "
  "nicht mehr von allein gefunden.\n\n"
  "Zusammen mit `skillListingMaxDescChars` kannst du steuern, ob du lieber viele kurze oder "
  "wenige ausfuehrliche Beschreibungen hast.\n\n"
  "Ob dein Budget reicht, sagt dir `/doctor`."),

 ("promptCacheTtl", "Kosten", "settings.json",
  "Wie lange der Zwischenspeicher fuer das Hauptgespraech vorhaelt.",
  "Choose the prompt cache lifetime for the main conversation",
  "Wiederholt gesendeter Text wird deutlich billiger abgerechnet, solange er noch im "
  "Zwischenspeicher liegt. Das ist der groesste Sparhebel ueberhaupt.\n\n"
  "Diese Einstellung legt fest, wie lange der Speicher vorhaelt. Ein langer Zeitraum lohnt "
  "sich, wenn du zwischen deinen Eingaben laengere Pausen machst.\n\n"
  "Wie gut der Speicher greift, siehst du seit Version 2.1.251 in `/cost`.\n\n"
  "Fuer Unteragenten und Hintergrundanfragen gibt es die eigene Einstellung "
  "`subagentPromptCacheTtl`."),

 ("claudeMdExcludes", "Kontext und Gedaechtnis", "settings.json",
  "Laesst bestimmte `CLAUDE.md`-Dateien beim Laden weg.",
  "Skip specific CLAUDE.md files when memory loads",
  "Claude Code sucht `CLAUDE.md`-Dateien in mehreren Ebenen und laedt sie alle. In einem "
  "grossen Projekt kommt so einiges zusammen.\n\n"
  "Hier traegst du ein, welche davon uebersprungen werden sollen — zum Beispiel eine veraltete "
  "Datei in einem Unterordner.\n\n"
  "Das spart Platz im Gedaechtnis, und weniger Anweisungen bedeuten erfahrungsgemaess, dass "
  "die verbleibenden besser befolgt werden.\n\n"
  "Was gerade geladen ist, siehst du mit `/context` und `/memory`."),

 ("disableBundledSkills", "Erweiterungen", "settings.json",
  "Schaltet die mitgelieferten Skills und Arbeitsablaeufe ab.",
  "Turn off the skills and workflows included with Claude Code",
  "Claude Code bringt Skills mit, etwa `/code-review`, `/debug` oder `/dataviz`. Sie stehen "
  "sofort zur Verfuegung.\n\n"
  "Wenn du eigene Entsprechungen hast, stoeren die mitgelieferten: Sie belegen Platz im "
  "Gedaechtnis und koennen dir dazwischenfunken.\n\n"
  "Diese Einstellung schaltet sie alle auf einmal ab.\n\n"
  "Willst du nur einzelne loswerden, ist `skillOverrides` der feinere Weg — dort kannst du "
  "einen Skill gezielt verstecken."),

 ("skillOverrides", "Erweiterungen", "settings.json",
  "Versteckt oder kuerzt einzelne Skills, ohne deren Datei zu aendern.",
  "Hide or collapse a skill without editing its SKILL.md",
  "Manchmal soll ein einzelner Skill weg — weil er stoert, weil du eine eigene Fassung hast "
  "oder weil seine Beschreibung zu viel Platz frisst.\n\n"
  "Hier kannst du ihn verstecken oder seine Beschreibung kuerzen lassen, ohne die "
  "Skill-Datei selbst anzufassen.\n\n"
  "Der Vorteil: Kommt eine neue Fassung des Skills, bleibt deine Anpassung erhalten.\n\n"
  "Ein verstecker Skill ist wirklich weg — auch fuer den direkten Aufruf mit Schraegstrich."),

 ("spinnerTipsEnabled", "Darstellung", "settings.json",
  "Blendet die Tipps aus, die waehrend der Arbeit angezeigt werden.",
  "Hide tips in the spinner while Claude works",
  "Waehrend Claude arbeitet, laeuft eine kleine Anzeige, in der wechselnde Tipps stehen.\n\n"
  "Fuer Neulinge ist das nuetzlich. Wenn man Claude Code laenger benutzt, ist es nur noch "
  "Unruhe im Bild.\n\n"
  "Setzt du die Einstellung auf `false`, bleibt die Anzeige ruhig.\n\n"
  "Eigene Tipps kannst du ueber `spinnerTipsOverride` einspielen, eigene Taetigkeitswoerter "
  "ueber `spinnerVerbs`."),

 ("prefersReducedMotion", "Darstellung", "settings.json",
  "Reduziert oder entfernt Animationen.",
  "Reduce or turn off spinner, shimmer, and flash animations",
  "Bewegte Elemente koennen stoeren oder — bei manchen Menschen — sogar unwohl machen. Diese "
  "Einstellung nimmt sie zurueck.\n\n"
  "Betroffen sind die Lade-Anzeige, das Schimmern und aufblitzende Hinweise.\n\n"
  "Auch bei einer langsamen Verbindung, etwa ueber SSH, ist das angenehmer: Weniger Bewegung "
  "bedeutet weniger uebertragene Zeichen.\n\n"
  "Fuer Bildschirmleser gibt es zusaetzlich `axScreenReader`."),

 ("axScreenReader", "Barrierefreiheit", "settings.json",
  "Gibt die Ausgabe so aus, dass ein Bildschirmleser damit zurechtkommt.",
  "Render screen-reader friendly output",
  "Rahmen, Farben und Animationen sind fuer das Auge gedacht. Ein Bildschirmleser liest sie "
  "als Zeichensalat vor.\n\n"
  "Diese Einstellung schaltet auf flachen Text um: keine schmueckenden Rahmen, keine "
  "Animationen, eine klare Reihenfolge.\n\n"
  "Ueber die Umgebungsvariablen `CLAUDE_AX_STARTUP_QUIET_MS` und `CLAUDE_AX_PREPARK_MS` "
  "laesst sich zusaetzlich einstellen, wie schnell neue Zeilen erscheinen.\n\n"
  "Auf der Kommandozeile geht dasselbe mit dem Schalter `--ax-screen-reader`."),

 ("agent", "Agenten und Sitzungen", "settings.json",
  "Startet jede Sitzung als benannter Unteragent.",
  "Start every session as a named subagent with its prompt, tools, and model",
  "Normalerweise startet Claude Code als es selbst. Traegst du hier einen Agentennamen ein, "
  "startet stattdessen dieser Agent — mit seiner Beschreibung, seinem Werkzeugsatz und seinem "
  "Modell.\n\n"
  "Damit kannst du eine Umgebung fest auf eine Rolle einstellen, etwa auf reines Pruefen "
  "ohne Schreibrechte.\n\n"
  "Das ist besonders nuetzlich in einem Projekt, in dem immer dieselbe Art Arbeit anfaellt.\n\n"
  "Auf der Kommandozeile geht dasselbe einmalig mit `--agent`."),

 ("disableWorkflows", "Automatisierung", "settings.json",
  "Schaltet die dynamischen Arbeitsablaeufe fuer alle ab.",
  "Turn dynamic workflows off for everyone",
  "Ein Arbeitsablauf kann sehr viele Agenten gleichzeitig starten. Das ist maechtig — und es "
  "kann sehr teuer werden.\n\n"
  "Diese Einstellung schaltet die Moeglichkeit fuer alle ab, die von den Einstellungen "
  "betroffen sind.\n\n"
  "Fuer dich selbst nimmst du stattdessen `enableWorkflows`.\n\n"
  "Ohne diese Sperre werden Arbeitsablaeufe ohnehin nur auf ausdrueckliche Anweisung "
  "gestartet, nie von allein."),

 ("preferredNotifChannel", "Mitteilungen", "settings.json",
  "Waehlt, wie du benachrichtigt wirst, wenn eine Aufgabe fertig ist.",
  "Choose a terminal bell or desktop notification for task completion",
  "Wenn Claude laenger arbeitet, willst du nicht daneben sitzen und warten. Diese Einstellung "
  "legt fest, wie du erfaehrst, dass es fertig ist.\n\n"
  "Zur Wahl stehen der Signalton des Terminals und eine Mitteilung des Betriebssystems.\n\n"
  "Der Signalton funktioniert ueberall, auch ueber SSH. Die Systemmitteilung ist auffaelliger "
  "und bleibt stehen, wenn du gerade nicht hinsiehst.\n\n"
  "Fuer Mitteilungen aufs Handy gibt es `agentPushNotifEnabled` und `inputNeededNotifEnabled`."),

 ("inputNeededNotifEnabled", "Mitteilungen", "settings.json",
  "Schickt eine Mitteilung aufs Handy, wenn Claude auf dich wartet.",
  "Get a push notification when Claude is waiting on you",
  "Das ist die wichtigste Mitteilung ueberhaupt: Claude ist auf eine Rueckfrage gestossen und "
  "kommt ohne deine Antwort nicht weiter.\n\n"
  "Ohne diese Mitteilung merkst du das erst, wenn du wieder hinsiehst — und bis dahin ist "
  "nichts passiert.\n\n"
  "Sie setzt die Handy-App und eingerichtete Fernsteuerung voraus.\n\n"
  "Wie lange auf eine Antwort gewartet wird, bevor von allein weitergemacht wird, steuert "
  "`askUserQuestionTimeout`."),

 ("askUserQuestionTimeout", "Arbeitsweise", "settings.json",
  "Laesst eine unbeantwortete Rueckfrage nach einer Wartezeit von allein weiterlaufen.",
  "Let an unanswered question auto-continue after idle time",
  "Wenn Claude eine Rueckfrage stellt und niemand antwortet, steht die Arbeit. Bei einem "
  "unbeaufsichtigten Lauf ist das aergerlich.\n\n"
  "Diese Einstellung legt fest, nach welcher Wartezeit die naheliegendste Antwort genommen "
  "und weitergemacht wird.\n\n"
  "Das ist bequem, hat aber einen Preis: Eine Entscheidung, die eigentlich dir gehoert, faellt "
  "dann ohne dich.\n\n"
  "Fuer wichtige Entscheidungen ist Warten die richtige Wahl. Die zugehoerigen "
  "Umgebungsvariablen heissen `CLAUDE_AFK_TIMEOUT_MS` und `CLAUDE_AFK_COUNTDOWN_MS`."),

 ("autoContinueAtUsageLimit", "Konto", "settings.json",
  "Wartet bei erschoepftem Kontingent und macht danach von allein weiter.",
  "Wait in the open session and continue the task automatically after a usage limit resets",
  "Wenn dein Kontingent aufgebraucht ist, bricht die Arbeit normalerweise ab. Mit dieser "
  "Einstellung bleibt die Sitzung stehen und wartet, bis das Kontingent sich zuruecksetzt.\n\n"
  "Danach macht sie genau dort weiter, wo sie war — ohne dass du etwas tun musst.\n\n"
  "Das ist bei langen, unbeaufsichtigten Laeufen sehr angenehm.\n\n"
  "Die Wartezeit kann allerdings Stunden betragen. Wenn du das Ergebnis frueher brauchst, ist "
  "ein Modellwechsel der schnellere Weg."),

 ("crossSessionInbound", "Sicherheit", "settings.json",
  "Bestimmt, ob Nachrichten aus deinen anderen Sitzungen ankommen duerfen.",
  "Choose whether Claude Code delivers messages from your other sessions",
  "Deine Sitzungen koennen einander Nachrichten schicken. Das ist nuetzlich, aber auch ein "
  "Einfallstor: Was eine andere Sitzung schickt, ist eine Anweisung von aussen.\n\n"
  "Es gibt drei Stufen: `accept` liefert aus, `hold` zeigt nur einen Hinweis ohne Inhalt, "
  "`refuse` lehnt ganz ab.\n\n"
  "Besonderheit: Eine strengere Einstellung im Projekt gilt auch dann, wenn die Organisation "
  "eine lockerere vorgibt. Sicherheit gewinnt hier.\n\n"
  "Auf einem Rechner mit fremdem Code ist `hold` oder `refuse` die vorsichtige Wahl."),

 ("isolatePeerMachines", "Sicherheit", "settings.json",
  "Fragt nach, bevor Claude eine deiner Sitzungen auf einem anderen Rechner anspricht.",
  "Ask you before Claude messages one of your sessions on another machine",
  "Sitzungen auf verschiedenen Rechnern koennen miteinander reden. Eine Anweisung springt "
  "damit von einem Rechner auf den naechsten.\n\n"
  "Diese Einstellung schiebt eine Rueckfrage dazwischen: Du entscheidest, ob die Nachricht "
  "rausgehen darf.\n\n"
  "Auch hier gilt die Sicherheitsausnahme: Ein `true` wird beachtet, selbst wenn die "
  "Organisation `false` vorgibt.\n\n"
  "Immer sinnvoll, wenn ein Rechner in einem anderen Netz steht oder anderen Regeln unterliegt."),
]
