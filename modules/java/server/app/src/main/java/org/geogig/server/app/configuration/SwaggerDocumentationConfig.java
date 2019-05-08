package org.geogig.server.app.configuration;

import org.geogig.web.server.api.UsersApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerDocumentationConfig {

    ApiInfo apiInfo() {
        return new ApiInfoBuilder()//
                .title("GeoGig Server")//
                .description(
                        "GeoGig Web API v2.0. You can find out more about GeoGig at [geogig.org](http://geogig.org).")
                .license("Eclipse Distribution License - v 1.0")
                .licenseUrl("https://github.com/locationtech/geogig/blob/master/LICENSE.txt")
                .termsOfServiceUrl("")//
                .version("2.0.0")//
                .contact(new Contact("Gabriel Roldan", "", "groldan@boundlessgeo.com"))//
                .build();
    }

    @Bean
    public Docket customImplementation() {
        return new Docket(DocumentationType.SWAGGER_2)//
                .select()//
                .apis(RequestHandlerSelectors.basePackage(UsersApi.class.getPackage().getName()))//
                .build()//
                .directModelSubstitute(java.time.LocalDate.class, java.sql.Date.class)//
                .directModelSubstitute(java.time.OffsetDateTime.class, java.util.Date.class)//
                .useDefaultResponseMessages(false)//
                .apiInfo(apiInfo());
    }

}
