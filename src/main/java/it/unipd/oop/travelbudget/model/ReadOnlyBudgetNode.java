package it.unipd.oop.travelbudget.model;

import java.util.List;
import java.util.Objects;

// questo è un Decorator: prende un nodo qualsiasi e lo avvolge in modo che si possa solo
// leggere e non modificare, se qualcuno prova a chiamare addChild su questo wrapper lancio
// un'eccezione così il nodo originale resta protetto da modifiche non volute.
public final class ReadOnlyBudgetNode implements BudgetNode {

    private final BudgetNode nodoSottostante;

    public ReadOnlyBudgetNode(final BudgetNode nodoSottostante) {
        this.nodoSottostante = Objects.requireNonNull(nodoSottostante, "nodoSottostante non puo essere nullo");
    }

    @Override
    public double getTotal() {
        return this.nodoSottostante.getTotal();
    }

    @Override
    public String getName() {
        return this.nodoSottostante.getName();
    }

    @Override
    public void addChild(final BudgetNode nodo) {
        throw new UnsupportedOperationException("Questo nodo è in sola lettura.");
    }

    @Override
    public List<BudgetNode> getChildren() {
        return this.nodoSottostante.getChildren();
    }
}
