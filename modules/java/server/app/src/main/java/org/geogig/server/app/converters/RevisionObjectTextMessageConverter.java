package org.geogig.server.app.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.geogig.server.service.presentation.GeogigObjectModelBridge;
import org.geogig.web.model.RevisionObject;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.storage.text.TextSerializationFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class RevisionObjectTextMessageConverter implements HttpMessageConverter<RevisionObject> {

    private static final TextSerializationFactory SERIALIZER = TextSerializationFactory.INSTANCE;

    @Override
    public void write(RevisionObject t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        OutputStream out = outputMessage.getBody();
        RevObject revObject = GeogigObjectModelBridge.map(t);

        SERIALIZER.write(revObject, out);
    }

    @Override
    public RevisionObject read(Class<? extends RevisionObject> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        InputStream body = inputMessage.getBody();
        RevObject read = SERIALIZER.read(null, body);

        RevisionObject revisionObject = GeogigObjectModelBridge.map(read);

        return revisionObject;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.TEXT_PLAIN);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        boolean canRead = RevisionObject.class.isAssignableFrom(clazz)
                && MediaType.TEXT_PLAIN.equals(mediaType);
        return canRead;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        boolean canWrite = canRead(clazz, mediaType);
        return canWrite;
    }

}
