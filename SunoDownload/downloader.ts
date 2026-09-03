/**
 * Massen-Downloader: holt die komplette Bibliothek in einem Lauf.
 *
 * Warum es diesen Weg gibt: Das alte Verfahren holte die Download-Links einzeln,
 * nacheinander, mit 600 ms Pause und bis zu 2,5 s Warten pro Song — bei 2400 Songs
 * sind das Stunden. Gemessen verträgt /api/download/clip aber 25 gleichzeitige
 * Anfragen ohne Bremse (429), und ein Link, der beim ersten Abruf noch "processing"
 * meldet, ist beim zweiten Abruf sofort fertig. Daraus wird: erst alle anstoßen,
 * dann alle einsammeln.
 *
 * Aufbau: Node kann sich bei Suno nicht anmelden — der Anmelde-Nachweis lebt im
 * angemeldeten Chrome. Darum läuft nur die Beschaffung (Songliste + signierte Links)
 * im Browser; sie schickt ihre Ergebnisse an diesen kleinen Server auf 127.0.0.1.
 * Alles Weitere — Laden, Cover, Titel, Nummerierung — macht Node selbst und parallel.
 * Der Anmelde-Nachweis verlässt den Browser dabei nie.
 *
 * Aufruf:
 *   node downloader.ts                       ... Zielordner C:\Sono Backup
 *   node downloader.ts "D:\Musik"
 *   node downloader.ts --limit 15 "D:\Test"  ... nur die ersten 15 (zum Ausprobieren)
 *   node downloader.ts --alles               ... ganze Bibliothek prüfen statt nur Neues
 */

import { createServer, type IncomingMessage, type ServerResponse } from 'node:http';
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFile } from 'node:child_process';
import NodeID3 from 'node-id3';
import {
  DEFAULT_TARGET,
  type Bestand,
  type Song,
  dateiName,
  erstbefuellung,
  holeCover,
  holeTitel,
  hoechsteNummer,
  ladeBestand,
  ladeDatei,
  makeLogger,
  nachAlter,
  speichereBestand,
} from './gemeinsam.ts';

const HIER = dirname(fileURLToPath(import.meta.url));
const { log, probe } = makeLogger('downloader');

/** Gemessen: 25 gleichzeitige Link-Anfragen laufen ohne 429 durch, 12 Feed-Seiten nicht. */
const GLEICHZEITIG = 8; // Downloads parallel — mehr bringt nichts, die Leitung ist der Engpass

// ---------------------------------------------------------------- Argumente

const args = process.argv.slice(2);
const zahlNach = (flagge: string): number | null => {
  const i = args.indexOf(flagge);
  if (i < 0) return null;
  const wert = Number(args[i + 1]);
  return Number.isFinite(wert) ? wert : null;
};
const LIMIT = zahlNach('--limit');
const PORT = zahlNach('--port') ?? 8787;
const ALLES = args.includes('--alles');
/** Ohne Zwischenablage und ohne Browser zu öffnen — für den Probelauf. */
const STILL = args.includes('--still');
/**
 * Seit September 2026 muss jeder Song vor dem Download bei Suno freigeschaltet werden
 * (POST /api/download/authorize); das verbraucht einen Download aus dem Monats-
 * kontingent des Abos. Standard: freischalten, älteste zuerst, bis das Kontingent
 * erschöpft ist. Mit --nicht-freischalten werden nur schon freigeschaltete geholt.
 */
const FREISCHALTEN = !args.includes('--nicht-freischalten');
const ZIEL = args.find((a) => !a.startsWith('--') && !/^\d+$/.test(a)) ?? DEFAULT_TARGET;

if (!existsSync(ZIEL)) {
  console.error(`❗ Zielordner gibt es nicht: ${ZIEL}`);
  process.exit(1);
}

// ---------------------------------------------------------------- Zustand

type Auftrag = { song: Song; nummer: number; datei: string };

const bestand: Bestand = ladeBestand(ZIEL);
/** Alles, was der Browser bisher gemeldet hat — nach id, damit Dubletten zusammenfallen. */
const gemeldet = new Map<string, Song>();
/** Songs, deren Link abgelaufen ist: der Browser wird um einen frischen gebeten. */
const nachschubOffen = new Set<string>();
const nachschubEingang = new Map<string, string>();
let browserFertig = false;
let browserGesamt = 0;
let ladephaseLaeuft = false;
let stand = { geladen: 0, fehler: 0, gesamt: 0, aktuell: '' };

