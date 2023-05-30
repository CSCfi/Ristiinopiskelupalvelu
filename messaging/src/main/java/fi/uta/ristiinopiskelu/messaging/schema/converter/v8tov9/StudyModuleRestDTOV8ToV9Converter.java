package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.schema.converter.AbstractObjectMappingObjectConverter;
import org.springframework.stereotype.Component;

@Component
public class StudyModuleRestDTOV8ToV9Converter extends AbstractObjectMappingObjectConverter<
    fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO, StudyModuleReadDTO> {

    @Override
    public Class<fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO> getSourceType() {
        return fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO.class;
    }

    @Override
    public Class<StudyModuleReadDTO> getTargetType() {
        return StudyModuleReadDTO.class;
    }

    @Override
    public StudyModuleReadDTO convertObject(Object object) throws ObjectConversionException {
        TreeNode json = getObjectMapper().valueToTree(object);
        // Do conversion stuff here if necessary at some point

        try {
            return getObjectMapper().treeToValue(json, getTargetType());
        } catch (JsonProcessingException e) {
            throw new ObjectConversionException("Error while converting StudyModuleRestDTO from v8 to v9", e);
        }
    }
}
