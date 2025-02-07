package fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CreateCourseUnitValidatorTest {

    @MockBean
    private NetworkService networkService;

    @MockBean
    private Validator beanValidator;

    private CreateCourseUnitValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(CreateCourseUnitValidatorTest.class);

    @BeforeEach
    public void before() {
        validator = spy(new CreateCourseUnitValidator(new ArrayList<>(), networkService, beanValidator));
    }

    @Test
    public void testValidate_shouldSuccess() {
        CourseUnitWriteDTO courseUnit = new CourseUnitWriteDTO();
        courseUnit.setStudyElementId("CU-1");
        courseUnit.setStudyElementIdentifierCode("CUCODE-1");

        doNothing().when(validator).callSuperValidateObject(anyList(), any());
        validator.validateObject(Collections.singletonList(courseUnit), "TUNI");
        verify(validator, times(1)).callSuperValidateObject(anyList(), any());
    }
    
    @Test
    public void testValidate_shouldThrowMissingMessageHeaderException() {
        CourseUnitWriteDTO courseUnit = new CourseUnitWriteDTO();
        courseUnit.setStudyElementId("CU-1");
        courseUnit.setStudyElementIdentifierCode("CUCODE-1");

        assertThrows(MissingMessageHeaderException.class, () -> validator.validateObject(Collections.singletonList(courseUnit), null));
        verify(validator, times(0)).callSuperValidateObject(anyList(), any());
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyException() {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateObject(Collections.emptyList(), "TUNI"));
        verify(validator, times(0)).callSuperValidateObject(anyList(), any());
    }
}
