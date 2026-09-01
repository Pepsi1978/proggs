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
  // Aendert sich, sobald sich der Aufbau der Eintraege aendert. Ein Zwischenstand aus
  // einer aelteren Fassung wird dann verworfen statt weitergeschleppt.
  const FORMAT = 2;

  const songs = new Map();
  /**
   * Eigener Suno-Name (handle). Er wird aus der ersten Antwort des eigenen Feeds
   * gelesen — der Clerk-Benutzername taugt nicht, dort steht die E-Mail-Adresse.
   */
  let ICH = null;
  /**
   * Zeitstempel des juengsten bereits gesicherten Songs. Das Start-Skript traegt ihn
   * hier ein; Download-Links werden dann nur fuer wirklich neue Songs geholt — sonst
   * waere der Lauf bei mehreren tausend privaten Songs stundenlang beschaeftigt.
   */
  const SEIT = '__SEIT__';
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
      // Suno liefert fuer private Songs Platzhalter wie .../api/forbidden statt einer
      // echten Adresse. Solche Werte fuehren beim Laden zwangslaeufig zu HTTP 403 und
      // werden darum verworfen — fuer diese Songs wird spaeter ein signierter
      // Download-Link geholt.
      const echt = (u) => typeof u === 'string' && u.startsWith('http')
        && !/\/api\//i.test(u) && !/forbidden|unauthorized|placeholder|audiopipe/i.test(u);

      let url = null;
      for (const k of ['audio_url', 'audio_url_mp3', 'mp3_url', 'stream_audio_url']) {
        if (echt(n[k])) { url = n[k]; break; }
      }

      let medien = [];
      if (Array.isArray(n.media_urls)) {
        medien = n.media_urls
          .map((m) => (typeof m === 'string' ? m : (m && m.url) || null))
          .filter(echt);
      }

      // Nur eigene Songs: in der Antwort haengen auch fremde Stuecke (Aehnliches,
      // Vorlagen von Coverversionen), und die gehoeren nicht in die eigene Sicherung.
      // Verglichen wird der handle — der Anzeigename taugt nicht, er lautet anders.
      const wer = n.handle || (n.user && n.user.handle);
      const meins = ICH ? (typeof wer === 'string' && wer.toLowerCase() === ICH) : true;

      // Ein Song ist alles, was Audio-Felder mitbringt. Ob eine Adresse brauchbar ist,
      // entscheidet erst das Ladeprogramm — sonst fielen private Songs hier heraus.
      const istSong = ('audio_url' in n) || Array.isArray(n.media_urls);

      if (meins && istSong) {
        zaehler.roh++;
        const alt = songs.get(n.id) || {};
        songs.set(n.id, {
          id: n.id,
          title: (typeof n.title === 'string' && n.title.trim()) ? n.title.trim() : (alt.title || ''),
          created_at: n.created_at || n.createdAt || alt.created_at || null,
          audio_url: url || '',
          media_urls: medien.length ? medien : (alt.media_urls || []),
          // Privat heisst: das CDN sperrt den Song aus, es braucht einen signierten Link.
          // Erkennbar an is_public — und daran, dass die API keine echte Adresse nennt.
          privat: typeof n.is_public === 'boolean' ? !n.is_public : (!url || alt.privat || false),
          download_url: alt.download_url || undefined,
        });
      }
    }
    for (const k in n) ernte(n[k], zaehler);
    return zaehler.roh;
  };

  // ---------------------------------------------------------------- sichern
  const sicherung = () => {
    try {
      localStorage.setItem(SPEICHER, JSON.stringify({ format: FORMAT, songs: [...songs.values()] }));
    } catch (e) { /* voll */ }
  };
  try {
    const alt = JSON.parse(localStorage.getItem(SPEICHER) || 'null');
    if (alt && alt.format === FORMAT && Array.isArray(alt.songs)) {
      alt.songs.forEach((s) => { if (s && s.id) songs.set(s.id, s); });
      if (songs.size) zeig('↩️ Frueherer Stand geladen: ' + songs.size + ' Songs', '#666');
    } else if (alt) {
      zeig('↩️ Frueherer Stand stammt aus einer aelteren Fassung — die Bibliothek wird neu gelesen.', '#666');
    }
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
   * Das Suno-CDN liefert keine Songs mehr aus — jeder direkte Ladeversuch endet in
   * HTTP 403, bei privaten wie bei veroeffentlichten Stuecken. Der einzige Weg ist
   * der offizielle Download-Endpunkt, der einen zeitlich begrenzten, signierten Link
   * ausstellt. Der wird hier geholt und als download_url in die Liste geschrieben.
   */
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

  const linkeNachholen = async (alle) => {
    const grenze = (!alle && SEIT && SEIT.indexOf('__') !== 0) ? Date.parse(SEIT) : null;
    // cdn1/cdn2.suno.ai zaehlen ausdruecklich nicht: die Adressen stehen zwar in der
    // Antwort, liefern aber seit Sunos Umstellung fuer jeden Song nur noch HTTP 403.
    const brauchbar = (u) => typeof u === 'string' && /\.mp3(\?|$)/i.test(u)
      && !/\/api\//i.test(u) && !/cdn\d*\.suno\.ai/i.test(u);
    const offen = [...songs.values()].filter((s) => {
      if (s.download_url) return false;
      if (brauchbar(s.audio_url) || (s.media_urls || []).some(brauchbar)) return false;
      if (!grenze) return true;
      const t = s.created_at ? Date.parse(s.created_at) : NaN;
      return isNaN(t) || t > grenze; // alles Aeltere liegt bereits gesichert auf der Platte
    });
    if (!offen.length) { zeig('🔑 Keine neuen Songs — es werden keine Download-Links gebraucht.', '#666'); return 0; }

    zeig('🔑 Fuer ' + offen.length + ' neue Songs werden Download-Links geholt …', '#06c');
    let fertig = 0;
    for (let i = 0; i < offen.length; i++) {
      const link = await linkHolen(offen[i].id);
      if (link) { offen[i].download_url = link; fertig++; }
      if (i % 10 === 0) sicherung();
      console.log('   Link ' + (i + 1) + ' von ' + offen.length + (link ? '' : ' — fehlgeschlagen'));
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
      if (!ICH && a.daten && Array.isArray(a.daten.clips) && a.daten.clips.length) {
        const erster = a.daten.clips[0];
        const wer = erster.handle || (erster.user && erster.user.handle);
        if (typeof wer === 'string' && wer) {
          ICH = wer.toLowerCase();
          console.log('   Eigener Suno-Name: ' + ICH);
        }
      }
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
    zeig('   Fehlen spaeter Links (Meldung "Song ist privat"), hilft:  await sunoLinks(true); sunoSpeichern()', '#666');
    if (gesamtLautApi && songs.size < gesamtLautApi) {
      zeig('⚠️ Es fehlen noch ' + (gesamtLautApi - songs.size) + ' Songs.', '#c60');
      zeig('   Weitermachen mit:  sunoWeiter(' + seite + ')', '#c60');
    }
    return songs.size;
  };
  window.sunoWeiter = lauf;

  await lauf(0);
})();
