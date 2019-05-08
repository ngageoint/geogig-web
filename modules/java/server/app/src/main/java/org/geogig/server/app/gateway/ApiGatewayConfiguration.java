package org.geogig.server.app.gateway;

import org.geogig.server.app.converters.MappingJackson2SmileMessageConverter;
import org.geogig.server.app.converters.RevisionObjectTextMessageConverter;
import org.geogig.server.app.converters.geojson.FeatureCollectionHttpMessageConverter;
import org.geogig.server.app.converters.geojson.FeatureHttpMessageConverter;
import org.geogig.server.app.converters.geojson.SimplifiedGeoJSONConverter;
import org.geogig.server.app.converters.proto3.FeatureCollectionHttpMessageConverterProto3;
import org.geogig.web.streaming.geojson.GeoGigGeoJsonJacksonModule;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableAsync
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = { ApiGatewayConfiguration.class }, lazyInit = false)
public class ApiGatewayConfiguration {

    public @Bean WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            public @Override void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }
        };
    }

    @SuppressWarnings("unchecked")
    public @Bean Jackson2ObjectMapperBuilder objectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        // builder.findModulesViaServiceLoader(true);
        builder.dateFormat(new StdDateFormat());
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.modulesToInstall(GeoGigGeoJsonJacksonModule.class, JavaTimeModule.class);
        return builder;
    }

    public @Bean RevisionObjectTextMessageConverter revisionObjectTextConverter() {
        return new RevisionObjectTextMessageConverter();
    }

    public @Bean MappingJackson2SmileMessageConverter smileObjectConverter() {
        return new MappingJackson2SmileMessageConverter();
    }

    public @Bean HttpMessageConverter<org.geogig.web.model.FeatureCollection> featureCollectionGeoJSONConverter() {
        return FeatureCollectionHttpMessageConverter.geoJSON();
    }

    public @Bean HttpMessageConverter<org.geogig.web.model.FeatureCollection> featureCollectionGeoJSONSmileConverter() {
        return FeatureCollectionHttpMessageConverter.geoJSONSmile();
    }

    public @Bean HttpMessageConverter<org.geogig.web.model.FeatureCollection> FeatureCollectionHttpMessageConverterProto3() {
        return FeatureCollectionHttpMessageConverterProto3.geoFormatProto3();
    }

    public @Bean HttpMessageConverter<org.geogig.web.model.Feature> featureGeoJSONConverter() {
        return FeatureHttpMessageConverter.geoJSON();
    }

    public @Bean HttpMessageConverter<org.geogig.web.model.Feature> featureGeoJSONSmileConverter() {
        return FeatureHttpMessageConverter.geoJSONSmile();
    }

    public @Bean HttpMessageConverter<org.geogig.web.model.FeatureCollection> featuresSimplifiedJSONConverter() {
        return SimplifiedGeoJSONConverter.simplifiedGeoJSON();
    }

    public @Bean HttpMessageConverter<org.geogig.web.model.FeatureCollection> featuresSimplifiedJSONBinaryConverter() {
        return SimplifiedGeoJSONConverter.simplifiedGeoJSONSmile();
    }
}
