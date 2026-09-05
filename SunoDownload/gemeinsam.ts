/**
 * Shared building blocks for all SunoDownload tools.
 *
 * Everything that must behave identically across download, cover and update runs
 * lives here — above all the file naming and the number registry, because a second
 * implementation of those would silently drift and renumber the user's library.
 */

import { appendFileSync, existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
export const LOG_DIR = join(HERE, 'logs');
export const DEFAULT_TARGET = 'C:\\Suno Backup';
export const COVER_BASE = 'https://cdn1.suno.ai';
export const AUDIO_BASE = 'https://cdn1.suno.ai';
/**
 * cdn1.suno.ai liefert seit Sunos Umstellung keinen Song mehr aus — weder private noch
 * veröffentlichte, alles endet in HTTP 403 —, und die CloudFront-Auslieferung gibt nur
 * einen verschleierten Datenstrom heraus. Der einzige tragfähige Weg ist der signierte
 * Link aus /api/download/clip/<id>, den das Browser-Skript beim Erstellen der Liste
 * mitholt (Feld download_url). Die alte Adresse bleibt nur als letzter Versuch stehen.
 */
export const BROWSER_KOPF = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36',
  Referer: 'https://suno.com/',
  Accept: '*/*',
} as const;
export const BESTAND_DATEI = '_bestand.json';

