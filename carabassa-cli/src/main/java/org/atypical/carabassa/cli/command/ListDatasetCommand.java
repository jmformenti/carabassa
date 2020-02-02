package org.atypical.carabassa.cli.command;

import java.util.List;
import java.util.concurrent.Callable;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.DateFormatter;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "list", description = "list datasets.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class ListDatasetCommand implements Callable<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(ListDatasetCommand.class);

	private static final String OUTPUT_FORMAT = "%10s\t%20s\t%40s\t%20s\t%20s\n";

	@Autowired
	private DatasetApiService datasetApiService;

	@Option(names = { "-d", "--dataset" }, description = "dataset name.")
	private String dataset;

	@Override
	public Integer call() throws Exception {
		try {
			List<DatasetRepresentation> datasets = datasetApiService.findAll();
			if (!datasets.isEmpty()) {
				System.out.format(OUTPUT_FORMAT, "id", "name", "description", "creation", "modification");
				for (DatasetRepresentation datasetRepresentation : datasets) {
					System.out.format(OUTPUT_FORMAT, datasetRepresentation.getId(), datasetRepresentation.getName(),
							datasetRepresentation.getDescription(),
							DateFormatter.toLocalDateFormatted(datasetRepresentation.getCreation()),
							datasetRepresentation.getModification() == null ? ""
									: DateFormatter.toLocalDateFormatted(datasetRepresentation.getModification()));
				}
			} else {
				System.out.println("No datasets found.");
			}
		} catch (ApiException e) {
			logger.error("API error", e);
			System.err.println(e.getMessage());
			return 1;
		}
		return 0;
	}

}
