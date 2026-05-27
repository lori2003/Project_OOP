package it.unipd.oop.travelbudget.model;

// le valute supportate dall'app, uso un enum così il compilatore mi impedisce di passare
// stringhe sbagliate e ho la garanzia che il tipo sia sempre valido
public enum Currency {
    EUR,
    USD,
    JPY,
    GBP
}
