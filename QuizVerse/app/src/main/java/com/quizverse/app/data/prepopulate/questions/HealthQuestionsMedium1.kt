package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMedium1(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 16,
        questionText = "Welche Kammern hat das menschliche Herz?",
        answerA = "Zwei Kammern und keine Vorhoefe",
        answerB = "Zwei Vorhoefe und zwei Kammern",
        answerC = "Drei Kammern und einen Vorhof",
        answerD = "Vier Kammern ohne Vorhoefe",
        correctAnswer = 1,
        explanation = "Das Herz besteht aus zwei Vorhoehen (rechts und links) und zwei Kammern (rechts und links). Die rechte Seite pumpt Blut zur Lunge, die linke in den Koerperkreislauf.",
        difficulty = 2,
        funFact = "Das Herz eines ungeborenen Babys beginnt bereits in der dritten Schwangerschaftswoche zu schlagen -- also oft bevor die Schwangerschaft ueberhaupt bekannt ist."
    ),

    // Question 2
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Arterien und Venen?",
        answerA = "Arterien transportieren Blut zur Lunge, Venen transportieren Blut vom Herz weg",
        answerB = "Arterien fuehren Blut vom Herz weg, Venen fuehren Blut zum Herz zurueck",
        answerC = "Arterien und Venen transportieren beide sauerstoffreiches Blut",
        answerD = "Arterien sind dicker als Venen und haben weniger Aufgaben",
        correctAnswer = 1,
        explanation = "Arterien leiten Blut vom Herzen weg zu den Organen, Venen leiten Blut von den Organen zurueck zum Herzen. Die Aorta ist die groesste Arterie, die Hohlvene die groesste Vene.",
        difficulty = 2,
        funFact = "Die Aorta hat einen Durchmesser von etwa 2,5 Zentimetern -- das ist in etwa so gross wie ein Gartensprinklerschlauch."
    ),

    // Question 3
    Question(
        categoryId = 16,
        questionText = "Was ist Gasaustausch in der Lunge?",
        answerA = "Luft wird im Magen gefiltert und dann zur Lunge weitergeleitet",
        answerB = "Sauerstoff gelangt ins Blut und Kohlendioxid wird abgegeben",
        answerC = "Stickstoff aus der Luft wird in Energie umgewandelt",
        answerD = "Kohlendioxid wird in Sauerstoff umgewandelt",
        correctAnswer = 1,
        explanation = "In den Lungenblaeschen (Alveolen) gelangt Sauerstoff aus der eingeatmeten Luft ins Blut, waehrend gleichzeitig Kohlendioxid aus dem Blut in die Luft abgegeben und ausgeatmet wird.",
        difficulty = 2,
        funFact = "Die Lunge hat etwa 300 Millionen Alveolen. Wuerde man sie alle flach ausbreiten, haetten sie eine Gesamtflaeche von etwa 70 Quadratmetern."
    ),

    // Question 4
    Question(
        categoryId = 16,
        questionText = "Welche Hauptaufgabe hat die Leber im Stoffwechsel?",
        answerA = "Sie produziert Insulin und reguliert den Blutzucker",
        answerB = "Sie filtert das Blut und entgiftet Schadstoffe",
        answerC = "Sie pumpt das Blut durch den grossen Kreislauf",
        answerD = "Sie speichert Sauerstoff fuer Notfallsituationen",
        correctAnswer = 1,
        explanation = "Die Leber entgiftet das Blut, indem sie Schadstoffe wie Alkohol und Medikamente abbaut. Ausserdem produziert sie Galle, speichert Glykogen und stellt Blutgerinnungsfaktoren her.",
        difficulty = 2,
        funFact = "Die Leber ist das einzige innere Organ, das sich vollstaendig regenerieren kann. Selbst wenn 70 Prozent der Leber entfernt werden, waechst sie wieder auf volle Groesse nach."
    ),

    // Question 5
    Question(
        categoryId = 16,
        questionText = "Was ist Insulin und wo wird es produziert?",
        answerA = "Ein Verdauungsenzym, das in der Leber produziert wird",
        answerB = "Ein Hormon der Bauchspeicheldruse, das den Blutzucker senkt",
        answerC = "Ein Hormon der Nebenniere, das den Blutdruck reguliert",
        answerD = "Ein Botenstoff des Gehirns, der den Appetit steuert",
        correctAnswer = 1,
        explanation = "Insulin wird in den Betazellen der Langerhans-Inseln in der Bauchspeicheldruse gebildet. Es sorgt dafuer, dass Glukose aus dem Blut in die Koerperzellen aufgenommen werden kann.",
        difficulty = 2,
        funFact = "Insulin wurde 1921 von Frederick Banting und Charles Best entdeckt. Vor dieser Entdeckung war Diabetes Typ 1 fast immer toedlich."
    ),

    // Question 6
    Question(
        categoryId = 16,
        questionText = "Was bewirkt Adrenalin im Koerper?",
        answerA = "Es verlangsamt den Herzschlag und entspannt die Muskulatur",
        answerB = "Es bereitet den Koerper auf Kampf oder Flucht vor -- Herzschlag steigt, Muskeln werden aktiviert",
        answerC = "Es foerdert die Verdauung und senkt den Blutdruck",
        answerD = "Es reguliert den Schlaf-Wach-Rhythmus",
        correctAnswer = 1,
        explanation = "Adrenalin wird von der Nebenniere in Stresssituationen ausgeschuettet. Es erhoehlt Herzfrequenz und Blutdruck, weitet die Atemwege und stellt Energie fuer die Muskeln bereit.",
        difficulty = 2,
        funFact = "Adrenalin kann in Extremsituationen ausserordentliche koerperliche Leistungen ermoglichen -- daher gibt es Berichte von Menschen, die in Notfaellen schwere Gegenstaende heben konnten."
    ),

    // Question 7
    Question(
        categoryId = 16,
        questionText = "Welche Zellen des Immunsystems produzieren Antikoerper?",
        answerA = "Rote Blutkoerperchen",
        answerB = "Thrombozyten (Blutplaettchen)",
        answerC = "B-Lymphozyten (B-Zellen)",
        answerD = "Neutrophile Granulozyten",
        correctAnswer = 2,
        explanation = "B-Lymphozyten (B-Zellen) sind weisse Blutkoerperchen, die bei Kontakt mit einem Erreger zu Plasmazellen reifen und dann Antikoerper produzieren, die Krankheitserreger markieren.",
        difficulty = 2,
        funFact = "Ein einziger B-Lymphozyt kann zu einer Plasmazelle werden, die bis zu 2.000 Antikoerper pro Sekunde produziert."
    ),

    // Question 8
    Question(
        categoryId = 16,
        questionText = "Was ist die Hauptfunktion der Nieren?",
        answerA = "Blut mit Sauerstoff anreichern",
        answerB = "Hormone produzieren und in den Koerper abgeben",
        answerC = "Blut filtern und den Wasser- und Salzhaushalt regulieren",
        answerD = "Nahrung enzymatisch verdauen",
        correctAnswer = 2,
        explanation = "Die Nieren filtern taeglich etwa 180 Liter Blut, entfernen Harnstoff und andere Abfallstoffe und regulieren den Elektrolyt- sowie Wasserhaushalt des Koerpers.",
        difficulty = 2,
        funFact = "Obwohl die Nieren zusammen nur etwa 300 Gramm wiegen, erhalten sie rund 20 Prozent des Herzminutenvolumens -- einen sehr grossen Anteil des gesamten Blutflusses."
    ),

    // Question 9
    Question(
        categoryId = 16,
        questionText = "Was ist Melatonin und welche Funktion hat es?",
        answerA = "Ein Verdauungshormon, das nach dem Essen ausgeschuettet wird",
        answerB = "Ein Schlafhormon, das von der Zirbeldruse produziert wird und den Tag-Nacht-Rhythmus steuert",
        answerC = "Ein Stresshormon der Nebenniere",
        answerD = "Ein Wachstumshormon der Hypophyse",
        correctAnswer = 1,
        explanation = "Melatonin wird von der Zirbeldruse im Gehirn produziert, hauptsaechlich bei Dunkelheit. Es signalisiert dem Koerper, dass Schlafenszeit ist, und steuert so den zirkadianen Rhythmus.",
        difficulty = 2,
        funFact = "Blaues Licht von Smartphones und Bildschirmen hemmt die Melatoninproduktion besonders stark -- das ist einer der Gruende, warum abendlicher Bildschirmkonsum den Schlaf stoert."
    ),

    // Question 10
    Question(
        categoryId = 16,
        questionText = "Was ist Kortisol und in welchen Situationen wird es verstaerkt ausgeschuettet?",
        answerA = "Ein Schlafhormon, das bei Dunkelheit ausgeschuettet wird",
        answerB = "Ein Stresshormon der Nebenniere, das bei koumlrperlichem oder seelischem Stress erhoehlt ist",
        answerC = "Ein Verdauungshormon, das nach Mahlzeiten ansteigt",
        answerD = "Ein Immunhormon, das Enthuendungen foerdert",
        correctAnswer = 1,
        explanation = "Kortisol ist ein Glucocorticoid, das in der Nebennierenrinde gebildet wird. Bei Stress erhoelt es den Blutzucker, unterdruckt das Immunsystem kurzfristig und mobilisiert Energiereserven.",
        difficulty = 2,
        funFact = "Kortisol folgt einem tageszeitlichen Muster -- der Spiegel ist morgens am hoechsten (weckt uns quasi auf) und sinkt im Laufe des Tages ab."
    ),

    // Question 11
    Question(
        categoryId = 16,
        questionText = "Was ist das lymphatische System?",
        answerA = "Ein System, das Naehrstoffe aus dem Darm ins Blut uebertraegt",
        answerB = "Ein Netzwerk aus Gefaessen und Organen, das Fluessigkeit abtransportiert und das Immunsystem unterstuetzt",
        answerC = "Das System, das den Blutdruck reguliert",
        answerD = "Ein Druesenssystem, das Hormone produziert",
        correctAnswer = 1,
        explanation = "Das Lymphsystem besteht aus Lymphgefaessen, Lymphknoten, Milz und Thymus. Es transportiert Lymphfluessigkeit ab, filtert Krankheitserreger und ist ein zentraler Teil der Immunabwehr.",
        difficulty = 2,
        funFact = "Anders als das Blutgefaesssystem hat das Lymphsystem keine Pumpe -- die Lymphe wird durch Muskelbewegungen und die Atmung durch den Koerper transportiert."
    ),

    // Question 12
    Question(
        categoryId = 16,
        questionText = "Wozu dienen Lymphknoten?",
        answerA = "Sie pumpen Blut durch den koerperlichen Kreislauf",
        answerB = "Sie produzieren Hormone fuer das Wachstum",
        answerC = "Sie filtern Lymphfluessigkeit und beherbergen Immunzellen, die Krankheitserreger bekaempfen",
        answerD = "Sie speichern Reserven an Sauerstoff fuer Notfaelle",
        correctAnswer = 2,
        explanation = "Lymphknoten sind kleine bohnenfoermige Strukturen entlang der Lymphgefaesse. Sie filtern die Lymphfluessigkeit und enthalten Lymphozyten, die Krankheitserreger erkennen und bekaempfen.",
        difficulty = 2,
        funFact = "Wenn Lymphknoten anschwellen, ist das ein Zeichen, dass das Immunsystem aktiv gegen eine Infektion kaempft. Geschwollene Lymphknoten am Hals sind bei Halsschmerzen daher haeufig zu ertasten."
    ),

    // Question 13
    Question(
        categoryId = 16,
        questionText = "Was ist die Aufgabe des Duenndarms?",
        answerA = "Wasser und Salze aus dem Verdauungsbrei zurueckgewinnen",
        answerB = "Naehrstoffe aus der Nahrung aufnehmen und ins Blut uebergeben",
        answerC = "Unverdauliche Reste ausscheiden",
        answerD = "Nahrung mechanisch zerkleinern",
        correctAnswer = 1,
        explanation = "Im Duenndarm werden Naehrstoffe wie Zucker, Aminosaeuren und Fette aufgenommen und ins Blut transportiert. Die Falten und Zotten vergroessern die Aufnahmeflaeche enorm.",
        difficulty = 2,
        funFact = "Der Duenndarm ist trotz seines Namens mit etwa 5 bis 7 Metern Laenge viel laenger als der Dickdarm -- er ist nur im Durchmesser duenner."
    ),

    // Question 14
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet das sympathische vom parasympathischen Nervensystem?",
        answerA = "Das sympathische System steuert bewusste Bewegungen, das parasympathische unbewusste",
        answerB = "Das sympathische aktiviert den Koerper (Stress), das parasympathische beruhigt ihn (Ruhe und Verdauung)",
        answerC = "Das parasympathische System ist nur im Gehirn aktiv, das sympathische im Rueckenmark",
        answerD = "Beide Systeme haben identische Funktionen und wirken gemeinsam",
        correctAnswer = 1,
        explanation = "Das vegetative Nervensystem hat zwei Gegenspiele: Das Sympathikus aktiviert den Koerper fuer Kampf oder Flucht. Der Parasympathikus foerdert Ruhe, Erholung und Verdauung.",
        difficulty = 2,
        funFact = "Der Parasympathikus wird manchmal als 'Rest and Digest'-System bezeichnet, der Sympathikus als 'Fight or Flight'-System -- zwei Zustaende, die sich gegenseitig hemmen."
    ),

    // Question 15
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat der Thymus?",
        answerA = "Er produziert Schilddruesenhormon",
        answerB = "Er reift T-Lymphozyten heran, die fuer die Immunabwehr zustaendig sind",
        answerC = "Er speichert Blut als Reserve fuer Notfaelle",
        answerD = "Er filtert Abfallprodukte aus dem Lymphsystem",
        correctAnswer = 1,
        explanation = "Der Thymus ist ein Organ hinter dem Brustbein, in dem T-Lymphozyten (T-Zellen) ausgebildet und geprueft werden. Er ist besonders im Kindesalter aktiv und bildet sich im Erwachsenenalter zurueck.",
        difficulty = 2,
        funFact = "T-Zellen heissen so, weil sie im Thymus reifen -- das T steht fuer Thymus. Der Buchstabe B in B-Zellen stand urspruenglich fuer 'Bursa of Fabricius', ein Organ bei Voegeln."
    ),

    // Question 16
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen dem kleinen und grossen Blutkreislauf?",
        answerA = "Der grosse Kreislauf geht durch die Lunge, der kleine durch alle anderen Organe",
        answerB = "Der kleine Kreislauf verbindet Herz und Lunge, der grosse Kreislauf versorgt den restlichen Koerper",
        answerC = "Beide Kreislaeufe sind identisch, nur in verschiedene Richtungen",
        answerD = "Der kleine Kreislauf ist nur bei Kindern aktiv, der grosse bei Erwachsenen",
        correctAnswer = 1,
        explanation = "Im kleinen (Lungen-)Kreislauf wird sauerstoffarmes Blut zur Lunge gepumpt, dort mit Sauerstoff angereichert und zurueck zum Herz geleitet. Im grossen Kreislauf versorgt das Herz alle Organe.",
        difficulty = 2,
        funFact = "William Harvey beschrieb 1628 als erster den Blutkreislauf korrekt. Davor glaubten Aerzte, dass Blut im Koerper einfach verbraucht und staendig neu gebildet wird."
    ),

    // Question 17
    Question(
        categoryId = 16,
        questionText = "Welche Druesenart gibt ihre Produkte direkt ins Blut ab?",
        answerA = "Exokrine Druesen",
        answerB = "Endokrine Druesen",
        answerC = "Schweissdruesen",
        answerD = "Speicheldruesen",
        correctAnswer = 1,
        explanation = "Endokrine Druesen (hormonproduzierende Druesen) geben ihre Sekrete direkt ins Blut ab. Beispiele sind Schilddruese, Nebenniere und Bauchspeicheldruse. Exokrine Druesen leiten ihre Sekrete ueber Ausfuehrungsgaenge nach aussen.",
        difficulty = 2,
        funFact = "Das Wort 'endokrin' kommt vom griechischen 'endo' (innen) und 'krinein' (absondern) -- also 'nach innen absondernd'. Exokrin bedeutet entsprechend 'nach aussen absondernd'."
    ),

    // Question 18
    Question(
        categoryId = 16,
        questionText = "Was ist die Aufgabe der Alveolen in der Lunge?",
        answerA = "Sie filtern Staubpartikel aus der eingeatmeten Luft",
        answerB = "Sie sind die Orte des Gasaustausches zwischen Luft und Blut",
        answerC = "Sie produzieren den Schleim, der die Luftroehre auskleidet",
        answerD = "Sie regulieren die Atemfrequenz",
        correctAnswer = 1,
        explanation = "Alveolen sind winzige Luftblaeschen am Ende der Bronchiolen. Ihre duennen Waende und ihr dichtes Blutkapillarnetz ermoglichen den effizienten Austausch von Sauerstoff und Kohlendioxid.",
        difficulty = 2,
        funFact = "Jede Alveole ist von einem feinen Netz aus Kapillaren umgeben. Die Wand zwischen Alveolenluft und Blut ist nur 0,5 Mikrometer duenn -- duenner als ein Blatt Papier."
    ),

    // Question 19
    Question(
        categoryId = 16,
        questionText = "Welches Hormon wird als 'Stresshormon' und 'Weckhormon' bezeichnet?",
        answerA = "Insulin",
        answerB = "Melatonin",
        answerC = "Kortisol",
        answerD = "Oestrogen",
        correctAnswer = 2,
        explanation = "Kortisol wird in der Nebennierenrinde gebildet. Es steigt morgens stark an, um den Koerper aufzuwecken, und wird bei Stress verstaerkt ausgeschuettet, um Energie zu mobilisieren.",
        difficulty = 2,
        funFact = "Chronisch erhoehte Kortisolspiegel -- zum Beispiel durch Dauerstress -- koennen das Immunsystem schwaechenm den Schlaf stoeren und sogar Gedaechtnisabnahme bewirken."
    ),

    // Question 20
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Immungedaechtnis?",
        answerA = "Die Faehigkeit des Gehirns, Krankheitssymptome zu erinnern",
        answerB = "Die Faehigkeit des Immunsystems, nach Erstkontakt mit einem Erreger schneller auf denselben zu reagieren",
        answerC = "Das automatische Vergessen von Schmerzerlebnissen",
        answerD = "Die gespeicherten Informationen ueber Medikamente im Koerper",
        correctAnswer = 1,
        explanation = "Nach dem Erstkontakt mit einem Erreger bildet das Immunsystem Gedaechtniszellen. Bei erneutem Kontakt koennen diese viel schneller und staerker reagieren -- das ist das Prinzip der Immunisierung.",
        difficulty = 2,
        funFact = "Das Immungedaechtnis kann Jahrzehnte oder sogar ein Leben lang anhalten. Forscher fanden Antikoerper gegen die Spanische Grippe von 1918 noch bei Ueberlebenden im Jahr 2008."
    ),

    // Question 21
    Question(
        categoryId = 16,
        questionText = "Was ist die Milz und welche Aufgabe hat sie?",
        answerA = "Ein Organ im Bauchraum, das alte rote Blutkoerperchen abbaut und als Blutreservoir dient",
        answerB = "Eine Drue hinter dem Magen, die Verdauungsenzyme produziert",
        answerC = "Ein Teil des Gehirns, der Emotionen verarbeitet",
        answerD = "Ein Muskel, der bei der Atmung hilft",
        correctAnswer = 0,
        explanation = "Die Milz liegt links im Oberbauch und baut alte oder beschaedigte rote Blutkoerperchen ab. Gleichzeitig dient sie als Reservoir fuer Blut und unterstuetzt die Immunabwehr.",
        difficulty = 2,
        funFact = "Die Milz ist kein lebensnotwendiges Organ. Menschen ohne Milz koennen normal leben, haben aber ein erhoehtes Infektionsrisiko, besonders durch Bakterien mit Kapsel wie Pneumokokken."
    ),

    // Question 22
    Question(
        categoryId = 16,
        questionText = "Wie funktioniert die Blutdruckregulierung im Koerper?",
        answerA = "Der Blutdruck wird ausschliesslich durch die Herzfrequenz gesteuert",
        answerB = "Herz, Nieren, Hormone und Gefaessspannung wirken zusammen, um den Blutdruck konstant zu halten",
        answerC = "Die Lunge reguliert den Blutdruck durch die Atemfrequenz",
        answerD = "Der Blutdruck wird nur durch die Nahrungsaufnahme beeinflusst",
        correctAnswer = 1,
        explanation = "Der Blutdruck wird durch ein komplexes Zusammenspiel von Herzleistung, Gefaessweite, Blutvolumen und Hormonen wie Adrenalin und dem Renin-Angiotensin-System der Nieren reguliert.",
        difficulty = 2,
        funFact = "Der Blutdruck aendert sich staendig -- selbst das blossem Aufstehen aus dem Sitzen loest einen kurzen Blutdruckabfall aus, den der Koerper innerhalb von Sekunden ausgleicht."
    ),

    // Question 23
    Question(
        categoryId = 16,
        questionText = "Was sind T-Killerzellen?",
        answerA = "Spezielle rote Blutkoerperchen, die Sauerstoff binden",
        answerB = "Immunzellen, die virusinfizierte oder entartete Koerperzellen direkt toeten",
        answerC = "Zellen, die Antikoerper produzieren",
        answerD = "Zellen in der Lunge, die Staub abfangen",
        correctAnswer = 1,
        explanation = "Zytotoxische T-Zellen (T-Killerzellen) erkennen koerpereigene Zellen, die mit Viren infiziert sind oder entartet sind (Krebszellen), und vernichten sie gezielt durch Ausloesen des Zelltods.",
        difficulty = 2,
        funFact = "T-Killerzellen koennen bis zu 1.000 Zielzellen pro Tag abtoenm. Sie sind entscheidend fuer die Kontrolle von Virusinfektionen und spielen eine wichtige Rolle in der Krebsabwehr."
    ),

    // Question 24
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat die Schilddruese?",
        answerA = "Sie produziert Antikoerper fuer das Immunsystem",
        answerB = "Sie produziert Hormone, die Stoffwechsel, Wachstum und Energieverbrauch regulieren",
        answerC = "Sie filtert Schadstoffe aus dem Blut",
        answerD = "Sie produziert Verdauungsenzyme fuer den Duenndarm",
        correctAnswer = 1,
        explanation = "Die Schilddruese produziert die Hormone Thyroxin (T4) und Trijodthyronin (T3), die den Grundumsatz, die Herzfrequenz, die Koerpertemperatur und das Wachstum regulieren.",
        difficulty = 2,
        funFact = "Jod ist fuer die Schilddruesenhormonproduktion unverzichtbar. In vielen Laendern wird Speisesalz daher mit Jod angereichert, um Jodmangel und Schilddruesenerkrankungen zu verhindern."
    ),

    // Question 25
    Question(
        categoryId = 16,
        questionText = "Was passiert bei der Blutgerinnung?",
        answerA = "Rote Blutkoerperchen loesen sich auf und verschliessen die Wunde",
        answerB = "Thrombozyten und Gerinnungsfaktoren bilden einen Pfropfen, der die Wunde verschliesst",
        answerC = "Weisse Blutkoerperchen bilden eine Schutzschicht ueber der Wunde",
        answerD = "Adrenalin zieht die Wundraender zusammen",
        correctAnswer = 1,
        explanation = "Bei einer Verletzung klumpen Thrombozyten an der Wunde zusammen. Zusammen mit einer Kaskade von Gerinnungsfaktoren entsteht ein Fibringeflecht, das die Wunde verschliesst und Blutverlust stoppt.",
        difficulty = 2,
        funFact = "An der Blutgerinnungskaskade sind mindestens 13 verschiedene Gerinnungsfaktoren beteiligt. Fehlt einer davon -- wie bei der Blutkrankheit Haemophilie -- gerinnt das Blut nicht richtig."
    ),

    // Question 26
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen angeborenem und erworbenem Immunsystem?",
        answerA = "Das angeborene Immunsystem lernt mit der Zeit, das erworbene ist von Geburt an vorhanden",
        answerB = "Das angeborene reagiert schnell und unspezifisch, das erworbene reagiert spezifisch auf bestimmte Erreger",
        answerC = "Beide Systeme sind identisch und bekaempfen alle Erreger auf dieselbe Weise",
        answerD = "Das angeborene Immunsystem wird nur durch Impfungen aktiviert",
        correctAnswer = 1,
        explanation = "Das angeborene Immunsystem reagiert sofort und unspezifisch auf alle Eindringlinge. Das erworbene (adaptive) Immunsystem lernt, spezifische Erreger zu erkennen und bildet dabei Gedaechtniszellen.",
        difficulty = 2,
        funFact = "Das angeborene Immunsystem ist evolutionaer sehr alt -- selbst einfache Tiere wie Insekten haben es. Das adaptive Immunsystem mit Antikoerperbildung entstand erst bei Wirbeltieren."
    ),

    // Question 27
    Question(
        categoryId = 16,
        questionText = "Welche Organe gehoeren zum weiblichen Fortpflanzungssystem?",
        answerA = "Eierstoecke, Eileiter, Gebaermutter und Scheide",
        answerB = "Nieren, Harnblase und Harnroehre",
        answerC = "Lunge, Bronchien und Luftroehre",
        answerD = "Leber, Gallenblase und Bauchspeicheldruse",
        correctAnswer = 0,
        explanation = "Das weibliche Fortpflanzungssystem umfasst die Eierstoecke (Ovarien), Eileiter (Tuben), die Gebaermutter (Uterus) und die Scheide (Vagina). In den Ovarien reifen die Eizellen heran.",
        difficulty = 2,
        funFact = "Eine Frau wird bereits mit allen Eizellen geboren, die sie je haben wird -- etwa ein bis zwei Millionen. Bis zur Pubertat sind davon nur noch etwa 300.000 bis 400.000 vorhanden."
    ),

    // Question 28
    Question(
        categoryId = 16,
        questionText = "Welche Hauptfunktion hat das Testosteron beim Mann?",
        answerA = "Es reguliert den Blutzuckerspiegel",
        answerB = "Es foerdert die Ausbildung maennlicher Geschlechtsmerkmale, Muskelaufbau und Spermienproduktion",
        answerC = "Es steuert den Schlaf-Wach-Rhythmus",
        answerD = "Es reguliert die Schilddruesenfunktion",
        correctAnswer = 1,
        explanation = "Testosteron ist das wichtigste maennliche Sexualhormon. Es wird hauptsaechlich in den Hoden produziert und steuert maennliche Geschlechtsmerkmale, Muskelaufbau, Knochendichte und Spermienproduktion.",
        difficulty = 2,
        funFact = "Testosteron wird nicht nur von Maennern produziert -- auch Frauen haben Testosteron, allerdings in viel geringerer Menge. Es beeinflusst auch bei Frauen Libido und Muskelkraft."
    ),

    // Question 29
    Question(
        categoryId = 16,
        questionText = "Was ist Peristaltik?",
        answerA = "Die rhythmischen Wellenbewegungen der Muskulatur im Verdauungstrakt, die Nahrung vorwaertstransportieren",
        answerB = "Der Herzschlagrhythmus beim Sport",
        answerC = "Die Bewegung der Flimmerhaarchen in der Luftroehre",
        answerD = "Der Blutfluss in den Kapillaren",
        correctAnswer = 0,
        explanation = "Peristaltik bezeichnet die wellenfoermigen Kontraktionen der glatten Muskulatur in Speiseroehre, Magen und Darm. Sie transportieren den Nahrungsbrei automatisch von oben nach unten.",
        difficulty = 2,
        funFact = "Peristaltik funktioniert auch gegen die Schwerkraft -- Astronauten koennen im Weltraum normal essen und verdauen, weil der Nahrungstransport nicht von der Schwerkraft abhaengt."
    ),

    // Question 30
    Question(
        categoryId = 16,
        questionText = "Welche Aufgabe hat die Gallenblase?",
        answerA = "Sie produziert selbst Galle und leitet sie in den Magen",
        answerB = "Sie speichert die von der Leber produzierte Galle und gibt sie bei Bedarf in den Duenndarm ab",
        answerC = "Sie filtert Abfallstoffe aus dem Blut wie eine kleine Niere",
        answerD = "Sie produziert Insulin fuer die Blutzuckerregulierung",
        correctAnswer = 1,
        explanation = "Die Gallenblase ist ein kleines Sackchen unter der Leber. Sie speichert die in der Leber produzierte Galle und gibt sie konzentriert ab, wenn Fette im Duenndarm verdaut werden muessen.",
        difficulty = 2,
        funFact = "Gallensteine entstehen, wenn Cholesterin oder andere Stoffe in der Gallenblase auskristallisieren. Sie koennen voellig symptomlos sein oder starke Schmerzen (Gallenkolik) verursachen."
    ),

    // Question 31
    Question(
        categoryId = 16,
        questionText = "Was ist die Hypophyse (Hirnanhangdruese)?",
        answerA = "Ein Teil des Hirnstamms, der die Atmung steuert",
        answerB = "Eine uebergeordnete Hormondruse im Gehirn, die viele andere Druesen steuert",
        answerC = "Der Teil des Gehirns, der fuer das Gedaechtnis zustaendig ist",
        answerD = "Eine Druese in der Schilddruese, die Jod speichert",
        correctAnswer = 1,
        explanation = "Die Hypophyse ist eine erbsengrosse Druese an der Unterseite des Gehirns. Sie produziert und steuert viele Hormone, die andere Druesen wie Schilddruese, Nebennieren und Gonaden kontrollieren.",
        difficulty = 2,
        funFact = "Obwohl die Hypophyse nur etwa 0,5 Gramm wiegt, wird sie als 'Master Gland' bezeichnet, weil sie wie ein Dirigent die meisten anderen Hormondresen des Koerpers koordiniert."
    ),

    // Question 32
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Gasaustausch in den Kapillaren (innere Atmung)?",
        answerA = "Kohlendioxid gelangt von der Lunge ins Blut und wird zu den Organen transportiert",
        answerB = "Sauerstoff wird vom Blut an die Koerperzellen abgegeben und Kohlendioxid wird aufgenommen",
        answerC = "Sauerstoff und Kohlendioxid werden in der Leber ausgetauscht",
        answerD = "Das Herz tauscht Gase zwischen beiden Blutkreislaeufen aus",
        correctAnswer = 1,
        explanation = "In den Kapillaren der Organe gibt das Blut Sauerstoff an die Koerperzellen ab und nimmt das bei der Zellatmung entstandene Kohlendioxid auf. Dieses wird dann zur Lunge transportiert und ausgeatmet.",
        difficulty = 2,
        funFact = "Die Gesamtlaenge aller Kapillaren im menschlichen Koerper betraegt etwa 100.000 Kilometer -- das waere mehr als zweimal der Weg zum Mond und zurueck."
    ),

    // Question 33
    Question(
        categoryId = 16,
        questionText = "Welche Zellen bilden das Herzmuskelgewebe (Myokard)?",
        answerA = "Skelettmuskelzellen, die willkuerlich gesteuert werden koennen",
        answerB = "Kardiomyozyten -- spezialisierte Herzmuskelzellen, die sich rhythmisch und unwillkuerlich zusammenziehen",
        answerC = "Glatte Muskelzellen wie in den Blutgefaessen",
        answerD = "Nervenzellen, die elektrische Impulse erzeugen",
        correctAnswer = 1,
        explanation = "Kardiomyozyten sind die Muskelzellen des Herzens. Sie sind miteinander elektrisch verbunden, ziehen sich rhythmisch zusammen und koennen sich -- anders als Skelettmuskeln -- nicht willkuerlich steuern lassen.",
        difficulty = 2,
        funFact = "Herzmuskelzellen teilen sich kaum. Das bedeutet, dass das Herz nach einem Herzinfarkt beschaedigtes Gewebe kaum ersetzen kann -- weshalb Herzinfarkte dauerhafte Schaeden hinterlassen."
    ),

    // Question 34
    Question(
        categoryId = 16,
        questionText = "Was ist Oestrogen und wo wird es hauptsaechlich produziert?",
        answerA = "Ein Stresshormon der Nebenniere",
        answerB = "Ein weibliches Sexualhormon, das hauptsaechlich in den Eierstoecken produziert wird",
        answerC = "Ein Verdauungshormon des Duenndarmsm",
        answerD = "Ein Schlafhormon der Zirbeldruse",
        correctAnswer = 1,
        explanation = "Oestrogen ist das wichtigste weibliche Sexualhormon. Es wird hauptsaechlich in den Eierstoecken gebildet und steuert die weibliche Geschlechtsentwicklung, den Menstruationszyklus und die Schwangerschaft.",
        difficulty = 2,
        funFact = "Oestrogen sschuetzt Frauen vor dem Klimakterium (Wechseljahren) vor Herzerkrankungen. Nach den Wechseljahren steigt das Herzerkrankungsrisiko bei Frauen deutlich an."
    ),

    // Question 35
    Question(
        categoryId = 16,
        questionText = "Welche Aufgabe uebernehmen Makrophagen im Immunsystem?",
        answerA = "Sie produzieren Antikoerper gegen Krankheitserreger",
        answerB = "Sie verschlingen und verdauen Krankheitserreger, Zelltraemmer und tote Zellen",
        answerC = "Sie transportieren Sauerstoff zu den Koerperzellen",
        answerD = "Sie regulieren die Koerpertemperatur bei Fieber",
        correctAnswer = 1,
        explanation = "Makrophagen sind grosse Fresszellen des Immunsystems. Sie nehmen Krankheitserreger, tote Zellen und Fremdpartikel durch Phagozytose auf und verdauen sie. Ausserdem praesentieren sie Antigene fuer das erworbene Immunsystem.",
        difficulty = 2,
        funFact = "Der Name 'Makrophage' kommt aus dem Griechischen und bedeutet 'Grosser Fresser'. Ein einzelner Makrophage kann innerhalb seiner Lebenszeit Dutzende von Bakterien aufnehmen und vernichten."
    ),

    // Question 36
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem Renin-Angiotensin-Aldosteron-System?",
        answerA = "Ein Hormonsystem der Nieren, das den Blutdruck und den Natriumhaushalt reguliert",
        answerB = "Ein Hormonsystem der Schilddruese fuer den Energiestoffwechsel",
        answerC = "Ein Nervensystem, das die Herzfrequenz steuert",
        answerD = "Ein Enzymsystem der Leber fuer den Abbau von Alkohol",
        correctAnswer = 0,
        explanation = "Wenn der Blutdruck oder der Natriumgehalt sinkt, schuettet die Niere Renin aus. Diese Kaskade fuehrt schliesslich zur Ausschuettung von Aldosteron, das Natrium und Wasser zurueckhalt und so den Blutdruck erhoehlt.",
        difficulty = 2,
        funFact = "Viele Blutdruckmittel (ACE-Hemmer, AT1-Antagonisten) greifen genau in dieses System ein. Sie werden weltweit zu den am haeufigsten verschriebenen Medikamenten ueberhaupt gezaehlt."
    ),

    // Question 37
    Question(
        categoryId = 16,
        questionText = "Was ist die Blut-Hirn-Schranke?",
        answerA = "Eine knoeCherne Trennwand zwischen Grosshirn und Kleinhirn",
        answerB = "Eine selektive Barriere aus speziellen Kapillarwandezellen, die das Gehirn vor Schadstoffen schuetzt",
        answerC = "Eine Schleimhaut im Nacken, die das Rueckenmark schuetzt",
        answerD = "Ein Ventilsystem im Herzen, das den Blutfluss zum Gehirn reguliert",
        correctAnswer = 1,
        explanation = "Die Blut-Hirn-Schranke besteht aus eng miteinander verbundenen Endothelzellen der Hirnkapillaren. Sie laesst nur bestimmte Stoffe ins Gehirn -- Sauerstoff, Glukose und fettloesliche Substanzen -- und haelt Giftstoffe und die meisten Medikamente aus.",
        difficulty = 2,
        funFact = "Die Blut-Hirn-Schranke macht die Entwicklung von Gehirnmedikamenten besonders schwierig -- viele wirksame Substanzen koennen das Gehirn schlicht nicht erreichen."
    ),

    // Question 38
    Question(
        categoryId = 16,
        questionText = "Welche Hauptfunktion hat die Nebenniere?",
        answerA = "Sie filtert Natriumchlorid aus dem Urin",
        answerB = "Sie produziert Stresshormone wie Adrenalin und Kortisol",
        answerC = "Sie speichert Blut als Reserve fuer Notfaelle",
        answerD = "Sie produziert Bile zur Fettverdauung",
        correctAnswer = 1,
        explanation = "Die Nebennieren sitzen auf den Nieren und bestehen aus Rinde und Mark. Das Mark produziert Adrenalin und Noradrenalin, die Rinde produziert Kortisol, Aldosteron und kleine Mengen Sexualhormone.",
        difficulty = 2,
        funFact = "Die Nebenniere reagiert auf Stress innerhalb von Sekunden -- Adrenalin wird fast sofort ins Blut ausgeschuettet. Kortisol folgt etwas langsamer und haelt laenger an."
    ),

    // Question 39
    Question(
        categoryId = 16,
        questionText = "Was ist die Hauptfunktion der roten Blutkoerperchen (Erythrozyten)?",
        answerA = "Krankheitserreger bekaempfen und Antikoerper produzieren",
        answerB = "Sauerstoff von der Lunge zu den Koerperzellen transportieren",
        answerC = "Blutgerinnung bei Verletzungen einleiten",
        answerD = "Naehrstoffe aus dem Darm ins Blut aufnehmen",
        correctAnswer = 1,
        explanation = "Erythrozyten enthalten Haemoglobin, das Sauerstoff in der Lunge bindet und ihn zu allen Koerperzellen transportiert. Sie geben dort den Sauerstoff ab und nehmen Kohlendioxid auf.",
        difficulty = 2,
        funFact = "Erythrozyten haben keinen Zellkern -- dadurch haben sie mehr Platz fuer Haemoglobin. Sie leben nur etwa 120 Tage und werden dann in der Milz und Leber abgebaut."
    ),

    // Question 40
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Phagozytose?",
        answerA = "Die Aufnahme von Naehrstoffen durch die Darmschleimhaut",
        answerB = "Das Verschlingen und Verdauen von Partikeln durch Immunzellen",
        answerC = "Die Zellteilung bei der Wundheilung",
        answerD = "Die Ausschuettung von Antikoerpern ins Blut",
        correctAnswer = 1,
        explanation = "Phagozytose ist der Prozess, bei dem Immunzellen wie Makrophagen und Neutrophile Krankheitserreger, Zelltraemmer oder Fremdpartikel umhullen und in ihr Inneres aufnehmen, um sie dort zu verdauen.",
        difficulty = 2,
        funFact = "Eli Metchnikoff entdeckte die Phagozytose 1882 und erhielt dafuer 1908 den Nobelpreis fuer Physiologie oder Medizin. Er beobachtete, wie Zellen von Seesternen Fremdkoerper umschlossen."
    ),

    // Question 41
    Question(
        categoryId = 16,
        questionText = "Was ist der Sinusknoten im Herz?",
        answerA = "Ein Ventil zwischen rechter und linker Herzkammer",
        answerB = "Der natuerliche Schrittmacher des Herzens, der den elektrischen Herzrhythmus erzeugt",
        answerC = "Ein Blutgefaess, das die Herzmuskulatur versorgt",
        answerD = "Eine Schleimhaut, die das Herz von innen auskleidet",
        correctAnswer = 1,
        explanation = "Der Sinusknoten im rechten Vorhof erzeugt den elektrischen Impuls, der sich ueber das Herzleitungssystem ausbreitet und eine koordinierte Herzkontraktion ausloest. Er gilt als natuerlicher Herzschrittmacher.",
        difficulty = 2,
        funFact = "Ein kuenstlicher Herzschrittmacher ersetzt oder unterstuetzt den Sinusknoten, wenn dieser versagt. Seit 1958 wurden weltweit Millionen solcher Geraete implantiert."
    ),

    // Question 42
    Question(
        categoryId = 16,
        questionText = "Welche Druesenart gibt ihre Sekrete ueber Ausfuehrungsgaenge nach aussen ab?",
        answerA = "Endokrine Druesen",
        answerB = "Exokrine Druesen",
        answerC = "Lymphatische Druesen",
        answerD = "Endovaskulaere Druesen",
        correctAnswer = 1,
        explanation = "Exokrine Druesen wie Speicheldruesen, Schweissdruesen und Bauchspeicheldruse geben ihre Sekrete ueber Ausfuehrungsgaenge nach aussen oder in Koerperhoehlen ab -- im Gegensatz zu endokrinen Druesen, die direkt ins Blut sezernieren.",
        difficulty = 2,
        funFact = "Die Bauchspeicheldruse ist sowohl exokrin (gibt Verdauungsenzyme in den Darm ab) als auch endokrin (produziert Insulin und Glukagon fuer das Blut) -- sie hat also eine Doppelfunktion."
    ),

    // Question 43
    Question(
        categoryId = 16,
        questionText = "Was ist das Diaphragma (Zwerchfell) und welche Rolle spielt es beim Atmen?",
        answerA = "Eine Schleimhaut in der Luftroehre, die Staubpartikel abfaengt",
        answerB = "Ein gewoelbter Atemmuskel zwischen Brust- und Bauchhoehl, der durch Kontraktion die Lunge ausdehnt",
        answerC = "Ein Knochen, der die Lunge vor Verletzungen schuetzt",
        answerD = "Ein Ventil zwischen Lunge und Herz",
        correctAnswer = 1,
        explanation = "Das Zwerchfell ist der wichtigste Atemmuskel. Beim Einatmen zieht es sich zusammen und flacht ab, dadurch vergrossert sich der Brustraum und Luft stroemt in die Lunge. Beim Ausatmen entspannt es sich wieder.",
        difficulty = 2,
        funFact = "Schluckauf entsteht, wenn das Zwerchfell unwillkuerlich krampfhaft zusammenzuckt. Das abrupte Einatmen durch die Stimmritze erzeugt dann das charakteristische 'Hik'-Gerausch."
    ),

    // Question 44
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter dem enterischen Nervensystem?",
        answerA = "Das Nervensystem der Augenmuskeln",
        answerB = "Das eigenstaendige Nervensystem des Verdauungstrakts, das oft als zweites Gehirn bezeichnet wird",
        answerC = "Das Nervensystem der Haut, das Schmerz und Beruehrung wahrnimmt",
        answerD = "Das Nervensystem des Kleinhirns fuer die Gleichgewichtssteuerung",
        correctAnswer = 1,
        explanation = "Das enterische Nervensystem im Verdauungstrakt hat etwa 100 bis 500 Millionen Nervenzellen und steuert Peristaltik, Sekretion und Durchblutung weitgehend eigenstaendig -- daher sein Beiname 'Bauchhirn'.",
        difficulty = 2,
        funFact = "Das Darmnervensystem kommuniziert ueber den Vagusnerv eng mit dem Gehirn. Forscher glauben heute, dass Darmbakterien ueber diesen Weg sogar Stimmung und Verhalten beeinflussen koennen."
    ),

    // Question 45
    Question(
        categoryId = 16,
        questionText = "Was sind natuerliche Killerzellen (NK-Zellen)?",
        answerA = "T-Zellen, die Antikoerper produzieren",
        answerB = "Lymphozyten des angeborenen Immunsystems, die ohne vorherige Sensibilisierung virusinfizierte und Krebszellen toeten",
        answerC = "Makrophagen, die im Blut patrouillieren",
        answerD = "Zellen der Milz, die alte Erythrozyten abbauen",
        correctAnswer = 1,
        explanation = "NK-Zellen sind Teil des angeborenen Immunsystems und koennen infizierte oder entartete Zellen erkennen und abtoten, ohne vorher auf sie 'trainiert' worden zu sein. Sie wirken als erste Verteidigungslinie.",
        difficulty = 2,
        funFact = "NK-Zellen erkennen Krebszellen unter anderem daran, dass diese bestimmte Oberflaechenproteine verloren haben. Dieses Fehlen von 'Erkennungszeichen' ist fuer NK-Zellen ein Signal zum Angriff."
    ),

    // Question 46
    Question(
        categoryId = 16,
        questionText = "Was regelt das Hormon Glukagon?",
        answerA = "Es senkt den Blutzucker, indem es Zucker in die Zellen schleust",
        answerB = "Es erhoehlt den Blutzucker, indem es Glykogen in der Leber zu Glukose abbaut",
        answerC = "Es steuert den Mineralhaushalt in den Nieren",
        answerD = "Es foerdert den Aufbau von Fettgewebe",
        correctAnswer = 1,
        explanation = "Glukagon ist das Gegenspieler-Hormon zu Insulin. Wenn der Blutzucker zu niedrig sinkt, schuettet die Bauchspeicheldruse Glukagon aus, das die Leber anweist, gespeichertes Glykogen in Glukose umzuwandeln.",
        difficulty = 2,
        funFact = "Insulin und Glukagon werden beide in den Langerhans-Inseln der Bauchspeicheldruse produziert -- allerdings in verschiedenen Zelltypen: Insulin in Betazellen, Glukagon in Alphazellen."
    ),

    // Question 47
    Question(
        categoryId = 16,
        questionText = "Welche Aufgabe hat das Endothel in den Blutgefaessen?",
        answerA = "Es transportiert Naehrstoffe durch die Gefaesswand",
        answerB = "Es bildet die innere Auskleidung der Gefaesse, reguliert den Tonus und steuert den Gasaustausch",
        answerC = "Es produziert rote Blutkoerperchen fuer das Blut",
        answerD = "Es schuetzt die Gefaesse mechanisch vor Verletzungen von aussen",
        correctAnswer = 1,
        explanation = "Das Endothel ist eine einzelne Zellschicht, die alle Blutgefaesse von innen auskleidet. Es reguliert den Gefaesstonus durch Botenstoffe wie Stickstoffmonoxid und steuert den Stoffaustausch zwischen Blut und Gewebe.",
        difficulty = 2,
        funFact = "Alle Endothelzellen des menschlichen Koerpers zusammengenommen wuerden eine Flaeche von etwa 700 Quadratmetern bedecken -- so gross wie ein halbes Fussballfeld."
    ),

    // Question 48
    Question(
        categoryId = 16,
        questionText = "Was ist die Funktion der Bronchien?",
        answerA = "Sie sind zustaendig fuer den Gasaustausch zwischen Luft und Blut",
        answerB = "Sie leiten die Luft von der Luftroehre in die Lunge und verteilen sie auf die Lungenblaeschen",
        answerC = "Sie filtern Staub und Schmutzpartikel aus der Atemluft im Nasenbereich",
        answerD = "Sie steuern die Atemfrequenz durch Dehnungsrezeptoren",
        correctAnswer = 1,
        explanation = "Die Bronchien sind die Verlaengerung der Luftroehre in der Lunge. Sie verzweigen sich baumartig in immer feinere Bronchiolen, die schliesslich in die Alveolen muenden, wo der Gasaustausch stattfindet.",
        difficulty = 2,
        funFact = "Bei Asthma verengen sich die Bronchien durch Muskelkrampf und Schleimhautschwellung, was das Atmen erschwert. Das typische Pfeifen beim Ausatmen entsteht durch die verengte Luftpassage."
    ),

    // Question 49
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Haematopoese und Haemostase?",
        answerA = "Haematopoese bedeutet Blutgerinnung, Haemostase bedeutet Blutbildung",
        answerB = "Haematopoese ist die Blutbildung im Knochenmark, Haemostase ist die Blutstillung bei Verletzungen",
        answerC = "Beide Begriffe bezeichnen denselben Vorgang der Bluterneuerung",
        answerD = "Haematopoese ist der Bluttransport, Haemostase ist der Sauerstofftransport",
        correctAnswer = 1,
        explanation = "Haematopoese ist die Bildung aller Blutzellanteile (rote, weisse Blutkoerperchen und Thrombozyten) im roten Knochenmark. Haemostase bezeichnet den Prozess der Blutstillung nach einer Gefaessverletzung.",
        difficulty = 2,
        funFact = "Taglich werden im Knochenmark eines Erwachsenen etwa 200 Milliarden neue rote Blutkoerperchen, 10 Milliarden weisse Blutkoerperchen und 400 Milliarden Thrombozyten gebildet."
    ),

    // Question 50
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Homoeostase?",
        answerA = "Das Gleichgewicht zwischen Sympathikus und Parasympathikus im Nervensystem",
        answerB = "Die Faehigkeit des Koerpers, innere Bedingungen wie Temperatur, pH-Wert und Blutzucker konstant zu halten",
        answerC = "Die Regulierung des Schlaf-Wach-Rhythmus durch Melatonin",
        answerD = "Den Ausgleich von Sauerstoff und Kohlendioxid in der Lunge",
        correctAnswer = 1,
        explanation = "Homoeostase bezeichnet die Selbstregulierung des Koerpers, um innere Parameter wie Koerpertemperatur (37 Grad), Blut-pH (7,4) und Blutzucker konstant zu halten, trotz wechselnder aeusserer Bedingungen.",
        difficulty = 2,
        funFact = "Der Begriff Homoeostase wurde 1932 vom amerikanischen Physiologen Walter Cannon gepraegt. Er leitete ihn vom griechischen 'homoios' (gleich) und 'stasis' (Stillstand) ab."
    ),

)
