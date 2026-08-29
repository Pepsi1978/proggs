# -*- coding: utf-8 -*-
"""Weitere Config-Einstellungen, kompakt erklaert (n bis z) plus Sandbox-Unterpunkte."""

CONFIG_REST_2 = [
 ("otelHeadersHelper", "Auswertung", "settings.json",
  "Erzeugt wechselnde Kopfzeilen fuer die Telemetrie mit einem eigenen Befehl.",
  "Generate rotating OpenTelemetry headers with your own command",
  "Wer Messdaten an ein eigenes System schickt, braucht dafuer oft kurzlebige Zugangsdaten. "
  "Dieser Befehl liefert sie jeweils frisch.\n\n"
  "Damit steht kein fester Schluessel in einer Einstellungsdatei."),

 ("parentSettingsBehavior", "Einstellungen", "managed-settings.json",
  "Bestimmt, ob Einschraenkungen einer uebergeordneten Umgebung gelten.",
  "Apply or drop restrictions an SDK or IDE host passes when you deploy managed settings",
  "Startet Claude Code innerhalb einer anderen Anwendung, kann diese Einschraenkungen "
  "mitgeben. Hier legst du fest, ob sie beachtet oder verworfen werden.\n\n"
  "Relevant, wenn zwei Regelwerke aufeinandertreffen und klar sein muss, welches gilt."),

 ("permissionExplainerEnabled", "Berechtigungen", "~/.claude.json",
  "Zeigt bei einer Rueckfrage auf Wunsch, was der Befehl genau tut.",
  "Turn off the Ctrl+E command explanation on shell permission prompts",
  "Fragt Claude Code, ob ein Befehl laufen darf, kannst du dir mit Strg und E erklaeren lassen, "
  "was er bewirkt.\n\n"
  "Das ist die beste Hilfe gegen ein unbedachtes Ja bei einem Befehl, den man nicht versteht. "
  "Ich wuerde es anlassen."),

 ("permissions.ask", "Berechtigungen", "settings.json",
  "Erzwingt eine Rueckfrage bei bestimmten Werkzeugaufrufen.",
  "Always prompt before listed tool uses",
  "Die mittlere Stufe zwischen Erlauben und Verbieten: Es geht, aber nur mit deiner "
  "ausdruecklichen Zustimmung.\n\n"
  "Richtig fuer alles, was folgenreich, aber manchmal noetig ist — Auslieferungen, Loeschungen, "
  "Zugriffe auf Produktivsysteme."),

 ("permissions.disableBypassPermissionsMode", "Berechtigungen", "settings.json",
  "Verbietet den Modus ganz ohne Rueckfragen.",
  "Prevent anyone from entering bypassPermissions mode",
  "Im Modus ohne Rueckfragen tut Claude alles, ohne zu fragen. Auf einem Rechner mit Zugriff "
  "auf wichtige Systeme ist das ein zu grosses Risiko.\n\n"
  "Diese Einstellung schliesst den Modus vollstaendig aus — auch ueber die Kommandozeile."),

 ("plansDirectory", "Arbeitsweise", "settings.json",
  "Bestimmt, wo der Planungsmodus seine Plaene ablegt.",
  "Choose where plan mode writes plan files",
  "Plaene werden als Dateien gespeichert, damit du sie spaeter noch einmal ansehen kannst.\n\n"
  "Legst du den Ordner ins Projekt, koennen andere die Ueberlegungen mitlesen; legst du ihn "
  "nach aussen, bleiben sie privat."),

 ("pluginConfigs", "Erweiterungen", "settings.json",
  "Speichert die Antworten, die du einer Erweiterung bei der Einrichtung gegeben hast.",
  "Store the answers you gave a plugin's configuration dialog",
  "Manche Erweiterungen fragen bei der Installation nach Angaben. Diese landen hier und "
  "muessen nicht erneut eingegeben werden.\n\n"
  "Achte darauf, dass keine Zugangsdaten darin stehen — die Datei liegt unverschluesselt."),

 ("pluginSuggestionMarketplaces", "Erweiterungen", "settings.json",
  "Legt fest, welche Marktplaetze Vorschlaege machen duerfen.",
  "Choose which marketplaces can surface plugin install suggestions in /plugin",
  "Beim Oeffnen der Erweiterungsverwaltung erscheinen Vorschlaege. Woher die kommen duerfen, "
  "steht hier.\n\n"
  "Eine leere Liste bedeutet: keine Vorschlaege, nur was du selbst suchst."),

 ("pluginTrustMessage", "Erweiterungen", "settings.json",
  "Ergaenzt den Warnhinweis vor der Installation um eigenen Text.",
  "Add your own text to the plugin trust warning",
  "Vor dem Installieren erscheint ein Hinweis, dass eine Erweiterung Befehle ausfuehren kann. "
  "Hier kannst du eine eigene Anmerkung ergaenzen.\n\n"
  "Zum Beispiel den Hinweis, wo im Haus die Freigabe einzuholen ist."),

 ("policyHelper", "Einstellungen", "managed-settings.json",
  "Laesst ein eigenes Programm die zentralen Einstellungen beim Start berechnen.",
  "Run an executable that computes managed settings at startup",
  "Statt einer festen Datei liefert ein Programm die Vorgaben — abhaengig davon, wer gerade "
  "arbeitet, auf welchem Rechner und in welchem Netz.\n\n"
  "Unterpunkte legen Pfad, Zeitgrenze und Wiederholungsabstand fest."),

 ("prUrlTemplate", "Zusammenarbeit", "settings.json",
  "Lenkt Pull-Request-Links auf ein eigenes System statt auf GitHub.",
  "Point PR links at an internal code-review tool instead of github.com",
  "Wer intern ein eigenes Pruefwerkzeug benutzt, will die Links dorthin und nicht nach aussen.\n\n"
  "Die Vorlage bestimmt, wie aus einer Nummer eine vollstaendige Adresse wird."),

 ("processWrapper", "Sicherheit", "settings.json",
  "Startet Hintergrundprozesse ueber ein vorgeschaltetes Programm.",
  "Run Claude Code's background processes through a corporate launcher on macOS and Linux",
  "In verwalteten Umgebungen laufen Programme oft nicht direkt, sondern ueber einen Starter, "
  "der Rechte setzt und protokolliert.\n\n"
  "Diese Einstellung sorgt dafuer, dass auch die Hintergrundprozesse diesen Weg nehmen."),

 ("promptSuggestionEnabled", "Darstellung", "settings.json",
  "Blendet die blassen Eingabevorschlaege aus.",
  "Hide the grayed-out prompt suggestions in the input box",
  "Im leeren Eingabefeld stehen blasse Vorschlaege, was man tippen koennte.\n\n"
  "Fuer Neulinge hilfreich; wer weiss, was er will, empfindet sie als Unruhe."),

 ("respondToBashCommands", "Arbeitsumgebung", "settings.json",
  "Bestimmt, ob Claude nach einem Ausrufezeichen-Befehl etwas sagt.",
  "Stop Claude from responding after a ! shell command runs",
  "Tippst du einen Befehl mit Ausrufezeichen, laeuft er direkt. Ohne diese Einstellung "
  "kommentiert Claude anschliessend das Ergebnis.\n\n"
  "Wer die Kommandozeile nur als Kommandozeile nutzen will, schaltet den Kommentar ab."),

 ("showClearContextOnPlanAccept", "Arbeitsweise", "settings.json",
  "Bietet beim Annehmen eines Plans an, das Gedaechtnis zu leeren.",
  "Show a clear context option on the plan accept screen",
  "Beim Planen entsteht viel Zwischenmaterial, das fuer die Umsetzung nicht mehr gebraucht wird.\n\n"
  "Diese Einstellung bietet an, mit dem angenommenen Plan frisch zu starten — das macht die "
  "Umsetzung schneller und genauer."),

 ("showThinkingSummaries", "Modell und Antworten", "settings.json",
  "Zeigt Zusammenfassungen dessen, was Claude gedacht hat.",
  "See summaries of Claude's thinking instead of a collapsed stub",
  "Statt eines eingeklappten Hinweises siehst du eine Kurzfassung der Ueberlegungen.\n\n"
  "Hilfreich, um zu verstehen, warum ein bestimmter Weg gewaehlt wurde — und um frueh zu "
  "merken, wenn Claude in die falsche Richtung denkt."),

 ("showTurnDuration", "Darstellung", "settings.json",
  "Blendet die Dauer-Anzeige nach jeder Antwort aus.",
  "Hide the 'Cooked for' duration after each response",
  "Nach jeder Antwort steht, wie lange sie gedauert hat.\n\n"
  "Nuetzlich beim Einschaetzen von Kosten und Tempo, sonst nur eine Zeile mehr."),

 ("skillListingMaxDescChars", "Kontext und Gedaechtnis", "settings.json",
  "Begrenzt die Laenge jeder Skill-Beschreibung in der Auflistung.",
  "Cap each skill's description length in the skill listing",
  "Bei vielen Skills wird die Auflistung lang. Eine kuerzere Beschreibung je Skill bedeutet, "
  "dass mehr Skills hineinpassen.\n\n"
  "Zu kurz gekappt findet Claude allerdings nicht mehr den richtigen — die Beschreibung ist "
  "das, woran es erkennt, wann ein Skill passt."),

 ("skipAutoPermissionPrompt", "Berechtigungen", "settings.json",
  "Ueberspringt den einmaligen Hinweis beim ersten Wechsel in den Auto-Modus.",
  "Skip the one-time notice Claude Code shows when you first enter auto mode yourself",
  "Beim ersten selbst gewaehlten Wechsel in den Auto-Modus erscheint eine Erklaerung, was das "
  "bedeutet.\n\n"
  "Wer den Modus kennt, kann sie hier abstellen."),

 ("skipWebFetchPreflight", "Integrationen", "settings.json",
  "Ueberspringt die Vorabpruefung beim Abrufen einer Webseite.",
  "Skip the WebFetch hostname check when Anthropic is unreachable",
  "Vor dem Abrufen wird der Rechnername geprueft. Ist der Pruefdienst nicht erreichbar, "
  "scheitert sonst jeder Abruf.\n\n"
  "Diese Einstellung laesst den Abruf trotzdem zu — praktisch in abgeschotteten Netzen, aber "
  "eben ohne diese Vorabpruefung."),

 ("spellcheck", "Darstellung", "settings.json",
  "Unterstreicht Tippfehler in der Eingabe.",
  "Underline misspelled words in the prompt input with a spell checker you install",
  "Setzt eine installierte Rechtschreibpruefung voraus. Claude Code bringt keine mit.\n\n"
  "Hilfreich bei laengeren Beschreibungen; bei Code und Befehlen eher stoerend, weil dort "
  "fast alles als Fehler gilt."),

 ("spinnerTipsOverride", "Darstellung", "settings.json",
  "Ersetzt die Tipps waehrend der Arbeit durch eigene.",
  "Add your own tips to the spinner rotation, or replace the built-in tips",
  "Statt der allgemeinen Tipps koennen dort eure eigenen Hinweise stehen — etwa auf hausinterne "
  "Regeln oder Werkzeuge.\n\n"
  "Eine unaufdringliche Stelle, um Wissen im Team zu verbreiten."),

 ("spinnerVerbs", "Darstellung", "settings.json",
  "Aendert die Taetigkeitswoerter, die waehrend der Arbeit angezeigt werden.",
  "Add or replace the verbs shown while a turn runs",
  "Waehrend Claude arbeitet, steht dort ein wechselndes Wort. Das laesst sich ersetzen.\n\n"
  "Reine Kosmetik, die den Arbeitsplatz persoenlicher macht."),

 ("sshConfigs", "Integrationen", "settings.json",
  "Traegt SSH-Verbindungen in die Auswahlliste der Desktop-Anwendung ein.",
  "Add SSH connections to the Desktop environment dropdown",
  "Damit laesst sich eine Sitzung direkt auf einem anderen Rechner starten, ohne die "
  "Verbindungsdaten jedes Mal einzugeben.\n\n"
  "Praktisch bei Arbeit auf Servern oder in einer Testumgebung."),

 ("sshHostAllowlist", "Sicherheit", "settings.json",
  "Grenzt ein, welche Rechner ueber SSH erreichbar sind.",
  "Limit which hosts Desktop SSH sessions can reach",
  "Ohne Einschraenkung koennte eine Sitzung jeden erreichbaren Rechner ansprechen.\n\n"
  "Diese Liste macht daraus eine bewusste Auswahl — wichtig, wenn Produktivsysteme im selben "
  "Netz stehen."),

 ("strictKnownMarketplaces", "Erweiterungen", "settings.json",
  "Laesst nur die genannten Bezugsquellen fuer Erweiterungen zu.",
  "Allowlist the marketplace sources users can add and install from",
  "Die strengere Umkehrung von `blockedMarketplaces`: Was nicht in der Liste steht, geht nicht.\n\n"
  "Das ist der richtige Weg in einer Umgebung, in der jede Erweiterung geprueft sein muss."),

 ("strictPluginOnlyCustomization", "Erweiterungen", "settings.json",
  "Laesst Anpassungen nur noch aus Erweiterungen und zentralen Quellen zu.",
  "Block skills, agents, hooks, and MCP servers from user and project sources",
  "Eigene Skills, Agenten, Hooks und Dienste aus Nutzer- oder Projektordnern werden ignoriert. "
  "Es zaehlt nur, was ueber eine geprueft ausgelieferte Erweiterung kommt.\n\n"
  "Die Unterpunkte erlauben es, das je Art einzeln zu entscheiden — etwa nur Hooks zu sperren."),

 ("subagentPromptCacheTtl", "Kosten", "settings.json",
  "Wie lange der Zwischenspeicher fuer Unteragenten vorhaelt.",
  "Choose the prompt cache lifetime for subagents and other requests outside the main conversation",
  "Unteragenten und Hintergrundanfragen haben ihren eigenen Zwischenspeicher, getrennt vom "
  "Hauptgespraech.\n\n"
  "Wer viel mit Unteragenten arbeitet, spart hier spuerbar — jede wiederholte Anweisung wird "
  "guenstiger abgerechnet."),

 ("subagentStatusLine", "Darstellung", "settings.json",
  "Gestaltet die Zeilen in der Anzeige der Unteragenten selbst.",
  "Rewrite rows in the subagent task display with your own command",
  "Laufen mehrere Unteragenten, zeigt eine Liste ihren Zustand. Was dort steht, kann dein "
  "eigener Befehl bestimmen.\n\n"
  "Sinnvoll, wenn du je Agent etwas Bestimmtes sehen willst — den bearbeiteten Bereich etwa."),

 ("switchModelsOnFlag", "Modell und Antworten", "settings.json",
  "Bestimmt, was passiert, wenn eine Anfrage auffaellig eingestuft wird.",
  "Switch models automatically or pause when a safety classifier flags a request",
  "Wird eine Anfrage als heikel eingestuft, kann Claude Code entweder auf ein anderes Modell "
  "wechseln oder anhalten und dich fragen.\n\n"
  "Anhalten ist die durchsichtigere Wahl: Du erfaehrst, dass etwas aufgefallen ist."),

 ("syntaxHighlightingDisabled", "Darstellung", "settings.json",
  "Schaltet die Farbhervorhebung in Code und Unterschieden ab.",
  "Turn off syntax highlighting in diffs and code blocks",
  "Ohne Farben ist die Ausgabe schlichter und in manchen Terminals besser lesbar.\n\n"
  "Auch bei einem Bildschirmleser sinnvoll, wo Farben ohnehin nicht ankommen."),

 ("teammateMode", "Zusammenarbeit", "settings.json",
  "Bestimmt, wie Mitglieder eines Agenten-Teams angezeigt werden.",
  "Choose how agent team teammates display",
  "Arbeiten mehrere Agenten als Team, kann das entweder ausfuehrlich oder nur als Zustand "
  "dargestellt werden.\n\n"
  "Bei vielen Mitgliedern ist die knappe Darstellung deutlich uebersichtlicher."),

 ("terminalProgressBarEnabled", "Darstellung", "settings.json",
  "Blendet den Fortschrittsbalken des Terminals aus.",
  "Hide the terminal progress bar in terminals that support it",
  "Manche Terminals zeigen einen Fortschritt in der Titelleiste oder in der Taskleiste an.\n\n"
  "Wo das stoert oder falsch dargestellt wird, schaltest du es hier ab."),

 ("useLocalModelsOnly", "Modell und Antworten", "settings.json",
  "Laesst nur Modelle zu, die auf dem eigenen Rechner laufen.",
  "Restrict models to locally-running providers",
  "Damit verlaesst kein Text den eigenen Rechner. Das ist die strengste Form des Datenschutzes.\n\n"
  "Der Preis ist deutlich: Lokal laufende Modelle sind heute erheblich schwaecher als die "
  "grossen Modelle im Netz."),

 ("sandbox.excludedCommands", "Sicherheit", "settings.json",
  "Nennt Befehle, die immer ausserhalb der Abschottung laufen.",
  "Name commands that always run outside the sandbox",
  "Manche Werkzeuge brauchen mehr Zugriff, als die Abschottung erlaubt, und scheitern darin.\n\n"
  "Hier nimmst du sie gezielt aus. Halte die Liste kurz — jeder Eintrag ist eine Ausnahme vom "
  "Schutz."),

 ("sandbox.autoAllowBashIfSandboxed", "Sicherheit", "settings.json",
  "Laesst abgeschottete Befehle ohne Rueckfrage laufen.",
  "Run sandboxed commands without a permission prompt",
  "Das ist die angenehmste Kombination ueberhaupt: Weil der Befehl ohnehin eingesperrt ist, "
  "kann er kaum Schaden anrichten — die Rueckfrage bringt also wenig.\n\n"
  "Du bekommst deutlich weniger Unterbrechungen und behaeltst trotzdem den Schutz."),

 ("sandbox.failIfUnavailable", "Sicherheit", "settings.json",
  "Verweigert den Start, wenn die Abschottung nicht funktioniert.",
  "Refuse to start when the sandbox can't, instead of running unsandboxed",
  "Ohne diese Einstellung laeuft Claude Code einfach ungeschuetzt weiter, wenn die Abschottung "
  "auf dem System nicht startet. Das merkt man leicht nicht.\n\n"
  "Wer sich auf den Schutz verlaesst, sollte hier `true` setzen — sonst ist er still weg."),

 ("sandbox.network.allowedDomains", "Sicherheit", "settings.json",
  "Erlaubt bestimmten Adressen den Zugriff aus der Abschottung heraus.",
  "Pre-allow domains so sandboxed commands don't prompt for them",
  "Abgeschottete Befehle koennen normalerweise nicht ins Netz. Was hier steht, geht ohne "
  "Rueckfrage — typischerweise die eigenen Paketquellen.\n\n"
  "Das Gegenstueck `deniedDomains` sperrt einzelne Adressen auch innerhalb eines erlaubten "
  "Bereichs."),

 ("sandbox.network.strictAllowlist", "Sicherheit", "settings.json",
  "Lehnt alles ab, was nicht auf der Erlaubnisliste steht, statt zu fragen.",
  "Deny hosts outside the allowlist instead of prompting",
  "Ohne diese Einstellung erscheint bei einer unbekannten Adresse eine Rueckfrage. Mit ihr "
  "wird sie schlicht abgelehnt.\n\n"
  "Das ist die richtige Wahl fuer unbeaufsichtigte Laeufe, in denen niemand antworten kann."),

 ("sandbox.filesystem.denyRead", "Sicherheit", "settings.json",
  "Sperrt bestimmte Pfade fuers Lesen aus der Abschottung heraus.",
  "Block sandboxed commands from reading specific paths",
  "Der richtige Ort fuer alles, was Zugangsdaten enthaelt: Schluesselordner, Konfigurationen "
  "mit Passwoertern, private Schluessel.\n\n"
  "Mit `allowRead` laesst sich innerhalb eines gesperrten Bereichs eine einzelne Ausnahme "
  "wieder oeffnen."),

 ("sandbox.filesystem.denyWrite", "Sicherheit", "settings.json",
  "Sperrt bestimmte Pfade fuers Schreiben aus der Abschottung heraus.",
  "Block sandboxed commands from writing to specific paths",
  "Damit bleibt geschuetzt, was nicht veraendert werden darf — Systemordner, fremde Projekte, "
  "Sicherungen.\n\n"
  "`allowWrite` ergaenzt umgekehrt Pfade, in die geschrieben werden darf."),

 ("sandbox.credentials", "Sicherheit", "settings.json",
  "Verbirgt oder verschleiert Zugangsdaten innerhalb der Abschottung.",
  "Hide or mask credential files and variables inside the sandbox",
  "Selbst wenn ein Befehl ausgesperrt ist, koennte er Zugangsdaten lesen und weiterschicken. "
  "Diese Gruppe verhindert das.\n\n"
  "Verschleiern statt sperren hat einen Vorteil: Das Werkzeug laeuft weiter, bekommt aber nur "
  "einen Platzhalter zu sehen."),
]
