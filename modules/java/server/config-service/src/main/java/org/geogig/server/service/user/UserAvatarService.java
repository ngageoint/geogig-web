package org.geogig.server.service.user;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.geogig.server.model.Avatar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("UserAvatarService")
public class UserAvatarService {

    /**
     * Using a loading cache take care of loading concurrent requests of the same image only once
     * for us
     */
    private LoadingCache<String, Avatar> cache;

    public @Autowired UserAvatarService(AvatarStore repository, GravatarService gravatarClient) {

        AvatarCacheLoader cacheLoader = new AvatarCacheLoader(repository, //
                (e) -> gravatarClient.getGravatar(e));

        cache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).softValues()
                .build(cacheLoader);

    }

    // @Async("externalServicesCallsExecutor")
    public CompletableFuture<byte[]> getRawByEmailAdress(@NonNull String emailAddress) {
        Avatar avatar = cache.getUnchecked(emailAddress);
        if (avatar.isEmpty()) {
            return CompletableFuture.failedFuture(new NoSuchElementException());
        }
        return CompletableFuture.completedFuture(avatar.getRawImage());
    }

    public CompletableFuture<BufferedImage> getImageByEmailAdress(@NonNull String emailAddress) {
        CompletableFuture<byte[]> avatar = getRawByEmailAdress(emailAddress);

        return avatar.thenApply((bytes) -> {
            BufferedImage image;
            try {
                image = ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return image;
        });
    }

    private static final class AvatarCacheLoader extends CacheLoader<String, Avatar> {

        private static final byte[] NO_DATA = {};

        private AvatarStore repository;

        private List<Function<String, CompletableFuture<byte[]>>> loadingServices;

        public @SafeVarargs AvatarCacheLoader(AvatarStore repository,
                Function<String, CompletableFuture<byte[]>>... loaders) {
            this.repository = repository;
            this.loadingServices = ImmutableList.copyOf(loaders);
        }

        public @Override Avatar load(@NonNull String emailAddress) {
            Preconditions.checkState(!loadingServices.isEmpty());

            Optional<Avatar> saved = repository.findById(emailAddress);
            if (saved.isPresent()) {
                log.debug("avatar for {} obtained from database", emailAddress);
                return saved.get();
            }

            CompletableFuture<byte[]> future;
            future = loadingServices.get(0).apply(emailAddress);
            for (int i = 1; i < loadingServices.size(); i++) {

                CompletableFuture<byte[]> loaderFuture = loadingServices.get(i).apply(emailAddress);

                future = future.handle((r, e) -> {
                    if (r == null) {
                        return loaderFuture.join();
                    }
                    loaderFuture.cancel(true);
                    return r;
                });
            }

            byte[] image;
            try {
                image = future.get();
            } catch (InterruptedException | ExecutionException e) {
                image = NO_DATA;
            }
            Avatar avatar = new Avatar(emailAddress, image);
            repository.save(avatar);
            return avatar;
        }

    }

}
