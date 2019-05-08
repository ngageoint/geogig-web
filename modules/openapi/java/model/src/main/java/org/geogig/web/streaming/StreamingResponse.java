package org.geogig.web.streaming;

/**
 * Interface to be implemented by client response objects that process the response body in a
 * streaming fashion, in order to close any needed resource (e.g. http URL connection) once the
 * response is consumed.
 * <p>
 */
public interface StreamingResponse {

    public void onClose(Runnable additionalCloseTask);
}
