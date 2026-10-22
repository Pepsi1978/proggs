// Jarvis-Auftrag v1.0.1 - 22.10.2026, 20:08 Uhr
//
// Bringt OpenCode-Sitzungen, die Jarvis (Franks Sprachassistent) über den OpenLauncher gestartet hat
// (--jarvis-auftrag --cli opencode, Sol 6.1), dieselbe Anbindung wie Claude-Code-Sitzungen mit den Jarvis-Hooks:
// Rückfragen (Werkzeug question) gehen aufs Handy und Franks Antwort zurück, Zwischentexte, Bau, Commit, Push und
// Installation erscheinen im Jarvis-Chat, das Zugende als Abschlussbericht, Nachträge kommen in dieselbe Sitzung.
// Die Arbeit macht Jarvis/werkzeuge/pc-bruecke/jarvis_hook.py (dieselbe Datei wie für Claude Code); dieses Plugin
// übersetzt nur die OpenCode-Ereignisse in deren Eingabe. Ohne JARVIS_AUFTRAG_ID (jede normale Sitzung) tut es nichts.

import { spawn } from "node:child_process"
import { homedir } from "node:os"
import { join } from "node:path"

const AUFTRAG = (process.env.JARVIS_AUFTRAG_ID ?? "").trim()
const HOOK = join(homedir(), "proggs", "Jarvis", "werkzeuge", "pc-bruecke", "jarvis_hook.py")
const PYTHON = process.platform === "win32" ? "python" : "python3"

// OpenCode-Werkzeuge unter den Namen, die jarvis_hook.py von Claude Code kennt (Bau, Commit, Push, Installation, Dateien).
const WERKZEUGE = { bash: "Bash", edit: "Edit", write: "Write", multiedit: "MultiEdit", patch: "Edit", question: "AskUserQuestion", task: "Task", skill: "Skill" }

/** Startet jarvis_hook.py mit [art] und [eingabe] auf stdin; liefert Exit-Code, stdout und stderr. */
function hook(art, eingabe, merke) {
  return new Promise((fertig) => {
    let aus = ""
    let fehler = ""
    let kind
    try {
      kind = spawn(PYTHON, [HOOK, art], {
        env: { ...process.env, JARVIS_CLI: "opencode", JARVIS_ELTERN_PID: String(process.pid), PYTHONIOENCODING: "utf-8" },
        stdio: ["pipe", "pipe", "pipe"],
        windowsHide: true,
      })
    } catch {
      fertig({ code: -1, aus, fehler })
      return
    }
    merke?.(kind)
    kind.stdout.on("data", (d) => (aus += d.toString("utf8")))
    kind.stderr.on("data", (d) => (fehler += d.toString("utf8")))
    kind.on("error", () => fertig({ code: -1, aus, fehler }))
    kind.on("close", (code) => fertig({ code: code ?? -1, aus, fehler }))
    kind.stdin.end(JSON.stringify(eingabe ?? {}), "utf8")
  })
}

