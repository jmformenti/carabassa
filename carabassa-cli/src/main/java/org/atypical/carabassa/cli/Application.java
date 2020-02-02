package org.atypical.carabassa.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// TODO Crear service DatasetApiService en carabassa-uploader
//      Fer tests
//		Mostrar missatges localized
//		Canviar banner i a restapi també
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
