package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.Currency;
import java.util.Objects;
import java.util.logging.Logger;

// questa classe è il contesto del pattern Strategy: riceve una ConversionStrategy e la usa per fare le conversioni
// non sa come funziona la conversione internamente, conosce solo l'interfaccia (principio di inversione delle dipendenze)
public final class CurrencyConverter {

    private static final Logger LOGGER = Logger.getLogger(CurrencyConverter.class.getName());

    private final ConversionStrategy strategy;

    public CurrencyConverter() {
        this(new FixedRateStrategy());
    }

    public CurrencyConverter(final ConversionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public double convertToEuro(final double importo, final Currency valutaOrigine) {
        if (importo < 0) {
            throw new IllegalArgumentException("Amount must not be negative.");
        }
        Objects.requireNonNull(valutaOrigine, "valutaOrigine");
        final double importoInEuro = this.strategy.convert(importo, valutaOrigine);
        LOGGER.fine(() -> "Converted " + importo + " " + valutaOrigine + " -> " + importoInEuro + " EUR");
        return importoInEuro;
    }
}
