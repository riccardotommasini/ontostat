package it.polimi.sr.ontostat;


import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.impl.InfModelImpl;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.FileManager;

/**
 * Created by Riccardo on 07/04/16.
 */
public class Materialize implements Program {


    private Entailment ent = Entailment.NONE;
    private boolean persist = false;
    private String tbox_file;
    private String abox_file;

    public Materialize(Entailment ent, boolean persist, String tbox_file, String abox_file) {
        this.ent = ent;
        this.persist = persist;
        this.tbox_file = tbox_file;
        this.abox_file = abox_file;
    }

    public void run() {
        System.out.println("Materialize");

        Reasoner reasoner = null;

        String output_filename = abox_file.split("\\.")[0] + "-materialized-"
                + ent.toString();

        Model abox = FileManager.get().loadModel(abox_file, null, "N-TRIPLE");
        Model m = abox;
        OntModelSpec onto_lang = OntModelSpec.OWL_MEM;
        OntModel tbox = ModelFactory.createOntologyModel(onto_lang);
        tbox.read(FileManager.get().open(tbox_file), "", "RDF/XML");

        switch (ent) {
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
        m = infmodel;


        if (persist)

        {
            System.out.println("Persisting");
            QueryUtils.saveResult(output_filename + ".nt", m);
        }
    }
}
