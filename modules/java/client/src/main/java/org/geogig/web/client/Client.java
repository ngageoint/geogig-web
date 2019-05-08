package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.geogig.web.client.internal.ApiClient;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.AsyncApi;
import org.geogig.web.client.internal.BranchesApi;
import org.geogig.web.client.internal.FeatureServiceApi;
import org.geogig.web.client.internal.Pair;
import org.geogig.web.client.internal.PullRequestsApi;
import org.geogig.web.client.internal.RawRepositoryAccessApi;
import org.geogig.web.client.internal.RepositoryManagementApi;
import org.geogig.web.client.internal.RepositoryStoresApi;
import org.geogig.web.client.internal.ServiceInfoApi;
import org.geogig.web.client.internal.StringUtil;
import org.geogig.web.client.internal.TransactionManagementApi;
import org.geogig.web.client.internal.UsersApi;
import org.geogig.web.client.internal.auth.Authentication;
import org.geogig.web.client.jersey.FeatureCollectionMessageBodyProvider;
import org.geogig.web.client.jersey.FeatureMessageBodyProvider;
import org.geogig.web.client.jersey.GeometryMessageBodyProvider;
import org.geogig.web.model.AppMediaTypes;
import org.geogig.web.model.Version;
import org.geogig.web.model.VersionInfo;
import org.geogig.web.streaming.StreamingResponse;
import org.geogig.web.streaming.geojson.GeoGigGeoJsonJacksonModule;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.GZipEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

public class Client {

    final RepositoryStoresApi stores;

    final UsersApi users;

    final FeatureServiceApi features;

    final RawRepositoryAccessApi rawAccess;

    final TransactionManagementApi transactions;

    final RepositoryManagementApi repositories;

    final BranchesApi branches;

    final PullRequestsApi prs;

    final AsyncApi async;

    final GeogigApiClient apiClient;

    private Map<UUID, UUID> currentTransactionByRepo = new ConcurrentHashMap<>();

    public static enum FeatureStreamFormat {
        //@formatter:off
        GEOJSON(AppMediaTypes.GEOJSON),
        GEOJSON_BINARY(AppMediaTypes.GEOJSON_SMILE);
        //SIMPLIFIED_GEOJSON(AppMediaTypes.SIMPLIFIED_GEOJSON),
        //SIMPLIFIED_GEOJSON_BINARY(AppMediaTypes.SIMPLIFIED_GEOJSON_BINARY);
        //@formatter:on

        private final String mediaType;

        private FeatureStreamFormat(String mediaType) {
            this.mediaType = mediaType;
        }

        public String getMediaType() {
            return mediaType;
        }
    }

    /**
     * @param baseURL (e.g. {@code "http://localhost:8080/geogig/v2"}
     */
    public Client(@NonNull String baseURL) {
        if (baseURL.endsWith("/")) {
            /*
             * The server may reject URL's that are not normalized, and if the baseURL has a
             * trailing slash, the swagger generated client call will result in a double slash (e.g.
             * http://localhost:8181//users)
             */
            baseURL = baseURL.substring(0, baseURL.length() - 1);
        }
        VersionInfo gigVersion = Version.get();
        String userAgent = String.format("GeoGig Java Client (%s@%s)", gigVersion.getVersion(),
                gigVersion.getCommitId());

        apiClient = new GeogigApiClient();
        apiClient.setBasePath(baseURL);
        apiClient.setUserAgent(userAgent);
        if (Boolean.getBoolean("geogig.web.client.disablegzip")) {
            System.err.println(
                    "Geogig web client: gzip compression disabled by geogig.web.client.disablegzip=true System property");
        } else {
            apiClient.addDefaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
        }
        if (Boolean.getBoolean("geogig.web.client.debug")) {
            apiClient.setDebugging(true);
        }

        stores = new RepositoryStoresApi(apiClient);
        users = new UsersApi(apiClient);
        features = new FeatureServiceApi(apiClient);
        transactions = new TransactionManagementApi(apiClient);
        repositories = new RepositoryManagementApi(apiClient);
        rawAccess = new RawRepositoryAccessApi(apiClient);
        async = new AsyncApi(apiClient);
        branches = new BranchesApi(apiClient);
        prs = new PullRequestsApi(apiClient);
    }

    protected Authentication getAuthentication(@NonNull String name) {
        return apiClient.getAuthentication(name);
    }

