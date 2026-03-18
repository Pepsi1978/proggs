package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsExpert5(): List<Question> = listOf(

    // Question 1 -- Radiology: Air Bronchogram Sign
    Question(
        categoryId = 16,
        questionText = "Was beschreibt das radiologische Zeichen 'Air Bronchogram' und bei welcher Erkrankung ist es typischerweise zu sehen?",
        answerA = "Luft in den Pleurahoehlen, typisch bei Pneumothorax",
        answerB = "Luftgefuellte Bronchien, die durch konsolidiertes oder atelektatisches Lungenparenchym sichtbar werden, typisch bei lobaerem Pneumonie oder ARDS",
        answerC = "Erweiterung der Bronchien mit Luft-Fluessigkeits-Spiegeln, typisch bei Bronchiektasen",
        answerD = "Verdickung der Bronchialwaende im CT, typisch bei chronischer Bronchitis",
        correctAnswer = 1,
        explanation = "Das Air-Bronchogramm entsteht, wenn die Alveolen mit Fluessigkeit oder Exsudat gefuellt sind, die Atemwege aber noch Luft enthalten -- die luftgefuellten Bronchien heben sich dadurch vom undurchsichtigen Parenchym ab. Es ist charakteristisch fuer lobaere Pneumonie, ARDS und Lungenoedeme.",
        difficulty = 4,
        funFact = "Das Fehlen eines Air-Bronchogramms bei einer Konsolidierung spricht eher fuer eine obstruktive Atelektase oder einen Tumor als fuer eine infektioese Ursache."
    ),

    // Question 2 -- Radiology: Butterfly Sign
    Question(
        categoryId = 16,
        questionText = "Das 'Butterfly'- oder 'Fledermausfluegelzeichen' im Thorax-Roentgen beschreibt welches Muster?",
        answerA = "Bilateral symmetrische, hilusnahe Verschattungen bei kardiogenem Lungenoedeme, die die Lungen wie Schmetterlingsfluegelabschnitte ausfuellen",
        answerB = "Beidseitige Rippenbrueche mit sternfoermigem Muster bei Thoraxtrauma",
        answerC = "Schmetterlingsfoemige Verdichtung eines einzelnen Oberlappens bei Tuberkulose",
        answerD = "Symmetrische Kerley-B-Linien bei interstitieller Lungenfibrose",
        correctAnswer = 0,
        explanation = "Das Butterfly-Zeichen beschreibt das bilaterale, hilusnahe, symmetrische Oedemmuster beim kardiogenen Lungenoedeme -- die perihilaeren Verschattungen erinnern an Schmetterlings- oder Fledermausfluegelumrisse. Es entsteht durch die erhoehte hydrostische Druckbelastung der perihilaeren Lymphbahnen.",
        difficulty = 4,
        funFact = "Das Butterfly-Muster unterscheidet kardiogenes Lungenoedeme oft von nichtkardiogegenem ARDS, das eher diffuse, periphere Infiltrate zeigt."
    ),

    // Question 3 -- Radiology: Codman Triangle
    Question(
        categoryId = 16,
        questionText = "Das 'Codman-Dreieck' im konventionellen Roentgen eines Knochens ist ein Hinweiszeichen auf was?",
        answerA = "Benignen enchondroiden Tumor mit Matrixverkalkung",
        answerB = "Aggressives, malignes Knochentumor -- klassisch beim Osteosarkom -- durch abgehobenes Periost mit reaktiver Knochenneubildung",
        answerC = "Stressreaktion mit lamellaerer Periostreaktion bei Ueberbelastung",
        answerD = "Haematogene Osteomyelitis mit subperiostaler Eiteransammlung",
        correctAnswer = 1,
        explanation = "Das Codman-Dreieck entsteht, wenn ein schnell wachsender Tumor das Periost von der Kortikalis abhebt; der Bereich zwischen normaler Kortikalis, abgehobenem Periost und Tumor bildet ein dreieckiges, roentgendichtes Muster. Es ist ein klassisches Zeichen aggressiver Knochentumoren wie dem Osteosarkom.",
        difficulty = 4,
        funFact = "Ernest Amory Codman beschrieb dieses Zeichen Anfang des 20. Jahrhunderts -- er gilt als Pionier der systematischen Knochentumorchirurgie und der Qualitaetssicherung in der Chirurgie."
    ),

    // Question 4 -- Contrast Agents: Gadolinium Risks
    Question(
        categoryId = 16,
        questionText = "Welche schwerwiegende Nebenwirkung ist spezifisch fuer gadoliniumhaltige MRT-Kontrastmittel bei Patienten mit schwerer Niereninsuffizienz (GFR < 30)?",
        answerA = "Kontrastmittelinduzierte Nephropathie mit akutem Nierenversagen",
        answerB = "Nephrogene systemische Fibrose (NSF) mit Verdickung und Fibrosierung von Haut, Gelenken und inneren Organen",
        answerC = "Anaphylaktischer Schock durch IgE-vermittelte Ueberempfindlichkeit gegen Gadolinium",
        answerD = "Thyreotoxische Krise durch jodinduzierten Hyperthyreoidismus",
        correctAnswer = 1,
        explanation = "Nephrogene systemische Fibrose (NSF) ist eine seltene, aber schwere Systemerkrankung, die ausschliesslich bei Patienten mit stark eingeschraenkter Nierenfunktion nach Exposition gegenueber bestimmten (linearen) gadoliniumhaltigen Kontrastmitteln auftritt. Freies Gadolinium-Ion, das bei Dissoziation des Chelats entsteht, fuehrt zur Fibroblastenaktivierung.",
        difficulty = 4,
        funFact = "Seit der Einfuehrung makrozyklischer Gadoliniumchelate (z.B. Gadoteridol, Gadobutrol) und strikter Kontraindikation bei GFR < 30 ml/min ist die NSF-Inzidenz auf nahezu null gesunken."
    ),

    // Question 5 -- Contrast Agents: Iodine Allergy
    Question(
        categoryId = 16,
        questionText = "Ein Patient mit bekannter schwerer Reaktion auf jodhaltige Kontrastmittel benoetigt ein Kontrastmittel-CT. Welches Vorgehen ist korrekt?",
        answerA = "Kontrastmittel ist absolut kontraindiziert -- MRT ohne Gadolinium durchfuehren",
        answerB = "Praemedikation mit Kortikosteroiden und Antihistaminika 12-13 Stunden vor Gabe, Bereitschaft fuer Notfallbehandlung",
        answerC = "Nichtionisches Kontrastmittel verwenden -- keine Praemedikation noetig da keine Kreuzallergie mit ionischen Mitteln",
        answerD = "Auf Megluminkontrast wechseln, da Jodreaktionen nur gegen Natriumsalze gerichtet sind",
        correctAnswer = 1,
        explanation = "Bei bekannter schwerer Kontrastmittelreaktion (Anaphylaxie-Grad III/IV) wird eine Praemedikation mit Kortikosteroiden (z.B. Prednisolon 32 mg oral 13 und 1 Stunde vor KM-Gabe) und Antihistaminika empfohlen, sofern die Untersuchung unverzichtbar ist. Notfallausstattung muss bereitstehen.",
        difficulty = 4,
        funFact = "Jodreaktionen auf Kontrastmittel sind keine echten Jod-Allergien -- sie richten sich gegen das Gesamtmolekuel des Kontrastmittels. Meeresfruechteallergie ist kein eigenstaendiger Risikofaktor fuer Kontrastmittelreaktionen."
    ),

    // Question 6 -- Interventional Radiology: Embolization
    Question(
        categoryId = 16,
        questionText = "Bei welcher klinischen Indikation wird eine transarterielle Embolisation (TAE) bevorzugt eingesetzt?",
        answerA = "Chronische venoeser Insuffizienz der unteren Extremitaeten",
        answerB = "Aktive arterielle Blutung (z.B. Traumablutung, Gastrointestinalblutung, Haemoptoe) oder Tumorembolisation bei Hypernephrom",
        answerC = "Pulmonale Hypertonie durch chronische Thromboembolie",
        answerD = "Portale Hypertension durch Leberzirrhose",
        correctAnswer = 1,
        explanation = "Die TAE stoppt aktive arterielle Blutungen durch selektive Katheterisierung des blutenden Gefaesses und Okklusion mit Emboli (Coils, Partikel, Alkohol). Sie ist auch bei hypervaskularisierten Tumoren wie Nierenzellkarzinomen zur Devaskularisation vor Resektion indiziert.",
        difficulty = 4,
        funFact = "Bei traumatischen Milzrupturen ermoeglicht die Milzembolisation in etwa 85-90% der Faelle eine milzerhaltende Behandlung -- frueherer Standard war die Notfallsplenektomie."
    ),

    // Question 7 -- Interventional Radiology: TACE
    Question(
        categoryId = 16,
        questionText = "Was ist das Wirkprinzip der transarteriellen Chemoembolisation (TACE) beim hepatozellulaeren Karzinom (HCC)?",
        answerA = "Direkte intratumorale Injektion von Chemotherapeutika per CT-Guidance",
        answerB = "Kombinierte Applikation von Chemotherapeutikum und Embolisationsmaterial ueber den Tumorgefaessast -- Ischaemie plus lokale Chemotherapiekonzentration",
        answerC = "Systemische Chemotherapie kombiniert mit radiologisch gesteuerter Hyperthermie",
        answerD = "Embolisation der Pfortader zur Hypertrophie des Restlebergewebes vor Resektion",
        correctAnswer = 1,
        explanation = "Bei der TACE wird ein Chemotherapeutikum (meist Doxorubicin) in Kombination mit einem Embolusmaterial (Lipiodol oder Drug-Eluting Beads) selektiv in den Tumorgefaessast injiziert. Dies erzeugt eine lokale Hochkonzentration des Zytostatikums bei gleichzeitiger ischaemischer Nekrose durch Gefaessverschluss.",
        difficulty = 4,
        funFact = "Drug-Eluting Beads (DEB-TACE) setzen das Chemotherapeutikum ueber mehrere Wochen kontrolliert frei und haben geringere systemische Nebenwirkungen als konventionelle TACE -- die klinische Ueberlegenheit ist aber noch nicht eindeutig belegt."
    ),

    // Question 8 -- Interventional Radiology: Biopsy Guidance
    Question(
        categoryId = 16,
        questionText = "Welches bildgebende Verfahren wird bevorzugt fuer die Biopsie tief gelegener abdominaler Laesionen (z.B. Pankreaskopfmasse) eingesetzt?",
        answerA = "Fluoroskopie -- wegen Echtzeit-Bildgebung und geringstem Strahlenrisiko",
        answerB = "CT-Guidance -- praezise Nadelpositionierung in Weichteillaesionen, unabhaengig von Gasartefakten",
        answerC = "MRT-Guidance -- als einzige Methode ohne Strahlenbelastung mit Weichteilkontrast",
        answerD = "Roentgen-Durchleuchtung mit Kontrastmittelinjektion in den Pankreasgang",
        correctAnswer = 1,
        explanation = "Die CT-gesteuerte Biopsie ist Standard fuer tief gelegene abdominale Laesionen, da sie eine exzellente raeumliche Aufloesung bietet, Darmgas ueberbrueckt und die Nadelspitze praezise in Echtzeit dokumentiert werden kann. Alternativ wird zunehmend Endosonographie (EUS-FNA) fuer Pankreaslaesionen eingesetzt.",
        difficulty = 4,
        funFact = "Die EUS-gesteuerte Feinnadel-Aspiration (FNA) hat fuer Pankreaslaesionen eine Sensitivitaet von ueber 85% und vermeidet die Strahlenbelastung -- sie hat die CT-gesteuerte Biopsie bei vielen Pankreaslaesionen weitgehend abgeloest."
    ),

    // Question 9 -- Surgery: Whipple Procedure
    Question(
        categoryId = 16,
        questionText = "Welche Strukturen werden bei einer klassischen Whipple-Operation (pyloruserhaltend vs. standard) reseziert?",
        answerA = "Nur Pankreaskopf und Duodenum, Magenauslass bleibt erhalten",
        answerB = "Pankreaskopf, Duodenum, Teile des Magenausgangs (distal 1/3 Magen), Gallenblase, distaler Choledochus -- bei Standard-Whipple; Pylorus bleibt bei pyloruserhaltendem Whipple",
        answerC = "Gesamtpankreas, Milz, Duodenum und distaler Magen (totale Pankreatektomie)",
        answerD = "Nur Pankreaskopf und Gallenblase bei isoliertem Pankreaskopftumor",
        correctAnswer = 1,
        explanation = "Die klassische Whipple-Prozedur (Kausch-Whipple) reseziert Pankreaskopf, Duodenum, distales Magenantrum, Gallenblase und distalen Choledochus mit anschliessender Rekonstruktion durch Pankreatikojejunostomie, Hepatikojejunostomie und Gastrojejunostomie. Die pyloruserhaltende Variante belaesst den Magenausgang.",
        difficulty = 4,
        funFact = "Allen Whipple beschrieb die Prozedur 1935 in drei Schritten ueber mehrere Sitzungen -- heute wird sie als Eingriff durchgefuehrt. Die perioperative Mortalitaet sank von ueber 25% auf unter 3% in erfahrenen Zentren."
    ),

    // Question 10 -- Surgery: Nissen Fundoplication
    Question(
        categoryId = 16,
        questionText = "Bei welcher Indikation wird eine Nissen-Fundoplikatio durchgefuehrt und was ist das operative Prinzip?",
        answerA = "Achalasie -- Spaltung des unteren Oesophagussphinkters durch laparoskopische Heller-Myotomie",
        answerB = "Gastrooesophageale Refluxkrankheit (GERD) -- 360-Grad-Umschlingung des unteren Oesophagus mit Magenanteil zur Verstaerkung des unteren Sphinkters",
        answerC = "Magenulkus -- Resektion der Ulkusbasis mit Pyloroplastik",
        answerD = "Hiatushernie -- alleinige Reposition des Magens ohne Sphinkterrekonstruktion",
        correctAnswer = 1,
        explanation = "Die Nissen-Fundoplikatio ist die Standardoperation bei konservativ therapierefraktaerem GERD. Der Magenfundus wird laparoskopisch 360 Grad um den unteren Oesophagus gewickelt, was den basalen Druck des unteren Oesophagussphinkters erhoeht und Reflux verhindert.",
        difficulty = 4,
        funFact = "Rudolf Nissen fuehrte die erste Fundoplikatio 1936 durch -- sein erster Patient war Albert Einstein, bei dem er eine Fundoplikatio im Rahmen einer anderen Bauchoperation durchfuehrte."
    ),

    // Question 11 -- Surgery: Sleeve Gastrectomy
    Question(
        categoryId = 16,
        questionText = "Durch welchen Mechanismus fuehrt die Schlauchmagenoperation (Sleeve Gastrectomy) zur Gewichtsreduktion?",
        answerA = "Ausschliesslich durch Restriktion -- der kleine Restmagen ermoeglichen nur geringe Nahrungsmengen",
        answerB = "Restriktion plus hormonelle Wirkung -- Entfernung des Ghrelinproduzierenden Magenabschnitts (Fundus) senkt den Hunger-Hormonspiegel",
        answerC = "Malabsorption durch Verkuerzung der Darmpassage nach Gastric-Bypass-Anastomose",
        answerD = "Ausschliesslich psychologisch -- kleineres Magenvolumen veraendert das Essverhalten",
        correctAnswer = 1,
        explanation = "Die Schlauchmagenoperation wirkt zweigleisig: Zum einen wird das Magenvolumen auf ca. 80-150 ml reduziert (Restriktion). Zum anderen wird der Fundus entfernt, der den groessten Teil des appetitanregenden Hormons Ghrelin produziert -- Ghrelinspiegel sinken postoperativ deutlich.",
        difficulty = 4,
        funFact = "Die Sleeve Gastrectomy wurde urspruenglich als erste Stufe einer zweizeitigen Bypass-Operation bei Hochrisikopatienten entwickelt -- viele Chirurgen stellten jedoch fest, dass sie allein ausreichend wirksam war, und sie wurde zum eigenstaendigen Standardverfahren."
    ),

    // Question 12 -- Intensive Care: APACHE Score
    Question(
        categoryId = 16,
        questionText = "Was misst der APACHE-II-Score auf der Intensivstation und woraus setzt er sich zusammen?",
        answerA = "Schmerzintensitaet und Sedierungstiefe -- aus NRS und Ramsay-Score",
        answerB = "Schwere der Erkrankung und Sterberisiko -- aus 12 physiologischen Variablen, Alter und chronischen Erkrankungen in den ersten 24 Stunden",
        answerC = "Beatmungsadaequanz -- aus Tidalvolumen, PEEP und FiO2-Bedarf",
        answerD = "Ernaehrungsstatus -- aus Koerpergewicht, Albumin und Stickstoffbilanz",
        correctAnswer = 1,
        explanation = "APACHE II (Acute Physiology And Chronic Health Evaluation) bewertet die Schwere der Erkrankung anhand von 12 physiologischen Variablen (inkl. Temperatur, MAP, Herzfrequenz, Atemfrequenz, Oxygenierung, pH, Na, K, Kreatinin, Haematokrit, Leukozyten, GCS), Alter und Vorerkrankungen. Hoeherer Score korreliert mit hoehrer Krankenhaussterblichkeit.",
        difficulty = 4,
        funFact = "Ein APACHE-II-Score von 25 entspricht einer prognostizierten Krankenhaussterblichkeit von ca. 50% -- der Score wurde entwickelt, um Patientengruppen fuer klinische Studien zu stratifizieren, nicht um individuelle Prognosen zu stellen."
    ),

    // Question 13 -- Intensive Care: Sepsis Bundle
    Question(
        categoryId = 16,
        questionText = "Was beinhaltet das '1-Stunden-Bundle' der Surviving Sepsis Campaign (SSC) bei septischem Schock?",
        answerA = "Sofortige Gabe von Breitspektrumantibiotika, 30 ml/kg kristalloide Infusion, Laktat-Messung, Blutkulturen vor Antibiotika, Vasopressoren bei anhaltendem Schock",
        answerB = "Nur Blutkulturen und Antibiotika innerhalb von 1 Stunde",
        answerC = "Volumentherapie mit Kolloiden, Steroide und Immunglobuline innerhalb 1 Stunde",
        answerD = "Intensivaufnahme, ZVK-Anlage und arterieller Zugang innerhalb 1 Stunde",
        correctAnswer = 0,
        explanation = "Das SSC-1-Stunden-Bundle (2018) umfasst: (1) Laktatmessung, (2) Blutkulturen vor Antibiotika, (3) Breitspektrum-Antibiotika, (4) 30 ml/kg kristalloide Infusion bei Hypotension oder Laktat >=4 mmol/l, (5) Vasopressoren (Noradrenalin) bei anhaltendem MAP < 65 mmHg.",
        difficulty = 4,
        funFact = "Jede Stunde Verzoegerung der Antibiotikagabe beim septischen Schock erhoehe die Sterblichkeit um etwa 7% -- schnelles Handeln ist bei keiner anderen Erkrankung zeitkritischer als bei Sepsis."
    ),

    // Question 14 -- Intensive Care: Ventilator Settings
    Question(
        categoryId = 16,
        questionText = "Welche Beatmungsstrategie wird beim ARDS (Acute Respiratory Distress Syndrome) laut ARDSNet-Studie empfohlen?",
        answerA = "Hohe Tidalvolumina (12-15 ml/kg idealem Koerpergewicht) fuer optimale Oxygenierung",
        answerB = "Lungenprotektive Beatmung mit niedrigen Tidalvolumina (6 ml/kg IBW), Plateaudruck < 30 cmH2O, permissive Hyperkapnie",
        answerC = "Druckunterstuetzte Spontanatmung ohne PEEP fuer minimale Sedierung",
        answerD = "Hochfrequenzoszillationsbeatmung (HFOV) als primaerem Modus bei allen ARDS-Schweregraden",
        correctAnswer = 1,
        explanation = "Die ARDSNet-Studie (2000) zeigte eine signifikante Mortalitaetsreduktion durch lungenprotektive Beatmung mit 6 ml/kg idealem Koerpergewicht vs. 12 ml/kg. Plateaudruckbegrenzung auf < 30 cmH2O schutzt vor Volutrauma und Barotrauma; permissive Hyperkapnie wird toleriert.",
        difficulty = 4,
        funFact = "Die ARDSNet-Studie wurde vorzeitig abgebrochen, weil der Vorteil der lungenprotektiven Beatmung so deutlich war -- Sterblichkeit sank von 39.8% auf 31.0%. Sie ist eine der wenigen ICU-Studien mit klarem Sterblichkeitsvorteil."
    ),

    // Question 15 -- Mechanical Ventilation: PEEP
    Question(
        categoryId = 16,
        questionText = "Was ist der Wirkmechanismus von PEEP (Positive End-Expiratory Pressure) bei der invasiven Beatmung?",
        answerA = "Erhoehung des Atemminutenvolumens durch kuerzere Exspirationszeit",
        answerB = "Offenhalten kollabierter Alveolen am Ende der Ausatmung durch endexspiratorischen Gegendruck -- verbessert funktionelle Residualkapazitaet und Oxygenierung",
        answerC = "Reduktion der Atemarbeit durch vollstaendige Entlastung der Atemmuskulatur",
        answerD = "Direkte Bronchodilatation durch positive Druckuebertragung auf die Atemwege",
        correctAnswer = 1,
        explanation = "PEEP haelt am Ende der Exspiration einen positiven Atemwegsdruck aufrecht, der verhindert, dass instabile Alveolen kollabieren (Atelektasenbildung). Dies erhoehe die funktionelle Residualkapazitaet (FRC), verbessert das Ventilations-Perfusions-Verhaeltnis und steigert die Oxygenierung -- besonders wichtig bei ARDS.",
        difficulty = 4,
        funFact = "Zu hoher PEEP kann durch Ueberblahung gesunder Alveolen (Volutrauma) und Reduktion des Herzzeitvolumens (Vorlastsenkung) schaedlich sein -- der optimale PEEP ist individuell zu titrieren."
    ),

    // Question 16 -- Mechanical Ventilation: Modes
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen druckkontrollierter (PC) und volumenkontrollierter (VC) Beatmung?",
        answerA = "Bei PC wird das Tidalvolumen konstant gehalten, bei VC der Atemwegsdruck",
        answerB = "Bei PC wird ein definierter Inspirationsdruck vorgegeben (variables Volumen), bei VC ein definiertes Tidalvolumen (variables Druckniveau)",
        answerC = "PC eignet sich nur fuer Spontanatmung, VC nur fuer kontrollierte Beatmung",
        answerD = "PC und VC unterscheiden sich nur in der Triggerung durch den Patienten",
        correctAnswer = 1,
        explanation = "Bei volumenkontrollierter Beatmung (VCV) wird das Tidalvolumen festgelegt -- der Druck variiert je nach Compliance und Resistance. Bei druckkontrollierter Beatmung (PCV) wird der Inspirationsdruck vorgegeben -- das Volumen variiert. PC-Beatmung schuetzt vor Druckspitzen, VCV garantiert das Tidalvolumen.",
        difficulty = 4,
        funFact = "Beim ARDS wird oft PC-Beatmung bevorzugt, da Druckspitzen vermieden werden -- allerdings kann bei Compliance-Aenderungen das Tidalvolumen schwanken, weshalb engmaschiges Monitoring erforderlich ist."
    ),

    // Question 17 -- Shock Types: Cardiogenic
    Question(
        categoryId = 16,
        questionText = "Welches haemodynamische Profil charakterisiert den kardiogenen Schock?",
        answerA = "Hoher HZV, niedriger SVR, normaler PCWP -- typisches distributives Muster",
        answerB = "Niedriger HZV, hoher SVR, erhoehter PCWP (> 18 mmHg) -- durch Pumpversagen mit peripherer Vasokonstriktion",
        answerC = "Niedriger HZV, niedriger SVR, niedriger PCWP -- durch Vorlastmangel",
        answerD = "Normaler HZV, niedriger PCWP, hoher SVR -- durch mechanische Obstruktion",
        correctAnswer = 1,
        explanation = "Der kardiogene Schock ist durch Pumpversagen mit niedrigem Herzzeitvolumen (HZV) charakterisiert. Kompensatorisch steigt der Systemgefaesswiderstand (SVR) an (kalte, blasse Peripherie). Stauung fuehrt zu erhoehtem pulmonalem Verschlussdruck (PCWP > 18 mmHg). Klassisches Bild: kalter, feuchter Schock.",
        difficulty = 4,
        funFact = "Die intra-aortale Ballonpumpe (IABP) galt jahrzehntelang als Standard beim kardiogenen Schock -- die IABP-SHOCK-II-Studie (2012) zeigte jedoch keinen Sterblichkeitsvorteil, woraufhin ihre routinemaessige Anwendung eingestellt wurde."
    ),

    // Question 18 -- Shock Types: Obstructive
    Question(
        categoryId = 16,
        questionText = "Welche akute Ursache eines obstruktiven Schocks muss bei einem Traumapatienten mit Jugularvenenstauung, Hypotension und fehlendem Atemgeraeusch einseitig sofort bedacht werden?",
        answerA = "Aortendissektion Typ A mit Perikardtamponade",
        answerB = "Spannungspneumothorax -- mediastinale Verschiebung komprimiert Herz und grosse Gefaesse",
        answerC = "Massive Lungenembolie mit akutem Rechtsherzversagen",
        answerD = "Haematothorax mit Hypovolaaemie und verminderter Vorlast",
        correctAnswer = 1,
        explanation = "Die Beck-Trias (Halsvenenstauung, Hypotension, gedaempfte Herztone) weist auf Perikardtamponade hin, aber einseitig fehlendes Atemgeraeusch plus Trachealdeviation sind pathognomonisch fuer Spannungspneumothorax. Dieser obstruiert durch Mediastinalverschiebung den venoeser Rueckstrom -- sofortige Entlastung ist lebensrettend.",
        difficulty = 4,
        funFact = "Die Soforttherapie beim Spannungspneumothorax ist die Nadeldekompression im 2. ICR medioklavikulaer oder 4./5. ICR vordere Axilarlinie -- erst danach Thoraxdrainage. Jede Verzoegerung kostet Leben."
    ),

    // Question 19 -- Shock Types: Distributive
    Question(
        categoryId = 16,
        questionText = "Welches pathophysiologische Merkmal unterscheidet den distributiven Schock von anderen Schockformen?",
        answerA = "Erniedrigung des Herzminutenvolumens bei erhoehtem Gefaesswiderstand",
        answerB = "Massiver Verlust intravaskulaerer Fluessigkeit durch Blutung oder Dehydratation",
        answerC = "Pathologische Vasodilatation mit Maladistribution des Blutflusses -- trotz normalem oder erhoehtem HZV ist die Gewebeoxygenierung insuffizient",
        answerD = "Mechanische Behinderung der Herzfuellung oder des Auswurfs",
        correctAnswer = 2,
        explanation = "Beim distributiven Schock (septischer, anaphylaktischer, neurogener Schock) fuehrt massive Vasodilatation und erhoehte Kapillarpermeabilitaet dazu, dass das Blut in Kompartimenten gepoolt wird. Trotz oft erhoehtem HZV ist die Gewebeperfusion inadaequat -- mikrovaskulaere Shunts leiten Blut an Kapillaren vorbei.",
        difficulty = 4,
        funFact = "Beim septischen Schock ist Noradrenalin das Vasopressor der ersten Wahl -- es wirkt primaer alpha-adrenerg und erhoehe den systemischen Gefaesswiderstand. Dopamin ist mit hoeherem Risiko fuer Arrhythmien und Sterblichkeit assoziiert."
    ),

    // Question 20 -- Acid-Base: Anion Gap
    Question(
        categoryId = 16,
        questionText = "Ein Patient hat: pH 7,20 -- pCO2 20 mmHg -- HCO3- 8 mmol/l -- Na 138 -- Cl 100 -- Albumin 40 g/l. Wie hoch ist die Anionenlucke und was ist die Diagnose?",
        answerA = "AG = 22 -- normwertiger AG, hyperchloraemische metabolische Azidose",
        answerB = "AG = 30 -- erhoehte AG, metabolische Azidose mit erhoehter Anionenlucke (z.B. Laktat, Ketone, Uraemietoxine)",
        answerC = "AG = 14 -- normaler AG, respiratorische Azidose mit metabolischer Kompensation",
        answerD = "AG = 8 -- erniedriegter AG, Hypalbuminaemie als Ursache",
        correctAnswer = 1,
        explanation = "AG = Na - (Cl + HCO3) = 138 - (100 + 8) = 30 mEq/l (Normalwert 12 +/- 2). Bei normalem Albumin (40 g/l) keine Korrektur noetig. Erhoehte AG weist auf unmessbare Anionen hin (Laktat, Ketoazidose, Uraemietoxine, Methanol, Ethylenglykol). Die Kompensation (pCO2 20) entspricht nach Winters Formel: erwartet = 1,5 x 8 + 8 = 20 mmHg -- adaequate respiratorische Kompensation.",
        difficulty = 4,
        funFact = "Die MUDPILES-Eselsbruecke hilft bei erhoehter Anionenlucke: Methanol, Uraemie, Diabetische Ketoazidose, Propylene Glycol, Isoniazid/Eisen, Laktatazidose, Ethylenglykol, Salizylate."
    ),

    // Question 21 -- Acid-Base: Winter's Formula
    Question(
        categoryId = 16,
        questionText = "Ein Patient mit metabolischer Azidose hat HCO3- = 12 mmol/l. Was ist der nach Winters Formel erwartete pCO2 und was bedeutet eine Abweichung davon?",
        answerA = "Erwartet pCO2 = 26 mmHg; ein hoehrer pCO2 weist auf eine zusaetzliche respiratorische Azidose hin",
        answerB = "Erwartet pCO2 = 30 mmHg; ein niedrigerer pCO2 weist auf eine zusaetzliche respiratorische Alkalose hin",
        answerC = "Erwartet pCO2 = 36 mmHg; eine Abweichung ist klinisch nicht relevant",
        answerD = "Erwartet pCO2 = 20 mmHg; ein hoehrer pCO2 weist auf eine metabolische Alkalose hin",
        correctAnswer = 0,
        explanation = "Winters Formel: erwartet pCO2 = (1,5 x HCO3) + 8 (+/-2) = (1,5 x 12) + 8 = 26 mmHg. Liegt der gemessene pCO2 hoeher (z.B. 35 mmHg), besteht eine zusaetzliche respiratorische Azidose (Hypoventilation). Liegt er niedriger (z.B. 18 mmHg), liegt eine ueberproportionale Hyperventilation vor -- Hinweis auf gleichzeitige respiratorische Alkalose.",
        difficulty = 4,
        funFact = "Die Winters-Formel gilt streng genommen nur fuer chronische metabolische Azidosen -- bei akuten Aenderungen ist die respiratorische Kompensation etwas geringer als erwartet."
    ),

    // Question 22 -- Acid-Base: Triple Disorder
    Question(
        categoryId = 16,
        questionText = "Welche Saeure-Basen-Stoerung liegt vor bei: pH 7,48 -- pCO2 30 mmHg -- HCO3- 22 mmol/l -- AG 24 -- Albumin 40 g/l?",
        answerA = "Einfache respiratorische Alkalose mit adaequater metabolischer Kompensation",
        answerB = "Gemischte Stoerung: respiratorische Alkalose PLUS metabolische Azidose mit hoher AG PLUS metabolische Alkalose",
        answerC = "Metabolische Alkalose mit uebermaessiger respiratorischer Kompensation",
        answerD = "Nur metabolische Azidose mit hoher AG -- pH und pCO2 sind Messfehler",
        correctAnswer = 1,
        explanation = "Analyse: pH 7,48 = Alkalose. pCO2 30 = Hyperventilation (primaer respiratorische Alkalose). Erwartetes HCO3 bei resp. Alk. = 24 - 0,2 x (40-30) = 22 -- HCO3 22 passt als Kompensation. ABER: AG = 24 (erhoehen) weist auf gleichzeitige metabolische Azidose (hohe AG) hin. Delta-Delta = (AG-12)/(24-HCO3) = 12/2 = 6 > 2 -- metabolische Alkalose maskiert den HCO3-Abfall. Triple-Disorder.",
        difficulty = 4,
        funFact = "Triple-Saeure-Basen-Stoerungen kommen bei Leberzirrhotikern mit Erbrechen und Sepsis vor: respiratorische Alkalose (Hyperventilation bei Enzephalopathie) + metabolische Azidose (Laktat) + metabolische Alkalose (Erbrechen/Diuretika)."
    ),

    // Question 23 -- Nutrition in Critical Illness: Enteral vs Parenteral
    Question(
        categoryId = 16,
        questionText = "Warum wird in der Intensivmedizin enterale Ernaehrung der parenteralen vorgezogen, sofern kein Kontraindikation besteht?",
        answerA = "Enterale Ernaehrung ist billiger und erfordert keinen zentralen Venenkatheter",
        answerB = "Enterale Ernaehrung erhaelt die intestinale Barrierefunktion und Mukosaintegritaet, reduziert bakterielle Translokation, Infektionsrate und systemische Inflammationsreaktion",
        answerC = "Parenterale Ernaehrung fuehrt immer zu Hyperglykamie und ist deshalb kontraindiziert",
        answerD = "Enterale Ernaehrung verbessert die Resorption von fettloeslichen Vitaminen besser als parenterale",
        correctAnswer = 1,
        explanation = "Die enteral zugefuehrten Naehrstoffe stimulieren die Darmmukosa (trophischer Effekt), erhalten Tight Junctions und verhindern villoeise Atrophie. Ohne enterale Ernaehrung nimmt die intestinale Barriere innerhalb von Tagen ab -- bakterielle Translokation und erhoehte Infektionsraten (Pneumonie, Sepsis) sind die Folge.",
        difficulty = 4,
        funFact = "Das Konzept 'If the gut works, use it' (wenn der Darm funktioniert, benutze ihn) ist in der Intensivmedizin seit den 1980ern belegt -- fruehzeitige enterale Ernaehrung innerhalb von 24-48 Stunden nach Aufnahme ist heute Standard."
    ),

    // Question 24 -- Nutrition in Critical Illness: Refeeding Syndrome
    Question(
        categoryId = 16,
        questionText = "Was ist das Refeeding-Syndrom und welche Elektrolytstoerung steht im Vordergrund?",
        answerA = "Uebelkeit und Diarrhoee beim zu schnellen Kostaufbau nach Darmoperationen -- primaer Hyperkalaemie",
        answerB = "Lebensbedrohliche Elektrolytverschiebungen bei Wiederbeginn von Ernaehrung nach laengerer Nahrungskarenz -- primaer Hypophosphataaemie durch intrazellulaerem Phosphatshift",
        answerC = "Hyperglykaamie durch Insulinresistenz beim Beginn parenteraler Glukosezufuhr",
        answerD = "Stickstoffretention durch uebermaessige Proteinzufuhr bei Niereninsuffizienz",
        correctAnswer = 1,
        explanation = "Beim Refeeding-Syndrom fuehrt Glukosezufuhr nach Nahrungskarenz zu Insulinausschuettung, die Phosphat, Kalium und Magnesium in die Zellen verschiebt. Schwere Hypophosphataaemie (< 0,5 mmol/l) kann Herzrhythmustoerungen, Atemmuskelversagen, Haemolyse und neurologische Stoerungen verursachen.",
        difficulty = 4,
        funFact = "NICE-Kriterien fuer Refeeding-Risiko: BMI < 18,5 kg/m2, Gewichtsverlust > 10% in 3-6 Monaten, keine/minimale Ernaehrung > 5 Tage, Hypophosphataaemie/Hypokaliaamie/Hypomagnesiaamie -- bei Vorliegen von 1-2 Kriterien: langsamer Kostaufbau mit Elektrolytsubstitution."
    ),

    // Question 25 -- Burns: Rule of Nines
    Question(
        categoryId = 16,
        questionText = "Wie wird die verbrannte Koerperoberflaeche (VKOF) bei Erwachsenen nach der 'Neuner-Regel' berechnet und wofuer wird sie benoetigt?",
        answerA = "Kopf = 18%, Rumpf = 36%, jeder Arm = 9%, jedes Bein = 9%, Genitalien = 1% -- fuer die Abschaetzung des Fluessigkeitsbedarfs",
        answerB = "Kopf = 9%, Rumpf vorne 18% + hinten 18%, jeder Arm = 9%, jedes Bein = 18%, Genitalien = 1% -- fuer die Abschaetzung des Fluessigkeitsbedarfs nach Parkland-Formel",
        answerC = "Kopf = 21%, Rumpf = 42%, jede Extremitaet = 9% -- nur fuer Kinder gueltig",
        answerD = "Kopf = 9%, Arme je 4,5%, Beine je 9%, Rumpf 36%, Genitalien 1% -- fuer die Ernaehrungsberechnung",
        correctAnswer = 1,
        explanation = "Bei Erwachsenen gilt die Neuner-Regel: Kopf und Hals = 9%, jeder Arm = 9%, Vorderrumpf = 18%, Hinterrumpf = 18%, jedes Bein = 18%, Genitalien = 1% -- Gesamtkoerper = 100%. Die VKOF wird mit dem Gewicht in die Parkland-Formel eingesetzt, um den Infusionsbedarf der ersten 24 Stunden zu berechnen.",
        difficulty = 4,
        funFact = "Bei Kindern ist der Kopf groesser und die Beine kleiner (Lund-Browder-Diagramm) -- die Neuner-Regel gilt nicht paediatrisch. Faustregel: Handflaeche des Patienten (inkl. Finger) = ca. 1% VKOF fuer unregelmaessige Verbrennungsmuster."
    ),

    // Question 26 -- Burns: Parkland Formula
    Question(
        categoryId = 16,
        questionText = "Nach der Parkland-Formel (Baxter-Formel), wie viel Fluessigkeit benoetigt ein 70 kg schwerer Patient mit 40% VKOF in den ersten 24 Stunden?",
        answerA = "5.600 ml Ringerlaktat -- 50% in den ersten 8 Stunden, 50% in den folgenden 16 Stunden",
        answerB = "11.200 ml Ringerlaktat -- 50% in den ersten 8 Stunden, 50% in den folgenden 16 Stunden",
        answerC = "2.800 ml Kolloide -- gleichmaessig ueber 24 Stunden",
        answerD = "8.400 ml Ringerlaktat -- gleichmaessig ueber 24 Stunden",
        correctAnswer = 1,
        explanation = "Parkland-Formel: 4 ml x kg Koerpergewicht x % VKOF = 4 x 70 x 40 = 11.200 ml Ringerlaktat in den ersten 24 Stunden. Die erste Haelfte (5.600 ml) wird in den ersten 8 Stunden gegeben (ab Verletzungszeitpunkt, nicht ab Krankenhausaufnahme), die zweite Haelfte in den naechsten 16 Stunden.",
        difficulty = 4,
        funFact = "Charles Baxter entwickelte die Formel in den 1960ern am Parkland Memorial Hospital in Dallas -- deswegen heisst sie auch Baxter- oder Parkland-Formel. Sie ist eine Schaetzung -- tatsaechlicher Bedarf wird an Urinausscheidung (0,5-1 ml/kg/h) titriert."
    ),

    // Question 27 -- Burns: Escharotomy
    Question(
        categoryId = 16,
        questionText = "Wann ist eine Escharotomie bei einer Verbrennungsverletzung indiziert und was ist das Ziel des Eingriffs?",
        answerA = "Bei allen Verbrennungen > 20% VKOF zur Infektionsprophylaxe",
        answerB = "Bei zirkulaeren Verbrennungen mit Kompartmentsyndrom-Gefahr (Extremitaeten, Thorax) -- Spaltung des inelastischen Schorfes zur Druckentlastung",
        answerC = "Zur Vorbereitung der Hauttransplantation am 5.-7. Tag nach Trauma",
        answerD = "Bei Inhalationstrauma zur Verbesserung der Thorax-Compliance",
        correctAnswer = 1,
        explanation = "Zirkulaere Vollhauttverbrennungen bilden einen inelastischen Schorf (Eschar), der bei Oedembildung den Gewebedruck erhoehen und Durchblutung kompromittieren kann. An Extremitaeten droht distale Ischaemie, am Thorax Beatmungsbehinderung. Die Escharotomie (Spaltung laengs) entlastet den Druck sofort.",
        difficulty = 4,
        funFact = "Die Escharotomie wird ohne Anaesthesie durchgefuehrt -- Vollhautverbrennung (Grad III) zerstoert die Schmerzrezeptoren in der Dermis. Blutungsrisiko ist aber vorhanden, da das Subkutangewebe noch durchblutet ist."
    ),

    // Question 28 -- Radiology: Kerley Lines
    Question(
        categoryId = 16,
        questionText = "Welche Bedeutung haben Kerley-B-Linien im Thorax-Roentgen?",
        answerA = "Laengsgestreckte, zentralwaerts verlaufende Linien, die erweiterte Pulmonalarterien darstellen",
        answerB = "Kurze (1-2 cm), horizontale, periphere Linien senkrecht zur Pleura, die gestaute interlobulaere Lymphbahnen und Septen repraesentieren -- Zeichen erhoeuhten pulmonaerkapillaeren Drucks",
        answerC = "Feine retikulaere Zeichnung beider Unterlappen bei Lungenfibrose",
        answerD = "Diagonale Linien im Mittelfeld, die erweiterte Venenstaemme bei pulmonaler Hypertonie zeigen",
        correctAnswer = 1,
        explanation = "Kerley-B-Linien sind 1-2 cm lange, feine, horizontale Linien peripher basal in beiden Unterlappen, senkrecht zur Pleura. Sie entstehen durch Fluessigkeitsansammlungen in den interlobulaeren Septen und Lymphgefaessen bei Linksherzdekompensation oder Mitralklappenstenose. Zeichen erhoeuhten pulmonaerkapillaeren Verschlussdrucks.",
        difficulty = 4,
        funFact = "Es gibt auch Kerley-A-Linien (laengere, zentrifugal verlaufende Linien im Oberfeld) und Kerley-C-Linien (retikulaeres Muster). A- und C-Linien sind klinisch weniger bedeutsam als die B-Linien."
    ),

    // Question 29 -- Radiology: Sunburst Pattern
    Question(
        categoryId = 16,
        questionText = "Das 'Sunburst'- oder 'Stacheldrahtsterne'-Muster im Knoechenroentgen ist charakteristisch fuer welchen Tumor?",
        answerA = "Enchondrom -- benigner Knorpeltumor mit Matrixverkalkung",
        answerB = "Osteosarkom -- aggressives malignes Knochentumor mit spikulaerer, sonnenstrahlenfoermiger Periostreaktion",
        answerC = "Riesenzelltumor -- epiphysaerer, lytischer Tumor ohne Periostreaktion",
        answerD = "Fibrosarkom -- lytischer Tumor ohne Knochenneubildung",
        correctAnswer = 1,
        explanation = "Das Sunburst-Muster entsteht beim Osteosarkom durch perpendikular zur Knochenachse wachsende reaktive Knochenspikulae, die das abgehobene Periost senkrecht durchbrechen. In Kombination mit dem Codman-Dreieck ergibt sich das klassische radiologische Bild aggressiver Knochen-Malignome.",
        difficulty = 4,
        funFact = "Das Osteosarkom hat einen zweiten Altersgipfel: Den ersten bei Teenagern (Wachstumsschub) und einen zweiten bei aelteren Patienten -- letzterer oft auf dem Boden eines Morbus Paget."
    ),

    // Question 30 -- Interventional Radiology: TIPS
    Question(
        categoryId = 16,
        questionText = "Was ist ein TIPS (Transjugulaerer intrahepatischer portosystemischer Shunt) und bei welcher Indikation wird er eingesetzt?",
        answerA = "Chirurgische Anlage einer Anastomose zwischen Pfortader und V. cava zur Behandlung des Budd-Chiari-Syndroms",
        answerB = "Interventionell-radiologisch angelegter Shunt zwischen Portalvene und Lebervene durch die Leber -- bei refraktaerem Aszites oder Oesophagusvarizenblutung bei portaler Hypertension",
        answerC = "Perkutane transhepatische Cholangiodrainage zur Dekompression obstruierter Gallenwege",
        answerD = "Embolisation der Leberarterien zur Behandlung des hepatozellulaeren Karzinoms",
        correctAnswer = 1,
        explanation = "Beim TIPS wird ueber eine transvenoeser Zugang (V. jugularis) ein Metallstent durch das Leberparenchym zwischen einer Ast der Pfortader und einer Lebervene implantiert. Dies senkt den portalen Druck und behandelt Aszites, Oesophagusvarizenblutungen und hepatorenales Syndrom.",
        difficulty = 4,
        funFact = "Eine seltene aber schwere Komplikation des TIPS ist die hepatische Enzephalopathie -- da Blut am Leberparenchym vorbeigeleitet wird, werden Ammoniak und andere hepatotoxische Substanzen nicht mehr gefiltert. TIPS-Kalibrierung (Flusslimitierung) kann das Risiko mindern."
    ),

    // Question 31 -- Intensive Care: Lactate Clearance
    Question(
        categoryId = 16,
        questionText = "Welche Bedeutung hat die Laktatclearance in der Intensivmedizin bei Sepsis?",
        answerA = "Ein sinkendes Laktat zeigt Verbesserung der Gewebeoxygenierung und Therapieansprechen -- Clearance > 10% nach 2 Stunden als Therapieziel",
        answerB = "Laktat ist nur bei diabetischer Ketoazidose relevant und zeigt Insulinmangel an",
        answerC = "Laktat spiegelt ausschliesslich die Leberfunktion wider und steigt bei Leberversagen an",
        answerD = "Ein stabiles Laktat zeigt optimale Beatmungseinstellung ohne Bedeutung fuer die Perfusion",
        correctAnswer = 0,
        explanation = "Laktat ist ein Marker anaerober Gewebehypoxie -- ein Anstieg zeigt inadaequate Sauerstoffversorgung an. Laktatclearance (Abfall des Laktats ueber Zeit) reflektiert Therapieansprechen. Die SSC empfiehlt Laktatmessung bei > 2 mmol/l und Normalisierung als Resuscitationsziel -- Clearance > 10% in 2 Stunden ist prognostisch guenstig.",
        difficulty = 4,
        funFact = "Persistierendes Laktat > 4 mmol/l trotz Fluessigkeitstherapie ist ein unabhaengiger Praediktor fuer ICU-Sterblichkeit. Das Konzept der 'kryptischen Schock' beschreibt Patienten mit normalen Blutdruck aber erhoehtem Laktat -- auch diese benoetigen aggressive Resuscitation."
    ),

    // Question 32 -- Mechanical Ventilation: Prone Position
    Question(
        categoryId = 16,
        questionText = "Welchen Mechanismus hat die Bauchlagerung (prone positioning) bei schwerem ARDS und welchen Mortalitaetsvorteil zeigt die PROSEVA-Studie?",
        answerA = "Verbesserte Herzfunktion durch Entlastung des rechten Ventrikels -- Mortalitaet reduziert um 5%",
        answerB = "Umverteilung des Blutflusses zu besser beluefteten dorsalen Lungenabschnitten, Reduktion des Ventilations-Perfusions-Mismatches -- PROSEVA: 28-Tage-Sterblichkeit von 32,8% auf 16%",
        answerC = "Drainage von Sekret aus Atemwegen durch Schwerkraft -- Mortalitaet unveraendert, aber Pneumonieraten sinken",
        answerD = "Reduktion des intrakranialen Drucks bei Hirnoedem-Patienten -- keine Mortalitaetsdaten verfuegbar",
        correctAnswer = 1,
        explanation = "Bei ARDS sind dorsale Lungenabschnitte durch Atelektasen und Oedeme nicht beluftet, aber perfundiert (Shunt). Bauchlagerung bringt diese Abschnitte nach ventral, wo Schwerkraft die Beliftung verbessert. PROSEVA (2013) zeigte bei schweren ARDS (PaO2/FiO2 < 150) eine dramatische Sterblichkeitsreduktion von 28-Tage-Mortalitaet 32,8% vs. 16,0%.",
        difficulty = 4,
        funFact = "Die Bauchlagerung benoetigt mindestens 16 Stunden pro Tag um optimal wirksam zu sein -- diese Erkenntnis aus PROSEVA aenderte die klinische Praxis weltweit. Vorher wurden oft nur 7-8 Stunden als ausreichend angesehen."
    ),

    // Question 33 -- Shock Types: Hypovolemic
    Question(
        categoryId = 16,
        questionText = "Nach der ATLS-Klassifikation entspricht ein Blutverlust von 30-40% des Blutvolumens (ca. 1500-2000 ml bei 70 kg) welchem haemodynamischen Stadium?",
        answerA = "Klasse I -- leichter Schock, keine haemodynamischen Veraenderungen",
        answerB = "Klasse III -- Herzfrequenz > 120, RR erniedrigt, Bewusstseinsveraenderung, Urinausscheidung 5-15 ml/h",
        answerC = "Klasse II -- Tachykardie, normaler oder leicht erniedrigter Blutdruck, Urinausscheidung 20-30 ml/h",
        answerD = "Klasse IV -- lebensbedrohlich, kein messbarer Blutdruck, Bewusstlosigkeit",
        correctAnswer = 1,
        explanation = "ATLS-Klasse III (30-40% Blutverlust): Herzfrequenz 120-140/min, erniedrigter Blutdruck, erhoehte Atemfrequenz (30-40/min), reduzierte Urinausscheidung (5-15 ml/h), veraendertes Bewusstsein (Verwirrtheit, Angst). Dieser Schockzustand erfordert Bluttransfusionen.",
        difficulty = 4,
        funFact = "Das ATLS-Klassifikationssystem wurde 1978 nach einem Flugzeugunfall entwickelt, bei dem der Unfallchirurg James Styner und seine Familie verungluckten. Frustriert ueber die unzureichende Notfallversorgung entwickelte er systematische Traumaversorgungsprotokolle."
    ),

    // Question 34 -- Radiology: Hampton Hump
    Question(
        categoryId = 16,
        questionText = "Was beschreibt das 'Hampton-Hoecker'-Zeichen im Thorax-Roentgen und welche Erkrankung deutet es an?",
        answerA = "Keilfoermige, periphere Pleura-basierte Verschattung mit Basis zur Pleura -- Zeichen eines haemorrhagischen Lungeninfarkts bei Lungenembolie",
        answerB = "Halbmondfoermige Aufhellung ueber einem Lungenrundherd -- Zeichen eines Aspergillusballs",
        answerC = "Abgerundete, zentrale Verschattung im Hilusbereich -- Zeichen einer Sarkoidose",
        answerD = "Randstaendige Verschattung mit Hoehlenbildung -- Zeichen einer Lungentuberkulose",
        correctAnswer = 0,
        explanation = "Das Hampton-Hoecker-Zeichen (Hampton's Hump) ist eine pleurabasierte, keilfoermige Verschattung als Folge eines peripheren haemorrhagischen Lungeninfarkts bei Lungenembolie. Das embolisierte Gefaess versorgt den Bereich nicht mehr mit Sauerstoff, die resultierende haemorrhagische Nekrose erscheint als Keil im Roentgenbild.",
        difficulty = 4,
        funFact = "Das Hampton-Hoecker-Zeichen tritt nur bei etwa 15% der Lungenembolien auf -- der Grossteil bleibt roentgenologisch unauffaellig. CT-Pulmonalisangiographie (CTPA) ist der Goldstandard mit Sensitivitaet > 95%."
    ),

    // Question 35 -- Intensive Care: Sedation
    Question(
        categoryId = 16,
        questionText = "Welche Aussage zum Sedationsmanagement auf der Intensivstation entspricht aktueller Empfehlung (PADIS-Guideline)?",
        answerA = "Tiefe Sedation (RASS -4 bis -5) reduziert PTSD und Mortalitaet -- Standard fuer alle beatmeten Patienten",
        answerB = "Analgosedation ('Analgesia-First') mit minimalem Sedationsziel (RASS 0 bis -2), taegliche Sedationsunterbrechung, Bevorzugung von Propofol oder Dexmedetomidin",
        answerC = "Benzodiazepine sind Mittel der Wahl fuer Langzeitsedation wegen guenstiger Nebenwirkungsprofil",
        answerD = "Sedation sollte bis zum Extubationstag unveraendert beibehalten werden, um Stressaktivaierung zu vermeiden",
        correctAnswer = 1,
        explanation = "PADIS-Richtlinien (2018) empfehlen: Schmerzkontrolle vor Sedierung (Analgo-Sedation), leichtes Sedationsniveau (RASS 0 bis -2 als Ziel), taegliche spontane Aufwachversuche (SATs), Kombination mit Atemversuchen (SBTs), Vermeidung von Benzodiazepinen -- Propofol oder Dexmedetomidin bevorzugt.",
        difficulty = 4,
        funFact = "Die ABCDEF-Bundle-Strategie (Awakening, Breathing, Coordination, Delirium, Early mobility, Family) reduziert ICU-Delir, Beatmungsdauer und postintensivmedizinisches Syndrom (PICS) signifikant -- konsequente Umsetzung verbessert Langzeitergebnisse deutlich."
    ),

    // Question 36 -- Surgery: Damage Control
    Question(
        categoryId = 16,
        questionText = "Was ist das Prinzip der 'Damage Control Surgery' (DCS) beim Traumapatienten?",
        answerA = "Sofortige komplette Definitivversorgung aller Verletzungen in einem einzigen langen Eingriff",
        answerB = "Dreiphasige Strategie: (1) lebensrettende Sofortmassnahmen und Blutungskontrolle, (2) Intensivstabilisierung, (3) spaetere Definitivversorgung nach Korrektur der letalen Triade",
        answerC = "Ausschliesslich konservative Behandlung aller Verletzungen bis haemodynamische Stabilitaet erreicht ist",
        answerD = "Verzicht auf chirurgische Intervention bis der Gerinnungsstatus normalisiert ist",
        correctAnswer = 1,
        explanation = "DCS beruht auf der Erkenntnis, dass die 'letale Triade' (Hypothermie, Azidose, Koagulopathie) sich im OP verschlimmert. Phase 1: Blutungskontrolle (Packing, voruebergehende Abklemmung), schneller Wundverschluss (Abdomen open). Phase 2: ICU-Aufnahme, Normothermie, Gerinnungskorrektur. Phase 3: Definitivoperation nach 24-48 Stunden.",
        difficulty = 4,
        funFact = "Das Konzept der DCS wurde von Harlan Stone 1983 beschrieben, aber erst durch Matthew Wall und Mattox in den 1990ern systematisiert. Es revolutionierte das Traumamanagement und senkte die Sterblichkeit schwerverletzter Patienten deutlich."
    ),

    // Question 37 -- Nutrition: TPN Complications
    Question(
        categoryId = 16,
        questionText = "Welche leberspezifische Komplikation tritt bei laengerfristiger totaler parenteraler Ernaehrung (TPN) auf?",
        answerA = "Autoimmune Hepatitis durch Fremdproteinstimulation",
        answerB = "TPN-assoziierte Leberkrankheit (PNALD/IFALD) mit Cholestase, hepatischer Steatose und progressiver Fibrose bei Langzeit-TPN",
        answerC = "Akute Hepatitis durch bakterielle Kontamination der Infusionsloesungen",
        answerD = "Haemochromatose durch eisenreiche Infusionsloesungen",
        correctAnswer = 1,
        explanation = "PNALD (Parenteral Nutrition-Associated Liver Disease) / IFALD (Intestinal Failure-Associated Liver Disease) manifestiert sich als Cholestase (erhoehtes direktes Bilirubin, erhoeuhte alkalische Phosphatase), Steatose und bei Langzeitverlauf als Fibrose bis Zirrhose. Phytosterolelemente in Sojaoel-basierten Lipiden sind mitverantwortlich.",
        difficulty = 4,
        funFact = "Fischoel-basierte Lipidloesungen (reich an Omega-3-Fettsaeuren) wie Omegaven reduzierten PNALD-Inzidenz bei Kinder mit Kurzdarmsyndrom dramatisch -- ein bahnbrechender Befund der paediatrischen Intensivmedizin."
    ),

    // Question 38 -- Radiology: Reverse Bat Wing
    Question(
        categoryId = 16,
        questionText = "Das 'Reversed Bat Wing'- oder 'Reversed Butterfly'-Muster im HR-CT der Lunge mit bilateralen peripheren Konsolidierungen bei zentraler Aussparung ist typisch fuer welche Erkrankung?",
        answerA = "Kardiogenes Lungenoedeme mit zentraler Betonung",
        answerB = "Kryptogen organisierende Pneumonie (COP) oder eosinophile Pneumonie mit peripheren Konsolidierungen",
        answerC = "Pulmonale Sarkoiodose mit perihilaeren Lymphknoten und zentraler Fibrose",
        answerD = "ARDS mit diffusem bilateralem Infiltrat ohne peripheren Schwerpunkt",
        correctAnswer = 1,
        explanation = "Das umgekehrte Schmetterlingzeichen (reversed bat wing) beschreibt periphere, subpleurale Konsolidierungen bilateral mit relativer Aussparung des zentralen Lungengewebes. Dies ist charakteristisch fuer kryptogen organisierende Pneumonie (COP), eosinophile Pneumonie und periphere Adenokarzinome.",
        difficulty = 4,
        funFact = "COP (fruehuer BOOP -- Bronchiolitis obliterans organising pneumonia) spricht oft gut auf Kortikosteroide an -- Fehler bei der Erstdiagnose als Pneumonie sind haeufig, da klinische Praesentation aehnlich ist. HR-CT-Muster ist der Schluessel zur Diagnose."
    ),

    // Question 39 -- Intensive Care: CRRT
    Question(
        categoryId = 16,
        questionText = "Was ist der Vorteil der kontinuierlichen Nierenersatztherapie (CRRT) gegenueber der intermittierenden Haemodialyse (IHD) bei haemodynamisch instabilen ICU-Patienten?",
        answerA = "CRRT ist kostenguenstiger und erfordert keinen Spezialisten",
        answerB = "CRRT ermoeglicht langsame, kontinuierliche Fluessigkeitsentfernung und Toxinelimination ohne haemodynamische Instabilitaet -- weniger kardiovaskulaere Stressreaktionen als bei IHD",
        answerC = "CRRT eliminiert Elektrolyte schneller und ist bei Hyperkalaemie ueberlegen",
        answerD = "CRRT hat geringeres Infektionsrisiko durch geschlossenes System ohne Membranexposition",
        correctAnswer = 1,
        explanation = "Bei IHD werden grosse Fluessigkeitsmengen schnell entfernt -- haemodynamisch instabile Patienten tolerieren diese Fluessigkeitsverschiebungen schlecht (Hypotension, Organhypoperfusion). CRRT entfernt Fluessigkeit mit Raten von 100-200 ml/h kontinuierlich, was haemodynamisch besser toleriert wird.",
        difficulty = 4,
        funFact = "CRRT-Filter verstopfen bei Antikoagulationsversagen -- Zitrat-Antikoagulation (regional) hat gegenueber Heparin den Vorteil, die systemische Gerinnung nicht zu beeinflussen und laengere Filterlebensdauer zu erreichen."
    ),

    // Question 40 -- Surgery: Laparotomy Closure
    Question(
        categoryId = 16,
        questionText = "Was ist das abdominale Kompartmentsyndrom (ACS) und welches Drucklimit gilt als kritisch?",
        answerA = "Intraabdominaler Druck > 12 mmHg mit Organdysfunktion -- kritisch ab 25 mmHg, Therapie: Dekompressions-Laparotomie",
        answerB = "Intraabdominaler Druck > 20 mmHg mit Organdysfunktion (Oligurie, erhoehter Beatmungsdruck, vermindertes HZV) -- kritisch ab 25 mmHg",
        answerC = "Intraabdominaler Druck > 35 mmHg ohne Organdysfunktion -- nur bei Peritonitis relevant",
        answerD = "Nur postoperativ nach Laparotomie auftretend -- primaeres ACS durch externe Kompression",
        correctAnswer = 1,
        explanation = "Das abdominale Kompartmentsyndrom ist definiert als IAP > 20 mmHg mit neuer Organdysfunktion. Ab IAP > 20 mmHg entstehen: Oligurie/Anurie (V.-cava-Kompression, renale Hypoperfusion), erhoehte Beatmungsdruecke (Zwerchfellhochstand), vermindertes HZV. Die Dekompressionslaparotomie ist die definitive Therapie.",
        difficulty = 4,
        funFact = "IAP-Messung erfolgt klassisch ueber den Blasenkatheter -- intravesikaler Druck korreliert gut mit dem intraperitonealen Druck. Messung: 25 ml NaCl instillieren, 30-60 Sekunden Wartezeit, Messung in Exspirium, Nullpunkt Symphysenhoeher."
    ),

    // Question 41 -- Radiology: Signet Ring Sign
    Question(
        categoryId = 16,
        questionText = "Das 'Siegelring-Zeichen' im Thorax-CT (erweiterte Bronchien im Vergleich zur begleitenden Pulmonalarterie) ist diagnostisch fuer welche Erkrankung?",
        answerA = "Pulmonale arterielle Hypertonie mit erweitertem Pulmonalgefaessbaum",
        answerB = "Bronchiektasen -- durch irreversible Bronchialerweiterung sind die Atemwege breiter als das begleitende Gefaess",
        answerC = "Bronchuskarzinom mit peribronchialem Wachstum",
        answerD = "Pulmonale Oedem mit peribronchialem Manschettenzeichen",
        correctAnswer = 1,
        explanation = "Normal sind Bronchien und begleitende Pulmonalarterienast annaehernd gleich gross (Bronchus-Arterien-Ratio ca. 1:1). Bei Bronchiektasen ueberschreitet der Bronchialdurchmesser den Arteriendurchmesser (Ratio > 1,5) -- im Axialschnitt ergibt der weitgestellte Bronchus mit dem kleinen Arterienlumen das Bild eines Siegelrings.",
        difficulty = 4,
        funFact = "Zystische Fibrose ist die haeufigste Ursache fuer schwere Bronchiektasen bei jungen Patienten -- CFTR-Modulatoren (Ivacaftor, Tezacaftor/Ivacaftor-Elexacaftor) revolutionieren die Behandlung und koennen Bronchiektasen-Progression verlangsamen."
    ),

    // Question 42 -- Interventional Radiology: IVC Filter
    Question(
        categoryId = 16,
        questionText = "Bei welcher Indikation ist die Implantation eines Vena-cava-Filters (IVC-Filter) absolut indiziert?",
        answerA = "Tiefe Beinvenenthrombose als Primaerprophylaxe bei langer Bettlagerigkeit",
        answerB = "Rezidivierende Lungenembolie unter adaequater Antikoagulation ODER Lungenembolie mit absoluter Kontraindikation zur Antikoagulation",
        answerC = "Alle Patienten mit bekannter tiefer Beinvenenthrombose vor elektiven Operationen",
        answerD = "Chronisch-thrombembolische pulmonale Hypertonie (CTEPH) als Langzeitprophylaxe",
        correctAnswer = 1,
        explanation = "Absolute Indikationen fuer IVC-Filter: (1) Lungenembolie bei Kontraindikation fuer Antikoagulation (aktive Blutung, kuerzliche intrakranielle Operation) oder (2) rezidivierende Lungenembolie trotz adaequater Antikoagulation. Wann immer moeglich, sollten entfernbare Filter (retrievable) implantiert werden.",
        difficulty = 4,
        funFact = "Die PREPIC-Studie zeigte: IVC-Filter reduzieren kurzfristig das Lungenembolie-Risiko, erhoehen jedoch langfristig das Risiko tiefer Beinvenenthrombosen. Permanente Filter sind mit erhoehter Spaetmorbiditaet assoziiert -- retrievable Filter sollen nach Moeglichkeit entfernt werden."
    ),

    // Question 43 -- Intensive Care: Delirium
    Question(
        categoryId = 16,
        questionText = "Mit welchem Instrument wird ICU-Delir standardmaessig ueberprueft und welche nicht-pharmakologische Massnahme reduziert die Delirhaeufigkeit am effektivsten?",
        answerA = "GCS-Score taegliche Messung -- Benzodiazepingabe als Primaerpraevention",
        answerB = "CAM-ICU (Confusion Assessment Method for ICU) -- fruehzeitige Mobilisation, Schlaf-Wach-Rhythmus, Orientierungshilfen (Familie, Zeitgeber)",
        answerC = "RASS-Score allein -- Antipsychotika wie Haloperidol als Routineprophylaxe",
        answerD = "Bildgebung (Schaedel-CT) -- taegliche Sedationsunterbrechung ohne weitere Massnahmen",
        correctAnswer = 1,
        explanation = "Der CAM-ICU ist das validierte Standardinstrument fuer ICU-Delirerkennung (4 Merkmale: akuter Beginn oder Fluktuation, Unaufmerksamkeit, desorganisiertes Denken, veraenderter Bewusstseinszustand). Das ABCDEF-Bundle -- insbesondere fruehzeitige Mobilisation und Delirasessment -- ist die effektivste nicht-pharmakologische Intervention.",
        difficulty = 4,
        funFact = "ICU-Delir ist kein harmloser Verwirrtheitszustand -- es ist mit laengerer Beatmungsdauer, hoeherem PTSD-Risiko, kognitiven Langzeitschaeden und erhoehter Sterblichkeit verbunden. Etwa 30-80% aller beatmeten ICU-Patienten entwickeln ein Delir."
    ),

    // Question 44 -- Surgery: Anastomotic Leak
    Question(
        categoryId = 16,
        questionText = "Was sind die typischen klinischen Zeichen einer Anastomoseninsuffizienz nach kolorektaler Chirurgie und wann treten sie auf?",
        answerA = "Sofort postoperativ: starke Schmerzen und Peritonismus -- tritt immer innerhalb von 24 Stunden auf",
        answerB = "Typisch Tag 3-7 postoperativ: Fieber, Tachykardie, Peritoneismus, Darmparalyse, eventuell Entleerung von Darminhalt ueber Drainage oder Wunde",
        answerC = "Erst nach 2-3 Wochen: Ileus-Symptomatik ohne Fieber bei subklinischem Verlauf",
        answerD = "Immer asymptomatisch -- nur im Kontrastmitteleinlauf nachweisbar",
        correctAnswer = 1,
        explanation = "Anastomoseninsuffizienzen manifestieren sich typischerweise an Tag 3-7 -- dies entspricht dem physiologischen Nadir der Wundheilungsphase (maximale Entzuendungsreaktion). Zeichen: Fieber (> 38,5 Grad C), Tachykardie, abdominale Abwehrspannung, Leukozytenanstieg, veraenderte Drainage (trueb, kotig). Frueherkennung ist entscheidend fuer das Outcome.",
        difficulty = 4,
        funFact = "Die Anastomoseninsuffizienzrate bei tiefer anteriorer Rektumresektion betraegt 5-15% -- sie sinkt durch Defunctioning Stoma (vorgelagertes Ileostoma) auf ca. 5%. Erfahrene Zentren und hohe Operationsvolumina korrelieren mit geringeren Raten."
    ),

    // Question 45 -- Acid-Base: Metabolic Alkalosis
    Question(
        categoryId = 16,
        questionText = "Ein Patient mit Magenausgang-Stenose entwickelt metabolische Alkalose. Was ist der Hauptmechanismus und warum wird sie aufrechterhalten?",
        answerA = "Natriumverlust durch Erbrechen fuehrt zu Hyponatraemie und kompensatorischer HCO3-Senkung",
        answerB = "Verlust von HCl durch Erbrechen (Alkalose-Genese) plus renale HCO3-Rueckresorption durch Volumenmangel und Hypokalaemie (Aufrechterhaltung)",
        answerC = "Hypoventilation durch Zwerchfellhochstand bei Magendilatation mit pCO2-Anstieg",
        answerD = "Aldosteronmangel durch Adrenale Erschoepfung bei chronischem Erbrechen",
        correctAnswer = 1,
        explanation = "HCl-Verlust durch Erbrechen entzieht dem Blut Saeure, HCO3 steigt an (Genese). Der resultierende Volumenmangel aktiviert RAAS -- Aldosteron foerdert renale Na-Rueckresorption auf Kosten von H+ und K+-Sekretion. Hypokalaemie foerdert zusaetzlich H+-Exkretion. Niere 'verteidigt' das HCO3 trotz Alkalose -- paradoxe saure Urin (Erhaltung der Alkalose).",
        difficulty = 4,
        funFact = "Die Hypochloridam-Alkalose (Saline-responsive Alkalose) bei Erbrechen wird mit NaCl-Infusion behandelt -- Chloridzufuhr ermoeglicht der Niere, HCO3 auszuscheiden. Eine Kochsalzinfusion zur Behandlung einer Alkalose -- auf den ersten Blick paradox, aber pathophysiologisch logisch."
    ),

    // Question 46 -- Radiology: Westermark Sign
    Question(
        categoryId = 16,
        questionText = "Das 'Westermark-Zeichen' im Thorax-Roentgen bei Lungenembolie beschreibt was?",
        answerA = "Pleuraergusse beidseits als Folge kardiovaskulaerer Rechtsherzbelastung",
        answerB = "Fokale Oligaemie (verminderter Gefaesszeichnung) distal des Embolus durch fehlende Perfusion des betroffenen Lungensegments",
        answerC = "Erweiterung des rechten Herzens im Roentgenbild als Zeichen akuter Rechtsherzbelastung",
        answerD = "Verschiebung des Mediastinums zur embolisierten Seite durch Atelektasen",
        correctAnswer = 1,
        explanation = "Das Westermark-Zeichen beschreibt eine relative Aufhellung (Oligaemie) im Lungenfeld distal eines grossen Embolus -- die Gefaesszeichnung ist durch fehlende Perfusion vermindert. Es tritt bei grossen, zentralen Embolien auf und ist in ca. 10-15% der Faelle sichtbar -- ein empfindliches aber spezifisches Zeichen.",
        difficulty = 4,
        funFact = "Niels Westermark beschrieb dieses Zeichen 1938 -- lange vor der CT-Aera war es eines der wenigen roentgenologischen Hinweise auf Lungenembolie. Das klassische Thorax-Roentgen ist bei PE-Verdacht heute meistens normal oder zeigt nur unspezifische Befunde."
    ),

    // Question 47 -- Burns: Inhalation Injury
    Question(
        categoryId = 16,
        questionText = "Welche drei Mechanismen beschreiben das Inhalationstrauma und welche direkte Massnahme hat hoechste Prioritaet?",
        answerA = "Thermischer Schaden nur der Atemwege, CO-Vergiftung, Zyanidvergiftung -- Prioritaet: Kortikosteroide",
        answerB = "Thermischer Schaden oberer Atemwege, CO-Intoxikation (Haemoglobinbindung), toxische Verbrennung der unteren Atemwege durch Rauchpartikel -- Prioritaet: 100% O2 (Hochfluss), fruehzeitige Intubation bei Oedemprogression",
        answerC = "Nur CO-Intoxikation und Methaaemoglobinaemie -- Prioritaet: Antidottherapie mit Hydroxocobalamin",
        answerD = "Allergische Reaktion auf Verbrennungsgase, Hyperkapnie und HCN-Vergiftung -- Prioritaet: Bronchodilatatoren",
        correctAnswer = 1,
        explanation = "Inhalationstrauma umfasst: (1) thermischen Schaden der oberen Atemwege mit Oedembildung (Stridor, Heiserkeit), (2) CO-Vergiftung durch kompetitive Haemoglobinbindung, (3) toxische Pneumonitis durch chemische Verbrennung (Akrolein, HCN). Sofortmassnahme: 100% O2 per non-rebreather Maske (halbiert CO-HB-Halbwertszeit von 5h auf 1,5h), fruehzeitige Atemwegssicherung bei Zeichen der Oedemprogression.",
        difficulty = 4,
        funFact = "CO-Haemoglobin erscheint im Pulsoxymeter als normal gesaettigtes Blut -- Pulsoximetrie unterscheidet nicht zwischen Oxyhaemoglobin und CO-Haemoglobin. Bei Verdacht auf CO-Vergiftung ist Co-Oximetrie im BGA zwingend."
    ),

    // Question 48 -- Intensive Care: Transfusion Triggers
    Question(
        categoryId = 16,
        questionText = "Bei welchem Haemoglobinwert ist eine Erythrozytentransfusion bei stabilen, nicht-kardialen ICU-Patienten nach aktueller TRICC-Studie gerechtfertigt?",
        answerA = "Hb < 10 g/dl -- liberale Strategie reduziert Komplikationen bei kritisch kranken Patienten",
        answerB = "Hb < 7 g/dl -- restriktive Strategie ist der liberalen Strategie (Hb < 10) mindestens ebenbuertig und reduziert Transfusionsvolumen und -komplikationen",
        answerC = "Hb < 5 g/dl -- Transfusion erst bei Zeichen der Gewebehypoxie trotz Optimierung",
        answerD = "Kein fixer Wert -- Transfusion nur bei klinischen Hypoxiezeichen, unabhaengig vom Hb-Wert",
        correctAnswer = 1,
        explanation = "Die TRICC-Studie (1999) zeigte, dass bei haemodynamisch stabilen ICU-Patienten eine restriktive Transfusionsstrategie (Hb < 7 g/dl als Trigger) genauso sicher ist wie eine liberale (Hb < 10 g/dl). Ausnahmen: akutes Koronarsyndrom, aktive Blutung, haemodynamische Instabilitaet.",
        difficulty = 4,
        funFact = "Jede Erythrozytentransfusion traegt Immunmodulations-, Infektions- und Fehlbehandlungsrisiken. 'One size fits all' gilt nicht -- bei kardialen Patienten empfehlen Leitlinien oft einen Trigger von Hb < 8 g/dl wegen erhoehtem myokardialem Sauerstoffbedarf."
    ),

    // Question 49 -- Surgery: Thyroidectomy Complications
    Question(
        categoryId = 16,
        questionText = "Welche beiden spezifischen Komplikationen muss man nach totaler Thyreoidektomie engmaschig ueberwachen und durch welche klinischen Zeichen erkennt man sie?",
        answerA = "Hypothyreose (Muedigkeit, Gewichtszunahme) und Wundinfektion (Roetung, Eiter)",
        answerB = "Hypoparathyreoidismus mit Hypokalzaemie (Chvostek-Zeichen, Trousseau-Zeichen, Kribbeln, Tetanie) und N.-recurrens-Parese (Heiserkeit, Schluckbeschwerden, Stridor)",
        answerC = "Thyreotoxische Krise (Hyperthermie, Tachyarrhythmie) und Trachealkompression durch Haematom",
        answerD = "Hypothyreoidismus und Kehlkopfoedem durch Intubationstrauma",
        correctAnswer = 1,
        explanation = "Nach totaler Thyreoidektomie drohen: (1) Hypoparathyreoidismus durch akzidentelle Entfernung der Nebenschilddruesen (kritische Hypokalzaemie: Chvostek-Zeichen = Zucken bei Beklopfen des N. facialis, Trousseau-Zeichen = Karpopedalspasmen bei Blutdruckmessung, Kribbeln perioral/Extremitaeten). (2) N.-recurrens-Laesion: einseitig = Heiserkeit, beidseitig = Stridor und respiratorische Insuffizienz.",
        difficulty = 4,
        funFact = "Das postoperative Serumkalzium sollte am Abend des Operationstages gemessen werden -- ein Abfall unter 2,0 mmol/l erfordert orale Kalzium- und Vitamin-D-Substitution. Prophylaktische Kalziumgabe nach totaler Thyreoidektomie verkuerzt den Krankenhausaufenthalt."
    ),

    // Question 50 -- Intensive Care: Vasopressor Hierarchy
    Question(
        categoryId = 16,
        questionText = "Was ist die empfohlene Vasopressoren-Hierarchie beim refraktaeren septischen Schock laut aktueller SSC-Guideline?",
        answerA = "Dopamin als Erstlinientherapie, Adrenalin als zweite Linie, Vasopressin als dritte Linie",
        answerB = "Noradrenalin (erste Wahl) plus Vasopressin (zum Noradrenalinsparenden Effekt) plus Adrenalin (dritte Linie) -- Dopamin nur in besonderen Faellen",
        answerC = "Phenylephrin als erstes Mittel bei Tachykardie, dann Noradrenalin, dann Vasopressin",
        answerD = "Adrenalin als universeller Vasopressor, Noradrenalin nur bei Bradykardie",
        correctAnswer = 1,
        explanation = "SSC 2021: Noradrenalin ist Erstlinien-Vasopressor beim septischen Schock (starke Empfehlung). Vasopressin kann hinzugefuegt werden, um den Noradrenalinspiegel zu senken oder MAP-Ziel zu erreichen (schwache Empfehlung). Adrenalin ist dritte Linie. Dopamin nur bei bestimmten Patienten (bradykard, niedriges Schlagvolumen). Vermeidung von Dopamin als Vasopressor (hoehere Arrhythmierate).",
        difficulty = 4,
        funFact = "Die VASST-Studie zeigte, dass Vasopressin in niedrigen Dosen (0,03 U/min) als Noradrenalin-Spareffekt genutzt werden kann -- Sterblichkeit war nicht signifikant verschieden, aber der Noradrenalinsverbrauch sank. Vasopressin wirkt ueber V1-Rezeptoren und ist unabhaengig von Adrenozeptoren."
    )

)
