package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsExpert4(): List<Question> = listOf(

    // Question 1 -- ECG: ST-elevation patterns
    Question(
        categoryId = 16,
        questionText = "Ein Patient zeigt ST-Hebungen in den Ableitungen II, III und aVF. Welche Struktur ist am wahrscheinlichsten betroffen?",
        answerA = "Linke Koronararterie, Ramus interventricularis anterior",
        answerB = "Rechte Koronararterie, inferiorer Myokardabschnitt",
        answerC = "Linke Koronararterie, Ramus circumflexus, lateraler Abschnitt",
        answerD = "Linke Koronararterie, Hauptstamm",
        correctAnswer = 1,
        explanation = "ST-Hebungen in II, III und aVF entsprechen einem inferioren STEMI, der klassischerweise durch Verschluss der rechten Koronararterie (RCA) verursacht wird, da diese den inferioren Myokardabschnitt versorgt. Eine reciproke ST-Senkung in aVL ist typisch.",
        difficulty = 4,
        funFact = "Bei einem inferioren STEMI sollte immer auch Ableitung V4R aufgezeichnet werden, um einen begleitenden rechtsventrikulaeren Infarkt auszuschliessen, da dieser die Therapie veraendert -- Nitrate sind dann kontraindiziert."
    ),

    // Question 2 -- ECG: QT prolongation
    Question(
        categoryId = 16,
        questionText = "Die QTc-Verlaengerung im EKG wird nach der Formel von Bazett berechnet. Ab welchem korrigierten QTc-Wert gilt das Risiko fuer Torsades de Pointes bei Maennern als signifikant erhoehen?",
        answerA = "QTc > 400 ms",
        answerB = "QTc > 430 ms",
        answerC = "QTc > 450 ms",
        answerD = "QTc > 500 ms",
        correctAnswer = 3,
        explanation = "Ein QTc ueber 500 ms gilt als kritischer Schwellenwert, ab dem das Risiko fuer lebensbedrohliche Torsades de Pointes deutlich ansteigt. Werte zwischen 450 und 500 ms gelten als grenzwertig verlaengert und beduerfens der engmaschigen Kontrolle.",
        difficulty = 4,
        funFact = "Das Long-QT-Syndrom Typ 1 (KCNQ1-Mutation) ist besonders beim Schwimmen gefaehrlich, waehrend Typ 2 (KCNH2-Mutation) typisch durch plotzliche Geraeusche ausgeloest wird -- daher der Begriff 'auditory trigger'."
    ),

    // Question 3 -- ECG: bundle branch block
    Question(
        categoryId = 16,
        questionText = "Welches EKG-Kriterium ist pathognomonisch fuer einen kompletten Linksschenkelblock (LBBB)?",
        answerA = "RSR'-Muster in V1 mit QRS-Dauer >= 120 ms",
        answerB = "Breite, gekerbte R-Zacke in I, aVL, V5, V6 mit QRS-Dauer >= 120 ms",
        answerC = "Tiefe S-Zacke in I kombiniert mit Q-Zacke in III und negativem T in aVF",
        answerD = "Positive Konkordanz in allen Praekordialableitungen",
        correctAnswer = 1,
        explanation = "Der LBBB zeigt breite, gekerbte oder eingekerbte R-Wellen in den linksseitigen Ableitungen (I, aVL, V5, V6) mit einer QRS-Dauer von mindestens 120 ms. Das RSR'-Muster in V1 ist dagegen typisch fuer den Rechtsschenkelblock (RBBB).",
        difficulty = 4,
        funFact = "Ein neu aufgetretener LBBB bei Brustschmerz gilt nach den Sgarbossa-Kriterien als STEMI-Aequivalent und muss wie ein STEMI behandelt werden -- der sogenannte Sgarbossa-Score hilft, darunter liegende Infarkte zu erkennen."
    ),

    // Question 4 -- Heart failure: NYHA classification
    Question(
        categoryId = 16,
        questionText = "Ein Patient mit chronischer Herzinsuffizienz berichtet, dass er bereits beim ruhigen Sitzen Dyspnoe verspuert. Welchem NYHA-Stadium entspricht dies?",
        answerA = "NYHA I -- keine Einschraenkung bei koerperlicher Aktivitaet",
        answerB = "NYHA II -- leichte Einschraenkung, Beschwerden bei starker Belastung",
        answerC = "NYHA III -- deutliche Einschraenkung, Beschwerden bei leichter Belastung",
        answerD = "NYHA IV -- Beschwerden in Ruhe oder bei minimaler Belastung",
        correctAnswer = 3,
        explanation = "NYHA IV bezeichnet Patienten, die selbst in Ruhe oder bei minimaler koerperlicher Aktivitaet Symptome wie Dyspnoe oder Angina haben. Diese Patienten sind meist bettlaegerig und haben eine sehr eingeschraenkte Lebensqualitaet.",
        difficulty = 4,
        funFact = "Die NYHA-Klassifikation wurde 1928 von der New York Heart Association eingefuehrt. Trotz ihres Alters ist sie nach wie vor das wichtigste klinische Instrument zur Einschaetzung der funktionellen Kapazitaet bei Herzinsuffizienz."
    ),

    // Question 5 -- Heart failure: ACC/AHA stages
    Question(
        categoryId = 16,
        questionText = "Das ACC/AHA-Stagingsystem fuer Herzinsuffizienz umfasst vier Stadien (A-D). Was unterscheidet Stadium B von Stadium A?",
        answerA = "Stadium B hat Symptome, Stadium A nicht",
        answerB = "Stadium B hat strukturelle Herzerkrankung ohne Symptome, Stadium A hat nur Risikofaktoren",
        answerC = "Stadium B hat refraktaere Herzinsuffizienz, Stadium A ist therapierbar",
        answerD = "Stadium B erfordert Transplantation, Stadium A nur medikamentoese Therapie",
        correctAnswer = 1,
        explanation = "Im ACC/AHA-System bezeichnet Stadium A Patienten mit Risikofaktoren fuer Herzinsuffizienz ohne strukturelle Herzerkrankung (z.B. Hypertoniker), waehrend Stadium B bereits strukturelle Veraenderungen zeigt (z.B. reduzierte EF, linksventriklulaere Hypertrophie), aber noch keine Symptome.",
        difficulty = 4,
        funFact = "Das ACC/AHA-Staging komplementiert die NYHA-Klassifikation -- NYHA beschreibt den aktuellen Funktionszustand, waehrend ACC/AHA den Krankheitsverlauf und die Progression abbildet. Ein Patient kann im ACC/AHA-Stadium D gleichzeitig NYHA IV haben."
    ),

    // Question 6 -- Valvular disease: aortic stenosis
    Question(
        categoryId = 16,
        questionText = "Welche haemodynamischen Kriterien definieren eine hochgradige Aortenklappenstenose?",
        answerA = "Mittlerer Gradient >= 20 mmHg und KOF-indexierte Klappenoeffnungsflaeche < 1,5 cm2",
        answerB = "Mittlerer Gradient >= 40 mmHg und Klappenoeffnungsflaeche < 1,0 cm2",
        answerC = "Spitzengradient >= 50 mmHg und Klappenoeffnungsflaeche < 1,5 cm2",
        answerD = "Mittlerer Gradient >= 30 mmHg und Vmax >= 3,5 m/s",
        correctAnswer = 1,
        explanation = "Die hochgradige Aortenklappenstenose wird echokardiographisch durch einen mittleren Druckgradienten von mindestens 40 mmHg, eine maximale Jetgeschwindigkeit (Vmax) von mindestens 4,0 m/s und eine Klappenoeffnungsflaeche unter 1,0 cm2 definiert.",
        difficulty = 4,
        funFact = "Die klassische Symptomentrias der Aortenstenose -- Angina, Synkope und Herzinsuffizienz (Hegglin-Trias) -- geht mit unterschiedlichen mittleren Ueberlebenszeiten einher: 5 Jahre bei Angina, 3 Jahre bei Synkope und nur 2 Jahre bei Herzinsuffizienz."
    ),

    // Question 7 -- Valvular disease: mitral regurgitation grading
    Question(
        categoryId = 16,
        questionText = "Bei der Beurteilung einer Mitralklappeninsuffizienz gilt die Vena contracta als wichtiger echokardiographischer Parameter. Ab welchem Wert spricht man von einer schweren Mitralklappeninsuffizienz?",
        answerA = "Vena contracta >= 3 mm",
        answerB = "Vena contracta >= 5 mm",
        answerC = "Vena contracta >= 7 mm",
        answerD = "Vena contracta >= 10 mm",
        correctAnswer = 2,
        explanation = "Eine Vena contracta von mindestens 7 mm gilt als Zeichen einer schweren (hochgradigen) Mitralklappeninsuffizienz. Die Vena contracta beschreibt die engste Stelle des Regurgitationsjets direkt am Klappenostium und korreliert gut mit der Schwere der Insuffizienz.",
        difficulty = 4,
        funFact = "Bei der primaeren Mitralklappeninsuffizienz (z.B. durch Sehnenfadenabriss beim Mitralklappenprolaps) zeigt der Regurgitationsjet oft eine exzentrische Richtung, weil er an der Gegenwand des linken Vorhofs entlangstreicht -- dieser sogenannte 'Coanda-Effekt' kann die Schwere unterschaetzen lassen."
    ),

    // Question 8 -- Stroke: tPA criteria
    Question(
        categoryId = 16,
        questionText = "Welches ist eine absolute Kontraindikation fuer die intravenoese Thrombolyse (rt-PA/Alteplase) beim ischaemischen Schlaganfall?",
        answerA = "Blutdruck > 160/90 mmHg bei Therapiebeginn",
        answerB = "Alter ueber 80 Jahre",
        answerC = "Intrakranielle Blutung in der Bildgebung",
        answerD = "NIHSS-Score unter 4 Punkten",
        correctAnswer = 2,
        explanation = "Eine intrakranielle Blutung in der akuten CT- oder MRT-Untersuchung ist eine absolute Kontraindikation fuer die Thrombolyse, da rt-PA die Blutung massiv vergroessern wuerde. Blutdruck bis 185/110 mmHg ist hingegen durch Antihypertensiva behandelbar, und Alter ueber 80 Jahre ist keine absolute Kontraindikation mehr.",
        difficulty = 4,
        funFact = "Das Zeitfenster fuer intravenoese Thrombolyse wurde 2019 durch die WAKE-UP-Studie erweitert: Patienten mit unbekanntem Symptombeginn ('wake-up stroke') koennen bei passendem DWI/FLAIR-Mismatch im MRT behandelt werden."
    ),

    // Question 9 -- Stroke: mechanical thrombectomy
    Question(
        categoryId = 16,
        questionText = "Bis zu welchem Zeitfenster nach Symptombeginn ist eine mechanische Thrombektomie bei proximalem Gefaessverschluss gemaess aktueller Evidenz indiziert, wenn ein guenstiges Penumbra-Profil vorliegt?",
        answerA = "Bis 4,5 Stunden",
        answerB = "Bis 6 Stunden",
        answerC = "Bis 16 Stunden",
        answerD = "Bis 24 Stunden",
        correctAnswer = 3,
        explanation = "Die DAWN- und DEFUSE-3-Studien zeigten, dass bei selektionierten Patienten mit guenstigem Penumbra-Profil (kleines Infarktvolumen, grosses Penumbragebiet) eine Thrombektomie bis 24 Stunden nach Symptombeginn klinisch signifikanten Nutzen bringt.",
        difficulty = 4,
        funFact = "Der Begriff 'Time is Brain' laesst sich quantifizieren: Pro Minute ohne Reperfusion sterben beim ischaemischen Schlaganfall rund 1,9 Millionen Neuronen. Die schnelle Rekanalisation ist damit bildlich gesprochen 'Zeit gegen Gehirn'."
    ),

    // Question 10 -- Stroke: NIHSS score
    Question(
        categoryId = 16,
        questionText = "Welche neurologische Funktion wird im NIH Stroke Scale (NIHSS) NICHT direkt bewertet?",
        answerA = "Bewusstseinslage und Orientierung",
        answerB = "Fazialisparese",
        answerC = "Kognitive Leistung im Mini-Mental-State",
        answerD = "Extinktion und Neglect",
        correctAnswer = 2,
        explanation = "Der NIHSS bewertet insgesamt 11 Bereiche, darunter Bewusstsein, Augenmotilitaet, Gesichtsfeld, Motorik, Ataxie, Sensibilitaet, Sprache, Dysarthrie und Neglect. Ein standardisierter Kognitionstest wie der MMSE ist nicht Bestandteil des NIHSS.",
        difficulty = 4,
        funFact = "Der NIHSS-Score korreliert stark mit dem Infarktvolumen und dem funktionellen Ergebnis. Ein Score ueber 20 weist auf einen schweren Schlaganfall hin; Patienten mit NIHSS unter 4 werden wegen geringerem Risiko-Nutzen-Verhaeltnis oft nicht thrombolysiert."
    ),

    // Question 11 -- Epilepsy: EEG patterns
    Question(
        categoryId = 16,
        questionText = "Welches EEG-Muster ist charakteristisch fuer die juvenile myoklonische Epilepsie (JME)?",
        answerA = "Fokale Theta-Wellen temporal mit Phasenumkehr",
        answerB = "3-Hz-Spike-Wave-Komplexe bei Hyperventilation",
        answerC = "Irregulaere 4-6-Hz-Polyspike-Wave-Komplexe, generalisiert",
        answerD = "Hypsarrhythmie mit desorganisiertem Hintergrund",
        correctAnswer = 2,
        explanation = "Die JME zeigt im EEG typischerweise irregulaere generalisierte Polyspike-Wave-Komplexe mit einer Frequenz von 4-6 Hz, besonders in den Morgenstunden und bei Schlafentzug. Das Muster unterscheidet sich von der Absence-Epilepsie, die klassische 3-Hz-Spike-Waves zeigt.",
        difficulty = 4,
        funFact = "Die JME ist eine der haeufigsten genetischen Epilepsien und beginnt meist im Jugendalter. Trotz exzellenter Pharmakokontrolle mit Valproat brauchen die meisten Patienten lebenslang Medikamente, da die Anfallsrate nach Absetzen sehr hoch ist."
    ),

    // Question 12 -- Epilepsy: status epilepticus treatment
    Question(
        categoryId = 16,
        questionText = "Ein generalisierter tonisch-klonischer Status epilepticus besteht seit 30 Minuten trotz Benzodiazepin-Gabe. Welche Behandlung wird als 'second-line'-Therapie empfohlen?",
        answerA = "Erneute Benzodiazepin-Gabe in doppelter Dosis",
        answerB = "Levetiracetam, Valproat oder Phenytoin intravenoese Infusion",
        answerC = "Phenobarbital oral als Aufsaettigungsdosis",
        answerD = "Sofortige Intubation und Thiopental-Infusion",
        correctAnswer = 1,
        explanation = "Nach gescheiterter Benzodiazepin-Therapie (first-line) werden als second-line-Optionen intravenoeses Levetiracetam, Valproat oder Phenytoin/Fosphenytoin empfohlen. Erst bei einem refraktaeren Status (> 30-60 min nach second-line) wird die Narkose mit Midazolam, Propofol oder Thiopental eingeleitet.",
        difficulty = 4,
        funFact = "Der Zeitpunkt '5 Minuten' ist klinisch entscheidend: Ein Anfall der laenger als 5 Minuten dauert, stoppt statistisch nicht mehr spontan und sollte sofort medikamentoes unterbrochen werden -- deswegen gilt 5 Minuten als pragmatische Definition des Status epilepticus."
    ),

    // Question 13 -- Parkinson: mechanism
    Question(
        categoryId = 16,
        questionText = "Welcher pathophysiologische Mechanismus erklaert die Bradykinese bei der Parkinson-Erkrankung am praezisesten?",
        answerA = "Degeneration des Nucleus subthalamicus mit unkontrollierter Erregung des Thalamus",
        answerB = "Dopaminmangel in der Substantia nigra pars compacta fuehrt zur Uebererregung des indirekten Striatumweges",
        answerC = "Cholinerges Uebergewicht im Striatum hemmt die Pyramidalbahn direkt",
        answerD = "Serotoninmangel im Raphekern fuehrt zur motorischen Verlangsamung",
        correctAnswer = 1,
        explanation = "Der Dopaminmangel in der Substantia nigra pars compacta verstaerkt den indirekten striatalen Weg (Striatum -> Globus pallidus externus -> Nucleus subthalamicus -> Globus pallidus internus), was zu einer Ueberhemmung des Thalamus und damit zur Bradykinese fuehrt.",
        difficulty = 4,
        funFact = "Erst wenn etwa 80% der dopaminergen Neurone der Substantia nigra degeneriert sind, werden Parkinson-Symptome klinisch manifest -- das Gehirn kompensiert den Verlust durch Upregulation der verbleibenden Neurone erstaunlich lange."
    ),

    // Question 14 -- Huntington: genetics
    Question(
        categoryId = 16,
        questionText = "Die Huntington-Erkrankung wird durch eine CAG-Repeat-Expansion im HTT-Gen verursacht. Welche Repeat-Anzahl gilt als sicher erkrankt?",
        answerA = "10-26 Repeats",
        answerB = "27-35 Repeats (intermediaer, meist klinisch stumm)",
        answerC = "36-39 Repeats (reduzierte Penetranz)",
        answerD = ">= 40 Repeats (vollstaendige Penetranz)",
        correctAnswer = 3,
        explanation = "Ab 40 CAG-Repeats im HTT-Gen besteht vollstaendige Penetranz -- alle Traeger dieser Mutation erkranken im Laufe ihres Lebens. Repeats zwischen 36 und 39 zeigen reduzierte Penetranz, und unter 36 Repeats gilt die Person als gesund.",
        difficulty = 4,
        funFact = "Das Phaenomen der Antizipation -- frueherer Beginn und schwererer Verlauf in nachfolgenden Generationen -- ist bei Huntington besonders ausgepraegte, wenn das erkrankte Gen vaeterlicherseits vererbt wird, da Spermien eine hoehere Instabilitaet der Repeats zeigen."
    ),

    // Question 15 -- Dystonia
    Question(
        categoryId = 16,
        questionText = "Welches Gen ist am haeufigsten bei der isolierten, generalisierten Dystonie (DYT-TOR1A) mutiert, und welches Vererbungsmuster liegt vor?",
        answerA = "SGCE-Gen, autosomal dominant mit maternalem Imprinting",
        answerB = "TOR1A-Gen (Torsin A), autosomal dominant mit reduzierter Penetranz (30-40%)",
        answerC = "GCH1-Gen, autosomal dominant mit vollstaendiger Penetranz",
        answerD = "ATP1A3-Gen, autosomal rezessiv",
        correctAnswer = 1,
        explanation = "Die DYT-TOR1A-Dystonie wird durch eine Deletion (GAG-Deletion) im TOR1A-Gen verursacht und autosomal dominant vererbt, jedoch mit nur 30-40% Penetranz. Die Erkrankung beginnt meist im Kindes- oder Jugendalter in einer Extremitaet und generalisiert haeufig.",
        difficulty = 4,
        funFact = "Die tiefe Hirnstimulation (DBS) des Globus pallidus internus ist bei der DYT-TOR1A-Dystonie hochwirksam -- die Verbesserung tritt jedoch nicht sofort auf, sondern entwickelt sich ueber Monate, was als 'delayed benefit' bezeichnet wird."
    ),

    // Question 16 -- HPA axis
    Question(
        categoryId = 16,
        questionText = "Welche Substanz mediiert die negative Rueckkopplung auf die CRH-Freisetzung im Hypothalamus im Rahmen der HPA-Achsen-Regulation?",
        answerA = "ACTH aus dem Hypophysenvorderlappen",
        answerB = "Cortisol aus der Nebennierenrinde",
        answerC = "Aldosteron aus der Zona glomerulosa",
        answerD = "Adrenalin aus dem Nebennierenmark",
        correctAnswer = 1,
        explanation = "Cortisol aus der Nebennierenrinde hemmt sowohl die CRH-Freisetzung im Hypothalamus als auch die ACTH-Sekretion in der Hypophyse (negative Rueckkopplung). Dieser Mechanismus reguliert den basalen und stressinduzierten Cortisolspiegel.",
        difficulty = 4,
        funFact = "Die HPA-Achse zeigt eine ausgepragte zirkadiane Rhythmik: Cortisol erreicht seinen Tagespeak gegen 8 Uhr morgens ('cortisol awakening response') und faellt bis Mitternacht auf Minimalwerte ab. Nachtschichtarbeiter haben deswegen ein erhoehtes Metabolisches-Syndrom-Risiko."
    ),

    // Question 17 -- HPG axis
    Question(
        categoryId = 16,
        questionText = "Warum fuehrt eine kontinuierliche (nicht-pulsierende) GnRH-Applikation paradoxerweise zur Suppression der Gonadotropine LH und FSH?",
        answerA = "Kontinuierliches GnRH aktiviert eine Negativrueckkopplung im Hypothalamus",
        answerB = "Downregulation der GnRH-Rezeptoren in der Hypophyse durch Desensitisierung",
        answerC = "GnRH hemmt direkt die LH-Synthese im Hypophysenvorderlappen",
        answerD = "Kontinuierliches GnRH stimuliert die Inhibin-Freisetzung aus den Gonaden",
        correctAnswer = 1,
        explanation = "Physiologisch wird GnRH pulsatil (alle 60-90 Minuten) freigesetzt, was fuer die normale LH- und FSH-Sekretion notwendig ist. Bei kontinuierlicher GnRH-Exposition kommt es zur Desensitisierung und Downregulation der GnRH-Rezeptoren in der Hypophyse, was den Gonadotropinabfall erklaert.",
        difficulty = 4,
        funFact = "Dieses Prinzip wird therapeutisch genutzt: GnRH-Agonisten wie Leuprorelin werden bei Prostatakarzinom und Endometriose eingesetzt, um durch kontinuierliche Gabe eine medizinische Kastration zu induzieren -- ein elegantes Beispiel pharmakologischer Paradoxie."
    ),

    // Question 18 -- Thyroid feedback
    Question(
        categoryId = 16,
        questionText = "Bei einem Patienten mit einem TSH-produzierenden Hypophysenadenom (TSHom) sind sowohl TSH als auch fT4 und fT3 erhoeht. Warum ist die negative Rueckkopplung gestort?",
        answerA = "Das Adenom produziert TRH, das die TSH-Sekretion stimuliert",
        answerB = "Die Hypophysenzellen des Adenoms haben eine verminderte Sensitivitaet gegenueber dem negativen Feedback von Schilddruesenhormonen",
        answerC = "Ein erhoehtes T4 hemmt selektiv nur die TRH-Synthese, nicht die TSH-Sekretion",
        answerD = "Das Adenom produziert biologisch inaktives TSH, das nicht von T4 gehemmt werden kann",
        correctAnswer = 1,
        explanation = "TSH-produzierende Adenome (Thyreotropinome) haben eine verminderte Expression von Schilddruesenhormon-Rezeptoren und sind damit weitgehend resistent gegenueber der normalen Negativrueckkopplung durch T3/T4. Dadurch werden trotz hoher Schilddruesenhormonwerte weiter grosse Mengen TSH sezerniert.",
        difficulty = 4,
        funFact = "Das TSHom ist mit einem Anteil von < 1% aller Hypophysenadenome sehr selten. Die Diagnose wird oft verzoegert, da das gleichzeitige Vorliegen erhohter TSH- und Schilddruesenhormon-Werte fuer viele Aerzte ungewohnt ist -- normalerweise sind sie gegenlaefig."
    ),

    // Question 19 -- Pheochromocytoma
    Question(
        categoryId = 16,
        questionText = "Welcher biochemische Marker hat die hoechste Sensitivitaet fuer die Diagnose eines Phaeochromozytoms?",
        answerA = "Urin-Catecholamine (Adrenalin + Noradrenalin) im 24h-Sammelurin",
        answerB = "Plasma-Metanephrine (Metanephrin + Normetanephrin)",
        answerC = "Chromogranin A im Serum",
        answerD = "Vanillinmandelsaeure im 24h-Sammelurin",
        correctAnswer = 1,
        explanation = "Plasma-freie Metanephrine (Metanephrin und Normetanephrin) haben mit ~99% die hoechste Sensitivitaet fuer das Phaeochromozytom und sind laut aktuellen Leitlinien der bevorzugte Screening-Test. Sie entstehen durch kontinuierlichen Metabolismus in den Tumorzellen, unabhaengig von episodischer Catecholaminausschuettung.",
        difficulty = 4,
        funFact = "Etwa 25% aller Phaeochromozytome sind heredaer und mit Syndromen wie MEN2 (RET-Mutation), Von-Hippel-Lindau-Syndrom (VHL-Mutation) oder der SDH-Genmutation assoziiert. Deswegen empfehlen Leitlinien bei jedem Phaeochromozytom eine genetische Testung."
    ),

    // Question 20 -- Conn syndrome
    Question(
        categoryId = 16,
        questionText = "Welches Testergebnis bestaetigt den primaeren Hyperaldosteronismus (Conn-Syndrom) als Screeningtest am zuverlaessigsten?",
        answerA = "Einmalige Messung eines erhohten Aldosterons im Blut",
        answerB = "Aldosteron-Renin-Quotient (ARQ) > 30 (mit Aldosteron in ng/dL)",
        answerC = "Natriumspiegel > 148 mmol/L bei gleichzeitig erniedrigtem Kalium",
        answerD = "24h-Sammelurin-Aldosteron unter Kochsalzbelastung",
        correctAnswer = 1,
        explanation = "Der Aldosteron-Renin-Quotient (ARQ) ist der empfohlene Screeningtest. Ein ARQ > 30 (Aldosteron in ng/dL, Renin als Plasmarenin-Aktivitaet in ng/mL/h) bei gleichzeitig erhoehtem absolutem Aldosteron ist hochverdaechtig auf primaeren Hyperaldosteronismus.",
        difficulty = 4,
        funFact = "Der primaere Hyperaldosteronismus ist mit etwa 10% der Hypertoniefaelle wesentlich haeufiger als frueher angenommen. Viele dieser Patienten haben ein normales Kalium -- die fruehere Annahme, dass Hypokalaemie obligat ist, hat zu vielen verpassten Diagnosen gefuehrt."
    ),

    // Question 21 -- Diabetic retinopathy
    Question(
        categoryId = 16,
        questionText = "Welches Zeichen unterscheidet die proliferative diabetische Retinopathie (PDR) von der nicht-proliferativen (NPDR)?",
        answerA = "Vorhandensein von harten Exsudaten (Lipidablagerungen)",
        answerB = "Neovaskularisationen an der Papille oder anderswo in der Retina",
        answerC = "Mikroaneurysmen und punktfoermige Blutungen",
        answerD = "Verbreiterung der arteriolovenosen Kreuzungszeichen",
        correctAnswer = 1,
        explanation = "Das definierende Merkmal der proliferativen diabetischen Retinopathie ist das Auftreten pathologischer Neovaskularisationen (neue Blutgefaesse an der Papille -- NVD -- oder anderswo -- NVE) als Reaktion auf retinale Ischaemie. Die NPDR zeigt zwar Mikroaneurysmen, Blutungen und Exsudate, aber keine Neubildung von Gefaessen.",
        difficulty = 4,
        funFact = "VEGF (Vascular Endothelial Growth Factor) ist der zentrale Treiber der Neovaskularisation bei PDR. Intravitreoese Anti-VEGF-Injektionen (z.B. Ranibizumab, Aflibercept) haben die Behandlung revolutioniert und ersetzen zunehmend die Laserkoagulation."
    ),

    // Question 22 -- Diabetic nephropathy
    Question(
        categoryId = 16,
        questionText = "Ab welchem Stadium der chronischen Nierenerkrankung (CKD) bei Diabetes spricht man von einer etablierten diabetischen Nephropathie mit persistenter Proteinurie?",
        answerA = "Stadium G1, Albumin/Kreatinin-Quotient > 3 mg/mmol",
        answerB = "Makroalbuminurie (ACR >= 300 mg/g oder Proteinurie >= 500 mg/24h)",
        answerC = "Mikroalbuminurie (ACR 30-300 mg/g) bei erster Messung",
        answerD = "Stadium G4 (eGFR < 29 ml/min) unabhaengig von der Proteinurie",
        correctAnswer = 1,
        explanation = "Die diabetische Nephropathie ist durch persistente Makroalbuminurie (ACR >= 300 mg/g oder Urinprotein >= 500 mg/24h) definiert, die das Stadium der Mikroalbuminurie ueberschritten hat. Sie korreliert mit fortgeschrittenem glomerulaeren Schaden und ist ein starker kardiovaskulaerer Risikomarker.",
        difficulty = 4,
        funFact = "SGLT2-Inhibitoren (Empagliflozin, Dapagliflozin) zeigen in grossen Studien nephroprotektive Effekte unabhaengig vom Blutzuckerspiegel -- sie reduzieren die tubuloglomerulaere Rueckkopplung und den intraglomerulaeren Druck durch verminderte Natriumresorption."
    ),

    // Question 23 -- Diabetic neuropathy
    Question(
        categoryId = 16,
        questionText = "Welche Neuropathieform ist bei Diabetes mellitus am haeufigsten und welche Messmethode gilt als Goldstandard zur Fruehdiagnose?",
        answerA = "Autonome Neuropathie, Herzfrequenzvariabilitaet",
        answerB = "Distale symmetrische Polyneuropathie, Nervenleitgeschwindigkeit (NLG)",
        answerC = "Mononeuropathia multiplex, EMG",
        answerD = "Proximale motorische Neuropathie (Amyotrophie), MRT des Plexus",
        correctAnswer = 1,
        explanation = "Die distale symmetrische sensomotorische Polyneuropathie ist mit Abstand die haeufigste Neuropathieform bei Diabetes (~50% nach 25 Jahren). Die Nervenleitgeschwindigkeitsmessung (NLG/EMG) gilt als Goldstandard und kann subklinische Schaeden erkennen, bevor Symptome auftreten.",
        difficulty = 4,
        funFact = "Das diabetische Fuss-Syndrom ist eine direkte Folge der peripheren Neuropathie: Betroffene spueren keine Schmerzen mehr bei kleinen Wunden. Kombiniert mit einer Mikroangiopathie entsteht ein hoehes Amputationsrisiko -- Diabetes ist die haeufigste nicht-traumatische Ursache von Beinamputationen weltweit."
    ),

    // Question 24 -- Hyponatremia: SIADH
    Question(
        categoryId = 16,
        questionText = "Welche Konstellation von Laborwerten entspricht dem Syndrom der inadaequaten ADH-Sekretion (SIADH)?",
        answerA = "Serum-Na < 135 mmol/L, Serum-Osmolalitaet erniedrigt, Urin-Osmolalitaet > 100 mosmol/kg, Urin-Na > 40 mmol/L",
        answerB = "Serum-Na < 135 mmol/L, Serum-Osmolalitaet erniedrigt, Urin-Osmolalitaet < 100 mosmol/kg, Urin-Na < 20 mmol/L",
        answerC = "Serum-Na > 145 mmol/L, erhoehte Serum-Osmolalitaet, hohe ADH-Spiegel",
        answerD = "Serum-Na < 135 mmol/L, metabolische Alkalose, Hypokalaemie, erhoehter Aldosteron",
        correctAnswer = 0,
        explanation = "Das SIADH ist gekennzeichnet durch Hypoosmolalitaet im Serum (hypotone Hyponatriaemie), aber paradoxerweise konzentrierten Urin (Urin-Osmolalitaet > 100 mosmol/kg, oft > 300) und relativ hohes Urin-Natrium (> 40 mmol/L) -- der Koerper verliert Natrium trotz Natriumarmut im Blut.",
        difficulty = 4,
        funFact = "SIADH ist die haeufigste Ursache einer Hyponatriaemie im Krankenhaus. Wichtig: Die zu schnelle Korrektur (> 8-10 mmol/L pro 24h) einer chronischen Hyponatriaemie kann eine pontine Myelinolyse (osmotisches Demyelinisierungssyndrom) ausloesen -- eine gefuerchtete irreversible Komplikation."
    ),

    // Question 25 -- Hyperkalemia: ECG changes
    Question(
        categoryId = 16,
        questionText = "In welcher Reihenfolge treten typische EKG-Veraenderungen bei zunehmender Hyperkalaemie auf?",
        answerA = "P-Wellen-Verlust -> Sinusbradykardie -> Kammerflimmern -> Asystolie",
        answerB = "Spitze, hohe T-Wellen -> Verlust der P-Welle -> Verbreiterung des QRS -> Sinusoidalmuster -> Kammerflimmern",
        answerC = "ST-Senkungen -> QT-Verlaengerung -> AV-Block I -> AV-Block III -> Asystolie",
        answerD = "Verlust der P-Welle -> U-Wellen -> Flache T-Wellen -> QT-Verlaengerung",
        correctAnswer = 1,
        explanation = "Bei steigendem Kalium treten EKG-Veraenderungen in einer typischen Sequenz auf: Erst spitze, zeltfoermige T-Wellen (K > 5,5), dann Verlust der P-Welle und Verbreiterung des QRS-Komplexes (K > 6,5), dann ein sinusoidales Muster und schliesslich Kammerflimmern oder Asystolie (K > 7-8 mmol/L).",
        difficulty = 4,
        funFact = "Kalzium ist der schnellste und direkteste Antagonist bei lebensbedrohlicher Hyperkalaemie: Intravenoeses Kalziumglukonat stabilisiert die Myokardmembran innerhalb von Minuten, ohne den Kaliumspiegel zu senken -- es ist ein Membranstabilisator, kein Kaliumsenker."
    ),

    // Question 26 -- Hyponatremia: management
    Question(
        categoryId = 16,
        questionText = "Ein Patient mit symptomatischer schwerer Hyponatriaemie (Na = 115 mmol/L, Krampfanfaelle) wird notfallmaessig behandelt. Was ist die korrekte Initialtherapie?",
        answerA = "Freie Wasserrestriktion auf 500 ml/Tag und orales NaCl",
        answerB = "Infusion hypertoner NaCl (3%) mit dem Ziel einer Anhebung von 4-6 mmol/L in 6 Stunden",
        answerC = "Schnellste moegliche Korrektur auf Normalwerte mit 0,9% NaCl",
        answerD = "Furosemid-Infusion zur Wasserausscheidung ohne Natriumersatz",
        correctAnswer = 1,
        explanation = "Bei symptomatischer schwerer Hyponatriaemie (Kramfpanfaelle, tiefe Bewusstlosigkeit) ist 3%ige NaCl-Infusion indiziert mit dem Ziel, den Natriumspiegel schnell um 4-6 mmol/L anzuheben (max. 5 ml/kg KG ueber 10 min). Dies lindert die zerebrale Oedemkomplikation, ohne das Risiko einer Myelinolyse wesentlich zu erhoehen.",
        difficulty = 4,
        funFact = "Ein klinisch praktischer Merksatz: 'Bei Symptomen schnell handeln, aber langsam korrigieren' -- die erste Anhebung um 4-6 mmol/L darf schnell gehen, danach nie mehr als 8-10 mmol/L pro 24h insgesamt. Der Tolvaptan (Vasopressin-Antagonist) wird beim SIADH zunehmend eingesetzt, aber nicht in der Akutsituation."
    ),

    // Question 27 -- ECG: Wolff-Parkinson-White
    Question(
        categoryId = 16,
        questionText = "Ein Patient mit Wolff-Parkinson-White-Syndrom (WPW) hat eine Delta-Welle im EKG. Welche akute Arrhythmie ist bei WPW besonders gefuerchtet und potenziell letal?",
        answerA = "AV-Knoten-Reentrytachykardie (AVNRT) mit langen RP-Intervallen",
        answerB = "Vorhofflimmern mit schneller Ueberleitung ueber die akzessorische Bahn auf die Kammern",
        answerC = "Vorhofflattern mit 2:1-Ueberleitung und regelmaessiger Tachykardie",
        answerD = "Ventrikulaere Tachykardie aus dem rechten Ausflusstrakt",
        correctAnswer = 1,
        explanation = "Bei WPW-Patienten mit Vorhofflimmern kann die akzessorische Bahn eine sehr schnelle Kammerstimulation ermoglichen (> 200/min), da sie im Gegensatz zum AV-Knoten keine physiologische Verzoegelungsfunktion hat. Dies kann in Kammerflimmern und plotzlichen Herztod uebergehen.",
        difficulty = 4,
        funFact = "AV-Knoten-blockierende Medikamente (Digoxin, Verapamil, Adenosin) sind bei WPW mit Vorhofflimmern absolut kontraindiziert: Durch Blockade des AV-Knotens wird die akzessorische Bahn bevorzugt und die Kammerstimulation kann sich dramatisch beschleunigen."
    ),

    // Question 28 -- Adrenal insufficiency
    Question(
        categoryId = 16,
        questionText = "Welcher Test ist der Goldstandard zur Diagnose einer primaeren Nebenniereninsuffizienz (Morbus Addison)?",
        answerA = "Basales Serumcortisol unter 100 nmol/L morgens",
        answerB = "ACTH-Stimulationstest (Kurztest): kein adaequater Cortisolanstieg > 550 nmol/L nach 250 mcg Synacthen",
        answerC = "Metyrapon-Test: fehlender Anstieg von 11-Desoxycortisol",
        answerD = "CRH-Test: fehlender ACTH-Anstieg nach CRH-Gabe",
        correctAnswer = 1,
        explanation = "Der ACTH-Kurztest (Synacthen-Test) gilt als Goldstandard: 250 mcg ACTH i.v. werden gegeben und nach 30-60 Minuten der Cortisolspiegel gemessen. Ein Anstieg unter 550 nmol/L gilt als pathologisch. Der Test testet direkt die Cortisolreserve der Nebenniere.",
        difficulty = 4,
        funFact = "Morbus Addison wurde 1855 von Thomas Addison beschrieben, lange bevor Cortisol bekannt war. Kennedy-Prasident John F. Kennedy litt an Morbus Addison und nahm taglich Cortisonpraeparate -- ein wohlgehutetes medizinisches Geheimnis seiner Zeit."
    ),

    // Question 29 -- Parkinson: drug therapy
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die motorischen Fluktuationen ('Wearing-off') bei langjaehriger Levodopa-Therapie am besten?",
        answerA = "Tachyphylaxie an dopaminergen Rezeptoren durch chronische Stimulation",
        answerB = "Abnahme der dopaminergen Speicherkapazitaet im Striatum durch neuronalen Verlust, kuerzere Halbwertszeit des Effekts",
        answerC = "Induktion von Levodopa-abbauenden Enzymen in der Leber",
        answerD = "Zunehmende Blut-Hirn-Schrankenstoerung reduziert Levodopa-Transport",
        correctAnswer = 1,
        explanation = "Mit fortschreitender Neurodegeneration und abnehmendem dopaminergem Speichervermogen im Striatum wird die Wirkungsdauer von Levodopa kuerzer. Das Gehirn wird abhaengiger von der pulsatilen Zufuhr -- bei fehlendem Levodopa entstehen die charakteristischen 'Off'-Phasen mit Akinese.",
        difficulty = 4,
        funFact = "COMT-Inhibitoren (Entacapon, Opicapon) und MAO-B-Inhibitoren (Rasagilin, Safinamid) verlaengern die Levodopa-Wirkung, indem sie den Abbau verlangsamen -- eine pharmakologische Strategie gegen das Wearing-off-Phaenomen."
    ),

    // Question 30 -- Epilepsy syndromes
    Question(
        categoryId = 16,
        questionText = "Lennox-Gastaut-Syndrom ist eine schwere Epilepsieform des Kindesalters. Welches ist das charakteristische EEG-Muster im Wachzustand?",
        answerA = "Hochamplitudige hypsarrhythmische Aktivitaet mit Blitzspitzen",
        answerB = "Slow spike-wave-Komplexe (< 2,5 Hz), oft 1,5-2 Hz, generalisiert",
        answerC = "3-Hz-Spike-wave-Komplexe bei Hyperventilation, symmetrisch",
        answerD = "Fokale sharp waves temporal mit sekundaerer Generalisation",
        correctAnswer = 1,
        explanation = "Das Lennox-Gastaut-Syndrom zeigt im Wach-EEG charakteristischerweise langsame Spike-Wave-Komplexe mit einer Frequenz von weniger als 2,5 Hz (meist 1,5-2 Hz), die von der normalen Absence-Epilepsie (3 Hz) zu unterscheiden sind. Dazu kommen tonische Anfaelle, Sturzanfaelle und mentale Retardierung.",
        difficulty = 4,
        funFact = "Das Lennox-Gastaut-Syndrom ist eine der schwersten Epilepsieformen mit meist polypharmakotherapieresistenten Anfaellen. Neuere Cannabidiol-Praeparate (Epidiolex/Epidyolex) haben in kontrollierten Studien signifikante Anfallsreduktion gezeigt."
    ),

    // Question 31 -- Endocrine: MEN syndromes
    Question(
        categoryId = 16,
        questionText = "Multiple Endokrine Neoplasie Typ 1 (MEN1) ist assoziiert mit einer Trias von Tumoren. Welche Kombination ist korrekt?",
        answerA = "Phaeochromozytom, Hyperparathyreoidismus, Medullaeres Schilddruesenkarzinom",
        answerB = "Hypophysenadenom, Nebenschilddruesenadenom, Pankreas-Inselzelltumoren",
        answerC = "Akromegalie, Insulinom, Nebennierenrindenadenom",
        answerD = "Prolaktinom, Phaeochromozytom, Hyperparathyreoidismus",
        correctAnswer = 1,
        explanation = "MEN1 (Wermer-Syndrom) ist durch Mutationen im MEN1-Tumorsuppressorgen verursacht und umfasst die Trias: Hypophysenadenom (meist Prolaktinom), primaerer Hyperparathyreoidismus (Nebenschilddruesenadenom) und Pankreas-Inselzelltumoren (Gastrinom, Insulinom). MEN2A beinhaltet dagegen medullaeres Schilddruesenkarzinom und Phaeochromozytom.",
        difficulty = 4,
        funFact = "Der Gastrin-sezernierende Tumor (Gastrinom) im Rahmen von MEN1 verursacht das Zollinger-Ellison-Syndrom mit multiplen rezidivierenden Magenulcera. Bis zu 60% der Gastrinome im Rahmen von MEN1 sind maligne und metastasieren in die Leber."
    ),

    // Question 32 -- Cardiology: heart failure pharmacology
    Question(
        categoryId = 16,
        questionText = "Welches Medikament hat in der EMPHASIS-HF-Studie eine signifikante Mortalitaetsreduktion bei Herzinsuffizienz mit reduzierter EF (HFrEF) gezeigt?",
        answerA = "Spironolacton (unselektiver Mineralokortikoidrezeptorantagonist)",
        answerB = "Eplerenon (selektiver Mineralokortikoidrezeptorantagonist)",
        answerC = "Torasemid (Schleifendiuretikum) im Vergleich zu Furosemid",
        answerD = "Ivabradine (If-Kanal-Inhibitor) bei Sinusrhythmus",
        correctAnswer = 1,
        explanation = "In der EMPHASIS-HF-Studie reduzierte Eplerenon bei HFrEF mit leichten Symptomen (NYHA II) die Rate an kardiovaskulaeren Todesfaellen und Herzinsuffizienz-Hospitalisierungen signifikant um 37% gegenueber Placebo. Eplerenon ist selektiver als Spironolacton und hat weniger antiandrogene Nebenwirkungen.",
        difficulty = 4,
        funFact = "Die 'Viererkombination' bei HFrEF (ACE-Hemmer/ARB/ARNI + Betablocker + MRA + SGLT2-Inhibitor) kann die Mortalitaet verglichen mit alleiniger Diuretikatherapie um ueber 60% reduzieren -- und soll laut Leitlinien so schnell wie moeglich vollstaendig eingeleitet werden."
    ),

    // Question 33 -- ECG: Brugada syndrome
    Question(
        categoryId = 16,
        questionText = "Das Brugada-Syndrom zeigt ein charakteristisches EKG-Muster. Welche Beschreibung entspricht dem Typ-1-Brugada-Muster?",
        answerA = "Koved (kuppelfoermige) ST-Hebung >= 2 mm in V1 und/oder V2 mit negativer T-Welle",
        answerB = "Sattelfoermige ST-Hebung in V1-V3 mit positiver T-Welle",
        answerC = "Nur QT-Verlaengerung in V1-V3 ohne ST-Hebung",
        answerD = "Epsilon-Welle mit negativer T-Welle in V1-V3",
        correctAnswer = 0,
        explanation = "Das diagnostische Typ-1-Brugada-Muster zeigt eine koved (kuppelfoermige) ST-Hebung von mindestens 2 mm in Ableitung V1 und/oder V2 (mit den Elektroden in Standardposition oder eine Rippe hoeher) mit anschliessend negativer T-Welle. Das Typ-2-Muster (sattelfoermig) ist nur diagnostisch, wenn es durch Medikamente in Typ-1 konvertiert werden kann.",
        difficulty = 4,
        funFact = "Das Brugada-Syndrom ist in Suedostasien die haeufigste Ursache des plotzlichen Herztodes bei jungen Maennern ohne strukturelle Herzerkrankung. In den Philippinen wird das Phaenomen als 'Bangungut' ('aufschreien und sterben') bezeichnet -- Fischerfamilien berichteten frueh von jungen gesunden Maennern, die nachts verstarben."
    ),

    // Question 34 -- Neurology: myasthenia gravis
    Question(
        categoryId = 16,
        questionText = "Welches pathophysiologische Prinzip erklaert die Ermuedbarkeit der Muskulatur bei Myasthenia gravis?",
        answerA = "Praejunktionale Hemmung der Acetylcholinfreisetzung durch IgG-Antikoerper",
        answerB = "Postjunktionale Blockade und Zerstoerung von nikotinischen AChR durch IgG-Autoantikoerper, was das sicherheitsintermediale Signal reduziert",
        answerC = "Defekt der Acetylcholinesterase mit zufaelliger Transmitterakkumulation",
        answerD = "Entzuendliche Infiltration der motorischen Endplatte durch T-Lymphozyten",
        correctAnswer = 1,
        explanation = "Bei Myasthenia gravis richten sich IgG-Autoantikoerper gegen den nikotinischen Acetylcholinrezeptor (AChR) an der motorischen Endplatte. Durch Blockade, Internalisierung und komplementvermittelte Zerstoerung der Rezeptoren nimmt der 'Sicherheitsabstand' (safety margin) der neuromuskulaeren Uebertragung ab, was bei wiederholter Stimulation zur Ermuedung fuehrt.",
        difficulty = 4,
        funFact = "Bei etwa 10-15% der Myasthenie-Patienten findet sich ein Thymom, und fast alle Thymom-Patienten entwickeln im Verlauf eine Myasthenie. Deswegen ist eine CT des Thorax zur Thymom-Suche bei jeder Myasthenie-Diagnose obligat."
    ),

    // Question 35 -- Cardiology: aortic regurgitation
    Question(
        categoryId = 16,
        questionText = "Welches klinische Zeichen bei der Aorteninsuffizienz wird durch den hohen Pulsdruck und die rasche Pulsdruckveraenderung erklaert und als 'Corrigan-Puls' bezeichnet?",
        answerA = "Pulsus paradoxus mit Druckabfall > 10 mmHg in der Inspiration",
        answerB = "Wasserhammerartiger, rasch ansteigender und kollabierender Puls durch hohen Pulsdruck",
        answerC = "Doppelgipfliger Puls (Pulsus bisferiens) durch Aortenstenose",
        answerD = "Alternierender Puls (Pulsus alternans) durch schwere Herzinsuffizienz",
        correctAnswer = 1,
        explanation = "Der Corrigan-Puls (auch 'Wasserhammerimpuls') bei Aorteninsuffizienz entsteht durch den massiv erhohten Pulsdruck: In der Systole schnellt der Druck hoch (grosse Vorwaertsvolumen) und kollabiert rasch in der Diastole (Rueckfluss in den LV). Dieser rasche Druckabfall ist an der Arteria radialis taktil als 'Wasserhammerimpuls' spuerbar.",
        difficulty = 4,
        funFact = "Dominic Corrigan, irischer Arzt des 19. Jahrhunderts, beschrieb diesen Puls lange vor dem Echokardiographiezeitalter. Das physikalische Spielzeug 'Wasserball' (aufgehaengter Wasserstab, der schlagartig umschlaegt) wurde zum Namensgeber fuer dieses Pulszeichen."
    ),

    // Question 36 -- Endocrine: acromegaly
    Question(
        categoryId = 16,
        questionText = "Welcher Parameter gilt als Goldstandard fuer die biochemische Kontrolle nach Therapie einer Akromegalie?",
        answerA = "Basales IGF-1 im Normbereich fuer Alter und Geschlecht",
        answerB = "GH-Suppression < 1 ng/mL (oder < 0,4 ng/mL bei ultrasensitiven Assays) im oralen Glukosetoleranztest",
        answerC = "Normalisierung des Serumcortisols nach ACTH-Stimulation",
        answerD = "Basales GH < 5 ng/mL bei einer einmaligen Messung",
        correctAnswer = 1,
        explanation = "Als Remissionskriterium nach Akromegalie-Therapie gilt: GH-Nadir im oGTT < 1 ng/mL (bzw. < 0,4 ng/mL mit ultrasensitiven Assays) UND normales alters- und geschlechtsadaptiertes IGF-1. Beide Parameter muessen erfullt sein -- IGF-1 allein genuegt nicht, da es bei aktiver Erkrankung normal sein kann.",
        difficulty = 4,
        funFact = "Unbehandelte Akromegalie verkuerzt die Lebenserwartung um durchschnittlich 10 Jahre, vor allem durch kardiovaskulaere Komplikationen (Kardiomyopathie, Hypertonie) und erhoehtes Kolonkarzinomrisiko. Somatostatin-Analoga (Octreotid, Lanreotid) sind die wichtigste medikamentoese Therapie."
    ),

    // Question 37 -- Neurology: multiple sclerosis
    Question(
        categoryId = 16,
        questionText = "Welches MRT-Kriterium nach McDonald (2017) erlaubt die Diagnose MS bereits nach einem einzigen klinischen Schubereignis?",
        answerA = "Mindestens 1 Herd im Corpus callosum",
        answerB = "Gleichzeitiger Nachweis kontrastmittelaufnehmender und nicht-aufnehmender Laesionen (zeitliche Dissemination in einem einzigen MRT)",
        answerC = "Mehr als 9 T2-Laesionen supratentoriell",
        answerD = "Vorhandensein von oligoklonalem IgG im Serum",
        correctAnswer = 1,
        explanation = "Die McDonald-Kriterien 2017 erlauben die zeitliche Dissemination (DIT) in einem einzigen MRT nachzuweisen, wenn gleichzeitig kontrastmittelaufnehmende (akute) und nicht-aufnehmende (chronische) T2-Laesionen vorhanden sind. Dies ermoeglicht die Fruhdiagnose MS nach einem CIS (klinisch isoliertem Syndrom).",
        difficulty = 4,
        funFact = "Oligoklonale Banden im Liquor sind bei > 95% der MS-Patienten nachweisbar und spiegeln eine intrathekale IgG-Synthese wider. Sie sind aber nicht MS-spezifisch -- auch bei ZNS-Infektionen oder anderen Entzuendungen konnen sie auftreten."
    ),

    // Question 38 -- Cardiology: cardiac resynchronization therapy
    Question(
        categoryId = 16,
        questionText = "Welcher Patientengruppe hat den groessten Nutzen von einer kardialen Resynchronisationstherapie (CRT)?",
        answerA = "HFrEF (EF < 35%), NYHA III-IV, Sinusrhythmus, QRS-Dauer >= 150 ms mit LBBB-Morphologie",
        answerB = "HFpEF (EF >= 50%), NYHA II, permanentes Vorhofflimmern",
        answerC = "HFrEF (EF < 35%), NYHA I, schmaler QRS < 120 ms",
        answerD = "HFmrEF (EF 40-49%), NYHA IV, Rechtsschenkelblock",
        correctAnswer = 0,
        explanation = "Die staerkste Evidenz fuer CRT besteht bei Patienten mit HFrEF (EF < 35%), NYHA III-IV, Sinusrhythmus und einem QRS-Komplex >= 150 ms mit LBBB-Morphologie. Diese Gruppe profitiert von reduzierter Mortalitaet, weniger Hospitalisierungen und reverser Remodeling.",
        difficulty = 4,
        funFact = "CRT bewirkt durch simultane Stimulation beider Ventrikel eine Resynchronisierung der ventrikulaeren Kontraktionssequenz. In etwa 30% der CRT-implantierten Patienten kommt es zu einem spektakulaeren 'Reverse Remodeling' -- die EF steigt deutlich an und der LV wird kleiner, quasi eine Erholung des Herzens."
    ),

    // Question 39 -- Neurology: Guillain-Barre syndrome
    Question(
        categoryId = 16,
        questionText = "Welches ist das wichtigste diagnostische Merkmal im Liquorbefund bei Guilain-Barre-Syndrom (GBS)?",
        answerA = "Massive Leukozytose > 100 Zellen/mm3 mit neutrophiler Pleozytose",
        answerB = "Albumino-zytologische Dissoziation: erhoehtes Gesamtprotein bei normaler Zellzahl",
        answerC = "Erhoehte Laktatwerte > 4 mmol/L als Zeichen eines ZNS-Infekts",
        answerD = "Oligoklonale Banden als Zeichen intrathekaler Immunaktivierung",
        correctAnswer = 1,
        explanation = "Das GBS zeigt die klassische albumino-zytologische Dissoziation im Liquor: deutlich erhoehtes Gesamtprotein (oft > 1 g/L) bei normaler oder nur leicht erhoehter Zellzahl. Diese Konstellation entsteht durch Entzuendung und Demyelinisierung der Nervenwurzeln mit Proteinleckage, ohne wesentliche Zellinfiltration.",
        difficulty = 4,
        funFact = "GBS ist weltweit die haeufigste Ursache einer akuten schlaffen Laehmung. Die Therapie mit intravenoesen Immunglobulinen (IVIG) oder Plasmapherese zeigt gleichwertige Ergebnisse. Kortikosteroide sind beim GBS dagegen nicht wirksam und koennen sogar schaedlich sein -- ein fuer viele Arzte kontraintuitives Ergebnis."
    ),

    // Question 40 -- Endocrine: Cushing syndrome
    Question(
        categoryId = 16,
        questionText = "Welcher Test wird initial zum Ausschluss eines Cushing-Syndroms eingesetzt und basiert auf dem Prinzip der Kortikosteroidsuppression?",
        answerA = "Hochdosis-Dexamethason-Suppressionstest (8 mg ueber 2 Tage)",
        answerB = "Niederdosis-Dexamethason-Suppressionstest (1 mg Overnight) mit Cortisolmessung am naechsten Morgen",
        answerC = "CRH-Stimulationstest mit ACTH-Messung",
        answerD = "24h-Urin-Cortisol als alleiniger Screeningtest",
        correctAnswer = 1,
        explanation = "Der 1-mg-Overnight-Dexamethason-Suppressionstest ist der bevorzugte Screeningtest: 1 mg Dexamethason wird um 23 Uhr genommen, und am naechsten Morgen (8 Uhr) wird das Serumcortisol gemessen. Ein Wert > 50 nmol/L (1,8 mcg/dL) gilt als nicht-supprimiert und muss weiter abgeklaert werden.",
        difficulty = 4,
        funFact = "Harvey Cushing, der Namenspatron des Syndroms, war einer der Vater der modernen Neurochirurgie. Er beschrieb das Syndrom 1932 und verband erstmals Hypophysenadenom, Nebennierenadenom und die charakteristischen Symptome -- eine Meisterleistung klinischer Beobachtung ohne moderne Bildgebung."
    ),

    // Question 41 -- Cardiology: pulmonary hypertension
    Question(
        categoryId = 16,
        questionText = "Ab welchem mittleren pulmonal-arteriellen Druck (mPAP) in der Rechtsherzkatheteruntersuchung wird eine pulmonale Hypertonie definiert?",
        answerA = "mPAP > 20 mmHg in Ruhe",
        answerB = "mPAP > 25 mmHg in Ruhe",
        answerC = "mPAP > 30 mmHg bei Belastung",
        answerD = "Systolischer PAP > 40 mmHg im Echo",
        correctAnswer = 0,
        explanation = "Gemaess der aktualisierten ESC/ERS-Leitlinie (2022) wird die pulmonale Hypertonie neu als mPAP > 20 mmHg im Rechtsherzkatheter (statt frueherer > 25 mmHg) definiert. Gleichzeitig wird der pulmonale vaskulaere Widerstand (PVR) >= 2 Wood-Einheiten gefordert.",
        difficulty = 4,
        funFact = "Der Rechtsherzkatheter ist der Goldstandard fuer die Diagnose und Therapiemonitoring der pulmonalen arteriellen Hypertonie (PAH). Die Einfuehrung von Prostanoiden, Endothelin-Rezeptorantagonisten und PDE-5-Inhibitoren hat die Prognose dieser fruehers meist fatal verlaufenden Erkrankung deutlich verbessert."
    ),

    // Question 42 -- Neurology: transverse myelitis
    Question(
        categoryId = 16,
        questionText = "Eine junge Frau entwickelt innerhalb von 24 Stunden eine Querschnittlaehmung, Blasenstoerungen und ein sensibles Niveau bei Th6. Im MRT findet sich eine langstreckige Myelitislaesion ueber > 3 Wirbelsaeulensegmente. An welche Diagnose muss vorrangig gedacht werden?",
        answerA = "Multiple Sklerose mit primaer progredientem Verlauf",
        answerB = "Neuromyelitis-optica-Spektrum-Erkrankung (NMOSD), AQP4-IgG",
        answerC = "Akute disseminierte Enzephalomyelitis (ADEM)",
        answerD = "Spinale Ischaemie bei Aortendissektion",
        correctAnswer = 1,
        explanation = "Die longitudinal extensive transverse Myelitis (LETM) -- Laesion ueber >= 3 Wirbelsaeulensegmente -- ist das klassische MRT-Merkmal der NMOSD. AQP4-IgG (Antikoerper gegen Aquaporin-4) ist bei etwa 70-80% der NMOSD-Patienten positiv und ein hochspezifischer Marker.",
        difficulty = 4,
        funFact = "Aquaporin-4-Antikoerper bei NMOSD zielen auf Astrozyten-Wasserkanaele am Blut-Hirn-Schranke-Interface. Die NMOSD war lange als MS-Variante fehlklassifiziert -- dieser Irrtum hatte therapeutische Konsequenzen, da einige MS-Medikamente (Interferone, Natalizumab) die NMOSD verschlechtern koennen."
    ),

    // Question 43 -- Endocrine: hyperthyroidism pharmacology
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert, warum Thiamazol (Methimazol) nicht bei thyreotoxischer Krise als alleinige Therapie ausreicht?",
        answerA = "Thiamazol hemmt zwar die Neusynthese, hat aber keinen Einfluss auf die bereits gespeicherten Schilddruesenhormone",
        answerB = "Thiamazol wird durch hohe Hormonspiegel schneller abgebaut",
        answerC = "Bei der Krise ist die Thiamazol-Resorption aus dem Darm gestort",
        answerD = "Thiamazol erhoht paradoxerweise die T3-Konversion in der Peripherie",
        correctAnswer = 0,
        explanation = "Thiamazol hemmt die thyroidale Peroxidase und damit die Neusynthese von Schilddruesenhormonen, hat aber keinen Effekt auf bereits synthetisierte und im Follikel gespeicherte Hormone. Bei der thyreotoxischen Krise werden deswegen zusaetzlich Jodid-Loesungen (Plummerung) eingesetzt, um die Hormonfreisetzung zu blockieren.",
        difficulty = 4,
        funFact = "Das Wolff-Chaikoff-Effekt beschreibt die paradoxe Hemmung der Schilddruesenfunktion durch hohe Joddosen: Bei exzessiver Jodzufuhr reduziert die Schilddruese voruebergehend die Hormonsynthese. Dieser Mechanismus wird therapeutisch bei der thyreotoxischen Krise (Lugol-Loesung) und prophylaktisch bei radioaktiver Kontamination (Jodtabletten) genutzt."
    ),

    // Question 44 -- Cardiology: HOCM
    Question(
        categoryId = 16,
        questionText = "Bei hypertropher obstruktiver Kardiomyopathie (HOCM) nimmt das Herzgeraeusch bei welchen Maenoeuvern typischerweise zu?",
        answerA = "Hinhocken (squatting) und Anheben der Beine",
        answerB = "Valsalva-Maenoveur und Aufstehen aus dem Liegen",
        answerC = "Handgrip-Test und passive Beinerhebung",
        answerD = "Tiefer Inspiration und koerperliche Belastung im Liegen",
        correctAnswer = 1,
        explanation = "Das systolische Geraeusch bei HOCM verstaerkt sich bei Maenoeuvern, die das Ventrikelvolumen verringern (Valsalva, Aufstehen): Weniger Fuellung fuehrt zu starkerer systolischer Obstruktion. Im Gegensatz dazu nimmt das Geraeusch bei Squat oder Beinerhebung (mehr Vorlast, mehr Fuellung) ab.",
        difficulty = 4,
        funFact = "HOCM ist die haeufigste Ursache des plotzlichen Herztods bei jungen Sportlern unter 35 Jahren. Die SAM (systolic anterior motion) der Mitralklappe -- ein Vorwaertsziehen des anterioren Mitralsegels in den Ausflusstrakt -- ist der Schlusselmechanismus der dynamischen Obstruktion."
    ),

    // Question 45 -- Neurology: Wernicke encephalopathy
    Question(
        categoryId = 16,
        questionText = "Die klassische klinische Trias der Wernicke-Enzephalopathie umfasst welche drei Symptome?",
        answerA = "Demenz, Ataxie und peripheres Oedeme",
        answerB = "Ophthalmoplegie, Ataxie und Verwirrtheit/Bewusstseinsstoerung",
        answerC = "Nystagmus, Polyneuropathie und Psychose",
        answerD = "Komazensuales, Kopfschmerz und Meningismus",
        correctAnswer = 1,
        explanation = "Die Wernicke-Enzephalopathie (Thiamin-Mangel) zeigt klassisch die Trias: Ophthalmoplegie (Augenmuskelparesen, Nystagmus), Gangataxie und Verwirrtheitszustand/Bewusstseinsveraenderung. Alle drei Symptome sind bei der Diagnosestellung oft nicht vollstaendig ausgepraegt.",
        difficulty = 4,
        funFact = "Thiamin (Vitamin B1) muss beim Verdacht auf Wernicke-Enzephalopathie IMMER VOR der Glukosegabe gegeben werden: Glucose ohne Thiamin kann einen latenten Thiaminmangel akut dekompensieren und die Enzephalopathie massiv verschlechtern. 'Thiamin first, glucose second' ist eine der wichtigsten Notfallregeln."
    ),

    // Question 46 -- Cardiology: long QT inheritance
    Question(
        categoryId = 16,
        questionText = "Das Romano-Ward-Syndrom (Long-QT-Syndrom Typ 1-3) hat ein anderes Vererbungsmuster als das Jervell-Lange-Nielsen-Syndrom. Was ist der Unterschied?",
        answerA = "Romano-Ward: autosomal dominant, nur Herzarrhythmien; Jervell-Lange-Nielsen: autosomal rezessiv, Herzarrhythmien und kongenitale Taubheit",
        answerB = "Romano-Ward: autosomal rezessiv ohne Taubheit; Jervell-Lange-Nielsen: X-chromosomal mit Taubheit",
        answerC = "Romano-Ward betrifft nur KCNQ1-Mutationen; Jervell-Lange-Nielsen nur KCNH2",
        answerD = "Beide haben autosomal dominantes Muster, aber unterschiedliche Penetranz",
        correctAnswer = 0,
        explanation = "Romano-Ward ist autosomal dominant und manifestiert sich nur mit Herzarrhythmien (Long QT). Jervell-Lange-Nielsen ist autosomal rezessiv (homozygote KCNQ1 oder KCNE1 Mutation) und kombiniert Long-QT mit kongenitaler sensorineuraler Taubheit, da KCNQ1 auch in der Stria vascularis des Innenohrs exprimiert wird.",
        difficulty = 4,
        funFact = "Die Stria vascularis des Innenohrs benoetigt die gleichen Kaliumkanaele (KCNQ1) wie das Herz fuer die Endolymphproduktion. Deswegen fuehrt der homozygote Defekt zur Taubheit -- ein faszinierendes Beispiel, wie dasselbe Ionenkanal-Gen in zwei voellig verschiedenen Organen kritische Funktionen hat."
    ),

    // Question 47 -- Endocrine: hyperparathyroidism
    Question(
        categoryId = 16,
        questionText = "Welches Organ ist das primaere Zielgewebe von Parathormon (PTH) und durch welchen Second-Messenger wirkt PTH auf Osteoblasten?",
        answerA = "Niere und Darm, Second-Messenger: IP3/DAG (Phospholipase C-Weg)",
        answerB = "Knochen und Niere, Second-Messenger: cAMP (Adenylatzyklase-Weg)",
        answerC = "Leber und Darm, Second-Messenger: JAK-STAT-Signalweg",
        answerD = "Knochen und Nebenschilddruese, Second-Messenger: Ca2+-Calmodulin",
        correctAnswer = 1,
        explanation = "PTH wirkt primaer an Niere und Knochen ueber einen PTH/PTHrP-Rezeptor (PTHR1), der an Adenylatzyklase koppelt. Die cAMP-Erhoehung aktiviert Proteinkinase A und mediiert die Effekte auf Osteoblasten (indirekte Osteoklastenaktivierung ueber RANKL) und renale Kalziumrueckresorption.",
        difficulty = 4,
        funFact = "PTHrP (PTH-related Protein) wird von vielen Tumoren produziert und ist der wichtigste Mediator der Hyperkalaemie bei malignen Erkrankungen (humoral hypercalcemia of malignancy). Es bindet denselben Rezeptor wie PTH, kann aber nicht im Standard-PTH-Assay gemessen werden -- Fehldiagnosen sind moeglich."
    ),

    // Question 48 -- Neurology: normal pressure hydrocephalus
    Question(
        categoryId = 16,
        questionText = "Die Hakim-Adams-Trias des Normaldruckhydrozephalus (NPH) umfasst welche drei Symptome in welcher typischen Reihenfolge des Auftretens?",
        answerA = "Zuerst Demenz, dann Inkontinenz, dann Gangstorung",
        answerB = "Zuerst Gangstorung (magnetisches Gangbild), dann Inkontinenz, dann Demenz (frontal)",
        answerC = "Gleichzeitiges Auftreten aller drei Symptome ohne Praedilektion",
        answerD = "Zuerst Demenz, dann Gangstorung, zuletzt Inkontinenz",
        correctAnswer = 1,
        explanation = "Beim NPH tritt typischerweise zuerst die charakteristische Gangstorung auf (breitbasig, kleinschrittig, 'magnetisches Gangbild', als ob die Fuesse am Boden kleben), gefolgt von Harninkontinenz und zuletzt subkortikaler/frontaler Demenz. Diese Sequenz gilt als klinischer Merksatz 'Wet, Wobbly, Wacky' -- aber in umgekehrter Reihenfolge zu Alzheimer.",
        difficulty = 4,
        funFact = "Der Liquor-Ablasstest (Entnahme von 30-50 ml Liquor mit anschliessender Ganganalyse) kann beim NPH eine kurzfristige Verbesserung zeigen und gilt als positiver praediktiver Faktor fuer das Ansprechen auf eine ventrikuloperitoneal Shunt-Anlage -- mit Erfolgsraten von bis zu 80%."
    ),

    // Question 49 -- Cardiology: troponin interpretation
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet den Typ-1-Myokardinfarkt vom Typ-2-Myokardinfarkt gemaess der 4. Universellen Myokardinfarkt-Definition?",
        answerA = "Typ-1 ist durch koronare Plaqueruptur/Thrombose verursacht; Typ-2 durch Angebot-Nachfrage-Imbalance ohne Plaqueruptur (z.B. Tachyarrhythmie, Hypotension)",
        answerB = "Typ-1 hat hoeheres Troponin als Typ-2",
        answerC = "Typ-1 tritt nur bei KHK-Patienten auf; Typ-2 nur bei kardio-toxischen Erkrankungen",
        answerD = "Typ-2 ist ein STEMI, Typ-1 ein NSTEMI per Definition",
        correctAnswer = 0,
        explanation = "Typ-1-Infarkt: spontane koronare Plaqueruptur/-erosion mit Thrombus und konsekutivem Gefaessverschluss. Typ-2-Infarkt: Myokardschaeden durch sauerstofflieferndes Missverhaltnis ohne akute Plaqueruptur -- z.B. bei schwerer Tachyarrhythmie, Hypotension, Anaeamie oder Vasospasmus.",
        difficulty = 4,
        funFact = "Die klinische Unterscheidung von Typ-1 und Typ-2 ist therapeutisch entscheidend: Typ-1 erfordert sofortige Koronarangiographie und Reperfusion, waehrend Typ-2 primae die Behandlung der Grundursache (z.B. Herzfrequenzkontrolle, Bluttransfusion) erfordert. Troponin allein kann diese Unterscheidung nicht treffen."
    ),

    // Question 50 -- Endocrine: diabetes insipidus
    Question(
        categoryId = 16,
        questionText = "Wie unterscheidet man klinisch und laborchemisch zuverlassig zwischen einem zentralen und einem nephrogenen Diabetes insipidus (DI)?",
        answerA = "Beim zentralen DI ist die Serum-Osmolalitaet erniedrigt, beim nephrogenen erhoehen",
        answerB = "Durstversuch mit anschliessender Desmopressin-Gabe: Beim zentralen DI steigt die Urinosmolalitaet nach Desmopressin > 50% an, beim nephrogenen < 10%",
        answerC = "Beim zentralen DI sind ADH-Spiegel erhoehen, beim nephrogenen erniedrigt",
        answerD = "Nephrogenener DI zeigt obligat Hyperkalaemie, zentraler DI nicht",
        correctAnswer = 1,
        explanation = "Der Wasserdeprivationstest (Durstversuch) mit anschliessender Desmopressin (DDAVP)-Gabe ist der Standardtest: Bei zentralem DI ist ADH-Mangel die Ursache -- exogenes Desmopressin wirkt an der intakten Niere und steigert die Urinosmolalitaet > 50%. Bei nephrologischem DI ist die Niere ADH-resistent -- Desmopressin zeigt kaum Effekt.",
        difficulty = 4,
        funFact = "Das Aquaporin-2 (AQP2)-Kanalprotein im Sammelrohr der Niere ist das Haupttarget von ADH/Vasopressin. Bei nephrologischem DI sind entweder der V2-Rezeptor (AVPR2, X-chromosomal) oder AQP2 selbst (autosomal rezessiv) mutiert -- deswegen betrifft die X-chromosomale Variante fast ausschliesslich Maenner."
    )

)
