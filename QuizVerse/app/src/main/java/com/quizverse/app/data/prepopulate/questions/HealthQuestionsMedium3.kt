package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMedium3(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 16,
        questionText = "Welche der folgenden Naehrstoffe zaehlen zu den Makronaehrstoffen?",
        answerA = "Vitamine, Mineralstoffe und Spurenelemente",
        answerB = "Kohlenhydrate, Proteine und Fette",
        answerC = "Antioxidantien, Ballaststoffe und Wasser",
        answerD = "Omega-3-Fettsaeuren, Aminosaeuren und Zucker",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Makronaehrstoffe sind die drei Hauptnaehrstoffgruppen, die der Koerper in grossen Mengen benoetigt: Kohlenhydrate, Proteine (Eiweiss) und Fette. Sie liefern Energie und Baustoffe fuer den Koerper.",
        funFact = "Der Name 'Makronaehrstoff' kommt vom griechischen Wort 'makros' (gross) -- im Gegensatz zu Mikronaehrstoffen wie Vitaminen, die nur in winzigen Mengen benoetigt werden."
    ),

    // Question 2
    Question(
        categoryId = 16,
        questionText = "Wie viel Energie liefert ein Gramm Fett?",
        answerA = "4 Kilokalorien",
        answerB = "7 Kilokalorien",
        answerC = "9 Kilokalorien",
        answerD = "12 Kilokalorien",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Ein Gramm Fett liefert 9 Kilokalorien -- mehr als doppelt so viel wie Kohlenhydrate oder Proteine, die jeweils 4 Kilokalorien pro Gramm liefern.",
        funFact = "Weil Fett so energiedicht ist, speichert der Koerper ueberschuessige Energie bevorzugt als Koerperfett. Ein Kilogramm Koerperfett entspricht etwa 7.000 Kilokalorien gespeicherter Energie."
    ),

    // Question 3
    Question(
        categoryId = 16,
        questionText = "Was ist Vitamin B12 und worin ist es hauptsaechlich enthalten?",
        answerA = "Ein fettloesliches Vitamin, hauptsaechlich in Pflanzenoel",
        answerB = "Ein wasserloesliches Vitamin, hauptsaechlich in tierischen Lebensmitteln",
        answerC = "Ein Mineral, hauptsaechlich in Vollkornprodukten",
        answerD = "Ein Antioxidans, hauptsaechlich in Beeren",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vitamin B12 ist ein wasserloesliches Vitamin, das fast ausschliesslich in tierischen Lebensmitteln wie Fleisch, Fisch, Eiern und Milchprodukten vorkommt. Es ist wichtig fuer Nerven und Blutbildung.",
        funFact = "Veganer haben ein hohes Risiko fuer Vitamin-B12-Mangel, weil pflanzliche Lebensmittel kaum B12 enthalten. Ein Mangel kann zu schweren neurologischen Schaeden fuehren, die sich schleichend ueber Jahre entwickeln."
    ),

    // Question 4
    Question(
        categoryId = 16,
        questionText = "Wie wird der Body-Mass-Index (BMI) berechnet?",
        answerA = "Gewicht in kg geteilt durch Groesse in cm",
        answerB = "Gewicht in kg geteilt durch Groesse in Meter zum Quadrat",
        answerC = "Groesse in cm minus 100",
        answerD = "Taillenumfang in cm geteilt durch Hueftumfang in cm",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der BMI berechnet sich: Koerpergewicht (kg) geteilt durch Koerpergroesse (m) zum Quadrat. Ein BMI von 18,5 bis 24,9 gilt als Normalgewicht.",
        funFact = "Der BMI wurde um 1830 vom belgischen Mathematiker Adolphe Quetelet entwickelt -- urspruenglich nicht als Gesundheitsmass, sondern fuer statistische Zwecke. Als medizinisches Werkzeug hat er deutliche Grenzen, weil er Muskel- und Fettmasse nicht unterscheidet."
    ),

    // Question 5
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen HDL- und LDL-Cholesterin?",
        answerA = "HDL ist 'schlechtes' Cholesterin, LDL ist 'gutes' Cholesterin",
        answerB = "HDL ist 'gutes' Cholesterin, das Arterien schuetzt; LDL ist 'schlechtes', das Arterien verstopfen kann",
        answerC = "Beide sind gleich schlecht fuer das Herz",
        answerD = "LDL kommt nur aus tierischen Lebensmitteln, HDL wird vom Koerper produziert",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "HDL (High-Density-Lipoprotein) transportiert Cholesterin zur Leber, wo es abgebaut wird -- es schuetzt Arterien. LDL (Low-Density-Lipoprotein) lagert Cholesterin in Arterienwaenden ab und kann zu Arteriosklerose fuehren.",
        funFact = "Die Merkregel: HDL = 'Healthy' (gut), LDL = 'Lousy' (schlecht). Sport erhoht den HDL-Wert nachweislich -- ein weiterer Grund, regelmaessig koerperlich aktiv zu sein."
    ),

    // Question 6
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Grundumsatz (Basalstoffwechsel)?",
        answerA = "Die Energie, die beim Sport verbraucht wird",
        answerB = "Die Energie, die der Koerper in Ruhe fuer lebensnotwendige Funktionen benoetigt",
        answerC = "Die maximale Energiemenge, die ein Mensch pro Tag essen kann",
        answerD = "Die Energie, die bei der Verdauung freigesetzt wird",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der Grundumsatz ist die Energiemenge, die der Koerper in voelliger Ruhe und nuechtern benoetigt, um lebensnotwendige Funktionen wie Herzschlag, Atmung und Koerperwaerme aufrechtzuerhalten.",
        funFact = "Das Gehirn allein verbraucht etwa 20 Prozent des gesamten Grundumsatzes -- obwohl es nur 2 Prozent des Koerpergewichts ausmacht. Denken ist also tatsaechlich anstrengend fuer den Koerper."
    ),

    // Question 7
    Question(
        categoryId = 16,
        questionText = "Welche Funktion haben Omega-3-Fettsaeuren im Koerper?",
        answerA = "Sie erhoehen den Cholesterinspiegel",
        answerB = "Sie haben keine nachgewiesene Wirkung",
        answerC = "Sie wirken entzuendungshemmend und schuetzen das Herz-Kreislauf-System",
        answerD = "Sie ersetzen Proteine als Baustoffe",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Omega-3-Fettsaeuren sind essenzielle Fettsaeuren, die entzuendungshemmend wirken, den Triglyceridspiegel senken und das Herz-Kreislauf-System schuetzen. Der Koerper kann sie nicht selbst herstellen.",
        funFact = "Die beste pflanzliche Quelle fuer Omega-3 ist Leinoel -- es enthaelt mehr Omega-3 als Fischoel. Allerdings muss der Koerper pflanzliches Omega-3 (ALA) erst in die wirksameren Formen EPA und DHA umwandeln."
    ),

    // Question 8
    Question(
        categoryId = 16,
        questionText = "Was sind Antioxidantien und welche Rolle spielen sie im Koerper?",
        answerA = "Stoffe, die den Blutdruck senken",
        answerB = "Stoffe, die freie Radikale neutralisieren und Zellschaeden verhindern",
        answerC = "Stoffe, die den Blutzucker regulieren",
        answerD = "Stoffe, die Fett im Blut abbauen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Antioxidantien sind Verbindungen, die freie Radikale -- aggressive Sauerstoffverbindungen -- neutralisieren, bevor sie Zellen und DNA schadigen koennen. Sie schuetzen so vor Alterung und chronischen Krankheiten.",
        funFact = "Gruener Tee enthaelt besonders viele Antioxidantien (Catechine). Paradoxerweise ist auch dunkle Schokolade mit einem Kakaogehalt ueber 70 Prozent eine gute Antioxidantienquelle."
    ),

    // Question 9
    Question(
        categoryId = 16,
        questionText = "Welche Vitamine gehoeren zu den fettloeslichen Vitaminen?",
        answerA = "Vitamin C und alle B-Vitamine",
        answerB = "Vitamin A, D, E und K",
        answerC = "Vitamin B12 und Folsaeure",
        answerD = "Vitamin C, E und Folsaeure",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die fettloeslichen Vitamine sind A, D, E und K -- sie werden mit Nahrungsfetten aufgenommen und im Koerper (vor allem in der Leber und im Fettgewebe) gespeichert. Im Gegensatz zu wasserloeslichen Vitaminen kann ein Ueberschuss toxisch werden.",
        funFact = "Merkregel fuer fettloesliche Vitamine: 'ADEK' -- wie ein Verein. Weil sie gespeichert werden koennen, kann eine Ueberdosierung gefaehrlich sein -- vor allem Vitamin A und D koennen in grossen Mengen toxisch sein."
    ),

    // Question 10
    Question(
        categoryId = 16,
        questionText = "Was ist das Darmmikrobiom?",
        answerA = "Ein Organ, das Enzyme produziert",
        answerB = "Die Gesamtheit aller Mikroorganismen im Darm",
        answerC = "Eine Schicht der Darmwand",
        answerD = "Ein Hormon, das die Verdauung steuert",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Darmmikrobiom ist die Gemeinschaft von Billionen von Bakterien, Pilzen, Viren und anderen Mikroorganismen, die im Darm leben. Es beeinflusst Verdauung, Immunsystem und sogar die psychische Gesundheit.",
        funFact = "Im menschlichen Darm leben etwa 38 Billionen Bakterien -- mehr als Zellen im gesamten Koerper. Ihr Gesamtgewicht betraegt etwa 0,2 Kilogramm -- so schwer wie ein kleiner Apfel."
    ),

    // Question 11
    Question(
        categoryId = 16,
        questionText = "Was sind Probiotika?",
        answerA = "Nahrungsergaenzungsmittel, die das Muskelwachstum foerdern",
        answerB = "Lebende Mikroorganismen, die die Darmgesundheit foerdern",
        answerC = "Ballaststoffe, die das Wachstum von Darmbakterien foerdern",
        answerD = "Enzyme, die die Fettverdauung verbessern",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Probiotika sind lebende Mikroorganismen (meist Bakterien oder Hefen), die -- in ausreichender Menge aufgenommen -- gesundheitliche Vorteile fuer den Wirt haben, besonders fuer das Darmmikrobiom.",
        funFact = "Joghurt, Kefir, Sauerkraut, Kimchi und Miso sind natuerliche Probiotika-Quellen. Das Wort 'Probiotika' bedeutet auf Griechisch 'fuer das Leben' -- im Gegensatz zu Antibiotika ('gegen das Leben')."
    ),

    // Question 12
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat Vitamin D im Koerper?",
        answerA = "Es foerdert die Eisenaufnahme im Darm",
        answerB = "Es reguliert die Kalziumaufnahme und ist wichtig fuer Knochen und Immunsystem",
        answerC = "Es schuetzt vor Sonnenbrand",
        answerD = "Es hilft bei der Blutgerinnung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vitamin D reguliert die Aufnahme von Kalzium und Phosphor im Darm und ist entscheidend fuer starke Knochen und Zaehne. Es unterstuetzt auch das Immunsystem und spielt eine Rolle bei der Zellentwicklung.",
        funFact = "Vitamin D ist das einzige Vitamin, das der Koerper selbst herstellen kann -- durch Sonnenlicht auf der Haut. In Deutschland haben jedoch viele Menschen im Winter einen Vitamin-D-Mangel, weil die Sonne nicht stark genug ist."
    ),

    // Question 13
    Question(
        categoryId = 16,
        questionText = "Was sind saettigte und ungesaettigte Fettsaeuren?",
        answerA = "Gesaettigte Fettsaeuren sind fluessig, ungesaettigte sind fest bei Raumtemperatur",
        answerB = "Gesaettigte Fettsaeuren sind fest (meist tierisch), ungesaettigte sind fluessig (meist pflanzlich) und herzgesunder",
        answerC = "Es gibt keinen gesundheitlichen Unterschied",
        answerD = "Ungesaettigte Fettsaeuren erhoehen das Cholesterin, gesaettigte senken es",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Gesaettigte Fettsaeuren kommen vor allem in tierischen Produkten vor, sind bei Raumtemperatur fest und koennen bei uebermassigem Konsum das LDL-Cholesterin erhoehen. Ungesaettigte Fettsaeuren (z.B. in Olivenoel) gelten als herzgesunder.",
        funFact = "Kokosoel enthaelt trotz seines exotischen Images fast 90 Prozent gesaettigte Fettsaeuren -- mehr als Butter. Der Gesundheitshype darum ist wissenschaftlich nicht gut belegt."
    ),

    // Question 14
    Question(
        categoryId = 16,
        questionText = "Was ist Anorexia nervosa?",
        answerA = "Eine Stoerung, bei der Betroffene unkontrolliert essen und erbrechen",
        answerB = "Eine Essstoerung mit starkem Gewichtsverlust durch Nahrungsverweigerung und verzerrte Koerperwahrnehmung",
        answerC = "Eine Form von Lebensmittelallergie",
        answerD = "Eine Stoerung des Fettstoffwechsels",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Anorexia nervosa (Maagersucht) ist eine schwere psychische Essstoerung, bei der Betroffene wenig essen, starken Gewichtsverlust erleiden und trotz gefaehrlichem Untergewicht eine verzerrte Koerperwahrnehmung haben.",
        funFact = "Anorexia nervosa hat unter allen psychischen Erkrankungen eine der hoechsten Sterblichkeitsraten. Die Erkrankung betrifft hauptsaechlich junge Frauen, aber auch Maenner koennen betroffen sein."
    ),

    // Question 15
    Question(
        categoryId = 16,
        questionText = "Was ist Bulimia nervosa?",
        answerA = "Extreme Nahrungsverweigerung mit Gewichtsverlust",
        answerB = "Eine Essstoerung mit Essanfaellen, gefolgt von Gegenmassnahmen wie Erbrechen",
        answerC = "Starkes Uebergewicht durch genetische Ursachen",
        answerD = "Eine Nahrungsmittelintoleranz",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bulimia nervosa ist eine Essstoerung, bei der Betroffene wiederholte Essanfaelle haben und danach versuchen, die Folgen zu kompensieren -- oft durch selbst herbeigefoertes Erbrechen, Abfuehrmittel oder exzessiven Sport.",
        funFact = "Im Gegensatz zur Magersucht haben Menschen mit Bulimie oft Normalgewicht -- was die Diagnose erschwert. Durch wiederholtes Erbrechen kann die Magensaeure die Zaehne stark schaedigen."
    ),

    // Question 16
    Question(
        categoryId = 16,
        questionText = "Welche Lebensmittel sind besonders reich an Eisen?",
        answerA = "Milch, Joghurt und Kaese",
        answerB = "Rotes Fleisch, Huelsenfruechte und gruenes Blattgemuese",
        answerC = "Weissbrot, Nudeln und Reis",
        answerD = "Bananen, Aepfel und Birnen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Gute Eisenquellen sind rotes Fleisch (haem-Eisen, sehr gut verwertbar), Huelsenfruechte wie Linsen und Bohnen sowie gruenes Blattgemuese wie Spinat (nicht-haem-Eisen, weniger gut verwertbar).",
        funFact = "Vitamin C verbessert die Aufnahme von pflanzlichem Eisen erheblich -- deshalb ist es gut, Spinat mit etwas Zitronensaft zu essen. Kaffee und Tee hingegen hemmen die Eisenaufnahme."
    ),

    // Question 17
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einfachen und komplexen Kohlenhydraten?",
        answerA = "Einfache Kohlenhydrate kommen nur in Obst vor, komplexe nur in Gemuese",
        answerB = "Einfache Kohlenhydrate werden schnell verstoffwechselt (Zucker), komplexe langsamer (Staerke, Ballaststoffe)",
        answerC = "Komplexe Kohlenhydrate enthalten mehr Kalorien",
        answerD = "Es gibt keinen nennenswerten Unterschied",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Einfache Kohlenhydrate (Zucker) lassen den Blutzucker schnell ansteigen und fallen. Komplexe Kohlenhydrate (Staerke in Vollkorn, Huelsenfruechten) werden langsamer abgebaut und halten laenger satt.",
        funFact = "Der glykamische Index misst, wie schnell ein Lebensmittel den Blutzucker anhebt. Weissbrot hat einen hoeheren glykamischen Index als Vollkornbrot -- obwohl beides Brot ist."
    ),

    // Question 18
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat Magnesium im Koerper?",
        answerA = "Es transportiert Sauerstoff im Blut",
        answerB = "Es ist an ueber 300 enzymatischen Reaktionen beteiligt, u.a. Muskel- und Nervenfunktion",
        answerC = "Es staerkt ausschliesslich die Knochen",
        answerD = "Es reguliert den Blutzuckerspiegel",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Magnesium ist an ueber 300 biochemischen Reaktionen im Koerper beteiligt. Es ist wichtig fuer Muskelfunktion (verhindert Krampfe), Nervenfunktion, Energiestoffwechsel und Knochengesundheit.",
        funFact = "Muskelkraempfe (besonders Wadenkraempfe) sind ein klassisches Zeichen fuer Magnesiummangel. Gute Magnesiumquellen sind Nuesse, Vollkornprodukte, Huelsenfruechte und Bananen."
    ),

    // Question 19
    Question(
        categoryId = 16,
        questionText = "Was ist Kalzium und wie viel benoetigt ein Erwachsener taeglich?",
        answerA = "Ein Vitamin mit einer Tagesdosis von 10 Mikrogramm",
        answerB = "Ein Mineral, wichtig fuer Knochen und Zaehne, Tagesdosis etwa 1000 mg",
        answerC = "Ein Protein, das im Muskelgewebe vorkommt",
        answerD = "Eine Fettsaeure mit einer Tagesdosis von 2 Gramm",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Kalzium ist das mengenmassig haeufigste Mineral im menschlichen Koerper. Es ist entscheidend fuer Knochen und Zaehne sowie fuer Muskelkontraktion und Nervenleitung. Erwachsene benoetigen etwa 1000 mg pro Tag.",
        funFact = "99 Prozent des Kalziums im Koerper stecken in Knochen und Zaehnen. Wenn die Ernaehrung zu wenig Kalzium liefert, 'klaut' sich der Koerper Kalzium aus den Knochen -- was langfristig zu Osteoporose fuehrt."
    ),

    // Question 20
    Question(
        categoryId = 16,
        questionText = "Was bedeutet Laktoseintoleranz?",
        answerA = "Eine Allergie gegen Milchprotein",
        answerB = "Die Unvertraeglichkeit von Milchzucker (Laktose) durch Mangel an Laktase",
        answerC = "Eine Stoerung der Kalziumaufnahme",
        answerD = "Eine genetische Unfaehigkeit, Milch zu verdauen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Laktoseintoleranz entsteht, wenn der Koerper zu wenig Laktase produziert -- das Enzym, das Milchzucker (Laktose) spaltet. Unverdaute Laktose wird von Darmbakterien vergoren, was Blaehnungen, Bauchschmerzen und Durchfall verursacht.",
        funFact = "Weltweit sind etwa 65 bis 70 Prozent der Erwachsenen laktoseintolerant. In Nordeuropa haben sich Menschen durch jahrtausende lange Milchwirtschaft eine Toleranz entwickelt -- hier sind nur etwa 15 Prozent betroffen."
    ),

    // Question 21
    Question(
        categoryId = 16,
        questionText = "Was sind Trans-Fettsaeuren und warum sind sie schaedlich?",
        answerA = "Natuerliche Fettsaeuren aus Olivenoel, die herzgesund sind",
        answerB = "Kuenstlich gehaertete Fette, die LDL erhoehen, HDL senken und das Herzrisiko steigern",
        answerC = "Fettsaeuren, die nur in Fisch vorkommen und Entzuendungen hemmen",
        answerD = "Fette, die bei der Verdauung entstehen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Trans-Fettsaeuren entstehen bei der industriellen Haertung von Pflanzenoelen. Sie sind besonders schaedlich, weil sie gleichzeitig das 'schlechte' LDL-Cholesterin erhoehen und das 'gute' HDL senken.",
        funFact = "Viele Laender haben Trans-Fette inzwischen verboten oder stark eingeschraenkt. Daenemark war das erste Land weltweit, das 2003 einen gesetzlichen Grenzwert fuer Trans-Fettsaeuren in Lebensmitteln einfuehrte."
    ),

    // Question 22
    Question(
        categoryId = 16,
        questionText = "Welche Nahrungsmittel sind besonders reich an Omega-3-Fettsaeuren?",
        answerA = "Rindfleisch, Butter und Milch",
        answerB = "Fetter Fisch (Lachs, Makrele, Hering), Leinoel und Walnuesse",
        answerC = "Weissbrot, Kartoffeln und Reis",
        answerD = "Bananen, Orangen und Trauben",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Fetter Seefisch wie Lachs, Makrele, Hering und Sardinen sind die reichhaltigsten Quellen fuer die Omega-3-Fettsaeuren EPA und DHA. Leinoel und Walnuesse enthalten die pflanzliche Vorstufe ALA.",
        funFact = "Der Fisch stellt selbst kein Omega-3 her -- er nimmt es durch das Fressen von Algen und Zooplankton auf. Deshalb enthalten Zuchtlachse, die mit Getreidefutter aufgezogen werden, deutlich weniger Omega-3 als Wildlachse."
    ),

    // Question 23
    Question(
        categoryId = 16,
        questionText = "Was ist der glykamische Index (GI)?",
        answerA = "Ein Mass fuer den Fettgehalt eines Lebensmittels",
        answerB = "Ein Mass dafuer, wie schnell ein Lebensmittel den Blutzucker anhebt",
        answerC = "Ein Mass fuer den Proteingehalt eines Lebensmittels",
        answerD = "Ein Qualitaetsmass fuer biologisch angebaute Lebensmittel",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der glykamische Index gibt an, wie schnell und stark ein kohlenhydrathaltiges Lebensmittel den Blutzucker ansteigen laesst -- gemessen im Vergleich zu reiner Glukose (GI = 100).",
        funFact = "Interessant: Kartoffeln haben einen hoeheren GI als Schokolade. Wenn Kartoffeln jedoch abgekuehlt und dann wieder erwaermt werden, sinkt ihr GI -- denn beim Abkuehlen bildet sich resistente Staerke."
    ),

    // Question 24
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat Zink im menschlichen Koerper?",
        answerA = "Zink transportiert Sauerstoff im Blut",
        answerB = "Zink unterstuetzt das Immunsystem, Wundheilung und DNA-Synthese",
        answerC = "Zink ist hauptsaechlich fuer die Verdauung von Fetten zustaendig",
        answerD = "Zink reguliert den Blutdruck",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Zink ist ein essenzielles Spurenelement, das das Immunsystem staerkt, die Wundheilung foerdert, an der DNA-Synthese beteiligt ist und fuer Wachstum und Entwicklung benoetigt wird.",
        funFact = "Austern enthalten mehr Zink als jedes andere Lebensmittel -- eine einzige Auster kann den Tagesbedarf decken. Deshalb gelten Austern seit der Antike als Aphrodisiakum, was moeglicherweise auf ihren Einfluss auf Testosteron durch Zink zurueckzufuehren ist."
    ),

    // Question 25
    Question(
        categoryId = 16,
        questionText = "Was sind Ballaststoffe und wozu sind sie gut?",
        answerA = "Unverdauliche Pflanzenfasern, die die Verdauung foerdern und die Darmgesundheit unterstuetzen",
        answerB = "Nahrungsergaenzungsmittel fuer den Muskelaufbau",
        answerC = "Eine Form von Protein, die in Getreide vorkommt",
        answerD = "Verdauliche Kohlenhydrate mit hohem Naehrwert",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Ballaststoffe sind unverdauliche Pflanzenfasern, die den Stuhlgang regulieren, Darmbakterien ernaehren, den Blutzuckeranstieg verlangsamen und das Saettigungsgefuehl erhoehen.",
        funFact = "Die Deutsche Gesellschaft fuer Ernaehrung empfiehlt mindestens 30 Gramm Ballaststoffe pro Tag -- die meisten Deutschen erreichen nur etwa 18 bis 20 Gramm. Vollkornprodukte, Huelsenfruechte und Gemuese sind die besten Quellen."
    ),

    // Question 26
    Question(
        categoryId = 16,
        questionText = "Wie wird Lebensmittel durch Pasteurisierung haltbar gemacht?",
        answerA = "Durch Einfrieren auf unter minus 18 Grad",
        answerB = "Durch kurzes Erhitzen auf 60 bis 90 Grad, um Krankheitserreger abzutoeten",
        answerC = "Durch Zugabe von kuenstlichen Konservierungsstoffen",
        answerD = "Durch Entzug von Luft im Vakuumverfahren",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei der Pasteurisierung werden Lebensmittel (z.B. Milch, Saft) kurzzeitig auf 60 bis 90 Grad erhitzt, um krankheitserregende Keime abzutoeten, ohne den Geschmack oder Naehrwert stark zu beeintraechtigssen.",
        funFact = "Das Verfahren wurde 1864 vom franzoesischen Wissenschaftler Louis Pasteur entwickelt -- daher der Name. Er bewies als erster, dass Mikroorganismen Lebensmittel verderben lassen."
    ),

    // Question 27
    Question(
        categoryId = 16,
        questionText = "Was gibt der Kalorienbedarf eines Menschen an?",
        answerA = "Nur die beim Sport verbrannte Energie",
        answerB = "Die Gesamtmenge an Energie, die der Koerper taeglich benoetigt (Grundumsatz plus Aktivitaet)",
        answerC = "Die maximale Menge an Nahrung, die man essen sollte",
        answerD = "Die Anzahl der Mahlzeiten pro Tag",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der Gesamtkalorienbedarf setzt sich aus dem Grundumsatz (Energie fuer Koerperfunktionen in Ruhe) und dem Leistungsumsatz (Energie fuer Aktivitaeten) zusammen.",
        funFact = "Ein durchschnittlicher Erwachsener hat einen Gesamtkalorienbedarf von etwa 2000 bis 2500 kcal pro Tag. Der genaue Wert haengt von Alter, Geschlecht, Groesse, Gewicht und Aktivitaetslevel ab."
    ),

    // Question 28
    Question(
        categoryId = 16,
        questionText = "Welche Lebensmittel enthalten besonders viele Antioxidantien?",
        answerA = "Weissbrot, Kartoffeln und weisser Reis",
        answerB = "Beeren (Heidelbeeren, Aronia), gruener Tee, Kurkuma und dunkle Schokolade",
        answerC = "Milch, Butter und Sahne",
        answerD = "Wurstwaren und verarbeitetes Fleisch",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Besonders antioxidantienreich sind Beeren (vor allem Heidelbeeren und Aroniabeeren), gruener Tee, Kurkuma, Rotwein in Massen und dunkle Schokolade mit hohem Kakaoanteil.",
        funFact = "Aroniabeeren (Schwarze Apfelbeere) haben eine der hoechsten Antioxidantienkonzentrationen aller bekannten Fruechte -- sie werden deshalb manchmal als 'Superbeere' bezeichnet."
    ),

    // Question 29
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Begriff 'Praeabiotika'?",
        answerA = "Antibiotika fuer den Pflanzenanbau",
        answerB = "Unverdauliche Nahrungsbestandteile, die das Wachstum nuetzlicher Darmbakterien foerdern",
        answerC = "Vorstufen von Probiotika, die erst im Darm aktiv werden",
        answerD = "Eine Art Nahrungsergaenzungsmittel fuer den Sport",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Praebiotika (auch: Prebiotika) sind unverdauliche Ballaststoffe, die als 'Futter' fuer nuetzliche Darmbakterien dienen und so ein gesundes Mikrobiom foerdern. Inulin in Chichoree und Zwiebeln ist ein bekanntes Beispiel.",
        funFact = "Der Unterschied: Probiotika sind die lebenden Bakterien selbst, Praebiotika sind ihre Nahrung. 'Synbiotika' kombinieren beide -- zum Beispiel Joghurt (Probiotika) mit Banane (Praebiotika)."
    ),

    // Question 30
    Question(
        categoryId = 16,
        questionText = "Was ist Vitamin K und welche Funktion hat es?",
        answerA = "Ein wasserloesliches Vitamin, das die Eisenaufnahme foerdert",
        answerB = "Ein fettloesliches Vitamin, das fuer die Blutgerinnung und Knochengesundheit wichtig ist",
        answerC = "Ein Vitamin, das ausschliesslich in tierischen Produkten vorkommt",
        answerD = "Ein Vitamin, das den Energiestoffwechsel reguliert",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vitamin K ist ein fettloesliches Vitamin, das eine zentrale Rolle bei der Blutgerinnung spielt (Vitamin K1) und die Einlagerung von Kalzium in Knochen reguliert (Vitamin K2).",
        funFact = "Das K in Vitamin K steht fuer das daenische/deutsche Wort 'Koagulation' (Blutgerinnung). Bestimmte Blutverduenner wie Marcumar hemmen genau die Wirkung von Vitamin K -- Patienten muessen deshalb auf grossen Mengen Gruenkohl achten."
    ),

    // Question 31
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Koerperfett und Koerpermasse?",
        answerA = "Es gibt keinen Unterschied -- beides bezeichnet dasselbe",
        answerB = "Koerperfett ist nur der Fettanteil, Koerpermasse umfasst Muskeln, Knochen, Organe und Wasser",
        answerC = "Koerpermasse ist immer hoeher als Koerperfett",
        answerD = "Koerperfett wird durch den BMI gemessen, Koerpermasse durch Bluttests",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Koerperzusammensetzung unterscheidet zwischen Fettmasse (Koerperfettanteil) und fettfreier Masse (Muskeln, Knochen, Organe, Wasser). Der BMI unterscheidet diese Anteile nicht.",
        funFact = "Ein Kraftsportler mit viel Muskelmasse kann laut BMI 'uebergewichtig' sein, obwohl er sehr wenig Koerperfett hat. Der Koerperfettanteil ist daher ein genaueres Mass fuer die Gesundheit als der BMI allein."
    ),

    // Question 32
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat Vitamin E im Koerper?",
        answerA = "Es reguliert den Blutzucker",
        answerB = "Es wirkt als fettloesliches Antioxidans und schuetzt Zellmembranen vor Oxidation",
        answerC = "Es foerdert die Kalziumaufnahme",
        answerD = "Es ist wichtig fuer die Produktion von roten Blutkoerperchen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vitamin E ist ein fettloesliches Antioxidans, das Zellmembranen vor Schaeden durch freie Radikale schuetzt. Es unterstuetzt auch das Immunsystem und wirkt entzuendungshemmend.",
        funFact = "Vitamin E loeste in den 1980ern einen grossen Hype aus -- man hoffte, es koenne Krebs und Herzerkrankungen verhindern. Spaetere Studien zeigten, dass hochdosierte Ergaenzungspraeparate sogar schaedlich sein koennen."
    ),

    // Question 33
    Question(
        categoryId = 16,
        questionText = "Was sind essentielle Aminosaeuren?",
        answerA = "Aminosaeuren, die der Koerper selbst herstellen kann",
        answerB = "Aminosaeuren, die der Koerper nicht selbst herstellen kann und ueber die Ernaehrung aufnehmen muss",
        answerC = "Aminosaeuren, die nur in tierischen Proteinen vorkommen",
        answerD = "Die Aminosaeuren, die fuer den Muskelaufbau unwichtig sind",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Von den 20 Aminosaeuren, aus denen Koerperproteine aufgebaut sind, gelten 9 als essenziell -- der Koerper kann sie nicht selbst synthetisieren und muss sie ueber die Nahrung aufnehmen.",
        funFact = "Pflanzliche Proteine enthalten oft nicht alle 9 essenziellen Aminosaeuren in ausreichender Menge -- man nennt das 'limitierende Aminosaeuren'. Durch clevere Kombination (z.B. Reis und Bohnen) kann man alle essentiellen Aminosaeuren abdecken."
    ),

    // Question 34
    Question(
        categoryId = 16,
        questionText = "Was ist Fermentation in der Lebensmittelverarbeitung?",
        answerA = "Ein Erhitzungsverfahren zur Keimreduzierung",
        answerB = "Ein Konservierungsverfahren durch Einfrieren",
        answerC = "Ein biochemischer Prozess, bei dem Mikroorganismen Zucker in Saeure, Gas oder Alkohol umwandeln",
        answerD = "Ein Verfahren zur Entfernung von Konservierungsstoffen",
        correctAnswer = 2,
        difficulty = 2,
        explanation = "Fermentation ist ein uraltes Konservierungsverfahren, bei dem Bakterien, Hefen oder Pilze Zucker abbauen und dabei Milchsaeure, Essigsaeure, Alkohol oder CO2 produzieren -- das macht Lebensmittel haltbar und verdaulicher.",
        funFact = "Fermentierte Lebensmittel gibt es in fast jeder Kultur: Sauerkraut (Deutschland), Kimchi (Korea), Kefir (Kaukasus), Miso (Japan), Tempeh (Indonesien). Alle basieren auf demselben Prinzip."
    ),

    // Question 35
    Question(
        categoryId = 16,
        questionText = "Was ist die Aufgabe von Enzymen im Verdauungssystem?",
        answerA = "Enzyme transportieren Naehrstoffe durch die Darmwand",
        answerB = "Enzyme zerlegen grosse Naehrstoffmolekuele in kleinere, aufnehmbare Bestandteile",
        answerC = "Enzyme produzieren Energie fuer den Darm",
        answerD = "Enzyme toeten Krankheitserreger im Darm ab",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Verdauungsenzyme zerkleinern Naehrstoffe biochemisch: Amylase baut Staerke ab, Lipase spaltet Fette, Proteasen zerlegen Proteine in Aminosaeuren -- erst dann koennen sie durch die Darmwand aufgenommen werden.",
        funFact = "Ananas und Papaya enthalten natuerliche Proteasen (Bromelain und Papain), die Fleisch zartoermachen koennen. Deshalb kann man keine frische Ananas in Gelatine geben -- das Bromelain loest die Gelatine auf."
    ),

    // Question 36
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Lebensmittelvergiftung?",
        answerA = "Eine allergische Reaktion auf bestimmte Lebensmittel",
        answerB = "Erkrankung durch Gifte oder Krankheitserreger in Lebensmitteln",
        answerC = "Unvertraeglichkeit gegen Laktose oder Gluten",
        answerD = "Schaden durch zu viel Zucker oder Salz",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eine Lebensmittelvergiftung entsteht durch Bakterien (z.B. Salmonellen, Staphylokokken), deren Gifte (Toxine) oder Schimmelpilzgifte in Lebensmitteln. Symptome sind Uebelkeit, Erbrechen, Durchfall und Bauchschmerzen.",
        funFact = "Einer der haufigsten Ausloeser von Lebensmittelvergiftungen ist das Bakterium Campylobacter aus rohem oder ungenuegend gegartem Haehnchenfleiisch. Allein in Deutschland gibt es jaehrlich etwa 100.000 gemeldete Campylobacter-Erkrankungen."
    ),

    // Question 37
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat die Leber beim Fettstoffwechsel?",
        answerA = "Die Leber speichert alle aufgenommenen Fette als Koerperfett",
        answerB = "Die Leber produziert Galle, baut Cholesterin ab und kann Fett in Energie umwandeln",
        answerC = "Die Leber hat keine Funktion beim Fettstoffwechsel",
        answerD = "Die Leber blockiert die Aufnahme von Fett aus dem Darm",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Leber spielt eine zentrale Rolle im Fettstoffwechsel: Sie produziert Galle fuer die Fettverdauung, synthetisiert und baut Cholesterin ab, stellt Lipoproteine her und kann Fett als Energiequelle nutzen.",
        funFact = "Zu viel Fruchtzucker (Fructose) -- z.B. aus zuckerhaltigen Getraenken -- wird in der Leber direkt in Fett umgewandelt. Das ist ein Hauptgrund fuer nicht-alkoholische Fettleber, die haeufigste Lebererkrankung in Deutschland."
    ),

    // Question 38
    Question(
        categoryId = 16,
        questionText = "Was ist Folsaeure und warum ist sie besonders fuer Schwangere wichtig?",
        answerA = "Ein Mineral, das die Knochenentwicklung des Foetus foerdert",
        answerB = "Ein B-Vitamin, das fuer Zellteilung wichtig ist und Neuralrohrdefekte beim Foetus verhindert",
        answerC = "Ein Hormon, das die Schwangerschaft aufrechterhaelt",
        answerD = "Eine Fettsaeure, die die Gehirnentwicklung des Kindes foerdert",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Folsaeure (Vitamin B9) ist essenziell fuer Zellteilung und DNA-Synthese. In der Fruehschwangerschaft verhindert ausreichend Folsaeure Neuralrohrdefekte wie Spina bifida beim ungeborenen Kind.",
        funFact = "Frauen, die schwanger werden moechten, sollten bereits vor der Empfaengnis Folsaeure erganzen -- denn in den ersten Wochen der Schwangerschaft, bevor viele Frauen wissen dass sie schwanger sind, wird das Neuralrohr des Kindes gebildet."
    ),

    // Question 39
    Question(
        categoryId = 16,
        questionText = "Wie beeinflusst das Darmmikrobiom das Immunsystem?",
        answerA = "Es hat keinen Einfluss auf das Immunsystem",
        answerB = "Etwa 70 Prozent der Immunzellen befinden sich im Darm; das Mikrobiom trainiert das Immunsystem",
        answerC = "Das Darmmikrobiom schwaeacht das Immunsystem",
        answerD = "Das Mikrobiom beeinflusst nur die Verdauung, nicht das Immunsystem",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Etwa 70 bis 80 Prozent der Immunzellen des Koerpers befinden sich im und um den Darm. Das Darmmikrobiom trainiert das Immunsystem, hilft ihm zwischen Freund und Feind zu unterscheiden und moduliert Entzuendungsreaktionen.",
        funFact = "Studien an keimfreien Maeussen (ohne Darmbakterien aufgezogen) zeigen dramatisch schlechtere Immunantworten. Dies belegt, wie entscheidend Darmbakterien fuer die Entwicklung eines funktionierenden Immunsystems sind."
    ),

    // Question 40
    Question(
        categoryId = 16,
        questionText = "Was ist Gluten und bei welchen Personen verursacht es Probleme?",
        answerA = "Ein Fett in Milchprodukten, das bei Laktoseintoleranz schadet",
        answerB = "Ein Kleberprotein in Getreide, das bei Zoeliakie das Duenndarm schaedigt",
        answerC = "Ein Zucker in Obst, der bei Fruchtzuckerintoleranz Probleme macht",
        answerD = "Ein Enzym im Brot, das bei Magengeschwueren schadet",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Gluten ist ein Proteingemisch in Weizen, Roggen und Gerste. Bei Menschen mit Zoeliakie (einer Autoimmunerkrankung) loest Gluten eine Immunreaktion aus, die die Duenndarmschleimhaut schaedigt und Naehrstoffmangel verursacht.",
        funFact = "In Deutschland haben etwa 1 Prozent der Bevoelkerung Zoeliakie -- viele Faelle sind aber undiagnostiziert. Glutensensitivitaet ohne Zoeliakie ist haeufiger, aber wissenschaftlich noch nicht vollstaendig verstanden."
    ),

    // Question 41
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einer Nahrungsmittelallergie und einer Nahrungsmittelintoleranz?",
        answerA = "Es gibt keinen Unterschied -- beides ist dasselbe",
        answerB = "Allergie = Immunreaktion (oft schnell und heftig), Intoleranz = Stoffwechselproblem (meist langsamer)",
        answerC = "Allergien kommen nur bei Kindern vor, Intoleranzen nur bei Erwachsenen",
        answerD = "Intoleranzen sind immer lebensbedrohlich, Allergien nicht",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei einer Nahrungsmittelallergie reagiert das Immunsystem ueberschiessend auf bestimmte Proteine -- das kann von Nesselsucht bis zum lebensbedrohlichen Schock reichen. Bei Intoleranzen (z.B. Laktose) fehlt ein Enzym oder ein Transporter, was Verdauungsprobleme verursacht.",
        funFact = "Eine echte Erdnussallergie kann lebensbedrohlich sein -- schon kleinste Mengen koennen einen anaphylaktischen Schock ausloesen. Deshalb tragen viele Allergiker Adrenalin-Autoinjektoren (EpiPen) mit sich."
    ),

    // Question 42
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem thermischen Effekt der Nahrung?",
        answerA = "Die Kalorien, die beim Kochen von Lebensmitteln verloren gehen",
        answerB = "Die Energie, die der Koerper fuer Verdauung, Absorption und Verstoffwechselung von Nahrung benoetigt",
        answerC = "Die Waerme, die beim Essen aufgenommen wird",
        answerD = "Der Einfluss von heissen Speisen auf den Stoffwechsel",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der thermische Effekt der Nahrung (TEF) beschreibt die Energie, die der Koerper fuer die Verdauung, Aufnahme und Verstoffwechselung von Nahrung verbraucht. Er macht etwa 10 Prozent des Gesamtkalorienverbrauchs aus.",
        funFact = "Protein hat den hoechsten thermischen Effekt: Fuer je 100 aufgenommene Kalorien aus Eiweiss werden bis zu 30 Kalorien nur fuer die Verdauung verbraucht. Bei Fett sind es nur 3 bis 5 Prozent."
    ),

    // Question 43
    Question(
        categoryId = 16,
        questionText = "Welche Nahrungsmittel sind besonders reich an Kalzium?",
        answerA = "Fleisch, Wurst und Eier",
        answerB = "Milchprodukte, Sesam, Mandeln und Gruenkohl",
        answerC = "Weissbrot, Nudeln und Kartoffeln",
        answerD = "Rote Beete, Karotten und Zuckerrueben",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Milch, Kaese und Joghurt sind die bekanntesten Kalziumquellen. Auch Sesam, Mandeln, Gruenkohl, Brokkoli und kalziumreiches Mineralwasser enthalten bedeutende Mengen Kalzium.",
        funFact = "100 Gramm Parmesan enthalten mehr Kalzium als ein grosses Glas Milch. Und Sesamsamen haben sogar noch mehr Kalzium -- allerdings ist die Verwertbarkeit aus pflanzlichen Quellen geringer als aus Milchprodukten."
    ),

    // Question 44
    Question(
        categoryId = 16,
        questionText = "Was ist Uebergewicht im medizinischen Sinne nach BMI-Definition?",
        answerA = "Ein BMI ueber 20",
        answerB = "Ein BMI ueber 25",
        answerC = "Ein BMI ueber 30",
        answerD = "Ein BMI ueber 35",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Nach der WHO-Definition gilt ein BMI von 25 bis 29,9 als Uebergewicht. Ab einem BMI von 30 spricht man von Adipositas (Fettleibigkeit). Normalgewicht liegt bei einem BMI von 18,5 bis 24,9.",
        funFact = "Laut WHO-Zahlen von 2024 sind weltweit erstmals mehr als 1 Milliarde Menschen adipoes (BMI ueber 30). Gleichzeitig leiden noch immer Hunderte Millionen Menschen an Unternaehrung -- beides sind globale Gesundheitskrisen."
    ),

    // Question 45
    Question(
        categoryId = 16,
        questionText = "Was passiert beim 'Jojo-Effekt' nach einer Diaet?",
        answerA = "Der Koerper verbrennt nach der Diaet mehr Kalorien als vorher",
        answerB = "Nach der Diaet nimmt man schnell wieder zu, oft ueber das Ausgangsgewicht hinaus",
        answerC = "Der Jojo-Effekt betrifft nur Personen mit genetischer Veranlagung",
        answerD = "Der Koerper schuetzt sich vor dem Jojo-Effekt durch erhohten Grundumsatz",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Beim Jojo-Effekt sinkt der Grundumsatz waehrend einer Kalorienrestriktion. Nach der Diaet wird genauso viel gegessen wie zuvor, aber weniger verbrannt -- die Folge ist schnelle Gewichtszunahme, oft ueber das Ausgangsgewicht hinaus.",
        funFact = "Studien zeigen, dass wiederholte Diaeten den Koerper trainieren, effizienter mit Energie umzugehen -- was zukuenftige Abnahme schwieriger macht. Nachhaltige Lebensstilaenderungen wirken langfristig besser als kurze Crashdiaeten."
    ),

    // Question 46
    Question(
        categoryId = 16,
        questionText = "Was ist die Funktion von Eisen im menschlichen Koerper?",
        answerA = "Eisen staerkt die Knochen wie Kalzium",
        answerB = "Eisen ist Bestandteil des Haemoglobins und notwendig fuer den Sauerstofftransport im Blut",
        answerC = "Eisen reguliert den Blutzuckerspiegel",
        answerD = "Eisen wird fuer die Verdauung von Fetten benoetigt",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eisen ist ein zentraler Bestandteil des Haemoglobins in roten Blutkoerperchen, das Sauerstoff von der Lunge zu allen Organen transportiert. Eisenmangel fuehrt zu Anaemie mit Symptomen wie Muedigkeit und Erschoepfung.",
        funFact = "Frauen haben ein deutlich hoeheres Risiko fuer Eisenmangel als Maenner, weil sie durch die Menstruation regelmaessig Eisen verlieren. Weltweit ist Eisenmangel der haeufigste Naehrstoffmangel."
    ),

    // Question 47
    Question(
        categoryId = 16,
        questionText = "Was sind Phytochemikalien (sekundaere Pflanzenstoffe)?",
        answerA = "Kuenstliche Zusatzstoffe in verarbeiteten Lebensmitteln",
        answerB = "Natuerliche bioaktive Verbindungen in Pflanzen mit gesundheitsfoerdernden Eigenschaften",
        answerC = "Pestizide, die auf Pflanzen gesprueht werden",
        answerD = "Naehrstoffe, die nur in Tiernahrung vorkommen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Sekundaere Pflanzenstoffe (Phytochemikalien) sind bioaktive Verbindungen in Pflanzen -- wie Flavonoide, Carotinoide und Polyphenole -- die antioxidativ, entzuendungshemmend und antimikrobiell wirken koennen.",
        funFact = "Es gibt ueber 100.000 verschiedene sekundaere Pflanzenstoffe. Viele Gewuerze enthalten besonders hohe Konzentrationen: Kurkumin aus Kurkuma, Resveratrol aus Rotwein und Sulforaphan aus Brokkoli sind besonders gut erforscht."
    ),

    // Question 48
    Question(
        categoryId = 16,
        questionText = "Was passiert bei einer Hypoglykamie (Unterzuckerung)?",
        answerA = "Der Blutzucker steigt zu stark an",
        answerB = "Der Blutzucker faellt unter den Normalwert, was zu Schwaeche, Zittern und Bewusstseinsstoerungen fuehrt",
        answerC = "Der Koerper speichert zu viel Zucker als Fett",
        answerD = "Die Bauchspeicheldruse produziert zu wenig Verdauungsenzyme",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Hypoglykamie tritt auf, wenn der Blutzuckerspiegel unter 70 mg/dl sinkt. Das Gehirn, das fast ausschliesslich Glukose als Energiequelle nutzt, reagiert mit Zittern, Schweissausbruechen, Konzentrationsstoerungen bis hin zu Bewusstlosigkeit.",
        funFact = "Menschen mit Diabetes koennen in eine gefaehrliche Unterzuckerung geraten, wenn sie zu viel Insulin spritzenoder zu wenig essen. Notfallbehandlung: schnell Traubenzucker oder Fruchtsaft -- beides hebt den Blutzucker innerhalb von Minuten an."
    ),

    // Question 49
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Vollkornprodukten und Auszugsmehlprodukten?",
        answerA = "Vollkornprodukte enthalten mehr Kalorien und Zucker",
        answerB = "Vollkornprodukte enthalten alle Teile des Korns (inkl. Kleie und Keimling) und mehr Ballaststoffe, Vitamine und Mineralstoffe",
        answerC = "Auszugsmehlprodukte sind naehrstoffreicher, weil das Korn gereinigt wird",
        answerD = "Es gibt keinen ernaehrungsphysiologischen Unterschied",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Vollkornprodukte enthalten das gesamte Getreidekorn -- Schale (Kleie), Mehlkoerper und Keimling. Auszugsmehl (z.B. Weissmehl) besteht nur aus dem Mehlkoerper und hat deutlich weniger Ballaststoffe, Vitamine und Mineralstoffe.",
        funFact = "Beim Mahlen von Weizenkorn zu Weissmehl gehen bis zu 80 Prozent des Magnesiums, 75 Prozent des Zinks und 70 Prozent der B-Vitamine verloren. Deshalb wird Weissmehl in manchen Laendern mit Vitaminen angereichert."
    ),

    // Question 50
    Question(
        categoryId = 16,
        questionText = "Was ist der 'Darm-Hirn-Achse' (Gut-Brain-Axis)?",
        answerA = "Ein Nervenstrang, der Darm und Gehirn direkt verbindet und nur beim Menschen vorkommt",
        answerB = "Das bidirektionale Kommunikationssystem zwischen Darm und Gehirn, das Stimmung und Verhalten beeinflusst",
        answerC = "Die Verbindung zwischen Darm und Gehirn, die nur fuer die Steuerung der Verdauung zustaendig ist",
        answerD = "Ein Blutgefaess, das Naehrstoffe vom Darm zum Gehirn transportiert",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Darm-Hirn-Achse ist ein komplexes bidirektionales Kommunikationsnetzwerk zwischen Darm und Gehirn, das ueber Nerven (Vagusnerv), Hormone und Botenstoffe des Mikrobioms laeuft und Stimmung, Stress und kognitive Funktionen beeinflusst.",
        funFact = "Etwa 90 Prozent des Serotonins -- dem 'Glueckshormon' -- werden im Darm produziert, nicht im Gehirn. Das erklaert, warum Darmproblemme oft mit Depressionen und Angst zusammenhaengen und umgekehrt."
    ),

)
