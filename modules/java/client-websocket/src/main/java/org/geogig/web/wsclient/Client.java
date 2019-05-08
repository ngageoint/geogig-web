package org.geogig.web.wsclient;

import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;

import org.geogig.web.client.User;
import org.geogig.web.client.internal.auth.Authentication;
import org.geogig.web.client.internal.auth.HttpBasicAuth;
import org.geogig.web.model.ServerEvent;

import com.google.common.eventbus.Subscribe;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * An extension of the REST {@link org.geogig.web.client.Client Client} that also handles websocket
 * events through {@link #addEventListener(Object) addEventListener}.
 */
@Slf4j
public class Client extends org.geogig.web.client.Client {

    private WSClient websocketClient;

    public Client(@NonNull String httpURL) {
        super(httpURL);
        final URL baseURL = super.getBaseURL();
        URI stompURL = toWebsocketURI(baseURL);
        this.websocketClient = new WSClient(stompURL, () -> getBasicAuth());
    }

    public @Override User login() {
        User user = super.login();
        websocketClient.connect();
        return user;
    }
    
    public @Override void dispose() {
        websocketClient.disconnect();
    }


    public <T extends ServerEvent> void addServerEventListener(@NonNull Class<T> eventType,
            @NonNull Consumer<T> consumer, boolean includingSubclasses) {
        websocketClient.addEventListener(eventType, consumer, includingSubclasses);
    }

    public <T extends ServerEvent> void addServerEventListener(@NonNull Class<T> eventType,
            @NonNull Consumer<T> consumer) {
        websocketClient.addEventListener(eventType, consumer, true);
    }

    public <T extends ServerEvent> void removeEventListener(@NonNull Class<T> eventType,
            @NonNull Consumer<T> consumer) {
        websocketClient.removeEventListener(eventType, consumer);
    }

    /**
     * Registers an event listener that will asynchronously be notified of server push events
     * 
     * @param an object that uses Guava's eventbus {@link Subscribe @Subscribe} to listen to
     *        specific event types
     */
    public void addServerEventListener(@NonNull Object listener) {
        websocketClient.addEventListener(listener);
    }

    public void removeServerEventListener(@NonNull Object listener) {
        websocketClient.removeEventListener(listener);
    }

    private HttpBasicAuth getBasicAuth() {
        Authentication auth = super.getAuthentication("BasicAuth");
        return auth instanceof HttpBasicAuth ? (HttpBasicAuth) auth : null;
    }

    private URI toWebsocketURI(@NonNull URL baseURL) {
        final String protocol = baseURL.getProtocol();
        String wsProtocol;
        if ("http".equals(protocol)) {
            wsProtocol = "ws";
        } else if ("https".equals(protocol)) {
            wsProtocol = "wss";
        } else {
            throw new IllegalArgumentException();
        }
        String host = baseURL.getHost();
        int port = baseURL.getPort();
        String path = baseURL.getPath();
        String wsBaseUrl;
        if (path.endsWith("/")) {
            path = path.substring(1);
        }
        if (path.isEmpty()) {
            wsBaseUrl = String.format("%s://%s:%d/ws", wsProtocol, host, port);
        } else {
            wsBaseUrl = String.format("%s://%s:%d/%s/ws", wsProtocol, host, port, path);
        }
        log.debug("Creating websockets client at URI {}", wsBaseUrl);
        return URI.create(wsBaseUrl);
    }
}
