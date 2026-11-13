package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.schema.converter.AbstractObjectMappingObjectConverter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TreeNode;

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
        TreeNode json = getJsonMapper().valueToTree(object);
        // Do conversion stuff here if necessary at some point

        try {
            return getJsonMapper().treeToValue(json, getTargetType());
        } catch (JacksonException e) {
            throw new ObjectConversionException("Error while converting StudyModuleRestDTO from v8 to v9", e);
        }
    }
}
