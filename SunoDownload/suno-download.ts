/**
 * SunoDownload — downloads every song of your own Suno library as MP3.
 *
 * Reads the song list produced by `bibliothek-holen.js` (pasted into the console of
 * the user's own, already signed-in Chrome) and downloads every track, numbered from
 * oldest to newest:
 *
 *     001 - My first song.mp3
 *     002 - Another song.mp3
 *
 * Usage:  node suno-download.ts [list.json] [target folder]
 */

import {
  createWriteStream,
  existsSync,
  mkdirSync,
  statSync,
  appendFileSync,
  renameSync,
  readFileSync,
  readdirSync,
} from 'node:fs';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { join, dirname, isAbsolute } from 'node:path';
import { fileURLToPath } from 'node:url';
import { homedir } from 'node:os';

const HERE = dirname(fileURLToPath(import.meta.url));
const LOG_DIR = join(HERE, 'logs');
const DOWNLOADS = join(homedir(), 'Downloads');
const DEFAULT_TARGET = 'C:\\Sono Backup';

const pkg = JSON.parse(readFileSync(join(HERE, 'package.json'), 'utf8')) as {
  version: string;
  versionBumpedAt: string;
};
const VERSION = `Version ${pkg.version} (${pkg.versionBumpedAt})`;

type Song = {
  id: string;
  title: string;
  created_at: string | null;
  audio_url: string;
};

// ---------------------------------------------------------------- observability

mkdirSync(LOG_DIR, { recursive: true });
const LOG_FILE = join(LOG_DIR, 'suno-download.jsonl');

function log(level: string, msg: string, ctx: Record<string, unknown> = {}): void {
  try {
    appendFileSync(LOG_FILE, JSON.stringify({ ts: new Date().toISOString(), level, msg, ctx }) + '\n', 'utf8');
  } catch {
    /* logging must never kill the run */
  }
}

/** Logic probe: reports a broken assumption instead of failing silently. */
function probe(condition: boolean, msg: string, ctx: Record<string, unknown> = {}): void {
  if (!condition) {
    console.warn(`   ⚠️  ${msg}`);
    log('probe', msg, ctx);
  }
}

// ---------------------------------------------------------------- list discovery

/** Finds the newest suno-liste*.json — given explicitly, in Downloads, or next to the script. */
function findList(explicit: string | undefined): string | null {
  if (explicit) {
    const path = isAbsolute(explicit) ? explicit : join(process.cwd(), explicit);
    if (existsSync(path)) return path;
    console.error(`❗ Diese Datei gibt es nicht: ${path}`);
    return null;
  }

  const candidates: Array<{ path: string; mtime: number }> = [];
  for (const dir of [DOWNLOADS, HERE]) {
    if (!existsSync(dir)) continue;
    for (const name of readdirSync(dir)) {
      if (/^suno-liste.*\.json$/i.test(name)) {
        const path = join(dir, name);
        candidates.push({ path, mtime: statSync(path).mtimeMs });
      }
    }
  }
  candidates.sort((a, b) => b.mtime - a.mtime);
  return candidates.length ? candidates[0].path : null;
}

function readList(path: string): Song[] {
  const raw = JSON.parse(readFileSync(path, 'utf8'));
  const list = Array.isArray(raw) ? raw : Array.isArray(raw?.songs) ? raw.songs : [];

  const songs: Song[] = [];
  for (const entry of list) {
    if (entry && typeof entry.id === 'string' && typeof entry.audio_url === 'string') {
      songs.push({
        id: entry.id,
        title: typeof entry.title === 'string' ? entry.title : '',
        created_at: typeof entry.created_at === 'string' ? entry.created_at : null,
        audio_url: entry.audio_url,
      });
    }
  }
  probe(songs.length === list.length, `${list.length - songs.length} Einträge der Liste waren unbrauchbar`, { path });
  return songs;
}

// ---------------------------------------------------------------- downloading

function sanitize(name: string): string {
  const withoutControls = [...name].filter((c) => (c.codePointAt(0) ?? 0) >= 32).join('');
  const cleaned = withoutControls
    .replace(/[<>:"/\\|?*]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/[. ]+$/, '');
  return (cleaned || 'Ohne Titel').slice(0, 120);
}

async function download(url: string, target: string): Promise<number> {
  let lastError: Error | null = null;

  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(180000) });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      if (!response.body) throw new Error('Leere Antwort');

      const temp = `${target}.teil`;
      await pipeline(Readable.fromWeb(response.body as any), createWriteStream(temp));

      const size = statSync(temp).size;
      if (size < 1024) throw new Error(`Datei zu klein (${size} Byte)`);

      renameSync(temp, target); // only a complete file ever gets the final name
      return size;
    } catch (error) {
      lastError = error as Error;
      if (attempt < 3) await new Promise((r) => setTimeout(r, attempt * 2000));
    }
  }
  throw lastError ?? new Error('Unbekannter Fehler');
}

