/**
 * Trägt Cover und Titel-Informationen in die bereits geladenen MP3-Dateien nach.
 *
 * Zuordnung Datei -> Song über die führende Nummer im Dateinamen: Nummer N ist der
 * N-te Song der nach Alter sortierten Liste — genau die Reihenfolge, in der
 * suno-download.ts die Dateien benannt hat.
 *
 * Usage:  node cover-nachtragen.ts [list.json] [folder]
 */

import { existsSync, mkdirSync, readFileSync, readdirSync, appendFileSync } from 'node:fs';
import { join, dirname, isAbsolute } from 'node:path';
import { fileURLToPath } from 'node:url';
import NodeID3 from 'node-id3';

const HERE = dirname(fileURLToPath(import.meta.url));
const LOG_DIR = join(HERE, 'logs');
const DEFAULT_TARGET = 'C:\\Sono Backup';
const COVER_BASE = 'https://cdn1.suno.ai';

type Song = { id: string; title: string; created_at: string | null; audio_url: string };

mkdirSync(LOG_DIR, { recursive: true });
const LOG_FILE = join(LOG_DIR, 'cover-nachtragen.jsonl');

function log(level: string, msg: string, ctx: Record<string, unknown> = {}): void {
  try {
    appendFileSync(LOG_FILE, JSON.stringify({ ts: new Date().toISOString(), level, msg, ctx }) + '\n', 'utf8');
  } catch {
    /* logging must never kill the run */
  }
}

function probe(condition: boolean, msg: string, ctx: Record<string, unknown> = {}): void {
  if (!condition) {
    console.warn(`   ⚠️  ${msg}`);
    log('probe', msg, ctx);
  }
}

/** Loads the cover image; Suno serves several variants, the large one first. */
async function fetchCover(id: string): Promise<Buffer | null> {
  for (const variant of ['image_large_', 'image_']) {
    for (const ext of ['.jpeg', '.png']) {
      try {
        const response = await fetch(`${COVER_BASE}/${variant}${id}${ext}`, { signal: AbortSignal.timeout(45000) });
        if (!response.ok) continue;
        const buffer = Buffer.from(await response.arrayBuffer());
        if (buffer.length > 500) return buffer;
      } catch {
        /* try the next variant */
      }
    }
  }
  return null;
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
  console.log('  Cover und Titel werden in die MP3-Dateien eingetragen');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Songliste  : ${listPath}`);
  console.log(`  Ordner     : ${target}`);
  console.log('');

  if (!existsSync(listPath)) {
    console.log(`❗ Songliste nicht gefunden: ${listPath}`);
    process.exit(1);
  }

  const raw = JSON.parse(readFileSync(listPath, 'utf8')) as Song[];
  const ordered = [...raw].sort((a, b) => {
    const ta = a.created_at ? Date.parse(a.created_at) : Number.MAX_SAFE_INTEGER;
    const tb = b.created_at ? Date.parse(b.created_at) : Number.MAX_SAFE_INTEGER;
    return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
  });

  const files = readdirSync(target)
    .filter((n) => n.toLowerCase().endsWith('.mp3'))
    .sort();
  console.log(`  ${files.length} MP3-Dateien, ${ordered.length} Songs in der Liste`);
  probe(files.length === ordered.length, 'Anzahl Dateien und Songs stimmt nicht überein', {
    files: files.length,
    songs: ordered.length,
  });
  console.log('');

  let done = 0;
  let hadCover = 0;
  let noTitle = 0;
  const failed: string[] = [];

  for (let i = 0; i < files.length; i++) {
    const fileName = files[i];
    const match = /^(\d+)\s*-/.exec(fileName);
    if (!match) {
      probe(false, `Datei ohne führende Nummer übersprungen: ${fileName}`);
      continue;
    }

    const index = Number(match[1]) - 1;
    const song = ordered[index];
    if (!song) {
      probe(false, `Zu Nummer ${match[1]} gibt es keinen Song in der Liste`, { fileName });
      failed.push(fileName);
      continue;
    }

    const fullPath = join(target, fileName);
    const label = `[${String(i + 1).padStart(String(files.length).length, ' ')}/${files.length}] ${fileName}`;

    const existing = NodeID3.read(fullPath);
    const tags: Record<string, unknown> = {};

    if (song.title) tags.title = song.title;
    else noTitle++;
    if (song.created_at) tags.year = String(new Date(song.created_at).getFullYear());
    if (!existing.artist) tags.artist = 'Suno';

    if (existing.image) {
      hadCover++;
    } else {
      const cover = await fetchCover(song.id);
      if (cover) {
        tags.image = {
          mime: 'image/jpeg',
          type: { id: 3, name: 'front cover' },
          description: 'Cover',
          imageBuffer: cover,
        };
      } else {
        probe(false, `Kein Cover gefunden für ${song.id}`, { fileName });
      }
    }

    process.stdout.write(`${label}  … `);
    const result = NodeID3.update(tags as never, fullPath);
    if (result === true) {
      console.log(tags.image ? 'Cover + Titel ✅' : 'Titel ✅');
      log('info', 'Tags geschrieben', { fileName, id: song.id, cover: Boolean(tags.image) });
      done++;
    } else {
      console.log('FEHLER ❗');
      log('error', 'Tags nicht geschrieben', { fileName, id: song.id, result: String(result) });
      failed.push(fileName);
    }
  }

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${done} bearbeitet · ${hadCover} hatten schon ein Cover · ${failed.length} fehlgeschlagen`);
  if (noTitle) console.log(`  Hinweis: ${noTitle} Songs haben in der Liste keinen Titel.`);
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('');
}

main().catch((error) => {
  console.error(`❗ Abbruch: ${error.message}`);
  log('fatal', 'Abbruch', { error: String(error?.stack ?? error) });
  process.exit(1);
});
