package it.polimi.sr.ontostat;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.InfModelImpl;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.util.FileManager;

/**
 * Created by Riccardo on 07/04/16.
 */
public class Reasoner implements Program {


    private Entailment ent = Entailment.NONE;
    private boolean persist = false;
    private String tbox_file;
    private String abox_file;

    public Reasoner(Entailment ent, boolean persist, String tbox_file, String abox_file) {
        this.ent = ent;
        this.persist = persist;
        this.tbox_file = tbox_file;
        this.abox_file = abox_file;
    }

    public void run() {
        System.out.println("Materialize");

        com.hp.hpl.jena.reasoner.Reasoner reasoner = null;

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
