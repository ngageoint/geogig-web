package org.geogig.server.geogig;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;

import org.geogig.server.model.Store;
import org.geogig.server.service.stores.StoreInfoStore;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GlobalContextBuilder;
import org.locationtech.geogig.storage.ConfigDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

/**
 * TODO: greaceful shutdown mechanism to close all the repos open by the cached
 * {@link RepositoryStore}s
 */
public @Service class GeogigRepositoryProvider implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(GeogigRepositoryProvider.class);

    /**
     * The pair of {@link Store} and {@link RepositoryStore} that's cached by this
     * {@code StoreService} instance, allows to avoid a distributed cluster notification when a
     * {@link Store} changes. Whenever a {@link RepositoryStore} is required, it's {@link Store} is
     * acquired from the {@link StoreInfoStore} and compared for equality with the cached version.
     * If they differ, the {@link RepositoryStore} is re-acquired.
     *
     */
    private static @Value @EqualsAndHashCode(of = "baseURI") class CachedStore {
        final @NonNull String baseURI;

        final @NonNull RepositoryStore provider;
    }

    private final Striped<ReadWriteLock> locks = Striped
            .readWriteLock(4 * Runtime.getRuntime().availableProcessors());

    private final Cache<UUID, CachedStore> repoProviders;

    public @Autowired GeogigRepositoryProvider() {
        repoProviders = CacheBuilder.newBuilder()//
                .build();
    }

    public @Override void setApplicationContext(ApplicationContext applicationContext) {
        GlobalContextBuilder.builder(new ServerContextBuilder(applicationContext));
        log.info("Registered {}", ServerContextBuilder.class.getName());
    }

    public RepositoryStore connect(final @NonNull UUID storeId, final @NonNull String baseURI)
            throws RepositoryConnectionException {
        final RepositoryStore repoProvider;

        final ReadWriteLock lock = locks.get(storeId);
        lock.readLock().lock();
        try {
            CachedStore cachedStore = repoProviders.getIfPresent(storeId);

            String cachedParams = cachedStore == null ? null : cachedStore.baseURI;

            if (!Objects.equals(cachedParams, baseURI)) {
                if (cachedParams != null) {
                    log.info("Detected StoreInfo configuration change, "
                            + "re-connecting with the updated connection parameters");
                }
                lock.readLock().unlock();
                try {
                    repoProvider = connectOrCreate(storeId, baseURI);
                } finally {
                    lock.readLock().lock();
                }
            } else {
                repoProvider = cachedStore.provider;
            }
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, RepositoryConnectionException.class);
            RepositoryConnectionException ex = new RepositoryConnectionException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } finally {
            lock.readLock().unlock();
        }

        return repoProvider;
    }

    public RepositoryStore connectOrCreate(final @NonNull UUID storeId,
            final @NonNull String rootURI) throws RepositoryConnectionException {

        RepositoryStore repositoryProvider;
        final ReadWriteLock lock = locks.get(storeId);
        lock.writeLock().lock();
        try {
            repoProviders.invalidate(storeId);
            URI rootReposURI = connectOrCreate(rootURI);
            repositoryProvider = new RepositoryStore(rootReposURI);

            CachedStore cachedStore = new CachedStore(rootURI, repositoryProvider);
            repoProviders.put(storeId, cachedStore);
        } finally {
            lock.writeLock().unlock();
        }

        return repositoryProvider;
    }

    private URI connectOrCreate(@NonNull String baseURI) throws RepositoryConnectionException {
        final URI uri = URI.create(baseURI);
        final String scheme = uri.getScheme();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(scheme),
                "base URI schem not provided, expected file:// or postgresql://, %s", baseURI);
        if ("file".equals(scheme)) {
            return fileConnectOrCreate(uri);
        } else if ("postgresql".equals(scheme)) {
            return postgresConnectOrCreate(uri);
        }
        throw new IllegalArgumentException(String.format(
                "Unknown URI scheme %s, expected file:// or postgresql://, %s", scheme, baseURI));
    }

    private URI fileConnectOrCreate(URI rootURI) throws RepositoryConnectionException {
        File dir = new File(rootURI);
        if (!dir.exists()) {
            File parent = dir.getParentFile();
            checkArgument(parent.exists(), "Parent directory does not exist");
            if (!dir.mkdir()) {
                throw new RepositoryConnectionException("Unable to create directory " + dir);
            }
        }
        rootURI = dir.toURI();
        return rootURI;
    }

    private URI postgresConnectOrCreate(URI rootURI) throws RepositoryConnectionException {
        final boolean isRootUri = true;
        try (ConfigDatabase c = RepositoryResolver.resolveConfigDatabase(rootURI, null,
                isRootUri)) {
            c.getAllGlobal();
            log.debug("Connected to postgres database {}", rootURI);
        } catch (Exception e) {
            String msg = e.getMessage();
            throw new IllegalArgumentException(msg, e);
        }
        return rootURI;
    }

    public void invalidate(@NonNull UUID id) {
        repoProviders.invalidate(id);
    }

}
