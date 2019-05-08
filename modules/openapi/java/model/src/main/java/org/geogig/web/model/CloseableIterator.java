package org.geogig.web.model;

import java.io.Closeable;
import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T>, Closeable {

    /**
     * Close override as an idempotent method
     */
    public @Override void close();

}
