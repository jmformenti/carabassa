package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.cli.util.InteractiveCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Component
@Command(name = "delete", description = "delete dataset.")
public class DeleteDatasetCommand implements Callable<Integer> {

    private final CommandLogger cmdLogger = new CommandLogger();

    @Option(names = {"-d", "--dataset"}, description = "dataset name.", required = true)
    private String dataset;

    @Autowired
    private DatasetApiService datasetApiService;

    @Override
    public Integer call() {
        try {
            cmdLogger.info(String.format("Deleting dataset %s ...", dataset));

            Long datasetId = datasetApiService.findByName(dataset);
            if (InteractiveCommand.doConfirm(String.format(
                    "This action cannot be reversed. Are you sure you want to delete the dataset '%s'? [y|N] ",
                    dataset))) {
                datasetApiService.delete(datasetId);

                cmdLogger.info("deleted.");
            }
        } catch (ApiException e) {
            cmdLogger.error("API error", e);
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }
}
