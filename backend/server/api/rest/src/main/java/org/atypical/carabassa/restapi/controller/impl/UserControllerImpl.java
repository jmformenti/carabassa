package org.atypical.carabassa.restapi.controller.impl;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.User;
import org.atypical.carabassa.core.service.UserService;
import org.atypical.carabassa.restapi.controller.UserController;
import org.atypical.carabassa.restapi.representation.assembler.ItemModelAssembler;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserControllerImpl implements UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ItemModelAssembler itemModelAssembler;

    @Autowired
    private PagedResourcesAssembler<IndexedItem> itemPagedResourcesAssembler;

    @Override
    public PagedModel<ItemRepresentation> findFavorites(Pageable pageable) {
        User user = () -> SecurityContextHolder.getContext().getAuthentication().getName();
        Page<IndexedItem> items = userService.findFavorites(user, pageable);
        return itemPagedResourcesAssembler.toModel(items, itemModelAssembler);
    }

}
