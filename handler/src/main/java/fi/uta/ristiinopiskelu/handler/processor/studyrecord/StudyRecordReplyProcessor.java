package fi.uta.ristiinopiskelu.handler.processor.studyrecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyRecordService;
import fi.uta.ristiinopiskelu.handler.validator.studyrecord.StudyRecordStatusValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.StudyRecordMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.ForwardedStudyRecordReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.StudyRecordReplyRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class StudyRecordReplyProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordReplyProcessor.class);

    private StudyRecordStatusValidator validator;
    private OrganisationService organisationService;
    private StudyRecordService studyRecordService;
    private JmsMessageForwarder jmsMessageForwarder;
    private ObjectMapper objectMapper;

    @Autowired
    public StudyRecordReplyProcessor(StudyRecordStatusValidator validator, OrganisationService organisationService,
                                     StudyRecordService studyRecordService, JmsMessageForwarder jmsMessageForwarder, ObjectMapper objectMapper) {
        this.validator = validator;
        this.organisationService = organisationService;
        this.studyRecordService = studyRecordService;
        this.jmsMessageForwarder = jmsMessageForwarder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        StudyRecordReplyRequest req = objectMapper.readValue(exchange.getIn().getBody(String.class), StudyRecordReplyRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);

        logger.info("Handling study record reply request {}", req.toString());

        validator.validateRequest(req, organisationId);

        Optional<StudyRecordEntity> studyRecordEntityOptional = studyRecordService.findById(req.getStudyRecordRequestId());

        if(!studyRecordEntityOptional.isPresent()) {
            throw new EntityNotFoundException(StudyRecordEntity.class, req.getStudyRecordRequestId());
        }

        StudyRecordEntity studyRecordEntity = studyRecordEntityOptional.get();

        Optional<OrganisationEntity> organisationEntityOptional = organisationService.findById(studyRecordEntity.getSendingOrganisation());
        if(!organisationEntityOptional.isPresent()) {
            throw new EntityNotFoundException(OrganisationEntity.class, studyRecordEntity.getSendingOrganisation());
        }

        OrganisationEntity organisation = organisationEntityOptional.get();

        // Persist the reply.
        studyRecordEntity.setStatus(req.getStatus());
        studyRecordEntity.setRejectionReason(req.getRejectionReason());
        studyRecordEntity = studyRecordService.update(studyRecordEntity);

        // send to correct organisations
        jmsMessageForwarder.forwardRequestToOrganisation(studyRecordEntity.getId(), new ForwardedStudyRecordReplyRequest(studyRecordEntity),
            MessageType.FORWARDED_STUDYRECORD_REPLY_REQUEST, correlationId, organisation,
            Collections.singletonMap("studyRecordRequestId", req.getStudyRecordRequestId()));

        exchange.setMessage(new StudyRecordMessage(exchange, MessageType.DEFAULT_RESPONSE, correlationId, req.getStudyRecordRequestId(),
                new DefaultResponse(Status.OK, "Study record reply processed successfully and forwarded to recipient organisations")));
    }
}
