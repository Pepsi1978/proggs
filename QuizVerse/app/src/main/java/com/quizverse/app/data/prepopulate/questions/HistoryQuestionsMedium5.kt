package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsMedium5(): List<Question> = listOf(

    // --- Decolonization of Africa ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr erlangte Ghana als erstes Land südlich der Sahara die Unabhängigkeit von Großbritannien?",
        answerA = "1952",
        answerB = "1957",
        answerC = "1960",
        answerD = "1963",
        correctAnswer = 1,
        explanation = "Ghana erlangte am 6. März 1957 unter Kwame Nkrumah die Unabhängigkeit von Großbritannien und wurde damit das erste Land südlich der Sahara, das die Kolonialherrschaft abschüttelte.",
        difficulty = 2,
        funFact = "Ghana hieß unter britischer Herrschaft Goldküste – benannt nach dem reichen Goldvorkommen der Region."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Jahr wird als 'Jahr Afrikas' bezeichnet, weil 17 afrikanische Länder unabhängig wurden?",
        answerA = "1955",
        answerB = "1958",
        answerC = "1960",
        answerD = "1962",
        correctAnswer = 2,
        explanation = "1960 wird als das 'Jahr Afrikas' bezeichnet, da in diesem Jahr 17 afrikanische Länder ihre Unabhängigkeit erlangten, hauptsächlich ehemalige französische Kolonien.",
        difficulty = 2,
        funFact = "Zu den 17 Ländern gehörten u.a. Senegal, Mali, Kamerun, Togo, die Elfenbeinküste und der Kongo."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Präsident Algeriens nach der Unabhängigkeit von Frankreich 1962?",
        answerA = "Houari Boumédiène",
        answerB = "Ahmed Ben Bella",
        answerC = "Abdelaziz Bouteflika",
        answerD = "Ferhat Abbas",
        correctAnswer = 1,
        explanation = "Ahmed Ben Bella wurde nach dem Ende des Algerienkrieges 1962 der erste Präsident Algeriens. Er regierte bis zu seinem Sturz durch Houari Boumédiène im Jahr 1965.",
        difficulty = 2,
        funFact = "Der Algerienkrieg dauerte von 1954 bis 1962 und kostete schätzungsweise 300.000 bis 1,5 Millionen Algerier das Leben."
    ),

    // --- Decolonization of Asia ---

    Question(
        categoryId = 3,
        questionText = "Welches asiatische Land wurde 1949 unter Ho Chi Minh zur Demokratischen Republik und kämpfte gegen die französische Kolonialherrschaft?",
        answerA = "Kambodscha",
        answerB = "Vietnam",
        answerC = "Laos",
        answerD = "Myanmar",
        correctAnswer = 1,
        explanation = "Ho Chi Minh rief am 2. September 1945 die Demokratische Republik Vietnam aus. Der anschließende Indochina-Krieg gegen Frankreich endete 1954 mit der Niederlage Frankreichs bei Điện Biên Phủ.",
        difficulty = 2,
        funFact = "Ho Chi Minh erklärte die Unabhängigkeit mit Worten, die direkt aus der amerikanischen Unabhängigkeitserklärung stammten."
    ),

    Question(
        categoryId = 3,
        questionText = "Die Konferenz von Bandung 1955 war ein Treffen afro-asiatischer Länder. Welches Hauptziel verfolgte sie?",
        answerA = "Gründung einer Militärallianz gegen die USA",
        answerB = "Förderung wirtschaftlicher Zusammenarbeit mit der Sowjetunion",
        answerC = "Zusammenschluss blockfreier Nationen gegen Kolonialismus und den Kalten Krieg",
        answerD = "Schaffung einer gemeinsamen Währung für Entwicklungsländer",
        correctAnswer = 2,
        explanation = "Die Bandung-Konferenz war ein Treffen von 29 afro-asiatischen Ländern, die Solidarität, Antikolonialismus und Neutralität im Kalten Krieg anstrebten. Sie legte den Grundstein für die Bewegung der Blockfreien.",
        difficulty = 2,
        funFact = "An der Konferenz nahmen Staatsmänner wie Jawaharlal Nehru (Indien), Gamal Abdel Nasser (Ägypten) und Zhou Enlai (China) teil."
    ),

    // --- Chinese History ---

    Question(
        categoryId = 3,
        questionText = "Welche Dynastie regierte China am längsten und endete 1912 mit der Abdankung des letzten Kaisers Puyi?",
        answerA = "Ming-Dynastie",
        answerB = "Han-Dynastie",
        answerC = "Tang-Dynastie",
        answerD = "Qing-Dynastie",
        correctAnswer = 3,
        explanation = "Die Qing-Dynastie (1644–1912) war die letzte imperiale Dynastie Chinas. Mit der Abdankung von Kaiser Puyi am 12. Februar 1912 endete über 2000 Jahre chinesische Kaiserherrschaft.",
        difficulty = 2,
        funFact = "Kaiser Puyi war bei seiner Thronbesteigung gerade einmal drei Jahre alt und lebte nach seiner Abdankung später als einfacher Gärtner in Peking."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Kulturrevolution in China, die Mao Zedong 1966 einleitete?",
        answerA = "Ein Programm zur Industrialisierung des Landes",
        answerB = "Eine politische Kampagne zur Vernichtung vermeintlicher Feinde des Kommunismus und zur Festigung von Maos Macht",
        answerC = "Eine Reformbewegung zur Demokratisierung Chinas",
        answerD = "Ein wirtschaftlicher Fünfjahresplan nach sowjetischem Vorbild",
        correctAnswer = 1,
        explanation = "Die Kulturrevolution (1966–1976) war eine von Mao initiierte politische Kampagne, die traditionelle Kultur, Intellektuelle und politische Rivalen verfolgte. Rote Garden terrorisierten die Bevölkerung und Millionen Menschen starben.",
        difficulty = 2,
        funFact = "Bücher, Kunstwerke und Tempel wurden zerstört. Der Begriff 'rote Garden' bezeichnete die jugendlichen Milizen, die Maos Ideen fanatisch umsetzten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was versteht man unter dem 'Großen Sprung nach vorn' (1958–1962) unter Mao Zedong?",
        answerA = "Chinas ersten Raumfahrtprogramm",
        answerB = "Ein Wirtschaftsprogramm zur schnellen Industrialisierung, das zu einer verheerenden Hungersnot führte",
        answerC = "Die militärische Modernisierung der Volksbefreiungsarmee",
        answerD = "Die Öffnung Chinas gegenüber dem Westen",
        correctAnswer = 1,
        explanation = "Der Große Sprung nach vorn war Maos katastrophaler Versuch, China schnell zu industrialisieren. Schlechte Planung, Zwangskollektivierung und Misswirtschaft lösten eine der schlimmsten Hungersnöte der Geschichte aus, mit 15–55 Millionen Todesopfern.",
        difficulty = 2,
        funFact = "Um Stahlquoten zu erfüllen, schmelzten Bauern ihre Werkzeuge und Töpfe ein – was die Nahrungsmittelproduktion weiter zerstörte."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann rief Mao Zedong die Volksrepublik China aus?",
        answerA = "1945",
        answerB = "1947",
        answerC = "1949",
        answerD = "1953",
        correctAnswer = 2,
        explanation = "Am 1. Oktober 1949 rief Mao Zedong in Peking die Volksrepublik China aus, nachdem seine Kommunisten den Bürgerkrieg gegen die Nationalisten (Kuomintang) gewonnen hatten.",
        difficulty = 2,
        funFact = "Die besiegten Nationalisten unter Chiang Kai-shek flohen auf die Insel Taiwan, wo sie die Republik China weiterführten – ein Status quo, der bis heute anhält."
    ),

    // --- Japanese History ---

    Question(
        categoryId = 3,
        questionText = "Was war die Meiji-Restauration (ab 1868) in Japan?",
        answerA = "Eine religiöse Bewegung zur Erneuerung des Shintoismus",
        answerB = "Die Rückkehr des Kaisers zur Macht und eine umfassende Modernisierung Japans nach westlichem Vorbild",
        answerC = "Ein Bauernaufstand gegen den Feudaladel",
        answerD = "Die Gründung des japanischen Parlaments",
        correctAnswer = 1,
        explanation = "Die Meiji-Restauration beendete die Herrschaft der Shogune und gab Kaiser Meiji die Macht zurück. Japan modernisierte sich rasant: Industrie, Militär, Bildung und Rechtssystem wurden nach westlichem Vorbild umgebaut.",
        difficulty = 2,
        funFact = "Innerhalb weniger Jahrzehnte verwandelte sich Japan von einem isolierten Feudalstaat in eine imperialistische Weltmacht – eine der erstaunlichsten Transformationen der Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Auf welche japanischen Städte wurden im August 1945 Atombomben abgeworfen?",
        answerA = "Tokio und Osaka",
        answerB = "Hiroshima und Nagasaki",
        answerC = "Kyoto und Hiroshima",
        answerD = "Nagasaki und Yokohama",
        correctAnswer = 1,
        explanation = "Am 6. August 1945 wurde Hiroshima und am 9. August Nagasaki durch amerikanische Atombomben zerstört. Die Bomben töteten sofort ca. 110.000–210.000 Menschen und zwangen Japan zur Kapitulation.",
        difficulty = 2,
        funFact = "Die Atombombe auf Hiroshima hieß 'Little Boy', die auf Nagasaki 'Fat Man'. Hiroshima beherbergt heute das Friedensgedenkmuseum."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann kapitulierte Japan im Zweiten Weltkrieg offiziell?",
        answerA = "6. August 1945",
        answerB = "15. August 1945",
        answerC = "2. September 1945",
        answerD = "8. Mai 1945",
        correctAnswer = 2,
        explanation = "Japan kapitulierte offiziell am 2. September 1945 auf dem Schlachtschiff USS Missouri in der Bucht von Tokio. Am 15. August hatte Kaiser Hirohito in einer Radiorede die Kapitulation bekanntgegeben.",
        difficulty = 2,
        funFact = "Es war das erste Mal, dass das japanische Volk die Stimme seines Kaisers im Radio hörte. Viele Soldaten glaubten der Botschaft zunächst nicht."
    ),

    // --- Indian Independence ---

    Question(
        categoryId = 3,
        questionText = "Was war der 'Salzmarsch' von Mahatma Gandhi im Jahr 1930?",
        answerA = "Ein Protestmarsch gegen die britische Salzsteuer als Symbol des gewaltlosen Widerstands",
        answerB = "Eine religiöse Pilgerreise zum Meer",
        answerC = "Ein Marsch der indischen Armee gegen britische Truppen",
        answerD = "Ein Boykott britischer Textilien",
        correctAnswer = 0,
        explanation = "Gandhi führte 1930 einen 388 km langen Marsch zum Meer, um dort Salz aus dem Meerwasser zu gewinnen und die britische Salzsteuer zu umgehen. Es war ein Symbol des zivilen Ungehorsams gegen die Kolonialherrschaft.",
        difficulty = 2,
        funFact = "Der Salzmarsch dauerte 24 Tage. Tausende Menschen schlossen sich Gandhi an, und die Bilder gingen um die Welt und stärkten die internationale Aufmerksamkeit für die indische Unabhängigkeitsbewegung."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bedeutete die Teilung Britisch-Indiens 1947?",
        answerA = "Die Aufteilung in Nord- und Südindien",
        answerB = "Die Entstehung der unabhängigen Staaten Indien und Pakistan",
        answerC = "Die Schaffung eines Commonwealth-Staates unter britischer Aufsicht",
        answerD = "Die Trennung Indiens von Burma",
        correctAnswer = 1,
        explanation = "Die Teilung von 1947 schuf die unabhängigen Staaten Indien (mehrheitlich hinduistisch) und Pakistan (mehrheitlich muslimisch). Sie löste eine der größten Bevölkerungswanderungen der Geschichte aus – ca. 14 Millionen Menschen flohen.",
        difficulty = 2,
        funFact = "Schätzungsweise 200.000 bis 2 Millionen Menschen kamen durch kommunale Gewalt während der Teilung ums Leben. Die Grenze wurde in nur 36 Tagen vom britischen Anwalt Cyril Radcliffe gezogen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Premierminister Indiens nach der Unabhängigkeit 1947?",
        answerA = "Mahatma Gandhi",
        answerB = "B. R. Ambedkar",
        answerC = "Jawaharlal Nehru",
        answerD = "Subhas Chandra Bose",
        correctAnswer = 2,
        explanation = "Jawaharlal Nehru wurde am 15. August 1947 der erste Premierminister des unabhängigen Indiens und regierte bis zu seinem Tod 1964. Gandhi war politischer Führer der Unabhängigkeitsbewegung, bekleidete aber kein Amt.",
        difficulty = 2,
        funFact = "Nehrus berühmte Rede 'Tryst with Destiny' am Vorabend der Unabhängigkeit gilt als eine der großartigsten Reden des 20. Jahrhunderts."
    ),

    // --- Middle East History ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde der Staat Israel gegründet?",
        answerA = "1945",
        answerB = "1947",
        answerC = "1948",
        answerD = "1950",
        correctAnswer = 2,
        explanation = "Am 14. Mai 1948 proklamierte David Ben-Gurion die Unabhängigkeit des Staates Israel. Am folgenden Tag endete das britische Mandat über Palästina, und fünf arabische Länder griffen Israel an.",
        difficulty = 2,
        funFact = "Die USA erkannten Israel als ersten Staat an – nur elf Minuten nach der Unabhängigkeitserklärung. Präsident Truman handelte gegen den Rat seines Außenministers Marshall."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Suez-Krise von 1956?",
        answerA = "Ein ägyptisch-israelischer Krieg um die Sinai-Halbinsel",
        answerB = "Die Verstaatlichung des Suezkanals durch Nasser und die darauffolgende britisch-französisch-israelische Militäraktion",
        answerC = "Ein sowjetischer Versuch, den Suezkanal zu kontrollieren",
        answerD = "Ein Aufstand der ägyptischen Bevölkerung gegen König Faruk",
        correctAnswer = 1,
        explanation = "Ägyptens Präsident Nasser verstaatlichte 1956 den Suezkanal. Großbritannien, Frankreich und Israel griffen militärisch ein, mussten aber unter US-amerikanischem und sowjetischem Druck abziehen – ein Symbol des schwindenden europäischen Kolonialflusses.",
        difficulty = 2,
        funFact = "Die Suez-Krise markierte das Ende Großbritanniens und Frankreichs als Weltmächte. Die USA erzwangen den Abzug ihrer eigenen Verbündeten – ein Wendepunkt im Kalten Krieg."
    ),

    Question(
        categoryId = 3,
        questionText = "Was versteht man unter dem Sechs-Tage-Krieg von 1967?",
        answerA = "Einen Bürgerkrieg im Libanon",
        answerB = "Einen israelischen Präventivschlag gegen Ägypten, Jordanien und Syrien, nach dem Israel bedeutende Gebiete eroberte",
        answerC = "Einen Krieg zwischen Ägypten und Saudi-Arabien",
        answerD = "Die israelische Gründungskrieg gegen seine arabischen Nachbarn",
        correctAnswer = 1,
        explanation = "Im Juni 1967 besiegte Israel in nur sechs Tagen Ägypten, Jordanien und Syrien und eroberte Sinai, Gazastreifen, Westjordanland und Golanhöhen. Diese Gebiete prägen den Nahost-Konflikt bis heute.",
        difficulty = 2,
        funFact = "Israels Luftwaffe zerstörte innerhalb der ersten Stunden fast die gesamte ägyptische Luftwaffe am Boden – der entscheidende Faktor für den schnellen Sieg."
    ),

    // --- Latin American Revolutions ---

    Question(
        categoryId = 3,
        questionText = "Wer war Simón Bolívar und was ist sein historisches Vermächtnis?",
        answerA = "Ein mexikanischer Revolutionär, der 1810 die Unabhängigkeit ausrief",
        answerB = "Der 'Befreier', der mehrere südamerikanische Länder von der spanischen Kolonialherrschaft befreite",
        answerC = "Ein brasilianischer Kaiser, der das Sklavereisystem abschaffte",
        answerD = "Ein argentinischer General, der im Ersten Weltkrieg kämpfte",
        correctAnswer = 1,
        explanation = "Simón Bolívar (1783–1830) führte die Unabhängigkeitsbewegungen in Venezuela, Kolumbien, Ecuador, Peru und Bolivien an. Er träumte von einem vereinten Südamerika und wird 'El Libertador' genannt.",
        difficulty = 2,
        funFact = "Bolivien ist das einzige Land der Welt, das nach einer lebenden Person benannt wurde – nach Simón Bolívar selbst, während er noch lebte."
    ),

    Question(
        categoryId = 3,
        questionText = "Che Guevara war an der kubanischen Revolution beteiligt. Wo und wann wurde er getötet?",
        answerA = "In Kuba, 1959",
        answerB = "In Bolivien, 1967",
        answerC = "In der Demokratischen Republik Kongo, 1965",
        answerD = "In Argentinien, 1970",
        correctAnswer = 1,
        explanation = "Che Guevara wurde am 9. Oktober 1967 in La Higuera, Bolivien, von bolivianischen Soldaten (mit CIA-Unterstützung) gefangen genommen und hingerichtet, nachdem sein Guerillakampf in Bolivien gescheitert war.",
        difficulty = 2,
        funFact = "Das berühmte Foto von Che Guevara ('Guerrillero Heroico') wurde 1960 aufgenommen und gilt als das meistreproduzierte Foto der Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann fand die kubanische Revolution statt, die Fidel Castro an die Macht brachte?",
        answerA = "1953",
        answerB = "1956",
        answerC = "1959",
        answerD = "1961",
        correctAnswer = 2,
        explanation = "Am 1. Januar 1959 floh Diktator Batista aus Kuba, nachdem Castros Guerillas Havanna eingenommen hatten. Fidel Castro übernahm die Macht und regierte Kuba bis 2008.",
        difficulty = 2,
        funFact = "Castros erste Guerilla-Landung 1956 mit der Yacht 'Granma' war katastrophal – von 82 Kämpfern überlebten nur 19. Dennoch führte die Bewegung drei Jahre später zur Revolution."
    ),

    // --- Fall of Communism in Eastern Europe ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fiel die Berliner Mauer?",
        answerA = "1987",
        answerB = "1988",
        answerC = "1989",
        answerD = "1990",
        correctAnswer = 2,
        explanation = "Die Berliner Mauer fiel am 9. November 1989, nachdem die DDR-Regierung unter dem Druck von Massenprotesten die Reisefreiheit bekanntgegeben hatte. Tausende Ostdeutsche strömten durch die Grenzübergänge.",
        difficulty = 2,
        funFact = "Der DDR-Sprecher Günter Schabowski verkündete die Reisefreiheit in einer Pressekonferenz versehentlich 'sofort, unverzüglich' – obwohl sie eigentlich erst am nächsten Morgen gelten sollte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Solidarność'-Bewegung in Polen?",
        answerA = "Eine kommunistische Partei, die an der Seite der Sowjetunion stand",
        answerB = "Eine unabhängige Gewerkschaft, die zur ersten freien Opposition im Ostblock wurde",
        answerC = "Eine nationalistische Bewegung gegen die deutsche Minderheit",
        answerD = "Eine kirchliche Organisation zur Unterstützung des Papstes",
        correctAnswer = 1,
        explanation = "Solidarność (Solidarität) wurde 1980 unter Lech Wałęsa gegründet und war die erste unabhängige Gewerkschaft im Ostblock. Sie spielte eine Schlüsselrolle beim Ende des Kommunismus in Polen.",
        difficulty = 2,
        funFact = "Lech Wałęsa war Elektriker in der Danziger Werft und wurde später Präsident Polens (1990–1995). Er erhielt 1983 den Friedensnobelpreis."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bezeichnet man als die 'Samtene Revolution' (1989)?",
        answerA = "Die friedliche Revolution in der Tschechoslowakei, die das kommunistische Regime beendete",
        answerB = "Den Sturz von Nicolae Ceaușescu in Rumänien",
        answerC = "Den Zusammenbruch des sowjetischen Imperiums",
        answerD = "Die demokratische Transition in Ungarn",
        correctAnswer = 0,
        explanation = "Die Samtene Revolution war der gewaltfreie Übergang von der kommunistischen Herrschaft zur Demokratie in der Tschechoslowakei im November 1989. Václav Havel wurde der neue Präsident.",
        difficulty = 2,
        funFact = "Im Gegensatz dazu verlief die rumänische Revolution blutig: Diktator Ceaușescu und seine Frau wurden am 25. Dezember 1989 nach einem Schnellverfahren erschossen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann löste sich die Sowjetunion offiziell auf?",
        answerA = "9. November 1989",
        answerB = "3. Oktober 1990",
        answerC = "25. Dezember 1991",
        answerD = "1. Januar 1992",
        correctAnswer = 2,
        explanation = "Am 25. Dezember 1991 trat Michail Gorbatschow als Präsident der Sowjetunion zurück und erklärte deren Auflösung. Die rote Fahne über dem Kreml wurde durch die russische Trikolore ersetzt.",
        difficulty = 2,
        funFact = "Gorbatschows Reformen Glasnost (Offenheit) und Perestroika (Umstrukturierung) sollten die Sowjetunion retten, beschleunigten aber ihren Untergang."
    ),

    // --- German Reunification ---

    Question(
        categoryId = 3,
        questionText = "Wann trat die Deutsche Wiedervereinigung offiziell in Kraft?",
        answerA = "9. November 1989",
        answerB = "18. März 1990",
        answerC = "3. Oktober 1990",
        answerD = "31. Dezember 1990",
        correctAnswer = 2,
        explanation = "Am 3. Oktober 1990 trat die Deutsche Einheit in Kraft: Die DDR trat der Bundesrepublik Deutschland bei. Dieser Tag wird seitdem als 'Tag der Deutschen Einheit' gefeiert.",
        difficulty = 2,
        funFact = "Die Wiedervereinigung kostete Deutschland schätzungsweise 1,5 Billionen Euro – finanziert u.a. durch den 'Solidaritätszuschlag', der erst 2021 für die meisten Steuerzahler abgeschafft wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Zwei-plus-Vier-Vertrag' von 1990?",
        answerA = "Ein Wirtschaftsvertrag zwischen der BRD und der DDR",
        answerB = "Der internationale Vertrag, der die volle Souveränität des wiedervereinigten Deutschlands regelte",
        answerC = "Ein NATO-Beitrittsvertrag für Ostdeutschland",
        answerD = "Ein Grenzvertrag mit Polen und der Tschechoslowakei",
        correctAnswer = 1,
        explanation = "Der Zwei-plus-Vier-Vertrag wurde am 12. September 1990 zwischen den beiden deutschen Staaten (Zwei) und den vier Siegermächten des Zweiten Weltkriegs (USA, Sowjetunion, Großbritannien, Frankreich) unterzeichnet und gab Deutschland seine volle Souveränität zurück.",
        difficulty = 2,
        funFact = "Deutschland erkannte im Vertrag die Oder-Neiße-Linie als endgültige Ostgrenze an – ein wichtiger Schritt zur Aussöhnung mit Polen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der letzte Generalsekretär der DDR, der kurz vor der Wende zurücktrat?",
        answerA = "Erich Honecker",
        answerB = "Egon Krenz",
        answerC = "Hans Modrow",
        answerD = "Günter Schabowski",
        correctAnswer = 0,
        explanation = "Erich Honecker regierte die DDR von 1971 bis Oktober 1989. Unter dem Druck der Massenproteste trat er am 18. Oktober 1989 zurück. Sein Nachfolger Egon Krenz konnte den Untergang der DDR nicht mehr aufhalten.",
        difficulty = 2,
        funFact = "Honecker floh 1991 in die sowjetische Botschaft in Moskau, lebte später in Chile und starb 1994 in Santiago de Chile an Leberkrebs."
    ),

    // --- EU Expansion ---

    Question(
        categoryId = 3,
        questionText = "Wie viele Länder traten der EU in der großen Erweiterungsrunde vom 1. Mai 2004 bei?",
        answerA = "5",
        answerB = "8",
        answerC = "10",
        answerD = "12",
        correctAnswer = 2,
        explanation = "Am 1. Mai 2004 traten zehn Länder der EU bei: Polen, Ungarn, Tschechien, Slowakei, Slowenien, Estland, Lettland, Litauen, Malta und Zypern. Es war die größte Erweiterung in der Geschichte der EU.",
        difficulty = 2,
        funFact = "Die Erweiterungsrunde 2004 verdoppelte fast die Anzahl der EU-Mitgliedstaaten – von 15 auf 25 Länder innerhalb eines einzigen Tages."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Schuman-Plan' von 1950, der zur Gründung der EU beitrug?",
        answerA = "Ein Vorschlag zur gemeinsamen europäischen Verteidigung",
        answerB = "Der Plan zur Zusammenlegung der deutschen und französischen Kohle- und Stahlindustrie",
        answerC = "Ein Friedensvertrag zwischen Deutschland und Frankreich",
        answerD = "Ein Wirtschaftshilfeprogramm für Westeuropa",
        correctAnswer = 1,
        explanation = "Robert Schumans Plan von 1950 sah vor, die Kohle- und Stahlproduktion Frankreichs und Deutschlands einer gemeinsamen Behörde zu unterstellen. Daraus entstand die EGKS, der Vorläufer der EU.",
        difficulty = 2,
        funFact = "Der 9. Mai, der Tag der Schuman-Erklärung, wird als 'Europatag' gefeiert."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann trat der Vertrag von Maastricht in Kraft, der die Europäische Union offiziell gründete?",
        answerA = "1989",
        answerB = "1991",
        answerC = "1993",
        answerD = "1995",
        correctAnswer = 2,
        explanation = "Der Vertrag von Maastricht trat am 1. November 1993 in Kraft und wandelte die Europäische Gemeinschaft in die Europäische Union um. Er legte auch den Grundstein für den Euro als gemeinsame Währung.",
        difficulty = 2,
        funFact = "Maastricht ist eine niederländische Stadt. Das dänische Volk lehnte den Vertrag in einer ersten Volksabstimmung 1992 ab, stimmte aber nach einigen Ausnahmeregelungen 1993 zu."
    ),

    // --- Modern Conflicts ---

    Question(
        categoryId = 3,
        questionText = "Was war der Auslöser des Golfkrieges von 1991?",
        answerA = "Der irakische Angriff auf den Iran",
        answerB = "Die irakische Invasion und Annexion Kuwaits im August 1990",
        answerC = "Ein Terroranschlag auf amerikanische Soldaten in Saudi-Arabien",
        answerD = "Die Entdeckung von Massenvernichtungswaffen im Irak",
        correctAnswer = 1,
        explanation = "Am 2. August 1990 marschierte Saddam Husseins Irak in Kuwait ein und annektierte das Land. Eine internationale Koalition unter US-Führung befreite Kuwait im Januar/Februar 1991 in der Operation 'Desert Storm'.",
        difficulty = 2,
        funFact = "Beim Rückzug aus Kuwait zündeten irakische Truppen über 600 kuwaitische Ölquellen an, was zu einer der größten Umweltkatastrophen der Geschichte führte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Jugoslawienkrieg der 1990er Jahre?",
        answerA = "Ein Konflikt zwischen Jugoslawien und seinen NATO-Nachbarn",
        answerB = "Eine Reihe von Kriegen nach dem Zerfall Jugoslawiens, geprägt durch ethnische Säuberungen und Kriegsverbrechen",
        answerC = "Ein Bürgerkrieg zwischen Kommunisten und Demokraten",
        answerD = "Ein Grenzstreit zwischen Jugoslawien und Österreich",
        correctAnswer = 1,
        explanation = "Nach dem Tod Titos (1980) und dem Ende des Kalten Krieges zerfiel Jugoslawien in blutigen Kriegen. Besonders brutal war der Bosnienkrieg (1992–1995), der das Massaker von Srebrenica mit über 8000 ermordeten Bosniaken umfasste.",
        difficulty = 2,
        funFact = "Das Massaker von Srebrenica 1995 ist das schlimmste Kriegsverbrechen in Europa seit dem Zweiten Weltkrieg und wurde vom Internationalen Gerichtshof als Völkermord eingestuft."
    ),

    Question(
        categoryId = 3,
        questionText = "Was waren die Terroranschläge vom 11. September 2001?",
        answerA = "Anschläge der Al-Qaida auf das World Trade Center und das Pentagon mit entführten Flugzeugen",
        answerB = "Bombenanschläge der IRA auf britische Institutionen in den USA",
        answerC = "Ein Angriff Nordkoreas auf amerikanische Militärbasen",
        answerD = "Cyberangriffe auf die amerikanische Infrastruktur",
        correctAnswer = 0,
        explanation = "Am 11. September 2001 entführten 19 Al-Qaida-Terroristen vier Passagierflugzeuge. Zwei trafen das World Trade Center, eines das Pentagon. Ein viertes stürzte in Pennsylvania ab. Etwa 3000 Menschen starben.",
        difficulty = 2,
        funFact = "Die Anschläge des 11. September lösten den 'Krieg gegen den Terror' aus, der zu den Invasionen Afghanistans (2001) und des Iraks (2003) führte."
    ),

    // --- Additional topics ---

    Question(
        categoryId = 3,
        questionText = "Was war der 'Eiserne Vorhang', den Winston Churchill 1946 beschrieb?",
        answerA = "Eine echte Stahlmauer entlang der innerdeutschen Grenze",
        answerB = "Die ideologische und physische Trennlinie zwischen dem kommunistischen Ostblock und dem demokratischen Westen",
        answerC = "Ein britisches Verteidigungssystem gegen Luftangriffe",
        answerD = "Der Name der Berliner Mauer",
        correctAnswer = 1,
        explanation = "Churchill prägte 1946 in seiner Fulton-Rede den Begriff 'Eiserner Vorhang' für die Trennlinie, die Europa von der Ostsee bis zur Adria in zwei Blöcke teilte – Ost und West, Kommunismus und Demokratie.",
        difficulty = 2,
        funFact = "Churchill hielt diese Rede in Fulton, Missouri, in Anwesenheit von US-Präsident Truman. Sie gilt als eine der ersten Erklärungen des Kalten Krieges."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Koreakrieg (1950–1953) und wie endete er?",
        answerA = "Ein Krieg zwischen China und Japan um die koreanische Halbinsel, endete mit japansichem Sieg",
        answerB = "Ein Konflikt zwischen Nord- und Südkorea (mit internationaler Beteiligung), der mit einem Waffenstillstand endete",
        answerC = "Eine US-Invasion Nordkoreas, die zur Vereinigung Koreas führte",
        answerD = "Ein Stellvertreterkrieg zwischen den USA und der Sowjetunion, der mit einem Friedensvertrag endete",
        correctAnswer = 1,
        explanation = "Nordkorea überfiel 1950 Südkorea. Die UN (hauptsächlich USA) verteidigte Südkorea, China unterstützte den Norden. 1953 endete der Krieg mit einem Waffenstillstand – kein Friedensvertrag. Korea ist bis heute geteilt.",
        difficulty = 2,
        funFact = "Technisch gesehen befinden sich Nord- und Südkorea noch immer im Kriegszustand, da bisher kein offizieller Friedensvertrag unterzeichnet wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Vietnam-Krieg (1955–1975) aus US-amerikanischer Perspektive?",
        answerA = "Ein Krieg zur Verteidigung der südvietnamesischen Demokratie gegen den kommunistischen Norden",
        answerB = "Ein Kolonialkrieg zur Wiedereingliederung Vietnams in westliche Einflusssphären",
        answerC = "Ein Präventivschlag gegen China",
        answerD = "Eine UN-Friedensmission",
        correctAnswer = 0,
        explanation = "Die USA intervenierten militärisch in Vietnam, um Südvietnam vor der kommunistischen Übernahme durch Nordvietnam zu schützen. Nach massiven Verlusten zogen die USA 1973 ab; 1975 fiel Saigon und Vietnam wurde vereint.",
        difficulty = 2,
        funFact = "Über 58.000 US-Soldaten und schätzungsweise 2 Millionen Vietnamesen starben im Krieg. Der Konflikt polarisierte die amerikanische Gesellschaft und löste die Anti-Kriegsbewegung der 1960er/70er aus."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Nelson Mandela und was bedeutete seine Freilassung 1990?",
        answerA = "Der erste schwarze Präsident Nigerias, der nach politischer Haft freigelassen wurde",
        answerB = "Der südafrikanische Anti-Apartheid-Kämpfer, dessen Freilassung den Weg zur Abschaffung der Apartheid ebnete",
        answerC = "Ein kenianischer Freiheitskämpfer gegen die britische Kolonialherrschaft",
        answerD = "Der rhodesische Premierminister, der die Unabhängigkeit ausrief",
        correctAnswer = 1,
        explanation = "Nelson Mandela wurde 27 Jahre lang auf Robben Island inhaftiert. Seine Freilassung am 11. Februar 1990 und die folgenden Verhandlungen führten zur Abschaffung der Apartheid und zu den ersten freien Wahlen 1994, bei denen Mandela Präsident wurde.",
        difficulty = 2,
        funFact = "Mandela sass für Sabotage und Verschwörung, nicht für Gewalt gegen Menschen. Er lehnte während der Haft mehrfach eine bedingte Freilassung ab, weil er seine Grundsätze nicht aufgeben wollte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das Tokioter Tribunal nach dem Zweiten Weltkrieg?",
        answerA = "Ein japanischer Kriegsverbrecherprozess gegen deutsche Offiziere",
        answerB = "Der Internationale Militärgerichtshof für den Fernen Osten, der japanische Kriegsverbrecher verurteilte",
        answerC = "Eine Konferenz zur Aufteilung Japans in Besatzungszonen",
        answerD = "Ein Friedensvertrag zwischen Japan und den alliierten Mächten",
        correctAnswer = 1,
        explanation = "Das Tokioter Tribunal (1946–1948) war das asiatische Gegenstück zu den Nürnberger Prozessen. 28 japanische Führungspersönlichkeiten wurden wegen Kriegsverbrechen und Verbrechen gegen den Frieden angeklagt; sieben wurden hingerichtet.",
        difficulty = 2,
        funFact = "Anders als bei den Nürnberger Prozessen wurde Kaiser Hirohito nicht angeklagt. Die USA entschieden, ihn als Symbol der Stabilität für den Wiederaufbau Japans zu erhalten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Wunder am Han-Fluss' in Südkorea?",
        answerA = "Ein religiöses Ereignis, das zur Gründung einer neuen Glaubensrichtung führte",
        answerB = "Südkoreas rapider wirtschaftlicher Aufstieg von einem der ärmsten zu einem der reichsten Länder der Welt",
        answerC = "Der Bau der längsten Brücke Asiens über den Han-Fluss",
        answerD = "Eine Hochwasserkatastrophe, die Seoul zerstörte",
        correctAnswer = 1,
        explanation = "Der Begriff 'Wunder am Han-Fluss' beschreibt Südkoreas außergewöhnliches Wirtschaftswachstum von den 1960er bis 1990er Jahren. Das Land verwandelte sich von einem der ärmsten der Welt zu einer bedeutenden Industrienation.",
        difficulty = 2,
        funFact = "Südkoreas Pro-Kopf-Einkommen stieg von ca. 100 Dollar in den 1960ern auf über 30.000 Dollar heute – eine der bemerkenswertesten wirtschaftlichen Transformationen der Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Tiananmen-Platz-Protest 1989 in China?",
        answerA = "Eine Siegesfeier nach der Ausrufung der Volksrepublik",
        answerB = "Demokratieproteste, die vom chinesischen Militär blutig niedergeschlagen wurden",
        answerC = "Eine staatliche Gedenkveranstaltung für Mao Zedong",
        answerD = "Ein Aufstand der Roten Garden während der Kulturrevolution",
        correctAnswer = 1,
        explanation = "Im Frühjahr 1989 versammelten sich Hunderttausende auf dem Tiananmen-Platz in Peking, um für Demokratie zu demonstrieren. Am 4. Juni befahl die Regierung den Militäreinsatz; die Opferzahl wird auf Hunderte bis Tausende geschätzt.",
        difficulty = 2,
        funFact = "Das Foto des 'Tank Man' – ein einzelner Mann, der allein vor einer Panzerkolonne steht – ist eines der bekanntesten Bilder des 20. Jahrhunderts. Seine Identität ist bis heute unbekannt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Cambodian Genozid unter den Roten Khmer (1975–1979)?",
        answerA = "Ein Krieg zwischen Kambodscha und Vietnam",
        answerB = "Eine Massentötung, bei der Pol Pots Regime schätzungsweise 1,5–2 Millionen Kambodschaner ermordete",
        answerC = "Die französische Kolonisierung Kambodschas",
        answerD = "Ein US-amerikanischer Bombardierungsfeldzug gegen Kambodscha",
        correctAnswer = 1,
        explanation = "Die Roten Khmer unter Pol Pot errichteten ein radikales maoistisches Regime und ermordeten durch Massaker, Zwangsarbeit und Hunger etwa ein Viertel der kambodschanischen Bevölkerung.",
        difficulty = 2,
        funFact = "Pol Pots Regime erklärte 1975 zum 'Jahr Null' und wollte die Gesellschaft vollständig neu aufbauen. Städte wurden zwangsevakuiert und Intellektuelle systematisch ermordet."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Arabische Frühling' (2010–2012)?",
        answerA = "Eine Reihe von Volksaufständen in arabischen Ländern gegen autoritäre Regime",
        answerB = "Ein arabisch-israelischer Friedensprozess",
        answerC = "Ein Wirtschaftsabkommen zwischen arabischen Staaten",
        answerD = "Eine islamische Erweckungsbewegung",
        correctAnswer = 0,
        explanation = "Der Arabische Frühling begann Ende 2010 in Tunesien und breitete sich auf Ägypten, Libyen, Syrien und andere Länder aus. Einige Diktatoren wurden gestürzt, andere Aufstände führten zu langen Bürgerkriegen.",
        difficulty = 2,
        funFact = "Der Arabische Frühling begann, als sich der tunesische Straßenhändler Mohamed Bouazizi am 17. Dezember 2010 aus Protest gegen Polizeiwillkür selbst anzündete – sein Tod löste eine Welle des Protests aus."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Land war der erste Staat, der von der NATO bomardiert wurde?",
        answerA = "Irak",
        answerB = "Afghanistan",
        answerC = "Jugoslawien / Serbien",
        answerD = "Libyen",
        correctAnswer = 2,
        explanation = "1999 bombardierte die NATO Jugoslawien (Serbien) während des Kosovo-Krieges, um die albanische Bevölkerung im Kosovo vor serbischen Truppen zu schützen. Es war der erste Kampfeinsatz der NATO in ihrer Geschichte.",
        difficulty = 2,
        funFact = "Die NATO-Intervention erfolgte ohne UN-Mandat, was rechtlich umstritten war. Das Kosovo erklärte 2008 einseitig die Unabhängigkeit, die jedoch nicht von allen UN-Mitgliedern anerkannt wird."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Ruandische Genozid von 1994?",
        answerA = "Ein Bürgerkrieg zwischen Uganda und Ruanda",
        answerB = "Die Massentötung von ca. 800.000 Tutsi und gemäßigten Hutu durch Hutu-Extremisten innerhalb von 100 Tagen",
        answerC = "Ein Aufstand gegen die belgische Kolonialherrschaft",
        answerD = "Ein Konflikt zwischen Ruanda und dem Kongo um Bodenschätze",
        correctAnswer = 1,
        explanation = "In 100 Tagen wurden 1994 in Ruanda schätzungsweise 800.000 bis 1 Million Menschen ermordet – hauptsächlich Angehörige der Tutsi-Minderheit. Die internationale Gemeinschaft griff nicht ein.",
        difficulty = 2,
        funFact = "Die Vereinten Nationen und westliche Länder wurden scharf dafür kritisiert, trotz früher Warnzeichen nicht eingegriffen zu haben. General Roméo Dallaire, UN-Kommandeur in Ruanda, kämpfte vergeblich um Genehmigung zum Handeln."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann und wodurch endete die Apartheid in Südafrika offiziell?",
        answerA = "1985 durch internationale Sanktionen",
        answerB = "1990 durch die Freilassung Mandelas",
        answerC = "1994 durch die ersten freien Wahlen, bei denen alle Bürger wählen durften",
        answerD = "1996 durch die neue südafrikanische Verfassung",
        correctAnswer = 2,
        explanation = "Die ersten freien, demokratischen Wahlen in Südafrika fanden am 27. April 1994 statt. Nelson Mandela wurde mit überwältigender Mehrheit zum Präsidenten gewählt – das formelle Ende der Apartheid.",
        difficulty = 2,
        funFact = "Millionen Südafrikaner standen stundenlang Schlange, um zum ersten Mal in ihrem Leben wählen zu können. Viele weinten vor Freude. Mandela nannte es 'die Geburt einer Demokratie'."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Kalte Krieg und wann begann er?",
        answerA = "Ein Wirtschaftskrieg zwischen den USA und Japan, der 1941 begann",
        answerB = "Der ideologische und geopolitische Konflikt zwischen den USA und der Sowjetunion, der nach 1945 begann",
        answerC = "Ein militärischer Konflikt zwischen NATO und Warschauer Pakt, der 1947 begann",
        answerD = "Ein nuklearer Krieg zwischen den Supermächten, der 1950 begann",
        correctAnswer = 1,
        explanation = "Der Kalte Krieg war die Rivalität zwischen den USA (und dem Westen) und der Sowjetunion (und dem Ostblock) nach dem Zweiten Weltkrieg. Er dauerte bis zum Zerfall der Sowjetunion 1991 und prägte die gesamte Weltpolitik.",
        difficulty = 2,
        funFact = "Der Begriff 'Kalter Krieg' wurde populär durch den Journalisten Walter Lippmann. 'Kalt' deshalb, weil beide Seiten einen direkten militärischen Zusammenstoß – einen 'heißen Krieg' – wegen der nuklearen Abschreckung vermieden."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Kubakrise von 1962?",
        answerA = "Der Versuch der USA, Kuba militärisch zu besetzen",
        answerB = "Der 13-tägige Konflikt zwischen USA und Sowjetunion um sowjetische Atomraketen auf Kuba",
        answerC = "Castros Übernahme der Macht in Kuba mit sowjetischer Unterstützung",
        answerD = "Ein US-amerikanischer Wirtschaftsboykott gegen Kuba",
        correctAnswer = 1,
        explanation = "Im Oktober 1962 entdeckten die USA sowjetische Atomraketenbasen auf Kuba. 13 Tage lang stand die Welt am Rand eines Atomkriegs, bis die Sowjetunion die Raketen abzog und die USA versprachen, Kuba nicht anzugreifen.",
        difficulty = 2,
        funFact = "In diesem Moment wurden U-Boote eingesetzt, die atomare Torpedos führten. Ein sowjetischer U-Boot-Kommandeur, Vasili Arkhipov, verweigerte den Befehl zum Schuss – er könnte die Welt vor dem Dritten Weltkrieg gerettet haben."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Wirtschaftswunder' in Westdeutschland nach dem Zweiten Weltkrieg?",
        answerA = "Deutschlands Sieg im Ersten Weltkrieg",
        answerB = "Der rasante wirtschaftliche Wiederaufstieg Westdeutschlands in den 1950er und 1960er Jahren",
        answerC = "Die Entdeckung von Ölvorkommen in der Nordsee",
        answerD = "Die Gründung der Bundesbank als stärkste Bank Europas",
        correctAnswer = 1,
        explanation = "Das 'Wirtschaftswunder' bezeichnet die außergewöhnlich schnelle wirtschaftliche Erholung der Bundesrepublik Deutschland nach dem Zweiten Weltkrieg. Unter Wirtschaftsminister Ludwig Erhard wuchs die Wirtschaft in den 1950ern jährlich um bis zu 8%.",
        difficulty = 2,
        funFact = "Deutschland lag 1945 in Trümmern, war aber bis 1955 bereits wieder eine der stärksten Wirtschaftsmächte Europas – maßgeblich gefördert durch den Marshallplan der USA."
    ),

    Question(
        categoryId = 3,
        questionText = "Was versteht man unter dem Begriff 'Dekolonisierung'?",
        answerA = "Die Gründung neuer Kolonien durch europäische Mächte nach 1945",
        answerB = "Der Prozess, durch den ehemals kolonisierte Länder ihre Unabhängigkeit erlangten",
        answerC = "Die wirtschaftliche Modernisierung von Entwicklungsländern",
        answerD = "Die Migration europäischer Siedler in Überseegebiete",
        correctAnswer = 1,
        explanation = "Dekolonisierung bezeichnet den historischen Prozess, durch den die kolonisierten Länder Afrikas, Asiens und der Karibik nach dem Zweiten Weltkrieg ihre Unabhängigkeit von europäischen Kolonialmächten erlangten.",
        difficulty = 2,
        funFact = "1900 kontrollierten europäische Mächte etwa 85% der Erdoberfläche. Bis 1975 hatten die meisten Kolonien ihre Unabhängigkeit erlangt – eine der größten politischen Transformationen der Geschichte."
    ),

)
