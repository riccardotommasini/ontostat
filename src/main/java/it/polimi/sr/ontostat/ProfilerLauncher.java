package it.polimi.sr.ontostat;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.AllowedRawValues;
import com.github.rvesse.airline.annotations.restrictions.MutuallyExclusiveWith;
import com.github.rvesse.airline.annotations.restrictions.Required;

import javax.inject.Inject;

/**
 * Created by Riccardo on 25/02/16.
 */

@Command(name = "profile", description = "Start some Ontology Profiling utils")

public class ProfilerLauncher implements Runnable {

    @Option(name = {"-m", "--materialize"}, description = "Materialize the data under a certain entailment regime")
    @MutuallyExclusiveWith(tag = "mat")

    private boolean materialize = false;

    @Option(name = {"-e", "--ent"}, title = "Entailment", arity = 1, description = "The entailment regime to perform reasoning")
    @AllowedRawValues(allowedValues = {"RDFS", "RHODFL", "OWL"})
    private Entailment ent = Entailment.NONE;

    @Option(name = {"-t", "--tbox"}, description = "Given TBox")
    @Required
    private String tbox_file;

    @Option(name = {"-a", "--abox"}, description = "Given ABox")
    @Required
    private String abox_file;

    @Option(name = {"-as", "--aboxs"}, description = "Given Materialized ABox")
    @MutuallyExclusiveWith(tag = "mat")

    private String abox_star_file;


    @Option(name = {"--db",}, description = "Database folder")
    private String db = "./db/";


    @Inject
    private HelpOption<ProfilerLauncher> help;

    public ProfilerLauncher() {
    }

    public static void main(String[] args) {

        SingleCommand<ProfilerLauncher> parser = SingleCommand.singleCommand(ProfilerLauncher.class);
        try {
            ProfilerLauncher launcher = parser.parse(args);

            // Show help if requested
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
        Program p = new Profiler(ent, materialize, abox_star_file, tbox_file, abox_file, db);
        p.run();

    }
}
