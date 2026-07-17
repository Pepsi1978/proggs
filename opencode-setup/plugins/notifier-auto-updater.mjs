import { spawn } from "node:child_process"
import { access, mkdir, mkdtemp, open, readFile, rename, rm, stat, writeFile, appendFile } from "node:fs/promises"
import { homedir, tmpdir } from "node:os"
import { dirname, join, resolve } from "node:path"
import { fileURLToPath } from "node:url"

const PACKAGE_NAME = "@mohak34/opencode-notifier"
const DAY_MS = 24 * 60 * 60 * 1000
const FAILURE_RETRY_MS = 60 * 60 * 1000
const STALE_LOCK_MS = 15 * 60 * 1000
const VERSION_PATTERN = /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/

const scriptDirectory = dirname(fileURLToPath(import.meta.url))

export function compareVersions(left, right) {
  const parse = (value) => value.split("-", 1)[0].split(".").map(Number)
  const a = parse(left)
  const b = parse(right)
  for (let index = 0; index < 3; index += 1) {
    if (a[index] !== b[index]) return a[index] > b[index] ? 1 : -1
  }
  if (left === right) return 0
  return left.includes("-") ? -1 : 1
}

export function shouldDeferUpdate(state, installedVersion, now = Date.now()) {
  const nextCheck = Date.parse(state?.nextCheckNotBeforeUtc ?? "")
  return Number.isFinite(nextCheck)
    && nextCheck > now
    && state?.installedVersion === installedVersion
}

async function readJson(path) {
  try {
    return JSON.parse(await readFile(path, "utf8"))
  } catch {
    return null
  }
}

async function writeAtomicJson(path, value) {
  const temporary = `${path}.${process.pid}.${Date.now()}.tmp`
  await writeFile(temporary, `${JSON.stringify(value, null, 2)}\n`, "utf8")
  await rename(temporary, path)
}

async function appendLog(configDirectory, message) {
  try {
    await appendFile(
      join(configDirectory, "opencode-notifier-update.log"),
      `${new Date().toISOString()} ${message}\n`,
      "utf8",
    )
  } catch {
    // State JSON remains the primary diagnostic channel if logging itself fails.
  }
}

function runProcess(command, args, options = {}) {
  return new Promise((resolvePromise, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd,
      env: options.env ?? process.env,
      windowsHide: true,
      shell: options.shell ?? false,
      stdio: ["ignore", "pipe", "pipe"],
    })
    let stdout = ""
    let stderr = ""
    let settled = false
    const timeout = setTimeout(() => {
      child.kill()
      if (!settled) {
        settled = true
        reject(new Error(`${command} timed out after ${options.timeoutMs ?? 120000} ms`))
      }
    }, options.timeoutMs ?? 120000)

    child.stdout.on("data", (chunk) => { stdout += chunk.toString() })
    child.stderr.on("data", (chunk) => { stderr += chunk.toString() })
    child.on("error", (error) => {
      clearTimeout(timeout)
      if (!settled) {
        settled = true
        reject(error)
      }
    })
    child.on("close", (code) => {
      clearTimeout(timeout)
      if (settled) return
      settled = true
      if (code === 0) {
        resolvePromise({ stdout: stdout.trim(), stderr: stderr.trim() })
      } else {
        reject(new Error(`${command} exited with ${code}: ${stderr.trim() || stdout.trim()}`))
      }
    })
  })
}

let npmInvocationPromise = null

async function resolveNpmInvocation() {
  if (process.platform !== "win32") return { command: "npm", prefix: [] }
  const result = await runProcess("where.exe", ["npm.cmd"], { timeoutMs: 10000 })
  for (const commandPath of result.stdout.split(/\r?\n/).filter(Boolean)) {
    const cliPath = join(dirname(commandPath), "node_modules", "npm", "bin", "npm-cli.js")
    try {
      await access(cliPath)
      return { command: process.execPath, prefix: [cliPath] }
    } catch {
      // Continue to the next PATH result; no shell fallback is allowed.
    }
  }
  throw new Error("npm-cli.js could not be resolved safely")
}

