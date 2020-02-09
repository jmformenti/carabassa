package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.restapi.representation.model.NoContentRepresentation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = { "/api" })
public interface IndexController {

	@GetMapping("/")
	public NoContentRepresentation index();

}