// ---------------------------------------------------------------- Server

const KOPF = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': '*',
  'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
  // Chrome 138+ fragt sonst nach, bevor eine Webseite ins lokale Netz darf.
  'Access-Control-Allow-Private-Network': 'true',
  'Access-Control-Max-Age': '86400',
  'Content-Type': 'application/json; charset=utf-8',
};

function antwort(res: ServerResponse, daten: unknown): void {
  res.writeHead(200, KOPF);
  res.end(JSON.stringify(daten));
}

async function körper(req: IncomingMessage): Promise<Record<string, never>> {
  const teile: Buffer[] = [];
  for await (const stück of req) teile.push(stück as Buffer);
  if (!teile.length) return {} as Record<string, never>;
  try {
    return JSON.parse(Buffer.concat(teile).toString('utf8'));
  } catch {
    return {} as Record<string, never>;
  }
}

const server = createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    res.writeHead(204, KOPF);
    res.end();
    return;
  }
  const pfad = (req.url ?? '/').split('?')[0];

  // Das Brücken-Skript selbst — so genügt in der Konsole ein kurzer Einzeiler,
  // statt zehn Kilobyte Text einzufügen.
  if (pfad === '/skript') {
    res.writeHead(200, { ...KOPF, 'Content-Type': 'application/javascript; charset=utf-8' });
    res.end(skriptText());
    return;
  }

  // Der Browser meldet sich an und erfährt, was schon auf der Platte liegt.
  if (pfad === '/start') {
    const bekannt: string[] = [];
    const fehlt: string[] = [];
    for (const [id, e] of Object.entries(bestand.eintraege)) {
      (existsSync(join(ZIEL, e.datei)) ? bekannt : fehlt).push(id);
    }
    let neuester: string | null = null;
    for (const song of Object.values(bestand.eintraege)) void song;
    const listePfad = join(ZIEL, 'suno-liste.json');
    if (existsSync(listePfad)) {
      try {
        const alt = JSON.parse(readFileSync(listePfad, 'utf8')) as Song[];
        for (const s of alt) {
          if (s?.created_at && bestand.eintraege[s.id] && (!neuester || s.created_at > neuester)) {
            neuester = s.created_at;
          }
        }
      } catch {
        /* ohne Zeitstempel wird eben die ganze Bibliothek gelesen */
      }
    }
    console.log(
      `  ➜ Browser verbunden. Bekannt: ${bekannt.length} Songs` +
        (fehlt.length ? `, ${fehlt.length} Dateien fehlen auf der Platte.` : '.'),
    );
    // fehlt: Songs, die einmal geladen waren und deren Datei verschwunden ist. Sie
    // müssen auch dann geholt werden, wenn sie älter sind als die Zeitgrenze — sonst
    // repariert sich eine gelöschte Datei aus dem letzten Jahr nie wieder.
    antwort(res, {
      ok: true,
      bekannt,
      fehlt,
      neuester: ALLES ? null : neuester,
      alles: ALLES,
      limit: LIMIT,
      freischalten: FREISCHALTEN,
    });
    return;
  }

  // Die Brücke meldet, wie es um das Download-Kontingent steht.
  if (pfad === '/kontingent') {
    const daten = (await körper(req)) as unknown as {
      frei?: number;
      gesperrt?: number;
      freigeschaltet?: number;
      erneuert?: string;
      text?: string;
    };
    if (daten.text) console.log(`\n  ${daten.text}`);
    log('info', 'Download-Kontingent', daten);
    antwort(res, { ok: true });
    return;
  }

  // Der Browser liefert Songs samt signiertem Link — in Häppchen, während er liest.
  if (pfad === '/songs') {
    const daten = (await körper(req)) as unknown as { songs?: Song[] };
    let neu = 0;
    for (const s of daten.songs ?? []) {
      if (!s || typeof s.id !== 'string') continue;
      const alt = gemeldet.get(s.id);
      gemeldet.set(s.id, { ...alt, ...s });
      if (!alt) neu++;
    }
    process.stdout.write(`\r  Songliste: ${gemeldet.size} Songs empfangen …   `);
    antwort(res, { ok: true, gesamt: gemeldet.size, neu });
    return;
  }

  // Der Browser ist mit Lesen durch — jetzt wird geladen.
  if (pfad === '/fertig') {
    const daten = (await körper(req)) as unknown as { gesamt?: number };
    browserGesamt = daten.gesamt ?? gemeldet.size;
    browserFertig = true;
    antwort(res, { ok: true });
    return;
  }

  // Der Browser fragt regelmäßig nach, ob Nachschub an Links gebraucht wird.
  if (pfad === '/auftrag') {
    antwort(res, {
      ids: [...nachschubOffen],
      stand,
      fertig: !ladephaseLaeuft && browserFertig && stand.gesamt > 0 && stand.geladen + stand.fehler >= stand.gesamt,
    });
    return;
  }

  if (pfad === '/links') {
    const daten = (await körper(req)) as unknown as { links?: Record<string, string> };
    for (const [id, url] of Object.entries(daten.links ?? {})) {
      // Auch eine leere Antwort wird eingetragen — sonst wartet frischerLink 30 s ins Leere.
      nachschubEingang.set(id, typeof url === 'string' && url.startsWith('http') ? url : '');
      nachschubOffen.delete(id);
    }
    antwort(res, { ok: true });
    return;
  }

  antwort(res, { ok: false });
});

