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
      let url = null;
      for (const k of ['audio_url', 'audio_url_mp3', 'mp3_url', 'stream_audio_url']) {
        if (typeof n[k] === 'string' && n[k].startsWith('http')) { url = n[k]; break; }
      }
      if (!url && typeof n.title === 'string' && (n.metadata || n.audio_url === '')) {
        url = 'https://cdn1.suno.ai/' + n.id + '.mp3';
      }
      if (url) {
        zaehler.roh++;
        const alt = songs.get(n.id) || {};
        songs.set(n.id, {
          id: n.id,
          title: (typeof n.title === 'string' && n.title.trim()) ? n.title.trim() : (alt.title || ''),
          created_at: n.created_at || n.createdAt || alt.created_at || null,
          audio_url: url,
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
