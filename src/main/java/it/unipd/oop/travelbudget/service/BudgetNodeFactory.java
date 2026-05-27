package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.BudgetNode;
import it.unipd.oop.travelbudget.model.Category;
import it.unipd.oop.travelbudget.model.Currency;
import it.unipd.oop.travelbudget.model.Day;
import it.unipd.oop.travelbudget.model.Expense;
import it.unipd.oop.travelbudget.model.Stage;
import it.unipd.oop.travelbudget.model.Trip;
import java.time.LocalDate;
import java.util.logging.Logger;

// implementazione concreta della Abstract Factory, è anche un Singleton perché non ha stato mutabile
// quindi basta una sola istanza per tutta l'app. il costruttore è privato e si accede tramite getInstance()
// in questo modo la CLI non usa mai direttamente la parola chiave new per creare gli oggetti del dominio
public final class BudgetNodeFactory implements BudgetNodeAbstractFactory {

    private static final Logger LOGGER = Logger.getLogger(BudgetNodeFactory.class.getName());

    private static final BudgetNodeFactory INSTANCE = new BudgetNodeFactory();

    private BudgetNodeFactory() {}

    public static BudgetNodeFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public BudgetNode createExpense(
            final double amount,
            final Currency currency,
            final Category category,
            final LocalDate date,
            final String location,
            final String description) {
        LOGGER.fine("Creating Expense: " + category + " " + amount + " " + currency + " at " + location);
        // uso il builder perche Expense ha 6 parametri — senza builder l'ordine degli argomenti
        // sarebbe impossibile da ricordare per chi chiama questo metodo
        return Expense.builder()
                .amount(amount)
                .currency(currency)
                .category(category)
                .date(date)
                .location(location)
                .description(description)
                .build();
    }

    @Override
    public BudgetNode createTrip(
            final String name,
            final LocalDate startDate,
            final LocalDate endDate,
            final Currency currency,
            final double budget) {
        LOGGER.fine("Creating Trip: '" + name + "' " + startDate + " → " + endDate);
        return new Trip(name, startDate, endDate, currency, budget);
    }

    @Override
    public BudgetNode createStage(
            final String name,
            final LocalDate startDate,
            final LocalDate endDate,
            final Currency currency,
            final double budget) {
        LOGGER.fine("Creating Stage: '" + name + "' " + startDate + " → " + endDate);
        return new Stage(name, startDate, endDate, currency, budget);
    }

    @Override
    public BudgetNode createDay(final LocalDate date) {
        LOGGER.fine("Creating Day: " + date);
        return new Day(date);
    }
}
