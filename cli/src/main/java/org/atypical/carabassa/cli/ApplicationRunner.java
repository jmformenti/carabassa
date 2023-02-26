package org.atypical.carabassa.cli;

import org.atypical.carabassa.cli.command.CarabassaCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@Component
public class ApplicationRunner implements CommandLineRunner, ExitCodeGenerator {

    private final CarabassaCommand carabassaCommand;

    private final IFactory factory; // auto-configured to inject PicocliSpringFactory

    private int exitCode;

    public ApplicationRunner(CarabassaCommand carabassaCommand, IFactory factory) {
        this.carabassaCommand = carabassaCommand;
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(carabassaCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
