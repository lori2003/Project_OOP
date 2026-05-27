package it.unipd.oop.travelbudget.model;

import java.util.List;

// questa interfaccia è il pezzo base di tutto il progetto, la uso per trattare viaggio, tappe e spese
// allo stesso modo grazie al polimorfismo, cosi il resto del codice non deve sapere con quale classe
// concreta sta lavorando - nel pattern Composite questo sarebbe il Component
public interface BudgetNode {

    double getTotal();

    String getName();

    void addChild(BudgetNode node);

    List<BudgetNode> getChildren();
}
