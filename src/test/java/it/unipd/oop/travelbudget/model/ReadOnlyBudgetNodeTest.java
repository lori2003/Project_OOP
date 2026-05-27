package it.unipd.oop.travelbudget.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReadOnlyBudgetNodeTest {

    private static final LocalDate START = LocalDate.of(2026, 4, 1);
    private static final LocalDate END   = LocalDate.of(2026, 4, 30);

    @Test
    void decorator_delegatesGetTotal() {
        final Trip trip = new Trip("Test", START, END, Currency.EUR, 500.0);
        final BudgetNode readOnly = new ReadOnlyBudgetNode(trip);
        assertEquals(0.0, readOnly.getTotal());
    }

    @Test
    void decorator_delegatesGetName() {
        final Trip trip = new Trip("Giappone", START, END, Currency.JPY, 2000.0);
        final BudgetNode readOnly = new ReadOnlyBudgetNode(trip);
        assertEquals("Giappone", readOnly.getName());
    }

    @Test
    void decorator_delegatesGetChildren() {
        final Trip trip = new Trip("Test", START, END, Currency.EUR, 100.0);
        final BudgetNode readOnly = new ReadOnlyBudgetNode(trip);
        assertTrue(readOnly.getChildren().isEmpty());
    }

    @Test
    void decorator_blocksAddChild() {
        final Trip trip = new Trip("Test", START, END, Currency.EUR, 100.0);
        final BudgetNode readOnly = new ReadOnlyBudgetNode(trip);
        final Stage stage = new Stage("Roma", START, END, Currency.EUR, 50.0);
        assertThrows(UnsupportedOperationException.class, () -> readOnly.addChild(stage));
    }

    @Test
    void decorator_rejectsNullWrapped() {
        assertThrows(NullPointerException.class, () -> new ReadOnlyBudgetNode(null));
    }
}
