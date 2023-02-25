package org.atypical.carabassa.cli.command;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.cli.util.DateFormatter;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Component
@Command(name = "items", description = "list items.")
public class ListItemsCommand implements Callable<Integer> {

    private static final String OUTPUT_FORMAT = "%8s\t%5s\t%50s\t%6s\t%19s\t%19s\t%19s\t%32s\n";

    private static final CommandLogger cmdLogger = new CommandLogger();

    @Autowired
    private DatasetApiService datasetApiService;

    @Option(names = {"-d", "--dataset"}, description = "dataset name.", required = true)
    private String dataset;

    @Option(names = {"-s", "--search"}, description = "search conditions.")
    private String searchString;

    @Override
    public Integer call() {
        try {
            Long datasetId = datasetApiService.findByName(dataset);
            if (datasetId != null) {
                List<ItemRepresentation> items;
                if (searchString == null) {
                    items = datasetApiService.findItems(datasetId);
                } else {
                    items = datasetApiService.findItems(datasetId, searchString);
                }
                if (!items.isEmpty()) {
                    System.out.format(OUTPUT_FORMAT, "id", "type", "filename", "format", "creation", "modification",
                            "archivetime", "hash");
                    for (ItemRepresentation itemRepresentation : items) {
                        System.out.format(OUTPUT_FORMAT, itemRepresentation.getId(), itemRepresentation.getType(),
                                itemRepresentation.getFilename(), itemRepresentation.getFormat(),
                                DateFormatter.toLocalDateFormatted(
                                        itemRepresentation.getCreationAsZoned(ZoneId.systemDefault().getId())),
                                itemRepresentation.getModification() == null ? ""
                                        : DateFormatter.toLocalDateFormatted(itemRepresentation
                                        .getModificationAsZoned(ZoneId.systemDefault().getId())),
                                itemRepresentation.getArchiveTime() == null ? ""
                                        : DateFormatter.toLocalDateFormatted(itemRepresentation
                                        .getArchiveTimeAsZoned(ZoneId.systemDefault().getId())),
                                itemRepresentation.getHash() == null ? "" : itemRepresentation.getHash());
                    }
                    cmdLogger.info(String.format("%d items.", items.size()));
                } else {
                    cmdLogger.info("No items found.");
                }
            } else {
                cmdLogger.info("No dataset found.");
            }
        } catch (ApiException e) {
            cmdLogger.error("API error", e);
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }
}
