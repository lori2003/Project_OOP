package it.unipd.oop.travelbudget.model;

// le categorie di spesa disponibili nell'app, anche qui uso un enum per avere la sicurezza
// dei tipi a compile-time e non rischiare di scrivere stringhe sbagliate
public enum Category {
    FOOD,
    TRANSPORT,
    LODGING,
    ACTIVITY,
    SHOPPING,
    OTHER
}
