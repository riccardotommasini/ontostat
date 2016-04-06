package it.polimi.sr.ontostat;

/**
 * Created by Riccardo on 25/02/16.
 */
public enum Entailment {


    NONE(""),
    RHODF("rhodf"),
    RDFS("rfds"),
    OWL("owl");

    private String name;


    Entailment(String s) {
        this.name = s;
    }
}
