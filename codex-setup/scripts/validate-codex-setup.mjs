#!/usr/bin/env node

import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const DESCRIPTION_LIMIT = 1024;
const CONTROL_FILE_EXTENSIONS = new Set([
  ".md",
  ".mjs",
  ".js",
  ".ts",
  ".ps1",
  ".sh",
  ".json",
  ".toml",
]);
const BASH_PYTHON_HEREDOC_PATTERN =
  /\b(?:python(?:3)?|py(?:\.exe)?|uv\s+run\s+python)\b[\s\S]{0,160}?<<-?\s*["']?[A-Za-z0-9_]+["']?/i;

function resolveRepoRoot(startDir) {
  let current = path.resolve(startDir);
  while (true) {
    if (fs.existsSync(path.join(current, "codex-setup"))) {
      return current;
    }
    const parent = path.dirname(current);
    if (parent === current) {
      throw new Error(
        "Konnte den Repo-Root mit codex-setup/ nicht aufloesen.",
      );
    }
    current = parent;
  }
}

function walkFiles(rootDir, predicate, results = []) {
  if (!fs.existsSync(rootDir)) {
    return results;
  }

  for (const entry of fs.readdirSync(rootDir, { withFileTypes: true })) {
    const fullPath = path.join(rootDir, entry.name);
    if (entry.isDirectory()) {
      walkFiles(fullPath, predicate, results);
      continue;
    }
    if (entry.isFile() && predicate(fullPath)) {
      results.push(fullPath);
    }
  }
  return results;
}

function stripWrappingQuotes(value) {
  if (
    value.length >= 2 &&
    ((value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'")))
  ) {
    return value.slice(1, -1);
  }
  return value;
}

function readFrontmatter(filePath) {
  const text = fs.readFileSync(filePath, "utf8");
  const match = text.match(/^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/);
  return match ? { text, frontmatter: match[1] } : { text, frontmatter: null };
}

function parseFrontmatterFields(frontmatter) {
  const lines = frontmatter.split(/\r?\n/);
  let name = "";
  let description = "";
  let inlineDescription = null;

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    const nameMatch = line.match(/^name:\s*(.+?)\s*$/);
    if (nameMatch) {
      name = stripWrappingQuotes(nameMatch[1].trim());
      continue;
    }

    const descriptionMatch = line.match(/^description:\s*(.*)$/);
    if (!descriptionMatch) {
      continue;
    }

    const rawValue = descriptionMatch[1].trim();
    inlineDescription = rawValue;

    if (["|", "|-", ">", ">-"].includes(rawValue)) {
      const blockLines = [];
      for (let nextIndex = index + 1; nextIndex < lines.length; nextIndex += 1) {
        const nextLine = lines[nextIndex];
        if (nextLine.trim() === "") {
          blockLines.push("");
          continue;
        }
        if (!/^\s/.test(nextLine)) {
          break;
        }
        blockLines.push(nextLine.replace(/^\s+/, ""));
        index = nextIndex;
      }

      description = rawValue.startsWith(">")
        ? blockLines.join(" ").replace(/\s+/g, " ").trim()
        : blockLines.join("\n").trim();
      continue;
    }

    description = stripWrappingQuotes(rawValue).trim();
  }

  return { name, description, inlineDescription };
}

function formatPath(repoRoot, filePath) {
  const relative = path.relative(repoRoot, filePath);
  if (!relative.startsWith("..")) {
    return relative;
  }
  return filePath;
}

function validateSkillFrontmatter(repoRoot, skillFile, errors) {
  const { frontmatter } = readFrontmatter(skillFile);
  const label = formatPath(repoRoot, skillFile);

  if (!frontmatter) {
    errors.push(`${label}: fehlender oder unvollstaendiger Frontmatter-Block`);
    return;
  }

  const { name, description, inlineDescription } =
    parseFrontmatterFields(frontmatter);

  if (!name) {
    errors.push(`${label}: name fehlt im Frontmatter`);
  }

  if (!description) {
    errors.push(`${label}: description fehlt oder ist leer`);
  }

  if (description.length > DESCRIPTION_LIMIT) {
    errors.push(
      `${label}: description ist ${description.length} Zeichen lang (Limit ${DESCRIPTION_LIMIT})`,
    );
  }

  if (
    inlineDescription &&
    !["|", "|-", ">", ">-"].includes(inlineDescription) &&
    !/^["']/.test(inlineDescription) &&
    /:\s/.test(inlineDescription)
  ) {
    errors.push(
      `${label}: inline-description enthaelt ': ' ohne Quotes oder Block-Syntax und ist YAML-fragil`,
    );
  }
}

function validatePythonHeredoc(repoRoot, codexSetupDir, errors) {
  const controlRoots = [
    path.join(codexSetupDir, "rules"),
    path.join(codexSetupDir, "scripts"),
    path.join(codexSetupDir, "README.md"),
  ];
  const controlFiles = [];

  for (const root of controlRoots) {
    if (!fs.existsSync(root)) {
      continue;
    }
    const stat = fs.statSync(root);
    if (stat.isDirectory()) {
      walkFiles(
        root,
        (filePath) => CONTROL_FILE_EXTENSIONS.has(path.extname(filePath)),
        controlFiles,
      );
      continue;
    }
    controlFiles.push(root);
  }

  for (const filePath of controlFiles) {
    const content = fs.readFileSync(filePath, "utf8");
    if (BASH_PYTHON_HEREDOC_PATTERN.test(content)) {
      errors.push(
        `${formatPath(repoRoot, filePath)}: Bash-Python-Heredoc gefunden — auf Windows PowerShell-Here-String oder temporaere .py-Datei verwenden`,
      );
    }
  }
}

function validateSessionScorerBinding(repoRoot, errors, warnings) {
  const home = os.homedir();
  const profilePath = path.join(
    home,
    "Documents",
    "PowerShell",
    "Microsoft.PowerShell_profile.ps1",
  );
  const scorerPath = path.join(home, ".codex", "hooks", "session-scorer.ts");
  const scoresFile = path.join(home, ".codex", "session-scores.jsonl");

  if (!fs.existsSync(profilePath)) {
    errors.push("PowerShell-Profil fehlt — aktive Codex-Startlogik nicht pruefbar");
    return;
  }

  if (!fs.existsSync(scorerPath)) {
    errors.push("session-scorer.ts fehlt unter ~/.codex/hooks/");
    return;
  }

  const profileText = fs.readFileSync(profilePath, "utf8");
  const scorerText = fs.readFileSync(scorerPath, "utf8");

  if (!profileText.includes("session-scorer.ts")) {
    errors.push(
      `${formatPath(repoRoot, profilePath)}: aktiver Codex-Startpfad ruft session-scorer.ts nicht auf`,
    );
  }

  if (
    /whiteboard-insert|Insert-WhiteboardEntry|insertViaWhiteboardInsert/.test(
      scorerText,
    )
  ) {
    errors.push(
      `${formatPath(repoRoot, scorerPath)}: session-scorer.ts ist nicht side-effect-frei und haengt noch an Whiteboard-Insert`,
    );
  }

  if (!fs.existsSync(scoresFile)) {
    warnings.push(
      "~/.codex/session-scores.jsonl fehlt noch — den Scorer einmal laufen lassen oder eine Codex-Session sauber beenden.",
    );
  }
}

function validateBridgeInfrastructure(repoRoot, codexSetupDir, errors) {
  const requiredFiles = [
    "bridges/bridge-registry.json",
    "bridges/claude-delta-bridge.md",
    "bridges/claude-delta-bridge.json",
    "bridges/gemini-delta-bridge.md",
    "bridges/gemini-delta-bridge.json",
    "bridges/environment-fix-exchange-bridge.json",
    "bridges/intelligence-suggestion-exchange-bridge.json",
    "scripts/bridge-registry.mjs",
    "scripts/audit-claude-delta.mjs",
    "scripts/audit-claude-delta.sh",
    "scripts/audit-gemini-delta.mjs",
    "scripts/audit-gemini-delta.sh",
    "scripts/bootstrap-codex-setup.sh",
    "scripts/bootstrap-codex-setup.ps1",
    "scripts/bootstrap-report.mjs",
    "scripts/bootstrap-report.sh",
    "scripts/bootstrap-report.ps1",
  ];

  for (const relativePath of requiredFiles) {
    const fullPath = path.join(codexSetupDir, relativePath);
    if (!fs.existsSync(fullPath)) {
      errors.push(`codex-setup/${relativePath}: fehlt`);
    }
  }

  const registryPath = path.join(codexSetupDir, "bridges", "bridge-registry.json");
  if (!fs.existsSync(registryPath)) {
    return;
  }

  let registry;
  try {
    registry = JSON.parse(fs.readFileSync(registryPath, "utf8"));
  } catch (error) {
    errors.push(`codex-setup/bridges/bridge-registry.json: ungültiges JSON (${error.message})`);
    return;
  }

  for (const bridgeId of ["claude-delta", "gemini-delta"]) {
    const bridge = registry.bridges?.[bridgeId];
    if (!bridge) {
      errors.push(`codex-setup/bridges/bridge-registry.json: bridges.${bridgeId} fehlt`);
      continue;
    }

    for (const bridgeFile of bridge.bridge_files || []) {
      if (!fs.existsSync(path.join(repoRoot, bridgeFile))) {
        errors.push(`${bridgeFile}: in Registry referenziert, aber Datei fehlt`);
      }
    }

    if (!bridge.state_file) {
      errors.push(`codex-setup/bridges/bridge-registry.json: ${bridgeId}.state_file fehlt`);
    }
  }
}

function validateGitLockRuntime(repoRoot, codexSetupDir, errors) {
  const requiredRepoFiles = [
    "bin/git",
    "bin/git.cmd",
    "hooks/codex-git-multi-session-lock.cmd",
    "hooks/codex-git-multi-session-lock.ps1",
    "hooks/codex-git-multi-session-lock.sh",
    "hooks/codex-git-wrapper.ps1",
    "scripts/ensure-codex-git-lock-config.py",
    "scripts/test-git-multi-session-lock.ps1",
  ];

  for (const relativePath of requiredRepoFiles) {
    const fullPath = path.join(codexSetupDir, relativePath);
    if (!fs.existsSync(fullPath)) {
      errors.push(`codex-setup/${relativePath}: Git-Lock-Runtime-Datei fehlt`);
    }
  }

  const home = os.homedir();
  const configPath = path.join(home, ".codex", "config.toml");
  if (!fs.existsSync(configPath)) {
    errors.push("~/.codex/config.toml fehlt - Git-Lock-Runtime nicht prüfbar");
    return;
  }

  const configText = fs.readFileSync(configPath, "utf8");
  const normalizedConfigText = configText.replace(/\\\\/g, "\\");
  const homeBin = path.join(home, "bin");
  const homeLocalBin = path.join(home, ".local", "bin");

  if (!configText.includes("[shell_environment_policy.set]")) {
    errors.push("~/.codex/config.toml: [shell_environment_policy.set] fehlt für Git-Wrapper-PATH");
  }
  if (!normalizedConfigText.includes(homeBin)) {
    errors.push(`~/.codex/config.toml: ${homeBin} fehlt im Codex-PATH vor realem Git`);
  }
  if (!normalizedConfigText.includes(homeLocalBin)) {
    errors.push(`~/.codex/config.toml: ${homeLocalBin} fehlt im Codex-PATH vor realem Git`);
  }

  if (process.platform === "win32") {
    const activeFiles = [
      path.join(home, "bin", "git.cmd"),
      path.join(home, ".codex", "hooks", "codex-git-multi-session-lock.cmd"),
      path.join(home, ".codex", "hooks", "codex-git-multi-session-lock.ps1"),
      path.join(home, ".codex", "hooks", "codex-git-wrapper.ps1"),
    ];
    for (const filePath of activeFiles) {
      if (!fs.existsSync(filePath)) {
        errors.push(`${formatPath(repoRoot, filePath)}: aktive Git-Lock-Runtime-Datei fehlt`);
      }
    }
    if (
      !configText.includes("codex-git-multi-session-lock.cmd") &&
      !configText.includes("codex-git-multi-session-lock.ps1")
    ) {
      errors.push("~/.codex/config.toml: PreToolUse-Hook codex-git-multi-session-lock.cmd fehlt");
    }
  } else if (!configText.includes("codex-git-multi-session-lock.sh")) {
    errors.push("~/.codex/config.toml: PreToolUse-Hook codex-git-multi-session-lock.sh fehlt");
  }
}

function main() {
  const repoRoot = resolveRepoRoot(process.cwd());
  const codexSetupDir = path.join(repoRoot, "codex-setup");
  const repoSkillsDir = path.join(codexSetupDir, "skills");
  const localSkillsDir = path.join(os.homedir(), ".codex", "skills");

  const errors = [];
  const warnings = [];
  const repoSkillFiles = walkFiles(
    repoSkillsDir,
    (filePath) => path.basename(filePath) === "SKILL.md",
  );
  const localSkillFiles = walkFiles(
    localSkillsDir,
    (filePath) => path.basename(filePath) === "SKILL.md",
  );

  for (const skillFile of [...repoSkillFiles, ...localSkillFiles]) {
    validateSkillFrontmatter(repoRoot, skillFile, errors);
  }

  validatePythonHeredoc(repoRoot, codexSetupDir, errors);
  validateSessionScorerBinding(repoRoot, errors, warnings);
  validateBridgeInfrastructure(repoRoot, codexSetupDir, errors);
  validateGitLockRuntime(repoRoot, codexSetupDir, errors);

  console.log("Codex-Setup-Validierung");
  console.log(`- Repo-Skills geprueft: ${repoSkillFiles.length}`);
  console.log(`- Lokale Skills geprueft: ${localSkillFiles.length}`);
  console.log(`- Fehler: ${errors.length}`);
  console.log(`- Warnungen: ${warnings.length}`);

  if (warnings.length > 0) {
    console.log("");
    console.log("Warnungen:");
    for (const warning of warnings) {
      console.log(`- ${warning}`);
    }
  }

  if (errors.length > 0) {
    console.log("");
    console.log("Fehler:");
    for (const error of errors) {
      console.log(`- ${error}`);
    }
    process.exitCode = 1;
    return;
  }

  console.log("");
  console.log("Status: OK");
}

main();
