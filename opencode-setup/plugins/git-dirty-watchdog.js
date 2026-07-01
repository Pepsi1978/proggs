// git-dirty-watchdog.js — Hoerbare+sichtbare Warnung, wenn eine Session idle wird, obwohl das
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
// stattdessen UNMOEGLICH zu uebersehen: sobald die Session idle wird und `git status` NICHT
// sauber ist, ertoent ein Warnton (dasselbe error.wav wie bei anderen Fehlern) + ein Log-Eintrag.
//
// Direktive #3: jeder Hook in try/catch, darf OpenCode NIE crashen (fail-open).

export const GitDirtyWatchdog = async ({ client, $, directory }) => {
	const SOUND_PATH = "C:\\Users\\barwa\\.config\\opencode\\sounds\\error.wav";

	const log = async (level, message) => {
		try {
			await client.app.log({
				body: { service: "git-dirty-watchdog", level, message },
			});
		} catch {}
		try {
			(level === "error" ? console.error : console.warn)(
				`[git-dirty-watchdog] ${message}`,
			);
		} catch {}
	};

	const playAlertSound = async () => {
		try {
			await $`powershell -NoProfile -Command "(New-Object Media.SoundPlayer '${SOUND_PATH}').PlaySync()"`;
		} catch {
			// Sound ist nur Komfort — nie deswegen crashen oder die Warnung unterdruecken.
		}
	};

	return {
		event: async ({ event }) => {
			if (!event || event.type !== "session.idle") return;

			let dirty = "";
			try {
				const dir = directory || process.cwd();
				const result = await $`cd "${dir}" && git status --porcelain`;
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
			await playAlertSound();
		},
	};
};
