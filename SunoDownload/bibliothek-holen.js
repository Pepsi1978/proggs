/**
 * Wird im NORMALEN, bereits angemeldeten Chrome in die Entwickler-Konsole eingefuegt.
 * Liest die komplette Suno-Bibliothek aus und speichert sie als suno-liste.json
 * in den Downloads-Ordner. Es wird nichts veraendert und nichts hochgeladen.
 */
(async () => {
  const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  const songs = new Map();

  // Durchsucht beliebige JSON-Strukturen nach allem, was wie ein Suno-Song aussieht.
  const ernte = (n) => {
    if (!n || typeof n !== 'object') return;
    if (Array.isArray(n)) return n.forEach(ernte);

    if (typeof n.id === 'string' && UUID.test(n.id)) {
      let url = null;
      for (const k of ['audio_url', 'audio_url_mp3', 'mp3_url', 'stream_audio_url']) {
        if (typeof n[k] === 'string' && n[k].startsWith('http')) { url = n[k]; break; }
      }
      if (!url && typeof n.title === 'string' && n.metadata) url = 'https://cdn1.suno.ai/' + n.id + '.mp3';

      if (url) {
        const alt = songs.get(n.id) || {};
        songs.set(n.id, {
          id: n.id,
          title: (typeof n.title === 'string' && n.title.trim()) ? n.title.trim() : (alt.title || ''),
          created_at: n.created_at || n.createdAt || alt.created_at || null,
          audio_url: url,
        });
      }
    }
    for (const k in n) ernte(n[k]);
  };

  const zeig = (text, farbe) =>
    console.log('%c' + text, 'font-size:15px;font-weight:bold;color:' + (farbe || '#0a0'));

  const holToken = async () => {
    try { return await window.Clerk.session.getToken(); } catch (e) { return null; }
  };

  const speichern = () => {
    const liste = [...songs.values()].sort((a, b) => {
      const ta = a.created_at ? Date.parse(a.created_at) : 8.64e15;
      const tb = b.created_at ? Date.parse(b.created_at) : 8.64e15;
      return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
    });
    const blob = new Blob([JSON.stringify(liste, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'suno-liste.json';
    document.body.appendChild(a);
    a.click();
    a.remove();
    zeig('✅ ' + liste.length + ' Songs gefunden — die Datei "suno-liste.json" liegt jetzt in deinem Downloads-Ordner.');
    zeig('   Aeltester Song: ' + (liste[0] ? liste[0].title : '-'), '#666');
    zeig('   Neuester Song : ' + (liste.length ? liste[liste.length - 1].title : '-'), '#666');
    return liste.length;
  };
  window.sunoSpeichern = speichern;

  // --- Schritt 1: Anmeldung pruefen -----------------------------------------
  if (!window.Clerk || !window.Clerk.session) {
    zeig('❗ Auf dieser Seite ist keine Suno-Anmeldung sichtbar.', '#c00');
    zeig('   Bitte zuerst https://suno.com/me oeffnen und dann erneut einfuegen.', '#c00');
    return;
  }
  zeig('🔎 Bibliothek wird gelesen — bitte kurz warten …', '#06c');

  // --- Schritt 2: API Seite fuer Seite durchblaettern ------------------------
  const hosts = ['https://studio-api.prod.suno.com', 'https://studio-api.suno.ai'];
  const pfade = ['/api/feed/v2', '/api/feed/', '/api/project/default/clips'];

  for (const host of hosts) {
    for (const pfad of pfade) {
      const vorher = songs.size;
      let leer = 0;

      for (let seite = 0; seite < 500 && leer < 2; seite++) {
        let antwort;
        try {
          antwort = await fetch(host + pfad + (pfad.includes('?') ? '&' : '?') + 'page=' + seite, {
            headers: { Authorization: 'Bearer ' + (await holToken()), Accept: 'application/json' },
          });
        } catch (e) { break; }

        if (!antwort.ok) break;
        const vor = songs.size;
        try { ernte(await antwort.json()); } catch (e) { break; }

        if (songs.size === vor) leer++;
        else { leer = 0; console.log('   … ' + songs.size + ' Songs'); }
      }
      if (songs.size > vorher) { speichern(); return; }
    }
  }

  // --- Schritt 3: Notweg, falls die API sich geaendert hat -------------------
  zeig('⚠️ Der direkte Weg hat nicht funktioniert — jetzt der Notweg:', '#c60');
  zeig('   1. Scrolle deine Bibliothek langsam bis ganz nach unten.', '#c60');
  zeig('   2. Tippe danach hier in die Konsole:  sunoSpeichern()', '#c60');

  const originalFetch = window.fetch;
  window.fetch = async function (...args) {
    const antwort = await originalFetch.apply(this, args);
    try {
      const kopie = antwort.clone();
      if ((kopie.headers.get('content-type') || '').includes('json')) {
        kopie.json().then((daten) => {
          const vor = songs.size;
          ernte(daten);
          if (songs.size > vor) console.log('   … ' + songs.size + ' Songs');
        }).catch(() => {});
      }
    } catch (e) { /* egal */ }
    return antwort;
  };
})();
