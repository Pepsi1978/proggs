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
        await warte(1500 * (v + 1));
      }
    }
    return { ok: false, reason: 'keine_antwort' };
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
  // Songs, deren Datei auf der Platte fehlt: sie werden geholt, egal wie alt sie sind.
  const fehlt = new Set(hallo.fehlt || []);
  const grenze = hallo.neuester ? Date.parse(hallo.neuester) : null;
  const limit = hallo.limit || null;
  zeig('🔗 Mit dem Downloader verbunden. Er kennt bereits ' + bekannt.size + ' Songs.', '#06c');
  if (fehlt.size) zeig('   ' + fehlt.size + ' Dateien fehlen auf der Platte — sie werden mitgesucht.', '#c60');
  if (limit) zeig('   Probelauf: es werden höchstens ' + limit + ' Songs geholt.', '#666');

  // ------------------------------------------------------------- Songliste lesen
  const gefunden = new Map(); // id -> Song, nur eigene und nur unbekannte
  let ich = null;
  let ende = false;

  // Seit September 2026: Der Feldname für die Freischaltung hat schon gewechselt —
  // darum mehrere Schreibweisen prüfen und beim ersten Clip alle Kandidaten loggen.
  let feldLogSchon = false;
  const istFreigeschaltet = (c) => {
    if (!feldLogSchon) {
      feldLogSchon = true;
      const kandidaten = Object.keys(c || {}).filter((k) => /download|unlock|lock/i.test(k));
      console.log('   Freischalt-Felder im Feed: ' + (kandidaten.length ? kandidaten.join(', ') : '(keine)') +
        ' | is_download_unlocked=' + c.is_download_unlocked + ' is_public=' + c.is_public);
    }
    return c.is_download_unlocked === true || c.download_unlocked === true ||
      c.isDownloadUnlocked === true || c.downloadUnlocked === true ||
      c.is_unlocked === true || c.unlocked === true;
  };

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
      const frei = istFreigeschaltet(c);
      // Zeitgrenze gilt nur für noch gesperrte Songs: wer von Hand freigeschaltet hat,
      // will auch alte Songs holen — die lägen sonst ewig hinter der Grenze.
      if (!frei && grenze && c.created_at && Date.parse(c.created_at) <= grenze && !fehlt.has(c.id)) continue;

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
        // Seit September 2026: erst freischalten, dann gibt es einen Download-Link.
        freigeschaltet: frei,
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
      void neu; void nochGesucht; void ohneNeues;
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
  zeig('📄 ' + liste.length + ' Songs zu holen.', '#06c');

  // ------------------------------------------------------------- Freischalten
  /**
   * Seit September 2026 gibt /api/download/clip nur noch für freigeschaltete Songs
   * einen Link ("not_authorized" sonst). Freischalten heißt POST /api/download/authorize
   * und kostet einen Download aus dem Monatskontingent des Abos (Premier: 60 im Monat
   * plus gekaufte Zusatz-Downloads). Freigeschaltet wird älteste zuerst — dieselbe
   * Reihenfolge wie die Nummerierung — bis das Kontingent erschöpft ist. Der Rest
   * bleibt liegen und wird beim nächsten Lauf nach der Erneuerung geholt.
   */
  const kontingent = async () => {
    const b = await api('/api/billing/info/', 3);
    const u = b && b.download_usage;
    if (!u) return null;
    const frei =
      Math.max(0, (u.current_period_downloads_limit || 0) - (u.current_period_downloads_used || 0)) +
      (u.additional_download_remaining || 0);
    const erneuert = b.period_end || b.renews_on || null;
    return { frei, erneuert };
  };
  const datum = (iso) => {
    if (!iso) return 'unbekannt';
    const d = new Date(iso);
    return isNaN(d) ? String(iso) : d.toLocaleDateString('de-DE');
  };

  const gesperrt = liste.filter((s) => !s.freigeschaltet);
  let freigeschaltet = 0;
  let uebrig = gesperrt.length;
  if (gesperrt.length && hallo.freischalten === true) {
    const k = await kontingent();
    if (!k) {
      zeig('❗ Download-Kontingent konnte nicht gelesen werden — es wird trotzdem versucht.', '#c60');
    } else {
      zeig('🔓 ' + gesperrt.length + ' Songs sind noch nicht freigeschaltet. Kontingent: ' + k.frei + ' Downloads frei, neues am ' + datum(k.erneuert) + '.', '#06c');
    }
    const budget = k ? Math.min(k.frei, gesperrt.length) : gesperrt.length;
    let erschoepft = false;
    for (let i = 0; i < budget && !erschoepft; i += 5) {
      const schub = gesperrt.slice(i, Math.min(i + 5, budget));
      const antworten = await Promise.all(
        schub.map((s) => apiPost('/api/download/authorize', { item_id: s.id, item_type: 'clip' })),
      );
      for (let j = 0; j < schub.length; j++) {
        const a = antworten[j];
        if (a && a.ok) {
          schub[j].freigeschaltet = true;
          freigeschaltet++;
        } else {
          const grund = (a && (a.reason || a.message)) || 'unbekannt';
          console.log('   ❗ Freischalten fehlgeschlagen für ' + (schub[j].title || schub[j].id) + ': ' + grund);
          if (/limit|quota|exceed|insufficient|no_download|remaining/i.test(grund)) erschoepft = true;
        }
      }
      console.log('   Freigeschaltet: ' + freigeschaltet + ' von ' + gesperrt.length);
    }
    uebrig = gesperrt.length - freigeschaltet;
    const text =
      freigeschaltet + ' Songs freigeschaltet' +
      (uebrig ? ', ' + uebrig + ' bleiben gesperrt (Kontingent erschöpft — neues am ' + datum(k && k.erneuert) + ')' : '') +
      '.';
    zeig((uebrig ? '⚠️ ' : '✅ ') + text, uebrig ? '#c60' : '#0a0');
    await anDownloader('/kontingent', {
      frei: k ? k.frei - freigeschaltet : null,
      gesperrt: uebrig,
      freigeschaltet,
      erneuert: k ? k.erneuert : null,
      text: 'Freischaltung: ' + text,
    });
  } else if (gesperrt.length) {
    zeig('⏭️ ' + gesperrt.length + ' nicht freigeschaltete Songs werden übersprungen — bitte auf der Suno-Seite von Hand freischalten.', '#c60');
  }

  liste = liste.filter((s) => s.freigeschaltet);
  if (!liste.length && gefunden.size) {
    // Sicherheitsnetz: Falls Suno den Feldnamen erneut geändert hat und deshalb kein
    // einziger Song als freigeschaltet erkannt wurde, den Link-Endpunkt entscheiden
    // lassen statt alles zu verwerfen — gesperrte meldet er mit "not_authorized".
    zeig('⚠️ Kein Song als freigeschaltet erkannt — versuche Links für alle ' + gefunden.size + ' (Suno entscheidet).', '#c60');
    liste = [...gefunden.values()].sort((a, b) => {
      const ta = a.created_at ? Date.parse(a.created_at) : 8.64e15;
      const tb = b.created_at ? Date.parse(b.created_at) : 8.64e15;
      return ta === tb ? a.id.localeCompare(b.id) : ta - tb;
    });
    if (limit) liste = liste.slice(-limit);
  }
  if (!liste.length) {
    zeig('❗ Kein freigeschalteter Song übrig — nichts zu laden.', '#c00');
    await anDownloader('/fertig', { gesamt: 0 });
    return;
  }
  zeig('🔑 Jetzt die Download-Links für ' + liste.length + ' Songs …', '#06c');

  // ------------------------------------------------------------- Links holen
  /**
   * Ein Abruf von /api/download/clip/<id> stößt die Aufbereitung an und liefert
   * entweder gleich den Link oder "processing". Statt pro Song zu warten, wird
   * ein ganzer Schub angestoßen und danach erneut abgefragt — bis alle da sind.
   */
  const linkeHolen = async (ids) => {
    const ergebnis = new Map();
    let offen = ids.slice();
    // Seit September 2026 antwortet Suno oft mit ok:false / "not_authorized" — dann
    // gibt es keinen signierten Link, egal wie oft man fragt. Der Downloader nimmt
    // in dem Fall die m4a aus media_urls; hier wird nur nicht sinnlos gewartet.
    let verweigert = 0;
    for (let runde = 0; runde < 8 && offen.length; runde++) {
      const naechste = [];
      for (let i = 0; i < offen.length; i += LINKS_GLEICH) {
        const schub = offen.slice(i, i + LINKS_GLEICH);
        const antworten = await Promise.all(
          schub.map(async (id) => {
            const d = await api('/api/download/clip/' + id, 4);
            return {
              id,
              url: d && d.download_url ? d.download_url : null,
              endgueltig: !d || d.ok === false,
            };
          }),
        );
        for (const a of antworten) {
          if (a.url) ergebnis.set(a.id, a.url);
          else if (a.endgueltig) verweigert++;
          else naechste.push(a.id);
        }
        console.log('   Links: ' + ergebnis.size + ' von ' + ids.length + (verweigert ? ' (' + verweigert + ' verweigert)' : ''));
      }
      offen = naechste;
      if (offen.length) await warte(2500); // die Aufbereitung braucht einen Moment
    }
    if (verweigert) {
      zeig('   ' + verweigert + ' Links hat Suno verweigert ("not_authorized") — dafür nimmt der Downloader die m4a-Datei.', '#c60');
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
  // Ohne Link würde der Download nur in einem Fehler enden — also gar nicht erst übergeben.
  const ohneLink = liste.filter((s) => !s.download_url);
  if (ohneLink.length) {
    zeig('⚠️ ' + ohneLink.length + ' Songs ohne Link bleiben liegen (beim nächsten Lauf nochmal).', '#c60');
    liste = liste.filter((s) => s.download_url);
  }
  if (!liste.length) {
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
