package it.polimi.sr.ontostat;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.AllowedRawValues;

import javax.inject.Inject;

/**
 * Created by Riccardo on 25/02/16.
 */

@Command(name = "main", description = "Start some Ontology Profiling utils")

public class Launcher {

    @Option(name = {"-m", "--materialize"}, description = "Materialize the data under a certain entailment regime")

    private boolean materialize = false;

    @Option(name = {
            "--ent"}, title = "Entailment", arity = 1, description = "The entailment regime to perform reasoning")
    @AllowedRawValues(allowedValues = {"RDFS", "RHODFL", "OWL"})
    private Entailment ent = Entailment.NONE;

    @Option(name = {"-p", "--persist"}, description = "Persist the materialized data")

    private boolean persist = false;

    @Option(name = {"-q", "--profile"}, description = "Persist the materialized data")

    private boolean profile = false;

    @Option(name = {"-o", "--onto"}, description = "Given TBox")

    private String tbox_file;

    @Option(name = {"-a", "--abox"}, description = "Given ABox")

    private String abox_file;

    @Option(name = {"-as", "--aboxstar"}, description = "Given materialized ABox")

    private String abox_star_file;


    @Inject
    private HelpOption<Launcher> help;

    public static void main(String[] args) {

        SingleCommand<Launcher> parser = SingleCommand.singleCommand(Launcher.class);
        try {
            Launcher launcher = parser.parse(args);

            // Show help if requested
            if (launcher.help.showHelpIfRequested()) {
                System.exit(1);
            }

            Program p = new Program();
            p.run(launcher.materialize, launcher.persist, launcher.profile, launcher.tbox_file, launcher.abox_file, launcher.ent);


        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(3);
        }

        // If we got here everything worked OK
        System.exit(0);

    }


}
