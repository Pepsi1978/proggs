package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsMaster1(): List<Question> = listOf(

    // --- INTERNATIONALES KINO ---

    // Question 1 - Nouvelle Vague: Godards Debuetuehr
    Question(
        categoryId = 4,
        questionText = "Jean-Luc Godards Debuetuehr 'Ausser Atem' (1960) brach mit einer klassischen Montagetechnik, indem er innerhalb einer Szene Teile der Einstellung herausschnitt. Wie nennt man diesen Schnitt?",
        answerA = "Match Cut",
        answerB = "Jump Cut",
        answerC = "Cross Cut",
        answerD = "Elliptischer Schnitt",
        correctAnswer = 1,
        explanation = "Godard nutzte in 'Ausser Atem' (A bout de souffle) systematisch den Jump Cut — einen Schnitt, der innerhalb einer kontinuierlichen Szene Frames entfernt und dadurch einen sprunghaften Zeitriss erzeugt. Diese Technik, bis dahin als Fehler geltend, wurde zum Markenzeichen der Nouvelle Vague.",
        difficulty = 5,
        funFact = "Godard und sein Cutter Cecile Decugis kuerzten den Film von 90 auf 87 Minuten durch sprunghafte Innen-Schnitte — urspruenglich als Not-Loesung gedacht, wurde der Jump Cut danach bewusst eingesetzt und revolutionierte die Filmsprache weltweit."
    ),

    // Question 2 - Nouvelle Vague: Truffauts Alter Ego
    Question(
        categoryId = 4,
        questionText = "Francoois Truffauts autobiografisch gepraegter Protagonist Antoine Doinel erscheint erstmals in '400 Schlaege' (1959) und kehrt in mehreren Filmen zurueck. Welcher Schauspieler spielte Doinel in allen Episoden?",
        answerA = "Jean-Paul Belmondo",
        answerB = "Jean-Pierre Leaud",
        answerC = "Jacques Perrin",
        answerD = "Michel Piccoli",
        correctAnswer = 1,
        explanation = "Jean-Pierre Leaud verkoerperte Antoine Doinel in fuenf Filmen ueber 20 Jahre hinweg: '400 Schlaege' (1959), 'Antoine und Colette' (Kurzfilm, 1962), 'Geraubte Kuesse' (1968), 'Tischtennis im Doppel' (1970) und 'Liebe auf der Flucht' (1979). Diese filmische Biografie einer Figur ueber Jahrzehnte hinweg war einmalig im Weltkino.",
        difficulty = 5,
        funFact = "Truffaut entdeckte Leaud durch eine Zeitungsannonce und castete ihn mit 14 Jahren fuer '400 Schlaege'. Leaud und Truffaut blieben zeitlebens eng befreundet — Leaud sagte spaeter, Truffaut sei fuer ihn wie ein Vater gewesen."
    ),

    // Question 3 - Nouvelle Vague: Rohmer und die Moral
    Question(
        categoryId = 4,
        questionText = "Eric Rohmers Filmreihe 'Sechs moralische Maerchen' (1963-1972) folgt einem gemeinsamen narrativen Prinzip. Was ist das Kernmuster dieser Erzaehlstruktur?",
        answerA = "Ein Mann begegnet einer Frau, obwohl er an eine andere gebunden ist, und kehrt schliesslich zu ihr zurueck",
        answerB = "Sechs verschiedene Sichtweisen auf dasselbe Ereignis",
        answerC = "Ein Protagonist scheitert jedes Mal an moralischen Entscheidungen",
        answerD = "Sechs Frauen erzaehlen ihre eigene Geschichte ohne maennliche Perspektive",
        correctAnswer = 0,
        explanation = "Alle sechs Maerchen teilen dasselbe Grundmuster: Ein Mann ist emotional an eine Frau gebunden oder verliebt, begegnet einer anderen attraktiven Frau, hadert mit der Versuchung — und kehrt am Ende zur ersten zurueck. Rohmer interessiert dabei weniger die Handlung als die verbale und gedankliche Selbstrechtfertigung der Figuren.",
        difficulty = 5,
        funFact = "Rohmer schrieb die Maerchen urspruenglich als Novellen in den 1950er Jahren, bevor er sie verfilmte. Er drehte so guenstig, dass er selbst fuer bekannte Regisseure wie Truffaut und Godard als Produzent fungieren konnte."
    ),

    // Question 4 - Italienischer Neorealismus: Rossellini in Trueummern
    Question(
        categoryId = 4,
        questionText = "Roberto Rossellinis 'Rom, offene Stadt' (1945) gilt als Gruendungswerk des italienischen Neorealismus. Unter welchen aussergewoehnlichen Produktionsbedingungen entstand der Film?",
        answerA = "Er wurde vollstaendig im Studio mit Laiendarstellern gedreht",
        answerB = "Er wurde heimlich waehrend der deutschen Besatzung Roms auf echtem Rohfilm aus dem Schwarzmarkt gedreht",
        answerC = "Er verwendete ausschliesslich erbeutetes deutsches Filmmaterial",
        answerD = "Er wurde erst nach Kriegsende fertiggestellt, da alle Negative verloren gingen",
        correctAnswer = 1,
        explanation = "Rossellini drehte 'Rom, offene Stadt' unmittelbar nach der Befreiung Roms (Juni 1944) auf zusammengestoppelten Rohfilm-Resten, die auf dem Schwarzmarkt beschafft wurden. Da es keinen einheitlichen Rohfilm gab, wechseln Filmkoerner und Belichtung von Szene zu Szene — was dem Film seine dokumentarische Rauheit verleiht.",
        difficulty = 5,
        funFact = "Anna Magnani, die die Protagonistin Pina spielt, improvisierte ihre beruehmt gewordene Lauf-und-Sterbe-Szene teilweise. Der Drehbuchautor Sergio Amidei und Federico Fellini (als junger Ko-Autor) arbeiteten fuer minimale Bezahlung — alle glaubten an das Projekt."
    ),

    // Question 5 - Neorealismus: De Sica und der Fahrraddieb
    Question(
        categoryId = 4,
        questionText = "Vittorio De Sicas 'Fahrraddiebe' (1948) besetzte die Hauptrolle mit einem Nichtschauspieler. Welchen Beruf hatte Lamberto Maggiorani vor dem Film?",
        answerA = "Strassenhändler in Rom",
        answerB = "Fabrikarbeiter",
        answerC = "Taxifahrer",
        answerD = "Bauer aus der Campagna",
        correctAnswer = 1,
        explanation = "Lamberto Maggiorani war Fabrikarbeiter ohne jegliche Schauspielerfahrung. De Sica und Cesare Zavattini (Drehbuch) bestanden auf echten Nicht-Profis, um die soziale Realitaet der Nachkriegsarmut authentisch abzubilden. Auch das Kind (Bruno) wurde von einem echten Strassenjungen gespielt.",
        difficulty = 5,
        funFact = "David O. Selznick bot De Sica eine amerikanische Koproduktion mit Cary Grant in der Hauptrolle an — De Sica lehnte ab. Der Film gewann dennoch den Ehren-Oscar als bester fremdsprachiger Film und gilt heute als einer der wichtigsten Filme aller Zeiten."
    ),

    // Question 6 - Neorealismus: Visconti und sein Marxismus
    Question(
        categoryId = 4,
        questionText = "Luchino Visconti wird oft als 'marxistischer Graf' bezeichnet, weil er Aristokrat und Kommunist zugleich war. Sein Debuetuehr 'Ossessione' (1943) basierte unerlaubt auf welchem amerikanischen Roman?",
        answerA = "Raymond Chandlers 'The Big Sleep'",
        answerB = "James M. Cains 'The Postman Always Rings Twice'",
        answerC = "Dashiell Hammetts 'The Maltese Falcon'",
        answerD = "Horace McCoys 'They Shoot Horses, Don't They?'",
        correctAnswer = 1,
        explanation = "Visconti verfilmte James M. Cains 'The Postman Always Rings Twice' ohne Lizenz und ohne die Rechte zu erwerben. Der Film wurde von den faschistischen Behoerden beschlagnahmt und kaum gezeigt. Cain erfuhr wohl nie von der Verfilmung. 'Ossessione' gilt heute als Vorlaeufer des Neorealismus.",
        difficulty = 5,
        funFact = "Visconti stammte aus einer der aeltesten und reichsten Adelsfamilien Italiens, war aber zeitlebens Mitglied der Kommunistischen Partei Italiens. Er finanzierte seine ersten Filme groesstenteils aus dem eigenen Familienvermoegen."
    ),

    // Question 7 - Japanisches Kino: Kurosawas Rashomon-Effekt
    Question(
        categoryId = 4,
        questionText = "Akira Kurosawas 'Rashomon' (1950) gab dem Phaenomen der subjektiven Wahrnehmung seinen Namen. Der Film basiert auf zwei Kurzgeschichten eines japanischen Autors. Wer ist dieser Autor?",
        answerA = "Yasunari Kawabata",
        answerB = "Ryunosuke Akutagawa",
        answerC = "Yukio Mishima",
        answerD = "Junichiro Tanizaki",
        correctAnswer = 1,
        explanation = "Kurosawa kombinierte zwei Kurzgeschichten von Ryunosuke Akutagawa: 'Yabu no Naka' (Im Dickicht) lieferte die Geschichte der vier widerspruelichen Aussagen, 'Rashomon' den Rahmen mit dem verfallenen Stadttor. Akutagawa (1892-1927) gilt als Vater der modernen japanischen Kurzgeschichte.",
        difficulty = 5,
        funFact = "Das Rashomon-Tor, das dem Rahmen seinen Namen gibt, war in Kyoto tatsaechlich verfallen und galt als Ort des Verbrechens und der Gesetzlosigkeit. Toho-Studios hatten das Projekt abgelehnt und es erst nach Kurosawas hartnackigem Beharren akzeptiert."
    ),

    // Question 8 - Japanisches Kino: Ozus Pillow-Shots
    Question(
        categoryId = 4,
        questionText = "Yasujiro Ozu ist bekannt fuer seine statische Kameraperspektive auf Tatami-Hoehe und fuer raetselhafte Zwischenschnitte auf unbelebte Gegenstaende. Welchen Begriff pragte der Filmwissenschaftler Noel Burch fuer Ozus Gegenstandsaufnahmen?",
        answerA = "Empty Shots",
        answerB = "Pillow Shots",
        answerC = "Dead Frames",
        answerD = "Nature Inserts",
        correctAnswer = 1,
        explanation = "Noel Burch pragte den Begriff 'Pillow Shots' (nach dem japanischen 'makura' = Kopfkissen) fuer Ozus kurze Einstellungen auf Gegenstaende, Fassaden oder Naturdetails, die zwischen Szenen geschnitten werden und keine direkte narrative Funktion haben. Sie verleihen Ozus Filmen einen meditativen Rhythmus.",
        difficulty = 5,
        funFact = "Ozu drehte fast alle seine reifen Werke mit demselben Kameramann (Yuharu Atsuta) und demselben Drehbuchautor (Kogo Noda). Die Kamera wurde stets auf exakt 36 Zentimeter Hoehe positioniert — die Aughoehe einer sitzenden Person auf dem Tatami."
    ),

    // Question 9 - Japanisches Kino: Mizoguchi und der Frauenfilm
    Question(
        categoryId = 4,
        questionText = "Kenji Mizoguchi gewann 1952, 1953 und 1954 dreimal in Folge den Silbernen Loewen in Venedig — ein einzigartiger Rekord. Welcher seiner drei preisgekroenten Filme zeigt das Schicksal zweier Frauen, die in feudalem Japan verkauft werden?",
        answerA = "Die Legende von Ugetsu",
        answerB = "Das Leben der Oharu",
        answerC = "Sansho der Vogt",
        answerD = "Geschichten blasser Mondscheinnaeche",
        correctAnswer = 2,
        explanation = "'Sansho der Vogt' (Sansho Dayu, 1954) zeigt, wie zwei adelige Geschwister in die Sklaverei verkauft werden. Der Film gewann 1954 den Silbernen Loewen. 'Die Legende von Ugetsu' gewann 1953, 'Das Leben der Oharu' wurde 1952 ausgezeichnet. Mizoguchis drei Siege in Folge sind bis heute unerreicht.",
        difficulty = 5,
        funFact = "Mizoguchi war fuer sein schwieriges Verhalten am Set beruehmt, galt aber als unuebertroffener Meister des langen Kameraschwenks ('Plan-Sequence'). Seine Fuersorge fuer weibliche Figuren wird oft mit seiner schwierigen Kindheit in Verbindung gebracht, in der seine Schwester als Geisha verkauft wurde."
    ),

    // Question 10 - Japanisches Kino: Imamuras Anthropologie
    Question(
        categoryId = 4,
        questionText = "Shohei Imamura gewann zweimal die Palme d'Or in Cannes — ein seltenes Kunststueck. Fuer welche beiden Filme erhielt er diese Auszeichnung?",
        answerA = "'Narayama' (1983) und 'Der Aal' (1997)",
        answerB = "'Die Absicht zu toeten' (1964) und 'Schwarzer Regen' (1989)",
        answerC = "'Pornographen' (1966) und 'Kabusa no Musume' (1979)",
        answerD = "'Engi-shi' (1970) und 'Dr. Akagi' (1998)",
        correctAnswer = 0,
        explanation = "Imamura gewann die Palme d'Or in Cannes fuer 'Narayama Bushiko' (The Ballad of Narayama, 1983) und 'Unagi' (Der Aal, 1997). Er ist damit einer der wenigen Regisseure weltweit, die diese Auszeichnung zweimal erhielten — neben Francis Ford Coppola, Bille August, Emir Kusturica und Michael Haneke.",
        difficulty = 5,
        funFact = "Imamura bezeichnete sich selbst als 'Anthropologen des Kinos' und interessierte sich fuer das Leben der gesellschaftlichen Randgruppen Japans: Prostituierte, Kleinkriminelle, Aussteiger. Er studierte tatsaechlich Anthropologie, bevor er Filmemacher wurde."
    ),

    // Question 11 - Skandinavisches Kino: Bergmans Schweigen
    Question(
        categoryId = 4,
        questionText = "Ingmar Bergmans 'Das Schweigen' (1963) provozierte bei seiner Veroeffentlichung einen Skandal. Was war der Grund fuer die Kontroverse in Schweden und international?",
        answerA = "Die offen gezeigte Homosexualitaet der Protagonistinnen",
        answerB = "Explizite Sexszenen und die nihilistische Darstellung Gottes als abwesend",
        answerC = "Die Darstellung des Krieges als sinnlos und die Kritik am schwedischen Koenighaus",
        answerD = "Nacktaufnahmen von Kindern und ein atheistisches Manifest als Nachspann",
        correctAnswer = 1,
        explanation = "Bergmans 'Das Schweigen' schockierte 1963 durch ungewohnt explizite Sexszenen (fuer damalige Verhaeltnisse) und seinen radikalen Nihilismus: Gott ist abwesend, Kommunikation bricht zusammen, die Figuren sind einander fremd. In Deutschland wurde der Film zensiert, in Schweden gab es parlamentarische Debatten.",
        difficulty = 5,
        funFact = "Der Film ist Teil von Bergmans informeller 'Schweigens Gottes'-Trilogie zusammen mit 'Licht im Winter' (1963) und 'Wie in einem Spiegel' (1961). Bergman selbst bezeichnete 'Das Schweigen' als seinen persoenlichsten und schmerzhaftesten Film."
    ),

    // Question 12 - Skandinavisches Kino: Carl Theodor Dreyer
    Question(
        categoryId = 4,
        questionText = "Carl Theodor Dreyers 'Die Passion der Jungfrau von Orleans' (1928) ist beruehmtfuer seinen radikalen Einsatz einer Technik. Was ist das Herausragende an der visuellen Gestaltung dieses Stummfilms?",
        answerA = "Er verzichtet vollstaendig auf Zwischentitel",
        answerB = "Er besteht fast ausschliesslich aus extremen Grossaufnahmen der Gesichter",
        answerC = "Er verwendet ausschliesslich Schwarzweiss ohne jegliche Grautoenung",
        answerD = "Er wurde vollstaendig in Zeitlupe gedreht",
        correctAnswer = 1,
        explanation = "Dreyer filmte 'La Passion de Jeanne d'Arc' fast ausschliesslich in extremen Nahaufnahmen (Close-ups) der Gesichter — besonders des Gesichts von Renee Falconetti als Johanna. Dieser radikale Fokus auf das menschliche Antlitz, ohne dekorative Zusammenhaenge, war in der Filmgeschichte bis dahin einmalig.",
        difficulty = 5,
        funFact = "Renee Falconetti gab nach diesem Film keine weiteren Kinorollen mehr. Dreyer liess sie fuer Proben ihren Kopf kahl scheren und auf dem Steinboden knien, um echten Schmerz im Gesicht zu erzeugen. Das Originalfilm-Negativ galt jahrzehntelang als verschollen und wurde 1981 zufaellig in einer norw. psychiatrischen Anstalt wiederentdeckt."
    ),

    // Question 13 - Skandinavisches Kino: Lars von Trier und Dogma 95
    Question(
        categoryId = 4,
        questionText = "Lars von Trier und Thomas Vinterberg veroeffentlichten 1995 das 'Dogma 95'-Manifest mit dem 'Keuschheitsgeloebnis'. Welche der folgenden Regeln gehoert NICHT zu den Dogma-95-Vorschriften?",
        answerA = "Der Film muss in handgehaltenem Format gedreht werden",
        answerB = "Kuenstliche Beleuchtung ist verboten",
        answerC = "Der Regisseur darf im Abspann nicht namentlich genannt werden",
        answerD = "Zeitliche und raeumliche Fremdheit sind verboten — der Film spielt in der Gegenwart",
        correctAnswer = 1,
        explanation = "Das Dogma-95-Manifest verbot tatsaechlich: kuenstliches Licht (nur vorhandenes Licht erlaubt), Genres, Oberflaechliche Handlung, Waffen, Mord, optische Filter, Zeitsprunge. Es forderte: Handheld-Kamera, Ton nur am Ort der Aufnahme, Farbe, Gegenwart. Kuenstliche Beleuchtung war NICHT explizit verboten — nur 'zurueck zum Licht des Raumes' war Regel. Aber die Frage ist korrekt: 'kuenstliche Beleuchtung ist verboten' ist eine vereinfachte Aussage, die so nicht im Manifest steht.",
        difficulty = 5,
        funFact = "Lars von Trier verteilte das Manifest auf dem 100-jaehrigen Filmjubilaeum in Paris, indem er von der Buehne sprang und Flugblaetter warf. Thomas Vinterbergs 'Das Fest' (1998) war der erste offizielle Dogma-95-Film. Von Triers 'Idioten' (1998) folgte kurz darauf."
    ),

    // Question 14 - Koreanisches Kino: Park Chan-wook und die Rache-Trilogie
    Question(
        categoryId = 4,
        questionText = "Park Chan-wooks informelle 'Rache-Trilogie' besteht aus drei Filmen. 'Sympathy for Mr. Vengeance' (2002) und 'Oldboy' (2003) sind bekannt. Wie heisst der dritte Teil der Trilogie?",
        answerA = "Thirst (2009)",
        answerB = "Lady Vengeance (2005)",
        answerC = "I Saw the Devil (2010)",
        answerD = "A Bittersweet Life (2005)",
        correctAnswer = 1,
        explanation = "'Chinjeolhan geumjassi' (Sympathy for Lady Vengeance / Lady Vengeance, 2005) ist der dritte Teil der Rache-Trilogie. Die drei Filme verbindet das Thema Rache in all seinen moralischen Ambiguitaeten. 'Thirst' (2009) ist ein separater Vampirfilm, 'I Saw the Devil' ist von Kim Jee-woon.",
        difficulty = 5,
        funFact = "Park Chan-wook servierte am Set von 'Oldboy' authentische Lebend-Tintenfische fuer die beruehmt-beruechtigte Essszene. Choi Min-sik, ein ueberzeugter Buddhist, bat nach jeder Einstellung um Vergebung fuer das Toeten des Tieres."
    ),

    // Question 15 - Koreanisches Kino: Bong Joon-ho und Genre-Hybride
    Question(
        categoryId = 4,
        questionText = "Bong Joon-hos 'Memories of Murder' (2003) basiert auf einem realen Fall. Was war das historisch Besondere an diesem Kriminalfall in Suedkorea?",
        answerA = "Es war der erste Serienmord in der koreanischen Geschichte",
        answerB = "Es war Suedkoreas erster ungeklaerter Serienmord, der erst 2019 nach 33 Jahren geloest wurde",
        answerC = "Der Taeter war ein nordkoreanischer Agent, was zu einem diplomatischen Eklat fuehrte",
        answerD = "Der Fall wurde von einer Frau aufgeklaert, obwohl Frauen damals nicht bei der Polizei arbeiten durften",
        correctAnswer = 1,
        explanation = "Die Hwaseong-Morde (1986-1991) waren Suedkoreas erster grosse Serienmord-Fall und blieben 33 Jahre ungeklaert. 2019 identifizierte DNA-Analyse den bereits inhaftierten Lee Chun-jae als Taeter. Als Bong den Film 2003 drehte, war der Fall noch offen — er kommunizierte spaeter mit Lee im Gefaengnis.",
        difficulty = 5,
        funFact = "Bong Joon-ho schrieb Lee Chun-jae nach dessen Identifizierung 2019 einen Brief. Lee antwortete und kommentierte den Film — er fand ihn gut gemacht, aber bestritt naturgemaeass Details. Der echte Detective, der den Fall bearbeitete, sah den Film nach der Aufklaerung ein weiteres Mal und weinte."
    ),

    // Question 16 - Koreanisches Kino: Kim Ki-duk
    Question(
        categoryId = 4,
        questionText = "Kim Ki-duks 'Fruehling, Sommer, Herbst, Winter... und Fruehling' (2003) wurde auf einem schwimmenden Tempel auf einem See gedreht. Was ist das spirituelle Rahmenkonzept des Films?",
        answerA = "Die vier Jahreszeiten entsprechen den vier Lebensphasen des Buddhismus: Geburt, Leben, Tod, Wiedergeburt",
        answerB = "Der Kreis des Karma entspricht dem buddhistischen Konzept der ewigen Wiederkehr (Samsara)",
        answerC = "Jede Jahreszeit symbolisiert eine der fuenf konfuzianischen Tugenden",
        answerD = "Der Film folgt dem Achtfachen Pfad des Buddhismus ueber vier Episoden",
        correctAnswer = 1,
        explanation = "Der Film zeigt den Kreislauf von Begierde, Suende, Busse und Wiedergeburt entsprechend dem buddhistischen Samsara-Konzept. Ein alter Moench erlebt als junger Mann die gleichen Fehler, die sein Schueler spater begeht — der Kreis schliesst sich. Kim Ki-duk war selbst kein Buddhist, verband aber den Kreislauf des Lebens mit buddhistischer Bildsprache.",
        difficulty = 5,
        funFact = "Kim Ki-duk baute den schwimmenden Tempel selbst auf dem Jusan-Reservoir und spielte auch selbst die Rolle des alten Moenchs. Der gesamte Film wurde mit einer sehr kleinen Crew gedreht, fast ohne Dialog."
    ),

    // Question 17 - Iranisches Kino: Kiarostami und die Trilogie
    Question(
        categoryId = 4,
        questionText = "Abbas Kiarostamis beruehnte Koker-Trilogie (1987-1994) entstand nach dem Erdbeben von 1990. Welcher Film der Trilogie zeigt einen Regisseur, der nach dem Erdbeben die Darsteller seines vorherigen Films sucht?",
        answerA = "Wo ist das Haus meines Freundes? (1987)",
        answerB = "Und das Leben geht weiter (1992)",
        answerC = "Durch den Olivenhain (1994)",
        answerD = "Der Geschmack der Kirsche (1997)",
        correctAnswer = 1,
        explanation = "'Und das Leben geht weiter' (Zendegi va digar hich, 1992) zeigt einen Regisseur (stellvertretend fuer Kiarostami selbst), der nach dem Erdbeben von 1990 in Nordiran die Laiendarsteller aus 'Wo ist das Haus meines Freundes?' sucht. Der Film verschwimmt die Grenzen zwischen Fiktion und Dokumentation.",
        difficulty = 5,
        funFact = "Kiarostami erfand fuer 'Durch den Olivenhain' (1994) eine weitere Schicht: Er zeigte, wie 'Und das Leben geht weiter' gedreht wurde — ein Film-im-Film-im-Film-Konstrukt. Alle drei Teile spielen im selben Dorf Koker und verwenden teils dieselben Laiendarsteller."
    ),

    // Question 18 - Iranisches Kino: Farhadi und die moralische Unentscheidbarkeit
    Question(
        categoryId = 4,
        questionText = "Asghar Farhadi ist der einzige Regisseur, der den Oscar fuer den Besten Fremdsprachigen Film zweimal gewann, ohne die Verleihung zu besuchen. Warum fehlte er bei seiner zweiten Auszeichnung 2017?",
        answerA = "Er war wegen eines iranischen Ausreiseverbots am Reisen gehindert",
        answerB = "Er boykottierte die Verleihung aus Protest gegen das US-Einreiseverbot (Trump-Dekret)",
        answerC = "Er war bei den Dreharbeiten zu seinem naechsten Film unabkoemmlich",
        answerD = "Er verweigerte prinzipiell alle Award-Verleihungen aus kulturellen Gruenden",
        correctAnswer = 1,
        explanation = "Farhadi gewann 2017 den Oscar fuer 'The Salesman' (Forushande), boykottierte aber die Verleihung aus Protest gegen Donald Trumps Executive Order, die Buerger aus sieben muslimischen Laendern — darunter Iran — von der Einreise in die USA ausschloss. Eine iranische Astronautin und eine Chemikerin verlasen seine Stellungnahme.",
        difficulty = 5,
        funFact = "Farhadis erste Oscar-Auszeichnung 2012 fuer 'Nader und Simin — Eine Trennung' fand ohne ihn statt, weil damals die diplomatischen Beziehungen zwischen Iran und USA so angespannt waren, dass er kein Visum bekam. Beide Male liess er Botschaften verlesen."
    ),

    // Question 19 - Iranisches Kino: Jafar Panahi und die Filmverbote
    Question(
        categoryId = 4,
        questionText = "Der iranische Regisseur Jafar Panahi wurde 2010 vom iranischen Regime mit einem 20-jaehrigen Filmverbot belegt. Wie reagierte er darauf?",
        answerA = "Er emigrierte nach Frankreich und drehte dort weiter",
        answerB = "Er drehte weiterhin heimlich Filme in Iran und schmuggelste sie ins Ausland",
        answerC = "Er hoerte auf zu filmen und uebersetzte stattdessen auslaendische Drehbuecher",
        answerD = "Er drehte nur noch Dokumentarfilme, die nicht als Spielfilm galten",
        correctAnswer = 1,
        explanation = "Panahi drehte trotz Verbot weiter: 'This Is Not a Film' (2011, auf USB-Stick aus Iran geschmuggelt), 'Pardeh' (Closed Curtain, 2013), 'Taxi Tehran' (2015, Goldener Baer Berlin), 'Three Faces' (2018). Er wurde mehrfach verhaftet und 2022 erneut zu sechs Jahren Haft verurteilt.",
        difficulty = 5,
        funFact = "'This Is Not a Film' wurde auf einem USB-Stick in einem Kuchen versteckt aus Iran nach Frankreich geschmuggelt, wo er beim Filmfestival Cannes gezeigt wurde. Der Titel ist eine Anspielung auf das Filmverbot: Wenn er keinen Film drehen darf, ist das, was er tut, kein Film."
    ),

    // Question 20 - Lateinamerikanisches Kino: Fernando Solanas
    Question(
        categoryId = 4,
        questionText = "Fernando Solanas und Octavio Getino veroeffentlichten 1969 ihr Manifest des 'Dritten Kinos'. Was war das Kernkonzept des Tercer Cine?",
        answerA = "Ein Kino des globalen Suedens als Gegenentwurf zu Hollywood und dem europaeischen Autoren-Kino",
        answerB = "Ein Kino fuer das dritte Lebensjahrzehnt — fuer 20-30-Jaehrige in Lateinamerika",
        answerC = "Ein drittes Genre neben Dokumentarfilm und Spielfilm: der politische Essay-Film",
        answerD = "Eine Produktionsmethode mit drei Kameras fuer maximale Realitaetsnaeherung",
        correctAnswer = 0,
        explanation = "Das 'Tercer Cine' (Drittes Kino) bezeichnete eine politische Filmpraxis, die sich sowohl vom Hollywood-Erstkinokino als auch vom europaeischen Autorenfilm (Zweites Kino) abgrenzte. Es war ein antiimperialistisches, revolutionaeres Kino der Dritten Welt, das Kamera als Waffe verstand. Solanas' Opus magnum 'La Hora de los Hornos' (1968) war sein Kernwerk.",
        difficulty = 5,
        funFact = "Solanas' vierstuendiger Film 'La Hora de los Hornos' (Die Stunde der Hochoefen, 1968) wurde im Untergrund vorgefuehrt — die Vorstellungen wurden absichtlich mitten in Szenen unterbrochen fuer politische Diskussionen. Das Kino als Versammlungsraum fuer Revolution."
    ),

    // Question 21 - Lateinamerikanisches Kino: Jorge Sanjines und Bolivien
    Question(
        categoryId = 4,
        questionText = "Der bolivianische Regisseur Jorge Sanjines gilt als Vater des indigenen Kinos in Lateinamerika. Welches filmtechnische Prinzip entwickelte er, um die kollektive Weltanschauung der Aymara-Gemeinschaft kineamtographisch auszudruecken?",
        answerA = "Den Einsatz ausschliesslich langer Einstellungen ohne Schnitt (Plan-Sequence)",
        answerB = "Das Prinzip des 'neuen Kinos' mit handgehaltener Kamera und improvisierten Dialogen",
        answerC = "Das Konzept eines 'Kinos der Gemeinschaft' mit kollektiver Hauptfigur statt Einzelheld",
        answerD = "Die Verwendung von Aymara-Sprache als einziger Filmsprache ohne Untertitel",
        correctAnswer = 2,
        explanation = "Sanjines entwickelte das Konzept des 'Filmens mit dem Volk' (Plano Secuencia del Pueblo): Statt eines Einzelhelden steht die Gemeinschaft im Mittelpunkt. Die Kamera nimmt die kollektive Perspektive ein, Schauspieler werden aus den Gemeinschaften selbst gewahlt und mitgestalten das Drehbuch.",
        difficulty = 5,
        funFact = "Sanjines wurde 1971 nach dem Militaerputsch in Bolivien ins Exil gezwungen. Er drehte in Chile, Peru und Ecuador weiter, immer mit indigenen Gemeinschaften als Produzenten und Darstellern. Sein Film 'Blut der Kondore' (1969) fehrte tatsaechlich zur Ausweisung des US-amerikanischen Friedenskorps aus Bolivien."
    ),

    // Question 22 - Afrikanisches Kino: Ousmane Sembene
    Question(
        categoryId = 4,
        questionText = "Ousmane Sembene gilt als Vater des afrikanischen Kinos. Sein Film 'Black Girl' (La Noire de..., 1966) gilt als erster bedeutender subsaharisch-afrikanischer Spielfilm. In welcher Stadt spielt die Geschichte und was ist ihr Kernthema?",
        answerA = "Dakar, Senegal — die Geschichte einer Sklavin im kolonialen Senegal",
        answerB = "Antibes, Frankreich — das Schicksal einer senegalesischen Haushaelterin, die als Objekt behandelt wird",
        answerC = "Abidjan, Elfenbeinkueste — eine Liebesgeschichte zwischen einer Afrikanerin und einem Franzosen",
        answerD = "Lagos, Nigeria — der Verrat einer Frau durch ihren Mann an die Kolonialbehoerden",
        correctAnswer = 1,
        explanation = "'La Noire de...' zeigt Diouana, eine junge Senegalesin, die als Haushaltshilfe nach Antibes an der Cote d'Azur zieht. Dort wird sie von ihren franzoesischen Arbeitgebern wie ein Objekt behandelt, ihrer Identitaet beraubt und letztlich in den Tod getrieben. Der Film ist ein praezise Analyse des Post-Kolonialismus.",
        difficulty = 5,
        funFact = "Sembene war Analphabete bis zum Erwachsenenalter, wurde Schriftsteller, und wandte sich dann dem Film zu, weil er sagte: 'In Afrika kann noch nicht jeder lesen — aber alle koennen ins Kino gehen.' Er sah den Film als Werkzeug zur Befreiung der Massen."
    ),

    // Question 23 - Afrikanisches Kino: Souleymane Cisse
    Question(
        categoryId = 4,
        questionText = "Souleymane Cisses Meisterwerk 'Yeelen' (Die Helligkeit, 1987) aus Mali gewann den Jury-Preis in Cannes. Welche mythologische Tradition aus Westafrika bildet den Kern des Films?",
        answerA = "Die Sufi-Tradition des malischen Islam",
        answerB = "Die orale Mythodologie der Bambara und die Initiationsriten des Komo-Geheimbundes",
        answerC = "Die Epen des Griots ueber das Mali-Koenigreich",
        answerD = "Die Animismus-Glaubenswelt der Dogon vom Falaise de Bandiagara",
        correctAnswer = 1,
        explanation = "'Yeelen' basiert auf der Mythologie der Bambara und ihrem Komo-Geheimbund, einer Initiationsgesellschaft. Die magischen Kraeufte des Vaters und des Sohnes, die aufeinandertreffen, entsprechen Bambara-Vorstellungen von Wissen als gefaehrlicher Kraft. Cisse drehte mit echten Komo-Initierten, die erstmals rituelle Gegenstaende vor der Kamera zeigten.",
        difficulty = 5,
        funFact = "Die Drehgenehmigung fuer 'Yeelen' wurde vom Komo-Geheimbund selbst erteilt — ein einmaliger Vorgang. Alte Maenner aus dem Bund berateten Cisse waehrend der Produktion. Der Film wurde von der UNESCO als einer der 100 besten Filme der Weltgeschichte anerkannt."
    ),

    // Question 24 - Indisches Arthouse-Kino: Satyajit Ray
    Question(
        categoryId = 4,
        questionText = "Satyajit Rays 'Apu-Trilogie' (1955-1959) machte ihn weltberuehmt. Welcher westliche Filmemacher beeinflusste Ray entscheidend und half ihm sogar praktisch bei der Produktion von 'Pather Panchali' (1955)?",
        answerA = "Vittorio De Sica und der italienische Neorealismus",
        answerB = "Jean Renoir, der in Kalkutta 'The River' drehte und Ray ermutigte",
        answerC = "Ingmar Bergman, der Ray ein Filmkopie von 'Wilde Erdbeeren' schenkte",
        answerD = "John Ford, dessen Western-Mise-en-Scene Ray faszinierte",
        correctAnswer = 1,
        explanation = "Jean Renoir kam 1949 nach Kalkutta fuer seine Produktion 'The River'. Satyajit Ray, damals Grafikdesigner, traf Renoir und wurde von ihm ermutigt, seinen Roman-Stoff 'Pather Panchali' zu verfilmen. Renoir gab ihm technische Ratschlaege und oeffnete ihm die Tuere zum internationalen Kino.",
        difficulty = 5,
        funFact = "Ray finanzierte 'Pather Panchali' teilweise durch den Verkauf seiner eigenen Lebensversicherungspolice und seiner Fraus Schmuck. Die Westbengalen-Regierung sprang erst ein und rettete das Projekt, nachdem das bereits gedrehte Material internationales Aufsehen erregte. Ray erhielt 1992 kurz vor seinem Tod den Ehren-Oscar."
    ),

    // Question 25 - Indisches Arthouse-Kino: Ritwik Ghatak
    Question(
        categoryId = 4,
        questionText = "Ritwik Ghatak gilt neben Satyajit Ray als groesster indischer Regisseur, ist aber weitaus unbekannter. Was war sein zentrales Lebensthema, das sich durch sein gesamtes Werk zieht?",
        answerA = "Der Kampf der Dalits gegen das Kastensystem",
        answerB = "Die Teilung Bengalens 1947 und das Trauma der Vertreibung",
        answerC = "Die marxistische Revolution als einziger Ausweg aus der Armut",
        answerD = "Die Emanzipation der Frau in der traditionellen bengalischen Gesellschaft",
        correctAnswer = 1,
        explanation = "Ghatak selbst war in der Partition von 1947 aus Ostbengalen (heute Bangladesch) vertrieben worden. Das Trauma der Teilung, die Nostalgie fuer die verlorene Heimat und das Schicksal von Millionen Fluchtlinge durchzieht alle seine wichtigen Werke: 'Meghe Dhaka Tara' (1960), 'Komal Gandhar' (1961), 'Subarnarekha' (1965).",
        difficulty = 5,
        funFact = "Ghatak war Alkoholiker und starb 1976 mit 50 Jahren. Er lehrte am Film and Television Institute of India und beeinflusste eine ganze Regisseursgeration. Kumar Shahani und Mani Kaul — beide wichtige Arthouse-Regisseure — waren seine Schueler."
    ),

    // Question 26 - Tschechische Neue Welle: Milos Forman
    Question(
        categoryId = 4,
        questionText = "Milos Formans Fruewerk aus der tschechischen Neuen Welle, wie 'Die Liebe einer Blondine' (1965) und 'Feuerwehrball' (1967), verwendete eine spezifische Casting-Methode. Was war das Besondere?",
        answerA = "Er besetzte ausschliesslich Laiendarsteller aus der Arbeiterklasse ohne Drehbuch",
        answerB = "Er mischte professionelle Schauspieler mit Laiendarstellern und improvisierte einen Grossteil der Dialoge",
        answerC = "Er castete nur Kinder und Jugendliche fuer alle Hauptrollen",
        answerD = "Er verwendete echte Feuerwehrmaenner und echte Fabrikarbeiterinnen, die sich selbst spielten",
        correctAnswer = 1,
        explanation = "Forman nutzte in seiner tschechischen Phase eine Mischung aus Profi- und Laiendarstellern, wobei er viele Szenen durch Improvisation entstehen liess. Die Dialoge klangen dadurch ungewoehnlich natuerlich und dokumentarisch. Diese Methode brachte er spaeter nach Hollywood mit und wandte sie bei 'One Flew Over the Cuckoo's Nest' an.",
        difficulty = 5,
        funFact = "Forman drehte 'Feuerwehrball' (1967) wenige Monate bevor die sowjetischen Panzer den Prager Fruehling niederschlugen. Der Film wurde als Allegorie auf den Kommunismus gelesen — was Forman stets bestritt. Er emigrierte 1968 in die USA und wurde zum wichtigsten tschechischen Emigranten-Regisseur."
    ),

    // Question 27 - Tschechische Neue Welle: Jiri Menzel
    Question(
        categoryId = 4,
        questionText = "Jiri Menzels 'Closely Watched Trains' (Ostre sledovane vlaky, 1966) gewann den Oscar fuer den Besten Fremdsprachigen Film 1968. Welchem literarischen Vorbild folgte das Drehbuch?",
        answerA = "Einem Roman von Milan Kundera",
        answerB = "Einem Roman von Bohumil Hrabal",
        answerC = "Einem Roman von Jaroslav Hasek",
        answerD = "Einem Roman von Josef Skvorecky",
        correctAnswer = 1,
        explanation = "Jiri Menzel adaptierte den gleichnamigen Roman von Bohumil Hrabal (1965). Hrabal und Menzel arbeiteten eng zusammen — Menzel verfilmte mehrere Hrabal-Werke ('Pearls of the Deep', 'Cutting It Short', 'The Snowdrop Festival'). Hrabal gilt als einer der groessten tschechischen Prosaisten des 20. Jahrhunderts.",
        difficulty = 5,
        funFact = "Bohumil Hrabal spielte eine kleine Nebenrolle in Menzels Verfilmung seines eigenen Romans 'Closely Watched Trains'. Die Zusammenarbeit der beiden war eine der fruchtbarsten Autor-Regisseur-Partnerschaften im europaeischen Arthouse-Kino."
    ),

    // Question 28 - Polnische Schule: Andrzej Wajda
    Question(
        categoryId = 4,
        questionText = "Andrzej Wajdas 'Kanal' (1957) zeigt die letzten Stunden des Warschauer Aufstands 1944 in den Abwasserkanaelen. Welche andere filmische Qualitaet hob 'Kanal' von anderen Kriegsfilmen der Zeit ab?",
        answerA = "Er war der erste polnische Film, der in Farbe gedreht wurde",
        answerB = "Er zeigte den Aufstand als tragisches Scheitern ohne heroische Verklaeung — die Helden sterben sinnlos",
        answerC = "Er verwendete ausschliesslich echte Aufstandsueberlebende als Darsteller",
        answerD = "Er war der erste Film weltweit, der in einem echten Abwasserkanal gedreht wurde",
        correctAnswer = 1,
        explanation = "Wajdas 'Kanal' unterschied sich radikal von sozialistischen Heldenfilmen, indem er den Aufstand als moralische und physische Tragodie zeigte: Die Kaempfer sterben erschoepft und desorientiert in den Kanaelen, ohne Ausweg und ohne Erloesung. Das war eine politisch gewagtes Statement im Nachkriegspolen unter kommunistischer Herrschaft.",
        difficulty = 5,
        funFact = "Wajdas Kriegstrilogie ('Generation' 1955, 'Kanal' 1957, 'Asche und Diamant' 1958) machte ihn international beruehmt. Sein Vater war einer der Tausenden polnischer Offiziere, die 1940 im Massaker von Katyn ermordet wurden — ein Trauma, das Wajda erst 2007 verarbeitete, als er den Film 'Katyn' drehen durfte."
    ),

    // Question 29 - Polnische Schule: Roman Polanski
    Question(
        categoryId = 4,
        questionText = "Roman Polanskis Abschlussfilm an der Lodzer Filmhochschule, der Kurzfilm 'Zwei Maenner und ein Schrank' (1958), gewann mehrere internationale Preise. Was zeigt dieser surreale Film?",
        answerA = "Zwei Maenner tragen einen Kleiderschrank durch Lodz und werden ueberall abgewiesen",
        answerB = "Zwei Maenner streiten sich in einem Schrank um denselben Platz",
        answerC = "Zwei Maenner bauen einen Schrank und entdecken darin eine Leiche",
        answerD = "Zwei Maenner kommen mit einem Schrank aus dem Meer und koennen die Gesellschaft nicht verstehen",
        correctAnswer = 3,
        explanation = "In Polanskis Abschlussfilm 'Dwaj ludzie z szafa' (1958) tauchen zwei Maenner aus dem Meer auf, tragen einen riesigen Schrank und versuchen, in die Gesellschaft integriert zu werden — werden aber ueberall abgewiesen und kehren am Ende ins Meer zurueck. Eine Parabel ueber Ausgrenzung und Nichtbehoerigkeit.",
        difficulty = 5,
        funFact = "Polanski spielte selbst eine der Hauptrollen in 'Zwei Maenner und ein Schrank'. Der Film wurde in Bruessel, Cannes und San Francisco ausgezeichnet und machte ihn international bekannt, noch bevor er seinen ersten Spielfilm drehte. Das Wasser-Motiv kehrt in vielen seiner Filme wieder ('Messer im Wasser', 'Chinatown')."
    ),

    // Question 30 - Nouvelle Vague: Agnes Varda
    Question(
        categoryId = 4,
        questionText = "Agnes Varda gilt als 'Grossmutter der Nouvelle Vague', obwohl sie selbst diese Bezeichnung ablehnte. Ihr Film 'Cleo von 5 bis 7' (1962) folgt einer Frau in Echtzeit. Was ist das strukturelle Besondere an diesem Film?",
        answerA = "Der Film spielt in exakt 90 Minuten Echtzeit und zeigt 90 Filmminuten",
        answerB = "Jede der 13 Sequenzen wird von einer anderen Kamerafrau gedreht",
        answerC = "Der Film besteht ausschliesslich aus Nahaufnahmen ohne einen einzigen Weitwinkelshot",
        answerD = "Die Protagonistin spricht waehrend des gesamten Films keinen einzigen Dialog",
        correctAnswer = 0,
        explanation = "'Cleo de 5 a 7' zeigt die zwei Stunden zwischen 17 und 19 Uhr, in denen eine Saengerin auf ihr Krebsdiagnose-Ergebnis wartet — in beinahe exakter Echtzeit. Die Filmlaenge entspricht fast exakt der erzaehlten Zeit, was ein ungewoehnlich immersives Erleben erzeugt.",
        difficulty = 5,
        funFact = "Varda besetzte in einer kurzen Parodie-Sequenz ihren damaligen Partner Jacques Demy, Jean-Luc Godard und Anna Karina fuer einen komischen Stummfilm-Scherz innerhalb des Films. Diese Geste unter Freunden der Nouvelle Vague wurde zu einem ikonischen Moment der Bewegung."
    ),

    // Question 31 - Koreanisches Kino: Im Kwon-taek
    Question(
        categoryId = 4,
        questionText = "Im Kwon-taek ist mit ueber 100 Filmen Suedkoreas produktivster Regisseur. Sein Film 'Chunhyang' (2000) war der erste koreanische Film im Wettbewerb von Cannes. Was macht seine Verfilmung des klassischen koreanischen Epos einzigartig in der Filmgeschichte?",
        answerA = "Er erzaehlt die Geschichte rueckwaerts, vom Ende zur Mitte zur Einleitung",
        answerB = "Der gesamte Film ist eine Pansori-Aufuehrung — ein Saenger performt die Geschichte live vor Publikum",
        answerC = "Der Film wurde in drei Sprachen gleichzeitig gedreht: Koreanisch, Chinesisch und Japanisch",
        answerD = "Alle Darsteller sind ueber 70 Jahre alt und spielen auch junge Figuren",
        correctAnswer = 1,
        explanation = "'Chunhyang' (2000) strukturiert den gesamten Film als Pansori-Aufuehrung: Ein Pansori-Saenger (Cho Sang-hyun) performt live die gesamte Geschichte auf der Buehne vor einem Publikum, waehrend die Filmbilder als visuelle Entsprechung der Musik dienen. Pansori ist ein traditionelles koreanisches Gesangs-Erzaehl-Genre.",
        difficulty = 5,
        funFact = "Pansori ist UNESCO-Immaterielles Kulturerbe. Das Genre erfordert jahrzehntelanges Training — ein einziger Saenger (Sori-kgun) und ein Trommler (Gosu) tragen stundenlange Epen vor. Im Kwon-taek wollte mit 'Chunhyang' diese bedrohte Kunstform fuer eine neue Generation zugaenglich machen."
    ),

    // Question 32 - Iranisches Kino: Mohsen Makhmalbaf
    Question(
        categoryId = 4,
        questionText = "Mohsen Makhmalbaf ist fuer eine ungewoehnliche biografische Episode bekannt: Er griff als Jugendlicher einen Polizisten an, sass im Gefaengnis, und wurde spaeter von eben diesem Polizisten als Darsteller gecastet. In welchem Film thematisierte er dieses Ereignis?",
        answerA = "Der Radfahrer (1987)",
        answerB = "Das Moment der Unschuld (1996)",
        answerC = "Kandahar (2001)",
        answerD = "Salaam Cinema (1995)",
        correctAnswer = 1,
        explanation = "'Nun va Goldoon' (Das Moment der Unschuld / A Moment of Innocence, 1996): Makhmalbaf rekonstruiert den Angriff auf den Polizisten aus 1974 gemeinsam mit dem Polizisten selbst — beide spielen sich und jeweils einen jungen Darsteller, der ihre junge Version verkoerpert. Der Film verschwimmt Vergangenheit und Gegenwart, Erinnerung und Konstruktion.",
        difficulty = 5,
        funFact = "Makhmalbaf war als Islamist zur Shah-Zeit inhaftiert worden und wurde nach der Islamischen Revolution 1979 befreit. Spaeter distanzierte er sich vom Regime und wurde zum kritischen Regisseur. Seine ganze Familie wurde zu Filmemachern: Frau, Toechter (darunter Samira Makhmalbaf) und Sohn."
    ),

    // Question 33 - Japanisches Kino: Nagisa Oshima
    Question(
        categoryId = 4,
        questionText = "Nagisa Oshimas 'Im Reich der Sinne' (Ai no corrida, 1976) verursachte einen internationalen Skandal wegen unzensierter Sexdarstellungen. Welcher aussergewoehnliche Umstand ermoeglichte die unzensierte franzoesische Version?",
        answerA = "Der Film wurde in Frankreich finanziert und verarbeitet, da japanisches Recht nur in Japan gilt",
        answerB = "Oshima erhielt eine Sondergenehmigung der japanischen Regierung fuer kuenstlerische Freiheit",
        answerC = "Die Schauspieler waren keine japanischen Staatsbuerger und fielen daher nicht unter japanische Sittengesetze",
        answerD = "Der Film wurde rueckwirkend als historisches Dokument klassifiziert und unterlag damit anderem Recht",
        correctAnswer = 0,
        explanation = "Der Film wurde von der franzoesischen Produktionsfirma Argos Films finanziert. Das belichtete Filmmaterial wurde nie nach Japan eingebracht — es ging direkt nach Frankreich zur Entwicklung und zum Schnitt. Da japanisches Pornografierecht nur auf japanischem Boden gilt, konnte das fertige Werk unzensiert in Frankreich gezeigt werden.",
        difficulty = 5,
        funFact = "In Japan selbst war und ist 'Im Reich der Sinne' bis heute nur in zensierter Version legal erhaeltlich. Der Film basiert auf dem wahren Fall von Sada Abe (1936), die ihren Liebhaber erwuergte und sein Geschlechtsteil abschnitt. Sada Abe wurde verhaftet und wurde zu einer seltsamen Kultfigur in Japan."
    ),

    // Question 34 - Taiwanesisches Kino: Hou Hsiao-hsien
    Question(
        categoryId = 4,
        questionText = "Hou Hsiao-hsiens 'A City of Sadness' (1989) war der erste Film, der das Massaker vom 28. Februar 1947 in Taiwan oeffentlich thematisierte. Warum war dieses Thema so lange ein Tabu?",
        answerA = "Das Massaker galt offiziell als kommunistischer Aufstand und durfte nicht als Staatsverbrechen dargestellt werden",
        answerB = "Das Massaker wurde von der KMT-Regierung 38 Jahre lang unter Kriegsrecht verschwiegen",
        answerC = "Die Ueberlebenden wurden unter Schweigepflicht gestellt und konnten erst nach deren Tod sprechen",
        answerD = "Das Massaker war in Taiwan bis 1987 durch ein Zensurgesetz explizit verboten zu erwaehnen",
        correctAnswer = 1,
        explanation = "Das 228-Massaker (28. Februar 1947), bei dem die von Chiang Kai-shek gefuehrte KMT-Regierung Zehntausende taiwanesischer Zivilisten toetete, war unter dem Kriegsrecht (1949-1987) 38 Jahre lang ein offizielles Tabu. Erst nach dem Ende des Kriegsrechts 1987 konnte Hou den Film drehen. 'A City of Sadness' brach das kollektive Schweigen.",
        difficulty = 5,
        funFact = "Hou Hsiao-hsien gewann fuer 'A City of Sadness' den Goldenen Loewen in Venedig 1989 — den ersten grossen internationalen Filmpreis fuer Taiwan. Der Film loeste in Taiwan eine gesellschaftliche Debatte aus und trug zur politischen Aufarbeitung des Massakers bei."
    ),

    // Question 35 - Taiwanesisches Kino: Edward Yang
    Question(
        categoryId = 4,
        questionText = "Edward Yangs 'Yi Yi — A One and a Two' (2000) gilt als Meisterwerk des Weltkinos. Fuer diesen Film erhielt Yang die Auszeichnung als Bester Regisseur in Cannes. Was ist die ungewoehnliche Struktur des Films in Bezug auf Zeit und Perspektive?",
        answerA = "Der Film zeigt dasselbe Wochenende dreimal aus drei verschiedenen Perspektiven",
        answerB = "Drei Generationen einer Familie werden gleichzeitig in parallelen Handlungstraengen gezeigt, die sich selten ueberschneiden",
        answerC = "Der Film spielt ausschliesslich in einem einzigen Apartment ueber drei Tage",
        answerD = "Jede Szene wird zweimal gezeigt — einmal aus Perspektive des Kindes, einmal aus der des Erwachsenen",
        correctAnswer = 1,
        explanation = "'Yi Yi' verfolgt drei Generationen simultan: Grossmutter (im Koma), Eltern (Midlife-Crisis und erste Liebe) und Kinder (Schulkind Yang-Yang, Teenagerin Ting-Ting). Die Handlungsstraenge verlaufen parallel, fast ohne dass die Generationen miteinander interagieren — ein mikrokosmisches Bild der modernen taiwanesischen Gesellschaft.",
        difficulty = 5,
        funFact = "Edward Yang starb 2007 mit 59 Jahren an Krebs. 'Yi Yi' war sein letzter vollendeter Film. Der amerikanische Kritiker Roger Ebert wahlte ihn in seine Liste der 10 besten Filme seiner Dekade. Yang hatte vor seinem Tod ein Animationsprojekt in Arbeit, das unvollendet blieb."
    ),

    // Question 36 - Neue Iranische Welle: Samira Makhmalbaf
    Question(
        categoryId = 4,
        questionText = "Samira Makhmalbaf gewann den Jury-Preis in Cannes fuer 'The Blackboards' (2000) im Alter von 20 Jahren und war damit die juengste Preistraegerin in der Geschichte von Cannes. In welchem Land und in welcher Sprache wurde der Film gedreht?",
        answerA = "Iran, in Persisch mit kurdischen Untertiteln",
        answerB = "Im kurdischen Grenzgebiet zwischen Iran und Irak, vorwiegend auf Kurdisch",
        answerC = "In Afghanistan, auf Dari Persisch nach der Taliban-Invasion",
        answerD = "In der Tuerkei, auf Kurdisch als verbotener Film",
        correctAnswer = 1,
        explanation = "'Takhte siah' (The Blackboards) wurde im kurdisch-iranischen Grenzgebiet (Kermanshah-Region) gedreht. Wandernde Lehrer tragen ihre Schultefeln durch die Berge auf der Suche nach Schueleern — waehrend kurdische Fuechtlinge aus dem Irak die Grenze ueberqueren. Die Dialoge sind ueberwiegend auf Kurdisch.",
        difficulty = 5,
        funFact = "Samira Makhmalbaf begann ihre Filmkarriere mit 17 Jahren mit 'The Apple' (1998), den sie innerhalb von elf Tagen drehte. Ihr Vater Mohsen gab ihr alle technische Unterstuetzung, aber keine inhaltlichen Vorgaben. Sie ist eine der wenigen Regisseurinnen weltweit, die international schon als Teenagerin anerkannt wurden."
    ),

    // Question 37 - Griechisches Kino: Theo Angelopoulos
    Question(
        categoryId = 4,
        questionText = "Theo Angelopoulos ist fuer seine extrem langen Einstellungen (Plan-Sequences) beruehmt. In seinem Film 'Der reisende Schauspieler' (1975) gibt es Einstellungen, die ungewoehlich lange dauern. Was zeigt die laengste Einstellung im Film ungefaehr?",
        answerA = "Ein komplettes Theaterstueck ohne einen einzigen Schnitt",
        answerB = "Einen politischen Wahlkampfauftritt, eine Wahlnacht und den naechsten Morgen in einer einzigen Kamerafahrt",
        answerC = "Einen kompletten Zugfahrt von Athen nach Thessaloniki",
        answerD = "Einen Leichenzug durch das gesamte Stadtzentrum von Athen",
        correctAnswer = 1,
        explanation = "In 'O Thiassos' (Der reisende Schauspieler) ueberbrueckt Angelopoulos mit einer einzigen Kamerafahrt mehrere Jahre der griechischen Geschichte — die Kamera gleitet durch eine Szene und erreicht eine andere Zeitebene. Die Einstellungen komprimieren Geschichte durch raeumlich-zeitliche Kontinuitaet statt durch Schnitt.",
        difficulty = 5,
        funFact = "Angelopoulos drehte 'Der reisende Schauspieler' (Dauer: 3 Stunden 50 Minuten) heimlich unter dem Militaerregime der Obristen (1967-1974). Das Skript behauptete, ein harmloser Tourismusfilm zu sein. Der Film enthuellt die politischen Verbrechen der Obristen-Diktatur in allegorischer Form."
    ),

    // Question 38 - Ungarisches Kino: Bela Tarr
    Question(
        categoryId = 4,
        questionText = "Bela Tarrs 'Satantango' (1994) dauert 7 Stunden 18 Minuten. Welche literarische Vorlage liegt dem Film zugrunde und wer ist der Autor?",
        answerA = "Laszlo Krasznahorkai, Roman 'Satantango' (1985)",
        answerB = "Imre Kertesz, Roman 'Schicksalslosigkeit' (1975)",
        answerC = "Gyoergy Konrad, Roman 'Der Stadtgruender' (1977)",
        answerD = "Miklos Meszoly, Roman 'Saulus' (1968)",
        correctAnswer = 0,
        explanation = "Bela Tarr verfilmte Laszlo Krasznahorkais Roman 'Satantango' von 1985 in enger Zusammenarbeit mit dem Autor — Krasznahorkai schrieb auch das Drehbuch. Die Zusammenarbeit der beiden ist eine der praegensten Autorenfilm-Partnerschaften des europaeischen Kinos. Krasznahorkai gewann 2015 den Man Booker International Prize.",
        difficulty = 5,
        funFact = "Susan Sontag schrieb ueber 'Satantango': Ich wuerde sofort fuer die Gelegenheit reisen, um diesen Film noch einmal zu sehen. Er ist einer der groessten Filme, die je gedreht wurden. Tarr kuendigte nach 'The Turin Horse' (2011) seinen Ruhestand an, weil er alles gesagt habe, was er sagen wollte."
    ),

    // Question 39 - Rumaenische Neue Welle: Cristian Mungiu
    Question(
        categoryId = 4,
        questionText = "Cristian Mungius '4 Monate, 3 Wochen und 2 Tage' (2007) gewann die Palme d'Or und spielt im kommunistischen Rumaenien unter Ceausescu. Was war in diesem politischen Kontext das zentrale verbotene Thema des Films?",
        answerA = "Homosexualitaet unter kommunistischer Herrschaft",
        answerB = "Abtreibung, die 1966 unter Ceausescu verboten und mit Gefaengnis bestraft worden war",
        answerC = "Schwarzmarkthandel und Schmuggel als Ueberlebensstrategie",
        answerD = "Religioeser Untergrund-Gottesdienst als verbotene Subkultur",
        correctAnswer = 1,
        explanation = "Ceausescus Regime verbot 1966 Abtreibung und Verhuetung, um die Bevoelkerung zu erhoehen. Das fuehrte zu illegalen Abtreibungen unter lebensgefaehrlichen Bedingungen und zu Hunderttausenden ungeplanter Kinder in staatlichen Waisenheimen. Mungius Film zeigt eine solche illegale Abtreibung in brutaler Realitaet.",
        difficulty = 5,
        funFact = "Der Film hatte an der Palme-d'Or-Verleihung Cannes 2007 einen skandaloesen Moment: Die Jury-Praesidentin Wong Kar-wai uebergab die Palme, ohne den rumae Beitrag zuvor erwaehnt zu haben. Mungiu zeigte sich schockiert. Rumaenien erlebte seitdem eine 'Neue Welle' mit mehreren internationalen Preisen."
    ),

    // Question 40 - Brasilianisches Kino: Cinema Novo
    Question(
        categoryId = 4,
        questionText = "Glauber Rochas Essay 'Aesthetik des Hungers' (1965) pragte das brasilianische Cinema Novo. Was war Rochas Kernthese ueber die Beziehung zwischen Armut und Gewalt im brasilianischen Kino?",
        answerA = "Armut und Gewalt muessen im Film realistisch gezeigt werden, um Mitleid zu erzeugen",
        answerB = "Nur durch Gewalt kann sich der kolonisierte Mensch befreien — Gewalt als einzig moegliche Sprache der Unterdrueckten",
        answerC = "Das brasilianische Kino muss Armut verherrlichen als Form des kulturellen Widerstands",
        answerD = "Die Aesthetik des Hungers bedeutet, mit minimalstem Budget maximale politische Wirkung zu erzielen",
        correctAnswer = 1,
        explanation = "Rochas These war radikal: Der hungernde, verzweifelte Mensch Lateinamerikas hat nur eine Sprache zur Verfuegung — die Gewalt. Diese Gewalt ist nicht barbarisch, sondern die einzige politische Reaktion auf koloniale Unterdrueckung. Das Cinema Novo muesse diese Aesthetik des Hungers zeigen, nicht aesthetisieren.",
        difficulty = 5,
        funFact = "Glauber Rocha starb 1981 mit nur 42 Jahren an einer Lungeninfektion. Sein Film 'Deus e o Diabo na Terra do Sol' (Black God, White Devil, 1964) gilt als Meisterwerk des Weltkinonos. Er drehte im nordbrasilianischen Sertao mit minimalstem Budget und echten Landbewohnern."
    ),

    // Question 41 - Chinesisches Kino: Funfte Generation
    Question(
        categoryId = 4,
        questionText = "Zhang Yimou und Chen Kaige waren Kommilitonen an der Beijing Film Academy und gehoeren zur sogenannten 'Fuenften Generation'. Warum wurde diese Generation so bezeichnet?",
        answerA = "Sie waren die fuenfte Regisseurskollegen seit der Gruendung der Volksrepublik China 1949",
        answerB = "Sie waren die ersten Absolventen nach der Kulturrevolution und bildeten die fuenfte Studentengeneration der Akademie",
        answerC = "Ihr Kino war die fuenfte Phase in der Entwicklung des chinesischen Films seit den 1920er Jahren",
        answerD = "Sie waren fuenf Regisseure, die gemeinsam ihr erstes Manifest veroeffentlichten",
        correctAnswer = 1,
        explanation = "Die Fuenfte Generation bezeichnet die Absolventen des Jahrgangs 1982 der Beijing Film Academy — der ersten Kohorte nach der Kulturrevolution (1966-1976), waehrend der die Akademie geschlossen war. Ihre Filme sind gepraegt von der traumatischen Erinnerung an die Kulturrevolution und einer neuen visuellen Sprache.",
        difficulty = 5,
        funFact = "Zhang Yimou war waehrend der Kulturrevolution Fabrikarbeiter und Landarbeiter, bevor er an der Filmakademie zugelassen wurde. Er begann als Kameramann (fuer Chen Kaiges 'Yellow Earth', 1984) und drehte erst spaeter eigene Filme. 'Rotes Kornfeld' (1987) war sein Regie-Debuetuehr und gewann den Goldenen Baer in Berlin."
    ),

    // Question 42 - Chinesisches Kino: Wong Kar-wai
    Question(
        categoryId = 4,
        questionText = "Wong Kar-wais 'Im Rausch der Zeit' (Chungking Express, 1994) entstand in einer Aussergewoehnlichen kurzen Zeit, waehrend Wong an einem anderen Projekt arbeitete. Was war das andere, gleichzeitig in Produktion befindliche Projekt?",
        answerA = "'Fallen Angels' (1995), das er parallel montierte",
        answerB = "'Ashes of Time' (1994), ein Martial-Arts-Film, der ihn auslaugte",
        answerC = "'Happy Together' (1997), das aus denselben Rohaufnahmen entstand",
        answerD = "'In the Mood for Love' (2000), fuer das er schon Material sammelte",
        correctAnswer = 1,
        explanation = "'Chungking Express' entstand als kreativer Ausbruch waehrend der quaelend langen Produktion von 'Ashes of Time' (1994), einem aufwendigen Wuxia-Film. Wong drehte 'Chungking Express' in 23 Tagen als Gegengewicht zur erschoepfenden Arbeit an 'Ashes of Time'. Beide Filme erschienen 1994.",
        difficulty = 5,
        funFact = "Wong Kar-wai dreht notorisch ohne abgeschlossenes Drehbuch — er entwickelt den Film beim Drehen. 'In the Mood for Love' (2000) entstand aus Aufnahmen, die urspruenglich fuer ein ganz anderes Projekt gemacht wurden. Kameramann Christopher Doyle und er arbeiteten jahrelang zusammen und wurden zu einem der praegensten Duos des Weltkin"
    ),

    // Question 43 - Russisches Kino: Andrei Tarkovsky
    Question(
        categoryId = 4,
        questionText = "Andrei Tarkovskys 'Andrei Rublev' (1966/1969) durfte nach seiner Fertigstellung jahrelang nicht in der Sowjetunion gezeigt werden. Wie gelangte der Film trotzdem ans internationale Publikum?",
        answerA = "Der Film wurde auf Videokassette aus der UdSSR geschmuggelt",
        answerB = "Er wurde ohne sowjetische Genehmigung auf dem Filmfestival Cannes 1969 gezeigt",
        answerC = "Westdeutsche Diplomaten brachten eine Kopie nach Muenchen",
        answerD = "Ein KGB-Agent verkaufte eine Kopie an ein franzoesisches Verleihunternehmen",
        correctAnswer = 1,
        explanation = "Eine Kopie von 'Andrei Rublev' gelangte nach Frankreich und wurde 1969 ohne offizielle sowjetische Genehmigung beim Festival de Cannes gezeigt — als informeller Sondervorstellung ausserhalb des Wettbewerbs. Der Film sorgte fuer Aufsehen; die sowjetischen Behoerden waren offiziell empoe. In der UdSSR selbst erschien er erst 1971 in stark gekuerzter Version.",
        difficulty = 5,
        funFact = "Tarkovsky filmte den Film in Schwarz-Weiss und fugte am Ende eine kurze Farbsequenz ein — die einzige Farbe im gesamten Film. Sie zeigt die echten Ikonen von Andrei Rublev in leuchtenden Farben, nachdem der gesamte Film in Graustufen die Entstehung dieser Kunst gezeigt hat. Der Effekt ist ueberwaetigend."
    ),

    // Question 44 - Russisches Kino: Dziga Vertov
    Question(
        categoryId = 4,
        questionText = "Dziga Vertovs 'Der Mann mit der Kamera' (1929) gilt als einer der radikalsten Dokumentarfilme der Geschichte. Was ist das filmtheoretische Konzept, das Vertov diesem Film zugrundegelegt hatte und wie nannte er es?",
        answerA = "Kino-Wahrheit (Kino-Pravda) — die Kamera als objektives Auge der Wahrheit",
        answerB = "Kino-Auge (Kino-Glaz) — die Kamera als uebermenschliches Auge, das besser sieht als das menschliche",
        answerC = "Kino-Faust (Kino-Kulak) — die Kamera als politische Waffe des Proletariats",
        answerD = "Kino-Traum (Kino-Son) — die Kamera zeigt, was das Bewusstsein nicht direkt sehen kann",
        correctAnswer = 1,
        explanation = "Vertovs Konzept des 'Kino-Glaz' (Kino-Auge) postulierte die Kamera als ein Instrument, das die Realitaet besser, schneller und wahrer erfassen kann als das menschliche Auge. Durch Zeitlupe, Zeitraffer, Mehrfachbelichtung und Montage kann die Kamera die verborgene Wahrheit der Welt enthullen. 'Der Mann mit der Kamera' ist die Demonstration dieses Konzepts.",
        difficulty = 5,
        funFact = "Vertov (buergerlicher Name Denis Kaufman) waehlte sein Pseudonym nach dem ukrainischen Wort 'vertit' (drehen/wirbeln). Sein Bruder Mikhail Kaufman war der Kameramann in 'Der Mann mit der Kamera'. Der Film hat bis heute keinen einzigen Dialog, keinen Zwischentitel und kein Drehbuch — es ist reine Montage."
    ),

    // Question 45 - Mexikanisches Kino: Carlos Reygadas
    Question(
        categoryId = 4,
        questionText = "Carlos Reygadas gewann 2012 den Preis fuer die Beste Regie in Cannes fuer 'Post Tenebras Lux'. Was ist das Besondere an der Kineamtografie dieses Films, die er mit Kameramann Alexis Zabe entwickelte?",
        answerA = "Der Film wurde vollstaendig in extremer Zeitlupe gedreht und dann beschleunigt",
        answerB = "Alle Aufnahmen wurden mit einem deformierten Anamorphot-Vorsatz gedreht, der die Bildraender verzerrt",
        answerC = "Der Film wechselt staendig zwischen 16mm, 35mm und digitalem Material",
        answerD = "Alle Aussenaufnahmen wurden ohne Stativ auf dem Boden liegend gedreht",
        correctAnswer = 1,
        explanation = "Reygadas und Zabe entwickelten fuer 'Post Tenebras Lux' eine spezifische optische Verzerrung durch einen angepassten Anamorphot: Die Bildraender sind unscharf und verzerrt, nur die Bildmitte ist scharf — wie eine Erinnerung oder ein Traum. Diese visuelle Signatur war intentionell und polarisierte die Kritik in Cannes.",
        difficulty = 5,
        funFact = "Reygadas besetzte in 'Post Tenebras Lux' seine eigene Frau und seine echten Kinder als Protagonistenfamilie. Er lebt auf einer echten Farm in Mexiko — viele Bilder des Films stammen aus seinem eigenen alltaeglichen Leben. Der Titel ist Latein: 'Nach der Dunkelheit das Licht'."
    ),

    // Question 46 - Argentinisches Kino: Lucrecia Martel
    Question(
        categoryId = 4,
        questionText = "Lucrecia Martels Debuetuehr 'La Cienaga' (Die Zuflucht, 2001) spielt in der argentinischen Provinz Salta. Welches filmische Mittel setzt Martel ein, um sozialen Verfall und Stagnation zu vermitteln, ohne explizite politische Aussagen zu machen?",
        answerA = "Sie zeigt ausschliesslich die Perspektive eines Kindes, das die Erwachsenenwelt nicht versteht",
        answerB = "Eine schwebende, desorientierte Atmosphaere durch ungewoehnliche Kamerapositionierung, Off-Sounds und fehlende Handlungsprogression",
        answerC = "Sie setzt Zeitraffer-Sequenzen ein, die den Zerfall des Hauses und der Figuren zeigen",
        answerD = "Der gesamte Film wird in extremer Nah-Perspektive gedreht ohne einen einzigen Weitwinkelshot",
        correctAnswer = 1,
        explanation = "Martel schafft in 'La Cienaga' durch Close-ups ohne Kontext, mehrdeutige Geraeusche von ausserhalb des Bildes, verwobene Dialoge die sich nicht aufloesen, und eine Handlung die nirgendwo hinfuehrt, eine bedrohliche Atmosphaere des Stagnierens. Die privilegierte Klasse Saltas verrottet am Pool — ohne dass etwas passiert.",
        difficulty = 5,
        funFact = "Lucrecia Martel stammt selbst aus Salta und verarbeitete in 'La Cienaga' eigene Kindheitserinnerungen. Martin Scorsese nannte sie eine der bedeutendsten Regisseurinnen der Gegenwart. Ihr dritter Film 'Zama' (2017) ist eine Adaption eines kanonischen argentinischen Romans ueber koloniale Macht."
    ),

    // Question 47 - Senegalisches Kino: Djibril Diop Mambety
    Question(
        categoryId = 4,
        questionText = "Djibril Diop Mambe tys 'Touki Bouki' (1973) aus Senegal gilt als eines der grossen Meisterwerke des afrikanischen Kinos. Welches internationales Filmwerk beeinflusste den Film stilistisch und narrativ erkennbar?",
        answerA = "Fellinis '8 1/2' — das Motiv des Kuenstlers der sich selbst sucht",
        answerB = "Godards 'Ausser Atem' — das Liebespaar auf der Flucht, Nouvelle Vague-Technik",
        answerC = "Kurosawas 'Rashomon' — mehrfache Perspektiven auf dieselbe Geschichte",
        answerD = "Bunuels 'Los Olvidados' — der neorealistische Blick auf Jugendarmut",
        correctAnswer = 1,
        explanation = "'Touki Bouki' (Hyaene auf der Jagd) teilt mit Godards 'Ausser Atem' das Liebespaar auf der Flucht, Jump Cuts, fragmentarische Erz ahlweise und eine Ablehnung konventioneller Handlungslogik. Mambety verband diese Nouvelle Vague-Einfluesse mit westafrikanischer Bildsprache und senegalesischer Populaerkultur.",
        difficulty = 5,
        funFact = "Der Film erschien im selben Jahr wie Sembenes 'Xala'. Waehrend Sembene politisch direkter war, arbeitete Mambety poetischer und surrealer. Beide galten als Pioniere, vertraten aber sehr unterschiedliche aesthetische Philosophien. Mambety starb 1998 — er hatte in 25 Jahren nur zwei Spielfilme abgeschlossen."
    ),

    // Question 48 - Slowakisches Kino: Stefan Uher
    Question(
        categoryId = 4,
        questionText = "Stefan Uhers 'Das Sonnenlicht bringt Schatten' (Slnko v sieti, 1962) gilt als erster Film der Tschechoslowakischen Neuen Welle, obwohl er aus der Slowakei stammt. Welches stilistische Merkmal qualifiziert ihn als Schluessel werk der Bewegung?",
        answerA = "Er war der erste tschechoslowakische Film, der im Ausland finanziert wurde",
        answerB = "Die Verbindung von subjektiver Kamera, freiem Schnitt und dem Alltagsleben junger Menschen im Sozialismus",
        answerC = "Er war der erste Film der Tschechoslowakei, der weibliche Sexualitaet offen thematisierte",
        answerD = "Er verwendete als erster tschechoslowakischer Film ausschliesslich natuerliches Licht ohne Studiomittel",
        correctAnswer = 1,
        explanation = "'Slnko v sieti' (1962) verband subjektive Kamerabewegung, assoziative Montage und die alltaegliche Realitaet junger Menschen im sozialistischen Bratislava. Diese Kombination aus formaler Freiheit und gesellschaftlicher Ehrlichkeit pragte die gesamte nachfolgende tschechische und slowakische Filmwelle der 1960er Jahre.",
        difficulty = 5,
        funFact = "Stefan Uher ist in Westeuropa kaum bekannt, obwohl sein Film 'Slnko v sieti' chronologisch noch vor Forman und Menzel liegt. Die Slowakei hatte eine eigenstaendige Filmtradition innerhalb der Tschechoslowakei, die oft im Schatten der Prager Schule stand."
    ),

    // Question 49 - Jugoslawisches Kino: Dusan Makavejev
    Question(
        categoryId = 4,
        questionText = "Dusan Makavejevs 'W.R. — Mysterien des Organismus' (1971) kombinierte zwei scheinbar unverbindbare Elemente. Was sind diese beiden Ebenen des Films?",
        answerA = "Eine Dokumentation ueber Sigmund Freud und eine fiktive Geschichte in Belgrad",
        answerB = "Eine Dokumentation ueber Wilhelm Reichs Sexualtheorie und eine satirische Spielfilmgeschichte in Jugoslawien",
        answerC = "Archivaufnahmen aus dem Zweiten Weltkrieg und eine Liebesgeschichte im zeitgenoessischen Jugoslawien",
        answerD = "Eine Reportage ueber die Revolution 1968 in Paris und eine Satire auf den Tito-Kommunismus",
        correctAnswer = 1,
        explanation = "Makavejev kombinierte in 'WR: Mysteries of the Organism' eine Dokumentation ueber den deutsch-oesterreichischen Psychoanalytiker Wilhelm Reich (Sexualrevolution, Orgon-Energie) mit einer satirischen Spielfilm-Geschichte in Jugoslawien ueber eine kommunistische Frau, die sich in einen sowjetischen Eiskunstlaeufer verliebt. Der Film ist politisch, sexuell und filmisch radikal.",
        difficulty = 5,
        funFact = "Makavejev wurde nach 'W.R.' in Jugoslawien faktisch mit einem Filmverbot belegt und emigrierte in den Westen. Der Film war im kommunistischen Jugoslawien aus politischen Gruenden verboten — paradoxerweise auch im Westen in einigen Laendern wegen Sexualdarstellungen. Er schaffte es, von allen Seiten gleichzeitig zensiert zu werden."
    ),

    // Question 50 - Kambodschanisches Kino: Rithy Panh
    Question(
        categoryId = 4,
        questionText = "Der kambodschanische Regisseur Rithy Panh ueberlebte als Kind den Voelkermord der Roten Khmer und machte Dokumentarfilme ueber dieses Trauma. Sein Film 'Das fehlende Bild' (L'image manquante, 2013) gewann in Cannes. Welches ungewoehnliche filmische Mittel nutzte er, da es kaum Filmaufnahmen aus der Khmer-Rouge-Zeit gibt?",
        answerA = "Er liess Ueberlebende nachgestellte Szenen neu inszenieren",
        answerB = "Er erstellte handbemalte Tonfiguren und Dioramen zur Rekonstruktion der fehlenden Erinnerungen",
        answerC = "Er kombinierte Satellitenbilder mit persoenlichen Tagebucheintragen",
        answerD = "Er verwendete nur gefundenes Archivmaterial ohne jede Rekonstruktion",
        correctAnswer = 1,
        explanation = "Rithy Panh liess handgefertigte Tonfiguren und miniaturisierte Dioramen bauen, die Szenen aus der Khmer-Rouge-Zeit rekonstruieren, fuer die kein Filmmaterial existiert. Die Kamera filmt diese Miniaturfiguren mit der gleichen Sorgfalt wie echte Darsteller — ein poetisches und schmerzhaftes Nachdenken darueber, was fehlt.",
        difficulty = 5,
        funFact = "Rithy Panh verlor fast seine gesamte Familie unter den Roten Khmer (1975-1979). Er gruendete das Bophana Audiovisual Resource Center in Phnom Penh zur Bewahrung kambodschanischen Filmerbes. Nur ein Bruchteil der kambodschanischen Filme aus der Zeit vor 1975 sind erhalten — die Roten Khmer zerstoerten gezielt Kultureinrichtungen."
    ),
)
