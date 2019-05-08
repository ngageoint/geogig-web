package org.geogig.web.client;

/**
 * The origin server requires the request to be conditional. Intended to prevent the 'lost update'
 * problem, where a client GETs a resource's state, modifies it, and PUTs it back to the server,
 * when meanwhile a third party has modified the state on the server, leading to a conflict.
 */
public class PreconditionRequiredException extends RuntimeException {
    private static final long serialVersionUID = 6883316817420983842L;

    public PreconditionRequiredException(Throwable cause) {
        super(cause);
    }

    public PreconditionRequiredException(Throwable cause, String msgFormat, Object... msgArgs) {
        super(String.format(msgFormat, msgArgs), cause);
    }

}
