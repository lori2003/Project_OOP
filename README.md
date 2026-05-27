# Travel Budget Companion

Ho sviluppato questo progetto, chiamato Travel Budget Companion, come un'applicazione a riga di comando in Java 17 per semplificare la pianificazione e il controllo delle spese di viaggio. L'idea di fondo è quella di superare i limiti dei classici fogli Excel, che spesso risultano rigidi e difficili da aggiornare quando si visitano luoghi diversi o si usano valute differenti. Con questo applicativo, le spese vengono organizzate in modo gerarchico e la gestione dei cambi o dei limiti di budget avviene in modo del tutto automatico.

---

## Come ho modellato il dominio (La gerarchia dei dati)

Per organizzare al meglio le informazioni durante un itinerario reale, ho strutturato i dati seguendo una gerarchia molto intuitiva. Al vertice di tutto c'è il Viaggio, che definisce i dettagli generali come il nome, le date di inizio e fine, la valuta principale di riferimento e il budget totale che si ha a disposizione. Subito sotto troviamo le Tappe, che rappresentano le città o le mete geografiche del viaggio: ogni tappa ha le sue date e un sotto-budget dedicato per tenere sotto controllo i costi locali. All'interno di ogni tappa si collocano le Giornate, utili per raggruppare i costi giorno per giorno. Infine, alla base della struttura ci sono le singole Spese, che contengono tutti i dettagli dell'acquisto come l'importo, la valuta usata, la categoria di spesa, il luogo fisico e una descrizione di testo.

Tutti i dati vengono salvati localmente su un file in formato JSON. Questa scelta mi permette di salvare l'intera struttura ad albero sul disco in modo semplice ed immediato, senza dover configurare database esterni e mantenendo l'applicazione estremamente snella e focalizzata sulla qualità del codice e dei pattern.

---

## come è organizzato il progetto

La struttura dei file segue fedelmente lo standard dei progetti Maven. Nella cartella principale del progetto si trova il file pom.xml che contiene la configurazione e tutte le dipendenze esterne necessarie, in particolare la libreria Jackson che ho utilizzato per gestire la persistenza dei dati in formato JSON. Il codice sorgente si trova all'interno della cartella dei sorgenti principali ed è suddiviso in package specifici per garantire una netta separazione delle responsabilità. Il package model ospita tutte le classi di dominio che descrivono le entità del viaggio, delle tappe, delle giornate e delle singole spese. Il package service contiene le classi destinate alla conversione delle valute e la factory per istanziare i vari nodi in maniera centralizzata. Il package persistence racchiude la classe repository che si occupa di salvare e caricare i file JSON dal disco locale in modo sicuro. Infine, il package cli ospita la classe principale che gestisce interamente l'interfaccia utente a terminale e la lettura robusta e controllata di tutti gli input inseriti dall'utente.

I test unitari scritti con JUnit 5 si trovano invece nella cartella corrispondente dei test e servono a verificare che ogni singola funzionalità dell'applicazione risponda correttamente e in modo isolato ad ogni minima modifica del codice sorgente.

---

## Come compilare ed avviare l'applicazione

Visto che il progetto usa Maven per gestire l'intero ciclo di vita e la build, ho strutturato la configurazione in modo da offrire due modi semplicissimi per avviare l'applicativo direttamente da terminale. Il primo metodo, ideale durante lo sviluppo, consiste nell'avviare l'interfaccia a riga di comando direttamente con il plugin exec di Maven digitando il comando `mvn exec:java` all'interno del terminale dopo essersi posizionati nella cartella principale del progetto; questo comando compila i file e carica automaticamente tutte le dipendenze esterne. Il secondo metodo, invece, serve per generare un pacchetto eseguibile autonomo: basta lanciare il comando `mvn clean package` per compilare il codice e creare un file JAR completo (un "fat JAR" che racchiude anche le dipendenze di Jackson grazie al plugin shade che ho configurato nel file pom.xml) all'interno della cartella target. A questo punto, per avviare l'applicativo in modo del tutto indipendente da Maven, è sufficiente digitare nel proprio terminale il classico comando Java `java -jar target/progetto-oop-1.0-SNAPSHOT.jar`.

---

## Architettura e Diagrammi UML

Per descrivere la struttura delle classi e l'architettura dei package del progetto, ho preparato due diagrammi UML formali scritti in formato PlantUML. Ho deciso di salvarli all'interno della cartella `docs/uml/` per mantenere il file README leggero, pulito e facile da leggere, evitando di appesantirlo con lunghi frammenti di codice grafico che risulterebbero illeggibili in assenza di plugin appositi.

