package fi.uta.ristiinopiskelu.handler.processor.studyrecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.RoutingType;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyRecordService;
import fi.uta.ristiinopiskelu.handler.validator.studyrecord.CreateStudyRecordValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.StudyRecordMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.CreateStudyRecordRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.ForwardedCreateStudyRecordRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.StudyRecordResponse;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class CreateStudyRecordProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(CreateStudyRecordProcessor.class);

    private OrganisationService organisationService;
    private StudyRecordService studyRecordService;
    private ObjectMapper objectMapper;
    private ModelMapper modelMapper;
    private JmsMessageForwarder jmsMessageForwarder;
    private CreateStudyRecordValidator validator;

    public CreateStudyRecordProcessor(OrganisationService organisationService, StudyRecordService studyRecordService,
                                      ObjectMapper objectMapper, ModelMapper modelMapper, JmsMessageForwarder jmsMessageForwarder, CreateStudyRecordValidator validator) {
        this.organisationService = organisationService;
        this.studyRecordService = studyRecordService;
        this.objectMapper = objectMapper;
        this.jmsMessageForwarder = jmsMessageForwarder;
        this.validator = validator;
        this.modelMapper = modelMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        CreateStudyRecordRequest req = objectMapper.readValue(exchange.getIn().getBody(String.class), CreateStudyRecordRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);

        logger.info("Handling study record request");

        if(req.getRoutingType() == RoutingType.CROSS_STUDY) {
            // only cross studies need to be validated here
            validator.validateRequest(req, organisationId);
        }

        OrganisationEntity organisation = organisationService.findById(req.getReceivingOrganisation())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, req.getReceivingOrganisation()));

        // Persist the record
        StudyRecordEntity studyRecordEntity = studyRecordService.create(modelMapper.map(req, StudyRecordEntity.class));

        // Add generated id for study record
        ForwardedCreateStudyRecordRequest forwardedRequest = modelMapper.map(req, ForwardedCreateStudyRecordRequest.class);
        forwardedRequest.setStudyRecordRequestId(studyRecordEntity.getId());

        // send to correct organisations
        jmsMessageForwarder.forwardRequestToOrganisation(studyRecordEntity.getId(), forwardedRequest,
            MessageType.FORWARDED_CREATE_STUDYRECORD_REQUEST, correlationId, organisation,
            Collections.singletonMap("studyRecordRequestId", forwardedRequest.getStudyRecordRequestId()));

        exchange.setMessage(new StudyRecordMessage(exchange, MessageType.STUDYRECORD_RESPONSE, correlationId, forwardedRequest.getStudyRecordRequestId(),
                new StudyRecordResponse(Status.OK, "Study record requests processed successfully and forwarded to recipient organisations",
                        forwardedRequest.getStudyRecordRequestId())));
    }
}
