package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsHard1(): List<Question> = listOf(

    // --- AUTOIMMUNE DISEASES ---

    // Question 1 -- Lupus butterfly rash
    Question(
        categoryId = 16,
        questionText = "Welches klassische Hautzeichen tritt bei systemischem Lupus erythematodes (SLE) auf und wie entsteht es pathophysiologisch?",
        answerA = "Schmetterlingserythem durch Immunkomplex-Ablagerungen in dermalen Gefaessen",
        answerB = "Livedo reticularis durch arterielle Thrombosen der Hautkapillaren",
        answerC = "Erythema nodosum durch granulomatoese Entzuendung des subkutanen Fettgewebes",
        answerD = "Purpura durch Thrombozytopenie infolge Knochenmarkssuppression",
        correctAnswer = 0,
        explanation = "Das Schmetterlingserythem (Malar Rash) entsteht durch Ablagerung von Antigen-Antikoerper-Komplexen (Immunkomplexe) in den dermalen Blutgefaessen der Wangen und Nase, was eine Komplementaktivierung und Entzuendungsreaktion ausloest.",
        difficulty = 3,
        funFact = "Bei SLE richten sich Autoantikoerper vor allem gegen doppelstraengige DNA (dsDNA) -- ein Befund, der fast ausschliesslich bei dieser Erkrankung vorkommt und diagnostisch wegweisend ist."
    ),

    // Question 2 -- MS mechanism
    Question(
        categoryId = 16,
        questionText = "Welcher pathophysiologische Mechanismus liegt der Multiplen Sklerose (MS) zugrunde?",
        answerA = "Degeneration dopaminerger Neurone in der Substantia nigra durch Lewy-Koerperchen",
        answerB = "Autoimmune Demyelinisierung zentralnervoesen Gewebes durch T-Lymphozyten",
        answerC = "Amyloid-Beta-Ablagerungen in kortikalen Neuronen mit progressivem Zelluntergang",
        answerD = "Vaskulitische Schaedigung der Blut-Hirn-Schranke durch zirkulierende Immunkomplexe",
        correctAnswer = 1,
        explanation = "Bei MS greifen autoreaktive T-Lymphozyten das Myelin der Nervenfasern im ZNS an und zerstoeren die Myelinscheide (Demyelinisierung), was die Nervenleitung verlangsamt oder unterbricht. Im Verlauf kommt es auch zur Axonschaedigung.",
        difficulty = 3,
        funFact = "MS tritt etwa doppelt so haeufig bei Frauen auf wie bei Maennern, und Menschen, die weit weg vom Aequator leben, erkranken haeufiger -- ein Hinweis auf die Rolle von Vitamin D und Sonnenlicht."
    ),

    // Question 3 -- Rheumatoid arthritis marker
    Question(
        categoryId = 16,
        questionText = "Welcher Laborparameter ist der spezifischste serologische Marker fuer die rheumatoide Arthritis?",
        answerA = "Antinukleaere Antikoerper (ANA)",
        answerB = "Erhoehte Blutsenkungsgeschwindigkeit (BSG)",
        answerC = "Anti-CCP-Antikoerper (antizyklische citrullinierte Peptide)",
        answerD = "Rheumafaktor (IgM-Antikoerper gegen IgG)",
        correctAnswer = 2,
        explanation = "Anti-CCP-Antikoerper haben bei rheumatoider Arthritis eine Spezifitaet von ueber 95 % und sind dem klassischen Rheumafaktor deutlich ueberlegen. Sie koennen bereits Jahre vor dem klinischen Ausbruch der Erkrankung positiv sein.",
        difficulty = 3,
        funFact = "Der Rheumafaktor, der seit den 1940er-Jahren bekannt ist, ist zwar sensitiver, aber auch bei anderen Erkrankungen wie dem Sjoegren-Syndrom oder chronischen Infektionen positiv -- daher ist er allein wenig aussagekraeftig."
    ),

    // Question 4 -- Hashimoto's pathology
    Question(
        categoryId = 16,
        questionText = "Welches histologische Bild ist charakteristisch fuer die Hashimoto-Thyreoiditis?",
        answerA = "Papillaere Wucherungen des Follikelepithels mit Psammokoerperchen",
        answerB = "Lymphozytaere Infiltration des Schilddruesengewebes mit Keimzentrumsbildung",
        answerC = "Granulomatoese Entzuendung mit mehrkernigen Riesenzellen",
        answerD = "Hyaline Verquellung der Follikelbasalmembran mit Amyloidablagerung",
        correctAnswer = 1,
        explanation = "Bei Hashimoto-Thyreoiditis infiltrieren Lymphozyten und Plasmazellen das Schilddruesengewebe und bilden Lymphfollikel mit Keimzentren. Zusaetzlich kommt es zur Zerstoerung der Follikel und zur Huerthle-Zell-Metaplasie des Epithels.",
        difficulty = 3,
        funFact = "Hashimoto ist mit Abstand die haeufigste Autoimmunerkrankung der Schilddruese und die haeufigste Ursache einer Hypothyreose in Industrielaendern -- betroffen sind etwa 8-mal mehr Frauen als Maenner."
    ),

    // Question 5 -- Systemic sclerosis
    Question(
        categoryId = 16,
        questionText = "Welches Phaenomen gilt als Fruehzeichen der systemischen Sklerose (Sklerodermie) und tritt bei fast allen Patienten auf?",
        answerA = "Butterfly-Exanthem im Gesicht",
        answerB = "Raynaud-Phaenomen mit triphasischem Farbwechsel der Finger",
        answerC = "Heliotropes Erythem der Augenlider",
        answerD = "Gottron-Papeln ueber den Fingerknocheln",
        correctAnswer = 1,
        explanation = "Das Raynaud-Phaenomen (Weiss-Blau-Rot-Farbwechsel der Finger bei Kaelte oder Stress) tritt bei ueber 95 % der Patienten mit systemischer Sklerose auf und ist oft das erste Symptom, Jahre vor der Hautfibrose.",
        difficulty = 3,
        funFact = "Bei systemischer Sklerose lagert sich Kollagen nicht nur in der Haut ab, sondern auch in inneren Organen wie Lunge, Niere und Herz. Die Lungenfibrose ist heute die haeufigste todesursaechliche Komplikation."
    ),

    // --- CARDIOVASCULAR SYSTEM ---

    // Question 6 -- Atherosclerosis mechanism
    Question(
        categoryId = 16,
        questionText = "Welche Zellen spielen die zentrale initiale Rolle bei der Entstehung atherosklerotischer Plaques?",
        answerA = "Mastzellen, die Histamin und Heparin aus Granula ausschuetten",
        answerB = "Makrophagen, die LDL-Cholesterin phagozytieren und zu Schaumzellen werden",
        answerC = "Neutrophile Granulozyten, die proteolytische Enzyme freisetzen",
        answerD = "NK-Zellen, die geschaedigte Endothelzellen lysieren",
        correctAnswer = 1,
        explanation = "Oxidiertes LDL wird von Makrophagen in der Intima durch Scavengerrezeptoren aufgenommen. Die Makrophagen koennen das Cholesterin nicht abbauen und werden zu lipidbeladenen Schaumzellen -- die Grundlage des Fettstreifens (Fatty Streak).",
        difficulty = 3,
        funFact = "Atherosklerose beginnt bereits in der Kindheit -- Autopsien junger US-Soldaten im Koreakrieg (Durchschnittsalter 22 Jahre) zeigten bei 77 % bereits erkennbare Koronarveraenderungen."
    ),

    // Question 7 -- Atrial fibrillation mechanism
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert am besten, warum Vorhofflimmern das Schlaganfallrisiko erhoeht?",
        answerA = "Koronarspasmen durch sympathische Ueberaktivierung verursachen Mikroembolien",
        answerB = "Stase des Blutes im Vorhofohr fuehrt zur Thrombusbildung und Embolisierung ins Gehirn",
        answerC = "Erhoehter Herzauswurf destabilisiert bestehende Karotis-Plaques",
        answerD = "Kardiale Hypertrophie komprimiert den Ductus thoracicus mit Lymphoedem",
        correctAnswer = 1,
        explanation = "Bei Vorhofflimmern kontrahieren die Vorhoefe unkoordiniert, sodass Blut insbesondere im linken Vorhofohr stagniert. Diese Stase foerdert die Thrombusbildung -- loest sich ein Thrombus, kann er als Embolus ins Gehirn gelangen und einen Schlaganfall verursachen.",
        difficulty = 3,
        funFact = "Das linke Vorhofohr wird bei manchen Patienten mit Vorhofflimmern heute interventionell mit einem Okkluder verschlossen, um Schlaganfaelle zu verhindern -- ohne lebenslange Blutverduenung."
    ),

    // Question 8 -- Heart valve disease
    Question(
        categoryId = 16,
        questionText = "Welche pathophysiologische Konsequenz hat eine schwere Aortenstenose fuer den linken Ventrikel?",
        answerA = "Exzentrische Hypertrophie durch Volumenueberlastung des linken Ventrikels",
        answerB = "Konzentrische Hypertrophie durch Druckueberlastung mit erhoehtem Wandstress",
        answerC = "Dilatative Kardiomyopathie durch retrograden Druckanstieg im Lungenkreislauf",
        answerD = "Restriktive Kardiomyopathie durch Amyloidablagerung in der Herzwand",
        correctAnswer = 1,
        explanation = "Bei Aortenstenose muss der linke Ventrikel gegen einen erhoehten Ausfluss-Widerstand pumpen (Druckueberlastung). Dies fuehrt nach dem Laplace-Gesetz zur Wandverdickung (konzentrische Hypertrophie), um den Wandstress zu normalisieren.",
        difficulty = 3,
        funFact = "Die klinische Trias der schweren Aortenstenose -- Angina, Synkope, Herzinsuffizienz -- ist prognostisch bedeutsam: ohne Klappenersatz betraegt die mittlere Ueberlebenszeit nach Auftreten dieser Symptome nur 1-3 Jahre."
    ),

    // Question 9 -- Myocardial infarction biomarker
    Question(
        categoryId = 16,
        questionText = "Welches kardiale Biomarker-Protein gilt heute als Goldstandard fuer die Diagnose des akuten Myokardinfarkts und warum?",
        answerA = "Myoglobin, weil es als erstes Protein nach Zelluntergang ins Blut gelangt",
        answerB = "CK-MB, weil es herzspezifisch ist und schnell abfaellt",
        answerC = "Hochsensitives Troponin I oder T, weil es herzspezifisch ist und frueh ansteigt",
        answerD = "LDH-1-Isoenzym, weil es 24-72 Stunden erhoeht bleibt",
        correctAnswer = 2,
        explanation = "Hochsensitives Troponin I und T sind herzmuskelspezifisch, steigen bereits 1-3 Stunden nach Infarktereignis an, erreichen ein Maximum nach 12-24 Stunden und bleiben bis zu 14 Tage erhoeht -- ideal fuer Diagnose und zeitliche Einordnung.",
        difficulty = 3,
        funFact = "Troponin ist so herzspezifisch, dass selbst winzige Mengen (milliardstel Gramm pro Milliliter Blut) nachweisbar sind -- moderne hochsensitive Tests erkennen Herzschaeden, die noch keine Symptome verursacht haben."
    ),

    // Question 10 -- Long QT syndrome
    Question(
        categoryId = 16,
        questionText = "Was bedeutet ein verlaengertes QT-Intervall im EKG und welches lebensbedrohliche Ereignis kann daraus entstehen?",
        answerA = "Verlaengerte ventrikulaere Repolarisation, die Torsade-de-Pointes-Tachykardien ausloesen kann",
        answerB = "Beeintraechtigte AV-Ueberleitung, die zu komplettem AV-Block fuehren kann",
        answerC = "Verlangsamte sinoatriale Erregung, die Sick-Sinus-Syndrom begruendet",
        answerD = "Ventrikulaere Praexzitation durch akzessorische Leitungsbahnen mit WPW-Syndrom",
        correctAnswer = 0,
        explanation = "Ein verlaengertes QT-Intervall zeigt eine verzoegerte ventrikulaere Repolarisation an. Waehrend dieser vulnerablen Phase kann ein fruehzeitig einfallender Extraschlag Torsade-de-Pointes ausloesen -- eine polymorphe Kammertachykardie, die in Kammerflimmern und ploetzlichen Tod uebergehen kann.",
        difficulty = 3,
        funFact = "Viele gaengige Medikamente -- darunter Antibiotika (Azithromycin), Antihistaminika und Psychopharmaka -- koennen das QT-Intervall verlaengern und so bei genetisch vorbelasteten Personen toedliche Arrhythmien ausloesen."
    ),

    // --- RESPIRATORY DISEASES ---

    // Question 11 -- Pneumonia pathophysiology
    Question(
        categoryId = 16,
        questionText = "Welches pathophysiologische Phaenomen erklaert bei einer Lobaerpneumonie die Hypoxaemie trotz normaler Ventilation angrenzender Lungensegmente?",
        answerA = "Diffusionsstoerung durch Verdickung der alveolaren Basalmembran",
        answerB = "Intrapulmonaler Rechts-links-Shunt durch perfundierte, aber nicht ventilierte Alveolen",
        answerC = "Totraum-Ventilation durch ventilierte, aber nicht perfundierte Alveolen",
        answerD = "Bronchospasmus mit Erhoehung des Atemwegswiderstands",
        correctAnswer = 1,
        explanation = "Bei Pneumonie fuellen Exsudat und Entzuendungszellen die Alveolen -- diese werden nicht mehr belueftet. Das Blut fliesst aber weiterhin durch die befallenen Lungenabschnitte (Perfusion ohne Ventilation), ein Rechts-links-Shunt, der den Sauerstoffaustausch verhindert.",
        difficulty = 3,
        funFact = "Die Lobaerpneumonie durchlaeuft klassisch vier histologische Stadien: Kongestion (1. Tag), rote Hepatisation (2-3 Tage), graue Hepatisation (4-6 Tage) und Loesung -- benannt nach der leberaehnlichen Konsistenz der Lunge in den mittleren Stadien."
    ),

    // Question 12 -- Tuberculosis reactivation
    Question(
        categoryId = 16,
        questionText = "In welchem Lungensegment manifestiert sich die Reaktivierungstuberkulose bevorzugt und warum?",
        answerA = "Unterlappen basal, weil dort die Perfusion am besten ist",
        answerB = "Mittellappen, weil er schlechter belueftet ist und Sekret stagniert",
        answerC = "Oberlappen apikal, weil dort der Sauerstoffpartialdruck am hoechsten ist",
        answerD = "Hilnaehe, weil Lymphknoten als Reservoir fuer Mykobakterien dienen",
        correctAnswer = 2,
        explanation = "Mycobacterium tuberculosis ist ein obligat aerober Erreger und bevorzugt Areale mit hohem Sauerstoffgehalt. In aufrechter Koerperhaltung ist der pO2 in den Ober- und Spitzensegmenten der Lunge am hoechsten, weshalb sich Kavitaeten der Reaktivierungs-TB dort bilden.",
        difficulty = 3,
        funFact = "Etwa ein Drittel der Weltbevoelkerung traegt Mycobacterium tuberculosis als latente Infektion in sich -- bei 90 % bricht die Krankheit nie aus, bleibt aber reaktivierbar, besonders bei Immunsuppression."
    ),

    // Question 13 -- Pulmonary embolism
    Question(
        categoryId = 16,
        questionText = "Welche drei Faktoren bilden die Virchow-Trias und erklaeren die Entstehung von Venenthrombosen (Vorstufe der Lungenembolie)?",
        answerA = "Hypertonie, Hyperlipidomie und Nikotinabusus",
        answerB = "Stase des Blutes, Gefaessendothelschaeden und Hyperkoagulabilitaet",
        answerC = "Herzrhythmusstoerungen, Klappenveraenderungen und paradoxe Embolie",
        answerD = "Immobilitaet, Adipositas und erhoehter Haematokrit",
        correctAnswer = 1,
        explanation = "Rudolf Virchow beschrieb 1856 drei thrombogene Faktoren: verlangsamter Blutfluss (Stase), Gefaessendothelschaeden und veraenderte Blutgerinnungseigenschaften (Hyperkoagulabilitaet). Alle drei erhoehen das Thromboembolierisiko unabhaengig und in Kombination.",
        difficulty = 3,
        funFact = "Die tiefe Beinvenenthrombose als Vorstufe der Lungenembolie kann nach nur 3 Stunden Immobilitaet in einem Flugzeug entstehen -- weshalb Langstreckenpassagieren Kompressionsstruempfe und regelmaessige Bewegung empfohlen werden."
    ),

    // Question 14 -- ARDS
    Question(
        categoryId = 16,
        questionText = "Was kennzeichnet das Acute Respiratory Distress Syndrome (ARDS) auf radiologischer und physiologischer Ebene?",
        answerA = "Einseitiger Pleuraerguss mit ipsilateraler Lungenkollabierung",
        answerB = "Bilaterale Infiltrate durch kapillaere Leckage mit schwerer Hypoxaemie trotz hoher Sauerstoffzufuhr",
        answerC = "Ueberblahung der Lunge mit erniedrigtem CO2 durch Hyperventilation",
        answerD = "Zentrale Bronchiektasen durch chronischen Mukus-Stau mit rekurrenten Infekten",
        correctAnswer = 1,
        explanation = "ARDS entsteht durch massive Entzuendung, die die alveolaer-kapillare Membran schaedigt. Protein- und fluessigkeitsreiches Exsudat fuellt die Alveolen, was im Roentgen/CT bilaterale diffuse Verschattungen zeigt. Der PaO2/FiO2-Quotient faellt unter 300 mmHg.",
        difficulty = 3,
        funFact = "Bei ARDS-Beatmung gilt heute das Prinzip der Lungenschutzventilation mit kleinen Tidalvolumina (6 ml/kg Idealkoerpergewicht) -- groessere Volumina verschlimmern den Schaden durch Volutrauma, was erst in den 2000er-Jahren erkannt wurde."
    ),

    // Question 15 -- Cystic fibrosis lung
    Question(
        categoryId = 16,
        questionText = "Wie beeintraechtigt der CFTR-Defekt bei zystischer Fibrose die Lungenfunktion auf molekularer Ebene?",
        answerA = "CFTR-Mutation aktiviert Mastzellen, die bronchiale Hyperreaktivitaet ausloesen",
        answerB = "Defekter Chloridkanal vermindert Wassersekretion, sodass zaehes Sekret Atemwege verlegt",
        answerC = "CFTR reguliert Surfactant-Produktion; Mangel fuehrt zu Alveolenkollaps",
        answerD = "Fehlendes CFTR-Protein hemmt Makrophagen-Funktion mit chronischen Infektionen",
        correctAnswer = 1,
        explanation = "Der CFTR-Kanal transportiert Chloridionen in den Bronchialschleim. Ohne funktionierendes CFTR bleibt der Schleim wasserarm und hochviskoes, klebt an Epithelzellen und kann nicht abgehustet werden -- idealer Naehrboden fuer chronische bakterielle Infektionen.",
        difficulty = 3,
        funFact = "Pseudomonas aeruginosa ist der haeufigste chronische Lungenerreger bei zystischer Fibrose und bildet im Laufe der Zeit einen Biofilm, der Antibiotika nahezu unzugaenglich ist -- einmal etabliert, kann er nicht mehr eliminiert werden."
    ),

    // --- ENDOCRINE DISORDERS ---

    // Question 16 -- Cushing syndrome
    Question(
        categoryId = 16,
        questionText = "Welches klinische Zeichen unterscheidet ein echtes Cushing-Syndrom von einer einfachen Adipositas?",
        answerA = "Body-Mass-Index ueber 35 mit stammbeton ter Fettverteilung",
        answerB = "Proximale Muskelschwaeche mit violetten Striae und Vollmondgesicht",
        answerC = "Diastolische Hypertonie mit erhoehter Harnsaeure",
        answerD = "Nuechterhyperglykamie ueber 126 mg/dl bei zwei Messungen",
        correctAnswer = 1,
        explanation = "Das Cushing-Syndrom (Hyperkortisolismus) zeigt charakteristische Zeichen der katabolen Kortisol-Wirkung: proximale Muskelschwaeche durch Muskelabbau, breite violette Striae durch Hautatrophie und Kollagenverlust sowie Vollmondgesicht durch faziale Fettumverteilung.",
        difficulty = 3,
        funFact = "Harvey Cushing beschrieb das nach ihm benannte Syndrom 1912 bei einer Patientin mit Hypophysenadenom -- er vermutete damals als erster eine hormonelle Ursache und legte damit den Grundstein fuer die moderne Neuroendokrinologie."
    ),

    // Question 17 -- Addison's disease
    Question(
        categoryId = 16,
        questionText = "Welcher Elektrolyt-Befund ist typisch fuer die unbehandelte Addison-Krise und erklaert sich aus dem Aldosteronmangel?",
        answerA = "Hyperkalaemie und Hyponatriaemie durch Verlust der Mineralokortikoid-Wirkung",
        answerB = "Hypokalaemie und Hypernatriaemie durch kompensatorische Renin-Aktivierung",
        answerC = "Hyperkalzaemie und Hypophosphatamie durch Mangel an adrenalen Kalzitonin",
        answerD = "Hypomagnesiamie und metabolische Alkalose durch renalen Magnesiumverlust",
        correctAnswer = 0,
        explanation = "Aldosteron foerdert die Natriumrueckresorption und Kaliumausscheidung in den Nierentubuli. Bei Aldosteronmangel (Morbus Addison) verliert der Koerper Natrium mit dem Urin und retiniert Kalium -- Hyponatriaemie, Hyperkalaemie und Hypovolaemie sind die Folge.",
        difficulty = 3,
        funFact = "US-Praesident John F. Kennedy litt an Morbus Addison und nahm taeglich Kortison -- dieses wurde jahrzehntelang geheimgehalten, weil man befuerchtete, die oeffentliche Meinung zu verlieren, wenn die Krankheit bekannt wuerde."
    ),

    // Question 18 -- Acromegaly
    Question(
        categoryId = 16,
        questionText = "Welches Hormon ist bei Akromegalie direkt fuer das Knochenwachstum verantwortlich und von wo wird es ausgeschuettet?",
        answerA = "Wachstumshormon (GH) direkt aus einem Hypophysenadenom",
        answerB = "IGF-1 (Insulin-like Growth Factor 1), produziert in der Leber auf Stimulation durch GH",
        answerC = "Prolaktin aus der Adenohypophyse mit indirekter Wachstumswirkung",
        answerD = "Parathormon aus den Nebenschilddruesen mit osteoblastaerer Aktivierung",
        correctAnswer = 1,
        explanation = "Bei Akromegalie produziert ein Hypophysenadenom exzessiv Wachstumshormon (GH). GH stimuliert die Leber zur IGF-1-Synthese, und IGF-1 vermittelt die eigentlichen Wachstumseffekte an Knochen, Knorpel und Weichteilgewebe.",
        difficulty = 3,
        funFact = "Akromegalie entsteht erst nach dem Schluss der Wachstumsfugen -- vorher wuerde exzessives GH zum Gigantismus fuehren. Der groesste bekannte Mensch der Geschichte, Robert Wadlow (2,72 m), litt an hypophysaerem Gigantismus."
    ),

    // Question 19 -- Hypothyroidism ECG
    Question(
        categoryId = 16,
        questionText = "Welches EKG-Zeichen tritt typischerweise bei schwerer Hypothyreose auf?",
        answerA = "ST-Streckenelevation durch Perikardreizung bei Myxoedem-Perikarditis",
        answerB = "Niedervoltage mit PQ-Verlaengerung durch Perikarderguss und verminderte Leitungsgeschwindigkeit",
        answerC = "Deltawelle durch akzessorische Leitungsbahn bei Bradykardie",
        answerD = "Spitze P-Wellen durch biatriale Hypertrophie",
        correctAnswer = 1,
        explanation = "Bei schwerer Hypothyreose koennen Perikarderguss (fuehrt zu Niedervoltage) sowie verminderte Herzleitungsgeschwindigkeit (verlaengert PQ-Intervall und QRS-Komplex) auftreten. Bradykardie ist fast immer vorhanden.",
        difficulty = 3,
        funFact = "Das Myxoedem-Koma ist die lebensbedrohlichste Komplikation der Hypothyreose -- mit einer Mortalitaet von bis zu 40 % trotz Behandlung. Es wird oft durch Kaelte, Infektionen oder Medikamente ausgeloest."
    ),

    // Question 20 -- Pheochromocytoma
    Question(
        categoryId = 16,
        questionText = "Welche biochemischen Marker werden zur Diagnose eines Phaeochromozytoms bevorzugt bestimmt?",
        answerA = "Serum-Aldosteron und Plasma-Renin-Aktivitaet im Liegen und Stehen",
        answerB = "Metanephrine im Plasma oder fraktionierte Metanephrine im 24h-Urin",
        answerC = "Chromogranin A und NSE (Neuronenspezifische Enolase) im Serum",
        answerD = "VMA (Vanillinmandelsaeure) und Homovanillinmandelsaeure im Spot-Urin",
        correctAnswer = 1,
        explanation = "Plasmametanephrine (Normetanephrin, Metanephrin) werden im Tumor kontinuierlich produziert und haben eine Sensitivitaet von ueber 97 % fuer Phaeochromozytom. Fraktionierte Metanephrine im 24h-Urin sind die Alternative -- beide sind sensitiver als Katecholamine selbst.",
        difficulty = 3,
        funFact = "Phaeochromozytome gehoeren zu den sogenannten 10-Prozent-Tumoren: ca. 10 % sind boesartig, 10 % bilateral, 10 % extraadrenal und 10 % treten bei Kindern auf -- obwohl diese Zahlen in der modernen Literatur etwas abweichen."
    ),

    // --- NEUROLOGICAL CONDITIONS ---

    // Question 21 -- Parkinson's mechanism
    Question(
        categoryId = 16,
        questionText = "Welcher neurochemische Mechanismus liegt dem Morbus Parkinson zugrunde und ab welchem Ausmass des Neuronenverlusts werden Symptome klinisch sichtbar?",
        answerA = "Acetylcholinmangel im Hippocampus, ab 20 % Verlust kortikaler Neurone",
        answerB = "Dopaminmangel im nigrostriatalen Trakt, ab ca. 60-80 % Verlust der Neurone der Substantia nigra",
        answerC = "Serotonin-Defizit in den Raphe-Kernen, ab 50 % synaptischem Verlust",
        answerD = "GABA-Ueberschuss in den Basalganglien durch Hemmung der thalamokortikalen Projektion",
        correctAnswer = 1,
        explanation = "Beim Morbus Parkinson degenerieren dopaminerge Neurone in der Substantia nigra pars compacta. Da das Gehirn gut kompensieren kann, werden erst Symptome wie Tremor und Rigor klinisch manifest, wenn 60-80 % dieser Neurone abgestorben sind.",
        difficulty = 3,
        funFact = "Alpha-Synuclein-Aggregate (Lewy-Koerperchen) gelten als pathologisches Kennzeichen des Morbus Parkinson -- nach der Braak-Hypothese finden sich diese Ablagerungen zuerst im Riechkolben und Hirnstamm, erst spaeter im Mittelhirn."
    ),

    // Question 22 -- Stroke types
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einem haemorrhagischen und einem ischaemischen Schlaganfall hinsichtlich der Akutbehandlung?",
        answerA = "Beide werden mit Thrombolytika (rtPA) behandelt, haemorrhagisch mit niedrigerer Dosis",
        answerB = "Ischaemischer Schlaganfall kann mit rtPA lysiert werden; haemorrhagischer Schlaganfall ist eine Kontraindikation fuer Lysetherapie",
        answerC = "Haemorrhagischer Schlaganfall wird mit Heparin antikoaguliert, ischaemischer mit Kortison",
        answerD = "Beide Formen werden initial mit Thrombozytenaggregationshemmern behandelt",
        correctAnswer = 1,
        explanation = "Beim ischaemischen Schlaganfall kann die thrombolytische Therapie mit rtPA innerhalb von 4,5 Stunden den Thrombus aufloesen und Nervenzellen retten. Beim haemorrhagischen Schlaganfall wuerde rtPA die bestehende Blutung massiv verstaerken und ist absolut kontraindiziert.",
        difficulty = 3,
        funFact = "Vor der Lysetherapie muss zwingend ein CT des Kopfes gemacht werden -- denn klinisch sind ischaemischer und haemorrhagischer Schlaganfall kaum voneinander zu unterscheiden, waehrend die Behandlung diametral entgegengesetzt ist."
    ),

    // Question 23 -- Guillain-Barre
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert das Guillain-Barre-Syndrom und welcher Liquorbefund ist charakteristisch?",
        answerA = "Direkte Virusinfektion motorischer Neurone; Liquor mit Pleozytose und normalem Protein",
        answerB = "Autoimmune Demyelinisierung peripherer Nerven; Liquor mit erhoehtem Protein bei normaler Zellzahl (albuminozytaere Dissoziation)",
        answerC = "Vaskulitische Schaedigung der Vasa nervorum; Liquor mit xanthochromem Ueberstand",
        answerD = "Toxininduzierte neuromuskulaere Blockade; Liquor mit Nachweis bakterieller Antigene",
        correctAnswer = 1,
        explanation = "GBS ist eine akute autoimmune Polyneuropathie, bei der meist nach einer Infektion kreuzreaktive Antikoerper die Myelinscheiden peripherer Nerven angreifen. Der klassische Liquorbefund ist hohes Protein bei normaler Zellzahl -- albuminozytaere Dissoziation.",
        difficulty = 3,
        funFact = "Das Guillain-Barre-Syndrom schreitet klassisch aufsteigend vor -- beginnend mit Schwaechegefuehl in den Beinen bis hin zur Atemlaehming. Bei rechtzeitiger Intensivbehandlung erholen sich jedoch 80 % der Patienten vollstaendig."
    ),

    // Question 24 -- Meningitis diagnosis
    Question(
        categoryId = 16,
        questionText = "Welcher Liquorbefund ist typisch fuer eine eitrige (bakterielle) Meningitis im Vergleich zur viralen Meningitis?",
        answerA = "Klarer Liquor, Lymphozytose, normales Glukose, leicht erhoehtes Protein",
        answerB = "Trueber Liquor, Granulozytose, erniedrigte Glukose, stark erhoehtes Protein",
        answerC = "Xanthochromer Liquor, Erythrozyten, erhoehtes Ferritin",
        answerD = "Farbloser Liquor, Eosinophilie, normales Protein, niedrige Laktat",
        correctAnswer = 1,
        explanation = "Bakterielle Meningitis zeigt im Liquor eine massiv erhoehte Zellzahl mit vorwiegend neutrophilen Granulozyten, erniedrigte Glukose (Bakterien verbrauchen sie), stark erhoehtes Protein und erhoehtes Laktat. Die virale Meningitis zeigt dagegen eine lymphozytaere Pleozytose mit normaler Glukose.",
        difficulty = 3,
        funFact = "Das bakterielle Meningitis-Risiko ist beim Haushaltskontakt eines Erkrankten 1000-fach hoeher als in der Normalbevoelkerung -- weshalb Chemoprophylaxe mit Rifampicin oder Ciprofloxacin fuer Kontaktpersonen empfohlen wird."
    ),

    // Question 25 -- Status epilepticus
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Status epilepticus und ab welcher Dauer spricht man klinisch von diesem Notfall?",
        answerA = "Ein einziger generalisierter Anfall, der laenger als 30 Sekunden andauert",
        answerB = "Anhaltende epileptische Aktivitaet oder wiederkehrende Anfaelle ohne Bewusstseinserholung, die laenger als 5 Minuten andauern",
        answerC = "Drei oder mehr Anfaelle innerhalb von 24 Stunden bei bekannter Epilepsie",
        answerD = "Postictal-Phase mit Bewusstseinstroebung, die laenger als 1 Stunde besteht",
        correctAnswer = 1,
        explanation = "Der Status epilepticus ist ein neurologischer Notfall: Anfaelle, die laenger als 5 Minuten anhalten, oder multiple Anfaelle ohne zwischenzeitliche Erholung. Ab dieser Zeitgrenze normalisieren sich GABA-Rezeptoren nicht spontan -- die Situation wird ohne Behandlung zunehmend therapieresistenter.",
        difficulty = 3,
        funFact = "Fruehiere Leitlinien definierten Status epilepticus erst ab 30 Minuten -- die Absenkung auf 5 Minuten in modernen Leitlinien spiegelt die Erkenntnis wider, dass auch kuerzere Anfaelle bereits zu Hirnschaeden fuehren koennen."
    ),

    // --- KIDNEY DISEASES ---

    // Question 26 -- Nephritis vs nephrosis
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet das nephritische vom nephrotischen Syndrom klinisch und pathophysiologisch?",
        answerA = "Nephritisch: massive Proteinurie (>3,5 g/d) mit Oedemen; nephrotisch: Haematurie mit Hypertonie",
        answerB = "Nephritisch: Haematurie, Hypertonie, Azotaemie; nephrotisch: massive Proteinurie, Hypoalbuminaemie, Oedeme",
        answerC = "Beide Syndrome unterscheiden sich nur durch die Ursache, nicht durch klinische Zeichen",
        answerD = "Nephritisch: Hyperurikamie durch Tubulusstoerung; nephrotisch: Hyperkalaemie durch GFR-Abfall",
        correctAnswer = 1,
        explanation = "Das nephritische Syndrom (Glomerulonephritis) entsteht durch Entzuendung der Glomeruli: Haematurie, Hypertonie durch Salz-Wasser-Retention und Azotaemie. Das nephrotische Syndrom zeigt geschaedigte Filtrationsmembran: massive Proteinurie, Hypoalbuminaemie und Oedeme.",
        difficulty = 3,
        funFact = "Beim nephrotischen Syndrom koennen durch den Albumin- und Immunglobulinverlust im Urin schwere Infektionskomplikationen entstehen -- besonders gefuerchtet ist die spontane bakterielle Peritonitis bei Kindern mit minimalem Laesionssyndrom."
    ),

    // Question 27 -- Dialysis principles
    Question(
        categoryId = 16,
        questionText = "Welches physikalische Prinzip unterscheidet Haemofiltration von der Haemodialyse?",
        answerA = "Haemodialyse: Diffusion entlang Konzentrationsgradienten; Haemofiltration: Konvektion (Ultrafiltration mit Substitutionslosung)",
        answerB = "Haemodialyse: Osmotischer Druckausgleich; Haemofiltration: Elektrostatische Ionenbindung",
        answerC = "Beide basieren auf identischen Mechanismen, unterscheiden sich nur in der Membrangroesse",
        answerD = "Haemodialyse: Adsorption an Aktivkohlemembran; Haemofiltration: Fraktionierung durch Zentrifugation",
        correctAnswer = 0,
        explanation = "Bei der Haemodialyse wandern geloeste Stoffe entlang eines Konzentrationsgradienten durch eine semipermeable Membran in die Dialyseloesung (Diffusion). Bei der Haemofiltration wird Plasma unter Druck durch die Membran gepresst (Konvektion/Ultrafiltration) und gleichzeitig durch Substitutionsloesung ersetzt.",
        difficulty = 3,
        funFact = "Ein durchschnittlicher Haemodialyse-Patient verbringt etwa 600 Stunden pro Jahr an der Dialyse -- bei 3 Sitzungen woechentlich a 4-5 Stunden. Eine Nierentransplantation verbessert die Lebensqualitaet und -erwartung erheblich."
    ),

    // Question 28 -- Kidney stones
    Question(
        categoryId = 16,
        questionText = "Welcher Nierenstein-Typ ist roentgenologisch transparent (im normalen Roentgenbild nicht sichtbar) und aus welchem Stoff besteht er?",
        answerA = "Kalziumoxalat-Stein, weil Kalzium schwach roentgendicht ist",
        answerB = "Harnsaeure-Stein (Urat), weil organische Saeure keine Roentgendichte hat",
        answerC = "Struvit-Stein (Magnesium-Ammonium-Phosphat), weil Phosphat roentgentransparent ist",
        answerD = "Zystin-Stein, weil Aminosaeuren keine Metallatome enthalten",
        correctAnswer = 1,
        explanation = "Harnsaeure-Steine bestehen aus organischen Molekuelen ohne Kalzium oder andere schwere Atome und sind daher im konventionellen Roentgenbild nicht sichtbar. Sie sind jedoch im CT und Ultraschall gut nachweisbar und koennen durch Alkalisierung des Urins (pH >6,5) aufgeloest werden.",
        difficulty = 3,
        funFact = "Kalziumoxalatsteine sind mit 70-80 % die haeufigsten Nierensteine, aber Harnsaeuresteine haben gute Nachrichten fuer Patienten: Sie sind einer der wenigen Steintypen, der sich medikamentoes durch Natriumbicarbonat-Gabe aufloesen laesst."
    ),

    // Question 29 -- Chronic kidney disease stages
    Question(
        categoryId = 16,
        questionText = "Ab welchem GFR-Wert (glomerulaere Filtrationsrate) spricht man von einer chronischen Niereninsuffizienz im Stadium G5 (Nierenversagen) nach KDIGO-Klassifikation?",
        answerA = "GFR unter 60 ml/min/1,73m2",
        answerB = "GFR unter 30 ml/min/1,73m2",
        answerC = "GFR unter 15 ml/min/1,73m2",
        answerD = "GFR unter 5 ml/min/1,73m2",
        correctAnswer = 2,
        explanation = "Die KDIGO-Klassifikation teilt chronische Niereninsuffizienz in 5 Stadien ein. Stadium G5 (Nierenversagen) beginnt bei GFR unter 15 ml/min/1,73m2 -- ab diesem Punkt ist in der Regel eine Nierenersatztherapie (Dialyse oder Transplantation) notwendig.",
        difficulty = 3,
        funFact = "Die Niere hat eine enorme Reservekapazitaet -- man kann problemlos mit einer gesunden Niere leben. Daher kann eine gesunde Person eine Niere spenden, ohne relevante Einschraenkung der Lebensqualitaet oder -erwartung zu erleiden."
    ),

    // Question 30 -- Renal tubular acidosis
    Question(
        categoryId = 16,
        questionText = "Welcher Typ der renalen tubulaeren Azidose (RTA) ist durch eine Stoerung der Bikarbonatrueckresorption im proximalen Tubulus charakterisiert?",
        answerA = "Typ 1 (distale RTA) durch Defekt der H+-Sekretion im Sammelrohr",
        answerB = "Typ 2 (proximale RTA) durch gestoerte Bikarbonatrueckresorption im proximalen Tubulus",
        answerC = "Typ 4 (hyperkalaemische RTA) durch Aldosteronmangel oder -resistenz",
        answerD = "Typ 3 (gemischte RTA) durch kombinierte proximal-distale Stoerung",
        correctAnswer = 1,
        explanation = "Bei Typ-2-RTA (proximale RTA) ist die Bikarbonatrueckresorption im proximalen Tubulus gestoert. Trotz normaler distaler Ansauerung kann der Urin-pH nie unter 5,5 gesenkt werden, weil nicht ausreichend Bikarbonat rueckresorbiert wird. Haeufige Ursache: Fanconi-Syndrom.",
        difficulty = 3,
        funFact = "Das Fanconi-Syndrom als Ursache der Typ-2-RTA zeigt eine globale Tubulusstoerung mit Verlust von Glukose, Aminosaeuren, Phosphat und Harnsaeure -- alle mit normalem Blutspiegel. Morbus Wilson und Cystinose sind klassische Ursachen."
    ),

    // --- LIVER DISEASES ---

    // Question 31 -- Hepatitis types
    Question(
        categoryId = 16,
        questionText = "Welche beiden Hepatitis-Viren werden hauptsaechlich faekal-oral uebertragen (also ueber kontaminiertes Wasser und Lebensmittel)?",
        answerA = "Hepatitis B und D",
        answerB = "Hepatitis A und E",
        answerC = "Hepatitis C und G",
        answerD = "Hepatitis A und C",
        correctAnswer = 1,
        explanation = "Hepatitis A (HAV) und Hepatitis E (HEV) werden faekal-oral uebertragen. Hepatitis B, C und D werden parenteral (Blut) oder sexuell uebertragen. HAV fuehrt nie zur chronischen Hepatitis; HEV kann bei Immunsupprimierten chronisch werden.",
        difficulty = 3,
        funFact = "Hepatitis E ist in Europa selten und wird meist als Reisemitbringsel betrachtet -- aber Schweinefleisch ist ein weiterer Uebertragungsweg, da Hausschweinbestaende in Deutschland zu 30-50 % HEV-positiv sind."
    ),

    // Question 32 -- Liver cirrhosis complications
    Question(
        categoryId = 16,
        questionText = "Durch welchen Mechanismus entsteht die Oesophagusvarizenblutung bei Leberzirrhose?",
        answerA = "Gerinnungsstoerung fuehrt zu spontaner Blutung aus gesunden Venen",
        answerB = "Portale Hypertension leitet Blut ueber Kollateralen in die Oesophagusplexusvenen um, die unter Druck rupturieren",
        answerC = "Leberversagen fuehrt zu Erbrechen, das mechanisch Venen zerreisst (Mallory-Weiss)",
        answerD = "Hypersplenismus zerstoert Thrombozyten, was Blutungen in varikoese Venen ausloest",
        correctAnswer = 1,
        explanation = "Bei Leberzirrhose erhoehen Fibrose und Regeneratknoten den Widerstand im portalen Kreislauf (portale Hypertension). Das Blut sucht Umgehungswege (Kollateralen) -- eine Route fuehrt ueber den Plexus oesophageus, dessen duennwandige Varizen bei Druckerhoehung rupturieren koennen.",
        difficulty = 3,
        funFact = "Oesophagusvarizenblutungen sind hochgefaehrlich: die erste Blutungsepisode hat eine Mortalitaet von 15-30 %, und ohne Behandlung blutet die Haelfte der Patienten innerhalb von 2 Jahren erneut."
    ),

    // Question 33 -- NAFLD/NASH
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet die nicht-alkoholische Fettleberhepatitis (NASH) von einer einfachen Fettleber (NAFL) histologisch und prognostisch?",
        answerA = "NASH und NAFL sind identisch, unterscheiden sich nur im Grad der Triglyzeridablagerung",
        answerB = "NASH zeigt zusaetzlich zur Steatose eine lobulare Entzuendung und Hepatozytenschaeden; nur NASH kann zur Zirrhose progredieren",
        answerC = "NAFL ist immer autoimmun; NASH ist durch Insulinresistenz bedingt",
        answerD = "Nur NAFL zeigt Ballonierung der Hepatozyten; NASH zeigt reine Infiltration",
        correctAnswer = 1,
        explanation = "NAFL (simple Steatose) zeigt nur Fetteinlagerung in Hepatozyten ohne wesentliche Entzuendung -- sie ist in der Regel gutartig. NASH (Steatohepatitis) zeigt zusaetzlich lobulare Entzuendung, Hepatozytenballonierung und oft Fibrose -- und kann zur Zirrhose und hepatozellulaeren Karzinom fortschreiten.",
        difficulty = 3,
        funFact = "NAFLD ist heute die haeufigste chronische Lebererkrankung weltweit und betrifft schaetzungsweise 25 % der Weltbevoelkerung -- direkt korreliert mit dem globalen Anstieg von Adipositas und Typ-2-Diabetes."
    ),

    // Question 34 -- Hepatic encephalopathy
    Question(
        categoryId = 16,
        questionText = "Welcher Stoff spielt die zentrale Rolle in der Pathogenese der hepatischen Enzephalopathie?",
        answerA = "Bilirubin, das die Blut-Hirn-Schranke penetriert und Neurotransmitter hemmt",
        answerB = "Ammoniak (NH3), der bei Leberversagen akkumuliert und neurotoxisch wirkt",
        answerC = "Laktat durch anaerobe Glukoseverstoffwechselung in der Leber",
        answerD = "Gallensaeuren, die im Liquor direkt neuronale Apoptose ausloesen",
        correctAnswer = 1,
        explanation = "Die Leber entgiftet Ammoniak durch den Harnstoffzyklus. Bei Leberversagen akkumuliert NH3 im Blut, ueberwindet die Blut-Hirn-Schranke und stoert neuronale Funktion -- besonders Astrozyten quellen auf (Hirnoedem). Lactulose und Rifaximin senken Ammoniak durch Reduktion der intestinalen Produktion.",
        difficulty = 3,
        funFact = "Der Asterixis (Flapping Tremor) -- ein flatterndes Zittern bei gestreckten Haenden -- ist das klassische klinische Zeichen der hepatischen Enzephalopathie und entsteht durch periodische Unterbrechung motorischer Impulse."
    ),

    // Question 35 -- Wilson's disease
    Question(
        categoryId = 16,
        questionText = "Welches klinische Zeichen an den Augen ist pathognomonisch fuer den Morbus Wilson und wie entsteht es?",
        answerA = "Kayser-Fleischer-Ring: kupferhaltige Ablagerungen in der Descemet-Membran der Hornhaut",
        answerB = "Kirschroter Fleck: Lipidablagerung in der Macula durch Kupfertoxizitaet",
        answerC = "Roth-Flecken: retinale Blutungen durch haemolytische Anaemie",
        answerD = "Argyll-Robertson-Pupille: autonome Innervationsstoerung durch Kupfer-Neurotoxizitaet",
        correctAnswer = 0,
        explanation = "Beim Morbus Wilson (Kupferspeicherkrankheit) lagert sich ueberschuessiges Kupfer in der Descemet-Membran der Hornhautperipherie ab und bildet einen goldbraun-gruenen Ring (Kayser-Fleischer-Ring). Dieser ist mit der Spaltlampe nachweisbar und fast beweisend fuer Wilson.",
        difficulty = 3,
        funFact = "Morbus Wilson ist eine der wenigen genetischen Erkrankungen, die gut behandelbar sind -- D-Penicillamin oder Trientine chelatieren das Kupfer und ermoeglichen bei fruehzeitiger Diagnose ein nahezu normales Leben."
    ),

    // --- GENETIC DISORDERS ---

    // Question 36 -- Cystic fibrosis genetics
    Question(
        categoryId = 16,
        questionText = "Auf welchem Chromosom liegt das CFTR-Gen und welche Mutation ist bei zystischer Fibrose am haeufigsten?",
        answerA = "Chromosom 7, deltaF508-Mutation (Deletion von Phenylalanin an Position 508)",
        answerB = "Chromosom 11, HbS-Mutation (Glutamat zu Valin an Position 6 der beta-Kette)",
        answerC = "X-Chromosom, Duplikation Exon 45-55 als haeufigste europaeische Variante",
        answerD = "Chromosom 4, CAG-Repeat-Expansion im Huntingtin-Gen",
        correctAnswer = 0,
        explanation = "Das CFTR-Gen liegt auf Chromosom 7q31.2. Die haeufigste Mutation weltweit (ca. 70 % aller CF-Allele in Europa) ist deltaF508 -- eine Deletion von drei Nukleotiden, die zum Fehlen von Phenylalanin an Position 508 des CFTR-Proteins fuehrt.",
        difficulty = 3,
        funFact = "Neuartige CFTR-Modulatoren wie Ivacaftor, Lumacaftor und Elexacaftor korrigieren den Proteinfehler direkt -- Elexacaftor/Tezacaftor/Ivacaftor (Kaftrio) verbessert bei deltaF508-Patienten die Lungenfunktion um 10-15 Prozentpunkte und gilt als Durchbruch."
    ),

    // Question 37 -- Sickle cell disease
    Question(
        categoryId = 16,
        questionText = "Warum verleiht die Heterozygotie fuer Sichelzellanaemie (Sichelzell-Trait) einen Schutz gegen schwere Malaria?",
        answerA = "HbS verhindert Plasmodium-Einlagerung von Haemozoin und stoert ATP-Produktion",
        answerB = "Sichelzell-Trait-Erythrozyten sicheln bei niedrigem O2, was Plasmodien-infizierte Zellen vorzeitig zerstoert",
        answerC = "HbS ist zu gross fuer die Merozoiten-Kapsel und verhindert Reinfektion",
        answerD = "Heterozygote haben erhoehte Spiegel an fetalem Haemoglobin, das Plasmodium nicht abbaut",
        correctAnswer = 1,
        explanation = "Plasmodium-infizierte Erythrozyten haben erhoehten Sauerstoffverbrauch. In Traegern des Sichelzell-Traits sicheln diese Zellen bei niedrigem O2 fruehzeitig, werden von der Milz erkannt und eliminiert -- bevor Plasmodien sich vollstaendig reproduzieren koennen.",
        difficulty = 3,
        funFact = "Die Sichelzellmutation ist in Malaria-endemischen Gebieten Afrikas auf eine Haeufigkeit von bis zu 40 % angestiegen -- ein beeindruckendes Beispiel fuer den evolutionaeren Selektionsdruck einer Infektionskrankheit auf das menschliche Genom."
    ),

    // Question 38 -- Huntington's disease mechanism
    Question(
        categoryId = 16,
        questionText = "Welcher genetische Mechanismus fuehrt zur Huntington-Erkrankung und welcher Erbgang liegt vor?",
        answerA = "Deletionsmutation im HTT-Gen; X-chromosomal-rezessiv",
        answerB = "CAG-Trinukleotid-Repeat-Expansion im Huntingtin-Gen; autosomal-dominant",
        answerC = "Methylierungsdefekt des HTT-Promoters; epigenetisch mit maternaler Praegung",
        answerD = "Frameshift-Mutation durch Insertion; autosomal-rezessiv mit 25 % Erkrankungsrisiko",
        correctAnswer = 1,
        explanation = "Beim Morbus Huntington liegt eine pathologische Expansion von CAG-Repeats (>36 Wiederholungen) im HTT-Gen auf Chromosom 4 vor. Das mutante Huntingtin-Protein ist toxisch und fuehrt zum Untergang von Neuronen im Striatum. Der Erbgang ist autosomal-dominant.",
        difficulty = 3,
        funFact = "Je mehr CAG-Repeats vorliegen, desto frueher bricht die Krankheit aus -- ein Phaenomen namens Antizipation. Bei juvenilem Beginn (unter 20 Jahren, mehr als 60 Repeats) ueberwiegen Steifigkeit und Krampfanfaelle statt Chorea."
    ),

    // Question 39 -- Phenylketonuria
    Question(
        categoryId = 16,
        questionText = "Welches Enzym ist bei der Phenylketonurie (PKU) defekt und welche Substanz akkumuliert?",
        answerA = "Phenylalanin-Hydroxylase; Phenylalanin akkumuliert und schaedigt das Gehirn",
        answerB = "Tyrosin-Aminotransferase; Tyrosin akkumuliert und fuehrt zu Leberschaeden",
        answerC = "Homogentisinsaeure-Oxidase; Homogentisinsaeure akkumuliert in Knorpel und Niere",
        answerD = "Galaktose-1-Phosphat-Uridyltransferase; Galaktose-1-Phosphat schaedigt Leber und Linse",
        correctAnswer = 0,
        explanation = "PKU entsteht durch Defekt der Phenylalanin-Hydroxylase (PAH), die Phenylalanin zu Tyrosin umbaut. Ohne PAH akkumuliert Phenylalanin im Blut und wird zu Phenylpyruvat abgebaut, das bei Saeuglingen neurotoxisch ist und ohne Behandlung zur geistigen Behinderung fuehrt.",
        difficulty = 3,
        funFact = "PKU ist der Klassiker des Neugeborenen-Screenings -- seit 1961 (Deutschland 1969) wird jedes Neugeborene getestet. Eine phenylalaninarme Ernaehrung reicht aus, um bei rechtzeitiger Diagnose alle Spaetfolgen zu verhindern."
    ),

    // Question 40 -- Down syndrome genetics
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus verursacht Trisomie 21 (Down-Syndrom) in den meisten Faellen?",
        answerA = "Ungleiche Rekombination zwischen Chromosom 21 und 14 mit Robertson-Translokation",
        answerB = "Non-Disjunction (Fehlverteilung der Chromosomen) waehrend meiotischer Zellteilung der Mutter",
        answerC = "Chromosomale Mosaikbildung durch mitotische Non-Disjunction in den ersten Zellteilungen",
        answerD = "Genomisches Imprinting durch Hypermethylierung der Chromosom-21-Zentromerregion",
        correctAnswer = 1,
        explanation = "In 95 % der Trisomie-21-Faelle tritt waehrend der Meiose I oder II eine Non-Disjunction (Nicht-Trennung der Chromosomen) auf, meist in der muetterlichen Eizelle. Das Kind erhaelt dadurch zwei Chromosomen 21 von der Mutter und eines vom Vater.",
        difficulty = 3,
        funFact = "Das Risiko fuer Trisomie 21 steigt stark mit dem muetterlichen Alter: Bei 20-jaehrigen Muettern liegt es bei ca. 1:1500, bei 40-jaehrigen bei 1:100 und bei 45-jaehrigen bei etwa 1:30 -- bedingt durch laengere Lagerungszeit der Eizellen."
    ),

    // --- MIXED ADVANCED TOPICS ---

    // Question 41 -- Sepsis definition
    Question(
        categoryId = 16,
        questionText = "Wie definiert die aktuelle Sepsis-3-Definition (2016) den Unterschied zwischen Sepsis und septischem Schock?",
        answerA = "Sepsis: Infektion mit Fieber >38,5 Grad; Schock: systolischer Blutdruck unter 90 mmHg",
        answerB = "Sepsis: lebensbedrohliche Organdysfunktion durch dysregulierte Wirtsantwort auf Infektion; Schock: zusaetzlich Vasopressor-Bedarf und Laktat >2 mmol/l",
        answerC = "Sepsis: positiver Blutkulturbefund; Schock: positiver Befund mit haemodynamischer Instabilitaet",
        answerD = "Sepsis: SIRS-Kriterien mit Infektionsnachweis; Schock: SIRS mit Multiorganversagen",
        correctAnswer = 1,
        explanation = "Sepsis-3 definiert Sepsis als lebensbedrohliche Organdysfunktion durch dysregulierte Immunantwort auf Infektion (SOFA-Score-Anstieg >=2). Septischer Schock ist Sepsis mit Vasopressor-Bedarf fuer mittleren arteriellen Druck >=65 mmHg UND Laktat >2 mmol/l.",
        difficulty = 3,
        funFact = "Das SIRS-Konzept (systemisches Inflammationssyndrom), das jahrzehntelang Basis der Sepsis-Definition war, wurde 2016 bewusst aufgegeben -- da SIRS-Kriterien zu unspezifisch sind und auch bei harmlosen Infekten auftreten."
    ),

    // Question 42 -- DIC
    Question(
        categoryId = 16,
        questionText = "Was ist das paradoxe Phaenomen bei der disseminierten intravasalen Koagulopathie (DIC)?",
        answerA = "Gleichzeitiges Auftreten von Thrombosen und Blutungen durch Verbrauchskoagulopathie",
        answerB = "Spontane Gerinnungsaktivierung mit kompensatorischer Hyperfibrinolyse ohne klinische Folgen",
        answerC = "Thrombozytose durch Knochenmarksstimulation bei Verbrauch peripherer Thrombozyten",
        answerD = "Antikoagulation durch massive Heparinausschuettung aktivierter Mastzellen",
        correctAnswer = 0,
        explanation = "Bei DIC werden Gerinnungsfaktoren und Thrombozyten im gesamten Koerper aktiviert (Mikrothrombosen), gleichzeitig werden diese Faktoren verbraucht (Verbrauchskoagulopathie). Das Paradoxon: Patienten zeigen sowohl thrombotische Organschaeden als auch schwere Blutungen durch Gerinnungsmangel.",
        difficulty = 3,
        funFact = "DIC ist typische Komplikation von Sepsis, Fruchtwasserembolie, schweren Traumata und bestimmten Karzinomen. Trotz intensivmedizinischer Behandlung liegt die Mortalitaet je nach Ursache bei 20-50 %."
    ),

    // Question 43 -- Inflammation mediators
    Question(
        categoryId = 16,
        questionText = "Welche Funktion haben TNF-alpha und IL-1 bei der systemischen Entzuendungsreaktion?",
        answerA = "Antiinflammatorische Zytokine, die regulatorische T-Zellen aktivieren",
        answerB = "Proinflammatorische Zytokine, die Fieber ausloesen, Gefaesspermeabilitaet erhoehen und Akutphasenproteine induzieren",
        answerC = "Wachstumsfaktoren fuer neutrophile Granulozyten aus dem Knochenmark",
        answerD = "Komplementfaktoren, die direkt bakterielle Membranen lysieren",
        correctAnswer = 1,
        explanation = "TNF-alpha und IL-1 sind primaere proinflammatorische Zytokine, die von Makrophagen freigesetzt werden. Sie induzieren Fieber (ueber Prostaglandin E2 im Hypothalamus), erhoehen die Gefaesspermeabilitaet, aktivieren Leukozyten und stimulieren die Leber zur Produktion von Akutphasenproteinen wie CRP.",
        difficulty = 3,
        funFact = "TNF-alpha-Blocker (wie Adalimumab oder Infliximab) gehoeren zu den erfolgreichsten Biologika in der Medizin und werden bei rheumatoider Arthritis, Morbus Crohn und Psoriasis eingesetzt -- kosten aber oft ueber 10.000 Euro pro Jahr."
    ),

    // Question 44 -- Coagulation cascade
    Question(
        categoryId = 16,
        questionText = "Was misst die Prothrombinzeit (PT/INR) und welche Gerinnungsfaktoren werden dabei getestet?",
        answerA = "Intrinsischer Gerinnungsweg: Faktoren VIII, IX, XI, XII",
        answerB = "Extrinsischer und gemeinsamer Gerinnungsweg: Faktoren I (Fibrinogen), II, V, VII, X",
        answerC = "Thrombozytenaktivierung und primaere Haemostase",
        answerD = "Fibrinolyse: Plasminogen und t-PA-Aktivitaet",
        correctAnswer = 1,
        explanation = "Die Prothrombinzeit misst den extrinsischen Gerinnungsweg (aktiviert durch Gewebethromboplastin/Faktor III) und den gemeinsamen Weg. Faktor VII (extrinsisch, kuerzeste Halbwertszeit) und die gemeinsamen Faktoren I, II, V, X werden erfasst -- weshalb PT/INR empfindlich fuer Vitamin-K-Mangel und Lebersyntheseleistung ist.",
        difficulty = 3,
        funFact = "Warfarin hemmt die Vitamin-K-abhaengige Aktivierung der Faktoren II, VII, IX und X. Da Faktor VII die kuerzeste Halbwertszeit hat (4-6 Stunden), steigt die INR nach Warfarin-Beginn am schnellsten an -- was aber nicht sofort vollstaendige Antikoagulation bedeutet."
    ),

    // Question 45 -- Cancer staging
    Question(
        categoryId = 16,
        questionText = "Was bedeuten die Buchstaben T, N und M im TNM-Staging-System fuer boesartige Tumoren?",
        answerA = "T = Tumorart, N = Nodulaer oder diffus, M = Malignitaetsgrad",
        answerB = "T = Tumorgroesse/-ausdehnung, N = Lymphknoten-Befall, M = Fernmetastasen",
        answerC = "T = Therapierbarkeit, N = Nekroseanteil, M = Mutationslast",
        answerD = "T = Tumormarker-Level, N = Nekrosegrad, M = Metastasierungsrate",
        correctAnswer = 1,
        explanation = "Das TNM-System der UICC beschreibt: T die Tumorgroesse und Invasion lokaler Strukturen (T1-T4), N den Befall regionaler Lymphknoten (N0-N3) und M das Vorhandensein von Fernmetastasen (M0/M1). Diese drei Parameter bestimmen zusammen das klinische Stadium.",
        difficulty = 3,
        funFact = "Das TNM-System wurde von Pierre Denoix in den 1940er-Jahren entwickelt und ist heute international standardisiert -- ein Tumor der als T2N1M0 klassifiziert wird, bedeutet weltweit dasselbe, was internationale Therapiestudien erst moeglich macht."
    ),

    // Question 46 -- Immunoglobulin structure
    Question(
        categoryId = 16,
        questionText = "Welche Immunglobulinklasse kann als einzige die Plazenta durchqueren und somit den Foeten und Neugeborenen schuetzen?",
        answerA = "IgA, weil es in sezernierten Koerperflussigkeiten wie Muttermilch vorkommt",
        answerB = "IgG, durch aktiven Transport ueber Fc-Rezeptoren (FcRn) der Trophoblastzellen",
        answerC = "IgM, weil es als Pentamer gross genug ist, um an Trophoblastenrezeptoren zu binden",
        answerD = "IgE, weil seine Fc-Region speziell fuer plazentaren Transport konstruiert ist",
        correctAnswer = 1,
        explanation = "IgG ist das einzige Immunglobulin, das aktiv durch die Plazenta transportiert wird. Trophoblastzellen exprimieren den neonatalen Fc-Rezeptor (FcRn), der IgG bindet und in die foetale Zirkulation uebertraegt. Dies verleiht Neugeborenen passive Immunitaet fuer die ersten Lebenswochen.",
        difficulty = 3,
        funFact = "Der plazentare IgG-Transfer ist so effizient, dass Neugeborene bei der Geburt sogar hoehere IgG-Spiegel haben als die Mutter. Dieser Leihschutz nimmt aber ab dem ersten Lebensmonat ab und verschwindet bis zum 6. Monat -- danach muss das Kind selbst Antikoerper bilden."
    ),

    // Question 47 -- Antibiotics mechanism
    Question(
        categoryId = 16,
        questionText = "Durch welchen Mechanismus toeten Beta-Lactam-Antibiotika Bakterien?",
        answerA = "Hemmung der bakteriellen RNA-Polymerase und Stoerung der Proteinsynthese",
        answerB = "Irreversible Bindung an Penicillin-bindende Proteine (PBPs) mit Hemmung der Zellwandsynthese",
        answerC = "Porenbildung in der bakteriellen Zellmembran mit Verlust des Membranpotenzials",
        answerD = "Chelatierung zweiwertiger Kationen, die fuer bakterielle DNA-Gyrase notwendig sind",
        correctAnswer = 1,
        explanation = "Beta-Lactame (Penicilline, Cephalosporine, Carbapeneme) binden kovalent und irreversibel an Penicillin-bindende Proteine (Transpeptidasen), die fuer die Quervernetzung des bakteriellen Peptidoglykans (Zellwand) essenziell sind. Ohne Zellwandsynthese lysen wachsende Bakterien.",
        difficulty = 3,
        funFact = "Bakterien entwickeln Resistenzen gegen Beta-Lactame vor allem durch Beta-Lactamasen -- Enzyme, die den Beta-Lactam-Ring hydrolytisch spalten. ESBL-bildende Erreger (Extended Spectrum Beta-Lactamases) sind heute ein massives Problem in Krankenhaeusern weltweit."
    ),

    // Question 48 -- Vitamin B12 deficiency
    Question(
        categoryId = 16,
        questionText = "Welche neurologische Erkrankung kann durch schweren Vitamin-B12-Mangel entstehen und welche Rueckenmark-Abschnitte werden geschaedigt?",
        answerA = "Periphere Polyneuropathie durch Demyelinisierung der Beine",
        answerB = "Funikulaere Myelose durch Degeneration der Hinter- und Seitenstrange des Rueckenmarks",
        answerC = "Amyotrophe Lateralsklerose durch oxidativen Stress in Vorderhornzellen",
        answerD = "Friedreich-Ataxie durch Degeneration der Kleinhirnbahnen",
        correctAnswer = 1,
        explanation = "Vitamin B12 ist fuer die Myelinsynthese essenziell. Bei Mangel degenerieren die Hinterstraenge (Propriozeption, Tiefensensibilitaet) und Seitenstrange (Pyramidenbahn) des Rueckenmarks -- funikulaere Myelose. Klinisch: spastische Parese, Ataxie und sensible Defizite.",
        difficulty = 3,
        funFact = "Die megaloblastische Anaemie tritt bei B12-Mangel oft vor der Myelose auf. Eine B12-Substitution behandelt die Anaemie, kann die Myelose aber nicht mehr rueckgaengig machen, wenn die neurologische Schaedigung bereits fortgeschritten ist."
    ),

    // Question 49 -- Acid-base balance
    Question(
        categoryId = 16,
        questionText = "Welcher Befund kennzeichnet eine metabolische Azidose mit normaler Anionenluecke (hyperchloraemische Azidose)?",
        answerA = "Erhoehter Harnstoff und Kreatinin bei diabetischer Ketoazidose",
        answerB = "Bikarbonatverlust ueber Niere oder Darm (z.B. renale tubulaere Azidose, Diarrhoe)",
        answerC = "Akkumulation organischer Saeure (Laktat, Ketonkoerper) mit erhoehter Anionenluecke",
        answerD = "CO2-Retention durch Hypoventilation mit sekundaerer Bikarbonat-Erhoehung",
        correctAnswer = 1,
        explanation = "Metabolische Azidosen mit normaler Anionenluecke entstehen durch Bikarbonat-Verlust, nicht durch Saeureakkumulation. Ursachen: renale tubulaere Azidose (Bikarbonaturie), schwere Diarrhoe (Verlust alkalischer Darmsekrete) oder Gabe von Chlorid-reichen Infusionen.",
        difficulty = 3,
        funFact = "Die Anionenluecke (AG = Na minus Cl minus HCO3) ist ein cleveres Rechentool mit Normalwert 8-12 mEq/l. Eine erhoehte AG zeigt kumulative Saeuren an -- die Merkformel MUDPILES steht fuer Methanol, Uraemie, Diabetische Ketoazidose, Propylene Glycol, Isoniazid, Laktatazidose, Ethylenglycol und Salizylatvergiftung."
    ),

    // Question 50 -- Immunodeficiency
    Question(
        categoryId = 16,
        questionText = "Welche opportunistischen Infektionen sind klassisch fuer eine schwere T-Zell-Immundefizienz (z.B. fortgeschrittene HIV-Erkrankung mit CD4 unter 200/ul)?",
        answerA = "Staphylokokken-Sepsis und rezidivierende Pneumokokken-Pneumonien",
        answerB = "Pneumocystis-jirovecii-Pneumonie, Toxoplasma-Enzephalitis, CMV-Retinitis",
        answerC = "Chronische Sinusitis durch Haemophilus und rezidivierende Otitis media",
        answerD = "Schwere Candida-Oesophagitis mit Aspergillus-Sinusitis und mukokutaner Candidose",
        correctAnswer = 1,
        explanation = "T-Zell-Defizienzen heben die zellulaere Immunitaet auf, die intrazellulraere Erreger und bestimmte Pilze bekaempft. Unter 200 CD4-Zellen/ul treten AIDS-definierende Erkrankungen auf: PCP (Pneumocystis), CMV-Retinitis (kann zur Erblindung fuehren), Toxoplasma-Abszesse im Gehirn.",
        difficulty = 3,
        funFact = "Antiretrovirale Therapie (ART) hat HIV von einer toedlichen Diagnose zu einer kontrollierbaren chronischen Erkrankung gemacht -- Menschen mit HIV, die fruehzeitig behandelt werden, haben heute eine nahezu normale Lebenserwartung."
    )

)
