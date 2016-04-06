# Ontostat

Ontostat allows to profile a RDF dataset and a given ontology.

## Options

-m, --materialize : provide the materialization of the given datasate w.r.t. the provided ontology/vocabulary/tbox and a specified entailment regime;
-ent : the specified entailment regime under which execute the materialization
-o, --onto: the tbox/ontology/schema of the knowledge base
-a, --abox: the abox/dataset of the knowledge base
-p, --persist: required to persiste the materialized dataset (output file name include -materialized-<ENTAILMENT>)
-q, --profile: execute a series of queries agains the dataset to provide:
	for each class in the tbox, retrieve the number of instances present in the dataset (before and after the materialization)
	for each property (object property or datatype property) provide the number of time that property is involved by into a relations in the dataset (before and after the materialization)
	for each property provide the class of the instance used a subject, the class of the instance used as an object and the number of occurrence for that upper level pattern (before and after the materialization) 


## Example of usage

java -jar target/ontostat-0-jar-with-dependencies.jar --persist -d data/lubm10.nt -o data/univ-bench.owl --ent OWL -m
