package org.atypical.carabassa.core.component.tagger;

import java.io.IOException;
import java.util.Set;

import org.atypical.carabassa.core.model.Tag;
import org.springframework.core.io.Resource;

public interface Tagger {

	public Set<Tag> getTags(Resource inputImage) throws IOException;

}
