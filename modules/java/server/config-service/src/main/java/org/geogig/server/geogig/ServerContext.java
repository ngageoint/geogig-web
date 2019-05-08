package org.geogig.server.geogig;

import java.util.HashMap;
import java.util.Map;

import org.locationtech.geogig.di.DelegatingContext;
import org.locationtech.geogig.di.GuiceContext;
import org.springframework.context.ApplicationContext;

import com.google.inject.Inject;

import lombok.Getter;
import lombok.NonNull;

public class ServerContext extends DelegatingContext {

    private final ApplicationContext appContext;

    private final @Getter Map<String, Object> userData = new HashMap<>();

    public @Inject ServerContext(@NonNull GuiceContext context,
            @NonNull ApplicationContext appContext) {
        super(context);
        this.appContext = appContext;
    }

    public <T> T bean(Class<T> type) {
        return appContext.getBean(type);
    }
}
