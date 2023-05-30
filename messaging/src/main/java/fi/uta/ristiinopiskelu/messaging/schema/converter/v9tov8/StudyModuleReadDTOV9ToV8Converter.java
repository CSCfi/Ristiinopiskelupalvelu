package fi.uta.ristiinopiskelu.messaging.schema.converter.v9tov8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.schema.converter.AbstractObjectMappingObjectConverter;
import org.springframework.stereotype.Component;

@Component
public class StudyModuleReadDTOV9ToV8Converter extends AbstractObjectMappingObjectConverter<StudyModuleReadDTO,
    fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO> {

    @Override
    public Class<StudyModuleReadDTO> getSourceType() {
        return StudyModuleReadDTO.class;
    }

    @Override
    public Class<fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO> getTargetType() {
        return fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO.class;
    }

    @Override
    public fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO convertObject(Object object) throws ObjectConversionException {
        TreeNode json = getObjectMapper().valueToTree(object);
        // Do conversion stuff here if necessary at some point

        try {
            return getObjectMapper().treeToValue(json, getTargetType());
        } catch (JsonProcessingException e) {
            throw new ObjectConversionException("Error while converting StudyModuleReadDTO from v9 to v8", e);
        }
    }
}
