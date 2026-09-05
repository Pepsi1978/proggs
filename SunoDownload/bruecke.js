/**
 * Brücke zwischen dem angemeldeten Chrome und dem Downloader auf 127.0.0.1.
 *
 * Wird einmal in die Konsole des angemeldeten Chrome eingefügt. Der Anmelde-Nachweis
 * bleibt dabei im Browser — an den Downloader gehen nur Songliste und die von Suno
 * ausgestellten, zeitlich begrenzten Download-Links.
 *
 * ---------------------------------------------------------------------------
 * Gemessen am 05.09.2026 an der echten Bibliothek (3247 Songs). Die Zahlen hier
 * sind nachgemessen, nicht geschätzt:
 *
 * 1) WOHER DIE SONGLISTE KOMMT.  /api/feed/v2 meldete "num_total_results: 21" —
 *    der Feed zeigt nur die letzten Erzeugungen, nicht die Bibliothek. Dort stand
 *    also nie mehr als ein Bruchteil der Songs. Die vollständige Bibliothek liegt
 *    in /api/project/default: "clip_count" nennt die Gesamtzahl, die Songs stehen
 *    als project_clips[].clip. Die Seiten sind EINS-basiert (page=0 liefert
 *    dieselbe Seite wie page=1), page_size wird ignoriert — es sind immer 20 pro
 *    Seite. 8 Seiten gleichzeitig liefen über 170 Seiten ohne einen Fehlschlag.
 *
 * 2) WORAN MAN DIE FREISCHALTUNG ERKENNT.  is_download_unlocked im Clip stimmt
 *    (10 von 3247 auf true, darunter genau der vom Benutzer genannte Song). Es
 *    wird als Vorfilter benutzt, das letzte Wort hat aber der Link-Abruf. Fehlt
 *    das Feld einmal ganz — Suno hat den Namen schon gewechselt —, schaltet die
 *    Brücke von selbst auf Nachfragen für jeden Song um.
 *
 * 3) WIE STARK SUNO BREMST.  /api/download/clip verträgt KEINE gleichzeitigen
 *    Abrufe mehr: 4 auf einmal ergaben 3-mal "rate_limited". Nacheinander mit
 *    1,5 s Abstand ging jeder durch. Darum wird sequenziell und mit selbst
 *    nachregelnder Pause gefragt. Die drei Antworten sind:
 *      {ok:true,  download_url, status:"ready"}   → freigeschaltet
 *      {ok:false, reason:"not_authorized"}        → gesperrt, endgültig
 *      {ok:false, reason:"rate_limited"}          → zu schnell, WIEDERHOLEN
 *    Die letzten beiden auseinanderzuhalten ist entscheidend: "rate_limited" als
 *    Absage zu werten hieße, freigeschaltete Songs stillschweigend liegen zu lassen.
 * ---------------------------------------------------------------------------
 *
 * Freigeschaltet wird nichts von selbst — das macht der Benutzer auf suno.com.
 * Nur mit --freischalten schaltet die Brücke zusätzlich frei; das verbraucht
 * Kontingent aus dem Abo.
 *
 * Von Hand jederzeit möglich:
 *   sunoStand()        wie weit ist es
 *   sunoWeiter(42)     ab Seite 42 weiterlesen
 */
