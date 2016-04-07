package it.polimi.sr.ontostat;

import com.github.rvesse.airline.annotations.Cli;

@Cli(name = "basic",
        description = "Provides a basic example CLI",
        defaultCommand = MaterializerLauncher.class,
        commands = { MaterializerLauncher.class, ProfilerLauncher.class })


public class BasicCli {
    public static void main(String[] args) {
        com.github.rvesse.airline.Cli<Runnable> cli = new com.github.rvesse.airline.Cli<Runnable>(BasicCli.class);
        Runnable cmd = cli.parse(args);
        System.out.print(cmd.getClass().toString());
        cmd.run();
    }
}