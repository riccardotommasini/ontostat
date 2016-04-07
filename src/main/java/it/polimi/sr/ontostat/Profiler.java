package it.polimi.sr.ontostat;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.InfModelImpl;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.util.FileManager;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by Riccardo on 06/04/16.
 */
public class Profiler implements Program {

    private static final String[] property_usage_header = new String[]{"PROPERTY", "SUBJECT CLASS", "OBJECT CLASS", "OCCURRENCE"};

    private static final String[] prop_occurrence_header = new String[]{"PROPERTY", "OCCURRENCE", "OCCURRENCE AFTER MATERIALIZATION"};

    private static final String[] class_occurrence_header = new String[]{"CLASS", "OCCURRENCE", "OCCURRENCE AFTER MATERIALIZATION"};


    private Entailment ent;
    private boolean materialize;
    private boolean persist;
    private String tbox_file;
    private String abox_file;

    public Profiler(Entailment ent, boolean materialize, boolean persist, String tbox_file, String abox_file) {
        this.ent = ent;
        this.materialize = materialize;
        this.persist = persist;
        this.tbox_file = tbox_file;
        this.abox_file = abox_file;
    }


    public void run() {
        try {
            String output_filename = abox_file.split("\\.")[0] + "-materialized-"
                    + ent.toString();


            System.out.println("Running");

            Model abox = FileManager.get().loadModel(abox_file, null, "N-TRIPLE");

            Model m = abox;
            OntModelSpec onto_lang = OntModelSpec.OWL_MEM;
            OntModel tbox = ModelFactory.createOntologyModel(onto_lang);
            tbox.read(FileManager.get().open(tbox_file), "", "RDF/XML");

            if (materialize) {
                System.out.println("Materialize");

                Reasoner reasoner = null;


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

            }


            System.out.println("Class Profiling");
            if (persist) {
                List<String> classes = QueryUtils.listClasses(tbox);
                CSVWriter class_occurrence_writer = new CSVWriter(new FileWriter(abox_file.split("\\.")[0] + "_class_occurrence.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
                class_occurrence_writer.writeNext(class_occurrence_header);
                for (String c : classes) {
                    class_occurrence_writer.writeNext(new String[]{c.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", ""),
                            QueryUtils.countClassOccurence(c, abox) + "", "" + QueryUtils.countClassOccurence(c, m)});

                }
                class_occurrence_writer.close();

                System.out.println("-----");

                System.out.println("Property Profiling");
                List<String> obj_properties = QueryUtils.listObjectProperties(tbox);

                CSVWriter prop_occurrence_writer = new CSVWriter(new FileWriter(abox_file.split("\\.")[0] + "_prop_occurrence.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
                prop_occurrence_writer.writeNext(prop_occurrence_header);

                for (String p : obj_properties) {
                    prop_occurrence_writer.writeNext(new String[]{p.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", ""),
                            QueryUtils.countPropertyOccurence(p, abox) + "", "" + QueryUtils.countPropertyOccurence(p, m)});

                }

                List<String> datatype_properties = QueryUtils.listDatatypeProperties(tbox);

                for (String p : datatype_properties) {
                    prop_occurrence_writer.writeNext(new String[]{p.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", ""),
                            QueryUtils.countPropertyOccurence(p, abox) + "", "" + QueryUtils.countPropertyOccurence(p, m)});

                }

                prop_occurrence_writer.close();

                System.out.println("Property Usage Profiling");


                CSVWriter property_usage_writer = new CSVWriter(new FileWriter(abox_file.split("\\.")[0] + "_prop_usage.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
                property_usage_writer.writeNext(property_usage_header);

                CSVWriter property_usage_materialized_writer = new CSVWriter(new FileWriter(output_filename + "_prop_usage.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
                property_usage_materialized_writer.writeNext(property_usage_header);


                for (String p : obj_properties) {
                    System.out.println("Dataset: [" + p + "]");
                    List<String[]> strings = QueryUtils.propertyUsage(p, abox);
                    for (String[] s : strings) {
                        property_usage_writer.writeNext(s);
                    }
                    if (materialize) {
                        System.out.println("Materialized Dataset: [" + p + "]");
                        strings = QueryUtils.propertyUsage(p, m);
                        for (String[] s : strings) {
                            property_usage_materialized_writer.writeNext(s);
                        }
                    }
                }


                System.out.println("-----");


                for (String p : datatype_properties) {
                    System.out.println("Dataset: [" + p + "]");
                    List<String[]> strings = QueryUtils.propertyUsage(p, abox);
                    for (String[] s : strings) {
                        property_usage_writer.writeNext(s);
                    }
                    if (materialize) {
                        System.out.println("Materialized Dataset: [" + p + "]");
                        strings = QueryUtils.propertyUsage(p, m);
                        for (String[] s : strings) {
                            property_usage_materialized_writer.writeNext(s);
                        }
                    }
                }

                property_usage_writer.close();

                property_usage_materialized_writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
