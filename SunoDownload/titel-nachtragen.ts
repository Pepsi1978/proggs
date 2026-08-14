/**
 * Holt die Titel der Songs nach, die in der Liste ohne Titel stehen, und benennt die
 * zugehörigen Dateien um. Die führende Nummer bleibt dabei unverändert — die
 * Reihenfolge ältester -> neuester Song bleibt also exakt erhalten.
 *
 * Der Titel kommt von der öffentlichen Songseite (og:title), es ist keine Anmeldung nötig.
 *
 * Usage:  node titel-nachtragen.ts [list.json] [folder]
 */

import { existsSync, readFileSync, writeFileSync, readdirSync, renameSync, mkdirSync, appendFileSync } from 'node:fs';
import { join, dirname, isAbsolute } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const LOG_DIR = join(HERE, 'logs');
const DEFAULT_TARGET = 'C:\\Sono Backup';
const PAUSE_MS = 700;

type Song = { id: string; title: string; created_at: string | null; audio_url: string };

mkdirSync(LOG_DIR, { recursive: true });
const LOG_FILE = join(LOG_DIR, 'titel-nachtragen.jsonl');

function log(level: string, msg: string, ctx: Record<string, unknown> = {}): void {
  try {
    appendFileSync(LOG_FILE, JSON.stringify({ ts: new Date().toISOString(), level, msg, ctx }) + '\n', 'utf8');
  } catch {
    /* logging must never kill the run */
  }
}

function sanitize(name: string): string {
  const withoutControls = [...name].filter((c) => (c.codePointAt(0) ?? 0) >= 32).join('');
  const cleaned = withoutControls
    .replace(/[<>:"/\\|?*]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/[. ]+$/, '');
  return (cleaned || 'Ohne Titel').slice(0, 120);
}

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** Reads the title from the public song page. Retries politely when Suno throttles. */
async function fetchTitle(id: string): Promise<string | null> {
  for (let attempt = 1; attempt <= 4; attempt++) {
    try {
      const response = await fetch(`https://suno.com/song/${id}`, { signal: AbortSignal.timeout(30000) });
      if (response.status === 429) {
        await wait(attempt * 8000);
        continue;
      }
      if (!response.ok) return null;

      const html = await response.text();
      const og = /<meta property="og:title" content="([^"]*)"/.exec(html);
      if (og?.[1]) return decode(og[1]).trim();

      const title = /<title>([^<]*)<\/title>/.exec(html);
      if (title?.[1]) return decode(title[1]).replace(/\s*\|\s*Suno.*$/i, '').trim();
      return null;
    } catch {
      await wait(attempt * 3000);
    }
  }
  return null;
}

function decode(text: string): string {
  return text
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&#x27;/g, "'");
}

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const listArg = args.find((a) => a.toLowerCase().endsWith('.json'));
  const target = args.find((a) => !a.toLowerCase().endsWith('.json')) ?? DEFAULT_TARGET;
  const listPath = listArg
    ? (isAbsolute(listArg) ? listArg : join(process.cwd(), listArg))
    : join(target, 'suno-liste.json');

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  Fehlende Songtitel werden nachgetragen');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('');

  if (!existsSync(listPath)) {
    console.log(`❗ Songliste nicht gefunden: ${listPath}`);
    process.exit(1);
  }

  const songs = JSON.parse(readFileSync(listPath, 'utf8')) as Song[];
  const ordered = [...songs].sort((a, b) => {
    const ta = a.created_at ? Date.parse(a.created_at) : Number.MAX_SAFE_INTEGER;
    const tb = b.created_at ? Date.parse(b.created_at) : Number.MAX_SAFE_INTEGER;
    return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
  });

  const missing = ordered.map((s, i) => ({ song: s, nummer: i + 1 })).filter((e) => !e.song.title);
  console.log(`  ${missing.length} von ${ordered.length} Songs haben keinen Titel.`);
  console.log('');

  if (missing.length === 0) {
    console.log('  Nichts zu tun.');
    return;
  }

  // Keep a copy of the original list before touching it.
  const backup = listPath.replace(/\.json$/i, `-vorher.json`);
  if (!existsSync(backup)) writeFileSync(backup, JSON.stringify(songs, null, 2), 'utf8');

  const files = readdirSync(target).filter((n) => n.toLowerCase().endsWith('.mp3'));
  const byNumber = new Map<number, string>();
  for (const name of files) {
    const m = /^(\d+)\s*-/.exec(name);
    if (m) byNumber.set(Number(m[1]), name);
  }
  const taken = new Set(files.map((f) => f.toLowerCase()));

  let found = 0;
  let renamed = 0;

  for (let i = 0; i < missing.length; i++) {
    const { song, nummer } = missing[i];
    process.stdout.write(`[${String(i + 1).padStart(3, ' ')}/${missing.length}] Nr. ${nummer} … `);

    const title = await fetchTitle(song.id);
    if (!title) {
      console.log('kein Titel gefunden');
      log('probe', 'Titel nicht ermittelbar', { id: song.id, nummer });
      await wait(PAUSE_MS);
      continue;
    }

    song.title = title;
    found++;

    const oldName = byNumber.get(nummer);
    if (!oldName) {
      console.log(`"${title}" (keine Datei zu Nr. ${nummer})`);
      await wait(PAUSE_MS);
      continue;
    }

    const width = String(ordered.length).length;
    let base = `${String(nummer).padStart(Math.max(3, width), '0')} - ${sanitize(title)}`;
    if (taken.has(`${base}.mp3`.toLowerCase()) && `${base}.mp3` !== oldName) {
      let n = 2;
      while (taken.has(`${base} (${n}).mp3`.toLowerCase())) n++;
      base = `${base} (${n})`;
    }
    const newName = `${base}.mp3`;

    if (newName !== oldName) {
      try {
        renameSync(join(target, oldName), join(target, newName));
        taken.delete(oldName.toLowerCase());
        taken.add(newName.toLowerCase());
        renamed++;
        console.log(`"${title}" ✅`);
        log('info', 'Datei umbenannt', { von: oldName, nach: newName, id: song.id });
      } catch (error) {
        console.log(`"${title}" — Umbenennen fehlgeschlagen (${(error as Error).message})`);
        log('error', 'Umbenennen fehlgeschlagen', { oldName, newName, error: String(error) });
      }
    } else {
      console.log(`"${title}" (Name schon richtig)`);
    }

    await wait(PAUSE_MS);
  }

  writeFileSync(listPath, JSON.stringify(songs, null, 2), 'utf8');

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${found} Titel gefunden · ${renamed} Dateien umbenannt`);
  console.log(`  Die Songliste wurde aktualisiert (Sicherung: ${backup})`);
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('');
}

main().catch((error) => {
  console.error(`❗ Abbruch: ${error.message}`);
  log('fatal', 'Abbruch', { error: String(error?.stack ?? error) });
  process.exit(1);
});
