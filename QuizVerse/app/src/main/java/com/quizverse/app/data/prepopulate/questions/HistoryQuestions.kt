package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestions(): List<Question> = listOf(

    // --- EASY (10 questions) ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr begann der Erste Weltkrieg?",
        answerA = "1912",
        answerB = "1914",
        answerC = "1916",
        answerD = "1918",
        correctAnswer = 1,
        explanation = "Der Erste Weltkrieg begann am 28. Juli 1914 mit der Kriegserklärung Österreich-Ungarns an Serbien.",
        difficulty = 1,
        funFact = "Der Auslöser war das Attentat auf Erzherzog Franz Ferdinand in Sarajevo am 28. Juni 1914."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Kaiser des Deutschen Reiches (1871)?",
        answerA = "Otto von Bismarck",
        answerB = "Friedrich III.",
        answerC = "Wilhelm I.",
        answerD = "Wilhelm II.",
        correctAnswer = 2,
        explanation = "Wilhelm I. wurde am 18. Januar 1871 im Spiegelsaal von Versailles zum ersten deutschen Kaiser ausgerufen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt war die Hauptstadt des Dritten Reiches?",
        answerA = "München",
        answerB = "Hamburg",
        answerC = "Nürnberg",
        answerD = "Berlin",
        correctAnswer = 3,
        explanation = "Berlin war die Hauptstadt des Deutschen Reiches und während der NS-Zeit das politische Zentrum des Dritten Reiches.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Wann fiel die Berliner Mauer?",
        answerA = "3. Oktober 1990",
        answerB = "9. November 1989",
        answerC = "1. September 1989",
        answerD = "12. August 1961",
        correctAnswer = 1,
        explanation = "Am 9. November 1989 öffnete die DDR ihre Grenzen, was zum Fall der Berliner Mauer führte.",
        difficulty = 1,
        funFact = "Die Mauer war seit dem 13. August 1961 in Betrieb – insgesamt 28 Jahre lang."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Christoph Kolumbus?",
        answerA = "Ein portugiesischer König",
        answerB = "Ein spanischer Feldherr",
        answerC = "Ein genuesischer Seefahrer im Dienst Spaniens",
        answerD = "Ein englischer Entdecker",
        correctAnswer = 2,
        explanation = "Christoph Kolumbus war ein genuesischer Seefahrer, der 1492 im Auftrag der spanischen Krone Amerika entdeckte.",
        difficulty = 1,
        funFact = "Kolumbus glaubte bis zu seinem Tod, er habe Asien erreicht, nicht einen neuen Kontinent."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Reich errichtete Julius Cäsar?",
        answerA = "Das Byzantinische Reich",
        answerB = "Das Römische Reich",
        answerC = "Das Griechische Reich",
        answerD = "Das Osmanische Reich",
        correctAnswer = 1,
        explanation = "Julius Cäsar war eine Schlüsselfigur bei der Umwandlung der Römischen Republik in das Römische Kaiserreich.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr endete der Zweite Weltkrieg in Europa?",
        answerA = "1943",
        answerB = "1944",
        answerC = "1945",
        answerD = "1946",
        correctAnswer = 2,
        explanation = "Der Zweite Weltkrieg in Europa endete am 8. Mai 1945 mit der bedingungslosen Kapitulation Deutschland.",
        difficulty = 1,
        funFact = "In Russland wird der 9. Mai als Tag des Sieges gefeiert, da die Kapitulationsurkunde nach Mitternacht Moskauer Zeit unterzeichnet wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Napoleon Bonaparte?",
        answerA = "Ein englischer Admiral",
        answerB = "Ein österreichischer Kaiser",
        answerC = "Ein französischer Kaiser und Feldherr",
        answerD = "Ein russischer Zar",
        correctAnswer = 2,
        explanation = "Napoleon Bonaparte war ein französischer General, der sich 1804 zum Kaiser krönte und weite Teile Europas eroberte.",
        difficulty = 1,
        funFact = "Napoleon war nicht besonders klein – er maß etwa 1,69 m, was für seine Zeit durchschnittlich war."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Magna Carta?",
        answerA = "Ein Vertrag zwischen England und Frankreich",
        answerB = "Ein englisches Rechtsdokument von 1215",
        answerC = "Eine päpstliche Verkündigung",
        answerD = "Die erste englische Verfassung",
        correctAnswer = 1,
        explanation = "Die Magna Carta war ein 1215 von König Johann ohne Land unterzeichnetes Dokument, das die königliche Macht einschränkte.",
        difficulty = 1,
        funFact = "Nur vier Originalkopien der Magna Carta sind erhalten geblieben."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Zivilisation baute die Pyramiden von Gizeh?",
        answerA = "Die Griechen",
        answerB = "Die Sumerer",
        answerC = "Die alten Ägypter",
        answerD = "Die Römer",
        correctAnswer = 2,
        explanation = "Die Pyramiden von Gizeh wurden von den alten Ägyptern erbaut, die größte um 2560 v. Chr. für Pharao Cheops.",
        difficulty = 1,
        funFact = "Die Große Pyramide von Gizeh war etwa 3800 Jahre lang das höchste Bauwerk der Welt."
    ),

    // --- MEDIUM (15 questions) ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde das Heilige Römische Reich Deutscher Nation aufgelöst?",
        answerA = "1789",
        answerB = "1800",
        answerC = "1806",
        answerD = "1815",
        correctAnswer = 2,
        explanation = "Das Heilige Römische Reich Deutscher Nation wurde am 6. August 1806 aufgelöst, als Kaiser Franz II. abdankte.",
        difficulty = 2,
        funFact = "Voltaire bemerkte spöttisch, es sei weder heilig, noch römisch, noch ein Reich."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Vertrag beendete den Dreißigjährigen Krieg?",
        answerA = "Vertrag von Utrecht",
        answerB = "Westfälischer Friede",
        answerC = "Vertrag von Paris",
        answerD = "Friede von Augsburg",
        correctAnswer = 1,
        explanation = "Der Westfälische Friede von 1648 beendete den Dreißigjährigen Krieg und legte Grundlagen des modernen Staatensystems.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Revolution brach 1789 in Frankreich aus?",
        answerA = "Die Bourgeoisie-Revolution",
        answerB = "Die Industrielle Revolution",
        answerC = "Die Französische Revolution",
        answerD = "Die Glorreiche Revolution",
        correctAnswer = 2,
        explanation = "Die Französische Revolution begann 1789 und beendete die absolute Monarchie. Sie führte zur Republik und zu Napoleons Aufstieg.",
        difficulty = 2,
        funFact = "Das Motto der Revolution – Liberté, Égalité, Fraternité – ist bis heute das Nationalwahrzeichen Frankreichs."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann wurde die DDR (Deutsche Demokratische Republik) gegründet?",
        answerA = "1945",
        answerB = "1947",
        answerC = "1949",
        answerD = "1952",
        correctAnswer = 2,
        explanation = "Die DDR wurde am 7. Oktober 1949 in der sowjetischen Besatzungszone Deutschlands gegründet.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher englische König wurde 1649 hingerichtet?",
        answerA = "Heinrich VIII.",
        answerB = "Karl I.",
        answerC = "Jakob I.",
        answerD = "Richard II.",
        correctAnswer = 1,
        explanation = "Karl I. wurde am 30. Januar 1649 nach dem englischen Bürgerkrieg auf Beschluss des Parlaments hingerichtet.",
        difficulty = 2,
        funFact = "Es war das erste Mal in der Geschichte, dass ein englischer König öffentlich hingerichtet wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis löste den Ersten Weltkrieg unmittelbar aus?",
        answerA = "Die Invasion Belgiens durch Deutschland",
        answerB = "Das Attentat auf Erzherzog Franz Ferdinand",
        answerC = "Die Mobilmachung Russlands",
        answerD = "Der Angriff Österreichs auf Serbien",
        correctAnswer = 1,
        explanation = "Das Attentat auf Erzherzog Franz Ferdinand in Sarajevo am 28. Juni 1914 war der unmittelbare Auslöser des Ersten Weltkriegs.",
        difficulty = 2,
        funFact = "Der Attentäter Gavrilo Princip war 19 Jahre alt und Mitglied der serbischen Nationalistenbewegung Schwarze Hand."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Schlachtformation machte Alexander der Große berühmt?",
        answerA = "Die Testudo-Formation",
        answerB = "Die Phalanx",
        answerC = "Die Cohors",
        answerD = "Die Schildwall-Formation",
        correctAnswer = 1,
        explanation = "Die makedonische Phalanx, mit langen Speeren (Sarissa) bewaffnet, war Alexanders wichtigste Kampfformation.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Marshall-Plan?",
        answerA = "Ein Militärbündnis nach dem Zweiten Weltkrieg",
        answerB = "Ein US-amerikanisches Wirtschaftshilfeprogramm für Europa",
        answerC = "Ein Friedensvertrag zwischen den USA und der UdSSR",
        answerD = "Ein NATO-Verteidigungsplan",
        correctAnswer = 1,
        explanation = "Der Marshall-Plan (1948–1952) war ein US-Programm zur wirtschaftlichen Unterstützung Europas nach dem Zweiten Weltkrieg.",
        difficulty = 2,
        funFact = "Deutschland erhielt etwa 1,4 Milliarden Dollar aus dem Marshall-Plan – damals eine enorme Summe."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Land war das erste, das Frauen das Wahlrecht einräumte?",
        answerA = "Australien",
        answerB = "Schweden",
        answerC = "Neuseeland",
        answerD = "Finnland",
        correctAnswer = 2,
        explanation = "Neuseeland war 1893 das erste Land der Welt, das Frauen das Wahlrecht gewährte.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt war das Zentrum der Hanse?",
        answerA = "Hamburg",
        answerB = "Bremen",
        answerC = "Lübeck",
        answerD = "Rostock",
        correctAnswer = 2,
        explanation = "Lübeck war als 'Königin der Hanse' das führende Mitglied und Versammlungsort des Hansebundes.",
        difficulty = 2,
        funFact = "Auf dem Höhepunkt ihrer Macht umfasste die Hanse über 200 Städte von London bis Nowgorod."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Vertrag beendete offiziell den Ersten Weltkrieg?",
        answerA = "Vertrag von Brest-Litowsk",
        answerB = "Vertrag von Versailles",
        answerC = "Vertrag von Saint-Germain",
        answerD = "Londoner Vertrag",
        correctAnswer = 1,
        explanation = "Der Vertrag von Versailles, unterzeichnet am 28. Juni 1919, beendete offiziell den Ersten Weltkrieg.",
        difficulty = 2,
        funFact = "Deutschland musste im Versailler Vertrag die alleinige Kriegsschuld (Artikel 231) akzeptieren."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann fand die Oktoberrevolution in Russland statt?",
        answerA = "1905",
        answerB = "1915",
        answerC = "1917",
        answerD = "1920",
        correctAnswer = 2,
        explanation = "Die Oktoberrevolution fand im Oktober/November 1917 statt und brachte die Bolschewiki unter Lenin an die Macht.",
        difficulty = 2,
        funFact = "Nach dem julianischen Kalender fand sie im Oktober statt, nach dem gregorianischen Kalender war es November – daher 'Oktoberrevolution'."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis markierte den Beginn des Zweiten Weltkriegs?",
        answerA = "Der japanische Angriff auf Pearl Harbor",
        answerB = "Die Besetzung Österreichs durch Deutschland",
        answerC = "Der deutsche Überfall auf Polen",
        answerD = "Die Kriegserklärung Frankreichs an Deutschland",
        correctAnswer = 2,
        explanation = "Am 1. September 1939 überfiel Deutschland Polen, was den Beginn des Zweiten Weltkriegs markierte.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Wer führte die Mongolen zu ihrer größten Machtentfaltung?",
        answerA = "Kublai Khan",
        answerB = "Dschingis Khan",
        answerC = "Timur Lenk",
        answerD = "Ögedei Khan",
        correctAnswer = 1,
        explanation = "Dschingis Khan gründete das Mongolische Reich und wurde zu einem der mächtigsten Herrscher der Geschichte.",
        difficulty = 2,
        funFact = "Das Mongolische Reich war flächenmäßig das größte zusammenhängende Landimperium aller Zeiten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher deutsche Kanzler trieb die Wiedervereinigung Deutschlands 1990 voran?",
        answerA = "Willy Brandt",
        answerB = "Helmut Schmidt",
        answerC = "Gerhard Schröder",
        answerD = "Helmut Kohl",
        correctAnswer = 3,
        explanation = "Helmut Kohl trieb als Bundeskanzler die deutsche Wiedervereinigung voran, die am 3. Oktober 1990 vollzogen wurde.",
        difficulty = 2,
        funFact = "Kohl wurde später als 'Kanzler der Einheit' bezeichnet."
    ),

    // --- HARD (12 questions) ---

    Question(
        categoryId = 3,
        questionText = "Welche Schiffe versenkten die Deutschen im Ersten Weltkrieg, was den USA fast den Kriegseintritt kostete?",
        answerA = "RMS Titanic",
        answerB = "SS Sussex",
        answerC = "RMS Lusitania",
        answerD = "HMS Audacious",
        correctAnswer = 2,
        explanation = "Die RMS Lusitania wurde am 7. Mai 1915 von einem deutschen U-Boot versenkt. Dabei starben 1.198 Menschen, darunter 128 US-Bürger.",
        difficulty = 3,
        funFact = "Die Versenkung der Lusitania trug wesentlich zur Kriegsstimmung in den USA bei, auch wenn der Eintritt erst 1917 erfolgte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Ems-Depesche' von 1870?",
        answerA = "Ein Telegramm, das den Deutsch-Französischen Krieg provozierte",
        answerB = "Die erste telegrafische Nachricht in Deutschland",
        answerC = "Ein diplomatisches Ultimatum Frankreichs",
        answerD = "Die Kriegserklärung Preußens an Österreich",
        correctAnswer = 0,
        explanation = "Bismarck redigierte die Ems-Depesche, um Frankreich zu einer Kriegserklärung zu provozieren, die zum Deutsch-Französischen Krieg 1870/71 führte.",
        difficulty = 3,
        funFact = "Bismarck sagte later, er habe die Depesche 'dem Stier ein rotes Tuch gezeigt'."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Dolchstoßlegende' in der Weimarer Republik?",
        answerA = "Eine Verschwörungstheorie, die Juden und Linke für Deutschlands Niederlage im WWI verantwortlich machte",
        answerB = "Ein militärischer Angriff auf die deutsche Heimat",
        answerC = "Eine Propaganda-Kampagne der Alliierten",
        answerD = "Ein Attentat auf Kaiser Wilhelm II.",
        correctAnswer = 0,
        explanation = "Die Dolchstoßlegende behauptete, Deutschland sei im Ersten Weltkrieg nicht militärisch besiegt worden, sondern von innen 'erdolcht'.",
        difficulty = 3,
        funFact = "Die Legende wurde intensiv von den Nationalsozialisten instrumentalisiert, um Hass auf die Weimarer Republik zu schüren."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte die Schlacht von Sedan 1870?",
        answerA = "Sie besiegelte die Niederlage Napoleons I.",
        answerB = "Sie führte zur Gefangennahme von Kaiser Napoleon III. und dem Ende des Zweiten Kaiserreichs",
        answerC = "Sie war die erste bedeutende Eisenbahnschlacht der Geschichte",
        answerD = "Sie leitete die Gründung des Deutschen Kaiserreichs unmittelbar ein",
        correctAnswer = 1,
        explanation = "In der Schlacht bei Sedan am 2. September 1870 wurde Kaiser Napoleon III. gefangen genommen, was das Ende des Zweiten Kaiserreichs bedeutete.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Wunder von Bern' 1954 historisch gesehen?",
        answerA = "Die erste Direktwahl des Bundespräsidenten",
        answerB = "Ein diplomatischer Erfolg der BRD bei den UN",
        answerC = "Der WM-Sieg Westdeutschlands, der als nationales Wiedergeburtssymbol gilt",
        answerD = "Die Unterzeichnung der Westeuropäischen Union",
        correctAnswer = 2,
        explanation = "Der Fußball-WM-Sieg 1954 in Bern gilt als symbolischer Neuanfang Deutschlands und nationaler Identitätspunkt der frühen BRD.",
        difficulty = 3,
        funFact = "Der Sieg über Ungarn 3:2 im Finale gegen den haushohen Favoriten gilt als eine der größten Überraschungen der WM-Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher osmanische Sultan eroberte Konstantinopel 1453?",
        answerA = "Suleiman der Prächtige",
        answerB = "Selim I.",
        answerC = "Mehmed II.",
        answerD = "Bayezid II.",
        correctAnswer = 2,
        explanation = "Mehmed II., auch 'der Eroberer' genannt, nahm 1453 Konstantinopel ein und beendete damit das Byzantinische Reich.",
        difficulty = 3,
        funFact = "Die Eroberung markierte für viele Historiker das Ende des Mittelalters."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Bedeutung der Lateinischen Union (Münzunion) im 19. Jahrhundert?",
        answerA = "Eine militärische Allianz lateinischsprachiger Staaten",
        answerB = "Eine Währungsunion zwischen Frankreich, Belgien, Italien, Schweiz und Griechenland",
        answerC = "Ein Handelsabkommen zwischen Südeuropa und dem Deutschen Reich",
        answerD = "Ein Bildungsprojekt zur Verbreitung des Lateinischen",
        correctAnswer = 1,
        explanation = "Die Lateinische Münzunion (1865–1927) war eine Währungsunion, die einheitliche Silber- und Goldmünzen zwischen mehreren Staaten schuf.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Rolle spielte die 'Volksfront' in Spanien in den 1930er Jahren?",
        answerA = "Sie war eine faschistische Bewegung unter Franco",
        answerB = "Sie war ein Linksbündnis, das 1936 die Wahlen gewann und den Bürgerkrieg auslöste",
        answerC = "Sie bildete die Exilregierung nach dem Bürgerkrieg",
        answerD = "Sie war eine monarchistische Partei",
        correctAnswer = 1,
        explanation = "Die Volksfront gewann 1936 die spanischen Wahlen, was Francos Militärputsch und den Spanischen Bürgerkrieg auslöste.",
        difficulty = 3,
        funFact = "Der Spanische Bürgerkrieg gilt als 'Generalprobe' für den Zweiten Weltkrieg, da Deutschland und die UdSSR darin ihre Waffen testeten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Berliner Konferenz' von 1884/85?",
        answerA = "Die Gründungskonferenz des Deutschen Kaiserreichs",
        answerB = "Ein europäischer Kongress zur Aufteilung Afrikas unter Kolonialmächten",
        answerC = "Eine Friedenskonferenz nach dem Deutsch-Französischen Krieg",
        answerD = "Der erste internationale Kongress zur Arbeiterfrage",
        correctAnswer = 1,
        explanation = "Auf der Berliner Konferenz (1884/85) teilten die europäischen Mächte Afrika unter sich auf, ohne afrikanische Vertreter einzubeziehen.",
        difficulty = 3,
        funFact = "Diese Aufteilung legte Grenzen fest, die noch heute afrikanische Konflikte mitbedingen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Augsburger Religionsfrieden von 1555?",
        answerA = "Ein Vertrag zwischen dem Papst und dem Kaiser über Kirchenreform",
        answerB = "Ein Abkommen, das lutherischen Fürsten das Recht auf ihre Konfession garantierte",
        answerC = "Ein Friedensschluss zwischen Protestanten und Katholiken in Augsburg",
        answerD = "Die kaiserliche Erlaubnis zur Reformation in Süddeutschland",
        correctAnswer = 1,
        explanation = "Der Augsburger Religionsfrieden gewährte den Reichsfürsten das Recht, die Konfession ihres Territoriums zu bestimmen ('cuius regio, eius religio').",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte die Völkerschlacht bei Leipzig 1813?",
        answerA = "Sie beendete den Dreißigjährigen Krieg",
        answerB = "Sie war Napoleons erste bedeutende Niederlage gegen Preußen",
        answerC = "Sie war eine entscheidende Niederlage Napoleons, die seinen Rückzug aus Deutschland erzwang",
        answerD = "Sie besiegelte die Auflösung des Rheinbundes",
        correctAnswer = 2,
        explanation = "Die Völkerschlacht bei Leipzig (16.–19. Oktober 1813) war Napoleons entscheidende Niederlage, die seine Herrschaft über Deutschland beendete.",
        difficulty = 3,
        funFact = "Mit über 600.000 beteiligten Soldaten war sie bis zum Ersten Weltkrieg die größte Landschlacht der Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Neue Ostpolitik' Willy Brandts?",
        answerA = "Eine Handelspolitik mit Ostasien in den 1970ern",
        answerB = "Eine Annäherungspolitik an die Sowjetunion und Ostblockstaaten in den 1970ern",
        answerC = "Ein Militärbündnis mit Polen und der Tschechoslowakei",
        answerD = "Eine Entspannungspolitik gegenüber der DDR nach dem Mauerbau",
        correctAnswer = 1,
        explanation = "Willy Brandts Neue Ostpolitik (ab 1969) suchte Entspannung und Normalisierung mit der UdSSR, DDR, Polen und anderen Ostblockstaaten.",
        difficulty = 3,
        funFact = "Brandts Kniefall in Warschau 1970 vor dem Denkmal des Warschauer Ghettoaufstands gilt als eines der bewegendsten politischen Symbole des 20. Jahrhunderts."
    ),

    // --- EXPERT (8 questions) ---

    Question(
        categoryId = 3,
        questionText = "Was bedeutete der Begriff 'Translatio imperii' im mittelalterlichen Europa?",
        answerA = "Die Übertragung von Handelsprivilegien zwischen Fürsten",
        answerB = "Die päpstliche Übertragung der kaiserlichen Würde von den Römern auf die Franken und dann die Deutschen",
        answerC = "Der Übergang des Kaisertitels vom Oströmischen auf das Weströmische Reich",
        answerD = "Die Vererbung von Reichsgebieten durch Heirat",
        correctAnswer = 1,
        explanation = "Translatio imperii beschreibt die Theorie, dass die römische Kaiserwürde auf Karl den Großen und dann auf die deutschen Kaiser übertragen wurde.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Rolle spielte das 'Zimmermannsche Telegramm' im Ersten Weltkrieg?",
        answerA = "Es war ein deutsches Friedensangebot an Wilson",
        answerB = "Es war eine verschlüsselte Botschaft, in der Deutschland Mexiko ein Kriegsbündnis gegen die USA anbot",
        answerC = "Es enthielt den deutschen Plan für den U-Boot-Krieg",
        answerD = "Es war Deutschlands Kriegserklärung an die USA",
        correctAnswer = 1,
        explanation = "Das Zimmermann-Telegramm (Januar 1917) war eine deutsche Geheimbotschaft, die Mexiko im Gegenzug für ein Bündnis gegen die USA texanisches, arizonianisches und New Mexico-Territorium anbot.",
        difficulty = 4,
        funFact = "Die Entschlüsselung durch den britischen Geheimdienst und die anschließende Veröffentlichung beschleunigte den US-Kriegseintritt erheblich."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter dem 'Ancien Régime' in Frankreich?",
        answerA = "Die Regierung Napoleons III. nach 1852",
        answerB = "Die konstitutionelle Monarchie nach 1791",
        answerC = "Das politische und soziale System Frankreichs vor der Revolution von 1789",
        answerD = "Die Adelsherrschaft nach der Restauration 1815",
        correctAnswer = 2,
        explanation = "Das Ancien Régime bezeichnet das politische, soziale und wirtschaftliche System Frankreichs vor der Revolution, geprägt von absolutistischer Monarchie und Ständegesellschaft.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Bedeutung der 'Haager Landkriegsordnung' von 1907?",
        answerA = "Sie gründete den Ständigen Schiedshof in Den Haag",
        answerB = "Sie kodifizierte erstmals internationale Regeln für die Kriegsführung und den Schutz von Zivilisten",
        answerC = "Sie verbot den Einsatz chemischer Waffen in Kriegen",
        answerD = "Sie regelte die Seeschifffahrt während Kriegen",
        correctAnswer = 1,
        explanation = "Die Haager Landkriegsordnung von 1907 kodifizierte Regeln für die Kriegsführung, den Schutz von Kriegsgefangenen und der Zivilbevölkerung.",
        difficulty = 4,
        funFact = "Sie bildete die Grundlage für die späteren Genfer Konventionen und das moderne humanitäre Völkerrecht."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Kapp-Putsch' von 1920?",
        answerA = "Ein kommunistischer Aufstand in Bayern",
        answerB = "Ein rechtsextremer Putschversuch in Berlin gegen die Weimarer Republik",
        answerC = "Eine Meuterei der Reichsmarine in Kiel",
        answerD = "Ein separatistischer Aufstand im Rheinland",
        correctAnswer = 1,
        explanation = "Der Kapp-Putsch (März 1920) war ein rechtsextremer Putschversuch gegen die Weimarer Republik, der durch einen Generalstreik scheiterte.",
        difficulty = 4,
        funFact = "Es war das erste Mal in der Geschichte, dass ein Generalstreik einen Staatsstreich erfolgreich verhinderte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Sykes-Picot-Vereinbarung' von 1916?",
        answerA = "Ein Waffenstillstand zwischen Russland und dem Osmanischen Reich",
        answerB = "Ein geheimes Abkommen zwischen Großbritannien und Frankreich zur Aufteilung des Nahen Ostens",
        answerC = "Ein Friedensangebot der Mittelmächte an die Entente",
        answerD = "Eine britisch-französische Militärstrategie gegen die Osmanen",
        correctAnswer = 1,
        explanation = "Die Sykes-Picot-Vereinbarung war ein geheimes britisch-französisches Abkommen (1916) zur Aufteilung der arabischen Gebiete des Osmanischen Reiches.",
        difficulty = 4,
        funFact = "Die Grenzen, die durch Sykes-Picot entstanden, prägen noch heute die politischen Konflikte im Nahen Osten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bezeichnete man als 'Canossagang' und was war seine historische Bedeutung?",
        answerA = "Kaiser Heinrichs IV. Bußgang zu Papst Gregor VII. 1077 als Symbol des Investiturstreits",
        answerB = "Die Pilgerreise Karls des Großen nach Rom",
        answerC = "Die Unterwerfung Italiens unter das Deutsche Reich",
        answerD = "Der erste Kreuzzug unter deutschem Kommando",
        correctAnswer = 0,
        explanation = "Beim Canossagang 1077 unterwarf sich Kaiser Heinrich IV. Papst Gregor VII. im Investiturstreit. Das Ereignis symbolisiert den Konflikt zwischen weltlicher und geistlicher Macht.",
        difficulty = 4,
        funFact = "Bismarcks Ausspruch 'Nach Canossa gehen wir nicht' (1872) im Kulturkampf zeigt, wie lebendig dieses historische Symbol noch 800 Jahre später war."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Dawes-Plan' von 1924?",
        answerA = "Ein amerikanischer Plan zur Neugestaltung der deutschen Reparationszahlungen nach dem WWI",
        answerB = "Ein britisches Wirtschaftsprogramm für die Weimarer Republik",
        answerC = "Ein Schuldenerlass für die von Deutschland besetzten Gebiete",
        answerD = "Ein Investitionsprogramm des Völkerbundes für Deutschland",
        correctAnswer = 0,
        explanation = "Der Dawes-Plan (1924) regelte Deutschlands Reparationszahlungen neu und ermöglichte durch US-Kredite die wirtschaftliche Stabilisierung der Weimarer Republik.",
        difficulty = 4,
        funFact = "Charles Dawes erhielt für diesen Plan 1925 den Friedensnobelpreis."
    ),

    // --- MASTER (5 questions) ---

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter der 'Kontinentalsperre' Napoleons und warum scheiterte sie?",
        answerA = "Eine militärische Blockade britischer Küstengewässer durch die französische Marine",
        answerB = "Ein Wirtschaftsembargo gegen Großbritannien, das europäische Kontinentalmächte zur Beteiligung zwang, aber durch Schmuggel und den Russlandfeldzug scheiterte",
        answerC = "Eine politische Isolation Frankreichs nach der Niederlage bei Waterloo",
        answerD = "Ein Handelspakt zwischen Frankreich und Preußen gegen britische Waren",
        correctAnswer = 1,
        explanation = "Napoleons Kontinentalsperre (1806) sollte Großbritannien wirtschaftlich ruinieren, scheiterte aber an weit verbreitetem Schmuggel und daran, dass Russlands Ausstieg 1812 den Russlandfeldzug provozierte.",
        difficulty = 5,
        funFact = "Die Kontinentalsperre schadete Europa oft mehr als Großbritannien, da sie den Zugang zu wichtigen Kolonialwaren unterband."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Gleichschaltung' im nationalsozialistischen Deutschland?",
        answerA = "Die Vereinheitlichung der deutschen Streitkräfte unter einem Oberkommando",
        answerB = "Die wirtschaftliche Angleichung der deutschen Bundesstaaten",
        answerC = "Der systematische Prozess der Unterordnung aller gesellschaftlichen Institutionen unter NS-Kontrolle und -Ideologie",
        answerD = "Die politische Integration Österreichs ins Deutsche Reich",
        correctAnswer = 2,
        explanation = "Die Gleichschaltung (ab 1933) bezeichnete den Prozess, durch den die Nationalsozialisten Parteien, Gewerkschaften, Medien und alle anderen Institutionen unter ihre Kontrolle brachten.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Historikerstreit' der 1980er Jahre in Deutschland?",
        answerA = "Eine akademische Debatte über die Echtheit von Hitlers Tagebüchern",
        answerB = "Ein öffentlicher Streit unter deutschen Historikern über die Einzigartigkeit des Holocaust und Deutschlands historische Verantwortung",
        answerC = "Eine Kontroverse über die Bewertung der DDR-Geschichte nach der Wiedervereinigung",
        answerD = "Ein Disput über die Ursachen der Weimarer Inflation von 1923",
        correctAnswer = 1,
        explanation = "Der Historikerstreit (1986–89) war eine öffentliche Debatte, ausgelöst durch Ernst Noltes These, der Holocaust sei eine 'Reaktion' auf sowjetische Verbrechen gewesen – was Jürgen Habermas und andere scharf zurückwiesen.",
        difficulty = 5,
        funFact = "Der Streit prägte die deutsche Erinnerungskultur nachhaltig und beeinflusste, wie Deutschland mit seiner NS-Vergangenheit umgeht."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche geopolitische Theorie formulierte Halford Mackinder 1904, die die strategische Planung im 20. Jahrhundert beeinflusste?",
        answerA = "Die Seemacht-Theorie, wonach Weltmacht durch Kontrolle der Ozeane entsteht",
        answerB = "Die Heartland-Theorie, wonach Kontrolle über Eurasiens Kernland zur Weltherrschaft führe",
        answerC = "Die Randstaaten-Theorie, die Randgebiete als entscheidend für Weltmacht betrachtete",
        answerD = "Die Dominotheorie, die kommunistische Ausbreitung als Kettenreaktion beschrieb",
        correctAnswer = 1,
        explanation = "Mackinders Heartland-Theorie (1904) besagte: 'Wer Osteuropa beherrscht, gebietet über das Herzland; wer das Herzland beherrscht, gebietet über die Weltinsel; wer die Weltinsel beherrscht, gebietet über die Welt.'",
        difficulty = 5,
        funFact = "Mackinders Theorie beeinflusste sowohl NS-Geopolitiker wie Karl Haushofer als auch die US-Eindämmungsstrategie im Kalten Krieg."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Piłsudski-Doktrin' im Polen der Zwischenkriegszeit?",
        answerA = "Eine Wirtschaftspolitik zur Industrialisierung Polens in den 1920ern",
        answerB = "Eine außenpolitische Strategie, die Polen zwischen Deutschland und der UdSSR durch Gleichgewichtspolitik sichern sollte",
        answerC = "Ein Militärpakt zwischen Polen und Frankreich gegen das Deutsche Reich",
        answerD = "Eine Strategie zur ethnischen Homogenisierung Polens durch Umsiedlung von Minderheiten",
        correctAnswer = 1,
        explanation = "Józef Piłsudski verfolgte eine 'Politique d'équilibre', die Polen durch Nichtangriffspakte mit beiden mächtigen Nachbarn (Deutschland 1934, UdSSR 1932) schützen sollte.",
        difficulty = 5,
        funFact = "Nach Piłsudskis Tod 1935 brach diese Strategie zusammen, was Polen anfälliger für die spätere Doppelbedrohung machte."
    ),

    // --- NEW EASY (15 questions) ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Berliner Mauer gebaut?",
        answerA = "1955",
        answerB = "1958",
        answerC = "1961",
        answerD = "1963",
        correctAnswer = 2,
        explanation = "Die Berliner Mauer wurde in der Nacht vom 12. auf den 13. August 1961 von der DDR errichtet, um die Massenflucht in den Westen zu stoppen.",
        difficulty = 1,
        funFact = "In den zwölf Jahren vor dem Mauerbau flohen rund 2,6 Millionen Menschen aus der DDR in den Westen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher deutsche Reformator löste 1517 die Reformation aus?",
        answerA = "Johannes Calvin",
        answerB = "Ulrich Zwingli",
        answerC = "Martin Luther",
        answerD = "Thomas Müntzer",
        correctAnswer = 2,
        explanation = "Martin Luther veröffentlichte 1517 seine 95 Thesen gegen den Ablasshandel und löste damit die Reformation der christlichen Kirche aus.",
        difficulty = 1,
        funFact = "Luther übersetzte die Bibel ins Deutsche und prägte damit maßgeblich die deutsche Schriftsprache."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches antike Weltwunder stand in Alexandria?",
        answerA = "Der Koloss von Rhodos",
        answerB = "Der Leuchtturm von Alexandria",
        answerC = "Der Artemistempel von Ephesos",
        answerD = "Die hängenden Gärten von Babylon",
        correctAnswer = 1,
        explanation = "Der Leuchtturm von Alexandria war eines der Sieben Weltwunder der Antike und stand auf der Insel Pharos vor Alexandria.",
        difficulty = 1,
        funFact = "Der Leuchtturm war über 100 Meter hoch und diente jahrhundertelang als Seezeichen für Schiffe im Mittelmeer."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie nannte man den Zeitraum von 1933 bis 1945 in Deutschland?",
        answerA = "Die Weimarer Republik",
        answerB = "Das Zweite Reich",
        answerC = "Das Dritte Reich",
        answerD = "Die Bundesrepublik",
        correctAnswer = 2,
        explanation = "Der Zeitraum der nationalsozialistischen Herrschaft unter Adolf Hitler von 1933 bis 1945 wird als Drittes Reich oder NS-Zeit bezeichnet.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Schiff sank 1912 auf seiner Jungfernfahrt?",
        answerA = "RMS Lusitania",
        answerB = "SS Olympic",
        answerC = "HMS Britannic",
        answerD = "RMS Titanic",
        correctAnswer = 3,
        explanation = "Die RMS Titanic sank in der Nacht vom 14. auf den 15. April 1912 nach einer Kollision mit einem Eisberg im Nordatlantik.",
        difficulty = 1,
        funFact = "Beim Untergang der Titanic starben rund 1.500 Menschen — etwa zwei Drittel der Passagiere und Besatzungsmitglieder."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann wurde die Bundesrepublik Deutschland gegründet?",
        answerA = "1945",
        answerB = "1947",
        answerC = "1949",
        answerD = "1951",
        correctAnswer = 2,
        explanation = "Die Bundesrepublik Deutschland wurde am 23. Mai 1949 gegründet, als das Grundgesetz in Kraft trat.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war die erste Frau auf dem englischen Thron?",
        answerA = "Elisabeth I.",
        answerB = "Maria I.",
        answerC = "Viktoria",
        answerD = "Anna",
        correctAnswer = 1,
        explanation = "Maria I. (auch 'Bloody Mary' genannt) war 1553 die erste Königin von England und regierte bis 1558.",
        difficulty = 1,
        funFact = "Maria I. ließ über 280 Protestanten verbrennen, was ihr den Beinamen 'Bloody Mary' einbrachte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis begann am 6. Juni 1944?",
        answerA = "Die Kapitulation Deutschlands",
        answerB = "Der D-Day – die allierte Landung in der Normandie",
        answerC = "Die Befreiung Roms durch die Alliierten",
        answerD = "Die Ardennenoffensive",
        correctAnswer = 1,
        explanation = "Am 6. Juni 1944 (D-Day) landeten alliierte Truppen in der Normandie in Frankreich – der größte amphibische Angriff der Geschichte.",
        difficulty = 1,
        funFact = "An der Landung waren über 156.000 Soldaten, 5.000 Schiffe und 13.000 Flugzeuge beteiligt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt war das Zentrum des antiken Griechenlands?",
        answerA = "Sparta",
        answerB = "Korinth",
        answerC = "Theben",
        answerD = "Athen",
        correctAnswer = 3,
        explanation = "Athen war das kulturelle und politische Zentrum des antiken Griechenlands und Geburtsort der Demokratie.",
        difficulty = 1,
        funFact = "Die Akropolis von Athen mit dem Parthenon-Tempel wurde um 447 v. Chr. unter Perikles erbaut."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Krieg fand von 1950 bis 1953 statt?",
        answerA = "Vietnamkrieg",
        answerB = "Koreakrieg",
        answerC = "Falklandkrieg",
        answerD = "Golfkrieg",
        correctAnswer = 1,
        explanation = "Der Koreakrieg dauerte von 1950 bis 1953 und endete mit einem Waffenstillstand, der die Halbinsel an der 38. Breitengrade teilte.",
        difficulty = 1,
        funFact = "Der Koreakrieg gilt als 'vergessener Krieg', obwohl über 5 Millionen Menschen dabei ihr Leben verloren."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die erste Hauptstadt des vereinten Deutschen Reiches nach 1871?",
        answerA = "Frankfurt",
        answerB = "München",
        answerC = "Hamburg",
        answerD = "Berlin",
        correctAnswer = 3,
        explanation = "Berlin war die Hauptstadt des Deutschen Kaiserreichs, das am 18. Januar 1871 im Spiegelsaal von Versailles ausgerufen wurde.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Kleopatra?",
        answerA = "Eine griechische Göttin",
        answerB = "Die letzte Pharaonin des antiken Ägyptens",
        answerC = "Eine römische Kaiserin",
        answerD = "Eine karthagische Feldherrin",
        correctAnswer = 1,
        explanation = "Kleopatra VII. war die letzte Herrscherin aus der ptolemäischen Dynastien und die letzte Pharaonin des antiken Ägyptens.",
        difficulty = 1,
        funFact = "Kleopatra war nicht ägyptisch, sondern griechisch-makedonischer Herkunft und die erste der Ptolemäer, die Ägyptisch sprach."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Land führte als erstes die Todesstrafe in Friedenszeiten ab?",
        answerA = "Frankreich",
        answerB = "Schweden",
        answerC = "San Marino",
        answerD = "Portugal",
        correctAnswer = 2,
        explanation = "San Marino schaffte die Todesstrafe bereits 1865 ab und war damit weltweit eines der ersten Länder, die dies taten.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß der erste Mensch im Weltall?",
        answerA = "Neil Armstrong",
        answerB = "Buzz Aldrin",
        answerC = "Juri Gagarin",
        answerD = "Alan Shepard",
        correctAnswer = 2,
        explanation = "Juri Gagarin war am 12. April 1961 der erste Mensch im Weltall und umkreiste die Erde in 108 Minuten.",
        difficulty = 1,
        funFact = "Gagarin war Pilot der sowjetischen Luftwaffe und wurde nach seinem Flug weltweit als Held gefeiert."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fand die amerikanische Unabhängigkeitserklärung statt?",
        answerA = "1770",
        answerB = "1776",
        answerC = "1783",
        answerD = "1789",
        correctAnswer = 1,
        explanation = "Die amerikanische Unabhängigkeitserklärung wurde am 4. Juli 1776 vom Kontinentalkongress verabschiedet.",
        difficulty = 1,
        funFact = "Der 4. Juli wird in den USA als 'Independence Day' gefeiert — einer der wichtigsten Nationalfeiertage des Landes."
    ),

    // --- NEW MEDIUM (20 questions) ---

    Question(
        categoryId = 3,
        questionText = "Was war der 'Schwarze Freitag' von 1929?",
        answerA = "Der Tag, an dem die Weimarer Republik zusammenbrach",
        answerB = "Der Beginn der Weltwirtschaftskrise durch den Börsencrash in New York",
        answerC = "Der größte Streik in der deutschen Geschichte",
        answerD = "Der Tag, an dem Hindenburg die Notstandsgesetze unterzeichnete",
        correctAnswer = 1,
        explanation = "Am 24. Oktober 1929 (in den USA der 'Black Thursday') brach die New Yorker Börse zusammen und löste die Weltwirtschaftskrise aus.",
        difficulty = 2,
        funFact = "In Deutschland sprach man vom 'Schwarzen Freitag' (25. Oktober), weil der Crash erst mit Zeitverzögerung die europäischen Märkte traf."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher chinesische Kaiser ließ die Große Mauer in ihrer heutigen Form ausbauen?",
        answerA = "Qin Shi Huangdi",
        answerB = "Han Wudi",
        answerC = "Kaiser Yongle",
        answerD = "Wanli",
        correctAnswer = 3,
        explanation = "Die meisten heute noch sichtbaren Abschnitte der Chinesischen Mauer wurden unter Kaiser Wanli der Ming-Dynastie (16./17. Jh.) errichtet.",
        difficulty = 2,
        funFact = "Die Große Mauer ist über 21.000 km lang und wurde über viele Dynastien hinweg gebaut und erweitert."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Anschluss' von 1938?",
        answerA = "Die Eingliederung des Sudetenlandes ins Deutsche Reich",
        answerB = "Die Annexion Österreichs durch das nationalsozialistische Deutschland",
        answerC = "Der Beitritt Deutschlands zum Völkerbund",
        answerD = "Die Vereinigung von Preußen und Bayern",
        correctAnswer = 1,
        explanation = "Der 'Anschluss' bezeichnete die Annexion Österreichs durch das Deutsche Reich am 12./13. März 1938.",
        difficulty = 2,
        funFact = "Österreich blieb bis 1945 Teil des Deutschen Reichs und wurde erst nach dem Zweiten Weltkrieg wieder ein eigenständiger Staat."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Konferenz legte 1945 die Nachkriegsordnung Europas fest?",
        answerA = "Konferenz von Teheran",
        answerB = "Konferenz von Casablanca",
        answerC = "Potsdamer Konferenz",
        answerD = "Konferenz von Jalta",
        correctAnswer = 2,
        explanation = "Auf der Potsdamer Konferenz (Juli/August 1945) einigten sich die Siegermächte USA, UdSSR und Großbritannien auf die Grundzüge der Nachkriegsordnung Deutschlands.",
        difficulty = 2,
        funFact = "In Potsdam wurde u.a. die Aufteilung Deutschlands in vier Besatzungszonen und die Entnazifizierung beschlossen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Otto von Bismarck?",
        answerA = "Der erste Kaiser des Deutschen Reiches",
        answerB = "Der erste Bundeskanzler der Weimarer Republik",
        answerC = "Der preußische Ministerpräsident und erste Reichskanzler, der Deutschland einigte",
        answerD = "Ein preußischer General, der Frankreich besiegte",
        correctAnswer = 2,
        explanation = "Otto von Bismarck war preußischer Ministerpräsident und erster Reichskanzler, der durch drei Kriege die deutschen Staaten 1871 zum Kaiserreich einte.",
        difficulty = 2,
        funFact = "Bismarck führte als erster Staatsmann eine Kranken- und Rentenversicherung ein und gilt als Vater des modernen Sozialstaats."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Kristallnacht' (Novemberpogrome) von 1938?",
        answerA = "Ein Bombenangriff auf jüdische Viertel in Berlin",
        answerB = "Ein landesweites Pogrom gegen Juden in Deutschland und Österreich",
        answerC = "Die erste staatliche Deportation von Juden",
        answerD = "Die Einführung der Nürnberger Rassegesetze",
        correctAnswer = 1,
        explanation = "In der Nacht vom 9. auf den 10. November 1938 wurden jüdische Geschäfte, Synagogen und Wohnungen zerstört. Dabei wurden ca. 400 Menschen getötet und 30.000 verhaftet.",
        difficulty = 2,
        funFact = "Der Name 'Kristallnacht' bezieht sich auf die zersplitterten Schaufensterscheiben, verniedlicht aber das tatsächliche Ausmaß der Gewalt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Herrscher ließ den Petersdom in Rom im 16. Jahrhundert neu errichten?",
        answerA = "Papst Leo X.",
        answerB = "Papst Julius II.",
        answerC = "Kaiser Karl V.",
        answerD = "Papst Paul III.",
        correctAnswer = 1,
        explanation = "Papst Julius II. beauftragte 1506 den Neubau des Petersdoms und legte den Grundstein. Der Bau dauerte über 100 Jahre.",
        difficulty = 2,
        funFact = "Die Finanzierung des Petersdoms durch den Ablasshandel war ein Hauptauslöser für Luthers Kritik und die Reformation."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der NATO-Doppelbeschluss von 1979?",
        answerA = "Ein Vertrag zur nuklearen Abrüstung zwischen NATO und Warschauer Pakt",
        answerB = "Die Entscheidung, neue US-Mittelstreckenraketen in Europa zu stationieren, verbunden mit einem Verhandlungsangebot",
        answerC = "Ein Verteidigungsplan gegen eine sowjetische Invasion Westeuropas",
        answerD = "Der Beitritt Spaniens und Portugals zur NATO",
        correctAnswer = 1,
        explanation = "Der NATO-Doppelbeschluss (1979) sah die Stationierung von Pershing-II-Raketen in Europa vor, falls die UdSSR ihre SS-20-Raketen nicht abrüsten würde.",
        difficulty = 2,
        funFact = "Der Beschluss löste in Deutschland die größte Friedensbewegung der Nachkriegsgeschichte aus, mit bis zu 300.000 Demonstranten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Zivilisation errichtete Machu Picchu?",
        answerA = "Die Azteken",
        answerB = "Die Maya",
        answerC = "Die Olmeken",
        answerD = "Die Inka",
        correctAnswer = 3,
        explanation = "Machu Picchu wurde von den Inka im 15. Jahrhundert auf einem Bergrücken in den peruanischen Anden errichtet.",
        difficulty = 2,
        funFact = "Machu Picchu war wahrscheinlich eine Sommerresidenz des Inka-Herrschers Pachacuti und wurde von den Spaniern nie entdeckt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Prager Frühling' von 1968?",
        answerA = "Ein Studentenaufstand gegen die kommunistische Regierung der CSSR",
        answerB = "Eine Reformbewegung unter Alexander Dubček, die durch sowjetische Truppen beendet wurde",
        answerC = "Die Gründung der Tschechoslowakei als demokratischer Staat",
        answerD = "Ein diplomatischer Annäherungsversuch zwischen der CSSR und Westdeutschland",
        correctAnswer = 1,
        explanation = "Der Prager Frühling war eine Reformbewegung unter KP-Chef Dubček (Sozialismus mit menschlichem Antlitz), die im August 1968 durch Truppen des Warschauer Pakts beendet wurde.",
        difficulty = 2,
        funFact = "In der Nacht vom 20. auf den 21. August 1968 marschierten 200.000 Soldaten aus fünf Ostblockstaaten in die CSSR ein."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Pharao ließ den Tempel von Abu Simbel erbauen?",
        answerA = "Tutanchamun",
        answerB = "Echnaton",
        answerC = "Ramses II.",
        answerD = "Thutmosis III.",
        correctAnswer = 2,
        explanation = "Ramses II. ließ den Großen Tempel von Abu Simbel um 1264 v. Chr. in den Nubischen Sandstein hauen – als Monument seiner eigenen Vergöttlichung.",
        difficulty = 2,
        funFact = "1968 wurde der Tempel aufwändig versetzt, um ihn vor dem steigenden Wasser des Assuan-Staudamms zu retten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis löste die Kuba-Krise 1962 aus?",
        answerA = "Die gescheiterte Invasion in der Schweinebucht",
        answerB = "Die Entdeckung sowjetischer Raketenbasen auf Kuba durch US-Aufklärungsflugzeuge",
        answerC = "Fidel Castros Machtergreifung auf Kuba",
        answerD = "Ein sowjetisches Ultimatum zur Räumung Westberlins",
        correctAnswer = 1,
        explanation = "Am 14. Oktober 1962 fotografierten US-Aufklärungsflugzeuge sowjetische Raketenbasen auf Kuba und lösten damit die Kuba-Krise aus.",
        difficulty = 2,
        funFact = "Die 13-tägige Kuba-Krise gilt als der Moment, in dem die Welt einem Atomkrieg am nächsten war."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Weimarer Republik?",
        answerA = "Das Deutsche Reich unter Kaiser Wilhelm II.",
        answerB = "Die erste parlamentarische Demokratie in Deutschland von 1919 bis 1933",
        answerC = "Ein Stadtstaatenbund mit Weimar als Hauptstadt",
        answerD = "Die Übergangsregierung nach dem Zweiten Weltkrieg",
        correctAnswer = 1,
        explanation = "Die Weimarer Republik war die erste deutsche Demokratie, die von 1919 bis zur Machtergreifung der Nationalsozialisten 1933 bestand.",
        difficulty = 2,
        funFact = "Die Verfassung wurde in Weimar beschlossen, weil die Hauptstadt Berlin nach der Novemberrevolution als zu unruhig galt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte der Wiener Kongress von 1814/15?",
        answerA = "Er beendete den Krimkrieg und regelte die Nachfolge des Osmanischen Reiches",
        answerB = "Er stellte nach den Napoleonischen Kriegen die politische Ordnung Europas neu auf",
        answerC = "Er gründete den Deutschen Bund als Vorläufer des Deutschen Reiches",
        answerD = "Er besiegelte die Aufteilung Polens unter Russland, Preußen und Österreich",
        correctAnswer = 1,
        explanation = "Der Wiener Kongress (1814/15) schuf nach Napoleons Niederlagen eine neue europäische Ordnung auf Basis von Restauration und Gleichgewicht der Mächte.",
        difficulty = 2,
        funFact = "Der österreichische Außenminister Metternich war der dominierende Gestalter des Kongresses und der europäischen Politik danach."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Kalte Krieg'?",
        answerA = "Ein Krieg zwischen Russland und Finnland im Winter 1939/40",
        answerB = "Die militärische Konfrontation zwischen NATO und Warschauer Pakt in Mitteleuropa",
        answerC = "Der ideologische und geopolitische Konflikt zwischen den USA und der UdSSR von 1947 bis 1991",
        answerD = "Ein Konflikt um arktische Territorien zwischen Russland und Kanada",
        correctAnswer = 2,
        explanation = "Der Kalte Krieg war die Systemkonfrontation zwischen den kapitalistischen USA und dem kommunistischen Sowjetblock von ca. 1947 bis zum Zerfall der UdSSR 1991.",
        difficulty = 2,
        funFact = "Der Begriff 'Kalter Krieg' wurde 1947 vom US-Berater Bernard Baruch geprägt, um den Konflikt ohne direkten militärischen Zusammenstoß zu beschreiben."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt wurde durch den Vesuv-Ausbruch 79 n. Chr. begraben?",
        answerA = "Neapel",
        answerB = "Herculaneum",
        answerC = "Sorrent",
        answerD = "Pompeji",
        correctAnswer = 3,
        explanation = "Pompeji wurde beim Ausbruch des Vesuvs am 24. August 79 n. Chr. unter Asche und Bimsstein begraben und dadurch für die Nachwelt konserviert.",
        difficulty = 2,
        funFact = "Auch die Stadt Herculaneum wurde beim gleichen Ausbruch zerstört, allerdings durch einen pyroklastischen Strom statt durch Asche."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann begann der Vietnamkrieg für die USA?",
        answerA = "1950",
        answerB = "1955",
        answerC = "1964",
        answerD = "1968",
        correctAnswer = 2,
        explanation = "Die direkte US-Kriegsbeteiligung begann nach dem Tonkin-Zwischenfall im August 1964, als der Kongress Präsident Johnson weitreichende Kriegsvollmachten erteilte.",
        difficulty = 2,
        funFact = "Der Vietnamkrieg kostete über 58.000 US-Soldaten das Leben und endete 1975 mit dem Sieg Nordvietnams."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Entdeckung machte Howard Carter 1922?",
        answerA = "Das Grab des Pharaos Ramses II. im Tal der Könige",
        answerB = "Das nahezu unberührte Grab des Pharaos Tutanchamun",
        answerC = "Die Überreste der versunkenen Stadt Theben",
        answerD = "Die erste vollständige Hieroglyphen-Inschrift",
        correctAnswer = 1,
        explanation = "Howard Carter entdeckte am 4. November 1922 das fast unberührte Grab des Pharaos Tutanchamun im Tal der Könige in Ägypten.",
        difficulty = 2,
        funFact = "Der Fund der goldenen Totenmaske Tutanchamuns gilt als einer der bedeutendsten archäologischen Funde des 20. Jahrhunderts."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der Sputnik-Schock von 1957?",
        answerA = "Der Absturz eines sowjetischen Satelliten über den USA",
        answerB = "Die Erschütterung des westlichen Selbstverständnisses durch den ersten Satelliten der UdSSR im Orbit",
        answerC = "Ein sowjetischer Atomtest, der die US-Raketenabwehr überlistete",
        answerD = "Die erste Mondlandung der Sowjetunion",
        correctAnswer = 1,
        explanation = "Als die Sowjetunion am 4. Oktober 1957 mit Sputnik 1 den ersten Satelliten ins All schoss, erschütterte dies die USA und löste das Wettrennen ins All aus.",
        difficulty = 2,
        funFact = "Als Reaktion auf den Sputnik-Schock gründeten die USA 1958 die NASA und verstärkten massiv Bildungsinvestitionen in Naturwissenschaften."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Vertrag beendete den Siebenjährigen Krieg?",
        answerA = "Friede von Basel",
        answerB = "Vertrag von Hubertusburg",
        answerC = "Friede von Rijswijk",
        answerD = "Vertrag von Fontainebleau",
        correctAnswer = 1,
        explanation = "Der Siebenjährige Krieg (1756–1763) endete mit dem Frieden von Hubertusburg (zwischen Preußen, Österreich und Sachsen) sowie dem Pariser Frieden (Kolonialkrieg).",
        difficulty = 2,
        funFact = "Der Siebenjährige Krieg gilt als erster 'Weltkrieg', da er auf mehreren Kontinenten gleichzeitig geführt wurde."
    ),

    // --- NEW HARD (15 questions) ---

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter dem 'Kulturkampf' im Deutschen Kaiserreich?",
        answerA = "Bismarcks Konflikt mit der katholischen Kirche und dem Zentrum in den 1870er Jahren",
        answerB = "Der Streit zwischen Konservativen und Liberalen über die deutsche Nationalliteratur",
        answerC = "Ein Konflikt zwischen dem Kaiser und dem Reichstag über Pressefreiheit",
        answerD = "Die Auseinandersetzung zwischen preußischen und bayerischen Kulturtraditionen",
        correctAnswer = 0,
        explanation = "Bismarcks Kulturkampf (1871–1878) war ein Machtkampf gegen den politischen Katholizismus und die Ultramontanen, der u.a. Jesuiten verbot und Kirchengesetze einführte.",
        difficulty = 3,
        funFact = "Bismarck beendete den Kulturkampf schließlich, als er die Unterstützung des Zentrums gegen die Sozialdemokraten benötigte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Bedeutung der 'Emser Depesche' und welche Konsequenz hatte sie?",
        answerA = "Sie war eine diplomatische Note, die den Deutsch-Dänischen Krieg 1864 einleitete",
        answerB = "Bismarck kürzte und verschärfte eine königliche Depesche, um Frankreich zu einer Kriegserklärung zu verleiten",
        answerC = "Sie war das erste Telegramm, das eine Kaiserproklamation ankündigte",
        answerD = "Sie enthielt Preußens Forderungen nach dem Sieg über Österreich 1866",
        correctAnswer = 1,
        explanation = "Bismarck redigierte König Wilhelms I. Bericht über sein Treffen mit dem französischen Botschafter so, dass es wie eine gegenseitige Beleidigung wirkte – was Frankreich zur Kriegserklärung provozierte.",
        difficulty = 3,
        funFact = "Bismarck gab später zu, er habe den 'roten Lumpen vor dem gallischen Stier' geschwenkt, um den gewünschten Krieg auszulösen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Ribbentrop-Molotow-Pakt' von 1939?",
        answerA = "Ein Handelsabkommen zwischen Deutschland und der Sowjetunion über Rohstoffe",
        answerB = "Ein geheimer Nichtangriffspakt zwischen Deutschland und der UdSSR mit geheimem Protokoll zur Aufteilung Osteuropas",
        answerC = "Ein Waffenstillstandsabkommen nach dem Winterkrieg",
        answerD = "Eine gegenseitige Verteidigungsgarantie zwischen dem Deutschen Reich und der Sowjetunion",
        correctAnswer = 1,
        explanation = "Der Ribbentrop-Molotow-Pakt (August 1939) war ein Nichtangriffspakt mit geheimem Zusatzprotokoll, das Osteuropa in deutsche und sowjetische Einflusszonen aufteilte.",
        difficulty = 3,
        funFact = "Das geheime Protokoll wurde von der Sowjetunion bis 1989 offiziell geleugnet."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die historische Bedeutung der 'Wannsee-Konferenz' vom Januar 1942?",
        answerA = "Die Planung der Invasion der Sowjetunion durch SS und Wehrmacht",
        answerB = "Die bürokratische Koordination der 'Endlösung der Judenfrage' – des systematischen Völkermords",
        answerC = "Die Entscheidung zur Einführung des Judensterns im besetzten Europa",
        answerD = "Die Abstimmung über die Deportation von Juden in Arbeitslager",
        correctAnswer = 1,
        explanation = "Auf der Wannsee-Konferenz (20. Januar 1942) koordinierten hochrangige NS-Beamte die systematische Vernichtung der europäischen Juden.",
        difficulty = 3,
        funFact = "Das Protokoll der Konferenz ist eines der erschreckendsten historischen Dokumente, da es den Massenmord in bürokratischer Sprache beschreibt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bezeichnete man als 'Scramble for Africa' im 19. Jahrhundert?",
        answerA = "Die Sklavenhandelsrouten zwischen Afrika und Amerika",
        answerB = "Das Wetteifern europäischer Mächte um die Kolonisierung Afrikas in den 1880er–1900ern",
        answerC = "Den Widerstand afrikanischer Völker gegen europäische Kolonialherrschaft",
        answerD = "Die Suche nach dem Ursprung des Nils durch europäische Entdecker",
        correctAnswer = 1,
        explanation = "Als 'Scramble for Africa' bezeichnet man den intensiven Wettbewerb europäischer Mächte um afrikanische Kolonien, der Afrika bis 1914 fast vollständig aufgeteilt hatte.",
        difficulty = 3,
        funFact = "1880 kontrollierten Europäer nur 10 % Afrikas — bis 1914 waren es 90 %."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Lebensraum'-Politik im Nationalsozialismus?",
        answerA = "Ein Programm zur Umsiedlung von Deutschen in Südamerika",
        answerB = "Hitlers Konzept der Eroberung osteuropäischer Gebiete zur deutschen Besiedlung und Ausbeutung",
        answerC = "Eine Stadtplanung zur Entlastung überfüllter Großstädte",
        answerD = "Ein Naturschutzprogramm zur Renaturierung deutscher Wälder",
        correctAnswer = 1,
        explanation = "Hitlers 'Lebensraum im Osten'-Konzept sah die Eroberung slawischer Gebiete vor, um dort Deutsche anzusiedeln und die einheimische Bevölkerung zu versklagen oder zu vernichten.",
        difficulty = 3,
        funFact = "Dieses Konzept war ein zentrales Motiv für den Überfall auf die Sowjetunion 1941 und eng mit der Vernichtungspolitik verbunden."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Gunboat Diplomacy' im 19./frühen 20. Jahrhundert?",
        answerA = "Eine britische Strategie zur Kontrolle der Handelsrouten im Mittelmeer",
        answerB = "Die Nutzung militärischer Überlegenheit (oft Kriegsschiffe) durch Großmächte, um schwächere Staaten zu Konzessionen zu zwingen",
        answerC = "Ein Abkommen zwischen europäischen Mächten über Flottengrößen",
        answerD = "Die erste internationale Konvention zum Schutz ziviler Schiffe in Kriegen",
        correctAnswer = 1,
        explanation = "Gunboat Diplomacy bezeichnete die Drohung oder den Einsatz militärischer Macht — oft durch Kriegsschiffe — als diplomatisches Druckmittel gegenüber schwächeren Staaten.",
        difficulty = 3,
        funFact = "Ein bekanntes Beispiel ist die 'Venezuelakrise' 1902/03, als Deutschland und Großbritannien Venezuela mit Kriegsschiffen blockierten, um Schulden einzutreiben."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte die 'Reichsgründung von unten' 1848/49?",
        answerA = "Sie war die erste gelungene demokratische Revolution in Deutschland",
        answerB = "Die Frankfurter Nationalversammlung versuchte, Deutschland von unten durch Volkssouveränität zu einigen, scheiterte aber",
        answerC = "Sie führte zur Eingliederung Österreichs in den Deutschen Bund",
        answerD = "Sie war der Ursprung des norddeutschen Bundes unter preußischer Führung",
        correctAnswer = 1,
        explanation = "Die Frankfurter Nationalversammlung (Paulskirchenparlament) versuchte 1848/49, Deutschland demokratisch von unten zu einigen. Das Scheitern führte zur Reaktion und späteren Einigung von oben durch Bismarck.",
        difficulty = 3,
        funFact = "Die Nationalversammlung bot dem preußischen König Friedrich Wilhelm IV. die Kaiserkrone an, den er aber als 'von der Revolution beschmutzt' ablehnte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Sonderweg'-Begriff in der deutschen Geschichtsschreibung?",
        answerA = "Deutschlands besondere Leistungen in Wissenschaft und Kultur im 19. Jahrhundert",
        answerB = "Die These, dass Deutschlands historische Entwicklung von einem 'normalen' westlichen Pfad abwich und zum Nationalsozialismus führte",
        answerC = "Bismarcksdeutsche Außenpolitik der Bündnisse und Gleichgewichtspolitik",
        answerD = "Die These, dass Deutschland einen eigenen Weg zwischen Kapitalismus und Kommunismus gesucht hat",
        correctAnswer = 1,
        explanation = "Die 'Sonderweg'-These besagt, dass Deutschland keine vollständige bürgerliche Revolution erlebte und daher einen Sonderweg zur Moderne nahm, der in den Nationalsozialismus mündete.",
        difficulty = 3,
        funFact = "Die These ist umstritten — Kritiker wie David Blackbourn und Geoff Eley argumentierten, der deutsche Weg sei nicht wirklich 'besonders' gewesen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Aktion T4' im nationalsozialistischen Deutschland?",
        answerA = "Ein Geheimplan zur Deportation aller Juden aus dem Deutschen Reich",
        answerB = "Das NS-Programm zur systematischen Ermordung von Menschen mit Behinderungen",
        answerC = "Eine Geheimoperation zur Sabotage feindlicher Infrastruktur",
        answerD = "Die Geheimdienstoperation zur Überwachung von Regimegegnern",
        correctAnswer = 1,
        explanation = "Die 'Aktion T4' war das NS-Vernichtungsprogramm für Menschen mit körperlichen oder geistigen Behinderungen, bei dem ca. 200.000 Menschen ermordet wurden.",
        difficulty = 3,
        funFact = "Die dabei entwickelten Methoden der Massentötung (Gaswagen, Vergasungsanlagen) wurden später bei der Judenvernichtung eingesetzt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Balfour-Deklaration' von 1917?",
        answerA = "Großbritanniens Kriegserklärung an das Osmanische Reich",
        answerB = "Ein britisches Dokument, das die Errichtung einer jüdischen Heimstätte in Palästina befürwortete",
        answerC = "Ein Friedensangebot Großbritanniens an Deutschland im Ersten Weltkrieg",
        answerD = "Der britische Plan zur Neugestaltung des Nahen Ostens nach dem Krieg",
        correctAnswer = 1,
        explanation = "Die Balfour-Deklaration (November 1917) war ein Brief des britischen Außenministers Balfour, in dem Großbritannien die Errichtung einer nationalen Heimstätte für das jüdische Volk in Palästina befürwortete.",
        difficulty = 3,
        funFact = "Die Deklaration ist bis heute ein zentrales Dokument im israelisch-palästinensischen Konflikt, da sie zugleich die Rechte der arabischen Bevölkerung sichern sollte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter 'Appeasement-Politik' in den 1930er Jahren?",
        answerA = "Eine aggressive Außenpolitik Großbritanniens gegen Hitler-Deutschland",
        answerB = "Die Beschwichtigungspolitik westlicher Mächte gegenüber Hitler, symbolisiert durch das Münchener Abkommen 1938",
        answerC = "Die Isolationspolitik der USA gegenüber europäischen Konflikten",
        answerD = "Frankreichs Annäherungsversuche an die Sowjetunion als Gegengewicht zu Deutschland",
        correctAnswer = 1,
        explanation = "Die Appeasement-Politik (bes. unter Neville Chamberlain) versuchte, Hitler durch Zugeständnisse — wie die Abtretung des Sudetenlandes 1938 — zu besänftigen.",
        difficulty = 3,
        funFact = "Chamberlain kehrte aus München mit dem Versprechen von 'Frieden für unsere Zeit' zurück — ein Jahr später begann der Zweite Weltkrieg."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Koloniale Schuldlüge' im Deutschen Reich nach dem WWI?",
        answerA = "Deutschlands Weigerung, Kriegsreparationen an seine ehemaligen Kolonien zu zahlen",
        answerB = "Die NS-Propaganda, die deutschen Kolonialismus als Wohltat für die Einheimischen darstellte",
        answerC = "Die deutsche Ablehnung des Versailler Mandatssystems mit dem Argument, Deutschlands Kolonialverwaltung sei besser als die der Konkurrenten",
        answerD = "Bismarcks Argumentation, Deutschland brauche keine Kolonien",
        correctAnswer = 2,
        explanation = "Nach Verlust der Kolonien 1919 propagierte Deutschland die 'Koloniale Schuldlüge': die Behauptung, Deutschland sei ein guter Kolonialherr gewesen und müsse die Kolonien zurückbekommen.",
        difficulty = 3,
        funFact = "Tatsächlich hatte Deutschland im Herero- und Nama-Aufstand 1904–08 einen der ersten Völkermorde des 20. Jahrhunderts begangen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Containment-Politik' und wer formulierte sie?",
        answerA = "General MacArthurs Plan zur militärischen Eindämmung Chinas im Koreakrieg",
        answerB = "George Kennans Strategie zur politischen und wirtschaftlichen Eindämmung der sowjetischen Expansion",
        answerC = "Präsident Trumans Plan zur nuklearen Abschreckung der Sowjetunion",
        answerD = "Dwight Eisenhowers Verteidigungsstrategie der massiven Vergeltung",
        correctAnswer = 1,
        explanation = "George Kennan formulierte 1946/47 die Containment-Strategie: Die UdSSR solle nicht militärisch besiegt, sondern durch politische und wirtschaftliche Mittel eingedämmt werden.",
        difficulty = 3,
        funFact = "Kennans 'Long Telegram' von 1946 und sein anonymer 'X-Artikel' von 1947 sind zwei der einflussreichsten außenpolitischen Dokumente des 20. Jahrhunderts."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Generalplan Ost' des NS-Regimes?",
        answerA = "Hitlers strategischer Operationsplan für den Überfall auf die Sowjetunion",
        answerB = "Ein SS-Plan zur Germanisierung Osteuropas durch Vertreibung und Vernichtung der einheimischen Bevölkerung",
        answerC = "Der Plan zur Errichtung eines Ostwall-Verteidigungssystems an der Oder",
        answerD = "Ein Wirtschaftsprogramm zur Ausbeutung besetzter Ostgebiete",
        correctAnswer = 1,
        explanation = "Der Generalplan Ost war ein geheimer SS-Plan (1941–1942) zur Germanisierung Osteuropas durch Massenmord, Vertreibung und Versklavung von 30–50 Millionen Menschen.",
        difficulty = 3,
        funFact = "Der Plan sah vor, innerhalb von 25–30 Jahren 80 % der polnischen, 75 % der weißrussischen und 64 % der ukrainischen Bevölkerung zu vernichten oder zu vertreiben."
    ),

    // --- NEW EASY BATCH 2 (25 questions) ---

    Question(
        categoryId = 3,
        questionText = "Wann wurde Deutschland nach dem Zweiten Weltkrieg wiedervereinigt?",
        answerA = "9. November 1989",
        answerB = "3. Oktober 1990",
        answerC = "1. Januar 1991",
        answerD = "12. September 1990",
        correctAnswer = 1,
        explanation = "Die deutsche Wiedervereinigung fand am 3. Oktober 1990 statt, als die fünf neuen Bundesländer der BRD beitraten.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher US-Präsident wurde 1963 in Dallas erschossen?",
        answerA = "Richard Nixon",
        answerB = "Lyndon B. Johnson",
        answerC = "John F. Kennedy",
        answerD = "Dwight D. Eisenhower",
        correctAnswer = 2,
        explanation = "John F. Kennedy wurde am 22. November 1963 in Dallas, Texas, bei einem Attentat erschossen.",
        difficulty = 1,
        funFact = "Lee Harvey Oswald wurde als Attentäter verhaftet, aber zwei Tage später von Jack Ruby erschossen, bevor er vor Gericht kam."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Land fand die Französische Revolution statt?",
        answerA = "England",
        answerB = "Deutschland",
        answerC = "Italien",
        answerD = "Frankreich",
        correctAnswer = 3,
        explanation = "Die Französische Revolution begann 1789 in Frankreich mit dem Sturm auf die Bastille und veränderte die politische Ordnung Europas grundlegend.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Bundeskanzler der Bundesrepublik Deutschland?",
        answerA = "Willy Brandt",
        answerB = "Ludwig Erhard",
        answerC = "Konrad Adenauer",
        answerD = "Kurt Georg Kiesinger",
        correctAnswer = 2,
        explanation = "Konrad Adenauer war von 1949 bis 1963 der erste Bundeskanzler der Bundesrepublik Deutschland.",
        difficulty = 1,
        funFact = "Adenauer war bei seiner Amtseinführung 73 Jahre alt und regierte bis er 87 war — ein Rekord in der deutschen Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Weltereignis begann am 11. September 2001?",
        answerA = "Der Irakkrieg",
        answerB = "Die Terroranschläge auf die USA",
        answerC = "Die Finanzkrise",
        answerD = "Der Afghanistandkrieg",
        correctAnswer = 1,
        explanation = "Am 11. September 2001 entführten islamistische Terroristen vier Flugzeuge und griffen das World Trade Center in New York und das Pentagon an.",
        difficulty = 1,
        funFact = "Bei den Anschlägen starben fast 3.000 Menschen aus über 90 Ländern."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Apartheid?",
        answerA = "Eine Regierungsform in Nordkorea",
        answerB = "Das System der Rassentrennung in Südafrika",
        answerC = "Ein Kolonialregime in Algerien",
        answerD = "Die Sklaverei in den USA",
        correctAnswer = 1,
        explanation = "Die Apartheid war das System der institutionalisierten Rassentrennung in Südafrika von 1948 bis 1994.",
        difficulty = 1,
        funFact = "Nelson Mandela, der die Apartheid bekämpfte, saß 27 Jahre im Gefängnis und wurde 1994 zum ersten schwarzen Präsidenten Südafrikas gewählt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Fluss war für das antike Ägypten am wichtigsten?",
        answerA = "Tigris",
        answerB = "Euphrat",
        answerC = "Jordan",
        answerD = "Nil",
        correctAnswer = 3,
        explanation = "Der Nil war die Lebensader des antiken Ägyptens. Sein jährliches Hochwasser machte das Niltal zu einem fruchtbaren Ackerbaugebiet.",
        difficulty = 1,
        funFact = "Die Ägypter nannten ihr Land 'Kemet' (das Schwarze Land), wegen des fruchtbaren schwarzen Nilschlammbodens."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann begann der Zweite Weltkrieg?",
        answerA = "1. September 1939",
        answerB = "3. September 1939",
        answerC = "1. September 1940",
        answerD = "10. Mai 1940",
        correctAnswer = 0,
        explanation = "Der Zweite Weltkrieg begann am 1. September 1939 mit dem deutschen Überfall auf Polen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr landete der erste Mensch auf dem Mond?",
        answerA = "1965",
        answerB = "1967",
        answerC = "1969",
        answerD = "1971",
        correctAnswer = 2,
        explanation = "Am 20. Juli 1969 landeten Neil Armstrong und Buzz Aldrin im Rahmen der Apollo-11-Mission als erste Menschen auf dem Mond.",
        difficulty = 1,
        funFact = "Neil Armstrongs erste Worte auf dem Mond waren: 'Das ist ein kleiner Schritt für einen Menschen, aber ein riesiger Sprung für die Menschheit.'"
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Karl der Große?",
        answerA = "Ein englischer König im Mittelalter",
        answerB = "Der erste König von Frankreich",
        answerC = "Ein fränkischer König, der weite Teile Westeuropas vereinigte",
        answerD = "Der erste Kaiser des Heiligen Römischen Reiches",
        correctAnswer = 2,
        explanation = "Karl der Große (748–814) war König der Franken und vereinigte weite Teile Westeuropas. Er wurde 800 von Papst Leo III. zum Kaiser gekrönt.",
        difficulty = 1,
        funFact = "Karl der Große gilt als 'Vater Europas' und ist Namenspatron des Karlspreises, der jährlich in Aachen für europäische Verdienste vergeben wird."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde Nelson Mandela freigelassen?",
        answerA = "1985",
        answerB = "1988",
        answerC = "1990",
        answerD = "1994",
        correctAnswer = 2,
        explanation = "Nelson Mandela wurde am 11. Februar 1990 nach 27 Jahren Gefangenschaft auf Robben Island freigelassen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Land wurde 1776 unabhängig?",
        answerA = "Kanada",
        answerB = "Australien",
        answerC = "Mexiko",
        answerD = "Die Vereinigten Staaten von Amerika",
        correctAnswer = 3,
        explanation = "Die Vereinigten Staaten erklärten am 4. Juli 1776 ihre Unabhängigkeit von Großbritannien.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das Römische Reich?",
        answerA = "Ein Stadtstaatenbund in Griechenland",
        answerB = "Ein antikes Reich mit Zentrum in Rom, das große Teile Europas und des Mittelmeerraums beherrschte",
        answerC = "Ein Handelsnetz im Mittelalter",
        answerD = "Eine Monarchie, die von Konstantinopel aus regiert wurde",
        correctAnswer = 1,
        explanation = "Das Römische Reich war ein antikes Großreich, das auf dem Höhepunkt seiner Macht große Teile Europas, Nordafrikas und des Nahen Ostens umfasste.",
        difficulty = 1,
        funFact = "Das Weströmische Reich fiel 476 n. Chr., das Oströmische (Byzantinische) Reich bestand bis 1453."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Krankheit wütete im 14. Jahrhundert als 'Schwarzer Tod' in Europa?",
        answerA = "Cholera",
        answerB = "Pocken",
        answerC = "Pest",
        answerD = "Typhus",
        correctAnswer = 2,
        explanation = "Der Schwarze Tod war eine Pestepidemie, die zwischen 1347 und 1353 schätzungsweise ein Drittel der europäischen Bevölkerung tötete.",
        difficulty = 1,
        funFact = "Die Pest wurde durch Flöhe auf Ratten übertragen und breitete sich entlang der Handelsrouten aus."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Adolf Hitler?",
        answerA = "Ein österreichischer Kaiser im Ersten Weltkrieg",
        answerB = "Der Diktator des nationalsozialistischen Deutschlands von 1933 bis 1945",
        answerC = "Ein deutsches Militärgeneral im Ersten Weltkrieg",
        answerD = "Der erste Präsident der Weimarer Republik",
        correctAnswer = 1,
        explanation = "Adolf Hitler war der Führer der NSDAP, der 1933 Reichskanzler wurde und Deutschland in eine totalitäre Diktatur verwandelte, die zum Zweiten Weltkrieg führte.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fiel die Sowjetunion auseinander?",
        answerA = "1989",
        answerB = "1990",
        answerC = "1991",
        answerD = "1993",
        correctAnswer = 2,
        explanation = "Die Sowjetunion löste sich am 25. Dezember 1991 auf, als Michail Gorbatschow als Präsident zurücktrat.",
        difficulty = 1,
        funFact = "Aus der Sowjetunion entstanden 15 unabhängige Staaten, darunter Russland, Ukraine und die baltischen Staaten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was ist der Kalte Krieg hauptsächlich?",
        answerA = "Ein Krieg, der im Winter 1944/45 stattfand",
        answerB = "Ein Krieg zwischen Russland und Finnland",
        answerC = "Ein geopolitischer Konflikt zwischen den USA und der Sowjetunion",
        answerD = "Ein Wirtschaftskrieg zwischen Europa und Asien",
        correctAnswer = 2,
        explanation = "Der Kalte Krieg war der ideologische und geopolitische Konflikt zwischen dem westlichen und dem östlichen Block, der von ca. 1947 bis 1991 dauerte.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher deutschen Stadt gaben die Nazis ihren Nürnberger Parteitagen ihren Namen?",
        answerA = "München",
        answerB = "Frankfurt",
        answerC = "Nürnberg",
        answerD = "Stuttgart",
        correctAnswer = 2,
        explanation = "Die NSDAP hielt von 1933 bis 1938 jährliche Reichsparteitage in Nürnberg ab. Dort wurden 1935 auch die antisemitischen Nürnberger Gesetze verkündet.",
        difficulty = 1,
        funFact = "Die Nürnberger Reichsparteitage waren riesige Propagandainszenierungen — Leni Riefenstahls Film 'Triumph des Willens' dokumentierte den Parteitag 1934."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer erfand den Buchdruck mit beweglichen Lettern in Europa?",
        answerA = "Leonardo da Vinci",
        answerB = "Johannes Gutenberg",
        answerC = "Albrecht Dürer",
        answerD = "Roger Bacon",
        correctAnswer = 1,
        explanation = "Johannes Gutenberg erfand um 1450 den Buchdruck mit beweglichen Metalllettern und druckte damit die erste Gutenberg-Bibel.",
        difficulty = 1,
        funFact = "Gutenbergs Erfindung gilt als eine der wichtigsten der Menschheitsgeschichte — sie ermöglichte die Massenverbreitung von Wissen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie lautete der Spitzname der deutschen Hyperinflation von 1923?",
        answerA = "Die Große Depression",
        answerB = "Der Geldtod",
        answerC = "Die Inflationskatastrophe",
        answerD = "Das Geldchaos",
        correctAnswer = 2,
        explanation = "Die deutsche Hyperinflation von 1923 war eine extreme Geldentwertung, bei der ein Brot zeitweise Milliarden Mark kostete.",
        difficulty = 1,
        funFact = "Auf dem Höhepunkt der Inflation brauchte man eine Schubkarre voll Geldscheine, um ein Brot zu kaufen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann begann die Industrielle Revolution?",
        answerA = "Mitte des 16. Jahrhunderts in Italien",
        answerB = "Mitte des 18. Jahrhunderts in England",
        answerC = "Anfang des 19. Jahrhunderts in Deutschland",
        answerD = "Ende des 17. Jahrhunderts in Frankreich",
        correctAnswer = 1,
        explanation = "Die Industrielle Revolution begann in der zweiten Hälfte des 18. Jahrhunderts in England mit der Dampfmaschine und mechanischen Webstühlen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis markierte das Ende des Mittelalters?",
        answerA = "Die Entdeckung Amerikas 1492",
        answerB = "Die Erfindung des Buchdrucks 1450",
        answerC = "Der Fall Konstantinopels 1453",
        answerD = "Die Reformation 1517",
        correctAnswer = 0,
        explanation = "Als historische Grenze zwischen Mittelalter und Neuzeit gilt häufig die Entdeckung Amerikas durch Kolumbus im Jahr 1492.",
        difficulty = 1,
        funFact = "Je nach Historiker wird auch der Fall Konstantinopels 1453 oder die Reformation 1517 als Ende des Mittelalters gesehen."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Land begann der Erste Weltkrieg mit der Kriegserklärung?",
        answerA = "Deutschland",
        answerB = "Russland",
        answerC = "Österreich-Ungarn",
        answerD = "Serbien",
        correctAnswer = 2,
        explanation = "Österreich-Ungarn erklärte Serbien am 28. Juli 1914 den Krieg — die erste offizielle Kriegserklärung des Ersten Weltkriegs.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Martin Luther King Jr.?",
        answerA = "Ein US-Präsident in den 1960ern",
        answerB = "Ein Bürgerrechtler, der für die Gleichberechtigung afroamerikanischer US-Bürger kämpfte",
        answerC = "Der erste schwarze Senator der USA",
        answerD = "Ein protestantischer Pfarrer in Deutschland",
        correctAnswer = 1,
        explanation = "Martin Luther King Jr. war ein baptistischer Pastor und führender Bürgerrechtsaktivist, der mit gewaltlosen Mitteln für die Gleichberechtigung der Afroamerikaner kämpfte.",
        difficulty = 1,
        funFact = "Seine bekannteste Rede 'I Have a Dream' hielt er am 28. August 1963 bei einem Marsch auf Washington vor 250.000 Menschen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bedeutet der Begriff 'Holocaust'?",
        answerA = "Der deutsche Überfall auf Polen",
        answerB = "Der systematische Massenmord an sechs Millionen Juden durch das NS-Regime",
        answerC = "Die Bombardierung deutscher Städte im Zweiten Weltkrieg",
        answerD = "Die Vertreibung der Deutschen nach 1945",
        correctAnswer = 1,
        explanation = "Der Holocaust bezeichnet den systematischen, staatlich organisierten Massenmord an rund sechs Millionen europäischen Juden durch das nationalsozialistische Deutschland.",
        difficulty = 1,
        funFact = null
    ),

    // --- NEW MEDIUM BATCH 2 (15 questions) ---

    Question(
        categoryId = 3,
        questionText = "Was war die 'Blitzkrieg'-Strategie im Zweiten Weltkrieg?",
        answerA = "Eine neue Taktik der deutschen Luftwaffe zur Bombardierung von Städten",
        answerB = "Eine schnelle Kombinierung von Panzer, Infanterie und Luftwaffe für rasante Durchbrüche",
        answerC = "Der deutsche Plan für einen Zweifrontenkrieg",
        answerD = "Eine Strategie der totalen Zermürbung des Feindes durch Belagerung",
        correctAnswer = 1,
        explanation = "Der Blitzkrieg war eine Taktik des schnellen Krieges, bei der Panzer, motorisierte Infanterie und Luftunterstützung koordiniert für rasche Durchbrüche eingesetzt wurden.",
        difficulty = 2,
        funFact = "Der Begriff 'Blitzkrieg' wurde weniger von deutschen Militärs selbst als von ausländischen Beobachtern und Journalisten geprägt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher deutsche Kaiser löste durch seine aggressive Außenpolitik die Entente Cordiale mit aus?",
        answerA = "Friedrich III.",
        answerB = "Wilhelm I.",
        answerC = "Wilhelm II.",
        answerD = "Karl I.",
        correctAnswer = 2,
        explanation = "Kaiser Wilhelm II. betrieb eine aggressive Weltpolitik und rüstete die Marine auf, was Großbritannien und Frankreich 1904 zur Entente Cordiale trieb.",
        difficulty = 2,
        funFact = "Wilhelm II. war ein Enkel der britischen Königin Victoria — ein Beispiel für die paradoxen dynastischen Verbindungen im Ersten Weltkrieg."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Lend-Lease Act' der USA im Zweiten Weltkrieg?",
        answerA = "Ein Gesetz, das US-Bürger zum Kriegsdienst verpflichtete",
        answerB = "Ein Programm zur Lieferung von Kriegsmaterial an die Alliierten ohne direkte Bezahlung",
        answerC = "Ein Friedensangebot der USA an Japan nach Pearl Harbor",
        answerD = "Ein Abkommen zur gemeinsamen Nutzung von Atom-Technologie",
        correctAnswer = 1,
        explanation = "Der Lend-Lease Act (1941) erlaubte den USA, Kriegsmaterial an die Alliierten zu liefern, ohne formell am Krieg beteiligt zu sein.",
        difficulty = 2,
        funFact = "Die USA lieferten über den Lend-Lease über 50 Milliarden Dollar an Kriegsmaterial, davon ein Großteil an Großbritannien und die Sowjetunion."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Kolonialisierung Australiens' durch Großbritannien?",
        answerA = "Die Entdeckung Australiens durch Captain Cook 1770",
        answerB = "Die Einrichtung einer Strafkolonie in Botany Bay ab 1788",
        answerC = "Die Annexion Australiens als britische Kronkolonie 1837",
        answerD = "Die Besiedlung Australiens durch irische Einwanderer im 19. Jahrhundert",
        correctAnswer = 1,
        explanation = "Großbritannien gründete 1788 eine Strafkolonie in New South Wales, wohin zunächst vor allem Strafgefangene deportiert wurden.",
        difficulty = 2,
        funFact = "Bis 1868 wurden insgesamt rund 162.000 Strafgefangene nach Australien deportiert."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte die Erfindung des Schießpulvers für die Geschichte?",
        answerA = "Es beendete den Einsatz von Rittern und veränderte die Kriegsführung grundlegend",
        answerB = "Es ermöglichte erstmals den Bergbau im großen Stil",
        answerC = "Es führte zur Entdeckung Amerikas durch verbesserte Schiffstechnik",
        answerD = "Es löste die erste Industrialisierungswelle in Europa aus",
        correctAnswer = 0,
        explanation = "Das Schießpulver, in Europa ab dem 13./14. Jahrhundert verbreitet, machte Rüstungen und Burgen wirkungslos und veränderte die mittelalterliche Kriegsführung grundlegend.",
        difficulty = 2,
        funFact = "Schießpulver wurde zuerst in China erfunden und gelangte über die arabische Welt nach Europa."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Herero-Aufstand' von 1904 in Deutsch-Südwestafrika?",
        answerA = "Ein anti-britischer Aufstand, den Deutschland unterstützte",
        answerB = "Ein Aufstand der Herero und Nama gegen die deutsche Kolonialherrschaft, der zum ersten Völkermord des 20. Jahrhunderts führte",
        answerC = "Ein Grenzkonflikt zwischen deutschen und britischen Kolonialgebieten",
        answerD = "Ein Bergarbeiteraufstand in den deutschen Diamantenminen",
        correctAnswer = 1,
        explanation = "Der Herero-Aufstand (1904–1908) gegen die deutsche Kolonialherrschaft wurde brutal niedergeschlagen; schätzungsweise 80 % der Herero und 50 % der Nama wurden getötet.",
        difficulty = 2,
        funFact = "Deutschland erkannte den Völkermord an den Herero und Nama erst 2021 offiziell an — mehr als 100 Jahre nach den Ereignissen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Pakt des Stahls' von 1939?",
        answerA = "Ein deutsch-japanisches Bündnis gegen die Sowjetunion",
        answerB = "Ein militärischer Beistandspakt zwischen Deutschland und Italien",
        answerC = "Deutschlands Bündnis mit der Sowjetunion vor dem Überfall auf Polen",
        answerD = "Ein Rüstungsabkommen zwischen Deutschland, Italien und Japan",
        correctAnswer = 1,
        explanation = "Der Pakt des Stahls (Mai 1939) war ein militärisches Bündnis zwischen Deutschland und Italien, das gegenseitige Unterstützung im Kriegsfall vorsah.",
        difficulty = 2,
        funFact = "Mussolini selbst gab dem Pakt seinen Namen — er wollte damit die Unerschütterlichkeit der Achse Berlin-Rom symbolisieren."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bedeutete der Begriff 'Dekolonisierung' nach 1945?",
        answerA = "Die Gründung neuer Kolonien durch die USA in Asien und Afrika",
        answerB = "Der schrittweise Rückzug europäischer Kolonialmächte aus ihren überseeischen Gebieten",
        answerC = "Die wirtschaftliche Entflechtung von Kolonien und Mutterländern",
        answerD = "Die Aufteilung der deutschen Kolonien unter den Siegermächten nach 1918",
        correctAnswer = 1,
        explanation = "Als Dekolonisierung bezeichnet man den Prozess, durch den die europäischen Kolonialmächte nach dem Zweiten Weltkrieg schrittweise ihre Kolonien in die Unabhängigkeit entließen.",
        difficulty = 2,
        funFact = "1960 gilt als das 'Jahr Afrikas' — 17 afrikanische Länder erlangten in diesem Jahr ihre Unabhängigkeit."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Warschauer Pakt'?",
        answerA = "Ein Friedensvertrag zwischen Polen und Deutschland nach dem Zweiten Weltkrieg",
        answerB = "Ein Militärbündnis der osteuropäischen Staaten unter Führung der Sowjetunion",
        answerC = "Ein Wirtschaftsbündnis der sozialistischen Staaten des Ostblocks",
        answerD = "Willy Brandts Vertrag zur Anerkennung der Oder-Neiße-Grenze",
        correctAnswer = 1,
        explanation = "Der Warschauer Pakt (1955–1991) war das Militärbündnis der sozialistischen Ostblockstaaten unter sowjetischer Führung, das Gegenstück zur westlichen NATO.",
        difficulty = 2,
        funFact = "Der Warschauer Pakt wurde als Reaktion auf den NATO-Beitritt Westdeutschlands 1955 gegründet."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Beinamen trug Friedrich II. von Preußen?",
        answerA = "Der Starke",
        answerB = "Der Große",
        answerC = "Der Weise",
        answerD = "Der Gerechte",
        correctAnswer = 1,
        explanation = "Friedrich II. von Preußen (1712–1786) wird als Friedrich der Große bezeichnet und gilt als bedeutendster Vertreter des aufgeklärten Absolutismus.",
        difficulty = 2,
        funFact = "Friedrich der Große war selbst Komponist und Flötenvirtuose und korrespondierte mit Voltaire — er verkörperte den philosophischen König."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Münchener Konferenz' von 1938?",
        answerA = "Eine Konferenz zur Gründung der NATO",
        answerB = "Ein Treffen von Hitler, Mussolini, Chamberlain und Daladier, das die Abtretung des Sudetenlandes besiegelte",
        answerC = "Die Konferenz, auf der der Zweite Weltkrieg offiziell erklärt wurde",
        answerD = "Eine Friedenskonferenz nach dem Spanischen Bürgerkrieg",
        correctAnswer = 1,
        explanation = "Auf der Münchener Konferenz (September 1938) stimmten Großbritannien und Frankreich der Abtretung des tschechoslowakischen Sudetenlandes an Deutschland zu.",
        difficulty = 2,
        funFact = "Die Tschechoslowakei selbst war nicht zur Konferenz eingeladen — ihre Aufspaltung wurde über ihren Kopf hinweg beschlossen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Schlieffen-Plan' im Ersten Weltkrieg?",
        answerA = "Deutschlands Plan für den Einsatz von U-Booten im Atlantik",
        answerB = "Der deutsche Operationsplan zum raschen Sieg über Frankreich durch Belgien, bevor Russland mobilisieren konnte",
        answerC = "Ein Verteidigungsplan für die Westfront",
        answerD = "Der österreichische Plan für die Invasion Serbiens",
        correctAnswer = 1,
        explanation = "Der Schlieffen-Plan sah vor, Frankreich schnell durch einen Schwenk durch Belgien zu besiegen, bevor Russland vollständig mobilisiert war, und dann nach Osten zu wechseln.",
        difficulty = 2,
        funFact = "Das Scheitern des Schlieffenplans führte zum langen Stellungskrieg an der Westfront, der vier Jahre dauerte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Konquistadoren'-Bewegung?",
        answerA = "Spanische und portugiesische Entdecker und Soldaten, die Amerika im 16. Jahrhundert eroberten",
        answerB = "Eine arabische Handelsbewegung entlang der afrikanischen Küste",
        answerC = "Portugiesische Seefahrer, die den Seeweg nach Indien fanden",
        answerD = "Christliche Kreuzfahrer, die das Heilige Land zurückerobern wollten",
        correctAnswer = 0,
        explanation = "Die Konquistadoren waren spanische und portugiesische Soldaten und Abenteurer, die im 16. Jahrhundert die Hochkulturen Lateinamerikas (Azteken, Inka) eroberten.",
        difficulty = 2,
        funFact = "Hernán Cortés besiegte mit nur rund 500 Soldaten das Aztekenreich — maßgeblich dank einheimischer Verbündeter und eingeschleppter Krankheiten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte der 'Atatürk'-Titel Mustafa Kemals?",
        answerA = "Er bedeutete 'Vater aller Muslime'",
        answerB = "Er bedeutete 'Vater der Türken' und symbolisierte die Gründung der modernen Türkei",
        answerC = "Er war ein osmanischer Ehrentitel für militärische Helden",
        answerD = "Er bedeutete 'Erster unter Gleichen' im türkischen Parlament",
        correctAnswer = 1,
        explanation = "Mustafa Kemal erhielt 1934 den Ehrentitel 'Atatürk' (Vater der Türken), da er die moderne Türkei nach dem Zerfall des Osmanischen Reiches gegründet hatte.",
        difficulty = 2,
        funFact = "Atatürk modernisierte die Türkei radikal: Er ersetzte die arabische Schrift durch das lateinische Alphabet und schaffte das Sultanat und das Kalifat ab."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bezeichnete man als 'Wirtschaftswunder' in der BRD?",
        answerA = "Die Ölentdeckungen in der Nordsee in den 1970ern",
        answerB = "Das rasche Wirtschaftswachstum der Bundesrepublik Deutschland in den 1950er und 60er Jahren",
        answerC = "Die erfolgreiche Währungsreform von 1948",
        answerD = "Den wirtschaftlichen Boom nach der Wiedervereinigung",
        correctAnswer = 1,
        explanation = "Das Wirtschaftswunder beschreibt den rasanten wirtschaftlichen Wiederaufbau und Aufschwung Westdeutschlands in den 1950er und 1960er Jahren.",
        difficulty = 2,
        funFact = "Westdeutschland erreichte bereits Mitte der 1950er Jahre wieder das Vorkriegsniveau des Bruttoinlandsprodukts."
    ),

    // --- NEW HARD BATCH 2 (23 questions) ---

    Question(
        categoryId = 3,
        questionText = "Was war die 'Kaiserliche Flotte' und welche politische Kontroverse löste ihr Bau aus?",
        answerA = "Die österreichische Kriegsflotte im Ersten Weltkrieg",
        answerB = "Das Flottenbauprogramm Kaiser Wilhelms II., das das deutsch-britische Verhältnis belastete und das Wettrüsten beschleunigte",
        answerC = "Die Reichsmarine der Weimarer Republik",
        answerD = "Bismarcks Handelsmarine zur Sicherung der deutschen Kolonialinteressen",
        correctAnswer = 1,
        explanation = "Das deutsche Flottenbauprogramm unter Admiral Tirpitz (ab 1898) sollte Deutschland zur Seemacht machen, bedrohte aber britische Hegemonialinteressen und trieb England in die Entente.",
        difficulty = 3,
        funFact = "Die deutschen Schlachtschiffe wurden fast nie eingesetzt — ihre bloße Existenz war die strategische Bedrohung, nicht ihr Einsatz."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Nansen-Pass'-Initiative nach dem Ersten Weltkrieg?",
        answerA = "Ein norwegisches Programm zur Aufnahme von Kriegsflüchtlingen",
        answerB = "Ein internationaler Reisepass für Staatenlose und Flüchtlinge, eingeführt vom Völkerbund",
        answerC = "Ein diplomatisches Abkommen Norwegens mit Russland",
        answerD = "Ein Vertrag zur Öffnung der arktischen Handelsrouten",
        correctAnswer = 1,
        explanation = "Der Nansen-Pass war ein 1922 eingeführtes internationales Dokument für Staatenlose, benannt nach dem norwegischen Polarforscher und Flüchtlingshochkommissar Fridtjof Nansen.",
        difficulty = 3,
        funFact = "Fridtjof Nansen erhielt 1922 den Friedensnobelpreis für seine humanitären Bemühungen um Kriegsgefangene und Flüchtlinge."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Septemberprogramm' von 1914 in Deutschland?",
        answerA = "Der deutsche Mobilmachungsplan für den Herbst 1914",
        answerB = "Reichskanzler Bethmann Hollwegs Kriegszielprogramm mit weitreichenden Annexionsplänen in Europa und Afrika",
        answerC = "Deutschlands erster Friedensvorschlag nach der Marneschlacht",
        answerD = "Der Plan für die Winteroffensive an der Ostfront 1914",
        correctAnswer = 1,
        explanation = "Das Septemberprogramm (September 1914) enthielt weitreichende deutsche Kriegsziele: Annexionen in Frankreich, Belgien und Afrika sowie wirtschaftliche Dominanz in Europa.",
        difficulty = 3,
        funFact = "Das Programm zeigt, dass Deutschland im Ersten Weltkrieg imperialistische Expansionsziele verfolgte — ein wichtiges Argument in der Fischer-Kontroverse."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Fischer-Kontroverse' in der deutschen Geschichtswissenschaft?",
        answerA = "Ein Streit über die Ursachen der Weltwirtschaftskrise 1929 und Deutschlands Mitverantwortung",
        answerB = "Fritz Fischers These, Deutschland trage die Hauptverantwortung für den Ausbruch des Ersten Weltkriegs durch aktives Anstreben",
        answerC = "Eine Debatte über Deutschlands Verantwortung für den Zweiten Weltkrieg",
        answerD = "Ein Historikerstreit über die Echtheit von Bismarcks Tagebüchern",
        correctAnswer = 1,
        explanation = "Fritz Fischer legte 1961 in 'Griff nach der Weltmacht' dar, Deutschland habe den Ersten Weltkrieg aktiv herbeigeführt und weitreichende Expansionsziele verfolgt — eine bis dahin tabuisierte These.",
        difficulty = 3,
        funFact = "Die Fischer-Kontroverse erschütterte das deutsche Geschichtsbild und führte zu einer fundamentalen Neubeurteilung der deutschen Kriegsschuld am Ersten Weltkrieg."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bezeichnete man als 'Stunde Null' in Deutschland 1945?",
        answerA = "Den Moment der deutschen Kapitulation um Mitternacht am 8. Mai 1945",
        answerB = "Den symbolischen Neuanfang Deutschlands nach dem totalen Zusammenbruch des NS-Regimes",
        answerC = "Die erste Sitzung des Parlamentarischen Rates 1948",
        answerD = "Den Beginn der Währungsreform und der Deutschen Mark 1948",
        correctAnswer = 1,
        explanation = "Die 'Stunde Null' bezeichnet den Neuanfang Deutschlands nach der totalen Kapitulation 1945 — ein Begriff, der zugleich den Bruch mit der NS-Vergangenheit symbolisieren sollte.",
        difficulty = 3,
        funFact = "Historiker kritisieren den Begriff, weil er eine vollständige Erneuerung suggeriert, die tatsächlich nicht stattfand — viele NS-Täter lebten unbehelligt weiter."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Grundlagenvertrag' von 1972 zwischen BRD und DDR?",
        answerA = "Ein Wirtschaftsvertrag über innerdeutschen Handel",
        answerB = "Ein Abkommen, in dem sich BRD und DDR gegenseitig als Staaten anerkannten und normale Beziehungen aufnahmen",
        answerC = "Der Vertrag über die Berlin-Frage und Transitrechte",
        answerD = "Ein Abkommen über die Reisefreiheit für DDR-Bürger",
        correctAnswer = 1,
        explanation = "Der Grundlagenvertrag (Dezember 1972) normalisierte die Beziehungen zwischen BRD und DDR — ohne volle diplomatische Anerkennung — und war ein Kernelement der Ostpolitik.",
        difficulty = 3,
        funFact = "Beide Staaten wurden 1973 gleichzeitig in die UNO aufgenommen — ein Symbol der neuen politischen Normalität."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter 'Euthanasie' im Sprachgebrauch des NS-Regimes?",
        answerA = "Den Tod im Kampf für das Vaterland",
        answerB = "Das Programm zur Ermordung von Menschen mit körperlichen oder geistigen Behinderungen, verhüllend als 'Gnadentod' bezeichnet",
        answerC = "Die freiwillige Sterbehilfe, die im Dritten Reich gefördert wurde",
        answerD = "Die Vernichtung politischer Gegner durch das Regime",
        correctAnswer = 1,
        explanation = "Das NS-Regime bezeichnete die Ermordung von Menschen mit Behinderungen euphemistisch als 'Euthanasie' (griech.: schöner Tod), obwohl die Opfer keinerlei Zustimmung gaben.",
        difficulty = 3,
        funFact = "Kirchlicher Widerstand — u.a. von Bischof Clemens August Graf von Galen — zwang das Regime 1941, das Programm offiziell einzustellen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Bizone' und die 'Trizone' nach dem Zweiten Weltkrieg?",
        answerA = "Die drei getrennt verwalteten Sektoren Berlins unter alliierter Kontrolle",
        answerB = "Die schrittweise Zusammenlegung der westlichen Besatzungszonen Deutschlands, die letztlich zur BRD führte",
        answerC = "Eine Unterteilung Ostdeutschlands in sowjetische Verwaltungsbezirke",
        answerD = "Die drei Wirtschaftsregionen des Ruhrgebiets unter alliierter Kontrolle",
        correctAnswer = 1,
        explanation = "Die Bizone entstand 1947 durch Zusammenlegung der US-amerikanischen und britischen Zone, die Trizone 1948 durch Einbeziehung der französischen Zone — Vorstufe zur Bundesrepublik 1949.",
        difficulty = 3,
        funFact = "Die Zusammenlegung der Westzonen war ein direktes Signal an die Sowjetunion, die im Gegenzug die Berlin-Blockade (1948/49) verhängte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Ermächtigungsgesetz' vom März 1933?",
        answerA = "Das Gesetz, das die SA als staatliche Polizeitruppe legalisierte",
        answerB = "Ein Gesetz, das dem Reichstag alle Macht enteignete und Hitler ermöglichte, ohne Parlament zu regieren",
        answerC = "Hindenburgs Erlass, der die NSDAP zur einzigen legalen Partei machte",
        answerD = "Das Gesetz, das die Verfassung der Weimarer Republik außer Kraft setzte",
        correctAnswer = 1,
        explanation = "Das Ermächtigungsgesetz (23. März 1933) übertrug der Reichsregierung die Gesetzgebungsgewalt und entzog dem Parlament seine Macht — die formale Grundlage der NS-Diktatur.",
        difficulty = 3,
        funFact = "Nur die SPD stimmte geschlossen gegen das Ermächtigungsgesetz. SPD-Fraktionschef Otto Wels hielt dabei eine mutige Rede — trotz SA-Männer im Saal."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Dreikaiserbund' (1873/1881) in der Bismarck-Ära?",
        answerA = "Ein Militärbündnis zwischen Deutschland, Österreich-Ungarn und Russland gegen Frankreich",
        answerB = "Ein Konsultationsbündnis zwischen Deutschland, Österreich-Ungarn und Russland zur Wahrung der konservativen Ordnung",
        answerC = "Ein Verteidigungspakt der drei Kaiserreiche gegen britische Seemacht",
        answerD = "Eine dynastische Allianz der Hohenzollern, Habsburger und Romanows durch Heiratspolitik",
        correctAnswer = 1,
        explanation = "Der Dreikaiserbund war Bismarcks diplomatisches Instrument, um Russland, Österreich und Deutschland zusammenzuhalten und Frankreich diplomatisch zu isolieren.",
        difficulty = 3,
        funFact = "Der Bund scheiterte letztlich am russisch-österreichischen Gegensatz auf dem Balkan — Bismarck musste dann auf den Rückversicherungsvertrag mit Russland allein ausweichen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was waren die 'Nürnberger Rassengesetze' von 1935?",
        answerA = "Gesetze zur eugenischen Sterilisation von 'Erbkranken'",
        answerB = "Zwei Gesetze, die Juden die Staatsbürgerschaft entzogen und Ehen zwischen Juden und Nichtjuden verboten",
        answerC = "Gesetze zur Regelung der Arbeit ausländischer Arbeitskräfte im Dritten Reich",
        answerD = "Gesetze zum Schutz der deutschen Sprache und Kultur vor ausländischen Einflüssen",
        correctAnswer = 1,
        explanation = "Die Nürnberger Gesetze (1935) — Reichsbürgergesetz und Blutschutzgesetz — entzogen Juden die Staatsbürgerschaft und kriminalisierten Beziehungen zwischen Juden und Nichtjuden.",
        difficulty = 3,
        funFact = "Die Gesetze wurden auf dem Reichsparteitag in Nürnberg verkündet und bildeten die gesetzliche Grundlage für die systematische Ausgrenzung der jüdischen Bevölkerung."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Hoare-Laval-Affäre' von 1935?",
        answerA = "Ein britisch-französisches Geheimabkommen, das Mussolinis Abessinien-Invasion billigte und einen Skandal auslöste",
        answerB = "Ein Wirtschaftsabkommen zwischen Großbritannien und Frankreich über Kolonialgebiete",
        answerC = "Ein gescheiterter Versuch, Deutschland durch wirtschaftliche Zusammenarbeit zu besänftigen",
        answerD = "Eine britisch-französische Initiative zur Gründung eines gemeinsamen Kommandos",
        correctAnswer = 0,
        explanation = "Der Hoare-Laval-Plan (1935) sah vor, Mussolinis Invasion in Abessinien durch territoriale Zugeständnisse zu belohnen. Der Skandal zwang beide Außenminister zum Rücktritt.",
        difficulty = 3,
        funFact = "Die Affäre zeigte die Grenzen der kollektiven Sicherheit durch den Völkerbund und stärkte letztlich Hitlers Verachtung für die westlichen Demokratien."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Mao-Sprung nach vorn' in China (1958–62)?",
        answerA = "Maos militärische Kampagne zur Befreiung Taiwans",
        answerB = "Ein chinesisches Raketenprogramm als Reaktion auf den Sputnik-Schock",
        answerC = "Maos radikales Industrialisierungs- und Kollektivierungsprogramm, das eine verheerende Hungersnot verursachte",
        answerD = "Eine kulturelle Reformbewegung zur Modernisierung der chinesischen Gesellschaft",
        correctAnswer = 2,
        explanation = "Der 'Große Sprung nach vorn' war Maos Versuch, China durch erzwungene Kollektivierung und Industrialisierung zu modernisieren. Er führte zur größten Hungersnot des 20. Jahrhunderts mit 15–55 Millionen Toten.",
        difficulty = 3,
        funFact = "Viele Bauern wurden gezwungen, ihre Kochtöpfe einzuschmelzen für Stahl — während die Ernte auf den Feldern verfaulte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Entscheidungsschlacht' bei Stalingrad und welche Wendung brachte sie?",
        answerA = "Sie war die erste bedeutende deutsche Niederlage an der Ostfront und markierte die strategische Wende des Krieges",
        answerB = "Sie entschied den gesamten Zweiten Weltkrieg zu Gunsten der Alliierten durch den Eintritt der USA",
        answerC = "Sie war die größte Tankschlacht des Zweiten Weltkriegs und beendete den deutschen Vormarsch",
        answerD = "Sie führte direkt zur bedingungslosen Kapitulation Deutschlands",
        correctAnswer = 0,
        explanation = "Die Kesselschlacht von Stalingrad (1942/43) endete mit der Vernichtung der deutschen 6. Armee und gilt als Wendepunkt des Zweiten Weltkriegs an der Ostfront.",
        difficulty = 3,
        funFact = "Von den rund 300.000 eingeschlossenen deutschen Soldaten überlebten nur etwa 6.000 die sowjetische Kriegsgefangenschaft."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Konferenz von Jalta' 1945 und welche Auswirkungen hatte sie?",
        answerA = "Sie bestimmte die Kriegsstrategie der Alliierten für die letzten Kriegsmonate und legte die Nachkriegsordnung in Europa fest",
        answerB = "Sie beschloss die bedingungslose Kapitulation Deutschlands und die Einsetzung eines alliierten Kontrollrats",
        answerC = "Sie verhandelte die Friedensbedingungen für Japan nach dem Krieg",
        answerD = "Sie gründete die Vereinten Nationen und bestimmte deren Satzung",
        correctAnswer = 0,
        explanation = "Auf der Konferenz von Jalta (Februar 1945) einigten sich Roosevelt, Churchill und Stalin auf die Grundzüge der Nachkriegsordnung: Besatzungszonen, Reparationen und freie Wahlen in Osteuropa.",
        difficulty = 3,
        funFact = "Die Zusicherung freier Wahlen in Osteuropa wurde von Stalin niemals eingehalten — Jalta gilt für viele Osteuropäer als 'Verrat des Westens'."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Dreyfus-Affäre' in Frankreich und welche gesellschaftliche Bedeutung hatte sie?",
        answerA = "Eine politische Krise in Frankreich, ausgelöst durch die fälschliche Verurteilung eines jüdischen Offiziers wegen Spionage",
        answerB = "Ein Skandal um ein britisches Spionagenetz in der französischen Armee",
        answerC = "Ein Korruptionsskandal in der Dritten Französischen Republik",
        answerD = "Eine republikanische Revolte gegen den Einfluss der katholischen Kirche auf die Justiz",
        correctAnswer = 0,
        explanation = "Alfred Dreyfus, ein jüdischer Hauptmann, wurde 1894 fälschlich des Hochverrats verurteilt. Die Affäre spaltete Frankreich und wurde zum Symbol des modernen Antisemitismus.",
        difficulty = 3,
        funFact = "Der Journalist Émile Zola schrieb den offenen Brief 'J'accuse!' (Ich klage an!) und prangerte damit die antisemitische Justizmanipulation öffentlich an."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Zimmerwalder Bewegung' (1915)?",
        answerA = "Eine Schweizer Initiative zur Vermittlung im Ersten Weltkrieg",
        answerB = "Ein internationales Treffen sozialistischer Parteien, die den Krieg ablehnten und für Frieden eintraten",
        answerC = "Eine pazifistische Bewegung in der Schweiz gegen die Waffenexporte",
        answerD = "Eine Zusammenkunft europäischer Sozialisten zur Gründung der Komintern",
        correctAnswer = 1,
        explanation = "Die Zimmerwalder Konferenz (September 1915) versammelte internationale Sozialisten, die gegen den Krieg eintraten — im Gegensatz zur Mehrheit der sozialistischen Parteien, die ihre Regierungen unterstützten.",
        difficulty = 3,
        funFact = "Lenin formulierte auf der Konferenz die Parole 'Den imperialistischen Krieg in den Bürgerkrieg verwandeln' — ein Vorläufer der Russischen Revolution."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Juliusturm' im Deutschen Kaiserreich?",
        answerA = "Bismarcks geheimes Archiv für diplomatische Korrespondenz in der Wilhelmstraße",
        answerB = "Ein preußischer Kriegsschatz aus Reparationsgeldern von 1871, der als eiserne Reserve im Berliner Schloss aufbewahrt wurde",
        answerC = "Das Finanzministerium des Deutschen Reiches unter Wilhelm II.",
        answerD = "Ein geheimer Fonds zur Finanzierung der deutschen Flottenrüstung",
        correctAnswer = 1,
        explanation = "Der Juliusturm war ein Kriegsschatz von 120 Millionen Goldmark aus den französischen Kriegsentschädigungen von 1871, der im Berliner Juliusturm als Reserve für den nächsten Krieg gelagert wurde.",
        difficulty = 3,
        funFact = "Der Juliusturm wurde im Ersten Weltkrieg aufgebraucht — als Reserve hatte er nur 14 Tage Kriegskosten gedeckt."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter 'Volksdeutsche' im NS-Sprachgebrauch?",
        answerA = "Deutsche Staatsbürger ohne jüdische Vorfahren, die als 'arisch' galten",
        answerB = "Deutsch-ethnische Bevölkerungsgruppen im Ausland, die das NS-Regime als Teil des deutschen Volkes betrachtete",
        answerC = "NSDAP-Mitglieder aus dem einfachen Volk im Gegensatz zur Parteielite",
        answerD = "Deutschstämmige Einwanderer, die in der Weimarer Republik eingebürgert worden waren",
        correctAnswer = 1,
        explanation = "Als 'Volksdeutsche' bezeichnete das NS-Regime deutsch-ethnische Minderheiten in Osteuropa (Sudetenland, Polen, Baltikum etc.), die als Teil des deutschen 'Volkes' galten.",
        difficulty = 3,
        funFact = "Das Konzept der Volksdeutschen diente als Rechtfertigung für die Expansion ins östliche Europa und die Vertreibung anderer Bevölkerungsgruppen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die 'Bücherbücherverbrennung' vom Mai 1933 in Deutschland?",
        answerA = "Eine spontane Aktion von SA-Männern gegen jüdische Verlage",
        answerB = "Eine organisierte NS-Aktion, bei der Werke 'undeutscher' Autoren öffentlich verbrannt wurden",
        answerC = "Eine Kampagne zur Zensur der deutschen Presse durch das Propagandaministerium",
        answerD = "Die Schließung der Berliner Staatsbibliothek und Einzug 'entarteter' Bücher",
        correctAnswer = 1,
        explanation = "Am 10. Mai 1933 verbrannten NS-Studenten auf Universitätsplätzen in ganz Deutschland Bücher von als 'undeutsch' eingestuften Autoren, darunter Heine, Marx und Freud.",
        difficulty = 3,
        funFact = "Heinrich Heine schrieb bereits 1820 prophetisch: 'Dort wo man Bücher verbrennt, verbrennt man auch am Ende Menschen.'"
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der 'Imperialismus' im historischen Sinne des 19. und frühen 20. Jahrhunderts?",
        answerA = "Die Herrschaft des Kaisers über das deutsche Militär",
        answerB = "Das Streben europäischer Großmächte nach globaler Ausdehnung durch Kolonisierung und wirtschaftliche Kontrolle",
        answerC = "Die politische Bewegung zur Wiederherstellung der Monarchien nach Napoleon",
        answerD = "Der Einfluss des Papsttums auf die europäische Politik im Mittelalter",
        correctAnswer = 1,
        explanation = "Imperialismus bezeichnet die Ausdehnung staatlicher Macht durch politische, militärische und wirtschaftliche Kontrolle über andere Völker und Territorien, die im 19. Jahrhundert ihren Höhepunkt erreichte.",
        difficulty = 3,
        funFact = "Auf dem Höhepunkt des Imperialismus um 1914 kontrollierten europäische Mächte rund 85 % der Erdoberfläche."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war der '20. Juli 1944' in der deutschen Geschichte?",
        answerA = "Der Tag der alliierten Befreiung von Paris",
        answerB = "Das gescheiterte Attentat von Claus Schenk Graf von Stauffenberg auf Adolf Hitler",
        answerC = "Die erste alliierte Offensive nach der Normandie-Landung",
        answerD = "Die Unterzeichnung des Waffenstillstands in Rumänien",
        correctAnswer = 1,
        explanation = "Am 20. Juli 1944 detonierte eine von Stauffenberg platzierte Bombe im Führerhauptquartier Wolfsschanze, verfehlte Hitler jedoch schwer zu verletzen. Der Aufstand scheiterte.",
        difficulty = 3,
        funFact = "Über 200 Beteiligte und Verdächtige wurden hingerichtet — viele in besonders grausamer Weise. Das Attentat gilt heute als zentrales Symbol des deutschen Widerstands."
    )
)
