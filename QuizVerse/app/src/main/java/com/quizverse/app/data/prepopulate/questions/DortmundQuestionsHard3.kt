package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun dortmundQuestionsHard3(): List<Question> = listOf(

    // --- BVB Finanzkrise 2003–2005 ---

    Question(
        categoryId = 14,
        questionText = "Auf wie viel Millionen Euro belief sich die Gesamtverschuldung des BVB auf dem Höhepunkt der Finanzkrise im Jahr 2003?",
        answerA = "ca. 80 Mio. Euro",
        answerB = "ca. 118 Mio. Euro",
        answerC = "ca. 220 Mio. Euro",
        answerD = "ca. 55 Mio. Euro",
        correctAnswer = 1,
        explanation = "Auf dem Höhepunkt der Krise 2003 hatte der BVB Verbindlichkeiten von rund 118–120 Millionen Euro angehäuft, was die Existenz des Klubs ernsthaft bedrohte.",
        difficulty = 3,
        funFact = "Der BVB stand so nah vor der Insolvenz, dass eine Notlösung nur durch die Unterstützung der Commerzbank und einen Notfallkredit abgewendet werden konnte."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr ging der BVB als erstes börsennotiertes deutsches Fußballunternehmen an die Frankfurter Wertpapierbörse?",
        answerA = "1998",
        answerB = "2000",
        answerC = "2002",
        answerD = "1996",
        correctAnswer = 0,
        explanation = "Der BVB ging am 31. Oktober 2000 an die Frankfurter Börse, war damit aber nicht der erste – der Gang fand 2000 statt. Tatsächlich war der BVB 2000 das erste deutsche Fußball-Unternehmen, das an die Börse gegangen ist.",
        difficulty = 3,
        funFact = "Der Ausgabepreis der BVB-Aktie beim Börsengang betrug 11 Euro pro Stück – auf dem Tiefpunkt der Krise fiel sie auf unter 1 Euro."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Bank gewährte dem BVB im Jahr 2002/2003 einen entscheidenden Notfallkredit, der den Konkurs abwendete?",
        answerA = "Deutsche Bank",
        answerB = "Dresdner Bank",
        answerC = "Commerzbank",
        answerD = "HypoVereinsbank",
        correctAnswer = 2,
        explanation = "Die Commerzbank stellte dem BVB einen Überbrückungskredit in Höhe von rund 25 Millionen Euro zur Verfügung und verhinderte so die drohende Insolvenz des Klubs.",
        difficulty = 3,
        funFact = "Ohne diesen Kredit hätte der BVB möglicherweise nicht an der Bundesligasaison 2003/04 teilnehmen können."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Finanzierungsmodell nutzte der BVB 2003, um dringend benötigte Liquidität zu beschaffen, indem er das Stadiondach 'verkaufte'?",
        answerA = "Sale-and-leaseback-Transaktion",
        answerB = "Schuldverschreibung",
        answerC = "Hypothekenkredit",
        answerD = "Wandelanleihe",
        correctAnswer = 0,
        explanation = "Der BVB verkaufte das Stadiondach im Rahmen eines Sale-and-leaseback-Verfahrens: Der Käufer erwarb das Dach, der BVB mietete es zurück und erhielt sofort Kapital, ohne das Stadion physisch aufgeben zu müssen.",
        difficulty = 3,
        funFact = "Dieses ungewöhnliche Finanzierungsmodell brachte dem BVB rund 75 Millionen Euro ein und war eine der kreativsten Notlösungen in der Geschichte des deutschen Fußballs."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Manager war während der schlimmsten Phase der Finanzkrise 2003/2004 als Geschäftsführer verantwortlich und musste den Sanierungsplan umsetzen?",
        answerA = "Michael Zorc",
        answerB = "Gerd Niebaum",
        answerC = "Hans-Joachim Watzke",
        answerD = "Thomas Treß",
        correctAnswer = 2,
        explanation = "Hans-Joachim Watzke übernahm im Jahr 2005 als Geschäftsführer die Leitung des BVB und führte den Klub durch die Sanierungsphase. Er gilt als derjenige, der den Verein vor dem endgültigen Ruin gerettet hat.",
        difficulty = 3,
        funFact = "Watzke musste Spielergehälter stunden und verhandelte persönlich mit Banken und Gläubigern, um den BVB zu retten."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Präsidenten-Duo trat infolge der Finanzkrise 2004 beim BVB zurück?",
        answerA = "Reinhard Rauball und Michael Zorc",
        answerB = "Gerd Niebaum und Michael Meier",
        answerC = "Hans-Joachim Watzke und Aki Schmidt",
        answerD = "Wilfried Sauerland und Ernst Lehmann",
        correctAnswer = 1,
        explanation = "Präsident Gerd Niebaum und Geschäftsführer Michael Meier traten im Jahr 2004 zurück. Sie standen in der Kritik, durch übermäßige Ausgaben in der Champions-League-Ära die Finanzkrise verursacht zu haben.",
        difficulty = 3,
        funFact = "Niebaum hatte den BVB in den 1990ern zu einem europäischen Spitzenklub geformt – doch die teuren Spielerverpflichtungen nach dem Champions-League-Sieg 1997 wurden zum Verhängnis."
    ),

    Question(
        categoryId = 14,
        questionText = "Um wie viel Prozent musste der BVB laut Sanierungsplan von 2005 seine Personalkosten senken?",
        answerA = "ca. 10 %",
        answerB = "ca. 25 %",
        answerC = "ca. 40 %",
        answerD = "ca. 55 %",
        correctAnswer = 2,
        explanation = "Der BVB musste seine Personalkosten um rund 40 % reduzieren. Das bedeutete den Abgang zahlreicher hochbezahlter Spieler und drastische Gehaltskürzungen im gesamten Verein.",
        difficulty = 3,
        funFact = "Einige Spieler verzichteten freiwillig auf Gehaltsteile, um dem Klub zu helfen – ein in der Bundesliga einzigartiger Akt der Solidarität."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Anleihe emittierte der BVB im Jahr 2012, um sich langfristig zu refinanzieren und von Bankkrediten unabhängiger zu werden?",
        answerA = "Unternehmensanleihe über 50 Mio. Euro",
        answerB = "Fananleihe über 75 Mio. Euro",
        answerC = "Bundesanleihe über 30 Mio. Euro",
        answerD = "Wandelanleihe über 100 Mio. Euro",
        correctAnswer = 1,
        explanation = "Der BVB emittierte 2012 eine Fananleihe (offiziell: Unternehmensanleihe) über 75 Millionen Euro mit einem Kupon von 5,5 %, die von Fans und Investoren gezeichnet wurde.",
        difficulty = 3,
        funFact = "Die Anleihe war so erfolgreich, dass sie innerhalb kürzester Zeit überzeichnet war – ein Zeichen dafür, wie sehr die Fans dem Klub nach den Krisenjahren vertrauten."
    ),

    // --- Stadiongeschichte ---

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wurde das Westfalenstadion ursprünglich eröffnet?",
        answerA = "1965",
        answerB = "1974",
        answerC = "1971",
        answerD = "1969",
        correctAnswer = 1,
        explanation = "Das Westfalenstadion wurde am 2. April 1974 eingeweiht. Es war als eine der Spielstätten für die Fußball-Weltmeisterschaft 1974 in Deutschland geplant und gebaut worden.",
        difficulty = 3,
        funFact = "Das Eröffnungsspiel fand am 2. April 1974 zwischen dem BVB und Schalke 04 statt – BVB gewann 3:0 vor 54.000 Zuschauern."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viel kostete der Bau des ursprünglichen Westfalenstadions (1971–1974) in DM?",
        answerA = "ca. 25 Mio. DM",
        answerB = "ca. 50 Mio. DM",
        answerC = "ca. 75 Mio. DM",
        answerD = "ca. 100 Mio. DM",
        correctAnswer = 1,
        explanation = "Der Bau des Westfalenstadions kostete rund 50 Millionen Deutsche Mark. Die Kosten wurden hauptsächlich von der Stadt Dortmund getragen, da das Stadion als Bundesligazentrum und WM-Spielort geplant war.",
        difficulty = 3,
        funFact = "Zum Vergleich: Der Neubau der Allianz Arena in München kostete 2005 rund 340 Millionen Euro."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Kapazität hatte das Westfalenstadion bei seiner Eröffnung 1974?",
        answerA = "42.000",
        answerB = "54.000",
        answerC = "62.000",
        answerD = "80.000",
        correctAnswer = 1,
        explanation = "Das Westfalenstadion fasste bei der Eröffnung 1974 rund 54.000 Zuschauer, davon ausschließlich Stehplätze auf den Rängen außer den überdachten Sitzplatzbereichen.",
        difficulty = 3,
        funFact = "Nach mehreren Ausbauphasen fasst das Stadion heute als Signal Iduna Park 81.365 Zuschauer und ist damit das größte Fußballstadion Deutschlands."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wurde das Westfalenstadion erstmals in 'Signal Iduna Park' umbenannt?",
        answerA = "2002",
        answerB = "2005",
        answerC = "2008",
        answerD = "2011",
        correctAnswer = 1,
        explanation = "Das Westfalenstadion wurde im Jahr 2005 in 'Signal Iduna Park' umbenannt, nachdem der BVB einen Namensrechtevertrag mit der Versicherungsgesellschaft Signal Iduna abgeschlossen hatte.",
        difficulty = 3,
        funFact = "Trotz der offiziellen Umbenennung nennen viele Dortmunder Fans das Stadion noch immer liebevoll 'Westfalenstadion'."
    ),

    Question(
        categoryId = 14,
        questionText = "Auf welche Kapazität wurde das Westfalenstadion für die WM 2006 ausgebaut?",
        answerA = "67.000",
        answerB = "74.000",
        answerC = "80.552",
        answerD = "83.000",
        correctAnswer = 2,
        explanation = "Für die Fußball-WM 2006 wurde das Stadion auf eine Kapazität von 80.552 Plätzen (bei reiner Sitzplatzbestuhlung) ausgebaut. Im Ligabetrieb mit Stehplätzen auf der Südtribüne liegt die Kapazität bei 81.365.",
        difficulty = 3,
        funFact = "Während der WM 2006 wurden im Signal Iduna Park unter anderem die Gruppenspiele sowie ein Viertelfinale ausgetragen."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Sitzplätze fasst die berühmte Südtribüne des Signal Iduna Parks im DFB-Pokalbetrieb (ohne Stehplätze)?",
        answerA = "ca. 10.000",
        answerB = "ca. 20.000",
        answerC = "ca. 25.000",
        answerD = "ca. 15.000",
        correctAnswer = 3,
        explanation = "Im reinen Sitzplatzbetrieb (z.B. bei internationalen Spielen oder DFB-Pokal) werden auf der Südtribüne rund 15.000 Sitzplätze aufgestellt. Als Stehplatztribüne bietet sie Platz für rund 24.454 Fans.",
        difficulty = 3,
        funFact = "Die Südtribüne des Signal Iduna Parks gilt als größte Stehplatztribüne Europas und erzeugt eine einzigartige Atmosphäre, die weltweit bekannt ist."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wurde die Südtribüne (Stehplatz-Haupttribüne) des Westfalenstadions überdacht?",
        answerA = "1992",
        answerB = "1995",
        answerC = "1999",
        answerD = "2003",
        correctAnswer = 0,
        explanation = "Die Südtribüne wurde 1992 überdacht und im Zuge dieser Baumaßnahme modernisiert. Die Überdachung verbesserte die Atmosphäre erheblich, da der Schall nun im Stadion gehalten wurde.",
        difficulty = 3,
        funFact = "Seitdem gilt die Südtribüne als 'Gelbe Wand' – ein Begriff, der weltweit für die Fankultur des BVB steht."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Unternehmen fungiert seit 2005 als Namenssponsor des Signal Iduna Parks und hat seinen Sitz ebenfalls in Dortmund?",
        answerA = "DEVK Versicherungen",
        answerB = "Westfälische Provinzial",
        answerC = "Signal Iduna Gruppe",
        answerD = "R+V Versicherung",
        correctAnswer = 2,
        explanation = "Die Signal Iduna Gruppe, ein großer Versicherungskonzern mit Hauptsitz in Dortmund und Hamburg, ist seit 2005 Namenssponsor. Der Vertrag wurde seither mehrfach verlängert.",
        difficulty = 3,
        funFact = "Es ist eine seltene Konstellation, dass Verein und Namenssponsor beide in derselben Stadt beheimatet sind – ein Zeichen der tiefen regionalen Verbundenheit."
    ),

    // --- BVB-Spieler als Trainer/Manager ---

    Question(
        categoryId = 14,
        questionText = "Welcher ehemalige BVB-Torhüter wurde später Cheftrainer bei der deutschen Nationalmannschaft der Frauen?",
        answerA = "Stefan Klos",
        answerB = "Wolfgang de Beer",
        answerC = "Stefan Reuter",
        answerD = "Horst Köppel",
        correctAnswer = 3,
        explanation = "Horst Köppel spielte für den BVB und trainierte später unter anderem die deutsche Frauen-Nationalmannschaft sowie verschiedene Bundesligavereine als Cheftrainer.",
        difficulty = 3,
        funFact = "Köppel war in den 1970ern ein erfolgreicher Bundesligaspieler und machte dann eine erfolgreiche Karriere als Trainer."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher langjährige BVB-Kapitän der 1990er/2000er-Jahre übernahm nach seiner Spielerkarriere das Amt des Sportdirektors beim BVB?",
        answerA = "Karl-Heinz Riedle",
        answerB = "Stefan Reuter",
        answerC = "Michael Zorc",
        answerD = "Jürgen Kohler",
        correctAnswer = 2,
        explanation = "Michael Zorc war von 1981 bis 1998 Spieler beim BVB und ist seit 1998 Sportdirektor des Klubs. Er gilt als eine der prägendsten Figuren in der Geschichte des BVB.",
        difficulty = 3,
        funFact = "Zorc ist der am längsten dienende Sportdirektor in der Geschichte der Bundesliga und kennt den BVB wie kein anderer."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher ehemalige BVB-Stürmer trainierte später Energie Cottbus und den 1. FC Nürnberg in der Bundesliga?",
        answerA = "Flemming Povlsen",
        answerB = "Frank Mill",
        answerC = "Stéphane Chapuisat",
        answerD = "Dieter Eilts",
        correctAnswer = 1,
        explanation = "Frank Mill war ein erfolgreicher BVB-Stürmer und arbeitete nach seiner Spielerkarriere als Trainer, unter anderem bei Energie Cottbus und später bei weiteren deutschen Klubs.",
        difficulty = 3,
        funFact = "Frank Mill war an der Europameisterschaft 1988 beteiligt und erzielte zahlreiche wichtige Tore für den BVB in den 1980er Jahren."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher frühere BVB-Verteidiger und Weltmeister 1990 wurde nach seiner Karriere Chefscout und Berater bei verschiedenen Bundesligavereinen?",
        answerA = "Jürgen Kohler",
        answerB = "Wolfgang Patzke",
        answerC = "Dieter Burdenski",
        answerD = "Lothar Huber",
        correctAnswer = 0,
        explanation = "Jürgen Kohler, Weltmeister 1990 und langjähriger BVB-Innenverteidiger, war nach seiner Spielerkarriere als Trainer tätig, unter anderem beim VfB Stuttgart als Co-Trainer und später in leitenden Funktionen.",
        difficulty = 3,
        funFact = "Kohler galt als einer der weltbesten Innenverteidiger seiner Zeit und war ein zentraler Baustein in den BVB-Meisterschaftsteams der 1990er."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher legendäre BVB-Trainer der 1970er war zuvor selbst als Spieler für den Verein aktiv?",
        answerA = "Udo Lattek",
        answerB = "Otto Rehhagel",
        answerC = "Hermann Eppenhoff",
        answerD = "Helmut Schneider",
        correctAnswer = 3,
        explanation = "Helmut Schneider spielte für den BVB und trainierte den Verein auch. Er war eine wichtige Figur im Klub in den 1960er/70er Jahren als Spieler und später in Trainerfunktionen.",
        difficulty = 3,
        funFact = "Die Tradition, eigene Ex-Spieler in Schlüsselpositionen einzusetzen, zieht sich durch die gesamte BVB-Geschichte."
    ),

    // --- UEFA-Cup / Europa-League-Kampagnen ---

    Question(
        categoryId = 14,
        questionText = "In welcher Saison erreichte der BVB das Finale des UEFA-Pokals und verlor gegen Juventus Turin?",
        answerA = "Saison 1992/93",
        answerB = "Saison 1993/94",
        answerC = "Saison 1994/95",
        answerD = "Saison 1995/96",
        correctAnswer = 1,
        explanation = "In der Saison 1992/93 erreichte der BVB das Finale des UEFA-Pokals. Im Finale trafen sie jedoch auf Juventus Turin und verloren das Rückspiel, nachdem das Hinspiel 1:3 endete. Das Finale der Saison 1992/93 verloren sie mit 1:6 im Aggregat.",
        difficulty = 3,
        funFact = "Trotz der Niederlage war das UEFA-Pokal-Finale 1993 ein Meilenstein in der europäischen Entwicklung des BVB."
    ),

    Question(
        categoryId = 14,
        questionText = "Gegen welchen spanischen Verein schied der BVB in der Saison 2013/14 im Achtelfinale der Europa League aus?",
        answerA = "Athletic Bilbao",
        answerB = "Sevilla FC",
        answerC = "FC Zaragoza",
        answerD = "Real Sociedad",
        correctAnswer = 0,
        explanation = "In der Saison 2013/14 schied der BVB in der Europa League im Achtelfinale gegen Athletic Bilbao aus. Das Rückspiel im Signal Iduna Park endete 2:1 für den BVB, reichte aber nicht für das Weiterkommen.",
        difficulty = 3,
        funFact = "Athletic Bilbao ist einer der wenigen Klubs weltweit, der ausschließlich Spieler aus dem Baskenland einsetzt."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr gewann der BVB den UEFA-Pokal und gegen wen spielten sie im Finale?",
        answerA = "1997 gegen Ajax Amsterdam",
        answerB = "2002 gegen Feyenoord Rotterdam",
        answerC = "1993 gegen Juventus Turin",
        answerD = "BVB hat den UEFA-Pokal nie gewonnen",
        correctAnswer = 3,
        explanation = "Der BVB hat den UEFA-Pokal bzw. die Europa League nie gewonnen. Der größte europäische Titelgewinn war der Champions-League-Sieg 1997 in München gegen Juventus Turin.",
        difficulty = 3,
        funFact = "Trotzdem spielte der BVB in mehreren wichtigen europäischen Wettbewerben eine bedeutende Rolle und prägte den Kontinentalfußball in den 1990ern maßgeblich."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Klub besiegte der BVB im UEFA-Pokal 1992/93 im Halbfinale, um ins Finale einzuziehen?",
        answerA = "Werder Bremen",
        answerB = "AS Roma",
        answerC = "RFC Lüttich",
        answerD = "Karlsruher SC",
        correctAnswer = 1,
        explanation = "Im Halbfinale des UEFA-Pokals 1992/93 besiegte der BVB AS Roma und zog damit erstmals in ein europäisches Pokalfinale ein.",
        difficulty = 3,
        funFact = "Dieser Halbfinalerfolg gegen AS Roma war ein wichtiger Schritt auf dem Weg des BVB zur europäischen Spitzenklasse."
    ),

    Question(
        categoryId = 14,
        questionText = "In der Europa-League-Saison 2014/15 erreichte der BVB die Runde der letzten 16. Gegen welchen ukrainischen Verein trat der BVB in der Gruppenphase an?",
        answerA = "Schachtar Donezk",
        answerB = "Dynamo Kiew",
        answerC = "Metalist Charkiw",
        answerD = "FC Dnipro Dnipropetrowsk",
        correctAnswer = 0,
        explanation = "In der Gruppenphase der Europa League 2014/15 spielte der BVB u.a. gegen Schachtar Donezk sowie Galatasaray und Anderlecht.",
        difficulty = 3,
        funFact = "Die Saison 2014/15 war für den BVB unter Jürgen Klopp eine schwierige Zeit – trotz zwischenzeitlicher Abstiegsangst rettete sich der Klub und machte den Weg frei für den Neuanfang."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem europäischen Wettbewerb trat der BVB in der Saison 2002/03 an und erreichte dabei die Gruppenphase?",
        answerA = "Champions League",
        answerB = "UEFA-Pokal",
        answerC = "UEFA-Intertoto-Cup",
        answerD = "UEFA Super Cup",
        correctAnswer = 1,
        explanation = "In der Saison 2002/03 nahm der BVB am UEFA-Pokal teil. Dies war kurz vor der schlimmsten Phase der Finanzkrise – der Verein konnte sich keine Champions-League-Qualifikation sichern.",
        difficulty = 3,
        funFact = "In dieser Zeit musste der BVB ständig zwischen sportlichem Anspruch und finanzieller Not abwägen."
    ),

    // --- BVB Supercup-Geschichte ---

    Question(
        categoryId = 14,
        questionText = "Gegen welchen Verein bestritt der BVB seinen ersten deutschen Supercup im Jahr 1989?",
        answerA = "Werder Bremen",
        answerB = "Bayern München",
        answerC = "Eintracht Frankfurt",
        answerD = "1. FC Köln",
        correctAnswer = 1,
        explanation = "Der BVB bestritt 1989 den DFL-Supercup gegen Bayern München. Dieses Spiel gilt als eine der frühen Begegnungen in der modernen Supercup-Geschichte des deutschen Fußballs.",
        difficulty = 3,
        funFact = "Der DFL-Supercup (früher: DFB-Supercup) wird seit 1987 ausgetragen und ist das erste Pflichtspiel einer neuen Bundesligasaison."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr gewann der BVB zum ersten Mal den DFL-Supercup nach der Wiedervereinigung der modernen Supercup-Ära?",
        answerA = "1989",
        answerB = "1995",
        answerC = "1996",
        answerD = "2002",
        correctAnswer = 2,
        explanation = "Der BVB gewann den DFL-Supercup 1996 und etablierte sich damit als einer der führenden Klubs des deutschen Fußballs in dieser Ära.",
        difficulty = 3,
        funFact = "Der BVB war in den 1990ern so dominant, dass er mehrfach sowohl den Supercup als auch die Bundesliga gewann."
    ),

    Question(
        categoryId = 14,
        questionText = "Mit welchem Ergebnis gewann der BVB den DFL-Supercup 2011 gegen Bayern München?",
        answerA = "2:0",
        answerB = "3:1",
        answerC = "5:0",
        answerD = "1:0",
        correctAnswer = 2,
        explanation = "Im DFL-Supercup 2011 besiegte der BVB Bayern München mit 5:0 – eines der deutlichsten Ergebnisse in der Geschichte des deutschen Supercups.",
        difficulty = 3,
        funFact = "Dieses 5:0 war ein Vorgeschmack auf die Dominanz des BVB in der Saison 2011/12, in der sie die Bundesliga mit beeindruckenden Leistungen gewannen."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele DFL-Supercups hat der BVB insgesamt in seiner Geschichte gewonnen (Stand 2024)?",
        answerA = "3",
        answerB = "5",
        answerC = "7",
        answerD = "9",
        correctAnswer = 1,
        explanation = "Der BVB hat insgesamt 5 DFL-Supercups gewonnen: 1989, 1995, 1996, 2013 und 2019.",
        difficulty = 3,
        funFact = "Damit liegt der BVB in der ewigen Supercup-Siegerliste hinter Bayern München, das diesen Wettbewerb am häufigsten gewann."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer schoss im DFL-Supercup 2013 gegen Bayern München das entscheidende Tor für den BVB?",
        answerA = "Robert Lewandowski",
        answerB = "Marco Reus",
        answerC = "Kevin Großkreutz",
        answerD = "Henrikh Mkhitaryan",
        correctAnswer = 0,
        explanation = "Robert Lewandowski traf im DFL-Supercup 2013 für den BVB, der das Spiel gewann. Lewandowski war in dieser Phase einer der torgefährlichsten Stürmer Europas.",
        difficulty = 3,
        funFact = "Im selben Jahr erzielte Lewandowski in der Champions League vier Tore gegen Real Madrid in einem einzigen Halbfinale."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Stadion fand der DFL-Supercup 2019 zwischen BVB und Bayern München statt, den der BVB gewann?",
        answerA = "Allianz Arena München",
        answerB = "Signal Iduna Park Dortmund",
        answerC = "Mercedes-Benz Arena Stuttgart",
        answerD = "VELTINS-Arena Gelsenkirchen",
        correctAnswer = 1,
        explanation = "Der DFL-Supercup 2019 fand im Signal Iduna Park in Dortmund statt. Der BVB gewann das Spiel mit 2:0 gegen Bayern München.",
        difficulty = 3,
        funFact = "Torschützen beim Supercup 2019 waren Marco Reus und Alcácer – der BVB dominierte Bayern an diesem Tag von Beginn an."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Ergebnis erzielte der BVB im DFL-Supercup 2019 gegen Bayern München?",
        answerA = "1:0",
        answerB = "2:0",
        answerC = "3:2",
        answerD = "2:1",
        correctAnswer = 1,
        explanation = "Im DFL-Supercup 2019 besiegte der BVB Bayern München mit 2:0 im Signal Iduna Park. Es war ein klarer Erfolg für die Mannschaft von Trainer Lucien Favre.",
        difficulty = 3,
        funFact = "Es war der erste Titel für den BVB unter Trainer Lucien Favre und gab dem Team Selbstvertrauen für die Bundesligasaison 2019/20."
    ),

    Question(
        categoryId = 14,
        questionText = "Gegen welchen Gegner verlor der BVB den UEFA Super Cup 1997, kurz nach dem Champions-League-Sieg?",
        answerA = "FC Barcelona",
        answerB = "Paris Saint-Germain",
        answerC = "Ajax Amsterdam",
        answerD = "AC Mailand",
        correctAnswer = 1,
        explanation = "Im UEFA Super Cup 1997 traf der BVB als Champions-League-Sieger auf den UEFA-Pokal-Sieger Paris Saint-Germain. Der BVB verlor diesen Supercup.",
        difficulty = 3,
        funFact = "Der UEFA Super Cup 1997 war eines der wenigen Spiele, in denen der BVB in diesem triumphalen Jahr als Verlierer vom Platz ging."
    ),

    // --- Weitere HARD-Fragen zur Vereinsgeschichte ---

    Question(
        categoryId = 14,
        questionText = "Welches war das erste offizielle Meisterschaftsspiel des BVB in der Bundesliga-Geschichte?",
        answerA = "BVB vs. Werder Bremen am 24.08.1963",
        answerB = "BVB vs. 1. FC Köln am 24.08.1963",
        answerC = "Hamburger SV vs. BVB am 24.08.1963",
        answerD = "BVB vs. Eintracht Frankfurt am 25.08.1963",
        correctAnswer = 2,
        explanation = "Das erste Bundesligaspiel des BVB fand am 24. August 1963 statt. Der BVB verlor auswärts beim Hamburger SV mit 1:3.",
        difficulty = 3,
        funFact = "Dieses erste Bundesligaspiel markierte den Start einer Reise, die den BVB zu einem der größten Klubs Europas machen sollte."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wurde Borussia Dortmund offiziell als Verein gegründet?",
        answerA = "1906",
        answerB = "1909",
        answerC = "1912",
        answerD = "1900",
        correctAnswer = 1,
        explanation = "Borussia Dortmund wurde am 19. Dezember 1909 gegründet. Die Gründer waren hauptsächlich junge Männer aus der Arbeiterpfarrei St. Johannes Baptist.",
        difficulty = 3,
        funFact = "Die Gründer benannten den Verein 'Borussia' nach der Borussia-Brauerei, die sich in der Nähe befand."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie hieß der legendäre BVB-Trainer, der in den 1950ern zwei Deutsche Meisterschaften in Folge mit dem BVB gewann?",
        answerA = "Helmut Rahn",
        answerB = "Heinz Murach",
        answerC = "Karl Milller",
        answerD = "Georg Stollenwerk",
        correctAnswer = 1,
        explanation = "Heinz Murach trainierte den BVB zu Deutschen Meisterschaften in den 1950er Jahren. Er war eine der prägenden Trainerfiguren in der frühen BVB-Geschichte.",
        difficulty = 3,
        funFact = "Die BVB-Meisterschaften der 1950er Jahre legten den Grundstein für die Tradition des Klubs als erfolgreicher Titelsammler."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler erzielte im Champions-League-Finale 1997 gegen Juventus Turin den spielentscheidenden zweiten Treffer?",
        answerA = "Lars Ricken",
        answerB = "Karl-Heinz Riedle",
        answerC = "Andreas Möller",
        answerD = "Paulo Sousa",
        correctAnswer = 0,
        explanation = "Lars Ricken erzielte nach seiner Einwechslung in der 71. Spielminute mit seinem ersten Ballkontakt ein Traumtor zum 3:1-Endstand und wurde damit zur Ikone dieses Finales.",
        difficulty = 3,
        funFact = "Rickens Tor nur Sekunden nach seiner Einwechslung gilt als eines der ikonischsten Tore in der Geschichte der Champions League."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Trainer holte den Verein in den Saisons 2010/11 und 2011/12 zur Deutschen Meisterschaft?",
        answerA = "Thomas Tuchel",
        answerB = "Peter Bosz",
        answerC = "Jürgen Klopp",
        answerD = "Ottmar Hitzfeld",
        correctAnswer = 2,
        explanation = "Jürgen Klopp trainierte den BVB von 2008 bis 2015 und holte in den Saisons 2010/11 und 2011/12 zweimal in Folge die Deutsche Meisterschaft.",
        difficulty = 3,
        funFact = "Klopps BVB 2011/12 erzielte mit 81 Punkten einen neuen Bundesliga-Rekord und ließ Bayern München mit 25 Punkten Abstand hinter sich."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore erzielte der BVB in der Meistersaison 2011/12 in der Bundesliga?",
        answerA = "74",
        answerB = "80",
        answerC = "91",
        answerD = "67",
        correctAnswer = 1,
        explanation = "In der Meistersaison 2011/12 erzielte der BVB 80 Tore in der Bundesliga und war damit die mit Abstand torreichste Mannschaft der Liga.",
        difficulty = 3,
        funFact = "Lewandowski allein steuerte 30 Tore bei – ein Wert, den nur wenige Stürmer in der Bundesligageschichte erreicht haben."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Saison erreichte der BVB das Finale der UEFA Champions League, das er im Wembley-Stadion gegen Bayern München verlor?",
        answerA = "2011/12",
        answerB = "2012/13",
        answerC = "2013/14",
        answerD = "2010/11",
        correctAnswer = 1,
        explanation = "In der Saison 2012/13 erreichte der BVB das Champions-League-Finale im Wembley-Stadion und verlor gegen Bayern München mit 1:2.",
        difficulty = 3,
        funFact = "Das Finale 2013 war das erste rein deutsche Champions-League-Finale der Geschichte und wurde als 'Finale dahoam' bezeichnet."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Ergebnis erzielte der BVB im Halbfinale der Champions League 2012/13 gegen Real Madrid im Hinspiel?",
        answerA = "1:0",
        answerB = "2:1",
        answerC = "4:1",
        answerD = "3:0",
        correctAnswer = 2,
        explanation = "Im Halbfinal-Hinspiel 2012/13 gewann der BVB mit 4:1 gegen Real Madrid im Signal Iduna Park – eines der beeindruckendsten Ergebnisse in der BVB-Champions-League-Geschichte.",
        difficulty = 3,
        funFact = "Robert Lewandowski erzielte alle vier BVB-Tore in diesem Spiel und schrieb damit Champions-League-Geschichte."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Spitznamen trägt der BVB offiziell als Teil seiner Vereinsidentität?",
        answerA = "Die Schwarz-Gelben",
        answerB = "Die Borussen",
        answerC = "BVB",
        answerD = "Der Echte Westen",
        correctAnswer = 3,
        explanation = "Der offizielle Slogan und Kampagnenname des BVB lautet 'Echter Liebe' – 'Der echte Westen' ist ein weiterer Identitätsbegriff. Der bekannteste Spitzname ist jedoch 'Die Schwarzgelben' oder 'BVB'.",
        difficulty = 3,
        funFact = "Das Marketingkonzept 'Echter Liebe' wurde vom BVB genutzt, um die enge Verbundenheit zwischen Klub und Fans zu unterstreichen."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr zog der BVB aus seinem ursprünglichen Heimstadion Rote Erde ins neue Westfalenstadion um?",
        answerA = "1970",
        answerB = "1974",
        answerC = "1978",
        answerD = "1965",
        correctAnswer = 1,
        explanation = "Der BVB zog 1974 ins neue Westfalenstadion um, das speziell für die Weltmeisterschaft 1974 gebaut worden war. Das Stadion Rote Erde steht noch heute als Nebenspielstätte.",
        difficulty = 3,
        funFact = "Das Stadion 'Rote Erde' erhielt seinen Namen wegen des roten Aschensandes, der früher auf dem Gelände abgebaut wurde."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Zuschauer fasst das Stadion 'Rote Erde', die historische Nebenspielstätte des BVB?",
        answerA = "ca. 5.500",
        answerB = "ca. 10.000",
        answerC = "ca. 25.000",
        answerD = "ca. 15.000",
        correctAnswer = 2,
        explanation = "Das Stadion Rote Erde fasst rund 25.000 Zuschauer und dient dem BVB als Nebenspielstätte sowie für Jugendspiele und kleinere Veranstaltungen.",
        difficulty = 3,
        funFact = "Das Rote Erde ist eines der ältesten noch genutzten Fußballstadien Deutschlands und steht unter Denkmalschutz."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Rekordtorschütze erzielte in 781 Pflichtspielen für den Verein 461 Tore?",
        answerA = "Michael Zorc",
        answerB = "Alfred Kelbassa",
        answerC = "August Lenz",
        answerD = "Manfred Burgsmüller",
        correctAnswer = 0,
        explanation = "Michael Zorc ist mit 463 Toren in 571 Pflichtspielen der Rekordtorschütze des BVB – je nach Quelle variieren die Zahlen leicht. Er spielte von 1981 bis 1998 für den BVB.",
        difficulty = 3,
        funFact = "Zorc wechselte nie zu einem anderen Verein und verbrachte seine gesamte Profikarriere beim BVB – eine absolute Seltenheit im modernen Fußball."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler wurde bei der Europameisterschaft 2000 von Deutschland als Torschütze im Turnierverlauf eingesetzt?",
        answerA = "Stefan Reuter",
        answerB = "Christian Wörns",
        answerC = "Jürgen Kohler",
        answerD = "Heiko Herrlich",
        correctAnswer = 1,
        explanation = "Christian Wörns war bei der EM 2000 Teil der deutschen Nationalmannschaft und spielte als BVB-Verteidiger. Deutschland schied in der Gruppenphase aus.",
        difficulty = 3,
        funFact = "Die EM 2000 war eines der schlechtesten Turniere der deutschen Nationalelf – sie schieden als erstes Team ohne Punkt und ohne Tor in der Gruppenphase aus."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches war das erste ausländische Finale, an dem ein BVB-Spieler als Torschütze teilnahm, bevor der BVB selbst 1997 Champion wurde?",
        answerA = "Andreas Möller im EM-Finale 1996",
        answerB = "Matthias Sammer im WM-Finale 1990",
        answerC = "Jürgen Kohler im WM-Finale 1990",
        answerD = "Stefan Reuter im EM-Finale 1992",
        correctAnswer = 2,
        explanation = "Jürgen Kohler war Teil der deutschen WM-Siegerelf 1990 in Italien. Als Innenverteidiger spielte er eine entscheidende Rolle – als BVB-Spieler war er mit dem Klub verbunden.",
        difficulty = 3,
        funFact = "Sowohl Kohler als auch Matthias Sammer wurden als Weltklassespieler bekannt, bevor sie zu BVB-Legenden wurden."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Investor erwarb 2014 einen Anteil am BVB und wurde damit zum größten Einzelaktionär des börsennotierten Klubs?",
        answerA = "Sheikh Mansour",
        answerB = "Dietmar Hopp",
        answerC = "Roman Abramovich",
        answerD = "Puma AG",
        correctAnswer = 1,
        explanation = "Dietmar Hopp, Mitgründer von SAP und Mäzen des Bundesligisten TSG Hoffenheim, erwarb 2014 einen signifikanten Anteil am BVB als Kapitalanlage.",
        difficulty = 3,
        funFact = "Hopps Investment beim BVB sorgte für Aufsehen, da er gleichzeitig Hauptsponsor des Rivalen TSG Hoffenheim war."
    ),

)
