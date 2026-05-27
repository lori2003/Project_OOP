package it.unipd.oop.travelbudget.model;

// questa classe fa parte del pattern Memento e serve a conservare un'istantanea dello stato
// del viaggio. è completamente immutabile (non ha setter) così una volta salvato lo stato
// nessuno può modificarlo per sbaglio e l'undo funziona sempre correttamente
public final class TripMemento {
    private final String istantaneaJSON;

    public TripMemento(final String istantaneaJSON) {
        this.istantaneaJSON = istantaneaJSON;
    }

    public String getIstantaneaJSON() {
        return this.istantaneaJSON;
    }
}
