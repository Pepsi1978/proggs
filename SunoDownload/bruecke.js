/**
 * Brücke zwischen dem angemeldeten Chrome und dem Downloader auf 127.0.0.1.
 *
 * Wird einmal in die Konsole des angemeldeten Chrome eingefügt. Der Anmelde-Nachweis
 * bleibt dabei im Browser — an den Downloader gehen nur Songliste und die von Suno
 * ausgestellten, zeitlich begrenzten Download-Links.
 *
 * Der Geschwindigkeitsgewinn steckt in zwei Stellen:
 *  1. Die Songliste wird über mehrere Seiten gleichzeitig gelesen (statt einzeln).
 *  2. Die Download-Links werden in Schüben angefordert: erst alle anstoßen, dann
 *     einsammeln. Suno meldet beim ersten Abruf oft "processing", beim zweiten ist
 *     der Link sofort da — nacheinander zu warten kostete früher Stunden.
 *
 * Von Hand jederzeit möglich:
 *   sunoStand()        wie weit ist es
 *   sunoWeiter(42)     ab Seite 42 weiterlesen
 */
(async () => {
  const BASIS = 'http://127.0.0.1:__PORT__';
  const HOST = 'https://studio-api.prod.suno.com';
  const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  // Gemessen: 5 Feed-Seiten gleichzeitig laufen sauber durch, ab 12 bremst Suno mit 429.
  const SEITEN_GLEICH = 5;
  // Gemessen: 25 Link-Anfragen gleichzeitig erzeugen keine einzige Bremse.
  const LINKS_GLEICH = 25;
  // So viele Seiten in Folge ohne einen einzigen unbekannten Song = wir sind durch.
  const GENUG = 3;

  const warte = (ms) => new Promise((r) => setTimeout(r, ms));
  const zeig = (t, f) => console.log('%c' + t, 'font-size:15px;font-weight:bold;color:' + (f || '#0a0'));

  if (!window.Clerk || !window.Clerk.session) {
    zeig('❗ Keine Suno-Anmeldung auf dieser Seite. Bitte https://suno.com/me öffnen.', '#c00');
    return;
  }

  // ------------------------------------------------------------- Anmeldung
  // Ein Token ist nur rund eine Minute gültig; für einen Lauf über tausende Songs
  // wird es darum gepuffert und rechtzeitig erneuert, statt bei jeder Anfrage neu.
  let token = null;
  let tokenBis = 0;
  const holeToken = async () => {
    if (token && Date.now() < tokenBis) return token;
    token = await window.Clerk.session.getToken();
    tokenBis = Date.now() + 40000;
    return token;
  };

  const api = async (pfad, versuche) => {
    for (let v = 0; v < (versuche || 6); v++) {
      try {
        const r = await fetch(HOST + pfad, {
          headers: { Authorization: 'Bearer ' + (await holeToken()), Accept: 'application/json' },
        });
        if (r.status === 429) {
          await warte(2000 + v * 2000); // Suno bremst: warten und dieselbe Anfrage wiederholen
          continue;
        }
        if (r.status === 401) {
          token = null; // abgelaufen — beim nächsten Versuch wird ein frisches geholt
          continue;
        }
        if (!r.ok) return null;
        return await r.json();
      } catch (e) {
        await warte(1500 * (v + 1));
      }
    }
    return null;
  };

  const anDownloader = async (pfad, daten) => {
    try {
      const r = await fetch(BASIS + pfad, {
        method: daten ? 'POST' : 'GET',
        headers: daten ? { 'Content-Type': 'application/json' } : undefined,
        body: daten ? JSON.stringify(daten) : undefined,
      });
      return await r.json();
    } catch (e) {
      zeig('❗ Der Downloader antwortet nicht (' + BASIS + '). Läuft das Programm noch?', '#c00');
      return null;
    }
  };

  // ------------------------------------------------------------- anmelden
  const hallo = await anDownloader('/start');
  if (!hallo) return;
  const bekannt = new Set(hallo.bekannt || []);
  const grenze = hallo.neuester ? Date.parse(hallo.neuester) : null;
  const limit = hallo.limit || null;
  zeig('🔗 Mit dem Downloader verbunden. Er kennt bereits ' + bekannt.size + ' Songs.', '#06c');
  if (limit) zeig('   Probelauf: es werden höchstens ' + limit + ' Songs geholt.', '#666');

  // ------------------------------------------------------------- Songliste lesen
  const gefunden = new Map(); // id -> Song, nur eigene und nur unbekannte
  let ich = null;
  let ende = false;

  /** Liest eine Feed-Seite und trägt die eigenen, noch unbekannten Songs ein. */
  const seiteLesen = async (nr) => {
    const daten = await api('/api/feed/v2?page=' + nr + '&page_size=20');
    if (!daten || !Array.isArray(daten.clips)) return { leer: true, neu: 0 };

    if (!ich && daten.clips.length) {
      const erster = daten.clips[0];
      const wer = erster.handle || (erster.user && erster.user.handle);
      if (typeof wer === 'string' && wer) ich = wer.toLowerCase();
    }

    let neu = 0;
    for (const c of daten.clips) {
      if (!c || typeof c.id !== 'string' || !UUID.test(c.id)) continue;
      // In der Antwort hängen auch fremde Stücke (Vorlagen von Coverversionen).
      const wer = c.handle || (c.user && c.user.handle);
      if (ich && typeof wer === 'string' && wer.toLowerCase() !== ich) continue;
      if (bekannt.has(c.id) || gefunden.has(c.id)) continue;
      if (grenze && c.created_at && Date.parse(c.created_at) <= grenze) continue;

      const medien = Array.isArray(c.media_urls)
        ? c.media_urls.map((m) => (typeof m === 'string' ? m : m && m.url)).filter((u) => typeof u === 'string')
        : [];
      gefunden.set(c.id, {
        id: c.id,
        title: typeof c.title === 'string' ? c.title.trim() : '',
        created_at: c.created_at || null,
        audio_url: '',
        media_urls: medien,
        privat: typeof c.is_public === 'boolean' ? !c.is_public : true,
        download_url: undefined,
      });
      neu++;
    }
    return { leer: daten.clips.length === 0, neu, weiter: daten.has_more !== false };
  };

  const listeLesen = async (abSeite) => {
    let seite = abSeite || 0;
    let ohneNeues = 0;

    zeig('🔎 Die Songliste wird gelesen …', '#06c');
    while (seite < 900 && !ende) {
      const block = [];
      for (let i = 0; i < SEITEN_GLEICH; i++) block.push(seite + i);
      const ergebnisse = await Promise.all(block.map(seiteLesen));
      seite += SEITEN_GLEICH;

      const neu = ergebnisse.reduce((s, e) => s + e.neu, 0);
      const alleLeer = ergebnisse.every((e) => e.leer);
      console.log('   Seiten bis ' + seite + ' → ' + gefunden.size + ' neue Songs gefunden');

      if (alleLeer) break;
      if (neu === 0) {
        ohneNeues++;
        if (ohneNeues >= GENUG) {
          zeig('🏁 Ab hier ist alles bereits gesichert.', '#06c');
          break;
        }
      } else {
        ohneNeues = 0;
      }
      if (limit && gefunden.size >= limit) break;
      await warte(250);
    }
    return gefunden.size;
  };
  window.sunoWeiter = listeLesen;
  window.sunoStand = () => gefunden.size;

  await listeLesen(0);

  let liste = [...gefunden.values()].sort((a, b) => {
    const ta = a.created_at ? Date.parse(a.created_at) : 8.64e15;
    const tb = b.created_at ? Date.parse(b.created_at) : 8.64e15;
    return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
  });
  if (limit) liste = liste.slice(-limit); // beim Probelauf die neuesten nehmen

  if (!liste.length) {
    zeig('✅ Nichts Neues — der Downloader hat bereits alles.', '#0a0');
    await anDownloader('/fertig', { gesamt: 0 });
    return;
  }
  zeig('📄 ' + liste.length + ' Songs zu holen. Jetzt die Download-Links …', '#06c');

  // ------------------------------------------------------------- Links holen
  /**
   * Ein Abruf von /api/download/clip/<id> stößt die Aufbereitung an und liefert
   * entweder gleich den Link oder "processing". Statt pro Song zu warten, wird
   * ein ganzer Schub angestoßen und danach erneut abgefragt — bis alle da sind.
   */
  const linkeHolen = async (ids) => {
    const ergebnis = new Map();
    let offen = ids.slice();
    for (let runde = 0; runde < 8 && offen.length; runde++) {
      const naechste = [];
      for (let i = 0; i < offen.length; i += LINKS_GLEICH) {
        const schub = offen.slice(i, i + LINKS_GLEICH);
        const antworten = await Promise.all(
          schub.map(async (id) => {
            const d = await api('/api/download/clip/' + id, 4);
            return { id, url: d && d.download_url ? d.download_url : null, laeuft: d && d.status === 'processing' };
          }),
        );
        for (const a of antworten) {
          if (a.url) ergebnis.set(a.id, a.url);
          else naechste.push(a.id);
        }
        console.log('   Links: ' + ergebnis.size + ' von ' + ids.length);
      }
      offen = naechste;
      if (offen.length) await warte(2500); // die Aufbereitung braucht einen Moment
    }
    return ergebnis;
  };

  const beginn = Date.now();
  const links = await linkeHolen(liste.map((s) => s.id));
  const dauer = Math.round((Date.now() - beginn) / 1000);
  zeig('🔑 ' + links.size + ' von ' + liste.length + ' Links in ' + dauer + ' s geholt.', links.size === liste.length ? '#0a0' : '#c60');

  for (const s of liste) {
    const u = links.get(s.id);
    if (u) s.download_url = u;
  }

  // ------------------------------------------------------------- übergeben
  for (let i = 0; i < liste.length; i += 200) {
    await anDownloader('/songs', { songs: liste.slice(i, i + 200) });
  }
  await anDownloader('/fertig', { gesamt: liste.length });
  zeig('📦 Übergeben. Der Downloader lädt jetzt — dieses Fenster bitte offen lassen.', '#0a0');

  // ------------------------------------------------------------- Nachschub
  // Signierte Links laufen ab. Fällt einer während des Ladens um, fragt der
  // Downloader hier nach einem frischen — dafür bleibt dieser Tab in Bereitschaft.
  let stillstand = 0;
  while (true) {
    const auftrag = await anDownloader('/auftrag');
    if (!auftrag) break;
    if (auftrag.fertig) {
      zeig('✅ Der Downloader ist fertig: ' + auftrag.stand.geladen + ' geladen, ' + auftrag.stand.fehler + ' Fehler.', '#0a0');
      break;
    }
    if (auftrag.ids && auftrag.ids.length) {
      stillstand = 0;
      const frisch = await linkeHolen(auftrag.ids);
      const paket = {};
      for (const [id, url] of frisch) paket[id] = url;
      // Auch die erfolglosen zurückmelden, sonst wartet der Downloader ins Leere.
      for (const id of auftrag.ids) if (!paket[id]) paket[id] = '';
      await anDownloader('/links', { links: paket });
    } else if (auftrag.stand) {
      const s = auftrag.stand;
      console.log('   ' + s.geladen + ' / ' + s.gesamt + ' geladen (' + s.fehler + ' Fehler) — ' + (s.aktuell || ''));
      stillstand++;
      if (stillstand > 900) break; // eine halbe Stunde ohne Lebenszeichen
    }
    await warte(2000);
  }
})();
