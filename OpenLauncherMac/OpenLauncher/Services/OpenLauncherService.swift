import Foundation

/// Startet OpenCode bzw. Claude Code mit einem gewaehlten Modell ueber einen gewaehlten Provider.
/// 1:1-Port von Services/OpenLauncherService.cs.
///
/// Vorgehen (unveraendert gegenueber Windows):
///   1. Globale opencode.jsonc/.json (~/.config/opencode/) BOM-frei einlesen und als JSONNode
///      verarbeiten (JSONC: Kommentare + nachgestellte Kommata erlaubt).
///   2. provider.openrouter.models.<slug>.options.provider.order = [gewaehlter Provider]
///      + allow_fallbacks:false setzen -> OpenCode laeuft exakt ueber den gewaehlten Provider.
///   3. Datei atomar + BOM-frei zurueckschreiben (.bak-Backup).
///   4. `opencode -m <provider>/<slug>` in einem neuen Terminal-Tab starten.
///
/// Plattform-Unterschiede zu Windows:
///   - Startskript ist ein zsh-Skript statt eines PowerShell-Skripts.
///   - Terminal ist Terminal.app (das Standard-Terminal von macOS) statt Windows Terminal.
///   - Prozess-Prioritaet: `renice` statt ProcessPriorityClass.AboveNormal.
final class OpenLauncherService {
    private static let gpt55Slug = "gpt-5.5"
    private static let gpt55FastSlug = "gpt-5.5-fast"
    private static let gpt56SolSlug = "gpt-5.6-sol"
    private static let gpt56SolFastSlug = "gpt-5.6-sol-fast"
    private static let gpt56TerraSlug = "gpt-5.6-terra"
    private static let gpt56TerraFastSlug = "gpt-5.6-terra-fast"
    private static let gpt56LunaSlug = "gpt-5.6-luna"
    private static let gpt56LunaFastSlug = "gpt-5.6-luna-fast"

    /// Kontextlaenge, mit der ein noch nicht geladenes LM-Studio-Modell geladen wird.
    private static let defaultLmStudioContext = LmStudioService.preferredContext

    /// Hebt die Prioritaet der Sitzung an. Windows setzt ProcessPriorityClass.AboveNormal; auf macOS
    /// ist das Gegenstueck ein negativer nice-Wert. Ohne root sind nur Werte >= 0 erlaubt, deshalb
    /// wird der Fehlschlag bewusst geschluckt (`|| true`) - die Sitzung laeuft dann mit
    /// Normalprioritaet weiter, statt am Start zu scheitern.
    private static let processPriorityScript = """
    renice -n -5 -p $$ >/dev/null 2>&1 || true
    """

    /// Entfernt die Umgebung eines uebergeordneten KI-Agenten, bevor eine Sitzung startet.
    ///
    /// Der Launcher erbt die Umgebung des Prozesses, der ihn gestartet hat; wurde er aus einer
    /// Claude-Sitzung heraus gestartet (typisch: der Agent baut den Launcher und startet ihn neu),
    /// reicht er sie an jedes Terminal weiter, das er oeffnet. Folgen in der neuen Sitzung:
    ///   NO_COLOR=1                -> alle Farben aus (weisses statt oranges Logo, blasse Syntax)
    ///   CLAUDE_CODE_CHILD_SESSION -> Sitzung startet als Kind-Sitzung: kein Transcript, dadurch
    ///                                kein ctx-Wert in der Statusline
    /// CLAUDE_CONFIG_DIR bleibt bewusst stehen - das Profil setzt es selbst.
    /// Das CLAUDE*-Muster faengt auch Marker ab, die kuenftige Claude-Versionen neu einfuehren.
    private static let inheritedAgentEnvScrubScript = """
    for staleName in NO_COLOR FORCE_COLOR CLICOLOR CLICOLOR_FORCE AI_AGENT GIT_TERMINAL_PROMPT; do
        unset "$staleName" 2>/dev/null || true
    done
    for staleClaude in $(env | sed -n 's/^\\(CLAUDE[A-Za-z0-9_]*\\)=.*/\\1/p'); do
        if [ "$staleClaude" != "CLAUDE_CONFIG_DIR" ]; then
            unset "$staleClaude" 2>/dev/null || true
        fi
    done
    """

    /// Laedt die Login-Umgebung nach, damit alle installierten Werkzeuge erreichbar sind. Das
    /// Windows-Gegenstueck liest Machine- und User-PATH aus der Registry; auf macOS uebernehmen das
    /// path_helper und die Shell-Profile. Prozesslokale Eintraege bleiben erhalten.
    private static let persistentPathRefreshScript = """
    if [ -x /usr/libexec/path_helper ]; then
        eval "$(/usr/libexec/path_helper -s)"
    fi
    for profileFile in "$HOME/.zprofile" "$HOME/.zshrc"; do
        [ -f "$profileFile" ] && . "$profileFile" >/dev/null 2>&1
    done
    for extraDir in "$HOME/.local/bin" "$HOME/.bun/bin" "$HOME/.opencode/bin" "$HOME/.cargo/bin" /opt/homebrew/bin /usr/local/bin; do
        case ":$PATH:" in
            *":$extraDir:"*) ;;
            *) [ -d "$extraDir" ] && PATH="$extraDir:$PATH" ;;
        esac
    done
    export PATH
    """

    private static var configDir: String { Paths.openCodeConfigDir }

    // ===================== Provider konfigurieren =====================

