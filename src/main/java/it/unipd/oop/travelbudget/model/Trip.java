package it.unipd.oop.travelbudget.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// questa è la classe principale che rappresenta un viaggio intero, eredita da AbstractNamedNode
// così riusa i campi comuni (nome, date, budget ecc) senza doverli riscrivere ogni volta
// implementa Iterable perché volevo poter fare un for-each su tutto l'albero del viaggio
// senza che la CLI dovesse conoscere la struttura interna dei nodi
public final class Trip extends AbstractNamedNode implements Iterable<BudgetNode> {

    // questo ObjectMapper serve solo per il memento, lo uso per trasformare le tappe in una
    // stringa JSON e fare una copia profonda dello stato senza problemi di riferimenti condivisi
    @JsonIgnore
    private static final ObjectMapper MEMENTO_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @JsonIgnore
    private final List<BudgetObserver> osservatori = new ArrayList<>();

    @JsonCreator
    public Trip(
            @JsonProperty("name") final String name,
            @JsonProperty("startDate") final LocalDate startDate,
            @JsonProperty("endDate") final LocalDate endDate,
            @JsonProperty("currency") final Currency currency,
            @JsonProperty("budget") final double budget) {
        super(name, startDate, endDate, currency, budget);
    }

    @Override
    protected void validateChild(final BudgetNode nodo) {
        // qui decido che un viaggio può contenere solo tappe (Stage), se qualcuno prova ad
        // aggiungere qualcos'altro lancio un'eccezione
        // controllo anche che le date della tappa rientrino nel periodo del viaggio
        if (!(nodo instanceof Stage tappa)) {
            throw new IllegalArgumentException("A trip can only contain stages.");
        }
        if (tappa.getStartDate().isBefore(getStartDate())
                || tappa.getEndDate().isAfter(getEndDate())) {
            throw new IllegalArgumentException(
                    "Stage dates must fall within the trip period ("
                            + getStartDate() + " - " + getEndDate() + ").");
        }
    }

    @Override
    public Iterator<BudgetNode> iterator() {
        // qui creo un iteratore che attraversa tutto l'albero in profondità (DFS) e lo appiattisce
        // in una lista semplice, in questo modo la CLI può scorrere tappe, giorni e spese con un
        // unico for-each senza sapere come è fatto l'albero dentro
        final List<BudgetNode> nodiAppiattiti = new ArrayList<>();
        for (final BudgetNode tappa : getChildren()) {
            nodiAppiattiti.add(tappa);
            for (final BudgetNode giorno : tappa.getChildren()) {
                nodiAppiattiti.add(giorno);
                nodiAppiattiti.addAll(giorno.getChildren());
            }
        }
        return nodiAppiattiti.iterator();
    }

    @JsonGetter("stages")
    public List<Stage> getStages() {
        return getChildren().stream().map(c -> (Stage) c).toList();
    }

    @JsonSetter("stages")
    void setStagesFromJson(final List<Stage> tappe) {
        if (tappe != null) {
            tappe.forEach(this::addChild);
        }
    }

    @Override
    public String toString() {
        return "Trip{name='" + getName() + "', " + getStartDate() + " → " + getEndDate()
                + ", budget=" + getBudget() + " " + getCurrency()
                + ", stages=" + getChildren().size() + "}";
    }

    // qui tengo la lista degli observer e li notifico quando qualcosa cambia nel viaggio,
    // così per esempio la CLI può mostrare un avviso se il budget sta finendo

    public void addObserver(final BudgetObserver osservatore) {
        if (osservatore != null && !osservatori.contains(osservatore)) {
            osservatori.add(osservatore);
        }
    }

    public void notifyObservers() {
        for (final BudgetObserver obs : osservatori) {
            obs.update(this);
        }
    }

    // per implementare l'undo uso il pattern Memento: converto tutte le tappe in una stringa JSON
    // così ottengo una copia profonda completa, se usassi un semplice clone() i riferimenti interni
    // resterebbero condivisi e modifiche future rovinerebbero il backup
    public TripMemento createMemento() {
        try {
            final String istantaneaJSON = MEMENTO_MAPPER.writeValueAsString(getStages());
            return new TripMemento(istantaneaJSON);
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile creare il memento: " + e.getMessage(), e);
        }
    }

    public void restore(final TripMemento memento) {
        // per ripristinare uno stato precedente svuoto tutti i figli attuali e ricarico quelli
        // salvati nel memento deserializzando il JSON
        try {
            final List<Stage> tappeSalvate = MEMENTO_MAPPER.readValue(
                    memento.getIstantaneaJSON(),
                    new TypeReference<List<Stage>>() {});
            clearChildren();
            tappeSalvate.forEach(this::addChild);
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile ripristinare il memento: " + e.getMessage(), e);
        }
    }
}
