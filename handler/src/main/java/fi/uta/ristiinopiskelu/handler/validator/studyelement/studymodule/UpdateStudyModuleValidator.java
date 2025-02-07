package fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.handler.service.impl.AbstractStudyElementService;
import fi.uta.ristiinopiskelu.handler.validator.JsonRequestValidator;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.AbstractStudyElementValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.validation.Validator;
import java.util.List;

@Component
public class UpdateStudyModuleValidator extends AbstractStudyElementValidator<StudyModuleWriteDTO> implements JsonRequestValidator<StudyModuleEntity> {

    private ObjectMapper objectMapper;
    private Validator beanValidator;

    @Autowired
    public UpdateStudyModuleValidator(List<AbstractStudyElementService> studyElementServices,
                                      NetworkService networkService,
                                      ObjectMapper objectMapper, Validator beanValidator) {
        super(studyElementServices, networkService, beanValidator);
        this.objectMapper = objectMapper;
        this.beanValidator = beanValidator;
    }

    @Override
    public StudyModuleEntity validateJson(JsonNode requestJson, String organisationId) {
        if(StringUtils.isEmpty(organisationId)) {
            throw new InvalidMessageHeaderException("Cannot perform update. Organisation Id is missing from header. This should not happen.");
        }

        if(requestJson == null || !requestJson.hasNonNull("studyModule")) {
            throw new InvalidMessageBodyException("Received update study module request -message without studyModule -object.");
        }

        JsonNode studyModuleJson = requestJson.get("studyModule");

        // If type is not given add it, since we know type is study module but jackson does not know it unless type is given
        if(!studyModuleJson.hasNonNull("type")) {
            ((ObjectNode)studyModuleJson).put("type", StudyElementType.STUDY_MODULE.name());
        }

        StudyModuleWriteDTO parsedStudyModule = objectMapper.convertValue(studyModuleJson, StudyModuleWriteDTO.class);
        this.beanValidator.validate(parsedStudyModule);

        StudyModuleEntity originalStudyModuleEntity = getServiceForClass(parsedStudyModule)
                .findByStudyElementIdAndOrganizingOrganisationId(
                        parsedStudyModule.getStudyElementId(), organisationId)
                .orElse(null);


        if(originalStudyModuleEntity == null) {
            throw new EntityNotFoundException("Unable to update study module. Study module with " + getStudyElementString(parsedStudyModule) + " does not exists");
        }

        if(studyModuleJson.hasNonNull("parents")) {
            validateParentReferences(parsedStudyModule.getParents());
        }

        if(studyModuleJson.has("organisationReferences")) {
            validateOrganisationReferences(parsedStudyModule, organisationId);
        }

        if(studyModuleJson.has("cooperationNetworks")) {
            validateGivenNetworks(parsedStudyModule, organisationId);
        }

        return originalStudyModuleEntity;
    }

    protected void validateParentReferences(List<StudyElementReference> parents) {
        super.validateParentReferences(parents);
    }

    protected void validateOrganisationReferences(StudyModuleWriteDTO studyModule, String organisationId) {
        super.validateOrganisationReferences(studyModule.getStudyElementId(), studyModule.getType(),
                studyModule.getOrganisationReferences(), organisationId);
    }

    protected void validateGivenNetworks(StudyModuleWriteDTO studyModule, String organisationId) {
        super.validateGivenNetworks(studyModule, organisationId, StudyElementType.COURSE_UNIT, false);
    }

    protected StudyModuleService getServiceForClass(StudyModuleWriteDTO studyModule) {
        return (StudyModuleService) super.getServiceForClass(studyModule);
    }
}
