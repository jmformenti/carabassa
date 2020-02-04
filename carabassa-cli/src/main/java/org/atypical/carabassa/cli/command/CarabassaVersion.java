package org.atypical.carabassa.cli.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import picocli.CommandLine.IVersionProvider;

@Component
public class CarabassaVersion implements IVersionProvider {

	@Value("${app.version}")
	private String appVersion;

	@Override
	public String[] getVersion() throws Exception {
		return new String[] { "v" + appVersion };
	}
}