export type Song = {
  id: string;
  title: string;
  created_at: string | null;
  audio_url: string;
  /** Alle von Suno gemeldeten Medien-Adressen. */
  media_urls?: string[];
  /** Signierter Link aus /api/download/clip/<id> — trägt auch bei privaten Songs. */
  download_url?: string;
  /** Nicht veröffentlicht — das Suno-CDN sperrt diesen Song aus. */
  privat?: boolean;
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

/**
 * Suno liefert seit Kurzem für neuere Songs Platzhalter statt einer echten Adresse
 * (z. B. .../api/forbidden). Solche Einträge müssen ignoriert werden, sonst endet
 * jeder Download in einem 403.
 */
export function brauchbareAudioUrl(url: unknown): url is string {
  if (typeof url !== 'string' || !url.startsWith('http')) return false;
  if (/\/api\//i.test(url)) return false;
  if (/forbidden|unauthorized|placeholder/i.test(url)) return false;
  // audiopipe.suno.ai antwortet zwar mit 200, liefert aber einen leeren Datenstrom.
  if (/audiopipe/i.test(url)) return false;
  return true;
}

/**
 * Alle Adressen, unter denen ein Song erreichbar sein kann — beste zuerst.
 *
 * Die m4a aus media_urls (CloudFront, "m4a-opus") ist zwar ohne Anmeldung erreichbar,
 * aber verschlüsselt — ffmpeg findet darin kein moov-Atom. Sie wird darum bewusst
 * nicht als Quelle genommen; der einzige Weg ist der signierte Link nach Freischaltung.
 */
export function audioKandidaten(id: string, url?: string, medien?: string[], signiert?: string): string[] {
  const liste: string[] = [];

  if (brauchbareAudioUrl(signiert)) liste.push(signiert);
  for (const m of medien ?? []) {
    if (brauchbareAudioUrl(m) && /\.mp3(\?|$)/i.test(m)) liste.push(m);
  }
  if (brauchbareAudioUrl(url)) liste.push(url);
  liste.push(`${AUDIO_BASE}/${id}.mp3`);

  return [...new Set(liste)];
}

export function leseListe(pfad: string): Song[] {
  const raw = JSON.parse(readFileSync(pfad, 'utf8'));
  const list = Array.isArray(raw) ? raw : Array.isArray(raw?.songs) ? raw.songs : [];

  const songs: Song[] = [];
  for (const entry of list) {
    if (entry && typeof entry.id === 'string') {
      songs.push({
        id: entry.id,
        title: typeof entry.title === 'string' ? entry.title : '',
        created_at: typeof entry.created_at === 'string' ? entry.created_at : null,
        audio_url: brauchbareAudioUrl(entry.audio_url) ? entry.audio_url : '',
        media_urls: Array.isArray(entry.media_urls)
          ? entry.media_urls
              .map((m: unknown) => (typeof m === 'string' ? m : (m as { url?: string })?.url))
              .filter((m: unknown): m is string => typeof m === 'string')
          : undefined,
        download_url: typeof entry.download_url === 'string' ? entry.download_url : undefined,
        privat: entry.privat === true,
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

/**
 * Erstlauf: leitet die Nummernliste aus den Dateien ab, die schon auf der Platte liegen.
 * Die Nummer im Dateinamen entspricht der Stelle in der nach Alter sortierten Liste —
 * genau so wurden die Dateien seinerzeit benannt.
 */
export function erstbefuellung(
  ziel: string,
  sortiert: Song[],
  bestand: Bestand,
  probe: (bedingung: boolean, msg: string, ctx?: Record<string, unknown>) => void,
): number {
  const dateien = readdirSync(ziel).filter((n) => n.toLowerCase().endsWith('.mp3'));
  let uebernommen = 0;

  for (const datei of dateien) {
    const treffer = /^(\d+)\s*-/.exec(datei);
    if (!treffer) continue;

    const nummer = Number(treffer[1]);
    const song = sortiert[nummer - 1];
    if (!song) {
      probe(false, `Zu Nummer ${nummer} gibt es keinen Song in der Liste`, { datei });
      continue;
    }
    bestand.eintraege[song.id] = { nummer, datei, titel: song.title };
    uebernommen++;
  }

  probe(uebernommen === dateien.length, 'Nicht jede Datei konnte zugeordnet werden', {
    dateien: dateien.length,
    uebernommen,
  });
  return uebernommen;
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

/** Wandelt eine heruntergeladene m4a-Datei in eine MP3 um; ohne ffmpeg schlägt das fehl. */
async function nachMp3(quelle: string, ziel: string): Promise<void> {
  const { execFile } = await import('node:child_process');
  const { promisify } = await import('node:util');
  await promisify(execFile)(
    'ffmpeg',
    ['-y', '-loglevel', 'error', '-i', quelle, '-codec:a', 'libmp3lame', '-q:a', '2', ziel],
    { windowsHide: true, maxBuffer: 1 << 24 },
  );
}

export async function ladeDatei(
  url: string,
  ziel: string,
  id?: string,
  medien?: string[],
  signiert?: string,
): Promise<number> {
  const { createWriteStream, renameSync, unlinkSync } = await import('node:fs');
  const { Readable } = await import('node:stream');
  const { pipeline } = await import('node:stream/promises');

  const quellen = id ? audioKandidaten(id, url, medien, signiert) : [url];
  const putz = (pfad: string) => {
    try {
      unlinkSync(pfad);
    } catch {
      /* nichts zu putzen */
    }
  };

  let lastError: Error | null = null;

  for (const quelle of quellen) {
    // Alles, was nicht schon MP3 ist, wird nach dem Laden umgewandelt.
    const istMp3 = /\.mp3(\?|$)/i.test(quelle);
    const roh = istMp3 ? `${ziel}.teil` : `${ziel}.teil.m4a`;

    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        const response = await fetch(quelle, { signal: AbortSignal.timeout(180000), headers: BROWSER_KOPF });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        if (!response.body) throw new Error('Leere Antwort');

        await pipeline(Readable.fromWeb(response.body as never), createWriteStream(roh));

        if (statSync(roh).size < 1024) throw new Error(`Datei zu klein (${statSync(roh).size} Byte)`);

        if (istMp3) {
          renameSync(roh, ziel); // nur eine vollständige Datei bekommt den endgültigen Namen
        } else {
          const mp3 = `${ziel}.teil`;
          await nachMp3(roh, mp3);
          putz(roh);
          if (statSync(mp3).size < 1024) throw new Error('Umwandlung ergab keine brauchbare Datei');
          renameSync(mp3, ziel);
        }
        return statSync(ziel).size;
      } catch (error) {
        lastError = error as Error;
        putz(roh);
        putz(`${ziel}.teil`);
        // 4xx heißt: diese Quelle liefert den Song nicht — sofort die nächste versuchen.
        if (/HTTP 4\d\d/.test(lastError.message)) break;
        if (attempt < 3) await wait(attempt * 2000);
      }
    }
  }
  if (lastError && /HTTP 40\d|zu klein/.test(lastError.message) && !brauchbareAudioUrl(signiert)) {
    throw new Error('gesperrt — es fehlt ein gültiger Download-Link (Songliste neu holen)');
  }
  throw lastError ?? new Error('Unbekannter Fehler');
}

export const warte = wait;
