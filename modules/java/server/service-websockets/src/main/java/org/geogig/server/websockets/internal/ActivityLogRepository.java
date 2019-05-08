package org.geogig.server.websockets.internal;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

public @Repository interface ActivityLogRepository
        extends PagingAndSortingRepository<ActivityEntry, Integer>,
        JpaSpecificationExecutor<ActivityEntry> {

}
