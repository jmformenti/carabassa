package org.atypical.carabassa.cli.command;

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
@Command(name = "update", description = "update dataset.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class UpdateDatasetCommand implements Callable<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(UpdateDatasetCommand.class);

	@Autowired
	private DatasetApiService datasetApiService;

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-e", "--description" }, description = "dataset description.")
	private String description;

	@Override
	public Integer call() throws Exception {
		try {
			Long datasetId = datasetApiService.findByName(dataset);
			datasetApiService.update(datasetId, description);
			System.out.println("updated.");
		} catch (ApiException e) {
			logger.error("API error", e);
			System.err.println(e.getMessage());
			return 1;
		}
		return 0;
	}

}
