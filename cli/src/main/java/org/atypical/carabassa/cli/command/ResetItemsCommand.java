package org.atypical.carabassa.cli.command;

import java.util.List;
import java.util.concurrent.Callable;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Component
@Command(name = "reset", description = "reset items.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class ResetItemsCommand implements Callable<Integer> {

	private static final CommandLogger cmdLogger = new CommandLogger();

	@Autowired
	private DatasetApiService datasetApiService;

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-s", "--search" }, description = "search conditions.")
	private String searchString;

	private int count;
	private int error;
	private int reset;
	private int total;

	@Override
	public Integer call() throws Exception {
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
						resetItem(datasetId, itemRepresentation);
					}
					cmdLogger.info(String.format("Reset %d of %d items (%d error)", reset, total, error));
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

	private void resetItem(Long datasetId, ItemRepresentation itemRepresentation) {
		try {
			cmdLogger.info(String.format("Reset item %s ( %d / %d ) ...", itemRepresentation.getId(), ++count, total));
			datasetApiService.resetItem(datasetId, itemRepresentation.getId());
			reset++;
		} catch (ApiException e) {
			cmdLogger.error(String.format("Error reset item %s", itemRepresentation.getId()), e);
			error++;
		}
	}
}
