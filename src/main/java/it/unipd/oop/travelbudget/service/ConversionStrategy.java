package it.unipd.oop.travelbudget.service;

import it.unipd.oop.travelbudget.model.Currency;

// questa interfaccia definisce come deve funzionare un algoritmo di cambio valuta
// grazie al pattern Strategy posso cambiare il modo in cui converto le valute senza toccare il resto del codice
public interface ConversionStrategy {
    double convert(double importo, Currency valutaOrigine);
}