async function runNpm(args, options = {}) {
  npmInvocationPromise ??= resolveNpmInvocation()
  const invocation = await npmInvocationPromise
  return runProcess(invocation.command, [...invocation.prefix, ...args], options)
}

async function readInstalledVersion(configDirectory) {
  const packageJson = join(configDirectory, "node_modules", "@mohak34", "opencode-notifier", "package.json")
  const value = await readJson(packageJson)
  return typeof value?.version === "string" ? value.version : null
}

async function acquireLock(lockPath, now) {
  const tryOpen = () => open(lockPath, "wx")
  try {
    return await tryOpen()
  } catch (error) {
    if (error?.code !== "EEXIST") throw error
    try {
      const lockStat = await stat(lockPath)
      if (now - lockStat.mtimeMs <= STALE_LOCK_MS) return null
      await rm(lockPath, { force: true })
      return await tryOpen()
    } catch (retryError) {
      if (retryError?.code === "EEXIST") return null
      throw retryError
    }
  }
}

async function resolveCandidateEntry(stagingDirectory) {
  const packageDirectory = join(stagingDirectory, "node_modules", "@mohak34", "opencode-notifier")
  const packageJson = await readJson(join(packageDirectory, "package.json"))
  if (!packageJson || typeof packageJson.main !== "string") {
    throw new Error("Notifier candidate has no package main entry")
  }
  const candidateEntry = resolve(packageDirectory, packageJson.main)
  const packageRoot = `${resolve(packageDirectory)}${process.platform === "win32" ? "\\" : "/"}`
  if (!candidateEntry.startsWith(packageRoot)) {
    throw new Error("Notifier candidate main entry escapes its package directory")
  }
  return candidateEntry
}

async function validateCandidate(candidateEntry, stagingDirectory) {
  const contractScript = join(scriptDirectory, "notifier-candidate-contract.mjs")
  const guardScript = join(scriptDirectory, "notifier-completion-guard.js")
  await runProcess(process.execPath, [
    contractScript,
    "--candidate", candidateEntry,
    "--guard", guardScript,
    "--work-dir", join(stagingDirectory, "contract"),
  ], {
    timeoutMs: 30000,
    env: { ...process.env, OPENCODE_CLIENT: "cli" },
  })
}

function stateValue(status, message, installedVersion, candidateVersion, now, retryMs = DAY_MS) {
  return {
    schemaVersion: 1,
    status,
    message,
    installedVersion,
    candidateVersion,
    lastCheckedAtUtc: new Date(now).toISOString(),
    nextCheckNotBeforeUtc: new Date(now + retryMs).toISOString(),
  }
}

