package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun dortmundQuestionsMedium4(): List<Question> = listOf(

    // --- Legendäre Spieler der 1990er ---

    Question(
        categoryId = 14,
        questionText = "Wie viele Bundesliga-Tore erzielte Stéphane Chapuisat in seiner Zeit beim BVB?",
        answerA = "67 Tore",
        answerB = "82 Tore",
        answerC = "54 Tore",
        answerD = "101 Tore",
        correctAnswer = 1,
        explanation = "Stéphane Chapuisat erzielte in seinen Jahren beim BVB (1991–1999) insgesamt 82 Bundesliga-Tore und war damit einer der treffsichersten Stürmer der Vereinsgeschichte.",
        difficulty = 2,
        funFact = "Chapuisat war der erste Schweizer Spieler, der in einem Champions-League-Finale stand – beim BVB-Triumph 1997 gegen Juventus."
    ),

    Question(
        categoryId = 14,
        questionText = "Lars Ricken erzielte im Champions-League-Finale 1997 das 3:1 gegen Juventus. Wie lange war er da erst auf dem Platz?",
        answerA = "16 Sekunden",
        answerB = "32 Sekunden",
        answerC = "51 Sekunden",
        answerD = "4 Minuten",
        correctAnswer = 2,
        explanation = "Lars Ricken traf 51 Sekunden nach seiner Einwechslung mit einem Lupfer über Juventus-Torwart Angelo Peruzzi – eines der ikonischsten Tore der BVB-Geschichte.",
        difficulty = 2,
        funFact = "Ricken spielte seine gesamte Karriere beim BVB und wurde nie an einen anderen Verein verkauft."
    ),

    Question(
        categoryId = 14,
        questionText = "Karl-Heinz Riedle schoss im Champions-League-Finale 1997 zwei Tore. Was war sein Beruf nach der aktiven Karriere beim BVB?",
        answerA = "Sportdirektor beim BVB",
        answerB = "Cheftrainer der BVB-U17",
        answerC = "TV-Experte bei der ARD",
        answerD = "Scout bei Borussia Dortmund",
        correctAnswer = 3,
        explanation = "Karl-Heinz Riedle arbeitete nach seiner aktiven Karriere als Scout für Borussia Dortmund und half dem Verein, Talente zu entdecken.",
        difficulty = 2,
        funFact = "Riedle erzielte im Finale gegen Juventus per Kopf den Ausgleich und das 2:1 – beide Tore nach Flanken von Paul Lambert."
    ),

    Question(
        categoryId = 14,
        questionText = "Andreas Möller gewann mit dem BVB die Champions League 1997. Für welchen anderen deutschen Großklub spielte er ebenfalls mehrere Jahre?",
        answerA = "Bayern München",
        answerB = "Schalke 04",
        answerC = "Borussia Mönchengladbach",
        answerD = "Eintracht Frankfurt",
        correctAnswer = 0,
        explanation = "Andreas Möller spielte sowohl für Borussia Dortmund als auch für Bayern München – als Wechsel zu den Erzrivalen, was bei den BVB-Fans für Unmut sorgte.",
        difficulty = 2,
        funFact = "Möller war DFB-Pokalsieger, Meister und Champions-League-Sieger – jedoch mit verschiedenen deutschen Vereinen."
    ),

    Question(
        categoryId = 14,
        questionText = "Matthias Sammer gewann 1996 den Ballon d'Or. Als welche Position spielte er überwiegend beim BVB?",
        answerA = "Defensives Mittelfeld",
        answerB = "Libero / Ausputzer",
        answerC = "Linker Außenverteidiger",
        answerD = "Hängende Spitze",
        correctAnswer = 1,
        explanation = "Matthias Sammer spielte beim BVB als Libero – eine Position, die er aus seiner Zeit im DDR-Fußball mitbrachte und auf höchstem Niveau ausfüllte.",
        difficulty = 2,
        funFact = "Sammer ist bisher der letzte deutsche Spieler, der den Ballon d'Or gewann – und der einzige, der den Preis als Abwehrspieler holte."
    ),

    Question(
        categoryId = 14,
        questionText = "Stefan Reuter war in den 1990ern BVB-Kapitän. Von welchem Verein wechselte er 1992 zu Borussia Dortmund?",
        answerA = "Bayern München",
        answerB = "Juventus Turin",
        answerC = "FC Nürnberg",
        answerD = "Eintracht Frankfurt",
        correctAnswer = 1,
        explanation = "Stefan Reuter wechselte 1992 von Juventus Turin zu Borussia Dortmund, wo er zum prägenden Kapitän der Meister- und Champions-League-Elf wurde.",
        difficulty = 2,
        funFact = "Reuter gewann in seiner Karriere die WM 1990, die EM 1996 und die Champions League 1997 – eine der vollständigsten Titelsammlungen eines deutschen Spielers."
    ),

    Question(
        categoryId = 14,
        questionText = "Heiko Herrlich erzielte für den BVB in der Saison 1994/95 bemerkenswerte Leistungen. Womit ist er in Fußballdeutschland noch bekannt?",
        answerA = "Jüngster Torschütze in der Bundesliga-Geschichte",
        answerB = "Er überstand als aktiver Spieler einen Hirntumor",
        answerC = "Er traf als Einwechselspieler im WM-Finale 1994",
        answerD = "Er hält den Rekord für die meisten Freistoßtore beim BVB",
        correctAnswer = 1,
        explanation = "Heiko Herrlich erkrankte während seiner aktiven Karriere an einem Hirntumor, kämpfte sich zurück und spielte anschließend noch Jahre im Profifußball.",
        difficulty = 2,
        funFact = "Herrlich wurde später Bundesliga-Trainer, unter anderem bei Bayer Leverkusen und dem FC Augsburg."
    ),

    // --- Klopp-Ära 2010–2015 ---

    Question(
        categoryId = 14,
        questionText = "Mit wie vielen Punkten gewann der BVB die Bundesliga-Saison 2011/12?",
        answerA = "75 Punkte",
        answerB = "81 Punkte",
        answerC = "68 Punkte",
        answerD = "87 Punkte",
        correctAnswer = 1,
        explanation = "Borussia Dortmund holte in der Saison 2011/12 insgesamt 81 Punkte und gewann damit die Meisterschaft deutlich vor Bayern München – ein damaliger Bundesliga-Rekord für den BVB.",
        difficulty = 2,
        funFact = "Der BVB erzielte in dieser Saison 80 Tore in 34 Spielen – ein Schnitt von über 2,3 Toren pro Spiel."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches taktische System setzte Jürgen Klopp beim BVB hauptsächlich ein?",
        answerA = "4-3-3 mit Ballbesitzfußball",
        answerB = "4-4-2 mit zwei Spitzen",
        answerC = "Gegenpressing im 4-2-3-1",
        answerD = "3-5-2 mit Wingbacks",
        correctAnswer = 2,
        explanation = "Klopps BVB spielte vorwiegend im 4-2-3-1 mit intensivem Gegenpressing – sofortiges Rückgewinnen des Balls nach Ballverlust war der taktische Markenkern.",
        difficulty = 2,
        funFact = "Klopp prägte den Begriff 'Gegenpressing' – im englischen Sprachraum wird diese Taktik heute oft als 'Gegenpressing' oder 'Klopp-Press' bezeichnet."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Saison erreichte der BVB unter Klopp das Champions-League-Finale?",
        answerA = "2010/11",
        answerB = "2011/12",
        answerC = "2012/13",
        answerD = "2013/14",
        correctAnswer = 2,
        explanation = "In der Saison 2012/13 erreichte der BVB das Champions-League-Finale in Wembley, das gegen Bayern München mit 1:2 verloren ging.",
        difficulty = 2,
        funFact = "Es war das erste rein deutsche Champions-League-Finale überhaupt – das sogenannte 'Finale dahoam' aus Bayern-Sicht."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer schoss das BVB-Tor im Champions-League-Finale 2013 gegen Bayern?",
        answerA = "Marco Reus",
        answerB = "Ilkay Gündogan",
        answerC = "Robert Lewandowski",
        answerD = "Mario Götze",
        correctAnswer = 1,
        explanation = "Ilkay Gündogan verwandelte in der 75. Minute einen Elfmeter zum zwischenzeitlichen 1:1. Arjen Robben traf in der Nachspielzeit zum 2:1-Endstand für Bayern.",
        difficulty = 2,
        funFact = "Mario Götze war beim Finale nicht mehr dabei – er hatte kurz zuvor seinen Wechsel zu Bayern München bekanntgegeben, was die BVB-Fans sehr schmerzte."
    ),

    Question(
        categoryId = 14,
        questionText = "Robert Lewandowski erzielte im Champions-League-Halbfinale 2013 gegen Real Madrid vier Tore. Was war das Endergebnis des Hinspiels?",
        answerA = "4:0",
        answerB = "4:1",
        answerC = "3:0",
        answerD = "5:2",
        correctAnswer = 1,
        explanation = "Der BVB schlug Real Madrid im Hinspiel des Halbfinales 2012/13 mit 4:1 in Dortmund. Lewandowski erzielte alle vier BVB-Tore – eine historische Einzelleistung.",
        difficulty = 2,
        funFact = "Lewandowski war in dieser Champions-League-Saison mit 10 Toren gemeinsam mit Cristiano Ronaldo der erfolgreichste Torschütze des Turniers."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen BVB-Rekordsieg verbuchte die Mannschaft in der Bundesliga unter Klopp im Januar 2012?",
        answerA = "9:1 gegen den Hamburger SV",
        answerB = "8:0 gegen Bayer Leverkusen",
        answerC = "8:0 gegen Hannover 96",
        answerD = "7:0 gegen den VfB Stuttgart",
        correctAnswer = 2,
        explanation = "Der BVB besiegte Hannover 96 in der Saison 2011/12 mit 8:0 – ein historisches Ergebnis, das die Dominanz der Klopp-Mannschaft in dieser Spielzeit eindrucksvoll belegte.",
        difficulty = 2,
        funFact = "Robert Lewandowski traf in diesem Spiel gleich dreifach und war damit einer der Hauptdarsteller des Kantersiegs."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler war in der Klopp-Ära bekannt als 'Fußballgott' bei den Fans und trug die Rückennummer 11?",
        answerA = "Marco Reus",
        answerB = "Kevin Großkreutz",
        answerC = "Mario Götze",
        answerD = "Jakub Blaszczykowski",
        correctAnswer = 2,
        explanation = "Mario Götze trug beim BVB die Rückennummer 10 – allerdings wurde er von den Fans als 'Fußballgott' bezeichnet. Die Nummer 11 trug Marco Reus.",
        difficulty = 2,
        funFact = "Marco Reus trägt seit seinem Wechsel 2012 die Nummer 11 und ist bis heute einer der treuestens Spieler der Vereinsgeschichte."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Bundesliga-Punkte holte der BVB in der Saison 2014/15 – Klopps letzter Saison?",
        answerA = "46 Punkte",
        answerB = "38 Punkte",
        answerC = "55 Punkte",
        answerD = "42 Punkte",
        correctAnswer = 0,
        explanation = "Klopps letzte Saison 2014/15 war eine der schwächsten seiner BVB-Zeit: Der Verein holte nur 46 Punkte und beendete die Runde auf Platz 7 – ein deutlicher Einbruch.",
        difficulty = 2,
        funFact = "Trotz des schwachen Saisonverlaufs erreichte der BVB unter Klopp das DFB-Pokalfinale 2015, das gegen den VfL Wolfsburg mit 1:3 verloren ging."
    ),

    // --- Internationale Karrieren der BVB-Spieler ---

    Question(
        categoryId = 14,
        questionText = "Matthias Sammer spielte für die DDR-Nationalmannschaft und später für die DFB-Elf. Wie viele Länderspiele machte er insgesamt?",
        answerA = "74 Spiele",
        answerB = "51 Spiele",
        answerC = "23 DDR + 51 DFB = 74 Spiele",
        answerD = "23 DDR + 28 DFB = 51 Spiele",
        correctAnswer = 3,
        explanation = "Sammer bestritt 23 Länderspiele für die DDR und 51 für die wiedervereinigte deutsche Nationalmannschaft, also insgesamt 74 Länderspiele.",
        difficulty = 2,
        funFact = "Sammer war der letzte DDR-Nationalspieler, der in einem offiziellen Länderspiel Deutschlands auflief, bevor die DDR-Mannschaft aufgelöst wurde."
    ),

    Question(
        categoryId = 14,
        questionText = "Stéphane Chapuisat spielte die WM 1994 für die Schweiz. In welcher Runde schied die Schweiz aus?",
        answerA = "Vorrunde",
        answerB = "Achtelfinale",
        answerC = "Viertelfinale",
        answerD = "Halbfinale",
        correctAnswer = 1,
        explanation = "Die Schweiz schied bei der WM 1994 in den USA im Achtelfinale aus – sie verlor gegen Spanien mit 0:3. Chapuisat war einer der Leistungsträger der Schweizer.",
        difficulty = 2,
        funFact = "Chapuisats Vater Yvan spielte ebenfalls für die Schweizer Nationalmannschaft – eine der wenigen Vater-Sohn-Kombinationen in der Schweizer WM-Geschichte."
    ),

    Question(
        categoryId = 14,
        questionText = "Andreas Möller schoss bei der EM 1996 den entscheidenden Elfmeter im Halbfinale gegen England. In welcher Minute traf er?",
        answerA = "Im Elfmeterschießen, als dritter Schütze",
        answerB = "Im Elfmeterschießen, als sechster und letzter Schütze",
        answerC = "Als regulärer Strafstoß in der Verlängerung",
        answerD = "Im Elfmeterschießen, als fünfter Schütze",
        correctAnswer = 1,
        explanation = "Möller verwandelte als sechster und letzter Schütze den entscheidenden Elfmeter und schoss Deutschland ins EM-Finale. England schoss als fünfter Schütze – Southgate verschoss.",
        difficulty = 2,
        funFact = "Möller provozierte die englischen Fans nach seinem Treffer mit einer Jubel-Geste, was ihn in England zur unbeliebtesten Person des Turniers machte."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler der Klopp-Ära nahm an der WM 2014 teil und wurde Weltmeister?",
        answerA = "Mats Hummels",
        answerB = "Marco Reus",
        answerC = "Nuri Sahin",
        answerD = "Ilkay Gündogan",
        correctAnswer = 0,
        explanation = "Mats Hummels war Teil des deutschen WM-Kaders 2014 und wurde mit der Nationalmannschaft Weltmeister. Marco Reus verpasste die WM verletzungsbedingt.",
        difficulty = 2,
        funFact = "Hummels erzielte im Viertelfinale gegen Frankreich das einzige Tor des Spiels per Kopf – ein entscheidender Treffer auf dem Weg zum Titel."
    ),

    Question(
        categoryId = 14,
        questionText = "Jakub Blaszczykowski spielte viele Jahre für den BVB und war polnischer Nationalspieler. Bei welcher EM erzielte er einen denkwürdigen Treffer gegen Deutschland?",
        answerA = "EM 2008",
        answerB = "EM 2012",
        answerC = "EM 2016",
        answerD = "EM 2004",
        correctAnswer = 1,
        explanation = "Bei der EM 2012 in Polen und der Ukraine erzielte Blaszczykowski im Gruppenspiel gegen Deutschland das Tor zum zwischenzeitlichen 1:1 – das Spiel endete 1:2 für Deutschland.",
        difficulty = 2,
        funFact = "Die EM 2012 fand teilweise in Polen statt – für Blaszczykowski war es ein besonders emotionales Turnier vor heimischem Publikum."
    ),

    Question(
        categoryId = 14,
        questionText = "Robert Lewandowski spielte die WM 2018 für Polen. Wie viele Tore erzielte er in der Gruppenphase?",
        answerA = "3 Tore",
        answerB = "2 Tore",
        answerC = "1 Tor",
        answerD = "Kein Tor",
        correctAnswer = 3,
        explanation = "Robert Lewandowski erzielte bei der WM 2018 in der Gruppenphase kein einziges Tor. Polen schied als Letzter der Gruppe H aus – trotz großer Erwartungen.",
        difficulty = 2,
        funFact = "Lewandowski galt als einer der besten Stürmer der Welt, aber die WM 2018 war für ihn und Polen eine herbe Enttäuschung ohne ein einziges Gruppenspieltor."
    ),

    Question(
        categoryId = 14,
        questionText = "Ilkay Gündogan debütierte in der Nationalmannschaft noch als BVB-Spieler. Für welches Land entschied er sich nach Überlegungen?",
        answerA = "Türkei",
        answerB = "Deutschland",
        answerC = "Er besaß nur die deutsche Staatsbürgerschaft",
        answerD = "Er spielte zunächst für die Türkei, wechselte dann zu Deutschland",
        correctAnswer = 1,
        explanation = "Ilkay Gündogan entschied sich trotz türkischer Wurzeln für die deutsche Nationalmannschaft. Er debütierte 2011 und wurde 2014 Weltmeister.",
        difficulty = 2,
        funFact = "Gündogan wurde beim DFB trotz seiner türkischen Herkunft nie besonders kontrovers diskutiert – anders als zum Beispiel Mesut Özil in dieser Hinsicht."
    ),

    // --- Transferverkäufe ---

    Question(
        categoryId = 14,
        questionText = "Für welche Ablösesumme wurde Mario Götze 2013 zu Bayern München transferiert?",
        answerA = "32 Millionen Euro",
        answerB = "37 Millionen Euro",
        answerC = "28 Millionen Euro",
        answerD = "42 Millionen Euro",
        correctAnswer = 1,
        explanation = "Mario Götze wechselte im Sommer 2013 für 37 Millionen Euro von Borussia Dortmund zu Bayern München – eine der spektakulärsten Transfers zwischen deutschen Rivalen.",
        difficulty = 2,
        funFact = "Der Transfer wurde mitten in der laufenden Champions-League-Saison bekannt gegeben – Klopp erfuhr davon kurz vor dem Halbfinale gegen Real Madrid."
    ),

    Question(
        categoryId = 14,
        questionText = "Robert Lewandowski wechselte 2014 ablösefrei zu Bayern München. Warum gab es keine Ablöse?",
        answerA = "BVB verzichtete aus Dankbarkeit auf die Ablöse",
        answerB = "Sein Vertrag war ausgelaufen und er verhandelte bereits im Januar mit Bayern",
        answerC = "Lewandowski hatte eine Ausstiegsklausel in Höhe von null Euro",
        answerD = "Der Transfer scheiterte fast und wurde ohne Ablöse als Kompromiss finalisiert",
        correctAnswer = 1,
        explanation = "Lewandowskis Vertrag beim BVB lief 2014 aus. Ab Januar 2014 durfte er offiziell mit anderen Vereinen verhandeln – Bayern München sicherte sich ihn ablösefrei.",
        difficulty = 2,
        funFact = "Der BVB versuchte verzweifelt, Lewandowski zu halten, doch dieser entschied sich bewusst gegen eine Vertragsverlängerung – und für den Erzrivalen."
    ),

    Question(
        categoryId = 14,
        questionText = "Für welche Summe wurde Mats Hummels 2016 zu Bayern München transferiert?",
        answerA = "25 Millionen Euro",
        answerB = "35 Millionen Euro",
        answerC = "38 Millionen Euro",
        answerD = "30 Millionen Euro",
        correctAnswer = 1,
        explanation = "Mats Hummels wechselte im Sommer 2016 für 35 Millionen Euro von Borussia Dortmund zu Bayern München, kehrte jedoch 2019 wieder zum BVB zurück.",
        difficulty = 2,
        funFact = "Hummels ist einer der wenigen Spieler, die sowohl für BVB als auch Bayern München Meistertitel gewannen – und wieder zum BVB zurückkehrten."
    ),

    Question(
        categoryId = 14,
        questionText = "Ousmane Dembélé wechselte 2017 für eine Rekordablöse zum FC Barcelona. Wie hoch war die Grundablöse?",
        answerA = "105 Millionen Euro",
        answerB = "135 Millionen Euro",
        answerC = "80 Millionen Euro",
        answerD = "95 Millionen Euro",
        correctAnswer = 0,
        explanation = "Ousmane Dembélé wechselte für eine Grundablöse von 105 Millionen Euro zum FC Barcelona, mit Bonuszahlungen von bis zu 45 Millionen Euro – der damalige BVB-Transferrekord.",
        difficulty = 2,
        funFact = "Dembélé wechselte als Ersatz für Neymar, der selbst für 222 Millionen Euro zu PSG gegangen war – eine Transferkette der Superlative."
    ),

    Question(
        categoryId = 14,
        questionText = "Henrikh Mkhitaryan wechselte 2016 zu Manchester United. Was war die Ablösesumme?",
        answerA = "30 Millionen Euro",
        answerB = "42 Millionen Euro",
        answerC = "27 Millionen Euro",
        answerD = "36 Millionen Euro",
        correctAnswer = 1,
        explanation = "Henrikh Mkhitaryan wechselte im Sommer 2016 für rund 42 Millionen Euro zu Manchester United unter Trainer José Mourinho.",
        difficulty = 2,
        funFact = "Mkhitaryan war beim BVB einer der kreativsten Spieler der Saison 2015/16 mit 23 Torbeteiligungen in der Bundesliga."
    ),

    Question(
        categoryId = 14,
        questionText = "Für welchen Verein verließ Ilkay Gündogan den BVB im Sommer 2016?",
        answerA = "FC Liverpool",
        answerB = "Manchester City",
        answerC = "FC Barcelona",
        answerD = "Paris Saint-Germain",
        correctAnswer = 1,
        explanation = "Ilkay Gündogan wechselte ablösefrei zu Manchester City, wo er unter Pep Guardiola zu einem der wichtigsten Mittelfeldspieler Europas wurde.",
        difficulty = 2,
        funFact = "Gündogan verließ den BVB nach einer langen Verletzungspause aufgrund eines Kreuzbandrisses und startete bei City neu durch."
    ),

    Question(
        categoryId = 14,
        questionText = "Für wie viel Millionen Euro wechselte Jude Bellingham 2023 zu Real Madrid?",
        answerA = "80 Millionen Euro",
        answerB = "103 Millionen Euro",
        answerC = "115 Millionen Euro",
        answerD = "88 Millionen Euro",
        correctAnswer = 2,
        explanation = "Jude Bellingham wechselte im Sommer 2023 für 103 Millionen Euro Grundablöse (mit Boni bis zu 130 Millionen Euro) zu Real Madrid – der teuerste Transfer aus dem BVB.",
        difficulty = 2,
        funFact = "Bellingham traf in seiner ersten Real-Saison regelmäßig spektakuläre Tore und gewann umgehend die Champions League und die Meisterschaft."
    ),

    Question(
        categoryId = 14,
        questionText = "Pierre-Emerick Aubameyang wechselte 2018 zu Arsenal. Was war die Ablösesumme?",
        answerA = "52 Millionen Euro",
        answerB = "67 Millionen Euro",
        answerC = "63,75 Millionen Euro",
        answerD = "58 Millionen Euro",
        correctAnswer = 2,
        explanation = "Pierre-Emerick Aubameyang wechselte im Januar 2018 für 63,75 Millionen Euro zu Arsenal – damals der teuerste Winterkauf in der Geschichte des englischen Klubs.",
        difficulty = 2,
        funFact = "Aubameyang war im Jahr zuvor Torschützenkönig der Bundesliga mit 31 Treffern – eine der Hauptgründe für Arsenals Rekordinvestition."
    ),

    // --- BVB-Rekorde ---

    Question(
        categoryId = 14,
        questionText = "Wer ist der BVB-Spieler mit den meisten Pflichtspieltreffern in der Vereinsgeschichte?",
        answerA = "Stéphane Chapuisat",
        answerB = "Alfred Preißler",
        answerC = "August Lenz",
        answerD = "Lothar Emmerich",
        correctAnswer = 1,
        explanation = "Alfred Preißler gilt als historisch treffsicherster Spieler des BVB mit über 240 Toren in der Zeit nach dem Zweiten Weltkrieg – ein Rekord der alten Vereinsgeschichte.",
        difficulty = 2,
        funFact = "Alfred Preißler ist in Dortmund als Legende bekannt. Seiner wurde nach seinem Tod 1995 mit diversen Ehrungen gedacht – inklusive eines Zitats, das bis heute bei BVB-Fans weiterlebt."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer hält den Rekord für die meisten Bundesliga-Einsätze als BVB-Spieler?",
        answerA = "Michael Zorc",
        answerB = "Stefan Reuter",
        answerC = "Karl-Heinz Thielen",
        answerD = "Norbert Dickel",
        correctAnswer = 0,
        explanation = "Michael Zorc bestritt in seiner Karriere als Spieler über 460 Bundesliga-Spiele für Borussia Dortmund – Vereinsrekord. Heute ist er als Sportdirektor tätig.",
        difficulty = 2,
        funFact = "Zorc erzielte als defensiver Mittelfeldspieler überraschend viele Tore – über 150 in Pflichtspielena – und ist damit einer der produktivsten Mittelfeldspieler der BVB-Geschichte."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler debütierte als jüngster Spieler in der Vereinsgeschichte in der Bundesliga?",
        answerA = "Mario Götze",
        answerB = "Youssoufa Moukoko",
        answerC = "Nuri Sahin",
        answerD = "Lars Ricken",
        correctAnswer = 1,
        explanation = "Youssoufa Moukoko debütierte am 21. November 2020 im Alter von 16 Jahren und 1 Tag in der Bundesliga – und wurde damit zum jüngsten Bundesliga-Spieler aller Zeiten.",
        difficulty = 2,
        funFact = "Moukoko durfte erst am Tag nach seinem 16. Geburtstag spielen, weil das Mindestalter für die Bundesliga in dieser Saison von 17 auf 16 Jahre gesenkt worden war."
    ),

    Question(
        categoryId = 14,
        questionText = "Nuri Sahin war einst jüngster Bundesliga-Spieler der Geschichte. In welchem Alter debütierte er für den BVB?",
        answerA = "16 Jahre, 11 Monate",
        answerB = "16 Jahre, 335 Tage",
        answerC = "16 Jahre, 5 Monate",
        answerD = "15 Jahre, 10 Monate",
        correctAnswer = 1,
        explanation = "Nuri Sahin debütierte im Alter von 16 Jahren und 335 Tagen für Borussia Dortmund und hielt damit jahrelang den Rekord als jüngster Bundesliga-Spieler.",
        difficulty = 2,
        funFact = "Sahin wurde 2012 kurz zu Real Madrid transferiert, kehrte aber nicht zur Form zurück und spielte später wieder in der Türkei. Heute ist er Trainer beim BVB."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler erzielte das schnellste Tor in der Geschichte des Signal Iduna Parks?",
        answerA = "Robert Lewandowski nach 9 Sekunden",
        answerB = "Pierre-Emerick Aubameyang nach 8 Sekunden",
        answerC = "Kevin Großkreutz nach 12 Sekunden",
        answerD = "Marco Reus nach 7 Sekunden",
        correctAnswer = 1,
        explanation = "Pierre-Emerick Aubameyang erzielte eines der schnellsten Tore in der Geschichte des BVB-Stadions – innerhalb weniger Sekunden nach Anpfiff – in einem Bundesliga-Spiel.",
        difficulty = 2,
        funFact = "Aubameyang war für seine explosiven Sprints bekannt und wurde mehrmals als schnellster Spieler der Bundesliga gemessen."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Bundesliga-Meisterschaften gewann Borussia Dortmund in seiner Geschichte insgesamt (Stand 2024)?",
        answerA = "5 Meisterschaften",
        answerB = "6 Meisterschaften",
        answerC = "8 Meisterschaften",
        answerD = "10 Meisterschaften",
        correctAnswer = 2,
        explanation = "Borussia Dortmund gewann bis 2024 insgesamt 8 Deutsche Meisterschaften: 1956, 1957, 1963, 1995, 1996, 2002, 2011 und 2012.",
        difficulty = 2,
        funFact = "Die ersten drei Meisterschaften (1956, 1957, 1963) wurden noch vor Gründung der Bundesliga gewonnen – in der alten Deutschen Meisterschaft."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Bundesliga-Heimrekord hält der BVB an der Stätte Signal Iduna Park?",
        answerA = "Meiste Heimsiege in Folge (25)",
        answerB = "Höchste Zuschauerzahl aller Bundesliga-Stadien",
        answerC = "Längste Heim-Unbesiegbarkeit (35 Spiele)",
        answerD = "Niedrigste Heimniederlagenanfälligkeit (0,2 pro Saison)",
        correctAnswer = 1,
        explanation = "Das Signal Iduna Park ist mit 81.365 Plätzen das größte Fußballstadion Deutschlands und hat die höchste Kapazität aller Bundesliga-Spielstätten.",
        difficulty = 2,
        funFact = "Im Europacup fasst das Stadion nur rund 66.000 Zuschauer, da Stehplätze in Sitzplätze umgewandelt werden müssen – trotzdem bleibt die Atmosphäre legendär."
    ),

    Question(
        categoryId = 14,
        questionText = "Was war die höchste Bundesliga-Niederlage in der Geschichte des BVB?",
        answerA = "0:9 gegen Borussia Mönchengladbach",
        answerB = "0:8 gegen Bayern München",
        answerC = "1:9 gegen Bayern München",
        answerD = "0:7 gegen Schalke 04",
        correctAnswer = 2,
        explanation = "Borussia Dortmund erlitt gegen Bayern München eine 1:9-Niederlage – eine der schlimmsten Bundesliga-Niederlagen der Vereinsgeschichte.",
        difficulty = 2,
        funFact = "Trotz historischer Tiefpunkte konnte der BVB in späteren Jahren sogar Doppelmeister werden – der Fußball schreibt immer wieder neue Geschichten."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Rückennummer trug Michael Zorc während seiner gesamten Spielerkarriere beim BVB?",
        answerA = "Nummer 8",
        answerB = "Nummer 11",
        answerC = "Keine feste Rückennummer (vor der Einführung fester Nummern)",
        answerD = "Nummer 10",
        correctAnswer = 2,
        explanation = "Michael Zorc spielte in einer Ära, als in der Bundesliga noch keine festen Rückennummern vergeben wurden – die Nummern wurden pro Spiel nach Position zugeteilt.",
        difficulty = 2,
        funFact = "Feste Rückennummern wurden in der Bundesliga erst 1995/96 eingeführt – ab diesem Zeitpunkt trugen Spieler dauerhaft eine Nummer."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Stadion war die Heimstätte des BVB, bevor das heutige Westfalenstadion (Signal Iduna Park) gebaut wurde?",
        answerA = "Rote-Erde-Stadion",
        answerB = "Hohenbuschei-Stadion",
        answerC = "Stadion Brackel",
        answerD = "Kampfbahn Hohenbuschei",
        correctAnswer = 0,
        explanation = "Das Rote-Erde-Stadion war die Heimstätte des BVB, bevor das Westfalenstadion 1974 zur WM fertiggestellt wurde. Es liegt direkt neben dem Signal Iduna Park.",
        difficulty = 2,
        funFact = "Das Rote-Erde-Stadion existiert noch heute und wird von der BVB-Nachwuchsabteilung sowie für Leichtathletik-Veranstaltungen genutzt."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Torwart hält den Vereinsrekord für die meisten Pflichtspiele ohne Gegentor in Folge?",
        answerA = "Roman Weidenfeller",
        answerB = "Stefan Klos",
        answerC = "Jens Lehmann",
        answerD = "Dede",
        correctAnswer = 0,
        explanation = "Roman Weidenfeller hielt in der Klopp-Ära bemerkenswerte Serien ohne Gegentor und ist der Torwart mit den meisten BVB-Pflichtspiel-Einsätzen der jüngeren Geschichte.",
        difficulty = 2,
        funFact = "Weidenfeller war trotz seiner starken BVB-Leistungen lange kein Nationalspieler – sein erstes Länderspiel für Deutschland kam erst mit 32 Jahren."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr gewann Borussia Dortmund seinen ersten DFB-Pokal?",
        answerA = "1963",
        answerB = "1965",
        answerC = "1972",
        answerD = "1989",
        correctAnswer = 3,
        explanation = "Den ersten DFB-Pokal gewann Borussia Dortmund erst 1989 – vergleichsweise spät für einen Verein dieser Größe. Das Finale gegen Werder Bremen endete 4:1.",
        difficulty = 2,
        funFact = "Trotz früher Bundesliga-Meisterschaften dauerte es bis 1989, bis der BVB erstmals den DFB-Pokal in die Höhe stemmen konnte."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler war in der Saison 2015/16 mit 25 Toren Bundesliga-Torschützenkönig?",
        answerA = "Marco Reus",
        answerB = "Henrikh Mkhitaryan",
        answerC = "Pierre-Emerick Aubameyang",
        answerD = "Shinji Kagawa",
        correctAnswer = 2,
        explanation = "Pierre-Emerick Aubameyang wurde in der Saison 2015/16 mit 25 Toren Torschützenkönig der Bundesliga. Im Folgejahr verbesserte er sich sogar auf 31 Treffer.",
        difficulty = 2,
        funFact = "Aubameyang ist einer von sehr wenigen Spielern, der den Torschützenkönig-Titel in zwei aufeinanderfolgenden Bundesliga-Saisons holen konnte."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer ist der BVB-Spieler mit den meisten Toren in Champions-League-Spielen für Dortmund?",
        answerA = "Robert Lewandowski",
        answerB = "Pierre-Emerick Aubameyang",
        answerC = "Erling Haaland",
        answerD = "Stéphane Chapuisat",
        correctAnswer = 2,
        explanation = "Erling Haaland erzielte in nur 67 BVB-Pflichtspielen insgesamt 86 Tore – darunter sehr viele in der Champions League. Er ist BVBs treffsicherster Torjäger in der Königsklasse.",
        difficulty = 2,
        funFact = "Haaland erzielte in der Champions-League-Saison 2020/21 zehn Tore in nur acht Spielen für den BVB – eine Trefferquote, die nahezu unerreicht ist."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler erzielte in der Saison 1995/96 die meisten Bundesliga-Tore für den Verein?",
        answerA = "Heiko Herrlich",
        answerB = "Stéphane Chapuisat",
        answerC = "Karl-Heinz Riedle",
        answerD = "Fredi Bobic",
        correctAnswer = 1,
        explanation = "Stéphane Chapuisat war in der Meistersaison 1995/96 einer der erfolgreichsten BVB-Torschützen und erzielte entscheidende Tore auf dem Weg zum Titel.",
        difficulty = 2,
        funFact = "Der BVB gewann die Bundesliga 1995/96 unter Otto Rehhagel – dem Trainer, der später mit Griechenland die EM 2004 gewann."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt der Fan-Block im Signal Iduna Park, der für die außergewöhnliche Stimmung bekannt ist?",
        answerA = "Ostkurve",
        answerB = "Südtribüne",
        answerC = "Nordkurve",
        answerD = "Westtribüne",
        correctAnswer = 1,
        explanation = "Die Südtribüne des Signal Iduna Parks ist mit rund 25.000 Stehplätzen die größte Stehplatztribüne Europas und berühmt für ihre lautstarke Atmosphäre.",
        difficulty = 2,
        funFact = "Die Südtribüne, im BVB-Jargon 'Die Wand' genannt, wurde sogar von internationalen Fachmedien zur eindrucksvollsten Stadiontribüne Europas gewählt."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Platz belegte der BVB in der Bundesliga-Saison 2013/14 – dem Jahr nach dem Champions-League-Finale?",
        answerA = "Meister",
        answerB = "Vizemeister",
        answerC = "Dritter",
        answerD = "Vierter",
        correctAnswer = 1,
        explanation = "Der BVB wurde in der Saison 2013/14 Vizemeister hinter Bayern München. Die Saison war nach dem Wembley-Finale eine Art Konsolidierungsphase.",
        difficulty = 2,
        funFact = "Bayern München holte in der Saison 2013/14 saghafte 90 Punkte – ein bis dahin nie dagewesener Bundesliga-Rekord."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Trainer holte nach Klopp die Meistertitel 1995 und 1996?",
        answerA = "Ottmar Hitzfeld",
        answerB = "Nevio Scala",
        answerC = "Dettmar Cramer",
        answerD = "Peter Neururer",
        correctAnswer = 0,
        explanation = "Ottmar Hitzfeld trainierte Borussia Dortmund von 1991 bis 1997 und holte zwei Bundesliga-Meisterschaften sowie die Champions League 1997.",
        difficulty = 2,
        funFact = "Hitzfeld trainierte danach Bayern München und gewann auch dort die Champions League 2001 – er ist einer der wenigen Trainer mit zwei verschiedenen CL-Titeln."
    ),

    Question(
        categoryId = 14,
        questionText = "Stéphane Chapuisat wechselte 1999 vom BVB zu welchem Verein?",
        answerA = "Grasshopper Club Zürich",
        answerB = "BSC Young Boys",
        answerC = "FC Zürich",
        answerD = "Lausanne-Sport",
        correctAnswer = 1,
        explanation = "Stéphane Chapuisat kehrte 1999 in seine Schweizer Heimat zurück und wechselte zu den BSC Young Boys in Bern, wo er seine Karriere ausklingen ließ.",
        difficulty = 2,
        funFact = "Chapuisat ist in der Schweiz bis heute eine Fußball-Ikone – er wird als bester Schweizer Stürmer seiner Generation betrachtet."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Rekordergebnis erzielte der BVB in der Bundesliga-Saison 2011/12 in Bezug auf Gegentore?",
        answerA = "Nur 22 Gegentore in 34 Spielen",
        answerB = "Nur 18 Gegentore in 34 Spielen",
        answerC = "Nur 25 Gegentore in 34 Spielen",
        answerD = "Nur 30 Gegentore in 34 Spielen",
        correctAnswer = 0,
        explanation = "In der Meistersaison 2011/12 kassierte der BVB nur 22 Gegentore in 34 Spielen – eine der defensiv stärksten Leistungen der Bundesliga-Geschichte.",
        difficulty = 2,
        funFact = "Die Kombination aus 80 erzielten und nur 22 kassierten Toren ergab eine Tordifferenz von +58 – ein außergewöhnlicher Wert für die Bundesliga."
    ),

)
