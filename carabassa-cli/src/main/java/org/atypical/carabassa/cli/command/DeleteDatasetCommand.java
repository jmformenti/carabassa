package org.atypical.carabassa.cli.command;

import java.util.Scanner;
import java.util.concurrent.Callable;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "delete", description = "delete dataset.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class DeleteDatasetCommand implements Callable<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(DeleteDatasetCommand.class);

	@Autowired
	private DatasetApiService datasetApiService;

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Override
	public Integer call() throws Exception {
		try {
			Long datasetId = datasetApiService.findByName(dataset);
			if (doConfirm(String.format(
					"This action cannot be reversed. Are you sure you want to delete the dataset '%s'? [y|N] ",
					dataset))) {
				datasetApiService.delete(datasetId);
				System.out.println("deleted.");
			}
		} catch (ApiException e) {
			logger.error("API error", e);
			System.err.println(e.getMessage());
			return 1;
		}
		return 0;
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
}
