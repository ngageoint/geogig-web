package org.geogig.web.client.datastore;

import org.geogig.web.client.Client.FeatureStreamFormat;
import org.geotools.data.simple.SimpleFeatureSource;

public interface GeogigVersionedFeatureSource extends SimpleFeatureSource {

    public void setPreferredStreamFormat(FeatureStreamFormat... preferredOrdered);
}
