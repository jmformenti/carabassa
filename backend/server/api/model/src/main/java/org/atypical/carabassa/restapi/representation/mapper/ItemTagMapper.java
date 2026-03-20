package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.ItemTagInfo;
import org.atypical.carabassa.restapi.representation.model.ItemTagEntityRepresentation;

public interface ItemTagMapper {

    ItemTagEntityRepresentation toRepresentation(ItemTagInfo itemTagInfo);

}
