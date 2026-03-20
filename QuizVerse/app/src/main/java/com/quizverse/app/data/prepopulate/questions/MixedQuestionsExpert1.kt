package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsExpert1(): List<Question> = listOf(

    // --- PHILOSOPHIE (10) ---

    Question(
        categoryId = 11,
        questionText = "Welchen Begriff prägte Immanuel Kant für die Erkenntnis, die unabhängig von jeder Erfahrung möglich ist?",
        answerA = "Empirisches Urteil",
        answerB = "A-posteriori-Erkenntnis",
        answerC = "A-priori-Erkenntnis",
        answerD = "Transzendente Intuition",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Kant unterscheidet zwischen 'a priori' (vor der Erfahrung, rein durch Vernunft) und 'a posteriori' (aus der Erfahrung stammend). A-priori-Erkenntnis ist notwendig und allgemeingültig.",
        funFact = "Kant verbrachte sein gesamtes Leben in Königsberg und soll so pünktlich spazieren gegangen sein, dass die Einwohner ihre Uhren nach ihm stellten."
    ),

    Question(
        categoryId = 11,
        questionText = "In welchem Werk entwickelte Georg Wilhelm Friedrich Hegel seine Dialektik aus Thesis, Antithesis und Synthesis?",
        answerA = "Kritik der reinen Vernunft",
        answerB = "Phänomenologie des Geistes",
        answerC = "Sein und Zeit",
        answerD = "Jenseits von Gut und Böse",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "In der 'Phänomenologie des Geistes' (1807) beschreibt Hegel die Entwicklung des Geistes durch dialektische Bewegung. Die Begriffe Thesis/Antithesis/Synthesis wurden jedoch eher von Fichte verwendet.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welchen philosophischen Begriff bezeichnete Leibniz als die kleinsten, unteilbaren, nicht-materiellen Einheiten der Wirklichkeit?",
        answerA = "Atome",
        answerB = "Nomoi",
        answerC = "Monaden",
        answerD = "Ideen",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Gottfried Wilhelm Leibniz entwickelte in seiner 'Monadologie' (1714) das Konzept der Monaden: geistige Substanzen ohne Fenster nach außen, die die Grundbausteine der Realität bilden.",
        funFact = "Leibniz erfand unabhängig von Newton die Infinitesimalrechnung — der Prioritätsstreit der beiden gilt als einer der bittersten in der Wissenschaftsgeschichte."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnete Sokrates mit dem Begriff 'Maieutik'?",
        answerA = "Die Lehre von der Seelenwanderung",
        answerB = "Die Kunst des Gesprächs zur Wahrheitsfindung durch Hebammenmethode",
        answerC = "Die Tugendlehre der Stoa",
        answerD = "Die Theorie des idealen Staates",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Maieutik (griech. 'Hebammenkunst') ist die sokratische Methode, durch gezieltes Fragen latentes Wissen im Gesprächspartner hervorzuholen — analog zur Geburtshilfe seiner Mutter.",
        funFact = "Sokrates schrieb selbst nichts auf. Alles, was wir über ihn wissen, stammt aus Schriften seiner Schüler, vor allem Platon."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer formulierte den kategorischen Imperativ: 'Handle nur nach derjenigen Maxime, durch die du zugleich wollen kannst, dass sie ein allgemeines Gesetz werde'?",
        answerA = "John Locke",
        answerB = "Jeremy Bentham",
        answerC = "Immanuel Kant",
        answerD = "Jean-Jacques Rousseau",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Kant formulierte den kategorischen Imperativ in der 'Grundlegung zur Metaphysik der Sitten' (1785) als oberstes Prinzip der Moral, unabhängig von Konsequenzen oder Neigungen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Konzept beschreibt Sartre mit dem Satz 'Die Existenz geht der Essenz voraus'?",
        answerA = "Deterministischen Fatalismus",
        answerB = "Den existentialistischen Atheismus und menschliche Freiheit",
        answerC = "Platons Ideenlehre",
        answerD = "Den cartesischen Dualismus",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Sartre meint: Der Mensch wird nicht mit einer vorbestimmten Natur geboren (Essenz), sondern erschafft sich durch seine Entscheidungen selbst. Dies ist der Kern des existentialistischen Humanismus.",
        funFact = "Sartre lehnte 1964 den Nobelpreis für Literatur ab — eines der wenigen Male, dass ein Autor diese Auszeichnung freiwillig zurückwies."
    ),

    Question(
        categoryId = 11,
        questionText = "Was verstand Platon unter der 'Anamnesis'?",
        answerA = "Die Erinnerung an vergangene Leben durch Seelenwanderung",
        answerB = "Das Wiedererinnern der Seele an ewige Ideen, die sie vor der Geburt geschaut hat",
        answerC = "Den kollektiven Gedächtnisspeicher der Gesellschaft",
        answerD = "Die traumatische Verdrängung von Kindheitserlebnissen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Platon lehrte, die unsterbliche Seele habe vor der Geburt die ewigen Ideen geschaut. Lernen bedeutet daher Wiedererinnern (Anamnesis) — nicht Neuerwerb von Wissen.",
        funFact = "Platon demonstrierte die Anamnesis im Dialog 'Menon', indem Sokrates einen ungebildeten Sklaven durch Fragen zur korrekten geometrischen Lösung führte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Philosoph prägte den Begriff 'Übermensch' und sah ihn als Ziel der Menschheitsentwicklung jenseits von Gut und Böse?",
        answerA = "Arthur Schopenhauer",
        answerB = "Friedrich Nietzsche",
        answerC = "Max Stirner",
        answerD = "Ludwig Feuerbach",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Nietzsche entwickelte das Konzept des Übermenschen in 'Also sprach Zarathustra' (1883–85) als Ideal eines Menschen, der eigene Werte schafft, jenseits der bürgerlichen Moral.",
        funFact = "Nietzsche schrieb 'Also sprach Zarathustra' in einem schöpferischen Rausch in nur zehn Tagen — Teil I im Januar, Teil II im Juni 1883."
    ),

    Question(
        categoryId = 11,
        questionText = "Wie bezeichnet man in der Erkenntnistheorie die These, dass alle Erkenntnis letztlich auf Sinneserfahrungen beruht?",
        answerA = "Rationalismus",
        answerB = "Idealismus",
        answerC = "Empirismus",
        answerD = "Skeptizismus",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Der Empirismus (Locke, Hume, Berkeley) postuliert: Nichts ist im Verstand, was nicht zuvor in den Sinnen war (nihil est in intellectu quod non prius fuerit in sensu).",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Gedankenexperiment von John Searle soll zeigen, dass ein Computer kein echtes Verständnis besitzt, auch wenn er wie ein denkender Mensch reagiert?",
        answerA = "Turing-Test",
        answerB = "Chinesisches Zimmer",
        answerC = "Schrödingers Katze",
        answerD = "Trolley-Problem",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Im 'Chinesischen Zimmer' (1980) sitzt eine Person, die kein Chinesisch versteht, in einem Raum und beantwortet chinesische Fragen mithilfe von Regelbüchern. Searle argumentiert: Syntax allein ergibt keine Semantik.",
        funFact = "Der Turing-Test wurde von Alan Turing 1950 in seinem Aufsatz 'Computing Machinery and Intelligence' vorgeschlagen — damals unter dem Begriff 'Imitation Game'."
    ),

    // --- KUNST & ARCHITEKTUR (10) ---

    Question(
        categoryId = 11,
        questionText = "Welcher Architekt entwarf das Guggenheim-Museum in Bilbao, das 1997 eröffnet wurde?",
        answerA = "Renzo Piano",
        answerB = "Zaha Hadid",
        answerC = "Frank Gehry",
        answerD = "Norman Foster",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Frank Gebrys dekonstruktivistisches Meisterwerk aus Titan und Glas gilt als eines der einflussreichsten Gebäude des 20. Jahrhunderts und löste den sogenannten 'Bilbao-Effekt' aus.",
        funFact = "Der 'Bilbao-Effekt' beschreibt das Phänomen, wie ein spektakuläres Architekturprojekt eine ganze Stadtregion wirtschaftlich transformieren kann."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Stilrichtung kennzeichnet Werke mit verzerrten Perspektiven und beängstigenden, traumartigen Szenen, wie in El Grecos Spätwerk?",
        answerA = "Manierismus",
        answerB = "Barock",
        answerC = "Impressionismus",
        answerD = "Expressionismus",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Der Manierismus (ca. 1520–1600) folgte der Hochrenaissance und betonte künstliche Eleganz, gestreckte Figuren und emotionale Intensität. El Grecos Spätwerk gilt als proto-manieristisch.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Wie nennt man in der Architektur den keilförmigen Stein im Scheitelpunkt eines Bogens, der das gesamte Gewölbe zusammenhält?",
        answerA = "Kapitell",
        answerB = "Schlussstein",
        answerC = "Konsole",
        answerD = "Tympanon",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der Schlussstein (auch Keilstein oder Scheitelstein) schließt einen Bogen ab. Erst wenn er eingesetzt wird, kann das Lehrgerüst entfernt werden, da alle Kräfte sich gegenseitig abstützen.",
        funFact = "Im Mittelalter galten Schlusssteine oft als besonders repräsentativ und wurden mit Wappen, Heiligenfiguren oder ornamentalen Motiven verziert."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Kunstströmung verarbeitete unbewusste Vorstellungen und Traumbilder in surrealen Gemälden — begründet durch André Breton 1924?",
        answerA = "Dadaismus",
        answerB = "Futurismus",
        answerC = "Surrealismus",
        answerD = "Kubismus",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Der Surrealismus entstand nach dem Dadaismus und war stark von Freuds Psychoanalyse beeinflusst. Wichtige Vertreter: Salvador Dalí, René Magritte, Max Ernst.",
        funFact = "André Bretons 'Erstes Surrealistisches Manifest' (1924) definierte Surrealismus als 'psychischen Automatismus in seiner reinen Form'."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer malte das Triptychon 'Der Garten der Lüste', eines der rätselhaftesten Werke der Kunstgeschichte (ca. 1490–1510)?",
        answerA = "Pieter Bruegel der Ältere",
        answerB = "Hieronymus Bosch",
        answerC = "Lucas Cranach der Ältere",
        answerD = "Albrecht Dürer",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Hieronymus Boschs Triptychon im Prado zeigt links das Paradies, in der Mitte eine phantastische Lustgartenwelt, rechts die Hölle — bis heute ist seine genaue Bedeutung Gegenstand von Forschung.",
        funFact = "Bosch ist heute einer der meistgesehenen Maler im Prado in Madrid, obwohl er zu Lebzeiten kaum außerhalb der Niederlande bekannt war."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches architektonische Prinzip beschreibt die Verteilung des Gewichts einer Wölbung auf außenliegende Stützpfeiler durch schräge Bögen?",
        answerA = "Pendentiv",
        answerB = "Strebewerk mit Strebebogen",
        answerC = "Opus incertum",
        answerD = "Pilaster",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Das gotische Strebewerk mit Strebebogen leitet den Gewölbeschub über außenliegende Bögen zu Strebepfeilern ab. Das ermöglichte dünne Wände mit großen Fenstern — charakteristisch für gotische Kathedralen.",
        funFact = "Die Kathedrale Notre-Dame in Paris besitzt einige der frühesten und elegantesten Strebebögen der gotischen Architektur (ab ca. 1180)."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Technik verwendet der Künstler bei der 'Sfumato'-Malweise, die Leonardo da Vinci berühmt machte?",
        answerA = "Lasieren mit transparenten Farbschichten ohne harte Konturen",
        answerB = "Auftragen von Farbe mit dem Spachtel",
        answerC = "Ritzen von Linien in frischen Putz",
        answerD = "Mischen von Sand in die Farbe für Textur",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Sfumato (ital. 'verdampft') ist Leonardos Technik, bei der Konturen durch hauchdünne Farbschichten weich verschwimmen — wie in Dunst aufgelöst. Das Mona Lisa-Lächeln entsteht so.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Kunstbewegung propagierte ab 1909 Geschwindigkeit, Technik und Gewalt als ästhetische Werte und entstand in Italien?",
        answerA = "Bauhaus",
        answerB = "De Stijl",
        answerC = "Futurismus",
        answerD = "Konstruktivismus",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Filippo Tommaso Marinetti gründete den Futurismus 1909 mit seinem Manifest. Die Bewegung verherrlichte Maschinen, Tempo und Krieg — viele Futuristen sympathisierten später mit dem Faschismus.",
        funFact = "Marinettis Futuristisches Manifest wurde zuerst auf der Titelseite der Pariser Zeitung 'Le Figaro' veröffentlicht — eine ungewöhnliche Bühne für eine Kunstbewegung."
    ),

    Question(
        categoryId = 11,
        questionText = "In welchem Stil ist die Sagrada Família in Barcelona entworfen worden, und wer begann ihren Bau?",
        answerA = "Neoklassizismus — Ildefons Cerdà",
        answerB = "Katalanischer Modernisme — Antoni Gaudí",
        answerC = "Art Déco — Josep Puig i Cadafalch",
        answerD = "Gotische Revivalbewegung — Eugène Viollet-le-Duc",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Antoni Gaudí übernahm 1883 den Bau der Sagrada Família und prägte sie mit seinem organisch-katalanischen Modernisme-Stil. Gaudí widmete die letzten 12 Jahre seines Lebens fast ausschließlich diesem Projekt.",
        funFact = "Gaudí starb 1926, überfahren von einer Straßenbahn. Da er so ärmlich gekleidet war, wurde er zunächst für einen Bettler gehalten."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Konzept steht hinter dem 'Gesamtkunstwerk', das Richard Wagner für seine Opern forderte?",
        answerA = "Die Reduktion auf ein einzelnes Ausdrucksmittel zur maximalen Reinheit",
        answerB = "Die Fusion aller Künste (Musik, Dichtung, Bild, Bewegung) zu einer einheitlichen Wirkung",
        answerC = "Die Dokumentation von Alltagsleben als Kunstform",
        answerD = "Die mathematische Strukturierung von Kunstwerken nach dem Goldenen Schnitt",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Wagner forderte in seiner Schrift 'Das Kunstwerk der Zukunft' (1849), alle Künste im Musikdrama zu vereinen, um eine Gesamtwirkung zu erzielen, die keine Einzelkunst erreichen kann.",
        funFact = "Wagners Bayreuther Festspielhaus wurde speziell für seine Opern gebaut — mit verdecktem Orchestergraben, damit die Musik wie von überall zu kommen scheint."
    ),

    // --- WIRTSCHAFT & RECHT (10) ---

    Question(
        categoryId = 11,
        questionText = "Welches wirtschaftliche Konzept beschreibt die Situation, in der ein Markt aufgrund von Informationsasymmetrien zusammenbricht — beschrieben von George Akerlof anhand von Gebrauchtwagen?",
        answerA = "Moralisches Risiko",
        answerB = "Der Markt für 'Lemons'",
        answerC = "Das Gefangenendilemma",
        answerD = "Coase-Theorem",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Akerlofs 'The Market for Lemons' (1970) zeigt: Wenn Käufer die Qualität nicht kennen, sinkt der Marktpreis so weit, dass gute Ware aus dem Markt verdrängt wird — adverse Selektion.",
        funFact = "Akerlofs Arbeit wurde zunächst von drei Zeitschriften abgelehnt, bevor sie veröffentlicht wurde — sie brachte ihm 2001 den Nobelpreis für Wirtschaft ein."
    ),

    Question(
        categoryId = 11,
        questionText = "Was besagt das 'Coase-Theorem' in der Wirtschaftsrechtslehre?",
        answerA = "Monopole entstehen immer durch staatliche Regulierung",
        answerB = "Bei null Transaktionskosten führen private Verhandlungen stets zu einer effizienten Ressourcenallokation, unabhängig von der Eigentumsordnung",
        answerC = "Externe Effekte können nie internalisiert werden",
        answerD = "Kartellrecht ist wirtschaftlich ineffizient",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Ronald Coase zeigte 1960: Wenn Transaktionskosten null sind, handeln Parteien eigenständig zu einer Pareto-optimalen Lösung. Daher liegt das Problem externer Effekte nicht in der Eigentumsordnung, sondern in Transaktionskosten.",
        funFact = "Coase erhielt 1991 im Alter von 80 Jahren den Wirtschaftsnobelpreis — zu einem Zeitpunkt, als viele glaubten, er sei längst verstorben."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Rechtsprinzip besagt, dass niemand aus eigenem Unrecht einen Vorteil ziehen darf (lateinisch: 'nemo ex delicto suo condicionem suam meliorem facere potest')?",
        answerA = "Pacta sunt servanda",
        answerB = "Ex turpi causa non oritur actio",
        answerC = "In dubio pro reo",
        answerD = "Nemo-tenetur-Grundsatz",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "'Ex turpi causa non oritur actio' (aus schändlichem Grund entsteht keine Klage) verwehrt einer Partei Rechtsschutz, wenn der Anspruch auf eigenem rechtswidrigen Verhalten beruht.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht man in der Volkswirtschaft unter dem 'Pigou-Steuer'?",
        answerA = "Eine Steuer auf importierte Luxusgüter",
        answerB = "Eine Steuer, die negative externe Effekte internalisiert und damit den sozialen Optimalpunkt erreicht",
        answerC = "Eine Einkommensteuer nach dem Leistungsfähigkeitsprinzip",
        answerD = "Eine Steuer auf natürliche Monopole",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Arthur Pigou schlug vor, negative Externalitäten (z.B. Umweltverschmutzung) durch eine Steuer in Höhe des externen Schadens zu korrigieren. CO₂-Steuern sind ein modernes Beispiel.",
        funFact = "Die CO₂-Steuer in Deutschland wurde 2021 eingeführt und basiert konzeptionell direkt auf Pigous Theorie aus dem Jahr 1920."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Rechtsprinzip des deutschen Grundgesetzes besagt, dass staatliche Eingriffe geeignet, erforderlich und angemessen sein müssen?",
        answerA = "Rechtsstaatsprinzip",
        answerB = "Verhältnismäßigkeitsgrundsatz",
        answerC = "Subsidiaritätsprinzip",
        answerD = "Bestimmtheitsgrundsatz",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der Verhältnismäßigkeitsgrundsatz (Art. 20 GG) ist ein Kernprinzip des deutschen Verfassungsrechts: staatliche Maßnahmen müssen einem legitimen Zweck dienen und das mildeste geeignete Mittel sein.",
        funFact = "Das Bundesverfassungsgericht verwendet den Verhältnismäßigkeitsgrundsatz als zentrales Prüfungsinstrument — fast jedes Urteil zur Grundrechtsbeschränkung enthält diese Prüfung."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt in der Spieltheorie das 'Nash-Gleichgewicht'?",
        answerA = "Eine Situation, in der alle Spieler kooperieren und gemeinsam den Gewinn maximieren",
        answerB = "Ein Zustand, in dem kein Spieler durch einseitige Strategieänderung besser gestellt werden kann",
        answerC = "Die optimale Lösung bei vollständiger Information",
        answerD = "Das Ergebnis einer Nullsummenspiel-Auktion",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "John Nash zeigte 1950: In jedem endlichen Spiel existiert mindestens ein Gleichgewicht, in dem jeder Spieler die beste Antwort auf die Strategien der anderen spielt. Das Gefangenendilemma hat ein Nash-Gleichgewicht, das beide schlechter stellt.",
        funFact = "John Nashs Leben wurde im Film 'A Beautiful Mind' (2001) verfilmt — allerdings mit einigen dramatischen Freiheiten gegenüber der historischen Realität."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Eigenschaft macht ein Gut zu einem 'öffentlichen Gut' in der Wirtschaftstheorie?",
        answerA = "Es wird vom Staat produziert und kostenlos verteilt",
        answerB = "Nicht-Rivalität im Konsum und Nicht-Ausschließbarkeit vom Konsum",
        answerC = "Es unterliegt staatlicher Preisregulierung",
        answerD = "Es wird von einer gemeinnützigen Organisation bereitgestellt",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Öffentliche Güter (z.B. Landesverteidigung, Leuchtturm) sind nicht-rival (Konsum durch Person A mindert Verfügbarkeit für B nicht) und nicht-ausschließbar (niemanden kann man ausschließen). Das führt zum Trittbrettfahrerproblem.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist unter dem rechtlichen Begriff 'Culpa in contrahendo' zu verstehen?",
        answerA = "Haftung für Mängel nach Vertragsabschluss",
        answerB = "Verschulden beim Vertragsschluss — Haftung für vorvertragliche Pflichtverletzungen",
        answerC = "Haftung des Arbeitgebers für Arbeitnehmer",
        answerD = "Strafbarkeit bei Vertragsbruch",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Culpa in contrahendo (§ 311 II BGB) begründet Schadensersatz bei Pflichtverletzungen im Vertragsanbahnungsstadium — z.B. wenn jemand im Laden ausrutscht, ohne dass ein Vertrag zustande kam.",
        funFact = "Das Konzept wurde maßgeblich vom deutschen Juristen Rudolf von Jhering im 19. Jahrhundert entwickelt und prägt das Vertragsrecht bis heute."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche ökonomische Theorie besagt, dass die langfristige Philipps-Kurve senkrecht verläuft und Geldpolitik dauerhaft keine Wirkung auf Beschäftigung hat?",
        answerA = "Keynesianismus",
        answerB = "Angebotstheorie (Supply-Side Economics)",
        answerC = "Monetarismus (Friedman/Phelps, natürliche Arbeitslosigkeit)",
        answerD = "Moderne Geldtheorie (MMT)",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Milton Friedman und Edmund Phelps zeigten unabhängig voneinander 1968: Geldpolitik kann kurzfristig Beschäftigung erhöhen, aber langfristig passen sich Inflationserwartungen an, und die Wirtschaft kehrt zur 'natürlichen Arbeitslosenquote' zurück.",
        funFact = "Friedman und Phelps' Vorhersage bestätigte sich mit der Stagflation der 1970er Jahre — Inflation und Arbeitslosigkeit stiegen gleichzeitig, was der Keynesianer-These widersprach."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Prinzip regelt im deutschen Insolvenzrecht, dass alle Gläubiger gleich behandelt werden sollen?",
        answerA = "Prioritätsprinzip",
        answerB = "Gläubigergleichbehandlungsgrundsatz (par conditio creditorum)",
        answerC = "Treuhandprinzip",
        answerD = "Absonderungsrecht",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "'Par conditio creditorum' (gleiche Bedingungen für alle Gläubiger) ist das Kernprinzip des Insolvenzrechts. Gesicherte Gläubiger haben jedoch Vorrang durch Absonderungsrechte (§ 49 InsO).",
        funFact = null
    ),

    // --- PSYCHOLOGIE (10) ---

    Question(
        categoryId = 11,
        questionText = "Welcher Psychologe beschrieb die 'kognitive Dissonanz' als unangenehmen Zustand, der entsteht, wenn Überzeugungen und Verhalten im Widerspruch stehen?",
        answerA = "B.F. Skinner",
        answerB = "Leon Festinger",
        answerC = "Abraham Maslow",
        answerD = "Carl Rogers",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Leon Festinger entwickelte 1957 die Theorie der kognitiven Dissonanz: Menschen streben nach Konsistenz ihrer Überzeugungen und Handlungen — bei Widersprüchen ändern sie oft eher ihre Einstellung als ihr Verhalten.",
        funFact = "Festinger infiltrierte eine apokalyptische Sekte, um zu studieren, wie Menschen reagieren, wenn ihre Prophezeiungen sich nicht erfüllen — Ergebnis: noch stärkerer Glaube."
    ),

    Question(
        categoryId = 11,
        questionText = "Was zeigte Stanley Milgrams berühmtes Gehorsamkeitsexperiment (1961)?",
        answerA = "Menschen helfen Fremden, wenn sie allein sind, aber nicht in der Gruppe",
        answerB = "Eine erschreckend hohe Bereitschaft, auf Autorität hin anderen Menschen Schmerzen zuzufügen",
        answerC = "Kinder lernen aggressives Verhalten durch Beobachtung",
        answerD = "Selbsterfüllende Prophezeiungen verbessern schulische Leistungen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "65% der Probanden verabreichten auf Anweisung des 'Wissenschaftlers' scheinbar lebensbedrohliche Elektroschocks an Schauspieler. Das Experiment gilt als Beweis für den destruktiven Gehorsam gegenüber Autorität.",
        funFact = "Milgrams Studie löste eine der heftigsten Debatten über Forschungsethik in der Geschichte der Psychologie aus und führte zu strengeren Ethikrichtlinien."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Phänomen beschreibt, dass Anwesende in Notfällen weniger helfen, je mehr Beobachter anwesend sind?",
        answerA = "Soziale Erleichterung",
        answerB = "Bystander-Effekt (Verantwortungsdiffusion)",
        answerC = "Gruppendenken",
        answerD = "Konformitätseffekt",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der Bystander-Effekt (Darley & Latané, 1968) entsteht durch Verantwortungsdiffusion: Je mehr Menschen anwesend sind, desto weniger fühlt sich jeder einzelne verantwortlich einzugreifen.",
        funFact = "Das Experiment entstand nach dem Mordfall Kitty Genovese (1964), bei dem angeblich 38 Zeugen nicht eingriffen — die genaue Zahl ist historisch umstritten, aber der Fall löste die Forschung aus."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt in der Entwicklungspsychologie die 'Objektpermanenz' nach Jean Piaget?",
        answerA = "Die Fähigkeit, Sprache durch Imitation zu erlernen",
        answerB = "Das Verständnis, dass Objekte weiterexistieren, auch wenn sie nicht sichtbar sind",
        answerC = "Die motorische Koordination von Greifbewegungen",
        answerD = "Die Fähigkeit zur Empathie ab dem zweiten Lebensjahr",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Piaget beschrieb, dass Säuglinge bis ca. 8 Monate keine Objektpermanenz besitzen — versteckt man ein Spielzeug, suchen sie nicht danach. Ab ca. 8–12 Monaten entwickelt sich dieses Verständnis.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Begriff aus Freuds Strukturmodell bezeichnet die moralische Instanz der Psyche, die verinnerlichte gesellschaftliche Normen repräsentiert?",
        answerA = "Es",
        answerB = "Ich",
        answerC = "Über-Ich",
        answerD = "Libido",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Freuds Strukturmodell (1923) besteht aus: Es (Triebe, Unbewusstes), Ich (Realitätsprinzip, vermittelt) und Über-Ich (verinnerlichte Normen, Gewissen, Elterninstanz).",
        funFact = "Freud entwickelte das Strukturmodell als Ergänzung zu seinem früheren topografischen Modell (Bewusstes / Vorbewusstes / Unbewusstes)."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das Kernprinzip der 'Theorie der Sozialen Identität' von Tajfel und Turner?",
        answerA = "Menschen verhalten sich in Gruppen immer altruistischer als allein",
        answerB = "Die Zugehörigkeit zu einer Gruppe beeinflusst das Selbstbild — Menschen werten die Eigengruppe auf und fremde Gruppen ab",
        answerC = "Soziale Rollen bestimmen vollständig das individuelle Verhalten",
        answerD = "Hierarchien in Gruppen entstehen ausschließlich durch physische Überlegenheit",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Tajfel & Turner zeigten: Schon minimale Gruppenmerkmale ('minimale Gruppen-Paradigma') führen zu Ingroup-Favoritismus und Outgroup-Diskriminierung — ein Grundmechanismus von Vorurteil und Diskriminierung.",
        funFact = "Das 'Minimale-Gruppen-Paradigma' wurde mit zufälligen Gruppen getestet — z.B. Präferenz für Klee- vs. Kandinsky-Bilder. Selbst dann bevorzugten Probanden die eigene Gruppe."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches psychologische Konzept beschreibt die Tendenz, eine erste Information so stark zu gewichten, dass sie alle weiteren Urteile verzerrt?",
        answerA = "Bestätigungsfehler",
        answerB = "Ankereffekt",
        answerC = "Verfügbarkeitsheuristik",
        answerD = "Halo-Effekt",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der Ankereffekt (Kahneman & Tversky) zeigt: Wenn zuerst eine Zahl genannt wird (Anker), beeinflusst sie alle nachfolgenden Schätzungen erheblich — selbst wenn der Anker offensichtlich willkürlich ist.",
        funFact = "In einem Experiment schätzten Probanden den Anteil afrikanischer Länder in der UNO höher/niedriger, je nachdem ob vorher ein Glücksrad auf 10 oder 65 zeigte — vollkommen irrelevant, aber wirksam."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Experiment von Solomon Asch (1951) untersuchte die Bereitschaft, einer offensichtlich falschen Mehrheitsmeinung nachzugeben?",
        answerA = "Konformitätsexperiment mit Linienlängen",
        answerB = "Stanford-Gefängnisexperiment",
        answerC = "Hawthorne-Effekt-Studie",
        answerD = "Bobo-Doll-Experiment",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Asch ließ Probanden Linienlängen vergleichen. Wenn Eingeweihte falsche Antworten gaben, stimmten ca. 37% der echten Probanden mit der falschen Mehrheit überein — Konformitätsdruck überwältigt Sinneswahrnehmung.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt die 'Theorie der gelernten Hilflosigkeit' von Martin Seligman?",
        answerA = "Kinder lernen Hilfsbereitschaft durch positive Verstärkung",
        answerB = "Organismen, die wiederholt unkontrollierbare negative Ereignisse erfahren, hören auf, Kontrolle zu versuchen — auch wenn Kontrolle möglich wäre",
        answerC = "Menschen mit hoher Empathie helfen häufiger als andere",
        answerD = "Hilfeverhalten nimmt in Gruppen durch soziale Verstärkung zu",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Seligman (1967) beobachtete Hunde, die bei unvermeidbaren Elektroschocks aufhörten zu fliehen — selbst als Flucht möglich wurde. Das Modell erklärt Depressionen als erlernte Hilflosigkeit.",
        funFact = "Seligmans Theorie führte zur Entwicklung der kognitiven Verhaltenstherapie als wirksamste Behandlung von Depressionen."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Zone beschreibt Lew Wygotski als den Bereich zwischen dem, was ein Kind allein kann, und dem, was es mit Hilfe erreichen kann?",
        answerA = "Proximale Entwicklungszone",
        answerB = "Kognitive Assimilation",
        answerC = "Scaffolding-Bereich",
        answerD = "Sensomotorisches Stadium",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Die 'Zone der nächsten Entwicklung' (proximal development zone) nach Wygotski definiert den pädagogisch produktiven Bereich: Aufgaben, die mit Unterstützung, aber nicht allein lösbar sind, fördern Entwicklung am stärksten.",
        funFact = "Wygotski starb 1934 mit nur 37 Jahren an Tuberkulose — viele seiner Werke wurden in der Sowjetunion jahrzehntelang verboten und erst in den 1960ern weltweit bekannt."
    ),

    // --- MYTHOLOGIE (8) ---

    Question(
        categoryId = 11,
        questionText = "Welcher griechische Titan war Zeus' Vater und verschluckte seine Kinder, um einer Prophezeiung zu entgehen?",
        answerA = "Prometheus",
        answerB = "Kronos",
        answerC = "Hyperion",
        answerD = "Atlas",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Kronos verschluckte alle seine Kinder von Rhea, da prophezeit wurde, sein Sohn würde ihn stürzen. Rhea rettete Zeus, indem sie Kronos einen eingewickelten Stein übergab.",
        funFact = "In der römischen Mythologie entspricht Kronos dem Gott Saturn — daher 'Saturnus' und der Samstag (Saturday) in der englischen Sprache."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer ist in der nordischen Mythologie der Wächter der Regenbogenbrücke Bifröst, der mit seinem Horn Gjallarhorn die Götter warnen kann?",
        answerA = "Tyr",
        answerB = "Baldur",
        answerC = "Heimdall",
        answerD = "Freyr",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Heimdall ist der Wächter des Asgard, schläft kaum, hört Gras wachsen und sieht hundert Meilen weit. Beim Ragnarök bläst er Gjallarhorn und kämpft gegen Loki — beide sterben.",
        funFact = "Heimdall gilt auch als Vater aller Menschenklassen (Jarls, Karls, Thralls) — er zeugte sie unter dem Namen Rigr während einer Wanderschaft durch die Menschenwelt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das Schicksal von Sisyphos in der griechischen Mythologie als Strafe für seine List gegenüber den Göttern?",
        answerA = "Er muss täglich seinen Adler vertreiben, der seine Leber frisst",
        answerB = "Er muss für die Ewigkeit einen Felsbrocken den Berg hinaufrollen, der immer wieder zurückfällt",
        answerC = "Er ist verdammt, nie den Horizont zu erreichen",
        answerD = "Er muss stets hungrig und durstig unter Früchten und Wasser stehen, die er nicht erreichen kann",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Sisyphos wurde bestraft, weil er den Tod (Thanatos) übertölpelte und die Götter mehrfach betrog. Seine endlose, sinnlose Arbeit gilt als Symbol für das Absurde — Camus machte ihn zum philosophischen Helden.",
        funFact = "Albert Camus schloss seinen Essay 'Der Mythos des Sisyphos' mit dem Satz: 'Man muss sich Sisyphos als einen glücklichen Menschen vorstellen.'"
    ),

    Question(
        categoryId = 11,
        questionText = "Welche ägyptische Gottheit wird mit dem Kopf eines Ibis dargestellt und gilt als Erfinder der Schrift und der Weisheit?",
        answerA = "Anubis",
        answerB = "Horus",
        answerC = "Thot",
        answerD = "Seth",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Thot (auch Djehuti) ist der Mondgott, Schreibergott und Herr der Weisheit. Er hielt das Gleichgewicht der Ma'at aufrecht und dokumentierte das Totengericht. Den Griechen entspricht er Hermes.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Held der griechischen Mythologie tötete die Chimäre — ein Mischwesen aus Löwe, Ziege und Schlange?",
        answerA = "Theseus",
        answerB = "Perseus",
        answerC = "Bellerophon",
        answerD = "Achill",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Bellerophon ritt auf dem geflügelten Pferd Pegasos und tötete die feuerspeiende Chimäre von Lykien. Er versuchte danach, auf Pegasos den Olymp zu erklimmen — Zeus schickte eine Bremse, die Pegasos erschreckte.",
        funFact = "Der Begriff 'Chimäre' lebt in der modernen Sprache weiter: in der Biologie bezeichnet er Organismen mit genetisch unterschiedlichen Zellpopulationen."
    ),

    Question(
        categoryId = 11,
        questionText = "Welchen Namen trug die Göttin der Zwietracht in der griechischen Mythologie, die den goldenen Apfel 'für die Schönste' auf die Hochzeit warf und damit den Trojanischen Krieg auslöste?",
        answerA = "Nemesis",
        answerB = "Eris",
        answerC = "Hecate",
        answerD = "Enyo",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Eris war die Göttin der Zwietracht. Da sie nicht zur Hochzeit von Peleus und Thetis eingeladen wurde, warf sie den Apfel der Zwietracht — das Parisurteil folgte, Paris wählte Aphrodite, und Troja fiel.",
        funFact = "Der Zwergplanet Eris wurde nach der Göttin benannt, da seine Entdeckung eine Kontroverse über die Definition von 'Planet' auslöste — die zur Degradierung Plutos führte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Halbgott der aztekischen Mythologie galt als gefiederter Schlangengott, Schöpfer der Menschheit und Herrscher über Wind und Morgenröte?",
        answerA = "Huitzilopochtli",
        answerB = "Tlaloc",
        answerC = "Quetzalcoatl",
        answerD = "Tezcatlipoca",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Quetzalcoatl (Nahuatl: 'gefiederte Schlange') war eine der wichtigsten aztekischen Gottheiten — Schöpfergott, Kulturbringer, Patron der Priester. Die Legende seiner Wiederkehr soll Cortés' Eroberung beeinflusst haben.",
        funFact = "Eine Theorie besagt, Hernán Cortés sei von den Azteken mit dem zurückkehrenden Quetzalcoatl verwechselt worden — ob das historisch stimmt, ist unter Forschern umstritten."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher japanische Gott des Windes und Sturms, Bruder der Sonnengöttin Amaterasu, wurde aus dem Himmel verbannt und erschlug dann eine achtköpfige Schlange?",
        answerA = "Izanagi",
        answerB = "Fujin",
        answerC = "Susanoo",
        answerD = "Raijin",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Susanoo no Mikoto tötete die achtköpfige Schlange Yamata no Orochi und fand in einem ihrer Schwänze das legendäre Schwert Kusanagi — eines der drei kaiserlichen Insignien Japans.",
        funFact = "Das Kusanagi-Schwert gilt als eines der drei heiligen Schätze Japans (zusammen mit dem Spiegel Yata no Kagami und dem Juwel Yasakani no Magatama) und soll im Atsuta-Schrein in Nagoya aufbewahrt werden."
    ),

    // --- LINGUISTIK & SOZIOLOGIE (12) ---

    Question(
        categoryId = 11,
        questionText = "Welches linguistische Konzept von Ferdinand de Saussure beschreibt die willkürliche Verbindung zwischen Lautbild (Signifikant) und Vorstellung (Signifikat)?",
        answerA = "Arbitrarität des Zeichens",
        answerB = "Ikonizität",
        answerC = "Indexikalität",
        answerD = "Performativität",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "De Saussures Kerngedanke: Das sprachliche Zeichen ist arbiträr (willkürlich) — es gibt keinen natürlichen Zusammenhang zwischen dem Wort 'Baum' und dem Konzept Baum. Andere Sprachen beweisen es.",
        funFact = "De Saussures 'Cours de linguistique générale' (1916) wurde posthum von Studenten anhand ihrer Mitschriften zusammengestellt — Saussure selbst veröffentlichte es nie."
    ),

    Question(
        categoryId = 11,
        questionText = "Was besagt die Sapir-Whorf-Hypothese in ihrer starken Form?",
        answerA = "Sprache ist ein universelles Werkzeug, das alle Menschen gleich denken lässt",
        answerB = "Die Struktur einer Sprache bestimmt zwingend, wie ihre Sprecher die Welt wahrnehmen und denken können",
        answerC = "Alle Sprachen haben universelle Tiefenstrukturen",
        answerD = "Grammatik ist biologisch angeboren",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Die starke Sapir-Whorf-Hypothese (linguistischer Determinismus) behauptet: Sprache bestimmt Denken vollständig. Die schwache Form (linguistische Relativität) — Sprache beeinflusst Denken — ist wissenschaftlich besser gestützt.",
        funFact = "Ein modernes Beispiel: Die Sprache Hopi hat keine Zeitformen für Vergangenheit/Zukunft — beeinflusst das das Zeitverständnis der Sprecher? Forscher streiten bis heute."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Soziologe entwickelte das Konzept des 'Habitus' als verinnerlichte Wahrnehmungs- und Handlungsschemata, die gesellschaftliche Positionen reproduzieren?",
        answerA = "Émile Durkheim",
        answerB = "Max Weber",
        answerC = "Pierre Bourdieu",
        answerD = "Talcott Parsons",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Pierre Bourdieu entwickelte 'Habitus' als unbewusste Dispositionen, die durch soziale Herkunft geprägt werden. Zusammen mit 'Kapital' (ökonomisch, kulturell, sozial) und 'Feld' bildet er sein Hauptkonzept.",
        funFact = "Bourdieus Werk 'Die feinen Unterschiede' (1979) analysierte, wie Geschmack und Konsum soziale Klassen reproduzieren — er gilt als Gründungstext der Kultursoziologie."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet in der Linguistik die 'Deixis'?",
        answerA = "Die Lehre von der Herkunft der Wörter",
        answerB = "Ausdrücke, die ihre Bedeutung aus dem Kontext der Äußerung beziehen (ich, hier, jetzt, dieser)",
        answerC = "Die Untersuchung von Sprachklangmustern",
        answerD = "Der Wandel von Wortbedeutungen über Generationen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Deiktische Ausdrücke (Demonstrativpronomen, Personalpronomen, Orts- und Zeitadverbien) verweisen auf den Kontext der Sprechsituation. 'Ich bin hier jetzt' hat ohne Kontext keine feste Bedeutung.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Soziologe unterschied zwischen 'mechanischer Solidarität' (ähnliche Menschen, einfache Gesellschaft) und 'organischer Solidarität' (Arbeitsteilung, komplexe Gesellschaft)?",
        answerA = "Karl Marx",
        answerB = "Herbert Spencer",
        answerC = "Émile Durkheim",
        answerD = "Georg Simmel",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Durkheim analysierte in 'Über die soziale Arbeitsteilung' (1893) den gesellschaftlichen Wandel: Einfache Gesellschaften halten durch Ähnlichkeit zusammen, moderne Gesellschaften durch funktionale Abhängigkeit.",
        funFact = "Durkheims Studie zum Suizid (1897) gilt als Gründungswerk der empirischen Soziologie — er zeigte erstmals statistische Zusammenhänge zwischen sozialem Zusammenhalt und Selbsttötungsraten."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt Noam Chomskys Konzept der 'Universalgrammatik'?",
        answerA = "Alle Sprachen der Welt haben denselben Wortschatz",
        answerB = "Das menschliche Gehirn ist biologisch mit einem angeborenen Spracherwerbssystem ausgestattet, das allen Sprachen zugrundeliegende Strukturen enthält",
        answerC = "Grammatikregeln können auf alle Sprachen übertragen werden",
        answerD = "Kinder lernen Sprache ausschließlich durch Imitation",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Chomsky postulierte: Kinder lernen Sprache zu schnell und mit zu wenig Input (Armut des Stimulus), als dass dies durch Lernen allein erklärbar wäre. Ein angeborenes Spracherwerbsgerät (LAD) erklärt dies.",
        funFact = "Chomskys Spracherwerbs-Theorie revolutionierte die Linguistik und trug maßgeblich zur kognitiven Wende in der Psychologie bei — weg von Skinners reinem Behaviorismus."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches soziologische Konzept beschreibt Erving Goffman mit dem Begriff 'Stigma'?",
        answerA = "Die Auszeichnung besonders verdienter Personen durch die Gesellschaft",
        answerB = "Ein tiefgreifend diskreditierendes Merkmal, das eine Person in der sozialen Identität auf einen Makel reduziert",
        answerC = "Die körperliche Markierung von Sklaven im antiken Griechenland",
        answerD = "Den Prozess der Eingewöhnung in neue soziale Gruppen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Goffmans 'Stigma' (1963) analysiert, wie gesellschaftliche Abweichungen (körperliche, moralische, tribale) Personen diskreditieren. Stigmatisierte entwickeln Strategien wie Verbergen oder Passing.",
        funFact = "Goffman entwickelte auch das dramaturgische Modell der Gesellschaft: Wir alle spielen auf 'Vorder-' und 'Hinterbühnen' soziale Rollen — Gesellschaft als Theater."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet in der Pragmatik John Austins Begriff des 'performativen Sprechakts'?",
        answerA = "Eine Aussage, die eine Tatsache beschreibt",
        answerB = "Eine Äußerung, die durch ihr Aussprechen selbst eine Handlung vollzieht (z.B. 'Ich erkläre die Sitzung für eröffnet')",
        answerC = "Eine Frage, die eine Antwort erzwingt",
        answerD = "Eine Metapher, die sprachliche Kreativität ausdrückt",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Austin unterschied in 'How to Do Things with Words' (1962) konstative (beschreibende) von performativen Äußerungen. Letztere vollziehen eine Handlung: Taufen, Heiraten, Versprechen, Verurteilen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Soziologe entwickelte den Begriff der 'Entzauberung der Welt' als Kennzeichen der Moderne — das Zurückdrängen von Magie, Religion und Tradition durch rationale Bürokratie?",
        answerA = "Émile Durkheim",
        answerB = "Max Weber",
        answerC = "Ferdinand Tönnies",
        answerD = "Georg Simmel",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Max Weber beschrieb die 'Entzauberung der Welt' als Prozess, in dem die rationale Weltbeherrschung durch Wissenschaft und Bürokratie religiöse und magische Deutungen verdrängt — ein Kernthema der Modernisierungssoziologie.",
        funFact = "Weber sah die protestantische Ethik als Treibkraft des Kapitalismus — Sparsamkeit und Arbeit als Gottesberuf führten zur Kapitalakkumulation, die den Kapitalismus antrieb."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das linguistische Konzept der 'Polysemie'?",
        answerA = "Die Fähigkeit, mehrere Sprachen zu sprechen",
        answerB = "Mehrere bedeutungsähnliche Wörter für denselben Begriff",
        answerC = "Ein einzelnes Wort mit mehreren verwandten, historisch zusammenhängenden Bedeutungen",
        answerD = "Die Übertragung von Wortbedeutungen zwischen Sprachen",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Polysemie bezeichnet mehrdeutige Wörter mit zusammenhängenden Bedeutungen (z.B. 'Bank' = Sitzgelegenheit / Geldinstitut). Zu unterscheiden von Homonymie, wo Bedeutungen nicht zusammenhängen.",
        funFact = "Das englische Wort 'set' gilt als das polysemischste Wort der Sprache — das Oxford English Dictionary listet über 430 Bedeutungen und Verwendungen."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Konzept beschreibt in der Soziologie die Tendenz, dass Gruppen in Isolation extremere Entscheidungen treffen als Einzelne — in dieselbe Richtung wie die anfängliche Mehrheitsmeinung?",
        answerA = "Konformitätseffekt",
        answerB = "Gruppendenken (Groupthink)",
        answerC = "Gruppenpolarisierung",
        answerD = "Soziale Faulheit",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Gruppenpolarisierung (Moscovici & Zavalloni, 1969): Gruppen verstärken die anfängliche Tendenz ihrer Mitglieder. War die Gruppe zu Beginn risikobereit, wird sie nach Diskussion noch risikobereiter.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Schriftsystem gilt als das älteste der Welt, das um ca. 3200 v. Chr. in Mesopotamien entstand?",
        answerA = "Hieroglyphen",
        answerB = "Keilschrift",
        answerC = "Phönizisches Alphabet",
        answerD = "Indus-Schrift",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Die sumerische Keilschrift (ca. 3200 v. Chr.) entstand in Mesopotamien für Handelsdokumente. Sie entwickelte sich von Piktogrammen zu abstrakten Keileindrücken auf Tontafeln und wurde über 3000 Jahre verwendet.",
        funFact = "Die Keilschrift wurde erst 1835 entziffert, als Henry Rawlinson die dreisprachige Behistun-Inschrift (Altpersisch, Elamisch, Babylonisch) übersetzte — ähnlich wie der Stein von Rosetta für die Hieroglyphen."
    )

)
