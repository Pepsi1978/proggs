/**
 * Wird im NORMALEN, bereits angemeldeten Chrome in die Entwickler-Konsole eingefügt.
 * Liest die KOMPLETTE Suno-Bibliothek aus und speichert sie als suno-liste.json.
 *
 * Wichtigste Eigenschaft: Suno bremst haeufige Abfragen aus (429 Too Many Requests).
 * Darum wird zwischen den Abfragen gewartet und eine gebremste Seite so lange
 * wiederholt, bis sie durchkommt — es geht also keine Seite verloren.
 *
 * Jederzeit von Hand moeglich:
 *   sunoSpeichern()   aktuellen Stand als Datei speichern
 *   sunoStand()       wie viele Songs sind bisher da
 *   sunoWeiter(42)    ab Seite 42 weitermachen (nach einem Abbruch)
 */
(async () => {
  const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  const HOST = 'https://studio-api.prod.suno.com';
  const PFAD = '/api/feed/v2';
  const PAUSE = 900;        // Millisekunden zwischen zwei Abfragen
  const SPEICHER = 'suno-download-stand';

  const songs = new Map();
  let gesamtLautApi = null;

  const warte = (ms) => new Promise((r) => setTimeout(r, ms));
  const zeig = (t, f) => console.log('%c' + t, 'font-size:15px;font-weight:bold;color:' + (f || '#0a0'));

  // ---------------------------------------------------------------- einsammeln
  /** Traegt alle Songs aus einer Antwort ein und liefert zurueck, wie viele darin standen. */
  const ernte = (n, zaehler) => {
    zaehler = zaehler || { roh: 0 };
    if (!n || typeof n !== 'object') return zaehler.roh;
    if (Array.isArray(n)) { n.forEach((x) => ernte(x, zaehler)); return zaehler.roh; }

    for (const k of ['num_total_results', 'total_results', 'total']) {
      if (typeof n[k] === 'number' && n[k] > (gesamtLautApi || 0)) gesamtLautApi = n[k];
    }

    if (typeof n.id === 'string' && UUID.test(n.id)) {
      // Suno liefert fuer neuere Songs Platzhalter wie .../api/forbidden statt einer
      // echten Adresse. Solche Werte fuehren beim Laden zwangslaeufig zu HTTP 403 und
      // werden darum verworfen — das Ladeprogramm findet den Song ueber seine ID.
      const echt = (u) => typeof u === 'string' && u.startsWith('http')
        && !/\/api\//i.test(u) && !/forbidden|unauthorized|placeholder/i.test(u);
      let url = null;
      for (const k of ['audio_url', 'audio_url_mp3', 'mp3_url', 'stream_audio_url']) {
        if (echt(n[k])) { url = n[k]; break; }
      }
      if (!url && typeof n.title === 'string') {
        url = 'https://audiopipe.suno.ai/?item_id=' + n.id;
      }
      // media_urls ist die einzige Quelle, die auch bei privaten Songs traegt —
      // cdn1.suno.ai sperrt die mit HTTP 403 aus.
      let medien = [];
      if (Array.isArray(n.media_urls)) {
        medien = n.media_urls
          .map((m) => (typeof m === 'string' ? m : (m && m.url) || null))
          .filter((m) => typeof m === 'string' && m.startsWith('http'));
      }
      if (url || medien.length) {
        zaehler.roh++;
        const alt = songs.get(n.id) || {};
        songs.set(n.id, {
          id: n.id,
          title: (typeof n.title === 'string' && n.title.trim()) ? n.title.trim() : (alt.title || ''),
          created_at: n.created_at || n.createdAt || alt.created_at || null,
          audio_url: url || '',
          media_urls: medien.length ? medien : (alt.media_urls || []),
          download_url: alt.download_url || undefined,
        });
      }
    }
    for (const k in n) ernte(n[k], zaehler);
    return zaehler.roh;
  };

  // ---------------------------------------------------------------- sichern
  const sicherung = () => {
    try { localStorage.setItem(SPEICHER, JSON.stringify([...songs.values()])); } catch (e) { /* voll */ }
  };
  try {
    const alt = JSON.parse(localStorage.getItem(SPEICHER) || '[]');
    alt.forEach((s) => { if (s && s.id) songs.set(s.id, s); });
    if (songs.size) zeig('↩️ Frueherer Stand geladen: ' + songs.size + ' Songs', '#666');
  } catch (e) { /* egal */ }

  const speichern = () => {
    const liste = [...songs.values()].sort((a, b) => {
      const ta = a.created_at ? Date.parse(a.created_at) : 8.64e15;
      const tb = b.created_at ? Date.parse(b.created_at) : 8.64e15;
      return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
    });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([JSON.stringify(liste, null, 2)], { type: 'application/json' }));
    a.download = 'suno-liste.json';
    document.body.appendChild(a); a.click(); a.remove();

    zeig('✅ ' + liste.length + ' Songs gespeichert — Datei "suno-liste.json" bitte in den Downloads ablegen.');
    if (gesamtLautApi) zeig('   Suno nennt insgesamt: ' + gesamtLautApi + ' Songs', '#666');
    return liste.length;
  };
  window.sunoSpeichern = speichern;
  window.sunoStand = () => songs.size;

  // ---------------------------------------------------------------- Anmeldung
  if (!window.Clerk || !window.Clerk.session) {
    zeig('❗ Keine Suno-Anmeldung auf dieser Seite. Bitte https://suno.com/me öffnen.', '#c00');
    return;
  }
  const token = async () => { try { return await window.Clerk.session.getToken(); } catch (e) { return null; } };

  const hol = async (adresse) => {
    try {
      const r = await fetch(adresse, {
        headers: { Authorization: 'Bearer ' + (await token()), Accept: 'application/json' },
      });
      if (!r.ok) return { fehler: r.status };
      return { daten: await r.json() };
    } catch (e) { return { fehler: String(e).slice(0, 60) }; }
  };

  // ------------------------------------------------------- Download-Links holen
  /**
   * Private Songs (alles, was nicht veroeffentlicht ist) sperrt das Suno-CDN aus:
   * jeder Ladeversuch endet in HTTP 403. Nur der offizielle Download-Endpunkt
   * liefert einen signierten Link. Der wird hier fuer genau diese Songs geholt und
   * als download_url in die Liste geschrieben.
   */
  const echtesMp3 = (u) => typeof u === 'string' && /\.mp3(\?|$)/i.test(u)
    && !/\/api\//i.test(u) && !/forbidden/i.test(u);

  const linkHolen = async (id) => {
    for (let versuch = 0; versuch < 12; versuch++) {
      const a = await hol(HOST + '/api/download/clip/' + id);
      if (a.fehler === 429) { await warte(8000); continue; }
      if (a.fehler) return null;
      if (a.daten && a.daten.download_url) return a.daten.download_url;
      if (a.daten && a.daten.status === 'processing') { await warte(2500); continue; }
      return null;
    }
    return null;
  };

  const linkeNachholen = async () => {
    const offen = [...songs.values()].filter(
      (s) => !echtesMp3(s.audio_url) && !(s.media_urls || []).some(echtesMp3) && !s.download_url,
    );
    if (!offen.length) return 0;

    zeig('🔑 ' + offen.length + ' Songs sind privat — es werden Download-Links geholt …', '#06c');
    let fertig = 0;
    for (const song of offen) {
      const link = await linkHolen(song.id);
      if (link) { song.download_url = link; fertig++; }
      if (fertig % 10 === 0) sicherung();
      console.log('   Link ' + (fertig) + ' von ' + offen.length + (link ? '' : ' — fehlgeschlagen'));
      await warte(600);
    }
    sicherung();
    zeig('🔑 ' + fertig + ' von ' + offen.length + ' Download-Links geholt.', fertig === offen.length ? '#0a0' : '#c60');
    return fertig;
  };
  window.sunoLinks = linkeNachholen;

  // ---------------------------------------------------------------- Hauptlauf
  const lauf = async (startSeite) => {
    let seite = startSeite || 0;
    let bremsen = 0;
    let fehler = 0;
    let leer = 0;

    zeig('🔎 Die Bibliothek wird gelesen. Bitte den Tab offen lassen — das dauert einige Minuten.', '#06c');

    while (seite < 800) {
      const a = await hol(HOST + PFAD + '?page=' + seite + '&page_size=20');

      // Suno bremst: geduldig warten und DIESELBE Seite erneut holen
      if (a.fehler === 429) {
        bremsen++;
        const sekunden = Math.min(60, 8 * bremsen);
        zeig('⏳ Suno bremst — warte ' + sekunden + ' s und hole Seite ' + seite + ' erneut …', '#c60');
        await warte(sekunden * 1000);
        continue;
      }

      if (a.fehler) {
        fehler++;
        console.log('   Seite ' + seite + ': Fehler ' + a.fehler + ' (Versuch ' + fehler + ' von 5)');
        if (fehler >= 5) { zeig('⚠️ Seite ' + seite + ' bleibt fehlerhaft — Lauf wird hier beendet.', '#c60'); break; }
        await warte(4000);
        continue;
      }

      bremsen = 0;
      fehler = 0;
      const anzahl = ernte(a.daten);

      if (anzahl === 0) {
        leer++;
        if (leer >= 2) { zeig('🏁 Ende der Bibliothek erreicht (Seite ' + seite + ').', '#06c'); break; }
      } else {
        leer = 0;
        console.log('   Seite ' + seite + ' → insgesamt ' + songs.size + ' Songs'
          + (gesamtLautApi ? ' von ' + gesamtLautApi : ''));
      }

      seite++;
      if (seite % 10 === 0) sicherung();
      await warte(PAUSE);
    }

    sicherung();
    await linkeNachholen();
    speichern();
    if (gesamtLautApi && songs.size < gesamtLautApi) {
      zeig('⚠️ Es fehlen noch ' + (gesamtLautApi - songs.size) + ' Songs.', '#c60');
      zeig('   Weitermachen mit:  sunoWeiter(' + seite + ')', '#c60');
    }
    return songs.size;
  };
  window.sunoWeiter = lauf;

  await lauf(0);
})();
