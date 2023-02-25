package org.atypical.carabassa.cli.command;

import java.util.Scanner;
import java.util.concurrent.Callable;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Component
@Command(name = "delete", description = "delete dataset.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class DeleteDatasetCommand implements Callable<Integer> {

	private CommandLogger cmdLogger = new CommandLogger();

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Autowired
	private DatasetApiService datasetApiService;

	@Override
	public Integer call() throws Exception {
		try {
			cmdLogger.info(String.format("Deleting dataset %s ...", dataset));

			Long datasetId = datasetApiService.findByName(dataset);
			if (doConfirm(String.format(
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
