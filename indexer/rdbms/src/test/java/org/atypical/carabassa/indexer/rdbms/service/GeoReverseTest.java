package org.atypical.carabassa.indexer.rdbms.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.atypical.carabassa.core.component.tagger.impl.ImageMetadataTagger;
import org.atypical.carabassa.core.configuration.CoreConfiguration;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.indexer.rdbms.configuration.IndexerRdbmsConfiguration;
import org.atypical.carabassa.indexer.rdbms.test.configuration.TestConfiguration;
import org.atypical.carabassa.indexer.rdbms.test.helper.TestHelper;
import org.atypical.carabassa.storage.fs.configuration.StorageFSConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import atlas.Atlas;
import atlas.City;

@ContextConfiguration(classes = {CoreConfiguration.class, IndexerRdbmsConfiguration.class,
        StorageFSConfiguration.class, TestConfiguration.class})
@DataJpaTest
class GeoReverseTest {

    @Autowired
    private ImageMetadataTagger imageMetadataTagger;

    @Test
    void test() throws IOException {
        Atlas atlas = new Atlas();

        Resource inputItem = TestHelper.getImageResource("cf5fbce739271d5ad66529cfe792a567.jpg");
        Set<Tag> tags = imageMetadataTagger.getTags(inputItem);

        Double lat = tags.stream().filter(t -> ImageMetadataTagger.TAG_GEO_LATITUDE.equals(t.getName())).findFirst()
                .get().getValue(Double.class);
        Double lng = tags.stream().filter(t -> ImageMetadataTagger.TAG_GEO_LONGITUDE.equals(t.getName())).findFirst()
                .get().getValue(Double.class);

        long startTime = System.currentTimeMillis();
        City city = new Atlas().find(lat, lng);
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println(city.name);
        System.out.println(estimatedTime);

        System.out.println("First query");
        List<City> cities = atlas.findAll(lat, lng);

        for (City c : cities) {
            System.out.println(c);
        }

        System.out.println("Second query");
        cities = atlas.withLimit(3).withMaxDistance(5000).findAll(lat, lng);

        for (City c : cities) {
            System.out.println(c);
        }
    }

}
