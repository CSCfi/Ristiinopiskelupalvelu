package fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.CreateCourseUnitValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CreateStudyModuleValidatorTest {

    @MockBean
    private NetworkService networkService;

    @MockBean
    private Validator beanValidator;

    @MockBean
    private CreateCourseUnitValidator createCourseUnitValidator;

    private CreateStudyModuleValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(CreateStudyModuleValidatorTest.class);

    @BeforeEach
    public void before() {
        validator = spy(new CreateStudyModuleValidator(new ArrayList<>(), networkService, beanValidator, createCourseUnitValidator));
    }

    @Test
    public void testValidate_shouldSuccess() {
        StudyModuleWriteDTO studyModule = new StudyModuleWriteDTO();
        studyModule.setStudyElementId("SM-1");
        studyModule.setStudyElementIdentifierCode("SMCODE-1");

        doNothing().when(validator).callSuperValidateObject(anyList(), any());

        validator.validateObject(Collections.singletonList(studyModule), "TUNI");

        verify(validator, times(1)).callSuperValidateObject(anyList(), any());
    }

    @Test
    public void testValidate_shouldThrowMissingMessageHeaderException() {
        StudyModuleWriteDTO studyModule = new StudyModuleWriteDTO();
        studyModule.setStudyElementId("SM-1");
        studyModule.setStudyElementIdentifierCode("SMCODE-1");

        assertThrows(MissingMessageHeaderException.class, () -> validator.validateObject(Collections.singletonList(studyModule), null));
        verify(validator, times(0)).callSuperValidateObject(anyList(), any());
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyException() {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateObject(Collections.emptyList(), "TUNI"));
        verify(validator, times(0)).callSuperValidateObject(anyList(), any());
    }
}
