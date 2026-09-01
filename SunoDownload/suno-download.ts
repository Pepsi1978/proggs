/**
 * Erstlauf: lädt alle Songs der Songliste als nummerierte MP3s.
 *
 * Für den Normalfall gibt es aktualisieren.ts — das legt zusätzlich die Bestandsdatei
 * an, setzt Cover und Titel und holt bei späteren Läufen nur noch das Neue.
 * Dieses Werkzeug bleibt für den reinen Download ohne Bestandsführung.
 *
 * Usage:  node suno-download.ts [list.json] [target folder]
 */

import { existsSync, mkdirSync, readdirSync, statSync } from 'node:fs';
import { join, isAbsolute } from 'node:path';
import { homedir } from 'node:os';
import { DEFAULT_TARGET, dateiName, ladeDatei, leseListe, makeLogger, nachAlter } from './gemeinsam.ts';

const { log, probe, file: LOG_FILE } = makeLogger('suno-download');
const DOWNLOADS = join(homedir(), 'Downloads');

/** Findet die neueste suno-liste*.json — angegeben, in Downloads oder neben dem Skript. */
function findeListe(explizit: string | undefined, ziel: string): string | null {
  if (explizit) {
    const pfad = isAbsolute(explizit) ? explizit : join(process.cwd(), explizit);
    if (existsSync(pfad)) return pfad;
    console.error(`❗ Diese Datei gibt es nicht: ${pfad}`);
    return null;
  }

  const kandidaten: Array<{ pfad: string; zeit: number }> = [];
  for (const ordner of [DOWNLOADS, ziel]) {
    if (!existsSync(ordner)) continue;
    for (const name of readdirSync(ordner)) {
      if (/^suno-liste.*\.json$/i.test(name) && !/-vorher\.json$/i.test(name)) {
        const pfad = join(ordner, name);
        kandidaten.push({ pfad, zeit: statSync(pfad).mtimeMs });
      }
    }
  }
  kandidaten.sort((a, b) => b.zeit - a.zeit);
  return kandidaten.length ? kandidaten[0].pfad : null;
}

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const listArg = args.find((a) => a.toLowerCase().endsWith('.json'));
  const ziel = args.find((a) => !a.toLowerCase().endsWith('.json')) ?? DEFAULT_TARGET;

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  SunoDownload — lädt die Songs der Songliste als MP3');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('');

  const listePfad = findeListe(listArg, ziel);
  if (!listePfad) {
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

  const songs = leseListe(listePfad);
  console.log(`  Songliste  : ${listePfad}`);
  console.log(`  Zielordner : ${ziel}`);
  console.log(`  Protokoll  : ${LOG_FILE}`);
  console.log('');

  if (songs.length === 0) {
    console.log('❗ Die Songliste ist leer. Bitte den Schritt im Browser wiederholen.');
    process.exit(1);
  }

  mkdirSync(ziel, { recursive: true });
  const sortiert = nachAlter(songs);
  const breite = Math.max(3, String(sortiert.length).length);

  console.log(`⬇️  ${sortiert.length} Songs werden geladen (ältester zuerst).`);
  console.log('');

  let geladen = 0;
  let uebersprungen = 0;
  const fehler: Array<{ name: string; grund: string }> = [];
  const belegt = new Set<string>();

  for (let i = 0; i < sortiert.length; i++) {
    const song = sortiert[i];
    const datei = dateiName(i + 1, song.title, breite, belegt);
    belegt.add(datei.toLowerCase());

    const pfad = join(ziel, datei);
    const label = `[${String(i + 1).padStart(breite, ' ')}/${sortiert.length}] ${datei}`;

    if (existsSync(pfad) && statSync(pfad).size > 1024) {
      console.log(`${label}  — schon vorhanden, übersprungen`);
      uebersprungen++;
      continue;
    }

    process.stdout.write(`${label}  … `);
    try {
      const groesse = await ladeDatei(song.audio_url, pfad, song.id, song.media_urls, song.download_url);
      console.log(`${(groesse / 1024 / 1024).toFixed(1)} MB ✅`);
      log('info', 'Song geladen', { id: song.id, datei, groesse });
      geladen++;
    } catch (error) {
      const grund = (error as Error).message;
      console.log(`FEHLER (${grund}) ❗`);
      log('error', 'Download fehlgeschlagen', { id: song.id, datei, grund, url: song.audio_url });
      fehler.push({ name: datei, grund });
    }
  }

  probe(fehler.length === 0, `${fehler.length} Songs konnten nicht geladen werden`);

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${geladen} geladen · ${uebersprungen} schon vorhanden · ${fehler.length} fehlgeschlagen`);
  console.log(`  Ordner:  ${ziel}`);
  console.log('════════════════════════════════════════════════════════════════════════════════');

  if (fehler.length) {
    console.log('');
    console.log('  Diese Songs kamen nicht durch:');
    for (const f of fehler) console.log(`   ❗ ${f.name} — ${f.grund}`);
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
