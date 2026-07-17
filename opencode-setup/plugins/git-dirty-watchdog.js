// git-dirty-watchdog.js — Stille Protokollwarnung, wenn eine Session idle wird, obwohl das
// Repo ungespeicherte Git-Aenderungen hat (Code-Durchsetzung, nicht Prompt-Bitte).
//
// Hintergrund (Vorfall 2026-07-01, Almanach bugs/claude-tooling/claude-config.md §1.1 /
// bugs/opencode/opencode-cli.md): Ein Modell (GPT-5.5) beendete eine CortexAndroid-Aufgabe mit
// erfolgreichem Build+Install, aber OHNE Commit/Push — Begruendung: "Commit/Push habe ich nicht
// gemacht, weil das hier nur auf ausdruecklich Anweisung erlaubt ist." Diese Regel steht NIRGENDS
// in Franks Config (AGENTS.md/rules-opencode verlangen im Gegenteil Commit+Push nach JEDER
// Aufgabe, siehe commit-push-jede-aufgabe-vor-build.md). Vermutete Ursache: trainiertes
// Vorsichts-Verhalten des Modells gegenueber "sichtbaren"/geteilten Aktionen wie `git push` —
// das schlaegt reine Text-Regeln, egal wie oft man sie wiederholt ("Rules in prompts are
// requests, hooks in code are laws", best-practices/agents/anti-halluzination-regeln.md §7).
//
// Was dieses Plugin NICHT tut: automatisch committen/pushen. Ein Plugin kann nicht zuverlaessig
// unterscheiden, welche geaenderten Dateien zu DIESER Session gehoeren vs. einer parallelen
// Session (siehe parallel-sessions-git.md) — ein automatisches `git add -A && commit && push`
// waere selbst ein Risiko (fremde/halbfertige Aenderungen mitreissen). Es macht das Vergessen
// stattdessen im strukturierten OpenCode-Log sichtbar. Akustik und System-Notifications gehoeren
// ausschliesslich dem Completion-Guard, damit Zwischen-Idle-Ereignisse nicht unterbrechen.
//
// Direktive #3: jeder Hook in try/catch, darf OpenCode NIE crashen (fail-open).

export const GitDirtyWatchdog = async ({ client, $, directory }) => {
	const log = async (level, message) => {
		// NUR strukturiert in die OpenCode-Log-Datei schreiben — NIEMALS console.* / stdout / stderr.
		// Plugins laufen IM TUI-Prozess: jeder direkte Terminal-Schreibvorgang zerstoert das
		// TUI-Rendering (Vorfall 2026-07-01: die [git-dirty-watchdog]-Zeile blutete sichtbar in die
		// TUI). best-practices/opencode/plugins-mcp-skills.md: client.app.log statt console.*.
		try {
			await client.app.log({
				body: { service: "git-dirty-watchdog", level, message },
			});
		} catch {}
	};

	return {
		event: async ({ event }) => {
			if (!event || event.type !== "session.idle") return;

			let dirty = "";
			try {
				const dir = directory || process.cwd();
				// .quiet() captured stdout OHNE es ans TTY zu echoen. OHNE .quiet echoed Bun's
				// Shell die git-porcelain-Zeilen (?? / M ...) direkt in die TUI -> Layout kaputt
				// (Vorfall 2026-07-01: linke Spalte + untracked-Zeilen bluteten sichtbar rein).
				const result = await $`cd "${dir}" && git status --porcelain`.quiet();
				dirty = String((result && result.stdout) || "").trim();
			} catch {
				// Kein Git-Repo / git nicht verfuegbar -> nichts zu warnen, sauber beenden.
				return;
			}

			if (!dirty) return; // Working Tree sauber -> alles gut, kein Alarm.

			const changedCount = dirty.split("\n").filter((l) => l.trim()).length;
			await log(
				"error",
				`Session beendet, aber ${changedCount} ungespeicherte Git-Aenderung(en) im Repo! ` +
					`Commit+Push ist Pflicht nach jeder Aufgabe (AGENTS.md / commit-push-jede-aufgabe-vor-build.md). ` +
					`Bitte pruefen: git status --short`,
			);
		},
	};
};
