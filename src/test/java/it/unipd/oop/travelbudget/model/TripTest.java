package it.unipd.oop.travelbudget.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Iterator;
import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    private static final LocalDate START = LocalDate.of(2026, 4, 1);
    private static final LocalDate END = LocalDate.of(2026, 4, 30);

    private Trip validTrip() {
        return new Trip("Giappone 2026", START, END, Currency.JPY, 2000.0);
    }

    @Test
    void constructorValid() {
        final Trip t = validTrip();
        assertEquals("Giappone 2026", t.getName());
        assertEquals(START, t.getStartDate());
        assertEquals(END, t.getEndDate());
        assertEquals(Currency.JPY, t.getCurrency());
        assertEquals(2000.0, t.getBudget());
    }

    @Test
    void constructorRejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () ->
                new Trip("Test", END, START, Currency.EUR, 0.0));
    }

    @Test
    void constructorRejectsNegativeBudget() {
        assertThrows(IllegalArgumentException.class, () ->
                new Trip("Test", START, END, Currency.EUR, -1.0));
    }

    @Test
    void constructorRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Trip("  ", START, END, Currency.EUR, 0.0));
    }

    @Test
    void addChild_acceptsStage() {
        final Trip trip = validTrip();
        final Stage stage = new Stage("Tokyo", START, END, Currency.JPY, 500.0);
        trip.addChild(stage);
        assertEquals(1, trip.getChildren().size());
    }

    @Test
    void addChild_rejectsExpense() {
        final Trip trip = validTrip();
        final Expense exp = new Expense(10.0, Currency.EUR, Category.FOOD,
                LocalDate.of(2026, 4, 5), "Kyoto", "Test");
        assertThrows(IllegalArgumentException.class, () -> trip.addChild(exp));
    }

    @Test
    void addChild_rejectsDay() {
        final Trip trip = validTrip();
        final Day day = new Day(LocalDate.of(2026, 4, 5));
        assertThrows(IllegalArgumentException.class, () -> trip.addChild(day));
    }

    @Test
    void getTotal_sumsDelegation() {
        final Trip trip = validTrip();
        final Stage stage = new Stage("Tokyo", START, END, Currency.JPY, 500.0);
        final Day day = new Day(LocalDate.of(2026, 4, 5));
        day.addChild(new Expense(30.0, Currency.EUR, Category.FOOD,
                LocalDate.of(2026, 4, 5), "Tokyo", "Ramen"));
        day.addChild(new Expense(20.0, Currency.EUR, Category.TRANSPORT,
                LocalDate.of(2026, 4, 5), "Tokyo", "Metro"));
        stage.addChild(day);
        trip.addChild(stage);

        assertEquals(50.0, trip.getTotal());
    }

    @Test
    void iterator_traversesAllNodes() {
        final Trip trip = validTrip();
        final Stage stage = new Stage("Tokyo", START, END, Currency.JPY, 500.0);
        final Day day = new Day(LocalDate.of(2026, 4, 5));
        final Expense exp = new Expense(15.0, Currency.EUR, Category.FOOD,
                LocalDate.of(2026, 4, 5), "Shinjuku", "Gyudon");
        day.addChild(exp);
        stage.addChild(day);
        trip.addChild(stage);

        final Iterator<BudgetNode> it = trip.iterator();
        assertTrue(it.hasNext());
        assertSame(stage, it.next());
        assertTrue(it.hasNext());
        assertSame(day, it.next());
        assertTrue(it.hasNext());
        assertSame(exp, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void addChild_rejectsStageOutsideTripPeriod() {
        final Trip trip = validTrip();
        final Stage outside = new Stage("FuoriRange",
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 5),
                Currency.JPY, 100.0);
        assertThrows(IllegalArgumentException.class, () -> trip.addChild(outside));
    }

    @Test
    void stage_addChild_rejectsDayOutsideStagePeriod() {
        final Stage stage = new Stage("Tokyo",
                LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 10),
                Currency.JPY, 100.0);
        final Day farFuture = new Day(LocalDate.of(2099, 12, 31));
        assertThrows(IllegalArgumentException.class, () -> stage.addChild(farFuture));
    }

    @Test
    void getChildren_returnsUnmodifiableView() {
        final Trip trip = validTrip();
        assertThrows(UnsupportedOperationException.class, () ->
                trip.getChildren().add(new Stage("X", START, END, Currency.EUR, 0.0)));
    }

    @Test
    void memento_canRestoreStateToPreviousSnapshot() {
        final Trip trip = validTrip();
        trip.addChild(new Stage("Tokyo", START, END, Currency.JPY, 500.0));

        final TripMemento snapshot = trip.createMemento();

        trip.addChild(new Stage("Kyoto",
                LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 10),
                Currency.JPY, 300.0));
        assertEquals(2, trip.getStages().size());

        trip.restore(snapshot);

        assertEquals(1, trip.getStages().size());
        assertEquals("Tokyo", trip.getStages().get(0).getName());
    }

    @Test
    void observer_isCalledOnNotifyObservers() {
        final Trip trip = validTrip();
        final boolean[] called = {false};
        trip.addObserver(t -> called[0] = true);
        trip.notifyObservers();
        assertTrue(called[0]);
    }

    @Test
    void observer_receivesTripReference() {
        final Trip trip = validTrip();
        final Trip[] received = {null};
        trip.addObserver(t -> received[0] = t);
        trip.notifyObservers();
        assertSame(trip, received[0]);
    }
}