    /// Schreibt bei OpenRouter die gewaehlte Provider-Order ohne Fallback. Native Direktmodelle
    /// werden ueber ihre unveraenderte OpenCode-Modell-ID gestartet. Gibt den String fuer
    /// `opencode -m` zurueck.
    func configureProvider(model: ModelEntry, chosen: ProviderEntry,
                           allProviders: [ProviderEntry], thinkingLevel rawLevel: String?) throws -> String {
        let modelString = model.modelString
        let thinkingLevel = Self.normalizeThinkingLevel(rawLevel)

        if model.providerId.caseInsensitiveCompare(LmStudioService.providerId) == .orderedSame {
            // Lokales Modell: OpenCode kennt LM Studio nicht von Haus aus. Der Provider-Block
            // (OpenAI-kompatibler Endpunkt auf localhost:1234) wird deshalb hier in die globale
            // Konfig geschrieben, und der LM-Studio-Server wird gestartet. Das Modell selbst laedt
            // LM Studio per JIT-Loading bei der ersten Anfrage.
            let root = Self.readConfig()
            let patched = Self.patchLmStudioModel(root, slug: model.slug, displayName: model.displayName)
            try Self.writeConfig(patched)
            let serverOk = LmStudioService.ensureServerRunning()
            // Das Modell selbst wird NICHT hier geladen: ein 26B-Modell braucht dafuer Minuten und
            // wuerde die Oberflaeche einfrieren, bevor das Terminal ueberhaupt aufgeht. Das Laden
            // mit ausreichend Kontext passiert sichtbar im Startskript des Terminals.
            Logger.shared.info("OpenLauncherService", "configureProvider", "LM-Studio-Modell gesetzt",
                               ["model": modelString, "serverOk": serverOk])
            return modelString
        }

        if model.providerId.caseInsensitiveCompare("openrouter") != .orderedSame {
            let usesPriorityServiceTier = Self.usesPriorityServiceTier(providerId: model.providerId, slug: model.slug)
            if Self.isGpt56Model(providerId: model.providerId, slug: model.slug) {
                // GPT-5.6 einschliesslich der drei Fast-Varianten ist im OpenCode-Katalog nativ
                // definiert. Eigene model.id/options-Overrides koennen von dieser Definition
                // abweichen; deshalb nur die exakte native Modell-ID an -m weiterreichen.
                let root = Self.readConfig()
                if Self.removeLegacyGpt56Overrides(root) { try Self.writeConfig(root) }
                if let thinkingLevel { Self.patchModelVariantState(modelString: modelString, thinkingLevel: thinkingLevel) }
                Logger.shared.info("OpenLauncherService", "configureProvider", "Natives OpenCode-GPT-5.6-Modell gesetzt", [
                    "model": modelString,
                    "thinkingLevel": thinkingLevel ?? "",
                    "serviceTier": usesPriorityServiceTier ? "priority" : "standard"
                ])
                return modelString
            }

            if thinkingLevel != nil || usesPriorityServiceTier {
                let root = Self.readConfig()
                let patched = Self.patchDirectModel(root, providerId: model.providerId, slug: model.slug,
                                                    displayName: model.displayName)
                try Self.writeConfig(patched)
                if let thinkingLevel { Self.patchModelVariantState(modelString: modelString, thinkingLevel: thinkingLevel) }
                Logger.shared.info("OpenLauncherService", "configureProvider", "Direktmodell-Variante gesetzt", [
                    "model": modelString,
                    "thinkingLevel": thinkingLevel ?? "",
                    "serviceTier": usesPriorityServiceTier ? "priority" : ""
                ])
            }
            Logger.shared.info("OpenLauncherService", "configureProvider", "Direktmodell ohne OpenRouter-Routing: \(modelString)")
            return modelString
        }

        do {
            let root = Self.readConfig()
            let patched = Self.patchProvider(root, slug: model.slug, modelDisplayName: model.displayName,
                                             chosen: chosen, thinkingLevel: thinkingLevel)
            try Self.writeConfig(patched)
            Self.patchModelVariantState(modelString: modelString, thinkingLevel: thinkingLevel)
            Logger.shared.info("OpenLauncherService", "configureProvider",
                               "opencode-Konfig gepatched: \(model.slug) via \(chosen.providerName)",
                               ["order": [chosen.providerSlug], "thinkingLevel": thinkingLevel ?? ""])
        } catch {
            Logger.shared.error("OpenLauncherService", "configureProvider", error.localizedDescription,
                                ["slug": model.slug, "providerId": model.providerId, "provider": chosen.providerName])
            throw error
        }

        return modelString
    }

    // ===================== Starten =====================

    /// Startet opencode in einem neuen Terminal-Tab.
    func launch(modelString: String, workDir: String, thinkingLevel rawLevel: String?,
                profileConfigPath: String, workMode: String) throws {
        let thinkingLevel = Self.normalizeThinkingLevel(rawLevel)
        do {
            Paths.ensureDirectory(workDir)
            let tabColor = TerminalLauncher.pickOpenCodeColor()
            let title = thinkingLevel == nil ? "OpenCode-\(tabColor.name)" : "OpenCode-\(tabColor.name)-\(thinkingLevel!)"
            let script = try Self.buildOpenCodeStartScript(modelString: modelString, workDir: workDir,
                                                           thinkingLevel: thinkingLevel,
                                                           profileConfigPath: profileConfigPath,
                                                           workMode: workMode, tabColor: tabColor, title: title)
            let terminal = TerminalLauncher.openScript(script, workDir: workDir)
            Logger.shared.info("OpenLauncherService", "launch", "opencode gestartet (\(terminal))",
                               ["modelString": modelString, "workDir": workDir,
                                "thinkingLevel": thinkingLevel ?? "", "tabColor": tabColor.name])
        } catch {
            Logger.shared.error("OpenLauncherService", "launch", error.localizedDescription,
                                ["modelString": modelString, "workDir": workDir])
            throw error
        }
    }

    /// Startet Claude Code mit gewaehltem Modell und Effort.
    /// - Parameter claudeConfigDir: Config-Ordner des gewaehlten Profils
    ///   (Profiles/ClaudeCodeMac/<id>) -> CLAUDE_CONFIG_DIR. Standard/Strikt tragen versionierte
    ///   skills/rules/agents/commands, Minimal ist regelfrei (Skills per Symlink).
    func launchClaudeCode(modelId: String, workDir: String, effortLevel rawEffort: String?,
                          claudeConfigDir: String?) throws {
        let effortLevel = Self.normalizeThinkingLevel(rawEffort)
        do {
            Paths.ensureDirectory(workDir)
            let tabColor = TerminalLauncher.pickClaudeColor()
            let title = Self.buildClaudeCodeTitle(colorName: tabColor.name, effortLevel: effortLevel)
            let script = try Self.buildClaudeCodeStartScript(modelId: modelId, workDir: workDir,
                                                             effortLevel: effortLevel, colorName: tabColor.name,
                                                             claudeConfigDir: claudeConfigDir,
                                                             tabColor: tabColor, title: title)
            let terminal = TerminalLauncher.openScript(script, workDir: workDir)
            Logger.shared.info("OpenLauncherService", "launchClaudeCode", "Claude Code gestartet (\(terminal))",
                               ["modelId": modelId, "workDir": workDir,
                                "effortLevel": effortLevel ?? "", "tabColor": tabColor.name])
        } catch {
            Logger.shared.error("OpenLauncherService", "launchClaudeCode", error.localizedDescription,
                                ["modelId": modelId, "workDir": workDir])
            throw error
        }
    }

