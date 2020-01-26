package org.atypical.core.component.tagger;

import java.io.IOException;
import java.util.Set;

import org.atypical.core.model.Tag;
import org.springframework.core.io.Resource;

public interface Tagger {

	public Set<Tag> getTags(Resource inputImage) throws IOException;

}
