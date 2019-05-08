package org.geogig.server.events.model;

import java.util.Optional;

import org.geogig.server.model.AuthUser;

public interface Event {

    public Optional<AuthUser> getCaller();

    public void setCaller(Optional<AuthUser> caller);

    public interface CreatedEvent extends Event {
    };

    public interface UpdatedEvent extends Event {
    };

    public interface DeletedEvent extends Event {
    };
    //
    // @SuperBuilder
    // @EqualsAndHashCode(callSuper = true)
    // @Accessors(chain = true, fluent = true)
    // public static class Missing extends Event {
    // /**
    // * The object for which stats are missing when requested
    // */
    // private @NonNull Object source;
    // }
    //
    // @Data
    // @SuperBuilder
    // @EqualsAndHashCode(callSuper = true)
    // @Accessors(chain = true, fluent = true)
    // public static class Created<T> extends Event {
    // private @NonNull T target;
    // }
    //
    // @Data
    // @NoArgsConstructor
    // @EqualsAndHashCode(callSuper = true)
    // @Accessors(chain = true, fluent = true)
    // public static class Updated<T> extends Event {
    // private @NonNull T target;
    // }
    //
    // @Data
    // @NoArgsConstructor
    // @EqualsAndHashCode(callSuper = true)
    // @Accessors(chain = true, fluent = true)
    // public static class Deleted<T> extends Event {
    // private @NonNull T target;
    // }
}