    // ===================== Startskripte =====================

    static func buildClaudeCodeStartScript(modelId: String, workDir: String, effortLevel: String?,
                                                   colorName: String, claudeConfigDir: String?,
                                                   tabColor: TerminalTabColor, title: String) throws -> String {
        let tempScript = (Paths.tempDir as NSString)
            .appendingPathComponent("openlauncher-claude-code-\(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()).sh")
        let tempSettings = try buildClaudeCodeSessionSettings(modelId: modelId, effortLevel: effortLevel)

        let script = """
        #!/bin/zsh
        # Von OpenLauncher (macOS) erzeugtes Startskript. Loescht sich am Ende selbst.
        SELF=\(Shell.singleQuoted(tempScript))
        SETTINGS=\(Shell.singleQuoted(tempSettings))
        cleanup() { rm -f "$SELF" "$SETTINGS" 2>/dev/null || true; }
        trap cleanup EXIT INT TERM

        \(processPriorityScript)
        \(tabColor.tabColorScript)
        printf '\\033]0;\(title)\\a'

        cd \(Shell.singleQuoted(workDir)) || exit 1

        # Login-Umgebung laden, damit alle installierten Werkzeuge erreichbar sind.
        \(persistentPathRefreshScript)

        # Geerbte Agenten-Umgebung entfernen -- vor dem Profil, damit das Profil gesetzte Werte behaelt.
        \(inheritedAgentEnvScrubScript)

        # Jedes Profil hat seinen eigenen Config-Ordner (CLAUDE_CONFIG_DIR) im Repo. Standard/Strikt
        # tragen versionierte skills/rules/agents/commands; Minimal ist regelfrei (Skills per Symlink).
        CLAUDE_PROFILE_DIR=\(Shell.singleQuoted(claudeConfigDir ?? ""))
        if [ -n "$CLAUDE_PROFILE_DIR" ]; then
            mkdir -p "$CLAUDE_PROFILE_DIR"
            export CLAUDE_CONFIG_DIR="$CLAUDE_PROFILE_DIR"
        fi

        # Anmeldung in dieses Profil spiegeln, BEVOR Claude startet. Claude Code legt den Login je
        # CLAUDE_CONFIG_DIR getrennt im Schluesselbund ab -- ohne diesen Abgleich verlangt jedes
        # Profil beim ersten Start (und nach jedem Profilwechsel) eine neue Anmeldung. Der Abgleich
        # schreibt nur dort, wo gar kein oder ein voellig abgelaufener Login liegt.
        LOGIN_SYNC="$HOME/proggs/OpenLauncher/Profiles/hooks/claude-login-sync.py"
        if [ -f "$LOGIN_SYNC" ]; then
            python3 "$LOGIN_SYNC" >/dev/null 2>&1 || true
        fi

        claudeArgs=(--dangerously-skip-permissions --settings "$SETTINGS" --model \(Shell.singleQuoted(modelId)))
        EFFORT=\(Shell.singleQuoted(effortLevel ?? ""))
        if [ -n "$EFFORT" ]; then
            # --effort NUR als Startwert; NIEMALS CLAUDE_CODE_EFFORT_LEVEL setzen. Die Umgebungsvariable
            # ueberstimmt den /effort-Befehl zur Laufzeit, sodass jede Aenderung still zurueckspringt.
            claudeArgs+=(--effort "$EFFORT")
        fi
        COLORNAME=\(Shell.singleQuoted(colorName))
        if [ -n "$COLORNAME" ]; then
            claudeArgs+=("/color $COLORNAME")
        else
            claudeArgs+=("/color")
        fi

        claude "${claudeArgs[@]}"

        # Tab offen lassen (Gegenstueck zu -NoExit unter Windows) und Temp-Dateien vorher raeumen.
        cleanup
        trap - EXIT INT TERM
        exec /bin/zsh -l
        """
        guard Paths.writeAtomic(script, to: tempScript) else {
            throw LauncherError.message("Startskript konnte nicht geschrieben werden: \(tempScript)")
        }
        return tempScript
    }

    private static func buildClaudeCodeSessionSettings(modelId: String, effortLevel: String?) throws -> String {
        let tempSettings = (Paths.tempDir as NSString)
            .appendingPathComponent("openlauncher-claude-settings-\(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()).json")
        let root = JSONNode.object()
        root["model"] = .string(modelId)
        if let effortLevel, isPersistableClaudeEffort(effortLevel) {
            root["effortLevel"] = .string(effortLevel)
        }
        guard Paths.writeAtomic(root.serialized(), to: tempSettings) else {
            throw LauncherError.message("Sitzungs-Settings konnten nicht geschrieben werden: \(tempSettings)")
        }
        return tempSettings
    }

    private static func isPersistableClaudeEffort(_ effortLevel: String?) -> Bool {
        ["low", "medium", "high", "xhigh"].contains(effortLevel ?? "")
    }

    private static func buildClaudeCodeTitle(colorName: String, effortLevel: String?) -> String {
        effortLevel == nil ? "Claude-\(colorName)" : "Claude-\(colorName)-\(effortLevel!)"
    }

