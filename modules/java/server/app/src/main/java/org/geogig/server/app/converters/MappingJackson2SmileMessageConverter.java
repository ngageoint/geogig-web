package org.geogig.server.app.converters;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class MappingJackson2SmileMessageConverter extends AbstractJackson2HttpMessageConverter {

    private static final MediaType SMILE_MEDIA_TYPE = MediaType
            .valueOf("application/x-jackson-smile");

    public MappingJackson2SmileMessageConverter() {
        super(new ObjectMapper(new SmileFactory()).registerModule(new JavaTimeModule()), //
                SMILE_MEDIA_TYPE);
    }

    // protected @Override void writeInternal(Object object, Type type,
    // HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
    //
    // MockHttpOutputMessage m = new MockHttpOutputMessage();
    // super.writeInternal(object, type, m);
    // byte[] body = m.getBodyAsBytes();
    // Object read = super.read(type, null, new MockHttpInputMessage(body));
    // boolean equal = Objects.equals(((MappingJacksonValue) object).getValue(), read);
    // Preconditions.checkState(equal);
    // outputMessage.getBody().write(body);
    // }
}
