package org.atypical.carabassa.core.component.storage;

import java.io.IOException;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.springframework.core.io.Resource;

public interface DatasetStorage {

	public void create(String datasetName) throws IOException, EntityExistsException;

	public void addImage(Dataset dataset, IndexedImage image, Resource inputImage)
			throws IOException, EntityExistsException;

	public StoredImage getImage(Dataset dataset, IndexedImage image) throws IOException, EntityNotFoundException;

	public void deleteAll() throws IOException;

	public void delete(Dataset dataset) throws IOException;

	public void deleteImage(Dataset dataset, IndexedImage image) throws IOException;

	public void update(Dataset dataset) throws IOException;

}
