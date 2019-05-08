package org.geogig.server.app.gateway;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

public class AbstractService {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected HttpServletRequest currentRequest;

    public Optional<ObjectMapper> getObjectMapper() {
        return Optional.of(objectMapper);
    }

    public Optional<HttpServletRequest> getRequest() {
        return Optional.of(currentRequest);
    }

    protected <T> ResponseEntity<T> created(T entity) {
        return new ResponseEntity<>(entity, HttpStatus.CREATED);
    }

    protected <T> ResponseEntity<T> ok(T result) {
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    protected <T> ResponseEntity<T> ok(Supplier<T> op) {
        return run(HttpStatus.OK, op);
    }

    protected <T> ResponseEntity<T> okOrNotFound(Optional<T> entity) {
        return entity.map((s) -> ok(s)).orElse(notFound());
    }

    protected <T> ResponseEntity<T> notFound() {
        return error(HttpStatus.NOT_FOUND, "Not found");
    }

    protected <T> ResponseEntity<T> notFound(String msgFormat, Object... msgArgs) {
        return error(HttpStatus.NOT_FOUND, msgFormat, msgArgs);
    }

    protected <T> ResponseEntity<T> badRequest(String msgFormat, Object... msgArgs) {
        return error(HttpStatus.BAD_REQUEST, msgFormat, msgArgs);
    }

    protected <T> ResponseEntity<T> error(HttpStatus code, String messageFormat,
            Object... messageArguments) {

        String message = String.format(messageFormat, messageArguments);
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-geogig-error-message", message);
        return new ResponseEntity<>(headers, code);
    }

    protected <T> ResponseEntity<T> create(Supplier<T> op) {
        return run(HttpStatus.CREATED, op);
    }

    protected <T> ResponseEntity<T> run(HttpStatus successCode, Runnable voidOp) {
        try {
            voidOp.run();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(successCode);
    }

    protected <T> ResponseEntity<T> run(HttpStatus successCode, Supplier<T> op) {
        return run(successCode, op, (MultiValueMap<String, String>) null);
    }

    protected <T> ResponseEntity<T> run(HttpStatus successCode, Supplier<T> op,
            @Nullable Map<String, String> headers) {
        HttpHeaders httpheaders = toHeaders(headers);
        return run(successCode, op, httpheaders);
    }

    private HttpHeaders toHeaders(@Nullable Map<String, String> headers) {
        HttpHeaders h = null;
        if (headers != null) {
            final HttpHeaders httph = new HttpHeaders();
            headers.forEach((k, v) -> httph.add(k, v));
            h = httph;
        }
        return h;
    }

    protected <T> ResponseEntity<T> run(HttpStatus successCode, Supplier<T> op,
            @Nullable MultiValueMap<String, String> headers) {
        T tx;
        try {
            tx = op.get();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(tx, headers, successCode);
    }
}
