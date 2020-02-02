package org.atypical.carabassa.restapi.controller;

import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = { "/api" })
public interface RootController {

	@GetMapping
	public EntityModel<String> actions();

}
