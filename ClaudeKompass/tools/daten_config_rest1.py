# -*- coding: utf-8 -*-
"""Weitere Config-Einstellungen, kompakt erklaert (a bis m).
Format: name, kategorie, quelle, kurz, englisch, erklaerung."""

CONFIG_REST_1 = [
 ("advisorModel", "Modell und Antworten", "settings.json",
  "Waehlt das Modell, das der Ratgeber benutzt.",
  "Pick which model answers when Claude asks the advisor tool",
  "Der Ratgeber ist ein zweites Modell, das Claude bei schweren Stellen um Rat fragen kann. "
  "Hier legst du fest, welches das sein soll.\n\n"
  "Ein staerkeres Modell gibt bessere Einschaetzungen, kostet aber pro Nachfrage mehr. "
  "Eingeschaltet wird der Ratgeber selbst ueber den Befehl `/advisor`."),

 ("agentPushNotifEnabled", "Mitteilungen", "settings.json",
  "Erlaubt Claude, dir von sich aus eine Mitteilung aufs Handy zu schicken.",
  "Let Claude send a push notification to your phone when it decides to",
  "Anders als die Mitteilung bei einer Rueckfrage entscheidet Claude hier selbst, wann sich "
  "eine Meldung lohnt — etwa wenn eine lange Aufgabe fertig ist.\n\n"
  "Setzt die Handy-App und eingerichtete Fernsteuerung voraus. Ohne diese Einstellung bleibt "
  "das Handy still, auch wenn etwas Wichtiges passiert."),

 ("allowAllClaudeAiMcps", "Integrationen", "settings.json",
  "Laedt zusaetzlich die von claude.ai geholten Dienste, obwohl eine feste Liste vorgegeben ist.",
  "Load the claude.ai connectors Claude Code fetches itself alongside a deployed managed-mcp.json",
  "Wenn eine Organisation eine feste Liste erlaubter Zusatzdienste vorgibt, gilt normalerweise "
  "nur diese. Mit dieser Einstellung kommen die persoenlich verbundenen Dienste von claude.ai "
  "noch dazu.\n\n"
  "Das ist bequem, weicht aber die Vorgabe auf — in einem geregelten Umfeld sollte es "
  "abgestimmt sein."),

 ("allowedChannelPlugins", "Erweiterungen", "settings.json",
  "Legt fest, welche Erweiterungen Nachrichten in Kanaele schreiben duerfen.",
  "Replace the default allowlist of channel plugins that can push messages",
  "Manche Erweiterungen koennen von sich aus Nachrichten in einen Kanal schicken. Diese Liste "
  "bestimmt, welche das duerfen.\n\n"
  "Eine kurze Liste ist die sichere Wahl: Was hier steht, kann Inhalte aus deiner Sitzung nach "
  "aussen tragen."),

 ("allowedHttpHookUrls", "Automatisierung", "settings.json",
  "Grenzt ein, welche Adressen ein HTTP-Hook ansprechen darf.",
  "Limit which URLs HTTP hooks can target",
  "Ein HTTP-Hook schickt bei bestimmten Ereignissen Daten an eine Adresse im Netz. Ohne "
  "Einschraenkung koennte das jede beliebige Adresse sein.\n\n"
  "Diese Liste macht daraus eine bewusste Auswahl. In einem Umfeld mit vertraulichem Code ist "
  "sie Pflicht, nicht Kuer."),

 ("allowedMcpServers", "Integrationen", "settings.json",
  "Legt fest, welche Zusatzdienste ueberhaupt benutzt werden duerfen.",
  "Allowlist which MCP servers people can use",
  "Ein Zusatzdienst laeuft mit den Rechten deiner Sitzung und sieht, was du siehst. Diese "
  "Liste bestimmt, welche zugelassen sind.\n\n"
  "Das Gegenstueck heisst `deniedMcpServers` und sperrt einzelne. Ist eine Erlaubnisliste "
  "gesetzt, gilt alles andere als nicht zugelassen."),

 ("allowManagedHooksOnly", "Automatisierung", "settings.json",
  "Laesst nur die Hooks laufen, die die Organisation ausliefert.",
  "Run only the hooks your organization deploys",
  "Eigene Hooks werden damit ignoriert — es zaehlen ausschliesslich die zentral verteilten.\n\n"
  "Sinnvoll dort, wo Hooks Teil der Sicherheitsvorgaben sind und ein eigener Hook sie "
  "aushebeln koennte."),

 ("allowManagedMcpServersOnly", "Integrationen", "settings.json",
  "Macht die zentral verwaltete Dienstliste zur einzig gueltigen.",
  "Make the managed MCP allowlist the only one that applies",
  "Eigene Eintraege in Projekt- oder Nutzerdateien werden nicht mehr beachtet. Es gilt nur, "
  "was die Organisation vorgibt.\n\n"
  "Das schliesst die Luecke, dass jemand einen eigenen Dienst nachtraegt, der nicht geprueft ist."),

 ("allowManagedPermissionRulesOnly", "Berechtigungen", "settings.json",
  "Macht die zentral verwalteten Berechtigungsregeln zur einzigen Quelle.",
  "Make managed settings the only source of permission rules",
  "Normalerweise werden Berechtigungsregeln aus allen Ebenen zusammengefuehrt. Mit dieser "
  "Einstellung zaehlen nur noch die zentral verteilten.\n\n"
  "Damit kann eine eigene Erlaubnisliste keine Vorgabe mehr aufweichen."),

 ("autoConnectIde", "Integrationen", "~/.claude.json",
  "Verbindet sich von allein mit einer laufenden Entwicklungsumgebung.",
  "Connect to a running VS Code or JetBrains IDE automatically from an external terminal",
  "Startest du Claude Code in einem eigenen Terminalfenster, sucht es die laufende "
  "Entwicklungsumgebung und verbindet sich damit.\n\n"
  "Dann sieht es die geoeffnete Datei und kann Unterschiede dort anzeigen. Wenn du das nicht "
  "willst, schaltest du es hier ab."),

 ("autoInstallIdeExtension", "Integrationen", "~/.claude.json",
  "Installiert die Erweiterung fuer die Entwicklungsumgebung automatisch.",
  "Turn off automatic install of the IDE extension from a VS Code terminal",
  "Startest du Claude Code aus dem Terminal in VS Code, wird die passende Erweiterung von "
  "allein nachinstalliert.\n\n"
  "Wer genau bestimmen will, was auf seinem Rechner installiert wird, schaltet das ab und "
  "installiert von Hand."),

 ("autoMemoryDirectory", "Kontext und Gedaechtnis", "settings.json",
  "Bestimmt, wo das automatische Gedaechtnis abgelegt wird.",
  "Store auto memory in a directory you choose",
  "Standardmaessig liegen die selbst gemerkten Notizen an einem festen Ort. Hier kannst du "
  "einen anderen Ordner waehlen.\n\n"
  "Nuetzlich, wenn du das Gedaechtnis mitsichern oder zwischen Rechnern abgleichen willst."),

 ("autoMode", "Berechtigungen", "settings.json",
  "Ergaenzt eigene Regeln fuer die Entscheidung im Auto-Modus.",
  "Add your own allow and deny rules to the auto mode classifier",
  "Im Auto-Modus entscheidet eine Einordnung, ob ein Befehl ohne Rueckfrage laufen darf. Hier "
  "kannst du eigene Regeln ergaenzen.\n\n"
  "Damit passt du die Automatik an dein Projekt an — etwa indem du ein hauseigenes Werkzeug "
  "als harmlos einstufst."),

 ("autoMode.classifyAllShell", "Berechtigungen", "settings.json",
  "Schickt jeden Befehl durch die Einordnung, auch bereits erlaubte.",
  "Send every shell command through the auto mode classifier, even ones a narrow allow rule matches",
  "Normalerweise wird ein Befehl, den eine enge Erlaubnisregel abdeckt, direkt durchgelassen. "
  "Mit dieser Einstellung wird trotzdem geprueft.\n\n"
  "Das ist gruendlicher, kostet aber bei jedem Aufruf einen Moment mehr."),

 ("autoScrollEnabled", "Darstellung", "settings.json",
  "Laesst die Anzeige der neuen Ausgabe nach unten folgen.",
  "Follow new output to the bottom in fullscreen rendering",
  "Bei laufender Ausgabe springt die Ansicht mit ans Ende, damit du immer das Neueste siehst.\n\n"
  "Wenn du beim Lesen weiter oben staendig nach unten gerissen wirst, schaltest du es hier ab."),

 ("awaySummaryEnabled", "Darstellung", "settings.json",
  "Zeigt eine Zusammenfassung, wenn du ans Terminal zurueckkommst.",
  "Turn off the session recap shown when you come back to the terminal",
  "Warst du eine Weile weg, fasst Claude Code kurz zusammen, was in der Zwischenzeit passiert "
  "ist.\n\n"
  "Das erspart das Zurueckscrollen. Wer lieber selbst nachsieht, schaltet es ab."),

 ("awsAuthRefresh", "Anmeldung", "settings.json",
  "Erneuert abgelaufene Bedrock-Zugangsdaten mit einem eigenen Befehl.",
  "Refresh expired Bedrock credentials in .aws with your own command",
  "Wer ueber Amazon Bedrock arbeitet, hat oft kurzlebige Zugangsdaten. Hier hinterlegst du den "
  "Befehl, der sie erneuert.\n\n"
  "Claude Code ruft ihn auf, wenn die Daten abgelaufen sind, statt mit einem Fehler stehenzubleiben."),

 ("awsCredentialExport", "Anmeldung", "settings.json",
  "Liefert Bedrock-Zugangsdaten als JSON aus einem eigenen Befehl.",
  "Supply Bedrock credentials as JSON from your own command",
  "Eine Variante zum vorigen Punkt: Statt eine Datei zu erneuern, gibt dein Befehl die "
  "Zugangsdaten direkt als JSON zurueck.\n\n"
  "Praktisch, wenn die Daten aus einem Tresor kommen und nie auf der Platte liegen sollen."),

 ("blockedMarketplaces", "Erweiterungen", "settings.json",
  "Sperrt Bezugsquellen fuer Erweiterungen.",
  "Block plugin marketplace sources for your organization",
  "Erweiterungen kommen aus Marktplaetzen. Was hier steht, wird abgelehnt — unabhaengig davon, "
  "wer es installieren will.\n\n"
  "Das Gegenstueck `strictKnownMarketplaces` dreht es um und laesst nur Genanntes zu."),

 ("browserExternalPageTools", "Sicherheit", "settings.json",
  "Haelt Claudes Werkzeuge von fremden Seiten fern.",
  "Keep Claude's tools off external pages in the desktop Browser pane",
  "Im Browser-Bereich der Desktop-Anwendung kann Claude Seiten bedienen. Diese Einstellung "
  "begrenzt das auf die eigenen Seiten.\n\n"
  "Wichtig, weil der Browser auf deine angemeldeten Konten Zugriff hat."),

 ("channelsEnabled", "Zusammenarbeit", "settings.json",
  "Erlaubt Kanaele fuer die Organisation.",
  "Allow channels for your organization",
  "Ueber Kanaele koennen mehrere Beteiligte an einem Gespraech teilnehmen. Diese Einstellung "
  "schaltet die Moeglichkeit frei.\n\n"
  "Ohne sie bleibt jede Sitzung fuer sich."),

 ("claudeMd", "Kontext und Gedaechtnis", "managed-settings.json",
  "Spielt organisationsweite Anweisungen in jede Sitzung ein.",
  "Inject organization-wide CLAUDE.md instructions from managed settings",
  "Damit lassen sich Vorgaben verteilen, die in jedem Projekt gelten sollen — ohne in jedem "
  "Projekt eine Datei anzulegen.\n\n"
  "Bedenke: Der Text wird in JEDER Sitzung geladen und kostet dort Platz. Kurz halten."),

 ("companyAnnouncements", "Darstellung", "settings.json",
  "Zeigt Mitteilungen der Organisation beim Start.",
  "Show your organization's announcements at startup",
  "Damit erreichen Ankuendigungen alle, die Claude Code benutzen — etwa ein Hinweis auf eine "
  "geaenderte Vorgabe.\n\n"
  "Sinnvoll sparsam einsetzen: Was bei jedem Start erscheint, wird schnell ueberlesen."),

 ("defaultShell", "Arbeitsumgebung", "settings.json",
  "Waehlt, welche Kommandozeile deine Ausrufezeichen-Befehle ausfuehrt.",
  "Choose whether Bash or PowerShell runs the shell commands you type with the ! prefix",
  "Tippst du einen Befehl mit einem Ausrufezeichen davor, laeuft er direkt in der "
  "Kommandozeile. Hier legst du fest, in welcher.\n\n"
  "Unter Windows ist das wichtig: Bash und PowerShell haben voellig verschiedene Schreibweisen "
  "fuer Pfade, Variablen und Umleitungen."),

 ("deniedMcpServers", "Integrationen", "settings.json",
  "Sperrt einzelne Zusatzdienste.",
  "Block specific MCP servers by URL, command, or name",
  "Hier werden gezielt einzelne Dienste ausgeschlossen — ueber Adresse, Startbefehl oder Namen.\n\n"
  "Eine Sperre schlaegt eine Erlaubnis: Was hier steht, laeuft auch dann nicht, wenn es "
  "anderswo erlaubt ist."),

 ("dialogExpiry", "Arbeitsweise", "settings.json",
  "Legt fest, wie lange auf eine weitergereichte Rueckfrage gewartet wird.",
  "Set how long Claude Code waits before it cancels a forwarded dialog",
  "Wird eine Rueckfrage an die Fernsteuerung oder eine andere Umgebung weitergereicht, wartet "
  "Claude Code auf die Antwort. Hier bestimmst du, wie lange.\n\n"
  "Danach wird die Rueckfrage zurueckgezogen, statt die Sitzung endlos haengen zu lassen."),

 ("diffTool", "Darstellung", "~/.claude.json",
  "Bestimmt, wo vorgeschlagene Aenderungen angezeigt werden.",
  "Choose whether Claude's proposed file changes open in the IDE diff viewer or stay in the terminal",
  "Aenderungen kannst du im Terminal ansehen oder in der Vergleichsansicht deiner "
  "Entwicklungsumgebung.\n\n"
  "Die Entwicklungsumgebung ist bei groesseren Aenderungen deutlich uebersichtlicher, das "
  "Terminal ist ueberall verfuegbar."),

 ("disableAgentView", "Agenten und Sitzungen", "settings.json",
  "Schaltet Hintergrundagenten und deren Ansicht ab.",
  "Turn off background agents and agent view",
  "Ohne Hintergrundagenten laeuft alles im Vordergrund und ist unmittelbar sichtbar.\n\n"
  "Das ist uebersichtlicher, nimmt aber die Moeglichkeit, lange Aufgaben nebenher laufen zu lassen."),

 ("disableAutoMode", "Berechtigungen", "settings.json",
  "Nimmt den Auto-Modus aus der Auswahl.",
  "Remove auto mode from the permission mode cycle",
  "Der Auto-Modus laesst Claude bei vielen Befehlen selbst entscheiden. Wo das nicht erwuenscht "
  "ist, wird er hier entfernt.\n\n"
  "Seit Version 2.1.251 wirkt die Einstellung auch mitten in einer laufenden Sitzung: Eine "
  "bereits im Auto-Modus laufende Sitzung faellt zurueck in den normalen Modus."),

 ("disableBrowserExternalNavigation", "Sicherheit", "settings.json",
  "Begrenzt den Browser-Bereich auf den eigenen Rechner.",
  "Limit the desktop Browser pane to localhost for people and Claude",
  "Damit kann im Browser-Bereich nur noch geoeffnet werden, was auf dem eigenen Rechner laeuft "
  "— typischerweise die Anwendung, an der du gerade baust.\n\n"
  "Fremde Seiten bleiben aussen vor. Das gilt fuer dich genauso wie fuer Claude."),

 ("disableClaudeAiConnectors", "Integrationen", "settings.json",
  "Verhindert, dass Claude Code die Dienste von claude.ai holt.",
  "Turn off claude.ai connectors so Claude Code doesn't fetch them",
  "Normalerweise werden die auf claude.ai verbundenen Dienste mitgeladen. Diese Einstellung "
  "unterbindet das.\n\n"
  "Besonderheit: Ein `true` gilt aus jeder Ebene — auch wenn die Organisation `false` vorgibt. "
  "Sicherheit gewinnt hier."),

 ("disableCommandPluginSources", "Erweiterungen", "settings.json",
  "Sperrt Erweiterungen, die sich ueber einen Befehl installieren.",
  "Block plugins that install by running a marketplace-declared command",
  "Manche Erweiterungen installieren sich, indem ein im Marktplatz hinterlegter Befehl laeuft. "
  "Dieser Befehl kann alles tun, was du auch koenntest.\n\n"
  "Wo das zu weit geht, schliesst diese Einstellung den Weg."),

 ("disableDeepLinkRegistration", "Integrationen", "settings.json",
  "Verhindert, dass sich Claude Code als Handler fuer eigene Links eintraegt.",
  "Stop Claude Code from registering the claude-cli:// handler",
  "Normalerweise traegt sich Claude Code als zustaendig fuer Links der Form `claude-cli://` "
  "ein. Damit lassen sich Sitzungen von einer Webseite aus oeffnen.\n\n"
  "Wer keine Aenderungen an der Systemregistrierung will, schaltet das ab."),

 ("disableDesktopLocalSessions", "Integrationen", "settings.json",
  "Schaltet Desktop-Sitzungen auf dem Geraet selbst ab.",
  "Turn off Desktop Code sessions that run on the device",
  "Verbindungen zu anderen Rechnern und in die Cloud bleiben moeglich; nur das Laufen direkt "
  "auf diesem Geraet faellt weg.\n\n"
  "Gedacht fuer Umgebungen, in denen lokal nichts ausgefuehrt werden darf."),

 ("disabledMcpjsonServers", "Integrationen", "settings.json",
  "Lehnt bestimmte Dienste aus der Projektdatei ab.",
  "Reject specific servers from a project's .mcp.json",
  "Ein Projekt kann in seiner `.mcp.json` Dienste mitbringen. Hier bestimmst du, welche davon "
  "trotzdem nicht laufen.\n\n"
  "Sinnvoll bei fremden Projekten: Du liest den Code, aber die mitgelieferten Dienste sollen "
  "nicht ungeprueft starten."),

 ("disableMobileSimulatorTools", "Sicherheit", "settings.json",
  "Sperrt Claudes Werkzeuge im iOS-Simulator der Desktop-Anwendung.",
  "Block Claude's tools in the desktop iOS Simulator pane",
  "Im Simulator kann Claude eine App bedienen und pruefen. Diese Einstellung nimmt ihm das.\n\n"
  "Du kannst den Simulator weiterhin selbst benutzen — nur die Werkzeuge sind gesperrt."),

 ("disableRemoteControl", "Sicherheit", "settings.json",
  "Schaltet die Fernsteuerung ueberall ab.",
  "Turn off Remote Control everywhere it can start",
  "Die Fernsteuerung erlaubt es, eine Sitzung von einem anderen Geraet aus zu begleiten. Das "
  "ist bequem und zugleich ein Zugang von aussen.\n\n"
  "Wo das nicht erwuenscht ist, schliesst diese Einstellung ihn vollstaendig."),

 ("disableSideloadFlags", "Sicherheit", "settings.json",
  "Lehnt die Kommandozeilen-Schalter ab, die Erweiterungen direkt einspielen.",
  "Reject the CLI flags that sideload plugins, subagents, and MCP servers",
  "Ueber bestimmte Schalter lassen sich Erweiterungen, Agenten und Dienste am regulaeren Weg "
  "vorbei einspielen.\n\n"
  "Diese Einstellung schliesst diese Umgehung — sonst waeren alle anderen Sperren wirkungslos."),

 ("disableSkillShellExecution", "Sicherheit", "settings.json",
  "Verbietet Skills, eingebettete Befehle auszufuehren.",
  "Stop skills and custom commands from running inline shell",
  "Ein Skill kann Befehle enthalten, die beim Laden ausgefuehrt werden. Das ist maechtig und "
  "entsprechend heikel bei fremden Skills.\n\n"
  "Diese Einstellung nimmt ihnen diese Faehigkeit; der uebrige Inhalt wirkt weiter."),

 ("emojiCompletionEnabled", "Darstellung", "settings.json",
  "Schaltet die Emoji-Vorschlaege in der Eingabe ab.",
  "Turn off :shortcode: emoji suggestions and replacement in the prompt input",
  "Tippst du einen Doppelpunkt und ein Stichwort, schlaegt Claude Code passende Emoji vor und "
  "ersetzt den Text.\n\n"
  "Wer haeufig Doppelpunkte tippt — in Pfaden, in Code —, empfindet das als Stoerung und "
  "schaltet es ab."),

 ("enableAllProjectMcpServers", "Integrationen", "settings.json",
  "Nimmt alle Dienste aus Projektdateien ohne Rueckfrage an.",
  "Approve every server in project .mcp.json files without a prompt",
  "Spart die Bestaetigung bei jedem Projekt. Der Preis ist, dass ein fremdes Projekt seine "
  "Dienste ungefragt starten kann.\n\n"
  "In eigenen Projekten bequem, bei fremdem Code besser nicht."),

 ("enabledMcpjsonServers", "Integrationen", "settings.json",
  "Nimmt einzelne Dienste aus der Projektdatei ohne Rueckfrage an.",
  "Approve specific servers from a project's .mcp.json",
  "Die feinere Variante zum vorigen Punkt: Statt alles pauschal anzunehmen, benennst du die "
  "Dienste einzeln.\n\n"
  "Das ist der empfohlene Weg — bequem und trotzdem bewusst."),

 ("enableWorkflows", "Automatisierung", "settings.json",
  "Schaltet die dynamischen Arbeitsablaeufe fuer dich ein oder aus.",
  "Turn dynamic workflows on or off against your plan's default",
  "Ein Arbeitsablauf steuert viele Agenten in einer festen Ordnung. Hier bestimmst du fuer "
  "dich selbst, ob das moeglich ist.\n\n"
  "Das Gegenstueck fuer eine ganze Organisation heisst `disableWorkflows`."),

 ("enforceAvailableModels", "Modell und Antworten", "settings.json",
  "Haelt die Voreinstellung im Modell-Waehler innerhalb der erlaubten Liste.",
  "Keep the /model Default choice inside your availableModels allowlist",
  "Ohne diese Einstellung koennte die Voreinstellung auf ein Modell zeigen, das gar nicht mehr "
  "erlaubt ist.\n\n"
  "Sie sorgt dafuer, dass die Auswahl und die Erlaubnisliste zusammenpassen."),

 ("externalEditorContext", "Darstellung", "~/.claude.json",
  "Zeigt beim Bearbeiten im Editor die letzte Antwort als Kommentar.",
  "Show Claude's last response as comments when you press Ctrl+G to edit",
  "Bearbeitest du deine Eingabe in einem richtigen Editor, steht die letzte Antwort als "
  "Kommentar mit darin.\n\n"
  "So musst du beim Formulieren nicht zwischen zwei Fenstern hin und her springen."),

 ("fastMode", "Modell und Antworten", "settings.json",
  "Schaltet den schnellen Modus dauerhaft ein.",
  "Turn fast mode on for sessions where it's available",
  "Der schnelle Modus gibt die Antwort desselben Modells zuegiger aus — er schaltet nicht auf "
  "ein schwaecheres Modell um.\n\n"
  "Steht nicht bei jedem Modell zur Verfuegung. Fuer eine einzelne Sitzung nimmst du `/fast`."),

 ("fastModePerSessionOptIn", "Modell und Antworten", "settings.json",
  "Verlangt, dass der schnelle Modus je Sitzung eingeschaltet wird.",
  "Require people to turn fast mode on each session",
  "Damit bleibt der schnelle Modus eine bewusste Entscheidung und wird nicht stillschweigend "
  "zur Gewohnheit.\n\n"
  "Sinnvoll dort, wo Gruendlichkeit vor Tempo geht."),

 ("feedbackDrafts", "Rueckmeldung", "settings.json",
  "Bestimmt, ob Rueckmeldungs-Entwuerfe zur Durchsicht gesammelt werden.",
  "Control whether Claude queues feedback drafts for you to review",
  "Faellt Claude etwas auf, das eine Rueckmeldung wert waere, legt es einen Entwurf an. "
  "Abgeschickt wird er erst nach deiner Zustimmung.\n\n"
  "Ohne diese Einstellung entstehen gar keine Entwuerfe."),

 ("feedbackSurveyRate", "Rueckmeldung", "settings.json",
  "Bestimmt, wie oft nach der Zufriedenheit gefragt wird.",
  "Change how often the session quality survey appears",
  "Gelegentlich erscheint eine kurze Frage, wie zufrieden du mit der Sitzung warst. Hier "
  "stellst du die Haeufigkeit ein.\n\n"
  "Auf null gesetzt, erscheint sie gar nicht mehr."),

 ("fileSuggestion", "Darstellung", "settings.json",
  "Laesst die Dateivorschlaege von einem eigenen Befehl liefern.",
  "Supply @ file autocomplete from your own command",
  "Tippst du den Klammeraffen, schlaegt Claude Code Dateien vor. Diese Liste kann stattdessen "
  "aus deinem eigenen Befehl kommen.\n\n"
  "Nuetzlich in sehr grossen Projekten, in denen eine eigene Suche schneller ist als die "
  "eingebaute."),

 ("footerLinksRegexes", "Darstellung", "settings.json",
  "Macht Kennungen in der Ausgabe zu anklickbaren Links.",
  "Make issue or review IDs in output into clickable links below the input box",
  "Erkennt Claude Code eine Ticketnummer oder eine Pruefkennung, kann es daraus einen Link "
  "bauen. Das Muster gibst du hier vor.\n\n"
  "Spart das Suchen im Ticketsystem — man klickt statt zu kopieren."),

 ("forceLoginGatewayUrl", "Anmeldung", "settings.json",
  "Legt die Adresse fest, ueber die die Anmeldung laeuft.",
  "Set the gateway URL the login screen connects to",
  "In Umgebungen mit eigenem Zugangspunkt laeuft die Anmeldung nicht ueber die uebliche "
  "Adresse. Hier steht die richtige.\n\n"
  "Ohne diesen Eintrag versucht Claude Code den Standardweg und scheitert."),

 ("forceLoginMethod", "Anmeldung", "settings.json",
  "Beschraenkt, womit man sich anmelden darf.",
  "Restrict login to claude.ai, Claude Console, or a cloud gateway",
  "Damit legt eine Organisation fest, ob die Anmeldung ueber ein Abo, ueber ein Konsolen-Konto "
  "oder ueber einen eigenen Zugangspunkt laeuft.\n\n"
  "Das verhindert, dass jemand versehentlich mit einem privaten Konto arbeitet."),

 ("forceLoginOrgUUID", "Anmeldung", "managed-settings.json",
  "Bindet die Anmeldung an eine bestimmte Organisation.",
  "Pin claude.ai logins to your organization",
  "Nur Konten der genannten Organisation werden angenommen. Ein privates Konto wird abgelehnt.\n\n"
  "Wirksam ist der Eintrag nur aus zentral verwalteten Einstellungen — sonst koennte man ihn "
  "selbst wieder herausnehmen."),

 ("forceRemoteSettingsRefresh", "Einstellungen", "settings.json",
  "Wartet beim Start, bis die zentralen Einstellungen frisch geholt sind.",
  "Block startup until server-managed settings are freshly fetched",
  "Normalerweise startet Claude Code sofort und holt die zentralen Vorgaben nebenher. Diese "
  "Einstellung dreht das um.\n\n"
  "Der Start dauert dadurch etwas laenger, dafuer gilt garantiert der aktuelle Stand."),

 ("gcpAuthRefresh", "Anmeldung", "settings.json",
  "Erneuert Google-Cloud-Zugangsdaten mit einem eigenen Befehl.",
  "Refresh Google Cloud credentials with your own command",
  "Das Gegenstueck zum Bedrock-Eintrag, nur fuer Google Cloud. Der hinterlegte Befehl liefert "
  "gueltige Zugangsdaten nach.\n\n"
  "Damit bleibt die Arbeit auch dann moeglich, wenn die Daten regelmaessig ablaufen."),

 ("httpHookAllowedEnvVars", "Automatisierung", "settings.json",
  "Grenzt ein, welche Umgebungsvariablen ein HTTP-Hook in Kopfzeilen setzen darf.",
  "Limit which env vars HTTP hooks can put in headers",
  "Ein Hook kann Werte aus Umgebungsvariablen in seine Anfrage schreiben. Darunter waeren auch "
  "Zugangsdaten.\n\n"
  "Diese Liste bestimmt, welche Variablen dafuer erlaubt sind — alles andere bleibt drinnen."),

 ("includeGitInstructions", "Git und Herkunft", "settings.json",
  "Nimmt die eingebauten Git-Anweisungen aus den Systemvorgaben.",
  "Remove the built-in commit and PR instructions from the system prompt",
  "Claude Code bringt Anweisungen mit, wie Commits und Pull Requests aussehen sollen. Wenn dein "
  "Projekt eigene Vorgaben hat, stoeren sie.\n\n"
  "Diese Einstellung nimmt sie heraus und spart zugleich Platz im Gedaechtnis."),

 ("keybindingFlavor", "Darstellung", "settings.json",
  "Aendert das Verhalten der Tastenkuerzel zum Loeschen.",
  "Make Ctrl+W delete back to the previous whitespace, as Bash does",
  "In Bash loescht Strg und W zurueck bis zum letzten Leerzeichen. Wer das gewohnt ist, "
  "stellt es hier so ein.\n\n"
  "Eine kleine Sache, die im Alltag ueberraschend viel ausmacht."),

 ("managedSourcesBehavior", "Einstellungen", "managed-settings.json",
  "Legt fest, ob zentrale Quellen zusammengefuehrt werden oder nur eine gilt.",
  "Compose every managed source you deploy instead of using the highest-priority one alone",
  "Gibt es mehrere zentral verteilte Einstellungsquellen, gilt normalerweise nur die "
  "wichtigste. Mit dieser Einstellung werden alle zusammengefuehrt.\n\n"
  "Das erlaubt es, Vorgaben aufzuteilen — etwa Sicherheit getrennt von Bequemlichkeit."),

 ("minimumVersion", "Aktualisierung", "settings.json",
  "Verhindert, dass die automatische Aktualisierung unter eine Version faellt.",
  "Keep auto-updates from installing anything below a version",
  "Damit landest du nicht versehentlich auf einer Fassung, in der eine benoetigte Funktion "
  "noch fehlt.\n\n"
  "Die strengeren Gegenstuecke `requiredMinimumVersion` und `requiredMaximumVersion` "
  "verweigern sogar den Start."),

 ("modelOverrides", "Modell und Antworten", "settings.json",
  "Bildet Modellnamen auf die Kennungen deines Anbieters ab.",
  "Map model IDs to your provider's IDs, such as Bedrock ARNs",
  "Laeuft Claude ueber einen anderen Anbieter, heissen die Modelle dort anders. Diese Zuordnung "
  "uebersetzt zwischen beiden Welten.\n\n"
  "Damit funktionieren die gewohnten Kurznamen weiter, obwohl im Hintergrund eine ganz andere "
  "Kennung steht."),

 ("modelPicker", "Modell und Antworten", "settings.json",
  "Bestimmt, welche Modelle in der Auswahl stehen und wie sie heissen.",
  "Choose which models the /model picker lists, in your own order and with your own labels",
  "Du legst die Reihenfolge und die Beschriftungen selbst fest — etwa „Schnell“, „Gruendlich“, "
  "„Fuer schwere Faelle“ statt technischer Kennungen.\n\n"
  "Das macht die Auswahl fuer alle verstaendlicher, die sich die Modellnamen nicht merken wollen."),

 ("modelPricing", "Kosten", "settings.json",
  "Rechnet die Kosten mit den vereinbarten Preisen statt mit den Listenpreisen.",
  "Report spend at your organization's contracted rates instead of list price",
  "Hat eine Organisation eigene Preise vereinbart, waeren die angezeigten Kosten sonst falsch.\n\n"
  "Mit dieser Einstellung zeigen `/cost` und `/usage` die tatsaechlichen Betraege."),
]
