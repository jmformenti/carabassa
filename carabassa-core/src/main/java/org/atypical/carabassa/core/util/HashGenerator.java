package org.atypical.carabassa.core.util;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;

public class HashGenerator {

	public static String generate(Resource resource) throws IOException {
		try (InputStream inputStream = resource.getInputStream()) {
			return generate(inputStream);
		}
	}

	public static String generate(InputStream inputStream) throws IOException {
		return DigestUtils.md5DigestAsHex(inputStream);
	}

}
