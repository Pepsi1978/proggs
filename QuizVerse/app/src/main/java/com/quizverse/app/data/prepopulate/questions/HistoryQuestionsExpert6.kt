package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsExpert6(): List<Question> = listOf(

    // --- Weimar Constitution Specifics ---

    Question(
        categoryId = 3,
        questionText = "Welcher Artikel der Weimarer Verfassung von 1919 ermöglichte dem Reichspräsidenten, per Notverordnung ohne Reichstag zu regieren – und wurde 1933 systematisch missbraucht?",
        answerA = "Artikel 54",
        answerB = "Artikel 48",
        answerC = "Artikel 25",
        answerD = "Artikel 76",
        correctAnswer = 1,
        explanation = "Artikel 48 der Weimarer Verfassung ermächtigte den Reichspräsidenten, bei Störung der öffentlichen Sicherheit notwendige Maßnahmen zu ergreifen und dazu die Reichswehr einzusetzen sowie Grundrechte außer Kraft zu setzen. Zwischen 1930 und 1932 erließ Hindenburg über 250 Notverordnungen; nach dem Reichstagsbrand 1933 nutzte Hitler denselben Mechanismus zur Machtkonsolidierung.",
        difficulty = 4,
        funFact = "Der Staatsrechtler Carl Schmitt rechtfertigte die Diktaturgewalt des Artikel 48 theoretisch – und avancierte damit zum 'Kronjuristen des Dritten Reiches'. Er sah im Ausnahmezustand das eigentliche Wesen politischer Souveränität."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Artikel umfasste die Weimarer Reichsverfassung vom 11. August 1919, und welches Gremium verabschiedete sie?",
        answerA = "181 Artikel, Nationalversammlung in Weimar",
        answerB = "165 Artikel, Reichstag in Berlin",
        answerC = "200 Artikel, Nationalversammlung in Frankfurt",
        answerD = "148 Artikel, Verfassungskonvent in Dresden",
        correctAnswer = 0,
        explanation = "Die Weimarer Reichsverfassung umfasste 181 Artikel und wurde von der Deutschen Nationalversammlung in Weimar verabschiedet. Sie war in zwei Hauptteile gegliedert: 'Aufbau und Aufgaben des Reiches' (Art. 1–108) und 'Grundrechte und Grundpflichten der Deutschen' (Art. 109–165), plus Übergangs- und Schlussbestimmungen.",
        difficulty = 4,
        funFact = "Die Wahl Weimars als Tagungsort war kein Zufall: Berlin galt nach dem Januaraufstand der Spartakisten 1919 als zu unruhig. Weimar bot zudem symbolischen Wert als Ort der deutschen Klassik – Goethe und Schiller sollten die neue Demokratie adeln."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bestimmte der sogenannte 'Artikel 231' des Versailler Vertrags, der in Deutschland als 'Kriegsschuldlüge' empört zurückgewiesen wurde?",
        answerA = "Die Übergabe der deutschen Hochseeflotte an die Alliierten",
        answerB = "Die alleinige Kriegsschuld Deutschlands und seiner Verbündeten als Basis der Reparationspflicht",
        answerC = "Das Verbot einer deutschen Luftwaffe für 20 Jahre",
        answerD = "Die Abtretung des Saargebiets für 15 Jahre unter Völkerbundverwaltung",
        correctAnswer = 1,
        explanation = "Artikel 231 ('Kriegsschuldklausel') des Versailler Vertrags legte die alleinige Verantwortung Deutschlands und seiner Verbündeten für alle Verluste und Schäden des Krieges fest. Dies war die rechtliche Grundlage für Reparationsforderungen, die zunächst auf 132 Milliarden Goldmark festgesetzt wurden. Die Klausel erzeugte tiefe Verbitterung in Deutschland und nährte revanchistische Bewegungen.",
        difficulty = 4,
        funFact = "John Maynard Keynes verließ die Pariser Friedenskonferenz aus Protest und schrieb 'The Economic Consequences of the Peace' (1919), in dem er die Reparationsforderungen als wirtschaftlich ruinös und politisch destabilisierend brandmarkte – eine Prophezeiung, die sich bewahrheiten sollte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Wahlsystem verwendete die Weimarer Republik, das zum massiven Parteiensplitting beitrug?",
        answerA = "Relative Mehrheitswahl in Einerwahlkreisen (britisches System)",
        answerB = "Reines Verhältniswahlrecht ohne Sperrklausel nach d'Hondt",
        answerC = "Verhältniswahl mit 5%-Sperrklausel nach Hare-Niemeyer",
        answerD = "Zweistimmensystem mit gemischter Kompensation",
        correctAnswer = 1,
        explanation = "Die Weimarer Republik nutzte ein reines Verhältniswahlrecht ohne Sperrklausel: Pro 60.000 Stimmen erhielt eine Partei einen Reichstagssitz. Dies führte zur extremen Fragmentierung – im Reichstag saßen zeitweise über 15 Parteien. Die fehlende Sperrklausel ermöglichte Klein- und Splitterparteien den Einzug und erschwerte die Regierungsbildung dramatisch.",
        difficulty = 4,
        funFact = "Die Bundesrepublik Deutschland zog die historische Lehre und führte 1949 die 5%-Sperrklausel ein. Diese 'Weimarer Lektion' wird bis heute als Begründung für die Hürde herangezogen, obwohl Historiker debattieren, ob das Wahlrecht tatsächlich der Hauptgrund für Weimars Scheitern war."
    ),

    // --- Stresemann Era ---

    Question(
        categoryId = 3,
        questionText = "Gustav Stresemann war 1923 nur 100 Tage Reichskanzler, blieb aber bis 1929 Außenminister. Welches war sein größter außenpolitischer Triumph?",
        answerA = "Der Beitritt Deutschlands zum Völkerbund 1926",
        answerB = "Die Unterzeichnung des Kellogg-Briand-Pakts 1928",
        answerC = "Der Locarno-Vertrag 1925 mit Westgrenzgarantien",
        answerD = "Die Räumung des Rheinlandes durch alliierte Truppen 1930",
        correctAnswer = 2,
        explanation = "Der Locarno-Vertrag (Oktober 1925) war Stresemanns Meisterwerk: Deutschland, Frankreich, Belgien, Großbritannien und Italien garantierten gegenseitig die Unverletzlichkeit der deutsch-französischen und deutsch-belgischen Grenzen. Dies leitete Deutschlands Rückkehr in die europäische Staatenwelt ein und brachte Stresemann gemeinsam mit Briand und Chamberlain den Friedensnobelpreis 1926.",
        difficulty = 4,
        funFact = "Stresemann spielte ein doppeltes Spiel: Westlich betrieb er Entspannung, östlich hielt er die geheime Militärkooperation mit der Sowjetunion (Rapallo) aufrecht. Für die östlichen Grenzen (Versailler Korridor, Danzig) gab er keine Garantien ab – was er für eine spätere 'friedliche Revision' offen hielt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Währungsreform beendete 1923 die deutsche Hyperinflation, und wer war ihr maßgeblicher Architekt?",
        answerA = "Einführung der Goldmark durch Reichsbankpräsident Rudolf Havenstein",
        answerB = "Einführung der Rentenmark durch Hjalmar Schacht",
        answerC = "Rückkehr zum Goldstandard durch Reichsfinanzminister Hans Luther",
        answerD = "Dollarisierung der deutschen Wirtschaft durch Charles Dawes",
        correctAnswer = 1,
        explanation = "Die Rentenmark wurde am 15. November 1923 eingeführt und ersetzte die wertlose Papiermark im Verhältnis 1:1.000.000.000.000 (eine Billion). Hjalmar Schacht, der neue Reichsbankkommissar, stoppte die Geldmenge konsequent und verankerte die neue Währung hypothekarisch am deutschen Grund- und Industrievermögen – ohne echten Golddeckung, aber psychologisch wirksam.",
        difficulty = 4,
        funFact = "Auf dem Höhepunkt der Hyperinflation im November 1923 kostete ein Brot 201 Milliarden Mark. Ein US-Dollar entsprach 4,2 Billionen Mark. Frauen wurden berichtet, ihr Geld in Körben statt Geldbeuteln zu tragen – und Diebe warfen die Scheine weg, behielten aber die Körbe."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter dem 'Dawes-Plan' von 1924 genau, und welche zentrale Neuerung enthielt er gegenüber dem ursprünglichen Reparationsregime?",
        answerA = "Er erließ Deutschland 50% der Schulden und strich die Kriegsschuldklausel",
        answerB = "Er staffelte Zahlungen nach Deutschlands wirtschaftlicher Leistungsfähigkeit und ermöglichte US-Kredite",
        answerC = "Er ersetzte Geldleistungen durch Sachlieferungen (Kohle, Stahl, Holz)",
        answerD = "Er übertrug die Reparationsabwicklung vom deutsch-alliierten Gemischten Schiedsgerichtshof auf den Völkerbund",
        correctAnswer = 1,
        explanation = "Der Dawes-Plan (August 1924) schuf ein revidiertes Reparationssystem: Zahlungen wurden nach Deutschlands wirtschaftlicher Kapazität gestaffelt (anfangs 1 Milliarde, steigend auf 2,5 Milliarden Goldmark jährlich). Crucial: Er öffnete Deutschland für amerikanische Privatanleihen (zunächst 800 Millionen Goldmark), die die Wirtschaft stabilisierten und den 'Reparationskreislauf' schufen.",
        difficulty = 4,
        funFact = "Charles G. Dawes, der den Plan entwarf, war beim Empfang des Friedensnobelpreises 1925 nicht anwesend – er hielt eine Bankeransprache. Sein Co-Preisträger Austen Chamberlain (für Locarno) bemerkte trocken, Dawes scheine seinen Preis für dringlicher zu halten als seine Bankerrede."
    ),

    // --- Hugenberg / Media Empire ---

    Question(
        categoryId = 3,
        questionText = "Alfred Hugenberg kontrollierte bis 1933 ein Medienimperium, das einen erheblichen Teil der deutschen Presselandschaft beeinflusste. Welches Nachrichtenagentur-Herzstück gehörte dazu?",
        answerA = "Reuters Deutschland (Berliner Büro)",
        answerB = "Telegraphen-Union (TU) und die Ala-Anzeigen-AG",
        answerC = "Wolff'sches Telegraphisches Bureau (WTB)",
        answerD = "Deutsches Nachrichtenbüro (DNB)",
        correctAnswer = 1,
        explanation = "Hugenbergs Konzern umfasste die Telegraphen-Union (größte deutsche Nachrichtenagentur nach dem WTB), die Ala-Werbe-AG (größtes Anzeigenvermittlungsunternehmen), den August-Scherl-Verlag mit Berliner Tageszeitungen sowie die UFA (Universum Film AG). Damit kontrollierte er ca. 50% der deutschen Provinzpresse und die wichtigste Filmgesellschaft.",
        difficulty = 4,
        funFact = "Hugenberg nutzte sein Medienimperium aktiv für nationalkonservative Politik – er war DNVP-Vorsitzender und Mitunterzeichner der 'Harzburger Front' 1931. Als er Hitler 1933 als Koalitionspartner akzeptierte und ins Kabinett eintrat, glaubte er, ihn kontrollieren zu können. Wenige Monate später hatte Hitler sein Amt übernommen und Hugenberg war politisch erledigt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Filmstudio übernahm Hugenberg 1927, das fortan zum wichtigsten deutschen Filmkonzern der Weimarer Republik und des Dritten Reiches wurde?",
        answerA = "Tobis-Klangfilm AG",
        answerB = "Messter Film GmbH",
        answerC = "Universum Film AG (UFA)",
        answerD = "Terra-Filmkunst GmbH",
        correctAnswer = 2,
        explanation = "Hugenberg übernahm 1927 die UFA (gegründet 1917 als Kriegspropagandainstrument). Die UFA produzierte Klassiker wie 'Metropolis' (Fritz Lang, 1927) und 'Der blaue Engel' (1930). Nach 1933 wurde sie zum zentralen Propagandainstrument des NS-Regimes; Joseph Goebbels übernahm die faktische Kontrolle, formell blieb sie bis 1937 privat.",
        difficulty = 4,
        funFact = "Marlene Dietrichs Durchbruch im 'Blauen Engel' (1930) war eine UFA-Produktion unter Hugenbergs Ägide. Regisseur Josef von Sternberg verpflichtete Dietrich gegen Hugenbergs anfänglichen Widerstand – ein Casting-Entscheid, der Weltgeschichte schrieb."
    ),

    // --- Bretton Woods ---

    Question(
        categoryId = 3,
        questionText = "Die Bretton-Woods-Konferenz 1944 schuf zwei internationale Institutionen. Welche Aufgaben hatten IWF und Weltbank ursprünglich präzise?",
        answerA = "IWF: Entwicklungsfinanzierung; Weltbank: Währungsstabilisierung",
        answerB = "IWF: kurzfristige Zahlungsbilanzstützung; Weltbank: langfristige Wiederaufbaukredite",
        answerC = "IWF: Handelsstreitschlichtung; Weltbank: Infrastrukturprojekte in Kolonien",
        answerD = "IWF: Goldpreisfestsetzung; Weltbank: europäischer Wiederaufbau als Marshallplan-Vorgänger",
        correctAnswer = 1,
        explanation = "Der IWF (Internationaler Währungsfonds) sollte kurzfristige Zahlungsbilanzdefizite überbrücken, um Währungsabwertungswettläufe wie in den 1930ern zu verhindern. Die Weltbank (IBRD) vergab langfristige Kredite für Wiederaufbau und Entwicklung. Das System fixierte Wechselkurse bei einer Dollarparität von 35 USD pro Feinunze Gold.",
        difficulty = 4,
        funFact = "Die britische Delegation unter John Maynard Keynes schlug stattdessen eine 'Bancor'-Weltwährung vor, die niemanden begünstigen sollte. Die USA, als neuer Gläubiger der Welt, lehnten ab – und der Dollar wurde zur Weltwährung. Keynes soll kommentiert haben: 'We have given them the show.'"
    ),

    Question(
        categoryId = 3,
        questionText = "Richard Nixon beendete 1971 das Bretton-Woods-System mit dem sogenannten 'Nixon-Schock'. Was genau verkündete er am 15. August 1971?",
        answerA = "Den Austritt der USA aus dem IWF und die Einführung flexibler Wechselkurse",
        answerB = "Die Aussetzung der Dollarkonvertibilität in Gold und einen 10%-Importzoll",
        answerC = "Die Abwertung des Dollars um 8,5% gegenüber Gold und Währungskörben",
        answerD = "Das Ende des Festkurssystems und den Beitritt der USA zur Sonderziehungsrecht-Zone",
        correctAnswer = 1,
        explanation = "Nixon verkündete am 15. August 1971 die sofortige Aussetzung der Goldbindung des Dollars (bisher 35 USD/Unze Gold) sowie einen temporären 10%-Einfuhrzoll. Dies beendete de facto das Bretton-Woods-Festkurssystem. Die Smithsonian-Vereinbarung (Dezember 1971) versuchte eine Neuordnung, doch 1973 gingen alle großen Währungen zu flexiblen Wechselkursen über.",
        difficulty = 4,
        funFact = "Nixon unterbrach für die Bekanntgabe die Fernsehserie 'Bonanza' – die populärste Sendung des amerikanischen Sonntagabends. Die Nachricht traf Europa schlafend: Bundesbankpräsident Karl Klasen erfuhr davon am Montagmorgen und musste spontan milliardenschwere Dollarinterventionen anordnen."
    ),

    // --- Marshall Plan Details ---

    Question(
        categoryId = 3,
        questionText = "Wie hoch war die Gesamtsumme des Marshallplans (European Recovery Program), und welches Land erhielt den größten Anteil?",
        answerA = "Ca. 13 Milliarden Dollar; Großbritannien erhielt den größten Anteil",
        answerB = "Ca. 17 Milliarden Dollar; Frankreich erhielt den größten Anteil",
        answerC = "Ca. 20 Milliarden Dollar; West-Deutschland erhielt den größten Anteil",
        answerD = "Ca. 12 Milliarden Dollar; Italien erhielt den größten Anteil",
        correctAnswer = 0,
        explanation = "Der Marshallplan (1948–1952) umfasste ca. 13,3 Milliarden Dollar (etwa 140 Milliarden in heutiger Kaufkraft). Großbritannien erhielt mit ca. 3,3 Milliarden Dollar den größten Einzelanteil, gefolgt von Frankreich (2,3 Mrd.) und West-Deutschland (1,4 Mrd.). Die Hilfe bestand zu ca. 90% aus Zuschüssen, nicht Krediten.",
        difficulty = 4,
        funFact = "Die Sowjetunion lehnte den Marshallplan ab und untersagte auch osteuropäischen Satellitenstaaten die Teilnahme – Tschechoslowakei und Polen hatten anfänglich Interesse gezeigt. Stalin interpretierte das Programm korrekt als US-Instrument zur Bindung Westeuropas an die amerikanische Wirtschaftssphäre."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche organisatorische Bedingung stellten die USA für den Erhalt von Marshallplan-Geldern, die zur Gründung der OEEC führte?",
        answerA = "Einführung demokratischer Verfassungen in allen Empfängerländern",
        answerB = "Koordinierte europäische Wirtschaftsplanung und gemeinsame Verwendungsplanung der Mittel",
        answerC = "Beitritt zur NATO als Voraussetzung für Wirtschaftshilfe",
        answerD = "Liberalisierung des Außenhandels und Aufhebung aller Einfuhrzölle innerhalb von 5 Jahren",
        correctAnswer = 1,
        explanation = "Die USA verlangten, dass Europa die Mittel koordiniert beantragt und verwendet – nicht jedes Land einzeln. Dies zwang zur Gründung der Organisation für Europäische Wirtschaftliche Zusammenarbeit (OEEC, 1948, später OECD), die als Vorläufer europäischer Integration gilt. 16 westeuropäische Staaten koordinierten erstmals gemeinsam ihre Wirtschaftspolitik.",
        difficulty = 4,
        funFact = "Konrad Adenauer schätzte den Marshallplan als wichtigsten Faktor für das 'Wirtschaftswunder'. Tatsächlich startete die eigentliche Hochkonjunktur erst nach dem Koreakrieg (1950) – der Plan schuf aber das institutionelle und psychologische Fundament für den Aufschwung."
    ),

    // --- GATT ---

    Question(
        categoryId = 3,
        questionText = "Das GATT (General Agreement on Tariffs and Trade) wurde 1947 unterzeichnet. Welches Prinzip war sein organisatorisches Fundament?",
        answerA = "Gegenseitige Meistbegünstigung (Most Favoured Nation, MFN)",
        answerB = "Gemeinsamer Außenzoll der Unterzeichnerstaaten",
        answerC = "Verbindliche Streitschlichtung durch den Internationalen Gerichtshof",
        answerD = "Vollständige Zollfreiheit für Industriegüter unter allen Mitgliedern",
        correctAnswer = 0,
        explanation = "Das MFN-Prinzip (Meistbegünstigungsklausel) verpflichtete GATT-Mitglieder, Handelsvergünstigungen die einem Partner gewährt wurden, automatisch auf alle anderen Partner auszudehnen. Dies verhinderte diskriminierende bilaterale Abkommen. Ergänzt wurde es durch das 'National Treatment'-Prinzip: Importierte Waren durften nicht schlechter als inländische behandelt werden.",
        difficulty = 4,
        funFact = "Das GATT war eigentlich ein Provisorium – geplant war eine 'Internationale Handelsorganisation' (ITO), die aber am US-Kongress scheiterte. Dieses Provisorium existierte 47 Jahre lang (1947–1994), bis es durch die WTO abgelöst wurde. Manchmal sind die dauerhaftesten Institutionen die unbeabsichtigten."
    ),

    // --- Stasi Methods ---

    Question(
        categoryId = 3,
        questionText = "Was verstand die Stasi unter der Methode der 'Zersetzung', und in welchem internen Dokument war sie systematisiert?",
        answerA = "Physische Verhaftung und Verhör ohne Anklage nach der 'Verordnung 1/81'",
        answerB = "Psychische Destabilisierung ohne physische Gewalt, beschrieben in der 'Richtlinie 1/76'",
        answerC = "Telefonüberwachung und Briefkontrolle nach dem 'Postgesetz 1958'",
        answerD = "Anwerbung von Informellen Mitarbeitern nach der 'Direktive 2/73'",
        correctAnswer = 1,
        explanation = "Die Richtlinie 1/76 des MfS ('zur Entwicklung und Bearbeitung Operativer Vorgänge') systematisierte Zersetzungsmaßnahmen: Rufschädigung durch gezielte Gerüchte, heimliche Einbrüche mit minimalen Veränderungen (umgestellte Möbel, verdorbene Lebensmittel), berufliche Sabotage, Manipulation von Beziehungen. Ziel war die psychische Destabilisierung 'feindlich-negativer Personen' ohne Verhaftung.",
        difficulty = 4,
        funFact = "Betroffene der Zersetzung zeigten oft klassische Symptome paranoider Schizophrenie und wurden von Psychiatern entsprechend diagnostiziert – was die Stasi bewusst einkalkulierte. Nach 1989 erkannte man viele 'psychische Erkrankungen' der DDR-Zeit als Folgen gezielter staatlicher Psychoterror."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Inoffizielle Mitarbeiter (IM) unterhielt die Stasi auf dem Höhepunkt ihrer Macht ca. 1988?",
        answerA = "Ca. 30.000 hauptamtliche Mitarbeiter und 180.000 IMs",
        answerB = "Ca. 91.000 hauptamtliche Mitarbeiter und 174.000 IMs",
        answerC = "Ca. 50.000 hauptamtliche und 300.000 informelle Mitarbeiter",
        answerD = "Ca. 120.000 hauptamtliche Mitarbeiter und 90.000 IMs",
        correctAnswer = 1,
        explanation = "1988 beschäftigte das MfS ca. 91.000 hauptamtliche Mitarbeiter und ca. 174.000 registrierte Inoffizielle Mitarbeiter – in einem Land mit 16 Millionen Einwohnern. Dies ergibt ein Verhältnis von etwa 1 Stasi-Mitarbeiter (haupt- + ehrenamtlich) auf 63 Bürger. Zum Vergleich: Die Gestapo hatte im gesamten 'Dritten Reich' ca. 32.000 Beamte.",
        difficulty = 4,
        funFact = "Der Stasi-Chef Erich Mielke, der das MfS von 1957 bis 1989 leitete, sagte bei seiner letzten Rede vor der Volkskammer im November 1989: 'Aber ich liebe doch alle Menschen!' – ein Satz, der zur historischen Kuriosität wurde, als der Saal in Gelächter ausbrach."
    ),

    // --- CIA Coups ---

    Question(
        categoryId = 3,
        questionText = "Operation AJAX/BOOT 1953: Welche Regierung wurde durch CIA und MI6 gestürzt, und wer war der gestürzte Premierminister?",
        answerA = "Ägypten unter Mustafa an-Nahhas Pascha",
        answerB = "Iran unter Mohammad Mosaddegh",
        answerC = "Guatemala unter Jacobo Árbenz Guzmán",
        answerD = "Syrien unter Adib al-Schischakli",
        correctAnswer = 1,
        explanation = "Operation AJAX (CIA) / BOOT (MI6) stürzte im August 1953 den demokratisch gewählten iranischen Premierminister Mohammad Mosaddegh, der die Anglo-Iranian Oil Company (AIOC, heute BP) verstaatlicht hatte. Shah Mohammad Reza Pahlavi wurde mit US-Unterstützung wieder eingesetzt. Die CIA gab ihre Beteiligung erst 2013 offiziell zu.",
        difficulty = 4,
        funFact = "Kermit Roosevelt Jr., Enkel von Präsident Theodore Roosevelt und CIA-Operationsleiter, bezahlte iranische Schläger, die pro-Mosaddegh-Demonstrationen angreifen und Unruhe stiften sollten. Später prahlte er in Washington damit, wie einfach der Coup gewesen sei – was CIA-Chef Allen Dulles beunruhigte, da er fürchtete, Roosevelt würde ähnliche Aktionen überall einsetzen wollen."
    ),

    Question(
        categoryId = 3,
        questionText = "Operation PBSUCCESS (1954): Welches mittelamerikanische Land wurde durch eine CIA-Operation destabilisiert, und was war der wirtschaftliche Hintergrund?",
        answerA = "Nicaragua; Nationalisierung von Standard Oil-Konzessionen",
        answerB = "Guatemala; Landreform bedrohte United Fruit Company-Besitz",
        answerC = "Honduras; Enteignung von Bananenplantagen der Chiquita-Vorgänger",
        answerD = "El Salvador; Kupferverstaatlichung schadete US-Bergbaukonzernen",
        correctAnswer = 1,
        explanation = "Jacobo Árbenz Guzmáns Agrarreform in Guatemala (1952) enteignete unbebautes Land der United Fruit Company (UFCO) gegen Entschädigung zum Steuerwert. Die UFCO besaß 42% der guatemaltekischen Landmasse, nutzte aber nur 15%. CIA-Direktor Allen Dulles und sein Bruder, Außenminister John Foster Dulles, hatten persönliche UFCO-Verbindungen. 'Operation PBSUCCESS' installierte Militärdiktator Castillo Armas.",
        difficulty = 4,
        funFact = "Guatemala war das erste CIA-Coup-Opfer, das durch Desinformation wesentlich destabilisiert wurde: Eine 'Stimme Guatemalas' (Scheinradiosender der CIA) verbreitete erfundene Berichte über heranrückende Invasionstruppen, was die guatemaltekische Armee zur Kapitulation veranlasste, obwohl Árbenz' Armee noch schlagkräftig war."
    ),

    // --- MI6 ---

    Question(
        categoryId = 3,
        questionText = "Der 'Cambridge Five' Spionagering lieferte jahrzehntelang britische Staatsgeheimnisse an die Sowjetunion. Welches Mitglied entkam 1963 nach Moskau, nachdem es als MI6-Verbindungsmann nach Washington gedient hatte?",
        answerA = "Guy Burgess",
        answerB = "Donald Maclean",
        answerC = "Anthony Blunt",
        answerD = "Kim Philby",
        correctAnswer = 3,
        explanation = "Harold Adrian Russell 'Kim' Philby war das wertvollste Mitglied: Er stieg bis zum Leiter der anti-sowjetischen Sektion des MI6 auf und diente als Verbindungsoffizier zur CIA in Washington, wo er CIA-Geheimoperationen an den KGB verriet (u.a. die albanischen Partisanen-Infiltrationen). 1963 floh er aus Beirut nach Moskau, kurz bevor seine Enttarnung unvermeidlich wurde.",
        difficulty = 4,
        funFact = "Philby wurde trotz früher Verdachtsmomente mehrfach intern 'freigesprochen', weil das Establishment einfach nicht glauben wollte, dass einer 'von ihnen' (Eton, Cambridge) ein Verräter sein könnte. Als CIA-Chef James Angleton erfuhr, wen er als engen Freund und Vertrauensmann behandelt hatte, verfiel er in jahrelanges misstrauisches Paranoia, das die CIA lähmte."
    ),

    // --- Radar Development ---

    Question(
        categoryId = 3,
        questionText = "Das britische 'Chain Home'-Radarnetz war entscheidend in der Battle of Britain. Welches technische Merkmal unterschied es vom deutschen Freya-Radar und war gleichzeitig taktischer Vorteil?",
        answerA = "Chain Home nutzte kürzere Wellenlängen und konnte Flugzeuge bis 800 km orten",
        answerB = "Chain Home nutzte lange Wellenlängen (10-13 m), war ungenauer aber schwerer zu stören und lieferte früher Frühwarnungen",
        answerC = "Chain Home war das erste rotierende Radar mit 360°-Abdeckung",
        answerD = "Chain Home konnte als erstes System die Flughöhe präzise messen und Staffeln zählen",
        correctAnswer = 1,
        explanation = "Chain Home (CH) nutzte Langwellen (10–13 Meter), was technisch primitiver war als das deutsche Freya (2,4 m) oder Würzburg (53 cm). Die langen Wellen lieferten aber frühe Warnungen über 120–150 km Entfernung. Die Stationen waren fest und richtungslos – ihre Signale konnten nicht einfach abgeschirmt werden. Das Dowding-System integrierte CH-Daten mit Beobachterkorps in ein zentralisiertes Führungsnetz.",
        difficulty = 4,
        funFact = "Die Deutschen erkannten die britischen CH-Masten, bombardierten sie aber nicht systematisch – sie glaubten, deren lange Wellenlängen seien zu primitiv für effektive Luftraumüberwachung. Dies war einer der größten Aufklärungsfehler der Luftwaffe im Zweiten Weltkrieg."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Grundprinzip der Radarentwicklung nutzte Robert Watson-Watt 1935 für das erste britische Radarsystem, das ursprünglich für einen ganz anderen Zweck gedacht war?",
        answerA = "Das Mikrowellen-Resonanzprinzip, ursprünglich aus der Medizintechnik",
        answerB = "Den Doppler-Effekt, ursprünglich zur Windmessung in der Meteorologie entwickelt",
        answerC = "Die Hertz'schen Radiowellen, die Watson-Watt ursprünglich für einen 'Todesstrahl' erforschte",
        answerD = "Die Ionosphäre-Reflexion, die er beim Studium atmosphärischer Störungen entdeckte",
        correctAnswer = 3,
        explanation = "Watson-Watt wurde vom Air Ministry gefragt, ob ein 'Todesstrahl' aus Radiowellen möglich sei. Er rechnete nach: nein, unmöglich. Aber er merkte, dass zurückgeworfene Radiowellen Flugzeuge detektierbar machen könnten. Aus seinem Memo vom Februar 1935 und dem ersten Experiment in Daventry (26. Februar 1935, wo ein Bomber auf 8 Meilen Entfernung orten) entstand Chain Home.",
        difficulty = 4,
        funFact = "Das Wort 'Radar' wurde erst 1940 von der US Navy geprägt (Radio Detection And Ranging) – vorher nannten Briten es schlicht 'RDF' (Radio Direction Finding), um seine Fähigkeiten zu verschleiern. Auch intern wurde über die Technologie mit Decknamen gearbeitet."
    ),

    // --- V-Weapons ---

    Question(
        categoryId = 3,
        questionText = "Die V-2-Rakete war das erste ballistische Fernlenkgeschoss der Geschichte. Welcher technische Aspekt ihres Antriebs war revolutionär?",
        answerA = "Sie war die erste Rakete mit Feststoffantrieb, der in Großserie produziert werden konnte",
        answerB = "Sie nutzte als erste Waffe Flüssigsauerstoff und Ethanol in einer Turbopumpen-getriebenen Brennkammer",
        answerC = "Sie war die erste Strahltriebwerk-getriebene Rakete mit Überschallgeschwindigkeit",
        answerD = "Sie nutzte als erste Rakete Atomenergie zur Treibstofferhitzung im Düsenantrieb",
        correctAnswer = 1,
        explanation = "Die V-2 (A4) nutzte einen Flüssigtreibstoff-Raketenmotor mit 75%-igem Ethanol und flüssigem Sauerstoff, befördert durch eine Turbopumpe mit ca. 56.000 PS (angetrieben durch Wasserstoffperoxid-Dampf). Der Motor lieferte 25 Tonnen Schub und beschleunigte die 14-Tonnen-Rakete auf 1.600 m/s (Mach 5,5). Wernher von Braun und Walter Thiel entwickelten diesen Antrieb in Peenemünde.",
        difficulty = 4,
        funFact = "Der V-2 tötete mehr Menschen in der Produktion als durch ihren Einsatz: Ca. 9.000 KZ-Häftlinge (vor allem aus dem Lager Dora/Mittelbau) starben bei der Herstellung der Raketen unter brutalen Bedingungen in unterirdischen Tunneln. Die ca. 3.000 V-2-Angriffe auf Antwerpen und London töteten ca. 7.250 Personen."
    ),

    Question(
        categoryId = 3,
        questionText = "Die V-1 (Fieseler Fi 103) wurde auf welchem Antriebsprinzip basierend angetrieben, und warum konnte sie im Gegensatz zur V-2 abgeschossen werden?",
        answerA = "Raketentriebwerk; sie flog zu tief für Flak aber zu langsam für Düsenjäger",
        answerB = "Pulsstrahlrohr (Argus As 014); ihre konstante niedrige Geschwindigkeit (ca. 640 km/h) ermöglichte Abfang durch schnelle Jagdflugzeuge",
        answerC = "Ramjet-Triebwerk; ihre geradlinige Flugbahn war vorhersehbar, aber ihre Höhe über 8.000 m schützte sie",
        answerD = "Turbostrahltriebwerk; sie war zu laut zum Abfangen aber zu schnell für Kolbenmotor-Jäger",
        correctAnswer = 1,
        explanation = "Das Pulsstrahlrohr der V-1 erzeugte das charakteristische Brummgeräusch ('doodlebug'). Mit ca. 640 km/h konnte sie von Spitfires und Tempests abgefangen werden – teils durch Kippen des Flügels (gefährlich) oder Beschuss. Fortschrittliche Prädiktions-Flak und Ballon-Sperren ergänzten die Abwehr. Bis Kriegsende wurden ca. 55% der V-1 abgefangen oder zerstört.",
        difficulty = 4,
        funFact = "RAF-Pilot Roland Beaumont entwickelte die Technik, die V-1 durch Unterfliegen des Flügels mit dem eigenen Flügel zu kippen und so die Steuerung zu stören – eine Lösung, die Munition sparte. Manche Piloten vollführten dieses Kunststück bei über 600 km/h in wenigen Metern Abstand zur Bombe."
    ),

    // --- Nuclear Arms Race ---

    Question(
        categoryId = 3,
        questionText = "Das 'Baruch-Plan' (1946) sollte die nukleare Abrüstung einleiten. Warum scheiterte er an der Sowjetunion?",
        answerA = "Die USA forderten sofortige Übergabe aller Atomwaffen an die UN, bevor Kontrollen vereinbart wurden",
        answerB = "Baruch forderte ein internationales Kontrollorgan mit unbeschränktem Inspektionsrecht und Veto-freien Sanktionen, was Sowjetsouveränität verletzte",
        answerC = "Stalin lehnte ab, weil er kurz vor dem eigenen Atomtest stand und keine Konkurrenz wollte",
        answerD = "Die Sowjetunion forderte, dass alle Atommächte gleichzeitig abrüsten, was die USA ablehnten",
        correctAnswer = 1,
        explanation = "Bernard Baruchs Plan sah eine 'International Atomic Development Authority' (IADA) vor, die Atomenergie weltweit kontrollieren sollte – mit unbegrenztem Inspektionsrecht und einem Veto-freien Sanktionsmechanismus. Die Sowjetunion lehnte das Inspektionsrecht als Souveränitätsverletzung ab und bestand darauf, dass die USA zuerst ihre Waffen vernichten. Die USA wollten nicht abrüsten, bevor Kontrollen gesichert waren – ein klassisches Sicherheitsdilemma.",
        difficulty = 4,
        funFact = "Die USA besaßen 1946 gerade einmal neun funktionsfähige Atombomben und hatten kaum Plutonium für weitere. Die sowjetische Ablehnung des Baruch-Plans galt lange als Beweis bösen Willens – aus heutiger Sicht war die amerikanische Forderung nach Veto-freien Sanktionen tatsächlich eine Forderung nach US-Dominanz über globale Atomkontrolle."
    ),

    Question(
        categoryId = 3,
        questionText = "Die 'Wasserstoffbombe' (H-Bombe) basiert auf Kernfusion. Wer entwickelte das entscheidende 'Teller-Ulam-Design' (1951), das überhaupt erst eine praktikable Fusionsbombe ermöglichte?",
        answerA = "Edward Teller allein; Stanislaw Ulam übernahm nur die mathematische Berechnung",
        answerB = "Stanislaw Ulam mit dem Konzept der Strahlungsimplosion; Edward Teller ergänzte die Stufentrennung",
        answerC = "Gemeinsam John von Neumann und Klaus Fuchs noch vor dem sowjetischen Atomtest",
        answerD = "Enrico Fermi entwickelte das Grundkonzept; Teller und Ulam waren die mathematischen Ausfertiger",
        correctAnswer = 1,
        explanation = "Das Teller-Ulam-Design (März 1951) löste das Problem: Ulam erkannte, dass Röntgenstrahlung der Primärstufe (Atombombe) zur Implosion einer separaten Fusionskapseln genutzt werden kann (Strahlungsimplosion). Teller ergänzte, dass nicht Implosionsdruck sondern Strahlung der Mechanismus ist und entwickelte die zweistufige Konfiguration. Ohne diesen Durchbruch wäre eine praktikable H-Bombe kaum möglich gewesen.",
        difficulty = 4,
        funFact = "Edward Teller wurde nach dem Krieg als 'Vater der Wasserstoffbombe' gefeiert, was zu einem erbitterten Streit mit Ulam führte. Im Los-Alamos-Milieu war Teller nach seiner Aussage gegen Robert Oppenheimer (1954) persona non grata – viele Wissenschaftler schüttelten ihm jahrelang nicht die Hand."
    ),

    // --- Sputnik Crisis ---

    Question(
        categoryId = 3,
        questionText = "Der Sputnik-Schock (4. Oktober 1957) löste in den USA eine umfassende institutionelle Reaktion aus. Welche zwei wichtigsten Gründungsreaktionen folgten unmittelbar?",
        answerA = "NASA-Gründung und National Defense Education Act (NDEA)",
        answerB = "DARPA-Gründung und Übernahme der Raumfahrt durch das Militär",
        answerC = "NSA-Erweiterung und Einführung der Wehrpflicht für Naturwissenschaftler",
        answerD = "NASA und Gründung der Rand Corporation als Strategieinstitut",
        correctAnswer = 0,
        explanation = "Als Reaktion auf Sputnik gründeten die USA 1958 die NASA (National Aeronautics and Space Administration, 29. Juli 1958) als zivile Raumfahrtbehörde, um das Militär aus dem Weltraum herauszuhalten. Parallel verabschiedeten sie den National Defense Education Act (NDEA, September 1958), der massiv Bundesgelder für Mathematik-, Naturwissenschafts- und Fremdsprachenausbildung bereitstellte – Bildungspolitik als nationale Sicherheitsfrage.",
        difficulty = 4,
        funFact = "Sputnik wog gerade 83,6 kg und sendete nur ein einfaches Funksignal (Beep-Beep). Trotzdem versetzte sein Überflug US-amerikanische Städte in kollektive Panik. Life-Magazine warnte: 'The Red Moons of Sputnik' – und die amerikanische Selbstwahrnehmung als technologisch überlegene Nation zerbrach über Nacht."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche geheime US-Aufklärungsmission nutzte Sputnik-Start und Weltraumrecht als strategischen Präzedenzfall?",
        answerA = "Die U-2-Überflüge über der Sowjetunion, die 1957 durch Sputnik-Präzedenz als legal galten",
        answerB = "Das Corona-Satelliten-Aufklärungsprogramm, das Überflüge als legale Raumfahrt klassifizierte",
        answerC = "Operation OXCART (SR-71-Vorläufer), die Sputnik als Ablenkungsmanöver nutzte",
        answerD = "Das MIDAS-Frühwarnsystem, das unter dem Deckmantel ziviler Wettersatelliten operierte",
        correctAnswer = 1,
        explanation = "Die USA erkannten, dass Sputnik einen Präzedenzfall schuf: Wenn Satelliten freien Überflug über fremdem Territorium beanspruchen können, gilt dies auch für Spionagesatelliten. Das geheime Corona-Programm (erste erfolgreiche Mission 1960, KH-1) nutzte genau dieses 'Weltraumrecht' und lieferte mehr Bildaufklärung als alle U-2-Missionen zusammen – mit weit geringerem politischem Risiko.",
        difficulty = 4,
        funFact = "Die erste erfolgreiche Corona-Kapsel wurde mitten im Pazifik aus dem Orbit geborgen – ein C-119-Flugzeug 'fing' die fallende Kapsel mit einem Haken, bevor sie ins Meer eintauchte. Die Fotos zeigten mehr sowjetische Raketenstarts als die CIA für möglich gehalten hatte."
    ),

    // --- Gemini Program ---

    Question(
        categoryId = 3,
        questionText = "Das Gemini-Programm (1961–1966) war die Brücke zwischen Mercury und Apollo. Welche drei kritischen Technologien testete es, ohne die Apollo unmöglich gewesen wäre?",
        answerA = "Mondlandungsfähre, Radarnavigation und Mondanzüge",
        answerB = "Langzeitaufenthalte im Orbit (bis 14 Tage), Rendezvous/Andocken im Weltraum und Außenbordeinsätze (EVA)",
        answerC = "Wiedereintrittssystem, Triebwerkstest und Mondorbit-Navigation",
        answerD = "Mondorbit-Rendezvous, Schwerkraftmanöver und Lebenserhaltungssysteme für 30 Tage",
        correctAnswer = 1,
        explanation = "Gemini testete die drei entscheidenden Apollo-Technologien: (1) Langzeitaufenthalte bis 14 Tage (Gemini 7) – Apollo dauerte ca. 8 Tage; (2) Rendezvous und Andocken (Gemini 8 – erste Andockung im Orbit, März 1966); (3) Außenbordeinsätze/EVA (Gemini 4, Edward White, erste US-EVA 1965). Ohne diese Tests wäre die Mondlandung 1969 technisch nicht möglich gewesen.",
        difficulty = 4,
        funFact = "Gemini 8 erlebte fast eine Katastrophe: Ein fehlerhaftes Triebwerk feuerte dauerhaft und brachte die Kapsel in eine lebensbedrohliche Taumelrotation (1 Umdrehung/Sekunde). Astronaut Neil Armstrong rettete die Situation durch Abkopplung vom Agena-Zielobjekt und Einleiten des Notlandungssystems – sein erster Test unter extremem Druck vor dem Mond."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches war die erste bemannten Gemini-Mission, die ein Andocken mit einem unbemannten Zielobjekt vollendete, und welche kritische Fehlfunktion trat dabei auf?",
        answerA = "Gemini 6A dockte erstmals an Gemini 7 an, aber das Rendezvous-Radar versagte",
        answerB = "Gemini 8 dockte an die Agena-Zieldrohne an, aber ein Gemini-Triebwerk feuerte unkontrolliert und zwang zur Notlandung",
        answerC = "Gemini 10 dockte an zwei Agena-Zieldrohnen an, verlor aber die Orientierung im Orbit",
        answerD = "Gemini 9A vollendete das Andocken, aber der EVA-Astronaut konnte den Jetpack nicht anlegen",
        correctAnswer = 1,
        explanation = "Gemini 8 (16. März 1966) vollendete die erste Andockung im Orbit mit der Agena-Zieldrohne. Kurz darauf feuerte ein Gemini-Manövriertriebwerk ohne Befehl und verursachte eine gefährliche Taumelrotation (400°/Sekunde). Armstrong koppelte von Agena ab, was die Situation verschlimmerte (das fehlerhafte Triebwerk rotierte nun noch schneller). Durch Aktivierung des Wiedereintrittssystems stabilisierte er die Kapsel und landete nach nur 6 Stunden Flugzeit notmäßig.",
        difficulty = 4,
        funFact = "Astronaut David Scott sollte bei Gemini 8 einen Außenbordeinsatz durchführen – er kam nicht dazu. Als er drei Jahre später bei Apollo 15 auf dem Mond stand, war er der einzige Astronaut der ersten Mondlande-Ära, der nie eine EVA im Erdorbit absolviert hatte."
    ),

    // --- Apollo Specifics ---

    Question(
        categoryId = 3,
        questionText = "Das Apollo-Programm nutzte die 'Lunar Orbit Rendezvous' (LOR)-Methode statt 'Direct Ascent' oder 'Earth Orbit Rendezvous'. Wer setzte sich mit diesem Konzept gegen massive interne Widerstände durch?",
        answerA = "Wernher von Braun, der damit die Saturn V kleiner halten wollte",
        answerB = "John Houbolt vom Langley Research Center, dessen Idee zunächst ignoriert wurde",
        answerC = "George Mueller von der NASA-Zentrale, der das LOR-Konzept aus der Air Force übernahm",
        answerD = "Max Faget, der Apollo-Kapsel-Chefingenieur, der LOR als einzig sicheres Konzept berechnete",
        correctAnswer = 1,
        explanation = "John Houbolt (NASA Langley) kämpfte jahrelang für LOR – das Konzept, ein leichtes Fahrzeug nur vom Mondorbit aus landen zu lassen statt die gesamte Kapsel. Anfangs wurde er ignoriert oder belächelt; er schrieb einen ungewöhnlichen direkten Brief an NASA-Vize Seamans und setzte sich schließlich 1962 durch. LOR reduzierte die erforderliche Startmasse dramatisch und machte Apollo mit einer einzigen Saturn V möglich.",
        difficulty = 4,
        funFact = "Wernher von Braun bevorzugte zunächst die 'Earth Orbit Rendezvous'-Methode (mehrere Starts, Zusammenbau im Erdorbit), da ihm LOR zu riskant erschien – ein 'Rendezvous am Mond' war in seinen Augen wahnsinnig. Als er die Berechnungen akzeptierte, wurde er zum überzeugten LOR-Befürworter. Ohne diesen Konzeptschwenk wäre Apollo erst 1975 oder später möglich gewesen."
    ),

    Question(
        categoryId = 3,
        questionText = "Der Apollo-1-Brand (27. Januar 1967) kostete drei Astronauten das Leben. Welches spezifische technische Problem verursachte die rasche Ausbreitung des Feuers?",
        answerA = "Ein Kurzschluss in der Treibstoffleitung entzündete das Raketentreibmittel während Bodentests",
        answerB = "Reiner Sauerstoff unter Überdruck in der Kapsel kombiniert mit brennbaren Materialien und Kabelfehlern",
        answerC = "Ein fehlendes Kühlsystem überhitzte die Elektronik während des Bodendruckversuchs",
        answerD = "Der Hitzeschild versagte beim Bodentest, was zu unkontrollierter Erwärmung führte",
        correctAnswer = 1,
        explanation = "Die Apollo-1-Kapsel war mit reinem Sauerstoff unter 1,1 Atmosphären Druck gefüllt (Bodentestdruck) – 60% mehr als später im Orbit genutzt. In reiner O2-Atmosphäre unter Druck verbrennen Materialien, die normal schwer entflammbar sind, explosionsartig. Kombiniert mit defekten Kabeln und billigen, brennbaren Werkstoffen breitete sich das Feuer in Sekunden aus. Die Innentür öffnete nach innen (Drucktür) und konnte nicht geöffnet werden.",
        difficulty = 4,
        funFact = "Gus Grissom hatte vor dem Test gegenüber Kollegen geäußert: 'We're going to be burned alive in this thing' – er bezog sich auf die generellen Sicherheitsmängel. Das Gemini-Programm hatte deutlich strenger auf Sicherheit geachtet; bei Apollo hatte der Kostendruck zu Kompromissen geführt. Der Brand erzwang eine 20-monatige Redesignpause, die das Programm paradoxerweise rettete."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches war die genaue Bezeichnung der Mondlandefähre (LM) von Apollo 11, und wie lautete der Code-Name der Kommandokapsel?",
        answerA = "Mondlandefähre 'Eagle'; Kommandokapsel 'Columbia'",
        answerB = "Mondlandefähre 'Falcon'; Kommandokapsel 'Endeavour'",
        answerC = "Mondlandefähre 'Intrepid'; Kommandokapsel 'Yankee Clipper'",
        answerD = "Mondlandefähre 'Aquarius'; Kommandokapsel 'Odyssey'",
        correctAnswer = 0,
        explanation = "Apollo 11 flog mit der Mondlandefähre 'Eagle' (LM-5, Pilot: Edwin 'Buzz' Aldrin) und der Kommandokapsel 'Columbia' (Pilot: Michael Collins). Neils Armstrongs berühmtes 'The Eagle has landed' (20. Juli 1969, 20:17 UTC) bezog sich auf den Codenamen. Die Kombination 'Eagle/Columbia' ist historisch die bekannteste aller Apollo-Missionen.",
        difficulty = 4,
        funFact = "Michael Collins, der allein in der Columbia im Mondorbit blieb, wird oft als der 'vergessene Astronaut' bezeichnet. Bei jedem Mondumrundung war er für 48 Minuten von der Erde abgeschnitten – allein auf der Rückseite, während Milliarden Menschen auf Aldrin und Armstrong schauten. Er selbst beschrieb dieses Gefühl als 'serene isolation, not loneliness'."
    ),

    Question(
        categoryId = 3,
        questionText = "Apollo 13 (April 1970) scheiterte am Mondlandeziel durch die Explosion eines Sauerstofftanks. Welcher versteckte Fabrikationsfehler war die eigentliche Ursache?",
        answerA = "Ein NASA-Techniker hatte versehentlich Isolierung am Sauerstofftank beschädigt",
        answerB = "Ein Heizthermoschalter im Tank war für 28 Volt ausgelegt, aber mit 65 Volt betrieben worden, was ihn schweißte",
        answerC = "Der Tank war bei einem Falltest auf dem Boden beschädigt worden und nie ausgetauscht worden",
        answerD = "Schlechte Schweißnähte am Tank hatten einen Mikroriss erzeugt, der unter Druck versagte",
        correctAnswer = 1,
        explanation = "Der eigentliche Fehler lag beim Subunternehmer Beech Aircraft: Der Thermoschalter des Sauerstofftank-Heizelements war für 28 Volt spezifiziert, aber bei einem Bodentest 1968 versehentlich mit 65 Volt betrieben worden. Die überhöhte Spannung schweißte den Schalter zu – er konnte nicht mehr öffnen. Beim Entleeren des Tanks auf Apollo 13 überhitzte das Heizelement, entzündete Isolierung, und der Tank explodierte.",
        difficulty = 4,
        funFact = "Astronaut Ken Mattingly, ursprünglich für Apollo 13 geplant, wurde kurz vor dem Start wegen möglicher Masernexposition ausgetauscht – ohne je Masern zu bekommen. Jack Swigert flog statt seiner, überlebte die Katastrophe, und Mattingly flog later fehlerlos auf Apollo 16. Die Geschichte wurde von Ed Harris im Film verewigt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Apollo-Experiment hinterließ dauerhaft Retroreflektoren auf dem Mond, die bis heute für Laser-Entfernungsmessungen genutzt werden, und auf welchen Missionen wurden sie platziert?",
        answerA = "ALSEP-Seismographen auf Apollo 12, 14 und 16",
        answerB = "LRRR (Laser Ranging Retroreflector Arrays) auf Apollo 11, 14 und 15",
        answerC = "SWC (Solar Wind Composition) auf Apollo 11 und 12",
        answerD = "SNAP-27 Plutonium-Generatoren auf Apollo 12, 14, 15, 16 und 17",
        correctAnswer = 1,
        explanation = "Die Laser Ranging Retroreflector Arrays (LRRR) wurden auf Apollo 11 (Mare Tranquillitatis), Apollo 14 (Fra Mauro) und Apollo 15 (Hadley-Apennin) platziert. Laserimpulse von Observatorien auf der Erde werden reflektiert und ermöglichen die Messung der Mond-Erde-Distanz auf wenige Millimeter genau. Die Messungen beweisen, dass der Mond sich jährlich ca. 3,8 cm von der Erde entfernt.",
        difficulty = 4,
        funFact = "Die Retroreflektoren wurden von der Sowjetunion für ihre Luna-17 (1970) und Luna-21 (1973) Rover-Missionen ebenfalls abgelegt. Heute konkurrieren internationale Observatorien darum, wer die genauesten Monddistanz-Messungen liefern kann – ein friedliches wissenschaftliches Erbe des Kalten Krieges."
    ),

    Question(
        categoryId = 3,
        questionText = "Warum landete Neil Armstrong beim Apollo-11-Mondlandung ca. 6 km vom Zielpunkt entfernt, und welcher Fehler im Bordcomputer AGC löste den berühmten '1202 Alarm' aus?",
        answerA = "Mondstaub blockierte das Radar; der 1202-Alarm meldete Radarausfall",
        answerB = "Eintritt in die Atmosphäre der falschen Gezeiten; der 1202-Alarm signalisierte Navigationsfehler",
        answerC = "Computerspeicher-Überlastung (Executive Overflow) durch unbeabsichtigtes Aktivieren des Rendezvous-Radars; Armstrong steuerte manuell",
        answerD = "Triebwerksfehler beim Abstieg; der 1202-Alarm meldete Treibstoffmangel",
        correctAnswer = 2,
        explanation = "Der 1202-Alarm ('Executive Overflow') meldete, dass der AGC-Computer mit zu vielen gleichzeitigen Aufgaben überlastet war – weil das Rendezvous-Radar irrtümlich aktiv blieb und Daten lieferte. MIT-Ingenieur Jack Garman (22 Jahre alt) hatte diesen Fehlercode auf einer handgeschriebenen Liste als unkritisch eingestuft und riet zum Weiterfliegen. Armstrong übernahm manuell, weil der Zielpunkt ein felsiges Kraterfeld war – daher die 6 km Abweichung.",
        difficulty = 4,
        funFact = "Margaret Hamilton, Leiterin der AGC-Software bei MIT, hatte bestanden, dass der Computer Aufgaben nach Priorität abbricht statt zu crashen (Executive Overflow statt Totalausfall). Dieser Designentscheid – damals umstritten, weil er Entwicklungszeit kostete – rettete buchstäblich die Mondlandung. Hamilton gilt als Pionierin des Begriffs 'Software Engineering'."
    ),

    Question(
        categoryId = 3,
        questionText = "Das Apollo-Programm endete mit Apollo 17 im Dezember 1972. Welcher Astronaut war der einzige ausgebildete Geologe, der den Mond betrat, und welche wissenschaftliche Entdeckung trug sein Name?",
        answerA = "Edgar Mitchell (Apollo 14); entdeckte Anorthosit-Krater nahe Fra Mauro",
        answerB = "Harrison 'Jack' Schmitt (Apollo 17); entdeckte als erster 'orangefarbene Erde' (Pyroclastic Orange Soil)",
        answerC = "Charles Conrad (Apollo 12); entdeckte anhand Surveyor-3-Materialien die UV-Beständigkeit",
        answerD = "Alan Bean (Apollo 12); klassifizierte als erster die KREEP-Gesteine im Oceanus Procellarum",
        correctAnswer = 1,
        explanation = "Harrison 'Jack' Schmitt (Geologe, als erster Wissenschafts-Astronaut auf dem Mond) entdeckte in der Taurus-Littrow-Senke (Apollo 17) den 'Orangen Boden' bei Station 4 (Shorty Crater) – pyroclastisches Glas aus einem vulkanischen Ausbruch vor ca. 3,5 Milliarden Jahren. Diese Entdeckung war wissenschaftlich außergewöhnlich und bewies vulkanische Aktivität in bestimmten Mondregionen.",
        difficulty = 4,
        funFact = "Schmitts Kommandant Eugene Cernan, der Apollo 17 führte, war der letzte Mensch, der den Mond betrat. Bevor er in die Fähre stieg, ritzte er die Initialen seiner Tochter Tracy in den Mondstaub. Da es auf dem Mond keine Erosion gibt, könnten diese Initialen theoretisch Millionen Jahre bestehen."
    ),

    // --- Space Race: Soviet Details ---

    Question(
        categoryId = 3,
        questionText = "Sergei Koroljow, der 'Chief Designer' des sowjetischen Raumfahrtprogramms, blieb zu Lebzeiten anonym. Warum wurde seine Identität geheimgehalten?",
        answerA = "Er war nach dem Gulag psychisch instabil und konnte nicht öffentlich auftreten",
        answerB = "Die Sowjetunion wollte verhindern, dass westliche Geheimdienste ihn attentatsmäßig ausschalten",
        answerC = "Er war technisch zu wertvoll und galt als nationaler Sicherheits-Asset der höchsten Kategorie",
        answerD = "Koroljow weigerte sich selbst, öffentlich bekannt zu sein, da er die Kollektivarbeit betonen wollte",
        correctAnswer = 1,
        explanation = "Die offizielle sowjetische Begründung war die Sorge vor westlichen Attentatsversuchen – Koroljow galt als zu wertvoll, um ihn zu exponieren. Khrushchev selbst bestätigte, dass westliche Geheimdienste ihn töten würden, wenn sie seine Identität kennen. Tatsächlich diente die Anonymität auch dazu, keinen einzelnen Erfolgs-Helden herauszustellen und das kollektive System zu betonen.",
        difficulty = 4,
        funFact = "Koroljow hatte von 1938 bis 1944 im Gulag gesessen – Stalins Säuberungen hatten ihn fast umgebracht. Er verlor alle Zähne durch Misshandlungen und litt lebenslang an Herzproblemen. Ausgerechnet dieser Überlebende des stalinistischen Terrors baute den Sputnik und trieb das Weltraumprogramm voran, das Stalins Nachfolger Ruhm einbringen sollte."
    ),

    Question(
        categoryId = 3,
        questionText = "Die sowjetische Mondrakete N1 scheiterte an allen vier Starts (1969–1972). Was war das grundlegende technische Problem?",
        answerA = "Die NK-15-Triebwerke waren zu schwach; die Sowjets hatten kein äquivalent zur Amerikanischen F-1",
        answerB = "Zu viele Triebwerke (30 in der ersten Stufe) erzeugten Resonanzvibrationen und Steuerungsprobleme; computergestützte Kontrolle fehlte",
        answerC = "Fehlende Tieftemperatur-Technologie für flüssigen Wasserstoff verhinderte ausreichende Treibstoffeffizienz",
        answerD = "Koroljows Tod 1966 führte zu Rivalitätskämpfen; das N1-Design war nie fertig entwickelt worden",
        correctAnswer = 1,
        explanation = "Das N1 nutzte 30 NK-15-Triebwerke in der ersten Stufe (statt eines oder weniger sehr großer wie Saturn V's F-1). Dies erzeugte extrem komplexe Steuerungsanforderungen und Vibrationsresonanz. Ein Feuer in einer Triebwerksleitung koppelte sich auf andere Triebwerke, da kein digitales Steuerungssystem existierte. Der zweite N1-Start (Juli 1969) erzeugte die größte nicht-nukleare Explosion in der Geschichte der Raumfahrt.",
        difficulty = 4,
        funFact = "Die N1-Katastrophe von Juli 1969 (8 Tage vor Apollo 11) war so verheerend, dass die gesamte Startanlage zerstört wurde. Die Sowjetunion gestand dieses Scheitern jahrzehntelang nicht ein – das Mondlandungsprogramm war offiziell 'nie geplant' gewesen. Erst 1989 gab Gorbatschows glasnost die Wahrheit frei."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Zond'-Programm (1968), und welche historische Chance verpasste die Sowjetunion damit?",
        answerA = "Zond war das bemannte Mondorbit-Programm; Zond 5 hätte als erstes bemanntes Fahrzeug den Mond umrundet, wurde aber unbemannt geflogen",
        answerB = "Zond waren Venussonden; der erste Venusorbit wäre ohne technische Fehlplanung möglich gewesen",
        answerC = "Zond war ein Marsprogramm; Mars 3 hätte als erste bemanntes Fahrzeug Mars umrundet",
        answerD = "Zond war ein militärisches Aufklärungssatellitenprogramm, das durch Apollo 8 überflüssig wurde",
        correctAnswer = 0,
        explanation = "Zond 5 (September 1968) umrundete den Mond unbemannt und kehrte sicher zur Erde zurück – die Technologie für eine bemannte Mondumrundung war damit prinzipiell vorhanden. Sowjetische Kosmonauten (besonders Alexei Leonow) drängten auf einen bemannten Zond-Flug vor Apollo 8 (Dezember 1968). Technische Probleme (zu hohe G-Kräfte beim Wiedereintritt) und Rivalitäten im Konstruktionsbüro verhinderten dies. Apollo 8 holte dann die erste bemannte Mondumrundung.",
        difficulty = 4,
        funFact = "Wäre Zond 5 bemannt geflogen und sicher zurückgekehrt, hätten sowjetische Kosmonauten als erste Menschen den Mond umrundet – ein historischer Triumph, der Apollo 8 ins Vergessen gestürzt hätte. Die Konkurrenz zwischen den Konstruktionsbüros Koroljow (RSC Energia) und Chelomei (der Zond-Macher) kostete die Sowjetunion ihren letzten echten Chancenmoment im Mondwettlauf."
    ),

    // --- GATT Uruguay Round ---

    Question(
        categoryId = 3,
        questionText = "Nach wie vielen Verhandlungsrunden wurde das GATT durch die WTO abgelöst, und wie hieß die finale Runde?",
        answerA = "Nach 6 Runden; die Kennedy-Runde (1964–1967) war die letzte",
        answerB = "Nach 8 Runden; die Uruguay-Runde (1986–1994) war die letzte",
        answerC = "Nach 10 Runden; die Tokio-Runde (1973–1979) war die letzte reguläre",
        answerD = "Nach 5 Runden; die Dillon-Runde (1960–1962) war die letzte",
        correctAnswer = 1,
        explanation = "Das GATT durchlief acht Verhandlungsrunden: Genf (1947), Annecy (1949), Torquay (1951), Genf (1956), Dillon-Runde (1960–62), Kennedy-Runde (1964–67), Tokio-Runde (1973–79) und Uruguay-Runde (1986–94). Die Uruguay-Runde schuf die WTO (Welthandelsorganisation, 1. Januar 1995), erweiterte das Regelwerk auf Dienstleistungen (GATS) und geistiges Eigentum (TRIPS).",
        difficulty = 4,
        funFact = "Die Uruguay-Runde dauerte 7,5 Jahre – fast doppelt so lang wie geplant – und endete erst nach einer dramatischen Endverhandlung in Genf im Dezember 1993. Die USA und EU stritten monatelang über Agrarsubventionen ('Blair House Agreement'), was fast zum Scheitern der gesamten Runde führte."
    ),

    // --- Stasi foreign intelligence ---

    Question(
        categoryId = 3,
        questionText = "Die HVA (Hauptverwaltung Aufklärung) war die Auslandsgeheimdienstsparte der Stasi. Ihr legendärer Chef leitete sie von 1952 bis 1986. Wie hieß er, und was war sein bekanntester Erfolg?",
        answerA = "Erich Mielke; Anwerbung von NATO-Generalstabsoffizieren in Brüssel",
        answerB = "Markus Wolf; Platzierung von Günter Guillaume als persönlichem Referenten von Bundeskanzler Brandt",
        answerC = "Werner Großmann; Infiltration des BND durch den 'Topas'-Agenten",
        answerD = "Heinz Felfe; doppelte Mitgliedschaft im BND und KGB gleichzeitig",
        correctAnswer = 1,
        explanation = "Markus Wolf ('Mann ohne Gesicht' – westliche Geheimdienste kannten sein Gesicht jahrzehntelang nicht) leitete die HVA von 1952 bis 1986. Sein größter Coup: Günter Guillaume, der bis zum persönlichen Referenten von Bundeskanzler Willy Brandt aufstieg. Als Guillaume 1974 enttarnt wurde, trat Brandt zurück. Wolf galt als einer der effektivsten Geheimdienstchefs des 20. Jahrhunderts.",
        difficulty = 4,
        funFact = "Markus Wolf lebte nach 1989 zunächst in Moskau im Exil, kehrte aber zurück und wurde in Deutschland verurteilt – ein Urteil, das der Bundesgerichtshof später teilweise aufhob, da er für einen souveränen Staat gearbeitet hatte. Er schrieb Memoiren, kochte auf hohem Niveau (sein zweites Buch war ein Kochbuch) und starb 2006 in Berlin."
    ),

    // --- Weimar: Article 25 ---

    Question(
        categoryId = 3,
        questionText = "Artikel 25 der Weimarer Verfassung erlaubte dem Reichspräsidenten, den Reichstag aufzulösen. Welche Einschränkung enthielt er, und wie oft wurde er zwischen 1930 und 1932 angewendet?",
        answerA = "Auflösung nur mit Zustimmung des Reichsrats; dreimal angewendet",
        answerB = "Auflösung nur einmal aus demselben Anlass erlaubt; viermal genutzt (1930, 1931, zweimal 1932)",
        answerC = "Auflösung mit anschließender Neuwahl innerhalb 60 Tagen; dreimal angewendet (1930, zweimal 1932)",
        answerD = "Auflösung nur möglich, wenn der Reichstag einen Misstrauensantrag gegen den Kanzler gestellt hatte",
        correctAnswer = 2,
        explanation = "Artikel 25 erlaubte dem Reichspräsidenten die Auflösung des Reichstags, jedoch nur einmal aus demselben Anlass, und schrieb Neuwahlen innerhalb von 60 Tagen vor. Hindenburg löste den Reichstag auf: September 1930 (nach dem Sturz des Kabinetts Müller), Juni 1932 (nach dem Scheitern des Kabinetts Brüning) und September 1932 (nach dem Misstrauensvotum gegen Papen). Dreimal in zweieinhalb Jahren – ein Zeichen der Regierungsunfähigkeit.",
        difficulty = 4,
        funFact = "Die häufigen Reichstagsauflösungen kosteten pro Wahlkampagne Millionen Reichsmark und erschöpften die demokratischen Parteien finanziell, während NSDAP und KPD von Großspendern und Sowjetgeldern profitieren konnten. Die Wahlkampfmaschinerie war selbst ein Faktor im Niedergang der Weimarer Republik."
    ),

)