// ---------------------------------------------------------------- main

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const listArg = args.find((a) => a.toLowerCase().endsWith('.json'));
  const targetArg = args.find((a) => !a.toLowerCase().endsWith('.json'));
  const target = targetArg ?? DEFAULT_TARGET;

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  SunoDownload — lädt deine komplette Suno-Bibliothek als MP3');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  ${VERSION}`);
  console.log('');

  const listPath = findList(listArg);
  if (!listPath) {
    console.log('  Es wurde keine Songliste gefunden.');
    console.log('');
    console.log('  So erzeugst du sie — einmalig, dauert eine halbe Minute:');
    console.log('   1. Öffne in deinem normalen Chrome die Seite  https://suno.com/me');
    console.log('   2. Drücke F12 und klicke oben auf den Reiter "Console" / "Konsole"');
    console.log('   3. Füge das Skript aus  bibliothek-holen.js  ein und drücke Enter');
    console.log('      (fragt Chrome nach Erlaubnis: "allow pasting" eintippen, Enter)');
    console.log('   4. Es landet eine Datei "suno-liste.json" in deinem Downloads-Ordner');
    console.log('   5. Danach dieses Programm einfach nochmal starten');
    console.log('');
    process.exit(1);
  }

  const songs = readList(listPath);
  console.log(`  Songliste  : ${listPath}`);
  console.log(`  Zielordner : ${target}`);
  console.log(`  Protokoll  : ${LOG_FILE}`);
  console.log('');

  if (songs.length === 0) {
    console.log('❗ Die Songliste ist leer. Bitte den Schritt im Browser wiederholen.');
    process.exit(1);
  }

  mkdirSync(target, { recursive: true });

  // Oldest first — number 001 is the oldest song.
  const ordered = [...songs].sort((a, b) => {
    const ta = a.created_at ? Date.parse(a.created_at) : Number.MAX_SAFE_INTEGER;
    const tb = b.created_at ? Date.parse(b.created_at) : Number.MAX_SAFE_INTEGER;
    return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
  });

  const width = Math.max(3, String(ordered.length).length);
  console.log(`⬇️  ${ordered.length} Songs werden geladen (ältester zuerst).`);
  console.log('');

  let saved = 0;
  let skipped = 0;
  const failed: Array<{ name: string; reason: string }> = [];
  const usedNames = new Set<string>();

  for (let i = 0; i < ordered.length; i++) {
    const song = ordered[i];
    const number = String(i + 1).padStart(width, '0');

    let base = `${number} - ${sanitize(song.title)}`;
    if (usedNames.has(base.toLowerCase())) {
      let n = 2;
      while (usedNames.has(`${base} (${n})`.toLowerCase())) n++;
      base = `${base} (${n})`;
    }
    usedNames.add(base.toLowerCase());

    const fileName = `${base}.mp3`;
    const fullPath = join(target, fileName);
    const label = `[${String(i + 1).padStart(width, ' ')}/${ordered.length}] ${fileName}`;

    if (existsSync(fullPath) && statSync(fullPath).size > 1024) {
      console.log(`${label}  — schon vorhanden, übersprungen`);
      skipped++;
      continue;
    }

    process.stdout.write(`${label}  … `);
    try {
      const size = await download(song.audio_url, fullPath);
      console.log(`${(size / 1024 / 1024).toFixed(1)} MB ✅`);
      log('info', 'Song geladen', { id: song.id, fileName, size });
      saved++;
    } catch (error) {
      const reason = (error as Error).message;
      console.log(`FEHLER (${reason}) ❗`);
      log('error', 'Download fehlgeschlagen', { id: song.id, fileName, reason, url: song.audio_url });
      failed.push({ name: fileName, reason });
    }
  }

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${saved} geladen · ${skipped} schon vorhanden · ${failed.length} fehlgeschlagen`);
  console.log(`  Ordner:  ${target}`);
  console.log('════════════════════════════════════════════════════════════════════════════════');

  if (failed.length) {
    console.log('');
    console.log('  Diese Songs kamen nicht durch:');
    for (const f of failed) console.log(`   ❗ ${f.name} — ${f.reason}`);
    console.log('  Einfach nochmal starten: Fertiges wird übersprungen, nur der Rest wird geholt.');
  }
  console.log('');
}

main().catch((error) => {
  console.error('');
  console.error(`❗ Abbruch: ${error.message}`);
  log('fatal', 'Abbruch', { error: String(error?.stack ?? error) });
  process.exit(1);
});
