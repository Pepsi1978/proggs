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

  // ---- Quiz ----
  var quizze = Array.prototype.slice.call(document.querySelectorAll('.quiz'));
  var punkte = 0, beantwortet = 0;
  function stand() {
    var el = document.querySelector('.quiz-stand');
    if (!el || !quizze.length) return;
    el.textContent = beantwortet < quizze.length
      ? beantwortet + ' von ' + quizze.length + ' Fragen beantwortet'
      : punkte + ' von ' + quizze.length + ' beim ersten Versuch richtig';
    if (beantwortet === quizze.length) schreiben('kurs-quiz-' + SEITE, punkte + '/' + quizze.length);
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
        if (b === richtig) punkte++; else b.classList.add('falsch');
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
      feld.addEventListener('input', function () { schreiben(key, feld.value); });
    }
    knopf.type = 'button';
    knopf.addEventListener('click', function () { loesung.hidden = false; knopf.hidden = true; });
  });

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
