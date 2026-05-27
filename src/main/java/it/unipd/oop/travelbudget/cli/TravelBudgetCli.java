
package it.unipd.oop.travelbudget.cli;

import it.unipd.oop.travelbudget.model.BudgetNode;
import it.unipd.oop.travelbudget.model.Category;
import it.unipd.oop.travelbudget.model.Currency;
import it.unipd.oop.travelbudget.model.Day;
import it.unipd.oop.travelbudget.model.Expense;
import it.unipd.oop.travelbudget.model.Stage;
import it.unipd.oop.travelbudget.model.Trip;
import it.unipd.oop.travelbudget.model.ReadOnlyBudgetNode;
import it.unipd.oop.travelbudget.persistence.JsonRepository;
import it.unipd.oop.travelbudget.service.BudgetNodeAbstractFactory;
import it.unipd.oop.travelbudget.service.BudgetNodeFactory;
import it.unipd.oop.travelbudget.service.CurrencyConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.Stack;
import it.unipd.oop.travelbudget.model.TripMemento;

// gestisce tutta la CLI: menu, lettura input e navigazione.
// observer registrato via lambda per monitorare il budget in tempo reale,
// stack di TripMemento (LIFO) per l'undo, exception shielding su tutti gli input dell'utente
// cosi nessuna eccezione interna arriva mai visibile all'utente finale
public final class TravelBudgetCli {

    private static final Logger LOGGER = Logger.getLogger(TravelBudgetCli.class.getName());
    private static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final BudgetNodeAbstractFactory factory;
    private final JsonRepository repository;
    private final CurrencyConverter converter;
    private final Scanner scanner;

    private final Stack<TripMemento> cronologiaAnnullamenti = new Stack<>();

    private Trip viaggioCorrente;

    public TravelBudgetCli(final BudgetNodeAbstractFactory factory,
            final JsonRepository repository,
            final CurrencyConverter converter) {
        this.factory = factory;
        this.repository = repository;
        this.converter = converter;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printBanner();
        initTrip();

        boolean inEsecuzione = true;
        while (inEsecuzione) {
            printMainMenu();
            final int scelta = readInt("Scelta: ");
            switch (scelta) {
                case 1 -> addStage();
                case 2 -> addDay();
                case 3 -> addExpense();
                case 4 -> showSummary();
                case 5 -> showStatsByCategory();
                case 6 -> saveTrip();
                case 7 -> undoLastAction();
                case 8 -> inEsecuzione = false;
                default -> System.out.println("Opzione non valida. Scegliere un numero tra 1 e 8.");
            }
        }
        System.out.println("\nArrivederci!");
    }

    // prepariamo il viaggio iniziale: chiediamo all'utente se vuole riprendere un viaggio salvato su file o crearne uno nuovo di zecca

    private void initTrip() {
        System.out.println("Ehi! Ti va di caricare un viaggio che hai già salvato oppure ne iniziamo uno nuovo da zero?");
        System.out.print("Scrivi 's' per caricare o 'n' per iniziare un nuovo viaggio: ");
        final String answer = scanner.nextLine().trim().toLowerCase();
        if ("s".equals(answer)) {
            loadTrip();
        } else {
            createNewTrip();
        }
    }

    private void createNewTrip() {
        System.out.println("\n--- Bene, iniziamo a pianificare un nuovo viaggio! ---");
        while (true) {
            try {
                final String nomeViaggio = readText("Come vogliamo chiamare questo viaggio? ");
                final LocalDate dataInizio = readDate("Quando inizia il viaggio? (GG/M/AAAA, es. 15/4/2026): ");
                final LocalDate dataFine = readDate("E quando si conclude? (GG/M/AAAA): ");
                final Currency valuta = readCurrency(
                        "Quale sarà la valuta principale di riferimento? " + Arrays.toString(Currency.values()) + ": ");
                final double budgetTotale = readPositiveDouble("Qual è la cifra massima di budget pianificata? ");
                this.viaggioCorrente = (Trip) factory.createTrip(nomeViaggio, dataInizio, dataFine, valuta, budgetTotale);
                setupCurrentTrip();
                System.out.println("Perfetto! Ho creato il viaggio '" + nomeViaggio + "'.");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Ops, c'è un errore: " + e.getMessage() + " Riprova a inserire i dati.");
                LOGGER.warning("Trip creation failed: " + e.getMessage());
            }
        }
    }

