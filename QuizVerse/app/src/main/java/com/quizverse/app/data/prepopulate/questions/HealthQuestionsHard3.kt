package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsHard3(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 16,
        questionText = "Welche Zellen werden durch HIV bevorzugt infiziert und zerstoert?",
        answerA = "B-Lymphozyten",
        answerB = "CD4-positive T-Helferzellen",
        answerC = "Neutrophile Granulozyten",
        answerD = "Natuerliche Killerzellen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "HIV bindet ueber sein Glykoprotein gp120 an den CD4-Rezeptor und den Korezeptor CCR5 oder CXCR4 auf T-Helferzellen und zerstoert sie -- ein CD4-Wert unter 200/microliter definiert AIDS.",
        funFact = "Ein kleiner Anteil der Bevoelkerung traegt eine CCR5-delta32-Mutation und ist dadurch weitgehend immun gegen HIV-Infektionen -- dieser genetische Schutz wurde als Basis fuer die CCR5-Blocker-Therapie genutzt."
    ),

    // Question 2
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter der antiretroviralen Kombinationstherapie cART bei HIV?",
        answerA = "Eine Impfung, die die Viruslast dauerhaft auf null senkt",
        answerB = "Eine Kombination mehrerer Medikamente, die verschiedene Schritte des HIV-Replikationszyklus hemmen",
        answerC = "Eine einmalige Infusion, die HIV aus dem Koerper eliminiert",
        answerD = "Eine Therapie ausschliesslich mit Proteaseinhibitoren",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "cART (combinierte antiretrovirale Therapie) nutzt mindestens drei Wirkstoffe aus verschiedenen Klassen -- z.B. Reverse-Transkriptase-Hemmer, Proteaseinhibitoren und Integraseinhibitoren -- um Resistenzentwicklung zu verhindern.",
        funFact = "Seit Einfuehrung der cART in den 1990er Jahren hat sich die Lebenserwartung HIV-positiver Menschen dramatisch verbessert -- heute koennen viele Patienten ein nahezu normales Leben fuehren."
    ),

    // Question 3
    Question(
        categoryId = 16,
        questionText = "Was bedeutet 'latente Tuberkulose'?",
        answerA = "Eine bereits behandelte und ausgeheilte TB-Infektion",
        answerB = "Eine Infektion mit Mycobacterium tuberculosis ohne aktive Erkrankung und ohne Ansteckungsgefahr",
        answerC = "Eine multiresistente Form der Tuberkulose",
        answerD = "Tuberkulose ausschliesslich in den Lymphknoten",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Bei latenter TB lebt Mycobacterium tuberculosis im Koerper, wird aber durch das Immunsystem in Granulomen eingeschlossen -- der Mensch ist nicht krank und nicht infektioese, hat aber ein Reaktivierungsrisiko.",
        funFact = "Schatzungsweise ein Viertel der Weltbevoelkerung -- ueber 2 Milliarden Menschen -- traegt eine latente TB-Infektion in sich, ohne es zu wissen."
    ),

    // Question 4
    Question(
        categoryId = 16,
        questionText = "Welches Antibiotikum ist das Mittel der Wahl bei der Erstlinientherapie der aktiven Tuberkulose?",
        answerA = "Amoxicillin und Clavulansaeure",
        answerB = "Isoniazid, Rifampicin, Pyrazinamid und Ethambutol (HRZE-Schema)",
        answerC = "Ciprofloxacin und Metronidazol",
        answerD = "Penicillin G und Streptomycin",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die TB-Therapie folgt dem HRZE-Schema: zwei Monate vier Medikamente (Isoniazid, Rifampicin, Pyrazinamid, Ethambutol), dann vier Monate zwei Medikamente (Isoniazid, Rifampicin) -- Gesamtdauer mindestens sechs Monate.",
        funFact = "Die lange Behandlungsdauer ist noetig, weil Mycobacterium tuberculosis sehr langsam waechst und in Ruhephasen (sog. Persistern) gegen kuerzere Therapien weitgehend unempfindlich ist."
    ),

    // Question 5
    Question(
        categoryId = 16,
        questionText = "Welcher Plasmodium-Typ verursacht die gefahrlichste Form der Malaria (Malaria tropica)?",
        answerA = "Plasmodium vivax",
        answerB = "Plasmodium ovale",
        answerC = "Plasmodium falciparum",
        answerD = "Plasmodium malariae",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Plasmodium falciparum verursacht Malaria tropica, die schwerste Form, da befallene Erythrozyten im Mikrogefaessnetz der Organe verklumpen und zu Zerebralmalaria, Multiorganversagen und Tod fuehren koennen.",
        funFact = "Malaria tropica durch P. falciparum toetet noch immer ueber 600.000 Menschen pro Jahr -- die meisten davon sind Kinder unter fuenf Jahren in Subsahara-Afrika."
    ),

    // Question 6
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Hepatitis B und Hepatitis C hinsichtlich der Uebertragung?",
        answerA = "Hepatitis B wird nur faekal-oral uebertragen, Hepatitis C parenteral",
        answerB = "Beide werden identisch uebertragen -- kein Unterschied",
        answerC = "Hepatitis B wird parenteral und sexuell uebertragen, Hepatitis C vorwiegend parenteral (Blut-zu-Blut)",
        answerD = "Hepatitis C wird nur durch Troepfcheninfektion uebertragen",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Hepatitis B ist hochinfektioese und wird parenteral, sexuell und perinatal uebertragen. Hepatitis C wird fast ausschliesslich durch Blut-zu-Blut-Kontakt uebertragen -- sexuelle Uebertragung ist selten.",
        funFact = "Hepatitis B ist 50- bis 100-mal infektioser als HIV -- ein Mikroliter infiziertes Blut kann ausreichen, um die Krankheit zu uebertragen."
    ),

    // Question 7
    Question(
        categoryId = 16,
        questionText = "Was beschreibt der Begriff 'Basisreproduktionszahl R0' in der Epidemiologie?",
        answerA = "Die Sterblichkeitsrate einer Infektionskrankheit in einer bestimmten Region",
        answerB = "Die durchschnittliche Anzahl von Neuinfektionen, die ein einziger Infizierter in einer voellig empfaenglichen Bevoelkerung verursacht",
        answerC = "Die Inkubationszeit einer Infektionskrankheit in Tagen",
        answerD = "Den prozentualen Anteil Geimpfter in der Bevoelkerung",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "R0 beschreibt das epidemiologische Ausbreitungspotenzial: Bei R0 > 1 breitet sich eine Krankheit aus, bei R0 < 1 erlischt sie. Masern haben R0 von 12-18, SARS-CoV-2 Original ca. 2-3.",
        funFact = "Der Deltavariant von SARS-CoV-2 hatte ein R0 von etwa 5-6, die Omikronvariante sogar 8-15 -- damit war Omikron ansteckender als Windpocken."
    ),

    // Question 8
    Question(
        categoryId = 16,
        questionText = "Wie hoch muss die Herdenimmunitaetsrate bei Masern mindestens sein, damit Herdenschutz funktioniert?",
        answerA = "Etwa 50 Prozent",
        answerB = "Etwa 70 Prozent",
        answerC = "Etwa 85 Prozent",
        answerD = "Etwa 95 Prozent",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Aufgrund des sehr hohen R0 der Masern (12-18) muessen etwa 92-95 Prozent der Bevoelkerung immun sein, um Herdenschutz zu erreichen -- diese Formel lautet: Herdenimmunitat = 1 - (1/R0).",
        funFact = "Selbst ein kleiner Rueckgang der Impfquote von 95 auf 90 Prozent reicht aus, um Masernausbrueche zu ermoeglichen -- das erklaert, warum selbst westliche Laender immer wieder Ausbrueche erleben."
    ),

    // Question 9
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Inzidenz und Praevalenz in der Epidemiologie?",
        answerA = "Inzidenz misst alte Faelle, Praevalenz neue Faelle",
        answerB = "Inzidenz bezeichnet die Neuerkrankungsrate in einem Zeitraum, Praevalenz den Anteil aller Erkrankten zu einem Zeitpunkt",
        answerC = "Beide Begriffe sind synonym und werden wechselweise verwendet",
        answerD = "Inzidenz ist nur fuer akute Erkrankungen, Praevalenz nur fuer chronische",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Inzidenz = Neuerkrankungen pro Zeitraum (z.B. 100 neue Faelle pro 100.000 Einwohner pro Jahr). Praevalenz = alle aktuell Erkrankten zu einem Stichtag oder in einem Zeitraum -- wichtig fuer die Gesundheitsplanung.",
        funFact = "Fuer lange chronische Krankheiten ist die Praevalenz hoch, weil Patienten lange krank bleiben -- fuer toedliche Akutkrankheiten kann die Praevalenz trotz hoher Inzidenz niedrig sein."
    ),

    // Question 10
    Question(
        categoryId = 16,
        questionText = "Welches Protein auf der Oberflaeche von SARS-CoV-2 ist entscheidend fuer den Eintritt in menschliche Zellen?",
        answerA = "Haemagglu-tinin",
        answerB = "Das Spike-Protein (S-Protein)",
        answerC = "Die RNA-Polymerase",
        answerD = "Neuraminidase",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Das Spike-Protein von SARS-CoV-2 bindet an den ACE2-Rezeptor auf menschlichen Zellen. Alle mRNA-Impfstoffe (BioNTech, Moderna) instruieren den Koerper, dieses Spike-Protein zu produzieren, um Antikoerper zu bilden.",
        funFact = "ACE2-Rezeptoren sind besonders dicht in Lunge, Herz, Niere und Darm vorhanden -- das erklaert, warum COVID-19 nicht nur die Lunge, sondern viele Organe gleichzeitig befallen kann."
    ),

    // Question 11
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet den antigenic shift vom antigenic drift beim Influenzavirus?",
        answerA = "Antigenic drift ist gefaehrlicher als antigenic shift",
        answerB = "Antigenic drift sind kleine Mutationen, antigenic shift ist ein abrupter Austausch ganzer Gensegmente zwischen verschiedenen Virusststaemmen",
        answerC = "Antigenic shift tritt nur bei Influenza B auf, drift bei Influenza A",
        answerD = "Beide Begriffe beschreiben dasselbe Phaenomen mit unterschiedlichen Namen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Antigenic drift: langsame Punktmutationen, die saisonale Impfanpassungen erfordern. Antigenic shift: abrupte Neukombination von Haemagglu-tinin/Neuraminidase zwischen Tier- und Menschenviren -- kann Pandemien ausloesen.",
        funFact = "Die Spanische Grippe 1918 (H1N1), die Asiatische Grippe 1957 (H2N2) und die Hongkong-Grippe 1968 (H3N2) entstanden alle durch antigenic shift -- ein 'Reassortment' von Voegel- und Menschenviren."
    ),

    // Question 12
    Question(
        categoryId = 16,
        questionText = "Wie wird das Ebolavirus hauptsaechlich uebertragen?",
        answerA = "Durch Troepfcheninfektion wie bei Influenza",
        answerB = "Durch direkten Kontakt mit Koerperfluessigkeiten infizierter Personen",
        answerC = "Durch Insektenstiche wie bei Malaria",
        answerD = "Durch kontaminiertes Trinkwasser",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Ebolavirus wird durch direkten Kontakt mit Blut, Sekreten oder Organen infizierter Menschen oder Tiere uebertragen -- keine aerogene Uebertragung, was Ausbrueche eingrenzbar macht.",
        funFact = "Fruechtefledermaeuse gelten als das natuerliche Reservoir von Ebolavirus -- Menschen infizieren sich meist durch Kontakt mit toten Tieren oder durch den Verzehr von Buschfleisch."
    ),

    // Question 13
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen SARS (2002/03) und MERS hinsichtlich des Reservoirts?",
        answerA = "SARS kommt aus Kaemelen, MERS aus Fledermaeuse",
        answerB = "SARS hatte Zibetkatzen als Zwischenwirt, MERS hat Dromedarkamele als Zwischenwirt",
        answerC = "Beide Viren haben identische Tierreservoire",
        answerD = "MERS ist keine Zoonose -- es entstand direkt im Menschen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "SARS-CoV-1 wurde wahrscheinlich von Fledermaeuse ueber Zibetkatzen auf Menschen uebertragen. MERS-CoV wird bis heute sporadisch durch Dromedarkamele auf Menschen uebertragen -- hauptsaechlich auf der Arabischen Halbinsel.",
        funFact = "MERS hat eine Sterblichkeitsrate von etwa 35 Prozent -- deutlich hoeher als SARS mit ca. 10 Prozent und COVID-19 mit unter 1 Prozent. Der Grund fuer die begrenzte Ausbreitung ist die schlechtere Mensch-zu-Mensch-Uebertragung."
    ),

    // Question 14
    Question(
        categoryId = 16,
        questionText = "Was sind nosokomiale Infektionen?",
        answerA = "Infektionen durch importierte Tropenkrankheiten",
        answerB = "Infektionen, die im Zusammenhang mit einem Krankenhausaufenthalt oder medizinischen Einrichtungen erworben werden",
        answerC = "Infektionen, die ausschliesslich durch Antibiotikaresistenz entstehen",
        answerD = "Nur Wundinfektionen nach chirurgischen Eingriffen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Nosokomiale (krankenhauserworbene) Infektionen entstehen 48 Stunden oder spaeter nach Krankenhausaufnahme -- haeufige Erreger sind MRSA, VRE, Klebsiella und Clostridium difficile.",
        funFact = "In Deutschland erkranken jaehrlich ca. 400.000 bis 600.000 Menschen an nosokomialen Infektionen -- davon sterben etwa 10.000 bis 20.000, was diese Infektionen zu einem der groessten Sicherheitsprobleme im Gesundheitswesen macht."
    ),

    // Question 15
    Question(
        categoryId = 16,
        questionText = "Welchen Pathomechanismus hat Clostridium difficile bei Krankenhausinfektionen?",
        answerA = "Es produziert Betalaktamasen, die Antibiotika unwirksam machen",
        answerB = "Es bildet Toxine (Toxin A und B), die den Darmepithelzellen schaedigen, besonders nach Antibiotika-Therapie",
        answerC = "Es verursacht Sepsis durch direkte Invasion in den Blutkreislauf",
        answerD = "Es produziert Endotoxine, die Nierenversagen ausloesen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "C. difficile besiedelt den Darm wenn die normale Mikrobiota durch Antibiotika gestoert wird. Seine Toxine A und B schaedigen das Darmepithel und verursachen pseudomembranoese Kolitis -- bis hin zu lebensgefahrlichem toxischem Megakolon.",
        funFact = "Clostridium difficile bildet extrem widerstandsfaehige Sporen, die auf Oberflaechen monatelang ueberleben koennen -- normales Haendedesinfektionsmittel toetet die Sporen nicht, nur gruendliches Haendewaschen mit Seife hilft."
    ),

    // Question 16
    Question(
        categoryId = 16,
        questionText = "Was beschreibt 'Long COVID' und welche Organe sind haeufig betroffen?",
        answerA = "Eine Reinfektion mit COVID-19 innerhalb von 30 Tagen nach Erstinfektion",
        answerB = "Anhaltende oder neu auftretende Symptome mehr als 12 Wochen nach COVID-19-Infektion, besonders Erschoepfung, Hirnnebel und Dyspnoe",
        answerC = "Ein schwerer COVID-19-Verlauf mit Beatmungspflicht auf der Intensivstation",
        answerD = "Langzeitkomplikationen ausschliesslich bei Ungeimpften",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Long COVID (Post-COVID-Syndrom) bezeichnet Symptome, die nach der akuten Phase andauern -- am haeufigsten Fatigue, kognitive Einschraenkungen (brain fog), Dyspnoe und posturale Tachykardie (POTS).",
        funFact = "Aktuelle Studien schaetzen, dass 10 bis 20 Prozent aller COVID-19-Infizierten Long-COVID-Symptome entwickeln -- selbst nach milden Verlaeufen -- was Millionen von Patienten weltweit betrifft."
    ),

    // Question 17
    Question(
        categoryId = 16,
        questionText = "Was ist eine Zoonose und welche der folgenden ist ein klassisches Beispiel?",
        answerA = "Eine durch Umweltverschmutzung entstehende Krankheit -- Beispiel: Mesotheliom",
        answerB = "Eine Infektionskrankheit, die natuerlich zwischen Tier und Mensch uebertragbar ist -- Beispiel: Tollwut",
        answerC = "Eine Erbkrankheit, die nur bei Tieren vorkommt",
        answerD = "Eine durch Ernaehrungsfehler entstehende Erkrankung -- Beispiel: Skorbut",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Zoonosen sind Infektionskrankheiten mit natuerlicher Uebertragung zwischen Tier und Mensch -- Tollwut (Carnivoren), Lyme-Borreliose (Zecken/Rehe), Vogelgrippe (Voegel) und Ebola (Fledermaeuse) sind klassische Beispiele.",
        funFact = "Schatzungsweise 60 Prozent aller neu entstehenden Infektionskrankheiten des Menschen haben zoonotischen Ursprung -- die Zerstoerung natuerlicher Lebensraeume bringt Mensch und Wildtiere naeher zusammen."
    ),

    // Question 18
    Question(
        categoryId = 16,
        questionText = "Welche Influenzatypen koennen Pandemien verursachen?",
        answerA = "Nur Influenza A",
        answerB = "Influenza A und B gleichermassen",
        answerC = "Influenza B und C, aber nicht A",
        answerD = "Alle drei Typen A, B und C gleichermassen",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Nur Influenza A kann Pandemien verursachen, da es das groesste Reservoir (Wasservoegel, Saeugetiere) hat und durch antigenic shift neue Subtypen bilden kann. Influenza B infiziert nur Menschen und verursacht saisonale Epidemien.",
        funFact = "Influenza C ist so mild, dass die meisten Menschen eine Infektion gar nicht bemerken -- es verursacht nur leichte Erkaltungssymptome und spielt epidemiologisch keine bedeutende Rolle."
    ),

    // Question 19
    Question(
        categoryId = 16,
        questionText = "Was ist die Rolle des CD4-Wertes beim Management von HIV-Patienten?",
        answerA = "Er misst die Virusmenge im Blut und zeigt die Therapieeffizienz",
        answerB = "Er gibt die Anzahl der T-Helferzellen pro Mikroliter Blut an und spiegelt den Immunstatus wider",
        answerC = "Er misst die Antikoerperkonzentration gegen HIV",
        answerD = "Er bestimmt die Resistenzmuster des Virus gegenueber Medikamenten",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der CD4-Wert zeigt, wie stark das Immunsystem durch HIV beschaedigt ist. Unter 500/microliter beginnt cART, unter 200/microliter liegt AIDS vor -- opportunistische Infektionen wie PCP oder Toxoplasmose drohen.",
        funFact = "Mit erfolgreicher cART kann der CD4-Wert sich vollstaendig erholen und ein HIV-positiver Mensch ein praktisch normales Immunsystem erreichen -- viele Patienten haben CD4-Werte von 600-1000/microliter."
    ),

    // Question 20
    Question(
        categoryId = 16,
        questionText = "Wie heisst der Erreger der Lyme-Borreliose und wie wird er uebertragen?",
        answerA = "Borrelia burgdorferi, uebertragen durch den Biss von Ixodes-Zecken",
        answerB = "Treponema pallidum, uebertragen durch sexuellen Kontakt",
        answerC = "Rickettsia rickettsii, uebertragen durch Schildzecken",
        answerD = "Bartonella henselae, uebertragen durch Katzenkratzer",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Borrelia burgdorferi wird durch Ixodes-Zecken (Holzbock) uebertragen -- das Erythema migrans (Wanderroete) um den Stich ist das klassische Fruehzeichen. Spaetmanifestationen betreffen Gelenke, Herz und Nervensystem.",
        funFact = "Nicht jede Borreliose-infizierte Zecke uebertraegt die Erkrankung -- die Zecke muss mindestens 16-24 Stunden anhaften, damit ausreichend Borrelien in die Wunde gelangen."
    ),

    // Question 21
    Question(
        categoryId = 16,
        questionText = "Was ist die Besonderheit von Plasmodium vivax im Vergleich zu Plasmodium falciparum?",
        answerA = "P. vivax ist toedlicher und schwerer behandelbar",
        answerB = "P. vivax kann als Hypnozoit in der Leber ruhen und Monate bis Jahre spaeter Rueckfaelle verursachen",
        answerC = "P. vivax wird durch eine andere Mueckenart uebertragen",
        answerD = "P. vivax verursacht ausschliesslich zerebrales Befall",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Plasmodium vivax und P. ovale bilden Hypnozyten -- inaktive Leberstadien, die jahrelang im Koerper schlummern koennen. Zur vollstaendigen Heilung muss Primaquin eingesetzt werden, das diese Schlafstadien abtoetest.",
        funFact = "Britische Soldaten, die im Zweiten Weltkrieg in Asien dienten, erlitten noch Jahre nach Kriegsende Malaria-Rueckfaelle durch reaktivierte P.-vivax-Hypnozyten in der Leber."
    ),

    // Question 22
    Question(
        categoryId = 16,
        questionText = "Was beschreibt der Begriff 'opportunistische Infektion' bei AIDS-Patienten?",
        answerA = "Eine Infektion, die nur in Entwicklungslaendern vorkommt",
        answerB = "Eine Infektion durch normalerweise harmlose Erreger, die nur bei stark geschwachtem Immunsystem schwere Erkrankungen verursachen",
        answerC = "Eine neu entstandene resistente Infektion durch Antibiotikafehlgebrauch",
        answerD = "Eine Infektion, die ausschliesslich durch sexuellen Kontakt uebertragen wird",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Bei einem CD4-Wert unter 200/microliter drohen opportunistische Infektionen wie Pneumocystis-jirovecii-Pneumonie (PCP), zerebrale Toxoplasmose, CMV-Retinitis und Kryptokokkenmeningitis -- Erreger, die ein gesundes Immunsystem problemlos kontrolliert.",
        funFact = "Die Pneumocystis-jirovecii-Pneumonie (PCP) war in den 1980er Jahren eine der haeufigsten Todesursachen bei AIDS-Patienten -- bevor man erkannte, dass eine einfache Prophylaxe mit Trimethoprim-Sulfamethoxazol sie verhindern kann."
    ),

    // Question 23
    Question(
        categoryId = 16,
        questionText = "Welcher Hepatitis-Typ hat den groessten Anteil an der weltweiten Leberzirrhose und dem hepatozellulaeren Karzinom?",
        answerA = "Hepatitis A",
        answerB = "Hepatitis B und C gemeinsam",
        answerC = "Hepatitis D",
        answerD = "Hepatitis E",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Chronische Infektionen mit Hepatitis B und C verursachen weltweit die Mehrheit aller Faelle von Leberzirrhose und hepatozellulaeren Karzinomen -- Hepatitis B allein ist fuer etwa 50-55 Prozent aller Leberkrebserkrankungen weltweit verantwortlich.",
        funFact = "Hepatitis C ist seit 2015 heilbar -- direkte antivirale Wirkstoffe (DAAs) wie Sofosbuvir erreichen Heilungsraten von ueber 95 Prozent in 8-12 Wochen. Eine Impfung gegen Hepatitis C gibt es allerdings noch nicht."
    ),

    // Question 24
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen der Fallsterblichkeit (CFR) und der Infektionssterblichkeit (IFR) bei einer Epidemie?",
        answerA = "CFR und IFR sind identische Kennzahlen mit verschiedenen Namen",
        answerB = "CFR bezieht sich auf bestaettigte Faelle (oft hoeher), IFR auf alle Infizierten inklusive unentdeckter Faelle (oft niedriger)",
        answerC = "CFR gilt nur fuer Pandemien, IFR nur fuer endemische Erkrankungen",
        answerD = "IFR ist immer hoeher als CFR",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "CFR = Tote / bestaettigte Faelle (ueberschaetzt oft die Gefaehrlichkeit weil milde Faelle unentdeckt bleiben). IFR = Tote / alle Infizierten (genauer, aber schwerer zu messen) -- bei COVID-19 lag CFR initial bei 2-3%, IFR eher bei 0,1-0,5%.",
        funFact = "Das Problem mit CFR am Anfang einer Pandemie ist, dass hauptsaechlich schwere Faelle getestet werden -- dadurch wirkt die Krankheit anfangs toedlicher als sie tatsaechlich ist, was zu Ueberreaktionen fuehren kann."
    ),

    // Question 25
    Question(
        categoryId = 16,
        questionText = "Welche Impfstrategie wurde zur Ausrottung der Pocken eingesetzt?",
        answerA = "Massenimpfung der gesamten Weltbevoelkerung gleichzeitig",
        answerB = "Ring-Impfung: Impfung aller Kontaktpersonen um jeden neu entdeckten Fall",
        answerC = "Impfung nur von Hochrisikogruppen in Endemiegebieten",
        answerD = "Pflichtimpfung ausschliesslich in betroffenen Laendern",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die Ring-Impfung (Surveillance-Containment) isolierte Pocken-Erkrankte und impfte alle engen Kontaktpersonen sofort -- diese Strategie war effizienter als universelle Massenimpfung und fuehrte 1980 zur Ausrottung.",
        funFact = "Der letzte natuerliche Pockenfall weltweit wurde 1977 in Somalia entdeckt -- heute existiert Pockenvirus nur noch in zwei offiziell anerkannten Hochsicherheitslabors: in den USA (CDC Atlanta) und in Russland (VECTOR Nowosibirsk)."
    ),

    // Question 26
    Question(
        categoryId = 16,
        questionText = "Was ist die Funktion der Neuraminidase (N) beim Influenzavirus?",
        answerA = "Sie ermoeglicht das Andocken des Virus an die Wirtszelle",
        answerB = "Sie spaltet die Verbindung neu gebildeter Viren zur Wirtszellmembran und ermoeglicht ihre Freisetzung",
        answerC = "Sie produziert die viralen RNA-Strangkopien",
        answerD = "Sie hemmt das Immunsystem des Wirts",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Haemagglu-tinin bindet das Virus an Wirtszellen. Neuraminidase hingegen spaltet die Sialsaure-Bindung, damit neue Viruspartikel aus der Zelle freigesetzt werden koennen -- Oseltamivir (Tamiflu) hemmt genau dieses Enzym.",
        funFact = "Oseltamivir (Tamiflu) wurde aus Sternanis entwickelt -- der Wirkstoff Shikimisaeure, der als Vorstufe dient, wird grossteils aus dieser Gewuerzpflanze gewonnen."
    ),

    // Question 27
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter MRSA und warum ist er klinisch bedeutsam?",
        answerA = "Methicillin-resistenter Staphylococcus aureus -- ein Erreger, der gegen viele Antibiotika resistent ist und nosokomial schwer zu bekaempfen ist",
        answerB = "Ein mutierts Virus, das Resistenzen gegen antivirale Therapien entwickelt hat",
        answerC = "Eine Form von Streptokokken, die Rachenentzuendungen verursachen",
        answerD = "Ein Pilzerreger, der bevorzugt bei immungeschwachten Patienten vorkommt",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "MRSA (Methicillin-resistenter Staphylococcus aureus) traegt das mecA-Gen, das eine veraenderte Penicillinbindungsprotein produziert -- dadurch sind alle Betalaktam-Antibiotika unwirksam. Therapie der Wahl ist Vancomycin.",
        funFact = "Es gibt zwei Haupttypen von MRSA: HA-MRSA (hospital-acquired, im Krankenhaus) und CA-MRSA (community-acquired, in der Gemeinschaft erworben) -- CA-MRSA ist oft aggressiver und kann auch gesunde Menschen befallen."
    ),

    // Question 28
    Question(
        categoryId = 16,
        questionText = "Welcher Rezeptor auf menschlichen Zellen wird von Tollwutvirus bevorzugt genutzt und welches Organ befaellt es zuletzt?",
        answerA = "CD4-Rezeptor auf Immunzellen -- befaellt zuletzt die Leber",
        answerB = "Acetylcholinrezeptoren an Nervenzellen -- befaellt zuletzt das Gehirn (ZNS)",
        answerC = "ACE2-Rezeptor auf Lungenepithelzellen -- befaellt zuletzt die Lunge",
        answerD = "Glukoserezeptoren auf Muskelzellen -- befaellt zuletzt das Herz",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Tollwutvirus bindet an nikotinische Acetylcholinrezeptoren und breitet sich retrograd entlang der Nervenbahnen zum Zentralnervensystem aus -- bis zum Auftreten der Enzephalitis vergehen Wochen bis Monate.",
        funFact = "Tollwut ist nach Ausbruch der Enzephalitis nahezu 100 Prozent toedlich -- weltweit sterben jaehlrich noch immer 59.000 Menschen daran, fast alle in Asien und Afrika durch Hundebisse."
    ),

    // Question 29
    Question(
        categoryId = 16,
        questionText = "Was ist ein 'Superverbreiter' (superspreader) in der Epidemiologie?",
        answerA = "Ein besonders aggressives Virus, das viele Mutationen in kurzer Zeit anhaeuft",
        answerB = "Eine infizierte Person, die uberproportional viele weitere Personen ansteckt -- weit mehr als der Durchschnitt",
        answerC = "Ein Tier, das als Hauptreservoir einer Infektionskrankheit dient",
        answerD = "Ein Krankenhaus, in dem viele nosokomiale Infektionen entstehen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Superspreader infizieren unverhaaeltnismaessig viele andere -- bei COVID-19 und SARS waren Superspreader-Ereignisse in Innenraeumen fuer einen Grossteil der Ausbrueche verantwortlich (sog. 80/20-Regel: 20% der Infizierten verursachen 80% der Weiterinfektionen).",
        funFact = "Das beruhmteste historische Superspreader-Beispiel ist 'Typhoid Mary' (Mary Mallon), eine irische Koeechin in New York um 1900, die als asymptomatische Traeagerin Typhus auf Dutzende von Menschen uebertrug."
    ),

    // Question 30
    Question(
        categoryId = 16,
        questionText = "Welches Medikament wird zur Prophylaxe der Pneumocystis-jirovecii-Pneumonie (PCP) bei HIV-Patienten mit CD4 unter 200 eingesetzt?",
        answerA = "Oseltamivir",
        answerB = "Trimethoprim-Sulfamethoxazol (Cotrimoxazol)",
        answerC = "Isoniazid",
        answerD = "Chloroquin",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Cotrimoxazol (TMP-SMX) ist die Standardprophylaxe gegen PCP bei HIV-Patienten mit CD4 < 200/microliter und bei anderen Immundefizienzen -- es verhindert auch zerebrale Toxoplasmose.",
        funFact = "In den fruehen AIDS-Jahren starben viele Patienten an PCP, bevor die prophylaktische Wirkung von Cotrimoxazol erkannt wurde -- seit dieser Entdeckung sank die PCP-Sterblichkeit dramatisch."
    ),

    // Question 31
    Question(
        categoryId = 16,
        questionText = "Was ist der wesentliche Unterschied zwischen Hepatitis A und Hepatitis B bezueglich des Krankheitsverlaufs?",
        answerA = "Hepatitis A verursacht immer Leberversagen, Hepatitis B nie",
        answerB = "Hepatitis A heilt stets selbstlimitierend aus, Hepatitis B kann chronisch werden und zu Zirrhose fuehren",
        answerC = "Hepatitis B ist immer akut und kurzfristig, Hepatitis A immer chronisch",
        answerD = "Beide verlaufen identisch -- nur die Uebertragung unterscheidet sich",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Hepatitis A ist eine selbstlimitierende Erkrankung und wird niemals chronisch -- vollstaendige Ausheilung mit lebenslanger Immunitaet. Hepatitis B chronifiziert bei 5-10 Prozent der Erwachsenen und bei bis zu 90 Prozent der Neugeborenen.",
        funFact = "Hepatitis A wird durch Impfung und Haendehygiene verhindert -- bei Auslandsreisen in Endemiegebiete ist die Impfung dringend empfohlen. Hepatitis A durch Muscheln und Meeresfruchte ist in Europa ein klassischer Ausbruchsweg."
    ),

    // Question 32
    Question(
        categoryId = 16,
        questionText = "Was ist das 'window of susceptibility' bei Infektionskrankheiten neugeborener Kinder?",
        answerA = "Die Zeit nach dem 6. Lebensmonat, wenn der Schutz durch muetterliche Antikoerper nachlasst",
        answerB = "Die Fruehgeburtsperiode, in der noch keine Impfungen moeglich sind",
        answerC = "Der Zeitraum zwischen erstem Kontakt und Ausbruch der Erkrankung",
        answerD = "Die kritische Phase waehrend des Geburtskanals, in der Infektionen uebertragen werden",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Muetterliche IgG-Antikoerper schuetzen Saeuglinge die ersten Monate des Lebens -- nach 4-6 Monaten nehmen diese maternalen Antikoerper ab, bevor der eigene Impfschutz vollstaendig aufgebaut ist. In dieser Luecke sind Saeuglinge besonders gefaehrdet.",
        funFact = "Keuchhusten (Pertussis) ist besonders gefaehrlich in diesem Fenster -- deshalb empfehlen Gesundheitsbehoerden die Impfung von Schwangeren und engen Kontaktpersonen ('cocooning'), um das Neugeborene zu schuetzen."
    ),

    // Question 33
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die Antibiotikaresistenz durch Betalaktamasen?",
        answerA = "Betalaktamasen veraendern die Zellwand der Bakterien, sodass Antibiotika nicht mehr binden",
        answerB = "Betalaktamasen sind bakterielle Enzyme, die den Betalaktamring von Penicillinen und Cephalosporinen hydrolytisch spalten und damit inaktivieren",
        answerC = "Betalaktamasen pumpen Antibiotika aktiv aus der Bakterienzelle heraus",
        answerD = "Betalaktamasen binden Antibiotika irreversibel und neutralisieren sie ausserhalb der Zelle",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Betalaktamasen oeffnen enzymatisch den Betalaktamring -- die chemische Grundstruktur aller Penicilline und Cephalosporine -- und machen sie wirkungslos. ESBL (Extended Spectrum Beta-Lactamase) inaktivieren sogar Breitspektrum-Cephalosporine.",
        funFact = "Clavulansaeure, Sulbactam und Tazobactam sind Betalaktamase-Inhibitoren -- sie 'opfern' sich fuer das Enzym und schuetzen so das eigentliche Antibiotikum. Amoxicillin-Clavulansaeure (Augmentin) nutzt dieses Prinzip."
    ),

    // Question 34
    Question(
        categoryId = 16,
        questionText = "Was ist die Besonderheit des HIV-Virus im Vergleich zu anderen Viren hinsichtlich seines Genoms?",
        answerA = "HIV hat ein doppelstraengiges DNA-Genom wie menschliche Zellen",
        answerB = "HIV ist ein Retrovirus mit einem einzelstraengigen RNA-Genom, das durch Reverse Transkriptase in DNA umgeschrieben und ins Wirtsgenom eingebaut wird",
        answerC = "HIV hat ein segmentiertes RNA-Genom wie Influenza",
        answerD = "HIV repliziert ausschliesslich im Zellkern ohne Integration ins Wirtsgenom",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Als Retrovirus traegt HIV zwei Kopien einzelstraengiger RNA. Reverse Transkriptase (RT) schreibt diese in doppelstraengige DNA um, die dann durch die Integrase ins Chromatin der Wirtszelle eingebaut wird -- daher ist HIV nicht heilbar, nur kontrollierbar.",
        funFact = "Reverse Transkriptase macht mehr Fehler als normale DNA-Polymerase und korrigiert diese nicht -- das fuehrt zu hoher Mutationsrate, die zwar Resistenzen begienstigt, aber auch die Achillesferse bei der Impfstoffentwicklung ist."
    ),

    // Question 35
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Begriff 'Vektorkontrolle' bei Infektionskrankheiten wie Malaria?",
        answerA = "Die Entwicklung neuer Impfstoffe gegen vektoruebertragene Krankheiten",
        answerB = "Massnahmen zur Reduzierung der Population oder der Aktivitaet von Uebertragungsvektoren (z.B. Muecken)",
        answerC = "Die Kontrolle des Vektors im genetischen Sinne -- also der Virusgensequenz",
        answerD = "Die Isolation von Krankheitstraeagern in Quarantaenestationen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Vektorkontrolle umfasst Insektizid-behandelte Moskitonetze, Innenraumbesprueung (IRS), Beseitigung von Brutgewaessern und genetisch modifizierte sterile Muecken -- sie ist neben Chemoprophylaxe der Hauptpfeiler der Malariabekaempfung.",
        funFact = "Das Projekt 'sterile Insektentechnik' (SIT) setzt gentechnisch sterilisierte Muecken frei, die sich mit Wildmuecken paaren -- da keine Nachkommen entstehen, bricht die Population ein. In einigen Gebieten Brasiliens wurden Aedes-Mueckenpopulationen so um 90 Prozent reduziert."
    ),

    // Question 36
    Question(
        categoryId = 16,
        questionText = "Welches Antigen der Influenza ist Grundlage der Subtyp-Nomenklatur (z.B. H3N2)?",
        answerA = "Ribonukleoproteinkomplex (RNP) und Matrix-Protein M1",
        answerB = "Haemagglu-tinin (H) und Neuraminidase (N) -- Oberflaeachenproteine mit 18 bzw. 11 Subtypen",
        answerC = "Polymerase PB1 und PB2",
        answerD = "Nicht-strukturelle Proteine NS1 und NS2",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Influenza A wird anhand seiner Oberflaeachenproteine klassifiziert: 18 bekannte Haemagglu-tinin-Subtypen (H1-H18) und 11 Neuraminidase-Subtypen (N1-N11) -- alle 198 Kombinationen sind theoretisch moeglich, beim Menschen zirkulieren aber nur wenige.",
        funFact = "Die gefahrlichste pandemische Grippestamm im 20. Jahrhundert war H1N1 (Spanische Grippe 1918) -- derselbe H1N1-Subtyp kehrte 2009 als Schweinegrippe zurueck, war aber deutlich weniger virulent."
    ),

    // Question 37
    Question(
        categoryId = 16,
        questionText = "Was ist das Konzept der 'seroepidemiology' und wofuer wird es genutzt?",
        answerA = "Die genetische Analyse von Erregern in Abwassersystemen",
        answerB = "Die Analyse von Antikoerpern in Bevoelkerungsstichproben zur Bestimmung der tatsaechlichen Infektionsausbreitung",
        answerC = "Die Messung der Serumenzyme zur Leberschadenbestimmung",
        answerD = "Die Typisierung von Bakterien nach ihrer Serumresistenz",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Seroepidemiologie misst Antikoerperpraevalen in Bevoelkerungsstichproben -- dadurch werden auch asymptomatische und unentdeckte Infektionen erfasst. Bei COVID-19 zeigten Serostudien, dass die tatsaechliche Durchseuchung viel hoeher war als offiziell gemeldet.",
        funFact = "Seroepidemiologie spielte eine Schluesselrolle bei der Erkennung, dass COVID-19 viel haeufiger asymptomatisch verlief als anfangs gedacht -- in manchen Regionen waren 5-10 Mal mehr Menschen infiziert als offiziell bestaetigt."
    ),

    // Question 38
    Question(
        categoryId = 16,
        questionText = "Was ist der Mechanismus der Antibiotikaresistenz durch horizontalen Gentransfer?",
        answerA = "Spontane Mutationen in der DNA-Polymerase, die zufaellig Resistenzgene erzeugen",
        answerB = "Uebertragung von Resistenzgenen zwischen Bakterien durch Plasmide, Transformationen oder Transduktion",
        answerC = "Evolution der Bakterien ueber viele Generationen durch natuerliche Selektion ohne Gentransfer",
        answerD = "Biochemische Anpassung einzelner Bakterienzellen an das Antibiotikum",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Horizontaler Gentransfer ermoeglicht Bakterien, Resistenzgene untereinander auszutauschen -- durch Konjugation (Plasmide), Transformation (freie DNA) oder Transduktion (Bakteriophagen). So koennen Resistenzen schnell zwischen Arten springen.",
        funFact = "Resistenzgene fuer mehrere verschiedene Antibiotikaklassen koennen auf einem einzigen Plasmid gebundelt sein -- dieses Plasmid kann dann von einem empfindlichen auf ein resistentes Bakterium uebertragen werden und sofortige Multiresistenz verleihen."
    ),

    // Question 39
    Question(
        categoryId = 16,
        questionText = "Welches ist die wichtigste Uebertragungsroute fuer Hepatitis A?",
        answerA = "Parenteral (Blut-zu-Blut-Kontakt)",
        answerB = "Sexuell uebertragen",
        answerC = "Faekal-oral (verunreinigtes Wasser oder Lebensmittel)",
        answerD = "Durch Troepfcheninfektion beim Niesen",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Hepatitis A wird faekal-oral uebertragen -- kontaminiertes Wasser, rohe Muscheln und ungekochte Lebensmittel in Laendern mit schlechter Sanitaerhygiene sind typische Quellen. Haendehygiene und Impfung schuetzen effektiv.",
        funFact = "Der groesste Hepatitis-A-Ausbruch in den USA ereignete sich 2003 in einem Restaurant in Pennsylvania -- 555 Menschen erkrankten nach dem Genuss von mit dem Virus kontaminierten Fruehlingszwiebeln aus Mexiko."
    ),

    // Question 40
    Question(
        categoryId = 16,
        questionText = "Was bedeutet 'multiresistente Tuberkulose' (MDR-TB)?",
        answerA = "TB, die gegen alle bekannten Antibiotika resistent ist",
        answerB = "TB, die mindestens gegen Isoniazid und Rifampicin -- die zwei wichtigsten Erstlinienmedikamente -- resistent ist",
        answerC = "TB bei einem Patienten, der bereits mehrere Kurse Antibiotika erhalten hat",
        answerD = "TB-Staemme, die nur in bestimmten geografischen Regionen vorkommen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "MDR-TB (Multi-Drug-Resistant TB) ist per Definition resistent gegen Isoniazid und Rifampicin -- die zwei Eckpfeiler der Standardtherapie. XDR-TB (Extensively Drug Resistant) ist zusaetzlich gegen Fluorchinolone und injizierbare Zweitlinienmedikamente resistent.",
        funFact = "Die Behandlung von MDR-TB dauert 18-24 Monate und kostet das 100-fache der normalen TB-Therapie -- weltweit erkranken jaehlrich rund 500.000 Menschen an MDR-TB, besonders in Osteuropa und Zentralasien."
    ),

    // Question 41
    Question(
        categoryId = 16,
        questionText = "Was ist das Konzept der 'PrEP' bei HIV?",
        answerA = "Post-Expositionsprophylaxe -- Einnahme von HIV-Medikamenten nach einem Risikokonakt",
        answerB = "Praeexpositionsprophylaxe -- regelmaessige Einnahme von antiretroviralen Medikamenten vor einem Risikokontakt, um eine HIV-Infektion zu verhindern",
        answerC = "Prophylaxe gegen opportunistische Infektionen bei AIDS-Patienten",
        answerD = "Eine chirurgische Praeventionsmethode fuer HIV-gefaehrdete Personen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "PrEP (Pre-Exposure Prophylaxis) -- meist Tenofovir/Emtricitabin -- reduziert bei regelmaessiger Einnahme das HIV-Infektionsrisiko um ueber 99 Prozent. PEP (Post-Exposure Prophylaxis) wird innerhalb von 72 Stunden nach einem Risikokontakt begonnen.",
        funFact = "Seit 2019 ist PrEP in Deutschland gesetzlich kassenfinanziert fuer HIV-gefaehrdete Personen -- davor mussten Patienten die Kosten von etwa 500-800 Euro pro Monat selbst tragen."
    ),

    // Question 42
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem 'One Health'-Konzept in der Infektiologie?",
        answerA = "Ein Einheitliches Gesundheitssystem fuer alle Bevoelkerungsgruppen weltweit",
        answerB = "Der Ansatz, dass Mensch-, Tier- und Umweltgesundheit untrennbar miteinander verbunden sind und gemeinsam betrachtet werden muessen",
        answerC = "Eine Methode zur Berechnung des Gesamtgesundheitszustands einer Bevoelkerung",
        answerD = "Das Konzept, dass jeder Mensch nur an einer Erkrankung gleichzeitig leiden kann",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "One Health anerkennt, dass die meisten neuen Infektionskrankheiten des Menschen zoonotischen Ursprungs sind -- die Zerstoerung von Wildtierlebensraeumen, intensivierte Landwirtschaft und Klimawandel erhoehen das Zoonose-Risiko.",
        funFact = "Die WHO schaetzt, dass 75 Prozent aller neu entstehenden Infektionskrankheiten des Menschen von Tieren stammen -- COVID-19, HIV, Ebola, SARS und MERS sind alle zoonotischen Ursprungs."
    ),

    // Question 43
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einem endemischen, einem epidemischen und einem pandemischen Geschehen?",
        answerA = "Endemisch = besonders schwerer Verlauf, epidemisch = mittelschwer, pandemisch = milder Verlauf",
        answerB = "Endemisch = konstantesdas Vorkommen in einer Region, epidemisch = uebermaessiges Auftreten in einer Region, pandemisch = weltweite Ausbreitung",
        answerC = "Alle drei Begriffe beschreiben dasselbe mit unterschiedlicher Betonung auf die Erkrankungsschwere",
        answerD = "Endemisch gilt fuer Viren, epidemisch fuer Bakterien, pandemisch fuer Pilze",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Endemisch: Krankheit tritt in einer Region dauerhaft mit voraussehbarer Rate auf (Malaria in Subsahara-Afrika). Epidemisch: Erhoehtes Auftreten ueber dem Erwartungswert in einer Region. Pandemisch: Epidemie mit weltweiter geografischer Ausbreitung.",
        funFact = "Influenza ist weltweit endemisch, aber gleichzeitig saisonal epidemisch -- und alle paar Jahrzehnte pandemisch. COVID-19 begann als Epidemie in Wuhan, wurde zur Pandemie und wird moeglicherweise irgendwann endemisch sein."
    ),

    // Question 44
    Question(
        categoryId = 16,
        questionText = "Was ist die Funktion des Integrase-Inhibitors (z.B. Dolutegravir) in der HIV-Therapie?",
        answerA = "Er verhindert die Bindung von HIV an den CD4-Rezeptor",
        answerB = "Er hemmt das Enzym Integrase, das die virale DNA in das menschliche Chromosom einbaut",
        answerC = "Er blockiert die Protease, die Virusproteine prozessiert",
        answerD = "Er verhindert die Umschreibung von RNA in DNA durch Reverse Transkriptase",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Integraseinhibitoren (INSTI) wie Dolutegravir oder Bictegravir hemmen das HIV-Enzym Integrase, das fuer den Einbau der viralen cDNA in das Wirtsgenom notwendig ist -- ohne Integration kann keine neue Virusproduktion stattfinden.",
        funFact = "Dolutegravir ist inzwischen Bestandteil der meisten bevorzugten Erstlinientherapien weltweit -- es hat eine extrem hohe genetische Barriere gegenueber Resistenzen und wirkt in einer Einmaldosis taeglich."
    ),

    // Question 45
    Question(
        categoryId = 16,
        questionText = "Was ist das diagnostische Goldstandardverfahren fuer den Nachweis einer aktiven Tuberkulose?",
        answerA = "Tuberkulintest (Mendel-Mantoux)",
        answerB = "Interferon-Gamma-Release-Assay (IGRA/Quantiferon-Test)",
        answerC = "Kulturanzucht von Mycobacterium tuberculosis aus Sputum oder bronchoskopischem Material",
        answerD = "Roentgen-Thorax-Untersuchung",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Der kulturelle Erregernachweis ist der diagnostische Goldstandard fuer aktive TB -- er dauert jedoch 2-8 Wochen. PCR-Verfahren (Xpert MTB/RIF) ermoglichen schnelleren Nachweis und direkte Resistenztestung innerhalb weniger Stunden.",
        funFact = "Mycobacterium tuberculosis ist eines der langsamsten wachsenden Bakterien -- Generationszeit bis zu 20 Stunden (Vergleich: E. coli alle 20 Minuten). Das erklaert die langen Kultivierungszeiten und die monatelange Therapiedauer."
    ),

    // Question 46
    Question(
        categoryId = 16,
        questionText = "Welche Variante von SARS-CoV-2 wird als 'besorgniserregend' (Variant of Concern, VoC) eingestuft und warum?",
        answerA = "Varianten, die ausschliesslich in Tierreservoire gefunden werden",
        answerB = "Varianten mit Mutationen, die erhoehte Uebertragbarkeit, Krankheitsschwere oder Impfstoffumgehung nachweislich erhoehen",
        answerC = "Varianten, die nur bei immunsupprimierten Patienten auftreten",
        answerD = "Varianten mit veraenderter Farbe der Virushuelle im Elektronenmikroskop",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die WHO klassifiziert Varianten als VoC (Variant of Concern) wenn sie erhoehte Uebertragbarkeit, schwerere Erkrankung, veringerte Impfeffektivitaet oder Therapieversagen zeigen -- Alpha, Beta, Delta und Omikron waren alle VoCs.",
        funFact = "Die Omikronvariante hatte 32 Mutationen allein im Spike-Protein -- so viele, dass Wissenschaftler anfangs bezweifelten, ob sie wirklich aus der Delta-Linie abstammt. Moeglicherweise evolvierte sie in einem chronisch infizierten immunsupprimierten Patienten."
    ),

    // Question 47
    Question(
        categoryId = 16,
        questionText = "Was ist das Prinzip des attenuierten Lebendimpfstoffs und bei welchen Erregern wird er eingesetzt?",
        answerA = "Ein vollstaendig inaktivierter Erreger wird injiziert -- eingesetzt bei Hepatitis B und Influenza",
        answerB = "Ein abgeschwachter (virulenzreduzierter) Lebenderreger induziert starke Immunantwort -- eingesetzt bei Masern, Mumps, Roeteln (MMR), Gelbfieber und Varizellen",
        answerC = "Einzelne Proteine des Erregers werden als Antigen injiziert -- eingesetzt bei HPV und Meningokokken",
        answerD = "Genetische Information des Erregers wird injiziert -- eingesetzt bei COVID-19 und Influenza",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Attenuierte Lebendimpfstoffe enthalten abgeschwachte, aber noch lebensfaehige Erreger. Sie erzeugen starke und lang anhaltende Immunantworten, koennen aber bei Immunsupprimierten gefaehrlich sein -- MMR, Gelbfieber, OPV und Varizellen sind Beispiele.",
        funFact = "Der Gelbfieberimpfstoff 17D ist seit 1937 unveraendert in Gebrauch und gilt als einer der erfolgreichsten Impfstoffe aller Zeiten -- er bietet lebenslangen Schutz nach einer einzigen Impfung."
    ),

    // Question 48
    Question(
        categoryId = 16,
        questionText = "Welche Infektionskrankheit wird durch Aedes-aegypti-Muecken uebertragen und ist in tropischen Regionen verbreitet?",
        answerA = "Malaria (Plasmodium spp.)",
        answerB = "Denguefieber, Zikavirus, Gelbfieber und Chikungunya",
        answerC = "Schlafkrankheit (Trypanosoma brucei)",
        answerD = "West-Nil-Virus und Encephalitis durch Culex-Muecken",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Aedes aegypti ist der Hauptvektor fuer Dengue, Zika, Gelbfieber und Chikungunya -- diese Muecke erkennt man an ihren weissen Streifen, sie sticht tagsueber und brutet in stehenden Wasserbehaeltern.",
        funFact = "Denguefieber hat sich in den letzten 50 Jahren 30-fach ausgebreitet -- heute sind 3,9 Milliarden Menschen in ueber 128 Laendern gefaehrdet. Der Klimawandel bringt Aedes aegypti auch in suedeuropaeische Regionen wie Suedfrankreich und Norditalien."
    ),

    // Question 49
    Question(
        categoryId = 16,
        questionText = "Was ist das Prinzip des DOTS-Programms der WHO zur Tuberkulosebekaempfung?",
        answerA = "Directly Observed Treatment Short-course -- Patienten nehmen Medikamente unter direkter Beobachtung ein, um Abbrueche und Resistenzentwicklung zu verhindern",
        answerB = "Digitales Online-Tracking-System zur weltweiten TB-Fallerfassung",
        answerC = "Diagnostisches Schema fuer Schnelltestverfahren in Entwicklungslaendern",
        answerD = "Dezentrales Therapieprogramm ohne staatliche Ueberwachung",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "DOTS (Directly Observed Treatment Short-course) ist die WHO-Strategie zur TB-Bekaempfung: Patienten nehmen ihre Medikamente tagtaeglich unter Aufsicht eines Gesundheitsarbeiters ein -- das verhindert Therapieabbrueche und die Entstehung von Resistenzen.",
        funFact = "Ohne DOTS wuerden viele TB-Patienten ihre Behandlung abbrechen, sobald sie sich besser fuehlen -- nach nur 2-4 Wochen -- obwohl die volle 6-monatige Therapie zur Resistenzverhinderung unverzichtbar ist."
    ),

    // Question 50
    Question(
        categoryId = 16,
        questionText = "Was ist die Kaskadenregel der Antibiotikaresistenz und was bedeutet 'karbapenemresistente Enterobacteriaceae' (CRE)?",
        answerA = "CRE sind Darmbakterien, die Karbapenem-Antibiotika -- die letzte Reserveoption bei schweren gramnegativen Infektionen -- durch Karbapenemasen inaktivieren",
        answerB = "CRE sind Viren, die ausschliesslich in der Intensivmedizin auftreten und gegen alle bekannten antiviralen Mittel resistent sind",
        answerC = "CRE bezeichnet Pilzinfektionen, die gegen Echinocandine resistent sind",
        answerD = "CRE sind Mykobakterien mit erweitertem Resistenzspektrum gegen Isoniazid und Rifampicin",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "CRE (Karbapenem-resistente Enterobacteriaceae) wie KPC-produzierende Klebsiella pneumoniae oder NDM-Produzenten sind problematisch, weil Karbapenem die 'letzte Eskalationsstufe' bei gramnegativen Infektionen ist -- CRE laesst oft nur noch Colistin als Option.",
        funFact = "New Delhi Metallo-Beta-Laktamase (NDM-1) wurde 2008 erstmals bei einem Patienten beschrieben, der aus Indien zurueckkehrte -- seitdem hat sich dieses Resistenzgen weltweit verbreitet und wird als eine der groessten globalen Gesundheitsbedrohungen betrachtet."
    )

)
