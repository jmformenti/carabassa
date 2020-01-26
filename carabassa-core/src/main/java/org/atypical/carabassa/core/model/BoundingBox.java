package org.atypical.carabassa.core.model;

import java.io.Serializable;

public interface BoundingBox extends Serializable {

	public int getMinX();

	public int getMinY();

	public int getWidth();

	public int getHeight();

}
