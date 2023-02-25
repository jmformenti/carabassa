package org.atypical.carabassa.indexer.rdbms.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.atypical.carabassa.core.model.BoundingBox;

@Entity
@Table(name = "BOUNDING_BOX")
@SequenceGenerator(name = "bbox_id_gen", sequenceName = "bbox_sequence")
public class BoundingBoxEntity implements BoundingBox {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "bbox_id_gen")
	private Long id;
	
	@Column(nullable = false)
	private int minX;
	
	@Column(nullable = false)
	private int minY;
	
	@Column(nullable = false)
	private int width;
	
	@Column(nullable = false)
	private int height;

	public int getMinX() {
		return minX;
	}

	public void setMinX(int minX) {
		this.minX = minX;
	}

	public int getMinY() {
		return minY;
	}

	public void setMinY(int minY) {
		this.minY = minY;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

}
