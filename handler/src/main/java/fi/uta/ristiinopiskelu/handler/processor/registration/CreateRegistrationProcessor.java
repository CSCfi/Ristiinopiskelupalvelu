package fi.uta.ristiinopiskelu.handler.processor.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.handler.service.result.EntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.registration.CreateRegistrationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RegistrationMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.ForwardedCreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationResponse;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreateRegistrationProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(CreateRegistrationProcessor.class);

    private RegistrationService registrationService;
    private OrganisationService organisationService;
    private StudentService studentService;
    private ObjectMapper objectMapper;
    private CreateRegistrationValidator validator;
    private ModelMapper modelMapper;
    private JmsMessageForwarder messageForwarder;

    @Autowired
    public CreateRegistrationProcessor(RegistrationService registrationService,
                                       OrganisationService organisationService,
                                       StudentService studentService,
                                       CreateRegistrationValidator validator,
                                       ObjectMapper objectMapper,
                                       ModelMapper modelMapper,
                                       JmsMessageForwarder messageForwarder) {
        this.registrationService = registrationService;
        this.organisationService = organisationService;
        this.studentService = studentService;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
        this.messageForwarder = messageForwarder;
    }

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Creates a registration",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Creates a registration"
            ),
            payloadType = CreateRegistrationRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Forwarded registration request",
            servers = {"production", "staging"},
            payloadType = ForwardedCreateRegistrationRequest.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        CreateRegistrationRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), CreateRegistrationRequest.class);
        logger.info("Handling registration request");

        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);

        validator.validateRequest(request, organisationId);

        OrganisationEntity organisation = organisationService.findById(request.getReceivingOrganisationTkCode())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, request.getReceivingOrganisationTkCode()));

        // Collect student warnings in the current request
        List<StudentWarning> studentWarnings = new ArrayList<>();

        if (!CollectionUtils.isEmpty(request.getWarnings())) {
            studentWarnings.addAll(request.getWarnings());
        }

        // Collect existing student warnings sent separately in student updates, if available
        List<StudentEntity> studentRecords = studentService.findByOidOrPersonId(request.getStudent().getOid(),
                        request.getStudent().getPersonId(), PageRequest.of(0, 1,
                                Sort.by(Sort.Direction.DESC, "timestamp")));

        if (!CollectionUtils.isEmpty(studentRecords)) {
            // get the warnings from the latest student update if still valid
            List<StudentWarning> existingWwarnings = studentRecords.get(0).getWarnings();
            if (!CollectionUtils.isEmpty(existingWwarnings)) {
                existingWwarnings = existingWwarnings.stream().filter(warning ->
                        // always valid, both dates null
                        (warning.getStartDate() == null && warning.getEndDate() == null) ||
                                // valid from start date with no end date
                                (DateUtils.isBeforeOrEqual(warning.getStartDate(), LocalDate.now()) && warning.getEndDate() == null) ||
                                // valid during specific date range
                                (DateUtils.isBeforeOrEqual(warning.getStartDate(), LocalDate.now()) &&
                                        DateUtils.isAfterOrEqual(warning.getEndDate(), LocalDate.now()))
                ).collect(Collectors.toList());

                studentWarnings.addAll(existingWwarnings);
            }
        }
        request.setWarnings(studentWarnings);

        // Persist the request. RegistrationRequest & RegistrationReply map nicely to entity :)
        RegistrationEntity registrationEntity = modelMapper.map(request, RegistrationEntity.class);
        registrationEntity.setReceivingDateTime(OffsetDateTime.now());

        List<? extends EntityModificationResult<?>> modificationResults = registrationService.create(registrationEntity);
        registrationEntity = (RegistrationEntity) modificationResults.get(0).getCurrent();

        // Add generated registration id for registration
        ForwardedCreateRegistrationRequest forwardedRequest = modelMapper.map(request, ForwardedCreateRegistrationRequest.class);
        forwardedRequest.setRegistrationRequestId(registrationEntity.getId());

        // send to correct organisations
        this.messageForwarder.forwardRequestToOrganisation(registrationEntity.getId(), forwardedRequest,
                MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST, correlationId, organisation,
                Collections.singletonMap("registrationRequestId", forwardedRequest.getRegistrationRequestId()));

        registrationEntity.setSendDateTime(OffsetDateTime.now());
        registrationService.update(registrationEntity);

        exchange.setMessage(new RegistrationMessage(exchange, MessageType.REGISTRATION_RESPONSE, correlationId, forwardedRequest.getRegistrationRequestId(),
                new RegistrationResponse(Status.OK, "Registration requests processed successfully and forwarded to recipient organisations",
                        forwardedRequest.getRegistrationRequestId())));
    }
}
