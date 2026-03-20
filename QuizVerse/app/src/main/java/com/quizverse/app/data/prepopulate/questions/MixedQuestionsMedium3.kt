package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMedium3(): List<Question> = listOf(

    // Question 1 — Wirtschaft: BIP
    Question(
        categoryId = 11,
        questionText = "Was misst das Bruttoinlandsprodukt (BIP) eines Landes?",
        answerA = "Den Gewinn aller boersennotierten Unternehmen",
        answerB = "Den Wert aller in einem Land produzierten Gueter und Dienstleistungen",
        answerC = "Die Gesamtschulden des Staates",
        answerD = "Die Anzahl der Erwerbstaetigen",
        correctAnswer = 1,
        explanation = "Das BIP misst den Gesamtwert aller Gueter und Dienstleistungen, die innerhalb eines Landes in einem bestimmten Zeitraum produziert werden. Es ist der wichtigste Indikator fuer die Wirtschaftsleistung eines Landes.",
        difficulty = 2,
        funFact = "Deutschland hat eines der groessten BIPs weltweit und liegt regelmaessig unter den Top 5 der groessten Volkswirtschaften."
    ),

    // Question 2 — Waehrung: Euro-Einfuehrung
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde der Euro als Bargeld in Deutschland eingefuehrt?",
        answerA = "1999",
        answerB = "2000",
        answerC = "2002",
        answerD = "2004",
        correctAnswer = 2,
        explanation = "Der Euro wurde am 1. Januar 2002 als Bargeld in Deutschland und elf weiteren EU-Staaten eingefuehrt. Als Buchgeld existierte er bereits seit dem 1. Januar 1999.",
        difficulty = 2,
        funFact = "In der Nacht auf den 1. Januar 2002 wurden in Deutschland ueber 15 Milliarden Euromuenzen und 1,4 Milliarden Banknoten in Umlauf gebracht — die groesste Waehrungsumstellung der Geschichte."
    ),

    // Question 3 — Boerse: DAX
    Question(
        categoryId = 11,
        questionText = "Wie viele Unternehmen sind im deutschen Leitindex DAX enthalten?",
        answerA = "20",
        answerB = "30",
        answerC = "40",
        answerD = "50",
        correctAnswer = 2,
        explanation = "Seit September 2021 umfasst der DAX 40 Unternehmen (zuvor waren es 30). Er spiegelt die Kursentwicklung der 40 groessten und umsatzstaerksten deutschen Aktiengesellschaften wider.",
        difficulty = 2,
        funFact = "Der DAX wurde am 1. Juli 1988 eingefuehrt und startete mit einem Basiswert von 1.000 Punkten. Mittlerweile steht er bei einem Vielfachen davon."
    ),

    // Question 4 — Unternehmen: Apple-Gruendung
    Question(
        categoryId = 11,
        questionText = "Von wem wurde Apple am 1. April 1976 gegruendet?",
        answerA = "Bill Gates und Paul Allen",
        answerB = "Steve Jobs, Steve Wozniak und Ronald Wayne",
        answerC = "Larry Page und Sergey Brin",
        answerD = "Elon Musk und Peter Thiel",
        correctAnswer = 1,
        explanation = "Apple wurde am 1. April 1976 von Steve Jobs, Steve Wozniak und Ronald Wayne gegruendet. Ronald Wayne verkaufte seinen 10-prozentigen Anteil bereits nach 12 Tagen fuer 800 Dollar — heute waere er Milliarden wert.",
        difficulty = 2,
        funFact = "Ronald Waynes 10-Prozent-Anteil an Apple waere heute mehr als 300 Milliarden Dollar wert. Er bedauert den Verkauf, erklaert aber, er haette damals keine andere Wahl gesehen."
    ),

    // Question 5 — Wirtschaft: Inflation
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der Begriff Inflation in der Wirtschaft?",
        answerA = "Das Wachstum der Bevoelkerung",
        answerB = "Den Anstieg der Staatsschulden",
        answerC = "Den allgemeinen Anstieg des Preisniveaus",
        answerD = "Die Erweiterung eines Unternehmens",
        correctAnswer = 2,
        explanation = "Inflation bezeichnet den anhaltenden Anstieg des allgemeinen Preisniveaus fuer Gueter und Dienstleistungen. Dadurch verliert Geld an Kaufkraft — man bekommt fuer denselben Betrag weniger.",
        difficulty = 2,
        funFact = "Die schlimmste Inflation Deutschlands war die Hyperinflation von 1923. Ein US-Dollar kostete zeitweise 4,2 Billionen Mark — Menschen trugen Geld in Schubkarren zur Baeckerei."
    ),

    // Question 6 — Waehrung: Japanische Waehrung
    Question(
        categoryId = 11,
        questionText = "Wie heisst die offizielle Waehrung Japans?",
        answerA = "Won",
        answerB = "Yuan",
        answerC = "Ringgit",
        answerD = "Yen",
        correctAnswer = 3,
        explanation = "Die offizielle Waehrung Japans ist der Yen (Zeichen: ¥). Er ist nach dem US-Dollar und dem Euro die drittmeistgehandelte Waehrung der Welt.",
        difficulty = 2
    ),

    // Question 7 — Boerse: Aktie
    Question(
        categoryId = 11,
        questionText = "Was verbrieft eine Aktie fuer ihren Inhaber?",
        answerA = "Einen Kredit an das Unternehmen",
        answerB = "Eine Mitgliedschaft in einer Gewerkschaft",
        answerC = "Einen Anteil am Eigenkapital eines Unternehmens",
        answerD = "Eine Garantie auf Rueckzahlung",
        correctAnswer = 2,
        explanation = "Eine Aktie ist ein Wertpapier, das einen Anteil am Eigenkapital einer Aktiengesellschaft verbrieft. Aktionaere sind damit Miteigentuemer des Unternehmens und haben Stimmrecht sowie Anspruch auf Dividenden.",
        difficulty = 2,
        funFact = "Die aelteste noch existierende Aktie der Welt wurde 1606 von der Niederlaendischen Ostindien-Kompanie ausgegeben und befindet sich heute im Besitz eines privaten Sammlers."
    ),

    // Question 8 — Unternehmen: Microsoft-Gruendung
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr gruendeten Bill Gates und Paul Allen Microsoft?",
        answerA = "1970",
        answerB = "1975",
        answerC = "1980",
        answerD = "1985",
        correctAnswer = 1,
        explanation = "Microsoft wurde am 4. April 1975 von Bill Gates und Paul Allen in Albuquerque, New Mexico, gegruendet. Das Unternehmen entwickelte sich von einem kleinen Softwarehaus zum weltgroessten Softwarekonzern.",
        difficulty = 2,
        funFact = "Der Name Microsoft setzt sich aus den Woertern Microcomputer und Software zusammen. Gates und Allen gaben dem Unternehmen diesen Namen, weil sie Software fuer Microcomputer entwickelten."
    ),

    // Question 9 — Wirtschaft: Zins
    Question(
        categoryId = 11,
        questionText = "Was ist der Leitzins einer Zentralbank?",
        answerA = "Der Zinssatz fuer Hypothekenkredite privater Banken",
        answerB = "Der Zinssatz, zu dem sich Geschaeftsbanken Geld bei der Zentralbank leihen",
        answerC = "Der Zinssatz fuer Staatanleihen",
        answerD = "Der durchschnittliche Sparzins aller Banken",
        correctAnswer = 1,
        explanation = "Der Leitzins ist der Zinssatz, zu dem sich Geschaeftsbanken kurzfristig Geld bei der Zentralbank leihen koennen. Er beeinflusst massgeblich die Zinsen fuer Kredite und Spareinlagen im gesamten Wirtschaftssystem.",
        difficulty = 2,
        funFact = "Die Europaeische Zentralbank (EZB) hatte den Leitzins zwischen 2016 und 2022 auf null Prozent gesetzt — ein historisches Tief, um die Wirtschaft nach der Finanzkrise anzukurbeln."
    ),

    // Question 10 — Waehrung: Britisches Pfund
    Question(
        categoryId = 11,
        questionText = "Welches Land verwendet das Pfund Sterling als Waehrung?",
        answerA = "Irland",
        answerB = "Schottland allein",
        answerC = "Grossbritannien",
        answerD = "Australien",
        correctAnswer = 2,
        explanation = "Das Pfund Sterling (GBP) ist die Waehrung des Vereinigten Koenigreichs Grossbritannien und Nordirland. Es ist eine der aeltesten noch im Umlauf befindlichen Waehrungen der Welt.",
        difficulty = 2,
        funFact = "Das Pfund Sterling ist die aelteste noch verwendete Waehrung der Welt. Es wurde vor ueber 1.200 Jahren eingefuehrt und geht auf angelsaechsische Silbermuenzen zurueck."
    ),

    // Question 11 — Boerse: Schwarzer Freitag
    Question(
        categoryId = 11,
        questionText = "Wann fand der beruehnte Boersencrash an der Wall Street statt, der die Weltwirtschaftskrise ausloeste?",
        answerA = "Oktober 1919",
        answerB = "Oktober 1929",
        answerC = "Oktober 1939",
        answerD = "Oktober 1949",
        correctAnswer = 1,
        explanation = "Am 24. Oktober 1929 (Schwarzer Donnerstag) und 29. Oktober 1929 (Schwarzer Dienstag) brach die New Yorker Boerse ein. In Deutschland wird der 25. Oktober 1929 als Schwarzer Freitag bezeichnet. Der Crash loeste die grosse Weltwirtschaftskrise aus.",
        difficulty = 2,
        funFact = "Nach dem Boersencrash von 1929 verloren Millionen Amerikaner ihre Ersparnisse. Die Arbeitslosigkeit in den USA stieg auf fast 25 Prozent."
    ),

    // Question 12 — Wirtschaft: Angebot und Nachfrage
    Question(
        categoryId = 11,
        questionText = "Was passiert mit einem Preis, wenn das Angebot sinkt, die Nachfrage aber gleichbleibt?",
        answerA = "Der Preis faellt",
        answerB = "Der Preis steigt",
        answerC = "Der Preis bleibt gleich",
        answerD = "Der Markt bricht zusammen",
        correctAnswer = 1,
        explanation = "Sinkt das Angebot bei gleichbleibender Nachfrage, wird das Gut knapper. Da mehr Kaeufer um weniger Ware konkurrieren, steigt der Preis. Dieses Grundprinzip nennt sich Gesetz von Angebot und Nachfrage.",
        difficulty = 2
    ),

    // Question 13 — Unternehmen: Google-Gruendung
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde Google gegruendet?",
        answerA = "1994",
        answerB = "1996",
        answerC = "1998",
        answerD = "2001",
        correctAnswer = 2,
        explanation = "Google Inc. wurde am 4. September 1998 von Larry Page und Sergey Brin gegruendet. Die beiden trafen sich als Doktoranden an der Stanford University. Der Name leitet sich vom mathematischen Begriff Googol (10 hoch 100) ab.",
        difficulty = 2,
        funFact = "Der erste Google-Server war aus Lego gebaut — Page und Brin nutzten Lego-Teile, um ihre Festplatten stabil zu befestigen, weil sie guenstiger waren als echte Server-Gehaeusse."
    ),

    // Question 14 — Waehrung: Schweizer Franken
    Question(
        categoryId = 11,
        questionText = "Welches Land gilt als sicherer Hafen und hat den Franken als Waehrung?",
        answerA = "Oesterreich",
        answerB = "Liechtenstein und Schweiz",
        answerC = "Norwegen",
        answerD = "Luxemburg",
        correctAnswer = 1,
        explanation = "Die Schweiz und das Fuerstentum Liechtenstein verwenden den Schweizer Franken (CHF) als Waehrung. Der Franken gilt als eine der stabilsten Waehrungen der Welt und wird in Krisenzeiten als sicherer Hafen betrachtet.",
        difficulty = 2,
        funFact = "Der Schweizer Franken ist als einzige grosse Waehrung der Welt nicht dezimal unterteilt. Ein Franken hat 100 Rappen — aber der Begriff Rappen ist in der Deutschschweiz gebaeuchlich, waehrend Tessiner Centesimi sagen."
    ),

    // Question 15 — Boerse: ETF
    Question(
        categoryId = 11,
        questionText = "Was ist ein ETF (Exchange Traded Fund)?",
        answerA = "Eine Versicherung gegen Kursverluste",
        answerB = "Ein Fonds, der einen Index nachbildet und an der Boerse gehandelt wird",
        answerC = "Ein staatlicher Rentenfonds",
        answerD = "Eine Kryptoswaehrung",
        correctAnswer = 1,
        explanation = "Ein ETF ist ein boersengehandelter Fonds, der die Wertentwicklung eines Index (z.B. DAX oder S&P 500) nachbildet. Er vereint die Diversifikation eines Fonds mit der einfachen Handelbarkeit einer Aktie.",
        difficulty = 2,
        funFact = "Weltweit sind in ETFs mehr als 10 Billionen US-Dollar investiert. Der erste ETF wurde 1990 in Kanada aufgelegt — in Deutschland gibt es ETFs seit dem Jahr 2000."
    ),

    // Question 16 — Wirtschaft: Zentralbank Europa
    Question(
        categoryId = 11,
        questionText = "Wo hat die Europaeische Zentralbank (EZB) ihren Sitz?",
        answerA = "Bruessel",
        answerB = "Paris",
        answerC = "Frankfurt am Main",
        answerD = "Luxemburg",
        correctAnswer = 2,
        explanation = "Die Europaeische Zentralbank (EZB) hat ihren Sitz in Frankfurt am Main. Sie wurde 1998 gegruendet und ist fuer die Geldpolitik der Eurozone zustaendig, also fuer die Steuerung von Inflation und Zinsen.",
        difficulty = 2,
        funFact = "Das EZB-Hochhaus in Frankfurt ist 185 Meter hoch und wurde 2015 eroeffnet. Es steht auf dem Gelaende der ehemaligen Grossmarkthalle, die 1928 erbaut wurde."
    ),

    // Question 17 — Unternehmen: Amazon-Gruendung
    Question(
        categoryId = 11,
        questionText = "Womit begann Jeff Bezos, als er Amazon 1994 gruendete?",
        answerA = "Mit dem Online-Verkauf von Elektronik",
        answerB = "Mit einem Cloud-Computing-Dienst",
        answerC = "Mit dem Online-Verkauf von Buecher",
        answerD = "Mit einer Versandapotheke",
        correctAnswer = 2,
        explanation = "Jeff Bezos gruendete Amazon 1994 in seiner Garage in Seattle und begann mit dem Online-Verkauf von Buechern. Buecher waren ideal: universell, leicht zu versenden und in riesiger Auswahl vorhanden. Danach expandierte Amazon stetig.",
        difficulty = 2,
        funFact = "Bezos nannte das Unternehmen zuerst Cadabra (wie Abrakadabra), aenderte es dann aber, weil ein Anwalt Cadabra mit Cadaver (Leiche) verwechselte. Amazon klang global und suggerierte Groesse."
    ),

    // Question 18 — Waehrung: US-Dollar
    Question(
        categoryId = 11,
        questionText = "Welches Symbol steht fuer den US-Dollar?",
        answerA = "£",
        answerB = "$",
        answerC = "€",
        answerD = "¥",
        correctAnswer = 1,
        explanation = "Das Dollarzeichen ($) steht fuer den US-Dollar (USD). Es ist die Leitwährung der Welt und wird fuer den Grossteil des internationalen Handels und der Rohstoffpreise verwendet.",
        difficulty = 2,
        funFact = "Die genaue Herkunft des $-Zeichens ist umstritten. Eine Theorie besagt, es sei aus dem spanischen Zeichen fuer Peso/Piaster entstanden, eine andere sieht es als Abkuerzung fuer US (United States)."
    ),

    // Question 19 — Boerse: Dividende
    Question(
        categoryId = 11,
        questionText = "Was ist eine Dividende?",
        answerA = "Eine Strafe fuer Aktionaere bei Kursverlusten",
        answerB = "Eine Gebuehr fuer das Handelskonto",
        answerC = "Die Beteiligung der Aktionaere am Unternehmensgewinn",
        answerD = "Der Kurswert einer Aktie",
        correctAnswer = 2,
        explanation = "Eine Dividende ist der Teil des Unternehmensgewinns, der an die Aktionaere ausgeschuettet wird. Nicht alle Unternehmen zahlen Dividenden — viele reinvestieren Gewinne lieber ins Wachstum.",
        difficulty = 2,
        funFact = "Deutsche Unternehmen wie Allianz, Deutsche Telekom und BASF gehoeren zu den dividendenstarksten Aktien Europas und schuetten jaehrlich Milliarden an ihre Aktionaere aus."
    ),

    // Question 20 — Wirtschaft: Rezession
    Question(
        categoryId = 11,
        questionText = "Wann spricht man offiziell von einer wirtschaftlichen Rezession?",
        answerA = "Wenn die Arbeitslosigkeit ueber 5 Prozent steigt",
        answerB = "Wenn das BIP zwei Quartale hintereinander schrumpft",
        answerC = "Wenn die Inflation ueber 3 Prozent steigt",
        answerD = "Wenn ein Land Staatsschulden aufnimmt",
        correctAnswer = 1,
        explanation = "Eine Rezession liegt vor, wenn die Wirtschaftsleistung (BIP) zwei Quartale hintereinander zurueckgeht. Es ist eine Phase wirtschaftlicher Abschwaeche mit sinkender Produktion, haeufig steigender Arbeitslosigkeit und ruecklaeufigemn Konsum.",
        difficulty = 2,
        funFact = "Die schwerste Rezession der Nachkriegszeit war die Grosse Rezession von 2008/2009, ausgeloest durch die US-Immobilienkrise und den Zusammenbruch von Lehman Brothers."
    ),

    // Question 21 — Unternehmen: Tesla-Gruendung
    Question(
        categoryId = 11,
        questionText = "Wann wurde Tesla Motors gegruendet?",
        answerA = "1999",
        answerB = "2001",
        answerC = "2003",
        answerD = "2006",
        correctAnswer = 2,
        explanation = "Tesla Motors wurde am 1. Juli 2003 von Martin Eberhard und Marc Tarpenning gegruendet. Elon Musk war kein Mitgruender, sondern trat 2004 als Hauptinvestor ein und uebernahm spaeter die Fuehrung.",
        difficulty = 2,
        funFact = "Tesla ist nach dem Erfinder und Elektroingenieur Nikola Tesla benannt. Das Unternehmen brachte 2008 mit dem Tesla Roadster sein erstes Auto auf den Markt — das erste Serienauto mit Lithium-Ionen-Batterie."
    ),

    // Question 22 — Waehrung: Russischer Rubel
    Question(
        categoryId = 11,
        questionText = "Wie heisst die offizielle Waehrung Russlands?",
        answerA = "Zloty",
        answerB = "Rubel",
        answerC = "Tenge",
        answerD = "Hrywnja",
        correctAnswer = 1,
        explanation = "Die offizielle Waehrung Russlands ist der Russische Rubel (RUB). Der Rubel ist eine der aeltesten nationalen Waehrungen der Welt und geht auf das 13. Jahrhundert zurueck.",
        difficulty = 2
    ),

    // Question 23 — Boerse: Leerverkauf
    Question(
        categoryId = 11,
        questionText = "Was versteht man unter einem Leerverkauf an der Boerse?",
        answerA = "Den Kauf von Aktien ohne Eigenkapital",
        answerB = "Den Verkauf von geliehenen Aktien in Erwartung fallender Kurse",
        answerC = "Den Handel mit wertlosen Aktien",
        answerD = "Den automatischen Verkauf bei Kursrueckgang",
        correctAnswer = 1,
        explanation = "Bei einem Leerverkauf (Short Selling) leiht sich ein Investor Aktien und verkauft sie sofort. Er hofft, sie spaeter guenstiger zurueckzukaufen und die Differenz als Gewinn einzustreichen. Leerverkaeufer wetten also auf fallende Kurse.",
        difficulty = 2,
        funFact = "Leerverkaeufer werden oft als Boesenbuben der Boerse bezeichnet — aber sie decken auch Betruege auf: Der Leerverkaeuf Hindenburg Research enthuellte mehrere spektakulaere Unternehmensbetruge."
    ),

    // Question 24 — Wirtschaft: Soziale Marktwirtschaft
    Question(
        categoryId = 11,
        questionText = "Was ist das charakteristische Merkmal der sozialen Marktwirtschaft in Deutschland?",
        answerA = "Der Staat bestimmt alle Preise",
        answerB = "Private Unternehmen sind verboten",
        answerC = "Freier Markt mit staatlichem Sozialausgleich",
        answerD = "Alle Unternehmen gehoeren dem Staat",
        correctAnswer = 2,
        explanation = "Die soziale Marktwirtschaft kombiniert die Effizienz eines freien Marktes mit staatlichen Korrekturmassnahmen fuer sozialen Ausgleich. Unternehmen konkurrieren frei, der Staat greift aber ein, um Armut, Monopole und soziale Ungerechtigkeit zu begrenzen.",
        difficulty = 2,
        funFact = "Das Konzept der sozialen Marktwirtschaft wurde massgeblich von Ludwig Erhard gepraegt, dem ersten deutschen Wirtschaftsminister und spaeteren Bundeskanzler."
    ),

    // Question 25 — Unternehmen: IKEA-Gruendung
    Question(
        categoryId = 11,
        questionText = "In welchem Land wurde IKEA gegruendet?",
        answerA = "Norwegen",
        answerB = "Daenemark",
        answerC = "Finnland",
        answerD = "Schweden",
        correctAnswer = 3,
        explanation = "IKEA wurde 1943 von Ingvar Kamprad im schwedischen Agunnaryd gegruendet. Der Name ist ein Akronym aus den Initialen des Gruenders (I.K.) sowie dem Gutshof (Elmtaryd) und dem Heimatdorf (Agunnaryd).",
        difficulty = 2,
        funFact = "IKEA gruendete Kamprad mit 17 Jahren. Anfangs verkaufte er Streichhoelzer, Kugelschreiber und Bilder — Moebel kamen erst 1947 ins Sortiment."
    ),

    // Question 26 — Waehrung: Chinesischer Yuan
    Question(
        categoryId = 11,
        questionText = "Wie heisst die offizielle Waehrung der Volksrepublik China?",
        answerA = "Yen",
        answerB = "Won",
        answerC = "Renminbi (Yuan)",
        answerD = "Baht",
        correctAnswer = 2,
        explanation = "Die Waehrung der Volksrepublik China heisst offiziell Renminbi (RMB), was 'Volkswswaehrung' bedeutet. Die Grundeinheit ist der Yuan. China ist die zweitgroesste Volkswirtschaft der Welt.",
        difficulty = 2,
        funFact = "Der Renminbi ist eine der meistgehandelten Waehrungen der Welt, aber sein Wert wird von der chinesischen Regierung kontrolliert und nicht vollstaendig frei gehandelt."
    ),

    // Question 27 — Boerse: Bulle und Baer
    Question(
        categoryId = 11,
        questionText = "Was bedeutet ein 'Bullenmarkt' an der Boerse?",
        answerA = "Starke Kursschwankungen",
        answerB = "Anhaltend steigende Kurse",
        answerC = "Anhaltend fallende Kurse",
        answerD = "Handel mit Rohstoffen",
        correctAnswer = 1,
        explanation = "Ein Bullenmarkt (Bull Market) bezeichnet eine Phase anhaltend steigender Boersenkurse. Der Begriff kommt vom Angriff des Stiers, der seine Hoerner von unten nach oben stoesst — im Gegensatz zum Baer, der von oben nach unten schlaegt (Baeremarkt = fallende Kurse).",
        difficulty = 2,
        funFact = "Der laengste Bullenmarkt der US-Geschichte dauerte von 2009 bis 2020 — exakt 11 Jahre. Er endete mit dem Boersensturz durch die Corona-Pandemie."
    ),

    // Question 28 — Wirtschaft: Mindestlohn
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde der gesetzliche Mindestlohn in Deutschland eingefuehrt?",
        answerA = "2010",
        answerB = "2012",
        answerC = "2015",
        answerD = "2018",
        correctAnswer = 2,
        explanation = "Der gesetzliche Mindestlohn wurde in Deutschland am 1. Januar 2015 eingefuehrt. Er startete bei 8,50 Euro pro Stunde und wurde seitdem schrittweise erhoet.",
        difficulty = 2,
        funFact = "Deutschland war eines der letzten grossen Industrielaender, das einen gesetzlichen Mindestlohn einfuehrte. Laender wie Frankreich, Grossbritannien und die USA haben ihn schon seit Jahrzehnten."
    ),

    // Question 29 — Unternehmen: Volkswagen-Gruendung
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde Volkswagen gegruendet?",
        answerA = "1925",
        answerB = "1933",
        answerC = "1937",
        answerD = "1945",
        correctAnswer = 2,
        explanation = "Volkswagen wurde am 28. Mai 1937 als 'Gesellschaft zur Vorbereitung des Deutschen Volkswagens mbH' gegruendet. Die Idee eines erschwinglichen Autos fuer jedermann stammte von Ferdinand Porsche.",
        difficulty = 2,
        funFact = "Der erste Volkswagen, der Kaefer, war eines der meistproduzierten Autos der Geschichte. Insgesamt wurden ueber 21 Millionen Einheiten gebaut."
    ),

    // Question 30 — Waehrung: Indien
    Question(
        categoryId = 11,
        questionText = "Wie heisst die Waehrung Indiens?",
        answerA = "Rupiah",
        answerB = "Rupie",
        answerC = "Dirham",
        answerD = "Peso",
        correctAnswer = 1,
        explanation = "Die Waehrung Indiens heisst Indische Rupie (INR). Indien ist eine der am schnellsten wachsenden grossen Volkswirtschaften der Welt und inzwischen die fuenftgroesste nach BIP.",
        difficulty = 2,
        funFact = "Indonesien hat auch eine aehnliche Waehrung namens Rupiah. Beide Namen leiten sich vom Sanskrit-Wort 'Rupya' ab, das 'Silbermuenze' bedeutet."
    ),

    // Question 31 — Boerse: Marktkapitalisierung
    Question(
        categoryId = 11,
        questionText = "Was versteht man unter der Marktkapitalisierung eines Unternehmens?",
        answerA = "Den Jahresgewinn des Unternehmens",
        answerB = "Den Gesamtwert aller ausstehenden Aktien",
        answerC = "Das Eigenkapital des Unternehmens",
        answerD = "Den Boersenplatz des Unternehmens",
        correctAnswer = 1,
        explanation = "Die Marktkapitalisierung ist der Gesamtwert aller ausgegebenen Aktien eines Unternehmens. Sie errechnet sich aus: Aktienkurs x Anzahl der Aktien. Sie zeigt, wie viel ein Unternehmen an der Boerse wert ist.",
        difficulty = 2,
        funFact = "Apple war 2022 das erste Unternehmen der Geschichte mit einer Marktkapitalisierung von ueber 3 Billionen US-Dollar — mehr als das BIP der meisten Laender der Welt."
    ),

    // Question 32 — Wirtschaft: Handelsbilanz
    Question(
        categoryId = 11,
        questionText = "Was ist ein Handelsbilanzueberschuss?",
        answerA = "Wenn ein Land mehr importiert als exportiert",
        answerB = "Wenn ein Land mehr exportiert als importiert",
        answerC = "Wenn der Staatshaushalt ausgeglichen ist",
        answerD = "Wenn die Inflation unter 2 Prozent liegt",
        correctAnswer = 1,
        explanation = "Ein Handelsbilanzueberschuss entsteht, wenn ein Land mehr Waren und Dienstleistungen exportiert als importiert. Deutschland hat traditionell einen der groessten Handelsbilanzueberschuesse der Welt.",
        difficulty = 2,
        funFact = "Deutschland war jahrelang Weltmeister beim Handelsbilanzueberschuss. Das fuehrt international manchmal zu Kritik — USA und EU werfen Deutschland vor, zu viel zu exportieren und zu wenig zu importieren."
    ),

    // Question 33 — Unternehmen: Samsung-Herkunft
    Question(
        categoryId = 11,
        questionText = "Aus welchem Land stammt der Technologiekonzern Samsung?",
        answerA = "Japan",
        answerB = "China",
        answerC = "Taiwan",
        answerD = "Suedkorea",
        correctAnswer = 3,
        explanation = "Samsung wurde 1938 in Suedkorea von Lee Byung-chul gegruendet. Der Konzern startete als Lebensmittelhandel und entwickelte sich zum weltweit groessten Hersteller von Smartphones und Halbleitern.",
        difficulty = 2,
        funFact = "Samsung bedeutet auf Koreanisch 'drei Sterne'. Der Gruender Lee Byung-chul waehlte diese Bedeutung, weil er sich etwas Grosses, Maechchtiges und Ewiges erhoffte."
    ),

    // Question 34 — Waehrung: Kryptoswaehrung Bitcoin
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde der Bitcoin erstmals als Zahlungsmittel eingesetzt?",
        answerA = "2007",
        answerB = "2009",
        answerC = "2011",
        answerD = "2013",
        correctAnswer = 1,
        explanation = "Bitcoin wurde 2009 von einer Person oder Gruppe unter dem Pseudonym Satoshi Nakamoto ins Leben gerufen. Am 3. Januar 2009 wurde der erste Bitcoin-Block (Genesis-Block) erzeugt. Die erste reale Transaktion folgte kurz darauf.",
        difficulty = 2,
        funFact = "Die erste bekannte Transaktion mit Bitcoin als echtem Zahlungsmittel fand im Mai 2010 statt: Laszlo Hanyecz bezahlte 10.000 Bitcoin fuer zwei Pizzen — heute waeren das Hunderte von Millionen Dollar."
    ),

    // Question 35 — Boerse: Risikostreuung
    Question(
        categoryId = 11,
        questionText = "Was bedeutet Diversifikation in der Geldanlage?",
        answerA = "Alles auf eine einzige Aktie setzen",
        answerB = "Nur in sichere Staatsanleihen investieren",
        answerC = "Das Kapital auf viele verschiedene Anlagen aufteilen",
        answerD = "Regelmaessig Aktien kaufen und sofort verkaufen",
        correctAnswer = 2,
        explanation = "Diversifikation bedeutet, das Kapital auf viele verschiedene Anlageklassen, Branchen und Laender aufzuteilen, um das Risiko zu streuen. Das Prinzip: 'Nicht alle Eier in einen Korb legen.'",
        difficulty = 2,
        funFact = "Der Nobelprestraeger Harry Markowitz entwickelte in den 1950ern die moderne Portfoliotheorie, die mathematisch beweist, dass Diversifikation das Risiko reduziert, ohne Rendite zu opfern."
    ),

    // Question 36 — Wirtschaft: Steuern
    Question(
        categoryId = 11,
        questionText = "Was ist der Unterschied zwischen direkten und indirekten Steuern?",
        answerA = "Direkte Steuern werden vom Staat, indirekte von den Gemeinden erhoben",
        answerB = "Direkte Steuern zahlt die Person selbst, indirekte werden ueber Preise weitergewillzt",
        answerC = "Direkte Steuern sind freiwillig, indirekte Pflicht",
        answerD = "Es gibt keinen Unterschied",
        correctAnswer = 1,
        explanation = "Direkte Steuern (z.B. Einkommensteuer) zahlt der Steuerpflichtige direkt an den Staat. Indirekte Steuern (z.B. Mehrwertsteuer) werden von Unternehmen erhoben und auf die Verbraucherpreise aufgeschlagen — der Kaeufer traegt sie, ohne es direkt zu merken.",
        difficulty = 2,
        funFact = "Die Mehrwertsteuer ist in Deutschland eine der wichtigsten Einnahmequellen des Staates. Sie macht rund 30 Prozent der gesamten Steuereinnahmen des Bundes aus."
    ),

    // Question 37 — Unternehmen: McDonald's-Gruendung
    Question(
        categoryId = 11,
        questionText = "Wer gilt als eigentlicher Gruender des weltweiten McDonald's-Franchisesystems?",
        answerA = "Die Brueder Richard und Maurice McDonald",
        answerB = "Ray Kroc",
        answerC = "Ronald McDonald",
        answerD = "Howard Johnson",
        correctAnswer = 1,
        explanation = "Ray Kroc gilt als der eigentliche Gruender des McDonald's-Konzerns, auch wenn die Brueder Richard und Maurice McDonald das erste Restaurant 1940 eroeffneten. Kroc kaufte 1961 die Markenrechte von den Bruederna und baute das weltweite Franchisesystem auf.",
        difficulty = 2,
        funFact = "Der Film 'The Founder' (2016) mit Michael Keaton zeigt, wie Ray Kroc die McDonald's-Brueder ueberlistete. Die Brueder erhielten eine Einmalzahlung von 2,7 Millionen Dollar, verzichteten aber auf die Franchise-Royalties — die sie zu Milliardaeren gemacht haetten."
    ),

    // Question 38 — Waehrung: Goldstandard
    Question(
        categoryId = 11,
        questionText = "Was war der Goldstandard?",
        answerA = "Ein System, bei dem Goldbarren als Zahlungsmittel dienten",
        answerB = "Ein System, bei dem der Wert einer Waehrung an Gold gekoppelt war",
        answerC = "Ein internationaler Vertrag ueber Goldpreise",
        answerD = "Die Bezeichnung fuer besonders stabile Waehrungen",
        correctAnswer = 1,
        explanation = "Beim Goldstandard war der Wert einer Waehrung direkt an eine bestimmte Menge Gold gebunden. Banken mussten Gold-Reserven in der Hoehe des im Umlauf befindlichen Geldes halten. Die USA verliessen den Goldstandard endgueltig 1971.",
        difficulty = 2,
        funFact = "Richard Nixon beendete den Goldstandard am 15. August 1971 mit einem Fernsehauftritt. Dieser Schritt wird als 'Nixon-Schock' bezeichnet und veraenderte das globale Waehrungssystem fundamental."
    ),

    // Question 39 — Boerse: Anleihe
    Question(
        categoryId = 11,
        questionText = "Was ist eine Staatsanleihe?",
        answerA = "Eine Aktie eines staatlichen Unternehmens",
        answerB = "Ein Kredit, den der Staat bei Investoren aufnimmt",
        answerC = "Ein staatliches Sparkonto",
        answerD = "Eine Versicherung gegen staatliche Insolvenz",
        correctAnswer = 1,
        explanation = "Eine Staatsanleihe ist ein Schuldtitel, mit dem ein Staat Geld von Investoren borgt. Der Staat verpflichtet sich, den geliehenen Betrag plus Zinsen zurueckzuzahlen. Staatsanleihen gelten in der Regel als relativ sicher.",
        difficulty = 2,
        funFact = "Deutschland gibt Staatsanleihen unter der Bezeichnung 'Bundesanleihen' aus. In Krisenzeiten sind sie so begehrt, dass die Zinsen manchmal negativ werden — Investoren zahlen also dafuer, Deutschland Geld zu leihen."
    ),

    // Question 40 — Wirtschaft: Monopol
    Question(
        categoryId = 11,
        questionText = "Was ist ein Monopol in der Wirtschaft?",
        answerA = "Wenn zwei Unternehmen einen Markt dominieren",
        answerB = "Wenn ein einziges Unternehmen einen Markt beherrscht",
        answerC = "Wenn der Staat alle Preise festlegt",
        answerD = "Wenn auslaendische Unternehmen einen Markt kontrollieren",
        correctAnswer = 1,
        explanation = "Ein Monopol besteht, wenn ein einziges Unternehmen den gesamten Markt fuer ein Gut oder eine Dienstleistung kontrolliert. Monopole sind in vielen Laendern reguliert oder verboten, weil sie Wettbewerb verhindern und Preise kuenstlich hochhalten koennen.",
        difficulty = 2,
        funFact = "Deutsche Bahn, Wasserversorger und der Netzbetreiber fuer Strom gelten als Beispiele fuer natuerliche Monopole, die staatlich reguliert werden muessen."
    ),

    // Question 41 — Unternehmen: Bayer-Gruendung
    Question(
        categoryId = 11,
        questionText = "In welcher deutschen Stadt wurde der Pharma- und Chemiekonzern Bayer gegruendet?",
        answerA = "Frankfurt",
        answerB = "Muenchen",
        answerC = "Wuppertal",
        answerD = "Hamburg",
        correctAnswer = 2,
        explanation = "Bayer wurde 1863 in Barmen gegruendet, das heute ein Stadtteil von Wuppertal ist. Das Unternehmen wurde von Friedrich Bayer und Johann Friedrich Weskott ins Leben gerufen und ist heute einer der groessten Pharmakonzerne der Welt.",
        difficulty = 2,
        funFact = "Bayer erfand 1897 Aspirin und 1900 Heroin — beide wurden als Arzneimittel vermarktet. Heroin wurde urspruenglich als Hustenmittel und Morphiumersatz vertrieben."
    ),

    // Question 42 — Waehrung: Brasilien
    Question(
        categoryId = 11,
        questionText = "Wie heisst die Waehrung Brasiliens?",
        answerA = "Peso",
        answerB = "Sol",
        answerC = "Real",
        answerD = "Cruzeiro",
        correctAnswer = 2,
        explanation = "Die Waehrung Brasiliens ist der Brasilianische Real (BRL). Er wurde 1994 eingefuehrt, um die Hyperinflation zu bekaempfen, die Brasilien zuvor jahrelang gelaehmmt hatte. Brasilien ist die groesste Volkswirtschaft Lateinamerikas.",
        difficulty = 2,
        funFact = "Brasilien hatte bis 1994 mit einer extremen Hyperinflation zu kaempfen. Im Jahr 1993 betrug die Jahresinflation ueber 2.000 Prozent. Die Einfuehrung des Real beendete diese Phase."
    ),

    // Question 43 — Boerse: Boersenkurs
    Question(
        categoryId = 11,
        questionText = "Was bestimmt in erster Linie den Kurs einer Aktie an der Boerse?",
        answerA = "Der Firmensitz des Unternehmens",
        answerB = "Die Anzahl der Mitarbeiter",
        answerC = "Angebot und Nachfrage der Marktteilnehmer",
        answerD = "Der Buchwert des Unternehmens",
        correctAnswer = 2,
        explanation = "Der Boersenkurs einer Aktie wird durch Angebot und Nachfrage bestimmt. Wollen viele Anleger eine Aktie kaufen, steigt der Kurs. Wollen viele verkaufen, faellt er. Dabei spielen Gewinnerwartungen, Nachrichten und das allgemeine Marktumfeld eine Rolle.",
        difficulty = 2
    ),

    // Question 44 — Wirtschaft: Freihandel
    Question(
        categoryId = 11,
        questionText = "Was ist ein Freihandelsabkommen?",
        answerA = "Ein Abkommen, bei dem Waren kostenlos geliefert werden",
        answerB = "Ein Vertrag zwischen Laendern zum Abbau von Handelsschranken",
        answerC = "Eine internationale Vereinbarung ueber Mindestloehne",
        answerD = "Ein Abkommen ueber kostenlose Nutzung von Rohstoffen",
        correctAnswer = 1,
        explanation = "Ein Freihandelsabkommen ist ein Vertrag zwischen Laendern, bei dem Zollschranken, Einfuhrquoten und andere Handelshemmnisse abgebaut werden. Ziel ist es, den gegenseitigen Handel zu foerdern und guenstigere Preise fuer Verbraucher zu ermoeglichen.",
        difficulty = 2,
        funFact = "Die EU ist die groesste Freihandelszone der Welt. Innerhalb der EU gibt es keine Zollgrenzen — Waren koennen frei und kostenlos zwischen allen 27 Mitgliedsstaaten transportiert werden."
    ),

    // Question 45 — Unternehmen: Bosch-Gruendung
    Question(
        categoryId = 11,
        questionText = "In welcher deutschen Stadt gruendete Robert Bosch 1886 seine Werkstatt?",
        answerA = "Muenchen",
        answerB = "Stuttgart",
        answerC = "Nuernberg",
        answerD = "Mannheim",
        correctAnswer = 1,
        explanation = "Robert Bosch gruendete am 15. November 1886 die 'Werkstaette fuer Feinmechanik und Elektrotechnik' in Stuttgart. Das Unternehmen ist heute einer der groessten Automobilzulieferer und Technologiekonzerne der Welt.",
        difficulty = 2,
        funFact = "Robert Bosch war bekannt fuer seine fortschrittliche Unternehmenskultur. Er verkuerzte 1906 freiwillig den Arbeitstag seiner Mitarbeiter auf acht Stunden — zu einer Zeit, als 10-12 Stunden normal waren."
    ),

    // Question 46 — Waehrung: Euro-Laender
    Question(
        categoryId = 11,
        questionText = "Wie viele Laender in der EU verwenden den Euro als offizielle Waehrung (Stand 2024)?",
        answerA = "17",
        answerB = "19",
        answerC = "20",
        answerD = "23",
        correctAnswer = 2,
        explanation = "Seit Januar 2023 verwenden 20 EU-Mitgliedsstaaten den Euro als offizielle Waehrung (Kroatien trat der Eurozone am 1. Januar 2023 bei). Diese Laender bilden die Eurozone.",
        difficulty = 2,
        funFact = "Nicht alle EU-Laender haben den Euro. Schweden, Daenemark, Polen, Ungarn und andere haben den Euro zwar theoretisch zu uebernehmen verpflichtet, aber noch kein konkretes Datum festgelegt."
    ),

    // Question 47 — Boerse: Risikoklassen
    Question(
        categoryId = 11,
        questionText = "Welche der folgenden Anlagen gilt im Allgemeinen als am sichersten?",
        answerA = "Aktien wachstumsstarker Start-ups",
        answerB = "Kryptoswaehrungen",
        answerC = "Deutsche Staatsanleihen",
        answerD = "Rohstoff-Futures",
        correctAnswer = 2,
        explanation = "Deutsche Staatsanleihen (Bundesanleihen) gelten als eine der sichersten Anlagen weltweit, weil das Ausfallrisiko des deutschen Staates als extrem gering eingestuft wird. Im Gegenzug ist die Rendite meist niedrig.",
        difficulty = 2
    ),

    // Question 48 — Wirtschaft: Arbeitslosenquote
    Question(
        categoryId = 11,
        questionText = "Was misst die Arbeitslosenquote?",
        answerA = "Den Anteil der Arbeitslosen an der Gesamtbevoelkerung",
        answerB = "Den Anteil der Arbeitslosen an allen Erwerbspersonen",
        answerC = "Die Anzahl offener Stellen im Verhaeltnis zu Arbeitslosen",
        answerD = "Den durchschnittlichen Lohn aller Arbeitnehmer",
        correctAnswer = 1,
        explanation = "Die Arbeitslosenquote gibt den Anteil der Arbeitslosen an allen Erwerbspersonen (Beschaeftigte + Arbeitslose) an. Sie ist ein wichtiger Indikator fuer die wirtschaftliche Lage eines Landes.",
        difficulty = 2,
        funFact = "Die Bundesagentur fuer Arbeit unterscheidet zwischen der 'registrierten Arbeitslosen' (gemeldet beim Jobcenter) und der 'Unterbeschaeftigung', die auch Menschen umfasst, die stundenweise arbeiten, aber mehr Arbeit suchen."
    ),

    // Question 49 — Unternehmen: Adidas und Puma
    Question(
        categoryId = 11,
        questionText = "Was haben Adidas und Puma gemeinsam?",
        answerA = "Sie wurden beide in Bayern gegruendet und gehen auf Bruedestreit zurueck",
        answerB = "Sie sind Toechterunternehmen von Nike",
        answerC = "Beide wurden in Stuttgart gegruendet",
        answerD = "Beide sind japanische Unternehmen",
        correctAnswer = 0,
        explanation = "Adidas und Puma wurden beide in Herzogenaurach in Bayern gegruendet und entstanden aus dem Streit der Brueder Adolf und Rudolf Dassler. Adolf gruendete Adidas (aus Adi Dassler), Rudolf gruendete Puma.",
        difficulty = 2,
        funFact = "Der Streit der Brueder teilte das ganze Staedtchen Herzogenaurach in zwei Lager. Man schaute zuerst auf die Schuhe eines Fremden, bevor man mit ihm sprach. Die Fehde dauerte bis zu ihren Toden an."
    ),

    // Question 50 — Waehrung: Erste Papiergeldwaehrung
    Question(
        categoryId = 11,
        questionText = "Welches Land verwendete als erstes Papiergeld?",
        answerA = "Aegypten",
        answerB = "Griechenland",
        answerC = "China",
        answerD = "Persien",
        correctAnswer = 2,
        explanation = "China war das erste Land, das Papiergeld verwendete. Bereits in der Tang-Dynastie (7.-10. Jahrhundert) gab es Vorlaeuferdokumente, und in der Song-Dynastie (10.-13. Jahrhundert) wurde offizielles Papiergeld namens 'Jiaozi' ausgegeben.",
        difficulty = 2,
        funFact = "Marco Polo beschrieb im 13. Jahrhundert das chinesische Papiergeld mit Staunen. In Europa dauerte es noch Jahrhunderte, bis sich Papiergeld durchsetzte — die erste europaeische Banknote wurde 1661 in Schweden ausgegeben."
    )

)
