package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsExpert2(): List<Question> = listOf(

    // --- Holy Roman Empire: Golden Bull 1356 ---

    Question(
        categoryId = 3,
        questionText = "Wie viele Kurfürsten wurden in der Goldenen Bulle von 1356 als Königswähler des Heiligen Römischen Reiches festgelegt?",
        answerA = "Fünf",
        answerB = "Sechs",
        answerC = "Sieben",
        answerD = "Neun",
        correctAnswer = 2,
        explanation = "Die Goldene Bulle Kaiser Karls IV. legte sieben Kurfürsten fest: die Erzbischöfe von Mainz, Trier und Köln sowie die Könige von Böhmen, den Pfalzgrafen bei Rhein, den Herzog von Sachsen-Wittenberg und den Markgrafen von Brandenburg.",
        difficulty = 4,
        funFact = "Der Böhmenkönig hatte unter den weltlichen Kurfürsten den Ehrenvorrang und durfte als erster seine Stimme abgeben. Die Goldene Bulle galt als Grundgesetz des Reiches bis zu seiner Auflösung 1806."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt wurde in der Goldenen Bulle von 1356 als alleiniger Ort für Königswahlen festgeschrieben?",
        answerA = "Aachen",
        answerB = "Regensburg",
        answerC = "Frankfurt am Main",
        answerD = "Nürnberg",
        correctAnswer = 2,
        explanation = "Die Goldene Bulle bestimmte Frankfurt am Main als ausschließlichen Wahlort des deutschen Königs. Die Krönung hingegen sollte weiterhin in Aachen stattfinden, dem traditionellen Krönungsort der Karolinger.",
        difficulty = 4,
        funFact = "Nürnberg wurde in der Goldenen Bulle ebenfalls geehrt: Hier sollten die ersten Hoftage (Reichstage) eines neu gewählten Königs abgehalten werden. Frankfurt behielt seine Wahlrolle bis zur letzten Kaiserwahl 1792."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Privileg sicherte die Goldene Bulle den Kurfürsten in Bezug auf ihre eigenen Territorien zu?",
        answerA = "Steuerfreiheit gegenüber dem Kaiser",
        answerB = "Erstgeburtsrecht und Unteilbarkeit ihrer Kurwürde-Länder",
        answerC = "Das Recht, eigene Päpste zu wählen",
        answerD = "Militärische Unabhängigkeit ohne kaiserliche Genehmigung",
        correctAnswer = 1,
        explanation = "Die Goldene Bulle schrieb das Erstgeburtsrecht (Primogenitur) für Kurfürstentümer vor: Die Ländereien durften nicht geteilt werden, sondern gingen ungeteilt an den ältesten Sohn über. Dies stabilisierte die Kurwürde und verhinderte Erbstreitigkeiten.",
        difficulty = 4,
        funFact = "Vor der Goldenen Bulle wurde Reichsland oft unter Erben aufgeteilt, was zu zunehmender Zersplitterung führte. Das Primogeniturprinzip wurde für Kurfürsten verbindlich und setzte sich später in vielen deutschen Territorien durch."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Kaiser erließ die Goldene Bulle von 1356 und welcher Herrscherfamilie gehörte er an?",
        answerA = "Friedrich II. aus dem Haus der Staufer",
        answerB = "Karl IV. aus dem Haus Luxemburg",
        answerC = "Rudolf I. aus dem Haus Habsburg",
        answerD = "Ludwig IV. aus dem Haus Wittelsbach",
        correctAnswer = 1,
        explanation = "Karl IV. (1316–1378) aus dem Haus Luxemburg, König von Böhmen und ab 1355 Kaiser, erließ die Goldene Bulle auf den Reichstagen zu Nürnberg (Januar 1356) und Metz (Dezember 1356). Sie war sein wichtigstes Verfassungsdokument.",
        difficulty = 4,
        funFact = "Karl IV. regierte hauptsächlich von Prag aus und machte die Stadt zur prachtvollen Kaiserresidenz. Er gründete 1348 die Karls-Universität, eine der ältesten Universitäten Mitteleuropas, benannt nach ihm selbst."
    ),

    // --- Guelphs vs Ghibellines ---

    Question(
        categoryId = 3,
        questionText = "Wofür standen die Begriffe 'Guelfen' und 'Ghibellinen' im mittelalterlichen Italien?",
        answerA = "Guelfen = päpstliche Partei, Ghibellinen = kaiserliche Partei",
        answerB = "Guelfen = Normannen, Ghibellinen = Langobarden",
        answerC = "Guelfen = Händler, Ghibellinen = Adelige",
        answerD = "Guelfen = Flottentruppen, Ghibellinen = Landschlachttruppen",
        correctAnswer = 0,
        explanation = "Die Guelfen (Welfen) unterstützten den Papst, die Ghibellinen (abgeleitet von Waiblingen, einer Burg der Staufer) den Kaiser. Der Konflikt spaltete italienische Städte und Familien über Generationen und war eng mit dem Investiturstreit verknüpft.",
        difficulty = 4,
        funFact = "Dante Alighieri war ein begeisterter Ghibelline und musste deshalb aus Florenz, einer Guelfen-Stadt, ins Exil flüchten. Er schrieb die 'Göttliche Komödie' im Exil und starb 1321 in Ravenna, ohne je nach Florenz zurückzukehren."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche berühmte Stauferdynastie war der Ursprung des Ghibellinen-Namens?",
        answerA = "Die Salier mit ihrer Burg in Speyer",
        answerB = "Die Staufer mit ihrer Burg Waiblingen",
        answerC = "Die Welfen mit ihrer Burg Ravensburg",
        answerD = "Die Luxemburger mit ihrer Burg Vianden",
        correctAnswer = 1,
        explanation = "Der Name 'Ghibellinen' leitet sich von 'Waiblingen' ab, einer Stauferburg in Schwaben. Der Ruf 'Waiblingen!' diente als Schlachtruf der Staufer-Anhänger in Italien, was sich zu 'Ghibellini' italianisierte.",
        difficulty = 4,
        funFact = "Parallel dazu wurde 'Welf!' zum Schlachtruf der Gegenseite. In England wurde 'Guelph' zur Familienbezeichnung des Hauses Windsor: Das britische Königshaus hieß offiziell bis 1917 Sachsen-Coburg-Gotha-Windsor und zuvor Haus Hannover-Guelph."
    ),

    // --- Hanseatic League ---

    Question(
        categoryId = 3,
        questionText = "Welches war der wichtigste Handelsartikel der Hanse aus dem Ostseeraum, der den Grundstein für ihren Wohlstand legte?",
        answerA = "Bernstein und Pelze",
        answerB = "Getreide, Holz und Hering",
        answerC = "Seide und Gewürze",
        answerD = "Silber und Kupfer",
        correctAnswer = 1,
        explanation = "Getreide (vor allem Roggen und Weizen aus Preußen und Polen), Holz (für Schiffbau und Konstruktion) und Hering (aus dem Öresund, besonders die Scania-Märkte) bildeten das Rückgrat des hanseatischen Osthandels.",
        difficulty = 4,
        funFact = "Der Skagenheringsfang war so lukrativ, dass die Hanse dafür die 'Schonenmärkte' in Schonen (heute Südschweden) kontrollierte. Im 15. Jahrhundert kollabierte der Heringshandel als die Heringschwärme plötzlich aus der Ostsee verschwanden – möglicherweise durch klimatische Veränderungen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß das exklusive Handelsprivileg, das hanseatische Kaufleute in England besaßen und ihnen den Handel mit bestimmten Waren gestattete?",
        answerA = "Jus primae noctis",
        answerB = "Carta Mercatoria",
        answerC = "Stalhof-Privileg",
        answerD = "Handelskompagniegeset",
        correctAnswer = 2,
        explanation = "Der Stalhof (Steelyard) in London war die hanseatische Kontoranlage und das Zentrum hanseatischer Handelsprivilegien in England. Die Hanse genoss dort weitreichende Zollbefreiungen und Handelsrechte, die englische Kaufleute nicht hatten.",
        difficulty = 4,
        funFact = "Das Stalhof-Privileg war so wertvoll, dass die englische Krone und einheimische Kaufleute immer wieder versuchten, es einzuschränken. König Eduard IV. bestätigte die Rechte 1474 im Utrechter Frieden, aber Heinrich VIII. hob sie schließlich im 16. Jahrhundert auf."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche vier Hauptkontore unterhielt die Hanse außerhalb ihres Kerngebietes?",
        answerA = "Antwerpen, Paris, Genf, Venedig",
        answerB = "London (Stalhof), Brügge, Bergen, Nowgorod",
        answerC = "Kopenhagen, Stockholm, Oslo, Helsinki",
        answerD = "Riga, Tallinn, Danzig, Königsberg",
        correctAnswer = 1,
        explanation = "Die vier großen Außenhandelsposten (Kontore) der Hanse waren: das Stalhof in London, die Brügger Niederlassung (Flandern), das Bryggen-Kontor in Bergen (Norwegen) und das Peterhof-Kontor in Nowgorod (Russland).",
        difficulty = 4,
        funFact = "Das Bryggen-Kontor in Bergen war berühmt für seinen strikten Codex: Kaufleute durften nicht heiraten, kein eigenes Feuer unterhalten und mussten in den Kontorgebäuden wohnen. Die Bryggen-Häuser stehen noch heute als UNESCO-Welterbe in Bergen."
    ),

    // --- Medieval Siege Warfare ---

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter einem 'Trebuchet' im mittelalterlichen Belagerungskrieg, und wie funktionierte es?",
        answerA = "Ein Rammhebel, der durch menschliche Kraft bedient wurde",
        answerB = "Eine Wurfmaschine mit Gegengewicht, die schwere Steine warf",
        answerC = "Ein auf Rädern montierter Belagerungsturm",
        answerD = "Ein unterirdischer Tunnel zum Untergraben von Mauern",
        correctAnswer = 1,
        explanation = "Das Trebuchet nutzte ein schweres Gegengewicht (bis zu 10 Tonnen), um einen langen Arm mit einer Schleuder an der Spitze anzuheben und damit Steine von 50–150 kg über 300 Meter weit zu schleudern. Es war die wirkungsvollste Belagerungsmaschine des Mittelalters.",
        difficulty = 4,
        funFact = "Beim Trebuchet konnte man auch biologische Kriegsführung einsetzen: 1346 schleuderten die Mongolen bei der Belagerung von Kaffa (Krim) Leichen von Pestopfern über die Stadtmauern – eine der frühesten dokumentierten Fälle von Biowaffen-Einsatz, der möglicherweise die Pest nach Europa brachte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Verteidigungsarchitektur entwickelten mittelalterliche Burgenbauer, um Belagerungsrammen zu neutralisieren?",
        answerA = "Höhere Türme, die Rammen aus der Schussweite hielten",
        answerB = "Abschrägte Mauerfüße (Böschungen/Talus), die Rammen abweisen",
        answerC = "Breite Wassergräben ohne Brücken",
        answerD = "Holzpalisaden vor Steinmauern",
        correctAnswer = 1,
        explanation = "Abgeschrägte Sockelverkleidungen (Böschungen oder Talus) wurden an Mauerfüßen angebracht, um Rammböcke abzulenken, herabgeworfene Steine abprallen zu lassen und Untergrabearbeiten zu erschweren. Zudem machten sie den Maueraufstieg für Sturmleitern schwieriger.",
        difficulty = 4,
        funFact = "Die Kreuzfahrer übernahmen viele arabische und byzantinische Befestigungstechniken und bauten damit im Heiligen Land imposante Burgen wie die Krak des Chevaliers (Syrien), die als eine der best erhaltenen Kreuzfahrerburgen gilt."
    ),

    // --- Longbow vs Crossbow ---

    Question(
        categoryId = 3,
        questionText = "Welcher militärische Vorteil sprach für das Langbogen gegenüber der Armbrust im mittelalterlichen Fernkampf?",
        answerA = "Höhere Durchschlagskraft bei schwerer Rüstung",
        answerB = "Deutlich höhere Feuerrate (bis zu 12 Pfeile pro Minute vs. 2–4 bei Armbrust)",
        answerC = "Geringere Ausbildungszeit (2 Wochen statt 10 Jahre)",
        answerD = "Bessere Präzision auf 400 Meter Entfernung",
        correctAnswer = 1,
        explanation = "Ein geübter Langbogenschütze konnte 10–12 Pfeile pro Minute abschießen, während ein Armbrustschütze nur 2–4 Bolzen laden und abfeuern konnte. Diese Feuerrate war entscheidend in Schlachten wie Crécy (1346) und Azincourt (1415).",
        difficulty = 4,
        funFact = "Englische Langbogenschützen wurden von Kindheit an trainiert: Archäologische Analysen ihrer Skelette zeigen deutliche Knochendeformationen des linken Arms und der Wirbelsäule durch jahrelanges intensives Training. Der nötige Zugwiderstand betrug bis zu 70 kg."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher Schlachtsituation hatte die Armbrust einen klaren Vorteil gegenüber dem Langbogen?",
        answerA = "Bei Kavallerieangriffen auf offenes Feld",
        answerB = "Bei der Stadtverteidigung und in engen Schussscharten",
        answerC = "Beim Beschuss beweglicher Infanterie",
        answerD = "Im Regen, da die Sehne weniger empfindlich war",
        correctAnswer = 1,
        explanation = "In Stadtverteidigungen und durch enge Schussscharten war die kompakte Armbrust ideal: Sie erforderte weniger Platz zum Spannen und Abfeuern, war einfacher bei langsamer Zielauswahl einzusetzen und ließ sich auch von unerfahreneren Schützen bedienen.",
        difficulty = 4,
        funFact = "Das Laterankonzil von 1139 versuchte, die Armbrust als 'zu tödlich und unchristlich für den Einsatz gegen Christen' zu verbieten – allerdings nur im Kampf gegen andere Christen, nicht gegen Heiden und Sarazenen. Das Verbot wurde weitgehend ignoriert."
    ),

    // --- Black Death Consequences ---

    Question(
        categoryId = 3,
        questionText = "Welche wirtschaftliche Folge hatte der Schwarze Tod (1347–1353) langfristig für die überlebenden Bauern in Westeuropa?",
        answerA = "Verschärfung der Leibeigenschaft durch Mangel an Arbeitskraft",
        answerB = "Stärkung der Verhandlungsposition der Arbeit durch Arbeitskräftemangel und höhere Löhne",
        answerC = "Rückfall in die Subsistenzwirtschaft durch Zusammenbruch der Städte",
        answerD = "Verstaatlichung von Ländereien durch die Krone",
        correctAnswer = 1,
        explanation = "Der Tod von 30–50% der Bevölkerung erzeugte einen extremen Arbeitskräftemangel. Überlebende Bauern konnten höhere Löhne und bessere Pachtverhältnisse aushandeln. In Westeuropa beschleunigte dies das Ende der Leibeigenschaft, während Osteuropa gegenläufige Tendenzen zeigte.",
        difficulty = 4,
        funFact = "In England reagierte die Krone mit dem 'Statute of Laborers' (1351), das Löhne auf Vorpest-Niveau einfrieren sollte. Die Maßnahme scheiterte weitgehend und trug zur sozialen Spannung bei, die 1381 im Bauernaufstand unter Wat Tyler gipfelte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche religiöse Reaktion auf die Pest führte zu Pogromen gegen Juden in Deutschland und der Schweiz?",
        answerA = "Die Nikolausbewegung, die Juden für Teufelspakte verantwortlich machte",
        answerB = "Die Geißlerbewegung und der Vorwurf der Brunnenvergiftung",
        answerC = "Die Hussitenbewegung, die Juden als Verbündete des Papstes betrachtete",
        answerD = "Der Erste Kreuzzug, der sich ursprünglich gegen Juden richtete",
        correctAnswer = 1,
        explanation = "Die Geißler (Flagellanten) zogen durch Europa und geißelten sich zur Buße. Gleichzeitig verbreitete sich der Vorwurf der Brunnenvergiftung: Juden wurden beschuldigt, die Pest durch vergiftete Brunnen verursacht zu haben, was zu massiven Pogromen führte.",
        difficulty = 4,
        funFact = "Papst Clemens VI. erließ 1348 eine Bulle, die Juden als Opfer und nicht als Verursacher der Pest bezeichnete – sie starben genauso an der Krankheit. Dennoch wurden 1348/49 zahlreiche jüdische Gemeinden in Speyer, Straßburg, Basel und Köln vernichtet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie veränderte der Schwarze Tod die Theologie und Kunst des späten Mittelalters?",
        answerA = "Er löste eine Rückkehr zur antiken griechisch-römischen Mythologie aus",
        answerB = "Er begünstigte Totentanz-Motivik, Memento-mori-Kunst und mystische Frömmigkeitsbewegungen",
        answerC = "Er führte zur Aufgabe religiöser Kunst zugunsten rein weltlicher Themen",
        answerD = "Er löste eine Hinwendung zum Pietismus und privater Schriftlektüre aus",
        correctAnswer = 1,
        explanation = "Der Schwarze Tod prägte die spätmittelalterliche Kultur tiefgreifend: Totentänze (Danse Macabre) zeigten den Tod als Gleichmacher aller Stände. Mystiker wie Heinrich Seuse und Johannes Tauler betonten innige persönliche Gottesbeziehung. Memento-mori-Motive ('Bedenke, dass du sterben musst') durchdrangen Kunst und Architektur.",
        difficulty = 4,
        funFact = "Der 'Codex Manesse' und andere spätmittelalterliche Handschriften zeigen den kulturellen Wandel: Darstellungen von Skeletten, verfallenden Leibern und dem Tod als Tanzpartner wurden zu einem zentralen Bildmotiv, das zuvor kaum vorkam."
    ),

    // --- Medieval Universities ---

    Question(
        categoryId = 3,
        questionText = "Welche war die erste Universität Europas, die als solche anerkannt wird, und wofür war sie berühmt?",
        answerA = "Paris (Sorbonne), berühmt für Theologie",
        answerB = "Bologna, berühmt für Rechtswissenschaften",
        answerC = "Oxford, berühmt für Naturphilosophie",
        answerD = "Salerno, berühmt für Medizin",
        correctAnswer = 1,
        explanation = "Die Universität Bologna (gegründet ca. 1088) gilt als älteste Universität Europas. Sie war primär für ihr Rechtsstudium berühmt und zog Studenten aus ganz Europa an, die das römische und kirchliche Recht studierten.",
        difficulty = 4,
        funFact = "In Bologna begannen die Studenten selbst (nicht die Professoren) die Universität zu organisieren: Sie gründeten 'nationes' (Studentenvereinigungen nach Herkunftsländern) und konnten Professoren sogar mit Geldstrafen belegen, wenn diese zu spät kamen oder zu früh aufhörten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bedeutete der Begriff 'Trivium' im mittelalterlichen Universitätscurriculum?",
        answerA = "Die drei praktischen Künste: Medizin, Recht, Theologie",
        answerB = "Die drei Sprachkünste: Grammatik, Rhetorik, Dialektik (Logik)",
        answerC = "Die drei mathematischen Künste: Arithmetik, Geometrie, Astronomie",
        answerD = "Die drei philosophischen Schulen: Platonismus, Aristotelismus, Stoizismus",
        correctAnswer = 1,
        explanation = "Das Trivium (lateinisch: drei Wege) bildete die erste Stufe der mittelalterlichen Artes liberales und umfasste Grammatik (korrekte Sprache), Rhetorik (überzeugende Rede) und Dialektik/Logik (schlüssiges Denken). Es war Voraussetzung für das höhere Studium im Quadrivium.",
        difficulty = 4,
        funFact = "Das Wort 'trivial' (=unbedeutend, gewöhnlich) stammt vom Trivium: Es war die Grundausbildung, die jeder kennen musste, bevor er sich dem Quadrivium (Arithmetik, Geometrie, Musik, Astronomie) widmen durfte – also tatsächlich die 'gewöhnliche' Basisbildung."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche philosophische Methode dominierte die mittelalterlichen Universitäten und wie wurde sie bezeichnet?",
        answerA = "Empirismus: Wissen aus direkter Sinnesbeobachtung",
        answerB = "Scholastik: Vernunftgeleitete Auseinandersetzung mit Autoritäten und Glaubensfragen",
        answerC = "Humanismus: Fokus auf antike Texte und menschliche Würde",
        answerD = "Nominalismus: Ablehnung universeller Begriffe zugunsten von Einzeldingen",
        correctAnswer = 1,
        explanation = "Die Scholastik (von lateinisch 'schola' = Schule) versuchte, christlichen Glauben mit aristotelischer Vernunft zu harmonisieren. Typisch war die disputatio: eine These wurde aus Autoritäten belegt, Gegenargumente formuliert und dann systematisch widerlegt.",
        difficulty = 4,
        funFact = "Thomas von Aquin (1225–1274) vollendete die Scholastik mit seiner 'Summa Theologiae', die alle theologischen Fragen in systematischer Form behandelt. Sie ist so umfangreich, dass keine vollständige deutsche Übersetzung existiert."
    ),

    // --- Ottoman Empire: Devshirme System ---

    Question(
        categoryId = 3,
        questionText = "Was war das osmanische Devshirme-System und welchen Zweck erfüllte es?",
        answerA = "Ein Steuersystem für nicht-muslimische Untertanen",
        answerB = "Ein Rekrutierungssystem, das christliche Knaben als Staatssklaven und Soldaten ausbildete",
        answerC = "Ein Gelehrten-Stipendium für die osmanischen Madresen",
        answerD = "Ein Handelsabkommen zwischen dem Sultan und europäischen Kaufleuten",
        correctAnswer = 1,
        explanation = "Das Devshirme ('Knabenlese') war ein System, bei dem osmanische Beamte regelmäßig christliche Knaben aus dem Balkan und anderen Gebieten rekrutierten, zum Islam konvertierten und zu Janitscharen, Hofbeamten oder hohen Staatsministern ausbildeten.",
        difficulty = 4,
        funFact = "Paradoxerweise ermöglichte das Devshirme außergewöhnlichen sozialen Aufstieg: Ibrahim Pascha, der mächtigste Großwesir unter Süleiman dem Prächtigen, war ein griechisches Devshirme-Kind, das zum de-facto Herrscher des Reiches aufstieg, bevor er auf Befehl des Sultans hingerichtet wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß das osmanische System der autonomen Verwaltung nichtmuslimischer Religionsgemeinschaften?",
        answerA = "Dhimmi-Ordnung",
        answerB = "Millet-System",
        answerC = "Tanzimât-Ordnung",
        answerD = "Waqf-Verwaltung",
        correctAnswer = 1,
        explanation = "Das Millet-System ('Volk' oder 'Nation' auf Arabisch/Türkisch) erlaubte nichtmuslimischen Gemeinschaften (griechisch-orthodoxe Christen, armenische Christen, Juden) eigene Rechtssprechung in religiösen und persönlichen Angelegenheiten unter ihren eigenen religiösen Führern.",
        difficulty = 4,
        funFact = "Der ökumenische Patriarch von Konstantinopel wurde durch das Millet-System zum politischen Oberhaupt aller griechisch-orthodoxen Christen im Osmanischen Reich – eine unerwartete Machtstellung, die die griechische Kirche unter osmanischer Herrschaft erhielt."
    ),

    // --- Ottoman Empire: Janissaries ---

    Question(
        categoryId = 3,
        questionText = "Wann und wie wurden die Janitscharen als osmanische Elitetruppe gegründet?",
        answerA = "Unter Osman I. ca. 1299, aus freiwilligen muslimischen Kämpfern",
        answerB = "Unter Orhan I. oder Murad I. ca. 1363–1383, aus Devshirme-Rekruten",
        answerC = "Unter Mehmed II. nach der Eroberung Konstantinopels 1453",
        answerD = "Unter Süleiman dem Prächtigen ca. 1520 als Elitegarde",
        correctAnswer = 1,
        explanation = "Die Janitscharen (türkisch 'Yeni Çeri' = neue Truppe) wurden unter Orhan I. oder Murad I. im späten 14. Jahrhundert gegründet. Sie rekrutierten sich aus dem Devshirme-System und waren die erste stehende Berufsarmee des Osmanischen Reiches.",
        difficulty = 4,
        funFact = "Im Gegensatz zu europäischen Armeen erhielten Janitscharen regulären Sold, Pension und staatliche Unterkunft. Sie waren dem Sultan direkt unterstellt, nicht dem Adel, was sie zu einem mächtigen Instrument gegen feudale Unabhängigkeit machte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Ayin-i Cedid' und warum scheiterte dieser Reformversuch der osmanischen Armee?",
        answerA = "Eine neue Schießpulver-Technologie, die von Europa abgelehnt wurde",
        answerB = "Selim III.'s neue westlich ausgebildete Armee, die von den Janitscharen gewaltsam aufgelöst wurde",
        answerC = "Eine neue Steuerpolitik, die den Janitscharen ihre Privilegien entzog",
        answerD = "Ein Geheimdienstnetzwerk, das von Janitscharen infiltriert und sabotiert wurde",
        correctAnswer = 1,
        explanation = "Sultan Selim III. (reg. 1789–1807) gründete das 'Nizam-ı Cedid' (Neue Ordnung), eine modern ausgebildete Armee nach europäischem Vorbild. Die Janitscharen, die ihre Privilegien bedroht sahen, revoltierten 1807, stürzten Selim und ließen ihn ermorden.",
        difficulty = 4,
        funFact = "Erst Sultan Mahmud II. löste das Problem grundlegend: Er ließ 1826 die Janitscharen-Kaserne in Konstantinopel niederbrennen und massakrierte tausende Janitscharen. Dieses Ereignis, bekannt als 'Auspicious Incident', beendete die 500-jährige Geschichte der Janitscharen."
    ),

    // --- Mughal Empire ---

    Question(
        categoryId = 3,
        questionText = "Welcher Mogul-Kaiser führte die religiös-inklusive Politik des 'Din-i-Ilahi' ein, und was bedeutete sie?",
        answerA = "Akbar der Große, der eine synkretistische Hofkult-Bewegung schuf",
        answerB = "Aurangzeb, der einen islamischen Staat nach Scharia-Recht errichtete",
        answerC = "Babur, der den Hinduismus mit dem Islam zu verschmelzen versuchte",
        answerD = "Shah Jahan, der buddhistische Elemente in den Staatsislam integrierte",
        correctAnswer = 0,
        explanation = "Akbar (reg. 1556–1605) gründete den 'Din-i-Ilahi' (Göttliche Religion), eine synkretistische Hofbewegung, die Elemente aus Islam, Hinduismus, Zoroastrismus, Christentum und Jainismus vereinte. Sie blieb auf den Hof beschränkt und verschwand nach Akbars Tod.",
        difficulty = 4,
        funFact = "Akbars Heiratspolitik war ebenso inklusiv wie sein Glaube: Er heiratete rajputische Hinduprinzessinnen und erlaubte ihnen, ihren Glauben zu behalten. Seine Mutter war Perserin, seine wichtigste Frau Hinduistin. Diese Pragmatik sicherte die Loyalität der Hindu-Mehrheitsbevölkerung."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche wirtschaftliche Besonderheit kennzeichnete das Mansabdar-System des Mogulreiches?",
        answerA = "Grundbesitz wurde erblich an den Adel vergeben, vergleichbar dem europäischen Feudalismus",
        answerB = "Adlige erhielten kein Land, sondern Einkünfte aus bestimmten Regionen (Jagir) als temporäre Zuweisung",
        answerC = "Der Kaiser besteuerte direkt alle Bauern und teilte Einnahmen prozentual auf",
        answerD = "Adelstitel wurden durch Kauf erworben und an Nachkommen vererbt",
        correctAnswer = 1,
        explanation = "Das Mansabdar-System vergab keine erblichen Lehen, sondern temporäre Einkommensrechte (Jagir) über bestimmte Gebiete. Mansabdare (Amtsinhaber) hatten eine Doppelzahl (zat und sawar), die ihre persönliche Stellung und ihre Kavallerieverpflichtung bezeichnete.",
        difficulty = 4,
        funFact = "Das Jagir-System verhinderte die Entstehung erblicher Feudalherren, da Jagirs bei Tod oder Ungnade des Kaisers zurückfielen. Allerdings führte dies zu chronischer Instabilität: Mansabdare hatten wenig Anreiz, ihre Regionen langfristig zu entwickeln, sondern extrahierten möglichst viel Ertrag."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Mogul-Kaiser baute den Taj Mahal, und für wen ließ er ihn errichten?",
        answerA = "Akbar, für seine Mutter Hamida Banu Begum",
        answerB = "Shah Jahan, für seine verstorbene Lieblingsfrau Mumtaz Mahal",
        answerC = "Jahangir, für seine persische Gemahlin Nur Jahan",
        answerD = "Aurangzeb, für seinen Vater Shah Jahan",
        correctAnswer = 1,
        explanation = "Shah Jahan (reg. 1628–1658) ließ den Taj Mahal in Agra zwischen 1631 und 1653 als Mausoleum für seine dritte und Lieblingsfrau Mumtaz Mahal errichten, die 1631 bei der Geburt ihres 14. Kindes starb.",
        difficulty = 4,
        funFact = "Shah Jahan selbst wurde nach seinem Tod 1666 neben Mumtaz im Taj Mahal beigesetzt – allerdings nicht geplant. Sein Sohn Aurangzeb hatte ihn jahrelang in der Agra-Festung gefangen gehalten. Der asymmetrische Sarkophag Shah Jahans im Inneren stört angeblich die perfekte Symmetrie des Gebäudes."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Politik Aurangzebs (1658–1707) beschleunigte den Verfall des Mogulreiches?",
        answerA = "Die Wiedereinsetzung der Jizyah-Steuer auf Nichtmuslime und rigide sunnitische Religionspolitik",
        answerB = "Die Aufgabe des Mansabdar-Systems zugunsten erblicher Feudalherrschaft",
        answerC = "Die Verlegung der Hauptstadt von Agra nach Lahore",
        answerD = "Die Ausweitung der Kriege gegen das Osmanische Reich",
        correctAnswer = 0,
        explanation = "Aurangzeb reaktivierte die Jizyah-Kopfsteuer auf Hindus (die Akbar abgeschafft hatte), zerstörte Hindu-Tempel und verfolgte eine streng sunnitisch-islamische Innenpolitik. Dies entfremdete die hinduistische Mehrheit und die Rajputen und stärkte Aufstandsbewegungen wie die Marathen.",
        difficulty = 4,
        funFact = "Aurangzeb regierte länger als jeder andere Mogul (49 Jahre), verbrachte aber die letzten 26 Jahre seines Lebens im Krieg gegen die Marathen im Deccan. Trotz militärischer Erschöpfung und riesiger Kosten konnte er die Marathen nicht unterwerfen – das Deccan-Abenteuer ruinierte das Reich finanziell."
    ),

    // --- Ming Dynasty and Zheng He ---

    Question(
        categoryId = 3,
        questionText = "Welcher Ming-Kaiser ordnete die großen Schatzreisen unter Zheng He an, und welche politische Motivation hatte er dabei?",
        answerA = "Kaiser Hongwu (Zhu Yuanzhang), um Handelsrouten nach Indien zu öffnen",
        answerB = "Kaiser Yongle (Zhu Di), um seine Legitimität zu festigen und Tribute einzusammeln",
        answerC = "Kaiser Xuande, um den verlorenen Ming-Prinzen zu suchen",
        answerD = "Kaiser Jiajing, um mit dem Osmanischen Reich ein Bündnis gegen die Portugiesen zu schließen",
        correctAnswer = 1,
        explanation = "Kaiser Yongle (reg. 1402–1424) hatte den Thron durch Usurpation erlangt und benötigte Legitimation. Die Schatzflotten sollten ausländische Tribute einsammeln, China als mächtigstes Reich demonstrieren und internationale Anerkennung der Yongle-Herrschaft fördern.",
        difficulty = 4,
        funFact = "Yongle verlegte auch die Hauptstadt von Nanjing nach Peking und begann mit dem Bau der Verbotenen Stadt. Alle diese Projekte – Flotten, Hauptstadtverlegung, Verbotene Stadt – kosteten enorme Ressourcen und wurden später von konfuzianischen Beamten als verschwenderisch kritisiert."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Schatzreisen unternahm Zheng He insgesamt, und wohin reichte die weiteste?",
        answerA = "Drei Reisen bis nach Japan und Korea",
        answerB = "Sieben Reisen, die weiteste bis zur Ostküste Afrikas",
        answerC = "Zehn Reisen, die weiteste bis nach Arabien",
        answerD = "Fünf Reisen, bis nach Indien und Ceylon",
        correctAnswer = 1,
        explanation = "Zheng He unternahm zwischen 1405 und 1433 sieben Reisen. Die weitesten Fahrten führten entlang der arabischen Halbinsel und zur ostafrikanischen Küste (heutiges Kenia und Somalia), wo er exotische Tiere wie Giraffen mitbrachte.",
        difficulty = 4,
        funFact = "Eine Giraffe, die Zheng He aus Afrika mitbrachte, wurde als lebende Legende an den Kaiserhof gebracht. Die Ming-Gelehrten identifizierten sie als 'Qilin' (ein mythisches chinesisches Fabeltier), was als göttliches Zeichen für Yongles gerechte Herrschaft gewertet wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Größenangabe macht die historische Überlieferung über die Flaggschiffe ('Schatzschiffe') von Zheng Hes Flotte?",
        answerA = "Ca. 30 Meter Länge – vergleichbar mit europäischen Karavellen",
        answerB = "Ca. 120–135 Meter Länge – weitaus größer als europäische Zeitgenossen",
        answerC = "Ca. 60–70 Meter Länge – vergleichbar mit venezianischen Galeeren",
        answerD = "Ca. 200 Meter Länge – die größten Holzschiffe der Geschichte",
        correctAnswer = 1,
        explanation = "Chinesische Quellen beschreiben die Baochuan (Schatzschiffe) als bis zu 44 Zhang (ca. 120–135 Meter) lang – deutlich größer als Kolumbus' Santa María (ca. 20 Meter). Archäologische Funde eines Ruders von 11 Metern Länge unterstützen diese Angaben.",
        difficulty = 4,
        funFact = "Die genaue Größe der Schatzschiffe ist unter Historikern umstritten. Moderne Nachbauten und Berechnungen zeigen, dass 120-Meter-Holzschiffe mit damaliger Technologie tatsächlich gebaut werden konnten, aber schwerlich hochseetüchtig gewesen wären. Einige Historiker vermuten, dass die Quellenangaben übertrieben sind."
    ),

    Question(
        categoryId = 3,
        questionText = "Warum stellte China seine Schatzreisen nach 1433 ein und vernichtete sogar die Schifffahrtspläne?",
        answerA = "Wegen verheerender Seestürme, die mehrere Flotten vernichteten",
        answerB = "Konfuzianische Beamte setzten Handelsverbote durch, da Seefahrt als unzivilisiert galt, und ein Haizitribut entfiel",
        answerC = "Portugal blockierte die Handelswege im Indischen Ozean militärisch",
        answerD = "Die Mongoleninvasion zwang China zur Konzentration auf Landverteidigung",
        correctAnswer = 1,
        explanation = "Nach Yongles Tod gewannen konfuzianische Literatenbeamte Einfluss, die maritime Unternehmungen als kostspielig, gefährlich und kulturell minderwertig betrachteten. 1430er erlassene Verbote untersagten den Schiffbau großer Seeschiffe. Pläne wurden vernichtet, um die Verbote zu sichern.",
        difficulty = 4,
        funFact = "Der Historiker Gavin Menzies argumentierte kontrovers in '1421: The Year China Discovered America', dass Zheng Hes Flotten Amerika vor Kolumbus erreichten. Die These wird von der Fachwissenschaft überwiegend abgelehnt, hat aber populäre Aufmerksamkeit erregt."
    ),

    // --- More Holy Roman Empire ---

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter dem 'Interregnum' (1254–1273) im Heiligen Römischen Reich?",
        answerA = "Eine Periode päpstlichen Regentschaft nach dem Tod des Kaisers",
        answerB = "Eine Phase ohne anerkannten deutschen König nach dem Aussterben der Staufer",
        answerC = "Der Zeitraum zwischen der Kaiserkrönung und der Königswahl",
        answerD = "Die Regentschaft einer Kaisermutter für ihren minderjährigen Sohn",
        correctAnswer = 1,
        explanation = "Nach dem Aussterben der Staufer mit Konradin (1268 hingerichtet) gab es keine starke Zentralmacht im Reich. Zwei Gegenkönige – Richard von Cornwall und Alfons von Kastilien – wurden gewählt, ohne sich je wirklich durchzusetzen. Die Fürstengewalten festigten ihre Unabhängigkeit.",
        difficulty = 4,
        funFact = "Das Interregnum endete mit der Wahl Rudolfs I. von Habsburg 1273, der eine neue Kaiserdynastie begründete. Die Habsburger sollten – mit wenigen Unterbrechungen – bis 1806 die Kaiser des Heiligen Römischen Reiches stellen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte der Begriff 'Kurfürst' ('Churfürst') im mittelalterlichen Deutschland?",
        answerA = "Ein Fürst, der das Recht hatte, den deutschen König zu wählen",
        answerB = "Ein Fürst, der direkt dem Kaiser unterstand ohne Zwischenherren",
        answerC = "Ein Fürst, der das Recht hatte, Zölle an Flussübergängen zu erheben",
        answerD = "Ein Fürst, der die kaiserliche Armee im Feld führte",
        correctAnswer = 0,
        explanation = "Der Begriff 'Kurfürst' (von 'küren' = wählen) bezeichnete jene Fürsten, die das Recht hatten, den deutschen König/römischen Kaiser zu wählen. Die Goldene Bulle von 1356 legte die sieben Kurfürsten und das Wahlverfahren verbindlich fest.",
        difficulty = 4,
        funFact = "Das Kurkolleg versammelte sich bei Königsleichen und Vakanzen im Frankfurter Dom. Die drei geistlichen Kurfürsten (Mainz, Trier, Köln) hatten dabei besondere Ehrenrechte als Hofkanzler, Erzkanzler für Italien und Archchanzler für Burgund."
    ),

    // --- Additional Medieval Politics ---

    Question(
        categoryId = 3,
        questionText = "Welche Krise löste 1378 das 'Große Abendländische Schisma' aus, das zu drei gleichzeitigen Päpsten führte?",
        answerA = "Der Streit zwischen Papst Urban VI. und den Kardinälen, die ihn als instabil betrachteten",
        answerB = "Die Übersiedlung des Papsttums nach Avignon",
        answerC = "Die Wahl eines deutschen Papstes gegen den Willen der Kurie",
        answerD = "Der Investiturstreit zwischen Kaiser und Papst",
        correctAnswer = 0,
        explanation = "Nach der Wahl Urbans VI. (1378) behaupteten die Kardinäle, die Wahl unter Druck vollzogen zu haben, und wählten einen Gegenpapst (Clemens VII. in Avignon). Das Konzil von Pisa (1409) machte es noch schlimmer und wählte einen dritten Papst – bis zum Konzil von Konstanz (1414–1418).",
        difficulty = 4,
        funFact = "Das Schisma war katastrophal für die Kirchenautorität: Je nach Nationalität unterstützten Länder verschiedene Päpste (England und Deutschland: Rom; Frankreich und Schottland: Avignon). Der hl. Vinzenz Ferrer unterstützte den Avignoner Papst, die hl. Katharina von Siena den römischen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte der 'Westfälische Frieden' von 1648 für das Verhältnis von Landesfürsten und Kaiser im Heiligen Römischen Reich?",
        answerA = "Er stärkte die kaiserliche Zentralmacht auf Kosten der Fürstensouveränität",
        answerB = "Er kodifizierte die territoriale Souveränität der Fürsten und das Recht auf eigene Bündnispolitik",
        answerC = "Er übertrug die Kaiserwürde an das Haus Bourbon",
        answerD = "Er schuf eine einheitliche Rechtsprechung für das gesamte Reich",
        correctAnswer = 1,
        explanation = "Der Westfälische Frieden 1648 anerkannte die 'Landeshoheit' der Fürsten und ihr Recht auf eigene Bündnisse (jus foederis), solange diese nicht gegen Kaiser und Reich gerichtet waren. Dies verfestigte die föderale Natur des Reiches und begrenzte die kaiserliche Macht dauerhaft.",
        difficulty = 4,
        funFact = "Der Westfälische Frieden gilt als Geburtsstunde des modernen Staatensystems: Er verankerte das Prinzip der staatlichen Souveränität und der Nichteinmischung in innere Angelegenheiten. Selbst heute wird bei internationalen Konflikten noch auf 'westfälische Prinzipien' verwiesen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Funktion hatte der 'Reichstag' im Spätmittelalterlichen Heiligen Römischen Reich?",
        answerA = "Ein dauerhaftes Gesetzgebungsorgan mit festem Sitzungskalender",
        answerB = "Eine unregelmäßige Versammlung von Fürsten und Prälaten, die der Kaiser bei Bedarf einberief",
        answerC = "Ein Gericht, das kaiserliche Dekrete auf Rechtmäßigkeit prüfte",
        answerD = "Eine Verwaltungsbehörde für die Reichssteuern",
        correctAnswer = 1,
        explanation = "Der mittelalterliche Reichstag war keine permanente Institution, sondern wurde vom Kaiser einberufen, wenn er Beratung oder Zustimmung für wichtige Entscheidungen wie Kriege, Bündnisse oder Steuern benötigte. Erst der 'Ewige Reichstag' in Regensburg (ab 1663) tagte dauerhaft.",
        difficulty = 4,
        funFact = "Der Reichstag gliederte sich in drei Kollegien: das Kurfürstenkolleg, das Fürstenkolleg (Geistliche und Weltliche Fürsten) und das Städtekolleg. Die Reichsstädte bekamen erst 1648 volles Stimmrecht – zuvor hatten sie nur eine beratende Stimme."
    ),

    // --- More Black Death ---

    Question(
        categoryId = 3,
        questionText = "Welche Auswirkung hatte der Schwarze Tod auf die mittelalterliche Agrarwirtschaft in England konkret?",
        answerA = "Umwandlung von Ackerland in Schafweiden, da Wolle mit weniger Arbeitskraft produziert werden konnte",
        answerB = "Rückkehr zur Dreifelderwirtschaft nach Aufgabe verbesserter Rotationssysteme",
        answerC = "Masseneinwanderung von Bauern aus Osteuropa zur Auffüllung der Arbeitslücken",
        answerD = "Einführung der Dampfmaschine für landwirtschaftliche Aufgaben",
        correctAnswer = 0,
        explanation = "Der Arbeitskräftemangel nach der Pest machte es für englische Grundbesitzer attraktiver, Ackerland in Schafweiden umzuwandeln: Schafe erforderten weit weniger Arbeit als Getreideanbau. Dies beschleunigte die Enclosure-Bewegung und den englischen Wollhandel.",
        difficulty = 4,
        funFact = "Die englische Wollproduktion und der Wolltuchhandel wurden zur Grundlage des englischen Reichtums im späten Mittelalter. Das Lordkanzler-Symbol im britischen Oberhaus ist ein 'Woolsack' (Wollsack) – ein Symbol für den Reichtum, den die Wolle England brachte."
    ),

    // --- Additional Hanseatic League ---

    Question(
        categoryId = 3,
        questionText = "Welcher hanseatische Krieg gegen Dänemark (1368–1370) endete mit dem für die Hanse günstigsten Friedensvertrag?",
        answerA = "Krieg von 1360, beendet durch den Frieden von Stralsund",
        answerB = "Krieg gegen König Waldemar IV., beendet durch den Frieden von Stralsund (1370)",
        answerC = "Hanseatisch-dänischer Krieg 1426–1435, beendet durch den Frieden von Vordingborg",
        answerD = "Hanseatisch-niederländischer Krieg 1438–1441, beendet durch den Frieden von Kopenhagen",
        correctAnswer = 1,
        explanation = "Im Krieg gegen König Waldemar IV. Atterdag errangen die Hansestädte einen entscheidenden Sieg. Der Frieden von Stralsund (1370) sicherte der Hanse Zollfreiheit und die Verpfändung mehrerer Scania-Festungen sowie ein Vetorecht bei dänischen Königswahlen.",
        difficulty = 4,
        funFact = "Der Friede von Stralsund 1370 gilt als Höhepunkt hanseatischer Macht. Das Vetorecht bei dänischen Königswahlen war ohne Beispiel in der mittelalterlichen Geschichte: Eine Kaufmannsvereinigung ohne eigenes Territorium konnte Einfluss auf die Thronfolge eines souveränen Königreiches nehmen."
    ),

    // --- More Ming Dynasty ---

    Question(
        categoryId = 3,
        questionText = "Welche Institution übte in der Ming-Dynastie eine dominante administrative Kontrolle über die Bürokratie aus und wie hieß sie?",
        answerA = "Der Drachenthron-Rat mit sechs Kronprinzen",
        answerB = "Das Grand Secretariat (Nei-ko), das kaiserliche Reskripte bearbeitete",
        answerC = "Das Hanlin-Akademie-Kollegium als supreme Gesetzgebungsbehörde",
        answerD = "Die Censorate (Ducha Yuan) als oberste Regierungsbehörde",
        correctAnswer = 1,
        explanation = "Das Grand Secretariat (Nei-ke/Neige) entwickelte sich unter den Ming zu einem de-facto Kabinett. Obwohl formell nur beratendes Gremium, bearbeiteten die Großsekretäre alle kaiserlichen Dokumente und beeinflussten so die Politik erheblich, besonders wenn Kaiser wenig Interesse an Regierungsgeschäften zeigten.",
        difficulty = 4,
        funFact = "Zhang Juzheng (1525–1582), Großsekretär unter dem jungen Kaiser Wanli, war de-facto Regent des Reiches für über zehn Jahre. Er führte umfassende Reformen durch, die das Reich stabilisierten – nach seinem Tod revidierte Kaiser Wanli viele seiner Maßnahmen aus persönlichem Groll."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das 'Haijin' der Ming-Dynastie und welche Ausnahme wurde unter Zheng He gemacht?",
        answerA = "Ein Verbot privater Seefahrt und Außenhandelsbeschränkungen",
        answerB = "Ein staatliches Handelsmonopol für Porzellan und Seide",
        answerC = "Eine Steuer auf ausländische Schiffe in chinesischen Häfen",
        answerD = "Ein Verbot der Ansiedlung von Ausländern in Küstenstädten",
        correctAnswer = 0,
        explanation = "Das Haijin (Seefahrtsverbot) beschränkte private maritime Aktivitäten schwer und verbot privaten Außenhandel. Zheng Hes staatlich organisierte Schatzreisen waren eine explizite Ausnahme: staatlicher Handel im Namen des Kaisers war erlaubt, privater nicht.",
        difficulty = 4,
        funFact = "Das Haijin sollte eigentlich Piraterie und die Unterstützung von Ming-Gegnern auf See verhindern. Es hatte aber den paradoxen Effekt, legalen Handel zu kriminalisieren und damit tatsächlich Schmuggelhandel und Piraterie zu fördern, da die Nachfrage nach ausländischen Gütern nicht verschwand."
    ),

    // --- More Ottoman Empire ---

    Question(
        categoryId = 3,
        questionText = "Welches osmanische Verwaltungssystem regelte die Verteilung von Militärpfründen (Timar) an Kavalleristen?",
        answerA = "Das Sipahi-Timar-System: Kavalleristen erhielten Einkünfte aus Land als Bezahlung für Militärdienst",
        answerB = "Das Devshirme-System: Christliche Rekruten erhielten Landbesitz nach Bekehrung",
        answerC = "Das Millet-System: Religionsgemeinschaften verwalteten ihr Land autonom",
        answerD = "Das Waqf-System: Religiöse Stiftungen finanzierten die Kavallerietruppen",
        correctAnswer = 0,
        explanation = "Das Timar-System vergab Einkünfte aus bestimmten Ländereien (Timar) an Sipahi-Kavalleristen als Entlohnung für ihren Militärdienst. Die Sipahi mussten im Kriegsfall selbst ausgerüstet erscheinen und je nach Ertrag zusätzliche Reiter stellen.",
        difficulty = 4,
        funFact = "Das Timar-System ähnelte dem europäischen Lehnsystem, hatte aber einen entscheidenden Unterschied: Timars waren nicht erblich, sondern mussten bei jedem neuen Sultanat neu verliehen werden. Damit verhinderten die Osmanen die Entstehung eines erblichen Adels, der kaiserliche Macht einschränken könnte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Titel trug der Führer der osmanischen Janitscharen und wie hieß seine Einheit offiziell?",
        answerA = "Der Ağa der Janitscharen, die Einheit hieß 'Yeniçeri Ocağı' (Janitscharen-Herd)",
        answerB = "Der Pascha der Janitscharen, die Einheit hieß 'Akinci Alayı' (Reiter-Regiment)",
        answerC = "Der Bey der Sipahi, die Einheit hieß 'Kapikulu Süvarisi' (Torsklave-Kavallerie)",
        answerD = "Der Dey der Kapikulari, die Einheit hieß 'Devşirme Bölüğü' (Knabenlese-Kompanie)",
        correctAnswer = 0,
        explanation = "Der 'Yeniçeri Ağası' (Janitscharenaga) kommandierte das 'Yeniçeri Ocağı' (Janitscharenkorps, wörtlich: Herd). Das Bild des Feuers/Herds war zentral für die Janitscharenidentität – ihre Kessel galten als heiliges Symbol der Gemeinschaft.",
        difficulty = 4,
        funFact = "Als Zeichen des Protests kehrten Janitscharen ihre Reisekessel um – dieses Ritual war das sichtbarste Symbol einer Revolte. Der Kaiser war gezwungen, mit rangniedrigeren Vertretern zu verhandeln, da die umgekippten Kessel bedeuteten: 'Wir verweigern das Brot des Sultans.'"
    ),

    // --- Medieval Siege Additional ---

    Question(
        categoryId = 3,
        questionText = "Was war eine 'Sape' (Sapping) im mittelalterlichen Belagerungswesen?",
        answerA = "Das Untergraben von Mauerfundamenten durch unterirdische Tunnel",
        answerB = "Das Besetzen von Geländepunkten rund um die Burg zur Aushungerung",
        answerC = "Das Beschießen von Mauern mit Brandgeschossen",
        answerD = "Das Aufstellen mobiler Schutzschilder für anrückende Infanterie",
        correctAnswer = 0,
        explanation = "Beim Sapping gruben Ingenieure (Sappeure) Stollen unter die Mauerfundamente einer Burg. Die Tunnel wurden mit Holzstützen gesichert und dann mit brennbarem Material gefüllt. Wenn das Holz verbrannte, kollabierte der Tunnel und riss die Mauer mit sich.",
        difficulty = 4,
        funFact = "Gegen Sapping entwickelten Verteidiger den 'Gegenmine': Schüsseln mit Wasser auf dem Boden, um Vibrationen durch Grabarbeiten zu erspüren. Sie gruben dann eigene Gegentunnel, um die feindlichen Stollen zu entdecken und die Sappeure zu bekämpfen."
    ),

    // --- Longbow Additional ---

    Question(
        categoryId = 3,
        questionText = "Welches Material wurde für traditionelle englische Langbogen verwendet und warum war es besonders geeignet?",
        answerA = "Eichenholz, da es sehr hart und schwer ist",
        answerB = "Eibenholz (Taxus), das Zug- und Druckeigenschaften in Splint und Kernholz vereint",
        answerC = "Eschenholz, das leicht und flexibel ist",
        answerD = "Birkenholz, da es gleichmäßig wächst und nicht splittert",
        correctAnswer = 1,
        explanation = "Eibenholz (Taxus baccata) ist ideal für Langbogen: Das weiche Splintholz (außen) arbeitet auf Zug, das harte rötliche Kernholz (innen) auf Druck. Kein anderes Holz vereint diese Eigenschaften so optimal. Englische Bögen wurden möglichst so geschnitten, dass beide Holzarten enthalten sind.",
        difficulty = 4,
        funFact = "Eibe war in England so begehrt, dass sie fast ausgerottet wurde. Die englische Krone importierte Eibenholz in Massen aus Spanien, Portugal und dem deutschsprachigen Raum. Letzteres erklärt den deutschen Namen 'Eibe' – er kommt vom keltisch-germanischen 'iwa', was auch im englischen 'yew' steckt."
    ),

    // --- More Medieval Universities ---

    Question(
        categoryId = 3,
        questionText = "Was war der akademische Titel 'Doctor' im Mittelalter und wie unterschied er sich vom heutigen Gebrauch?",
        answerA = "Ein Titel ausschließlich für Mediziner, entspricht dem heutigen Dr. med.",
        answerB = "Ein Lehrtitel (Doktor = Lehrer), der die Befugnis zu unterrichten verlieh",
        answerC = "Eine Ehrenbezeichnung ohne Prüfungsvoraussetzung, vom Papst verliehen",
        answerD = "Ein Forschungstitel, der eigenständige wissenschaftliche Beiträge nachwies",
        correctAnswer = 1,
        explanation = "Das mittelalterliche 'Doctor' (vom lateinischen 'docere' = lehren) war primär eine Lehrerlaubnis, nicht ein Forschungstitel. Die Promotion bedeutete: Man durfte nun selbst an einer Universität unterrichten. Der Schwerpunkt lag auf der Vermittlung und Auslegung bestehenden Wissens.",
        difficulty = 4,
        funFact = "Im Mittelalter war die akademische Hierarchie: Baccalaureus (Grundabschluss), Magister/Lizentiat (fortgeschrittenes Studium) und Doctor (Lehrlizenz). Das 'Bachelor'-Konzept, das heute weltweit verwendet wird, stammt direkt aus dem mittelalterlichen Baccalaureat."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Universität gehört zu den ältesten im deutschsprachigen Raum und wann wurde sie gegründet?",
        answerA = "Universität Wien, gegründet 1365",
        answerB = "Universität Heidelberg, gegründet 1386",
        answerC = "Universität Köln, gegründet 1388",
        answerD = "Universität Prag (Karls-Universität), gegründet 1348",
        correctAnswer = 3,
        explanation = "Die Karls-Universität Prag wurde 1348 von Kaiser Karl IV. gegründet und ist die älteste Universität in Mitteleuropa sowie die älteste im heutigen deutschsprachigen Bereich. Sie war auch die erste Universität östlich des Rheins und nördlich der Alpen.",
        difficulty = 4,
        funFact = "Die Prager Universität war zunächst nach dem Pariser Modell mit vier Nationen organisiert (böhmisch, bayerisch, sächsisch, polnisch). Der Streit über die Stimmgewichtung zwischen Deutschen und Böhmen führte 1409 zur Abreise der deutschen Dozenten, die daraufhin die Universität Leipzig gründeten."
    ),

    // --- More Golden Bull and Elections ---

    Question(
        categoryId = 3,
        questionText = "Was regelte die Goldene Bulle bezüglich der 'Erzämter' der weltlichen Kurfürsten?",
        answerA = "Jeder weltliche Kurfürst hatte ein erbliches Hofamt bei der Kaiserkrönung",
        answerB = "Die Kurfürsten wechselten sich jährlich im Amt des Reichsverwalters ab",
        answerC = "Die Erzämter entschieden, wer bei Thronvakanz kommissarisch regierte",
        answerD = "Nur die drei geistlichen Kurfürsten hatten Erzämter, nicht die weltlichen",
        correctAnswer = 0,
        explanation = "Die Goldene Bulle verlieh jedem weltlichen Kurfürsten ein erbliches Erzamt bei Feierlichkeiten: Böhmen = Erzschenk, Pfalzgraf = Erztruchsess, Sachsen = Erzmarschall, Brandenburg = Erzkämmerer. Diese zeremoniellen Ämter symbolisierten ihren Sonderstatus im Reich.",
        difficulty = 4,
        funFact = "Die Erzämter waren primär zeremoniell und wurden nur bei Krönungen und großen Reichsfesten aktiv ausgeübt. Interessanterweise behielt der Böhmenkönig als Erzschenk seine Sonderstellung auch als nicht-deutschem Fürsten, was seine Integration in das Reich symbolisierte."
    ),

    // --- Plague additional question ---

    Question(
        categoryId = 3,
        questionText = "Welcher Erreger verursacht die Pest, und wie wird er hauptsächlich übertragen?",
        answerA = "Yersinia pestis, übertragen durch Flöhe auf infizierten Ratten (und anderen Nagetieren)",
        answerB = "Vibrio cholerae, übertragen durch verunreinigtes Wasser",
        answerC = "Variola virus, übertragen durch Tröpfcheninfektion",
        answerD = "Mycobacterium tuberculosis, übertragen durch Atemluft",
        correctAnswer = 0,
        explanation = "Das Bakterium Yersinia pestis wird primär durch Rattenflöhe übertragen, die zuerst infizierte Ratten und dann Menschen befallen. Die Beulenpest (Bubonenpest) entsteht durch Flohbiss; die noch tödlichere Lungenpest wird von Mensch zu Mensch durch Atemluft übertragen.",
        difficulty = 4,
        funFact = "Das Bakterium Yersinia pestis wurde erst 1894 von Alexandre Yersin und Kitasato Shibasaburo unabhängig voneinander entdeckt – über 550 Jahre nach dem Schwarzen Tod. Die Pest ist bis heute nicht ausgerottet: In den USA gibt es jährlich etwa 5–15 gemeldete Fälle, hauptsächlich im Südwesten."
    ),

    // --- Additional Mughal ---

    Question(
        categoryId = 3,
        questionText = "Wie hieß das Zentralverwaltungssystem des Mogulreiches für Finanzen und was war der Diwan?",
        answerA = "Der Diwan war der oberste Finanzminister, zuständig für Steuereinnahmen und Reichshaushalt",
        answerB = "Der Diwan war ein Beratungsgremium adliger Familien ohne Finanzkompetenz",
        answerC = "Der Diwan war ein religiöses Gericht, das Zakat-Zahlungen verwaltete",
        answerD = "Der Diwan war die Bezeichnung für das Militärbudget des Sultans",
        correctAnswer = 0,
        explanation = "Der Diwan (persisch für 'Büro/Register') war das Finanzministerium des Mogulreiches. Der Diwan-i-Ala (Oberster Diwan) war einer der mächtigsten Beamten des Reiches, zuständig für Steuerveranlagung, Kassen und Verwaltung der Staatseinnahmen.",
        difficulty = 4,
        funFact = "Die Mogul verwendeten das 'Ain-i-Akbari'-System zur Katastervermessung unter Akbar: Birbal und Raja Todar Mal entwickelten eine detaillierte Bodenbewertung und fixierte Steuersätze, die dem Staat stabile Einnahmen sicherten und als eines der ausgereiftesten Steuersysteme der Vormoderne gilt."
    ),

    // --- More Hanseatic League ---

    Question(
        categoryId = 3,
        questionText = "Welche Stadt gilt als inoffizielle 'Hauptstadt' der Hanse und warum?",
        answerA = "Hamburg, da es den größten Hafen besaß",
        answerB = "Lübeck, da hier die meisten Hansetage stattfanden und die Stadt führend in der Organisation war",
        answerC = "Bremen, da es als erstes Stadt die Hanse gründete",
        answerD = "Danzig, da es den lukrativsten Getreidehandel kontrollierte",
        correctAnswer = 1,
        explanation = "Lübeck war die dominierende Stadt der Hanse und Austragungsort der meisten Hansetage (Versammlungen). Ihre strategische Lage an der Trave, kurz von der Ostsee entfernt, machte sie zum natürlichen Zentrum des Ostseehandels.",
        difficulty = 4,
        funFact = "Der Lübecker Bürgermeister Heinrich Brömse prägte im 15. Jahrhundert die Formel 'Lübisches Recht', das in über 100 Städten als Stadtrecht eingeführt wurde. Städte von Reval bis Riga operierten nach Lübecker Rechtsvorstellungen – ein hanseatisches Rechtsnetzwerk avant la lettre."
    ),

)
