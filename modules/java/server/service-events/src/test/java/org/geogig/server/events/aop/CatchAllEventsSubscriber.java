package org.geogig.server.events.aop;

import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.geogig.server.events.model.Event;
import org.hamcrest.Matchers;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.NonNull;

public @Component class CatchAllEventsSubscriber {

    private List<Event> events = new ArrayList<>();

    public @EventListener void any(Event event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }

    public int size() {
        return events.size();
    }

    public Object first() {
        return events.get(0);
    }

    public <T> T first(Class<T> expectedType) {
        Object first = first();
        assertThat(first, Matchers.instanceOf(expectedType));
        return expectedType.cast(first);
    }

    public Object last() {
        return events.get(events.size() - 1);
    }

    public <T> T last(Class<T> expectedType) {
        Object last = last();
        assertThat(last, Matchers.instanceOf(expectedType));
        return expectedType.cast(last);
    }

    public <T> Optional<T> findLast(@NonNull Class<T> expectedType) {
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (expectedType.isInstance(event)) {
                return Optional.of(expectedType.cast(event));
            }
        }
        return Optional.empty();
    }

    public <T> List<T> findAll(@NonNull Class<T> expectedType) {
        return events.stream().filter(e -> expectedType.isInstance(e))
                .map(e -> expectedType.cast(e)).collect(Collectors.toList());
    }
}