    static func buildOpenCodeStartScript(modelString: String, workDir: String, thinkingLevel: String?,
                                                  profileConfigPath: String, workMode: String,
                                                  tabColor: TerminalTabColor, title: String) throws -> String {
        let executable = resolveOpenCodeExecutable()
        // configureProvider legt die gewaehlte Startstufe bereits im nativen Varianten-Zustand von
        // OpenCode ab. Ein zweiter, prozesslokaler --variant-Override wuerde mit Ctrl+T konkurrieren,
        // das der alleinige Besitzer der Varianten-Wechsel innerhalb einer Sitzung bleiben muss.

        // Fuer ALLE OpenCode-Profile: nur den CLAUDE.md-Prompt-Fallback abschalten, damit OpenCode
        // ausschliesslich die Profil-AGENTS.md als Regelquelle nutzt. NICHT der komplette Schalter
        // OPENCODE_DISABLE_CLAUDE_CODE -- der wuerde auch die .claude-Skills (~/.claude/skills) und
        // die uebrige .claude-Kompatibilitaet deaktivieren. Mit _PROMPT bleiben Skills UND MCP aktiv.
        let separator = modelString.firstIndex(of: "/")
        let launcherProvider = separator.map { String(modelString[modelString.startIndex..<$0]) } ?? ""
        let launcherSlug = separator.map { String(modelString[modelString.index(after: $0)...]) } ?? modelString
        let launcherServiceTier = usesPriorityServiceTier(providerId: launcherProvider, slug: launcherSlug) ? "priority" : "standard"

        let tempScript = (Paths.tempDir as NSString)
            .appendingPathComponent("openlauncher-opencode-run-\(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()).sh")

        // stderr MUSS in eine prozesseigene Datei umgeleitet werden: unbehandelte Bun/Effect-Fehler
        // im TUI-Hauptthread schreiben rohe Stacktraces auf stderr - dasselbe TTY, auf dem die TUI
        // rendert. Fremde Bytes zerstoeren das diff-gerenderte Bild bis zum naechsten Vollaufbau
        // (bugs/opencode/opencode-cli.md #14a). stdout/stdin bleiben auf der Konsole, damit TUI,
        // Maus und Eingaben funktionieren. Leere Protokolle werden geloescht, nicht leere gemeldet.
        let script = """
        #!/bin/zsh
        # Von OpenLauncher (macOS) erzeugtes Startskript. Loescht sich am Ende selbst.
        SELF=\(Shell.singleQuoted(tempScript))
        cleanup() { rm -f "$SELF" 2>/dev/null || true; }
        trap cleanup EXIT INT TERM

        \(processPriorityScript)
        \(tabColor.tabColorScript)
        printf '\\033]0;\(title)\\a'

        cd \(Shell.singleQuoted(workDir)) || exit 1

        # Login-Umgebung laden, damit alle installierten Werkzeuge erreichbar sind.
        \(persistentPathRefreshScript)
        # Geerbte Agenten-Umgebung entfernen -- sonst startet die TUI ohne Farben (NO_COLOR).
        \(inheritedAgentEnvScrubScript)
        \(buildNvidiaKeyScript(modelString: modelString))
        \(buildLmStudioPreloadScript(modelString: modelString))
        export OPENCODE_CONFIG=\(Shell.singleQuoted(profileConfigPath))
        export OPENLAUNCHER_MODEL=\(Shell.singleQuoted(modelString))
        export OPENLAUNCHER_SOURCE='OpenLauncher'
        export OPENLAUNCHER_SERVICE_TIER=\(Shell.singleQuoted(launcherServiceTier))
        export OPENLAUNCHER_WORK_MODE=\(Shell.singleQuoted(workMode))
        export OPENCODE_DISABLE_CLAUDE_CODE_PROMPT='1'

        stderrDir="$HOME/.local/share/opencode/log/stderr"
        mkdir -p "$stderrDir" 2>/dev/null || stderrDir="$TMPDIR"
        if [ -d "$stderrDir" ]; then
            find "$stderrDir" -maxdepth 1 -name 'opencode-stderr-*.log' -size 0 -delete 2>/dev/null || true
            oldNonEmpty=$(find "$stderrDir" -maxdepth 1 -name 'opencode-stderr-*.log' -size +0 2>/dev/null | wc -l | tr -d ' ')
            if [ "$oldNonEmpty" -gt 0 ] 2>/dev/null; then
                newest=$(ls -t "$stderrDir"/opencode-stderr-*.log 2>/dev/null | head -1)
                printf '\\033[33m[stderr-Waechter] %s Fehlerprotokoll(e) frueherer OpenCode-Laeufe - neuestes: %s\\033[0m\\n' "$oldNonEmpty" "$newest"
                head -2 "$newest" 2>/dev/null | sed 's/^/  /'
            fi
        else
            stderrDir="$TMPDIR"
        fi
        stderrLog="$stderrDir/opencode-stderr-$(date +%Y%m%d-%H%M%S)-$$.log"

        \(Shell.singleQuoted(executable)) -m \(Shell.singleQuoted(modelString)) 2>"$stderrLog"

        if [ -f "$stderrLog" ]; then
            if [ -s "$stderrLog" ]; then
                printf '\\033[33m[stderr-Waechter] Dieser Lauf hatte Fehlerausgaben: %s\\033[0m\\n' "$stderrLog"
            else
                rm -f "$stderrLog" 2>/dev/null || true
            fi
        fi

        # Tab offen lassen (Gegenstueck zu -NoExit unter Windows).
        cleanup
        trap - EXIT INT TERM
        exec /bin/zsh -l
        """
        guard Paths.writeAtomic(script, to: tempScript) else {
            throw LauncherError.message("Startskript konnte nicht geschrieben werden: \(tempScript)")
        }
        return tempScript
    }

