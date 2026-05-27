package it.unipd.oop.travelbudget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// questa classe astratta contiene tutti i campi comuni (nome, date, valuta, budget) che servono
// sia a Trip che a Stage, cosi non li devo riscrivere due volte - il metodo addChild è un
// Template Method perché la parte comune (controllare che il nodo non sia null e aggiungerlo)
// sta qui, mentre la parte specifica (che tipo di figlio accettare) la decide ogni sottoclasse
// con validateChild
public abstract class AbstractNamedNode implements BudgetNode {

    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Currency currency;
    private final double budget;

    @JsonIgnore
    private final List<BudgetNode> nodiFigli = new ArrayList<>();

    protected AbstractNamedNode(
            final String name,
            final LocalDate startDate,
            final LocalDate endDate,
            final Currency currency,
            final double budget) {
        this.name = requireText(name, "name");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = Objects.requireNonNull(endDate, "endDate");
        this.currency = Objects.requireNonNull(currency, "currency");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date.");
        }
        if (budget < 0) {
            throw new IllegalArgumentException("Budget must not be negative.");
        }
        this.budget = budget;
    }

    @JsonIgnore
    @Override
    public final double getTotal() {
        // il totale lo calcolo sommando ricorsivamente i totali di tutti i figli, come prevede
        // il pattern Composite: ogni nodo chiede ai propri figli quanto hanno speso
        return this.nodiFigli.stream().mapToDouble(BudgetNode::getTotal).sum();
    }

    @Override
    public final void addChild(final BudgetNode nodo) {
        // questo è il Template Method: qui fisso il flusso comune (controllo null, valido,
        // aggiungo), la parte che cambia è validateChild che ogni sottoclasse ridefinisce
        Objects.requireNonNull(nodo, "nodo");
        validateChild(nodo);
        this.nodiFigli.add(nodo);
    }

    // questo metodo astratto è il gancio (hook) del template method, ogni sottoclasse lo
    // implementa decidendo che figli accettare (Trip accetta solo Stage, Stage accetta solo Day)
    protected abstract void validateChild(BudgetNode nodo);


    @Override
    public String getName() {
        return this.name;
    }

    @JsonIgnore
    @Override
    public List<BudgetNode> getChildren() {
        // restituisco una copia non modificabile della lista così chi la riceve non può aggiungere
        // o togliere elementi saltando i controlli di addChild
        return Collections.unmodifiableList(this.nodiFigli);
    }

    // serve per il memento, quando faccio l'undo devo svuotare i figli e ricaricarli dal backup JSON
    protected void clearChildren() {
        this.nodiFigli.clear();
    }

    public LocalDate getStartDate() { return this.startDate; }
    public LocalDate getEndDate()   { return this.endDate; }
    public Currency getCurrency()   { return this.currency; }
    public double getBudget()       { return this.budget; }

    protected static String requireText(final String testo, final String nomeCampo) {
        if (testo == null || testo.isBlank()) {
            throw new IllegalArgumentException(nomeCampo + " must not be blank.");
        }
        return testo;
    }
}
