package org.atypical.carabassa.cli.command;

import java.util.List;
import java.util.Scanner;
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
@Command(name = "delete-items", description = "delete items.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class DeleteItemCommand implements Callable<Integer> {

	private CommandLogger cmdLogger = new CommandLogger();

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-s", "--search" }, description = "search conditions.")
	private String searchString;

	@Option(names = { "-f", "--force" }, description = "no ask before deleting.", defaultValue = "false")
	private Boolean force;

	@Autowired
	private DatasetApiService datasetApiService;

	private int count;
	private int error;
	private int deleted;
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
					if (force || doConfirm(String.format(
							"This action cannot be reversed. You are going to delete %d items in dataset '%s'. Are you sure? [y|N] ",
							items.size(), dataset))) {
						total = items.size();
						for (ItemRepresentation itemRepresentation : items) {
							deleteItem(datasetId, itemRepresentation);	
						}
						cmdLogger.info(String.format("Deleted %d of %d items (%d error)", deleted, total, error));
					}
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

	private boolean doConfirm(String text) {
		System.out.print(text);
		try (Scanner scanner = new Scanner(System.in)) {
			String userInput = scanner.next();
			if (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes")) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	private void deleteItem(Long datasetId, ItemRepresentation itemRepresentation) {
		try {
			cmdLogger.info(String.format("Delete item %s ( %d / %d ) ...", itemRepresentation.getId(), ++count, total));
			datasetApiService.deleteItem(datasetId, itemRepresentation.getId());
			deleted++;
		} catch (ApiException e) {
			cmdLogger.error(String.format("Error delete item %s", itemRepresentation.getId()), e);
			error++;
		}
	}

}
