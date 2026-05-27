package it.unipd.oop.travelbudget.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.unipd.oop.travelbudget.model.Trip;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

// questa classe si occupa di salvare e caricare i viaggi su file JSON
// ho isolato tutta la logica di persistenza qui dentro così se un giorno volessi cambiare formato
// (tipo database o XML) devo toccare solo questa classe e il resto del progetto resta uguale
public final class JsonRepository {

    private static final Logger LOGGER = Logger.getLogger(JsonRepository.class.getName());

    // configuro Jackson per tollerare campi sconosciuti nel JSON (tipo se una versione vecchia del file
    // ha campi in più non crasha) e registro il modulo JavaTimeModule per gestire correttamente le date LocalDate
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public void save(final Trip viaggio, final Path percorso) throws IOException {
        Objects.requireNonNull(viaggio, "viaggio");
        Objects.requireNonNull(percorso, "percorso");
        LOGGER.fine("Saving trip '" + viaggio.getName() + "' to " + percorso);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(percorso.toFile(), viaggio);
        LOGGER.fine("Trip saved successfully.");
    }

    public Trip load(final Path percorso) throws IOException {
        Objects.requireNonNull(percorso, "percorso");
        LOGGER.fine("Loading trip from " + percorso);
        final Trip viaggio = MAPPER.readValue(percorso.toFile(), Trip.class);
        LOGGER.fine("Trip '" + viaggio.getName() + "' loaded successfully.");
        return viaggio;
    }

    public String serialize(final Trip viaggio) throws IOException {
        return MAPPER.writeValueAsString(viaggio);
    }

    public Trip deserialize(final String jsonString) throws IOException {
        return MAPPER.readValue(jsonString, Trip.class);
    }
}