export async function runNotifierUpdate(options = {}) {
  const configDirectory = resolve(options.configDirectory ?? join(homedir(), ".config", "opencode"))
  const statePath = join(configDirectory, "opencode-notifier-update-state.json")
  const lockPath = join(configDirectory, "opencode-notifier-update.lock")
  const now = options.now ?? Date.now()
  await mkdir(configDirectory, { recursive: true })

  const lock = await acquireLock(lockPath, now)
  if (!lock) return { status: "busy", message: "Another notifier update check is running." }

  let stagingDirectory = null
  let installedVersion = await readInstalledVersion(configDirectory)
  let candidateVersion = options.candidateVersion ?? null
  let promotionStarted = false
  try {
    const previousState = await readJson(statePath)
    if (!options.force && shouldDeferUpdate(previousState, installedVersion, now)) {
      return { status: "deferred", installedVersion }
    }

    if (!candidateVersion) {
      const result = await runNpm(["view", PACKAGE_NAME, "dist-tags.latest", "--json"], { timeoutMs: 60000 })
      candidateVersion = JSON.parse(result.stdout)
    }
    if (typeof candidateVersion !== "string" || !VERSION_PATTERN.test(candidateVersion)) {
      throw new Error(`Invalid notifier version from npm: ${String(candidateVersion)}`)
    }

    if (installedVersion && compareVersions(candidateVersion, installedVersion) < 0) {
      const state = stateValue("ahead", `Installed notifier ${installedVersion} is newer than npm latest ${candidateVersion}.`, installedVersion, candidateVersion, now)
      await writeAtomicJson(statePath, state)
      return state
    }
    if (installedVersion === candidateVersion && !options.forceReinstall) {
      const state = stateValue("up-to-date", `Notifier ${installedVersion} is current.`, installedVersion, candidateVersion, now)
      await writeAtomicJson(statePath, state)
      return state
    }

    stagingDirectory = await mkdtemp(join(tmpdir(), "opencode-notifier-update-"))
    await runNpm([
      "install", "--prefix", stagingDirectory, "--ignore-scripts", "--no-audit", "--no-fund",
      `${PACKAGE_NAME}@${candidateVersion}`,
    ], { timeoutMs: 180000 })
    const candidateEntry = await resolveCandidateEntry(stagingDirectory)
    await validateCandidate(candidateEntry, stagingDirectory)

    promotionStarted = true
    await runNpm([
      "install", "--prefix", configDirectory, "--save-exact", "--ignore-scripts", "--no-audit", "--no-fund",
      `${PACKAGE_NAME}@${candidateVersion}`,
    ], { timeoutMs: 180000 })
    const promotedVersion = await readInstalledVersion(configDirectory)
    if (promotedVersion !== candidateVersion) {
      throw new Error(`Notifier promotion mismatch: expected ${candidateVersion}, found ${promotedVersion}`)
    }
    installedVersion = promotedVersion
    const state = stateValue(
      "installed-restart-required",
      `Notifier ${candidateVersion} passed the alert contract and is active for new OpenCode sessions.`,
      installedVersion,
      candidateVersion,
      now,
    )
    await writeAtomicJson(statePath, state)
    await appendLog(configDirectory, state.message)
    return state
  } catch (error) {
    let rollback = "not needed"
    if (promotionStarted && installedVersion && VERSION_PATTERN.test(installedVersion)) {
      try {
        await runNpm([
          "install", "--prefix", configDirectory, "--save-exact", "--ignore-scripts", "--no-audit", "--no-fund",
          `${PACKAGE_NAME}@${installedVersion}`,
        ], { timeoutMs: 180000 })
        rollback = await readInstalledVersion(configDirectory) === installedVersion ? "succeeded" : "verification failed"
      } catch (rollbackError) {
        rollback = `failed: ${rollbackError.message}`
      }
    }
    const message = `Notifier update rejected: ${error.message}; rollback ${rollback}.`
    const state = stateValue("failed", message, await readInstalledVersion(configDirectory), candidateVersion, now, FAILURE_RETRY_MS)
    await writeAtomicJson(statePath, state)
    await appendLog(configDirectory, message)
    throw error
  } finally {
    if (stagingDirectory) await rm(stagingDirectory, { recursive: true, force: true })
    await lock.close()
    await rm(lockPath, { force: true })
  }
}

function parseArguments(argv) {
  const options = {}
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index]
    if (value === "--config-dir") options.configDirectory = argv[++index]
    else if (value === "--candidate-version") options.candidateVersion = argv[++index]
    else if (value === "--force") options.force = true
    else if (value === "--force-reinstall") options.forceReinstall = true
    else if (value === "--verbose") options.verbose = true
    else throw new Error(`Unknown argument: ${value}`)
  }
  return options
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const options = parseArguments(process.argv.slice(2))
    const result = await runNotifierUpdate(options)
    if (options.verbose) console.log(JSON.stringify(result))
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