    /// Sorgt im Terminal-Fenster dafuer, dass ein lokales LM-Studio-Modell mit agent-tauglichem
    /// Kontext geladen ist, bevor OpenCode startet. Sichtbar statt im UI-Thread: das Laden eines
    /// grossen Modells dauert Minuten. Fuer alle anderen Provider ist das Ergebnis leer.
    ///
    /// Ein bereits geladenes Modell wird NUR dann uebernommen, wenn sein Kontext fuer OpenCode
    /// reicht. Hat der Benutzer es in LM Studio von Hand gestartet (Vorgabe dort oft 4096 Tokens),
    /// wird es entladen und mit grossem Kontext neu geladen - sonst bricht OpenCode sofort mit
    /// exceed_context_size_error ab, weil allein der Systemprompt rund 22000 Tokens braucht.
    private static func buildLmStudioPreloadScript(modelString: String) -> String {
        let prefix = "\(LmStudioService.providerId)/"
        guard modelString.lowercased().hasPrefix(prefix) else { return "" }
        let modelId = String(modelString.dropFirst(prefix.count))

        return """
        # Die rund 70 externen Skills aus ~/.claude/skills werden mit ihrer kompletten Beschreibung in
        # JEDE Anfrage eingebettet und kosten gemessene ~14000 Token. Bei Cloud-Modellen faellt das kaum
        # auf: dort liegt der Block im Prompt-Cache und das Fenster ist 200k oder groesser. Ein lokales
        # Modell hat weder Cache noch Platz. Prozess-lokale Variable, sie gilt nur fuer dieses Terminal.
        export OPENCODE_DISABLE_EXTERNAL_SKILLS='1'
        LMS="$HOME/.lmstudio/bin/lms"
        if [ -x "$LMS" ]; then
            LMSMODEL=\(Shell.singleQuoted(modelId))
            LMSMINCTX=\(LmStudioService.minimumAgentContext)
            LMSWANTCTX=\(LmStudioService.preferredContext)
            LMSPY="${TMPDIR:-/tmp}/openlauncher-lmstudio-$$.py"
            # Hilfsskript einmal ablegen: liest den Zustand aus der lms-CLI und traegt den echten
            # Kontext in die opencode-Konfig ein. Als Datei statt als python3 -c, damit die
            # Anfuehrungszeichen nicht zwischen zsh, Swift und Python zerrieben werden.
            cat > "$LMSPY" <<'PYEOF'
        import json, sys

        BACKSLASH = chr(92)
        NEWLINE = chr(10)
        BOM = chr(65279)


        def strip_jsonc(raw):
            \"\"\"Kommentare und nachgestellte Kommata entfernen - aber NUR ausserhalb von
            Zeichenketten. Eine naive Regex auf '//' zerschlaegt sonst jede URL in der Konfig
            (etwa den $schema-Eintrag) und der Parse scheitert immer.\"\"\"
            out = []
            i = 0
            n = len(raw)
            in_str = False
            while i < n:
                c = raw[i]
                if in_str:
                    out.append(c)
                    if c == BACKSLASH and i + 1 < n:
                        out.append(raw[i + 1])
                        i += 2
                        continue
                    if c == '"':
                        in_str = False
                    i += 1
                    continue
                if c == '"':
                    in_str = True
                    out.append(c)
                    i += 1
                    continue
                if c == '/' and i + 1 < n and raw[i + 1] == '/':
                    while i < n and raw[i] != NEWLINE:
                        i += 1
                    continue
                if c == '/' and i + 1 < n and raw[i + 1] == '*':
                    i += 2
                    while i + 1 < n and not (raw[i] == '*' and raw[i + 1] == '/'):
                        i += 1
                    i += 2
                    continue
                out.append(c)
                i += 1

            text = ''.join(out)
            cleaned = []
            j = 0
            m = len(text)
            in_str = False
            while j < m:
                ch = text[j]
                if in_str:
                    cleaned.append(ch)
                    if ch == BACKSLASH and j + 1 < m:
                        cleaned.append(text[j + 1])
                        j += 2
                        continue
                    if ch == '"':
                        in_str = False
                    j += 1
                    continue
                if ch == '"':
                    in_str = True
                    cleaned.append(ch)
                    j += 1
                    continue
                if ch == ',':
                    k = j + 1
                    while k < m and text[k].isspace():
                        k += 1
                    if k < m and (text[k] == '}' or text[k] == ']'):
                        j += 1
                        continue
                cleaned.append(ch)
                j += 1
            return ''.join(cleaned)


        def entries():
            try:
                data = json.load(sys.stdin)
            except Exception:
                return []
            return data if isinstance(data, list) else []


        def find(items, target):
            low = target.lower()
            for e in items:
                for key in ('identifier', 'modelKey', 'indexedModelIdentifier', 'path'):
                    value = e.get(key)
                    if isinstance(value, str) and value.lower() == low:
                        return e
            return None


        def number(entry, key):
            try:
                return int(entry.get(key) or 0)
            except Exception:
                return 0


        mode = sys.argv[1]

        if mode == 'ctx':
            # stdin: Ausgabe von "lms ps --json" -> "<geladen> <maximum>"
            e = find(entries(), sys.argv[2]) or {}
            print('%d %d' % (number(e, 'contextLength'), number(e, 'maxContextLength')))
        elif mode == 'max':
            # stdin: Ausgabe von "lms ls --json" -> Maximalkontext des Modells
            e = find(entries(), sys.argv[2]) or {}
            print('%d' % number(e, 'maxContextLength'))
        elif mode == 'patch':
            path, model, ctx = sys.argv[2], sys.argv[3], int(sys.argv[4])
            raw = open(path, encoding='utf-8').read()
            if raw[:1] == BOM:
                raw = raw[1:]
            try:
                cfg = json.loads(raw)
            except Exception:
                cfg = json.loads(strip_jsonc(raw))
            entry = cfg.get('provider', {}).get('lmstudio', {}).get('models', {}).get(model)
            if entry is None:
                sys.exit(0)
            limit = entry.setdefault('limit', {})
            if limit.get('context') != ctx:
                limit['context'] = ctx
                limit['output'] = min(8192, max(2048, ctx // 8))
                open(path, 'w', encoding='utf-8').write(json.dumps(cfg, indent=2, ensure_ascii=False) + NEWLINE)
                print('Kontextfenster aus LM Studio uebernommen: %d Tokens.' % ctx)
        PYEOF

            "$LMS" server start >/dev/null 2>&1

            lmsState() {
                LMSINFO=$("$LMS" ps --json 2>/dev/null | python3 "$LMSPY" ctx "$LMSMODEL" 2>/dev/null)
                [ -z "$LMSINFO" ] && LMSINFO='0 0'
                LOADEDCTX=${LMSINFO%% *}
                MAXCTX=${LMSINFO##* }
                [ -z "$LOADEDCTX" ] && LOADEDCTX=0
                [ -z "$MAXCTX" ] && MAXCTX=0
            }

            lmsState

            # Zu klein geladen (LM-Studio-Vorgabe ist oft 4096): entladen und gross neu laden.
            if [ "$LOADEDCTX" -gt 0 ] 2>/dev/null && [ "$LOADEDCTX" -lt "$LMSMINCTX" ] 2>/dev/null; then
                printf '\\033[33m%s ist in LM Studio mit nur %s Tokens Kontext geladen. OpenCode braucht allein fuer den Systemprompt rund 22000 - damit bricht die erste Anfrage ab. Das Modell wird jetzt mit groesserem Kontext neu geladen.\\033[0m\\n' "$LMSMODEL" "$LOADEDCTX"
                "$LMS" unload "$LMSMODEL" >/dev/null 2>&1
                LOADEDCTX=0
            fi

            if [ "$LOADEDCTX" -le 0 ] 2>/dev/null; then
                if [ "$MAXCTX" -le 0 ] 2>/dev/null; then
                    MAXCTX=$("$LMS" ls --json 2>/dev/null | python3 "$LMSPY" max "$LMSMODEL" 2>/dev/null)
                    [ -z "$MAXCTX" ] && MAXCTX=0
                fi
                TARGETCTX=$LMSWANTCTX
                if [ "$MAXCTX" -gt 0 ] 2>/dev/null && [ "$MAXCTX" -lt "$TARGETCTX" ] 2>/dev/null; then
                    TARGETCTX=$MAXCTX
                fi
                printf '\\033[36mLade lokales Modell %s mit %s Tokens Kontext - das kann einige Minuten dauern ...\\033[0m\\n' "$LMSMODEL" "$TARGETCTX"
                if ! "$LMS" load "$LMSMODEL" --context-length "$TARGETCTX" -y; then
                    printf '\\033[33m%s Tokens haben nicht gepasst - versuche %s ...\\033[0m\\n' "$TARGETCTX" "$LMSMINCTX"
                    if ! "$LMS" load "$LMSMODEL" --context-length "$LMSMINCTX" -y; then
                        printf '\\033[33mAutomatisches Laden fehlgeschlagen - bitte %s in LM Studio von Hand mit mindestens %s Tokens Kontext laden.\\033[0m\\n' "$LMSMODEL" "$LMSMINCTX"
                    fi
                fi
                lmsState
            fi

            if [ "$LOADEDCTX" -ge "$LMSMINCTX" ] 2>/dev/null; then
                printf '\\033[90mLokales Modell %s ist mit %s Tokens Kontext geladen.\\033[0m\\n' "$LMSMODEL" "$LOADEDCTX"
            elif [ "$LOADEDCTX" -gt 0 ] 2>/dev/null; then
                printf '\\033[33mAchtung: nur %s Tokens Kontext - OpenCode bricht die erste Anfrage moeglicherweise ab.\\033[0m\\n' "$LOADEDCTX"
            else
                printf '\\033[33m%s konnte nicht geladen werden - LM Studio versucht es bei der ersten Anfrage selbst.\\033[0m\\n' "$LMSMODEL"
            fi

            # Das Kontextfenster in der opencode-Konfig muss exakt der Ladeeinstellung in LM Studio
            # entsprechen. Sonst rechnet OpenCode gegen eine falsche Obergrenze: bei zu kleinem Wert
            # meldet es sofort einen fast vollen Kontext und komprimiert endlos im Kreis.
            if [ "$LOADEDCTX" -gt 0 ] 2>/dev/null; then
                CFGPATH="$HOME/.config/opencode/opencode.jsonc"
                [ -f "$CFGPATH" ] || CFGPATH="$HOME/.config/opencode/opencode.json"
                if [ -f "$CFGPATH" ]; then
                    python3 "$LMSPY" patch "$CFGPATH" "$LMSMODEL" "$LOADEDCTX" || printf '\\033[33mKontextfenster konnte nicht in die opencode-Konfig geschrieben werden.\\033[0m\\n'
                fi
            fi

            rm -f "$LMSPY" 2>/dev/null || true
        fi
        """
    }

