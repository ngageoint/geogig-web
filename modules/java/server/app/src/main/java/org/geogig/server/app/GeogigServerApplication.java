package org.geogig.server.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = {
        org.geogig.server.app.configuration.GeoGigWebApiConfig.class }, lazyInit = false)
public class GeogigServerApplication implements CommandLineRunner {

    @Override
    public void run(String... arg0) throws Exception {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
    }

    public static void main(String... args) throws Exception {
        new SpringApplication(GeogigServerApplication.class).run(args);
    }

    class ExitException extends RuntimeException implements ExitCodeGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }

    }
}
