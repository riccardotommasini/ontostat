package it.polimi.sr.ontostat;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFWriter;
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

    public static void materialize(String abox_file,String tbox_file, String output_file) {
        try {
            File abox = new File(abox_file);
            File tbox = new File(tbox_file);

            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(abox);
            manager.addAxioms(ontology, manager.loadOntologyFromOntologyDocument(tbox).getAxioms());

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


    public static Model materialize(String tbox_file, String abox_file, String db, Entailment ent, String output_filename, String format) {
        try {
            org.apache.jena.reasoner.Reasoner reasoner = null;

            OntModelSpec spec = OntModelSpec.RDFS_MEM_RDFS_INF;
            switch (ent) {
                case NONE:
                    spec = OntModelSpec.RDFS_MEM;
                    break;
                case RHODF:
                    break;
                case RDFS:
                    spec = OntModelSpec.RDFS_MEM_RDFS_INF;
                    reasoner = ReasonerRegistry.getRDFSReasoner();
                    break;
                case OWL:
                    spec = OntModelSpec.OWL_DL_MEM_RULE_INF;
                    reasoner = ReasonerRegistry.getOWLMiniReasoner();
                    break;
            }

            Dataset dataset = TDBFactory.createDataset(db);
            Model def_tbox = dataset.getDefaultModel();
            TDBLoader.loadModel(def_tbox, tbox_file, true);
            Model tbox = ModelFactory.createOntologyModel(spec, def_tbox);

            Model abox = dataset.getNamedModel("http://example.org/abox");
            TDBLoader.loadModel(abox, abox_file, true);

            InfModel m = ModelFactory.createInfModel(reasoner, tbox, abox);
            save(output_filename + "", m, format);

            dataset.end();
            return m;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void save(String fileName, Model onto, String format) throws IOException {
        System.out.println("Persisting [" + fileName + "] [" + format + "]");
        FileWriter out = new FileWriter(fileName);
        onto.write(out, format);
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
