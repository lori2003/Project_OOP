package it.unipd.oop.travelbudget.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// questa è la foglia (leaf) del pattern Composite, cioè rappresenta una singola spesa concreta
// tutti i campi sono final quindi una volta creata la spesa non si può più modificare, in questo
// modo evito problemi di stato inconsistente se qualcosa la tocca dopo la creazione
public final class Expense implements BudgetNode {
    private final double amount;
    private final Currency currency;
    private final Category category;
    private final LocalDate date;
    private final String location;
    private final String description;

    @JsonCreator
    public Expense(
            @JsonProperty("amount") final double amount,
            @JsonProperty("currency") final Currency currency,
            @JsonProperty("category") final Category category,
            @JsonProperty("date") final LocalDate date,
            @JsonProperty("location") final String location,
            @JsonProperty("description") final String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "currency");
        this.category = Objects.requireNonNull(category, "category");
        this.date = Objects.requireNonNull(date, "date");
        this.location = requireText(location, "location");
        this.description = requireText(description, "description");
    }



    @JsonIgnore
    @Override
    public double getTotal() { return this.amount; }

    @JsonIgnore
    @Override
    public String getName() { return this.description; }

    // una spesa è una foglia quindi non può avere figli, lanciare UnsupportedOperationException
    // tecnicamente viola il principio di Liskov (LSP) ma l'ho fatto apposta: preferisco avere
    // un'interfaccia unica per tutti i nodi (trasparenza) piuttosto che creare interfacce separate
    @Override
    public void addChild(final BudgetNode nodo) {
        throw new UnsupportedOperationException("An expense is a leaf and cannot have children.");
    }

    @JsonIgnore
    @Override
    public List<BudgetNode> getChildren() { return Collections.emptyList(); }


    public double getAmount()       { return this.amount; }
    public Currency getCurrency()   { return this.currency; }
    public Category getCategory()   { return this.category; }
    public LocalDate getDate()      { return this.date; }
    public String getLocation()     { return this.location; }
    public String getDescription()  { return this.description; }

    @Override
    public String toString() {
        return String.format("Expense{%.2f %s, %s, %s, '%s', '%s'}",
                this.amount, this.currency, this.category,
                this.date, this.location, this.description);
    }

    // uso il pattern Builder perché Expense ha 6 parametri nel costruttore e senza il builder
    // sarebbe difficile capire quale argomento è quale

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double amount;
        private Currency currency;
        private Category category;
        private LocalDate date;
        private String location;
        private String description;

        private Builder() {}

        public Builder amount(final double amount)           { this.amount = amount;           return this; }
        public Builder currency(final Currency currency)     { this.currency = currency;       return this; }
        public Builder category(final Category category)     { this.category = category;       return this; }
        public Builder date(final LocalDate date)            { this.date = date;               return this; }
        public Builder location(final String location)       { this.location = location;       return this; }
        public Builder description(final String description) { this.description = description; return this; }

        public Expense build() {
            return new Expense(amount, currency, category, date, location, description);
        }
    }


    private static String requireText(final String valore, final String nomeCampo) {
        if (valore == null || valore.isBlank()) {
            throw new IllegalArgumentException(nomeCampo + " must not be blank.");
        }
        return valore;
    }
}
