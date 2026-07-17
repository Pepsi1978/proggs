import { appendFileSync, mkdirSync } from "node:fs"
import { dirname } from "node:path"

const [, , markerPath, event] = process.argv
if (!markerPath || !event) process.exit(2)
mkdirSync(dirname(markerPath), { recursive: true })
appendFileSync(markerPath, `${event}\n`, "utf8")
