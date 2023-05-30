package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.AssessmentItemWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.StudyElementEntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.*;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.realisation.CreateRealisationValidator;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.CreateCourseUnitValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.CreateCourseUnitRequest;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CreateCourseUnitProcessor extends AbstractCompositeIdentifiedEntityProcessor<CourseUnitEntity> {

    private static final Logger logger = LoggerFactory.getLogger(CreateCourseUnitProcessor.class);

    private CourseUnitService courseUnitService;
    private StudyModuleService studyModuleService;
    private ObjectMapper objectMapper;
    private CreateCourseUnitValidator validator;
    private CreateRealisationValidator realisationValidator;

    @Autowired
    public CreateCourseUnitProcessor(CourseUnitService courseUnitService,
                                     StudyModuleService studyModuleService,
                                     NetworkService networkService,
                                     OrganisationService organisationService,
                                     JmsMessageForwarder jmsMessageForwarder,
                                     ObjectMapper objectMapper,
                                     CreateCourseUnitValidator validator,
                                     CreateRealisationValidator realisationValidator,
                                     MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.courseUnitService = courseUnitService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.realisationValidator = realisationValidator;
        this.studyModuleService = studyModuleService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        CreateCourseUnitRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), CreateCourseUnitRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        List<CourseUnitWriteDTO> courseUnits = request.getCourseUnits();
        validator.validateObject(courseUnits, organisationId);

        for (CourseUnitWriteDTO courseUnit : courseUnits) {
            try {
                if (!CollectionUtils.isEmpty(courseUnit.getRealisations())) {
                    realisationValidator.validateCreateCourseUnitRealisations(courseUnit.getRealisations(), organisationId, courseUnit);
                }

                for (AssessmentItemWriteDTO assessmentItem : courseUnit.getAssessmentItems()) {
                    if (!CollectionUtils.isEmpty(assessmentItem.getRealisations())) {
                        realisationValidator.validateCreateCourseUnitRealisations(assessmentItem.getRealisations(), organisationId, courseUnit);
                    }
                }

            } catch (Exception e) {
                // Wrap realisation validator exception again to add course unit ids for error message
                throw new ValidationException("Course unit's [studyElementId: " + courseUnit.getStudyElementId()
                    + ", organizer " + organisationId + "] Realisation validation failed with error: " + e.getMessage());
            }

            if (!CollectionUtils.isEmpty(courseUnit.getParents())) {
                for (StudyElementReference studyModuleReference : courseUnit.getParents()) {
                    studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                        studyModuleReference.getReferenceIdentifier(), studyModuleReference.getReferenceOrganizer())
                        .orElseThrow(() -> new StudyElementEntityNotFoundException("Course unit parent study module does not exist " +
                            "[studyElementId: " + studyModuleReference.getReferenceIdentifier() +
                            ", organizer: " + studyModuleReference.getReferenceOrganizer() + "]"));

                }
            }
        }

        // no subelements allowed for courseunit
        courseUnits.stream().forEach(cu -> cu.setSubElements(null));

        CompositeIdentifiedEntityModificationResult result = courseUnitService.createAll(courseUnits, organisationId);
        super.notifyNetworkMembers(organisationId, MessageType.COURSEUNIT_CREATED_NOTIFICATION, result);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
            new DefaultResponse(Status.OK, "Course unit creation successful " +
                "[createdCourseUnits=" + result.getCreatedAmount(CompositeIdentifiedEntityType.COURSE_UNIT) +
                ", createdRealisations=" + result.getCreatedAmount(CompositeIdentifiedEntityType.REALISATION) +
                ", updatedRealisations=" + result.getUpdatedAmount(CompositeIdentifiedEntityType.REALISATION) + "]")));
    }
}