    /// Stellt den NVIDIA-Schluessel fuer eine NVIDIA-Sitzung bereit. OpenCode kennt den
    /// models.dev-Provider "nvidia" nur, wenn NVIDIA_API_KEY in der Umgebung steht - ohne ihn taucht
    /// kein einziges nvidia/-Modell auf und der Start scheitert am unbekannten Provider.
    /// Der Schluessel liegt ausschliesslich in $HOME/SK/NvidiaDev/.env (Poka-Yoke: Secrets nie im
    /// Repo). Das Startskript liest ihn ZUR LAUFZEIT selbst, statt ihn hier einzusetzen - so steht er
    /// auch nicht im Temp-Skript. Fuer alle anderen Provider bleibt der Block leer.
    private static func buildNvidiaKeyScript(modelString: String) -> String {
        guard modelString.lowercased().hasPrefix("\(ModelEntry.nvidiaProviderId)/") else { return "" }
        return """
        NVIDIA_ENV_FILE="$HOME/SK/NvidiaDev/.env"
        if [ -f "$NVIDIA_ENV_FILE" ]; then
            while IFS= read -r nvidiaLine; do
                case "$nvidiaLine" in
                    *NVIDIA_API_KEY*=*)
                        nvidiaValue="${nvidiaLine#*=}"
                        nvidiaValue="${nvidiaValue%\\\"}"; nvidiaValue="${nvidiaValue#\\\"}"
                        nvidiaValue="${nvidiaValue%\\'}"; nvidiaValue="${nvidiaValue#\\'}"
                        export NVIDIA_API_KEY="$nvidiaValue"
                        ;;
                esac
            done < "$NVIDIA_ENV_FILE"
        fi
        if [ -z "$NVIDIA_API_KEY" ]; then
            printf '\\033[31m[NVIDIA] Kein Schluessel in %s gefunden - OpenCode kennt den Provider "nvidia" dann nicht.\\033[0m\\n' "$NVIDIA_ENV_FILE"
        fi
        """
    }

