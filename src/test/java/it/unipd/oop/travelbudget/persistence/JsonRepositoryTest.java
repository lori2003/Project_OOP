package it.unipd.oop.travelbudget.persistence;

import it.unipd.oop.travelbudget.model.Category;
import it.unipd.oop.travelbudget.model.Currency;
import it.unipd.oop.travelbudget.model.Day;
import it.unipd.oop.travelbudget.model.Expense;
import it.unipd.oop.travelbudget.model.Stage;
import it.unipd.oop.travelbudget.model.Trip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class JsonRepositoryTest {

    private static final LocalDate START = LocalDate.of(2026, 4, 1);
    private static final LocalDate END = LocalDate.of(2026, 4, 30);

    private final JsonRepository repository = new JsonRepository();

    private Trip buildSampleTrip() {
        final Trip trip = new Trip("Giappone 2026", START, END, Currency.JPY, 2000.0);
        final Stage stage = new Stage("Tokyo", START, LocalDate.of(2026, 4, 10),
                Currency.JPY, 500.0);
        final Day day = new Day(LocalDate.of(2026, 4, 5));
        day.addChild(new Expense(30.0, Currency.EUR, Category.FOOD,
                LocalDate.of(2026, 4, 5), "Shinjuku", "Ramen"));
        day.addChild(new Expense(20.0, Currency.EUR, Category.TRANSPORT,
                LocalDate.of(2026, 4, 5), "Tokyo", "Metro"));
        stage.addChild(day);
        trip.addChild(stage);
        return trip;
    }

    @Test
    void save_and_load_roundTrip_preservesTreeStructure(@TempDir final Path tmp) throws IOException {
        final Trip original = buildSampleTrip();
        final Path file = tmp.resolve("trip.json");

        repository.save(original, file);
        assertTrue(Files.exists(file), "Il file deve essere stato creato.");
        assertTrue(Files.size(file) > 0, "Il file non deve essere vuoto.");

        final Trip reloaded = repository.load(file);

        assertEquals(original.getName(), reloaded.getName());
        assertEquals(original.getStartDate(), reloaded.getStartDate());
        assertEquals(original.getEndDate(), reloaded.getEndDate());
        assertEquals(original.getCurrency(), reloaded.getCurrency());
        assertEquals(original.getBudget(), reloaded.getBudget());
        assertEquals(original.getStages().size(), reloaded.getStages().size());

        final Stage originalStage = original.getStages().get(0);
        final Stage reloadedStage = reloaded.getStages().get(0);
        assertEquals(originalStage.getName(), reloadedStage.getName());
        assertEquals(originalStage.getDays().size(), reloadedStage.getDays().size());

        final Day originalDay = originalStage.getDays().get(0);
        final Day reloadedDay = reloadedStage.getDays().get(0);
        assertEquals(originalDay.getDate(), reloadedDay.getDate());
        assertEquals(originalDay.getExpenses().size(), reloadedDay.getExpenses().size());

        assertEquals(original.getTotal(), reloaded.getTotal(),
                "Il totale ricorsivo deve essere preservato dal round-trip.");
    }

    @Test
    void serialize_and_deserialize_produceEquivalentTrip() throws IOException {
        final Trip original = buildSampleTrip();
        final String json = repository.serialize(original);

        assertNotNull(json);
        assertFalse(json.isBlank());
        assertTrue(json.contains("Giappone 2026"));
        assertTrue(json.contains("2026-04-01"),
                "Le date devono essere serializzate in ISO-8601 (non timestamp).");

        final Trip reloaded = repository.deserialize(json);
        assertEquals(original.getName(), reloaded.getName());
        assertEquals(original.getTotal(), reloaded.getTotal());
    }

    @Test
    void load_throwsIOException_onCorruptedFile(@TempDir final Path tmp) throws IOException {
        final Path corrupted = tmp.resolve("corrupted.json");
        Files.writeString(corrupted, "{ this is not valid json }");

        assertThrows(IOException.class, () -> repository.load(corrupted));
    }

    @Test
    void load_throwsIOException_onMissingFile(@TempDir final Path tmp) {
        final Path missing = tmp.resolve("does-not-exist.json");
        assertThrows(IOException.class, () -> repository.load(missing));
    }

    @Test
    void save_rejectsNullArguments(@TempDir final Path tmp) {
        final Trip trip = buildSampleTrip();
        final Path file = tmp.resolve("trip.json");

        assertThrows(NullPointerException.class, () -> repository.save(null, file));
        assertThrows(NullPointerException.class, () -> repository.save(trip, null));
    }

    @Test
    void deserialize_rejectsInvalidJson() {
        assertThrows(IOException.class, () -> repository.deserialize("{ invalid }"));
    }

    @Test
    void serializedJson_doesNotIncludeDerivedFields() throws IOException {
        // Regression: getTotal() e getName() (su Day/Expense) non devono
        // finire nel JSON. Se ci finiscono, il load fallisce con
        // UnrecognizedPropertyException sul prossimo round-trip.
        final String json = repository.serialize(buildSampleTrip());
        assertFalse(json.contains("\"total\""),
                "Il campo derivato 'total' non deve essere serializzato.");
        // Trip e Stage hanno un campo reale 'name', che deve restare.
        // Day e Expense invece NON devono esporre 'name' (derivato da date/description).
        // Verifichiamo che dentro il blocco "expenses": [...] non compaia "name".
        final int expensesIdx = json.indexOf("\"expenses\"");
        assertTrue(expensesIdx >= 0);
        final String expensesBlock = json.substring(expensesIdx);
        assertFalse(expensesBlock.contains("\"name\""),
                "Le Expense non devono serializzare il campo derivato 'name'.");
    }

    @Test
    void load_toleratesUnknownFields(@TempDir final Path tmp) throws IOException {
        // Robustezza: file salvato da una versione precedente che includeva
        // 'total' e 'name' come campi derivati deve rimanere leggibile.
        final String legacy = "{\"name\":\"Old\",\"startDate\":\"2026-04-01\","
                + "\"endDate\":\"2026-04-30\",\"currency\":\"EUR\",\"budget\":100.0,"
                + "\"total\":0.0,\"stages\":[]}";
        final Path file = tmp.resolve("legacy.json");
        Files.writeString(file, legacy);
        final Trip loaded = repository.load(file);
        assertEquals("Old", loaded.getName());
        assertEquals(0, loaded.getStages().size());
    }

    @Test
    void roundTrip_preservesEmptyTrip(@TempDir final Path tmp) throws IOException {
        final Trip empty = new Trip("Viaggio vuoto", START, END, Currency.EUR, 100.0);
        final Path file = tmp.resolve("empty.json");

        repository.save(empty, file);
        final Trip reloaded = repository.load(file);

        assertEquals("Viaggio vuoto", reloaded.getName());
        assertEquals(0, reloaded.getStages().size());
        assertEquals(0.0, reloaded.getTotal());
    }
}
