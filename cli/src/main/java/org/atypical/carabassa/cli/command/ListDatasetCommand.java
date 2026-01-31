package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.cli.util.DateFormatter;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "list", description = "list datasets.")
public class ListDatasetCommand implements Callable<Integer> {

    private static final String OUTPUT_FORMAT = "%10s\t%20s\t%40s\t%20s\t%20s\n";

    private final CommandLogger cmdLogger = new CommandLogger();

    @Autowired
    private DatasetApiService datasetApiService;

    @Override
    public Integer call() {
        try {
            List<DatasetEntityRepresentation> datasets = datasetApiService.findAll();
            if (!datasets.isEmpty()) {
                System.out.format(OUTPUT_FORMAT, "id", "name", "description", "creation", "modification");
                for (DatasetEntityRepresentation datasetRepresentation : datasets) {
                    System.out.format(OUTPUT_FORMAT, datasetRepresentation.getId(), datasetRepresentation.getName(),
                            datasetRepresentation.getDescription(),
                            DateFormatter.toLocalDateFormatted(
                                    datasetRepresentation.getCreationAsZoned(ZoneId.systemDefault().getId())),
                            datasetRepresentation.getModification() == null ? ""
                                    : DateFormatter.toLocalDateFormatted(datasetRepresentation
                                            .getModificationAsZoned(ZoneId.systemDefault().getId())));
                }
                cmdLogger.info("done.");
            } else {
                cmdLogger.info("No datasets found.");
            }
        } catch (ApiException e) {
            cmdLogger.error("API error", e);
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }

}