// ---------------------------------------------------------------- Browser-Schritt

/** Das Brücken-Skript mit eingesetztem Port. */
function skriptText(): string {
  return readFileSync(join(HIER, 'bruecke.js'), 'utf8').replace(/__PORT__/g, String(PORT));
}

/** Schreibt das Brücken-Skript und liefert den Pfad. */
function skriptSchreiben(): string {
  const tmp = join(HIER, 'logs', 'bruecke-fertig.js');
  writeFileSync(tmp, skriptText(), 'utf8');
  return tmp;
}

/** Legt das Brücken-Skript in die Zwischenablage und öffnet Suno. */
function browserVorbereiten(): void {
  const tmp = skriptSchreiben();
  // In die Zwischenablage kommt nur ein Einzeiler; er holt das eigentliche Skript
  // vom Downloader. Das erspart das Einfügen von zehn Kilobyte Text.
  const einzeiler = `fetch('http://127.0.0.1:${PORT}/skript').then(r=>r.text()).then(t=>eval(t))`;
  try {
    execFile('powershell', ['-NoProfile', '-Command', `Set-Clipboard -Value @'
${einzeiler}
'@`], {
      windowsHide: true,
    });
    execFile('cmd', ['/c', 'start', '', 'https://suno.com/me'], { windowsHide: true });
  } catch (fehler) {
    console.log(`  (Zwischenablage ging nicht: ${(fehler as Error).message} — Skript liegt in ${tmp})`);
  }
}

// ---------------------------------------------------------------- Ladephase

/** Wartet, bis der Browser fertig gemeldet hat — oder der Benutzer abbricht. */
function warteAufBrowser(): Promise<void> {
  return new Promise((fertig) => {
    const takt = setInterval(() => {
      if (browserFertig) {
        clearInterval(takt);
        fertig();
      }
    }, 300);
  });
}

/** Holt für einen Song einen frischen Link über den Browser (Link abgelaufen). */
async function frischerLink(id: string): Promise<string | undefined> {
  nachschubOffen.add(id);
  for (let i = 0; i < 100; i++) {
    if (nachschubEingang.has(id)) {
      const url = nachschubEingang.get(id);
      nachschubEingang.delete(id);
      return url || undefined;
    }
    await new Promise((r) => setTimeout(r, 300));
  }
  nachschubOffen.delete(id);
  return undefined;
}

const fehlerliste: Array<{ name: string; grund: string }> = [];

