package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsHard2(): List<Question> = listOf(

    // --- Renaissance Politics: Machiavelli, Medici, City-States ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr veröffentlichte Niccolò Machiavelli sein politisches Hauptwerk 'Il Principe' (Der Fürst) posthum?",
        answerA = "1513",
        answerB = "1521",
        answerC = "1532",
        answerD = "1540",
        correctAnswer = 2,
        explanation = "'Il Principe' wurde zwar um 1513 verfasst, aber erst 1532, fünf Jahre nach Machiavellis Tod, posthum veröffentlicht. Das Werk widmete Machiavelli Lorenzo de' Medici.",
        difficulty = 3,
        funFact = "Machiavelli schrieb den 'Fürsten' ursprünglich, um bei den Medici in Florenz wieder Fuß zu fassen – er war nach dem Sturz der Republik 1512 aus dem Amt gedrängt worden."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches florentinische Bankhaus kontrollierte im 15. Jahrhundert das größte Finanzimperium Europas und hatte Filialen in über einem Dutzend Städten?",
        answerA = "Bank der Pazzi",
        answerB = "Banca dei Bardi",
        answerC = "Banco dei Medici",
        answerD = "Banca Strozzi",
        correctAnswer = 2,
        explanation = "Der Banco dei Medici, gegründet 1397 von Giovanni di Bicci de' Medici, wuchs unter Cosimo dem Älteren zum mächtigsten Finanzinstitut Europas mit Filialen in London, Brügge, Lyon, Genf, Venedig und Rom.",
        difficulty = 3,
        funFact = "Die Medici-Bank erfand das Konzept des Wechsels in seiner modernen Form – eine frühe Art Kreditbrief, die das Transportieren von Bargeld über weite Strecken überflüssig machte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis von 1478 erschütterte die Vorherrschaft der Medici in Florenz und kostete Giuliano de' Medici das Leben?",
        answerA = "Der Aufstand der Ciompi",
        answerB = "Die Pazzi-Verschwörung",
        answerC = "Die Invasion König Karls VIII.",
        answerD = "Der Aufstand der Acht Heiligen",
        correctAnswer = 1,
        explanation = "Die Pazzi-Verschwörung vom 26. April 1478 war ein Attentat in der Kathedrale von Florenz: Giuliano de' Medici wurde ermordet, sein Bruder Lorenzo der Prächtige überlebte verletzt. Hinter der Verschwörung standen die Pazzi-Familie und Papst Sixtus IV.",
        difficulty = 3,
        funFact = "Lorenzo de' Medici ließ nach dem gescheiterten Attentat die Hauptverschwörer hinrichten – Jacopo de' Pazzi wurde zunächst exhumiert und noch einmal öffentlich 'hingerichtet', um die Macht der Medici zu demonstrieren."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches politische Konzept beschreibt Machiavellis Behauptung, dass ein Fürst lieber gefürchtet als geliebt sein sollte, wenn beides nicht gleichzeitig möglich ist?",
        answerA = "Die virtù",
        answerB = "Die fortuna",
        answerC = "Ragione di stato",
        answerD = "Der Realismus der necessità",
        correctAnswer = 3,
        explanation = "Machiavelli argumentiert im Kapitel XVII des 'Fürsten', dass ein Herrscher aus necessità (Notwendigkeit) handeln muss: Gefürchtet zu sein ist sicherer als geliebt zu sein, da die Liebe der Menschen schwankt, die Furcht aber dauerhaft ist.",
        difficulty = 3,
        funFact = "Der Begriff 'machiavellisch' – als Synonym für skrupellose Machtpolitik – wurde von Machiavellis Kritikern geprägt; er selbst sah sich als nüchternen Beobachter politischer Realitäten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche italienische Stadtrepublik entwickelte im 15. Jahrhundert das Konzept des Gleichgewichts zwischen den fünf Großmächten ('Pace di Lodi' 1454)?",
        answerA = "Genua und Florenz",
        answerB = "Mailand, Florenz, Venedig, Kirchenstaat und Neapel",
        answerC = "Rom, Venedig und Florenz",
        answerD = "Florenz, Neapel und Mailand",
        correctAnswer = 1,
        explanation = "Der Frieden von Lodi (1454) schuf ein Gleichgewichtssystem zwischen den fünf italienischen Mächten: Mailand, Florenz, Venedig, dem Kirchenstaat und Neapel. Dies gilt als eines der ersten modernen Gleichgewichtssysteme der internationalen Politik.",
        difficulty = 3,
        funFact = "Lorenzo de' Medici spielte als 'Zeiger der Waage Italiens' eine Schlüsselrolle bei der Aufrechterhaltung dieses Gleichgewichts – sein Tod 1492 gilt als einer der Auslöser für die folgende Destabilisierung Italiens."
    ),

    // --- Reformation: Diet of Worms, Augsburg Confession, Schmalkaldic League ---

    Question(
        categoryId = 3,
        questionText = "Auf welchem Reichstag in Worms (1521) verweigerte Martin Luther den Widerruf seiner Schriften mit den Worten 'Hier stehe ich, ich kann nicht anders'?",
        answerA = "Reichstag zu Worms 1495",
        answerB = "Reichstag zu Worms 1521",
        answerC = "Reichstag zu Augsburg 1530",
        answerD = "Reichstag zu Speyer 1526",
        correctAnswer = 1,
        explanation = "Auf dem Reichstag zu Worms 1521 forderte Kaiser Karl V. Luther auf, seine Schriften zu widerrufen. Luthers Weigerung am 18. April 1521 führte zum Wormser Edikt, das ihn zum Reichsfeind erklärte. Die exakten Worte sind historisch umstritten.",
        difficulty = 3,
        funFact = "Kurfürst Friedrich der Weise von Sachsen ließ Luther nach dem Reichstag auf der Wartburg 'entführen', um ihn zu schützen – dort übersetzte Luther das Neue Testament in zehn Wochen ins Deutsche."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer verfasste die Confessio Augustana (Augsburger Bekenntnis) von 1530, die auf dem Reichstag zu Augsburg verlesen wurde?",
        answerA = "Martin Luther",
        answerB = "Philipp Melanchthon",
        answerC = "Johannes Calvin",
        answerD = "Huldrych Zwingli",
        correctAnswer = 1,
        explanation = "Die Confessio Augustana wurde von Philipp Melanchthon verfasst, Luthers engem Mitarbeiter. Sie ist das wichtigste Bekenntnis der lutherischen Kirche und wurde am 25. Juni 1530 vor Kaiser Karl V. verlesen.",
        difficulty = 3,
        funFact = "Martin Luther durfte Augsburg wegen der Reichsacht nicht betreten – er verfolgte die Ereignisse aus der Feste Coburg, wohin er sich zurückgezogen hatte, und stand brieflich mit Melanchthon in Kontakt."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr und in welcher Stadt wurde der Schmalkaldische Bund gegründet, das protestantische Defensivbündnis gegen Kaiser Karl V.?",
        answerA = "1526 in Speyer",
        answerB = "1530 in Augsburg",
        answerC = "1531 in Schmalkalden",
        answerD = "1535 in Frankfurt",
        correctAnswer = 2,
        explanation = "Der Schmalkaldische Bund wurde am 27. Februar 1531 in Schmalkalden (Thüringen) gegründet. Anführer waren Kurfürst Johann von Sachsen und Landgraf Philipp von Hessen. Er diente der gemeinsamen Verteidigung gegen kaiserliche Angriffe auf die Reformation.",
        difficulty = 3,
        funFact = "Der Bund finanzierte seine Aktivitäten auch durch den Verkauf kirchlicher Güter – eine der ersten großen Kirchengutenteignungen der Reformationszeit."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher Schlacht von 1547 besiegte Kaiser Karl V. den Schmalkaldischen Bund entscheidend und nahm Kurfürst Johann Friedrich von Sachsen gefangen?",
        answerA = "Schlacht bei Breitenfeld",
        answerB = "Schlacht bei Mühlberg",
        answerC = "Schlacht bei Frankenhausen",
        answerD = "Schlacht bei Lauffen",
        correctAnswer = 1,
        explanation = "In der Schlacht bei Mühlberg an der Elbe am 24. April 1547 besiegte Karl V. die protestantischen Fürsten vernichtend. Kurfürst Johann Friedrich von Sachsen wurde gefangen genommen und verlor seinen Kurstand.",
        difficulty = 3,
        funFact = "Tizians berühmtes Reiterporträt Karls V. nach der Schlacht bei Mühlberg gilt als eines der ersten politischen Propagandagemälde der Neuzeit und beeinflusste jahrhundertelang die Herrscherdarstellung."
    ),

    Question(
        categoryId = 3,
        questionText = "Was legte der Augsburger Religionsfrieden von 1555 als Grundprinzip fest, das den protestantischen Fürsten das Recht gab, die Konfession ihres Territoriums zu bestimmen?",
        answerA = "Cuius regio, eius religio",
        answerB = "Extra ecclesiam nulla salus",
        answerC = "Cujus regio, ejus natio",
        answerD = "In nomine Domini",
        correctAnswer = 0,
        explanation = "'Cuius regio, eius religio' (Wessen das Land, dessen die Religion) war das Kernprinzip des Augsburger Religionsfriedens von 1555: Jeder Reichsfürst durfte für sein Territorium zwischen dem katholischen und dem lutherischen Bekenntnis wählen.",
        difficulty = 3,
        funFact = "Der Augsburger Religionsfrieden galt nur für Lutheraner – Calvinisten und andere reformierte Konfessionen waren ausdrücklich ausgeschlossen, was zu weiteren Konflikten führte."
    ),

    // --- Council of Trent ---

    Question(
        categoryId = 3,
        questionText = "In wie vielen Sitzungsperioden tagte das Konzil von Trient, das die Gegenreformation definierte, und wie lange dauerte es insgesamt?",
        answerA = "2 Perioden, 10 Jahre (1545–1555)",
        answerB = "3 Perioden, 18 Jahre (1545–1563)",
        answerC = "4 Perioden, 25 Jahre (1545–1570)",
        answerD = "2 Perioden, 15 Jahre (1545–1560)",
        correctAnswer = 1,
        explanation = "Das Konzil von Trient tagte in drei Perioden: 1545–1547, 1551–1552 und 1562–1563 – insgesamt 18 Jahre. Es definierte die Dogmen der katholischen Kirche als Antwort auf die Reformation und leitete die Gegenreformation ein.",
        difficulty = 3,
        funFact = "Das Konzil zählte nur wenige hundert Teilnehmer – viel weniger als mittelalterliche Konzilien. Die Beschlüsse wurden fast ausschließlich von italienischen Bischöfen gefasst."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Prinzip des Konzils von Trient besagte, dass sowohl die Heilige Schrift als auch die kirchliche Tradition als gleichwertige Quellen des Glaubens zu gelten haben?",
        answerA = "Sola scriptura",
        answerB = "Duplex origo fidei",
        answerC = "Scriptura et Traditio",
        answerD = "Magisterium Ecclesiae",
        correctAnswer = 2,
        explanation = "Das Konzil von Trient stellte die Heilige Schrift (Scriptura) und die kirchliche Tradition (Traditio) als gleichwertige Glaubensquellen nebeneinander – eine direkte Ablehnung von Luthers 'Sola scriptura'-Prinzip.",
        difficulty = 3,
        funFact = "Das Konzil legte auch die Vulgata (lateinische Bibelübersetzung des Hieronymus) als authentischen Text der Bibel fest – obwohl Humanisten wie Erasmus zahlreiche Übersetzungsfehler darin nachgewiesen hatten."
    ),

    // --- Thirty Years War ---

    Question(
        categoryId = 3,
        questionText = "Welches Datum wird als Auslöser des Dreißigjährigen Krieges bezeichnet, als protestantische Adlige zwei kaiserliche Statthalter aus einem Prager Fenster warfen?",
        answerA = "23. Mai 1618",
        answerB = "6. Juli 1615",
        answerC = "12. August 1620",
        answerD = "8. November 1620",
        correctAnswer = 0,
        explanation = "Am 23. Mai 1618 warfen protestantische böhmische Adlige die kaiserlichen Statthalter Slavata und Martinitz sowie deren Schreiber aus einem Fenster der Prager Burg – der 'Prager Fenstersturz'. Alle drei überlebten den Sturz.",
        difficulty = 3,
        funFact = "Die Überlebenden des Fenstersturzes behaupteten, von Engeln gerettet worden zu sein – Protestanten spotteten dagegen, sie seien auf einem Misthaufen gelandet, was ihnen das Leben gerettet habe."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher Schlacht von 1620 wurde das protestantische böhmische Heer entscheidend besiegt und die Herrschaft der Habsburger über Böhmen gefestigt?",
        answerA = "Weißer Berg bei Prag",
        answerB = "Breitenfeld bei Leipzig",
        answerC = "Lützen bei Leipzig",
        answerD = "Nördlingen",
        correctAnswer = 0,
        explanation = "Die Schlacht am Weißen Berg (Bílá hora) am 8. November 1620 bei Prag endete mit der totalen Niederlage des böhmischen Ständeheeres. Die Folge war die Rekatholisierung Böhmens und der Verlust der böhmischen Autonomie.",
        difficulty = 3,
        funFact = "27 Anführer des böhmischen Aufstands wurden 1621 auf dem Prager Altstädter Ring hingerichtet – ihre Köpfe wurden zur Abschreckung jahrelang an der Karlsbrücke ausgestellt."
    ),

    Question(
        categoryId = 3,
        questionText = "Albrecht von Wallenstein wurde zweimal als kaiserlicher Generalissimus eingesetzt. In welchem Jahr wurde er zum ersten Mal entlassen?",
        answerA = "1625",
        answerB = "1630",
        answerC = "1633",
        answerD = "1628",
        correctAnswer = 1,
        explanation = "Wallenstein wurde 1630 auf dem Regensburger Kurfürstentag auf Druck der katholischen Fürsten, die seine Macht fürchteten, von Kaiser Ferdinand II. entlassen. 1631 wurde er nach der Katastrophe von Breitenfeld erneut eingesetzt.",
        difficulty = 3,
        funFact = "Wallenstein finanzierte seine Armee durch ein System der 'Kontribution' – er zwang besetzte Gebiete, seine Truppen zu unterhalten. Dieses Modell wurde als 'Wallensteinsches Finanzierungssystem' bekannt."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo und unter welchen Umständen wurde Albrecht von Wallenstein 1634 ermordet?",
        answerA = "In Wien durch kaiserliche Gardisten",
        answerB = "In Eger durch irische und schottische Offiziere im kaiserlichen Dienst",
        answerC = "In Prag durch böhmische Verschwörer",
        answerD = "In Regensburg nach einem Gerichtsurteil",
        correctAnswer = 1,
        explanation = "Wallenstein wurde am 25. Februar 1634 in Eger (Cheb) von irischen und schottischen Offizieren unter Walter Devereux ermordet, nachdem Kaiser Ferdinand II. ihn wegen Hochverrats für vogelfrei erklärt hatte. Der Kaiser hatte heimlich seine Ermordung gebilligt.",
        difficulty = 3,
        funFact = "Schiller verarbeitete Wallensteins Aufstieg und Fall in seiner berühmten Wallenstein-Trilogie (1800) – einem der bedeutendsten deutschen Dramen, das bis heute aufgeführt wird."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher entscheidenden Schlacht von 1631 besiegte das schwedisch-sächsische Heer unter Gustav Adolf die kaiserliche Armee unter Tilly?",
        answerA = "Erste Schlacht bei Breitenfeld",
        answerB = "Schlacht bei Lützen",
        answerC = "Schlacht bei Nördlingen",
        answerD = "Schlacht am Weißen Berg",
        correctAnswer = 0,
        explanation = "Die Erste Schlacht bei Breitenfeld am 17. September 1631 war ein entscheidender Sieg Gustav Adolfs von Schweden und des Kurfürsten Johann Georg von Sachsen über die kaiserlich-ligistische Armee unter Tilly. Sie stoppte den Vormarsch der Gegenreformation.",
        difficulty = 3,
        funFact = "Gustav Adolf ließ seine Soldaten vor der Schlacht ein Kirchenlied singen – 'Eine feste Burg ist unser Gott' – was die protestantische Symbolkraft der Reformation in den Krieg trug."
    ),

    Question(
        categoryId = 3,
        questionText = "Bei welcher Schlacht fiel Gustav Adolf von Schweden 1632, obwohl seine Armee siegreich blieb?",
        answerA = "Breitenfeld",
        answerB = "Nördlingen",
        answerC = "Lützen",
        answerD = "Wittstock",
        correctAnswer = 2,
        explanation = "Gustav Adolf II. von Schweden fiel am 16. November 1632 in der Schlacht bei Lützen nahe Leipzig. Obwohl die Schweden die Schlacht gewannen, war sein Tod ein schwerer Schlag für die protestantische Sache.",
        difficulty = 3,
        funFact = "Der genaue Hergang von Gustav Adolfs Tod ist bis heute ungeklärt – Historiker streiten, ob er im Kampfgetümmel fiel oder gezielt ermordet wurde. Sein Pferd galoppierte ohne Reiter aus dem Gefecht."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche zwei wichtigen Bestimmungen des Westfälischen Friedens von 1648 regelten die territoriale Neuordnung Europas?",
        answerA = "Osnabrücker Frieden (mit Schweden) und Münsteraner Frieden (mit Frankreich)",
        answerB = "Prager Frieden und Westfälischer Frieden",
        answerC = "Nürnberger Exekutionskongress und Westfälischer Frieden",
        answerD = "Pyrenäenfrieden und Westfälischer Frieden",
        correctAnswer = 0,
        explanation = "Der Westfälische Frieden bestand aus zwei Verträgen: dem Instrumentum Pacis Osnabrugensis (mit Schweden, unterzeichnet in Osnabrück) und dem Instrumentum Pacis Monasteriensis (mit Frankreich, unterzeichnet in Münster), beide am 24. Oktober 1648.",
        difficulty = 3,
        funFact = "Die Friedensverhandlungen dauerten von 1644 bis 1648 – sie gelten als erster moderner Diplomatenkongress Europas, bei dem alle beteiligten Mächte gleichzeitig verhandelten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Prinzip des Westfälischen Friedens von 1648 legte das Normaljahr 1624 als Grundlage für die Besitzstände der Konfessionen fest?",
        answerA = "Uti possidetis",
        answerB = "Normaljahr-Regelung",
        answerC = "Status quo ante bellum",
        answerD = "Restitutionsedikt",
        correctAnswer = 1,
        explanation = "Die Normaljahr-Regelung des Westfälischen Friedens legte fest, dass der konfessionelle Zustand des Jahres 1624 als Maßstab für alle Restitutionsfragen gilt – eine direkte Reaktion auf das gescheiterte Restitutionsedikt von 1629.",
        difficulty = 3,
        funFact = "Schweden erhielt im Westfälischen Frieden Vorpommern, Wismar und die Bistümer Bremen und Verden als 'Satisfaktion' – und damit einen Fuß auf deutschem Boden, was sein Interesse am Reich für Generationen sicherte."
    ),

    // --- English Civil War ---

    Question(
        categoryId = 3,
        questionText = "Welches politische Prinzip stritt Karl I. von England mit dem Parlament aus, das zum Englischen Bürgerkrieg führte – bekannt als Konflikt um das 'Ship Money'?",
        answerA = "Das Recht auf Habeas Corpus",
        answerB = "Das königliche Recht, ohne parlamentarische Zustimmung Steuern zu erheben",
        answerC = "Das Recht des Königs, das Parlament aufzulösen",
        answerD = "Die Bischofsverfassung der anglikanischen Kirche",
        correctAnswer = 1,
        explanation = "Karl I. erhob ab 1634 die 'Ship Money'-Steuer ohne parlamentarische Bewilligung auf ganz England (nicht nur auf Küstengebiete). Dies widersprach der Petition of Right (1628) und wurde zum zentralen Streitpunkt zwischen Krone und Parlament.",
        difficulty = 3,
        funFact = "John Hampden weigerte sich 1637, das Ship Money zu zahlen, und wurde vor Gericht gestellt. Obwohl er verlor (7:5 Richterstimmen), wurde er ein Volksheld und sein Prozess machte das Steuerproblem landesweit bekannt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches parlamentarische Dokument von 1641 zählte in 204 Punkten die Missstände der Herrschaft Karls I. auf und beschleunigte den Bruch zwischen König und Parlament?",
        answerA = "Petition of Right",
        answerB = "Große Remonstranz",
        answerC = "Instrumentum of Government",
        answerD = "Bill of Rights",
        correctAnswer = 1,
        explanation = "Die Große Remonstranz (Grand Remonstrance) vom November 1641 listete 204 Beschwerden gegen die Regierung Karls I. auf. Sie wurde im Parlament mit knapper Mehrheit (159:148) angenommen und markierte die Spaltung des Parlaments in zwei Lager.",
        difficulty = 3,
        funFact = "Oliver Cromwell soll nach der knappen Abstimmung gesagt haben, wäre sie gescheitert, hätte er England verlassen und sei nach Amerika ausgewandert – das hätte den Gang der Geschichte grundlegend geändert."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher entscheidenden Feldschlacht von 1645 besiegten Cromwells 'New Model Army' und die Parlamentstruppen das königliche Heer Karls I. vernichtend?",
        answerA = "Edgehill",
        answerB = "Marston Moor",
        answerC = "Naseby",
        answerD = "Preston",
        correctAnswer = 2,
        explanation = "Die Schlacht bei Naseby am 14. Juni 1645 war die entscheidende Niederlage Karls I. im Ersten Englischen Bürgerkrieg. Cromwells Kavallerie, die 'Ironsides', durchbrach die königliche Linie, und fast das gesamte Heer des Königs wurde vernichtet oder gefangen genommen.",
        difficulty = 3,
        funFact = "Nach der Niederlage bei Naseby fielen Karls private Briefe in Parlamentshände – sie enthüllten, dass er heimlich mit Irland und Frankreich verhandelte und ausländische Truppen anwerben wollte. Das kostete ihn viel Rückhalt."
    ),

    Question(
        categoryId = 3,
        questionText = "Karl I. von England wurde am 30. Januar 1649 hingerichtet – auf welcher rechtlichen Grundlage wurde er verurteilt?",
        answerA = "Nach einem Hochverratsprozess vor dem Oberhaus",
        answerB = "Durch ein Sondergericht, das vom Unterhaus eingesetzt wurde, da Karl I. keinen normalen Prozess anerkannte",
        answerC = "Durch ein Volksgeschworenengericht in Westminster",
        answerD = "Durch ein Militärtribunal unter Oliver Cromwell",
        correctAnswer = 1,
        explanation = "Das Unterhaus errichtete ein außerordentliches Hohen Gericht, da Karl I. jede Zuständigkeit ablehnte ('Ich anerkenne kein irdisches Gericht über einen König'). 59 von 135 Richtern unterzeichneten das Todesurteil.",
        difficulty = 3,
        funFact = "Karl I. trug bei seiner Hinrichtung zwei Hemden, damit er nicht zitterte – er wollte nicht, dass das Volk glaubte, er zittere vor Angst statt vor Kälte."
    ),

    Question(
        categoryId = 3,
        questionText = "Unter welchem Titel regierte Oliver Cromwell England von 1653 bis zu seinem Tod 1658?",
        answerA = "König von England",
        answerB = "Protektor der Republik",
        answerC = "Lord Protektor des Commonwealth",
        answerD = "Präsident des Staatsrates",
        correctAnswer = 2,
        explanation = "Oliver Cromwell regierte England als 'Lord Protektor des Commonwealth von England, Schottland und Irland' – er lehnte die ihm angebotene Königskrone ab, übte aber faktisch eine monarchische Macht aus.",
        difficulty = 3,
        funFact = "Nach der Restauration Karls II. (1660) ließ dieser Cromwells Leiche exhumieren und symbolisch hängen – sein Kopf wurde auf einem Pfahl aufgesteckt und blieb dort bis 1685."
    ),

    // --- Glorious Revolution ---

    Question(
        categoryId = 3,
        questionText = "Was löste die Glorious Revolution von 1688 aus, die zur Vertreibung Jakobs II. führte?",
        answerA = "Jakobs II. Heirat mit einer französischen Katholikin",
        answerB = "Die Geburt eines katholischen Thronerben, der eine dauerhafte katholische Linie befürchten ließ",
        answerC = "Jakobs II. Bündnis mit Ludwig XIV. gegen das Parlament",
        answerD = "Die Auflösung des Parlaments durch Jakob II.",
        correctAnswer = 1,
        explanation = "Die Geburt eines Sohnes Jakobs II. im Juni 1688 – James Francis Edward Stuart – löste die Krise aus: Nun drohte eine dauerhafte katholische Thronfolge. Sieben führende Whigs und Tories luden daraufhin Wilhelm von Oranien zur Invasion ein.",
        difficulty = 3,
        funFact = "Gerüchte behaupteten, das Baby sei gar nicht das echte Kind Jakobs, sondern heimlich in einem Wärmepfannen-Behälter ins Bett geschmuggelt worden – dieser Mythos des 'warming pan baby' hielt sich jahrzehntelang."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Dokument von 1689 begrenzte die Macht der englischen Krone dauerhaft und gilt als Grundlage der konstitutionellen Monarchie Englands?",
        answerA = "Magna Carta",
        answerB = "Habeas Corpus Act",
        answerC = "Bill of Rights",
        answerD = "Act of Settlement",
        correctAnswer = 2,
        explanation = "Die Bill of Rights von 1689 legte fest, dass der König ohne parlamentarische Zustimmung keine Steuern erheben, keine stehende Armee aufstellen und keine Gesetze suspendieren darf. Sie ist die Grundlage der englischen Verfassungsordnung.",
        difficulty = 3,
        funFact = "Die amerikanische Bill of Rights von 1791 war direkt von der englischen Bill of Rights inspiriert – viele der Garantien wie das Recht auf eine bewaffnete Bürgerschaft und das Verbot grausamer Strafen wurden direkt übernommen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was versteht man unter dem Begriff 'Convention Parliament' im Zusammenhang mit der Glorious Revolution 1689?",
        answerA = "Das Parlament, das die Verfassung neu schrieb",
        answerB = "Ein ohne königliche Einberufung zusammengetretenes Parlament, das Wilhelm III. die Krone anbot",
        answerC = "Das Parlament, das die Krönung Wilhelms III. organisierte",
        answerD = "Eine Notversammlung in Zeiten der Regentschaft",
        correctAnswer = 1,
        explanation = "Das Convention Parliament von 1689 war ein Parlament, das sich ohne die formelle Einberufung durch den König versammelte (da Jakob II. geflohen war). Es erklärte den Thron für vakant und übertrug die Krone gemeinsam an Wilhelm III. und Maria II.",
        difficulty = 3,
        funFact = "Dies war erst das zweite 'Convention Parliament' in der englischen Geschichte – das erste hatte 1660 die Restauration Karls II. organisiert. In beiden Fällen musste die fehlende königliche Einberufung nachträglich legalisiert werden."
    ),

    // --- Scientific Revolution ---

    Question(
        categoryId = 3,
        questionText = "In welchem Werk von 1543 stellte Nikolaus Kopernikus das heliozentrische Weltbild vor, und welche strategische Widmung enthielt es?",
        answerA = "Almagest, gewidmet dem Kaiser",
        answerB = "De revolutionibus orbium coelestium, gewidmet Papst Paul III.",
        answerC = "Sidereus Nuncius, gewidmet dem Dogen von Venedig",
        answerD = "Astronomia Nova, gewidmet Kaiser Rudolf II.",
        correctAnswer = 1,
        explanation = "'De revolutionibus orbium coelestium' (Über die Umdrehungen der Himmelssphären) wurde Papst Paul III. gewidmet – eine strategische Schutzwidmung, um kirchliche Zensur zu vermeiden. Das Buch erschien kurz vor Kopernikus' Tod.",
        difficulty = 3,
        funFact = "Kopernikus soll das fertig gedruckte Exemplar seines Werkes erst auf seinem Sterbebett in den Händen gehalten haben – ein Schlaganfall hatte ihn monatelang gelähmt."
    ),

    Question(
        categoryId = 3,
        questionText = "Vor welchem kirchlichen Gericht wurde Galileo Galilei 1633 verurteilt, und zu welcher Strafe wurde er verurteilt?",
        answerA = "Vor dem Konzil von Trient – zu Gefängnishaft",
        answerB = "Vor der Römischen Inquisition – zu lebenslangem Hausarrest und dem Widerruf seiner Lehren",
        answerC = "Vor dem päpstlichen Tribunal – zur Verbrennung auf dem Scheiterhaufen",
        answerD = "Vor der Spanischen Inquisition – zur Verbannung aus dem Kirchenstaat",
        correctAnswer = 1,
        explanation = "Galilei wurde 1633 vor der Kongregation der Heiligen Römischen Inquisition in Rom verhört und verurteilt. Er musste seine Lehren widerrufen und verbrachte den Rest seines Lebens unter Hausarrest in Arcetri bei Florenz.",
        difficulty = 3,
        funFact = "Die Geschichte, Galilei habe nach seinem Widerruf gemurrt 'Eppur si muove' (Und sie bewegt sich doch), ist historisch nicht belegbar – sie taucht erst über ein Jahrhundert nach seinem Tod in Quellen auf."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches astronomische Instrument konstruierte Galilei 1609, obwohl er es nicht erfand, und womit überraschte er die Gelehrten durch seine Beobachtungen?",
        answerA = "Das Teleskop – er entdeckte die vier Jupitermonde (Galileische Monde)",
        answerB = "Das Fernrohr – er bewies die Kugelgestalt der Erde",
        answerC = "Das Astrolabium – er berechnete die genaue Erdumlaufbahn",
        answerD = "Das Helioskop – er entdeckte die Sonnenflecken als erster",
        correctAnswer = 0,
        explanation = "Galilei verbesserte das aus den Niederlanden stammende Teleskop erheblich und richtete es als erster systematisch auf den Himmel. Er entdeckte 1610 die vier großen Jupitermonde, die heute als Galileische Monde bekannt sind.",
        difficulty = 3,
        funFact = "Galilei benannte die vier Jupitermonde zunächst nach seinem Gönner Cosimo de' Medici als 'Mediceische Sterne' – in der Hoffnung, dessen finanzielle Unterstützung zu sichern. Die heute gebräuchlichen Namen (Io, Europa, Ganymed, Kallisto) stammen von Simon Marius."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Werk von Isaac Newton von 1687 legte die Grundlagen der klassischen Mechanik und das Gravitationsgesetz nieder?",
        answerA = "Opticks",
        answerB = "Principia Mathematica",
        answerC = "Philosophiae Naturalis Principia Mathematica",
        answerD = "De Motu Corporum",
        correctAnswer = 2,
        explanation = "Newtons 'Philosophiae Naturalis Principia Mathematica' (1687) formulierte die drei Bewegungsgesetze und das universelle Gravitationsgesetz. Es gilt als eines der bedeutendsten Werke der Wissenschaftsgeschichte.",
        difficulty = 3,
        funFact = "Die 'Principia' wurden auf Drängen und Kosten von Edmund Halley veröffentlicht – Newton selbst hätte sie wohl nie publiziert. Halley (nach dem der Halleysche Komet benannt ist) redigierte das Manuskript und bezahlte den Druck aus eigener Tasche."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Werk von Andreas Vesalius aus dem Jahr 1543 revolutionierte die Anatomie durch direkte Leichensektionen statt antiker Autoritäten?",
        answerA = "De Humani Corporis Fabrica",
        answerB = "Corpus Hippocraticum",
        answerC = "Anatomia Corporis Humani",
        answerD = "De Medicina",
        correctAnswer = 0,
        explanation = "'De Humani Corporis Fabrica' (Über den Bau des menschlichen Körpers) von Andreas Vesalius korrigierte Hunderte von Fehlern in Galens antiker Anatomie durch direkte Beobachtung und wurde mit präzisen Holzschnitten illustriert.",
        difficulty = 3,
        funFact = "Vesalius war erst 28 Jahre alt, als er die 'Fabrica' veröffentlichte – Galen, den er widerlegte, war über 1.300 Jahre lang die unangefochtene Autorität in der Medizin gewesen."
    ),

    // --- Enlightenment Philosophers ---

    Question(
        categoryId = 3,
        questionText = "Welches politische Konzept entwickelte John Locke in seinen 'Two Treatises of Government' (1689), das die amerikanische Unabhängigkeitserklärung direkt beeinflusste?",
        answerA = "Die Gewaltenteilung in drei Zweige",
        answerB = "Der Gesellschaftsvertrag und das Recht auf Revolution bei Verletzung natürlicher Rechte",
        answerC = "Das Mehrheitsprinzip in Demokratien",
        answerD = "Die Souveränität des Volkes durch Volksentscheide",
        correctAnswer = 1,
        explanation = "Locke entwickelte die Theorie, dass Menschen natürliche Rechte auf Leben, Freiheit und Eigentum besitzen. Regierungen entstehen durch einen Gesellschaftsvertrag; verletzt eine Regierung diese Rechte, hat das Volk das Recht auf Revolution.",
        difficulty = 3,
        funFact = "Thomas Jefferson übernahm Lockes Formulierung 'life, liberty and property' fast wörtlich – änderte aber 'property' in 'pursuit of happiness', um Sklaverei nicht implizit als natürliches Recht zu schützen."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Werk entwickelte Montesquieu 1748 das Prinzip der Gewaltenteilung in Legislative, Exekutive und Judikative?",
        answerA = "Lettres persanes",
        answerB = "De l'esprit des lois",
        answerC = "Contrat social",
        answerD = "Encyclopédie",
        correctAnswer = 1,
        explanation = "'De l'esprit des lois' (Vom Geist der Gesetze) von Montesquieu entwickelte die Gewaltenteilung als Schutz vor Tyrannei. Die amerikanische Verfassung und viele europäische Verfassungen basierten direkt auf diesem Prinzip.",
        difficulty = 3,
        funFact = "Montesquieu nahm das englische Verfassungssystem als Vorbild für seine Theorie – ironischerweise hatte England zu seiner Zeit keine klare Gewaltenteilung, er idealisierte das britische Modell erheblich."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie bezeichnete Jean-Jacques Rousseau in seinem 'Contrat social' (1762) den allgemeinen Willen, der über dem Willen einzelner Individuen steht?",
        answerA = "Volonté générale",
        answerB = "Volonté de tous",
        answerC = "La souveraineté populaire",
        answerD = "L'intérêt commun",
        correctAnswer = 0,
        explanation = "Rousseaus 'Volonté générale' (Allgemeinwille) ist das Kernkonzept des Contrat Social: Es ist nicht die Summe aller Einzelwillen, sondern das, was im wahren Interesse der gesamten Gemeinschaft liegt – ein Konzept, das später für Diktaturen als Rechtfertigung missbraucht wurde.",
        difficulty = 3,
        funFact = "Rousseau und Voltaire hassten sich trotz ihrer gemeinsamen aufklärerischen Überzeugungen zutiefst: Rousseau schickte Voltaire sein Werk über die natürliche Ungleichheit, woraufhin Voltaire antwortete, er habe nie eine solche 'Dummheit' gelesen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche philosophische Idee verband Voltaire mit dem Begriff 'ecrasez l'infame' (zerschmettert die Gemeine)?",
        answerA = "Die Ablehnung von Monarchie und Adel",
        answerB = "Die Bekämpfung von religiösem Fanatismus und kirchlicher Intoleranz",
        answerC = "Der Kampf gegen feudale Leibeigenschaft",
        answerD = "Die Forderung nach demokratischen Wahlen",
        correctAnswer = 1,
        explanation = "'Écrasez l'infâme' war Voltaires Kampfbegriff gegen religiösen Fanatismus, kirchliche Intoleranz und Aberglauben. Er schloss seine Briefe häufig mit diesem Ausdruck und bekämpfte kirchlichen Einfluss auf Justiz und Politik.",
        difficulty = 3,
        funFact = "Voltaire kämpfte öffentlich für die Rehabilitation des Protestant Jean Calas, der 1762 auf der Folter gestorben war, nachdem er zu Unrecht beschuldigt wurde, seinen Sohn ermordet zu haben, um ihn an der Konversion zum Katholizismus zu hindern."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches mehrbändige Enzyklopädie-Werk, herausgegeben von Denis Diderot und Jean le Rond d'Alembert, fasste das Wissen der Aufklärung zusammen und wurde in Frankreich mehrfach verboten?",
        answerA = "Encyclopaedia Britannica",
        answerB = "Encyclopédie ou Dictionnaire raisonné des sciences, des arts et des métiers",
        answerC = "Grande Encyclopédie universelle",
        answerD = "Dictionnaire philosophique",
        correctAnswer = 1,
        explanation = "Die 'Encyclopédie' (1751–1772) von Diderot und d'Alembert umfasste 28 Bände mit 71.818 Artikeln und 2.885 Bildtafeln. Sie propagierte Vernunft und Fortschritt und wurde mehrfach vom Pariser Parlament und der Kirche verboten.",
        difficulty = 3,
        funFact = "Diderot verbrachte kurz in Haft, weil sein frühes Werk als anstößig galt. Sein Mitherausgeber d'Alembert zog sich aus dem Projekt zurück, als der Druck zu groß wurde – Diderot vollendete die Encyclopédie allein."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Begriff verwendete Immanuel Kant in seinem Essay 'Was ist Aufklärung?' (1784) für den Zustand, aus dem die Menschen sich befreien sollen?",
        answerA = "Die Barbarei",
        answerB = "Die selbstverschuldete Unmündigkeit",
        answerC = "Die religiöse Unfreiheit",
        answerD = "Der Naturzustand",
        correctAnswer = 1,
        explanation = "Kant definierte Aufklärung als 'Ausgang des Menschen aus seiner selbstverschuldeten Unmündigkeit' und formulierte den berühmten Imperativ 'Sapere aude!' (Wage, dich deines eigenen Verstandes zu bedienen!).",
        difficulty = 3,
        funFact = "Kant selbst führte ein ausgesprochen geregeltes Leben in Königsberg und soll die Stadt niemals verlassen haben – sein Tagesrhythmus war so präzise, dass die Bürger ihre Uhren nach seinem täglichen Spaziergang stellen konnten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Philosophen der schottischen Aufklärung entwickelte in 'The Wealth of Nations' (1776) die Theorie der 'unsichtbaren Hand' des Marktes?",
        answerA = "David Hume",
        answerB = "Adam Ferguson",
        answerC = "Adam Smith",
        answerD = "Francis Hutcheson",
        correctAnswer = 2,
        explanation = "Adam Smith entwickelte in 'An Inquiry into the Nature and Causes of the Wealth of Nations' (1776) das Konzept der 'unsichtbaren Hand': Individuen, die ihren Eigeninteressen folgen, fördern unbeabsichtigt das Gemeinwohl – die Grundlage der klassischen Wirtschaftslehre.",
        difficulty = 3,
        funFact = "Adam Smith erwähnte die 'unsichtbare Hand' in seinem berühmten Werk nur einmal beiläufig – der Begriff wurde erst im 20. Jahrhundert zur zentralen Metapher des freien Marktes stilisiert."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Aufklärungsphilosoph schrieb 'Dei delitti e delle pene' (Über Verbrechen und Strafen, 1764) und forderte die Abschaffung der Todesstrafe und Folter?",
        answerA = "Giambattista Vico",
        answerB = "Cesare Beccaria",
        answerC = "Pietro Verri",
        answerD = "Gaetano Filangieri",
        correctAnswer = 1,
        explanation = "Der Mailänder Jurist Cesare Beccaria forderte in 'Dei delitti e delle pene' (1764) die Abschaffung der Todesstrafe und Folter sowie das Prinzip der Verhältnismäßigkeit von Strafe und Vergehen. Es war eines der einflussreichsten Rechtsbücher des 18. Jahrhunderts.",
        difficulty = 3,
        funFact = "Beccaria war so schüchtern, dass er sein revolutionäres Werk zunächst anonym veröffentlichte – erst nach dem enormen Erfolg und dem Lob von Voltaire bekannte er sich zu seiner Autorschaft."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Konzept entwickelte Francis Bacon in 'Novum Organum' (1620), das die Grundlage der modernen wissenschaftlichen Methode bildete?",
        answerA = "Die deduktive Methode der Logik",
        answerB = "Die induktive Methode und das Experiment als Erkenntnisquelle",
        answerC = "Die mathematische Beweisführung",
        answerD = "Die Autoritäten der Antike als Wissensbasis",
        correctAnswer = 1,
        explanation = "Francis Bacon entwickelte in 'Novum Organum' die induktive Methode: Wissen soll aus systematischen Beobachtungen und Experimenten abgeleitet werden, statt aus antiken Autoritäten. Er gilt als Begründer des empirischen Wissenschaftsideals.",
        difficulty = 3,
        funFact = "Bacon starb möglicherweise als Märtyrer der Wissenschaft: Er soll bei einem Experiment zum Einfrieren von Fleisch eine schwere Erkältung erlitten haben, an der er kurz darauf starb – er wollte prüfen, ob Schnee Fleisch konservieren kann."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Naturphilosoph formulierte 1619 die drei Keplerschen Gesetze, die die elliptischen Bahnen der Planeten beschreiben?",
        answerA = "Tycho Brahe",
        answerB = "Johannes Kepler",
        answerC = "Galileo Galilei",
        answerD = "Christiaan Huygens",
        correctAnswer = 1,
        explanation = "Johannes Kepler formulierte die drei Gesetze der Planetenbewegung: Planeten bewegen sich auf Ellipsen (1. Gesetz), überstreichen gleiche Flächen in gleichen Zeiten (2. Gesetz), und das Quadrat der Umlaufzeit verhält sich zum Kubus der großen Halbachse (3. Gesetz, 1619).",
        difficulty = 3,
        funFact = "Keplers Berechnungen basierten auf den Beobachtungsdaten von Tycho Brahe, den er nach Prag beerbt hatte. Brahe hatte sein Leben lang gegen das heliozentrische Weltbild gekämpft – seine eigenen Daten widerlegten ihn posthum."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Philosoph formulierte in 'Meditationes de prima philosophia' (1641) den berühmten Grundsatz 'Cogito, ergo sum' als unerschütterliche Grundlage der Erkenntnis?",
        answerA = "Baruch Spinoza",
        answerB = "Gottfried Wilhelm Leibniz",
        answerC = "René Descartes",
        answerD = "John Locke",
        correctAnswer = 2,
        explanation = "René Descartes leitete in den 'Meditationes' aus dem radikalen Zweifel an allem die einzig sichere Erkenntnis ab: 'Ich denke, also bin ich' (Cogito, ergo sum) – da das Denken selbst nicht bezweifelt werden kann.",
        difficulty = 3,
        funFact = "Descartes begann seinen systematischen Zweifel mit der Frage, ob er vielleicht von einem bösen Dämon getäuscht werde, der ihm eine falsche Realität vortäusche – eine Idee, die im Film 'Matrix' wiederaufgegriffen wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches 'Toleranzedikt' erließ Ludwig XVI. von Frankreich 1787, das Nichtkatholiken erstmals bürgerliche Rechte gewährte?",
        answerA = "Edikt von Fontainebleau",
        answerB = "Edikt von Versailles",
        answerC = "Edikt von Nantes",
        answerD = "Edikt von Compiègne",
        correctAnswer = 1,
        explanation = "Das Edikt von Versailles (1787) gewährte Protestanten und anderen Nichtkatholiken das Recht auf Ehe, Eigentumsrechte und ein legales Begräbnis – ohne sie jedoch zur politischen Teilhabe zuzulassen. Es war ein Produkt des aufklärerischen Einflusses auf die Reformpolitik Ludwigs XVI.",
        difficulty = 3,
        funFact = "Das Edikt von Nantes (1598) hatte Hugenotten weitgehende Rechte gegeben – Ludwig XIV. hatte es 1685 durch das Edikt von Fontainebleau widerrufen, was zur Emigration von 200.000 Hugenotten aus Frankreich führte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Aufklärungsphilosoph beschrieb in 'Leviathan' (1651) den Naturzustand als 'Krieg aller gegen alle' und plädierte für einen starken absolutistischen Staat?",
        answerA = "Jean-Jacques Rousseau",
        answerB = "John Locke",
        answerC = "Thomas Hobbes",
        answerD = "Hugo Grotius",
        correctAnswer = 2,
        explanation = "Thomas Hobbes beschrieb in 'Leviathan' den Naturzustand als 'bellum omnium contra omnes' (Krieg aller gegen alle), in dem das Leben 'einsam, armselig, ekelhaft, brutal und kurz' sei. Zur Sicherheit müssten Menschen alle Macht an einen Souverän abgeben.",
        difficulty = 3,
        funFact = "Hobbes schrieb den 'Leviathan' im Pariser Exil, wohin er geflohen war – einerseits weil er die königliche Seite im Englischen Bürgerkrieg unterstützt hatte, andererseits weil seine Ideen dem englischen Klerus zu atheistisch erschienen."
    ),

    // --- Additional Hard History Questions ---

    Question(
        categoryId = 3,
        questionText = "Welcher Begriff bezeichnete die protestantischen Fürsten, die 1529 auf dem Reichstag zu Speyer gegen das Verbot der weiteren Ausbreitung der Reformation 'protestierten'?",
        answerA = "Hugenotten",
        answerB = "Protestanten",
        answerC = "Lutheraner",
        answerD = "Reformierte",
        correctAnswer = 1,
        explanation = "Der Begriff 'Protestanten' entstand auf dem Reichstag zu Speyer 1529: Fünf Fürsten und 14 Reichsstädte protestierten förmlich gegen das Mehrheitsbeschluss, der die Ausbreitung der lutherischen Lehre verbot. Diese 'Protestation' gab der gesamten Bewegung ihren Namen.",
        difficulty = 3,
        funFact = "Das Wort 'Protestant' wurde ursprünglich als Schimpfwort verwendet – erst über Generationen wandelte es sich zu einem neutralen oder positiv besetzten Selbstbezeichner der reformierten Christen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Werk verfasste Erasmus von Rotterdam 1511, das mit beißendem Witz die Torheiten der Gesellschaft und Kirche kritisierte, aber die Reformation ablehnte?",
        answerA = "Utopia",
        answerB = "Laus Stultitiae (Lob der Torheit)",
        answerC = "De Civitate Dei",
        answerD = "Institutio Principis Christiani",
        correctAnswer = 1,
        explanation = "'Laus Stultitiae' (Lob der Torheit, 1511) von Erasmus von Rotterdam kritisierte Päpste, Bischöfe, Mönche und weltliche Herrscher mit satirischem Witz – ohne jedoch den Bruch mit der Kirche zu vollziehen, den Luther später wagte.",
        difficulty = 3,
        funFact = "Erasmus und Luther führten eine berühmte schriftliche Auseinandersetzung: Erasmus verteidigte den freien Willen des Menschen (De libero arbitrio, 1524), Luther widersprach vehement (De servo arbitrio, 1525) – einer der wichtigsten theologischen Debatten der Reformationszeit."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Instrument der Inquisition, 1486 von Heinrich Kramer verfasst, lieferte die theologische und rechtliche Grundlage für Hexenprozesse im späten 15. und 16. Jahrhundert?",
        answerA = "Directorium Inquisitorum",
        answerB = "Malleus Maleficarum",
        answerC = "Practica Inquisitionis",
        answerD = "Formicarius",
        correctAnswer = 1,
        explanation = "Der 'Malleus Maleficarum' (Hexenhammer) von Heinrich Kramer (1486) war das einflussreichste Handbuch zur Verfolgung von Hexen. Er beschrieb Hexerei als reales Phänomen, definierte Zeichen des Teufelspakts und legitimierte Folter und Hinrichtung.",
        difficulty = 3,
        funFact = "Der Hexenhammer wurde von der Inquisition selbst zunächst abgelehnt – Kramer fälschte eine päpstliche Approbation im Vorwort. Trotzdem wurde das Buch ein Bestseller: Es erschien bis 1669 in 29 Auflagen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Bündnis, das Frankreich mit protestantischen deutschen Fürsten einging, illustriert die politische Pragmatik des 16. Jahrhunderts, bei der Konfession Staatsinteressen untergeordnet wurde?",
        answerA = "Französisch-osmanisches Bündnis gegen Habsburg",
        answerB = "Französisch-protestantisches Bündnis im Schmalkaldischen Krieg",
        answerC = "Bündnis Heinrichs II. von Frankreich mit Moritz von Sachsen gegen Kaiser Karl V.",
        answerD = "Allianz Frankreichs mit den Schweizer Kantonen",
        correctAnswer = 2,
        explanation = "Heinrich II. von Frankreich, ein Katholik, verbündete sich 1551/52 mit dem lutherischen Kurfürsten Moritz von Sachsen gegen Kaiser Karl V. – das Bündnis des Passauer Vertrags. Frankreich erhielt dafür die Bistümer Metz, Toul und Verdun. Konfession stand hinter Machtpolitik zurück.",
        difficulty = 3,
        funFact = "Diese Realpolitik – das Verbünden mit dem religiösen 'Feind' aus strategischen Gründen – wurde zum Standardmodell europäischer Diplomatie und sollte im Dreißigjährigen Krieg noch extreme Formen annehmen, als das katholische Frankreich die protestantischen Schweden finanzierte."
    ),

)

