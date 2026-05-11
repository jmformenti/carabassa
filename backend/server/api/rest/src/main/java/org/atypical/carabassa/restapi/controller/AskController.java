package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.restapi.representation.model.AskRequestRepresentation;
import org.atypical.carabassa.restapi.representation.model.AskResponseRepresentation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = { "/api/dataset" })
public interface AskController {

    @PostMapping(value = "/{datasetId}/ask")
    AskResponseRepresentation ask(@PathVariable("datasetId") Long datasetId,
                                  @RequestBody @Valid AskRequestRepresentation request);
}