    private void loadTrip() {
        while (true) {
            final String percorsoFile = readText("Inserisci il percorso del file JSON da caricare (es. viaggio.json): ");
            try {
                final Path percorso = Paths.get(percorsoFile);
                this.viaggioCorrente = repository.load(percorso);
                setupCurrentTrip();
                System.out.println("Fatto! Ho caricato il viaggio '" + viaggioCorrente.getName() + "'.");
                return;
            } catch (InvalidPathException e) {
                System.out.println("Mmm, il percorso specificato non sembra valido. Controlla come lo hai scritto.");
                LOGGER.warning("Load failed (invalid path): " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Non sono riuscito a caricare il file: " + friendlyIoMessage(e));
                LOGGER.warning("Load failed: " + e.getMessage());
            }
            System.out.print("Vuoi riprovare a inserire il percorso? [s/n]: ");
            if (!"s".equals(scanner.nextLine().trim().toLowerCase())) {
                createNewTrip();
                return;
            }
        }
    }

    // qui configuriamo l'Observer per controllare che le spese non superino il budget
    // e definiamo le funzioni di supporto per salvare lo stato corrente (Memento) o ripristinarlo

    private void setupCurrentTrip() {
        cronologiaAnnullamenti.clear();
        this.viaggioCorrente.addObserver(trip -> {
            final double spent = totalInEuro();
            final double budget = converter.convertToEuro(trip.getBudget(), trip.getCurrency());
            if (budget > 0) {
                final double percent = (spent / budget) * 100;
                if (percent >= 100) {
                    System.out.println("\n[OBSERVER] 🚨 Attenzione: hai esaurito tutto il budget che avevi a disposizione! ("
                            + String.format("%.1f", percent) + "%)");
                } else if (percent >= 90) {
                    System.out.println("\n[OBSERVER] ⚠️ Occhio: hai superato il 90% del tuo budget disponibile! ("
                            + String.format("%.1f", percent) + "%)");
                }
            }
        });
    }

    private void saveSnapshot() {
        cronologiaAnnullamenti.push(viaggioCorrente.createMemento());
    }

    private void undoLastAction() {
        if (cronologiaAnnullamenti.isEmpty()) {
            System.out.println("Non ci sono azioni precedenti da annullare.");
            return;
        }
        final TripMemento memento = cronologiaAnnullamenti.pop();
        viaggioCorrente.restore(memento);
        System.out.println("Fatto! Ho annullato l'ultimo inserimento.");
    }

    // ecco le funzioni interattive della CLI: servono a guidare l'utente nell'inserimento di nuove tappe, giorni e singole spese

    private void addStage() {
        System.out.println("\n--- Aggiungiamo una nuova tappa a questo viaggio! ---");
        while (true) {
            try {
                final String nomeTappa = readText("Qual è il nome di questa tappa? (es. una città o una meta): ");
                final LocalDate dataInizio = readDate("In che data inizia questa tappa? (GG/M/AAAA): ");
                final LocalDate dataFine = readDate("E quando si conclude questa tappa? (GG/M/AAAA): ");
                final Currency valuta = readCurrency("Quale valuta si userà in questa tappa? " + Arrays.toString(Currency.values()) + ": ");
                final double budgetTappa = readPositiveDouble("Quanto budget vuoi dedicare a questa specifica tappa? ");
                final BudgetNode nuovaTappa = factory.createStage(nomeTappa, dataInizio, dataFine, valuta, budgetTappa);
                saveSnapshot();
                viaggioCorrente.addChild(nuovaTappa);
                viaggioCorrente.notifyObservers();
                System.out.println("Tappa '" + nomeTappa + "' inserita nel viaggio!");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Ops, c'è un errore: " + e.getMessage() + " Riprova a inserire i dati.");
                LOGGER.warning("Stage creation failed: " + e.getMessage());
            }
        }
    }

    private void addDay() {
        System.out.println("\n--- Aggiungiamo un giorno specifico ad una tappa ---");
        final List<Stage> tappeDisponibili = viaggioCorrente.getStages();
        if (tappeDisponibili.isEmpty()) {
            System.out.println("Non ci sono ancora tappe in questo viaggio. Aggiungine prima una!");
            return;
        }
        final Stage tappaSelezionata = selectStage(tappeDisponibili);
        if (tappaSelezionata == null)
            return;

        while (true) {
            try {
                final LocalDate dataGiorno = readDate("Per quale data vuoi creare questa giornata? (GG/M/AAAA): ");
                final BudgetNode nuovoGiorno = factory.createDay(dataGiorno);
                saveSnapshot();
                tappaSelezionata.addChild(nuovoGiorno);
                viaggioCorrente.notifyObservers();
                System.out.println("Giorno " + dataGiorno.format(FORMATTER_DATA) + " aggiunto alla tappa '" + tappaSelezionata.getName() + "'!");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Ops, c'è un errore: " + e.getMessage() + " Riprova.");
                LOGGER.warning("Day creation failed: " + e.getMessage());
            }
        }
    }

    private void addExpense() {
        System.out.println("\n--- Registriamo una nuova spesa ---");
        final List<Stage> tappeDisponibili = viaggioCorrente.getStages();
        if (tappeDisponibili.isEmpty()) {
            System.out.println("Non ci sono ancora tappe. Devi prima aggiungere una tappa per poter inserire una spesa.");
            return;
        }
        final Stage tappaSelezionata = selectStage(tappeDisponibili);
        if (tappaSelezionata == null)
            return;

        final List<Day> giorniDisponibili = tappaSelezionata.getDays();
        if (giorniDisponibili.isEmpty()) {
            System.out.println("Questa tappa non ha ancora giornate registrate. Crea prima un giorno!");
            return;
        }
        final Day giornoSelezionato = selectDay(giorniDisponibili);
        if (giornoSelezionato == null)
            return;

        while (true) {
            try {
                final double importo = readPositiveDouble("Quanto hai speso? (inserisci la cifra): ");
                final Currency valuta = readCurrency("Che valuta hai usato per pagare? Scegli tra " + Arrays.toString(Currency.values()) + ": ");
                final Category categoria = readCategory("A quale categoria appartiene questa spesa? Scegli tra " + Arrays.toString(Category.values()) + ": ");
                final String luogo = readText("In che posto fisico hai fatto questo acquisto? (es. nome del negozio o città): ");
                final String descrizione = readText("Scrivi una breve descrizione o nota per questa spesa: ");
                final BudgetNode nuovaSpesa = factory.createExpense(
                        importo, valuta, categoria, giornoSelezionato.getDate(), luogo, descrizione);
                saveSnapshot();
                giornoSelezionato.addChild(nuovaSpesa);
                viaggioCorrente.notifyObservers();
                System.out.printf("Spesa registrata con successo: %.2f %s per %s a %s!%n",
                        importo, valuta, descrizione, luogo);
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Ops, c'è un errore: " + e.getMessage() + " Riprova.");
                LOGGER.warning("Expense creation failed: " + e.getMessage());
            }
        }
    }

    private void showSummary() {
        // usiamo il pattern Decorator: avvolgiamo il viaggio in un oggetto di sola lettura per essere sicuri
        // al 100% che durante la stampa del riepilogo non vengano effettuate modifiche accidentali ai dati
        final BudgetNode readOnly = new ReadOnlyBudgetNode(viaggioCorrente);
        System.out.println("\n=== Ecco il riepilogo del viaggio: " + readOnly.getName() + " ===");
        System.out.printf("Periodo: %s → %s%n",
                viaggioCorrente.getStartDate().format(FORMATTER_DATA),
                viaggioCorrente.getEndDate().format(FORMATTER_DATA));
        System.out.printf("Budget totale pianificato: %.2f %s%n", viaggioCorrente.getBudget(), viaggioCorrente.getCurrency());
        System.out.printf("Totale speso finora: %.2f EUR%n", totalInEuro());

        // sfruttiamo il pattern Iterator: questo ci permette di percorrere in modo ricorsivo l'albero del budget
        // (attraverso una visita DFS in profondità) per stampare ordinatamente tappe, giorni e spese collegate
        for (final BudgetNode node : viaggioCorrente) {
            if (node instanceof Stage s) {
                System.out.printf("%n  -> Tappa: %s (Totale tappa: %.2f EUR)%n", s.getName(), stageInEuro(s));
            } else if (node instanceof Day d) {
                System.out.printf("    * Giorno %s (Speso oggi: %.2f EUR)%n", d.getDate().format(FORMATTER_DATA), dayInEuro(d));
            } else if (node instanceof Expense e) {
                System.out.printf("      [%s] %.2f %s - %s (%s)%n",
                        e.getCategory(), e.getAmount(), e.getCurrency(),
                        e.getDescription(), e.getLocation());
            }
        }
    }

    private void showStatsByCategory() {
        System.out.println("\n=== Statistiche per categoria ===");

        final List<Expense> tutteLeSpese = collectNodesOfType(Expense.class);

        if (tutteLeSpese.isEmpty()) {
            System.out.println("Non ci sono ancora spese registrate in questo viaggio.");
            return;
        }

        // Collectors.toMap costruisce una mappa categoria → totale convertito in euro;
        // iteriamo su tutti i valori dell'enum per avere anche le categorie a zero
        final Map<Category, Double> totalePerCategoriaEur = Arrays.stream(Category.values())
                .collect(Collectors.toMap(
                        cat -> cat,
                        cat -> tutteLeSpese.stream()
                                .filter(e -> e.getCategory() == cat)
                                .mapToDouble(e -> converter.convertToEuro(e.getAmount(), e.getCurrency()))
                                .sum()));

        totalePerCategoriaEur.forEach((cat, total) -> {
            if (total > 0) {
                System.out.printf("  %-12s %.2f EUR%n", cat, total);
            }
        });

        final double totaleComplessivoEur = totalePerCategoriaEur.values().stream().mapToDouble(Double::doubleValue).sum();
        System.out.printf("%nTotale: %.2f EUR%n", totaleComplessivoEur);

        // stream + Optional.ifPresent: cerco la spesa massima e la stampo solo se esiste
        tutteLeSpese.stream()
                .max((a, b) -> Double.compare(
                        converter.convertToEuro(a.getAmount(), a.getCurrency()),
                        converter.convertToEuro(b.getAmount(), b.getCurrency())))
                .ifPresent(e -> System.out.printf("Spesa più alta: %.2f %s — %s (%s)%n",
                        e.getAmount(), e.getCurrency(), e.getDescription(), e.getLocation()));
    }

    private void saveTrip() {
        final String percorsoFile = readText("Sotto quale nome o percorso vuoi salvare il file JSON di questo viaggio? (es. viaggio.json): ");
        try {
            final Path percorso = Paths.get(percorsoFile);
            repository.save(viaggioCorrente, percorso);
            System.out.println("Salvataggio completato! Ho salvato il viaggio in: " + percorso.toAbsolutePath());
        } catch (InvalidPathException e) {
            System.out.println("Mmm, il percorso specificato non sembra corretto. Controlla come lo hai scritto.");
            LOGGER.warning("Save failed (invalid path): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Non sono riuscito a salvare il viaggio: " + friendlyIoMessage(e));
            LOGGER.severe("Save failed: " + e.getMessage());
        }
    }

    // Semplici funzioni di supporto per permettere all'utente di selezionare una tappa o un giorno specifico da un elenco numerato

    private Stage selectStage(final List<Stage> tappe) {
        System.out.println("Ecco le tappe che ho trovato. Quale vuoi scegliere?");
        for (int i = 0; i < tappe.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, tappe.get(i).getName());
        }
        final int indiceSelezionato = readInt("Inserisci il numero corrispondente alla tappa: ") - 1;
        if (indiceSelezionato < 0 || indiceSelezionato >= tappe.size()) {
            System.out.println("Scelta non corretta. Riprova.");
            return null;
        }
        return tappe.get(indiceSelezionato);
    }

