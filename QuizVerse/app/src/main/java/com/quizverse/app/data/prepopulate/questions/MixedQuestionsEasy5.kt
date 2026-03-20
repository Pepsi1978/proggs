package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsEasy5(): List<Question> = listOf(

    // Question 1 — Neujahr: Spanien Trauben
    Question(
        categoryId = 11,
        questionText = "Was essen Spanier traditionell zu Silvester bei jedem Glockenschlag um Mitternacht?",
        answerA = "Eine Olive",
        answerB = "Eine Traube",
        answerC = "Eine Mandel",
        answerD = "Eine Kirsche",
        correctAnswer = 1,
        explanation = "In Spanien ist es Tradition, bei jedem der zwoelf Glockenschlaege zu Mitternacht eine Traube zu essen. Wer alle zwoelf Trauben schafft, dem sollen sich alle Wuensche erfuellen. Dafuer haben die Spanier nur 36 Sekunden Zeit.",
        difficulty = 1,
        funFact = "Die Tradition der zwoelf Trauben entstand angeblich 1909, als Weinbauern in Alicante eine Rekordernte hatten und geschickt eine neue Tradition erfanden, um den Ueberschuss zu verkaufen."
    ),

    // Question 2 — Neujahr: Japan Glocken
    Question(
        categoryId = 11,
        questionText = "Wie viele Mal lauten die Tempel-Glocken in Japan zu Silvester um Mitternacht?",
        answerA = "12 Mal",
        answerB = "50 Mal",
        answerC = "108 Mal",
        answerD = "365 Mal",
        correctAnswer = 2,
        explanation = "In Japan schlagen die Tempel zu Silvester exakt 108 Mal die Glocken. Diese Zahl steht im Buddhismus fuer die 108 Begierden und Uebel des Menschen, die durch jeden Schlag symbolisch vertrieben werden.",
        difficulty = 1,
        funFact = "Das japanische Silvester-Ritual heisst Joya no Kane. Die Menschen hoeren die Glocken in stiller Besinnung — ganz anders als das laute Feuerwerk bei uns."
    ),

    // Question 3 — Neujahr: Daenemark Geschirr
    Question(
        categoryId = 11,
        questionText = "Was tun Daenen zu Silvester traditionell vor die Haustuere ihrer Freunde?",
        answerA = "Sie legen Blumen hin",
        answerB = "Sie schreiben Wunschzettel",
        answerC = "Sie werfen altes Geschirr davor",
        answerD = "Sie haengen Laternen auf",
        correctAnswer = 2,
        explanation = "In Daenemark ist es Brauch, zu Silvester altes Porzellan und Geschirr vor die Haustuere von Freunden und Nachbarn zu werfen. Je mehr Scherben vor einer Tuer liegen, desto mehr Glueck soll es dem Hausbesitzer im neuen Jahr bringen.",
        difficulty = 1,
        funFact = "Manche Daenen sammeln das ganze Jahr ueber kaputtes Geschirr nur fuer diesen Anlass. Wer am Neujahrsmorgen den groessten Scherbenhaufen vor der Tuer findet, gilt als besonders beliebt."
    ),

    // Question 4 — Neujahr: Brasilien Wellen
    Question(
        categoryId = 11,
        questionText = "Wie viele Wellen muessen Brasilianer am Meer zu Silvester springen, um Wuensche freizuschalten?",
        answerA = "3 Wellen",
        answerB = "5 Wellen",
        answerC = "7 Wellen",
        answerD = "9 Wellen",
        correctAnswer = 2,
        explanation = "In Brasilien ist es Tradition, zu Silvester am Meer genau sieben anrollende Wellen zu ueberspringen. Fuer jeden gelungenen Sprung darf man sich einen Wunsch fuer das neue Jahr erbitten.",
        difficulty = 1,
        funFact = "Die Brasilianer tragen dabei traditionell weisse Kleidung als Zeichen fuer Frieden und Glueck. Die Feier heisst Reveillon und ist vor allem am Strand von Copacabana in Rio beeindruckend."
    ),

    // Question 5 — Neujahr: Griechenland Kuchen
    Question(
        categoryId = 11,
        questionText = "Was ist in Griechenland im Neujahrs-Kuchen Vasilopita versteckt?",
        answerA = "Eine Muenze",
        answerB = "Ein Zettel mit einem Wunsch",
        answerC = "Ein Zuckerstueck",
        answerD = "Ein kleines Kreuz",
        correctAnswer = 0,
        explanation = "Im griechischen Neujahrskuchen Vasilopita ist eine Goldmuenze eingebacken. Wer das Stueck mit der Muenze erhaelt, dem wird im neuen Jahr besonders viel Glueck versprochen.",
        difficulty = 1,
        funFact = "Vasilopita bedeutet Basilius-Kuchen und ist nach dem Heiligen Basilius benannt, dessen Namenstag am 1. Januar ist. Der Kuchen wird genau um Mitternacht angeschnitten."
    ),

    // Question 6 — Weihnachten: Island Monster
    Question(
        categoryId = 11,
        questionText = "Welches Monster soll laut islaendischer Weihnachtstradition faule Menschen fressen?",
        answerA = "Der Weihnachtstroll",
        answerB = "Der Jolakotturinn (Weihnachtskater)",
        answerC = "Der Gryla",
        answerD = "Der Skrattur",
        correctAnswer = 1,
        explanation = "In Island treibt zu Weihnachten der Jolakotturinn (der Weihnachtskater) sein Unwesen. Laut Tradition frisst dieses riesige Katzenmonster Menschen, die zu Weihnachten keine neue Kleidung bekommen haben — also faule Menschen.",
        difficulty = 1,
        funFact = "Wegen des Jolakotturinn schenken Islaender einander traditionell neue Kleidung zu Weihnachten. So ist jeder sicher vor dem Monster. Der Brauch ist heute noch lebendig."
    ),

    // Question 7 — Weihnachten: Australien
    Question(
        categoryId = 11,
        questionText = "Wie feiern viele Australier Weihnachten traditionell, da es dort Hochsommer ist?",
        answerA = "Mit einem Ski-Ausflug in die Berge",
        answerB = "Mit einem Barbecue im Garten oder am Strand",
        answerC = "Mit einer Bootsfahrt auf dem See",
        answerD = "Mit einem Picknick im Park bei Kerzenlicht",
        correctAnswer = 1,
        explanation = "In Australien ist Weihnachten mitten im Hochsommer. Deshalb verbringen viele Familien den 25. Dezember traditionell im Garten oder am Strand mit einem Barbecue — statt mit Punsch und Kerzenschein wie bei uns.",
        difficulty = 1,
        funFact = "Der australische Weihnachtsmann schwitzt in der Hitze und faehrt manchmal auf einem Surfbrett statt einem Schlitten. Im Hafen von Sydney gibt es jedes Jahr ein riesiges Feuerwerk zum Fest."
    ),

    // Question 8 — Weihnachten: Polen
    Question(
        categoryId = 11,
        questionText = "Wie viele verschiedene Gerichte werden in Polen traditionell am Heiligabend serviert?",
        answerA = "3 Gerichte",
        answerB = "7 Gerichte",
        answerC = "12 Gerichte",
        answerD = "24 Gerichte",
        correctAnswer = 2,
        explanation = "In Polen werden am Heiligabend traditionell genau zwoelf Gerichte serviert — eines fuer jeden Apostel. Fleisch ist dabei nicht erlaubt. Typische Gerichte sind Barszcz (rote-Bete-Suppe), Karpfen und Pierogi.",
        difficulty = 1,
        funFact = "In Polen wird am Heiligabend auch ein leerer Platz am Tisch gedeckt — fuer einen unerwarteten Gast oder zur Erinnerung an verstorbene Familienmitglieder."
    ),

    // Question 9 — Weihnachten: Frankreich Glocken
    Question(
        categoryId = 11,
        questionText = "Wohin reisen laut franzoesischer Volkserzaehlung die Kirchenglocken von Gruendonnerstag bis Karsamstag?",
        answerA = "Nach Bethlehem",
        answerB = "Nach Jerusalem",
        answerC = "Nach Rom",
        answerD = "Nach Paris",
        correctAnswer = 2,
        explanation = "In Frankreich erzaehlt man Kindern, dass die Kirchenglocken von Gruendonnerstag bis Karsamstag still sind, weil sie nach Rom geflogen sind, um den Papst zu besuchen. Wenn sie zurueckkommen, bringen sie als Souvenir die Ostereier mit.",
        difficulty = 1,
        funFact = "Deshalb schweigen in Frankreich die Kirchenglocken in der Karwoche wirklich. Am Ostersonntag lauten sie umso lauter, um ihre Rueckkehr aus Rom zu feiern."
    ),

    // Question 10 — Ostern: Finnland Birkenrute
    Question(
        categoryId = 11,
        questionText = "Was tun Kinder in Finnland am Palmsonntag traditionell mit Birkenruten?",
        answerA = "Sie dekorieren damit den Osterstrauss",
        answerB = "Sie bewerfen Freunde und Familie damit",
        answerC = "Sie schlagen damit leicht Freunde und Familie, um Glueck zu bringen",
        answerD = "Sie verbrennen sie als Symbol fuer den Winter",
        correctAnswer = 2,
        explanation = "In Finnland schlagen Kinder am Palmsonntag Freunde und Familie leicht mit Birkenruten. Das soll Glueck fuer das kommende Jahr bringen. Der Brauch symbolisiert die Palmwedel, mit denen Jesus bei seinem Einzug in Jerusalem begruessen wurde.",
        difficulty = 1,
        funFact = "Die Kinder verkleiden sich dabei oft als Hexen und erhalten im Austausch fuer ihre Glueckswuensche Suessigkeiten — aehnlich wie Halloween, aber zu Ostern."
    ),

    // Question 11 — Ostern: Bulgarien Eier
    Question(
        categoryId = 11,
        questionText = "Was machen Bulgaren zu Ostern mit bunten Ostereiern, anstatt sie zu verstecken?",
        answerA = "Sie rollen sie einen Huegel hinunter",
        answerB = "Sie werfen sie gegeneinander, um zu sehen welches nicht zerbricht",
        answerC = "Sie schmelzen sie ein und giessen neue Formen",
        answerD = "Sie bemalen sie mit dem Namen des Gluecksbringers",
        correctAnswer = 1,
        explanation = "In Bulgarien werden Ostereier nicht im Garten versteckt, sondern gegeneinander geworfen. Wessen Ei dabei nicht zerbricht, gilt als Sieger und soll im kommenden Jahr besonders viel Erfolg und Glueck haben.",
        difficulty = 1,
        funFact = "Das erste Osterei des Jahres wird in Bulgarien traditionell rot bemalt. Die Hausherrin reibt damit die Wangen der Kinder ein, damit sie gesund und rosig bleiben."
    ),

    // Question 12 — Ostern: Polen nasser Montag
    Question(
        categoryId = 11,
        questionText = "Was passiert in Polen am Ostermontag beim Brauch des Smigus-Dyngus?",
        answerA = "Man zuendet Lagerfeuer an",
        answerB = "Man ueberschuettet sich gegenseitig mit Wasser",
        answerC = "Man wirft Mehl auf Freunde",
        answerD = "Man tanzt um einen geschmueckten Baum",
        correctAnswer = 1,
        explanation = "Am polnischen Ostermontag feiern die Menschen Smigus-Dyngus (den Nassen Montag). Dabei ueberschuetten Maenner traditionell Frauen mit Wasser. Heute werfen alle einander gegenseitig Wasser, was zu riesigen Wasserschlachten fuehrt.",
        difficulty = 1,
        funFact = "Der Ursprung des Brauchs liegt im Mittelalter. Wer am Ostermontag nass wird, soll gluecklich und gesund sein. In manchen Regionen Polens ist es Ehrensache, komplett durchnaesst heimzukommen."
    ),

    // Question 13 — Feste: Holi Indien
    Question(
        categoryId = 11,
        questionText = "Was feiern die Menschen beim indischen Holi-Fest?",
        answerA = "Das Ende des Monsuns",
        answerB = "Den Beginn der Erntezeit",
        answerC = "Den Fruehlingsbeginn",
        answerD = "Den Geburtstag des Gottes Vishnu",
        correctAnswer = 2,
        explanation = "Das Holi-Fest in Indien feiert den Beginn des Fruehlings. Jung und Alt versammeln sich auf den Strassen und bewerfen sich mit bunten Farben und Wasser. Es ist eines der farbenfrohsten Feste der Welt.",
        difficulty = 1,
        funFact = "Holi gilt als das aelteste Fest Indiens. Die Farben bei Holi haben eine Bedeutung: Rot steht fuer Liebe, Blau fuer Krishna, Gelb fuer Glueck und Gruen fuer neues Leben."
    ),

    // Question 14 — Feste: La Tomatina Spanien
    Question(
        categoryId = 11,
        questionText = "In welchem Land findet das Fest La Tomatina statt, bei dem Tomaten geworfen werden?",
        answerA = "Italien",
        answerB = "Portugal",
        answerC = "Mexiko",
        answerD = "Spanien",
        correctAnswer = 3,
        explanation = "La Tomatina findet jedes Jahr im August im spanischen Ort Bunol in der Region Valencia statt. Tausende Menschen bewerfen sich dabei gegenseitig mit Tomaten. Am Ende ist die gesamte Stadt rot eingefaerbt.",
        difficulty = 1,
        funFact = "Bei La Tomatina werden jedes Jahr ueber 100.000 Kilogramm Tomaten verworfen. Damit die Tomaten nicht so hart sind und niemanden verletzen, werden sie vorher leicht zerquetscht."
    ),

    // Question 15 — Feste: Dia de los Muertos
    Question(
        categoryId = 11,
        questionText = "An welchen Tagen feiert Mexiko das Fest Dia de los Muertos (Tag der Toten)?",
        answerA = "Am 31. Oktober und 1. November",
        answerB = "Am 1. und 2. November",
        answerC = "Am 2. und 3. November",
        answerD = "Am 31. Oktober und 1. Dezember",
        correctAnswer = 1,
        explanation = "Der mexikanische Dia de los Muertos wird am 1. und 2. November gefeiert. Familien stellen bunte Altaere mit Fotos der Verstorbenen auf, zuenden Kerzen an und bringen deren Lieblingsessen zum Grab.",
        difficulty = 1,
        funFact = "Der Dia de los Muertos ist KEIN trauriges Fest, sondern eine froehliche Feier. Man glaubt, dass die Seelen der Toten an diesen Tagen zurueckkehren und mit den Lebenden feiern koennen."
    ),

    // Question 16 — Hochzeit: Deutschland Baumstamm
    Question(
        categoryId = 11,
        questionText = "Was muessen Braut und Braeutigam bei einer deutschen Hochzeit traditionell gemeinsam durchsaegen?",
        answerA = "Ein Seil",
        answerB = "Ein Brett",
        answerC = "Einen Baumstamm",
        answerD = "Eine Planke",
        correctAnswer = 2,
        explanation = "Bei vielen deutschen Hochzeiten saegen Braut und Braeutigam gemeinsam mit einer Zwei-Mann-Saege einen Baumstamm durch. Der Brauch symbolisiert, dass das Paar Herausforderungen gemeinsam anpackt und im Team loest.",
        difficulty = 1,
        funFact = "Wer beim Baumstamm-Saegen den Rhythmus vergibt, schnappt und zieht statt gleichmaessig zu saegen. Das Spektakel macht Hochzeitsgesellschaften viel Freude, da man dabei sieht, ob das Paar schon harmoniert."
    ),

    // Question 17 — Hochzeit: Suedkorea Fussschlagen
    Question(
        categoryId = 11,
        questionText = "Was passiert in Suedkorea nach der Hochzeit mit den Fuessen des Braeutigams?",
        answerA = "Sie werden mit roter Farbe bemalt",
        answerB = "Sie werden mit getrockneten Fischen geschlagen",
        answerC = "Sie werden mit Honig eingerieben",
        answerD = "Sie werden mit Wasser gewaschen",
        correctAnswer = 1,
        explanation = "In Suedkorea werden dem Braeutigam nach der Hochzeitszeremonie die Fuesse gefesselt und dann mit getrockneten Fischen oder Stoecken leicht geschlagen. Der Brauch soll die Potenz des Maenners testen und ihn auf die Ehe vorbereiten.",
        difficulty = 1,
        funFact = "Der Brauch klingt schmerzhaft, gilt aber als freundschaftlicher Spass. Freunde und Familie des Braeutigams sind dabei und lachen gemeinsam. Der Brauch heisst Faldarigi."
    ),

    // Question 18 — Hochzeit: Schottland Dirty Bride
    Question(
        categoryId = 11,
        questionText = "Was passiert bei der schottischen Hochzeitstradition Blackening of the Bride mit der Braut?",
        answerA = "Sie wird mit schwarzer Farbe geschminkt",
        answerB = "Sie wird mit Schmutz, faulen Eiern und anderen Substanzen beworfen",
        answerC = "Sie muss in einem schwarzen Kleid tanzen",
        answerD = "Sie wird in einem schwarzen Tuch durch die Stadt getragen",
        correctAnswer = 1,
        explanation = "Beim schottischen Blackening of the Bride wird die Braut mit allem moeglichen Schmutz beworfen: faule Eier, saure Milch, Fischsosse und mehr. Dann wird sie oft gefesselt und durch die Strassen gefuehrt. Man glaubt, wer das ertraegt, haelt auch die Ehe durch.",
        difficulty = 1,
        funFact = "Der Brauch gilt als gutes Omen: Je schlimmer das Blackening, desto gluecklicher die Ehe. Manche Braeute bestehen darauf, den Brauch durchzufuehren, obwohl er sehr unangenehm ist."
    ),

    // Question 19 — Hochzeit: Borneo Tidong
    Question(
        categoryId = 11,
        questionText = "Was duerfen Brautpaare beim Volk der Tidong auf Borneo drei Tage vor der Hochzeit nicht tun?",
        answerA = "Das Haus verlassen",
        answerB = "Mit der Familie sprechen",
        answerC = "Die Toilette benutzen",
        answerD = "Essen und trinken",
        correctAnswer = 2,
        explanation = "Beim Volk der Tidong auf Borneo duerfen Braut und Braeutigam in den drei Tagen vor der Hochzeit die Toilette nicht benutzen. Man glaubt, dass wer diesen Brauch missachtet, Pech in der Ehe haben wird, zum Beispiel den fruehen Tod eines Kindes.",
        difficulty = 1,
        funFact = "Die Eltern der Verlobten passen waehrend dieser drei Tage streng auf, dass die Regel eingehalten wird. Das Paar darf in dieser Zeit sehr wenig essen und trinken."
    ),

    // Question 20 — Neujahr: Thailand Songkran
    Question(
        categoryId = 11,
        questionText = "Wann beginnt in Thailand das Neujahrsfest Songkran, das fuer die groesste Wasserschlacht der Welt bekannt ist?",
        answerA = "1. Januar",
        answerB = "13. April",
        answerC = "1. Maerz",
        answerD = "20. Juni",
        correctAnswer = 1,
        explanation = "Das thailaendische Neujahrsfest Songkran beginnt am 13. April und dauert bis zum 15. April. In dieser Zeit verwandelt sich das ganze Land in eine riesige Wasserschlacht — Menschen ueberall bespritzen sich gegenseitig mit Wasser.",
        difficulty = 1,
        funFact = "Das Wasser bei Songkran symbolisiert Reinheit und Neuanfang. Urspruenglich wurde Wasser nur sanft auf die Haende aelterer Menschen gegossen als Zeichen des Respekts. Heute ist es ein froehliches Volksfest."
    ),

    // Question 21 — Feste: Oktoberfest
    Question(
        categoryId = 11,
        questionText = "Was war der Anlass fuer das allererste Oktoberfest im Jahr 1810?",
        answerA = "Ein Siegesfest nach einer gewonnenen Schlacht",
        answerB = "Die Hochzeit von Kronprinz Ludwig und Prinzessin Therese",
        answerC = "Eine besonders gute Bierernte",
        answerD = "Die Grundsteinlegung des Muenchner Rathauses",
        correctAnswer = 1,
        explanation = "Das allererste Oktoberfest am 12. Oktober 1810 war eine Hochzeitsfeier: Es wurde zur Feier der Hochzeit von Kronprinz Ludwig von Bayern und Prinzessin Therese von Sachsen-Hildburghausen veranstaltet. Die Wiese heisst bis heute Theresienwiese.",
        difficulty = 1,
        funFact = "Beim ersten Oktoberfest gab es noch keine Bierzelte. Die Hauptattraktion war ein Pferderennen. Erst spaeter wurden Bier und Speisen zum Mittelpunkt des Fests."
    ),

    // Question 22 — Feste: Chinesisches Neujahr
    Question(
        categoryId = 11,
        questionText = "Welche Farbe traegt man in China zu Neujahr, um das Monster Nian zu verscheuchen?",
        answerA = "Blau",
        answerB = "Gelb",
        answerC = "Weiss",
        answerD = "Rot",
        correctAnswer = 3,
        explanation = "Beim chinesischen Neujahrsfest traegt man traditionell rote Kleidung, denn die Farbe Rot soll das mythische Monster Nian verscheuchen, das Angst vor der Farbe Rot hat. Auch Feuerwerksmacher und rote Laternen dienen diesem Zweck.",
        difficulty = 1,
        funFact = "Das chinesische Neujahr wird nicht am 1. Januar, sondern nach dem Mondkalender gefeiert und faellt jedes Jahr auf einen anderen Tag zwischen Januar und Februar. Die Feiern dauern 15 Tage."
    ),

    // Question 23 — Silvester: Bleigiessen
    Question(
        categoryId = 11,
        questionText = "Was macht man in Deutschland traditionell zu Silvester beim Bleigiessen?",
        answerA = "Man schmilzt Blei und deutet die Formen als Orakel fuer das neue Jahr",
        answerB = "Man schmilzt Blei und giesst Souvenirs fuer Freunde",
        answerC = "Man vergrabt Blei im Garten als Gluecksbringer",
        answerD = "Man giesst Blei in den Schornstein",
        correctAnswer = 0,
        explanation = "Beim deutschen Silvester-Brauch des Bleigiesens schmilzt man Blei in einem Loeffel ueber einer Kerze und giesst es dann in Wasser. Die entstehenden Formen werden als Orakel gedeutet und sagen angeblich das neue Jahr voraus.",
        difficulty = 1,
        funFact = "Aus Sicherheitsgruenden wird seit 2018 in Deutschland kein echtes Blei mehr verkauft. Stattdessen gibt es Zinn-Kits fuer diesen Brauch. Die Form eines Ankers bedeutet Hoffnung, ein Herz steht fuer Liebe."
    ),

    // Question 24 — Nikolaus Tradition
    Question(
        categoryId = 11,
        questionText = "In welchem Land bringt der Nikolaus Geschenke mit einem Schiff statt mit einem Schlitten?",
        answerA = "Schweden",
        answerB = "Norwegen",
        answerC = "Den Niederlanden",
        answerD = "Finnland",
        correctAnswer = 2,
        explanation = "In den Niederlanden kommt Sinterklaas (der niederlaendische Nikolaus) traditionell mit einem Schiff aus Spanien an. Kinder stellen ihre Schuhe mit Moehren fuer sein Pferd Amerigo an den Kamin.",
        difficulty = 1,
        funFact = "Sinterklaas kommt laut Tradition aus Madrid in Spanien. Kinder stellen nicht nur Schuhe hin, sondern singen auch Lieder, damit Sinterklaas weiss, dass sie brav waren."
    ),

    // Question 25 — Weihnachten: Japan KFC
    Question(
        categoryId = 11,
        questionText = "Was essen Millionen Japaner traditionell an Weihnachten?",
        answerA = "Sushi",
        answerB = "Fried Chicken von KFC",
        answerC = "Ramen-Suppe",
        answerD = "Erdbeerkuchen",
        correctAnswer = 1,
        explanation = "In Japan ist es eine weit verbreitete Tradition, Weihnachten mit einem Eimer Fried Chicken von KFC zu feiern. Die Kampagne begann 1974 und wurde so erfolgreich, dass manche Familien ihren Tisch Wochen im Voraus reservieren muessen.",
        difficulty = 1,
        funFact = "Die KFC-Weihnachtskampagne in Japan heisst Kurisumasu ni wa Kentakkii (Kentucky fuer Weihnachten) und wurde 1974 von einem KFC-Manager erfunden. Heute ist es eine der groessten Werbeerfolge der Welt."
    ),

    // Question 26 — Hochzeit: Indien Mangobaum
    Question(
        categoryId = 11,
        questionText = "Was heiratet in Indien manchmal ein sogenannter Manglik, bevor er einen Menschen heiraten darf?",
        answerA = "Einen Fluss",
        answerB = "Einen Baum",
        answerC = "Einen Stein",
        answerD = "Eine Statue",
        correctAnswer = 1,
        explanation = "In einigen Regionen Indiens heiratet ein Manglik (eine Person unter bestimmten astrologischen Vorzeichen) zuerst symbolisch einen Baum, meistens einen Bananen- oder Peepalbaum. Danach wird der Baum gefaellt und der Weg fuer die echte Heirat ist frei.",
        difficulty = 1,
        funFact = "Der Grund ist Aberglaube: Man glaubt, dass ein Manglik seinen Partner in den Tod treibt. Durch die Heirat mit dem Baum wird der Fluch aufgehoben. Der Brauch ist in modernen Grossstaedten seltener geworden."
    ),

    // Question 27 — Feste: Karneval Koeln
    Question(
        categoryId = 11,
        questionText = "Wie nennen die Koelner ihren Karneval im Dialekt?",
        answerA = "De Fasching",
        answerB = "De Fastelovend",
        answerC = "De Jubelzeit",
        answerD = "De Narrenzeit",
        correctAnswer = 1,
        explanation = "Die Koelner nennen ihren Karneval liebevoll de Fastelovend (auf Koelnisch: Fastnachtsabend). Der Koelner Rosenmontagszug ist der groesste Karnevalszug Deutschlands und zieht jedes Jahr Hunderttausende Besucher an.",
        difficulty = 1,
        funFact = "Beim Koelner Rosenmontagszug werden traditionell Kamelle (Suessigkeiten), Straussblumen und Schocolade in die Menge geworfen. Pro Jahr kommen dabei mehrere Tonnen Kamelle zum Einsatz."
    ),

    // Question 28 — Ostern: Osterfeuer
    Question(
        categoryId = 11,
        questionText = "In welchen deutschsprachigen Laendern ist das Osterfeuer eine verbreitete Tradition?",
        answerA = "Nur in Bayern",
        answerB = "Nur in Oesterreich",
        answerC = "In Deutschland, Oesterreich und der Schweiz",
        answerD = "Vor allem in Norddeutschland",
        correctAnswer = 3,
        explanation = "Das Osterfeuer ist vor allem in Norddeutschland eine beliebte Tradition. Am Karsamstag werden grosse Lagerfeuer entzuendet, um den Winter zu vertreiben und den Fruehling willkommen zu heissen.",
        difficulty = 1,
        funFact = "Das Osterfeuer hat heidnische Wurzeln und war schon vor dem Christentum ein Fruehlingsbrauch. Die Kirche uebernahm und christianisierte ihn. Das Feuer symbolisiert heute das Licht Christi."
    ),

    // Question 29 — Silvester: Schottland First Footing
    Question(
        categoryId = 11,
        questionText = "Was bringt der erste Besucher nach Mitternacht laut schottischer Tradition ins Haus, um Glueck zu bringen?",
        answerA = "Eine Flasche Whisky und ein Stueck Brot",
        answerB = "Eine schwarze Katze",
        answerC = "Kohle, Whisky, Salz und ein dunkles Brot",
        answerD = "Eine rote Rose und Feuerholz",
        correctAnswer = 2,
        explanation = "In Schottland heisst der erste Besucher nach Mitternacht First Footer. Er bringt traditionell Kohle (Waerme), Whisky (gutes Leben), Salz (Wuerze) und ein dunkles Brot (Nahrung) als Gluecksbringer fuer das neue Jahr.",
        difficulty = 1,
        funFact = "Laut schottischem Aberglauben bringt ein grosser, dunkelhaariger Mann als First Footer das meiste Glueck. Ein rothaariger oder blonder First Footer gilt hingegen als schlechtes Omen."
    ),

    // Question 30 — Feste: Hanami Japan
    Question(
        categoryId = 11,
        questionText = "Was bedeutet das japanische Fruehlingsfest Hanami woertlich?",
        answerA = "Blumen giessen",
        answerB = "Blumen betrachten",
        answerC = "Blumen pflucken",
        answerD = "Blumen verschenken",
        correctAnswer = 1,
        explanation = "Hanami bedeutet auf Japanisch woertlich Blumen betrachten (hana = Blume, mi = betrachten). Gemeint ist das Betrachten der Kirschblueten im Fruehling. Millionen Japaner picknicken dann unter den Kirschbaumen.",
        difficulty = 1,
        funFact = "Hanami hat eine ueber 1.000 Jahre alte Geschichte und wurde urspruenglich am Kaiserlichen Hof gefeiert. Heute verfolgen die Japaner die Kirschblueten-Front (Sakura-Zensen) im Wetterbericht wie ein nationales Ereignis."
    ),

    // Question 31 — Weihnachten: Schweden
    Question(
        categoryId = 11,
        questionText = "Welche Tiergestalt aus Stroh ist in Schweden ein traditionischer Weihnachtsschmuck?",
        answerA = "Ein Weihnachtsschaf",
        answerB = "Ein Weihnachtsziege (Julbock)",
        answerC = "Ein Weihnachtspferd",
        answerD = "Ein Weihnachtsschwein",
        correctAnswer = 1,
        explanation = "In Schweden ist der Julbock (Weihnachtsziege) aus Stroh ein traditionelles Weihnachtssymbol. Die bekannteste Julbock-Figur wird jeden Advent in der Stadt Gavle aufgestellt und ist etwa 13 Meter gross.",
        difficulty = 1,
        funFact = "Die Gavle-Julbock ist beruehmt dafuer, dass sie fast jedes Jahr von Vandalen angezuendet wird. Seit 1966 wird sie aufgestellt und wurde bereits ueber 30 Mal in Brand gesetzt."
    ),

    // Question 32 — Neujahr: China Rotes Kuvert
    Question(
        categoryId = 11,
        questionText = "Wie nennt man die roten Briefumschlaege mit Geld, die Kinder in China zum Neujahr bekommen?",
        answerA = "Hongbao",
        answerB = "Xinbao",
        answerC = "Lihong",
        answerD = "Fuhong",
        correctAnswer = 0,
        explanation = "Zum chinesischen Neujahr bekommen Kinder von Erwachsenen Hongbao (rote Briefumschlaege) mit Geld drin. Rot steht fuer Glueck und Wohlstand. Diese Tradition ist in ganz Ostasien verbreitet.",
        difficulty = 1,
        funFact = "Heute verschicken Millionen Chinesen digitale Hongbao ueber die App WeChat. Allein zum Neujahr 2022 wurden ueber 10 Milliarden digitale rote Umschlaege verschickt."
    ),

    // Question 33 — Feste: Diwali
    Question(
        categoryId = 11,
        questionText = "Wie wird das hinduistische Lichterfest Diwali auch genannt?",
        answerA = "Das Fest der Farben",
        answerB = "Das Fest der Lichter",
        answerC = "Das Fest der Ernte",
        answerD = "Das Fest des Wassers",
        correctAnswer = 1,
        explanation = "Diwali ist als das Fest der Lichter bekannt. Bei diesem wichtigen hinduistischen Feiertag werden ueberall Oellampen (Diyas), Kerzen und Feuerwerk entzuendet, um das Licht ueber die Dunkelheit zu feiern.",
        difficulty = 1,
        funFact = "Diwali wird nicht nur von Hindus, sondern auch von Sikhs, Jains und einigen Buddhisten gefeiert. In Indien ist es ein gesetzlicher Feiertag und das Strassenbild ist dann von Millionen kleiner Flammen erleuchtet."
    ),

    // Question 34 — Hochzeit: Brautstrauss
    Question(
        categoryId = 11,
        questionText = "Was soll passieren, wenn eine unverheiratete Frau bei einer Hochzeit den Brautstrauss faengt?",
        answerA = "Sie wird zur Brautjungfer ernannt",
        answerB = "Sie wird als naechste heiraten",
        answerC = "Sie bekommt ein Kuscheltier",
        answerD = "Sie wird zur Ehrengaestin der naechsten Feier",
        correctAnswer = 1,
        explanation = "Nach dem alten Brauch wirft die Braut ihren Strauss in die Menge der unverheirateten Gaeste. Wer ihn faengt, soll laut Volksglauben als naechste heiraten. Dieser Brauch ist in vielen westlichen Laendern verbreitet.",
        difficulty = 1,
        funFact = "Frueher rissen Gaeste der Braut Stuecke aus ihrem Kleid, um einen Gluecksbringer zu haben. Um das zu verhindern, begann die Braut ihren Strauss in die Menge zu werfen und schnell zu fliehen."
    ),

    // Question 35 — Feste: Karneval Brasilien
    Question(
        categoryId = 11,
        questionText = "In welcher Stadt findet der weltberuehmt groesste Karneval mit dem Sambadrom statt?",
        answerA = "Sao Paulo",
        answerB = "Salvador",
        answerC = "Rio de Janeiro",
        answerD = "Buenos Aires",
        correctAnswer = 2,
        explanation = "Der Karneval in Rio de Janeiro (Brasilien) gilt als das groesste Karneval der Welt. Im Sambadrom treten die Sambaschulmit aufwendig geschmueckten Wagen und Kostuemdarbietungen gegeneinander an.",
        difficulty = 1,
        funFact = "Die Vorbereitung fuer den Rio-Karneval dauert das ganze Jahr. Manche Sambagruppen haben bis zu 5.000 Mitglieder und investieren Millionen in ihre Kostueue und Wagen."
    ),

    // Question 36 — Weihnachten: Philippinen Laternen
    Question(
        categoryId = 11,
        questionText = "Was schmuecken die Menschen auf den Philippinen traditionell zu Weihnachten?",
        answerA = "Palmen statt Weihnachtsbaeume",
        answerB = "Sternfoermige Bambus-Laternen (Parols)",
        answerC = "Bunte Faecher an den Fenstern",
        answerD = "Muschelketten an den Tueren",
        correctAnswer = 1,
        explanation = "Auf den Philippinen werden zu Weihnachten sternfoermige Laternen aus Bambus und Papier aufgehaengt, die Parols heissen. Sie symbolisieren den Stern von Bethlehem und leuchten in den Strassen und an Haeusern.",
        difficulty = 1,
        funFact = "Die Philippinen feiern mit der laengsten Weihnachtssaison weltweit — ab September beginnen dort schon die Weihnachtslieder und Dekorationen. Das Land ist ueberwiegend christlich und nimmt Weihnachten sehr ernst."
    ),

    // Question 37 — Neujahr: Spanien 12 Trauben Ort
    Question(
        categoryId = 11,
        questionText = "In welcher Stadt ist in Spanien der beruehnteste Ort, um die zwoelf Neujahrstrauben zu essen?",
        answerA = "Barcelona — auf dem Placa Catalunya",
        answerB = "Madrid — an der Puerta del Sol",
        answerC = "Sevilla — vor der Kathedrale",
        answerD = "Valencia — am Hafen",
        correctAnswer = 1,
        explanation = "Der beruehnteste Ort in Spanien fuer die Neujahrstrauben ist die Puerta del Sol in Madrid. Die Glocke des Real Casa de Correos schlaegt dort zwoelf Mal und Millionen Spanier folgen den Glockenschlaegen im Fernsehen.",
        difficulty = 1,
        funFact = "Viele Supermaerrkte in Spanien verkaufen zu Silvester Dosen mit genau 12 geschaelten und entkernte Trauben, damit man in der kurzen Zeit keine Probleme mit Kernen oder Schalen hat."
    ),

    // Question 38 — Feste: Laternenfest China
    Question(
        categoryId = 11,
        questionText = "Womit endet das chinesische Neujahrsfest nach 15 Tagen?",
        answerA = "Mit einem grossen Feuerwerk",
        answerB = "Mit dem Laternenfest",
        answerC = "Mit einer Parade des Kaisers",
        answerD = "Mit dem Drachenfest",
        correctAnswer = 1,
        explanation = "Das chinesische Neujahrsfest endet nach genau 15 Tagen mit dem Laternenfest (Yuanxiao). Bunte Laternen werden angezuendet und durch die Strassen getragen. Es ist der 15. Tag des ersten Mondmonats.",
        difficulty = 1,
        funFact = "Beim Laternenfest ist es Tradition, Raetsel zu loesen, die auf die Laternen geschrieben sind. Wer das Raetsel einer Laterne loest, bekommt sie oft als Preis geschenkt."
    ),

    // Question 39 — Hochzeit: Serbien Geld
    Question(
        categoryId = 11,
        questionText = "Was tun Gaeste bei einer serbischen Hochzeit mit Geldscheinen waehrend des Tanzes der Braut?",
        answerA = "Sie stecken sie in den Brautstrauss",
        answerB = "Sie kleben oder stecken sie auf das Kleid der Braut",
        answerC = "Sie legen sie in einen Korb neben der Braut",
        answerD = "Sie falten sie zu Origami-Formen",
        correctAnswer = 1,
        explanation = "Bei serbischen Hochzeiten stecken Gaeste der tanzenden Braut Geldscheine auf ihr Kleid. Dies ist ein Gluckwunsch und eine traditionelle Form des Geschenks. Die Braut tanzt mit immer mehr Geldscheinen auf dem Kleid.",
        difficulty = 1,
        funFact = "Dieser Brauch ist in vielen Balkan-Laendern verbreitet. Das Geld ist ein praktisches Hochzeitsgeschenk, das dem frisch verheirateten Paar beim Start ins neue Leben hilft."
    ),

    // Question 40 — Silvester: Neujahrsvorsaetze
    Question(
        categoryId = 11,
        questionText = "Aus welcher alten Kultur stammt die Tradition der Neujahrsvorsaetze?",
        answerA = "Aus dem alten China",
        answerB = "Aus dem alten Rom",
        answerC = "Aus dem alten Babylonien",
        answerD = "Aus dem mittelalterlichen Europa",
        correctAnswer = 2,
        explanation = "Neujahrsvorsaetze haben ihren Ursprung im alten Babylonien, vor etwa 4.000 Jahren. Die Babylonier machten am Neujahrstag Versprechen an ihre Goetter, zum Beispiel Schulden zu begleichen oder ausgeliehene Gegenstaende zurueckzugeben.",
        difficulty = 1,
        funFact = "Laut Forschungen halten nur etwa 8 bis 12 Prozent aller Menschen ihre Neujahrsvorsaetze das ganze Jahr durch. Trotzdem macht fast die Haelfte aller Erwachsenen jedes Jahr welche."
    ),

    // Question 41 — Feste: Midsommar Schweden
    Question(
        categoryId = 11,
        questionText = "Was ist das wichtigste Symbol beim schwedischen Midsommarfest?",
        answerA = "Ein grosses Lagerfeuer",
        answerB = "Eine Maypole (Maibaum) mit Kraenzen",
        answerC = "Ein Wikinger-Schiff aus Holz",
        answerD = "Riesige Blumenfelder",
        correctAnswer = 1,
        explanation = "Beim schwedischen Midsommarfest (Mittsommerfest) ist der mit Kraenzen und Baendern geschmueckte Maibaum das Herzstueck der Feier. Die Menschen tanzen singend darum herum und feiern den laengsten Tag des Jahres.",
        difficulty = 1,
        funFact = "In Schweden ist Midsommar fast wichtiger als Weihnachten. Es ist ein nationaler Feiertag und viele Schweden fahren aufs Land, sammeln Wildblumen und schlafen unter freiem Himmel."
    ),

    // Question 42 — Weihnachten: Norwegen Besen
    Question(
        categoryId = 11,
        questionText = "Was verstecken Norweger traditionell am Heiligabend?",
        answerA = "Ihre Geschenke unter dem Bett",
        answerB = "Ihre Besen und Reinigungsgeraete",
        answerC = "Ihre Wertgegenstaende im Keller",
        answerD = "Ihre Schuhe auf dem Dachboden",
        correctAnswer = 1,
        explanation = "In Norwegen ist es alter Brauch, am Heiligabend alle Besen und Reinigungsgeraete zu verstecken. Man glaubt, dass in der Weihnachtsnacht Hexen und boese Geister auf der Suche nach Besen sind, um auf ihnen durch die Luft zu reiten.",
        difficulty = 1,
        funFact = "Manche Norweger schiessen in der Weihnachtsnacht auch Schuesse in die Luft, um die boesen Geister zu verscheuchen. Der Brauch hat tief heidnische Wurzeln aus der Wikingerzeit."
    ),

    // Question 43 — Ostern: Eierlauf
    Question(
        categoryId = 11,
        questionText = "In welchem Land findet jedes Jahr der beruehmte Egg Rolling (Eierlauf) auf dem Rasen des Weissen Hauses statt?",
        answerA = "Grossbritannien",
        answerB = "Kanada",
        answerC = "Australien",
        answerD = "USA",
        correctAnswer = 3,
        explanation = "Der Egg Roll auf dem Suedrasen des Weissen Hauses in Washington D.C. ist eine jaeehrliche Ostertradition in den USA. Kinder rollen dabei hartgekochte Eier mit einem Loeffel ueber den Rasen.",
        difficulty = 1,
        funFact = "Der Egg Roll im Weissen Haus findet seit 1878 statt. Eigentlich begann er am Kapitol, wurde aber verboten, weil der Rasen beschaedigt wurde. Praesidentin Lucy Hayes lud die Kinder dann ins Weisse Haus ein."
    ),

    // Question 44 — Feste: Burning Man USA
    Question(
        categoryId = 11,
        questionText = "Was ist das bekannteste Merkmal des Burning Man Festivals in den USA?",
        answerA = "Ein riesiges Feuerwerk",
        answerB = "Das Verbrennen einer grossen Holzfigur eines Mannes",
        answerC = "Ein Konzert von 100 Bands gleichzeitig",
        answerD = "Das gemeinsame Pflanzen von Baeumen in der Wueste",
        correctAnswer = 1,
        explanation = "Das Burning Man Festival in der Wueste Nevadas endet jedes Jahr mit dem spektakuaelren Verbrennen einer riesigen Holzfigur in Form eines Menschen. Das Festival steht fuer Kreativitaet, Selbstausdruck und Gemeinschaft.",
        difficulty = 1,
        funFact = "Beim Burning Man entsteht jedes Jahr eine temporaere Stadt aus dem Nichts. Bis zu 80.000 Menschen campen dort eine Woche lang, und danach bleibt buchstaeblich nichts zurueck — Leave No Trace ist ein Kernprinzip."
    ),

    // Question 45 — Hochzeit: Eheringe
    Question(
        categoryId = 11,
        questionText = "An welchem Finger traegt man in Deutschland traditionell den Ehering?",
        answerA = "Am Ringfinger der linken Hand",
        answerB = "Am Mittelfinger der rechten Hand",
        answerC = "Am Ringfinger der rechten Hand",
        answerD = "Am Zeigefinger der linken Hand",
        correctAnswer = 2,
        explanation = "In Deutschland wird der Ehering traditionell am Ringfinger der rechten Hand getragen. In vielen anderen Laendern (z.B. USA, Frankreich) traegt man ihn links. Im angelsaechsischen Raum kommt der Verlobungsring auf die linke, der Trauring ebenfalls auf die linke Hand.",
        difficulty = 1,
        funFact = "Der Brauch, einen Ehering am Ringfinger zu tragen, geht auf die Roemer zurueck, die glaubten, durch diesen Finger verlaufe die Vena Amoris, die Liebesader, direkt zum Herzen."
    ),

    // Question 46 — Feste: St. Patricks Day
    Question(
        categoryId = 11,
        questionText = "Welche Farbe dominiert beim irischen St. Patricks Day Fest am 17. Maerz?",
        answerA = "Orange",
        answerB = "Blau",
        answerC = "Gruen",
        answerD = "Weiss",
        correctAnswer = 2,
        explanation = "Beim St. Patricks Day tragen alle gruen — Kleidung, Accessoires, manchmal sogar das Bier und Flusswasser wird gruen eingefaerbt. Gruen ist die Farbe Irlands, auch bekannt als die gruene Insel.",
        difficulty = 1,
        funFact = "In Chicago wird der Fluss Chicago River alljaeherlich gruen eingefaerbt. In Dublin wird Guinness mit gruener Lebensmittelfarbe serviert. Das Fest wird weltweit in Irish Pubs gefeiert."
    ),

    // Question 47 — Neujahr: Russland Vaeterfrost
    Question(
        categoryId = 11,
        questionText = "Wie heisst die russische Version des Weihnachtsmanns, der Kinder zu Neujahr beschenkt?",
        answerA = "Dedushka Moroz (Vaeter Frost)",
        answerB = "Snegorochka",
        answerC = "Moroshko",
        answerD = "Ded Snowman",
        correctAnswer = 0,
        explanation = "In Russland heisst die Figur, die Kindern Geschenke bringt, Ded Moroz (Vaeter Frost). Er bringt die Geschenke nicht zu Weihnachten, sondern zu Silvester und Neujahr. Seine Begleiterin ist die Eismaedchen Snegurotschka.",
        difficulty = 1,
        funFact = "Ded Moroz traegt anders als der westliche Weihnachtsmann keine roten, sondern oft blaue oder silberne Gewaender. Er kommt nicht durch den Schornstein, sondern durch die Haustuer."
    ),

    // Question 48 — Weihnachten: Mexiko Posadas
    Question(
        categoryId = 11,
        questionText = "Was feiern Mexikaner in den neun Naechten vor Weihnachten mit der Tradition Las Posadas?",
        answerA = "Das Hirtenfest mit lebenden Schafen",
        answerB = "Die Suche von Maria und Josef nach einer Unterkunft",
        answerC = "Die Ankunft der Heiligen Drei Koenige",
        answerD = "Das Fasten vor dem Weihnachtsessen",
        correctAnswer = 1,
        explanation = "Las Posadas (spanisch: Herbergen) sind neun Naechte mit Umzuegen und Feiern vom 16. bis 24. Dezember. Sie re-enaktieren die Suche von Maria und Josef nach einer Unterkunft in Bethlehem. Ein Teil der Teilnehmer bittet um Einlass, der andere lehnt ab.",
        difficulty = 1,
        funFact = "Bei Las Posadas gibt es immer eine buntgeschmueckte Pinata in Sternform mit sieben Spitzen. Die sieben Spitzen stehen fuer die sieben Todsunden, die man mit dem Stock (der Tugend) schlagen soll."
    ),

    // Question 49 — Ostern: Hasenursprung
    Question(
        categoryId = 11,
        questionText = "Welche Tiere brachten in manchen deutschen Regionen urspruenglich die Ostereier, bevor der Osterhase sich durchsetzte?",
        answerA = "Ente und Huhn",
        answerB = "Fuchs und Storch",
        answerC = "Wolf und Baer",
        answerD = "Igel und Eichhoernchen",
        correctAnswer = 1,
        explanation = "In verschiedenen deutschen Regionen gab es neben dem Osterhasen auch andere Tiere als Eierbringer: den Osterfuchs in der Schweiz und Teilen Bayerns sowie den Storch in einigen norddeutschen Regionen. Der Hase setzte sich erst im Laufe des 19. Jahrhunderts ueberall durch.",
        difficulty = 1,
        funFact = "In manchen Teilen Westfalens brachte der Hahn die Ostereier, und in Hessen war es der Kuckuck. Das zeigt, wie lokale Volksbraeche sich stark voneinander unterschieden."
    ),

    // Question 50 — Feste: Erntedank weltweit
    Question(
        categoryId = 11,
        questionText = "In welchem Land wird Erntedank (Thanksgiving) als nationaler Feiertag mit einem Truthahnessen gefeiert?",
        answerA = "Kanada und den USA",
        answerB = "Nur in den USA",
        answerC = "Australien und Neuseeland",
        answerD = "Grossbritannien und Irland",
        correctAnswer = 0,
        explanation = "Thanksgiving wird sowohl in den USA (vierter Donnerstag im November) als auch in Kanada (zweiter Montag im Oktober) als nationaler Feiertag gefeiert. Ein gebratener Truthahn ist die Hauptspeise beider Traditionen.",
        difficulty = 1,
        funFact = "Das erste amerikanische Thanksgiving im Jahr 1621 dauerte drei Tage. Die Pilger und Wampanoag-Ureinwohner feierten gemeinsam die Ernte. Truthahn stand nicht zwingend auf dem Speiseplan — eher Hirsch, Austern und Enten."
    )

)
