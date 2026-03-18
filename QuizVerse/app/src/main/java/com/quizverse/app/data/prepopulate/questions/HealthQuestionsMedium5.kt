package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMedium5(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 16,
        questionText = "In welchem Jahr entdeckte Alexander Fleming das Penicillin?",
        answerA = "1928",
        answerB = "1912",
        answerC = "1945",
        answerD = "1935",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Alexander Fleming beobachtete 1928 zufallig, dass ein Schimmelpilz der Gattung Penicillium Bakterien abtoetete -- eine Entdeckung, die die Medizin revolutionierte.",
        funFact = "Fleming entdeckte Penicillin, weil er eine verunreinigte Petrischale nicht sofort wegwarf -- Neugier und Zufall waren entscheidend fuer eine der groessten Entdeckungen der Medizingeschichte."
    ),

    // Question 2
    Question(
        categoryId = 16,
        questionText = "Wer entdeckte die Roentgenstrahlen im Jahr 1895?",
        answerA = "Marie Curie",
        answerB = "Louis Pasteur",
        answerC = "Wilhelm Conrad Roentgen",
        answerD = "Robert Koch",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Wilhelm Conrad Roentgen entdeckte am 8. November 1895 in Wuerzburg die nach ihm benannten Roentgenstrahlen und erhielt dafuer 1901 den ersten Nobelpreis fuer Physik.",
        funFact = "Roentgens erstes Roentgenbild zeigte die Hand seiner Frau -- sie soll beim Anblick der Knochen erschrocken ausgerufen haben, sie sehe ihren eigenen Tod."
    ),

    // Question 3
    Question(
        categoryId = 16,
        questionText = "Gegen welche Krankheit entwickelte Edward Jenner 1796 den ersten Impfstoff?",
        answerA = "Cholera",
        answerB = "Pocken",
        answerC = "Typhus",
        answerD = "Tuberkulose",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Edward Jenner impfte 1796 einen Jungen mit Kuhpockenmaterial und schuetzte ihn damit vor den gefaehrlichen Menschenpocken -- die erste dokumentierte Schutzimpfung der Geschichte.",
        funFact = "Das Wort 'Vakzin' (Impfstoff) leitet sich vom lateinischen 'vacca' (Kuh) ab -- eine direkte Ehrerbietung an Jenners Kuhpocken-Experiment."
    ),

    // Question 4
    Question(
        categoryId = 16,
        questionText = "Welcher deutsche Arzt gilt als Begruender der modernen Bakteriologie?",
        answerA = "Rudolf Virchow",
        answerB = "Paul Ehrlich",
        answerC = "Robert Koch",
        answerD = "Ignaz Semmelweis",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Robert Koch identifizierte die Erreger von Milzbrand, Tuberkulose und Cholera und entwickelte die nach ihm benannten Koch'schen Postulate als Grundlage der modernen Infektionsmedizin.",
        funFact = "Robert Koch erhielt 1905 den Nobelpreis fuer Medizin fuer seine Tuberkulose-Forschung -- das Robert Koch-Institut in Berlin ist bis heute nach ihm benannt."
    ),

    // Question 5
    Question(
        categoryId = 16,
        questionText = "Wie viele Menschen starben schatzungsweise an der Spanischen Grippe 1918-1920?",
        answerA = "Ca. 5 Millionen",
        answerB = "Ca. 20 bis 50 Millionen",
        answerC = "Ca. 500.000",
        answerD = "Ca. 100 Millionen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Spanische Grippe toedte schaetzungsweise 20 bis 50 Millionen Menschen weltweit -- damit war sie toedlicher als der Erste Weltkrieg, der zeitgleich stattfand.",
        funFact = "Die Spanische Grippe traf ungewoehnlicherweise besonders junge Erwachsene zwischen 20 und 40 Jahren -- normalerweise sterben bei Grippen vor allem Alte und Kinder."
    ),

    // Question 6
    Question(
        categoryId = 16,
        questionText = "Wofuer steht die Abkuerzung MRT in der medizinischen Diagnostik?",
        answerA = "Metabolische Reaktions-Therapie",
        answerB = "Magnetresonanztomografie",
        answerC = "Molekulare Rontgen-Technik",
        answerD = "Mikrobielle Resistenz-Testung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Magnetresonanztomografie (MRT) nutzt starke Magnetfelder und Radiowellen, um detaillierte Bilder des Koerperinneren zu erzeugen -- ohne Roentgenstrahlung.",
        funFact = "Ein MRT-Geraet erzeugt ein Magnetfeld, das etwa 60.000-mal staerker ist als das Erdmagnetfeld -- deshalb duerfen keine Metallgegenstande in den Untersuchungsraum mitgenommen werden."
    ),

    // Question 7
    Question(
        categoryId = 16,
        questionText = "Was misst ein Blutdruckmessgeraet beim systolischen Wert?",
        answerA = "Den Druck waehrend der Erschlaffung des Herzens",
        answerB = "Den Druck waehrend der Herzkontraktion (Auswurfphase)",
        answerC = "Die Herzfrequenz pro Minute",
        answerD = "Die Sauerstoffsaettigung im Blut",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der systolische Blutdruck (oberer Wert) entsteht waehrend der Herzkontraktion, wenn das Herz Blut in die Aorta pumpt -- er ist hoher als der diastolische Wert.",
        funFact = "Der ideale Blutdruck liegt bei etwa 120/80 mmHg -- 'mmHg' steht fuer Millimeter Quecksilbersaeule, eine Masseinheit aus der Zeit, als Blutdruckmessgeraete noch Quecksilber enthielten."
    ),

    // Question 8
    Question(
        categoryId = 16,
        questionText = "Was stellt der Asklepios-Stab als medizinisches Symbol dar?",
        answerA = "Einen Stab mit zwei umwundenen Schlangen und Fluegeln",
        answerB = "Einen Stab mit einer einzigen Schlange",
        answerC = "Ein Kreuz mit einem goldenen Rand",
        answerD = "Eine Waage mit einem Caduceus",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der Asklepios-Stab zeigt einen einfachen Stab mit einer einzigen umwundenen Schlange und ist das korrekte Symbol der Heilkunde -- benannt nach dem griechischen Heilgott Asklepios.",
        funFact = "Haeufig wird der Asklepios-Stab mit dem Hermes-Stab (Caduceus) verwechselt, der zwei Schlangen und Fluegel hat -- der Caduceus ist eigentlich ein Symbol fuer Handel, nicht fuer Medizin."
    ),

    // Question 9
    Question(
        categoryId = 16,
        questionText = "Wann und wo wurde das Rote Kreuz als humanitaere Organisation gegruendet?",
        answerA = "1863 in Genf durch Henry Dunant",
        answerB = "1901 in Paris durch Louis Pasteur",
        answerC = "1848 in Wien durch Ignaz Semmelweis",
        answerD = "1919 in Berlin nach dem Ersten Weltkrieg",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Henry Dunant gruendete 1863 das Internationale Komitee vom Roten Kreuz (IKRK) in Genf, inspiriert durch die Schrecken der Schlacht von Solferino 1859.",
        funFact = "Das Symbol des Roten Kreuzes ist eine Umkehrung der Schweizer Flagge -- rotes Kreuz auf weissem Grund statt weisses Kreuz auf rotem Grund -- als Ehrerbietung an Dunants Heimatland."
    ),

    // Question 10
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter 'Quarantaene' als oeffentliche Gesundheitsmassnahme?",
        answerA = "Die Isolierung kranker Personen zur Behandlung",
        answerB = "Die Absonderung von Personen, die moeglicherweise ansteckend sind",
        answerC = "Die Desinfektion oeffentlicher Gebaeude nach einem Ausbruch",
        answerD = "Die Impfpflicht in Risikogebieten",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Quarantaene bedeutet die Absonderung von Personen, die einer Infektionskrankheit ausgesetzt waren -- auch wenn sie noch keine Symptome zeigen -- um eine weitere Ausbreitung zu verhindern.",
        funFact = "Der Begriff 'Quarantaene' stammt vom italienischen 'quarantina giorni' (vierzig Tage) -- Venedig hielt im 14. Jahrhundert ankommende Schiffe 40 Tage auf See, um die Pest fernzuhalten."
    ),

    // Question 11
    Question(
        categoryId = 16,
        questionText = "Welche Krankheit wurde durch eine globale Impfkampagne der WHO vollstaendig ausgerottet?",
        answerA = "Polio",
        answerB = "Masern",
        answerC = "Pocken",
        answerD = "Typhus",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Die Pocken wurden 1980 von der WHO als weltweit ausgerottet erklaert -- die einzige Infektionskrankheit, die bisher vollstaendig durch Impfung besiegt wurde.",
        funFact = "Die letzten bekannten Pockenvirusproben der Welt werden heute in zwei gesicherten Labors aufbewahrt: eines in den USA (CDC Atlanta) und eines in Russland (VECTOR Nowosibirsk)."
    ),

    // Question 12
    Question(
        categoryId = 16,
        questionText = "Was ist die Hauptaufgabe des Robert Koch-Instituts (RKI) in Deutschland?",
        answerA = "Die Entwicklung neuer Impfstoffe fuer den deutschen Markt",
        answerB = "Die Ueberwachung und Erforschung von Infektionskrankheiten sowie Gesundheitsberichterstattung",
        answerC = "Die Zulassung von Medikamenten fuer den deutschen Markt",
        answerD = "Die Verwaltung der gesetzlichen Krankenversicherungen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Robert Koch-Institut ist die zentrale Bundesbehoerde Deutschlands fuer Krankheitsbeobachtung und -praevention -- es ueberwacht Infektionskrankheiten und erstellt den Gesundheitsbericht.",
        funFact = "Waehrend der COVID-19-Pandemie wurde das RKI durch taeglich aktualisierte Lageberichte in der deutschen Bevoelkerung einem breiten Publikum bekannt -- vorher kannten nur Fachleute die Behoerde."
    ),

    // Question 13
    Question(
        categoryId = 16,
        questionText = "Wo hat die Weltgesundheitsorganisation (WHO) ihren Hauptsitz?",
        answerA = "New York",
        answerB = "Genf",
        answerC = "Wien",
        answerD = "Bruessel",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die WHO hat ihren Hauptsitz in Genf, Schweiz, und wurde am 7. April 1948 als Sonderorganisation der Vereinten Nationen gegruendet.",
        funFact = "Der 7. April ist wegen der WHO-Gruendung der Weltgesundheitstag -- und der aktuelle WHO-Generaldirektor ist Tedros Adhanom Ghebreyesus aus Aethiopien, der erste Afrikaner in dieser Position."
    ),

    // Question 14
    Question(
        categoryId = 16,
        questionText = "Was ist das Ziel des deutschen Hausarzt-Modells in der Primaeversorgung?",
        answerA = "Patienten direkt an Spezialisten weiterzuleiten, ohne Allgemeinarzt",
        answerB = "Den Hausarzt als erste Anlaufstelle und Koordinator der Versorgung einzusetzen",
        answerC = "Alle medizinischen Leistungen in einem Krankenhaus zu buendeln",
        answerD = "Privatpatienten bevorzugt zu behandeln",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Im Hausarzt-Modell fungiert der Allgemeinarzt als 'Gatekeeper' -- er kennt den Patienten ganzheitlich, koordiniert die Behandlung und vermeidet unnoetige Facharztbesuche.",
        funFact = "In einigen deutschen Bundeslaendern gibt es freiwillige Hausarztvertraege mit Krankenkassen -- Patienten die sich einschreiben, erhalten oft bessere Versorgung und niedrigere Zuzahlungen."
    ),

    // Question 15
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen gesetzlicher (GKV) und privater Krankenversicherung (PKV) in Deutschland?",
        answerA = "GKV ist freiwillig, PKV ist Pflicht fuer alle Arbeitnehmer",
        answerB = "GKV ist beitragsfinanziert und einkommensabhaengig, PKV richtet sich nach individuellem Risiko",
        answerC = "GKV deckt nur ambulante, PKV nur stationaere Behandlungen ab",
        answerD = "GKV und PKV bieten identische Leistungen zu unterschiedlichen Preisen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "In der GKV zahlen Versicherte einen prozentualen Anteil ihres Einkommens, unabhaengig vom Gesundheitszustand. In der PKV wird der Beitrag nach Alter, Gesundheit und Leistungsumfang berechnet.",
        funFact = "Deutschland hat eines der aeltesten gesetzlichen Krankenversicherungssysteme der Welt -- Bismarck fuehrte es 1883 ein, urspruenglich um Arbeiter zu sichern und soziale Unruhen zu verhindern."
    ),

    // Question 16
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter 'Evidenzbasierter Medizin' (EBM)?",
        answerA = "Behandlung allein auf Basis der Erfahrung des Arztes",
        answerB = "Die Verknuepfung von bester wissenschaftlicher Forschung, klinischer Expertise und Patientenpraeferenz",
        answerC = "Die ausschliessliche Nutzung traditioneller Heilmethoden",
        answerD = "Die gesetzliche Abrechnung aerztlicher Leistungen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Evidenzbasierte Medizin verbindet die beste verfuegbare wissenschaftliche Evidenz mit der klinischen Erfahrung des Arztes und den individuellen Wuenschen des Patienten.",
        funFact = "Der Begriff 'Evidence-Based Medicine' wurde in den 1990er Jahren von Gordon Guyatt an der McMaster University in Kanada gepragt -- er revolutionierte, wie medizinische Entscheidungen getroffen werden."
    ),

    // Question 17
    Question(
        categoryId = 16,
        questionText = "Was ist ein Computertomograf (CT) und wie funktioniert er?",
        answerA = "Ein Geraet, das mit Schallwellen dreidimensionale Bilder erzeugt",
        answerB = "Ein Roentgengeraet, das aus vielen Winkeln Schnittbilder des Koerpers aufnimmt",
        answerC = "Ein Magnetgeraet, das Wasserstaerke im Gewebe misst",
        answerD = "Ein Geraet, das elektrische Aktivitaet im Gehirn aufzeichnet",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der CT rotiert eine Roentgenroehre um den Koerper und nimmt aus vielen Winkeln Bilder auf -- ein Computer berechnet daraus praezise Schnittbilder des Koerperinneren.",
        funFact = "Ein CT-Scan dauert oft nur wenige Sekunden, liefert aber hunderte von Schnittbildern -- ein grosses Fortschritt gegenueber klassischem Roentgen, das nur zweidimensionale Bilder erzeugt."
    ),

    // Question 18
    Question(
        categoryId = 16,
        questionText = "Welcher Arzt entdeckte, dass Haendewaschen in der Geburtshilfe Kindbettfieber verhindern kann?",
        answerA = "Louis Pasteur",
        answerB = "Robert Koch",
        answerC = "Ignaz Semmelweis",
        answerD = "Joseph Lister",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Ignaz Semmelweis erkannte um 1847, dass Aerzte, die ohne Haendewaschen von der Pathologie in den Kreisssaal gingen, Kindbettfieber uebertrugen -- doch seine Erkenntnis wurde von Kollegen abgelehnt.",
        funFact = "Tragischerweise starb Semmelweis selbst 1865 an einer Infektion -- moeglicherweise genau an dem Erreger, dessen Uebertragung er sein Leben lang bekaempft hatte."
    ),

    // Question 19
    Question(
        categoryId = 16,
        questionText = "Was ist Ultraschall (Sonografie) in der medizinischen Diagnostik?",
        answerA = "Ein bildgebendes Verfahren mit Roentgenstrahlen",
        answerB = "Ein bildgebendes Verfahren mit Schallwellen, die vom Gewebe reflektiert werden",
        answerC = "Ein Verfahren zur Messung der Hirnstroeme",
        answerD = "Ein Magnetresonanzverfahren fuer Weichteile",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Sonografie sendet hochfrequente Schallwellen aus, die vom Gewebe unterschiedlich stark reflektiert werden -- aus den Echosignalen wird ein Bild berechnet.",
        funFact = "Ultraschall wurde urspruenglich aus der Sonar-Technologie der Marine entwickelt -- die medizinische Anwendung begann in den 1950er Jahren, heute ist sie aus der Medizin nicht mehr wegzudenken."
    ),

    // Question 20
    Question(
        categoryId = 16,
        questionText = "Was war die historische Ursache der Pest (Schwarzer Tod) im 14. Jahrhundert?",
        answerA = "Ein Virus, der durch Atemluft uebertragen wurde",
        answerB = "Das Bakterium Yersinia pestis, uebertragen durch Floeche von Ratten",
        answerC = "Ein Pilz, der durch verunreinigtes Getreide aufgenommen wurde",
        answerD = "Ein Parasit, der durch Trinwasser uebertragen wurde",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Pest wird durch das Bakterium Yersinia pestis verursacht und wird hauptsaechlich durch den Biss infizierter Floeche uebertragen, die zuvor Nagetiere wie Ratten befallen hatten.",
        funFact = "Der Schwarze Tod toedte zwischen 1347 und 1353 schatzungsweise ein Drittel der europaeischen Bevoelkerung -- der groesste demographische Einbruch in der Geschichte Europas."
    ),

    // Question 21
    Question(
        categoryId = 16,
        questionText = "Was ist bei einem Bluttest der Haemoglobin-Wert (Hb)?",
        answerA = "Die Anzahl der weissen Blutkoerperchen",
        answerB = "Ein Mass fuer den Sauerstofftransport-Proteinkomplex in den roten Blutkoerperchen",
        answerC = "Der Zuckergehalt im Blut",
        answerD = "Die Anzahl der Blutplaettchen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Haemoglobin ist das eisenhaltige Protein in den roten Blutkoerperchen, das Sauerstoff bindet und transportiert -- ein niedriger Hb-Wert deutet auf Anaemie (Blutarmut) hin.",
        funFact = "Haemoglobin macht Blut rot -- das Eisenatom im Haem-Teil des Proteins bindet Sauerstoff und verleiht dabei dem oxygenierten Blut seine charakteristisch leuchtend rote Farbe."
    ),

    // Question 22
    Question(
        categoryId = 16,
        questionText = "Wofuer wird ein EKG (Elektrokardiogramm) eingesetzt?",
        answerA = "Um Hirnstroeme bei Epilepsie zu messen",
        answerB = "Um die elektrische Aktivitaet des Herzens aufzuzeichnen",
        answerC = "Um Muskelaktivitaet bei Lahmungen zu messen",
        answerD = "Um den Blutfluss in den Koronararterien darzustellen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das EKG zeichnet die elektrischen Impulse auf, die den Herzschlag steuern -- es ermoeglicht die Diagnose von Herzrhythmusstoerugen, Herzinfarkten und anderen Herzerkrankungen.",
        funFact = "Das erste brauchbare EKG wurde 1902 von Willem Einthoven aufgezeichnet -- er erhielt dafuer 1924 den Nobelpreis fuer Medizin und entwickelte die Ableitung, die noch heute als 'Einthoven-Dreieck' bekannt ist."
    ),

    // Question 23
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem 'Koch'schen Postulat' in der Infektionsmedizin?",
        answerA = "Die Regel, dass Antibiotika nur bei bakteriellen, nicht bei viralen Infektionen wirken",
        answerB = "Kriterien zum Nachweis, dass ein bestimmter Erreger eine bestimmte Krankheit verursacht",
        answerC = "Die Hygieneregelungen fuer offentliche Laboratorien",
        answerD = "Das Prinzip der Herdenimmunitaet bei Impfkampagnen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Robert Koch formulierte vier Bedingungen (Postulate), die erfuellt sein muessen, um zu beweisen, dass ein Mikroorganismus eine bestimmte Krankheit verursacht -- Grundlage der Infektionsmedizin.",
        funFact = "Koch's Postulate konnten auf Viren lange nicht angewendet werden, da Viren nicht auf Naehrmedien kultivierbar sind -- fuer Viren wurden spaeter modifizierte Versionen entwickelt."
    ),

    // Question 24
    Question(
        categoryId = 16,
        questionText = "Was ist die Aufgabe von Lymphknoten im menschlichen Immunsystem?",
        answerA = "Blut filtern und Sauerstoff ans Gewebe abgeben",
        answerB = "Krankheitserreger filtern und Immunzellen bereitstellen",
        answerC = "Hormone ins Blut abgeben",
        answerD = "Den Blutdruck regulieren",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Lymphknoten sind Filterstationen der Lymphgefaesse, in denen Immunzellen (Lymphozyten) Krankheitserreger erkennen und bekaempfen -- geschwollene Lymphknoten zeigen oft eine aktive Immunreaktion an.",
        funFact = "Im menschlichen Koerper gibt es etwa 600 bis 700 Lymphknoten, von denen viele in der Achselhoehle, Leiste und am Hals konzentriert sind -- deshalb tasten Aerzte diese Bereiche ab."
    ),

    // Question 25
    Question(
        categoryId = 16,
        questionText = "Welche Massnahme half am meisten, die Sterblichkeit in europaeischen Staedten im 19. Jahrhundert zu senken?",
        answerA = "Die Einfuehrung von Antibiotika",
        answerB = "Verbesserung der Kanalisation und sauberem Trinkwasser",
        answerC = "Die Entdeckung von Impfstoffen gegen alle wichtigen Krankheiten",
        answerD = "Der Einsatz moderner Roentgentechnik",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Sauberes Wasser und moderne Abwassersysteme reduzierten Krankheiten wie Cholera und Typhus drastisch -- diese oeffentlichen Hygienemassnahmen retteten mehr Leben als alle Medikamente zusammen.",
        funFact = "John Snow, ein Londoner Arzt, bewies 1854 waehrend einer Choleraepidemie durch Kartierung der Faelle, dass die Krankheit durch eine Wasserpumpe verbreitet wurde -- ein Grundlagenwerk der Epidemiologie."
    ),

    // Question 26
    Question(
        categoryId = 16,
        questionText = "Was ist ein Blutbild und welche Hauptgruppen von Zellen werden darin gemessen?",
        answerA = "Eine Aufzeichnung der Herzschlagkurve mit drei Kurventypen",
        answerB = "Eine Laboruntersuchung, die rote Blutkoerperchen, weisse Blutkoerperchen und Blutplaettchen misst",
        answerC = "Ein Bild des Blutgefaesssystems aufgenommen durch CT",
        answerD = "Eine Messung von Hormonen und Enzymen im Blutserum",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Blutbild zaehlt die wichtigsten Blutzellen: Erythrozyten (rote Blutkoerperchen fuer Sauerstofftransport), Leukozyten (weisse Blutkoerperchen fuer Immunabwehr) und Thrombozyten (Blutplaettchen fuer Gerinnung).",
        funFact = "Im grossen Blutbild werden Leukozyten noch weiter unterteilt in Untergruppen wie Neutrophile, Lymphozyten und Monozyten -- jede Gruppe zeigt andere Aspekte der Immunreaktion."
    ),

    // Question 27
    Question(
        categoryId = 16,
        questionText = "Wann begann die COVID-19-Pandemie offiziell laut WHO-Ausrufung?",
        answerA = "Januar 2020",
        answerB = "Maerz 2020",
        answerC = "Dezember 2019",
        answerD = "Juni 2020",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die WHO erklaerte COVID-19 am 11. Maerz 2020 offiziell zur Pandemie -- zu diesem Zeitpunkt hatten sich Faelle bereits auf uber 114 Laender ausgebreitet.",
        funFact = "Der Name COVID-19 steht fuer 'Coronavirus Disease 2019' -- das Virus selbst heisst SARS-CoV-2 und wurde nach der SARS-Coronavirus-Familie benannt, zu der es gehoert."
    ),

    // Question 28
    Question(
        categoryId = 16,
        questionText = "Was ist Homoeopathie und wie wird sie wissenschaftlich bewertet?",
        answerA = "Eine Naturheilkunde mit wissenschaftlich gut belegter Wirksamkeit",
        answerB = "Ein alternatives Heilsystem mit stark verduennten Substanzen, das nach aktuellem Forschungsstand nicht ueber den Placeboeffekt hinausgeht",
        answerC = "Eine Form der Phytotherapie mit pflanzlichen Extrakten",
        answerD = "Eine anerkannte Methode der chinesischen Medizin mit belegter Wirkung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Homoeopathie basiert auf der Idee, dass stark verduennte Substanzen heilen koennen -- systematische Reviews und Meta-Analysen finden keine Wirksamkeit ueber den Placeboeffekt hinaus.",
        funFact = "Bei homoeopathischen Hochpotenzen wie D30 ist theoretisch kein einziges Molekuel der Ausgangssubstanz mehr enthalten -- die Verduennung entspricht etwa einem Tropfen in allen Ozeanen der Erde kombiniert."
    ),

    // Question 29
    Question(
        categoryId = 16,
        questionText = "Welchen Blutdruckwert bezeichnet man als Hypertonie (Bluthochdruck)?",
        answerA = "Ueber 120/80 mmHg",
        answerB = "Ueber 140/90 mmHg",
        answerC = "Ueber 100/70 mmHg",
        answerD = "Ueber 160/100 mmHg",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bluthochdruck wird nach internationalen Leitlinien bei anhaltenden Werten ueber 140/90 mmHg diagnostiziert -- er ist ein wichtiger Risikofaktor fuer Herzinfarkt und Schlaganfall.",
        funFact = "Hypertonie wird oft als 'stiller Killer' bezeichnet, weil sie jahrelang keine Symptome macht -- weltweit hat etwa jeder dritte Erwachsene Bluthochdruck, viele ohne es zu wissen."
    ),

    // Question 30
    Question(
        categoryId = 16,
        questionText = "Wofuer steht die Abkuerzung PCR in der medizinischen Diagnostik?",
        answerA = "Pathologisches Cortison-Reaktionstest",
        answerB = "Polymerase-Kettenreaktion -- ein Verfahren zur Vervielfaeltigung von DNA",
        answerC = "Praeklinische Chemikalien-Resistenz",
        answerD = "Proteingesteuerte Chemotherapie-Reaktion",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die PCR (Polymerase Chain Reaction) vervielfaeltigt spezifische DNA-Abschnitte millionenfach, sodass auch kleinste Mengen von Erregern nachgewiesen werden koennen -- Grundlage vieler Infektionsdiagnosen.",
        funFact = "Kary Mullis erfand die PCR-Methode 1983 -- einer Legende nach hatte er die zundende Idee waehrend einer Autofahrt. Er erhielt dafuer 1993 den Nobelpreis fuer Chemie."
    ),

    // Question 31
    Question(
        categoryId = 16,
        questionText = "Was ist das Prinzip der Herdenimmunitaet bei Impfkampagnen?",
        answerA = "Nur kranke Personen muessen geimpft werden",
        answerB = "Wenn genuegend Menschen immun sind, koennen auch Ungeimpfte durch indirekte Protektion geschuetzt werden",
        answerC = "Herden von Tieren muessen geimpft werden, bevor Menschen Schutz erhalten",
        answerD = "Impfungen sind nur wirksam wenn alle Mitglieder einer Gemeinschaft gleichzeitig geimpft werden",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Herdenimmunitaet entsteht, wenn so viele Menschen immun sind, dass der Erreger sich nicht mehr ausreichend verbreiten kann -- so werden auch Personen geschuetzt, die nicht geimpft werden koennen.",
        funFact = "Die Herdenimmunitaetsschwelle haengt von der Infektioesitaet des Erregers ab -- bei Masern muessen etwa 95% der Bevoelkerung immun sein, bei Polio reichen etwa 80-85%."
    ),

    // Question 32
    Question(
        categoryId = 16,
        questionText = "Welche Impfung empfiehlt die STIKO (Staendige Impfkommission) allen Erwachsenen alle 10 Jahre?",
        answerA = "Hepatitis-B-Impfung",
        answerB = "Tetanus-Auffrischimpfung (kombiniert mit Diphtherie)",
        answerC = "Polio-Auffrischimpfung",
        answerD = "Typhus-Impfung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die STIKO empfiehlt allen Erwachsenen alle 10 Jahre eine Auffrischimpfung gegen Tetanus und Diphtherie -- der Impfschutz laesst nach und muss regelmaessig erneuert werden.",
        funFact = "Tetanus (Wundstarrkrampf) kommt durch das Bakterium Clostridium tetani vor, das weltweit im Boden vorkommt -- selbst kleine Wunden koennen zur Infektion fuehren, weshalb die Impfung so wichtig ist."
    ),

    // Question 33
    Question(
        categoryId = 16,
        questionText = "Was ist die Hauptaufgabe einer Krankenversicherungskarte (Versichertenkarte) in Deutschland?",
        answerA = "Die Finanzierung medizinischer Behandlungen direkt beim Arzt",
        answerB = "Den Versicherungsstatus und die Zugehoerigkeit zur Krankenkasse zu belegen und Daten digital zu speichern",
        answerC = "Das Ausstellen von Krankschreibungen fuer den Arbeitgeber",
        answerD = "Den Zugang zu Medikamenten in der Apotheke zu genehmigen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Versichertenkarte (seit 2015 die elektronische Gesundheitskarte) belegt die Kassenzugehoerigkeit und kann Stammdaten, Notfalldaten und kuenftig weitere Gesundheitsdaten speichern.",
        funFact = "Deutschland war eines der letzten Laender Europas, das eine elektronische Gesundheitskarte einfuehrte -- die Einfuehrung wurde durch Datenschutzdebatten jahrzehntelang verzoegert."
    ),

    // Question 34
    Question(
        categoryId = 16,
        questionText = "Was ist Akupunktur und wie wird ihre Wirksamkeit wissenschaftlich bewertet?",
        answerA = "Eine vollstaendig unwirksame Methode ohne nachgewiesene Effekte",
        answerB = "Eine Methode aus der traditionellen chinesischen Medizin mit nachgewiesener Wirkung bei bestimmten Schmerzzustaenden",
        answerC = "Eine Operationstechnik der modernen Chirurgie",
        answerD = "Ein Verfahren der Physiotherapie, das in Deutschland nicht anerkannt ist",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Akupunktur hat in Studien messbare Effekte bei chronischen Schmerzen wie Ruecken- und Knieschmerzen gezeigt -- allerdings zeigen Scheinakupunktur-Studien oft aehnliche Ergebnisse, was die Theorie der Meridiane in Frage stellt.",
        funFact = "Die groesste je durchgefuehrte Akupunkturstudie weltweit war die GERAC-Studie aus Deutschland (2006) -- sie zeigte, dass Akupunktur bei chronischen Rueckenschmerzen besser wirkte als Standardtherapie."
    ),

    // Question 35
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Pandemie und Epidemie?",
        answerA = "Eine Epidemie betrifft nur Tiere, eine Pandemie nur Menschen",
        answerB = "Eine Epidemie ist auf eine Region begrenzt, eine Pandemie betrifft mehrere Kontinente weltweit",
        answerC = "Eine Pandemie ist immer toedlicher als eine Epidemie",
        answerD = "Epidemien entstehen durch Viren, Pandemien durch Bakterien",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eine Epidemie ist eine gehaeuftes Auftreten einer Krankheit in einer definierten Region -- eine Pandemie ist die weltweite, Kontinente-uebergreifende Ausbreitung einer Infektionskrankheit.",
        funFact = "Das Wort Epidemie stammt aus dem Griechischen: 'epi' (auf) und 'demos' (Volk) -- es bedeutet also woertlich 'auf das Volk gekommen', also eine Krankheit die eine Bevoelkerung ueberwaeltigt."
    ),

    // Question 36
    Question(
        categoryId = 16,
        questionText = "Was ist der Zweck einer Magenspiegelung (Gastroskopie)?",
        answerA = "Den Herzrhythmus ueber Schallwellen zu messen",
        answerB = "Die Schleimhaut von Speiseroehre, Magen und Zwolffingerdarm optisch zu untersuchen",
        answerC = "Magensaeure durch Neutralisation zu behandeln",
        answerD = "Die Gallenblase auf Steine zu untersuchen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei der Gastroskopie wird ein flexibler Schlauch mit Kamera durch den Mund eingefuehrt, um die Schleimhaeute zu beurteilen, Biopsien zu entnehmen und Befunde wie Magengeschwuere oder Krebs zu diagnostizieren.",
        funFact = "Das erste starre Endoskop wurde 1805 von Philipp Bozzini entwickelt -- die moderne flexible Gastroskopie begann in den 1950er Jahren mit der Einfuehrung von Glasfasertechnik."
    ),

    // Question 37
    Question(
        categoryId = 16,
        questionText = "Welche Krankheit loeste das SARS-CoV-1-Virus im Jahr 2002/2003 aus?",
        answerA = "MERS (Middle East Respiratory Syndrome)",
        answerB = "SARS (Schweres Akutes Respiratorisches Syndrom)",
        answerC = "COVID-19",
        answerD = "Vogelgrippe (H5N1)",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "SARS-CoV-1 verursachte 2002/2003 den ersten SARS-Ausbruch, der von China ausging und 8.000 Menschen infizierte mit einer Sterblichkeitsrate von etwa 10% -- deutlich hoher als SARS-CoV-2.",
        funFact = "Der SARS-Ausbruch 2002/2003 wurde relativ schnell eingedaemmt -- durch konsequente Quarantaene und Kontaktverfolgung, bevor ein Impfstoff entwickelt werden konnte."
    ),

    // Question 38
    Question(
        categoryId = 16,
        questionText = "Was ist die Funktion des Pankreas (der Bauchspeicheldruse) im Koerper?",
        answerA = "Blut filtern und Gallenfluessigkeit produzieren",
        answerB = "Verdauungsenzyme und Hormone wie Insulin und Glucagon produzieren",
        answerC = "Rote Blutkoerperchen herstellen und Eisen speichern",
        answerD = "Nahrungsmittel mechanisch zerkleinern und in den Magen weitergeben",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Bauchspeicheldruse hat zwei Funktionen: Sie produziert Verdauungsenzyme (exokrin) und die Hormone Insulin und Glucagon (endokrin) zur Blutzuckerregulation.",
        funFact = "Die Entdeckung des Insulins 1921 durch Banting und Best in Kanada war eine der dramatischsten Rettungsaktionen der Medizingeschichte -- Typ-1-Diabetiker, die zuvor innerhalb von Monaten starben, konnten plotzlich gerettet werden."
    ),

    // Question 39
    Question(
        categoryId = 16,
        questionText = "Wer entdeckte die Struktur der DNA als Doppelhelix?",
        answerA = "Louis Pasteur und Robert Koch",
        answerB = "James Watson und Francis Crick (mit Daten von Rosalind Franklin)",
        answerC = "Gregor Mendel und Charles Darwin",
        answerD = "Marie Curie und Wilhelm Roentgen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Watson und Crick veroeffentlichten 1953 die Struktur der DNA als Doppelhelix -- ihre Arbeit basierte wesentlich auf den Roentgenkristallografiedaten von Rosalind Franklin.",
        funFact = "Rosalind Franklin machte das entscheidende Roentgenbild ('Photo 51'), das Watson und Crick den Durchbruch ermoeglichte -- sie erhielt jedoch keinen Nobelpreis, der 1962 an Watson, Crick und Wilkins ging."
    ),

    // Question 40
    Question(
        categoryId = 16,
        questionText = "Was ist das Ziel von Krebsvorsorgeuntersuchungen (Screening)?",
        answerA = "Krebs in einem fruehen, besser behandelbaren Stadium zu erkennen",
        answerB = "Krebszellen durch regelmaessige Untersuchungen zu zerstoeren",
        answerC = "Das Immunsystem zur Krebsabwehr zu trainieren",
        answerD = "Chemotherapie rechtzeitig einzuleiten bevor Symptome auftreten",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Krebsscreening zielt darauf ab, Krebs oder Krebsvorstufen zu entdecken, bevor Symptome auftreten -- frueh erkannter Krebs ist haeufig besser behandelbar und hat hoehere Heilungschancen.",
        funFact = "Der Darmkrebs-Vorsorgetest (Koloskopie) wird in Deutschland ab 50 Jahren empfohlen -- bei vollstaendiger Teilnahme koennte er die Darmkrebssterblichkeit um bis zu 80% senken."
    ),

    // Question 41
    Question(
        categoryId = 16,
        questionText = "Was war die historische Bedeutung der Entdeckung von Insulin 1921?",
        answerA = "Insulin heilte Typ-2-Diabetes vollstaendig",
        answerB = "Insulin rettete das Leben von Typ-1-Diabetikern, die zuvor innerhalb von Monaten starben",
        answerC = "Insulin ermoeglichte erstmals die Behandlung von Adipositas",
        answerD = "Insulin war das erste synthetisch hergestellte Hormon",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vor der Insulinentdeckung durch Banting und Best war Typ-1-Diabetes eine rasch toedliche Erkrankung -- mit Insulin konnten Betroffene plotzlich ein normales Leben fuehren.",
        funFact = "Frederick Banting war ein junger, kaum bekannter Arzt, als er die Insulin-Entdeckung machte -- John Macleod, der Laborleiter, erhielt trotzdem den Nobelpreis mit ihm, was zu einem oeffentlichen Streit fuehrte."
    ),

    // Question 42
    Question(
        categoryId = 16,
        questionText = "Was ist eine Biopsie in der medizinischen Diagnostik?",
        answerA = "Eine bildgebende Untersuchung des lebenden Gewebes",
        answerB = "Die Entnahme einer Gewebeprobe zur mikroskopischen Untersuchung",
        answerC = "Ein Bluttest zur Krebsfrueherkennung",
        answerD = "Die Bestrahlung von Gewebeproben zur Desinfektion",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eine Biopsie ist die Entnahme von Gewebeproben aus dem lebenden Koerper -- die Proben werden mikroskopisch untersucht, um Krebs, Entzuendungen oder andere Erkrankungen sicher zu diagnostizieren.",
        funFact = "Das Wort Biopsie stammt aus dem Griechischen: 'bios' (Leben) und 'opsis' (Anblick) -- es bedeutet also das 'Anschauen von lebendem Gewebe', im Gegensatz zur Autopsie nach dem Tod."
    ),

    // Question 43
    Question(
        categoryId = 16,
        questionText = "Welche Krankheit verursachte die groesste Epidemie in der US-amerikanischen Geschichte und infizierte Hunderttausende in den 1950er Jahren?",
        answerA = "Tuberkulose",
        answerB = "Polio (Kinderlahmung)",
        answerC = "Syphilis",
        answerD = "Scharlach",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Polio (Poliomyelitis) verursachte in den USA Epidemien der 1950er Jahre mit jaelichen Ausbruechen -- der Impfstoff von Jonas Salk (1955) beendete die Epidemien rasch.",
        funFact = "US-Praesident Franklin D. Roosevelt erkrankte 1921 an Polio und war seitdem auf einen Rollstuhl angewiesen -- er grundete die Organisation, die spaeter den Salk-Impfstoff mitfinanzierte."
    ),

    // Question 44
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter 'Antibiotikaresistenz'?",
        answerA = "Die Unfaehigkeit eines Menschen, Antibiotika zu vertragen",
        answerB = "Die Faehigkeit von Bakterien, Antibiotika wirkungslos zu machen",
        answerC = "Die Allergie gegen bestimmte Antibiotika-Wirkstoffe",
        answerD = "Die verringerte Wirksamkeit von Antibiotika durch falsche Lagerung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Antibiotikaresistenz entsteht, wenn Bakterien Mechanismen entwickeln, die Antibiotika unwirksam machen -- oft durch Ueberdosierung, unsachgemaessen Einsatz oder natuarliche Mutation.",
        funFact = "Die WHO bezeichnet Antibiotikaresistenz als eine der groessten Bedrohungen fuer die globale Gesundheit -- schaetzungsweise sterben weltweit bereits 700.000 Menschen jaehrlich an resistenten Keimen."
    ),

    // Question 45
    Question(
        categoryId = 16,
        questionText = "Was ist ein Anamnesegespraech in der aerztlichen Praxis?",
        answerA = "Die koerperliche Untersuchung des Patienten durch den Arzt",
        answerB = "Das systematische Erfragen der Krankengeschichte und aktuellen Beschwerden des Patienten",
        answerC = "Die Besprechung der Laborergebnisse mit dem Patienten",
        answerD = "Die Einwilligung des Patienten in eine Behandlung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Anamnese ist das strukturierte Gespraech ueber Beschwerden, Krankengeschichte, Vorerkrankungen und Risikofaktoren -- sie ist das wichtigste diagnostische Instrument und Ausgangspunkt jeder Untersuchung.",
        funFact = "Studien zeigen, dass Aerzte den Patienten im Durchschnitt nach nur 18 Sekunden unterbrechen -- dabei koennte das freie Erzaehlen in den meisten Faellen innerhalb von 2 Minuten abgeschlossen sein."
    ),

    // Question 46
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Infektion und Infektionskrankheit?",
        answerA = "Es gibt keinen Unterschied -- beide Begriffe bedeuten dasselbe",
        answerB = "Infektion ist das Eindringen eines Erregers, Infektionskrankheit entsteht nur wenn der Erreger Schaden anrichtet",
        answerC = "Infektionskrankheiten werden durch Viren verursacht, Infektionen durch Bakterien",
        answerD = "Eine Infektion ist ansteckend, eine Infektionskrankheit ist es nicht",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eine Infektion beschreibt das Eindringen und Vermehren von Erregern im Wirt -- eine Infektionskrankheit (klinische Manifestation) tritt nur auf, wenn der Erreger das Wirt-Immunsystem ueberwaeltigt.",
        funFact = "Viele Menschen tragen Erreger in sich, ohne krank zu werden -- z.B. traegt etwa ein Drittel der Weltbevoelkerung Mycobacterium tuberculosis in sich, aber nur 5-10% entwickeln jemals aktive Tuberkulose."
    ),

    // Question 47
    Question(
        categoryId = 16,
        questionText = "Was ist das Ziel des deutschen Praeventivsystems 'Frueherkennung' bei Kindern (U-Untersuchungen)?",
        answerA = "Kinder auf sportliche Eignung zu testen",
        answerB = "Entwicklungsstoerungen, Erkrankungen und Behinderungen fruehzeitig zu erkennen",
        answerC = "Impfungen in festen Abstaenden durchzufuehren",
        answerD = "Den Ernaehrungszustand von Kindern zu dokumentieren",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die U-Untersuchungen (U1 bis U9) sind standardisierte Vorsorgeuntersuchungen in Deutschland, die bei Kindern von der Geburt bis zum Schulalter Entwicklungsstoerungen, koerperliche Erkrankungen und Sinnesstorungen fruehzeitig aufdecken.",
        funFact = "Deutschland hat mit dem U-Untersuchungsprogramm eines der dichtesten Kinder-Vorsorgesysteme der Welt -- in den ersten 6 Lebensjahren gibt es neun Pflichtuntersuchungen plus Jugendgesundheitsuntersuchungen."
    ),

    // Question 48
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter 'Placeboeffekt' in der Medizin?",
        answerA = "Die Wirkung eines Medikaments, die ueber die erwartete Dosis hinausgeht",
        answerB = "Eine messbare positive Wirkung durch die Erwartungshaltung des Patienten, unabhaengig vom Wirkstoff",
        answerC = "Die Nebenwirkung eines Scheinpraeparats ohne Wirkstoff",
        answerD = "Die Immunreaktion des Koerpers auf eine Behandlung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der Placeboeffekt entsteht, wenn allein die Erwartung einer Behandlung messbare physiologische Veraenderungen ausloest -- er ist ein reales, neurobiologisches Phaenomen, nicht nur 'Einbildung'.",
        funFact = "Placeboeffekte koennen so stark sein, dass Schein-Operationen (Placebo-Chirurgie) in Studien aehnliche Ergebnisse erzielten wie echte Eingriffe -- z.B. bei bestimmten Knieschmerzen."
    ),

    // Question 49
    Question(
        categoryId = 16,
        questionText = "Welche Aufgabe hat die EMA (European Medicines Agency) in der oeffentlichen Gesundheit?",
        answerA = "Die Verwaltung der europaeischen Krankenversicherungssysteme",
        answerB = "Die wissenschaftliche Bewertung und Zulassung von Arzneimitteln in der EU",
        answerC = "Die Ausbildung von Aerztinnen und Aerzten in Europa",
        answerD = "Die Koordination von Impfprogrammen in EU-Mitgliedsstaaten",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die EMA prueft wissenschaftlich die Qualitaet, Sicherheit und Wirksamkeit von Arzneimitteln fuer die EU und empfiehlt die Zulassung -- die Europaeische Kommission erteilt dann die offizielle Genehmigung.",
        funFact = "Waehrend COVID-19 beschleunigte die EMA ihre Zulassungsverfahren durch ein Rolling-Review-Verfahren -- sie prufte Daten fortlaufend waehrend die Studien noch liefen, statt auf den fertigen Abschlussbericht zu warten."
    ),

    // Question 50
    Question(
        categoryId = 16,
        questionText = "Was ist das Cholera-Erreger und wie wird er uebertragen?",
        answerA = "Ein Virus, das durch Atemluft von Mensch zu Mensch uebertragen wird",
        answerB = "Das Bakterium Vibrio cholerae, hauptsaechlich durch verunreinigtes Trinkwasser",
        answerC = "Ein Parasit, der durch den Biss von Muecken uebertragen wird",
        answerD = "Ein Pilz, der durch Lebensmittel wie rohes Fleisch aufgenommen wird",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Cholera wird durch das Bakterium Vibrio cholerae ausgeloest, das hauptsaechlich durch verunreinigtes Wasser und Lebensmittel aufgenommen wird -- es verursacht schwere Durchfaelle mit Risiko der Austrocknung.",
        funFact = "In Deutschland gab es im 19. Jahrhundert mehrere Cholera-Wellen -- der Hamburger Cholera-Ausbruch von 1892 toedte ueber 8.000 Menschen und beschleunigte den Bau eines modernen Wasserleitungssystems."
    )

)
