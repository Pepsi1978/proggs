package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsExpert4(): List<Question> = listOf(

    // --- Concert of Europe / Eastern Question ---

    Question(
        categoryId = 3,
        questionText = "Welcher Vertrag von 1840 beendete die zweite Orientalische Krise und zwang Muhammad Ali zum Rückzug aus Syrien?",
        answerA = "Londoner Vertrag (1840)",
        answerB = "Vertrag von Unkiar Skelessi (1833)",
        answerC = "Pariser Friede (1856)",
        answerD = "Vertrag von Adrianopel (1829)",
        correctAnswer = 0,
        explanation = "Der Londoner Vertrag vom 15. Juli 1840 zwischen Großbritannien, Österreich, Preußen, Russland und dem Osmanischen Reich zwang Muhammad Ali, den Vizekönig Ägyptens, seine Truppen aus Syrien, dem Libanon und Arabien zurückzuziehen. Frankreich, das Muhammad Ali unterstützt hatte, war bei den Verhandlungen ausgeschlossen worden.",
        difficulty = 4,
        funFact = "Der Ausschluss Frankreichs aus dem Londoner Vertrag von 1840 führte zu einer schweren Krise im Konzert Europas. Frankreich drohte kurzzeitig mit Krieg, lenkte aber letztlich ein – ein Zeichen dafür, wie das Mächtekonsert den Frieden trotz Spannungen wahrte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was besagte die 'Sick Man of Europe'-Metapher, die Zar Nikolaus I. 1853 prägte?",
        answerA = "Das Osmanische Reich sei dem Untergang geweiht, seine Erbfolge müsse zwischen Großmächten geregelt werden",
        answerB = "Russland leide unter innenpolitischer Schwäche nach den Dekabristen-Aufständen",
        answerC = "Das Habsburgerreich sei durch Nationalitätenkonflikte zerrissen",
        answerD = "Frankreich habe sich nach 1815 noch immer nicht erholt",
        correctAnswer = 0,
        explanation = "Zar Nikolaus I. bezeichnete das Osmanische Reich gegenüber dem britischen Botschafter als 'kranken Mann am Bosporus' ('sick man of Europe'). Er meinte, das Imperium stehe kurz vor dem Zerfall und schlug vor, Großbritannien und Russland sollten seine Erbschaft schon jetzt aufteilen – was Großbritannien ablehnte und damit die Vorläufer des Krimkrieges einleitete.",
        difficulty = 4,
        funFact = "Ironischerweise überlebte das 'kranke' Osmanische Reich Zar Nikolaus I. um fast 70 Jahre. Das Reich endete erst 1922, der Zar starb 1855 mitten im Krimkrieg – möglicherweise an Gram über seine militärische Niederlage."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bestimmung des Pariser Friedens von 1856 demütigte Russland am stärksten?",
        answerA = "Abtretung der Krim an das Osmanische Reich",
        answerB = "Neutralisierung des Schwarzen Meeres – Russland durfte dort keine Kriegsflotte unterhalten",
        answerC = "Verbot russischer Expansion in Zentralasien",
        answerD = "Einschränkung der russischen Kontrolle über Polen",
        correctAnswer = 1,
        explanation = "Artikel XI des Pariser Friedens von 1856 neutralisierte das Schwarze Meer: Weder Russland noch das Osmanische Reich durften dort Kriegsschiffe stationieren oder Seearsenale anlegen. Für Russland, das das Schwarze Meer als sein Binnenmeer betrachtete, war dies eine tiefe Demütigung. Russland hob diese Klausel 1871 nach dem deutsch-französischen Krieg einseitig auf.",
        difficulty = 4,
        funFact = "Russland nutzte den Pontus-Vertrag von 1871 geschickt: Während Europa mit dem Deutsch-Französischen Krieg abgelenkt war, erklärte Russland einseitig, die Pontusklauseln nicht mehr anzuerkennen. Auf der Londoner Konferenz 1871 akzeptierten die Großmächte dies widerwillig."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Klausel des Berliner Kongresses 1878 beendete die russischen Gebietsgewinne aus dem Vertrag von San Stefano?",
        answerA = "Artikel 3: Rückgabe Bessarabiens an Rumänien",
        answerB = "Die drastische Verkleinerung Bulgariens auf ein Viertel der im Vertrag von San Stefano vorgesehenen Größe",
        answerC = "Verbot russischer Truppenstationierung am Bosporus",
        answerD = "Rückgabe von Kars und Batum an das Osmanische Reich",
        correctAnswer = 1,
        explanation = "Der Vertrag von San Stefano (März 1878) hatte ein riesiges 'Groß-Bulgarien' geschaffen, das die russische Einflusssphäre bis ans Mittelmeer ausgedehnt hätte. Beim Berliner Kongress (Juni/Juli 1878) zerschnitt Bismarck als 'ehrlicher Makler' dieses Konstrukt: Bulgarien wurde auf ein Viertel der ursprünglichen Fläche reduziert, und Ostrum­elien blieb unter osmanischer Oberhoheit als autonome Provinz.",
        difficulty = 4,
        funFact = "Der russische Außenminister Gortschakow verließ den Berliner Kongress bitter enttäuscht und sagte, dies sei 'die dunkelste Seite seiner Karriere'. Russland hatte im Krieg gegen die Türkei 100.000 Soldaten verloren und sah die Früchte des Sieges auf dem Kongresstisch zerstückelt."
    ),

    // --- Opium Wars ---

    Question(
        categoryId = 3,
        questionText = "Was war das unmittelbare Ereignis, das den Zweiten Opiumkrieg (1856–1860) auslöste?",
        answerA = "Die Versenkung britischer Handelsschiffe im Perlfluss-Delta",
        answerB = "Der Arrow-Zwischenfall: Chinesische Behörden enterten das Schiff 'Arrow' und verhafteten die Besatzung",
        answerC = "Die Ermordung britischer Kaufleute in Kanton",
        answerD = "Die Weigerung Chinas, Opiumlieferungen zu bezahlen",
        correctAnswer = 1,
        explanation = "Am 8. Oktober 1856 enterten chinesische Behörden das Schiff 'Arrow', das unter britischer Flagge registriert war (obwohl die Registrierung abgelaufen war), und verhafteten 12 chinesische Besatzungsmitglieder. Großbritannien nutzte diesen Vorwand für einen neuen Krieg, an dem sich Frankreich (nach dem Tod eines Missionars) anschloss.",
        difficulty = 4,
        funFact = "Die 'Arrow'-Flagge war zum Zeitpunkt des Vorfalls bereits seit 11 Tagen abgelaufen – Großbritanniens Casus Belli war also rechtlich mehr als dubios. Im Unterhaus bezeichnete Richard Cobden den Krieg als 'Nationalschande', trotzdem stimmte das Parlament für ihn."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Vertrag beendete den Ersten Opiumkrieg und welche Stadt wurde als erster Freihafen abgetreten?",
        answerA = "Vertrag von Nanjing (1842) – Hongkong Island",
        answerB = "Vertrag von Tientsin (1858) – Shanghai",
        answerC = "Vertrag von Peking (1860) – Kowloon",
        answerD = "Vertrag von Kanton (1843) – Guangzhou",
        correctAnswer = 0,
        explanation = "Der Vertrag von Nanjing (29. August 1842) beendete den Ersten Opiumkrieg. China musste Hongkong Island (nicht die ganze Halbinsel) an Großbritannien abtreten, 21 Millionen Silberdollar Entschädigung zahlen und fünf Vertragshäfen öffnen: Kanton, Amoy, Fuzhou, Ningbo und Shanghai.",
        difficulty = 4,
        funFact = "Der Vertrag von Nanjing gilt als das erste der 'ungleichen Verträge' in der chinesischen Geschichte. In China wird diese Periode bis 1949 als 'Jahrhundert der Demütigung' bezeichnet – ein Trauma, das bis heute das chinesische Nationalbewusstsein prägt."
    ),

    // --- Meiji Japan ---

    Question(
        categoryId = 3,
        questionText = "Was war die 'Iwakura-Mission' (1871–1873) und welches war ihr Hauptziel?",
        answerA = "Eine japanische Delegation aus 107 Regierungsbeamten, die Europa und Amerika bereiste, um westliche Institutionen zu studieren und Vertragsrevisionen auszuhandeln",
        answerB = "Eine militärische Gesandtschaft zur Beschaffung westlicher Waffen",
        answerC = "Eine Handelsexpedition zur Öffnung westlicher Märkte für japanische Seide",
        answerD = "Eine diplomatische Mission zur Anerkennung der Meiji-Restauration durch Großmächte",
        correctAnswer = 0,
        explanation = "Die Iwakura-Mission unter Führung von Iwakura Tomomi bereiste 1871–1873 die USA und zwölf europäische Länder. Ziel war sowohl das Studium westlicher politischer, wirtschaftlicher und bildungsbezogener Systeme als auch die Aushandlung einer Revision der ungleichen Verträge. Letzteres scheiterte, aber die Erkenntnisse der Mission prägten die Meiji-Reformen fundamental.",
        difficulty = 4,
        funFact = "Fast die gesamte Meiji-Führungselite war Teil der Mission – darunter fünf der neun Staatsratsmitglieder. Zurück in Japan beschleunigte sich die Modernisierung dramatisch. Die Begleiterin Tsuda Umeko (damals 8 Jahre alt) gründete später die erste Frauenuniversität Japans."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Dokument von 1889 formalisierte Japans konstitutionelle Monarchie nach preußischem Vorbild?",
        answerA = "Die Meiji-Verfassung (Kaiserliche Verfassung von Japan)",
        answerB = "Das Imperiale Reskript über Bildung",
        answerC = "Der Vertrag von Shimonoseki",
        answerD = "Das Fünf-Artikel-Eidschwur",
        correctAnswer = 0,
        explanation = "Die Meiji-Verfassung (Dai-Nihon Teikoku Kenpō), am 11. Februar 1889 verkündet, orientierte sich stark an der preußischen Verfassung von 1871. Itō Hirobumi, ihr Hauptarchitekt, hatte extra Preußen und Deutschland bereist, um das System zu studieren. Die Verfassung sicherte dem Kaiser weitreichende Prärogativrechte und ließ den Reichstag (Teikoku Gikai) nur begrenzte Macht.",
        difficulty = 4,
        funFact = "Itō Hirobumi reiste nach Deutschland, um Rudolf von Gneist und Lorenz von Stein zu konsultieren. Er bevorzugte das preußische Modell, weil es monarchische Macht stärker betonte als britischer Parlamentarismus. Stein warnte ihn allerdings, eine Verfassung ohne soziale Reformen würde nicht funktionieren."
    ),

    // --- Scramble for Africa / Berlin Conference ---

    Question(
        categoryId = 3,
        questionText = "Welcher Artikel der Berliner Kongoakte von 1885 legte das Prinzip der 'effektiven Besetzung' fest?",
        answerA = "Artikel 35",
        answerB = "Artikel 12",
        answerC = "Artikel 6",
        answerD = "Artikel 27",
        correctAnswer = 0,
        explanation = "Artikel 35 der Berliner Kongoakte verpflichtete die Unterzeichner, bei der Besitzergreifung afrikanischer Küstengebiete den anderen Mächten Mitteilung zu machen und 'eine genügend wirksame Autorität' zu schaffen, um erworbene Rechte zu schützen und Handelsfreiheit zu gewährleisten. Dieses 'Hinterland-Prinzip' trieb den Wettlauf um Afrika erheblich an.",
        difficulty = 4,
        funFact = "Die Berliner Konferenz (1884–1885) teilte Afrika auf, ohne dass ein einziger afrikanischer Vertreter anwesend war. Von 54 heute existierenden afrikanischen Staaten haben 44 Grenzen, die geradelinig dem kolonialen Stift folgen – eine Ursache vieler moderner Konflikte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Fashoda-Zwischenfall (1898) und wer gab nach?",
        answerA = "Eine britisch-französische Konfrontation im Sudan: Frankreich gab nach und zog seine Truppen zurück",
        answerB = "Ein deutsch-britischer Konflikt um Ostafrika: Deutschland gab nach",
        answerC = "Ein belgisch-britischer Streit um Kongobecken: Belgien gab nach",
        answerD = "Ein französisch-italienischer Konflikt um Tunesien: Italien gab nach",
        correctAnswer = 0,
        explanation = "Im September 1898 trafen britische Truppen unter Kitchener und eine französische Expedition unter Marchand im sudanesischen Fashoda aufeinander. Beide beanspruchten das Gebiet. Nach monatelanger diplomatischer Krise zog Frankreich im November 1898 zurück, da es militärisch und diplomatisch (nach der Dreyfus-Affäre innenpolitisch geschwächt) nachgeben musste. Die Krise besiegelte Großbritanniens Kontrolle über Ägypten und den Sudan.",
        difficulty = 4,
        funFact = "Der Fashoda-Zwischenfall hatte paradoxerweise eine positive Langzeitwirkung: Die Demütigung Frankreichs führte zu einem Umschwenken der französischen Außenpolitik weg von der Rivalität mit Großbritannien hin zur Entente. Nur sechs Jahre später, 1904, schlossen beide die Entente Cordiale."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Begriff prägte der britische Kolonialminister Joseph Chamberlain für die Idee, das Empire durch Präferenzzölle wirtschaftlich zu integrieren?",
        answerA = "Imperial Federation",
        answerB = "Tariff Reform / Imperial Preference",
        answerC = "Greater Britain Policy",
        answerD = "Colonial Union Scheme",
        correctAnswer = 1,
        explanation = "Joseph Chamberlain lancierte 1903 seine 'Tariff Reform'-Kampagne, die 'Imperial Preference' (bevorzugte Zölle innerhalb des Empire) gegen freien Handel mit der restlichen Welt stellte. Damit brach er mit der britischen Freihandelsdoktrin und spaltete die Konservative Partei. Die Kampagne scheiterte letztlich, aber sie veränderte die britische Innenpolitik nachhaltig.",
        difficulty = 4,
        funFact = "Chamberlains Tariff-Reform-Kampagne wurde zur wichtigsten Spaltungsfrage der britischen Politik vor dem Ersten Weltkrieg. Sein Sohn Neville Chamberlain sollte später als Premierminister durch das Münchner Abkommen berühmt-berüchtigt werden – eine tragische dynastische Parallele."
    ),

    // --- Boer Wars ---

    Question(
        categoryId = 3,
        questionText = "Was war der 'Jameson Raid' (1895/96) und welche politischen Konsequenzen hatte er?",
        answerA = "Ein gescheiterter britischer Putschversuch in der Südafrikanischen Republik (Transvaal), der die Spannungen zum Burenkrieg eskalierte",
        answerB = "Ein erfolgreicher britischer Angriff auf Johannesburg zur Niederschlagung eines Minenarbeiterstreiks",
        answerC = "Eine Strafexpedition gegen den Zululand-König Cetshwayo",
        answerD = "Eine geheime Militäroperation zur Sicherung der Kapkolonie gegen deutsche Einmischung",
        correctAnswer = 0,
        explanation = "Leander Starr Jameson führte im Dezember 1895/Januar 1896 mit 500 Mann einen Einfall in die Südafrikanische Republik (Transvaal) durch, um einen erwarteten Uitlander-Aufstand zu unterstützen – der nie stattfand. Die Expedition scheiterte kläglich. Cecil Rhodes musste zurücktreten, der 'Krüger-Telegramm' Wilhelms II. (das Transvaal beglückwünschte) schädigte die deutsch-britischen Beziehungen dauerhaft.",
        difficulty = 4,
        funFact = "Kaiser Wilhelms II. Telegramm an Präsident Krüger, in dem er zur 'Abwehr des Angriffs' gratulierte, löste in Großbritannien einen Sturm der Entrüstung aus. Es war ein früher Indikator für die zunehmend feindlichen Beziehungen zwischen Deutschland und Großbritannien, die zum Ersten Weltkrieg führten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche britische Maßnahme im Zweiten Burenkrieg (1899–1902) führte zum Tod von etwa 26.000 Burenzivilisten?",
        answerA = "Flächenbombardierung burischer Städte",
        answerB = "Konzentrationslager für Burenzivilisten, in denen Seuchen und Hunger wüteten",
        answerC = "Vergiftung von Wasserquellen durch britische Truppen",
        answerD = "Deportation der burischen Bevölkerung nach Indien",
        correctAnswer = 1,
        explanation = "General Kitchener ließ ab 1900 burische Farmen niederbrennen und die Zivilbevölkerung (Frauen, Kinder, Alte) in Konzentrationslager treiben, um die Guerillakämpfer von ihrer Versorgungsbasis abzuschneiden. In den überfüllten Lagern brachen Typhus, Masern und andere Seuchen aus. Etwa 26.000 Buren – überwiegend Kinder – starben, ebenso mindestens 14.000 schwarzafrikanische Lagerinsassen.",
        difficulty = 4,
        funFact = "Emily Hobhouse, eine britische Sozialreformerin, besuchte die Lager 1901 und veröffentlichte einen vernichtenden Bericht. Kitchener nannte sie öffentlich eine 'hysterische Frau'. Liberale Parlamentarier wie David Lloyd George nannten die Lager 'Methode der Barbarei' – ein Begriff, der in der Geschichte haften blieb."
    ),

    // --- Kulturkampf ---

    Question(
        categoryId = 3,
        questionText = "Welches Gesetz von 1872 war der erste große Schlag Bismarcks im Kulturkampf?",
        answerA = "Das Jesuitengesetz – Ausweisung des Jesuitenordens aus Deutschland",
        answerB = "Die Maigesetze – staatliche Kontrolle über kirchliche Ausbildung",
        answerC = "Das Kanzelparagraph-Gesetz – Verbot politischer Predigten",
        answerD = "Das Zivilstandsgesetz – Einführung der Zivilehe",
        correctAnswer = 0,
        explanation = "Das Jesuitengesetz vom 4. Juli 1872 verbot den Jesuitenorden im Deutschen Reich und verwies seine Mitglieder des Landes. Es war Bismarcks erster massiver Angriff auf die katholische Kirche im Kulturkampf und richtete sich gegen einen Orden, den er als staatsfeindlich und ultramontanistisch betrachtete. Das Gesetz blieb bis 1917 in Kraft.",
        difficulty = 4,
        funFact = "Das Jesuitengesetz war 45 Jahre in Kraft und überlebte damit den Kulturkampf selbst bei weitem. Bismarck beendete den Kulturkampf in den 1880ern aus politischem Kalkül (er brauchte das Zentrum gegen die Sozialdemokraten), aber das Jesuitengesetz blieb – ein Zeugnis, wie sich Gesetze von ihrem ursprünglichen Zweck ablösen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bewog Bismarck ab 1878/79, den Kulturkampf zu beenden und mit dem Zentrum Frieden zu schließen?",
        answerA = "Tod Pius IX. und Wahl des gemäßigteren Leo XIII.",
        answerB = "Die wachsende Stärke der Sozialdemokratie, gegen die Bismarck das Zentrum als konservativen Verbündeten brauchte",
        answerC = "Militärische Niederlagen des Deutschen Reiches in Kolonialkriegen",
        answerD = "Druck von Kaiser Wilhelm I. auf Bismarck",
        correctAnswer = 1,
        explanation = "Die aufkommende Sozialdemokratische Partei (SPD) wurde für Bismarck zur größeren Bedrohung. Um die Antisozialistengesetze von 1878 durch den Reichstag zu bringen, brauchte er das Zentrum. Hinzu kamen der Tod Pius IX. (1878) und der pragmatischere Leo XIII. sowie ein päpstliches Entgegenkommen. Bismarck beendete schrittweise den Kulturkampf, wobei viele Gesetze erst Jahre später aufgehoben wurden.",
        difficulty = 4,
        funFact = "Bismarck hatte den Kulturkampf selbst maßgeblich eskaliert, bezeichnete ihn am Ende aber als Fehler. Trotzdem behauptete er, er habe 'nie nach Canossa gegangen' – eine Anspielung auf Heinrichs IV. Gang nach Canossa und ein Zeichen seines politischen Stolzes."
    ),

    // --- Zabern-Affäre ---

    Question(
        categoryId = 3,
        questionText = "Was war der Kern des Zaberner Zwischenfalls (1913) und welche verfassungsrechtliche Frage stellte er?",
        answerA = "Ein preußischer Leutnant beleidigte und misshandelte elsässische Zivilisten; der Reichstag missbilligte Reichskanzler Bethmann Hollwegs Verteidigung des Militärs, das blieb aber folgenlos",
        answerB = "Ein Sabotageakt auf ein deutsches Militärdepot durch elsässische Nationalisten",
        answerC = "Ein Attentat auf den deutschen Statthalter im Elsass durch französische Agenten",
        answerD = "Ein Aufstand elsässischer Rekruten gegen preußische Offiziere",
        correctAnswer = 0,
        explanation = "Leutnant Günter Freiherr von Forstner beleidigte elsässische Rekruten als 'Wackes' (Schimpfwort) und versprach demjenigen, der einen Franzosen niederstäche, zehn Mark. Als Zivilisten protestierten, wurden sie von Soldaten verhaftet – ohne rechtliche Befugnis. Der Reichstag verurteilte Bethmann Hollwegs Verteidigung dieser Willkür mit überwältigender Mehrheit, aber der Kanzler musste nicht zurücktreten: Im Kaiserreich war die Regierung nicht dem Parlament verantwortlich.",
        difficulty = 4,
        funFact = "Die Zaberner Affäre enthüllte den fundamentalen Verfassungskonflikt des Kaiserreichs: Das Militär war faktisch dem Kaiser, nicht dem Reichstag verantwortlich. Leutnant Forstner wurde zunächst verurteilt, das Urteil dann aufgehoben. Die Affäre verstärkte elsässischen Separatismus und verschärfte den Antagonismus zwischen Militär und ziviler Gesellschaft."
    ),

    // --- Daily Telegraph Affair ---

    Question(
        categoryId = 3,
        questionText = "Was enthüllte das Daily Telegraph-Interview mit Kaiser Wilhelm II. (1908) und welche Folgen hatte es?",
        answerA = "Wilhelm äußerte, Deutschland strebe Weltmacht an; sein Kanzler musste zurücktreten",
        answerB = "Wilhelm machte diplomatisch unkluge Äußerungen über Deutschland als Englands Freund, über Deutschlands angebliche Loyalität zu England im Burenkrieg und beleidigte Frankreich und Russland; der Reichstag kritisierte ihn scharf, aber er behielt die Macht",
        answerC = "Wilhelm gestand heimliche Verhandlungen mit Frankreich über Marokko ein",
        answerD = "Wilhelm drohte mit einem Präventivkrieg gegen Russland und Frankreich",
        correctAnswer = 1,
        explanation = "Wilhelms Interview im Daily Telegraph (Oktober 1908) enthielt mehrere Fauxpas: Er behauptete, die Mehrheit der Deutschen sei frankreichfeindlich, nur er (Wilhelm) sei Englands Freund; der deutschen Marine gegenüber Japan geholfen zu haben; und einen Schlachtplan gegen Russland entworfen zu haben. Reichstag und Presse kritisierten ihn massiv. Bülow distanzierte sich stillschweigend. Wilhelm versprach künftig mehr Zurückhaltung, hielt das nie wirklich ein.",
        difficulty = 4,
        funFact = "Das Interview war ursprünglich von einem britischen Adligen aufgezeichnet worden, der es dem Kaiser zur Genehmigung vorlegte. Wilhelms eigene Berater sollten es prüfen – taten es aber nicht gründlich. Es war ein klassisches Beispiel für den Dilettantismus der wilhelminischen Außenpolitik."
    ),

    // --- Weltpolitik ---

    Question(
        categoryId = 3,
        questionText = "Welcher Flottengesetz-Architekt entwickelte das Konzept des 'Risikogedankens' als Kern der deutschen Flottenpolitik?",
        answerA = "Admiral Alfred von Tirpitz",
        answerB = "Kaiser Wilhelm II.",
        answerC = "Staatssekretär Bernhard von Bülow",
        answerD = "General Schlieffen",
        correctAnswer = 0,
        explanation = "Admiral Alfred von Tirpitz entwickelte den 'Risikogedanken' (Risikoflotte): Auch wenn Deutschland Großbritannien nicht besiegen könnte, sollte eine starke Flotte das Risiko für Britain so erhöhen, dass es keine Politik gegen Deutschland riskieren würde. Die deutschen Flottengesetze von 1898 und 1900 setzten diese Strategie um – mit dem unbeabsichtigten Effekt, Großbritannien in die Entente zu treiben.",
        difficulty = 4,
        funFact = "Tirpitz' Risikoflottenstrategie war ein strategisches Paradoxon: Sie sollte Großbritannien neutral halten, trieb es aber ins Gegenteil. Nach 1904 schloss Britain die Entente Cordiale mit Frankreich und 1907 die Entente mit Russland – genau das Einkreisungsszenario, das Deutschland vermeiden wollte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das Ziel der deutschen 'Marokko-Krisen' von 1905 und 1911?",
        answerA = "Deutschland wollte Marokko als Kolonie annektieren",
        answerB = "Deutschland wollte die Entente Cordiale testen und aufbrechen sowie eine eigene Machtposition im westlichen Mittelmeer gewinnen",
        answerC = "Deutschland wollte Frankreich wirtschaftlich aus Marokko verdrängen",
        answerD = "Deutschland wollte einen Militärhafen an der marokkanischen Atlantikküste",
        correctAnswer = 1,
        explanation = "In der Ersten Marokkokrise (1905–06, 'Tangerkrise') und der Zweiten (1911, 'Panthersprung') versuchte Deutschland, die Entente Cordiale zu sprengen und Frankreich zu demütigen. Beide Male scheiterte Deutschland: Die Algeciraskonferenz 1906 isolierte Deutschland diplomatisch, 1911 musste Deutschland mit dem Erwerb des 'Bec de Canard' (Teil des Kongo) eine beschämende Kompensation akzeptieren.",
        difficulty = 4,
        funFact = "Der 'Panthersprung' – das Entsenden des Kanonenboots SMS Panther nach Agadir 1911 – wurde zum Symbol deutschen Säbelrasselns. In Deutschland nannten Nationalisten das Ergebnis einen 'zweiten Olmütz', in Anspielung auf Preußens Demütigung 1850. Diese Stimmung trug zur Kriegsbereitschaft 1914 bei."
    ),

    // --- WWI: Plan XVII und Schlachten ---

    Question(
        categoryId = 3,
        questionText = "Was war der französische 'Plan XVII' und warum scheiterte er im August 1914?",
        answerA = "Frankreichs Angriffsplan durch das neutrale Belgien; scheiterte an deutschem Widerstand in Lüttich",
        answerB = "Frankreichs Offensivplan zur Rückeroberung von Elsass-Lothringen durch direkten Frontalangriff; scheiterte an deutschem Abwehrfeuer und überlegener Feuerkraft in den Grenzschlachten",
        answerC = "Frankreichs Defensivplan zum Aufbau einer Maginot-ähnlichen Befestigungslinie",
        answerD = "Frankreichs Bündnisplan für koordinierte Angriffe mit Russland und Großbritannien",
        correctAnswer = 1,
        explanation = "Plan XVII unter General Joffre setzte auf massiven Frontalangriff durch Elsass-Lothringen, getragen vom Offensivgeist (l'offensive à outrance). In den Grenzschlachten (August 1914) prallten französische Infanteriekolonnen in roten Hosen gegen deutsches Maschinengewehr- und Artilleriefeuer. Frankreich verlor in den ersten Kriegswochen über 300.000 Mann. Der Plan ignorierte die Realität moderner Feuerkraft komplett.",
        difficulty = 4,
        funFact = "Frankreich erlitt in den ersten drei Monaten des Krieges mehr Verluste als in den gesamten folgenden vier Kriegsjahren zusammen – proportional gesehen. Der 22. August 1914 gilt als der blutigste Einzeltag in der Geschichte der französischen Armee mit über 27.000 Gefallenen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche taktische Innovation ermöglichte Ludendorffs Sieg bei Tannenberg (August 1914)?",
        answerA = "Massenangriff von Panzern auf russische Infanteriestellungen",
        answerB = "Nutzung abgehörter unverschlüsselter russischer Funkmeldungen und Umfassung beider russischer Flanken",
        answerC = "Gasangriff auf die russischen Stellungen kombiniert mit Artilleriebeschuss",
        answerD = "Überraschender Nachtangriff auf schlafende russische Truppen",
        correctAnswer = 1,
        explanation = "Die Deutschen konnten die russischen Funkmeldungen der 1. und 2. Armee abhören, da die Russen sie unverschlüsselt sendeten. Diese Intelligence ermöglichte Ludendorff und Hindenburg, die 8. Armee von der 1. Armee wegzuziehen und beide Flanken der 2. Armee (Samsonow) gleichzeitig einzukesseln. Rennenkampffs 1. Armee unternahm keine Rettungsversuche.",
        difficulty = 4,
        funFact = "General Samsonow erschoss sich nach der Niederlage in einem Wald. Von seinen 150.000 Mann wurden 92.000 gefangen genommen, 30.000 getötet. Die Deutschen verloren nur 12.000 Mann. Tannenberg wurde zur Gründungslegende des Hindenburg-Mythos, obwohl Erich Ludendorff die eigentliche operative Planung leistete."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches war die strategische Absicht des deutschen Generals Falkenhayn bei der Planung der Verdun-Offensive 1916?",
        answerA = "Durchbruch durch die französischen Linien und Einnahme von Paris",
        answerB = "Frankreich 'ausbluten' lassen (Attritionsstrategie): Durch Angriff auf symbolisch wichtigen Ort Frankreich zu massiven Gegenangriffen zwingen und in einem Abnutzungskrieg zermürben",
        answerC = "Ablenkungsoffensive, um britische Reserven von der Somme wegzuziehen",
        answerD = "Einnahme strategisch wichtiger Eisenbahnknotenpunkte im Maas-Tal",
        correctAnswer = 1,
        explanation = "Falkenhayn beschrieb sein Ziel (laut einem Memorandum, dessen Authentizität historisch umstritten ist): Er wollte Frankreich an eine Stelle zwingen, die es nicht aufgeben kann (Verdun/Maas-Höhen), und dort durch massive Artillerie die französische Armee 'ausbluten' lassen. Die Ironie: Deutschland verlor bei Verdun ähnlich viele Männer wie Frankreich – ca. 300.000 Tote auf jeder Seite.",
        difficulty = 4,
        funFact = "Falkenhayns angebliches 'Ausblute-Memorandum' wurde erst nach dem Krieg in seinen Memoiren zitiert und ist möglicherweise eine nachträgliche Rationalisierung. Historiker streiten, ob Verdun wirklich von Anfang an als Attritionsschlacht geplant war oder ob dies eine spätere Rechtfertigung darstellt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Instrument der deutschen Artillerie bei Verdun erlangte aufgrund seiner verheerenden Wirkung den Spitznamen 'Dicke Berta'?",
        answerA = "Krupp 42-cm-Mörser M-Gerät (Gamma-Gerät)",
        answerB = "Krupp 38-cm-Langer Max Eisenbahngeschütz",
        answerC = "Rheinmetall 21-cm-Mörser 16",
        answerD = "Skoda 30,5-cm-Mörser M 11",
        correctAnswer = 0,
        explanation = "Das M-Gerät (auch 'Gamma-Gerät' oder 'Dicke Bertha' – nach Bertha Krupp von Bohlen und Halbach) war ein 42-cm-Mörser, der ursprünglich zur Zerstörung belgischer Festungen (Lüttich, Namur) entwickelt wurde. Er verschoss 930 kg schwere Granaten. Bei Verdun wurden schwerere und mobilere Varianten eingesetzt. Der Begriff 'Dicke Bertha' wurde populär für alle schweren deutschen Geschütze.",
        difficulty = 4,
        funFact = "Das M-Gerät konnte nur 8 Schuss pro Stunde abfeuern, musste nach jedem Schuss manuell neu ausgerichtet werden und brauchte 200 Männer zur Bedienung. Es zerstörte die belgischen Betonbunker in wenigen Stunden – eine Schockwirkung, die die strategischen Planungen der Entente erschütterte."
    ),

    // --- Skagerrakschlacht / Jutland ---

    Question(
        categoryId = 3,
        questionText = "Was war das strategische Ergebnis der Skagerrakschlacht / Battle of Jutland (Mai/Juni 1916)?",
        answerA = "Deutschland gewann die Seeschlacht und brach die britische Seeblockade",
        answerB = "Großbritannien erlitt höhere Verluste, behielt aber die strategische Überlegenheit; die deutsche Hochseeflotte wagte nie wieder einen großen Vorstoß",
        answerC = "Unentschieden mit gleichen Verlusten auf beiden Seiten; beide Flotten blieben operationell gelähmt",
        answerD = "Großbritannien gewann entscheidend und vernichtete zwei Drittel der deutschen Flotte",
        correctAnswer = 1,
        explanation = "Jutland war eine taktische Niederlage Großbritanniens (mehr Verluste: 14 Schiffe, 6.094 Tote gegen deutsche 11 Schiffe, 2.551 Tote) aber ein strategischer britischer Sieg. Die Hochseeflotte floh zweimal durch kühne Wendemanöver ('Gefechtskehrtwendung'). Danach unternahm sie keine nennenswerten Operationen mehr – Großbritannien behielt die Nordseekontrolle und die Blockade Deutschlands.",
        difficulty = 4,
        funFact = "Nach der Skagerrakschlacht quittierte der amerikanische Witz: 'Der deutsche Gefangene hat versucht zu entwischen, aber der Bewacher ist ihm entkommen.' Die deutsche Presse feierte einen Sieg, aber die Flotte lag danach im Hafen – was die britische Blockade unvermindert aufrechthielt."
    ),

    Question(
        categoryId = 3,
        questionText = "Mit welchem Manöver entkam Scheer der britischen Umfassung bei Jutland zweimal?",
        answerA = "Gefechtskehrtwendung (simultane 180°-Wende aller Schlachtschiffe)",
        answerB = "Torpedoboots-Massenangriff kombiniert mit Nebelwand",
        answerC = "Rückzug hinter Minenfelder in Richtung Helgoland",
        answerD = "Ausweichen in flaches Küstengewässer, wo britische Großkampfschiffe nicht folgen konnten",
        correctAnswer = 0,
        explanation = "Admiral Scheer befahl zweimal die 'Gefechtskehrtwendung' (Gefechtskehrt) – ein hochkomplexes Manöver, bei dem alle Schlachtschiffe simultan mit entgegengesetzten Kursen abdrehten, wobei die Heckschiffe das Feuer auf sich zogen. Dieses Manöver war intensiv geübt und rettete die Flotte vor der Umfassung durch Jellicoes stärkere Grand Fleet.",
        difficulty = 4,
        funFact = "Die Gefechtskehrtwendung galt als so schwierig, dass Marinestrategen bezweifelten, sie lasse sich unter Feuer ausführen. Scheer bewies, dass es möglich war – was sein Prestige enorm steigerte. Ironischerweise führte das perfekte Ausweichen langfristig zum Ende der deutschen Flotte als Machtfaktor."
    ),

    // --- Zimmermann-Telegramm ---

    Question(
        categoryId = 3,
        questionText = "Was genau schlug das Zimmermann-Telegramm vom Januar 1917 vor?",
        answerA = "Mexiko solle Deutschland mit Öl und Rohstoffen versorgen im Austausch gegen technische Hilfe",
        answerB = "Mexiko solle im Kriegsfall gegen die USA eintreten und dafür Texas, New Mexico und Arizona zurückerhalten; Japan solle ebenfalls zum Kriegseintritt gegen die USA eingeladen werden",
        answerC = "Mexiko solle als neutrales Vermittlerland zwischen Deutschland und den USA Friedensverhandlungen einleiten",
        answerD = "Deutschland wollte über Mexiko geheime Unterstützung für irische Unabhängigkeitsbewegungen leisten",
        correctAnswer = 1,
        explanation = "Außenstaatssekretär Arthur Zimmermann schickte am 19. Januar 1917 eine verschlüsselte Depesche an den deutschen Botschafter in Mexiko. Er sollte Mexiko ein Bündnis anbieten: Bei Kriegseintritt gegen die USA würde Deutschland Mexiko helfen, Texas, New Mexico und Arizona zurückzuerobern, und Japan sollte auch zum Kriegseintritt eingeladen werden. Die Depesche wurde von britischen Codeknackern (Room 40) entschlüsselt.",
        difficulty = 4,
        funFact = "Zimmermann machte den Fehler, das Telegramm zu gestehen, nachdem Britain es veröffentlicht hatte – anstatt es als Fälschung zu bezeichnen. Sein Geständnis machte das Telegramm zur tödlichen politischen Waffe für die US-Neutralität und trieb den US-Kriegseintritt im April 1917 maßgeblich mit voran."
    ),

    Question(
        categoryId = 3,
        questionText = "Über welchen Kommunikationsweg gelangte das Zimmermann-Telegramm nach Mexiko und wie entdeckte Britain es?",
        answerA = "Es wurde über transatlantische Unterseekabel übertragen; Britain hatte Zugang zum deutschen Kabel über neutrale Länder und der Raum 40 entschlüsselte es",
        answerB = "Es wurde per Kurierschiff transportiert und von britischen Agenten in Havanna abgefangen",
        answerC = "Es wurde über amerikanische Unterseekabel gesendet (mit US-Erlaubnis) und von der NSA-Vorläuferin entschlüsselt",
        answerD = "Es wurde per Funk gesendet und von britischen Abhörstationen in Irland abgefangen",
        correctAnswer = 0,
        explanation = "Deutschland nutzte den transatlantischen Unterseekabel-Dienst, den Wilson Deutschland als Konzession für neutrale Nutzung erlaubt hatte (sowie das schwedische Kabel), um das Telegramm zu übertragen. Room 40, die britische Codebrecher-Einheit, hatte das Telegramm abgefangen und entschlüsselt. Britain gab es an die USA weiter, ohne die Kompromittierung der deutschen Codes zu enthüllen – eine meisterhafte Geheimdienstoperation.",
        difficulty = 4,
        funFact = "Room 40 hatte das Telegramm schon Wochen abgefangen, bevor es der US-Regierung übergeben wurde. Die Briten mussten die Übergabe so gestalten, dass Deutschland nicht merkte, dass seine Codes gebrochen waren. Sie inszenierten eine Geschichte, wonach das Telegramm von einem Agenten in Mexiko gestohlen worden sei."
    ),

    // --- Brest-Litowsk ---

    Question(
        categoryId = 3,
        questionText = "Welche Gebietsverluste musste Sowjetrussland im Vertrag von Brest-Litowsk (März 1918) akzeptieren?",
        answerA = "Nur das Baltikum und Finnland",
        answerB = "Etwa ein Drittel des europäischen Russlands: Polen, Litauen, Lettland, Estland, Finnland, Ukraine und Kaukasusgebiete – insgesamt 34% der Bevölkerung, 54% der Industrie und 89% der Kohlereserven",
        answerC = "Die Ukraine, Belarus und Zentralasien",
        answerD = "Sibirien bis zum Ural und alle Ostseeprovinzen",
        correctAnswer = 1,
        explanation = "Der Vertrag von Brest-Litowsk war für Russland vernichtend: Es verlor Finnland, Estland, Lettland, Litauen, Polen, die Ukraine und Teile des Kaukasus (Kars, Ardahan, Batum). Diese Gebiete umfassten ca. 34% der Bevölkerung, 54% der Industriekapazität und 89% der Kohlereserven des ehemaligen Zarenreichs. Lenin nannte ihn einen 'obszönen Frieden', akzeptierte ihn aber als einzig möglichen Weg zum Überleben des Bolschewismus.",
        difficulty = 4,
        funFact = "Trotzki versuchte bei den Verhandlungen eine bizarre Taktik: 'Weder Krieg noch Frieden' – keine Unterschrift, aber auch keine Fortsetzung des Kampfes. Als Deutschland daraufhin einfach weitermarschiette, musste Lenin den noch härteren Frieden akzeptieren. Der Vertrag wurde im November 1918 nach Deutschlands Niederlage annulliert."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Position vertrat die Bolschewistische Partei intern bezüglich des Brest-Litowsker Friedens?",
        answerA = "Einigkeit: Alle Fraktionen unterstützten den Frieden",
        answerB = "Tiefer Fraktionsstreit: Lenin für sofortigen Frieden; Trotzki für 'weder Krieg noch Frieden'; Bucharin und 'Linke Kommunisten' für Revolutionskrieg",
        answerC = "Die Partei wollte den Krieg fortsetzen, wurde aber von der Armee überstimmt",
        answerD = "Stalin und Swerdlow unterstützten den Frieden, Lenin und Trotzki waren dagegen",
        correctAnswer = 1,
        explanation = "Der Brest-Litowsk-Frieden spaltete die Bolschewiki: Lenin bestand auf sofortigem Frieden, um das Regime zu retten ('atmen'). Trotzki wollte 'weder Krieg noch Frieden' in der Hoffnung auf deutsche Revolution. Bucharin und die 'Linken Kommunisten' wollten einen revolutionären Guerillakrieg weiterführen, der europäische Proletarier inspirieren sollte. Lenin setzte sich durch, aber nur knapp.",
        difficulty = 4,
        funFact = "Bucharin argumentierte, ein Revolutionskrieg, auch wenn Russland ihn verlöre, würde die europäische Arbeiterklasse zum Aufstand anstacheln. Lenin entgegnete trocken: 'Es gibt keine europäische Revolution, aber es gibt den Frieden von Brest-Litowsk.' Bucharins romantischer Revolutionismus scheiterte an Lenins nüchternem Pragmatismus."
    ),

    // --- Weitere WWI-Details ---

    Question(
        categoryId = 3,
        questionText = "Was war die 'Brusilow-Offensive' (1916) und welche Bedeutung hatte sie für die Kriegsgeschichte?",
        answerA = "Eine russische Offensive, die durch Überraschung und dezentralisierte Angriffspunkte ca. 400.000 österreichische Gefangene machte und als Musterbeispiel moderner Operationsführung gilt",
        answerB = "Eine russische Winteroffensive, die durch Kälte und schlechte Versorgung scheiterte",
        answerC = "Die letzte russische Offensive vor dem Zusammenbruch der Zarenarmee 1917",
        answerD = "Eine koordinierte russisch-rumänische Operation zur Einnahme von Budapest",
        correctAnswer = 0,
        explanation = "General Brussilow ersann ein innovatives Angriffskonzept: Statt eines konzentrierten Angriffs an einer Stelle wurden gleichzeitig viele Einbruchspunkte entlang einer 500 km langen Front angesetzt – so konnte der Feind keine Reserven konzentrieren. Die Offensive (Juni–September 1916) machte ca. 400.000 österreichisch-ungarische Gefangene, brachte Rumänien in den Krieg und entlastete Verdun und die Somme. Sie gilt als erste moderne Durchbruchsoperation.",
        difficulty = 4,
        funFact = "Die Brussilow-Offensive rettete möglicherweise Frankreich bei Verdun, indem sie deutsche Reserven nach Osten abzog. Sie kostete aber auch Russland ca. 500.000 Verluste und beschleunigte den Zerfall der Zarenarmee. Brussilow selbst trat später in die Rote Armee ein – einer der wenigen zaristischen Generäle, der das tat."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Bedeutung der 'Uneingeschränkten U-Boot-Kriegsführung', die Deutschland im Februar 1917 wiederaufnahm?",
        answerA = "Deutschland wollte die britische Seeblockade durchbrechen",
        answerB = "Deutschland kalkulierte, Großbritannien durch Versenkung aller Schiffe im Kriegsgebiet binnen fünf Monaten zum Frieden zu zwingen, auch wenn der US-Kriegseintritt dadurch riskiert wurde",
        answerC = "Deutschland reagierte auf britische Minenfelder in der Nordsee",
        answerD = "Deutschland wollte amerikanische Waffenlieferungen an Frankreich unterbinden",
        correctAnswer = 1,
        explanation = "Die deutsche Admiralität unter Holtzendorff errechnete, dass uneingeschränkte U-Boot-Kriegsführung Großbritannien besiegen würde, bevor die USA effektiv eingreifen könnten (5 Monate). Diese Kalkulation erwies sich als fatal falsch: Der US-Kriegseintritt im April 1917 und das britische Konvoi-System (eingeführt Mai 1917) neutralisierten den U-Boot-Krieg, während amerikanische Truppen den Unterschied machten.",
        difficulty = 4,
        funFact = "Holtzendorffs Memorandum vom Dezember 1916 enthielt den berühmten Satz: 'Ich garantiere Eurer Majestät, dass der uneingeschränkte U-Boot-Krieg, so früh begonnen wie möglich, England innerhalb von fünf Monaten zum Frieden zwingt.' Es war eine der folgenschwersten Fehleinschätzungen des Krieges."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter dem 'Schlieffen-Plan' und welchem strategischen Dilemma sollte er begegnen?",
        answerA = "Deutschlands Zweifrontenkriegs-Dilemma: Schneller Sieg im Westen (durch Belgien, dann Paris) vor Russlands Mobilisierung, dann Verlagerung nach Osten",
        answerB = "Österreich-Ungarns Plan zur schnellen Niederwerfung Serbiens",
        answerC = "Deutschlands Flottenplan für einen präventiven Angriff auf die britische Grand Fleet",
        answerD = "Preußisches Defensivkonzept zur Abwehr eines simultanen russisch-französischen Angriffs",
        correctAnswer = 0,
        explanation = "Graf von Schlieffen entwickelte bis 1905 einen Plan, der Deutschlands strategisches Dilemma lösen sollte: Zweifrontenkrieg gegen Frankreich und Russland gleichzeitig. Idee: Russland braucht 6–8 Wochen zur Mobilisierung; in dieser Zeit schlägt Deutschland Frankreich durch einen gewaltigen Bogen durch Belgien/Nordfrankreich. Danach werden Truppen per Eisenbahn nach Osten verlegt. Moltkef veränderte den Plan 1914 – zu Ungunsten des rechten Flügels.",
        difficulty = 4,
        funFact = "Schlieffens berühmter letzter Wunsch war angeblich: 'Stärkt den rechten Flügel!' Moltke schwächte genau diesen, weil er Ostpreußen nicht ausreichend gesichert sah. Historiker streiten, ob Schlieffens Original jemals realisierbar gewesen wäre – Belgiens Festungen und die Breite des Schwenkbogens hätten logistische Probleme geschaffen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte die erste Marneschlacht (September 1914) für den Kriegsverlauf?",
        answerA = "Sie verhinderte den deutschen Durchmarsch nach Paris und zwang zum Rückzug hinter die Aisne – damit scheiterte der Schlieffen-Plan und begann der Stellungskrieg",
        answerB = "Sie führte zur Einkreisung der deutschen Truppen westlich von Paris",
        answerC = "Sie ermöglichte Frankreich die Rückeroberung von Elsass-Lothringen",
        answerD = "Sie zwang Großbritannien zur Entsendung von 500.000 Zusatztruppen nach Frankreich",
        correctAnswer = 0,
        explanation = "An der Marne (5.–12. September 1914) hielten die Alliierten (Joffres 'Marnewunder'): Der 'Marnespalt' zwischen der deutschen 1. und 2. Armee wurde von britischen Truppen genutzt. Moltke befahl den Rückzug hinter die Aisne. Damit scheiterte der Schlieffen-Plan, Deutschland hatte kein schnelles Ergebnis im Westen erzielt, und der jahrelange Stellungskrieg begann.",
        difficulty = 4,
        funFact = "Die Pariser Taxifahrer ('Taxis de la Marne') wurden legendär: Sie transportierten angeblich 6.000 Soldaten zur Front. Historisch beförderten die Taxis tatsächlich ca. 6.000 Reservisten – ein kleiner, aber symbolisch wichtiger Beitrag. Der Mythos übertrieb die militärische Bedeutung massiv."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Septemberprogramm' der deutschen Regierung von 1914?",
        answerA = "Ein geheimes Kriegszieldokument unter Bethmann Hollweg, das weitreichende Annexionen in West und Ost sowie deutsche Hegemonie über Europa vorsah",
        answerB = "Deutschlands Mobilisierungsplan für September 1914",
        answerC = "Ein Friedensangebot an Russland nach den ersten Kriegswochen",
        answerD = "Das deutsche Bündnisangebot an das Osmanische Reich im September 1914",
        correctAnswer = 0,
        explanation = "Das Septemberprogramm (9. September 1914), vom Kanzleramtschef Kurt Riezler verfasst und von Bethmann Hollweg gebilligt, skizzierte weitreichende Kriegsziele: Annexion von Teilen Frankreichs (Erzbecken Longwy-Briey), wirtschaftliche Unterwerfung Belgiens, Mittelafrika als Kolonialreich, Ost-Annexionen von Russland, 'Mitteleuropäischer Wirtschaftsverband' unter deutscher Führung.",
        difficulty = 4,
        funFact = "Das Septemberprogramm war geheim und wurde erst nach dem Krieg entdeckt. Als der Historiker Fritz Fischer es 1961 in seinem Buch 'Griff nach der Weltmacht' veröffentlichte, löste er in Deutschland einen Historikerstreit aus: War Deutschland für den Ersten Weltkrieg hauptverantwortlich? Die 'Fischer-Kontroverse' veränderte die deutsche Geschichtsschreibung fundamental."
    ),

    Question(
        categoryId = 3,
        questionText = "Was waren die Hauptbestimmungen des 'Waffenstillstands von Compiègne' (November 1918)?",
        answerA = "Sofortiger Waffenstillstand; Deutschland muss alle besetzten Gebiete räumen, linkes Rheinufer und Brückenköpfe abtreten, Flotte ausliefern und 5.000 Geschütze und Lokomotiven abgeben",
        answerB = "Sofortiger Waffenstillstand; Deutschland muss nur die besetzten Westgebiete räumen",
        answerC = "Vollständige Kapitulation; Kaiser Wilhelm muss in alliierte Gefangenschaft",
        answerD = "Waffenstillstand auf 30 Tage; endgültige Bedingungen im Friedensvertrag",
        correctAnswer = 0,
        explanation = "Der Waffenstillstand vom 11. November 1918 enthielt äußerst harte Bedingungen: sofortiger Abzug aus besetzten Gebieten und dem linken Rheinufer (mit Brückenköpfen); Auslieferung von 5.000 Geschützen, 25.000 Maschinengewehren, 3.000 Minenwerfer, 1.700 Flugzeugen, 5.000 Lokomotiven; Internierung der Flotte in Scapa Flow; Fortsetzung der Seeblockade Deutschlands.",
        difficulty = 4,
        funFact = "Die Seeblockade, die Hunderttausende Deutsche durch Unterernährung das Leben kosten sollte, wurde nach dem Waffenstillstand noch Monate weitergeführt. Sie diente als Druckmittel für den Versailler Vertrag. Diese Tatsache prägte die deutsche 'Dolchstoßlegende' – das Gefühl, unbesiegt, aber im Rücken verraten worden zu sein."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Haltung nahm die Sozialdemokratische Partei Deutschlands (SPD) am 4. August 1914 zu den Kriegskrediten ein und warum?",
        answerA = "Die SPD verweigerte die Kriegskredite und blieb ihrer Antikriegs-Haltung treu",
        answerB = "Die SPD-Fraktion stimmte einstimmig für die Kriegskredite im Zeichen des 'Burgfriedens' – Klassensolidarität wurde durch Vaterlandsverteidigung überlagert",
        answerC = "Die SPD stimmte gespalten: 40% für, 60% gegen die Kredite",
        answerD = "Die SPD enthielt sich der Stimme als Kompromiss",
        correctAnswer = 1,
        explanation = "Am 4. August 1914 stimmte die SPD-Reichstagsfraktion einstimmig für die Kriegskredite – ein historischer Bruch mit der internationalistischen Antikriegs-Haltung der Zweiten Internationale. Die SPD glaubte an einen Verteidigungskrieg gegen das zaristische Russland ('das Schlimmste'). Kaiser Wilhelm II. erklärte berühmt: 'Ich kenne keine Parteien mehr, ich kenne nur noch Deutsche.'",
        difficulty = 4,
        funFact = "In einer geheimen Abstimmung vor der öffentlichen Abstimmung hatte die SPD-Fraktion 78:14 für die Kredite gestimmt. Die 14 Gegner (darunter Karl Liebknecht) fügten sich dem Fraktionszwang. Liebknecht stimmte im Dezember 1914 als Erster öffentlich gegen weitere Kriegskredite – ein Akt, der ihn zur Ikone der deutschen Antikriegsbewegung machte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Burgfrieden' ('Union Sacrée' in Frankreich) im Kontext des Ersten Weltkriegs?",
        answerA = "Ein Verteidigungsbündnis zwischen Deutschland und Österreich-Ungarn für die Dauer des Krieges",
        answerB = "Ein innenpolitischer Waffenstillstand: Alle Parteien (einschließlich Sozialisten) verzichteten auf Streiks und innenpolitische Opposition für die Dauer des Krieges",
        answerC = "Ein Abkommen zwischen Deutschland und der Schweiz zur Sicherung schweizerischer Neutralität",
        answerD = "Ein Protokoll des Deutschen Reiches zur Aussetzung des Wahlrechts für die Kriegsdauer",
        correctAnswer = 1,
        explanation = "Der 'Burgfrieden' (Deutschland) und die 'Union Sacrée' (Frankreich) bezeichneten den innenpolitischen Waffenstillstand bei Kriegsausbruch 1914: Sozialisten, Gewerkschaften und bürgerliche Parteien einigten sich, keine Streiks, keine innenpolitischen Forderungen und keine Opposition für die Kriegsdauer zu betreiben. Dies gab Regierungen freie Hand – zerfiel aber ab 1916/17 unter dem Druck von Verlusten und Hunger.",
        difficulty = 4,
        funFact = "Der Burgfrieden-Gedanke hinterließ tiefe Narben in der deutschen Arbeiterbewegung. Die SPD-Mehrheit (MSPD) hielt daran fest; eine Minderheit gründete 1917 die USPD. Aus der Antikriegslinken entstanden 1919 die Kommunistische Partei Deutschlands (KPD) – eine direkte Folge des Bruchs über die Kriegskredit-Frage von 1914."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Abkommen von 1916 teilte den Nahen Osten zwischen Großbritannien und Frankreich auf und wurde später als Verrat an arabischen Unabhängigkeitsversprechen kritisiert?",
        answerA = "Sykes-Picot-Abkommen",
        answerB = "Hussein-McMahon-Korrespondenz",
        answerC = "Balfour-Deklaration",
        answerD = "Lausanner Vertrag",
        correctAnswer = 0,
        explanation = "Das geheime Sykes-Picot-Abkommen (Mai 1916) zwischen dem Briten Mark Sykes und dem Franzosen François Georges-Picot teilte den arabischen Nahen Osten in britische und französische Einflusssphären. Frankreich bekam Syrien und den Libanon, Großbritannien den Irak und Palästina (international kontrolliert). Es widersprach den britischen Versprechen an die Araber (Hussein-McMahon) und der späteren Balfour-Deklaration.",
        difficulty = 4,
        funFact = "Das Sykes-Picot-Abkommen wurde von den Bolschewiki 1917 in russischen Staatsarchiven gefunden und veröffentlicht – eine diplomatische Bombe, die arabische Nationalisten empörte. Die heutigen Grenzen des Nahen Ostens gehen im Wesentlichen auf dieses Abkommen zurück und gelten als Ursache vieler moderner Konflikte in der Region."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Zimmermann-Note' im Kontext des deutschen Kriegseintritts-Kalküls für Mexiko?",
        answerA = "Eine Note, die Mexiko zu freier Wahl seiner Neutralitätspolitik einlud",
        answerB = "Eine diplomatische Note, die Mexiko ein formelles Militärbündnis anbot mit dem Versprechen finanzieller Unterstützung und territorialer Rückgewinnung von Texas, New Mexico und Arizona",
        answerC = "Eine Note, die Mexiko warnte, keine amerikanischen Truppen durch sein Territorium zu lassen",
        answerD = "Eine Note, in der Deutschland Mexiko für seine Neutralität dankte",
        correctAnswer = 1,
        explanation = "Das Zimmermann-Telegramm (auch Note genannt) bot Mexiko ein formelles Militärbündnis an: Im Gegenzug für einen Kriegseintritt gegen die USA würde Deutschland Mexiko finanziell unterstützen und beim Rückerwerb von 'verlorenem Territorium' helfen – gemeint waren Texas, New Mexico und Arizona, die Mexiko 1848 im Guadalupe Hidalgo-Vertrag abtreten musste.",
        difficulty = 4,
        funFact = "Der mexikanische Präsident Carranza ließ das Angebot tatsächlich von seinem Militär prüfen. Sein Generalstab riet ab: Mexiko sei zu schwach, die USA zu stark, und ein Krieg würde weitere Gebietsverluste riskieren statt alte zurückzugewinnen. Carranza lehnte ab – aber die bloße Prüfung zeigt, dass die Note ernst genommen wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Rolle spielte der 'Rat der Vier' bei den Pariser Friedensverhandlungen 1919?",
        answerA = "Ein Ausschuss aus Militärstrategegen der vier Alliierten, der die Demilitarisierung Deutschlands plante",
        answerB = "Die eigentliche Entscheidungsinstanz der Pariser Friedenskonferenz: Wilson (USA), Lloyd George (GB), Clemenceau (Frankreich), Orlando (Italien) – verhandelte die Kernfragen des Versailler Vertrags",
        answerC = "Ein Völkerbundgremium zur Verwaltung der ehemaligen deutschen Kolonien",
        answerD = "Eine Koordinierungsgruppe der vier Besatzungsmächte im Rheinland",
        correctAnswer = 1,
        explanation = "Der Rat der Vier (auch 'Großer Rat' genannt) war das eigentliche Machtzentrum der Pariser Friedenskonferenz von 1919. Woodrow Wilson (USA), David Lloyd George (Großbritannien), Georges Clemenceau (Frankreich) und Vittorio Orlando (Italien) trafen die grundlegenden Entscheidungen über den Versailler Vertrag, Gebietsabtretungen, Reparationen und den Völkerbund hinter verschlossenen Türen.",
        difficulty = 4,
        funFact = "Orlando verließ die Konferenz zeitweilig aus Protest, weil Italien seine Gebietsversprechen aus dem Londoner Vertrag von 1915 nicht vollständig erfüllt bekam. In Italien wurde dies als 'verstümmelter Sieg' (vittoria mutilata) bezeichnet – ein Trauma, das den Aufstieg Mussolinis begünstigte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Artikel des Versailler Vertrags bildete die rechtliche Grundlage für die deutschen Reparationszahlungen?",
        answerA = "Artikel 231 (Kriegsschuldklausel – 'War Guilt Clause')",
        answerB = "Artikel 48 (Notstandsvollmachten des Reichspräsidenten)",
        answerC = "Artikel 160 (Begrenzung des deutschen Heeres auf 100.000 Mann)",
        answerD = "Artikel 119 (Abtretung der Kolonien)",
        correctAnswer = 0,
        explanation = "Artikel 231 des Versailler Vertrags (Kriegsschuldklausel) erklärte Deutschland und seine Verbündeten für allein verantwortlich an Kriegsausbruch und -schäden. Diese Klausel bildete die rechtliche und moralische Grundlage für die Reparationsforderungen. In Deutschland wurde sie als Demütigung empfunden und von der gesamten politischen Rechten wie Linken abgelehnt.",
        difficulty = 4,
        funFact = "Die Höhe der Reparationen wurde 1921 auf 132 Milliarden Goldmark festgesetzt. Nach langen Verhandlungen, dem Dawes-Plan (1924) und dem Young-Plan (1929) wurde die Summe reduziert. Deutschland zahlte die letzte Rate tatsächlich erst am 3. Oktober 2010 – genau 20 Jahre nach der Wiedervereinigung."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Kieler Matrosenrevolte' (Oktober/November 1918) und welche Bedeutung hatte sie für das Ende des Deutschen Kaiserreichs?",
        answerA = "Ein Aufstand gegen schlechte Verpflegung; hatte keine politische Bedeutung",
        answerB = "Matrosen weigerten sich, einen aussichtslosen Flottenvorstoß gegen die Royal Navy durchzuführen; die Revolte weitete sich zu Revolution aus und bildete den Beginn der Novemberrevolution",
        answerC = "Ein gescheiterter Putschversuch monarchistischer Offiziere gegen die neue Volksregierung",
        answerD = "Ein Streik gegen Kriegsverlängerung; führte direkt zum Waffenstillstandsangebot Deutschlands",
        correctAnswer = 1,
        explanation = "Ende Oktober 1918 plante die Admiralität einen 'Ehrenkampf' der Hochseeflotte gegen die Royal Navy – trotz der laufenden Waffenstillstandsverhandlungen. Die Matrosen weigerten sich, in den sicheren Tod zu fahren. Die Revolte in Kiel (3. November) breitete sich über Deutschland aus. Soldaten- und Arbeiterräte entstanden. Wilhelm II. dankte ab, Scheidemann rief die Republik aus – die Novemberrevolution.",
        difficulty = 4,
        funFact = "Die Kieler Revolte begann in der Nacht vom 29. auf den 30. Oktober, als Matrosen auf mehreren Schlachtschiffen die Dampfkessel löschten und so den Auslaufbefehl sabotierten. Admiral Scheer bezeichnete dies als 'den Todesstoß unserer Marine'. Das Flottenkommando hatte nicht erkannt, wie tief die Kriegsmüdigkeit nach vier Jahren saß."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter dem 'Gaskrieg' des Ersten Weltkriegs und welches Gas wurde am häufigsten eingesetzt?",
        answerA = "Chlorgas, erstmals von Deutschland bei Ypern 1915 eingesetzt; dann Phosgen (lethalster Wirkstoff) und Senfgas (Haupt-Gefechtsstoff 1917–1918)",
        answerB = "Nur Senfgas, das von Frankreich entwickelt und 1916 erstmals eingesetzt wurde",
        answerC = "Zyklon B, später im Holocaust verwendet, erstmals im Ersten Weltkrieg getestet",
        answerD = "Kohlenmonoxid in Tunnelgängen; oberirdisch wurden keine Gase verwendet",
        correctAnswer = 0,
        explanation = "Deutschland setzte am 22. April 1915 bei Ypern erstmals im großen Stil Chlorgas ein (168 Tonnen). Phosgen war für ca. 80–85% aller Gassterbefälle verantwortlich. Das berüchtigste Gas war Senfgas (Lost, mustard gas), das 1917 eingeführt wurde: Es war kaum tödlich, verursachte aber schwere Hautverletzungen und Erblindung und band feindliche Sanitätskapazitäten massiv.",
        difficulty = 4,
        funFact = "Fritz Haber, Nobelpreisträger für Chemie (Haber-Bosch-Verfahren für Kunstdünger), war der Hauptarchitekt des deutschen Gaskriegs. Seine Frau Clara, selbst Chemikerin, erschoss sich in Protest gegen seine Arbeit – am Tag nach seinem Chlorgas-Erstansatz bei Ypern. Haber floh 1933 vor den Nazis, sein Zyklon-B-Vorläuferverfahren wurde für die Holocaust-Gaskammern genutzt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Hindenburg-Programm' von 1916 und warum gilt es als Wendepunkt der deutschen Kriegswirtschaft?",
        answerA = "Ein Programm zur massiven Steigerung der Rüstungsproduktion durch totale Mobilisierung aller wirtschaftlichen Ressourcen und Zwangsarbeit",
        answerB = "Hindenburg­s Plan zur Verteidigung der Westfront durch ein tiefgestaffeltes Grabensystem",
        answerC = "Ein Ernährungsprogramm gegen die britische Seeblockade",
        answerD = "Hindenburg­s politisches Programm zur Machtübernahme der Militärführung",
        correctAnswer = 0,
        explanation = "Das Hindenburg-Programm (August 1916) unter Hindenburg und Ludendorff forderte extreme Steigerung der Munitions-, Waffen- und Infanterie-Ausrüstungsproduktion, begleitet vom Hilfsdienstgesetz (Dezember 1916), das alle Männer zwischen 17 und 60 zur Rüstungsarbeit verpflichtete. Es läutete die totale Mobilisierung der deutschen Kriegswirtschaft ein, führte aber auch zu enormen sozialen Spannungen.",
        difficulty = 4,
        funFact = "Das Hindenburg-Programm erreichte seine Produktionsziele nicht – die Wirtschaft war bereits überdehnt. Es führte zu massiver Inflation, Konsumgütermangel und dem 'Kohlrübenwinter' 1916/17, in dem Hunderttausende Deutsche an Hunger und Unterernährung litten. Es beschleunigte paradoxerweise Deutschlands wirtschaftlichen Zusammenbruch."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis führte 1914 direkt zur Kriegserklärung Österreich-Ungarns an Serbien und damit zum Beginn des Weltkriegs?",
        answerA = "Die Ermordung Erzherzog Franz Ferdinands in Sarajevo am 28. Juni 1914",
        answerB = "Der russische Einmarsch in Galizien",
        answerC = "Die Ablehnung des österreichischen Ultimatums durch Serbien",
        answerD = "Der Anschlag serbischer Truppen auf die Grenzfestung Zvornik",
        correctAnswer = 2,
        explanation = "Obwohl die Ermordung Franz Ferdinands (28. Juni) der Auslöser war, war das unmittelbare Kriegsauslöse-Ereignis Serbiens Antwort auf das österreichische Ultimatum (23. Juli): Serbien akzeptierte 9 von 10 Forderungen, lehnte aber Punkt 5 (österreichische Beteiligung an serbischer Justizuntersuchung) als Verletzung der Souveränität ab. Wien erklärte daraufhin am 28. Juli 1914 den Krieg.",
        difficulty = 4,
        funFact = "Serbiens Antwort war so entgegenkommend, dass Kaiser Franz Joseph II. und sein Außenminister zunächst erstaunt waren. Deutschland drängte Wien zur Kriegserklärung, obwohl die serbische Note als mögliche Verhandlungsgrundlage hätte dienen können. Dies stützt die These der deutschen 'Blankoscheck'-Politik."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Schlachtfeldrevolution' der letzten Kriegsmonate 1918 und welche taktische Innovation ermöglichte die deutschen Frühjahrsoffensiven?",
        answerA = "Masseneinsatz von Panzern mit neuartiger Kommunikationstechnik",
        answerB = "Die 'Sturmtruppen-Taktik' (Hutier-Taktik): Schnelle Stoßtrupps in feindliche Hinterlinien, Umgehung starker Widerstandsnester statt Frontalangriff",
        answerC = "Koordinierter Luft-Boden-Angriff mit Fokker-Flugzeugen als Schlachtunterstützung",
        answerD = "Massengas-Einsatz kombiniert mit sofortiger Infanterienachfolge",
        correctAnswer = 1,
        explanation = "Die 'Hutier-Taktik' (nach General Oskar von Hutier) war die taktische Revolution der Sturmtruppen: Kurze, intensive Artillerie-Feuerwalzen, dann schnelle Stoßtrupps, die schwachen Punkte der feindlichen Linie suchen und durchstoßen, Widerstandsnester für nachfolgende Truppen überlassen. Diese Taktik ermöglichte in der Frühjahrsoffensive 1918 die größten Geländegewinne seit 1914 – aber ohne den entscheidenden Durchbruch.",
        difficulty = 4,
        funFact = "Die Sturmtruppen-Taktik wurde nach dem Krieg von der Reichswehr systematisiert und bildete die Grundlage der 'Blitzkrieg'-Doktrin des Zweiten Weltkriegs. Ironischerweise hatten auch die Alliierten ähnliche Methoden entwickelt – die Deutschen nannten die koordinierten alliierten Angriffe von August 1918 den 'Schwarzen Tag' des deutschen Heeres."
    ),

    // --- Additional Expert Questions ---

    Question(
        categoryId = 3,
        questionText = "Was beschloss die Haager Landkriegsordnung (1899/1907) bezüglich der Behandlung von Kriegsgefangenen und wie wurde sie im Ersten Weltkrieg verletzt?",
        answerA = "Sie verbot Kriegsgefangenschaft grundsätzlich; alle gefangenen Soldaten sollten unmittelbar freigelassen werden",
        answerB = "Sie verpflichtete zur menschlichen Behandlung von Kriegsgefangenen und Verbot ihrer Zwangsarbeit in kriegswichtiger Produktion; alle Kriegsmächte verletzten diese Bestimmungen durch Einsatz von Gefangenen in Rüstungsbetrieben und Bergwerken",
        answerC = "Sie regelte ausschließlich den Einsatz von Schusswaffen und Artillerie",
        answerD = "Sie verbot chemische Waffen; wurde durch den deutschen Gaseinsatz 1915 erstmals gebrochen",
        correctAnswer = 1,
        explanation = "Die Haager Landkriegsordnung (HLKO) verpflichtete zur menschenwürdigen Behandlung von Kriegsgefangenen und verbot ihren Einsatz in kriegswichtiger Produktion. Im Ersten Weltkrieg setzten alle Kriegsmächte Gefangene in Bergwerken, in der Landwirtschaft und teilweise in Rüstungsbetrieben ein. Deutschland nutzte russische Gefangene systematisch als billige Arbeitskräfte; Großbritannien und Frankreich griffen auf deutsche Gefangene zurück.",
        difficulty = 4,
        funFact = "Über 8 Millionen Soldaten gerieten im Ersten Weltkrieg in Kriegsgefangenschaft. Die Sterblichkeitsrate variierte enorm: Während westliche Gefangene in deutschen Lagern oft verhältnismäßig gut behandelt wurden, starb von den ca. 2,4 Millionen deutschen Gefangenen in russischer Hand ein erheblicher Teil an Hunger und Seuchen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Konzert Europas' (Concert of Europe) und welches Prinzip bildete seine ideologische Grundlage nach 1815?",
        answerA = "Ein Militärbündnis der fünf Großmächte zur gemeinsamen Verteidigung gegen externe Angriffe",
        answerB = "Ein informelles System der fünf europäischen Großmächte (Österreich, Preußen, Russland, GB, Frankreich) zur kollektiven Regulierung europäischer Angelegenheiten, gegründet auf dem Prinzip des Mächtegleichgewichts und der Legitimität monarchischer Ordnung",
        answerC = "Eine Freihandelszone der europäischen Großmächte",
        answerD = "Ein formelles Schiedsgericht zur Beilegung europäischer Grenzstreitigkeiten",
        correctAnswer = 1,
        explanation = "Das Konzert Europas (1815–1914) war das erste moderne System kollektiver Sicherheit: Die fünf Großmächte koordinierten ihre Außenpolitik durch regelmäßige Konferenzen (Kongress-System), um Kriege zwischen Großmächten zu verhindern und revolutionäre Umstürze zu bekämpfen. Die ideologische Grundlage war Metternichs Konzept der 'Legitimität' (Erhaltung monarchischer Ordnung) und des Gleichgewichts ('Balance of Power').",
        difficulty = 4,
        funFact = "Das Konzert Europas war trotz aller Spannungen erstaunlich erfolgreich: Es verhinderte zwischen 1815 und 1914 einen allgemeinen europäischen Krieg für fast 100 Jahre. Metternich nannte es sein größtes politisches Werk. Es scheiterte letztlich nicht an fehlenden Institutionen, sondern am Aufstieg des Nationalismus und der deutschen Weltmachtpolitik."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches war der Inhalt des geheimen 'Rückversicherungsvertrags' (1887) zwischen Deutschland und Russland, und warum ließ Bismarckss Nachfolger ihn auslaufen?",
        answerA = "Russland garantierte Deutschland Neutralität bei einem deutsch-französischen Krieg; Deutschland garantierte Russland freie Hand auf dem Balkan – Caprivi ließ ihn auslaufen, da er dem Zweibund mit Österreich widersprach",
        answerB = "Deutschland und Russland vereinbarten gemeinsame Kriegsführung gegen Großbritannien",
        answerC = "Russland erhielt deutsche Wirtschaftshilfe; Deutschland erhielt russische Rohstoffe zu Vorzugskonditionen",
        answerD = "Beide Mächte vereinbarten keine Angriffskriege in Europa für die Dauer von zehn Jahren",
        correctAnswer = 0,
        explanation = "Der Rückversicherungsvertrag (18. Juni 1887) verpflichtete beide Seiten zu wohlwollender Neutralität bei Angriff durch Dritte. Zusätzlich anerkannte Deutschland Russlands Interessen auf dem Balkan und in der Meerengen-Frage. General Caprivi, Bismarcks Nachfolger, ließ den Vertrag 1890 auslaufen, da er dem Zweibund mit Österreich widersprach (Österreich und Russland standen am Balkan in Konflikt). Dies trieb Russland in die Annäherung an Frankreich.",
        difficulty = 4,
        funFact = "Das Auslaufen des Rückversicherungsvertrags gilt als einer der größten diplomatischen Fehler der deutschen Geschichte. Innerhalb von drei Jahren schloss Russland mit Frankreich die Entente (1894) – genau das Einkreisungsszenario, das Bismarck jahrzehntelang verhindert hatte. Er selbst sagte dazu: 'Der Faden ist zerrissen.'"
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Kriegsschuldfrage' und welche Historiker-Kontroverse entbrannte im 20. Jahrhundert darüber?",
        answerA = "Ob das Osmanische Reich oder Österreich für den Kriegsausbruch 1914 verantwortlich war",
        answerB = "Fritz Fischers These (1961): Deutschland trug die Hauptverantwortung durch aggressiven Expansionismus; Gegenthese: Der Krieg entstand durch kollektives Versagen aller Großmächte in einer Krise, die außer Kontrolle geriet",
        answerC = "Ob Serbien oder Russland den Krieg durch Mobilisierung ausgelöst hatte",
        answerD = "Christopher Clarks These, dass Österreich alleinverantwortlich war",
        correctAnswer = 1,
        explanation = "Die Fischer-Kontroverse begann 1961 mit Fritz Fischers Buch 'Griff nach der Weltmacht', das deutsche Hauptverantwortung durch bewusste Annexionspolitik und Kriegsplanung belegte. Gegner (Gerhard Ritter) betonten kollektive Verantwortung. Neuere Historiographie (Christopher Clark, 'Die Schlafwandler', 2012) betont, wie alle Großmächte in eine Krise schlitterten, die niemand vollständig kontrollierte.",
        difficulty = 4,
        funFact = "Christopher Clarks 'Schlafwandler' wurde in Deutschland ein Bestseller, da es die Alleinschuld-These relativierte. In Großbritannien und Frankreich wurde das Buch kritischer aufgenommen. Die Debatte zeigt, wie Geschichtsschreibung nationalen Selbstbildern dient – und wie Historiker dieselben Quellen zu gegensätzlichen Schlüssen führen können."
    ),

)
