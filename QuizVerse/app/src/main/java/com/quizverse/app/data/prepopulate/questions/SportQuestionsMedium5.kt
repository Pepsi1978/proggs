package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsMedium5(): List<Question> = listOf(

    // --- Sportsponsoring & Werbung (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welches Unternehmen ist seit 1974 offizieller Ausruestungspartner der FIFA-Weltmeisterschaft und stattete das Turnier mit Spielbaellen aus?",
        answerA = "Nike",
        answerB = "Puma",
        answerC = "Adidas",
        answerD = "Umbro",
        correctAnswer = 2,
        explanation = "Adidas ist seit 1974 exklusiver Balllieferant der FIFA-Weltmeisterschaft. Jeder offizielle WM-Spielball – vom Tango ueber den Jabulani bis zum Al Rihla – stammt von Adidas.",
        difficulty = 2,
        funFact = "Der Jabulani-Ball der WM 2010 in Suedafrika war wegen seines unkontrollierbaren Fluges bei Torhuetern besonders unpopulaer."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Autohersteller ist seit 2000 offizieller Fahrzeugpartner der Olympischen Spiele und stellt die Fahrzeugflotte?",
        answerA = "Mercedes-Benz",
        answerB = "Toyota",
        answerC = "BMW",
        answerD = "Volkswagen",
        correctAnswer = 1,
        explanation = "Toyota ist seit 2015 globaler olympischer Partner (TOP-Partner) des IOC und stellt seit den Spielen 2016 in Rio die offizielle Fahrzeugflotte. Der Vertrag laeuft bis 2024 und wurde verlaengert.",
        difficulty = 2,
        funFact = "Toyota nutzte die Olympia-Partnerschaft intensiv fuer die Vorstellung seiner Wasserstoff-Technologie und autonomen Fahrzeuge."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst das Phaenomen, wenn Unternehmen ohne offizielle Sponsorenlizenz trotzdem mit einem Sportereignis assoziiert werden?",
        answerA = "Covert Marketing",
        answerB = "Ambush Marketing",
        answerC = "Shadow Sponsoring",
        answerD = "Guerilla Licensing",
        correctAnswer = 1,
        explanation = "Ambush Marketing bezeichnet die Strategie, bei der Unternehmen ohne offizielles Sponsoring die Aufmerksamkeit eines Grossevents kapern – z.B. durch Werbung in der Naehe des Veranstaltungsorts oder durch Ausstattung von Zuschauern.",
        difficulty = 2,
        funFact = "Bei der WM 1994 schaltete Nike riesige Kampagnen in den USA, obwohl Adidas der offizielle Sponsor war – ein klassisches Ambush-Marketing-Beispiel."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Energydrink-Hersteller sponsert seit 2005 ein eigenes Formel-1-Team und wurde dadurch zum Konstrukteursweltmeister?",
        answerA = "Monster Energy",
        answerB = "Rockstar Energy",
        answerC = "Red Bull",
        answerD = "Burn Energy",
        correctAnswer = 2,
        explanation = "Red Bull Racing wurde 2005 gegruendet und gewann 2010-2013 vier Konstrukteurs-Weltmeisterschaften in Folge. Das Team gilt als beispielhaftes Sport-Marketing-Modell eines Getraenkeunternehmens.",
        difficulty = 2,
        funFact = "Red Bull besitzt mehrere Fussball- und andere Sportteams weltweit und gibt jaehrlich ca. ein Drittel seines Umsatzes fuer Marketing aus."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Sportartikelunternehmen schloss 1984 mit Michael Jordan einen Schuhvertrag ab, der die Sportvermarktung revolutionierte?",
        answerA = "Reebok",
        answerB = "Converse",
        answerC = "Puma",
        answerD = "Nike",
        correctAnswer = 3,
        explanation = "Nike schloss 1984 mit Michael Jordan einen Vertrag ueber 2,5 Millionen Dollar pro Jahr – damals ein Rekord. Die Air Jordan Linie wurde zur erfolgreichsten Sportschuhserie der Geschichte und veraenderte Athleten-Sponsoring grundlegend.",
        difficulty = 2,
        funFact = "Die erste Air Jordan wurde von der NBA verbannt, weil sie nicht zu den Teamfarben passte. Nike zahlte Jordans Strafgelder – was die Schuhserie noch bekannter machte."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurde Bayer Leverkusen zum ersten Mal Namensgeber eines deutschen Bundesliga-Klubs durch ein Unternehmens-Sponsoring?",
        answerA = "Der Klub traegt seit seiner Gruendung 1904 den Firmennamen",
        answerB = "1933 nach einem Sponsoring-Deal mit Bayer AG",
        answerC = "1979 durch einen Bundesliga-Sponsoring-Vertrag",
        answerD = "2000 durch neues Jersey-Sponsoring",
        correctAnswer = 0,
        explanation = "Bayer 04 Leverkusen wurde 1904 als Betriebssportverein der Bayer AG gegruendet. Der Firmenname war von Beginn an Teil des Vereinsnamens – kein spaeters Sponsoring, sondern eine Gruendungsgeschichte.",
        difficulty = 2,
        funFact = "Bayer Leverkusen ist einer der wenigen Bundesligaklubs, dessen Hauptsponsor zugleich Vereinsgruender ist. Ohne Bayer AG gaebe es den Verein nicht."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches globale Finanzunternehmen ist seit 1994 Titelsponsor der UEFA Champions League?",
        answerA = "Visa",
        answerB = "Mastercard",
        answerC = "UniCredit",
        answerD = "PricewaterhouseCoopers",
        correctAnswer = 1,
        explanation = "Mastercard war von 1994 bis 2018 offizieller Sponsor der UEFA Champions League und praesent in der ikonischen 'Priceless'-Kampagne. Danach uebernahm andere Sponsoren, aber Mastercard blieb ein UEFA-Partner.",
        difficulty = 2,
        funFact = "Die Mastercard-Hymne der Champions League wurde von der belgischen Band 'Elias Arts' eingespielt und gilt als eine der bekanntesten Sportmelodien der Welt."
    ),

    // --- Dopingskandale fuer Fortgeschrittene (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welches verbotene Mittel wurde Lance Armstrong bei seinen Tour-de-France-Siegen 1999-2005 hauptsaechlich verwendet?",
        answerA = "Wachstumshormone (HGH)",
        answerB = "Erythropoetin (EPO)",
        answerC = "Anabole Steroide",
        answerD = "Stimulanzien wie Amphetamine",
        correctAnswer = 1,
        explanation = "EPO (Erythropoetin) war das Hauptdoping-Mittel in Armstrongs systematischem Dopingprogramm. EPO steigert die Produktion roter Blutkoerperchen und damit den Sauerstofftransport – ideal fuer Ausdauersport. USADA-Bericht 2012 dokumentierte das System detailliert.",
        difficulty = 2,
        funFact = "Armstrong verlor alle sieben Tour-Titel, aber die UCI erkannte keinem anderen Fahrer den Sieg zu – die offizielle Tour-Geschichte hat fuer diese Jahre keine Sieger."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei den Olympischen Sommerspielen 1988 in Seoul verlor Ben Johnson die Goldmedaille im 100m-Sprint. Welche verbotene Substanz wurde bei ihm nachgewiesen?",
        answerA = "EPO",
        answerB = "Stanozolol",
        answerC = "Testosteron",
        answerD = "HGH",
        correctAnswer = 1,
        explanation = "Ben Johnson wurde positiv auf Stanozolol getestet, ein anaboles Steroid. Er hatte in 9,79 Sekunden einen Weltrekord aufgestellt. Die Medaille ging an Carl Lewis (9,92 Sek.), obwohl spaeter herauskam, dass auch mehrere andere Finalisten gedopt hatten.",
        difficulty = 2,
        funFact = "Das 100m-Finale 1988 wird als 'das schmutzigste Rennen der Geschichte' bezeichnet – von 9 Finalisten wurden spaeter 6 wegen Doping ueberführt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches osteuropaeische Land wurde 2019-2020 wegen staatlich organisiertem Doping von der WADA fuer vier Jahre von Olympischen Spielen gesperrt?",
        answerA = "Ukraine",
        answerB = "Belarus",
        answerC = "Russland",
        answerD = "Kasachstan",
        correctAnswer = 2,
        explanation = "Russland wurde von der WADA gesperrt, weil staatliche Stellen Dopingproben manipulierten. Der Sportsminister liess laut Untersuchung sauber wirkende Proben durch Loecher in der Laborwand austauschen. Russische Athleten durften als 'ROC' neutral starten.",
        difficulty = 2,
        funFact = "Der russische Whistleblower Grigory Rodchenkov, ehemaliger Chefanalytiker des Moskauer Dopinglabors, enttarnte das System und lebt seitdem im US-amerikanischen Zeugenschutz."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der sogenannte 'biologische Pass' im Anti-Doping-System?",
        answerA = "Ein Reisedokument fuer Athleten bei internationalen Wettkampfreisen",
        answerB = "Eine Langzeitdokumentation biologischer Blut- und Urinparameter eines Athleten",
        answerC = "Ein DNA-Profil zur eindeutigen Athleten-Identifikation bei Tests",
        answerD = "Eine Genehmigung fuer medizinisch notwendige Substanzen",
        correctAnswer = 1,
        explanation = "Der Athleten-Biologische-Pass (ABP) dokumentiert ueber Jahre die individuellen Blut- und Steroidparameter eines Athleten. Abweichungen vom persoenlichen Profil koennen auf Doping hinweisen, ohne direkt eine verbotene Substanz nachweisen zu muessen.",
        difficulty = 2,
        funFact = "Der biologische Pass fuehrte zur Nachsperrung vieler Athleten – auch Jahre nach Wettkampfende – als Analysemethoden verbessert wurden und historische Proben neu ausgewertet wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Radprofi gab 1997 in einem ARD-Interview als erster prominenter Sportler offen zu, EPO genommen zu haben?",
        answerA = "Jan Ullrich",
        answerB = "Bjarne Riis",
        answerC = "Rolf Aldag",
        answerD = "Erik Zabel",
        correctAnswer = 2,
        explanation = "Bjarne Riis (Daene, kein Deutscher) gestand 2007 EPO-Doping. Rolf Aldag gestand 2007 ebenfalls in einem ARD-Interview fuer das Team Telekom gedopt zu haben. Aldag war einer der ersten Telekom-Profis, die oeffentlich gestanden.",
        difficulty = 2,
        funFact = "Sechs ehemalige Fahrer des Teams Telekom gestanden 2007 gleichzeitig EPO-Doping – was den deutschen Radsport in eine tiefe Krise stuerzte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Substanz steht hinter dem Akronym CERA, das beim Radsport als 'dritte Generation EPO' gilt?",
        answerA = "Kontinuierlich Erythropoetin-Rezeptor-Aktivator",
        answerB = "Chemisch erweiterter Ringanalysator",
        answerC = "Chronisch erhoehter Roterythrozyt-Anteil",
        answerD = "Corticoides erythropoietiques a retard",
        correctAnswer = 0,
        explanation = "CERA steht fuer 'Continuous Erythropoietin Receptor Activator', ein langwirksames EPO-Praeparat. Es wurde 2008 bei der Tour de France erstmals nachgewiesen und fuehrte zur Disqualifikation mehrerer Fahrer, darunter Bernhard Kohl und Stefan Schumacher.",
        difficulty = 2,
        funFact = "CERA wurde eigentlich zur Behandlung von Anaemie bei Nierenerkrankungen entwickelt. Es ist eine chemisch modifizierte Form von EPO mit deutlich laengerer Halbwertszeit im Blut."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Leichtathlet wurde 2013 zum rueckwirkenden Sechsfach-Doping-Sperrentscheid verurteilt und gilt als Symbolfigur staatlich gelenkten russischen Dopings?",
        answerA = "Jurij Borzakowski",
        answerB = "Sergei Kirdjaschkin",
        answerC = "Pawel Maslak",
        answerD = "Wadim Dewjatowski",
        correctAnswer = 1,
        explanation = "Sergei Kirdjaschkin, russischer Geher und Weltmeister, wurde 2014 fuer 8 Jahre gesperrt (spaeter reduziert) wegen Manipulationen im biologischen Pass. Er gilt als prominentes Beispiel des staatlich organisierten russischen Dopingsystems.",
        difficulty = 2,
        funFact = "Kirdjaschkin musste seine WM-Goldmedaille 2011 abgeben. Insgesamt wurden Dutzende russische Geher und Laeufer mit drastischen Sperren belegt."
    ),

    // --- Sportliche Rivalitaeten (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wie viele Grand-Slam-Titel gewann Rafael Nadal auf Sand im Vergleich zu Roger Federers gesamten Grand-Slam-Titeln?",
        answerA = "Nadal gewann 14 French-Open-Titel; Federer gewann insgesamt 20 Grand Slams",
        answerB = "Nadal 12 Sand-Titel; Federer 18 Gesamt-Titel",
        answerC = "Nadal 14 Sand-Titel; Federer 22 Gesamt-Titel",
        answerD = "Beide gewannen jeweils 20 Grand Slams",
        correctAnswer = 0,
        explanation = "Rafael Nadal gewann die French Open 14-mal (2005-2022). Roger Federer gewann insgesamt 20 Grand-Slam-Titel. Nadals Dominanz in Roland Garros ist beispiellos – 14 Titel bei 116 Sand-Satz-Bilanzen in Paris.",
        difficulty = 2,
        funFact = "Nadals einzige Niederlage in Roland Garros erlitten er 2009 gegen Robin Soderling im Achtelfinale – das einzige Mal, dass er vor dem Finale in Paris verlor."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Rivalitaet im Boxen wird als 'Thrilla in Manila' bezeichnet?",
        answerA = "Muhammad Ali vs. George Foreman",
        answerB = "Muhammad Ali vs. Joe Frazier",
        answerC = "Mike Tyson vs. Evander Holyfield",
        answerD = "Larry Holmes vs. Earnie Shavers",
        correctAnswer = 1,
        explanation = "Der dritte Kampf zwischen Muhammad Ali und Joe Frazier am 1. Oktober 1975 in Manila (Philippinen) wurde als 'Thrilla in Manila' bekannt. Ali gewann durch TKO in Runde 14. Beide Boxer bezeichneten es spaeter als den haertesten Kampf ihres Lebens.",
        difficulty = 2,
        funFact = "Nach dem Kampf sagte Ali, er hatte das Gefuehl gehabt, dem Tod naeher zu sein als je zuvor. Fraziers Trainer Eddie Futch warf das Handtuch, weil Frazier auf einem Auge fast blind war."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Rivalitaet praegt seit 1995 den Formel-1-Sport als 'Clasico' der Motorsportwelt zwischen zwei deutschen Traditionsteams?",
        answerA = "Mercedes vs. Porsche",
        answerB = "Ferrari vs. McLaren",
        answerC = "Ferrari vs. Red Bull",
        answerD = "Williams vs. Benetton",
        correctAnswer = 1,
        explanation = "Ferrari gegen McLaren gilt als die groesste Rivalitaet der Formel-1-Geschichte. Hoehepunkt war die Saison 2007/2008 mit Lewis Hamilton und Kimi Raikkonen/Felipe Massa – sowie die Ayrton Senna-vs-Alain Prost-Aera in den 1980/90ern.",
        difficulty = 2,
        funFact = "Die Spionage-Affaere 2007 ('Spygate') zwischen Ferrari und McLaren kostete McLaren 100 Millionen Dollar Strafe und alle Konstrukteurspunkte – die haerteste Strafe der F1-Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Die Rivalitaet zwischen Marta und Cristiano Ronaldo im Weltfussball unterscheidet sich fundamental. Was ist Martas besondere Auszeichnung?",
        answerA = "Marta wurde sechsmal zur FIFA-Weltfussballerin des Jahres gewaehlt – Rekord fuer Frauen und Maenner",
        answerB = "Marta haelt den Rekord als Torschuetzenkoenig/-koenigin bei Frauenweltmeisterschaften mit 17 Toren",
        answerC = "Marta spielte als einzige Frau in einer Maenner-Profiliga in Europa",
        answerD = "Marta gewann mit Brasilien fuenf Frauenweltmeisterschaften",
        correctAnswer = 1,
        explanation = "Marta (Brasilien) ist Rekordtorschuetzin bei Frauenweltmeisterschaften mit 17 Toren in 5 Turnieren (1999-2019). Sie wurde auch sechsmal FIFA-Weltfussballerin des Jahres (2006-2010, 2018).",
        difficulty = 2,
        funFact = "Marta traf bei jeder ihrer fuenf Weltmeisterschaften – als einzige Spielerin, Frau oder Mann, in der WM-Geschichte. Das gelang nicht mal Pele oder Ronaldo."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war das historische Resultat des sogenannten 'Jahrhundertspiels' zwischen Deutschland und Italien bei der WM 1970?",
        answerA = "Deutschland gewann 4:3 in der Verlaengerung",
        answerB = "Italien gewann 4:3 in der Verlaengerung",
        answerC = "Deutschland gewann 3:2 nach Elfmeterschiessen",
        answerD = "Unentschieden 3:3, Weiterkommen durch Losentscheid",
        correctAnswer = 1,
        explanation = "Das Halbfinale der WM 1970 in Mexiko ging als 'Jahrhundertspiel' in die Geschichte ein. Italien schlug Deutschland 4:3 nach Verlaengerung in einem spektakulaeren Spiel mit fuenf Toren in der Verlaengerung. Die Anzeigetafel im Aztekenstadion wurde zur Legende.",
        difficulty = 2,
        funFact = "Das Spiel wurde offiziell als 'Jahrhundertspiel' geehrt – am Aztekenstadion haengt eine Gedenktafel mit dieser Bezeichnung fuer das Halbfinale vom 17. Juni 1970."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst die buergerliche Rivalitaet im schottischen Fussball zwischen Celtic Glasgow und Rangers Glasgow?",
        answerA = "The Highland Derby",
        answerB = "The Glasgow Clash",
        answerC = "The Old Firm",
        answerD = "The Celtic Feud",
        correctAnswer = 2,
        explanation = "Das Duell zwischen Celtic FC und Rangers FC heisst 'The Old Firm' und gilt als eines der intensivsten Fussball-Derbys der Welt. Es hat tiefe religioese Wurzeln: Celtic ist katholisch-irisch gepraegt, Rangers protestantisch-britisch.",
        difficulty = 2,
        funFact = "Der Begriff 'Old Firm' stammt aus dem 19. Jahrhundert und bezieht sich auf die wirtschaftliche Zusammenarbeit beider Vereine als 'altes Unternehmen' bei der Vermarktung ihrer Derbys."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche zwei Tennisspielerinnen dominierten die WTA-Tour von 1999 bis 2016 und wurden als das groesste Schwesternpaar im Sport bekannt?",
        answerA = "Steffi Graf und Arantxa Sanchez",
        answerB = "Martina Navratilova und Chris Evert",
        answerC = "Venus Williams und Serena Williams",
        answerD = "Hingis und Mauresmo",
        correctAnswer = 2,
        explanation = "Venus und Serena Williams revolutionierten den Damentennis. Serena gewann 23 Grand-Slam-Titel, Venus 7. Beide spielten 9-mal im Grand-Slam-Finale gegeneinander – Serena gewann 7 dieser Familienfinali. Ihre Rivalitaet sprengte alle Grenzen.",
        difficulty = 2,
        funFact = "Ihr Vater Richard Williams hatte angeblich vorher aufgeschrieben, wie viele Grand Slams jede seiner Toechter gewinnen wuerde – und lag erstaunlich nah an den tatsaechlichen Zahlen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lautete das beruehmt-beruechtigte Ergebnis des 'El Clasico' am 5. Mai 1994, das Barcas 'Dream Team' die Meisterschaft kostete?",
        answerA = "Real Madrid gewann 5:0 gegen Barcelona",
        answerB = "Barcelona gewann 3:0 gegen Real Madrid",
        answerC = "Real Madrid gewann 2:1 durch ein Tor in der 93. Minute",
        answerD = "0:0 – Real stahl Barcelona den Titel durch Tordifferenz",
        correctAnswer = 0,
        explanation = "Real Madrid gewann am vorletzten Spieltag 1994 gegen Barcelona 5:0 in La Coruna (nicht direkt El Clasico) – das Spiel war Deportivo La Coruna. Barcelonas 'Dream Team' unter Johan Cruyff verlor die Meisterschaft dadurch.",
        difficulty = 2,
        funFact = "Das 5:0 gilt als eines der traumatischsten Erlebnisse in der Geschichte des FC Barcelona und beendete Cruyffs erfolgreichste Trainer-Aera beim Klub."
    ),

    // --- Sport und Kultur (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welcher Sportfilm aus dem Jahr 1981 thematisierte die Gewissenskonflikte zweier britischer Sprinter bei den Olympischen Spielen 1924?",
        answerA = "Rocky",
        answerB = "Chariots of Fire",
        answerC = "The Loneliness of the Long Distance Runner",
        answerD = "Running Brave",
        correctAnswer = 1,
        explanation = "Chariots of Fire (1981) gewann vier Oscars, darunter Bester Film. Er erzaehlt die Geschichte von Harold Abrahams (juedischer Sprinter gegen Diskriminierung) und Eric Liddell (schottischer Laeufer aus religioesen Ueberzeugungen). Soundtrack von Vangelis ist ikonisch.",
        difficulty = 2,
        funFact = "Die beruehmt gewordene Strandlaufszene mit der Vangelis-Melodie wurde tatsaechlich am Strand von West Sands in St Andrews (Schottland) gedreht, nicht in Suedfrankreich wie oft angenommen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Bedeutung hat der Ausdruck 'Corinthian Spirit' im Sport?",
        answerA = "Profisport-Mentalitaet mit Fokus auf Ergebnisse",
        answerB = "Amateurideal von Fair Play und Sport um des Sports willen",
        answerC = "Aggressives, auf Koerperlichkeit ausgerichtetes Spiel",
        answerD = "Technisch perfektionierter Spielstil nach griechischem Vorbild",
        correctAnswer = 1,
        explanation = "Der 'Corinthian Spirit' leitet sich vom Corinthian FC (England, 1882) ab – einem Amateurklub, der das Ideal des Sports um des Sports willen, Fair Play und das Ablehnen von Profidenken verkörperte. Legendaer: Der Verein zog Elfmeter ab, wenn Gegner einen verwandelt hatten.",
        difficulty = 2,
        funFact = "Der Corinthian FC war so einflussreich, dass ein brasilianischer Verein nach ihm benannt wurde: SC Corinthians Paulista, heute einer der groessten Klubs Suedamerikas."
    ),

    Question(
        categoryId = 6,
        questionText = "Die Geste des erhobenen Fausts bei den Olympischen Spielen 1968 von Tommie Smith und John Carlos war ein Protest fuer was?",
        answerA = "Protest gegen den Vietnamkrieg",
        answerB = "Symbol fuer die Black-Power-Bewegung und Buergerrechtsbewegung",
        answerC = "Protest gegen die Dopingkontrollen",
        answerD = "Zeichen der Solidaritaet mit suedafrikanischen Athleten",
        correctAnswer = 1,
        explanation = "Tommie Smith (Gold) und John Carlos (Bronze) streckten bei der Siegerehrung der 200m je eine schwarzbehandschuhte Faust in den Himmel – ein Symbol fuer Black Power und die Buergerrechtsbewegung. Beide wurden vom US-Olympia-Komitee ausgeschlossen.",
        difficulty = 2,
        funFact = "Der australische Silbermedaillengewinner Peter Norman trug als Solidaritaetszeichen ein Abzeichen des 'Olympic Project for Human Rights'. Er wurde deshalb von der australischen Olympiageschichte jahrzehntelang ignoriert."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der kulturelle Hintergrund des neuseelaendischen Rugbys-Ritualtanzes 'Haka'?",
        answerA = "Eine Siegesfeier, die erst 1987 von der All-Blacks-Mannschaft erfunden wurde",
        answerB = "Ein traditioneller Maori-Kriegs- und Zeremonialtanz zur Einschuechterung und Ehrbezeugung",
        answerC = "Ein polynesischer Ausdauertest, der aus militaerischen Uebungen stammt",
        answerD = "Ein religioeser Tanz aus dem Pazifik, der Goetter um Schutz bittet",
        correctAnswer = 1,
        explanation = "Der Haka ist ein traditioneller Maori-Tanz, der bei Zeremonien, Begegnungen und als Kriegsvorbereitung aufgefuehrt wurde. Die All Blacks fuehren ihn seit 1884 auf – urspruenglich war es der Haka 'Ka Mate', seit 2005 auch 'Kapa o Pango'.",
        difficulty = 2,
        funFact = "Der Haka 'Ka Mate' wurde um 1820 vom Maori-Hauptling Te Rauparaha komponiert, als er einer feindlichen Gruppe entkam, indem er sich in einer Grube versteckte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Buch aus den 1990ern beschreibt Fankultur, Fussball und englische Identitaet und fuehrte zu einer Wiederbelebung des Interesses an Fussball-Literatur?",
        answerA = "The Damned United von David Peace",
        answerB = "Fever Pitch von Nick Hornby",
        answerC = "Among the Thugs von Bill Buford",
        answerD = "My Autobiography von Alex Ferguson",
        correctAnswer = 1,
        explanation = "Nick Hornbys 'Fever Pitch' (1992) beschreibt seine Besessenheit mit Arsenal und wurde zum Kultbuch ueber Fussball und Fankultur. Es veroeffentlichte eine Welle von Sporterinnerungs-Literatur und wurde zweimal verfilmt.",
        difficulty = 2,
        funFact = "Nick Hornby schrieb Fever Pitch als ernstes Buch ueber Fanpsychologie und Identitaet – dass es auch komisch ist, war ihm anfangs gar nicht so klar. Es gilt als Pionierwerk der Sport-Autobiografik."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Bedeutung hatte das Wembley-Finale 1953 zwischen England und Ungarn fuer den Weltfussball?",
        answerA = "England gewann und bewies damit die Ueberlegenheit des britischen Fussballstils",
        answerB = "Ungarn gewann 6:3 und demolierte den Mythos der englischen Unbesiegbarkeit auf eigenem Boden",
        answerC = "Das Spiel endete 0:0 und fuehrte zur Abschaffung des Heimrecht-Vorteils",
        answerD = "Ungarn gewann 4:3 und wurde zum Weltmeister erklaert",
        correctAnswer = 1,
        explanation = "Ungarn besiegte England am 25. November 1953 in Wembley 6:3 – Englands erste Niederlage auf eigenem Boden gegen eine Nicht-Briten-Auswahl. Die 'Goldene Mannschaft' unter Puskas demonstrierte total neues taktisches Denken und veraenderte den Weltfussball.",
        difficulty = 2,
        funFact = "Das Rueckspiel in Budapest 1954 war noch demuetigender: Ungarn gewann 7:1. Stuermer Ferenc Puskas galt als bester Spieler der Welt und praegt bis heute das Erbe des ungarischen Fussballs."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die kulturelle Bedeutung des 'Tour de Suisse'-Starts in einer anderen Stadt als der gleichnamigen Hauptstadt?",
        answerA = "Der Startort wechselt jaehrlich, um alle Schweizer Regionen einzubeziehen und die kulturelle Vielfalt zu foerdern",
        answerB = "Bern hat einen Vertrag, der es von Rennveranstaltungen ausschliesst",
        answerC = "Aus Sicherheitsgruenden starten alle Radrennen ausserhalb von Bundeshauptstadten",
        answerD = "Der Ursprung liegt darin, dass die Schweiz keine touristische Hauptstadt hat",
        correctAnswer = 0,
        explanation = "Die Tour de Suisse wechselt Start- und Zielstaedte, um die kulturelle Vielfalt der Schweiz zu repraesentieren. Die vier Sprachregionen (Deutsch, Franzoesisch, Italienisch, Raetoromanisch) sollen gleichermassen einbezogen werden.",
        difficulty = 2,
        funFact = "Die Schweiz ist das einzige Land der Welt mit vier Amtssprachen, das ein nationales Radrennen ausrichtet. Die Route durch alle Sprachregionen ist ein kulturelles Symbol fuer Inklusion."
    ),

    // --- Sporttraining-Basics (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was beschreibt das Trainingsprinzip der 'Superkompensation'?",
        answerA = "Uebermaessiges Training zur maximalen Leistungssteigerung ohne Erholung",
        answerB = "Anpassung des Koerpers nach einer Belastung auf ein hoeheres Leistungsniveau als zuvor",
        answerC = "Ernaehrungsergaenzung mit Proteinen nach dem Training",
        answerD = "Das gleichzeitige Training mehrerer Muskelgruppen fuer maximale Effizienz",
        correctAnswer = 1,
        explanation = "Superkompensation beschreibt die Anpassungsreaktion: Nach einer Trainingsbelastung sinkt die Leistungsfaehigkeit (Ermuedung), steigt danach ueber das Ausgangsniveau hinaus (Adaptation) und faellt dann wieder ab. Optimales Training setzt im Superkompensationsgipfel an.",
        difficulty = 2,
        funFact = "Das Konzept der Superkompensation wurde in den 1960ern systematisch erforscht. Falsch verstandene Anwendung – zu wenig Erholung – fuehrt zu Uebertraining und Leistungsabfall statt -steigerung."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Herzfrequenzzonen gelten im Ausdauertraining als aerobe Grundlagenzone?",
        answerA = "90-100% der maximalen Herzfrequenz",
        answerB = "60-75% der maximalen Herzfrequenz",
        answerC = "40-55% der maximalen Herzfrequenz",
        answerD = "80-90% der maximalen Herzfrequenz",
        correctAnswer = 1,
        explanation = "Die aerobe Grundlagenzone (GA1/GA2) liegt bei 60-75% der maximalen Herzfrequenz. In diesem Bereich verbrennt der Koerper vorwiegend Fett als Energiequelle und trainiert die aerobe Kapazitaet ohne hohe Laktatbildung.",
        difficulty = 2,
        funFact = "Profisportler verbringen bis zu 80% ihrer Trainingszeit in der aeroben Grundlagenzone – intensives Training macht nur 20% aus. Dieses '80/20-Prinzip' gilt als wissenschaftlich optimal bestaetigt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'anaerobe Schwelle' im Ausdauersport?",
        answerA = "Die maximale Sauerstoffaufnahmerate (VO2max) eines Athleten",
        answerB = "Der Belastungspunkt, ab dem Laktat schneller produziert als abgebaut wird",
        answerC = "Der Puls, ab dem Fett nicht mehr als Energiequelle genutzt wird",
        answerD = "Die maximale Herzfrequenz, die dauerhaft aufrechterhalten werden kann",
        correctAnswer = 1,
        explanation = "Die anaerobe Schwelle (Laktatschwelle) ist der Intensitaetspunkt, ab dem die Laktatproduktion die Laktatentsorgung uebersteigt. Unterhalb dieser Schwelle kann Leistung lange aufrechterhalten werden; oberhalb steigt Laktat schnell an und fuehrt zur Ermuedung.",
        difficulty = 2,
        funFact = "Gut trainierte Ausdauersportler haben eine anaerobe Schwelle bei 85-90% ihrer maximalen Herzfrequenz. Untrainierte Menschen erreichen sie schon bei 55-60% – Training erhoeht diese Schwelle signifikant."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet das Akronym DOMS im Sportbereich?",
        answerA = "Dynamic Oxygen Measurement System",
        answerB = "Delayed Onset Muscle Soreness – verzoegerter Muskelkater",
        answerC = "Duration of Maximum Strength",
        answerD = "Daily Overload Management Strategy",
        correctAnswer = 1,
        explanation = "DOMS (Delayed Onset Muscle Soreness) ist der Muskelkater, der 24-48 Stunden nach ungewohnter oder intensiver Belastung auftritt. Er entsteht durch Mikrorisse in Muskelfasern, die Entzuendungsreaktionen ausloesen. Kein Zeichen fuer gutes Training, aber normal.",
        difficulty = 2,
        funFact = "Die Theorie, dass Muskelkater durch Milchsaeure entsteht, ist wissenschaftlich widerlegt. Laktat wird innerhalb einer Stunde nach dem Training abgebaut – DOMS tritt aber erst viel spaeter auf."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Krafttraining das Prinzip der 'progressiven Ueberlastung'?",
        answerA = "Das Trainieren mit immer leichteren Gewichten zur Verletzungspravention",
        answerB = "Das stufenweise Erhoehen von Gewicht, Wiederholungen oder Intensitaet ueber Zeit",
        answerC = "Das abwechselnde Training verschiedener Muskelgruppen zur Erholung",
        answerD = "Das Trainieren bis zum Muskelversagen bei jedem Satz",
        correctAnswer = 1,
        explanation = "Progressive Ueberlastung bedeutet, den Trainingsreiz kontinuierlich zu steigern – durch mehr Gewicht, mehr Wiederholungen, kuerzeere Pausen oder hoehere Frequenz. Nur so passt sich der Koerper langfristig an und wird staerker.",
        difficulty = 2,
        funFact = "Das Konzept stammt angeblich vom griechischen Ringer Milon von Kroton (ca. 6. Jhd. v. Chr.), der taeglich ein Kalb auf den Schultern trug – und mit dem Tier mitwuchs, bis er einen Stier trug."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt die 'VO2max' als Trainingsparameter?",
        answerA = "Maximale Sauerstoffaufnahme pro Minute pro Kilogramm Koerpergewicht",
        answerB = "Verhaeltnis von Sauerstoffverbrauch zu CO2-Abgabe bei maximaler Belastung",
        answerC = "Minimale Sauerstoffmenge, die der Koerper in Ruhe benoetigt",
        answerD = "Sauerstoffsaettigungswert des Blutes bei anaerober Belastung",
        correctAnswer = 0,
        explanation = "VO2max gibt die maximale Sauerstoffaufnahme in ml/(kg*min) an und gilt als wichtigste Messgrösse der aeroben Ausdauerkapazitaet. Spitzenausdauersportler erreichen Werte von 70-90 ml/(kg*min), untrainierte Erwachsene 30-45 ml/(kg*min).",
        difficulty = 2,
        funFact = "Oescar Svendsen, ein norwegischer Radsportler, erzielte 2012 den hoechsten je gemessenen VO2max-Wert: 97,5 ml/(kg*min). Zum Vergleich: Ein durchschnittlicher gesunder Mann hat ca. 40 ml/(kg*min)."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Sporternaehrung der empfohlene Proteinbedarf eines Kraftsportlers pro Kilogramm Koerpergewicht pro Tag?",
        answerA = "0,5-0,8 g/kg – wie bei Nichtsportlern",
        answerB = "1,6-2,2 g/kg Koerpergewicht",
        answerC = "3,0-4,0 g/kg – je mehr desto besser",
        answerD = "Proteinmenge spielt keine wissenschaftlich belegte Rolle",
        correctAnswer = 1,
        explanation = "Die aktuelle Sporternaehrungswissenschaft empfiehlt 1,6-2,2 g Protein pro kg Koerpergewicht taeglich fuer Kraftsportler zum Muskelaufbau und -erhalt. Mehr bringt keinen zusaetzlichen Nutzen und belastet Nieren und Leber.",
        difficulty = 2,
        funFact = "Frueherer Mythos: Mehr Protein = mehr Muskel. Studien zeigen, dass oberhalb von 2,2 g/kg der Ueberschuss einfach als Energie verbrannt wird. Qualitaet und Verteilung ueber den Tag sind wichtiger als pure Menge."
    ),

    // --- Sportverband-Organisation (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wie heisst die internationale Dachorganisation des Leichtathletik-Weltverbandes seit seiner Umbenennung 2019?",
        answerA = "IAAF",
        answerB = "World Athletics",
        answerC = "Global Athletics Federation",
        answerD = "International Athletic Union",
        correctAnswer = 1,
        explanation = "Die IAAF (International Association of Athletics Federations) wurde 2019 in 'World Athletics' umbenannt. Praesident ist Sebastian Coe. Die Organisation repraesentiert ueber 200 Nationalverbaende und organisiert Weltmeisterschaften und Weltranglistensystem.",
        difficulty = 2,
        funFact = "Die IAAF wurde 1912 in Stockholm waehrend der Olympischen Spiele gegruendet. 2019 fiel die Entscheidung zur Umbenennung zeitgleich mit verstaerkten Bemuehungen um Transparenz nach Korruptionsskandalen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen einem 'Nationalen Olympischen Komitee' (NOK) und einem 'nationalen Sportfachverband'?",
        answerA = "NOKs verwalten Profisport, Fachverbaende organisieren Amateursport",
        answerB = "NOKs koordinieren olympische Beteiligung einer Nation, Fachverbaende regeln eine spezifische Sportart",
        answerC = "NOKs sind oeffentlich-rechtlich, Fachverbaende privatwirtschaftlich",
        answerD = "Es gibt keinen Unterschied – die Begriffe sind austauschbar",
        correctAnswer = 1,
        explanation = "Ein NOK (z.B. DOSB in Deutschland) koordiniert die Olympia-Teilnahme der gesamten Nation. Ein Fachverband (z.B. DFB fuer Fussball, DLV fuer Leichtathletik) ist zustaendig fuer eine spezifische Sportart – Regelwerk, Wettkampfe, Kaderathleten.",
        difficulty = 2,
        funFact = "Deutschland hat ca. 90.000 registrierte Sportvereine unter dem Dach des DOSB. Die Vereinsdichte im deutschen Sport ist eine der hoechsten der Welt – ca. 27 Millionen Mitglieder in organisierten Vereinen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Mitgliedsland-Verbände hat die FIFA derzeit – mehr oder weniger als die Vereinten Nationen?",
        answerA = "Gleich viele wie die Vereinten Nationen (193)",
        answerB = "Weniger – nur 150 Mitglieder",
        answerC = "Mehr – die FIFA hat mehr Mitglieder als die UN",
        answerD = "Die FIFA hat genauso viele (211) wie Laender existieren",
        correctAnswer = 2,
        explanation = "Die FIFA hat 211 Mitgliedsverbände – deutlich mehr als die 193 Mitgliedsstaaten der Vereinten Nationen. Grund: Auch Regionen ohne UN-Mitgliedschaft (z.B. Palaestina, Kosovo, Tahiti) haben eigene Fussballverbaende.",
        difficulty = 2,
        funFact = "England, Schottland, Wales und Nordirland haben jeweils eigene FIFA-Mitgliedschaften – obwohl sie politisch zum selben Land gehoeren. Das ist ein historisches Privileg aus der Gruendungszeit des Weltfussballverbandes."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die Aufgabe des 'Court of Arbitration for Sport' (CAS) in Lausanne?",
        answerA = "Vergabe der Olympischen Spiele an Austragungsstadte",
        answerB = "Internationales Schiedsgericht fuer Streitigkeiten im Sport",
        answerC = "Dopingkontroll-Labor fuer internationale Wettkampfe",
        answerD = "Verwaltung der Finanzen internationaler Sportverbande",
        correctAnswer = 1,
        explanation = "Der CAS (Court of Arbitration for Sport), auch Internationales Sportschiedsgericht, ist das oberste Schiedsgericht fuer Sportstreitigkeiten weltweit. Hier werden Doping-Einsprueche, Transferstreitigkeiten, Qualification-Fragen und Verbands-Beschluesse angefochten.",
        difficulty = 2,
        funFact = "Der CAS wurde 1984 auf Initiative des IOC gegründet. Er hat seinen Sitz in Lausanne (Schweiz) und entscheidet jährlich über 500 Fälle. Sportler haben 21 Tage Zeit, um gegen einen Beschluss beim CAS zu klagen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Organisation vergibt die Fussball-Weltrangliste der Maenner und wie oft wird sie aktualisiert?",
        answerA = "UEFA, vierteljaehrlich",
        answerB = "FIFA, monatlich",
        answerC = "FIFA, monatlich (ausser in Turniermonaten woechentlich)",
        answerD = "FIFA, jaehrlich nach jedem Jahresturnierzyklus",
        correctAnswer = 1,
        explanation = "Die FIFA-Weltrangliste wird von der FIFA herausgegeben und monatlich aktualisiert. Punkte werden anhand von Spielergebnissen berechnet, wobei Wettbewerbstyp (WM, Kontinentalmeisterschaft, Freundschaftsspiel) und Stärke des Gegners gewichtet werden.",
        difficulty = 2,
        funFact = "Deutschland stand am 11. Juni 2014 auf Platz 2 der FIFA-Weltrangliste – und gewann die WM. Der aktuelle Weltranglisten-Erste zum Turnierbeginn ist selten auch der WM-Sieger."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter einem 'Wild Card'-Startplatz bei einem Grand-Slam-Tennisturnier?",
        answerA = "Ein Freilos in der ersten Runde ohne Gegner",
        answerB = "Eine direkte Einladung des Veranstalters unabhaengig von der Weltrangliste",
        answerC = "Ein Startplatz fuer den Weltranglistenersten ohne Qualifikation",
        answerD = "Ein Joker-Startrecht nach einer Verletzungspause",
        correctAnswer = 1,
        explanation = "Eine Wild Card ist ein direkter Startplatz im Hauptfeld, den der Turnierveranstalter unabhaengig von der Weltrangliste vergeben kann. Typischerweise gehen Wild Cards an Nachwuchsspieler, populaere Lokalmatadore oder Spieler, die nach Verletzung zurueckkehren.",
        difficulty = 2,
        funFact = "Wild Cards sind im Tennis eine der wichtigsten Foerdermoeglichkeiten fuer junge Talente. Rafael Nadal erhielt als 15-Jaehriger seine erste Wild Card fuer ein ATP-Turnier in seiner Heimatstadt Mallorca."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'Solidarity Payment' im internationalen Fussballs?",
        answerA = "Zwangsabgabe hochverschuldeter Klubs an die FIFA",
        answerB = "Finanztransfer bei Spielertransfers an alle Ausbildungsvereine des Spielers",
        answerC = "Pflichtspende der Top-Klubs an Entwicklungslaender-Verbände",
        answerD = "Versicherungsleistung bei Spielerverletzungen waehrend Nationalmannschaftseinsaetzen",
        correctAnswer = 1,
        explanation = "Das Solidarity Payment ist eine FIFA-Regelung: Bei jedem professionellen Spielertransfer fliessen 5% der Abloesesuemme anteilig an alle Vereine, die den Spieler zwischen dem 12. und 23. Lebensjahr ausgebildet haben. So werden Nachwuchsklubs belohnt.",
        difficulty = 2,
        funFact = "Das Solidarity Payment ist eine der wichtigsten Einnahmequellen kleiner Amateurvereine. Als Kylian Mbappe 2022 seinen Vertrag bei PSG verlaengerte, erhielten seine Jugendklubs (u.a. AS Bondy) anteilig Millionenbetraege."
    ),

    // --- Multisport-Events (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Bei welchem Multisport-Event treten Athleten aus afrikanischen Laendern ausserhalb der Olympischen Spiele gegeneinander an?",
        answerA = "Pan-African Games",
        answerB = "All-Africa Games",
        answerC = "African Championships",
        answerD = "Continental African Sports Festival",
        correctAnswer = 1,
        explanation = "Die All-Africa Games (auch African Games) sind ein multisportliches Ereignis fuer alle afrikanischen Staaten, aehnlich wie die Asienspiele oder Panamerikanischen Spiele. Sie finden alle vier Jahre statt und werden vom Supreme Council for Sport in Africa organisiert.",
        difficulty = 2,
        funFact = "Die ersten All-Africa Games fanden 1965 in Brazzaville (Republik Kongo) statt. Das Ziel war, afrikanische Solidaritaet und sportliche Entwicklung auf dem Kontinent zu foerdern."
    ),

    Question(
        categoryId = 6,
        questionText = "Was sind die Deaflympics und wann wurden sie zum ersten Mal abgehalten?",
        answerA = "Olympische Spiele fuer Gehoerlose, gegruendet 1924 in Paris",
        answerB = "Paralympics-Unterkategorie fuer taubblinde Athleten, seit 1960",
        answerC = "Weltspiele der Gehoerlosen, die alle zwei Jahre stattfinden",
        answerD = "Stille Olympiade ohne Zuschauer fuer konzentrationssensible Sportler",
        correctAnswer = 0,
        explanation = "Die Deaflympics (International Committee of Sports for the Deaf) fanden erstmals 1924 in Paris statt – noch vor den Paralympics (1960). Sie sind die aeltesten Multisport-Spiele fuer Menschen mit Behinderung. Gehoerlose Athleten starten hier, nicht bei den Paralympics.",
        difficulty = 2,
        funFact = "Bei den Deaflympics werden keine Hoerhilfen getragen. Startzeichen erfolgen optisch statt akustisch. Gehoerlose Sportler bevorzugen oft die Deaflympics, da sie eine eigene Identitaet und Gehoerlosenkultur zelebrieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportarten gehoeren zum modernen Fuenfkampf (Moderner Fuenfkampf) bei den Olympischen Spielen?",
        answerA = "Schwimmen, Schiessen, Fechten, Reiten, Querfeldeinlauf",
        answerB = "Schwimmen, Schiessen, Fechten, Leichtathletik, Radfahren",
        answerC = "Rudern, Bogenschiessen, Reiten, Schwimmen, Laufen",
        answerD = "Turnen, Schwimmen, Fechten, Reiten, Gewichtheben",
        correctAnswer = 0,
        explanation = "Der Moderne Fuenfkampf umfasst: Fechten (Einzeldegen), Schwimmen (200m Freistil), Schiessen (Laser-Pistole), Reiten (Springreiten) und Querfeldeinlauf (3200m). Bei Olympia 2024 wurde Reiten durch Hindernislauf ersetzt.",
        difficulty = 2,
        funFact = "Der Moderne Fuenfkampf wurde von Pierre de Coubertin erfunden und 1912 in das Olympia-Programm aufgenommen. Coubertin wollte die Faehigkeiten eines idealen Soldaten testen, der hinter feindlichen Linien operiert."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst das groesste Multisport-Event fuer Kinder und Jugendliche, das vom IOC veranstaltet wird?",
        answerA = "Junior Olympics",
        answerB = "Youth Olympic Games",
        answerC = "World Youth Championships",
        answerD = "International Junior Sports Festival",
        correctAnswer = 1,
        explanation = "Die Youth Olympic Games (YOG) wurden 2010 in Singapur erstmals abgehalten. Sie richten sich an Athleten im Alter von 14-18 Jahren und verbinden Wettkampf mit Bildungs- und Kulturprogrammen. Sommer- und Winter-YOG wechseln sich ab.",
        difficulty = 2,
        funFact = "Die YOG wurden 2001 vom damaligen IOC-Praesident Jacques Rogge vorgeschlagen. Neben dem Sport stehen Nachhaltigkeit, Bildung und kultureller Austausch gleichberechtigt im Mittelpunkt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was sind die 'World Masters Games' und welche Besonderheit haben sie?",
        answerA = "Ein Wettkampf fuer pensionierte Weltmeister aller Disziplinen",
        answerB = "Das groesste Multisport-Event der Welt fuer Sportler ab 30 Jahren",
        answerC = "Eine Elite-Veranstaltung nur fuer ehemalige Olympioniken ueber 40",
        answerD = "Ein paralympisches Veranstaltungsformat fuer aeltere Behindertensportler",
        correctAnswer = 1,
        explanation = "Die World Masters Games sind das nach Teilnehmerzahl groesste Multisport-Event der Welt – mit bis zu 30.000 Athleten aus ueber 100 Laendern. Sie richten sich an Sportler ab einem Mindestalter (meist 30-35 Jahre je Sportart) und betonen Teilnahme statt Professionalitaet.",
        difficulty = 2,
        funFact = "Die World Masters Games haben mehr Teilnehmer als die Olympischen Spiele – bei Olympia starten ca. 10.000 Athleten. Die Stimmung wird oft als freudiger und entspannter beschrieben, da nicht Medaillen, sondern Lebensfreude im Vordergrund steht."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Multisport-Event vereint Sportarten aus dem asiatisch-pazifischen Raum und findet alle vier Jahre statt?",
        answerA = "Asiatische Spiele",
        answerB = "Pazifische Spiele",
        answerC = "Asien-Pazifik-Spiele",
        answerD = "Ostasienspiele",
        correctAnswer = 0,
        explanation = "Die Asian Games (Asiatische Spiele) finden seit 1951 alle vier Jahre statt und werden vom Olympic Council of Asia (OCA) organisiert. Mit 45 Mitgliedslaendern und ueber 45 Sportarten sind sie nach den Olympischen Spielen das zweitgroesste Multisport-Event weltweit.",
        difficulty = 2,
        funFact = "Die Asian Games 1982 in Neu-Delhi waren die ersten, bei denen China mit vollem Kontingent teilnahm – und China wurde sofort Dritter im Medaillenspiegel, hinter der UdSSR und der USA."
    ),

    Question(
        categoryId = 6,
        questionText = "Was sind die 'Goodwill Games' und warum wurden sie gegruendet?",
        answerA = "Ein IOC-Programm fuer Laender ohne Olympia-Qualifikation",
        answerB = "Ein Wettkampf, der 1986 als Annaeherung zwischen USA und UdSSR nach dem Olympia-Boykott gegruendet wurde",
        answerC = "Ein Wohltaetigkeitssportevent fuer humanitaere Hilfsorganisationen",
        answerD = "Eine Multisport-Serie des UN-Entwicklungsprogramms",
        correctAnswer = 1,
        explanation = "Die Goodwill Games wurden 1986 von CNN-Gruender Ted Turner ins Leben gerufen, als Reaktion auf die gegenseitigen Olympia-Boykotte von 1980 (USA boycottierte Moskau) und 1984 (UdSSR boycottierte Los Angeles). Sie sollten die US-sowjetischen Sportbeziehungen normalisieren.",
        difficulty = 2,
        funFact = "Die Goodwill Games fanden von 1986 bis 2001 statt – wechselweise in den USA und der UdSSR/Russland. Turner verlor angeblich Hunderte Millionen Dollar mit dem Event, sah es aber als politisches und humanitaeres Investment."
    )
)
