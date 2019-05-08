package org.geogig.server.websockets;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.stores.StoreService;
import org.geogig.server.service.user.UserService;
import org.geogig.server.websockets.internal.ActivityEntry;
import org.geogig.server.websockets.internal.ActivityLogRepository;
import org.geogig.web.model.ActivityLogRequest;
import org.geogig.web.model.ActivityLogResponse;
import org.geogig.web.model.BranchEvent;
import org.geogig.web.model.ForkEvent;
import org.geogig.web.model.LayerEvent;
import org.geogig.web.model.PullRequestEvent;
import org.geogig.web.model.RepositoryEvent;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.StoreEvent;
import org.geogig.web.model.TransactionEvent;
import org.geogig.web.model.UserEvent;
import org.geogig.web.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public @Service class ActivityLogService implements SmartLifecycle {

    private @Autowired ActivityLogRepository repository;

    private @Autowired UserService users;

    private @Autowired RepositoryManagementService repos;

    private @Autowired StoreService stores;

    private boolean running;

    public @EventListener void onEvent(ServerEvent event) {
        if (log.isTraceEnabled()) {
            log.trace("Saving event %s to activity log", event);
        } else {
            log.debug("Saving event %s to activity log", event.getObjectType());
        }
        save(event);
    }

    public @Override void start() {
        running = true;
    }

    public @Override void stop(@NonNull Runnable callback) {
        stop();
        callback.run();
    }

    public @Override void stop() {
        running = false;
    }

    public @Override boolean isRunning() {
        return running;
    }

    public void save(@NonNull ServerEvent event) {
        if (!isRunning()) {
            log.info("Context closed, can't save event to event log: " + event);
        }
        ActivityEntry entity = new ActivityEntry();
        // payload.getObjectType() is null here, it's only set once serialized by the Jackson
        // ObjectMapper
        entity.setEvent(event.getClass().getSimpleName());
        entity.setEventType(event.getEventType());
        entity.setTimestamp(event.getTimestamp().toInstant());
        entity.setPayload(event);
        entity.setIssuer(inferEventIssuerUser(event));
        entity.setStore(inferStore(event));
        entity.setRepoOwner(inferRepoOwner(event));
        entity.setRepo(inferRepo(event));
        entity.setBranch(inferBranch(event));
        entity.setLayer(inferLayer(event));
        ActivityEntry entry = repository.save(entity);
        log.trace("Saved ActivityEntry {}", entry);
    }

    public ActivityLogResponse getActivity(final @NonNull ActivityLogRequest request) {

        Pageable pageReq = PageRequest.of(page(request), pageSize(request), Direction.DESC,
                "timestamp");

        Specification<ActivityEntry> spec = createQuerySpec(request);

        Page<ActivityEntry> page = repository.findAll(spec, pageReq);

        List<ActivityEntry> content = page.getContent();
        int number = page.getNumber();
        // int numberOfElements = page.getNumberOfElements();
        int totalPages = page.getTotalPages();
        long totalElements = page.getTotalElements();
        ActivityLogResponse response = new ActivityLogResponse();
        response.setRequest(request);
        response.setPageNumber(number);
        response.setTotalPages(totalPages);
        response.setTotalElements(totalElements);
        response.setResults(Lists.transform(content, entry -> entry.getPayload()));
        return response;
    }

    private Specification<ActivityEntry> createQuerySpec(@NonNull ActivityLogRequest req) {
        OffsetDateTime since = req.getSince();
        OffsetDateTime until = req.getUntil();

        String store = req.getStore();
        String user = req.getUser();
        String repository = req.getRepository();
        String branch = req.getBranch();
        String layer = req.getLayer();
        String event = req.getEvent();
        String topic = req.getTopic();

        Specification<ActivityEntry> spec = Specification.where(since(since)).and(until(until))//
                .and(eq("store", storeId(store)))//
                .and(eq("user", userId(user)))//
                .and(eq("repo", repositoryId(user, repository)))//
                .and(eq("branch", branch))//
                .and(eq("layer", layer))//
                .and(eq("event", event))//
                .and(eq("topic", topic));

        return spec;
    }

    private @Nullable UUID repositoryId(String ownerName, String repoName) {
        if (isNullOrEmpty(repoName)) {
            return null;
        }
        if (isNullOrEmpty(ownerName)) {
            throw new IllegalArgumentException(
                    "Owner name and repository name are required to query by repository, "
                            + "but only repository name was provided in the request");
        }
        return repos.getOrFail(ownerName, repoName).getId();
    }

    private @Nullable UUID userId(String user) {
        return isNullOrEmpty(user) ? null : users.getByNameOrFail(user).getId();
    }

    private @Nullable UUID storeId(String store) {
        return isNullOrEmpty(store) ? null : stores.getByNameOrFail(store).getId();
    }

    /**
     * @see Specification#toPredicate(Root, CriteriaQuery, CriteriaBuilder)
     */
    final @Nullable Specification<ActivityEntry> since(OffsetDateTime when) {
        return when == null ? null
                : (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("timestamp"),
                        builder.literal(when));
    }

    /**
     * @see Specification#toPredicate(Root, CriteriaQuery, CriteriaBuilder)
     */
    final @Nullable Specification<ActivityEntry> until(OffsetDateTime when) {
        return when == null ? null
                : (root, query, builder) -> builder.lessThanOrEqualTo(root.get("timestamp"),
                        builder.literal(when));
    }

    /**
     * @see Specification#toPredicate(Root, CriteriaQuery, CriteriaBuilder)
     */
    final @Nullable Specification<ActivityEntry> eq(@NonNull String property, Object value) {
        return value == null ? null
                : (root, query, builder) -> builder.equal(root.get(property), value);
    }

    private int pageSize(ActivityLogRequest request) {
        if (request.getPageSize() == null) {
            request.setPageSize(100);
        }
        return request.getPageSize();
    }

    private int page(ActivityLogRequest request) {
        if (request.getPage() == null) {
            request.setPage(1);
        }
        return request.getPage();
    }

    protected @Nullable String inferLayer(@NonNull ServerEvent event) {
        if (event instanceof LayerEvent) {
            return LayerEvent.class.cast(event).getSubject().getName();
        }
        return null;
    }

    private @Nullable String inferBranch(@NonNull ServerEvent event) {
        if (event instanceof BranchEvent) {
            return BranchEvent.class.cast(event).getSubject().getName();
        }
        if (event instanceof LayerEvent) {
            return LayerEvent.class.cast(event).getBranch().getName();
        }
        return null;
    }

    private @Nullable UUID inferRepo(@NonNull ServerEvent event) {
        if (event instanceof ForkEvent) {
            return ForkEvent.class.cast(event).getSubject().getForkedFrom().getId();
        }
        if (event instanceof TransactionEvent) {
            return ((TransactionEvent) event).getSubject().getRepository().getId();
        }
        if (event instanceof RepositoryEvent) {
            return RepositoryEvent.class.cast(event).getSubject().getId();
        }
        if (event instanceof PullRequestEvent) {
            return PullRequestEvent.class.cast(event).getSubject().getTargetRepo().getId();
        }
        if (event instanceof BranchEvent) {
            return BranchEvent.class.cast(event).getRepository().getId();
        }
        if (event instanceof LayerEvent) {
            return LayerEvent.class.cast(event).getRepository().getId();
        }

        return null;
    }

    private @Nullable UUID inferRepoOwner(@NonNull ServerEvent event) {
        if (event instanceof UserEvent) {
            return UserEvent.class.cast(event).getSubject().getId();
        }
        if (event instanceof ForkEvent) {
            return ForkEvent.class.cast(event).getSubject().getForkedFrom().getOwner().getId();
        }
        if (event instanceof TransactionEvent) {
            return ((TransactionEvent) event).getSubject().getRepository().getOwner().getId();
        }
        if (event instanceof RepositoryEvent) {
            return RepositoryEvent.class.cast(event).getSubject().getOwner().getId();
        }
        if (event instanceof PullRequestEvent) {
            return PullRequestEvent.class.cast(event).getSubject().getTargetRepo().getOwner()
                    .getId();
        }
        if (event instanceof BranchEvent) {
            return BranchEvent.class.cast(event).getRepository().getOwner().getId();
        }
        if (event instanceof LayerEvent) {
            return LayerEvent.class.cast(event).getRepository().getOwner().getId();
        }

        return null;
    }

    private @Nullable UUID inferEventIssuerUser(@NonNull ServerEvent event) {
        UserInfo caller = event.getCaller();
        return caller == null ? null : caller.getId();
    }

    protected @Nullable UUID inferStore(@NonNull ServerEvent event) {
        if (event instanceof StoreEvent) {
            return ((StoreEvent) event).getSubject().getId();
        }
        if (event instanceof ForkEvent) {
            return ForkEvent.class.cast(event).getSubject().getForkedFrom().getStore().getId();
        }
        if (event instanceof TransactionEvent) {
            return ((TransactionEvent) event).getSubject().getRepository().getStore().getId();
        }
        if (event instanceof RepositoryEvent) {
            return RepositoryEvent.class.cast(event).getSubject().getStore().getId();
        }
        if (event instanceof PullRequestEvent) {
            return PullRequestEvent.class.cast(event).getSubject().getTargetRepo().getStore()
                    .getId();
        }
        if (event instanceof BranchEvent) {
            return BranchEvent.class.cast(event).getRepository().getStore().getId();
        }
        if (event instanceof LayerEvent) {
            return LayerEvent.class.cast(event).getRepository().getStore().getId();
        }

        return null;
    }

    public @Override int getPhase() {
        return 0;
    }

    public @Override boolean isAutoStartup() {
        return true;
    }
}
