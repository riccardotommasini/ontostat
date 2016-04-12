package it.polimi.sr.ontostat;

import com.opencsv.CSVWriter;
import org.apache.commons.collections4.ListUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by Riccardo on 06/04/16.
 */
public class Profiler extends Program {

    private static final String[] property_usage_header = new String[]{"PROPERTY", "SUBJECT CLASS", "OBJECT CLASS", "OCCURRENCE"};

    private static final String[] prop_occurrence_header = new String[]{"PROPERTY", "OCCURRENCE", "OCCURRENCE AFTER MATERIALIZATION"};

    private static final String[] class_occurrence_header = new String[]{"CLASS", "OCCURRENCE", "OCCURRENCE AFTER MATERIALIZATION"};


    private Entailment ent;
    private boolean materialize;
    private String tbox_file;
    private String abox_file;
    private String abox_star_file;
    private String db;

    public Profiler(Entailment ent, boolean materialize, String abox_star_file, String tbox_file, String abox_file, String db) {
        this.ent = ent;
        this.materialize = materialize;
        this.tbox_file = tbox_file;
        this.abox_file = abox_file;
        this.abox_star_file = abox_star_file;
        this.db = db;

        File database = new File(db);
        if (!database.exists())
            database.mkdirs();
    }


    public void run() {
        try {

            System.out.println("Running");

            System.out.println(abox_file + " " + tbox_file + " " + abox_star_file);


            Dataset dataset = TDBFactory.createDataset(db);
            Model tbox = dataset.getDefaultModel();
            TDBLoader.loadModel(tbox, tbox_file);

            Model abox = dataset.getNamedModel("http://example.org/abox");
            TDBLoader.loadModel(abox, abox_file);

            Model abox_star = null;

            if (materialize) {
                abox_star = MaterializationUtils.materialize(tbox_file, abox_file, "./", ent, "out.owl");
            } else {
                abox_star = dataset.getNamedModel("http://example.org/abox_star");
                TDBLoader.loadModel(abox_star, abox_star_file);
            }


            System.out.println("Class Profiling");
            List<String> classes = QueryUtils.listClasses(tbox);

            saveClassCount(abox_file.split("\\.")[0] + "_class_occurrence.csv", class_occurrence_header, classes, abox, abox_star);

            System.out.println("Object Property Profiling");
            List<String> union = ListUtils.union(QueryUtils.listObjectProperties(tbox), QueryUtils.listDatatypeProperties(tbox));

            savePropCount(abox_file.split("\\.")[0] + "_prop_occurrence.csv", prop_occurrence_header, union, abox, abox_star);

            System.out.println("Property Usage Profiling");

            saveUsage(abox_file.split("\\.")[0] + "_prop_usage.csv", property_usage_header, union, abox, abox_star);

            close(dataset, tbox, abox);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close(Dataset dataset, Model tbox, Model abox) {
        abox.removeAll();
        tbox.removeAll();
        abox.commit();
        abox.close();
        tbox.commit();
        tbox.close();
        dataset.close();
    }

    private void saveClassCount(String name, String[] header, List<String> data, Model model, Model model_star) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(name), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        writer.writeNext(header);
        for (String c : data) {
            writer.writeNext(new String[]{c,
                    QueryUtils.countClassOccurence(c, model) + "", "" + (model_star != null ? QueryUtils.countClassOccurence(c, model_star) : "")});

        }
        writer.close();
    }

    private void savePropCount(String name, String[] header, List<String> data, Model model, Model model_star) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(name), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        writer.writeNext(header);
        for (String c : data) {
            writer.writeNext(new String[]{c,
                    QueryUtils.countPropertyOccurence(c, model) + "", "" + (model_star != null ? QueryUtils.countPropertyOccurence(c, model_star) : "")});

        }
        writer.close();
    }


    private void saveUsage(String name, String[] header, List<String> data, Model model, Model model_star) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(name), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        writer.writeNext(header);


        CSVWriter writer_star = new CSVWriter(new FileWriter(name.replace(".csv", "_mat.csv")), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        writer_star.writeNext(header);

        for (String p : data) {
            System.out.println("Processing [" + p + "]");
            for (String[] s : QueryUtils.propertyUsage(p, model)) {
                writer.writeNext(s);
                writer.flush();
            }
            for (String[] s : QueryUtils.propertyUsage(p, model_star)) {
                writer_star.writeNext(s);


                writer_star.flush();
            }

        }
        writer.close();
        writer_star.close();
    }
}
