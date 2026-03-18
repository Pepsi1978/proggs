package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsMaster4(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) — Advanced Ethology & Cognitive Zoology ──────

    // Q1
    Question(
        categoryId = 9,
        questionText = "Welche der vier Fragen Tinbergens bezieht sich auf die unmittelbare Kausalitaet eines Verhaltens — also auf die internen und externen Reize, die ein Verhalten ausloesen?",
        answerA = "Die Frage nach der Phylogenese",
        answerB = "Die Frage nach der Ontogenese",
        answerC = "Die Frage nach dem Mechanismus (Kausalitaet)",
        answerD = "Die Frage nach der Funktion (Anpassungswert)",
        correctAnswer = 2,
        explanation = "Tinbergens vier Fragen (1963) sind: Kausalitaet (Mechanismus), Ontogenese (Entwicklung), Funktion (Anpassungswert) und Phylogenese (Evolution). Die Frage nach dem Mechanismus fragt nach den unmittelbaren neuronalen, hormonellen und sensorischen Prozessen, die ein Verhalten ausloesen und steuern.",
        difficulty = 5,
        funFact = "Tinbergen formulierte diese vier Fragen 1963 in seinem einflussreichen Paper 'On aims and methods of ethology', das bis heute als konzeptionelles Grundgeruest der Verhaltensbiologie gilt."
    ),

    // Q2
    Question(
        categoryId = 9,
        questionText = "In Karl von Frischs Entdeckung des Bienentanzes: Welchen Winkel zur Senkrechten nimmt der Schwanzwedelgang im Schwanzeltanz ein, wenn die Nahrungsquelle in Richtung der Sonne liegt?",
        answerA = "90 Grad (horizontal)",
        answerB = "45 Grad (schraeg aufwaerts)",
        answerC = "0 Grad (senkrecht aufwaerts)",
        answerD = "180 Grad (senkrecht abwaerts)",
        correctAnswer = 2,
        explanation = "Im Schwanzeltanz auf der senkrechten Wabe wird die Himmelsrichtung in einen Gravitationswinkel umgewandelt: Ein Schwanzwedelgang senkrecht aufwaerts (0 Grad zur Vertikalen) bedeutet, die Nahrungsquelle liegt in Richtung der Sonne. Der Winkel des Tanzes zur Senkrechten entspricht dem Winkel der Flugrichtung zur Sonne.",
        difficulty = 5,
        funFact = "Von Frisch erhielt 1973 gemeinsam mit Lorenz und Tinbergen den Nobelpreis fuer Physiologie oder Medizin — die einzigen Ethologen, die je mit dem Nobelpreis ausgezeichnet wurden."
    ),

    // Q3
    Question(
        categoryId = 9,
        questionText = "Konrad Lorenz pragte das Konzept der Pragungsphase. In welchem kritischen Zeitfenster nach dem Schluepfen werden Graugaense (Anser anser) am staerksten auf ein Bewegtes Objekt als Mutterfigur gepraegt?",
        answerA = "Erste 1-2 Stunden nach dem Schluepfen",
        answerB = "13 bis 16 Stunden nach dem Schluepfen",
        answerC = "Erster ganzer Tag (0-24 Stunden)",
        answerD = "Zweiter bis dritter Tag nach dem Schluepfen",
        correctAnswer = 1,
        explanation = "Lorenz zeigte, dass Graugaense eine sensible Phase fuer Filialimpraegung haben, die etwa 13-16 Stunden nach dem Schluepfen ihren Hoehepunkt erreicht. In diesem Zeitfenster werden die Jungtiere am staerksten auf ein sich bewegendes Objekt gepraegt, das dann als Mutterfigur fungiert.",
        difficulty = 5,
        funFact = "Lorenz' Gaense, die auf ihn selbst gepraegt waren, folgten ihm ueberall hin — diese Bilder gingen um die Welt und machten die Verhaltensforschung populaer."
    ),

    // Q4
    Question(
        categoryId = 9,
        questionText = "David Premacks Experimente mit der Schimpansin Sarah demonstrierten erstmals symbolisches Denken bei nicht-menschlichen Primaten. Welches Medium nutzte Premack, um Sarah beizubringen, Symbole fuer Konzepte zu verwenden?",
        answerA = "Computerbildschirme mit Piktogrammen",
        answerB = "Gebaerdensprache (American Sign Language)",
        answerC = "Plastikplaettchen mit verschiedenen Formen und Farben",
        answerD = "Akustische Signale ueber Lautsprecher",
        correctAnswer = 2,
        explanation = "Premack verwendete ab 1966 unterschiedlich geformte und farbige Plastikplaettchen als Symbole. Sarah lernte, diese Plaettchen auf einer Magnetwand anzuordnen, um Konzepte wie Nahrungsmittel, Farben und logische Beziehungen wie 'gleich' und 'verschieden' zu kommunizieren.",
        difficulty = 5,
        funFact = "Premacks Arbeit inspirierte Sue Savage-Rumbaugh, die Lexigram-Methode fuer Bonobo Kanzi zu entwickeln — eine noch raffiniertere Form der Sprachforschung bei Menschenaffen."
    ),

    // Q5
    Question(
        categoryId = 9,
        questionText = "Sue Savage-Rumbaugh arbeitete mit dem Bonobo Kanzi, der Lexigramme erwarb. Durch welche besondere Lernweise erwarb Kanzi seine ersten Lexigramme, im Gegensatz zu anderen trainierten Menschenaffen?",
        answerA = "Durch klassische Konditionierung mit Futterbelohnung",
        answerB = "Durch Imitation seiner Mutter Matata beim Training",
        answerC = "Durch operante Konditionierung mit Klickerlautgeber",
        answerD = "Durch Zeigen auf Objekte im Labor ohne formales Training",
        correctAnswer = 1,
        explanation = "Kanzi erwarb Lexigramme durch unbeabsichtigtes Beobachtungslernen: Waehrend seine Mutter Matata formal mit Lexigrammen trainiert wurde (ohne grossen Erfolg), beobachtete der junge Kanzi das Training und erwarb die Symbole ohne gezieltes Konditionierungs-Training — ein Schluessel-Befund fuer soziales Lernen bei Bonobos.",
        difficulty = 5,
        funFact = "Kanzi konnte gesprochene englische Woerter verstehen und nicht nur Lexigramme produzieren — seine rezeptiven Sprachleistungen waren denen eines 2-3-jaehrigen Kindes vergleichbar."
    ),

    // Q6
    Question(
        categoryId = 9,
        questionText = "Der False-Belief-Test nach Wimmer und Perner (1983) prueft das Vorhandensein einer Theory of Mind. In welcher Studie wurde erstmals getestet, ob Schimpansen eine Theory of Mind (ToM) besitzen?",
        answerA = "Byrne & Whiten, 1988 (Machiavellian Intelligence)",
        answerB = "Povinelli & Eddy, 1996 (Sichtlinie Experimente)",
        answerC = "Premack & Woodruff, 1978 (Does the chimpanzee have a theory of mind?)",
        answerD = "de Waal & Roosmalen, 1979 (Reconciliation Studie)",
        correctAnswer = 2,
        explanation = "Premack und Woodruff publizierten 1978 in Behavioral and Brain Sciences die wegweisende Studie 'Does the chimpanzee have a theory of mind?' Dabei schaute die Schimpansin Sarah Filmausschnitte von Menschen in Problemsituationen an und waehlte passende Loesungsphotos aus — was eine Faehigkeit zur mentalen Zustandsattribution implizierte.",
        difficulty = 5,
        funFact = "Das Paper von Premack & Woodruff loeste eine Lawine an Folgeforschung aus. Die beruehmten Anmerkungen von Dennett, Pylyshyn und anderen zu diesem Paper fuehrten zur Entwicklung des False-Belief-Tests."
    ),

    // Q7
    Question(
        categoryId = 9,
        questionText = "Was versteht man in der Kognitionsforschung unter dem Begriff 'episodisches Gedaechtnis' nach Endel Tulving, und welches Tier lieferte den ersten uebertragbaren Nachweis eines 'episodenaehnlichen' Gedaechtnisses?",
        answerA = "Langzeitgedaechtnis fuer Fakten; Schimpansen in Menzel 1974",
        answerB = "Was-Wo-Wann-Gedaechtnis; Buschhaehers (Aphelocoma californica) in Clayton & Dickinson 1998",
        answerC = "Prospektives Gedaechtnis; Delfine in Herman 2002",
        answerD = "Autobiographisches Gedaechtnis; Ratten in Eacott & Norman 2004",
        correctAnswer = 1,
        explanation = "Clayton und Dickinson zeigten 1998 in Nature, dass Buschhaehers (Western scrub-jays, Aphelocoma californica) sich erinnern, WAS sie gecacht haben, WO sie es gecacht haben und WANN — das episodische Trias nach Tulving. Sie versteckten Wuermer oder Erdnuesse bevorzugt dort neu, wo andere Haehers sie beobachtet hatten, was eine komplexe What-Where-When-Kodierung belegt.",
        difficulty = 5,
        funFact = "Corviden wie Kraehen und Haehers haben eine im Vergleich zur Koerpergroesse ueberproportional grosse hippocampusaehnliche Struktur — parallel zu Primaten, aber unabhaengig evolviert."
    ),

    // Q8
    Question(
        categoryId = 9,
        questionText = "Welche Methode nutzte Nicola Clayton, um zu testen, ob Buschhaehers strategisch 'Diebstahl' durch Rekachierung verhindern, und was ist das Kernresultat?",
        answerA = "Haehers versteckten Futter immer tiefer, wenn Beobachter zugegen waren",
        answerB = "Haehers verlagerten Caches nur dann, wenn sie als Diebe erfahren waren und beobachtet wurden",
        answerC = "Haehers teilten Futter mit dominanten Beobachtern, um Konflikt zu vermeiden",
        answerD = "Haehers zeigten kein strategisches Rekachierungsverhalten",
        correctAnswer = 1,
        explanation = "Clayton et al. (2001, Nature) zeigten: Haehers, die selbst gestohlen hatten ('Dieb-Erfahrung'), verlagerten ihre Caches in neue Verstecke, wenn ein anderer Haehers beim Cachieren zugeschaut hatte. Haehers ohne Dieb-Erfahrung taten dies nicht. Dies deutet auf eine Perspektiveuebernahme hin — sie projizieren eigene Diebstahl-Motivation auf den Beobachter.",
        difficulty = 5,
        funFact = "Dieses Experiment gilt als eines der elegantesten Designs in der vergleichenden Kognitionsforschung, da es sowohl episodisches Gedaechtnis als auch rudimentaere Theory of Mind in einem einzigen Paradigma testet."
    ),

    // Q9
    Question(
        categoryId = 9,
        questionText = "Was ist das 'Umweg-Problem' (detour problem) nach Wolfgang Koehler, und welches kognitive Konzept belegte er damit bei Schimpansen?",
        answerA = "Das Tier muss sich von der Nahrung wegbewegen, um sie zu erreichen — belegt Einsicht (Insight Learning)",
        answerB = "Das Tier muss ein Objekt umgehen, um Futter zu erreichen — belegt assoziatives Lernen",
        answerC = "Das Tier navigiert durch ein Labyrinth — belegt raeumliches Gedaechtnis",
        answerD = "Das Tier nutzt Werkzeuge in einer Sequenz — belegt kausales Verstaendnis",
        correctAnswer = 0,
        explanation = "Koehler beschrieb in 'Intelligenzpruefungen an Menschenaffen' (1917), dass Schimpansen (Sultan) Umwege erst nach einer Pause loesten, waehrend der kein sichtbarer Lernprozess stattfand. Die ploetzliche Loesung ('Aha-Erlebnis') interpretierte er als Einsicht — eine mentale Umstrukturierung des Problems ohne Trial-and-Error.",
        difficulty = 5,
        funFact = "Koehlers Sultanexperiment auf Teneriffa (1913-1917) gilt als Geburtsstunde der vergleichenden Kognitionsforschung. Sultan stapelte Kisten und verlaeth Stoecke, um unerreichbare Bananen zu ergreifen."
    ),

    // Q10
    Question(
        categoryId = 9,
        questionText = "Das Konzept der 'kulturellen Transmission' bei japanischen Makaken (Macaca fuscata) wurde klassisch durch das Suessu-Washima-Potato-Washing-Phaenomen belegt. Wer beobachtete und dokumentierte dieses Verhalten erstmals, und welche besondere Lernmechanik wurde dabei identifiziert?",
        answerA = "Jane Goodall; vertikale Transmission von Mutter auf Kind",
        answerB = "Kinji Imanishi & Satsue Mito; Initiierung durch ein Weibchen namens Imo, horizontale und vertikale Transmission",
        answerC = "Frans de Waal; oblique Transmission von alten Maennchen auf Jungtiere",
        answerD = "Hans Kummer; bidirektionale Transmission zwischen Altersgruppen",
        correctAnswer = 1,
        explanation = "Der japanische Primatolog Kinji Imanishi und sein Team, insbesondere Satsue Mito, beobachteten ab 1953 auf der Insel Koshima, dass das Weibchen Imo als erste Kartoffeln im Meer wusch. Das Verhalten breitete sich zunaechst horizontal (unter Gleichaltrigen) und dann vertikal (Muetter auf Nachkommen) aus — ein Paradebeispiel fuer zoologische Kulturevolution.",
        difficulty = 5,
        funFact = "Imo entwickelte auch das 'Wheat Placer Mining': Sie warf Weizen-Sand-Gemische ins Wasser, damit Sand sank und Weizen schwamm. Zwei unabhaengige Innovationen von einem Individuum sind in der Tierkulturforschung extrem selten."
    ),

    // Q11
    Question(
        categoryId = 9,
        questionText = "Welcher Messwert wird im 'Match-to-Sample' (MTS) Paradigma verwendet, um das konzeptuelle Verstaendnis von 'Gleichheit' bei Tieren zu quantifizieren, und wodurch unterscheidet er sich von simplem Diskriminationslernen?",
        answerA = "Latenzzeit zur ersten Reaktion; Diskrimination misst nur Ausweichen",
        answerB = "Transfer auf neue, untrainierte Stimuli (Transfer-Test); bei Diskrimination werden spezifische S+-S- Paare gelernt",
        answerC = "Anzahl benoetigter Trials bis Kriterium; Diskrimination erfordert mehr Trials",
        answerD = "Fehlerquote bei Distraktoren; Diskrimination erlaubt keine Distraktor-Einfuehrung",
        correctAnswer = 1,
        explanation = "Im Match-to-Sample prueft man konzeptuelles Gleichheitsverstaendnis durch Transfer-Tests: Das Tier wird mit voellig neuen Stimuli konfrontiert, die es noch nie gesehen hat. Kann es trotzdem das passende (identische) Sample-Bild waehlen, zeigt es abstraktes Gleichheitsverstaendnis. Reines Diskriminationslernen wuerde bei neuen Stimuli versagen, da nur spezifische S+-S- Assoziationen gelernt wurden.",
        difficulty = 5,
        funFact = null
    ),

    // Q12
    Question(
        categoryId = 9,
        questionText = "Was ist 'kausales Verstaendnis' im Sinne des Paradigmas von Povinelli und Mitarbeitern, und welcher Befund lieferte den entscheidenden Hinweis, dass Schimpansen kausale Zusammenhaenge moeglicherweise nicht tief verstehen?",
        answerA = "Schimpansen konnten keine Werkzeuge selektieren; sie wahlten zufaellig",
        answerB = "Schimpansen lernten Werkzeugnutzung durch Imitation, konnten aber neue mechanische Probleme nicht loesen",
        answerC = "Schimpansen wahlten ein funktionales Werkzeug nicht zuverlaessig besser als ein nicht-funktionales bei neuartigen Werkzeugpaaren",
        answerD = "Schimpansen versagten bei der Unterscheidung von Ursache und Zufallskorrelation",
        correctAnswer = 2,
        explanation = "Povinelli und Mitarbeiter (1999, 2000) zeigten in einer Reihe von Studien, dass Schimpansen zwar Werkzeuge effizient nutzen, aber bei neuartigen Werkzeugpaaren (funktional vs. nicht-funktional) nicht zuverlaessig das korrekte Werkzeug waehlen. Dies deutet darauf hin, dass sie Werkzeugnutzung eher durch Erfahrungslernen als durch tiefes kausales Verstaendnis des mechanischen Wirkprinzips erwerben.",
        difficulty = 5,
        funFact = null
    ),

    // Q13
    Question(
        categoryId = 9,
        questionText = "Metacognition bei Tieren wird oft mit dem 'Uncertainty Monitoring Paradigma' getestet. Welcher Forscher fuehrte dieses Paradigma erstmals systematisch bei Rhesusaffen und Delfinen ein?",
        answerA = "Sara Shettleworth (Universitat Toronto)",
        answerB = "J. David Smith (Universitat Buffalo)",
        answerC = "Herbert Terrace (Columbia University)",
        answerD = "Gordon Gallup Jr. (SUNY Albany)",
        correctAnswer = 1,
        explanation = "J. David Smith und seine Mitarbeiter entwickelten ab den 1990er Jahren das Uncertainty Monitoring Paradigma, in dem Tiere bei schwierigen Diskriminationsaufgaben die Option haben, die Aufgabe zu 'passen' ('escape response' / Unsicherheitstaste). Rhesusaffe Murph und Delfin Natua nutzten diese Option adaptiv — ein Indiz fuer Metakognition.",
        difficulty = 5,
        funFact = "Die Debatte darueber, ob Tiere 'echte' Metakognition haben oder nur konditionierte Unsicherheitsreize nutzen, ist bis heute eines der umstrittensten Themen der vergleichenden Kognitionsforschung."
    ),

    // Q14
    Question(
        categoryId = 9,
        questionText = "In Lorenzscher Terminologie: Was ist ein 'Erbkoordinationsakt' (Fixed Action Pattern, FAP), und durch welches Merkmal unterscheidet er sich von einem erlernten Verhalten?",
        answerA = "Er wird durch positive Verstaerkung erworben und ist modifizierbar",
        answerB = "Er ist arttypisch, stereotyp, laeuft nach Auslosung vollstaendig ab (auch ohne Schluesselreiz) und ist nicht durch Erfahrung veraendert",
        answerC = "Er tritt nur bei adulten Tieren auf und haengt von Hormonen ab",
        answerD = "Er ist ausschliesslich auf Vertebraten beschraenkt und neuronale Basis ist der Kleinhirn",
        correctAnswer = 1,
        explanation = "Erbkoordinationsakte (Fixed Action Patterns) sind nach Lorenz und Tinbergen arttypische, stereotyp ablaufende Bewegungssequenzen, die durch einen Schluesselreiz ausgeloest werden, dann aber vollstaendig ablaufen — auch wenn der Reiz entfernt wird ('vacuum activity'). Sie sind genetisch verankert und werden nicht durch individuelle Erfahrung wesentlich veraendert.",
        difficulty = 5,
        funFact = "Das klassische Beispiel ist das Ei-Rollen der Graugans: Beginnt das Weibchen, ein weggerolltes Ei mit dem Schnabel ins Nest zurueckzurollen, fuehrt es die Bewegung auch dann vollstaendig aus, wenn das Ei entfernt wird."
    ),

    // Q15
    Question(
        categoryId = 9,
        questionText = "Welche neuronale Struktur im Mittelhirn der Frosche ist direkt fuer die selektive Reaktion auf bewegliche, kleine, kontrastreiche Objekte (Beute) verantwortlich, und wie nennt man die entsprechenden spezialisierten Neurone?",
        answerA = "Cerebellum; Purkinje-Zellen",
        answerB = "Tectum opticum; 'bug detector neurons' (Klasse-2-Ganglienzellen nach Lettvin)",
        answerC = "Amygdala; Beuteerkennungsneurone",
        answerD = "Hippocampus; Ortszellen (place cells)",
        correctAnswer = 1,
        explanation = "Lettvin et al. (1959) beschrieben in ihrem einflussreichen Paper 'What the frog's eye tells the frog's brain' vier Klassen von retinalen Ganglienzellen. Klasse-2-Fasern ('convex-edge detectors') reagieren selektiv auf kleine, konvexe, sich bewegende Objekte — sogenannte 'bug detectors'. Im Tectum opticum verarbeiten diese Signale Neurone, die spezifisch auf Beute-Charakteristika reagieren.",
        difficulty = 5,
        funFact = "Dieses Paper von Lettvin, Maturana, McCulloch und Pitts gilt als Grundlagenpaper der Neurophysiologie und Kybernetik — es war eines der ersten, das zeigte, dass das Nervensystem keine passive Bildverarbeitung betreibt, sondern aktiv featuren extrahiert."
    ),

    // Q16
    Question(
        categoryId = 9,
        questionText = "Das Konzept des 'Social Brain' (soziales Gehirn) wurde von welchem Forscher praesentiert, und welche Hypothese formuliert es zur Treibkraft der Enzephalisation bei Primaten?",
        answerA = "Richard Dawkins; Selektion auf Manipulation genetischer Verwandter",
        answerB = "Robin Dunbar; Selektion auf Verwaltung komplexer sozialer Beziehungen in grossen Gruppen",
        answerC = "Frans de Waal; Selektion auf Empathie und Konfliktvermeidung",
        answerD = "Michael Tomasello; Selektion auf kumulative kulturelle Transmission",
        correctAnswer = 1,
        explanation = "Robin Dunbar formulierte die Social Brain Hypothesis (auch 'Machiavellian Intelligence Hypothesis' nach Byrne & Whiten 1988): Primatenhirne sind groesser, weil die Verwaltung komplexer sozialer Beziehungen in grossen Gruppen kognitive Kapazitaet erfordert. Dunbars Zahl (~150) beschreibt die soziale Gruppe, die mit menschlicher Hirnkapazitaet 'verwaltet' werden kann.",
        difficulty = 5,
        funFact = "Dunbars Regel (150 stabile soziale Beziehungen pro Person) findet sich nicht nur in menschlichen Jaeger-Sammler-Gesellschaften, sondern auch in Unternehmensstrukturen, militaerischen Einheiten und sozialen Netzwerken."
    ),

    // Q17
    Question(
        categoryId = 9,
        questionText = "Was beschreibt das Konzept der 'Reziprozitaet' nach Trivers (1971) in Bezug auf altruistisches Verhalten bei Tieren, und welche Bedingung muss erfuellt sein, damit es evolvieren kann?",
        answerA = "Verwandte helfen sich gegenseitig; r > 0,5 nach Hamilton",
        answerB = "Nicht-Verwandte helfen sich, wenn kuenftiger Gegendienst moeglich ist; wiederholte Interaktionen ueber Zeit erforderlich",
        answerC = "Dominante Tiere helfen Subordinierten, um sozialen Status zu erhalten",
        answerD = "Tiere helfen unbekannten Artgenossen, da alle denselben Genpool teilen",
        correctAnswer = 1,
        explanation = "Robert Trivers formulierte 1971 die Reziprozitaetstheorie (reciprocal altruism): Nicht-verwandte Individuen koennen altruistisches Verhalten austauschen, wenn sie sich wiederholt begegnen und 'Betruefer' erkannt und gemieden werden koennen. Voraussetzungen sind: langfristige Beziehungen, Erkennungsmoeglichkeit des Partners, Asymmetrie der Kosten/Nutzen.",
        difficulty = 5,
        funFact = null
    ),

    // Q18
    Question(
        categoryId = 9,
        questionText = "Bei Neukaledonischen Kraehen (Corvus moneduloides) wurde ein besonders bemerkenswertes Werkzeugverhalten beschrieben. Was macht ihr Werkzeuggebrauch phylogenetisch besonders bedeutsam?",
        answerA = "Sie benutzen Steine als Haemmer — das einzige non-primate Beispiel fuer Perkussionswerkzeuge",
        answerB = "Sie fertigen Werkzeuge aus Pandanus-Blaettern mit definierten Hooked-Tool-Designs — nicht nur Nutzung, sondern Herstellung mit artspezifischem Design",
        answerC = "Sie lehren Werkzeuggebrauch aktiv ihren Nachkommen durch Demonstration",
        answerD = "Sie verwenden gleichzeitig zwei verschiedene Werkzeuge in einer koordinierten Sequenz",
        correctAnswer = 1,
        explanation = "Gavin Hunt (1996) beschrieb, dass Neukaledonische Kraehen Pandanus-Blaetter zu Haken-Werkzeugen mit drei verschiedenen Designs verarbeiten: Schmalflaechendesign, Breitflaechendesign und gestufte Breitflaechendesigns. Diese arttypischen, standardisierten Designs werden nicht individuell erfunden, sondern kulturell weitergegeben — ein Muster, das sonst nur beim Menschen bekannt ist.",
        difficulty = 5,
        funFact = "Alex Taylor zeigte in Cambridge, dass Neukaledonische Kraehe Betty spontan geraden Draht zu einem Haken bog, um Futter aus einem Rohr zu ziehen — ein Verhalten, das nie zuvor in der Literatur beschrieben worden war."
    ),

    // Q19
    Question(
        categoryId = 9,
        questionText = "Was versteht man in der Ethologie unter dem Begriff 'Anlockung durch Schluesselreize' (Supernormaler Stimulus), und welches klassische Experiment von Tinbergen illustriert dieses Phaenomen?",
        answerA = "Erfahrungsabhaengige Habituation an natuerliche Reize; Rueckkenner-Experiment bei Moewen",
        answerB = "Uebergrosse kuenstliche Modelle loesen staerkere Reaktionen aus als natuerliche Reize; Silbermoewenkueken tippen staerker auf ueberdimensionale rote Flecken",
        answerC = "Feinde werden durch Tarnung unerkennbar; Kamuflage-Experiment bei Faesanen",
        answerD = "Sexualpartner bevorzugen symmetrische Merkmale; Schmetterlings-Fluegelsymmetrie-Studie",
        correctAnswer = 1,
        explanation = "Tinbergen zeigte, dass Silbermoewen-Kueken (Larus argentatus) auf den roten Punkt am Schnabel der Eltern tippen, um Futter zu erbitten. Kuenstliche Modelle mit einem ueberdimensionalen roten Fleck oder mehreren Flecken loesten staerkere Bettelreaktionen aus als naturgetreue Elternmodelle — der supernormale Stimulus. Das Nervensystem reagiert auf die isolierte Schluesselkomponente staerker als auf den vollstaendigen natuerlichen Reiz.",
        difficulty = 5,
        funFact = "Auch Raubmoewenkueken tippen bevorzugt auf riesige, leuchtend gemusterte Schnabelmodelle. In der menschlichen Kulturforschung werden Supernormalstimuli diskutiert: Barbie-Puppen, uebergrosse Augen in Mangas oder Junkfood koennen ahnliche Mechanismen aktivieren."
    ),

    // Q20
    Question(
        categoryId = 9,
        questionText = "Welcher Begriff beschreibt die Faehigkeit eines Tieres, sich selbst im Spiegel zu erkennen (Mirror Self-Recognition, MSR), und welches Standardverfahren nach Gordon Gallup (1970) wird verwendet?",
        answerA = "Selbstwahrnehmung; das Tier schaut in den Spiegel und reagiert auf sein eigenes Spiegelbild mit Exploration",
        answerB = "Spiegelantworttest; das Tier greift ins Wasser und versucht, das Spiegelbild zu beruehren",
        answerC = "Mark-Test; ein Farbstoffmarkierung wird an schwer sichtbarer Stelle aufgetragen — das Tier beruehrt sie nur nach Spiegelkonfrontation",
        answerD = "Identitaetsstufe; das Tier erkennt Familienmitglieder im Spiegel an Lautaeusserungen",
        correctAnswer = 2,
        explanation = "Gordon Gallup (1970) entwickelte den Mark-Test (rouge test): Unter Anaesthesie wird ein geruchloses Farbstoffmarkierung an einer visuell schwer zugaenglichen Stelle des Gesichts angebracht. Nach Spiegelkonfrontation beruehren MSR-faehige Tiere die markierte Stelle an sich selbst, nicht am Spiegel — Beweis fuer Selbsterkennung. Schimpansen, Orang-Utans, Bonobo, Elefanten, Delfine und europ. Elstern bestehen diesen Test.",
        difficulty = 5,
        funFact = "Kleine Kinder bestehen den Mark-Test erst zwischen 18 und 24 Monaten. Gorillas zeigen im Mark-Test meist keine MSR, obwohl sie dem Menschen phylogenetisch nahe stehen — moeglicherweise wegen abweichender Sozialstruktur oder Scheu gegenueber Augenkontakt."
    ),

    // Q21
    Question(
        categoryId = 9,
        questionText = "In welchem Hirnbereich befinden sich die 'Spiegelneurone', die bei Makaken entdeckt wurden, und welche Funktion wird ihnen im Kontext sozialer Kognition zugeschrieben?",
        answerA = "Hippocampus CA3-Region; Raumnavigation und episodisches Gedaechtnis",
        answerB = "Praemotorischer Kortex (Area F5); Beobachtung und Ausfuehrung zweckgerichteter Handlungen werden gleichermassen kodiert",
        answerC = "Amygdala; Empathie und Angsterkennung bei Artgenossen",
        answerD = "Visueller Kortex V5 (MT); Bewegungswahrnehmung bei beobachteten Individuen",
        correctAnswer = 1,
        explanation = "Di Pellegrino, Fadiga, Fogassi, Gallese und Rizzolatti (1992) entdeckten in Area F5 des praemotorischen Kortex des Makaken Neurone, die sowohl bei eigener Ausfuehrung einer Handlung als auch beim Beobachten derselben Handlung bei einem anderen Individuum feuern. Diese 'Spiegelneurone' wurden als neurales Substrat fuer Imitation, Empathie und Theory of Mind diskutiert.",
        difficulty = 5,
        funFact = "Ramachandran bezeichnete Spiegelneurone als 'the neurons that shaped civilization'. Die urspruengliche Entdeckung war ein Zufallsfund: Die Elektrode im Makaken-Gehirn feuerte, als ein Forscher eine Erdnuss aufhob — dabei sollte das Tier sich nicht bewegen."
    ),

    // Q22
    Question(
        categoryId = 9,
        questionText = "Was ist das 'Audience Effect' Phaenomen in der Tier-Kommunikationsforschung, und durch welche Studie mit Huehnern (Gallus domesticus) wurde es klassisch demonstriert?",
        answerA = "Tiere singen lauter, wenn Artgenossen zuhoren; demonstriert bei Kanarienvogeln",
        answerB = "Hahn gibt Alarmrufe nur, wenn Hennen zugegen sind, aber nicht wenn er allein ist; Gyger, Marler & Pickert 1986",
        answerC = "Weibchen zeigen Balzverhalten nur wenn Maennchen zuschaut; Pfau-Studie",
        answerD = "Primafen zeigen soziale Lernbereitschaft nur in Gruppenanwesenheit; Whiten 1999",
        correctAnswer = 1,
        explanation = "Gyger, Marler und Pickert (1986) zeigten, dass Haushahn-Maennchen Alarmrufe (aerial predator calls) signifikant oefters absetzten, wenn Hennen anwesend waren, als wenn sie allein waren. Dieses 'Audience Effect' deutet darauf hin, dass Haehne ihr Kommunikationsverhalten an die soziale Situation anpassen — ein Zeichen fuer pragmatisch flexible Kommunikation.",
        difficulty = 5,
        funFact = null
    ),

    // Q23
    Question(
        categoryId = 9,
        questionText = "Was versteht man unter dem Konzept der 'Nischenbildung' (Niche Construction) im Kontext der Verhaltensevolution, und welches Tier gilt als prominentestes nicht-menschliches Beispiel?",
        answerA = "Tiere passen sich ihrer Nische an; Darwinfinken auf den Galapagos-Inseln",
        answerB = "Organismen veraendern durch ihr Verhalten die Selektionsumgebung fuer sich und ihre Nachkommen; Biber mit Dammbauten",
        answerC = "Tiere konkurrieren um Ressourcen und verschieben ihre Nische; Koexistenz-Studie bei Darwinfinken",
        answerD = "Symbiontische Beziehungen veraendern die Nische beider Arten; Ameisen-Akazien-Mutualismsus",
        correctAnswer = 1,
        explanation = "Niche Construction (Odling-Smee, Laland & Feldman 2003) beschreibt, wie Organismen ihre eigene selektive Umwelt aktiv modifizieren. Biber sind das klassische Beispiel: Dammbau veraendert Hydrologie, Nahrungsverfeugbarkeit und Raeuberdruck — sowohl fuer den Biber selbst als auch fuer viele andere Arten — und schafft damit vererbbare oekologische Erbschaft.",
        difficulty = 5,
        funFact = "Menschliche Landwirtschaft ist die groesste Nischenkonstruktion in der Evolutionsgeschichte. Das Konzept hat weitreichende Implikationen fuer das Verstaendnis der Koevolution von Genen, Kultur und Umwelt."
    ),

    // Q24
    Question(
        categoryId = 9,
        questionText = "Das 'Dialekt'-Phaenomen bei Zonotrichia-Amseln (White-crowned Sparrows) wurde durch Peter Marler und Mitarbeiter untersucht. Was zeigten die sensorischen Depriving-Experimente bezueglich des Gesangslernens?",
        answerA = "Tauben Voegel entwickeln artspezifischen Gesang spontan, ohne sensorisches Vorbild",
        answerB = "Isolationsaufzucht fuehrt zu einem vereinfachten 'Isolationsgesang'; Voegel brauchen akustisches Vorbild in einer sensiblen Phase, aber auch auditives Feedback beim Ueben",
        answerC = "Voegel lernen Dialekte ausschliesslich durch soziale Verstaerkung, nicht akustisches Lernen",
        answerD = "Gesang wird vollstaendig epigenetisch kodiert und benoetigt kein Lernen",
        correctAnswer = 1,
        explanation = "Marlers klassische Experimente zeigten: In Isolation aufgezogene White-crowned Sparrows entwickeln einen vereinfachten, aber arttypischen Gesang (Isolationsgesang), kein vollstaendiges Dialektvorbild. Voegel brauchen in einer sensiblen Phase (10-50 Tage) ein akustisches Vorbild (sensorische Phase) UND spaeter auditives Feedback beim Gesangsproduktion (sensomotorische Phase). Tauben (deafened) Voegel produzieren stark degradierten Gesang.",
        difficulty = 5,
        funFact = "Vogelgesanglernen ist eines der besten Tiermodelle fuer menschlichen Spracherwerb: Beide zeigen sensible Phasen, sensorische Vorlage, sensomotorisches Feedback und soziale Modulation."
    ),

    // Q25
    Question(
        categoryId = 9,
        questionText = "In welchem Jahr publizierte W.D. Hamilton seine Theorie der Verwandtenselektion (kin selection), und welche mathematische Formel (Hamiltons Regel) fasst die Bedingung zusammen, unter der altruistisches Verhalten evolviert?",
        answerA = "1964; rb > c (r = Verwandtschaftsgrad, b = Nutzen fuer Empfaenger, c = Kosten fuer Helfer)",
        answerB = "1971; rb - c > 0 nur wenn r > 0,5",
        answerC = "1955; r + b > c in stabilen Gruppen",
        answerD = "1976; nach Dawkins Revision: b/c > 1/r",
        correctAnswer = 0,
        explanation = "W.D. Hamilton publizierte 1964 in Journal of Theoretical Biology die Originalarbeiten 'The genetical evolution of social behaviour I & II'. Hamiltons Regel: rb > c — altruistisches Verhalten evolviert, wenn der Verwandtschaftsgrad (r) multipliziert mit dem Nutzen fuer den Empfaenger (b) groesser ist als die Kosten (c) fuer den Helfer. Fuer vollgeschwister gilt r = 0,5.",
        difficulty = 5,
        funFact = "J.B.S. Haldane soll schon vor Hamilton gesagt haben: 'I would lay down my life for two brothers or eight cousins' — genau die Zahlen, die sich aus Hamiltons Formel ergeben."
    ),

    // Q26
    Question(
        categoryId = 9,
        questionText = "Was ist die 'Handicap-Theorie' nach Amotz Zahavi (1975), und auf welchem evolutionaren Mechanismus beruht die Ehrlichkeit aufwendiger Signale?",
        answerA = "Kostspielige Signale sind nur bei Dominant-Tieren ehrlich, da diese niedrigere Kosten haben",
        answerB = "Aufwendige Signale (Handicaps) sind ehrlich, weil ihre Produktionskosten nur von hochwertigen Individuen getragen werden koennen — Qualitaet garantiert Ehrlichkeit",
        answerC = "Signale evolvieren nur dann, wenn Sender und Empfaenger verwandt sind und gemeinsame Interessen haben",
        answerD = "Teuer Signale evolvieren durch sexuelle Koevolution (runaway selection) ohne direkten Qualitaetsbezug",
        correctAnswer = 1,
        explanation = "Zahavis Handicap-Prinzip besagt: Nur ehrliche Signale koennen in Stabilitaet erhalten bleiben. Ein aufwendiges Merkmal (z.B. langer Pfauenschwanz) ist ein ehrliches Signal fuer Qualitaet, weil nur hochwertige Individuen die biologischen Kosten (Raeuberrisiko, Energieaufwand) tragen koennen. Mindere Individuen, die das Signal faelschen, werden staerker bestraft.",
        difficulty = 5,
        funFact = "Zahavi war jahrzehntelang umstritten — viele Evolutionsbiologen lehnten das Handicap-Prinzip ab. Erst Grafens formale Modellierung (1990) zeigte, dass es unter realistischen Bedingungen mathematisch stabil ist."
    ),

    // Q27
    Question(
        categoryId = 9,
        questionText = "Was ist 'prospektives Denken' (mental time travel forward) bei Tieren, und welche Studie mit Westernscrub-Jays (Aphelocoma californica) lieferte den staerksten Beweis?",
        answerA = "Tiere reagieren auf zukuenftige Nahrungsknappheit durch erhoehte Fetteinlagerung; metabolische Studie",
        answerB = "Haehers cachten mehr Futter in Abteilen, wo sie am naechsten Morgen hungrig sein wuerden (kein Fruehstueck erhalten), im Vergleich zu Abteilen mit Fruehstueck — Raby et al. 2007",
        answerC = "Schimpansen planten werkzeugnutzung fuer den naechsten Tag durch Aufbewahrung des Werkzeugs",
        answerD = "Ratten zeigten Vorahnungen zukuenftiger Futterpositionen in Hippocampus-Replay-Sequenzen",
        correctAnswer = 1,
        explanation = "Raby, Alexis, Dickinson und Clayton (2007, Nature) zeigten: Western scrub-jays, die morgens abwechselnd in 'No-Breakfast' und 'Breakfast'-Abteilen gehalten wurden, cachten abends mehr Futter in den No-Breakfast-Abteilen — als ob sie antizipierten, dort hungrig zu sein. Dieses Verhalten konnte nicht durch Hunger zum Cachierzeitpunkt erklaert werden.",
        difficulty = 5,
        funFact = null
    ),

    // Q28
    Question(
        categoryId = 9,
        questionText = "Welche neuronale Struktur bei Voegelns ist der hippocampale Formation der Saeugetiere analog (aber nicht homolog), und welche Verhaltensleistung ist damit assoziiert?",
        answerA = "Archistriatum; Vokallernen und Gesangsgedaechtnis",
        answerB = "Hippocampus (medialer Vorderhirnteil); raeumliche Orientierung und Cache-Erinnerung",
        answerC = "Nucleus robustus archistriatalis; motorische Gesangsprogramme",
        answerD = "Nidopallium caudolaterale; exekutive Kontrolle und Werkzeugplanung",
        correctAnswer = 1,
        explanation = "Der aviare Hippocampus (medialer Teil des Vorderhirns) ist in Struktur, neuronalen Typen (Koernerzellen, Moosfasern) und Funktion dem Saeugetier-Hippocampus analog. Voegel mit grossen Cache-Vorraeten (wie Tannenmeisen oder Buschhaehers) haben einen relativ groesseren Hippocampus. Laesionen fuehren zu Defiziten bei raeumlicher Orientierung und Cache-Abruf.",
        difficulty = 5,
        funFact = "Saisonale Veraenderungen im Hippocampusvolumen wurden bei Chickadees gefunden: Im Herbst (Cache-Anlage-Saison) ist der Hippocampus bis zu 30% groesser als im Fruehling — ein Paradebeispiel fuer neuronale Plastizitaet."
    ),

    // Q29
    Question(
        categoryId = 9,
        questionText = "Was ist das 'Inequity Aversion' Phaenomen bei Tieren, und durch welche bahnbrechende Studie wurde es erstmals bei Kapuzineraffen (Cebus apella) nachgewiesen?",
        answerA = "Tiere lehnen gleichwertige Tauschgeschaefte ab, wenn Partner mehr erhaelt; Brosnan & de Waal 2003",
        answerB = "Tiere verweigern Futter, wenn sie weniger attraktive Belohnung erhalten als Artgenosse fuer gleiche Leistung; Brosnan & de Waal 2003",
        answerC = "Dominante Tiere bestrafen Subordinierte bei unfairer Ressourcenverteilung; de Waal 1996",
        answerD = "Tiere teilen Futter proportional zu Verwandtschaftsgrad; Hamilton & Trivers Studie",
        correctAnswer = 1,
        explanation = "Brosnan und de Waal (2003, Nature) trainierten Kapuzineraffen, Steine gegen Futter zu tauschen. Erhalte ein Affe fuer dieselbe Leistung Gurke, waehrend ein Partner Traube bekam, verweigerten Affen die Gurke und warfen sie weg. Diese 'Ungleichheitsaversion' war robust und galt als erster Beleg fuer ein Gerechtigkeitsgefuehl bei nicht-menschlichen Tieren.",
        difficulty = 5,
        funFact = "Frans de Waals TED-Talk zu diesem Experiment wurde millionenfach angesehen. Spaeter zeigte sich, dass auch Hunde Ungleichheit erkennen — allerdings nur bei bekannten Partnern."
    ),

    // Q30
    Question(
        categoryId = 9,
        questionText = "Was beschreibt der Begriff 'conditional cooperation' im Kontext des Iterierten Gefangenendilemmas (repeated Prisoner's Dilemma), und welche einfachste Strategie erwies sich in Axelrods Turnier als robust?",
        answerA = "Kooperation haengt von Ressourcenabundanz ab; dominante Strategie: 'Always cooperate'",
        answerB = "Individuen kooperieren nur, wenn sie zuvor kooperiert wurden; robusteste Strategie: 'Tit for Tat' (TfT)",
        answerC = "Kooperation evolviert nur in verwandten Gruppen; dominante Strategie: 'Kin preference'",
        answerD = "Konditionelle Kooperation haengt von Gruppengroesse ab; robusteste Strategie: 'Pavlov' (Win-Stay-Lose-Shift)",
        correctAnswer = 1,
        explanation = "Robert Axelrods Computerturnier (1984) zeigte, dass 'Tit for Tat' (TfT) — beginnend mit Kooperation, dann spiegelbildlich den letzten Zug des Partners wiederholen — in iterativen Gefangenendilemma-Turnieren konsistent gewann. TfT ist nett (beginnt kooperativ), provozierbar (reagiert auf Defektion) und vergebend (kehrt nach Restitution zur Kooperation zurueck).",
        difficulty = 5,
        funFact = "Axelrods Buch 'The Evolution of Cooperation' (1984) wurde zu einem der meistzitierten Werke in Evolutionsbiologie, Politikwissenschaft und Wirtschaft gleichermassen."
    ),

    // Q31
    Question(
        categoryId = 9,
        questionText = "Was ist 'Emulation' im Kontext des sozialen Lernens (nach Tomasello), und wie unterscheidet sie sich von 'Imitation'?",
        answerA = "Emulation ist Lernen durch blosse Beobachtung ohne Modell; Imitation erfordert direkten Kontakt",
        answerB = "Emulation ist Lernen der Ziel-Umwelt-Beziehung (Ergebnisse); Imitation kopiert die exakten motorischen Handlungen des Modells",
        answerC = "Emulation betrifft nur Vokalkommunikation; Imitation betrifft motorische Verhaltensweisen",
        answerD = "Emulation ist schneller aber ungenauer; Imitation ist langsamer aber praeziser",
        correctAnswer = 1,
        explanation = "Michael Tomasello (1990er) unterschied: Emulation (goal emulation) — das Tier lernt, welche Umweltveraenderungen durch eine Handlung erzielt werden, und reproduziert das Ergebnis mit eigenen Mitteln. Imitation — das Tier kopiert die spezifischen motorischen Aktionen des Modells. Schimpansen zeigen ueberwiegend Emulation, menschliche Kinder echte Imitation ('over-imitation' inklusive irrelevanter Schritte).",
        difficulty = 5,
        funFact = "Kinder imitieren sogar kausal irrelevante Schritte ('over-imitation'), was als Hinweis auf sozialen Konformismus interpretiert wird. Schimpansen lassen diese Schritte clever aus."
    ),

    // Q32
    Question(
        categoryId = 9,
        questionText = "Das Konzept der 'Kumulativen Kulturevolution' (cumulative cultural evolution, CCE) ist zentral fuer das Verstaendnis menschlicher Kultur. Was sind die zwei Hauptvoraussetzungen nach Tomasello, die CCE bei Tieren selten macht?",
        answerA = "Sprache und Werkzeuggebrauch",
        answerB = "Echte Imitation (fidelity) und Lehren (teaching) — ohne diese koennen Innovationen nicht akkurat genug weitergegeben werden, um auf ihnen aufzubauen",
        answerC = "Gruppengroesse und Sesshaftigkeit",
        answerD = "Schriftliche Ueberlieferung und institutionelles Gedaechtnis",
        correctAnswer = 1,
        explanation = "Tomasello (1999) argumentierte, dass CCE — das 'Ratschen-Effekt' (ratchet effect) der Kulturevolution — echte Imitation und aktives Lehren erfordert. Ohne hohe Uebernahmetreue degradiert jede Innovation im Laufe von Generationen. Schimpansen-Kultur bleibt statisch, weil Emulation (nicht Imitation) die primae Lernmode ist und aktives Lehren selten vorkommt.",
        difficulty = 5,
        funFact = null
    ),

    // Q33
    Question(
        categoryId = 9,
        questionText = "In der Forschung zur Tiersprache: Welches war das erste Tier ausserhalb der Primaten, das nachweislich arbitraere symbolische Kommunikation erlernte, und in welcher Studie?",
        answerA = "Delfin Phoenix; Herman et al. 1984 — acoustische Symbol-Syntax",
        answerB = "Hund Rico; Kaminski et al. 2004 — Fast Mapping von Wortneuheiten",
        answerC = "Papagei Alex; Pepperberg ab 1977 — phonetische Vokallernkette",
        answerD = "Schimpanse Washoe; Gardner & Gardner 1969 — ASL Gebaerdensprache",
        correctAnswer = 0,
        explanation = "Louis Herman und Mitarbeiter (1984, Cognition) trainierten den Delfin Phoenix mit akustischen Symbolen (Pfeiftone), die in syntaktischen Regeln kombiniert wurden. Phoenix verstand Saetze mit bis zu 5 Symbolen inklusive Subjekt-Verb-Objekt-Struktur und demonstrierte damit symbolisch-syntaktisches Verstaendnis. Dies war der erste rigorose Nachweis ausserhalb der Primaten.",
        difficulty = 5,
        funFact = "Der andere Delfin Akeakamai lernte eine gestenbasierte Sprache. Bemerkenswert: Delfine verstanden umgekehrte Satzstruktur ('fetch the ball to the hoop' vs. 'fetch the hoop to the ball') korrekt — Belege fuer Syntaxverstaendnis."
    ),

    // Q34
    Question(
        categoryId = 9,
        questionText = "Was ist 'kooperatives Jagen' (cooperative hunting) im ethologischen Sinn, und welche Tierart gilt als das klarste nicht-menschliche Beispiel fuer taktisch koordiniertes Gruppenangriff?",
        answerA = "Lowen jagen gemeinsam auf Bueffelherde — klassische Ambush-Taktik ohne Rollenspezifikation",
        answerB = "Schwertwal (Orcinus orca) Pods verwenden koordinierte Wellenschlagtaktiken zum Eisfloesen-Kippen mit klaren Rollen (Ausguck, Taucher, Blocker)",
        answerC = "Schimpansen beim Stummelaffen-Jagen zeigen individuelle Rollen (Blocker, Treiber, Faenger, Wachter)",
        answerD = "Wolfrudel beim Elchjagen verwenden sequentielle Erschoepfungsstrategie mit Relaiswechsel",
        correctAnswer = 2,
        explanation = "Boesch und Boesch (1989, Animal Behaviour) beschrieben bei Tai-Forest-Schimpansen beim Jagen von Roten Stummelaffen (Colobus badius) vier spezialisierte Rollen: Fahrer/Treiber, Blocker, Jaeger/Chaser und Wachter/Ambusher. Diese Rollen werden individuell beibehalten und koordiniert eingesetzt — dies gilt als staerkster Beleg fuer taktische Koordination ausserhalb des Menschen.",
        difficulty = 5,
        funFact = "Die Beute wird unter Tai-Schimpansen sozial verteilt, auch an Individuen, die nicht direkt gejagt haben, wenn diese eine Rolle uebernahmen — ein Zeichen fuer Reziprozitaet im Jagdkontext."
    ),

    // Q35
    Question(
        categoryId = 9,
        questionText = "Was ist der 'Two-Action Test' in der Forschung zum sozialen Lernen, und warum ist er methodisch ueberlegen gegenueber einfachen Demonstrationsexperimenten?",
        answerA = "Er misst Lernrate bei zwei verschiedenen Verstaerkern; ueberlegen weil er Motivationseffekte kontrolliert",
        answerB = "Er testet zwei verschiedene Loesungsmethoden fuer dasselbe Ziel; Observer zeigen modellspezifische Methode, was Imitation vs. Emulation differenziert",
        answerC = "Er vergleicht soziales vs. assoziatives Lernen; ueberlegen durch Kontrollgruppe ohne Modell",
        answerD = "Er nutzt zwei Modelle gleichzeitig; ueberlegen weil er Social Facilitation ausschliesst",
        correctAnswer = 1,
        explanation = "Im Two-Action Test (Dawson & Foss 1965, weiterentwickelt durch Heyes) bekommen verschiedene Observer-Gruppen Demonstrationen der gleichen Aufgabe aber mit verschiedenen Methoden (z.B. Hebeldrehen mit Mund vs. mit Fuss). Wenn Observer die modellspezifische Methode zeigen, beweist dies Imitation — nicht nur Emulation oder Social Facilitation, da das Ziel identisch ist.",
        difficulty = 5,
        funFact = null
    ),

    // Q36
    Question(
        categoryId = 9,
        questionText = "In der klassischen Ethologie: Was ist eine 'Innate Releasing Mechanism' (IRM, angeborener ausloesender Mechanismus), und durch welches Experiment mit dem Stichling (Gasterosteus aculeatus) illustrierte Tinbergen das Konzept?",
        answerA = "Erlernter Filter fuer Arterkennungsmerkmale; Weibchen reagieren nur auf Balzfarbe der eigenen Art",
        answerB = "Hypothetischer neuraler Filter, der artspezifisch nur auf Schluesselreize anspricht; Maennchen griffen rote Puppenmodelle an, reagierten aber kaum auf lebensgetreue Modelle ohne Rot",
        answerC = "Hormoneller Mechanismus, der Brutverhalten triggert; Prolaktin loest Nestbau aus",
        answerD = "Konditionierter Ausloeser, der durch wiederholte Exposition verstaerkt wird; Maennchen lernten kuenstliche Objekte zu attackieren",
        correctAnswer = 1,
        explanation = "Tinbergen zeigte, dass Stichlings-Maennchen in der Laichsaison andere Maennchen (rote Bauchfaerbung = Territorialsignal) aggressiv angreifen. Modelle ohne rote Unterseite — selbst lebensechte Stichlingsmodelle — loesten kaum Angriff aus. Einfache rote Klumpen wurden heftig attackiert. Dies bewies den IRM: Der neurale Filter reagiert spezifisch auf das Schluesselreizerkmal Rot.",
        difficulty = 5,
        funFact = "Tinbergen beobachtete, dass seine Stichlingsbecken im Fenster aufgestellte Hauser mit roten Lastwagen dazu veranlassten, dass Maennchen an der Scheibe attackierten — die Lastwagen von aussen troffen den IRM genauso wie rivalisierende Maennchen."
    ),

    // Q37
    Question(
        categoryId = 9,
        questionText = "Welches Paradigma nutzt man, um 'causal reasoning' von 'statistical reasoning' bei Tieren zu unterscheiden, und was zeigte Pepperbergs Arbeit mit Graupapagei Alex in diesem Kontext?",
        answerA = "Transitives Schliessen; Alex loeste transitive Inferenz besser als Schimpansen",
        answerB = "Interventionistisches Kausalitatstests (do-calculus Paradigma); Alex konnte kausale Interventionen und blosse Korrelationen unterscheiden",
        answerC = "Object permanence Aufgaben; Alex bestand A-nicht-B-Test auf Niveau 6-monatiger Saeuglinge",
        answerD = "Analogy reasoning; Alex konnte visuell-abstrakte Analogien wie 'gleich:verschieden' auf neue Stimuli generalisieren",
        correctAnswer = 3,
        explanation = "Pepperberg (1987) trainierte Alex auf 'Same/Different'-Konzepte und testete dann Analogieschluss: Alex bekam Paare von Objekten und sollte beurteilen, ob sie gleich oder verschieden waren — bezueglich Farbe, Form oder Material. Dann erhielt er Analogie-Fragen: 'What's same?' Er antwortete korrekt auf voellig neue Stimuli, was abstraktes Relationsdenken (analogical reasoning) demonstrierte.",
        difficulty = 5,
        funFact = "Alex' letzte Worte zu Pepperberg am Abend vor seinem Tod (2007) sollen gewesen sein: 'You be good, I love you.' — ein Satz, den er routinemaessig am Ende jedes Arbeitstags sagte."
    ),

    // Q38
    Question(
        categoryId = 9,
        questionText = "Was ist 'Soziale Transmission von Nahrungsvorlieben' (social transmission of food preferences) in der Ratte, und welcher Neurotransmitter-Mechanismus wurde im olfaktorischen System dafuer identifiziert?",
        answerA = "Ratten lernen Futterpraeferenz durch gemeinsames Fressen; Dopamin-Belohnungssystem im Nucleus accumbens",
        answerB = "Ratten entwickeln Praeferenzen fuer Futter, das ein 'Demonstrator-Artgenosse' gefressen hat, ueber Kohlenstoffdisulfid-Markierung in Atem; piriformer Kortex und olfaktorische Bulbi",
        answerC = "Ratten meiden Futter, das kranke Artgenossen gefressen haben; Serotonin-Aversions-Signale im Amygdala",
        answerD = "Ratten imitieren Futtererwerb durch Mirror-Neuron-analoge Systeme; motorischer Kortex",
        correctAnswer = 1,
        explanation = "Galef und Wigmore (1983) zeigten, dass Beobachterratten eine Praeferenz fuer Futter erwerben, das ein Demonstrator-Artgenosse gefressen hatte. Spaetere Forschung identifizierte, dass Kohlenstoffdisulfid (CS2) im Atem des Demonstrators als Traegersubstanz dient — kombiniert mit dem Futtergeruch loest es im piriformen Kortex und olfaktorischen System eine Praeferenz aus. CS2 allein hat keine Wirkung.",
        difficulty = 5,
        funFact = null
    ),

    // Q39
    Question(
        categoryId = 9,
        questionText = "Was versteht man in der Verhaltensforschung unter 'Tandem Running' (Tandemlaufen) bei Ameisen, und was macht es aus kognitiver Sicht besonders interessant?",
        answerA = "Ameisen kommunizieren Futterquellen durch Pheromonspur ohne visuelle Fuehrung",
        answerB = "Eine erfahrene Ameise fuehrt eine unerfahrene aktiv und bidirektional zur Futterquelle — die Fuehrerin passt Tempo dem Schuelertempo an; Belege fuer aktives Lehren bei Tieren",
        answerC = "Koeniginnen fuehren Arbeiterinnen bei Schwarmgruendung durch koordiniertes Laufverhalten",
        answerD = "Ameisenpfade entstehen durch iterative positive Rueckkopplung ohne individuelle Fuehrung",
        correctAnswer = 1,
        explanation = "Franks und Richardson (2006, Nature) beschrieben Tandem Running bei der Ameise Temnothorax albipennis: Eine erfahrene Ameise (Fuehrerin) fuehrt eine unerfahrene (Schuelerin) durch eine neue Nestumgebung. Die Fuehrerin stoppt regelmaessig und wartet, bis die Schuelerin sie beruehrt und signalisiert, bereit zu sein. Die Fuehrerin passt ihr Tempo an. Dies wurde als erstes klares Beispiel fuer aktives Lehren (teaching) bei einem Insekt interpretiert.",
        difficulty = 5,
        funFact = "Tandem Running ist langsamer als alleine laufen — die Fuehrerin erleidet Kosten (Zeit) zugunsten der Schuelerin (Wissensgewinn), was ein Kriterium fuer 'teaching' nach Caro & Hauser erfuellt."
    ),

    // Q40
    Question(
        categoryId = 9,
        questionText = "In der vergleichenden Kognitionsforschung: Was ist das 'Gaze Following' und 'Joint Attention', und bei welchen Tieren wurde echte Joint Attention (nicht nur Blickverfolgung) nachgewiesen?",
        answerA = "Blickverfolgung und geteilte Aufmerksamkeit; nachgewiesen bei allen Primaten und Hunden",
        answerB = "Blickverfolgung ist das Verfolgen der Blickrichtung; Joint Attention erfordert zusaetzlich das Referenzieren zurueck zum Partner. Echte JA wurde nur bei Menschenaffen und bestimmten Voegeln (Kraehenartige) robust nachgewiesen",
        answerC = "Blickverfolgung entsteht durch konditionierte Orientierungsreaktion; Joint Attention ist spezifisch menschlich",
        answerD = "Gaze Following und Joint Attention sind phylogenetisch korreliert mit Hirngroesse; alle Saeugetiere zeigen es proportional",
        correctAnswer = 1,
        explanation = "Gaze Following (Blickfolgen) beschreibt das Ausrichten der Aufmerksamkeit in die Blickrichtung eines anderen. Joint Attention erfordert zusaetzlich, dass das Tier zwischen dem Objekt und dem Partner wechselt und das gemeinsame Erleben 'checkt'. Menschenaffen (Tomasello et al.) und einige Kraehenvoegelartige (Kotrschal) zeigen robuste JA, waehrend bei anderen Tieren nur Blickverfolgung belegt ist.",
        difficulty = 5,
        funFact = null
    ),

    // Q41
    Question(
        categoryId = 9,
        questionText = "Was beschreibt das Konzept der 'Lateralisation' des Gehirns bei Tieren, und welche Studie zeigte Lateralisation bei Fischen (Girardinus falcatus) in sozialen Kontexten?",
        answerA = "Bevorzugte Nutzung einer Koerperseite; Lateralisation wurde erst 2000 bei Fischen entdeckt durch Ugolini",
        answerB = "Asymmetrische Hirnfunktionen fuer Verhaltenskategorien; Vallortigara & Rogers zeigten bei Kuestengrundeln und Cichliden linksseitige Praeferenz fuer soziale/feindliche Stimuli, rechts fuer Beute",
        answerC = "Handpraeferenz bei Primaten; Lateralisation ist phylogenetisch auf Wirbeltiere beschraenkt",
        answerD = "Symmetrischer Aufbau des Gehirns mit funktionaler Lateralisation nur beim Menschen",
        correctAnswer = 1,
        explanation = "Giorgio Vallortigara, Lesley Rogers und Kolleginnen zeigten, dass Lateralisation bei Wirbeltieren phylogenetisch weit verbreitet ist. Bei verschiedenen Fischarten wurden konsistente Seiten-Praeferenzen gefunden: linkes Auge (rechte Hemisphare) fuer soziale Stimuli (Artgenossen, potenzielle Feinde), rechtes Auge (linke Hemisphare) fuer Beuteidentifikation. Dies deutet auf evolutionaer konservierte Lateralisation hin.",
        difficulty = 5,
        funFact = "Beim Haushuhn ist Lateralisation durch Lichtexposition im Ei beeinflussbar: Embryonen, die rechtem Auge Licht ausgesetzt wurden, entwickeln staerkere Rechts-Praeferenz fuer Koernerselektion — ein direkter Beweis fuer erfahrungsabhaengige Hirnlateralisation."
    ),

    // Q42
    Question(
        categoryId = 9,
        questionText = "Was ist das 'Cocktail-Party-Problem' in der Bioakustik, und welches neuronale Mechanismus ermoeglicht Schlaefermaeusen (Eptesicus fuscus), einzelne Echoortungssignale im Geraeuschgemisch zu verfolgen?",
        answerA = "Selektives Gehoer; Jamming Avoidance Response (JAR) durch Frequenzverschiebung",
        answerB = "Trennung simultaner Signalquellen; neuronale Eigenfrequenz-Schaltung im inferior colliculus und auditorischen Kortex filtert Eigenechos durch Timing-Fenster (forward masking suppression)",
        answerC = "Selektive Aufmerksamkeit; dopaminerge Modulation des medialen Geniculum filtert irrelevante Signale",
        answerD = "Echoortungs-Strahlformung durch Ohrmuschelbewegung trennt raeumlich Signalquellen",
        correctAnswer = 1,
        explanation = "Fleischige Fledermaeuse loesen das Cocktail-Party-Problem (Selektives Hoeren) durch spezialisierte neuronale Mechanismen: Im inferior colliculus und auditorischen Kortex gibt es Neurone, die durch 'forward masking' — Suppression nach eigenem Ruf — ein enges Zeitfenster schaffen, in dem Echos von eigenen Rufen von Fremdrufen unterschieden werden. Kombiniert mit Frequenzmodulation erlaubt dies exaktes Zielverfolgen im Schwarmbetrieb.",
        difficulty = 5,
        funFact = "Brasilianische Freischwanzfledermaeuse (Tadarida brasiliensis) nutzen aktive Jamming-Strategien: Sie senden gezielt stoerende Rufe auf die Frequenz konkurrierender Artgenossen aus, um deren Jagderfolg zu reduzieren — ein seltenes Beispiel fuer akustische Sabotage."
    ),

    // Q43
    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen 'funktionalem Verweis' (functional reference) und 'symbolischem Verweis' (symbolic reference) in der Tierkommunikation, und bei welcher Spezies wurde beides am staerksten dokumentiert?",
        answerA = "Funktional: Signal loest spezifisches Verhalten aus; symbolisch: Signal steht unabhaengig vom Kontext fuer ein Konzept. Starkste Evidenz: Honeybee fuer funktional, Schimpansen fuer symbolisch",
        answerB = "Funktional: Signal loest spezifisches, passende Verhaltensreaktionen aus (z.B. Luft- vs. Bodenpraedator-spezifisches Fluchtverhalten); symbolisch: Signal repraesentiert Konzept kontextunabhaengig. Starkste Evidenz fuer funktional: Vervet Monkeys (Cheney & Seyfarth); fuer symbolisch: Alex (Pepperberg)",
        answerC = "Beide Konzepte sind identisch, nur Terminologie unterscheidet sich; Vervet Monkey Alarmrufe belegen beide",
        answerD = "Funktional bezieht sich auf motorische Reaktion; symbolisch auf kognitive Repraesentation — nur beim Menschen nachgewiesen",
        correctAnswer = 1,
        explanation = "Cheney und Seyfarth (1980, Science) dokumentierten bei Vervet Monkeys (Chlorocebus pygerythrus) drei verschiedene Alarmrufe fuer Adler, Leoparden und Schlangen, die jeweils artspezifische Fluchtverhaltensweisen ausloesten. Dies belegt funktionale Referenz (Signal-Bedeutung-Beziehung). Symbolische Referenz erfordert Kontextunabhaengigkeit und Kombinierbarkeit — am staerksten bei Alexs Labeldokumentation (Pepperberg) und Kanzis Lexigrammen (Savage-Rumbaugh) belegt.",
        difficulty = 5,
        funFact = "Cheney und Seyfarths Buch 'How Monkeys See the World' (1990) ist ein Klassiker der vergleichenden Kognitionsforschung. Sie spielten Playback-Aufnahmen der Alarmrufe ab, um zu testen, ob Tiere die Rufe semantisch oder nur reaktiv verarbeiten."
    ),

    // Q44
    Question(
        categoryId = 9,
        questionText = "Was sind 'place cells' im Hippocampus der Ratte (nach O'Keefe und Dostrovsky 1971), und welche Theorie resultierte aus deren Entdeckung?",
        answerA = "Neurone fuer Objekterkennung im Raume; fuehrte zur Theorie des deklarativen Gedaechtnisses",
        answerB = "Neurone die selektiv feuern, wenn das Tier sich an einem bestimmten Ort befindet; fuehrten zur Theorie des kognitiven Kartens (cognitive map theory, Tolman)",
        answerC = "Neurone fuer dreidimensionale Raumwahrnehmung; fuehrten zur Grid-Cell-Theorie von Moser",
        answerD = "Sensorische Integrationsneurone; fuehrten zur Theory of Episodic Space Memory",
        correctAnswer = 1,
        explanation = "O'Keefe und Dostrovsky (1971, Brain Research) entdeckten Neurone im Hippocampus der Ratte, die selektiv dann feuern, wenn das Tier eine spezifische Position (Place Field) im Raum einnimmt — unabhaengig von Kopfrichtung oder Bewegungsgeschwindigkeit. Dies lieferte das neuronale Substrat fuer Tolmans cognitive map und fuehrte zur Erkenntnis, dass der Hippocampus eine innere Repraesentation des Raums erstellt. O'Keefe erhielt dafuer 2014 den Nobelpreis.",
        difficulty = 5,
        funFact = "Zusammen mit O'Keefe erhielten 2014 May-Britt Moser und Edvard Moser den Nobelpreis fuer die Entdeckung der Grid Cells im entorhinalen Kortex — Neurone, die ein hexagonales Gitter-Koordinatensystem bilden und mit Place Cells das 'GPS des Gehirns' bilden."
    ),

    // Q45
    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'Social Learning Strategies' (Soziale Lernstrategien), und welche zwei Strategien identifizierten Laland und Mitarbeiter als evolutionaer besonders stabil?",
        answerA = "Copy-when-uncertain und copy-if-better; stabil weil sie Informationskosten minimieren",
        answerB = "Copy-the-majority und copy-the-successful; stabil weil sie soziale Konformitaet und meritokratische Selektion verbinden",
        answerC = "Always copy und never copy; stabil als ESS-Gleichgewicht",
        answerD = "Copy young und copy dominant; stabil in hierarchischen Gesellschaften mit stabiler Dominanzordnung",
        correctAnswer = 0,
        explanation = "Laland (2004) und Galef & Laland (2005) identifizierten durch evolutionaere Simulationen und empirische Tests: 'Copy when uncertain' (soziales Lernen bei eigenem Informationsmangel) und 'Copy if better' (soziales Lernen, wenn das Modell bessere Ergebnisse erzielt) sind besonders adaptive Strategien. Sie erlauben optimale Balance zwischen eigenem Erkunden und sozialem Lernen.",
        difficulty = 5,
        funFact = null
    ),

    // Q46
    Question(
        categoryId = 9,
        questionText = "In Tinbergens klassischen Experimenten zu Signalreizen bei Lachmoewen (Larus ridibundus): Was demonstrierte das 'Egg Removal'-Experiment bezueglich des Verhaltens der Elternvoegel beim Eierwegrollen?",
        answerA = "Moeweneltern rollen nur echte Eier zurueck — Attrappen werden ignoriert",
        answerB = "Elternvoegel rollen grosse, ueberdimensionierte Attrappen bevorzugter zurueck als echte Eier — Supernormal-Stimulus-Effekt beim Brut-Pflege-Verhalten",
        answerC = "Elternvoegel erkennen ihre eigenen Eier durch Geruch und akzeptieren keine Fremd-Eier",
        answerD = "Eierwegrollen ist ein Zufallsverhalten und haengt nicht von Eiformeigenschaften ab",
        correctAnswer = 1,
        explanation = "Tinbergens Experimente zeigten, dass Lachmoewen-Eltern kuenstliche Attrap-Eier, die groesser als echte Eier sind, bevorzugt zurueckrollen — sie bevorzugen sogar eine riesige, aufgeblasene Attrappe gegenueber dem eigenen echten Ei. Dies illustriert den Supernormalstimulus-Effekt: Das Nervensystem reagiert supranormal auf uebertriebene Schluesselreizkombinationen.",
        difficulty = 5,
        funFact = "Dasselbe Phaenomen erklaert, warum Kuckuckseier, die oft groesser sind als Wirtseier, bevorzugt bebruetet werden — der Kuckuck hat den Superstimulus-Mechanismus evolutionaer ausgenutzt."
    ),

    // Q47
    Question(
        categoryId = 9,
        questionText = "Was ist das 'Observer-Based Audience Effect' bei Schimpansen, und welche Studie von Hare, Call und Tomasello (2001) zeigte, dass Schimpansen Wissen anderer Individuen konzeptuell repraesentieren?",
        answerA = "Schimpansen handeln anders, wenn ein Forscher zuschaut, als wenn sie allein sind",
        answerB = "Subordinate Schimpansen griffen verstecktes Futter bevorzugt dann, wenn der Dominant es nicht sehen konnte — nicht wenn es fuer beide sichtbar war. Demonstriert Wissensattribution.",
        answerC = "Dominante Schimpansen teilten Futter, wenn Beobachter anwesend waren, aber nicht allein",
        answerD = "Schimpansen logen aktiv, indem sie falsche Hinweissignale nach Nahrungsverbergen gaben",
        correctAnswer = 1,
        explanation = "Hare, Call und Tomasello (2001, Science) zeigten: Wenn ein Dominanter und ein Subordinierter Schimpanse um Futter konkurrierten, griff der Subordinierte bevorzugt nach Futter, das nur er (aber nicht der Dominante) sehen konnte. Dieses Design kontrollierte fuer einfaches Situationslernen und zeigte, dass Subordinierte repraesentieren, WAS der Dominante wissen kann — ein Kernbefund fuer ToM bei Schimpansen.",
        difficulty = 5,
        funFact = "Diese Studie war methodisch innovativ, weil sie natuerliche Motiv-Asymmetrien (Dominanz-Hunger) nutzte statt kuenstlicher Laboraufgaben — ein 'natural informativeness paradigm'."
    ),

    // Q48
    Question(
        categoryId = 9,
        questionText = "Was ist Kin Recognition (Verwandtenerkennung) bei Tieren, und welche vier Mechanismen wurden von Holmes und Sherman (1982) zur Unterscheidung von Verwandten identifiziert?",
        answerA = "Erkennung durch Duft; Mechanismen: MHC-Geruch, Pheromone, akustische Signale, visuelle Zeichen",
        answerB = "Erkennung genetisch verwandter Individuen; Mechanismen: Phaenotypisches Matching (eigener Geruch als Template), assoziative Prägung (frühe Sozialpartner), Ortsproximitaet, grobe genetische Aehnlichkeitserkennung",
        answerC = "Erkennung nur von Eltern und Geschwistern; Mechanismen: Geburtsverhalten, Milch-Geruch, akustische Bindungsrufe",
        answerD = "Nur bei Eusozialinsekten — chemische Nestgeruch-Erkennung durch kuticulare Kohlenwasserstoffe",
        correctAnswer = 1,
        explanation = "Holmes und Sherman (1982) unterschieden vier Hauptmechanismen fuer Kin Recognition bei Tieren: (1) Assoziative Praegung (fruehzeitige Sozialpartner = Verwandte), (2) Phaenotypisches Matching (eigener Geruch als Template fuer Verwandtenerkennung), (3) Ortsproximitaet (Nahbereichsbewohner werden als Verwandte behandelt), (4) Direkte Genomkodierung (theoretisch — 'Greenbeard'-Effekte). Die ersten drei wurden empirisch klar belegt.",
        difficulty = 5,
        funFact = "Das Greenbeard-Gen-Konzept (Dawkins 1976) beschreibt ein Gen, das gleichzeitig ein erkennbares Merkmal erzeugt und Traeger desselben Merkmals bevorzugt behandelt. Echte Greenbeard-Effekte wurden bei Schleimpilzen (Dictyostelium discoideum) und Blattlaeusen gefunden."
    ),

    // Q49
    Question(
        categoryId = 9,
        questionText = "Was ist 'Allocentric Navigation' im Gegensatz zu 'Egocentric Navigation', und welches Experiment mit Nagetieren belegte, dass der Hippocampus speziell fuer allocentrische raeumliche Repraesentationen benoetigt wird?",
        answerA = "Allocentrisch = absolute Richtungen; egocentrisch = koerperrelative Bewegungsfolgen. Morris Water Maze mit hippocampalen Laesionen.",
        answerB = "Allocentrisch = externe Landmarkenbezug (environmental frame); egocentrisch = koerperrelative Bewegungsfolgen (self-referenced). Morris Water Maze-Experimente: Hippocampus-Laesionen beeintraechtigen allocentrische, nicht egozentrische Navigation.",
        answerC = "Allocentrisch = soziale Navigation nach Artgenossen; egocentrisch = individuelle Pfadfindung. Radialarmlabyrint-Experimente.",
        answerD = "Allocentrisch = visuelle Navigation; egocentrisch = vestibulaere Navigation. Belegt durch Vorhofapparatus-Laesionen.",
        correctAnswer = 1,
        explanation = "Richard Morris (1981) entwickelte das Morris Water Maze: Ratten schwimmen in einem runden Becken und suchen eine unsichtbare Unterwasserplattform anhand externer Landmarken (allocentrisch). Hippocampus-Laesionen fuehren zu Zufallssuchen ohne Praeferenz fuer die korrekte Position — spezifisch allocentrische Navigation ist beeintraechtigt. Cue-Navigation (egozentrisch, durch lokale Hinweisreize) bleibt erhalten.",
        difficulty = 5,
        funFact = "Das Morris Water Maze ist bis heute ein Standard-Test fuer hippocampale Funktion und wurde in Tausenden Studien eingesetzt, unter anderem zur Charakterisierung von Alzheimer-Mausmodellen."
    ),

    // Q50
    Question(
        categoryId = 9,
        questionText = "In der Forschung zur Evolution der Intelligenz: Was ist die 'Ecological Intelligence Hypothesis' im Kontrast zur 'Social Intelligence Hypothesis', und welche empirische Befundlage stuetzt welche Hypothese bei Primaten?",
        answerA = "Oekologisch: Umweltkomplexitaet treibt Intelligenz; sozial: Gruppengroesse treibt Intelligenz. Nur oekologische Evidenz robust durch Korrelationen mit Nahrungsoekologie.",
        answerB = "Oekologisch: Nahrungsgewinnung in komplexen Umgebungen (Extraktion, Zeitplanung) treibt Enzephalisation; sozial: Soziale Komplexitaet (Koalitionen, Deception) treibt Enzephalisation. Primatendaten: neokortikale Ratio korreliert staerker mit Gruppengroesse (Dunbar) als mit oekologischen Variablen.",
        answerC = "Oekologisch: Saisonale Nahrungsvariabilitaet treibt Hirnentwicklung; sozial: Sexuelle Selektion treibt Hirnentwicklung. Beide unterstuetzt durch Hirnscanstudie an 19 Primatenarten.",
        answerD = "Beide Hypothesen wurden durch Milton 1988 in einer gemeinsamen Meta-Intelligenz-Theorie vereinigt, die fruchtsuchende Primaten als Muster nutzt.",
        correctAnswer = 1,
        explanation = "Die Ecological Intelligence Hypothesis (Clutton-Brock & Harvey, Gibson) betont, dass Komplexitaet der Nahrungsgewinnung (z.B. saisonal verteilte Fruechte finden, Werkzeug nutzen) kognitiven Aufwand erfordert. Die Social Brain Hypothesis (Humphrey, Byrne & Whiten, Dunbar) betont soziale Komplexitaet. Dunbars Metaanalysen zeigten, dass neokortikale Ratio besser mit Gruppengroesse und sozialer Komplexitaet als mit oekologischen Variablen korreliert — was die soziale Hypothese bevorteilt.",
        difficulty = 5,
        funFact = "Neuere Studien (Street et al. 2017, PNAS) mit Bayesianischer Phylogenie-Analyse zeigten, dass Foraging Komplexitaet und Soziale Komplexitaet beide unabhaengig zur Hirngroesse beitragen — moeglicherweise sind beide Hypothesen komplementaer, nicht konkurrierend."
    )

)
