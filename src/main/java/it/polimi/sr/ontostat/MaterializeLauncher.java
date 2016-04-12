package it.polimi.sr.ontostat;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.AllowedRawValues;
import com.github.rvesse.airline.annotations.restrictions.Required;

import javax.inject.Inject;

/**
 * Created by Riccardo on 25/02/16.
 */

@Command(name = "materialize", description = "Utility for ontology materialization")

public class MaterializeLauncher implements Runnable {

    @Option(name = {"-e", "--ent"}, title = "Entailment", arity = 1, description = "The entailment regime to perform reasoning")
    @AllowedRawValues(allowedValues = {"RDFS", "RHODFL", "OWL"})
    private Entailment ent = Entailment.NONE;

    @Option(name = {"-p", "--persist"}, description = "Persist the materialized data")

    private boolean persist = false;

    @Option(name = {"-t", "--tbox"}, description = "Given TBox")
    private String tbox_file;

    @Option(name = {"-a", "--abox"}, description = "Given ABox")
    @Required

    private String abox_file;

    @Option(name = {"--db", }, description = "Database folder")

    private String db="./db/";


    @Inject
    private HelpOption<MaterializeLauncher> help;


    public static void main(String[] args) {

        SingleCommand<MaterializeLauncher> parser = SingleCommand.singleCommand(MaterializeLauncher.class);
        try {

            MaterializeLauncher launcher = parser.parse(args);

            if (launcher.help.showHelpIfRequested()) {
                System.exit(1);
            }

            launcher.run();


        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(3);
        }

        // If we got here everything worked OK
        System.exit(0);

    }


    public void run() {

        // Show help if requested
        if (help.showHelpIfRequested()) {
            System.exit(1);
        }

        Program p = new Materialize(ent, tbox_file, abox_file, db);
        p.run();
    }
}
