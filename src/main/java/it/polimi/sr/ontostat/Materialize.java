package it.polimi.sr.ontostat;


import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.InfModelImpl;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;

/**
 * Created by Riccardo on 07/04/16.
 */
public class Materialize extends Program {


    private Entailment ent = Entailment.NONE;
    private boolean persist = false;
    private String tbox_file;
    private String abox_file;
    private String db;


    public Materialize(Entailment ent, boolean persist, String tbox_file, String abox_file, String db) {
        this.ent = ent;
        this.persist = persist;
        this.tbox_file = tbox_file;
        this.abox_file = abox_file;
        this.db = db;
    }

    public void run() {
        System.out.println("Materialize");


        String output_filename = abox_file.split("\\.")[0] + "-materialized-"
                + ent.toString();


        Dataset dataset = TDBFactory.createDataset(db);
        Model tbox = dataset.getDefaultModel();
        TDBLoader.loadModel(tbox, tbox_file);

        Model abox = dataset.getNamedModel("http://example.org/abox");
        TDBLoader.loadModel(abox, abox_file);

        Model m = materialize(tbox, abox, ent);


        if (persist) {
            System.out.println("Persisting");
            QueryUtils.saveResult(output_filename + ".nt", m);
        }

        abox.removeAll();
        tbox.removeAll();
        abox.commit();
        abox.close();
        tbox.commit();
        tbox.close();
        dataset.close();
    }

    public static Model materialize(Model tbox, Model abox, Entailment entailment) {

        Reasoner reasoner = null;

        switch (entailment) {
            case NONE:
                //TODO
                break;
            case RDFS:
                reasoner = ReasonerRegistry.getRDFSReasoner();
                break;
            case RHODF:
                reasoner = ReasonerRegistry.getRDFSSimpleReasoner();
                break;
            case OWL:
                reasoner = ReasonerRegistry.getOWLReasoner();
        }


        InfGraph graph = reasoner.bindSchema(tbox.getGraph()).bind(abox.getGraph());
        InfModel infmodel = new InfModelImpl(graph);
        return infmodel;
    }
}
