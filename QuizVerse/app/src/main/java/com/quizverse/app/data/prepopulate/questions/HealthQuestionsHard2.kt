package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsHard2(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 16,
        questionText = "Welchen Wirkmechanismus haben Beta-Lactam-Antibiotika wie Penicillin?",
        answerA = "Sie hemmen die bakterielle Proteinsynthese am Ribosom",
        answerB = "Sie blockieren die DNA-Gyrase und verhindern die DNA-Replikation",
        answerC = "Sie hemmen die Transpeptidase und stoeren den Aufbau der Zellwand",
        answerD = "Sie destabilisieren die Zellmembran durch Porenbildung",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Beta-Lactam-Antibiotika binden irreversibel an Penicillin-bindende Proteine (PBPs), die als Transpeptidasen die Quervernetzung von Peptidoglykan-Ketten in der Bakterienzellwand katalysieren. Ohne diese Quervernetzung wird die Zellwand instabil und die Bakterien platzen durch osmotischen Druck.",
        funFact = "Alexander Fleming entdeckte Penicillin 1928 zunaechst durch Zufall -- ein Schimmelpilz hatte seine Bakterienkultur kontaminiert und einen klaren Hof gebildet, in dem keine Bakterien wuchsen."
    ),

    // Question 2
    Question(
        categoryId = 16,
        questionText = "Wie wirken Aminoglykosid-Antibiotika wie Gentamicin?",
        answerA = "Sie hemmen die 30S-Untereinheit des bakteriellen Ribosoms und verfaelschen die Proteinsynthese",
        answerB = "Sie blockieren die 50S-Untereinheit und verhindern die Peptidkettenverlaengerung",
        answerC = "Sie hemmen die RNA-Polymerase und unterbinden die Transkription",
        answerD = "Sie hemmen die Zellwandsynthese durch Bindung an D-Alanin",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Aminoglykoside binden an die 16S-rRNA der 30S-Ribosomenuntereinheit. Dies fuehrt zu einer Fehlablesung der mRNA, wodurch fehlerhafte Proteine synthetisiert werden, die die Bakterienmembran schaedigen.",
        funFact = "Aminoglykoside sind wegen ihrer Nephrotoxizitaet und Ototoxizitaet gefuerchtet -- ein zu hoher Spiegel kann zu dauerhaftem Hoerverlust fuehren, weshalb das Therapeutische Drug Monitoring bei diesen Antibiotika Pflicht ist."
    ),

    // Question 3
    Question(
        categoryId = 16,
        questionText = "Was ist der Resistenzmechanismus bei MRSA gegenueber Methicillin?",
        answerA = "MRSA produziert Beta-Lactamasen, die den Beta-Lactam-Ring hydrolysieren",
        answerB = "MRSA exprimiert ein veraendertes Penicillin-bindendes Protein (PBP2a) mit geringer Affinitaet zu Beta-Lactamen",
        answerC = "MRSA pumpt Methicillin aktiv aus der Zelle heraus",
        answerD = "MRSA bildet einen Biofilm, der Antibiotika zurueckhaelt",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "MRSA traegt das mecA-Gen, das fuer PBP2a kodiert. Dieses veraenderte Protein uebernimmt die Zellwandsynthese, hat aber eine so geringe Bindungsaffinitaet zu Beta-Lactam-Antibiotika, dass diese ihre Wirkung nicht mehr entfalten koennen.",
        funFact = "MRSA wurde erstmals 1961 -- nur ein Jahr nach der Einfuehrung von Methicillin -- isoliert. Das zeigt, wie schnell Bakterien Resistenzmechanismen entwickeln koennen."
    ),

    // Question 4
    Question(
        categoryId = 16,
        questionText = "Welche Klasse von Antibiotika hemmt die DNA-Gyrase und Topoisomerase IV?",
        answerA = "Makrolide",
        answerB = "Tetracycline",
        answerC = "Fluorchinolone",
        answerD = "Glykopeptide",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Fluorchinolone wie Ciprofloxacin hemmen die bakterielle DNA-Gyrase (Topoisomerase II) und Topoisomerase IV. Diese Enzyme sind fuer das Entspiralisieren und Wiederverschliessen der DNA bei der Replikation unverzichtbar -- ihre Hemmung fuehrt zu Doppelstrangbruechen und zum Zelltod.",
        funFact = "Fluorchinolone sollten Kindern und Jugendlichen nicht gegeben werden, da sie in Tierversuchen Knorpelschaeden verursacht haben. In der Praxis werden sie nur eingesetzt, wenn keine Alternative verfuegbar ist."
    ),

    // Question 5
    Question(
        categoryId = 16,
        questionText = "Wie wirkt Vancomycin als Glykopeptid-Antibiotikum?",
        answerA = "Es bindet an D-Ala-D-Ala-Endgruppen des Peptidoglykans und blockiert dessen Vernetzung",
        answerB = "Es hemmt die ribosomale Peptidyltransferase-Aktivitaet",
        answerC = "Es bildet Poren in der Zellmembran von gramnegativen Bakterien",
        answerD = "Es inaktiviert die bakterielle Dihydrofolat-Reduktase",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Vancomycin bildet Wasserstoffbrueckenbindungen mit den D-Ala-D-Ala-Endgruppen der Peptidoglykan-Vorlaeufermolekuele und blockiert so sterisch die Transpeptidase. Vancomycin-resistente Enterokokken (VRE) umgehen dies durch den Austausch von D-Ala gegen D-Lac.",
        funFact = "Vancomycin gilt als 'letztes Mittel' fuer viele grampositive Infektionen -- doch auch dagegen gibt es inzwischen Resistenzen. Vancomycin-resistente Staphylococcus-aureus-Staemme (VRSA) sind zum Glueck noch sehr selten."
    ),

    // Question 6
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die Wirkung von mRNA-Impfstoffen wie dem COVID-19-Impfstoff von BioNTech?",
        answerA = "Die mRNA wird ins Zellkern-Genom eingebaut und produziert dauerhaft Antigen",
        answerB = "Die mRNA wird im Zytoplasma translatiert, das Antigen loest eine Immunantwort aus und die mRNA wird abgebaut",
        answerC = "Die mRNA aktiviert Toll-like-Rezeptoren und loest eine unspezifische Entzuendung aus",
        answerD = "Die mRNA wird durch reverse Transkription in DNA umgewandelt",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Lipid-Nanopartikel transportieren die mRNA in Koerperzellen, wo sie an Ribosomen das Spike-Protein translatiert wird. Das koerperfremde Protein loest eine B- und T-Zell-Antwort aus. Die mRNA selbst ist instabil und wird innerhalb weniger Tage durch zelleigene RNasen abgebaut.",
        funFact = "Die mRNA-Technologie wurde bereits seit den 1990er Jahren erforscht -- sie konnte nur kein stabiles Delivery-System entwickeln. Den Durchbruch brachte die Lipid-Nanopartikel-Technologie von Pieter Cullis und Katalin Karikoe."
    ),

    // Question 7
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet einen Lebendimpfstoff von einem inaktivierten Impfstoff?",
        answerA = "Lebendimpfstoffe enthalten abgestoetete Erreger; inaktivierte enthalten abgeschwaechte lebende Erreger",
        answerB = "Lebendimpfstoffe enthalten attenuierte (abgeschwaechte) lebende Erreger; inaktivierte enthalten abgetotete Erreger",
        answerC = "Lebendimpfstoffe wirken nur einmal; inaktivierte Impfstoffe brauchen keine Auffrischung",
        answerD = "Lebendimpfstoffe koennen kein Immungedaechtnis erzeugen; inaktivierte schon",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Lebendimpfstoffe (z.B. MMR, Gelbfieber) enthalten vermehrungsfaehige, aber abgeschwaechte Erreger, die eine starke und langanhaltende Immunantwort ausloesen, aber bei Immungesunden keine Krankheit verursachen. Inaktivierte Impfstoffe (z.B. Polio-IPV, Grippe) enthalten abgetotete Erreger und brauchen oft Auffrischungen.",
        funFact = "Der erste Lebendimpfstoff der Geschichte war der Pockenschutzimpfstoff -- Edward Jenner nutzte 1796 Kuhpockenviren, weil er beobachtet hatte, dass Melkerinnen, die Kuhpocken hatten, gegen Pocken immun waren."
    ),

    // Question 8
    Question(
        categoryId = 16,
        questionText = "Was ist ein Vektorimpfstoff und welches Beispiel gibt es?",
        answerA = "Ein Impfstoff, der ein harmloses Virus als Traeger nutzt, um genetische Information in Zellen einzuschleusen -- z.B. der AstraZeneca-COVID-Impfstoff",
        answerB = "Ein Impfstoff, der Immunzellen aus dem Blut des Patienten entnimmt und ausserhalb des Koerpers stimuliert",
        answerC = "Ein Impfstoff, der Antikoerper direkt injiziert anstatt das Immunsystem zu trainieren",
        answerD = "Ein Impfstoff, der Proteinfragmente des Erregers enthaelt und durch Liposomen verabreicht wird",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Vektorimpfstoffe nutzen harmlose Viren (z.B. modifizierte Adenoviren) als Vehikel, um genetische Information des Zielantigens in Koerperzellen einzuschleusen. Der AstraZeneca- und der Johnson-&-Johnson-COVID-Impfstoff basieren auf diesem Prinzip mit einem Schimpansen- bzw. menschlichen Adenovirus.",
        funFact = "Adenoviren als Impfstoffvektor werden schon seit den 1990er Jahren fuer die Krebsforschung und seltene Erbkrankheiten genutzt -- COVID-19 hat diese Technologie endlich auch in die Breite gebracht."
    ),

    // Question 9
    Question(
        categoryId = 16,
        questionText = "Welche Enzymfamilie ist hauptsaechlich fuer den Metabolismus von Medikamenten in der Leber verantwortlich?",
        answerA = "Monoaminoxidasen (MAO)",
        answerB = "Cytochrom-P450-Enzyme (CYP)",
        answerC = "Acetylcholinesterasen",
        answerD = "Glutathion-S-Transferasen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die Cytochrom-P450-Familie, insbesondere CYP3A4, CYP2D6 und CYP2C9, katalysiert Phase-I-Reaktionen (Oxidation, Reduktion, Hydrolyse) und metabolisiert etwa 70-80% aller klinisch eingesetzten Medikamente.",
        funFact = "Der Name 'Cytochrom P450' stammt daher, dass das Haem-Eisen im reduzierten Zustand bei der Wellenlaenge 450 Nanometer ein charakteristisches Absorptionsmaximum zeigt -- ein Zufall, der zum Namen fuer die gesamte Superfamilie wurde."
    ),

    // Question 10
    Question(
        categoryId = 16,
        questionText = "Was bedeutet es, wenn ein Medikament ein 'CYP3A4-Inhibitor' ist?",
        answerA = "Es beschleunigt den Abbau anderer Medikamente, die CYP3A4 nutzen",
        answerB = "Es verlangsamt den Abbau anderer Medikamente, die CYP3A4 nutzen, und kann deren Spiegel gefaehrlich erhoehen",
        answerC = "Es blockiert die Aufnahme von Medikamenten im Duenndarm",
        answerD = "Es verringert die Eiweissbindung von Medikamenten im Blut",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "CYP3A4-Inhibitoren (z.B. Clarithromycin, Grapefruitsaft) blockieren das Enzym, das viele Arzneimittel abbaut. Wenn ein CYP3A4-Substrat gleichzeitig eingenommen wird, akkumuliert es im Blut und kann toxische Spiegel erreichen -- eine haeufige Ursache fuer gefaehrliche Wechselwirkungen.",
        funFact = "Grapefruitsaft enthaelt Furanocumarine, die CYP3A4 im Darm irreversibel hemmen. Schon ein einzelnes Glas kann die Bioverfuegbarkeit einiger Statine oder Immunsuppressiva um das 10-fache steigern."
    ),

    // Question 11
    Question(
        categoryId = 16,
        questionText = "Was beschreibt die Halbwertszeit (t1/2) eines Medikaments?",
        answerA = "Die Zeit, nach der das Medikament seinen maximalen Plasmaspiegel erreicht",
        answerB = "Die Zeit, die das Koerper benoetigt, um 50% der absorbierten Dosis auszuscheiden",
        answerC = "Die Zeit, bis das Medikament vollstaendig aus dem Koerper eliminiert ist",
        answerD = "Die Zeitspanne, in der das Medikament wirksam ist",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die Halbwertszeit gibt an, in welcher Zeit die Plasmakonzentration eines Medikaments auf die Haelfte sinkt. Nach 4-5 Halbwertszeiten ist eine Substanz zu etwa 94-97% eliminiert. Die Halbwertszeit bestimmt das Dosierungsintervall: kurze t1/2 erfordert haeufige Gaben.",
        funFact = "Warfarin hat eine Halbwertszeit von 20-60 Stunden -- so eine grosse Schwankung, weil CYP2C9-Polymorphismen die Abbaurate individuell stark beeinflussen. Dasselbe gilt fuer die Dosisfindung, die deshalb sehr individuell sein muss."
    ),

    // Question 12
    Question(
        categoryId = 16,
        questionText = "Was ist der First-Pass-Effekt bei oral eingenommenen Medikamenten?",
        answerA = "Die Verzoegerung der Resorption durch Mageninhalt",
        answerB = "Der praesystemische Metabolismus in Darmwand und Leber, der die Bioverfuegbarkeit reduziert",
        answerC = "Die Ausscheidung des Wirkstoffs ueber die Nieren vor Erreichen des Zielorgans",
        answerD = "Die Bindung des Wirkstoffs an Plasmaproteine im Blut",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Nach oraler Resorption gelangt der Wirkstoff ueber die Pfortader direkt zur Leber. Dort wird ein Teil des Wirkstoffs bereits vor dem Erreichen des systemischen Kreislaufs metabolisiert. Bei starkem First-Pass-Effekt (z.B. Nitroglycerin, Morphin) ist die orale Bioverfuegbarkeit deutlich geringer als bei intravenoeser Gabe.",
        funFact = "Nitroglycerin hat einen so starken First-Pass-Effekt, dass von einer oral eingenommenen Dosis kaum etwas im Blut ankommt. Deshalb wird es als Spray unter die Zunge gegeben -- die Mundschleimhaut umgeht die Leber direkt."
    ),

    // Question 13
    Question(
        categoryId = 16,
        questionText = "Welche Rezeptoren sind primaer fuer die schmerzstillende Wirkung von Opioiden verantwortlich?",
        answerA = "GABA-A-Rezeptoren im Rueckenmark",
        answerB = "Mu-Opioid-Rezeptoren (MOR) im ZNS und Rueckenmark",
        answerC = "NMDA-Rezeptoren im Hippocampus",
        answerD = "Serotonin-5-HT3-Rezeptoren im Hirnstamm",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Mu-Opioid-Rezeptoren (MOP/MOR) vermitteln Analgesie, Euphorie, Atemdepression und Suchtpotenzial. Sie sind G-Protein-gekoppelte Rezeptoren (Gi/Go), die bei Aktivierung die Adenylylzyklase hemmen, Kaliumkanaele oeffnen und Calciumkanaele schliessen -- das Neuron wird hyperpolarisiert.",
        funFact = "Das koerpereigene Endorphin-System nutzt dieselben Rezeptoren. 'Runner's High', das Hochgefuehl nach langen Laeufen, basiert auf der Ausschuettung von beta-Endorphin, das an exakt diesen Mu-Rezeptoren wirkt."
    ),

    // Question 14
    Question(
        categoryId = 16,
        questionText = "Was beschreibt die Gate-Control-Theorie des Schmerzes nach Melzack und Wall?",
        answerA = "Schmerz entsteht nur, wenn ein Schwellenwert von Schmerzrezeptoren ueberschritten wird",
        answerB = "Signale aus nicht-schmerzleitenden Fasern koennen im Hinterhorn des Rueckenmarks Schmerzsignale hemmen -- ein 'Tor' oeffnet oder schliesst sich",
        answerC = "Schmerz wird ausschliesslich im somatosensorischen Kortex verarbeitet und kann dort geblockt werden",
        answerD = "Das Immunsystem reguliert die Schmerzleitung durch Freisetzung von Zytokinen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Melzack und Wall postulierten 1965, dass Interneurone im Hinterhorn als 'Tor' fungieren: Aktivitaet in dicken, schnellen Ab-Fasern (Beruehrung) hemmt Schmerzimpulse aus duennen C-Fasern. Das erklaert, warum Reiben auf einer Wunde den Schmerz lindert und bildet die Grundlage der TENS-Therapie.",
        funFact = "Diese Theorie war so revolutionaer, dass sie das gesamte Schmerzverstaendnis umkehrte: Schmerz ist kein passives Meldesystem, sondern ein aktiv regulierter Prozess -- was auch erklaert, warum Stress und Emotion den Schmerz veraendern."
    ),

    // Question 15
    Question(
        categoryId = 16,
        questionText = "Wie wirken nichtsteroidale Antirheumatika (NSAIDs) wie Ibuprofen?",
        answerA = "Sie blockieren Opioid-Rezeptoren und verstaerken endogene Schmerzmodulation",
        answerB = "Sie hemmen die Cyclooxygenase (COX-1 und COX-2) und reduzieren die Prostaglandinsynthese",
        answerC = "Sie blockieren Natriumkanaele und unterbrechen die Schmerzleitung",
        answerD = "Sie hemmen die Freisetzung von Substanz P aus Schmerzfasern",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "NSAIDs hemmen COX-Enzyme, die Arachidonsaeure in Prostaglandine und Thromboxane umwandeln. Prostaglandine sensibilisieren Nozizeptoren, verursachen Fieber und foerdern Entzuendung. COX-1-Hemmung erklaert die Magenirritationen (Schleimhautschutz reduziert), COX-2-Hemmung die entzuendungshemmende Wirkung.",
        funFact = "Aspirin (Acetylsalicylsaeure) ist das einzige NSAID, das COX irreversibel acetyliert. Alle anderen NSAIDs binden reversibel. Deshalb ist Aspirin so wirksam zur dauerhaften Thrombozytenhemmung -- Blutplaettchen koennen kein neues COX-1 produzieren."
    ),

    // Question 16
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen selektiven COX-2-Hemmern (Coxiben) und klassischen NSAIDs?",
        answerA = "Coxibe hemmen ausschliesslich COX-2 und haben ein geringeres gastrointestinales Risiko, aber erhoeHtes kardiovaskulaeres Risiko",
        answerB = "Coxibe wirken staerker analgetisch als klassische NSAIDs",
        answerC = "Coxibe hemmen auch COX-1 und schuerzen die Magenschleimhaut besser",
        answerD = "Coxibe haben kein Risiko fuer Herzinfarkt oder Schlaganfall",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "COX-2-Inhibitoren (Celecoxib, Etoricoxib) schonen die COX-1-abhaengige Magenschleimhaut und hemmen weniger die Thrombozytenaggregation. Allerdings wird durch COX-2-Hemmung das Gleichgewicht zwischen Prostazyklin (gefaesserweitend) und Thromboxan verschoben -- was das kardiovaskulaere Risiko erhoehen kann.",
        funFact = "Rofecoxib (Vioxx) wurde 2004 weltweit vom Markt genommen, nachdem Studien ein deutlich erhoehtes Herzinfarktrisiko zeigten. Es war eines der groessten Ruecknahme-Ereignisse in der Pharmakologiegeschichte."
    ),

    // Question 17
    Question(
        categoryId = 16,
        questionText = "Was beschreibt die Bioverfuegbarkeit eines Medikaments?",
        answerA = "Die maximale Konzentration, die ein Medikament im Blut erreicht",
        answerB = "Der Anteil der verabreichten Dosis, der unveraendert den systemischen Kreislauf erreicht",
        answerC = "Die Geschwindigkeit, mit der ein Medikament aus dem Koerper ausgeschieden wird",
        answerD = "Die Faehigkeit eines Medikaments, die Blut-Hirn-Schranke zu ueberwinden",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die Bioverfuegbarkeit (F) ist der Prozentsatz der verabreichten Dosis, der unveraendert in den Blutkreislauf gelangt. Intravenoes gegeben gilt F=100%. Bei oraler Einnahme reduzieren First-Pass-Effekt, schlechte Resorption und Abbau im Darm die Bioverfuegbarkeit oft erheblich.",
        funFact = "Morphin hat nach oraler Einnahme eine Bioverfuegbarkeit von nur etwa 20-30%, intravenoes dagegen 100%. Deshalb sind orale Morphin-Dosen deutlich hoeher als intravenoes -- der Rechenfehler hier war historisch eine haeufige Ursache gefaehrlicher Ueber- oder Unterdosierung."
    ),

    // Question 18
    Question(
        categoryId = 16,
        questionText = "Wie wirken Statine im Lipidstoffwechsel?",
        answerA = "Sie hemmen die Gallensaeurenrueckresorption im Darm",
        answerB = "Sie aktivieren den LDL-Rezeptor auf Leberzellen und erhoehen den LDL-Abbau",
        answerC = "Sie hemmen das Enzym HMG-CoA-Reduktase und reduzieren die Cholesterinsynthese in der Leber",
        answerD = "Sie blockieren den Cholesterintransport durch Chylomikronen",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Statine (Atorvastatin, Simvastatin) hemmen kompetitiv HMG-CoA-Reduktase, das Schluesselenzym der hepatischen Cholesterinsynthese. Weniger intrazellulaeres Cholesterin fuehrt zur Hochregulation von LDL-Rezeptoren, die LDL aus dem Blut aufnehmen -- der LDL-Spiegel sinkt.",
        funFact = "Das erste Statin -- Lovastatin -- wurde aus dem Schimmelpilz Aspergillus terreus isoliert. Natur-Pilze produzieren HMG-CoA-Reduktase-Hemmer, um wahrscheinlich konkurrierende Mikroorganismen zu behindern."
    ),

    // Question 19
    Question(
        categoryId = 16,
        questionText = "Was ist das primaere Ziel von Metformin bei der Behandlung von Typ-2-Diabetes?",
        answerA = "Es stimuliert die Insulinausschuettung aus Beta-Zellen der Bauchspeicheldruse",
        answerB = "Es hemmt den hepatischen Glukoseausstoss und verbessert die Insulinsensitivitaet",
        answerC = "Es blockiert die Glukoseabsorption im Duenndarm vollstaendig",
        answerD = "Es erhoecht die renale Glukoseausscheidung ueber SGLT2-Blockade",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Metformin aktiviert AMP-aktivierte Proteinkinase (AMPK) und hemmt den Komplex I der mitochondrialen Atmungskette in Leberzellen. Dadurch sinkt die hepatische Gluconeogenese, und gleichzeitig wird die Insulinsensitivitaet in Muskeln und Fettgewebe verbessert.",
        funFact = "Metformin ist seit ueber 60 Jahren auf dem Markt und gilt als sicherstes orales Antidiabetikum. Neuere Forschung zeigt moegliche Anti-Aging-Eigenschaften -- Metformin wird in der Alternsforschung als Kandidat fuer klinische Studien zur Lebenszeitverlaengerung untersucht."
    ),

    // Question 20
    Question(
        categoryId = 16,
        questionText = "Welche Nebenwirkung von ACE-Hemmern wie Ramipril ist so charakteristisch, dass sie zum Absetzen fuehrt?",
        answerA = "Erythrozytenabfall (Anhaemie)",
        answerB = "Trockener, anhaltender Reizhusten",
        answerC = "Erhoehte Blutzuckerwerte (Hyperglykamie)",
        answerD = "Muskelschwaerze (Rhabdomyolyse)",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "ACE-Hemmer blockieren das Enzym, das Angiotensin I in II umwandelt -- und gleichzeitig Bradykinin abbaut. Akkumuliertes Bradykinin reizt Atemwegsrezeptoren und verursacht bei 10-15% der Patienten trockenen Husten. Sartane (AT1-Blocker) haben diese Nebenwirkung nicht.",
        funFact = "Wegen des Reizhustens wechseln viele Patienten von ACE-Hemmern zu Sartanen. In Asien ist der Husten bei bis zu 40% der Patienten -- Asiaten haben genetische Varianten, die Bradykinin langsamer abbauen."
    ),

    // Question 21
    Question(
        categoryId = 16,
        questionText = "Wie wirken Protonenpumpenhemmer (PPIs) wie Omeprazol?",
        answerA = "Sie neutralisieren Magensaeure durch alkalische Puffer",
        answerB = "Sie blockieren H2-Rezeptoren auf Belegzellen und reduzieren die Histamin-stimulierte Saeure",
        answerC = "Sie hemmen irreversibel die H+/K+-ATPase (Protonenpumpe) der Magenbe legzellen",
        answerD = "Sie foerdern die Schleimproduktion und schaetzen die Magenschleimhaut",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "PPIs werden als Prodrugs resorbiert und im sauren Milieu der Sekretkanaelichen aktiviert. Das aktive Sulfenamid bindet kovalent und irreversibel an Cysteinreste der H+/K+-ATPase. Da neue Pumpen produziert werden muessen, haelt die Wirkung laenger als die Halbwertszeit des Wirkstoffs.",
        funFact = "PPIs werden extrem haeufig verschrieben -- oft laenger als noetig. Eine im Lancet erschienene Studie zeigte, dass Langzeitanwender ein erhoehtes Risiko fuer Magenkrebs und Nierenerkrankungen haben koennen, weshalb regelmaessige Ueberpruefung der Indikation empfohlen wird."
    ),

    // Question 22
    Question(
        categoryId = 16,
        questionText = "Was ist Pharmakogenetik und welches Beispiel ist klinisch besonders relevant?",
        answerA = "Die Untersuchung wie Drogenmissbrauch Gene veraendert",
        answerB = "Die Lehre vom Einfluss genetischer Varianten auf die individuelle Arzneimittelwirkung -- z.B. CYP2D6-Polymorphismen bei Codein",
        answerC = "Die genetische Veraenderung von Pflanzen zur Herstellung von Medikamenten",
        answerD = "Die Erforschung von Genmutationen, die durch Medikamente verursacht werden",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Pharmakogenetik untersucht, wie Genvarianten Arzneimittelwirkung und -abbau beeinflussen. CYP2D6-Langsam-Metabolisierer akkumulieren Codein kaum zu Morphin (kein Schmerzmitteleffekt), waehrend Ultraschnell-Metabolisierer gefaehrlich hohe Morphinspiegel entwickeln.",
        funFact = "In manchen Regionen Afrikas und des Nahen Ostens sind bis zu 29% der Bevoelkerung CYP2D6-Ultraschnell-Metabolisierer. Standarddosen von Codein koennen dort bei stillenden Muettern zu Morphinvergiftungen beim Saeugting fuehren -- ein lebensbedrohliches Problem, das lange unerkannt war."
    ),

    // Question 23
    Question(
        categoryId = 16,
        questionText = "Welche Substanzklasse wird bei Transplantationen eingesetzt, um Abstossungsreaktionen zu verhindern?",
        answerA = "Kortikosteroide in Kombination mit Calcineurin-Inhibitoren wie Ciclosporin",
        answerB = "Beta-Blocker zur Unterdrueckung der Immunantwort",
        answerC = "Antibiotika, um Infektionen zu verhindern die das Immunsystem triggern",
        answerD = "Antihistaminika zur Kontrolle der allergischen Reaktion",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Calcineurin-Inhibitoren (Ciclosporin, Tacrolimus) hemmen die T-Zell-Aktivierung, indem sie die Phosphatase Calcineurin blockieren -- dadurch kann NFAT nicht in den Kern wandern und IL-2 nicht transkribiert werden. Ohne IL-2 keine T-Zell-Proliferation, keine Abstossungsreaktion.",
        funFact = "Ciclosporin wurde aus einem norwegischen Bodenpilz (Tolypocladium inflatum) isoliert. Sein Entdecker Jean Francois Borel bemerkte 1970, dass der Pilz das Wachstum von T-Lymphozyten hemmte -- ein Zufallsfund, der die Transplantationsmedizin revolutionierte."
    ),

    // Question 24
    Question(
        categoryId = 16,
        questionText = "Wie wirken Biologika wie TNF-alpha-Inhibitoren bei rheumatoider Arthritis?",
        answerA = "Sie ersetzen fehlende Proteine durch rekombinante Kopien",
        answerB = "Sie neutralisieren TNF-alpha, ein Zytokin das Entzuendung und Gelenkzerstoerung foerdert",
        answerC = "Sie stimulieren regulatorische T-Zellen und daempfen die Autoimmunreaktion unspezifisch",
        answerD = "Sie blockieren B-Zellen und verhindern die Produktion von Rheumafaktoren",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "TNF-alpha-Inhibitoren (Adalimumab, Infliximab, Etanercept) sind monoklonale Antikoerper oder Fusionsproteine, die TNF-alpha binden und neutralisieren. TNF-alpha ist ein Schluesselmediator der Synovialitits bei RA -- seine Hemmung unterbricht die Entzuendungskaskade effektiv.",
        funFact = "Adalimumab (Humira) war jahrelang das umsatzstaerkste Medikament der Welt -- mit einem jahreserloes von bis zu 20 Milliarden Dollar. Sein Patentablauf fuehrte zu einem 'Biologics-Boom' mit zahlreichen Biosimilars."
    ),

    // Question 25
    Question(
        categoryId = 16,
        questionText = "Was ist der Wirkmechanismus von Methotrexat bei Krebs und Autoimmunerkrankungen?",
        answerA = "Es hemmt die Dihydrofolat-Reduktase und stoert die Synthese von Purinen und Pyrimidinen",
        answerB = "Es alkyliert die DNA und verhindert die Replikation",
        answerC = "Es hemmt Topoisomerase II und verursacht DNA-Strangbrueche",
        answerD = "Es bindet an Tubulin und blockiert die Spindelbildung",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Methotrexat ist ein Folsaeureantagonist: Es hemmt kompetitiv DHFR (Dihydrofolat-Reduktase) und dadurch die Regeneration von Tetrahydrofolat. Ohne aktives Folat koennen weder Thymidylat noch Purine synthetisiert werden -- proliferierende Zellen (Tumorzellen, aktivierte Lymphozyten) sterben ab.",
        funFact = "Methotrexat wird bei rheumatoider Arthritis in sehr viel niedrigeren Dosen als in der Onkologie eingesetzt. Im niedrigen Dosisbereich wirkt es ueber zusaetzliche Mechanismen (Adenosin-Freisetzung), die ebenfalls entzuendungshemmend wirken."
    ),

    // Question 26
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet alkylierende Zytostatika von antimetabolischen Zytostatika?",
        answerA = "Alkylierende Mittel binden kovalent an DNA; Antimetabolite imitieren natuerliche Metabolite und hemmen Biosynthesewege",
        answerB = "Alkylierende Mittel wirken nur in der S-Phase; Antimetabolite wirken in allen Zellzyklusphasen",
        answerC = "Alkylierende Mittel hemmen Topoisomerasen; Antimetabolite blockieren die Mitose",
        answerD = "Alkylierende Mittel haben keine Nebenwirkungen; Antimetabolite sind zellzyklusspezifisch",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Alkylierende Mittel (Cyclophosphamid, Cisplatin) fuegen kovalente Alkylgruppen an DNA-Basen an, bilden intrastrand- oder interstrand-Crosslinks und verhindern die Replikation. Antimetabolite (5-Fluorouracil, Gemcitabin) aehneln natuerlichen Bausteinen und werden in DNA/RNA eingebaut oder hemmen Enzyme der Nucleotidsynthese.",
        funFact = "Stickstoiflosit -- das erste alkylierende Zytostatikum -- wurde aus dem Kampfstoff Stickstofflost entwickelt, der im Ersten Weltkrieg eingesetzt wurde. Aerzte bemerkten, dass Soldaten mit Lost-Exposition oft Lymphomremissionen zeigten."
    ),

    // Question 27
    Question(
        categoryId = 16,
        questionText = "Was ist das Ziel von Taxanen wie Paclitaxel in der Chemotherapie?",
        answerA = "Sie hemmen Topoisomerase I und verursachen DNA-Einzelstrangbrueche",
        answerB = "Sie stabilisieren Mikrotubuli und verhindern deren Abbau -- die Zellteilung kommt zum Stillstand",
        answerC = "Sie binden an den EGF-Rezeptor und hemmen Proliferationssignale",
        answerD = "Sie hemmen das Proteasom und loesen Apoptose aus",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Paclitaxel und Docetaxel binden an beta-Tubulin und verhindern die Depolymerisation der Mikrotubuli. Der Spindelapparat besteht aus dynamisch wachsenden und schrumpfenden Mikrotubuli -- bei stabilen Mikrotubuli koennen sich Chromosomen nicht trennen, und die Mitose stoppt in der M-Phase.",
        funFact = "Paclitaxel wurde urspruenglich aus der Rinde der Pazifischen Eibe (Taxus brevifolia) gewonnen. Fuer 1 kg Wirkstoff benoetigte man frueher 6 Tonnen Baumrinde -- Synthese und Halbsynthese haben diesen Problem heute geloest."
    ),

    // Question 28
    Question(
        categoryId = 16,
        questionText = "Was sind Checkpoint-Inhibitoren in der Krebsimmuntherapie?",
        answerA = "Medikamente, die den Zellzyklus im G2-Kontrollpunkt stoppen",
        answerB = "Antikoerper, die inhibitorische Immunsignale (PD-1, CTLA-4) blockieren und T-Zellen reaktivieren",
        answerC = "Enzyme, die DNA-Schaeden in Tumorzellen reparieren",
        answerD = "Substanzen, die Tumorblutgefaesse zerstoeren (anti-angiogenetisch)",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Tumorzellen exprimieren PD-L1, das an PD-1 auf T-Zellen bindet und diese 'ausschaltet'. PD-1-Inhibitoren (Pembrolizumab, Nivolumab) und CTLA-4-Inhibitoren (Ipilimumab) heben diese Bremse auf -- T-Zellen koennen Tumorzellen wieder erkennen und angreifen.",
        funFact = "James Allison und Tasuku Honjo erhielten 2018 den Nobelpreis fuer Medizin fuer die Entdeckung der CTLA-4- und PD-1-Checkpoint-Inhibition. Ihre Arbeit hat bei manchen Melanompatienten zu dauerhaften Heilungen gefuehrt -- etwas, das bei metastasierten Melanomen zuvor fast unbekannt war."
    ),

    // Question 29
    Question(
        categoryId = 16,
        questionText = "Wie wirken selektive Serotonin-Wiederaufnahmehemmer (SSRIs) bei Depressionen?",
        answerA = "Sie hemmen den SERT-Transporter und erhoehen die synaptische Serotoninkonzentration",
        answerB = "Sie aktivieren direkt Serotonin-5-HT1A-Rezeptoren im limbischen System",
        answerC = "Sie hemmen den Abbau von Serotonin durch Monoaminoxidase",
        answerD = "Sie foerdern die Serotoninsynthese aus Tryptophan",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "SSRIs (Fluoxetin, Sertralin, Escitalopram) blockieren selektiv den Serotonin-Transporter (SERT), der Serotonin aus dem synaptischen Spalt zurueck in das praesynaptische Neuron pumpt. Durch diese Hemmung akkumuliert Serotonin im Spalt und kann laenger an postsynaptische Rezeptoren binden.",
        funFact = "SSRIs wirken erst nach 2-4 Wochen antidepressiv, obwohl der SERT nach der ersten Dosis bereits blockiert ist. Erklaerungsansaetze beinhalten die verzogerte Downregulation von Autorezeptoren und die Neuroplastizitaet (BDNF-Hochregulation) -- der genaue Mechanismus ist noch nicht vollstaendig verstanden."
    ),

    // Question 30
    Question(
        categoryId = 16,
        questionText = "Was ist der Wirkmechanismus von Benzodiazepinen wie Diazepam?",
        answerA = "Sie erhoehen die GABA-A-Rezeptor-Chloridkanaloffenungsfrequenz durch allosterische Bindung",
        answerB = "Sie hemmen NMDA-Glutamat-Rezeptoren und verringern exzitatorische Signale",
        answerC = "Sie blockieren Adenosinrezeptoren und foerdern Schlaf",
        answerD = "Sie aktivieren GABA-B-Rezeptoren und vermindern die Transmitterfreisetzung",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Benzodiazepine binden an eine allosterische Bindungsstelle zwischen der alpha- und gamma-Untereinheit des GABA-A-Rezeptors. Sie veraendern nicht direkt die Leitfaehigkeit, sondern erhoehen die Frequenz der Kanaloffnung in Gegenwart von GABA -- mehr Chlorid einwaerts, mehr Hyperpolarisation.",
        funFact = "Das Antidot Flumazenil kehrt die Benzodiazepinwirkung innerhalb von Minuten um -- es verdraengt das Benzodiazepin kompetitiv von der Bindungsstelle. Da Flumazenil kurzer wirkt als die meisten Benzodiazepine, muss es bei Overdosis wiederholend gegeben werden."
    ),

    // Question 31
    Question(
        categoryId = 16,
        questionText = "Was ist eine therapeutische Breite und warum ist sie klinisch wichtig?",
        answerA = "Der Zeitraum, in dem ein Medikament am effektivsten ist",
        answerB = "Der Abstand zwischen minimaler therapeutischer Konzentration und toxischer Konzentration",
        answerC = "Die Palette an Krankheiten, fuer die ein Medikament zugelassen ist",
        answerD = "Die Bandbreite von Dosierungsintervallen, die klinisch akzeptabel sind",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die therapeutische Breite (therapeutisches Fenster) ist der Konzentrationsbereich zwischen minimaler wirksamer Konzentration (MEC) und minimaler toxischer Konzentration (MTC). Bei schmaler therapeutischer Breite (Digoxin, Lithium, Theophyllin) ist Therapeutisches Drug Monitoring essentiell, da kleine Dosisaenderungen toxisch sein koennen.",
        funFact = "Lithium hat eine der schmalsten therapeutischen Breiten aller Medikamente: therapeutisch bei 0.6-1.2 mmol/L, toxisch ab 1.5 mmol/L. Ein einfacher Salzabfall (Dehydration, schweisstreiben) kann den Lithiumspiegel in den toxischen Bereich heben -- deshalb muessen Patienten auf ausreichend Fluessigkeit achten."
    ),

    // Question 32
    Question(
        categoryId = 16,
        questionText = "Wie wirken Antivirale wie Oseltamivir (Tamiflu) gegen Influenza?",
        answerA = "Sie hemmen die virale RNA-Polymerase und unterbinden die Genomreplikation",
        answerB = "Sie blockieren das virale Haemagglutinin und verhindern die Zelladhaesion",
        answerC = "Sie hemmen die virale Neuraminidase und verhindern die Freisetzung neuer Viren",
        answerD = "Sie aktivieren das Interferon-System und foerdern die angeborene Immunantwort",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Neuraminidase spaltet Sialinsaeure und ermoeglicht so, dass neue Virionen die infizierte Zelle verlassen und sich weiterverbreiten. Oseltamivir hemmt kompetitiv die Neuraminidase -- neue Viren bleiben an der Zelloberflaece gebunden und koennen keine weiteren Zellen infizieren.",
        funFact = "Oseltamivir muss innerhalb der ersten 48 Stunden nach Symptombeginn eingenommen werden, um wirksam zu sein -- ein Fenster, das viele Patienten verpassen. Deshalb ist seine klinische Wirksamkeit bei spaeterer Einnahme umstritten."
    ),

    // Question 33
    Question(
        categoryId = 16,
        questionText = "Was ist der Wirkmechanismus von Nukleosid-Reverse-Transkriptase-Inhibitoren (NRTIs) wie Tenofovir bei HIV?",
        answerA = "Sie blockieren den HIV-Integrase und verhindern die Einschleusung viraler DNA ins Wirtszellgenom",
        answerB = "Sie werden als falsche Bausteine eingebaut und beenden die DNA-Kettensynthese (Kettenabbruch)",
        answerC = "Sie hemmen die HIV-Protease und verhindern die Reifung neuer Virionen",
        answerD = "Sie blockieren CCR5/CXCR4-Korezeptoren und verhindern den Zelleintritt",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "NRTIs sind Prodrugs, die intrazellulaaer triphosphoryliert werden. Als falsche Desoxyribonukleoside werden sie von der HIV-Reversen Transkriptase eingebaut -- aber da ihnen die 3'-OH-Gruppe fehlt, kann kein weiteres Nukleotid angefuegt werden. Die DNA-Synthese bricht ab.",
        funFact = "AZT (Zidovudin), das erste HIV-Medikament (1987), war urspruenglich als Krebsmittel entwickelt worden und abgelehnt worden -- bis jemand es gegen HIV testete. Die klinischen Studien wurden 1986 vorzeitig abgebrochen, weil der Unterschied zur Placebo-Gruppe so dramatisch war."
    ),

    // Question 34
    Question(
        categoryId = 16,
        questionText = "Was ist Tachyphylaxie in der Pharmakologie?",
        answerA = "Eine allergische Reaktion bei wiederholter Medikamentengabe",
        answerB = "Die schnelle Entwicklung von Toleranz bei wiederholter oder kontinuierlicher Medikamentengabe",
        answerC = "Die verzoegerte Wirkung eines Medikaments bei ersten Dosen",
        answerD = "Das paradoxe Ansprechen auf ein Medikament bei hohen Dosen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Tachyphylaxie bezeichnet den raschen Wirkungsverlust nach wiederholter Applikation. Beim Nasendekongestionsmittel Xylometazolin tritt nach wenigen Tagen ein 'Rebound-Effekt' auf -- alpha-Adrenozeptoren sind desensibilisiert und internalisiert, die Nase wird noch mehr verstopft als zuvor.",
        funFact = "Tachyphylaxie bei Nitroglycerinpflastern ist so gut dokumentiert, dass Patienten explizit instruiert werden, ein 8-12 Stunden 'nitratfreies Intervall' einzuhalten -- damit die Rezeptoren regenerieren und die Wirksamkeit erhalten bleibt."
    ),

    // Question 35
    Question(
        categoryId = 16,
        questionText = "Was ist das Konzept des 'Enteral-parenteralen' Unterschieds und wann ist parenterale Ernaehrung indiziert?",
        answerA = "Enteral bedeutet ueber die Mundhoehle; parenteral ueber Infusion; Infusion wird bei Zahnproblemen bevorzugt",
        answerB = "Enteral bedeutet ueber den Verdauungstrakt; parenteral bedeutet unter Umgehung des Darms -- indiziert bei schwerem Malabsorptionssyndrom oder postoperativer Darmlaehmung",
        answerC = "Parenteral ist immer vorzuziehen, weil die Naehrstoffe schneller verfuegbar sind",
        answerD = "Enteral und parenteral sind gleichwertig -- die Wahl haengt ausschliesslich von Patientenpraeferenzen ab",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Enterale Ernaehrung (oral oder per Sonde) haelt die Darmbarriere intakt, foerdert intestinale Immunitaet und ist physiologischer. Parenterale Ernaehrung (intravenoes) umgeht den Darm vollstaendig und ist indiziert bei schwerem Kurzdarmsyndrom, aktiver Fistel, Ileus oder hochgradiger Mukositis.",
        funFact = "Der Darm gilt als 'Motor der Immunabwehr' -- etwa 70% der Immunzellen des Koerpers befinden sich im Darm-assoziierten Lymphgewebe (GALT). Laengere parenterale Ernaehrung kann zu Darmschleimhautatrophie und Bakteriell-translokation fuehren."
    ),

    // Question 36
    Question(
        categoryId = 16,
        questionText = "Wie wirken Antifungizide wie Amphotericin B gegen Pilzinfektionen?",
        answerA = "Es hemmt die pilzliche Ergosterolsynthese durch Blockade der Lanosterin-14-alpha-Demethylase",
        answerB = "Es bindet an Ergosterol in der Pilzmembran und bildet Poren, die zum Zelltod fuehren",
        answerC = "Es hemmt die Chitin-Synthase und stoert den Aufbau der Pilzzellwand",
        answerD = "Es blockiert nukleaere Pilzenzyme und unterbindet die DNA-Replikation",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Amphotericin B ist ein polyenes Antimykotikum, das mit hoher Affinitaet an Ergosterol -- das pilzspezifische Membransterol -- bindet. Es formt ringfoermige Poren in der Pilzmembran, durch die Kalium und Protonen austreten, was zum osmotischen Zelltod fuehrt.",
        funFact = "Amphotericin B ist eines der toxischsten klinisch eingesetzten Medikamente -- Fieber, Schtttelfrost, Nierenversagen und Hypokaliaemie sind bekannte Nebenwirkungen, weshalb es unter Aufsicht als 'Ampho-Terrible' bekannt ist. Liposomale Formulierungen haben die Vertraeglichkeit erheblich verbessert."
    ),

    // Question 37
    Question(
        categoryId = 16,
        questionText = "Welche Substanzklasse hemmt die pilzliche Zellwandsynthese durch Blockade der Glucan-Synthase?",
        answerA = "Triazole (Fluconazol, Voriconazol)",
        answerB = "Polyene (Amphotericin B)",
        answerC = "Echinocandine (Caspofungin, Micafungin)",
        answerD = "Azole (Ketoconazol)",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Echinocandine hemmen selektiv die 1,3-beta-D-Glucan-Synthase, die Glucan-Polymere fuer die pilzliche Zellwand synthetisiert. Da Saeugetierzellen kein Glucan in ihrer Membran haben, sind Echinocandine sehr gut vertraeglich -- das Ziel ist pilzspezifisch.",
        funFact = "Caspofungin war das erste Echinocandin, das 2001 zugelassen wurde. Es gilt heute als Mittel der Wahl bei invasiver Candidiasis -- besonders bei Patienten, die durch Azol-Resistenz versagt haben."
    ),

    // Question 38
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einem Agonisten und einem kompetitiven Antagonisten?",
        answerA = "Ein Agonist hemmt die Rezeptoraktivitaet; ein Antagonist aktiviert sie",
        answerB = "Ein Agonist aktiviert den Rezeptor und loest eine Wirkung aus; ein kompetitiver Antagonist bindet am gleichen Ort ohne Aktivierung und verdraengt den Agonisten reversibel",
        answerC = "Ein Agonist bindet nur an Rezeptoren im Ruhezustand; ein Antagonist nur an aktivierte Rezeptoren",
        answerD = "Ein Agonist wirkt nur systemisch; ein Antagonist nur lokal",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Agonisten binden an den Rezeptor und induzieren eine Konformationsaenderung, die eine Signalantwort ausloest (intrinsische Aktivitaet = 1). Kompetitive Antagonisten binden reversibel an die gleiche Bindungsstelle ohne Aktivierung (intrinsische Aktivitaet = 0) und koennen durch genug Agonist verdraengt werden -- die Konzentrations-Wirkungs-Kurve verschiebt sich nach rechts.",
        funFact = "Naloxon -- das Antidot bei Opioidvergiftung -- ist ein kompetitiver Antagonist an Mu-Opioid-Rezeptoren. Es hat so hohe Affinitaet, dass es Morphin oder Heroin vom Rezeptor verdraengt und Atemdepression innerhalb von Sekunden umkehrt."
    ),

    // Question 39
    Question(
        categoryId = 16,
        questionText = "Welches Enzym wird durch Aspirin irreversibel acetyliert und warum ist das fuer Thrombozytenhemmung relevant?",
        answerA = "Thrombin wird acetyliert und kann kein Fibrin mehr bilden",
        answerB = "COX-1 wird acetyliert; Thrombozyten koennen kein neues COX-1 synthetisieren und bleiben fuer ihre gesamte Lebensdauer (7-10 Tage) gehemmt",
        answerC = "ADP-Rezeptor P2Y12 wird acetyliert und Thrombozytenaggregation wird unterbunden",
        answerD = "Vitamin-K-Epoxid-Reduktase wird acetyliert und die Blutgerinnung unterbrochen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Aspirin acetyliert COX-1 irreversibel an Serin-530. Thrombozyten sind kernlose Fragmente und koennen kein neues COX-1 produzieren -- die Hemmung haelt fuer ihre gesamte Lebensdauer (7-10 Tage). Kein COX-1 bedeutet kein Thromboxan A2 -- weniger Thrombozytenaggregation und Vasokonstriktion.",
        funFact = "Schon 75-100 mg Aspirin taeglich genuegt zur vollstaendigen Thrombozytenhemmung -- hoehere Dosen bringen kaum mehr Thrombozytenwirkung, aber mehr Magennebenwirkungen. Deshalb wird Aspirin zur Herzinfarktpraevention in 'Low-Dose' gegeben."
    ),

    // Question 40
    Question(
        categoryId = 16,
        questionText = "Was ist Polypharmazie und welche besonderen Risiken birgt sie bei aelteren Menschen?",
        answerA = "Die Einnahme von mehr als 5 Medikamenten gleichzeitig -- erhoehtes Risiko fuer Wechselwirkungen, Nebenwirkungen und Sturz",
        answerB = "Die gleichzeitige Einnahme von Nahrungsergaenzungsmitteln und verschreibungspflichtigen Medikamenten",
        answerC = "Die Verschreibung von Medikamenten durch mehrere Aerzte ohne gegenseitige Kenntnis",
        answerD = "Die zu haeufige Einnahme eines einzelnen Medikaments",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Polypharmazie (typisch definiert als 5+ Medikamente) ist bei Aelteren besonders risikoreich, weil: Nierenfunktion nimmt mit Alter ab (langsamere Elimination), Verteilungsvolumen aendert sich, Hepatischer Metabolismus verlangsamt, und Arzneimittelwechselwirkungen exponentiell zunehmen. Bei 10 Medikamenten gibt es theoretisch 45 moegliche Zweifach-Interaktionen.",
        funFact = "Die Beers-Kriterien sind eine Liste von Medikamenten, die bei Aelteren besonders problematisch sind -- erstellt von Mark Beers 1991 und seitdem regelmaessig aktualisiert. Sie helfen Aerzten, potentiell ungeeignete Medikamente bei aelteren Patienten zu identifizieren."
    ),

    // Question 41
    Question(
        categoryId = 16,
        questionText = "Wie wirken Antihistaminika der 1. Generation anders als die der 2. Generation?",
        answerA = "1. Generation hemmt H1-Rezeptoren und ueberquert die Blut-Hirn-Schranke (muedemachend); 2. Generation ist hydrophiler, passiert kaum die BBB und macht weniger muede",
        answerB = "1. Generation hemmt H2-Rezeptoren; 2. Generation hemmt H1-Rezeptoren",
        answerC = "1. Generation wirkt nur bei allergischem Schnupfen; 2. Generation auch bei Asthma",
        answerD = "1. Generation ist staerker wirksam; 2. Generation wirkt langsamer aber laenger",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Antihistaminika der 1. Generation (Diphenhydramin, Clemastin) sind lipophil und passieren leicht die Blut-Hirn-Schranke, wo sie muskarinische und H1-Rezeptoren im ZNS hemmen (Sediierung, anticholinerge Effekte). Die 2. Generation (Cetirizin, Loratadin) ist zu polar, um die BBB effektiv zu passieren.",
        funFact = "Diphenhydramin (1. Gen.) ist der Wirkstoff in vielen nicht-verschreibungspflichtigen Schlafdmitteln wie Benadryl. Paradoxerweise koennen diese Mittel bei Aelteren statt Sediierung Erregung, Verwirrtheit und Delir ausloesen -- anticholinerge Effekte im Gehirn."
    ),

    // Question 42
    Question(
        categoryId = 16,
        questionText = "Was ist der Mechanismus hinter dem Serotoninsyndrom?",
        answerA = "Zu wenig Serotonin im ZNS durch Ueberdosierung von SERT-Inhibitoren",
        answerB = "Ueberstimulation von Serotonin-Rezeptoren durch Kombination serotonerger Substanzen -- gekennzeichnet durch Hyperthermie, Agitation und Myoklonus",
        answerC = "Eine allergische Reaktion auf Serotonin, die bei einigen Menschen auftritt",
        answerD = "Serotonindepletierung in Thrombozyten bei haeufiger SSRI-Einnahme",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Das Serotoninsyndrom entsteht durch exzessive serotonerge Aktivitaet, typisch durch Kombination von SSRI + MAO-Hemmer oder Tramadol + Sertralin. Klinisch: die Trias aus kognitiven Veraenderungen, autonomer Instabilitaet und neuromuskulaerer Abnormalitaet (Tremor, Klonus, Hyperthermie) -- lebensbedrohlich.",
        funFact = "Hunter-Entscheidungsregeln (2003) sind praeziser als die Sternbach-Kriterien zur Diagnose: Klonus (spontan, induziert oder okulaaer) in Verbindung mit serotonerger Medikation reicht zur Diagnose -- Klonus ist das pathognomischste Zeichen."
    ),

    // Question 43
    Question(
        categoryId = 16,
        questionText = "Wie wirken ACE-Hemmer im Renin-Angiotensin-Aldosteron-System (RAAS)?",
        answerA = "Sie hemmen Renin und verhindern die Bildung von Angiotensin I",
        answerB = "Sie hemmen ACE und verhindern die Umwandlung von Angiotensin I zu II -- weniger Vasokonstriktion und Aldosteronausschuettung",
        answerC = "Sie blockieren AT1-Rezeptoren und verhindern die Wirkung von Angiotensin II",
        answerD = "Sie hemmen Aldosteronrezeptoren und foerdern die Natriumausscheidung",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "ACE (Angiotensin-Converting-Enzyme) spaltet Angiotensin I zu Angiotensin II. ACE-Hemmer blockieren diesen Schritt: weniger Ang II bedeutet weniger Vasokonstriktion (RR sinkt), weniger Aldosteronausschuettung (weniger Na-Retention, Vorlast sinkt), weniger ADH. Ideal bei Herzinsuffizienz und Hypertonie.",
        funFact = "Das RAAS wurde erstmals 1898 von Tigerstedt und Bergman beschrieben, die einen nierenpressorischen Stoff -- Renin -- entdeckten. Genau 100 Jahre spaeter wurden ACE-Hemmer zum meistverkauften Antihypertensivum weltweit."
    ),

    // Question 44
    Question(
        categoryId = 16,
        questionText = "Was sind SGLT2-Inhibitoren und welchen Vorteil bieten sie bei Herzinsuffizienz?",
        answerA = "Sie hemmen den renalen Glucose-Natrium-Transporter, foerdern Glucosurie und haben kardioprotektive Effekte unabhaengig vom Blutzucker",
        answerB = "Sie stimulieren die Insulinausschuettung und senken den Blutzucker bei Typ-2-Diabetikern",
        answerC = "Sie hemmen die Aldosteronwirkung und wirken als Diuretikum bei Herzinsuffizienz",
        answerD = "Sie verbessern die myokardiale Kontraktilitaet durch Hemmung des If-Kanals",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "SGLT2-Inhibitoren (Empagliflozin, Dapagliflozin) hemmen den Natrium-Glucose-Cotransporter 2 im proximalen Tubulus -- Glucose und Natrium werden ausgeschieden. Klinische Studien zeigten ueberraschend starke kardioprotektive Effekte: reduzierte Hospitalisierung bei Herzinsuffizienz, verlangsamte Nierenschaeden, sogar bei Nicht-Diabetikern.",
        funFact = "Empagliflozin war das erste Diabetesmedikament, das in einer kardiovaskulaeren Outcome-Studie (EMPA-REG, 2015) eine signifikante Mortalitaetssenkung zeigte. Das war so unerwarts, dass manche Kardiologen zunaechst unglaeubbig waren -- heute sind SGLT2-Inhibitoren Standardtherapie bei Herzinsuffizienz."
    ),

    // Question 45
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen direkter und indirekter Antikoagulation -- Beispiel Heparin vs. Warfarin?",
        answerA = "Heparin (indirekt) aktiviert Antithrombin III; Warfarin (direkt) hemmt Thrombin und Faktor Xa",
        answerB = "Heparin (direkt) hemmt Thrombin; Warfarin hemmt die hepatische Vitamin-K-abhaengige Gerinnungsfaktorensynthese",
        answerC = "Heparin hemmt Vitamin-K-Epoxid-Reduktase; Warfarin aktiviert Antithrombin III",
        answerD = "Beide wirken gleich -- nur Heparin ist parenteral und Warfarin oral",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Heparin bindet an Antithrombin III und verstaerkt dessen hemmende Wirkung auf Thrombin und Faktor Xa um das 1000-fache -- sofortige Wirkung. Warfarin hemmt Vitamin-K-Epoxid-Reduktase und blockiert so die Carboxylierung (Aktivierung) der Faktoren II, VII, IX, X -- Wirkbeginn erst nach 2-3 Tagen (Abbau bereits gebildeter Faktoren).",
        funFact = "Warfarin wurde urspruenglich als Rattengift entwickelt. Der Name kommt von 'Wisconsin Alumni Research Foundation' (WARF) -- die Universitat Wisconsin isolierte den Wirkstoff aus schimmligem Sussklee, der bei Rindern Blutungsneigung verursachte."
    ),

    // Question 46
    Question(
        categoryId = 16,
        questionText = "Was ist das Konzept der 'Prodrug' und welches bekannte Beispiel gibt es?",
        answerA = "Ein inaktiver Vorlaeuferstoff, der im Koerper in den aktiven Wirkstoff umgewandelt wird -- z.B. Codein zu Morphin",
        answerB = "Ein Medikament, das nur bei professioneller Anwendung wirkt",
        answerC = "Ein Wirkstoff, der im Labor synthetisiert wird, im Gegensatz zu natuerlichen Substanzen",
        answerD = "Eine hohe Initialdosis die vor der regulaeren Dosierung gegeben wird",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Prodrugs sind pharmakologisch inaktiv und muessen erst durch Enzymen (meist Leber) in den aktiven Metaboliten umgewandelt werden. Vorteile: bessere Resorption, gezielter Gewebemetabolismus. Codein wird durch CYP2D6 zu Morphin aktiviert; Enalapril (Prodrug) wird zu Enalaprilat (aktiv); Clopidogrel zu aktivem Thiol-Metabolit.",
        funFact = "Etwa 10% aller zugelassenen Medikamente sind Prodrugs. Die Strategie wird gezielt eingesetzt, um Bioverfuegbarkeit zu verbessern oder Zielgewebe spezifisch anzusteuern -- z.B. Antibiotika, die nur im infizierten Gewebe aktiviert werden."
    ),

    // Question 47
    Question(
        categoryId = 16,
        questionText = "Was ist 'Antibiotika-Stewardship' und warum ist es klinisch wichtig?",
        answerA = "Das Programm zur Entwicklung neuer Antibiotika in der pharmazeutischen Industrie",
        answerB = "Ein strukturiertes Programm zur rationalen Antibiotikagabe -- richtige Substanz, Dosis, Dauer -- zur Reduzierung von Resistenzentwicklung und Nebenwirkungen",
        answerC = "Die internationale Koordination zwischen Laendern bei Pandemien durch resistente Keime",
        answerD = "Die gesetzliche Vorschrift zur rezeptpflichtigen Ausgabe von Antibiotika",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Antibiotic Stewardship Programme (ASP) optimieren den Antibiotikaeinsatz in Krankenhaeusern: De-Eskalation auf schmalspektrige Substanzen, Therapiedauerreduzierung, iv-to-oral-Switch. Ziel ist es, Selektion resistenter Keime zu verringern, Kosten zu senken und Nebenwirkungen wie Clostridium-difficile-Infektionen zu reduzieren.",
        funFact = "In der Veterinarmedizin werden weltweit mehr Antibiotika eingesetzt als in der Humanmedizin. Resistenzgene, die in Nutztieren entstehen, koennen auf Bakterien ueberspringen, die Menschen infizieren -- ein wichtiger Treiber der globalen Antibiotikaresistenz."
    ),

    // Question 48
    Question(
        categoryId = 16,
        questionText = "Was ist der Wirkmechanismus von Rituximab und bei welchen Erkrankungen wird es eingesetzt?",
        answerA = "Anti-CD20-monoklonaler Antikoerper, der B-Lymphozyten depletiert -- eingesetzt bei B-Zell-Lymphomen und Autoimmunerkrankungen",
        answerB = "Anti-CD3-Antikoerper, der T-Lymphozyten hemmt -- eingesetzt bei Transplantatabstossung",
        answerC = "Anti-VEGF-Antikoerper, der Tumorblutgefaesse hemmt -- eingesetzt bei Kolorektalkarzinom",
        answerD = "Anti-HER2-Antikoerper, der Tumorzellproliferation hemmt -- eingesetzt bei Mammakarzinom",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Rituximab ist ein chimaerer monoklonaler IgG1-Antikoerper gegen CD20, ein Oberflaechenprotein auf B-Lymphozyten. Es loest Zelllyse durch ADCC (antikoerperabhaengige zellulaere Zytotoxizitaet), CDC (Komplement) und direkte Apoptose aus. Stammzellen und Plasmazellen bleiben verschont (kein CD20).",
        funFact = "Rituximab war 1997 der erste monoklonale Antikoerper, der fuer die Krebstherapie zugelassen wurde -- ein Meilenstein, der die Onkologie revolutionierte und das Zeitalter der gezielten Antikoerper-Therapie einleutete."
    ),

    // Question 49
    Question(
        categoryId = 16,
        questionText = "Was ist die minimale Hemmkonzentration (MHK/MIC) und wie wird sie klinisch genutzt?",
        answerA = "Die hoechste Antibiotika-Konzentration, die Bakterien noch tolerieren koennen",
        answerB = "Die niedrigste Antibiotikakonzentration, die das sichtbare Wachstum eines Bakterienstammes hemmt -- Grundlage fuer Resistenztestung und Dosisfindung",
        answerC = "Die Konzentration, die 90% aller Bakterien einer Art abtoeetet",
        answerD = "Der Blutspiegel, der zur effektiven Behandlung notwendig ist",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die MIC ist ein Schwellenwert: Liegt der Plasmaspiegel des Antibiotikums deutlich ueber der MIC des Erregers, ist eine Therapie erfolgsversprechend. Die EUCAST- und CLSI-Breakpoints definieren 'sensibel', 'intermediaar' und 'resistent' basierend auf erreichbaren Plasmaspiegeln und MIC-Werten.",
        funFact = "Der MIC-Wert ist nicht das Ende der Geschichte: Bei zeitabhaengigen Antibiotika (Penicilline) zaehlt, wie lange der Spiegel ueber der MIC bleibt (T>MIC). Bei konzentrationsabhaengigen (Aminoglykoside, Fluorchinolone) zaehlt das Verhaeltnis Peak/MIC oder AUC/MIC -- pharmakokinetisch-pharmakodynamische Optimierung."
    ),

    // Question 50
    Question(
        categoryId = 16,
        questionText = "Was ist das Konzept der 'Therapierende des pharmakologischen Effekts' -- warum wirken Opioid-Analgetika, wenn die Konzentration schon gesunken ist?",
        answerA = "Weil Opioide eine lange Halbwertszeit haben und langsam eliminiert werden",
        answerB = "Weil die Wirkdauer durch Rezeptorbindungskinetik bestimmt wird -- langsame Dissoziation haelt den Effekt aufrecht, auch wenn Plasmaspiegel fallen (Hysterese)",
        answerC = "Weil der Metabolit aktiver ist als die Muttersubstanz",
        answerD = "Weil Opioide sich im Fettgewebe anreichern und langsam wieder freigesetzt werden",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Hysterese beschreibt die zeitliche Verzoegerung zwischen Plasmakonzentration und Effekt. Bei langsamer Rezeptordissoziation (off-rate) haelt die Bindung -- und damit die Wirkung -- laenger an als der Plasmaspiegel vermuten laesst. Die Effekt-Konzentrations-Kurve bildet eine Schleife ('Hysterese-Loop') statt einer geraden Linie.",
        funFact = "Buprenorphin ist ein partieller Mu-Agonist mit extrem langsamer Dissoziation vom Rezeptor (t1/2 Bindung mehrere Stunden) -- deshalb haelt sein analgetischer Effekt nach transdermaler Applikation bis zu 7 Tage an, obwohl die Plasmakonzentration schon gefallen ist. Dasselbe erklaert seine Effizienz bei der Opioidsubstitution."
    )

)
