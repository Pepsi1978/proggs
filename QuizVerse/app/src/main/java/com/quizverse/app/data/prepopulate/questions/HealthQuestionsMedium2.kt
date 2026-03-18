package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMedium2(): List<Question> = listOf(

    // Question 1 - Diabetes Type 1 vs Type 2
    Question(
        categoryId = 16,
        questionText = "Was ist der grundlegende Unterschied zwischen Typ-1- und Typ-2-Diabetes?",
        answerA = "Typ 1 betrifft nur Kinder, Typ 2 nur Senioren",
        answerB = "Bei Typ 1 produziert die Bauchspeicheldruse kein Insulin mehr, bei Typ 2 wirkt Insulin nicht mehr richtig",
        answerC = "Typ 2 ist heilbar, Typ 1 nicht",
        answerD = "Beide Typen sind identisch, nur die Behandlung unterscheidet sich",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei Typ-1-Diabetes zerstoert das Immunsystem die insulinproduzierenden Betazellen. Bei Typ-2-Diabetes reagieren die Koerperzellen nicht mehr empfindlich genug auf Insulin (Insulinresistenz).",
        funFact = "Typ-1-Diabetes ist eine Autoimmunerkrankung -- das Immunsystem greift irrtuemlicherweise die eigenen Betazellen in der Bauchspeicheldruse an und zerstoert sie."
    ),

    // Question 2 - Insulin function
    Question(
        categoryId = 16,
        questionText = "Welche Aufgabe hat Insulin im Koerper?",
        answerA = "Es erhoeht den Blutdruck",
        answerB = "Es ermoeglicht den Koerperzellen, Zucker aus dem Blut aufzunehmen",
        answerC = "Es baut Muskeln auf",
        answerD = "Es reguliert den Schlaf-Wach-Rhythmus",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Insulin wirkt wie ein Schluessel: Es oeffnet die Zellen, sodass Glukose (Zucker) aus dem Blut in die Zellen eintreten und dort als Energie genutzt werden kann.",
        funFact = "Insulin wurde 1921 von Frederick Banting und Charles Best entdeckt. Vor dieser Entdeckung war Typ-1-Diabetes fast immer toedlich."
    ),

    // Question 3 - Blood sugar normal values
    Question(
        categoryId = 16,
        questionText = "Welcher Nueechternblutzuckerwert gilt als normal bei Erwachsenen?",
        answerA = "Unter 50 mg/dl",
        answerB = "70 bis 100 mg/dl",
        answerC = "120 bis 150 mg/dl",
        answerD = "200 mg/dl und darueber",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Ein Nueechternblutzucker zwischen 70 und 100 mg/dl gilt als normal. Werte ueber 126 mg/dl nueechtern deuteten auf Diabetes hin.",
        funFact = "Nach einer kohlenhydratreichen Mahlzeit kann der Blutzucker kurzzeitig auf 140 mg/dl ansteigen -- das ist bei gesunden Menschen voellig normal."
    ),

    // Question 4 - High blood pressure normal values
    Question(
        categoryId = 16,
        questionText = "Welcher Blutdruckwert gilt fuer Erwachsene als optimal?",
        answerA = "90/60 mmHg",
        answerB = "120/80 mmHg",
        answerC = "150/100 mmHg",
        answerD = "180/120 mmHg",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der optimale Blutdruck liegt bei 120/80 mmHg. Ab 140/90 mmHg spricht man von Bluthochdruck (Hypertonie), der behandelt werden sollte.",
        funFact = "Der erste Wert (systolisch) misst den Druck beim Herzschlag, der zweite (diastolisch) den Druck in der Entspannungsphase des Herzens zwischen zwei Schlaegen."
    ),

    // Question 5 - High blood pressure causes
    Question(
        categoryId = 16,
        questionText = "Was ist die haeufigste Ursache fuer Bluthochdruck?",
        answerA = "Zu viel Schlafen",
        answerB = "Uebergewicht, Bewegungsmangel, Stress und zu viel Salz",
        answerC = "Zu wenig Sonnenlicht",
        answerD = "Haeufiges Kaltwasserduschen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der sogenannte primaere Bluthochdruck (90 Prozent aller Faelle) entsteht durch ein Zusammenspiel von Lebensstilfaktoren wie Uebergewicht, Bewegungsmangel, Stress, Rauchen und hohem Salzkonsum.",
        funFact = "Bluthochdruck wird oft als 'stiller Killer' bezeichnet, weil er jahrelang ohne Symptome verlaeuft und trotzdem Herzinfarkte und Schlaganfaelle verursacht."
    ),

    // Question 6 - Dangers of high blood pressure
    Question(
        categoryId = 16,
        questionText = "Welche Gefahr droht langfristig bei unbehandeltem Bluthochdruck?",
        answerA = "Haarausfall",
        answerB = "Herzinfarkt, Schlaganfall und Nierenversagen",
        answerC = "Erblindung durch zu viel Licht",
        answerD = "Gelenkentzuendungen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Dauerhaft erhoeehter Blutdruck schaedigt die Blutgefaesswaende und kann zu Herzinfarkt, Schlaganfall, Herzinsuffizienz und Nierenversagen fuehren.",
        funFact = "Bluthochdruck ist weltweit die haeufigste Ursache fuer vermeidbare Todesfaelle -- er betrifft etwa 1,3 Milliarden Menschen."
    ),

    // Question 7 - Asthma definition
    Question(
        categoryId = 16,
        questionText = "Was passiert bei einem Asthmaanfall in den Atemwegen?",
        answerA = "Die Lunge fuellt sich mit Fluessigkeit",
        answerB = "Die Bronchien verengen sich und schleimhaeute schwellen an, sodass Luft kaum durchkommt",
        answerC = "Das Zwerchfell verkrampft sich dauerhaft",
        answerD = "Die Lungenblaeschen platzen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei Asthma verkrampft sich die Bronchialmuskulatur, die Schleimhaut schwillt an und es wird vermehrt Schleim gebildet -- all das verengt die Atemwege stark.",
        funFact = "Asthma ist eine der haeufigsten chronischen Erkrankungen weltweit -- etwa 300 Millionen Menschen sind betroffen, darunter viele Kinder."
    ),

    // Question 8 - Asthma vs COPD
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet COPD von Asthma?",
        answerA = "COPD betrifft nur Kinder, Asthma nur Erwachsene",
        answerB = "COPD ist meist dauerhaft und durch Rauchen verursacht, Asthma ist oft reversibel und allergisch bedingt",
        answerC = "COPD ist heilbar, Asthma nicht",
        answerD = "Beide Erkrankungen sind identisch",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "COPD (Chronisch obstruktive Lungenerkrankung) entsteht meist durch jahrelanges Rauchen und fuehrt zu dauerhafter Lungenschaedigung. Asthma ist dagegen oft allergisch bedingt und die Atemwegsobstruktion ist reversibel.",
        funFact = "COPD ist die dritthaeufigste Todesursache weltweit. Etwa 90 Prozent aller COPD-Faelle sind auf Rauchen zurueckzufuehren."
    ),

    // Question 9 - Heart attack symptoms
    Question(
        categoryId = 16,
        questionText = "Welches ist das typischste Symptom eines Herzinfarkts?",
        answerA = "Juckreiz an den Haenden",
        answerB = "Starker, anhaltender Druckschmerz in der Brust, oft in den linken Arm ausstrahlend",
        answerC = "Plotzlicher Haarausfall",
        answerD = "Ploetzliche Sehverbesserung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der klassische Herzinfarkt zeigt sich mit starkem Druck- oder Engegefuehl in der Brust (Vernichtungsschmerz), der in den linken Arm, Kiefer oder Ruecken ausstrahlen kann.",
        funFact = "Frauen zeigen bei einem Herzinfarkt haeufig atypische Symptome wie Uebelkeit, Rueckenschmerzen oder Kurzatmigkeit -- deshalb wird bei ihnen der Infarkt oefter zu spaet erkannt."
    ),

    // Question 10 - Stroke vs heart attack difference
    Question(
        categoryId = 16,
        questionText = "Was ist der Hauptunterschied zwischen einem Herzinfarkt und einem Schlaganfall?",
        answerA = "Ein Herzinfarkt betrifft das Gehirn, ein Schlaganfall das Herz",
        answerB = "Beim Herzinfarkt ist ein Herzgefaess verstopft, beim Schlaganfall ein Hirngefaess",
        answerC = "Beide sind dasselbe, nur unterschiedliche Bezeichnungen",
        answerD = "Ein Schlaganfall tritt nur nachts auf",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Beim Herzinfarkt ist ein Herzkranzgefaess blockiert und Herzmuskelgewebe stirbt ab. Beim Schlaganfall ist ein Blutgefaess im Gehirn verstopft oder gerissen und Hirngewebe wird geschaedigt.",
        funFact = "Jede Minute zaehlt: Pro Minute Schlaganfall sterben etwa 1,9 Millionen Nervenzellen ab. Schnelles Handeln kann bleibende Schaeden verhindern."
    ),

    // Question 11 - Stroke symptoms FAST
    Question(
        categoryId = 16,
        questionText = "Was bedeutet der FAST-Test zur Schlaganfallserkennung?",
        answerA = "Fieber, Atemnot, Schwitzen, Tachykardie",
        answerB = "Face (Gesicht), Arms (Arme), Speech (Sprache), Time (Zeit)",
        answerC = "Frostgefuehl, Ausschlag, Schmerzen, Taubheit",
        answerD = "Fluessigkeit, Aktivitaet, Sauerstoff, Therapie",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der FAST-Test hilft, Schlaganfall-Symptome zu erkennen: Haengt eine Gesichtsseite herab? Kann man einen Arm nicht heben? Ist die Sprache unklar? Dann sofort Zeit nutzen und 112 rufen.",
        funFact = "Etwa 80 Prozent aller Schlaganfaelle waeren durch Behandlung von Risikofaktoren wie Bluthochdruck, Rauchen und Bewegungsmangel vermeidbar."
    ),

    // Question 12 - Cancer definition
    Question(
        categoryId = 16,
        questionText = "Was ist Krebs grundsaetzlich?",
        answerA = "Eine Virusinfektion der weissen Blutkoerperchen",
        answerB = "Unkontrolliertes, boeartiges Wachstum koerpereigener Zellen",
        answerC = "Eine Autoimmunerkrankung der Haut",
        answerD = "Ein Mangel an roten Blutkoerperchen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Krebs entsteht, wenn Koerperzellen aufgrund von Genveraenderungen beginnen, sich unkontrolliert zu teilen und in umliegendes Gewebe einzuwachsen oder Tochtergeschwuelste (Metastasen) zu bilden.",
        funFact = "Im menschlichen Koerper entstehen taeglich Tausende von Krebszellen -- das Immunsystem erkennt und vernichtet die meisten davon, bevor sie sich vermehren koennen."
    ),

    // Question 13 - Most common cancer types
    Question(
        categoryId = 16,
        questionText = "Welcher Krebs ist bei Maennern in Deutschland am haeufigsten?",
        answerA = "Lungenkrebs",
        answerB = "Prostatakrebs",
        answerC = "Darmkrebs",
        answerD = "Hautkrebs",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Prostatakrebs ist die haeufigste Krebsdiagnose bei Maennern in Deutschland, gefolgt von Darmkrebs und Lungenkrebs. Fruehzeitige Vorsorgeuntersuchungen koennen die Heilungschancen deutlich verbessern.",
        funFact = "Ab dem 45. Lebensjahr haben Maenner Anspruch auf eine jaehrliche Prostatakrebs-Frueherkennung beim Urologen -- sie wird von den Krankenkassen bezahlt."
    ),

    // Question 14 - Cancer in women
    Question(
        categoryId = 16,
        questionText = "Welcher Krebs ist bei Frauen in Deutschland am haeufigsten?",
        answerA = "Gebaermutterhalskrebs",
        answerB = "Brustkrebs",
        answerC = "Eierstockkrebs",
        answerD = "Schilddruesenkrebs",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Brustkrebs ist mit Abstand die haeufigste Krebsdiagnose bei Frauen. Jaehrliche Mammographie-Screenings ab 50 Jahren helfen, den Krebs frueh zu entdecken.",
        funFact = "Jede achte Frau in Deutschland erkrankt im Laufe ihres Lebens an Brustkrebs. Die 5-Jahres-Ueberlebensrate bei fruehzeitiger Diagnose liegt bei ueber 90 Prozent."
    ),

    // Question 15 - Arthritis vs arthrosis
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet Arthritis von Arthrose?",
        answerA = "Arthritis betrifft Knochen, Arthrose betrifft Nerven",
        answerB = "Arthritis ist eine Gelenkentzuendung, Arthrose ist ein Gelenkverschleiss",
        answerC = "Arthrose ist eine Entzuendung, Arthritis ist ein Verschleiss",
        answerD = "Beide Begriffe bedeuten dasselbe",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Arthritis ist eine (oft immunologisch bedingte) Entzuendung der Gelenke. Arthrose hingegen ist degenerativer Knorpelverschleiss durch Alter, Belastung oder Verletzungen.",
        funFact = "Arthrose ist die haeufigste Gelenkerkrankung weltweit -- etwa 25 Prozent der Erwachsenen sind davon betroffen, besonders Knie- und Hueeftgelenke."
    ),

    // Question 16 - Rheumatoid arthritis
    Question(
        categoryId = 16,
        questionText = "Was ist rheumatoide Arthritis?",
        answerA = "Eine Verschleisserkrankung durch zu viel Sport",
        answerB = "Eine Autoimmunerkrankung, bei der das Immunsystem die eigenen Gelenkschleimhaeute angreift",
        answerC = "Eine Pilzinfektion der Gelenke",
        answerD = "Ein Vitaminmangel, der Gelenke schwacht",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei rheumatoider Arthritis greift das Immunsystem irrtuemlicherweise die Gelenkschleimhaut an, was zu Entzuendung, Schmerzen, Schwellung und langfristiger Gelenkzerstoerung fuehrt.",
        funFact = "Rheumatoide Arthritis befaellt meist mehrere Gelenke gleichzeitig und ist morgens besonders schmerzhaft (Morgensteifigkeit) -- ein typisches Erkennungsmerkmal."
    ),

    // Question 17 - Osteoporosis
    Question(
        categoryId = 16,
        questionText = "Was ist Osteoporose?",
        answerA = "Eine Entzuendung der Knochen",
        answerB = "Ein Knochenschwund, bei dem die Knochen an Dichte verlieren und bruchanfaelliger werden",
        answerC = "Ein Knochengeschwulst",
        answerD = "Eine Infektion des Knochenmarks",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Osteoporose bezeichnet einen Abbau der Knochenmasse und -struktur. Die Knochen werden poroes und bruechen leichter -- besonders Wirbelknochen, Huefte und Handgelenk sind gefaehrdet.",
        funFact = "Osteoporose wird oft erst erkannt, wenn ein Knochen bricht. Frauen nach der Menopause sind besonders gefaehrdet, da der Oestrogenmangel den Knochenabbau beschleunigt."
    ),

    // Question 18 - Osteoporosis prevention
    Question(
        categoryId = 16,
        questionText = "Was beugt Osteoporose am besten vor?",
        answerA = "Schonung und Bettruhe",
        answerB = "Ausreichend Kalzium und Vitamin D sowie regelmaessige koerperliche Aktivitaet",
        answerC = "Viel Fleisch essen",
        answerD = "Vermeidung von Sonnenlicht",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Kalzium ist der Hauptbaustoff der Knochen, Vitamin D foerdert seine Aufnahme. Koerperliche Aktivitaet (besonders Krafttraining) regt den Knochenaufbau an und verstaerkt die Knochen.",
        funFact = "Die maximale Knochendichte wird im Alter von etwa 30 Jahren erreicht. Was man in jungen Jahren an Knochenmasse aufbaut, ist ein 'Vorrat' gegen den spaete Osteoporose."
    ),

    // Question 19 - Migraine vs tension headache
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet Migraene von einem normalen Spannungskopfschmerz?",
        answerA = "Migraene ist immer kurzfristiger als Spannungskopfschmerz",
        answerB = "Migraene ist einseitig, pulsierend, sehr stark und begleitet von Uebelkeit oder Lichtempfindlichkeit",
        answerC = "Spannungskopfschmerz ist stets einseitig",
        answerD = "Beide sind identisch, nur die Intensitaet unterscheidet sich",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Migraene ist charakteristisch durch einseitigen, pulsierenden, starken Schmerz, oft begleitet von Uebelkeit, Erbrechen sowie Licht- und Laermempfindlichkeit -- manchmal mit visueller Aura.",
        funFact = "Migraene ist eine neurologische Erkrankung, keine normale Kopfschmerzform. Sie betrifft weltweit etwa 1 Milliarde Menschen und ist haeufiger bei Frauen als bei Maennern."
    ),

    // Question 20 - Migraine aura
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter der 'Aura' bei Migraene?",
        answerA = "Eine Entspannungsphase vor dem Schmerz",
        answerB = "Neurologische Symptome wie Sehstoerungen oder Kribbeln, die dem Kopfschmerz vorausgehen",
        answerC = "Die nachwirkende Erschoepfung nach der Migraene",
        answerD = "Das Geraeuch von Kaeese, das Migraene ausloest",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Aura ist eine Phase reversibler neurologischer Stoerungen vor dem Migraeneschmerz -- oft Sehstoerungen (Lichtblitze, Flimmern), Kribbeln in den Glieder oder Sprachstoerungen.",
        funFact = "Nur etwa 25 bis 30 Prozent aller Migraenepatienten erleben eine Aura. Trotzdem dient sie manchmal als Fruehwarnzeichen, um rechtzeitig Medikamente einzunehmen."
    ),

    // Question 21 - Anemia definition
    Question(
        categoryId = 16,
        questionText = "Was ist eine Anaemie (Blutarmut)?",
        answerA = "Zu viele rote Blutkoerperchen im Blut",
        answerB = "Zu wenige rote Blutkoerperchen oder zu wenig Haemoglobin, wodurch weniger Sauerstoff transportiert wird",
        answerC = "Eine Infektion der weissen Blutkoerperchen",
        answerD = "Zu duennes Blut durch viel Trinken",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Anaemie bedeutet Mangel an funktionsfaehigen roten Blutkoerperchen (Erythrozyten) oder Haemoglobin. Dadurch kann das Blut weniger Sauerstoff zu den Organen transportieren.",
        funFact = "Anaemie ist weltweit die haeufigste Bluterkrankung -- besonders betroffen sind Frauen im gebaerfaehigen Alter, Schwangere und Menschen mit Ernaehrungsmangeln."
    ),

    // Question 22 - Iron deficiency anemia
    Question(
        categoryId = 16,
        questionText = "Welche Symptome deuten auf einen Eisenmangel hin?",
        answerA = "Gelbfaerbung der Haut und starkes Schwitzen",
        answerB = "Muedigkeit, Blaesse, Schwindel und Konzentrationsprobleme",
        answerC = "Ploetzliche Gewichtszunahme und Herzklopfen",
        answerD = "Haarwuchs und erhoehte Koerpertemperatur",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eisenmangel ist die haeufigste Ursache fuer Anaemie. Typische Symptome sind Muedigkeit, Blaesse der Haut und Schleimhaeute, Schwindel, Konzentrationsschwaeche und Kurzatmigkeit.",
        funFact = "Eisen wird fuer die Bildung von Haemoglobin benoetigt, dem roten Blutfarbstoff. Tierisches Eisen aus Fleisch wird vom Koerper bis zu dreimal besser aufgenommen als pflanzliches Eisen."
    ),

    // Question 23 - Thyroid - hypothyroidism
    Question(
        categoryId = 16,
        questionText = "Was sind typische Symptome einer Schilddruesenunterfunktion (Hypothyreose)?",
        answerA = "Herzrasen, Gewichtsverlust und Schlaflosigkeit",
        answerB = "Muedigkeit, Gewichtszunahme, Frieren und Verstopfung",
        answerC = "Ploetzlicher Haarwuchs am ganzen Koerper",
        answerD = "Starker Durst und haeufiges Wasserlassen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei Schilddruesenunterfunktion produziert die Schilddruese zu wenige Hormone, wodurch der Stoffwechsel verlangsamt wird -- Folge: Muedigkeit, Gewichtszunahme, Kaltegefuehl und Verdauungsprobleme.",
        funFact = "Die Schilddruese ist ein kleines schmetterlingsfoermiges Organ am Hals, das aber den Stoffwechsel des ganzen Koerpers steuert und fast jede Koerperfunktion beeinflusst."
    ),

    // Question 24 - Thyroid - hyperthyroidism
    Question(
        categoryId = 16,
        questionText = "Was sind typische Symptome einer Schilddruesenuberfunktion (Hyperthyreose)?",
        answerA = "Muedigkeit und Gewichtszunahme",
        answerB = "Herzrasen, Gewichtsverlust, Zittern und Hitzegefuehl",
        answerC = "Gedaechtnisverlust und Schlafsucht",
        answerD = "Hautaustrocknung und Haarausfall",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei Ueberfunktion produziert die Schilddruese zu viele Hormone -- der Stoffwechsel laeuft auf Hochtouren: Herzrasen, Schweissausbrueche, Gewichtsverlust trotz gutem Appetit und Nervositaet sind typisch.",
        funFact = "Der Morbus Basedow ist die haeufigste Ursache fuer Schilddruesenuberfunktion. Charakteristisch sind dabei auch hervortretende Augen (Exophthalmus)."
    ),

    // Question 25 - Epilepsy basics
    Question(
        categoryId = 16,
        questionText = "Was ist Epilepsie?",
        answerA = "Eine psychische Erkrankung durch Stress",
        answerB = "Eine neurologische Erkrankung mit wiederkehrenden epileptischen Anfaellen durch unkontrollierte elektrische Entladungen im Gehirn",
        answerC = "Ein Herzrythmusfehler",
        answerD = "Eine Schlaferkrankung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Epilepsie ist eine chronische Hirnerkrankung, bei der wiederholt epileptische Anfaelle auftreten -- verursacht durch unkontrollierte, synchrone elektrische Entladungen von Nervenzellen im Gehirn.",
        funFact = "Epilepsie ist eine der aeltesten bekannten Erkrankungen -- schon in altaegyptischen Texten aus dem Jahr 1700 v. Chr. werden epileptische Anfaelle beschrieben."
    ),

    // Question 26 - What to do during epileptic seizure
    Question(
        categoryId = 16,
        questionText = "Was soll man tun, wenn jemand einen epileptischen Anfall hat?",
        answerA = "Einen Loeffel oder Stift in den Mund legen, damit er sich nicht beisst",
        answerB = "Die Person schlagen, damit sie aufwacht",
        answerC = "Gefaehrliche Gegenstaende entfernen, die Person nicht festhalten und nach dem Anfall in stabile Seitenlage bringen",
        answerD = "Die Person sofort ins Bett tragen",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Bei einem epileptischen Anfall die Umgebung sichern, nichts in den Mund legen (Verletzungsgefahr!), die Person nicht festhalten und nach dem Anfall in die stabile Seitenlage bringen.",
        funFact = "Der Mythos, dass man jemanden im epileptischen Anfall am Schlucken der Zunge hindern muss, ist falsch -- man kann die eigene Zunge anatomisch nicht verschlucken."
    ),

    // Question 27 - Diabetes complications
    Question(
        categoryId = 16,
        questionText = "Welche Folgeerkrankung tritt haeufig bei langjahrigem, schlecht eingestelltem Diabetes auf?",
        answerA = "Haarausfall",
        answerB = "Netzhautschaedigung (Retinopathie) und Nierenschaedigung (Nephropathie)",
        answerC = "Uebermaessiges Knochenwachstum",
        answerD = "Erhoehte Infektanfaelligkeit der Lunge",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Anhaltend hoher Blutzucker schaedigt die kleinen Blutgefaesse -- besonders in der Netzhaut (Retinopathie, droht Erblindung) und den Nieren (Nephropathie, droht Dialysepflicht).",
        funFact = "Diabetes ist weltweit eine der haeufigsten Ursachen fuer Erblindung im Erwachsenenalter und fuer dialysepflichtiges Nierenversagen."
    ),

    // Question 28 - Type 2 diabetes risk factors
    Question(
        categoryId = 16,
        questionText = "Welcher Lebensstilfaktor erhoht das Risiko fuer Typ-2-Diabetes am starksten?",
        answerA = "Zu wenig Schlaf",
        answerB = "Uebergewicht, insbesondere Bauchfett",
        answerC = "Zu viel Obst essen",
        answerD = "Haeufiges Tanzen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bauchfett (viszerales Fett) produziert Entzuendungsstoffe und Hormone, die die Insulinwirkung verschlechtern. Uebergewicht -- besonders am Bauch -- ist der staerkste vermeidbare Risikofaktor fuer Typ-2-Diabetes.",
        funFact = "Bereits eine Gewichtsabnahme von 5 bis 10 Prozent des Koerpergewichts kann bei Praediabetes die Entwicklung eines Typ-2-Diabetes deutlich verzoegern oder ganz verhindern."
    ),

    // Question 29 - COPD main cause
    Question(
        categoryId = 16,
        questionText = "Was ist die mit Abstand haeufigste Ursache fuer COPD?",
        answerA = "Allergien gegen Haustiere",
        answerB = "Langjaehriges Rauchen",
        answerC = "Haeufige Erkaeltungen in der Kindheit",
        answerD = "Uebermassiger Kaffeekonsum",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Rauchen ist fuer etwa 90 Prozent aller COPD-Erkrankungen verantwortlich. Tabakrauch zerstoert langfristig die Flimmerhaarchen und Lungenblaeschen und fuehrt zu dauerhaftem Lungenschaden.",
        funFact = "COPD entwickelt sich ueber viele Jahrzehnte still -- die meisten Patienten bemerken erst im fortgeschrittenen Stadium deutliche Atemnot. Bis dahin ist bereits viel Lungengewebe dauerhaft zerstoert."
    ),

    // Question 30 - Asthma triggers
    Question(
        categoryId = 16,
        questionText = "Was koennen typische Ausloeserfaktoren (Trigger) eines Asthmaanfalls sein?",
        answerA = "Viel schlafen und ruhig atmen",
        answerB = "Pollen, Tierhaare, kalte Luft, koerperliche Belastung oder Stress",
        answerC = "Warmes Wetter und gute Luftqualitaet",
        answerD = "Viel Wasser trinken",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Asthmaanfaelle werden durch individuelle Trigger ausgeloest, die die Atemwege reizen: Allergene (Pollen, Milben, Tierhaare), Kaltluft, koerperliche Belastung, Rauchen oder emotionaler Stress.",
        funFact = "Auch Aspirin kann bei manchen Menschen Asthmaanfaelle ausloesen -- dieses Phaenomen heisst 'Analgetika-Asthma' oder 'Aspirin-exazerbierte Atemwegserkrankung'."
    ),

    // Question 31 - Cancer metastasis
    Question(
        categoryId = 16,
        questionText = "Was sind Metastasen?",
        answerA = "Gutartige Tumore, die sich auflosen koennen",
        answerB = "Tochtergeschwuelste, die entstehen, wenn Krebszellen in andere Koerperregionen streuen",
        answerC = "Entartete weisse Blutkoerperchen",
        answerD = "Bereiche gesunden Gewebes um einen Tumor",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Krebszellen koennen sich vom Ursprungstumor loesen und ueber Blut- oder Lymphbahnen in andere Koerperregionen wandern, wo sie neue Tumore (Metastasen) bilden.",
        funFact = "Das Vorhandensein von Metastasen ist einer der wichtigsten Faktoren fuer die Krebsprognose -- metastasierende Krebserkrankungen sind schwerer zu behandeln als lokalisierte Tumore."
    ),

    // Question 32 - High blood pressure treatment
    Question(
        categoryId = 16,
        questionText = "Welche nicht-medikamentose Massnahme kann Bluthochdruck effektiv senken?",
        answerA = "Taeglich kalte Bader nehmen",
        answerB = "Regelmaessige koerperliche Aktivitaet und Salzreduzierung in der Ernaehrung",
        answerC = "Mehr schlafen als 10 Stunden pro Nacht",
        answerD = "Mehr Kaffee trinken",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Ausdauersport senkt den Blutdruck nachweislich um 5 bis 10 mmHg. Weniger als 5 Gramm Salz taeglich zu konsumieren kann den systolischen Blutdruck ebenfalls deutlich reduzieren.",
        funFact = "Regelmaessige Ausdaueraktivitaet wie 30 Minuten zueigiges Gehen an den meisten Tagen der Woche kann den Blutdruck aehnlich stark senken wie ein einzelnes blutdrucksenkendes Medikament."
    ),

    // Question 33 - Stroke risk factors
    Question(
        categoryId = 16,
        questionText = "Welcher Faktor erhoht das Schlaganfallrisiko am staerksten?",
        answerA = "Zu wenig Fernseher schauen",
        answerB = "Unbehandelter Bluthochdruck",
        answerC = "Zu viel Schlafen",
        answerD = "Vegetarische Ernaehrung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bluthochdruck ist der wichtigste Risikofaktor fuer Schlaganfall -- er belastet die Hirngefaesse dauerhaft und erhoht das Risiko fuer Gefaessrisse und Gerinnsel.",
        funFact = "Vorhofflimmern, eine haeufige Herzrhythmusstoerung, verzehnfacht das Schlaganfallrisiko, weil sich in den Herzvorhoefen leicht Blutgerinnsel bilden koennen."
    ),

    // Question 34 - Thyroid - goiter
    Question(
        categoryId = 16,
        questionText = "Wodurch entsteht ein Kropf (Vergroesserung der Schilddruese) haeufig?",
        answerA = "Durch zu viel Vitamin C",
        answerB = "Durch Jodmangel",
        answerC = "Durch zu wenig Schlaf",
        answerD = "Durch Luftverschmutzung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Jodarmes Ernaehrung ist weltweit die haeufigste Ursache fuer Schilddruesenvergroesserung (Struma). Der Jodbedarf der Schilddruese wird durch Jodmangel nicht gedeckt, sie wachst als Kompensation.",
        funFact = "Deutschland ist ein Jodmangelland -- deshalb wird Speisesalz in Deutschland haeufig mit Jod angereichert. Jodiertes Speisesalz hat dazu beigetragen, die Kropfhaeufigkeit deutlich zu reduzieren."
    ),

    // Question 35 - Iron absorption
    Question(
        categoryId = 16,
        questionText = "Was verbessert die Eisenaufnahme aus pflanzlichen Lebensmitteln?",
        answerA = "Milch gleichzeitig trinken",
        answerB = "Vitamin C (z.B. Orangensaft) zur gleichen Mahlzeit",
        answerC = "Schwarztee zur Mahlzeit trinken",
        answerD = "Mehr Kohlenhydrate essen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vitamin C wandelt dreiwertiges Eisen in zweiwertiges um, das der Darm viel besser aufnehmen kann. Schwarztee hingegen enthalt Gerbstoffe (Tannine), die die Eisenaufnahme hemmen.",
        funFact = "Haem-Eisen aus tierischen Quellen (Fleisch, Fisch) wird mit 15 bis 35 Prozent Aufnahmerate aufgenommen, waehrend Non-Haem-Eisen aus Pflanzen nur 2 bis 20 Prozent Aufnahmerate hat."
    ),

    // Question 36 - Osteoporosis risk factors
    Question(
        categoryId = 16,
        questionText = "Wer hat das hoechste Risiko fuer Osteoporose?",
        answerA = "Junge Maenner mit viel Sport",
        answerB = "Aeltere Frauen nach der Menopause mit geringer Kalziumaufnahme",
        answerC = "Kinder unter 10 Jahren",
        answerD = "Uebergewichtige Menschen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Nach der Menopause sinkt der Oestrogengehalt, was den Knochenabbau beschleunigt. Zusammen mit altersbedingtem Knochenverlust und Kalziummangel ist das Osteoporoserisiko bei aelteren Frauen besonders hoch.",
        funFact = "Weltweit erleidet alle drei Sekunden ein Mensch einen osteoporosebedingten Knochenbruch. Besonders gefuerchtet ist der Schenkelhalbruch bei alteren Menschen."
    ),

    // Question 37 - Heart attack treatment
    Question(
        categoryId = 16,
        questionText = "Was ist beim Herzinfarkt die entscheidende Sofortmassnahme im Krankenhaus?",
        answerA = "Bettruhe fuer 48 Stunden",
        answerB = "Moeglichst schnelle Wiederoeffnung des blockierten Herzgefaesses durch Herzkatheter oder Lyse",
        answerC = "Sofortige Herzoperation",
        answerD = "Gabe von Antibiotika",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Beim Herzinfarkt muss das verstopfte Herzkranzgefaess so schnell wie moeglich wieder eroeffnet werden -- am besten durch einen Herzkathetereingriff (PCI). Jede Minute Verzoegerung bedeutet mehr abgestorbenes Herzgewebe.",
        funFact = "Die sogenannte 'door-to-balloon-time' (Zeit vom Krankenhauseingang bis zur Wiedererofnung des Gefaesses) sollte idealerweise unter 90 Minuten liegen."
    ),

    // Question 38 - Epilepsy types
    Question(
        categoryId = 16,
        questionText = "Was ist ein 'Absence' im Kontext von Epilepsie?",
        answerA = "Eine Ohnmacht durch Blutdruckabfall",
        answerB = "Ein kurzer Bewusstseinsausfall ohne Krampfe, bei dem der Betroffene starr wirkt",
        answerC = "Ein Schlafanfall tagsuebers",
        answerD = "Eine Gedaechtnisluecke nach starkem Trinken",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Absencen sind ein Anfallstyp bei Epilepsie -- der Betroffene 'schaltet' fuer wenige Sekunden weg, stiert ins Leere und reagiert nicht, dann geht es normal weiter, ohne dass Krampfe auftreten.",
        funFact = "Absencen dauern oft nur 5 bis 30 Sekunden und kommen bei Kindern besonders haeufig vor. Lehrer verwechseln sie manchmal mit Unaufmerksamkeit oder Traumerei."
    ),

    // Question 39 - Cancer screening
    Question(
        categoryId = 16,
        questionText = "Ab welchem Alter empfiehlt sich in Deutschland die Darmkrebsvorsorge durch Darmspiegelung?",
        answerA = "Ab 20 Jahren",
        answerB = "Ab 50 Jahren",
        answerC = "Ab 70 Jahren",
        answerD = "Nur bei Familiengeschichte mit Darmkrebs",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Ab 50 Jahren haben gesetzlich Versicherte in Deutschland Anspruch auf die kostenlose Darmkrebsfrueherkennung durch Koloskopie. Fruehes Entfernen von Polypen verhindert Krebs.",
        funFact = "Darmkrebsvorstufen (Polypen) wachsen oft ueber Jahre, bevor sie boesartig werden. Eine Darmspiegelung kann diese Vorstufen entfernen, bevor Krebs entsteht."
    ),

    // Question 40 - Migraine treatment
    Question(
        categoryId = 16,
        questionText = "Welche Medikamentengruppe wurde speziell zur Migraenebehandlung entwickelt?",
        answerA = "Antibiotika",
        answerB = "Triptane",
        answerC = "Antihistaminika",
        answerD = "Betablocker",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Triptane sind die bekanntesten spezifischen Migraene-Medikamente -- sie wirken, indem sie bestimmte Serotoninrezeptoren im Gehirn aktivieren und so den Migraeneschmerz gezielt bekampfen.",
        funFact = "Betablocker, eigentlich Herzmedikamente, koennen zur Vorbeugung von Migraene eingesetzt werden -- aber nicht zur Behandlung des akuten Anfalls."
    ),

    // Question 41 - Anemia types
    Question(
        categoryId = 16,
        questionText = "Welche Vitamine sind -- neben Eisen -- wichtig fuer die Blutbildung?",
        answerA = "Vitamin A und Vitamin K",
        answerB = "Vitamin B12 und Folsaeure",
        answerC = "Vitamin C und Vitamin E",
        answerD = "Vitamin D und Vitamin B6",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vitamin B12 und Folsaeure sind fuer die Reifung der roten Blutkoerperchen notwendig. Ein Mangel fuehrt zur sogenannten megaloblastischen Anaemie, bei der die roten Blutkoerperchen zu gross und unreif sind.",
        funFact = "Vitamin B12 kommt fast ausschliesslich in tierischen Produkten vor. Veganer muessen es deshalb supplementieren, sonst droht ein Mangel, der erst nach Jahren Symptome zeigt."
    ),

    // Question 42 - High blood pressure symptoms
    Question(
        categoryId = 16,
        questionText = "Warum wird Bluthochdruck 'stiller Killer' genannt?",
        answerA = "Weil er lautlos im Schlaf auftritt",
        answerB = "Weil er meist keine spuerbaren Symptome verursacht und trotzdem Organe schaedigt",
        answerC = "Weil die Medikamente dagegen still machen",
        answerD = "Weil er nur bei stillem Charakter vorkommt",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bluthochdruck verursacht meistens keine Beschwerden -- deshalb wissen viele Betroffene gar nichts davon, waehrend Herz, Gehirn, Nieren und Augen bereits geschaedigt werden.",
        funFact = "Etwa ein Drittel aller Erwachsenen in Deutschland hat Bluthochdruck -- aber fast die Haelfte davon weiss es nicht, weil keine Symptome auftreten."
    ),

    // Question 43 - COPD symptoms
    Question(
        categoryId = 16,
        questionText = "Was ist das typische Leitsymptom der fruehen COPD?",
        answerA = "Starkes Niesen am Morgen",
        answerB = "Husten, Auswurf und Atemnot bei Belastung ('Raucherlunge')",
        answerC = "Ploetzliche Brustschmerzen nachts",
        answerD = "Geschwollene Beine",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die fruehe COPD zeigt sich oft als 'Raucherlunge' mit morgens starkem Husten und Auswurf sowie zunehmender Atemnot bei koerperlicher Belastung -- viele Betroffene sehen das faelschlicherweise als normal an.",
        funFact = "Der 'Raucherhuster' am Morgen ist kein normales Phaaenomen -- er ist ein fruehes Warnsignal fuer eine sich entwickelnde COPD und sollte aerztlich abgeklaert werden."
    ),

    // Question 44 - Rheumatoid arthritis treatment
    Question(
        categoryId = 16,
        questionText = "Was ist das Ziel der Behandlung von rheumatoider Arthritis?",
        answerA = "Die Erkrankung vollstaendig zu heilen durch eine einmalige Impfung",
        answerB = "Entzuendung zu hemmen, Schmerzen zu lindern und Gelenkschaeden zu verlangsamen",
        answerC = "Betroffene Gelenke sofort zu entfernen",
        answerD = "Nur Schmerzlinderung ohne Eingriff in das Immunsystem",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Rheumatoide Arthritis ist nicht heilbar, aber gut behandelbar. Ziel ist es, die Entzuendungsaktivitaet zu stoppen, Schmerzen zu lindern und die Zerstoerung der Gelenke zu verhindern.",
        funFact = "Sogenannte Biologika -- gentechnisch hergestellte Eiweisse -- haben die Behandlung der rheumatoiden Arthritis in den letzten 20 Jahren revolutioniert und vielen Patienten ein normales Leben ermoeglicht."
    ),

    // Question 45 - Diabetes measurement
    Question(
        categoryId = 16,
        questionText = "Was misst der HbA1c-Wert bei Diabetikern?",
        answerA = "Den aktuellen Blutzuckerspiegel",
        answerB = "Den durchschnittlichen Blutzucker der letzten 2 bis 3 Monate",
        answerC = "Die Menge des produzierten Insulins",
        answerD = "Den Blutdruck waehrend der letzten Woche",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "HbA1c (glykiertes Haemoglobin) spiegelt den durchschnittlichen Blutzuckerspiegel der letzten 2 bis 3 Monate wider und gibt einen besseren Ueberblick ueber die Blutzuckereinstellung als eine Einzelmessung.",
        funFact = "Rote Blutkoerperchen leben etwa 3 Monate -- in dieser Zeit lagern sich Zuckermolek?le ans Haemoglobin an. Je mehr Zucker im Blut war, desto mehr HbA1c findet sich."
    ),

    // Question 46 - Skin cancer types
    Question(
        categoryId = 16,
        questionText = "Welcher Hautkrebs ist am gefaehrlichsten, weil er frueh Metastasen bildet?",
        answerA = "Basalzellkarzinom",
        answerB = "Malignes Melanom (schwarzer Hautkrebs)",
        answerC = "Aktinische Keratose",
        answerD = "Plattenepithelkarzinom",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das maligne Melanom ist die gefaehrlichste Form des Hautkrebses, weil es sehr frueh in Lymphknoten und andere Organe metastasiert. Fruehzeitige Diagnose ist entscheidend fuer die Heilungschancen.",
        funFact = "Die ABCDE-Regel hilft bei der Selbsterkennung verdaechtiger Muttermale: Asymmetrie, Begrenzung (unregelmassig), Color (verschiedene Farben), Durchmesser (ueber 5 mm), Evolution (Veraenderung)."
    ),

    // Question 47 - Heart failure
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Herzinsuffizienz?",
        answerA = "Ein zu schneller Herzrhythmus",
        answerB = "Die Unfaehigkeit des Herzens, genug Blut und Sauerstoff durch den Koerper zu pumpen",
        answerC = "Eine Entzuendung des Herzmuskels",
        answerD = "Das Verschliessen einer Herzklappe",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Herzinsuffizienz bedeutet, dass das Herz nicht mehr ausreichend Blut in den Koerper pumpen kann. Typische Zeichen sind Atemnot, Muedigkeit und Wassereinlagerungen in den Beinen.",
        funFact = "Herzinsuffizienz ist der haeufigste Grund fuer Krankenhausaufnahmen bei Menschen ueber 65 Jahren in Deutschland und weltweit ein massives Gesundheitsproblem."
    ),

    // Question 48 - Epilepsy medication
    Question(
        categoryId = 16,
        questionText = "Wie nennt man die Medikamente, die epileptische Anfaelle verhindern sollen?",
        answerA = "Antikoagulanzien",
        answerB = "Antiepileptika (Antikonvulsiva)",
        answerC = "Antidepressiva",
        answerD = "Antihistaminika",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Antiepileptika (auch Antikonvulsiva) sind Medikamente, die die Reizschwelle der Nervenzellen im Gehirn erhoehen und so epileptische Anfaelle verhindern oder verringern.",
        funFact = "Etwa 70 Prozent der Epilepsiepatienten koennen durch Medikamente anfallsfrei werden. Bei stabiler Anfallsfreiheit darf in Deutschland auch wieder Auto gefahren werden."
    ),

    // Question 49 - Migraine triggers
    Question(
        categoryId = 16,
        questionText = "Was sind typische Migraene-Ausloser?",
        answerA = "Zu viel Sport und frische Luft",
        answerB = "Schlafmangel oder zu viel Schlaf, Hormonschwankungen und bestimmte Lebensmittel",
        answerC = "Zu viel Wasser trinken",
        answerD = "Sonnenlicht und warme Temperaturen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Migraene-Trigger sind individuell verschieden -- typische Ausloser sind unregelmaessiger Schlaf, Hormonschwankungen (vor allem bei Frauen), Stress, Rotwein, gereifter Kaese und Wetteraenderungen.",
        funFact = "Das Schokolade als haeufiger Migraene-Trigger gilt, ist wissenschaftlich nicht eindeutig belegt -- manche Forschende vermuten, dass der Heisshunger auf Suesses ein fruehes Migraene-Vorzeichen ist, nicht die Ursache."
    ),

    // Question 50 - Anemia in vegetarians
    Question(
        categoryId = 16,
        questionText = "Warum haben Vegetarier und Veganer ein erhoehtes Risiko fuer Eisenmangel?",
        answerA = "Weil sie generell zu wenig essen",
        answerB = "Weil pflanzliches Eisen schlechter aufgenommen wird als tierisches Eisen",
        answerC = "Weil Gemuese den Eisenbedarf steigert",
        answerD = "Weil Obst die Eisenresorption hemmt",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Pflanzliche Lebensmittel enthalten nur Non-Haem-Eisen, das deutlich schlechter aus dem Darm aufgenommen wird als das Haem-Eisen aus Fleisch. Zudem hemmen Phytate in Getreideprodukten die Eisenaufnahme.",
        funFact = "Huelsenfruechte wie Linsen und Kichererbsen sind eisenreiche Pflanzenquellen. Wer sie einweicht und kocht und dazu Vitamin-C-reiche Lebensmittel isst, verbessert die Eisenaufnahme erheblich."
    )

)
