package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun techQuestionsExpert6(): List<Question> = listOf(

    // Question 1 — IoT: MQTT QoS
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Welche MQTT QoS-Stufe garantiert die Zustellung genau einmal (exactly-once delivery) und welches Handshake-Verfahren wird dabei verwendet?",
        answerA = "QoS 0 (At-most-once) mit einfachem PUBACK",
        answerB = "QoS 1 (At-least-once) mit PUBACK-Handshake",
        answerC = "QoS 2 (Exactly-once) mit PUBREC/PUBREL/PUBCOMP-Handshake",
        answerD = "QoS 3 (Guaranteed-once) mit CONFIRM/ACK-Handshake",
        correctAnswer = 2,
        explanation = "MQTT QoS 2 verwendet einen vierstufigen Handshake: Der Sender sendet PUBLISH, Empfaenger antwortet PUBREC, Sender sendet PUBREL, Empfaenger bestaetigt mit PUBCOMP. Erst dann gilt die Nachricht als endgueltig zugestellt.",
        difficulty = 4,
        funFact = "QoS 2 ist bis zu 4x langsamer als QoS 0, weil es 4 Nachrichten pro Nutzlast erfordert — in IoT-Netzwerken mit schlechter Konnektivitaet ist dieser Trade-off oft unvermeidlich."
    ),

    // Question 2 — IoT: CoAP vs MQTT
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist der fundamentale Unterschied zwischen CoAP und MQTT hinsichtlich Kommunikationsarchitektur und Transportprotokoll?",
        answerA = "CoAP ist Publish-Subscribe ueber TCP, MQTT ist Request-Response ueber UDP",
        answerB = "CoAP ist Request-Response ueber UDP mit RESTful-Semantik, MQTT ist Publish-Subscribe ueber TCP mit Broker",
        answerC = "CoAP verwendet AES-GCM-Verschluesselung, MQTT verwendet TLS 1.3",
        answerD = "CoAP ist nur fuer IPv6-Netzwerke geeignet, MQTT nur fuer IPv4",
        correctAnswer = 1,
        explanation = "CoAP (RFC 7252) folgt dem REST-Modell mit GET/POST/PUT/DELETE ueber UDP und ist fuer eingeschraenkte Geraete (6LoWPAN) optimiert. MQTT ist ein Broker-basiertes Pub/Sub-Protokoll ueber TCP, das fuer hohe Latenztoleranz ausgelegt ist.",
        difficulty = 4,
        funFact = "CoAP-Nachrichten sind im Schnitt nur 10-20 Bytes gross. MQTT-Headers beginnen bei 2 Bytes, aber der Broker-Overhead macht MQTT in Constrained-Netzwerken oft schwerer."
    ),

    // Question 3 — IoT: Thread-Protokoll
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Auf welchem IEEE-Standard basiert das Thread-Netzwerkprotokoll fuer IoT und welche Rolle spielt ein Border Router in einem Thread-Mesh?",
        answerA = "IEEE 802.11 (WiFi); Border Router verbindet Thread-Mesh mit WLAN",
        answerB = "IEEE 802.15.1 (Bluetooth LE); Border Router uebersetzt zwischen BLE und IP",
        answerC = "IEEE 802.3 (Ethernet); Border Router isoliert Thread-Subnetz vom Internet",
        answerD = "IEEE 802.15.4; Border Router verbindet das IPv6-Thread-Mesh mit externen IP-Netzwerken",
        correctAnswer = 3,
        explanation = "Thread verwendet IEEE 802.15.4 als Physical/MAC-Layer und baut darauf ein selbstheilendes IPv6-Mesh-Netzwerk. Der Border Router vermittelt zwischen dem Thread-Mesh und externen IPv6/IPv4-Netzwerken mittels NAT64 oder Dual-Stack.",
        difficulty = 4,
        funFact = "Thread wurde 2014 von Google, ARM, Samsung u.a. gegruendet. Apple HomeKit, Amazon Echo und Google Nest nutzen Thread als Basis fuer Matter-Geraete."
    ),

    // Question 4 — IoT: Zigbee Topologie
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen einem Zigbee-Coordinator, einem Router und einem End-Device in einer Zigbee-Netzwerktopologie?",
        answerA = "Coordinator gruendet das Netzwerk und vergibt PAN-ID; Router leitet Pakete weiter; End-Device schlaeft und kommuniziert nur mit Parent",
        answerB = "Coordinator leitet Pakete weiter, Router vergibt Adressen, End-Device speichert Routing-Tabellen",
        answerC = "Alle drei Rollen sind identisch — die Bezeichnungen beschreiben nur physische Positionen",
        answerD = "Router und Coordinator koennen nicht gleichzeitig im selben PAN existieren",
        correctAnswer = 0,
        explanation = "Im Zigbee-Netzwerk gruendet der Coordinator das PAN und vergibt PAN-IDs. Router verlaengern die Reichweite durch Paketweiterleitung und bleiben dauerhaft empfangsbereit. End-Devices koennen schlafen (RFD) und kommunizieren ausschliesslich ueber ihren Parent-Router.",
        difficulty = 4,
        funFact = "Zigbee-Netzwerke koennen theoretisch bis zu 65.000 Knoten umfassen. In der Praxis sind 200-300 Geraete pro Coordinator das Maximum fuer stabile Performance."
    ),

    // Question 5 — Edge Computing: Cloudlet
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Was versteht man unter dem Begriff 'Cloudlet' im Kontext von Mobile Edge Computing und welches Latenzproblem loest es?",
        answerA = "Ein Cloudlet ist ein dedizierter GPU-Cluster in Hyperscaler-Rechenzentren",
        answerB = "Ein Cloudlet ist ein virtueller Container fuer IoT-Geraete ohne eigene Rechenkapazitaet",
        answerC = "Ein Cloudlet ist ein kleines, dezentrales Rechenzentrum am Netzwerkrand, das Offloading von Rechenaufgaben von Mobilgeraeten mit Sub-10ms-Latenz ermoeoerlicht",
        answerD = "Ein Cloudlet bezeichnet eine Gruppe von maximal 5 Mikroservices in einer Kubernetes-Umgebung",
        correctAnswer = 2,
        explanation = "Cloudlets sind ressourcenreiche Rechner in der Naehe des Nutzers (z.B. in WLAN-Basisstationen), die rechenintensive Tasks von Smartphones uebernehmen. Sie adressieren das RTT-Problem zu fernen Cloud-Servern, wo Latenz ueber 50ms viele Echtzeitanwendungen unbrauchbar macht.",
        difficulty = 4,
        funFact = "Mahadev Satyanarayana von CMU praegte den Begriff 'Cloudlet' 2009. Das Konzept gilt als Vorlaeufer der heutigen ETSI-MEC-Spezifikation."
    ),

    // Question 6 — Edge Computing: KubeEdge
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist KubeEdge und wie unterscheidet es sich architekturell von einem Standard-Kubernetes-Cluster?",
        answerA = "KubeEdge ist eine vereinfachte Kubernetes-Distribution ausschliesslich fuer ARM-Prozessoren",
        answerB = "KubeEdge ersetzt etcd durch SQLite und kubelet durch einen eigenen Prozess ohne Container-Runtime",
        answerC = "KubeEdge ist ein CNCF-Projekt das Kubernetes ausschliesslich auf FPGA-Hardware portiert",
        answerD = "KubeEdge erweitert Kubernetes auf Edge-Knoten mit dem EdgeCore-Daemon, der auch bei unterbrochener Cloud-Verbindung Workloads autonom verwaltet",
        correctAnswer = 3,
        explanation = "KubeEdge teilt sich in CloudCore (in der Cloud) und EdgeCore (auf Edge-Geraeten) auf. Der EdgeCore-Daemon enthaelt einen Metamanager, der Workload-Status lokal haelt und Pods autonom verwaltet, wenn die Verbindung zur Cloud unterbrochen ist.",
        difficulty = 4,
        funFact = "KubeEdge kann auf Geraeten mit nur 256 MB RAM laufen. Standard-Kubernetes benoetigt mindestens 2 GB — damit ist KubeEdge 8x ressourcenschonender."
    ),

    // Question 7 — WebAssembly: Speichermodell
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Wie verwaltet WebAssembly seinen Speicher und welche Sicherheitseigenschaft hat das WebAssembly-Speichermodell gegenueber nativem Code?",
        answerA = "Wasm besitzt einen linearen Speicher als zusammenhaengendes ArrayBuffer-Array; alle Zugriffe werden durch den Runtime auf Grenzen geprueft, was Buffer-Overflows in den Host-Stack verhindert",
        answerB = "Wasm verwendet den nativen OS-Heap direkt mit mmap(), ohne zusaetzliche Sicherheitsschicht",
        answerC = "Wasm-Module haben keinen eigenen Speicher — sie greifen nur ueber JavaScript-Objekte auf DOM-Daten zu",
        answerD = "Wasm verwendet WASI-Capabilities fuer jeden Speicherzugriff, was 10x langsamer ist als nativer Code",
        correctAnswer = 0,
        explanation = "WebAssembly-Module operieren auf einem isolierten linearen Speicherbereich. Alle Speicherzugriffe werden zur Laufzeit gegen die deklarierten Grenzen geprueft. Ein Wasm-Modul kann niemals auf den Host-Prozessstack, andere Module oder den Browser-Heap zugreifen.",
        difficulty = 4,
        funFact = "Wasm's linearer Speicher beginnt bei 0 und kann in 64-KB-Seiten dynamisch mit memory.grow() erweitert werden — bis maximal 4 GB bei 32-Bit-Indexierung."
    ),

    // Question 8 — WebAssembly: WASI
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist das Ziel von WASI (WebAssembly System Interface) und welches Sicherheitsmodell implementiert es?",
        answerA = "WASI ist eine Browser-API fuer schnelle DOM-Manipulation durch Wasm-Module",
        answerB = "WASI definiert eine plattformunabhaengige Systemschnittstelle fuer Wasm ausserhalb des Browsers mit Capability-basiertem Sicherheitsmodell",
        answerC = "WASI ist ein Standard fuer WebAssembly-zu-JavaScript-Interoperabilitaet innerhalb von Node.js",
        answerD = "WASI implementiert POSIX-Syscalls direkt in WebAssembly ohne Sandbox-Einschraenkungen",
        correctAnswer = 1,
        explanation = "WASI ermoeglicht Wasm-Module ausserhalb des Browsers (Server, Edge, Embedded). Das Capability-Modell bedeutet: Ein Modul bekommt nur explizit gewaehlte Ressourcen als Handles — kein globaler Zugriff wie bei POSIX.",
        difficulty = 4,
        funFact = "Solomon Hykes (Docker-Gruender) schrieb 2019: 'If WASM+WASI had existed in 2008, we would not have needed Docker.' — Wasm-Container sind portabler als OCI-Images."
    ),

    // Question 9 — WebAssembly: Component Model
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Was ist das WebAssembly Component Model und welches Problem loest es gegenueber einfachen Wasm-Modulen?",
        answerA = "Das Component Model definiert eine einheitliche Schnittstelle fuer Wasm-GPU-Zugriff in Browsern",
        answerB = "Das Component Model standardisiert die Groesse von Wasm-Seiten von 64 KB auf 4 MB fuer bessere Performance",
        answerC = "Das Wasm Component Model fuegt strukturierte Heap-Typen via WIT-Interface hinzu; Kotlin/Dart konnten zuvor nur mit eigenem GC im linearen Speicher laufen — jetzt nutzen sie den nativen Browser-GC",
        answerD = "Das Component Model ist eine Erweiterung fuer SIMD-Instruktionen in WebAssembly 2.0",
        correctAnswer = 2,
        explanation = "Das Wasm Component Model (via WIT — WebAssembly Interface Types) loest das Problem der Sprach-Interoperabilitaet: Zwei Wasm-Module koennen typsicher miteinander kommunizieren, ohne Details ueber Speicherlayout oder ABI zu kennen. Jede Komponente hat ihren eigenen linearen Speicher.",
        difficulty = 4,
        funFact = "Vor dem Component Model mussten Entwickler manuelle 'glue code'-Schichten schreiben um String-Daten zwischen Wasm-Modulen zu uebergeben, da Wasm nur Integer und Floats als Typen kennt."
    ),

    // Question 10 — GraphQL: N+1 Problem
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist das N+1-Problem in GraphQL und welche Loesung adressiert es auf Server-Seite?",
        answerA = "GraphQL sendet N+1 HTTP-Anfragen fuer jeden verschachtelten Query-Knoten — geloest durch HTTP/2 Multiplexing",
        answerB = "GraphQL-Resolver werden N+1 mal aufgerufen wenn N Clients gleichzeitig die gleiche Query senden — geloest durch Query Deduplication",
        answerC = "Das N+1-Problem tritt auf wenn GraphQL-Subscriptions mehr als N Felder pro Event zurueckgeben",
        answerD = "Bei verschachtelten Queries werden N+1 Datenbankabfragen ausgefuehrt (1 fuer Parent + N fuer Child) — geloest durch DataLoader-Batching",
        correctAnswer = 3,
        explanation = "Beim N+1-Problem wird fuer eine Liste von N Parent-Objekten jeweils ein separater DB-Query fuer deren Child-Daten ausgefuehrt. DataLoader (Facebook, 2015) loest dies durch Batching: Alle Child-Anfragen innerhalb eines Event-Loop-Ticks werden zu einer einzigen Batch-Abfrage zusammengefasst.",
        difficulty = 4,
        funFact = "DataLoader wurde urspruenglich von Facebook entwickelt um Datenbank-Roundtrips bei der News-Feed-Generierung zu reduzieren — von Tausenden auf einige Dutzend pro Request."
    ),

    // Question 11 — GraphQL: Persisted Queries
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was sind 'Persisted Queries' in GraphQL und welchen Performance- und Sicherheitsvorteil bieten sie?",
        answerA = "Persisted Queries erlauben Clients, nur einen Hash statt des vollstaendigen Query-Strings zu senden; der Server laedt den Query aus einem Store — reduziert Bandbreite und verhindert beliebige Ad-hoc-Queries",
        answerB = "Persisted Queries speichern Query-Ergebnisse im Server-Cache fuer 24 Stunden",
        answerC = "Persisted Queries aktivieren automatisch Query-Batching fuer alle GraphQL-Subscriptions",
        answerD = "Persisted Queries sind eine Apollo-proprietary Erweiterung inkompatibel mit federierten GraphQL-Schemas",
        correctAnswer = 0,
        explanation = "Mit Automatic Persisted Queries (APQ) sendet der Client beim ersten Request den Query-Hash; fehlt er im Server-Store, uebertraegt der Client den vollstaendigen Query einmalig. Danach reicht der Hash. Das verbessert Bandbreite und erlaubt Whitelisting — nur registrierte Queries werden ausgefuehrt.",
        difficulty = 4,
        funFact = "Bei grossen GraphQL-APIs (z.B. Shopify) koennen Query-Strings 50 KB gross werden. Persisted Queries reduzieren diese auf 64-Byte-SHA256-Hashes — eine 800-fache Bandbreiteneinsparung."
    ),

    // Question 12 — GraphQL: Federation
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist Apollo Federation und wie werden Typen ueber mehrere Subgraphen hinweg aufgeloest?",
        answerA = "Apollo Federation ist eine Technik zum Sharding eines einzigen GraphQL-Schemas auf mehrere Datenbank-Shards",
        answerB = "Apollo Federation ermoeglicht mehreren unabhaengigen GraphQL-Services (Subgraphen), Typen mit @key-Direktiven zu deklarieren; ein Gateway plant Queries und leitet Teilabfragen an die zustaendigen Subgraphen weiter",
        answerC = "Apollo Federation verwendet REST-Endpoints als Datensource fuer jeden Subgraphen und wandelt sie automatisch in GraphQL um",
        answerD = "Apollo Federation ersetzt Resolver durch deklarative Datenbankabfragen aehnlich wie Hasura",
        correctAnswer = 1,
        explanation = "Mit @key-Direktiven deklarieren Subgraphen, welche Felder einen Typ eindeutig identifizieren (z.B. @key(fields: 'id')). Der Federation Router (Query Planner) zerlegt eine eingehende Query in Teilabfragen, die er parallel an die zustaendigen Subgraphen sendet und die Ergebnisse zusammenfuehrt.",
        difficulty = 4,
        funFact = "Airbnb, Netflix und GitHub nutzen GraphQL Federation um Hunderte von Microservices hinter einem einzigen oeffentlichen GraphQL-Schema zu vereinen — ohne dass ein Team Zugriff auf alle Services braucht."
    ),

    // Question 13 — Rust: Non-Lexical Lifetimes
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Was ist 'Non-Lexical Lifetimes' (NLL) in Rust und welches Problem des urspruenglichen Lifetime-Systems hat es geloest?",
        answerA = "NLL erlaubt Rust-Variablen ausserhalb ihrer geschweiften Klammern zu leben",
        answerB = "NLL ist ein Compiler-Flag das saemtliche Lifetime-Annotationen optional macht",
        answerC = "NLL berechnet Lifetimes anhand des tatsaechlichen Kontrollflusses (CFG) statt syntaktischer Scope-Grenzen, was false positives beim Borrow-Checker eliminiert",
        answerD = "NLL aktiviert automatische Garbage Collection fuer Rust-Heap-Objekte in bestimmten Kontexten",
        correctAnswer = 2,
        explanation = "Vor NLL (Rust 2018) pruefe der Borrow-Checker Lifetimes anhand syntaktischer Block-Grenzen. NLL analysiert den Control Flow Graph und bestimmt praezise, wann eine Variable zuletzt benutzt wird. Dadurch verschwanden viele korrekte Programme die vorher als 'unsicher' abgelehnt wurden.",
        difficulty = 4,
        funFact = "NLL wurde von Niko Matsakis (Rust-Kernteam) entwickelt und in einem beruehmten Blog-Post 'NLL RFC' 2016 vorgeschlagen. Die vollstaendige Implementierung dauerte zwei Jahre."
    ),

    // Question 14 — Rust: Async Runtime
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Warum implementiert Rust kein eigenes Async-Runtime in der Standardbibliothek und was sind die Konsequenzen fuer Entwickler?",
        answerA = "Rusts Standardbibliothek ist zu klein fuer ein vollstaendiges Async-Runtime — es wird in Rust 2025 hinzugefuegt",
        answerB = "Rust-Futures koennen nur in Single-Threaded-Umgebungen ausgefuehrt werden, weshalb kein Runtime benoetigt wird",
        answerC = "Das Fehlen eines Standard-Runtimes ist ein bekanntes Versehen — alle Rust-Futures blockieren den aktuellen Thread",
        answerD = "Rust definiert nur die Future-Trait und Waker-Infrastruktur in std; konkrete Runtimes (tokio, async-std, smol) sind externe Crates, was maximale Flexibilitaet fuer verschiedene Umgebungen ermoeglicht",
        correctAnswer = 3,
        explanation = "Rust standardisiert bewusst nur die abstrakte Schnittstelle (Future-Trait, Waker, Poll). Das ermoeglicht spezialisierte Runtimes: tokio fuer hochskalierbare Netzwerkanwendungen, embassy fuer Embedded-Systeme ohne OS, wasm-bindgen-futures fuer Browser-WASM.",
        difficulty = 4,
        funFact = "tokio.rs verarbeitet in Benchmarks ueber 1 Million TCP-Verbindungen gleichzeitig auf einem einzigen Server — vergleichbar mit nginx. Gegruesst von der Zero-Cost-Abstraction-Philosophie Rusts."
    ),

    // Question 15 — Rust: Unsafe Operationen
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Welche fuenf Operationen sind in Rust ausschliesslich in 'unsafe'-Bloecken erlaubt?",
        answerA = "Raw-Pointer-Dereferenzierung, unsichere Funktion aufrufen, unsichere Traits implementieren, Zugriff auf statische mut-Variablen, Union-Felder lesen",
        answerB = "Multithreading, Datenbankzugriffe, Datei-I/O, Netzwerkkommunikation, FFI-Aufrufe",
        answerC = "Arithmetik mit Overflow, Bit-Shifts, Casting zwischen Integer-Typen, Inline-Assembly, Syscalls",
        answerD = "Box<T>-Allokation, Rc<T>-Erzeugen, Mutex-Locking, Channel-Kommunikation, dynamischer Dispatch",
        correctAnswer = 0,
        explanation = "Rusts 'unsafe' Superset umfasst genau fuenf Kategorien: (1) Raw-Pointer dereferenzieren, (2) unsafe-Funktionen aufrufen, (3) unsafe-Traits implementieren (z.B. Send/Sync), (4) auf globale mut-Statics zugreifen, (5) Union-Felder lesen.",
        difficulty = 4,
        funFact = "In Crates.io sind ~23% aller Crates enthalten unsafe-Code. Dennoch ist der Grossteil des sichtbaren Rust-Ecosystems 'safe' — unsafe ist in Bibliotheken gekapselt und nach aussen hin verborgen."
    ),

    // Question 16 — Go: GMP-Scheduler
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Wie funktioniert der Go-Scheduler (GMP-Modell) und welche Rolle spielen M, G und P?",
        answerA = "M=Memory-Pool, G=Goroutine-Queue, P=Processor-Cache; alle drei sind global geteilt",
        answerB = "G=Goroutine, M=OS-Thread (Machine), P=Logical Processor (Context); P haelt eine lokale Run-Queue; M fuehrt G aus wenn an P gebunden; Work-Stealing sorgt fuer Lastausgleich",
        answerC = "M=Mutex, G=Go-Channel, P=Panic-Handler; zusammen implementieren sie Goroutine-Safety",
        answerD = "GMP ist eine externe Bibliothek — Go verwendet standard POSIX-Threads",
        correctAnswer = 1,
        explanation = "Im GMP-Modell haelt jedes P eine lokale Goroutine-Run-Queue (maximal 256 Eintraege). M-Threads fuehren Goroutinen aus, wenn sie an P gebunden sind. Wenn ein P seine Queue leert, stiehlt es die Haelfte der Queue eines anderen P (Work-Stealing).",
        difficulty = 4,
        funFact = "Go kann Hunderttausende von Goroutinen simultan verwalten, da jede Goroutine nur 2-8 KB Stack benoetigt (vs. 1-8 MB fuer OS-Threads). Der Stack waechst dynamisch bei Bedarf."
    ),

    // Question 17 — Go: Garbage Collector
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Welchen Garbage-Collection-Algorithmus verwendet die Go-Runtime und welche Technik minimiert Stop-the-World-Pausen?",
        answerA = "Go verwendet Reference Counting (wie Swift) mit automatischem Retain/Release",
        answerB = "Go verwendet generationellen GC mit Eden/Survivor-Raeumen wie der Java JVM",
        answerC = "Go verwendet concurrent Tri-Color-Mark-Sweep-GC; Schreibbarrieren halten den Invariant aufrecht, dass kein weisses Objekt von einem schwarzen direkt referenziert wird — STW-Pausen meist unter 500 Mikrosekunden",
        answerD = "Go hat keinen GC — der Entwickler muss Speicher manuell mit runtime.Free() freigeben",
        correctAnswer = 2,
        explanation = "Gos GC laueft groesstenteils concurrent mit den Goroutinen. Die Schreibbarriere (Dijkstra-Yuasa Write Barrier) stellt sicher, dass neu erzeugte Referenzen korrekt markiert werden. Die zwei kurzen STW-Pausen (Mark-Start, Mark-Term) sind seit Go 1.14 unter 500 Mikrosekunden.",
        difficulty = 4,
        funFact = "Go 1.5 (2015) war ein Meilenstein: Die GC-Pausen wurden von 50-300ms auf unter 1ms reduziert — eine 50-300-fache Verbesserung — durch pure Algorithmus-Optimierung ohne Generational GC."
    ),

    // Question 18 — Compiler: SCCP
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist 'Sparse Conditional Constant Propagation' (SCCP) in SSA-Form und welchen Vorteil hat sie gegenueber einfacher Constant Propagation?",
        answerA = "SCCP ersetzt Konstanten nur in Schleifen, nicht in bedingten Anweisungen",
        answerB = "SCCP ist eine Post-Link-Optimierung die ausschliesslich auf Maschinencode angewendet wird",
        answerC = "SCCP steht fuer 'Synchronized Cache Coherence Protocol' und ist keine Compiler-Optimierung",
        answerD = "SCCP analysiert gleichzeitig Kontrollfluesse und Datenfluesse — es propagiert Konstanten nur in erreichbaren Code-Pfaden und eliminiert dabei toten Code simultan",
        correctAnswer = 3,
        explanation = "Klassische Constant Propagation behandelt Kontrollfluss und Datenfluss getrennt. SCCP (Wegman & Zadeck 1991) kombiniert beides: Es markiert Basisbloecke als erreichbar oder nicht erreichbar und propagiert Konstantenwerte nur durch erreichbare Pfade. So koennen z.B. if(DEBUG) Bloecke vollstaendig eliminiert werden.",
        difficulty = 4,
        funFact = "SCCP ist der Standardalgorithmus in LLVM fuer Constant Propagation. Er loescht in typischen Programmen 10-20% des Dead Code — Code der geschrieben aber nie ausgefuehrt wuerde."
    ),

    // Question 19 — Compiler: Alias-Analyse
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen May-Alias und Must-Alias in der Pointer-Alias-Analyse eines Compilers?",
        answerA = "May-Alias bedeutet Zeiger KOENNEN auf dasselbe Objekt zeigen (unsicher); Must-Alias bedeutet Zeiger zeigen IMMER auf dasselbe Objekt — nur Must-Alias erlaubt sichere Optimierungen wie Redundancy-Elimination",
        answerB = "May-Alias bedeutet Zeiger zeigen immer auf dasselbe Objekt; Must-Alias bedeutet sie zeigen moeglicherweise auf dasselbe Objekt",
        answerC = "May-Alias ist eine inter-prozedurale Analyse; Must-Alias ist eine intra-prozedurale Analyse",
        answerD = "Beide Begriffe sind synonym — die Unterscheidung wird in modernen Compilern nicht mehr gemacht",
        correctAnswer = 0,
        explanation = "May-Alias ist eine konservative Ueberapproximation: Der Compiler geht davon aus, dass zwei Zeiger moeglicherweise aliasieren, und verhindert Optimierungen. Must-Alias ist eine praezise Information: Zwei Zeiger zeigen definitiv auf dasselbe Objekt — nutzbar fuer Copy-Propagation und Dead-Store-Elimination.",
        difficulty = 4,
        funFact = "Das Alias-Analyse-Problem ist im Allgemeinen unentscheidbar (Rice's Theorem). Alle praxistauglichen Analysen sind deshalb entweder May-Alias (konservativ) oder Must-Alias (unter-approx.) — niemals exakt."
    ),

    // Question 20 — Compiler: Inlining-Heuristik
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Warum koennen zu aggressive Inlining-Heuristiken in Compilern die Laufzeitperformance verschlechtern?",
        answerA = "Inlining erhoeht den Speicherverbrauch des Compilers und verlaengert die Compilation, hat aber keinen Laufzeiteffekt",
        answerB = "Exzessives Inlining kann I-Cache-Thrashing verursachen: Groessere Funktionen passen nicht mehr in den Instruction Cache, was zu mehr Cache-Misses und damit langsamerem Code fuehrt",
        answerC = "Inlining verhindert SIMD-Vektorisierung weil Schleifen durch den Inlining-Prozess aufgebrochen werden",
        answerD = "Inlining ist generell schaedlich — moderne Compiler deaktivieren es standardmaessig mit -O2",
        correctAnswer = 1,
        explanation = "Funktionsinlining eliminiert Call-Overhead und ermoeglicht weitere Optimierungen wie Constant Folding. Aber exzessives Inlining blaehft den Code auf. Wenn Hot-Paths nicht mehr in den L1-Instruction-Cache (typisch 32 KB) passen, verursacht jede Iteration Cache-Misses — das kostet 100-200 CPU-Zyklen pro Miss.",
        difficulty = 4,
        funFact = "GCC und Clang verwenden Heuristiken mit einem 'Inlining-Budget'. LLVM's Standard-Limit ist 225 IR-Instruktionen — groessere Funktionen werden nur inlined wenn sie 'heiss' (oft aufgerufen) sind."
    ),

    // Question 21 — FPGA: SRAM vs Antifuse
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen SRAM-basierten FPGAs und antifuse-basierten FPGAs hinsichtlich Sicherheit und Rekonfigurierbarkeit?",
        answerA = "SRAM-FPGAs sind permanent programmiert; antifuse-FPGAs sind unbegrenzt rekonfigurierbar",
        answerB = "Beide Technologien sind funktional identisch — der Unterschied liegt nur im Gehaeuse und der Leistungsaufnahme",
        answerC = "SRAM-FPGAs verlieren ihre Konfiguration beim Abschalten und sind unbegrenzt rekonfigurierbar aber anfaellig fuer Bitstream-Readout; Antifuse-FPGAs sind einmalig programmierbar (OTP), nicht auslesbar und strahlungstoleranter",
        answerD = "Antifuse-FPGAs sind ausschliesslich fuer Audioverarbeitung geeignet; SRAM-FPGAs fuer Netzwerk-Infrastruktur",
        correctAnswer = 2,
        explanation = "SRAM-FPGAs speichern ihre Konfiguration in fluechtigem SRAM — bei Stromverlust muss der Bitstream neu geladen werden. Antifuse-FPGAs (z.B. Microsemi ProASIC) schmelzen physische Verbindungen beim Programmieren — einmalig, nicht auslesbar, resistent gegen Single-Event-Upsets.",
        difficulty = 4,
        funFact = "Antifuse-FPGAs werden in Raumfahrt, Militaer und medizinischen Implantaten eingesetzt. Microsemis RTG4-Familie haelt bis zu 100 krad TID aus."
    ),

    // Question 22 — FPGA: HLS
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist High-Level Synthesis (HLS) fuer FPGAs und welche Einschraenkung hat sie gegenueber manuell geschriebenem RTL?",
        answerA = "HLS erzeugt automatisch Testbenches aus C-Code, aber keinen synthesefaehigen RTL-Code",
        answerB = "HLS unterstuetzt ausschliesslich datenflussorientierte Algorithmen wie FFT; Kontrollfluss-lastige Designs sind nicht moeglich",
        answerC = "HLS-generiertes RTL kann nicht in Timing-Analyse-Werkzeuge eingespeist werden",
        answerD = "HLS (z.B. Vitis HLS) uebersetzt C/C++ in RTL; der generierte RTL ist oft 2-5x groesser und langsamer als handoptimiertes RTL, da Synthesewerkzeuge Mikroarchitektur-Entscheidungen konservativ treffen",
        correctAnswer = 3,
        explanation = "HLS abstrahiert den Hardware-Entwurf auf Algorithmusebene. Der Compiler entscheidet ueber Scheduling, Binding und Interface-Synthesis. Der Trade-off: HLS ist 5-10x schneller in der Entwicklung, erzeugt aber typischerweise 2-3x mehr LUTs als handoptimiertes RTL.",
        difficulty = 4,
        funFact = "AMD/Xilinx Vitis HLS ist aus 'AutoESL AutoPilot' entstanden, das 2011 uebernommen wurde. Heute werden ~30% aller Xilinx-Designs teilweise mit HLS entwickelt."
    ),

    // Question 23 — FPGA: Timing Closure
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was versteht man unter 'Timing Closure' in der FPGA-Implementierung und welche Techniken werden eingesetzt wenn sie nicht erreicht wird?",
        answerA = "Timing Closure bedeutet alle Setup- und Hold-Zeitbedingungen fuer den gewuenschten Takt sind erfuellt; bei negativer Slack werden Retiming, Pipeline-Register, Placement-Constraints und Taktfrequenz-Reduzierung eingesetzt",
        answerB = "Timing Closure bedeutet dass alle Flip-Flops synchron getaktet werden — bei Misserfolg wird die Taktfrequenz erhoeht",
        answerC = "Timing Closure ist das Abschliessen der Routing-Phase — bei Misserfolg wird der Bitstream trotzdem generiert",
        answerD = "Timing Closure bezeichnet den Abschluss der Simulation vor der physischen Implementierung",
        correctAnswer = 0,
        explanation = "Timing-Verletzungen entstehen wenn kombinatorische Pfade laenger sind als die Taktperiode minus Setup-Zeit. Loesungsstrategien: Retiming (Register verschieben), Pipeline-Einfuegen (Pfad aufteilen), Placement-Constraints (kritische Pfade nah beieinander platzieren) oder Taktfrequenz reduzieren.",
        difficulty = 4,
        funFact = "In grossen FPGA-Designs (1+ Millionen LUTs) dauert ein vollstaendiger Place-and-Route-Durchlauf 6-12 Stunden. Timing-Closure ist oft ein iterativer Prozess ueber mehrere Tage."
    ),

    // Question 24 — Echtzeitsysteme: RMS vs EDF
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen Rate Monotonic Scheduling (RMS) und Earliest Deadline First (EDF) in Echtzeitsystemen?",
        answerA = "RMS ist dynamisch (Prioritaet aendert sich zur Laufzeit); EDF ist statisch (feste Prioritaeten)",
        answerB = "RMS weist statische Prioritaeten basierend auf Perioden zu (kuerzere Periode = hoehere Prioritaet); EDF ist dynamisch (hoehere Prioritaet fuer naechste Deadline); EDF ist CPU-Auslastung-optimal (100% theoretisch), RMS bis 69,3%",
        answerC = "RMS garantiert Zero-Jitter fuer alle Tasks; EDF erlaubt bis zu 10% Jitter pro Periode",
        answerD = "EDF ist nur in Single-Core-Echtzeitsystemen anwendbar; RMS unterstuetzt Multicore",
        correctAnswer = 1,
        explanation = "RMS (Liu & Layland 1973): Statische Prioritaeten proportional zu 1/Periode — optimal unter statischen Algorithmen. Auslastungs-Upper-Bound: n*(2^(1/n)-1) → 69,3% bei n→unendlich. EDF: Dynamische Prioritaeten basierend auf absoluter Deadline — theoretisch 100% Auslastung erreichbar.",
        difficulty = 4,
        funFact = "EDF wird in industriellen Echtzeitsystemen seltener eingesetzt als RMS — der Grund ist das Verhalten bei Ueberlast: EDF kann unvorhersehbar viele Tasks verpassen, RMS versagt deterministisch bei niedrig-prioren Tasks."
    ),

    // Question 25 — Embedded: Cortex-M Interrupt
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Was sind die Hauptfaktoren die die Interrupt-Latenz in ARM Cortex-M-Mikrocontrollern beeinflussen und was ist 'Interrupt Tail-Chaining'?",
        answerA = "Interrupt-Latenz wird ausschliesslich durch die CPU-Frequenz bestimmt; Tail-Chaining ist eine Methode zum Senden von Interrupts ueber UART",
        answerB = "ARM Cortex-M hat eine konstante Interrupt-Latenz von exakt 12 Taktzyklen — unabhaengig von Speicher-Wait-States",
        answerC = "Interrupt-Latenz wird durch Pipeline-Flushes, Register-Stacking, NVIC-Arbitrierung und Speicher-Wait-States beeinflusst; Tail-Chaining vermeidet vollstaendiges Unstacking/Stacking wenn mehrere ISRs aufeinander folgen",
        answerD = "Tail-Chaining ist eine Technik zum Verketten von DMA-Transfers ohne CPU-Beteiligung",
        correctAnswer = 2,
        explanation = "Cortex-M Interrupt-Response: 12 Zyklen Minimum (kein Wait-State, Pipeline geflusht, 8 Register automatisch auf Stack gepusht). Tail-Chaining: Wenn eine neue ISR ausloest waehrend eine andere endet, spart der Prozessor das Unstacking/Stacking und wechselt direkt zur naechsten ISR (~6 Zyklen statt 12+12).",
        difficulty = 4,
        funFact = "Die automatische Register-Stacking-Hardwareeinheit des Cortex-M wurde speziell fuer C-ABI-Kompatibilitaet designed — Interrupt-Handler koennen direkt als normale C-Funktionen geschrieben werden."
    ),

    // Question 26 — Embedded: DMA Double-Buffering
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist Double-Buffering im DMA-Kontext bei Embedded-Systemen und wie vermeidet es Data-Race-Bedingungen?",
        answerA = "Double-Buffering verdoppelt die DMA-Bandbreite durch zwei simultane DMA-Channels auf denselben Speicherbereich",
        answerB = "Double-Buffering ist eine Hardware-Funktion des Cortex-M7-Cache die automatisch aktiviert wird wenn DMA aktiv ist",
        answerC = "Double-Buffering verhindert DMA-Bursts laenger als 256 Bytes durch aufteilen in zwei Haelften",
        answerD = "Double-Buffering verwendet zwei alternierende Puffer: Waehrend der DMA-Controller in Puffer A schreibt, verarbeitet die CPU Puffer B; nach Puffer-voll-Interrupt tauschen die Rollen — kein gleichzeitiger Zugriff auf denselben Puffer",
        correctAnswer = 3,
        explanation = "Durch den alternierenden Doppelpuffer-Mechanismus greifen DMA und CPU niemals gleichzeitig auf denselben Speicher zu. Der DMA-Interrupt signalisiert den Puffer-Wechsel. Ohne Puffer-Synchronisation koennte die CPU gecachte Daten lesen bevor der DMA-Write komplett ist.",
        difficulty = 4,
        funFact = "STM32 MCUs haben dedizierte Hardware-Unterstuetzung fuer Double-Buffered DMA: Der DMA-Controller wechselt automatisch zwischen zwei Base-Adressen ohne CPU-Eingriff — was Zero-Latency-Streaming ermoeglicht."
    ),

    // Question 27 — Echtzeitsysteme: WCET
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Warum ist die WCET-Analyse (Worst-Case Execution Time) auf modernen Out-of-Order-CPUs fundamental schwieriger als auf einfachen in-order Prozessoren?",
        answerA = "Out-of-Order-CPUs mit spekulativer Ausfuehrung, Branch Prediction und Cache-Hierarchien erzeugen nicht-deterministisches Timing; Cache-Zustand haengt von Ausfuehrungshistorie ab — WCET-Berechnung ohne praezises Hardware-Modell ist unmoeolich",
        answerB = "Out-of-Order-CPUs sind schneller, weshalb WCET-Berechnungen immer zu konservativ sind",
        answerC = "WCET-Analyse ist auf Out-of-Order-CPUs identisch einfach — nur die absolute Zeit aendert sich",
        answerD = "Out-of-Order-CPUs garantieren deterministisches Timing durch ihren Reorder-Buffer",
        correctAnswer = 0,
        explanation = "In-Order-Prozessoren (z.B. ARM Cortex-M) haben deterministisches Timing: Jede Instruktion hat bekannte Zyklen. Out-of-Order-CPUs mit Cache (mehrere Ebenen), Branch Prediction und superscalar Dispatch machen das Timing vom Systemzustand abhaengig — WCET muss alle moeglichen Cache-Zustaende beruecksichtigen.",
        difficulty = 4,
        funFact = "Safety-critical Luftfahrtsysteme (DO-178C) verwenden deshalb oft veraltete Prozessoren wie den PowerPC 750 (G3, Baujahr 1997) — nicht wegen Geschwindigkeit, sondern wegen vollstaendig berechenbarem Timing."
    ),

    // Question 28 — Echtzeitsysteme: Priority Inversion
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist Priority Inversion in Echtzeitsystemen und welche zwei Protokolle loesen es?",
        answerA = "Priority Inversion tritt auf wenn Interrupts hoehere Prioritaet als RTOS-Tasks haben; Loesung: Interrupt-Masking und Task-Locking",
        answerB = "Priority Inversion tritt auf wenn ein hoch-priorer Task von einem niedrig-prioren Task blockiert wird (Mutex gehalten); Loesung: Priority Inheritance Protocol (PIP) und Priority Ceiling Protocol (PCP)",
        answerC = "Priority Inversion ist ein Compiler-Bug der Prioritaets-Annotationen vertauscht; es gibt keine Software-Loesung",
        answerD = "Priority Inversion bezeichnet das Umkehren der Scheduling-Reihenfolge durch den Betriebssystem-Timer-Interrupt",
        correctAnswer = 1,
        explanation = "Priority Inversion: High-Task H wartet auf Mutex, Low-Task L haelt ihn. Wenn Medium-Task M laeuft, kann L den Mutex nicht freigeben — H wartet auf L, waehrend M laeuft. PIP: L erbt temporaer die Prioritaet von H. PCP: Task darf Ressource nur belegen wenn seine Prioritaet hoeher als Ceiling-Prioritaet ist.",
        difficulty = 4,
        funFact = "Priority Inversion war der Grund fuer den Mars Pathfinder 'System Reset' Bug 1997: Der meteorologische Task hatte hohe Prioritaet, blockierte am Bus-Mutex — und der hochpriore Information Bus Task loeste einen Watchdog-Reset aus."
    ),

    // Question 29 — OS-Kernel: Syscall-Mechanismus
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Wie unterscheidet sich der Syscall-Mechanismus auf x86-64 Linux zwischen der alten 'int 0x80'-Methode und dem modernen 'syscall'-Befehl hinsichtlich Performance?",
        answerA = "int 0x80 ist schneller weil es den TLB nicht invalidiert; syscall ist veraltet",
        answerB = "Beide Methoden sind identisch schnell — der Unterschied ist nur syntaktisch",
        answerC = "int 0x80 loest einen vollstaendigen Interrupt aus (IDT-Lookup, Privilege-Level-Wechsel, ~100-300ns); syscall/sysret ist ein dedizierter Befehl der den IDT-Lookup eliminiert und ueber MSR-Register konfiguriert wird (~50-100ns)",
        answerD = "syscall-Befehl erfordert Kernel-Patches und ist nicht Teil des x86-64-ISA",
        correctAnswer = 2,
        explanation = "Der 'syscall'-Befehl (AMD64) laedt CS/SS-Selektoren aus MSR-Registern statt den IDT zu durchsuchen. Er sichert RIP/RFLAGS in RCX/R11 und springt direkt zur Syscall-Handler-Adresse. Damit entfaellt der Interrupt-Gate-Lookup — etwa 50ns schneller auf modernen CPUs.",
        difficulty = 4,
        funFact = "Linux 2.6 fuehrte vDSO (virtual Dynamic Shared Object) ein: Fuer einige Syscalls (gettimeofday, clock_gettime) wird der Kernel-Code in den Userspace gemappt — kein Privilege-Level-Wechsel noetig, 10x schneller als normaler Syscall."
    ),

    // Question 30 — OS-Kernel: CFS Scheduler
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist der Completely Fair Scheduler (CFS) in Linux und wie verwendet er 'vruntime' fuer Scheduling-Entscheidungen?",
        answerA = "CFS ist ein Echtzeit-Scheduler der Tasks nach absoluten Deadlines einteilt",
        answerB = "CFS verwendet eine einfache FIFO-Queue und teilt CPU-Zeit gleichmaessig in 10ms-Scheiben auf",
        answerC = "CFS implementiert Multi-Level-Feedback-Queues mit 140 Prioritaetsleveln wie Windows NT",
        answerD = "CFS verwaltet Tasks in einem Red-Black-Tree nach 'vruntime' (gewichtete CPU-Zeit); der Task mit kleinster vruntime wird als naechstes ausgefuehrt; Nice-Werte skalieren die Gewichtung der vruntime-Erhoehung",
        correctAnswer = 3,
        explanation = "CFS zaehlt fuer jeden Task die verbrauchte CPU-Zeit gewichtet nach Prioritaet ('vruntime'). Niedrig-prioere Tasks (hoher Nice-Wert) akkumulieren vruntime schneller — sie werden seltener gewaehlt. Der Red-Black-Tree ermoeglicht O(log n) Zugriff auf den Task mit kleinster vruntime.",
        difficulty = 4,
        funFact = "CFS wurde von Ingo Molnar entwickelt und in Linux 2.6.23 (2007) eingefuehrt. Der RB-Tree mit ~1000 Tasks hat nur ~10 Vergleiche fuer den naechsten Task — praktisch O(1)."
    ),

    // Question 31 — OS-Kernel: io_uring
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Wie funktioniert io_uring in Linux und warum ist es bis zu 3x performanter als epoll fuer hochvolumige I/O-Operationen?",
        answerA = "io_uring nutzt zwei geteilte Ring-Buffer (Submission Queue, Completion Queue) zwischen User- und Kernel-Space; I/O-Anfragen koennen gebatcht werden ohne Syscalls; mit SQPOLL laueft ein Kernel-Thread der Requests ohne Syscall-Overhead verarbeitet",
        answerB = "io_uring verwendet Kernel-Bypass durch DPDK und sendet I/O-Anfragen direkt an den NIC",
        answerC = "io_uring ist ein neues Dateisystem-API das NVMe-Zugriffe ohne Page-Cache ermoeoerlicht",
        answerD = "io_uring ersetzt den Block-Layer komplett und kommuniziert direkt mit Storage-Treibern",
        correctAnswer = 0,
        explanation = "io_uring's Kernidee: User und Kernel teilen Speicher (Submission Queue Ring + Completion Queue Ring). Requests werden in die SQ geschrieben ohne Syscall. Mehrere I/Os koennen in einem einzigen Syscall gebatcht werden — drastisch reduzierter Kontext-Wechsel-Overhead.",
        difficulty = 4,
        funFact = "io_uring wurde 2019 von Jens Axboe (Linux Storage-Maintainer) entwickelt. Bei HTTP-Server-Benchmarks erreicht io_uring 3,5x mehr Anfragen pro Sekunde als epoll+sendfile."
    ),

    // Question 32 — OS-Kernel: eBPF Verifier
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist eBPF (extended Berkeley Packet Filter) und wie garantiert der Linux-Kernel die Sicherheit von eBPF-Programmen?",
        answerA = "eBPF ist ein Netzwerk-Filterframwork das ausschliesslich auf eingehende Pakete angewendet werden kann",
        answerB = "eBPF ist eine Kernel-VM fuer sicheres Ausfuehren von Bytecode im Kernel-Kontext; der Verifier prueft statisch: kein unbegrenztes Looping, keine ungueltigen Speicherzugriffe — vor der JIT-Compilation zum nativen Code",
        answerC = "eBPF-Programme werden als Kernel-Module geladen und haben unbegrenzten Kernel-Speicherzugriff",
        answerD = "eBPF ersetzt iptables und ist nur fuer Firewall-Regeln im Netfilter-Framework einsetzbar",
        correctAnswer = 1,
        explanation = "Der eBPF-Verifier analysiert alle moeglichen Code-Pfade statisch: Schleifen muessen terminieren, alle Pointer-Zugriffe muessen validiert sein, Stack-Zugriffe muessen in Grenzen liegen. Erst nach Bestehen des Verifiers wird der eBPF-Bytecode JIT-compiliert und als Kernel-Code ausgefuehrt.",
        difficulty = 4,
        funFact = "Cloudflare nutzt eBPF fuer DDoS-Mitigation: eBPF-Programme droppen schaedliche Pakete direkt in der NIC-Treiber-Queue (XDP) mit 25 Millionen Paketen/Sekunde — ohne jemals den TCP-Stack zu belasten."
    ),

    // Question 33 — OS-Kernel: Namespaces und cgroups
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Welche Linux-Kernel-Features bilden die technische Grundlage von Container-Isolation (Docker/Podman)?",
        answerA = "SELinux und AppArmor isolieren alle Container-Ressourcen vollstaendig",
        answerB = "Hypervisor-basierte Isolation durch KVM — Container und VMs sind technisch identisch",
        answerC = "Namespaces (PID, Mount, Network, IPC, UTS, User, Cgroup, Time) isolieren Kernel-Ressourcen-Sicht; cgroups v2 begrenzen und bilanzieren CPU, Speicher und I/O-Bandbreite",
        answerD = "Container-Isolation basiert auf chroot() und setuid() — Namespaces sind optional",
        correctAnswer = 2,
        explanation = "Namespaces geben jedem Container eine isolierte Sicht auf Kernel-Ressourcen: PID-Namespace (eigene PID-1), Network-Namespace (eigenes Netzwerkinterface), Mount-Namespace (eigenes Dateisystem-Tree), User-Namespace (eigene UID/GID-Mappings). cgroups v2 setzen harte Limits auf Ressourcenverbrauch.",
        difficulty = 4,
        funFact = "Linux-Container ohne Virtualisierung teilen denselben Kernel mit dem Host. Ein Kernel-Exploit kann deshalb aus dem Container ausbrechen — weshalb sicherheitskritische Deployments Kata Containers oder gVisor verwenden."
    ),

    // Question 34 — Edge Computing: ONNX Runtime
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist ONNX Runtime und wie ermoeglicht es das Deployment von ML-Modellen auf Edge-Geraeten mit heterogener Hardware?",
        answerA = "ONNX Runtime ist ein neuronales Netz-Framework das Modelle von Grund auf trainiert",
        answerB = "ONNX Runtime konvertiert automatisch ONNX-Modelle zu TensorFlow-Format fuer breitere Kompatibilitaet",
        answerC = "ONNX Runtime ist ein Microsoft-proprietary Format inkompatibel mit Open-Source-Frameworks",
        answerD = "ONNX Runtime ist eine plattformuebergreifende Inferenz-Engine; Execution Providers (CPU, CUDA, TensorRT, CoreML, DirectML, ACL) ermoeoerlicht das gleiche Modell auf GPU, NPU oder CPU ohne Code-Aenderung zu deployen",
        correctAnswer = 3,
        explanation = "ONNX Runtime's Execution-Provider-Architektur abstrahiert die Hardware: Ein Modell wird einmal nach ONNX konvertiert (von PyTorch/TensorFlow) und dann durch Provider-spezifische Optimierungen auf der Ziel-Hardware ausgefuehrt. Graph-Optimierungen laufen Hardware-unabhaengig.",
        difficulty = 4,
        funFact = "ONNX Runtime ist mit 3-7x schnellerer Inferenz als nativer PyTorch (ohne torch.compile) auf CPU-Targets gemessen worden. Auf ARM-Edge-Geraeten mit ONNX + ACL werden Modelle oft nahe an Peak-FLOPS betrieben."
    ),

    // Question 35 — IoT: LoRaWAN vs NB-IoT
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen LoRaWAN und NB-IoT hinsichtlich Spektrum-Nutzung und Betreiber-Modell?",
        answerA = "LoRaWAN nutzt unlizenziertes ISM-Band (868 MHz EU) mit privatem oder offenem Netzwerk-Betrieb; NB-IoT nutzt lizenziertes LTE-Spektrum und wird von Mobilfunknetzbetreibern betrieben mit garantierter QoS aber Abo-Kosten",
        answerB = "LoRaWAN ist ein Mobilfunkstandard der Telekommunikationsanbieter; NB-IoT ist ein freier ISM-Band-Standard",
        answerC = "Beide Protokolle nutzen dasselbe 900-MHz-Band — der einzige Unterschied ist der Verschluesselungsalgorithmus",
        answerD = "LoRaWAN bietet hoehere Datendurchsatz als NB-IoT (1 Mbps vs 100 kbps)",
        correctAnswer = 0,
        explanation = "LoRaWAN verwendet Chirp-Spread-Spectrum-Modulation im freien ISM-Band. Jeder kann ein LoRaWAN-Gateway betreiben (The Things Network). NB-IoT ist ein 3GPP-Standard (Rel. 13) der in LTE-Guardband oder In-Band laeuft — mit Roaming-Faehigkeit durch Mobilfunknetz.",
        difficulty = 4,
        funFact = "LoRa-Modems koennen mit 14-20 dBm Sendeleistung Reichweiten von 15-40 km in laendlichen Gebieten erzielen. Der Weltrekord fuer LoRa-Kommunikation liegt bei 832 km — von einem Ballon ausgesandt."
    ),

    // Question 36 — WebAssembly: GC Proposal
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ermoeglicht das WebAssembly GC Proposal (standardisiert 2024) und warum war es fuer Garbage-Collected Sprachen wie Kotlin oder Dart zuvor schwierig?",
        answerA = "Das GC Proposal fuegt einen Stop-the-World-Garbage-Collector direkt in die Wasm-Engine ein",
        answerB = "Das Wasm GC Proposal fuegt strukturierte Heap-Typen (struct, array, ref-Types) hinzu; Kotlin/Dart konnten zuvor nur mit eigenem GC im linearen Speicher laufen (grosser Overhead) — jetzt nutzen sie den nativen Browser-GC",
        answerC = "Das GC Proposal erlaubt Wasm-Modulen Zugriff auf den JavaScript-Garbage-Collector ueber postMessage",
        answerD = "Das GC Proposal ist ausschliesslich fuer Rust und C++ relevant, nicht fuer Managed-Language-Kompilierung",
        correctAnswer = 1,
        explanation = "Vor Wasm GC mussten Kotlin/Wasm, Dart/Wasm ihren eigenen Speicherverwaltungs-Code in den Wasm-Binary einbetten (~1-3 MB Overhead). Mit dem GC-Proposal koennen sie Browser-native GC-Heap-Objekte erzeugen — kleinere Binaries, bessere Interoperabilitaet mit JavaScript-Objekten.",
        difficulty = 4,
        funFact = "Kotlin/Wasm (mit GC Proposal) erzeugt Binaries die 40% kleiner sind als Kotlin/JS. JetBrains hat Compose Multiplatform UI fuer Browser auf Kotlin/Wasm portiert."
    ),

    // Question 37 — Compiler: PGO
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Was ist Profile-Guided Optimization (PGO) und welche spezifischen Optimierungen werden durch Profiling-Daten ermoeglicht?",
        answerA = "PGO analysiert den Quellcode statisch und optimiert alle Pfade gleichmaessig",
        answerB = "PGO ist nur fuer interpreted Languages (Python, Ruby) verfuegbar, nicht fuer AOT-Compiler",
        answerC = "PGO sammelt in einem Instrumentierungs-Run Laufzeitdaten (Branch-Wahrscheinlichkeiten, Call-Haeufigkeiten); der finale Compile-Durchlauf kann dann Hot-Paths inlinen, Cold-Code auslagern, Branch-Layout optimieren und Funktionen nach Cache-Lokalitaet ordnen",
        answerD = "PGO erhoeht die Compile-Zeit und Speicherverbrauch ohne messbaren Performance-Vorteil",
        correctAnswer = 2,
        explanation = "PGO (auch FDO) arbeitet zweistufig: Erst wird ein instrumentiertes Binary erzeugt, das waehrend repraesentativer Workloads Profiling-Daten sammelt. Im zweiten Compile-Durchlauf nutzt der Compiler diese Daten fuer fundierte Optimierungsentscheidungen statt statischer Heuristiken.",
        difficulty = 4,
        funFact = "Chrome/V8 wird mit PGO gebaut und erzielt dadurch ~5-10% bessere Laufzeit. Clang's BOLT (Binary Optimization and Layout Tool) kann PGO sogar post-linking auf fertige Binaries anwenden."
    ),

    // Question 38 — FPGA: DSP48 Slices
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was sind DSP48-Slices in Xilinx/AMD FPGAs und warum sollte ein FPGA-Designer sie bevorzugen statt Multiplizierer in LUT-Logik zu implementieren?",
        answerA = "DSP48-Slices sind dedizierte I/O-Pins fuer analoge Signaleingaben",
        answerB = "DSP48-Slices ersetzen den BRAM fuer Koeffizientenspeicherung in Signalverarbeitungs-Pipelines",
        answerC = "DSP48-Slices sind ausschliesslich fuer Gleitkomma-Arithmetik im IEEE-754-Format ausgelegt",
        answerD = "DSP48-Slices sind dedizierte Hardwareeinheiten fuer Multiply-Accumulate mit eigenem Pre-Adder, 27x18-Multiplizierer und 48-Bit-Akkumulator; sie sind 10-20x flaechen- und energieeffizienter als LUT-Implementierungen mit hoeherer Taktfrequenz",
        correctAnswer = 3,
        explanation = "DSP48E2 (UltraScale) enthaelt: Pre-Adder (27-Bit), Multiplizierer (27x18 Bit) und Akkumulator (48 Bit) in einer Kaskadenkette. Taktfrequenz: bis zu 741 MHz. Eine 8-Tap-FIR-Filter-Implementierung in LUTs benoetigt ~200 LUTs vs. 8 DSP48-Slices — bei 3x langsamerer Taktfrequenz.",
        difficulty = 4,
        funFact = "Xilinx UltraScale+ FPGAs enthalten bis zu 3.528 DSP48E2-Slices. Ein KI-Inferenz-Accelerator darauf kann bis zu 12 TOPS (tera operations per second) bei INT8-Quantisierung erreichen."
    ),

    // Question 39 — Rust: Lifetime-Elision
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was sind die drei Lifetime-Elision-Regeln in Rust und wann muessen Lifetimes trotzdem explizit annotiert werden?",
        answerA = "Regel 1: Jeder Referenz-Parameter bekommt eigene Lifetime. Regel 2: Wenn genau ein Input-Lifetime, wird er auf alle Outputs angewendet. Regel 3: Wenn &self/&mut self als Input, wird dessen Lifetime auf alle Outputs angewendet. Explizit noetig wenn mehrere Inputs und kein self vorhanden",
        answerB = "Lifetime-Elision erlaubt das Weglassen aller Lifetime-Annotationen in Rust-Code — sie sind immer optional",
        answerC = "Lifetime-Elision gilt nur fuer Funktionsparameter, nicht fuer Structs oder Impl-Bloecke",
        answerD = "Lifetime-Elision ist eine optionale Compiler-Extension und muss per Feature-Flag aktiviert werden",
        correctAnswer = 0,
        explanation = "Die drei Elision-Regeln decken die haeufigsten Faelle ab. Wenn der Compiler nach Anwendung aller drei Regeln nicht alle Output-Lifetimes bestimmen kann, verlangt er explizite Annotationen. Typisches Beispiel: fn longest(s1: &str, s2: &str) -> &str — zwei Input-Lifetimes, keine Regel greift.",
        difficulty = 4,
        funFact = "Die Lifetime-Elision-Regeln wurden in RFC 141 (2014) formalisiert. Vor diesem RFC mussten in Rust alle Lifetimes explizit angegeben werden — praktisch jede Funktion mit Referenzen hatte Lifetime-Annotationen."
    ),

    // Question 40 — Go: Channel-Internals
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Wie ist ein buffered Go-Channel intern implementiert und was passiert auf Speicherebene wenn ein Sender blockiert?",
        answerA = "Buffered Channels sind einfache Mutex-geschuetzte Slices; blockierende Sender spinnen in einer Busy-Wait-Schleife",
        answerB = "Ein buffered Channel ist ein hchan-Struct mit zirkularem Ringpuffer, Sende-/Empfangs-Index und zwei Goroutine-Wartelisten (sendq/recvq); blockierende Sender werden als sudog-Struct in sendq eingereiht und von der Runtime suspendiert bis Platz verfuegbar ist",
        answerC = "Buffered Channels verwenden OS-Semaphoren; blockierende Sender werden durch futex-syscalls blockiert",
        answerD = "Go-Channels sind syntaktischer Zucker fuer sync.Mutex — intern gibt es keinen Unterschied zwischen unbuffered und buffered Channels",
        correctAnswer = 1,
        explanation = "Das hchan-Struct enthaelt: buf (Ringpuffer), qcount (Anzahl Elemente), dataqsiz (Kapazitaet), sendq/recvq (verkettete Listen wartender Goroutinen als sudog-Structs). Wenn buf voll ist, wird die sendende Goroutine in sendq eingereiht und aus dem M-P-Scheduling entfernt.",
        difficulty = 4,
        funFact = "Ein unbuffered Go-Channel ist ein buffered Channel mit dataqsiz=0. Direkter Goroutine-zu-Goroutine-Transfer ist moeglich wenn beide gleichzeitig senden und empfangen — der Wert wird direkt in den Stack der empfangenden Goroutine kopiert."
    ),

    // Question 41 — Embedded: CAN-Bus Arbitrierung
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Wie funktioniert die Arbitrierung im CAN-Bus-Protokoll und warum ist sie zerstoerungsfrei (non-destructive)?",
        answerA = "CAN-Arbitrierung verwendet ein Token-Ring-Verfahren — nur der Token-Inhaber darf senden",
        answerB = "CAN benutzt Collision-Detection wie Ethernet — bei Kollision senden alle Knoten nach Backoff-Timer neu",
        answerC = "CAN verwendet bitweise CSMA/CA: Alle Sender senden gleichzeitig ihre Message-ID; ein rezessiver Bit (1) wird von einem dominanten Bit (0) ueberschrieben; Sender die rezessiv senden aber dominant lesen, verlieren Arbitrierung und warten — der Gewinner sendet unbeeinflusst weiter",
        answerD = "CAN-Arbitrierung erfordert einen dedizierten Bus-Master der Sendeerlaubnis explizit vergibt",
        correctAnswer = 2,
        explanation = "CAN's Arbitrierung ist zerstoerungsfrei weil der Gewinner (niedrigste numerische CAN-ID = dominante Bits) seinen Frame ohne Unterbrechung beendet. Verlierer warten und versuchen erneut nach Busfreiheit. Das Prinzip: Wired-AND Logik auf der Busleitung — dominant (0) gewinnt immer gegen rezessiv (1).",
        difficulty = 4,
        funFact = "CAN-FD (2012) erweitert CAN auf bis zu 8 Mbps (statt 1 Mbps) und 64 Bytes Payload (statt 8). Moderne Fahrzeuge haben bis zu 150 ECUs die ueber CAN/CAN-FD kommunizieren — ca. 2-3 km Kabellaenge insgesamt."
    ),

    // Question 42 — Compiler: Devirtualization
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist 'Devirtualization' als Compiler-Optimierung in C++/Java und unter welcher Bedingung kann sie angewendet werden?",
        answerA = "Devirtualization entfernt alle virtual-Deklarationen aus C++-Klassen zur Compile-Zeit",
        answerB = "Devirtualization ist nur in Ahead-of-Time-Compilern moeglich, nicht in JIT-Compilern",
        answerC = "Devirtualization eliminiert virtuelle Destruktoren und ist deshalb in polymorphen Hierarchien unsicher",
        answerD = "Devirtualization ersetzt einen indirekten virtuellen Funktionsaufruf (vtable-Lookup) durch einen direkten Aufruf; moeglich wenn der Compiler nachweisen kann, dass nur ein konkreter Typ moeglich ist (z.B. Typ-Propagation, PGO, final-Modifier)",
        correctAnswer = 3,
        explanation = "Ein vtable-indirekter Aufruf kostet ~3-5 Taktzyklen mehr als ein direkter Aufruf (Pipeline-Flush wegen indirekter Branch). Durch Devirtualization entfaellt der vtable-Lookup und Inlining wird moeglich. Bedingungen: final-Klassen/Methoden, Typ-Analyse zeigt konkreten Typ, oder PGO zeigt 99% der Aufrufe sind ein bestimmter Typ.",
        difficulty = 4,
        funFact = "HotSpot JVM verwendet speculative Devirtualization mit inline caches: Die JIT-compilierte Methode prueft ob der Typ mit dem erwarteten uebereinstimmt und springt bei Mismatch zur deoptimierten Version. In der Praxis ist speculative Devirtualization in >95% der Faelle korrekt."
    ),

    // Question 43 — FPGA: Partial Reconfiguration
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was ist Partial Reconfiguration (PR) bei FPGAs und welchen operationalen Vorteil bietet sie in eingebetteten Systemen?",
        answerA = "PR erlaubt das Neukonfigurieren eines definierten Bereichs (Reconfigurable Partition) des FPGAs waehrend andere Bereiche aktiv bleiben; ermoeglicht Zeit-Multiplexing von Hardware-Funktionen und Reduzierung der FPGA-Groesse",
        answerB = "PR erlaubt das Aktualisieren der FPGA-Firmware ohne Neustarten — identisch zu Software-Updates",
        answerC = "PR ist eine Xilinx-spezifische Technologie ausschliesslich auf UltraScale+-FPGAs verfuegbar",
        answerD = "PR beschreibt das gleichzeitige Konfigurieren mehrerer FPGAs in einem Daisy-Chain-Setup",
        correctAnswer = 0,
        explanation = "Bei PR wird der FPGA in statische (immer aktiv) und dynamische Regionen (Reconfigurable Partitions) unterteilt. Waehrend der statische Bereich (z.B. Kommunikationsinterface) laeuft, kann eine neue Bitstream-Partial-Datei geladen werden — der Chip stoppt nicht. Typisch fuer SDR oder Multi-Algorithmen-Systeme.",
        difficulty = 4,
        funFact = "Amazon AWS F1-Instances (FPGA-Cloud) nutzen Partial Reconfiguration: Nutzer koennen ihre eigenen FPGA-Designs laden ohne die AWS-Infrastruktur zu unterbrechen — ein Partial Reconfig dauert ~100ms."
    ),

    // Question 44 — Echtzeitsysteme: Task-Modelle
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen einem 'periodic', einem 'sporadic' und einem 'aperiodic' Task im Echtzeitsystem-Modell?",
        answerA = "Periodic: Kommt einmalig; Sporadic: Kommt periodisch; Aperiodic: Kommt nie",
        answerB = "Periodic: Aktivierung in festen Intervallen (Periode T); Sporadic: Aktivierung mit minimalem Abstand zwischen Ereignissen (Minimum Inter-Arrival Time); Aperiodic: Aktivierung ohne Mindestabstand-Garantie — nur Sporadic und Periodic sind formal analysierbar",
        answerC = "Alle drei Modelle sind identisch — die Unterscheidung ist nur akademisch",
        answerD = "Sporadic Tasks koennen keine Deadlines haben; Aperiodic Tasks haben immer Best-Effort-Prioritaet",
        correctAnswer = 1,
        explanation = "Periodic Tasks (regelmaessig, exakt): Timer-getriggert mit Periode T. Sporadic Tasks (unregelmassig, aber mit Minimalabstand): formal analysierbar. Aperiodic Tasks (vollkommen unregelmassig): Nur mit Acceptance-Test oder Bandwidth-Server (Sporadic Server) in harte Systeme integrierbar.",
        difficulty = 4,
        funFact = "Der Sporadic Server-Algorithmus (Sprunt, Sha, Lehoczky 1989) erlaubt es, aperiodische Tasks mit garantierter Bandbreite in RMS-Systeme einzubinden ohne die Schedulierbarkeit der periodischen Tasks zu gefaehrden."
    ),

    // Question 45 — OS-Kernel: Memory Barriers
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Warum sind Memory Barriers (Fence-Instruktionen) in Multicore-Systemen notwendig und was unterscheidet einen StoreLoad-Barrier von einem StoreStore-Barrier?",
        answerA = "Memory Barriers sind nur in Systemen ohne Hardware-Cache-Kohaerenz noetig — x86 braucht keine",
        answerB = "Memory Barriers sind Compiler-Direktiven die ausschliesslich die Code-Generierung beeinflussen, nicht die Hardware",
        answerC = "Moderne CPUs koennen Speicheroperationen aus Performance-Gruenden neu ordnen; StoreStore-Barrier verhindert Umordnung von Stores untereinander; StoreLoad-Barrier (teuerste Variante) verhindert dass ein Store nach einem Load ausgefuehrt erscheint",
        answerD = "x86 und ARM haben identische Memory-Ordering-Modelle — Barriers sind auf beiden Architekturen optional",
        correctAnswer = 2,
        explanation = "x86 hat ein staerkeres Memory-Modell (TSO — Total Store Order): StoreStore und LoadLoad sind implizit geordnet, aber StoreLoad-Umordnung ist erlaubt. ARM (Weak Memory Model) erlaubt alle vier Umordnungen. Ein MFENCE auf x86 oder DMB ISH auf ARM ist ein Full Barrier der alle Umordnungen verhindert.",
        difficulty = 4,
        funFact = "Java's volatile-Keyword generiert auf ARM vier Barrier-Instruktionen (DMB ISH vor und nach jedem Zugriff). Das macht volatile auf ARM teurer als auf x86 — ein Grund warum Java-Performance auf ARM historisch schlechter war."
    ),

    // Question 46 — GraphQL: Subscriptions
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Wie sind GraphQL-Subscriptions technisch implementiert und welcher Transport-Layer wird typischerweise verwendet?",
        answerA = "GraphQL-Subscriptions basieren auf HTTP-Polling alle 100ms — kein persistenter Verbindungstyp noetig",
        answerB = "GraphQL-Subscriptions verwenden UDP-Multicast fuer effiziente Verteilung an viele Clients",
        answerC = "GraphQL-Subscriptions sind eine Apollo-proprietary Erweiterung die nicht im GraphQL-Spezifikation enthalten ist",
        answerD = "GraphQL-Subscriptions sind meist ueber WebSocket (graphql-ws-Protokoll) oder Server-Sent Events (SSE) implementiert; der Client sendet eine Subscribe-Operation, der Server haelt die Verbindung offen und sendet Events wenn der PubSub-Datenstrom neue Daten liefert",
        correctAnswer = 3,
        explanation = "Das graphql-ws-Protokoll definiert Message-Types: connection_init, subscribe, next, error, complete. Der Server verwendet intern typischerweise einen PubSub-Mechanismus (Redis, NATS, in-memory) um Events an WebSocket-Verbindungen zu routen. SSE ist eine Alternative fuer unidirektionale Push-Daten.",
        difficulty = 4,
        funFact = "Das aeltere 'subscriptions-transport-ws'-Protokoll hatte ein Design-Problem: Es sendete keinen Ping um WebSocket-Verbindungen lebendig zu halten. Das neuere 'graphql-ws'-Protokoll (2021) loeste dies und wird von allen modernen GraphQL-Servern unterstuetzt."
    ),

    // Question 47 — Embedded: Secondary Bootloader
    // Correct: A (correctAnswer=0)
    Question(
        categoryId = 7,
        questionText = "Was ist ein Secondary Bootloader (SBL) in eingebetteten Systemen und welche Schritte fuehrt er typischerweise durch?",
        answerA = "Der SBL initialisiert DRAM-Controller, konfiguriert Clocks/PLLs, verifiziert Anwendungs-Image-Signatur (Secure Boot), kopiert Image in DRAM und springt zum Entry-Point der Anwendung oder des RTOS-Kernels",
        answerB = "Ein SBL ist identisch mit dem Primary Bootloader und wird nur bei Firmware-Updates ausgefuehrt",
        answerC = "SBL-Aufgaben werden vollstaendig von der Hardware erledigt — Software-Bootloader sind in modernen MCUs nicht mehr noetig",
        answerD = "Ein SBL ist nur auf FPGA-basierten SoCs noetig; ARM Cortex-M-Mikrocontroller haben keinen Secondary Bootloader",
        correctAnswer = 0,
        explanation = "Typischer SBL-Ablauf: ROM-Bootloader laedt SBL von Flash in on-chip-SRAM → SBL initialisiert DRAM-PHY und -Controller → SBL initialisiert Peripherie (Clocks, PLLs, Watchdog) → SBL verifiziert Anwendungs-Hash/Signatur → SBL kopiert Image in DRAM → SBL springt zu Reset-Handler.",
        difficulty = 4,
        funFact = "Der ARM Trusted Firmware (ATF) implementiert einen mehrstufigen Bootchain: BL1 (ROM) → BL2 (SBL) → BL31 (EL3 Runtime) → BL33 (UEFI/U-Boot). Jede Stufe verifiziert die naechste kryptografisch — dies bildet die Hardware Root of Trust."
    ),

    // Question 48 — Go: Escape Analysis
    // Correct: B (correctAnswer=1)
    Question(
        categoryId = 7,
        questionText = "Was ist Escape Analysis in Go und welche Auswirkung hat sie auf Heap-Allokationen und Garbage-Collector-Druck?",
        answerA = "Escape Analysis ist ein Sicherheitscheck der sicherstellt, dass Go-Variablen keine Speichergrenzen ueberschreiten",
        answerB = "Escape Analysis bestimmt ob eine Variable auf dem Stack oder Heap allokiert werden muss; Variablen die nach Funktionsrueckkehr nicht mehr referenziert werden bleiben auf dem Stack (kein GC-Druck); 'entweichende' Variablen (z.B. Adresse zurueckgegeben) werden auf den Heap allokiert",
        answerC = "Escape Analysis ist ein zur Laufzeit aktiver Mechanismus der Stack-Variablen dynamisch auf den Heap verschiebt",
        answerD = "Escape Analysis in Go ist standardmaessig deaktiviert und muss durch go build -gcflags='-escape' aktiviert werden",
        correctAnswer = 1,
        explanation = "Gos Compiler fuehrt Escape Analysis bei der Compilation durch (sichtbar mit -gcflags='-m'). Stack-Allokationen sind extrem schnell und erzeugen keinen GC-Druck. Heap-Allokationen belasten den GC. Durch Escape Analysis verbleiben lokale Structs auf dem Stack wenn sie nicht als Pointer herausgegeben werden.",
        difficulty = 4,
        funFact = "In Performance-kritischen Go-Diensten optimieren Entwickler Code explizit um Heap-Escapes zu reduzieren — messbar als GC-Pause-Reduktion. Mit go build -gcflags='-m' zeigt der Compiler fuer jede Variable an ob sie 'escapes to heap' oder nicht."
    ),

    // Question 49 — Rust: Pin und Unpin
    // Correct: C (correctAnswer=2)
    Question(
        categoryId = 7,
        questionText = "Was ist 'Pin<P>' in Rust und warum ist es fuer selbstreferenzielle Strukturen in async/await notwendig?",
        answerA = "Pin<P> ist ein Wrapper der verhindert, dass ein Wert ueberhaupt abgelesen werden kann — nur schreibender Zugriff ist erlaubt",
        answerB = "Pin<P> ist ein Alias fuer Box<T> der nur auf dem Stack allokierte Werte erlaubt",
        answerC = "Pin<P> garantiert, dass der Speicherort eines Wertes nicht verschoben wird; async-Futures koennen selbstreferenzielle Strukturen enthalten — ein Move wuerde diese Zeiger ungueltig machen; Pin verhindert diesen Move",
        answerD = "Pin<P> implementiert automatisch den Send-Trait damit Futures zwischen Threads gesendet werden koennen",
        correctAnswer = 2,
        explanation = "Wenn eine async-Funktion einen Wert ueber mehrere await-Punkte haelt und gleichzeitig einen Pointer auf diesen Wert speichert (self-referential), wuerde ein Move den Pointer unvalide machen. Pin<P> markiert einen Wert als 'nicht mehr verschiebbar'. Typen die trotzdem verschiebbar sind implementieren Unpin.",
        difficulty = 4,
        funFact = "Das Pin-Konzept wurde 2018 in RFC 2349 eingefuehrt. Der Witz in der Rust-Community: 'Pin ist das Konzept das jeden Rust-Entwickler mindestens einmal zum Nachdenken bringt was Move eigentlich bedeutet.'"
    ),

    // Question 50 — Edge Computing: Federated Learning
    // Correct: D (correctAnswer=3)
    Question(
        categoryId = 7,
        questionText = "Was ist Federated Learning und welches kryptografische Problem loest Secure Aggregation in diesem Kontext?",
        answerA = "Federated Learning trainiert ein zentrales Modell auf einem zentralen Server mit Daten von allen Clients",
        answerB = "Federated Learning ist ein Synonym fuer verteiltes Training mit MPI auf HPC-Clustern",
        answerC = "Secure Aggregation in Federated Learning bedeutet, dass alle Clients dieselben Trainingsdaten verwenden muessen",
        answerD = "Federated Learning trainiert Modelle dezentral auf Edge-Geraeten; nur Gradienten werden an den Server gesendet, nicht Rohdaten; Secure Aggregation (via Secret Sharing oder Homomorphe Verschluesselung) verhindert, dass der Server individuelle Gradienten eines Geraets einsehen kann",
        correctAnswer = 3,
        explanation = "Im Federated Learning-Setup trainiert jedes Geraet lokal auf seinen Daten und sendet nur den Gradientenvektor. Ohne Secure Aggregation koennte der Server durch Gradientenanalyse private Trainingsdaten rekonstruieren (Gradient Inversion). Secure Aggregation summiert alle Gradienten so, dass der Server nur die Summe sieht.",
        difficulty = 4,
        funFact = "Google verwendet Federated Learning fuer die Tastaturvorhersage in Gboard auf Android-Geraeten seit 2017. Millionen von Geraeten verbessern das Modell ohne dass Google jemals die Tippprotokolle der Nutzer gesehen hat."
    )
)