(async () => {
  const BASIS = 'http://127.0.0.1:__PORT__';
  const HOST = 'https://studio-api.prod.suno.com';
  const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  /** Gemessen: 8 Bibliotheks-Seiten gleichzeitig laufen über 170 Seiten fehlerfrei. */
  const SEITEN_GLEICH = 8;
  /** Suno liefert immer 20 Songs je Seite; page_size wird ignoriert. */
  const JE_SEITE = 20;
  /** Gemessen: unter 1,5 s Abstand antwortet der Link-Endpunkt mit "rate_limited". */
  const LINK_PAUSE_MIN = 1500;
  const LINK_PAUSE_MAX = 8000;
  /** Notbremse gegen eine Bibliothek, die kein Ende meldet. */
  const MAX_SEITEN = 1200;

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
          // Beim Link-Endpunkt ist ein 4xx eine endgültige Auskunft; der Grund aus der
          // Antwort wird mitgenommen. Bei der Bibliothek wird dagegen weiter
          // wiederholt — eine übersprungene Seite wäre eine Lücke in der Songliste.
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
  let gesamtInBibliothek = 0;

  /**
   * Sagt Sunos Songliste überhaupt etwas über die Freischaltung? Sobald ein Clip das
   * Feld mitbringt (egal ob true oder false), wird danach vorgefiltert. Fehlt es auf
   * allen Clips — weil Suno es wieder umbenannt hat —, wird stattdessen für jeden
   * Song einzeln nachgefragt. So kippt ein Namenswechsel nie den ganzen Lauf.
   */
  const UNLOCK_FELDER = [
    'is_download_unlocked', 'download_unlocked', 'isDownloadUnlocked',
    'downloadUnlocked', 'is_unlocked', 'unlocked',
  ];
  let feldVorhanden = false;
  let feldLogSchon = false;

  const istFrei = (c) => {
    let frei = false;
    for (const f of UNLOCK_FELDER) {
      if (Object.prototype.hasOwnProperty.call(c, f)) {
        feldVorhanden = true;
        if (c[f] === true) frei = true;
      }
    }
    return frei;
  };

  const feldHinweis = (c) => {
    if (feldLogSchon) return;
    feldLogSchon = true;
    const kandidaten = Object.keys(c || {}).filter((k) => /download|unlock|lock/i.test(k));
    console.log('   (Info) Freischalt-Felder in der Songliste: ' + (kandidaten.length ? kandidaten.join(', ') : '(keine)'));
  };

  /** Liest eine Bibliotheks-Seite (1-basiert) und trägt die unbekannten Songs ein. */
  const seiteLesen = async (nr) => {
    const daten = await api('/api/project/default?page=' + nr);
    const eintraege = daten && Array.isArray(daten.project_clips) ? daten.project_clips : null;
    if (!eintraege) {
      // Eine Seite, die gar nicht durchkam, ist eine Lücke in der Songliste — das
      // muss man sehen, sonst fehlt hinterher unerklärlich ein Song.
      console.log('%c   ⚠️ Seite ' + nr + ' war nicht lesbar — mit sunoWeiter(' + nr + ') nachholen.', 'color:#c60');
      return { leer: true, neu: 0 };
    }
    if (daten.clip_count > gesamtInBibliothek) gesamtInBibliothek = daten.clip_count;

    let neu = 0;
    for (const e of eintraege) {
      const c = e && e.clip;
      if (!c || typeof c.id !== 'string' || !UUID.test(c.id)) continue;
      if (c.is_trashed === true) continue; // im Papierkorb — nicht sichern
      // In der Bibliothek können auch fremde Stücke liegen (Vorlagen von Coverversionen).
      const wer = c.handle || (c.user && c.user.handle);
      if (ich && typeof wer === 'string' && wer.toLowerCase() !== ich) continue;
      feldHinweis(c);
      const frei = istFrei(c);
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
        freigeschaltet: frei,
      });
      neu++;
    }
    return { leer: eintraege.length === 0, neu };
  };

  /**
   * Liest die Bibliothek von vorn bis hinten durch. Es gibt bewusst keinen Frühstopp:
   * ein Song, den du gestern von Hand freigeschaltet hast, kann drei Jahre alt sein
   * und liegt dann tief in der Liste.
   */
  const listeLesen = async (abSeite) => {
    // Seite 1 zuerst allein: sie nennt die Gesamtzahl und legt fest, wessen Songs
    // gesucht werden. Beides nebenbei aus parallelen Seiten zu ziehen wäre ein
    // Wettlauf — und träfe die Songliste ins Mark, wenn dabei ein Fremdstück gewinnt.
    const start = abSeite && abSeite > 1 ? abSeite : 1;
    if (!ich) {
      const erste = await api('/api/project/default?page=1');
      const eintraege = (erste && erste.project_clips) || [];
      const namen = new Map();
      for (const e of eintraege) {
        const wer = e && e.clip && (e.clip.handle || (e.clip.user && e.clip.user.handle));
        if (typeof wer === 'string' && wer) namen.set(wer.toLowerCase(), (namen.get(wer.toLowerCase()) || 0) + 1);
      }
      let beste = 0;
      for (const [name, n] of namen) {
        if (n > beste) {
          beste = n;
          ich = name;
        }
      }
      if (erste && erste.clip_count) gesamtInBibliothek = erste.clip_count;
    }

    const seitenGesamt = gesamtInBibliothek ? Math.ceil(gesamtInBibliothek / JE_SEITE) : MAX_SEITEN;
    zeig('🔎 Die Bibliothek wird gelesen — ' + (gesamtInBibliothek || '?') + ' Songs auf ' + seitenGesamt + ' Seiten …', '#06c');

    let seite = start;
    while (seite <= Math.min(seitenGesamt, MAX_SEITEN)) {
      const block = [];
      for (let i = 0; i < SEITEN_GLEICH && seite + i <= seitenGesamt; i++) block.push(seite + i);
      if (!block.length) break;
      const ergebnisse = await Promise.all(block.map(seiteLesen));
      seite += block.length;

      console.log('   Seite ' + (seite - 1) + ' von ' + seitenGesamt + ' → ' + gefunden.size + ' noch nicht gesichert');
      await puls('Bibliothek: Seite ' + (seite - 1) + ' von ' + seitenGesamt + ', ' + gefunden.size + ' noch nicht gesichert');

      if (ergebnisse.every((e) => e.leer)) break;
      if (limit && gefunden.size >= limit) break;
      await warte(150);
    }
    return gefunden.size;
  };
  window.sunoWeiter = listeLesen;
  window.sunoStand = () => gefunden.size;

  await listeLesen(1);

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

  // ------------------------------------------------------------- Vorauswahl
  /**
   * Gefragt wird nur für Songs, die überhaupt Aussicht auf einen Link haben. Der
   * Link-Endpunkt verträgt nur einen Abruf je anderthalb Sekunden — für alle 3200
   * gesperrten Songs zu fragen dauerte über eine Stunde und liefe fast vollständig
   * in die Bremse. Sagt die Songliste nichts über die Freischaltung, wird trotzdem
   * jeder gefragt; dann ist Langsamkeit besser als ein leerer Lauf.
   */
  const alleFragen = hallo.alleFragen === true || !feldVorhanden;
  const kandidaten = alleFragen ? liste : liste.filter((s) => s.freigeschaltet);
  const vorgefiltert = liste.length - kandidaten.length;

  if (!feldVorhanden) {
    zeig('⚠️ Die Songliste nennt kein Freischalt-Feld mehr — es wird für jeden Song einzeln nachgefragt.', '#c60');
  }
  zeig('📄 ' + liste.length + ' Songs fehlen auf der Platte, davon sind ' + kandidaten.length + ' freigeschaltet.', '#06c');
  if (vorgefiltert) {
    console.log('   ' + vorgefiltert + ' gesperrte Songs werden gar nicht erst gefragt (auf suno.com freischalten).');
  }

  if (!kandidaten.length && hallo.freischalten !== true) {
    zeig('⏭️ Kein freigeschalteter Song dabei — nichts zu laden.', '#c60');
    await anDownloader('/kontingent', {
      gesperrt: liste.length,
      text: liste.length + ' fehlende Songs sind alle gesperrt. Auf suno.com freischalten, dann erneut starten.',
    });
    await anDownloader('/fertig', { gesamt: 0 });
    return;
  }

  // ------------------------------------------------------------- Links holen
  /**
   * Sequenziell mit selbst nachregelnder Pause: Bei "rate_limited" wird die Pause
   * um die Hälfte verlängert, nach mehreren glatten Abrufen wieder verkürzt. So
   * findet der Lauf die schnellste Geschwindigkeit, die Suno gerade durchlässt,
   * ohne sich festzufahren.
   */
  const GESPERRT = /not_?auth|unauthor|forbidden|no_?access|denied|payment|subscription|not_?found/i;
  const BREMSE = /rate_?limit|too_?many|slow_?down|throttl/i;

  let pause = LINK_PAUSE_MIN;
  const linkeHolen = async (ids, wortmeldung) => {
    const links = new Map();
    const abgelehnt = new Map(); // id -> Grund
    let offen = ids.slice();
    let glatt = 0;

    for (let runde = 0; runde < 4 && offen.length; runde++) {
      const naechste = [];
      for (let i = 0; i < offen.length; i++) {
        const id = offen[i];
        const d = await api('/api/download/clip/' + id, 3);
        const grund = String((d && (d.reason || d.detail || d.message || d.error)) || '');

        if (d && typeof d.download_url === 'string' && d.download_url) {
          links.set(id, d.download_url);
          if (++glatt >= 5 && pause > LINK_PAUSE_MIN) {
            pause = Math.max(LINK_PAUSE_MIN, Math.round(pause * 0.8));
            glatt = 0;
          }
        } else if (BREMSE.test(grund)) {
          // NICHT als Absage werten — genau hier gingen freigeschaltete Songs verloren.
          glatt = 0;
          pause = Math.min(LINK_PAUSE_MAX, Math.round(pause * 1.5));
          naechste.push(id);
        } else if (!d || d.ok === false || GESPERRT.test(grund)) {
          abgelehnt.set(id, grund || 'nicht freigeschaltet');
        } else {
          naechste.push(id); // "processing" — die Aufbereitung läuft noch
        }

        if ((i + 1) % 5 === 0 || i + 1 === offen.length) {
          console.log(
            '   ' + (wortmeldung || 'Links') + ': ' + links.size + ' von ' + ids.length +
              (abgelehnt.size ? ' · ' + abgelehnt.size + ' gesperrt' : '') +
              (naechste.length ? ' · ' + naechste.length + ' noch einmal' : '') +
              ' (Pause ' + pause + ' ms)',
          );
          await puls((wortmeldung || 'Links') + ': ' + links.size + ' von ' + ids.length);
        }
        await warte(pause);
      }
      offen = naechste;
      if (offen.length) {
        const wartezeit = (runde + 1) * 10;
        console.log('   Suno hat gebremst — ' + offen.length + ' Songs werden in ' + wartezeit + ' s erneut gefragt.');
        for (let w = 0; w < wartezeit; w += 5) {
          await warte(5000);
          await puls((wortmeldung || 'Links') + ': warte auf Sunos Bremse …');
        }
      }
    }
    for (const id of offen) if (!abgelehnt.has(id)) abgelehnt.set(id, 'Suno bremst dauerhaft');
    return { links, abgelehnt };
  };

  const beginn = Date.now();
  const ersteRunde = await linkeHolen(kandidaten.map((s) => s.id), 'Links');
  const links = ersteRunde.links;
  const abgelehnt = ersteRunde.abgelehnt;

  // ------------------------------------------------------------- Freischalten (nur --freischalten)
  /**
   * Standardweg: Es wird NICHTS freigeschaltet. Freischalten heißt POST
   * /api/download/authorize und kostet einen Download aus dem Monatskontingent des
   * Abos. Nur mit --freischalten wird das gemacht — und dann erst, nachdem der
   * Link-Abruf gelaufen ist, damit nie Kontingent für einen Song verbrannt wird,
   * der ohnehin schon frei war.
   */
  const datum = (iso) => {
    if (!iso) return 'unbekannt';
    const d = new Date(iso);
    return isNaN(d.getTime()) ? String(iso) : d.toLocaleDateString('de-DE');
  };

  if (hallo.freischalten === true) {
    const kontingent = async () => {
      const b = await api('/api/billing/info/', 3);
      const u = b && b.download_usage;
      if (!u) return null;
      const frei =
        Math.max(0, (u.current_period_downloads_limit || 0) - (u.current_period_downloads_used || 0)) +
        (u.additional_download_remaining || 0);
      return { frei, erneuert: b.period_end || b.renews_on || null };
    };

    // Alles, was keinen Link hat: die vorgefilterten Gesperrten plus die abgelehnten.
    const gesperrt = liste.filter((s) => !links.has(s.id)); // schon nach Alter sortiert
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

  zeig('🔑 ' + liste.length + ' Songs mit Download-Link (' + dauer + ' s).', liste.length ? '#0a0' : '#c60');

  if (ohneLink.length) {
    // Der Benutzer soll sehen, WAS er noch von Hand freischalten müsste.
    zeig('⏭️ ' + ohneLink.length + ' Songs bleiben liegen — auf suno.com freischalten, dann beim nächsten Lauf holen.', '#c60');
    for (const s of ohneLink.slice(0, 10)) {
      console.log('      · ' + (s.title || '(ohne Titel)') + '  [' + (abgelehnt.get(s.id) || 'gesperrt') + ']');
    }
    if (ohneLink.length > 10) console.log('      … und ' + (ohneLink.length - 10) + ' weitere.');
    if (hallo.freischalten !== true) {
      await anDownloader('/kontingent', {
        gesperrt: ohneLink.length,
        text: ohneLink.length + ' Songs sind nicht freigeschaltet und wurden übersprungen.',
      });
    }
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
  // auf ein Ergebnis, das nie kommt.
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
