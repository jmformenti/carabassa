package org.atypical.carabassa.restapi.representation.assembler;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.representation.mapper.ItemMapper;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ItemModelAssembler extends RepresentationModelAssemblerSupport<IndexedItem, ItemRepresentation> {

    private static final String ANONYMOUS_USER = "anonymousUser";

    @Autowired
    private ItemMapper itemMapper;

    public ItemModelAssembler() {
        super(DatasetController.class, ItemRepresentation.class);
    }

    @Override
    public ItemRepresentation toModel(IndexedItem indexedItem) {
        ItemRepresentation model = itemMapper.toBaseRepresentation(indexedItem);
        return enrichModel(indexedItem, model);
    }

    public ItemRepresentation toDetailedModel(IndexedItem indexedItem) {
        ItemRepresentation model = itemMapper.toRepresentation(indexedItem);
        return enrichModel(indexedItem, model);
    }

    private ItemRepresentation enrichModel(IndexedItem indexedItem, ItemRepresentation model) {
        model.setFavorite(isFavorite(indexedItem));
        if (indexedItem.getDataset() != null) {
            model.setDatasetId(indexedItem.getDataset().getId());
            model.setDatasetName(indexedItem.getDataset().getName());
        }
        return model;
    }

    private boolean isFavorite(IndexedItem item) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !ANONYMOUS_USER.equals(authentication.getName())) {
            String username = authentication.getName();
            return item.getTags().stream().anyMatch(t -> Tag.FAVORITE_NAME.equals(t.getName()) && username.equals(t.getValue()));
        }
        return false;
    }

}
