package org.atypical.carabassa.cli.command;

import java.util.List;
import java.util.concurrent.Callable;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.cli.util.DateFormatter;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Component
@Command(name = "list", description = "list datasets.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class ListDatasetCommand implements Callable<Integer> {

	private static final String OUTPUT_FORMAT = "%10s\t%20s\t%40s\t%20s\t%20s\n";

	private CommandLogger cmdLogger = new CommandLogger();

	@Option(names = { "-d", "--dataset" }, description = "dataset name.")
	private String dataset;

	@Autowired
	private DatasetApiService datasetApiService;

	@Override
	public Integer call() throws Exception {
		try {
			List<DatasetEntityRepresentation> datasets = datasetApiService.findAll();
			if (!datasets.isEmpty()) {
				System.out.format(OUTPUT_FORMAT, "id", "name", "description", "creation", "modification");
				for (DatasetEntityRepresentation datasetRepresentation : datasets) {
					System.out.format(OUTPUT_FORMAT, datasetRepresentation.getId(), datasetRepresentation.getName(),
							datasetRepresentation.getDescription(),
							DateFormatter.toLocalDateFormatted(datasetRepresentation.getCreation()),
							datasetRepresentation.getModification() == null ? ""
									: DateFormatter.toLocalDateFormatted(datasetRepresentation.getModification()));
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