function json(text) {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function werkzeugEingabe(werkzeug, args) {
  const a = args ?? {}
  if (werkzeug === "Edit" || werkzeug === "Write" || werkzeug === "MultiEdit") return { file_path: a.filePath ?? a.file_path ?? a.path ?? "" }
  if (werkzeug === "Bash") return { command: a.command ?? "" }
  if (werkzeug === "Skill") return { skill: a.name ?? a.skill ?? "" }
  if (werkzeug === "Task") return { description: a.description ?? "", subagent_type: a.subagent_type ?? a.agent ?? "" }
  return a
}

/** OpenCode-Fragen im Format von AskUserQuestion (multiple heißt dort multiSelect). */
function claudeFragen(fragen) {
  return (fragen ?? []).map((f) => ({
    question: f.question ?? "",
    header: f.header ?? "",
    options: (f.options ?? []).map((o) => ({ label: o.label ?? "", description: o.description ?? "" })),
    multiSelect: Boolean(f.multiple),
  }))
}

export const JarvisAuftragPlugin = async ({ client }) => {
  if (!AUFTRAG) return {}

  let hauptSitzung = ""          // die Sitzung des Auftrags (die erste ohne Elternsitzung)
  const texte = new Map()        // fertige Zwischentexte der KI seit dem letzten Hook-Aufruf (Teil-ID -> Text)
  const gemeldet = new Set()     // Teil-IDs, die schon an einen Hook gingen
  const fragen = new Map()       // laufende Rückfragen (requestID -> wartender Hook), jede nur einmal ans Handy
  let selbstGeweckt = false      // das nächste Idle folgt auf einen eigenen Prompt (Antwort oder Nachtrag)
  let warter = null

  const log = (level, message) => Promise.resolve(client.app?.log?.({ body: { service: "jarvis-auftrag", level, message } })).catch(() => undefined)

  /** Gehört [sessionID] zum Auftrag? Ereignisse legen die Sitzung nie fest, nur merkeHaupt. */
  function istHaupt(sessionID) {
    return Boolean(sessionID) && sessionID === hauptSitzung
  }

  /**
   * Legt die Auftragssitzung fest: die erste Sitzung ohne Elternsitzung, die eine echte Nachricht bekommt (der Auftrag per
   * --prompt) oder ein Werkzeug ausführt. Früher reichte das erste Ereignis; das kam am 22.10.2026 von OpenCodes eigener
   * Hilfssitzung für den Titel, und alle Werkzeuge, Rückfragen und das Zugende der echten Sitzung galten danach als fremd.
   */
  async function merkeHaupt(sessionID, agent) {
    if (!sessionID || hauptSitzung) return istHaupt(sessionID)
    if (agent && ["title", "summary", "compaction"].includes(agent)) return false
    try {
      const antwort = await client.session.get({ path: { id: sessionID } })
      if (!antwort?.data || antwort.data.parentID) return false
    } catch {
      return false
    }
    if (hauptSitzung) return istHaupt(sessionID)
    hauptSitzung = sessionID
    await log("info", `Jarvis-Auftrag ${AUFTRAG}: Auftragssitzung ${sessionID}`)
    starteWarter()
    return true
  }

  function abholen() {
    const liste = [...texte.values()]
    for (const id of texte.keys()) gemeldet.add(id)
    texte.clear()
    return liste
  }

  async function weiter(sessionID, text) {
    selbstGeweckt = true
    try {
      await client.session.promptAsync({ path: { id: sessionID }, body: { parts: [{ type: "text", text }] } })
    } catch (fehler) {
      selbstGeweckt = false
      await log("error", `Nachricht an die Sitzung fehlgeschlagen: ${fehler?.message ?? fehler}`)
    }
  }

  // Wie der Claude-Hook „warte“ (asyncRewake): wartet am Relay auf Nachträge und gibt sie in die Sitzung, auch wenn sie
  // gerade nichts tut. Endet mit dem OpenCode-Prozess (JARVIS_ELTERN_PID) oder nach zwölf Stunden.
  function starteWarter() {
    if (warter || !hauptSitzung) return
    warter = (async () => {
      for (;;) {
        const { code, fehler } = await hook("warte", { hook_event_name: "SessionStart" })
        if (code === 2 && fehler.trim()) {
          await weiter(hauptSitzung, fehler.trim())
          continue
        }
        // 0: ein anderer Warter läuft schon, die Zeit ist um oder Python fehlt. Kurz warten statt im Kreis zu starten.
        await new Promise((r) => setTimeout(r, 60_000))
      }
    })()
  }

  async function frage(info) {
    if (!info?.id || fragen.has(info.id) || !(istHaupt(info.sessionID))) return
    fragen.set(info.id, null)
    const liste = claudeFragen(info.questions)
    const { aus } = await hook("frage", {
      tool_name: "AskUserQuestion",
      tool_use_id: info.tool?.callID ?? info.id,
      tool_input: { questions: liste },
      texte: abholen(),
    }, (kind) => fragen.set(info.id, kind))
    // Inzwischen am Rechner beantwortet: nichts mehr übergeben (post meldet den Ausgang an Jarvis).
    if (!fragen.has(info.id)) return
    fragen.delete(info.id)
    const antworten = json(aus)?.hookSpecificOutput?.updatedInput?.answers
    // Ohne Antwort (abgelaufen oder in Jarvis „am Rechner beantworten“) bleibt der Dialog in OpenCode offen.
    if (!antworten || typeof antworten !== "object") return
    const answers = liste.map((f, i) => {
      const wert = String(antworten[f.question] ?? Object.values(antworten)[i] ?? "").trim()
      if (!wert) return []
      return f.multiSelect ? wert.split(/,\s*/).filter(Boolean) : [wert]
    })
    try {
      // Der Plugin-Client kennt question.reply noch nicht: derselbe Endpunkt über seinen eigenen Transport.
      await client._client.post({
        url: "/question/{requestID}/reply",
        path: { requestID: info.id },
        body: { answers },
        headers: { "Content-Type": "application/json" },
      })
    } catch (fehler) {
      await log("error", `Antwort aus Jarvis nicht übergeben: ${fehler?.message ?? fehler}`)
    }
  }

  async function letzteNachricht(sessionID) {
    try {
      const antwort = await client.session.messages({ path: { id: sessionID } })
      const liste = antwort?.data ?? []
      for (let i = liste.length - 1; i >= 0; i--) {
        const m = liste[i]
        if (m?.info?.role !== "assistant") continue
        const text = (m.parts ?? []).filter((p) => p.type === "text" && !p.synthetic).map((p) => p.text ?? "").join("\n").trim()
        if (text) return text
      }
    } catch {}
    return ""
  }

  async function zugende(sessionID) {
    if (!(istHaupt(sessionID))) return
    if (selbstGeweckt) {
      // Das Idle direkt nach einem eigenen Prompt kann vor dessen Start kommen: einmal auslassen, sonst doppelt.
      selbstGeweckt = false
      const status = await client.session.status?.().catch(() => null)
      if (status?.data?.[sessionID]?.type === "busy") return
    }
    const letzte = await letzteNachricht(sessionID)
    const { aus } = await hook("stop", { last_assistant_message: letzte, stop_hook_active: false, texte: abholen() })
    const ergebnis = json(aus)
    if (ergebnis?.decision === "block" && ergebnis.reason) await weiter(sessionID, ergebnis.reason)
  }

  await log("info", `Jarvis-Auftrag ${AUFTRAG}: Plugin aktiv`)

  return {
    event: async ({ event }) => {
      const p = event?.properties ?? {}
      switch (event?.type) {
        case "message.part.updated": {
          const teil = p.part
          if (teil?.type === "text" && !teil.synthetic && teil.time?.end && teil.text?.trim() && !gemeldet.has(teil.id)) {
            if (istHaupt(teil.sessionID)) texte.set(teil.id, teil.text)
          }
          break
        }
        case "question.asked":
          // Nicht abwarten: Die Antwort aus Jarvis kann Minuten dauern, andere Ereignisse laufen weiter.
          void frage(p)
          break
        case "question.replied":
        case "question.rejected": {
          // Am Rechner beantwortet oder abgebrochen: Der Hook wartet nicht länger auf Jarvis.
          const kind = fragen.get(p.requestID)
          fragen.delete(p.requestID)
          try {
            kind?.kill()
          } catch {}
          break
        }
        case "session.idle":
          void zugende(p.sessionID)
          break
      }
    },
    // Die Nachricht mit dem Auftrag (--prompt) legt die Auftragssitzung fest.
    "chat.message": async (input) => {
      await merkeHaupt(input?.sessionID, input?.agent)
    },
    "tool.execute.before": async (input, output) => {
      if (!(await merkeHaupt(input.sessionID))) return
      const werkzeug = WERKZEUGE[input.tool] ?? input.tool
      void hook("puls", { tool_name: werkzeug, tool_input: werkzeugEingabe(werkzeug, output?.args), texte: abholen() })
    },
    "tool.execute.after": async (input, output) => {
      if (!(istHaupt(input.sessionID))) return
      const werkzeug = WERKZEUGE[input.tool] ?? input.tool
      const args = input.args ?? output?.metadata?.input ?? {}
      void hook("post", {
        hook_event_name: output?.metadata?.exit && output.metadata.exit !== 0 ? "PostToolUseFailure" : "PostToolUse",
        tool_name: werkzeug,
        tool_use_id: input.callID,
        tool_input: werkzeugEingabe(werkzeug, args),
        tool_response: typeof output?.output === "string" ? output.output.slice(-4000) : "",
      })
    },
  }
}
