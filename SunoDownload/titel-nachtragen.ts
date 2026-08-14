/**
 * Holt die Titel der Songs nach, die in der Liste ohne Titel stehen, und benennt die
 * zugehörigen Dateien um. Die führende Nummer bleibt unverändert — die Reihenfolge
 * ältester -> neuester Song bleibt also exakt erhalten.
 *
 * Der Titel kommt von der öffentlichen Songseite, es ist keine Anmeldung nötig.
 *
 * Usage:  node titel-nachtragen.ts [list.json] [folder]
 */

import { existsSync, readFileSync, writeFileSync, readdirSync, renameSync } from 'node:fs';
import { join, isAbsolute } from 'node:path';
import { DEFAULT_TARGET, type Song, dateiName, holeTitel, makeLogger, nachAlter, warte } from './gemeinsam.ts';

const { log } = makeLogger('titel-nachtragen');
const PAUSE_MS = 700;

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const listArg = args.find((a) => a.toLowerCase().endsWith('.json'));
  const ziel = args.find((a) => !a.toLowerCase().endsWith('.json')) ?? DEFAULT_TARGET;
  const listePfad = listArg
    ? (isAbsolute(listArg) ? listArg : join(process.cwd(), listArg))
    : join(ziel, 'suno-liste.json');

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  Fehlende Songtitel werden nachgetragen');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('');

  if (!existsSync(listePfad)) {
    console.log(`❗ Songliste nicht gefunden: ${listePfad}`);
    process.exit(1);
  }

  const songs = JSON.parse(readFileSync(listePfad, 'utf8')) as Song[];
  const sortiert = nachAlter(songs);
  const fehlend = sortiert.map((song, i) => ({ song, nummer: i + 1 })).filter((e) => !e.song.title);

  console.log(`  ${fehlend.length} von ${sortiert.length} Songs haben keinen Titel.`);
  console.log('');
  if (fehlend.length === 0) {
    console.log('  Nichts zu tun.');
    console.log('');
    return;
  }

  // Sicherung der ursprünglichen Liste, bevor sie verändert wird.
  const sicherung = listePfad.replace(/\.json$/i, '-vorher.json');
  if (!existsSync(sicherung)) writeFileSync(sicherung, JSON.stringify(songs, null, 2), 'utf8');

  const dateien = readdirSync(ziel).filter((n) => n.toLowerCase().endsWith('.mp3'));
  const nachNummer = new Map<number, string>();
  for (const name of dateien) {
    const m = /^(\d+)\s*-/.exec(name);
    if (m) nachNummer.set(Number(m[1]), name);
  }
  const belegt = new Set(dateien.map((f) => f.toLowerCase()));
  const breite = Math.max(3, String(sortiert.length).length);

  let gefunden = 0;
  let umbenannt = 0;

  for (let i = 0; i < fehlend.length; i++) {
    const { song, nummer } = fehlend[i];
    process.stdout.write(`[${String(i + 1).padStart(3, ' ')}/${fehlend.length}] Nr. ${nummer} … `);

    const titel = await holeTitel(song.id);
    if (!titel) {
      console.log('kein Titel gefunden');
      log('probe', 'Titel nicht ermittelbar', { id: song.id, nummer });
      await warte(PAUSE_MS);
      continue;
    }

    song.title = titel;
    gefunden++;

    const alterName = nachNummer.get(nummer);
    if (!alterName) {
      console.log(`"${titel}" (keine Datei zu Nr. ${nummer})`);
      await warte(PAUSE_MS);
      continue;
    }

    belegt.delete(alterName.toLowerCase());
    const neuerName = dateiName(nummer, titel, breite, belegt);
    belegt.add(neuerName.toLowerCase());

    if (neuerName !== alterName) {
      try {
        renameSync(join(ziel, alterName), join(ziel, neuerName));
        umbenannt++;
        console.log(`"${titel}" ✅`);
        log('info', 'Datei umbenannt', { von: alterName, nach: neuerName, id: song.id });
      } catch (error) {
        console.log(`"${titel}" — Umbenennen fehlgeschlagen (${(error as Error).message})`);
        log('error', 'Umbenennen fehlgeschlagen', { alterName, neuerName, error: String(error) });
      }
    } else {
      console.log(`"${titel}" (Name schon richtig)`);
    }

    await warte(PAUSE_MS);
  }

  writeFileSync(listePfad, JSON.stringify(songs, null, 2), 'utf8');

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${gefunden} Titel gefunden · ${umbenannt} Dateien umbenannt`);
  console.log(`  Die Songliste wurde aktualisiert (Sicherung: ${sicherung})`);
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('');
}

main().catch((error) => {
  console.error(`❗ Abbruch: ${error.message}`);
  log('fatal', 'Abbruch', { error: String(error?.stack ?? error) });
  process.exit(1);
});
