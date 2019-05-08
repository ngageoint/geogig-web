/* Copyright (c) 2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.geogig.server.geogig;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigAction;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.repository.impl.GlobalContextBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 */
public @Slf4j class RepositoryStore {

    private LoadingCache<String, Repository> repositories;

    private final URI rootRepoURI;

    private final RepositoryResolver resolver;

    public RepositoryStore(final URI rootRepoURI) {
        checkNotNull(rootRepoURI, "root repo URI is null");

        resolver = RepositoryResolver.lookup(rootRepoURI);

        this.rootRepoURI = rootRepoURI;

        try {
            this.repositories = buildCache();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Repository create(@NonNull String repositoryName) {
        return create(repositoryName, Collections.emptyMap());
    }

    public Repository create(@NonNull String repositoryName, @NonNull Map<String, String> config) {

        final URI repoURI = RepositoryResolver.lookup(rootRepoURI).buildRepoURI(rootRepoURI,
                repositoryName);
        final Hints hints = Hints.readWrite().uri(repoURI);
        hints.set(Hints.REPOSITORY_NAME, repositoryName);
        final Context context = GlobalContextBuilder.builder().build(hints);
        Repository repo = context.command(InitOp.class).setConfig(config).call();
        this.repositories.put(repositoryName, repo);
        return repo;
    }

    public Repository getRepoByName(@NonNull String repositoryName)
            throws RepositoryConnectionException {
        try {
            return repositories.get(repositoryName);
        } catch (ExecutionException e) {
            log.warn("Unable to load repository {}", repositoryName, e);
            Throwable cause = e.getCause();
            Throwables.throwIfInstanceOf(cause, RepositoryConnectionException.class);
            Throwables.throwIfUnchecked(cause);
            throw new RuntimeException(cause);
        }
    }

    private static final RemovalListener<String, Repository> REMOVAL_LISTENER = new RemovalListener<String, Repository>() {

        public void onRemoval(RemovalNotification<String, Repository> notification) {
            final RemovalCause cause = notification.getCause();
            final String key = notification.getKey();
            final Repository repo = notification.getValue();
            log.info("Disposing repository {}. Cause: {}", key, cause(cause));
            try {
                if (repo != null && repo.isOpen()) {
                    repo.close();
                }
            } catch (RuntimeException e) {
                log.warn("Error closing repository {}", key, e);
            }
        }

        private String cause(RemovalCause cause) {
            switch (cause) {
            case COLLECTED:
                return "removed automatically because its key or value was garbage-collected";
            case EXPIRED:
                return "expiration timestamp has passed";
            case EXPLICIT:
                return "manually removed by remove() or invalidateAll()";
            case REPLACED:
                return "manually replaced";
            case SIZE:
                return "evicted due to cache size constraints";
            default:
                return "Unknown";
            }
        }
    };

    private LoadingCache<String, Repository> buildCache() throws IOException {

        CacheLoader<String, Repository> loader = new CacheLoader<String, Repository>() {

            public Repository load(final String key) throws Exception {
                Repository repo = loadGeoGIG(key);
                return repo;
            }

        };

        LoadingCache<String, Repository> cache = CacheBuilder.newBuilder()//
                .concurrencyLevel(1)//
                // .expireAfterAccess(1, TimeUnit.MINUTES)//
                .maximumSize(1024)//
                .removalListener(REMOVAL_LISTENER)//
                .build(loader);

        return cache;
    }

    @VisibleForTesting
    Repository loadGeoGIG(@NonNull String repositoryName) {
        log.debug("Loading repository " + repositoryName + " using "
                + resolver.getClass().getSimpleName());
        Hints hints = new Hints();
        final URI repoURI = resolver.buildRepoURI(rootRepoURI, repositoryName);
        hints.set(Hints.REPOSITORY_URL, repoURI);
        hints.set(Hints.REPOSITORY_NAME, repositoryName);

        Context context = GlobalContextBuilder.builder().build(hints);

        Repository repository = context.repository();

        if (!repository.isOpen()) {
            // Only open it if is was an existing repository.
            for (String existingRepo : resolver.listRepoNamesUnderRootURI(rootRepoURI)) {
                if (existingRepo.equals(repositoryName)) {
                    try {
                        repository.open();
                    } catch (RepositoryConnectionException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }
        return repository;
    }

    public void delete(@NonNull String repoName) {
        try {
            final URI repoURI = resolveRepoURI(repoName);
            // remove from cache and close
            this.invalidate(repoName);
            // actually delete the repo
            GeoGIG.delete(repoURI);
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    public URI resolveRepoURI(@NonNull String repoName) {
        final URI repoURI = resolver.buildRepoURI(rootRepoURI, repoName);
        return repoURI;
    }

    public void invalidate(@NonNull String repoName) {
        this.repositories.invalidate(repoName);
    }

    public void invalidateAll() {
        this.repositories.invalidateAll();
    }

    public List<String> getAllNames() {
        List<String> allNames = this.resolver.listRepoNamesUnderRootURI(rootRepoURI);
        return allNames;
    }

    public void setConfig(@NonNull String name, @NonNull Map<String, String> config)
            throws RepositoryConnectionException {
        Repository repo = getRepoByName(name);
        config.forEach((key, value) -> {
            repo.command(ConfigOp.class)
                    .setAction(value == null ? ConfigAction.CONFIG_UNSET : ConfigAction.CONFIG_SET)
                    .setName(key).setValue(value).call();
        });
        this.invalidate(name);
    }

}