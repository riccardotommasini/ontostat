package it.polimi.sr.ontostat;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Riccardo on 16/03/16.
 */
public class QueryUtils {


    public static final String prefixes = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
            "PREFIX : <http://swat.cse.lehigh.edu/onto/univ-bench.owl#> \n";


    public static final String class_listing_query_string = prefixes + "SELECT DISTINCT ?class " +
            "WHERE {  ?class a owl:Class .}\n";


    public static final String obj_prop_listing_query_string = prefixes + "SELECT DISTINCT ?prop \n" +
            "WHERE { ?prop a owl:ObjectProperty . }\n";

    public static final String datatype_prop_listing_query_string = prefixes + "SELECT DISTINCT ?prop \n" +
            "WHERE { ?prop a owl:DatatypeProperty . }\n";


    public static final String class_counter_query_string = prefixes + "SELECT (count(distinct *) as ?occurrence)\n" +
            "WHERE { ?s a ?c }";

    private String p;

    public static final String prop_counter_query_string = prefixes + "SELECT (count(distinct *) as ?occurrence) \n" +
            "WHERE { ?s ?p ?o }";


    public static final String prop_usage_counter = prefixes + "SELECT ?d ?r (count(distinct *) as ?occurrence) \n" +
            "WHERE {" +
            " ?s ?p ?o .\n" +
            " OPTIONAL {?s a ?d .}\n" +
            " OPTIONAL {?o a ?r .}\n" +
            "}\n" +
            "GROUP BY ?d ?r";


    public static void selectPrinter(String queryString, Model onto) {
        Query query = QueryFactory.create(queryString);
        ResultSet resultSet = QueryExecutionFactory.create(query, onto).execSelect();

        ResultSetFormatter.out(System.out, resultSet, query);
    }

    public static List<String> listClasses(Model onto) {
        Query query = QueryFactory.create(class_listing_query_string);
        ResultSet results = QueryExecutionFactory.create(query, onto).execSelect();
        List<String> classes = new ArrayList<String>();

        while (results.hasNext()) {
            QuerySolution binding = results.nextSolution();
            Resource subj = (Resource) binding.get("class");
            classes.add(subj.getURI());
        }

        return classes;
    }


    public static List<String> listObjectProperties(Model onto) {
        Query query = QueryFactory.create(obj_prop_listing_query_string);
        ResultSet results = QueryExecutionFactory.create(query, onto).execSelect();
        List<String> classes = new ArrayList<String>();

        while (results.hasNext()) {
            QuerySolution binding = results.nextSolution();
            Resource subj = (Resource) binding.get("prop");
            classes.add(subj.getURI());
        }

        return classes;
    }

    public static List<String> listDatatypeProperties(Model onto) {
        Query query = QueryFactory.create(datatype_prop_listing_query_string);
        ResultSet results = QueryExecutionFactory.create(query, onto).execSelect();
        List<String> classes = new ArrayList<String>();

        while (results.hasNext()) {
            QuerySolution binding = results.nextSolution();
            Resource subj = (Resource) binding.get("prop");
            classes.add(subj.getURI());
        }

        return classes;
    }


    public static int countClassOccurence(String class_string, Model onto) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(class_counter_query_string);
        pss.setParam("c", NodeFactory.createURI(class_string));


        Query query = pss.asQuery();
        ResultSet results = QueryExecutionFactory.create(query, onto).execSelect();
        Literal res = null;

        while (results.hasNext()) {
            QuerySolution binding = results.nextSolution();
            res = (Literal) binding.get("occurrence");
            //System.out.println("class " + class_string.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#","") + " " + subj.getInt());
        }

        return res != null ? res.getInt() : 0;
    }

    public static int countPropertyOccurence(String prop_string, Model onto) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(prop_counter_query_string);
        pss.setParam("p", NodeFactory.createURI(prop_string));


        Query query = pss.asQuery();

        ResultSet results = QueryExecutionFactory.create(query, onto).execSelect();
        Literal res = null;

        while (results.hasNext()) {
            QuerySolution binding = results.nextSolution();
            res = (Literal) binding.get("occurrence");
            //System.out.println("class " + class_string.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#","") + " " + subj.getInt());
        }

        return res != null ? res.getInt() : 0;
    }


    public static List<String[]> propertyUsage(String prop_string, Model onto) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(prop_usage_counter);
        pss.setParam("p", NodeFactory.createURI(prop_string));

        String p = prop_string.replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", "");

        Query query = pss.asQuery();

        ResultSet results = QueryExecutionFactory.create(query, onto).execSelect();

        List<String[]> res = new ArrayList<String[]>();

        while (results.hasNext()) {
            QuerySolution binding = results.nextSolution();

            RDFNode d_binding = binding.get("d");
            String d = d_binding != null ? d_binding.toString().replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", "") : "";
            d.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");

            RDFNode r_binding = binding.get("r");
            String r = r_binding != null ? r_binding.toString().replace("http://swat.cse.lehigh.edu/onto/univ-bench.owl#", "") : "";
            r.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");

            String occurrence = "" + binding.get("occurrence").asLiteral().getInt();

            res.add(new String[]{p, d, r, occurrence});
        }

        return res;
    }


    public static void saveResult(String fileName, Model onto) {
        try {
            FileWriter out = new FileWriter(fileName);
            onto.write(out, "N-TRIPLE");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
