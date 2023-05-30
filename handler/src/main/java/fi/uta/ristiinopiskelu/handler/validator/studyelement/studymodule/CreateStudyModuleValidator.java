package fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.impl.AbstractStudyElementService;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.AbstractStudyElementCreateValidator;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.CreateCourseUnitValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.Validator;
import java.util.List;

@Component
public class CreateStudyModuleValidator extends AbstractStudyElementCreateValidator<StudyModuleWriteDTO> {

    private CreateCourseUnitValidator createCourseUnitValidator;

    @Autowired
    public CreateStudyModuleValidator(List<AbstractStudyElementService> studyElementServices,
                                      NetworkService networkService, Validator beanValidator, CreateCourseUnitValidator createCourseUnitValidator) {
        super(studyElementServices, networkService, beanValidator);
        this.createCourseUnitValidator = createCourseUnitValidator;
    }
    
    @Override
    public void validateObject(List<StudyModuleWriteDTO> studyModules, String organisationId) {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }
        
        if(CollectionUtils.isEmpty(studyModules)) {
            throw new InvalidMessageBodyException("Received create study module request -message without any study modules.");
        }

        this.validateSubElements(studyModules, organisationId);
        callSuperValidateObject(studyModules, organisationId);
    }

    protected void validateSubElements(List<StudyModuleWriteDTO> studyModules, String organisationId) {
        for (StudyModuleWriteDTO studyModule : studyModules) {
            List<AbstractStudyElementWriteDTO> subElements = studyModule.getSubElements();

            if(!CollectionUtils.isEmpty(subElements)) {
                for(AbstractStudyElementWriteDTO subElement : subElements) {
                    if (studyModule.getType() == StudyElementType.COURSE_UNIT) {
                        CourseUnitWriteDTO cu = (CourseUnitWriteDTO) subElement;
                        this.createCourseUnitValidator.validateObject(cu, organisationId);
                    } else if (subElement.getType() == StudyElementType.STUDY_MODULE) {
                        StudyModuleWriteDTO sm = (StudyModuleWriteDTO) subElement;
                        super.validateObject(sm, organisationId);
                    }
                }
            }
        }
    }

    protected void callSuperValidateObject(List<StudyModuleWriteDTO> studyModules, String organisationId) {
        super.validateObject(studyModules, organisationId);
    }
}
