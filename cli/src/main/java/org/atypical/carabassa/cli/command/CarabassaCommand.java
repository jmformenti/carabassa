package org.atypical.carabassa.cli.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Component
@Command(name = "carabassa", mixinStandardHelpOptions = true, versionProvider = CarabassaVersion.class, subcommands = {
        CreateDatasetCommand.class, ListDatasetCommand.class, UpdateDatasetCommand.class, DeleteDatasetCommand.class,
        UploadDatasetCommand.class, ListItemsCommand.class, ReindexItemsCommand.class, DeleteItemCommand.class})
public class CarabassaCommand implements Callable<Integer> {

    @Option(names = {"--token"}, description = "JWT token for authentication (or set CARABASSA_TOKEN env var)")
    private String token;

    @Override
    public Integer call() {
        new CommandLine(new CarabassaCommand()).usage(System.out);
        return ExitCode.OK;
    }
}
