package org.geogig.server.websockets;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.stats.StatsConfiguration;
import org.geogig.server.test.ConfigTestConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@SpringBootApplication
@EnableAutoConfiguration
@EnableAsync
@ComponentScan(lazyInit = true)
@Import({ WebSocketPushEventsConfiguration.class, ConfigTestConfiguration.class,
        EventsConfiguration.class, StatsConfiguration.class })
public class TestApplication implements CommandLineRunner {

    // if it's not referenced anywhere it won't be instantiated
//    private @Autowired UserPushEventsService service;

    @Override
    public void run(String... arg0) throws Exception {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
    }

    public static void main(String... args) throws Exception {
        new SpringApplication(TestApplication.class).run(args);
    }

    class ExitException extends RuntimeException implements ExitCodeGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }

    }
}
