package it.unipd.oop.travelbudget.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseBuilderTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 15);

    @Test
    void builder_createsExpenseCorrectly() {
        final Expense expense = Expense.builder()
                .amount(25.50)
                .currency(Currency.EUR)
                .category(Category.FOOD)
                .date(DATE)
                .location("Kyoto")
                .description("Sushi dinner")
                .build();

        assertEquals(25.50, expense.getAmount());
        assertEquals(Currency.EUR, expense.getCurrency());
        assertEquals(Category.FOOD, expense.getCategory());
        assertEquals(DATE, expense.getDate());
        assertEquals("Kyoto", expense.getLocation());
        assertEquals("Sushi dinner", expense.getDescription());
    }

    @Test
    void builder_propagatesValidationOnBuild() {
        assertThrows(IllegalArgumentException.class, () ->
                Expense.builder()
                        .amount(-5.0)
                        .currency(Currency.USD)
                        .category(Category.TRANSPORT)
                        .date(DATE)
                        .location("Tokyo")
                        .description("Bus")
                        .build());
    }

    @Test
    void builder_producesEquivalentResultToDirectConstructor() {
        final Expense fromBuilder = Expense.builder()
                .amount(10.0).currency(Currency.GBP).category(Category.LODGING)
                .date(DATE).location("London").description("Hotel")
                .build();
        final Expense direct = new Expense(10.0, Currency.GBP, Category.LODGING, DATE, "London", "Hotel");

        assertEquals(direct.getTotal(), fromBuilder.getTotal());
        assertEquals(direct.getDescription(), fromBuilder.getDescription());
    }
}
