/**
 * Brücke zwischen dem angemeldeten Chrome und dem Downloader auf 127.0.0.1.
 *
 * Wird einmal in die Konsole des angemeldeten Chrome eingefügt. Der Anmelde-Nachweis
 * bleibt dabei im Browser — an den Downloader gehen nur Songliste und die von Suno
 * ausgestellten, zeitlich begrenzten Download-Links.
 *
 * Grundregel seit September 2026: Es wird NICHTS selbst freigeschaltet. Wer entscheidet,
 * ob ein Song geladen wird, ist Suno selbst — /api/download/clip/<id> gibt für einen
 * freigeschalteten Song einen signierten Link heraus und für alle anderen
 * "not_authorized". Es wird also nicht mehr geraten, welches Feld im Feed die
 * Freischaltung anzeigt (die Feldnamen haben schon zweimal gewechselt); gefragt wird
 * die Stelle, die es wirklich weiß. Der Abruf des Links kostet nichts — nur
 * /api/download/authorize verbraucht Kontingent, und das läuft nur mit --freischalten.
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
  // Sicherheitsnetz gegen eine endlos antwortende Seite.
  const MAX_SEITEN = 900;

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
        if (!r.ok) {
          // 403/404 heißt bei /api/download/clip: nicht freigeschaltet. Das ist eine
          // endgültige Antwort und kein Grund, es fünfmal zu wiederholen. Beim Feed
          // gilt das NICHT — dort wäre ein übersprungener Fehlschlag eine Lücke in
          // der Songliste, also wird dort weiter wiederholt.
          if (r.status >= 400 && r.status < 500 && pfad.startsWith('/api/download/clip')) {
            try {
              const d = await r.json();
              return d && typeof d === 'object' ? { ...d, ok: false } : { ok: false, reason: 'http_' + r.status };
            } catch (e) {
              return { ok: false, reason: 'http_' + r.status };
            }
          }
          return null;
        }
        return await r.json();
      } catch (e) {
        await warte(1500 * (v + 1));
      }
    }
    return null;
  };

  /** POST an Suno — liefert immer ein Objekt, bei Fehlern {ok:false, reason}. */
  const apiPost = async (pfad, body) => {
    for (let v = 0; v < 4; v++) {
      try {
        const r = await fetch(HOST + pfad, {
          method: 'POST',
          headers: {
            Authorization: 'Bearer ' + (await holeToken()),
            Accept: 'application/json',
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(body),
        });
        if (r.status === 429) {
          await warte(2000 + v * 2000);
          continue;
        }
        if (r.status === 401) {
          token = null;
          continue;
        }
        const text = await r.text();
        try {
          const d = JSON.parse(text);
          return typeof d === 'object' && d ? { ...d, ok: d.ok !== false && r.ok } : { ok: r.ok };
        } catch (e) {
          return { ok: r.ok, reason: 'http_' + r.status };
        }
      } catch (e) {
        return { ok: false, reason: 'netzfehler' };
      }
    }
    return { ok: false, reason: 'keine_antwort' };
  };

  const anDownloader = async (pfad, daten, still) => {
    try {
      const r = await fetch(BASIS + pfad, {
        method: daten ? 'POST' : 'GET',
        headers: daten ? { 'Content-Type': 'application/json' } : undefined,
        body: daten ? JSON.stringify(daten) : undefined,
      });
      return await r.json();
    } catch (e) {
      if (!still) zeig('❗ Der Downloader antwortet nicht (' + BASIS + '). Läuft das Programm noch?', '#c00');
      return null;
    }
  };
  /** Lebenszeichen an den Downloader — sonst hält der ihn für abgestürzt. */
  const puls = (text) => anDownloader('/puls', { text }, true);

  // ------------------------------------------------------------- anmelden
  const hallo = await anDownloader('/start');
  if (!hallo) return;
  const bekannt = new Set(hallo.bekannt || []);
  // Songs, deren Datei auf der Platte fehlt: sie werden geholt, egal wie alt sie sind.
  const fehlt = new Set(hallo.fehlt || []);
  const limit = hallo.limit || null;
  zeig('🔗 Mit dem Downloader verbunden. Er kennt bereits ' + bekannt.size + ' Songs.', '#06c');
  if (fehlt.size) zeig('   ' + fehlt.size + ' Dateien fehlen auf der Platte — sie werden mitgesucht.', '#c60');
  if (limit) zeig('   Probelauf: es werden höchstens ' + limit + ' Songs geholt.', '#666');

  // ------------------------------------------------------------- Songliste lesen
  const gefunden = new Map(); // id -> Song, nur eigene und nur unbekannte
  let ich = null;

  // Der Feldname für die Freischaltung im Feed hat schon zweimal gewechselt und ist
  // darum keine Entscheidungsgrundlage mehr. Er wird nur noch einmal protokolliert,
  // damit man sieht, was Suno gerade liefert — entschieden wird über den Link-Abruf.
  let feldLogSchon = false;
  const feldHinweis = (c) => {
    if (feldLogSchon) return;
    feldLogSchon = true;
    const kandidaten = Object.keys(c || {}).filter((k) => /download|unlock|lock/i.test(k));
    console.log(
      '   (Info) Freischalt-Felder im Feed: ' + (kandidaten.length ? kandidaten.join(', ') : '(keine)') +
        ' — entschieden wird trotzdem über /api/download/clip.',
    );
  };

  /** Liest eine Feed-Seite und trägt die eigenen, noch unbekannten Songs ein. */
  const seiteLesen = async (nr) => {
    const daten = await api('/api/feed/v2?page=' + nr + '&page_size=20');
    if (!daten || !Array.isArray(daten.clips)) {
      // Eine Seite, die gar nicht durchkam, ist eine Lücke in der Songliste — das
      // muss man sehen, sonst fehlt hinterher unerklärlich ein Song.
      console.log('%c   ⚠️ Seite ' + nr + ' war nicht lesbar — mit sunoWeiter(' + nr + ') nachholen.', 'color:#c60');
      return { leer: true, neu: 0, weiter: true };
    }

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
      feldHinweis(c);
      // Alles, was schon als fertige Datei auf der Platte liegt, wird übersprungen.
      // Was fehlt, steckt nicht in "bekannt" und kommt darum hier durch.
      if (bekannt.has(c.id) || gefunden.has(c.id)) continue;

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

  /**
   * Liest die Bibliothek von vorn bis hinten durch. Es gibt bewusst keinen Frühstopp
   * mehr: ein Song, den du gestern von Hand freigeschaltet hast, kann drei Jahre alt
   * sein und liegt dann tief in der Liste. Ein Durchlauf über gut 150 Seiten dauert
   * eine knappe halbe Minute — das ist der Preis dafür, nichts zu verpassen.
   */
  const listeLesen = async (abSeite) => {
    let seite = abSeite || 0;
    zeig('🔎 Die Songliste wird gelesen …', '#06c');

    while (seite < MAX_SEITEN) {
      const block = [];
      for (let i = 0; i < SEITEN_GLEICH; i++) block.push(seite + i);
      const ergebnisse = await Promise.all(block.map(seiteLesen));
      seite += SEITEN_GLEICH;

      console.log('   Seiten bis ' + seite + ' → ' + gefunden.size + ' noch nicht gesicherte Songs');
      await puls('Songliste: Seite ' + seite + ', ' + gefunden.size + ' noch nicht gesichert');

      if (ergebnisse.every((e) => e.leer)) break;
      // Suno meldet auf der letzten Seite has_more:false — dann ist Schluss.
      if (ergebnisse.every((e) => e.weiter === false)) break;
      if (limit && gefunden.size >= limit) break;
      await warte(200);
    }
    return gefunden.size;
  };
  window.sunoWeiter = listeLesen;
  window.sunoStand = () => gefunden.size;

  await listeLesen(0);

  const nachAlter = (a, b) => {
    const ta = a.created_at ? Date.parse(a.created_at) : 8.64e15;
    const tb = b.created_at ? Date.parse(b.created_at) : 8.64e15;
    return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
  };

  let liste = [...gefunden.values()].sort(nachAlter);
  if (limit) liste = liste.slice(-limit); // beim Probelauf die neuesten nehmen

  if (!liste.length) {
    zeig('✅ Nichts Neues — der Downloader hat bereits alles.', '#0a0');
    await anDownloader('/fertig', { gesamt: 0 });
    return;
  }
  zeig('📄 ' + liste.length + ' Songs sind noch nicht gesichert. Jetzt fragen, welche davon freigeschaltet sind …', '#06c');

  // ------------------------------------------------------------- Links holen
  /**
   * /api/download/clip/<id> ist die einzige verlässliche Auskunft darüber, ob ein Song
   * freigeschaltet ist: freigeschaltet → signierter Link, sonst "not_authorized".
   * Ein Abruf stößt zugleich die Aufbereitung an; kommt "processing" zurück, wird in
   * der nächsten Runde erneut gefragt. Eine endgültige Absage wird sofort erkannt und
   * nicht acht Runden lang wiederholt — genau daran hing das ewige Warten.
   */
  const ENDGUELTIG = /not_?auth|unauthor|forbidden|not_found|no_?access|denied|payment|subscription/i;
  const linkeHolen = async (ids, wortmeldung) => {
    const ergebnis = new Map();
    const abgelehnt = new Map(); // id -> Grund
    let offen = ids.slice();

    for (let runde = 0; runde < 6 && offen.length; runde++) {
      const naechste = [];
      for (let i = 0; i < offen.length; i += LINKS_GLEICH) {
        const schub = offen.slice(i, i + LINKS_GLEICH);
        const antworten = await Promise.all(
          schub.map(async (id) => {
            const d = await api('/api/download/clip/' + id, 3);
            const url = d && typeof d.download_url === 'string' && d.download_url ? d.download_url : null;
            const grund = String((d && (d.reason || d.detail || d.message || d.error)) || '');
            return {
              id,
              url,
              // Endgültig heißt: fragen bringt nichts mehr. Entweder sagt Suno das
              // ausdrücklich (ok:false / not_authorized) oder es kommt gar nichts.
              endgueltig: !d || d.ok === false || ENDGUELTIG.test(grund),
              grund: grund || 'nicht freigeschaltet',
            };
          }),
        );
        for (const a of antworten) {
          if (a.url) ergebnis.set(a.id, a.url);
          else if (a.endgueltig) abgelehnt.set(a.id, a.grund);
          else naechste.push(a.id);
        }
        const zeile =
          '   Links: ' + ergebnis.size + ' von ' + ids.length +
          (abgelehnt.size ? ' (' + abgelehnt.size + ' nicht freigeschaltet)' : '') +
          (naechste.length ? ' · ' + naechste.length + ' in Arbeit' : '');
        console.log(zeile);
        await puls((wortmeldung || 'Links') + ': ' + ergebnis.size + ' von ' + ids.length);
      }
      offen = naechste;
      if (offen.length) await warte(2500); // die Aufbereitung braucht einen Moment
    }
    // Was nach der letzten Runde immer noch offen ist, wird wie abgelehnt behandelt —
    // sonst wartet hier jemand auf etwas, das nicht mehr kommt.
    for (const id of offen) if (!abgelehnt.has(id)) abgelehnt.set(id, 'kein Link nach 6 Runden');
    return { links: ergebnis, abgelehnt };
  };

  const beginn = Date.now();
  let { links, abgelehnt } = await linkeHolen(liste.map((s) => s.id), 'Links');

  // ------------------------------------------------------------- Freischalten (nur --freischalten)
  /**
   * Standardweg: Es wird NICHTS freigeschaltet. Freischalten heißt POST
   * /api/download/authorize und kostet einen Download aus dem Monatskontingent des
   * Abos. Nur mit --freischalten wird das gemacht — und dann ausschließlich für
   * Songs, für die Suno gerade eben keinen Link herausgerückt hat. So wird nie
   * Kontingent für einen Song verbrannt, der ohnehin schon frei war.
   */
  const datum = (iso) => {
    if (!iso) return 'unbekannt';
    const d = new Date(iso);
    return isNaN(d.getTime()) ? String(iso) : d.toLocaleDateString('de-DE');
  };

  if (hallo.freischalten === true && abgelehnt.size) {
    const kontingent = async () => {
      const b = await api('/api/billing/info/', 3);
      const u = b && b.download_usage;
      if (!u) return null;
      const frei =
        Math.max(0, (u.current_period_downloads_limit || 0) - (u.current_period_downloads_used || 0)) +
        (u.additional_download_remaining || 0);
      return { frei, erneuert: b.period_end || b.renews_on || null };
    };

    const gesperrt = liste.filter((s) => abgelehnt.has(s.id)); // schon nach Alter sortiert
    const k = await kontingent();
    if (!k) zeig('❗ Download-Kontingent konnte nicht gelesen werden — es wird trotzdem versucht.', '#c60');
    else
      zeig(
        '🔓 ' + gesperrt.length + ' Songs sind gesperrt. Kontingent: ' + k.frei +
          ' Downloads frei, neues am ' + datum(k.erneuert) + '.',
        '#06c',
      );

    const budget = k ? Math.min(k.frei, gesperrt.length) : gesperrt.length;
    const frisch = [];
    let erschoepft = false;
    for (let i = 0; i < budget && !erschoepft; i += 5) {
      const schub = gesperrt.slice(i, Math.min(i + 5, budget));
      const antworten = await Promise.all(
        schub.map((s) => apiPost('/api/download/authorize', { item_id: s.id, item_type: 'clip' })),
      );
      for (let j = 0; j < schub.length; j++) {
        const a = antworten[j];
        if (a && a.ok) {
          frisch.push(schub[j].id);
        } else {
          const grund = (a && (a.reason || a.message || a.detail)) || 'unbekannt';
          console.log('   ❗ Freischalten fehlgeschlagen für ' + (schub[j].title || schub[j].id) + ': ' + grund);
          if (/limit|quota|exceed|insufficient|no_download|remaining/i.test(grund)) erschoepft = true;
        }
      }
      console.log('   Freigeschaltet: ' + frisch.length + ' von ' + gesperrt.length);
      await puls('Freischalten: ' + frisch.length + ' von ' + gesperrt.length);
    }

    if (frisch.length) {
      const nachschlag = await linkeHolen(frisch, 'Links (neu freigeschaltet)');
      for (const [id, url] of nachschlag.links) {
        links.set(id, url);
        abgelehnt.delete(id);
      }
    }
    const uebrig = gesperrt.length - frisch.length;
    const text =
      frisch.length + ' Songs freigeschaltet' +
      (uebrig ? ', ' + uebrig + ' bleiben gesperrt (Kontingent erschöpft — neues am ' + datum(k && k.erneuert) + ')' : '') +
      '.';
    zeig((uebrig ? '⚠️ ' : '✅ ') + text, uebrig ? '#c60' : '#0a0');
    await anDownloader('/kontingent', {
      frei: k ? k.frei - frisch.length : null,
      gesperrt: uebrig,
      freigeschaltet: frisch.length,
      erneuert: k ? k.erneuert : null,
      text: 'Freischaltung: ' + text,
    });
  }

  const dauer = Math.round((Date.now() - beginn) / 1000);
  for (const s of liste) {
    const u = links.get(s.id);
    if (u) s.download_url = u;
  }
  const ohneLink = liste.filter((s) => !s.download_url);
  liste = liste.filter((s) => s.download_url);

  zeig(
    '🔑 ' + liste.length + ' von ' + (liste.length + ohneLink.length) + ' Songs sind freigeschaltet (' + dauer + ' s).',
    ohneLink.length ? '#c60' : '#0a0',
  );

  if (ohneLink.length) {
    // Der Benutzer soll sehen, WAS er noch von Hand freischalten müsste.
    zeig(
      '⏭️ ' + ohneLink.length + ' Songs sind nicht freigeschaltet und bleiben liegen. ' +
        'Auf suno.com von Hand freischalten, dann beim nächsten Lauf holen.',
      '#c60',
    );
    for (const s of ohneLink.slice(0, 10)) {
      console.log('      · ' + (s.title || '(ohne Titel)') + '  [' + (abgelehnt.get(s.id) || 'gesperrt') + ']');
    }
    if (ohneLink.length > 10) console.log('      … und ' + (ohneLink.length - 10) + ' weitere.');
    await anDownloader('/kontingent', {
      gesperrt: ohneLink.length,
      text:
        ohneLink.length + ' Songs sind nicht freigeschaltet und wurden übersprungen ' +
        '(auf suno.com freischalten, dann erneut starten).',
    });
  }

  if (!liste.length) {
    zeig('❗ Kein freigeschalteter Song übrig — nichts zu laden.', '#c00');
    await anDownloader('/fertig', { gesamt: 0 });
    return;
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
    const auftrag = await anDownloader('/auftrag', null, true);
    if (!auftrag) {
      // Der Downloader hat den Server geschlossen — er ist fertig.
      zeig('✅ Der Downloader hat sich beendet.', '#0a0');
      break;
    }
    if (auftrag.fertig) {
      zeig('✅ Der Downloader ist fertig: ' + auftrag.stand.geladen + ' geladen, ' + auftrag.stand.fehler + ' Fehler.', '#0a0');
      break;
    }
    if (auftrag.ids && auftrag.ids.length) {
      stillstand = 0;
      const frisch = await linkeHolen(auftrag.ids, 'Nachschub');
      const paket = {};
      for (const [id, url] of frisch.links) paket[id] = url;
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
})().catch(async (fehler) => {
  // Ohne diesen Fang stirbt das Skript still und der Downloader wartet für immer
  // auf ein Ergebnis, das nie kommt. Genau das war der Fehler "wartet ewig".
  const text = String((fehler && fehler.stack) || fehler);
  console.log('%c❗ Das Brücken-Skript ist gestolpert — der Downloader wird benachrichtigt.', 'font-size:15px;font-weight:bold;color:#c00');
  console.error(fehler);
  try {
    await fetch('http://127.0.0.1:__PORT__/fehler', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
    });
  } catch (e) {
    /* dann muss der Downloader in seinen Zeitablauf laufen */
  }
});
