package fi.uta.ristiinopiskelu.handler.processor.student;

import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.student.StudentMessageTypeEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.NoForwardingOrganisationsValidationException;
import fi.uta.ristiinopiskelu.handler.exception.validation.OrganizingOrganisationMismatchValidationException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.handler.service.result.EntityModificationResult;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.StudentMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentStudyRightRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.StudentResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentStudyRightRequest;
import org.apache.camel.Exchange;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class UpdateStudentStudyRightProcessor extends AbstractStudentProcessor {

    private RegistrationService registrationService;
    private StudentService studentService;
    private JsonMapper jsonMapper;

    @Autowired
    public UpdateStudentStudyRightProcessor(StudentService studentService, RegistrationService registrationService,
                                            JsonMapper jsonMapper, JmsMessageForwarder jmsMessageForwarder) {
        super(jmsMessageForwarder);
        this.registrationService = registrationService;
        this.studentService = studentService;
        this.jsonMapper = jsonMapper;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        UpdateStudentStudyRightRequest request = jsonMapper.readValue(exchange.getIn().getBody(String.class), UpdateStudentStudyRightRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader(MessageHeader.JMS_MESSAGEID, String.class);

        if (!organisationId.equals(request.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference())) {
            throw new OrganizingOrganisationMismatchValidationException("Organisation sending the request must match home " +
                    "study right organizing organisation");
        }

        Map<OrganisationEntity, List<RegistrationEntity>> organisationsAndRegistrations = registrationService.findAllRegistrationsWithValidStudyRightPerOrganisation(
                request.getHomeStudyRight().getIdentifiers().getStudyRightId(), request.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());

        // If no registrations / organisations are found return failure message
        if (MapUtils.isEmpty(organisationsAndRegistrations)) {
            throw new NoForwardingOrganisationsValidationException(
                    "Stopped handling update student study right-message. No registrations found with " +
                            "[studyRightId: " + request.getHomeStudyRight().getIdentifiers().getStudyRightId() +
                            " organisationId: " + request.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference() +
                            " Message would not be forwarded to anyone so it is redundant.");
        }

        StudentEntity student = UpdateStudentStudyRightRequest.toStudentEntity(request);
        student.setId(null);
        student.setHomeOrganisationTkCode(organisationId);
        student.setTimestamp(OffsetDateTime.now());
        student.setMessageType(StudentMessageTypeEntity.UPDATE_STUDENT_STUDYRIGHT);

        List<? extends EntityModificationResult<?>> modificationResults = studentService.create(student);
        student = (StudentEntity) modificationResults.get(0).getCurrent();

        ForwardedUpdateStudentStudyRightRequest forwardedRequest = ForwardedUpdateStudentStudyRightRequest.from(request);
        forwardedRequest.setStudentRequestId(student.getId());

        // send to correct organisations and add host study rights
        super.forwardMessage(exchange, MessageType.FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REQUEST, organisationsAndRegistrations,
                forwardedRequest, Collections.singletonMap("studentRequestId", forwardedRequest.getStudentRequestId()));

        exchange.setMessage(new StudentMessage(exchange, MessageType.STUDENT_RESPONSE, correlationId, forwardedRequest.getStudentRequestId(),
                new StudentResponse(Status.OK, "Update student study right request processed successfully and forwarded to recipient organisations",
                        forwardedRequest.getStudentRequestId())));
    }
}
