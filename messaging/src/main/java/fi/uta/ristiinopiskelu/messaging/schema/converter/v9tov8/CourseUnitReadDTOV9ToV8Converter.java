package fi.uta.ristiinopiskelu.messaging.schema.converter.v9tov8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.schema.converter.AbstractObjectMappingObjectConverter;
import org.springframework.stereotype.Component;

@Component
public class CourseUnitReadDTOV9ToV8Converter extends AbstractObjectMappingObjectConverter<CourseUnitReadDTO,
    fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO> {

    @Override
    public Class<CourseUnitReadDTO> getSourceType() {
        return CourseUnitReadDTO.class;
    }

    @Override
    public Class<fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO> getTargetType() {
        return fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO.class;
    }

    @Override
    public fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO convertObject(Object object) throws ObjectConversionException {
        TreeNode json = getObjectMapper().valueToTree(object);
        // Do conversion stuff here if necessary at some point

        try {
            return getObjectMapper().treeToValue(json, getTargetType());
        } catch (JsonProcessingException e) {
            throw new ObjectConversionException("Error while converting CourseUnitReadDTO from v9 to v8", e);
        }
    }
}
