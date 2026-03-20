package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsExpert3(): List<Question> = listOf(

    // --- WIRTSCHAFTSNOBELPREIS & ÖKONOMEN (20) ---

    Question(
        categoryId = 11,
        questionText = "Wer erhielt 1969 den allerersten Wirtschaftsnobelpreis, gemeinsam mit Jan Tinbergen?",
        answerA = "Paul Samuelson",
        answerB = "Milton Friedman",
        answerC = "Ragnar Frisch",
        answerD = "Kenneth Arrow",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Der erste Alfred-Nobel-Gedächtnispreis für Wirtschaftswissenschaften ging 1969 an Ragnar Frisch (Norwegen) und Jan Tinbergen (Niederlande) für die Entwicklung und Anwendung dynamischer Modelle zur Analyse wirtschaftlicher Prozesse.",
        funFact = "Ragnar Frisch prägte den Begriff 'Ökonometrie' — die Verbindung von Mathematik, Statistik und Wirtschaftstheorie."
    ),

    Question(
        categoryId = 11,
        questionText = "Milton Friedman erhielt 1976 den Wirtschaftsnobelpreis. Mit welchem geldpolitischen Konzept ist er vor allem verbunden?",
        answerA = "Keynesianismus",
        answerB = "Monetarismus",
        answerC = "Institutionalismus",
        answerD = "Neoklassik",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Milton Friedman begründete den Monetarismus: Die Geldmenge ist der zentrale Steuerungshebel der Wirtschaft. Er kritisierte staatliche Eingriffe scharf und prägte den Satz: 'Inflation ist immer und überall ein monetäres Phänomen.'",
        funFact = "Friedman beriet in den 1970er Jahren die chilenische Militärjunta unter Pinochet — ein bis heute umstrittenes Kapitel seiner Biografie."
    ),

    Question(
        categoryId = 11,
        questionText = "John Forbes Nash Jr. erhielt 1994 den Wirtschaftsnobelpreis für seine Arbeit zur Spieltheorie. Was ist das 'Nash-Gleichgewicht'?",
        answerA = "Ein Zustand, in dem kein Spieler durch einseitigen Strategiewechsel besser gestellt werden kann",
        answerB = "Der optimale Ausgang für alle Beteiligten in einem Spiel",
        answerC = "Die Strategie, bei der alle Spieler kooperieren",
        answerD = "Ein Marktgleichgewicht ohne Staatseingriff",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Das Nash-Gleichgewicht beschreibt einen strategischen Zustand, bei dem kein Spieler seine Auszahlung verbessern kann, indem er allein seine Strategie ändert, während alle anderen ihre Strategie beibehalten.",
        funFact = "Nashs Leben wurde im Film 'A Beautiful Mind' (2001) verfilmt. Er litt unter Schizophrenie und erholte sich nach Jahrzehnten überraschend davon."
    ),

    Question(
        categoryId = 11,
        questionText = "Daniel Kahneman erhielt 2002 den Wirtschaftsnobelpreis gemeinsam mit Vernon Smith. Was ist der Kerngedanke seiner 'Prospect Theory'?",
        answerA = "Menschen maximieren immer ihren rationalen Nutzen",
        answerB = "Verluste werden psychologisch stärker gewichtet als gleichgroße Gewinne",
        answerC = "Risikoaversion ist bei allen Menschen gleich stark ausgeprägt",
        answerD = "Märkte sind grundsätzlich effizient",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Kahneman und Amos Tversky entwickelten die Prospect Theory: Menschen empfinden Verluste etwa doppelt so stark wie gleichgroße Gewinne (Verlustaversion). Dies widerspricht dem Modell des vollständig rationalen 'Homo Oeconomicus'.",
        funFact = "Kahneman ist Psychologe, kein Ökonom — er ist einer der wenigen Nobelpreisträger in Wirtschaft ohne wirtschaftswissenschaftlichen Hintergrund."
    ),

    Question(
        categoryId = 11,
        questionText = "Joseph Stiglitz erhielt 2001 den Wirtschaftsnobelpreis zusammen mit Akerlof und Spence. Für welches Konzept?",
        answerA = "Analyse asymmetrischer Information auf Märkten",
        answerB = "Spieltheorie",
        answerC = "Wachstumstheorie",
        answerD = "Geldpolitik bei Inflation",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Akerlof, Spence und Stiglitz wurden für ihre Analyse von Märkten mit asymmetrischer Information ausgezeichnet. Akerlofs berühmtes 'Market for Lemons'-Papier zeigt, wie Informationsasymmetrie Märkte zum Zusammenbruch bringen kann.",
        funFact = "George Akerlofs 'The Market for Lemons' (1970) wurde zunächst von drei Fachzeitschriften abgelehnt — bevor es zum meistzitierten Wirtschaftsartikel des 20. Jahrhunderts wurde."
    ),

    Question(
        categoryId = 11,
        questionText = "Friedrich August von Hayek erhielt 1974 den Wirtschaftsnobelpreis. Was ist sein bekanntestes wirtschaftspolitisches Werk?",
        answerA = "Das Kapital",
        answerB = "Allgemeine Theorie der Beschäftigung",
        answerC = "Der Weg zur Knechtschaft",
        answerD = "Die Wohlstandsnationen",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Hayeks 'Der Weg zur Knechtschaft' (1944) argumentiert, dass staatliche Zentralplanung zwangsläufig zur politischen Unterdrückung führt. Das Buch war ein direkter Angriff auf den Sozialismus und machte Hayek weltberühmt.",
        funFact = "Hayek und Keynes waren befreundet, obwohl sie entgegengesetzte Wirtschaftstheorien vertraten. Ihr Briefwechsel gilt als faszinierendes intellektuelles Duell."
    ),

    Question(
        categoryId = 11,
        questionText = "Paul Krugman erhielt 2008 den Wirtschaftsnobelpreis. Für welches Forschungsgebiet wurde er ausgezeichnet?",
        answerA = "Verhaltensökonomik und Entscheidungstheorie",
        answerB = "Analyse von Finanzblasen",
        answerC = "Neue Außenhandelstheorie und Wirtschaftsgeographie",
        answerD = "Theorie des optimalen Währungsraums",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Krugman wurde für seine Neue Außenhandelstheorie geehrt: Er erklärte, warum Länder mit ähnlicher Wirtschaftsstruktur dennoch intensiv handeln — wegen Skaleneffekten und Produktvielfalt. Er entwickelte auch die 'Neue Wirtschaftsgeographie'.",
        funFact = "Krugman erhielt den Nobelpreis ausgerechnet im Jahr der großen Finanzkrise — sein öffentlicher Kommentar: 'Das Timing hätte besser nicht sein können.'"
    ),

    Question(
        categoryId = 11,
        questionText = "Ben Bernanke erhielt 2022 den Wirtschaftsnobelpreis. Was war sein wichtigster Forschungsbeitrag?",
        answerA = "Analyse von Banken und Finanzkrisen",
        answerB = "Effizienzlohntheorie",
        answerC = "Optimale Währungsraumtheorie",
        answerD = "Theorie der rationalen Erwartungen",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Bernanke, Diamond und Dybvig erhielten den Preis für ihre Forschung zu Banken und Finanzkrisen. Bernanke zeigte, dass Bankenzusammenbrüche die Weltwirtschaftskrise 1929 massiv verschlimmerten — Erkenntnisse, die er als Fed-Chef 2008 direkt anwandte.",
        funFact = "Bernanke war während der Finanzkrise 2008 gleichzeitig Notenbank-Chef der USA — er lebte sein Forschungsthema buchstäblich in der Praxis."
    ),

    Question(
        categoryId = 11,
        questionText = "Adam Smith gilt als Begründer der modernen Wirtschaftswissenschaft. In welchem Werk beschrieb er das Konzept der 'unsichtbaren Hand'?",
        answerA = "Das Kapital",
        answerB = "Principien der Volkswirtschaft",
        answerC = "Der Wohlstand der Nationen",
        answerD = "Theorie der moralischen Gefühle",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "In 'An Inquiry into the Nature and Causes of the Wealth of Nations' (1776) beschreibt Smith die 'unsichtbare Hand': Durch die Verfolgung eigener Interessen fördert jeder Einzelne unbeabsichtigt das Wohl der Gesellschaft.",
        funFact = "Adam Smith war ursprünglich Moralphilosoph, kein Wirtschaftswissenschaftler. Sein früheres Werk 'Theorie der moralischen Gefühle' gilt als ebenso bedeutend wie der Wohlstand der Nationen."
    ),

    Question(
        categoryId = 11,
        questionText = "John Maynard Keynes revolutionierte die Wirtschaftstheorie. In welchem Jahr erschien sein Hauptwerk 'Allgemeine Theorie der Beschäftigung, des Zinses und des Geldes'?",
        answerA = "1919",
        answerB = "1929",
        answerC = "1936",
        answerD = "1944",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Keynes' 'General Theory' erschien 1936, mitten in der Weltwirtschaftskrise. Er argumentierte, dass staatliche Nachfragestimulierung (Deficit Spending) Arbeitslosigkeit bekämpfen kann — ein Paradigmenwechsel gegenüber der klassischen Theorie.",
        funFact = "Keynes war auch ein äußerst erfolgreicher Börsenspekulant und verwaltete das Stiftungsvermögen seines College in Cambridge — mit beeindruckendem Erfolg."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht man unter dem 'Coase-Theorem', für das Ronald Coase 1991 den Wirtschaftsnobelpreis erhielt?",
        answerA = "Bei klar definierten Eigentumsrechten und ohne Transaktionskosten können private Verhandlungen externe Effekte optimal lösen",
        answerB = "Staatseingriffe sind immer effizienter als Marktlösungen",
        answerC = "Monopole entstehen zwangsläufig ohne staatliche Regulierung",
        answerD = "Arbeitslosigkeit ist stets durch Lohnrigiditäten verursacht",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Das Coase-Theorem besagt: Wenn Eigentumsrechte klar definiert sind und Transaktionskosten vernachlässigbar sind, werden externe Effekte (z.B. Umweltverschmutzung) durch private Verhandlungen effizient gelöst — unabhängig von der Ausgangszuordnung der Rechte.",
        funFact = "Coase wurde erst mit 80 Jahren für ein Papier aus dem Jahr 1960 mit dem Nobelpreis geehrt — eine der längsten Wartezeiten in der Geschichte des Preises."
    ),

    Question(
        categoryId = 11,
        questionText = "Elinor Ostrom gewann 2009 als erste Frau den Wirtschaftsnobelpreis. Für welche Erkenntnis?",
        answerA = "Frauen sind bessere Anlegerinnen als Männer",
        answerB = "Gemeingüter können ohne Privatisierung oder Staatskontrolle effizient verwaltet werden",
        answerC = "Armut ist primär durch mangelnde Bildung bedingt",
        answerD = "Steuerwettbewerb zwischen Staaten ist wohlfahrtsfördernd",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Ostrom widerlegte die 'Tragödie der Allmende': Gemeinschaften können gemeinsam genutzte Ressourcen (Fischgründe, Wälder) ohne Privatisierung oder staatliche Kontrolle nachhaltig verwalten, wenn klare Regeln und Selbstverwaltung existieren.",
        funFact = "Ostrom war Politikwissenschaftlerin, keine Ökonomin — ihr Preis löste eine Kontroverse aus, ob der Wirtschaftsnobelpreis zu weit ausgedehnt wird."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt das 'Prisoner's Dilemma' (Gefangenendilemma) in der Spieltheorie?",
        answerA = "Wie Strafverfolgungsbehörden Kartelle aufdecken",
        answerB = "Eine Situation, wo individuell rationales Verhalten zu kollektiv suboptimalen Ergebnissen führt",
        answerC = "Warum Gefängnisstrafen abschreckend wirken",
        answerD = "Die Theorie der Wiedereingliederung von Straftätern",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Im Gefangenendilemma können zwei Spieler durch Kooperation ein besseres Ergebnis erzielen, aber die dominante Strategie ist für jeden Einzelnen der Verrat — das Ergebnis ist schlechter für alle. Dieses Prinzip erklärt viele reale Dilemmata wie Rüstungswettläufe.",
        funFact = "Das Gefangenendilemma wurde 1950 von Merrill Flood und Melvin Dresher bei der RAND Corporation entwickelt — einer Denkfabrik, die strategische Nuklearkriegsführung analysierte."
    ),

    Question(
        categoryId = 11,
        questionText = "Robert Schiller erhielt 2013 den Wirtschaftsnobelpreis. Er ist bekannt für den 'CAPE-Ratio' zur Aktienbewertung. Wofür steht CAPE?",
        answerA = "Capital Asset Price Estimation",
        answerB = "Cyclically Adjusted Price-to-Earnings Ratio",
        answerC = "Current Asset Portfolio Evaluation",
        answerD = "Compound Annual Price Earnings",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "CAPE (Cyclically Adjusted Price-to-Earnings Ratio) ist Shillers Bewertungsmaßstab: Der Aktienkurs geteilt durch den inflationsbereinigten Durchschnittsgewinn der letzten 10 Jahre. Ein hoher CAPE deutet auf Überbewertung hin — Shiller warnte damit vor dem Dot-com-Crash 1999.",
        funFact = "Shiller prognostizierte sowohl den Dot-com-Crash 2000 als auch die Immobilienkrise 2007 öffentlich — und wurde beide Male zunächst belächelt."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Wirtschaftsprinzip beschreibt das 'Pareto-Optimum'?",
        answerA = "Ein Zustand maximaler Gleichheit der Einkommensverteilung",
        answerB = "Ein Zustand, in dem niemand besser gestellt werden kann, ohne jemand anderen schlechter zu stellen",
        answerC = "Das Prinzip der maximalen Staatsintervention",
        answerD = "Die optimale Inflationsrate einer Volkswirtschaft",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Ein Pareto-optimaler Zustand liegt vor, wenn keine Umverteilung möglich ist, die mindestens eine Person besser stellt, ohne eine andere schlechter zu stellen. Benannt nach Vilfredo Pareto, der auch die 80/20-Regel (Pareto-Prinzip) formulierte.",
        funFact = "Pareto entdeckte die 80/20-Regel ursprünglich in seinem Garten: 80% der Erbsen wuchsen in 20% der Schoten. Er übertrug dieses Prinzip auf die Vermögensverteilung."
    ),

    // --- WIRTSCHAFTSGESCHICHTE & INSTITUTIONEN (15) ---

    Question(
        categoryId = 11,
        questionText = "Das System von Bretton Woods (1944) verband alle Währungen an den US-Dollar. Wie wurde der Dollar selbst abgesichert?",
        answerA = "Durch europäische Goldreserven",
        answerB = "Durch den Internationalen Währungsfonds",
        answerC = "Durch Bindung an Gold zum Kurs von 35 Dollar pro Unze",
        answerD = "Durch amerikanische Staatsanleihen",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Im Bretton-Woods-System war der US-Dollar an Gold gebunden (35 USD pro Unze), alle anderen Währungen hingen am Dollar. Das System schuf Währungsstabilität nach dem Zweiten Weltkrieg, brach aber 1971/73 zusammen.",
        funFact = "Die Konferenz in Bretton Woods, New Hampshire, dauerte nur drei Wochen (1.-22. Juli 1944). In dieser kurzen Zeit wurde die gesamte Nachkriegs-Weltfinanzordnung entworfen."
    ),

    Question(
        categoryId = 11,
        questionText = "US-Präsident Nixon beendete 1971 die Goldkonvertibilität des Dollars. Wie wird dieses Ereignis genannt?",
        answerA = "Nixon-Schock",
        answerB = "Dollar-Krise",
        answerC = "Bretton-Woods-Kollaps",
        answerD = "Der Schwarze Montag",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Am 15. August 1971 verkündete Nixon das Ende der Goldkonvertibilität des Dollars — der 'Nixon-Schock'. Dies beendete faktisch das Bretton-Woods-System und führte zur Ära flexibler Wechselkurse, die bis heute gilt.",
        funFact = "Nixon traf diese Entscheidung an einem Sonntagabend ohne Rücksprache mit den Verbündeten — Europa und Japan erfuhren es erst aus dem Fernsehen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt die 'Große Depression' (Weltwirtschaftskrise) der 1930er Jahre besonders treffend?",
        answerA = "Eine lokale US-Bankenkrise ohne internationale Auswirkungen",
        answerB = "Eine europäische Inflationskrise ähnlich der deutschen Hyperinflation",
        answerC = "Eine kurze Rezession die durch Goldstandard-Aufgabe schnell überwunden wurde",
        answerD = "Den schlimmsten weltweiten Wirtschaftsabschwung des 20. Jahrhunderts mit bis zu 25% Arbeitslosigkeit in den USA",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Die Große Depression begann mit dem Börsencrash 1929 (Schwarzer Donnerstag). In den USA stieg die Arbeitslosigkeit auf 25%, das BIP fiel um 30%. Die Krise dauerte bis in die späten 1930er Jahre und veränderte die Wirtschaftspolitik weltweit.",
        funFact = "In Deutschland war die Große Depression ein entscheidender Faktor für den Aufstieg der NSDAP — Massenarbeitslosigkeit und Armut trieben verzweifelte Menschen in extreme Parteien."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Institution entstand 1944 in Bretton Woods als 'Kreditgeber der letzten Instanz' für verschuldete Länder?",
        answerA = "Weltbank",
        answerB = "OECD",
        answerC = "Bank für Internationalen Zahlungsausgleich",
        answerD = "Internationaler Währungsfonds (IWF)",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Der IWF wurde 1944 in Bretton Woods gegründet und nahm 1945 die Arbeit auf. Er soll Länder mit Zahlungsbilanzschwierigkeiten mit Krediten unterstützen und globale Währungsstabilität fördern.",
        funFact = "Der IWF-Hauptsitz liegt in Washington D.C., während die Weltbank — ebenfalls in Washington — von einem Amerikaner geleitet wird. IWF-Chef ist traditionell immer ein Europäer — eine informelle Vereinbarung seit 1945."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war die direkte Ursache für den Zusammenbruch der US-Investmentbank Lehman Brothers am 15. September 2008?",
        answerA = "Betrugsvorwürfe gegen den CEO",
        answerB = "Staatliche Enteignung im Zuge der Verstaatlichungspolitik",
        answerC = "Übermäßiges Engagement in hypothekarisch gesicherten Wertpapieren (MBS) bei kollabierendem Immobilienmarkt",
        answerD = "Devisenverluste durch Wechselkursrisiken",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Lehman Brothers hatte massiv in Mortgage-Backed Securities (hypothekarisch gesicherte Wertpapiere) investiert. Als der US-Immobilienmarkt 2007-2008 kollabierte, wurden diese Papiere wertlos. Lehman meldete Insolvenz an — der größte Bankrott in der US-Geschichte.",
        funFact = "Lehman Brothers war 1850 als Baumwollhandelsunternehmen in Alabama gegründet worden — die Bank überstand den Bürgerkrieg, zwei Weltkriege und die Große Depression, aber nicht die Subprime-Krise."
    ),

    Question(
        categoryId = 11,
        questionText = "Was sind 'Subprime-Kredite', die im Zentrum der Finanzkrise 2007/2008 standen?",
        answerA = "Staatsanleihen mit geringem Zinssatz",
        answerB = "Europäische Unternehmensanleihen mit Rating unter BBB",
        answerC = "Kurzfristige Interbankenkredite mit Risiko",
        answerD = "Immobilienkredite an Kreditnehmer mit geringer Bonität ohne ausreichende Absicherung",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Subprime-Kredite wurden an US-Hausbesitzer mit schlechter Kreditwürdigkeit vergeben (teils als 'NINJA-Loans': No Income, No Job, No Assets). Diese wurden zu Wertpapieren (CDOs) gebündelt und weltweit verkauft — als die Hauseigentümer ausfielen, kollabierte das System.",
        funFact = "Ratingagenturen wie Moody's und S&P vergaben AAA-Ratings (höchste Sicherheitsstufe) an viele dieser toxischen Wertpapiere — ein fundamentales Versagen der Finanzmarktaufsicht."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches wirtschaftliche Phänomen beschreibt die 'Stagflation', die in den 1970er Jahren auftrat?",
        answerA = "Hohe Inflation bei gleichzeitigem Wirtschaftswachstum",
        answerB = "Deflation bei gleichzeitiger Stagnation",
        answerC = "Sinkende Preise bei steigender Produktion",
        answerD = "Gleichzeitiges Auftreten von wirtschaftlicher Stagnation (hohe Arbeitslosigkeit) und Inflation",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Die Stagflation der 1970er, ausgelöst durch den Ölpreisschock 1973, widersprach dem damaligen Konsens: Die Phillips-Kurve sagte vor, Inflation und Arbeitslosigkeit seien gegenläufig — Stagflation zeigte, dass beide gleichzeitig hoch sein können.",
        funFact = "Der Begriff 'Stagflation' soll vom britischen Politiker Iain Macleod 1965 geprägt worden sein — eine Kombination aus 'Stagnation' und 'Inflation'."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt das 'Triffin-Dilemma' in der Internationalen Währungspolitik?",
        answerA = "Der Konflikt zwischen Geldmengenwachstum und Inflation",
        answerB = "Der Widerspruch, dass eine Reservewährung gleichzeitig Handelsbilanzdefizite und Vertrauen erfordert",
        answerC = "Die Unmöglichkeit fixer und flexibler Wechselkurse gleichzeitig",
        answerD = "Der Konflikt zwischen nationaler Geldpolitik und Währungsunionen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Das Triffin-Dilemma (benannt nach Robert Triffin, 1960) beschreibt: Das Land, dessen Währung Weltreservewährung ist, muss Leistungsbilanzdefizite laufen lassen, um die Welt mit Liquidität zu versorgen — das untergräbt langfristig das Vertrauen in diese Währung.",
        funFact = "Triffin sagte das Ende des Bretton-Woods-Systems 1960 akkurat voraus — 11 Jahre bevor es 1971 tatsächlich kollabierte. Er wurde damals kaum ernst genommen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist die 'Theorie der komparativen Kostenvorteile' (David Ricardo, 1817)?",
        answerA = "Freihandel schadet stets dem schwächeren Handelspartner",
        answerB = "Zölle sind der effizienteste Weg zur Industrieförderung",
        answerC = "Länder profitieren vom Handel, indem sie sich auf das spezialisieren, was sie relativ kostengünstiger produzieren",
        answerD = "Länder sollen nur exportieren, was sie absolut billiger produzieren",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Ricardo zeigte: Selbst wenn ein Land alles absolut billiger produziert, lohnt sich Handel. Jedes Land sollte sich auf das Gut spezialisieren, bei dem es den relativ größten Vorteil hat (oder den relativ kleinsten Nachteil).",
        funFact = "Ricardo illustrierte sein Konzept am Beispiel von England (Tuch) und Portugal (Wein). Er machte dabei außen vor, dass Portugal tatsächlich beides billiger produzieren konnte."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht man unter dem 'Multiplikator-Effekt' in der keynesianischen Wirtschaftstheorie?",
        answerA = "Die Vervielfachung von Produktionskosten bei Massenproduktion",
        answerB = "Die Wirkung von Zöllen auf den Außenhandel",
        answerC = "Der Zinssatz multipliziert die Inflationsrate",
        answerD = "Eine staatliche Ausgabe löst eine Kette von Ausgaben aus, die das BIP um ein Vielfaches des ursprünglichen Betrags steigert",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Der Multiplikator beschreibt: Wenn der Staat 1 Milliarde Euro ausgibt, bekommt ein Unternehmen das Geld und gibt einen Teil davon aus, das nächste Unternehmen gibt wieder einen Teil aus usw. — das BIP steigt um ein Vielfaches der ursprünglichen Ausgabe.",
        funFact = "Die Theorie des Multiplikators wurde ursprünglich von Richard Kahn (1931) entwickelt, Keynes' Schüler — Keynes übernahm und verallgemeinerte sie in seiner 'Allgemeinen Theorie'."
    ),

    // --- WIRTSCHAFTSTHEORIE & KONZEPTE (15) ---

    Question(
        categoryId = 11,
        questionText = "Was beschreibt die 'Philips-Kurve' in der Makroökonomie?",
        answerA = "Den positiven Zusammenhang zwischen Zinssatz und Investitionen",
        answerB = "Die Beziehung zwischen Staatsschulden und Wirtschaftswachstum",
        answerC = "Den Zusammenhang zwischen Wechselkurs und Handelsbilanz",
        answerD = "Den negativen Zusammenhang zwischen Inflationsrate und Arbeitslosenquote",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Die Phillips-Kurve (A.W. Phillips, 1958) zeigt einen empirischen inversen Zusammenhang: Hohe Inflation geht mit niedriger Arbeitslosigkeit einher und umgekehrt. Die Stagflation der 1970er stellte diesen Zusammenhang in Frage.",
        funFact = "Phillips, ein Neuseeländer, entdeckte die Kurve durch Analyse britischer Lohn- und Beschäftigungsdaten von 1861 bis 1957 — fast 100 Jahre historischer Daten."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist 'Quantitative Easing' (QE), das Zentralbanken nach der Finanzkrise 2008 einsetzten?",
        answerA = "Erhöhung der Leitzinsen zur Inflationsbekämpfung",
        answerB = "Devisenmarktinterventionen zur Währungsstabilisierung",
        answerC = "Direkte Geldzahlungen an Bürger durch den Staat",
        answerD = "Ankauf von Staatsanleihen und anderen Wertpapieren durch die Zentralbank zur Geldmengenausweitung",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Beim QE kauft die Zentralbank Wertpapiere (meist Staatsanleihen) vom Markt auf und erhöht damit die Geldmenge. Da der Leitzins bereits bei null lag, sollte QE weitere Impulse geben — die EZB kaufte ab 2015 über 2,6 Billionen Euro Wertpapiere.",
        funFact = "Der Begriff 'Quantitative Easing' soll erstmals 2001 von Bundesbank-Volkswirt Richard Werner geprägt worden sein — um die unkonventionelle Geldpolitik der Bank of Japan zu beschreiben."
    ),

    Question(
        categoryId = 11,
        questionText = "Was besagt die 'Efficient Market Hypothesis' (EMH) von Eugene Fama (Nobelpreis 2013)?",
        answerA = "Märkte tendieren zu Monopolen ohne staatliche Regulierung",
        answerB = "Staat ist immer effizienter als Markt bei der Ressourcenallokation",
        answerC = "Aktienkurse spiegeln stets alle verfügbaren Informationen wider, sodass keine systematische Überrendite erzielbar ist",
        answerD = "Effizienz erfordert vollständige Konkurrenz auf allen Märkten",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Famas EMH besagt: In effizienten Märkten sind alle Informationen bereits im Preis enthalten. Schlagzeilenbasiertes Trading ('Buy the rumor, sell the news') oder Fundamentalanalyse können keine systematische Überrendite erzielen.",
        funFact = "Ironischerweise erhielten 2013 sowohl Fama (für EMH) als auch Shiller (der EMH kritisiert und Marktineffizienz nachweist) gemeinsam den Nobelpreis."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das 'Moral Hazard'-Problem (moralisches Risiko) in der Finanzökonomie?",
        answerA = "Die Tendenz, ethisch fragwürdige Investitionen zu vermeiden",
        answerB = "Regulatorisches Versagen bei der Bankenaufsicht",
        answerC = "Korruption in staatlichen Wirtschaftsbehörden",
        answerD = "Die Neigung zu risikoreichem Verhalten, wenn man vor den Konsequenzen geschützt ist",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Moral Hazard beschreibt: Wer weiß, dass er bei Misserfolg gerettet wird (z.B. 'Too big to fail'-Banken), verhält sich risikoreicher. Dieser Mechanismus war 2008 zentral: Banken gingen enorme Risiken ein, weil sie staatliche Rettung erwarteten.",
        funFact = "Der Begriff 'Moral Hazard' stammt ursprünglich aus dem Versicherungswesen des 17. Jahrhunderts — Versicherte neigten dazu, weniger vorsichtig zu sein, wenn sie versichert waren."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht man unter dem 'Gini-Koeffizient' in der Wirtschaftsstatistik?",
        answerA = "Ein Maß für die Wirtschaftsleistung pro Einwohner",
        answerB = "Ein Index für die Wettbewerbsfähigkeit von Volkswirtschaften",
        answerC = "Ein Indikator für die Staatsverschuldung",
        answerD = "Ein Maß für die Einkommens- oder Vermögensungleichheit (0 = vollständige Gleichheit, 1 = maximale Ungleichheit)",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Der Gini-Koeffizient (Corrado Gini, 1912) misst Ungleichheit: 0 bedeutet alle haben gleich viel, 1 bedeutet eine Person besitzt alles. Deutschland liegt bei etwa 0,31, die USA bei 0,41, Südafrika bei über 0,60.",
        funFact = "Corrado Gini war Italiener und veröffentlichte seinen Koeffizienten 1912 mit 25 Jahren — er wurde später zu einem Unterstützer des Faschismus, was seine wissenschaftliche Reputation nachhaltig beschädigte."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt das 'Heckscher-Ohlin-Modell' des internationalen Handels?",
        answerA = "Länder handeln, weil sie unterschiedliche Technologien haben",
        answerB = "Länder exportieren Güter, bei deren Produktion ihre reichlich vorhandenen Produktionsfaktoren intensiv eingesetzt werden",
        answerC = "Handel ist nur zwischen Ländern mit ähnlichem Einkommensniveau vorteilhaft",
        answerD = "Wechselkurse bestimmen vollständig die Handelsströme",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Das H-O-Modell erklärt Handel durch Faktorausstattungsunterschiede: Ein kapitalreiches Land exportiert kapitalintensive Güter, ein arbeitsreiches Land exportiert arbeitsintensive Güter. Es baut auf Ricardos komparativen Vorteilen auf.",
        funFact = "Das Modell sagt voraus, dass Handel die Einkommensunterschiede zwischen Ländern angleicht (Faktorpreisausgleich). Empirisch ist dieser Effekt viel schwächer als theoretisch erwartet — ein bekanntes Rätsel der Handelstheorie."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist die 'Laffer-Kurve' und welche politische Schlussfolgerung wird aus ihr gezogen?",
        answerA = "Der Spitzensteuersatz sollte bei 50% liegen",
        answerB = "Niedrige Steuern führen immer zu Deflation",
        answerC = "Ab einem bestimmten Steuersatz sinken die Staatseinnahmen, weil Leistungsanreize und Steuereinnahmen fallen",
        answerD = "Höhere Steuern erhöhen stets die Staatseinnahmen linear",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Die Laffer-Kurve zeigt: Bei 0% und 100% Steuersatz sind Staatseinnahmen null. Dazwischen gibt es ein Optimum. Konservative Ökonomen nutzen sie, um Steuersenkungen zu begründen — wenn der aktuelle Satz 'über dem Scheitelpunkt' liegt, würden Steuersenkungen die Einnahmen erhöhen.",
        funFact = "Arthur Laffer soll seine Kurve 1974 auf einer Cocktailserviette gezeichnet haben — obwohl er das selbst bestreitet. Das historische Dokument ist nie aufgetaucht."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist die 'Kaufkraftparität' (KKP) und wofür wird sie verwendet?",
        answerA = "Berechnung des offiziellen Wechselkurses zwischen Währungen",
        answerB = "Maß für die Exportkompetitivität einer Volkswirtschaft",
        answerC = "Messung der Inflationsrate in verschiedenen Ländern",
        answerD = "Methode zum Vergleich von Lebensstandards, die unterschiedliche Preisniveaus berücksichtigt",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Die KKP berücksichtigt, dass ein Dollar in Indien mehr kauft als in der Schweiz. BIP-Vergleiche nach KKP zeigen den realen Lebensstandard besser als nominale Umrechnungen. Nach KKP ist China seit 2017 die größte Volkswirtschaft der Welt.",
        funFact = "Der 'Big Mac Index' der Zeitschrift The Economist ist eine spielerische Anwendung der KKP: Er vergleicht, was ein McDonald's Big Mac in verschiedenen Ländern kostet, um Währungsüberbewertungen zu schätzen."
    ),

    Question(
        categoryId = 11,
        questionText = "Joseph Schumpeter prägte den Begriff der 'kreativen Zerstörung'. Was beschreibt er?",
        answerA = "Staatliche Vernichtung von Monopolen zur Wettbewerbsförderung",
        answerB = "Kriegszerstörung als Motor wirtschaftlichen Aufbaus",
        answerC = "Die destruktive Wirkung von Finanzkrisen auf reale Wirtschaft",
        answerD = "Den Prozess, durch den Innovationen alte Technologien und Unternehmen verdrängen und Neues schaffen",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Schumpeters 'kreative Zerstörung' beschreibt den Kapitalismus als dynamischen Prozess: Innovationen (der Unternehmer als 'schöpferischer Zerstörer') verdrängen bestehende Strukturen — Dampfmaschine verdrängt Pferdekutschen, Internet verdrängt Zeitungen.",
        funFact = "Schumpeter übernahm den Begriff 'kreative Zerstörung' vom marxistischen Ökonomen Werner Sombart — obwohl Schumpeter selbst kein Marxist war."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt das 'Dutch Disease' (Holländische Krankheit) in der Wirtschaftstheorie?",
        answerA = "Volkswirtschaftliche Stagnation durch zu starke Gewerkschaften",
        answerB = "Übermäßige Staatsausgaben im Sozialbereich",
        answerC = "Die Schädigung des verarbeitenden Sektors durch Ressourcenbooms, die den Wechselkurs aufwerten",
        answerD = "Wirtschaftliche Probleme durch hohe Tulpenimporte",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Dutch Disease (geprägt 1977 im The Economist) beschreibt das Phänomen nach der Entdeckung von Erdgas in den Niederlanden 1959: Der Ressourcenexport wertete den Gulden auf, verteuerte niederländische Exporte und schrumpfte die Industrie.",
        funFact = "Viele erdölexportierende Länder leiden unter Dutch Disease — Nigeria, Venezuela und Saudi-Arabien kämpfen seit Jahrzehnten damit, ihre Wirtschaft jenseits des Öls zu diversifizieren."
    ),

    // --- ZUSÄTZLICHE FRAGEN: WIRTSCHAFTSTHEORIE & NOBELPREIS (15) ---

    Question(
        categoryId = 11,
        questionText = "Robert Mundell erhielt 1999 den Wirtschaftsnobelpreis. Für welches Konzept ist er bekannt, das die Grundlage für den Euro legte?",
        answerA = "Theorie der komparativen Vorteile",
        answerB = "Theorie der rationalen Erwartungen",
        answerC = "Theorie der Wachstumspole",
        answerD = "Theorie des optimalen Währungsraums",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Mundells Theorie des optimalen Währungsraums (1961) definiert Bedingungen, wann ein gemeinsamer Währungsraum sinnvoll ist: hohe Faktormobilität, flexible Löhne, fiskalpolitische Transfers. Die Eurokrise zeigte, dass die Eurozone diese Kriterien nicht vollständig erfüllte.",
        funFact = "Mundell gilt als 'Vater des Euros' — ironischerweise zweifelte er selbst daran, ob Europa alle Bedingungen für einen optimalen Währungsraum erfüllt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt die 'Quantitätstheorie des Geldes' (MV = PT)?",
        answerA = "Geldmenge (M) mal Umlaufgeschwindigkeit (V) entspricht Preisniveau (P) mal realem Output (T)",
        answerB = "Der Wert einer Währung ist proportional zu Goldreserven",
        answerC = "Zinssätze bestimmen die Investitionsbereitschaft direkt",
        answerD = "Die Inflationsrate steigt mit dem Staatsdefizit",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Die Quantitätstheorie (Fischer-Gleichung: MV = PT) besagt: Eine Verdopplung der Geldmenge M führt — bei konstanter Umlaufgeschwindigkeit V und konstantem Output T — zu einer Verdopplung des Preisniveaus P. Sie ist die theoretische Grundlage des Monetarismus.",
        funFact = "Die Quantitätstheorie ist über 400 Jahre alt — erste Formulierungen finden sich beim spanischen Scholastiker Martín de Azpilcueta um 1556, der die Preissteigerungen durch Gold aus Amerika erklärte."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist 'Adverse Selection' (adverse Selektion) in der Informationsökonomik?",
        answerA = "Die staatliche Auswahl der effizientesten Unternehmen für Subventionen",
        answerB = "Die strategische Qualitätsminderung durch Marktführer",
        answerC = "Die negative Auswahl von Arbeitnehmern durch diskriminierende Personalabteilungen",
        answerD = "Die Neigung, dass auf Märkten mit Informationsasymmetrie überproportional viele schlechte Risiken verbleiben",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Adverse Selektion: Wenn Käufer die Qualität nicht beurteilen können (Informationsasymmetrie), verdrängen schlechte Angebote gute — z.B. bei Gebrauchtwagen (Akerlofs 'Lemons'), Krankenversicherungen oder Kreditmärkten.",
        funFact = "Das Problem der adversen Selektion erklärt, warum Krankenversicherungen ohne Pflichtversicherung kollabieren: Nur Kranke versichern sich, Gesunde nicht — die Prämien steigen so stark, dass am Ende niemand mehr zahlen kann."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Ökonom entwickelte das Konzept der 'Rationalen Erwartungen', das die Makroökonomie revolutionierte?",
        answerA = "Robert Lucas",
        answerB = "Paul Samuelson",
        answerC = "James Tobin",
        answerD = "Franco Modigliani",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Robert Lucas (Nobelpreis 1995) entwickelte die Theorie der rationalen Erwartungen: Wirtschaftssubjekte nutzen alle verfügbaren Informationen und können staatliche Stabilisierungspolitik vorhersehen und antizipieren — dadurch wird sie unwirksam.",
        funFact = "Die 'Lucas-Kritik' zeigt, dass makroökonometrische Modelle unzuverlässig werden, wenn sich die Politik ändert — weil die Modellparameter selbst von der erwarteten Politik abhängen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das 'Trilemma der offenen Volkswirtschaft' (Mundell-Fleming-Trilemma)?",
        answerA = "Ein Land kann nicht gleichzeitig festen Wechselkurs, freien Kapitalverkehr und unabhängige Geldpolitik haben",
        answerB = "Vollbeschäftigung, Preisstabilität und Wachstum können nie gleichzeitig erreicht werden",
        answerC = "Freihandel, Währungsstabilität und soziale Gerechtigkeit schließen sich aus",
        answerD = "Demokratie, Kapitalismus und Gleichheit sind unvereinbar",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Das Mundell-Fleming-Trilemma: Ein Land kann nur zwei der drei Ziele gleichzeitig erreichen — fester Wechselkurs, freier Kapitalverkehr und autonome Geldpolitik. Die Eurozone wählte festen Kurs und freien Kapitalverkehr und gab nationale Geldpolitik auf.",
        funFact = "Das Trilemma erklärt, warum China seinen Kapitalverkehr kontrolliert: Es will sowohl einen kontrollierten Wechselkurs als auch unabhängige Geldpolitik — dann muss der Kapitalverkehr begrenzt sein."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht man unter 'Crowding Out' in der Fiskalpolitik?",
        answerA = "Die Verdrängung von Staatsbetrieben durch Privatisierung",
        answerB = "Der Rückgang des Außenhandels durch hohe Zölle",
        answerC = "Staatliche Kreditaufnahme verdrängt private Investitionen durch Zinserhöhung",
        answerD = "Die Verdrängung kleiner Firmen durch Konzerne",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Crowding Out: Wenn der Staat Geld leiht, erhöht sich die Kreditnachfrage, was die Zinsen treibt — dadurch werden private Investitionen teurer und verdrängt. Kritiker des keynesianischen Deficit Spending nutzen dieses Argument gegen Konjunkturprogramme.",
        funFact = "Empirisch ist Crowding Out bei tiefen Zinsen und Rezessionen gering — in der Nullzinsphase nach 2008 argumentierten viele Ökonomen, staatliche Ausgaben hätten kaum private Investitionen verdrängt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist der 'Balassa-Samuelson-Effekt' in der Wirtschaftstheorie?",
        answerA = "Reiche Länder haben höhere Preisniveaus, weil ihre produktivere Wirtschaft höhere Löhne im gesamten Dienstleistungssektor treibt",
        answerB = "Arme Länder holen reiche Länder automatisch ein (Konvergenztheorie)",
        answerC = "Exportstärke führt automatisch zur Währungsaufwertung",
        answerD = "Produktivitätswachstum senkt immer das Preisniveau",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Der Balassa-Samuelson-Effekt erklärt, warum ein Hamburger in Zürich viel teurer ist als in Warschau: Höhere Produktivität im Exportsektor treibt Löhne auch im Dienstleistungssektor hoch — nicht-handelbare Güter werden teurer.",
        funFact = "Dieser Effekt erklärt auch, warum osteuropäische Länder nach EU-Beitritt trotz niedrigerer Nominallöhne teurer erschienen: Produktivitätsfortschritte treiben Preise im Dienstleistungssektor."
    ),

    Question(
        categoryId = 11,
        questionText = "Welchen Begriff verwendete John Maynard Keynes für die Unfähigkeit der Geldpolitik, in einer tiefen Rezession die Wirtschaft zu stimulieren?",
        answerA = "Crowding Out",
        answerB = "Stagflationsfalle",
        answerC = "Liquiditätsfalle",
        answerD = "Deflationsspirale",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Die Liquiditätsfalle: Wenn Zinsen nahe null sind, horten Menschen lieber Bargeld als Anleihen zu kaufen. Geldmengensteigerungen verpuffen — Japan erlebte dies in den 1990ern, Europa und USA nach 2008.",
        funFact = "In der Liquiditätsfalle ist Fiskalpolitik (Staatsausgaben) effektiver als Geldpolitik — das ist der Kern des keynesianischen Arguments für Konjunkturprogramme in Krisenzeiten."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das 'Paradox of Thrift' (Sparparadoxon) in der keynesianischen Theorie?",
        answerA = "Sparen ist für den Einzelnen gut, aber wenn alle gleichzeitig sparen, schrumpft die Gesamtnachfrage und Einkommen sinken",
        answerB = "Armut führt paradoxerweise zu höherer Sparquote",
        answerC = "Staatliches Sparen erhöht immer das Wirtschaftswachstum",
        answerD = "Hohe Sparquoten sind immer ein Zeichen wirtschaftlicher Stärke",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Das Sparparadoxon (Keynes): Was für den Einzelnen klug ist — in der Krise mehr sparen — ist für alle zusammen schädlich. Wenn alle gleichzeitig weniger ausgeben, sinken Umsätze, Löhne und Jobs — am Ende hat die Gesellschaft weniger gespart, nicht mehr.",
        funFact = "Das Sparparadoxon erklärt, warum Sparaufrufe in der Rezession 2009 von Ökonomen wie Krugman heftig kritisiert wurden — sie hätten die Krise verlängert, nicht verkürzt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht man unter 'Too Big to Fail' in der Bankenregulierung?",
        answerA = "Unternehmen, die aufgrund ihrer Größe nie in Insolvenz gehen können",
        answerB = "Staatseigene Banken, die per Gesetz nicht bankrott gehen dürfen",
        answerC = "Systemrelevante Finanzinstitute, deren Zusammenbruch das gesamte Finanzsystem gefährden würde und daher staatlich gerettet werden",
        answerD = "Unternehmen, die durch ihre Monopolstellung nie scheitern",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Systemrelevante Banken werden gerettet, weil ihr Zusammenbruch das gesamte Finanzsystem destabilisieren würde. Das schafft Moral Hazard: Diese Banken gehen mehr Risiken ein, weil sie auf staatliche Rettung bauen können.",
        funFact = "Nach der Finanzkrise 2008 bezeichneten Kritiker das Prinzip zynisch als 'Too big to fail, too big to jail' — große Banken wurden gerettet, aber kein einziger Top-Manager kam ins Gefängnis."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist die 'Moderne Geldtheorie' (MMT — Modern Monetary Theory) in ihrer Kernaussage?",
        answerA = "Zentralbanken können unbegrenzt Geld drucken, ohne Konsequenzen",
        answerB = "Digitale Währungen werden den Goldstandard ersetzen",
        answerC = "Negative Einkommensteuer ist das optimale Sozialpolitikinstrument",
        answerD = "Ein Staat mit eigener Währung wird nie zahlungsunfähig und Defizite bis zur Vollbeschäftigung sind unproblematisch",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "MMT argumentiert: Ein Staat mit eigener Währung (wie USA, UK, Japan) kann nicht zahlungsunfähig werden, weil er Geld drucken kann. Defizite sind unproblematisch bis zur Vollbeschäftigung — erst dann entsteht Inflation. Kritiker warnen vor unkontrollierter Inflation.",
        funFact = "MMT erlebte nach 2020 einen Aufschwung — die massiven Corona-Konjunkturprogramme ohne sofortige Inflation schienen ihre Hauptthese zu bestätigen. Die Inflation 2021/22 gab dann wieder ihren Kritikern Argumente."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das 'Solow-Modell' (Solow-Residuum) in der Wachstumstheorie?",
        answerA = "Eine Theorie, die Wachstum durch internationalen Handel erklärt",
        answerB = "Ein Gleichgewichtsmodell für optimale Staatsausgaben",
        answerC = "Ein Modell, das Wachstum nur durch Kapitalakkumulation erklärt",
        answerD = "Ein Modell, das zeigt, dass langfristiges Wachstum nur durch technologischen Fortschritt möglich ist, nicht durch mehr Kapital oder Arbeit allein",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Robert Solow (Nobelpreis 1987) zeigte: Kapital- und Arbeitsakkumulation erklären nur einen Teil des Wachstums. Der Rest — das 'Solow-Residuum' — ist technologischer Fortschritt. Ohne technologischen Fortschritt stagniert Wachstum wegen abnehmender Grenzerträge.",
        funFact = "Das Solow-Modell erklärt, warum Entwicklungsländer nicht einfach durch Kapitalzuflüsse reicher werden: Das Kapital unterliegt abnehmenden Erträgen — irgendwann bringt ein weiterer Dollar Kapitalinvestition kaum mehr Wachstum."
    ),

    Question(
        categoryId = 11,
        questionText = "Angus Deaton erhielt 2015 den Wirtschaftsnobelpreis. Womit befasste sich seine preisgekrönte Forschung?",
        answerA = "Konstruktion des Human Development Index (HDI)",
        answerB = "Messung von Einkommensungleichheit durch erweiterten Gini-Koeffizienten",
        answerC = "Entwicklung des BIP als Wohlstandsmaß",
        answerD = "Analyse von Konsum, Armut und Wohlfahrt durch Haushaltserhebungen",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Deaton revolutionierte die Entwicklungsökonomie: Er analysierte Konsum- und Wohlfahrtsdaten auf Haushaltsebene statt aggregierter BIP-Daten. Seine Forschung zur globalen Armut zeigte, dass viele traditionelle Entwicklungshilfemaßnahmen unwirksam waren.",
        funFact = "Deatons Buch 'The Great Escape' (2013) dokumentiert den dramatischen Rückgang globaler Armut seit 1800 — ein optimistisches Gegenstück zu pessimistischen Entwicklungsberichten."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt das 'Heckscher-Ohlin-Modell' des internationalen Handels konkret?",
        answerA = "Länder handeln, weil sie unterschiedliche Technologien haben",
        answerB = "Länder exportieren Güter, bei deren Produktion ihre reichlich vorhandenen Produktionsfaktoren intensiv eingesetzt werden",
        answerC = "Handel ist nur zwischen Ländern mit ähnlichem Einkommensniveau vorteilhaft",
        answerD = "Wechselkurse bestimmen vollständig die Handelsströme",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Das H-O-Modell erklärt Handel durch Faktorausstattungsunterschiede: Ein kapitalreiches Land exportiert kapitalintensive Güter, ein arbeitsreiches Land exportiert arbeitsintensive Güter. Es baut auf Ricardos komparativen Vorteilen auf.",
        funFact = "Das Modell sagt voraus, dass Handel die Einkommensunterschiede zwischen Ländern angleicht (Faktorpreisausgleich). Empirisch ist dieser Effekt viel schwächer als theoretisch erwartet — ein bekanntes Rätsel der Handelstheorie."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist die 'Laffer-Kurve' und welche Schlussfolgerung wird aus ihr gezogen?",
        answerA = "Höhere Steuern erhöhen stets die Staatseinnahmen linear",
        answerB = "Ab einem bestimmten Steuersatz sinken die Staatseinnahmen, weil Leistungsanreize fallen — deshalb können Steuersenkungen die Einnahmen erhöhen",
        answerC = "Niedrige Steuern führen immer zu Deflation",
        answerD = "Der Spitzensteuersatz sollte immer bei 50% liegen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Die Laffer-Kurve zeigt: Bei 0% und 100% Steuersatz sind Staatseinnahmen null. Dazwischen gibt es ein Maximum. Konservative Ökonomen nutzen sie, um Steuersenkungen zu begründen — wenn der aktuelle Satz 'über dem Scheitelpunkt' liegt, würden Steuersenkungen die Einnahmen erhöhen.",
        funFact = "Arthur Laffer soll seine Kurve 1974 auf einer Cocktailserviette gezeichnet haben — obwohl er das selbst bestreitet. Das historische Dokument ist nie aufgetaucht."
    )

)

