package it.unipd.oop.travelbudget.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.LocalDate;
import java.util.List;

// una tappa del viaggio (es Roma, Firenze), estende AbstractNamedNode per riutilizzare
// campi e logica comune, nel validateChild controllo che si possano aggiungere solo giorni
// (Day) con date che rientrano nel periodo della tappa
public final class Stage extends AbstractNamedNode {

    @JsonCreator
    public Stage(
            @JsonProperty("name") final String name,
            @JsonProperty("startDate") final LocalDate startDate,
            @JsonProperty("endDate") final LocalDate endDate,
            @JsonProperty("currency") final Currency currency,
            @JsonProperty("budget") final double budget) {
        super(name, startDate, endDate, currency, budget);
    }

    @Override
    protected void validateChild(final BudgetNode nodo) {
        // hook del template method: Stage specializza la regola dicendo che accetta solo Day
        // e che la data del giorno deve stare dentro le date della tappa, cosi la gerarchia
        // resta sempre coerente senza dover controllare nulla fuori dal modello
        if (!(nodo instanceof Day giorno)) {
            throw new IllegalArgumentException("A stage can only contain days.");
        }
        if (giorno.getDate().isBefore(getStartDate())
                || giorno.getDate().isAfter(getEndDate())) {
            throw new IllegalArgumentException(
                    "Day date must fall within the stage period ("
                            + getStartDate() + " - " + getEndDate() + ").");
        }
    }

    @JsonGetter("days")
    public List<Day> getDays() {
        return getChildren().stream().map(c -> (Day) c).toList();
    }

    @JsonSetter("days")
    void setDaysFromJson(final List<Day> giorni) {
        if (giorni != null) {
            giorni.forEach(this::addChild);
        }
    }

    @Override
    public String toString() {
        return "Stage{name='" + getName() + "', " + getStartDate() + " → " + getEndDate()
                + ", budget=" + getBudget() + " " + getCurrency()
                + ", days=" + getChildren().size() + "}";
    }
}
