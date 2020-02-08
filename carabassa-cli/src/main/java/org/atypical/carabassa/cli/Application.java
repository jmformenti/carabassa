package org.atypical.carabassa.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// TODO Show localized messages (afegir core com a dependencia? carabassa-indexer-db, carabassa-storage-fs)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
