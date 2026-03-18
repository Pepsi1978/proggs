package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsExpert(): List<Question> = listOf(

    // Question 1 — Hittite Empire: Battle of Qadesh treaty
    Question(
        categoryId = 3,
        questionText = "Welcher hethitische König schloss mit Ramses II. nach der Schlacht von Kadesch den ältesten erhaltenen Friedensvertrag der Geschichte?",
        answerA = "Suppiluliuma I.",
        answerB = "Mursili II.",
        answerC = "Hattusili III.",
        answerD = "Tudhaliya IV.",
        correctAnswer = 2, // C
        explanation = "Hattusili III. und Ramses II. schlossen um 1259 v. Chr. den Vertrag von Kadesch. Das Original ist auf Silbertafeln überliefert; eine akkadische Fassung wurde in Hattusa gefunden, eine ägyptische Version steht als Hieroglyphen im Karnak-Tempel.",
        difficulty = 4,
        funFact = "Eine Abschrift des Vertrags hängt heute als Symbol internationaler Diplomatie im UN-Hauptquartier in New York."
    ),

    // Question 2 — Hittite Empire: Tabarna title
    Question(
        categoryId = 3,
        questionText = "Welchen Titel trugen hethitische Großkönige als universale Herrscherbezeichnung, abgeleitet vom Namen des Dynastiegründers?",
        answerA = "Lugal",
        answerB = "Tabarna",
        answerC = "Šarru",
        answerD = "En",
        correctAnswer = 1, // B
        explanation = "Der Titel 'Tabarna' (auch Labarna) ging auf den Gründer der hethitischen Großreichsdynastie zurück und wurde von allen Nachfolgekönigen als Thronepitheion getragen. Er entspricht in seiner Funktion dem ägyptischen 'Pharao' als institutioneller Königsbezeichnung.",
        difficulty = 4,
        funFact = "Die hethitische Königin trug den entsprechenden Titel 'Tawananna', der ebenfalls dynastisch weitervererbt wurde."
    ),

    // Question 3 — Hittite Empire: Indo-European language
    Question(
        categoryId = 3,
        questionText = "Welche Besonderheit macht das Hethitische in der Linguistik so bedeutsam, dass Ferdinand de Saussures Laryngaltheorie dadurch bestätigt wurde?",
        answerA = "Es besaß als einzige altorientalische Sprache ein Alphabet",
        answerB = "Es bewahrte Laryngale, die de Saussure theoretisch für das Urindogermanische erschlossen hatte",
        answerC = "Es zeigte, dass Sumerisch eine indogermanische Ursprache war",
        answerD = "Es wurde in einer bisher unbekannten Silbenschrift geschrieben",
        correctAnswer = 1, // B
        explanation = "Ferdinand de Saussure postulierte 1879 Laryngallaute im Urindogermanischen rein auf Basis struktureller Überlegungen. Als Edgar Sturtevant das Hethitische entzifferte, fand man darin tatsächlich Laute (ḫ), die de Saussures hypothetischen Laryngalen entsprachen — eine spektakuläre nachträgliche Bestätigung.",
        difficulty = 4,
        funFact = "Das Hethitische wurde erst 1915 von Bedřich Hrozný entziffert, nachdem man jahrzehntelang nicht wusste, ob es überhaupt indogermanisch war."
    ),

    // Question 4 — Assyrian tactics: Deportation policy
    Question(
        categoryId = 3,
        questionText = "Welche systematische Politik betrieben die assyrischen Könige ab Tiglatpileser III., um unterworfene Völker dauerhaft zu kontrollieren?",
        answerA = "Massenhinrichtungen der gesamten männlichen Bevölkerung",
        answerB = "Zwangsumsiedlung ganzer Bevölkerungsgruppen in andere Reichsteile",
        answerC = "Einführung des Aramäischen als einzige zugelassene Sprache",
        answerD = "Zerstörung aller Kultbilder und Tempel der Unterworfenen",
        correctAnswer = 1, // B
        explanation = "Tiglatpileser III. (reg. 745–727 v. Chr.) institutionalisierte die Deportationspolitik: Unterworfene Eliten und Handwerker wurden gezielt umgesiedelt, um regionale Identitäten aufzulösen und Aufstände zu verhindern. Das Nordreich Israel fiel 722 v. Chr. unter Sargon II. dieser Politik zum Opfer ('die zehn verlorenen Stämme').",
        difficulty = 4,
        funFact = "Assyrische Inschriften belegen über 4 Millionen deportierte Menschen über drei Jahrhunderte."
    ),

    // Question 5 — Assyrian tactics: Siege warfare
    Question(
        categoryId = 3,
        questionText = "Welche Belagerungsmaschine entwickelten die Assyrer, die bis dahin einzigartig war und ihnen die Einnahme stark befestigter Städte ermöglichte?",
        answerA = "Den Trebuchet (Gegengewichtskatapult)",
        answerB = "Den fahrbaren Belagerungsturm mit integriertem Rammbock",
        answerC = "Die Balliste (Torsionsgeschütz)",
        answerD = "Das Onager (Schleuderwurfmaschine)",
        correctAnswer = 1, // B
        explanation = "Die Assyrer entwickelten um das 9. Jh. v. Chr. komplexe fahrbare Belagerungstürme mit eingebautem Rammbock. Assyrische Reliefs aus dem Palast Sanheribs zeigen detailliert, wie diese Maschinen mit Wasserschläuchen gegen Brandpfeile geschützt und von Bogenschützen begleitet wurden.",
        difficulty = 4,
        funFact = "Sanheribs Zerstörung Babylons 689 v. Chr. galt in der Antike als so ungeheuerlich, dass sein Sohn Asarhaddon die Stadt wiederaufbaute, um den Zorn der Götter zu besänftigen."
    ),

    // Question 6 — Neo-Babylonian Empire: Nebuchadnezzar's administration
    Question(
        categoryId = 3,
        questionText = "Welches Wirtschaftssystem ermöglichte es dem neubabylonischen Reich unter Nebukadnezar II., trotz ständiger Kriege enorme Bauprogramme zu finanzieren?",
        answerA = "Ein staatliches Monopol auf den Fernhandel mit Zinn und Kupfer",
        answerB = "Tempelwirtschaft mit Grundbesitz, Handwerk und Kreditvergabe durch große Tempelkomplexe",
        answerC = "Ein Steuersystem nach persischem Vorbild mit festen Satrapien",
        answerD = "Tributzahlungen unterworfener Vasallenkönige in Gold und Silber",
        correctAnswer = 1, // B
        explanation = "Die neubabylonische Wirtschaft basierte maßgeblich auf der Tempelwirtschaft: Institutionen wie der Eanna-Tempel in Uruk und der Esagila-Tempel in Babylon besaßen riesigen Grundbesitz, betrieben Handwerk und Handel und vergaben Kredite. Sie fungierten als Banken und Unternehmen zugleich.",
        difficulty = 4,
        funFact = "Die Murashu-Archive aus Nippur (5. Jh. v. Chr.) liefern einmalige Einblicke in das Bankenwesen des babylonischen Raums — eine Kaufmannsfamilie dokumentierte über Generationen Tausende Kreditverträge."
    ),

    // Question 7 — Neo-Babylonian Empire: Nabonidus
    Question(
        categoryId = 3,
        questionText = "Warum zog sich der letzte neubabylonische König Nabonid für zehn Jahre nach Tayma in Arabien zurück, was letztlich zum Fall Babylons beitrug?",
        answerA = "Er floh vor einem Aufstand der babylonischen Priesterschaft",
        answerB = "Er verehrte den Mondgott Sin statt Marduk und entzweite sich mit der Priesterschaft Babylons",
        answerC = "Er gründete eine neue Hauptstadt, um dem Perserdruck zu entgehen",
        answerD = "Er unternahm einen Feldzug zur Kontrolle der Weihrauchstraße",
        correctAnswer = 1, // B
        explanation = "Nabonid (reg. 556–539 v. Chr.) förderte den Mondgott Sin auf Kosten des Reichsgottes Marduk, was ihn in scharfen Konflikt mit der mächtigen Marduks-Priesterschaft in Babylon brachte. Seine zehnjährige Abwesenheit in Tayma ließ seinen Sohn Belsazar als Regent zurück und destabilisierte das Reich, bevor Kyros II. 539 v. Chr. kampflos einziehen konnte.",
        difficulty = 4,
        funFact = "Die persische Propagandainschrift, der 'Kyros-Zylinder', stellte den Einzug in Babylon als freiwillige Befreiung der Babylonier von dem impietätischen Nabonid dar."
    ),

    // Question 8 — Achaemenid administration: Satrapies
    Question(
        categoryId = 3,
        questionText = "Welche Verwaltungsneuerung führte Darius I. ein, um die Satrapen zu kontrollieren und Korruption zu verhindern?",
        answerA = "Er ernannte ausschließlich Mitglieder der Achämenidenfamilie zu Satrapen",
        answerB = "Er installierte unabhängige königliche Sekretäre und Militärkommandanten als Kontrolleure neben den Satrapen",
        answerC = "Er ließ alle Satrapen jährlich an den Hof nach Persepolis kommen und Rechenschaft ablegen",
        answerD = "Er führte eine einheitliche Schriftsprache ein, damit alle Berichte zentral geprüft werden konnten",
        correctAnswer = 1, // B
        explanation = "Darius I. schuf ein Gegengewicht zur Macht der Satrapen: Neben jedem Satrapen stand ein königlicher Schreiber/Sekretär, der direkt an den König berichtete, und ein separater Militärkommandant, der nicht dem Satrapen unterstand. Zusätzlich schickte Darius 'Augen und Ohren des Königs' als wandernde Inspektoren.",
        difficulty = 4,
        funFact = "Das Persische Reich unter Darius I. umfasste etwa 20 Satrapien, die zusammen über 40% der damaligen Weltbevölkerung verwalteten."
    ),

    // Question 9 — Achaemenid administration: Royal Road
    Question(
        categoryId = 3,
        questionText = "Wie lang war die Königsstraße der Achämeniden von Sardes nach Susa laut Herodot, und welche Reisezeit garantierte der königliche Postdienst (Angareion)?",
        answerA = "ca. 1.500 km, Reisezeit 30 Tage",
        answerB = "ca. 2.400 km, Reisezeit 7 Tage",
        answerC = "ca. 3.000 km, Reisezeit 14 Tage",
        answerD = "ca. 2.000 km, Reisezeit 90 Tage für normale Reisende",
        correctAnswer = 1, // B
        explanation = "Herodot berichtet, dass die Königsstraße von Sardes nach Susa ca. 450 Parasangen (etwa 2.400 km) lang war. Normale Karawanen brauchten 90 Tage. Der königliche Postdienst Angareion mit Pferdewechselstationen schaffte die Strecke in etwa 7 Tagen — ein System, das Napoleon noch beeindruckte.",
        difficulty = 4,
        funFact = "Das Wort 'Angareion' gelangte ins Griechische und von dort ins Lateinische; es ist der Ursprung des deutschen Wortes 'Angabe' im Sinne von 'Meldung'."
    ),

    // Question 10 — Achaemenid administration: Persepolis tablets
    Question(
        categoryId = 3,
        questionText = "Was belegen die Persepolis Fortification Tablets (ca. 509–494 v. Chr.) über die achämenidische Verwaltungspraxis?",
        answerA = "Detaillierte Tributlisten aller 20 Satrapien mit Goldmengen",
        answerB = "Lohnzahlungen in Naturalien an Arbeiter und Handwerker verschiedenster Herkunft am Königshof",
        answerC = "Militärische Truppenstärken und Waffenreserven des Reichs",
        answerD = "Diplomatische Korrespondenz mit griechischen Stadtstaaten",
        correctAnswer = 1, // B
        explanation = "Die über 30.000 erhaltenen Tontafeln aus Persepolis, verfasst in Elamisch, dokumentieren vor allem Lohnzahlungen in Wein, Bier, Getreide und gelegentlich Silber an Arbeiter, Handwerker und Beamte. Sie zeigen eine höchst differenzierte Bürokratie mit Frauen in qualifizierten Positionen und Arbeitern aus dem ganzen Reich.",
        difficulty = 4,
        funFact = "Die Tafeln belegen, dass Frauen am achämenidischen Hof teils dieselben Rationen wie männliche Kollegen erhielten — ungewöhnlich für die Antike."
    ),

    // Question 11 — Diadochi wars: Partition of Triparadisus
    Question(
        categoryId = 3,
        questionText = "Welche Aufteilung des Alexanderreichs wurde 320 v. Chr. in Triparadisus vorgenommen, und wer erhielt dabei die strategisch wichtigste Satrapie?",
        answerA = "Antipater erhielt Makedonien, Antigonos Monophthalmos Asien, Seleukos Babylon",
        answerB = "Antigonos Monophthalmos erhielt Asien, Ptolemaios Ägypten, Seleukos Babylon bestätigt",
        answerC = "Perdikkas' Nachfolger Antipater wurde Reichsverweser, Ptolemaios Ägypten, Seleukos Babylonien",
        answerD = "Lysimachos erhielt Thrakien, Kassander Makedonien, Antiochos Syrien",
        correctAnswer = 2, // C
        explanation = "In Triparadisus (321/320 v. Chr.) übernahm Antipater als Reichsverweser das Erbe des getöteten Perdikkas. Ptolemaios behielt Ägypten, Seleukos erhielt (wieder) Babylon. Antigonos Monophthalmos wurde Stratege Asiens. Diese Teilung begründete die Grundstruktur der späteren Diadochenreiche.",
        difficulty = 4,
        funFact = "Der Name Triparadisus bedeutet 'Dreifacher Garten' und lag wahrscheinlich in der heutigen libanesischen Bekaa-Ebene."
    ),

    // Question 12 — Diadochi wars: Battle of Ipsus
    Question(
        categoryId = 3,
        questionText = "Welche entscheidende Rolle spielten Seleukos' Kriegselefanten in der Schlacht von Ipsos 301 v. Chr., die zum Tod des Antigonos führte?",
        answerA = "Sie durchbrachen das makedonische Phalanx-Zentrum und töteten Antigonos direkt",
        answerB = "Sie schnitten nach dem Ausbruch von Antigonos' Reiterei unter Demetrios die Verbindung zur Infanterie ab",
        answerC = "Sie wurden eingesetzt, um die Festungsanlagen von Ipsos zu zerstören",
        answerD = "Sie vertrieben Ptolemaios' Flotte aus dem Hafen von Milet",
        correctAnswer = 1, // B
        explanation = "Demetrios Poliorketes, Sohn des Antigonos, brach mit der Reiterei durch, verfolgte den Gegner und ließ sein Zentrum ungedeckt. Seleukos' 480 Elefanten riegelten den Rückweg ab, sodass die antigonidische Infanterie eingeschlossen war. Antigonos starb unter einem Pfeilhagel. Der Sieg der Koalition (Seleukos, Lysimachos, Kassander, Ptolemaios) beendete den Traum eines wiedervereinigten Alexanderreichs.",
        difficulty = 4,
        funFact = "Seleukos hatte seine Elefanten im Indusvertrag 305 v. Chr. von Chandragupta Maurya erhalten — im Austausch für sein indisches Territorium."
    ),

    // Question 13 — Seleucid Kingdom: Antiochus IV and Judaism
    Question(
        categoryId = 3,
        questionText = "Was bezeichnete der Begriff 'Bdelygma Eremon' (Greuel der Verwüstung) im Danielbuch, der auf Antiochus IV. Epiphanes zurückgeführt wird?",
        answerA = "Die Schleifung der Stadtmauern Jerusalems durch syrische Truppen",
        answerB = "Die Aufstellung eines Zeus-Altars im Tempel und die Opferung von Schweinen im Allerheiligsten",
        answerC = "Das Verbot des jüdischen Kalenders und die Zerstörung aller Torarollen",
        answerD = "Die Ermordung des Hohepriesters Onias III. auf Befehl des Königs",
        correctAnswer = 1, // B
        explanation = "Antiochus IV. ließ 167 v. Chr. den Jerusalemer Tempel in ein Zeus-Heiligtum umwandeln, einen Altar errichten und dort Schweine opfern — für Juden die schlimmste kultische Entweihung. Dies löste den Makkabäeraufstand aus. Die Wiedereinweihung des Tempels 164 v. Chr. wird bis heute als Chanukka gefeiert.",
        difficulty = 4,
        funFact = "Antiochus IV. nannte sich selbst 'Epiphanes' (der Erscheinende/Gottoffenbarte); jüdische Quellen nannten ihn spöttisch 'Epimanes' (der Wahnsinnige)."
    ),

    // Question 14 — Ptolemaic Kingdom: Bureaucracy
    Question(
        categoryId = 3,
        questionText = "Welches wirtschaftliche System führten die Ptolemäer in Ägypten ein, das ein staatliches Monopol über Produktion und Handel zahlreicher Güter schuf?",
        answerA = "Das Latifundiensystem nach römischem Vorbild",
        answerB = "Das Apomoira-System mit königlichem Monopol auf Öl, Papyrus, Textilien und Bier",
        answerC = "Das hellenistische Emporionssystem mit freiem Markt unter staatlicher Aufsicht",
        answerD = "Die Übertragung aller Ländereien an makedonische Kleruchen als Militärland",
        correctAnswer = 1, // B
        explanation = "Die Ptolemäer etablierten eines der am stärksten regulierten Wirtschaftssysteme der Antike: Der Staat monopolisierte Sesamöl, Leinöl, Papyrus, Bier, Textilien und andere Waren. Preise wurden festgesetzt, Produktion staatlich kontrolliert. Der Reichtum des ptolemäischen Ägyptens beruhte maßgeblich auf diesem System.",
        difficulty = 4,
        funFact = "Der Papyrus Tebtunis II gibt uns einen detaillierten Einblick in ptolemäische Steuererhebungspraxis — inkl. einer Liste von über 50 verschiedenen Steuerarten."
    ),

    // Question 15 — Ptolemaic Kingdom: Library of Alexandria
    Question(
        categoryId = 3,
        questionText = "Welcher ptolemäische König beauftragte Demetrios von Phaleron, eine Universalbibliothek zu schaffen, und welches intellektuelle Ziel stand dabei im Vordergrund?",
        answerA = "Ptolemaios III. Euergetes, der alle Schriften Homers sammeln wollte",
        answerB = "Ptolemaios I. Soter, mit dem Ziel, sämtliches Wissen der Menschheit an einem Ort zu versammeln",
        answerC = "Ptolemaios II. Philadelphos, der die Septuaginta übersetzen lassen wollte",
        answerD = "Ptolemaios IV. Philopator, der ein Gegengewicht zu Pergamon schaffen wollte",
        correctAnswer = 1, // B
        explanation = "Ptolemaios I. Soter holte Demetrios von Phaleron (Schüler des Aristoteles) nach Alexandria, um eine Universalbibliothek zu planen. Das erklärte Ziel: alle Bücher aller Völker zu sammeln. Unter Ptolemaios II. wurde die Bibliothek mit dem Museion zum führenden Wissenszentrum der Antike mit ca. 400.000–700.000 Schriftrollen.",
        difficulty = 4,
        funFact = "Ptolemaios III. soll Schiffe in Alexandria angehalten und alle Bücher kopieren lassen haben — die Originale behielt er, die Kopien gab er zurück."
    ),

    // Question 16 — Roman Republic: Lex Sempronia Agraria
    Question(
        categoryId = 3,
        questionText = "Welche Bestimmung des Licinisch-Sextischen Ackergesetzes (367 v. Chr.) reaktivierte Tiberius Gracchus 133 v. Chr. in seiner Lex Sempronia Agraria?",
        answerA = "Das Verbot, mehr als 500 Iugera Staatsland (ager publicus) zu besitzen",
        answerB = "Die Verteilung des Ackerlandes an alle Bürger ohne Unterschied der Herkunft",
        answerC = "Die Verpflichtung der Latifundienbesitzer, ein Drittel ihres Landes zu verpachten",
        answerD = "Das Recht der Plebejer, eines der beiden Konsulamter zu bekleiden",
        correctAnswer = 0, // A
        explanation = "Das Licinisch-Sextische Gesetz von 367 v. Chr. hatte die Nutzung von ager publicus auf 500 Iugera (ca. 125 ha) pro Person begrenzt — wurde aber nie konsequent durchgesetzt. Tiberius Gracchus griff diese Begrenzung 133 v. Chr. auf und wollte das über diese Grenze hinaus genutzte Land einziehen und an Landlose verteilen.",
        difficulty = 4,
        funFact = "Tiberius Gracchus wurde noch im Amtsjahr 133 v. Chr. von senatorischen Gegnern erschlagen — der erste politische Mord in der Geschichte der Römischen Republik."
    ),

    // Question 17 — Roman Republic: Gaius Gracchus
    Question(
        categoryId = 3,
        questionText = "Welches institutionelle Mittel nutzte Gaius Gracchus (123–121 v. Chr.), das seinen Bruder Tiberius nicht hatte, um seine Reformgesetzgebung abzusichern?",
        answerA = "Er ließ sich zum Diktator auf Zeit ernennen",
        answerB = "Er gewann die Ritterklasse durch das Ius Iudicandi (Gerichtsrecht) als Gegengewicht zum Senat",
        answerC = "Er schloss ein Bündnis mit den italischen Bundesgenossen für gemeinsame Abstimmung",
        answerD = "Er nutzte das Militärtribunat als Machtbasis außerhalb der Volksversammlung",
        correctAnswer = 1, // B
        explanation = "Gaius Gracchus übertrug die Besetzung der Gerichtskommissionen (quaestiones perpetuae) für Erpressung (lex Acilia) von Senatoren auf Ritter (equites). Damit schuf er eine mächtige Unterstützerbasis außerhalb des Senats. Die Ritter profitierten auch von seiner lex frumentaria (vergünstigtes Getreide) und den Provinzialpachtverträgen.",
        difficulty = 4,
        funFact = "Gaius Gracchus liess sich nach seinem Tod 121 v. Chr. von einem Sklaven töten, um der Verhaftung zu entgehen. Sein Kopf soll gegen Gold aufgewogen worden sein."
    ),

    // Question 18 — Marius: Military Reform
    Question(
        categoryId = 3,
        questionText = "Welche zwei miteinander zusammenhängenden Reformen des Gaius Marius (107 v. Chr.) veränderten das römische Militär grundlegend und legten den Keim der späteren Bürgerkriege?",
        answerA = "Einführung der Kohortentaktik und des Pilum als Standardwaffe",
        answerB = "Öffnung der Legionen für Besitzlose (capite censi) und Ausrüstung durch den Staat statt durch den Einzelnen",
        answerC = "Einführung des Adlers als Legionssymbol und Abschaffung der Manipeltaktik",
        answerD = "Auflösung der Bundesgenossenkontingente und Schaffung reiner Bürgerlegionen",
        correctAnswer = 1, // B
        explanation = "Marius öffnete die Legionen für Bürger ohne Grundbesitz (capite censi), die bisher vom Dienst ausgeschlossen waren, und stattete sie auf Staatskosten aus. Da der Staat keine Veteranenversorgung sicherstellte, wurden die Soldaten von ihrem Feldherrn abhängig, dem sie ihr Land erhofften. Diese Klientelisierung des Heeres war die strukturelle Voraussetzung für Sullas und Caesars Bürgerkriege.",
        difficulty = 4,
        funFact = "Marius wurde siebenmal zum Konsul gewählt — eine in der Republik beispiellose Häufung, die das Prinzip der Annuität unterhöhlte."
    ),

    // Question 19 — Sulla: Proscriptions
    Question(
        categoryId = 3,
        questionText = "Was waren die Sullanischen Proskriptionen (82 v. Chr.) juristisch gesehen, und welche wirtschaftliche Funktion erfüllten sie neben der politischen?",
        answerA = "Strafrechtliche Verfahren, die Vermögenseinzug ermöglichten",
        answerB = "Ächtungslisten, die Gegner zu vogelfrei Erklärten machten und deren Güter dem Staat zufielen",
        answerC = "Senatsurteile, die Verbannte mit Gütereinzug bestraften",
        answerD = "Volksversammlungsbeschlüsse, die ohne Anklage Todesurteile ermöglichten",
        correctAnswer = 1, // B
        explanation = "Sullas Proskriptionen waren öffentlich ausgehängte Todeslisten: Wer auf der Liste stand, war vogelfrei, hatte kein Recht auf Prozess, und sein Vermögen fiel nach der Tötung an den Staat. Sulla verkaufte diese Güter günstig an Anhänger — ein Instrument zur Belohnung der eigenen Partei und zur Vernichtung der Gegner. Bis zu 10.000 Menschen kamen ums Leben.",
        difficulty = 4,
        funFact = "Marcus Licinius Crassus machte einen Teil seines enormen Vermögens damit, Güter von Proskribierten weit unter Wert aufzukaufen."
    ),

    // Question 20 — Sulla: Constitutional reforms
    Question(
        categoryId = 3,
        questionText = "Welche Bestimmung in Sullas Verfassungsreform von 81 v. Chr. sollte die Macht der Volkstribunen dauerhaft brechen?",
        answerA = "Abschaffung des Volkstribunats als Amt",
        answerB = "Verbot für Tribunen, nach ihrem Amt weitere Ämter zu bekleiden, und Einschränkung ihres Interzessionsrechts",
        answerC = "Bindung aller Tribunen an die Entscheidungen des Senats",
        answerD = "Reduzierung der Tributkomitien auf rein beratende Funktion",
        correctAnswer = 1, // B
        explanation = "Sulla ließ das Volkstribunat nicht ab, degradierte es aber: Tribunen durften keine höheren Ämter mehr anstreben, was das Amt für Aufstiegswillige unattraktiv machte. Zudem wurde ihr Gesetzgebungsrecht eingeschränkt und an Senatsgenehmigung gebunden. Diese Reformen wurden nach Sullas Rückzug 79 v. Chr. jedoch schnell rückgängig gemacht.",
        difficulty = 4,
        funFact = "Sulla legte 79 v. Chr. seine Diktatur nieder — der einzige römische Diktator, der dies freiwillig tat — und starb ein Jahr später in der Privatperson."
    ),

    // Question 21 — First Triumvirate
    Question(
        categoryId = 3,
        questionText = "Was war das konkrete politische Ziel, das Pompeius, Crassus und Caesar 60 v. Chr. zum Ersten Triumvirat zusammenführte, und wie wurde es technisch umgesetzt?",
        answerA = "Crassus wollte Syrien, Pompeius Spanien, Caesar Gallien — alle drei kooperierten für gegenseitige Provinzkontingente",
        answerB = "Pompeius benötigte Landgesetz für Veteranen und Ratifikation seiner Ostordnung, Crassus Steuerrabatt für Steuerpächter, Caesar Konsulat und Provinz",
        answerC = "Alle drei wollten gemeinsam eine Reform der Provinzverwaltung durchsetzen, um Korruption zu beenden",
        answerD = "Das Triumvirat entstand um Ciceros Konsulatsbewerbung 63 v. Chr. zu verhindern",
        correctAnswer = 1, // B
        explanation = "Das Erste Triumvirat war eine Interessenkoalition: Pompeius scheiterte im Senat mit dem Landgesetz für seine Veteranen und der Ratifikation seiner Ostordnung; Crassus' Steuerfarmer wollten Nachlässe; Caesar brauchte Unterstützung für sein Konsulatsziel und eine Provinz danach. Als Konsul 59 v. Chr. setzte Caesar alle drei Ziele durch.",
        difficulty = 4,
        funFact = "Cicero nannte das Triumvirat das 'Monster mit drei Köpfen' — er erkannte als erster klar die informelle Machtstruktur hinter der republikanischen Fassade."
    ),

    // Question 22 — Second Triumvirate
    Question(
        categoryId = 3,
        questionText = "Worin unterschied sich das Zweite Triumvirat (43 v. Chr.) rechtlich grundlegend vom Ersten?",
        answerA = "Es war ein formell durch Volksgesetz geschaffenes Amt mit exekutiven Vollmachten",
        answerB = "Es hatte keine rechtliche Grundlage, sondern war ein geheimer Privatpakt",
        answerC = "Es wurde vom Senat auf fünf Jahre bewilligt und konnte jederzeit widerrufen werden",
        answerD = "Es war identisch mit der Diktatur nach sullanischem Vorbild",
        correctAnswer = 0, // A
        explanation = "Das Zweite Triumvirat der lex Titia (43 v. Chr.) schuf das Amt der 'tresviri rei publicae constituendae' — drei Männer zur Neuordnung des Staates — mit vollständigen konsularischen und mehr Vollmachten, zunächst auf fünf Jahre. Dies war ein offizielles Staatsamt, kein heimlicher Pakt wie das Erste Triumvirat.",
        difficulty = 4,
        funFact = "Sofort nach der Schaffung des Zweiten Triumvirats begannen die Proskriptionen, deren erstes prominentes Opfer Cicero war."
    ),

    // Question 23 — Late Roman Empire: Diocletian's Tetrarchy
    Question(
        categoryId = 3,
        questionText = "Welches theologische Prinzip legitimierte die Tetrarchie Diokletians, und welche Götterpaare standen jeweils hinter den beiden Augusti?",
        answerA = "Diokletian als Augustus des Westens war Jupiter zugeordnet, Maximian als Augustus des Ostens Herkules",
        answerB = "Diokletian als Iovius (Jupiter) und Maximian als Herculius (Herkules), die Caesaren erhielten keine eigene göttliche Zuordnung",
        answerC = "Diokletian berief sich auf Sol Invictus, Maximian auf Mars als göttliche Patrone",
        answerD = "Die Tetrarchie wurde vom Christentum theologisch legitimiert, da Diokletian Toleranzedikte erließ",
        correctAnswer = 1, // B
        explanation = "Diokletian nahm den Beinamen 'Iovius' (von Jupiter), Maximian 'Herculius' (von Herkules). Jupiter als höchster Gott entsprach dem leitenden Augustus Diokletian, Herkules als Vollstrecker dem militärisch aktiven Maximian. Dieses Zuordnungssystem gab dem Kollegium eine sakrale Hierarchie und legitimierte die Doppelherrschaft.",
        difficulty = 4,
        funFact = "Diokletian war der erste Kaiser, der bewusst und freiwillig abdankte — 305 n. Chr. zog er sich nach Spalatum (heute Split) zurück, wo sein Palast erhalten ist."
    ),

    // Question 24 — Diocletian: Economic reforms
    Question(
        categoryId = 3,
        questionText = "Welche wirtschaftspolitische Maßnahme Diokletians aus dem Jahr 301 n. Chr. ist als eines der ersten staatlichen Preisregulierungsversuche der Geschichte bekannt?",
        answerA = "Die Einführung des Solidus als stabile Goldmünze",
        answerB = "Das Edictum de pretiis rerum venalium — Höchstpreisedikt für über 1.000 Waren und Dienstleistungen",
        answerC = "Die Bindung der Handwerker an ihre Berufe durch das Colonat-System",
        answerD = "Die staatliche Übernahme aller Fernhandelswege im Reich",
        correctAnswer = 1, // B
        explanation = "Das Edictum de pretiis (301 n. Chr.) setzte Höchstpreise für über 1.000 Waren und Dienstleistungen fest, von Weizen bis Prostituierte. Überschreitungen wurden mit dem Tod bestraft. Das Edikt scheiterte weitgehend, weil Händler Waren zurückhielten statt sie zu Verlustpreisen zu verkaufen, zeigt aber die Tiefe der Inflationskrise des späten 3. Jh.",
        difficulty = 4,
        funFact = "Erhaltene Inschriftenfragmente des Edikts wurden in über 40 Orten des Reichs gefunden — ein Zeichen der massiven Verbreitungskampagne."
    ),

    // Question 25 — Constantine: Edict of Milan
    Question(
        categoryId = 3,
        questionText = "Was war das Edikt von Mailand (313 n. Chr.) genau, und was unterscheidet es rechtlich vom Toleranzedikt des Galerius von 311 n. Chr.?",
        answerA = "Es erklärte das Christentum zur Staatsreligion; Galerius hatte nur partiellen Schutz gewährt",
        answerB = "Es war eine kaiserliche Vereinbarung zwischen Konstantin und Licinius, die allgemeine Religionsfreiheit für alle Kulte gewährte; Galerius hatte nur Christen geduldet",
        answerC = "Es ordnete die Rückgabe konfiszierter Kirchengüter an; Galerius hatte dies nicht eingeschlossen",
        answerD = "Es wurde von Konstantin alleine erlassen und hob alle Verfolgungsgesetze auf",
        correctAnswer = 1, // B
        explanation = "Das 'Edikt von Mailand' war keine offizielle Urkunde, sondern ein Brief Konstantins und Licinius' nach ihrer Mailänder Konferenz. Es gewährte allen Bürgern volle Religionsfreiheit (nicht nur Christen) und befahl Rückgabe konfiszierter Güter. Galerius' Edikt von 311 hatte Christen nur geduldet, sofern sie die öffentliche Ordnung nicht störten.",
        difficulty = 4,
        funFact = "Das 'Edikt von Mailand' ist kein Mailänder Dokument: Konstantin und Licinius trafen sich in Mailand, aber die Anordnung wurde als Brief von Licinius in Nikomedien veröffentlicht."
    ),

    // Question 26 — Constantine: Nicaea Council
    Question(
        categoryId = 3,
        questionText = "Welche theologische Formel verabschiedete das Konzil von Nicäa (325 n. Chr.) zur Klärung des arianischen Streits, und was bedeutete der Schlüsselbegriff?",
        answerA = "Homoiousios (wesensähnlich) — der Sohn ist dem Vater ähnlich, aber nicht gleich",
        answerB = "Homoousios (wesensgleich/konsubstantiell) — der Sohn ist aus demselben Wesen wie der Vater",
        answerC = "Homoios (gleich) — der Sohn ist dem Vater gleich in allen Dingen nach der Schrift",
        answerD = "Heteroousios (wesensverschieden) — der Sohn ist ein Geschöpf des Vaters",
        correctAnswer = 1, // B
        explanation = "Das Konzil von Nicäa verwarf den Arianismus (Sohn als Geschöpf, subordinat dem Vater) und formulierte homoousios: Der Sohn ist wesensgleich mit dem Vater. Dieser Begriff, ein einzelner griechischer Buchstabe Unterschied zu homoiousios, spaltete die Christenheit für Jahrzehnte. Konstantin unterstützte die nicänische Formel politisch.",
        difficulty = 4,
        funFact = "Edward Gibbons Bemerkung, dass das Schicksal des Reichs von einem einzelnen Buchstaben (Iota in homoiousios vs. homoousios) abhing, ist historiographisch berühmt."
    ),

    // Question 27 — Constantine: Constantinople founding
    Question(
        categoryId = 3,
        questionText = "Warum wählte Konstantin Byzantion als Standort seiner neuen Hauptstadt, und welche urbanistische Symbolik verband er bewusst mit der neuen Stadt?",
        answerA = "Byzantion lag strategisch zwischen Europa und Asien, und Konstantin orientierte sich bewusst am Vorbild Roms mit sieben Hügeln, einem Forum und einem Senat",
        answerB = "Die Stadt war bereits Christen heilig wegen der Apostelgräber, und Konstantin wollte ein rein christliches Zentrum schaffen",
        answerC = "Konstantin wählte Byzanz wegen seines natürlichen Hafens für den Getreidehandel aus Ägypten",
        answerD = "Die Stadt war von Alexander dem Großen gegründet worden, an dessen Erbe Konstantin anknüpfte",
        correctAnswer = 0, // A
        explanation = "Konstantinopel lag am Bosporus — strategisch ideal zwischen Europa, Asien und dem Schwarzmeer. Konstantin gestaltete die Stadt bewusst als Neues Rom: sieben Hügel, ein großes Forum (Forum Constantini), eine Rennbahn (Hippodrom), einen Senat und Getreidedistributionen nach römischem Muster. Damit beanspruchte Konstantinopel symbolisch das Erbe Roms.",
        difficulty = 4,
        funFact = "Am Hippodrom von Konstantinopel stand der Schlangenturm aus Delphi — Konstantin ließ wichtige Kunstwerke aus dem ganzen Reich dorthin transferieren."
    ),

    // Question 28 — Julian the Apostate: Religious policy
    Question(
        categoryId = 3,
        questionText = "Welche subtile, aber wirkungsvolle Maßnahme Julians gegen das Christentum war juristisch schwer angreifbar, traf die Kirche aber empfindlich?",
        answerA = "Das Verbot christlicher Gottesdienste in öffentlichen Gebäuden",
        answerB = "Das Schuledikt von 362, das Christen von der Unterweisung in klassischer Literatur und Rhetorik ausschloss",
        answerC = "Die Konfiszierung aller Kirchengebäude, die seit Konstantin gebaut worden waren",
        answerD = "Das Verbot christlicher Bischöfe, kaiserliche Ämter zu bekleiden",
        correctAnswer = 1, // B
        explanation = "Julians Schuledikt (362 n. Chr.) verbot Christen, klassische Texte als Lehrer zu unterrichten, da sie nicht an die Götter glaubten, über die sie lehrten. Da Bildung und gesellschaftlicher Aufstieg auf klassischer Literatur basierten, traf dies die christliche Elite. Das Edikt provozierte Empörung; selbst heidnische Gelehrte kritisierten es als unfair.",
        difficulty = 4,
        funFact = "Als Reaktion auf Julians Schuledikt schrieben die Apollinaristen eine christliche Version der gesamten griechischen Literatur — von Homer bis zu den Tragikern — mit biblischen Inhalten."
    ),

    // Question 29 — Julian the Apostate: Persian campaign
    Question(
        categoryId = 3,
        questionText = "Welche strategisch fatale Entscheidung traf Julian auf seinem Persienfeldzug 363 n. Chr. kurz vor Ktesiphon, die zum Tod des Kaisers und zum Schmachfrieden führte?",
        answerA = "Er trennte seine Reiterei vom Hauptheer, die dann bei Samarra vernichtet wurde",
        answerB = "Er ließ seine Versorgungsflotte auf dem Euphrat verbrennen, um den Rückweg abzuschneiden",
        answerC = "Er lehnte das Angebot eines Waffenstillstands ab und griff Ktesiphon ohne ausreichend Belagerungsgerät an",
        answerD = "Er schickte ein Drittel seines Heeres unter Procopius nach Armenien, was das Hauptheer schwächte",
        correctAnswer = 1, // B
        explanation = "Julian ließ seine Euphrat-Versorgungsflotte verbrennen — angeblich um die persische Nutzung zu verhindern und den Rückmarsch über Land durch fruchtbareres Gebiet zu erzwingen. Dies erwies sich als Katastrophe: Die Truppen mussten durch verwüstetes Land marschieren, Julian fiel bei einem Reitergefecht, und sein Nachfolger Jovian schloss einen demütigenden Frieden.",
        difficulty = 4,
        funFact = "Julian starb an einem Speer, angeblich durch sein eigenes Heer oder einen Christen — eine ungelöste historische Kontroverse."
    ),

    // Question 30 — Theodosius: Edict of Thessalonica
    Question(
        categoryId = 3,
        questionText = "Was ordnete das Edikt von Thessalonika (380 n. Chr.) unter Theodosius I. an, und welche drei Kaiser unterzeichneten es?",
        answerA = "Es erklärte nicänisches Christentum zur einzigen legitimen Religion aller Untertanen und wurde von Theodosius, Gratian und Valentinian II. erlassen",
        answerB = "Es erlaubte allen Religionen freie Ausübung und wurde von Theodosius, Gratian und Arcadius unterzeichnet",
        answerC = "Es erklärte den Arianismus für ketzerisch und verbannte alle arianischen Bischöfe; nur Theodosius und Gratian unterzeichneten",
        answerD = "Es verpflichtete das Militär zum Christentum und wurde von Theodosius allein erlassen",
        correctAnswer = 0, // A
        explanation = "Das Edikt Cunctos populos (380 n. Chr.) erklärte das nicänische Christentum zur einzig legitimen Religion. Alle anderen religiösen Gruppen wurden als Häretiker oder Ketzer bezeichnet und verloren rechtlichen Schutz. Theodosius I., Gratian (Westkaiser) und Valentinian II. unterzeichneten es. Dies war der entscheidende Schritt zur Staatsreligion.",
        difficulty = 4,
        funFact = "Der Begriff 'Häresie' (griech. hairesis = Wahl, Partei) wurde durch dieses Edikt endgültig zum Rechtsbegriff mit strafrechtlichen Konsequenzen."
    ),

    // Question 31 — Theodosius: Gothic settlement
    Question(
        categoryId = 3,
        questionText = "Welche neuartige Rechtsstellung erhielten die Westgoten nach dem Frieden von 382 n. Chr. unter Theodosius, die sich fundamental von früheren Barbarensiedlungen unterschied?",
        answerA = "Sie wurden als reguläre römische Bürger in Thrakien angesiedelt und dem Konsistorium unterstellt",
        answerB = "Sie siedelten als föderati auf römischem Boden unter eigenen Königen und eigenen Gesetzen, ohne Steuerpflicht",
        answerC = "Sie erhielten den Status von Laeten — halbfreie Militärkolonisten unter römischen Offizieren",
        answerD = "Sie wurden in bestehende römische Legionen integriert und verloren damit ihre ethnische Selbstverwaltung",
        correctAnswer = 1, // B
        explanation = "Der Frieden von 382 n. Chr. siedelte die Westgoten in Thrakien als föderati (Verbündete) unter ihren eigenen Häuptlingen, nach eigenen Gesetzen und ohne Steuerpflicht an. Im Gegenzug stellten sie Truppen. Dies war beispiellos: Zuvor wurden Siedler immer der römischen Jurisdiktion unterstellt. Dieser Präzedenzfall öffnete das Reich für weitgehend autonome Barbarenverbände.",
        difficulty = 4,
        funFact = "Theodosius' Heer bei Adrianopel 378 n. Chr. hatte 2/3 seiner Männer verloren — die schlimmste Niederlage Roms seit Cannae. Das machte den Kompromiss von 382 politisch unvermeidbar."
    ),

    // Question 32 — Hittite Empire: Hattusa archaeology
    Question(
        categoryId = 3,
        questionText = "Was entdeckte Hugo Winckler 1907 in Hattusa, das die hethitische Historiographie grundlegend veränderte?",
        answerA = "Den Tempel des Wettergottes mit vollständigen Kultinventaren",
        answerB = "Das Königsarchiv mit über 10.000 Keilschrifttafeln auf Akkadisch und Hethitisch",
        answerC = "Die Königsgräber der frühen hethitischen Dynastiegründer",
        answerD = "Den Vertrag von Kadesch im Original auf Silbertafeln",
        correctAnswer = 1, // B
        explanation = "Hugo Winckler fand in Hattusa (heute Boğazkale) das königliche Archiv mit über 10.000 Keilschrifttafeln in Akkadisch und Hethitisch. Darunter war auch die hethitische Version des Vertrags von Kadesch. Diese Entdeckung ermöglichte die Erforschung der hethitischen Geschichte und Sprache überhaupt erst systematisch.",
        difficulty = 4,
        funFact = "Winckler starb 1913, bevor Bedřich Hrozný 1915 das Hethitische als indogermanische Sprache entzifferte — die Bestätigung von Wincklers Entdeckung erlebte er nicht mehr."
    ),

    // Question 33 — Assyrian Empire: Sargon II
    Question(
        categoryId = 3,
        questionText = "Was besagt die 'Sargon-Geographie', die unter Sargon II. von Assyrien (722–705 v. Chr.) entstand und ein einzigartiges weltpolitisches Denken belegt?",
        answerA = "Eine präzise Karte des assyrischen Reichs mit Entfernungsangaben zwischen Provinzhauptstädten",
        answerB = "Eine literarische Weltbeschreibung, die Assyrien als Mittelpunkt aller Völker und Länder darstellt",
        answerC = "Astronomische Berechnungen zur Bestimmung von Feldzugsterminen",
        answerD = "Eine Tributliste aller unterworfenen Völker mit geografischer Lokalisierung",
        correctAnswer = 1, // B
        explanation = "Die assyrische 'Imago Mundi' (Weltkarte) und zugehörige Texte aus der Zeit Sargons II. stellen Babylon als Nabel der Welt auf einer runden Erdscheibe dar, umgeben von einem Ring Ozean. Assyrische königliche Inschriften beanspruchten systematisch die Herrschaft über die Gesamtheit aller Völker — ein erstmals explizites universales Herrschaftsdenken.",
        difficulty = 4,
        funFact = "Die babylonische Weltkarte (Imago Mundi, ca. 600 v. Chr., British Museum) ist die älteste erhaltene Karte der Welt überhaupt."
    ),

    // Question 34 — Neo-Babylonian: Hanging Gardens
    Question(
        categoryId = 3,
        questionText = "Welche moderne Forschungsthese stellt in Frage, dass die Hängenden Gärten wirklich in Babylon unter Nebukadnezar lagen?",
        answerA = "Stephanie Dalley argumentiert auf Basis assyrischer Texte, dass die Gärten in Ninive unter Sanherib angelegt wurden",
        answerB = "Andrew George behauptet, die Gärten existierten nie und seien eine rein literarische Erfindung",
        answerC = "Robert Koldewey fand bei Ausgrabungen in Babylon keinerlei botanische Spuren großer Gartenanlagen",
        answerD = "Martin West zeigte, dass griechische Beschreibungen auf persischen Paradeisos-Gärten basieren",
        correctAnswer = 0, // A
        explanation = "Stephanie Dalley argumentiert in 'The Mystery of the Hanging Garden of Babylon' (2013), dass die Gärten tatsächlich in Ninive unter Sanherib (705–681 v. Chr.) angelegt wurden. Assyrische Texte beschreiben ein aufwändiges Bewässerungssystem, und spätere Quellen könnten 'Babylon' als allgemeinen Begriff für Mesopotamien verwendet haben.",
        difficulty = 4,
        funFact = "Babylon selbst liegt so nahe am Grundwasser, dass üppige Dachgärten technisch kaum möglich gewesen wären — ein Argument für die Ninive-These."
    ),

    // Question 35 — Achaemenid: Behistun inscription
    Question(
        categoryId = 3,
        questionText = "Was berichtet die Behistun-Inschrift (ca. 522–519 v. Chr.) über den Beginn von Darius' Herrschaft, und welche historiographische Debatte löst sie aus?",
        answerA = "Sie beschreibt Darius als rechtmäßigen Erben Kambyses', was von griechischen Quellen bestritten wird",
        answerB = "Darius behauptet, einen Usurpator namens Gaumata erschlagen zu haben, der als falscher Sohn Kyros' aufgetreten war — ob Gaumata der echte Kambyses-Bruder Bardiya war, ist umstritten",
        answerC = "Die Inschrift verschweigt Kambyses und stellt Darius als direkten Nachfolger Kyros' dar",
        answerD = "Sie zeigt Darius als Vertreter Ahura Mazdas, was erste Belege für einen zoroastrischen Staatskult liefert",
        correctAnswer = 1, // B
        explanation = "Darius behauptet in Behistun, den Magier Gaumata getötet zu haben, der sich als Kambyses' Bruder Bardiya ausgab. Ob Gaumata wirklich ein Usurpator war oder ob Darius den echten Bardiya ermordete und ihn post mortem als Betrüger brandmarkte, ist zentrale Debatte. Herodot und Ktesias bieten widersprüchliche Berichte.",
        difficulty = 4,
        funFact = "Henry Rawlinson entzifferte das Altpersische der Behistun-Inschrift 1838–1847, indem er sich unter Lebensgefahr an einem Seil über den Fels abseilen ließ."
    ),

    // Question 36 — Diadochi: Seleucus and India
    Question(
        categoryId = 3,
        questionText = "Welchen Vertrag schloss Seleukos I. um 305 v. Chr. mit Chandragupta Maurya, und was erhielt er als Gegenleistung für das Indusgebiet?",
        answerA = "Er erhielt Tributzahlungen in Gold und das Recht, Elefanten zu kaufen",
        answerB = "Er erhielt 500 Kriegselefanten und tauschte dafür das östliche Alexanderreich bis zum Indus ab",
        answerC = "Er erhielt indische Truppen für seinen Kampf gegen Antigonos und heiratete eine Maurya-Prinzessin",
        answerD = "Er schloss einen Nichtangriffspakt ohne territoriale Zugeständnisse, erhielt aber diplomatische Präsenz",
        correctAnswer = 1, // B
        explanation = "Seleukos tauschte die Satrapien östlich des Hindukusch (Arachosien, Gedrosien, Paropamisadai — ca. 500.000 km²) gegen 500 Kriegselefanten, die er bei Ipsos 301 v. Chr. einsetzte. Dieser Tausch war strategisch rational: Das Gebiet war kaum zu halten, die Elefanten entschieden den Diadochenkrieg.",
        difficulty = 4,
        funFact = "Megasthenes, der Gesandte Seleukos' am Maurya-Hof, schrieb die 'Indica' — die wichtigste antike Beschreibung Indiens, von der nur Fragmente erhalten sind."
    ),

    // Question 37 — Roman Republic: Lex Villia Annalis
    Question(
        categoryId = 3,
        questionText = "Was regelte die Lex Villia Annalis (180 v. Chr.) im römischen Ämterwesen, und welche politische Funktion hatte sie?",
        answerA = "Sie schrieb Mindestalter für jedes Amt vor und definierte die verbindliche Reihenfolge (cursus honorum)",
        answerB = "Sie verbot die Wiederwahl in dasselbe Amt innerhalb von zehn Jahren",
        answerC = "Sie schuf erstmals den Senat als oberstes Beratungsorgan aller Magistrate",
        answerD = "Sie begrenzte die Amtszeit der Diktatur auf sechs Monate",
        correctAnswer = 0, // A
        explanation = "Die Lex Villia Annalis schrieb Mindestalter für die wichtigsten Ämter vor (Quästur, Ädilität, Prätur, Konsulat) und codifizierte damit den cursus honorum als verbindlichen Karrierepfad. Ziel war es, politische Karrieren zu verlangsamen und die Konzentration von Macht in jungen Händen zu verhindern.",
        difficulty = 4,
        funFact = "Julius Caesar verstieß mehrfach gegen den cursus honorum, indem er Ämter übersprang oder das Mindestalter unterbot — ein bewusstes Zeichen gegen die republikanische Tradition."
    ),

    // Question 38 — Hellenistic world: Pyrrhus of Epirus
    Question(
        categoryId = 3,
        questionText = "Was macht die Italicataktik des Pyrrhos von Epirus in der Heeresgeschichte bemerkenswert, und was bedeutet der Begriff 'Pyrrhussieg'?",
        answerA = "Pyrrhos kombinierte Elefanten mit Phalanxtaktik; ein 'Pyrrhussieg' bezeichnet einen Sieg, der mehr kostet als eine Niederlage",
        answerB = "Pyrrhos entwickelte die erste Kavallerietaktik mit Lancen; ein 'Pyrrhussieg' bezeichnet einen militärischen Erfolg ohne strategischen Nutzen",
        answerC = "Pyrrhos führte die Belagerungsmaschinen in den Westen ein; ein 'Pyrrhussieg' kommt vom griechischen pyr (Feuer)",
        answerD = "Pyrrhos schuf die erste kombinierte See-Land-Operation; sein 'Sieg' über Karthago wird als sinnlos bezeichnet",
        correctAnswer = 0, // A
        explanation = "Pyrrhos kombinierte makedonische Phalanxtaktik mit Kriegselefanten und war damit den Römern zunächst überlegen. Nach seinen Siegen bei Herakleia (280) und Ausculum (279 v. Chr.) soll er gesagt haben: 'Noch ein solcher Sieg und ich bin verloren' — seine Verluste waren so hoch, dass ein weiterer 'Sieg' das Heer vernichtet hätte. Daher 'Pyrrhussieg'.",
        difficulty = 4,
        funFact = "Pyrrhos gilt als einer der größten Feldherren der Antike — Hannibal soll ihn als den zweitbesten Feldherren aller Zeiten nach Alexander gerühmt haben."
    ),

    // Question 39 — Ptolemaic: Cleopatra VII's languages
    Question(
        categoryId = 3,
        questionText = "Was unterschied Kleopatra VII. von allen vorherigen ptolemäischen Herrschern linguistisch, und welche politische Bedeutung hatte dies?",
        answerA = "Sie war die erste Ptolemäerin, die Latein sprach, was ihr Verhandlungen mit Caesar erleichterte",
        answerB = "Sie war die erste ptolemäische Herrscherin, die Ägyptisch lernte, was sie zur Einheimischen Priestern zugänglich und zu einer Volksfigur machte",
        answerC = "Sie war die einzige Ptolemäerin, die Aramäisch sprach und damit den Osthandel persönlich kontrollierte",
        answerD = "Sie beherrschte als einzige Ptolemäerin kein Griechisch und führte regierte vollständig auf Ägyptisch",
        correctAnswer = 1, // B
        explanation = "Laut Plutarch war Kleopatra VII. die erste der Ptolemäer, die Ägyptisch lernte — ihre Vorgänger regierten 300 Jahre lang auf Griechisch über ein Land, dessen Sprache sie nicht sprachen. Sie beherrschte zudem neun weitere Sprachen. Das ermöglichte ihr direkten Kontakt zu Priestern und Volk und war Teil ihrer bewussten Inszenierung als Neue Isis.",
        difficulty = 4,
        funFact = "Kleopatra VII. ist in der modernen Vorstellung eine Schönheit, war aber laut antiken Quellen vor allem für ihre Intelligenz, Bildung und Sprachbegabung berühmt."
    ),

    // Question 40 — Late Roman: Ammianus Marcellinus
    Question(
        categoryId = 3,
        questionText = "Welche historiographische Besonderheit macht Ammianus Marcellinus zum wichtigsten Historiker des 4. Jahrhunderts, und in welcher Sprache schrieb er?",
        answerA = "Er war der letzte pagane Historiker, schrieb auf Griechisch und setzte Tacitus bewusst fort",
        answerB = "Er war ein syrischer Grieche, schrieb auf Latein und setzte Tacitus ab dem Jahr 96 n. Chr. fort",
        answerC = "Er war ein christlicher Bischof, schrieb auf Lateinisch und ist die einzige zeitgenössische Quelle für Julian",
        answerD = "Er lebte in Alexandria, schrieb auf Griechisch und war Augenzeuge der Zerstörung der Bibliothek",
        correctAnswer = 1, // B
        explanation = "Ammianus Marcellinus war ein gebürtiger Grieche aus Antiochien, der als Offizier unter Iulianus diente und später auf Latein schrieb. Er setzte ausdrücklich Tacitus fort, begann bei 96 n. Chr. und führte die Geschichte bis 378 n. Chr. Seine Res Gestae sind für das 4. Jh. unersetzlich — vor allem für Julian und die Schlacht von Adrianopel, die er möglicherweise selbst erlebte.",
        difficulty = 4,
        funFact = "Ammianus war Heide, schrieb aber relativ fair über Christen — eine Ausnahme in der polemisch aufgeladenen Literatur des 4. Jahrhunderts."
    ),

    // Question 41 — Hittite: Religious syncretism
    Question(
        categoryId = 3,
        questionText = "Welche religiöse Besonderheit der Hethiter wurde als 'Tausend Götter der Hatti' bezeichnet, und wie entstand dieses Pantheon?",
        answerA = "Die Hethiter hatten das größte polytheistische Pantheon der Antike durch konsequente Synkretisierung aller unterworfenen Kulturen",
        answerB = "Das Pantheon wuchs durch die Praxis, göttliche Statuen unterworfener Städte nach Hattusa zu transferieren",
        answerC = "Die Hethiter glaubten, jede natürliche Erscheinung sei eine eigene Gottheit, was zu Tausenden lokaler Kulte führte",
        answerD = "Das hethitische Staatsritual erforderte die gleichzeitige Verehrung aller Götter der Vasallenstaaten",
        correctAnswer = 1, // B
        explanation = "Die Hethiter deportierten nicht nur Menschen, sondern auch Gottesbilder (Kultstatuen) aus unterworfenen Städten nach Hattusa. Die Götter der Feinde wurden so zu hethitischen Göttern. Bei Friedensschlüssen wurden Götterstatuen manchmal zurückgegeben. Dieses Prinzip der kultischen Inkorporation führte zu einem enormen, aber heterogenen Pantheon.",
        difficulty = 4,
        funFact = "Im hethitischen Gebetstext bittet König Mursili II. um Beistand gegen eine Pest — und entschuldigt sich bei 20 namentlich genannten Göttern für mögliche Versäumnisse."
    ),

    // Question 42 — Assyrian: Tiglath-Pileser III provincial system
    Question(
        categoryId = 3,
        questionText = "Welche Verwaltungsreform machte Tiglatpileser III. gegenüber dem älteren assyrischen Vasallensystem aus, die das Reich dauerhaft stabiler machte?",
        answerA = "Er schaffte alle Vasallenstaaten ab und wandelte sie direkt in assyrische Provinzen mit assyrischen Gouverneuren um",
        answerB = "Er ersetzte lokale Dynastien schrittweise durch assyrische Gouverneure und unterteilte große Provinzen in kleinere, um Machtzentren zu verhindern",
        answerC = "Er führte ein einheitliches Rechtssystem für alle unterworfenen Gebiete ein nach dem Vorbild des Codex Hammurabi",
        answerD = "Er kreierte einen Berufsbeamtenstand, der aus dem persönlichen Gefolge des Königs rekrutiert wurde",
        correctAnswer = 1, // B
        explanation = "Tiglatpileser III. (reg. 745–727 v. Chr.) reformierte die Provinzialverwaltung grundlegend: Große Provinzen wurden in kleinere aufgeteilt, um die Macht einzelner Gouverneure zu begrenzen. Lokale Dynastien wurden durch direkt vom König abhängige Beamte ersetzt. Kombiniert mit der Deportationspolitik schuf er ein wesentlich direkteres Herrschaftssystem.",
        difficulty = 4,
        funFact = "Tiglatpileser III. ist in der Bibel unter dem Namen 'Pul' bekannt (2. Könige 15,19) — ein Zeichen seiner Bedeutung für die zeitgenössische Wahrnehmung."
    ),

    // Question 43 — Seleucid: Antiochus III and Rome
    Question(
        categoryId = 3,
        questionText = "Was waren die Bedingungen des Friedens von Apameia (188 v. Chr.), der das Seleukidenreich nach der Niederlage bei Magnesia (190 v. Chr.) traf?",
        answerA = "Antiochus III. musste ganz Kleinasien westlich des Taurus abtreten, 15.000 Talente zahlen und Hannibal ausliefern",
        answerB = "Das Seleukidenreich wurde auf Syrien und Mesopotamien beschränkt, und Rom erhielt alle Kriegsschiffe",
        answerC = "Antiochus musste seine Flotte auf 10 Schiffe reduzieren, alle Elefanten abgeben und Kleinasien westlich des Halys abtreten",
        answerD = "Der Seleukide verlor Koilesyrien an Ptolemaios und musste 12.000 Talente in jährlichen Raten zahlen",
        correctAnswer = 0, // A
        explanation = "Der Frieden von Apameia war vernichtend: Antiochus III. musste alle Gebiete in Europa und Kleinasien westlich des Taurusgebirges abtreten (an Rom und seine Verbündeten Rhodos und Pergamon), eine Kriegsentschädigung von 15.000 Talenten zahlen, seine Kriegsflotte auf 10 Schiffe reduzieren, alle Elefanten übergeben und Hannibal ausliefern.",
        difficulty = 4,
        funFact = "Der Frieden von Apameia machte Pergamon zur Großmacht Kleinasiens und setzte den Prozess der schrittweisen römischen Kontrolle über den Osten in Gang."
    ),

    // Question 44 — Roman Republic: Lex Hortensia
    Question(
        categoryId = 3,
        questionText = "Was regelte die Lex Hortensia von 287 v. Chr., und warum gilt sie als formaler Abschluss der ständischen Kämpfe in Rom?",
        answerA = "Sie erklärte plebiszitäre Volksversammlungsbeschlüsse (plebiscita) für alle Bürger bindend ohne Senatsgenehmigung",
        answerB = "Sie eröffnete Plebejern den Zugang zu allen Priestämtern inklusive des Pontifex Maximus",
        answerC = "Sie schrieb fest, dass einer der beiden Konsuln immer Plebejer sein musste",
        answerD = "Sie garantierte Volkstribunen vollständige Immunität auch gegen prokonsulare Entscheidungen",
        correctAnswer = 0, // A
        explanation = "Die Lex Hortensia (287 v. Chr.) erklärte Beschlüsse der Tributkomitien (plebiscita) zum allgemein verbindlichen Gesetz, ohne dass der Senat zustimmen musste. Damit hatten die Plebejer formell gleiches Gesetzgebungsrecht erlangt — der symbolische Abschluss des Ständekampfes (494–287 v. Chr.), auch wenn der Senat faktisch dominant blieb.",
        difficulty = 4,
        funFact = "Quintus Hortensius, Urheber des Gesetzes, war als Diktator eingesetzt worden, um einen Plebejersecession (Streik) zu beenden — das Gesetz war sein Kompromissangebot."
    ),

    // Question 45 — Late Roman: Battle of Adrianople
    Question(
        categoryId = 3,
        questionText = "Welche taktische Fehlentscheidung Kaiser Valens' unmittelbar vor der Schlacht von Adrianopel (378 n. Chr.) gilt als mitursächlich für die katastrophale Niederlage?",
        answerA = "Er ließ seine Reiterei zu früh abziehen, um Nahrung zu suchen, was das linke Flügel ungesichert ließ",
        answerB = "Er griff an, bevor Gratians Verstärkungsheer eintraf, und begann den Angriff ohne abgeschlossene Aufstellung während die gotische Reiterei noch unterwegs war",
        answerC = "Er ließ sich durch gefälschte Friedensverhandlungen von Fritigern hinhalten, bis die Goten ihre Wagenburg vollständig befestigt hatten",
        answerD = "Er setzte seine schwere Kavallerie als erste Linie ein, die durch den gotischen Pfeilhagel vernichtet wurde",
        correctAnswer = 1, // B
        explanation = "Valens wollte nicht auf Gratians Heer warten und griff an, bevor seine eigene Aufstellung abgeschlossen war. Fritigernis gotische Reiterei war von einem Ausritt zurückgekehrt und traf die noch im Marsch befindlichen römischen Flanken. Die gotische Kavallerie rollte die unfertig aufgestellten Römer auf. Ca. 2/3 des Ostheers fiel, darunter Valens selbst.",
        difficulty = 4,
        funFact = "Adrianopel 378 n. Chr. gilt als Beginn des Endes des Weströmischen Reichs — obwohl es noch fast ein Jahrhundert dauerte. Edward Gibbon begann hier seine Analyse des 'Verfalls'."
    ),

    // Question 46 — Achaemenid: Zoroastrianism
    Question(
        categoryId = 3,
        questionText = "Welche Debatte besteht in der Wissenschaft darüber, ob die Achämenidenkönige tatsächlich Zoroastrier waren, und welches Hauptargument spricht gegen eine strikte zoroastrische Staatsreligion?",
        answerA = "Die Könige verehrten nachweislich auch Marduk, Bel und andere babylonische Götter, was zoroastrischer Exklusivität widerspricht",
        answerB = "Es gibt keine hethitischen Texte, die Ahura Mazda erwähnen, was auf späte Einführung hindeutet",
        answerC = "Die Behistun-Inschrift nennt Ahura Mazda nicht, was zeigt, dass die Religion erst unter Darius II. eingeführt wurde",
        answerD = "Zarathustra lebte nach moderner Forschung erst im 3. Jh. v. Chr. und kann die Achämeniden nicht beeinflusst haben",
        correctAnswer = 0, // A
        explanation = "Achämenidische Könige nannten sich 'durch Ahura Mazdas Gnade', verwendeten aber auch andere Götter in ihren Inschriften (Mithra, Anahita) und verehrten in Babylon Marduk und andere Götter. Kyros ließ den Marduk-Kult restaurieren. Dies passt nicht zu einer exklusiv zoroastrischen Theologie und legt synkretistische Religionspolitik nahe.",
        difficulty = 4,
        funFact = "Das Alte Testament nennt Kyros II. den 'Gesalbten des Herrn' (Jesaja 45,1) — eine einzigartige Ehrung für einen nichtjüdischen König in der hebräischen Bibel."
    ),

    // Question 47 — Diadochi: Lysimachus and Seleucus
    Question(
        categoryId = 3,
        questionText = "Was löste die letzte große Diadochenschlacht bei Kurupedion (281 v. Chr.) aus, bei der Lysimachos fiel, und welche Ironie kennzeichnet ihren Ausgang?",
        answerA = "Lysimachos griff Seleukos an, weil dieser seinen Sohn Agathokles ermorden lassen hatte; Seleukos wurde wenige Monate nach seinem Sieg in Europa ermordet",
        answerB = "Seleukos wollte Makedonien und das gesamte Alexanderreich wieder vereinigen, scheiterte aber kurz vor dem Ziel",
        answerC = "Ptolemaios III. provozierte den Krieg durch Gebietsansprüche in Thrakien; beide Könige starben in der Schlacht",
        answerD = "Der Krieg entstand durch einen Streit um den Besitz von Korinth als Symbol griechischer Hegemonie",
        correctAnswer = 0, // A
        explanation = "Seleukos nutzte den Mordvorwurf (Lysimachos hatte seinen Sohn Agathokles auf Betreiben seiner zweiten Frau Arsinoe töten lassen) als Vorwand. Er besiegte Lysimachos bei Kurupedion — damit war er der letzte Überlebende der Diadochengeneration. Doch wenige Monate später wurde Seleukos in Europa von Ptolemaios Keraunos ermordet, dem er vertraut hatte.",
        difficulty = 4,
        funFact = "Ptolemaios Keraunos ('der Donner'), der Seleukos ermordete, war ein Sohn Ptolemaios I. und hatte nach Thronverlust in Ägypten jahrelang als Abenteurer gelebt."
    ),

    // Question 48 — Theodosius: Ambrose of Milan
    Question(
        categoryId = 3,
        questionText = "Was war die historische Bedeutung von Bischof Ambrosius' Buß-Forderung an Theodosius nach dem Massaker von Thessalonika (390 n. Chr.)?",
        answerA = "Ambrosius zwang den Kaiser erstmals in der Geschichte zur öffentlichen Buße, was das Prinzip der kirchlichen Überordnung über weltliche Herrscher begründete",
        answerB = "Die Buße war rein symbolisch und hatte keine praktische Auswirkung auf die kaiserliche Macht",
        answerC = "Theodosius verweigerte die Buße, was zum Schisma zwischen Mailand und Konstantinopel führte",
        answerD = "Ambrosius wurde wegen Hochverrats angeklagt, weil er sich dem Kaiser widersetzte",
        correctAnswer = 0, // A
        explanation = "Nach dem Massaker von Thessalonika (ca. 7.000 Tote auf kaiserlichen Befehl) verweigerte Ambrosius dem Kaiser die Kommunion bis zur öffentlichen Buße. Theodosius unterwarf sich. Dies war ein Präzedenzfall: Ein Kaiser musste kirchliche Autorität in moralischen Fragen anerkennen — eine theologisch-politische Weichenstellung für das gesamte Mittelalter.",
        difficulty = 4,
        funFact = "Ambrosius war erst wenige Tage nach seiner Taufe zum Bischof gewählt worden — er war zuvor nur Katechumene, was sein Einfluss später noch bemerkenswerter machte."
    ),

    // Question 49 — Roman Republic: Mithridatic Wars
    Question(
        categoryId = 3,
        questionText = "Was war der 'Asiatische Vesper' (88 v. Chr.), und wie nutzte Sulla dieses Ereignis innenpolitisch in Rom?",
        answerA = "Mithridates VI. ließ an einem Tag alle Römer und Italiker in Kleinasien töten (ca. 80.000 Menschen); Sulla nutzte dies, um seinen Anspruch auf den Oberbefehl gegen Marius durchzusetzen",
        answerB = "Ein Massaker an pontischen Gesandten in Rom, das Sulla als Kriegsgrund gegen Mithridates nutzte",
        answerC = "Eine pontische Seeschlacht, bei der Sullas Flotte fast vernichtet wurde, die er nutzte, um Notstandsgesetze durchzusetzen",
        answerD = "Der plötzliche Tod von Sullas Verbündetem Murena in Pontos, den Sulla Marius anlastete",
        correctAnswer = 0, // A
        explanation = "Mithridates VI. Eupator ordnete an einem Tag die Tötung aller Römer und Italiker in der Provinz Asia an — antike Schätzungen nennen 80.000 Opfer. Dies löste den Ersten Mithridatischen Krieg aus. Sulla, der den Oberbefehl hatte, wurde von Marius mit Volkstribunsgesetz abgelöst. Sulla marschierte daraufhin erstmals in der Geschichte mit einem Heer auf Rom — ein unerhörter Zivilisationsbruch.",
        difficulty = 4,
        funFact = "Mithridates VI. sprach angeblich 22 Sprachen ohne Dolmetscher — was ihm den Beinamen Eupatores ('aus edlem Vater') einbrachte."
    ),

    // Question 50 — Late Roman: Valentinian's death
    Question(
        categoryId = 3,
        questionText = "Wie starb Kaiser Valentinian I. (375 n. Chr.), und was offenbart diese Todesursache über die extreme Belastung spätrömischer Kaiser?",
        answerA = "Er wurde bei der Niederschlagung eines Germanenaufstands getötet",
        answerB = "Er erlitt einen Schlaganfall (tödlichen Blutungsanfall) während einer wutentbrannten Audienz mit Quaden-Gesandten",
        answerC = "Er starb an den Folgen einer Verwundung in der Schlacht gegen Alemannen am Oberrhein",
        answerD = "Er wurde von seinem Sohn Gratian vergiftet, der ungeduldig auf den Thron wartete",
        correctAnswer = 1, // B
        explanation = "Valentinian I. starb 375 n. Chr. während einer Audienz mit Gesandten der Quaden, die dreist über Übergriffe gegen sein Reich sprachen. In einem Wutanfall erlitt er einen Schlaganfall (tödliche Gehirnblutung). Dies symbolisiert die extreme physische und psychische Belastung spätrömischer Kaiser, die ständig zwischen Militärfeldzügen, Verwaltungskrisen und Diplomatieversagen navigieren mussten.",
        difficulty = 4,
        funFact = "Valentinian I. war einer der letzten starken weströmischen Kaiser — nach ihm regierten fast nur noch Kinder oder machtlose Marionetten bis 476 n. Chr."
    ),
)
