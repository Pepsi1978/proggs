/**
 * SunoDownload — downloads every song of your own Suno library as MP3.
 *
 * Opens a real Chrome window with its own profile, waits until you are logged in
 * at suno.com, collects the whole library (network sniffing + API pagination +
 * lazy-load scrolling) and writes the files numbered from oldest to newest:
 *
 *     001 - My first song.mp3
 *     002 - Another song.mp3
 *
 * Usage:  node suno-download.ts [target folder]
 */

import { chromium } from 'playwright-core';
import type { BrowserContext, Page } from 'playwright-core';
import {
  createWriteStream,
  existsSync,
  mkdirSync,
  statSync,
  appendFileSync,
  renameSync,
  readFileSync,
} from 'node:fs';
import { readdir } from 'node:fs/promises';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { homedir } from 'node:os';

const HERE = dirname(fileURLToPath(import.meta.url));
const PROFILE_DIR = join(HERE, '.browser-profil');
const LOG_DIR = join(HERE, 'logs');
const DEFAULT_TARGET = join(homedir(), 'Music', 'Suno');
const SUNO_LIBRARY = 'https://suno.com/me';
const API_BASE = 'https://studio-api.prod.suno.com';
const LOGIN_TIMEOUT_MS = 15 * 60 * 1000;

const pkg = JSON.parse(readFileSync(join(HERE, 'package.json'), 'utf8')) as {
  version: string;
  versionBumpedAt: string;
};
const VERSION = `Version ${pkg.version} (${pkg.versionBumpedAt})`;

type Song = {
  id: string;
  title: string;
  createdAt: string | null;
  audioUrl: string;
  source: string;
};

const songs = new Map<string, Song>();
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// ---------------------------------------------------------------- observability

mkdirSync(LOG_DIR, { recursive: true });
const LOG_FILE = join(LOG_DIR, 'suno-download.jsonl');

function log(level: string, msg: string, ctx: Record<string, unknown> = {}): void {
  const line = JSON.stringify({ ts: new Date().toISOString(), level, msg, ctx });
  try {
    appendFileSync(LOG_FILE, line + '\n', 'utf8');
  } catch {
    /* logging must never kill the run */
  }
}

function say(msg: string): void {
  console.log(msg);
  log('info', msg);
}

/** Logic probe: reports a broken assumption instead of failing silently. */
function probe(condition: boolean, msg: string, ctx: Record<string, unknown> = {}): void {
  if (!condition) {
    console.warn(`   ⚠️  ${msg}`);
    log('probe', msg, ctx);
  }
}

// ---------------------------------------------------------------- song extraction

/** Walks any JSON blob and picks out everything that looks like a Suno song. */
function harvest(node: unknown, source: string): void {
  if (!node || typeof node !== 'object') return;

  if (Array.isArray(node)) {
    for (const child of node) harvest(child, source);
    return;
  }

  const obj = node as Record<string, any>;
  const id = obj.id;

  if (typeof id === 'string' && UUID_RE.test(id)) {
    let audioUrl: string | null = null;
    for (const key of ['audio_url', 'audio_url_mp3', 'mp3_url', 'stream_audio_url']) {
      if (typeof obj[key] === 'string' && obj[key].startsWith('http')) {
        audioUrl = obj[key];
        break;
      }
    }
    // A clip that has a title and duration but no direct URL still resolves on the CDN.
    if (!audioUrl && typeof obj.title === 'string' && (obj.metadata || obj.audio_url === '')) {
      audioUrl = `https://cdn1.suno.ai/${id}.mp3`;
    }

    if (audioUrl) {
      const known = songs.get(id);
      const title = typeof obj.title === 'string' && obj.title.trim() ? obj.title.trim() : known?.title ?? '';
      const createdAt =
        typeof obj.created_at === 'string'
          ? obj.created_at
          : typeof obj.createdAt === 'string'
            ? obj.createdAt
            : known?.createdAt ?? null;
      songs.set(id, { id, title, createdAt, audioUrl, source: known?.source ?? source });
    }
  }

  for (const key of Object.keys(obj)) harvest(obj[key], source);
}

// ---------------------------------------------------------------- browser session

async function clerkToken(page: Page): Promise<string | null> {
  try {
    return await page.evaluate(async () => {
      const clerk = (globalThis as any).Clerk;
      if (clerk?.session?.getToken) return await clerk.session.getToken();
      return null;
    });
  } catch {
    return null;
  }
}

async function waitForLogin(page: Page): Promise<void> {
  const started = Date.now();
  let announced = false;

  while (Date.now() - started < LOGIN_TIMEOUT_MS) {
    const token = await clerkToken(page);
    if (token) {
      say('✅ Angemeldet — die Bibliothek wird jetzt gelesen.');
      return;
    }
    if (!announced) {
      console.log('');
      console.log('👉 Bitte melde dich im geöffneten Chrome-Fenster bei Suno an.');
      console.log('   Danach läuft alles von allein weiter — das Fenster bitte offen lassen.');
      console.log('');
      announced = true;
    }
    await page.waitForTimeout(2000);
  }
  throw new Error('Zeitüberschreitung: Innerhalb von 15 Minuten kam keine Anmeldung zustande.');
}

/** Scrolls the library so lazy-loaded pages fire their requests. */
async function scrollLibrary(page: Page): Promise<void> {
  let unchanged = 0;
  let lastCount = songs.size;

  for (let i = 0; i < 400 && unchanged < 6; i++) {
    await page.mouse.wheel(0, 4000);
    await page.waitForTimeout(700);
    if (songs.size > lastCount) {
      process.stdout.write(`\r   Gefunden: ${songs.size} Songs …`);
      lastCount = songs.size;
      unchanged = 0;
    } else {
      unchanged++;
    }
  }
  process.stdout.write(`\r   Gefunden: ${songs.size} Songs (Seite durchgescrollt).\n`);
}

