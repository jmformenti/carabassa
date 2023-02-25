package org.atypical.carabassa.cli.command;

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
@Command(name = "create", description = "create new dataset.")
public class CreateDatasetCommand implements Callable<Integer> {

	private final CommandLogger cmdLogger = new CommandLogger();

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-e", "--description" }, description = "dataset description.")
	private String description;

	@Autowired
	private DatasetApiService datasetApiService;

	@Override
	public Integer call() throws Exception {
		try {
			cmdLogger.info(String.format("Creating dataset %s ...", dataset));

			Long id = datasetApiService.create(dataset, description);

			cmdLogger.info(String.format("created dataset with id = %d.", id));
		} catch (ApiException e) {
			cmdLogger.error("API error", e);
			return ExitCode.SOFTWARE;
		}
		return ExitCode.OK;
	}

}
