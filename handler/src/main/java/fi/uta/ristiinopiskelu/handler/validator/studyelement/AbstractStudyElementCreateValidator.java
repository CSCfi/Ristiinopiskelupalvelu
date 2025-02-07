package fi.uta.ristiinopiskelu.handler.validator.studyelement;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.impl.AbstractStudyElementService;
import fi.uta.ristiinopiskelu.handler.utils.KeyHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.validation.Validator;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public abstract class AbstractStudyElementCreateValidator<T extends AbstractStudyElementWriteDTO> extends AbstractStudyElementValidator<T> {

    public AbstractStudyElementCreateValidator(List<AbstractStudyElementService> studyElementServices, NetworkService networkService, Validator beanValidator) {
        super(studyElementServices, networkService, beanValidator);
    }

    @Override
    public void validateObject(T object, String organisationId) throws ValidationException {
        this.validateObject(Collections.singletonList(object), organisationId);
    }

    @Override
    public void validateObject(List<T> studyElements, String organisationId) throws ValidationException {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }

        if(CollectionUtils.isEmpty(studyElements)) {
            throw new InvalidMessageBodyException("Received create message without any elements.");
        }

        HashSet<KeyHelper> duplicateTest = new HashSet<>();
        for(T studyElement : studyElements) {
            super.validateObject(studyElement, organisationId);
            validateOrganisationReferences(studyElement.getStudyElementId(), studyElement.getType(),
                    studyElement.getOrganisationReferences(), organisationId);
            validateGivenNetworks(studyElement, organisationId, studyElement.getType(), true);
            validateNotDuplicate(duplicateTest, studyElement, organisationId);
            validateParentReferences(studyElement.getParents());
        }
    }
}
