package org.geogig.web.client;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Range;

import lombok.NonNull;

public class PagedIterator<T> implements Iterator<T> {

    private Function<Range<Integer>, List<T>> pager;

    public PagedIterator(@NonNull Function<Range<Integer>, List<T>> pager) {
        this.pager = pager;
    }

    public @Override boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    public @Override T next() {
        // TODO Auto-generated method stub
        return null;
    }
}
