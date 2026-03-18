package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsExpert2(): List<Question> = listOf(

    // --- TRAININGSMETHODIK / PERIODISIERUNG (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welches Periodisierungsmodell beschreibt Tudor Bompa als 'Undulating Periodization', bei dem die Belastungsparameter innerhalb einer Woche variieren?",
        answerA = "Lineare Periodisierung",
        answerB = "Block-Periodisierung",
        answerC = "Wellenfoermige (nichtlineare) Periodisierung",
        answerD = "Konjugierte Periodisierung",
        correctAnswer = 2,
        explanation = "Die nichtlineare oder wellenfoermige Periodisierung variiert Volumen und Intensitaet innerhalb kurzer Zeitraeume (Tage oder Wochen), was gegenueber der linearen Methode Plateaus vermeiden soll.",
        difficulty = 4,
        funFact = "Das Konzept der wellenfoermigen Periodisierung wurde urspruenglich in der osteuropaeischen Sportwissenschaft der 1960er Jahre entwickelt und erst spaeter im Westen bekannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher sowjetische Sportwissenschaftler entwickelte in den 1960er Jahren das Konzept der 'potenzierenden Wirkung' konzentrierter Kraftbelastungen auf Schnellkraftleistungen, bekannt als 'Konjugierter Sequenz-Ansatz'?",
        answerA = "Anatolij Bondartschuk",
        answerB = "Juri Werchoshanskij",
        answerC = "Lew Matwejew",
        answerD = "Wladimir Platonow",
        correctAnswer = 1,
        explanation = "Juri Werchoshanskij (auch Verkhoshansky) entwickelte den 'Conjugate Sequence System'-Ansatz, der konzentrierte Kraftphasen nutzt, um nachfolgende Schnellkraftleistungen durch residuale Trainingseffekte zu potenzieren.",
        difficulty = 4,
        funFact = "Werchoshanskij gilt als 'Vater des Plyometrics' und seine Methoden wurden von Elitesportlern weltweit uebernommen, auch wenn sein Name ausserhalb der Sportwissenschaft kaum bekannt ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Das Konzept der 'Superkompensation' setzt voraus, dass zwischen Belastung und Wiederherstellung ein optimales Zeitfenster eingehalten wird. Wie nennt man den Zustand, wenn die naechste Trainingsbelastung vor vollstaendiger Erholung erfolgt?",
        answerA = "Overreaching",
        answerB = "Detraining",
        answerC = "Monotonie-Syndrom",
        answerD = "Akkumulations-Plateau",
        correctAnswer = 0,
        explanation = "Funktionelles Overreaching ist eine kurzfristige Ueberbelastung, die nach ausreichender Erholung zu Superkompensation fuehrt. Nicht-funktionelles Overreaching hingegen kann Wochen dauern und in Uebertraining muenden.",
        difficulty = 4,
        funFact = "Die Grenze zwischen funktionellem Overreaching und Uebertraining ist fliessend; Biomarker wie Testosteron/Kortisol-Quotient oder Herzratenvariabilitaet helfen Trainern, diese Grenze zu erkennen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Begriff beschreibt im Trainingsmodell von Issurin die kurzen, hochspezialisierten Trainingsabschnitte von 2-6 Wochen mit konzentrierter Belastung einer dominanten Faehigkeit?",
        answerA = "Mesozyklus",
        answerB = "Akkumulationsblock",
        answerC = "Makrozyklus",
        answerD = "Transitphase",
        correctAnswer = 1,
        explanation = "Vladimir Issurin's Block-Periodisierung besteht aus Akkumulations-, Transmutations- und Realisierungssbloecken. Der Akkumulationsblock entwickelt die aerobe Basis und allgemeine Faehigkeiten.",
        difficulty = 4,
        funFact = "Issurins Block-Periodisierung wurde urspruenglich fuer Hochleistungsschwimmer entwickelt und findet heute in fast allen Ausdauer- und Kraftsportarten Anwendung."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter der 'Potenzierungskapazitaet' (Post-Activation Potentiation, PAP) im Hochleistungstraining?",
        answerA = "Langfristige Muskelhypertrophie nach schwerem Training",
        answerB = "Kurzfristige Leistungssteigerung durch vorangehende maximale Muskelkontraktion",
        answerC = "Erholung durch aktive Regenerationsmassnahmen",
        answerD = "Neuronale Anpassung durch wiederholte Belastung",
        correctAnswer = 1,
        explanation = "PAP bezeichnet die kurzfristige Steigerung der Muskelkontraktionsgeschwindigkeit und -kraft nach einer vorangehenden maximalen oder submaximalen Kontraktion durch veraenderte Myosin-Leichtketten-Phosphorylierung.",
        difficulty = 4,
        funFact = "Sprinttrainer setzen PAP gezielt ein: Ein schwerer Kniebeugen-Satz vor Sprintlaeufen kann die Sprintzeit um 1-3% verbessern, wenn das optimale Intervall von 7-12 Minuten eingehalten wird."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Trainingsmethode basiert auf der gezielten Erschoepfung von Typ-I-Fasern vor der Hauptuebung, um eine verstaerkte Rekrutierung von Typ-II-Fasern zu erzwingen?",
        answerA = "Drop-Set-Methode",
        answerB = "Pre-Exhaustion-Methode",
        answerC = "Konzentrische Ueberlastung",
        answerD = "Okklusions-Training (KAATSU)",
        correctAnswer = 1,
        explanation = "Bei der Pre-Exhaustion-Methode wird zuerst eine Isolationsuebung zur Erschoepfung des Zielmuslels durchgefuehrt, bevor eine Verbunduebung folgt. Dies soll die Aktivierung hochschwelliger Typ-II-Fasern erhoehen.",
        difficulty = 4,
        funFact = "Das KAATSU- oder Blutfluss-Restriktions-Training erzielt aehnliche Effekte bei nur 20-30% des Trainingsgewichts, indem venoeser Abfluss gedrosselt wird und metabolischer Stress Typ-II-Fasern rekrutiert."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Sportwissenschaftler pragte den Begriff 'Generalizable Repeated Bout Effect' (RBE) und beschrieb damit die verringerte Muskelschaedigungsreaktion bei wiederholten exzentrischen Belastungen?",
        answerA = "Vladimir Zatsiorsky",
        answerB = "Priscilla Clarkson",
        answerC = "Ken Nosaka",
        answerD = "Tim Noakes",
        correctAnswer = 2,
        explanation = "Ken Nosaka von der Edith Cowan University gilt als fuehrender Forscher zum Repeated Bout Effect. Dieser Schutzmechanismus erklaert, warum Muskelkater nach wiederholten exzentrischen Belastungen nachlasst.",
        difficulty = 4,
        funFact = "Der RBE tritt bereits nach einer einzigen exzentrischen Trainingseinheit auf und haelt bis zu 6 Monate an, obwohl die genauen Mechanismen (neuronale vs. mechanische Anpassungen) noch diskutiert werden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt die optimale Trainingsfrequenz pro Muskelgruppe, bei der die Proteinsynthese-Rate maximiert wird, ohne kumulativen Muskelschaden zu verursachen?",
        answerA = "Maximum Adaptive Volume (MAV)",
        answerB = "Minimum Effective Volume (MEV)",
        answerC = "Maximum Recoverable Volume (MRV)",
        answerD = "Hypertrophy-Frequency-Threshold (HFT)",
        correctAnswer = 0,
        explanation = "Das Maximum Adaptive Volume (MAV), popularisiert von Mike Israetel, beschreibt das optimale Trainingsvolumen pro Muskelgruppe fuer maximalen Hypertrophiereiz ohne Uebertraining. Es liegt individuell verschieden.",
        difficulty = 4,
        funFact = "Renaissance Periodization (RP) von Mike Israetel hat das Volume-Landmark-Konzept (MEV, MAV, MRV) in der Trainingswelt popularisiert, obwohl es wissenschaftlich noch keine einheitliche Definition gibt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der 'Interference Effect' (auch: Konkurrenzeffekt) im kombinierten Kraft-Ausdauer-Training?",
        answerA = "Positive Wechselwirkung zwischen Kraft- und Ausdauertraining",
        answerB = "Hemmung von Kraftzuwaechsen durch paralleles Ausdauertraining via AMPK-mTOR-Antagonismus",
        answerC = "Steigerung der VO2max durch Krafttraining",
        answerD = "Konflikt zwischen Trainingszeit und Erholung im Wettkampfkalender",
        correctAnswer = 1,
        explanation = "Der Interference Effect beschreibt die Hemmung von Muskelhypertrophie und Kraftzuwaechsen durch paralleles Ausdauertraining. AMPK (aktiviert durch Ausdauer) hemmt mTOR (Schluessel-Signalweg fuer Hypertrophie).",
        difficulty = 4,
        funFact = "Robert Hickson beschrieb den Interference Effect erstmals 1980. Neuere Forschung zeigt, dass zeitlicher Abstand (>6 Std.) und Art des Ausdauertrainings (Radfahren hemmt Beinkraft weniger als Laufen) den Effekt modulieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt das Phaenomen, dass hochtrainierte Athleten auf identische Trainingsreize geringere Anpassungsreaktionen zeigen als Untrainierte?",
        answerA = "Diminishing Returns Principle",
        answerB = "Adaptation Reserve Capacity",
        answerC = "Trainability Ceiling",
        answerD = "Accommodation Principle",
        correctAnswer = 3,
        explanation = "Das Akkommodationsprinzip (nach Verkhoshansky) besagt, dass unveraenderte Trainingsreize zunehmend wirkungsloser werden. Eliteathleten brauchen groessere und variiertere Reize, um weitere Anpassungen auszuloesen.",
        difficulty = 4,
        funFact = "Weshalb Weltklasse-Sprinter manchmal schlechter trainieren als B-Kader-Athleten: Ihre Akkommodation an hohe Belastungen macht es schwieriger, noch steigerungsfaehige Reize zu setzen."
    ),

    // --- SPORTPHYSIOLOGIE FORTGESCHRITTEN (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welcher Enzymkomplex ist der zentrale Regulator der mitochondrialen Biogenese und wird durch Ausdauertraining aktiviert?",
        answerA = "mTORC1",
        answerB = "PGC-1alpha",
        answerC = "FOXO3a",
        answerD = "Calcineurin",
        correctAnswer = 1,
        explanation = "PGC-1alpha (Peroxisome Proliferator-Activated Receptor Gamma Coactivator 1-alpha) ist der Hauptregulator der mitochondrialen Biogenese. Ausdauertraining aktiviert PGC-1alpha via AMPK und p38-MAPK.",
        difficulty = 4,
        funFact = "PGC-1alpha reguliert nicht nur Mitochondrien, sondern auch Angiogenese, Glukosestoffwechsel und sogar kognitive Funktion, weshalb Forscher es als potenziellen Ansatz fuer neurodegenerative Erkrankungen untersuchen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche physiologische Adaptation erklaert die hoehere Fettsaeureoxidationsrate trainierter Ausdauersportler bei gleicher absoluter Belastungsintensitaet?",
        answerA = "Hoeherer Testosteronspiegel",
        answerB = "Dichtere Kapillarnetzwerke im Muskel",
        answerC = "Erhoehte Aktivitaet von Fettsaeureoxidations-Enzymen und vermehrte Mitochodrien",
        answerD = "Groesseres Herzschlagvolumen",
        correctAnswer = 2,
        explanation = "Training erhoht die Aktivitaet von Schluessel-Enzymen der Beta-Oxidation (z.B. HAD, CS) und verdoppelt bis verdreifacht die Mitochondriendichte. Dies ermoeglicht hoehere Fettsaeureoxidation und spart Glykogen.",
        difficulty = 4,
        funFact = "Hochtrainierte Ausdauersportler können bei moderater Intensitaet fast ausschliesslich Fett verbrennen, waehrend Untrainierte bei gleichem Tempo bereits stark auf Kohlenhydrate angewiesen sind."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die physiologische Ursache des Phänomens 'Cardiac Drift' bei laengeren Ausdauerbelastungen in der Hitze?",
        answerA = "Zunehmende Azidose hemmt den Sinusknoten",
        answerB = "Progressiver Blutvolumenverlust durch Schweiss senkt den Schlagvolumen, was die HF kompensatorisch ansteigt",
        answerC = "Laktat hemmt die Herzmuskelkontraktion",
        answerD = "Katecholamine saturieren Beta-Rezeptoren",
        correctAnswer = 1,
        explanation = "Cardiac Drift bezeichnet den progressiven Herzfrequenzanstieg bei konstanter Belastung. Dehydration vermindert das Blutvolumen, das Schlagvolumen sinkt, und der Koerper kompensiert durch hoehere Herzfrequenz (Frank-Starling-Mechanismus versagt).",
        difficulty = 4,
        funFact = "Gut trainierte Ausdauersportler haben ein um 20-30% groesseres Plasmavolumen als Untrainierte, was den Cardiac Drift verzoegert und die Hitzetoleranz verbessert."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Transportmechanismus ist primaer fuer den Laktat-Shuttle zwischen Muskelfasern und Leber verantwortlich?",
        answerA = "Passive Diffusion",
        answerB = "Natrium-Kalium-ATPase",
        answerC = "Monocarboxylat-Transporter (MCT1 und MCT4)",
        answerD = "Glukose-Transporter (GLUT4)",
        correctAnswer = 2,
        explanation = "Monocarboxylat-Transporter (MCT1 in oxid. Fasern/Herzmuskel, MCT4 in glykolyt. Fasern) transportieren Laktat und Pyruvat durch Zellmembranen. MCT1 nimmt Laktat auf, MCT4 gibt es ab.",
        difficulty = 4,
        funFact = "George Brooks Laktat-Shuttle-Theorie (1985) revolutionierte das Verstaendnis des Laktatstoffwechsels: Laktat ist kein Abfallprodukt, sondern ein wichtiger Energietraeger zwischen Geweben."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche hormonelle Reaktion beschreibt das 'Cortisol-Awakening-Response' (CAR) und wie beeinflusst intensives Hochleistungstraining dieses Phänomen?",
        answerA = "CAR steigt mit Training und zeigt bessere Stressanpassung",
        answerB = "CAR sinkt bei Uebertraining durch HPA-Achsen-Suppression",
        answerC = "CAR ist trainingsunabhaengig und bleibt konstant",
        answerD = "CAR erhoht sich proportional zum Trainingsvolumen",
        correctAnswer = 1,
        explanation = "Bei Uebertraining zeigt die HPA-Achse (Hypothalamus-Hypophysen-Nebennierenrinden-Achse) Erschoepfungszeichen. Der CAR, ein Kortisolpeak 30 Min. nach Aufwachen, flacht bei chronischer Erschoepfung ab.",
        difficulty = 4,
        funFact = "Das CAR ist ein etablierter Biomarker fuer das Uebertraining-Syndrom und wird in der Sportmedizin verwendet, um Athleten zu monitoren, da er einfach durch Speichelproben messbar ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Mechanismus erklaert die erhoehte Glukoseaufnahme im Muskel nach dem Training ohne Insulineinfluss?",
        answerA = "Erhoehte Insulinsensitivitaet der Insulinrezeptoren",
        answerB = "AMPK-vermittelte GLUT4-Translokation an die Zellmembran",
        answerC = "Aktivierung der Phosphofruktokinase",
        answerD = "Anstieg des Glukagon-Spiegels",
        correctAnswer = 1,
        explanation = "Training aktiviert AMPK (AMP-aktivierte Proteinkinase), die unabhaengig von Insulin GLUT4-Vesikel zur Zellmembran transportiert. Dieser Mechanismus ermoeglicht Glukoseaufnahme auch bei niedrigem Insulinspiegel.",
        difficulty = 4,
        funFact = "Dieser insulin-unabhaengige Weg der Glukoseaufnahme ist therapeutisch relevant: Sport ist eine der wirksamsten Therapien bei Typ-2-Diabetes, weil er GLUT4-Translokation unabhaengig von Insulin aktiviert."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Muscle Memory' auf molekularbiologischer Ebene (Myonuklear Domain Theory)?",
        answerA = "Neuronale Verbindungen im Motorischen Kortex werden staerker",
        answerB = "Muskelfasern behalten Myonuklei auch nach Detraining, was bei Retraining schnellere Hypertrophie ermoeglicht",
        answerC = "Muskeln speichern Bewegungsmuster als Protein-Konfigurationen",
        answerD = "Propriorezeptoren kalibrieren sich auf haeufige Bewegungen ein",
        correctAnswer = 1,
        explanation = "Die Myonuklear Domain Theory besagt, dass durch Training gewonnene Myonuklei (Muskelzellkerne) auch nach Detraining verbleiben. Diese 'gespeicherten' Kerne ermöglichen bei erneutem Training schnellere Hypertrophie.",
        difficulty = 4,
        funFact = "Epigenetische Veraenderungen an der DNA (Histonmodifikationen, DNA-Methylierung) koennen Jahrzehnte bestehen bleiben und zum Muscle Memory beitragen - ein Argument gegen lebenslange Sperren fuer Dopingsünder."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Parameter beschreibt am praezisesten die anaerobe Kapazitaet und wird durch den Wingate-Test gemessen?",
        answerA = "VO2max",
        answerB = "Maximale Laktatproduktionsrate",
        answerC = "Peak Power Output und Fatigue Index",
        answerD = "Kritische Leistung (Critical Power)",
        correctAnswer = 2,
        explanation = "Der Wingate-Anaerobic Test (30 Sek. maximale Radergometer-Belastung) misst Peak Power (maximale Sprintleistung) und Fatigue Index (prozentualer Abfall). Er gilt als Goldstandard fuer anaerobe Kapazitaet.",
        difficulty = 4,
        funFact = "Worldclass-Sprinter erzielen beim Wingate-Test Peak-Power-Werte von ueber 2000 Watt, waehrend Untrainierte typischerweise bei 500-700 Watt bleiben."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt die 'Fettverbrennung' praeziser als 'Fettoxidation' im Sinne der Sporternaeehrung?",
        answerA = "Fettsaeure-Esterifizierung",
        answerB = "Adipozyten-Lipolyse mit anschliessender Betaoxidation in Mitochondrien",
        answerC = "Glukoneogenese aus Glycerol",
        answerD = "Ketonkoerper-Synthese in der Leber",
        correctAnswer = 1,
        explanation = "Fettsaeure-Oxidation umfasst zwei Schritte: Lipolyse (Triglyzeridspaltung in Glycerol und freie Fettsaeuren durch HSL/ATGL) und anschliessende Beta-Oxidation in Mitochondrien zur ATP-Synthese.",
        difficulty = 4,
        funFact = "Ausdauertraining verdoppelt die Aktivitaet der Hormon-sensitiven Lipase (HSL) und der Fettgewebe-Triglyzeridlipase (ATGL), was die Lipolyserate und damit die Energiebereitstellung aus Fett verbessert."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Forschungsgruppe entwickelte die 'Central Governor Theory' (CGT) als alternatives Modell zur klassischen Erschoepfungsphysiologie?",
        answerA = "A.V. Hill und Otto Meyerhof (Oxford, 1920er)",
        answerB = "Tim Noakes und Kollegen (Kapstadt, 1990er-2000er)",
        answerC = "David Costill und William Fink (Ball State, 1970er)",
        answerD = "Per-Olof Astrand und Kaare Rodahl (Stockholm, 1960er)",
        correctAnswer = 1,
        explanation = "Tim Noakes (University of Cape Town) entwickelte die Central Governor Theory: Das Gehirn reguliert aktiv die Muskelrekrutierung als Schutzreaktion, bevor physiologische Grenzwerte erreicht sind. Erschoepfung ist demnach eine Empfindung, kein Versagen.",
        difficulty = 4,
        funFact = "Die CGT erklaert Phänomene wie den 'Endspurt': Wenn das Ziel sichtbar ist, erlaubt das Gehirn mehr Muskelrekrutierung, weil die 'Sicherheitsmarge' aufgebraucht werden darf - ein rein zentraler Mechanismus."
    ),

    // --- SPORTGESCHICHTE-RARITAETEN (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welcher Athlet gewann bei den Olympischen Spielen 1904 in St. Louis den Marathon, wurde aber disqualifiziert, weil er einen Teil der Strecke im Auto zurueckgelegt hatte?",
        answerA = "Thomas Hicks",
        answerB = "Fred Lorz",
        answerC = "Len Tau",
        answerD = "Felix Carvajal",
        correctAnswer = 1,
        explanation = "Fred Lorz ueberquerte als Erster die Ziellinie, wurde aber disqualifiziert, als bekannt wurde, dass er 17 km im Auto mitgefahren war. Goldmedaille erhielt Thomas Hicks, der unter extremem Doping-Einfluss (Strychnin, Brandy) lief.",
        difficulty = 4,
        funFact = "Die Olympischen Spiele 1904 in St. Louis gelten als chaotischste der Geschichte: Heisse Strecke, Staubwolken durch Begleitautos und kaum Wasser machten den Marathon zum gefaehrlichsten Rennen der Olympia-Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fand das erste Wimbledon-Tennisturnier statt, und wer gewann den Herren-Einzeltitel?",
        answerA = "1875 - James Renshaw",
        answerB = "1877 - Spencer Gore",
        answerC = "1880 - William Renshaw",
        answerD = "1872 - Henry Jones",
        correctAnswer = 1,
        explanation = "Das erste Wimbledon-Turnier wurde 1877 ausgetragen. Spencer Gore gewann den Herren-Einzel, das einzige Disziplin. Das Turnier fand damals noch auf einem Croquet-Rasen statt und hatte nur 22 Teilnehmer.",
        difficulty = 4,
        funFact = "Spencer Gore nahm nie wieder an Wimbledon teil und aeusserte Zweifel, ob Tennis jemals ein populaerer Sport werden wuerde. Er scheiterte damit spektakulaer mit seiner Prognose."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer war der erste Athlet, der eine Meile in weniger als 4 Minuten lief, und welche Zeit erzielte er genau?",
        answerA = "Gunder Haegg - 4:01,4 Min",
        answerB = "Roger Bannister - 3:59,4 Min",
        answerC = "John Landy - 3:57,9 Min",
        answerD = "Arne Andersson - 4:02,6 Min",
        correctAnswer = 1,
        explanation = "Roger Bannister lief am 6. Mai 1954 in Oxford die erste Sub-4-Minuten-Meile in 3:59,4 Minuten. Weniger als 2 Monate spaeter unterbot John Landy diese Marke mit 3:57,9 Min.",
        difficulty = 4,
        funFact = "Bannister war Medizinstudent und trainierte nur eine Stunde pro Tag. Er entwickelte spaeter als Neurologe Standardwerke der klinischen Neurologie - Sport war fuer ihn immer Hobby, nicht Beruf."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann die erste FIFA Fussball-Weltmeisterschaft 1930 in Uruguay, und gegen wen spielte sie im Finale?",
        answerA = "Argentinien gegen Brasilien",
        answerB = "Uruguay gegen Argentinien",
        answerC = "Uruguay gegen Jugoslawien",
        answerD = "Argentinien gegen USA",
        correctAnswer = 1,
        explanation = "Uruguay gewann die erste WM 1930 im Heimatland mit einem 4:2-Finalsieg gegen Argentinien. Das Turnier fand ohne europaeische Topnationen statt, da viele die weite Reise scheuten.",
        difficulty = 4,
        funFact = "Im WM-Finale 1930 gab es Streit um den Spielball: Uruguay und Argentinien bestanden auf eigenen Baellen. Schliesslich wurde in der 1. Halbzeit Argentiniens Ball verwendet (Argentinien fuehrte 2:1), in der 2. Halbzeit Uruguays Ball (Uruguay gewann 4:2)."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer stellte bei den Olympischen Spielen 1972 in Muenchen einen Weltrekord ein, der 36 Jahre lang bestand?",
        answerA = "Mark Spitz",
        answerB = "Shane Gould",
        answerC = "Roland Matthes",
        answerD = "Kornelia Ender",
        correctAnswer = 2,
        explanation = "Roland Matthes (DDR) dominierte den Rueckenschwimm-Wettkampf in Muenchen und stellte Weltrekorde auf. Sein 100m-Ruecken-Weltrekord hatte eine aussergewoehnliche Langlebigkeit.",
        difficulty = 4,
        funFact = "Roland Matthes war der vielleicht technisch perfekteste Rueckenschwimmer der Geschichte. Er war von 1967 bis 1974 ungeschlagen auf 100m und 200m Ruecken bei Grossereignissen - 36 Rennen ohne Niederlage."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land boykottierte die Olympischen Spiele 1980 in Moskau als groesste Gruppe, und wie viele Nationen folgten dem amerikanischen Boykottaufruf?",
        answerA = "Ca. 30 Nationen unter Fuehrung der USA",
        answerB = "Ca. 65 Nationen unter Fuehrung der USA",
        answerC = "Ca. 50 Nationen unter Fuehrung Grossbritanniens",
        answerD = "Ca. 80 Nationen unter Fuehrung der BRD",
        correctAnswer = 1,
        explanation = "Etwa 65-66 Nationen boykottierten die Moskauer Spiele 1980 als Reaktion auf den sowjetischen Einmarsch in Afghanistan 1979. Westdeutschland und Japan schlossen sich dem US-Boykott an.",
        difficulty = 4,
        funFact = "Als Gegenmassnahme organisierten viele boykottierende Laender eigene Veranstaltungen ('Liberty Bell Classic'), die jedoch medial bedeutungslos blieben. Die Sowjetunion boykottierte 1984 die LA-Spiele als Vergeltung."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Sport und bei welchem Ereignis erzielte Eric 'The Eel' Moussambani 2000 in Sydney Beruehmtheit, obwohl er der Langsamste war?",
        answerA = "100m Lauf - Heatlauf in 14,8 Sekunden",
        answerB = "100m Schwimmen (Freistil) - Heatlauf in 1:52,72 Minuten",
        answerC = "Marathon - Ziel nach 6 Stunden",
        answerD = "Gewichtheben - technischer Aufnahme-Versuch",
        correctAnswer = 1,
        explanation = "Eric Moussambani aus Aequatorialguinea schwamm 100m Freistil in 1:52,72 Min - fast doppelt so langsam wie die Qualifikationszeit. Er hatte erst 8 Monate vor Olympia das Schwimmen gelernt.",
        difficulty = 4,
        funFact = "Moussambani qualifizierte sich durch ein IOC-Wildcardprogramm fuer Entwicklungslaender und trainierte in einem 12m-Hotelpool ohne Schwimmbrille. Sein Finaleinzug kam nur, weil seine beiden Gegner wegen Fehlstarts disqualifiziert wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer war der erste schwarze Tennisprofi, der Wimbledon und die US Open gewann, und in welchem Jahr gelang ihm der Wimbledonsieg?",
        answerA = "Arthur Ashe - 1975",
        answerB = "Yannick Noah - 1983",
        answerC = "MaliVai Washington - 1996",
        answerD = "James Blake - 2007",
        correctAnswer = 0,
        explanation = "Arthur Ashe gewann Wimbledon 1975 gegen Jimmy Connors und war der erste Schwarze, der diesen Titel holte. Zuvor hatte er 1968 die erste US Open (Open Era) gewonnen.",
        difficulty = 4,
        funFact = "Arthur Ashes Sieg in Wimbledon 1975 gegen den haushohen Favoriten Connors gilt als eine der groessten Upset-Leistungen der Tennisgeschichte. Ashe spielte eine voellig unerwartete Chip-and-Charge-Taktik."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Eisschnelllaeufer stellte bei den Olympischen Spielen 1980 in Lake Placid fünf Goldmedaillen auf und erzielte damit eine bis dahin einzigartige Leistung in einer Wintersportart?",
        answerA = "Hjalmar Andersen",
        answerB = "Eric Heiden",
        answerC = "Gaetan Boucher",
        answerD = "Ard Schenk",
        correctAnswer = 1,
        explanation = "Eric Heiden gewann alle fuenf Eisschnelllauf-Distanzen (500m bis 10.000m) bei den Olympischen Spielen 1980 in Lake Placid - eine bis heute unuebertroffene Leistung in einer Einzelsportart.",
        difficulty = 4,
        funFact = "Heiden lehnte lukrative Profi-Angebote ab und wurde Arzt. Er ist heute als orthopädischer Chirurg taetig - sein Goldhelm wurde nach den Spielen eingeschmolzen und zu einem Tablett verarbeitet."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Boxer gewann den Schwergewichts-Weltmeistertitel als 20-Jaehriger 1986 und war damit juengster Weltmeister in dieser Klasse?",
        answerA = "George Foreman",
        answerB = "Lennox Lewis",
        answerC = "Mike Tyson",
        answerD = "Evander Holyfield",
        correctAnswer = 2,
        explanation = "Mike Tyson wurde am 22. November 1986 mit 20 Jahren und 4 Monaten juengster Schwergewichtsweltmeister der Geschichte, als er Trevor Berbick in Runde 2 stoppte (WBC-Titel).",
        difficulty = 4,
        funFact = "Tyson trainierte unter Cus D'Amato, der ihn im Alter von 13 Jahren aus dem Jugendgefaengnis holte. D'Amato starb 1985, ein Jahr vor Tysons WM-Sieg, und verpasste damit den Triumph seines Schuetzlings."
    ),

    // --- SPORTPSYCHOLOGIE-FORSCHUNG (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt Mihaly Csikszentmihalyi als Zustand optimaler Leistung und vollstaendiger Absorption in eine Taetigkeit?",
        answerA = "Zone of Optimal Functioning (ZOF)",
        answerB = "Flow-Zustand",
        answerC = "Peak Performance State",
        answerD = "Individual Zones of Optimal Functioning (IZOF)",
        correctAnswer = 1,
        explanation = "Mihaly Csikszentmihalyi entwickelte das Flow-Konzept: Ein Zustand vollstaendiger Versunkenheit in eine Taetigkeit, die weder zu schwer noch zu leicht ist. Im Flow ist die intrinsische Motivation maximal.",
        difficulty = 4,
        funFact = "Csikszentmihalyi-Forschungen zeigen, dass Flow-Erlebnisse das subjektive Wohlbefinden langfristig erhoehen. Profisportler berichten, im Flow die Umgebung kaum wahrzunehmen - Zeit scheint stillzustehen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Modell erklaert das Phänomen, dass Athleten unter extremem Leistungsdruck auf frueherlernten Automatismen zurueckfallen und technische Fehler machen (Choking under Pressure)?",
        answerA = "Self-Determination Theory (SDT)",
        answerB = "Attentional Control Theory (ACT) von Eysenck",
        answerC = "Reversal Theory",
        answerD = "Processing Efficiency Theory",
        correctAnswer = 1,
        explanation = "Die Attentional Control Theory (Eysenck & Calvo) erklaert Choking: Angst verschiebt die Aufmerksamkeit von aufgabenrelevanten zu bedrohungsrelevanten Reizen. Inhibition und Verschiebung der Exekutivfunktionen werden gestoert.",
        difficulty = 4,
        funFact = "Choking tritt paradoxerweise haeufiger bei Experten auf als bei Anfaengern: Experten versuchen unter Druck, automatisierte Prozesse bewusst zu kontrollieren, was die Flüssigkeit zerstoert - Reinvestment genannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Psychologe entwickelte das IZOF-Modell (Individual Zones of Optimal Functioning) als Alternative zum umgekehrten U-Modell?",
        answerA = "Bruce Ogilvie",
        answerB = "Yuri Hanin",
        answerC = "Robert Nideffer",
        answerD = "Richard Lazarus",
        correctAnswer = 1,
        explanation = "Yuri Hanin (finnischer Sportpsychologe) zeigte, dass jeder Athlet eine individuelle optimale Erregungszone hat, die nicht universell in der 'Mitte' liegt. Manche leisten ihr Bestes bei hoher, andere bei niedriger Erregung.",
        difficulty = 4,
        funFact = "Hanins IZOF-Modell revolutionierte die Praxis der mentalen Wettkampfvorbereitung: Athleten erstellen 'emotionale Profile' ihres besten Wettkampfzustands und replizieren diese gezielt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt das Konzept der 'Selbstwirksamkeit' (Self-Efficacy) nach Albert Bandura im sportpsychologischen Kontext?",
        answerA = "Intrinsische Motivation eines Athleten",
        answerB = "Ueberzeugung, eine spezifische Aufgabe erfolgreich ausfuehren zu koennen",
        answerC = "Emotionale Stabilitaet unter Wettkampfbedingungen",
        answerD = "Kohaerenzsinn und psychische Resilienz",
        correctAnswer = 1,
        explanation = "Banduras Self-Efficacy beschreibt die aufgabenspezifische Ueberzeugung, eine bestimmte Leistung erbringen zu koennen. Im Sport beeinflusst sie Ausdauer bei Rueckschlaegen, Anstrengungsbereitschaft und Zielsetzung.",
        difficulty = 4,
        funFact = "Banduras Theorie zeigt vier Quellen der Selbstwirksamkeit: Eigene Mastery-Erfahrungen (staerkste Quelle), stellvertretendes Erleben (andere erfolgreich sehen), soziale Ueberzeugung und physiologische/affektive Zustaende."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches mentales Training nutzen Athleten, um sensorische, emotionale und kognitive Aspekte einer Leistung vorab zu simulieren, ohne physische Bewegung?",
        answerA = "Progressive Muskelentspannung",
        answerB = "Motorisches Imagery (Mentales Vorstellen)",
        answerC = "Autogenes Training",
        answerD = "Biofeedback-Training",
        correctAnswer = 1,
        explanation = "Motorisches Imagery aktiviert dieselben Hirnareale (supplementaerer Motorkortex, Basalganglien, Kleinhirn) wie tatsaechliche Bewegungsausfuehrung. Neuroimaging-Studien bestaetigen eine funktionale Aequivalenz.",
        difficulty = 4,
        funFact = "Spitzensportler wie Michael Phelps visualisierten jeden Wettkampf hunderte Male vor dem Start - Phelps berichtete, er 'lief' seine Rennen in Gedanken ab, einschliesslich unerwarteter Probleme (wie eine vollgelaufene Brille in Peking 2008)."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt die sportpsychologische Forschung unter dem Begriff 'Psychological Skills Training' (PST) als Kernkompetenz professioneller Sportpsychologen?",
        answerA = "Diagnose und Behandlung psychischer Erkrankungen im Sport",
        answerB = "Systematische Entwicklung mentaler Fertigkeiten (Zielsetzung, Imagery, Entspannung, Selbstgespraeche)",
        answerC = "Teamkommunikation und Konfliktvermittlung",
        answerD = "Karriereplanung und Laufbahnberatung fuer Athleten",
        correctAnswer = 1,
        explanation = "PST umfasst die systematische Lehre und Uebung mentaler Fertigkeiten: Zielsetzungsstrategien, mentales Imagery, Entspannungstechniken, Selbstgespraeche und Aufmerksamkeitssteuerung zur Leistungsoptimierung.",
        difficulty = 4,
        funFact = "Trotz nachgewiesener Effektivitaet nutzen nur ca. 20-40% der Elitesportler regelmaessig PST-Techniken. Das zeigt eine persistente Stigmatisierung: Sportler fuerchten, als 'mental schwach' zu gelten, wenn sie Sportpsychologen konsultieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Phaenomen beschreibt die Forschungsgruppe um Roy Baumeister als 'Ego Depletion', und wie beeinflusst es sportliche Leistung?",
        answerA = "Nachlassende Motivation durch externe Belohnungen",
        answerB = "Erschoepfung der Willenskraft durch vorangehende Selbstkontrolle, die spaetere Leistungen beeintraechtigt",
        answerC = "Identitaetsverlust durch uebertriebene Spezialisierung im Sport",
        answerD = "Burnout durch Mismatch zwischen Faehigkeiten und Anforderungen",
        correctAnswer = 1,
        explanation = "Baumeisters Ego-Depletion-Theorie: Willenskraft und Selbstkontrolle nutzen eine gemeinsame kognitive Ressource, die erschoepft werden kann. Nach mentalen Aufgaben sinkt die physische Ausdauerleistung messbar.",
        difficulty = 4,
        funFact = "Obwohl Ego Depletion in der Replikationskrise der Psychologie in Frage gestellt wurde, zeigen Sportpsychologen-Studien unter realistischen Bedingungen konsistente Effekte - besonders bei Entscheidungsmuedigkeit in langen Wettkampfen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Theorie erklaert, warum Athleten nach Niederlagen haeufig external attribuieren (Pech, Schiedsrichter) und nach Siegen internal (eigene Faehigkeit)?",
        answerA = "Achievement Goal Theory",
        answerB = "Self-serving Attribution Bias",
        answerC = "Locus of Control Theory",
        answerD = "Self-Determination Theory",
        correctAnswer = 1,
        explanation = "Der Self-serving Attribution Bias schreibt Erfolge internen Ursachen (Faehigkeit, Anstrengung) und Misserfolge externen Ursachen (Unglueck, Gegner) zu. Er schuetzt das Selbstwertgefuehl, kann aber Lernprozesse hemmen.",
        difficulty = 4,
        funFact = "Ironisch ist: Athleten mit maessig ausgepraegtem Self-serving Bias lernen besser aus Niederlagen als solche mit starkem Bias. Der Bias schuetzt kurzfristig, blockiert langfristig die Entwicklung."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt das sportpsychologische Konstrukt 'Mental Toughness' laut dem Modell von Clough & Earle (MTQ48)?",
        answerA = "Vier C's: Challenge, Commitment, Control, Confidence",
        answerB = "Vier F's: Focus, Fitness, Flexibility, Fortitude",
        answerC = "Resilienz, Ausdauer, Stresstoleranz und Belastbarkeit",
        answerD = "Drei P's: Preparation, Performance, Persistence",
        correctAnswer = 0,
        explanation = "Clough und Earles MTQ48-Modell basiert auf vier Dimensionen: Challenge (Herausforderungen als Chance), Commitment (Durchhaltevermogen), Control (Kontrolluberzeugung) und Confidence (Selbstvertrauen).",
        difficulty = 4,
        funFact = "Mental Toughness korreliert mit akademischen Leistungen, Buerotaetigkeit und Gesundheitsverhalten - es ist kein rein sportspezifisches Konstrukt. Hoch in MT scoring Personen berichten weniger Burnout und mehr Wohlbefinden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept der Motivationsforschung beschreibt Edward Deci und Richard Ryan in ihrer Self-Determination Theory als drei angeborene psychologische Grundbeduerfnisse?",
        answerA = "Macht, Status, Zugehoerigkeit",
        answerB = "Autonomie, Kompetenz, soziale Eingebundenheit",
        answerC = "Leistung, Sicherheit, Annerkennung",
        answerD = "Kontrolle, Vorhersagbarkeit, Bindung",
        correctAnswer = 1,
        explanation = "Decis und Ryans SDT postuliert drei angeborene psychologische Grundbeduerfnisse: Autonomie (Selbstbestimmung), Kompetenz (Wirksamkeit erleben) und soziale Eingebundenheit. Deren Befriedigung foerdert intrinsische Motivation.",
        difficulty = 4,
        funFact = "SDT-Forschung zeigt, dass leistungsorientiertes Coaching (kontrollierende Sprache, Druck) zwar kurzfristig Leistung steigert, langfristig aber Burnout, Dropout und Freudverlust am Sport foerdert."
    ),

    // --- SPORTOEKONOMIE / BUSINESS (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welches oekonomische Konzept erklaert, warum Sportligen den Ausgleich von Staerkeunterschieden zwischen Teams anstreben, um Wettbewerbsspannung und damit Zuschauerinteresse zu erhalten?",
        answerA = "Monopsontheorie",
        answerB = "Competitive Balance Theory",
        answerC = "Revenue Sharing Principle",
        answerD = "Natural Monopoly Approach",
        correctAnswer = 1,
        explanation = "Competitive Balance Theory (nach Rottenberg, 1956): Sportligen brauchen ausgewogene Wettbewerbe, da Fans uninteressierte Spiele mit vorhersehbarem Ausgang meiden. Revenue Sharing und Draftsysteme foerdern diese Balance.",
        difficulty = 4,
        funFact = "Simon Rottenberg veroeffentlichte 1956 den ersten sportoekonomischen Artikel ueberhaupt - ueber den Baseball-Arbeitsmarkt. Er antizipierte viele Effekte, die erst Jahrzehnte spaeter durch Freiheitsrechte (Free Agency) eintraten."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie hoch war der Transferrekord, den Paris Saint-Germain 2017 fuer Neymar zahlte, und an welchen Klub?",
        answerA = "180 Millionen Euro an Real Madrid",
        answerB = "222 Millionen Euro an FC Barcelona",
        answerC = "200 Millionen Euro an FC Barcelona",
        answerD = "250 Millionen Euro an Juventus Turin",
        correctAnswer = 1,
        explanation = "PSG loeste im Sommer 2017 Neymars Ausstiegsklausel beim FC Barcelona in Hoehe von 222 Millionen Euro - ein weltweiter Rekord, der bis heute unuebertroffen ist.",
        difficulty = 4,
        funFact = "Die 222-Millionen-Klausel galt als so astronomisch, dass Barcelona sie als sicher ansah. PSG nutzte das Financial Fair Play-Schlupfloch von staatlichen Katar-Subventionen, was UEFA-Untersuchungen ausloeste."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche amerikanische Sportoekonomie-Theorie besagt, dass der Markt fuer Athletenleistungen ein Monopson ist, bei dem Ligen als einzige Kaeufermacht Loehne unter dem Wettbewerbsniveau druecken koennen?",
        answerA = "Reserve Clause System",
        answerB = "Monopsony Power Theory",
        answerC = "Bilateral Monopoly Model",
        answerD = "Market Segmentation Theory",
        correctAnswer = 1,
        explanation = "Monopsony Power im Sport: Ligen agieren als einzige Kaeufermacht fuer Spitzensportler. Ohne Free Agency koennen sie Gehaelter unter dem Marktgleichgewicht halten. Floodgate-Urteile (1972, MLB) und Bozman-Urteile (1995) brachen diese Macht auf.",
        difficulty = 4,
        funFact = "MLB-Spieler verdienten bis zur Free Agency 1975 typischerweise 50% unter ihrem Marktwert. Nach der Einfuehrung der Free Agency stiegen Gehaelter innerhalb von 5 Jahren um 400%, was Monopsony-Effekte klar belegte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt den oekonomischen Effekt, dass grosse Sportevents nicht die versprochenen wirtschaftlichen Impulse bringen und haeufig negative Langzeitfolgen fuer Austraeger haben?",
        answerA = "Olympic Dividend",
        answerB = "White Elephant Effect",
        answerC = "Mega-Event Surplus",
        answerD = "Sports-Led Regeneration",
        correctAnswer = 1,
        explanation = "Der 'White Elephant Effect' beschreibt Sportstadien und Infrastruktur, die nach Grossevents leer stehen und hohe Unterhaltskosten verursachen. Athen 2004, Rio 2016 und viele WM-Stadien sind klassische Beispiele.",
        difficulty = 4,
        funFact = "Brendan Gibbons Studien zeigen, dass keine olympischen Spiele seit Montreal 1976 ihr Budget eingehalten haben. Die durchschnittliche Kostenuebersteigung betraegt 156%, Athen 2004 ueberzog um ueber 100%."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Unternehmen zahlte 2021 laut Medienberichten den hoechsten Preis fuer Sportrechte und erwarb die Rechte an der NFL Sunday Ticket in den USA?",
        answerA = "Amazon Prime Video",
        answerB = "Apple TV+",
        answerC = "Google / YouTube",
        answerD = "Disney / ESPN",
        correctAnswer = 2,
        explanation = "Google (YouTube) erwarb 2022 die NFL Sunday Ticket-Rechte fuer ca. 2 Milliarden USD jaehrlich ab der Saison 2023 - einer der teuersten Sportrechte-Deals der Geschichte, deutlich ueber DirecTVs frueheren 1,5 Mrd./Jahr.",
        difficulty = 4,
        funFact = "Der Sunday Ticket-Deal markiert den Durchbruch von Streaming-Plattformen in den Premium-Sportrechte-Markt. Traditionelle Broadcaster wie ESPN zahlten vorher weniger fuer breitere Pakete als YouTube fuer ein einziges Produkt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches oekonomische Modell erklaert, warum Profifussballer in Europa trotz Arbeitnehmerfreiizuegigkeit als EU-Buerger oft an nationale Transfersysteme gebunden sind?",
        answerA = "Bosman-Urteil-Anpassungsmodell",
        answerB = "Solidarity Mechanism und Training Compensation",
        answerC = "UEFA Financial Fair Play Regelwerk",
        answerD = "FIFA-Statuten-Primaet ueber EU-Recht",
        correctAnswer = 1,
        explanation = "Das Bosman-Urteil (1995) garantiert EU-weite Freiizuegigkeit nach Vertragsende. FIFA und UEFA haben aber Solidarity-Mechanismen und Training-Kompensationssysteme etabliert, die Ausbildungsklubs entschaedigen.",
        difficulty = 4,
        funFact = "Jean-Marc Bosman, der belgische Spieler, der den EuGH-Fall ausloeste, litt nach dem Urteil an Depressionen und Alkoholismus. Das Urteil veraenderte den europaeeischen Fussballmarkt fundamental - dem Klaeger selbst half es kaum."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man in der Sportoekonomie unter dem Begriff 'Superstar Economics', gepraegt durch Sherwin Rosen 1981?",
        answerA = "Marktmacht grosser Ligen gegenueber kleinen",
        answerB = "Ueberproportionale Einkommenskonzentration bei Topathleten durch Medien-Multiplikator-Effekte",
        answerC = "Monopolgewinne bei Sportereignissen mit Exklusivitaet",
        answerD = "Netzwerkeffekte bei Fangemeinschaften bekannter Teams",
        correctAnswer = 1,
        explanation = "Sherwin Rosens 'The Economics of Superstars' (1981) erklaert: Kleine Qualitaetsunterschiede fuehren zu enormen Einkommensunterschieden, weil Medien- und Unterhaltungsmaerkte Winner-Takes-All-Dynamiken haben.",
        difficulty = 4,
        funFact = "Rosens Theorie erklaert, warum der Weltmeister 100-mal mehr verdient als der Weltranglistenzehnte: Konsumenten koennen dank Medien immer den Besten konsumieren - warum den Zehntbesten waehlen?"
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher NBA-Spieler loeste 1984 den groessten Wandel in der Sportvermarktung aus, als sein Nike-Deal die Schuhbranche revolutionierte?",
        answerA = "Magic Johnson",
        answerB = "Larry Bird",
        answerC = "Michael Jordan",
        answerD = "Kareem Abdul-Jabbar",
        correctAnswer = 2,
        explanation = "Michael Jordans Nike-Deal 1984 fuer die Air Jordan-Linie revolutionierte Athleten-Endorsements. Nike zahlte 2,5 Millionen USD ueber 5 Jahre und ein Royalty-Modell, das Jordan 50 Millionen USD jaehrlich einbrachte.",
        difficulty = 4,
        funFact = "Die NBA verbot Air Jordans anfangs, weil sie nicht die Teamfarben hatten. Nike zahlte Jordans Strafe von 5.000 USD pro Spiel und nutzte das Verbot als Marketingmassnahme - verkaufte das Verbot als Beweis der Einzigartigkeit."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches finanztechnische Instrument nutzen Spitzensportklubs (z.B. Juventus Turin 2019), um zukuenftige Transfereinnahmen oder TV-Rechte zu verbrieven und sofort zu monetarisieren?",
        answerA = "Abloesegebueehr-Futures",
        answerB = "Asset-Backed Securities (Verbriefung kuenftiger Einnahmen)",
        answerC = "Sportwetten-Derivate",
        answerD = "UEFA-Kredit-Fazilitaeten",
        correctAnswer = 1,
        explanation = "Asset-Backed Securities (ABS) erlauben Klubs, zukuenftige Einnahmen (TV-Rechte, Ticketverkauefe) zu verbrieven und sofort Kapital zu erhalten. Juventus nutzte dies 2019 fuer 175 Millionen EUR zur Transferfinanzierung.",
        difficulty = 4,
        funFact = "Manchester United emittierte 2012 als erster Fussballklub Hochzins-Anleihen an der Boerse. Heute werden Sportklub-Anleihen regelmaessig an Finanzmaerkten gehandelt - Sport ist Finanzprodukt geworden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Begriff beschreibt in der Sportoekonomie die Praxis, durch den Namen eines Unternehmens ein Stadion zu benennen und damit Werbeexposure zu kaufen?",
        answerA = "Stadium Branding",
        answerB = "Naming Rights",
        answerC = "Facility Sponsorship",
        answerD = "Title Partnership",
        correctAnswer = 1,
        explanation = "Naming Rights bezeichnen den kommerziellen Kauf des Rechts, einem Stadion oder einer Sportanlage einen Unternehmensnamen zu geben. Der erste grosse Deal war 1988 Great Western Forum in LA, fuer 1,5 Mio. USD jaehrlich.",
        difficulty = 4,
        funFact = "Der teuerste aktive Naming Rights-Deal ist das 'SoFi Stadium' in Los Angeles (Rams/Chargers) fuer 625 Millionen USD ueber 20 Jahre (2020). Zum Vergleich: der erste NFL-Naming-Rights-Deal war 1 Million USD."
    )

)
