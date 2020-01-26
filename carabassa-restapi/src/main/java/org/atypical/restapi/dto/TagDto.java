package org.atypical.restapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TagDto implements Comparable<TagDto> {

	private Long id;
	private String name;
	private Object value;
	@JsonInclude(Include.NON_NULL)
	private BoundingBoxDto boundingBox;

	public TagDto() {
		super();
	}

	public TagDto(Long id, String name, Object value) {
		this.id = id;
		this.name = name;
		this.value = value;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public BoundingBoxDto getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(BoundingBoxDto boundingBox) {
		this.boundingBox = boundingBox;
	}

	@Override
	public int compareTo(TagDto o) {
		return getId().compareTo(o.getId());
	}

}
