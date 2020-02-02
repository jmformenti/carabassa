package org.atypical.carabassa.cli.command;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Component
@Command(name = "carabassa", mixinStandardHelpOptions = true, subcommands = { CreateDatasetCommand.class,
		ListDatasetCommand.class, UpdateDatasetCommand.class, DeleteDatasetCommand.class, UploadDatasetCommand.class })
// TODO show version
public class CarabassaCommand implements Callable<Integer> {

	@Override
	public Integer call() throws Exception {
		new CommandLine(new CarabassaCommand()).usage(System.out);

		return 0;
	}

}
