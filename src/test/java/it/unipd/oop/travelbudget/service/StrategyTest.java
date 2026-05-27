package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.Currency;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StrategyTest {

    @Test
    void fixedRateStrategy_convertEur() {
        final ConversionStrategy strategy = new FixedRateStrategy();
        assertEquals(100.0, strategy.convert(100.0, Currency.EUR), 0.001);
    }

    @Test
    void fixedRateStrategy_convertUsd() {
        final ConversionStrategy strategy = new FixedRateStrategy();
        assertEquals(92.0, strategy.convert(100.0, Currency.USD), 0.001);
    }

    @Test
    void currencyConverter_usesFixedRateByDefault() {
        final CurrencyConverter converter = new CurrencyConverter();
        assertEquals(100.0, converter.convertToEuro(100.0, Currency.EUR), 0.001);
    }

    @Test
    void currencyConverter_acceptsCustomStrategy() {
        // Strategia personalizzata: tutto vale 2x per test
        final ConversionStrategy doubler = (amount, from) -> amount * 2;
        final CurrencyConverter converter = new CurrencyConverter(doubler);
        assertEquals(200.0, converter.convertToEuro(100.0, Currency.USD), 0.001);
    }

    @Test
    void fixedRateStrategy_rejectsNullCurrency() {
        final ConversionStrategy strategy = new FixedRateStrategy();
        assertThrows(NullPointerException.class, () -> strategy.convert(10.0, null));
    }
}
