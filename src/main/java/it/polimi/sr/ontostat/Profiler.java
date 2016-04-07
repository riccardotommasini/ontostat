package it.polimi.sr.ontostat;

import com.opencsv.CSVWriter;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.InfModelImpl;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;

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

            System.out.println(abox_file + " "+tbox_file);


            Dataset dataset = TDBFactory.createDataset("./database/");
            Model tbox = dataset.getDefaultModel();
            TDBLoader.loadModel(tbox, tbox_file);

            Model abox = dataset.getNamedModel("http://example.org/abox");
            TDBLoader.loadModel(abox, abox_file);

            InfModel abox_star = null;


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
                abox_star = new InfModelImpl(graph);
                abox = abox_star;

            }


            System.out.println("Class Profiling");
            if (persist) {
                List<String> classes = QueryUtils.listClasses(tbox);
                CSVWriter class_occurrence_writer = new CSVWriter(new FileWriter(abox_file.split("\\.")[0] + "_class_occurrence.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
                class_occurrence_writer.writeNext(class_occurrence_header);
                for (String c : classes) {
                    class_occurrence_writer.writeNext(new String[]{c.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", ""),
                            QueryUtils.countClassOccurence(c, abox) + "", "" + (abox_star != null ? QueryUtils.countClassOccurence(c, abox_star) : "")});

                }
                class_occurrence_writer.close();

                System.out.println("-----");

                System.out.println("Property Profiling");
                List<String> obj_properties = QueryUtils.listObjectProperties(tbox);

                CSVWriter prop_occurrence_writer = new CSVWriter(new FileWriter(abox_file.split("\\.")[0] + "_prop_occurrence.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
                prop_occurrence_writer.writeNext(prop_occurrence_header);

                for (String p : obj_properties) {
                    prop_occurrence_writer.writeNext(new String[]{p.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", ""),
                            QueryUtils.countPropertyOccurence(p, abox) + "", "" + (abox_star != null ? QueryUtils.countPropertyOccurence(p, abox_star) : "")});

                }

                List<String> datatype_properties = QueryUtils.listDatatypeProperties(tbox);

                for (String p : datatype_properties) {
                    prop_occurrence_writer.writeNext(new String[]{p.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", ""),
                            QueryUtils.countPropertyOccurence(p, abox) + "", "" + (abox_star != null ? QueryUtils.countPropertyOccurence(p, abox_star) : "")});

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
                        strings = QueryUtils.propertyUsage(p, abox_star);
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
                        strings = QueryUtils.propertyUsage(p, abox_star);
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
