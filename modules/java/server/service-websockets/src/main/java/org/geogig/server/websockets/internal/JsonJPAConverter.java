package org.geogig.server.websockets.internal;

import java.io.IOException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.events.EventsJSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

@Converter(autoApply = true)
public class JsonJPAConverter implements AttributeConverter<ServerEvent, byte[]> {

    private ObjectMapper mapper = EventsJSON.newObjectMapper();

    public @Override byte[] convertToDatabaseColumn(@NonNull ServerEvent attribute) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(attribute);
            return bytes;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public @Override ServerEvent convertToEntityAttribute(@NonNull byte[] dbData) {
        try {
            ServerEvent event = mapper.readValue(dbData, ServerEvent.class);
            return event;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
