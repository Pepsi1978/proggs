package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMaster2(): List<Question> = listOf(

    // --- DOPAMIN-BAHNEN ---

    // Question 1 - Mesolimbisches System
    Question(
        categoryId = 16,
        questionText = "Welche dopaminerge Bahn projiziert vom ventralen Tegmentum (VTA) zum Nucleus accumbens und ist primaer fuer die Verarbeitung von Belohnung und Suchtmechanismen verantwortlich?",
        answerA = "Nigrostriatale Bahn",
        answerB = "Mesolimbische Bahn",
        answerC = "Mesokortikale Bahn",
        answerD = "Tuberoinfundibulaere Bahn",
        correctAnswer = 1,
        explanation = "Die mesolimbische Bahn laeuft vom VTA zum Nucleus accumbens (ventrales Striatum) und ist das zentrale Substrat fuer Belohnungsverarbeitung, motivationales Verhalten und die Entstehung von Sucht. Nahezu alle Drogen, die Abhaengigkeit erzeugen, erhoehen die Dopaminausschuettung in diesem System.",
        difficulty = 5,
        funFact = "Ratten, denen Elektroden in das mesolimbische Belohnungssystem implantiert werden, druecken lieber den Stimulationsknopf als dass sie fressen oder trinken -- bis zur Erschoepfung."
    ),

    // Question 2 - Mesokortikale Bahn
    Question(
        categoryId = 16,
        questionText = "Die mesokortikale dopaminerge Bahn projiziert vom VTA zum praefrontalen Kortex. Welcher klinische Befund entsteht bei Hypoaktivitaet dieser Bahn bei Schizophrenie?",
        answerA = "Positivsymptome wie Halluzinationen und Wahn",
        answerB = "Negativsymptome wie Antriebsminderung, Affektverflachung und kognitive Defizite",
        answerC = "Tardive Dyskinesien durch Dopamin-Ueberaktivitaet",
        answerD = "Hyperprolaktinaemie durch fehlende Dopamin-Hemmung",
        correctAnswer = 1,
        explanation = "Die Hypoaktivitaet der mesokortikalen Bahn mit konsekutiv reduziertem praefrontalem Dopamin erklaert die Negativsymptome und kognitiven Defizite bei Schizophrenie. Diese Bahn versorgt den DLPFC, der fuer Arbeitsgedaechtnis und Exekutivfunktionen zustaendig ist -- eine Unterfunktion fuehrt zu den therapieresistenten Negativsymptomen.",
        difficulty = 5,
        funFact = "Klassische Antipsychotika blockieren D2-Rezeptoren breit und bessern zwar Positivsymptome, verschlechtern aber die mesokortikale Hypoaktivitaet -- was erklaert, warum sie Negativsymptome kaum bessern."
    ),

    // Question 3 - Nigrostriatale Bahn und Parkinson
    Question(
        categoryId = 16,
        questionText = "In der nigrostriatalen Bahn projizieren Neurone der Substantia nigra pars compacta zum Striatum. Welcher Prozentsatz dieser Neurone muss degeneriert sein, bevor motorische Parkinson-Symptome klinisch manifest werden?",
        answerA = "Etwa 20-30% Verlust",
        answerB = "Etwa 50-60% Verlust",
        answerC = "Etwa 70-80% Verlust",
        answerD = "Etwa 90-95% Verlust",
        correctAnswer = 2,
        explanation = "Klinische Parkinson-Symptome (Tremor, Rigor, Akinesie) werden erst sichtbar, wenn etwa 70-80% der dopaminergen Neurone der Substantia nigra pars compacta degeneriert sind und die striatale Dopaminkonzentration auf ca. 20-30% des Normalwertes gesunken ist. Das Gehirn verfuegt ueber erhebliche Kompensationsmechanismen durch neuronale Plastizitaet.",
        difficulty = 5,
        funFact = "Diese massive Kompensationskapazitaet bedeutet, dass Parkinson eine 'stille' Prodromalphase von 5-10 Jahren hat -- Anosmie, REM-Schlaf-Verhaltensstoerung und Obstipation sind fruehe Warnsignale, die vor den motorischen Symptomen auftreten."
    ),

    // Question 4 - Tuberoinfundibulaere Bahn
    Question(
        categoryId = 16,
        questionText = "Die tuberoinfundibulaere dopaminerge Bahn reguliert die Prolaktinsekretion. Welcher klinische Effekt tritt bei D2-Blockade durch Antipsychotika in dieser Bahn auf?",
        answerA = "Suppression der Prolaktinsekretion und Amenorrhoe",
        answerB = "Hyperprolaktinaemie mit Galaktorrhoe, Amenorrhoe und sexuellen Dysfunktionen",
        answerC = "Erhoehte ACTH-Sekretion durch fehlende Dopamin-Hemmung",
        answerD = "Diabetes insipidus durch ADH-Suppression",
        correctAnswer = 1,
        explanation = "Dopamin hemmt normalerweise ueber D2-Rezeptoren die Prolaktinsekretion aus laktotrophen Zellen der Adenohypophyse. Bei D2-Blockade durch Antipsychotika entfaellt diese Hemmung, und Prolaktin steigt an -- klinische Folgen sind Galaktorrhoe, Amenorrhoe bei Frauen und erektile Dysfunktion sowie Gynaekomastie bei Maennern.",
        difficulty = 5,
        funFact = "Dopamin ist der einzige hypothalamische Faktor, der eine tonische Hemmung ausuebt -- alle anderen Hormone werden durch spezifische Releasing-Hormone stimuliert. Prolaktin ist daher das einzige Hypophysenhormon, das bei Hypophysenstielschaden ansteigt statt abzufallen."
    ),

    // --- SYNAPTISCHE PLASTIZITAET ---

    // Question 5 - LTP-Induktion
    Question(
        categoryId = 16,
        questionText = "Welche molekulare Voraussetzung muss erfuellt sein, damit NMDA-Rezeptoren bei der LTP-Induktion (Langzeitpotenzierung) geoeffnet werden koennen?",
        answerA = "Alleinige Bindung von Glutamat genuegt fuer die vollstaendige NMDA-Kanaloefffnung",
        answerB = "Simultane Bindung von Glutamat und Glycin sowie gleichzeitige postsynaptische Depolarisation, um die Mg2+-Blockade aufzuheben",
        answerC = "Phosphorylierung des NMDA-Rezeptors durch PKC ist die alleinige Voraussetzung",
        answerD = "GABA-Hemmung muss komplett sistieren, bevor NMDA-Rezeptoren oeffnen koennen",
        correctAnswer = 1,
        explanation = "NMDA-Rezeptoren sind spannungsabhaengig und ligandgesteuert: Im Ruhezustand blockiert Mg2+ den Ionenkanal. Fuer die Kanaloefffnung wird benoetigt: (1) Glutamat-Bindung an die GluN2-Untereinheit, (2) Glycin als Koagonist an der GluN1-Untereinheit und (3) ausreichende postsynaptische Depolarisation, die das Mg2+ verdraengt. Diese Koinzidenzdetektion macht NMDA-Rezeptoren zu Hebbschen 'Synapsendetektoren'.",
        difficulty = 5,
        funFact = "Diese Koinzidenzdetektion -- gleichzeitige Aktivitaet praesynaptischer und postsynaptischer Zelle -- ist die molekulare Umsetzung der Hebbbschen Lernregel von 1949: 'Neurons that fire together, wire together.'"
    ),

    // Question 6 - AMPA-Rezeptor-Trafficking bei LTP
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die fruehere Ausdruckphase von LTP (E-LTP) auf molekularer Ebene nach NMDA-Rezeptor-Aktivierung?",
        answerA = "Sofortige Neusynthese von AMPA-Rezeptoren am rauen ER",
        answerB = "CaMKII-abhaengige Phosphorylierung und Einbau zusaetzlicher AMPA-Rezeptoren aus intrazellulaerem Pool in die postsynaptische Membran",
        answerC = "Dendriten-Wachstum und Bildung neuer Synapsen innerhalb von Minuten",
        answerD = "Prasynaptische Erhoehung der Glutamat-Vesikel-Freisetzungswahrscheinlichkeit",
        correctAnswer = 1,
        explanation = "Nach Ca2+-Einstrom durch NMDA-Rezeptoren wird CaMKII (Calmodulin-abhaengige Proteinkinase II) aktiviert, die AMPA-Rezeptoren an Ser831 phosphoryliert und deren Einbau in die postsynaptische Dichte foerdert. AMPA-Rezeptoren aus Endosomen werden rasch in die Synapse eingebaut -- das erhoehte synaptische AMPA-Signal erklaert die E-LTP innerhalb von Minuten.",
        difficulty = 5,
        funFact = "Sogenannte 'stille Synapsen' enthalten nur NMDA-, aber keine AMPA-Rezeptoren. LTP 'weckt' sie auf, indem es AMPA-Rezeptoren einbaut -- ein Mechanismus, der fuer die fruehe Gedaechtnisbildung bei Saeuglingen besonders wichtig ist."
    ),

    // Question 7 - LTD und AMPA-Internalisierung
    Question(
        categoryId = 16,
        questionText = "Langzeitdepression (LTD) im Hippokampus wird durch niedrigfrequente Stimulation induziert. Welcher molekulare Hauptmechanismus vermittelt die synaptische Abschwaechtung?",
        answerA = "Caspase-3-abhaengige Spaltung von AMPA-Rezeptoruntereinheiten",
        answerB = "PP2B/Calcineurin-abhaengige Dephosphorylierung und clathrin-vermittelte Endozytose von AMPA-Rezeptoren",
        answerC = "mTOR-abhaengige Suppression der lokalen AMPA-Rezeptor-Translation",
        answerD = "Retrogrades Endocannabinoid-Signal, das praesynaptische GABA-Freisetzung steigert",
        correctAnswer = 1,
        explanation = "Niedrigfrequente Stimulation erzeugt moderaten Ca2+-Anstieg, der Calcineurin (PP2B) bevorzugt aktiviert. Calcineurin dephosphoryliert GluA1 und foerdert durch Interaktion mit PICK1 und AP-2-Komplex die clathrin-vermittelte Endozytose von AMPA-Rezeptoren. Der Verlust synaptischer AMPA-Rezeptoren vermindert die Synapsenantwort -- das ist die Grundlage von LTD.",
        difficulty = 5,
        funFact = "LTD ist kein Fehler im Gedaechtnis -- es ist essentiell fuer Lernflexibilitaet. Im Kleinhirn ist Purkinje-LTD die Grundlage der motorischen Lernkorrekturen beim Erlernen praeziser Bewegungen."
    ),

    // Question 8 - NMDA-Rezeptor-Untereinheiten und Entwicklung
    Question(
        categoryId = 16,
        questionText = "Welche Veraenderung der NMDA-Rezeptor-Untereinheitenzusammensetzung kennzeichnet den Uebergang von der Entwicklung zum reifen adulten Gehirn und beeinflusst die Plastizitaetsschwelle?",
        answerA = "GluN2A ersetzt GluN2B als dominante GluN2-Untereinheit -- dadurch sinkt die Ca2+-Leitfaehigkeit und die Oeffnungszeit verkuerzt sich",
        answerB = "GluN1 wird durch GluN3A ersetzt, was die Glycin-Affinitaet dramatisch erhoeh",
        answerC = "GluN2D-reiche Rezeptoren im Entwicklungsgehirn werden durch GluN2C-Rezeptoren ersetzt",
        answerD = "Heteromerische NMDA-Rezeptoren werden durch homomere GluN1-Rezeptoren ersetzt",
        correctAnswer = 0,
        explanation = "Im unreifen Gehirn dominieren GluN2B-enthaltende NMDA-Rezeptoren mit langer Oeffnungszeit und hoher Ca2+-Permeabilitaet -- ideal fuer maximale synaptische Plastizitaet. Mit der Hirnreifung wird GluN2B zunehmend durch GluN2A ersetzt, was kuerzere Oeffnungszeiten bewirkt und die Plastizitaetsschwelle anhebt. Dies erklaert das Schliessen sensibler Perioden.",
        difficulty = 5,
        funFact = "Ketamin als dissoziatives Anaesthetikum blockiert NMDA-Rezeptoren nicht-kompetitiv. Die groessere Praevalenz von GluN2B im Kindheitsgehirn macht Kinder empfindlicher gegenueber den dissoziativen Effekten von Ketamin als Erwachsene."
    ),

    // --- NEUROINFLAMMATION ---

    // Question 9 - Mikroglia-Aktivierung
    Question(
        categoryId = 16,
        questionText = "Mikroglia durchlaufen nach Hirnschaedigung eine Aktivierungskaskade. Welcher Rezeptor erkennt als 'Pattern Recognition Receptor' lipopolysaccharide (LPS) und initiiert die pro-inflammatorische M1-Polarisierung?",
        answerA = "P2X7-Rezeptor (ATP-gesteuert)",
        answerB = "TREM2 (Triggering Receptor Expressed on Myeloid Cells 2)",
        answerC = "TLR4 (Toll-like Receptor 4) in Komplex mit MD-2 und CD14",
        answerD = "CX3CR1 (Fraktalkine-Rezeptor)",
        correctAnswer = 2,
        explanation = "TLR4 erkennt in Komplex mit dem Co-Rezeptor MD-2 und CD14 das bakterielle Lipopolysaccharid. Die Signalkaskade verlaeuft ueber MyD88 und TRIF, aktiviert NF-kappaB und IRF3, was zur Transkription von pro-inflammatorischen Zytokinen (TNF-alpha, IL-1beta, IL-6) und Typ-I-Interferonen fuehrt. Dies ist ein entscheidender Mechanismus der Neuroinflammation bei Sepsis-assoziierter Enzephalopathie.",
        difficulty = 5,
        funFact = "TREM2, urspruenglich bei Nasu-Hakola-Krankheit entdeckt, ist ein Hauptrisikogen fuer Alzheimer. TREM2-defiziente Mikroglia koennen Amyloid-Beta-Plaques nicht effektiv phagozytieren -- ein zentraler Durchbruch im Verstaendnis der Alzheimer-Pathogenese."
    ),

    // Question 10 - Astrozyten-Reaktivitaet
    Question(
        categoryId = 16,
        questionText = "Reaktive Astrogliose wird in zwei funktionell gegensaetzliche Phaenotypen eingeteilt. Welcher Phaenotyp wird durch IL-1alpha, TNF und C1q (aus Mikroglia) induziert und foerdert neuronalen Zelltod?",
        answerA = "A2-reaktive Astrozyten (neuroprotektiv, durch Ischaemie induziert)",
        answerB = "A1-reaktive Astrozyten (neurotoxisch, durch Neuroinflammation induziert)",
        answerC = "Bergmann-Glia (nur im Kleinhirn relevant)",
        answerD = "GFAP-negative Astrozyten-Vorlaeuferzellen",
        correctAnswer = 1,
        explanation = "Barres-Labor (2017) beschrieb A1-Astrozyten, die durch mikroglia-sezernierte Signalmolekuele IL-1alpha, TNF und C1q induziert werden und neurotoxisch sind -- sie hemmen die synaptische Funktion und foerdern den Untergang von Neuronen und Oligodendrozyten. A2-Astrozyten dagegen entstehen nach Ischaemie und produzieren neurotrophe Faktoren. Diese Dichotomie ist bei Parkinson, ALS und Alzheimer relevant.",
        difficulty = 5,
        funFact = "A1-Astrozyten wurden in post-mortem-Hirngewebe von Parkinson-, Huntington-, ALS- und Alzheimer-Patienten nachgewiesen -- dies deutet auf eine uebergreifende astrozytaere Neurotoxizitaet als gemeinsamen Pfad neurodegenerativer Erkrankungen hin."
    ),

    // Question 11 - NLRP3-Inflammasom
    Question(
        categoryId = 16,
        questionText = "Das NLRP3-Inflammasom spielt eine Schluesserolle bei der Neuroinflammation. Welches Substrat schneidet Caspase-1 nach NLRP3-Aktivierung, um reife pro-inflammatorische Zytokine freizusetzen?",
        answerA = "NF-kappaB-Vorlaeufer und IkB-alpha",
        answerB = "Pro-IL-1beta und Pro-IL-18, sowie Gasdermin D zur Porenbildung (Pyroptose)",
        answerC = "Pro-Caspase-3 als Einleitung der Apoptose",
        answerD = "HMGB1 als spaeter Mediator der Entzuendung",
        correctAnswer = 1,
        explanation = "Aktiviertes NLRP3-Inflammasom rekrutiert ASC und Caspase-1. Aktive Caspase-1 schneidet Pro-IL-1beta zu IL-1beta und Pro-IL-18 zu IL-18 -- beides potente pro-inflammatorische Zytokine ohne Signalpeptid, die nur durch Porenbildung sezerniert werden koennen. Gasdermin D wird ebenfalls gespalten und bildet Membranporen, die IL-1beta/IL-18 freilassen und Pyroptose einleiten.",
        difficulty = 5,
        funFact = "Colchizin, ein uraltes Gichtmedikament aus Colchicum autumnale, hemmt das NLRP3-Inflammasom. Aktuelle klinische Studien untersuchen es bei kardiovaskulaeren Erkrankungen und sogar bei Alzheimer als Neuroinflammations-Blocker."
    ),

    // --- NEUROGENETICS ---

    // Question 12 - Huntington-Repeat-Expansion
    Question(
        categoryId = 16,
        questionText = "Das Huntingtin-Gen (HTT) enthaelt eine CAG-Repeat-Sequenz im Exon 1. Ab welcher Repeat-Anzahl gilt eine vollstaendige Penetranz fuer die Huntington-Erkrankung, und welches Protein wird entsprechend veraendert?",
        answerA = "Ab 27 CAG-Repeats; Huntingtin-Protein mit verlaengertem Polyglutamin-Trakt",
        answerB = "Ab 40 CAG-Repeats; Huntingtin-Protein mit verlaengertem Polyglutamin-Trakt",
        answerC = "Ab 36 CAG-Repeats; Huntingtin-Protein -- 36-39 Repeats reduzierte, ab 40 vollstaendige Penetranz",
        answerD = "Ab 55 CAG-Repeats; Huntingtin verliert seine nukleaere Exportfunktion",
        correctAnswer = 2,
        explanation = "36-39 CAG-Repeats in HTT Exon 1 bedeuten reduzierte Penetranz (nicht alle Traeger erkranken), ab 40 Repeats vollstaendige Penetranz. Das verlaengerte Polyglutamin (polyQ) im mutierten Huntingtin-Protein beeintraechtigt die normale Funktion als Geruest-Protein, foerdert toxische Aggregatbildung und stoert mitochondriale Funktion, axonalen Transport und Transkriptionsregulation.",
        difficulty = 5,
        funFact = "Die Repeat-Laenge korreliert invers mit dem Erkrankungsalter -- mehr Repeats, frueherer Beginn. Die juvenile Form (Westphal-Variante, Beginn <20 Jahre) geht mit >60 CAG-Repeats einher und praesentierts mit Rigor statt Chorea -- da das nigrostriatale System juenger staerker betroffen ist."
    ),

    // Question 13 - FMR1 und fragiles X-Syndrom
    Question(
        categoryId = 16,
        questionText = "Das fragile X-Syndrom (FXS) entsteht durch CGG-Repeat-Expansion im 5'-UTR von FMR1. Welcher Mechanismus fuehrt bei vollen Mutationen (>200 Repeats) zum Verlust des FMRP-Proteins?",
        answerA = "Missense-Mutation im FMRP-RNA-Bindemotiv durch fehlerhafte Transkription der Repeat-Region",
        answerB = "Hypermethylierung der CGG-Repeat-Region und des FMR1-Promoters, was die Transkription vollstaendig silenziert",
        answerC = "Nonsense-Mutation durch Frameshift in der Kodiersequenz",
        answerD = "Alternativer Spleissverlust des Exons 2 durch veraenderte Spleiss-Enhancer",
        correctAnswer = 1,
        explanation = "Bei vollstaendiger FMR1-Mutation (>200 CGG-Repeats) wird die DNA in der Repeat-Region und im FMR1-Promoter durch DNA-Methyltransferasen hypermethyliert. Dies fuehrt zum Schweigen des gesamten Gens -- kein FMR1-mRNA, kein FMRP-Protein. FMRP ist normalerweise ein RNA-Bindeprotein, das die Translation von synaptischen Proteinen reguliert; sein Fehlen fuehrt zu unkontrollierter ueberschiessender synaptischer Proteinsynthese.",
        difficulty = 5,
        funFact = "Praemutatioens-Traeger (55-200 Repeats) schweigen FMR1 nicht vollstaendig, produzieren aber abnorm viel FMR1-mRNA, die toxisch auf RNA-Bindeproteine wirkt -- dies fuehrt zum FXTAS (Fragiles-X-Tremor/Ataxie-Syndrom) im Alter."
    ),

    // Question 14 - APP-Mutationen und familiaerem Alzheimer
    Question(
        categoryId = 16,
        questionText = "Mutationen im Amyloid-Vorlaufer-Protein (APP) am Chromosom 21 fuehren zu familiaeren Alzheimer. An welcher Position liegt die Schwedische Doppelmutation (K670N/M671L) und wie beeinflusst sie die Amyloid-Produktion?",
        answerA = "Innerhalb der transmembranosen Domaine -- verstaerkt die gamma-Sekretase-Spaltung zu Abeta42",
        answerB = "N-terminal der Beta-Sekretase-Schnittstelle -- erhoehte Spaltungseffizienz von BACE1 und massiv gesteigerter Gesamt-Abeta-Spiegel",
        answerC = "C-terminal der Gamma-Sekretase-Schnittstelle -- verschiebt Verhaeltnis Abeta42/Abeta40",
        answerD = "Im Signalpeptid -- beschleunigt den sekretorischen Transport von APP an die Zelloberflaeche",
        correctAnswer = 1,
        explanation = "Die Schwedische Doppelmutation (KM670/671NL) liegt unmittelbar N-terminal der BACE1-Schnittstelle und erhoehte die Substrataffinitaet von BACE1 (Beta-Sekretase) erheblich. Das Ergebnis ist eine 6-8-fache Erhoehung des gesamten Abeta-Spiegels (sowohl Abeta40 als auch Abeta42), was zu familiaerem Alzheimer mit Beginn in der 6. Lebensdekade fuehrt.",
        difficulty = 5,
        funFact = "Trisomie 21 (Down-Syndrom) verursacht durch die dreifache APP-Genkopie nahezu universell Alzheimer-Pathologie bis zum 40. Lebensjahr -- ein direkter Beweis, dass erhoehte Abeta-Produktion Alzheimer verursacht."
    ),

    // --- PSYCHOPHARMAKOLOGIE ---

    // Question 15 - MAO-Hemmer-Mechanismus
    Question(
        categoryId = 16,
        questionText = "Irreversible MAO-Hemmer der ersten Generation (z.B. Phenelzin) hemmen sowohl MAO-A als auch MAO-B. Welche gefaehrliche Interaktion erklaert die tyraminhaltige Kaese-Reaktion (Cheese-Reaction)?",
        answerA = "MAO-A-Hemmung verhindert die intestinale Tyrosin-Hydroxylierung, was zu Tyramin-Anhaefung im Darm fuehrt",
        answerB = "Gehemmte MAO-A im Darm und in der Leber kann Tyramin aus der Nahrung nicht abbauen; Tyramin verdraengt Noradrenalin aus Vesikeln und loest hypertensive Krise aus",
        answerC = "Tyramin konkurriert direkt mit MAO-Hemmern um die aktive Bindestelle und reaktiviert MAO",
        answerD = "Tyramin aktiviert praesynaptische D3-Rezeptoren und steigert die Dopaminausschuettung massiv",
        correctAnswer = 1,
        explanation = "Normalerweise wird Tyramin aus der Nahrung durch MAO-A in der Darmwand und der Leber (First-Pass-Effekt) effektiv abgebaut. Bei irreversibler MAO-A-Hemmung gelangt Tyramin in den Kreislauf, wird durch NET in noradrenerge Nervenenden aufgenommen und verdraengt dort Noradrenalin aus Speichervesikeln -- massive Noradrenalin-Freisetzung bewirkt hypertensive Krise mit Risiko von Schlaganfall.",
        difficulty = 5,
        funFact = "Die Entdeckung der Cheese-Reaction 1963 durch den britischen Pharmakologen Barry Blackwell begann mit dem Tod einer Patientin, deren Mann Apotheker war und den Zusammenhang zwischen Kaese-Konsum und hypertensiver Krise unter MAO-Hemmern als erster systematisch beschrieb."
    ),

    // Question 16 - Lithium-Mechanismus
    Question(
        categoryId = 16,
        questionText = "Lithium wirkt als Stimmungsstabilisator bei bipolaren Stoerungen. Welchen intrazellulaerenSignalweg hemmt Lithium direkt und welche therapeutisch relevante Konsequenz hat dies?",
        answerA = "Lithium hemmt Adenylatzyklase und senkt cAMP -- reduziert noradrenerge Signaltransduktion",
        answerB = "Lithium hemmt GSK-3beta und Inositol-Monophosphatase, was den Phosphoinositid-Zyklus unterbricht und GSK-3beta-abhaengige neuronale Apoptose hemmt",
        answerC = "Lithium blockiert spannungsabhaengige Natriumkanaele im therapeutischen Konzentrationsbereich",
        answerD = "Lithium hemmt HDAC und erhoehte dadurch BDNF-Transkription als alleiniger Wirkmechanismus",
        correctAnswer = 1,
        explanation = "Lithium hemmt direkt und nicht-kompetitiv die Inositol-Monophosphatase, was den Phosphoinositid-Zyklus (PIP2-IP3-DAG) stoert (Inositol-Depletion-Hypothese). Gleichzeitig hemmt Lithium GSK-3beta -- eine Serin/Threonin-Kinase, die in Neuronen apoptotische Prozesse foerdert und Tau hyperphosphoryliert. GSK-3beta-Hemmung korreliert mit neuroprotektiven und stimmungsstabilisierenden Effekten.",
        difficulty = 5,
        funFact = "Lithium hat das schmalste therapeutische Fenster aller psychiatrischen Medikamente (0,6-1,2 mEq/L), weil es wie Natrium von Nierenkanaelen transportiert wird -- Dehydratation erhoehte die Lithium-Reabsorption und kann schnell zur Toxizitaet fuehren."
    ),

    // Question 17 - Ketamin als Antidepressivum
    Question(
        categoryId = 16,
        questionText = "Ketamin (S-Ketamin/Esketamin) wirkt als schnell wirkendes Antidepressivum innerhalb von Stunden. Welcher Mechanismus -- jenseits der direkten NMDA-Blockade -- erklaert den anhaltenden antidepressiven Effekt?",
        answerA = "Sustained NMDA-Blockade fuehrt zu kompensatorischer Hochregulation von AMPA-Rezeptoren",
        answerB = "Disinhibition von BDNF-Ausschuettung durch Hemmung tonisch aktiver inhibitorischer Interneurone, gefolgt von mTOR-Aktivierung und synaptischer Proteinsynthese -- synaptische Regenese",
        answerC = "Langanhaltende Dissoziation bewirkt therapeutische Habituation an depressive Kognitionen",
        answerD = "Ketamin-Metabolit (2R,6R)-HNK hemmt Alpha7-nikotinerge Acetylcholin-Rezeptoren",
        correctAnswer = 1,
        explanation = "Ketamin blockiert bevorzugt tonisch aktive NMDA-Rezeptoren auf GABAergen Interneuronen (Disinhibitionshypothese), was burst-artge Glutamat-Freisetzung auf kortikalen Pyramidenzellen ausloest. Dies aktiviert postsynaptische AMPA-Rezeptoren, stimuliert BDNF-TrkB-Signaltransduktion und aktiviert mTORC1 -- daraus resultiert rasche Synthese synaptischer Proteine und Wiederherstellung von Synapsen, die durch chronischen Stress verlorengegangen sind.",
        difficulty = 5,
        funFact = "Der Ketamin-Metabolit (2R,6R)-Hydroxynorketamin (HNK) wirkt antidepressiv ohne Dissoziation oder Abhaengigkeitspotenzial in Tiermodellen -- er koennte der Schluessel zur Entwicklung von Antidepressiva der naechsten Generation sein."
    ),

    // --- NEUROIMAGING ---

    // Question 18 - fMRI BOLD-Signal
    Question(
        categoryId = 16,
        questionText = "Das BOLD-Signal (Blood Oxygenation Level Dependent) in der fMRT basiert auf dem unterschiedlichen magnetischen Verhalten von Oxyhaemoglobin und Deoxyhaemoglobin. Welches physikalische Prinzip erklaert den BOLD-Kontrast?",
        answerA = "Oxyhaemoglobin ist paramagnetisch und erzeugt lokale Magnetfeldinhomogenitaeten, die T2*-Signal erhoehen",
        answerB = "Deoxyhaemoglobin ist paramagnetisch und verstaerkt lokale Magnetfeldinhomogenitaeten, die das T2*-Signal absenken -- erhohter regionaler Blutfluss verdraengt Deoxyhaemoglobin und erhoehte T2*",
        answerC = "Haemoglobin-Eisenionen erzeugen diamagnetisches Moment proportional zur Sauerstoffsaettigung",
        answerD = "BOLD beruht auf der Larmor-Frequenzverschiebung durch Haematokrit-Veraenderungen",
        correctAnswer = 1,
        explanation = "Deoxyhaemoglobin (desoxygeniertes Haemoglobin mit Fe2+ im Desoxy-Zustand) ist paramagnetisch und erzeugt lokale Magnetfeldinhomogenitaeten, die das T2*-Signal durch Spindephasierung absenken. Neuronale Aktivierung fuehrt zu einem Ueberschuss an oxygeniertem Blut (neurovaskulaere Kopplung via NO und Prostaglandine), was Deoxyhaemoglobin verdraengt -- T2* steigt, das BOLD-Signal steigt.",
        difficulty = 5,
        funFact = "Die Entdeckung der neurovaskulaeren Kopplung und des BOLD-Signals geht auf Seiji Ogawa (1990, Bell Labs) zurueck -- urspruenglich als Nebeneffekt bei MRT-Messungen an Ratten entdeckt, wurde daraus die wichtigste nicht-invasive Methode zur Hirnfunktionsmessung beim Menschen."
    ),

    // Question 19 - PET-Tracer fuer Dopamin
    Question(
        categoryId = 16,
        questionText = "Welcher PET-Tracer wird spezifisch zur Darstellung der praesynaptischen dopaminergen Aktivitaet (DOPA-Decarboxylase) in der klinischen Parkinson-Diagnostik eingesetzt?",
        answerA = "[11C]Racloprid -- bindet an postsynaptische D2/D3-Rezeptoren",
        answerB = "[18F]DOPA -- wird wie L-DOPA von DOPA-Decarboxylase zu [18F]Dopamin metabolisiert und in praesynaptischen Vesikeln gespeichert",
        answerC = "[11C]DAT -- Dopamintransporter-Ligand fuer praesynaptische Dichte",
        answerD = "[18F]FDG -- Glukoseutilisation als Surrogatmarker neuronaler Aktivitaet",
        correctAnswer = 1,
        explanation = "[18F]DOPA wird durch aromatische L-Aminosaeuredecarboxylase (DOPA-Decarboxylase) in praesynaptischen dopaminergen Neuronen zu [18F]Dopamin umgewandelt und in Vesikeln gespeichert. Der Tracer akkumuliert damit in aktiven dopaminergen Terminalen -- bei Parkinson zeigt sich das charakteristische asymmetrische Defizit im Putamen als Zeichen des nigrostriatalen Neuronenverlustes.",
        difficulty = 5,
        funFact = "Das DaTSCAN (123I-FP-CIT SPECT) ist eine guenstigere SPECT-Alternative zu PET und bindet an den Dopamintransporter -- es kann Parkinson von essentiellem Tremor unterscheiden, nicht aber Parkinson von anderen Parkinson-Syndromen wie MSA oder PSP."
    ),

    // Question 20 - Diffusion-Tensor-Imaging
    Question(
        categoryId = 16,
        questionText = "Diffusion-Tensor-Imaging (DTI) misst die Diffusionsanisotropie von Wassermolekuelen in Hirngewebe. Welcher DTI-Parameter beschreibt das Ausmass der richtungsabhaengigen Diffusion und dient als Marker fuer die Integritaet von Myelinscheiden?",
        answerA = "Mean Diffusivity (MD) -- gleichmassige Diffusion in alle Richtungen",
        answerB = "Fraktionelle Anisotropie (FA) -- Wert 0=isotrope Diffusion, 1=vollkommen gerichtete Diffusion entlang Fasern",
        answerC = "Axiale Diffusivitaet (AD) -- Diffusion parallel zur Faserorientierung",
        answerD = "Radiale Diffusivitaet (RD) -- Diffusion senkrecht zur Faserorientierung",
        correctAnswer = 1,
        explanation = "Die Fraktionelle Anisotropie (FA) ist das gebraeuchlichste DTI-Mass und beschreibt, wie stark die Diffusion richtungsabhaengig ist. In kompakten, gut myelinisierten Fasertrakten diffundieren Wassermolekuele bevorzugt entlang der Axone (FA nahe 1). Myelinschaeden (z.B. bei MS) oder axonaler Verlust erhoehen die radiale Diffusion und senken die FA -- DTI kann Plaques und Konnektivitaetsverluste sichtbar machen.",
        difficulty = 5,
        funFact = "DTI-basierte Traktographie hat eine vollstaendige 3D-Karte aller grossen Faserbahnen des menschlichen Gehirns ermoeglicht -- das Human Connectome Project nutzte hochfeldige 3-Tesla-MRT mit 7-Tesla-DTI, um das 'Kabeldiagramm' des Gehirns zu erstellen."
    ),

    // --- NEURODEGENERATIVE MECHANISMEN ---

    // Question 21 - Tau-Hyperphosphorylierung
    Question(
        categoryId = 16,
        questionText = "Tau ist ein mikrotubuli-assoziiertes Protein, das bei Alzheimer hyperphosphoryliert und zu neurofibrillaeren Tangles aggregiert. Welche Kinasen sind primaer fuer die pathologische Tau-Hyperphosphorylierung verantwortlich?",
        answerA = "CaMKII und PKA als hauptsaechliche Tau-Kinasen",
        answerB = "GSK-3beta und CDK5 als dominante Tau-Kinasen, die krankheitsrelevante Epitope phosphorylieren",
        answerC = "MAPK/ERK und mTOR als alleinige pathologische Tau-Kinasen",
        answerD = "DYRK1A (auf Chromosom 21) ist die einzige relevante Tau-Kinase",
        correctAnswer = 1,
        explanation = "GSK-3beta (Glykogen-Synthase-Kinase 3 beta) und CDK5 (Cyclin-abhaengige Kinase 5) sind die wichtigsten Kinasen fuer die pathologische Tau-Hyperphosphorylierung. GSK-3beta phosphoryliert Tau an Ser9, Thr231, Ser396/404 -- all dies sind krankheitsrelevante Epitope, die in Alzheimer-Tangles nachgewiesen werden. CDK5 (aktiviert durch p25 nach Calpain-Spaltung von p35) phosphoryliert komplementaere Sites.",
        difficulty = 5,
        funFact = "Die 'Amyloid-Kaskaden-Hypothese' postuliert, dass Abeta-Anhaefung upstream von Tau-Pathologie liegt -- Abeta aktiviert GSK-3beta und reduziert PP2A-Aktivitaet, was Tau-Hyperphosphorylierung foerdert. Klinische Studien mit Anti-Abeta-Antikoerpern zeigen tatsaechlich eine nachfolgende Verlangsamung der Tau-Pathologie."
    ),

    // Question 22 - Alpha-Synuclein und Prion-artige Ausbreitung
    Question(
        categoryId = 16,
        questionText = "Alpha-Synuclein aggregiert bei Parkinson zu Lewy-Koerpern. Welche experimentelle Evidenz unterstuetzt die Hypothese einer prion-artigen Zell-zu-Zell-Ausbreitung von Alpha-Synuclein-Aggregaten?",
        answerA = "Alpha-Synuclein-Mutationen (A53T) bewirken schnellere Fibrillenbildung in vitro",
        answerB = "Transplantierte embryonale Neurone in Parkinson-Patienten entwickeln nach 10-16 Jahren Lewy-Koerper -- Aggregate aus Host-Neuronen scheinen in Transplantat-Zellen eingedrungen zu sein",
        answerC = "Alpha-Synuclein wird sekretiert und aktiviert TLR2 auf Mikroglia",
        answerD = "Intranasale Alpha-Synuclein-Injektion bei Maeusen fuehrt zum Braak-Staging-Schema entsprechend zur Ausbreitung",
        correctAnswer = 1,
        explanation = "Kouri et al. und Olanow et al. (2008) berichteten, dass bei Parkinson-Patienten transplantierte embryonale mesenzephale Neurone nach 11-16 Jahren post-mortem Lewy-Koerper aufwiesen. Da diese Zellen jueng waren und die Traeger Jahrzehnte ueberlebt hatten, sprach dies dafuer, dass pathologisches Alpha-Synuclein aus Host-Neuronen in gesunde Transplantat-Zellen uebertragen worden war -- dies unterstuetzt das 'Prion-like spreading'-Konzept.",
        difficulty = 5,
        funFact = "Das Braak-Staging-Schema (2003) beschreibt die stereotypische kaudale-zu-rostrale Ausbreitung der Alpha-Synuclein-Pathologie: Beginn im enterischen Nervensystem und Bulbus olfactorius (Stadium 1-2), dann Hirnstamm, schliesslich Neokortex (Stadium 5-6) -- passend zu klinisch fruehen Symptomen wie Anosmie und Obstipation."
    ),

    // Question 23 - TDP-43-Pathologie
    Question(
        categoryId = 16,
        questionText = "TDP-43 (TARDBP-Protein) ist das Hauptaggregat bei ALS und FTLD-TDP. Welche normale Funktion von TDP-43 wird durch die pathologische Relokalisierung vom Nukleus ins Zytoplasma verloren?",
        answerA = "TDP-43 ist ein Transkriptionsfaktor der RNA-Polymerase II fuer motorische Neurotrophine",
        answerB = "TDP-43 reguliert als RNA-Bindeprotein RNA-Spleissen, mRNA-Stabilitaet und axonalen mRNA-Transport -- Verlust im Nukleus stoert diese essentiellen RNA-Verarbeitungsfunktionen",
        answerC = "TDP-43 ist eine Ubiquitin-E3-Ligase fuer fehlerhafte ER-Proteine im motorischen Neuron",
        answerD = "TDP-43 reguliert mitochondriale Fusion durch Interaktion mit Mitofusin-2",
        correctAnswer = 1,
        explanation = "TDP-43 ist ein nukleoplasmisches hnRNP-Protein (heterogeneous nuclear ribonucleoprotein), das Tausende von RNA-Targets bindet und deren alternatives Spleissen, mRNA-Stabilitaet und Transport ins Axon reguliert. Bei ALS und FTLD-TDP wird TDP-43 aus dem Nukleus ins Zytoplasma verlagert, wo es aggregiert -- der nukleare TDP-43-Verlust fuehrt zum Spleissfehler in Hunderten von Pre-mRNAs, darunter STMN2, das fuer axonale Regeneration essentiell ist.",
        difficulty = 5,
        funFact = "STMN2 (Stathmin-2), ein durch TDP-43 reguliertes Spleissprodukt, wird bei ALS-Patienten dramatisch reduziert. AAV-basierte Gentherapie mit kuenstlichem STMN2 verbessert in Tiermodellen die motorische Funktion -- klinische Studien sind angelaufen."
    ),

    // --- SCHLAFMEDIZIN ---

    // Question 24 - Orexin/Hypocretin-System
    Question(
        categoryId = 16,
        questionText = "Das Orexin/Hypocretin-System des lateralen Hypothalamus stabilisiert Wachheit. Durch welchen Mechanismus verursacht der Verlust von Orexin-Neuronen Narkolepsie Typ 1?",
        answerA = "Orexin-Verlust hemmt den VLPO (ventrolateraler praeptischer Kern) direkt",
        answerB = "Orexin-Neurone stabilisieren normalerweise monoaminerge Wachheits-Kerne (LC, Raphe, TMN) und hemmen VLPO -- bei Verlust kippt das 'Flip-Flop-Schalter'-System instabil und wechselt unkontrolliert zwischen Schlaf und Wachheit",
        answerC = "Fehlendes Orexin erhoehte adenosinerge Inhibition des ARAS (aufsteigendes retikulaeres Aktivierungssystem)",
        answerD = "Orexin-Verlust reduziert Histamin-Freisetzung aus dem Tuberomamillarkern und foerdert nur Non-REM-Schlaf",
        correctAnswer = 1,
        explanation = "Orexin-Neurone aktivieren alle arousal-foerdernden Kerne: LC (Noradrenalin), Raphe (Serotonin), TMN (Histamin), PPT/LDT (Acetylcholin) und hemmen gleichzeitig den schlafaktiven VLPO. Clifford Saper's 'Flip-Flop'-Modell beschreibt, wie diese gegenseitige Hemmung zwischen Schlaf- und Wachheitskernen normalerweise stabilen Zustandswechsel erzeugt -- Orexin dient als 'Finger auf der Waage'. Ohne Orexin ist der Schalter instabil und wechselt unkontrolliert.",
        difficulty = 5,
        funFact = "Narkolepsie Typ 1 ist eine Autoimmunerkrankung: HLA-DQB1*06:02 ist in ueber 95% der Patienten nachweisbar -- CD4+ T-Zellen richten sich vermutlich gegen Orexin-Neurone. Der Nachweis kam durch den Anstieg der Narkolepsie-Inzidenz nach H1N1-Pandemie-Impfung (Pandemrix) in Skandinavien 2009."
    ),

    // Question 25 - REM-Schlaf-Atonie-Mechanismus
    Question(
        categoryId = 16,
        questionText = "Waehrend des REM-Schlafs besteht muskulaere Atonie durch aktive Hemmung von Motoneuronen. Welche Neurotransmitter-Systeme vermitteln diese Atonie direkt am spinalen Motoneuron?",
        answerA = "Serotonin aus der Raphe hemmt direkt spinale Motoneurone waehrend des REM-Schlafs",
        answerB = "Glyzinerge und GABAerge Interneurone im Hirnstamm/Ruekenmark, aktiviert vom Sublaterodorsal-Kern (SLD) via Glutamat-Interneurone, senden inhibitorische Inputs zu Motoneuronen",
        answerC = "Orexin-Neurone hemmen direkt spinale Motoneurone via Orexin-Rezeptor 1 waehrend REM",
        answerD = "Adenosin akkumuliert in der Basalganglien und hemmt indirekt den Motorkortex waehrend REM",
        correctAnswer = 1,
        explanation = "Der Sublaterodorsal-Kern (SLD, auch SubLDT) des pontinen Tegmentums ist der REM-Schlaf-Atonie-Generator. SLD-Neurone projektieren zum ventralen und medialen Medulla, die glutamaterge Interneurone aktivieren, welche ihrerseits glyzinerge und GABAerge Interneurone im Vorderhornbereich des Rueckenmarks aktivieren -- diese hyperpolarisieren Motoneurone direkt. Laesionen im SLD oder Glyzin-Mangel erklaeren die REM-Schlaf-Verhaltensstoerung (RBD).",
        difficulty = 5,
        funFact = "RBD (REM-Sleep-Behavior-Disorder), bei der Patienten ihre Traeume ausagieren, ist das fruehste klinische Zeichen einer Alpha-Synuklein-Erkrankung -- 80-90% der RBD-Patienten entwickeln innerhalb von 10-15 Jahren Parkinson, MSA oder DLB."
    ),

    // --- NEUROENDOKRINOLOGIE ---

    // Question 26 - CRH-ACTH-Cortisol-Achse
    Question(
        categoryId = 16,
        questionText = "Die HPA-Achse wird durch Stressoren aktiviert. Welcher spezifische Mechanismus fuehrt zur CRH-Pulsatilitaet aus dem paraventrikulaeren Nukleus des Hypothalamus?",
        answerA = "CRH wird kontinuierlich sezerniert und ACTH-Pulsatilitaet entsteht durch Portsegment-Morphologie der Hypophyse",
        answerB = "Intrinsische Uhrengene (CLOCK/BMAL1) in PVN-Neuronen erzeugen circadiane CRH-Freisetzung; superimponiert sind ultradian Pulse durch negativen Feedback-Verzoegerungsoszillator (delayed negative feedback) zwischen Cortisol und CRH/ACTH",
        answerC = "Vasopressin (AVP) aus dem Suprachiasmatischen Nukleus ist der alleinige Pulsatilitaetsgenerator fuer ACTH",
        answerD = "Hippocampale Projektion zum PVN erzeugt 90-Minuten-Zyklen entsprechend dem BRAC (basic rest-activity cycle)",
        correctAnswer = 1,
        explanation = "Die circadiane CRH-Rhythmik entsteht durch Uhrengen-Aktivitaet im PVN (Peak morgens). Ueberlagert sind ultradiane Pulse (~60-90 min), die durch einen verzoegerten negativen Feedback-Mechanismus entstehen: Cortisol hemmt CRH/ACTH mit Zeitverzoegerung, was zu Oszillationen fuehrt. AVP aus PVN-Neuronen synergiert mit CRH und verstaerkt ACTH-Pulse, ist aber kein alleiniger Generator.",
        difficulty = 5,
        funFact = "Chronischer Stress fuehrt zu morphologischen Veraenderungen im CRH-Neuron: vermehrte Synapsenbildung durch glutamaterge afferente Fasern aus Amygdala und verringerte Hemmung durch GABAerge Interneurone -- das Netzwerk wird plastisch sensitiver fuer zukuenftige Stressoren."
    ),

    // Question 27 - GnRH-Pulsatilitaet und Reproduktion
    Question(
        categoryId = 16,
        questionText = "GnRH wird pulsatil aus dem Hypothalamus sezerniert. Was geschieht bei kontinuierlicher (nicht-pulsatiler) GnRH-Stimulation der Hypophyse und welche klinische Anwendung ergibt sich daraus?",
        answerA = "Kontinuierliches GnRH steigert LH/FSH durch kumulative Summation des Signals",
        answerB = "Kontinuierliches GnRH fuehrt zur Down-Regulation von GnRH-Rezeptoren und konsekutiver LH/FSH-Suppression -- therapeutisch genutzt bei Prostatakarzinom und Endometriose (GnRH-Agonisten wie Leuprolid)",
        answerC = "Kontinuierliches GnRH hemmt selektiv FSH aber nicht LH durch Desensitivierung des Gonadotropen",
        answerD = "GnRH-Agonisten als Dauergabe stimulieren ausschliesslich FSH und werden zur kontrollierten Ovarialhyperstimulation eingesetzt",
        correctAnswer = 1,
        explanation = "GnRH-Rezeptoren auf gonadotrophen Hypophysenzellen werden bei kontinuierlicher Stimulation internalisiert und down-reguliert (Desensitivierung). Die Folge ist ein Abfall von LH und FSH -- und in der Folge von Testosteron und Oestrogen -- auf Kastrationsniveau. Dieses Prinzip wird therapeutisch durch GnRH-Agonisten (Leuprolid, Goserelin) bei hormonabhaengigen Tumoren und Endometriose genutzt.",
        difficulty = 5,
        funFact = "GnRH-Antagonisten (Degarelix) blockieren sofort ohne initialen Flare -- GnRH-Agonisten hingegen fuehren initial zu einem Testosteron-Anstieg ('Flare') vor der Suppression, der bei Patienten mit fortgeschrittenem Prostatakarzinom voruebergehend klinisch gefaehrlich sein kann."
    ),

    // Question 28 - Kisspeptin als GnRH-Regulator
    Question(
        categoryId = 16,
        questionText = "Kisspeptin-Neurone sind der Hauptregulator der pulsatilen GnRH-Sekretion. Welche anatomischen Kisspeptin-Populationen steuern jeweils GnRH-Pulse und den LH-Surge?",
        answerA = "Arkuate Kern-Kisspeptin (KNDy-Neurone) generieren GnRH-Pulse; anteroventraler periventrikulaerer Nukleus (AVPV) vermittelt Oestrogen-positiven Feedback und LH-Surge",
        answerB = "Beide Kisspeptin-Populationen sind funktionell identisch und dienen nur der Feedback-Registrierung",
        answerC = "AVPV-Kisspeptin generiert Pulse; Arkuate Kisspeptin vermittelt nur negative Feedback-Hemmung",
        answerD = "Kisspeptin-Neurone sind ausschliesslich im Nucleus supraopticus und regulieren GnRH via Vasopressin",
        correctAnswer = 0,
        explanation = "KNDy-Neurone im Arkuaten Kern co-exprimieren Kisspeptin, Neurokinin B (NKB) und Dynorphin und bilden den 'GnRH-Pulsgenerator' -- NKB stimuliert Kisspeptin-Freisetzung, Dynorphin hemmt sie, was die Pulsatilitaet erzeugt. AVPV-Kisspeptin-Neurone vermitteln den oestrogen-positiven Feedback (LH-Surge) praeovulatorisch. Diese Trennung erklaert, warum Frauen nach der Menopause erhoehte pulsatile LH-Freisetzung zeigen (fehlende Hemmung), aber keinen Surge (intakter AVPV-Feedback notig).",
        difficulty = 5,
        funFact = "Inaktivierende Mutationen im KISS1- oder KISS1R-Gen fuehren zu hypogonadotropem Hypogonadismus und ausbleibender Pubertaet -- die Entdeckung 2003 definierte Kisspeptin als essenziellen 'Schluesselsignal' fuer die Pubertat. Aktivierende KISS1R-Mutationen fuehren zur peripheren Pubertas praecox."
    ),

    // --- BEWUSSTSEIN UND ANAESTHESIE ---

    // Question 29 - Integrierte Informationstheorie (IIT)
    Question(
        categoryId = 16,
        questionText = "Die Integrierte Informationstheorie (IIT) von Giulio Tononi postuliert ein Mass fuer Bewusstsein namens Phi. Was beschreibt Phi und welche neurobiologische Implikation ergibt sich?",
        answerA = "Phi misst die Feuerrate kortikaler Neurone -- hohes Phi bedeutet hohe Feuerrate und damit Bewusstsein",
        answerB = "Phi ist ein Mass fuer die Information, die ein System als Ganzes generiert jenseits seiner Teile (integrative Information) -- Systeme mit hohem Phi sind bewusst; Kleinhirn hat trotz vieler Neurone wenig Phi wegen modularer Architektur",
        answerC = "Phi misst die Synchronizitaet von Gamma-Oszillationen im Thalamo-kortikalen System",
        answerD = "Phi ist die mathematische Verbindung zwischen neuronaler Entropie und phenomenalem Bewusstsein via Boltzmann-Gleichung",
        correctAnswer = 1,
        explanation = "Phi in der IIT beschreibt, wie viel kausale Information ein System als integriertes Ganzes erzeugt -- also Information, die im Gesamtsystem vorhanden ist, aber nicht auf die Information der Teile reduzierbar ist. Systeme mit feed-forward-Architektur (wie Kleinhirn) haben wenig Phi trotz vieler Neurone. Das Kleinhirn hat 69 Mrd. Neurone (viermal der Kortex), ist aber fuer Bewusstsein nicht notwendig -- was IIT erklaert: seine modulare, nicht-reziproke Architektur erzeugt wenig Phi.",
        difficulty = 5,
        funFact = "IIT macht testbare Vorhersagen: Ein zweidimensionales Gitter aus Transistoren koennte grundsaetzlich hoehere Phi-Werte als ein biologisches neuronales Netzwerk haben -- ein kontrovers diskutierter Aspekt, da er impliziert, dass nicht-biologische Systeme bewusst sein koennten."
    ),

    // Question 30 - Propofol-Wirkmechanismus
    Question(
        categoryId = 16,
        questionText = "Propofol ist das am haeufigsten verwendete intravenoese Anaesthetikum. An welchem Rezeptor wirkt Propofol primar anaesthetisch und welches klinische Phaenomen entsteht bei Hochdosierung?",
        answerA = "Propofol hemmt N-Typ-Kalziumkanaele und NMDA-Rezeptoren als Hauptmechanismus",
        answerB = "Propofol potenziert GABA-A-Rezeptoren als positiver allosterischer Modulator -- bei hochdosierten Infusionen kann Propofol-Infusionssyndrom (PRIS) mit Azidose, Rhabdomyolyse und Herzversagen entstehen",
        answerC = "Propofol aktiviert muskarinische Acetylcholin-Rezeptoren und induziert retrograde Amnesie",
        answerD = "Propofol blockiert praesynaptische HCN1-Kanaele (Ih-Strom) als Hauptmechanismus",
        correctAnswer = 1,
        explanation = "Propofol potenziert GABA-A-Rezeptoren durch Bindung an eine allosterische Stelle (transmembranose Domaine, beta2/beta3-Untereinheit), verlangert Kanaloefffnungszeiten und verstaerkt die chloridvermittelte Hyperpolarisation. In hohen Konzentrationen aktiviert es GABA-A direkt. Das Propofol-Infusionssyndrom (PRIS) tritt bei langen Hochdosisinfusionen auf (>4 mg/kg/h) und ist durch mitochondriale Toxizitaet bedingt -- vermutlich Hemmung der Elektronentransportkette.",
        difficulty = 5,
        funFact = "Propofol erzeugt typischerweise ein charakteristisches EEG-Muster: Burst-Suppression -- abwechselnde Perioden hoher Aktivitaet ('Bursts') und absoluter Stille ('Suppression'). Dieses Muster entspricht mathematisch einem 'lethargischen' Zustand mit minimalem Phi in der IIT."
    ),

    // --- WEITERE NEUROWISSENSCHAFT ---

    // Question 31 - Blut-Hirn-Schranke Mechanismus
    Question(
        categoryId = 16,
        questionText = "Die Blut-Hirn-Schranke (BHS) wird durch zerebrale Kapillar-Endothelzellen gebildet. Welche molekulare Besonderheit der BHS-Endothelzellen erklaert ihre ausgepraegte Selektivitaet im Vergleich zu peripheren Kapillaren?",
        answerA = "BHS-Endothelzellen exprimieren keine Glukosetransporter und sind daher impermeable fuer hydrophile Molekuele",
        answerB = "Ausgepraegtes Tight-Junction-Netzwerk (Claudin-5, Occludin, ZO-Proteine) eliminiert parazellulare Diffusion; fehlende Fenestration; minimale Transzytose; ausgepraegter Efflux (P-Glykoprotein, BCRP)",
        answerC = "Hochdichte Basalmembran aus Kollagen Typ IV schirmt das Gehirn vollstaendig ab",
        answerD = "Perizyten produzieren Angiopoietin-2, das Endothelzellen veranlasst, alle Transportproteine herunter zu regulieren",
        correctAnswer = 1,
        explanation = "BHS-Endothelzellen exprimieren ausgepraegtes Tight-Junction-Netzwerk (Claudin-5, Occludin, JAM, ZO-1), das parazellulare Diffusion auf nahezu null reduziert (transepitheliaer elektrischer Widerstand ~1800 Ohm*cm2 vs. ~33 Ohm*cm2 in peripheren Kapillaren). Dazu kommen fehlende Fenestration, minimale Makropinozytose und ausgepraegter Efflux durch ABC-Transporter (P-Glykoprotein MDR1, BCRP). Astrozytaere Endfeeten und Perizyten induzieren/erhalten diese Eigenschaften.",
        difficulty = 5,
        funFact = "P-Glykoprotein (MDR1) ist der Hauptgrund, warum viele potenziell wirksame ZNS-Medikamente klinisch versagen -- MDR1-Substrate werden aktiv aus Endothelzellen ins Blut zurueckgepumpt. Durch MDR1-Induktion durch Antiepileptika kann Pharmakoresistenz bei Epilepsie entstehen."
    ),

    // Question 32 - Serotonerge Bahnen und 5-HT-Rezeptoren
    Question(
        categoryId = 16,
        questionText = "5-HT3-Rezeptoren sind die einzigen ionotropen Serotonin-Rezeptoren -- alle anderen sind metabotrope GPCRs. Welche klinisch wichtige Eigenschaft unterscheidet 5-HT3-Rezeptoren von 5-HT1A-Rezeptoren bezueglich Signaltransduktion und Pharmakologie?",
        answerA = "5-HT3 ist Gi-gekoppelt und hemmt cAMP; 5-HT1A oeffnet Kationenkanaele",
        answerB = "5-HT3 ist ein Na+/K+-permeabler Ionenkanal (LGI superfamily) mit schneller Depolarisation; 5-HT1A ist Gi-gekoppelt, hemmt cAMP und oeffnet GIRK-Kanaele -- 5-HT3-Antagonisten (Ondansetron) hemmen Uebelkeit; 5-HT1A-Agonisten (Buspiron) wirken anxiolytisch",
        answerC = "Beide Rezeptoren sind funktionell identisch und unterscheiden sich nur in der Aminosaeursequenz der extrazellularen Bindestelle",
        answerD = "5-HT3 ist Gs-gekoppelt und steigert cAMP; 5-HT1A aktiviert PLC und erhoehte IP3",
        correctAnswer = 1,
        explanation = "5-HT3 ist ein ligandgesteuerter Ionenkanal (LGI -- Ligand-Gated Ion Channel) der Cys-loop-Familie (verwandt mit nAChR, GABA-A, Glycin-Rezeptoren) und vermittelt schnelle Na+/K+-Leitfaehigkeit mit Depolarisation. 5-HT1A hingegen ist Gi/Go-gekoppelt: hemmt Adenylatzyklase (senkt cAMP) und aktiviert GIRK-Kanaele (Hyperpolarisation). 5-HT3-Antagonisten (Ondansetron, Granisetron) hemmen Uebelkeit/Erbrechen; 5-HT1A-Agonisten/-Partialagonisten (Buspiron, Tandospiron) wirken anxiolytisch.",
        difficulty = 5,
        funFact = "Ondansetron wurde als erster selektiver 5-HT3-Antagonist 1990 zugelassen und revolutionierte die Chemotherapie-bedingte Uebelkeit -- vorher war die Uebelkeit durch Chemotherapie so schwer, dass viele Patienten die Behandlung abbrachen."
    ),

    // Question 33 - Noradrenerges System und Locus Coeruleus
    Question(
        categoryId = 16,
        questionText = "Der Locus coeruleus (LC) ist die wichtigste noradrenerge Quelle des Gehirns. Welches elektrophysiologische Muster kennzeichnet LC-Neurone im Wachzustand versus Schlaf und welche Funktion hat der LC bei Aufmerksamkeit?",
        answerA = "LC-Neurone feuern gleichmaessig mit 5-10 Hz im Wachen und hoeren im REM-Schlaf nahezu vollstaendig auf; 'Phasisch-tonisch'-Muster reguliert Gain der kortikalen Verarbeitung",
        answerB = "LC-Neurone feuern schnell im NREM-Schlaf und langsam im Wachen; Aufmerksamkeitssteuerung ist eine Funktion des Raphe-Kerns",
        answerC = "LC-Neurone haben tonisches Feuern bei 20-30 Hz und zeigen keine Schlaf-Wach-Modulation",
        answerD = "LC ist ausschliesslich fuer die emotionale Valenzkodierung zustaendig und nicht fuer Aufmerksamkeit",
        correctAnswer = 0,
        explanation = "LC-Neurone zeigen tonisches Feuern (0,5-5 Hz im Wachen, gering im NREM-Schlaf, nahezu Stille im REM-Schlaf). Aston-Jones's Adaptive Gain Theory: Tonische LC-Aktivitaet setzt Noradrenalin-Baseline, die den Signal-Rausch-Abstand (Gain) kortikaler Reaktionen moduliert. Phasische LC-Burst-Antworten auf salienter Stimuli erhoehen kurzfristig die Signal-Rausch-Differenzierung und foerdern Umschalten zwischen Aufgaben.",
        difficulty = 5,
        funFact = "Atomoxetin (ADHD-Medikament ohne Stimulanzien-Charakter) hemmt spezifisch den Noradrenalin-Transporter (NET) im praefrontalen Kortex -- es verstaerkt den noradrenergen Effekt auf Alpha2A-Rezeptoren, die postsynaptische PFC-Netzwerke stabilisieren und Ablenkbarkeit reduzieren."
    ),

    // Question 34 - Cholinergisches System und Basalganglien
    Question(
        categoryId = 16,
        questionText = "Der Nucleus basalis Meynert (NBM) ist die Hauptquelle cholinerger Innervation des Neokortex. Welches neuropathologische Befundmuster im NBM ist charakteristisch fuer Alzheimer-Erkrankung und korreliert klinisch mit kognitivem Verfall?",
        answerA = "NBM-Neurone akkumulieren Lewy-Koerper aus Alpha-Synuklein -- dies erklaert die Ueberlappung von Alzheimer und Parkinson",
        answerB = "Massiver neuronaler Verlust im NBM (bis 80%) mit neurofibrillaren Tangles -- Korrelation des Acetylcholin-Defizits im Kortex mit kognitivem Defizit fuehrt zur cholinergen Hypothese der Alzheimer-Erkrankung",
        answerC = "NBM-Neurone verlieren ihre cholinerge Phenotyp-Expression ohne Zelltod (phenotypic silencing)",
        answerD = "Amyloid-Plaques blockieren cholinerge Synapsen selektiv im frontalen Kortex ohne NBM-Schaedigung",
        correctAnswer = 1,
        explanation = "Post-mortem-Studien von Whitehouse et al. (1982) zeigten massiven Neuronenverlust (>75%) im NBM von Alzheimer-Patienten mit entsprechendem ChAT-Aktivitaetsverlust im Kortex. Dies bildete die Grundlage der 'Cholinergen Hypothese der Alzheimer-Erkrankung' und der Entwicklung von Acetylcholinesterase-Hemmern (Donepezil, Galantamin, Rivastigmin) als symptomatische Therapie.",
        difficulty = 5,
        funFact = "Die cholinerge Hypothese erklaert nur die symptomatische Therapie -- Cholinesterasehemmer verbessern Kognition, verlangsamen aber nicht den Krankheitsprozess. Amyloid und Tau bleiben die kausalen Pathologien. Dennoch sind Cholinesterasehemmer nach 40 Jahren noch die am meisten verschriebene Alzheimer-Therapie."
    ),

    // Question 35 - Myelin und Oligodendrozyten
    Question(
        categoryId = 16,
        questionText = "Oligodendrozyten produzieren Myelin im ZNS. Welches Hauptprotein (ueber 30% des Myelin-Trockengewichts) ist bei der demyelinisierenden Erkrankung Multiple Sklerose Zielantigene von autoreaktiven T-Zellen?",
        answerA = "PLP (Proteolipid-Protein) als Hauptmembranprotein des Myelins ist das einzige MS-Antigen",
        answerB = "MBP (Myelin Basic Protein), MOG (Myelin Oligodendrozyten-Glykoprotein) und PLP sind relevante ZNS-Myelinantigene bei MS -- Anti-MOG-Antikoerper definieren eine eigene klinische Entitaet (MOGAD)",
        answerC = "MAG (Myelin-Assoziiertes Glykoprotein) ist das alleinige MS-relevante Antigen, da es peranodal exponiert ist",
        answerD = "CNPase (2',3'-cyclic nucleotide 3'-phosphohydrolase) ist das Hauptzielantigen bei MS",
        correctAnswer = 1,
        explanation = "Bei MS sind MBP, PLP und MOG beschriebene T-Zell-Antigene. Anti-MOG-IgG-Antikoerper definieren MOGAD (MOG-Antibody-Associated Disease) -- eine klinisch und pathophysiologisch eigenstaendige demyelinisierende Erkrankung mit anderem Verlauf, Behandlungsansprechen und MRT-Muster als klassische MS. MOGAD ist oft stark relapsierend und spricht gut auf Steroide an.",
        difficulty = 5,
        funFact = "MOG (Myelin Oligodendrozyten-Glykoprotein) ist an der aeussersten Myelinschicht exponiert -- evolutionaer gedacht, um Komplement-Aktivierung zu regulieren. Bei MOGAD binden Anti-MOG-IgG1-Antikoerper daran und aktivieren Komplement direkt auf der Myelinscheide -- ein anderer Pathomechanismus als T-Zell-vermittelte MS."
    ),

    // Question 36 - GABAerges System und Benzodiazepine
    Question(
        categoryId = 16,
        questionText = "Benzodiazepine binden an GABA-A-Rezeptoren und wirken als positive allosterische Modulatoren. An welcher Stelle des GABA-A-Rezeptors binden Benzodiazepine und welche Untereinheitenzusammensetzung ist dafuer Voraussetzung?",
        answerA = "Benzodiazepine binden an die GABA-Bindestelle und konkurrieren mit GABA um denselben Ort",
        answerB = "Benzodiazepine binden an die Interface zwischen alpha- und gamma2-Untereinheit (Benzodiazepine-Stelle) -- Voraussetzung ist das Vorhandensein einer alpha1-, alpha2-, alpha3- oder alpha5-Untereinheit zusammen mit gamma2",
        answerC = "Benzodiazepine binden am delta-Untereinheiteninterface und wirken nur auf extrasynaptische GABA-A-Rezeptoren",
        answerD = "Benzodiazepine binden an die beta-Untereinheit (gleicher Ort wie Etomidat und Propofol)",
        correctAnswer = 1,
        explanation = "Benzodiazepine binden an eine allosterische Stelle an der Grenzflaeche zwischen alpha- und gamma2-Untereinheit des GABA-A-Rezeptors. Rezeptoren mit alpha4- oder alpha6-Untereinheiten binden keine klassischen Benzodiazepine. alpha1-enthaltende Rezeptoren vermitteln Sedierung/Amnesie; alpha2/alpha3 vermitteln Anxiolyse und Muskelrelaxation -- Grundlage fuer die Entwicklung selektiver anxiolytischer Verbindungen ohne Sedierung.",
        difficulty = 5,
        funFact = "Zolpidem ('Z-drugs') bindet praeferenziell an alpha1-enthaltende GABA-A-Rezeptoren (sedierende Wirkung ohne Anxiolyse) -- trotzdem zeigt es Abhaengigkeitspotenzial und paradoxe Erregungsreaktionen, insbesondere bei hoeheren Dosierungen oder genetischen Varianten der alpha1-Untereinheit."
    ),

    // Question 37 - Glutamat-Exzitotoxizitaet
    Question(
        categoryId = 16,
        questionText = "Exzitotoxizitaet ist ein Schluessel-Pathomechanismus bei Schlaganfall und Hirntrauma. Welche intrazellulaere Kaskade loest Ca2+-Ueberladung nach massiver NMDA-Rezeptor-Aktivierung aus?",
        answerA = "Ca2+ aktiviert direkt Caspase-3 und loest sofortige Apoptose aus",
        answerB = "Ca2+-Ueberladung aktiviert Calpaine (Proteasen), Phospholipasen A2/C, Stickstoffmonoxidsynthase (nNOS) und mitochondriale Permeabilitaets-Transition -- diese Kombination fuehrt zu Zytoskelettzerfall, Lipidperoxidation, NO/Peroxynitrit-Toxizitaet und ATP-Verlust",
        answerC = "Ca2+ aktiviert PKC, die AMPA-Rezeptoren phosphoryliert und dadurch Na+-Ueberladung bewirkt",
        answerD = "Ca2+-Ueberladung fuehrt ausschliesslich zu ER-Stress durch Fehlfaltung von Proteinen im ER-Lumen",
        correctAnswer = 1,
        explanation = "Die exzitotoxische Ca2+-Ueberladung aktiviert parallel mehrere destruktive Enzymsysteme: Calpaene spalten Spektrin, alpha-II-Actinin und Tau; PLA2 setzt Arachidonsaeure und proinflammatorische Eikosanoide frei; nNOS produziert NO, das mit Superoxid zu hochreaktivem Peroxynitrit reagiert; mitochondriales Ca2+ loest den Permeabilitaetstransitions-Poren-Komplex aus -- dies fuehrt zu ATP-Kollaps, Cytochrom-C-Freisetzung und Zelltod.",
        difficulty = 5,
        funFact = "NMDA-Antagonisten schuetzten in Tiermodellen exzellent vor Exzitotoxizitaet, scheiterten aber alle in klinischen Schlaganfall-Studien -- entweder zu toxisch (MK-801) oder das therapeutische Fenster war zu kurz. Derzeit werden NMDA-Untereinheits-selektive Antagonisten (GluN2B-selektiv) als besser vertraegliche Alternative untersucht."
    ),

    // Question 38 - Neurotrophine und TrkB-Signalweg
    Question(
        categoryId = 16,
        questionText = "BDNF (Brain-Derived Neurotrophic Factor) bindet primar an TrkB-Rezeptoren. Welcher intrazellulaere Signalweg vermittelt die neuroprotektiven und plastizitaetssteigernden Effekte von BDNF auf Ebene der Gentranskription?",
        answerA = "BDNF-TrkB aktiviert JAK-STAT3 als einzige plastizitaetsrelevante Kaskade",
        answerB = "TrkB-Aktivierung rekrutiert Grb2/SOS -> Ras -> MAPK/ERK -> RSK/CREB-Phosphorylierung; parallel PI3K -> Akt -> GSK-3beta-Hemmung und mTORC1-Aktivierung -- CREB vermittelt Transkription von BDNF, BCL-2 und plastizitaetsrelevanten Genen",
        answerC = "BDNF-TrkB aktiviert ausschliesslich PLC-gamma -> IP3 -> Ca2+ -> Calcineurin -> NFAT",
        answerD = "TrkB-Signalweg ist identisch mit Insulin-Rezeptor-Signalweg und aktiviert FOXO-Transkriptionsfaktoren",
        correctAnswer = 1,
        explanation = "TrkB-Autophosphorylierung nach BDNF-Bindung aktiviert drei Hauptkaskaden: (1) Grb2/SOS -> Ras -> Raf -> MEK -> ERK -> RSK -> CREB-Phosphorylierung und Gentranskription (Plastizitaet, Ueberleben); (2) PI3K -> PIP3 -> PDK1 -> Akt -> mTORC1 (Proteinsynthese, Ueberleben) und GSK-3beta-Hemmung; (3) PLC-gamma -> IP3 + DAG -> Ca2+/CaMKIV + PKC (lokale Synapsenstaerkung). CREB als nukleaerer Integrationspunkt aktiviert plastizitaetsrelevante Gene.",
        difficulty = 5,
        funFact = "Fast alle antidepressiven Behandlungen (SSRI, Lithium, Elektrokrampftherapie, Sport) erhoehen langfristig BDNF im Hippokampus -- BDNF ist moeglicherweise das gemeinsame finale Molekuel, ueber das diverse antidepressive Strategien wirken. Die 'Neurotrophin-Hypothese der Depression' postuliert BDNF-Mangel als zentrales Substrat."
    ),

    // Question 39 - Kortikale Oszillationen und Kognition
    Question(
        categoryId = 16,
        questionText = "Gamma-Oszillationen (30-100 Hz) im Hippokampus und praefrontalen Kortex werden mit kognitiver Bindung und Arbeitsgedaechtnis assoziiert. Welches zellulaere Netzwerk generiert kortikale Gamma-Oszillationen?",
        answerA = "Pyramidenzellen feuern autonom mit Gamma-Frequenz durch intrinsische HCN1-Kanaele",
        answerB = "PING (Pyramidal-Interneuron-Network Gamma) -- schnelle GABAerge Korbzellen (PV+) werden durch Pyramidenzellen aktiviert und hemmen diese zurueck, erzeugen so Feedback-Oszillationen im Gamma-Bereich",
        answerC = "Thalamische Relay-Neurone senden 40-Hz-Signale an den Kortex, der sie nur weiterleitet",
        answerD = "Gap-Junction-Netzwerke zwischen Oligodendrozyten generieren Gamma-Rhythmen durch elektrotonische Kopplung",
        correctAnswer = 1,
        explanation = "PING (Pyramidal-Interneuron-Network Gamma) ist der dominante Gamma-Generierungsmechanismus: Pyramidenzellen aktivieren PV+-Korbzellen (schnelle GABAerge Interneurone), die nach kurzer Verzoegerung zurueckhaemmen -- durch diese Rueckkopplungsschleife entsteht das Gamma-Rhythmus. Alternativ koennen PV+-Interneuron-Netzwerke durch mutuelle hemmende Verbindungen ING (Interneuron-Network Gamma) erzeugen.",
        difficulty = 5,
        funFact = "PV+-Korbzellen sind die einzigen Interneurone, die perisomatic inhibition auf Pyramidenzellen ausueben -- sie kontrollieren 'Timing und Output' der Pyramidenzellen. PV-Zell-Defizite bei Schizophrenie (reduziertes PV und GAD67) erklaeren Gamma-Oszillationsdefizite und kognitive Dysfunktion bei dieser Erkrankung."
    ),

    // Question 40 - Spinale Schmerzverarbeitung
    Question(
        categoryId = 16,
        questionText = "Wind-up im Hinterhorn des Rueckenmarks beschreibt die progressive Amplifikation der Antwort auf wiederholte Schmerzreize. Welcher Rezeptor ist fuer diesen Prozess der synaptischen Sensibilisierung entscheidend?",
        answerA = "AMPA-Rezeptoren mit GluA2-Untereinheit (Ca2+-impermeable)",
        answerB = "NMDA-Rezeptoren -- repetitive C-Faser-Aktivierung loest repetitive Substantia-P/Glutamat-Freisetzung aus, die nach Mg2+-Blockade-Aufhebung NMDA-Rezeptoren aktiviert und progressive Amplifikation erzeugt",
        answerC = "NK1-Rezeptoren allein durch Substanz-P sind ausreichend fuer Wind-up",
        answerD = "TRPV1-Rezeptoren auf spinalen Hinterhornneuronen vermitteln Wind-up",
        correctAnswer = 1,
        explanation = "C-Faser-Stimulation setzt repetitiv Glutamat + Substanz P in Hinterhornneurone frei. Substanz-P an NK1-Rezeptoren depolarisiert das Neuron und hebt schrittweise die Mg2+-Blockade der NMDA-Rezeptoren auf -- bei ausreichender Depolarisation oeffnen NMDA-Kanaele vollstaendig. Der Ca2+-Einstrom aktiviert PKC und NOS, die synaptische Verstaerkung aufrechterhalten. Wind-up ist das spinale Korrelat der zentralen Sensibilisierung bei chronischen Schmerzzustaenden.",
        difficulty = 5,
        funFact = "Ketamin als NMDA-Antagonist blockiert Wind-up effektiv -- seine Anwendung als 'Opioid-sparer' und bei opioid-induzierter Hyperalgesie basiert genau auf diesem Mechanismus. Subanasthetische Ketamin-Infusionen bei chronischen Schmerzzustaenden reduzieren zentrale Sensibilisierung."
    ),

    // Question 41 - Adenosin und Schlaf-Homeostase
    Question(
        categoryId = 16,
        questionText = "Adenosin akkumuliert waehrend des Wachseins und induziert Schlaf als Teil der homeostatischen Schlafregulation. In welchem Hirnbereich ist die Adenosin-Akkumulation fuer die schlaffoerdernde Wirkung am wichtigsten, und an welchem Rezeptor wirkt sie?",
        answerA = "Adenosin akkumuliert im Striatum und aktiviert A2A-Rezeptoren, die dopaminerge D2-Rezeptoren hemmen",
        answerB = "Adenosin akkumuliert im basalen Vorderhirn (besonders Meynert-Basalkern und benachbarte Areale) und hemmt cholinerge Wachheitsneurone durch A1-Rezeptoren sowie aktiviert schlafaktive Neurone durch A2A-Rezeptoren im Nucleus accumbens-Gebiet",
        answerC = "Adenosin wirkt ausschliesslich im Hirnstamm (ARAS) durch A1-Rezeptoren auf monoaminerge Neurone",
        answerD = "Adenosin akkumuliert im Kortex und hemmt Pyramidenzellen direkt durch prasynaptische A1-Rezeptoren",
        correctAnswer = 1,
        explanation = "Porkka-Heiskanens Arbeiten zeigten, dass Adenosin besonders im basalen Vorderhirn (BF) ansteigt. Adenosin hemmt cholinerge BF-Neurone via A1-Rezeptoren (Wachheitshemmung) und aktiviert GABAerge schlafpromovente BF-Neurone und VLPO-Neurone via A2A-Rezeptoren -- der Netto-Effekt ist Schlaefinduktion. Koffein als A1/A2A-Antagonist blockiert diesen Effekt und foerdert Wachheit.",
        difficulty = 5,
        funFact = "Koffein haelt nicht nur wach, indem es Adenosin blockiert -- es verhindert dadurch, dass die schlaffoerdernde VLPO-Aktivierung einsetzt. Bei chronischer Einnahme werden Adenosin-Rezeptoren hochreguliert -- erklaert den 'Koffein-Hunger' am Morgen, bis das erste Koffein die Rezeptoren besetzt."
    ),

    // Question 42 - Prionerkrankungen und fehlgefaltete Proteine
    Question(
        categoryId = 16,
        questionText = "Prionerkrankungen (z.B. Creutzfeldt-Jakob-Krankheit) werden durch fehlerhafte PrP^Sc-Konformationen verursacht. Welches biochemische Prinzip der 'Prion-Hypothese' von Stanley Prusiner erklaert die Infektioesitaet ohne Nukleinsaeure?",
        answerA = "PrP^Sc enthaelt eine versteckte RNA, die als Nukleinsaeure-freier Informationstraeger fungiert",
        answerB = "PrP^Sc erzwingt konformationelle Umfaltung des normalen zellulaeren PrP^C durch direkte Protein-Protein-Interaktion -- das neue PrP^Sc kann wieder PrP^C umfalten (Templated Misfolding), was zur exponentiellen Aggregation ohne genetisches Material fuehrt",
        answerC = "Prionen enthalten Lipid-Membrananteile als infektioeses Prinzip",
        answerD = "PrP^Sc-Aggregate sind nicht infektioes, die Krankheit wird durch somatische Mutationen im PRNP-Gen spontan ausgeloest",
        correctAnswer = 1,
        explanation = "Prusiner's Prion-Hypothese (Nobelpreis 1997): PrP^Sc, die pathologische beta-Faltblatt-reiche Konformation des normalen alpha-Helix-reichen PrP^C, wirkt als 'Schablone' (Template) und zwingt neu synthetisiertes PrP^C in die PrP^Sc-Konformation. Diese Konformationskonversion ohne Nukleinsaeure erklaert die Infektioesitaet. Die Inkubationszeit ist die Zeit, bis genug PrP^Sc akkumuliert ist, um Neuronenverlust zu bewirken.",
        difficulty = 5,
        funFact = "Das Konzept des 'Prion-like spreading' auf andere Proteine (Tau, Alpha-Synuklein, TDP-43, Abeta) hat die Neurodegeneration revolutioniert -- es erklaert, warum Alzheimer und Parkinson auf bestimmten Hirnregionen beginnen und sich dann anatomisch ausbreiten, aehnlich wie klassische Prionerkrankungen."
    ),

    // Question 43 - Hypothalamische Kerngebiete und Circadianrhythmik
    Question(
        categoryId = 16,
        questionText = "Der Suprachiasmatische Nukleus (SCN) ist der Master-Circadianoscillator. Welches Genprodukt bildet den negativen Arm der transkriptionell-translationalen Feedback-Schleife (TTFL), die Circadianrhythmik erzeugt?",
        answerA = "CLOCK und BMAL1 bilden den negativen Arm; PER und CRY sind die positiven Stimulatoren",
        answerB = "PER1/2/3 und CRY1/2 akkumulieren tagsaeber und hemmen bei ausreichender Konzentration nachts ihren eigenen Transkriptionsaktivator (CLOCK:BMAL1-Heterodimer) -- das ist der negativen Feedback-Arm der TTFL",
        answerC = "REV-ERB-alpha und ROR-alpha bilden den alleinigen negativen Feedback-Arm",
        answerD = "TIM (Timeless) und DBT (Doubletime) bilden den Feedback-Komplex beim Menschen, analog zur Drosophila-Uhr",
        correctAnswer = 1,
        explanation = "In der saeugetierspezifischen TTFL: CLOCK:BMAL1-Heterodimer aktiviert PER1/2/3 und CRY1/2-Transkription. PER und CRY-Proteine akkumulieren, bilden Komplexe und werden phosphoryliert (CKI-epsilon), was ihre Stabilitaet und Kernimport reguliert. Im Kern hemmt PER:CRY den CLOCK:BMAL1-Komplex -- dieser negative Feedback erzeugt ~24h-Periodizitaet. REV-ERB/ROR bilden eine Hilfsschleife, die BMAL1-Transkription stabilisiert.",
        difficulty = 5,
        funFact = "Mutationen in CRY1 und PER2 beim Menschen fuehren zu familiaerem Schlafphasensyndrom (FASPS) -- Betroffene schlafen und wachen 4-6 Stunden frueher als Normalpersonen und koennen dies nicht willkuerlich aendern. Das beweist, dass die molekulare Uhr direkt Schlaf-Wach-Zyklen beim Menschen steuert."
    ),

    // Question 44 - Dopamin-Rezeptor-Klassifikation
    Question(
        categoryId = 16,
        questionText = "Dopaminrezeptoren werden in D1-artige (D1, D5) und D2-artige (D2, D3, D4) Familien eingeteilt. Welche funktionelle Eigenschaft unterscheidet sie auf Second-Messenger-Ebene und welche Bedeutung hat dies fuer die Schizophrenie-Pharmakologie?",
        answerA = "D1-artige Rezeptoren sind ionotrop; D2-artige sind GPCR -- alle Antipsychotika blockieren D1-Rezeptoren",
        answerB = "D1/D5 sind Gs-gekoppelt (steigern cAMP, aktivieren PKA); D2/D3/D4 sind Gi/Go-gekoppelt (hemmen cAMP, aktivieren GIRK) -- Antipsychotika blockieren D2/D3, was mesolimbische Positivsymptome bessert, mesokortikale Negativsymptome aber verschlechtert",
        answerC = "D1 ist Gi-gekoppelt und hemmt cAMP in striatalen Neuronen; D2 ist Gs-gekoppelt und steigert cAMP in limbischen Arealen",
        answerD = "Alle Dopaminrezeptoren sind beta-Arrestin-gekoppelt und Signal ausschliesslich durch Internalisierung",
        correctAnswer = 1,
        explanation = "D1/D5 (Gs-Protein): Adenylatzyklase-Aktivierung, cAMP-Erhoehung, PKA-Aktivierung -- praevalent in Striatum und PFC, wo sie Motorik und Kognition foerdern. D2/D3/D4 (Gi/Go-Protein): cAMP-Hemmung, GIRK-Oeffnung, Hemmung spannungsabhaengiger Ca2+-Kanaele -- praevalent mesolimbisch und mesokortisch. D2-Blockade durch Antipsychotika reduziert mesolimbische Hyperaktivitaet (Positivsymptome), verstaerkt aber mesokortikale D2-Hemmung -- da praefrontale D2-Signale fuer Kognition wichtig sind, erklaert dies kognitive Nebenwirkungen.",
        difficulty = 5,
        funFact = "Clozapin, das wirksamste Antipsychotikum fuer therapieresistente Schizophrenie, hat ein breites Rezeptorprofil mit niedriger D2-Affinitaet (hoher Ki-Wert) aber hoher Affinitaet zu D4, 5-HT2A, alpha1, H1 -- diese multireceptor-Pharmakologie erklaert weniger Extrapyramidale Nebenwirkungen und mehr Wirksamkeit bei Negativsymptomen."
    ),

    // Question 45 - Neurogenese im adulten Gehirn
    Question(
        categoryId = 16,
        questionText = "Adulte Neurogenese im menschlichen Hippokampus (Gyrus dentatus) ist umstritten. Welche Zelltypen sind die neuralen Stamm- und Vorlaeufer-Zellen in der Subgranularzellzone (SGZ) des Gyrus dentatus?",
        answerA = "Ependymzellen der Ventrikelwand wandern in den Hippokampus und differenzieren dort zu Neuronen",
        answerB = "Radiale Gliazellen (Typ-1-Zellen, GFAP+/Sox2+/Nestin+) sind die Stammzellen; sie produzieren Typ-2a-Vorlaeuferzellen (GFAP-/Sox2+), dann neuronale Vorlaeuferzellen (DCX+), die zu reifen Granularzellen differenzieren",
        answerC = "Oligodendrozyten-Vorlaeuferzellen (OPC) trans-differenzieren zu Neuronen im Gyrus dentatus",
        answerD = "Pericyten aus den Kapillaren des Hippokampus sind die Quellzellen der adulten hippokampalen Neurogenese",
        correctAnswer = 1,
        explanation = "In der SGZ des Gyrus dentatus sind radiale Gliazellen (Type-1: GFAP+/Sox2+/Nestin+/BLBP+) die quieszenten Stammzellen. Sie produzieren Typ-2a-Vorlaeuferzellen (GFAP-/Sox2+/Mash1+), dann Typ-2b-Zellen (DCX+/PSA-NCAM+), die sich zu reifen Granularzellen differenzieren. Antidepressiva, Sport und Ernaehrung foerdern hippokampale Neurogenese; chronischer Stress und Alkohol hemmen sie.",
        difficulty = 5,
        funFact = "Der Streit um adulte hippokampale Neurogenese beim Menschen ist noch nicht abgeschlossen: Sorrells et al. (2018, Nature) fanden keine Neurogenese im adulten menschlichen Hippokampus; Boldrini et al. (2018, Cell Stem Cell) fanden sie bis ins hohe Alter -- methodische Unterschiede (Fixierung, Antikoerper, Schnittdicke) erklaeren die widerspruechlichen Ergebnisse."
    ),

    // Question 46 - Acetylcholinrezeptoren und Myasthenie
    Question(
        categoryId = 16,
        questionText = "Myasthenia gravis ist eine autoimmune neuromuskulaere Erkrankung. Gegen welches Zielmolekuel richten sich die pathogenen Autoantikoerper bei der haeufigsten (seropoitiven) Form, und welcher Schaedigungsmechanismus ist primaer?",
        answerA = "Gegen MuSK (Muscle-Specific Kinase) mit Komplement-unabhaengiger Hemmung der AChR-Cluster-Bildung",
        answerB = "Gegen nikotinerge AChR (nAChR, besonders alpha1-Untereinheit) an der neuromuskulaeren Endplatte -- Antikoerper kompetieren mit ACh, aktivieren Komplement und beschleunigen AChR-Internalisierung (antigenic modulation), was die Rezeptordichte vermindert",
        answerC = "Gegen Agrin im synaptischen Spalt, was die Bildung der gesamten neuromuskulaeren Synapse verhindert",
        answerD = "Gegen spannungsabhaengige Kalziumkanaele (VGCC) praesynaptisch, analog zum Lambert-Eaton-Syndrom",
        correctAnswer = 1,
        explanation = "Bei seroposiver MG (~85%) richten sich IgG1/IgG3-Antikoerper gegen nAChR an der motorischen Endplatte. Pathomechanismus: (1) Kompetitive Hemmung von ACh an der alpha1-Untereinheit; (2) Komplementaktivierung mit Membranschaeden an der Endplatte; (3) Beschleunigte AChR-Internalisierung durch Quervernetzung (antigenic modulation). Das Netto-Ergebnis ist Reduktion funktioneller AChR und Miniaturen End-Plattenpotenziale.",
        difficulty = 5,
        funFact = "Der Thymus spielt eine Schluesselrolle: 70-80% der seroposiven MG-Patienten haben Thymus-Hyperplasie (ektopische Keimzentren mit autoreaktiven T- und B-Zellen), 10-15% ein Thymom. Thymektomie verbessert bei unter 60-Jaehrigen dauerhaft die MG-Kontrolle -- der Mechanismus der Thymus-Autoimmunaetaet ist noch nicht vollstaendig geklaert."
    ),

    // Question 47 - Cerebellaere Plastizitaet
    Question(
        categoryId = 16,
        questionText = "Im Kleinhirn ist die Langzeitdepression (Kleinhirn-LTD) an Purkinje-Zell-Synapsen der wichtigste Lernmechanismus fuer motorische Adaptation. Welche Co-Aktivierung ist fuer die Induktion der Kleinhirn-LTD erforderlich?",
        answerA = "Gleichzeitige Aktivierung von Kletterfasern (aus der inferioren Olive) und Parallelfasern (von Koernerzellen) auf Purkinje-Zellen -- Kletterfaser-induzierter Ca2+-Spike permissiv fuer die mGluR1-PKC-Kaskade aus Parallelfasern",
        answerB = "Gleichzeitige Aktivierung von Moosfasern und Kletterfasern ohne Beteiligung von Parallelfasern",
        answerC = "Alleinige Parallelfaser-Stimulation mit hoher Frequenz genuegt fuer Kleinhirn-LTD",
        answerD = "Kleinhirn-LTD erfordert Dopamin-Freisetzung aus dem Mesenzephalon als dritter Koinzidenz-Signal",
        correctAnswer = 0,
        explanation = "Kleinhirn-LTD erfordert die zeitlich praezise Koinzidenz: Kletterfasern (aus inferiorer Olive) signalisieren Motorfehlern durch komplexe Aktionspotenziale -- dies erzeugt massivers Ca2+-Signal in Purkinje-Dendriten. Gleichzeitig aktivieren Parallelfasern mGluR1-Rezeptoren (Gq/PLC-Weg) auf Purkinje-Zellen. Die Kombination aus Ca2+ und DAG (aus PLC) aktiviert PKC, die AMPA-Rezeptoren an Ser880 phosphoryliert und deren Endozytose foerdert -- synaptische Abschwaechung = Kleinhirn-LTD.",
        difficulty = 5,
        funFact = "Masao Ito's Entdeckung der Kleinhirn-LTD in den 1980er-Jahren war der erste Beweis, dass synaptische Depression ein Lernmechanismus ist -- zu einer Zeit, als LTP im Hippokampus das einzige bekannte Korrelat synaptischer Plastizitaet war."
    ),

    // Question 48 - Neurovaskulaere Kopplung
    Question(
        categoryId = 16,
        questionText = "Neurovaskulaere Kopplung (NVC) beschreibt die Verbindung zwischen neuronaler Aktivitaet und lokalem Blutfluss. Welche Signalmolekuele vermitteln die Vasodilatation bei glutamaterger synaptischer Aktivitaet?",
        answerA = "Adenosin aus Astrozyten ist der einzige NVC-Mediator",
        answerB = "NO (aus nNOS in Neuronen und eNOS in Endothel), Prostaglandin E2 (COX-2 in Neuronen/Astrozyten), EETs (Epoxyeikosatriensaeure aus Astrozyten via CYP2C11) und K+-Efflux aus Astrozyten-Endfuessen (Kir4.1) wirken synergistisch auf perizytaere Gefaessmuskelzellen",
        answerC = "Serotonin aus 5-HT-Neuronen des Raphekerns ist der Hauptmediator der NVC",
        answerD = "Endothelin-1 aus Endothelzellen vermittelt die NVC durch selektive ETA-Rezeptor-Aktivierung",
        correctAnswer = 1,
        explanation = "NVC ist multifaktoriell: Glutamat aktiviert nNOS in Interneuronen -> NO-Vasodilatation; Astrozyten nehmen Glutamat auf, erhoehen Ca2+ -> PLA2 -> Arachidonsaeure -> COX-2 -> PGE2 (Vasodilatation) oder CYP450-Epoxygenasen -> EETs (Vasodilatation). Gleichzeitig effluxiert K+ aus Astrozyten durch BK-Kanaele und Kir4.1 -> Perizyt-Entspannung. Vasokonstriktion kann durch 20-HETE entstehen. Das BOLD-fMRI-Signal misst diesen NVC-Effekt indirekt.",
        difficulty = 5,
        funFact = "Perizyten, lange als passive Bindegewebszellen betrachtet, sind aktive Regulatoren des kapillaeren Blutflusses -- sie kontrahieren und dilatieren auf NO, Endothelin und andere Signale. Perizyten-Dysfunktion ist ein fruehes Zeichen bei Alzheimer und Diabetes-bedingter zerebraler Mikroangiopathie."
    ),

    // Question 49 - Cholinerge Hyopthese und Praeklinische Biomarker
    Question(
        categoryId = 16,
        questionText = "Phospho-Tau-181 und Phospho-Tau-217 im Plasma sind praeklinische Alzheimer-Biomarker. Welcher pathophysiologische Zusammenhang erklaert, warum Tau-Phosphorylierungsstellen im Plasma schon Jahre vor klinischen Symptomen veraendert sind?",
        answerA = "Tau wird direkt aus toten Neuronen ins Blut sezerniert und spiegelt nur Zelltod wider",
        answerB = "Abeta-Oligomere aktivieren fruehzeitig GSK-3beta und CDK5 in noch lebenden Neuronen -- hyperphosphoryliertes Tau wird aktiv aus Synapsen sezerniert und gelangt ueber Liquor in den Kreislauf; Tau-181/217-Phosphorylierung korreliert mit Amyloid-Pathologie, nicht nur Tau-Tangles",
        answerC = "Phospho-Tau im Plasma entsteht durch peripheres Immunsystem, das reaktiv auf ZNS-Signale reagiert",
        answerD = "Tau-Phosphorylierung im Plasma spiegelt ausschliesslich nigrostriatale Degeneration wider und ist unspezifisch fuer Alzheimer",
        correctAnswer = 1,
        explanation = "Abeta-Anhaefung aktiviert fruehzeitig Tau-Kinasen (GSK-3beta, CDK5) in Neuronen -- lange bevor Neuronenverlust einsetzt. Hyperphosphoryliertes Tau an Thr181 und Thr217 wird aktiv aus Synapsen und Neuronen sezerniert, gelangt in den Liquor und diffundiert in den Kreislauf. Diese Signatur korreliert eng mit Amyloid-PET-Positivitaet und erscheint etwa 15-20 Jahre vor klinischer Demenz -- ideal als Screening-Biomarker.",
        difficulty = 5,
        funFact = "P-tau217-Plasma-Assays erreichen inzwischen aehnliche diagnostische Genauigkeit wie Amyloid-PET fuer die Alzheimer-Diagnose -- mit einem einfachen Bluttest. Grosse pharmazeutische Firmen entwickeln Massenscreening-Programme, die Hochrisikopatienten frueher identifizieren koennen, bevor Symptome auftreten."
    ),

    // Question 50 - Neuronale Kodierung und Populationsvektoren
    Question(
        categoryId = 16,
        questionText = "Im motorischen Kortex kodieren einzelne Neurone bevorzugt bestimmte Bewegungsrichtungen (Preferred Direction). Welches Konzept beschreibt, wie eine Gesamtbewegungsrichtung aus dem kombinierten Feuern einer Neuronenpopulation entsteht?",
        answerA = "Grandmother-Cell-Theorie: ein einzelnes Neuron kodiert jede spezifische Bewegung exklusiv",
        answerB = "Populationsvektor-Kodierung (Georgopoulos, 1986): Jedes Neuron traegt einen gewichteten vektoriellen Beitrag (proportional zu seinem Feuern) bei, und der Vektorsummen-Populationsvektor zeigt praezise die geplante Bewegungsrichtung an",
        answerC = "Rate Coding: Allein die durchschnittliche Feuerrate einer zustandslosen Neuronenpopulation bestimmt Bewegungsparameter",
        answerD = "Temporal Binding: Synchrones Feuern bestimmter Kortexneurone im Gamma-Bereich definiert jede Bewegungsrichtung",
        correctAnswer = 1,
        explanation = "Apostolos Georgopoulos (1986) zeigte in primaerem motorischen Kortex, dass motorische Neurone cosinus-foermig auf Bewegungsrichtungen reagieren -- jedes hat eine 'Preferred Direction' mit maximaler Feuerrate. Der 'Populationsvektor' entsteht durch Addition aller gewichteten Vektorbeitraege (Feuerrate x Preferred-Direction-Vektor) aller aktiven Neurone und zeigt die tatsaechliche Bewegungsrichtung praezise an. Dies ist Grundlage fuer Brain-Computer-Interface-Dekodierung.",
        difficulty = 5,
        funFact = "Georgopoulos's Populationsvektor-Konzept ist die theoretische Grundlage fuer moderne Brain-Computer-Interfaces wie BrainGate -- tetraplegischen Patienten mit implantierten Utah-Arrays im motorischen Kortex koennen durch Dekodierung des Populationsvektors Roboterarme kontrollieren und sogar tippen."
    )

)
