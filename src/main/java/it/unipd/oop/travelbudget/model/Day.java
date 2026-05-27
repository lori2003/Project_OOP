package it.unipd.oop.travelbudget.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// aggrega le spese di una singola giornata di viaggio.
// non estende AbstractNamedNode perche non ha nome, budget o valuta propri — solo una data.
// nel composite sta tra Stage (padre) ed Expense (foglia): è un nodo intermedio senza budget proprio
public final class Day implements BudgetNode {
    private final LocalDate date;
    
    @JsonIgnore
    private final List<BudgetNode> nodiFigli = new ArrayList<>();

    @JsonCreator
    public Day(@JsonProperty("date") final LocalDate date) {
        this.date = Objects.requireNonNull(date, "date");
    }

    @JsonIgnore
    @Override
    public double getTotal() {
        return this.nodiFigli.stream().mapToDouble(BudgetNode::getTotal).sum();
    }

    @JsonIgnore
    @Override
    public String getName() {
        return this.date.toString();
    }

    @Override
    public void addChild(final BudgetNode nodo) {
        // day è il confine tra Stage e Expense nel composite: solo le spese possono stare qui
        Objects.requireNonNull(nodo, "nodo");
        if (!(nodo instanceof Expense)) {
            throw new IllegalArgumentException("A day can only contain expenses.");
        }
        this.nodiFigli.add(nodo);
    }

    @JsonIgnore
    @Override
    public List<BudgetNode> getChildren() {
        return Collections.unmodifiableList(this.nodiFigli);
    }

    @JsonGetter("expenses")
    public List<Expense> getExpenses() {
        return this.nodiFigli.stream().map(c -> (Expense) c).toList();
    }

    @JsonSetter("expenses")
    void setExpensesFromJson(final List<Expense> spese) {
        if (spese != null) {
            spese.forEach(this::addChild);
        }
    }

    public LocalDate getDate() { return this.date; }

    @Override
    public String toString() {
        return "Day{date=" + this.date + ", expenses=" + this.nodiFigli.size() + "}";
    }
}
