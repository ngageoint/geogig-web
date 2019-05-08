package org.geogig.server.service.user;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import com.timgroup.jgravatar.GravatarRating;

import lombok.NonNull;

@Service("GravatarService")
public class GravatarService {
    private static final Logger log = LoggerFactory.getLogger(GravatarService.class);

    @Async("externalServicesCallsExecutor")
    public CompletableFuture<byte[]> getGravatar(@NonNull String emailAddress) {
        return getGravatar128px(emailAddress);
    }

    public CompletableFuture<byte[]> getGravatar128px(@NonNull String emailAddress) {

        Gravatar gravatar = new Gravatar();
        gravatar.setSize(128);
        gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
        gravatar.setDefaultImage(GravatarDefaultImage.HTTP_404);
        try {
            log.debug("Requesting gravatar for {}", emailAddress);
            byte[] jpg = gravatar.download(emailAddress);
            Objects.requireNonNull(jpg);
            log.debug("Obtained gravatar for {}", emailAddress);
            return CompletableFuture.completedFuture(jpg);
        } catch (Exception e) {
            log.debug("Unable to get gravatar for {}", emailAddress);
            return CompletableFuture.failedFuture(e);
        }
    }
}
