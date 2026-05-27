package it.unipd.oop.travelbudget.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 23);

    private Expense validExpense() {
        return new Expense(25.50, Currency.EUR, Category.FOOD, TODAY, "Kyoto", "Cena sushi");
    }

    @Test
    void constructorValid() {
        final Expense e = validExpense();
        assertEquals(25.50, e.getAmount());
        assertEquals(Currency.EUR, e.getCurrency());
        assertEquals(Category.FOOD, e.getCategory());
        assertEquals(TODAY, e.getDate());
        assertEquals("Kyoto", e.getLocation());
        assertEquals("Cena sushi", e.getDescription());
    }

    @Test
    void getTotal_returnsAmount() {
        assertEquals(25.50, validExpense().getTotal());
    }

    @Test
    void getName_returnsDescription() {
        assertEquals("Cena sushi", validExpense().getName());
    }

    @Test
    void constructorRejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new Expense(-1.0, Currency.EUR, Category.FOOD, TODAY, "Kyoto", "Test"));
    }

    @Test
    void constructorRejectsZeroAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new Expense(0.0, Currency.EUR, Category.FOOD, TODAY, "Kyoto", "Test"));
    }

    @Test
    void constructorRejectsNullCurrency() {
        assertThrows(NullPointerException.class, () ->
                new Expense(10.0, null, Category.FOOD, TODAY, "Kyoto", "Test"));
    }

    @Test
    void constructorRejectsNullCategory() {
        assertThrows(NullPointerException.class, () ->
                new Expense(10.0, Currency.EUR, null, TODAY, "Kyoto", "Test"));
    }

    @Test
    void constructorRejectsBlankLocation() {
        assertThrows(IllegalArgumentException.class, () ->
                new Expense(10.0, Currency.EUR, Category.FOOD, TODAY, "   ", "Test"));
    }

    @Test
    void constructorRejectsBlankDescription() {
        assertThrows(IllegalArgumentException.class, () ->
                new Expense(10.0, Currency.EUR, Category.FOOD, TODAY, "Kyoto", ""));
    }

    @Test
    void addChild_throwsUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class, () ->
                validExpense().addChild(validExpense()));
    }

    @Test
    void getChildren_returnsEmptyList() {
        assertTrue(validExpense().getChildren().isEmpty());
    }
}
