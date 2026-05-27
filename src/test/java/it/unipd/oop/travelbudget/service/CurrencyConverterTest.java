package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyConverterTest {

    private CurrencyConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CurrencyConverter();
    }

    @Test
    void convertEurToEur_returnsUnchanged() {
        assertEquals(100.0, converter.convertToEuro(100.0, Currency.EUR), 0.001);
    }

    @Test
    void convertUsdToEur() {
        assertEquals(92.0, converter.convertToEuro(100.0, Currency.USD), 0.001);
    }

    @Test
    void convertGbpToEur() {
        assertEquals(117.0, converter.convertToEuro(100.0, Currency.GBP), 0.001);
    }

    @Test
    void convertJpyToEur() {
        assertEquals(0.61, converter.convertToEuro(100.0, Currency.JPY), 0.001);
    }

    @Test
    void convertZero_returnsZero() {
        assertEquals(0.0, converter.convertToEuro(0.0, Currency.USD), 0.001);
    }

    @Test
    void convertNegativeAmount_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEuro(-10.0, Currency.EUR));
    }

    @Test
    void convertNullCurrency_throws() {
        assertThrows(NullPointerException.class, () ->
                converter.convertToEuro(10.0, null));
    }
}
