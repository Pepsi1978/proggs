package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsMaster4(): List<Question> = listOf(

    // ── Serialismus / Post-Serialismus (10) ──────────────────────────────────

    // Q1
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'totaler Serialismus' (Integral Serialism), wie er um 1950 von Messiaen, Boulez und Stockhausen entwickelt wurde?",
        answerA = "Die konsequente Erweiterung des Zwoeflton-Serismus auf alle kompositorischen Parameter: Tonhoehe, Dauer, Dynamik, Artikulation und Klangfarbe werden je eigenen Reihen unterworfen",
        answerB = "Eine serielle Methode, bei der ausschliesslich die Intervallstruktur, nicht aber Rhythmus oder Dynamik durch Reihen kontrolliert wird",
        answerC = "Die Integration von elektronischen Klaengen in die serielle Komposition ab dem WDR-Studio Koeln",
        answerD = "Ein Kompositionssystem, das alle Parameter durch eine einzige universelle Grundreihe ableitet",
        correctAnswer = 0,
        explanation = "Der totale oder integrale Serialismus dehnt das serielle Prinzip auf alle musikalischen Dimensionen aus. Messiaens 'Mode de valeurs et d'intensites' (1949) gilt als Vorlaefer. Boulezs 'Structures Ia' (1952) und Stockhausens 'Kreuzspiel' (1951) sowie 'Kontra-Punkte' (1953) sind Schluesseldokumente. Jeder Parameter — Hoehe, Dauer, Dynamik, Artikulation — folgt einer eigenen Reihenordnung.",
        difficulty = 5,
        funFact = "Messiaens 'Mode de valeurs et d'intensites' legte zwar die Grundlage, aber Messiaen selbst identifizierte sich nie mit dem nachfolgenden Serialismus seiner Schueler. Er bezeichnete sein Stueck spaeter als Experiment, nicht als Programm."
    ),

    // Q2
    Question(
        categoryId = 5,
        questionText = "Welchen Begriff praegte Pierre Boulez fuer die Kritik an Kompositionen, die serielle Methodik nur oberflaechlich anwenden, ohne die innere Logik wirklich zu durchdringen?",
        answerA = "Serieller Formalismus",
        answerB = "Serielle Scholastik",
        answerC = "Pseudo-Serialismus",
        answerD = "Mechanischer Serialismus",
        correctAnswer = 1,
        explanation = "Boulez praepgte den Begriff 'serielle Scholastik' fuer Kompositionen, die die aeussere Form des Serialismus imitieren, ohne dessen innere Logik und Konsequenz zu verwirklichen. In seinem Essay 'Alea' (1957) kritisierte er ausserdem die unstrukturierte Aleatorik Cages. Boulez forderte eine dialektische Synthese von Kontrolle und Freiheit.",
        difficulty = 5,
        funFact = "Boulezs intellektuelle Schaerfe machte ihn zur dominanten Stimme der Darmstaedter Ferienkurse. Seine Aufsaetze 'Schoenberg est mort' (1952) und 'Alea' (1957) setzten die Agenda fuer eine ganze Komponisten-Generation."
    ),

    // Q3
    Question(
        categoryId = 5,
        questionText = "Welches konkrete Verfahren wendet Boulez in 'Structures Ia' (1952) fuer zwei Klaviere an, um aus Messiaens Grundmaterial eine vollstaendig serialisierte Textur zu erzeugen?",
        answerA = "Er leitet aus Messiaens 12-Ton-Reihe eine Permutationstabelle ab, aus der er Tonhoehen-Reihen, Dauern-Reihen, Dynamik-Reihen und Anschlagsreihen systematisch kombiniert",
        answerB = "Er kombiniert zwei voneinander unabhaengige Reihen fuer jedes Klavier und laesst beide gleichzeitig ablaufen",
        answerC = "Er nutzt aleatorische Entscheidungen fuer die Zuweisung von Dauern und Dynamiken, waehrend Tonhoehen streng seriell gefuehrt werden",
        answerD = "Er uebernimmt Messiaens vollstaendiges Modalsystem und transponiert es systematisch durch alle 12 Tonarten",
        correctAnswer = 0,
        explanation = "Boulez leitete aus dem Tonhoehenset von Messiaens 'Mode de valeurs' eine quadratische 12x12-Permutationsmatrix ab. Aus dieser Matrix extrahierte er Dauernreihen (1-12 Demisemiquaver), Dynamikreihen (12 Stufen von pppp bis ffff) und Anschlagsreihen (12 Artikulationen). Alle vier Parameter sind vollstaendig serialisiert und werden nach klaren Regeln kombiniert.",
        difficulty = 5,
        funFact = "Boulez selbst aeu sserte spaeter Selbstkritik an 'Structures Ia': Er bezeichnete es als ein Werk, in dem er dem Material die Kompositionsarbeit ueberlassen habe — eine radikale Selbstkritik an einem seiner bedeutendsten fruehen Werke."
    ),

    // Q4
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Post-Serialismus' im musikhistorischen Kontext der 1960er bis 1980er Jahre?",
        answerA = "Kompositionen, die serielle Strukturen als Ausgangsmaterial verwenden, aber bewusst tonale, spektrale oder expressive Elemente reintegrieren, ohne zum neotonalen Traditionalismus zurueckzukehren",
        answerB = "Eine vollstaendige Abkehr von allen seriellen Techniken zugunsten der freien Improvisation",
        answerC = "Die Synthese von seriellem Denken mit der Volksmusiktradition des jeweiligen Heimatlandes des Komponisten",
        answerD = "Ausschliesslich Werke, die nach dem Tod Weberns (1945) im Webern-Stil komponiert wurden",
        correctAnswer = 0,
        explanation = "Post-Serialismus bezeichnet eine Phase, in der Komponisten wie Ligeti, Berio, Nono, Ferneyhough und spaeter Lachenmann serielle Techniken kritisch reflektieren und mit Spektralismus, Klangtexturen, Mikropolyphonie oder expressiven Gesten verbinden. Kein Rueckfall in Tonalitaet, aber auch keine dogmatische Anwendung des Darmstaedter Serialismus.",
        difficulty = 5,
        funFact = "Gyoergy Ligeti, der anfangs mit seriellem Denken experimentierte, distanzierte sich bewusst davon: 'Ich wollte weder Avantgardist noch Traditionalist sein.' Seine Etuden fuer Klavier vereinen mathematische Strukturen, Jazz-Rhythmik und karibische Polyrhythmik."
    ),

    // Q5
    Question(
        categoryId = 5,
        questionText = "Was ist die musikalische Bedeutung von Weberns 'Konzentrationsprinzip' und wie beeinflusste es den Nachkriegs-Serialismus der Darmstaedter Schule?",
        answerA = "Weberns Technik, alle melodischen, harmonischen und rhythmischen Strukturen eines Werkes aus kleinsten Intervallkeimen (Motive) zu entwickeln, wurde von Darmstadt als Modell maximaler kompositorischer Oekonomie verstanden",
        answerB = "Das Prinzip, Orchestermassen auf ein Minimum zu reduzieren und nur Kammerbesetzungen zu verwenden",
        answerC = "Weberns Methode, alle Stimmen auf den Ambitus einer Oktave zu beschraenken",
        answerD = "Die Reduktion aller Parameter auf eine einzige Grundreihe ohne Derivate oder Transformationen",
        correctAnswer = 0,
        explanation = "Weberns Konzentrationsprinzip — alle Elemente aus einem kleinstmoeglichen Kern zu entwickeln — wurde nach 1945 in Darmstadt zum Leitmodell. Boulez, Stockhausen, Nono und Maderna sahen Webern (nicht Schoenberg) als den entscheidenden Vorlaefer, weil bei ihm die Reihe wirklich strukturbestimmend ist. Sein 'Konzert' op.24 mit seiner Derivat-Reihe gilt als kanonisches Beispiel.",
        difficulty = 5,
        funFact = "Die Darmstaedter 'Webern-Rezeption' war historisch sehr selektiv: Man las Webern durch die Brille des Serialismus, was spaetere Musikologen als Fehldeutung kritisierten. Webern selbst haette den totalen Serialismus seiner Schueler wohl nicht akzeptiert."
    ),

    // Q6
    Question(
        categoryId = 5,
        questionText = "Welches Konzept entwickelte Helmut Lachenmann als kritische Reaktion auf den Serialismus und bezeichnete er als 'musique concrete instrumentale'?",
        answerA = "Eine Kompositionsweise, bei der konventionelle Klangerzeugung durch bewusst freigelassene Energieprozesse ersetzt wird: Reibung, Luftdurchfluss, Tastenmechanik werden als primaere Klangquellen eingesetzt",
        answerB = "Die Integration von Tonband-Aufnahmen konkreter Geraeusche in die Kammermusik",
        answerC = "Eine serielle Methode, bei der Instrumentaltexturen aus dem Frequenzspektrum konkreter Alltagsgeraeusche abgeleitet werden",
        answerD = "Die Komposition ausschliesslich fuer perkussive Klangerzeugung ohne Tonhoehendifferenzierung",
        correctAnswer = 0,
        explanation = "Lachenmanns 'musique concrete instrumentale' verwendet Instrumente als Energiequellen, deren Klang durch unkonventionelle Spieltechniken entsteht: Col legno, Reibung des Bogens am Korpus, Flageoletts, Atemgeraeusche. Das Ergebnis sind klangliche 'Situationen' statt traditioneller Toene. 'Air' (1968/69) und 'Gran Torso' (1971) sind zentrale Werke.",
        difficulty = 5,
        funFact = "Lachenmanns Konzept hat direkte politische Konnotationen: Er verstand sein Komponieren als Kritik an der 'Musikkonsumgesellschaft'. Die ungewohnten Klaenge sollten das Hoeren selbst hinterfragen — Musik als Erkenntnisprozess statt als Unterhaltung."
    ),

    // Q7
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'komplexer Musik' (New Complexity) im Kontext von Brian Ferneyhough und Richard Barrett?",
        answerA = "Eine kompositorische Richtung seit den 1970ern, die serielle Prazision mit extremer rhythmischer Komplexitaet, dichten Spieltechniken und vieldeutiger Notation verbindet, die an die Grenzen des Ausfuehrbaren stosst",
        answerB = "Elektronische Musik, die durch die Komplexitaet ihrer digitalen Signalverarbeitung definiert wird",
        answerC = "Orchesterkompositionen mit mehr als 100 simultanen Stimmfuehrungen",
        answerD = "Eine Schule, die Komplexitaet durch die Anzahl der verwendeten Instrumente definiert — ab 50 Instrumentalisten",
        correctAnswer = 0,
        explanation = "New Complexity (Begriff von Richard Toop, 1988) bezeichnet Werke, die serielle Prazision mit extremer rhythmischer Irrationaltaet (Nested Tuplets), polytemporalen Schichten, mikrotonalen Nuancen und mehrdeutiger Notation verbinden. Ferneyhoughs 'Cassandra's Dream Song' (1970) und die 'Etudes Transcendantales' sind Prototypen. Die Ausfuehrenden muessen oft Jahre ueben.",
        difficulty = 5,
        funFact = "Ferneyhough bezeichnete seine Notation bewusst als 'Rahmen fuer das Scheitern': Die extreme Schwierigkeit erzeugt beim Ausfuehren unvermeidliche Abweichungen, die er als organischen Teil des Klangergebnisses akzeptiert — eine konstruktive Einbeziehung des Unmoeglicherreichbaren."
    ),

    // Q8
    Question(
        categoryId = 5,
        questionText = "Welches strukturelle Prinzip wendet Karlheinz Stockhausen in 'Gruppen' (1955-57) fuer drei Orchester an?",
        answerA = "Serielle Kontrolle des Tempos durch die Kopplung von Taktart, Pulsdauer und Intervallstruktur — das Tempo selbst wird zur seriellen Parameter-Dimension",
        answerB = "Vollstaendige Unabhaengigkeit der drei Orchester ohne jede Synchronisation",
        answerC = "Antiphonie nach Gabrieli-Vorbild: Raumklang durch Wechselspiel der drei Orchestergruppen",
        answerD = "Integration von Elektronik in alle drei Orchestergruppen gleichzeitig",
        correctAnswer = 0,
        explanation = "In 'Gruppen' serialisierte Stockhausen erstmals das Tempo: Er leitete aus der Obertronreihe des Klangs Tempobeziehungen ab (Pulsdauerproportionen). Die drei Orchester umgeben das Publikum raeumlich und spielen teils synchron, teils polytemporalen Schichten. Das Stueck ist ein Schluesseldokument der raum- und tempowirtschaftenden Dimension des Serialismus.",
        difficulty = 5,
        funFact = "Die Uraufgabe 1958 in Koeln dirigierten Stockhausen, Boulez und Bruno Maderna gleichzeitig — jeder ein Orchester. Es war eine logistische Herausforderung, die drei Dirigenten praezise zu koordinieren, da ihre Tempi voneinander abwichen."
    ),

    // Q9
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet Luigi Nono mit dem Begriff 'Musiktheater des Bewusstseins' und welche Werke repraesentieren dieses Konzept?",
        answerA = "Musiktheater, das politische Bewusstseinsprozesse durch die Verbindung serieller Musik mit dokumentarischen Texten, Sprechtexten und raeumlicher Disposition des Publikums erfahrbar macht — 'Intolleranza 1960' und 'Al gran sole carico d'amore' sind Kernwerke",
        answerB = "Eine Form des inneren Monologs, bei dem Saenger ihre Gedanken ohne Text durch Vokalisierung ausdruecken",
        answerC = "Musiktheater, das ausschliesslich fuer ein geschlossenes Fachpublikum konzipiert ist und kompositorisches Bewusstsein voraussetzt",
        answerD = "Opern, in denen das Buehnengeschehen durch Leinwandprojektion des Bewusstseinszustands der Figuren erlaeutert wird",
        correctAnswer = 0,
        explanation = "Nono entwickelte ein politisch engagiertes Musiktheater, das serielle Komposition, dokumentarische Texte (Briefe von Widerstandskaempfern, politische Manifeste) und unkonventionelle Raumkonzepte verbindet. 'Intolleranza 1960' (1960/61) und 'Al gran sole carico d'amore' (1972-74) sind die Hauptwerke — politische Aktionen, nicht bloss Opern.",
        difficulty = 5,
        funFact = "Die Uraufgabe von 'Intolleranza 1960' in Venedig wurde von Neofaschisten gestoert: Sie warfen Stinkbomben in den Saal. Das Werk hatte explizit antifaschistische Texte, und die politische Reaktion bewies Nono die gesellschaftliche Relevanz seiner Arbeit."
    ),

    // Q10
    Question(
        categoryId = 5,
        questionText = "Welchen Beitrag leistete Milton Babbitt zur Entwicklung des amerikanischen Serialismus, und wie unterscheidet er sich konzeptuell vom europaeischen Darmstaedter Ansatz?",
        answerA = "Babbitt entwickelte eine mathmatisch-analytische Theorie des Serialismus basierend auf Gruppentheorie und Set Theory — er formalisierte den Serialismus als logisches System, waehrend Darmstadt ihn als kompositorische Praxis lebte",
        answerB = "Babbitt brachte den Jazz-Einfluss in den Serialismus ein und praegte den Begriff 'Jazz Serial Music'",
        answerC = "Er entwickelte den Serialismus in einer volksmusikalischen Richtung und integrierte nordamerikanische Folkmelodik",
        answerD = "Babbitt lehnte den Serialismus ab und entwickelte stattdessen die 'Total Chromatic Music' als Alternative",
        correctAnswer = 0,
        explanation = "Milton Babbitt (1916-2011) formalisierte den amerikanischen Serialismus in Aufsaetzen wie 'Some Aspects of Twelve-Tone Composition' (1955) und 'Twelve-Tone Rhythmic Structure' (1962) mit Mitteln der Gruppentheorie und set-theoretischen Analyse. Sein Ansatz ist akademisch-analytisch gepraegt, waehrend Darmstadt staerker von kompositorischer Praxis und politischem Kontext bestimmt war.",
        difficulty = 5,
        funFact = "Babbitts beruehmt-beruechtigter Aufsatz 'Who Cares If You Listen?' (1958, Originaltitel: 'The Composer as Specialist') provozierte einen Skandal: Er argumentierte, dass komplexe Musik ein Fachpublikum verdiene, so wie Fachphysik nur von Physikern verstanden werde. Der Titel stammte nicht von Babbitt selbst, sondern wurde redaktionell eingefuegt."
    ),

    // ── Byzantinische / Orthodoxe Musiktheorie (8) ───────────────────────────

    // Q11
    Question(
        categoryId = 5,
        questionText = "Was sind die 'Octoechos' in der byzantinischen Musiktheorie und wie unterscheiden sie sich von den westlichen Kirchentoenen?",
        answerA = "Das byzantinische System von acht Kirchentoenen (Echoi), das Tonarten nach Finalis, Ambitus und charakteristischen melodischen Formeln (Echoi-Melodien) definiert — strukturell ahnlich, aber mit anderen Intervallverhaeltnissen und Ornamentierungspraktiken als die westlichen Modi",
        answerB = "Acht gleichstufig gestimmte Skalen, die in der byzantinischen Kirche fuer Psalmengesang verwendet wurden",
        answerC = "Ein byzantinisches System von acht Modes, das identisch mit dem westlichen Gregorianik-Modalsystem ist",
        answerD = "Acht Tonleitern, die ausschliesslich Vierteltoene verwenden und in keiner westlichen Tradition vorkommen",
        correctAnswer = 0,
        explanation = "Die byzantinischen Octoechos (acht Toene) sind in vier authentische (Kyrioi) und vier Plagal-Toene (Plagales) aufgeteilt. Jeder Echos hat eine charakteristische Finalis, einen typischen Ambitus und melodische Formeln (Theseis), die Saenger auswendig kennen. Im Gegensatz zu westlichen Modi verwenden die Echoi nicht-gleichstufige Intervalle und haben reiche Ornamentierungstraditionen (Floiotes).",
        difficulty = 5,
        funFact = "Das Oktoechos-System wurde laut Tradition dem Heiligen Johannes von Damaskus (ca. 676-749) zugeschrieben, der die byzantinische Liturgiereform praepgte. Tatsaechlich hat das System aeltere Wurzeln in syrischer und judaeischer Liturgie."
    ),

    // Q12
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet das Neumen-System der byzantinischen Musiknotation, und wie wurde es im 13.-14. Jahrhundert durch die 'Mittelbyzantinische Notation' reformiert?",
        answerA = "Byzantinische Neumen sind keine Hoehen-Neumen (wie im Westen), sondern 'relative Intervall-Zeichen' — jedes Zeichen zeigt die Intervallbewegung vom vorherigen Ton an. Die Mittelbyzantinische Reform standardisierte die Zeichenformen und unterschied klar zwischen Bewegungszeichen (Somata) und Zierelementen (Pneumata)",
        answerB = "Das byzantinische Neuemensystem ist identisch mit dem westgregoriagischen und wurde durch arabischen Einfluss reformiert",
        answerC = "Byzantinische Neumen notieren absolute Tonhoehen und wurden im 13. Jahrhundert auf lineare Notenlinien ubertragen",
        answerD = "Die mittelbyzantinische Reform fuehrte ein Zwoeflton-System ein, das die aelteren Viertelton-Neumen abloeeste",
        correctAnswer = 0,
        explanation = "Byzantinische Neumen zeigen Intervallbewegungen (nicht absolute Hoehen): Das Zeichen 'Oligon' bedeutet Sekundschritt aufwaerts, 'Apostrophos' Sekundschritt abwaerts usw. Die Mittelbyzantinische Notation (ca. 1177-1821) standardisierte diese Zeichen in eine klare Morphologie aus Somata (Koerper/Bewegung) und Pneumata (Geist/Verzierungen). Heute wird die 'Neue Methode' von Chrysanthos (1814) gelehrt.",
        difficulty = 5,
        funFact = "Das byzantinische Notationssystem ist eines der wenigen, das nicht absolut, sondern relational notiert. Ein Saenger muss also immer den Startton kennen ('Ison') und kann dann die Melodie durch die Intervallzeichen rekonstruieren."
    ),

    // Q13
    Question(
        categoryId = 5,
        questionText = "Was ist der liturgische und musikalische Unterschied zwischen dem 'Ison' und dem 'Ison-Bordun' in der orthodoxen Kirchenmusik?",
        answerA = "Der Ison ist ein gehaltener Grundton (Bordun), der von einer speziellen Saengergruppe (Isonisten) waehrend des Solistengesangs gehalten wird — er dient als harmonisches Fundament und ist ein charakteristisches Merkmal der griechisch-orthodoxen Praxis",
        answerB = "Das Ison ist eine melodische Wendung am Ende jedes Psalmverses, aehnlich dem westlichen Repons",
        answerC = "Ison bezeichnet die vollstaendige Unisono-Intonation des Chores ohne melodische Differenzierung",
        answerD = "Das Ison ist eine Notationsvorschrift fuer gleich lange Toene, unabhaengig von ihrer Funktion",
        correctAnswer = 0,
        explanation = "In der griechisch-orthodoxen Praxis haelt der Chor-Ison einen langen Grundton (oft die Finalis oder Dominante des Echos), waehrend der Vorsaenger (Psaltes) die Melodie singt. Diese bordunhafte Praxis entstand aus aelteren byzantinischen Traditionen und verleiht dem Klang den charakteristischen 'schwebenden' Charakter. Der Isonist muss Atemtechnik beherrschen, um Toene ueber mehrere Minuten zu halten.",
        difficulty = 5,
        funFact = "Die Ison-Praxis ist nicht schriftlich fixiert — wann und wie lange der Ison gehalten wird, entscheidet der Dirigent (Protopsaltes) spontan. Diese improvisierte Bordunbegleitung macht jede Aufnahme byzantinischer Musik einzigartig."
    ),

    // Q14
    Question(
        categoryId = 5,
        questionText = "Welche Rolle spielte Simon Karas (1905-1999) fuer die wissenschaftliche Erforschung der byzantinischen Musik?",
        answerA = "Karas restaurierte und kodifizierte die authentische byzantinische Gesangstradition durch intensive Feldforschung in griechischen Kloestern und Kirchen, und gruendete 1931 die Gesellschaft zur Verbreitung byzantinischer Musik in Athen",
        answerB = "Karas erfand ein neues Notationssystem zur Transkription byzantinischer Musik in westliche Notenschrift",
        answerC = "Er war der erste, der byzantinische Musik elektronisch aufzeichnete und im Rundfunk sendete",
        answerD = "Karas entwickelte das moderne Ausbildungssystem fuer orthodoxe Kirchenmusiker in Konstantinopel",
        correctAnswer = 0,
        explanation = "Simon Karas war einer der bedeutendsten Forscher und Praktizierer byzantinischer Musik im 20. Jahrhundert. Er lehrte, sang und dokumentierte die lebendige Tradition durch Feldaufnahmen und theoretische Abhandlungen. Sein mehrbaendiges Werk 'Methodos tes Ellenikes Mousikes' gilt als Standardreferenz. Er bestand auf der Unveraenderlichkeit der authentischen Praxis gegen westliche Harmonisierungsversuche.",
        difficulty = 5,
        funFact = "Karas konnte Tonintervalle der byzantinischen Musik auf Bruchteile eines Halbtons genau singen und lehrte — eine Faehigkeit, die heute selten ist. Er lehnte jede 'Modernisierung' der byzantinischen Musik kategorisch ab."
    ),

    // Q15
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung hat das Konzept der 'Theseis' in der byzantinischen Kompositionspraxis?",
        answerA = "Theseis sind melodische Formeln oder Floskeln, die fuer jeden Echos (Kirchenton) charakteristisch sind und als Bausteine fuer neue Kompositionen dienen — vergleichbar mit dem westlichen 'Centonisation'-Prinzip",
        answerB = "Theseis bezeichnet die Stimmlagen-Zuweisung fuer byzantinische Chorsaenger",
        answerC = "Es handelt sich um rhythmische Akzentsetzungen, die aus der antiken griechischen Metrik uebernommen wurden",
        answerD = "Theseis sind harmonische Kadenzen am Ende byzantinischer Psalmverse",
        correctAnswer = 0,
        explanation = "Byzantinische Komponisten (Maistores) konstruieren neue Gesaenge nicht frei, sondern aus einem Repertoire modusspezifischer melodischer Wendungen (Theseis). Dieses Centonisationsprinzip — das Zusammenstellen aus vorgegebenen Formeln — verbindet byzantinische Musik mit arabischen Maqam-Traditionen und dem westgregoriakischen Centonisation-Verfahren.",
        difficulty = 5,
        funFact = "Das Centonisation-Prinzip bedeutet, dass byzantinische Saenger Tausende von Formeln auswendig kennen. Die Faehigkeit, diese Formeln spontan zu kombinieren und an Texte anzupassen, ist das Kernkoennen eines ausgebildeten byzantinischen Musikers."
    ),

    // Q16
    Question(
        categoryId = 5,
        questionText = "Wie beeinflusste die Kirchenmusikarbeit des Patriarchen von Konstantinopel die Entwicklung der russisch-orthodoxen Musikkultur?",
        answerA = "Byzantinische Melodien und Notationssysteme wurden ab dem 10. Jahrhundert mit der Christianisierung der Rus nach Russland uebertragen und bildeten die Grundlage des altrussischen Sbornik-Gesangs (Znamenny Raspev), der bis heute in traditionellen Kloestern gepflegt wird",
        answerB = "Russische Kirchenmusik entwickelte sich vollstaendig unabhaengig von Byzanz aus slawischen Volksmelodien",
        answerC = "Patriarchale Gesaenge aus Konstantinopel wurden erst im 17. Jahrhundert unter Peter dem Grossen eingefuehrt",
        answerD = "Russische Kirchenmusik ist staerker von roemisch-katholischen Choraltraditionen beeinflusst als von byzantinischen",
        correctAnswer = 0,
        explanation = "Mit der Christianisierung der Kiewer Rus (988) kamen byzantinische Gesangsformen und Notation. Der Znamenny Raspev (von 'Znamja' = Zeichen/Neumen) ist die aelteste russische Kirchengesangstradition, die auf byzantinischen Grundlagen beruht. Im 17. Jahrhundert wurde er durch den Kievsky Raspev und spaeter durch polyphonen europaeischen Einfluss ergaenzt.",
        difficulty = 5,
        funFact = "Der Znamenny Raspev wird bis heute in altglaeubigen (Raskolniki) Gemeinschaften unveraendert gepflegt, waehrend die Russisch-Orthodoxe Hauptkirche bereits im 17. Jahrhundert westeuropaeische Polyphonie adaptierte — ein grosser interner Streit der russischen Kirchenmusik."
    ),

    // Q17
    Question(
        categoryId = 5,
        questionText = "Was unterscheidet die Notation des 'Exegesis' in der byzantinischen Musikpraxis von der regulaeren Notation?",
        answerA = "Exegesis bezeichnet eine ausfuehrliche Ausschreibung (Entfaltung) einer kurz notierten melodischen Formel — ein Meister schreibt die impliziten Verzierungen und Ornamente explizit aus, die ein weniger erfahrener Saenger sonst aus der Tradition kennen muesste",
        answerB = "Exegesis ist die Uebersetzung byzantinischer Neumen in westliche Notenschrift",
        answerC = "Es bezeichnet eine Methode zur Vereinfachung komplizierter byzantinischer Melodien fuer den Gemeindegesang",
        answerD = "Exegesis beschreibt den Kommentar zu einem liturgischen Text, der neben der Notation steht",
        correctAnswer = 0,
        explanation = "In der byzantinischen Tradition werden komplexe Gesaenge oft in 'kurzer' Notation ueberliefert, die implizites Wissen erfordert. Die Exegesis (Auslegung/Entfaltung) ist die explizite Ausschreibung aller Ornamente und Nuancen. Bekannte Meister wie Gregorios Protopsaltes (18./19. Jh.) erstellten solche Exegesen aelterer Werke und machten sie dadurch lehrbar.",
        difficulty = 5,
        funFact = "Die Kunst der Exegesis ist ein Zeichen hoechsten musikalischen Wissens: Nur ein Meister, der die vollstaendige Tradition kennt, kann eine komprimierte Notation korrekt entfalten. Es ist die musikalische Entsprechung einer Kommentartradition in der Theologie."
    ),

    // Q18
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung haben die 'Glas' (Glasovi) im serbisch-orthodoxen Gesangssystem?",
        answerA = "Glasovi sind die acht Kirchentoene (Echoi) des serbisch-orthodoxen Gesangssystems, die zwar auf byzantinischen Grundlagen basieren, aber durch slaawische Melodietraditionen und spaeter durch westliche Harmonisierungsversuche (19.-20. Jh.) eigenstaendig gepraeegt wurden",
        answerB = "Glasovi bezeichnen die Stimmregister der serbischen Kirchensaenger: Tief, Mittel und Hoch",
        answerC = "Es handelt sich um mehrstimmige Chorwerke, die im 19. Jahrhundert von serbischen Nationalkomponisten geschaffen wurden",
        answerD = "Glasovi sind instrumentale Vorspielfiguren vor dem Gesang in der serbisch-orthodoxen Liturgie",
        correctAnswer = 0,
        explanation = "Das serbisch-orthodoxe System der 'Osmoglasnik' (acht Toene = Glasovi) ist das slawische Pendant zu den byzantinischen Octoechos. Durch die ottomanische Herrschaft, slaawische Volksmelodik und spaeter die Reformen von Stevan Stojanovic Mokranjac (19./20. Jh.) entwickelte sich ein eigenstaendiges serbisches Kirchengesangssystem mit eigener Melodiestruktur.",
        difficulty = 5,
        funFact = "Mokranjac (1856-1914), bekannt fuer seine 'Garlands' (Rukoveti), komponierte auch eine systematische Sammlung serbischer kirchlicher Gesaenge, die bis heute als Standardlehrwerk fuer serbisch-orthodoxe Kirchenmusiker dient."
    ),

    // ── Akustische Oekologie / Soundscape (8) ────────────────────────────────

    // Q19
    Question(
        categoryId = 5,
        questionText = "Wer praegte den Begriff 'Soundscape' und begrundete die 'World Soundscape Project' an der Simon Fraser University in Vancouver?",
        answerA = "R. Murray Schafer, der in seinem Buch 'The Tuning of the World' (1977) die Grundlagen der akustischen Oekologie legte",
        answerB = "Barry Truax, der als erster die akustische Oekologie als wissenschaftliches Fachgebiet an einer Universitaet etablierte",
        answerC = "Hildegard Westerkamp, die das Soundscape-Konzept durch ihre Hoerspaziergaenge (Soundwalks) popularisierte",
        answerD = "John Cage, der in '4:33' erstmals die akustische Umgebung als musikalisches Material thematisierte",
        correctAnswer = 0,
        explanation = "R. Murray Schafer (1933-2021) praegte den Begriff 'Soundscape' und gruendete das World Soundscape Project (WSP) an der Simon Fraser University in den fruehen 1970ern. Sein Buch 'The Tuning of the World' (1977, dt. 'Klang und Krach') ist das Grundlagenwerk der akustischen Oekologie. Schafer unterschied zwischen 'Hi-Fi' (klar, wenig Laerm) und 'Lo-Fi' (ueberlagert, urban) Soundscapes.",
        difficulty = 5,
        funFact = "Schafer hatte keine klassische Musikwissenschafts-Ausbildung — er war Komponist und Musikpaedagoge. Seine Ideen entstanden aus einer tiefen Besorgnis ueber die Klangverschmutzung (Noise Pollution) der modernen Welt, die er als oekologisches und aesthetisches Problem verstand."
    ),

    // Q20
    Question(
        categoryId = 5,
        questionText = "Was unterscheidet 'Keynote Sounds', 'Soundmarks' und 'Sound Signals' in Schafers Soundscape-Terminologie?",
        answerA = "Keynote Sounds sind permanente Hintergrundsounds einer Gemeinschaft (z.B. Wind, Wasser); Sound Signals sind bewusst wahrgenommene Vordergrundsounds (z.B. Glocken); Soundmarks sind gemeinschaftsspezifische Sounds mit besonderer kultureller Bedeutung (Pendant zum Landmark)",
        answerB = "Keynote Sounds sind musikalische Grundtoene; Sound Signals sind Warnsignale; Soundmarks sind Geraeusche, die auf Karte verzeichnet werden",
        answerC = "Alle drei Begriffe bezeichnen dasselbe Phaenomen aus verschiedenen analytischen Perspektiven",
        answerD = "Keynote Sounds sind urban; Sound Signals sind industrial; Soundmarks sind natuerlich",
        correctAnswer = 0,
        explanation = "Schafers Terminologie in 'The Tuning of the World': Keynote Sounds (nach der musikalischen Tonika) sind habituelle Hintergrundgeraeusche einer Gemeinschaft — oft unbewusst wahrgenommen. Sound Signals sind Foreground-Sounds, die als akustische Zeichen bewusst registriert werden. Soundmarks (von 'landmark') sind sounds mit besonderer lokaler Identitaet — akustische Wahrzeichen einer Gemeinschaft.",
        difficulty = 5,
        funFact = "Schafer verwendete den Begriff 'Soundmark' in Analogie zu 'Landmark' (Wahrzeichen). Heute werden Soundmarks von der UNESCO als Teil des immateriellen Kulturerbes diskutiert — z.B. der Klang der Glocken von Notre-Dame oder der Muezzin-Ruf."
    ),

    // Q21
    Question(
        categoryId = 5,
        questionText = "Was versteht Barry Truax unter 'Acoustic Ecology' und welche Verbindung sieht er zwischen Soundscape-Design und Kommunikation?",
        answerA = "Acoustic Ecology untersucht die Beziehung zwischen Organismen und ihrer Klangumgebung — Truax betont, dass Soundscapes als Kommunikationssysteme funktionieren: Geraeusche transportieren Informationen ueber soziale, biologische und oekologische Verhaeltnisse",
        answerB = "Acoustic Ecology ist die Wissenschaft von Schallwellen in Oekosystemen, beschraenkt auf Tier- und Pflanzenwelt",
        answerC = "Es handelt sich um eine Methode zur akustischen Kartierung von Naturschutzgebieten",
        answerD = "Truax definiert Acoustic Ecology als die Kunst des Soundscapes-Designs fuer urbane Raeume",
        correctAnswer = 0,
        explanation = "Truax (geb. 1947), Mitarbeiter des World Soundscape Project und Nachfolger Schafers, entwickelte die akustische Oekologie zu einer Kommunikationswissenschaft weiter. In 'Acoustic Communication' (1984, 2001) zeigt er, wie Soundscapes als Zeichensysteme funktionieren, die soziale Verhaeltnisse, oekologische Bedingungen und kulturelle Identitaet kommunizieren. Er entwickelte auch die Praxis des 'Soundscape Composition'.",
        difficulty = 5,
        funFact = "Truax ist auch Pionier der Computermusik und entwickelte das POD-System — ein fruehes Echtzeit-Kompositionssystem — in den 1970ern an der Simon Fraser University."
    ),

    // Q22
    Question(
        categoryId = 5,
        questionText = "Welches Konzept entwickelte Bernie Krause (geb. 1938) mit dem Begriff 'Niche Hypothesis' zur Analyse natuerlicher Soundscapes?",
        answerA = "Die Hypothese, dass Tiere in einem Oekosystem akustische Nischen besetzen und ihre Lautaeusserungen so evolviert haben, dass sie verschiedene Frequenz- und Zeitbereiche nutzen — eine Art akustischer Ressourcenteilung",
        answerB = "Die Theorie, dass Tiere ihre Klangnischen durch territoriales Verhalten schutzen und Eindringlinge mit Lautstaerke vertreiben",
        answerC = "Die Hypothese, dass Umweltlaerm durch Schaffung akustischer Nischen reduziert werden kann",
        answerD = "Ein Marketingkonzept fuer die Positionierung von Soundscapes-Aufnahmen in Medienmaerkten",
        correctAnswer = 0,
        explanation = "Bernie Krause, Pionier der Biophonie, entwickelte die Niche Hypothesis: In gesunden Oekosystemen haben Tierarten durch Evolution akustische 'Nischen' in Frequenz und Zeit belegt, um gleichzeitig kommunizieren zu koennen, ohne sich gegenseitig zu storen. Er bezeichnete die tierischen Lautaeusserungen als 'Biophonie', menschliche Geraeusche als 'Anthrophonie' und natuerliche Umgebungsgeraeusche als 'Geophonie'.",
        difficulty = 5,
        funFact = "Krauses Langzeitaufnahmen zeigen den oekologischen Verfall: An Orten, die vor Jahren reich an Biophonie waren, ist heute oft Stille oder Anthrophonie dominant. Er dokumentierte so den Biodiversitaetsverlust mit Mikrofon und Spektrogramm."
    ),

    // Q23
    Question(
        categoryId = 5,
        questionText = "Was ist 'Soundscape Composition' als kuenstlerische Praxis und wie unterscheidet sie sich von konventioneller elektroakustischer Musik?",
        answerA = "Soundscape Composition verwendet Feldaufnahmen (Field Recordings) als primaeres Material und behaelt deren Umweltkontext und Referenzialitaet bei — im Gegensatz zur elektroakustischen Musik, die Klang haeufig abstrahiert und de-kontextualisiert",
        answerB = "Soundscape Composition ist nur ein anderer Begriff fuer Musique Concrete und unterscheidet sich nicht konzeptuell",
        answerC = "Sie verwendet ausschliesslich synthetisch erzeugte Klaenge zur Imitation natuerlicher Klangegebungen",
        answerD = "Soundscape Composition bezeichnet die Hintergrundmusik fuer Naturfilme und Dokumentationen",
        correctAnswer = 0,
        explanation = "Soundscape Composition (Begriff massgeblich von Hildegard Westerkamp und Truax gepraeagt) haelt die referentielle Verbindung zwischen Klang und Quelle aufrecht — man soll die urspruengliche Umgebung erkennen oder imaginieren koennen. Werke wie Westerkamps 'Kits Beach Soundwalk' (1989) kontextualisieren Feldaufnahmen kompositorisch, ohne ihre oekologische Identitaet zu zerstoeren.",
        difficulty = 5,
        funFact = "Hildegard Westerkamp praegte auch die Praxis des 'Soundwalk' — eine gefuehrte Wahrnehmungsuebung, bei der Teilnehmer eine Klangumgebung bewusst erfahren. Diese Praxis wird heute in der Stadtplanung, Therapie und Paedagogik eingesetzt."
    ),

    // Q24
    Question(
        categoryId = 5,
        questionText = "Welche politische Dimension hat das Konzept der 'Noise Pollution' in Schafers Soundscape-Theorie?",
        answerA = "Schafer sah Laermverschmutzung als Symptom einer industriekapitalistischen Gesellschaft, die den Klang des oeffentlichen Raums vereinnahmt — er forderte buergerliche Rechte am akustischen Erbe ('Acoustic Rights') und eine demokratische Klanggestaltung des Gemeinwesens",
        answerB = "Schafer beschraenkte Noise Pollution auf technisch messbare Dezibel-Grenzwerte ohne politische Implikationen",
        answerC = "Er sah Laermverschmutzung als rein aesthetisches Problem ohne gesellschaftspolitische Dimension",
        answerD = "Die politische Dimension beschraenkte sich auf den Kampf gegen Militaerlarm in der Naehe von Wohngebieten",
        correctAnswer = 0,
        explanation = "Schafer verstand Laermverschmutzung als gesellschaftspolitisches Problem: Industrie und Verkehr okkupieren den akustischen oeffentlichen Raum, waehrend Buerger kaum Mitspracherecht haben. Er praeparierte das Konzept der 'Acoustic Rights' — das Recht auf eine hoerbare, saubere Klangumgebung als Buergerrecht. Diese Idee beeinflusste spaetere EU-Laermschutzgesetzgebung.",
        difficulty = 5,
        funFact = "Schafer liess seine Schueler 'Soundwalks' durch Stadtzentren machen und protokollieren, welche Klangquellen sie hoerten. Dabei entdeckten sie, wie wenig bewusste Wahrnehmung wir dem akustischen Alltag schenken — ein paedagogischer Ansatz, der heute in 'ear cleaning'-Kursen weltweit gelehrt wird."
    ),

    // Q25
    Question(
        categoryId = 5,
        questionText = "Was ist das 'World Forum for Acoustic Ecology' (WFAE) und welche wissenschaftlichen Disziplinen verbindet es?",
        answerA = "Das 1993 gegruendete internationale Netzwerk verbindet Komponisten, Oekologieforscher, Stadtplaner, Soziologen und Paedagogen, die sich mit der Beziehung zwischen Mensch, Klang und Umwelt beschaeftigen — ein interdisziplinaerer Raum zwischen Kunst, Wissenschaft und Aktivismus",
        answerB = "Eine UNO-Organisation zur Messung und Regulierung globaler Laermbelastung",
        answerC = "Ein exklusiver Kreis elektroakustischer Komponisten, die Soundscapes als kuenstlerisches Genre definieren",
        answerD = "Eine wissenschaftliche Datenbank fuer Tiergesangs-Aufnahmen aus aller Welt",
        correctAnswer = 0,
        explanation = "Das WFAE wurde 1993 in Banff, Kanada gegruendet und hat heute Mitglieder in ueber 40 Laendern. Es verbindet Musik, Wissenschaft und Umweltaktivismus. Mitglieder sind Komponisten (Westerkamp, Truax), Biologen (Krause), Stadtplaner, Hoertherapeuten und Paedagogen. Die Zeitschrift 'Soundscape: The Journal of Acoustic Ecology' ist das akademische Publikationsorgan.",
        difficulty = 5,
        funFact = "Das WFAE organisiert 'Soundscape Tours' an Orten, die durch industriellen Laerm oder Biodiversitaetsverlust bedroht sind — eine Kombination aus wissenschaftlicher Dokumentation, kuenstlerischer Intervention und oekologischem Aktivismus."
    ),

    // Q26
    Question(
        categoryId = 5,
        questionText = "Welchen Beitrag leistete die Forschungsgruppe 'European Acoustic Heritage' zur Dokumentation urbaner Soundscapes?",
        answerA = "Die Gruppe entwickelte standardisierte Methoden zur akustischen Kartierung und Langzeitdokumentation europaeischer Staedte und Kulturplaetze, um den akustischen Wandel durch Industrialisierung und Digitalisierung wissenschaftlich zu erfassen",
        answerB = "Sie produzierte eine Schallplattenreihe historischer Stadtgeraeusche aus dem 19. Jahrhundert",
        answerC = "Die Gruppe beschraenkte ihre Forschung auf mittelalterliche Klosterumgebungen in Suedeuropa",
        answerD = "Sie entwickelte eine App fuer touristische Stadtfuehrungen basierend auf historischen Klangatmosphaeren",
        correctAnswer = 0,
        explanation = "Das EU-gefoerderte Projekt 'Positive Soundscapes' (EPSRC/ESRC) und verwandte Projekte wie 'Quiet Areas in Europe' entwickelten Messkategorien und Erlebensskalen fuer Soundscape-Qualitaet. ISO 12913 (Soundscape — Definition) kodifiziert die Grundlagen. Forschungsgruppen an Universitaeten in Salford, Sheffield und Stockholm waren federfuehrend.",
        difficulty = 5,
        funFact = "Die ISO 12913 Norm 'Soundscape' (2014-2018) war eine internationale Leistung: Erstmals wurde Soundscape nicht nur als akustische Messung, sondern als subjektives Wahrnehmungskonzept in einem internationalen Standard verankert."
    ),

    // ── Musikmanuskript-Palaeographie (8) ────────────────────────────────────

    // Q27
    Question(
        categoryId = 5,
        questionText = "Was untersucht die Musikpalaeographie und welche Methoden setzt sie zur Datierung von Musikhandschriften ein?",
        answerA = "Musikpalaeographie analysiert die Schriftformen musikalischer Notationen in historischen Handschriften — zur Datierung verwendet sie Schriftvergleich, Pergament-/Papieranalyse (Karbondatierung, Wasserzeichenkunde), Tintenzusammensetzung, Skriptorienzuweisung und Vergleich mit datierten Parallelhandschriften",
        answerB = "Sie beschraenkt sich auf die Entzifferung unleserlicher Notentexte durch Infrarotphotographie",
        answerC = "Musikpalaeographie ist ein anderer Begriff fuer die Quellenkritik in der historischen Musikwissenschaft",
        answerD = "Die Disziplin untersucht ausschliesslich griechische und byzantinische Notationsformen",
        correctAnswer = 0,
        explanation = "Musikpalaeographie ist eine Spezialdisziplin an der Schnittflaeche von Palaeographie, Kodikoligie und Musikwissenschaft. Sie analysiert Notationsformen (Neumentypen, Schluessel, Taktangaben), Beschreibstoffe (Pergament vs. Papier), Tinte (Gallusstaub-, Russ-, Pflanzentinte), Schreiberhands und Scriptorium-spezifische Konventionen. Zentrale Texte: Carl Parrish 'The Notation of Medieval Music', Willi Apel 'The Notation of Polyphonic Music'.",
        difficulty = 5,
        funFact = "Eines der spektakulaersten palaeographischen Detektivprojekte: die Identifizierung der Hand von Josquin des Prez in mehreren Handschriften durch den Vergleich spezifischer Notenschreibgewohnheiten — ein Jahrhundertprojekt der Musikwissenschaft."
    ),

    // Q28
    Question(
        categoryId = 5,
        questionText = "Was sind die Hauptmerkmale der 'Notre-Dame-Schule'-Notation des 12.-13. Jahrhunderts und welche Probleme stellen sich bei ihrer Interpretation?",
        answerA = "Die Notre-Dame-Notation (Modalnotation) verwendet Ligaturen zur Darstellung rhythmischer Modi — das Hauptinterpretationsproblem ist die Bestimmung des genauen rhythmischen Modus aus dem Kontext, da gleiche Ligaturen verschiedene Modi signalisieren koennen",
        answerB = "Die Notre-Dame-Notation ist vollstaendig mensural und eindeutig in Rhythmus und Tonhoehe — Interpretationsprobleme existieren nicht",
        answerC = "Sie verwendet Buchstabennotation, die keine rhythmischen Informationen enthaelt",
        answerD = "Notre-Dame-Notation ist identisch mit der spatmittelalterlichen schwarzen Mensuralnotation und bereitet keine Interpretationsschwierigkeiten",
        correctAnswer = 0,
        explanation = "Die Modalnotation der Notre-Dame-Schule (Leonin, Perotin, ca. 1160-1250) basiert auf Ligatur-Mustern, die den sechs rhythmischen Modi entsprechen. Problem: Ein Dreierligatur kann Modus I (lang-kurz) oder Modus II (kurz-lang) signalisieren, je nach Kontext. Die historische Debatte ueber korrekte Rhythmusinterpretation (Mensuralistisches vs. Akzenttheoretisches System) wurde erst im 20. Jahrhundert weitgehend gloest.",
        difficulty = 5,
        funFact = "Johann Wolf (1919) und besonders William Waite ('The Rhythm of Twelfth-Century Polyphony', 1954) losten das Ligatur-Raetsel der Modalnotation durch systematische Analyse aller ueberlieferten Handschriften. Es war eines der grossen Raetsel der mittelalterlichen Musikwissenschaft."
    ),

    // Q29
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung hat das 'Squarcialupi-Codex' (ca. 1410-1415) fuer die Erforschung der Musik des Trecento?",
        answerA = "Er ist die umfangreichste Quelle der ars nova italiana mit 352 Stuecken von 12 Komponisten — die aufwendige Illuminierung mit Komponistenportraets macht ihn zum kostbarsten Musikmanuskript der Florentiner Renaissance",
        answerB = "Es ist das erste gedruckte Musikbuch aus Florenz, das Werke von Landini und seinen Zeitgenossen enthaelt",
        answerC = "Der Codex ist primaer eine Theorie-Abhandlung der ars nova mit nur wenigen musikalischen Beispielen",
        answerD = "Er ist die einzige Quelle der einstimmigen Ballata-Tradition und wurde von Squarcialupi selbst komponiert",
        correctAnswer = 0,
        explanation = "Der Squarcialupi-Codex (Biblioteca Medicea Laurenziana, Florenz) ist nach Antonio Squarcialupi (1416-80) benannt, in dessen Besitz er war. Mit 352 Stuecken (Ballate, Madrigale, Cacce) von Komponisten wie Francesco Landini, Jacopo da Bologna und Giovanni da Cascia ist er die reichhaltigste Quelle der ital. Trecentomusik. Jeder Komponistenabschnitt wird durch ein Miniaturportraet eingeleitet.",
        difficulty = 5,
        funFact = "Squarcialupi (der Organist, nicht der Auftraggeber) ist als 'Florentiner Organist' in Medicii-Dokumenten bezeugt. Lorenzo de' Medici schrieb ihm einen Nachruf — ein aussergewohnliches Zeugnis des sozialen Status eines Musikers in der Renaissance."
    ),

    // Q30
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Ars subtilior' und welche palaeographischen Besonderheiten kennzeichnen ihre Notation?",
        answerA = "Die Ars subtilior (ca. 1370-1420) ist ein Stilzweig der spaeten franzosisch-italiaenischen Musik mit extremer rhythmischer Komplexitaet — palaeographisch zeichnet sie sich durch farbige Noten (rote, blaue, halbschwarze Notenkoepfe), komplizierte Prolationes und ungewoehnliche Mensurzeichen aus",
        answerB = "Ars subtilior ist ein anderer Name fuer die Ars Nova und bezeichnet dieselbe Kompositionspraxis",
        answerC = "Sie ist eine vereinfachte Form der mensuralen Notation fuer den Laienchor des 14. Jahrhunderts",
        answerD = "Ars subtilior bezeichnet mittelbyzantinische Neumen, die fuer den Westen adaptiert wurden",
        correctAnswer = 0,
        explanation = "Ars subtilior (Begriff von Ursula Gunther, 1963) bezeichnet die hochkomplexe Musik des spaeten 14. Jhs. (Hauptzentren: Avignon, Foix). Komponisten wie Solage, Trebor und Ciconia schrieben Stuecke mit Polytempik, farbig notierten Synkopen und ungebraeuchlichen Mensurverhaeltnissen. Die Chantilly-Handschrift (Ms. 564) ist die Hauptquelle.",
        difficulty = 5,
        funFact = "Ein Stueck der Ars subtilior, Solages' 'Fumeux fume', ist eine musikalische Allegorie des Rauchens: Der Text beschreibt einen Raucher, und die harmonische Sprache ist bewusst 'verrauchert' — truebt, beissend dissonant, ein fruehes Beispiel eines Text-Bild-Verhaeltnisses in der westlichen Musikgeschichte."
    ),

    // Q31
    Question(
        categoryId = 5,
        questionText = "Welche Methode der digitalen Musikpalaeographie revolutionierte die Quellenforschung im 21. Jahrhundert?",
        answerA = "Multispektrale Bildgebung (z.B. RTI — Reflectance Transformation Imaging) ermoeglicht die Sichtbarmachung verblichener oder gecraster Notationen unter der Oberflaeche von Pergament und die dreidimensionale Analyse von Tintenschichten",
        answerB = "KI-basierte Tonsynthese, die aus Handschriften direkt Audio-Dateien erzeugt",
        answerC = "Druckverfahren, die historische Handschriften in hoher Aufloesung replizieren",
        answerD = "GPS-basierte Georeferenzierung von Handschriften in internationalen Bibliotheksdatenbanken",
        correctAnswer = 0,
        explanation = "Multispektrale Bildgebung (UV, IR, RTI) hat zahlreiche 'unsichtbare' Notationen sichtbar gemacht: verblichene Tinte, Korrekturen unter spaeterer Ueberschreibung (Palimpseste), Wasserzeichenanalyse. Projekte wie DIAMM (Digital Image Archive of Medieval Music) und IIIF-Standards digitalisieren weltweit Handschriften. Optische Zeichenerkennung (OMR — Optical Music Recognition) automatisiert zunehmend die Transkription.",
        difficulty = 5,
        funFact = "Das Archimedes-Palimpsest-Projekt (2000er Jahre) zeigte das Potential: Ein Gebet-Buch des 13. Jh. verbarg darunter Archimedestexte. Aehnliche Methoden haben inzwischen verborgene Notenhandschriften in juedischen Bibelhandschriften (Geniza-Funde) sichtbar gemacht."
    ),

    // Q32
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Tablatur' als Notationsform und welche Instrumente haben historisch die markantesten Tabulatur-Systeme entwickelt?",
        answerA = "Tablatur notiert nicht Tonhoehen, sondern Griff- oder Tastenpositionen auf einem Instrument — Laute, Gitarre und Orgel entwickelten eigenstaendige Systeme: franzoesische und italienische Lautentabulatur, deutsche Orgeltabulatur und spaenische Vihuala-Tablatur unterscheiden sich fundamental in ihrer graphischen Darstellung",
        answerB = "Tablatur ist eine vereinfachte Mensuralnotation, die nur fuer Percussionsinstrumente verwendet wurde",
        answerC = "Es handelt sich um ein byzantinisches Notationssystem, das im 15. Jahrhundert in den Westen kam",
        answerD = "Tablatur bezeichnet generell jede graphische Notation ohne Notenlinien",
        correctAnswer = 0,
        explanation = "Tabulaturen sind instrumentspezifische Griffnotierungen: Franzoesische Lautentabulatur (Buchstaben auf Linien), Italienische Lautentabulatur (Ziffern auf Linien), Deutsche Tabulatur (Buchstaben ohne Linien, aeltestes System), Gitarrentablatur (heute noch gebraeuchlich), Orgeltabulatur (alte deutsche: Buchstaben und Mensurzeichen). Spanische Vihuelen-Tabulatur ('cifras') war identisch mit ital. Lautentabulatur.",
        difficulty = 5,
        funFact = "Die deutsche Orgeltabulatur (14.-16. Jh.) ist so komplex, dass nur wenige Spezialisten sie lesen koennen. Das Buxheimer Orgelbuch (ca. 1460-70) enthaelt ca. 260 Stuecke in dieser Notation — ein Schatz, dessen vollstaendige Entzifferung Jahrzehnte dauerte."
    ),

    // Q33
    Question(
        categoryId = 5,
        questionText = "Welche Schriftform unterscheidet die karolingische Vokalnotation des 9.-11. Jahrhunderts von der spaeteren quadratischen Notation des 12.-13. Jahrhunderts?",
        answerA = "Karolingische Neumen sind 'in campo aperto' (im freien Feld ohne Hilfslinien) geschrieben — sie zeigen melodische Richtung und Ausdruck, aber keine exakten Tonhoehen; die quadratische Notation auf Linien fixiert erstmals die Tonhoehen eindeutig und ermoeglicht Wiederholbarkeit ohne muendliche Tradition",
        answerB = "Karolingische Neumen notieren exakte Tonhoehen, die spateren quadratischen Noten sind weniger praezise",
        answerC = "Beide Systeme sind funktional identisch und unterscheiden sich nur in der Strichfuehrung",
        answerD = "Karolingische Neumen wurden fuer mehrstimmige Musik geschaffen, waehrend quadratische Notation ausschliesslich fuer Einstimmigkeit dient",
        correctAnswer = 0,
        explanation = "Die fruehesten Neumen (adiastematisch) zeigen melodische Gesten und Ausdruck, aber keine fixen Tonhoehen — der Saenger musste die Melodie bereits kennen (muendliche Tradition). Erst Guido von Arezzo (ca. 991-1033) einfuehrte das Vier-Linien-System, das genaue Tonhoehen fixiert. Quadratische Notation (12. Jh., vor allem Notre-Dame) standardisierte die Darstellung fuer Polyphonie.",
        difficulty = 5,
        funFact = "Guidos Hut-Hand (Guidonische Hand) — Memorisierungshilfe fuer Toene durch Fingergelene — wurde noch im 17. Jahrhundert gelehrt. Die Erfindung der Tonhoehen-Fixierung auf Linien ist einer der radikalsten Einschnitte in der Musikgeschichte: Sie machte Musik ueber Zeit und Raum transportierbar."
    ),

    // Q34
    Question(
        categoryId = 5,
        questionText = "Was ist ein 'Palimpsest' im musikwissenschaftlichen Kontext und welche bedeutenden Musikpalimpseste wurden im 20. Jahrhundert entdeckt?",
        answerA = "Ein Palimpsest ist ein Pergament, das nach Abschaben einer frueheren Beschriftung wiederverwendet wurde — bedeutende Musikpalimpseste sind z.B. Fragmente der Missa Solemnis von Beethoven (spaetere Umarbeitungen) sowie mittelalterliche Gesangshandschriften unter juengeren Texten in der Stiftsbibliothek St. Gallen",
        answerB = "Palimpsest bezeichnet eine Technik der Notenkorrektur ohne Loeschemittel",
        answerC = "Es ist ein Kompositionsverfahren, das alte Melodien unter neuen Stimmen verbirgt",
        answerD = "Ein Palimpsest ist ein Druckfehler in fruehmittelalterlichen Traktaten, der durch Ueberschreiben korrigiert wurde",
        correctAnswer = 0,
        explanation = "Musikpalimpseste sind mehrfach verwendete Schreibmaterialien, bei denen unter dem sichtbaren Text aeltere Musik verborgen ist. Bedeutende Funde: Gregorianische Gesaenge unter juengeren Texten in Bibliotheken von Monte Cassino, Chartres und St. Gallen. Multispektrale Analyse hat teils verlorene Kompositionen des 9.-12. Jahrhunderts wiederhergestellt.",
        difficulty = 5,
        funFact = "In der Capitularisbibliothek Verona fanden sich unter einem Traktat des 8. Jahrhunderts fruehe Fragmente lateinischer Gesaenge — die aeltesten erhaltenen Zeugnisse westlicher Kirchenmusik. Der Fund erschuetterte die Timeline der Notationsgeschichte."
    ),

    // ── Mathematische Musiktheorie (8) ───────────────────────────────────────

    // Q35
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Pitch Class Set Theory' (Tonnklassen-Mengentheorie), wie sie Allen Forte (1973) systematisierte?",
        answerA = "Eine analytische Methode, die Toene ohne Beruecksichtigung von Oktavlage und enharmonischer Gleichheit in Klassen (Pitch Classes) gruppiert und ihre Mengen (Sets) durch Normalform, Primform und Intervallvektor beschreibt",
        answerB = "Eine Kompositionsmethode, die alle Toene eines Stueckes aus einer einzigen mathematischen Menge ableitet",
        answerC = "Ein Stimmungssystem, das gleichstufige Temperatur durch rationale Zahlen beschreibt",
        answerD = "Eine statistisch-analytische Methode zur Bestimmung der Haeufigkeit von Tonen in einem Musikstueck",
        correctAnswer = 0,
        explanation = "Allen Forte ('The Structure of Atonal Music', 1973) systematisierte die Pitch Class Set Theory: Toene werden unabhaengig von Oktav (C3 = C4 = Pitch Class 0) und enharmonischer Aehnlichkeit (C# = Db) betrachtet. Sets werden durch Normalform (kompakteste Anordnung) und Primform (niedrigstmoegliche Normalform) klassifiziert. Der Intervallvektor zeigt, wie oft jedes Intervall im Set vorkommt.",
        difficulty = 5,
        funFact = "Fortes Katalog listet alle 208 moeglichen Pitch Class Sets (von 3 bis 9 Elementen) mit Primform und Intervallvektor. Dieser Katalog ist das Standardnachschlagewerk der atonalen Analyse — jedes Set traegt eine Forte-Nummer (z.B. 3-1 bis 9-12)."
    ),

    // Q36
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung hat die 'Transformationstheorie' (Transformational Theory) von David Lewin in der Musikanalyse?",
        answerA = "Lewin beschreibt musikalische Beziehungen nicht als Messungen zwischen Objekten, sondern als Transformationen (Funktionen): Statt 'von Ton X zum Ton Y sind es 4 Halbtoene' fragt man 'welche Transformation T fuehrt von X nach Y?' — ein Perspektivwechsel von Abstand zu Handlung",
        answerB = "Transformationstheorie bezeichnet die mathematische Beschreibung von Modulation zwischen Tonarten",
        answerC = "Sie ist eine kompositorische Technik zur systematischen Transformation eines Themas durch Augmentation, Diminution und Umkehrung",
        answerD = "Lewin entwickelte eine Methode zur Umformung byzantinischer Modi in westliche Kirchentoene",
        correctAnswer = 0,
        explanation = "David Lewins 'Generalized Musical Intervals and Transformations' (GMIT, 1987) revolutionierte die Musiktheorie: Er ersetzte den statischen Begriff des 'Intervalls' durch den dynamischen Begriff der 'Transformation'. Musikanalytiker beschreiben nun, welche Operationen (Transpositionen, Inversionen, Retrograde) Toene, Akkorde oder Strukturen ineinander ueberfuehren. Grundlage: Gruppentheorie.",
        difficulty = 5,
        funFact = "Lewin war Mathematiker und Musikwissenschaftler — eine seltene Kombination. GMIT verwendete formale Notationen aus der Gruppentheorie, was es fuer viele Musikwissenschaftler schwer zugeganglich machte. Heute gilt es als Grundlagenwerk der Musiktheorie des 21. Jahrhunderts."
    ),

    // Q37
    Question(
        categoryId = 5,
        questionText = "Was ist der mathematische Hintergrund des 'Tonnetz' (Tonraumnetz) und wie wird es in der Transformationstheorie verwendet?",
        answerA = "Das Tonnetz ist ein zweidimensionales Gitter, in dem jeder Ton mit seinen Quint- und Terznachbarn verbunden ist — es visualisiert harmonische Naehbeziehungen und erlaubt die geometrische Darstellung von Neo-Riemannschen Transformationen (P, L, R)",
        answerB = "Das Tonnetz ist ein dreidimensionaler Klangkoerper zur Visualisierung des Harmonieraums in der computergenerierten Musik",
        answerC = "Es ist eine Methode zur graphischen Notierung atonaler Musik ohne Linien",
        answerD = "Das Tonnetz wurde von Riemann entwickelt, um die Durverwandtschaft zwischen Dur- und Molltonarten darzustellen",
        correctAnswer = 0,
        explanation = "Das Tonnetz (urspruenglich Euler 1739, dann Oettingen 1866, Riemann spaeter) ist ein Gitter, in dem benachbarte Toene im Quintabstand (horizontal) und im Terzenabstand (vertikal) stehen. Neo-Riemannian Theory (Cohn, Hyer, 1990er) nutzt es, um Transformationen wie Parallel (P: Terz-Aenderung), Leading-Tone (L: Halbton-Verschiebung) und Relative (R: relative Dur/Moll) zu visualisieren.",
        difficulty = 5,
        funFact = "Das Tonnetz hat eine torologische Struktur — wenn man es in alle Richtungen erweitert, schliesst es sich nach 12 Quint- und 4 Terz-Schritten zu einem Torus (Donut-Form). Diese geometrische Eigenschaft erklaert, warum enharmonische Gleichheit im Zwoeflton-System existiert."
    ),

    // Q38
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Godels Unvollstaendigkeitssatz' Analogie in der Musiktheorie nach Douglas Hofstadter und welche philosophischen Konsequenzen zieht er?",
        answerA = "Hofstadter argumentiert in 'Goedel, Escher, Bach' (1979), dass selbstreferenzielle Strukturen in Musik (z.B. Bachs Kanons, die ihre eigene Transposition enthalten) analoge Strukturen zu Goedels Satz aufweisen: Systeme, die reich genug sind, produzieren Aussagen, die sie selbst nicht beweisen koennen — auch Musik kann ueber sich selbst sprechen",
        answerB = "Hofstadter behauptet, dass alle Musik mathematisch unvollstaendig ist und deshalb nicht vollstaendig notiert werden kann",
        answerC = "Er argumentiert, dass Goedel die mathematische Grundlage aller tonalen Musik vorhergesagt hat",
        answerD = "Die Analogie beschraenkt sich auf die Zahl der Kanonstimmen bei Bach: Goedels Satz hat genau so viele Beweis-Stufen wie Bachs Kanon Stimmen hat",
        correctAnswer = 0,
        explanation = "Hofstadters 'Goedel, Escher, Bach: An Eternal Golden Braid' (1979, Pulitzer-Preis) zieht Parallelen zwischen Goedels Beweis (1931), Eschers Zeichnungen und Bachs Musik: Alle drei kreieren Systeme, die sich selbst referenzieren und damit 'Strange Loops' erzeugen. Der 'Endlessly Rising Canon' (BWV 1079) steigt in jeder Wiederholung um eine Stufe — und landet nach 6 Transpositionen wieder am Ausgangspunkt.",
        difficulty = 5,
        funFact = "Bachs 'Musikalisches Opfer' (BWV 1079) ist Hofstadters Hauptbeispiel: Das Koenigsthema, das Bach fuer Friedrich den Grossen improvisierte, ist so konstruiert, dass seine Kanonformen selbstaehnlich sind — ein mathematisches Kunstwerk vor der modernen Mathematik."
    ),

    // Q39
    Question(
        categoryId = 5,
        questionText = "Was beschreibt die 'Spectromorphologie' von Denis Smalley in der elektroakustischen Musikanalyse?",
        answerA = "Eine analytische Beschreibungssprache fuer elektroakustische Musik, die Klangbewegungen durch Prozess-Kategorien (z.B. Onset, Continuant, Termination) und Bewegungsmodelle (unidirektional, bidirektional, zyklisch) erfasst",
        answerB = "Ein Kompositionssystem, das elektronische Klangfarben nach mathematischen Spektrum-Algorithmen erzeugt",
        answerC = "Eine Methode zur Messung von Dezibel-Pegelverlaeufen in elektroakustischen Stuecken",
        answerD = "Die Analyse des Frequenzspektrums einzelner Instrumentaltoene mit FFT-Algorithmen",
        correctAnswer = 0,
        explanation = "Denis Smalleys Spectromorphologie ('Spectromorphology: explaining sound-shapes', 1997) beschreibt elektroakustische Klangbewegungen durch ein Vokabular aus Motion, Growth, Trajectory und Archetype. Kernbegriffe: Spectral Space (Verteilung im Frequenzraum), Motion (Bewegungstypen: Unidirectional, Reciprocal, Cyclic, Multidirectional) und Morphological Archetypes (wie Gestalten entstehen und vergehen). Es ist die wichtigste Analyse-Methode der elektroakustischen Musik.",
        difficulty = 5,
        funFact = "Smalley entwickelte die Spectromorphologie, um den Mangel einer gemeinsamen Analysesprache fuer elektroakustische Musik zu beheben — ein Bereich, in dem konventionelle Notationsanalyse scheitert. Heute wird sie weltweit in elektroakustischen Lehrprogrammen eingesetzt."
    ),

    // Q40
    Question(
        categoryId = 5,
        questionText = "Was ist die mathematische Grundlage des 'Ratio-Based Just Intonation' und wie unterscheidet es sich von der gleichstufigen Temperatur?",
        answerA = "Just Intonation basiert auf ganzzahligen Frequenzverhaeltnissen (z.B. Quinte = 3:2, grosse Terz = 5:4) — gleichstufige Temperatur (12-TET) approximiert diese durch irrationale Zahlen (12. Wurzel aus 2), was die Quinte um ca. 2 Cent schmaelt und die grosse Terz um ca. 14 Cent vergroessert",
        answerB = "Just Intonation und 12-TET sind mathematisch identisch und klingen praktisch gleich",
        answerC = "Just Intonation basiert auf gleichmaessiger Teilung der Oktave in 12 Intervalle mit rationalen Zahlen",
        answerD = "Just Intonation verwendet komplexe Brueche (z.B. 17:13), waehrend 12-TET einfache ganzzahlige Verhaeltnisse verwendet",
        correctAnswer = 0,
        explanation = "Just Intonation verwendet Frequenzverhaeltnisse kleiner ganzer Zahlen: Oktave 2:1, Quinte 3:2, reine grosse Terz 5:4, kleine Terz 6:5. Gleichstufige 12-TET approximiert: die 12-TET Quinte ist 2^(7/12) ≈ 1.4983 (statt 1.5000) — ca. 1.96 Cent zu niedrig. Die 12-TET grosse Terz ist 2^(4/12) ≈ 1.2599 (statt 1.2500) — ca. 13.69 Cent zu hoch. Diese Unterschiede sind fuer geoebte Ohren deutlich hoerbar.",
        difficulty = 5,
        funFact = "La Monte Young gruendete seine Minimal Music auf reinen Stimmungsverhaeltnissen bis zu 7- und 11-Teilern: Sein 'Well-Tuned Piano' (1964-) verwendet ein eigenes Just-Intonation-System mit hochprimzahligen Verhaeltnissen, das vom Stimmungsplan von Harry Partch (43-Ton-System) inspiriert ist."
    ),

    // Q41
    Question(
        categoryId = 5,
        questionText = "Welchen Beitrag leistete Guerino Mazzola mit dem Konzept des 'Topos of Music' zur mathematischen Musiktheorie?",
        answerA = "Mazzola entwickelte eine umfassende algebraisch-topologische Theorie der Musik, die musikalische Strukturen als mathematische Kategorien und Garben (Sheaves) formalisiert und Komposition, Analyse und Ausfuehrung in einem einheitlichen mathematischen Rahmen beschreibt",
        answerB = "Er bewies, dass alle Musik in endliche mathematische Gruppen zerlegt werden kann",
        answerC = "Mazzola entwickelte eine statistische Methode zur Analyse von Popularmusikhaeufigkeiten",
        answerD = "Er praegte den Begriff 'Topos' als Metapher fuer den Stimmungsraum einer Komposition",
        correctAnswer = 0,
        explanation = "Guerino Mazzolas 'The Topos of Music' (2002, mit David Mazzola und Stefan Mueller, 1300 Seiten) ist das umfangreichste Werk der mathematischen Musiktheorie. Es verwendet Kategorientheorie, Algebraische Topologie und Garbentheorie, um Musiktheorie axiomatisch zu begrnueden. Konzepte: Denotator, Form, Modulator, Nerve Theorie. Das Werk ist fuer Musikwissenschaftler ohne mathematischen Hintergrund kaum zugaenglich.",
        difficulty = 5,
        funFact = "Mazzolas Projekt laeuft seit den 1980ern und hat mehrere Versionen hervorgebracht. Er entwickelte auch die Software 'Rubato' — ein mathematisch fundiertes System zur Analyse und Echtzeit-Komposition, das Musikkritiker als 'das komplexeste Musiksoftware-System der Welt' bezeichneten."
    ),

    // Q42
    Question(
        categoryId = 5,
        questionText = "Was beschreibt die 'Diatonic Set Theory' und welche Eigenschaften hebt sie als besonders musikalisch relevant hervor?",
        answerA = "Diatonic Set Theory (Clough, Myerson, 1985; Carey, Clampitt, 1989) untersucht mathematische Eigenschaften diatonischer Skalen: Sie zeigt, dass die diatonische Skala einzigartige Eigenschaften besitzt (Myhill Property, Cardinality Equals Variety, Deep Scale Property), die ihre ubiquitaere Verbreitung in weltweiter Musik erklaeren koennen",
        answerB = "Eine Theorie, die beweist, dass diatonische Skalen die einzig mathematisch stabilen Skalensysteme sind",
        answerC = "Ein Analysesystem, das alle westlichen Tonarten auf eine einzige Urskala zurueckfuehrt",
        answerD = "Die mathematische Beschreibung diatonischer Skalen durch Fourier-Analyse",
        correctAnswer = 0,
        explanation = "Clough und Myerson zeigten (1985), dass die diatonische Skala die 'Myhill Property' besitzt: Jedes generische Intervall (Sekunde, Terz, usw.) nimmt genau zwei spezifische Groessen an (z.B. grosse/kleine Sekunde). 'Cardinality Equals Variety' (CEV): in einer Skala mit n Stufen gibt es n verschiedene modale Transpositionen. Diese mathematischen Eigenschaften erklaeren die ausserordentliche Flexibilitaet diatonischer Systeme.",
        difficulty = 5,
        funFact = "Norman Carey und David Clampitt zeigten 1989, dass diatonische Skalen 'Well-Formed Scales' sind: Skalen, die durch einen einzigen Generator (Quinte) erzeugt werden und in denen beide auftretenden Schrittgroessen in einem einfachen Zahlenverhaeltnis stehen. Dieses Konzept verbindet antike Stimmungstheorie mit moderner Gruppentheorie."
    ),

    // ── Orgelbau / Orgelliteratur (8) ────────────────────────────────────────

    // Q43
    Question(
        categoryId = 5,
        questionText = "Was ist der akustische Unterschied zwischen einer 'Labialpfeife' (Flue Pipe) und einer 'Lingualpfeife' (Reed Pipe) in der Orgel?",
        answerA = "Labialpfeifen erzeugen Klang durch Kantenaerregung (Luftstrahl trifft auf eine Schneide und erzeugt Wirbelung, aehnlich einer Floete); Lingualpfeifen erzeugen Klang durch das Schwingen einer Metallzunge (durchschlagendes Rohrblatt) — vergleichbar mit Klarinette oder Fagott",
        answerB = "Labialpfeifen sind offene Pfeifen, Lingualpfeifen sind gedeckte Pfeifen — der Unterschied liegt im Rohrende",
        answerC = "Labialpfeifen verwenden Holz als Material, Lingualpfeifen ausschliesslich Zinn",
        answerD = "Der Unterschied ist rein optisch: Labialpfeifen sind rund, Lingualpfeifen rechteckig",
        correctAnswer = 0,
        explanation = "Labialpfeifen (Fl&ouml;tenprinzip): Ein Luftstrahl trifft auf die Kernspalte (Labium) und erzeugt durch Wirbelbildung stehende Wellen im Rohr. Klangfarbe: weich bis fluoetenhafte Register. Lingualpfeifen (Rohrblatt-Prinzip): Eine Metallzunge schwingt gegen einen Stiefel, der Resonator verstaerkt selektiv. Klangfarbe: durchdringend, oberttonreich (Trompete, Oboe, Krummhorn, Vox Humana).",
        difficulty = 5,
        funFact = "Die Vox Humana ('Menschenstimme') ist eine Lingualpfeife, die einen Resonator hat, der kurz genug ist, um die charakteristisch 'bebende' Intonation zu erzeugen — ein akustisches Annaeherungsversuch an die menschliche Vokalstimme, das seit dem 17. Jahrhundert Organisten fasziniert."
    ),

    // Q44
    Question(
        categoryId = 5,
        questionText = "Was ist ein 'Werk' (Division) in der historischen norddeutschen Orgel und welche Werkkonzeption praegte die Orgeln des 17. Jahrhunderts?",
        answerA = "Norddeutsche Barockorgeln haben mehrere selbstaendige 'Werke' (Manual- und Pedalwerke): Hauptwerk, Rueckpositiv (hinter dem Organisten), Brustwerk (in der unteren Windlade) und Pedal — jedes Werk hat eigene Zungenstimmen und Labialstimmen und schafft raeumlich differenzierten Mehrchorklan",
        answerB = "Ein 'Werk' bezeichnet die Gesamtheit aller Register einer Orgel, ohne raeumliche Differenzierung",
        answerC = "Im norddeutschen Orgelbau bezeichnet 'Werk' ausschliesslich das Pedalwerk mit den groessten Pfeifen",
        answerD = "Das Werksystem gilt nur fuer die Hamburger Orgelbautradition und nicht fuer andere norddeutsche Zentren",
        correctAnswer = 0,
        explanation = "Die norddeutsche Werkorgel (Arp Schnitger, Zacharias Hildebrandt) basiert auf dem Prinzip mehrerer selbstaendiger, raeumlich getrennter Werke: Das Hauptwerk in der Mitte, das Rueckpositiv an der Rueckseite der Emporenbruestung (oft mit eigenen Pedalpfeifen darunter), Brustwerk und Oberwerk in Hoehenlagen, Pedalwerk seitlich. Jedes Werk hat eigene Windversorgung, Ladensystem und Klangcharakter.",
        difficulty = 5,
        funFact = "Arp Schnitger (1648-1719) baute ca. 170 Orgeln — sein Lebenswerk umfasst die bedeutendsten Instrumente Norddeutschlands und der Niederlande. Die Grosse Orgel der Hauptkirche St. Jacobi in Hamburg (1693) hat 60 Register auf 4 Manualen und Pedal und gilt als sein Meisterwerk."
    ),

    // Q45
    Question(
        categoryId = 5,
        questionText = "Was ist die kompositorische Bedeutung des 'Cantus firmus' in der Orgelmusik des 16.-18. Jahrhunderts?",
        answerA = "Ein vorgegebener, oft gregorianischer Melodie-Strang (Cantus firmus), der in grossen, langsamen Notenwerten in einer Stimme der Orgeltextur gehalten wird, waehrend die anderen Stimmen frei kontrapunktieren — eine grundlegende Technik von Scheidt bis Bach",
        answerB = "Der Cantus firmus bezeichnet die Bassstimme einer Orgelkomposition, die den Basso Continuo ausfuehrt",
        answerC = "Er ist ein spezifisches Register der Orgel (Cantus firmus-Register), das fuer Choralmelodien reserviert ist",
        answerD = "Cantus firmus in der Orgelmusik bezeichnet den Einstieg der Haende auf der Klaviatur, bevor die Fueße das Pedal betreten",
        correctAnswer = 0,
        explanation = "Der Cantus firmus (fester Gesang) ist eine ueberlieferte Melodie (Choralmelodie, weltliche Melodie), die in einer Orgelstimme in langen Notenwerten gehalten wird. In Buxtehudes Choralfantasien liegt er oft im Sopran (Manubrium-Technik), bei Bach teils im Pedal (Choralpartiten), teils in der Oberstimme (Choralbearb. BWV 599ff.). Scheidt ('Tabulatura Nova', 1624) systematisierte Cantus-firmus-Techniken.",
        difficulty = 5,
        funFact = "Dietrich Buxtehude (ca. 1637-1707), Bachs Vorgaenger in Luebeck, lockte jungen Musiker aus ganz Deutschland zu seinen Abendmusiken. Bach lief 1705 angeblich 400 km zu Fuss von Arnstadt nach Luebeck, um Buxtehude zu hoeren — ein Pilgerweg in der Musikgeschichte."
    ),

    // Q46
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen einer 'mechanischen Traktur' und einer 'pneumatischen/elektrischen Traktur' in der Orgel und welche aesthetischen Konsequenzen hat dies?",
        answerA = "Mechanische Traktur (Schleiflade) verbindet Taste und Ventil direkt durch Holz/Metallteile — der Organist spuert den Luftwiderstand und kann den Ton agogisch gestalten; pneumatische und elektrische Traktur loesen diese direkte Verbindung und geben dem Organisten mehr Freiheit in der Disposition, aber weniger Spielkontrolle",
        answerB = "Der Unterschied ist rein technisch-konstruktiv ohne aesthetische Auswirkungen auf das Spiel",
        answerC = "Mechanische Traktur ist lauter als elektrische und wird deshalb fuer groeße Raeume bevorzugt",
        answerD = "Elektrische Traktur ermoeglicht groessere Winddrucke und wird deshalb fuer dramatische Orgelmusik bevorzugt",
        correctAnswer = 0,
        explanation = "Mechanische Schleiflade: Taste → Wellenbrett → Abstrakte → Ventil. Der Organist hat direkte Rueckkopplung (Luftwiderstand an der Taste) und kann durch Beruehren der Taste subito Dynamik und Ansprache beeinflussen. Pneumatische/elektrische Systeme (19./20. Jh.): Schnellere Dispositionierung, groessere Abstande zwischen Spieltisch und Pfeifenwerk moeglich, aber Verlust der haptischen Rueckkopplung. Die Orgelbewegung (Organ Reform Movement, ab 1925) forderte Rueckkehr zur mechanischen Traktur.",
        difficulty = 5,
        funFact = "Albert Schweitzer war ein Hauptkritiker der pneumatischen Orgel: Er argumentierte, dass Bach auf mechanischen Orgeln komponierte und pneumatische Instrumente Bachs Musik verfaelschten. Sein Engagement trug massgeblich zur Orgelbewegung bei, die mechanische Traktur wieder etablierte."
    ),

    // Q47
    Question(
        categoryId = 5,
        questionText = "Was ist die musikalische und strukturelle Besonderheit von Cesar Francks Orgelsonate 'Prelude, Fugue et Variation' op. 18 und ihrem Stellung im franzoesischen Orgelrepertoire?",
        answerA = "Franck schuf mit seinen 'Six Pieces' op. 16-21 (1856-62) und 'Trois Chorals' (1890) die Grundlage des franzoesischen symphonischen Orgelstils: zyklische Form, chromatische Harmonik, weiche Registration und die Idee der Orgel als symphonisches Instrument — ein Bruch mit der deutschen Kontrapunkttradition",
        answerB = "Francks op. 18 ist eine zwoelftonalige Komposition, die den Serialismus im Orgelrepertoire einfuehrte",
        answerC = "Das Stueck ist ein transskribiertes Orchesterwerk, das Franck fuer die Orgel in der Notre-Dame bearbeitete",
        answerD = "Es handelt sich um die erste Orgelsonate, die ausschliesslich fuer das Pedal ohne Manuale konzipiert wurde",
        correctAnswer = 0,
        explanation = "Cesar Franck (1822-1890) revolutionierte die franzoesische Orgelmusik: Seine chromatische Harmonik, zyklische thematische Arbeit (Rueckkehr von Themen am Ende) und Nutzung des Cavaille-Coll-Harmoniums (mit Crescendo-Pedal) praegte den franzoesisch-symphonischen Orgelstil. Schueler wie Guilmant, Widor, Tournemire und Vierne entwickelten diesen Stil weiter zur 'Ecole Classique'.",
        difficulty = 5,
        funFact = "Franck spielte taeglich auf der Orgel der Basique Sainte-Clotilde in Paris, wo er Titulaire (Titularorganist) war. Er improvisierte dort stundenlang und entwickelte so seinen unverwechselbaren harmonischen Stil — Improvisation und Komposition waren fuer ihn untrennbar."
    ),

    // Q48
    Question(
        categoryId = 5,
        questionText = "Was ist die Orgelsymphonie-Tradition der franzoesischen Romantik und welche Komponisten praegte sie?",
        answerA = "Aristide Cavaille-Coll baute mit seinen zweimanualigen Orgeln den klangtechnischen Rahmen; Charles-Marie Widor schrieb zehn Orgelsymphoien (1872-1900), die mehrsaetzige grossangelegte Zyklen sind — sein Schueler Louis Vierne setzte die Tradition mit sechs Symphoieen fort, Marcel Dupre mit zwei",
        answerB = "Die Orgelsymphonie-Tradition bezeichnet speziell Orchestersymphoieen mit Orgelanteil, wie Saint-Saens' Sinfonie Nr. 3",
        answerC = "Widor komponierte die erste Orgelsymphonie als Programmusik, die die Schopfungsgeschichte darstellt",
        answerD = "Die franzoesische Orgelsymphonie-Tradition besteht ausschliesslich aus einsaetzigen Werken fuer Solo-Orgel",
        correctAnswer = 0,
        explanation = "Charles-Marie Widor (1844-1937) schrieb 10 Orgelsymfonien: op. 13 Nr. 1-4 (1872), op. 42 Nr. 5-8 (1879), op. 70 Nr. 9 'Gothique' (1895) und op. 73 Nr. 10 'Romane' (1900). Jede Symphonie hat 4-6 Saetze und ist fuer die Cavaille-Coll-Orgel der Saint-Sulpice in Paris konzipiert. Schueler Louis Vierne setzte 6 Orgelsymphoien hinzu, Dupre ergaenzte mit 2.",
        difficulty = 5,
        funFact = "Widors Toccata aus der 5. Orgelsymphonie op. 42 ist eine der meistgespielten Orgelkompositionen weltweit — paradoxerweise ein einzelner Satz aus einem zehnsaetzigen Zyklus. Widor selbst bevorzugte das Finale der 6. Symphonie als sein bedeutendstes Werk."
    ),

    // Q49
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen einer 'Prospektpfeife' und einer 'Blindpfeife' in der Orgelbauaesthetik?",
        answerA = "Prospektpfeifen sind die sichtbaren Pfeifen in der Schauseite (Prospekt) der Orgel, die auch klingende Pfeifen sind; Blindpfeifen sind Attrappen ohne Klangfunktion, rein fuer aesthetische Symmetrie im Prospekt eingebaut",
        answerB = "Prospektpfeifen sind im Inneren der Orgel verborgen, Blindpfeifen sind sichtbar aber stumm",
        answerC = "Beide Begriffe sind Synonyme fuer Labialpfeifen unterschiedlicher Groesse",
        answerD = "Prospektpfeifen sind klingende Pfeifen aus poliertem Zinn, Blindpfeifen aus Holz fuer tiefe Register",
        correctAnswer = 0,
        explanation = "In der Orgelbauaesthetik ist der Prospekt (Schauseite) oft symmetrisch gestaltet. Wo die symmetrische Anordnung Pfeifen erfordern wuerde, die aus akustischen Gruenden nicht an dieser Stelle stehen koennen, werden Blindpfeifen (stumme Attrappen) eingebaut. Im Barockorgelbau waren Prospekte oft aufwendig vergoldet und geschnitzt — das Instrument als Gesamtkunstwerk.",
        difficulty = 5,
        funFact = "In manchen historischen Orgeln sind die Prospektpfeifen aus reinem Zinn (95%+), waehrend innere Pfeifen aus Blei-Zinn-Legierungen bestehen. Zinn klingt heller und ist optisch glaenzend — ein akustisches und aesthetisches Argument fuer die teuren Schaufenster-Pfeifen."
    ),

    // Q50
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung hat Olivier Messiaens 'L'Ascension' (1934) fuer die Orgelliteratur des 20. Jahrhunderts und wie unterscheidet es sich von seiner Orchesterfassung?",
        answerA = "Messiaens Orgelfassung ersetzt den dritten Satz der Orchesterfassung ('Alleluia sur la trompette') durch ein neues 'Transports de joie d'une ame devant la gloire du Christ', das seine modalen Techniken, Vogelgesang-Imitation und rhythmische Transformationen in der Orgeltextur erstmals umfassend einsetzt",
        answerB = "Die Orgelfassung ist eine genaue Transkription der Orchesterfassung ohne strukturelle Aenderungen",
        answerC = "Messiaen verzichtete in der Orgelfassung auf das Pedal und schrieb ausschliesslich fuer die Manuale",
        answerD = "Die Orgelfassung wurde frueher als die Orchesterfassung uraufgefuehrt und gilt als das Originalwerk",
        correctAnswer = 0,
        explanation = "Messiaen schrieb 'L'Ascension' zunaechst fuer Orchester (1932-33), dann fuer Orgel (1934). In der Orgelfassung ersetzte er den dritten Satz, weil der Orchestercharakter des Originalsatzes nicht fuer Orgel taugte. Das neue Finale 'Transports de joie' ist ein Virtuosenstueck, das Messiaens eigene Orgeltechnik (schnelle Manualwechsel, polytempisches Pedal, Modi der begrenzten Transpositionsmoeiglichkeit) erstmals entfaltet.",
        difficulty = 5,
        funFact = "Messiaen war von 1931 bis 1992 — also 61 Jahre! — Titularorganist der Trinite in Paris. Er improvisierte jeden Sonntag und spielte saemtliche groesse Kirchenfeste. Diese jahrzehntelange Praxis ist die Grundlage seines einzigartigen Orgel-Stils."
    )
)
