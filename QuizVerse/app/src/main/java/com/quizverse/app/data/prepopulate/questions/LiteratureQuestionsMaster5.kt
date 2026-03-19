package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsMaster5(): List<Question> = listOf(

    Question(
        categoryId = 10,
        questionText = "Welchen zentralen Unterschied beschreibt Wittgensteins Begriff des 'Sprachspiels' in den Philosophischen Untersuchungen gegenüber dem Tractatus?",
        answerA = "Im Tractatus gilt Sprache als logisches Abbild der Welt; in den Untersuchungen gilt Bedeutung als Gebrauch in konkreten Lebensformen",
        answerB = "Im Tractatus dominiert Rhetorik; in den Untersuchungen die Grammatiktheorie Saussures",
        answerC = "Im Tractatus wird Sprache als rein national beschrieben; in den Untersuchungen als universell",
        answerD = "Im Tractatus werden Sprachspiele als Kinderspiele abgelehnt; in den Untersuchungen als mathematische Strukturen anerkannt",
        correctAnswer = 0,
        explanation = "Wittgensteins Kehre: Im Tractatus ist Sprache Abbildung logischer Tatsachen. In den Philosophischen Untersuchungen entsteht Bedeutung durch Gebrauch in sozialen Lebensformen — 'Die Bedeutung eines Wortes ist sein Gebrauch in der Sprache.'",
        difficulty = 5,
        funFact = "Wittgenstein veröffentlichte zu Lebzeiten nur den Tractatus. Die Philosophischen Untersuchungen erschienen posthum 1953 und galten als radikale Selbstkritik seines Frühwerks."
    ),

    Question(
        categoryId = 10,
        questionText = "In Thomas Manns 'Doktor Faustus' (1947) verkauft der Komponist Adrian Leverkühn seine Seele dem Teufel. Welche historische Allegorie entfaltet Mann damit?",
        answerA = "Die Verführung Deutschlands durch den Nationalsozialismus als faustischer Pakt — Leverkühn ist Nietzsche, Deutschland und Faust in einem",
        answerB = "Die Krise des religiösen Glaubens im 20. Jahrhundert",
        answerC = "Die Entfremdung des Künstlers vom kapitalistischen Markt",
        answerD = "Die Überlegenheit der Musik als Kunstform gegenüber der Sprache",
        correctAnswer = 0,
        explanation = "Mann schrieb den Roman 1943-47 im Exil als Rechenschaft über Deutschland. Leverkühns Pakt symbolisiert Deutschlands Tausch von humanistischer Tradition gegen den Rausch des Nationalsozialismus.",
        difficulty = 5,
        funFact = "Arnold Schönberg war so verärgert über Manns Darstellung der Zwölftonmusik, dass Mann in einer Nachbemerkung erklären musste, die Technik sei Schönbergs geistiges Eigentum."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht Walter Benjamin in 'Das Kunstwerk im Zeitalter seiner technischen Reproduzierbarkeit' (1936) unter dem Begriff 'Aura'?",
        answerA = "Die ästhetische Qualität eines Textes gemessen an seiner Länge",
        answerB = "Die moralische Autorität eines Autors über seine Leser",
        answerC = "Das einmalige Erscheinen eines fernen Dinges in seiner Echtheit und Authentizität am Ort seiner Überlieferung",
        answerD = "Die mystische Stimmung, die ein Kunstwerk beim Betrachter auslöst",
        correctAnswer = 2,
        explanation = "Benjamin definiert 'Aura' als 'das einmalige Erscheinen einer Ferne, so nah sie sein mag.' Technische Reproduktion zerstört die Aura, indem sie das Kunstwerk von seiner Einmaligkeit und seinem kultischen Ort löst.",
        difficulty = 5,
        funFact = "Benjamin schrieb den Essay im Pariser Exil. Er sah im Film politisches Emanzipationspotenzial, warnte aber gleichzeitig vor seiner faschistischen Instrumentalisierung."
    ),

    Question(
        categoryId = 10,
        questionText = "In welchem Werk und mit welchem Argument beansprucht Platon, dass Dichter aus dem idealen Staat verbannt werden müssen?",
        answerA = "In der 'Politeia', weil Dichtung dritte Stufe von den Ideen entfernt ist und den irrationalen Seelenteil stärkt",
        answerB = "In den 'Gesetzen', weil Dichter die Götter beleidigen",
        answerC = "Im 'Symposion', weil Dichter Liebeswahn erzeugen",
        answerD = "Im 'Phaidros', weil Dichter die Rhetorik korrumpieren",
        correctAnswer = 0,
        explanation = "In der Politeia (Buch X) begründet Platon die Dichterverbannung mit der Ideenlehre: Dichtung steht in dritter Stufe von der Wahrheit — der gemalte Tisch ist Abbild des Abbilds. Zudem stärkt sie den irrationalen Seelenteil.",
        difficulty = 5,
        funFact = "Platons Dichterkritik ist besonders paradox, da er selbst ein literarischer Meister war. Im Dialog Ion lobt er Dichter als von Gott Begeisterte — ein scheinbarer Widerspruch, der bis heute diskutiert wird."
    ),

    Question(
        categoryId = 10,
        questionText = "Welchen Begriff prägte Georg Lukács in seiner marxistischen Literaturtheorie, um das Gegenteil naturalistischer Oberflächenwiedergabe zu bezeichnen?",
        answerA = "Entfremdung",
        answerB = "Verdinglichung",
        answerC = "Typus und Totalität",
        answerD = "Widerspiegelung",
        correctAnswer = 2,
        explanation = "Lukács unterscheidet Naturalismus (oberflächliche Beschreibung) von Realismus, der durch den 'Typus' gesellschaftliche Totalität erfasst. Große Literatur zeigt durch individuelle Figuren die historischen Kräfte einer Epoche.",
        difficulty = 5,
        funFact = "Lukács' Formel vom 'Grand Hotel Abgrund' — mit der er Adorno und die Frankfurter Schule kritisierte — ist bis heute geflügeltes Wort für weltfremde Intellektuelle."
    ),

    Question(
        categoryId = 10,
        questionText = "Welchen philosophischen Begriff verwendete Albert Camus in 'Der Mythos des Sisyphos' (1942) für die Spannung zwischen dem menschlichen Verlangen nach Sinn und dem Schweigen der Welt?",
        answerA = "Das Absurde",
        answerB = "Das Nichts",
        answerC = "Die Kontingenz",
        answerD = "Die Geworfenheit",
        correctAnswer = 0,
        explanation = "Camus' 'Absurdes' entsteht aus der Kollision: Der Mensch schreit nach Sinn, die Welt antwortet mit schweigendem Chaos. Camus lehnt Selbstmord und den philosophischen 'Sprung' zu Gott ab — Sisyphos muss als glücklich gedacht werden.",
        difficulty = 5,
        funFact = "Camus schrieb den Essay gleichzeitig mit 'Der Fremde'. Beide erschienen 1942 und bilden eine philosophisch-literarische Einheit zur Absurditätsthese."
    ),

    Question(
        categoryId = 10,
        questionText = "Was bedeutet Sartres Formel in 'Das Sein und das Nichts' (1943): Der Mensch sei 'zur Freiheit verurteilt'?",
        answerA = "Der Mensch kann seiner radikalen Freiheit nicht entkommen — auch Nicht-Wählen ist eine Wahl, mauvaise foi (schlechter Glaube) ändert das nicht",
        answerB = "Freiheit kann je nach Situation gewählt oder abgelehnt werden",
        answerC = "Der Mensch wird gesellschaftlich bestraft, wenn er frei entscheidet",
        answerD = "Alle Entscheidungen sind vorherbestimmt, Freiheit ist eine Illusion",
        correctAnswer = 0,
        explanation = "Für Sartre ist das Bewusstsein reines Nichts — kein fixes Wesen. 'Existenz geht der Essenz voraus.' Auch Passivität und Rollenspiele sind selbst gewählte Haltungen — mauvaise foi.",
        difficulty = 5,
        funFact = "Sartre lehnte 1964 den Nobelpreis für Literatur ab — als konsequente Geste seiner Überzeugung, dass kein Mensch durch externe Institutionen definiert werden darf."
    ),

    Question(
        categoryId = 10,
        questionText = "Nietzsche entwickelt in 'Also sprach Zarathustra' das Konzept der 'ewigen Wiederkehr des Gleichen'. Welchen existenziellen Test formuliert er damit für das menschliche Handeln?",
        answerA = "Handle so, dass deine Tat der Mehrheit der Menschen gefällt",
        answerB = "Handle so, dass du die Konsequenzen deiner Tat im Jenseits verantworten kannst",
        answerC = "Handle so, dass du stets bereit wärst, dasselbe Tun unendlich oft zu wiederholen",
        answerD = "Handle so, dass deine Tat auf den Übermenschen als Ziel hinarbeitet",
        correctAnswer = 2,
        explanation = "Nietzsches Gedanke der ewigen Wiederkehr fungiert als ethischer Imperativ: 'Willst du das noch einmal und noch unzählige Male?' Er ersetzt theologische Jenseitsorientierung durch absolute diesseitige Bejahung.",
        difficulty = 5,
        funFact = "Nietzsche bezeichnete diesen Gedanken als seinen 'schwersten Gedanken'. Die Aufzeichnung dazu entstand am Silser Marie in der Schweiz."
    ),

    Question(
        categoryId = 10,
        questionText = "Simone de Beauvoir beginnt 'Das andere Geschlecht' (1949) mit: 'Man kommt nicht als Frau zur Welt, man wird es.' Welche These entfaltet dieser Satz?",
        answerA = "Weiblichkeit ist kein natürliches Wesen, sondern gesellschaftlich konstruiertes Dasein, das Frauen in die Position des 'Anderen' zwingt",
        answerB = "Biologische Geschlechterunterschiede sind wissenschaftlich unhaltbar",
        answerC = "Frauen sollten weibliche Eigenschaften ablegen und männliche annehmen",
        answerD = "Mutterschaft ist die einzige authentische Existenzform der Frau",
        correctAnswer = 0,
        explanation = "Beauvoir verbindet Sartres Existenzialismus mit Hegels Herr-Knecht-Dialektik: Das männliche Subjekt definiert sich als Norm, die Frau wird als das 'Andere' konstruiert. Weiblichkeit ist keine Natur, sondern aufgezwungene Situation.",
        difficulty = 5,
        funFact = "Beauvoir zweifelte lange, ob das Buch veröffentlicht werden sollte. Sartre ermutigte sie. Es wurde zum Grundlagentext des modernen Feminismus und in über 40 Sprachen übersetzt."
    ),

    Question(
        categoryId = 10,
        questionText = "In welchem Werk und mit welchem Begriff kritisieren Adorno und Horkheimer die massenmediale Kulturproduktion als System der Gleichschaltung?",
        answerA = "In der 'Dialektik der Aufklärung' (1947) mit dem Begriff 'Kulturindustrie'",
        answerB = "In 'Negative Dialektik' mit dem Begriff 'Identitätszwang'",
        answerC = "In 'Minima Moralia' mit dem Begriff 'Schlechte Unendlichkeit'",
        answerD = "In der 'Ästhetischen Theorie' mit dem Begriff 'Kunstautonomie'",
        correctAnswer = 0,
        explanation = "In der 'Dialektik der Aufklärung' analysieren Adorno und Horkheimer, wie das Aufklärungsprojekt in sein Gegenteil umschlug. Die 'Kulturindustrie' produziert standardisierte Waren, die als Kunst erscheinen, aber Konformität erzeugen.",
        difficulty = 5,
        funFact = "Das Buch entstand im US-amerikanischen Exil in Los Angeles. Adorno und Horkheimer beobachteten Hollywood aus nächster Nähe — was ihre Kulturindustrie-These direkt beeinflusste."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist der philosophische Kern von Voltaires Kritik in 'Candide' (1759) an Leibniz' These, dies sei die beste aller möglichen Welten?",
        answerA = "Leibniz habe die Existenz Gottes nicht bewiesen",
        answerB = "Leibniz habe Kant nicht berücksichtigt",
        answerC = "Der Optimismus führe nur zur Passivität, nicht zur Immoralität",
        answerD = "Die empirische Häufung von Katastrophen (Lissaboner Erdbeben, Krieg, Sklaverei) widerlegt rational den metaphysischen Optimismus",
        correctAnswer = 3,
        explanation = "Voltaire konfrontiert Leibniz' rationalen Optimismus mit der Realität: Das Lissaboner Erdbeben tötete 60.000 Menschen. Candide erlebt Krieg, Inquisition, Sklaverei. Pangloß wiederholt trotzdem 'alles ist zum Besten' — Voltaires satirischer Hieb auf apriorische Metaphysik.",
        difficulty = 5,
        funFact = "Candide erschien anonym unter dem Pseudonym 'Docteur Ralph'. Es wurde sofort verboten und konfisziert — was seinen Ruhm begründete."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche Position vertritt Samuel Beckett in 'Warten auf Godot' (Uraufführung 1953) gegenüber der existenzialistischen Forderung nach authentischer Entscheidung?",
        answerA = "Handeln, Warten und Nicht-Handeln sind gleichwertig sinnlos — das Stück persifliert die existenzialistische Handlungsphilosophie selbst",
        answerB = "Handeln und Entscheiden sind möglich, werden aber durch Strukturen verhindert",
        answerC = "Die Figuren wählen bewusst das Warten als Form der Transzendenz",
        answerD = "Godot symbolisiert Gott als Rettungsinstanz, auf die gewartet werden muss",
        correctAnswer = 0,
        explanation = "Beckett geht über Sartre hinaus: Vladimir und Estragon zeigen, dass alle Handlungen gleich bedeutungslos sind. Das absurde Theater dekonstruiert die existenzialistische Handlungsethik.",
        difficulty = 5,
        funFact = "Beckett schrieb 'Warten auf Godot' auf Französisch und übersetzte es selbst ins Englische. Er sagte, er wisse nicht, wer Godot ist — und das sei der Punkt."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche Technik aus Ionescos 'Die kahle Sängerin' (1950) macht das Drama zum Paradebeispiel des absurden Theaters?",
        answerA = "Sprache zerfällt in leere Phrasen und Tautologien, Kommunikation wird als prinzipiell unmöglich entlarvt",
        answerB = "Der Einsatz von Maschinen als Hauptdarsteller auf der Bühne",
        answerC = "Die Figuren sprechen in Hexametern nach antikem Vorbild",
        answerD = "Das Stück endet mit dem Tod aller Figuren als Atomkriegswarnung",
        correctAnswer = 0,
        explanation = "In 'La Cantatrice chauve' führt Ionesco bürgerliche Konversation ad absurdum: Sätze sind inhaltslos, Figuren erkennen sich nicht, Logik bricht zusammen. Sprache kommuniziert gar nichts mehr.",
        difficulty = 5,
        funFact = "Der Titel ist ein Zufallsprodukt: Bei einer Probe versprechen sich zwei Schauspieler. Ionesco fand es so passend, dass er es zum Titel machte."
    ),

    Question(
        categoryId = 10,
        questionText = "Welches Schopenhauer-Kapitel reflektiert Thomas Buddenbrook in Manns Roman und löst eine mystische Befreiungserfahrung aus?",
        answerA = "Über die Grundlage der Moral",
        answerB = "Über den Willen in der Natur",
        answerC = "Über den Tod und sein Verhältnis zur Unzerstörbarkeit unseres Wesens an sich",
        answerD = "Über den Satz vom zureichenden Grunde",
        correctAnswer = 2,
        explanation = "Im zehnten Teil liest Thomas Buddenbrook Schopenhauers Kapitel 'Über den Tod und sein Verhältnis zur Unzerstörbarkeit unseres Wesens an sich'. Er erlebt eine mystische Befreiung — der Tod als Erlösung vom blinden Willen zum Leben.",
        difficulty = 5,
        funFact = "Thomas Mann bekannte, er habe Schopenhauers Werk erst im letzten Drittel der Buddenbrooks gelesen. Trotzdem ist das Schopenhauer-Erlebnis die philosophische Klimax des gesamten Romans."
    ),

    Question(
        categoryId = 10,
        questionText = "Nietzsche unterscheidet in 'Die Geburt der Tragödie' (1872) das Apollinische und das Dionysische. Wie verbinden sich beide in der griechischen Tragödie?",
        answerA = "Das Apollinische (Maß, Schein, Individuation) und Dionysische (Rausch, Entgrenzung) verbinden sich in der Tragödie zur höchsten Kunstform",
        answerB = "Apollo repräsentiert den Chor, Dionysos die Hauptfigur",
        answerC = "Das Dionysische ist negativ besetzt als barbarisches Element ohne Kunstwert",
        answerD = "Apollo steht für Prosa, Dionysos ausschließlich für Lyrik",
        correctAnswer = 0,
        explanation = "Nietzsche sieht die attische Tragödie als Synthese: Apollinischer Schein und dionysischer Rausch verschmelzen. Sokrates und die Vernunft zerstörten diese Synthese — das Buch ist zugleich Angriff auf den Rationalismus.",
        difficulty = 5,
        funFact = "Das Buch wurde von der Fachwelt vernichtend kritisiert — Wilamowitz-Möllendorff veröffentlichte eine Gegenschrift. Heute gilt es als Meisterwerk."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht Roland Barthes in 'Der Tod des Autors' (1967) unter dieser provokanten These?",
        answerA = "Die Biographie des Autors darf nicht zur Interpretation herangezogen werden, da der Leser im Akt der Lektüre Bedeutung konstituiert",
        answerB = "Autoren sterben früh und hinterlassen unvollendete Werke als Problematik der Edition",
        answerC = "Durch das Internet stirbt das Konzept des Einzelautors endgültig ab",
        answerD = "Anonyme Literatur ist qualitativ hochwertiger als Autorliteratur",
        correctAnswer = 0,
        explanation = "Barthes wendet sich gegen biographische Interpretation: Ein Text hat keinen privilegierten Ursprung im Autorwillen. Bedeutung entsteht im Leser. Die Geburt des Lesers erkauft sich der Tod des Autors.",
        difficulty = 5,
        funFact = "Barthes' These erschien ein Jahr nach Foucaults Vortrag 'Was ist ein Autor?'. Zusammen bilden beide Texte das Fundament der poststrukturalistischen Literaturtheorie."
    ),

    Question(
        categoryId = 10,
        questionText = "Welchen Begriff führte der russische Formalist Viktor Schklowski ein, um das literarische Verfahren der künstlichen Fremdmachung zu beschreiben?",
        answerA = "Dialogizität als produktives Gespräch der Texte untereinander",
        answerB = "Chronotopos als Einheit von Raum und Zeit in der Erzählung",
        answerC = "Polyphonie als Gleichgewicht der Stimmen im Roman",
        answerD = "Defamiliarisierung (Ostranenie / Verfremdung) als Durchbrechen automatisierter Wahrnehmung",
        correctAnswer = 3,
        explanation = "Schklowskis 'Ostranenie' (1917): Literatur zeigt das Vertraute als fremd, um automatisierte Wahrnehmung zu durchbrechen. Brecht übernahm das Konzept als 'Verfremdungseffekt' für das Theater.",
        difficulty = 5,
        funFact = "Schklowski illustrierte Ostranenie mit Tolstois Erzählung 'Cholstomer', in der ein Pferd naiv die Eigentumsrechte der Menschen beschreibt — das Vertraute erscheint dadurch als absurd."
    ),

    Question(
        categoryId = 10,
        questionText = "Michail Bachtin entwickelte das Konzept der 'Polyphonie' am Beispiel Dostojewskis. Was unterscheidet den polyphonen Roman vom monologischen?",
        answerA = "Im polyphonen Roman gibt es mehr Nebenfiguren als im monologischen",
        answerB = "Im polyphonen Roman folgen alle Figuren der Weltanschauung des Autors",
        answerC = "Der polyphone Roman wechselt ständig zwischen verschiedenen Erzählperspektiven",
        answerD = "Im polyphonen Roman behalten die Figuren vollständige ideologische Eigenständigkeit gegenüber dem Autorstandpunkt",
        correctAnswer = 3,
        explanation = "Bachtin sieht in Dostojewskis Romanen ein Novum: Die Figuren haben eigenständige Bewusstseine, nicht reduzierbar auf die Autorposition. Der polyphone Roman ist ein Diskurs gleichberechtigter Stimmen.",
        difficulty = 5,
        funFact = "Bachtins Werk 'Probleme der Poetik Dostojewskis' wurde 1929 veröffentlicht, kurz bevor er ins Exil geschickt wurde. Es wurde erst in den 1960ern im Westen bekannt."
    ),

    Question(
        categoryId = 10,
        questionText = "Friedrich Schillers Essay 'Über naive und sentimentalische Dichtung' (1795) unterscheidet zwei Dichtertypen. Worin besteht der philosophische Kernunterschied?",
        answerA = "Naive Dichter sind einfach im Stil, sentimentalische sind kompliziert",
        answerB = "Naive Dichter sind Optimisten, sentimentalische sind Pessimisten",
        answerC = "Naive Dichter sind Naturkinder (Homer, Shakespeare), sentimentalische suchen reflektierend die verlorene Einheit mit der Natur",
        answerD = "Naive Dichtung gilt als kindlich, sentimentalische als erwachsen und reif",
        correctAnswer = 2,
        explanation = "Schillers Essay ist eine Selbstreflexion: Homer und Shakespeare sind naiv — sie sind selbst Natur, ohne Reflexionsabstand. Moderne Dichter sind sentimentalisch — sie haben die Einheit verloren und suchen sie bewusst.",
        difficulty = 5,
        funFact = "Goethe galt Schiller als naiver Dichter — dieser Kontrast schmerzte Schiller, der sich selbst als zerrissen erlebte."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht Paul Ricœur in 'Soi-même comme un autre' (1990) unter dem Begriff 'narrative Identität'?",
        answerA = "Die Identität eines Autors wird durch seine Werke geformt",
        answerB = "Jede Erzählung schafft eine neue Identität, die die alte ersetzt",
        answerC = "Identität ist eine Fiktion, die durch Sprache vorgetäuscht wird",
        answerD = "Menschen verstehen sich selbst als Figuren in einer Geschichte — personale Identität entsteht durch narrative Selbstdeutung im Zeitverlauf",
        correctAnswer = 3,
        explanation = "Ricœur unterscheidet idem-Identität und ipse-Identität. Narrative Identität vermittelt beide: Das Selbst versteht sich als Held seiner eigenen Geschichte — kohärent durch Zeit, aber offen für Uminterpretation.",
        difficulty = 5,
        funFact = "Ricœurs Konzept verbindet Literaturtheorie und Ethik: Nur wer sein Leben als Erzählung versteht, kann Versprechen halten und moralisch handeln."
    ),

    Question(
        categoryId = 10,
        questionText = "Bertolt Brecht entwickelte den 'Verfremdungseffekt' für das epische Theater. Welche philosophische Absicht steht dahinter?",
        answerA = "Das Publikum soll maximale emotionale Empathie mit den Figuren entwickeln",
        answerB = "Der V-Effekt soll die Spieler vor Überidentifikation schützen",
        answerC = "Brecht wollte Aristoteles' Katharsis durch einen kognitiven Reinigungsprozess ersetzen",
        answerD = "Das Verfahren soll Zuschauer aus der Identifikation herausreißen, damit sie gesellschaftliche Verhältnisse als veränderbar erkennen",
        correctAnswer = 3,
        explanation = "Brecht wendet sich gegen Aristoteles' Katharsis: Einfühlungstheater erzeugt passives Mitleid. Der V-Effekt verhindert Identifikation und zwingt zum Denken: 'So ist es — aber könnte es anders sein?'",
        difficulty = 5,
        funFact = "Brecht übernahm den Begriff 'Verfremdung' bewusst von Schklowskis 'Ostranenie'. Er kannte die russischen Formalisten durch Walter Benjamin."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist der philosophische Hauptunterschied zwischen dem romantischen Ironiebegriff bei Friedrich Schlegel und dem sokratischen Ironiebegriff?",
        answerA = "Sokratische Ironie ist humoristisch, romantische Ironie ist tragisch gemeint",
        answerB = "Romantische Ironie betrifft nur Prosa, sokratische Ironie nur Dialoge",
        answerC = "Sokratische Ironie täuscht Unwissenheit vor; romantische Ironie ist die permanente Selbsttranszendenz des Dichters über sein eigenes Werk",
        answerD = "Sokratische Ironie ist positiv besetzt, romantische Ironie ist nihilistisch",
        correctAnswer = 2,
        explanation = "Platon zeigt Sokrates als vorgeblich unwissend, um Gesprächspartner zu entlarven. Schlegels romantische Ironie geht weiter: Der Dichter muss sein eigenes Werk überschauen und sich über es erheben — permanente Selbstrelativierung des schöpferischen Ich.",
        difficulty = 5,
        funFact = "Schlegel prägte den Begriff im Athenäum-Fragment Nr. 116, dem Manifest der Romantik. Er beschreibt dort die romantische Poesie als 'progressive Universalpoesie'."
    ),

    Question(
        categoryId = 10,
        questionText = "George Orwells '1984' beschreibt Sprachmanipulation durch 'Neusprech'. Welche philosophische Sprachtheorie wird dabei literarisch aufgegriffen?",
        answerA = "Humboldt: Sprache als Spiegel des Nationalcharakters",
        answerB = "Chomsky: Tiefengrammatik als angeborene universale Struktur",
        answerC = "Saussure: Sprache als arbiträres Zeichensystem ohne feste Bedeutung",
        answerD = "Die Sapir-Whorf-Hypothese: Sprache bestimmt das Denken — wer bestimmte Wörter nicht hat, kann bestimmte Gedanken nicht denken",
        correctAnswer = 3,
        explanation = "Neusprech reduziert Wortschatz systematisch. Das Ziel: Gedankenverbrechen wird unmöglich, weil die Wörter fehlen. Orwell literarisiert die starke Version der Sapir-Whorf-Hypothese.",
        difficulty = 5,
        funFact = "Orwell verfasste einen eigenen Anhang über 'Die Prinzipien des Neusprech' als scheinbar akademischen Text. Diese Metafiktionalität war revolutionär für das dystopische Genre."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche wissenschaftsphilosophische These überträgt Émile Zola in 'Der Experimentalroman' (1880) auf die Literatur?",
        answerA = "Literatur soll wie Mathematik formale Strenge beweisen",
        answerB = "Claude Bernards Experimentalmethode der Medizin: Der Romancier beobachtet und 'experimentiert' mit Figuren unter bestimmten sozialen Bedingungen",
        answerC = "Darwins Evolutionstheorie soll auf Gesellschaftsformen angewandt werden",
        answerD = "Comtes Positivismus: Literatur als Soziologie in narrativer Form",
        correctAnswer = 1,
        explanation = "Zola übertrug Claude Bernards Methode auf die Literatur: Der Schriftsteller stellt Figuren unter bestimmte Milieubedingungen und beobachtet Gesetze der Vererbung und Umwelt. Der Roman wird zur Wissenschaft.",
        difficulty = 5,
        funFact = "Zola lebte zeitweilig in Bergwerken und Bordellen, um für die Rougon-Macquart-Reihe zu recherchieren. Die zwanzig Romane dokumentieren eine Familie über fünf Generationen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist das philosophische Paradox von Cervantes' 'Don Quijote' (1605/1615) in Bezug auf Literatur und Wirklichkeit?",
        answerA = "Don Quijote beweist, dass Ritterromane politisch gefährlich sind",
        answerB = "Cervantes zeigt die Unmöglichkeit von Heldentum in der modernen Gesellschaft",
        answerC = "Literatur und Realität sind untrennbar: Im zweiten Band weiß Don Quijote, dass er eine Romanfigur ist, und begegnet Lesern des ersten Bandes",
        answerD = "Cervantes demonstriert den Übergang von Mündlichkeit zur Schriftlichkeit",
        correctAnswer = 2,
        explanation = "Cervantes dekonstruiert die Grenze zwischen Fiktion und Realität: Im zweiten Band wissen die Figuren, dass ein erster Band über sie erschienen ist. Das ist radikale Metafiktion — 350 Jahre vor der Postmoderne.",
        difficulty = 5,
        funFact = "Foucault beginnt 'Die Ordnung der Dinge' (1966) mit einer Analyse des Don Quijote als Schwellenfigur zwischen dem klassischen Zeitalter und der Moderne."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche These formuliert Friedrich Hölderlin in der Elegie 'Brot und Wein' über die Funktion von Dichtung in 'dürftiger Zeit'?",
        answerA = "Dichtung soll politische Verhältnisse direkt anklagen",
        answerB = "In der gottfernen Moderne bewahrt Dichtung das Gedächtnis der göttlichen Gegenwart und bereitet die Wiederkehr des Heiligen vor",
        answerC = "Dichtung ist reiner Genuss ohne philosophischen Anspruch",
        answerD = "Moderne Dichter sollen die Griechen direkt imitieren",
        correctAnswer = 1,
        explanation = "Hölderlins 'dürftige Zeit' ist die götterferne Gegenwart nach dem Weggang der griechischen Götter. Der Dichter trägt göttliches Feuer weiter — er hält das Heilige lebendig, bis die Götter zurückkehren.",
        difficulty = 5,
        funFact = "Hölderlin verbrachte die letzten 37 Jahre seines Lebens im Tübinger Turm. Heidegger las Hölderlin als Kronzeugen seiner Seinsgeschichte."
    ),

    Question(
        categoryId = 10,
        questionText = "Der Begriff 'Intertextualität' wurde von Julia Kristeva geprägt. Welcher russische Theoretiker stand Pate für dieses Konzept?",
        answerA = "Roman Jakobson und seine Phonemlehre",
        answerB = "Michail Bachtin und seine Dialogizität: Jedes Wort ist Antwort auf andere Wörter",
        answerC = "Viktor Schklowski und sein Begriff der Ostranenie",
        answerD = "Juri Lotman und seine Semiotik der Kultur",
        correctAnswer = 1,
        explanation = "Kristeva las Bachtins 'Dialogizität' und übersetzte es in den Begriff 'Intertextualität' (1967): Jeder Text ist ein Mosaik von Transformationen anderer Texte. Kein Text ist original.",
        difficulty = 5,
        funFact = "Kristeva führte Bachtins Werk im Westen ein, als er im sowjetischen Exil kaum bekannt war. Bachtin selbst war mit ihrer Übertragung auf den Strukturalismus nicht einverstanden."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht Jacques Derrida in 'De la grammatologie' (1967) unter dem Begriff 'Logozentrismus' in der abendländischen Sprachtheorie?",
        answerA = "Die Vorliebe westlicher Philosophie für logische Strenge im Denken",
        answerB = "Die Privilegierung des gesprochenen Wortes gegenüber der Schrift als minderwertigem Supplement — was Derrida als metaphysischen Irrtum dekonstruiert",
        answerC = "Die Überzeugung, dass Vernunft alle Kulturphänomene erklären kann",
        answerD = "Der Glaube an eine universale Zeichensprache hinter allen Kulturen",
        correctAnswer = 1,
        explanation = "Derridas Logozentrismus-Kritik: Von Platon bis Saussure wurde Schrift als sekundäre Wiedergabe der gesprochenen Sprache behandelt. Derrida zeigt, dass Schrift (différance) der Sprache inhärent ist — Präsenz ist immer durch Spur und Differenz untergraben.",
        difficulty = 5,
        funFact = "Derrida präsentierte seine Thesen 1966 in Baltimore — der Strukturalismus hatte Amerika gerade entdeckt, und Derrida dekonstruierte ihn sofort vor dem versammelten Publikum."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist die These von Hegel über das 'Ende der Kunst' in den Berliner Vorlesungen zur Ästhetik (1820-29)?",
        answerA = "Moderne Kunst ist qualitativ schlechter als antike Kunst",
        answerB = "Die bildende Kunst stirbt, die Musik überlebt als einzige wahre Kunstform",
        answerC = "Kunst endet, wenn die Gesellschaft die vollkommene politische Form erreicht",
        answerD = "Kunst wird in der Moderne durch Religion und dann durch Philosophie überboten — 'Kunst ist und bleibt für uns ein Vergangenes'",
        correctAnswer = 3,
        explanation = "Hegels These: In der Antike war Kunst die höchste Manifestation des absoluten Geistes. Im Christentum übernahm die Religion diese Funktion. In der Moderne ist es die Philosophie. Kunst löst keine letzten Fragen mehr.",
        difficulty = 5,
        funFact = "Arthur Danto griff Hegels 'Ende der Kunst' 1984 auf — die Frage, ob nach Andy Warhols Brillo-Box noch eine Kunsttheorie möglich ist, ist eine direkte Weiterentwicklung."
    ),

    Question(
        categoryId = 10,
        questionText = "Welches Konzept entwickelte Hans Robert Jauss in seiner Rezeptionsästhetik (1967), das die Literaturgeschichtsschreibung revolutionierte?",
        answerA = "Werke haben eine objektiv feststellbare ästhetische Qualität jenseits aller Rezeption",
        answerB = "Der 'Erwartungshorizont': Jedes Werk wird vor dem Hintergrund zeitgenössischer Lesererwartungen rezipiert — Literaturgeschichte ist Geschichte der Rezeptionen, nicht der Texte",
        answerC = "Leser sind passiv und nehmen die Bedeutung direkt vom Autor auf",
        answerD = "Literaturgeschichte soll ausschließlich nach nationalen Perioden geordnet werden",
        correctAnswer = 1,
        explanation = "Jauss' 'Provokation der Literaturwissenschaft': Werke haben keine feste Bedeutung — sie entstehen im Akt der Rezeption. Große Werke durchbrechen den Erwartungshorizont — ästhetische Distanz als Qualitätsmerkmal.",
        difficulty = 5,
        funFact = "Jauss' Antrittsvorlesung in Konstanz wurde zum Gründungstext der Konstanzer Schule. Sie entwickelte sich parallel zur reader-response criticism in den USA — ohne Kenntnis voneinander."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht Hans-Georg Gadamer in 'Wahrheit und Methode' (1960) unter dem Begriff 'Horizontverschmelzung'?",
        answerA = "Zwei Menschen verstehen sich perfekt, wenn sie denselben kulturellen Hintergrund teilen",
        answerB = "Übersetzung zerstört immer den Horizont des Originals unwiederbringlich",
        answerC = "Verstehen eines Textes ist Verschmelzung des Horizonts des Textes mit dem des Interpreten — ein neues Sinngebilde entsteht",
        answerD = "Verstehen ist nur innerhalb derselben Kulturtradition möglich",
        correctAnswer = 2,
        explanation = "Gadamer wendet sich gegen Schleiermachers Rekonstruktionsmodell: Verstehen ist kein Hineinversetzen in den Autor, sondern ein Gespräch mit dem Text. Der Leser bringt seinen Horizont mit — Horizontverschmelzung schafft neuen Sinn.",
        difficulty = 5,
        funFact = "Gadamers Buch wurde von Habermas scharf kritisiert: Wer legt den Horizont fest? Habermas sah Machtungleichgewichte als blinden Fleck der Hermeneutik."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist das philosophisch-historische Argument von Franz Kafkas 'Das Schloss' im Vergleich mit Max Webers Analyse der Bürokratie als 'stahlhartes Gehäuse'?",
        answerA = "Das Schloss kritisiert den Feudalismus des Mittelalters",
        answerB = "Das Schloss ist eine religiöse Allegorie auf den protestantischen Gnadenbegriff",
        answerC = "Kafka kritisiert den böhmischen Nationalismus seiner Zeit",
        answerD = "K.s Scheitern symbolisiert die absolute Rationalität bürokratischer Herrschaft, die den Einzelnen in undurchdringlichen Zuständigkeitsprozeduren erdrückt",
        correctAnswer = 3,
        explanation = "Webers 'stahlhartes Gehäuse' beschreibt rationale Bürokratie als Herrschaftsform, die sich selbst perpetuiert. K. kann nie ins Schloss gelangen, weil die Bürokratie keine Außenperspektive kennt — Kafkas Parabel literarisiert Webers soziologische Diagnose der Moderne.",
        difficulty = 5,
        funFact = "Das Schloss blieb unvollendet. Kafka soll Max Brod gesagt haben, der Held K. solle am Ende sterben — kurz bevor er stirbt, erhalten die Dorfbewohner die Nachricht, dass er wohnen darf."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist das philosophische Paradox der Parabel 'Vor dem Gesetz' aus Kafkas 'Der Prozess', das Derrida analysierte?",
        answerA = "Das Gesetz ist ungerecht und bevorzugt systematisch die Mächtigen",
        answerB = "Das Gesetz ist nur für den Mann vom Lande bestimmt, aber er darf nie eintreten — das Gesetz konstituiert sich durch die Möglichkeit des Zugangs, den es selbst verweigert",
        answerC = "Das Gesetz ist ungeschrieben und deshalb beliebig interpretierbar",
        answerD = "Der Mann versäumt den Eingang, weil er nicht entschlossen genug ist",
        correctAnswer = 1,
        explanation = "Derridas Analyse: Der Türhüter ist nur für diesen einen Mann da. Autorisierung und Verbot sind identisch. Das ist die Aporie jeder rechtlichen Gründung — das Gesetz vor dem Gesetz kann nicht begründet werden.",
        difficulty = 5,
        funFact = "In 'Der Prozess' liest der Geistliche Josef K. die Parabel. Kafka liefert keine Auflösung — Interpretation bleibt prinzipiell offen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist das ästhetische Schlüsselkonzept von Umberto Ecos 'Der Name der Rose' (1980) in Bezug auf Interpretation und Texte?",
        answerA = "Das Buch beweist, dass der mittelalterliche Mönch die überlegene Lebensform ist",
        answerB = "Wahrheit ist immer rekonstruierbar, wenn man klug genug sucht",
        answerC = "Die Bibliothek als Labyrinth symbolisiert, dass Texte keine feste Bedeutung haben — Jorge tötet für den Text über das Lachen, weil Lachen Wahrheitsansprüche heiliger Texte untergräbt",
        answerD = "Der Roman ist eine historische Rekonstruktion realer Ereignisse des 14. Jahrhunderts",
        correctAnswer = 2,
        explanation = "Ecos semiotischer Schlüssel: Die Bibliothek ist ein Labyrinth ohne Zentrum — ein Modell der Intertextualität. Jorge verbirgt den zweiten Teil der Aristotelischen Poetik über die Komödie, weil Lachen die Autorität heiliger Texte untergräbt.",
        difficulty = 5,
        funFact = "Eco sagte, er wollte einen Mittelalter-Krimi schreiben, aber der Leser muss die ersten 100 Seiten als 'Buße' ertragen — wer das schafft, gehört zu seinem Zielpublikum."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche philosophische These vertritt Martha Nussbaum in 'Poetic Justice' (1995) über das Verhältnis von Literatur und Ethik?",
        answerA = "Literatur kann keine moralische Erkenntnis vermitteln, weil sie bloße Fiktion ist",
        answerB = "Literarische Imagination kultiviert moralische Empathie und ist konstitutiv für Gerechtigkeit — Richter und Bürger brauchen 'narrative Imagination'",
        answerC = "Ethik sollte ausschließlich auf reiner Vernunftlogik basieren, nicht auf Emotionen",
        answerD = "Nur philosophische Texte können moralische Wahrheiten wirklich vermitteln",
        correctAnswer = 1,
        explanation = "Nussbaums These: Das Lesen von Romanen kultiviert die 'narrative Imagination'. Richter, Gesetzgeber, Bürger brauchen diese Kompetenz für gerechte Entscheidungen. Literatur ist nicht Ornament, sondern Bedingung demokratischer Moral.",
        difficulty = 5,
        funFact = "Nussbaum analysiert Dickens' 'Schwere Zeiten' als Modellfall: Mr. Gradgrinds Utilitarismus scheitert an seiner Unfähigkeit zur narrativen Imagination."
    ),

    Question(
        categoryId = 10,
        questionText = "Was beschreibt Walter Benjamins 'Engel der Geschichte' in 'Über den Begriff der Geschichte' (1940) nach Paul Klees Bild 'Angelus Novus'?",
        answerA = "Den Fortschritt als zielgerichtete Bewegung der Geschichte vorwärts",
        answerB = "Den Engel mit dem Gesicht zur Vergangenheit, der die Trümmer der Geschichte sieht, während er vom Sturm des Fortschritts in die Zukunft getrieben wird",
        answerC = "Die Hoffnung auf messianische Erlösung jenseits aller Geschichte",
        answerD = "Den marxistischen Historiker, der die Geschichte der Unterdrückten schreibt",
        correctAnswer = 1,
        explanation = "Benjamins Neunter Geschichtsphilosophischer Satz: Der Engel will bei den Trümmern bleiben, die Toten aufwecken — aber der Sturm des Fortschritts treibt ihn vorwärts. Benjamin wendet sich gegen linearen Fortschrittsoptimismus.",
        difficulty = 5,
        funFact = "Benjamin schrieb die Thesen kurz vor seinem Tod 1940 auf der Flucht vor den Nazis. Sie sind sein philosophisches Testament."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist die philosophische Kernthese des Großinquisitors in Dostojewskis 'Die Brüder Karamasow'?",
        answerA = "Christus hat die Menschheit überfordert — Menschen wollen keine Freiheit, sondern Brot, Wunder und Autorität",
        answerB = "Die Kirche hat Christus zu Recht verurteilt, weil er eine soziale Gefahr darstellt",
        answerC = "Freiheit ist das höchste Gut, das Christus der Menschheit geschenkt hat",
        answerD = "Gott existiert nicht, daher kann Christus nur ein Betrüger sein",
        correctAnswer = 0,
        explanation = "Der Großinquisitor stellt Christus vor Gericht: Die Kirche habe die Freiheit aufgehoben — zu Recht. Menschen können absolute Freiheit nicht ertragen; sie brauchen Mysterium, Wunder, Autorität. Diese Szene ist eine der tiefgründigsten Freiheits-Debatten der Weltliteratur.",
        difficulty = 5,
        funFact = "Dostojewski selbst war tief religiös, aber er gab dem Großinquisitor die stärksten Argumente. Er wollte Atheismus so stark formulieren, dass er selbst fast an ihm zweifle."
    ),

    Question(
        categoryId = 10,
        questionText = "Welches zentrale ästhetische Konzept entwickelt Kant in der 'Kritik der Urteilskraft' (1790), das die Kunsttheorie bis heute prägt?",
        answerA = "Das Erhabene als Überwältigung des Verstandes durch die Unendlichkeit der Natur",
        answerB = "Das Schöne ist die vollkommene Harmonie von Form und Inhalt eines Kunstwerks",
        answerC = "Das interesselose Wohlgefallen: Ästhetisches Urteil ist subjektiv, beansprucht aber allgemeine Zustimmung — es ist weder rein kognitiv noch bloß sinnlich",
        answerD = "Kunst ist Nachahmung der Natur, die zu ihrer Vollkommenheit geführt wird",
        correctAnswer = 2,
        explanation = "Kants ästhetisches Urteil ist ein Dritter Weg: nicht objektives Erkennen, nicht subjektiver Sinnengenuss, sondern 'freies Spiel von Einbildungskraft und Verstand'. Das Schöne gefällt allgemein ohne Interesse — das ist der philosophische Grund für die Autonomie der Kunst.",
        difficulty = 5,
        funFact = "Die 'Kritik der Urteilskraft' war ursprünglich als Brücke zwischen theoretischer und praktischer Vernunft geplant. Sie wurde zur Grundlage der modernen Ästhetik und beeinflusste die gesamte Romantik."
    ),

    Question(
        categoryId = 10,
        questionText = "Was bedeutet Heideggers Analyse des 'Man' in 'Sein und Zeit' (1927) für das Dasein und seine Eigentlichkeit?",
        answerA = "Authentisches Schreiben ist unmöglich, weil Sprache immer das anonyme 'Man' reproduziert",
        answerB = "Das 'Man' verhindert, dass das Dasein zu seiner eigentlichsten Möglichkeit — dem Sein-zum-Tode — steht; Eigentlichkeit erfordert Entschlossenheit",
        answerC = "Literatur soll das 'Man' stärken, um Gemeinschaft zu bilden",
        answerD = "Authentizität ist nur durch Schweigen erreichbar, nicht durch Sprache",
        correctAnswer = 1,
        explanation = "Heideggers 'Man' ist das anonyme Subjekt des Alltags. Es zerstreut das Dasein in Uneigentlichkeit. Eigentlichkeit bedeutet, dem eigenen Tod ins Gesicht zu sehen (Sein-zum-Tode) und entschlossen zu leben.",
        difficulty = 5,
        funFact = "Heideggers Werk beeinflusste Sartre, Camus und die gesamte existenzialistische Literatur. Heidegger selbst lehnte die Bezeichnung 'Existentialist' ab — er betrieb Ontologie, keine Anthropologie."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist Lessings philosophisches Argument in der 'Laokoon'-Schrift (1766) über den Unterschied zwischen Malerei und Dichtung?",
        answerA = "Musik ist die höchste Kunst, weil sie Zeit und Raum vereint",
        answerB = "Malerei und Plastik sind 'Körper im Raum', Dichtung ist 'Handlung in der Zeit' — beide Künste haben eigene Gesetze aus ihrem Medium; der 'fruchtbare Augenblick' gilt nur für die bildende Kunst",
        answerC = "Dichtung ist der Malerei überlegen, weil sie unbegrenzt mehr ausdrücken kann",
        answerD = "Die Laokoon-Gruppe beweist, dass griechische Plastik emotional überlegen ist",
        correctAnswer = 1,
        explanation = "Lessing kritisiert Winckelmanns These: Der Laokoon schreit nicht, weil Plastik den 'fruchtbaren Augenblick' zeigen muss — jenen Moment, der Vergangenheit und Zukunft ahnen lässt. Dichtung kann Sukzession zeigen. Beide Künste haben eigene Gesetze.",
        difficulty = 5,
        funFact = "Lessings 'Laokoon' begründete die moderne Medienkritik. Marshall McLuhans 'The Medium is the Message' (1964) ist direkt von Lessings medialer Differenzierung inspiriert."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche philosophische These über Literatur und Macht entwickelt Michel Foucault in 'Die Ordnung des Diskurses' (1970)?",
        answerA = "Literatur ist frei von Machtverhältnissen, weil sie reine Fiktion ist",
        answerB = "Macht zensiert Literatur, kann aber ihren Inhalt nicht verändern",
        answerC = "Diskurse (auch literarische) sind produktive Machtsysteme, die Wissen, Wahrheit und Subjektivität konstituieren — wer sprechen darf und wie, ist eine Machtfrage",
        answerD = "Literatur ist das einzige Medium, das Macht effektiv kritisieren kann",
        correctAnswer = 2,
        explanation = "Foucaults Diskurstheorie: Es gibt keine 'freie' Rede außerhalb von Machtstrukturen. Diskurse schließen aus (Kommentar, Autor, Disziplin als Kontrollprinzipien). Auch Literatur ist diskursiv verfasst und reproduziert Wissen als Macht.",
        difficulty = 5,
        funFact = "Foucaults Antrittsvorlesung am Collège de France begann mit: 'Ich wünschte, ich müsste jetzt nicht sprechen.' — Eine performative Illustration der Rede-Zwänge, die er analysieren wollte."
    ),

    Question(
        categoryId = 10,
        questionText = "Welches Konzept von Ernst Cassirer beschreibt Sprache, Mythos und Kunst als gleichwertige Zugänge zur Wirklichkeit?",
        answerA = "Der Kantsche Kritizismus: alle drei sind transzendentale Erkenntnisformen",
        answerB = "Der Hegelsche Idealismus: Kunst ist die niedrigste Stufe des absoluten Geistes",
        answerC = "Cassirers Philosophie der symbolischen Formen (1923-29): Der Mensch ist 'animal symbolicum' — Sprache, Mythos, Kunst und Wissenschaft sind gleichwertige Modi der Welterschließung",
        answerD = "Der Positivismus: Wissenschaft ist die einzig valide Erkenntnisform",
        correctAnswer = 2,
        explanation = "Cassirer erweitert Kant: Der Mensch erschließt die Welt durch symbolische Systeme. Sprache formt Weltbild, Mythos strukturiert Erfahrung, Kunst öffnet ästhetische Dimension. Keine Form ist privilegiert.",
        difficulty = 5,
        funFact = "Cassirer und Heidegger trafen sich 1929 in Davos zur legendären 'Davoser Disputation'. Die Debatte gilt als Wasserscheide zwischen Neukantianismus und Existenzphilosophie."
    ),

    Question(
        categoryId = 10,
        questionText = "Welchen Einfluss hatte Nietzsche auf André Gides 'Die Verliese des Vatikans' (1914) durch das Konzept der 'acte gratuit'?",
        answerA = "Gide übernahm Nietzsches Christentumskritik als zentrale Botschaft des Romans",
        answerB = "Die Figur Lafcadio begeht eine 'acte gratuit' — eine unmotivierte Tat aus reiner Freiheit als literarische Umsetzung von Nietzsche jenseits von Gut und Böse",
        answerC = "Nietzsche beeinflusste Gides Romanform durch den Begriff der ewigen Wiederkehr",
        answerD = "Gide nutzte Nietzsche für seine philosophische Verteidigung der Homosexualität",
        correctAnswer = 1,
        explanation = "Lafcadios 'acte gratuit' — er wirft einen Fremden aus dem Zugfenster ohne Motiv — ist literarische Exploration der amoralistischen Freiheit jenseits von Gut und Böse: Eine Tat ohne Ursache, Zweck oder moralische Konsequenz.",
        difficulty = 5,
        funFact = "Gide selbst distanzierte sich später von der Figur. Aber die 'acte gratuit' beeinflusste Camus' 'Meursault' in 'Der Fremde' direkt."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist das philosophische Paradox des Erzählers in Prousts 'Auf der Suche nach der verlorenen Zeit' bezüglich Erinnerung und Zeit?",
        answerA = "Proust beweist, dass Erinnerung immer unzuverlässig und damit wertlos ist",
        answerB = "Prousts These ist bergsonsianisch: Bewusstseinsstrom ersetzt die objektive Zeit",
        answerC = "Der Erzähler kann sich nicht erinnern und sucht deshalb Spuren in der Außenwelt",
        answerD = "Die mémoire involontaire überwindet die Zeit: Der erinnerte Moment und der Moment des Erinnerns fallen zusammen — Zeit wird nicht zurückgewonnen, sondern außer Kraft gesetzt",
        correctAnswer = 3,
        explanation = "Prousts 'temps retrouvé': Die Madeleine-Szene illustriert mémoire involontaire — ein Geschmack löst totale Wiederkehr der Vergangenheit aus. Die unfreiwillige Erinnerung macht den vergangenen Moment real präsent und hebt die Zeit auf.",
        difficulty = 5,
        funFact = "Proust schrieb die sieben Bände in einem korkausgekleideten Zimmer, liegend, meist nachts. Er starb 1922, bevor die letzten Bände erschienen."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche gesellschaftsphilosophische These trägt Fontanes 'Effi Briest' (1895) über die wilhelminische Gesellschaft?",
        answerA = "Das wilhelminische Preußen ist ein idealer Rechtsstaat",
        answerB = "Effis Scheitern ist ausschließlich Folge ihres eigenen Charakterfehlers",
        answerC = "Fontane plädiert für eine konkrete Reform des Scheidungsrechts",
        answerD = "Die gesellschaftlichen Ehrenkodizes des wilhelminischen Bürgertums zerstören Individuen, die zwischen gesellschaftlicher Norm und persönlichem Glück zerrissen sind",
        correctAnswer = 3,
        explanation = "Instetten handelt nicht aus Gefühl, sondern aus gesellschaftlichem Kalkül — er duelliert sich sieben Jahre nach der Affäre aus Angst vor dem 'gesellschaftlichen Mechanismus'. Effi stirbt daran. Fontane zeigt die Unmenschlichkeit des Ehrenkodex als systemische Gewalt.",
        difficulty = 5,
        funFact = "Fontane schrieb 'Effi Briest' mit 75 Jahren — sein letzter vollendeter Roman. Er selbst sagte, Instetten sei 'der eigentliche Held': ein Mensch, der das Richtige weiß und das Falsche tut."
    ),

    Question(
        categoryId = 10,
        questionText = "Was beschreibt Harold Bloom in 'The Anxiety of Influence' (1973) als Grundmechanismus literarischer Originalität?",
        answerA = "Intertextualität als neutrale Aufnahme fremder Einflüsse",
        answerB = "Tradition als stabiles Fundament, auf dem jeder Dichter aufbaut",
        answerC = "Der Tod des Lesers als Voraussetzung originaler Schöpfung",
        answerD = "Die Einfluss-Angst: Dichter müssen ihre starken Vorgänger durch produktive Fehllesungen verdrängen und überwinden, um zur eigenen Stimme zu finden",
        correctAnswer = 3,
        explanation = "Blooms These: Literarische Originalität entsteht durch 'Misreading' — bewusst fehlerhafte Lektüre der Vorgänger. Jeder starke Dichter verdrängt seinen literarischen Vater durch schöpferische Fehldeutung — ödipale Struktur der Literaturgeschichte.",
        difficulty = 5,
        funFact = "Bloom nannte seine Theorie explizit ödipale Literaturgeschichte. Feminist critics kritisierten ihn scharf: Das Modell setzt ausschließlich männliche Dichterkarrieren voraus."
    ),

    Question(
        categoryId = 10,
        questionText = "Welchen philosophischen Unterschied zwischen Goethes und Hegels Begriff der 'Weltliteratur' kann man nachweisen?",
        answerA = "Hegel meinte damit nur die griechische Antike, Goethe alle Kulturen gleichermaßen",
        answerB = "Goethe sah Weltliteratur als internationalen Austausch zwischen Nationalliteraturen; Hegel sah sie als dialektische Stufenfolge des Weltgeistes",
        answerC = "Goethe sprach von Weltliteratur als Utopie, Hegel als bereits verwirklichte historische Realität",
        answerD = "Beide meinten dasselbe — der Unterschied ist nur terminologischer Natur",
        correctAnswer = 1,
        explanation = "Goethes 'Weltliteratur' (1827) meinte internationalen Austausch und gegenseitige Befruchtung der Nationalliteraturen. Hegels Literaturphilosophie sieht Kunst als sinnliche Erscheinung des Geistes — jede Kunststufe entspricht einer Stufe des Weltgeistes.",
        difficulty = 5,
        funFact = "Goethe prägte den Begriff nach der Lektüre eines chinesischen Romans. Marx griff Goethes Vision im Kommunistischen Manifest auf."
    ),

    Question(
        categoryId = 10,
        questionText = "Welchen Einfluss hatte Schopenhauers Musikphilosophie auf Wagners Musikdrama und den literarischen Symbolismus?",
        answerA = "Schopenhauer sah Musik als untergeordnete Kunst; Wagner widersprach dem",
        answerB = "Schopenhauer beeinflusste nur Wagners Harmonielehre, nicht seine Weltanschauung",
        answerC = "Wagner lehnte Schopenhauer ab und wandte sich ausschließlich Nietzsche zu",
        answerD = "Schopenhauer erklärte Musik zur unmittelbaren Sprache des Willens selbst; Wagner übernahm dies, der Symbolismus übertrug es auf die Sprache",
        correctAnswer = 3,
        explanation = "Schopenhauer: Anders als alle anderen Künste ist Musik direkter Ausdruck des Willens — ontologisch privilegiert. Wagner las Schopenhauer 1854 und komponierte Tristan und Isolde unter diesem Einfluss. Mallarmé und die Symbolisten wollten Sprache zur Musik machen.",
        difficulty = 5,
        funFact = "Wagner schickte Schopenhauer ein Exemplar des Nibelungen-Texts — Schopenhauer lobte es wenig enthusiastisch. Sie trafen sich nie persönlich."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht Maurice Blanchot in 'Der literarische Raum' (1955) unter 'l'espace littéraire' in Bezug auf das Verhältnis von Autor und Werk?",
        answerA = "Literatur braucht einen physischen Publikationsort",
        answerB = "Der literarische Raum ist ein autonomes Feld — das Werk fordert den Autor zur Auflösung auf, Schreiben ist ein Sterben des Subjekts ins Werk hinein",
        answerC = "Literatur schafft eine imaginäre Welt, die von der realen vollständig getrennt ist",
        answerD = "Der Autor kontrolliert vollständig den Raum seiner literarischen Schöpfung",
        correctAnswer = 1,
        explanation = "Blanchot radikalisiert Mallarmé: Das Werk saugt den Autor auf. Schreiben ist nicht Selbstausdruck, sondern Selbstauflösung. Der Autor stirbt ins Werk. Diese These antizipiert Barthes' 'Tod des Autors' und Foucaults Autorfunktion um ein Jahrzehnt.",
        difficulty = 5,
        funFact = "Blanchot gab keine Interviews und ließ sich nicht fotografieren. Sein Leben war eine Art Inkarnation seiner Theorie des Verschwindens."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht Hannah Arendt in 'Vita activa' (1958) unter der besonderen Bedeutung des 'Handelns' für Literatur und Erzählung?",
        answerA = "Das Herstellen ist entscheidend, weil Kunstwerke die Welt überdauern",
        answerB = "Das Arbeiten ist die höchste Kategorie, weil es das Leben erhält",
        answerC = "Das Handeln offenbart das 'Wer' einer Person, und nur Erzählung kann die flüchtige Tat festhalten und Bedeutung stiften",
        answerD = "Das Denken ist die höchste Tätigkeitsform, weil nur Philosophen die polis leiten können",
        correctAnswer = 2,
        explanation = "Arendts 'Handeln' offenbart das einmalige Selbst in der Öffentlichkeit. Aber Handeln ist flüchtig — es braucht Erzählung, um zu dauern. Der Sänger (Homer) macht Achill unsterblich. Literatur ist das Gedächtnis des Politischen.",
        difficulty = 5,
        funFact = "Arendts Buch entstand als Reaktion auf die Erfahrung des Totalitarismus. Sie zeigt, wie der Nationalsozialismus das Handeln als politische Pluralität systematisch zerstörte."
    )

)
