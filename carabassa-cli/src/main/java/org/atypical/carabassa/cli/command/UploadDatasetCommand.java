package org.atypical.carabassa.cli.command;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "upload", description = "upload images to dataset.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class UploadDatasetCommand implements Callable<Integer> {

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-p", "--path" }, description = "base path to upload images.", required = true)
	private String path;

	@Override
	public Integer call() throws Exception {
		// TODO implement
		System.out.printf("upload dataset=%s path=%s", dataset, path);
		return 0;
	}

}
