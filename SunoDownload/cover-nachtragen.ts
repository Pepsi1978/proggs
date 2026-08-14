/**
 * Trägt Cover und Titel-Informationen in bereits geladene MP3-Dateien nach.
 *
 * Zuordnung Datei -> Song über die führende Nummer im Dateinamen: Nummer N ist der
 * N-te Song der nach Alter sortierten Liste — genau die Reihenfolge, in der die
 * Dateien benannt wurden. Bereits vollständige Dateien werden übersprungen.
 *
 * Usage:  node cover-nachtragen.ts [list.json] [folder]
 */

import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join, isAbsolute } from 'node:path';
import NodeID3 from 'node-id3';
import { DEFAULT_TARGET, type Song, holeCover, makeLogger, nachAlter } from './gemeinsam.ts';

const { log, probe } = makeLogger('cover-nachtragen');

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const listArg = args.find((a) => a.toLowerCase().endsWith('.json'));
  const ziel = args.find((a) => !a.toLowerCase().endsWith('.json')) ?? DEFAULT_TARGET;

  const listePfad = listArg
    ? (isAbsolute(listArg) ? listArg : join(process.cwd(), listArg))
    : join(ziel, 'suno-liste.json');

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  Cover und Titel werden in die MP3-Dateien eingetragen');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Songliste  : ${listePfad}`);
  console.log(`  Ordner     : ${ziel}`);
  console.log('');

  if (!existsSync(listePfad)) {
    console.log(`❗ Songliste nicht gefunden: ${listePfad}`);
    process.exit(1);
  }

  const sortiert = nachAlter(JSON.parse(readFileSync(listePfad, 'utf8')) as Song[]);
  const dateien = readdirSync(ziel).filter((n) => n.toLowerCase().endsWith('.mp3')).sort();

  console.log(`  ${dateien.length} MP3-Dateien, ${sortiert.length} Songs in der Liste`);
  probe(dateien.length === sortiert.length, 'Anzahl Dateien und Songs stimmt nicht überein', {
    dateien: dateien.length,
    songs: sortiert.length,
  });
  console.log('');

  let bearbeitet = 0;
  let vollstaendig = 0;
  let ohneTitel = 0;
  const fehler: string[] = [];

  for (let i = 0; i < dateien.length; i++) {
    const datei = dateien[i];
    const treffer = /^(\d+)\s*-/.exec(datei);
    if (!treffer) {
      probe(false, `Datei ohne führende Nummer übersprungen: ${datei}`);
      continue;
    }

    const song = sortiert[Number(treffer[1]) - 1];
    if (!song) {
      probe(false, `Zu Nummer ${treffer[1]} gibt es keinen Song in der Liste`, { datei });
      fehler.push(datei);
      continue;
    }

    const pfad = join(ziel, datei);
    const vorhanden = NodeID3.read(pfad);

    // Schon vollständig — hält Wiederholungsläufe schnell.
    if (vorhanden.image && vorhanden.title === (song.title || vorhanden.title)) {
      vollstaendig++;
      continue;
    }

    const tags: Record<string, unknown> = {};
    if (song.title) tags.title = song.title;
    else ohneTitel++;
    if (song.created_at) tags.year = String(new Date(song.created_at).getFullYear());
    if (!vorhanden.artist) tags.artist = 'Suno';

    if (!vorhanden.image) {
      const cover = await holeCover(song.id);
      if (cover) {
        tags.image = { mime: 'image/jpeg', type: { id: 3, name: 'front cover' }, description: 'Cover', imageBuffer: cover };
      } else {
        probe(false, `Kein Cover verfügbar für ${song.id}`, { datei });
      }
    }

    process.stdout.write(`[${String(i + 1).padStart(String(dateien.length).length, ' ')}/${dateien.length}] ${datei}  … `);
    const ergebnis = NodeID3.update(tags as never, pfad);
    if (ergebnis === true) {
      console.log(tags.image ? 'Cover + Titel ✅' : 'Titel ✅');
      log('info', 'Tags geschrieben', { datei, id: song.id, cover: Boolean(tags.image) });
      bearbeitet++;
    } else {
      console.log('FEHLER ❗');
      log('error', 'Tags nicht geschrieben', { datei, id: song.id, ergebnis: String(ergebnis) });
      fehler.push(datei);
    }
  }

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${bearbeitet} bearbeitet · ${vollstaendig} waren schon vollständig · ${fehler.length} fehlgeschlagen`);
  if (ohneTitel) console.log(`  Hinweis: ${ohneTitel} Songs haben in der Liste keinen Titel.`);
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('');
}

main().catch((error) => {
  console.error(`❗ Abbruch: ${error.message}`);
  log('fatal', 'Abbruch', { error: String(error?.stack ?? error) });
  process.exit(1);
});