    public URL getBaseURL() {
        try {
            return new URL(apiClient.getBasePath());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public ObjectMapper getObjectMapper() {
        return apiClient.getJSON().getContext(null);
    }

    public void setDebugging(boolean debug) {
        apiClient.setDebugging(debug);
    }

    public void setPreferredFeatureStreamFormat(FeatureStreamFormat... format) {
        apiClient.setPreferredFeatureStreamFormat(format);
    }

    public Client setBasicAuth(@NonNull String user, @NonNull String password) {
        apiClient.setUsername(user);
        apiClient.setPassword(password);
        return this;
    }

    public User login(String user, String password) {
        setBasicAuth(user, password);
        return login();
    }

    public User login() {
        return users().getSelf();
    }

    public void dispose() {
        // override if need be
    }

    public org.geogig.web.model.VersionInfo getServerVersion() {
        try {
            return new ServiceInfoApi(apiClient).getVersion();
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public User getSelf() {
        return users().getSelf();
    }

    public StoresClient stores() {
        return new StoresClient(this);
    }

    public UsersClient users() {
        return new UsersClient(this);
    }

    public ReposClient repositories() {
        return new ReposClient(this);
    }

    public TransactionsClient transactions() {
        return new TransactionsClient(this);
    }

    public AsyncTaskClient async() {
        return new AsyncTaskClient(this);
    }

    Client currentTransaction(@NonNull UUID repositoryId, UUID transactionId) {
        this.currentTransactionByRepo.put(repositoryId, transactionId);
        return this;
    }

    Optional<UUID> currentTransaction(UUID repositoryId) {
        checkNotNull(repositoryId);
        UUID txId = currentTransactionByRepo.get(repositoryId);
        return Optional.ofNullable(txId);
    }

    private static class GeogigApiClient extends ApiClient {

        protected @Override void performAdditionalClientConfiguration(ClientConfig clientConfig) {
            // make it load our GeoGigModule from the classpath using the serviceloader
            // getJSON().getContext(Class.class).findAndRegisterModules();

            getJSON().getContext(Object.class).registerModule(GeoGigGeoJsonJacksonModule.INSTANCE);

            clientConfig.register(FeatureCollectionMessageBodyProvider.class,
                    MessageBodyReader.class, MessageBodyWriter.class);

            clientConfig.register(FeatureMessageBodyProvider.class, MessageBodyReader.class,
                    MessageBodyWriter.class);

            clientConfig.register(GeometryMessageBodyProvider.class, MessageBodyReader.class,
                    MessageBodyWriter.class);

            clientConfig.register(GZipEncoder.class);
        }

        private List<String> prefferredFeatureStreamFormats = new ArrayList<>();

        private static final Set<String> featureFormats = Arrays
                .asList(FeatureStreamFormat.values()).stream().map((f) -> f.getMediaType())
                .collect(Collectors.toSet());

        public void setPreferredFeatureStreamFormat(FeatureStreamFormat[] format) {
            prefferredFeatureStreamFormats = Arrays.asList(format).stream()
                    .map((f) -> f.getMediaType()).collect(Collectors.toList());
        }

        public @Override String selectHeaderAccept(String[] accepts) {
            if (accepts.length == 0) {
                return null;
            }
            final boolean isFeatureFormat = featureFormats.contains(accepts[0]);
            if (isFeatureFormat && !prefferredFeatureStreamFormats.isEmpty()) {
                // override
                String[] prefferredOrder = prefferredFeatureStreamFormats
                        .toArray(new String[prefferredFeatureStreamFormats.size()]);
                return StringUtil.join(prefferredOrder, ",");
            }
            return super.selectHeaderAccept(accepts);
        }

        /**
         * Cached value of returned JSESSIONID, saved on each response and added to the request as a
         * header, otherwise re-authenticating on each request adds ~100ms overhead due to BCrypt
         * encode and compare
         */
        private volatile Cookie JSESSIONID = null;

        /**
         * Overrides to implement handling of streaming responses in order to delegate the call to
         * {@link Response#close()} to the response object itself.
         * <p>
         * TODO: investigate if there's a prescribed way to do this instead.
         */
        public @Override <T> T invokeAPI(String path, String method, List<Pair> queryParams,
                Object body, Map<String, String> headerParams, Map<String, Object> formParams,
                String accept, String contentType, String[] authNames, GenericType<T> returnType)
                throws ApiException {
            updateParamsForAuth(authNames, queryParams, headerParams);

            // Not using `.target(this.basePath).path(path)` below,
            // to support (constant) query string in `path`, e.g. "/posts?draft=1"
            WebTarget target = httpClient.target(this.basePath + path);

            if (queryParams != null) {
                for (Pair queryParam : queryParams) {
                    if (queryParam.getValue() != null) {
                        target = target.queryParam(queryParam.getName(), queryParam.getValue());
                    }
                }
            }

            Invocation.Builder invocationBuilder = target.request().accept(accept);
            if (JSESSIONID != null) {
                // Adding directly as header, target.request().cookie(JSESSIONID) does not work
                headerParams.put("Cookie", JSESSIONID.toString());
            }
            for (Entry<String, String> entry : headerParams.entrySet()) {
                String value = entry.getValue();
                if (value != null) {
                    invocationBuilder = invocationBuilder.header(entry.getKey(), value);
                }
            }

            for (Entry<String, String> entry : defaultHeaderMap.entrySet()) {
                String key = entry.getKey();
                if (!headerParams.containsKey(key)) {
                    String value = entry.getValue();
                    if (value != null) {
                        invocationBuilder = invocationBuilder.header(key, value);
                    }
                }
            }

            Entity<?> entity = serialize(body, formParams, contentType);

            Response response = null;

            T result = null;
            try {
                if ("GET".equals(method)) {
                    response = invocationBuilder.get();
                } else if ("POST".equals(method)) {
                    response = invocationBuilder.post(entity);
                } else if ("PUT".equals(method)) {
                    response = invocationBuilder.put(entity);
                } else if ("DELETE".equals(method)) {
                    response = invocationBuilder.delete();
                } else if ("PATCH".equals(method)) {
                    response = invocationBuilder.method("PATCH", entity);
                } else if ("HEAD".equals(method)) {
                    response = invocationBuilder.head();
                } else {
                    throw new ApiException(500, "unknown method type " + method);
                }

                statusCode = response.getStatusInfo().getStatusCode();
                responseHeaders = buildResponseHeaders(response);

                if (response.getStatus() == Status.NO_CONTENT.getStatusCode()) {
                    return null;
                } else if (response.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL) {
                    Map<String, NewCookie> cookies = response.getCookies();
                    NewCookie sessionCookie = cookies.get("JSESSIONID");
                    if (sessionCookie != null) {
                        // save the session id, otherwise re-authenticating on each request adds
                        // ~100ms overhead due to BCrypt encode and compare
                        this.JSESSIONID = sessionCookie;
                    }
                    if (returnType != null) {
                        result = deserialize(response, returnType);
                    }
                } else {
                    String message = "error";
                    String respBody = null;
                    if (response.hasEntity()) {
                        try {
                            respBody = String.valueOf(response.readEntity(String.class));
                            message = respBody;
                        } catch (RuntimeException e) {
                            // e.printStackTrace();
                        }
                    }
                    throw new ApiException(response.getStatus(), message,
                            buildResponseHeaders(response), respBody);
                }
            } finally {
                if (result instanceof StreamingResponse) {
                    final Response closeMe = response;
                    ((StreamingResponse) result).onClose(() -> closeQuiet(closeMe));
                } else {
                    closeQuiet(response);
                }
            }
            return result;
        }

        private static void closeQuiet(Response response) {
            try {
                response.close();
            } catch (Exception e) {
                // it's not critical, since the response object is local in method
                // invokeAPI;
                // that's fine, just continue
            }
        }
    }

    public static RuntimeException propagate(@NonNull ApiException e) {
        final int statusCode = e.getCode();
        Map<String, List<String>> responseHeaders = e.getResponseHeaders();
        List<String> providedMsg = responseHeaders == null ? null
                : responseHeaders.getOrDefault("x-geogig-error-message", Collections.emptyList());
        String message = providedMsg == null || providedMsg.isEmpty() ? e.getMessage()
                : providedMsg.get(0);
        if (statusCode == 404) {
            NoSuchElementException ex = new NoSuchElementException(message);
            ex.initCause(e);
            throw ex;
        }
        if (statusCode == 400) {
            throw new IllegalArgumentException(message, e);
        }
        if (statusCode == 401) {
            throw new IllegalStateException("Authentication required. Server message: " + message,
                    e);
        }
        if (statusCode == 500) {
            throw new IllegalStateException("Server error: " + message, e);
        }

        // Conflict. Indicates that the request could not be processed
        // because of conflict in the current state of the resource, such as
        // an edit conflict between multiple simultaneous updates.
        if (statusCode == 409) {
            throw new ResourceStateConflictException(e, message);
        }

        if (statusCode == 428) {
            throw new PreconditionRequiredException(e, message);
        }

        throw new RuntimeException(message, e);
    }

    public static void propagateIfNot(@NonNull ApiException e, int statusCode) {
        if (e.getCode() != statusCode) {
            propagate(e);
        }
    }
}