async function ladeEinen(auftrag: Auftrag, belegt: Set<string>): Promise<void> {
  const { song, nummer, datei } = auftrag;
  const pfad = join(ZIEL, datei);
  stand.aktuell = datei;

  try {
    let groesse: number;
    try {
      groesse = await ladeDatei(song.audio_url, pfad, song.id, song.media_urls, song.download_url);
    } catch (ersterFehler) {
      // Typischer Fall bei einem langen Lauf: der signierte Link ist abgelaufen.
      // Der Browser kann jederzeit einen neuen ausstellen lassen — also fragen.
      const frisch = await frischerLink(song.id);
      if (!frisch) throw ersterFehler;
      groesse = await ladeDatei(song.audio_url, pfad, song.id, song.media_urls, frisch);
    }

    const tags: Record<string, unknown> = { artist: 'Suno' };
    if (song.title) tags.title = song.title;
    if (song.created_at) tags.year = String(new Date(song.created_at).getFullYear());
    const cover = await holeCover(song.id);
    if (cover) {
      tags.image = { mime: 'image/jpeg', type: { id: 3, name: 'front cover' }, description: 'Cover', imageBuffer: cover };
    }
    NodeID3.update(tags as never, pfad);

    bestand.eintraege[song.id] = { nummer, datei, titel: song.title };
    belegt.add(datei.toLowerCase());
    stand.geladen++;
    log('info', 'Song geladen', { id: song.id, datei, nummer, mb: +(groesse / 1048576).toFixed(1), cover: Boolean(cover) });
  } catch (fehler) {
    const grund = (fehler as Error).message;
    stand.fehler++;
    fehlerliste.push({ name: datei, grund });
    log('error', 'Download fehlgeschlagen', { id: song.id, datei, grund });
  }

  const anteil = stand.gesamt ? Math.round(((stand.geladen + stand.fehler) / stand.gesamt) * 100) : 0;
  process.stdout.write(
    `\r  ${String(anteil).padStart(3)} %  ${stand.geladen} geladen · ${stand.fehler} Fehler   ${datei.slice(0, 46).padEnd(46)}`,
  );
}

/** Lädt alle Aufträge mit fester Anzahl gleichzeitiger Downloads. */
async function ladeAlle(auftraege: Auftrag[], belegt: Set<string>): Promise<void> {
  let naechster = 0;
  const arbeiter = Array.from({ length: Math.min(GLEICHZEITIG, auftraege.length) }, async () => {
    while (naechster < auftraege.length) {
      const auftrag = auftraege[naechster++];
      await ladeEinen(auftrag, belegt);
      // Der Bestand wird laufend gesichert: ein Abbruch kostet dann höchstens
      // die gerade laufenden Downloads, nicht den ganzen Lauf.
      if ((stand.geladen + stand.fehler) % 25 === 0) speichereBestand(ZIEL, bestand);
    }
  });
  await Promise.all(arbeiter);
}

// ---------------------------------------------------------------- Hauptlauf

