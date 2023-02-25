package org.atypical.carabassa.cli.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import picocli.CommandLine.IVersionProvider;

@Component
public class CarabassaVersion implements IVersionProvider {

    @Value("${carabassa.version}")
    private String appVersion;

    @Override
    public String[] getVersion() {
        return new String[]{"v" + appVersion};
    }
}
