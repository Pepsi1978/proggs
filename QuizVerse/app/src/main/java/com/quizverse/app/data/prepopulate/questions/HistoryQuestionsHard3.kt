package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsHard3(): List<Question> = listOf(

    // --- French Revolution: Reign of Terror & Committee of Public Safety ---

    Question(
        categoryId = 3,
        questionText = "Wann begann die Herrschaft des Wohlfahrtsausschusses (Comité de salut public) in der Französischen Revolution?",
        answerA = "Januar 1792",
        answerB = "April 1793",
        answerC = "Juni 1791",
        answerD = "September 1794",
        correctAnswer = 1,
        explanation = "Der Wohlfahrtsausschuss wurde im April 1793 gegründet und übernahm unter Robespierre die faktische Regierungsgewalt in Frankreich während der Schreckensherrschaft.",
        difficulty = 3,
        funFact = "Der Ausschuss hatte zunächst nur neun, später zwölf Mitglieder – doch diese kleine Gruppe entschied über Leben und Tod Tausender Franzosen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Menschen wurden während der Schreckensherrschaft (La Terreur) von 1793 bis 1794 in Frankreich hingerichtet?",
        answerA = "Ca. 500",
        answerB = "Ca. 2.000",
        answerC = "Ca. 17.000 offiziell, bis zu 40.000 gesamt",
        answerD = "Ca. 100.000",
        correctAnswer = 2,
        explanation = "Während der Schreckensherrschaft wurden offiziell etwa 17.000 Menschen hingerichtet, darunter viele durch die Guillotine. Zählt man Todesfälle in Gefängnissen hinzu, steigt die Zahl auf bis zu 40.000.",
        difficulty = 3,
        funFact = "Die Guillotine wurde als 'humanere' Hinrichtungsmethode eingeführt – Dr. Joseph-Ignace Guillotin schlug sie vor, weil sie für alle Stände gleich war."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Gesetz vom 22. Prairial 1794 verschärfte die Schreckensherrschaft erheblich, indem es Angeklagten das Recht auf Verteidiger nahm?",
        answerA = "Das Gesetz der Verdächtigen",
        answerB = "Das Prairial-Dekret (Gesetz des 22. Prairial)",
        answerC = "Die Konstitution von 1793",
        answerD = "Das Ventôse-Dekret",
        correctAnswer = 1,
        explanation = "Das Gesetz des 22. Prairial (10. Juni 1794) vereinfachte Strafprozesse drastisch: Es gab keine Zeugen, keine Verteidiger, das Urteil lautete entweder Freispruch oder Tod.",
        difficulty = 3,
        funFact = "In den sieben Wochen nach diesem Gesetz bis zum Sturz Robespierres wurden in Paris mehr Menschen guillotiniert als in den gesamten zwölf Monaten davor."
    ),

    Question(
        categoryId = 3,
        questionText = "Am welchem Datum (nach dem Revolutionskalender: 9. Thermidor) wurde Robespierre verhaftet und damit die Schreckensherrschaft beendet?",
        answerA = "14. Juli 1794",
        answerB = "27. Juli 1794",
        answerC = "5. März 1794",
        answerD = "9. November 1799",
        correctAnswer = 1,
        explanation = "Der 9. Thermidor des Jahres II entspricht dem 27. Juli 1794 im gregorianischen Kalender. An diesem Tag wurde Robespierre im Konvent verhaftet und am folgenden Tag guillotiniert.",
        difficulty = 3,
        funFact = "Robespierre wurde mit einem Schuss verwundet, als er festgenommen wurde – ob Selbstmordversuch oder Unfall, ist bis heute umstritten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Gruppierung stürzte Robespierre am 9. Thermidor und gab dieser Reaktion ihren Namen?",
        answerA = "Die Girondisten",
        answerB = "Die Enragés",
        answerC = "Die Thermidorianer",
        answerD = "Die Feuillants",
        correctAnswer = 2,
        explanation = "Die Thermidorianer – eine Koalition aus gemäßigten Republikanern und ehemaligen Terroristen, die selbst Angst hatten, als nächstes guillotiniert zu werden – stürzten Robespierre.",
        difficulty = 3,
        funFact = "Viele der Thermidorianer hatten selbst aktiv an der Schreckensherrschaft mitgewirkt – die Reaktion war also kein moralischer Umschwung, sondern Überlebensinstinkt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche politische Körperschaft ersetzte nach der Thermidorianischen Reaktion den Wohlfahrtsausschuss als oberste Regierungsgewalt?",
        answerA = "Der Nationalkonvent",
        answerB = "Das Direktorium (Directoire)",
        answerC = "Der Rat der Alten",
        answerD = "Das Konsulat",
        correctAnswer = 1,
        explanation = "Das Direktorium regierte Frankreich von 1795 bis 1799 und bestand aus fünf Direktoren. Es wurde durch Napoleons Staatsstreich vom 18. Brumaire (9. November 1799) abgelöst.",
        difficulty = 3,
        funFact = "Das Direktorium war für seine weitverbreitete Korruption bekannt – Historiker nennen es auch 'die Republik der Schwindler'."
    ),

    // --- Napoleonic Wars ---

    Question(
        categoryId = 3,
        questionText = "In welcher Schlacht 1805 besiegte Napoleon die österreichisch-russischen Truppen in einer seiner brillantesten Schlachten – oft als 'Meisterschlacht' bezeichnet?",
        answerA = "Waterloo",
        answerB = "Austerlitz",
        answerC = "Jena",
        answerD = "Wagram",
        correctAnswer = 1,
        explanation = "Die Dreikaiserschlacht bei Austerlitz (2. Dezember 1805) gilt als Napoleons größter taktischer Triumph. Er täuschte seinen rechten Flügel zu schwächen, um dann die Mitte des Feindes zu durchbrechen.",
        difficulty = 3,
        funFact = "Napoleon selbst bezeichnete Austerlitz als seine schönste Schlacht. Der Sieg zerstörte die Dritte Koalition und zwang Österreich zum Frieden von Pressburg."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Kontinentalsystem', das Napoleon 1806 einführte?",
        answerA = "Ein europäisches Eisenbahnnetz",
        answerB = "Eine Wirtschaftsblockade gegen Großbritannien, die britische Waren vom europäischen Festland ausschließen sollte",
        answerC = "Eine Zollunion aller napoleonischen Vasallenstaaten",
        answerD = "Ein militärisches Bündnissystem gegen Russland",
        correctAnswer = 1,
        explanation = "Mit dem Kontinentalsystem (Berliner Dekret, 1806) versuchte Napoleon, Großbritannien wirtschaftlich zu ruinieren, indem er europäischen Häfen die Aufnahme britischer Schiffe verbot. Es scheiterte, weil Schmuggel verbreitet war und Europa selbst litt.",
        difficulty = 3,
        funFact = "Das Kontinentalsystem schadete am Ende Napoleon mehr als Großbritannien: Die wirtschaftliche Not trieb viele Verbündete, besonders Russland, dazu, sich davon zu befreien."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß der verheerende Rückzug der Grande Armée aus Russland im Jahr 1812?",
        answerA = "Die Retraite von Smolensk",
        answerB = "Der Rückzug über die Beresina",
        answerC = "Der Moskauer Rückzug (Rückzug aus Russland)",
        answerD = "Die Flucht von Vilna",
        correctAnswer = 2,
        explanation = "Der Rückzug der Grande Armée aus Russland (Oktober–Dezember 1812) war eine Katastrophe. Von ca. 600.000 Soldaten, die in Russland einmarschierten, kehrten nur etwa 100.000 zurück. Frost, Hunger und Kosakenüberfälle vernichteten die Armee.",
        difficulty = 3,
        funFact = "Der Übergang über die Beresina (26.–29. November 1812) wurde zum Symbol der Katastrophe: Trotz heldenhafter Pionierleistungen ertranken und erfroren Tausende."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Halbinselkrieg' (Peninsular War, 1808–1814) und welche Nation leistete den Hauptwiderstand gegen Napoleon in Spanien?",
        answerA = "Österreich widerstand in Spanien als Verbündeter der Spanier",
        answerB = "Großbritannien unterstützte spanische und portugiesische Guerilla-Kräfte gegen die französische Besatzung",
        answerC = "Russland sandte Truppen auf die Iberische Halbinsel",
        answerD = "Preußen führte den Krieg in Spanien als Pufferstaat",
        correctAnswer = 1,
        explanation = "Im Halbinselkrieg kämpften britische Truppen unter Wellington gemeinsam mit spanischen und portugiesischen Kräften gegen Napoleons Besatzung. Der Guerillakrieg band enorme französische Ressourcen und trug wesentlich zu Napoleons Niederlage bei.",
        difficulty = 3,
        funFact = "Der Begriff 'Guerilla' (spanisch für 'kleiner Krieg') stammt aus diesem Konflikt – die spanischen Partisanen prägten diese Kriegsführung für die Moderne."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher entscheidenden Seeschlacht 1805 zerstörte die britische Flotte unter Admiral Nelson die kombinierte französisch-spanische Flotte?",
        answerA = "Die Seeschlacht von Trafalgar",
        answerB = "Die Seeschlacht von Aboukir",
        answerC = "Die Seeschlacht von Kopenhagen",
        answerD = "Die Seeschlacht von Navarino",
        correctAnswer = 0,
        explanation = "In der Seeschlacht von Trafalgar (21. Oktober 1805) vernichtete Nelson die französisch-spanische Flotte und sicherte Britanniens Seeherrschaft für Jahrzehnte. Nelson selbst fiel in der Schlacht.",
        difficulty = 3,
        funFact = "Nelsons letzte Worte sollen 'Kiss me, Hardy' oder 'Kismet, Hardy' gewesen sein – Historiker streiten bis heute, welche Version stimmt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Koalitionen kämpften gegen Napoleon? In welcher Koalition schlossen sich erstmals Großbritannien, Russland, Österreich und Preußen gemeinsam gegen ihn zusammen?",
        answerA = "Vierte Koalition (1806)",
        answerB = "Fünfte Koalition (1809)",
        answerC = "Sechste Koalition (1813–1814)",
        answerD = "Siebte Koalition (1815)",
        correctAnswer = 2,
        explanation = "Die Sechste Koalition (1813–1814) vereinte erstmals alle Großmächte konsequent gegen Napoleon. Die Völkerschlacht bei Leipzig (Oktober 1813) entschied den Krieg in Deutschland zugunsten der Koalition.",
        difficulty = 3,
        funFact = "Die Völkerschlacht bei Leipzig war mit ca. 600.000 beteiligten Soldaten die größte Landschlacht der Geschichte bis zum Ersten Weltkrieg."
    ),

    // --- Congress of Vienna ---

    Question(
        categoryId = 3,
        questionText = "Welches Prinzip des Wiener Kongresses (1814/15) sollte die vorrevolutionäre dynastische Ordnung Europas wiederherstellen?",
        answerA = "Das Nationalitätenprinzip",
        answerB = "Das Legitimationsprinzip",
        answerC = "Das Gleichgewichtsprinzip",
        answerD = "Das Souveränitätsprinzip",
        correctAnswer = 1,
        explanation = "Das Legitimationsprinzip, maßgeblich von Talleyrand eingebracht, besagte, dass nur die 'rechtmäßigen' dynastischen Herrscher (vor der Revolution) Anspruch auf ihre Throne hätten. Es diente der Restauration der alten Ordnung.",
        difficulty = 3,
        funFact = "Talleyrand – Frankreichs Vertreter – verwandelte Frankreich trotz seiner Niederlage geschickt vom Besiegten in einen gleichberechtigten Partner des Kongresses."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer leitete den Wiener Kongress als österreichischer Außenminister und wurde zum wichtigsten Architekten der nachnapoleonischen Ordnung?",
        answerA = "Metternich",
        answerB = "Castlereagh",
        answerC = "Hardenberg",
        answerD = "Talleyrand",
        correctAnswer = 0,
        explanation = "Klemens von Metternich leitete als österreichischer Außenminister (später Staatskanzler) den Wiener Kongress und prägte die konservativ-restaurative Ordnung Europas bis 1848 – die sogenannte Metternich-Ära.",
        difficulty = 3,
        funFact = "Metternich sagte über sich: 'Ich bin kein Reaktionär, ich bin Konservateur.' Dennoch unterdrückte er mit dem Deutschen Bund systematisch liberale und nationale Bewegungen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Heilige Allianz', die nach dem Wiener Kongress 1815 gegründet wurde?",
        answerA = "Ein militärisches Defensivbündnis aller europäischen Staaten",
        answerB = "Ein von Zar Alexander I. initiiertes Bündnis christlicher Monarchen (Russland, Österreich, Preußen) zur gemeinsamen Wahrung christlicher Werte und zur Unterdrückung revolutionärer Bewegungen",
        answerC = "Ein Freihandelsabkommen zwischen den Siegermächten",
        answerD = "Eine Vereinbarung zur Aufteilung der napoleonischen Territorien",
        correctAnswer = 1,
        explanation = "Die Heilige Allianz (September 1815) war ein Bündnis Russlands, Österreichs und Preußens auf Initiative Zar Alexanders I. Sie verpflichtete die Monarchen zu christlichen Grundsätzen und zur gegenseitigen Unterstützung gegen Revolutionen.",
        difficulty = 3,
        funFact = "Der britische Außenminister Castlereagh nannte die Heilige Allianz 'einen erhabenen Unsinn' – Großbritannien blieb daher außen vor."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Territorium erhielt Preußen beim Wiener Kongress als bedeutendsten territorialen Gewinn, der die spätere Industriemacht Preußens begründete?",
        answerA = "Schlesien",
        answerB = "Das Rheinland und Westfalen",
        answerC = "Hannover",
        answerD = "Sachsen (ganz)",
        correctAnswer = 1,
        explanation = "Preußen erhielt das Rheinland und Westfalen – Regionen mit reichen Kohle- und Eisenerzvorkommen, die zur industriellen Basis des späteren Deutschen Kaiserreichs wurden.",
        difficulty = 3,
        funFact = "Viele preußische Staatsmänner sahen das Rheinland zunächst als Last – es lag weit entfernt vom Kernland. Dass es zur Wiege der deutschen Industrie wurde, war damals nicht absehbar."
    ),

    // --- Revolutions of 1848 ---

    Question(
        categoryId = 3,
        questionText = "In welcher Stadt begann im Februar 1848 die erste große Revolution, die eine europaweite Welle auslöste?",
        answerA = "Wien",
        answerB = "Berlin",
        answerC = "Paris",
        answerD = "Mailand",
        correctAnswer = 2,
        explanation = "Die Februarrevolution 1848 in Paris stürzte König Louis-Philippe und führte zur Ausrufung der Zweiten Französischen Republik. Die Nachricht löste revolutionäre Bewegungen in ganz Europa aus.",
        difficulty = 3,
        funFact = "Karl Marx und Friedrich Engels publizierten das Kommunistische Manifest im Februar 1848 – ironischerweise kurz vor dem Beginn der Revolutionen, die sie vorhergesagt hatten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Parlament tagte 1848/49 in der Paulskirche in Frankfurt und versuchte, Deutschland zu einen?",
        answerA = "Der Bundestag des Deutschen Bundes",
        answerB = "Die Frankfurter Nationalversammlung",
        answerC = "Der Norddeutsche Reichstag",
        answerD = "Die Preußische Nationalversammlung",
        correctAnswer = 1,
        explanation = "Die Frankfurter Nationalversammlung (Mai 1848 – Mai 1849) war das erste gesamtdeutsche Parlament. Sie erarbeitete die Paulskirchenverfassung, scheiterte aber letztlich an der Frage Kleindeutschland vs. Großdeutschland und der Ablehnung Preußens.",
        difficulty = 3,
        funFact = "Die Nationalversammlung bot Friedrich Wilhelm IV. von Preußen die Kaiserkrone an – er lehnte ab, weil sie 'aus dem Rinnstein der Revolution' komme."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der entscheidende Streitpunkt bei der Frankfurter Nationalversammlung bezüglich der deutschen Einheit – die 'Kleindeutsche' versus 'Großdeutsche' Frage?",
        answerA = "Ob Deutschland eine Monarchie oder eine Republik werden sollte",
        answerB = "Ob Deutschland mit Österreich (Großdeutschland) oder ohne Österreich unter preußischer Führung (Kleindeutschland) geeinigt werden sollte",
        answerC = "Ob die Verfassung eine Bundesstaat- oder Staatenbund-Lösung vorsehen sollte",
        answerD = "Ob die Grundrechte vor oder nach der Verfassung beschlossen werden sollten",
        correctAnswer = 1,
        explanation = "Die Kleindeutsche Lösung (ohne Österreich, unter Preußen) setzte sich durch, da Österreich sein multinationales Reich nicht aufgeben wollte. Diese Entscheidung bestimmte die spätere Reichsgründung von 1871.",
        difficulty = 3,
        funFact = "Österreichs Ablehnung kam nicht zuletzt daher, dass ein deutsches Reich ohne habsburgische Führung Österreichs Großmachtstellung gefährdet hätte."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer führte die österreichische Armee an, die 1849 die ungarische Revolution niederschlug – mit russischer Unterstützung?",
        answerA = "Windischgrätz",
        answerB = "Radetzky",
        answerC = "Franz Joseph I.",
        answerD = "Haynau (mit russischer Hilfe unter Paskewitsch)",
        correctAnswer = 3,
        explanation = "General Haynau führte die österreichische Armee, die zusammen mit russischen Truppen unter Feldmarschall Paskewitsch die ungarische Revolution im Sommer 1849 niederwarf. Haynau wurde wegen seiner Brutalität als 'Hyäne von Brescia' bekannt.",
        difficulty = 3,
        funFact = "Zar Nikolaus I. schickte 200.000 Soldaten, um dem jungen Kaiser Franz Joseph zu helfen – ein Schritt, der das Verhältnis zwischen den beiden Reichen langfristig vergiftete."
    ),

    // --- Unification of Italy ---

    Question(
        categoryId = 3,
        questionText = "Welche politische Bewegung und welches Königreich trieben die Einigung Italiens (Risorgimento) maßgeblich voran?",
        answerA = "Der Kirchenstaat unter Papst Pius IX.",
        answerB = "Das Königreich Sardinien-Piemont unter König Viktor Emanuel II. und Ministerpräsident Cavour",
        answerC = "Das Königreich Neapel-Sizilien unter den Bourbonen",
        answerD = "Das Herzogtum Toskana",
        correctAnswer = 1,
        explanation = "Sardinien-Piemont unter Viktor Emanuel II. und dem pragmatischen Ministerpräsidenten Cavour war der Motor der italienischen Einigung. Cavour nutzte diplomatische Allianzen (v.a. mit Frankreich) und Kriege geschickt für die Annexionen.",
        difficulty = 3,
        funFact = "Cavour sprach besser Französisch als Italienisch – und der König, dessen Einigung er betrieb, war eigentlich Piemontese und kein 'Italiener' im modernen Sinne."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war Giuseppe Garibaldis 'Expedition der Tausend' (Spedizione dei Mille) im Jahr 1860?",
        answerA = "Ein Aufstand in der Lombardei gegen österreichische Herrschaft",
        answerB = "Eine Seereise von 1.000 Freiwilligen (Rothem-Hemden), die Sizilien und Süditalien von den Bourbonen eroberten",
        answerC = "Eine Invasion Venedigs mit 1.000 Freischärlern",
        answerD = "Ein Marsch auf Rom mit päpstlicher Unterstützung",
        correctAnswer = 1,
        explanation = "Garibaldi landete im Mai 1860 mit ca. 1.000 Freiwilligen ('I Mille') in Marsala, Sizilien. Er eroberte Sizilien und dann das Festland Süditaliens und übergab es Viktor Emanuel II. – ein entscheidender Schritt zur Einigung.",
        difficulty = 3,
        funFact = "Garibaldis Männer trugen rote Hemden – eine Farbe, die ursprünglich gewählt worden sein soll, weil rote Stoffe in Uruguay billig waren, wo Garibaldi im Exil gewesen war."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Abkommen (1858) zwischen Cavour und Napoleon III. sicherte französische Militärhilfe gegen Österreich im Austausch für Nizza und Savoyen?",
        answerA = "Der Vertrag von Turin",
        answerB = "Das Geheimabkommen von Plombières",
        answerC = "Der Friede von Villafranca",
        answerD = "Das Abkommen von Zürich",
        correctAnswer = 1,
        explanation = "Im Geheimabkommen von Plombières (Juli 1858) vereinbarten Cavour und Napoleon III. ein Bündnis: Frankreich würde Sardinien-Piemont im Krieg gegen Österreich helfen, dafür erhielte Frankreich Nizza und Savoyen.",
        difficulty = 3,
        funFact = "Garibaldi war in Nizza geboren und war empört, dass seine Heimatstadt an Frankreich abgetreten wurde – er brach deshalb fast mit Viktor Emanuel II."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann wurde das Königreich Italien ausgerufen und wer wurde sein erster König?",
        answerA = "1860, Garibaldi",
        answerB = "1861, Viktor Emanuel II.",
        answerC = "1866, Viktor Emanuel II.",
        answerD = "1871, Umberto I.",
        correctAnswer = 1,
        explanation = "Das Königreich Italien wurde am 17. März 1861 vom italienischen Parlament ausgerufen, und Viktor Emanuel II. von Sardinien-Piemont wurde zum ersten König von Italien ernannt.",
        difficulty = 3,
        funFact = "Rom war 1861 noch nicht Teil Italiens – die Hauptstadt blieb zunächst Turin, dann Florenz. Rom wurde erst 1871 nach dem Abzug französischer Schutztruppen die Hauptstadt."
    ),

    // --- Crimean War ---

    Question(
        categoryId = 3,
        questionText = "Was war der unmittelbare Auslöser des Krimkrieges (1853–1856)?",
        answerA = "Russlands Annexion der Krim",
        answerB = "Ein Streit über die Schutzrechte christlicher Minderheiten im Osmanischen Reich, insbesondere über das Recht zur Verwaltung der Heiligen Stätten in Jerusalem",
        answerC = "Großbritanniens Blockade russischer Schwarzmeerhäfen",
        answerD = "Osmanische Angriffe auf russische Handelswege",
        correctAnswer = 1,
        explanation = "Der unmittelbare Auslöser war ein Streit zwischen Russland und Frankreich über Schutzrechte christlicher Minderheiten im osmanischen Jerusalem. Dahinter stand Russlands Interesse an einer Ausdehnung auf Kosten des schwächelnden Osmanischen Reiches.",
        difficulty = 3,
        funFact = "Napoleon III. nutzte den Streit gezielt, um Frankreich als Schutzmacht der Katholiken zu profilieren – und gleichzeitig Russlands Einfluss im Nahen Osten einzudämmen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche militärische Belagerung des Krimkrieges dauerte fast ein Jahr und endete mit dem Fall der russischen Festung?",
        answerA = "Die Belagerung von Konstantinopel",
        answerB = "Die Belagerung von Sewastopol (September 1854 – September 1855)",
        answerC = "Die Belagerung von Balaklawa",
        answerD = "Die Belagerung von Odessa",
        correctAnswer = 1,
        explanation = "Die Belagerung von Sewastopol dauerte von September 1854 bis September 1855 – fast ein Jahr. Die Einnahme der Festung durch französische und britische Truppen erzwang Russlands Niederlage.",
        difficulty = 3,
        funFact = "Sewastopol erlangte in der sowjetischen Geschichte heldenhafte Bedeutung: Im Zweiten Weltkrieg wurde es erneut über 250 Tage belagert und 1945 als 'Heldenstadt' ausgezeichnet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wofür ist Florence Nightingale im Krimkrieg berühmt geworden?",
        answerA = "Als erste Frau, die aktiv in der Artillerie kämpfte",
        answerB = "Als Krankenpflegerin, die das Militärhospital in Scutari reformierte und die Sterblichkeitsrate dramatisch senkte",
        answerC = "Als britische Diplomatin, die den Friedensvertrag aushandelte",
        answerD = "Als Fotografin, die den ersten Kriegsdokumentarfilm schuf",
        correctAnswer = 1,
        explanation = "Florence Nightingale revolutionierte die Militärkrankenpflege im Lazarett von Scutari (Üsküdar). Durch Hygienemaßnahmen senkte sie die Sterblichkeitsrate von ca. 40 % auf unter 2 % und begründete die moderne Krankenpflege.",
        difficulty = 3,
        funFact = "Nightingale war eine Pionierin der Datenvisualisierung: Sie erfand das 'Rosendiagramm', eine frühe Form des Kreisdiagramms, um die Ursachen der Soldatensterblichkeit darzustellen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Friedensvertrag beendete den Krimkrieg 1856 und welche wichtige Klausel enthielt er bezüglich Russlands Schwarzmeerflotte?",
        answerA = "Vertrag von Wien – Russland musste die Krim abtreten",
        answerB = "Frieden von Paris (1856) – Russland musste die Schwarzmeerflotte auflösen und durfte keine Kriegsschiffe im Schwarzen Meer unterhalten",
        answerC = "Vertrag von Berlin – Russland erhielt das Recht auf freie Durchfahrt durch die Meerengen",
        answerD = "Friede von London – alle Mächte erhielten Handelsprivilegien im Schwarzen Meer",
        correctAnswer = 1,
        explanation = "Im Frieden von Paris (1856) musste Russland akzeptieren, dass das Schwarze Meer neutralisiert wird und keine Kriegsflotte dort unterhalten werden darf. Russland annullierte diese Bestimmungen nach dem Deutsch-Französischen Krieg 1871.",
        difficulty = 3,
        funFact = "Die Bestimmungen des Pariser Friedens waren für Russland so demütigend, dass Zar Nikolaus I. noch vor dem Kriegsende starb – möglicherweise an den psychischen Folgen der absehbaren Niederlage."
    ),

    // --- Franco-Prussian War ---

    Question(
        categoryId = 3,
        questionText = "Was war die 'Emser Depesche' von 1870 und welche Rolle spielte sie beim Ausbruch des Deutsch-Französischen Krieges?",
        answerA = "Ein Geheimabkommen zwischen Preußen und Österreich gegen Frankreich",
        answerB = "Ein Telegramm König Wilhelms I. über Gespräche mit dem französischen Botschafter, das Bismarck verkürzt und provokant veröffentlichte, um Frankreich zur Kriegserklärung zu reizen",
        answerC = "Frankreichs ultimative Kriegserklärung an Preußen per Telegraf",
        answerD = "Ein russisches Angebot zur Vermittlung im preußisch-französischen Streit",
        correctAnswer = 1,
        explanation = "Bismarck kürzte das Emser Telegramm so, dass es nach einer gegenseitigen Beleidigung zwischen Wilhelm I. und dem französischen Botschafter klang. Damit provozierte er Frankreich zur Kriegserklärung – und Preußen kämpfte als angegriffene Partei.",
        difficulty = 3,
        funFact = "Bismarck schrieb in seinen Memoiren offen, dass er die Depesche so bearbeitete, um 'einen roten Lappen vor den gallischen Stier' zu halten – eine freimütige Eingeständnis diplomatischer Manipulation."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher Schlacht des Deutsch-Französischen Krieges wurde Kaiser Napoleon III. im September 1870 zusammen mit seiner Armee gefangen genommen?",
        answerA = "Die Schlacht bei Gravelotte",
        answerB = "Die Kapitulation bei Sedan",
        answerC = "Die Belagerung von Paris",
        answerD = "Die Schlacht bei Metz",
        correctAnswer = 1,
        explanation = "In der Kesselschlacht bei Sedan (1.–2. September 1870) wurde die französische Armee eingeschlossen. Napoleon III. kapitulierte persönlich mit ca. 100.000 Soldaten – ein beispielloser Triumph der deutschen Streitkräfte.",
        difficulty = 3,
        funFact = "Nach der Kapitulation Napoleons III. wurde in Paris die Dritte Republik ausgerufen – Frankreich kämpfte nun als Republik weiter, während sein Kaiser als Gefangener nach Deutschland gebracht wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Territorium musste Frankreich nach dem Deutsch-Französischen Krieg 1871 an das Deutsche Reich abtreten?",
        answerA = "Die Normandie und Bretagne",
        answerB = "Elsass-Lothringen",
        answerC = "Burgund und Lothringen",
        answerD = "Nur das Elsass",
        correctAnswer = 1,
        explanation = "Im Frankfurter Frieden (Mai 1871) musste Frankreich Elsass-Lothringen an das Deutsche Reich abtreten. Diese Annexion blieb bis 1918 und schürte ein tiefes französisches Ressentiment gegenüber Deutschland.",
        difficulty = 3,
        funFact = "Frankreich zahlte auch eine Kriegsentschädigung von 5 Milliarden Francs – eine Summe, die so schnell beglichen wurde, dass die deutschen Besatzungstruppen früher als erwartet abzogen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo und wann wurde das Deutsche Kaiserreich ausgerufen, und wer wurde zum ersten deutschen Kaiser proklamiert?",
        answerA = "Berlin, 18. Januar 1871, Bismarck",
        answerB = "Im Spiegelsaal von Versailles, 18. Januar 1871, Wilhelm I.",
        answerC = "Frankfurt, 1. September 1870, Wilhelm I.",
        answerD = "Sedan, 2. September 1870, Bismarck",
        correctAnswer = 1,
        explanation = "Am 18. Januar 1871 wurde Wilhelm I. im Spiegelsaal des Schlosses von Versailles zum Deutschen Kaiser ausgerufen. Die Wahl des Ortes im besiegten Frankreich war ein bewusster symbolischer Akt.",
        difficulty = 3,
        funFact = "Wilhelm I. war über den Titel 'Kaiser' unglücklich – er hätte lieber 'König von Preußen' geheißen. Bismarck musste ihn mit diplomatischem Geschick überreden."
    ),

    // --- Bismarck's Alliance System ---

    Question(
        categoryId = 3,
        questionText = "Was war das 'Dreikaiserbündnis' (Dreikaiserliga), das Bismarck 1873 schloss?",
        answerA = "Ein Bündnis zwischen Deutschland, Österreich-Ungarn und dem Osmanischen Reich",
        answerB = "Ein informelles Bündnis zwischen Kaiser Wilhelm I., Kaiser Franz Joseph I. und Zar Alexander II. zur Wahrung des Status quo in Europa",
        answerC = "Eine Allianz der drei deutschen Königreiche (Preußen, Bayern, Sachsen)",
        answerD = "Ein militärisches Defensivbündnis von Deutschland, Österreich-Ungarn und Russland gegen Frankreich",
        correctAnswer = 1,
        explanation = "Die Dreikaiserliga von 1873 war ein informelles Bündnis der drei konservativen Kaiser, das Europa stabilisieren und Frankreich isolieren sollte. Es zerbrach jedoch an österreichisch-russischen Rivalitäten auf dem Balkan.",
        difficulty = 3,
        funFact = "Bismarck versuchte mehrfach, das Bündnis zu erneuern (auch 1881 als 'Dreikaiservertrag'), aber die Interessen Österreichs und Russlands auf dem Balkan erwiesen sich als unvereinbar."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Zweibund' (1879) und gegen wen richtete er sich primär?",
        answerA = "Ein deutsch-österreichisches Defensivbündnis, das sich gegen Russland richtete",
        answerB = "Ein deutsch-russisches Bündnis gegen Frankreich und Österreich",
        answerC = "Ein österreichisch-italienisches Bündnis gegen das Osmanische Reich",
        answerD = "Ein deutsch-britisches Bündnis gegen Russland",
        correctAnswer = 0,
        explanation = "Der Zweibund (1879) zwischen Deutschland und Österreich-Ungarn war ein Defensivbündnis: Griff Russland einen der Partner an, würde der andere helfen. Er bildete das Kernstück von Bismarcks Bündnissystem.",
        difficulty = 3,
        funFact = "Bismarck selbst war skeptisch gegenüber dem Bündnis – er fürchtete, von Österreich in Balkankriege hineingezogen zu werden, die nicht Deutschlands Interessen dienten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Dreibund' (1882) und welche drei Mächte gehörten ihm an?",
        answerA = "Deutschland, Österreich-Ungarn und das Osmanische Reich",
        answerB = "Deutschland, Österreich-Ungarn und Italien",
        answerC = "Deutschland, Russland und Österreich-Ungarn",
        answerD = "Deutschland, Österreich-Ungarn und die Niederlande",
        correctAnswer = 1,
        explanation = "Der Dreibund (1882) war ein Defensivbündnis zwischen Deutschland, Österreich-Ungarn und Italien, das den Zweibund um die dritte europäische Großmacht erweiterte. Italien verließ den Dreibund 1915 und trat auf Seiten der Entente in den Ersten Weltkrieg ein.",
        difficulty = 3,
        funFact = "Italiens Beitritt war ironisch: Das Land hatte erst durch Kriege gegen Österreich seine Einheit errungen und schloss sich nun einem Bündnis mit seinem ehemaligen Feind an."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Rückversicherungsvertrag' (1887) zwischen Deutschland und Russland?",
        answerA = "Ein Vertrag, der Deutschland gegen russische Angriffe absicherte",
        answerB = "Ein Geheimvertrag, der Russland Neutralität zusicherte, falls Frankreich Deutschland angriff, und Deutschland Neutralität, falls Österreich Russland angriff",
        answerC = "Ein Handelsvertrag zwischen Deutschland und Russland zur Absicherung gegenseitiger Wirtschaftsinteressen",
        answerD = "Ein Militärpakt, der beide Mächte zur gegenseitigen Hilfe verpflichtete",
        correctAnswer = 1,
        explanation = "Der Rückversicherungsvertrag (1887) war ein Geheimvertrag, der sowohl den Zweibund als auch das Dreikaiservertragssystem ergänzte. Er sicherte Neutralität ab, solange kein Angriffskrieg geführt wurde. Kaiser Wilhelm II. ließ ihn 1890 auslaufen – ein Fehler, der den Weg zum Zweibund Frankreich-Russland öffnete.",
        difficulty = 3,
        funFact = "Bismarck jonglierte gleichzeitig mit Österreich und Russland als Bündnispartnern – obwohl beide Rivalen auf dem Balkan waren. Er allein war in der Lage, dieses widersprüchliche System aufrechtzuerhalten."
    ),

    Question(
        categoryId = 3,
        questionText = "Warum ließ Kaiser Wilhelm II. den Rückversicherungsvertrag mit Russland 1890 nicht erneuern?",
        answerA = "Weil Russland den Vertrag selbst ablehnte",
        answerB = "Weil Bismarcks Nachfolger Caprivi und der Kaiser den Vertrag für unvereinbar mit dem Zweibund hielten und eine eindeutigere Ausrichtung wollten",
        answerC = "Weil Großbritannien dagegen protestierte",
        answerD = "Weil Russland Österreich angegriffen hatte",
        correctAnswer = 1,
        explanation = "Bismarcks Nachfolger Caprivi und Kaiser Wilhelm II. hielten das gleichzeitige Aufrechterhalten von Verpflichtungen gegenüber Österreich und Russland für unlautere Doppelzüngigkeit. Das Nichtернeuern des Vertrages trieb Russland in die Arme Frankreichs.",
        difficulty = 3,
        funFact = "Bismarck warnte prophetisch, dass ohne den Rückversicherungsvertrag Russland und Frankreich sich verbünden würden – was genau 1894 mit dem Zweibund-Vertrag geschah."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Berliner Kongress' von 1878 und welches Problem versuchte er zu lösen?",
        answerA = "Ein Kongress zur Aufteilung Afrikas unter die europäischen Mächte",
        answerB = "Ein Kongress zur Neuordnung des Balkans nach dem Russisch-Osmanischen Krieg, der den russischen Einfluss auf dem Balkan (San-Stefano-Frieden) zurückstutzte",
        answerC = "Ein Abrüstungsgipfel nach dem Deutsch-Französischen Krieg",
        answerD = "Ein Handelsabkommen zwischen den europäischen Großmächten",
        correctAnswer = 1,
        explanation = "Bismarck leitete den Berliner Kongress (1878) als 'ehrlicher Makler'. Er revidierte den für Russland vorteilhaften Frieden von San Stefano und schuf ein neues Gleichgewicht auf dem Balkan – zum Verdruss Russlands.",
        difficulty = 3,
        funFact = "Russland fühlte sich auf dem Berliner Kongress um seinen Kriegsgewinn betrogen – obwohl Deutschland neutral gewesen war. Dies vergiftete die deutsch-russischen Beziehungen dauerhaft."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche politische Strategie verfolgte Bismarck nach 1871 gegenüber Frankreich?",
        answerA = "Eine Politik der Versöhnung durch Handelsverträge",
        answerB = "Die dauerhafte Isolierung Frankreichs durch ein Netz von Bündnissen, das verhinderte, dass Frankreich Verbündete fand, um Elsass-Lothringen zurückzugewinnen",
        answerC = "Ein Präventivkrieg gegen Frankreich, bevor es sich erholen konnte",
        answerD = "Eine aggressive Kolonialpolitik, um Frankreich in Übersee zu schwächen",
        correctAnswer = 1,
        explanation = "Bismarcks Bündnissystem hatte als zentrales Ziel die Isolierung Frankreichs. Er wollte verhindern, dass Frankreich einen Verbündeten unter den Großmächten findet, der einen Revanchekrieg um Elsass-Lothringen unterstützen würde.",
        difficulty = 3,
        funFact = "Bismarck sagte: 'Ich habe Nachtmahre. Immer sehe ich den Alptraum von Koalitionen.' Seine gesamte Außenpolitik war vom Schrecken einer Zwei-Fronten-Koalition gegen Deutschland getrieben."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte der Berliner Kongress von 1884/85 (Kongo-Konferenz) für die Bismarcksche Außenpolitik?",
        answerA = "Bismarck nutzte ihn, um Deutschland koloniale Gebiete in Afrika zu sichern und gleichzeitig durch Kooperation mit Frankreich England zu isolieren",
        answerB = "Bismarck lehnte jede Kolonialpolitik ab und nutzte den Kongress nur als diplomatisches Forum",
        answerC = "Deutschland erhielt das gesamte Kongobecken zugesprochen",
        answerD = "Bismarck schloss auf dem Kongress ein Geheimabkommen mit Belgien",
        correctAnswer = 0,
        explanation = "Auf der Berliner Kongo-Konferenz (1884/85) errang Deutschland nicht nur Kolonien (Kamerun, Togo, Deutsch-Südwestafrika), sondern nutzte auch die Zusammenarbeit mit Frankreich gegen britische Interessen als diplomatisches Mittel.",
        difficulty = 3,
        funFact = "Bismarck war persönlich lange gegen Kolonien – er sagte, seine Karte Afrikas liege in Europa. Er betrieb Kolonialpolitik v.a. auf Druck innenpolitischer Kräfte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Aufstand 1871 erschütterte Paris nach dem Deutsch-Französischen Krieg und wurde blutig niedergeschlagen?",
        answerA = "Die Pariser Julirevolution",
        answerB = "Die Pariser Kommune",
        answerC = "Der Aufstand der Communards",
        answerD = "Die Febrevolution von Paris",
        correctAnswer = 1,
        explanation = "Die Pariser Kommune (März–Mai 1871) war eine radikale Stadtregierung, die Paris für 72 Tage kontrollierte. Sie wurde von der französischen Versailler Regierung in der 'Blutigen Woche' (Mai 1871) brutal niedergeschlagen – ca. 10.000–30.000 Kommunarden wurden getötet.",
        difficulty = 3,
        funFact = "Karl Marx sah in der Pariser Kommune das erste Beispiel der Diktatur des Proletariats – obwohl sie nur 72 Tage dauerte und ihre Mitglieder keine einheitliche marxistische Ideologie teilten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Kulturkampf' unter Bismarck in den 1870er Jahren?",
        answerA = "Ein preußisches Schulreformprogramm zur Modernisierung des Bildungswesens",
        answerB = "Ein staatlicher Kampf gegen den politischen Katholizismus und die Autorität des Papstes, der u.a. die Zivilehe einführte und Jesuiten aus Preußen vertrieb",
        answerC = "Ein Programm zur Germanisierung polnischer Bevölkerungsgruppen in Preußen",
        answerD = "Eine Auseinandersetzung zwischen Bismarck und den Liberalen über die Pressefreiheit",
        correctAnswer = 1,
        explanation = "Der Kulturkampf (1871–1878) war Bismarcks Auseinandersetzung mit der Katholischen Kirche. Er richtete sich gegen ultramontane Einflüsse und die Zentrumspartei. Bismarck scheiterte weitgehend – er beendete den Kulturkampf, um die Kirche gegen die Sozialdemokraten als Verbündeten zu gewinnen.",
        difficulty = 3,
        funFact = "Bismarck prägte das Wort 'Kulturkampf' – er benutzte es als Propagandabegriff, um seinen Konflikt mit der Kirche als Kampf der modernen Zivilisation gegen mittelalterliche Rückständigkeit darzustellen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Gesetz Bismarcks von 1878 versuchte, die aufstrebende Sozialdemokratie in Deutschland zu unterdrücken?",
        answerA = "Das Sozialistengesetz",
        answerB = "Das Vereinsgesetz",
        answerC = "Das Pressegesetz",
        answerD = "Das Staatsgefährdungsgesetz",
        correctAnswer = 0,
        explanation = "Das Sozialistengesetz (1878–1890) verbot sozialdemokratische Organisationen, Versammlungen und Schriften. Trotzdem wuchs die SPD weiter, da Kandidaten kandidieren durften. Es war eines der wenigen großen Scheitern Bismarcks.",
        difficulty = 3,
        funFact = "Bismarck kombinierte das Sozialistengesetz mit der Sozialversicherungsgesetzgebung (Kranken-, Unfall-, Rentenversicherung) – 'Zuckerbrot und Peitsche': die Arbeiter unterdrücken und gleichzeitig mit sozialen Leistungen gewinnen."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr und aus welchem Anlass wurde Bismarck von Kaiser Wilhelm II. als Reichskanzler entlassen?",
        answerA = "1888, wegen eines Krieges mit Frankreich",
        answerB = "1890, wegen grundlegender Meinungsverschiedenheiten über den Umgang mit der Sozialdemokratie und die Außenpolitik",
        answerC = "1892, wegen des Scheiterns des Rückversicherungsvertrages",
        answerD = "1895, wegen einer Erkrankung Bismarcks",
        correctAnswer = 1,
        explanation = "Wilhelm II. entließ Bismarck im März 1890. Auslöser waren Konflikte über die Erneuerung des Sozialistengesetzes (Wilhelm II. war dagegen), die Außenpolitik und generell Wilhelms Wunsch, selbst zu regieren.",
        difficulty = 3,
        funFact = "Der britische Punch illustrierte Bismarcks Entlassung mit der berühmten Karikatur 'Dropping the Pilot' (Den Lotsen von Bord lassen) – eines der bekanntesten politischen Bilder des 19. Jahrhunderts."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das Ergebnis des Österreichisch-Preußischen Krieges (Deutschen Krieges) von 1866, der Preußens Vorherrschaft in Deutschland begründete?",
        answerA = "Österreich trat Böhmen an Preußen ab",
        answerB = "Preußen schloss Österreich aus dem Norddeutschen Bund aus; durch den Frieden von Prag musste Österreich dem Norddeutschen Bund zustimmen und Venetien an Italien abtreten",
        answerC = "Österreich und Preußen einigten sich auf gemeinsame Führung Deutschlands im Dualismus",
        answerD = "Bayern und Baden schlossen sich als Sieger dem Deutschen Bund an",
        correctAnswer = 1,
        explanation = "Im Frieden von Prag (1866) nach der Entscheidungsschlacht bei Königgrätz musste Österreich den Norddeutschen Bund unter preußischer Führung anerkennen und Venetien an Italien abtreten. Österreich verlor seinen Einfluss in Deutschland dauerhaft.",
        difficulty = 3,
        funFact = "Bismarck bestand auf einem milden Frieden gegenüber Österreich – gegen den Willen Wilhelms I. und der Generäle, die Wien besetzen wollten. Bismarck wollte Österreich als künftigen Verbündeten behalten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Norddeutsche Bund' (1867–1871) und welche Rolle spielte er auf dem Weg zur deutschen Einheit?",
        answerA = "Ein loser Staatenbund der norddeutschen Fürstentümer ohne preußische Führung",
        answerB = "Ein von Preußen dominierter Bundesstaat mit Verfassung, gemeinsamem Militär und Zollpolitik, der als Vorläufer des Deutschen Kaiserreichs diente",
        answerC = "Eine Wirtschaftsunion der deutschen Staaten nördlich des Mains ohne politische Einheit",
        answerD = "Eine kurzlebige Konföderation, die bereits vor dem Krieg gegen Frankreich wieder aufgelöst wurde",
        correctAnswer = 1,
        explanation = "Der Norddeutsche Bund (1867) war Preußens Reaktion auf den Sieg von 1866. Er vereinte alle deutschen Staaten nördlich des Mains unter preußischer Führung mit gemeinsamer Verfassung und Bundesrat – eine Blaupause für das spätere Kaiserreich.",
        difficulty = 3,
        funFact = "Die Verfassung des Norddeutschen Bundes war weitgehend identisch mit der späteren Reichsverfassung von 1871 – Bismarck musste kaum etwas ändern, als die süddeutschen Staaten hinzukamen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann und wo wurde Garibaldi bei dem Versuch, Rom zu erobern, von französischen und päpstlichen Truppen gestoppt?",
        answerA = "1862 in Aspromonte und 1867 in Mentana",
        answerB = "1860 in Neapel",
        answerC = "1866 in Venetien",
        answerD = "1870 in Turin",
        correctAnswer = 0,
        explanation = "Garibaldis erste Versuche, Rom zu nehmen, wurden 1862 in der Gefechtsschlacht von Aspromonte und 1867 bei Mentana durch französische Truppen (die den Papst schützten) gestoppt. Erst 1870, nach dem Abzug der Franzosen wegen des Deutsch-Französischen Krieges, fiel Rom.",
        difficulty = 3,
        funFact = "Bei Aspromonte wurden Garibaldi und seine Männer von der eigenen (sardinischen) Armee beschossen – eine groteske Situation, die Cavours politisches Dilemma zeigt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter dem Begriff 'Risorgimento' in der italienischen Geschichte?",
        answerA = "Die Wiedergeburt der Römischen Republik im 19. Jahrhundert",
        answerB = "Die politische und kulturelle Bewegung für die nationale Einheit und Unabhängigkeit Italiens im 19. Jahrhundert",
        answerC = "Die religiöse Reformbewegung der italienischen Kirche",
        answerD = "Die wirtschaftliche Modernisierungsbewegung in Norditalien",
        correctAnswer = 1,
        explanation = "Risorgimento (ital. 'Wiederauferstehung') bezeichnet die politisch-kulturelle Bewegung für Italiens nationale Einigung (ca. 1815–1871). Ihre wichtigsten Vertreter waren Mazzini (Ideologe), Cavour (Staatsmann) und Garibaldi (Freiheitskämpfer).",
        difficulty = 3,
        funFact = "Giuseppe Mazzini gründete 1831 die Bewegung 'Junges Italien' – eine der ersten modernen nationalistischen Geheimgesellschaften, die europaweit nachahmende Bewegungen inspirierten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Rolle spielte das Königreich Sardinien-Piemont im Krimkrieg (1853–1856), und was gewann es dadurch?",
        answerA = "Es kämpfte auf Seiten Russlands und erhielt dafür venezianische Gebiete",
        answerB = "Es schickte 15.000 Soldaten auf Seiten Frankreichs und Großbritanniens, um diplomatisch auf dem Friedenskongress in Paris als gleichberechtigte Macht anerkannt zu werden und die 'italienische Frage' auf die europäische Agenda zu bringen",
        answerC = "Es blieb neutral und profitierte von Waffenlieferungen an beide Seiten",
        answerD = "Es vermittelte zwischen Russland und den Westmächten und erhielt dafür Österreichs Neutralität",
        correctAnswer = 1,
        explanation = "Cavour schickte sardische Truppen in den Krimkrieg, obwohl Sardinien kein direktes Interesse hatte. Sein Ziel: Am Pariser Friedenskongress als Großmacht auftreten und die 'Unterdrückung Italiens durch Österreich' zum europäischen Thema zu machen.",
        difficulty = 3,
        funFact = "Cavours Strategie war brillant: Obwohl Sardinien kaum zum Kriegsergebnis beitrug, erschien Cavour in Paris und hielt eine Rede über Italiens Lage – die ersten Schritte zur europäischen Unterstützung der Einigung."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche zwei Hauptströmungen kämpften in der Frankfurter Nationalversammlung 1848 um die künftige Staatsform Deutschlands?",
        answerA = "Monarchisten und Anarchisten",
        answerB = "Liberale (konstitutionelle Monarchie) und Demokraten (Republik)",
        answerC = "Konservative (Fürstenherrschaft) und Sozialisten (Räterepublik)",
        answerD = "Katholicken (Großdeutschland) und Protestanten (Kleindeutschland)",
        correctAnswer = 1,
        explanation = "In der Paulskirche stritten vor allem liberale Bürger (die eine konstitutionelle Monarchie unter einem Erbkaiser wollten) gegen demokratische Linke (die eine Volksrepublik anstrebten). Die Liberalen setzten sich durch – das Kaisertum unter Preußen wurde angeboten.",
        difficulty = 3,
        funFact = "Friedrich Wilhelm IV. lehnte die angebotene Krone ab – seine berühmte Begründung, er wolle keine 'Krone aus dem Rinnstein der Revolution', besiegelte das Scheitern der Nationalversammlung."
    ),

)
