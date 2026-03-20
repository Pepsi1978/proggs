package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsExpert2(): List<Question> = listOf(

    // --- PHILOSOPHIE — EXPERTEN-NIVEAU (50 Fragen) ---
    // correctAnswer-Verteilung: 0→13, 1→13, 2→12, 3→12

    // 1 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welchen Begriff verwendete Heidegger für die Tatsache, dass das Dasein sich immer schon in einer Welt vorfindet, ohne diese Situation gewählt zu haben?",
        answerA = "Transzendenz",
        answerB = "Verfallenheit",
        answerC = "Geworfenheit",
        answerD = "Erschlossenheit",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Heidegger nennt in 'Sein und Zeit' (1927) die Tatsache, dass das Dasein ungefragt in eine bestimmte Welt, Zeit und Situation hineingeworfen wird, 'Geworfenheit' (Faktizität). Das Dasein findet sich vor, ohne seinen Ursprung gewählt zu haben.",
        funFact = "'Sein und Zeit' blieb Fragment — Heidegger veröffentlichte nur die ersten zwei Divisionen. Den dritten Teil, der die 'Zeitlichkeit des Seins' behandeln sollte, gab er nie heraus."
    ),

    // 2 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was versteht Heidegger unter 'Angst' als Grundstimmung des Daseins — im Unterschied zur Furcht?",
        answerA = "Angst hat ein konkretes Objekt, Furcht dagegen ist gegenstandslos",
        answerB = "Angst ist gegenstandslos und erschließt die eigene Nichtigkeit; Furcht hat ein bestimmtes Objekt",
        answerC = "Angst ist eine pathologische Störung, Furcht eine philosophisch relevante Stimmung",
        answerD = "Angst und Furcht sind bei Heidegger synonym",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "In 'Sein und Zeit' §40 unterscheidet Heidegger: Furcht hat immer ein bestimmtes Seiendes als Objekt. Angst dagegen ist unbestimmt — ihr 'Wovor' ist das In-der-Welt-sein selbst. Sie reißt das Dasein aus dem Alltag und konfrontiert es mit seiner eigenen Nichtigkeit und Freiheit.",
        funFact = null
    ),

    // 3 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welches philosophische Problem beschreibt David Chalmers mit dem Begriff 'The Hard Problem of Consciousness'?",
        answerA = "Warum physische Prozesse subjektives Erleben (Qualia) erzeugen",
        answerB = "Warum das Gehirn überhaupt Informationen verarbeitet",
        answerC = "Wie freier Wille mit Determinismus vereinbar ist",
        answerD = "Ob andere Menschen wirklich Bewusstsein besitzen",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Chalmers unterscheidet 'easy problems' (Erklärung kognitiver Funktionen) vom 'hard problem': Warum und wie physische Gehirnprozesse überhaupt subjektive, qualitative Erlebnisse (Qualia) erzeugen — warum es sich 'wie etwas anfühlt', rot zu sehen oder Schmerz zu empfinden.",
        funFact = "Chalmers prägte den Begriff 1995 in seinem Aufsatz 'Facing Up to the Problem of Consciousness'. Er gilt heute als einer der meistzitierten Texte der Philosophie des Geistes."
    ),

    // 4 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was besagt Thomas Nagels berühmter Aufsatz 'What Is It Like to Be a Bat?' (1974)?",
        answerA = "Fledermäuse besitzen kein echtes Bewusstsein, da sie keine Sprache haben",
        answerB = "Die Echolokation der Fledermaus ist ein Modell für menschliche Wahrnehmung",
        answerC = "Tierrechte lassen sich aus dem Gedankenexperiment ableiten",
        answerD = "Subjektives Erleben kann nicht durch objektive physikalische Beschreibungen erfasst werden",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Nagel argumentiert: Wir können zwar physikalisch alles über Fledermäuse wissen, aber niemals verstehen, wie es sich subjektiv anfühlt, eine Fledermaus zu sein. Dies zeigt, dass subjektives Erleben nicht durch objektiv-physikalische Beschreibungen erfasst wird.",
        funFact = "Nagels Aufsatz löste eine Flut von Reaktionen aus und gilt als Schlüsseltext, der die Qualia-Debatte in der analytischen Philosophie des 20. Jahrhunderts entfachte."
    ),

    // 5 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welches Argument verwendete Ludwig Wittgenstein in den 'Philosophischen Untersuchungen', um zu zeigen, dass eine vollständig private Sprache unmöglich ist?",
        answerA = "Das Induktionsproblem",
        answerB = "Der Kategorienfehler",
        answerC = "Das Käfer-Gleichnis (Privatsprachen-Argument)",
        answerD = "Das Lügner-Paradoxon",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Im Käfer-Gleichnis (§293 der Philosophischen Untersuchungen) stellt Wittgenstein vor: Jeder hat eine Schachtel mit einem 'Käfer', in die nur er schauen kann. Selbst wenn der Inhalt verschieden oder leer wäre, würde sich an der Sprache nichts ändern. Daraus folgt: Bedeutung liegt in öffentlichem Gebrauch, nicht in privaten inneren Zuständen.",
        funFact = "Wittgenstein veröffentlichte zu Lebzeiten nur einen einzigen philosophischen Text als Buch: den 'Tractatus Logico-Philosophicus' (1921). Die 'Philosophischen Untersuchungen' erschienen posthum 1953."
    ),

    // 6 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was meinte Wittgenstein mit dem Begriff 'Sprachspiel' in seinen 'Philosophischen Untersuchungen'?",
        answerA = "Die kontextgebundenen, regelgeleiteten Praktiken, in denen Sprache verwendet wird",
        answerB = "Ein Kinderspiel zur spielerischen Sprachentwicklung",
        answerC = "Rhetorische Tricks zur Verwirrung des Gesprächspartners",
        answerD = "Die formale Grammatik einer Sprache",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Wittgenstein zeigt: Sprache hat keine einheitliche Bedeutungsstruktur, sondern besteht aus vielen 'Sprachspielen' — unterschiedlichen regelgeleiteten Praktiken des Sprachgebrauchs (Befehlen, Beschreiben, Bitten, Scherzen etc.). Bedeutung ist Gebrauch ('die Bedeutung eines Wortes ist sein Gebrauch in der Sprache').",
        funFact = null
    ),

    // 7 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was versteht John Rawls unter dem 'Schleier des Nichtwissens' (veil of ignorance) in 'A Theory of Justice' (1971)?",
        answerA = "Die menschliche Unfähigkeit, die Zukunft vorauszusehen",
        answerB = "Die moralische Unwissenheit der Masse",
        answerC = "Die Theorie, dass Wissen immer perspektivisch und verzerrt ist",
        answerD = "Ein Gedankenexperiment, in dem Entscheider ihre gesellschaftliche Position nicht kennen, um gerechte Prinzipien zu wählen",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Im 'Urzustand' (original position) wissen die Vertragsparteien nicht, welche gesellschaftliche Position, Klasse, Fähigkeiten oder Präferenzen sie haben werden. Hinter diesem Schleier wählen sie rational Gerechtigkeitsprinzipien — Rawls' Ergebnis: das Differenzprinzip und gleiche Grundfreiheiten.",
        funFact = "Rawls' 'A Theory of Justice' gilt als das einflussreichste politisch-philosophische Werk des 20. Jahrhunderts und löste eine Renaissance der normativen Ethik aus."
    ),

    // 8 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Wie lautet Robert Nozicks libertäre Kritik an Rawls' Umverteilungstheorie in 'Anarchy, State, and Utopia' (1974)?",
        answerA = "Rawls berücksichtigt nicht genug die Rechte der Armen",
        answerB = "Jede Umverteilung verletzt individuelle Eigentumsrechte und ist einer Zwangsarbeit vergleichbar",
        answerC = "Der Staat sollte stärker in die Wirtschaft eingreifen als Rawls fordert",
        answerD = "Das Differenzprinzip ist logisch inkonsistent",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Nozick argumentiert aus einer Anspruchstheorie der Gerechtigkeit: Eigentumsrechte sind absolut, solange sie durch freien Erwerb oder freien Tausch entstanden. Steuern zur Umverteilung sind Zwang — Nozick vergleicht sie mit Zwangsarbeit. Nur ein minimaler 'Nachtwächterstaat' ist legitim.",
        funFact = "Nozick nannte Rawls' Werk explizit als Auslöser für 'Anarchy, State, and Utopia'. Das Buch gewann 1975 den National Book Award und machte den Libertarismus philosophisch hoffähig."
    ),

    // 9 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was beschreibt das 'Trolley-Problem', das Philippa Foot 1967 formulierte?",
        answerA = "Das Dilemma zwischen persönlicher Freiheit und gesellschaftlicher Pflicht",
        answerB = "Die Frage, ob Technologie den Menschen ersetzen kann",
        answerC = "Ein moralisches Dilemma zwischen aktivem Eingriff (eine Person opfern) und passivem Geschehenlassen (fünf sterben lassen)",
        answerD = "Das Problem der gerechten Ressourcenverteilung im Kapitalismus",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Foot beschreibt: Eine Straßenbahn rollt auf fünf Personen zu. Durch Umlegen einer Weiche rettet man die fünf, tötet aber eine Person auf dem Nebengleis. Das Dilemma trennt utilitaristische (Gesamtnutzen maximieren) von deontologischen (keine aktive Tötung) Positionen.",
        funFact = "Die Fußgängerbrücken-Variante ('fat man'-Variante) wurde von Judith Jarvis Thomson 1985 eingeführt: Hier wird ein dicker Mann von der Brücke gestoßen. Die meisten Menschen, die die Weiche umlegen würden, lehnen dies ab — ein Hinweis auf die Bedeutung von Berührung und Kausalität in der Moral."
    ),

    // 10 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was besagt Kants 'Kopernikanische Wende' in der Erkenntnistheorie?",
        answerA = "Nicht Erkenntnis richtet sich nach Gegenständen, sondern Gegenstände richten sich nach unserer Erkenntnis",
        answerB = "Die Erde dreht sich um die Sonne — also ist unser Weltbild immer perspektivisch",
        answerC = "Die menschliche Vernunft ist von Natur aus fehlerhaft",
        answerD = "Alle Erkenntnis ist letztlich auf Sinneseindrücke zurückzuführen",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Kant dreht in der 'Kritik der reinen Vernunft' (1781) die traditionelle Erkenntnistheorie um: Nicht unser Geist passt sich der Wirklichkeit an, sondern die Erscheinungen passen sich den apriorischen Formen unserer Anschauung (Raum, Zeit) und den Kategorien des Verstandes an.",
        funFact = null
    ),

    // 11 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist der Unterschied zwischen 'analytischen' und 'synthetischen' Urteilen bei Kant?",
        answerA = "Analytische Urteile sind falsch, synthetische wahr",
        answerB = "Analytische Urteile basieren auf Erfahrung, synthetische auf reiner Vernunft",
        answerC = "Analytische Urteile sind wissenschaftlich, synthetische philosophisch",
        answerD = "Analytische Urteile entfalten nur, was im Begriff enthalten ist; synthetische fügen echte neue Erkenntnis hinzu",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Analytisch: Das Prädikat ist im Subjektbegriff schon enthalten (z.B. 'Alle Körper sind ausgedehnt'). Synthetisch: Das Prädikat erweitert den Begriff (z.B. 'Alle Körper sind schwer'). Kants zentrale Frage: Wie sind synthetische Urteile a priori möglich? — das ist die Grundfrage der Erkenntnistheorie.",
        funFact = "Kant bezeichnete die Frage 'Wie sind synthetische Urteile a priori möglich?' als das 'eigentliche Problem der reinen Vernunft', das er in der Kritik lösen wollte."
    ),

    // 12 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Welches Konzept beschreibt Hegels Begriff der 'Aufhebung' in seiner Dialektik?",
        answerA = "Die vollständige Vernichtung einer These durch die Antithese",
        answerB = "Das dreifache Moment: Negieren, Bewahren und auf eine höhere Ebene Heben",
        answerC = "Die Rückkehr zum Ausgangspunkt nach einem Kreislauf",
        answerD = "Die Aufhebung aller Gesetze im Absoluten Geist",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "'Aufheben' hat bei Hegel drei Bedeutungen gleichzeitig: (1) Negieren/Beseitigen, (2) Bewahren/Erhalten, (3) Auf eine höhere Stufe Heben. Das Negative wird bewahrt, aber in einer höheren Einheit überwunden — die dialektische Bewegung des Geistes.",
        funFact = "Hegel war so begeistert von Napoleon, den er 'den Weltgeist zu Pferde' nannte, dass er die Phänomenologie des Geistes am Tag der Schlachtentscheidung von Jena (1806) fertigstellte."
    ),

    // 13 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was meinte Edmund Husserl mit der 'epoché' (phänomenologischen Reduktion)?",
        answerA = "Das Einklammern der natürlichen Einstellung, um Bewusstseinsinhalte rein zu beschreiben",
        answerB = "Die historische Epoche, in der die Phänomenologie entstand",
        answerC = "Die Methode des logischen Beweises durch Widerlegung",
        answerD = "Die Reduktion aller Erkenntnis auf sinnliche Wahrnehmung",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Die epoché (griech. 'Enthaltung') ist Husserls Methode: Man 'klammert' die Frage nach der Existenz der Außenwelt ein und konzentriert sich auf die Struktur des reinen Bewusstseins und seiner intentionalen Akte. Ziel ist die Beschreibung der Phänomene, wie sie dem Bewusstsein erscheinen.",
        funFact = "Husserl hinterließ rund 40.000 Seiten unveröffentlichte Manuskripte in Kurzschrift, die nach seinem Tod 1938 heimlich aus Nazi-Deutschland nach Belgien geschmuggelt wurden — heute als 'Husserl-Archiv' in Löwen zugänglich."
    ),

    // 14 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was verstand Husserl unter 'Intentionalität' als grundlegender Eigenschaft des Bewusstseins?",
        answerA = "Die Absicht, etwas zu tun oder zu planen",
        answerB = "Den freien Willen als Quelle moralischer Handlung",
        answerC = "Die Eigenschaft des Bewusstseins, immer auf etwas gerichtet zu sein",
        answerD = "Die Fähigkeit, zukünftige Ereignisse vorherzusehen",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Husserl übernimmt von Franz Brentano den Begriff: Jeder Bewusstseinsakt (Wahrnehmen, Vorstellen, Urteilen, Wünschen) ist auf ein Objekt gerichtet — 'Bewusstsein ist immer Bewusstsein von etwas'. Diese Gerichtetheit heißt Intentionalität und ist das Kernmerkmal des Geistigen.",
        funFact = null
    ),

    // 15 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Welche These vertritt der Physikalismus (Materialismus) in der Philosophie des Geistes?",
        answerA = "Geist und Materie sind zwei gleich fundamentale Substanzen",
        answerB = "Geistige Zustände sind irreduzibel und existieren unabhängig vom Körper",
        answerC = "Das Bewusstsein ist eine Illusion ohne reale Existenz",
        answerD = "Alle mentalen Zustände sind letztlich physikalische Zustände des Gehirns",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Der Physikalismus besagt: Alles, was existiert, ist letztlich physikalisch. Mentale Zustände wie Gedanken, Schmerzen und Überzeugungen sind identisch mit oder superveniieren auf Gehirnzuständen. Varianten: Identitätstheorie (Smart, Place), Funktionalismus (Putnam), Eliminativer Materialismus (Churchland).",
        funFact = "Der Funktionalismus argumentiert, dass mentale Zustände durch ihre funktionale Rolle definiert sind, nicht durch ihre materielle Realisierung. Das macht Multiple Realizability möglich: Auch Silizium könnte denken."
    ),

    // 16 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was ist das 'Leib-Seele-Problem' (mind-body problem) in der Philosophie?",
        answerA = "Die Frage, ob der Leib nach dem Tod weiterlebt",
        answerB = "Die Frage, wie sich physische Gehirnzustände zu mentalen Zuständen verhalten",
        answerC = "Das ethische Problem der Körperverletzung",
        answerD = "Die Frage nach der Einheit von Körper und Psyche in der Medizin",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Das Leib-Seele-Problem fragt: Wie verhält sich das Physische (Neuronen, Gehirn) zum Mentalen (Gedanken, Schmerzen, Überzeugungen)? Hauptpositionen: Dualismus (Descartes: zwei Substanzen), Physikalismus, Panpsychismus, Epiphänomenalismus, Eigenschaftsdualismus.",
        funFact = "Descartes lokalisierte die Verbindung zwischen Leib und Seele in der Zirbeldrüse — heute wissen wir, dass diese Drüse Melatonin produziert und keine besondere Verbindungsrolle spielt."
    ),

    // 17 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welchen Begriff prägte Aristoteles für das Sein des Seienden — die grundlegende Substanz, die ein Ding zu dem macht, was es ist?",
        answerA = "Ousia (Substanz/Wesen)",
        answerB = "Hyle (Materie)",
        answerC = "Morphe (Form/Gestalt)",
        answerD = "Eidos (Idee/Art)",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Ousia (ousia) ist bei Aristoteles der Grundbegriff der Metaphysik: das Sein des Seienden, die Substanz. Im Gegensatz zu Platons Ideen sind bei Aristoteles die Formen in den Dingen selbst (Hylomorphismus): Materie (hyle) wird durch Form (morphe) zu einer konkreten Substanz (ousia).",
        funFact = "Aristoteles' 'Metaphysik' hat ihren Namen nicht von Aristoteles selbst, sondern vom hellenistischen Bibliographen Andronikos von Rhodos, der die Schriften 'nach der Physik' (meta ta physika) ordnete."
    ),

    // 18 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Aristoteles mit den 'vier Ursachen' (vier Arten von Gründen)?",
        answerA = "Die vier Grundelemente Feuer, Wasser, Luft, Erde",
        answerB = "Die vier logischen Schlussformen des syllogistischen Beweises",
        answerC = "Material-, Form-, Wirk- und Finalursache als vollständige Erklärung eines Dinges",
        answerD = "Die vier Tugenden Weisheit, Mut, Besonnenheit und Gerechtigkeit",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Aristoteles erklärt ein Ding durch vier Ursachen: (1) Materialursache (Stoff, aus dem es besteht), (2) Formursache (das Wesen/die Form), (3) Wirkursache (was es hervorgebracht hat), (4) Finalursache (der Zweck/das Ziel). Erst alle vier zusammen geben eine vollständige Erklärung.",
        funFact = null
    ),

    // 19 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Welches Argument führte Anselm von Canterbury für die Existenz Gottes an (ontologischer Gottesbeweis)?",
        answerA = "Alles Existierende hat eine Ursache; diese Kausalkette muss bei einem ersten Beweger enden",
        answerB = "Die Schönheit und Ordnung der Welt beweist einen Schöpfer",
        answerC = "Das allgemeine Gottesgefühl aller Menschen beweist Gottes Existenz",
        answerD = "Gott ist das, worüber hinaus nichts Größeres gedacht werden kann — also muss er auch real existieren",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Anselm formuliert in 'Proslogion' (1078): Gott ist 'id quo maius cogitari non potest' (das, worüber hinaus nichts Größeres gedacht werden kann). Ein solches Wesen, das nur im Denken existiert, wäre kleiner als eines, das auch real existiert. Also muss Gott real existieren.",
        funFact = "Kant widerlegte den ontologischen Gottesbeweis mit der Feststellung, dass 'Existenz kein Prädikat ist' — man fügt dem Begriff nichts hinzu, indem man sagt, dass etwas existiert."
    ),

    // 20 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was versteht man unter dem 'Sein-Sollen-Fehlschluss' (Humes Guillotine)?",
        answerA = "Den unzulässigen Schluss von Tatsachenaussagen (Sein) auf normative Aussagen (Sollen)",
        answerB = "Den Fehler, aus logischen Prämissen empirische Schlüsse zu ziehen",
        answerC = "Die falsche Annahme, dass Wille und Verstand identisch sind",
        answerD = "Den Widerspruch zwischen absolutem Wissen und relativem Handeln",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "David Hume bemerkte in 'A Treatise of Human Nature' (1739): Viele moralische Theorien springen unvermittelt von 'ist'-Aussagen zu 'soll'-Aussagen. Doch aus rein deskriptiven Prämissen folgt logisch keine normative Konklusion. Diese Lücke gilt als grundlegendes Problem der Metaethik.",
        funFact = "G.E. Moore nannte den verwandten Fehler 'Naturalistic Fallacy': Moralische Eigenschaften (wie 'gut') lassen sich nicht auf natürliche Eigenschaften (wie 'lustvoll' oder 'nützlich') reduzieren."
    ),

    // 21 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das philosophische Problem des 'freien Willens' im Kontext des Determinismus?",
        answerA = "Ob Menschen schneller laufen können als ihre genetischen Anlagen es erlauben",
        answerB = "Ob menschliche Entscheidungen wirklich frei sind, wenn alle Ereignisse durch Naturgesetze determiniert sind",
        answerC = "Ob Tiere den gleichen freien Willen wie Menschen besitzen",
        answerD = "Ob politische Freiheit ein Naturrecht oder eine gesellschaftliche Konstruktion ist",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der Determinismus besagt: Jedes Ereignis, einschließlich menschlicher Entscheidungen, ist durch vorherige Ursachen vollständig bestimmt. Der Kompatibilismus (Hume, Frankfurt) sagt: Freiheit und Determinismus sind vereinbar. Der Inkompatibilismus bestreitet dies. Der Libertarismus (Kane) verteidigt genuine Anderskönnlichkeit.",
        funFact = "Benjamin Libets neuropsychologisches Experiment (1983) zeigte, dass das Gehirn Bewegungsimpulse auslöst, bevor der Proband sich bewusst entschieden hat. Dies entfachte die Debatte um den freien Willen neu — obwohl spätere Experimente das Bild nuancierten."
    ),

    // 22 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist der 'Kompatibilismus' in der Debatte um freien Willen?",
        answerA = "Die These, dass Determinismus und freier Wille sich gegenseitig ausschließen",
        answerB = "Die Position, dass Quantenindeterminiertheit den freien Willen ermöglicht",
        answerC = "Die Überzeugung, dass freier Wille eine religiöse Illusion ist",
        answerD = "Die These, dass freier Wille und Determinismus miteinander vereinbar sind",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Kompatibilisten (Hume, Kant in praktischer Hinsicht, Frankfurt, Dennett) argumentieren: 'Freier Wille' bedeutet nicht, außerhalb der Kausalität zu stehen, sondern aus eigenen Wünschen und Überlegungen zu handeln, ohne äußeren Zwang. Determinismus schließt diese Art von Freiheit nicht aus.",
        funFact = null
    ),

    // 23 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welcher Philosoph entwickelte das Konzept des 'Panoptikums' als Modell sozialer Kontrolle, das Michel Foucault philosophisch analysierte?",
        answerA = "Jeremy Bentham",
        answerB = "John Stuart Mill",
        answerC = "Thomas Hobbes",
        answerD = "Herbert Spencer",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Jeremy Bentham entwarf das Panoptikum (1785–91) als Gefängnisarchitektur: Ein zentraler Wachturm ermöglicht die potenzielle Beobachtung aller Häftlinge, ohne dass diese wissen, ob sie tatsächlich beobachtet werden. Foucault analysierte es in 'Überwachen und Strafen' (1975) als Modell disziplinärer Macht.",
        funFact = "Benthams Körper — der 'Auto-Icon' — ist bis heute im University College London ausgestellt. Der mumifizierte Kopf wurde 1975 gestohlen und durch einen Wachskopf ersetzt."
    ),

    // 24 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was bezeichnete Nietzsche mit dem 'Nihilismus' als historische Diagnose seiner Zeit?",
        answerA = "Die politische Bewegung russischer Anarchisten",
        answerB = "Die philosophische Position, dass die Außenwelt nicht existiert",
        answerC = "Den Zusammenbruch aller überlieferten Werte und den Verlust des Sinns nach dem 'Tod Gottes'",
        answerD = "Den Verzicht auf alle materiellen Güter",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Nietzsche diagnostiziert: Mit dem Niedergang des christlichen Weltbildes ('Gott ist tot') verlieren die bisherigen Werte (Moral, Wahrheit, Zweck) ihren Halt. Passiver Nihilismus ist Resignation; aktiver Nihilismus zerstört Altes, um Raum für neue Wertsetzung zu schaffen — Aufgabe des Übermenschen.",
        funFact = "'Gott ist tot' erscheint erstmals in Nitzsches 'Die fröhliche Wissenschaft' (1882). Der tolle Mensch fragt: 'Wer gab uns den Schwamm, um den ganzen Horizont wegzuwischen?' — eine der philosophisch dramatischsten Passagen des 19. Jahrhunderts."
    ),

    // 25 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was versteht man in der Philosophie unter 'Solipsismus'?",
        answerA = "Die Überzeugung, dass die Gesellschaft dem Individuum untergeordnet ist",
        answerB = "Die These, dass nur das eigene Bewusstsein sicher existiert und die Außenwelt unbeweisbar ist",
        answerC = "Den Glauben an die Unsterblichkeit der Seele",
        answerD = "Die moralische Position absoluter Selbstlosigkeit",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Solipsismus (lat. solus ipse — 'ich allein'): Nur das eigene Bewusstsein ist sicher erkennbar; ob andere Geister oder die Außenwelt wirklich existieren, kann nicht bewiesen werden. Erkenntnistheoretischer Solipsismus ist kaum widerlegbar, aber auch kaum vertretbar.",
        funFact = null
    ),

    // 26 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Wer schrieb den 'Gesellschaftsvertrag' (Du Contrat Social, 1762) und prägte den Begriff der 'Volkssouveränität'?",
        answerA = "Jean-Jacques Rousseau",
        answerB = "Thomas Hobbes",
        answerC = "John Locke",
        answerD = "Montesquieu",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Rousseau lehrte: Der Gesellschaftsvertrag begründet den Staat durch den allgemeinen Willen (volonte generale) aller Bürger — nicht durch bloße Mehrheitsmeinung. Der Mensch ist von Natur gut; die Zivilisation verdirbt ihn. Die Volkssouveränität ist unveräußerlich und unteilbar.",
        funFact = "Rousseaus Satz 'Der Mensch ist frei geboren, und überall liegt er in Ketten' eröffnet das erste Kapitel des Contrat Social und ist einer der meistzitierten Sätze der Politikphilosophie."
    ),

    // 27 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was unterscheidet Hobbes' Staatskonzept im 'Leviathan' (1651) von Lockes Gesellschaftsvertragstheorie?",
        answerA = "Hobbes lehnte jeden Staat ab; Locke befürwortete absolute Monarchie",
        answerB = "Beide Theorien sind inhaltlich identisch",
        answerC = "Hobbes befürwortet Demokratie; Locke Monarchie",
        answerD = "Hobbes begründet absolute Souveränität durch Sicherheit; Locke betont Naturrechte und Gewaltentrennung",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Hobbes: Im Naturzustand herrscht Krieg aller gegen alle ('homo homini lupus'). Der Vertrag überträgt alle Macht auf den Souverän. Locke: Menschen haben unveräußerliche Naturrechte (Leben, Freiheit, Eigentum). Der Staat dient deren Schutz; bei Versagen darf er gestürzt werden — Grundlage liberaler Demokratie.",
        funFact = "Lockes politische Philosophie beeinflusste direkt Thomas Jefferson und die US-Unabhängigkeitserklärung (1776). Die 'inalienable rights' spiegeln Lockes Triade Leben, Freiheit, Eigentum."
    ),

    // 28 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der Begriff 'Hermeneutik' in der philosophischen Tradition?",
        answerA = "Die Lehre vom Aufbau logischer Argumente",
        answerB = "Die Theorie und Methode der Interpretation von Texten und Sinn",
        answerC = "Die Untersuchung der mathematischen Grundlagen der Sprache",
        answerD = "Die philosophische Analyse von Träumen und dem Unbewussten",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Hermeneutik (von Hermes, dem Götterboten) ist die Lehre vom Verstehen und Auslegen. Wichtige Vertreter: Schleiermacher, Dilthey, Gadamer ('Wahrheit und Methode', 1960), Ricoeur. Gadamer entwickelt den 'hermeneutischen Zirkel': Teile verstehen wir nur vom Ganzen her, das Ganze nur von seinen Teilen.",
        funFact = "Hans-Georg Gadamer vollendete 'Wahrheit und Methode' mit 60 Jahren und lebte danach noch über 40 Jahre — bis ins Jahr 2002, als er 102-jährig starb."
    ),

    // 29 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was ist der 'hermeneutische Zirkel' in der Interpretation von Texten?",
        answerA = "Die Fehlerquelle, bei der Interpreten ihre eigene Meinung in Texte hineinlesen",
        answerB = "Die Pflicht des Interpreten, alle bekannten Texte eines Autors zu lesen",
        answerC = "Die Zirkularstruktur, in der Teile und Ganzes sich gegenseitig zur Auslegung bedingen",
        answerD = "Die zirkuläre Begründung religiöser Texte durch sich selbst",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Der hermeneutische Zirkel besagt: Um einen Teil (Satz, Abschnitt) zu verstehen, braucht man das Ganze; um das Ganze zu verstehen, muss man die Teile kennen. Gadamer wertet diese Zirkularität positiv: Sie ist keine logische Falle, sondern die produktive Struktur des Verstehens.",
        funFact = null
    ),

    // 30 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welche These vertritt die 'Kritische Theorie' der Frankfurter Schule (Horkheimer, Adorno) in der 'Dialektik der Aufklärung'?",
        answerA = "Die Aufklärung ist in ihren eigenen Rationalismus umgeschlagen und produziert neue Barbarei",
        answerB = "Der Kapitalismus ist das beste Wirtschaftssystem und muss verteidigt werden",
        answerC = "Alle gesellschaftlichen Probleme lassen sich durch wissenschaftliche Methoden lösen",
        answerD = "Religion ist die einzige Alternative zur kapitalistischen Gesellschaft",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "In der 'Dialektik der Aufklärung' (1944) argumentieren Horkheimer und Adorno: Die Aufklärung, die Natur und Mythos überwinden wollte, hat sich in ein neues Instrument der Herrschaft und Manipulation verwandelt (instrumentelle Vernunft). Holocaust und Massenkultur sind Produkte einer pervertierten Vernunft.",
        funFact = "Die 'Dialektik der Aufklärung' wurde im amerikanischen Exil verfasst und war ursprünglich nur für den Freundeskreis bestimmt. Erst nach 1969 wurde sie einem breiten Publikum zugänglich und zur Pflichtlektüre der 68er-Bewegung."
    ),

    // 31 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was versteht Hannah Arendt unter der 'Banalität des Bösen' in ihrer Analyse des Eichmann-Prozesses?",
        answerA = "Das Böse ist so alltäglich, dass es uns nicht mehr auffällt",
        answerB = "Alle Menschen sind von Natur aus böse",
        answerC = "Politische Gewalt ist immer banal und bedeutungslos",
        answerD = "Das Böse kann durch gedankenlose Pflichterfüllung entstehen, ohne dämonische Absicht",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Arendt beobachtet 1961 den Prozess gegen Adolf Eichmann und ist schockiert: Nicht ein Monster, sondern ein bürokratischer Beamter, der 'nur Befehle befolgte' und kaum nachdachte. Ihre These: Das schlimmste Böse kann aus gedankenlosem Gehorsam und dem Versagen kritischen Denkens entstehen.",
        funFact = "Arendts Begriff löste einen heftigen Streit aus. Viele jüdische Intellektuelle warfen ihr vor, Eichmann zu verharmlosen. Sie bestand darauf, dass ihr Begriff die Grausamkeit nicht mindert, sondern ihr eine neue, erschreckendere Dimension gibt."
    ),

    // 32 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Welches philosophische Konzept entwickelte Michel Foucault mit dem Begriff 'Diskurs'?",
        answerA = "Die formale Sprache der Wissenschaft im Gegensatz zur Alltagssprache",
        answerB = "Das System von Aussagen, Regeln und Praktiken, das bestimmt, was als wahr gilt und was sagbar ist",
        answerC = "Die rhetorische Kunst der öffentlichen Rede",
        answerD = "Den Dialog zweier Philosophen nach sokratischer Methode",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Foucault analysiert in 'Archäologie des Wissens' (1969): Ein Diskurs ist nicht bloß Sprache, sondern ein historisch bestimmtes System von Regeln, das festlegt, welche Aussagen möglich, wahr oder bedeutsam sind. Diskurse konstituieren Wissen, Wahrheit und damit Macht.",
        funFact = "Foucault war einer der ersten prominenten Intellektuellen, die an AIDS starben (1984). Seine letzten Werke über Selbsttechniken und die 'Sorge um sich' entstanden in Kenntnis seiner Krankheit."
    ),

    // 33 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was besagt der 'Utilitarismus' von Jeremy Bentham und John Stuart Mill?",
        answerA = "Die moralisch richtige Handlung ist diejenige, die das größte Glück der größten Zahl herbeiführt",
        answerB = "Nur nützliche Handlungen sind erlaubt; Verschwendung ist verboten",
        answerC = "Moralische Prinzipien sind unabhängig von ihren Konsequenzen bindend",
        answerD = "Nur rationale Handlungen sind moralisch erlaubt",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Bentham entwickelt das 'Prinzip der Nützlichkeit' (principle of utility): Richtig ist, was den größten Gesamtnutzen (happiness/pleasure minus pain) für alle Betroffenen produziert. Mill verfeinerte dies durch qualitative Unterscheidung der Lüste ('better to be Socrates dissatisfied than a fool satisfied').",
        funFact = "Benthams 'felicific calculus' versuchte, Lust und Schmerz mathematisch zu messen — nach Kriterien wie Intensität, Dauer, Gewissheit und Reichweite. Die Idee einer Moralrechnung gilt heute als überholt."
    ),

    // 34 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was unterscheidet Kants deontologische Ethik grundlegend vom Utilitarismus?",
        answerA = "Kant interessierte sich nur für religiöse Moral",
        answerB = "Kant dachte, Konsequenzen seien das einzig relevante Kriterium der Moral",
        answerC = "Für Kant bestimmt die Absicht und das Prinzip die Moral — unabhängig von den Konsequenzen",
        answerD = "Kant lehnte jede Form von Glück als moralischen Maßstab ab",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Kant: Eine Handlung ist moralisch gut, wenn sie aus Pflicht (Pflichtethik) und nach einem verallgemeinerbaren Prinzip (kategorischer Imperativ) erfolgt — unabhängig davon, ob sie gute Konsequenzen hat. Der gute Wille ist das einzige unbedingt Gute. Der Utilitarismus dagegen bewertet Handlungen nur nach ihren Folgen.",
        funFact = null
    ),

    // 35 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Welches Paradoxon formulierte Zenon von Elea, um zu zeigen, dass Bewegung logisch unmöglich sei?",
        answerA = "Das Paradoxon des Lügners",
        answerB = "Das Höhlengleichnis",
        answerC = "Das Schiff des Theseus",
        answerD = "Achilles und die Schildkröte",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Zenon: Achilles gibt der Schildkröte Vorsprung. Um sie einzuholen, muss er den halben Weg zurücklegen — aber dann wieder die Hälfte des Rests, und so fort ins Unendliche. Logisch scheint er die Schildkröte nie einzuholen, obwohl er es offensichtlich tut. Das Paradoxon provozierte Debatten über Unendlichkeit, Kontinuum und Zeit.",
        funFact = "Zenons Paradoxien wurden erst durch die mathematische Theorie unendlicher Reihen und den Infinitesimalkalkulus befriedigend aufgelöst. Die Summe 1/2 + 1/4 + 1/8 + ... konvergiert gegen 1 — eine endliche Strecke."
    ),

    // 36 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was verstand Spinoza unter der Formel 'Deus sive Natura' (Gott oder Natur)?",
        answerA = "Es gibt nur eine einzige Substanz — Gott und Natur sind dasselbe",
        answerB = "Gott und Natur sind zwei verschiedene Substanzen in Konkurrenz",
        answerC = "Die Natur beweist die Existenz Gottes",
        answerD = "Gott ist der Schöpfer der Natur und von ihr getrennt",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Spinozas Pantheismus: Es gibt genau eine unendliche Substanz, die gleichzeitig Gott und Natur ist. Was wir 'Gott' nennen, ist dieselbe Realität wie die Natur. Menschen, Tiere, Gedanken sind allesamt Modi (Modifikationen) dieser einen Substanz. Diese Position machte Spinoza für seine Zeit skandalös.",
        funFact = "Spinoza wurde 1656 mit einem der härtesten Bannflüche aus der jüdischen Gemeinde Amsterdams ausgestoßen. Er arbeitete als Linsenschleifer und lehnte einen Professorenstuhl in Heidelberg ab."
    ),

    // 37 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was ist Descartes' 'cogito ergo sum' ('Ich denke, also bin ich') als Grundlage seiner Philosophie?",
        answerA = "Ein logischer Syllogismus, der aus dem Denken das Sein ableitet",
        answerB = "Der einzige Satz, an dem auch radikaler Zweifel scheitert — Grundstein des Rationalismus",
        answerC = "Der Beweis, dass Gott existiert, weil er denkt",
        answerD = "Die Aussage, dass nur denkende Wesen moralische Rechte haben",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Descartes zweifelt in den 'Meditationen' (1641) an allem — Sinnen, Mathematik, sogar an der Existenz der Außenwelt (böser Dämon). Doch: Den Zweifel selbst kann er nicht bezweifeln, denn Zweifeln ist Denken, und Denken setzt ein denkendes Subjekt voraus. Cogito ergo sum ist das erste Fundament der Erkenntnis.",
        funFact = "Descartes soll seine besten Ideen im Bett gehabt haben. Als er 1649 nach Stockholm reiste und Königin Christina Unterricht um 5 Uhr morgens verlangte, starb er wenige Monate später — angeblich an Lungenentzündung durch die schwedische Kälte."
    ),

    // 38 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was versteht man unter 'Epikureismus' und worin unterscheidet er sich von der vulgären Vorstellung?",
        answerA = "Eine Lehre des hemmungslosen Genusses von Essen und Trinken",
        answerB = "Eine asketische Lehre der vollständigen Entsagung aller Lüste",
        answerC = "Eine Philosophie der ruhigen Freude durch Seelenfrieden, Freundschaft und Befreiung von Angst",
        answerD = "Eine politische Theorie des gerechten Staates",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Epikur lehrte: Das höchste Gut ist Ataraxia (Seelenfrieden) und Aponie (Schmerzfreiheit) — nicht maßlose Lust. Freundschaft ist wichtiger als Reichtum. Man soll Angst vor Tod und Göttern ablegen (nach dem Tod fühlt man nichts). Das bekannte 'Garten-Leben' der Epikureer war schlicht und philosophisch.",
        funFact = "Epikur lehrte im 'Garten' in Athen — einem der ersten philosophischen Orte, der auch Frauen und Sklaven offenstand, was für die Antike revolutionär war."
    ),

    // 39 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welchen Begriff führte Gilbert Ryle in 'The Concept of Mind' (1949) ein, um den cartesischen Dualismus zu kritisieren?",
        answerA = "The Ghost in the Machine",
        answerB = "Qualia",
        answerC = "The Frame Problem",
        answerD = "Multiple Realizability",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Ryle bezeichnet Descartes' Dualismus (Leib als Maschine, Geist als separater Geist darin) als 'the ghost in the machine'. Der Fehler ist ein Kategorienfehler: Geist ist keine separate Substanz, sondern eine Art zu handeln und Dispositionen zu haben — beschreibbar durch Verhaltensdispositionen.",
        funFact = null
    ),

    // 40 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist der 'Naturalistische Fehlschluss' (naturalistic fallacy) laut G.E. Moore in 'Principia Ethica' (1903)?",
        answerA = "Der Fehler, natürliche Phänomene mit übernatürlichen zu verwechseln",
        answerB = "Der Fehler, Ethik auf Biologie zu gründen",
        answerC = "Die Behauptung, dass Natur immer gut ist",
        answerD = "Der Fehler, 'gut' auf eine natürliche Eigenschaft zu reduzieren (z.B. angenehm oder evolutionär vorteilhaft)",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Moore argumentiert: 'Gut' ist eine einfache, unanalysierbare Eigenschaft — man kann sie nicht auf natürliche Eigenschaften reduzieren (z.B. 'gut = lustvoll' oder 'gut = evolutionär nützlich'). Der Versuch dieser Reduktion ist der naturalistische Fehlschluss. 'Gut' bleibt eine irreduzible normative Eigenschaft.",
        funFact = "Moores 'Principia Ethica' beeinflusste stark die Bloomsbury-Gruppe (Virginia Woolf, John Maynard Keynes). Keynes schrieb, das Buch habe einen 'enormen Einfluss' auf seine Generation gehabt."
    ),

    // 41 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was zeigt das 'Trolley-Brücken-Szenario' (fat-man variant) im Unterschied zum klassischen Trolley-Problem?",
        answerA = "Es zeigt, dass Utilitarismus immer richtig ist",
        answerB = "Persönliche Interaktion und Instrumentalisierung einer Person als Mittel machen moralisch relevante Unterschiede",
        answerC = "Es beweist, dass Deontologie das Trolley-Problem löst",
        answerD = "Es unterscheidet sich nicht relevant vom klassischen Szenario",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Im Brückenszenario wird eine Person aktiv als Mittel benutzt und körperlich angestoßen. Die meisten Menschen, die die Weiche umlegen, lehnen dies ab — obwohl die Kalkulation (1 gegen 5) identisch ist. Das zeigt: Moral hängt nicht nur vom Ergebnis ab, sondern auch davon, ob jemand direkt als Mittel instrumentalisiert wird.",
        funFact = "Studien zeigen, dass Menschen mit Läsionen des ventromedialen präfrontalen Kortex häufiger die utilitaristische Wahl treffen (den Mann stoßen). Dies deutet auf emotionale Komponenten moralischer Urteile hin."
    ),

    // 42 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der Begriff 'Tugendethik' (virtue ethics) und welcher antike Philosoph ist ihr Hauptvertreter?",
        answerA = "Eine Ethik, die ausschließlich auf Regeln basiert; Hauptvertreter: Kant",
        answerB = "Eine Ethik des größten Glücks; Hauptvertreter: Mill",
        answerC = "Eine Ethik der Charaktertugenden und des guten Lebens; Hauptvertreter: Aristoteles",
        answerD = "Eine Ethik des gesellschaftlichen Vertrags; Hauptvertreter: Rawls",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Aristoteles' Tugendethik fragt: Was ist ein gutes Leben (eudaimonia)? Antwort: Ein Leben, das die menschlichen Tugenden (arete) entfaltet — Mittelmaß zwischen Extremen. Tugenden werden durch Übung erworben (Habitus). Die Tugendethik erlebt seit MacIntyre ('After Virtue', 1981) eine Renaissance.",
        funFact = "Aristoteles definiert eine Tugend als die 'Mitte' zwischen zwei Lastern: Mut liegt zwischen Feigheit und Tollkühnheit; Großzügigkeit zwischen Geiz und Verschwendung — die 'mesotes-Lehre'."
    ),

    // 43 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was unterscheidet den erkenntnistheoretischen Skeptizismus von einem konsequenten Nihilismus?",
        answerA = "Skeptizismus zweifelt an Gewissheit der Erkenntnis; Nihilismus leugnet den Wert und Sinn",
        answerB = "Beide Positionen sind inhaltlich identisch",
        answerC = "Skeptizismus ist eine religiöse, Nihilismus eine atheistische Position",
        answerD = "Nihilismus zweifelt an Erkenntnis; Skeptizismus leugnet alle Werte",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Erkenntnistheoretischer Skeptizismus (Pyrrho, Hume) zweifelt an der Möglichkeit sicherer Erkenntnis, suspendiert das Urteil (epoché). Nihilismus (Nietzsche, existenzialistisches Verständnis) bedeutet Wert- und Sinnlosigkeit — nicht Erkenntniszweifel. Beide können aber kombiniert auftreten.",
        funFact = null
    ),

    // 44 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist das 'Schiff des Theseus'-Paradoxon und welches philosophische Problem illustriert es?",
        answerA = "Das Problem der Zeitreise und ihrer logischen Widersprüche",
        answerB = "Das Problem des freien Willens in einer deterministischen Welt",
        answerC = "Das Problem der Rechtfertigung politischer Autorität",
        answerD = "Das Problem der personalen Identität und Kontinuität: Was macht ein Ding über Zeit zum selben Ding?",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Das Schiff des Theseus wird Plank für Plank ersetzt. Nach vollständigem Austausch: Ist es noch dasselbe Schiff? Wenn jemand alle alten Planken sammelt und neu zusammensetzt — welches ist das 'echte' Schiff? Das Paradoxon illustriert das Problem der Identität über Zeit und Veränderung.",
        funFact = "Thomas Hobbes erweiterte das Paradoxon im 17. Jahrhundert: Was wenn ein Flickschuster alle alten Teile sammelt und wieder zusammensetzt? Dann gibt es zwei Schiffe des Theseus."
    ),

    // 45 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welches philosophische Konzept beschreibt John Locke mit der Metapher des 'tabula rasa'?",
        answerA = "Den Geist als unbeschriebene Tafel, der alle Ideen aus Erfahrung gewinnt",
        answerB = "Den leeren Staat nach einer Revolution",
        answerC = "Die moralische Reinheit des Neugeborenen",
        answerD = "Die ungeschriebene Verfassung eines Naturstaates",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Locke argumentiert in 'Essay Concerning Human Understanding' (1689) gegen angeborene Ideen: Der Geist ist bei Geburt eine leere Tafel — alle Ideen kommen aus Sinneserfahrung (simple ideas) und Reflexion darüber. Dies ist die empiristische Grundthese.",
        funFact = "Die Metapher 'tabula rasa' stammt ursprünglich aus Aristoteles' 'De Anima', wurde aber durch Locke berühmt. Heute ist die Debatte durch Chomskys universelle Grammatik und die Kognitionswissenschaft neu entfacht."
    ),

    // 46 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was ist das philosophische Problem der 'anderen Geister' (problem of other minds)?",
        answerA = "Wie wir wissen können, dass historische Philosophen klüger waren als wir",
        answerB = "Ob Geister im Sinne von Gespenstern existieren",
        answerC = "Wie wir wissen können, dass andere Menschen ein Bewusstsein und Innenleben haben",
        answerD = "Wie Kommunikation zwischen unterschiedlichen Kulturen möglich ist",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Das Problem: Mein eigenes Bewusstsein kenne ich durch direkte Erfahrung. Aber wie weiß ich, dass andere Menschen wirklich Bewusstsein haben und nicht wie philosophische Zombies agieren? Dies ist epistemologisch ein ungelöstes Problem und relevant für KI-Ethik.",
        funFact = "Der Begriff 'philosophischer Zombie' (p-zombie) wurde von David Chalmers geprägt: Ein Wesen, das physisch identisch mit einem Menschen ist, aber kein subjektives Erleben hat. Seine Vorstellbarkeit soll zeigen, dass Bewusstsein nicht auf Physisches reduzierbar ist."
    ),

    // 47 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was verstand Simone de Beauvoir mit dem Satz 'Man wird nicht als Frau geboren, man wird es' in 'Das andere Geschlecht' (1949)?",
        answerA = "Biologisches Geschlecht ist bei der Geburt unbestimmt",
        answerB = "Weiblichkeit ist keine natürliche Gegebenheit, sondern ein gesellschaftlich konstruiertes Rollenmuster",
        answerC = "Frauen müssen mehr lernen als Männer, um anerkannt zu werden",
        answerD = "Nur durch Mutterschaft wird eine Frau vollständig",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "De Beauvoir analysiert aus existentialistischer Sicht: 'Frau-Sein' ist keine biologische Essenz, sondern eine gesellschaftlich aufgezwungene Situation. Die Frau wird zum 'Anderen' gemacht — definiert in Relation zum Mann als Norm. Dies ist ein Gründungstext des modernen Feminismus.",
        funFact = "De Beauvoirs 'Das andere Geschlecht' wurde von der Vatikanischen Kirche auf den Index gesetzt. Sartre, ihr langjähriger Partner, soll das Buch nicht vollständig gelesen haben — de Beauvoir selbst berichtete dies mit Ironie."
    ),

    // 48 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was versteht Aristoteles unter 'Eudaimonia' und warum ist sie kein subjektives Glücksgefühl?",
        answerA = "Eudaimonia ist das subjektive Hochgefühl des Moments, das man maximieren soll",
        answerB = "Eudaimonia bezeichnet den Zustand nach dem Tod für tugendhafte Menschen",
        answerC = "Eudaimonia ist ein politisches Konzept für gerechte Gesellschaften",
        answerD = "Eudaimonia ist das objektive Gedeihen des Menschen durch tugendgemäßes Leben nach der menschlichen Natur",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Eudaimonia (oft unzulänglich als 'Glück' übersetzt) ist bei Aristoteles das Gelingen des Menschenlebens — nicht ein flüchtiges Gefühl, sondern eine Aktivität der Seele gemäß ihrer besten Tugend. Es geht um das aktive Entfalten der spezifisch menschlichen Fähigkeiten über ein ganzes Leben.",
        funFact = "Aristoteles schreibt: 'Eine Schwalbe macht noch keinen Frühling' — ebenso wenig macht ein einziger glücklicher Tag ein glückliches Leben. Eudaimonia braucht Zeit und vollständige Entfaltung."
    ),

    // 49 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was ist der wesentliche Unterschied zwischen Platons und Aristoteles' Verständnis der Ideen/Formen?",
        answerA = "Platon lässt Ideen in einer transzendenten Welt existieren; Aristoteles lokalisiert Formen in den Dingen selbst",
        answerB = "Platon glaubt an angeborene Ideen; Aristoteles glaubt nicht an Ideen",
        answerC = "Aristoteles glaubt an Ideen als Gedanken Gottes; Platon glaubt an materielle Ideen",
        answerD = "Beide Philosophen vertreten dieselbe Ideenlehre",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Platon: Ideen (das Gute, das Schöne, der Kreis) existieren in einer eigenen transzendenten Welt — Dinge der Sinneswelt sind nur deren fehlerhafte Abbilder. Aristoteles kritisiert dies: Formen existieren nicht getrennt, sondern inhärieren in den konkreten Dingen selbst (Immanenz der Formen, Hylomorphismus).",
        funFact = "Raffael malte in der 'Schule von Athen' (1509–11) Platon mit dem Finger nach oben (zur Ideenwelt) und Aristoteles mit der Hand horizontal (zur Erde) — ein ikonisches Bild des Kernunterschieds beider Philosophen."
    ),

    // 50 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was ist der philosophische Gehalt von Kants 'Ding an sich' (Noumenon) im Unterschied zur Erscheinung (Phänomen)?",
        answerA = "Das Ding an sich ist die materielle Substanz; Phänomene sind Gedanken darüber",
        answerB = "Phänomene sind die reale Welt; das Ding an sich ist eine Illusion",
        answerC = "Das Ding an sich ist die Realität wie sie unabhängig von uns existiert — prinzipiell unerkennbar; Phänomene sind Erscheinungen in unseren Anschauungsformen",
        answerD = "Das Ding an sich ist Gott; Phänomene sind die sündige Welt",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Kant trennt: Phänomene — Dinge, wie sie uns erscheinen, geformt durch unsere apriorischen Anschauungsformen (Raum, Zeit) und Verstandeskategorien. Noumena (Dinge an sich) — wie Dinge unabhängig von uns sind. Diese sind prinzipiell unerkennbar, weil jede Erkenntnis durch unsere kognitiven Formen geformt wird.",
        funFact = "Kants Unterscheidung wird oft missverstanden: Er behauptet nicht, Phänomene seien Illusionen — sie sind real, aber relativ auf menschliche Erkenntnisvermögen. Das Ding an sich ist schlicht außerhalb der Reichweite menschlicher Erkenntnis."
    )

)
