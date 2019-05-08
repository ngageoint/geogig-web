package org.geogig.server.stats.repository;

import org.geogig.server.events.model.StoreEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class StoreStatsService {

    final @EventListener void onStoreCreated(StoreEvent.Created event) {

    }

    final @EventListener void onStoreModified(StoreEvent.Created event) {

    }

    final @EventListener void onStoreDeleted(StoreEvent.Created event) {

    }

}
