package org.atypical.core.component.storage;

import java.io.IOException;

import org.atypical.core.exception.EntityExistsException;
import org.atypical.core.exception.EntityNotFoundException;
import org.atypical.core.model.Dataset;
import org.atypical.core.model.IndexedImage;
import org.atypical.core.model.StoredImage;
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