async function main(): Promise<void> {
  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log('  Suno-Downloader — die ganze Bibliothek in einem Lauf');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Ordner : ${ZIEL}`);
  if (LIMIT) console.log(`  Grenze : nur ${LIMIT} Songs (Probelauf)`);
  if (ALLES) console.log('  Umfang : ganze Bibliothek (nicht nur Neues)');

  // Bestand aus vorhandenen Dateien anlegen, falls es ihn noch nicht gibt.
  const mp3s = readdirSync(ZIEL).filter((n) => n.toLowerCase().endsWith('.mp3'));
  if (Object.keys(bestand.eintraege).length === 0 && mp3s.length > 0) {
    console.log(`  Hinweis: ${mp3s.length} MP3-Dateien ohne Bestandsliste gefunden — sie werden nach dem`);
    console.log('           Einlesen der Songliste zugeordnet, damit keine Nummer wandert.');
  }
  console.log('');

  await new Promise<void>((fertig) => server.listen(PORT, '127.0.0.1', fertig));

  console.log(`  Schritt 1  Das Auslese-Skript liegt in der Zwischenablage, Suno öffnet sich.`);
  console.log('             Dort F12 → Reiter "Console" → Strg+V → Enter.');
  console.log('             (Meckert Chrome: "allow pasting" tippen, Enter, nochmal Strg+V.)');
  console.log('');
  if (STILL) {
    console.log(`  (Probelauf: Skript liegt in logs/bruecke-fertig.js, Port ${PORT})`);
    skriptSchreiben();
  } else {
    browserVorbereiten();
  }
  console.log('  Warte auf den Browser …');

  await warteAufBrowser();
  process.stdout.write('\n');

  let songs = [...gemeldet.values()];
  if (!songs.length) {
    console.log('  Der Browser hat keine Songs gemeldet — Abbruch.');
    server.close();
    return;
  }

  // Die Nummer richtet sich nach dem Alter: 001 ist der älteste Song. Wer schon eine
  // Nummer hat, behält sie für immer — auch wenn Suno die Reihenfolge ändert.
  const sortiert = nachAlter(songs);
  if (Object.keys(bestand.eintraege).length === 0 && mp3s.length > 0) {
    const anzahl = erstbefuellung(ZIEL, sortiert, bestand, probe);
    speichereBestand(ZIEL, bestand);
    console.log(`  Bestand angelegt aus ${anzahl} vorhandenen Dateien.`);
  }

  const neue = sortiert.filter((s) => !bestand.eintraege[s.id]);
  const fehlende = sortiert.filter((s) => {
    const e = bestand.eintraege[s.id];
    return e && !existsSync(join(ZIEL, e.datei));
  });

  console.log(`  Songliste  : ${sortiert.length} Songs`);
  console.log(`  Neu        : ${neue.length}`);
  console.log(`  Fehlt      : ${fehlende.length}`);

  if (!neue.length && !fehlende.length) {
    console.log('');
    console.log('  ✅ Alles auf dem neuesten Stand — nichts zu tun.');
    server.close();
    return;
  }

  // Titel nachholen: ohne Titel hieße die Datei nur "Ohne Titel".
  const ohneTitel = neue.filter((s) => !s.title);
  if (ohneTitel.length) {
    console.log(`  ${ohneTitel.length} Songs ohne Titel — wird von der Songseite geholt …`);
    let offen = 0;
    const holen = Array.from({ length: Math.min(6, ohneTitel.length) }, async () => {
      while (offen < ohneTitel.length) {
        const song = ohneTitel[offen++];
        const titel = await holeTitel(song.id);
        if (titel) song.title = titel;
      }
    });
    await Promise.all(holen);
  }

  const breite = Math.max(3, String(hoechsteNummer(bestand) + neue.length).length);
  const belegt = new Set(mp3s.map((n) => n.toLowerCase()));
  let naechsteNummer = hoechsteNummer(bestand);

  const auftraege: Auftrag[] = [];
  for (const song of fehlende) {
    const e = bestand.eintraege[song.id];
    auftraege.push({ song, nummer: e.nummer, datei: e.datei });
  }
  for (const song of neue) {
    naechsteNummer++;
    auftraege.push({ song, nummer: naechsteNummer, datei: dateiName(naechsteNummer, song.title, breite, belegt) });
    belegt.add(auftraege[auftraege.length - 1].datei.toLowerCase());
  }

  // Ohne signierten Link geht nichts — die Brücke schickt nur noch Songs mit Link.
  const hatAdresse = (s: Song) => Boolean(s.download_url || s.audio_url);
  probe(
    auftraege.every((a) => hatAdresse(a.song)),
    'Aufträge ohne jede Adresse dabei',
    { ohne: auftraege.filter((a) => !hatAdresse(a.song)).length },
  );

  stand.gesamt = auftraege.length;
  ladephaseLaeuft = true;
  console.log('');
  console.log(`  Schritt 2  ${auftraege.length} Songs werden geladen (${GLEICHZEITIG} gleichzeitig) …`);
  console.log('');

  const start = Date.now();
  await ladeAlle(auftraege, belegt);
  ladephaseLaeuft = false;
  speichereBestand(ZIEL, bestand);

  const dauer = Math.round((Date.now() - start) / 1000);
  process.stdout.write('\n');
  console.log('');
  console.log('════════════════════════════════════════════════════════════════════════════════');
  console.log(`  Fertig:  ${stand.geladen} geladen · ${stand.fehler} fehlgeschlagen · ${dauer} s`);
  console.log(`  Bestand: ${Object.keys(bestand.eintraege).length} Songs, höchste Nummer ${hoechsteNummer(bestand)}`);
  console.log('════════════════════════════════════════════════════════════════════════════════');
  if (fehlerliste.length) {
    console.log('');
    console.log('  Nicht geklappt hat:');
    for (const f of fehlerliste.slice(0, 20)) console.log(`   ❗ ${f.name} — ${f.grund}`);
    if (fehlerliste.length > 20) console.log(`   … und ${fehlerliste.length - 20} weitere (siehe logs/downloader.jsonl)`);
    console.log('  Einfach nochmal starten — Fertiges wird übersprungen.');
  }
  console.log('');

  // Dem Browser noch einen Moment Zeit, das Ende mitzubekommen.
  await new Promise((r) => setTimeout(r, 1200));
  server.close();
}

main().catch((fehler) => {
  console.error(`\n❗ Abbruch: ${(fehler as Error).message}`);
  log('fatal', 'Abbruch', { error: String((fehler as Error)?.stack ?? fehler) });
  try {
    speichereBestand(ZIEL, bestand);
  } catch {
    /* dann eben nicht */
  }
  server.close();
  process.exit(1);
});
