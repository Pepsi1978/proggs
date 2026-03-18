package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMedium4(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 16,
        questionText = "Welcher Neurotransmitter ist bei Depressionen haeufig im Ungleichgewicht?",
        answerA = "Adrenalin",
        answerB = "Serotonin",
        answerC = "Insulin",
        answerD = "Cortisol",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Serotonin ist ein Botenstoff im Gehirn, dessen Mangel mit Depressionen in Verbindung gebracht wird. Viele Antidepressiva wirken, indem sie den Serotoninspiegel erhoehen.",
        funFact = "Etwa 90 Prozent des gesamten Serotonins im Koerper befinden sich nicht im Gehirn, sondern im Magen-Darm-Trakt."
    ),

    // Question 2
    Question(
        categoryId = 16,
        questionText = "Wie nennt man die haeufigste Form der Angststoerung?",
        answerA = "Panikstoerung",
        answerB = "Spezifische Phobie",
        answerC = "Generalisierte Angststoerung",
        answerD = "Soziale Phobie",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Die Generalisierte Angststoerung (GAS) ist gekennzeichnet durch anhaltende, schwer kontrollierbare Sorgen ueber viele verschiedene Lebensbereiche -- nicht nur eine spezifische Situation.",
        funFact = "Die Generalisierte Angststoerung betrifft weltweit etwa 3 bis 5 Prozent der Bevoelkerung und ist bei Frauen doppelt so haeufig wie bei Maennern."
    ),

    // Question 3
    Question(
        categoryId = 16,
        questionText = "Welche Abkuerzung steht fuer die Aufmerksamkeitsdefizit-Hyperaktivitaetsstoerung?",
        answerA = "ADHS",
        answerB = "PTBS",
        answerC = "OCD",
        answerD = "GAD",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "ADHS steht fuer Aufmerksamkeitsdefizit-Hyperaktivitaetsstoerung und ist eine neurobiologische Entwicklungsstoerung, die vor allem Konzentration, Impulsivitaet und Aktivitaetsniveau beeinflusst.",
        funFact = "ADHS wurde frueher oft als reines Kindheitsproblem betrachtet -- heute weiss man, dass etwa 60 Prozent der betroffenen Kinder die Stoerung ins Erwachsenenalter mitnehmen."
    ),

    // Question 4
    Question(
        categoryId = 16,
        questionText = "Was ist das Hauptmerkmal einer Panikattacke?",
        answerA = "Schleichend einsetzende Traurigkeit",
        answerB = "Ploetzliche intensive Angst mit koerperlichen Symptomen",
        answerC = "Chronische Muedigkeit ohne erklaerbare Ursache",
        answerD = "Gedaechtnisverlust fuer kurze Zeitraeume",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eine Panikattacke ist ein ploetzlicher Anfall intensiver Angst, der mit koerperlichen Symptomen wie Herzrasen, Schweissausbruechen, Zittern und Atemnot einhergeht und meist innerhalb von 10 Minuten seinen Hoehepunkt erreicht.",
        funFact = "Viele Menschen, die zum ersten Mal eine Panikattacke erleben, glauben, einen Herzinfarkt zu haben -- die Symptome koennen sich sehr aehnlich anfuehlen."
    ),

    // Question 5
    Question(
        categoryId = 16,
        questionText = "Welcher Neurotransmitter spielt bei ADHS eine zentrale Rolle?",
        answerA = "Serotonin",
        answerB = "GABA",
        answerC = "Dopamin",
        answerD = "Acetylcholin",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Bei ADHS ist die Dopaminregulation im praefrontalen Kortex des Gehirns gestoert. Medikamente wie Methylphenidat erhoehen die Verfuegbarkeit von Dopamin in diesem Bereich.",
        funFact = "Das bekannteste ADHS-Medikament Ritalin (Methylphenidat) wurde bereits 1944 synthetisiert -- seine Wirkung bei ADHS wurde aber erst Jahrzehnte spaeter entdeckt."
    ),

    // Question 6
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Insomnie?",
        answerA = "Uebertriebenes Schlafen tagsueber",
        answerB = "Schlafstoerung mit Ein- oder Durchschlafproblemen",
        answerC = "Ploetzliche Schlafanfaelle am Tag",
        answerD = "Schlafwandeln in der Nacht",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Insomnie bezeichnet eine Schlafstoerung, bei der Betroffene Schwierigkeiten haben einzuschlafen, durchzuschlafen oder zu frueh aufwachen -- und sich am naechsten Tag beeintraechtigt fuehlen.",
        funFact = "Chronische Insomnie erhoert das Risiko fuer Depressionen um das Fuenffache -- Schlaf und psychische Gesundheit sind eng miteinander verbunden."
    ),

    // Question 7
    Question(
        categoryId = 16,
        questionText = "Was ist das charakteristische Merkmal der Schlafapnoe?",
        answerA = "Zu fruehes Aufwachen am Morgen",
        answerB = "Atemaussetzer waehrend des Schlafs",
        answerC = "Lebhafte Alptraeume in der Nacht",
        answerD = "Schmerzen in den Beinen beim Einschlafen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei der Schlafapnoe kommt es zu wiederholten Atemaussetzern waehrend des Schlafs, was den Sauerstoffgehalt im Blut senkt und den Schlaf stark fragmentiert.",
        funFact = "Schnarchen ist das haeufigste Symptom der Schlafapnoe -- aber nicht jeder, der schnarcht, hat auch eine Schlafapnoe. Nur die Kombination mit Atemaussetzern ist typisch."
    ),

    // Question 8
    Question(
        categoryId = 16,
        questionText = "Was bedeutet die Abkuerzung PTBS?",
        answerA = "Psychische Therapie bei Burnout-Syndrom",
        answerB = "Posttraumatische Belastungsstoerung",
        answerC = "Panikattacken und Trennungsangst bei Stress",
        answerD = "Psychiatrische Teilbehandlung bei Sucht",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "PTBS steht fuer Posttraumatische Belastungsstoerung und entsteht nach dem Erleben extremer Traumata wie Unfaellen, Gewalt oder Katastrophen -- Betroffene erleben das Trauma immer wieder in Flashbacks.",
        funFact = "PTBS war frueher vor allem unter dem Begriff Kriegsneurose bekannt -- heute weiss man, dass viele verschiedene traumatische Erlebnisse die Stoerung ausloesen koennen."
    ),

    // Question 9
    Question(
        categoryId = 16,
        questionText = "Welche Therapieform steht hinter der Abkuerzung KVT?",
        answerA = "Koerperorientierte Verhaltenstherapie",
        answerB = "Kognitive Verhaltenstherapie",
        answerC = "Kreative Vitalitaetstherapie",
        answerD = "Kurzzeitige Verhaltenstherapie",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Kognitive Verhaltenstherapie (KVT) ist eine evidenzbasierte Psychotherapieform, die darauf abzielt, negative Denkmuster zu erkennen und durch gesundheitsfoerderliche Gedanken zu ersetzen.",
        funFact = "Die KVT ist eine der am besten erforschten Psychotherapieformen -- ueber 1.000 Studien belegen ihre Wirksamkeit bei Depressionen, Angststoerungen und vielen weiteren Erkrankungen."
    ),

    // Question 10
    Question(
        categoryId = 16,
        questionText = "Welche Medikamentengruppe wird am haeufigsten bei Depressionen eingesetzt?",
        answerA = "Benzodiazepine",
        answerB = "Neuroleptika",
        answerC = "Selektive Serotonin-Wiederaufnahmehemmer (SSRI)",
        answerD = "Opioide",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "SSRIs wie Fluoxetin oder Sertralin sind die Erstlinienmedikamente bei Depressionen. Sie verhindern die Wiederaufnahme von Serotonin in die Nervenzellen und erhoehen so dessen Wirkung.",
        funFact = "Fluoxetin (bekannt als Prozac) war der erste SSRI weltweit und wurde 1987 zugelassen -- er revolutionierte die Behandlung von Depressionen und Angststoerungen."
    ),

    // Question 11
    Question(
        categoryId = 16,
        questionText = "Was ist ein typisches Entzugssymptom bei Alkoholabhaengigkeit?",
        answerA = "Erhoehter Blutdruck und Krampfanfaelle",
        answerB = "Erhoehter Appetit und Muedigkeit",
        answerC = "Verminderte Herzfrequenz und Schlafrigkeit",
        answerD = "Verbessertes Gedaechtnis und Konzentration",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Alkoholentzug kann lebensbedrohlich sein und mit erhoehtem Blutdruck, Zittern, Schweissausbruechen und Krampfanfaellen einhergehen -- schwere Faelle koennen zum Delir fuehren.",
        funFact = "Im Gegensatz zur populaeren Meinung ist der Entzug von Alkohol medizinisch gefaehrlicher als der Entzug von Heroin -- ohne Behandlung kann er zum Tod fuehren."
    ),

    // Question 12
    Question(
        categoryId = 16,
        questionText = "Wie heisst das haufig verschriebene Medikament gegen Bluthochdruck, das auch Herzrasen lindert?",
        answerA = "Metformin",
        answerB = "Metoprolol",
        answerC = "Ibuprofen",
        answerD = "Lorazepam",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Metoprolol ist ein Betablocker, der den Herzschlag verlangsamt und den Blutdruck senkt -- er wird auch bei Angststoerungen eingesetzt, wenn koerperliche Symptome wie Herzrasen im Vordergrund stehen.",
        funFact = "Betablocker wie Metoprolol werden manchmal von Buehnensprechern oder Musikern vor Auftritten eingenommen, um das Zittern und Herzrasen durch Lampenfieber zu reduzieren."
    ),

    // Question 13
    Question(
        categoryId = 16,
        questionText = "Was ist das wichtigste fruehe Symptom der Alzheimer-Krankheit?",
        answerA = "Persoenlichkeitsveraenderungen und Aggressivitaet",
        answerB = "Gedaechtnisveerlust, besonders fuer kuerzliche Ereignisse",
        answerC = "Koerperliche Laehmungen und Sehstoerungen",
        answerD = "Unkontrollierbares Zittern der Haende",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Alzheimer beginnt typischerweise mit Vergesslichkeit fuer kuerzlich Erlebtes -- Langzeitgedaechtnis und Kindheitserinnerungen bleiben dagegen oft lange erhalten.",
        funFact = "Alzheimer wurde 1906 von dem deutschen Psychiater Alois Alzheimer beschrieben, nachdem er das Gehirn einer 51-jaehrigen Patientin untersucht hatte, die an seltsamen Verhaltensstoerungen gelitten hatte."
    ),

    // Question 14
    Question(
        categoryId = 16,
        questionText = "Welches Protein lagert sich beim Alzheimer im Gehirn ab und bildet sogenannte Plaques?",
        answerA = "Dopamin",
        answerB = "Amyloid-beta",
        answerC = "Haemoglobin",
        answerD = "Kollagen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei Alzheimer lagert sich Amyloid-beta-Protein zwischen den Gehirnzellen ab und bildet sogenannte senile Plaques, die die Kommunikation zwischen Neuronen stoeren.",
        funFact = "Obwohl Amyloid-Plaques seit Jahrzehnten als Ursache von Alzheimer gelten, zeigen neuere Studien, dass einige Menschen mit vielen Plaques kognitiv voellig gesund bleiben koennen."
    ),

    // Question 15
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Demenz und Alzheimer?",
        answerA = "Es gibt keinen -- beides sind Namen fuer dieselbe Krankheit",
        answerB = "Demenz ist ein Oberbegriff, Alzheimer ist eine spezifische Demenzform",
        answerC = "Alzheimer betrifft nur das Kurzzeitgedaechtnis, Demenz das Langzeitgedaechtnis",
        answerD = "Demenz ist heilbar, Alzheimer nicht",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Demenz ist ein Sammelbegriff fuer verschiedene Erkrankungen mit kognitivem Abbau -- Alzheimer ist die haeufigste Form und macht etwa 60 bis 70 Prozent aller Demenzen aus.",
        funFact = "Es gibt ueber 50 verschiedene Erkrankungen, die Demenz verursachen koennen -- darunter Gefaessdemenz, Lewy-Koerperchen-Demenz und frontotemporale Demenz."
    ),

    // Question 16
    Question(
        categoryId = 16,
        questionText = "Wie wirken Benzodiazepine im Gehirn?",
        answerA = "Sie erhoehen den Dopaminspiegel",
        answerB = "Sie verstaerken die Wirkung des hemmenden Botenstoffs GABA",
        answerC = "Sie blockieren Serotoninrezeptoren",
        answerD = "Sie stimulieren die Produktion von Endorphinen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Benzodiazepine wie Diazepam oder Lorazepam binden an GABA-Rezeptoren und verstaerken die hemmende Wirkung dieses Botenstoffs -- dadurch entstehen beruhigende, angstloesende und schlaffOrderende Effekte.",
        funFact = "Benzodiazepine wurden 1963 mit Diazepam (Valium) einfuehrt und galten als revolutionaere Beruhigungsmittel -- erst spaeter erkannte man ihr hohes Suchtpotenzial."
    ),

    // Question 17
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Nikotinabhaengigkeit als neurologischem Prozess?",
        answerA = "Nikotin schaedigt die Blut-Hirn-Schranke dauerhaft",
        answerB = "Nikotin aktiviert Dopaminausschuettung und erzeugt Belohnungsgefuehl",
        answerC = "Nikotin erhoht den Serotoninspiegel nachhaltig",
        answerD = "Nikotin hemmt die Cortisol-Ausschuettung in Stresssituationen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Nikotin bindet an Acetylcholinrezeptoren und loest eine erhoehte Dopaminausschuettung aus -- dieses Belohnungsgefuehl ist der Kern der Suchtentwicklung.",
        funFact = "Nikotin ist so schnell wirksam, dass es innerhalb von 7 bis 10 Sekunden nach dem Rauchen das Gehirn erreicht -- schneller als eine intravenoese Injektion."
    ),

    // Question 18
    Question(
        categoryId = 16,
        questionText = "Was ist das Restless-Legs-Syndrom?",
        answerA = "Eine Angststoerung mit Bewegungsdrang in sozialen Situationen",
        answerB = "Ein unangenehmes Kribbeln in den Beinen mit Bewegungsdrang, besonders nachts",
        answerC = "Krampfartige Schmerzen in den Beinen bei koerperlicher Anstrengung",
        answerD = "Eine Durchblutungsstoerung in den Unterschenkeln",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Restless-Legs-Syndrom ist eine neurologische Erkrankung mit einem quaelenden Drang, die Beine zu bewegen -- oft begleitet von Kribbeln oder Ziehen, das in Ruhe und besonders abends oder nachts auftritt.",
        funFact = "Das Restless-Legs-Syndrom betrifft 5 bis 10 Prozent der Bevoelkerung und ist einer der haeufigsten Gruende fuer Schlaflosigkeit -- viele Betroffene wissen jahrelang nicht, dass sie eine behandelbare Erkrankung haben."
    ),

    // Question 19
    Question(
        categoryId = 16,
        questionText = "Was ist ein typisches Symptom einer schweren depressiven Episode?",
        answerA = "Gesteigerte Energie und vermindertes Schlafbeduerfnis",
        answerB = "Anhedonie -- der Verlust der Freude an frueheren Aktivitaeten",
        answerC = "Gesteigerte Risikobereitschaft und impulsives Verhalten",
        answerD = "Gedankenflucht und uebersteigertes Selbstwertgefuehl",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Anhedonie -- die Unfaehigkeit, Freude oder Vergnuegen zu empfinden -- gilt als eines der Kernsymptome der Depression und unterscheidet sie von normaler Traurigkeit.",
        funFact = "Das Wort Anhedonie stammt aus dem Griechischen: 'an' bedeutet 'ohne' und 'hedone' bedeutet 'Freude' -- es beschreibt also woertlich den Zustand ohne Freude."
    ),

    // Question 20
    Question(
        categoryId = 16,
        questionText = "Welche Psychotherapieform wurde von Sigmund Freud begruendet?",
        answerA = "Verhaltenstherapie",
        answerB = "Psychoanalyse",
        answerC = "Gestalttherapie",
        answerD = "Systemische Therapie",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Sigmund Freud entwickelte die Psychoanalyse, die davon ausgeht, dass unbewusste Konflikte und verdraengte Erlebnisse psychische Stoerungen verursachen koennen.",
        funFact = "Freud begann seine Karriere als Neurologe und behandelte zunaechst Patienten mit Hysterie -- erst spaeter entwickelte er seine Theorie des Unbewussten und der Psychoanalyse."
    ),

    // Question 21
    Question(
        categoryId = 16,
        questionText = "Was beschreibt der Begriff 'Komorbiditaet' in der Psychiatrie?",
        answerA = "Die Sterblichkeitsrate bei psychiatrischen Erkrankungen",
        answerB = "Das gleichzeitige Vorliegen von zwei oder mehr Erkrankungen",
        answerC = "Die Uebertragung psychiatrischer Erkrankungen auf andere Personen",
        answerD = "Die Nebenwirkungen psychiatrischer Medikamente",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Komorbiditaet bedeutet, dass ein Patient gleichzeitig an mehreren Erkrankungen leidet -- zum Beispiel haben viele Menschen mit Depressionen gleichzeitig auch eine Angststoerung.",
        funFact = "Komorbiditaet ist in der Psychiatrie eher die Regel als die Ausnahme -- ueber 50 Prozent der Menschen mit einer psychiatrischen Diagnose erfullen auch die Kriterien fuer eine weitere Stoerung."
    ),

    // Question 22
    Question(
        categoryId = 16,
        questionText = "Welches Medikament wird haeufig als Blutverduenner eingesetzt, um Schlaganfaelle zu verhindern?",
        answerA = "Ibuprofen",
        answerB = "Paracetamol",
        answerC = "Marcumar (Phenprocoumon)",
        answerD = "Amoxicillin",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Phenprocoumon (Marcumar) ist ein Vitamin-K-Antagonist, der die Blutgerinnung hemmt und das Risiko von Thrombosen und Schlaganfaellen bei Risikogruppen deutlich reduziert.",
        funFact = "Phenprocoumon wurde urspruenglich als Rattengift entwickelt -- in kleinen Dosen wirkt es als lebenswichtige Medizin, in grossen Mengen toedlich."
    ),

    // Question 23
    Question(
        categoryId = 16,
        questionText = "Was passiert bei einer Wechselwirkung zwischen Alkohol und Schlafmitteln?",
        answerA = "Die Wirkung des Alkohols wird abgschwaecht",
        answerB = "Es kommt zu einer gefaehrlichen Wirkungsverstaerkung mit Atemdepression",
        answerC = "Das Schlafmittel verliert seine Wirkung voellig",
        answerD = "Die Leberwerte normalisieren sich schneller",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Alkohol und Schlafmittel (z.B. Benzodiazepine) verstaerken sich gegenseitig in ihrer zentral-daempfenden Wirkung -- die Kombination kann zu lebensgefaehrlicher Atemdepression fuehren.",
        funFact = "Viele Medikamenten-Todesfaelle sind nicht auf eine einzige Substanz zurueckzufuehren, sondern auf die Kombination mehrerer Substanzen -- besonders Alkohol mit Schlaf- oder Schmerzmitteln."
    ),

    // Question 24
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Toleranzentwicklung bei Suchtmitteln?",
        answerA = "Die Faehigkeit des Koerpers, Suchtmittel schneller abzubauen",
        answerB = "Die Notwendigkeit, immer hoehere Dosen zu nehmen, um den gleichen Effekt zu erzielen",
        answerC = "Die psychologische Akzeptanz der eigenen Abhaengigkeit",
        answerD = "Die Verbesserung der Lebensqualitaet trotz Suchtmittelkonsum",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Toleranzentwicklung bedeutet, dass der Koerper sich an ein Suchtmittel anpasst und immer groessere Mengen benoetigt, um denselben Effekt zu erreichen -- ein zentrales Merkmal von Abhaengigkeit.",
        funFact = "Toleranz ist nicht nur ein psychologisches Phaenomen -- der Koerper baut tatsaechlich mehr Enzyme zur Verstoffwechslung auf und die Rezeptoren veraendern ihre Empfindlichkeit."
    ),

    // Question 25
    Question(
        categoryId = 16,
        questionText = "Welches ist das haeufigste Schlafstadium, in dem Traeumeauftreten?",
        answerA = "Tiefschlaf (N3)",
        answerB = "Leichtschlaf (N1)",
        answerC = "REM-Schlaf",
        answerD = "Einschlafphase",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Der REM-Schlaf (Rapid Eye Movement) ist das Schlafstadium mit intensiven Traeumen. Dabei sind die Augen schnell bewegend unter den Lidern, und die Hirnaktivitaet aehnelt dem Wachzustand.",
        funFact = "Waehrend des REM-Schlafs ist der Koerper fast voellig gelahmt -- eine Schutzfunktion, damit wir unsere Traeume nicht tatsaechlich ausleben und uns verletzen."
    ),

    // Question 26
    Question(
        categoryId = 16,
        questionText = "Welche Substanz im Gehirn reguliert den Schlaf-Wach-Rhythmus und wird als 'Schlafhormon' bezeichnet?",
        answerA = "Cortisol",
        answerB = "Melatonin",
        answerC = "Adrenalin",
        answerD = "Testosteron",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Melatonin wird von der Zirbeldruese produziert und steuert den zirkadianen Rhythmus -- bei Dunkelheit steigt der Spiegel an und macht muede, bei Licht sinkt er wieder.",
        funFact = "Melatonin kann als Nahrungsergaenzungsmittel eingenommen werden -- besonders bei Jetlag hat es eine wissenschaftlich belegte Wirkung beim Anpassen des Schlafrhythmus."
    ),

    // Question 27
    Question(
        categoryId = 16,
        questionText = "Was sind die Drei-Kernsymptome von ADHS laut DSM?",
        answerA = "Depressivitaet, Angst und Schlaflosigkeit",
        answerB = "Aufmerksamkeitsdefizit, Hyperaktivitaet und Impulsivitaet",
        answerC = "Gedaechtnisschwaece, Sprachprobleme und motorische Unruhe",
        answerD = "Soziale Isolation, Aggressivitaet und Leistungsversagen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das DSM (Diagnostisches und Statistisches Manual) definiert ADHS durch die drei Kernsymptome Unaufmerksamkeit, Hyperaktivitaet und Impulsivitaet, die in mehreren Lebensbereichen auftreten muessen.",
        funFact = "Es gibt drei Subtypen von ADHS: den vorwiegend unaufmerksamen Typ (fruehers ADS), den vorwiegend hyperaktiven Typ und den Mischtyp -- der Mischtyp ist am haeufigsten."
    ),

    // Question 28
    Question(
        categoryId = 16,
        questionText = "Was beschreibt ein 'Flashback' bei PTBS?",
        answerA = "Das Erinnern an positive Erlebnisse aus der Vergangenheit",
        answerB = "Das lebhafte Wiedererleben traumatischer Ereignisse als wuerden sie gerade passieren",
        answerC = "Eine Gedaechtnisstoerung, bei der Ereignisse vergessen werden",
        answerD = "Das Verleugnen eines traumatischen Ereignisses",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Flashbacks bei PTBS sind intensive, ungewollte Wiedererlebnisse des Traumas -- Betroffene fuehlen, riechen und sehen das Ereignis oft so lebendig, als wuerde es gerade stattfinden.",
        funFact = "Bestimmte Sinneswahrnehmungen -- ein Geruch, ein Klang oder eine bestimmte Lichtsituation -- koennen als 'Trigger' Flashbacks ausloesen, weil das Gehirn sie mit dem Trauma verbunden hat."
    ),

    // Question 29
    Question(
        categoryId = 16,
        questionText = "Warum darf man beim Nehmen von MAO-Hemmern kein Kaeseessen?",
        answerA = "Kaese verschlechtert die Aufnahme des Medikaments im Darm",
        answerB = "Tyramin in Kaese kann zu lebensgefahrlichen Blutdruckkrisen fuehren",
        answerC = "Das Fett im Kaese beschleunigt den Abbau des Medikaments",
        answerD = "Kaese erhoeht den Serotoninspiegel unkontrolliert",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "MAO-Hemmer blockieren das Enzym, das Tyramin abbaut. Tyramin in Lebensmitteln wie Kaese kann sich anhaeufen und gefaehrliche Blutdruckkrisen ausloesen.",
        funFact = "Diese Wechselwirkung wird als 'Kaese-Reaktion' bezeichnet und kann zu hypertensiven Krisen mit Blutdruckwerten ueber 200 mmHg fuehren -- potentiell toedlich ohne Behandlung."
    ),

    // Question 30
    Question(
        categoryId = 16,
        questionText = "Was ist das Prinzip der Expositionstherapie bei Angststoerungen?",
        answerA = "Das Vermeide der angstausloesenden Situationen zur Stressreduktion",
        answerB = "Das schrittweise, kontrollierte Konfrontieren mit dem Angstausloeser",
        answerC = "Das Erinnern an positive Erlebnisse waehrend der Angstsituation",
        answerD = "Das Einnehmen von Beruhigungsmitteln vor Angstsituationen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei der Expositionstherapie werden Patienten schrittweise und kontrolliert mit dem angstausloesenden Reiz konfrontiert -- durch wiederholte Exposition lernt das Gehirn, dass die Situation nicht wirklich gefaehrlich ist.",
        funFact = "Studien zeigen, dass virtuelle Realitaet (VR) zunehmend fuer Expositionstherapie eingesetzt wird -- Patienten mit Hoehenangst koennen so virtuell den Eiffelturm besteigen."
    ),

    // Question 31
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Begriff 'bipolarer Stoerung'?",
        answerA = "Eine Persoenlichkeitsstoerung mit zwei verschiedenen Persoenlichkeiten",
        answerB = "Wechselnde Episoden von Depression und Manie (uebertriebene Hochstimmung)",
        answerC = "Eine Angststoerung, die sich auf zwei verschiedene Situationen bezieht",
        answerD = "Gleichzeitiges Auftreten von Schizophrenie und Depression",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Bipolare Stoerung ist gekennzeichnet durch Episoden tiefer Depression, die sich mit manischen Phasen abwechseln -- in der Manie fuehlen sich Betroffene euphorisch, benoetigen wenig Schlaf und handeln impulsiv.",
        funFact = "Viele bekannte kreative Menschen wie Vincent van Gogh, Ernest Hemingway und Kurt Cobain sollen an einer bipolaren Stoerung gelitten haben -- ob Kreativitaet und Bipolaritaet zusammenhaengen, ist wissenschaftlich diskutiert."
    ),

    // Question 32
    Question(
        categoryId = 16,
        questionText = "Was ist Methadon und wozu wird es eingesetzt?",
        answerA = "Ein Schlafmittel zur Behandlung von Insomnie",
        answerB = "Ein Substitutionsmittel zur Behandlung von Opioidabhaengigkeit",
        answerC = "Ein Antidepressivum fuer therapieresistente Depressionen",
        answerD = "Ein Schmerzmittel bei leichten bis mittelschweren Schmerzen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Methadon ist ein synthetisches Opioid, das als Substitutionsmittel bei Heroinabhaengigkeit eingesetzt wird -- es stillt das Verlangen, ohne den typischen Rausch, und ermoeglicht ein stabiles Leben.",
        funFact = "Methadon wurde im Zweiten Weltkrieg in Deutschland entwickelt, als Morphin knapp wurde -- der Name 'Dolophine' wurde von einigen faelschlicherweise auf Hitler zurueckgefuehrt, aber diese Geschichte ist ein Mythos."
    ),

    // Question 33
    Question(
        categoryId = 16,
        questionText = "Was ist das Serotonin-Syndrom?",
        answerA = "Ein Mangel an Serotonin, der zu Depressionen fuehrt",
        answerB = "Eine gefaehrliche Ueberdosierung mit zu viel Serotonin im Nervensystem",
        answerC = "Eine Allergie gegen serotonerge Medikamente",
        answerD = "Ein seltenes genetisches Syndrom mit Serotonin-Produktion im Darm",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Serotonin-Syndrom ist ein medizinischer Notfall durch exzessiv erhohte Serotoninaktivitaet -- Symptome sind Zittern, Verwirrung, erhoehte Herzfrequenz und im schlimmsten Fall Krampfanfaelle.",
        funFact = "Das Serotonin-Syndrom kann entstehen, wenn mehrere Medikamente kombiniert werden, die alle den Serotoninspiegel erhoehen -- zum Beispiel SSRI mit Tramadol oder bestimmten Migraine-Mitteln."
    ),

    // Question 34
    Question(
        categoryId = 16,
        questionText = "Was ist die sogenannte kognitive Verzerrung 'Katastrophisieren'?",
        answerA = "Das Unterschaetzen von Risiken in gefaehrlichen Situationen",
        answerB = "Das Ueberbewerten negativer Ereignisse und Annehmen schlimmster Szenarien",
        answerC = "Das Verleugnen realer Probleme als unbedeutend",
        answerD = "Das unrealistische Optimismus gegenueber der Zukunft",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Katastrophisieren ist ein in der KVT bekanntes Denkmuster, bei dem normale Probleme als katastrophal bewertet werden -- z.B. denkt man bei Kopfschmerzen sofort an einen Tumor.",
        funFact = "Katastrophisieren ist bei chronischen Schmerzpatienten besonders verbreitet und verstaerkt nachweislich die Schmerzwahrnehmung -- die Behandlung der Denkverzerrung kann Schmerzen tatsaechlich lindern."
    ),

    // Question 35
    Question(
        categoryId = 16,
        questionText = "Welche Schlafphase ist am wichtigsten fuer koerperliche Erholung und Zellregeneration?",
        answerA = "REM-Schlaf",
        answerB = "Tiefschlaf (N3 / Slow-Wave-Sleep)",
        answerC = "Leichtschlaf (N1 und N2)",
        answerD = "Die Einschlafphase",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Im Tiefschlaf (N3) werden Wachstumshormone ausgeschuettet, das Immunsystem staerkt sich, und Gewebereparatur findet statt -- er ist entscheidend fuer koerperliche Erholung.",
        funFact = "Kinder und Jugendliche verbringen deutlich mehr Zeit im Tiefschlaf als Erwachsene -- mit zunehmendem Alter nimmt die Tiefschlafphase ab, was ein Grund fuer schlechtere Erholung im Alter ist."
    ),

    // Question 36
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter 'Psychoedukation' in der psychiatrischen Behandlung?",
        answerA = "Eine Form der Hypnose zur Traumabehandlung",
        answerB = "Das Informieren von Patienten ueber ihre Erkrankung und Behandlungsmoeglichkeiten",
        answerC = "Ein Intensivprogramm fuer psychisch kranke Kinder",
        answerD = "Das Erstellen eines Schulungskonzepts fuer psychiatrisches Pflegepersonal",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Psychoedukation bedeutet, dass Patienten strukturiert ueber ihre Diagnose, Symptome und Behandlungsmoeglichkeiten informiert werden -- Wissen ueber die eigene Krankheit verbessert nachweislich die Behandlungsergebnisse.",
        funFact = "Studien zeigen, dass Psychoedukation bei bipolarer Stoerung die Rueckfallrate um bis zu 50 Prozent reduzieren kann -- Patienten, die ihre Erkrankung verstehen, gehen besser damit um."
    ),

    // Question 37
    Question(
        categoryId = 16,
        questionText = "Was ist ein haeufiger Grund fuer Schlafprobleme bei aelteren Menschen?",
        answerA = "Erhoehte Melatoninproduktion im Alter",
        answerB = "Veraenderungen im zirkadianen Rhythmus und Abnahme des Tiefschlafs",
        answerC = "Uebererregung durch verlaengertes Kurzzeitgedaechtnis",
        answerD = "Zu viel koerperliche Aktivitaet im Alter",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Im Alter verschiebt sich der zirkadiane Rhythmus nach vorne (Fruehtyp), der Tiefschlaf nimmt ab und die Schlafarchitektur veraendert sich -- das fuehrt haeufig zu fruehzeitigem Aufwachen und schlechterer Schlafqualitaet.",
        funFact = "Die Idee, dass aeltere Menschen weniger Schlaf brauchen, ist ein Mythos -- ihr Schlafbeduerfnis bleibt gleich, aber ihre Faehigkeit, tief und erholsam zu schlafen, nimmt ab."
    ),

    // Question 38
    Question(
        categoryId = 16,
        questionText = "Welche Wirkstoffgruppe sind trizyklische Antidepressiva (TCA)?",
        answerA = "Die neueste Generation der Antidepressiva mit wenigen Nebenwirkungen",
        answerB = "Eine aeltere Antidepressiva-Gruppe, die mehrere Neurotransmitter beeinflusst",
        answerC = "Speziell fuer Angststoerungen entwickelte Medikamente",
        answerD = "Natuerliche Praeparate auf Pflanzenbasis wie Johanniskraut",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Trizyklische Antidepressiva wie Amitriptylin sind aeltere Medikamente, die Serotonin- und Noradrenalin-Wiederaufnahme hemmen -- sie sind wirksam, aber haben mehr Nebenwirkungen als neuere SSRIs.",
        funFact = "Trizyklische Antidepressiva werden heute oft in niedrigen Dosen gegen chronische Schmerzen und Schlafprobleme eingesetzt -- nicht als Antidepressivum, sondern wegen spezifischer Nebenwirkungen."
    ),

    // Question 39
    Question(
        categoryId = 16,
        questionText = "Was ist das Prinzip des CAGE-Fragebogens?",
        answerA = "Ein Test zur Diagnose von ADHS bei Erwachsenen",
        answerB = "Ein Screening-Instrument zur Erkennung von Alkoholproblemen",
        answerC = "Ein Bewertungsschema fuer depressive Episoden",
        answerD = "Ein Fragebogen zur Einschaetzung von Schlafqualitaet",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "CAGE steht fuer Cut, Annoyed, Guilty, Eye-opener -- vier Fragen, die schnell Hinweise auf problematischen Alkoholkonsum geben. Zwei oder mehr positive Antworten gelten als auffaellig.",
        funFact = "Der CAGE-Fragebogen wurde 1974 entwickelt und ist mit nur 4 Fragen eines der kuerzesten und zugleich validesten Screening-Instrumente in der gesamten Medizin."
    ),

    // Question 40
    Question(
        categoryId = 16,
        questionText = "Was ist die Hauptwirkung von Ibuprofen im Koerper?",
        answerA = "Hemmung der Prostaglandin-Synthese durch COX-Blockade",
        answerB = "Erhoehung des Cortisolspiegels zur Entzuendungshemmung",
        answerC = "Blockade von Schmerzrezeptoren im Rueckenmark",
        answerD = "Foerderung der Endorphin-Ausschuettung im Gehirn",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Ibuprofen hemmt das Enzym Cyclooxygenase (COX), das fuer die Bildung von Prostaglandinen verantwortlich ist -- Prostaglandine verursachen Schmerz, Fieber und Entzuendung.",
        funFact = "Ibuprofen wurde urspruenglich als Behandlung fuer rheumatoide Arthritis entwickelt -- heute ist es eines der am meisten konsumierten Schmerzmittel weltweit mit ueber 100 Millionen Anwendern taegach."
    ),

    // Question 41
    Question(
        categoryId = 16,
        questionText = "Was bedeutet 'Dissoziation' als psychologisches Phaenomen?",
        answerA = "Die bewusste Entscheidung, eine Beziehung zu beenden",
        answerB = "Ein Zustand, in dem sich Gedanken, Gefuehle oder Wahrnehmungen vom Bewusstsein abkoppeln",
        answerC = "Die Unfaehigkeit, neue Erinnerungen zu bilden",
        answerD = "Das Vergessen von Ereignissen durch Schlafmangel",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Dissoziation ist ein Schutzmechanismus der Psyche, bei dem Bewusstsein, Erinnerung oder Identitaet sich zeitweise trennen -- leichte Dissoziation kennt jeder (Tagtraeumen), schwere Formen treten bei Trauma auf.",
        funFact = "Dissoziation waehrend eines Traumas -- das Gefuehl, von oben auf sich selbst zu schauen -- ist evolutionaer sinnvoll, da es die psychische Belastung in extremen Situationen reduziert."
    ),

    // Question 42
    Question(
        categoryId = 16,
        questionText = "Welche Substanz ist der aktive Wirkstoff in Paracetamol?",
        answerA = "Acetylsalicylsaeure",
        answerB = "Acetaminophen (Paracetamol selbst)",
        answerC = "Naproxen",
        answerD = "Diclofenac",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Paracetamol ist sowohl der Handelsname als auch der chemische Name des Wirkstoffs Acetaminophen -- er wirkt schmerzlindernd und fiebersenkend, hat aber keine relevante entzuendungshemmende Wirkung.",
        funFact = "Paracetamol ist das Schmerzmittel mit der schmalsten Sicherheitsmarge -- die Differenz zwischen einer wirksamen und einer leberschaedigenden Dosis ist erschreckend gering."
    ),

    // Question 43
    Question(
        categoryId = 16,
        questionText = "Was beschreibt das Konzept der 'erlernten Hilflosigkeit' (nach Seligman)?",
        answerA = "Der Zustand, in dem man aufhoert, nach Loesungen zu suchen, weil man gelernt hat, dass nichts funktioniert",
        answerB = "Das Annehmen von Hilfe durch andere Menschen als Zeichen von Staerke",
        answerC = "Die Abhaengigkeit von Pflegepersonal bei chronischen Erkrankungen",
        answerD = "Das Erlernen von Hilfeleistungen als Berufsausbildung",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Martin Seligman entdeckte, dass Lebewesen, die wiederholt unkontrollierbaren negativen Ereignissen ausgesetzt sind, aufhoeren, nach Auswegen zu suchen -- auch wenn diese spaeter verfuegbar waeren.",
        funFact = "Seligmans Experimente mit Hunden fuehrten zur Theorie der erlernten Hilflosigkeit als Modell fuer Depression -- dasselbe Prinzip wurde spaeter bei Menschen in unkontrollierbaren Lebensumstaenden beobachtet."
    ),

    // Question 44
    Question(
        categoryId = 16,
        questionText = "Was ist ein 'triggernder' Faktor bei Suchterkrankungen?",
        answerA = "Eine medizinische Nebenwirkung des Suchtmittels auf Blutdruck",
        answerB = "Ein Reiz oder eine Situation, die Craving (Suchtdrang) ausloest",
        answerC = "Ein Medikament, das die Sucht beendet",
        answerD = "Der genetische Faktor fuer Suchtanfaelligkeit",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Trigger sind konditionierte Reize -- Personen, Orte, Emotionen oder Situationen --, die mit dem Suchtmittelkonsum assoziiert sind und starkes Verlangen (Craving) ausloesen koennen.",
        funFact = "Das Gehirn kann Trigger aus der Suchtzeit jahrzehntelang abspeichern -- selbst nach Jahren der Abstinenz kann ein bestimmter Geruch oder Ort Craving ausloesen, was Rueckfaelle erklaert."
    ),

    // Question 45
    Question(
        categoryId = 16,
        questionText = "Was ist Lithium in der Psychiatrie?",
        answerA = "Ein Antidepressivum der zweiten Generation",
        answerB = "Ein Stimmungsstabilisator fuer bipolare Stoerungen",
        answerC = "Ein Schlafmittel fuer schwere Insomnien",
        answerD = "Ein Angstloser der ersten Wahl bei Panikattacken",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Lithium ist ein Stimmungsstabilisator, der seit den 1950er Jahren zur Behandlung bipolarer Stoerungen eingesetzt wird -- es reduziert manische Episoden und kann depressive Rueckfaelle verhindern.",
        funFact = "Lithium ist ein chemisches Element (Li, Ordnungszahl 3) -- es ist buchstaeblich ein Mineralstein, das als Medikament wirkt. Warum genau es bei bipolarer Stoerung hilft, ist bis heute nicht vollstaendig verstanden."
    ),

    // Question 46
    Question(
        categoryId = 16,
        questionText = "Was ist das 'Burnout-Syndrom' und wie unterscheidet es sich von Depression?",
        answerA = "Burnout ist medizinisch identisch mit Depression, nur der Name unterscheidet sich",
        answerB = "Burnout entsteht spezifisch durch Arbeitsbelastung, Depression kann viele Ursachen haben",
        answerC = "Burnout ist schwerer heilbar als Depression und wird nicht therapiert",
        answerD = "Burnout betrifft nur das Privatleben, Depression nur den Beruf",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Burnout ist ein Erschoepfungszustand, der aus chronischem Arbeitsstress entsteht -- Depression ist eine klinische Erkrankung mit vielen Ursachen. Sie koennen sich ueberlappen, sind aber konzeptuell verschieden.",
        funFact = "Burnout ist keine offizielle psychiatrische Diagnose im ICD-11 als eigenstaendige Erkrankung -- es wird als Faktor eingestuft, der die Gesundheit beeinflusst, aber nicht als Krankheit selbst."
    ),

    // Question 47
    Question(
        categoryId = 16,
        questionText = "Welche Erkrankung ist durch Plaques aus Beta-Amyloid UND Neurofibrillen aus Tau-Protein gekennzeichnet?",
        answerA = "Parkinson",
        answerB = "Alzheimer",
        answerC = "Multiple Sklerose",
        answerD = "Huntington",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Alzheimer ist pathologisch durch zwei Kennzeichen charakterisiert: extrazellulaere Plaques aus Amyloid-beta und intrazellulaere Neurofibrillenbuendel aus dem Tau-Protein.",
        funFact = "Neue Bluttests koennen Tau-Protein und Amyloid-beta messen und Alzheimer damit bis zu 20 Jahre vor dem Auftreten erster Symptome erkennen -- die Forschung zur Fruehdiagnose schreitet rapide voran."
    ),

    // Question 48
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter 'Abhaengigkeitspotenzial' eines Medikaments?",
        answerA = "Die Wahrscheinlichkeit, dass das Medikament allergische Reaktionen ausloest",
        answerB = "Das Mass, in dem ein Medikament koerperliche oder psychische Abhaengigkeit verursachen kann",
        answerC = "Die Dauer, die das Medikament im Koerper verbleibt",
        answerD = "Die Faehigkeit eines Medikaments, andere Medikamente zu verstaerken",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Abhaengigkeitspotenzial beschreibt, wie stark ein Medikament Abhaengigkeit (koerperlich und/oder psychisch) erzeugen kann -- Benzodiazepine haben z.B. ein hohes, Paracetamol ein sehr niedriges Potenzial.",
        funFact = "Nicht verschreibungspflichtige Medikamente koennen trotzdem Abhaengigkeiten erzeugen -- zum Beispiel Nasensprays mit Oxymetazolin, von denen viele Menschen abhaengig werden, ohne es als Sucht wahrzunehmen."
    ),

    // Question 49
    Question(
        categoryId = 16,
        questionText = "Was ist das Ziel der Dialektisch-Behavioralen Therapie (DBT)?",
        answerA = "Die Verarbeitung von Kindheitstraumata durch freies Assoziieren",
        answerB = "Emotionsregulation und Bewaltigung bei Borderline-Persoenlichkeitsstoerung",
        answerC = "Die Analyse von Traeumen und unbewussten Konflikten",
        answerD = "Die Desensibilisierung gegenueber spezifischen Phobien",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "DBT wurde speziell fuer die Borderline-Persoenlichkeitsstoerung entwickelt und kombiniert Akzeptanz- mit Veraenderungsstrategien -- Kernziel ist die Verbesserung der Emotionsregulation und Beziehungsgestaltung.",
        funFact = "DBT wurde von Marsha Linehan entwickelt -- sie offenbarte spaeter, dass sie selbst in jungen Jahren an Borderline gelitten hatte und die Therapie auf Basis ihrer eigenen Erfahrungen konzipiert hatte."
    ),

    // Question 50
    Question(
        categoryId = 16,
        questionText = "Was ist das 'Sucht-Gedaechtnis' und warum ist es therapierelevant?",
        answerA = "Ein Test zur Messung der Intelligenz bei Suchtpatienten",
        answerB = "Tiefe neuronale Verbindungen, die Suchtverhalten langfristig im Gehirn abspeichern",
        answerC = "Das Erinnerungsvermoegen fuer Ereignisse unter Alkoholeinfluss",
        answerD = "Ein Modul in der kognitiven Verhaltenstherapie fuer Suchtpatienten",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Sucht-Gedaechtnis beschreibt stabile neuronale Verschaltungen, die durch Suchtmittelkonsum entstehen und Craving und Rueckfaelle auch nach Jahren der Abstinenz ausloesen koennen -- es kann nicht geloescht, nur ueberlagert werden.",
        funFact = "Das Sucht-Gedaechtnis ist so tief im Gehirn verankert, dass es selbst nach Jahrzehnten der Abstinenz reaktiviert werden kann -- das erklaert, warum Sucht als chronische Erkrankung und nicht als Willensschwaeche verstanden werden sollte."
    )

)