    private Day selectDay(final List<Day> giorni) {
        System.out.println("Ecco i giorni disponibili per questa tappa. Quale vuoi scegliere?");
        for (int i = 0; i < giorni.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, giorni.get(i).getDate().format(FORMATTER_DATA));
        }
        final int indiceSelezionato = readInt("Inserisci il numero del giorno: ") - 1;
        if (indiceSelezionato < 0 || indiceSelezionato >= giorni.size()) {
            System.out.println("Scelta non corretta. Riprova.");
            return null;
        }
        return giorni.get(indiceSelezionato);
    }

    // qui leggiamo e validiamo l'input dell'utente. applichiamo l'Exception Shielding: se l'utente inserisce dati non validi
    // (come lettere al posto di numeri o date scritte male), gestiamo l'errore senza far crashare l'app e gli chiediamo di riprovare

    private int readInt(final String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Inserimento non valido: per favore inserisci un numero intero.");
            }
        }
    }

    private double readPositiveDouble(final String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                final double valore = Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
                if (valore <= 0) {
                    System.out.println("La cifra deve essere maggiore di zero.");
                    continue;
                }
                return valore;
            } catch (NumberFormatException e) {
                System.out.println("Inserimento non valido: per favore inserisci un numero decimale (es. 12.50).");
            }
        }
    }

    private String readText(final String prompt) {
        while (true) {
            System.out.print(prompt);
            final String valore = scanner.nextLine().trim();
            if (!valore.isBlank()) {
                return valore;
            }
            System.out.println("Questo valore non può essere lasciato vuoto.");
        }
    }

    private LocalDate readDate(final String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalDate.parse(scanner.nextLine().trim(), FORMATTER_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("Formato data non valido. Usare GG/M/AAAA (es. 15/4/2026).");
            }
        }
    }

    private Currency readCurrency(final String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Currency.valueOf(scanner.nextLine().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Valuta non riconosciuta. Valori validi: " + Arrays.toString(Currency.values()));
            }
        }
    }

    private Category readCategory(final String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Category.valueOf(scanner.nextLine().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Categoria non riconosciuta. Valori validi: " + Arrays.toString(Category.values()));
            }
        }
    }

    // metodi pratici per convertire e sommare rapidamente in Euro le spese effettuate a livello di viaggio, tappa o singolo giorno

    // scorre l'albero del viaggio e raccoglie solo i nodi del tipo richiesto;
    // il tipo bound <T extends BudgetNode> garantisce che funzioni solo su nodi del modello
    private <T extends BudgetNode> List<T> collectNodesOfType(final Class<T> type) {
        return StreamSupport.stream(viaggioCorrente.spliterator(), false)
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    private double totalInEuro() {
        return collectNodesOfType(Expense.class).stream()
                .mapToDouble(e -> converter.convertToEuro(e.getAmount(), e.getCurrency()))
                .sum();
    }

    private double stageInEuro(final Stage stage) {
        return stage.getDays().stream()
                .flatMap(d -> d.getExpenses().stream())
                .mapToDouble(e -> converter.convertToEuro(e.getAmount(), e.getCurrency()))
                .sum();
    }

    private double dayInEuro(final Day day) {
        return day.getExpenses().stream()
                .mapToDouble(e -> converter.convertToEuro(e.getAmount(), e.getCurrency()))
                .sum();
    }

    // traduce le eccezioni di I/O in messaggi comprensibili — exception shielding verso l'utente
    private static String friendlyIoMessage(final IOException e) {
        if (e instanceof NoSuchFileException || e instanceof FileNotFoundException) {
            return "il file non esiste o non è leggibile.";
        }
        if (e instanceof AccessDeniedException) {
            return "permessi insufficienti per accedere al file.";
        }
        if (e instanceof JsonProcessingException) {
            return "il contenuto del file non è un viaggio valido.";
        }
        return "operazione I/O non riuscita.";
    }

    private void printBanner() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     Travel Budget Companion          ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    private void printMainMenu() {
        System.out.println("\n=== VIAGGIO CORRENTE: " + viaggioCorrente.getName() + " ===");
        System.out.println("Soldi spesi finora: " + String.format("%.2f", totalInEuro()) + " EUR | Budget totale: " + String.format("%.2f", viaggioCorrente.getBudget()) + " " + viaggioCorrente.getCurrency());
        System.out.println("Cosa vuoi fare adesso?");
        System.out.println("1. Aggiungi tappa");
        System.out.println("2. Aggiungi giorno a una tappa");
        System.out.println("3. Aggiungi spesa a un giorno");
        System.out.println("4. Visualizza riepilogo");
        System.out.println("5. Statistiche per categoria");
        System.out.println("6. Salva viaggio");
        System.out.println("7. Annulla ultima azione (Undo)");
        System.out.println("8. Esci");
    }

    // il punto di partenza effettivo dell'applicazione: configuriamo i registri di sistema (logger) e facciamo partire la CLI

    public static void main(final String[] args) {
        configureLogging();
        try {
            // poiché la nostra BudgetNodeFactory implementa il pattern Singleton, non possiamo usare il costruttore new,
            // ma dobbiamo recuperare l'unica istanza condivisa chiamando il metodo statico getInstance()
            new TravelBudgetCli(
                    BudgetNodeFactory.getInstance(),
                    new JsonRepository(),
                    new CurrencyConverter()).run();
        } catch (RuntimeException e) {
            System.out.println("Errore applicativo inatteso. Riprovare.");
            LOGGER.severe("Unhandled runtime exception: " + e.getMessage());
        }
    }

    // proviamo a caricare il file esterno logging.properties. se non ci riusciamo, silenziamo i log di livello informativo (INFO)
    // per assicurarci che l'interfaccia a riga di comando rimanga bella pulita e non venga sporcata da dettagli tecnici
    private static void configureLogging() {
        try (InputStream in = TravelBudgetCli.class.getResourceAsStream(
                "/logging.properties")) {
            if (in != null) {
                LogManager.getLogManager().readConfiguration(in);
                return;
            }
        } catch (IOException ignored) {
            // se per qualsiasi motivo la configurazione personalizzata non dovesse caricarsi, applichiamo un piano B (fallback) qui sotto
        }
        // Fallback: alza il livello dei console handler a WARNING
        final Logger root = Logger.getLogger("");
        for (final var h : root.getHandlers()) {
            h.setLevel(java.util.logging.Level.WARNING);
        }
    }
}
