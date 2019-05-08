package org.geogig.server.app.configuration;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.service.ConfigServiceConfig;
import org.geogig.server.stats.StatsConfiguration;
import org.geogig.server.websockets.WebSocketPushEventsConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableAutoConfiguration
@ComponentScan(basePackages = { //
        "org.geogig.web.server.api", //
        "org.geogig.server.service", //
        "org.geogig.server.app.gateway" }, lazyInit = false)
@Import({ ConfigServiceConfig.class, StatsConfiguration.class, EventsConfiguration.class,
        WebSocketPushEventsConfiguration.class })
public class GeoGigWebApiConfig {

}
