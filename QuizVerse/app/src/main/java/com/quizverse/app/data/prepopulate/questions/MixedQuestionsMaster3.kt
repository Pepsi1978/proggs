package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMaster3(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) — Wissenschaftsphilosophie & Epistemologie ──

    // Frage 1 — Falsifikationismus / Popper
    Question(
        categoryId = 11,
        questionText = "Karl Popper lehnte den Induktivismus ab und schlug als Demarkationskriterium zwischen Wissenschaft und Nicht-Wissenschaft die Falsifizierbarkeit vor. Welche logische Asymmetrie begruendet dieses Kriterium?",
        answerA = "Eine endliche Zahl positiver Beobachtungen kann eine Allaussage beweisen, aber eine einzige negative Beobachtung genuegt, sie zu widerlegen",
        answerB = "Eine einzige positive Beobachtung kann eine Allaussage beweisen, aber keine Anzahl negativer Beobachtungen sie widerlegen",
        answerC = "Eine endliche Zahl positiver Beobachtungen kann eine Allaussage nicht beweisen, aber eine einzige negative Beobachtung genuegt, sie zu widerlegen",
        answerD = "Weder positive noch negative Beobachtungen haben logische Beweiskraft — nur die intersubjektive Ueberpruefbarkeit zaehlt",
        correctAnswer = 2,
        explanation = "Popper erkannte die Asymmetrie: Aus logischen Gruenden koennen beliebig viele positive Instanzen ('Alle Schwaene sind weiss') eine Allaussage nicht beweisen (Induktionsproblem nach Hume). Ein einziger schwarzer Schwan genuegt jedoch, sie zu falsifizieren (modus tollens). Daher muss Wissenschaft falsifizierbare Aussagen machen — Theorien die unter keinen Umstaenden widerlegt werden koennen (z.B. Freudsche Psychoanalyse, Astrologie) sind fuer Popper Pseudowissenschaft.",
        difficulty = 5,
        funFact = "Popper entwickelte seinen Falsifikationismus als junger Mann in Wien, nachdem er Einsteins Relativitaetstheorie mit der Psychoanalyse von Freud und Adler verglich. Einstein formulierte konkrete Vorhersagen, die prinzipiell scheitern konnten — und taten es nicht. Freud und Adler hingegen konnten jeden Fall erklaeren, egal was geschah."
    ),

    // Frage 2 — Induktionsproblem
    Question(
        categoryId = 11,
        questionText = "Das Induktionsproblem, das David Hume im 18. Jahrhundert formulierte, stellt die Begruendbarkeit jedes induktiven Schlusses in Frage. Welche Antwort darauf gilt als Kants 'kopernikanische Wende' in der Erkenntnistheorie?",
        answerA = "Hume hatte Unrecht: Induktion ist durch probabilistische Kalkuele vollstaendig gerechtfertigt",
        answerB = "Kausalprinzip und Naturgesetze sind keine Entdeckungen der Erfahrung, sondern Kategorien des Verstandes, die der Verstand selbst in die Natur hineinlegt — Erkenntnis richtet sich nach dem Gegenstand, nicht der Gegenstand nach der Erkenntnis",
        answerC = "Induktive Schluesse sind nur dann gultig, wenn sie durch logische Deduktion ersetztwerden koennen",
        answerD = "Hume hatte Recht — Wissenschaft ist letztlich Gewohnheit und hat keine rationale Grundlage",
        correctAnswer = 1,
        explanation = "Kant antwortete Hume: Das Kausalprinzip ist kein induktiv gewonnenes Naturgesetz, sondern eine reine Verstandeskategorie (Kategorie der Kausalitaet), die der menschliche Geist a priori besitzt und auf alle Erfahrung anwendet. Die 'kopernikanische Wende': Nicht der Gegenstand bestimmt unsere Erkenntnis, sondern unsere Erkenntnisformen (Anschauungsformen Raum/Zeit, Verstandeskategorien) bestimmen, wie Gegenstaende fuer uns erscheinen. Erfahrung ist dadurch moeglich, aber auf den Bereich moeglicher Erfahrung begrenzt.",
        difficulty = 5
    ),

    // Frage 3 — Paradigmenwechsel / Kuhn
    Question(
        categoryId = 11,
        questionText = "Thomas Kuhns Begriff der 'Inkommensurabilitat' zwischen aufeinanderfolgenden Paradigmen besagt, dass konkurrierende Paradigmen nicht rational verglichen werden koennen. Welcher Aspekt seiner Theorie wurde von Vertretern des wissenschaftlichen Realismus am staerksten kritisiert?",
        answerA = "Kuhn behauptet, Wissenschaft mache niemals Fortschritt, was empirisch widerlegt ist",
        answerB = "Die Inkommensurabilitat impliziert, dass Paradigmenwechsel irrational oder zumindest ausserrational (durch Ueberzeugung, nicht Beweis) vollzogen werden — und damit wissenschaftlicher Realismus bezueglich theoretischer Entitaeten undermined wird",
        answerC = "Kuhn ignoriert die Mathematik als paradigmauebergreifende Sprache",
        answerD = "Seine historischen Fallstudien sind empirisch ungenau",
        correctAnswer = 1,
        explanation = "Kuhns Kernthese: Nach einem Paradigmenwechsel (z.B. von newtonischer Mechanik zu Quantenmechanik) bedeuten selbst scheinbar gleiche Begriffe etwas anderes ('Masse' bei Newton vs. Einstein). Daher koennen Paradigmen nicht durch einen gemeinsamen rationalen Massstab verglichen werden. Kritiker wie Putnam und Boyd argumentieren: Wenn der Uebergang von Ptolemaios zu Kopernikus irrational war, wie erklaert man dann den kumulativen technologischen Erfolg der Wissenschaft? Der wissenschaftliche Realismus sieht darin ein 'no-miracles argument'.",
        difficulty = 5,
        funFact = "Kuhn war urspruenglich Physiker, nicht Philosoph. Beim Lesen von Aristoteles fuer ein Seminar bemerkte er, dass Aristoteles' Physik nicht einfach 'schlechte Newtonsche Physik' war, sondern ein voellig anderes Denkgebaude. Diese Erfahrung inspirierte die Idee der Inkommensurabilitat."
    ),

    // Frage 4 — Lakatos
    Question(
        categoryId = 11,
        questionText = "Imre Lakatos entwickelte das Konzept der 'wissenschaftlichen Forschungsprogramme' als Antwort auf Popper und Kuhn. Was unterscheidet laut Lakatos ein 'progressives' von einem 'degenerativen' Forschungsprogramm?",
        answerA = "Ein progressives Programm wird von mehr Wissenschaftlern unterstuetzt; ein degeneratives nur von einer Minderheit",
        answerB = "Ein progressives Programm macht neuartige, empirisch bestaetigte Vorhersagen und erhoht seinen empirischen Gehalt; ein degeneratives erklaert nur nachtraeglich bereits bekannte Faelle durch ad-hoc-Modifikationen",
        answerC = "Ein progressives Programm widerspricht dem herrschenden Paradigma; ein degeneratives bestaetigt es",
        answerD = "Ein progressives Programm ist mathematisch formalisierbar; ein degeneratives nicht",
        correctAnswer = 1,
        explanation = "Lakatos' Methodologie wissenschaftlicher Forschungsprogramme (MSRP): Jedes Programm hat einen unantastbaren 'harten Kern' (z.B. Newtons Gravitationsgesetz) und einen 'Schutzguertel' aus Hilfshypothesen, der angepasst wird wenn Anomalien auftreten. Progressiv: Der Schutzguertel wird so modifiziert, dass neue Vorhersagen entstehen, die sich bewaehren. Degenerativ: Ad-hoc-Anpassungen erklaeren nur bereits bekannte Fakten. Lakatos wollte so rationale Wissenschaftsgeschichte schreiben, ohne Poppers naive Falsifikation zu akzeptieren.",
        difficulty = 5
    ),

    // Frage 5 — Feyerabend
    Question(
        categoryId = 11,
        questionText = "Paul Feyerabend argumentierte in 'Against Method' (1975), dass es keine universelle wissenschaftliche Methode gibt. Mit welchem Schlagwort fasste er seine Position zusammen und welches historische Beispiel nutzte er als Hauptbeleg?",
        answerA = "'Everything goes' — das Beispiel der newtonschen Gravitation, die altere Theorien verdraengte",
        answerB = "'Anything goes' — die Kopernikanische Revolution, bei der Galilei gegen die Regeln der damaligen Wissenschaft verstiess und trotzdem (oder gerade deshalb) Recht hatte",
        answerC = "'Nothing is sacred' — Darwins Evolutionstheorie, die theologische Dogmen brach",
        answerD = "'Science is an ideology' — Einsteins Relativitaetstheorie als Produkt politischer Verhaeltnisse",
        correctAnswer = 1,
        explanation = "Feyerabends 'Anything goes' (auf Deutsch: 'Alles erlaubt') ist kein Nihilismus, sondern eine historische These: Grosse wissenschaftliche Durchbrueche entstanden durch Verstoss gegen geltende Methodenregeln. Galilei nutzte Gegenindukion (Theorien, die bekannten Fakten widersprachen), ignorierte die damals ueberlegene Optik der Kirchenteleskope und prasentierte Daten selektiv. Feyerabend wollte zeigen: Die retrospektive Rationalisierung der Wissenschaftsgeschichte durch Methodologen verdreht, wie Wissenschaft wirklich funktioniert.",
        difficulty = 5,
        funFact = "Feyerabend und Lakatos waren enge Freunde und planten, gemeinsam ein Buch mit dem Titel 'For and Against Method' zu schreiben — Lakatos dafuer, Feyerabend dagegen. Lakatos starb 1974, bevor das Buch fertig war. Feyerabend veroeffentlichte seinen Teil alleine als 'Against Method'."
    ),

    // Frage 6 — Duhem-Quine-These
    Question(
        categoryId = 11,
        questionText = "Die Duhem-Quine-These behauptet, dass keine einzelne Hypothese isoliert getestet werden kann. Welche Konsequenz ergibt sich daraus fuer den Popperschen Falsifikationismus?",
        answerA = "Jede Hypothese kann durch Falsifikation eindeutig widerlegt werden, wenn der Test sorgfaeltig genug durchgefuehrt wird",
        answerB = "Wenn ein Experiment eine Vorhersage widerlegt, ist nicht klar, welche der vielen beteiligten Hilfshypothesen falsch ist — die zentrale Hypothese kann immer durch Anpassung einer Hilfshypothese gerettet werden",
        answerC = "Wissenschaftliche Theorien sind nur dann testbar, wenn sie vollstaendig formalisiert sind",
        answerD = "Experimente koennen Theorien nur in Verbindung mit anderen Theorien best--tigen, nicht widerlegen",
        correctAnswer = 1,
        explanation = "Duhem zeigte (1906): Wenn Foucaults Pendelversuch die ptolemaeische Astronomie widerlegen soll, wird die Theorie zusammen mit Hilfshypothesen ueber Luftreibung, Pendelbauweise, Erdrotation etc. getestet. Schlaegt das Experiment fehl, weiss man nicht, welche Annahme falsch ist. Quine radikalisierte dies: Unser ganzes Wissensgebaude ('web of belief') wird an der Erfahrung gemessen — und wir koennen immer jede beliebige Aussage retten, indem wir anderswo anpassen. Das untergrabt Poppers Idee eindeutiger Falsifikation.",
        difficulty = 5
    ),

    // Frage 7 — Demarkationsproblem
    Question(
        categoryId = 11,
        questionText = "Das Demarkationsproblem der Wissenschaftsphilosophie fragt: Welche Kriterien unterscheiden Wissenschaft von Pseudowissenschaft? Welche Antwort gibt der logische Empirismus des Wiener Kreises?",
        answerA = "Ein Satz ist wissenschaftlich, wenn er falsifizierbar ist (Popper-Kriterium)",
        answerB = "Ein Satz ist wissenschaftlich (sinnvoll), wenn er durch Beobachtungsaussagen verifizierbar oder zumindest bestaetigt werden kann — metaphysische Saetze sind sinnlos (Verifikationsprinzip)",
        answerC = "Wissenschaftliche Saetze zeichnen sich durch ihren Konsens in der Wissenschaftsgemeinschaft aus",
        answerD = "Wissenschaft ist definiert durch den Einsatz von Mathematik und formalisierten Methoden",
        correctAnswer = 1,
        explanation = "Der Wiener Kreis (Carnap, Schlick, Neurath) vertrat das Verifikationsprinzip: Nur Saetze, die durch Beobachtung verifiziert (oder zumindest in ihrer Wahrscheinlichkeit erhoehen werden koennen), sind kognitiv sinnvoll. Metaphysische Aussagen wie 'Das Absolute ist zeitlos' sind demnach nicht falsch, sondern sinnlos — sie sagen nichts ueber die Welt. Popper lehnte das Verifikationsprinzip ab: Es schliesse auch anerkannte Naturgesetze (Allaussagen) als unwissenschaftlich aus.",
        difficulty = 5
    ),

    // Frage 8 — Synthetisch a priori
    Question(
        categoryId = 11,
        questionText = "Kant unterschied vier Urteilsklassen: analytisch a priori, synthetisch a priori, analytisch a posteriori, synthetisch a posteriori. Welches der folgenden Urteile ist nach Kant ein synthetisches Urteil a priori und warum ist diese Klasse philosophisch zentral?",
        answerA = "Alle Koerper sind ausgedehnt — analytisch, weil Ausdehnung im Begriff Koerper enthalten ist",
        answerB = "Die gerade Linie zwischen zwei Punkten ist die kuerzeste — synthetisch a priori, weil 'kuerzeste' nicht im Begriff 'gerade' enthalten ist, aber dennoch notwendig und allgemein gilt, ohne auf Erfahrung zu beruhen",
        answerC = "Der Stein waermt sich durch die Sonne — synthetisch a posteriori, weil aus Erfahrung gewonnen",
        answerD = "Alle Junggesellen sind unverheiratet — synthetisch a priori, weil Notwendigkeit ohne Erfahrung begruendbar",
        correctAnswer = 1,
        explanation = "Kants Schluessel-Entdeckung: Mathematische und geometrische Urteile erweitern unser Wissen ('synthetisch'), sind aber trotzdem notwendig und allgemeingueltig ('a priori'). Das ist nur moeglich, weil Raum und Zeit reine Anschauungsformen unseres Geistes sind, nicht Eigenschaften der Dinge an sich. Naturgesetze wie das Kausalprinzip sind ebenfalls synthetisch a priori. Diese Klasse erklaert, wie Naturwissenschaft moeglich ist: objektiv und notwendig, nicht bloss aus Erfahrung abstrahiert.",
        difficulty = 5,
        funFact = "Die nicht-euklidische Geometrie (Gauss, Lobatschewski, Bolyai) erschuetterte Kants Behauptung, geometrische Urteile seien notwendig wahr. Wenn es mehrere konsistente Geometrien gibt, kann Euklidische Geometrie nicht das einzig moegliche Anschauungsschema sein."
    ),

    // Frage 9 — Wissenschaftlicher Realismus
    Question(
        categoryId = 11,
        questionText = "Das 'No-Miracles Argument' (Hilary Putnam) ist das staerkste Argument fuer den wissenschaftlichen Realismus. Was besagt es?",
        answerA = "Wissenschaftliche Theorien erfordern keine Wunder und sind daher von Natur aus wahr",
        answerB = "Es waere ein Wunder, wenn unsere besten wissenschaftlichen Theorien so erfolgreich waeren (Vorhersagen, Technologie), ohne dass die von ihnen postulierten theoretischen Entitaeten (Elektronen, Gene) wirklich existieren",
        answerC = "Wissenschaft erklaert alle scheinbaren Wunder als Naturphaenomene",
        answerD = "Nur beobachtbare Entitaeten duerfen in wissenschaftliche Theorien eingehen — alles andere waere wie ein Wunderglaube",
        correctAnswer = 1,
        explanation = "Putnams Argument (1975): Die progressive Erfolgsgeschichte der Wissenschaft (Vorhersagen auf die Stelle genau, Technologien die funktionieren) laesst sich nur erklaeren, wenn die Theorien zumindest annaehernd wahr sind und auf reale Entitaeten Bezug nehmen. Die Alternative — rein instrumentalistisch alle Theorieerfolge als Zufall zu betrachten — macht den Erfolg der Wissenschaft zu einem unerklaerbaren Wunder. Anti-Realisten wie van Fraassen entgegnen mit dem 'argument from underdetermination': Empirisch aequivalente Theorien koennen nicht durch Erfolg allein unterschieden werden.",
        difficulty = 5
    ),

    // Frage 10 — Anti-Realismus / van Fraassen
    Question(
        categoryId = 11,
        questionText = "Bas van Fraassen vertritt in 'The Scientific Image' (1980) den 'konstruktiven Empirismus'. Was ist der Unterschied zwischen seinem Anti-Realismus und dem Instrumentalismus?",
        answerA = "Instrumentalismus und konstruktiver Empirismus sind identische Positionen",
        answerB = "Van Fraassen bestreitet nicht, dass Theorien buchstaeblich wahr oder falsch sein koennen — er bestreitet nur, dass wir das Ziel haben sollten, Theorien zu glauben; wissenschaftliches Ziel ist 'empirische Adaequatheit' (Uebereinstimmung mit Beobachtbarem), nicht Wahrheit",
        answerC = "Instrumentalismus akzeptiert theoretische Entitaeten als real; van Fraassen lehnt sie ab",
        answerD = "Van Fraassen glaubt, dass theoretische Saetze sinnlos sind; Instrumentalisten sagen nur, sie seien nuetzliche Werkzeuge",
        correctAnswer = 1,
        explanation = "Instrumentalisten (wie Duhem, frueher Mach) sagen oft, theoretische Aussagen ueber Elektronen etc. seien gar keine echten Behauptungen, sondern nur Kalkulationsregeln (also buchstaeblich weder wahr noch falsch). Van Fraassen hingegen sagt: Theorien haben Wahrheitswerte — ich glaube nur nicht, dass Elektronen existieren; ich glaube, die Theorie beschreibt das Beobachtbare korrekt. Diese Unterscheidung ist subtil aber wichtig: Van Fraassen bleibt Empirist im engeren Sinne, kein Eliminativist.",
        difficulty = 5
    ),

    // Frage 11 — Unterbestimmtheit
    Question(
        categoryId = 11,
        questionText = "Die Unterbestimmtheitsthese (underdetermination) Quinesscher Pragung besagt, dass zu jedem Saetze immer empirisch aequivalente aber inhaltlich verschiedene Theorien existieren. Welche Konsequenz zieht der wissenschaftliche Realist aus der schwachen Version dieser These?",
        answerA = "Da mehrere Theorien gleich gut empirisch passen, muessen alle gleichermassen falsch sein",
        answerB = "Empirische Aequivalenz zweier Theorien macht es rational, zwischen ihnen agnostisch zu bleiben — aber theoretische Tugenden (Einfachheit, Erklaerungskraft) koennen trotzdem eine rationale Praeferenz begruenden",
        answerC = "Die Unterbestimmtheit beweist, dass es keine Wahrheit in der Wissenschaft gibt",
        answerD = "Theorien die empirisch aequivalent sind, sagen tatsaechlich dasselbe aus — es handelt sich nur um unterschiedliche Formulierungen",
        correctAnswer = 1,
        explanation = "Die Unterbestimmtheit ist ein zentrales Problem: Ptolemaeusche und Kopernikanische Astronomie waren lange empirisch aequivalent. Realisten antworten mit 'Inference to the Best Explanation' (IBE): Theoretische Tugenden wie Einfachheit (Ockhams Rasiermesser), Kohaerenz, Erklaerungstiefe und Fruchtbarkeit rechtfertigen eine rationale Praeferenz, auch wenn keine rein empirische Entscheidung moeglich ist. Quines radikale Version ('ontological relativity') fuehrt dagegen zu Holismus und Pragmatismus.",
        difficulty = 5
    ),

    // Frage 12 — Holismus
    Question(
        categoryId = 11,
        questionText = "W.V.O. Quines 'Two Dogmas of Empiricism' (1951) kritisierte zwei Grundannahmen des Logischen Empirismus. Welche zwei Dogmen meinte er?",
        answerA = "Die Annahme, Wissenschaft sei kumulativ, und die Annahme, Beobachtung sei theoriefrei",
        answerB = "Die analytisch-synthetisch-Unterscheidung und den Reduktionismus (jede sinnvolle Aussage sei in Beobachtungsaussagen uebersetzbar)",
        answerC = "Die Verifikationstheorie der Bedeutung und die Correspondenz-Wahrheitstheorie",
        answerD = "Die Annahme der Wertfreiheit der Wissenschaft und die Trennbarkeit von Kontext der Entdeckung und Kontext der Rechtfertigung",
        correctAnswer = 1,
        explanation = "Quines Schluesselaufsatz: Erstes Dogma — die Unterscheidung zwischen analytischen Wahrheiten (qua Bedeutung wahr, z.B. 'Junggesellen sind unverheiratet') und synthetischen Wahrheiten (durch Tatsachen wahr) ist nicht klar aufrechtzuerhalten; auch vermeintlich analytische Wahrheiten koennten im Extremfall revidiert werden. Zweites Dogma — Reduktionismus, die Annahme, jeder Satz koenne unabhaengig auf Beobachtungsaussagen reduziert werden. Stattdessen behauptet Quine: Nur das Gesamtsystem unserer Ueberzeugungen trifft auf Erfahrung.",
        difficulty = 5,
        funFact = "Quines Ablehnung der analytisch-synthetisch-Unterscheidung gilt als eines der einflussreichsten philosophischen Argumente des 20. Jahrhunderts — obwohl es bis heute heftig umstritten ist. Kripke und Putnam versuchten spaeter, notwendige a-posteriori-Wahrheiten zu retten."
    ),

    // Frage 13 — Wissenschaftssoziologie / Merton
    Question(
        categoryId = 11,
        questionText = "Robert K. Merton formulierte 1942 das normative Ethos der Wissenschaft mit dem Akronym CUDOS. Was steht hinter diesen vier Buchstaben?",
        answerA = "Creativity, Universality, Discipline, Openness, Systematicity",
        answerB = "Communalism, Universalism, Disinterestedness, Organized Skepticism",
        answerC = "Cooperation, Understanding, Deduction, Observation, Synthesis",
        answerD = "Consensus, Unity, Determinism, Objectivity, Simplicity",
        correctAnswer = 1,
        explanation = "Mertons CUDOS-Normen (auch CUDOS-System): Communalism — wissenschaftliches Wissen ist Gemeingut, nicht Privateigentum. Universalism — Wahrheit von Behauptungen haengt nicht vom Ansehen des Forschers ab. Disinterestedness — Wissenschaftler sollen im Interesse der Wissenschaft handeln, nicht aus Eigennutz. Organized Skepticism — systematische und offentliche Pruefung aller Behauptungen. Merton entwickelte diese Normen im Kontext des Aufstiegs des Nationalsozialismus, der 'arische Physik' und 'juedische Wissenschaft' unterschied.",
        difficulty = 5
    ),

    // Frage 14 — Normalwissenschaft
    Question(
        categoryId = 11,
        questionText = "Was versteht Kuhn unter 'Normalwissenschaft' und welche Rolle spielen Anomalien in diesem Konzept?",
        answerA = "Normalwissenschaft ist die durchschnittliche wissenschaftliche Leistung; Anomalien treiben sie zum Fortschritt an",
        answerB = "Normalwissenschaft ist Raetsellose innerhalb eines akzeptierten Paradigmas; Anomalien werden zunaechst ignoriert oder als Messfehler behandelt — erst wenn sie sich haeufen und das Paradigma unter Druck geraten lassen, koennen sie eine Krise ausloesen",
        answerC = "Normalwissenschaft bedeutet, dass Wissenschaftler sich an gesellschaftliche Normen halten; Anomalien sind gesellschaftliche Abweichungen",
        answerD = "Kuhn sieht Anomalien als sofortige Falsifikationsbelege fuer das bestehende Paradigma",
        correctAnswer = 1,
        explanation = "Kuhns Normalwissenschaft ist keine abwertende Bezeichnung: In der Phase der Normalwissenschaft loesen Wissenschaftler 'Raetsel' (puzzles) innerhalb eines Paradigmas — sie zweifeln nicht am Paradigma selbst, sondern verfeinern es. Anomalien (Beobachtungen, die sich nicht erklaeren lassen) werden zunaechst als individuelle Fehler abgetan. Erst wenn sie sich haeuften und ernst genommen werden, entsteht eine Krise, die eine wissenschaftliche Revolution ermoeglichen kann. Klassisches Beispiel: Die anomale Merkurperihelverschiebung, die erst Einstein erklaerte.",
        difficulty = 5
    ),

    // Frage 15 — Bayesianismus
    Question(
        categoryId = 11,
        questionText = "Der Bayesianismus in der Erkenntnistheorie behandelt Wissen als graduierten Glaubenszustand. Was ist 'Conditionalization' (Konditionalisierung) und warum ist sie fuer Bayesianer das Herzstuck rationaler Ueberzeugungsrevision?",
        answerA = "Konditionalisierung bedeutet, eine Hypothese zu akzeptieren, wenn ihre Wahrscheinlichkeit ueber 0,5 steigt",
        answerB = "Konditionalisierung ist die Regel, dass ein rationaler Agent seine a-priori-Wahrscheinlichkeit P(H) durch P(H|E) ersetzen soll, wenn er neue Evidenz E erhaelt — gemaess dem Satz von Bayes",
        answerC = "Konditionalisierung ist das Prozedere, alle Hypothesen gleich wahrscheinlich zu machen, bevor neue Evidenz eintrifft",
        answerD = "Konditionalisierung bedeutet, Hypothesen abzulehnen, wenn sie mit Vorbedingungen des Experiments kollidieren",
        correctAnswer = 1,
        explanation = "Bayesianer: P(H|E) = P(E|H) * P(H) / P(E). Die a-priori-Wahrscheinlichkeit P(H) einer Hypothese wird durch neue Evidenz E zur a-posteriori-Wahrscheinlichkeit P(H|E) aktualisiert. 'Jeffrey Conditionalization' ist eine Verallgemeinerung fuer unsichere Evidenz. Zentrale Debatte: Woher kommen die a-priori-Wahrscheinlichkeiten (Priors)? Subjektivisten sagen: aus rationalen Praferenzen, die sich bei hinreichender Evidenz angleichen (convergence theorem). Objektivisten wie Carnap suchten nach logisch eindeutigen Priors.",
        difficulty = 5,
        funFact = "Thomas Bayes (1701–1761) veroeffentlichte seinen Theorem nie selbst. Der Aufsatz 'An Essay towards solving a Problem in the Doctrine of Chances' wurde posthum 1763 von seinem Freund Richard Price eingereicht. Bayes selbst hielt seine Arbeit offenbar fuer unfertig."
    ),

    // Frage 16 — TheorieLadung der Beobachtung
    Question(
        categoryId = 11,
        questionText = "Die These der 'Theoriegeladenheit der Beobachtung' (theory-ladenness) besagt, dass es keine voraussetzungsfreien Beobachtungsaussagen gibt. Welche Konsequenz zieht Norwood Russell Hanson in 'Patterns of Discovery' (1958) daraus?",
        answerA = "Wissenschaftler mit verschiedenen Theorien sehen buchstaeblich verschiedene Dinge, wenn sie dasselbe Objekt betrachten — zwei Astronomen (Tycho Brahe und Kopernikus) sehen beim Sonnenaufgang tatsaechlich Verschiedenes",
        answerB = "Alle Beobachtungen sind subjektiv und daher fuer die Wissenschaft unbrauchbar",
        answerC = "Beobachtungsaussagen koennen durch Protokollierung theoriefrei gemacht werden",
        answerD = "Theoriegeladenheit bedeutet nur, dass Beobachtungen durch theoretisches Wissen verbessert werden",
        correctAnswer = 0,
        explanation = "Hansons beruehmtes Beispiel: Brahe und Kopernikus beobachten die aufgehende Sonne. Brahe 'sieht' die Sonne, die sich um die Erde bewegt; Kopernikus 'sieht' den Horizont, der sich von der Sonne wegdreht. Das Netzhautbild ist gleich, aber was 'gesehen' wird, ist theoriebeeinflusst. Hanson argumentiert, dass 'sehen' nicht ein passives Empfangen von Sinneseindrucken ist, sondern ein aktives Interpretieren mit begrifflichem Hintergrundwissen. Dies untergrabt die empiristische Idee einer sicheren, theorieefreien Beobachtungsbasis.",
        difficulty = 5
    ),

    // Frage 17 — Logischer Empirismus
    Question(
        categoryId = 11,
        questionText = "Der Wiener Kreis versuchte die Einheitswissenschaft (Unity of Science) zu begruenden. Was war das Kernprogramm dieser Bewegung und an welchem Problem scheiterte es?",
        answerA = "Das Programm wollte alle Wissenschaften in Physik reduzieren; es scheiterte weil Biologie zu komplex ist",
        answerB = "Das Programm wollte alle sinnvollen Aussagen auf Protokollsaetze (einfachste Beobachtungsaussagen) zurueckfuehren und durch logische Analyse verbinden; es scheiterte am Problem, Protokollsaetze theoriefrei zu formulieren und am Verifikationsprinzip selbst (welcher Beobachtung entspricht 'Es gibt Elektronen'?)",
        answerC = "Das Programm wollte Religion aus dem oeffentlichen Diskurs eliminieren; es scheiterte politisch",
        answerD = "Das Programm wollte Mathematik auf Logik reduzieren; es scheiterte durch Goedels Unvollstaendigkeitssaetze",
        correctAnswer = 1,
        explanation = "Das Einheitswissenschafts-Programm (Carnap, Neurath, Schlick): Alle wissenschaftlichen Aussagen sollen in eine gemeinsame 'wissenschaftliche Sprache' uebersetzt werden, deren elementare Einheiten Beobachtungsaussagen (Protokollsaetze) sind. Problem: (1) Die intersubjektive Basis der Protokollsaetze ist unklar (Neuraths 'Schiffsreparatur auf hoher See'). (2) Theoretische Terme wie 'Elektron' sind nicht direkt auf Beobachtungen reduzierbar. (3) Das Verifikationsprinzip selbst ist nicht empirisch verifizierbar. Carnap konzedierte spaeter: Theoretische Terme koennen nur 'partiell definiert' werden.",
        difficulty = 5
    ),

    // Frage 18 — Instrumentalismus vs. Realismus
    Question(
        categoryId = 11,
        questionText = "Pierre Duhem war ein fruehes Beispiel fuer instrumentalistische Wissenschaftsphilosophie. Welche Haltung nahm er zu theoretischen Entitaeten wie dem Atom ein, die bis 1909 noch nicht direkt beobachtet worden waren?",
        answerA = "Er glaubte fest an die Realitaet des Atoms und nutzte es als Grundlage seiner Thermodynamik",
        answerB = "Atome sind fuer Duhem nutzliche Rechenannahmen ohne ontologische Verpflichtung — der Zweck einer physikalischen Theorie ist nicht, die Wirklichkeit zu beschreiben, sondern ein System zu liefern, das Gesetze zusammenfasst und Experimente vorhersagt",
        answerC = "Duhem war unentschieden und wartete auf experimentelle Beweise",
        answerD = "Er lehnte Atomtheorien als unvereinbar mit der Thermodynamik voellig ab",
        correctAnswer = 1,
        explanation = "Duhem (1861–1916): Seine Position war ausgepraegt instrumentalistisch. In 'La theorie physique, son objet et sa structure' (1906) argumentierte er: Eine physikalische Theorie ist kein metaphysisches Erklaerungssystem, sondern ein System von mathematischen Saetzen, das experimentelle Gesetze klassifiziert. Ob Atome 'wirklich' existieren, ist eine metaphysische, keine physikalische Frage. Diese Position wurde erschuettert, als Jean Baptiste Perrins Brownsche-Bewegungs-Experimente (1909) die Avogadro-Zahl direkt bestimmten — was viele als starken Realitaetsbeweis fuer Atome werteten.",
        difficulty = 5
    ),

    // Frage 19 — Erklaerung in der Wissenschaft
    Question(
        categoryId = 11,
        questionText = "Das deduktiv-nomologische Modell (DN-Modell) wissenschaftlicher Erklaerung von Carl Hempel beschreibt Erklaerung als logische Ableitung. Was ist das 'Problem der Symmetrie' dieses Modells?",
        answerA = "Das Modell kann nicht erklaeren, warum mathematische Beweise erklaerenswert sind",
        answerB = "Das Modell erlaubt symmetrische Erklaerungen: Die Hoehe eines Flaggenmasten erklaert die Laenge des Schattens, aber nach dem Modell erklaert umgekehrt auch die Schattenlange die Masthoehe — was intuitiv falsch ist",
        answerC = "Das Modell funktioniert nur in der Physik, nicht in der Biologie",
        answerD = "Das Modell setzt voraus, dass alle Erklaerungen kausal sind",
        correctAnswer = 1,
        explanation = "Hempels DN-Modell: Eine Erklaerung ist ein gueltiges Argument, in dem die Konklusion (das zu Erklaerende, Explanandum) aus Gesetzen und Anfangsbedingungen (Explanans) abgeleitet wird. Asymmetrie-Problem (Bromberger): Das Gesetz ueber Winkelfunktionen und die Schattenlange sind logisch aequivalente Argumente — man kann sowohl die Hoehe aus dem Schatten als auch den Schatten aus der Hoehe ableiten. Aber intuitiv ist nur einer der beiden Schluessel erklaerend (die Masthoeheerklaert den Schatten, nicht umgekehrt). Das DN-Modell kann diese Asymmetrie nicht erfassen.",
        difficulty = 5,
        funFact = "Hempel selbst war sich der Grenzen seines Modells bewusst und arbeitete jahrzehntelang an Verfeinerungen. Sein Schueler Salmon entwickelte daraus ein kausales Erklaerungsmodell."
    ),

    // Frage 20 — Kausale Erklaerung
    Question(
        categoryId = 11,
        questionText = "Wesley Salmon kritisierte das DN-Modell und schlug ein kausales Erklaerungsmodell vor. Was ist der Kern seines 'Statistical Relevance'-Modells?",
        answerA = "Erklaerung ist nur durch deterministische Gesetze moeglich",
        answerB = "Eine Erklaerung muss eine statistisch relevante Partitionierung des Ereignisses liefern — ein Faktor F erklaert E genau dann, wenn P(E|F) ungleich P(E) ist (F macht E wahrscheinlicher oder unwahrscheinlicher als ohne F)",
        answerC = "Erklaerungen muessen immer auf Quantenmechanik reduzierbar sein",
        answerD = "Statistische Erklaerungen sind gar keine echten Erklaerungen",
        correctAnswer = 1,
        explanation = "Salmons SR-Modell: Erklaerung besteht darin, die statistisch relevanten Faktoren einer Klasse von Ereignissen zu identifizieren. Wenn rauchen die Wahrscheinlichkeit von Lungenkrebs erhoeht (P(Krebs|Rauchen) > P(Krebs)), dann erklaert Rauchen das Auftreten von Krebs. Problematisch: Das Modell erlaubt 'Erklaerungen' durch bloss statistisch korrelierte Faktoren ohne Kausalitaet (z.B. Storche und Geburtenraten). Salmon erkannte dies und entwickelte spaeter sein 'causal mechanism'-Modell.",
        difficulty = 5
    ),

    // Frage 21 — Wissenschaftliche Revolutionen
    Question(
        categoryId = 11,
        questionText = "Welches war Kuhns paradigmatisches historisches Beispiel fuer eine wissenschaftliche Revolution und was genau aenderte sich dabei?",
        answerA = "Newtons Mechanik ersetzte die Impetustheorie; es aenderte sich die Vorstellung der Kraft als innewohnende Eigenschaft",
        answerB = "Die chemische Revolution (Lavoisier): Phlogistontheorie wurde durch Sauerstoffchemie ersetzt; es aenderte sich nicht nur eine Theorie, sondern die gesamte Begriffsstruktur der Chemie — Verbrennung, Luft, Saeure wurden fundamental neu definiert",
        answerC = "Darwins Evolution ersetzte den Kreationismus — ein klarer kumulativer Fortschritt",
        answerD = "Einsteins Relativitaetstheorie war kein Paradigmenwechsel, sondern eine Erweiterung der Newtonschen Physik",
        correctAnswer = 1,
        explanation = "Kuhn nutzte die chemische Revolution als Musterbeispiel: Vor Lavoisier erklaerete die Phlogistontheorie (Stahl, Priestley) Verbrennung als Freisetzung einer Substanz 'Phlogiston'. Lavoisier (1770er-1780er) zeigte: Verbrennung ist Aufnahme von Sauerstoff. Das war kein bloss additiver Schritt — das ganze begriffliche Netz (was Verbrennung ist, was Luft ist, wie chemische Reaktionen funktionieren) musste neu aufgebaut werden. Kuhns Punkt: Inkommensurabilitat bedeutet genau dies — Phlogiston und Sauerstoff sind nicht dasselbe unter verschiedenen Namen.",
        difficulty = 5
    ),

    // Frage 22 — Wissenschaftlicher Fortschritt
    Question(
        categoryId = 11,
        questionText = "Philip Kitcher unterscheidet in 'The Advancement of Science' (1993) 'wahre Konsensurteile' von blossen 'Konsensurteilen'. Was ist seine Antwort auf Kuhns Herausforderung des wissenschaftlichen Fortschritts?",
        answerA = "Kitcher lehnt den Begriff des Fortschritts ab und folgt Kuhn vollstaendig",
        answerB = "Wissenschaftlicher Fortschritt besteht in der Zunahme von 'significant truths' (bedeutsamen Wahrheiten) — auch nach Paradigmenwechseln werden bestimmte Fragen richtig beantwortet und bestimmte Entitaeten erfolgreich referenziert, was einen kumulativen Kern ergibt",
        answerC = "Fortschritt ist nur innerhalb eines Paradigmas sinnvoll; ueber Paradigmen hinweg ist der Begriff leer",
        answerD = "Fortschritt bedeutet zunehmende Falsifizierbarkeit der Theorien",
        correctAnswer = 1,
        explanation = "Kitchers Antwort auf Kuhn: Auch wenn nach einem Paradigmenwechsel vieles neu konzipiert wird, gibt es einen harten Kern des Fortschritts: Wir wissen jetzt, dass Phlogiston nicht existiert; wir wissen, dass Sauerstoff real ist; wir wissen Dinge, die stimmen. Die historische Referenz wissenschaftlicher Terme (causal theory of reference nach Kripke/Putnam) erlaubt es zu sagen, dass fruhere Wissenschaftler auf dieselben Entitaeten referierten, auch wenn sie falsche Theorien darueber hatten.",
        difficulty = 5
    ),

    // Frage 23 — Reduktionismus
    Question(
        categoryId = 11,
        questionText = "Was ist der Unterschied zwischen ontologischem Reduktionismus und erklaerendem (epistemischem) Reduktionismus in der Wissenschaftsphilosophie?",
        answerA = "Es gibt keinen Unterschied; beide Begriffe sind synonym",
        answerB = "Ontologischer Reduktionismus: Alles Existierende besteht letztlich aus physischen Grundbestandteilen. Epistemischer Reduktionismus: Erklaerungen und Gesetze hoeherstufiger Wissenschaften (z.B. Biologie) koennen prinzipiell aus fundamentaleren Wissenschaften (z.B. Physik) abgeleitet werden — was Nagel als 'theoryreduction' formalisierte",
        answerC = "Ontologischer Reduktionismus gilt nur fuer Physik; epistemischer nur fuer Sozialwissenschaften",
        answerD = "Epistemischer Reduktionismus ist eine staerkere These als ontologischer",
        correctAnswer = 1,
        explanation = "Die Trennung ist entscheidend: Man kann Physikalist (ontologischer Reduktionist) sein und trotzdem den epistemischen Reduktionismus ablehnen. Jerry Fodors 'Sondergesetz-Argument' (1974): Psychologie hat eigene Gesetze ('desire-belief' Erklaerungen), die nicht in neuronale Sprache uebersetzbar sind, weil mentale Typen 'multipel realisiert' werden koennen. Selbst wenn jeder mentale Zustand physisch ist, gibt es kein psychophysisches Bridgegesetz, das den psychologischen Gesetzen entspricht.",
        difficulty = 5,
        funFact = "Ernst Nagel formalisierte 1961 in 'The Structure of Science' das Reduktionsmodell: Theorie T1 reduziert auf T2 wenn T1-Gesetze mit Brueckengesetzen aus T2 ableitbar sind. Nagels Modell gilt heute als zu einfach, inspirierte aber Jahrzehnte der Debatte."
    ),

    // Frage 24 — Wissenschaft und Werte
    Question(
        categoryId = 11,
        questionText = "Die Unterscheidung zwischen 'Kontext der Entdeckung' und 'Kontext der Rechtfertigung' (Reichenbach) soll die Wissenschaftsphilosophie auf Letzteren beschraenken. Was kritisiert die feministische Wissenschaftsphilosophie (z.B. Helen Longino) an dieser Unterscheidung?",
        answerA = "Die Unterscheidung ist logisch fehlerhaft, weil Entdeckung und Rechtfertigung dieselben logischen Strukturen nutzen",
        answerB = "Soziale und politische Werte durchdringen nicht nur die Entdeckung, sondern auch die Rechtfertigung: Die Wahl von Forschungsfragen, die akzeptierten Hintergrundannahmen und sogar die Kriterien fuer gute Erklaerung sind wertebeladen — Wertfreiheit der Rechtfertigung ist eine Illusion",
        answerC = "Feministische Wissenschaftsphilosophie akzeptiert die Unterscheidung, fordert aber Gleichberechtigung im Kontext der Entdeckung",
        answerD = "Die Unterscheidung ist in der Physik sinnvoll, aber nicht in den Sozialwissenschaften",
        correctAnswer = 1,
        explanation = "Longinos 'Science as Social Knowledge' (1990): Der Kontext der Rechtfertigung ist nicht wertfrei. Hintergrundannahmen ('Hilfshypothesen'), die Erklaerungen als gut oder schlecht bewerten, spiegeln kulturelle Werte wider. Beispiel: In der Primatologie wurden lange Maennchen als aktiv und Weibchen als passiv beschrieben — nicht weil Beobachtungen das eindeutig zeigten, sondern weil soziobiologische Hintergrundannahmen das nahelegten. Longino schlaegt 'social objectivity' vor: Objektivitaet entsteht durch kritische Interaktion diverser Standpunkte.",
        difficulty = 5
    ),

    // Frage 25 — Wissenschaftliche Methode
    Question(
        categoryId = 11,
        questionText = "Was ist das 'Problem der alten Evidenz' (Old Evidence Problem) fuer den Bayesianismus?",
        answerA = "Der Bayesianismus kann keine historischen Daten verarbeiten, weil Priors nur fuer zukuenftige Ereignisse gelten",
        answerB = "Wenn eine Evidenz E bereits mit Sicherheit bekannt ist (P(E) = 1), erhoeht E gemaess Bayes die Wahrscheinlichkeit jeder Hypothese nicht mehr — aber historisch bewahrte Erklaerungen bereits bekannter Phaenomene (z.B. Merkurperihelanomalie durch Einsteins Relativitaetstheorie) gelten als Bestaetigung",
        answerC = "Der Bayesianismus kann nur Evidenzen bewerten, die quantitativ messbar sind",
        answerD = "Das Problem ist, dass verschiedene Wissenschaftler unterschiedliche Priors haben und keine gemeinsame Basis finden",
        correctAnswer = 1,
        explanation = "Das Old-Evidence-Problem (Glymour 1980): Einsteins allgemeine Relativitaetstheorie erklaerte die Perihelverschiebung des Merkur (ein Problem, das seit Leverrier 1859 bekannt war). Dies gilt als wichtige Bestaetigung der ART. Aber Bayesianer haben ein Problem: P(E) = 1 (wir wissen bereits mit Sicherheit von der Perihelverschiebung), also aendert E die Wahrscheinlichkeit der Theorie nicht. Loesungsvorschlaege: 'Counterfactual conditionalisation' (stelle dir vor, du wusstest E nicht), oder 'likelihoodism' als Alternative zum klassischen Bayesianismus.",
        difficulty = 5
    ),

    // Frage 26 — Wissenschaft und Wahrheit
    Question(
        categoryId = 11,
        questionText = "Hilary Putnams 'convergent realism' behauptete, dass aufeinanderfolgende Theorien zunehmend der Wahrheit annähern. Larry Laudan widerlegte diese These mit dem 'Pessimistischen Metainduktionsargument'. Was besagt dieses Argument?",
        answerA = "Spaetere Theorien sind zunaechst schlechter, werden aber mit der Zeit immer besser",
        answerB = "Die Geschichte der Wissenschaft ist voll erfolgreicher Theorien, die wir heute als falsch betrachten (Phlogiston, Aether, Kaloritheorie, Lebenskraft) — also ist per Induktion ueber die Wissenschaftsgeschichte zu erwarten, dass auch unsere besten heutigen Theorien falsch sind",
        answerC = "Wissenschaftliche Theorien koennen grundsaetzlich nicht mit der Wahrheit verglichen werden",
        answerD = "Fortschritt in der Wissenschaft ist eine Illusion, die durch den Siegerperspektive erzeugt wird",
        correctAnswer = 1,
        explanation = "Laudans pessimistische Metainduktion (1981): Nimm die Liste vergangener wissenschaftlicher Theorien, die (a) empirisch erfolgreich waren und (b) heute als falsch gelten: Phlogiston, caloric, Newtonscher Aether, Vitalismus, Planetenepizyklel etc. Per Induktion: Unsere heutigen Theorien (Standardmodell der Teilchenphysik, neuronale Theorien des Geistes) werden mit hoher Wahrscheinlichkeit ebenfalls falsch sein. Realisten antworten: Die falschen Teile wurden eliminiert (Elektronen blieben, Phlogiston nicht) — 'structural realism' (Worrall) rettet die mathematische Struktur.",
        difficulty = 5,
        funFact = "John Worralls struktureller Realismus als Antwort auf die Metainduktion: Fresnel und Maxwell verwendeten verschiedene Aether-Ontologien, aber die mathematischen Gleichungen blieben. Wir sollten nur an die Struktur, nicht an die Entitaeten glauben."
    ),

    // Frage 27 — Abduktion
    Question(
        categoryId = 11,
        questionText = "C.S. Peirce unterschied drei Schlussformen: Deduktion, Induktion und Abduktion. Was ist Abduktion und welche Rolle spielt sie in der wissenschaftlichen Entdeckung?",
        answerA = "Abduktion ist ein deduktiver Schluss mit unsicheren Praemissen",
        answerB = "Abduktion ist der Schluss auf die beste Erklaerung: Aus der ueberraschenden Tatsache C und der Hypothese H (die C erklaeren wuerde) wird geschlossen, H anzunehmen — es ist das kreative Moment wissenschaftlicher Hypothesenbildung",
        answerC = "Abduktion ist eine Form der Induktion, die von Spezialfaellen auf allgemeine Gesetze schliesst",
        answerD = "Abduktion ist der Rueckschluss von Wirkungen auf Ursachen mit logischer Notwendigkeit",
        correctAnswer = 1,
        explanation = "Peirce (1839–1914): Abduktion ist die einzige Schlussform, die neue Hypothesen einfuehrt. Schema: Beobachtung C ist ueberraschend. Wenn H wahr waere, waere C selbstverstaendlich. Also gibt es Grund, H anzunehmen. Beispiel: Der Arzt beobachtet Symptome und schliesst auf eine Diagnose. Abduktion ist nicht logisch zwingend (viele Hypothesen koennten C erklaeren), sondern pragmatisch-kreativ. Harman pragte spaeter den Begriff 'Inference to the Best Explanation' (IBE) als moderne Variante.",
        difficulty = 5
    ),

    // Frage 28 — Wissenschaft und Gesellschaft
    Question(
        categoryId = 11,
        questionText = "Was ist das 'Merton-Paradox' in der Wissenschaftssoziologie, das sich aus dem Widerspruch zwischen dem CUDOS-Ethos und der tatsaechlichen wissenschaftlichen Praxis ergibt?",
        answerA = "Wissenschaftler klagen ueber Plagiarismus, obwohl das CUDOS-Ethos Communalism (Gemeinguetigkeit) vorschreibt — aber sind selbst bereit zu stehlen",
        answerB = "Das CUDOS-Ethos fordert Disinterestedness (Uneigennutzigkeit), aber wissenschaftliche Praxis zeigt massive Prioritaetskonflikte — Wissenschaftler bekaempfen sich um Prioritaet bei Entdeckungen (Originalitaet als Kapital), was mit dem Communalism-Ethos kollidiert: Teilen vs. als Erster sein",
        answerC = "Das Ethos fordert Universalism, aber in der Praxis dominiert nationale Wissenschaftsfoerderung",
        answerD = "Organized Skepticism macht wissenschaftliche Zusammenarbeit unmoeglich",
        correctAnswer = 1,
        explanation = "Merton selbst beobachtete diesen Widerspruch: Das CUDOS-Ethos fordert, Wissen zu teilen (Communalism) und eigennuetzige Motive auszublenden (Disinterestedness). Gleichzeitig ist der wissenschaftliche Statusmarkt auf Prioritaet aufgebaut: Wer zuerst publiziert, hat den Anspruch. Dies erklaert bittere Prioritaetskonflikte (Newton vs. Leibniz bei der Infinitesimalrechnung, Wallace vs. Darwin bei der Evolutionstheorie). Merton nannte dies den 'ambivalence of scientists': Man will teilen und gleichzeitig der Erste sein.",
        difficulty = 5
    ),

    // Frage 29 — Wissenschaftliche Erkenntnis
    Question(
        categoryId = 11,
        questionText = "Edmund Gettier zeigte 1963 in einem dreiseitigen Aufsatz, dass die klassische Definition von Wissen als 'gerechtfertigte wahre Ueberzeugung' (justified true belief / JTB) falsch ist. Was ist ein 'Gettier-Fall'?",
        answerA = "Ein Fall, in dem eine Ueberzeugung wahr ist, aber ohne jede Rechtfertigung gehalten wird",
        answerB = "Ein Fall, in dem jemand eine gerechtfertigte, wahre Ueberzeugung hat, aber diese Wahrheit zufall ist — die Rechtfertigung stuetzt eine Ueberzeugung, die zufaellig wahr ist, aber aus den falschen Gruenden",
        answerC = "Ein Fall, in dem Rechtfertigung und Wahrheit auseinanderfallen, weil die Person Wahrheit nicht erkennen kann",
        answerD = "Ein Paradox, das zeigt, dass Wissen ohne Rechtfertigung auskommt",
        correctAnswer = 1,
        explanation = "Gettiers Beispiel: Smith glaubt gerechtfertigt 'Der Mann, der den Job bekommt, hat 10 Muenzen in der Tasche' (basierend auf Zeugnis ueber Jones). Tatsaechlich bekommt Smith selbst den Job und hat zufaellig auch 10 Muenzen. Die Ueberzeugung ist wahr, gerechtfertigt — aber Smith weiss es nicht wirklich, weil die Rechtfertigung auf einer falschen Praemisse (Jones bekommt den Job) beruht. Gettier-Faelle loesten eine Flut von Versuchen aus, Wissen durch vierte Bedingungen zu definieren (Kausaltheorie, Defeasibility-Theorie, Reliabilismus).",
        difficulty = 5,
        funFact = "Gettiers Aufsatz 'Is Justified True Belief Knowledge?' ist moeglicherweise der einflussreichste dreiseitige Aufsatz in der Geschichte der Philosophie. Er wurde 1963 in Analysis veroeffentlicht und zeigte, dass 2500 Jahre Erkenntnistheorie auf einem fehlerhaften Fundament standen."
    ),

    // Frage 30 — Externalism vs. Internalism
    Question(
        categoryId = 11,
        questionText = "Alvin Goldman entwickelte den 'Reliabilismus' als Antwort auf die Gettier-Probleme. Was ist der Kern dieser Position und wie unterscheidet sie sich vom epistemischen Internalismus?",
        answerA = "Reliabilismus: Wissen erfordert ausschliesslich innere bewusste Rechtfertigung; Internalismus erlaubt externe Faktoren",
        answerB = "Reliabilismus (Externalismus): Eine Ueberzeugung ist gerechtfertigt, wenn sie durch einen zuverlaessigen kognitiven Prozess entstanden ist — auch ohne dass die Person diesen Prozess kennt. Internalismus: Rechtfertigung muss vom Subjekt aus dessen eigener Perspektive zuganglich sein",
        answerC = "Beide Positionen sind identisch und unterscheiden sich nur terminologisch",
        answerD = "Reliabilismus verlangt soziale Verifikation; Internalismus individuelle",
        correctAnswer = 1,
        explanation = "Goldman: Wissen = wahre Ueberzeugung, die durch einen reliablen (zuverlaessigen) Prozess entstand. Beispiel: Wahrnehmen unter normalen Bedingungen ist reliabel; Raterei nicht. Das ist externalistisch: Die Person muss nicht wissen, warum ihr Prozess reliabel ist. Internalistische Gegner (BonJour, Chisholm): Rechtfertigung muss 'deontisch' sein — der Erkennende muss Zugang zu seinen Rechtfertigungsgruenden haben. Das Dilemma: Internalism ist anspruchsvoll (wenig Wissen moeglich), Externalism ist kontraintuitiv (eine Thermometer 'weiss' in diesem Sinne, wie warm es ist).",
        difficulty = 5
    ),

    // Frage 31 — Wissenschaftliche Erklarung (Statistisch)
    Question(
        categoryId = 11,
        questionText = "Hempel und Oppenheim entwickelten neben dem DN-Modell auch das induktiv-statistische (IS) Modell. Was ist das 'Problem der epistemischen Relativitaet' dieses Modells?",
        answerA = "Statistische Erklaerungen koennen nur in der Physik angewendet werden, nicht in der Medizin",
        answerB = "Dieselbe Erklaerung kann je nach Referenzklasse (Wissensbasis des Erklaerenden) stark oder schwach sein — ein Raucher bekommt Lungenkrebs: erklaert durch 'er rauchte' (schwache Wahrscheinlichkeit) oder erklaert durch 'er rauchte und hatte Gendefekt X und war exponiert zu...' (hohe Wahrscheinlichkeit). Die Erklaerungsstaerke haengt von dem ab, was der Erklaerende weiss",
        answerC = "Das IS-Modell laesst keine quantitativen Wahrscheinlichkeiten zu",
        answerD = "Das Problem ist, dass statistische Gesetze keine echten Gesetze sind",
        correctAnswer = 1,
        explanation = "Hempel erkannte das Problem selbst: Im IS-Modell muss die Erklaerung eine hohe induktive Staerke haben (das Ereignis wahrscheinlich machen). Aber je mehr wir wissen, desto mehr koennen wir die Referenzklasse verfeinern (die 'narrowest reference class'). Das macht Erklaerungen relativ zur Wissensbasis — was fuer einen Arzt eine vollstaendige Erklaerung ist, ist fuer einen Genetiker unvollstaendig. Salmon kritisierte: Wir sollten statistische Relevanz, nicht hohe Wahrscheinlichkeit als Erklaerungskriterium verwenden.",
        difficulty = 5
    ),

    // Frage 32 — Sprachphilosophie der Wissenschaft
    Question(
        categoryId = 11,
        questionText = "Hilary Putnam und Saul Kripke entwickelten die kausale Referenztheorie fuer natuerliche-Arten-Terme. Was besagt diese Theorie und welche Konsequenz hat sie fuer den wissenschaftlichen Realismus?",
        answerA = "Natuerliche-Arten-Terme referieren auf das, was Wissenschaftler damit meinen — und das aendert sich mit dem Wissen",
        answerB = "Natuerliche-Arten-Terme wie 'Wasser' oder 'Gold' referieren durch eine kausale Kette zurueck auf eine urspruengliche 'Taufe' — auch wenn die Theorie ueber die Art falsch war. Konsequenz: Galilei referierte auf Gravitation, auch wenn seine Theorie falsch war; wir reden ueber dasselbe, wenn wir 'Elektron' sagen wie Thomson 1897",
        answerC = "Natuerliche-Arten-Terme haben keine Referenz, nur Sinn (im Frege'schen Sinne)",
        answerD = "Die kausale Theorie zeigt, dass wissenschaftliche Terme bedeutungslos sind ohne experimentellen Kontext",
        correctAnswer = 1,
        explanation = "Kripkes 'Naming and Necessity' (1970/1980) und Putnams 'The Meaning of Meaning' (1975): Wenn wir 'Wasser' sagen, meinen wir die Substanz, was immer ihre chemische Struktur ist — und diese Struktur (H2O) wurde empirisch entdeckt, ist aber notwendig wahr (Wasser ist notwendigerweise H2O). Das erlaubt 'aposteriori Notwendigkeiten'. Fuer den Realismus: Wissenschaftler referieren auf reale Entitaeten durch kausale Ketten, auch wenn ihre Theorien darueber falsch sind. Dies rettet die Idee wissenschaftlichen Fortschritts trotz Theoriewandel.",
        difficulty = 5
    ),

    // Frage 33 — Wissenschaft und Erklaerung (Funktional)
    Question(
        categoryId = 11,
        questionText = "Was ist das zentrale Problem der 'funktionalen Erklaerung' in den Sozial- und Biowissenschaften, wie es Ernest Nagel und Carl Hempel herausarbeiteten?",
        answerA = "Funktionale Erklaerungen sind zu komplex fuer das DN-Modell",
        answerB = "Funktionale Erklaerungen ('Das Herz schlaegt, um Blut zu pumpen') erklaeren einen Zustand durch seine zukuenftige Funktion — eine Art Teleologie, die zirkular zu sein droht: Wir erklaeren den Herzschlag durch die Funktion, die er erbringt, aber die Funktion setzt den Herzschlag voraus",
        answerC = "Funktionale Erklaerungen sind nur in der Biologie und nicht in den Sozialwissenschaften anwendbar",
        answerD = "Das Problem ist, dass Funktionen nicht beobachtbar sind",
        correctAnswer = 1,
        explanation = "Hempel (1959): Funktionale Erklaerungen in der Form 'X hat Funktion F in System S' sind erklaerend nur wenn gezeigt werden kann, dass X notwendig fuer F ist (Dispositions-Analyse). Aber oft gibt es aequifunktionale Alternativen: Das Herz pumpt Blut, aber auch eine Herzluft-Maschine tut es. Warum existiert gerade das Herz? Evolutionaere Erklaerungen loesen das Problem durch historische Kausalitaet (Selektionsgeschichte statt Teleologie). In den Sozialwissenschaften bleibt das Problem: Funktionalismus (Durkheim, Parsons) wurde fuer Zirkularitaet und konservative Implikationen kritisiert.",
        difficulty = 5
    ),

    // Frage 34 — Soziale Erkenntnistheorie
    Question(
        categoryId = 11,
        questionText = "Was unterscheidet die 'starke Soziologie des Wissens' (Strong Programme, David Bloor) von einer traditionellen Wissenschaftsphilosophie?",
        answerA = "Die starke Soziologie untersucht nur erfolgreiche Wissenschaft; die schwache auch Fehlschlaege",
        answerB = "Das Strong Programme (Symmetrieprinzip): Sowohl wahre als auch falsche wissenschaftliche Ueberzeugungen muessen symmetrisch durch soziale Faktoren erklaert werden — es ist illegitim, wahre Theorien durch rationale Gruende und nur falsche durch soziale Erklaerungen zu erklaeren",
        answerC = "Die starke Soziologie behauptet, dass Wahrheit sozial konstruiert wird und keine objektive Basis hat",
        answerD = "Das Strong Programme lehnt jede empirische Analyse der Wissenschaftsgeschichte ab",
        correctAnswer = 1,
        explanation = "Bloors 'Knowledge and Social Imagery' (1976): Das Strong Programme hat vier Grundsaetze — Kausalitaet (soziale Ursachen erklaeren Ueberzeugungen), Unparteilichkeit (gegenueber Wahrheit/Falschheit), Symmetrie (beide erklaeren), Reflexivitaet (gilt auch fuer die Soziologie selbst). Das Symmetrieprinzip ist der Kernanspruch: Wir duerfen nicht sagen 'Lavoisiers Chemie war erfolgreich, weil sie wahr ist, aber Stahls Phlogistonchemie war erfolgreich, weil er politisch gut vernetzt war'. Kritiker: Symmetrie droht, den Unterschied zwischen Wahrheit und Irrtum aufzuloesen.",
        difficulty = 5,
        funFact = "Das Strong Programme wurde vom Wissenschaftsphilosophen Larry Laudan scharf kritisiert: 'Irrationality and the sociology of knowledge' (1981) — Bloors Symmetrieprinzip macht den Unterschied zwischen Wahrheit und Irrtum unerklaerbar."
    ),

    // Frage 35 — Naturalismus
    Question(
        categoryId = 11,
        questionText = "W.V.O. Quines 'epistemologischer Naturalismus' fordert, die traditionelle Erkenntnistheorie durch empirische Kognitionswissenschaft zu ersetzen. Was kritisiert Jaegwon Kim an diesem Programm?",
        answerA = "Quines Naturalismus ist zu stark vereinfacht und ignoriert mathematische Erkenntnisse",
        answerB = "Wenn Erkenntnistheorie nur beschreibt, wie Menschen faktisch Ueberzeugungen bilden, verliert sie ihre normative Dimension — Erkenntnistheorie soll sagen, wie man Ueberzeugungen bilden SOLLTE, nicht nur wie es faktisch geschieht. Naturalismus kann das 'Soll' nicht erklaeren",
        answerC = "Naturalisierte Erkenntnistheorie ist zu abhaengig von Laborexperimenten",
        answerD = "Quines Naturalismus fuehrt zwingend zu Relativismus",
        correctAnswer = 1,
        explanation = "Kims Kritik (1988): Die traditionelle Erkenntnistheorie ist normativ — sie beantwortet die Frage 'Wann sind Ueberzeugungen gerechtfertigt?'. Wenn wir Erkenntnistheorie durch Kognitionswissenschaft ersetzen, erhalten wir nur deskriptive Berichte ueber kognitive Prozesse. Aber daraus folgt kein Soll. Das ist ein Sein-Sollen-Problem (naturalistic fallacy). Goldman versuchte, diese Spannung im Reliabilismus aufzuloesen: Zuverlässige Prozesse sind auch normativ bevorzugte.",
        difficulty = 5
    ),

    // Frage 36 — Wissenschaftliche Modelle
    Question(
        categoryId = 11,
        questionText = "Was ist die 'semantic view of theories' (semantische Sicht auf Theorien) und wie unterscheidet sie sich von der 'syntaktischen Sicht' des Logischen Empirismus?",
        answerA = "Die semantische Sicht untersucht die Bedeutung wissenschaftlicher Terme; die syntaktische nur ihre grammatikalische Struktur",
        answerB = "Syntaktische Sicht: Theorien sind axiomatisierte Saetze in formaler Sprache (Carnap). Semantische Sicht (van Fraassen, Suppe): Theorien sind Familien von Modellen — abstrakte Strukturen, die empirische Systeme repraesentieren; eine Theorie ist empirisch adaequat, wenn ein Modell isomorph zur beobachtbaren Realitaet ist",
        answerC = "Die semantische Sicht ist eine neuere Variante des Logischen Positivismus",
        answerD = "Die semantische Sicht bewertet Theorien nach ihrer Nuetzlichkeit; die syntaktische nach ihrer logischen Konsistenz",
        correctAnswer = 1,
        explanation = "Der Wandel von syntaktischer zu semantischer Sicht war ein zentraler Fortschritt der 1970er-1980er Wissenschaftsphilosophie. Die syntaktische Sicht (Carnap, Hempel) hatte Probleme: Axiomatisierung in erster-Ordnungs-Logik ist fuer komplexe Theorien unpraktikabel; 'theoretische Terme' vs. 'Beobachtungsterme' lassen sich nicht klar trennen. Die semantische Sicht: Theorien liefern Modelle. Die Newtonsche Mechanik ist ein Modell-System (Systeme mit Massen, Kraeften). Empirische Adaequatheit = es gibt ein Modell, das isomorph zu beobachtbaren Phaenomenen ist.",
        difficulty = 5
    ),

    // Frage 37 — Wissenschaftsethik
    Question(
        categoryId = 11,
        questionText = "Was ist die 'value-ladenness' These in der feministischen Wissenschaftsphilosophie und wie unterscheidet sie sich von der Forderung nach Wertfreiheit (Max Weber)?",
        answerA = "Wertfreiheit und value-ladenness sind identische Konzepte, nur unterschiedlich benannt",
        answerB = "Weber fordert Wertfreiheit in der wissenschaftlichen Erklaerung (keine Werturteile in Forschungsergebnissen). Value-ladenness-Vertreter (Longino, Anderson) argumentieren: Epistemische Werte (Einfachheit, Erklaerungskraft) sind untrennbar mit nicht-epistemischen (sozialen, politischen) Werten verwoben — vollstaendige Wertfreiheit ist unmoeglich und nicht wuenschenswert",
        answerC = "Weber ist Vorlaeufer der feministischen Wissenschaftsphilosophie",
        answerD = "Value-ladenness bedeutet nur, dass Forschungsfragen durch gesellschaftliche Interessen bestimmt werden",
        correctAnswer = 1,
        explanation = "Weber (1917): In den Sozialwissenschaften soll der Forscher eigene Werturteile aus den Forschungsergebnissen heraushalten ('Werturteilsstreit'). Diese Trennung gilt als wissenschaftliches Ideal. Value-ladenness-Argumente (ab 1970er): Nicht nur die Wahl der Forschungsfragen (was erforscht wird), sondern die Standards fuer gute Erklaerung, die Wahl von Modellen, die Interpretation von Daten — all das ist wertegeladen. Beispiel: Was als 'normale' Gehirnentwicklung gilt, haengt von gesellschaftlichen Normen ab (Neurowissenschaft der Geschlechtsunterschiede).",
        difficulty = 5
    ),

    // Frage 38 — Wissenschaftsgeschichte
    Question(
        categoryId = 11,
        questionText = "Was versteht Alexandre Koyre unter dem Begriff des 'Bruchs' (rupture) in der Wissenschaftsgeschichte und wie beeinflusste dies Kuhns Paradigmentheorie?",
        answerA = "Koyre beschreibt technologische Bruche in der Wissenschaft durch neue Instrumente",
        answerB = "Koyre zeigte (1930er-1940er), dass die wissenschaftliche Revolution des 17. Jahrhunderts kein kumulativer Fortschritt war, sondern ein radikaler konzeptueller Bruch — das Universum des 'approximativen Masses' wurde durch das 'Universum der Praezision' ersetzt; Galilei dachte fundamental anders als Aristoteles, nicht nur besser",
        answerC = "Koyre meint politische Bruche, die Wissenschaft beeinflussen",
        answerD = "Koyre beschreibt Bruche zwischen Theorie und Experiment",
        correctAnswer = 1,
        explanation = "Koyre gilt als einer der wichtigsten Wissenschaftshistoriker. In 'Etudes Galileennes' (1939) zeigte er: Galileis Mechanik war kein Refinement aristotelischer Physik, sondern eine vollstaendige konzeptuelle Revolution. Das aristotelische Universum ist qualitativ, endlich, teleologisch; das galileisch-newtonsche ist mathematisch, unendlich, kausal. Kuhn zitierte Koyre explizit als entscheidende Inspiration fuer die Idee nicht-kumulativer wissenschaftlicher Revolutionen und Paradigmenwechsel.",
        difficulty = 5,
        funFact = "Gaston Bachelard praegte gleichzeitig den Begriff 'epistemologischer Bruch' (coupure epistemologique), der spaeter von Althusser auf Marx angewendet wurde — was zeigt, wie Konzepte der Wissenschaftsphilosophie in politische Theorie ueberschwappen koennen."
    ),

    // Frage 39 — Probabilismus
    Question(
        categoryId = 11,
        questionText = "Rudolf Carnap versuchte eine 'induktive Logik' zu entwickeln, die den Bestaetigungsgrad einer Hypothese durch Evidenz logisch bestimmt. An welchem Grundproblem scheiterte sein Programm?",
        answerA = "Carnaps Logik war zu komplex fuer damalige Computer",
        answerB = "Das Problem der 'Gleichverteilung' (indifference principle): Es gibt keine logisch ausgezeichnete Verteilung der a-priori-Wahrscheinlichkeiten ueber Hypothesen — verschiedene Beschreibungssprachen fuehren zu verschiedenen Bestaetigungsgraden, ohne dass eine als 'die richtige' ausgezeichnet werden kann",
        answerC = "Induktive Logik funktioniert nur fuer deduktiv ableitbare Saetze",
        answerD = "Carnap fand keine adaequate Formalisierung des Hypothesenbegriffs",
        correctAnswer = 1,
        explanation = "Carnap (1945-1962): c-Funktionen sollen den Bestaetigungsgrad c(H,E) einer Hypothese H durch Evidenz E angeben. Problem: Wie waehlt man die a-priori-Wahrscheinlichkeiten? Das klassische Indifferenzprinzip (gleiche Wahrscheinlichkeit aller Moeglichkeiten) haengt von der Wahl der Beschreibungssprache ab. Benutzt man Individuen als Grundelemente, ergibt sich eine andere Verteilung als bei Eigenschaften. Es gibt keinen sprachunabhaengigen 'privilegierten' Prior. Goodman zeigte mit dem 'Grue-Paradox', dass das Bestaetigung-Problem noch tiefer geht.",
        difficulty = 5
    ),

    // Frage 40 — Grue-Paradox
    Question(
        categoryId = 11,
        questionText = "Nelson Goodman formulierte das 'neue Raetsel der Induktion' (new riddle of induction) mit dem Praedikat 'grue'. Was ist 'grue' und was zeigt das Paradox?",
        answerA = "Grue ist eine Farbmischung aus gruen und blau; das Paradox zeigt, dass Farbbeschreibungen relativ sind",
        answerB = "Grue ist das Praedikat: 'vor Zeitpunkt T beobachtet und gruen, oder nach T beobachtet und blau'. Alle bisher beobachteten Smaragde bestaetigen 'Alle Smaragde sind gruen' genauso wie 'Alle Smaragde sind grue' — Induktion bestaetigt beide gleich, obwohl eine vorhersagt, Smaragde werden nach T blau. Das zeigt: Nur 'projizierbare' Praedikate sind induktionsgeeignet, aber was projectible ist, haengt von Gewohnheit (entrenchment) ab",
        answerC = "Grue ist ein logischer Fehler in der Induktionstheorie, der auf Sprachverwirrung beruht",
        answerD = "Das Grue-Paradox zeigt, dass Beobachtung keine Evidenz fuer induktive Schluesse liefert",
        correctAnswer = 1,
        explanation = "Goodmans 'Fact, Fiction, and Forecast' (1955): Das Paradox zeigt, dass nicht alle Regularitaeten gleich projizierbar sind. 'Gruen' ist entrenchment (tief in unserer sprachlichen Praxis verankert), 'grue' nicht. Aber das ist eine historische/soziologische Tatsache, keine logische. Goodman schloss: Induktionslogik kann nicht rein formal sein — sie haengt von unserer sprachlichen Geschichte ab. Das ist ein tiefer Einwand gegen Carnaps Programm und jede rein formale Induktionslogik.",
        difficulty = 5,
        funFact = "Der Philosoph I.J. Good erfand 'bleen' als komplementaeres Praedikat zu grue: 'vor T beobachtet und blau, oder nach T und gruen'. In der Sprache mit grue und bleen sind gruen und blau die 'seltsameren' Praedikate — was zeigt, dass Natiuerlichkeit eines Praedikats kulturrelativ ist."
    ),

    // Frage 41 — Wissenschaft und Pseudowissenschaft
    Question(
        categoryId = 11,
        questionText = "Warum gilt die Homöopathie aus Sicht des wissenschaftstheoretischen Kritikalismus (Popper) und des Bayesianismus als unwissenschaftlich bzw. durch Evidenz nicht gestützt?",
        answerA = "Homoeopathie verwendet Wasser und ist daher chemisch nicht erklärbar",
        answerB = "Aus Popperscher Sicht: Homoeopathie macht keine klar falsifizierbaren Vorhersagen; Misserfolge werden als falsche Potenzierung, falsche Diagnose etc. erklaert (ad-hoc-Schutz). Aus Bayesianischer Sicht: Die a-priori-Wahrscheinlichkeit ist extrem niedrig (widerspricht gesichertem Wissen ueber Verduennung und biologische Wirkung), und positive Studien haben methodische Maengel — P(H|E) bleibt nach Konditionalisierung extrem niedrig",
        answerC = "Homoeopathie ist widerlegt, weil Doppelblindstudien nie durchgefuehrt wurden",
        answerD = "Das Demarkationsproblem ist irrelevant fuer Medizin; nur Wirksamkeitsstudien zaehlen",
        correctAnswer = 1,
        explanation = "Das Zusammenspiel beider Kriterien ist instruktiv: Popper-Kriterium schlaegt an, weil jede negative Studie mit Hilfshypothesen erklaert wird (falsche Potenz, falsche Konstitution des Patienten etc.) — ein echtes Falsifikat wird nie akzeptiert. Bayesianisch: Wenn P(E|H_Homoeopathie) fast gleich P(E|H_Placebo) ist (weil bei starker Verduennung kein Wirkstoff uebrig ist), aendert auch positive Evidenz die Wahrscheinlichkeit kaum. Metaanalysen (Cochrane) zeigen: Wirkung nicht ueber Placebo hinaus.",
        difficulty = 5
    ),

    // Frage 42 — Wissenschaftlicher Realismus (Struktural)
    Question(
        categoryId = 11,
        questionText = "John Worrall schlug 1989 den 'strukturellen Realismus' als Mittelweg zwischen Realismus und Anti-Realismus vor. Was ist die Kernidee und welches historische Beispiel stuetzt sie?",
        answerA = "Struktureller Realismus: Wir sollen nur an die Existenz unbeobachtbarer Entitaeten, nicht an ihre Eigenschaften glauben",
        answerB = "Struktureller Realismus: Wir sollen nicht an die Natur unbeobachtbarer Entitaeten glauben, sondern nur an die mathematischen Strukturrelationen. Beispiel: Fresnels Aetherelastizitaetsgleichungen und Maxwells elektromagnetische Gleichungen haben dieselbe mathematische Struktur — obwohl der Aether verschwand, blieben die Gleichungen. Was wir wirklich kennen, ist die Struktur, nicht das Substrat",
        answerC = "Struktureller Realismus ist eine Variante des Instrumentalismus, der mathematische Formeln als nuetzliche Fiktionen behandelt",
        answerD = "Struktureller Realismus behauptet, dass alle Wissenschaft letztlich auf Gruppentheorie reduzierbar ist",
        correctAnswer = 1,
        explanation = "Worralls Argument: Die Pessimistische Metainduktion zeigt, dass Entitaeten (Aether, Phlogiston, Kaloritheorie-Waerme) kommen und gehen. Aber die mathematischen Gleichungen zeigen Kontinuitaet: Fresnels Sinusgesetz und das Gesetz der Reflexion sind in Maxwells Theorie erhalten. Was durch den Wandel bewahrt wird, sind Strukturen (Gleichungen, abstrakte Relationen), nicht Entitaeten. Ontic structural realism (Ladyman, French) radikalisiert dies: Es gibt gar keine Entitaeten, nur Strukturen.",
        difficulty = 5
    ),

    // Frage 43 — Erkenntnis a priori
    Question(
        categoryId = 11,
        questionText = "Saul Kripke argumentierte in 'Naming and Necessity', dass es Wahrheiten gibt, die notwendig aber a posteriori sind. Was ist ein Beispiel und warum widerspricht das Kants Kategorisierung?",
        answerA = "2 + 2 = 4 ist notwendig a posteriori, weil wir es durch Erfahrung lernen",
        answerB = "'Wasser ist H2O' ist notwendig (koennte in keiner moeglichen Welt falsch sein) und a posteriori (wurde durch chemische Forschung entdeckt, nicht durch bloses Denken). Das widerspricht Kant: Fuer Kant sind alle notwendigen Wahrheiten a priori, alle a-posteriori-Wahrheiten kontingent",
        answerC = "'Julius Caesar ist Julius Caesar' ist notwendig a posteriori",
        answerD = "Kripke ist mit Kant vereinbar, weil er eine andere Definition von 'notwendig' verwendet",
        correctAnswer = 1,
        explanation = "Kripkes Revolution (1970er): Kant verknoepfte zwei Unterscheidungen — a priori/a posteriori (epistemisch: Weg der Erkenntnis) und notwendig/kontingent (metaphysisch: haette es anders sein koennen?). Kripke zeigt: Diese Unterscheidungen fallen auseinander. 'Wasser ist H2O' wurde empirisch entdeckt (a posteriori), aber es haette nicht anders sein koennen — in jeder moeglichen Welt ist die Substanz, die wir 'Wasser' nennen, H2O (notwendig). Umgekehrt gibt es kontingente a-priori-Wahrheiten ('Der Standardmeter in Paris ist 1 Meter lang').",
        difficulty = 5,
        funFact = "Kripkes Vorlesungen 'Naming and Necessity' (1970) galten als muendlich gehaltene Vorlesung ohne Skript. Sie wurden aus Bandaufnahmen transkribiert. Trotzdem gelten sie als eines der einflussreichsten philosophischen Werke des 20. Jahrhunderts."
    ),

    // Frage 44 — Wissenschaftliche Gesetze
    Question(
        categoryId = 11,
        questionText = "Was ist der Unterschied zwischen 'echten Naturgesetzen' (laws of nature) und blossen 'zufalligen Verallgemeinerungen' (accidental generalizations) in der Wissenschaftsphilosophie, und welche der drei Haupttheorien erklaert diesen Unterschied am einflussreichsten?",
        answerA = "Naturgesetze sind quantitativ; zufaellige Verallgemeinerungen qualitativ",
        answerB = "Echte Gesetze unterstuetzen Kontrafaktika ('Wenn x ein Kupferding waere, leitete es Elektrizitaet'); zufaellige Verallgemeinerungen nicht. Drei Haupttheorien: (1) Regularitaetstheorie (Hume): Gesetze sind nur allgemeine Wahrheiten. (2) Notwendigkeitstheorie (Armstrong, Tooley): Gesetze involvieren Notwendigkeitsrelationen zwischen Universalien. (3) Best-Systems-Theorie (Lewis): Gesetze sind Axiome des besten deduktiven Systems, das Einfachheit und Staerke maximal verbindet",
        answerC = "Naturgesetze sind von Wissenschaftlern anerkannt; zufaellige Verallgemeinerungen nicht",
        answerD = "Nur quantenmechanische Saetze sind echte Gesetze; klassische Physik beschreibt nur Naherungen",
        correctAnswer = 1,
        explanation = "Die Kontrafaktika-Frage ist entscheidend: 'Alle Saeugetiere stinken' (angenommen wahr) ist keine Gesetzmaessigkeit — ein Saeugetier, das nicht stinkt, waere trotzdem ein Saeugetier. 'Alle Kupferdinge leiten Strom' ist ein Gesetz — ein nicht-leitendes Ding waere kein Kupfer. Lewis' Best-Systems Analysis ist heute sehr einflussreich: Gesetze sind diejenigen allgemeinen Wahrheiten, die in einer optimalen systematischen Beschreibung des Universums als Axiome fungieren (Balance zwischen Einfachheit und informativer Staerke).",
        difficulty = 5
    ),

    // Frage 45 — Wissenschaft und Gesellschaft
    Question(
        categoryId = 11,
        questionText = "Was ist der Kern von Bruno Latours und Steve Woolgars 'Laboratoryleben' (1979) als Beitrag zur Wissenschaftssoziologie?",
        answerA = "Latour und Woolgar zeigten, dass Laborexperimente reproducierbar sein muessen",
        answerB = "Wissenschaftliche Fakten werden im Labor 'konstruiert' durch soziale Prozesse (Verhandlungen, Rhetorik, Inschriften in Geraete) — 'Tatsachen' gewinnen ihre Stabilitaet nicht durch Entsprechung zur Natur, sondern durch das Netzwerk von Uebersetzungen und Allianzbildungen in der wissenschaftlichen Gemeinschaft",
        answerC = "Laborleben dokumentiert, dass Wissenschaftler harte Realisten sind",
        answerD = "Das Buch argumentiert fuer mehr staatliche Foerderung von Grundlagenforschung",
        correctAnswer = 1,
        explanation = "Latour und Woolgar (Science and Technology Studies / STS): Statt zu fragen 'Wie entdecken Wissenschaftler Tatsachen?', fragten sie 'Wie werden Tatsachen im Labor gemacht?'. Ihre ethnographische Studie des Salk-Instituts zeigte: Wissenschaftliche Tatsachen durchlaufen Phasen der Modalisierung ('X behauptet, dass...', 'Es scheint, dass...', 'Es ist' — ohne Modifikation). Fakten werden 'black boxed', wenn Zweifel verschwinden. Latour entwickelte dies zur Akteur-Netzwerk-Theorie (ANT): Fakten, Menschen und Geraete sind gleichwertige Akteure.",
        difficulty = 5
    ),

    // Frage 46 — Transzendentale Erkenntnistheorie
    Question(
        categoryId = 11,
        questionText = "Was unterscheidet Kants 'transzendentale Erkenntnistheorie' von empiristischer und rationalistischer Erkenntnistheorie und was sind die 'Formen der sinnlichen Anschauung'?",
        answerA = "Kants transzendentale Erkenntnistheorie ist eine Variante des Rationalismus ohne praktische Relevanz",
        answerB = "Kant verbindet Empirismus (Erkenntnis beginnt mit Erfahrung) und Rationalismus (a-priori-Strukturen sind entscheidend): Unsere Sinne liefern Rohmaterial, aber Raum und Zeit als reine Anschauungsformen und die Verstandeskategorien (Kausalitaet, Substanz etc.) strukturieren erst die Erfahrung. 'Transzendental' heisst: Es untersucht die Bedingungen der Moeglichkeit von Erfahrung ueberhaupt, nicht Inhalte",
        answerC = "Die Formen der sinnlichen Anschauung sind bei Kant logische Kategorien wie Quantitaet und Qualitaet",
        answerD = "Transzendentale Erkenntnistheorie untersucht uebernatuerliche Quellen der Erkenntnis",
        correctAnswer = 1,
        explanation = "Kants 'Kritik der reinen Vernunft' (1781/1787): Anschauungsformen — Raum (Form des aeusseren Sinns) und Zeit (Form des inneren Sinns) — strukturieren alle sinnliche Wahrnehmung a priori. Verstandeskategorien (12 Stck.: Quantitaet, Qualitaet, Relation, Modalitaet) strukturieren Urteile. Das transzendentale Ich appliziert diese auf Erscheinungen. Erkenntnis ist so moeglich, aber auf Erscheinungen ('Phaenomene') begrenzt — das 'Ding an sich' ('Noumenon') bleibt unerkennbar. Kants Antwort auf Humes Skeptizismus: Kausalitaet ist keine Gewohnheit, sondern eine Bedingung moeglicher Erfahrung.",
        difficulty = 5
    ),

    // Frage 47 — Wissenschaftsphilosophie des Geistes
    Question(
        categoryId = 11,
        questionText = "Was ist das 'Hard Problem of Consciousness' (Chalmers) und warum stellt es eine Herausforderung fuer den wissenschaftlichen Naturalismus dar?",
        answerA = "Das Hard Problem ist die Frage, wie das Gehirn komplexe Berechnungen ausfuehren kann",
        answerB = "Chalmers unterscheidet 'easy problems' (wie das Gehirn Verhalten, Aufmerksamkeit und Berichte vermittelt — erklaerbar durch neuronale Mechanismen) vom 'hard problem': Warum ist irgendetwas davon mit subjektivem Erleben (Qualia: das Rotsein von Rot, der Schmerz von Schmerz) verbunden? Selbst wenn wir jeden neuronalen Korrelat kennen, bleibt erklaerungslos, warum es 'sich nach etwas anfuehlt'",
        answerC = "Das Hard Problem ist die Frage der freien Entscheidung gegen deterministische neuronale Prozesse",
        answerD = "Es ist das Problem, kuenstliche Intelligenz mit echtem Bewusstsein auszustatten",
        correctAnswer = 1,
        explanation = "Chalmers' 'The Conscious Mind' (1996): Die 'explanatory gap' (erklaerende Luecke, Levine) — selbst eine vollstaendige Neurowissenschaft erklaert nicht, warum neuronale Aktivitaet subjektives Erleben erzeugt. Physikalismus muss entweder sagen: (a) Qualia sind illusorisch (Eliminativismus, Churchland), (b) Funktionalismus ist ausreichend (Dennett), oder (c) Qualia sind physische Eigenschaften, aber eine breitere Physik (Panpsychismus). Chalmers selbst bevorzugt einen 'naturalistischen Dualismus': Bewusstsein ist real, aber nicht physisch reduzierbar.",
        difficulty = 5,
        funFact = "Der Begriff 'Qualia' (Einzahl: Quale) wurde von C.I. Lewis 1929 eingefuehrt. Die bekannteste Illustration ist Frank Jacksons Gedankenexperiment 'Mary's Room' (1982): Mary weiss alles ueber Farbwahrnehmung, war aber ihr Leben lang in einem Schwarz-Weiss-Raum. Lernt sie etwas Neues, wenn sie erstmals Rot sieht?"
    ),

    // Frage 48 — Wissenschaft und Werte (Wertfreiheitsideal)
    Question(
        categoryId = 11,
        questionText = "Was ist der Unterschied zwischen 'konstitutiven Werten' und 'kontextuellen Werten' in der Wissenschaftsphilosophie (Longino), und warum ist diese Unterscheidung wissenschaftlich und politisch bedeutsam?",
        answerA = "Konstitutive Werte beziehen sich auf die Forschungsfoerderung; kontextuelle auf die Ergebnisse",
        answerB = "Konstitutive (epistemische) Werte (Praezision, Konsistenz, erklaerende Kraft) gelten als legitimate Kriterien wissenschaftlicher Erklaerung. Kontextuelle (nicht-epistemische) Werte (politische, wirtschaftliche, kulturelle Interessen) gelten traditionell als illegitime Einfluesse. Longinos These: Diese Trennung ist nicht aufrechtzuerhalten — kontextuelle Werte beeinflussen auch die Wahl epistemischer Standards",
        answerC = "Konstitutive Werte gelten nur in der Physik; kontextuelle nur in den Sozialwissenschaften",
        answerD = "Kontextuelle Werte sind in der Wissenschaft stets verboten; konstitutive Werte erwaenscht",
        correctAnswer = 1,
        explanation = "Longinos 'Science as Social Knowledge' (1990): Das Beispiel der Agrarwissenschaft zeigt die Bedeutung: Welche Probleme als relevant gelten (Ertragsmaximierung vs. Biodiversitaet), welche Modelle als erklaerungskraeftig gelten — das haengt von Interessen und Werten ab. In der Medizin: Die Definition von 'Krankheit' ist normbehaftet (medicalization debate). Longino schlaegt 'kritischen Kontextualismus' vor: Offene Kommunikation und Pluralismus ermoeglicht intersubjektive Objektivitaet trotz wertgeladenem Kontext.",
        difficulty = 5
    ),

    // Frage 49 — Mechanistische Erklarung
    Question(
        categoryId = 11,
        questionText = "Was ist der Kern der 'mechanistischen Erklaerung' (Machamer, Darden, Craver) und warum wird sie als Alternative zum Hempel'schen Erklaerungsmodell in der Biologie bevorzugt?",
        answerA = "Mechanistische Erklaerungen verwenden Maschinen-Analogien, die in der Physik nicht funktionieren",
        answerB = "Mechanismen bestehen aus Entitaeten (mit Aktivitaeten und Eigenschaften) und den Aktivitaeten, die sie durchfuehren, in einer kausal-zeitlichen Organisation, die ein Phaenomen produziert. Im Gegensatz zu Hempels Gesetzesableitung: Biologische Erklaerungen (Neurotransmission, Zellmitose) nennen keine Gesetze, sondern beschreiben kausal-mechanische Ablaeufe mit Entitaeten auf verschiedenen Organisationsebenen",
        answerC = "Mechanistische Erklaerungen sind reduktionistischer als das DN-Modell",
        answerD = "Das mechanistische Modell wurde von Hempel selbst fuer die Biologie entwickelt",
        correctAnswer = 1,
        explanation = "Machamer, Darden, Craver 'Thinking about Mechanisms' (2000): Das Standardbeispiel ist die Synaptische Uebertragung — kein Gesetz, sondern eine Beschreibung von Entitaeten (Vesikel, Rezeptoren, Ionen) und Aktivitaeten (Diffusion, Bindung, Oeffnung). Erklaerungen in der Biologie nennen Mechanismen, nicht Gesetze. Das passt zum 'levels of organization' Ansatz (Craver): Erklaerungen gehen von einer Phaenomenebene auf tiefere Mechanismusebenen. Dies wird als realistischer Ansatz betrachtet, der Biologie nicht auf Physik reduziert.",
        difficulty = 5
    ),

    // Frage 50 — Abduktion und Inference to the Best Explanation
    Question(
        categoryId = 11,
        questionText = "Peter Lipton entwickelte in 'Inference to the Best Explanation' (1991/2004) einen ausgefeilten IBE-Begriff. Was ist der Unterschied zwischen der 'lovelier' und 'likeliest' Erklaerung und was ist Liptons 'loveliness-likeliness gap'?",
        answerA = "Lovelier und likeliest sind identische Begriffe; der Gap bezeichnet die Differenz zwischen schlechten und guten Erklaerungen",
        answerB = "Loveliest Erklaerung: die verstehende (understanding-providing) Erklaerung, die am tiefsten einsichtig ist (kausal, unifizierend, detailliert). Likeliest Erklaerung: die wahrscheinlichste Erklaerung angesichts der Evidenz. Liptons Gap-Problem: Diese koennen auseinanderfallen — die tiefste, most-understanding Erklaerung muss nicht die wahrscheinlichste sein. IBE muss erklaeren, welche wir waehlen sollen",
        answerC = "Likeliest bedeutet empirisch bestaetigt; lovelier bedeutet eleganter formuliert",
        answerD = "Der Gap beschreibt den Unterschied zwischen wissenschaftlicher und alltagssprachlicher Erklaerung",
        correctAnswer = 1,
        explanation = "Liptons ausgefeiltes IBE: 'Lovely' Erklaerungen bieten echtes Verstehen — sie zeigen warum etwas so ist, bieten kausale Tiefe und Unifikation. 'Likely' bedeutet: am besten durch verfuegbare Evidenz gestuetzt. In der Praxis zielt IBE auf die lovliest Erklaerung und hofft, dass diese auch die likeliest ist. Aber van Fraassens Einwand: Warum sollte eine tolle Erklaerung wahrscheinlicher sein? Die Natur muss unsere Erklaerungsideale nicht teilen. Lipton antwortete: Lovliness ist epistemisch ein verlässlicher Indikator fuer Likeness, weil unsere Erklaerungsideale evolutionaer auf Wahrheit ausgerichtet wurden.",
        difficulty = 5,
        funFact = "Peter Lipton starb ueberraschend 2007 mit 53 Jahren, kurz nachdem er den Hans Rausing Chair in History and Philosophy of Science in Cambridge erhalten hatte. Sein Buch 'Inference to the Best Explanation' wird von Wissenschaftsphilosophen als Standardwerk bezeichnet."
    )

)
