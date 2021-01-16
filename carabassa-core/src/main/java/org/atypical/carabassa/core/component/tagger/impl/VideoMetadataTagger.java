package org.atypical.carabassa.core.component.tagger.impl;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import org.atypical.carabassa.core.component.tagger.Tagger;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.atypical.carabassa.core.util.HashGenerator;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

@Component
public class VideoMetadataTagger implements Tagger {

	private final static String METADATA_CREATION_TIME_FIELD = "creation_time";

	@Override
	public Set<Tag> getTags(Resource inputItem) throws IOException {
		Set<Tag> tags = new HashSet<>();

		MultimediaObject mmObject = new MultimediaObject(inputItem.getFile());
		MultimediaInfo info;
		try {
			info = mmObject.getInfo();
		} catch (EncoderException e) {
			throw new IOException(e);
		}

		tags.add((Tag) new TagImpl(TAG_FILE_TYPE, info.getFormat()));
		tags.add((Tag) new TagImpl(TAG_PREFIX + "Duration", info.getDuration()));
		if (info.getMetadata().get(METADATA_CREATION_TIME_FIELD) != null) {
			tags.add((Tag) new TagImpl(TAG_ARCHIVE_TIME,
					ZonedDateTime.parse(info.getMetadata().get(METADATA_CREATION_TIME_FIELD))));
		}

		if (info.getVideo() != null) {
			tags.add((Tag) new TagImpl(TAG_PREFIX + "VideoBitRate", info.getVideo().getBitRate()));
			tags.add((Tag) new TagImpl(TAG_PREFIX + "VideoDecoder", info.getVideo().getDecoder()));
			tags.add((Tag) new TagImpl(TAG_PREFIX + "VideoFrameRate", info.getVideo().getFrameRate()));
			tags.add((Tag) new TagImpl(TAG_PREFIX + "VideoSizeHeight", info.getVideo().getSize().getHeight()));
			tags.add((Tag) new TagImpl(TAG_PREFIX + "VideoSizeWidth", info.getVideo().getSize().getWidth()));
		}
		if (info.getAudio() != null) {
			tags.add((Tag) new TagImpl(TAG_PREFIX + "AudioBitRate", info.getAudio().getBitRate()));
			tags.add((Tag) new TagImpl(TAG_PREFIX + "AudioChannels", info.getAudio().getChannels()));
			tags.add((Tag) new TagImpl(TAG_PREFIX + "AudioDecoder", info.getAudio().getDecoder()));
			tags.add((Tag) new TagImpl(TAG_PREFIX + "AudioSamplingRate", info.getAudio().getSamplingRate()));
		}

		tags.add((Tag) new TagImpl(TAG_HASH, HashGenerator.generate(inputItem)));

		return tags;
	}

}
