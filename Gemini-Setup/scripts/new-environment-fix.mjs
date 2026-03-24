#!/usr/bin/env node

import path from "node:path";
import { spawnSync } from "node:child_process";
import { createInterface } from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REGISTER_SCRIPT = path.join(__dirname, "register-environment-fix.mjs");

const REQUIRED_FIELDS = [
	{ key: "id", prompt: "Fix-ID" },
	{ key: "category", prompt: "Kategorie" },
	{ key: "summary", prompt: "Kurzfassung" },
	{ key: "context", prompt: "Kontext fuer andere CLIs" },
	{ key: "symptom", prompt: "Symptom vor dem Fix" },
	{ key: "root-cause", prompt: "Root Cause" },
	{ key: "what", prompt: "Was wurde konkret gefixt" },
	{ key: "why", prompt: "Warum wurde es gefixt" },
	{ key: "exact-error", prompt: "Exakter Fehlertext" },
	{ key: "why-chain", prompt: "Why-Chain" },
	{ key: "related-checks", prompt: "Gepruefte verwandte Fehlerquellen" },
	{ key: "wrong-pattern", prompt: "Falsches Muster" },
	{ key: "right-pattern", prompt: "Richtiges Muster" },
	{ key: "avoidance-rule", prompt: "Vermeidungsregel" },
	{ key: "resilience-summary", prompt: "Resilienz-Zusammenfassung" },
	{ key: "failure-review", prompt: "Fix-Induced-Failure-Review" },
	{ key: "verification", prompt: "Verifikation" },
	{ key: "portability-notes", prompt: "Portierungshinweise" },
];

const OPTIONAL_FIELDS = [
	{
		key: "portable-to",
		prompt: "Portable-to CSV",
		defaultValue: "Claude Code, Codex",
	},
	{ key: "artifacts", prompt: "Artefakte CSV", defaultValue: "" },
	{ key: "impact", prompt: "Impact", defaultValue: "" },
	{ key: "source-cli", prompt: "Source CLI", defaultValue: "Gemini CLI" },
	{ key: "status", prompt: "Status", defaultValue: "active" },
];

function usage() {
	return [
		"Usage:",
		"  new-environment-fix.mjs [--state PATH] [--json]",
		"  new-environment-fix.mjs --id ID --category CATEGORY ...",
		"",
		"If required fields are missing and stdin is interactive, the script prompts for them.",
	].join("\n");
}

function parseArgs(argv) {
	const args = { _: [], json: false };
	for (let i = 0; i < argv.length; i += 1) {
		const part = argv[i];
		if (!part.startsWith("--")) {
			args._.push(part);
			continue;
		}
		const key = part.slice(2);
		if (key === "json" || key === "help") {
			args[key] = true;
			args.json = true;
			if (key === "help") {
				args.json = false;
			}
			continue;
		}
		const next = argv[i + 1];
		if (!next || next.startsWith("--")) {
			throw new Error(`Missing value for --${key}`);
		}
		args[key] = next;
		i += 1;
	}
	return args;
}

function hasValue(value) {
	return `${value || ""}`.trim().length > 0;
}

async function promptForMissing(args) {
	if (!input.isTTY) {
		const missing = REQUIRED_FIELDS.filter(
			(field) => !hasValue(args[field.key]),
		).map((field) => field.key);
		if (missing.length > 0) {
			throw new Error(
				`Missing required fields in non-interactive mode: ${missing.join(", ")}`,
			);
		}
		return args;
	}

	const rl = createInterface({ input, output });
	try {
		for (const field of REQUIRED_FIELDS) {
			if (hasValue(args[field.key])) continue;
			args[field.key] = (await rl.question(`${field.prompt}: `)).trim();
		}
		for (const field of OPTIONAL_FIELDS) {
			if (hasValue(args[field.key])) continue;
			const suffix = field.defaultValue
				? ` [Default: ${field.defaultValue}]`
				: "";
			const answer = (await rl.question(`${field.prompt}${suffix}: `)).trim();
			args[field.key] = answer || field.defaultValue;
		}
	} finally {
		rl.close();
	}
	return args;
}

function buildRegisterArgs(args) {
	const cliArgs = [REGISTER_SCRIPT, "add"];
	for (const field of REQUIRED_FIELDS) {
		cliArgs.push(`--${field.key}`, args[field.key]);
	}
	for (const field of OPTIONAL_FIELDS) {
		if (hasValue(args[field.key])) {
			cliArgs.push(`--${field.key}`, args[field.key]);
		}
	}
	if (hasValue(args.state)) {
		cliArgs.push("--state", args.state);
	}
	if (args.json) {
		cliArgs.push("--json");
	}
	return cliArgs;
}

async function main() {
	const args = parseArgs(process.argv.slice(2));
	if (args.help || args._.includes("help")) {
		process.stdout.write(`${usage()}\n`);
		return;
	}

	await promptForMissing(args);
	const result = spawnSync(process.execPath, buildRegisterArgs(args), {
		stdio: "inherit",
	});
	if (result.status !== 0) {
		process.exit(result.status ?? 1);
	}
}

main().catch((error) => {
	process.stderr.write(`${error.message}\n`);
	process.exit(1);
});
