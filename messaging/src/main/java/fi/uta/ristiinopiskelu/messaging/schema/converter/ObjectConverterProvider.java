package fi.uta.ristiinopiskelu.messaging.schema.converter;

import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObjectConverterProvider {

    private static final Logger logger = LoggerFactory.getLogger(ObjectConverterProvider.class);

    @Autowired
    private List<ObjectConverter> converters;

    @Autowired
    private JsonMapper jsonMapper;

    public <S, T> ObjectConverter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
        for(ObjectConverter converter : this.converters) {
            if(converter.getSourceType().equals(sourceType)
                    && converter.getTargetType().equals(targetType)) {
                return converter;
            }
        }

        logger.debug("No converter found for sourceType {} to targetType {}; using default converter", sourceType, targetType);
        return new DefaultObjectConverter(jsonMapper, sourceType, targetType);
    }
}