All'interno della cartella ho inserito sia i sorgenti in formato `.puml` (utili per chi desidera integrarli o modificarli nel proprio ambiente di sviluppo), sia le rispettive immagini pre-compilate in formato `.png` (`class_diagram.png` e `architectural_diagram.png`), rendendole immediatamente visualizzabili. Il diagramma delle classi illustra l'intera gerarchia del dominio di business modellata tramite il pattern Composite, evidenziando il modo in cui le classi concrete estendono la classe astratta comune e come interagiscono i vari pattern strutturali e comportamentali che ho implementato. Il diagramma architetturale descrive invece visivamente l'architettura dei package, mostrando come sono disaccoppiati i vari strati dell'applicativo (l'interfaccia utente a riga di comando, i servizi di business, il modello e lo strato di persistenza) e le rispettive dipendenze logiche.

---

## I Design Pattern che ho utilizzato nel progetto

Nello sviluppo dell'applicativo ho applicato diversi pattern orientati agli oggetti per risolvere problemi tipici della gestione dei dati, cercando di mantenere il codice il più flessibile e disaccoppiato possibile.

### Gestire l'albero con il pattern Composite
Dato che un viaggio contiene tappe, che a loro volta contengono giorni, che contengono spese, per fare operazioni come il calcolo del totale o lo scorrimento dei dati avrei dovuto inserire molti cicli annidati all'interno della CLI. Per evitare questo forte accopiamento ho usato il pattern Composite. Ho definito un'interfaccia comune che rappresenta un nodo del budget e dichiara i metodi condivisi per calcolare il totale e leggere il nome. Le classi che rappresentano il viaggio, le tappe e i giorni agiscono come nodi compositi (rami dell'albero) e gestiscono liste di nodi figli, mentre la spesa rappresenta la foglia finale. In questo modo, quando la CLI chiede il totale speso al viaggio, la chiamata si propaga in automatico lungo i rami fino alle singole spese e somma tutto ricorsivamente, senza che il client debba conoscere la struttura interna dell'albero.

### Cambiare valuta con il pattern Strategy
L'applicazione deve poter convertire spese fatte in valute diverse nella valuta di riferimento del viaggio. Visto che la logica con cui vengono stabiliti i tassi di cambio può variare nel tempo (ad esempio usando cambi fissi memorizzati offline, caricando i tassi da file o scaricandoli in tempo reale dal web), ho isolato questa responsabilità con il pattern Strategy. Ho definito un'interfaccia per la strategia di conversione e ho scritto un'implementazione concreta a tassi fissi che usa una EnumMap per memorizzare i cambi offline in modo estremamente rapido. Il convertitore di valuta agisce da contesto e riceve la strategia concreta al momento dell'avvio: in questo modo, se in futuro vorrò integrare un'API web per i tassi in tempo reale, mi basterà scrivere una nuova classe per la strategia senza dover modificare una singola riga di codice nel modello o nelle spese.

### Creare gli oggetti con Abstract Factory, Singleton e Builder
Per evitare che la CLI sia strettamente accoppiata alle classi concrete del modello quando crea nuovi elementi, ho centralizzato la creazione degli oggetti in una factory. L'interfaccia astratta della factory definisce i metodi di creazione per viaggio, tappe, giorni e spese, mentre l'implementazione concreta gestisce la nascita reale degli oggetti. Per la spesa ho sfruttato anche il pattern Builder all'interno della factory, dato che ha molti campi nel costruttore e il builder mi aiuta a impostarli in modo ordinato e leggibile mantenendo l'immutabilità dell'oggetto una volta creato. Inoltre, siccome la factory non ha uno stato modificabile e basta una sola istanza globale in tutta l'app, l'ho implementata come un Singleton a cui si accede tramite un metodo statico.

### Monitorare il budget con il pattern Observer
Il controllo del superamento del budget è una regola di business importante che deve stare nel modello, ma l'allarme visivo per l'utente fa parte dell'interfaccia grafica. Per separare queste due cose ho usato il pattern Observer. Il viaggio gestisce una lista di osservatori registrati e li avvisa quando viene inserita una nuova spesa nell'albero. All'avvio, la CLI registra un osservatore sul viaggio tramite espressione Lambda: non appena le spese totali superano il 90% o il 100% del budget del viaggio, viene stampato subito a schermo un avviso per l'utente, mantenendo il modello dati del tutto pulito e separato dall'interfaccia testuale.

### annullare le azioni con il pattern Memento
Per permettere all'utente di correggere eventuali errori di inserimento, ho voluto implementare una funzione di Undo. Per salvare lo stato del viaggio prima di ogni modifica senza violare l'incapsulamento dei campi privati, ho applicato il pattern Memento. Prima di ogni modifica di scrittura, la CLI salva uno snapshot dello stato del viaggio inserendolo in uno stack LIFO. Per evitare i tipici problemi dovuti alla condivisione di riferimenti mutabili in memoria (che rovinerebbero i salvataggi passati), il memento crea una copia profonda completa serializzando l'intera gerarchia delle tappe del viaggio in una stringa JSON tramite Jackson. Quando l'utente chiede l'Undo, l'ultimo snapshot viene estratto dallo stack e deserializzato, ripristinando in modo sicuro e pulito lo stato precedente.

### Proteggere i dati con il pattern Decorator
Quando devo stampare il riepilogo a schermo, i dati del viaggio vengono passati alle funzioni della CLI. Per essere sicuro al 100% che l'interfaccia utente non possa modificare o aggiungere elementi per errore durante il rendering, ho usato il pattern Decorator creando un wrapper di sola lettura. Questa classe avvolge un nodo qualsiasi implementandone la stessa interfaccia: delega tutte le chiamate di lettura e di calcolo del totale al nodo interno originale, ma se qualcuno prova a chiamare l'aggiunta di un figlio sul wrapper, viene lanciata subito un'eccezione, proteggendo l'integrità del modello di dominio in modo simile alle collezioni non modificabili del JDK.

### Condividere il flusso con il Template Method e scorrere con l'Iterator
Per evitare di scrivere lo stesso codice di inserimento e gestione dei figli sia nel viaggio che nelle tappe, ho inserito la logica comune all'interno di una classe astratta base usando il pattern Template Method. Questo metodo definisce il flusso standard di controllo ed inserimento, lasciando la validazione del tipo specifico di figlio (ad esempio che un viaggio accetti solo tappe e una tappa accetti solo giorni) ad un gancio astratto (hook) che ogni sottoclasse implementa singolarmente. In più, la classe del viaggio implementa l'interfaccia Iterable fornendo un Iterator personalizzato che esegue una visita in profondità (DFS) sull'albero e lo appiattisce. In questo modo la CLI può scorrere ordinatamente tappe, giorni e spese con un unico ciclo for-each senza dover conoscere la forma interna dell'albero.

---

## Persistenza dei dati e Sicurezza

La persistenza del viaggio viene gestita in modo esclusivo dal repository JSON nel package dedicato. Il caricamento e il salvataggio su file avvengono tramite l'ObjectMapper di Jackson, che ho configurato per disabilitare la scrittura delle date come timestamp numerici (così nel file JSON restano leggibili nello standard AAAA-MM-GG, mentre l'interfaccia a terminale legge e mostra le date all'utente nel formato italiano GG/M/AAAA) e per tollerare campi sconosciuti o aggiuntivi nel JSON, evitando crash in caso di file generati da versioni diverse. Questa separazione fa sì che se un giorno decidessi di salvare i dati su un database SQL, dovrei cambiare solo questa classe senza dover toccare il modello o i pattern del dominio.

Per quanto riguarda la sicurezza e la stabilità, ho seguito i principi di Exception Shielding e programmazione sicura. La CLI non espone stack trace tecnici o dettagli interni della JVM: ogni eccezione viene intercettata e tradotta in un messaggio comprensibile in italiano. Tutti gli input numerici, testuali e temporali vengono validati nella CLI, in modo da evitare crash in caso di errori di digitazione o valori negativi. I tassi di cambio fissi sono memorizzati in una EnumMap senza credenziali cablate nel codice.

---

## La Suite dei test unitari

per assicurarmi che il codice rimanga stabile ad ogni piccola modifica, ho preparato una suite di 60 test automatici scriti con JUnit 5, distribuiti su 8 classi di test. questi test verificano in isolamento il corretto funzionamento di tutte le parti principali, comprese le operazioni più delicate come il calcolo ricorsivo dei totali nell'albero, la conversione delle valute con le diverse strategie, la persistenza JSON su disco e l'efficacia protettiva del wrapper di sola lettura.

---

## Idee e sviluppi futuri

La struttura modulare basata su interfacce e design pattern rende l'applicazione molto semplice da estendere. In futuro mi piacerebbe aggiungere una nuova strategia di cambio che scarichi i tassi in tempo reale da un'API web esterna, implementare la possibilità di rimuovere singole tappe o spese aggiornando in automatico il totale del viaggio, e sviluppare un database locale per poter salvare e gestire più viaggi contemporaneamente.
