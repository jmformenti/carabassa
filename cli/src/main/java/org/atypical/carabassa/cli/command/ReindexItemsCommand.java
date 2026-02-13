package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "reindex", description = "reindex items.")
public class ReindexItemsCommand implements Callable<Integer> {

    private static final CommandLogger cmdLogger = new CommandLogger();

    @Autowired
    private DatasetApiService datasetApiService;

    @Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
    private String dataset;

    @Option(names = { "-s", "--search" }, description = "search conditions.")
    private String searchString;

    private int count;
    private int error;
    private int reindex;
    private int total;

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
                    total = items.size();
                    for (ItemRepresentation itemRepresentation : items) {
                        reindex(datasetId, itemRepresentation);
                    }
                    cmdLogger.info(String.format("Reindexed %d of %d items (%d error)", reindex, total, error));
                    cmdLogger.info("done.");
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

    private void reindex(Long datasetId, ItemRepresentation itemRepresentation) {
        try {
            cmdLogger
                    .info(String.format("Reindex item %s ( %d / %d ) ...", itemRepresentation.getId(), ++count, total));
            datasetApiService.reindex(datasetId, itemRepresentation.getId());
            reindex++;
        } catch (ApiException e) {
            cmdLogger.error(String.format("Error reindex item %s", itemRepresentation.getId()), e);
            error++;
        }
    }
}
