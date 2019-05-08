package org.geogig.server.geogig;

import org.locationtech.geogig.di.GeogigModule;
import org.locationtech.geogig.di.HintsModule;
import org.locationtech.geogig.di.PluginsModule;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.impl.ContextBuilder;
import org.locationtech.geogig.repository.impl.PluginsContextBuilder.DefaultPlugins;
import org.springframework.context.ApplicationContext;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class ServerContextBuilder extends ContextBuilder {

    private final ApplicationContext appContext;

    public @Override Context build(Hints hints) {
        return Guice
                .createInjector(Modules.override(new GeogigModule(), new HintsModule(hints)).with(
                        new PluginsModule(), new DefaultPlugins(), new ServerModule(appContext)))
                .getInstance(org.locationtech.geogig.repository.Context.class);
    }

    private @RequiredArgsConstructor static class ServerModule extends AbstractModule {

        private final @NonNull ApplicationContext appContext;

        /**
         * 
         * @see com.google.inject.AbstractModule#configure()
         */
        @Override
        protected void configure() {
            bind(ApplicationContext.class).toInstance(appContext);
            bind(Context.class).to(ServerContext.class).in(Scopes.SINGLETON);
        }
    }
}