/** Pages through the Suno API directly — far more reliable than scrolling alone. */
async function pageThroughApi(context: BrowserContext, page: Page, feedPath: string): Promise<void> {
  let emptyPages = 0;

  for (let pageNo = 0; pageNo < 500 && emptyPages < 2; pageNo++) {
    const token = await clerkToken(page); // Clerk tokens expire after ~60 s → fetch a fresh one every time
    if (!token) {
      probe(false, 'Kein Clerk-Token für die API-Abfrage erhalten', { pageNo });
      return;
    }

    const url = `${API_BASE}${feedPath}${feedPath.includes('?') ? '&' : '?'}page=${pageNo}`;
    const before = songs.size;

    try {
      const response = await context.request.get(url, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
        timeout: 30000,
      });
      if (!response.ok()) {
        probe(false, `API antwortete mit ${response.status()}`, { url });
        return;
      }
      harvest(await response.json(), `api:${feedPath}`);
    } catch (error) {
      probe(false, `API-Abfrage fehlgeschlagen: ${(error as Error).message}`, { url });
      return;
    }

    if (songs.size === before) emptyPages++;
    else {
      emptyPages = 0;
      process.stdout.write(`\r   Gefunden: ${songs.size} Songs (Seite ${pageNo + 1}) …`);
    }
  }
  process.stdout.write(`\r   Gefunden: ${songs.size} Songs (Bibliothek vollständig gelesen).\n`);
}

// ---------------------------------------------------------------- downloading

function sanitize(name: string): string {
  const cleaned = name
    .replace(/[\x00-\x1f\x7f]/g, '')
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

      renameSync(temp, target);
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
  const target = process.argv[2] ? process.argv[2] : DEFAULT_TARGET;
  mkdirSync(target, { recursive: true });
  mkdirSync(PROFILE_DIR, { recursive: true });

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  SunoDownload — lädt deine komplette Suno-Bibliothek als MP3');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  ${VERSION}`);
  console.log(`  Zielordner : ${target}`);
  console.log(`  Protokoll  : ${LOG_FILE}`);
  console.log('');

  const context = await chromium.launchPersistentContext(PROFILE_DIR, {
    channel: 'chrome',
    headless: false,
    viewport: null,
    args: ['--start-maximized'],
  });

  let feedPath: string | null = null;

  // Every JSON response the site fetches is scanned for songs.
  context.on('response', async (response) => {
    const url = response.url();
    if (!/suno/i.test(url)) return;
    if (!/json/i.test(response.headers()['content-type'] ?? '')) return;

    try {
      const body = await response.json();
      const before = songs.size;
      harvest(body, url);
      if (songs.size > before && /\/api\/.*(feed|clip|project|song)/i.test(url) && !feedPath) {
        feedPath = new URL(url).pathname + new URL(url).search.replace(/[?&]page=\d+/, '');
        log('info', 'Feed-Endpunkt erkannt', { feedPath });
      }
    } catch {
      /* not every response is parseable JSON */
    }
  });

  const page = context.pages()[0] ?? (await context.newPage());
  say('🌐 Suno wird geöffnet …');
  await page.goto(SUNO_LIBRARY, { waitUntil: 'domcontentloaded', timeout: 120000 });

  await waitForLogin(page);
  await page.goto(SUNO_LIBRARY, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForTimeout(5000);

  say('📚 Bibliothek wird gelesen …');
  await scrollLibrary(page);

  const candidates = feedPath ? [feedPath] : ['/api/feed/v2', '/api/feed/', '/api/project/default/clips'];
  for (const candidate of candidates) {
    const before = songs.size;
    await pageThroughApi(context, page, candidate);
    if (songs.size > before || feedPath) break;
  }

  probe(songs.size > 0, 'Es wurde kein einziger Song gefunden — hat die Bibliothek Inhalte?');
  await context.close();

  if (songs.size === 0) {
    console.log('');
    console.log('❗ Keine Songs gefunden. Bitte melden — das Protokoll liegt unter:');
    console.log(`   ${LOG_FILE}`);
    process.exit(1);
  }

  // Oldest first — number 001 is the oldest song.
  const ordered = [...songs.values()].sort((a, b) => {
    const ta = a.createdAt ? Date.parse(a.createdAt) : Number.MAX_SAFE_INTEGER;
    const tb = b.createdAt ? Date.parse(b.createdAt) : Number.MAX_SAFE_INTEGER;
    return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
  });

  const width = Math.max(3, String(ordered.length).length);
  const existing = new Set(await readdir(target));

  console.log('');
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

    if (existing.has(fileName) && existsSync(fullPath) && statSync(fullPath).size > 1024) {
      console.log(`${label}  — schon vorhanden, übersprungen`);
      skipped++;
      continue;
    }

    process.stdout.write(`${label}  … `);
    try {
      const size = await download(song.audioUrl, fullPath);
      console.log(`${(size / 1024 / 1024).toFixed(1)} MB ✅`);
      log('info', 'Song geladen', { id: song.id, fileName, size });
      saved++;
    } catch (error) {
      const reason = (error as Error).message;
      console.log(`FEHLER (${reason}) ❗`);
      log('error', 'Download fehlgeschlagen', { id: song.id, fileName, reason, url: song.audioUrl });
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
