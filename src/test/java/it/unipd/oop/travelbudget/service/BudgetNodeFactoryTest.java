package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class BudgetNodeFactoryTest {

    private BudgetNodeAbstractFactory factory;
    private static final LocalDate START = LocalDate.of(2026, 4, 1);
    private static final LocalDate END = LocalDate.of(2026, 4, 30);

    @BeforeEach
    void setUp() {
        factory = BudgetNodeFactory.getInstance();
    }

    @Test
    void createExpense_returnsExpenseAsBudgetNode() {
        final BudgetNode node = factory.createExpense(
                10.0, Currency.EUR, Category.FOOD,
                LocalDate.of(2026, 4, 5), "Roma", "Pizza");
        assertInstanceOf(Expense.class, node);
        assertEquals(10.0, node.getTotal());
    }

    @Test
    void createTrip_returnsTripAsBudgetNode() {
        final BudgetNode node = factory.createTrip("Italia", START, END, Currency.EUR, 1000.0);
        assertInstanceOf(Trip.class, node);
        assertEquals("Italia", node.getName());
    }

    @Test
    void createStage_returnsStageAsBudgetNode() {
        final BudgetNode node = factory.createStage("Roma", START, END, Currency.EUR, 400.0);
        assertInstanceOf(Stage.class, node);
        assertEquals("Roma", node.getName());
    }

    @Test
    void createDay_returnsDayAsBudgetNode() {
        final BudgetNode node = factory.createDay(LocalDate.of(2026, 4, 10));
        assertInstanceOf(Day.class, node);
    }

    @Test
    void createExpense_propagatesValidationError() {
        assertThrows(IllegalArgumentException.class, () ->
                factory.createExpense(-5.0, Currency.EUR, Category.FOOD,
                        LocalDate.of(2026, 4, 5), "Roma", "Test"));
    }
}
