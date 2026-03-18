package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsExpert3(): List<Question> = listOf(

    // --- Peace of Augsburg details ---
    Question(
        categoryId = 3,
        questionText = "Welcher lateinische Grundsatz des Augsburger Religionsfriedens (1555) legte fest, dass der Landesherr die Konfession seiner Untertanen bestimmt?",
        answerA = "Cujus regio, ejus religio",
        answerB = "Extra ecclesiam nulla salus",
        answerC = "Ius reformandi",
        answerD = "Reservatum ecclesiasticum",
        correctAnswer = 0,
        explanation = "Der Grundsatz 'Cuius regio, eius religio' (wessen Land, dessen Religion) war das Kernprinzip des Augsburger Religionsfriedens: Der Herrscher eines Territoriums bestimmte, ob seine Untertanen lutherisch oder katholisch sein mussten.",
        difficulty = 4,
        funFact = "Den Begriff prägte erst 1586 der Jurist Joachim Stephani – der Augsburger Frieden selbst formulierte das Prinzip umständlicher als 'geistliche Vorbehalt' und 'Ius Reformandi'."
    ),

    Question(
        categoryId = 3,
        questionText = "Was regelte der 'Geistliche Vorbehalt' (Reservatum Ecclesiasticum) im Augsburger Religionsfrieden von 1555?",
        answerA = "Protestanten durften keine Kirchensteuern erheben",
        answerB = "Wenn ein Bischof oder Abt zum Protestantismus übertrat, verlor er sein Amt und seine weltlichen Güter",
        answerC = "Geistliche Güter waren für alle Konfessionen gesperrt",
        answerD = "Klöster blieben unter kaiserlicher Aufsicht, unabhängig von der Landesreligion",
        correctAnswer = 1,
        explanation = "Das Reservatum Ecclesiasticum besagte: Wenn ein Bischof, Abt oder anderer geistlicher Fürst zum evangelischen Glauben übertrat, musste er sein Amt und die damit verbundenen weltlichen Güter aufgeben. Die Protestanten erkannten diese Klausel nie offiziell an.",
        difficulty = 4,
        funFact = "Die Protestanten weigerten sich, das Reservatum im Vertragstext anzuerkennen. Kaiser Ferdinand I. ließ es einseitig als kaiserliche Deklaration einfügen – ein Kompromiss, der den späteren Konflikt vorprogrammierte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Konfession wurde durch den Augsburger Religionsfrieden von 1555 ausdrücklich NICHT anerkannt?",
        answerA = "Lutheraner (Anhänger der Augsburger Konfession)",
        answerB = "Katholiken",
        answerC = "Calvinisten (Reformierte)",
        answerD = "Zwinglianern",
        correctAnswer = 2,
        explanation = "Der Augsburger Religionsfrieden anerkannte nur zwei Konfessionen: Katholiken und Lutheraner (Anhänger der Confessio Augustana von 1530). Calvinisten, Zwinglianern und Täufer wurden ausdrücklich ausgeschlossen – was später zum Dreißigjährigen Krieg beitrug.",
        difficulty = 4,
        funFact = "Kurfürst Friedrich III. von der Pfalz konvertierte 1563 zum Calvinismus und riskierte damit seinen Kurstand, da Calvinisten nicht unter den Schutz des Augsburger Friedens fielen."
    ),

    // --- Edict of Nantes ---
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr und von welchem König wurde das Edikt von Nantes erlassen?",
        answerA = "1562 von Karl IX.",
        answerB = "1589 von Heinrich III.",
        answerC = "1598 von Heinrich IV.",
        answerD = "1610 von Ludwig XIII.",
        correctAnswer = 2,
        explanation = "Das Edikt von Nantes wurde 1598 von König Heinrich IV. (früher Hugenotte, dann konvertierter Katholik) erlassen. Es gewährte den Hugenotten Religionsfreiheit, bürgerliche Gleichstellung und militärische Sicherheitsplätze (places de sûreté).",
        difficulty = 4,
        funFact = "Heinrich IV. soll gesagt haben: 'Paris vaut bien une messe' (Paris ist eine Messe wert) – ob authentisch oder nicht, illustriert es seinen pragmatischen Konfessionswechsel zum Katholizismus, um König von Frankreich zu werden."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele 'places de sûreté' (Sicherheitsplätze) erhielten die Hugenotten im Edikt von Nantes, die sie militärisch halten durften?",
        answerA = "Etwa 50",
        answerB = "Etwa 100",
        answerC = "Etwa 200",
        answerD = "Etwa 500",
        correctAnswer = 2,
        explanation = "Das Edikt von Nantes gewährte den Hugenotten etwa 200 befestigte Plätze, darunter wichtige Städte wie La Rochelle und Montauban. Diese 'places de sûreté' machten die Hugenotten faktisch zu einem Staat im Staat.",
        difficulty = 4,
        funFact = "Richelieu sah diese Hugenottenplätze als Bedrohung für die Zentralmacht. Nach dem Fall von La Rochelle 1628 entzog die Gnade von Alès den Hugenotten alle Sicherheitsplätze, beließ ihnen aber die Religionsfreiheit."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer widerrief das Edikt von Nantes, und in welchem Jahr?",
        answerA = "Ludwig XIV., 1685 mit dem Edikt von Fontainebleau",
        answerB = "Kardinal Richelieu, 1629 mit der Gnade von Alès",
        answerC = "Ludwig XIII., 1635 beim Eintritt in den Dreißigjährigen Krieg",
        answerD = "Ludwig XV., 1715 nach dem Tod Ludwigs XIV.",
        correctAnswer = 0,
        explanation = "Ludwig XIV. widerrief das Edikt von Nantes 1685 mit dem Edikt von Fontainebleau. Daraufhin flohen etwa 200.000–400.000 Hugenotten aus Frankreich, darunter wertvolle Handwerker, Kaufleute und Militärs.",
        difficulty = 4,
        funFact = "Brandenburg-Preußen profitierte enorm: Friedrich Wilhelm I. erließ das Edikt von Potsdam (1685) und bot Hugenotten Zuflucht. Etwa 20.000 kamen und brachten Seidenweberei, Glasherstellung und andere Handwerke mit."
    ),

    // --- Treaty of Tordesillas ---
    Question(
        categoryId = 3,
        questionText = "Auf welchem Längengrad legte der Vertrag von Tordesillas (1494) die Demarkationslinie zwischen spanischen und portugiesischen Einflussgebieten fest?",
        answerA = "46° 37' West",
        answerB = "30° West",
        answerC = "60° West",
        answerD = "370 Leagues westlich der Kapverdischen Inseln (ca. 46° 37' West)",
        correctAnswer = 3,
        explanation = "Der Vertrag von Tordesillas legte die Grenze 370 Seemeilen (leguas) westlich der Kapverdischen Inseln fest – entspricht etwa 46° 37' West. Alles östlich davon gehörte Portugal, alles westlich davon Spanien.",
        difficulty = 4,
        funFact = "Weil Brasilien östlich dieser Linie liegt, kam es unter portugiesische Herrschaft, was erklärt, warum Brasilianer heute Portugiesisch sprechen – während der Rest Südamerikas Spanisch spricht."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche päpstliche Bulle von 1493 bildete die Grundlage für den Vertrag von Tordesillas?",
        answerA = "Dum Diversas (1452)",
        answerB = "Romanus Pontifex (1455)",
        answerC = "Inter Caetera (1493)",
        answerD = "Sublimis Deus (1537)",
        correctAnswer = 2,
        explanation = "Papst Alexander VI. (Rodrigo Borgia) erließ 1493 die Bulle Inter Caetera, die Spanien das Recht auf alle neu entdeckten Gebiete westlich einer Meridianline gab. Portugal akzeptierte dies nicht, woraufhin 1494 der Vertrag von Tordesillas ausgehandelt wurde.",
        difficulty = 4,
        funFact = "König Johann II. von Portugal nutzte seinen Verhandlungsgeschick, um die Linie von den ursprünglich geplanten 100 auf 370 Seemeilen westlich der Kapverden zu verschieben – genug, um Brasilien einzuschließen, obwohl Cabral es erst 1500 'entdeckte'."
    ),

    // --- Dutch East India Company (VOC) ---
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Vereinigte Ostindische Compagnie (VOC) gegründet, und welches besondere Recht erhielt sie durch das niederländische Parlament?",
        answerA = "1600, monopolistisches Handelsrecht mit dem Fernen Osten und eigene Schiffs- und Handelsflagge",
        answerB = "1602, das Recht zur Kriegsführung, Vertragsabschlüssen und Münzprägung im eigenen Namen",
        answerC = "1609, das Recht auf eigene Kolonien ohne staatliche Aufsicht",
        answerD = "1621, das Recht auf Sklavenhandel in Asien und dem Pazifik",
        correctAnswer = 1,
        explanation = "Die VOC wurde 1602 gegründet und erhielt vom niederländischen Generalstaat einzigartige quasi-staatliche Befugnisse: das Recht, Kriege zu führen, Verträge mit Fürsten zu schließen, Festungen zu bauen und eigene Münzen zu prägen.",
        difficulty = 4,
        funFact = "Die VOC war das erste Unternehmen der Geschichte, das Aktien ausgab, die an der Börse frei gehandelt wurden – und damit der Vorgänger des modernen Aktienmarkts."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die sogenannte 'Banda-Massaker' von 1621 im Kontext der VOC?",
        answerA = "Vernichtung der einheimischen Bevölkerung der Banda-Inseln durch Jan Pieterszoon Coen zur Kontrolle des Muskatnussmonopols",
        answerB = "Angriff portugiesischer Kräfte auf niederländische Handelsstationen in Ostindien",
        answerC = "Aufstand der Banda-Einwohner gegen englische Händler",
        answerD = "Seeschlacht zwischen VOC und Englischer Ostindien-Kompanie um die Banda-Inseln",
        correctAnswer = 0,
        explanation = "Jan Pieterszoon Coen, Generalgouverneur der VOC, ließ 1621 die Bevölkerung der Banda-Inseln fast vollständig ausrotten (von ca. 15.000 auf einige Hundert), um das Monopol auf Muskatnuss und Macis zu sichern. Die Inseln wurden dann mit Sklaven und Niederländern neu besiedelt.",
        difficulty = 4,
        funFact = "England und die Niederlande tauschten 1667 im Vertrag von Breda: England gab Nieuw Amsterdam (New York) auf und erhielt im Gegenzug die kleine Banda-Insel Run – die Niederlande wollten ihr Muskatnuss-Monopol schützen."
    ),

    // --- British East India Company ---
    Question(
        categoryId = 3,
        questionText = "Mit welcher Entscheidungsschlacht von 1757 begründete die Britische Ostindienkompanie ihre Herrschaft in Bengalen?",
        answerA = "Schlacht von Plassey (Palashi)",
        answerB = "Schlacht von Buxar",
        answerC = "Erste Mysore-Krieg",
        answerD = "Maratha-Krieg",
        correctAnswer = 0,
        explanation = "In der Schlacht von Plassey (23. Juni 1757) besiegte Robert Clive mit nur 3.000 Soldaten das 50.000 Mann starke Heer des Nawabs von Bengalen, Siraj ud-Daulah, der durch Verrat geschwächt wurde. Dies legte den Grundstein für die britische Kontrolle über Bengalen.",
        difficulty = 4,
        funFact = "Robert Clive wurde durch die Plünderung Bengalens steinreich und erhielt enorme persönliche Bestechungsgelder. Später vor dem Parlament zu seinen Bereicherungen befragt, sagte er: 'Ich stehe erstaunt über meine eigene Mäßigung.'"
    ),

    Question(
        categoryId = 3,
        questionText = "Welches britische Parlamentsgesetz von 1773 setzte erstmals staatliche Aufsicht über die Britische Ostindien-Kompanie durch?",
        answerA = "India Act 1784",
        answerB = "Regulating Act 1773",
        answerC = "Charter Act 1813",
        answerD = "Government of India Act 1858",
        correctAnswer = 1,
        explanation = "Der Regulating Act von 1773 war das erste britische Parlamentsgesetz zur direkten Kontrolle der Ostindien-Kompanie. Er schuf den Posten des Generalgouverneurs (Warren Hastings wurde der erste) und einen Obersten Gerichtshof in Kalkutta.",
        difficulty = 4,
        funFact = "Der Regulating Act entstand, weil die Kompanie nach der Hungersnot in Bengalen (1770, die ein Drittel der Bevölkerung tötete) finanziell pleite war und Notfallkredite von der britischen Regierung erbetteln musste."
    ),

    // --- Slave Trade Economics ---
    Question(
        categoryId = 3,
        questionText = "Was bezeichnet das ökonomische System des 'Dreieckshandels' im atlantischen Sklavenhandel?",
        answerA = "Handel zwischen England, Frankreich und Portugal mit Sklaven als Zahlungsmittel",
        answerB = "Europa exportierte Waren nach Afrika, Afrika lieferte Sklaven nach Amerika, Amerika sandte Rohstoffe nach Europa",
        answerC = "Tauschhandel zwischen drei afrikanischen Reichen und europäischen Händlern",
        answerD = "Dreistufiges Auktionssystem für Sklaven in der Karibik",
        correctAnswer = 1,
        explanation = "Der Dreieckshandel funktionierte in drei Etappen: 1) Europäer transportierten Waren (Textilien, Waffen, Alkohol) nach Westafrika. 2) Afrikanische Zwischenhändler tauschten Sklaven gegen diese Waren; Sklaven wurden auf der 'Middle Passage' nach Amerika gebracht. 3) In Amerika wurden Sklaven gegen Rohstoffe (Zucker, Tabak, Baumwolle) getauscht, die nach Europa gingen.",
        difficulty = 4,
        funFact = "Die 'Middle Passage' – die Überfahrt von Afrika nach Amerika – dauerte 4–8 Wochen. Sklavenspezifische Sterblichkeitsrate lag bei etwa 12–15%. Insgesamt überquerten ca. 12,5 Millionen Menschen als Sklaven den Atlantik."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr verabschiedete das britische Parlament den Slave Trade Act, der den Sklavenhandel (nicht die Sklaverei selbst) im Britischen Empire verbot?",
        answerA = "1772 (Somerset-Urteil)",
        answerB = "1807",
        answerC = "1833 (Slavery Abolition Act)",
        answerD = "1838",
        correctAnswer = 1,
        explanation = "Der Slave Trade Act von 1807 verbot den Sklavenhandel (den Transport neuer Sklaven) im Britischen Empire. Die Sklaverei selbst wurde erst 1833 mit dem Slavery Abolition Act abgeschafft, der ab 1834 wirksam wurde.",
        difficulty = 4,
        funFact = "Großbritannien entschädigte nach 1833 die Sklavenhalter mit 20 Millionen Pfund Sterling – die befreiten Sklaven selbst erhielten nichts. Diese Schulden wurden erst 2015 vollständig vom britischen Steuerzahler abbezahlt."
    ),

    // --- Seven Years War ---
    Question(
        categoryId = 3,
        questionText = "Warum gilt der Siebenjährige Krieg (1756–1763) oft als der erste 'Weltkrieg'?",
        answerA = "Weil alle europäischen Großmächte gleichzeitig Krieg führten",
        answerB = "Weil die Kämpfe auf fünf Kontinenten stattfanden: Europa, Nordamerika, Südamerika, Afrika und Asien",
        answerC = "Weil erstmals Atombwaffen (Brandgeschosse) eingesetzt wurden",
        answerD = "Weil die Vereinten Nationen als Vorläuferinstitution gegründet wurden",
        correctAnswer = 1,
        explanation = "Der Siebenjährige Krieg fand tatsächlich auf mehreren Kontinenten statt: Europa (Preußen vs. Österreich/Frankreich/Russland), Nordamerika (French and Indian War: Britisch vs. Frankreich), Karibik, Indien, West- und Ostafrika sowie auf den Weltmeeren. Winston Churchill nannte ihn später den 'ersten Weltkrieg'.",
        difficulty = 4,
        funFact = "Preußen mit 5 Millionen Einwohnern kämpfte gleichzeitig gegen Österreich (20 Mio.), Frankreich (25 Mio.), Russland (30 Mio.) und Schweden – und überlebte nur durch Friedrichs des Großen Genie und das 'Mirakel des Hauses Brandenburg' (Russlands Rückzug 1762)."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Bündnissystem kehrte die traditionellen europäischen Allianzen 1756 um und wird als 'Diplomatische Revolution' bezeichnet?",
        answerA = "Österreich verbündete sich mit Preußen gegen Frankreich und Russland",
        answerB = "Frankreich und Österreich (frühere Erbfeinde) verbündeten sich, während Großbritannien und Preußen ein Bündnis schlossen",
        answerC = "Russland wechselte von der österreichischen zur britischen Seite",
        answerD = "Die Osmanen traten dem Bündnis gegen Preußen bei",
        correctAnswer = 1,
        explanation = "Die 'Diplomatische Revolution' von 1756 war ein radikaler Bruch mit jahrhundertealten Bündnisstrukturen: Das seit 250 Jahren verfeindete Paar Frankreich-Habsburg verbündete sich (erster Versailler Vertrag, Mai 1756). Gleichzeitig schlossen Großbritannien und Preußen das Westminsterkonvention.",
        difficulty = 4,
        funFact = "Dieser Bündniswechsel wurde durch die Heirat zwischen Marie Antoinette (Tochter Maria Theresias) und dem späteren Ludwig XVI. 1770 symbolisch besiegelt – ein Bündnis, das durch die Französische Revolution gesprengt wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Friedensvertrag von 1763 beendete den Siebenjährigen Krieg zwischen Großbritannien und Frankreich, und welche Gebiete tauschten die Parteien?",
        answerA = "Vertrag von Hubertusburg: Preußen gab Schlesien zurück",
        answerB = "Vertrag von Paris: Frankreich gab Kanada und Ostlouisiana an Großbritannien ab, erhielt aber Guadeloupe und Martinique zurück",
        answerC = "Vertrag von Utrecht: Frankreich gab die Karibik auf, behielt aber Kanada",
        answerD = "Vertrag von Fontainebleau: Frankreich und Spanien tauschten Louisiana gegen Florida",
        correctAnswer = 1,
        explanation = "Im Vertrag von Paris (1763) trat Frankreich Kanada und alle Gebiete östlich des Mississippi an Großbritannien ab. Frankreich erhielt im Tausch die wirtschaftlich wertvolleren Zuckerinseln Guadeloupe und Martinique zurück. Spanien trat Florida ab und erhielt dafür Westlouisiana von Frankreich.",
        difficulty = 4,
        funFact = "In Frankreich debattierte man heftig: 'Kanada oder Guadeloupe?' – Voltaire spottete über Kanada als 'quelques arpents de neige' (einige Morgen Schnee). Viele Franzosen bevorzugten die profitablen Zuckerinseln, was die Entscheidung beeinflusste."
    ),

    // --- American Revolution ---
    Question(
        categoryId = 3,
        questionText = "Bei welcher militärischen Wendeschlacht von 1777 kapitulierten 5.800 britische Soldaten unter General Burgoyne, was Frankreich zur Allianz mit den amerikanischen Kolonien bewog?",
        answerA = "Battle of Bunker Hill",
        answerB = "Battle of Trenton",
        answerC = "Battle of Saratoga",
        answerD = "Battle of Yorktown",
        correctAnswer = 2,
        explanation = "Die Battle of Saratoga (Oktober 1777) war die Wendeschlacht des Amerikanischen Unabhängigkeitskriegs. General Burgoynes gescheiterter 'Grand Strategy' zur Teilung der Kolonien endete mit der Kapitulation von 5.800 Mann. Dies überzeugte Frankreich, im Februar 1778 ein formales Bündnis mit den USA zu schließen.",
        difficulty = 4,
        funFact = "Der polnische Brigadier Thaddeus Kosciuszko spielte eine entscheidende Rolle bei Saratoga: Er wählte die strategisch überlegene Verteidigungsstellung auf Bemis Heights und koordinierte die Befestigungen, die Burgoynes Vormarsch stoppten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was waren die Articles of Confederation (1781) und warum scheiterten sie?",
        answerA = "Die erste Verfassung der USA – sie scheiterten weil der Zentralregierung das Recht zur Besteuerung und zur Durchsetzung von Gesetzen fehlte",
        answerB = "Ein Bündnisvertrag zwischen den Kolonien und Frankreich – er scheiterte am Widerstand Britanniens",
        answerC = "Die Verfassung von Massachusetts – sie scheiterte an der Shays Rebellion",
        answerD = "Ein Handelsvertrag zwischen den Nordstaaten – er scheiterte an Südstaaten-Opposition",
        correctAnswer = 0,
        explanation = "Die Articles of Confederation waren die erste Verfassung der USA (1781–1789). Sie scheiterten, weil der Zentralregierung entscheidende Befugnisse fehlten: kein Recht zur Besteuerung (nur Bitten an Einzelstaaten), keine eigene Armee, keine Handelsregulierung, Einstimmigkeit bei Verfassungsänderungen erforderlich.",
        difficulty = 4,
        funFact = "Die Articles of Confederation erlaubten den USA, Kanada einzuladen, dem Bund beizutreten – was Kanada ablehnte. Dieses Angebot, Kanada als 14. Staat aufzunehmen, steht technisch noch immer im Dokument."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Shays Rebellion' (1786–1787) und welche Wirkung hatte sie auf die amerikanische Verfassungsgebung?",
        answerA = "Ein britischer Gegenangriff nach dem Friedensvertrag; er zeigte die militärische Schwäche der USA",
        answerB = "Ein Bauernaufstand in Massachusetts gegen Schulden und Steuern; er zeigte die Unfähigkeit der Confederation, innere Aufstände zu unterdrücken",
        answerC = "Eine Revolte der Loyalisten in New York; sie beschleunigte die Ausweisung britischer Sympathisanten",
        answerD = "Ein Streik der Drucker und Zeitungsmacher; er führte zur Pressefreiheitsgarantie in der Bill of Rights",
        correctAnswer = 1,
        explanation = "Daniel Shays führte 1786–1787 Massachusetts-Bauern an, die durch Schulden und Steuern nach dem Krieg verarmt waren. Die Rebellion erschreckte die Elite, weil die Bundesregierung keine Armee hatte, um sie zu unterdrücken. Dies war ein Hauptantrieb für die Verfassungskonvention von Philadelphia 1787.",
        difficulty = 4,
        funFact = "George Washington, der auf seinem Gut Ruhe gesucht hatte, wurde durch Shays Rebellion alarmiert und schrieb: 'Wenn wir keinen kräftigen Regierungsrahmen haben, werden wir bald in die tiefste Anarquía fallen.' Dieser Brief überzeugte viele, die Verfassungskonvention zu unterstützen."
    ),

    Question(
        categoryId = 3,
        questionText = "Bei welcher Seeschlacht 1781 entschied Frankreich den Amerikanischen Unabhängigkeitskrieg zu Gunsten der Colonisten, indem es die britische Flotte verdrängte?",
        answerA = "Battle of Flamborough Head (1779)",
        answerB = "Battle of the Chesapeake (Virginia Capes, 1781)",
        answerC = "Battle of Minorca (1782)",
        answerD = "Battle of the Saintes (1782)",
        correctAnswer = 1,
        explanation = "Die Battle of the Chesapeake (5. September 1781) war strategisch entscheidend: Admiral de Grasse schlug die britische Flotte und schnitt Cornwallis' Armee in Yorktown von der Versorgung ab. Ohne Seeunterstützung musste Cornwallis kapitulieren, was den Krieg effektiv beendete.",
        difficulty = 4,
        funFact = "Die meisten Menschen kennen Yorktown (Oktober 1781) als entscheidende Kapitulation – aber die eigentliche militärische Entscheidung fiel drei Wochen früher auf See. Ohne de Grasses Sieg bei den Virginia Capes hätte Cornwallis entkommen oder verstärkt werden können."
    ),

    // --- French Revolution Historiography ---
    Question(
        categoryId = 3,
        questionText = "Welche Historikerposition vertritt die 'revisionistische' Interpretation der Französischen Revolution gegen die klassische 'marxistische' Interpretation?",
        answerA = "Die Revolution war kein Klassenkampf des Bürgertums gegen den Adel, sondern ein komplexes politisches Ereignis ohne klare soziale Basis",
        answerB = "Die Revolution war von England finanziert worden, um Frankreich zu schwächen",
        answerC = "Die Revolution war eine Konterrevolution des Adels gegen die Bauernschaft",
        answerD = "Die Revolution war primär eine religiöse Bewegung gegen den Klerikalismus",
        correctAnswer = 0,
        explanation = "Die revisionistische Schule (Alfred Cobban, Denis Richet, François Furet) bestritt das marxistische Narrativ, die Revolution sei ein Sieg des kapitalistischen Bürgertums über den feudalen Adel gewesen. Sie zeigten, dass die Revolutionsführer selbst oft Anwälte und Beamte waren, und der Adel teilweise modernisierend wirkte.",
        difficulty = 4,
        funFact = "François Furet – ehemaliger Kommunist – wurde zum schärfsten Kritiker der marxistischen Revolutionsinterpretation. Sein 1978 erschienenes Werk 'Penser la Révolution française' löste in Frankreich einen akademischen Kulturkampf aus."
    ),

    Question(
        categoryId = 3,
        questionText = "Was versteht man unter dem 'atlantischen Revolutionsparadigma' (R.R. Palmer, Jacques Godechot)?",
        answerA = "Die Theorie, dass alle atlantischen Demokratien nach 1776 durch geheime Freimaurernetzwerke verbunden waren",
        answerB = "Die Interpretation, dass die Amerikanische, Französische und andere Revolutionen Teil einer gemeinsamen demokratischen Revolution im atlantischen Raum waren",
        answerC = "Das Konzept, dass Revolutionen zyklisch im Abstand von einer Generation auftreten",
        answerD = "Die Erklärung der Französischen Revolution als direkte Folge des atlantischen Sklavenhandels",
        correctAnswer = 1,
        explanation = "R.R. Palmer und Jacques Godechot entwickelten in den 1950ern das Konzept einer 'Demokratischen Revolution' im atlantischen Raum (ca. 1760–1800): Amerikanische, Französische, Batavische, Helvetische und andere Revolutionen bildeten demnach ein zusammenhängendes Phänomen mit gemeinsamen liberalen Idealen.",
        difficulty = 4,
        funFact = "Das 'atlantische Paradigma' war im Kalten Krieg politisch aufgeladen: Palmer und Godechot wollten zeigen, dass die liberale Demokratie eine westliche, atlantische Tradition war – als Gegengewicht zur sowjetischen Revolutionsinterpretation."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bezeichnet der Begriff 'Thermidorianische Reaktion' in der Geschichte der Französischen Revolution?",
        answerA = "Die Erhebung Napoleon Bonapartes zum Ersten Konsul im November 1799",
        answerB = "Den Sturz Robespierres am 9. Thermidor II (27. Juli 1794) und die anschließende Abkehr von der jakobinischen Terrorherrschaft",
        answerC = "Die Flucht Ludwigs XVI. nach Varennes im Juni 1791",
        answerD = "Die royalistische Gegenrevolution in der Vendée (1793)",
        correctAnswer = 1,
        explanation = "Die 'Thermidorianische Reaktion' bezeichnet den Sturz Robespierres am 9. Thermidor des Jahres II (27. Juli 1794) durch Mitglieder des Wohlfahrtsausschusses, die um ihr eigenes Leben fürchteten. Die Schreckensherrschaft endete, Jakobiner wurden verfolgt ('Weißer Terror'), und ein konservativeres Direktorium übernahm die Macht.",
        difficulty = 4,
        funFact = "Karl Marx verwendete 'Thermidorianische Reaktion' als politischen Begriff für jede konservative Gegenbewegung nach einer Revolution. Leon Trotzki nannte Stalins Machtübernahme einen 'Thermidor der bolschewistischen Revolution'."
    ),

    // --- Haitian Revolution ---
    Question(
        categoryId = 3,
        questionText = "Wer war Toussaint Louverture, und welche Besonderheit hatte seine militärische Karriere während der Haitianischen Revolution?",
        answerA = "Französischer Kolonialoffizier, der die Sklaven-Rebellion brutal niederschlug",
        answerB = "Selbstbefreiter Sklave, der zunächst auf spanischer Seite kämpfte, dann zur französischen Seite wechselte und Saint-Domingue regierte",
        answerC = "Englischer Militärberater, der den Haitianern bei der Unabhängigkeit half",
        answerD = "Freier Schwarzer aus Martinique, der die ersten Aufstände 1791 organisierte",
        correctAnswer = 1,
        explanation = "Toussaint Louverture war ein ehemaliger Sklave (freigekauft durch seinen Herrn), der 1791 die Sklaven-Rebellion anführte. Er kämpfte zuerst für Spanien, wechselte 1794 zu Frankreich (nach der Abschaffung der Sklaverei durch den Nationalkonvent), und regierte dann Saint-Domingue de facto autonom. Napoleon schickte 1801 eine Armee, die ihn gefangennahm.",
        difficulty = 4,
        funFact = "Toussaint starb 1803 im französischen Gefängnis Fort de Joux, aber sein Ausspruch bleibt legendär: 'In meinen Sturz habt ihr nur den Stamm des Baumes der schwarzen Freiheit in Saint-Domingue gefällt. Er wird wieder wachsen, denn seine Wurzeln sind tief.'"
    ),

    Question(
        categoryId = 3,
        questionText = "Welche wirtschaftliche Rolle spielte Saint-Domingue (Haiti) für Frankreich vor der Revolution, und warum war dies für Napoleon so wichtig?",
        answerA = "Saint-Domingue war Frankreichs wichtigster Militärstützpunkt in der Karibik",
        answerB = "Saint-Domingue produzierte etwa 40% des weltweiten Zuckers und 50% des Kaffees und war Frankreichs reichste Kolonie",
        answerC = "Saint-Domingue war das Zentrum des französischen Sklavenhandels mit Westafrika",
        answerD = "Saint-Domingue lieferte Tabak für den europäischen Markt",
        correctAnswer = 1,
        explanation = "Vor der Revolution war Saint-Domingue (die westliche Hälfte Hispaniolas) Frankreichs wertvollste Kolonie und eine der produktivsten Zonen der Welt: Sie produzierte etwa 40% des europäischen Zuckers und 50% des Kaffees. Napoleon wollte sie als Basis für ein nordamerikanisches Imperium zurückgewinnen.",
        difficulty = 4,
        funFact = "Nach der gescheiterten Expedition nach Haiti (die Truppen starben massenhaft an Gelbfieber) und dem Tod von 50.000 Soldaten gab Napoleon sein amerikanisches Imperiumsprojekt auf und verkaufte 1803 Louisiana an die USA für 15 Millionen Dollar."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann und unter welchem Anführer erklärte Haiti seine Unabhängigkeit, und welche Besonderheit machte diese Unabhängigkeit historisch einzigartig?",
        answerA = "1803 unter Toussaint Louverture – erste Sklavenrepublik nach einer erfolgreichen Revolution",
        answerB = "1804 unter Jean-Jacques Dessalines – erste Schwarze Republik, die durch eine erfolgreiche Sklavenrevolution entstand",
        answerC = "1806 unter Henri Christophe – erste Monarchie in der neuen Welt nach den USA",
        answerD = "1810 unter Alexandre Pétion – erste Demokratie in der Karibik",
        correctAnswer = 1,
        explanation = "Haiti erklärte am 1. Januar 1804 unter Jean-Jacques Dessalines die Unabhängigkeit. Es war die erste Schwarze Republik der Geschichte und die einzige Nation, die durch eine erfolgreiche Sklavenrevolution entstand. Haiti war nach den USA die zweite unabhängige Republik des amerikanischen Kontinents.",
        difficulty = 4,
        funFact = "Frankreich erkannte Haiti erst 1825 an – gegen eine Entschädigung von 150 Millionen Francs (die Haiti erst 1947 vollständig abzahlte). Diese Entschädigungsschuld hielt Haiti über ein Jahrhundert in wirtschaftlicher Abhängigkeit."
    ),

    // --- Latin American Independence ---
    Question(
        categoryId = 3,
        questionText = "Was war das 'Grito de Dolores' von 1810, und von wem wurde es ausgerufen?",
        answerA = "Die Unabhängigkeitserklärung Argentiniens durch José de San Martín",
        answerB = "Ein Aufstandsaufruf des Priesters Miguel Hidalgo y Costilla in Dolores, Mexiko, der den mexikanischen Unabhängigkeitskrieg einleitete",
        answerC = "Die Rede Simón Bolívars in Caracas zur Gründung Großkolumbiens",
        answerD = "Das Manifest der Kreolen-Elite in Buenos Aires gegen die spanische Herrschaft",
        correctAnswer = 1,
        explanation = "Das 'Grito de Dolores' (Ruf von Dolores) war der Aufstandsaufruf von Pater Miguel Hidalgo y Costilla am 16. September 1810 in der Stadt Dolores (heute Dolores Hidalgo, Mexiko). Er läutete die Kirchenglocke und rief Bauern und Indigene zur Rebellion auf – der Beginn des mexikanischen Unabhängigkeitskrieges.",
        difficulty = 4,
        funFact = "Der 16. September gilt als mexikanischer Unabhängigkeitstag. Jedes Jahr um Mitternacht rezitiert der amtierende Präsident den 'Grito' vom Balkon des Nationalpalastes – obwohl Mexiko erst 1821 formell unabhängig wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Schlachten Simón Bolívars in den Jahren 1819–1821 sicherten die Unabhängigkeit Großkolumbiens (Venezuela und Kolumbien)?",
        answerA = "Schlachten von Carabobo und Ayacucho",
        answerB = "Schlachten von Boyacá (1819) und Carabobo (1821)",
        answerC = "Schlachten von Chacabuco und Maipú",
        answerD = "Schlachten von Pichincha und Junín",
        correctAnswer = 1,
        explanation = "Bolívar überquerte 1819 die Anden (ein militärisches Meisterstück vergleichbar mit Hannibals Alpenüberquerung) und besiegte die Spanier bei Boyacá (7. August 1819), was Kolumbien befreite. Die Battle of Carabobo (24. Juni 1821) sicherte schließlich Venezuelas Unabhängigkeit.",
        difficulty = 4,
        funFact = "Bolívars Andenüberquerung von Venezuela nach Kolumbien im Jahr 1819 galt als militärisches Wunder: Er führte 2.500 Soldaten und 500 Vieh durch undurchdringliche Llanos und über 4.000 Meter hohe Andenpässe – im tropischen Sommerregenregen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Rolle spielte José de San Martín bei der südamerikanischen Unabhängigkeit, und wie unterschied sich sein Ansatz von dem Bolívars?",
        answerA = "San Martín befreite nur Argentinien und übergab dann das Kommando; Bolívar befreite Nordamerika",
        answerB = "San Martín befreite Argentinien, Chile und Peru von Süden her (über die Anden); Bolívar befreite Nordkolumbien, Venezuela und Ecuador vom Norden her",
        answerC = "San Martín war Royalist der später wechselte; Bolívar war von Anfang an Revolutionär",
        answerD = "San Martín bevorzugte Monarchien als Staatsform; Bolívar war überzeugter Republikaner",
        correctAnswer = 1,
        explanation = "Die lateinamerikanische Unabhängigkeit wurde in einer Zangenoperation gewonnen: San Martín befreite Argentinien (1816), überquerte die Anden nach Chile (Chacabuco 1817, Maipú 1818) und landete dann in Peru. Bolívar kam von Norden aus Kolumbien und Venezuela. Beide trafen sich 1822 in Guayaquil.",
        difficulty = 4,
        funFact = "Das Treffen von Guayaquil (1822) zwischen Bolívar und San Martín ist historisch rätselhaft – niemand weiß genau was besprochen wurde. San Martín zog sich danach freiwillig zurück und verbrachte den Rest seines Lebens im europäischen Exil, während Bolívar allein weiter kämpfte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Monroe-Doktrin' (1823) und wie bezog sie sich auf die lateinamerikanische Unabhängigkeit?",
        answerA = "Ein US-Angebot, die neuen lateinamerikanischen Republiken in die USA aufzunehmen",
        answerB = "Eine Erklärung von US-Präsident Monroe, die jede europäische Einmischung oder Rekolonisierung in der westlichen Hemisphäre als Bedrohung der US-Sicherheit betrachtete",
        answerC = "Ein Bündnisvertrag zwischen den USA und den lateinamerikanischen Republiken gegen Spanien",
        answerD = "Ein Handelsabkommen, das den USA bevorzugten Zugang zu lateinamerikanischen Märkten sicherte",
        correctAnswer = 1,
        explanation = "Präsident James Monroe erklärte in seiner Botschaft an den Kongress (2. Dezember 1823), dass jede europäische Einmischung in die Angelegenheiten der unabhängigen Nationen Amerikas als unfreundlicher Akt gegenüber den USA angesehen würde. Dies richtete sich gegen Pläne der Heiligen Allianz, Spanien bei der Rückeroberung seiner Kolonien zu unterstützen.",
        difficulty = 4,
        funFact = "Die Monroe-Doktrin war 1823 faktisch zahnlos – die USA hatten keine Macht, sie durchzusetzen. Es war die britische Royal Navy, die eine Rekolonisierung verhinderte, weil Großbritannien von Freihandel mit Lateinamerika profitierte, nicht von spanischen Monopolen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Kongress von Panama' (1826), und warum scheiterte er an Bolívars Vision?",
        answerA = "Ein Treffen der lateinamerikanischen Länder zur Gründung eines gemeinsamen Militärbündnisses, das an nationalen Rivalitäten scheiterte",
        answerB = "Ein von Bolívar einberufener Kongress zur Schaffung einer lateinamerikanischen Konföderation (wie ein 'Vereinigte Staaten von Lateinamerika'), der an nationalen Egoismen und US-/britischen Interessen scheiterte",
        answerC = "Eine panamerikanische Konferenz mit den USA und europäischen Mächten, die an der Sklavereifrage scheiterte",
        answerD = "Das Gründungstreffen der Großkolumbien-Verfassung, das Bolívar als Diktator einsetzte",
        correctAnswer = 1,
        explanation = "Der Kongress von Panama (1826) war Bolívars Versuch, eine lateinamerikanische Konföderation ähnlich den USA zu schaffen. Nur vier Länder schickten Vertreter (Mexiko, Zentralamerika, Kolumbien, Peru). Brasilien, Argentinien und die USA (wo Südstaaten eine Konferenz mit Haiti ablehnten) blieben fern. Das Projekt scheiterte vollständig.",
        difficulty = 4,
        funFact = "Bolívar starb 1830 verbittert und im Exil – Großkolumbien zerfiel in drei Staaten, Venezuelas Kongress hatte sein Attentat beschlossen, und er sagte: 'Amerika ist unregierbar. Wer einer Revolution dient, pflügt das Meer.'"
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Rolle spielten die 'Llaneros' (Gauchos der Llanos) in Bolívars Unabhängigkeitskampf, und wie gewann er ihre Unterstützung?",
        answerA = "Die Llaneros kämpften immer für Bolívar, weil sie Sklaven waren, die er befreit hatte",
        answerB = "Die Llaneros kämpften zunächst unter dem Royalisten Boves gegen Bolívar, wechselten aber nach Boves Tod 1814 zur Patriotenseite unter Páez",
        answerC = "Die Llaneros waren neutrale Söldner, die Bolívar mit britischem Gold anwarb",
        answerD = "Die Llaneros weigerten sich zu kämpfen und wurden zwangsrekrutiert",
        correctAnswer = 1,
        explanation = "Die Llaneros (Rinderhirten der Llanos) waren die gefürchtetsten Kämpfer Venezuelas. Unter José Tomás Boves kämpften sie brutal gegen die kreolische Oberschicht (einschließlich Bolívars Kräfte) für den König. Nach Boves Tod (1814) wechselten sie unter José Antonio Páez zur patriotischen Seite – was Bolívars Erfolg sicherte.",
        difficulty = 4,
        funFact = "Boves' Armee von Llaneros bestand hauptsächlich aus Mestizen, Schwarzen und Indigenen – sie kämpften nicht für den König, sondern gegen die weiße Kreolen-Oberschicht. Dies war ein Klassenkrieg innerhalb des Unabhängigkeitskriegs."
    ),

    Question(
        categoryId = 3,
        questionText = "Warum scheiterte die erste mexikanische Unabhängigkeitsbewegung unter Hidalgo und Morelos, und wer vollendete die Unabhängigkeit 1821?",
        answerA = "Hidalgo/Morelos scheiterten an US-Intervention; die Unabhängigkeit vollendete Benito Juárez",
        answerB = "Hidalgo/Morelos scheiterten wegen ihrer radikalen sozialen Agenda (Aufhebung der Sklaverei, Landreform), die die Kreolen abschreckte; die Unabhängigkeit vollendete der konservative Kreolen-Offizier Agustín de Iturbide",
        answerC = "Hidalgo/Morelos wurden von den USA verraten; die Unabhängigkeit vollendete Santa Anna",
        answerD = "Sie scheiterten an einem spanischen Militärsieg; Spanien gab Mexiko nach der Liberalen Revolution in Spanien freiwillig auf",
        correctAnswer = 1,
        explanation = "Hidalgo (hingerichtet 1811) und Morelos (hingerichtet 1815) scheiterten, weil ihre sozialen Forderungen (Gleichheit der Rassen, Abschaffung der Sklaverei, Landreform) die konservative Kreolen-Elite gegen sie aufbrachte. 1821 vollendete ausgerechnet der royalistische Offizier Agustín de Iturbide die Unabhängigkeit mit seinem Plan de Iguala – weil er die liberale Revolution in Spanien fürchtete.",
        difficulty = 4,
        funFact = "Iturbide proklamierte sich 1822 als Kaiser Agustín I. von Mexiko – das erste und einzige mexikanische Kaiserreich. Es dauerte nur zwei Jahre, bevor er gestürzt und verbannt wurde. Bei seiner Rückkehr 1824 wurde er sofort erschossen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Caste War of Yucatan' (1847–1901) im Kontext der lateinamerikanischen Geschichte nach der Unabhängigkeit?",
        answerA = "Ein Bürgerkrieg zwischen Konservativen und Liberalen in Mexiko",
        answerB = "Ein Maya-Aufstand gegen weiße und mestizische Großgrundbesitzer in Yucatán, der jahrzehntelang andauerte",
        answerC = "Ein Krieg zwischen Mexiko und Guatemala um die Kontrolle der Halbinsel",
        answerD = "Ein Sklavenaufstand in der Karibikregion Mexikos",
        correctAnswer = 1,
        explanation = "Der 'Caste War of Yucatan' (1847–1901) war ein Maya-Aufstand gegen die weiße und mestizische Oberschicht, die nach der mexikanischen Unabhängigkeit das Land der Maya-Bauern für Zuckerplantagen konfisziert hatte. Die Maya gründeten einen quasi-unabhängigen Staat und kämpften über 50 Jahre.",
        difficulty = 4,
        funFact = "Die Maya des 'Caste War' hatten den 'Sprechenden Kreuz'-Kult entwickelt, bei dem Orakel durch ein Kreuz ihre religiöse und militärische Führung legitimierten. Dieser Kult hielt die Widerstandsbewegung jahrzehntelang zusammen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche wirtschaftliche Struktur bezeichnen Historiker als 'Dependency Theory' im Kontext der lateinamerikanischen Nachunabhängigkeitsentwicklung?",
        answerA = "Die These, dass Lateinamerika wirtschaftlich abhängig von den USA wurde, weil beide Seiten von dieser Abhängigkeit profitierten",
        answerB = "Die Theorie, dass die lateinamerikanischen Länder trotz politischer Unabhängigkeit wirtschaftlich als Rohstofflieferanten für den industrialisierten 'Kern' (Europa/USA) eingebunden blieben und dadurch strukturell unterentwickelt wurden",
        answerC = "Das Konzept, dass Lateinamerikas Rückstand durch den Kultureinfluss der indigenen Bevölkerung verursacht wurde",
        answerD = "Die Erklärung, dass politische Instabilität allein Lateinamerikas Unterentwicklung verursachte",
        correctAnswer = 1,
        explanation = "Die Dependencia-Theorie (Raúl Prebisch, André Gunder Frank, Celso Furtado) besagt: Lateinamerika blieb nach der Unabhängigkeit wirtschaftlich abhängig, da es weiterhin Rohstoffe exportierte und Industriewaren importierte. Der 'Kern' (Europa/USA) profitierte vom ungleichen Tausch, während die 'Peripherie' strukturell unterentwickelt blieb.",
        difficulty = 4,
        funFact = "CEPAL (Comisión Económica para América Latina), gegründet 1948 in Chile, war das intellektuelle Zentrum der Dependencia-Theorie. Raúl Prebisch entwickelte hier die 'Prebisch-Singer-These' über die langfristige Verschlechterung der Terms of Trade für Rohstoffexporteure."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche europäische Intervention in Mexiko (1861–1867) war ein direkter Ausdruck des Scheiterns der Monroe-Doktrin und endete mit der Erschießung eines habsburgischen Kaisers?",
        answerA = "Die britische Besetzung Veracruzes (1861–1862)",
        answerB = "Die französische Intervention unter Napoleon III., die zur Etablierung des Kaisers Maximilian I. von Habsburg führte",
        answerC = "Die spanische Reannexion Kubas (1861)",
        answerD = "Die österreichische Expedition zur Rückgewinnung mexikanischer Schulden",
        correctAnswer = 1,
        explanation = "Napoleon III. nutzte Mexikos Schuldenmoratorium (1861), um zu intervenieren und Erzherzog Maximilian von Habsburg als Kaiser Maximilian I. von Mexiko einzusetzen (1864). Nach dem Abzug der französischen Truppen (auf US-Druck nach dem Bürgerkrieg) wurde Maximilian 1867 von Juárez' republikanischen Kräften besiegt und erschossen.",
        difficulty = 4,
        funFact = "Edouard Manet malte 1867/68 das Gemälde 'Die Erschießung Kaiser Maximilians' – eines der berühmtesten politischen Gemälde des 19. Jahrhunderts. Die französische Regierung versuchte, die Ausstellung zu verhindern, weil das Bild Frankreichs Verantwortung implizierte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte die 'Abolitionsbewegung' für Brasiliens späte Unabhängigkeitspolitik, und wann wurde die Sklaverei in Brasilien abgeschafft?",
        answerA = "Brasilien schaffte die Sklaverei 1822 gleichzeitig mit seiner Unabhängigkeit ab",
        answerB = "Brasilien schaffte die Sklaverei erst 1888 ab (als letztes Land im Westen) mit dem 'Lei Áurea', und dies führte 1889 zum Sturz der Monarchie und zur Gründung der Republik",
        answerC = "Brasilien schaffte die Sklaverei 1850 auf britischen Druck hin ab",
        answerD = "Brasilien schaffte die Sklaverei 1871 durch das Freigeburtsgesetz vollständig ab",
        correctAnswer = 1,
        explanation = "Brasilien war das letzte westliche Land, das die Sklaverei abschaffte – erst 1888 mit dem 'Lei Áurea' unter Prinzessin Regent Isabella. Die 700.000 befreiten Sklaven erhielten keine Entschädigung und kein Land. Die Kaffeepflanzer fühlten sich verraten und unterstützten 1889 den Militärputsch, der die Monarchie stürzte.",
        difficulty = 4,
        funFact = "Brasilien importierte schätzungsweise 4,9 Millionen afrikanische Sklaven – mehr als jedes andere Land (ca. 40% aller atlantischen Sklaven). Zum Vergleich: Die USA importierten etwa 400.000 Sklaven direkt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was versteht man unter dem 'Creole consciousness' (Kreolen-Bewusstsein) als Ursache der lateinamerikanischen Unabhängigkeitsbewegungen?",
        answerA = "Das Identitätsbewusstsein der in Amerika geborenen Europäer (Kreolen), die trotz gleicher Abstammung gegenüber in Spanien geborenen Europäern politisch und wirtschaftlich benachteiligt wurden",
        answerB = "Das Bewusstsein der Mischlinge (Mestizen) für ihre afrikanischen und indigenen Wurzeln",
        answerC = "Der religiöse Glaube der Kreolen an eine besondere Sendung Lateinamerikas",
        answerD = "Die Sprachtrennung zwischen Kreolen-Spanisch und kastilischem Spanisch",
        correctAnswer = 0,
        explanation = "Benedict Anderson und andere Historiker betonen das 'Creole consciousness': In Spanisch-Amerika geborene Europäer (Kreolen) waren von hohen Regierungsposten ausgeschlossen, die Spanien geborenen Peninsulares vorbehaltenen blieben. Dieses Diskriminierungserlebnis schuf ein regionales Identitätsgefühl, das zur Unabhängigkeitsbewegung beitrug.",
        difficulty = 4,
        funFact = "Anderson's einflussreiches Buch 'Imagined Communities' (1983) argumentiert, dass lateinamerikanische Nationen überhaupt erst als 'vorgestellte Gemeinschaften' entstanden sind, die durch Zeitungen (die in regionalen Hauptstädten gedruckt wurden) und gemeinsame Verwaltungsgrenzen geformt wurden."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Rolle spielten die Napoleonischen Kriege als äußerer Auslöser für die lateinamerikanischen Unabhängigkeitsbewegungen?",
        answerA = "Napoleon schickte Truppen nach Lateinamerika, die die Unabhängigkeit unterstützten",
        answerB = "Napoleons Invasion in Spanien (1808) und die Einsetzung Joseph Bonapartes als König destabilisierten die spanische Kolonialherrschaft, da die Kreolen sich fragen mussten, wem sie loyaler sein sollten",
        answerC = "Die britische Blockade Spaniens schnitt die Kolonien von Handelsgütern ab und zwang sie zur Selbstversorgung",
        answerD = "Der Wiener Kongress (1815) erkannte die Unabhängigkeit der lateinamerikanischen Staaten an",
        correctAnswer = 1,
        explanation = "Napoleons Invasion Spaniens 1808 und die Einsetzung seines Bruders Joseph als König schuf eine Legitimitätskrise: Den Kolonien wurde nicht von 'dem König' regiert, dem sie Gehorsam schuldeten, sondern von einem fremden Usurpator. Lokale Juntas wurden gegründet 'im Namen des Königs Ferdinand VII.' – was oft Tarnung für wachsende Unabhängigkeitsbestrebungen war.",
        difficulty = 4,
        funFact = "Brasilien verlief anders: Als Napoleon Portugal bedrohte, floh König João VI. 1807/08 mit dem gesamten Hof nach Rio de Janeiro. Brasilien wurde dadurch Hauptstadt des Portugiesischen Reiches – und erhielt 1815 den Status eines gleichberechtigten Königreichs, was seinen Weg zur Unabhängigkeit 1822 ohne Guerillakrieg ermöglichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Banda Oriental' und warum war sie für die südamerikanische Unabhängigkeitspolitik von zentraler Bedeutung?",
        answerA = "Die östliche Küstenregion Brasiliens, umkämpft zwischen Portugiesen und Spaniern",
        answerB = "Das heutige Uruguay, das zwischen Argentinien, Brasilien und lokalen Nationalisten um Artigas umkämpft war und 1828 als Pufferstaat entstand",
        answerC = "Die Region um Buenos Aires, wo die erste unabhängige Junta 1810 gegründet wurde",
        answerD = "Ein Karibikinseln-Archipel, umkämpft zwischen England und Spanien",
        correctAnswer = 1,
        explanation = "Die 'Banda Oriental' (heutiges Uruguay) war Schauplatz komplizierter Machtkämpfe: José Artigas führte einen Volksaufstand gegen Buenos Aires und Portugal-Brasilien. Brasilien annektierte es 1821. Im Vertrag von Montevideo (1828) wurde es als unabhängiger 'Pufferstaat' zwischen Argentinien und Brasilien anerkannt.",
        difficulty = 4,
        funFact = "Artigas gilt als Nationalheld Uruguays, obwohl er nie in der 'República Oriental del Uruguay' regierte – er wurde 1820 von den Brasilianern verdrängt und starb 1850 im paraguayischen Exil."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche wirtschaftliche Theorie bezeichnet Historikers als 'Freihandels-Imperialismus' im Kontext der britischen Rolle in Lateinamerika nach der Unabhängigkeit?",
        answerA = "Die Praxis, britische Waren zollfrei in lateinamerikanische Märkte zu exportieren, während lateinamerikanische Produkte in Großbritannien mit Schutzzöllen belegt wurden",
        answerB = "Die britische Strategie, durch Freihandelsverträge und Anleihen wirtschaftliche Kontrolle über formell unabhängige Staaten zu erlangen – 'informelles Empire' ohne formale Kolonisierung",
        answerC = "Die britische Geldpolitik, die lateinamerikanische Silberwährungen entwertete",
        answerD = "Das britische System, Sklavenhändler gegen Bezahlung zu schützen",
        correctAnswer = 1,
        explanation = "John Gallagher und Ronald Robinson prägten 1953 den Begriff 'informelles Empire' für Britanniens Praxis in Lateinamerika: Durch Freihandelsverträge, Kapitalexporte und Eisenbahnbau sicherte Großbritannien wirtschaftliche Dominanz ohne formale Kolonisierung. 'Free Trade Imperialism' war die Kontrolle ohne Flagge.",
        difficulty = 4,
        funFact = "Argentinien wurde oft als 'sechstes Dominion' Britanniens bezeichnet – so stark war der britische wirtschaftliche Einfluss. Die Eisenbahn in Argentinien wurde fast vollständig mit britischem Kapital gebaut und anfangs von britischen Managern geleitet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Vicente Guerrero, und welche historische Besonderheit machte ihn in der mexikanischen Geschichte außergewöhnlich?",
        answerA = "Der erste Präsident Mexikos, der von einer indigenen Mutter und einem spanischen Vater abstammte",
        answerB = "Einer der letzten Anführer des Guerilla-Widerstands (1815–1821), der zur Unabhängigkeit beitrug und 1829 der zweite und erste afro-mexikanische Präsident Mexikos wurde – er schaffte die Sklaverei ab",
        answerC = "Der Anführer der ersten mexikanischen Armee, der Hidalgo von der Guerillaführung abgelöste",
        answerD = "Ein Jesuit, der als Berater Iturbides fungierte und den Plan de Iguala entwarf",
        correctAnswer = 1,
        explanation = "Vicente Guerrero (1782–1831) war afromexikanischer Abstammung (einer der wenigen nicht-kreolischen Unabhängigkeitsführer), kämpfte ab 1810 als Guerillaführer unter Morelos und gegen die Royalisten. Als Präsident (1829) schaffte er die Sklaverei in Mexiko ab – Jahrzehnte bevor die USA es taten.",
        difficulty = 4,
        funFact = "Guerreros Abschaffung der Sklaverei 1829 hatte eine direkte Wirkung auf Texas: Viele US-Siedler dort hatten Sklaven mitgebracht. Die mexikanische Regierung hatte Texas-Siedlern Ausnahmen gewährt, aber Guerreros Erlass bedrohte ihr System – was zu Spannungen beitrug, die 1836 zum Texas-Aufstand führten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand Simón Bolívar unter 'La Jamaica Letter' (Brief aus Jamaika, 1815), und welche politische Vision formulierte er darin?",
        answerA = "Eine Bitte an britische Händler, den Unabhängigkeitskampf finanziell zu unterstützen",
        answerB = "Eine prophetische Analyse der politischen Zukunft Lateinamerikas – Bolívar sagte die Fragmentierung in viele Republiken voraus, warnte aber vor direkter Demokratie und empfahl starke Exekutivgewalten",
        answerC = "Ein Manifest, das alle amerikanischen Nationen zu einer einzigen Konföderation aufforderte",
        answerD = "Eine Anklage gegen britische Handelspolitik, die den Unabhängigkeitsbewegungen schadete",
        correctAnswer = 1,
        explanation = "Die 'Jamaica Letter' (September 1815) war eine politische Analyse Bolívars aus dem Exil. Er sagte die Fragmentierung Lateinamerikas in Nationalstaaten voraus (was sich erfüllte), warnte vor 'demokratischen Tyranneien' und argumentierte für starke, zentralisierte Republiken mit lebenslagen Präsidenten – eine autoritäre Neigung, die seine Kritiker bis heute beschäftigt.",
        difficulty = 4,
        funFact = "Bolívar entging in Jamaika einem Mordanschlag: Ein Attentäter erstach seinen Schlafkameraden in der falschen Hängematters – Bolívar schlief zufällig woanders. Dieser Zufall rettete seine Karriere und damit möglicherweise die Unabhängigkeit von fünf Nationen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Pacto Federal' von 1831 in Argentinien, und wie spiegelt er die Spannung zwischen Unitariern und Föderalisten wider?",
        answerA = "Ein Bundesvertrag der argentinischen Provinzen, der Buenos Aires die Hauptstadtrolle zusprach",
        answerB = "Ein Bundesvertrag der wichtigsten argentinischen Provinzen (Entre Ríos, Santa Fe, Buenos Aires, Corrientes), der die föderale Struktur gegen die Unitarier von Buenos Aires verteidigte – Vorläufer der Bundesverfassung von 1853",
        answerC = "Ein Waffenstillstandsabkommen zwischen Argentina und der Banda Oriental",
        answerD = "Ein Handelsvertrag zwischen den Rio-de-la-Plata-Provinzen und Bolivien",
        correctAnswer = 1,
        explanation = "Der Pacto Federal (1831) war ein Bündnis der Provinzen Entre Ríos, Santa Fe, Buenos Aires (unter Rosas) und Corrientes gegen die unitaristische Regierung in Buenos Aires. Er spiegelt den fundamentalen Konflikt: Föderalisten wollten mächtige Provinzen, Unitarier eine starke Zentralregierung – dieser Konflikt dominierte argentinische Politik bis 1853.",
        difficulty = 4,
        funFact = "Juan Manuel de Rosas (Gouverneur von Buenos Aires, 1829–1832 und 1835–1852) war der mächtigste der Caudillos und ein brutaler Diktator. Er schuf ein Netzwerk von Spionen (mazorca) und seine roten Ribbon-Symbole wurden zur Erkennungsmarke – das Tragen des Gegensatzfarbe Blau konnte tödlich sein."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche zentrale Frage teilte die lateinamerikanischen Unabhängigkeitsbewegungen intern in 'Patrioten' und 'Royalisten'?",
        answerA = "Die Frage der Pressefreiheit und der republikanischen Verfassung",
        answerB = "Die soziale Frage: Ob die Unabhängigkeit nur den Kreolen nützen oder auch Sklaven, Indigene und Mestizen befreien würde – was die Kreolen-Oberschicht spaltete",
        answerC = "Die Religionsfrage: Ob die Kirche ihre Privilegien behalten sollte",
        answerD = "Die außenpolitische Frage: Ob Lateinamerika britische oder US-amerikanische Unterstützung suchen sollte",
        correctAnswer = 1,
        explanation = "Der tiefste interne Riss war die soziale Frage: Radikale Unabhängigkeitsführer (Hidalgo, Morelos, Artigas, Bolívar in späteren Jahren) wollten auch Sklaven, Indigene und Arme befreien. Konservative Kreolen wollten nur die spanischen Bürokraten loswerden und sonst alles beim Alten lassen. Diese Spaltung erklärt viele Niederlagen und interne Konflikte.",
        difficulty = 4,
        funFact = "Nach der Unabhängigkeit verlief die soziale Position der Indigenen in vielen Gebieten schlechter als unter Spanien: Die spanische Krone hatte (wenn auch unvollkommen) Schutzgesetze für Indigene erlassen. Die neuen Republiken sahen Indigene oft als Hindernis für 'Fortschritt' und enteigneten ihr Land massenhaft."
    ),

    // --- Additional Expert Questions ---
    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte das britische Verbot des Sklavenhandels für den atlantischen Sklavenhandel insgesamt, und wie effektiv war die 'West Africa Squadron'?",
        answerA = "Das britische Verbot beendete den Sklavenhandel sofort und vollständig, da andere Mächte folgten",
        answerB = "Die West Africa Squadron (1808–1867) befreite schätzungsweise 150.000 Menschen, konnte aber den Sklavenhandel nicht stoppen – er expandierte weiter bis in die 1850er Jahre, da andere Mächte (USA, Brasilien, Kuba) weiter Sklaven importierten",
        answerC = "Das britische Verbot war nur symbolisch, da Großbritannien weiterhin in der Sklavenwirtschaft investierte",
        answerD = "Die West Africa Squadron operierte nur in der Karibik, nicht vor der afrikanischen Küste",
        correctAnswer = 1,
        explanation = "Die britische West Africa Squadron (Royal Navy) kreuzte 60 Jahre vor Westafrikas Küste, um Sklavenschiffe zu kapern. Sie befreite etwa 150.000 Sklaven. Trotzdem expandierte der illegale Sklavenhandel: Brasilien importierte nach 1807 mehr Sklaven als davor, und die USA ließen British Right of Search nicht zu, was die Durchsetzung schwächte.",
        difficulty = 4,
        funFact = "Das Verhör eines gefangenen Sklavenschiffkapitäns durch die Royal Navy war oft fruchtlos: Da die USA das britische Durchsuchungsrecht ablehnten, hissten Sklavenhändler einfach die amerikanische Flagge und waren vorübergehend geschützt – ein systematisches Schlupfloch."
    ),

    Question(
        categoryId = 3,
        questionText = "Warum gilt die 'Declaratory Act' (1766) als eigentliche Ursache der amerikanischen Revolution, obwohl zeitgleich der Stamp Act aufgehoben wurde?",
        answerA = "Weil der Declaratory Act das Recht Britanniens bekräftigte, die Kolonien 'in allen Fällen wie auch immer' zu besteuern und zu gesetzgeben – was die Kolonien als prinzipiell unannehmbar betrachteten",
        answerB = "Weil der Declaratory Act die Zahlung einer Strafsteuer für den Stamp Act verlangte",
        answerC = "Weil der Declaratory Act koloniale Parlamente offiziell abschaffte",
        answerD = "Weil der Declaratory Act erstmals das Prinzip 'taxation without representation' kodifizierte",
        correctAnswer = 0,
        explanation = "Der Declaratory Act (März 1766) wurde gleichzeitig mit der Aufhebung des Stamp Act verabschiedet und erklärte, das britische Parlament habe das Recht, Gesetze für die Kolonien 'in allen Fällen, wie auch immer' (in all cases whatsoever) zu erlassen. Obwohl Kolonisten den Stamp Act-Sieg feierten, sahen viele den Declaratory Act als Prinzipfrage an.",
        difficulty = 4,
        funFact = "Benjamin Franklin sagte vor dem britischen Parlament, die Kolonien würden den Declaratory Act 'als einen leeren Anspruch' betrachten, solange er nicht vollzogen werde. Wenige Jahre später vollzog ihn die Townshend Revenue Act (1767) – und die Kolonien reagierten erneut mit Boykotten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches diplomatische Dokument von 1778 sicherte die französische Allianz mit den USA, und welche wirtschaftlichen Zugeständnisse erhielt Frankreich dafür?",
        answerA = "Der Vertrag von Paris (1778): Frankreich erhielt Louisiana als Gegenleistung",
        answerB = "Der Vertrag von Amity and Commerce (1778): Frankreich erhielt Meistbegünstigungsstatus im US-Handel und die USA versprachen, Frankreich bei einem europäischen Krieg zu unterstützen",
        answerC = "Der geheime Vertrag von Versailles (1778): die USA traten Kanada formell an Frankreich ab",
        answerD = "Die Allianz von Fontainebleau (1778): Frankreich und die USA teilten alle britischen Kolonien unter sich auf",
        correctAnswer = 1,
        explanation = "Am 6. Februar 1778 schlossen Frankreich und die USA zwei Verträge: Den 'Treaty of Amity and Commerce' (gegenseitiger Meistbegünstigungsstatus im Handel) und die 'Treaty of Alliance' (gegenseitige Verteidigungspflicht). Frankreich erkannte damit faktisch die USA als souveränen Staat an – der erste Staat, der es tat.",
        difficulty = 4,
        funFact = "Benjamin Franklin hatte monatelang in Paris verhandelt und spielte seine Karten meisterhaft: Er ließ Gerüchte über geheime britisch-amerikanische Friedensverhandlungen streuen, um Frankreichs Angst vor einem Separatfrieden zu nutzen und Paris zum Handeln zu bewegen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Asiento'-Klausel im Vertrag von Utrecht (1713), und welche Verbindung hatte sie zur britischen Beteiligung am Sklavenhandel?",
        answerA = "Eine Klausel die Spanien erlaubte, britische Schiffe in der Karibik zu kontrollieren",
        answerB = "Das britische Monopolrecht, jährlich 4.800 Sklaven (piezas de Indias) nach Spanisch-Amerika zu liefern und zusätzlich ein Schiff pro Jahr mit Handelsgütern zu senden (navio de permiso) – vergeben an die South Sea Company",
        answerC = "Ein Zollabkommen zwischen England und Spanien über Tabak- und Zuckerhandel",
        answerD = "Das Recht Englands, Silber aus den spanischen Kolonialsilberminen zu kaufen",
        correctAnswer = 1,
        explanation = "Im Vertrag von Utrecht (1713) erhielt England das 'Asiento' – das exklusive Recht, jährlich 4.800 afrikanische Sklaven (piezas de Indias) in die spanischen Kolonien zu liefern, plus ein Handelsschiff pro Jahr. Dieses Recht wurde der South Sea Company übertragen, die daraus riesige Gewinne erhoffte (und damit die South Sea Bubble auslöste).",
        difficulty = 4,
        funFact = "Die South Sea Company nutzte das Asiento als Köder für Investoren, die eigentlich in lateinamerikanischen Handelsgewinnen steckten. Als die Gewinne ausblieben und die Aktien 1720 einbrachen, verloren Tausende britischer Investoren ihr Vermögen – die erste große Börsenkrise der Geschichte neben der gleichzeitigen französischen Mississippi-Blase."
    ),

)
