/**
 * Nachlade-Lauf: holt nur, was seit dem letzten Mal dazugekommen ist.
 *
 * Die Nummerierung ist dauerhaft: Welche Nummer zu welchem Song gehört, steht in
 * _bestand.json im Zielordner. Einmal vergebene Nummern bleiben unverändert, neue
 * Songs bekommen die nächsten freien — auch dann, wenn Suno die Reihenfolge ändert
 * oder ein älterer Song erst später auftaucht.
 *
 * Usage:  node aktualisieren.ts [list.json] [folder]
 */

import { existsSync, readdirSync, statSync, renameSync } from 'node:fs';
import { join, isAbsolute } from 'node:path';
import { homedir } from 'node:os';
import NodeID3 from 'node-id3';
import {
  DEFAULT_TARGET,
  type Bestand,
  type Song,
  dateiName,
  holeCover,
  holeTitel,
  hoechsteNummer,
  ladeBestand,
  ladeDatei,
  leseListe,
  makeLogger,
  nachAlter,
  speichereBestand,
  warte,
} from './gemeinsam.ts';

const { log, probe } = makeLogger('aktualisieren');
const DOWNLOADS = join(homedir(), 'Downloads');

function findeListe(explizit: string | undefined, ziel: string): string | null {
  if (explizit) {
    const pfad = isAbsolute(explizit) ? explizit : join(process.cwd(), explizit);
    return existsSync(pfad) ? pfad : null;
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

/**
 * First run: derive the registry from the files already on disk. The number in the
 * file name maps to the position in the age-sorted list — exactly how they were named.
 */
function erstbefuellung(ziel: string, sortiert: Song[], bestand: Bestand): number {
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

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const listArg = args.find((a) => a.toLowerCase().endsWith('.json'));
  const ziel = args.find((a) => !a.toLowerCase().endsWith('.json')) ?? DEFAULT_TARGET;

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  Nachlade-Lauf — es wird nur geholt, was neu dazugekommen ist');
  console.log('════════════════════════════════════════════════════════════════════════════════');

  const listePfad = findeListe(listArg, ziel);
  if (!listePfad) {
    console.log('');
    console.log('  Keine Songliste gefunden.');
    console.log('  Bitte zuerst das Skript bibliothek-holen.js in der Chrome-Konsole ausführen.');
    console.log('');
    process.exit(1);
  }

  const songs = leseListe(listePfad);
  const sortiert = nachAlter(songs);
  const bestand = ladeBestand(ziel);

  console.log(`  Songliste  : ${listePfad}`);
  console.log(`  Ordner     : ${ziel}`);
  console.log(`  In der Liste: ${songs.length} Songs`);

  if (Object.keys(bestand.eintraege).length === 0) {
    const anzahl = erstbefuellung(ziel, sortiert, bestand);
    speichereBestand(ziel, bestand);
    console.log(`  Bestand    : neu angelegt aus ${anzahl} vorhandenen Dateien`);
  } else {
    console.log(`  Bestand    : ${Object.keys(bestand.eintraege).length} Songs bereits erfasst`);
  }
  console.log('');

  // Was ist neu, was fehlt auf der Platte?
  const neue = sortiert.filter((s) => !bestand.eintraege[s.id]);
  const fehlende = sortiert.filter((s) => {
    const e = bestand.eintraege[s.id];
    return e && !existsSync(join(ziel, e.datei));
  });

  console.log(`  Neu dazugekommen : ${neue.length}`);
  console.log(`  Fehlt auf Platte : ${fehlende.length}`);
  console.log('');

  if (neue.length === 0 && fehlende.length === 0) {
    console.log('  ✅ Alles auf dem neuesten Stand — nichts zu tun.');
    console.log('');
    return;
  }

  // Fehlende Titel nur für die neuen Songs nachholen
  const ohneTitel = neue.filter((s) => !s.title);
  if (ohneTitel.length) {
    console.log(`  ${ohneTitel.length} neue Songs haben keinen Titel — wird nachgeholt …`);
    for (const song of ohneTitel) {
      const titel = await holeTitel(song.id);
      if (titel) song.title = titel;
      await warte(700);
    }
    console.log('');
  }

  const breite = Math.max(3, String(hoechsteNummer(bestand) + neue.length).length);
  const belegt = new Set(readdirSync(ziel).filter((n) => n.toLowerCase().endsWith('.mp3')).map((n) => n.toLowerCase()));
  let naechste = hoechsteNummer(bestand);

  let geladen = 0;
  const fehler: Array<{ name: string; grund: string }> = [];

  const holen = async (song: Song, nummer: number, datei: string): Promise<void> => {
    const pfad = join(ziel, datei);
    process.stdout.write(`  ${datei}  … `);
    try {
      const groesse = await ladeDatei(song.audio_url, pfad, song.id, song.media_urls, song.download_url);

      const tags: Record<string, unknown> = { artist: 'Suno' };
      if (song.title) tags.title = song.title;
      if (song.created_at) tags.year = String(new Date(song.created_at).getFullYear());
      const cover = await holeCover(song.id);
      if (cover) {
        tags.image = { mime: 'image/jpeg', type: { id: 3, name: 'front cover' }, description: 'Cover', imageBuffer: cover };
      }
      NodeID3.update(tags as never, pfad);

      console.log(`${(groesse / 1024 / 1024).toFixed(1)} MB${cover ? ' + Cover' : ''} ✅`);
      log('info', 'Song geladen', { id: song.id, datei, nummer, cover: Boolean(cover) });
      bestand.eintraege[song.id] = { nummer, datei, titel: song.title };
      belegt.add(datei.toLowerCase());
      geladen++;
    } catch (error) {
      const grund = (error as Error).message;
      console.log(`FEHLER (${grund}) ❗`);
      log('error', 'Download fehlgeschlagen', { id: song.id, datei, grund });
      fehler.push({ name: datei, grund });
    }
  };

  if (fehlende.length) {
    console.log('  Fehlende Dateien werden erneut geladen:');
    for (const song of fehlende) {
      const eintrag = bestand.eintraege[song.id];
      await holen(song, eintrag.nummer, eintrag.datei);
    }
    console.log('');
  }

  if (neue.length) {
    console.log('  Neue Songs (älteste zuerst):');
    for (const song of neue) {
      naechste++;
      const datei = dateiName(naechste, song.title, breite, belegt);
      await holen(song, naechste, datei);
    }
  }

  speichereBestand(ziel, bestand);

  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${geladen} geladen · ${fehler.length} fehlgeschlagen`);
  console.log(`  Bestand: ${Object.keys(bestand.eintraege).length} Songs, höchste Nummer ${hoechsteNummer(bestand)}`);
  console.log('════════════════════════════════════════════════════════════════════════════════');
  if (fehler.length) {
    console.log('');
    console.log('  Nicht geklappt hat:');
    for (const f of fehler) console.log(`   ❗ ${f.name} — ${f.grund}`);
    console.log('  Einfach nochmal starten — Fertiges wird übersprungen.');
  }
  console.log('');
}

main().catch((error) => {
  console.error(`❗ Abbruch: ${error.message}`);
  log('fatal', 'Abbruch', { error: String(error?.stack ?? error) });
  process.exit(1);
});
