package org.atypical.carabassa.core.component.tagger;

import java.io.IOException;
import java.util.Set;

import org.atypical.carabassa.core.model.Tag;
import org.springframework.core.io.Resource;

public interface Tagger {

	public static final String TAG_PREFIX = "meta.";
	public static final String TAG_HASH = TAG_PREFIX + "Hash";
	public static final String TAG_ARCHIVE_TIME = TAG_PREFIX + "ArchiveTime";
	public static final String TAG_FILE_TYPE = TAG_PREFIX + "FileType";

	public Set<Tag> getTags(Resource inputItem) throws IOException;

}
