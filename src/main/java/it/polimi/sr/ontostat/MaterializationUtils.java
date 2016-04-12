package it.polimi.sr.ontostat;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.InfModelImpl;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Riccardo on 08/04/16.
 */
public class MaterializationUtils {

    private static final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private static final Configuration c = new Configuration();
    private static final Reasoner.ReasonerFactory factory = new Reasoner.ReasonerFactory();

    static {


        c.reasonerProgressMonitor = new ConsoleProgressMonitor();

    }

    public static void materialize(String abox_file, String output_file) {
        try {
            File abox = new File(abox_file);

            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(abox);
            //manager.addAxioms(ontology, manager.loadOntologyFromOntologyDocument(tbox).getAxioms());

            OWLReasoner reasoner = factory.createReasoner(ontology, c);
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.CLASS_ASSERTIONS, InferenceType.OBJECT_PROPERTY_HIERARCHY, InferenceType.DATA_PROPERTY_HIERARCHY, InferenceType.OBJECT_PROPERTY_ASSERTIONS);


            InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, hermitOWL());

            OWLOntology inferredAxiomsOntology = manager.createOntology();

            iog.fillOntology(manager, inferredAxiomsOntology);

            save(output_file, new RDFXMLOntologyFormat(), inferredAxiomsOntology);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }


    public static Model materialize(String tbox_file, String abox_file, String db, Entailment ent, String output_filename) {
        try {
            org.apache.jena.reasoner.Reasoner reasoner = null;

            switch (ent) {
                case RDFS:
                    reasoner = ReasonerRegistry.getRDFSReasoner();
                    break;
                case RHODF:
                    reasoner = ReasonerRegistry.getRDFSSimpleReasoner();
                    break;
            }

            Dataset dataset = TDBFactory.createDataset(db);
            Model tbox = dataset.getDefaultModel();
            TDBLoader.loadModel(tbox, tbox_file);

            Model abox = dataset.getNamedModel("http://example.org/abox");
            TDBLoader.loadModel(abox, abox_file);

            InfGraph graph = reasoner.bindSchema(tbox.getGraph()).bind(abox.getGraph());
            InfModel m = new InfModelImpl(graph);

            save(output_filename + ".nt", m);

            abox.removeAll();
            tbox.removeAll();
            abox.commit();
            abox.close();
            tbox.commit();
            tbox.close();
            dataset.close();
            return m;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void save(String fileName, Model onto) throws IOException {
        System.out.println("Persisting");
        FileWriter out = new FileWriter(fileName);
        onto.write(out, "N-TRIPLE");

    }

    private static void save(String output_file, OWLOntologyFormat format, OWLOntology inferredAxiomsOntology) throws IOException, OWLOntologyStorageException {
        System.out.println("Persisting");
        File inferredOntologyFile = new File(output_file);
        if (!inferredOntologyFile.exists())
            inferredOntologyFile.createNewFile();
        inferredOntologyFile = inferredOntologyFile.getAbsoluteFile();
        OutputStream outputStream = new FileOutputStream(inferredOntologyFile);
        manager.saveOntology(inferredAxiomsOntology, format, outputStream);
    }

    private static List<InferredAxiomGenerator<? extends OWLAxiom>> hermitOWL() {
        List<InferredAxiomGenerator<? extends OWLAxiom>> axiomGenerators = hermitRDFS();

        axiomGenerators = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        axiomGenerators.add(new InferredDataPropertyCharacteristicAxiomGenerator());
        axiomGenerators.add(new InferredEquivalentClassAxiomGenerator());
        axiomGenerators.add(new InferredEquivalentDataPropertiesAxiomGenerator());
        axiomGenerators.add(new InferredEquivalentObjectPropertyAxiomGenerator());
        axiomGenerators.add(new InferredInverseObjectPropertiesAxiomGenerator());
        axiomGenerators.add(new InferredObjectPropertyCharacteristicAxiomGenerator());
        axiomGenerators.add(new InferredPropertyAssertionGenerator());
        axiomGenerators.add(new InferredSubDataPropertyAxiomGenerator());
        axiomGenerators.add(new InferredDisjointClassesAxiomGenerator() {
            boolean precomputed = false;

            protected void addAxioms(OWLClass entity, OWLReasoner reasoner, OWLDataFactory dataFactory, Set<OWLDisjointClassesAxiom> result) {
                if (!precomputed) {
                    reasoner.precomputeInferences(InferenceType.DISJOINT_CLASSES);
                    precomputed = true;
                }
                for (OWLClass cls : reasoner.getDisjointClasses(entity).getFlattened()) {
                    result.add(dataFactory.getOWLDisjointClassesAxiom(entity, cls));
                }
            }
        });

        return axiomGenerators;
    }

    private static List<InferredAxiomGenerator<? extends OWLAxiom>> hermitRDFS() {
        List<InferredAxiomGenerator<? extends OWLAxiom>> axiomGenerators = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        axiomGenerators.add(new InferredSubClassAxiomGenerator());
        axiomGenerators.add(new InferredClassAssertionAxiomGenerator());
        axiomGenerators.add(new InferredSubObjectPropertyAxiomGenerator());

        return axiomGenerators;
    }

}
