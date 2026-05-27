package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.Currency;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

// implementazione concreta della strategy che usa tassi di cambio fissi salvati in una EnumMap
// (è una mappa ottimizzata per chiavi di tipo enum, più veloce di una HashMap normale)
public final class FixedRateStrategy implements ConversionStrategy {

    private static final Map<Currency, Double> TASSI_CONVERSIONE = new EnumMap<>(Currency.class);

    static {
        TASSI_CONVERSIONE.put(Currency.EUR, 1.0);
        TASSI_CONVERSIONE.put(Currency.USD, 0.92);
        TASSI_CONVERSIONE.put(Currency.JPY, 0.0061);
        TASSI_CONVERSIONE.put(Currency.GBP, 1.17);
    }

    @Override
    public double convert(final double importo, final Currency valutaOrigine) {
        final Currency valutaSorgente = Objects.requireNonNull(valutaOrigine, "valutaOrigine");
        final Double tassoConversione = TASSI_CONVERSIONE.get(valutaSorgente);
        if (tassoConversione == null) {
            throw new IllegalArgumentException("Unsupported currency: " + valutaSorgente);
        }
        return importo * tassoConversione;
    }
}