    /// Sucht die zu verwendende opencode-Binaerdatei. Windows nutzt den Mousefix-Build; auf macOS
    /// gibt es den nicht, deshalb Reihenfolge: Mousefix-Zeiger (falls doch vorhanden) ->
    /// ~/.opencode/bin/opencode -> PATH.
    private static func resolveOpenCodeExecutable() -> String {
        let root = (Paths.home as NSString).appendingPathComponent(".local/share/opencode-mousefix")
        for pointerName in ["current.json", "current.json.bak"] {
            let pointerPath = (root as NSString).appendingPathComponent(pointerName)
            guard let data = FileManager.default.contents(atPath: pointerPath),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { continue }
            for entryName in ["active", "previous"] {
                guard let entry = json[entryName] as? [String: Any],
                      let relative = entry["relativeExe"] as? String,
                      !relative.trimmingCharacters(in: .whitespaces).isEmpty else { continue }
                let fullRoot = (root as NSString).standardizingPath + "/"
                let fullPath = ((root as NSString).appendingPathComponent(relative) as NSString).standardizingPath
                guard fullPath.hasPrefix(fullRoot) else { continue }
                if FileManager.default.isExecutableFile(atPath: fullPath) { return fullPath }
            }
        }

        let userInstall = (Paths.home as NSString).appendingPathComponent(".opencode/bin/opencode")
        if FileManager.default.isExecutableFile(atPath: userInstall) { return userInstall }
        return Shell.which("opencode") ?? "opencode"
    }

    private static func normalizeThinkingLevel(_ thinkingLevel: String?) -> String? {
        guard let thinkingLevel, !thinkingLevel.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return thinkingLevel.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    // ===================== opencode-Konfig =====================

    private static var modelStatePath: String {
        (Paths.home as NSString).appendingPathComponent(".local/state/opencode/model.json")
    }

    private static func patchModelVariantState(modelString: String, thinkingLevel rawLevel: String?) {
        guard let thinkingLevel = normalizeThinkingLevel(rawLevel) else { return }

        let statePath = modelStatePath
        Paths.ensureDirectory((statePath as NSString).deletingLastPathComponent)

        var root = JSONNode.object()
        if Paths.fileExists(statePath) {
            let raw = Paths.readText(statePath)
            if !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                if let parsed = JSONNode.parse(raw) {
                    root = JSONNode.ensureObject(parsed)
                } else {
                    Logger.shared.warn("OpenLauncherService", "patchModelVariantState",
                                       "model.json konnte nicht gelesen werden, State wird neu aufgebaut",
                                       ["statePath": statePath])
                }
            }
        }

        let variants = root.getOrAddObject("variant")
        variants[modelString] = .string(thinkingLevel)
        Paths.writeAtomic(root.serialized(), to: statePath)
    }

