package it.polimi.sr.ontostat;


import java.io.File;

/**
 * Created by Riccardo on 07/04/16.
 */
public class Materialize extends Program {


    private Entailment ent = Entailment.NONE;
    private String tbox_file;
    private String abox_file;
    private String db;


    public Materialize(Entailment ent, String tbox_file, String abox_file, String db) {
        this.ent = ent;
        this.tbox_file = tbox_file;
        this.abox_file = abox_file;
        this.db = db;

        File database = new File(db);
        if (!database.exists())
            database.mkdirs();
    }

    public void run() {
        System.out.println("Materialize");
        String output_filename = abox_file.split("\\.")[0] + "-materialized-"
                + ent.toString();


        if (Entailment.OWL.equals(ent)) {
            MaterializationUtils.materialize(abox_file, output_filename + ".owl");
        } else {
            MaterializationUtils.materialize(tbox_file, abox_file, db, ent, output_filename);
        }


    }

}
