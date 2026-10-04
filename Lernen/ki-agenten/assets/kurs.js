// Gemeinsame Bausteine für alle Lektionen: Quiz, Abruf-Aufgabe, Sprachumschalter.
//
// Quiz:
//   <div class="quiz" data-richtig="2">
//     <p class="frage">…</p>
//     <div class="antworten"><button>…</button><button>…</button><button>…</button></div>
//     <p class="erklaerung" hidden>…</p>
//   </div>
//   data-richtig = Nummer der richtigen Antwort, ab 1. Die Antworten werden bei jedem Laden gemischt.
//   <p class="quiz-stand"></p> zeigt den Punktestand der Seite.
//
// Abruf-Aufgabe (erst aus dem Kopf schreiben, dann vergleichen):
//   <div class="abruf"><p class="frage">…</p><textarea></textarea>
//     <button>Mit Lösung vergleichen</button><div class="loesung" hidden>…</div></div>
//
// Sprachumschalter:
//   <div class="sprachen"><pre data-sprache="TypeScript">…</pre><pre data-sprache="Kotlin">…</pre></div>
//   Die Wahl gilt für alle Umschalter und bleibt gespeichert.
(function () {
  var SEITE = location.pathname.split('/').pop();
  function lesen(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }
  function schreiben(k, v) { try { localStorage.setItem(k, v); } catch (e) {} }

  // ---- Ergebnis für den Agenten ----
  // Die Seite kann nichts an den Agenten zurückmelden. Deshalb steht am Ende jeder Lektion ein Text
  // mit Quiz-Ergebnis und Abruf-Antworten, den der Lerner kopiert und im Terminal einfügt.
  var berichtFeld = null;
  function bericht() {
    if (!berichtFeld) return;
    var z = ['Ergebnis ' + document.title + ' (' + SEITE + ')'];
    if (quizze.length) {
      z.push('Quiz: ' + beantwortet + ' von ' + quizze.length + ' beantwortet, ' + punkte + ' beim ersten Versuch richtig' +
        (falsche.length ? ', falsch: Frage ' + falsche.sort(function (a, b) { return a - b; }).join(', ') : ''));
    }
    document.querySelectorAll('.abruf textarea').forEach(function (f, i) {
      z.push('Abruf ' + (i + 1) + ': ' + (f.value.trim() || '(leer)'));
    });
    berichtFeld.value = z.join(String.fromCharCode(10));
  }

  // ---- Quiz ----
  var quizze = Array.prototype.slice.call(document.querySelectorAll('.quiz'));
  var punkte = 0, beantwortet = 0, falsche = [];
  function stand() {
    var el = document.querySelector('.quiz-stand');
    if (!el || !quizze.length) return;
    el.textContent = beantwortet < quizze.length
      ? beantwortet + ' von ' + quizze.length + ' Fragen beantwortet'
      : punkte + ' von ' + quizze.length + ' beim ersten Versuch richtig';
    if (beantwortet === quizze.length) schreiben('kurs-quiz-' + SEITE, punkte + '/' + quizze.length);
    bericht();
  }
  quizze.forEach(function (q) {
    var box = q.querySelector('.antworten');
    var knoepfe = Array.prototype.slice.call(box.querySelectorAll('button'));
    var richtig = knoepfe[parseInt(q.dataset.richtig, 10) - 1];
    knoepfe.sort(function () { return Math.random() - 0.5; }).forEach(function (b) { box.appendChild(b); });
    knoepfe.forEach(function (b) {
      b.type = 'button';
      b.addEventListener('click', function () {
        knoepfe.forEach(function (x) { x.disabled = true; });
        richtig.classList.add('richtig');
        if (b === richtig) punkte++; else { b.classList.add('falsch'); falsche.push(quizze.indexOf(q) + 1); }
        beantwortet++;
        var e = q.querySelector('.erklaerung');
        if (e) e.hidden = false;
        stand();
      });
    });
  });
  stand();

  // ---- Abruf-Aufgabe ----
  document.querySelectorAll('.abruf').forEach(function (a, i) {
    var feld = a.querySelector('textarea'), knopf = a.querySelector('button'), loesung = a.querySelector('.loesung');
    var key = 'kurs-abruf-' + SEITE + '-' + i;
    if (feld) {
      feld.value = lesen(key) || '';
      feld.addEventListener('input', function () { schreiben(key, feld.value); bericht(); });
    }
    knopf.type = 'button';
    knopf.addEventListener('click', function () { loesung.hidden = false; knopf.hidden = true; });
  });

  // Berichtskasten vor der Fußnavigation einfügen, wenn die Seite Übungen hat.
  if (quizze.length || document.querySelector('.abruf')) {
    var kasten = document.createElement('section');
    kasten.className = 'bericht';
    kasten.innerHTML = '<h2>Ergebnis für den Agenten</h2>' +
      '<p>Die Seite kann mir nichts zurückmelden. Kopier diesen Text und füg ihn im Terminal ein, dann trage ich deinen Stand ein und passe die nächste Lektion an.</p>' +
      '<textarea readonly id="bericht-text" aria-label="Ergebnis zum Kopieren"></textarea>' +
      '<button type="button">Ergebnis kopieren</button>';
    var nav = document.querySelector('.nav');
    nav.parentNode.insertBefore(kasten, nav);
    berichtFeld = kasten.querySelector('textarea');
    var kopf = kasten.querySelector('button');
    kopf.addEventListener('click', function () {
      bericht();
      function fertig() { kopf.textContent = 'Kopiert'; setTimeout(function () { kopf.textContent = 'Ergebnis kopieren'; }, 1800); }
      function ersatz() { berichtFeld.focus(); berichtFeld.select(); try { document.execCommand('copy'); fertig(); } catch (e) {} }
      if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(berichtFeld.value).then(fertig, ersatz); else ersatz();
    });
    bericht();
  }

  // ---- Sprachumschalter ----
  var gruppen = Array.prototype.slice.call(document.querySelectorAll('.sprachen'));
  function zeigen(sprache) {
    gruppen.forEach(function (g) {
      var bloecke = Array.prototype.slice.call(g.querySelectorAll('pre[data-sprache]'));
      var hat = bloecke.some(function (p) { return p.dataset.sprache === sprache; });
      var ziel = hat ? sprache : bloecke[0].dataset.sprache;
      bloecke.forEach(function (p) { p.hidden = p.dataset.sprache !== ziel; });
      g.querySelectorAll('.sprachen-leiste button').forEach(function (b) {
        b.setAttribute('aria-pressed', String(b.textContent === ziel));
      });
    });
  }
  gruppen.forEach(function (g) {
    var leiste = document.createElement('div');
    leiste.className = 'sprachen-leiste';
    g.querySelectorAll('pre[data-sprache]').forEach(function (p) {
      var b = document.createElement('button');
      b.type = 'button';
      b.textContent = p.dataset.sprache;
      b.addEventListener('click', function () { schreiben('kurs-sprache', p.dataset.sprache); zeigen(p.dataset.sprache); });
      leiste.appendChild(b);
    });
    g.insertBefore(leiste, g.firstChild);
  });
  if (gruppen.length) zeigen(lesen('kurs-sprache') || 'TypeScript');
})();
