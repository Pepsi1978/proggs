package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsExpert3(): List<Question> = listOf(

    // --- LEISTUNGSDIAGNOSTIK / TESTVERFAHREN (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Der Wingate-Anaerob-Test misst anaerobe Spitzenleistung und anaerobe Kapazitaet. Welcher Widerstand wird standardmaessig beim klassischen 30-Sekunden-Protokoll pro Kilogramm Koerpergewicht eingestellt?",
        answerA = "0,065 kp/kg KG",
        answerB = "0,075 kp/kg KG",
        answerC = "0,085 kp/kg KG",
        answerD = "0,095 kp/kg KG",
        correctAnswer = 1,
        explanation = "Der Standardwiderstand beim Wingate-Test betraegt 0,075 kp pro kg Koerpergewicht (Frauen: 0,065 kp/kg). Der Fatigue-Index beschreibt den Leistungsabfall in Prozent ueber die 30 Sekunden.",
        difficulty = 4,
        funFact = "Der Wingate-Test wurde Ende der 1970er Jahre am Wingate Institute in Israel entwickelt und ist bis heute der meistgenutzte Test fuer anaerobe Leistungsdiagnostik."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Ventilationsschwelle wird in der Spiroergometrie als 'VT2' (zweite ventilatorische Schwelle) bezeichnet und entspricht physiologisch dem Anstieg welchen Parameters?",
        answerA = "Anstieg des VCO2/VO2-Quotienten ueber 1,0",
        answerB = "Anstieg des Atemzugvolumens ohne weiteren Frequenzanstieg",
        answerC = "Nichtlinearer Anstieg der VE/VCO2-Kurve nach vorherigem Minimum",
        answerD = "Verdopplung der Atemfrequenz gegenueber Ruhewert",
        correctAnswer = 2,
        explanation = "VT2 (respiratory compensation point) ist durch den erneuten nichtlinearen Anstieg des VE/VCO2-Verhaeltnisses nach dessen Minimum gekennzeichnet und entspricht dem Beginn der vollstaendigen metabolischen Azidose-Kompensation.",
        difficulty = 4,
        funFact = "Die Identifikation beider Ventilationsschwellen (VT1 und VT2) erlaubt eine praezise Trainingszonen-Steuerung ohne invasive Laktatmessungen."
    ),

    Question(
        categoryId = 6,
        questionText = "Beim Countermovement Jump (CMJ) im Kraftdiagnostik-Labor wird die 'reactive strength index modified' (RSImod) berechnet. Aus welchen beiden Parametern setzt sich dieser Index zusammen?",
        answerA = "Sprunghöhe dividiert durch Kontaktzeit",
        answerB = "Sprunghöhe dividiert durch Bewegungszeit (time to takeoff)",
        answerC = "Spitzenkraft dividiert durch Koerpergewicht",
        answerD = "Impuls dividiert durch Kontaktdauer",
        correctAnswer = 1,
        explanation = "Der RSImod (reactive strength index modified) = Sprunghöhe / Bewegungszeit (time to takeoff). Dieser Index bewertet, wie effizient ein Athlet Kraft im Zeitfenster des Absprungs entwickelt.",
        difficulty = 4,
        funFact = "Der klassische RSI (Sprunghöhe / Kontaktzeit) wird beim Drop Jump gemessen, waehrend der RSImod speziell fuer den CMJ entwickelt wurde, der keine definierte Kontaktzeit hat."
    ),

    Question(
        categoryId = 6,
        questionText = "Die isokinetische Kraftdiagnostik misst das Drehmoment bei konstanter Winkelgeschwindigkeit. Welches Koeffizient beschreibt das Verhaeltnis von exzentrischer zu konzentrischer Kraft und wird als 'Exzentrik-Konzentrik-Ratio' bezeichnet?",
        answerA = "Hamstrings-Quadrizeps-Ratio (H:Q-Ratio konventionell)",
        answerB = "Funktionelle H:Q-Ratio (exzentrische Hamstrings zu konzentrischen Quadrizeps)",
        answerC = "Bilateral-Defizit-Index",
        answerD = "Koexzentrizitaets-Index nach Dvir",
        correctAnswer = 1,
        explanation = "Die funktionelle H:Q-Ratio vergleicht die exzentrischen Hamstrings (Bremsmoment bei Knieextension) mit dem konzentrischen Quadrizeps (Beschleunigungsmoment) und ist ein wichtiger Marker fuer Verletzungsrisiko im Knie.",
        difficulty = 4,
        funFact = "Eine funktionelle H:Q-Ratio unter 0,6 gilt als Risikofaktor fuer Kreuzband- und Hamstring-Verletzungen im Profifussball."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Bestandteil der 'Physical Performance Battery' (PPB) nach Guralnik et al. wird als unabhaengiger Praediktor fuer Mortalitaet bei aelteren Erwachsenen angesehen?",
        answerA = "400-Meter-Gehtest",
        answerB = "5-mal-Aufstehen-aus-dem-Stuhl-Test",
        answerC = "Einbein-Standzeit",
        answerD = "Short Physical Performance Battery (SPPB-Gesamtscore)",
        correctAnswer = 3,
        explanation = "Der SPPB-Gesamtscore (aus Gleichgewichtstests, 4-m-Gehgeschwindigkeit und Stuhlaufstehtest) hat sich in zahlreichen prospektiven Kohortenstudien als starker unabhaengiger Praediktor fuer Mortalitaet und Heimeinweisung bewaehrt.",
        difficulty = 4,
        funFact = "Bereits eine Reduktion des SPPB-Scores um einen Punkt erhoehte in Langzeitstudien das Mortalitaetsrisiko um 15-20 Prozent."
    ),

    Question(
        categoryId = 6,
        questionText = "In der Leistungsdiagnostik des Ruderns wird der '2000-Meter-Ergometertest' als Standardtest genutzt. Welcher Parameter korreliert am staerksten mit der 2000-m-Wettkampfleistung im Rudern?",
        answerA = "Maximale Sauerstoffaufnahme (VO2max) absolut in L/min",
        answerB = "Laktat-Leistungs-Schwelle bei 4 mmol/L",
        answerC = "Maximale Wattleistung relativ zum Koerpergewicht",
        answerD = "Mittlere Wattleistung der letzten 500 Meter",
        correctAnswer = 0,
        explanation = "Die absolute VO2max (L/min, nicht relativ) korreliert am staerksten mit der Ruder-Ergometerleistung ueber 2000 Meter, da Rudern eine absolute Leistungssportart ist, bei der groessere Athleten einen Vorteil haben.",
        difficulty = 4,
        funFact = "Elite-Ruderer erreichen VO2max-Werte von 6-7 L/min (absolut) und gehoeren damit zu den Sportlern mit der weltweit hoechsten gemessenen Sauerstoffaufnahme."
    ),

    Question(
        categoryId = 6,
        questionText = "Der 'Yo-Yo Intermittent Recovery Test Level 2' (YYIR2) misst vorwiegend welche Faehigkeit und unterscheidet sich vom YYIR1 durch welches Merkmal?",
        answerA = "Aerobe Ausdauer; kuerzere Erholungspausen von 5 Sekunden statt 10 Sekunden",
        answerB = "Anaerobe Kapazitaet; hoehere Startgeschwindigkeit und 10-s-Erholungspausen",
        answerC = "Wiederholte Sprintfaehigkeit; Erholungspausen von 15 Sekunden und maximale Sprintintensitaet",
        answerD = "Laktattoleranzvermögen; unterbrochene Laufphasen mit variablen Distanzen",
        correctAnswer = 1,
        explanation = "Der YYIR2 beginnt bei 13 km/h (YYIR1: 10 km/h) und hat ebenfalls 10-Sekunden-Erholungspausen. Er misst primaer die Faehigkeit, anaerob-intensive Belastungen zu wiederholen, und ist sensibler fuer Leistungsaenderungen bei Hochtrainierten.",
        difficulty = 4,
        funFact = "Jens Bangsbo entwickelte die Yo-Yo-Tests in den 1990er Jahren als felddianostisches Werkzeug, das inzwischen von der FIFA fuer weltweite Spielerstudien eingesetzt wird."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher biochemische Marker wird in der akuten sportlichen Leistungsdiagnostik als 'Muscle Damage Index' eingesetzt und reflektiert die Sarkomerschaedigung nach exzentrischer Belastung am genauesten?",
        answerA = "Kreatinkinase (CK-MM Isoform) im Serum",
        answerB = "Myoglobin im Urin",
        answerC = "Troponin I skelettmuskulaer (sTnI)",
        answerD = "Laktatdehydrogenase Isoform 5 (LDH-5)",
        correctAnswer = 2,
        explanation = "Skelettales Troponin I (sTnI) ist hochspezifisch fuer Skelettmuskelschaeden, hat eine laengere Halbwertszeit als Myoglobin und einen engeren zeitlichen Verlauf als CK, was es zum praezisesten Marker fuer Sarkomerschaeden nach exzentrischer Belastung macht.",
        difficulty = 4,
        funFact = "Die Erforschung von sTnI als Muskelschadensmarker ist relativ jung — kardiales Troponin (cTnI) ist seit Jahrzehnten bekannt, sein skelettales Gegenstueck erst seit den 2000ern gezielt untersucht."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Force-Velocity-Profiling' nach Pierre Samozino erlaubt die individuelle Optimierung von Kraft-Geschwindigkeits-Profilen im Sprint. Welcher Parameter beschreibt die Abweichung des gemessenen Profils vom theoretischen Optimum?",
        answerA = "Mechanische Effizienz (ME-Index)",
        answerB = "Optimale Kraft-Geschwindigkeits-Slope (Sfv_opt)",
        answerC = "Deficit-Index (DI) nach Bosco",
        answerD = "Acceleration-Force-Ratio (AFR)",
        correctAnswer = 1,
        explanation = "Das Force-Velocity-Profiling definiert eine theoretisch optimale Slope (Sfv_opt) der F-v-Beziehung. Die Abweichung (DFv) des individuellen Profils vom Optimum zeigt, ob ein Athlet eher Kraft- oder Geschwindigkeitstraining benoetigt.",
        difficulty = 4,
        funFact = "Samozinos Methode erlaubt es, aus einfachen Sprintzeiten auf 10-40 Meter das vollstaendige Kraft-Geschwindigkeitsprofil ohne Labor zu berechnen."
    ),

    Question(
        categoryId = 6,
        questionText = "In der sportlichen Leistungsdiagnostik bezeichnet 'Heart Rate Variability Threshold' (HRVt) welchen physiologischen Schwellenwert?",
        answerA = "Die Intensitaet, bei der die HRV auf Null sinkt und maximale Sympathikusaktivierung eintritt",
        answerB = "Den Intensitaetspunkt, an dem die hochfrequente HRV-Komponente (HF) signifikant abnimmt und die LF/HF-Ratio steigt",
        answerC = "Die Herzfrequenz, bei der die RMSSD-Werte die individuell anaerobe Schwelle anzeigen",
        answerD = "Die Ruheherzfrequenzvariabilitaet als Baseline fuer Uebertrainingsdetektion",
        correctAnswer = 2,
        explanation = "Der HRVt korrespondiert mit der aeroben Schwelle (VT1/LT1). An diesem Punkt beginnt der parasympathische Einfluss (RMSSD, HF-Power) zu sinken, was eine nichtinvasive Schwellenbestimmung ermoeglicht.",
        difficulty = 4,
        funFact = "Studien zeigen eine Uebereinstimmung von HRVt und ventilatorischer Schwelle VT1 von 90-95%, was HRV-basierte Schwellendiagnostik als valide Alternative zu Laktat- und Spiroergometrie positioniert."
    ),

    // --- SPORTGENOMIK / EPIGENETIK (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Das ACTN3-Gen kodiert das Protein Alpha-Actinin-3, das ausschliesslich in schnellen Typ-II-Muskelfasern exprimiert wird. Welches Allel des R577X-Polymorphismus (rs1815739) ist bei Elite-Sprintern signifikant haeufiger?",
        answerA = "XX-Genotyp (Homozygot fuer das Nullallel)",
        answerB = "RX-Genotyp (Heterozygot)",
        answerC = "RR-Genotyp (Homozygot fuer das R-Allel)",
        answerD = "XR-Genotyp (seltene Variante)",
        correctAnswer = 2,
        explanation = "Das R-Allel (Arginin, funktionales Alpha-Actinin-3) ist bei Elite-Sprintern signifikant haeufiger als in der Allgemeinbevoelkerung. Der XX-Genotyp (kein funktionales Alpha-Actinin-3) findet sich dagegen gehaueft bei Elite-Ausdauersportlern.",
        difficulty = 4,
        funFact = "Rund 18% der Weltbevoelkerung sind XX-homozygot und produzieren kein Alpha-Actinin-3 — ohne sichtbare Einschraenkung im Alltag, aber mit Nachteil im Hochleistungssprint."
    ),

    Question(
        categoryId = 6,
        questionText = "Epigenetische DNA-Methylierung am 'Exercise-Induced Enhancer' des PGC-1alpha-Gens (PPARGC1A) reagiert auf akutes Ausdauertraining. In welche Richtung veraendert sich die Methylierung nach einer Einheit intensiven Trainings?",
        answerA = "Hypermethylierung am Promotor fuehrt zu reduzierter PGC-1alpha-Expression",
        answerB = "Demethylierung am Enhancer erleichtert Transkriptionsfaktor-Bindung und erhoehte Expression",
        answerC = "Keinerlei akute epigenetische Veraenderungen; nur chronische Adaptation nach Wochen",
        answerD = "Methylierung wechselt zwischen Promotor und Enhancer durch DNA-Looping",
        correctAnswer = 1,
        explanation = "Akutes Ausdauertraining induziert innerhalb von Stunden eine Demethylierung spezifischer CpG-Sites im Enhancer-Bereich von PPARGC1A, was die Transkription von PGC-1alpha erleichtert — ein epigenetischer 'Schalter' fuer mitochondriale Biogenese.",
        difficulty = 4,
        funFact = "Diese akute Demethylierung wurde erstmals 2010 von Barres und Zierath nachgewiesen und revolutionierte das Verstaendnis, wie eine einzelne Trainingseinheit langfristige Anpassungen einleiten kann."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Gen codiert das 'Myostatin'-Protein (GDF-8), und welche Mutation fuehrt zu aussergewoehnlicher Muskelhypertrophie bei Menschen und Tieren?",
        answerA = "MSTN-Gen; Loss-of-function-Mutationen im Exon 3 (Frameshift oder Stoppcodon)",
        answerB = "GDF8-Gen; Gain-of-function-Mutation durch Duplikation von Exon 1",
        answerC = "ACVR2B-Gen; Heterozygote Missense-Mutation im Liganden-Bindungsbereich",
        answerD = "SMAD7-Gen; Promotor-Methylierung fuehrt zu dauerhaft erhoehter Myostatin-Hemmung",
        correctAnswer = 0,
        explanation = "Das MSTN-Gen kodiert Myostatin. Homozygote Loss-of-function-Mutationen (Frameshift, Nonsense) fuehren zu fehlender Myostatin-Inhibition der Muskelhypertrophie — bekannt durch 'Superboy'-Faelle und Rassen wie das Blaue Belgische Rind.",
        difficulty = 4,
        funFact = "2004 wurde ein Kind in Deutschland mit einer MSTN-Homozygot-Mutation beschrieben, das mit 4 Jahren 3 kg schwere Gewichte halten konnte — ein lebender Beweis fuer Myostatins Schluesselrolle."
    ),

    Question(
        categoryId = 6,
        questionText = "Das ACE-Gen-Insertions-/Deletionspolymorphismus (ACE I/D) beeinflusst den Angiotensin-converting-Enzym-Spiegel. Welcher Genotyp ist mit erhöhter aerober Kapazität und Ausdauerleistung assoziiert?",
        answerA = "DD-Genotyp (hoehere ACE-Aktivitaet, hoehere Angiotensin-II-Produktion)",
        answerB = "II-Genotyp (niedrigere ACE-Aktivitaet, weniger vasokonstriktive Wirkung)",
        answerC = "ID-Genotyp (intermediäre ACE-Aktivitaet als Vorteil durch Heterozygotieeffekt)",
        answerD = "Es besteht kein signifikanter Zusammenhang zwischen ACE-Genotyp und Ausdauerleistung",
        correctAnswer = 1,
        explanation = "Der II-Genotyp ist mit niedrigerer ACE-Aktivitaet und geringerer Angiotensin-II-Konzentration assoziiert, was fuer verbesserte Gefaessdilatation, hoehere Kapillarisierung und bessere Sauerstoffversorgung spricht. Meta-Analysen bestaetigen eine Assoziation mit Ausdauerleistung.",
        difficulty = 4,
        funFact = "Montague et al. 1997 zeigten, dass britische Bergsteiger mit II-Genotyp groessere Hoehenerfolge ohne Sauerstoff erzielten — ein fruehes Beispiel sportgenetischer Feldforschung."
    ),

    Question(
        categoryId = 6,
        questionText = "MikroRNAs (miRNAs) werden nach Sportbelastung in den Blutkreislauf freigesetzt ('exercise-responsive miRNAs'). Welche miRNA gilt als 'muscle-enriched' und ist stark reguliert durch Ausdauertraining?",
        answerA = "miR-21 (proinflammatorisch, Makrophagen-assoziiert)",
        answerB = "miR-133a (myogene Differenzierung, Muskelregeneration)",
        answerC = "miR-126 (endothelial, Angiogenese-regulierend)",
        answerD = "miR-206 (Muskel-spezifisch, 'myomiR', Typ-I-Faser-Marker)",
        correctAnswer = 3,
        explanation = "miR-206 ist ein typischer 'myomiR' (muskelspezifische mikroRNA), hochreguliert bei Ausdauertraining und assoziiert mit Typ-I-Faser-Differenzierung und mitochondrialer Biogenese. Sie gilt als vielversprechender nicht-invasiver Biomarker fuer Trainingsadaptation.",
        difficulty = 4,
        funFact = "Die Erforschung zirkulierender miRNAs als Trainingsmarker ist ein aktives Feld — Wissenschaftler hoffen, durch Bluttests ohne Muskelbiopsie Trainingsanpassungen praezise messen zu koennen."
    ),

    Question(
        categoryId = 6,
        questionText = "Histonmodifikationen spielen bei Trainingsadaptation eine Rolle. Welche Histonmodifikation am H3K27-Rest ist primaer mit Genstilllegung (Repression) assoziiert und wird durch Ausdauertraining an Fatigue-Genen modifiziert?",
        answerA = "H3K27-Acetylierung (H3K27ac) durch HATs",
        answerB = "H3K27-Trimethylierung (H3K27me3) durch EZH2 (Polycomb-Komplex)",
        answerC = "H3K27-Phosphorylierung durch CaM-Kinasen",
        answerD = "H3K27-Ubiquitinierung durch RNF2",
        correctAnswer = 1,
        explanation = "H3K27me3 (Trimethylierung von Histon H3 an Lysin 27) ist ein etablierter Marker fuer transkriptionelle Repression. Der Polycomb-Repressive Complex 2 (PRC2/EZH2) setzt diese Markierung und kann durch Trainingsreize modifiziert werden.",
        difficulty = 4,
        funFact = "Durch gezieltes Ausdauertraining werden H3K27me3-Markierungen an Muskelatrophie-Genen (z.B. Atrogin-1/FBXO32) reduziert, was die Muskelerhaltung bei gleichzeitigem Ausdauertraining erklaert."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Telomerlaengen-Paradoxon' im Leistungssport beschreibt den Befund, dass extreme Ausdauersportler im Vergleich zu Moderatsportlern kuerze Telomere aufweisen. Welche Hypothese erklaert dieses Phaenomen?",
        answerA = "Oxidativer Stress durch chronisch hohe Trainingsvolumina beschleunigt telomere Erosion",
        answerB = "Extreme Belastung aktiviert Telomerase, was paradoxerweise zu ungleichmaeßigem Telomerwachstum fuehrt",
        answerC = "Selektion: Athleten mit natuerlich kuerzeren Telomeren haben hoehere Erythropoetin-Sensitivitaet",
        answerD = "Zellulare Seneszenz wird durch Ueberschreitung individueller Belastungsschwellen induziert",
        correctAnswer = 0,
        explanation = "Chronisch hohes Trainingsvolumen erzeugt systemischen oxidativen Stress, der DNA-Schaeden induziert und die Telomerase-Kapazitaet uebersteigt. Moderate Sportler profitieren von Telomerase-Aktivierung ohne ueberschiessenden Oxidationsstress — der sogenannte hormetic sweet spot.",
        difficulty = 4,
        funFact = "Larocca et al. (2010) zeigten, dass moderat aktive Senioren um 200 Basenpaare laengere Telomere hatten als ihre sedentaeren Altersgenossen, waehrend Ultra-Marathonlaeufer aehnliche Werte wie Inaktive aufwiesen."
    ),

    Question(
        categoryId = 6,
        questionText = "Transgenerationelle Epigenetik im Sport untersucht, ob Trainingseffekte auf die naechste Generation vererbt werden koennen. Ueber welchen Mechanismus wird diese Weitergabe primaer diskutiert?",
        answerA = "Uebertragung von Trainings-mRNA durch Spermien-RNA (Sperm RNA inheritance)",
        answerB = "Direkte DNA-Sequenzaenderung durch trainingsinduzierte Mutagenese",
        answerC = "Maternale Mitochondrien-Selektion durch sportliche Aktivitaet",
        answerD = "Postnatale Praeimprinting-Effekte durch haematopoetische Stammzellen",
        correctAnswer = 0,
        explanation = "Aktuelle Forschung zeigt, dass Spermien-RNA (kleine nicht-kodierende RNAs, tRNAs, miRNAs) epigenetische Informationen aus dem Stoffwechsel des Vaters tragen und transgenerationelle Effekte vermitteln koennen — ein revolutionaeres Konzept der epigenetischen Vererbung.",
        difficulty = 4,
        funFact = "Mourier et al. und spaeter Bhatt et al. zeigten in Mausmodellen, dass die Nachkommen trainierter Vaeter verbesserte Glukosetoleranz und erhoehte Ausdauerleistung aufwiesen — ohne selbst trainiert zu haben."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Athlete's Genome' Projekt (u.a. durch Sports Genomics Society koordiniert) identifiziert sportleistungsrelevante SNPs. Welches Gen zeigt den staerksten dokumentierten Einfluss auf VO2max-Trainierbarkeit (coachability)?",
        answerA = "NRF2 (NFE2L2) als Master-Regulator oxidativer Stressantwort",
        answerB = "PPARA (Peroxisom-Proliferator-aktivierter Rezeptor Alpha) mit dem rs4253778-Polymorphismus",
        answerC = "VEGF (vaskulaerer endothelialer Wachstumsfaktor) mit dem +936C/T-Polymorphismus",
        answerD = "EPOR (Erythropoetin-Rezeptor) mit Gain-of-function-Variante",
        correctAnswer = 1,
        explanation = "PPARA rs4253778 (G-Allel) ist in der HERITAGE Family Study als einer der staerksten Modulatoren der individuellen VO2max-Reaktion auf standardisiertes Ausdauertraining identifiziert worden. PPARA reguliert Fettsaeurenoxidation und mitochondriale Biogenese.",
        difficulty = 4,
        funFact = "Die HERITAGE Family Study (1990er Jahre) war die erste grosse Familienstudie, die zeigte, dass die VO2max-Trainierbarkeit zu 47% genetisch determiniert ist — mit enormer individueller Variabilitaet."
    ),

    Question(
        categoryId = 6,
        questionText = "Zirkulierende 'cell-free DNA' (cfDNA) steigt nach intensivem Sport an. Welcher Mechanismus gilt als primaere Quelle dieser trainings-induzierten cfDNA?",
        answerA = "Nekrotischer Zelltod belasteter Muskelzellen mit Freisetzung genomischer DNA",
        answerB = "NETosis (Neutrophil Extracellular Traps) als Antwort auf systemische Inflammation",
        answerC = "Apoptotische Fragmentierung von Leukozyten durch Cortisol-Anstieg",
        answerD = "Aktive Exosom-Sekretion durch kontrahierende Myozyten",
        correctAnswer = 2,
        explanation = "Kortisol-induzierte Lymphozytenapoptose und nachfolgende DNA-Fragmentierung gilt als Hauptquelle trainings-induzierter cfDNA. Intensive Belastungen erhoehen cfDNA um das 5-10-fache des Ruhewertes innerhalb von 30 Minuten.",
        difficulty = 4,
        funFact = "cfDNA wird als potenzielle Dopingkontrolle erforscht: Bestimmte Gendosierungsprofile in der cfDNA koennen Hinweise auf Gentransfer-Doping liefern."
    ),

    // --- SPORTMANAGEMENT / LIGA-OEKONOMIE (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Das 'Fort-Belot-Model' in der Sportoekonometrie beschreibt die optimale Wettbewerbsintensitaet in Sportligen. Welches Konzept steht im Zentrum dieses Modells?",
        answerA = "Maximale Umsatzsteigerung durch Monopolisierung der starksten Teams",
        answerB = "Optimale Competitive Balance als Gleichgewicht zwischen Spannung und Qualitaet",
        answerC = "Revenue Sharing als einziges Instrument zur Talentnivellierung",
        answerD = "Gate-Receipt-Maximierung durch geographische Monopole pro Liga",
        correctAnswer = 1,
        explanation = "Das Fort-Belot-Model zeigt, dass Zuschauerinteresse und damit Ligaumsaetze von der Competitive Balance (Ausgeglichenheit) abhaengen. Zu viel Dominanz einzelner Teams senkt Spannung und Einnahmen — die optimale Balance maximiert Gesamteinnahmen.",
        difficulty = 4,
        funFact = "Paradoxerweise profitieren dominante Teams (Real Madrid, FC Bayern) vom Ligasystem, da spannungsarme Ligen langfristig schrumpfende TV-Einnahmen erzeugen, von denen alle Klubs abhaengen."
    ),

    Question(
        categoryId = 6,
        questionText = "Der UEFA Financial Fair Play Regulations (FFP, seit 2011) verwendete den 'Relevant Expense'-Begriff. Welche Ausgabenkategorie war explizit NICHT in den Relevant Expenses enthalten und durfte unbegrenzt aus Eigenkapital finanziert werden?",
        answerA = "Spielergehaelter und Spielertransfers",
        answerB = "Infrastrukturinvestitionen (Stadionbau, Nachwuchsakademie, Frauenfussball)",
        answerC = "Beraterhonorare und Agentenprovisionenn",
        answerD = "Rueckzahlung von Altschulden aus der Zeit vor 2010",
        correctAnswer = 1,
        explanation = "Infrastrukturinvestitionen (Stadion, Akademie, Frauenfussball, Community-Projekte) wurden explizit aus den 'Relevant Expenses' ausgeklammert, da die UEFA Langzeitinvestitionen foerdern wollte. Diese Lucke nutzten viele Klubs fuer bilanzoptimierte Ausgabengestaltung.",
        difficulty = 4,
        funFact = "Die neuen UEFA-Regeln seit 2023 ('Financial Sustainability Regulations') ersetzten FFP und setzten eine strikte Football Earnings Rule (FER) ein, die auch Infrastruktur staerker limitiert."
    ),

    Question(
        categoryId = 6,
        questionText = "In der Ligaoekonometrie beschreibt der 'Uncertainty of Outcome Hypothesis' (UOH) von Rottenberg (1956) die Nachfragefunktion von Sportveranstaltungen. Welche empirische Evidenz hat diese Hypothese in den letzten Jahrzehnten mehrheitlich gefunden?",
        answerA = "Volle Bestaetigung: Ausgewogene Spiele erzeugen konsistent hoehere Zuschauerzahlen",
        answerB = "Gemischte Evidenz: Lokal-Dominanz (Heimteam-Staerke) erhoehte oft Zuschauerzahlen mehr als Balance",
        answerC = "Vollstaendige Widerlegung: Zuschauer bevorzugen dominante Heimteams bei allen Spielen",
        answerD = "Zeitabhaengige Evidenz: UOH gilt fuer Europa, nicht fuer amerikanische Sportligen",
        correctAnswer = 1,
        explanation = "Die empirische Evidenz zur UOH ist gemischt. Viele Studien zeigen, dass starke Heimteams (Lokalpatriotismus) die Nachfrage staerker treiben als Spielausgeglichenheit. Fans kommen bevorzugt, wenn ihr Team wahrscheinlich gewinnt — das widerspricht der reinen UOH.",
        difficulty = 4,
        funFact = "Szymanski (2003) und Pawlowski & Anders (2012) zeigten fuer die Bundesliga, dass Lokal-Favoriten-Effekte die UOH deutlich ueberwiegen — Spannung allein fuellt keine Stadien."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Moneyball'-Konzept (Billy Beane, Oakland Athletics) revolutionierte die Spielerrekrutierung durch Sabermetrics. Welche Statistik war dabei am meisten unterbewertet und bildete den Kern der neuen Bewertungsmethode?",
        answerA = "Schlagdurchschnitt (Batting Average, BA)",
        answerB = "On-Base Percentage (OBP) inkl. Walks und Hit-by-Pitch",
        answerC = "Slugging Percentage (SLG) als Mass fuer Schlagstaerke",
        answerD = "Runs Batted In (RBI) als situative Effizienz",
        correctAnswer = 1,
        explanation = "On-Base Percentage (OBP) war am Markt massiv unterbewertet. Beane erkannte, dass OBP Runs besser vorhersagt als BA, weil jeder erreichte Base (auch durch Walk) gleichwertig ist. Spieler mit hoher OBP wurden fuer deutlich weniger Geld verpflichtet.",
        difficulty = 4,
        funFact = "Die Oakland Athletics kamen 2002 mit dem kleinsten Budget der AL in den Playoffs und gewannen 20 Spiele in Folge — Michael Lewis' Buch 'Moneyball' (2003) und der Film (2011) machten das Konzept weltbekannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Im professionellen Teamsport existiert das Phaenomen des 'superstar externality'. Was beschreibt diesen oekonomischen Effekt im Sportkontext?",
        answerA = "Superstars erhalten Gehaelter weit ueber ihrem Grenzprodukt, was andere Teamgehaelter drueckt",
        answerB = "Die Anwesenheit eines Superstars erhoehte Ticketeinnahmen, TV-Ratings und Merchandising fuer den gesamten Ligabetrieb (nicht nur sein Team)",
        answerC = "Superstars generieren negative Externalitaeten durch Teamkonflikte und reduzierte Teamkohesion",
        answerD = "Hohe Superstar-Gehaelter fuehren zu Inflationseffekten bei allen Spielergehaeltern in der Liga",
        correctAnswer = 1,
        explanation = "Die 'superstar externality' beschreibt positive Spill-over-Effekte: Wenn LeBron James oder Cristiano Ronaldo in einer Liga spielen, profitieren ALLE Teams durch hoehere TV-Einnahmen, gestiegenes Interesse und Merchandising — ein positiver externer Effekt.",
        difficulty = 4,
        funFact = "Noll (1974) und spaeter Hausman & Leonard (1997) quantifizierten den Michael-Jordan-Effekt: Jordan erhoehte allein die NBA-TV-Einnahmen geschaetzt um 50-100 Millionen Dollar pro Saison durch gesteigertes Gesamtinteresse."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Salary Cap'-System in nordamerikanischen Profiligen (NFL, NBA, NHL) verwendet verschiedene Designs. Welcher Typ von Salary Cap ermoeglicht Klubs Ueberschreitungen gegen Strafzahlungen ('Luxury Tax')?",
        answerA = "Hard Cap (absolutes Maximum ohne Ausnahmen)",
        answerB = "Soft Cap mit Luxury Tax (NBA-Modell seit 1984)",
        answerC = "Revenue-Share Cap mit automatischen Ausschluss-Regeln",
        answerD = "Franchise Tag Cap (NFL-Modell fuer Einzelspieler)",
        correctAnswer = 1,
        explanation = "Der NBA-Soft Cap erlaubt Klubs, ueber das Cap-Limit zu gehen, wenn sie Eigengewaechse re-signen ('Bird Rights') oder andere Ausnahmen nutzen. Bei Ueberschreitung des Luxury Tax Threshold werden gestaffelte Steuern faellig — die NBA zahlt diese an alle Teams aus.",
        difficulty = 4,
        funFact = "Die Golden State Warriors zahlten in der Saison 2018/19 allein an Luxury Tax-Zahlungen 170 Millionen Dollar — fast das 2,5-fache des gesamten Salary Caps vieler kleinerer Teams."
    ),

    Question(
        categoryId = 6,
        questionText = "Im Bereich Sportsponsoring beschreibt das Konzept des 'Ambush Marketing' eine spezifische Strategie. Welche Form gilt als 'direkte Ambush'-Strategie im Gegensatz zu indirektem Ambush?",
        answerA = "Werbung im Umfeld eines Events ohne offizielle Sponsorenrechte durch Medienpraesenz",
        answerB = "Falsche Nutzung offizieller Event-Logos und geschuetzter Bezeichnungen",
        answerC = "Sponsoring eines Athleten, waehrend ein Konkurrent offizieller Teamsponsor ist",
        answerD = "Social-Media-Kampagnen mit Hashtags waehrend eines Events ohne Nennung des Veranstalters",
        correctAnswer = 1,
        explanation = "Direktes Ambush Marketing umfasst die unbefugte Nutzung geschuetzter Markenkennzeichen, Logos oder Bezeichnungen (z.B. 'Olympia', '2024 Paris Games') ohne Sponsoring-Rechte — rechtlich verfolgt. Indirekte Methoden (Medienpraesenz, Athletensponsoring) sind hingegen legal.",
        difficulty = 4,
        funFact = "Fuer London 2012 erliess Grossbritannien ein spezielles Olympic Games Act, das sogar Wortkombinationen wie 'London 2012' oder 'Gold Medal Summer' ohne IOC-Genehmigung verbot."
    ),

    Question(
        categoryId = 6,
        questionText = "Die 'invariance principle' in der Sportoekonometrie (Coase-Theorem im Sport, nach Rottenberg 1956) besagt, dass Spielertransfers und Talentallokkation unabhaengig von der Eigentuemerschaft der Spielerrechte effizient sind. Unter welcher Bedingung gilt dieses Theorem?",
        answerA = "Nur bei vollstaendigem Informationszugang aller Marktteilnehmer",
        answerB = "Bei vernachlaessigbaren Transaktionskosten und klaren Eigentumsrechten",
        answerC = "Ausschliesslich in Ligen mit Revenue-Sharing-Systemen",
        answerD = "Wenn die Liga eine zentrale Spielervermittlungsbehoerde betreibt",
        correctAnswer = 1,
        explanation = "Das Coase-Theorem (und Rottenbergs Invarianz-Prinzip im Sport) gilt unter der Bedingung vernachlaessigbarer Transaktionskosten: Wenn Verhandlungen kostenlos sind, fuehrt jede anfaengliche Eigentumsstruktur zu effizienter Allokation durch Tausch.",
        difficulty = 4,
        funFact = "Rottenbergs Papier von 1956 ('The Baseball Players' Labor Market') gilt als Gruendungsdokument der Sportoekonometrie und erschien 40 Jahre vor Michael Lewis' Popularisierung durch Moneyball."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches oekonomische Phaenomen erklaert, warum Sportklubs mit hoeherem Publikum (Fanbase-Groesse) strukturell hoehere Spielergehaelter zahlen koennen, ohne zwangslaeufig hoehere Gewinne zu erzielen?",
        answerA = "Economies of Scale durch groessere TV-Vertraege und Merchandising-Einnahmen",
        answerB = "Ricardianische Rente: Differenzierte Nachfrage ermoeglicht Preisspielraum",
        answerC = "Das Prinzip des 'Winning Maximization' statt 'Profit Maximization' in europaeischen Klubs",
        answerD = "Der 'Big-Market Advantage' durch geographisches Einzugsgebiet und lokale Steuervorteile",
        correctAnswer = 2,
        explanation = "Sloane (1971) beschrieb europaeische Sportklubs als 'Utility-Maximizer' statt 'Profit-Maximizer'. Grosse Klubs streben nach sportlichem Erfolg ('winning maximization'), was zu Gehaltseskalation fuehrt, ohne dass Gewinne entsprechend steigen — erklaert chronische Verluste vieler Topklubs.",
        difficulty = 4,
        funFact = "Deloitte's Football Money League zeigt regelmaessig, dass die umsatzstaerksten Klubs (Real Madrid, Barcelona, Bayern) trotz Milliardeneinnahmen nur moderate Gewinne oder gar Verluste schreiben — Winning Maximization in Aktion."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Designated Player Rule' (MLS, 'Beckham Rule') erlaubt MLS-Teams, bis zu drei Spieler ausserhalb des Salary Cap zu verpflichten. Welchen Betrag muss jeder Klub fuer Designated Player pauschal im Cap anrechnen (Stand Regelwerk 2023)?",
        answerA = "612.500 USD pro Designated Player",
        answerB = "683.750 USD pro Designated Player",
        answerC = "750.000 USD pro Designated Player",
        answerD = "1.000.000 USD pro Designated Player",
        correctAnswer = 1,
        explanation = "MLS-Teams muessen 683.750 USD (Stand: 2023 Threshold) pro Designated Player als Cap Hit anrechnen, unabhaengig vom tatsaechlichen Gehalt. Alles ueber diesem Betrag wird vom Klub selbst (oder durch Allocation Money) bezahlt.",
        difficulty = 4,
        funFact = "Die Regel wurde 2007 speziell fuer David Beckhams LA-Galaxy-Wechsel eingefuehrt — daher der Volksmund-Name 'Beckham Rule'. Seitdem kamen Drogba, Henry, Pirlo, Ibrahimovic und Messi in die MLS."
    ),

    // --- SPORTTECHNOLOGIE / MATERIALWISSENSCHAFT (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Carbon-Fiber-Reinforced Polymer (CFRP) Laufprothesen (z.B. Ossur Flex-Foot Cheetah) speichern und geben Energie bei jedem Schritt zurueck. Welcher Wirkungsgrad der Energierueckgabe (Energy Return) wurde fuer moderne Blade-Prothesen experimentell gemessen?",
        answerA = "45-55% des gespeicherten Energieimpulses werden zurueckgegeben",
        answerB = "62-72% des gespeicherten Energieimpulses werden zurueckgegeben",
        answerC = "80-92% des gespeicherten Energieimpulses werden zurueckgegeben",
        answerD = "Nahezu 100% Energierueckgabe durch optimale CFRP-Kompositstruktur",
        correctAnswer = 2,
        explanation = "Moderne Energy-Return-Prothesen erreichen 80-92% Energierueckgabe (verglichen mit ca. 60% beim menschlichen Fuss-Feder-Mechanismus), da CFRP nahezu keine Daempfung hat. Dies erklaert die kontroverse Diskussion ueber Wettbewerbsvorteile im Parasport.",
        difficulty = 4,
        funFact = "Oscar Pistorius' CAS-Fall (2008) war der erste, bei dem biomechanische Energierueckgabe-Daten als Beweis fuer oder gegen einen Wettkampfvorteil vor einem Sportschiedsgericht praezentiert wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei Schwimmanzuegen aus Polyurethan (verboten ab 2010 durch FINA, jetzt World Aquatics) wurden Weltrekorde massenhaft in der 'Tech Suit Era' (2008-2009) gebrochen. Welcher physikalische Effekt erklaerte hauptsaechlich die Leistungsverbesserung?",
        answerA = "Reduzierter Wellenwiderstand durch hydrophobe Oberflaeche und Kanalstruktur",
        answerB = "Kompression des Koerpers mit verbesserter Koerperlage und reduziertem frontalem Querschnitt",
        answerC = "Eingeschlossene Luftmolekuleschicht reduzierte Reibungswiderstand (air lubrication)",
        answerD = "Erhoeher Auftrieb durch geschlossenzelliger Schaum als Schwimmhilfe",
        correctAnswer = 3,
        explanation = "Polyurethan-Anzuege (wie Speedo LZR Racer, Arena X-Glide) enthielten geschlossenzelligen Schaum (open- und closed-cell foam), der zusaetzlichen Auftrieb erzeugte und den Koerper angehobener und waagerechter im Wasser hielt — eine verdeckte hydrodynamische Hilfe.",
        difficulty = 4,
        funFact = "Bei den Olympischen Spielen 2008 in Peking wurden 23 von 25 Schwimm-Weltrekorden in Polyurethan-Anzuegen aufgestellt. FINA verbot diese Anzuege rueckwirkend ab 1. Januar 2010."
    ),

    Question(
        categoryId = 6,
        questionText = "Im Radsport beschreibt der Begriff 'aero drag coefficient times frontal area' (CdA) den aerodynamischen Widerstand. Welche Einheit hat CdA und welcher typische Wert gilt fuer einen Zeitfahrspezialisten in optimaler Position?",
        answerA = "CdA in m2; typischer Wert 0,45-0,55 m2 fuer Zeitfahrer",
        answerB = "CdA in m2; typischer Wert 0,18-0,22 m2 fuer Zeitfahrer in Aero-Position",
        answerC = "CdA dimensionslos; Wert zwischen 0,7-0,9 je nach Koerperhaltung",
        answerD = "CdA in N/m2; abhaengig von Luftdichte und Fahrgeschwindigkeit",
        correctAnswer = 1,
        explanation = "CdA (Widerstandskoeffizient x Stirnflaeche) hat die Einheit m2. Elite-Zeitfahrer erreichen in optimaler Aero-Position 0,18-0,22 m2, verglichen mit 0,30-0,40 m2 im aufrechten Strassenradfahrstil. Jede Reduktion um 0,01 m2 spart mehrere Watt Widerstand.",
        difficulty = 4,
        funFact = "Chris Boardmans Stundenweltrekord 1996 wurde auf einer Hightech-Velodrome in der Hinterradverkleidung ermittelt. Moderne Velodrom-Radsportler erreichen CdA-Werte unter 0,17 m2 durch Windkanaloptimierung."
    ),

    Question(
        categoryId = 6,
        questionText = "Visco-elastische Polymere werden in sportlichen Schutzausruestungen (Helm-Liner, Polster) eingesetzt. Das Konzept der 'rate-dependent stiffness' erklaert ihre Schutzwirkung. Wie verhaelt sich die Steifigkeit bei schnellen Impacts?",
        answerA = "Bei schnellen Impacts werden visco-elastische Materialien weicher und absorbieren mehr Energie durch Viskositaet",
        answerB = "Bei schnellen Impacts werden sie steifer, verteilen Kraft ueber groessere Flaeche und reduzieren Spitzenbelastung",
        answerC = "Die Steifigkeit bleibt bei allen Aufprallgeschwindigkeiten konstant (newtonisches Verhalten)",
        answerD = "Bei schnellen Impacts versagt das Material plastisch und deformiert permanent als Energieabsorber",
        correctAnswer = 1,
        explanation = "Visco-elastische Materialien (z.B. D3O, EPP-Schaum) werden bei schnellen Impacts steifer (strain-rate stiffening). Dies verteilt die Aufprallkraft auf eine groessere Kontaktflaeche und verlaengert die Aufprallzeit, was beide Faktoren die Spitzenbe-schleunigung am Kopf reduzieren.",
        difficulty = 4,
        funFact = "D3O, entwickelt in den 2000er Jahren, wird in militaerischen Schutzwesten, Skihelmen und sogar Smartphone-Huellen eingesetzt und vereint Flexibilitaet bei normaler Handhabung mit Haerte im Aufprallmoment."
    ),

    Question(
        categoryId = 6,
        questionText = "In der Fussball-Ballmaterialkunde beschreibt der 'Knuckling Effect' (Flattern) das unvorhersehbare Flugverhalten unbespinnerter Baelle. Welcher physikalische Mechanismus erzeugt dieses Phaenomen?",
        answerA = "Magnus-Effekt durch minimalen Eigendrall und asymmetrische Grenzschichttrennung",
        answerB = "Periodischer Wechsel zwischen laminarer und turbulenter Umstroemung durch Paneelnaehte",
        answerC = "Karman-Wirbelstrasse durch Stroemungsabriss an regelmaessigen Abstaenden",
        answerD = "Bernoulli-Effekt durch Druckunterschied zwischen Ober- und Unterseite beim Flug",
        correctAnswer = 1,
        explanation = "Der Knuckling Effect entsteht durch den periodischen Wechsel zwischen laminarer und turbulenter Grenzschicht an den Nahtstrukturen des Balles. Beim Jabulani (WM 2010) mit wenigen, grossen Paneelen traten die Uebergaenge bei niedrigeren Geschwindigkeiten auf, was das extreme Flattern erklaerte.",
        difficulty = 4,
        funFact = "NASA-Windkanalstudien zum Jabulani (WM 2010) zeigten, dass er bei 30-35 m/s besonders instabil flatterte — genau die Geschwindigkeit typischer Schussversuche aus 20-25 Metern."
    ),

    Question(
        categoryId = 6,
        questionText = "Super-Shoes (Nike Vaporfly, Adidas Adizero Adios Pro) nutzen Carbon-Fiber-Platten in Kombination mit superelastischen Sohlen (PEBA-Schaum). Welcher primaere biomechanische Mechanismus erklaert die gemessene Laufeffizienz-Verbesserung von 4-6%?",
        answerA = "Verlaengerung der Bodenkontaktzeit erhoehter Schubkraft-Akkumulation",
        answerB = "Erhoehte Energierueckgabe aus der PEBA-Sohle plus Steifigkeitsaenderung des Zehengelenks durch Carbonplatte",
        answerC = "Reduktion der Fersenschlagbelastung durch anterograde Belastungsumleitung",
        answerD = "Verbesserter Wadenmuskeleinsatz durch erhoehten Fersenabsatz (Heel-Drop-Effekt)",
        correctAnswer = 1,
        explanation = "PEBA-Schaum (Pebax) gibt bis zu 87% der Aufprallenergie zurueck (vs. 58% bei EVA). Die Carbonplatte reduziert die Energiedissipation im Zehengelenk (Steifigkeitseffekt) und veraendert die Kraftverteilung auf die Wadenmuskulatur. Beide Effekte zusammen erklaeren die gemessene Oekonomie-Verbesserung.",
        difficulty = 4,
        funFact = "Eine Studie von Hoogkamer et al. (2018) im Journal of Sports Sciences messe erstmals die 4,16% Effizienverbesserung der Vaporfly 4% — das Ergebnis war so ueberraschend, dass es eine weltweite Diskussion ueber Technologiedoping ausloeste."
    ),

    Question(
        categoryId = 6,
        questionText = "Sportliche Wurfgeraete (Speere, Diskus) unterliegen strengen IAAF-Spezifikationen. Der Leichtathletik-Speer hat seit 1986 einen veraenderten Schwerpunkt. Welches aerodynamische Problem bei den alten Speeren fuehrte zur Regelaenderung?",
        answerA = "Zu hohe Wurfweiten durch Gleiteigenschaften, die Sicherheitsbereich ueberschritten",
        answerB = "Unvorhersehbares Flattern der alten Speere gefaehrdete Sportler und Zuschauer",
        answerC = "Diskriminierung durch unterschiedliche aerodynamische Eigenschaften bei verschiedenen Herstellern",
        answerD = "Zu geringe internationale Vergleichbarkeit wegen variablem Windeinfluss",
        correctAnswer = 0,
        explanation = "Alte Speere (vor 1986) konnten bei Tailwind aero-dynamisch 'segeln' und Weiten ueber 100 Meter erreichen. Uwe Hohn warf 1984 den Weltrekord von 104,80 m — jenseits der Sicherheitszone jeder normalen Stadionanlage. Die IAAF verschob den Schwerpunkt nach vorne, was Gleitflug verhindert.",
        difficulty = 4,
        funFact = "Uwe Hohns Rekord von 104,80 m ist der einzige Leichtathletik-Weltrekord, der aufgrund einer Regelaenderung geloescht wurde und nie mehr gebrochen werden kann — er steht seither als 'All-Time-Best' im Protokoll."
    ),

    Question(
        categoryId = 6,
        questionText = "In der Sporttechnologie beschreibt 'Load Monitoring' durch GPS und IMU-Sensoren die externe Belastungsquantifizierung. Welche Metrik des GPS-Trackings gilt als sensibelster Praediktor fuer Verletzungsrisiko im Mannschaftssport?",
        answerA = "Gesamtdistanz pro Trainingseinheit (Total Distance, TD)",
        answerB = "Acute:Chronic Workload Ratio (ACWR) aus Wochen-Belastung und 4-Wochen-Rollmittel",
        answerC = "High-Speed Running Distance (HSR, > 19,8 km/h)",
        answerD = "Maximalgeschwindigkeit (Vmax) und Anzahl Sprint-Aktionen",
        correctAnswer = 1,
        explanation = "Die Acute:Chronic Workload Ratio (ACWR) vergleicht die akute Wochenbelastung mit dem chronischen 4-Wochen-Durchschnitt. Eine ACWR > 1,5 gilt als 'danger zone' mit signifikant erhoehtem Verletzungsrisiko — ein zentrales Konzept im modernen GPS-basierten Belastungsmanagement.",
        difficulty = 4,
        funFact = "Tim Gabbett entwickelte das ACWR-Konzept 2016 in einer bahnbrechenden Studie in British Journal of Sports Medicine und veraenderte damit das Verletzungspraevention-Management professioneller Teamsportklubs weltweit."
    ),

    Question(
        categoryId = 6,
        questionText = "Optische Bewegungserfassungssysteme (Motion Capture, MoCap) im Sport nutzen retroreflektive Marker. Welche Systemarchitektur ermoeglicht Markerlose MoCap in Echtzeit ohne spezielle Anzuege?",
        answerA = "Structured-Light-Systeme mit infrarotem Schachbrettprojektor (Kinect-Prinzip)",
        answerB = "Deep-Learning-basierte Pose-Estimation (z.B. MediaPipe BlazePose, OpenPose) auf Kamerabild",
        answerC = "Time-of-Flight-Kameras mit Millimeter-Tiefenaufloesung (LiDAR-Chipsets)",
        answerD = "Elektromagnetische Feld-Tracking-Systeme mit Koerpersensor-Array",
        correctAnswer = 1,
        explanation = "Deep-Learning-basierte Pose-Estimation (OpenPose, MediaPipe, YOLOv8 Pose) erkennt Koerperskelette aus Kamerabildern ohne Marker oder Spezialanzuege in Echtzeit. Diese Systeme werden zunehmend im Sport fuer Ganganalyse, Technik-Coaching und Taktikanalyse eingesetzt.",
        difficulty = 4,
        funFact = "Google MediaPipe BlazePose kann in Echtzeit 33 Koerperlandmarken auf Smartphones tracken — ein democratisierter MoCap fuer jeden Coach mit einem Handy, ohne teure Laborsysteme."
    ),

    Question(
        categoryId = 6,
        questionText = "Polymere Materialien fuer Sportgeraete werden durch ihre 'Shore-Haerte' charakterisiert. Welche Shore-Skala wird typischerweise fuer harte Gummi- und Kunststoffmaterialien (z.B. Schuhsohlen, Schlittenlaeufer) verwendet?",
        answerA = "Shore A (Skala fuer weiche Elastomere, 0-100)",
        answerB = "Shore D (Skala fuer harte Polymere und Elastomere, 0-100)",
        answerC = "Shore OO (Skala fuer extrem weiche Gele und Schaeume)",
        answerD = "Shore C (Skala fuer gewebeverstaerkte Gummiprodukte)",
        correctAnswer = 1,
        explanation = "Shore D wird fuer harte Polymere (PE, PP, Nylon, harte Gummimischungen) verwendet und reicht von 0 (sehr weich) bis 100 (Stahl-aehnlich). Laufschuh-Zwischensohlen aus EVA liegen bei Shore C 40-60, waehrend Schuhsteife aus TPU Shore D 50-70 erreicht.",
        difficulty = 4,
        funFact = "Albert Ferdinand Shore entwickelte das Shore-Haertepruefgeraet (Durometer) 1915 urspruenglich fuer Lederhaerte-Tests — es wurde erst Jahrzehnte spaeter standardisiert und fuer Polymere adaptiert."
    ),

    // --- SPORTGESCHICHTE-AKADEMISCH (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Das antike griechische Konzept der 'Arete' (Tugend/Exzellenz) war zentral fuer das Verstaendnis von Sport in der Antike. Welcher griechische Philosoph unterschied zwischen 'koerperlicher Arete' und 'ethischer Arete' explizit im Kontext athletischer Ausbildung?",
        answerA = "Platon in 'Politeia' (Buch III, Gymnastic und Musik als Seelenerziehung)",
        answerB = "Aristoteles in 'Nikomachische Ethik' (Tugend als Mitte zwischen Extremen)",
        answerC = "Pindar in den 'Epinikien' (Siegesoden als Dokument athletischer Exzellenz)",
        answerD = "Pausanias in 'Beschreibung Griechenlands' (ethnographische Dokumentation)",
        correctAnswer = 0,
        explanation = "Platon unterschied in der 'Politeia' (Buch III) explizit zwischen koerperlicher Ausbildung (Gymnastic) und musischer Bildung als komplementaere Formen der Arete. Fuer Platon diente koerperliches Training nicht primaer der Gesundheit, sondern der Seelenerziehung und Charakterbildung.",
        difficulty = 4,
        funFact = "Platon war selbst Ringer und soll den Namen 'Platon' (breite Schultern) als Kampfname im Ringkampf erhalten haben — sein Geburtsname war Aristokles."
    ),

    Question(
        categoryId = 6,
        questionText = "Die Olympischen Spiele der Antike wurden 393 n. Chr. verboten. Durch welches kaiserliche Edikt und welche Begruendung wurden sie abgeschafft?",
        answerA = "Edikt von Thessaloniki (380 n. Chr.) durch Theodosius I. als Teil der Christianisierung des Roemischen Reiches",
        answerB = "Edikt von Theodosius I. (393 n. Chr.) als heidnisches Kult-Fest, unvereinbar mit christlichem Staatsglauben",
        answerC = "Senats-Beschluss Roms (394 n. Chr.) wegen politischer Unruhen in der Peloponnes",
        answerD = "Vandalen-Invasion (412 n. Chr.) zerstoerte Olympia, woraufhin ein faktisches Verbot folgte",
        correctAnswer = 1,
        explanation = "Kaiser Theodosius I. verbot 393 n. Chr. die Olympischen Spiele als Teil seiner Politik zur Christianisierung des Roemischen Reiches. Die Spiele galten als heidnisches Kultfest zu Ehren des Zeus und waren damit mit dem Staatschristentum unvereinbar.",
        difficulty = 4,
        funFact = "Die Olympischen Spiele liefen tatsaechlich bis 393 n. Chr. ununterbrochen — 1169 Jahre seit der ersten dokumentierten Austragung 776 v. Chr., was sie zur laengsten ununterbrochenen Sportveranstaltung der Menschheitsgeschichte macht."
    ),

    Question(
        categoryId = 6,
        questionText = "Der Sporthistoriker Allen Guttmann entwickelte das Konzept der 'Modernisierung des Sports'. Welche seiner sieben Merkmale modernen Sports gilt als das am staerksten differenzierende gegenueber vormodernem Sport?",
        answerA = "Saekularisierung (Trennung von religioesem Ritual und Sport)",
        answerB = "Quantifizierung (Messbarkeit aller Leistungen in Zahlen)",
        answerC = "Buerokratisierung (formelle Organisationen und Regelwerke)",
        answerD = "Spezialisierung (dedizierte Athletenrollen und Positionen)",
        correctAnswer = 1,
        explanation = "Guttmann betonte die Quantifizierung als zentrales Merkmal modernen Sports: Das Streben nach messbaren Rekorden ('higher, faster, stronger') unterscheidet modernen Sport fundamental von vormodernen Formen, bei denen relativer Sieg, nicht der absolute Wert, zaehlt.",
        difficulty = 4,
        funFact = "Guttmanns Buch 'From Ritual to Record' (1978) ist eines der meistzitierten Werke der Sportgeschichte und legte den Grundstein fuer die akademische Sportsoziologie als eigenstaendige Disziplin."
    ),

    Question(
        categoryId = 6,
        questionText = "Die Turnbewegung (Turnen) des Friedrich Ludwig Jahn ('Vater des Turnens') hatte eine explizit politische Dimension. Welche ideologische Verbindung praegte Jahns Turnbewegung der Napoleonischen Aera?",
        answerA = "Internationalismus und Voelkerverstaendigung als Gegenentwurf zu nationalistischen Kriegen",
        answerB = "Deutscher Nationalismus und koerperliche Ertuechtigung zur Befreiung von napoleonischer Besatzung",
        answerC = "Sozialer Liberalismus und Klassenueberwindung durch gemeinsames koerperliches Training",
        answerD = "Preussischer Militarismus zur Ausbildung kampffaehiger Soldaten fuer das Heer",
        correctAnswer = 1,
        explanation = "Jahns Turnbewegung (ab 1811, erster Turnplatz in Berlin-Hasenheide) war explizit von deutschem Nationalismus gepraegt. Koerperliche Ertuechtigung sollte die deutsche Jugend befaehigen, die napoleonische Besatzung zu ueberwinden und einen deutschen Nationalstaat zu bilden.",
        difficulty = 4,
        funFact = "Friedrich Ludwig Jahn wurde nach Napoleons Niederlage (1815) von den deutschen Fuerstentumern als 'Demagoge' verfolgt und inhaftiert, da seine nationalistische Turnbewegung als revolutionaere Bedrohung der monarchischen Ordnung galt."
    ),

    Question(
        categoryId = 6,
        questionText = "Die 'Muscular Christianity'-Bewegung des 19. Jahrhunderts verband koerperliche Ertuechtigung mit protestantischer Moral. In welchem literarischen Werk wurde dieses Konzept erstmals popularisiert?",
        answerA = "Thomas Hughes: 'Tom Brown's School Days' (1857)",
        answerB = "Thomas Arnold: 'School Sermons' (1832) von Rugby School",
        answerC = "Charles Kingsley: 'Yeast: A Problem' (1848)",
        answerD = "Herbert Spencer: 'Education: Intellectual, Moral and Physical' (1861)",
        correctAnswer = 0,
        explanation = "'Tom Brown's School Days' (1857) von Thomas Hughes schilderte das Leben in der Rugby School unter Thomas Arnold und popularisierte die Verbindung von athletischer Tuechigkeit mit Charakterentwicklung, Fairness und protestantischer Moral — das literarische Manifest der Muscular Christianity.",
        difficulty = 4,
        funFact = "Das Buch verkaufte sich im viktorianischen England millionenfach und praegt bis heute das Ideal des 'fair play' im angelsaechsischen Sport. Der Begriff 'Muscular Christianity' wurde von einem Rezensenten gepraegt, nicht von Hughes selbst."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Black Power Salute' der Sprinter Tommie Smith und John Carlos bei den Olympischen Spielen 1968 in Mexiko-Stadt hatte eine spezifische symbolische Bedeutung ihrer Gesten. Was symbolisierten die schwarzen Handschuhe, die Smith und Carlos trugen?",
        answerA = "Trauer um gefallene Buergerrechtskaempfer (Martin Luther King Jr.)",
        answerB = "Smith trug einen schwarzen Handschuh an der rechten Hand (schwarze Macht), Carlos an der linken (Einheit der afroamerikanischen Gemeinschaft)",
        answerC = "Symbolische Verbindung zum Black Panther Movement als politische Parteinahme",
        answerD = "Schutz vor Kaltekraempfen waehrend der Siegerehrung (praktische Erklaerung)",
        correctAnswer = 1,
        explanation = "Smith trug den rechten Handschuh (symbolisch fuer 'Black Power'), Carlos den linken (fuer 'Black Unity'). Eigentlich hatten beide je einen Handschuh desselben Paares — Carlos hatte seinen vergessen, also teilten sie ein Paar. Der silberne Medaillengewinner Peter Norman (Australien) trug ein OPHR-Abzeichen in Solidaritaet.",
        difficulty = 4,
        funFact = "Peter Norman, der weisse Australier auf Platz 2, unterstuetzte die Aktion und trug ein Abzeichen des 'Olympic Project for Human Rights'. Er wurde dafuer in Australien jahrzehntelang ausgegrenzt und erst posthum rehabilitiert."
    ),

    Question(
        categoryId = 6,
        questionText = "Die akademische Sportgeschichte diskutiert das Konzept des 'Invented Traditions' (Hobsbawm/Ranger) im Sportkontext. Welche moderne Sporttradition wurde in der Forschung als 'Erfundene Tradition' des 19./fruehen 20. Jahrhunderts entlarvt?",
        answerA = "Die Olympischen Ringe als antikes Symbol der fuenf Kontinente",
        answerB = "Das Abendessen der Olympioniken im antiken Prytaneion als Vorform moderner Athletenbetreuung",
        answerC = "Die englischen FA-Cup-Traditionen (Pokaluebergabe durch Koenig/Koenigin) als viktorianische Erfindung",
        answerD = "Das Fussball-Handshake vor Spielen als mittelalterliche Ritterstradition",
        correctAnswer = 0,
        explanation = "Die fuenf Olympischen Ringe wurden von Pierre de Coubertin 1912/1913 entworfen und stellen die fuenf damaligen olympischen Kontinente dar — KEINE antike Symboltradition. Coubertin praesentierte sie 1914 auf dem Olympischen Kongress als neues Symbol, was eine 'invented tradition' im Sinne Hobsbawms darstellt.",
        difficulty = 4,
        funFact = "Das hartnaeaeckige Geruecht, die Ringe seien an einem antiken Stein in Delphi gefunden worden, entstand durch eine Fotomontage griechischer Propagandaarbeit in den 1930er Jahren und wurde erst 2005 endgueltig als Faelschung entlarvt."
    ),

    Question(
        categoryId = 6,
        questionText = "In der akademischen Sportgeschichte beschreibt das Konzept der 'Sportisierung' (Norbert Elias/Eric Dunning) einen langfristigen zivilisatorischen Prozess. Welches Kernmerkmal unterscheidet 'sportisierte' Kampfspiele von vormodernen Formen?",
        answerA = "Physische Trennung der Zuschauer von Teilnehmern durch Absperrungen und Spielfelder",
        answerB = "Standardisierung der Regeln, Kontrolle von Gewalt und Zentralisierung der Regelueberwachung durch Institutionen",
        answerC = "Professionalisierung der Athleten mit Gehaltszahlungen und Vertraegen",
        answerD = "Mediale Berichterstattung als externe Kontrolle ueber Regelkonformitaet",
        correctAnswer = 1,
        explanation = "Elias und Dunning definierten 'Sportisierung' als Prozess zunehmender Standardisierung von Regeln, Eindaemmung physischer Gewalt innerhalb klar definierter Grenzen und Etablierung institutioneller Regelueberwachung — als Teil des umfassenderen 'Zivilisationsprozesses' (Elias).",
        difficulty = 4,
        funFact = "Elias' Zivilisationstheorie und deren Anwendung auf Sport durch Dunning ('Quest for Excitement', 1986) erklaert auch das Hooligan-Phaenomen als Suche nach kontrollierten Erregungszustaenden in einer zunehmend geregelten Gesellschaft."
    ),

    Question(
        categoryId = 6,
        questionText = "Die Bosman-Entscheidung (EuGH, 1995) revolutionierte den europaeischen Fussball. Welcher rechtliche Grundsatz des EU-Rechts bildete die Basis fuer das Urteil gegen das Transfersystem der UEFA?",
        answerA = "Artikel 102 AEUV (Verbot des Missbrauchs einer marktbeherrschenden Stellung)",
        answerB = "Artikel 45 AEUV (Freizuegigkeit der Arbeitnehmer im EU-Binnenmarkt)",
        answerC = "Artikel 101 AEUV (Kartellverbot durch wettbewerbsbeschraenkende Vereinbarungen)",
        answerD = "Artikel 19 EUV (Zustaendigkeit des EuGH fuer EU-Grundrechte)",
        correctAnswer = 1,
        explanation = "Das Bosman-Urteil (C-415/93) stuetzte sich auf Artikel 48 EGV (heute Art. 45 AEUV) ueber Arbeitnehmerfreizuegigkeit. Das EuGH entschied, dass Transfergebuehren fuer vertragsfreie Spieler innerhalb der EU gegen diese Grundfreiheit verstoessen.",
        difficulty = 4,
        funFact = "Jean-Marc Bosman selbst lebte nach seinem Sieg in bitteren wirtschaftlichen Verhaeltnissen und kaempfte jahrelang mit Depressionen und Alkoholproblemen — sein Triumph fuer Millionen Fussballspieler brachte ihm persoenlich wenig."
    ),

    Question(
        categoryId = 6,
        questionText = "Das 'Amateurismus'-Ideal der viktorianischen olympischen Bewegung hatte eine explizite Klassenkomponente. Welche Personengruppe war in den fruehen modernen Olympischen Spielen (1896-1930) effektiv vom Amateurstatus ausgeschlossen, obwohl sie keine Bezahlung erhielt?",
        answerA = "Frauen, da koerperliche Konkurrenz als unweiblich galt",
        answerB = "Arbeiter, deren Freizeit als 'bezahlte Auszeit' von der Arbeit galt, was Professionalisierung implizierte",
        answerC = "Koloniale Athleten, da IOC-Mitgliedschaft auf souveraene Nationen beschraenkt war",
        answerD = "Studenten universitaerer Hochleistungsprogramme mit Stipendien",
        correctAnswer = 1,
        explanation = "Die viktorianische Amateurismus-Definition schloss Arbeiter explizit aus, weil ihre Teilnahme an Wettbewerben oft durch Arbeitgeber finanziert oder durch Lohnverlust ermoeglicht wurde — was als 'verdeckte Professionalisierung' galt. Dies war eine klar klassenbasierte Diskriminierung zugunsten buergerlicher Athleten.",
        difficulty = 4,
        funFact = "Jim Thorpe (USA), zweifacher Olympiasieger 1912, wurden seine Medaillen aberkannt, weil er als Teenager fuer ein Semi-Profi-Baseballteam gespielt und minimales Geld erhalten hatte — ein klassisches Beispiel der selektiven Amateurismus-Durchsetzung."
    )
)
