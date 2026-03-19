package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsExpert7(): List<Question> = listOf(

    // Question 1 -- Haematologie: Heparin-induzierte Thrombozytopenie
    Question(
        categoryId = 16,
        questionText = "Ein Patient entwickelt 7 Tage nach Heparingabe einen Thrombozytenabfall auf 45.000/µl und gleichzeitig eine neue Beinvenenthrombose. Welcher pathophysiologische Mechanismus liegt der Heparin-induzierten Thrombozytopenie Typ II (HIT II) zugrunde?",
        answerA = "Direkter toxischer Effekt von Heparin auf Megakaryozyten im Knochenmark",
        answerB = "IgG-Antikoerper gegen den Komplex aus Heparin und Plattchenfaktor 4 (PF4) aktivieren Thrombozyten ueber Fc-gamma-IIa-Rezeptoren und fuehren zur paradoxen Thrombose",
        answerC = "Verbrauchskoagulopathie durch ueberschiessende Fibrinolyse nach Heparin-Exposition",
        answerD = "Genetischer Mangel an Antithrombin III, der durch Heparin demaskiert wird",
        correctAnswer = 1,
        explanation = "Bei HIT II bildet das Immunsystem IgG-Antikoerper gegen den Komplex aus Heparin und Plattchenfaktor 4. Diese Antikoerper binden Thrombozyten ueber deren Fc-gamma-IIa-Rezeptoren und aktivieren sie massiv, was trotz niedriger Thrombozytenzahl zu einem prothrombotischen Zustand fuehrt. Heparin muss sofort abgesetzt und durch einen alternativen Antikoagulans (z.B. Argatroban) ersetzt werden.",
        difficulty = 4,
        funFact = "HIT II ist eine der gefaehrlichsten Arzneimittelnebenwirkungen: Bei bis zu 50 % der betroffenen Patienten kommt es zu thromboembolischen Komplikationen, wenn Heparin nicht rechtzeitig abgesetzt wird. Der 4T-Score hilft, die klinische Wahrscheinlichkeit zu berechnen."
    ),

    // Question 2 -- Endokrinologie: Diabetische Ketoazidose
    Question(
        categoryId = 16,
        questionText = "Welcher biochemische Mechanismus erklaert, warum bei diabetischer Ketoazidose trotz massiv erhoehter Blutglukose eine ausgepragte Ketogenese auftritt?",
        answerA = "Ueberschuss an Insulin hemmt die Fettsaeure-Oxidation und leitet Acetyl-CoA in die Ketonkoerpersynthese um",
        answerB = "Absoluter Insulinmangel fuehrt zur ungehemmten Lipolyse; der Ueberschuss an Fettsaeuren und hohes Glucagon erhoehen Malonyl-CoA-Mangel, sodass Carnitin-Palmitoyltransferase I (CPT I) aktiviert und die Beta-Oxidation maximiert wird",
        answerC = "Hyperglykamie hemmt direkt die Glukoneogenese und leitet Oxalacetat in die Ketonkoerpersynthese um",
        answerD = "Renaler Glukosverlust aktiviert kompensatorisch die Ketonkoerpersynthese als alternativen Energieweg",
        correctAnswer = 1,
        explanation = "Insulinmangel hebt die Hemmung der hormonsensitiven Lipase auf, sodass Triglyzeride massiv zu Fettsaeuren gespalten werden. Gleichzeitig sinkt Malonyl-CoA (ein Hemmstoff von CPT I), was den Transport langkettiger Fettsaeuren in die Mitochondrien erleichtert und die Beta-Oxidation sowie Ketonkoerpersynthese (Acetoacetat, Beta-Hydroxybutyrat) maximiert. Das steigende Glucagon verstaerkt diesen Effekt zusaetzlich.",
        difficulty = 4,
        funFact = "Das Verhaeltnis von Beta-Hydroxybutyrat zu Acetoacetat bei der DKA betraegt etwa 3:1 bis 8:1. Im POCT-Blutgasgeraet erscheint deshalb oft eine ausgepragte metabolische Azidose mit vergroesserter Anionenlucke, bevor Ketonstreifen im Urin positiv werden."
    ),

    // Question 3 -- Kardiologie: Wolff-Parkinson-White-Syndrom
    Question(
        categoryId = 16,
        questionText = "Im EKG eines asymptomatischen 22-Jaehrigen faellt eine verkuerzte PQ-Zeit von 90 ms und ein Delta-Welle auf. Welche EKG-Veraenderung ist pathognomonisch fuer das Wolff-Parkinson-White-Syndrom und was ist deren elektrophysiologische Ursache?",
        answerA = "Verlangsamte Ueberleitungszeit im AV-Knoten durch funktionellen Linksschenkelblock",
        answerB = "Verlaengerter QRS-Komplex durch retrograde Leitung ueber das akzessorische Buendel",
        answerC = "Verkuerzte PQ-Zeit kombiniert mit Delta-Welle durch akzessorische atrioventrikulaere Leitungsbahn (Kent-Buendel), die den AV-Knoten ueberbrueckt und das Ventrikelmyokard vor dem normalen HIS-System depolarisiert",
        answerD = "Spiegelbildliche ST-Senkungen in den Ableitungen V1-V4 durch anteriore Praexzitation",
        correctAnswer = 2,
        explanation = "Das Kent-Buendel ueberbrueckt den physiologischen AV-Knoten und leitet elektrische Impulse schneller zum Ventrikel. Dies fuehrt zu einer verkuerzten PQ-Zeit (fruehe atrioventrikulaere Leitung) und einer Delta-Welle (langsame Ausbreitung der fruehzeitigen ventrikulaeren Depolarisation). Das restliche Myokard wird dann normal ueber das HIS-Purkinje-System erregt.",
        difficulty = 4,
        funFact = "Beim WPW-Syndrom kann Vorhofflimmern lebensbedrohlich sein: Das akzessorische Buendel hat keine Dekrementalleitung und kann Frequenzen >300/min auf den Ventrikel uebertragen, was in Kammerflimmern muenden kann. Antiarrhythmika, die den AV-Knoten verlangsamen (z.B. Verapamil, Adenosin), sind dabei kontraindiziert."
    ),

    // Question 4 -- Nephrologie: Renale tubulaere Azidose
    Question(
        categoryId = 16,
        questionText = "Ein Patient zeigt eine hyperchloraemische metabolische Azidose mit normaler Anionenlucke, Hypokaliamie und einem Urin-pH von 6,5 trotz systemischer Azidose. Welcher Typ der renalen tubularen Azidose (RTA) liegt vor und welcher Defekt ist verantwortlich?",
        answerA = "RTA Typ IV: Hyporeninischer Hypoaldosteronismus mit Hyperkaliamie",
        answerB = "RTA Typ I (distal): Defekt der H+-ATPase oder des H+/K+-ATPase-Austauschers im distalen Tubulus, sodass der Urin nicht maximal angesaeuert werden kann",
        answerC = "RTA Typ II (proximal): Verminderte HCO3-Reabsorption im proximalen Tubulus mit niedrigem Urin-pH",
        answerD = "RTA Typ III: Kombinierter Typ I und II-Defekt durch Carboanhydrase-II-Mutation",
        correctAnswer = 1,
        explanation = "Der Urin-pH > 5,5 trotz systemischer Azidose ist das Charakteristikum der distalen RTA (Typ I). Der Defekt liegt im distalen Tubulus/Sammelrohr: H+-Ionen koennen nicht ausreichend in das Tubuluslumen sezerniert werden. Folgen sind Azidose, Hypokaliamie (kompensatorische K+-Ausscheidung), Hyperkalziurie und Neigung zu Nephrokalzinose. Ursachen: Sjogren-Syndrom, Amphotericin-B-Toxizitaet, Marfan-Syndrom.",
        difficulty = 4,
        funFact = "Patienten mit distaler RTA entwickeln haeufig Nierensteine und Nephrokalzinose, da die chronische Azidose Kalzium aus den Knochen mobilisiert und die Zitraturie (Zitrat hemmt Kalziumoxalat-Kristallisation) verringert ist."
    ),

    // Question 5 -- Pharmakologie: Cytochrom-P450-Interaktionen
    Question(
        categoryId = 16,
        questionText = "Ein Patient erhalt Warfarin und beginnt neu mit Fluconazol. Welcher pharmakologische Mechanismus erklaert die gefaehrliche Interaktion, und was ist die klinische Konsequenz?",
        answerA = "Fluconazol induziert CYP2C9 und erhoehte Warfarin-Clearance fuehrt zur Unterdosierung",
        answerB = "Fluconazol hemmt CYP2C9 (Hauptabbauweg von S-Warfarin), was den Warfarin-Spiegel ansteigen laesst und das Blutungsrisiko erhoeht",
        answerC = "Fluconazol verdraengt Warfarin kompetitiv aus der Plasmaproteinbindung ohne Einfluss auf den Metabolismus",
        answerD = "Fluconazol aktiviert das Efflux-Transporter P-Glycoprotein und vermindert die intestinale Warfarin-Resorption",
        correctAnswer = 1,
        explanation = "Fluconazol ist ein starker Inhibitor von CYP2C9, dem Hauptenzym fuer den Abbau des wirksameren S-Enantiomers von Warfarin. Durch die Hemmung steigt der Warfarin-Plasmaspiegel deutlich an, was zu einem erheblich verlaengerten INR und erhoehtem Blutungsrisiko fuehrt. Eine INR-Kontrolle und Dosisreduktion von Warfarin sind bei gleichzeitiger Gabe zwingend erforderlich.",
        difficulty = 4,
        funFact = "CYP2C9 ist auch fuer den Abbau von NSAIDs (Ibuprofen, Diclofenac), Sulfonylarnstoffen (Glipizid) und vielen Antihypertensiva zustaendig. Genetische Polymorphismen in CYP2C9 (z.B. CYP2C9*2, *3) koennen die Warfarin-Empfindlichkeit um das 3-5-fache beeinflussen."
    ),

    // Question 6 -- Neurologie: Status epilepticus
    Question(
        categoryId = 16,
        questionText = "Ein 35-jaehriger Patient befindet sich nach 45 Minuten noch im generalisierten tonisch-klonischen Status epilepticus. Benzodiazepin und Levetiracetam wurden bereits gegeben. Welches Medikament ist als naechste Eskalationsstufe (superrefraktaerer Status) indiziert?",
        answerA = "Orales Phenobarbital, da es die starkste antiepileptische Wirkung hat",
        answerB = "Hochdosiertes intravenoes Midazolam, Propofol oder Thiopental als Narkoseinduktion unter Intensivbedingungen mit EEG-Monitoring zur Unterdruckung des Anfallsmusters",
        answerC = "Adenosin i.v., da Adenosin-Rezeptoren neuronale Hyperaktivitaet hemmen",
        answerD = "Intravenoes Magnesium als einzige Therapie der zweiten Wahl ohne weitere Eskalation",
        correctAnswer = 1,
        explanation = "Nach Versagen von Benzodiazepin und einem zweiten Antiepileptikum spricht man vom refraktaeren Status epilepticus. Die naechste Stufe ist eine Allgemeinanasthesie (Narkose) mit Propofol, Midazolam oder Barbiturate (Thiopental/Phenobarbital i.v.) unter kontinuierlichem EEG-Monitoring auf der Intensivstation, um ein Burst-Suppression-Muster zu erreichen.",
        difficulty = 4,
        funFact = "Der Status epilepticus hat eine Mortalitaet von 10-30 %. Jede Minute anhaltender Anfallsaktivitaet erhoehrt das Risiko permanenter neuronaler Schaeden durch Exzitotoxizitaet, Hyperthermie und zerebrale Hypeoxie. Fruehzeitige und aggressive Therapie ist entscheidend."
    ),

    // Question 7 -- Gastroenterologie: Wilson-Krankheit
    Question(
        categoryId = 16,
        questionText = "Ein 19-Jaehriger prasentiert sich mit Leberzirrhose, neuropsychiatrischen Symptomen und einem goldbraunen Ring am Hornhautrand. Welcher Laborparameter ist bei Wilson-Krankheit typischerweise ERNIEDRIGT und welches Protein ist betroffen?",
        answerA = "Serumeisen, weil das Kupfer-Transportprotein Haemosiderin ausgefaellt wird",
        answerB = "Serum-Caeruloplasmin ist erniedrigt, weil das mutierte ATP7B-Protein den Einbau von Kupfer in Caeruloplasmin und dessen Sekretion in der Leber verhindert",
        answerC = "Serum-Albumin steigt durch Kupfer-Proteinbindung kompensatorisch an",
        answerD = "Serum-Zink ist erniedrigt, da Kupfer Zink aus Transportproteinen verdraengt",
        correctAnswer = 1,
        explanation = "ATP7B ist eine kupfertransportierende ATPase in der Leber. Bei Funktionsverlust wird Kupfer nicht in Caeruloplasmin eingebaut und nicht ueber die Galle ausgeschieden. Folge: Kupferakkumulation in Leber, Gehirn und Kornea (Kayser-Fleischer-Ring). Serum-Caeruloplasmin ist in ~85 % der Faelle erniedrigt (<20 mg/dl). Der 24h-Urin-Kupfer und Leberbiopsie sichern die Diagnose.",
        difficulty = 4,
        funFact = "Die Wilson-Krankheit ist eine der wenigen genetisch bedingten Erkrankungen, die durch Chelattherapie (D-Penicillamin, Trientine) oder Zinksupplementation effektiv behandelt werden kann. Bei rechtzeitiger Diagnose vor irreversiblen Organschaeden ist die Prognose gut."
    ),

    // Question 8 -- Pulmonologie: ARDS Berlin-Definition
    Question(
        categoryId = 16,
        questionText = "Gemaess der Berlin-Definition des ARDS von 2012 wird der Schweregrad anhand des PaO2/FiO2-Quotienten (Horovitz-Index) unter einem PEEP von mindestens 5 cmH2O klassifiziert. Welchem Schweregrad entspricht ein Horovitz-Index von 180 mmHg?",
        answerA = "Mildes ARDS (PaO2/FiO2 201-300 mmHg)",
        answerB = "Moderates ARDS (PaO2/FiO2 101-200 mmHg)",
        answerC = "Schweres ARDS (PaO2/FiO2 <= 100 mmHg)",
        answerD = "Kein ARDS, da der Wert ueber der Grenze liegt",
        correctAnswer = 1,
        explanation = "Die Berlin-Definition unterteilt ARDS in drei Schweregrade: mild (PaO2/FiO2 201-300 mmHg), moderat (101-200 mmHg) und schwer (<= 100 mmHg). Ein Wert von 180 mmHg faellt in die moderate Kategorie. Die Klassifikation hat therapeutische Konsequenzen: Bei schwerem ARDS wird fruehzeitige Bauchlagerung (Prone Positioning) und neuromuskulaere Blockade empfohlen.",
        difficulty = 4,
        funFact = "Die Sterblichkeit beim schweren ARDS liegt trotz moderner Intensivmedizin bei 40-50 %. Die ARDS Network-Studie (ARDSNet, 2000) zeigte, dass eine lungenprotektive Beatmung mit niedrigem Tidalvolumen (6 ml/kg IBW) die Mortalitaet um 22 % relativ senkt."
    ),

    // Question 9 -- Rheumatologie: Antiphospholipid-Syndrom
    Question(
        categoryId = 16,
        questionText = "Eine 32-jaehrige Frau erleidet nach zwei Spontanaborti einen Schlaganfall. Laborchemisch findet sich ein falsch positiver VDRL-Test und verlaengerte aPTT, die sich nicht durch Mischungsversuche normalisiert. Welche Diagnose ist am wahrscheinlichsten?",
        answerA = "Systemischer Lupus erythematodes mit zerebraler Vaskulitis",
        answerB = "Antiphospholipid-Syndrom mit Lupus-Antikoagulans, Anticardiolipin- und/oder Anti-beta2-Glykoprotein-I-Antikoerper",
        answerC = "Haemophilie A mit erworbenen FVIII-Hemmern",
        answerD = "Disseminierte intravasale Gerinnung durch okkulte Sepsis",
        correctAnswer = 1,
        explanation = "Das Antiphospholipid-Syndrom (APS) ist charakterisiert durch arterielle/venoese Thrombosen, Schwangerschaftskomplikationen und persistente Antiphospholipid-Antikoerper. Das Lupus-Antikoagulans verlaengert die aPTT in vitro (inhibiert Phospholipide der Reagenzien), bewirkt aber in vivo eine Thrombophilie. Der falsch-positive VDRL-Test entsteht, weil Cardiolipin ein Phospholipid ist.",
        difficulty = 4,
        funFact = "Das APS ist die haeufigste erworbene Ursache einer Thrombophilie. Bei Sapporo-Kriterien (klinisch + Labor) ist eine lebenslange Antikoagulation mit Vitamin-K-Antagonisten indiziert. Direkte orale Antikoagulanzien (DOAK) sind beim Triple-positiven APS wegen hoeherer Schlaganfallraten nicht empfohlen."
    ),

    // Question 10 -- Infektiologie: Malaria-Pathophysiologie
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die Zytoadhaerenz und Rosettenbildung bei Plasmodium falciparum-Malaria und welche klinischen Komplikationen entstehen dadurch?",
        answerA = "Plasmodium-Toxine lysieren Erythrozyten, was zu Haemolyse und anaemischer Hypoxie fuehrt",
        answerB = "PfEMP1 (P. falciparum Erythrozyten-Membranprotein 1) auf der Erythrozytenoberflaeche bindet Endothelrezeptoren (CD36, ICAM-1, PECAM-1), was Sequestration in Kapillaren verursacht und zu zerebraler Malaria, Organversagen und Hypoglykamie fuehrt",
        answerC = "Sporozoiten blockieren direkt Hepatozyten-Rezeptoren und verhindern Lebersynthese von Gerinnungsfaktoren",
        answerD = "Gametozyten von P. falciparum produzieren Haemazoin, das Makrophagen hemmt und zur Immunsuppression fuehrt",
        correctAnswer = 1,
        explanation = "P. falciparum exprimiert PfEMP1 an der Erythrozytenoberflaeche. Dieses Protein bindet verschiedene Endothelrezeptoren und bewirkt, dass infizierte Erythrozyten in der Mikrovaskulatur sezernieren, anstatt in der Milz zerstoert zu werden. Diese Sequestration fuehrt zur Mikrozirkulationsstoerung, besonders im Gehirn (zerebrale Malaria), Placenta und Lunge.",
        difficulty = 4,
        funFact = "Die Sicheltrait-Mutation (HbAS) schuetzt vor schwerer Malaria, weil Sichelzell-Erythrozyten weniger PfEMP1 exprimieren und rascher in der Milz abgebaut werden. Dies erklaert, warum die Sichelzell-Mutation trotz homozygoter Toedlichkeit in Malaria-Endemiegebieten evolutionaer erhalten blieb."
    ),

    // Question 11 -- Pharmakologie: Statine und Myopathie
    Question(
        categoryId = 16,
        questionText = "Welcher Transportermechanismus erklaert, warum die gleichzeitige Gabe von Simvastatin mit Ciclosporin das Risiko einer Rhabdomyolyse erheblich erhoehrt?",
        answerA = "Ciclosporin induziert CYP3A4 und erhoehrt den Simvastatin-Metabolismus, sodass toxische Abbauprodukte entstehen",
        answerB = "Ciclosporin hemmt den hepatischen Aufnahmetransporter OATP1B1 (SLCO1B1) und reduziert gleichzeitig CYP3A4-Aktivitaet, wodurch Simvastatin-Plasmaspiegel dramatisch ansteigen",
        answerC = "Ciclosporin erhoehrt die Bindungsaffinitaet von Simvastatin an HMG-CoA-Reduktase im Muskel",
        answerD = "Ciclosporin induziert eine Autoimmunmyositis, die durch Statine verstaerkt wird",
        correctAnswer = 1,
        explanation = "OATP1B1 ist ein Aufnahmetransporter in der hepatischen Sinusoidalmembran, der Statine effizient aus dem Blut in die Leber transportiert (wo sie wirken sollen). Ciclosporin hemmt OATP1B1 stark und reduziert zusaetzlich CYP3A4. Beides zusammen fuehrt zu drastisch erhoehten systemischen Simvastatin-Spiegeln, was Myopathie bis zur lebensbedrohlichen Rhabdomyolyse verursacht. Rosuvastatin wird als sicherere Alternative empfohlen.",
        difficulty = 4,
        funFact = "Das SLCO1B1*5-Allel (rs4149056) reduziert die OATP1B1-Funktion genetisch. Traeger dieses Allels haben bei Simvastatin 80 mg ein bis zu 17-fach erhoehtes Myopathie-Risiko. Pharmakogenetik-Tests fuer dieses SNP sind fuer Hochdosis-Simvastatin-Therapie empfohlen."
    ),

    // Question 12 -- Haematologie: Disseminierte intravasale Gerinnung
    Question(
        categoryId = 16,
        questionText = "Bei einem Patienten mit Sepsis zeigt das Labor: Thrombozytopenie (50.000/µl), erhoehte D-Dimere, erniedrigtes Fibrinogen und verlaengerte PT sowie aPTT. Welcher pathophysiologische Mechanismus erklaert das gleichzeitige Auftreten von Thrombosen UND Blutungen bei der DIC?",
        answerA = "Erhoehte Thrombozytenaggregation verbraucht Fibrinogen, waehrend ueberschuessiges Thrombin direkt Gefaesse schaedigt",
        answerB = "Systemische Thrombin-Uberaktivierung verbraucht Gerinnungsfaktoren und Thrombozyten (Thrombose in Mikrogefaessen), waehrend sekundaere Hyperfibrinolyse und Faktorenmangel zu Blutungen fuehren",
        answerC = "Bakterielle Endotoxine hemmen selektiv die Protein-C-Kaskade, waehrend Antithrombin III aktiviert wird",
        answerD = "Zytokin-vermittelte Knochenmarkdepression fuehrt zu Thrombozytopenie, die Blutungen bedingt, ohne direkten Gerinnungseffekt",
        correctAnswer = 1,
        explanation = "Bei der DIC aktivieren Trigger (Sepsis, Trauma, Malignome) massiv das Gerinnungssystem. Systemisches Thrombin bildet Mikrothrombosen und verbraucht dabei Gerinnungsfaktoren und Thrombozyten. Sekundaer wird Plasminogen zu Plasmin aktiviert (Hyperfibrinolyse). Das Resultat ist paradox: Gleichzeitig Thrombosen (Organversagen) und Blutungen (Faktorenverbrauch). D-Dimere als Fibrinspaltprodukte sind ein sensitiver Marker.",
        difficulty = 4,
        funFact = "Die vier grossen DIC-Trigger sind die '4 Ts': Trauma, Tumor (besonders Prostatakarzinom, AML M3), Toxine (Schlangenbiss, Amnionfluessigkeit) und thrombotische Mikroangiopathien. Die AML M3 (APML) gilt als Sonderfall mit besonders aggressiver DIC, die durch all-trans Retinsaeure (ATRA) behandelt wird."
    ),

    // Question 13 -- Immunologie: Primmaere Immundefekte
    Question(
        categoryId = 16,
        questionText = "Ein 6 Monate alter Junge entwickelt nach dem Rueckgang muetterlicher Antikoerper schwere bakterielle und virale Infektionen. Flusszytometrisch fehlen reife B-Zellen voellig, T-Zellen sind normal. Welche Diagnose und welches mutierte Gen ist am wahrscheinlichsten?",
        answerA = "Selektiver IgA-Mangel durch TACI-Mutation mit isoliertem B-Zell-Reifungsdefekt",
        answerB = "X-chromosomale Agammaglobulinaemie (XLA) durch BTK-Mutation (Bruton-Tyrosinkinase), die die pro-B-Zell-Reifung zur praB-Zelle blockiert",
        answerC = "Schwerer kombinierter Immundefekt (SCID) durch RAG1/RAG2-Mutation mit T- und B-Zell-Mangel",
        answerD = "DiGeorge-Syndrom durch 22q11-Deletion mit Thymusaplasie und T-Zell-Mangel",
        correctAnswer = 1,
        explanation = "BTK (Bruton-Tyrosinkinase) ist essentiell fuer die B-Zell-Entwicklung im Knochenmark, speziell den Schritt von der pro-B-Zelle zur prae-B-Zelle. Bei XLA fehlt BTK (X-chromosomal), sodass keine reifen B-Zellen entstehen und alle Immunglobulinklassen fehlen. T-Zellen sind normal. Klinisch: Gehaeufte schwere bakterielle Infektionen, beginnend nach dem 6. Lebensmonat wenn der Nestschutz endet. Therapie: Immunglobulin-Substitution.",
        difficulty = 4,
        funFact = "XLA wurde 1952 von Ogden Bruton als erste Immundefizienz beschrieben. Er bemerkte, dass ein Junge mit rezidivierenden Infektionen keine Gammaglobuline in der Serumproteinelektrophorese hatte. Die Therapie mit Gammaglobulin-Infusionen war revolutionaer und ist bis heute Goldstandard."
    )
)
