package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun logicQuestionsExpert6(): List<Question> = listOf(

    // Cryptography, Information Theory, Codes & Ciphers — 50 expert questions (internet-verified)

    Question(
        categoryId = 12,
        questionText = "Die Caesar-Chiffre verschiebt jeden Buchstaben um einen festen Wert. Wie viele verschiedene Schlüssel gibt es für das lateinische Alphabet mit 26 Buchstaben, wenn der Schlüssel 0 als 'keine Verschlüsselung' gilt?",
        answerA = "26",
        answerB = "25",
        answerC = "13",
        answerD = "52",
        correctAnswer = 1,
        explanation = "Es gibt 25 sinnvolle Schlüssel (Verschiebung 1–25), da Schlüssel 0 den Text unverändert lässt und Schlüssel 26 identisch mit Schlüssel 0 ist. Die Caesar-Chiffre ist daher durch einfaches Ausprobieren (Brute-Force) trivial knackbar.",
        difficulty = 4,
        funFact = "ROT13, das im Internet verbreitete Verfahren zum Verbergen von Spoilern, ist ein Caesar-Schlüssel mit Verschiebung 13 — und gleichzeitig sein eigenes Gegenstück: Zweimal ROT13 ergibt den Originaltext."
    ),

    Question(
        categoryId = 12,
        questionText = "Die Häufigkeitsanalyse ist eine Methode zum Knacken von Substitutionschiffren. Welcher Buchstabe tritt im deutschen Text statistisch am häufigsten auf?",
        answerA = "A",
        answerB = "I",
        answerC = "E",
        answerD = "N",
        correctAnswer = 2,
        explanation = "Im Deutschen ist E mit etwa 17,4% der häufigste Buchstabe, gefolgt von N (~9,8%) und I (~7,5%). Die Häufigkeitsanalyse wurde bereits im 9. Jahrhundert vom arabischen Gelehrten Al-Kindi beschrieben und machte monoalphabetische Chiffren unsicher.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Die Vigenère-Chiffre gilt lange als 'le chiffre indéchiffrable'. Welche Methode knackte sie erstmals systematisch, indem sie Schlüssellängen aus Wiederholungsabständen bestimmte?",
        answerA = "Babbage-Analyse",
        answerB = "Kasiski-Test",
        answerC = "Index of Coincidence",
        answerD = "Friedman-Test",
        correctAnswer = 1,
        explanation = "Friedrich Wilhelm Kasiski veröffentlichte 1863 seinen Test: Wiederholt sich eine Zeichenfolge im Geheimtext, ist der Abstand oft ein Vielfaches der Schlüssellänge. Daraus lässt sich die Schlüssellänge bestimmen, dann wird jeder Teiltext separat per Häufigkeitsanalyse geknackt.",
        difficulty = 4,
        funFact = "Charles Babbage hatte dieselbe Methode bereits um 1854 entdeckt, aber nie veröffentlicht — möglicherweise auf Bitten des britischen Geheimdienstes, der sie im Krimkrieg nutzen wollte."
    ),

    Question(
        categoryId = 12,
        questionText = "Claude Shannon definierte 1948 die Entropie einer diskreten Zufallsvariable X mit n Symbolen. Welcher Ausdruck gibt die Shannon-Entropie H(X) korrekt wieder?",
        answerA = "H(X) = Σ p(xᵢ) · log₂(p(xᵢ))",
        answerB = "H(X) = −Σ p(xᵢ) · log₂(p(xᵢ))",
        answerC = "H(X) = Σ p(xᵢ)²",
        answerD = "H(X) = log₂(n)",
        correctAnswer = 1,
        explanation = "H(X) = −Σ p(xᵢ)·log₂(p(xᵢ)) — das negative Vorzeichen sorgt dafür, dass H immer ≥ 0 ist, da log₂(p) ≤ 0 für Wahrscheinlichkeiten p ∈ (0,1]. Die Entropie misst den mittleren Informationsgehalt einer Nachricht in Bit.",
        difficulty = 4,
        funFact = "Shannon soll den Begriff 'Entropie' auf Vorschlag von John von Neumann gewählt haben, weil 'niemand weiß, was Entropie wirklich ist — damit gewinnst du jeden Streit'."
    ),

    Question(
        categoryId = 12,
        questionText = "Wann ist die Shannon-Entropie einer Zufallsvariable mit n gleichwahrscheinlichen Symbolen maximal?",
        answerA = "Wenn ein Symbol mit Wahrscheinlichkeit 1 auftritt",
        answerB = "Wenn alle n Symbole gleich wahrscheinlich sind: H = log₂(n) Bit",
        answerC = "Wenn zwei Symbole mit je 50% auftreten",
        answerD = "Wenn die Varianz der Verteilung minimal ist",
        correctAnswer = 1,
        explanation = "Die Entropie ist genau dann maximal, wenn alle n Symbole gleichwahrscheinlich sind (Gleichverteilung). Dann gilt H = log₂(n). Bei einer Münze (n=2): H = 1 Bit. Bei einem Würfel (n=6): H ≈ 2,58 Bit.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Die Enigma-Maschine der deutschen Wehrmacht hatte im Standardmodell drei Rotoren. Was machte sie kryptographisch stärker als eine einfache Substitutionschiffre?",
        answerA = "Sie verwendete ein 32-Bit-Schlüssel",
        answerB = "Jeder Tastendruck drehte mindestens einen Rotor, sodass jeder Buchstabe anders verschlüsselt wurde (polyalphabetisch)",
        answerC = "Sie nutzte asymmetrische Kryptographie",
        answerD = "Die Rotoren wurden täglich per Satellitensignal synchronisiert",
        correctAnswer = 1,
        explanation = "Die Enigma ist eine polyalphabetische Substitutionschiffre: Nach jedem Tastendruck dreht sich mindestens Rotor 1, was das Substitutionsalphabet ändert. Mit 3 Rotoren, Steckerbrett und Reflektor gab es theoretisch über 158 Quintillionen Einstellungen.",
        difficulty = 4,
        funFact = "Alan Turing und das Team in Bletchley Park nutzten bekannte Klartextfragmente ('Cribs') wie stereotypische Wetterberichte und Begrüßungsformeln, um die Enigma täglich zu knacken — ein früher Known-Plaintext-Angriff."
    ),

    Question(
        categoryId = 12,
        questionText = "Das One-Time-Pad (Vernam-Chiffre) ist nachweislich informationstheoretisch sicher. Welche Bedingung muss der Schlüssel zwingend erfüllen?",
        answerA = "Er muss mindestens 128 Bit lang sein",
        answerB = "Er muss zufällig, mindestens so lang wie der Klartext und niemals wiederverwendet werden",
        answerC = "Er muss eine Primzahl als Basis verwenden",
        answerD = "Er muss mit dem Empfänger über RSA ausgetauscht werden",
        correctAnswer = 1,
        explanation = "Shannon bewies 1949: Das OTP ist perfekt sicher, wenn der Schlüssel (1) echt zufällig, (2) mindestens so lang wie der Klartext und (3) nur einmal verwendet wird. Bei Wiederverwendung kann ein Angreifer per XOR beide Geheimtexte kombinieren und statistische Angriffe starten.",
        difficulty = 4,
        funFact = "Der 'Rote Telefon'-Hotline zwischen Washington und Moskau nutzte während des Kalten Krieges tatsächlich One-Time-Pads — die Schlüsselgurte wurden monatlich per diplomatischem Kurier ausgetauscht."
    ),

    Question(
        categoryId = 12,
        questionText = "Der Diffie-Hellman-Schlüsselaustausch (1976) ermöglicht es zwei Parteien, über einen unsicheren Kanal einen gemeinsamen Schlüssel zu vereinbaren. Auf welchem mathematischen Problem basiert seine Sicherheit?",
        answerA = "Faktorisierungsproblem großer Zahlen",
        answerB = "Diskretes Logarithmusproblem in endlichen Gruppen",
        answerC = "Traveling-Salesman-Problem",
        answerD = "NP-Vollständigkeit der Primzahltestung",
        correctAnswer = 1,
        explanation = "DH basiert auf dem Diskreten Logarithmusproblem: Gegeben gˣ mod p, ist es rechnerisch schwer, x zu finden. Alice sendet gᵃ mod p, Bob gᵇ mod p — beide berechnen dann gᵃᵇ mod p als gemeinsamen Schlüssel, ohne a oder b preiszugeben.",
        difficulty = 4,
        funFact = "Whitfield Diffie und Martin Hellman veröffentlichten ihre Idee 1976, ohne einen konkreten Verschlüsselungsalgorithmus zu haben — sie lieferten nur das Schlüsselaustauschrproblem. RSA folgte ein Jahr später."
    ),

    Question(
        categoryId = 12,
        questionText = "RSA verwendet zwei Schlüssel. Was ist die mathematische Grundlage der RSA-Verschlüsselung?",
        answerA = "Die Schwierigkeit, große zusammengesetzte Zahlen in ihre Primfaktoren zu zerlegen",
        answerB = "Das diskrete Logarithmusproblem",
        answerC = "Die NP-Schwierigkeit des Rucksackproblems",
        answerD = "Die Kollisionsresistenz von Hash-Funktionen",
        correctAnswer = 0,
        explanation = "RSA (Rivest, Shamir, Adleman, 1977): n = p·q (zwei große Primzahlen). Der öffentliche Schlüssel ist (n, e), der private (n, d) mit e·d ≡ 1 (mod φ(n)). Wer n nicht faktorisieren kann, kann d nicht aus e berechnen — und damit nicht entschlüsseln.",
        difficulty = 4,
        funFact = "Das britische GCHQ hatte RSA bereits 1973 intern entdeckt (Clifford Cocks), hielt es aber jahrzehntelang geheim. Erst 1997 wurde dies öffentlich — eine der folgenreichsten geheimgehaltenen Entdeckungen der Mathematik."
    ),

    Question(
        categoryId = 12,
        questionText = "Was versteht man unter einem 'Known-Plaintext-Angriff' (KPA) in der Kryptanalyse?",
        answerA = "Der Angreifer kennt nur den Geheimtext und versucht den Klartext zu rekonstruieren",
        answerB = "Der Angreifer kennt Paare aus Klartext und zugehörigem Geheimtext und nutzt diese, um den Schlüssel zu rekonstruieren",
        answerC = "Der Angreifer kann beliebige Klartexte verschlüsseln lassen",
        answerD = "Der Angreifer bricht physisch in das Rechenzentrum ein",
        correctAnswer = 1,
        explanation = "Beim KPA kennt der Angreifer einige (Klartext, Geheimtext)-Paare. Dies war entscheidend beim Knacken der Enigma: Turing nutzte bekannte Formulierungen wie 'Keine besonderen Ereignisse' als Cribs. Ein stärkeres Modell ist der Chosen-Plaintext-Angriff (CPA), bei dem der Angreifer selbst Klartexte zur Verschlüsselung einreichen kann.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welcher Algorithmus gewann 2001 den AES-Wettbewerb des NIST und wurde zum weltweiten Standard für symmetrische Verschlüsselung?",
        answerA = "Serpent",
        answerB = "Twofish",
        answerC = "Rijndael",
        answerD = "MARS",
        correctAnswer = 2,
        explanation = "Rijndael, entwickelt von den belgischen Kryptographen Joan Daemen und Vincent Rijmen, gewann den AES-Wettbewerb. Es wird heute als AES (Advanced Encryption Standard) mit Schlüssellängen von 128, 192 oder 256 Bit eingesetzt — in HTTPS, WPA2, Festplattenverschlüsselung und unzähligen anderen Systemen.",
        difficulty = 4,
        funFact = "Der Name 'Rijndael' ist eine Kombination der Nachnamen der Erfinder: Rijmen + Daemen. Der Wettbewerb lief von 1997 bis 2001 — fünf Finalisten traten an, Rijndael überzeugte durch Effizienz auf kleiner Hardware wie Smartcards."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Index of Coincidence (IC) in der Kryptanalyse, und wie hilft er bei der Bestimmung der Schlüssellänge einer Vigenère-Chiffre?",
        answerA = "Die mittlere Häufigkeit des häufigsten Buchstabens",
        answerB = "Die Wahrscheinlichkeit, dass zwei zufällig gezogene Buchstaben aus einem Text identisch sind — natürlicher Klartext hat IC ≈ 0,065 (Deutsch), Zufallstext ≈ 0,038",
        answerC = "Die Anzahl der verschiedenen Buchstaben dividiert durch 26",
        answerD = "Ein Maß für die Entropie des Schlüssels",
        correctAnswer = 1,
        explanation = "William Friedman definierte 1920 den IC. Für eine Vigenère-Chiffre mit Schlüssellänge k teilt man den Text in k Gruppen auf. Wenn der IC jeder Gruppe nahe 0,065 liegt, hat man die richtige Schlüssellänge gefunden — denn dann entspricht jede Gruppe einer Caesar-Verschlüsselung.",
        difficulty = 4,
        funFact = "William Friedman gilt als Vater der modernen Kryptanalyse. Er und seine Frau Elizebeth Friedman knackten während der Prohibition Geheimcodes von Rum-Schmugglern — Elizebeth war dabei oft erfolgreicher als er."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Birthday-Angriff' in der Kryptographie und warum ist er für Hash-Funktionen relevant?",
        answerA = "Ein Angriff, der am Geburtstag des Systemadministrators durchgeführt wird",
        answerB = "Ein Angriff, der das Geburtstagsparadoxon ausnutzt: Bei einer n-Bit-Hash-Funktion braucht man nur etwa 2^(n/2) zufällige Eingaben, um eine Kollision zu finden",
        answerC = "Eine Methode, bei der Geburtstagsdaten als Schlüssel verwendet werden",
        answerD = "Ein Timing-Angriff auf Zufallszahlengeneratoren",
        correctAnswer = 1,
        explanation = "Das Geburtstagsparadoxon: In einer Gruppe von nur 23 Personen liegt die Wahrscheinlichkeit einer gemeinsamen Geburtstagsübereinstimmung bei >50%. Analog: Bei einer 128-Bit-Hash-Funktion kann ein Angreifer mit etwa 2^64 Berechnungen eine Kollision finden — deshalb brauchen sichere Hash-Funktionen heute mindestens 256 Bit.",
        difficulty = 4,
        funFact = "MD5 mit 128 Bit wurde 2004 von Xiaoyun Wang gebrochen — sie fand Kollisionen in Sekunden. SHA-1 (160 Bit) fiel 2017 durch den 'SHAttered'-Angriff von Google und CWI Amsterdam."
    ),

    Question(
        categoryId = 12,
        questionText = "Die Skytale ist eines der ältesten bekannten Verschlüsselungsgeräte, verwendet von den Spartanern im 5. Jahrhundert v. Chr. Welches Prinzip nutzt sie?",
        answerA = "Substitution — jeder Buchstabe wird durch einen anderen ersetzt",
        answerB = "Transposition — die Buchstaben werden nur umgeordnet, nicht ersetzt",
        answerC = "Steganographie — der Text wird im Trägermedium versteckt",
        answerD = "XOR-Verknüpfung mit einem Schlüsselband",
        correctAnswer = 1,
        explanation = "Bei der Skytale wird ein Pergamentstreifen schraubenförmig um einen Zylinder bestimmten Durchmessers gewickelt und dann quer beschrieben. Ohne den gleichen Zylinder liest man nur einen sinnlosen Text — die Buchstaben sind lediglich umgeordnet (Transposition), nicht ersetzt.",
        difficulty = 4,
        funFact = "Plutarch berichtet, dass spartanische Feldherren mit der Skytale geheime Botschaften von Ephoren erhielten. Allerdings zweifeln Historiker heute daran, ob sie wirklich zur Verschlüsselung oder nur zur Authentifizierung genutzt wurde."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Perfect Forward Secrecy' (PFS) in modernen TLS-Verbindungen?",
        answerA = "Eine Methode, bei der Passwörter niemals im Klartext übertragen werden",
        answerB = "Die Eigenschaft, dass die Kompromittierung des langfristigen Serverschlüssels keine vergangenen Sitzungsschlüssel gefährdet, da für jede Sitzung ein neues ephemeres Schlüsselpaar erzeugt wird",
        answerC = "Eine Technik zur Vorwärtsfehlerkorrektur in Netzwerkprotokollen",
        answerD = "Das Prinzip, dass jede Nachricht ihren eigenen Schlüssel mitträgt",
        correctAnswer = 1,
        explanation = "PFS wird durch ephemere Diffie-Hellman-Schlüssel (DHE oder ECDHE) realisiert. Bei jeder TLS-Sitzung wird ein neues temporäres Schlüsselpaar erzeugt und danach gelöscht. Selbst wenn ein Angreifer später den privaten RSA-Schlüssel des Servers stiehlt, kann er aufgezeichnete vergangene Verbindungen nicht entschlüsseln.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Das Kerckhoffs'sche Prinzip (1883) ist ein Grundsatz moderner Kryptographie. Was besagt es?",
        answerA = "Ein Kryptosystem ist nur dann sicher, wenn der Algorithmus geheim gehalten wird",
        answerB = "Die Sicherheit eines Kryptosystems darf nur vom Schlüssel abhängen, nicht vom geheimgehaltenen Algorithmus",
        answerC = "Ein Schlüssel muss mindestens so lang sein wie der Klartext",
        answerD = "Kryptographische Algorithmen müssen von staatlichen Stellen zertifiziert werden",
        correctAnswer = 1,
        explanation = "Auguste Kerckhoffs formulierte: 'Il faut qu'il n'exige pas le secret, et qu'il puisse sans inconvénient tomber entre les mains de l'ennemi.' Der Algorithmus kann öffentlich sein — nur der Schlüssel muss geheim bleiben. Das ermöglicht öffentliche Überprüfung und erhöht das Vertrauen.",
        difficulty = 4,
        funFact = "Security through Obscurity — das Gegenteil des Kerckhoffs-Prinzips — gilt heute als schlechte Praxis. Die Enigma scheiterte auch, weil die Alliierten die Maschine erbeuteten und damit den 'geheimen' Algorithmus kannten."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Man-in-the-Middle-Angriff' (MITM) und welche Technologie schützt am effektivsten dagegen?",
        answerA = "Ein Angriff auf physische Kabel — geschützt durch Glasfaser",
        answerB = "Ein Angreifer schaltet sich zwischen zwei kommunizierende Parteien und gibt sich jeweils als die andere aus — PKI mit digitalen Zertifikaten und Certificate Authorities schützen dagegen",
        answerC = "Ein Brute-Force-Angriff auf Passwörter — abgewehrt durch lange Passwörter",
        answerD = "Eine Überlastung des Servers — verhindert durch Load Balancer",
        correctAnswer = 1,
        explanation = "Beim MITM-Angriff täuscht der Angreifer beiden Seiten vor, die jeweils andere zu sein. Public-Key-Infrastruktur (PKI) löst dies durch digitale Zertifikate: Eine vertrauenswürdige Certificate Authority (CA) bestätigt kryptographisch, dass ein öffentlicher Schlüssel wirklich zu einem bestimmten Server gehört.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was beschreibt das Shannon-Limit (Kanalkapazität) nach dem Shannon-Hartley-Theorem?",
        answerA = "Die maximale Schlüssellänge für symmetrische Verschlüsselung",
        answerB = "Die theoretische Obergrenze der fehlerfreien Datenübertragungsrate C = B · log₂(1 + S/N) in Abhängigkeit von Bandbreite B und Signal-Rausch-Verhältnis S/N",
        answerC = "Die minimale Entropie eines sicheren Passworts",
        answerD = "Die Anzahl der möglichen Schlüssel pro Sekunde bei Brute-Force",
        correctAnswer = 1,
        explanation = "C = B · log₂(1 + S/N): Mit Bandbreite B Hz und Signal-Rausch-Verhältnis S/N kann man maximal C Bit/s fehlerfrei übertragen. Mehr geht informationstheoretisch nicht — dieses Limit ist unabhängig von der verwendeten Codierung oder Modulation.",
        difficulty = 4,
        funFact = "Shannon bewies 1948, dass fehlerkorrigierende Codes existieren müssen, die beliebig nah an die Kanalkapazität heranreichen — aber ohne konstruktives Beispiel zu liefern. Es dauerte Jahrzehnte, bis Turbo-Codes (1993) und LDPC-Codes praktisch so nah ans Limit herankamen."
    ),

    Question(
        categoryId = 12,
        questionText = "Welche der folgenden Eigenschaften ist für eine kryptographisch sichere Hash-Funktion (wie SHA-256) NICHT erforderlich?",
        answerA = "Kollisionsresistenz: Es ist rechnerisch schwer, zwei verschiedene Eingaben x ≠ y mit H(x) = H(y) zu finden",
        answerB = "Urbildresistenz: Zu gegebenem Hash h ist es schwer, x mit H(x) = h zu finden",
        answerC = "Umkehrbarkeit: Aus dem Hash lässt sich der Originaltext rekonstruieren",
        answerD = "Zweites-Urbild-Resistenz: Zu gegebenem x ist es schwer, y ≠ x mit H(x) = H(y) zu finden",
        correctAnswer = 2,
        explanation = "Umkehrbarkeit ist ausdrücklich NICHT erwünscht — Hash-Funktionen sind Einwegfunktionen. Die drei Sicherheitseigenschaften sind: (1) Preimage-Resistance, (2) Second Preimage-Resistance und (3) Collision-Resistance. Wäre H umkehrbar, wären Passwort-Hashes sofort wertlos.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Die Lorenz-SZ-42 war eine deutsche Schlüsselmaschine im Zweiten Weltkrieg, die für höchste Führungsebenen eingesetzt wurde. Wie wurde sie gebrochen?",
        answerA = "Durch Verrat eines deutschen Offiziers",
        answerB = "Durch die Wiederverwendung desselben Schlüssels für zwei verschiedene Nachrichten (Tageseinstellung), was dem Mathematiker Bill Tutte erlaubte, die Maschine vollständig zu rekonstruieren — ohne sie je gesehen zu haben",
        answerC = "Durch Erbeutung einer Maschine bei der Invasion in der Normandie",
        answerD = "Durch Quantencomputer-Algorithmen",
        correctAnswer = 1,
        explanation = "Im August 1941 sendete ein deutscher Funker dieselbe lange Nachricht zweimal mit identischen Schlüsseleinstellungen ('tiefe' nach Regel). Diese 'Depth' ermöglichte es dem britischen Team in Bletchley Park, die Maschine statistisch zu rekonstruieren. Colossus — der erste programmierfähige elektronische Computer — wurde gebaut, um Lorenz zu brechen.",
        difficulty = 4,
        funFact = "Tommy Flowers, der Ingenieur der Colossus-Maschine, baute sie mit eigenen Ersparnissen vor, da das britische Militär zunächst skeptisch war. Nach dem Krieg durfte er jahrzehntelang nicht darüber sprechen — Colossus blieb bis 1975 geheim."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen symmetrischer und asymmetrischer Verschlüsselung hinsichtlich Schlüsselverwaltung und Geschwindigkeit?",
        answerA = "Symmetrisch ist langsamer, benötigt aber nur einen Schlüssel; asymmetrisch ist schneller, benötigt zwei",
        answerB = "Symmetrisch ist schneller (ca. 1000× schneller), benötigt aber sicheren Schlüsselaustausch; asymmetrisch löst das Schlüsselaustauschproblem, ist aber rechenaufwändig",
        answerC = "Beide Verfahren sind gleich schnell, unterscheiden sich nur in der Schlüssellänge",
        answerD = "Asymmetrisch ist schneller, weil nur ein Schlüssel öffentlich ist",
        correctAnswer = 1,
        explanation = "AES (symmetrisch) ist etwa 1000× schneller als RSA (asymmetrisch) für gleiche Datenmenge. In der Praxis löst man dies hybrid: Asymmetrische Kryptographie für den sicheren Austausch eines symmetrischen Sitzungsschlüssels, dann symmetrische Verschlüsselung der eigentlichen Daten — so wie es TLS macht.",
        difficulty = 4,
        funFact = "Die hybride Verschlüsselung in TLS wurde schon in SSL 2.0 (1995) verwendet. Damals gab es noch US-Exportbeschränkungen für Kryptographie — starke Verschlüsselung war als 'Kriegswaffe' klassifiziert."
    ),

    Question(
        categoryId = 12,
        questionText = "Was versteht man unter 'Steganographie' und wie unterscheidet sie sich von Kryptographie?",
        answerA = "Steganographie verschlüsselt Daten; Kryptographie versteckt sie",
        answerB = "Steganographie versteckt die Existenz einer Nachricht (z. B. Pixel in einem Bild); Kryptographie macht den Inhalt einer sichtbaren Nachricht unlesbar",
        answerC = "Beide Begriffe sind Synonyme für dieselbe Technik",
        answerD = "Steganographie ist eine Unterform der asymmetrischen Kryptographie",
        correctAnswer = 1,
        explanation = "Kryptographie: Die Nachricht ist sichtbar, aber unlesbar. Steganographie: Die Nachricht ist versteckt — ein Beobachter weiß nicht, dass überhaupt eine geheime Kommunikation stattfindet. Beispiel: LSB-Steganographie in Bilddateien, wo das niederwertigste Bit jedes Pixels zur Datenspeicherung genutzt wird.",
        difficulty = 4,
        funFact = "Im antiken Griechenland berichtete Herodot von einem Boten, dem man den Kopf rasierte, die Nachricht auf die Kopfhaut tätowierte und ihn wartete, bis die Haare nachwuchsen — das Paradebeispiel der Steganographie."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Rainbow Table' und welche Gegenmaßnahme macht ihn ineffektiv?",
        answerA = "Eine Tabelle mit allen bekannten WLAN-Passwörtern — abgewehrt durch WPA3",
        answerB = "Eine vorberechnete Tabelle von Hash-Werten für häufige Passwörter — abgewehrt durch das Hinzufügen eines zufälligen 'Salts' zum Passwort vor dem Hashing",
        answerC = "Eine Liste gestohlener Zertifikate — abgewehrt durch Certificate Revocation",
        answerD = "Ein Angriff auf Farbkanäle in Bilddateien",
        correctAnswer = 1,
        explanation = "Rainbow Tables speichern Millionen von (Passwort, Hash)-Paaren komprimiert. Wenn ein Datenbankdump geleakt wird, kann man Hashes sofort nachschlagen. Salt-Werte (zufällige Zusatzdaten pro Benutzer) machen alle vorberechneten Tabellen nutzlos, da H(Passwort + Salt) für jeden Nutzer einzigartig ist.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Primzahleigenschaft macht den Miller-Rabin-Primzahltest relevant für die RSA-Schlüsselgenerierung?",
        answerA = "Er beweist definitiv, ob eine Zahl prim ist, in O(log n) Zeit",
        answerB = "Er ist ein probabilistischer Test: Mit hoher Wahrscheinlichkeit korrekt, aber Fehlerwahrscheinlichkeit sinkt exponentiell mit der Anzahl der Runden",
        answerC = "Er faktorisiert Zahlen schneller als der Sieb des Eratosthenes",
        answerD = "Er testet ausschließlich Mersenne-Primzahlen",
        correctAnswer = 1,
        explanation = "Miller-Rabin ist probabilistisch: Pro Runde ist die Fehlerwahrscheinlichkeit maximal 1/4. Nach 50 Runden ist die Wahrscheinlichkeit, eine zusammengesetzte Zahl fälschlicherweise als prim einzustufen, kleiner als 4⁻⁵⁰ ≈ 10⁻³⁰ — für die Praxis ausreichend sicher.",
        difficulty = 4,
        funFact = "Die einzigen bekannten Zahlen, die alle deterministischen Miller-Rabin-Zeugen unter einer bestimmten Schranke täuschen, heißen Carmichael-Zahlen. Die kleinste ist 561 = 3 × 11 × 17."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen einem Blockchiffre (wie AES) und einem Stromchiffre (wie ChaCha20)?",
        answerA = "Blockchiffren sind immer unsicherer als Stromchiffren",
        answerB = "Blockchiffren verschlüsseln Daten in festen Blöcken (z. B. 128 Bit für AES); Stromchiffren erzeugen einen pseudozufälligen Schlüsselstrom und XOR-verknüpfen ihn Bit für Bit mit dem Klartext",
        answerC = "Stromchiffren verwenden immer asymmetrische Schlüssel",
        answerD = "Blockchiffren können keine Daten unter 128 Bit verschlüsseln",
        correctAnswer = 1,
        explanation = "Blockchiffren: Feste Blockgröße, verschiedene Betriebsmodi (ECB, CBC, GCM). Stromchiffren: Erzeugung eines Schlüsselstroms aus Schlüssel + Nonce. ChaCha20 wird u. a. in TLS 1.3 und QUIC eingesetzt, da es auf Systemen ohne Hardware-AES-Beschleunigung deutlich schneller ist.",
        difficulty = 4,
        funFact = "Der ECB-Modus (Electronic Codebook) gilt als gefährlich, weil identische Klartextblöcke identische Geheimtextblöcke erzeugen — das berühmte 'ECB-Penguin'-Bild zeigt das Linux-Tux-Logo, der durch ECB-verschlüsselt noch erkennbar ist."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Nonce' in kryptographischen Protokollen und warum ist sein einmaliger Einsatz kritisch?",
        answerA = "Ein Nonce ist eine Abkürzung für 'nonsense code' — ein Füllwort ohne kryptographische Bedeutung",
        answerB = "Ein Nonce (Number used Once) ist eine einmalig verwendete Zufallszahl; Wiederverwendung kann katastrophal sein, z. B. bei GCM-Modus den gesamten Schlüssel offenbaren",
        answerC = "Ein Nonce ist der öffentliche Teil eines RSA-Schlüssels",
        answerD = "Ein Nonce ist ein Zeitstempel zur Verhinderung von Replay-Angriffen",
        correctAnswer = 1,
        explanation = "Im AES-GCM-Modus: Werden zwei Nachrichten mit gleichem Schlüssel und gleichem Nonce verschlüsselt, kann ein Angreifer durch XOR beider Geheimtexte den Authentifizierungsschlüssel (GHASH) berechnen und damit die Integrität gefälscht werden. Sony PlayStation 3 verwendete 2010 denselben Nonce für alle ECDSA-Signaturen — der private Schlüssel ließ sich berechnen.",
        difficulty = 4,
        funFact = "Der Sony PS3-Hack durch fail0verflow (2010) offenbarte: Sony verwendete in der ECDSA-Signatur statt eines zufälligen k jedes Mal denselben Wert. Das ermöglichte die direkte Berechnung des privaten Schlüssels — ein Lehrbuchfehler mit enormen Folgen."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der 'Shor-Algorithmus' und welche Auswirkung hätte ein leistungsfähiger Quantencomputer auf aktuelle Public-Key-Kryptographie?",
        answerA = "Ein klassischer Algorithmus zur schnelleren Primzahltestung",
        answerB = "Peter Shors Quantenalgorithmus (1994) löst das Faktorisierungs- und das diskrete Logarithmusproblem in polynomialer Zeit — damit wären RSA, DH und ECC gebrochen",
        answerC = "Ein Algorithmus zur Post-Quanten-Verschlüsselung",
        answerD = "Eine Methode zur Verbesserung von AES auf Quantencomputern",
        correctAnswer = 1,
        explanation = "Shor's Algorithmus würde RSA (Faktorisierung) und Diffie-Hellman/ECC (diskreter Logarithmus) in polynomialer Zeit brechen. AES wäre durch Grover's Algorithmus nur halb so sicher (effektive Schlüssellänge halbiert). Daher entwickelt das NIST Post-Quantum-Cryptography-Standards wie CRYSTALS-Kyber und CRYSTALS-Dilithium.",
        difficulty = 4,
        funFact = "Das NIST standardisierte 2024 die ersten Post-Quanten-Algorithmen: ML-KEM (CRYSTALS-Kyber) für Schlüsselaustausch und ML-DSA (CRYSTALS-Dilithium) für digitale Signaturen — beide basieren auf mathematischen Gitterproblemen."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist elliptische Kurven-Kryptographie (ECC) und warum wird sie gegenüber RSA bevorzugt?",
        answerA = "ECC ist unsicherer als RSA, wird aber wegen seiner Geschwindigkeit trotzdem verwendet",
        answerB = "ECC bietet dieselbe Sicherheitsstärke wie RSA bei deutlich kürzeren Schlüsseln: Ein 256-Bit-ECC-Schlüssel entspricht etwa einem 3072-Bit-RSA-Schlüssel",
        answerC = "ECC verwendet elliptische Kurven zur Primzahlfaktorisierung",
        answerD = "ECC ist eine Variante des AES-Algorithmus für mobile Geräte",
        correctAnswer = 1,
        explanation = "ECC basiert auf dem Diskreten-Logarithmus-Problem auf elliptischen Kurven. Das beste bekannte Lösungsverfahren ist exponentiell — effizienter als für normale DL-Probleme. Ergebnis: 256-Bit-ECC ≈ 128-Bit-Sicherheitsniveau, 256-Bit-RSA dagegen wäre völlig unsicher. Bitcoins Signaturverfahren ECDSA nutzt die Kurve secp256k1.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Codierungstechnik verwendet David Huffman (1952), um eine optimale präfixfreie Codierung zu erzeugen?",
        answerA = "Er weist allen Symbolen Codes gleicher Länge zu",
        answerB = "Er baut einen binären Baum von unten nach oben: Die zwei unwahrscheinlichsten Symbole werden zusammengefasst, bis ein Wurzelknoten entsteht",
        answerC = "Er verwendet einen linearen Algorithmus basierend auf der ASCII-Reihenfolge",
        answerD = "Er löst ein lineares Programm zur Minimierung der mittleren Codewortlänge",
        correctAnswer = 1,
        explanation = "Huffman-Algorithmus: (1) Sortiere Symbole nach Häufigkeit. (2) Fasse die zwei seltensten zu einem neuen Knoten zusammen. (3) Wiederhole bis zur Wurzel. Der resultierende Baum liefert den optimalen Präfixcode — die mittlere Codewortlänge liegt zwischen H(X) und H(X)+1 Bit.",
        difficulty = 4,
        funFact = "David Huffman entwickelte seinen Algorithmus als Student am MIT — in einer Hausarbeit für Professor Robert Fano, der selbst an einem ähnlichen Verfahren arbeitete (Shannon-Fano-Codierung). Huffman schlug seinen eigenen Professor."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Differential Cryptanalysis' (Differentielle Kryptanalyse) und wer entwickelte sie?",
        answerA = "Eine Methode zur Analyse von Zufallszahlengeneratoren, entwickelt von Shannon",
        answerB = "Eine Angriffsmethode auf Blockchiffren von Biham und Shamir (1990): Sie analysiert, wie sich Unterschiede im Klartext auf Unterschiede im Geheimtext auswirken",
        answerC = "Eine Technik zur Schlüsselableitung aus Hash-Funktionen",
        answerD = "Eine Variante des Babystep-Giantstep-Algorithmus",
        correctAnswer = 1,
        explanation = "Biham und Shamir zeigten 1990, dass DES unter Differential Cryptanalysis schwächer ist als gedacht. Später stellte sich heraus, dass das IBM-Team DES absichtlich gegen diesen Angriff gehärtet hatte — die Methode war geheim gehalten worden. NSA kannte die Technik bereits seit den 1970ern.",
        difficulty = 4,
        funFact = "Die NSA hatte IBM gebeten, die S-Boxen von DES so zu gestalten, dass sie gegen Differential Cryptanalysis resistent sind — ohne zu erklären warum. IBM erfüllte die Anfrage, ohne zu verstehen, wogegen sie schützten."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Padding Oracle Angriff' auf AES-CBC, und welcher reale Angriff basiert darauf?",
        answerA = "Ein Angriff auf Zufallszahlengeneratoren bei der Schlüsselgenerierung",
        answerB = "Der Angreifer sendet manipulierte Geheimtexte und nutzt Fehlerantworten des Servers über ungültiges Padding aus, um den Klartext byteweise zu entschlüsseln — Basis des POODLE-Angriffs auf SSL 3.0",
        answerC = "Eine Methode zum Brechen von RSA durch Manipulation des PKCS#1-Paddings",
        answerD = "Ein Angriff auf elliptische Kurven durch absichtlich falsch gewählte Padding-Bits",
        correctAnswer = 1,
        explanation = "CBC-Modus + fehlerhafte Implementierung: Der Server verrät durch verschiedene Fehlermeldungen, ob das Padding korrekt war. Dies reicht aus, um jeden Block des Geheimtexts byteweise zu entschlüsseln — mit maximal 256 Anfragen pro Byte. POODLE (2014) nutzte SSLv3's CBC-Padding-Oracle und zwang die Abschaffung von SSL 3.0.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die 'Unicity-Distanz' (Eindeutigkeitsabstand) in der Kryptographie nach Shannon?",
        answerA = "Die minimale Schlüssellänge für informationstheoretische Sicherheit",
        answerB = "Die minimale Menge an Geheimtext, die ein Angreifer theoretisch benötigt, um den Schlüssel eindeutig zu bestimmen — bei natürlicher Sprache etwa n₀ = H(K) / D (H(K) = Schlüsselentropie, D = Redundanz der Sprache)",
        answerC = "Der Abstand zwischen zwei aufeinanderfolgenden Schlüsseln im Schlüsselraum",
        answerD = "Die Anzahl der Runden in einem Blockchiffre, ab der keine Verbesserung mehr eintritt",
        correctAnswer = 1,
        explanation = "Shannon definierte: U₀ ≈ H(K)/D. Für englischen Text mit D ≈ 3,2 Bit/Buchstabe und einem 26-Zeichen-Schlüssel: U₀ ≈ log₂(26)/3,2 ≈ 1,4 Buchstaben. Eine Caesar-Chiffre wird also schon bei knapp 2 Buchstaben Geheimtext theoretisch eineutig lösbar — praktisch hilft Häufigkeitsanalyse natürlich.",
        difficulty = 4,
        funFact = "Shannon bewies damit formal, dass das One-Time-Pad die einzige Chiffre mit unendlichem Eindeutigkeitsabstand ist — jeder Schlüssel ergibt gleich wahrscheinlich jeden möglichen Klartext."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Chosen Ciphertext Attack' (CCA2) und welches Verschlüsselungsschema ist dagegen sicher?",
        answerA = "Der Angreifer kann beliebige Klartexte zur Verschlüsselung einreichen",
        answerB = "Der Angreifer kann beliebige Geheimtexte zur Entschlüsselung einreichen (außer dem Zielgeheimtext) — OAEP-Padding macht RSA CCA2-sicher",
        answerC = "Ein Angriff auf Streaming-Daten bei laufender Verbindung",
        answerD = "Ein Angriff, der den Geheimtext durch Invertierung der Chiffrierfunktion direkt berechnet",
        correctAnswer = 1,
        explanation = "CCA2 (Adaptive Chosen Ciphertext Attack): Der Angreifer darf das Entschlüsselungsorakel nutzen. Textbook-RSA ist anfällig: Der Angreifer kann c·2ᵉ mod n einreichen und erhält damit Informationen über m. RSA-OAEP (Optimal Asymmetric Encryption Padding) ist IND-CCA2-sicher unter bestimmten Annahmen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Die Playfair-Chiffre (1854) verschlüsselt Buchstabenpaare (Bigramme) statt einzelne Buchstaben. Welche Schwäche bleibt trotzdem bestehen?",
        answerA = "Sie ist eine monoalphabetische Chiffre und durch Häufigkeitsanalyse einzelner Buchstaben leicht brechbar",
        answerB = "Bigramm-Häufigkeitsanalyse ist weiterhin möglich, da im deutschen Sprachraum bestimmte Buchstabenpaare wie 'EN', 'ER', 'CH' sehr häufig vorkommen",
        answerC = "Sie verwendet einen zu kurzen Schlüssel von nur 5×5 = 25 Möglichkeiten",
        answerD = "Das Verfahren ist symmetrisch und daher unsicher",
        correctAnswer = 1,
        explanation = "Playfair ist bigrammatisch polyalphabetisch, aber die statistischen Eigenschaften natürlicher Sprache auf Bigramm-Ebene bleiben erhalten. Ein geübter Kryptanalytiker kann mit genug Geheimtext durch Bigramm-Häufigkeitsanalyse und bekannte Sprachstrukturen Playfair brechen.",
        difficulty = 4,
        funFact = "Playfair wurde im Ersten und Zweiten Weltkrieg von verschiedenen Armeen verwendet, obwohl ihre Schwächen bekannt waren. John F. Kennedy nutzte Playfair im Zweiten Weltkrieg auf PT-109 — seine berühmte Nachricht nach dem Untergang des Schiffs wurde damit übermittelt."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Salted Password Hashing' und welcher Unterschied besteht zwischen bcrypt, scrypt und PBKDF2?",
        answerA = "Alle drei sind identisch und nur unterschiedliche Namen für dasselbe Verfahren",
        answerB = "Alle drei fügen Salts hinzu und sind absichtlich langsam; bcrypt und scrypt sind zudem speicherintensiv (memory-hard), was GPU-Angriffe bremst — scrypt am stärksten",
        answerC = "PBKDF2 ist unsicher, bcrypt und scrypt sind sichere Alternativen zu SHA-256",
        answerD = "bcrypt ist ein symmetrischer Verschlüsselungsalgorithmus, kein Hash-Verfahren",
        correctAnswer = 1,
        explanation = "PBKDF2: Iterativer Hash, konfigurierbare Iterationsanzahl, aber nicht memory-hard. bcrypt: Auf Blowfish-Basis, memory-hard bis zu einem gewissen Grad, bewährt seit 1999. scrypt: Stark memory-hard (Colin Percival, 2009), schwerer für ASICs — deshalb auch bei Litecoin verwendet. Argon2 (2015, PHC-Gewinner) ist der aktuelle Empfehling.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was beschreibt Shannons Begriff 'Konfusion und Diffusion' als Designprinzipien für Blockchiffren?",
        answerA = "Konfusion versteckt die Geheimtextlänge; Diffusion verteilt den Klartext auf mehrere Blöcke",
        answerB = "Konfusion: Jedes Bit des Geheimtexts hängt auf komplexe Weise vom Schlüssel ab (S-Boxen). Diffusion: Jedes Bit des Klartexts beeinflusst viele Bits des Geheimtexts (Permutationen/Feistel-Struktur)",
        answerC = "Konfusion beschreibt den Schlüsselaustausch; Diffusion beschreibt die Schlüsselexpansion",
        answerD = "Beide Begriffe bedeuten dasselbe: möglichst viele Runden im Chiffrierprozess",
        correctAnswer = 1,
        explanation = "Shannon 1949: Konfusion — macht die Beziehung zwischen Schlüssel und Geheimtext so komplex wie möglich (implementiert durch S-Boxen). Diffusion — verteilt den statistischen Einfluss eines Klartextbits über viele Geheimtextbits (implementiert durch Permutationen und XOR). AES nutzt beide Prinzipien: SubBytes (Konfusion) + ShiftRows/MixColumns (Diffusion).",
        difficulty = 4,
        funFact = "AES hat den 'Strict Avalanche Criterion' (SAC): Wenn ein einziges Bit im Klartext oder Schlüssel geändert wird, ändern sich statistisch 50% der Geheimtextbits — das ist maximale Diffusion."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein Feistel-Netzwerk und welche bekannte Blockchiffre basiert darauf?",
        answerA = "Ein Schlüsselaustauschprotokoll basierend auf dem Diskreten-Logarithmus-Problem",
        answerB = "Eine Struktur, bei der der Eingabeblock in zwei Hälften aufgeteilt wird und iterativ Rundenfunktionen angewendet werden — DES (Data Encryption Standard) basiert darauf",
        answerC = "Eine Methode zur Authentifizierung von Nachrichten durch Hash-Verkettung",
        answerD = "AES verwendet eine Feistel-Struktur mit 14 Runden",
        correctAnswer = 1,
        explanation = "Feistel (IBM, 1970er): Block aufteilen in L und R. Jede Runde: L_neu = R, R_neu = L XOR F(R, Rundenschlüssel). Vorteil: Die Entschlüsselung hat dieselbe Struktur wie die Verschlüsselung, nur mit umgekehrter Schlüsselreihenfolge — F muss nicht umkehrbar sein. DES verwendet 16 Feistel-Runden.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Warum gilt DES (Data Encryption Standard, 56-Bit-Schlüssel) heute als unsicher?",
        answerA = "DES verwendet eine defekte S-Box, die von der NSA absichtlich geschwächt wurde",
        answerB = "Der 56-Bit-Schlüsselraum ist mit moderner Hardware durch Brute-Force durchsuchbar: 1997 wurde DES in 96 Tagen gebrochen; 1999 in 22 Stunden mit DES Cracker",
        answerC = "DES wurde durch einen theoretischen Quantenangriff gebrochen",
        answerD = "DES-Schlüssel können nicht mehr geheim gehalten werden, weil der Algorithmus öffentlich ist",
        correctAnswer = 1,
        explanation = "2⁵⁶ ≈ 72 Billiarden mögliche Schlüssel klingt viel, aber 1999 zeigte EFF's 'Deep Crack' zusammen mit distributed.net: 22 Stunden für eine vollständige Schlüsselsuche. Heute würde das jede Gaming-GPU in Minuten schaffen. 3DES (3×DES, effektiv 112 Bit) ist ein Workaround, AES die richtige Lösung.",
        difficulty = 4,
        funFact = "Die NSA forderte 1970 bei der DES-Entwicklung eine Reduzierung des Schlüssels von ursprünglich 64 auf 56 Bit. Dies führte jahrelang zu Spekulationen über eine Backdoor — tatsächlich wollte die NSA nur sicherstellen, dass sie selbst entschlüsseln konnte."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Perfect Secrecy' nach Shannon und welches Kriterium muss ein Kryptosystem erfüllen?",
        answerA = "Ein System ist perfekt sicher, wenn kein Computer es in unter 1000 Jahren brechen kann",
        answerB = "Ein Kryptosystem hat Perfect Secrecy, wenn der Geheimtext keine statistischen Informationen über den Klartext liefert: P(M=m | C=c) = P(M=m) für alle m, c",
        answerC = "Perfect Secrecy bedeutet, dass der Schlüssel länger als 256 Bit ist",
        answerD = "Ein System ist perfekt sicher, wenn es NP-schwer zu brechen ist",
        correctAnswer = 1,
        explanation = "Shannon 1949: Perfekte Sicherheit bedeutet, dass Geheimtext und Klartext statistisch unabhängig sind. Ein Angreifer mit unbegrenzter Rechenleistung gewinnt keine Information. Shannon bewies: Dies erfordert |K| ≥ |M| — der Schlüssel muss mindestens so groß sein wie die Nachricht. Nur das OTP erfüllt das praktisch.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein HMAC (Hash-based Message Authentication Code) und wogegen schützt er?",
        answerA = "Ein HMAC ist eine verschlüsselte Version einer Hash-Funktion für Passwörter",
        answerB = "HMAC = H(K XOR opad || H(K XOR ipad || M)) — er schützt sowohl Integrität als auch Authentizität einer Nachricht durch einen geheimen Schlüssel",
        answerC = "Ein HMAC ist eine Methode zur symmetrischen Verschlüsselung mit Hash-Funktionen",
        answerD = "HMAC schützt ausschließlich gegen Abhören, nicht gegen Manipulation",
        correctAnswer = 1,
        explanation = "Einfaches Anhängen eines Hashs (H(K || M)) wäre anfällig für Length-Extension-Angriffe bei Merkle-Damgård-Hashfunktionen wie SHA-256. HMAC's doppelte Hash-Struktur verhindert das. Er garantiert: Nur wer den Schlüssel K kennt, kann einen gültigen HMAC erzeugen oder verifizieren.",
        difficulty = 4,
        funFact = "TLS 1.2 verwendete HMAC-SHA256 zur Integritätssicherung. TLS 1.3 ersetzt HMAC durch AEAD-Modi (wie AES-GCM), die Verschlüsselung und Authentifizierung in einem Schritt bieten."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Semantic Security' (semantische Sicherheit / IND-CPA) in der modernen Kryptographie?",
        answerA = "Ein System ist semantisch sicher, wenn der Algorithmus durch keinen mathematischen Beweis gebrochen werden kann",
        answerB = "Ein Verschlüsselungsschema ist IND-CPA-sicher, wenn kein effizienter Angreifer zwei Geheimtexte unterscheiden kann, auch wenn er selbst Klartexte zur Verschlüsselung wählen darf — deterministisches RSA ist NICHT IND-CPA-sicher",
        answerC = "Semantische Sicherheit bedeutet, dass die Bedeutung (Semantik) einer Nachricht verborgen ist",
        answerD = "IND-CPA-Sicherheit gilt als ausreichend für alle kryptographischen Anwendungen",
        correctAnswer = 1,
        explanation = "IND-CPA (Indistinguishability under Chosen Plaintext Attack): Der Angreifer wählt zwei Klartexte m₀, m₁, erhält c = E(mᵦ), und darf beliebige andere Klartexte verschlüsseln. Er soll b nicht mit Wahrscheinlichkeit > 1/2 + ε erraten können. Deterministisches RSA ohne Randomisierung ist nicht IND-CPA-sicher: E(m₀) und E(m₁) sind deterministisch unterschiedlich.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist das Kryptosystem ECDSA und welcher katastrophale Fehler wurde bei der Sony PlayStation 3 gemacht?",
        answerA = "Sony verwendete zu kurze Schlüssel — ein 64-Bit-Schlüssel statt 256 Bit",
        answerB = "ECDSA benötigt pro Signatur eine zufällige Zahl k; Sony verwendete für alle Signaturen dasselbe k, was die algebraische Berechnung des privaten Schlüssels ermöglichte",
        answerC = "Sony verwendete eine gebrochene elliptische Kurve mit bekannten Schwachstellen",
        answerD = "ECDSA wurde auf der PS3 in der falschen Richtung angewendet — Verifizierung statt Signierung",
        correctAnswer = 1,
        explanation = "Bei ECDSA gilt: r = (k·G).x mod n, s = k⁻¹·(H(m) + r·d) mod n. Wenn k immer gleich ist, ergibt sich aus zwei Signaturen (r, s₁) und (r, s₂): d = (s₁·H(m₂) − s₂·H(m₁)) / (s₂ − s₁) mod n — der private Schlüssel direkt. Diesen Fehler machte Sony, was zur vollständigen PS3-Kompromittierung führte.",
        difficulty = 4,
        funFact = "Die Hacker-Gruppe fail0verflow präsentierte dies auf dem 27C3-Kongress 2010. George Hotz (geohot) publizierte daraufhin den privaten Schlüssel öffentlich — Sony klagte, verlor aber letztlich den Kampf gegen das Homebrew-Ökosystem."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Timing-Angriff' (Side-Channel-Angriff) in der Kryptographie?",
        answerA = "Ein Angriff, der auf die schwächsten Stellen eines Kryptosystems zu einem günstigen Zeitpunkt abzielt",
        answerB = "Ein Angriff, der die Ausführungszeit kryptographischer Operationen misst, um Schlüsselbits zu erschließen — z. B. RSA: unterschiedliche Quadrier- vs. Multiplizieroperationen je nach Schlüsselbit",
        answerC = "Ein Angriff auf Zeitstempel in TLS-Handshakes",
        answerD = "Ein Angriff, der Netzwerkverzögerungen ausnutzt, um Pakete zu entschlüsseln",
        correctAnswer = 1,
        explanation = "Paul Kocher zeigte 1996: RSA-Entschlüsselungszeit variiert je nach Schlüsselbit (Square-and-Multiply-Algorithmus). Durch viele Messungen lassen sich einzelne Schlüsselbits bestimmen. Gegenmaßnahmen: Constant-Time-Implementierungen, Blinding (Multiplikation mit Zufallskomponente vor der Operation).",
        difficulty = 4,
        funFact = "Spectre und Meltdown (2018) sind gewissermaßen CPU-Level-Timing-Angriffe: Sie nutzen spekulative Ausführung und Cache-Timing aus. Der Angreifer misst Speicherzugriffszeiten, um privilegierte Daten zu lesen — ohne Kryptographie direkt anzugreifen."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die 'Diffie-Hellman-Schlüsselaustausch'-Schwachstelle beim Logjam-Angriff (2015)?",
        answerA = "Logjam nutzte eine Backdoor in OpenSSL aus",
        answerB = "Viele Server erlaubten Export-DH mit 512-Bit-Primzahlen; solche Primzahlen können mit Number Field Sieve vorberechnet werden — ein MITM-Angreifer konnte dann in Echtzeit entschlüsseln",
        answerC = "Logjam ist ein Downgrade-Angriff auf AES-256 zu AES-128",
        answerD = "Ein Angriff auf elliptische Kurven durch Precomputation der Gruppenordnung",
        correctAnswer = 1,
        explanation = "Logjam (2015): Für 512-Bit-DH kann Number Field Sieve (NFS) diskrete Logarithmen in etwa einer Woche vorberechnen. 92% aller VPN-Server nutzten damals dieselben 1024-Bit-Primzahlen — für diese lohnte sich eine teure Vorberechnung. NSA hatte dafür wahrscheinlich bereits 2012 einen Supercomputer eingesetzt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Zero-Knowledge-Beweis' und welches Alltagsbeispiel illustriert das Prinzip?",
        answerA = "Ein Beweis, der so kurz ist, dass er keine Information enthält",
        answerB = "Ein Protokoll, mit dem ein Prüfer einem Verifizierer beweisen kann, dass er ein Geheimnis kennt, ohne das Geheimnis selbst preiszugeben — Beispiel: Waldo-in-einem-Bild-finden ohne die Position zu zeigen",
        answerC = "Ein kryptographisches Protokoll, das keine Schlüssel benötigt",
        answerD = "Ein Hash-Verfahren, das keine Kollisionen produziert",
        correctAnswer = 1,
        explanation = "ZK-Beweise (Goldwasser, Micali, Rackoff, 1985) sind vollständig (ehrlicher Prüfer überzeugt immer), gesund (Betrüger kann kaum überzeugen) und Zero-Knowledge (Verifizierer erfährt nichts außer der Wahrheit). Anwendung: zk-SNARKs in Zcash (Blockchain-Zahlungen ohne Betragsoffenbarung) und zkRollups in Ethereum.",
        difficulty = 4,
        funFact = "Das 'Ali-Baba-Höhlen'-Gedankenexperiment illustriert ZK: Peggy beweist Victor, dass sie die Losung für eine Höhlentür kennt, ohne die Losung je zu sagen — sie tritt abwechselnd durch die eine oder andere Seite aus, je nachdem welche Victor verlangt."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Homomorphe Verschlüsselung' und welcher Durchbruch gelang Craig Gentry 2009?",
        answerA = "Ein Verfahren, bei dem verschiedene Algorithmen auf denselben Schlüssel zugreifen können",
        answerB = "Gentry konstruierte das erste vollständig homomorphe Verschlüsselungsschema (FHE): Berechnungen auf verschlüsselten Daten möglich, sodass das Ergebnis nach Entschlüsselung dem Ergebnis auf unverschlüsselten Daten entspricht",
        answerC = "Ein Verfahren zur Komprimierung von Schlüsseln für Cloud-Dienste",
        answerD = "Eine Form der symmetrischen Verschlüsselung basierend auf Homomorphismen in Gruppentheorie",
        correctAnswer = 1,
        explanation = "FHE: E(a) ○ E(b) = E(a ⊕ b) und E(a) ● E(b) = E(a · b). Ein Cloud-Dienst kann Berechnungen auf Ihren verschlüsselten Daten durchführen, ohne sie je zu entschlüsseln — das Ergebnis ist ebenfalls verschlüsselt. Gentries Konstruktion war bahnbrechend, aber noch sehr langsam. Aktuelle FHE-Implementierungen sind ~10.000–100.000× langsamer als Klartext-Berechnungen.",
        difficulty = 4,
        funFact = "IBM entwickelt mit HElib und Microsoft mit SEAL FHE-Bibliotheken. Anwendungen: HIPAA-konformes Machine Learning auf Patientendaten im Krankenhaus, ohne dass der Cloud-Anbieter die Daten sehen kann."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die Kasiski-Analyse und warum versagt sie bei sehr kurzen Schlüsseln der Vigenère-Chiffre?",
        answerA = "Sie versagt, weil kurze Schlüssel keine Wiederholungen erzeugen",
        answerB = "Bei sehr kurzen Schlüsseln (z. B. 2–3 Buchstaben) gibt es zu viele zufällige Koinzidenzen im Text, die nicht aus echten Schlüsselwiederholungen stammen — dies erschwert die Bestimmung der genauen Schlüssellänge",
        answerC = "Kasiski-Analyse funktioniert nur für lateinische, nicht für deutsche Texte",
        answerD = "Kurze Schlüssel erzeugen identische Geheimtexte, sodass die Analyse irrelevant wird",
        correctAnswer = 1,
        explanation = "Kasiski sucht nach sich wiederholenden Trigrammen oder längeren Sequenzen im Geheimtext. Bei einem sehr kurzen Schlüssel (k=2) gibt es zufällig viele Abstände die Vielfache von 2 sind — statistisches Rauschen überlagert das Signal. Der Friedman-Test (Index of Coincidence) ist robuster für kurze Schlüssel.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Commitment Scheme' in der Kryptographie und welche zwei Eigenschaften muss es haben?",
        answerA = "Ein Protokoll zum sicheren Übertragen von Transaktionsschlüsseln zwischen Banken",
        answerB = "Ein Protokoll, bei dem Alice einen Wert 'versiegelt' (commit), ohne ihn zu zeigen, und ihn später öffnet (reveal) — es muss 'binding' (nicht änderbar) und 'hiding' (verborgen) sein",
        answerC = "Ein Zeitstempel-System für digitale Verträge",
        answerD = "Eine Methode zur Erstellung unveränderlicher Blockchain-Einträge",
        correctAnswer = 1,
        explanation = "Binding: Alice kann nach dem Commit den Wert nicht mehr ändern. Hiding: Bob erfährt vor dem Reveal nichts über den Wert. Anwendung: Münzwurf-Protokolle über das Internet, Auktionen (Gebote erst offenbar nach allen Commits), Zero-Knowledge-Beweise, Blockchain-Smart-Contracts.",
        difficulty = 4,
        funFact = "Hash-Funktionen bilden einfache Commitment Schemes: Alice sendet H(Wert || Zufallssalz) als Commitment. Beim Reveal zeigt sie (Wert, Salz) — Bob prüft den Hash. Binding durch Kollisionsresistenz, Hiding durch Preimage-Resistance."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Differential Power Analysis' (DPA) und wer entwickelte sie?",
        answerA = "Eine mathematische Methode zur Analyse von Schlüsselverteilungen in Blockchiffren",
        answerB = "Paul Kocher et al. (1998): Durch statistische Analyse des Stromverbrauchs eines Chips während kryptographischer Operationen lassen sich Schlüsselbits ableiten — besonders relevant für Smartcards",
        answerC = "Ein Angriff auf den Zufallszahlengenerator von Smartcards",
        answerD = "Eine Methode zur Messung der Effizienz von Hash-Funktionen auf eingebetteten Systemen",
        correctAnswer = 1,
        explanation = "DPA: Der Stromverbrauch eines CMOS-Chips hängt von den verarbeiteten Daten ab (Hamminggewicht). Durch statistische Korrelation von vielen Messungen lassen sich Schlüsselbits extrahieren. Gegenmaßnahmen: Zufällige Verzögerungen, Masking (zufällige Datenmanipulation), Hardware-Abschirmung.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was versteht man unter 'Kryptographischer Zufälligkeit' (CSPRNG) und warum reicht ein normaler Pseudozufallsgenerator (PRNG) für Kryptographie nicht aus?",
        answerA = "CSPRNG sind schneller als normale PRNGs — das ist ihr einziger Vorteil",
        answerB = "Ein CSPRNG muss den 'Next-Bit-Test' bestehen: Ein Angreifer, der alle vorherigen Bits kennt, kann das nächste nicht mit Wahrscheinlichkeit > 1/2 vorhersagen — normale PRNGs wie rand() versagen diesen Test",
        answerC = "CSPRNG verwenden nur hardware-basierte Entropiequellen",
        answerD = "Normale PRNGs sind kryptographisch sicher, wenn man einen zufälligen Seed verwendet",
        correctAnswer = 1,
        explanation = "Normale PRNGs (Mersenne Twister, Linear Congruential) sind vorhersagbar: Wer 624 aufeinanderfolgende 32-Bit-Werte des Mersenne Twister kennt, kann den gesamten weiteren Strom vorhersagen. CSPRNG wie /dev/urandom, ChaCha20-Drain oder HMAC-DRBG sind darauf ausgelegt, diese Rückrechenbarkeit zu verhindern.",
        difficulty = 4,
        funFact = "Debian hatte 2008 einen katastrophalen Bug im OpenSSL-PRNG: Ein Entwickler hatte zur Vermeidung eines Valgrind-Warnfehlers zwei Zeilen auskommentiert, die den Entropiepool speisten. Für 2 Jahre generierte Debian-OpenSSL nur 32.767 verschiedene Schlüssel — ein kompletter Kompromiss."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Threshold Cryptography' und welches Problem löst das Shamir's Secret Sharing?",
        answerA = "Ein Verfahren, bei dem Schlüssel erst ab einer Mindestlänge als sicher gelten",
        answerB = "Shamir (1979): Ein Geheimnis S wird in n Anteile aufgeteilt, sodass mindestens k Anteile nötig sind, um S zu rekonstruieren, aber weniger als k Anteile keine Information über S verraten — basierend auf Polynominterpolation",
        answerC = "Ein System, bei dem mehrere Administratoren gleichzeitig authentifiziert sein müssen",
        answerD = "Ein Verfahren zur Aufteilung eines RSA-Schlüssels auf mehrere Server",
        correctAnswer = 1,
        explanation = "Shamir's (k,n)-Schwellwertschema: Man wählt ein Polynom p(x) vom Grad k-1 mit p(0)=S. Die n Anteile sind (i, p(i)). Jede Menge von k Punkten reicht per Lagrange-Interpolation, um p(0)=S zu rekonstruieren. Informationstheoretisch sicher: k-1 Punkte verraten nichts über S.",
        difficulty = 4,
        funFact = "Shamir's Secret Sharing wird in Hardware-Sicherheitsmodulen (HSMs) bei Zertifizierungsstellen eingesetzt: Der Root-CA-Schlüssel wird auf mehrere Zeremonienmitarbeiter verteilt — kein Einzelner kann ihn alleine nutzen."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die 'Avalanche-Eigenschaft' (Lawineneffekt) in Blockchiffren und wie wird sie gemessen?",
        answerA = "Die Eigenschaft, dass eine falsche Entschlüsselung zu einem exponentiell wachsenden Fehler führt",
        answerB = "Eine Änderung von einem einzigen Bit im Klartext oder Schlüssel sollte statistisch 50% der Geheimtextbits ändern — gemessen durch den Strict Avalanche Criterion (SAC)",
        answerC = "Die Eigenschaft, dass jede Runde eines Blockchiffres mehr Bits verschlüsselt als die vorherige",
        answerD = "Ein Maß dafür, wie schnell ein Blockchiffre Konvergenz erreicht",
        correctAnswer = 1,
        explanation = "SAC (Webster & Tavares, 1985): Für jeden einzelnen Bit-Flip in der Eingabe sollte jedes Ausgabe-Bit mit Wahrscheinlichkeit genau 1/2 flippen. AES erfüllt SAC nach wenigen Runden vollständig — das ist eine Folge der MixColumns-Operation (maximale Diffusion). DES erfüllt SAC nach 5 Runden.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der 'Kolmogorov-Komplexität'-Begriff und wie hängt er mit Kryptographie und Zufälligkeit zusammen?",
        answerA = "Die Kolmogorov-Komplexität K(s) beschreibt die Länge des kürzesten Programms, das s erzeugt — zufällige Strings haben K(s) ≈ |s|, kompressible Strings K(s) << |s|",
        answerB = "Eine Methode zur Messung der Schlüsselentropie bei RSA-Algorithmen",
        answerC = "Die Komplexität des Primfaktorisierungsproblems in Abhängigkeit von der Schlüssellänge",
        answerD = "Ein Maß für die Sicherheit von Hash-Funktionen gegen Kollisionsangriffe",
        correctAnswer = 0,
        explanation = "Kolmogorov-Komplexität ist nicht berechenbar, aber konzeptuell zentral: Ein kryptographisch sicherer Schlüssel ist 'zufällig', wenn kein kurzes Programm ihn erzeugen kann. PRNGs erzeugen Sequenzen mit niedriger Kolmogorov-Komplexität — ein Angreifer, der den Algorithmus kennt, kann die Sequenz durch das kurze Programm beschreiben.",
        difficulty = 4,
        funFact = "Gregory Chaitin entwickelte eine verwandte Konstante Ω (Omega), die Haltwahrscheinlichkeit eines zufälligen Programms — sie ist absolut zufällig im Kolmogorov-Sinne und unberechenbar, verkörpert aber ein konkretes mathematisches Konzept."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Merkle-Damgård-Konstruktion' und welche Schwäche hat sie gegenüber neueren Designs?",
        answerA = "Eine Methode zur Schlüsselableitung, die anfällig für Timing-Angriffe ist",
        answerB = "Die Struktur von MD5 und SHA-1/2: Iterative Kompression von Blöcken mit einem internen Zustand — anfällig für Length-Extension-Angriffe, weil der finale Zustand als neuer Startzustand nutzbar ist",
        answerC = "Eine Blockchain-Datenstruktur für dezentrale Verifizierung",
        answerD = "Eine Methode zur sicheren Schlüsselspeicherung in HSMs",
        correctAnswer = 1,
        explanation = "Merkle-Damgård: H(M) = Compress(Compress(...Compress(IV, M₁), M₂)..., Mₙ). Length Extension: Kennt ein Angreifer H(M) und len(M), kann er H(M || padding || M') berechnen, ohne M zu kennen. SHA-3 (Keccak) verwendet eine 'Sponge-Konstruktion', die immun dagegen ist.",
        difficulty = 4,
        funFact = "Length-Extension-Angriffe wurden gegen APIs wie AWS S3 verwendet: Wenn eine Signatur HMAC(K, message) fälschlicherweise als H(K || message) implementiert wurde (kein echter HMAC), konnte ein Angreifer signierte Nachrichten um beliebige Daten verlängern."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 'Certificateless Cryptography' und welches Problem der traditionellen PKI löst sie?",
        answerA = "Ein System ohne digitale Zertifikate, das auf Passwörtern basiert",
        answerB = "Ein Schema (Al-Riyami, Paterson 2003), das das 'Key Escrow'-Problem der Identity-Based Cryptography löst: Schlüssel werden nicht von einer zentralen Autorität generiert, vermeidet aber auch die CA-Abhängigkeit traditioneller PKI",
        answerC = "Eine Methode zur Erzeugung selbstsignierter Zertifikate ohne CA",
        answerD = "Ein Protokoll zur gegenseitigen Authentifizierung ohne Zertifikatshierarchie",
        correctAnswer = 1,
        explanation = "Traditional PKI: CA-Abhängigkeit, teuer. Identity-Based Crypto (Boneh-Franklin): Key Generator Center (KGC) kennt alle privaten Schlüssel (Key Escrow). Certificateless Crypto: Das KGC erzeugt nur einen Teilschlüssel; der Nutzer fügt einen eigenen Anteil hinzu — weder CA noch KGC kennt den vollen privaten Schlüssel.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was beschreibt Shannons 'Rauschkanaltheorie' (Noisy Channel Theorem) und was ist sein zentrales Ergebnis?",
        answerA = "Dass Rauschen in Kommunikationskanälen immer zur Informationsvernichtung führt und nicht korrigierbar ist",
        answerB = "Für jeden Kanal mit Kapazität C > 0 existieren Codes, die bei beliebig niedriger Fehlerrate C-ε Bit/s übertragen können — perfekte fehlerfreie Übertragung ist möglich, wenn die Rate unter der Kanalkapazität liegt",
        answerC = "Dass die maximale Verschlüsselungsstärke durch das Rauschen eines Kanals begrenzt ist",
        answerD = "Rauschen erhöht die Entropie und macht Verschlüsselung wirkungslos",
        correctAnswer = 1,
        explanation = "Shannons zweites Kodierungstheorem (1948): Man kann Informationen fehlerlos durch einen verrauschten Kanal senden, wenn die Übertragungsrate < Kanalkapazität C. Es garantiert die Existenz solcher Codes — ohne sie zu konstruieren. Turbo-Codes (1993) und LDPC-Codes kommen in der Praxis auf Bruchteile eines dB an die Kapazität heran.",
        difficulty = 4,
        funFact = "Shannons Beweis war 'nicht-konstruktiv' — er zeigte nur, dass solche Codes existieren müssen, aber nicht wie man sie baut. Die praktische Konstruktion dauerte 45 Jahre und gilt als ein der größten Ingenieurleistungen der Nachrichtentechnik."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Bleichenbacher-Angriff' auf RSA-PKCS#1 v1.5 und welche Verbindung besteht zu DROWN und ROBOT?",
        answerA = "Ein Timing-Angriff auf die Schlüsselgenerierung von RSA",
        answerB = "Daniel Bleichenbacher (1998): Durch padding-oracle-artige Fehlerantworten bei RSA-PKCS#1 v1.5-verschlüsselten Nachrichten kann ein Angreifer Klartexte adaptiv entschlüsseln — DROWN (2016) und ROBOT (2017) sind moderne Varianten gegen SSLv2 bzw. TLS",
        answerC = "Ein Angriff auf die Primfaktorisierung durch geschickt gewählte Chiffrate",
        answerD = "Ein Seitenkanalangriff auf die Potenzierungsoperation bei der RSA-Entschlüsselung",
        correctAnswer = 1,
        explanation = "Bleichenbacher: Server verrät durch verschiedene Fehlercodes, ob das Padding korrekt war. Mit ~1 Million adaptiver Anfragen kann ein RSA-1024-Geheimtext entschlüsselt werden. ROBOT (2017) fand moderne Bleichenbacher-Oracles in Facebook, Citrix, F5 — alle direkt exploitierbar.",
        difficulty = 4,
        funFact = "Der Name ROBOT steht für 'Return Of Bleichenbacher's Oracle Threat'. Die Forscher fanden 27 der Top-100-HTTPS-Seiten verwundbar — nach fast 20 Jahren war die 1998 entdeckte Schwachstelle noch immer weit verbreitet."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist Quantenschlüsselverteilung (QKD, z. B. BB84) und worin liegt ihr fundamentaler Vorteil gegenüber klassischer Kryptographie?",
        answerA = "QKD ist schneller als klassische Schlüsselverteilung und benötigt keine Hardware",
        answerB = "BB84 (Bennett, Brassard 1984) nutzt Quantenmechanik: Das Abhören eines Quantenkanals stört messbar den Zustand der Photonen — Lauschangriffe werden physikalisch nachweisbar, nicht nur rechnerisch schwer",
        answerC = "QKD verwendet Shors Algorithmus rückwärts zur Schlüsselgenerierung",
        answerD = "Quantenkryptographie basiert auf der Unmöglichkeit, Quantenzustände zu kopieren, und ist deshalb schneller als RSA",
        correctAnswer = 1,
        explanation = "QKD-Sicherheit basiert auf Heisenbergs Unschärfeprinzip und dem No-Cloning-Theorem: Wer Photonen misst, verändert ihre Zustände. Sender und Empfänger können durch Vergleich einer Teilmenge ihrer Messergebnisse Lauschangriffe erkennen. Die Sicherheit ist informationstheoretisch — nicht rechenkomplexitätsbasiert.",
        difficulty = 4,
        funFact = "China betreibt seit 2017 das weltweit erste QKD-Satelliten-Netzwerk 'Mozi' (Micius). Es ermöglichte QKD-geschützte Videokonferenzen zwischen Peking und Wien über 7600 km — im September 2017 durchgeführt."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist das 'Discrete Logarithm Problem in Elliptic Curve Groups' (ECDLP) und warum ist es schwerer als das klassische DLP?",
        answerA = "Es ist identisch mit dem klassischen DLP, nur auf anderen mathematischen Strukturen",
        answerB = "Das ECDLP hat keinen bekannten subexponentiellen Algorithmus (kein Index-Calculus), während klassisches DLP durch Number Field Sieve in subexponentieller Zeit lösbar ist — deshalb reichen 256-Bit-ECC-Schlüssel für 128-Bit-Sicherheit",
        answerC = "ECDLP ist schwerer, weil elliptische Kurven über reellen Zahlen definiert sind und mehr Rechenoperationen benötigen",
        answerD = "Das ECDLP ist leichter lösbar als klassisches DLP, wird aber trotzdem verwendet",
        correctAnswer = 1,
        explanation = "Bester bekannter ECDLP-Algorithmus: Baby-Step-Giant-Step in O(√n). Bester DLP-Algorithmus: Number Field Sieve in sub-exponentieller Zeit L[1/3, c]. Daher: 256-Bit-ECC ≈ 128-Bit-Sicherheit ≈ 3072-Bit-RSA/DH. ECC ist bei gleichem Sicherheitsniveau durch viel kürzere Schlüssel deutlich effizienter.",
        difficulty = 4,
        funFact = null
    ),

)
