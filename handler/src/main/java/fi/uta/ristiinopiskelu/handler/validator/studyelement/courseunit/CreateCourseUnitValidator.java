package fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.impl.AbstractStudyElementService;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.AbstractStudyElementCreateValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.validation.Validator;
import java.util.List;

@Component
public class CreateCourseUnitValidator extends AbstractStudyElementCreateValidator<CourseUnitWriteDTO> {

    @Autowired
    public CreateCourseUnitValidator(List<AbstractStudyElementService> studyElementServices,
                                     NetworkService networkService, Validator beanValidator) {
        super(studyElementServices, networkService, beanValidator);
    }
    
    @Override
    public void validateObject(List<CourseUnitWriteDTO> courseUnits, String organisationId) {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }
        
        if(CollectionUtils.isEmpty(courseUnits)) {
            throw new InvalidMessageBodyException("Received create course unit request -message without any course units.");
        }

        callSuperValidateObject(courseUnits, organisationId);
    }

    protected void callSuperValidateObject(List<CourseUnitWriteDTO> courseUnits, String organisationId) {
        super.validateObject(courseUnits, organisationId);
    }
}
