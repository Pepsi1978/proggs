/**
 * Shared building blocks for all SunoDownload tools.
 *
 * Everything that must behave identically across download, cover and update runs
 * lives here — above all the file naming and the number registry, because a second
 * implementation of those would silently drift and renumber the user's library.
 */

import { appendFileSync, existsSync, mkdirSync, readFileSync, writeFileSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
export const LOG_DIR = join(HERE, 'logs');
export const DEFAULT_TARGET = 'C:\\Sono Backup';
export const COVER_BASE = 'https://cdn1.suno.ai';
export const BESTAND_DATEI = '_bestand.json';

export type Song = {
  id: string;
  title: string;
  created_at: string | null;
  audio_url: string;
};

/** id -> assigned number and file name. Once assigned, a number never changes. */
export type Bestand = {
  stand: string;
  eintraege: Record<string, { nummer: number; datei: string; titel: string }>;
};

// ---------------------------------------------------------------- logging

mkdirSync(LOG_DIR, { recursive: true });

export function makeLogger(name: string) {
  const file = join(LOG_DIR, `${name}.jsonl`);

  const log = (level: string, msg: string, ctx: Record<string, unknown> = {}): void => {
    try {
      appendFileSync(file, JSON.stringify({ ts: new Date().toISOString(), level, msg, ctx }) + '\n', 'utf8');
    } catch {
      /* logging must never kill the run */
    }
  };

  /** Logic probe: reports a broken assumption instead of failing silently. */
  const probe = (condition: boolean, msg: string, ctx: Record<string, unknown> = {}): void => {
    if (!condition) {
      console.warn(`   ⚠️  ${msg}`);
      log('probe', msg, ctx);
    }
  };

  return { log, probe, file };
}

// ---------------------------------------------------------------- naming

export function sanitize(name: string): string {
  const withoutControls = [...name].filter((c) => (c.codePointAt(0) ?? 0) >= 32).join('');
  const cleaned = withoutControls
    .replace(/[<>:"/\\|?*]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/[. ]+$/, '');
  return (cleaned || 'Ohne Titel').slice(0, 120);
}

export function dateiName(nummer: number, titel: string, breite: number, belegt?: Set<string>): string {
  let base = `${String(nummer).padStart(Math.max(3, breite), '0')} - ${sanitize(titel)}`;
  if (belegt && belegt.has(`${base}.mp3`.toLowerCase())) {
    let n = 2;
    while (belegt.has(`${base} (${n}).mp3`.toLowerCase())) n++;
    base = `${base} (${n})`;
  }
  return `${base}.mp3`;
}

/** Oldest first — number 001 is the oldest song. */
export function nachAlter(songs: Song[]): Song[] {
  return [...songs].sort((a, b) => {
    const ta = a.created_at ? Date.parse(a.created_at) : Number.MAX_SAFE_INTEGER;
    const tb = b.created_at ? Date.parse(b.created_at) : Number.MAX_SAFE_INTEGER;
    return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
  });
}

export function leseListe(pfad: string): Song[] {
  const raw = JSON.parse(readFileSync(pfad, 'utf8'));
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
  return songs;
}

// ---------------------------------------------------------------- registry

export function ladeBestand(ordner: string): Bestand {
  const pfad = join(ordner, BESTAND_DATEI);
  if (existsSync(pfad)) {
    try {
      const b = JSON.parse(readFileSync(pfad, 'utf8')) as Bestand;
      if (b && b.eintraege) return b;
    } catch {
      /* fall through to an empty registry */
    }
  }
  return { stand: new Date().toISOString(), eintraege: {} };
}

export function speichereBestand(ordner: string, bestand: Bestand): void {
  bestand.stand = new Date().toISOString();
  writeFileSync(join(ordner, BESTAND_DATEI), JSON.stringify(bestand, null, 2), 'utf8');
}

export function hoechsteNummer(bestand: Bestand): number {
  let max = 0;
  for (const e of Object.values(bestand.eintraege)) if (e.nummer > max) max = e.nummer;
  return max;
}

// ---------------------------------------------------------------- network

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));

export async function ladeBild(url: string): Promise<Buffer | null> {
  try {
    const response = await fetch(url, {
      signal: AbortSignal.timeout(45000),
      headers: { Referer: 'https://suno.com/', Accept: 'image/*' },
    });
    if (!response.ok) return null;
    const buffer = Buffer.from(await response.arrayBuffer());
    return buffer.length > 500 ? buffer : null;
  } catch {
    return null;
  }
}

/**
 * Cover artwork. Fast path derives the URL from the song id; older songs carry a
 * different artwork id, so the public song page is asked for the real address.
 */
export async function holeCover(id: string): Promise<Buffer | null> {
  for (const variant of ['image_large_', 'image_']) {
    for (const ext of ['.jpeg', '.png']) {
      const buffer = await ladeBild(`${COVER_BASE}/${variant}${id}${ext}`);
      if (buffer) return buffer;
    }
  }

  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const page = await fetch(`https://suno.com/song/${id}`, { signal: AbortSignal.timeout(30000) });
      if (page.status === 429) {
        await wait(attempt * 6000);
        continue;
      }
      if (!page.ok) return null;

      const html = await page.text();
      const match = /<meta property="og:image" content="([^"]+)"/.exec(html);
      if (!match?.[1]) return null;
      return await ladeBild(match[1].replace(/&amp;/g, '&'));
    } catch {
      await wait(attempt * 3000);
    }
  }
  return null;
}

function entities(text: string): string {
  return text
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&#x27;/g, "'");
}

/** Title from the public song page — no sign-in required. */
export async function holeTitel(id: string): Promise<string | null> {
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
      if (og?.[1]) return entities(og[1]).trim();

      const title = /<title>([^<]*)<\/title>/.exec(html);
      if (title?.[1]) return entities(title[1]).replace(/\s*\|\s*Suno.*$/i, '').trim();
      return null;
    } catch {
      await wait(attempt * 3000);
    }
  }
  return null;
}

export async function ladeDatei(url: string, ziel: string): Promise<number> {
  const { createWriteStream, renameSync } = await import('node:fs');
  const { Readable } = await import('node:stream');
  const { pipeline } = await import('node:stream/promises');

  let lastError: Error | null = null;

  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(180000) });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      if (!response.body) throw new Error('Leere Antwort');

      const temp = `${ziel}.teil`;
      await pipeline(Readable.fromWeb(response.body as never), createWriteStream(temp));

      const size = statSync(temp).size;
      if (size < 1024) throw new Error(`Datei zu klein (${size} Byte)`);

      renameSync(temp, ziel); // only a complete file ever gets the final name
      return size;
    } catch (error) {
      lastError = error as Error;
      if (attempt < 3) await wait(attempt * 2000);
    }
  }
  throw lastError ?? new Error('Unbekannter Fehler');
}

export const warte = wait;
