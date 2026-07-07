// tool-first-guard.js — Anti-Halluzinations-Plugin fuer OpenCode (Code-Durchsetzung, nicht Prompt-Bitte)
//
// Setzt die WICHTIGSTE Anti-Halluzinations-Regel deterministisch durch: "Tool-first, nicht
// Memory-first". Bevor das Modell eine BESTEHENDE Datei aendert (edit/patch), muss es sie in
// dieser Session vorher mit `read` GELESEN haben. Aendert es eine Datei, die es nie gelesen hat,
// "behauptet" es ueber deren Inhalt aus dem Gedaechtnis (oft falsch) → klassische Halluzination.
//
// Hintergrund: best-practices/agents/anti-halluzination-regeln.md §1 (Regel 2 "Tool-first") + §7
// ("Rules in prompts are requests, hooks in code are laws"). Gerade schwache/guenstige Modelle
// (GLM-Flash, DeepSeek V4 Pro u.a.) ignorieren Prompt-Regeln haeufig — diese Schicht greift im Code.
//
// Verhalten:
//   - tool.execute.after : merkt sich jede mit `read` gelesene Datei (Set, pro Session/Prozess).
//   - tool.execute.before fuer `edit`/`patch`: ist die Zieldatei NICHT gelesen worden →
//       Default              : WARNEN (Log, EINMAL pro Datei) und durchlassen.
//       OPENCODE_TOOL_FIRST_ENFORCE=1 : BLOCKEN (throw) — die "Laws"-Ebene.
//   - `write` wird bewusst NICHT geprueft: eine NEUE Datei kann man nicht vorher lesen.
//
// Grenze (ehrlich): prueft die DATEI-Ebene (edit ohne read), nicht die FUNKTIONS-Ebene (erfundene
// Funktions-/API-Namen) — letzteres braeuchte LSP/grep-Analyse und ist hier nicht deterministisch.
//
// Robust (Direktive #3): jeder Hook in try/catch; ein Plugin-Fehler darf OpenCode NIE crashen.
// Der einzige bewusste throw ist der Block im ENFORCE-Modus — und der liegt AUSSERHALB des try.

export const ToolFirstGuard = async ({ client }) => {
	const seen = new Set(); // normalisierte Pfade, die per `read` gelesen wurden
	const warned = new Set(); // Pfade, fuer die schon gewarnt wurde (kein Spam)

	const enforce =
		(globalThis.process &&
			globalThis.process.env &&
			globalThis.process.env.OPENCODE_TOOL_FIRST_ENFORCE === "1") ||
		false;

	const READ_TOOLS = new Set(["read"]);
	const CHECK_TOOLS = new Set(["edit", "patch"]);

	const norm = (p) => {
		try {
			return String(p || "")
				.replace(/\\/g, "/")
				.toLowerCase()
				.trim();
		} catch {
			return "";
		}
	};

	const pathOf = (args) => {
		if (!args || typeof args !== "object") return "";
		return args.filePath || args.path || args.file || "";
	};

	const log = async (level, message) => {
		// NUR strukturiert loggen (client.app.log) — NIEMALS console.* : Plugins laufen IM
		// TUI-Prozess, direktes stderr/stdout zerstoert das TUI-Rendering
		// (best-practices/opencode/plugins-mcp-skills.md: client.app.log statt console.*).
		try {
			await client.app.log({
				body: { service: "tool-first-guard", level, message },
			});
		} catch {}
	};

	return {
		// Nach jedem `read` die gelesene Datei vormerken.
		"tool.execute.after": async (input, output) => {
			try {
				if (input && READ_TOOLS.has(input.tool)) {
					const p = norm(pathOf(output && output.args));
					if (p) seen.add(p);
				}
			} catch {}
		},

		// Vor jedem `edit`/`patch` pruefen, ob die Datei gelesen wurde.
		"tool.execute.before": async (input, output) => {
			let blockMsg = null;
			try {
				if (!input || !CHECK_TOOLS.has(input.tool)) return;
				const raw = pathOf(output && output.args);
				const p = norm(raw);
				if (!p || seen.has(p)) return; // unbekannter Arg-Shape oder bereits gelesen → ok

				const msg =
					`'${input.tool}' aendert '${raw}', das in dieser Session NICHT mit 'read' gelesen wurde. ` +
					`Tool-first statt Memory-first: erst lesen, dann aendern (Anti-Halluzination).`;

				if (enforce) {
					blockMsg = msg;
					await log(
						"error",
						"BLOCKIERT (OPENCODE_TOOL_FIRST_ENFORCE=1): " + msg,
					);
				} else if (!warned.has(p)) {
					warned.add(p);
					await log("warn", msg);
				}
			} catch {
				// Warn-Modus: niemals crashen. (Block laeuft ausserhalb dieses try.)
			}
			if (blockMsg) throw new Error("tool-first-guard: " + blockMsg);
		},
	};
};
