package org.atypical.carabassa.restapi.representation.assembler;

import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.restapi.controller.TagInfoController;
import org.atypical.carabassa.restapi.representation.mapper.TagInfoMapper;
import org.atypical.carabassa.restapi.representation.model.TagInfoEntityRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class TagInfoModelAssembler extends RepresentationModelAssemblerSupport<TagInfo, TagInfoEntityRepresentation> {

    @Autowired
    private TagInfoMapper tagInfoMapper;

    public TagInfoModelAssembler() {
        super(TagInfoController.class, TagInfoEntityRepresentation.class);
    }

    @Override
    public TagInfoEntityRepresentation toModel(TagInfo tagInfo) {
        return tagInfoMapper.toRepresentation(tagInfo);
    }
}
