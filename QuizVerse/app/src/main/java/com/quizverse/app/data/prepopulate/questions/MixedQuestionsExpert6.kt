package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsExpert6(): List<Question> = listOf(

    // --- SOZIOLOGIE & GESELLSCHAFTSTHEORIE (50) ---

    Question(
        categoryId = 11,
        questionText = "Welchen Begriff verwendete Émile Durkheim für den Zustand der Normenlosigkeit, der entsteht, wenn gesellschaftliche Regeln ihre Bindungskraft verlieren?",
        answerA = "Alienation",
        answerB = "Anomie",
        answerC = "Reifikation",
        answerD = "Entfremdung",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Durkheim prägte den Begriff 'Anomie' (von griech. a-nomos = ohne Gesetz) in 'De la division du travail social' (1893) und 'Le Suicide' (1897). Anomie bezeichnet einen gesellschaftlichen Zustand, in dem Normen und Werte ihre Orientierungsfunktion verlieren, etwa beim abrupten sozialen Wandel oder wirtschaftlichen Krisen. Robert Merton entwickelte den Begriff später für die Kriminologie weiter.",
        funFact = "Durkheims Studie über den Selbstmord (1897) gilt als Pionierstudie der empirischen Sozialforschung: Er analysierte amtliche Statistiken und zeigte, dass eine vermeintlich private Handlung von gesellschaftlichen Strukturen abhängt — ein Paradigmenwechsel im Denken über soziale Phänomene."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Max Weber unter 'wertrationalem Handeln' in seiner Handlungstypologie?",
        answerA = "Handeln, das ausschließlich auf wirtschaftlichen Gewinn ausgerichtet ist",
        answerB = "Handeln nach Gewohnheit und eingelebten Sitten",
        answerC = "Handeln, das einem bewussten Glauben an den eigenen unbedingten Eigenwert eines Verhaltens folgt",
        answerD = "Handeln, das durch affektuelle Emotionen geleitet wird",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Weber unterscheidet in 'Wirtschaft und Gesellschaft' (1922) vier Typen sozialen Handelns: zweckrational (Abwägung von Mitteln und Zwecken), wertrational (Handeln aus Überzeugung unabhängig von Konsequenzen, z.B. religiöse Pflicht), affektuell (emotional geleitet) und traditional (gewohnheitsmäßig). Wertrational handelt etwa ein Soldat, der aus Pflicht kämpft, auch wenn der Krieg verloren ist.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welche vier Funktionsprobleme muss laut Talcott Parsons jedes Sozialsystem erfüllen — bekannt als AGIL-Schema?",
        answerA = "Adaption, Goal Attainment, Integration, Latency",
        answerB = "Authority, Governance, Ideology, Legitimacy",
        answerC = "Aggregation, Growth, Inclusion, Learning",
        answerD = "Adjustment, Governance, Integration, Loyalty",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Parsons entwickelte das AGIL-Schema in 'The Social System' (1951): A = Adaption (Anpassung an die Umwelt, Wirtschaft), G = Goal Attainment (Zielerreichung, Politik), I = Integration (Koordination der Teilsysteme, Recht/Gemeinschaft), L = Latency/Pattern Maintenance (Erhalt von Wertmustern, Familie/Bildung). Jedes Teilsystem erfüllt primär eine Funktion für das Gesamtsystem.",
        funFact = "Parsons dominierte die amerikanische Soziologie so stark, dass sein Schüler Robert Merton für die Einführung von 'mittleren Theorien' plädierte — also handlicheren Konzepten statt der großen Gesellschaftstheorie. Diese Spannung zwischen 'grand theory' und empirischer Forschung prägt die Soziologie bis heute."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Pierre Bourdieu mit dem Begriff 'Habitus'?",
        answerA = "Die bewusste Strategie, mit der Akteure soziale Felder navigieren",
        answerB = "Das inkorporierte, unbewusste System von Dispositionen, das Wahrnehmen, Denken und Handeln strukturiert",
        answerC = "Das kulturelle Kapital in Form von Bildungstiteln",
        answerD = "Die Summe aller sozialen Netzwerke eines Individuums",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Bourdieus Habitus-Konzept (entwickelt in 'Entwurf einer Theorie der Praxis', 1972) bezeichnet dauerhafte, erworbene Dispositionen, die durch frühe Sozialisation einverleibt werden. Der Habitus ist nicht bewusst — er ist das 'Körper gewordene Soziale'. Er produziert und reproduziert Praxis, ohne sich explizit auf Regeln zu beziehen. Bourdieus Kernthese: Klasse ist nicht nur ökonomisch, sondern in Körper und Geschmack eingeschrieben.",
        funFact = "Bourdieu erhob in 'Die feinen Unterschiede' (1979) Geschmacksurteile zu soziologischen Daten: Welche Musik, welches Essen, welche Kunst werden von welchen Klassen bevorzugt? Seine Erkenntnis: Kultureller Geschmack ist kein Zufall, sondern ein Klassenmerkmal."
    ),

    Question(
        categoryId = 11,
        questionText = "Niklas Luhmann beschreibt soziale Systeme als 'autopoietisch'. Was bedeutet dies in seiner Systemtheorie?",
        answerA = "Systeme expandieren kontinuierlich in neue gesellschaftliche Bereiche",
        answerB = "Systeme passen sich automatisch an Umweltveränderungen an",
        answerC = "Systeme reproduzieren sich ausschließlich durch eigene Operationen und sind operativ geschlossen",
        answerD = "Systeme werden durch externe politische Steuerung kontrolliert",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Luhmann übernahm den Begriff Autopoiesis von den Biologen Maturana und Varela und übertrug ihn auf soziale Systeme (Soziale Systeme, 1984). Ein autopoietisches System reproduziert sich durch eigene Operationen: Gesellschaft besteht aus Kommunikationen, die weitere Kommunikationen erzeugen. Das System ist operativ geschlossen — es kann nur durch eigene Operationen auf die Umwelt reagieren, nicht direkt von außen gesteuert werden.",
        funFact = "Luhmann antwortete auf die Frage nach seinem Lebenswerk: 'Meine Theorie.' Er arbeitete über 30 Jahre an einer einzigen Frage: Was ist Gesellschaft? Das Ergebnis war ein Zettelkasten mit über 90.000 Notizzetteln, ein Frühmodell der digitalen Wissensorganisation."
    ),

    Question(
        categoryId = 11,
        questionText = "Womit begründete Max Weber die Entstehung des modernen Kapitalismus in seiner berühmten religionssoziologischen These?",
        answerA = "Mit der technischen Überlegenheit europäischer Produktionsmethoden",
        answerB = "Mit dem kolonialen Ressourcenzugang Westeuropas",
        answerC = "Mit der protestantischen Ethik, besonders dem calvinistischen Berufsethos und Prädestinationsglauben",
        answerD = "Mit der demographischen Überlegenheit Nordeuropas",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "In 'Die protestantische Ethik und der Geist des Kapitalismus' (1904/05) argumentierte Weber, dass der Calvinismus eine Wahlverwandtschaft zum kapitalistischen Geist aufweise. Der Prädestinationsglaube erzeugte Heilsangst; wirtschaftlicher Erfolg galt als Zeichen der Auserwählung. Askese, Sparsamkeit und harte Arbeit wurden zur religiösen Pflicht — Eigenschaften, die kapitalistische Akkumulation begünstigen.",
        funFact = "Webers These ist bis heute umstritten. Werner Sombart konterte: Nicht Protestanten, sondern Juden hätten den Kapitalismus begründet. Moderne Historiker weisen darauf hin, dass kapitalistische Strukturen in Norditalien (katholisch) entstanden — noch vor der Reformation."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Jürgen Habermas unter der 'Kolonialisierung der Lebenswelt'?",
        answerA = "Die kulturelle Vereinheitlichung durch Massenmedien",
        answerB = "Den Einbruch systemischer Steuerungsmedien (Geld und Macht) in kommunikative Lebensbereiche",
        answerC = "Die Ausbreitung westlicher Werte in Entwicklungsländer",
        answerD = "Die Bürokratisierung von Bildungsinstitutionen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "In der 'Theorie des kommunikativen Handelns' (1981) unterscheidet Habermas zwischen System (Wirtschaft und Staat, koordiniert durch Geld und Macht) und Lebenswelt (kommunikativ koordinierte soziale Praxis). 'Kolonialisierung' bezeichnet den Prozess, durch den systemische Medien in Lebensweltbereiche wie Familie, Schule und Kultur eindringen und kommunikative Rationalität durch instrumentelle verdrängen.",
        funFact = "Habermas und Luhmann führten in den 1970er Jahren eine berühmte akademische Debatte: 'Theorie der Gesellschaft oder Sozialtechnologie?' (1971). Habermas warf Luhmann vor, mit seiner wertfreien Systemtheorie das Bestehende unkritisch zu zementieren. Luhmann antwortete, Habermas' normative Theorie sei nur verkleidete Philosophie."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Karl Marx mit dem Begriff 'Entfremdung' (Alienation) in den 'Ökonomisch-philosophischen Manuskripten' (1844)?",
        answerA = "Die psychische Isolation des modernen Menschen in der Großstadt",
        answerB = "Den Verlust religiöser Bindungen durch Säkularisierung",
        answerC = "Die Trennung des Arbeiters von seinem Produkt, seiner Tätigkeit, der Gattung und anderen Menschen",
        answerD = "Die soziale Ausgrenzung unterer Schichten durch Bildungsbarrieren",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Marx beschreibt vier Dimensionen der Entfremdung unter kapitalistischen Produktionsverhältnissen: Entfremdung vom Produkt (gehört dem Kapitalisten), von der Tätigkeit (Arbeit ist fremdbestimmt), von der Gattung (der Mensch verliert sein gattungsmäßiges Wesen als freies Produzieren) und von anderen Menschen (Konkurrenz statt Solidarität). Entfremdung ist für Marx keine psychologische, sondern eine strukturelle Kategorie.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welche drei Kapitalformen unterscheidet Bourdieu — und welche gilt als die gesellschaftlich am meisten 'verkannte' Form?",
        answerA = "Ökonomisches, kulturelles und soziales Kapital; am meisten verkannt ist das soziale Kapital",
        answerB = "Ökonomisches, kulturelles und soziales Kapital; am meisten verkannt ist das symbolische Kapital",
        answerC = "Ökonomisches, bildungsbezogenes und politisches Kapital; am meisten verkannt ist das politische Kapital",
        answerD = "Materielles, immaterielles und symbolisches Kapital; am meisten verkannt ist das materielle Kapital",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Bourdieu unterscheidet ökonomisches (Geld, Eigentum), kulturelles (Bildung, Wissen, Titel) und soziales Kapital (Netzwerke, Beziehungen). Das symbolische Kapital ist eigentlich eine vierte Form — es ist die Anerkennung der anderen Kapitalformen durch die soziale Gruppe. Es ist am stärksten 'verkannt' (méconnu), weil es als natürliche Eigenschaft erscheint und nicht als akkumuliertes Kapital.",
        funFact = "Bourdieu war selbst ein Aufsteiger aus dem ländlichen Béarn in Südfrankreich. Sein eigener Habitus-Bruch — Bauernsohn wird Elite-Professor — prägte seine Sensibilität für soziale Reproduktion und die verborgenen Mechanismen der Macht."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Emile Durkheim als 'mechanische Solidarität' — im Gegensatz zur 'organischen Solidarität'?",
        answerA = "Solidarität durch arbeitsteilige gegenseitige Abhängigkeit in modernen Gesellschaften",
        answerB = "Solidarität durch Ähnlichkeit, gemeinsame Überzeugungen und kollektives Bewusstsein in traditionellen Gesellschaften",
        answerC = "Solidarität durch staatliche Zwangsmaßnahmen und Sanktionsmechanismen",
        answerD = "Solidarität durch formale Vertragsbeziehungen zwischen Individuen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "In 'De la division du travail social' (1893) entwickelt Durkheim zwei Solidaritätstypen: Mechanische Solidarität herrscht in traditionellen Gesellschaften — Menschen sind ähnlich, teilen gemeinsame Werte (kollektives Bewusstsein), Abweichung wird repressiv bestraft. Organische Solidarität in modernen Gesellschaften entsteht durch arbeitsteilige Differenzierung — Menschen sind verschieden, aber funktional voneinander abhängig wie Organe im Körper.",
        funFact = "Durkheims Unterscheidung beeinflusste Ferdinand Tönnies, der ähnliche Ideen mit 'Gemeinschaft' (traditionell, affektiv) und 'Gesellschaft' (modern, vertraglich) beschrieb. Tönnies' Werk erschien bereits 1887 — sechs Jahre vor Durkheim — und löste eine bis heute andauernde Prioritätsdebatte aus."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Konzept beschreibt Georg Simmel mit dem 'Fremden' als soziologische Figur?",
        answerA = "Eine Person, die durch Armut aus der Gesellschaft ausgeschlossen wurde",
        answerB = "Eine Figur, die zugleich drinnen und draußen ist — nah durch Präsenz, fern durch Andersartigkeit",
        answerC = "Ein Immigrant, der sich vollständig in die Aufnahmegesellschaft integriert hat",
        answerD = "Eine marginalitätserfahrene Person ohne festen Wohnsitz",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Simmel beschreibt den 'Fremden' (1908) als soziologischen Typus, der einzigartige soziale Funktionen erfüllt: Er ist Teil der Gruppe, aber nicht voll zugehörig. Diese Position gibt ihm eine besondere Objektivität — er sieht, was Eingeborene übersehen. Der Fremde steht für die spannungsvolle Einheit von Nähe und Distanz, die Simmel als fundamentale Form sozialer Vergesellschaftung begreift.",
        funFact = "Simmel schrieb 1903 den Essay 'Die Großstädte und das Geistesleben' — eine der ersten soziologischen Analysen urbaner Erfahrung. Seine These: Die Stadt überwältigt die Sinne, weshalb der Städter eine blasierte, distanzierte Haltung entwickelt. Ein Selbstschutzmechanismus, der heute als 'urbane Kälte' bekannt ist."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das Konzept der 'doppelten Kontingenz' in Luhmanns Systemtheorie?",
        answerA = "Die Unvorhersehbarkeit natürlicher Ereignisse für soziale Systeme",
        answerB = "Die wechselseitige Ungewissheit beim Handeln: Ego weiß nicht, wie Alter handelt, und Alter weiß nicht, wie Ego handelt",
        answerC = "Die Abhängigkeit politischer Systeme von wirtschaftlichen Zyklen",
        answerD = "Die Notwendigkeit von Recht als stabilisierender Institution zwischen Konflikten",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Doppelte Kontingenz (nach Parsons, von Luhmann weiterentwickelt) beschreibt das Grundproblem sozialer Interaktion: Ego kann nicht wissen, wie Alter handeln wird, und Alter nicht, wie Ego reagiert. Jeder wartet auf den anderen. Luhmann sieht darin den eigentlichen Emergenzmotor sozialer Ordnung: Aus dieser Ungewissheit entsteht Kommunikation als eigenständige Realität, die das Problem löst.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Robert K. Merton unter 'latenten Funktionen' im Rahmen seiner funktionalen Analyse?",
        answerA = "Funktionen, die bewusst intendiert und öffentlich anerkannt sind",
        answerB = "Dysfunktionale Nebenfolgen gesellschaftlicher Institutionen",
        answerC = "Unbeabsichtigte, den Akteuren nicht bewusste Konsequenzen sozialer Handlungen oder Institutionen",
        answerD = "Reservefunktionen, die aktiviert werden, wenn andere Institutionen versagen",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Merton unterscheidet in 'Social Theory and Social Structure' (1949) zwischen manifesten (beabsichtigten, anerkannten) und latenten (unbeabsichtigten, unbewussten) Funktionen. Sein Beispiel: Der Hopi-Regentanz hat manifest-religiöse Funktion, latent aber Gruppensolidarität und kollektive Identitätsstiftung. Diese Unterscheidung erlaubt Soziologie ohne naiven Intentionalismus.",
        funFact = "Merton prägte auch das Konzept des 'Matthäus-Effekts' — nach dem Bibelvers 'Wer hat, dem wird gegeben': Bekannte Wissenschaftler bekommen mehr Zitate und Aufmerksamkeit als Unbekannte für dieselbe Arbeit. Ein Phänomen, das in der Wissenschaftssoziologie intensiv diskutiert wird."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche fünf Anpassungsmodi beschreibt Merton in seiner Anomietheorie, wenn kulturelle Ziele und institutionelle Mittel auseinanderfallen?",
        answerA = "Akzeptanz, Ablehnung, Eskapismus, Rebellion, Konformismus",
        answerB = "Konformität, Innovation, Ritualismus, Rückzug, Rebellion",
        answerC = "Anpassung, Widerstand, Flucht, Reform, Revolution",
        answerD = "Inklusion, Exklusion, Marginalisierung, Assimilation, Segregation",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Mertons Anomietheorie: Gesellschaft definiert kulturelle Ziele (z.B. materiellem Erfolg) und legitime Mittel. Wenn Mittel ungleich verteilt sind, entstehen Anpassungsmodi: Konformität (Ziele + Mittel akzeptiert), Innovation (Ziele akzeptiert, illegitime Mittel gewählt — Kriminalität), Ritualismus (Ziele aufgegeben, Mittel rituell befolgt), Rückzug (beides aufgegeben — Drogenabhängigkeit), Rebellion (beides abgelehnt und ersetzt).",
        funFact = "Mertons Anomietheorie gilt als eine der einflussreichsten kriminologischen Theorien. Sie erklärt, warum Armut allein keine Kriminalität erzeugt — erst der kulturelle Druck auf Erfolg, den arme Menschen mit legitimen Mitteln nicht erreichen können, schafft kriminogene Strukturen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das Kernanliegen von Alfred Schütz' phänomenologischer Soziologie?",
        answerA = "Die Analyse makrosozialer Strukturen und sozialer Schichtung",
        answerB = "Die Untersuchung, wie Akteure im Alltag subjektiv Sinn konstruieren und die soziale Welt interpretieren",
        answerC = "Die Entwicklung einer quantitativen Methodik für Umfrageforschung",
        answerD = "Die Kritik kapitalistischer Wirtschaftsordnungen aus soziologischer Perspektive",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Alfred Schütz verbindet Husserls Phänomenologie mit Webers verstehender Soziologie ('Der sinnhafte Aufbau der sozialen Welt', 1932). Er untersucht, wie Menschen in der Alltagswelt subjektiv Sinn konstruieren: durch Typisierungen, Reziprozität der Perspektiven und das 'natürliche Einstellung'. Schütz' Werk wurde zur Grundlage der ethnomethodologischen Soziologie (Garfinkel) und der Wissenssoziologie (Berger/Luckmann).",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt Peter Berger und Thomas Luckmann in 'Die gesellschaftliche Konstruktion der Wirklichkeit' (1966)?",
        answerA = "Die Theorie, dass soziale Schichten durch wirtschaftliche Produktionsverhältnisse determiniert werden",
        answerB = "Den Prozess, durch den Wissen, Institutionen und soziale Realität durch menschliches Handeln konstruiert und institutionalisiert werden",
        answerC = "Die Analyse, wie Massenmedien gesellschaftliche Normen produzieren",
        answerD = "Die Entstehung bürgerlicher Öffentlichkeit im 18. Jahrhundert",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Berger und Luckmann begründeten den sozialen Konstruktivismus in der Soziologie: Soziale Wirklichkeit ist nicht naturgegeben, sondern wird durch menschliches Handeln konstruiert (Externalisierung), verselbstständigt sich dann (Objektivation) und wird von nachfolgenden Generationen als selbstverständlich übernommen (Internalisierung). Institutionen entstehen durch diesen dialektischen Prozess — sie erscheinen als 'Dinge', sind aber geronnene menschliche Praxis.",
        funFact = "Das Werk gilt als eines der meistzitierten soziologischen Bücher des 20. Jahrhunderts und beeinflusste Soziologie, Philosophie, Kommunikationswissenschaft und Erziehungswissenschaft. Der Titel wurde zum Schlagwort für den konstruktivistischen Paradigmenwechsel in den Sozialwissenschaften."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Max Weber mit dem Begriff 'Charisma' in seiner Herrschaftssoziologie?",
        answerA = "Die formale Amtsautorität eines demokratisch gewählten Führers",
        answerB = "Die außeralltägliche Qualität einer Persönlichkeit, durch die sie als mit übernatürlichen Kräften ausgestattet gilt",
        answerC = "Die durch Tradition legitimierte Autorität eines Stammesführers",
        answerD = "Die kommunikative Überzeugungskraft in bürokratischen Organisationen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Weber unterscheidet drei reine Typen legitimer Herrschaft: rational-legale (Bürokratie), traditionale (Patriarch, König) und charismatische Herrschaft. Charisma (griech. 'Gnadengabe') bezeichnet die als außeralltäglich wahrgenommene Qualität, die Gefolgschaft erzeugt. Webers Beispiele: religiöse Propheten, Kriegshelden, populistische Führer. Charismatische Herrschaft ist instabil und tendiert zur 'Veralltäglichung des Charismas'.",
        funFact = "Weber analysierte Napoleon, Muhammad und Jesus als charismatische Herrschaftstypen — ohne damit moralische Urteile zu verbinden. Diese Wertfreiheit war methodisch gewollt: Soziologie sollte beschreiben, nicht bewerten. Ein Prinzip, das in der soziologischen Wissenschaftstheorie bis heute diskutiert wird."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist der Unterschied zwischen 'Klasse' bei Marx und 'Stand' bei Weber?",
        answerA = "Klasse ist eine subjektive Identitätskategorie, Stand eine objektive Wirtschaftskategorie",
        answerB = "Klasse basiert auf Produktionsverhältnissen (Besitz an Produktionsmitteln), Stand auf sozialer Ehre und Lebensstil",
        answerC = "Klasse bezeichnet die Oberschicht, Stand die Mittelschicht",
        answerD = "Klasse ist eine horizontale Differenzierung, Stand eine vertikale Hierarchie",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Marx versteht Klasse als ökonomische Kategorie: bestimmt durch das Verhältnis zu den Produktionsmitteln (Besitzer vs. Lohnarbeiter). Weber ergänzt in 'Wirtschaft und Gesellschaft' mit dem mehrdimensionalen Schichtungsmodell: neben Klasse (ökonomisch) gibt es Stand (soziale Ehre, Lebensstil, Konsummuster) und Partei (politische Macht). Diese drei Dimensionen können unabhängig variieren — ein Adeliger kann arm sein, aber hohen Stand genießen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt Norbert Elias in 'Über den Prozess der Zivilisation' (1939)?",
        answerA = "Die Entstehung des modernen Nationalstaats durch Krieg und Steuererhebung",
        answerB = "Die langfristige Transformation von Verhaltensstandards und Affektkontrolle durch zunehmende Staatsmonopolisierung und gesellschaftliche Verflechtung",
        answerC = "Den Rückgang religiöser Bindungen in Europa seit dem Mittelalter",
        answerD = "Die Entstehung kapitalistischer Produktionsweisen aus feudalen Strukturen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Elias beschreibt den 'Zivilisationsprozess' als doppelten Wandel: soziogenetisch (Herausbildung des Gewaltmonopols des Staates) und psychogenetisch (zunehmende Affektkontrolle und Schamgefühle beim Individuum). Beide Prozesse sind verknüpft: Staatliche Befriedung erzeugt längere Handlungsketten, die selbstkontrolliertes Verhalten erfordern. Am Tischmanieren und Umgangsformen lässt Elias diese Veränderungen über Jahrhunderte ablesen.",
        funFact = "Elias' Werk wurde 1939 veröffentlicht — als der Zivilisationsprozess im Nationalsozialismus gerade brutal umgekehrt schien. Als jüdischer Emigrant in England erlebte er den Holocaust. Er bestand aber darauf: Der Zivilisationsprozess ist nicht linear, sondern kann zurückgehen — 'Dezivilisierungsschübe' sind möglich."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Harold Garfinkel unter 'Ethnomethodologie'?",
        answerA = "Die vergleichende Untersuchung von Kulturen und Ethnien durch teilnehmende Beobachtung",
        answerB = "Die Analyse der Methoden, mit denen Alltagsmenschen soziale Ordnung interaktiv herstellen und aufrechterhalten",
        answerC = "Die Entwicklung mathematischer Methoden für die Sozialforschung",
        answerD = "Die politische Soziologie ethnischer Minderheiten in modernen Gesellschaften",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Garfinkel prägte den Begriff 'Ethnomethodologie' in 'Studies in Ethnomethodology' (1967): die Untersuchung der Methoden (methodologie), die gewöhnliche Menschen (ethno) verwenden, um soziale Ordnung herzustellen. Sein berühmtes Mittel: 'Krisenexperimente' (breaching experiments), bei denen er Studenten aufforderte, alltägliche Selbstverständlichkeiten zu verletzen — etwa Fremde wie Vertraute zu behandeln — um die unsichtbaren Regeln sichtbar zu machen.",
        funFact = "Garfinkels Krisenexperimente zeigten, wie fragil soziale Ordnung ist: Wenn Studenten bei einer Schachpartie zufällig Figuren bewegen, reagieren die Gegenspieler mit Verwunderung, dann Irritation, dann Wut. Die Störung alltäglicher Erwartungen erzeugt starke emotionale Reaktionen — Ordnung wird aktiv verteidigt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist der Kern von Erving Goffmans dramaturgischem Ansatz in 'The Presentation of Self in Everyday Life' (1959)?",
        answerA = "Gesellschaft ist wie ein Drama mit vorgeschriebenen Rollen, die keine Improvisation zulassen",
        answerB = "Menschen stellen sich wie Schauspieler auf einer Bühne dar — mit Vorderbühne (Impression Management) und Hinterbühne (Backstage-Verhalten)",
        answerC = "Soziale Interaktion folgt deterministischen Drehbüchern, die durch Sozialisation eingeübt werden",
        answerD = "Theater ist die ursprünglichste Form gesellschaftlicher Integration",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Goffman verwendet die Theatermetapher: Im sozialen Leben unterscheiden Menschen zwischen 'front stage' (Vorderbühne — wo Impression Management stattfindet, man eine bestimmte Identität präsentiert) und 'back stage' (Hinterbühne — wo die Maske fällt). Die Performance ist nicht unbedingt unehrlich, sondern notwendige soziale Kompetenz. Goffman begründete damit die Interaktionssoziologie und das Konzept der 'sozialen Identität'.",
        funFact = "Goffman analysierte totale Institutionen (Gefängnisse, Psychiatrien) in 'Asylums' (1961) und zeigte, wie sie systematisch die Identität der Insassen abbauen — durch Depersonalisierungsrituale, Statusdegradation und Enteignung persönlicher Gegenstände. Diese Analyse wurde zur Grundlage für Kritiken psychiatrischer Zwangsbehandlung."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Bourdieu mit dem Begriff 'Feld' in seiner Feldtheorie?",
        answerA = "Einen geografischen Raum, in dem bestimmte Klassen dominant sind",
        answerB = "Einen strukturierten sozialen Raum mit eigenen Regeln, Einsätzen und Kapitalformen, in dem Akteure um Positionen kämpfen",
        answerC = "Die Gesamtheit aller sozialen Netzwerke einer Person",
        answerD = "Eine ethnographische Forschungssituation, in der Forscher Daten erheben",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Bourdieus Feldkonzept (entwickelt in 'Zur Soziologie der symbolischen Formen', 1970er) beschreibt relativ autonome Bereiche sozialer Praxis — das literarische Feld, das akademische Feld, das politische Feld. Jedes Feld hat eigene Spielregeln (Illusio), spezifische Kapitalformen und eine Feldlogik. Akteure mit entsprechendem Kapital kämpfen um Positionen im Feld. Habitus und Feld korrespondieren miteinander.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht C. Wright Mills unter der 'soziologischen Phantasie' (The Sociological Imagination, 1959)?",
        answerA = "Die Fähigkeit, mit kreativen Methoden gesellschaftliche Daten zu erheben",
        answerB = "Die Fähigkeit, private Probleme in gesellschaftliche Zusammenhänge einzubetten — persönliche Schicksale als public issues zu verstehen",
        answerC = "Die Kapazität, zukünftige gesellschaftliche Entwicklungen vorherzusagen",
        answerD = "Die Phantasie, eine utopische Gesellschaft jenseits von Klasse und Herrschaft zu entwerfen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Mills definiert soziologische Phantasie als Verbindung von 'personal troubles' (individuelle Probleme) und 'public issues' (gesellschaftliche Fragen). Arbeitslosigkeit als persönliches Problem zu behandeln ignoriert strukturelle Ursachen; als Massenphänomen ist es ein gesellschaftliches Problem. Die soziologische Phantasie ermöglicht, Biographie im Kontext von Geschichte und Gesellschaft zu verstehen.",
        funFact = "Mills kritisierte in 'The Power Elite' (1956) die These der pluralistischen Demokratie: Stattdessen herrsche in den USA eine kleine Machtelite aus Wirtschafts-, Politik- und Militärführern. Diese These beeinflusste die Neue Linke der 1960er und ist in Diskussionen über Oligarchie und Lobbyismus bis heute aktuell."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das Konzept der 'funktionalen Differenzierung' in Luhmanns Gesellschaftstheorie?",
        answerA = "Die Aufteilung der Gesellschaft in soziale Klassen mit unterschiedlichen Funktionen",
        answerB = "Die Entstehung autonomer Teilsysteme (Recht, Wirtschaft, Politik, Wissenschaft) mit je eigenen Codes und Funktionen",
        answerC = "Die Spezialisierung von Arbeit in der industriellen Produktion",
        answerD = "Die bürokratische Aufgabentrennung in modernen Organisationen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Luhmann beschreibt moderne Gesellschaft als funktional differenziert: Im Unterschied zu stratifizierten (geschichteten) oder segmentären (egalitären) Gesellschaften entstehen in der Moderne autonome Teilsysteme. Wirtschaft kommuniziert mit dem Code Zahlen/Nicht-Zahlen, Recht mit Recht/Unrecht, Wissenschaft mit wahr/unwahr, Politik mit Macht/Ohnmacht. Jedes System ist operativ geschlossen, kann aber strukturell gekoppelt mit anderen Systemen sein.",
        funFact = "Luhmanns Theorie erklärt, warum 'Politikversagen' in der Wirtschaft strukturell unvermeidlich ist: Wenn die Politik versucht, die Wirtschaft direkt zu steuern, scheitert sie an der Eigenlogik des Wirtschaftssystems. Diese Einsicht ist für Debatten über Marktregulierung relevant."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt Michel Foucault mit dem Begriff 'Diskurs' in seinen Machtanalysen?",
        answerA = "Formale politische Debatte im parlamentarischen Raum",
        answerB = "Strukturierte Wissens- und Redepraxen, die bestimmen, was als wahr gilt und wer sprechen darf",
        answerC = "Massenmediale Kommunikation über gesellschaftlich relevante Themen",
        answerD = "Habermas' Konzept herrschaftsfreier Kommunikation",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Foucault verwendet 'Diskurs' nicht im umgangssprachlichen Sinn, sondern als Ensemble von Aussagen, die einen Gegenstand konstituieren und regulieren. Diskurse schaffen das, worüber sie sprechen ('Sexualität', 'Wahnsinn', 'Kriminalität') erst als erkennbare Objekte. Sie sind Machtformationen: Sie bestimmen, welche Aussagen als wahr gelten, wer sprechen darf und was sagbar ist. Foucault nennt dies 'Diskurspolizei' (Ordnung des Diskurses, 1970).",
        funFact = "Foucaults 'Überwachen und Strafen' (1975) analysiert den Übergang vom öffentlichen Foltertod zum modernen Gefängnis als Paradigmenwechsel in der Machtausübung: von Körperstrafen zur Seelendisziplinierung. Das Panoptikum (Bentham) wurde zum Symbol einer Gesellschaft, in der Menschen sich selbst überwachen, weil sie beobachtet werden könnten."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Anthony Giddens mit dem Konzept der 'Strukturation' in seiner Strukturationstheorie?",
        answerA = "Den Prozess, durch den staatliche Institutionen soziale Klassen strukturieren",
        answerB = "Die Dualität von Struktur: Strukturen ermöglichen und beschränken Handeln, werden aber gleichzeitig durch Handeln reproduziert",
        answerC = "Die Entstehung neuer sozialer Schichten durch technologischen Wandel",
        answerD = "Die Hierarchisierung sozialer Netzwerke nach Macht und Prestige",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Giddens überwindet in 'The Constitution of Society' (1984) den Dualismus von Handlung und Struktur: Strukturen (Regeln und Ressourcen) sind nicht äußere Zwänge, sondern ermöglichen Handeln erst. Gleichzeitig werden Strukturen nur durch Handeln reproduziert. Diese 'Dualität der Struktur' macht Wandel und Kontinuität gleichzeitig erklärbar: Akteure können Strukturen durch ihr Handeln verändern oder stabilisieren.",
        funFact = "Giddens prägte auch den Begriff der 'reflexiven Moderne': In der Spätmoderne hinterfragen Individuen und Institutionen zunehmend die Grundlagen ihrer eigenen Praxis. Tradition verliert Selbstverständlichkeit. Risiko und Unsicherheit werden zum Dauerzustand — eine These, die in der COVID-Pandemie neue Aktualität gewann."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt Ulrich Beck in seiner 'Risikogesellschaft' (1986)?",
        answerA = "Den Anstieg von Kriminalität und sozialen Problemen durch Urbanisierung",
        answerB = "Den Übergang von einer Industriegesellschaft, die Reichtum produziert und verteilt, zu einer Gesellschaft, die Risiken produziert und verteilt",
        answerC = "Die Zunahme von Wohlstand und Konsummöglichkeiten in modernen Gesellschaften",
        answerD = "Die Entstehung neuer Risikogruppen durch Migration und demographischen Wandel",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Beck argumentiert, dass in der Risikogesellschaft die Logik der Risikoverteilung die Logik der Reichtumsverteilung überlagert: Umweltkatastrophen, Atomkraft, Gentechnik erzeugen Risiken, die unkontrollierbar und grenzüberschreitend sind. 'Not ist hierarchisch, Smog ist demokratisch': Reiche können Armen Risiken aufbürden, aber viele Risiken (Klimawandel) betreffen alle. Becks Buch erschien Wochen vor Tschernobyl — und wurde dadurch zur Prophezeiung.",
        funFact = "Beck entwickelte mit Elisabeth Beck-Gernsheim die Theorie der 'Individualisierung': In der Zweiten Moderne zerfallen traditionelle Bindungen (Klasse, Familie, Religion), und Individuen müssen ihre Biographie selbst konstruieren. Dies ist Freiheit und Zwang zugleich — das 'biographische Projekt' als moderne Lebensform."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Erving Goffman mit dem Begriff 'Stigma' in seinem gleichnamigen Werk (1963)?",
        answerA = "Formale rechtliche Strafen für abweichendes Verhalten",
        answerB = "Ein zutiefst diskreditierendes Attribut, das einen Menschen auf eine Eigenschaft reduziert und soziale Ausgrenzung bewirkt",
        answerC = "Religiöse Symbole zur Markierung von Außenseitern in traditionellen Gesellschaften",
        answerD = "Messbare Merkmale sozialer Schichtzugehörigkeit wie Einkommen und Bildung",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Goffman unterscheidet drei Stigmatypen: körperliche Abzeichen (Behinderung, Entstellung), Charakterfehler (Kriminalität, Sucht, psychische Erkrankung) und tribale Stigmata (ethnische, religiöse, nationale Zugehörigkeit). Stigma ist nicht die Eigenschaft selbst, sondern das soziale Verhältnis zwischen Attribut und Stereotyp. Goffman analysiert Strategien der Stigmaverwaltung: 'Passing' (Verbergen), Informationskontrolle, Stigmasymbole.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Karl Mannheim mit dem Begriff 'Wissenssoziologie'?",
        answerA = "Die empirische Untersuchung des Bildungsniveaus verschiedener sozialer Schichten",
        answerB = "Die Analyse, wie Wissen und Denken durch die soziale Position (Sein) des Denkers geprägt werden",
        answerC = "Die Soziologie wissenschaftlicher Institutionen und Forschungsgemeinschaften",
        answerD = "Die Untersuchung volkstümlichen Alltagswissens im Gegensatz zu akademischem Wissen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Mannheim entwickelt in 'Ideologie und Utopie' (1929) die Wissenssoziologie: Denken ist immer perspektivisch und standortgebunden ('Seinsverbundenheit des Denkens'). Klassenbewusstsein, Generationszugehörigkeit, Weltanschauung prägen, was als wahr gilt. Mannheims Problem: Gilt Relativismus auch für Wissenssoziologie selbst? Er rettet Objektivität durch den Begriff der 'freischwebenden Intelligenz' als relativ klassenloser Schicht.",
        funFact = "Mannheims Generationskonzept ('Das Problem der Generationen', 1928) beeinflusst bis heute Jugendforschung: Generationen teilen nicht nur Jahrgang, sondern gemeinsame historische Erlebnisse ('Generationslagerung'). Prägend ist das 'Erlebnis der Jugendjahre' — was um das 17. Lebensjahr erlebt wird, formt dauerhaft das Weltbild."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der Begriff 'relative Deprivation' in der Sozialpsychologie und Soziologie?",
        answerA = "Absolute Armut im Vergleich zu einem globalen Mindeststandard",
        answerB = "Das Gefühl der Benachteiligung, das entsteht, wenn man sich mit einer relevanten Referenzgruppe vergleicht und sich benachteiligt wahrnimmt",
        answerC = "Die soziale Isolation durch eingeschränkte Mobilität und mangelnden Bildungszugang",
        answerD = "Den Verlust sozialer Privilegien durch gesellschaftlichen Wandel",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Relative Deprivation (Stouffer et al., 1949; Runciman, 1966) erklärt, warum subjektives Ungerechtigkeitsgefühl nicht mit absoluter Armut korreliert: Wer arm unter Armen lebt, fühlt sich weniger benachteiligt als wer arm unter Reichen lebt. Referenzgruppenvergleiche prägen Zufriedenheit und Protestbereitschaft. Robert Merton verband das Konzept mit der Anomietheorie: Relative Deprivation erzeugt Frustration, die zu Devianz führen kann.",
        funFact = "Studien zeigen: In sehr ungleichen Gesellschaften leiden auch Reiche stärker unter Statusangst. Gleichheit macht nicht nur Arme, sondern alle glücklicher — eine These, die Richard Wilkinson und Kate Pickett in 'The Spirit Level' (2009) empirisch untermauerten."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Auguste Comte, der Begründer der Soziologie, unter dem 'Gesetz der drei Stadien'?",
        answerA = "Die historische Entwicklung von Stammesgesellschaft über Feudalismus zum Kapitalismus",
        answerB = "Die geistige Entwicklung der Menschheit durch theologisches, metaphysisches und positives Stadium",
        answerC = "Die soziologische Dreiteilung in Makro-, Meso- und Mikroebene",
        answerD = "Die dreiphasige Entstehung sozialer Bewegungen von Entstehung über Wachstum bis Institutionalisierung",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Comte formulierte in 'Cours de philosophie positive' (1830-1842) das 'Gesetz der drei Stadien': Im theologischen Stadium erklärt der Mensch die Welt durch übernatürliche Kräfte (Animismus, Polytheismus, Monotheismus). Im metaphysischen Stadium durch abstrakte Kräfte und Wesenheiten. Im positiven Stadium durch empirisch beobachtbare Gesetze. Comtes Ziel: Die Soziologie als 'Königin der Wissenschaften' auf positiver (empirischer) Basis zu errichten.",
        funFact = "Comte erfand nicht nur den Begriff 'Soziologie', sondern auch den Begriff 'Altruismus'. Er gründete zudem eine 'Religion der Menschheit' mit Heiligenkalender (Newton, Descartes, Goethe als Heilige) — ein letzter Rückfall in die theologische Denkweise, die er überwinden wollte."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Herbert Marcuse in 'Der eindimensionale Mensch' (1964) mit dem Begriff 'repressive Desublimierung'?",
        answerA = "Die durch staatliche Zensur erzwungene Unterdrückung sexueller Inhalte in der Massenkultur",
        answerB = "Die scheinbare Befreiung durch sexuelle Liberalisierung und Konsumkultur, die in Wirklichkeit kritisches Bewusstsein neutralisiert",
        answerC = "Die Verdrängung revolutionärer Energien durch bürokratische Institutionalisierung sozialer Bewegungen",
        answerD = "Die psychologische Anpassung an Autorität durch frühkindliche Sozialisation",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Marcuse (Frankfurter Schule) argumentiert: Fortgeschrittene Industriegesellschaft integriert Opposition durch 'repressive Toleranz' und Konsumkultur. 'Repressive Desublimierung' bezeichnet die kommerzielle Freisetzung sexueller Energie, die eigentlich subversives Potential hätte — stattdessen wird sie in Konsum kanalisiert und stabilisiert das System. Sexuelle Revolution ohne politische Revolution ist für Marcuse ein Herrschaftsinstrument.",
        funFact = "Marcuse wurde zur Vaterfigur der Studentenbewegung 1968 — in einer Formel: 'Marx, Mao, Marcuse'. Ironischerweise war er der einzige der drei, der noch lebte und Interviews geben konnte. Er warnte die Studenten: Rebellion ohne theoretische Grundlage ändert nichts."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Max Horkheimer und Theodor W. Adorno in der 'Dialektik der Aufklärung' (1944) mit dem Begriff 'Kulturindustrie'?",
        answerA = "Die staatliche Förderung von Hochkultur als Instrument nationaler Identitätspolitik",
        answerB = "Die industrielle Standardisierung von Kulturprodukten, die Massenbewusstsein manipuliert und kritisches Denken verhindert",
        answerC = "Die Entwicklung einer eigenständigen Arbeiterkultur als Gegenpol zur bürgerlichen Hochkultur",
        answerD = "Die Kommerzialisierung von Volkskunst und Traditionen durch den Tourismus",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Horkheimer und Adorno argumentieren: Kultur wird wie Waren produziert — standardisiert, kalkuliert, auf Effekt optimiert. Kulturindustrie verspricht Vergnügen, liefert aber nur vorgeformte Bedürfnisbefriedigung, die das Bewusstsein in Konformismus einschließt. Film, Radio, Zeitschriften werden zu Instrumenten der sozialen Kontrolle. Die Illusion der Wahl verstärkt die tatsächliche Konformität.",
        funFact = "Adorno analysierte auch Jazz und Populärmusik im Sinne der Kulturindustriekritik — ein Standpunkt, der ihm bis heute heftige Kritik einbringt. Seine These, Jazz sei eine standardisierte Pseudo-Individualität, gilt vielen als eurozentrisch und elitär. Die Debatte über Hochkultur vs. Populärkultur ist nie verstummt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Talcott Parsons mit dem Begriff 'pattern variables' (Musteralternativen)?",
        answerA = "Statistische Variablen zur Messung sozialer Ungleichheit in empirischen Studien",
        answerB = "Binäre Entscheidungsalternativen, die Akteure bei sozialen Interaktionen wählen müssen und gesellschaftliche Orientierungsmuster beschreiben",
        answerC = "Kulturelle Muster, die den Übergang von traditionellen zu modernen Gesellschaften kennzeichnen",
        answerD = "Variablen, die die Stabilität politischer Systeme in verschiedenen Kulturen messen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Parsons beschreibt fünf Musteralternativen als binäre Entscheidungsdichotomien: Affektivität vs. Affektneutralität, Kollektivorientierung vs. Selbstorientierung, Partikularismus vs. Universalismus, Askription vs. Leistung, Diffusität vs. Spezifität. Traditionelle Gesellschaften tendieren zu Affektivität, Partikularismus, Askription; moderne zu Affektneutralität, Universalismus, Leistung. Diese Konzepte beeinflussten Modernisierungstheorien.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet die soziologische Rollentheorie mit dem Begriff 'Rollenkonflikt'?",
        answerA = "Der Konflikt zwischen formaler und informaler Hierarchie in Organisationen",
        answerB = "Die Spannung, die entsteht, wenn eine Person gleichzeitig mehrere unvereinbare Rollenerwartungen erfüllen soll",
        answerC = "Der generationelle Konflikt zwischen gesellschaftlich etablierten und neuen sozialen Rollen",
        answerD = "Die Ablehnung zugewiesener sozialer Rollen als Form des sozialen Protests",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Rollentheoretiker wie Ralf Dahrendorf ('Homo Sociologicus', 1958) unterscheiden Inter-Rollenkonflikte (Anforderungen verschiedener Rollen kollodieren, z.B. Arzt und Vater) und Intra-Rollenkonflikte (verschiedene Erwartungen an dieselbe Rolle). Dahrendorf beschreibt den 'Homo Sociologicus' als Träger sozialer Rollen, der zwischen institutionellen Erwartungen und individuellem Ich eingeklemmt ist.",
        funFact = "Dahrendorf kritisierte Marx für ein zu simplistisches Klassenbild und entwickelte eine Konflikttheorie, die auf Herrschaft und Autorität statt auf Eigentum basiert. Er argumentierte: Nicht wer Produktionsmittel besitzt, sondern wer Autorität ausübt, bestimmt gesellschaftliche Konflikte — eine Theorie, die auf Bürokratien besser passt als die marxsche."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Randall Collins unter dem Konzept der 'Interaktionsritualsketten' (Interaction Ritual Chains)?",
        answerA = "Formelle Zeremonien, durch die Gesellschaften ihre Werte kollektiv bekräftigen",
        answerB = "Wiederholte Face-to-Face-Interaktionen, die emotionale Energie und kollektive Symbole erzeugen und soziale Solidarität schaffen",
        answerC = "Hierarchische Befehlsketten in bürokratischen Organisationen",
        answerD = "Ritualisierte Konfliktmuster zwischen sozialen Klassen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Collins erweitert Durkheims Ritualanalyse und Goffmans Interaktionsmikrosoziologie: Jede Interaktion ist ein Ritual, das 'emotionale Energie' (EE) und 'kollektive Symbole' produziert oder verbraucht. Erfolgreiche Rituale — gemeinsames Fokus, körperliche Kopräsenz, Rhythmuskoordination — erzeugen hohe EE und Gruppenzugehörigkeitsgefühl. Diese 'Interaktionsritualsketten' bilden das Mikrofundament aller sozialen Strukturen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der Begriff 'Panoptismus' in Foucaults Machtanalyse?",
        answerA = "Das umfassende Überwachungssystem totalitärer Staaten im 20. Jahrhundert",
        answerB = "Ein Mechanismus der Macht, bei dem die bloße Möglichkeit der Beobachtung Selbstdisziplinierung bewirkt",
        answerC = "Die kollektive Beobachtung von Machthabern durch die Bevölkerung in Demokratien",
        answerD = "Die mediale Dauerbeobachtung prominenter Persönlichkeiten in modernen Gesellschaften",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Foucault analysiert in 'Überwachen und Strafen' (1975) Benthams Panoptikum-Gefängnisarchitektur: Ein zentraler Turm erlaubt, alle Zellen einzusehen. Gefangene wissen nicht, ob sie beobachtet werden — und verhalten sich, als würden sie immer beobachtet. Foucault generalisiert dies: Moderne Gesellschaft funktioniert als dezentrales Panoptikum. Schule, Krankenhaus, Fabrik normieren durch das Prinzip der möglichen Beobachtung.",
        funFact = "Foucaults Panoptismus erlebt im Zeitalter von Überwachungskapitalismus (Shoshana Zuboff) und staatlicher Massenüberwachung (NSA-Skandal, Snowden 2013) neue Aktualität. Die entscheidende Differenz: Im digitalen Panoptikum weiß man, dass man beobachtet wird — und verhält sich trotzdem nicht anders."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das Konzept der 'Intersektionalität', geprägt von Kimberlé Crenshaw (1989)?",
        answerA = "Die statistische Überschneidung verschiedener demographischer Merkmale in Bevölkerungsstudien",
        answerB = "Das Zusammenwirken verschiedener Herrschaftsachsen (Rasse, Klasse, Geschlecht) zu spezifischen Formen von Diskriminierung und Privileg",
        answerC = "Die interdisziplinäre Verbindung von Soziologie, Rechtswissenschaft und Psychologie",
        answerD = "Die Vernetzung verschiedener sozialer Bewegungen zu übergreifenden Koalitionen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Crenshaw analysierte, dass schwarze Frauen weder durch rassismus- noch durch sexismusbezogene Rechtskategorien angemessen geschützt wurden: Antidiskriminierungsrecht konzipierte 'schwarze Personen' als männlich und 'Frauen' als weiß. Intersektionalität beschreibt, wie Herrschaftsachsen nicht additiv wirken, sondern sich gegenseitig konstituieren. Eine schwarze Frau erlebt nicht 'Rassismus + Sexismus', sondern eine spezifische Form von Unterdrückung, die beides umfasst.",
        funFact = "Intersektionalität wurde ursprünglich als Rechtskategorie entwickelt, nicht als soziologische Theorie. Die Reise des Begriffs aus der Rechtswissenschaft in akademische Disziplinen und schließlich in den Mainstream-Aktivismus ist selbst ein soziologisch interessantes Phänomen der Begriffsmigration."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt Fernand Braudel mit der Unterscheidung von 'longue durée', 'conjoncture' und 'événement'?",
        answerA = "Drei Methoden historischer Quellenanalyse für die Geschichtswissenschaft",
        answerB = "Drei Zeitebenen gesellschaftlicher Geschichte: Strukturen (Jahrhunderte), Konjunkturen (Jahrzehnte) und Ereignisse (kurzfristige Begebenheiten)",
        answerC = "Drei Entwicklungsphasen kapitalistischer Wirtschaftssysteme",
        answerD = "Drei geographische Zonen des historischen Mittelmeerraums",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Braudel unterscheidet drei Zeitebenen historischer Analyse ('La Méditerranée', 1949): Die longue durée umfasst langsame, kaum wahrnehmbare Strukturen (Geographie, Klimazyklen, Mentalitäten — Jahrhunderte). Die conjoncture bezeichnet mittelfristige ökonomische Wellen (Jahrzehnte). Das événement sind die kurzfristigen politischen Ereignisse. Historiker konzentrieren sich oft auf Ereignisse — Braudel fordert: Die tiefsten gesellschaftlichen Kräfte liegen in der longue durée.",
        funFact = "Braudels Ansatz wurde zur Grundlage der Annales-Schule, die die Geschichtswissenschaft revolutionierte: weg von der Ereignisgeschichte ('Geschichte der Könige'), hin zur Sozialgeschichte großer Massen. Die Annales-Schule beeinflusste Weltgeschichtsschreibung, Wirtschaftsgeschichte und schließlich die Soziologie."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Immanuel Wallerstein mit der 'Weltsystemtheorie' und dem Kern-Peripherie-Modell?",
        answerA = "Die geographische Konzentration von Industrieproduktion in städtischen Zentren",
        answerB = "Eine Theorie der kapitalistischen Weltökonomie, in der Kernländer Mehrwert aus Peripherieländern extrahieren",
        answerC = "Die historische Entstehung von Nationalstaaten durch Kolonialisierung",
        answerD = "Die kulturelle Dominanz westlicher Werte in einem globalen Mediensystem",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Wallerstein entwickelt in 'The Modern World-System' (1974) eine Theorie, nach der der Kapitalismus als einheitliches globales System zu verstehen ist: Kernländer (Nordamerika, Westeuropa) profitieren von ungleichem Tausch mit Peripherieländern (globaler Süden). Semiperiphere Länder (z.B. Brasilien) nehmen eine Zwischenposition ein. Entwicklung und Unterentwicklung sind zwei Seiten desselben Weltmarkts — daher ist 'Entwicklungspolitik' nach westlichem Modell strukturell begrenzt.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Georg Simmel mit der 'Wechselwirkung' (Vergesellschaftung) als Kernbegriff seiner Soziologie?",
        answerA = "Die ökonomischen Austauschbeziehungen zwischen Produzenten und Konsumenten",
        answerB = "Das reziproke Aufeinanderwirken von Individuen, durch das Gesellschaft erst entsteht und sich reproduziert",
        answerC = "Die Angleichung von Verhaltensweisen durch sozialen Druck und Konformität",
        answerD = "Die Dialektik von Individuum und Gesellschaft in Hegels Philosophie",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Simmel definiert Soziologie als Wissenschaft von den Formen der Vergesellschaftung. 'Gesellschaft' ist nicht ein Ding, sondern ein Prozess — die Summe der Wechselwirkungen zwischen Menschen. Simmel interessieren die formalen Muster: Konkurrenz, Unterordnung, Geheimnis, Treue, Dankbarkeit, Konflikt — diese Formen tauchen in verschiedensten Inhaltsbereichen auf und sind das eigentliche Objekt der Soziologie.",
        funFact = "Simmel analysierte als einer der ersten Soziologen das Geld als Kulturphänomen ('Philosophie des Geldes', 1900): Geld verallgemeinert alle Qualitäten zu Quantitäten und verändert damit die menschliche Welterfahrung. Seine These: Die Geldwirtschaft fördert eine intellektuelle, rationale, blasierte Haltung gegenüber der Welt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Hartmut Rosa mit dem Begriff der 'sozialen Beschleunigung' in seiner Gesellschaftsdiagnose?",
        answerA = "Den technologischen Fortschritt, der Produktionsprozesse schneller macht",
        answerB = "Drei Dimensionen der Beschleunigung: technische (Transport, Kommunikation), gesellschaftliche Wandlungsrate und Lebenstempo",
        answerC = "Die Beschleunigung sozialen Aufstiegs und Abstiegs in flexiblen Arbeitsmärkten",
        answerD = "Die zunehmende Geschwindigkeit globaler Warenströme im Kapitalismus",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Rosa entwickelt in 'Beschleunigung' (2005) eine umfassende Theorie der Moderne: Gesellschaftliche Beschleunigung verläuft in drei Dimensionen — technische (Kommunikation, Transport), sozialer Wandel (Institutionen, Werte, Moden veralten schneller), Lebenstempo (Handlungen pro Zeiteinheit steigen). Der paradoxe Effekt: Trotz Zeitersparnis durch Technik fehlt mehr Zeit. Beschleunigung wird zum Selbstzweck des kapitalistischen Systems.",
        funFact = "Rosa entwickelt als Gegenbegriff zur Beschleunigung das Konzept der 'Resonanz': ein gelingendes Verhältnis zur Welt, das Berührung, Transformation und Antwort ermöglicht. Resonanz ist das Gegenteil von Entfremdung — und kann nicht erzwungen oder gekauft werden."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Axel Honneth in seiner Anerkennungstheorie ('Kampf um Anerkennung', 1992) mit den drei Sphären der Anerkennung?",
        answerA = "Recht, Wirtschaft, Kultur als drei Bereiche sozialer Integration",
        answerB = "Liebe (Familie/Freundschaft), Recht (universale Bürgerrechte) und Solidarität (soziale Wertschätzung)",
        answerC = "Selbstachtung, Selbstwert und Selbstsicherheit als drei Dimensionen der Identität",
        answerD = "Familie, Staat, Zivilgesellschaft als Anerkennungsinstanzen in der Moderne",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Honneth entwickelt eine normative Sozialtheorie auf der Grundlage von Hegels Anerkennungsphilosophie: Soziale Konflikte entstehen aus Anerkennungsverweigerung. Er unterscheidet drei Anerkennungssphären: Liebe (emotionale Bindung in Primärbeziehungen — Basis des Selbstvertrauens), Recht (universale rechtliche Gleichbehandlung — Basis des Selbstrespekts), Solidarität (soziale Wertschätzung der Leistungen — Basis des Selbstwertgefühls). Missachtung in jeder Sphäre verletzt Identität.",
        funFact = "Honneth führte als Direktor des Frankfurter Instituts für Sozialforschung (2001-2018) die Tradition der Kritischen Theorie fort. Die Anerkennungstheorie ist zugleich eine Theorie sozialer Bewegungen: Arbeiterkämpfe, feministische Bewegungen, LGBTQ+-Rechte — alle lassen sich als 'Kämpfe um Anerkennung' in unterschiedlichen Sphären verstehen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Max Weber mit dem Begriff der 'Rationalisierung' als zentralem Merkmal der westlichen Moderne?",
        answerA = "Die Ausbreitung rationaler Argumente in politischen Debatten durch Alphabetisierung",
        answerB = "Den historischen Prozess der Ausweitung zweckrationalen Denkens und bürokratischer Ordnung auf alle Lebensbereiche — mit dem 'Stahlharten Gehäuse' als Metapher",
        answerC = "Die Entwicklung eines rationalen Rechtssystems als Grundlage moderner Gesellschaften",
        answerD = "Die Verbreitung wissenschaftlicher Erkenntnisse in der Bevölkerung durch Bildungsexpansion",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Weber sieht die Rationalisierung ('Entzauberung der Welt') als Schicksal der okzidentalen Moderne: Zweckrationalität verdrängt religiöse, traditionale und affektuelle Orientierungen. Bürokratie ist die rationalste Herrschaftsform — präzise, unpersönlich, regelgebunden. Das 'stahlharte Gehäuse' (iron cage) bezeichnet die Gefangenschaft in rationalen Institutionen, die kein Entkommen mehr erlauben. Weber sah diese Entwicklung ambivalent: Effizienzgewinn gepaart mit 'Sinnverlust'.",
        funFact = "Weber hielt die berühmten Vorträge 'Wissenschaft als Beruf' und 'Politik als Beruf' (1917/1919) an der Universität München — zwei Jahre vor seinem Tod. In beiden beklagte er den Sinnverlust der modernen Welt und die Unmöglichkeit, letzte Werte wissenschaftlich zu begründen. Es waren düstere Diagnosen aus der Trümmerzeit des Ersten Weltkriegs."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist der Kern von Dorothy E. Smiths 'Standpoint Epistemology' in der feministischen Soziologie?",
        answerA = "Die These, dass Frauen als Forscherinnen objektiver sind als Männer",
        answerB = "Die Erkenntnis, dass soziologisches Wissen immer von einem Standpunkt produziert wird — und dieser Standpunkt systematisch von Herrschaftsverhältnissen beeinflusst wird",
        answerC = "Die Forderung nach gleichberechtigter Repräsentation von Frauen in Forschungsinstitutionen",
        answerD = "Die Theorie, dass weibliche Sozialisationserfahrungen zu einer anderen Art des Denkens führen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Smith entwickelt in 'The Conceptual Practices of Power' (1990) die Standpunkttheorie: Soziologisches Wissen wird aus der Position der Herrschenden produziert ('ruling class standpoint'). Frauen, Schwarze, Arme haben Zugänge zu sozialer Realität, die in der Mainstream-Soziologie unsichtbar bleiben. Der 'Standpunkt der Unterdrückten' liefert nicht nur andere Perspektiven, sondern epistemisch reichhaltigeres Wissen über gesellschaftliche Machtstrukturen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Charles Tilly mit dem Konzept des 'Repertoires kollektiver Aktion'?",
        answerA = "Die Gesamtheit der kulturellen Ausdrucksformen einer sozialen Bewegung",
        answerB = "Den historisch begrenzten Vorrat an Formen des kollektiven Protests (Streik, Petition, Demonstration), die in einer Epoche zur Verfügung stehen",
        answerC = "Die Kommunikationsstrategie von Parteien im Wahlkampf",
        answerD = "Die verschiedenen Formen sozialer Solidarität in unterschiedlichen Gesellschaftstypen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Tilly zeigt in 'The Contentious French' (1986), dass kollektiver Protest nicht spontan entsteht, sondern aus einem historisch entstandenen Repertoire schöpft. Im 18. Jahrhundert protestierten Franzosen durch Brotunruhen, Scharivarien (Katzenmusik), Petitionen. Im 19. Jahrhundert entstanden neue Formen: Streik, öffentliche Demonstration, Wahlkampagne. Das Repertoire verändert sich langsam und ist pfadabhängig.",
        funFact = "Die Demonstration als Protestform ist eine Erfindung des 19. Jahrhunderts — sie setzt Presse, Bürgerrechte und städtischen Raum voraus. In Gesellschaften ohne freie Presse ist eine Demonstration sinnlos, da niemand davon erfährt. Tillys Einsicht: Politische Institutionen bestimmen, welche Protestformen effektiv sind."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Zygmunt Bauman mit dem Begriff der 'flüssigen Moderne' (liquid modernity)?",
        answerA = "Die schnelle Verbreitung von Informationen durch digitale Medien in modernen Gesellschaften",
        answerB = "Eine Gesellschaftsdiagnose, in der stabile Strukturen, Institutionen und Identitäten zunehmend fluide, kurzlebig und unverbindlich werden",
        answerC = "Die Globalisierung von Kapitalströmen und die Auflösung nationaler Wirtschaften",
        answerD = "Die Verflüssigung sozialer Klassen durch Bildungsexpansion und Wohlstandssteigerung",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Bauman diagnostiziert in 'Liquid Modernity' (2000) den Übergang von der 'soliden' (fordistischen, institutionalisierten) zur 'flüssigen' Moderne: Langfristige Beschäftigung, stabile Ehen, verlässliche Institutionen weichen prekären Arbeitsverhältnissen, kurzfristigen Beziehungen und flexiblen Identitäten. Individuen sind frei — aber auch allein verantwortlich für Risiken, die früher kollektiv getragen wurden.",
        funFact = "Bauman schrieb 'Flüchtige Moderne' im Exil — er war als Jude und Kommunist aus Polen geflohen. Sein Werk ist durchzogen vom Erfahrungshintergrund des Holocaust: 'Modernity and the Holocaust' (1989) argumentiert, der Massenmord sei keine Barbarei, sondern Produkt moderner Rationalität und Bürokratie gewesen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Ferdinand Tönnies mit dem Begriffspaar 'Gemeinschaft und Gesellschaft' (1887)?",
        answerA = "Mikro- und Makroebene soziologischer Analyse",
        answerB = "Zwei Sozialformen: organisch-affektive Bindungen (Gemeinschaft) vs. zweckrationale, vertragsförmige Assoziationen (Gesellschaft)",
        answerC = "Lokale und nationale Ebene politischer Organisation",
        answerD = "Zwei Phasen der gesellschaftlichen Entwicklung: Feudalismus und Kapitalismus",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Tönnies unterscheidet Gemeinschaft (Wesenswille: natürliche, emotionale, dauerhafte Bindungen — Familie, Dorf, Zunft) und Gesellschaft (Kürwille: rationale, kalkulierende, zweckgebundene Assoziationen — Vertrag, Markt, Staat). Die Modernisierung bedeutet den Übergang von der Gemeinschaft zur Gesellschaft — ein Gewinn an Freiheit, aber Verlust an Wärme und Geborgenheit. Diese Nostalgie machte das Werk auch für konservative und nationalistische Ideologien anschlussfähig.",
        funFact = "Tönnies' Werk wurde von Nazis missbraucht, um 'Volksgemeinschaft' gegen 'abstrakte Gesellschaft' zu stellen. Tönnies selbst war überzeugter Sozialdemokrat und lehnte den Nationalsozialismus ab. Er wurde 1933 als einer der ersten deutschen Professoren seines Amtes enthoben."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das Konzept der 'sozialen Schließung' bei Max Weber und Frank Parkin?",
        answerA = "Die soziale Isolation von Aussteigern und Marginalisierung von Dissidenten",
        answerB = "Strategien sozialer Gruppen, Ressourcen durch Ausschluss von Außenseitern zu monopolisieren — auf Basis von Kriterien wie Ethnizität, Geschlecht oder Bildung",
        answerC = "Die Schließung öffentlicher Debatten durch Mächtige zur Zementierung von Herrschaft",
        answerD = "Die Abschottung nationaler Märkte durch Protektionismus",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Weber analysiert soziale Schließung als Strategie, bei der Gruppen Zugang zu Ressourcen durch Monopolisierung begrenzen — Zünfte, Berufsverbände, Kasten, nationale Staatsbürgerschaft. Frank Parkin ('Marxism and Class Theory', 1979) systematisiert: 'Exclusionary closure' (dominierende Gruppen schließen andere aus) und 'usurpationary closure' (unterprivilegierte Gruppen kämpfen aufwärts, z.B. durch Gewerkschaften).",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt die 'Rational Choice'-Theorie in der Soziologie, besonders in der Fassung von James Coleman?",
        answerA = "Die Theorie, dass soziale Normen durch vernünftige Argumentation entworfen werden",
        answerB = "Das Modell, das soziales Handeln als nutzenmaximierendes Entscheiden rational kalkulierender Akteure erklärt",
        answerC = "Die Kritik irrationaler politischer Entscheidungsprozesse in Demokratien",
        answerD = "Die These, dass ökonomische Institutionen alle anderen sozialen Bereiche dominieren",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Rational Choice übernimmt das ökonomische Handlungsmodell für die Soziologie: Akteure haben Präferenzen und handeln, um Nutzen zu maximieren. Colemans 'Foundations of Social Theory' (1990) erklärt auch Kollektivphänomene (Normen, Institutionen) aus individuellen Entscheidungen: Normen entstehen, wenn sie für Gruppen kollektiv rational sind. Kritik: Das Modell abstrahiert zu stark von Emotionen, Habituseffekten und sozialen Strukturen.",
        funFact = "Rational Choice wurde in der Politikwissenschaft durch Public Choice (Buchanan, Tullock) und Spieltheorie (Nash) einflussreich. Die Kritik von Behavioural Economics (Kahneman, Tversky) zeigt jedoch: Menschen entscheiden systematisch irrational — heuristische Verzerrungen sind die Regel, nicht die Ausnahme."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der Begriff 'Bourdieus Kapital' in seiner symbolischen Dimension — und was unterscheidet es von ökonomischem Kapital?",
        answerA = "Symbolisches Kapital ist Prestige und Ehre, die als natürlich verkannte Form sozialer Macht funktionieren",
        answerB = "Symbolisches Kapital umfasst Kunstwerke und Kulturgüter mit materiellem Wert",
        answerC = "Symbolisches Kapital ist das accumulation von Wissen in Bildungsinstitutionen",
        answerD = "Symbolisches Kapital bezeichnet politische Einflussnahme durch Parteinetzwerke",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Symbolisches Kapital ist die Form, die die anderen Kapitalformen annehmen, wenn sie nicht als solche erkannt werden: Prestige, Ansehen, Ehre, Autorität. Das Entscheidende ist die 'Verkennung' (méconnaissance): Was als natürliche Eigenschaft erscheint (Würde, Kultiviertheit, Seriosität), ist in Wirklichkeit akkumuliertes soziales oder kulturelles Kapital. Diese Verkennung reproduziert Herrschaft, weil Unterdrückung als natürliche Überlegenheit erscheint.",
        funFact = "Bourdieu bezeichnete seine eigene Tätigkeit als Soziologe als ständigen Kampf gegen die 'doxa' — die unhinterfragte Selbstverständlichkeit sozialer Ordnung. Soziologie ist für ihn immer auch eine politische Praxis: Indem sie die verborgenen Mechanismen der Macht sichtbar macht, kann sie zu ihrer Kritik beitragen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was beschreibt Saskia Sassen mit dem Begriff der 'Global City'?",
        answerA = "Metropolen, die durch internationale Migration und kulturelle Diversität geprägt sind",
        answerB = "Strategische Knotenpunkte in der Weltwirtschaft, die Kontroll- und Managementfunktionen globaler Kapitalströme konzentrieren",
        answerC = "Städte, die aufgrund ihrer Größe über nationale Grenzen hinaus Einfluss ausüben",
        answerD = "Stadtregionen, die als Modell für nachhaltige Urbanisierung im globalen Süden gelten",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Sassen analysiert in 'The Global City' (1991) New York, London und Tokio als strategische Produktionsstandorte für die globale Ökonomie: Nicht nur Durchgangspunkte für Kapital, sondern Orte, wo Finanzinnovationen, Unternehmensdienstleistungen und globale Koordination produziert werden. Paradox: Je globaler die Ökonomie, desto mehr konzentriert sie sich in wenigen urbanen Knotenpunkten. Global Cities erzeugen extreme Polarisierung zwischen Hochlohnsektor und informellem Niedriglohnsektor.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Michel Crozier und Erhard Friedberg mit dem Konzept des 'Akteursspiels' in der Organisationssoziologie?",
        answerA = "Formale Hierarchien und Organigramme als Spielregeln in Unternehmen",
        answerB = "Die Strategie von Akteuren in Organisationen, Ungewissheitszonen zu kontrollieren und Machtzonen zu behaupten — jenseits formaler Regeln",
        answerC = "Spielerische Teambuilding-Methoden in modernen Unternehmensführung",
        answerD = "Spieltheoretische Modelle der strategischen Entscheidung in Oligopolen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Crozier/Friedberg ('L'acteur et le système', 1977) argumentieren: Organisationen sind politische Systeme. Akteure nutzen 'Ungewissheitszonen' — Bereiche, die formal nicht geregelt sind — als Machtressource. Wer Unsicherheit für andere kontrolliert (z.B. durch Expertenwissen, informelle Netzwerke, Technikkontrolle), hat Macht. Formale Strukturen allein erklären das Verhalten in Organisationen nicht.",
        funFact = "Croziers Forschung in der französischen Staatsverwaltung ('Le phénomène bureaucratique', 1963) zeigte: Bürokratie ist nicht ineffizient trotz, sondern wegen ihrer Regeln. Regeln schützen Akteure vor Risiken — daher haben alle Interesse an Bürokratie, auch wenn sie sie nach außen kritisieren."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der Begriff 'moral panic' (moralische Panik), geprägt von Stanley Cohen (1972)?",
        answerA = "Kollektive Angst vor moralischem Verfall als dauerhaftes Phänomen traditioneller Gesellschaften",
        answerB = "Eine episodische Überreaktion von Medien, Politik und Öffentlichkeit auf eine wahrgenommene soziale Bedrohung, die als 'folk devil' konstruiert wird",
        answerC = "Religiöse Bewegungen, die durch Moralpredigten gesellschaftliche Panik erzeugen",
        answerD = "Ethische Kontroversen in der Wissenschaft über die gesellschaftliche Verantwortung von Forschern",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Cohen analysiert in 'Folk Devils and Moral Panics' (1972) die britische Reaktion auf Mods und Rockers in den 1960ern: Medien dramatisierten Strandsschlägereien zur nationalen Krise. 'Folk devils' (Sündenböcke) werden konstruiert, die öffentliche Entrüstung mobilisiert, die Reaktion übertrifft die tatsächliche Bedrohung. Moral Panics enthüllen gesellschaftliche Spannungen und Wertkonflikte — sie sind nicht über das Bedrohungsphänomen, sondern über die Gesellschaft selbst.",
        funFact = "Das Konzept der moralischen Panik wurde auf viele Phänomene angewendet: Satanic Panic (1980er USA), Angst vor Videospielen, Drogen, Migranten, LGBTQ+-Rechten. Kritiker bemerken: Nicht jede gesellschaftliche Überreaktion ist eine moral panic — manchmal sind Bedrohungen real."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Bourdieu unter dem Begriff 'doxa' in seiner Soziologie sozialer Felder?",
        answerA = "Die wissenschaftliche Meinung der akademischen Gemeinschaft über ein Forschungsthema",
        answerB = "Die unhinterfragte Selbstverständlichkeit eines Feldes — die Gesamtheit der als natürlich geltenden Annahmen und Glaubenssätze",
        answerC = "Die offiziell vertretene politische Position einer Partei",
        answerD = "Religiöse Dogmen in institutionalisierten Glaubensgemeinschaften",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Doxa (griech. Meinung) bezeichnet bei Bourdieu den Bereich des Selbstverständlichen in einem Feld — Annahmen, die so fundamental sind, dass sie nicht einmal als Annahmen erkannt werden. Doxa ist unsichtbar, weil sie nicht diskutiert wird. Im Gegensatz dazu stehen Orthodoxie (verteidigter Glaube) und Heterodoxie (herausforderung). Soziale Kämpfe um Anerkennung zielen darauf ab, Doxa sichtbar und damit angreifbar zu machen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das Konzept der 'Vergesellschaftung' im Unterschied zu 'Gesellschaft' bei Georg Simmel?",
        answerA = "Vergesellschaftung bezeichnet die rechtliche Integration von Individuen in den Staatsbürgerverband",
        answerB = "Vergesellschaftung ist der dynamische Prozess des gegenseitigen Aufeinanderwirkens, durch den Gesellschaft erst entsteht — Gesellschaft ist das geronnene Ergebnis dieses Prozesses",
        answerC = "Vergesellschaftung bezeichnet die Sozialisationsprozesse in Bildungsinstitutionen",
        answerD = "Vergesellschaftung beschreibt die Übernahme von Privateigentum in Staatseigentum",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Simmel kritisiert die Vorstellung von 'Gesellschaft' als fertiges Objekt: Gesellschaft ist kein Ding, sondern ein fortwährender Prozess. 'Vergesellschaftung' (Soziation) bezeichnet die lebendige Wechselwirkung zwischen Menschen — das eigentliche Objekt der Soziologie. Diese Perspektive macht Simmel zum Vorläufer prozesssoziologischer und interaktionistischer Ansätze.",
        funFact = "Simmel wurde trotz seiner Bedeutung nie auf eine ordentliche Professur berufen — er war Jude in einem antisemitischen akademischen Milieu. Erst kurz vor seinem Tod 1918 erhielt er eine Professur in Straßburg — das kurz danach wieder Frankreich wurde. Seine akademische Marginalisierung hinderte ihn paradoxerweise nicht, einer der einflussreichsten deutschen Soziologen zu werden."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Norbert Elias mit dem Konzept der 'Figuration' in seiner Soziologie?",
        answerA = "Die symbolische Repräsentation sozialer Hierarchien in Kunst und Kultur",
        answerB = "Ein Geflecht wechselseitig aufeinander ausgerichteter Menschen, das durch seine Eigendynamik das Handeln Einzelner übersteigt",
        answerC = "Die graphische Darstellung von Machtbeziehungen in der Organisationssoziologie",
        answerD = "Historische Epochen, die durch spezifische gesellschaftliche Konfigurationen definiert werden",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Elias entwickelt den Figurationsbegriff, um den Gegensatz von Individuum und Gesellschaft zu überwinden: Menschen existieren immer in Geflechten wechselseitiger Abhängigkeit (Figurationen) — Familie, Betrieb, Nation. Diese Figurationen haben eigene Dynamiken, die kein Einzelner intendiert. In 'Was ist Soziologie?' (1970) illustriert er mit dem Ballspiel: Spieler handeln frei, aber die Figuration des Spiels steuert ihr Handeln.",
        funFact = "Elias' Konzept war Grundlage der britischen 'Figurations Sociology' (Eric Dunning, John Goudsblom), die sich intensiv mit Sport als Soziallaboratorium beschäftigte. Fußball als Figuration: Die Spannung zwischen Erlaubtem und Verbotenem, zwischen Kontrolle und Leidenschaft, erzeugt eine spezifische 'zivilisierte Erregung'."
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet Alvin Gouldner mit dem Konzept der 'reflexiven Soziologie'?",
        answerA = "Eine soziologische Methode, die durch wiederholte Datenerhebung Veränderungen über Zeit misst",
        answerB = "Die Forderung, dass Soziologie die sozialen Bedingungen ihrer eigenen Produktion reflektiert — einschließlich der Interessen und Positionen der Soziologen selbst",
        answerC = "Die Anwendung soziologischer Erkenntnisse auf pädagogische Praxis",
        answerD = "Die Rückwirkung soziologischer Theorien auf die Gesellschaft, die sie beschreiben",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Gouldner kritisiert in 'The Coming Crisis of Western Sociology' (1970) die Soziologie als institutionelle Praxis, die ihre eigenen Grundlagen nicht hinterfragt: Wer produziert soziologisches Wissen? Für wen? Mit welchen Mitteln? Er fordert eine reflexive Soziologie, die die sozialen Bedingungen ihrer Entstehung (Finanzierung, Karrierestrukturen, politische Einbindung) einbezieht. Dieser Impuls wurde von Bourdieu mit 'Homo Academicus' (1984) konkretisiert.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der Begriff 'Entdifferenzierung' in der Gegenwartssoziologie?",
        answerA = "Die Auflösung sozialer Klassen durch wirtschaftlichen Wohlstand",
        answerB = "Prozesse, bei denen ausdifferenzierte gesellschaftliche Bereiche wieder zusammenwachsen oder ihre Grenzen verwischen",
        answerC = "Die Abnahme religiöser Vielfalt durch Säkularisierung",
        answerD = "Die Angleichung von Männer- und Frauenbiographien durch Gleichstellungspolitik",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Entdifferenzierung bezeichnet Prozesse, die der funktionalen Differenzierung entgegenwirken: politische Einmischung in Wissenschaft (z.B. Autokratien, die Forschungsergebnisse zensieren), wirtschaftliche Übernahme von Medien (Verlust der publizistischen Autonomie), religiöse Politisierung. Auch die Verschmelzung von Freizeit und Arbeit durch Digitalisierung ('always on') gilt als Entdifferenzierung. Luhmann hielt Entdifferenzierung für gesellschaftlich problematisch.",
        funFact = "Jeffrey Alexander entwickelt als Alternative zu Luhmanns funktionaler Differenzierung eine 'Neofunktionalismus': Gesellschaft ist nicht nur durch Systemdifferenzierung, sondern durch symbolische und kulturelle Kohäsion integriert. Die Zivilgesellschaft ('Civil Sphere') als eigenständige Sphäre der Solidarität und Bürgerrechte ist sein Kernanliegen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht Manuel Castells unter der 'Netzwerkgesellschaft' in seiner gleichnamigen Trilogie (1996–1998)?",
        answerA = "Gesellschaften, in denen informelle soziale Netzwerke staatliche Institutionen ersetzen",
        answerB = "Eine Gesellschaftsform, in der die Netzwerklogik informationstechnologischer Systeme alle sozialen und wirtschaftlichen Strukturen transformiert",
        answerC = "Die Entstehung sozialer Bewegungen durch digitale Vernetzung und soziale Medien",
        answerD = "Die internationale Vernetzung von Eliten durch globale Konferenzen und Institutionen",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Castells analysiert, wie die Informationstechnologie eine neue Form gesellschaftlicher Organisation schafft: die Netzwerkgesellschaft. Netzwerke ersetzen hierarchische Organisationen als dominante Sozialform. Macht liegt daran, Netzwerke zu schalten, zu programmieren und zu verbinden. 'Flows' (Kapital-, Informations-, Migrationsströme) dominieren über 'places' (Orte). Raum der Ströme vs. Raum der Orte: lokale Identitäten widerstehen globalen Flüssen.",
        funFact = "Castells schrieb seine Trilogie in den frühen Jahren des Internets (1996-1998) — er analysierte Phänomene, die sich gerade erst entfalteten. Seine These, dass Netzwerkmacht eine neue Form von Herrschaft ist, wurde durch die Monopolstellung von Google, Facebook/Meta und Amazon (Plattformkapitalismus) eindrücklich bestätigt."
    ),

)
