package org.atypical.carabassa.core.component.tagger;

import org.atypical.carabassa.core.model.Tag;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Set;

public interface Tagger {

    String TAG_PREFIX = "meta.";
    String TAG_HASH = TAG_PREFIX + "Hash";
    String TAG_ARCHIVE_TIME = TAG_PREFIX + "ArchiveTime";
    String TAG_FILE_TYPE = TAG_PREFIX + "FileType";

    Set<Tag> getTags(Resource inputItem) throws IOException;

}