    private static func readConfig() -> JSONNode {
        let configPath = resolveConfigPath()
        guard Paths.fileExists(configPath) else { return .object() }
        // BOM-frei sicherstellen (Kurzcheck opencode-cli §10.3): ein BOM bricht den Parse.
        var raw = Paths.readText(configPath)
        if raw.hasPrefix("\u{FEFF}") { raw.removeFirst() }
        if raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return .object() }
        return JSONNode.parse(raw) ?? .object()
    }

    private static func writeConfig(_ root: JSONNode) throws {
        let configPath = resolveConfigPath()
        Paths.ensureDirectory((configPath as NSString).deletingLastPathComponent)
        // Backup (.bak ueberschreibt den vorherigen Lauf - bewusst, nur ein Rollback-Punkt noetig).
        if Paths.fileExists(configPath) {
            let backup = configPath + ".bak"
            try? FileManager.default.removeItem(atPath: backup)
            try? FileManager.default.copyItem(atPath: configPath, toPath: backup)
        }
        // Temp-Datei + atomares Ersetzen (verhindert korrupte Config bei Absturz mitten im Schreiben).
        guard Paths.writeAtomic(root.serialized(), to: configPath) else {
            throw LauncherError.message("opencode-Konfig konnte nicht geschrieben werden: \(configPath)")
        }
    }

    private static func resolveConfigPath() -> String {
        let jsonc = (configDir as NSString).appendingPathComponent("opencode.jsonc")
        if Paths.fileExists(jsonc) { return jsonc }
        return (configDir as NSString).appendingPathComponent("opencode.json")
    }

    private static func patchProvider(_ root: JSONNode, slug: String, modelDisplayName: String,
                                      chosen: ProviderEntry, thinkingLevel: String?) -> JSONNode {
        // order enthaelt bewusst nur den gewaehlten Provider: exaktes Routing ohne Fallback.
        let order = JSONNode.array([.string(chosen.providerSlug)])

        let providerBlock = JSONNode.object()
        providerBlock["order"] = order
        providerBlock["allow_fallbacks"] = .bool(false)
        providerBlock["require_parameters"] = .bool(true)

        let optionsBlock = JSONNode.object()
        optionsBlock["provider"] = providerBlock
        clearThinkingOptions(optionsBlock)
        applyThinkingOptions(optionsBlock, providerId: "openrouter", slug: slug, thinkingLevel: thinkingLevel)

        // ensureObject-Ergebnis zurueckgeben, NICHT root: ist root ausnahmsweise kein Objekt
        // (korrupte opencode.json als Array/Skalar), liefert ensureObject ein neues, losgeloestes
        // Objekt - ein "return root" wuerde die Datei unveraendert zurueckschreiben und den Patch
        // still verwerfen. Fuer gueltige Objekt-Configs ist rootObject identisch mit root.
        let rootObject = JSONNode.ensureObject(root)
        let models = rootObject.getOrAddObject("provider").getOrAddObject("openrouter").getOrAddObject("models")
        let modelNode = models.getOrAddObject(slug)
        modelNode["name"] = .string("\(modelDisplayName) via \(chosen.providerName)")
        modelNode["options"] = optionsBlock

        return rootObject
    }

    /// Traegt den LM-Studio-Provider (OpenAI-kompatibel, lokal) samt gewaehltem Modell in die globale
    /// opencode-Konfig ein. Bestehende Eintraege bleiben erhalten, es wird nur ergaenzt.
    private static func patchLmStudioModel(_ root: JSONNode, slug: String, displayName: String) -> JSONNode {
        let rootObject = JSONNode.ensureObject(root)
        let provider = rootObject.getOrAddObject("provider").getOrAddObject(LmStudioService.providerId)
        provider["npm"] = .string("@ai-sdk/openai-compatible")
        provider["name"] = .string("LM Studio (lokal)")

        let options = provider.getOrAddObject("options")
        options["baseURL"] = .string(LmStudioService.baseUrl)
        // LM Studio prueft keinen Schluessel, das SDK verlangt aber einen nicht-leeren Wert.
        options["apiKey"] = .string("lm-studio")

        let modelNode = provider.getOrAddObject("models").getOrAddObject(slug)
        modelNode["name"] = .string(displayName)
        modelNode["tool_call"] = .bool(true)

        // Das Kontextfenster MUSS der Ladeeinstellung in LM Studio entsprechen. Steht hier eine
        // kleinere Zahl, haelt OpenCode den Kontext fuer fast voll, startet sofort die
        // Auto-Komprimierung und komprimiert danach immer wieder das Komprimierte. Ist das Modell
        // noch nicht geladen, gilt vorlaeufig der Wert, mit dem das Startskript laedt - es korrigiert
        // den Eintrag danach auf den tatsaechlichen Wert.
        // Ein vom Benutzer selbst geladenes Modell hat oft nur die LM-Studio-Vorgabe von 4096
        // Tokens. Dieser Wert darf NICHT in die Konfig wandern - OpenCode bricht damit sofort ab.
        // Das Startskript laedt in dem Fall sichtbar mit groesserem Kontext neu und traegt den
        // tatsaechlichen Wert danach nach.
        let loadedContext = LmStudioService.loadedContextLength(modelId: slug)
        let context = loadedContext >= LmStudioService.minimumAgentContext ? loadedContext : defaultLmStudioContext
        let limit = modelNode.getOrAddObject("limit")
        limit["context"] = .number(context)
        limit["output"] = .number(LmStudioService.outputLimit(for: context))
        return rootObject
    }

    private static func patchDirectModel(_ root: JSONNode, providerId: String, slug: String,
                                         displayName: String) -> JSONNode {
        // Siehe patchProvider: ensureObject-Ergebnis verwenden und zurueckgeben, damit der Patch auch
        // bei einer nicht-objektfoermigen Wurzel greift statt still verloren zu gehen.
        let rootObject = JSONNode.ensureObject(root)
        let models = rootObject.getOrAddObject("provider").getOrAddObject(providerId).getOrAddObject("models")
        let modelNode = models.getOrAddObject(slug)
        modelNode["name"] = .string(displayName)

        let optionsBlock = modelNode.getOrAddObject("options")
        // OpenCode-Varianten besitzen die aktive Reasoning-Stufe. Bliebe reasoningEffort in den
        // Modell-Optionen stehen, waere jede Anfrage darauf festgenagelt und ein Wechsel innerhalb
        // der Sitzung wirkungslos.
        clearThinkingOptions(optionsBlock)
        if usesPriorityServiceTier(providerId: providerId, slug: slug) { normalizeExistingOpenAIModels(models) }
        return rootObject
    }

    @discardableResult
    private static func removeLegacyGpt56Overrides(_ root: JSONNode) -> Bool {
        guard root.isObject,
              let providers = root["provider"], providers.isObject,
              let openAi = providers["openai"], openAi.isObject,
              let models = openAi["models"], models.isObject else { return false }

        var changed = false
        for slug in [gpt56SolSlug, gpt56SolFastSlug, gpt56TerraSlug, gpt56TerraFastSlug,
                     gpt56LunaSlug, gpt56LunaFastSlug] {
            if models.remove(slug) { changed = true }
        }
        return changed
    }

    private static func normalizeExistingOpenAIModels(_ models: JSONNode) {
        normalizeModelPair(models, normalSlug: gpt55Slug, fastSlug: gpt55FastSlug, displayName: "GPT-5.5")
    }

    private static func normalizeModelPair(_ models: JSONNode, normalSlug: String, fastSlug: String, displayName: String) {
        if let normal = models[normalSlug], normal.isObject {
            normal["name"] = .string(displayName)
            normal.remove("id")
            let normalOptions = normal.getOrAddObject("options")
            normalOptions.remove("serviceTier")
            clearThinkingOptions(normalOptions)
        }

        if let fast = models[fastSlug], fast.isObject {
            fast["id"] = .string(normalSlug)
            fast["name"] = .string("\(displayName) Fast")
            let fastOptions = fast.getOrAddObject("options")
            clearThinkingOptions(fastOptions)
            fastOptions["serviceTier"] = .string("priority")
        }
    }

    private static func usesPriorityServiceTier(providerId: String, slug: String) -> Bool {
        guard providerId.caseInsensitiveCompare("openai") == .orderedSame else { return false }
        var modelId = slug.trimmingCharacters(in: .whitespacesAndNewlines)
        if modelId.lowercased().hasPrefix("openai/") { modelId = String(modelId.dropFirst("openai/".count)) }
        return [gpt56SolFastSlug, gpt56TerraFastSlug, gpt56LunaFastSlug, gpt55FastSlug]
            .contains { $0.caseInsensitiveCompare(modelId) == .orderedSame }
    }

    private static func isGpt56Model(providerId: String, slug: String) -> Bool {
        guard providerId.caseInsensitiveCompare("openai") == .orderedSame else { return false }
        var modelId = slug.trimmingCharacters(in: .whitespacesAndNewlines)
        if modelId.lowercased().hasPrefix("openai/") { modelId = String(modelId.dropFirst("openai/".count)) }
        return [gpt56SolSlug, gpt56SolFastSlug, gpt56TerraSlug, gpt56TerraFastSlug,
                gpt56LunaSlug, gpt56LunaFastSlug]
            .contains { $0.caseInsensitiveCompare(modelId) == .orderedSame }
    }

    private static func applyThinkingOptions(_ optionsBlock: JSONNode, providerId: String,
                                             slug: String, thinkingLevel rawLevel: String?) {
        guard let thinkingLevel = normalizeThinkingLevel(rawLevel) else { return }

        let id = slug.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if providerId.caseInsensitiveCompare("openai") == .orderedSame
            || id.hasPrefix("openai/")
            || id.contains("gpt") {
            optionsBlock["reasoningEffort"] = .string(thinkingLevel)
            return
        }

        let reasoning = JSONNode.object()
        reasoning["effort"] = .string(thinkingLevel)
        optionsBlock["reasoning"] = reasoning
    }

    private static func clearThinkingOptions(_ optionsBlock: JSONNode) {
        optionsBlock.remove("reasoningEffort")
        optionsBlock.remove("reasoningSummary")
        optionsBlock.remove("reasoning")
        optionsBlock.remove("thinking")
        optionsBlock.remove("effort")
    }
}
