package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMaster5(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) — Beruehmte Rechtsfaelle & Rechtsgeschichte ──

    // Frage 1 — Nuernberger Prozesse / Anklagepunkte
    Question(
        categoryId = 11,
        questionText = "Der Internationale Militaergerichtshof in Nuernberg 1945/46 klagte die NS-Hauptverbrecher unter vier Anklagepunkten an. Welcher Anklagepunkt wurde dabei als ERSTER und schwerster eingestuft und betraf die Planung eines Angriffskrieges?",
        answerA = "Kriegsverbrechen gegen die Zivilbevoelkerung",
        answerB = "Verbrechen gegen den Frieden",
        answerC = "Verbrechen gegen die Menschlichkeit",
        answerD = "Verschwoerungs-Anklagepunkt gegen den Weltfrieden",
        correctAnswer = 1,
        explanation = "Der erste und schwerste Anklagepunkt war 'Verbrechen gegen den Frieden' — die Planung, Vorbereitung, Einleitung und Fuehrung von Angriffskriegen. Damit wurde erstmals in der Geschichte Staatspolitik als individuelle Straftat definiert. Die vier Punkte waren: 1) Verschwoerungs-Anklagepunkt, 2) Verbrechen gegen den Frieden, 3) Kriegsverbrechen, 4) Verbrechen gegen die Menschlichkeit.",
        difficulty = 5,
        funFact = "Das Gericht tagte im Justizpalast Nuernberg — Saal 600. Dieser Saal wird bis heute fuer Gedenkveranstaltungen genutzt und ist ein nationales Denkmal."
    ),

    // Frage 2 — Nuernberger Prozesse / Freisprueche
    Question(
        categoryId = 11,
        questionText = "Drei der 24 Angeklagten im Nuernberger Hauptkriegsverbrecherprozess wurden freigesprochen. Welcher dieser drei war ehemaliger Reichsbankpraesident und Wirtschaftsminister, der nachweislich von den Verbrechen gewusst hatte?",
        answerA = "Franz von Papen",
        answerB = "Hans Fritzsche",
        answerC = "Hjalmar Schacht",
        answerD = "Albert Speer",
        correctAnswer = 2,
        explanation = "Hjalmar Schacht wurde freigesprochen, obwohl er die NS-Aufruestung finanziert hatte. Das Gericht sah keinen ausreichenden Beweis, dass er die Kriegsplanung kannte. Die drei Freigesprochenen waren: Hjalmar Schacht (Reichsbankpraesident), Franz von Papen (ehemaliger Reichskanzler) und Hans Fritzsche (Rundfunkchef). Alle drei wurden kurz darauf von deutschen Entnazifizierungsgerichten verurteilt.",
        difficulty = 5,
        funFact = "Schacht nannte seinen eigenen Freispruch spaeter einen 'Fehler des Gerichts'. Er ueberlebte bis 1970 und schrieb Memoiren, in denen er seine Unschuld weiterhin beteuerte."
    ),

    // Frage 3 — Nuernberger Prozesse / Hermann Goering
    Question(
        categoryId = 11,
        questionText = "Hermann Goering entzog sich seinem Todesurteil in Nuernberg durch Selbstmord. Wie hatte er das Zyankaligift in seine Zelle geschmuggelt — ein bis heute nicht vollstaendig aufgeklaerter Vorgang?",
        answerA = "In einem Hohlraum seines Zahnfleisches versteckt",
        answerB = "In einer Cremedose, die ein US-Wachsoldat einschmuggelte",
        answerC = "Ueber einen Kellner beim letzten Abendessen",
        answerD = "In einer Lederkapsel eingeschweisst in seinen Stiefeln",
        correctAnswer = 1,
        explanation = "Die genaue Herkunft des Giftes ist historisch umstritten. Die wahrscheinlichste Theorie laut neueren Untersuchungen (u.a. durch Ben Macintyre) ist, dass US-Leutnant Jack Wheelis das Zyanid in einer Cremedose einschmuggelte, die Goering gehoerte und bei der Aufnahme konfisziert worden war. Goering staerb am 15. Oktober 1946, zwei Stunden vor seiner geplanten Hinrichtung.",
        difficulty = 5,
        funFact = "Goering hinterliess einen Abschiedsbrief, in dem er erklaerte, er sterbe als Maertyrer fuer Deutschland. Seine Asche wurde im Isar-Fluss verstreut, damit keine Gedenkstaette entstehen konnte."
    ),

    // Frage 4 — Nuernberger Aerzteprozess / Kodex
    Question(
        categoryId = 11,
        questionText = "Der Nuernberger Aerzteprozess (1946-1947) fuehrte zur Formulierung des Nuernberger Kodex. Welches zentrale Prinzip wurde darin erstmals als absolutes Muss fuer medizinische Experimente festgelegt?",
        answerA = "Alle Experimente muessen von einem Ethikkomitee genehmigt werden",
        answerB = "Die freiwillige Einwilligung der Versuchsperson ist absolut notwendig",
        answerC = "Tierversuche muessen dem Menschenexperiment immer vorangehen",
        answerD = "Das Experiment muss dem gesellschaftlichen Nutzen dienen",
        correctAnswer = 1,
        explanation = "Der Nuernberger Kodex von 1947 stellte als alleroberste Bedingung die freiwillige, informierte Einwilligung ('informed consent') der Versuchsperson auf. Dies war eine direkte Reaktion auf die NS-Zwangsversuche in Konzentrationslagern. Der Kodex beeinflusste die Deklaration von Helsinki (1964) und bildet bis heute die ethische Grundlage der Forschungsmedizin weltweit.",
        difficulty = 5,
        funFact = "Von 23 angeklagten Aerzten wurden 7 zum Tode verurteilt, u.a. Karl Brandt (Hitlers Leibarzt). Der Prozess begann am 9. Dezember 1946 — genau einen Monat nach Ende des Hauptprozesses."
    ),

    // Frage 5 — Eichmann-Prozess
    Question(
        categoryId = 11,
        questionText = "Adolf Eichmann wurde 1960 vom israelischen Geheimdienst Mossad in Buenos Aires entfuehrt und nach Israel gebracht. Unter welchem Namen lebte er in Argentinien im Versteck?",
        answerA = "Hans Fischer",
        answerB = "Otto Henninger",
        answerC = "Ricardo Klement",
        answerD = "Klaus Mueller",
        correctAnswer = 2,
        explanation = "Eichmann lebte unter dem Namen 'Ricardo Klement' in einem Vorort von Buenos Aires (San Fernando). Argentinien protestierte scharf gegen die Entfuehrung als Verletzung des Voelkerrechts. Der israelische Ministerpraesident Ben-Gurion rechtfertigte die Aktion mit dem Argument, Schwerverbrecher duerfen nicht ungestraft bleiben — ein bis heute kontrovers diskutierter Praezedenzfall.",
        difficulty = 5,
        funFact = "Eichmann war der erste Mensch, der in Israel nach dem Todesurteil hingerichtet wurde — und bis heute einer von nur zwei. Er wurde am 31. Mai 1962 in Ramla gehaengt."
    ),

    // Frage 6 — Hannah Arendt / Banalitaet des Boesen
    Question(
        categoryId = 11,
        questionText = "Hannah Arendts Begriff 'Banalitaet des Boesen' aus ihrem Bericht ueber den Eichmann-Prozess wurde kontrovers diskutiert. Was meinte Arendt mit diesem Begriff im Kern?",
        answerA = "Das Boese ist immer durch ideologischen Fanatismus motiviert",
        answerB = "Eichmann war ein normaler Buerger ohne Ideologie, der blind Befehle ausfuehrte",
        answerC = "Massenverbrechen entstehen durch Gedankenlosigkeit und das Aussetzen des eigenen Urteilsvermoegen",
        answerD = "Das Boese wird banal, wenn es von Medien und Bevoelkerung normalisiert wird",
        correctAnswer = 2,
        explanation = "Arendts zentrales Argument war: Eichmann war kein Monster, kein ideologischer Fanatiker — er war erschreckend normal. Er dachte nicht nach. Diese 'Gedankenlosigkeit' ermoeglichte ihm, an einem Massenmord mitzuwirken, ohne die moralische Dimension zu erfassen. Arendt sprach von 'Gedankenlosigkeit' als eigenstaendiger Wurzel des Boesen — eine philosophisch revolutionaere These.",
        difficulty = 5,
        funFact = "Arendts Bericht spaltete die juedische Intellektuellengemeinschaft tief. Gershom Scholem warf ihr 'Herzlosigkeit' vor. Ihre Freundschaft zerbrach daran — ebenso wie viele andere persoenliche Beziehungen."
    ),

    // Frage 7 — Dreyfus-Affaere
    Question(
        categoryId = 11,
        questionText = "In der Dreyfus-Affaere (1894-1906) wurde der franzoesische Hauptmann Alfred Dreyfus faelschlich des Hochverrats beschuldigt. Wer war der wahre Spion, dessen Schuld jahrelang vertuscht wurde?",
        answerA = "Major Ferdinand Walsin Esterhazy",
        answerB = "Oberst Georges Picquart",
        answerC = "General Auguste Mercier",
        answerD = "Hauptmann Louis Lebrun-Renaud",
        correctAnswer = 0,
        explanation = "Major Ferdinand Walsin Esterhazy war der echte Spion, der Dokumente an Deutschland weitergegeben hatte. Oberst Picquart entdeckte dies 1896 und wurde dafuer selbst verfolgt und ins Ausland versetzt. Die Armee deckte Esterhazy und verfolgte weiterhin den unschuldigen Dreyfus — ein Fall institutionellen Antisemitismus. Esterhazy wurde 1898 offiziell freigesprochen und floeh nach England.",
        difficulty = 5,
        funFact = "Emile Zolas offener Brief 'J'accuse!' erschien am 13. Januar 1898 auf der Titelseite von L'Aurore und hatte eine Auflage von 300.000 Exemplaren — ein Meilenstein des investigativen Journalismus."
    ),

    // Frage 8 — Dreyfus / Herzl / Zionismus
    Question(
        categoryId = 11,
        questionText = "Die Dreyfus-Affaere hatte eine weitreichende historische Konsequenz ausserhalb Frankreichs. Welcher Journalist berichtete ueber den Prozess und wurde dadurch zum Gruender einer politischen Bewegung?",
        answerA = "Karl Kraus",
        answerB = "Theodor Herzl",
        answerC = "Max Nordau",
        answerD = "Nathan Birnbaum",
        correctAnswer = 1,
        explanation = "Theodor Herzl, Korrespondent der Wiener Neuen Freien Presse in Paris, erlebte die antisemitischen Hetzrufe waehrend der Degradierung von Dreyfus. Dieses Erlebnis ueberzeugte ihn, dass Juden nur in einem eigenen Staat sicher sein koennten. 1896 veroeffentlichte er 'Der Judenstaat', 1897 gruendete er den Zionistischen Weltkongress — der Startschuss fuer die zionistische Bewegung und letztlich fuer die Gruendung Israels 1948.",
        difficulty = 5,
        funFact = "Herzl starb 1904 mit 44 Jahren — er erlebte die Gruendung Israels nie. Sein letzter Wunsch war, seine Gebeine nach Palaestina zu ueberfuehren. Das geschah erst 1949, ein Jahr nach Israels Unabhaengigkeit."
    ),

    // Frage 9 — Volksgerichtshof / Roland Freisler
    Question(
        categoryId = 11,
        questionText = "Roland Freisler, Praesident des nationalsozialistischen Volksgerichtshofs, starb am 3. Februar 1945 in Berlin. Unter welchen Umstaenden kam er ums Leben?",
        answerA = "Er wurde von einem Gefangenen in seinem Buero erschossen",
        answerB = "Er starb bei einem alliierten Bombenangriff waehrend einer Gerichtsverhandlung",
        answerC = "Er beging Selbstmord bei Einmarsch der Sowjets",
        answerD = "Er wurde von SS-Offizieren nach dem gescheiterten Stauffenberg-Attentat liquidiert",
        correctAnswer = 1,
        explanation = "Freisler kam am 3. Februar 1945 ums Leben, als ein amerikanischer Bomberangriff auf Berlin das Gebaeude des Volksgerichtshofs traf, waehrend er gerade verhandelte. Er wurde von einem Balken erschlagen — und hielt dabei noch die Akten eines Angeklagten in den Haenden. Seine Beerdigung fand im engsten Kreis statt; Hitler soll kommentiert haben, Freisler sei fuer das Reich gestorben.",
        difficulty = 5,
        funFact = "Am selben Tag starb auch der Schriftsteller Hans Fallada. Freisler hatte kurz zuvor 15 Attentaeter des 20. Juli 1944 zum Tode verurteilt — viele wurden vor seinen Augen mit Pianodraht erdrosselt, Filmaufnahmen davon liess Hitler sich wiederholt vorfuehren."
    ),

    // Frage 10 — Reichstagsbrand / Van der Lubbe
    Question(
        categoryId = 11,
        questionText = "Marinus van der Lubbe wurde 1934 fuer den Reichstagsbrand hingerichtet. Welche juristische Besonderheit machte dieses Urteil zu einem Skandal in der Rechtsgeschichte?",
        answerA = "Er wurde nach einem Gesetz verurteilt, das erst NACH der Tat rueckwirkend erlassen wurde",
        answerB = "Das Urteil wurde ohne Anhoerung seines Verteidigers gesprochen",
        answerC = "Er wurde als Geisteskranker ohne Prozessfaehigkeit trotzdem hingerichtet",
        answerD = "Das Urteil wurde auf einem anderen Rechtsweg als der offizielle Strafprozess erlangt",
        correctAnswer = 0,
        explanation = "Die Nazis erliessen das 'Gesetz ueber Verhaengung und Vollzug der Todesstrafe' (Lex van der Lubbe) rueckwirkend auf den 1. Januar 1933, also nach der Tat vom 27. Februar 1933. Dieses Rueckwirkungsverbot (nulla poena sine lege praevia) ist ein Grundprinzip des Rechtsstaats. Van der Lubbe wurde 2008 posthum von der Bundesrepublik rehabilitiert.",
        difficulty = 5,
        funFact = "Ob van der Lubbe den Brand allein legte oder von den Nazis instrumentalisiert wurde, ist bis heute historisch umstritten. Die Nazis nutzten den Brand als Vorwand fuer die Reichstagsbrandverordnung, mit der die Buergerrechte ausgesetzt wurden."
    ),

    // Frage 11 — Tokio-Prozesse
    Question(
        categoryId = 11,
        questionText = "Bei den Tokio-Prozessen (1946-1948) wurde Kaiser Hirohito bewusst nicht angeklagt, obwohl er formell Oberbefehlshaber war. Wer war der einzige der 28 Hauptangeklagten, der freigesprochen wurde?",
        answerA = "Hideki Tojo (ehemaliger Premierminister)",
        answerB = "Shigenori Togo (ehemaliger Aussenminister)",
        answerC = "Shumei Okawa (Ultranationalist und Publizist)",
        answerD = "Mamoru Shigemitsu (Unterzeichner der Kapitulation)",
        correctAnswer = 2,
        explanation = "Shumei Okawa war der einzige Freigesprochene — jedoch nicht weil er unschuldig war, sondern weil er fuer geisteskrank erklaert wurde. Er schlug zu Prozessbeginn Tojo auf den Kahlkopf und lachte unkontrolliert. Okawa wurde in eine psychiatrische Anstalt eingewiesen. 7 Angeklagte wurden gehaengt, darunter Premierminister Hideki Tojo.",
        difficulty = 5,
        funFact = "Beim Haengen am 23. Dezember 1948 ueberlebte Kenji Doihara den ersten Strang — er wurde sofort erneut gehaengt. Der Richter Radhabinod Pal aus Indien war das einzige Mitglied, das alle Angeklagten fuer unschuldig hielt — sein Dissenting Opinion wurde jahrzehntelang in Japan zelebriert."
    ),

    // Frage 12 — Luethe-Urteil Bundesverfassungsgericht
    Question(
        categoryId = 11,
        questionText = "Das Luethe-Urteil des Bundesverfassungsgerichts von 1958 gilt als eine der bedeutsamsten Entscheidungen der deutschen Rechtsgeschichte. Was war der Kern seiner Bedeutung fuer die Rechtsdogmatik?",
        answerA = "Es erklaerte die Pressefreiheit zum absoluten Grundrecht ohne Einschraenkungen",
        answerB = "Es begruendete die mittelbare Drittwirkung der Grundrechte ins Privatrecht",
        answerC = "Es stellte fest, dass BGB-Normen grundgesetzwidrig sein koennen und nichtig sind",
        answerD = "Es einfuehrte das Prinzip der Verhaeltnismaessigkeit als Pruefmassstab",
        correctAnswer = 1,
        explanation = "Erich Luethe hatte zum Boykott eines Films des NS-Regisseurs Veit Harlan aufgerufen. Ein Zivilgericht verurteilte ihn. Das BVerfG hob dies auf und stellte fest: Grundrechte gelten nicht nur gegen den Staat, sondern strahlen als objektive Werteordnung in das gesamte Privatrecht aus (mittelbare Drittwirkung). Damit wurde das Grundgesetz zur Grundlage des gesamten deutschen Rechtssystems.",
        difficulty = 5,
        funFact = "Veit Harlan, der Regisseur von 'Jud Suss' (1940), war der am weitesten verbreitete antisemitische Propagandafilm des NS-Regimes. Das Luethe-Urteil erklaerte seinen Weiterbeschaeftigungsboykott fuer legal."
    ),

    // Frage 13 — Bundesverfassungsgericht / Zaehlkandidatenfall
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr gruendete das Bundesverfassungsgericht in Karlsruhe seine Arbeit auf — und welchen ungewoehnlichen Umstand gab es bei der Wahl seines ersten Praesidenten?",
        answerA = "1949 — der erste Praesident wurde vom Bundeskanzler persoenlich ernannt",
        answerB = "1951 — beide Senate wahlten verschiedene Praesident-Kandidaten und stritten um die Zustaendigkeit",
        answerC = "1951 — Praesident Heuss lehnte den ersten Vorschlag des Bundestages zweimal ab",
        answerD = "1950 — der erste Praesident war gleichzeitig amtierender Justizminister",
        correctAnswer = 1,
        explanation = "Das BVerfG nahm am 7. September 1951 seine Arbeit auf. Bei der Wahl des ersten Praesidenten gab es tatsaechlich einen Kompetenzkonflikt zwischen den beiden Senaten ueber Zustaendigkeit und Rangfolge. Hermann Hoepker-Aschoff wurde erster Praesident. Das Gericht hat seinen Sitz bis heute in Karlsruhe — eine bewusste Entscheidung, es vom politischen Zentrum Bonn/Berlin fernzuhalten.",
        difficulty = 5,
        funFact = "Das Bundesverfassungsgericht hat bisher erst zweimal in seiner Geschichte ein Gesetz fuer komplett nichtig erklaert — normalerweise werden Normen nur fuer 'unvereinbar mit dem Grundgesetz' erklaert, was dem Gesetzgeber Reparaturzeit gibt."
    ),

    // Frage 14 — Nicaragua gegen USA am IGH
    Question(
        categoryId = 11,
        questionText = "Im Nicaragua-Fall (1986) verurteilte der Internationale Gerichtshof die USA. Wie reagierten die USA auf das Urteil — und welche Konsequenz hatte das fuer das Voelkerrecht?",
        answerA = "Die USA zahlten Schadensersatz und entschuldigten sich offiziell bei Nicaragua",
        answerB = "Die USA erkannten die Zustaendigkeit des IGH in Sicherheitsfragen nicht an und ignorierten das Urteil",
        answerC = "Die USA legten Berufung beim UN-Sicherheitsrat ein, der das Urteil aufhob",
        answerD = "Die USA verliessen voruebergehend den Internationalen Gerichtshof",
        correctAnswer = 1,
        explanation = "Die USA erklaerten nach dem Urteil, sie erkennen die Pflichtgerichtsbarkeit des IGH in Fragen der nationalen Sicherheit nicht an, und kuendigten ihre Anerkennung der Gerichtsbarkeit auf. Das Urteil blieb vollstaendig unausgefuehrt. Dieser Fall illustriert das Grundproblem des Voelkerrechts: Es gibt keine Vollstreckungsinstanz, die Grossmaechte zwingen kann.",
        difficulty = 5,
        funFact = "Der IGH stellte fest, dass die USA durch die Unterstuetzung der Contra-Rebellen und die Verminung nicaraguanischer Haefen gegen das voelkerrechtliche Gewaltverbot verstossen hatten — ein Praezedenzfall, der fuer spaetere Konflikte immer wieder zitiert wird."
    ),

    // Frage 15 — LaGrand-Fall
    Question(
        categoryId = 11,
        questionText = "Im LaGrand-Fall (2001) stellte der IGH erstmals verbindlich fest, dass seine einstweiligen Verfuegungen rechtlich bindend sind. Was war der Hintergrund dieses bahnbrechenden Urteils?",
        answerA = "Deutschland klagte gegen die USA, weil zwei deutsche Bruedern die konsularische Betreuung verweigert und sie hingerichtet wurden",
        answerB = "Frankreich klagte gegen Israel wegen Missachtung eines IGH-Moratoriums",
        answerC = "Pakistan klagte gegen Indien nach einer Grenzverletzung trotz laufendem IGH-Verfahren",
        answerD = "Iran klagte gegen die USA wegen Einfrierung von Staatsvermogen",
        correctAnswer = 0,
        explanation = "Deutschland klagte gegen die USA, weil die Bruedern Karl und Walter LaGrand (deutsche Staatsangehoerige) in Arizona ohne konsularische Betreuung gemaess Wiener Konsularuebereinkommen zum Tode verurteilt und hingerichtet wurden. Der IGH hatte eine einstweilige Verfuegung erlassen, die Hinrichtung zu stoppen — die USA ignorierten sie. Das Gericht erklaerte daraufhin, einstweilige Verfuegungen des IGH sind bindend.",
        difficulty = 5,
        funFact = "Walter LaGrand wurde am 3. Maerz 1999 in Arizona hingerichtet, obwohl der IGH tags zuvor die Aussetzung angeordnet hatte. Das Verfahren dauerte danach noch zwei Jahre — das Urteil kam quasi posthum."
    ),

    // Frage 16 — Scopes Monkey Trial
    Question(
        categoryId = 11,
        questionText = "Im Scopes Monkey Trial (1925) wurde Lehrer John Scopes in Tennessee angeklagt, weil er Evolution unterrichtet hatte. Was war das tatsaechliche Strafmass, das er erhielt?",
        answerA = "6 Monate Gefaengnis und 500 Dollar Geldstrafe",
        answerB = "100 Dollar Geldstrafe — das niedrigste Strafmass das das Gesetz erlaubte",
        answerC = "Entlassung aus dem Schuldienst und 200 Dollar Geldstrafe",
        answerD = "Bewaehrungsstrafe ohne Geldstrafe nach Berufungsverfahren",
        correctAnswer = 1,
        explanation = "Scopes wurde zu einer Geldstrafe von 100 Dollar verurteilt — dem gesetzlichen Minimum. Das Urteil wurde jedoch vom Tennessee Supreme Court auf technischer Grundlage aufgehoben (der Richter haette die Hoehe selbst festsetzen muessen, nicht die Jury). Der Butler Act, der die Evolutionslehre verbot, blieb bis 1967 in Kraft.",
        difficulty = 5,
        funFact = "Die Verteidigung lud kein Geringeres als H.L. Mencken als Korrespondenten ein — er praegte den spottischen Begriff 'Monkey Trial'. Die Verhandlung war die erste in den USA, die live im Radio uebertragen wurde."
    ),

    // Frage 17 — Voelkermord-Konvention / Raphael Lemkin
    Question(
        categoryId = 11,
        questionText = "Das Wort 'Genozid' (Voelkermord) wurde von einem Juristen erfunden, der auch die UN-Voelkermordkonvention massgeblich formulierte. Wer war dieser Mensch und woher kam er?",
        answerA = "Hersch Lauterpacht — ein britischer Jurist geburtig aus Oesterreich",
        answerB = "Raphael Lemkin — ein polnisch-juedischer Jurist, der in die USA emigrierte",
        answerC = "Robert Jackson — US-Chefanklaeger in Nuernberg",
        answerD = "Egon Schwelb — tschechischer UN-Jurist in Genf",
        correctAnswer = 1,
        explanation = "Raphael Lemkin praegte 1944 das Wort 'Genocide' aus dem griechischen 'genos' (Volk, Stamm) und dem lateinischen 'caedere' (toeten) in seinem Buch 'Axis Rule in Occupied Europe'. Er verlor 49 Familienmitglieder im Holocaust. Er kaeempfte jahrelang alleine bei den UN fuer die Voelkermordkonvention, die am 9. Dezember 1948 verabschiedet wurde — einen Tag vor der Allgemeinen Erklaerung der Menschenrechte.",
        difficulty = 5,
        funFact = "Lemkin starb 1959 verarmt und weitgehend vergessen in New York. An seiner Beerdigung nahmen gerade einmal sieben Menschen teil. Heute gilt er als einer der wichtigsten Rechtstheoretiker des 20. Jahrhunderts."
    ),

    // Frage 18 — Internationaler Strafgerichtshof / Roemer Statut
    Question(
        categoryId = 11,
        questionText = "Der Internationale Strafgerichtshof (IStGH) in Den Haag wurde durch das Roemer Statut gegruendet. Wann trat dieses Statut in Kraft, und welche Grossmacht hat es bis heute nicht ratifiziert?",
        answerA = "1998 — Russland hat es nicht ratifiziert",
        answerB = "2002 — die USA, Russland und China haben es nicht ratifiziert",
        answerC = "2000 — die USA allein haben es nicht ratifiziert",
        answerD = "2002 — China und Indien haben es nicht ratifiziert",
        correctAnswer = 1,
        explanation = "Das Roemer Statut wurde 1998 verabschiedet und trat am 1. Juli 2002 in Kraft. Die USA unterzeichneten es unter Clinton 1998, zogen die Unterschrift aber unter Bush 2002 zurueck. Russland tat dasselbe 2016 nach dem Ukraine-Krieg. China hat es nie unterzeichnet. Damit bleiben drei der fuenf UN-Vetomachte ausserhalb des IStGH.",
        difficulty = 5,
        funFact = "US-Unterstaatssekretaer John Bolton bezeichnete den Rueckzug der USA als 'unsigning' — ein Begriff, den es im Voelkerrecht eigentlich nicht gibt, da Unterzeichnung und Ratifizierung unterschiedliche Rechtsakte sind."
    ),

    // Frage 19 — Magna Carta
    Question(
        categoryId = 11,
        questionText = "Die Magna Carta von 1215 wird als Grundstein des westlichen Rechtssystems gefeiert. Wie viele der urspruenglich 63 Klauseln haben in England bis heute offiziell Gesetzeskraft?",
        answerA = "Alle 63 Klauseln sind noch formal Gesetz",
        answerB = "27 Klauseln wurden als Gewohnheitsrecht anerkannt",
        answerC = "Nur 3 Klauseln gelten noch als aktives Recht",
        answerD = "Keine — sie wurde 1689 durch die Bill of Rights vollstaendig ersetzt",
        correctAnswer = 2,
        explanation = "Von den urspruenglich 63 Klauseln der Magna Carta sind heute nur noch drei in englischem Recht aktiv: Klausel 1 (Freiheit der Kirche), Klausel 9 (Rechte der City of London) und Klausel 29 (Habeas Corpus Grundsatz: kein freier Mann darf ohne ordentliches Gerichtsverfahren inhaftiert werden). Der Rest wurde durch neuere Gesetze aufgehoben oder ersetzt.",
        difficulty = 5,
        funFact = "Die Magna Carta wurde urspruenglich von Papst Innozenz III. fuer ungueltig erklaert und bereits nach 9 Wochen ausser Kraft gesetzt. Erst nach weiteren Konflikten wurde sie im 13. Jahrhundert dauerhaft etabliert."
    ),

    // Frage 20 — Habeas Corpus
    Question(
        categoryId = 11,
        questionText = "Die Habeas-Corpus-Akte von 1679 sicherte das Recht auf Vorfu'hrung vor Gericht. Dieser Schutz wurde aber in der englischen Rechtsgeschichte mehrfach ausgesetzt. Welcher Anlass fuehrte zur laengsten Aussetzung im 19. Jahrhundert?",
        answerA = "Die Napoleonischen Kriege und die Bedrohung durch Frankreich",
        answerB = "Der Irische Aufstand von 1798 und die Bekaempfung der United Irishmen",
        answerC = "Die Chartisten-Bewegung und Arbeiterproteste in den 1840ern",
        answerD = "Die Boxeraufstaende und koloniale Sicherheitsbedenken in Indien",
        correctAnswer = 1,
        explanation = "Der irische Aufstand von 1798 fuehrte zur Aussetzung des Habeas Corpus in Irland, die mehrere Jahrzehnte andauerte. Die britische Regierung suspendierte den Schutz systematisch, um irische Nationalisten und Rebell-Sympathisanten ohne Anklage inhaftieren zu koennen. Diese Aussetzung pragte das irisch-britische Verhaeltnis tiefgreifend.",
        difficulty = 5,
        funFact = "Der lateinische Begriff 'Habeas Corpus' bedeutet woertlich 'Du sollst den Koerper haben' — es geht also darum, dass die gefangene Person koerperlich vor ein Gericht gebracht werden muss."
    ),

    // Frage 21 — Haager Landkriegsordnung
    Question(
        categoryId = 11,
        questionText = "Die Haager Landkriegsordnung von 1907 kodifizierte Kriegsrecht. Welcher Artikel enthielt die sogenannte 'Martens-Klausel', und was bedeutet sie?",
        answerA = "Auch wenn ein Konflikt nicht explizit geregelt ist, bleiben Kombattanten durch Voelkergewohnheitsrecht geschuetzt",
        answerB = "Gefangene muessen immer medizinisch versorgt werden, unabhaengig von ihrer Nationalitaet",
        answerC = "Zivilisten duerfen niemals als menschliche Schutzschilde eingesetzt werden",
        answerD = "Alle Signatarstaaten sind verpflichtet, Kriegsverbrecher ihrem eigenen Gericht zu uebergeben",
        correctAnswer = 0,
        explanation = "Die Martens-Klausel (benannt nach dem russischen Diplomaten Friedrich von Martens) findet sich in der Praeambel der Haager Abkommen. Sie besagt: Auch in Faellen, die nicht durch spezifische Vertragsregeln abgedeckt sind, bleiben Zivilisten und Kombattanten durch Voelkergewohnheitsrecht, humanitaere Prinzipien und das Gewissen der Voelker geschuetzt. Sie schliesst Regelungsluecken im humanitaeren Voelkerrecht.",
        difficulty = 5,
        funFact = "Friedrich von Martens war ein estnisch-russischer Diplomat und Rechtsgelehrter. Er verhandelte die Haager Friedenskonferenzen 1899 und 1907 massgeblich mit und gilt als Gruendervater des modernen humanitaeren Voelkerrechts."
    ),

    // Frage 22 — Roemer Recht / Zwolftafelgesetz
    Question(
        categoryId = 11,
        questionText = "Das roemische Zwolftafelgesetz (Lex Duodecim Tabularum, ca. 450 v.Chr.) ist die aelteste kodifizierte Rechtssammlung des roemischen Rechts. Welche Regelung enthielt es zur Schuldknechtschaft, die spaeter abgeschafft wurde?",
        answerA = "Glaeubiger durften Schuldner nur fuer maximal 60 Tage festhalten",
        answerB = "Ein Schuldner konnte in Stuecke zerschnitten werden, wenn mehrere Glaeubiger Ansprueche hatten",
        answerC = "Schuldner wurden automatisch nach 3 Jahren Knechtschaft frei",
        answerD = "Schuldner durften nicht an Auslaender verkauft werden, nur an roemische Buerger",
        correctAnswer = 1,
        explanation = "Das Zwolftafelgesetz erlaubte tatseachlich, dass ein insolventem Schuldner, wenn mehrere Glaeubiger Ansprueche hatten, diese sich den Koerper buchstaeblich aufteilen durften (Tabula III, Abschnitt 6). Ob dies wirklich praktiziert wurde oder nur als Abschreckung diente, ist historisch umstritten. Die Schuldknechtschaft wurde in Rom 326 v.Chr. durch die Lex Poetelia abgeschafft.",
        difficulty = 5,
        funFact = "Das Zwolftafelgesetz wurde in Roms Hauptplatz, dem Forum Romanum, auf zwolf Bronzetafeln ausgestellt — damit alle Buerger es lesen konnten. Die Originaltafeln wurden bei der Gaulischen Invasion 390 v.Chr. zerstoert."
    ),

    // Frage 23 — Corpus Juris Civilis / Justinian
    Question(
        categoryId = 11,
        questionText = "Das Corpus Juris Civilis unter Kaiser Justinian I. (529-534 n.Chr.) ist die einflussreichste Rechtssammlung der Geschichte. Welcher Teil enthielt die Lehrmeinung von 39 roemischen Rechtsgelehrten in komprimierter Form?",
        answerA = "Codex Justinianus",
        answerB = "Digesten (auch Pandekten genannt)",
        answerC = "Institutiones",
        answerD = "Novellae Constitutiones",
        correctAnswer = 1,
        explanation = "Die Digesten (oder Pandekten), 533 n.Chr. veroeffentlicht, fassen die Meinungen von 39 roemischen Juristen aus fuenf Jahrhunderten zusammen — insgesamt 9.142 Exzerpte aus ueber 1.500 Buechern. Sie bildeten das Herzstu'ck des Corpus Juris Civilis und beeinflussten direkt das europaeische Zivilrecht bis zum 19./20. Jahrhundert. Der Codex Justinianus enthielt kaiserliche Gesetze, die Institutiones waren ein Lehrbuch.",
        difficulty = 5,
        funFact = "Justinian verbot unter Todesstrafe, Kommentare zu den Digesten zu schreiben — er fuerchtete, die Klarheit seines Rechtswerks wuerde durch Gelehrtenstreit verwassert. Trotzdem entstanden zahlreiche heimliche Kommentare."
    ),

    // Frage 24 — Westfaelischer Frieden / Souveraennitaetsprinzip
    Question(
        categoryId = 11,
        questionText = "Der Westfaelische Frieden von 1648 wird oft als Geburtsstunde des modernen Voelkerrechts bezeichnet. Welches Kernprinzip wurde dort erstmals rechtsverbindlich verankert?",
        answerA = "Das Prinzip der Handelsfreiheit zwischen Nationen",
        answerB = "Das Prinzip der staatlichen Souveraenitaet und Nichteinmischung in innere Angelegenheiten",
        answerC = "Das Verbot von Angriffskriegen ohne Billigung des Papstes",
        answerD = "Das Recht auf Selbstbestimmung der Voelker bei Grenzstreitigkeiten",
        correctAnswer = 1,
        explanation = "Der Westfaelische Frieden (Osnabrueck und Muenster, 1648) verankerte das Souveraennitaetsprinzip: Jeder Staat hat das alleinige Recht, seine inneren Angelegenheiten ohne Einmischung anderer Staaten zu regeln. Dieses Prinzip ist bis heute Grundlage der UN-Charta (Art. 2). Es beendete den Dreissigjaehrigen Krieg und schuf das europaeische Staatensystem.",
        difficulty = 5,
        funFact = "Die Friedensverhandlungen dauerten fuenf Jahre (1643-1648). Erstmals nahmen alle europaeischen Maechte an einer multilateralen Friedenskonferenz teil — ein Vorlaufer der modernen UN-Konferenzen."
    ),

    // Frage 25 — Nulla poena sine lege
    Question(
        categoryId = 11,
        questionText = "Das Rechtsprinzip 'Nulla poena sine lege' (keine Strafe ohne Gesetz) wird welchem deutschen Strafrechtler des 18. Jahrhunderts zugeschrieben, der es in seiner heutigen praezisen Form formulierte?",
        answerA = "Georg Friedrich Puchta",
        answerB = "Paul Johann Anselm von Feuerbach",
        answerC = "Karl von Savigny",
        answerD = "Samuel von Pufendorf",
        correctAnswer = 1,
        explanation = "Paul Johann Anselm von Feuerbach (1775-1833) praegte die lateinische Formel in ihrem klassischen Vierteil: 'nulla poena sine lege, nulla poena sine crimine, nullum crimen sine poena legali' — keine Strafe ohne Gesetz, keine Strafe ohne Verbrechen, kein Verbrechen ohne gesetzliche Strafdrohung. Dieser Grundsatz ist heute in Art. 103 II GG und Art. 7 EMRK verankert.",
        difficulty = 5,
        funFact = "Feuerbach war auch der Vater des Philosophen Ludwig Feuerbach und Pflegevater von Kaspar Hauser — jenem Raetselkind, das 1828 spurlos in Nuernberg auftauchte und behauptete, nie die Sonne gesehen zu haben."
    ),

    // Frage 26 — Haager Tribunal / Ex-Jugoslawien
    Question(
        categoryId = 11,
        questionText = "Der Internationale Strafgerichtshof fuer das ehemalige Jugoslawien (ICTY) verhandelte den Fall gegen Slobodan Milosevic. Wie endete dieser historisch bedeutsame Prozess?",
        answerA = "Milosevic wurde zu lebenslanger Haft verurteilt",
        answerB = "Milosevic starb 2006 in Untersuchungshaft vor Urteilsverkuendung",
        answerC = "Milosevic wurde aus Mangel an Beweisen freigesprochen",
        answerD = "Milosevic entkam durch Tausch gegen serbische Geiseln in die Russland",
        correctAnswer = 1,
        explanation = "Slobodan Milosevic starb am 11. Maerz 2006 in seiner Zelle im ICTY-Gefaengnis Scheveningen (Den Haag) an Herzversagen — kurz bevor das Urteil gesprochen werden sollte. Der Prozess hatte seit 2002 gedauert; es war das laengste Strafverfahren in der Geschichte des Voelkerstrafrechts. Postum stellte das Gericht 2016 fest, er trage zumindest Teilschuld fuer die Verbrechen.",
        difficulty = 5,
        funFact = "Milosevic verteidigte sich selbst (ohne Anwalt) — ein Recht, das das Statut erlaubte. Diese Selbstverteidigung fuehrte zu endlosen Prozesstagen und komplizierte das Verfahren erheblich."
    ),

    // Frage 27 — Radbruch'sche Formel
    Question(
        categoryId = 11,
        questionText = "Gustav Radbruchs Formel von 1946 loeste ein zentrales rechtsphilosophisches Problem nach dem NS-Regime. Was besagt sie in ihrem Kerngehalt?",
        answerA = "Gerechtigkeitsgebot geht ueber Positivismus: Extrem ungerechtes Recht verliert seinen Rechtscharakter",
        answerB = "Naturrecht ist dem positiven Recht generell uebergeordnet",
        answerC = "Richter duerfen einzelne Gesetze ablehnen, wenn ihr persoenliches Gewissen es verlangt",
        answerD = "Kein Staat darf Gesetze rueckwirkend anwenden, ohne dass ein ueberstaatlicher Gerichtshof zustimmt",
        correctAnswer = 0,
        explanation = "Radbruchs Formel lautet sinngemass: Wenn positives Recht ein unertraegliches Mass an Ungerechtigkeit erreicht oder bewusst auf die Verleugnung von Menschenwuerde abzielt, muss es dem Gerechtigkeitsprinzip weichen und verliert seinen Charakter als Recht ('gesetzliches Unrecht'). Diese Formel wurde vom Bundesgerichtshof und Bundesverfassungsgericht mehrfach angewandt — u.a. bei der Beurteilung von DDR-Mauerschuetzen.",
        difficulty = 5,
        funFact = "Radbruch war vor 1933 selbst Rechtspositivist — er glaubte, Richter sollten Gesetzen auch ohne Moralurteil folgen. Das NS-Regime liess ihn seine Meinung grundlegend aendern."
    ),

    // Frage 28 — Mauerschuetzenprozesse
    Question(
        categoryId = 11,
        questionText = "Bei den Mauerschuetzenprozessen der 1990er wurde die Radbruch'sche Formel angewandt. Welche Argumentation brachten die Angeklagten DDR-Grenzsoldaten zu ihrer Verteidigung vor?",
        answerA = "Sie hatten Befehlsnotstand und drohende Erschiessung bei Nichtbefolgung",
        answerB = "Sie handelten nach gueltigem DDR-Recht, das ihnen kein Unrecht vorwarf — Befehlsgehorsam",
        answerC = "Der Tatbestand war bei Wiedervereinigung verjahrt",
        answerD = "Die Bundesrepublik Deutschland hatte keine Jurisdiction ueber DDR-Staatshandeln",
        correctAnswer = 1,
        explanation = "Die Verteidigung argumentierte: Die Soldaten handelten nach gueltigem DDR-Recht (Grenzgesetz 1982) und konnten nicht fuer legal befohlenene Handlungen bestraft werden. Der BGH verwarf dies mit Radbruchs Formel: DDR-Grenzrecht war 'gesetzliches Unrecht' — es verletzte elementare Gebote der Menschlichkeit und verlor damit seinen Rechtscharakter. Die Verurteilungen standen.",
        difficulty = 5,
        funFact = "Erich Honecker selbst entging einer Verurteilung: Er war bei Prozessbeginn unheilbar an Leberkrebs erkrankt und floh 1993 nach Chile, wo er 1994 starb. Das Verfahren gegen ihn wurde aus humanitaeren Gruenden eingestellt."
    ),

    // Frage 29 — Sterbehilfe BVerfG 2020
    Question(
        categoryId = 11,
        questionText = "Das BVerfG erklaerte 2020 das Verbot der geschaeftsmaessigen Sterbehilfe (par. 217 StGB) fuer verfassungswidrig. Auf welchem Grundrecht gruendete das Gericht dieses Recht auf selbstbestimmten Tod?",
        answerA = "Art. 2 Abs. 2 GG — Recht auf Leben und koerperliche Unversehrtheit",
        answerB = "Art. 1 Abs. 1 GG in Verbindung mit Art. 2 Abs. 1 GG — Menschenwuerde und allgemeines Persoenlichkeitsrecht",
        answerC = "Art. 4 GG — Glaubens- und Gewissensfreiheit",
        answerD = "Art. 3 Abs. 3 GG — Benachteiligungsverbot bei Behinderung",
        correctAnswer = 1,
        explanation = "Das BVerfG leitete das Recht auf selbstbestimmten Tod aus dem allgemeinen Persoenlichkeitsrecht (Art. 2 Abs. 1 GG) in Verbindung mit der Menschenwuerde (Art. 1 Abs. 1 GG) ab. Die autonome Entscheidung ueber das eigene Sterben ist danach Ausdruck hoechstpersoenlicher Selbstbestimmung. Der Gesetzgeber muss seitdem rechtliche Rahmenbedingungen schaffen, die dieses Recht ermoglichen.",
        difficulty = 5
    ),

    // Frage 30 — Solvay-Konferenz / Sterbehilfe-Rahmenbedingungen
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde in Deutschland erstmals ein Gesetz ueber die 'Verfolgung Gewerbsmaessiger Beihilfe zum Selbstmord' (par. 216 StGB, 'Toetung auf Verlangen') erlassen, das von den Nazis erheblich verscharft wurde?",
        answerA = "1869 im Norddeutschen Bund",
        answerB = "1871 im Deutschen Reich",
        answerC = "1900 als Reichsgericht-Entscheidung",
        answerD = "1933 im Nationalsozialismus als neue Strafnorm",
        correctAnswer = 1,
        explanation = "Paragraph 216 StGB (Toetung auf Verlangen) entstammt dem Strafgesetzbuch des Deutschen Reichs von 1871. Er verbietet die Toetung einer Person auch dann, wenn diese ausdruecklich und ernsthaft darum bittet — und ist bis heute in Kraft. Die Nationalsozialisten verschaerften den Strafrechtsrahmen tatsaechlich, fuehrten aber parallel das Euthanasie-Programm T4 durch — ein paradoxer Widerspruch.",
        difficulty = 5
    ),

    // Frage 31 — Tschad/Libyen am IGH
    Question(
        categoryId = 11,
        questionText = "Im Aouzou-Streifen-Fall (Tschad gegen Libyen, 1994) entschied der IGH ueber einen Grenzstreit. Welches historische Dokument nutzte der IGH als Grundlage fuer seine Grenzziehung?",
        answerA = "Der Berliner Kongoakt von 1885",
        answerB = "Ein franzoesisch-libyscher Freundschaftsvertrag aus dem Jahr 1955",
        answerC = "Die Volkerbundsmandat-Urkunde von 1922",
        answerD = "Der Vertrag von Sevres von 1920",
        correctAnswer = 1,
        explanation = "Der IGH stuetzte sich auf den franzoesisch-libyschen Freundschaftsvertrag von 1955, der seinerseits auf aelteren franzoesisch-osmanischen Abkommen basierte. Der Aouzou-Streifen (ca. 114.000 km²) wurde Tschad zugesprochen. Libyen zog sich 1994 zurueck. Dies war ein seltenes Beispiel, bei dem ein arabischer Staat ein IGH-Urteil vollstaendig akzeptierte.",
        difficulty = 5,
        funFact = "Muammar al-Gaddafi hatte den Aouzou-Streifen 1973 einfach annektiert — ohne Krieg zu erklaeren, einfach durch Einmarsch. Der IGH-Prozess dauerte dann fast 20 Jahre."
    ),

    // Frage 32 — Rechtsinstitut der Immunitat
    Question(
        categoryId = 11,
        questionText = "Im Fall Kongo gegen Belgien (Haftbefehl-Fall, 2002) entschied der IGH ueber die Immunitat eines Aussenministers. Was war das Kernresultat fuer das Voelkerrecht?",
        answerA = "Ein amtierender Aussenminister geniesse absolute persoenliche Immunitat vor auslaendischer Strafverfolgung",
        answerB = "Voelkermord hebt die Immunitat des Aussenministers auch waehrend der Amtszeit auf",
        answerC = "Nationale Gerichte koennen Aussenminister bei Kriegsverbrechen anklagen, nicht aber fuer Voelkermord",
        answerD = "Nur der IStGH, nicht nationale Gerichte, darf Amtstraeger anklagen",
        correctAnswer = 0,
        explanation = "Der IGH entschied: Der kongolesische Aussenminister Yerodia Ndombasi geniesse als amtierender Minister absolute Immunitat vor auslaendischer (belgischer) Strafverfolgung — selbst bei Verdacht auf Verbrechen gegen die Menschlichkeit. Die Immunitat endet erst nach dem Ausscheiden aus dem Amt. Belgien musste den internationalen Haftbefehl zurueckziehen.",
        difficulty = 5,
        funFact = "Belgien hatte sich zum 'Weltgericht' erklaert und begann, fuer Verbrechen weltweit Haftbefehle auszustellen. Nach dem IGH-Urteil und US-amerikanischem Druck musste Belgien sein Weltrechtsprinzip-Gesetz deutlich einschraenken."
    ),

    // Frage 33 — Reparationsforderungen nach WW1
    Question(
        categoryId = 11,
        questionText = "Deutschland zahlte Reparationen aus dem Versailler Vertrag von 1919 ueber Jahrzehnte. In welchem Jahr leistete Deutschland die letzte Zahlung — und welcher besondere Umstand gab ihr historische Bedeutung?",
        answerA = "1953 — im Rahmen des Londoner Schuldenabkommens nach dem Wirtschaftswunder",
        answerB = "1983 — waehrend der Regierung Kohl als finaler Schuldentilgungsakt",
        answerC = "2010 — als letzte Rate auf ein Anleihepaket, das durch die Wiedervereinigung faellig wurde",
        answerD = "1990 — mit dem Zwei-plus-Vier-Vertrag wurden alle Reparationsansprueche abgegolten",
        correctAnswer = 2,
        explanation = "Deutschland leistete die allerletzte Zahlung aus dem Ersten Weltkrieg am 3. Oktober 2010 — am 20. Jahrestag der Deutschen Einheit. Es handelte sich um 69,9 Millionen Euro auf Obligationen aus dem Dawes-Plan von 1924, die durch das Londoner Schuldenabkommen von 1953 gestundet worden waren. Diese abschliessende Zahlung beendete nach 92 Jahren die Verpflichtungen aus dem Versailler Vertrag.",
        difficulty = 5,
        funFact = "Die urspruengliche Summe betrug 132 Milliarden Goldmark — ein astronomischer Betrag, der nach heutigem Wert schaetzungsweise 440 Milliarden Euro entsprechen wuerde."
    ),

    // Frage 34 — UN-Sicherheitsrat Veto
    Question(
        categoryId = 11,
        questionText = "Das Vetorecht im UN-Sicherheitsrat ist seit 1945 umstritten. Welches Land hat das Vetorecht am haeufigsten eingesetzt (Stand 2025)?",
        answerA = "USA",
        answerB = "Russland (fruher Sowjetunion)",
        answerC = "China",
        answerD = "Grossbritannien",
        correctAnswer = 1,
        explanation = "Russland (bzw. die Sowjetunion) hat das Vetorecht bei weitem am haeufigsten eingesetzt — ueber 120 Mal seit 1946. Die USA folgen mit ca. 80 Vetos auf Platz 2. China hat erst seit den 1970ern verstaerkt Gebrauch davon gemacht. Das erste Veto der Sowjetunion galt bereits 1946 einer Resolution ueber Syrien.",
        difficulty = 5,
        funFact = "In den ersten Jahren boykottierte die Sowjetunion den Sicherheitsrat monatelang aus Protest gegen die Nichtzulassung Chinas (damals Republik China/Taiwan). Dadurch konnte die UN den Korea-Krieg 1950 ohne Veto beschliessen."
    ),

    // Frage 35 — Casablanca / Genfer Konventionen
    Question(
        categoryId = 11,
        questionText = "Die Genfer Konventionen wurden mehrfach revidiert. Welche Konvention von 1977 erweiterte den Schutz erstmals auf nicht-internationale (innere) Konflikte als verbindliches Voelkerrecht?",
        answerA = "Das dritte Genfer Abkommen zum Schutz von Kriegsgefangenen",
        answerB = "Zusatzprotokoll II zu den Genfer Abkommen",
        answerC = "Die Haager Konvention IV",
        answerD = "Das Roemer Statut von 1998",
        correctAnswer = 1,
        explanation = "Das Zusatzprotokoll II (AP II) von 1977 war ein historischer Schritt: Erstmals wurden innere bewaffnete Konflikte (Buergerkriege) umfassend unter den humanitaerrechtlichen Schutz gestellt. Vorher galt internationales humanitaeres Recht fast ausschliesslich fuer Konflikte zwischen Staaten. AP II schuetzt Zivilisten auch wenn ein Staat gegen seine eigenen Buerger vorgeht.",
        difficulty = 5,
        funFact = "Die Entwicklung von AP II wurde massgeblich von Yves Sandoz vom IKRK vorangetrieben. Bis heute haben nicht alle Staaten AP II ratifiziert — die USA beispielsweise haben es bis 2025 nicht ratifiziert."
    ),

    // Frage 36 — Siebner-Ausschuss / Kinderhilfe
    Question(
        categoryId = 11,
        questionText = "Die UN-Kinderrechtskonvention von 1989 ist der am meisten ratifizierte Menschenrechtsvertrag der Geschichte. Welches einzige Land hat sie bis heute NICHT ratifiziert (Stand 2025)?",
        answerA = "Nordkorea",
        answerB = "USA",
        answerC = "Saudi-Arabien",
        answerD = "Sudan",
        correctAnswer = 1,
        explanation = "Die USA sind das einzige Land der Welt, das die UN-Kinderrechtskonvention nicht ratifiziert hat — trotz Unterzeichnung 1995. Als Gruende werden foederale Zustaendigkeitskonflikte und Bedenken wegen des Jugendstrafrechts (u.a. lebenslange Haft fuer Jugendliche) angefuehrt. Somalia und Sued-Sudan haben sie nach langem Zogern ratifiziert, womit die USA als einziger Nichtratifikant verblieben.",
        difficulty = 5,
        funFact = "Kein anderes Menschenrechtsdokument hatte je so viele Ratifikationen — 196 von 197 anerkannten Staaten. Die Ironie ist, dass die USA massgeblich an der Formulierung der Konvention mitgewirkt haben."
    ),

    // Frage 37 — Voelkermord an den Armeniern / Rechtsstatus
    Question(
        categoryId = 11,
        questionText = "Der Deutsche Bundestag erkannte den Voelkermord an den Armeniern 2016 offiziell an. Welche juristische Besonderheit hat diese Anerkennung im Voelkerrecht, da sie sich auf Ereignisse von 1915-1923 bezieht?",
        answerA = "Die Voelkermordkonvention gilt nur fuer Handlungen ab 1948 — keine rueckwirkende Strafverfolgung moeglich",
        answerB = "Die Anerkennung ist rechtlich bedeutungslos, da das Osmanische Reich nicht mehr existiert",
        answerC = "Die Tuerkei als Rechtsnachfolger muss entschaedigen, was aber ein IGH-Urteil erfordern wuerde",
        answerD = "Die Anerkennung wirkt sich auf alle EU-Beitrittsverhandlungen aus",
        correctAnswer = 0,
        explanation = "Die Voelkermordkonvention von 1948 gilt nicht rueckwirkend — Ereignisse vor ihrem Inkrafttreten koennen nicht strafrechtlich verfolgt werden. Die Bundestagsresolution ist daher eine moralisch-politische, keine rechtlich verpflichtende Anerkennung. Die Tuerkei bestreitet die Qualifikation als Voelkermord; eine Strafverfolgung vor dem IStGH ist rechtlich ausgeschlossen.",
        difficulty = 5,
        funFact = "Raphael Lemkin, der Erfinder des Begriffs Genozid, nannte den Voelkermord an den Armeniern ausdruecklich als historisches Beispiel, das sein Konzept gepraegt hatte — noch bevor er das Wort praegte."
    ),

    // Frage 38 — Grenze / ICJ Beratungsgutachten
    Question(
        categoryId = 11,
        questionText = "2010 gab der IGH ein wegweisendes Beratungsgutachten zur Unabhaengigkeitserklaerung des Kosovo ab. Welches Rechtsprinzip wurde dabei im Mittelpunkt diskutiert?",
        answerA = "Ausnahmsloser Vorrang des Selbstbestimmungsrechts der Voelker vor der territorialen Integritaet",
        answerB = "Einseitige Unabhaengigkeitserklaerungen sind nach Voelkerrecht nicht per se verboten",
        answerC = "Ein Volk darf sich nur mit UN-Sicherheitsratszustimmung von einem Staat trennen",
        answerD = "Das Selbstbestimmungsrecht gilt nur fuer ehemals kolonisierte Voelker",
        correctAnswer = 1,
        explanation = "Der IGH erkaerte: Die einseitige Unabhaengigkeitserklaerung des Kosovo vom 17. Februar 2008 verstoss nicht gegen das Voelkerrecht. Das Gericht stellte klar, dass Voelkerrecht kein allgemeines Verbot einseitiger Unabhaengigkeitserklaerungen kennt. Das Gutachten vermied jedoch eine Aussage, ob Kosovo ein Recht auf Sezession hatte — eine bewusste rechtliche Luecke.",
        difficulty = 5,
        funFact = "22 Richter befassten sich mit dem Fall; keiner hatte Richter aus dem Bereich des Ex-Jugoslawien-Konflikts. Das Gutachten hatte 10:4 Stimmen — und fuehrte trotzdem zu keiner einheitlichen internationalen Anerkennung Kosovos."
    ),

    // Frage 39 — US-Verfassung / Marbury v. Madison
    Question(
        categoryId = 11,
        questionText = "Der US-Supreme-Court-Fall 'Marbury v. Madison' (1803) gilt als Mutter aller Verfassungsgerichtsentscheidungen. Was begrundete Chief Justice John Marshall damit?",
        answerA = "Das Recht der Buerger auf Habeas Corpus auch gegen Bundesbehorden",
        answerB = "Das richterliche Pruefungsrecht (Judicial Review) — Gerichte koennen Gesetze fuer verfassungswidrig erklaeren",
        answerC = "Die Unabhaengigkeit der Bundesrichter von politischen Weisungen",
        answerD = "Das Vorrang-Prinzip von Bundesrecht gegenueber Staatsrecht",
        correctAnswer = 1,
        explanation = "Chief Justice Marshall begrundete das 'Judicial Review': Das Oberste Gericht kann Gesetze des Kongresses fuer verfassungswidrig erklaeren und damit nichtig. Paradoxerweise entschied Marshall gegen den Klaeger Marbury — um diese maechtiger Kompetenz fuer das Gericht zu gewinnen. Dieses Prinzip ist heute Grundlage aller Verfassungsgerichte weltweit, stand jedoch nicht ausdruecklich in der US-Verfassung.",
        difficulty = 5,
        funFact = "Marshall entschied gegen den Klaeger, obwohl er selbst als Aussenminister unter Adams dafuer verantwortlich war, dass Marburys Ernennungsurkunde nicht ausgefertigt worden war. Er haette den Fall eigentlich ablehnen muessen."
    ),

    // Frage 40 — Sondergerichte NS-Zeit / Freisler-Zahlen
    Question(
        categoryId = 11,
        questionText = "Unter Roland Freislers Praesidentschaft des Volksgerichtshofs (1942-1945) stieg die Verurteilungsrate dramatisch. Auf welchen Prozentsatz stieg die Todesurteilsrate im Vergleich zur Zeit davor an?",
        answerA = "Von 15% auf 35%",
        answerB = "Von 5% auf 46%",
        answerC = "Von 22% auf 68%",
        answerD = "Von 10% auf 83%",
        correctAnswer = 1,
        explanation = "Die Todesurteilsrate am Volksgerichtshof stieg unter Freisler von etwa 5% auf 46% aller Urteile. Das Gericht wurde von einem Instrument der politischen Justiz zu einem regelrechten Todesurteilsautomaten. Zwischen 1942 und 1945 wurden mehr Todesurteile gesprochen als in allen Jahren davor zusammen.",
        difficulty = 5,
        funFact = "Freisler beleidigte und demuetigte Angeklagte systematisch — er liess ihnen die Hosen wegnehmen, damit sie in der Verhandlung nicht aufrecht stehen konnten. Hitler sah sich Filmaufnahmen der Verhandlungen gegen die Attentaeter des 20. Juli begeistert an."
    ),

    // Frage 41 — Apartheid / Sharpeville
    Question(
        categoryId = 11,
        questionText = "Das Sharpeville-Massaker von 1960 in Suedafrika hatte eine bedeutsame voelkerrechtliche Konsequenz bei der UN. Was wurde als direkte Folge im UN-Sicherheitsrat beschlossen?",
        answerA = "Resolution 134: erste offizielle UN-Verurteilung der Apartheid als 'gefaehrdend fuer internationalen Frieden'",
        answerB = "Der sofortige Ausschluss Suedafrikas aus der UN-Vollversammlung",
        answerC = "Ein verbindliches Waffenembargo gegen Suedafrika",
        answerD = "Die Einsetzung einer UN-Untersuchungskommission mit erzwungenen Besuchsrechten",
        correctAnswer = 0,
        explanation = "UN-Resolution 134 vom 1. April 1960 war ein Meilenstein: erstmals erklaerte der Sicherheitsrat Apartheid als potenzielle Bedrohung des internationalen Friedens und forderte Suedafrika zur Aufgabe auf. Das verbindliche Waffenembargo folgte erst 1977 (Resolution 418). Das Massaker, bei dem 69 Menschen erschossen wurden, gab dem Anti-Apartheid-Kampf weltweiten Auftrieb.",
        difficulty = 5,
        funFact = "Suedafrika wurde 1974 aus der UN-Vollversammlung ausgeschlossen (Aussetzung der Stimmrechte) — eine faktische Isolation. Erst nach Mandelas Freilassung 1990 und Ende der Apartheid wurde die Vollmitgliedschaft wiederhergestellt."
    ),

    // Frage 42 — Internationaler Gerichtshof / Erstfall Korfu
    Question(
        categoryId = 11,
        questionText = "Der erste Fall, ueber den der IGH 1949 ein Urteil faellte, betraf den Korfu-Kanal. Worum ging es, und welches Novum brachte das Urteil fuer das Voelkerrecht?",
        answerA = "Grossbritannien klagte gegen Albanien wegen Minenlegen im Korfu-Kanal; Urteil begrundete Staatshaftung fuer Schaeden auf eigenem Territorium durch Dritte",
        answerB = "Griechenland klagte gegen Albanien wegen Verletzung griechischer Hoheitsgewaesser; Urteil definierte Archipelgewasser",
        answerC = "USA klagte gegen Albanien wegen Hinderung von Handelsschiffen; Urteil begrundete Schiffahrtsfreiheit",
        answerD = "Grossbritannien klagte gegen Jugoslawien; Urteil erkannte erstmals das Minenlegen als Kriegshandlung an",
        correctAnswer = 0,
        explanation = "Grossbritannien klagte gegen Albanien, nachdem britische Kriegsschiffe im albanischen Korfu-Kanal 1946 auf Minen liefen und 44 Seeleute starben. Der IGH urteilte: Albanien wusste von den Minen und hatte eine Warnpflicht — und muss Schadensersatz leisten. Das Urteil begrundete das Prinzip, dass Staaten fuer auf ihrem Territorium entstehende Schaeden an anderen Staaten haftbar sein koennen.",
        difficulty = 5,
        funFact = "Albanien weigerte sich, den zugesprochenen Schadensersatz zu zahlen. Grossbritannien 'confiscated' daraufhin albanisches Gold, das waehrend des Zweiten Weltkriegs zur Verwahrung nach England gebracht worden war. Dieser Streit dauerte Jahrzehnte."
    ),

    // Frage 43 — Frieden von Westfalen / erste Botschafter
    Question(
        categoryId = 11,
        questionText = "Das moderne Botschafterwesen basiert auf der Wiener Konvention ueber diplomatische Beziehungen von 1961. Aber wann wurde das Konzept der staendigen Botschaftsresidenz erstmals etabliert?",
        answerA = "Im 13. Jahrhundert durch die Hansestaedte im Ostseeraum",
        answerB = "Im 15. Jahrhundert durch die Stadtstaaten Norditaliens (Florenz, Venedig, Mailand)",
        answerC = "Im 17. Jahrhundert nach dem Westfaelischen Frieden von 1648",
        answerD = "Im 18. Jahrhundert durch das Diplomatisches Protokoll Ludwigs XIV.",
        correctAnswer = 1,
        explanation = "Die norditalienischen Stadtstaaten des 15. Jahrhunderts — besonders Mailand, Venedig und Florenz — entwickelten als erste das System der staendigen Botschafter ('ambasciatori residenti'). Venedig galt dabei als Pionierin. Vor dieser Zeit schickte man nur Gesandte fuer spezifische Missionen. Das System verbreitete sich im 16. und 17. Jahrhundert ueber ganz Europa.",
        difficulty = 5,
        funFact = "Venedig hatte ein geheimes System: Botschafter mussten nach Rueckkehr detaillierte vertrauliche Berichte ueber das Gastland abliefern ('relazioni'). Diese Dokumente sind heute wichtige Quellen fuer Historiker."
    ),

    // Frage 44 — War Crimes / Comando-Befehl
    Question(
        categoryId = 11,
        questionText = "Hitlers Kommando-Befehl von 1942 ordnete an, alliierte Kommandos auch wenn sie in Uniform waren sofort zu toeten. Nach welchem Rechtsprinzip wurden NS-Offiziere dafuer in Nuernberg verurteilt?",
        answerA = "Das Kriegsrecht verbietet Toetung von Kombattanten in Uniform nach ihrer Gefangennahme",
        answerB = "Der Befehl war schriftlich dokumentiert, was den Vorsatz bewies",
        answerC = "Es handelte sich um Toetungen ausserhalb des Kampfes, also um Mord ohne Kriegsnotwenigkeit",
        answerD = "Alle drei Antworten A, B und C sind korrekt",
        correctAnswer = 0,
        explanation = "Das fundamentale Kriegsrechtsprinzip: Kombattanten (Soldaten in Uniform), die sich ergeben oder gefangengenommen werden, haben sofort den Status als Kriegsgefangene nach Genfer Konvention — sie duerfen nicht getoetet werden. Hitlers Kommando-Befehl (Kommandobefehl) verstoss direkt dagegen. Offiziere, die ihn ausfuehrten, wurden als Kriegsverbrecher verurteilt.",
        difficulty = 5
    ),

    // Frage 45 — Amnesty International / Gewissensgefangene
    Question(
        categoryId = 11,
        questionText = "Amnesty International wurde 1961 vom britischen Anwalt Peter Benenson gegruendet. Was war der konkrete Anlass fuer seine Gruendung?",
        answerA = "Die Hinrichtung von Rosenbergs in den USA 1953 und sein jahrelanges Engagement dagegen",
        answerB = "Ein Zeitungsartikel ueber zwei portugiesische Studenten, die zu 7 Jahren Gefaengnis verurteilt wurden, weil sie auf die Freiheit getrunken hatten",
        answerC = "Die Entdeckung von KGB-Folterpraktiken gegen politische Gefangene in der Sowjetunion",
        answerD = "Die Hinrichtung von Patrice Lumumba im Kongo 1961 ohne ordentliches Gerichtsverfahren",
        correctAnswer = 1,
        explanation = "Peter Benenson las 1960 in der Londoner U-Bahn einen Zeitungsartikel: Zwei portugiesische Studenten waren zu sieben Jahren Gefaengnis verurteilt worden, weil sie in einem Restaurant auf die Freiheit getrunken hatten. Benenson schrieb einen Artikel in The Observer ('The Forgotten Prisoners'), der eine Massenbewegung ausloeste. Amnesty International wurde am 28. Mai 1961 offiziell gegruendet.",
        difficulty = 5,
        funFact = "Benenson war ein bekehrter Sozialist und Quaeker. Er versaehnte sich spaeter mit der Idee, dass Amnesty von britischen Geheimdiensten beeinflusst sei — eine Kontroverse, die seine Rolle bei der Organisation belastete."
    ),

    // Frage 46 — Europaeischer Gerichtshof fuer Menschenrechte
    Question(
        categoryId = 11,
        questionText = "Der Europaeische Gerichtshof fuer Menschenrechte (EGMR) in Strassburg ist kein EU-Organ, sondern Teil des Europarats. Wie viele Mitgliedsstaaten sind dem Europarat und damit der EMRK unterworfen (Stand 2025)?",
        answerA = "27 (alle EU-Mitglieder)",
        answerB = "46 (Russland wurde 2022 ausgeschlossen, Belarus nie aufgenommen)",
        answerC = "44 (einschliesslich Tuerkei und Aserbaidschan)",
        answerD = "50 (einschliesslich USA als Beobachter mit eingeschraenkter Unterwerfung)",
        correctAnswer = 1,
        explanation = "Der Europarat hat 46 Mitgliedsstaaten (Stand 2025). Russland wurde am 16. Maerz 2022 nach dem Einmarsch in die Ukraine ausgeschlossen — das erste Mal in der Geschichte des Europarats, dass ein Mitglied ausgeschlossen wurde. Belarus hat nie eine Mitgliedschaft gehabt. Die Tuerkei und Aserbaidschan sind weiterhin Mitglieder.",
        difficulty = 5,
        funFact = "Der EGMR ist das einzige internationale Gericht der Welt, bei dem Einzelpersonen (nicht nur Staaten) Klagen gegen Staaten einreichen koennen. Er hat seit seiner Gruendung mehr als 25.000 Urteile gefaellt."
    ),

    // Frage 47 — Piraten / Voelkerrecht
    Question(
        categoryId = 11,
        questionText = "Seepiraterie gilt voelkerrechtlich als 'hostis humani generis' (Feind der ganzen Menschheit). Was bedeutet dies juristisch fuer die Strafverfolgung?",
        answerA = "Seeraeuber haben keinerlei Rechte und koennen auf hoher See sofort erschossen werden",
        answerB = "Jeder Staat der Welt ist berechtigt, Seeraeuber auf Hoher See festzunehmen und nach eigenem Recht zu verfolgen (universelle Jurisdiktion)",
        answerC = "Nur Seefahrtsnationen duerfen Seeraeuber verfolgen; Binnenstaten haben keine Zustaendigkeit",
        answerD = "Seepiraterie wird ausschliesslich durch den IStGH verfolgt",
        correctAnswer = 1,
        explanation = "Piraterie auf Hoher See ist einer der wenigen Faelle, in denen das Voelkerrecht universelle Jurisdiktion gewaehrt: Jeder Staat darf, unabhaengig von Nationalitaet der Taeter oder Opfer, Piraten festnehmen und nach seinem nationalen Recht verurteilen. Diese Regel findet sich in der UN-Seerechtskonvention (Art. 105 UNCLOS) und gilt seit Jahrhunderten als Gewohnheitsvoelkerrecht.",
        difficulty = 5,
        funFact = "Grotius praegte in 'Mare Liberum' (1609) das Prinzip der Freiheit der Meere. Piraterie war das erste internationale Verbrechen mit universeller Jurisdiktion — noch vor Voelkermord, Kriegsverbrechen und Verbrechen gegen die Menschlichkeit."
    ),

    // Frage 48 — Folterverbot / Jus Cogens
    Question(
        categoryId = 11,
        questionText = "Das absolute Folterverbot gilt im Voelkerrecht als 'Jus Cogens' — zwingendes Recht, von dem kein Staat abweichen darf. Welche praktische Konsequenz hat dieser Status fuer bilaterale Vertraege?",
        answerA = "Kein Staat darf mit einem anderen Staat einen Vertrag schliessen, der Folter erlaubt",
        answerB = "Das Folterverbot kann durch nationalen Notstandszustand (Art. 15 EMRK) zeitweise ausgesetzt werden",
        answerC = "Folter-Informationen sind vor Gericht verwertbar, wenn das Leben vieler Menschen gerettet wird",
        answerD = "Das Verbot gilt nur fuer staatliche Akteure, nicht fuer nichtstaatliche bewaffnete Gruppen",
        correctAnswer = 0,
        explanation = "Als Jus Cogens kann kein Staat durch bilateralen Vertrag das Folterverbot beseitigen oder einschraenken — solche Vertrage waeren von Anfang an nichtig. Anders als bei den meisten Menschenrechten (EMRK Art. 15 erlaubt Einschraenkungen im Notstand) ist das Folterverbot aus Art. 3 EMRK ABSOLUT — keine Ausnahme ist erlaubt, auch nicht bei Terrorverdaechtigen.",
        difficulty = 5,
        funFact = "Der Europaeische Gerichtshof fuer Menschenrechte entschied im Fall 'Gaeefgen gegen Deutschland' (2010), dass selbst die Drohung mit Folter (ohne tatsaechliche Folter) Art. 3 EMRK verletzt — auch wenn sie ein Kinderleben retten sollte."
    ),

    // Frage 49 — Belagerung von Sarajevo / ICTY-Urteil
    Question(
        categoryId = 11,
        questionText = "Die Belagerung von Sarajevo (1992-1996) war die laengste Belagerung einer Hauptstadt in der Geschichte moderner Kriegsfuehrung. Welcher bosnisch-serbische General wurde dafuer vom ICTY zu einer historisch hohen Strafe verurteilt?",
        answerA = "Ratko Mladic — zu lebenslanger Haft verurteilt (2017)",
        answerB = "Radovan Karadzic — zu 40 Jahren Haft verurteilt (2016), spaeter zu lebenslang verschaerft",
        answerC = "Dragomir Milosevic — zu 29 Jahren Haft fuer die Belagerung von Sarajevo speziell",
        answerD = "Momcilo Perisic — zu 27 Jahren Haft als Befehlshaber der serbischen Armee",
        correctAnswer = 0,
        explanation = "Ratko Mladic wurde 2017 vom ICTY zu lebenslanger Haft verurteilt — fuer Voelkermord (Srebrenica-Massaker), Verbrechen gegen die Menschlichkeit und Kriegsverbrechen, einschliesslich der Belagerung Sarajevos. Karadzic wurde ebenfalls zu lebenslanger Haft verurteilt (2019 vom Berufungsgericht verschaerft von 40 auf lebenslang). Beide Urteile gelten als Meilensteine des Voelkerstrafrechts.",
        difficulty = 5,
        funFact = "Mladic war 16 Jahre auf der Flucht, bevor er 2011 in Serbien gefasst wurde. Waehrend seiner Zeit im Untergrund besuchte er regelmaessig Fussballspiele des FC Partizan Belgrad — offenbar ohne erkannt zu werden."
    ),

    // Frage 50 — Europaeische Menschenrechtskonvention / Artikel 3
    Question(
        categoryId = 11,
        questionText = "Der Fall 'Ireland v. United Kingdom' (1978) vor dem EGMR behandelte britische Verhoermethoden in Nordirland. Was entschied der Gerichtshof ueber den Unterschied zwischen Folter und unmenschlicher Behandlung?",
        answerA = "Das Gericht erklarte alle fuenf Verhoertechniken als Folter und verurteilte Grossbritannien nach Art. 3 EMRK",
        answerB = "Das Gericht unterschied: Die fuenf Techniken (Kapuze, Laerm, Schlaf-/Nahrungsentzug, Wandstehen) seien unmenschliche Behandlung, nicht Folter — Folter erfordere besondere Schwere",
        answerC = "Das Gericht sprach Grossbritannien frei, da nationale Sicherheit Einschraenkungen erlaubt",
        answerD = "Das Gericht definierte Folter als zweckgebundene Handlung — Gestaendnisentzwingen geniesse keinen Schutz nach Art. 3",
        correctAnswer = 1,
        explanation = "Der EGMR unterschied 1978 zwischen 'Folter' (requires besondere Grausamkeit/Intensitaet) und 'unmenschlicher oder erniedrigender Behandlung' — und ordnete die britischen fuenf Verhoertechniken (Wandstehen, Kapuze, weisses Laerm, Schlaf-/Nahrungsentzug) als letzteres ein. Irland beantragte 2014 eine Wiederaufnahme; 2018 entschied der EGMR ueberraschend, das Urteil bleibt bestehen trotz neu aufgetauchter Beweise.",
        difficulty = 5,
        funFact = "Die fuenf Techniken wurden spaeter als Vorlage fuer US-Verhoermethoden in Abu Ghraib und Guantanamo diskutiert. Das EGMR-Urteil von 1978 wurde in US-Justizdokumenten tatsaechlich als Praezedenz zitiert — als Argument, dass diese Methoden legal seien."
    )

)
