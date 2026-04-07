package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api/user"})
public interface UserController {

    @GetMapping("/favorite")
    PagedModel<ItemRepresentation> findFavorites(Pageable pageable);

}
