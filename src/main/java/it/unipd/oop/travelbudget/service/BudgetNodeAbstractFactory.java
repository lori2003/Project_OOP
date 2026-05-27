package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.BudgetNode;
import it.unipd.oop.travelbudget.model.Category;
import it.unipd.oop.travelbudget.model.Currency;
import java.time.LocalDate;

// abstract factory per gli oggetti del dominio.
// "abstract" perche c'è questa interfaccia separata dall'implementazione concreta (BudgetNodeFactory):
// la CLI dipende solo dall'interfaccia, quindi in un test posso passare una factory diversa
// senza cambiare nulla nel resto del codice
public interface BudgetNodeAbstractFactory {

    BudgetNode createExpense(
            double amount,
            Currency currency,
            Category category,
            LocalDate date,
            String location,
            String description);

    BudgetNode createTrip(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Currency currency,
            double budget);

    BudgetNode createStage(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Currency currency,
            double budget);

    BudgetNode createDay(LocalDate date);
}
