package org.geogig.web.client;

/**
 * Conflict. Indicates that the request could not be processed because of conflict in the current
 * state of the resource, such as an edit conflict between multiple simultaneous updates.
 */
public class ResourceStateConflictException extends RuntimeException {

    private static final long serialVersionUID = 5623726791023831740L;

    public ResourceStateConflictException(Throwable cause) {
        super(cause);
    }

    public ResourceStateConflictException(Throwable cause, String msgFormat, Object... msgArgs) {
        super(String.format(msgFormat, msgArgs), cause);
    }

}
