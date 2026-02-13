package org.atypical.carabassa.cli.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.util.concurrent.Callable;

@Component
@Command(name = "carabassa", mixinStandardHelpOptions = true, versionProvider = CarabassaVersion.class, subcommands = {
        CreateDatasetCommand.class, ListDatasetCommand.class, UpdateDatasetCommand.class, DeleteDatasetCommand.class,
        UploadDatasetCommand.class, ListItemsCommand.class, ReindexItemsCommand.class, DeleteItemCommand.class})
public class CarabassaCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        new CommandLine(new CarabassaCommand()).usage(System.out);
        return ExitCode.OK;
    }
}
