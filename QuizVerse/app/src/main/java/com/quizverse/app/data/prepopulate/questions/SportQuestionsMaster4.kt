package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsMaster4(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) ── 50 questions ──────────────────────────────

    // ── Antike Sportgeschichte (10) ───────────────────────────────────────────

    // Question 1 – Ancient Olympics: foundation myth
    Question(
        categoryId = 6,
        questionText = "Welcher griechische Mythologie-Heros gilt laut Pindar (5. Jh. v. Chr.) als Stifter der antiken Olympischen Spiele, und welches Ereignis feierte er damit?",
        answerA = "Zeus, der damit seinen Sieg ueber die Titanen feierte",
        answerB = "Herakles, der damit seinen Sieg im Wagenrennen gegen Augeias feierte und den Hain als heiligen Bezirk absteckte",
        answerC = "Pelops, der damit seinen Sieg im Wagenrennen gegen Oinomaos und die Errettung Hippodameias feierte",
        answerD = "Iphitos von Elis, der den Heiligen Frieden (Ekecheiria) proklamierte und damit die Spiele gruendete",
        correctAnswer = 2,
        explanation = "Pindar beschreibt in seinen Olympischen Oden Pelops als mythischen Stifter: Er gewann das Wagenrennen gegen Koenig Oinomaos von Pisa, der alle Freier seiner Tochter Hippodameia toetete. Der historische Gruender Iphitos (884 v. Chr.) steht daneben als historische Figur, ist aber mit der Neubelebung der Spiele verbunden, nicht mit dem Mythos.",
        difficulty = 5,
        funFact = "Pausanias berichtete im 2. Jh. n. Chr., im Heiligtum von Olympia die Diskusscheibe des Iphitos gesehen zu haben, in die der Ekecheiria-Vertrag eingeritzt war — ein Heiliger Waffenstillstand, der allen Griechen sichere Reise zu den Spielen garantierte."
    ),

    // Question 2 – Ancient Olympics: pentathlon composition
    Question(
        categoryId = 6,
        questionText = "Aus welchen fuenf Disziplinen bestand der antike griechische Pentathlon der Olympischen Spiele, und seit welchem Jahr wurde er dort erstmals ausgetragen?",
        answerA = "Lauf, Weitsprung, Diskuswurf, Speerwerfen, Ringen — seit 708 v. Chr.",
        answerB = "Lauf, Hochsprung, Speerwerfen, Boxen, Ringen — seit 724 v. Chr.",
        answerC = "Diskuswurf, Speerwurf, Steinwurf, Lauf, Faustkampf — seit 680 v. Chr.",
        answerD = "Weitsprung, Diskuswurf, Speerwerfen, Wagenrennen, Ringen — seit 648 v. Chr.",
        correctAnswer = 0,
        explanation = "Der Pentathlon wurde 708 v. Chr. in Olympia eingefuehrt und umfasste Weitsprung, Diskuswurf, Speerwerfen, Stadionlauf und Ringen. Die genaue Wertungsregel ist umstritten; wahrscheinlich musste ein Athlet drei Einzel-Siege erringen, um Gesamtsieger zu werden.",
        difficulty = 5,
        funFact = "Der Weitsprung im antiken Pentathlon wurde mit Haltgewichten (Halteres) ausgefuehrt — kurzenfoermige Bleibloecke, die der Athlet in den Haenden schwang, um Schwung zu erzeugen. Die gemessenen Weiten von bis zu 16 Metern sind physiologisch nur als Mehrfachsprung erkllaerbar."
    ),

    // Question 3 – Ancient Olympics: women's games
    Question(
        categoryId = 6,
        questionText = "Wie hiessen die eigenstaendigen Wettkampfspiele fuer Frauen, die parallel zu den antiken Olympischen Spielen in Olympia abgehalten wurden, und welcher Gottheit waren sie geweiht?",
        answerA = "Pythia — geweiht der Artemis",
        answerB = "Heraia — geweiht der Hera",
        answerC = "Isthmia — geweiht der Athena",
        answerD = "Nemeia — geweiht der Demeter",
        correctAnswer = 1,
        explanation = "Die Heraia waren Wettkampfspiele zu Ehren der Goettin Hera, ausschliesslich fuer unverheiratete Maedchen. Sie liefen in verkuerzten Chitons auf der olympischen Laufbahn, die fuer sie auf 5/6 der Laenge verkuerzt wurde. Verheiratete Frauen durften weder bei den Olympischen Spielen noch bei den Heraia teilnehmen.",
        difficulty = 5,
        funFact = "Pausanias berichtet, dass die sechzehn Frauen von Elis, die die Heraia organisierten, auch den Peplos (Festgewand) fuer Hera webten — eine Verbindung zwischen Textilkunst und Sportwettkampf, die in der antiken Welt einzigartig war."
    ),

    // Question 4 – Ancient Rome: gladiatorial schools
    Question(
        categoryId = 6,
        questionText = "Welcher Begriff bezeichnete die staatlichen Gladiatorenschulen im Roemischen Reich, und welche war die groesste und beruehrendste, die Kaiser Domitian in Rom errichten liess?",
        answerA = "Ludus Magnus — erbaut unter Domitian neben dem Colosseum, verbunden durch einen unterirdischen Gang",
        answerB = "Circus Maximus — Haupttrainingsstaette fuer alle Gladiatorentypen unter Augustus",
        answerC = "Amphitheatrum Flavium — gleichzeitig Kampf- und Ausbildungsstaette unter Vespasian",
        answerD = "Ludus Imperialis — kaiserliche Privatschule unter Nero in den Gaerten des Vatikans",
        correctAnswer = 0,
        explanation = "Der Ludus Magnus war die groesste der vier kaiserlichen Gladiatorenschulen in Rom und lag direkt nordoestlich des Colosseums. Ein unterirdischer Tunnel verband beide Gebaeude, sodass Gladiatoren direkt in die Arena einziehen konnten. Heute sind die Fundamentreste unter der Via Sacra sichtbar.",
        difficulty = 5,
        funFact = "In Capua befand sich der beruehm-beruechtigte Ludus Capuae, aus dem 73 v. Chr. Spartacus mit etwa 70 Gladiatoren ausbrach und den groessten Sklavenaufstand der roemischen Geschichte ausloeste — ein Aufstand, der fast drei Jahre dauerte."
    ),

    // Question 5 – Ancient Olympics: last games
    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fanden die letzten antiken Olympischen Spiele statt, und welches kaiserliches Edikt beendete sie offiziell?",
        answerA = "393 n. Chr. — durch das Edikt von Thessaloniki des Kaisers Theodosius I., das heidnische Kulte verbot",
        answerB = "426 n. Chr. — durch das Edikt des Kaisers Theodosius II., das den Zeustempel zerstoerte",
        answerC = "380 n. Chr. — durch das Edikt des Kaisers Gratianus, der den Pontifex-Maximus-Titel ablegte",
        answerD = "529 n. Chr. — durch das Edikt des Kaisers Justinian I., das alle heidnischen Schulen schloss",
        correctAnswer = 0,
        explanation = "Die letzten belegten antiken Olympischen Spiele fanden 393 n. Chr. statt. Theodosius I. erliess mit dem Edikt von Thessaloniki 380 n. Chr. das Christentum als Staatsreligion und verbot 391/392 n. Chr. heidnische Kulthandlungen, was das Ende der Spiele bedeutete. Das genaue Enddatum ist historisch umstritten, 393 gilt als wissenschaftlicher Konsens.",
        difficulty = 5,
        funFact = "Die antiken Olympischen Spiele dauerten 1169 Jahre — von 776 v. Chr. bis 393 n. Chr. In dieser Zeit fanden 293 Olympiaden statt. Der erste namentlich bekannte Olympionike war Koroibos von Elis, ein Backer, der 776 v. Chr. den Stadionlauf gewann."
    ),

    // Question 6 – Ancient sport: Roman chariot racing factions
    Question(
        categoryId = 6,
        questionText = "Welche vier Renngemeinschaften (Factiones) dominierten die Wagenrennen im Circus Maximus des Roemischen Reiches, und welche politische Bedeutung erlangten sie spaetestens in der Spaetantike?",
        answerA = "Rot, Weiss, Blau, Gruen — in der Spaetantike wurden Blau und Gruen zu Parteiorganisationen mit eigenem Militaer und politischem Einfluss",
        answerB = "Gold, Silber, Kupfer, Bronze — symbolisierten die vier Staende der roemischen Gesellschaft",
        answerC = "Loewen, Adler, Schlangen, Stiere — Wappenzeichen der vier groessten Stadtbezirke Roms",
        answerD = "Norden, Sueden, Osten, Westen — die Himmelsrichtungen als Symbole kosmischer Ordnung",
        correctAnswer = 0,
        explanation = "Die vier Factiones hiessen Russata (Rot), Albata (Weiss), Veneta (Blau) und Prasina (Gruen). In Konstantinopel wurden Blau und Gruen zu maechtigen politischen Parteien; beim Nika-Aufstand 532 n. Chr. gegen Kaiser Justinian I. kooperierten beide Fraktionen, zerstoerten fast die halbe Stadt und toeteten Zehntausende.",
        difficulty = 5,
        funFact = "Der beruehm-beruechtigste Wagenlenker der Antike, Gaius Appuleius Diocles, gewann in 24 Jahren 1462 von 4257 Rennen. Historiker berechneten, dass sein Gesamtpreisgeld von 35,8 Millionen Sesterzen dem Jahressold von 15 roemischen Legionen entsprach — er war vermutlich der bestbezahlte Sportler aller Zeiten."
    ),

    // Question 7 – Ancient sport: Pankration rules
    Question(
        categoryId = 6,
        questionText = "Welche Kampftechniken waren beim antiken griechischen Pankration ausdruecklich verboten, und was war die einzige Moeglichkeit eines Kaempfers, den Kampf zu beenden?",
        answerA = "Verboten: Beissen und Augenstecherei; Aufgabe durch Erheben des Zeigefingers oder Tod des Gegners",
        answerB = "Verboten: Waffeneinsatz und Schlaege unter die Guertelline; Aufgabe durch verbalen Ausruf",
        answerC = "Verboten: Haareissen und Kratzen; Aufgabe durch dreimaliges Beruehren des Bodens",
        answerD = "Es gab keine Verbote — Pankration war ein regelloser Kampf bis zur absoluten Erschoepfung",
        correctAnswer = 0,
        explanation = "Beim Pankration waren lediglich Beissen und das Stechen in die Augen verboten. Ein Kaempfer konnte aufgeben, indem er den Zeigefinger hob — der Schiedsrichter (Alytes) beendete dann den Kampf. Todesfaelle kamen vor; der Sieger konnte posthum mit einem Olympiasieg geehrt werden, falls sein Gegner als letzter aufgab.",
        difficulty = 5,
        funFact = "Arrhichion von Phigalia gewann 564 v. Chr. seinen dritten Olympia-Sieg im Pankration in dem Moment, als ihn sein Gegner erwuergte — sterbend brach er den Knoechel des Gegners, der daraufhin aufgab. Die Kampfrichter erklaerten Arrhichion posthum zum Sieger. Seine Geschichte gilt als einer der dramatischsten Momente der antiken Sportgeschichte."
    ),

    // Question 8 – Ancient sport: Greek gymnasion
    Question(
        categoryId = 6,
        questionText = "Welche zentrale gesellschaftliche und philosophische Funktion erfuellte das griechische Gymnasion jenseits seiner athletischen Rolle, und welche beruehm-beruechtigte Einrichtung war typischerweise angeschlossen?",
        answerA = "Es war primaer eine Militaerschule; angeschlossen war die Ephebeia zur Ausbildung zukuenftiger Soldaten",
        answerB = "Es war Zentrum fuer koerperliche und geistige Bildung; angeschlossen war die Xystos-Halle als Philosophen-Treffpunkt — Platon lehrte in der Akademia, Aristoteles im Lykeion, beide Gymnasien",
        answerC = "Es war eine medizinische Einrichtung; angeschlossen war das Asklepieion zur Behandlung von Sportverletzungen",
        answerD = "Es war eine religioes-kultische Staette; angeschlossen war der Temenos als Opferbezirk fuer Hermes und Herakles",
        correctAnswer = 1,
        explanation = "Das Gymnasion war das intellektuelle und koerperliche Herz der griechischen Polis. Platons Akademia und Aristoteles' Lykeion waren explizit als Gymnasien angelegt. Die Xystos-Halle (gedeckte Laufbahn) diente bei schlechtem Wetter fuer Training und Diskussion. Das Wort 'Gymnasium' lebt bis heute in der deutschen Bildungstradition fort.",
        difficulty = 5,
        funFact = "Das Wort 'Gymnasion' leitet sich von 'gymnos' (nackt) ab — griechische Athleten trainierten und kaempften traditionell ohne Kleidung. Frauen hatten keinen Zutritt. Diese Nacktheit war eine bewusste zivilisatorische Distinktion: Barbaren trugen Kleidung beim Sport, Griechen nicht."
    ),

    // Question 9 – Ancient sport: Roman gladiator types
    Question(
        categoryId = 6,
        questionText = "Welcher Gladiatorentyp kaempfte mit einem Dreizack (Fuscina) und einem Netz (Rete) und galt als das technisch anspruchsvollste Gladiatoren-Profil im roemischen Arena-Sport?",
        answerA = "Secutor — der 'Verfolger' mit Helm und Kurzschwert",
        answerB = "Retiarius — der Netzkampfer, der mit Dreizack, Netz und Dolch kaempfte und keinen geschlossenen Helm trug",
        answerC = "Murmillo — der 'Fischkampfer' mit Fischhelm und grossem Schild",
        answerD = "Dimachaerus — der Doppelschwert-Kaempfer ohne Schild",
        correctAnswer = 1,
        explanation = "Der Retiarius war der einzige Gladiatorentyp ohne geschlossenen Helm — er trug nur eine Schulterplatte (Galerus) links. Er versuchte, seinen Gegner (meist Secutor oder Murmillo) mit dem Netz zu fangen und mit Dreizack oder Dolch zu bezwingen. Die offene Silhouette machte ihn beim Publikum erkennbar und gleichzeitig verletzlich.",
        difficulty = 5,
        funFact = "Der Retiarius wurde gesellschaftlich als niedriger angesehen als andere Gladiatorentypen, weil er 'fliehend' kaempfte statt standzuhalten. Juvenal verspottete einen Adligen, der als Retiarius auftrat, als besonders entehrend — das Netz und der Dreizack galten als Fischerhandwerk, nicht als Kriegswaffen."
    ),

    // Question 10 – Ancient Olympics: truce mechanism
    Question(
        categoryId = 6,
        questionText = "Wie lange dauerte der Heilige Waffenstillstand (Ekecheiria) fuer die antiken Olympischen Spiele, und welche drei Stadte Elis waren Garanten dieses Friedens?",
        answerA = "Ein Monat — Olympia, Pisa und Elis",
        answerB = "Drei Monate — Elis, Pisa und Skillous",
        answerC = "Zwei Monate — Elis, Pisa und Dyme",
        answerD = "Sechs Wochen — Elis, Olympia und Triphylien",
        correctAnswer = 1,
        explanation = "Der Ekecheiria dauerte urspruenglich einen Monat, wurde aber spaeter auf drei Monate ausgedehnt, um Reisenden aus dem gesamten griechischen Raum sichere Anreise, Teilnahme und Heimreise zu garantieren. Theoretisch. In der Praxis wurde er haeufig gebrochen — auch von Sparta, das deshalb einmal von den Spielen ausgeschlossen wurde.",
        difficulty = 5,
        funFact = "Die UN-Resolution zur Olympischen Waffenruhe, die seit 1993 vor jedem Olympischen Spiel verabschiedet wird, beruft sich explizit auf den antiken Ekecheiria. Im modernen Syrien-Konflikt 2016 hielten sich manche Kampfparteien tatsaechlich waehrend der Spiele in Rio an eine informelle Feuerpause."
    ),

    // ── Olympische Spiele-Geschichte 1896–1960 (15) ───────────────────────────

    // Question 11 – 1896 Athens: IOC founding
    Question(
        categoryId = 6,
        questionText = "In welchem Jahr und an welchem genauen Ort wurde das Internationale Olympische Komitee (IOC) gegruendet, und wer war der erste Praesident?",
        answerA = "1894 in Paris, im Amphitheatre der Sorbonne — erster Praesident: Demetrios Vikelas (Griechenland)",
        answerB = "1896 in Athen, nach den ersten Spielen — erster Praesident: Pierre de Coubertin (Frankreich)",
        answerC = "1892 in London, nach Coubertins Rede vor der USFSA — erster Praesident: Lord Desborough (Grossbritannien)",
        answerD = "1894 in Bruessel, auf dem ersten Sportkongress — erster Praesident: Viktor Balck (Schweden)",
        correctAnswer = 0,
        explanation = "Das IOC wurde am 23. Juni 1894 auf dem Internationalen Athletik-Kongress an der Sorbonne in Paris gegruendet. Erster Praesident wurde der Grieche Demetrios Vikelas, der nach den ersten Spielen 1896 zuruecktrat. Pierre de Coubertin folgte als zweiter Praesident und blieb es bis 1925.",
        difficulty = 5,
        funFact = "Coubertin hatte urspruenglich 1900 in Paris als Ort fuer die ersten modernen Spiele geplant — passend zur Weltausstellung. Vikelas ueberzeugte ihn, Athen als symbolischen Startpunkt zu waehlen. Die Spiele 1900 fanden dann trotzdem in Paris statt, aber als zweite Ausgabe."
    ),

    // Question 12 – 1900 Paris: women's participation
    Question(
        categoryId = 6,
        questionText = "Welche Sportart war die erste, in der Frauen bei den Olympischen Spielen 1900 in Paris offiziell teilnahmen, und wer gewann die erste olympische Goldmedaille fuer eine Frau?",
        answerA = "Lawn Tennis — Charlotte Cooper (Grossbritannien) gewann Einzel und Mixed",
        answerB = "Golf — Margaret Abbott (USA) gewann das Damen-Turnier mit 47 Schlaegen",
        answerC = "Segeln — Helene de Pourtalees (Schweiz) war Teil der preisgekroenten Besatzung",
        answerD = "Bogenschiessen — Queenie Newall (Grossbritannien) gewann mit 664 Ringen",
        correctAnswer = 0,
        explanation = "Charlotte Cooper aus Grossbritannien gewann 1900 in Paris sowohl das Damen-Einzel als auch das Mixed-Doppel im Tennis — damit war sie die erste Frau, die eine olympische Goldmedaille gewann. Frauen nahmen erstmals 1900 an Olympia teil; insgesamt waren es 22 Athletinnen bei ueber 1000 Teilnehmern.",
        difficulty = 5,
        funFact = "Die Spiele 1900 in Paris waren so chaotisch organisiert, dass viele Athleten erst spaeter erfuhren, dass sie bei Olympia teilgenommen hatten. Margaret Abbott, die Golf-Gewinnerin, wusste bis zu ihrem Tod 1955 nicht, dass ihr Turniersieg ein olympischer Wettkampf gewesen war."
    ),

    // Question 13 – 1904 St. Louis: marathon scandal
    Question(
        categoryId = 6,
        questionText = "Was geschah beim Marathon der Olympischen Spiele 1904 in St. Louis mit dem vermeintlichen Erstplatzierten Fred Lorz, und was machte den Sieg des eigentlichen Gewinners Thomas Hicks besonders skandaloes?",
        answerA = "Lorz wurde disqualifiziert, weil er 19 km im Auto mitgefahren war; Hicks' Sieg war wegen Strychnin- und Kognak-Dopings durch seine Betreuer umstritten, wurde aber bestaetigt",
        answerB = "Lorz lief die falsche Strecke und kam zehn Minuten zu frueh ins Ziel; Hicks wurde wegen Bluttransfusion spaeter disqualifiziert",
        answerC = "Lorz kollabierte nach dem Zieleinlauf; Hicks gewann, aber sein Trainer hatte ihm illegal Morphin verabreicht",
        answerD = "Lorz lief rueckwaerts als Protest; Hicks trug verbotene Laufschuhe mit Metallspikes",
        correctAnswer = 0,
        explanation = "Fred Lorz wurde nach einer Siegesfeier und Medaillenzeremonie disqualifiziert, als herauskam, dass er 19 km im Auto mitgefahren war. Thomas Hicks gewann regulaer, aber seine Betreuer hatten ihm Strychnin (damals als Stimulans verwendet) und Weinbrand verabreicht. Er kollabierte nach dem Ziel, gewann aber offiziell das Rennen — Doping war 1904 noch nicht verboten.",
        difficulty = 5,
        funFact = "Der Marathon 1904 gilt als chaotischstes Rennen der Olympischen Geschichte. Die Strecke fuehrte ueber staubige Landstrassen bei 32 Grad Hitze. Nur 14 von 32 Laeuferagentschaft das Ziel. Ein kubanischer Laeufer, Felix Carvajal, machte unterwegs Pause und ass Aepfel von einem Obststand — trotzdem wurde er Vierter."
    ),

    // Question 14 – 1908 London: marathon distance
    Question(
        categoryId = 6,
        questionText = "Wie kam die heute standardisierte Marathon-Distanz von 42,195 Kilometern zustande, und welche Olympischen Spiele fixierten diese Laenge endgueltig?",
        answerA = "1908 in London: Die Strecke wurde von Windsor Castle (Startlinie am Kinderzimmer der koeniglichen Familie) bis zum Zielstrich im Olympic Stadium gemessen — 1924 in Paris als offizielle IAAF-Norm festgeschrieben",
        answerB = "1896 in Athen: Die Strecke von Marathon nach Athen wurde neu vermessen und ergab 42,195 km — sofort als Standard uebernommen",
        answerC = "1900 in Paris: Pierre de Coubertin legte die Distanz willkuerlich als 26 Meilen und 385 Yards fest",
        answerD = "1912 in Stockholm: Die Strecke wurde durch koeniglichen Erlass auf exakt 42 km plus 195 Meter festgelegt",
        correctAnswer = 0,
        explanation = "Beim Marathon 1908 in London sollte das Rennen vom Windsor Castle bis zum Olympiastadion fuehren. Die genaue Strecke — 26 Meilen und 385 Yards — ergab 42,195 km. Nach Jahren unterschiedlicher Distanzen bei verschiedenen Spielen standardisierte die IAAF 1921 diese Laenge rueckwirkend ab 1908 als offizielle Marathondistanz.",
        difficulty = 5,
        funFact = "Der dramatische Einsturz des Italieners Dorando Pietri kurz vor dem Ziel 1908 wurde weltberuehmt: Er wurde von Kampfrichtern aufgehoben und ins Ziel gebracht, aber sofort disqualifiziert. Queen Alexandra war so bewegt, dass sie ihm eine Sondertrophae ueberreichte. Der eigentliche Sieger John Hayes (USA) ist kaum bekannt."
    ),

    // Question 15 – 1916 Berlin: cancelled games
    Question(
        categoryId = 6,
        questionText = "Wie viele Olympische Spiele fielen wegen der beiden Weltkriege insgesamt aus, in welchen Jahren waren sie vergeben worden, und an welche Staedte?",
        answerA = "Drei Spiele: 1916 Berlin, 1940 Tokyo/Helsinki, 1944 London/Cortina d'Ampezzo",
        answerB = "Zwei Spiele: 1916 Berlin und 1940 Helsinki",
        answerC = "Vier Spiele: 1916 Berlin, 1940 Tokyo, 1940 Helsinki, 1944 London",
        answerD = "Zwei Spiele: 1940 Helsinki und 1944 London",
        correctAnswer = 0,
        explanation = "Drei Olympiaden fielen aus: 1916 (vergeben an Berlin, ausgefallen wegen WWI), 1940 (vergeben an Tokyo, nach Japans Rueckgabe an Helsinki, ausgefallen wegen WWII) und 1944 (vergeben an London fuer Sommer, Cortina d'Ampezzo fuer Winter, ausgefallen wegen WWII). Insgesamt gingen sechs olympische Veranstaltungen verloren (je Sommer und Winter).",
        difficulty = 5,
        funFact = "Japan hatte 1936 den Zuschlag fuer die Spiele 1940 erhalten. Nach Kriegsbeginn in China gab Japan 1938 die Spiele freiwillig zurueck. Das IOC vergab sie dann an Helsinki — aber auch diese Spiele fielen dem Winterkrieg 1939/40 zum Opfer. Helsinki bekam dann 1952 seinen lang ersehnten Olympia-Auftritt."
    ),

    // Question 16 – 1936 Berlin: Jesse Owens myth
    Question(
        categoryId = 6,
        questionText = "Welcher historische Mythos ueber die Berliner Spiele 1936 ist faktisch nachweislich falsch, und was geschah stattdessen wirklich?",
        answerA = "Mythos: Hitler schuettelte Jesse Owens absichtlich nicht die Hand, weil er ihn verachtete. Fakt: Hitler hatte alle Sieger am ersten Tag begruest, dann aufs IOC-Draengen hin aufgehoert — Owens wurde nicht begruest, aber auch kein anderer Sieger mehr",
        answerB = "Mythos: Jesse Owens gewann vier Goldmedaillen. Fakt: Er gewann nur drei, die vierte gehoerte dem Staffelteam",
        answerC = "Mythos: Die Spiele wurden im Rundfunk uebertragen. Fakt: Erst 1948 gab es erstmals Radio-Uebertragungen",
        answerD = "Mythos: Deutschland fuehrte die Medaillenwertung an. Fakt: Die USA gewannen die meisten Goldmedaillen",
        correctAnswer = 0,
        explanation = "Hitler begrueste am ersten Tag die deutschen und finnischen Goldmedaillengewinner persoenlich. Auf IOC-Draengen (es sei unpassend, manche zu empfangen und andere nicht) hoerte er auf. Owens sagte spaeter, Roosevelt habe ihn nie begruest oder gratuliert, obwohl Hitler ihm bei einer Begegnung zugewinkt habe. Der Mythos der gezielten Hitler-Verachtung ist historisch nicht korrekt.",
        difficulty = 5,
        funFact = "Jesse Owens befreundete sich in Berlin mit seinem deutschen Konkurrenten Luz Long, der ihm sogar Rat zum Weitsprung-Anlauf gab — was Owens den Einzug ins Finale sicherte. Long gewann Silber, umarmte Owens oeffentlich vor dem Berliner Publikum. Long starb 1943 als deutscher Soldat auf Sizilien; Owens hielt bis zu seinem Tod Kontakt zu Longs Sohn."
    ),

    // Question 17 – 1936 Berlin: Leni Riefenstahl
    Question(
        categoryId = 6,
        questionText = "Welches filmtechnische Novum fuer Sportdokumentationen fuerhrte Leni Riefenstahl in ihrem zweiteiligen Olympia-Film (1938) ein, das spaeter zum Standard wurde?",
        answerA = "Erstmals Kamerafahrten auf Schienen (Dolly-Shots) parallel zu Sprintern; Unterwasserkameras beim Hochspringen; Zeitlupenaufnahmen bei Diskuswurf und Stabhochsprung",
        answerB = "Erstmals Hubschrauber-Luftaufnahmen des Stadions von oben",
        answerC = "Erstmals Farbfilm-Aufnahmen bei Olympischen Spielen mit dem Agfacolor-Verfahren",
        answerD = "Erstmals Stereo-Tonaufnahmen der Zuschauermassen und Lautsprecheransagen",
        correctAnswer = 0,
        explanation = "Riefenstahl verwendete Kameragruben auf dem Sportfeld fuer Froschperspektive, Schienensysteme fuer Verfolgungsaufnahmen, Taucher mit Unterwasserkameras und bis dahin einmalige Zeitlupensequenzen. Sie verwendete uber 30 Kameras und entwickelte viele Einstellungen, die spaeter zum Standard der Sportfotografie wurden.",
        difficulty = 5,
        funFact = "Riefenstahls Olympia-Film wurde mit dem Internationalen Filmpreis der Weltausstellung 1937 ausgezeichnet, noch vor der eigentlichen Premiere. Die Frage, ob der Film Propagandawerk oder Kunstfilm ist, wird unter Filmwissenschaftlern bis heute kontrovers diskutiert. Riefenstahl bestritt bis zu ihrem Tod 2003 jede propagandistische Absicht."
    ),

    // Question 18 – 1948 London: Fanny Blankers-Koen
    Question(
        categoryId = 6,
        questionText = "Welchen Weltrekord hielt Fanny Blankers-Koen 1948 in London ausserdem, den sie aufgrund der Olympischen Reglements gar nicht antreten durfte, und wie reagierte die britische Presse auf ihre vier Goldmedaillen?",
        answerA = "Sie hielt den Weltrekord im Weitsprung und Hochsprung, durfte aber nur vier Einzeldisziplinen starten; Teile der britischen Presse kritisierten sie als 'zu alte Mutter' die zuhause bleiben sollte",
        answerB = "Sie hielt den Weltrekord im Marathon; die Presse feierte sie bedingungslos als groesste Sportlerin der Geschichte",
        answerC = "Sie hielt den Weltrekord im Kugel- und Speerwurf; die Presse ignorierte sie wegen ihrer Nationalitaet als Deutsche",
        answerD = "Sie hielt den Weltrekord im 400 m Lauf; die Presse bezeichnete ihre Siege als Betrug wegen angeblicher Dopingmittel",
        correctAnswer = 0,
        explanation = "Blankers-Koen hielt 1948 Weltrekorde in Weitsprung und Hochsprung, durfte aber Olympia-Reglements-bedingt nur in vier Disziplinen starten. Sie waehlte 100m, 200m, 80m Huerden und 4x100m — und gewann alle vier. Teile der britischen Presse spotteten: 'Eine Hausfrau aus Amsterdam' solle lieber zuhause bei ihren Kindern sein, statt zu laufen.",
        difficulty = 5,
        funFact = "Fanny Blankers-Koen war 30 Jahre alt und Mutter von zwei Kindern — in der Nachkriegszeit gaelt das als untypisch fuer eine Leistungssportlerin. Ihr Trainer und Ehemann Jan Blankers soll ihr vor den Spielen gesagt haben: 'Du bist schon zu alt, aber probiere es.' Sie gilt heute als 'schnellste Frau der Welt' ihrer Generation."
    ),

    // Question 19 – 1952 Helsinki: first Soviet participation
    Question(
        categoryId = 6,
        questionText = "Unter welcher aussergewoehnlichen Bedingung nahm die Sowjetunion erstmals 1952 in Helsinki an Olympischen Spielen teil, und welche diplomatische Besonderheit begleitete dies?",
        answerA = "Die UdSSR bestand auf einem separaten Olympischen Dorf ausschliesslich fuer den Ostblock; westliche und sowjetische Athleten wohnten in getrennten Lagern — ein einmaliges Phänomen der Olympia-Geschichte",
        answerB = "Die UdSSR nahm nur teil, wenn die Volksrepublik China ebenfalls zugelassen wurde",
        answerC = "Die UdSSR bestand darauf, als einziges Team unter dem Namen 'CCCP' statt 'Soviet Union' antreten zu duerfen",
        answerD = "Die UdSSR schickte nur Athleten aus der Roten Armee, keine Zivilisten, als Bedingung fuer die Teilnahme",
        correctAnswer = 0,
        explanation = "1952 bestand die UdSSR auf einem eigenen Olympischen Dorf fuer alle sozialistischen Staaten, getrennt vom westlichen Dorf. Ostblock-Athleten hatten minimalen Kontakt zu westlichen Teilnehmern. Trotzdem gab es beruehm-beruechtigte Szenen der Begegnung, etwa zwischen Emil Zatopek und sowjetischen Laeufernen. Diese Teilnahme war die erste nach 40-jaehrigem Ausschluss.",
        difficulty = 5,
        funFact = "Emil Zatopek gewann in Helsinki in acht Tagen drei Goldmedaillen: 5000m, 10000m und Marathon. Den Marathon bestritt er erstmals in seinem Leben — und siegte trotzdem. Er fragte den Brite Jim Peters, den Favoriten, ob das Tempo fuer einen Marathon stimme. Peters log ihn an und sagte 'Viel zu langsam' — Zatopek zog davon und gewann."
    ),

    // Question 20 – 1956 Melbourne: boycotts
    Question(
        categoryId = 6,
        questionText = "Welche drei separaten politischen Boykotte fanden bei den Olympischen Spielen 1956 in Melbourne statt, und was waren die jeweiligen Gruende?",
        answerA = "Aegypten/Irak/Libanon (Suez-Krise), Niederlande/Spanien/Schweiz (Ungarn-Niederschlagung), Volksrepublik China (Taiwan-Teilnahme als 'China')",
        answerB = "USA und Westdeutschland (DDR-Anerkennung), Israel (arabische Staaten-Druck), Suedafrika (Apartheid-Proteste)",
        answerC = "UdSSR (Berlin-Blockade-Nachwirkung), Japan (Kriegsverbrechertribunal-Protest), Indien (Kaschmir-Konflikt)",
        answerD = "Frankreich (Algerien-Krieg), Polen (Gomulka-Unruhen), Jugoslawien (Tito-Stalin-Bruch)",
        correctAnswer = 0,
        explanation = "1956 gab es in Melbourne gleich drei Boykottwellen: Aegypten, Irak und Libanon verliessen die Spiele wegen der Suez-Krise (britisch-franzoesische Invasion). Niederlande, Spanien und Schweiz protestierten gegen den sowjetischen Einmarsch in Ungarn. Die Volksrepublik China verliess die Spiele, weil Taiwan unter dem Namen 'China' teilnahm.",
        difficulty = 5,
        funFact = "Die Reiterlichen Wettbewerbe 1956 fanden wegen australischer Quarantaenevorschriften in Stockholm statt — als einziger Fall in der Olympia-Geschichte, dass verschiedene Sportarten einer Olympiade in verschiedenen Laendern ausgetragen wurden."
    ),

    // Question 21 – 1960 Rome: Cassius Clay
    Question(
        categoryId = 6,
        questionText = "Welche bekannte Legende erzaehlte Muhammad Ali (damals Cassius Clay) ueber seine Goldmedaille von 1960 in Rom, und was ist historisch daran belegt bzw. zweifelhaft?",
        answerA = "Ali behauptete, er habe seine Goldmedaille in den Ohio-Fluss geworfen, nachdem ihm in Louisville ein Restaurant wegen seiner Hautfarbe den Eintritt verweigert habe — historisch belegt ist die Diskriminierung in Louisville, der Medaillenwurf ist nur durch Alis eigene Aussagen bezeugt",
        answerB = "Ali behauptete, er habe den Sowjet-Boxer im Finale k.o. geschlagen, obwohl offiziell Punktsieg ausgewiesen wurde",
        answerC = "Ali behauptete, er habe Angst vor dem Flug und sei mit dem Schiff nach Rom gereist",
        answerD = "Ali behauptete, er habe die Medaille einer afrikanischen Nation gespendet als Zeichen der Solidaritaet",
        correctAnswer = 0,
        explanation = "Ali beschrieb in seiner Autobiografie von 1975, wie er seine Goldmedaille in den Ohio-Fluss warf nach einer Diskriminierungserfahrung in Louisville. Diese Geschichte ist nur durch Alis eigene Aussagen belegt. Historiker bezweifeln sie, da keine Zeugen oder zeitgenoessische Berichte existieren. Die Diskriminierung in Louisville ist hingegen historisch dokumentiert.",
        difficulty = 5,
        funFact = "Bei den Spielen 1996 in Atlanta erhielt Ali eine Ersatz-Goldmedaille in einer bewegenden Zeremonie — unabhaengig davon, ob seine Geschichte der weggeworfenen Medaille stimmt. Er entzuendete das Olympische Feuer trotz seiner durch Parkinson fortgeschrittenen Erkrankung — einer der beeindruckendsten Momente der Olympia-Geschichte."
    ),

    // Question 22 – 1960 Rome: first televised Olympics
    Question(
        categoryId = 6,
        questionText = "Die Olympischen Spiele 1960 in Rom waren historisch bedeutsam fuer die Medienentwicklung — welcher Meilenstein wurde hier erstmals erreicht, und welche Disziplin wurde zum ikonischen TV-Ereignis?",
        answerA = "Erstmals wurden die Spiele weltweit im Fernsehen live uebertragen; der Marathon — nachts durch die erleuchteten Strassen Roms — wurde mit dem barfuss laufenden Absebe Bikila zum globalen TV-Erlebnis",
        answerB = "Erstmals wurden Farbfernseh-Uebertragungen eingesetzt; der 100m-Sprint mit Armin Hary war das meistgesehene Einzelereignis",
        answerC = "Erstmals wurden Satelliten zur Bilduebertragung genutzt; die Schlussfeier war das erste globale Satellitenereignis",
        answerD = "Erstmals wurden Wiederholungen (Slow-Motion-Replays) eingesetzt; der Zehnkampf mit Rafer Johnson war das meistwiederholte Ereignis",
        correctAnswer = 0,
        explanation = "1960 in Rom wurden zum ersten Mal Olympische Spiele weltweit im Fernsehen uebertragen — 18 europaeische Laender empfingen die Bilder live. Der Marathon wurde bei Nacht entlang der Via Appia gefuehrt, beleuchtet von Fackeln; Absebe Bikila aus Aethiopien lief barfuss und gewann. Diese Bilder gingen um die Welt und schufen das moderne Olympia-Medienereignis.",
        difficulty = 5,
        funFact = "Absebe Bikila trainierte barfuss in Aethiopien, bekam aber kurz vor Olympia Adidas-Schuhe. Diese passten nicht perfekt und er entschied sich, lieber barfuss zu laufen. Er stellte als erster Afrikaner einen olympischen Weltrekord auf: 2:15:16 Stunden. Vier Jahre spaeter gewann er in Tokyo mit Schuhen — und war noch schneller."
    ),

    // Question 23 – 1896 Athens: German gymnastics refusal
    Question(
        categoryId = 6,
        questionText = "Welche bedeutende Turnsportorganisation weigerte sich, an den ersten modernen Olympischen Spielen 1896 in Athen teilzunehmen, und warum — und welche ideologische Fehde lag dahinter?",
        answerA = "Die Deutsche Turnerschaft unter dem Einfluss der Jahn'schen Turn-Ideologie lehnte ab, weil sie internationale Wettkaeampfe als Verrat am nationalen Turnen betrachteten und Coubertin als Rivalen sahen",
        answerB = "Die Britische Athletic Association lehnte ab, weil Amateurregeln verletzt wuerden und Preisgeld ausgezahlt werden sollte",
        answerC = "Die Amerikanische AAU lehnte ab, weil Griechenland keine Sicherheitsgarantien fuer die Athleten geben konnte",
        answerD = "Der Schwedische Olympische Verband lehnte ab, weil die Spiele den Turnkampf-Stil der Ling-Methode ignorierten",
        correctAnswer = 0,
        explanation = "Die Deutsche Turnerschaft unter Carl Euler und mit dem Erbe Friedrich Ludwig Jahns sah im olympischen Wettkampfsport einen Angriff auf die nationale Turnbewegung. Turner und Athleten (Leichtathleten) galten in Deutschland als verfeindete Bewegungen. Die Deutsche Turnerschaft boykottierte Olympia jahrzehntelang — erst ab 1920 nahm Deutschland regulaer teil.",
        difficulty = 5,
        funFact = "Friedrich Ludwig Jahn, der 'Turnvater', hatte 1811 den ersten oeffentlichen Turnplatz auf der Berliner Hasenheide eroeffnet — als nationales Ertueuchtigungsprojekt gegen Napoleon. Seine Ideologie des deutschen Nationalturns stand in direktem Konflikt mit Coubertins internationalem Wettkampfgedanken. Dieser Kulturkampf zog sich bis in die Weimarer Republik."
    ),

    // Question 24 – Olympic history: the double-decker medals
    Question(
        categoryId = 6,
        questionText = "Warum wurden bei den Olympischen Spielen 1900 in Paris und 1904 in St. Louis keine klassischen Medaillen vergeben, und was erhielten die Sieger stattdessen?",
        answerA = "1900 erhielten Sieger Kunstobjekte und Pokale; 1904 gab es Medaillen, aber keine Goldmedaillen — Sieger erhielten Silber, Zweite Bronze, Dritte keine Auszeichnung",
        answerB = "Beide Male wurden nur Urkunden ausgegeben, weil das IOC kein Budget fuer Medaillen hatte",
        answerC = "1900 und 1904 gab es keine offiziellen Auszeichnungen — Sieger erhielten Preisgeld statt Medaillen",
        answerD = "Beide Spiele vergaben Medaillen, aber mit nationalen statt olympischen Symbolen, weil das IOC keine Gestaltungsrechte besass",
        correctAnswer = 0,
        explanation = "Die Spiele 1900 waren Teil der Pariser Weltausstellung; Sieger erhielten Kunstgegenstände, Bücher und Pokale — kaum jemand wusste, dass er an Olympia teilnahm. 1904 in St. Louis gab es erstmals Medaillen, aber in der damaligen Hierarchie: Sieger bekamen Silber, Zweite Bronze. Die heute bekannte Gold-Silber-Bronze-Hierarchie wurde 1904 eingeführt, aber Gold war noch Silber vergoldet.",
        difficulty = 5,
        funFact = "Die goldene Goldmedaille ist seit 1912 Pflicht — seitdem muss sie mindestens 6 Gramm echtes Gold enthalten. In der Realitaet sind Goldmedaillen meist aus Silber mit Goldauflage. Die letzte vollmassive Goldmedaille wurde 1912 in Stockholm vergeben; seitdem war reines Gold zu teuer."
    ),

    // Question 25 – 1936 Berlin: torch relay invention
    Question(
        categoryId = 6,
        questionText = "Wer erfand den olympischen Fackellauf von Olympia zur Austragungsstaette, der heute als uralte Tradition gilt, und was war seine ideologische Motivation?",
        answerA = "Carl Diem, Organisationschef der Berliner Spiele 1936, erfand ihn als PR-Spektakel — er nutzte antike Elemente, verband sie aber mit nationalsozialistischer Inszenierung und schuf damit eine 'Tradition', die keine war",
        answerB = "Pierre de Coubertin erfand den Fackellauf 1896 fuer die Athener Spiele als Symbol der Kontinuitaet zur Antike",
        answerC = "Das IOC beschloss 1920 in Antwerpen den Fackellauf als Symbol der Wiedergeburt nach dem Weltkrieg",
        answerD = "Leni Riefenstahl schlug den Fackellauf 1934 als filmisches Element fuer ihren Olympia-Film vor",
        correctAnswer = 0,
        explanation = "Carl Diem erfand den modernen olympischen Fackellauf speziell fuer die Berliner Spiele 1936. Er nutzte reale antike Elemente (Hestia-Feuer in Olympia) und verband sie mit einer Inszenierung, die Griechenland und das 'Dritte Reich' als kulturelle Kontinuitaet darstellen sollte. Diese 'Tradition' wurde nach 1945 unhinterfragt weitergeführt.",
        difficulty = 5,
        funFact = "Die erste Fackelstrecke 1936 fuehrte ueber 3187 km durch Griechenland, Bulgarien, Jugoslawien, Ungarn, Oesterreich, die Tschechoslowakei und Deutschland — 3331 Laeufer trugen die Fackel in elf Tagen. In Bulgarien kam es zu Demonstrationen gegen das NS-Regime entlang der Strecke."
    ),

    // ── Sportpolitik: Boykotte und Skandale (10) ──────────────────────────────

    // Question 26 – 1980 Moscow boycott: background
    Question(
        categoryId = 6,
        questionText = "Wie viele Nationen boykottierten die Olympischen Sommerspiele 1980 in Moskau, und welche international ungewoehnliche Konstellation ergab sich bei der Eroefffnungsfeier?",
        answerA = "65 Nationen boykottierten; 15 Laender marschierten unter der Olympischen Flagge statt unter ihrer Nationalflagge ein, weil ihre NOKs die Spiele trotz Regierungsboykott unterstuetzten",
        answerB = "80 Nationen boykottierten; alle verbleibenden Laender marschierten ausnahmsweise in alphabetischer Reihenfolge nach Englisch statt nach Russisch ein",
        answerC = "45 Nationen boykottierten; die USA versuchten, eine Gegenolympiade in Marokko zu organisieren",
        answerD = "55 Nationen boykottierten; das IOC drohte, Moskau die Gastgeberrechte zu entziehen, scheiterte aber am Veto der UdSSR",
        correctAnswer = 0,
        explanation = "65 Nationen boykottierten 1980 Moskau wegen des sowjetischen Einmarschs in Afghanistan 1979. 15 Nationen (darunter Grossbritannien, Australien, Frankreich) ignorierten den Regierungsboykott und liessen ihre Athleten unter der Olympischen Flagge antreten — eine historische Besonderheit. Die USA, Westdeutschland und Japan fehlten vollstaendig.",
        difficulty = 5,
        funFact = "Die Schlussfeier 1980 in Moskau enthielt ein emotionales Element: Der Olympia-Baer 'Mischa' — das Maskottchen — wurde als riesiger Heliumballon in den Himmel entlassen. Millionen Sowjetbuerger weinten vor den Fernsehern. Der Ballon landete spaeter irgendwo in der Sowjetunion und wurde von einem Bauern gefunden."
    ),

    // Question 27 – 1984 Los Angeles: Eastern bloc boycott
    Question(
        categoryId = 6,
        questionText = "Mit welcher offiziellen Begruendung rechtfertigten die UdSSR und der Ostblock ihren Boykott der Olympischen Spiele 1984 in Los Angeles, und welche parallele Veranstaltung organisierten sie als Ersatz?",
        answerA = "Offizielle Begruendung: fehlende Sicherheitsgarantien fuer sowjetische Athleten und anti-sowjetische Stimmung; Ersatzveranstaltung: Freundschaftsspiele (Druzhba-84) in 9 sozialistischen Laendern mit ca. 2000 Athleten",
        answerB = "Offizielle Begruendung: IOC-Regelverstoesse durch US-Veranstalter; Ersatz: CISM-Weltspiele der Militaersportler in Moskau",
        answerC = "Offizielle Begruendung: Anerkennung Taiwans als 'China' durch die USA; kein offizieller Ersatz organisiert",
        answerD = "Offizielle Begruendung: Kommerzialisierung der Spiele durch private Sponsoren (Peter Ueberroth); Ersatz: Sozialistischer Weltmeisterschaftszyklus",
        correctAnswer = 0,
        explanation = "Die UdSSR begruendete den Boykott mit Sicherheitsbedenken und anti-sowjetischer Propaganda, was allgemein als Vergeltung fuer den US-Boykott 1980 gesehen wurde. Die Druzhba-84 (Freundschaftsspiele) fanden in 9 sozialistischen Staaten statt. Bei den inoffiziellen Vergleichen zeigten Analysen: Der Ostblock haette in LA deutlich mehr Medaillen gewonnen als der Westen.",
        difficulty = 5,
        funFact = "Die Spiele 1984 in LA waren die ersten, die ohne staatliche Zuschüsse auskamen — Organisator Peter Ueberroth erzielte einen Gewinn von 225 Millionen Dollar durch private Sponsorenvertraege. Dieses Modell revolutionierte die olympische Finanzierung und machte Olympia zur globalen Marke."
    ),

    // Question 28 – Doping history: East German state doping
    Question(
        categoryId = 6,
        questionText = "Unter welchem internen Codenamen lief das staatliche Dopingprogramm der DDR, welches Ministerium koordinierte es, und seit welchem Jahr ist die Existenz durch Stasi-Akten lueckenlos belegt?",
        answerA = "Codename 'Staatsplan 14.25', koordiniert durch Ministerium fuer Staatssicherheit (MfS) und DTSB; durch Stasi-Akten lueckenlos belegt seit 1974 (Beginn des flaechen-deckenden Oral-Turinabol-Einsatzes)",
        answerB = "Codename 'Operation Goldmedaille', koordiniert durch das DDR-Sportministerium; belegt seit 1968",
        answerC = "Codename 'Projekt Olymp', koordiniert durch die Humboldt-Universitaet Berlin; belegt seit 1976",
        answerD = "Codename 'Plan K', koordiniert durch das Zentralkomitee der SED; belegt erst seit dem Mauerfall 1989",
        correctAnswer = 0,
        explanation = "Der 'Staatsplan 14.25' war das zentrale DDR-Dopingprogramm, das ab 1974 Oral-Turinabol (ein synthetisches Androgen) flaechendeckend einsetzte. Die Stasi koordinierte Geheimhaltung, aerztliche Betreuung und internationale Abschirmung. Heute sind ueber 3000 Athleten als Opfer des Staatsdopings anerkannt; viele leiden bis heute an Spaetfolgen.",
        difficulty = 5,
        funFact = "Manfred Ewald, Chef des DTSB (Deutscher Turn- und Sportbund der DDR) von 1961 bis 1988, wurde 2000 zu einer Bewaehrungsstrafe verurteilt — einer der seltenen strafrechtlichen Konsequenzen fuer staatlich organisierten Sportbetrug. Aerzte wurden milder bestraft als erwartet, da Bewaeis-lage fuer Vorsatz schwierig war."
    ),

    // Question 29 – Apartheid and sport
    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurde Suedafrika erstmals von den Olympischen Spielen ausgeschlossen, und welche Organisation spielte die entscheidende Rolle bei diesem Beschluss — trotz anfaenglichem IOC-Widerstand?",
        answerA = "1964 Tokyo — erster Ausschluss; 1970 endgueltige Verbannung durch SANOC-Druck; Supreme Council for Sport in Africa (SCSA) drohte mit Massenboykott afrikanischer Nationen",
        answerB = "1960 Rome — direkt nach dem Sharpeville-Massaker; das IOC handelte auf Druck der UN",
        answerC = "1968 Mexiko-Stadt — nach dem Attentat auf Martin Luther King; die USA uebten Druck auf das IOC aus",
        answerD = "1972 Muenchen — nach dem Attentat auf israelische Athleten; Suedafrikas Apartheid wurde mit dem Terrorismus gleichgesetzt",
        correctAnswer = 0,
        explanation = "Suedafrika wurde 1964 in Tokyo suspendiert (nicht offiziell ausgeschlossen) und 1970 endgueltig aus dem IOC ausgeschlossen. Den entscheidenden Druck uebte der Supreme Council for Sport in Africa (SCSA) aus, der mit dem Boykott der Muenchner Spiele drohte. Ohne Afrika-Boykott haette das IOC-Praesident Avery Brundage Suedafrika gerne weiter toleriert.",
        difficulty = 5,
        funFact = "Suedafrika kehrte erst nach Ende der Apartheid 1992 in Barcelona zu Olympia zurueck. Bei der Eroefffnungsfeier trugen weisse und schwarze suedafrikanische Athleten gemeinsam die Fahne — ein Symbol, das weltweit als historischer Moment gefeiert wurde. Suedafrika hatte damit 32 Jahre gefehlt."
    ),

    // Question 30 – 1972 Munich massacre
    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Bundesinnenminister trug die Hauptverantwortung fuer die katastrophale Sicherheitsstrategie bei der Geiselnahme in Muenchen 1972, und welche Entscheidung wurde spaeter als toedlicher Fehler bewertet?",
        answerA = "Hans-Dietrich Genscher (FDP) als Innenminister; die falsche Lagebeurteilung am Flughafen Fuerstenfeldbruck — nur 5 Scharfschuetzen fuer 8 Terroristen, kein ausgebildetes Antiterrorkommando vorhanden",
        answerB = "Werner Maihofer (FDP) als Innenminister; die Entscheidung, keine israelischen Sicherheitskraefte anzufordern",
        answerC = "Gerhard Jahn (SPD) als Justizminister; die falsche Verhandlungsstrategie ohne psychologische Experten",
        answerD = "Karl Carstens (CDU) als Oppositionsfuehrer; er blockierte Geheimdienstinformationen aus Israel",
        correctAnswer = 0,
        explanation = "Hans-Dietrich Genscher (Innenminister 1969-1974) koordinierte die Krisenbewaltigung. Am Flughafen Fuerstenfeldbruck versagten die improvisierten Sicherheitsmassnahmen: nur 5 Scharfschuetzen ohne Nachtsichtgeraete, kein koordinierter Angriffplan, ein Hubschrauberpilot weigerte sich. Das eigens entwickelte Antiterrorkommando GSG 9 entstand erst 1973 als direkte Konsequenz.",
        difficulty = 5,
        funFact = "IOC-Praesident Avery Brundage bestand darauf, die Spiele fortzusetzen — sein beruehm-beruechtigtes Statement 'The Games must go on' wurde weltberühmt und ist bis heute umstritten. 34 Stunden nach dem Ende der Geiselnahme, bei der alle 11 israelischen Geiseln gestorben waren, liefen die Wettkaeampfe weiter."
    ),

    // ── Sport und Gesellschaft (5) ────────────────────────────────────────────

    // Question 31 – Weimar Republic sport
    Question(
        categoryId = 6,
        questionText = "Welche Besonderheit hatte der Deutsche Hochschul-Sport-Verband (DHfB) in der Weimarer Republik hinsichtlich seiner politischen Verortung, und welche Sportorganisation stand ihm als Gegenpol gegenueber?",
        answerA = "Der DHfB war hochgradig nationalistisch und antisemitisch ausgerichtet; ihm stand die Arbeiter-Turn- und Sportbewegung (ATSB) als sozialdemokratische Massenorganisation gegenueber, die mit ueber 1,2 Millionen Mitgliedern groesser war als das IOC-affiliierte deutsche Sportlager",
        answerB = "Der DHfB war kommunistisch gepraegt; ihm stand der Reichsbund fuer Leibesuebungen als buergerlicher Sportverband gegenueber",
        answerC = "Der DHfB war parteipolitisch neutral; ihm stand der Juedische Sportverband Makkabi als wichtigste Minderheitenorganisation gegenueber",
        answerD = "Der DHfB war katholisch-konfessionell; ihm stand der evangelische CVJM-Sportverband als protestantisches Gegengewicht gegenueber",
        correctAnswer = 0,
        explanation = "Die Weimarer Sportlandschaft war tief gespalten: Das buergerlich-nationalistische Lager (DRL, DHfB) stand der sozialdemokratischen ATSB gegenueber, die 1928 ueber 1,2 Millionen Mitglieder hatte. Dazu kamen juedische Sportverbände (Makkabi, Bar Kochba), konfessionelle Verbände und der kommunistische Kampfbund. 1933 wurden alle nicht-nationalsozialistischen Verbände verboten oder aufgelost.",
        difficulty = 5,
        funFact = "Die ATSB organisierte eigene 'Arbeiterolympiaden' als Gegenprogramm zu den buergerlichen Olympischen Spielen: 1925 in Frankfurt (150.000 Zuschauer), 1931 in Wien (100.000 Teilnehmer). Diese sozialistischen Sportfeste sollten proletarische Solidaritaet statt nationalen Wettbewerb verkörpern."
    ),

    // Question 32 – Sport in the NS era
    Question(
        categoryId = 6,
        questionText = "Wie behandelte das NS-Regime juedische Sportvereine und -verbände zwischen 1933 und 1936, und welche Doppelstrategie verfolgte es hierbei im Hinblick auf die Olympischen Spiele 1936?",
        answerA = "Nach anfaenglichem Vereinsverbot wurde eine Schein-Integration betrieben: Juedische Athleten durften in eigenen Verbänden trainieren und theoretisch fuer Olympia-Trials antreten — als Beweis angeblicher Fairness. Tatsaechlich wurde kein Jude in die Olympiamannschaft aufgenommen (Ausnahme: Halbjuede Bergmann im Hochsprung, dann kurzfristig ausgeschlossen)",
        answerB = "Alle juedischen Vereine wurden sofort 1933 verboten und Mitglieder verhaftet; das Regime bemuehte sich nicht um internationale Tarnung",
        answerC = "Juedische Athleten durften in der gleichen Mannschaft wie arische Athleten starten, wurden aber bei Erfolg nicht geehrt",
        answerD = "Das Regime zwang juedische Sportler, unter falschen Namen anzutreten, um Protest im Ausland zu vermeiden",
        correctAnswer = 0,
        explanation = "Das NS-Regime verfolgte eine bewusste Doppelstrategie: Oeffentlich wurden juedische Sportler aus deutschen Vereinen ausgeschlossen, durften aber in separaten juedischen Verbänden aktiv sein. Gegenueber dem Ausland (besonders den USA, die einen Boykott diskutierten) praesentierten die Nazis dies als Beweis von Fairness. Gretel Bergmann war Deutschlands beste Hochspringerin 1936, wurde aber kurz vor Olympia ausgeschlossen.",
        difficulty = 5,
        funFact = "Gretel Bergmann emigrierte in die USA, wo sie unter dem Namen Margaret Lambert Karriere machte. 2009, mit 95 Jahren, erhielt sie die Goldmedaille der Deutschen Sporthochschule Koeln — 73 Jahre nach dem Olympia, von dem sie ausgeschlossen worden war. Sie starb 2017 mit 103 Jahren in New York."
    ),

    // Question 33 – Black Power salute 1968
    Question(
        categoryId = 6,
        questionText = "Was geschah mit Tommie Smith und John Carlos nach ihrer Black-Power-Geste bei den Olympischen Spielen 1968 in Mexiko-Stadt, und welche symbolischen Details ihrer Protestpose werden oft uebersehen?",
        answerA = "Beide wurden sofort aus dem olympischen Dorf ausgewiesen und disqualifiziert; Smith trug den rechten Handschuh (schwarze Macht), Carlos den linken (schwarze Einheit); beide trugen Perlen als Symbol fuer Lynchopfer und geoefffnete Jacken als Zeichen der Solidaritaet mit blauen US-Arbeitern",
        answerB = "Beide wurden lebenslang von Olympia gesperrt; ihre Medaillen wurden aberkannt und eingeschmolzen",
        answerC = "Nur Smith wurde ausgewiesen; Carlos durfte bleiben, weil er nur den Kopf senkend protestierte",
        answerD = "Beide wurden erst drei Jahre spaeter bestraft: Berufsverbot als Trainer im US-Sportsystem",
        correctAnswer = 0,
        explanation = "Smith und Carlos wurden vom IOC auf Druck Avery Brundages sofort aus dem Olympischen Dorf verwiesen. Die Symbolik war komplex: Smith der rechte schwarze Handschuh, Carlos der linke (weil Smith seinen vergessen hatte, teilten sie ein Paar). Smith trug eine schwarze Halskette fuer Lynchopfer; Carlos Perlen fuer dieselben. Ihre offenen Jacken symbolisierten blaue Arbeitersolidaritaet.",
        difficulty = 5,
        funFact = "Der australische Silbermedaillengewinner Peter Norman trug ebenfalls das OPHR-Abzeichen (Olympic Project for Human Rights) auf seiner Brust — in Solidaritaet mit Smith und Carlos. Er wurde dafuer von der australischen Olympia-Bewegung jahrelang gemieden. Bei seiner Beerdigung 2006 trugen Smith und Carlos den Sarg."
    ),

    // Question 34 – Amateurism and professionalism
    Question(
        categoryId = 6,
        questionText = "Welche beruehmte Olympia-Disqualifikation wegen Verletzung des Amateurstatus ereignete sich 1912, und welche historische Ironie ergab sich Jahrzehnte spaeter?",
        answerA = "Jim Thorpe (USA) wurde 1913 disqualifiziert und musste seine zwei Goldmedaillen (Fuenfkampf, Zehnkampf) zurueckgeben, weil er 1909/10 Baseball gegen geringes Entgelt gespielt hatte; 70 Jahre spaeter, 1982, wurden ihm posthum die Medaillen zuerkannt",
        answerB = "Paavo Nurmi (Finnland) wurde 1932 wegen Vergabe von Autogrammen gegen Bezahlung disqualifiziert; er wurde 1970 rehabilitiert",
        answerC = "Harold Abrahams (Grossbritannien) wurde 1924 disqualifiziert, weil er einen professionellen Trainer hatte; das Urteil wurde 1952 aufgehoben",
        answerD = "Hannes Kolehmainen (Finnland) verlor seinen Goldmedaillen-Status 1912, weil er Werbevertrag mit Sportschuhhersteller hatte",
        correctAnswer = 0,
        explanation = "Jim Thorpe gewann 1912 in Stockholm beide multidisziplinaeren Wettbewaerbe ueberlegen. Wegen 25-Dollar-Baseball-Gagen 1909/10 wurde er 1913 disqualifiziert. Nach jahrzehntelangen Kampagnen erkannte das IOC 1982, 29 Jahre nach Thorpes Tod, seine Medaillen offiziell an. Er gilt als vielseitigster Athlet der Olympia-Geschichte.",
        difficulty = 5,
        funFact = "Konig Gustav V. von Schweden soll zu Jim Thorpe gesagt haben: 'Sie sind der groesste Athlet der Welt.' Thorpes Antwort: 'Thanks, King.' Diese Anekdote ist wahrscheinlich apokyph, aber sie zeigt, welchen Eindruck Thorpe hinterliess. Er war auch NFL-Pionier und erster Praesident der American Professional Football Association."
    ),

    // Question 35 – Sport and Cold War ideology
    Question(
        categoryId = 6,
        questionText = "Welche Idee lag dem sowjetischen Sport-Konzept des 'GTO' (Gotov k trudu i oborone — Bereit fuer Arbeit und Verteidigung) zugrunde, und wann wurde es eingefuehrt?",
        answerA = "GTO war ein staatliches Fitness-Zertifizierungssystem, eingefuehrt 1931; Buerger mussten Mindestleistungen in Lauf, Schwimmen, Schiessen und Turnen nachweisen, um Medaillen und gesellschaftliche Vorteile zu erhalten — es verband Massenbreitensport mit militaerischer Vorvorbereitung",
        answerB = "GTO war das sowjetische Dopingprogramm, eingefuehrt 1952 nach den ersten Olympia-Teilnahmen",
        answerC = "GTO war der sowjetische Geheimdienst-Sportverband, der Athleten fuer Auslandsspionage rekrutierte",
        answerD = "GTO war ein marxistisches Sportphilosophie-System, das 1917 nach der Revolution eingefuehrt wurde",
        correctAnswer = 0,
        explanation = "GTO wurde 1931 unter Stalin eingefuehrt als Massenbreitensport-Programm mit klar militaerischer Ausrichtung. Millionen Sowjetbuerger absolvierten jaehrlich die Tests und erhielten Abzeichen. Das Programm pragte die sowjetische Sportkultur bis 1991 und wurde 2014 unter Putin wiederbelebt — als Symbol nationaler Staerke.",
        difficulty = 5,
        funFact = "Das GTO-System hatte massive Auswirkungen auf den sowjetischen Olympia-Erfolg: Es identifizierte Talente fruehzeitig in der Bevoelkerungsmasse und leitete sie in Sportschulen weiter. Die Sowjetunion dominierte ab 1952 die Olympia-Medaillenspiele — ein direktes Ergebnis des systematischen Breitenport-Talentscreenings."
    ),

    // ── Historische Regelaenderungen (5) ──────────────────────────────────────

    // Question 36 – Rules change: forward pass in American football
    Question(
        categoryId = 6,
        questionText = "Welcher politische Druck fuehrte 1906 zur Legalisierung des Vorwaertspasses im American Football, und welcher US-Praesident war daran direkt beteiligt?",
        answerA = "Theodore Roosevelt drohte 1905 mit einem Bundesverbot des Footballs, nachdem in einer Saison 18 Spieler gestorben und 150 schwer verletzt worden waren — die Legalisierung des Vorwaertspasses 1906 sollte das Spiel sicherer und schneller machen",
        answerB = "William Howard Taft unterstuetzte 1910 die Regelaenderung, weil Football an Militaerakademien verbreitet wurde und der Vorwaertspass taktisch an Kriegsfuehrung erinnerte",
        answerC = "Woodrow Wilson legte 1913 einen Gesetzentwurf vor, der Universitaeten bei Footballverletzungen haftbar machte — als Reaktion darauf aenderten Ligen freiwillig die Regeln",
        answerD = "Abraham Lincoln hatte bereits 1863 Football-Varianten verboten; die Rehabilitierung 1906 war ein Symbol der nationalen Einheit nach dem Buergerkrieg",
        correctAnswer = 0,
        explanation = "Theodore Roosevelt lud 1905 Vertreter grosser Universitaeten ins Weisse Haus und drohte mit Verbot. In der Saison 1905 starben 18 Collegespieler direkt durch Spielgewalt. Die Intercollegiate Athletic Association (Vorlaeuferin der NCAA) beschloss 1906 den Vorwaertspass, der das Spiel oeffffnete und Masse-gegen-Masse-Stuerm (sogenannte 'mass plays') reduzierte.",
        difficulty = 5,
        funFact = "Der erste dokumentierte Vorwaertspass in einem Pflichtspiel wurde am 5. September 1906 von Bradbury Robinson zu Jack Scheider geworfen — Wesleyan University gegen Saint Louis University. Das Spiel endete 22:0. Der Vorwaertspass revolutionierte Football und machte ihn zur spektakulaersten Mannschaftssportart der USA."
    ),

    // Question 37 – Rules change: offside in football
    Question(
        categoryId = 6,
        questionText = "Welche gravierende Aenderung der Abseitsregel im Association Football (Fussball) wurde 1925 eingefuehrt, und wie dramatisch aenderte sich dadurch die durchschnittliche Toranzahl pro Spiel?",
        answerA = "Die Dreispieler-Regel wurde durch eine Zweispieler-Regel ersetzt (statt drei muessen nur noch zwei Gegner zwischen Angreifer und Tor stehen); die Torquote stieg innerhalb einer Saison von durchschnittlich 2,6 auf 3,7 Tore pro Spiel",
        answerB = "Abseits wurde abgeschaafft und erst 1938 in modifizierter Form wieder eingefuehrt",
        answerC = "Die Abseitsregel wurde auf die gegnerische Haelfte beschraenkt; in der eigenen Haelfte konnte man nicht mehr abseit stehen",
        answerD = "Torhuetern wurde verboten, Abseitsfallen zu stellen; die Verteidigung musste statisch bleiben",
        correctAnswer = 0,
        explanation = "Bis 1925 mussten drei gegnerische Spieler zwischen dem Angreifer und dem Tor stehen. Herbert Chapman (Arsenal) hatte die alte Regel mit einer tief stehenden Abwehrreihe ausgenutzt. Nach der Regelaenderung auf zwei Gegner stiegen die Tore in England von 4594 auf 6373 in einer Saison. Chapman erfand dann den WM-Systemm als Reaktion.",
        difficulty = 5,
        funFact = "Herbert Chapman von Arsenal entwickelte nach der Regelaenderung das revolutionaere WM-System (3-4-3 Variante mit zurueckgezogenem Mittelstuermer). Dieses taktische System dominierte den Weltfussball bis in die 1950er Jahre und beeinflusste alle spaetere Taktik-Entwicklung. Chapman gilt als Erfinder des modernen Fussballmanagements."
    ),

    // Question 38 – Rules change: straddle to Fosbury flop
    Question(
        categoryId = 6,
        questionText = "Warum konnte Dick Fosburys revolutionaere Rueckwaerts-Hochsprungtechnik (Fosbury-Flop) erst nach der Einfuehrung von Schaumstoffmatten an Bedeutung gewinnen, und wann verdraengte sie endgueltig alle anderen Techniken?",
        answerA = "Vor Schaumstoffmatten (eingesetzt ab ca. 1960er) waren Saeggemehl- oder Sandgruben Standard — eine Landung auf dem Ruecken waere gefaehrlich gewesen. Fosbury gewann 1968 Olympia; bis Ende der 1970er nutzten fast alle Hochspringer weltweit den Flop",
        answerB = "Fosbury erfand die Technik mit Schaumstoffmatten — er trainierte als Erster damit ab 1962 und patentierte die Methode",
        answerC = "Die IAAF verbot den Fosbury-Flop zunaechst als gefaehrlich; erst nach einem Gerichtsurteil 1972 wurde er offiziell erlaubt",
        answerD = "Der Fosbury-Flop konnte sich erst durchsetzen, als Hochsprunganlagen standardisiert wurden — vorher variierten Mattengrössen zu stark",
        correctAnswer = 0,
        explanation = "Traditionelle Hochsprungtechniken (Scherensprung, Westerroll, Straddle) endeten mit dem Koerper nach vorne — sicher fuer Sagemehl. Der Fosbury-Flop mit Rueckenlandung erforderte Schaumstoffmatten. Fosbury gewann 1968 in Mexiko-Stadt mit 2,24 m Weltrekord. Bis 1980 verwendeten fast alle Weltklasse-Hochspringer den Flop.",
        difficulty = 5,
        funFact = "Dick Fosbury erfand den Flop zunaechst ohne Coaching — er experimentierte als Teenager im Training und entwickelte die Technik intuitiv. Sein Trainer versuchte ihn anfangs davon abzubringen. Heute trainieren Kinder auf der ganzen Welt ausschliesslich den Flop — die alten Techniken sind praktisch ausgestorben."
    ),

    // Question 39 – Rules change: two-point conversion
    Question(
        categoryId = 6,
        questionText = "Wann und warum fuehrte die NFL die Zwei-Punkte-Conversion ein, und welche andere Football-Liga hatte diese Regel Jahrzehnte frueher entwickelt?",
        answerA = "NFL 1994 — eingeführt um das Spiel spannender zu machen und auf Kritik an zu vielen Unentschieden zu reagieren; die AFL (American Football League) hatte sie bereits seit ihrer Gruendung 1960 verwendet und damit einen Wettbewerbsvorteil in der Zuschauerbindung gehabt",
        answerB = "NFL 1970 — nach dem AFL-NFL-Merger; die kanadische CFL hatte sie seit 1948",
        answerC = "NFL 1982 — nach dem Spielerstreik; als Konzession an Spieler wurde die Regel als taktische Erweiterung eingefuehrt",
        answerD = "NFL 2000 — als Reaktion auf hohe Kicker-Trefferquoten bei Extra Points, die zu vorhersehbar geworden waren",
        correctAnswer = 0,
        explanation = "Die NFL fuehrte die Zwei-Punkte-Conversion 1994 ein — 34 Jahre nach der AFL, die sie von Beginn an hatte. Die canadische CFL und das NCAA-College-Football hatten sie ebenfalls frueher. Der NFL-Traditionskonservativismus verzoegerte die Einfuehrung; erst der Wettbewerbsdruck durch interessanteres Spieldesign in anderen Ligen erzwang die Aenderung.",
        difficulty = 5,
        funFact = "Die AFL wurde oft als 'Outlaw League' bezeichnet, war aber in vielen taktischen Regelneuerungen innovativer als die etablierte NFL: Namens-Trikotnummern, zwei Punkte fuer Conversion, Spieler-Namen auf Trikots. Als AFL und NFL 1970 fusionierten, uebernahm die NFL viele AFL-Regeln — ein seltener Fall wo der Herausforderer den Marktfuehrer taktisch beeinfluss-te."
    ),

    // Question 40 – Rules change: basketball 24-second clock
    Question(
        categoryId = 6,
        questionText = "Welches Spiel veranlasste 1954 die NBA, die 24-Sekunden-Regel einzufuehren, und wer war der Erfinder dieser Regel, die den Basketball revolutionierte?",
        answerA = "Das Spiel Fort Wayne Pistons vs. Minneapolis Lakers (19:18) am 22. November 1950; Erfinder: Danny Biasone (Besitzer der Syracuse Nationals), der 2880 Sekunden durch 120 Wuerfe teilte und so auf 24 Sekunden kam",
        answerB = "Das All-Star-Game 1953, das nach Overtime kein Ergebnis lieferte; Erfinder: Red Auerbach (Boston Celtics), der Korbwurffrequenz analysierte",
        answerC = "Das Finale 1952 zwischen Lakers und Knicks, das 58 Minuten lang kein Korb fiel; Erfinder: Ned Irish (MSG-Besitzer)",
        answerD = "Ein Regular-Season-Spiel 1949 mit nur 11 Punkten insgesamt; Erfinder: Gottlieb (Philadelphia Warriors) nach Berechnung optimaler Wurfquoten",
        correctAnswer = 0,
        explanation = "Das Spiel vom 22. November 1950 (19:18 Endstand) gilt als Ausloeser: Die Pistons hielten einfach den Ball, um Lakers-Star George Mikan auszubremsen. Danny Biasone teilte 48 Minuten (2880 Sekunden) durch 120 gewuenschte Wuerfe = 24 Sekunden. Die Regel wurde 1954 eingefuehrt; die Punktzahl stieg sofort dramatisch, das Spiel wurde attraktiver.",
        difficulty = 5,
        funFact = "Danny Biasone gewann 1955 mit den Syracuse Nationals die einzige NBA-Meisterschaft seiner Franchise. Trotz seiner revolutionaeren Regelerfindung wurde er nie in die NBA Hall of Fame aufgenommen — ein historisches Versaeumnis. Die 24-Sekunden-Uhr gilt heute als wichtigste Regelaenderung in der Basketball-Geschichte."
    ),

    // ── Vergessene Sportarten (5) ──────────────────────────────────────────────

    // Question 41 – Forgotten sport: Pelota vasca at Olympics
    Question(
        categoryId = 6,
        questionText = "Welche baskische Ballsportart war bei den Olympischen Spielen 1900 in Paris als Demonstrationssport vertreten und wurde nie vollwertiger olympischer Wettkampf, obwohl sie bis heute ein eigenes Weltmeisterschaftssystem besitzt?",
        answerA = "Pelota vasca (Baskenball/Jai alai) — 1900 Demonstration, danach 1924 und 1968 weitere Demonstrationen; trotz Millionen Spielern weltweit nie in den olympischen Kanon aufgenommen",
        answerB = "Fronton — eine spanische Wandballvariante, 1904 Demonstration in St. Louis",
        answerC = "Trinquete — die Hallenvariante der Pelota, 1900 erstmals gespielt",
        answerD = "Pilota valenciana — katalanische Variante, 1900 Demonstrationssport",
        correctAnswer = 0,
        explanation = "Pelota vasca war 1900, 1924 und 1968 Demonstrationssport bei Olympia. Trotz Verbreitung in Spanien, Frankreich, Mexiko, Argentinien, USA und den Philippinen wurde sie nie regulaerer Wettkampf. Die FIPV (Weltverband) unternimmt regelmaessig Anlaeufe zur olympischen Aufnahme. Jai alai (die schnellste Ballsportart der Welt) ist die populaerste Variante.",
        difficulty = 5,
        funFact = "Jai alai gilt als die schnellste Ballsportart der Welt — der Ball (Pelota) kann Geschwindigkeiten von ueber 300 km/h erreichen. In den USA war Jai alai in den 1970er und 1980er Jahren eine beliebte Wettveranstaltung in Florida. Der Niedergang kam mit der Legalisierung von Kasinos und anderem Gluecksspiel."
    ),

    // Question 42 – Forgotten sport: Tug of war at Olympics
    Question(
        categoryId = 6,
        questionText = "Tauziehen war von 1900 bis 1920 olympische Disziplin. Welcher Skandal beim Tauziehen 1908 in London gilt als fruehestes bekanntes Beispiel von Materialmanipulation bei Olympischen Spielen?",
        answerA = "Die britische Polizei-Mannschaft aus Liverpool trat in speziellen Stiefeln mit extra schweren Metallbeschlaegen an; die US-Mannschaft protestierte erfolglos, verliess den Wettkampf und beschwerte sich beim IOC",
        answerB = "Die schwedische Mannschaft verwendete Handschuhe mit Klebesubstanz; nach Protest wurden alle Ergebnisse annulliert",
        answerC = "Die deutsche Mannschaft wog mehr als erlaubt und betrog bei der Wiegepflicht; Grossbritannien gewann nach Neuwertung",
        answerD = "Die amerikanische Mannschaft verwendete ein Seil aus anderem Material als vorgeschrieben und gewann damit unrechtmaessig",
        correctAnswer = 0,
        explanation = "Das US-Team protestierte, weil die britischen Polizisten aus Liverpool Arbeitsschuhe mit schweren Stahlkappen und tiefen Absaetzen trugen, die ihnen besonderen Halt gaben. Die Briten argumentierten, es seien ihre normalen Dienstschuhe. Das IOC wies den Protest ab; Grossbritannien gewann drei der fuenf Medaillenplaetze im Tauziehen 1908.",
        difficulty = 5,
        funFact = "Tauziehen hatte bei den Olympischen Spielen besondere Regeln: Jede Mannschaft bestand aus acht Mann, Reissen war verboten, und der Kampf dauerte maximal fuenf Minuten. Grossbritannien gewann die meisten olympischen Tauziehmedaillen in der Geschichte. Die Disziplin wurde nach 1920 nie wieder aufgenommen, obwohl es bis heute eine Welt-Tauziehmeisterschaft gibt."
    ),

    // Question 43 – Forgotten sport: Croquet at Olympics
    Question(
        categoryId = 6,
        questionText = "Welche weitgehend vergessene Rasensportart trat 1900 in Paris olympisch auf und ist bis heute der einzige Fall, in dem nur Franzosen teilnahmen — obwohl es eine internationale Veranstaltung war?",
        answerA = "Croquet — alle sechs Teilnehmer waren Franzosen; die einzige Ausnahme war ein Englaender, der sich zwar angemeldet, aber nicht erschienen war; als Zuschauer war laut Dokumenten nur eine einzige Person dabei",
        answerB = "Boules/Petanque — alle Teilnehmer aus der Provence, obwohl Schweizer und Belgier eingeladen wurden",
        answerC = "Roque — amerikanische Crocket-Variante, aber die US-Delegation reiste erst nach den Wettkaeampfen an",
        answerD = "Lawn Tennis — Frankreich stellte alle Spieler, weil andere Nationen die Anmeldegebuehl nicht zahlten",
        correctAnswer = 0,
        explanation = "Croquet 1900 in Paris gilt als der unbesuchteste olympische Wettkampf aller Zeiten: Alle Teilnehmer waren Franzosen; ein britischer Anmelder erschien nicht. Laut zeitgenoessischen Quellen war genau ein (1) Zuschauer anwesend — ein Englaender aus Nizza. Croquet war danach nie wieder olympisch.",
        difficulty = 5,
        funFact = "Croquet ist eine der wenigen Sportarten, in der Maenner und Frauen traditionell gemeinsam wettkaeampften. Die englische Croquet Association wurde 1896 gegruendet — noch vor dem Lawn Tennis-Verband. Croquet galt lange als Damen-sport, wurde aber in der viktorianischen Aera auch von Maennern gespielt und war ein wichtiges gesellschaftliches Ereignis."
    ),

    // Question 44 – Forgotten sport: Solo synchronized swimming
    Question(
        categoryId = 6,
        questionText = "Welche Olympia-Disziplin wurde nur zweimal (1984 und 1988) ausgetragen und dann fuer immer aus dem Programm gestrichen, weil sie als logisch widerspruechlich galt?",
        answerA = "Solo-Synchronschwimmen — eine Einzelperson die sich mit sich selbst 'synchronisiert'; nach 1988 gestrichen, weil das Konzept des 'Synchronisierens' bei einem Solo-Auftritt oxymoronisch ist",
        answerB = "Einzel-Rudern auf 500m — als zu kurz und nicht repraesentativ fuer Rudersport gewertet",
        answerC = "Solo-Wasserspringen vom 3m-Brett mit Pflichtprogramm — nach 1988 durch freies Programm ersetzt",
        answerD = "Einzel-Eiskunstlauf pflicht-solo — durch die Pflicht-Kuer-Kombination ersetzt",
        correctAnswer = 0,
        explanation = "Solo-Synchronschwimmen war 1984 und 1988 olympisch, wurde aber wegen des inhärenten logischen Widerspruchs (man kann sich als Einzelperson nicht synchronisieren) nach 1988 gestrichen. 1996 wurde Synchronschwimmen als Duett und Mannschaft weitergeführt. Das Solo verschwand aus dem olympischen Programm — und auch international ist es heute weitgehend bedeutungslos.",
        difficulty = 5,
        funFact = "Tracie Ruiz-Conforto (USA) gewann sowohl 1984 als auch 1988 Gold im Solo-Synchronschwimmen und wurde damit zweifache Olympiasiegerin in einer Disziplin, die heute nicht mehr existiert. Ihr Stil praegend das fruehe Synchronschwimmen; sie gilt als Pionierin der Sportart in den USA."
    ),

    // Question 45 – Forgotten sport: Plunge for distance
    Question(
        categoryId = 6,
        questionText = "Bei den Olympischen Spielen 1904 in St. Louis gab es eine Schwimmdisziplin namens 'Plunge for Distance' — welche einzigartige Regel definierte diesen Wettbewerb, und wer gewann ihn?",
        answerA = "Der Athlet tauchte vom Startblock ins Wasser und musste dann 60 Sekunden lang reglos treiben — wer in dieser Zeit die groesste Distanz zuruecklegte (ohne Schwimmbewegung, nur durch Auftrieb und Anfangsimpuls), gewann; Sieger: William Dickey (USA) mit 19,05 Metern",
        answerB = "Der Athlet musste so tief wie moeglich tauchen; Tiefe wurde mit einem Senkblei gemessen; Sieger: Charles Daniels (USA)",
        answerC = "Der Athlet musste unter Wasser schwimmend die groesste Strecke zuruecklegen; Sieger: Zoltan Halmay (Ungarn)",
        answerD = "Der Athlet sprang aus groesster Hoehe und wurde nach Sprungweite vom Absprungpunkt bis Eintauchpunkt bewertet; Sieger: Eugen Sandow (Deutschland)",
        correctAnswer = 0,
        explanation = "Plunge for Distance war eine einmalige Disziplin: Athleten sprangen kopfueber ins Wasser und mussten dann 60 Sekunden lang vollkommen still bleiben (keine Schwimmbe-wegung). Wer rein durch den Anfangsimpuls und Auftrieb am weitesten trieb, gewann. William Dickey gewann mit 19,05 Metern. Die Disziplin wurde nie wiederholt.",
        difficulty = 5,
        funFact = "Die Olympischen Spiele 1904 in St. Louis hatten viele bizarre Wettbewerbe: Neben Plunge for Distance gab es 'Obstacle Race' im Schwimmen (Athleten mussten Boote ueberqueren und unter anderen hindurchtauchen), und das Turnen enthielt 'Clubs'-Jonglage als Pflichtdisziplin. Das Chaos dieser Spiele ist fuer Sporthistoriker ein reiches Studienfeld."
    ),

    // ── Sport in der Weimarer Republik und NS-Zeit (5) ───────────────────────

    // Question 46 – Weimar Republic: Worker Olympics
    Question(
        categoryId = 6,
        questionText = "Die zweite Arbeiterolympiade 1931 in Wien gilt als die groesste internationale Sportveranstaltung zwischen den Weltkriegen. Wie viele Laender und Athleten nahmen teil, und welche politische Botschaft transportierte die Eroefffnungsfeier?",
        answerA = "26 Laender mit ca. 80.000 Teilnehmern (Athleten + Kulturkuenstler); die Eroefffnungsfeier vermied Nationalflaggen und Nationalhymnen — stattdessen internationale Arbeiterlieder und rote Fahnen als Symbol proletarischer Solidaritaet jenseits des Nationalstaats",
        answerB = "15 Laender mit 25.000 Athleten; die Eroefffnungsfeier kopierte bewusst das olympische Zeremoniell als Gegenprogramm",
        answerC = "40 Laender mit 150.000 Teilnehmern; die Eroefffnungsfeier hatte eine riesige Parade durch Wien mit sozialistischen Symbolen",
        answerD = "10 Laender mit 10.000 Athleten; die Eroefffnungsfeier wurde wegen politischer Unruhen in Wien abgebrochen",
        correctAnswer = 0,
        explanation = "Die Wiener Arbeiterolympiade 1931 war mit rund 80.000 Teilnehmern (inklusive Kulturwettbewerbe in Musik, Theater, Kunst) die groesste internationale Sportveranstaltung ihrer Zeit. Die bewusste Ablehnung von Nationalsymbolen war politisches Programm: Proletarischer Internationalismus gegen buergerlichen Nationalismus. 1933 wurden alle Arbeiter-Sportverbände in NS-Deutschland verboten.",
        difficulty = 5,
        funFact = "Die Arbeiter-Sportbewegung hatte in der Weimarer Republik ueber 2,5 Millionen Mitglieder — mehr als alle anderen Sportorganisationen zusammen. Durch die NS-Machtergreifung wurden innerhalb weniger Monate alle Arbeiter-Sportvereine aufgeloest, Vermögen konfisziert und Funktionäre verhaftet. Viele emigrierten; andere landeten in Konzentrationslagern."
    ),

    // Question 47 – NS sport policy: Reichssportfuehrer
    Question(
        categoryId = 6,
        questionText = "Wer war von 1933 bis 1945 'Reichssportfuehrer' in Nazi-Deutschland, welche Organisation fusionierte er unter NS-Kontrolle, und warum gilt seine Sportpolitik als Paradox?",
        answerA = "Hans von Tschammer und Osten — er fueherte alle deutschen Sportverbände im Deutschen Reichsbund fuer Leibesuebungen (DRL) zusammen; Paradox: Er modernisierte die Sportinfrastruktur massiv (Olympia-Stadion, Schwimmbäder, Turnhallen) — jedoch ausschliesslich fuer 'arische' Deutsche, waehrend juedische und politisch Andersdenkende ausgeschlossen wurden",
        answerB = "Joseph Goebbels — er kontrollierte Sport als Teil seiner Medienpropaganda und fusionierte Sport mit Filmproduktion",
        answerC = "Heinrich Himmler — er organisierte SS-Sport als Pendant zum Reichssport; Sportler wurden nach Rassenmerkmalen ausgewaehlt",
        answerD = "Baldur von Schirach — als Hitlerjugend-Fuehrer organisierte er den gesamten deutschen Jugendsport unter NS-Kontrolle",
        correctAnswer = 0,
        explanation = "Hans von Tschammer und Osten (1887-1943) schuf 1934 den DRL (spaeter NSRL) als Einheitsverband. Er investierte massiv in Sportinfrastruktur — fuer Olympia 1936 und als Massenorganisations-Werkzeug. Das Paradox: Moderner Massensport wurde aufgebaut, waehrend systematisch Juden, politische Gegner und 'Rassisch Unerwuenschte' ausgeschlossen wurden.",
        difficulty = 5,
        funFact = "Von Tschammer und Osten starb 1943 an einer Lungenentzuendung — kurz bevor die NS-Niederlage absehbar wurde. Er erlebte nicht mehr den Untergang des Systems, das er mit aufgebaut hatte. Sein Nachfolger Arno Breker (Bildhauer) uebernahm nominell die Sportfuehrung, aber der Krieg hatte den organisierten Sport laengst bedeutungslos gemacht."
    ),

    // Question 48 – NS sport: Felix Linnemann and DFB
    Question(
        categoryId = 6,
        questionText = "Wie reagierte der Deutsche Fussball-Bund (DFB) unter seinem Vorsitzenden Felix Linnemann auf die NS-Machtergreifung 1933, und welches 'Ehrenmitglied' wurde dabei geschaffen?",
        answerA = "Der DFB 'gleichschaltete' sich freiwillig, bevor er dazu gezwungen wurde; Linnemann bot Hitler die Ehrenmitgliedschaft an — Hitler lehnte ab, aber der DFB entfernte juedische Mitglieder proaktiv, noch bevor entsprechende NS-Gesetze dies forderten",
        answerB = "Der DFB widersetzte sich der Gleichschaltung sechs Monate lang; erst nach Inhaftierung des Vorsitzenden gab er nach",
        answerC = "Der DFB bestand auf Autonomie des Sports und konnte bis 1936 seine Unabhaengigkeit bewahren",
        answerD = "Der DFB bot Himmler die Ehrenpraesidentschaft an als Kompromiss fuer den Erhalt der Vereinsstrukturen",
        correctAnswer = 0,
        explanation = "Der DFB unter Linnemann kollaborierte proaktiv: Juedische Mitglieder wurden ausgeschlossen, noch ehe entsprechende NS-Gesetze galten. Das Angebot der Hitler-Ehrenmitgliedschaft zeigt den opportunistischen Charakter der Selbstgleichschaltung. Linnemann blieb bis 1945 DFB-Vorsitzender. Der DFB entschuldigte sich erst 2005 offiziell fuer sein Verhalten 1933-1945.",
        difficulty = 5,
        funFact = "Der DFB-Hauptsitz in Frankfurt traegt seit 2009 den Namen 'DFB-Campus' — bewusst ohne Bezug auf belastete historische Persönlichkeiten. Der Verband hat inzwischen seine NS-Geschichte systematisch aufgearbeitet. Aber noch 2023 diskutierte die Historiker-Kommission ueber die vollstaendige Publizierung aller Erkenntnisse."
    ),

    // Question 49 – NS sport: Jewish Maccabi movement
    Question(
        categoryId = 6,
        questionText = "Welche juedische Sportorganisation organisierte 1935 in Tel Aviv die ersten 'Maccabiah Games' (juedische Olympiade), und wie reagierten 51 juedische Athleten aus Deutschland auf diese Veranstaltung in historisch dramatischer Weise?",
        answerA = "Maccabi World Union organisierte die zweiten Maccabiah 1935 (die ersten waren 1932); 51 deutsche juedische Athleten, die nach Tel Aviv gereist waren, weigerten sich nach dem Ende der Spiele zur Rueckkehr nach Deutschland und blieben illegal in Palaestina — eine der ersten organisierten juedischen Auswanderungsaktionen",
        answerB = "Hapoel (Arbeiter-Sportbewegung) organisierte 1935 die ersten internationalen juedischen Spiele; deutsche Athleten wurden vom NS-Regime an der Ausreise gehindert",
        answerC = "YMHA (Young Men's Hebrew Association) organisierte 1935 die Spiele; 51 Athleten wanderten nach Argentinien aus statt nach Palaestina",
        answerD = "Bar Kochba (Berliner Sportverein) organisierte 1933 die ersten Maccabiah in Berlin als Gegenprogramm zur Gleichschaltung",
        correctAnswer = 0,
        explanation = "Die zweiten Maccabiah 1935 in Tel Aviv sahen eine historische Masseneinwanderung: 51 juedische Sportler aus Deutschland weigerten sich nach den Spielen zur Rueckkehr. Sie blieben illegal in Palaestina und beantragten spaeter Aufenthaltsgenehmigungen. Dies war eine der fruehesten organisierten Reaktionen auf die NS-Verfolgung in Form von sportlicher Aktivitaet.",
        difficulty = 5,
        funFact = "Die Maccabiah Games finden bis heute alle vier Jahre statt — ein Jahr nach den Olympischen Spielen. Sie gelten als 'juedische Olympiade'. 2022 nahmen ueber 10.000 Athleten aus 109 Laendern teil. Die Spiele haben eine direkte historische Linie zur zionistischen Turnbewegung Bar Kochba, die 1898 in Berlin gegruendet wurde."
    ),

    // Question 50 – Historiography: sport history methods
    Question(
        categoryId = 6,
        questionText = "Welches methodologische Problem ist fuer die Sportgeschichtsschreibung der NS-Zeit besonders charakteristisch, und welche Forschungsinitiative des Deutschen Olympischen Sportbundes (DOSB) versuchte ab 2012 systematisch Abhilfe zu schaffen?",
        answerA = "Das Täterproblem: Viele NS-Sportfunktionäre setzten nach 1945 ihre Karriere nahtlos fort und kontrollierten die Archive ihrer eigenen Verbände; der DOSB startete 2012 das Projekt 'Sportdeutschland und Nationalsozialismus' mit unabhaengigen Historikerkommissionen fuer alle grossen Verbände",
        answerB = "Das Quellenproblem: NS-Regime vernichtete systematisch Sportakten; DOSB-Digitalisierungsprojekt ab 2012 sollte Reste sichern",
        answerC = "Das Opferproblem: Ueberlebende juedische Sportler wurden nie befragt; DOSB-Zeitzeugen-Programm ab 2012",
        answerD = "Das Vergleichsproblem: Internationale Verbände weigerten sich, Akten zu teilen; DOSB schloss 2012 Abkommen mit IOC-Archiv",
        correctAnswer = 0,
        explanation = "Das Taeterproblem ist zentral: Funktionäre wie Karl Ritter von Halt (IOC-Mitglied bis 1964), Willi Daume (DFB, NOK) und andere wechselten nahtlos von NS- in Bundesrepublik-Positionen. Sie kontrollierten Verbands-Archive und praegten die Erinnerungskultur. Die DOSB-Historikerkommissionen ab 2012 arbeiteten unabhaengig und publizierten teils unbequeme Wahrheiten.",
        difficulty = 5,
        funFact = "Karl Ritter von Halt war 1936 Mitorganisator der Berliner Spiele und IOC-Mitglied bis 1964. Er war auch SS-Sturmbannfuehrer und NSDAP-Mitglied. Trotz Internierung nach Kriegsende kehrte er nahtlos in den olympischen Sport zurueck. Sein Fall steht exemplarisch fuer die unvollstaendige Entnazifizierung des westdeutschen Sports."
    )

)
