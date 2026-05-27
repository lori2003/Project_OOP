package it.unipd.oop.travelbudget.model;

// interfaccia Observer: separa chi produce il cambiamento (Trip) da chi reagisce (es. la CLI).
// Trip non sa quanti observer ha né cosa fanno — sa solo che deve notificarli.
// in questo progetto lo uso per avvisare l'utente quando il budget sta per finire
public interface BudgetObserver {
    void update(Trip viaggio);
}
