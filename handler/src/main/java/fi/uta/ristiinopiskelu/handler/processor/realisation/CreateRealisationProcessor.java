package fi.uta.ristiinopiskelu.handler.processor.realisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.realisation.CreateRealisationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.CreateRealisationRequest;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import org.apache.camel.Exchange;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreateRealisationProcessor extends AbstractCompositeIdentifiedEntityProcessor<RealisationEntity> {
    private static final Logger logger = LoggerFactory.getLogger(CreateRealisationProcessor.class);

    private RealisationService realisationService;
    private CreateRealisationValidator realisationValidator;
    private ObjectMapper objectMapper;
    private ModelMapper modelMapper;

    @Autowired
    public CreateRealisationProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      RealisationService realisationService,
                                      CreateRealisationValidator realisationValidator,
                                      ObjectMapper objectMapper,
                                      ModelMapper modelMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.realisationValidator = realisationValidator;
        this.realisationService = realisationService;
        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
    }

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Creates a realisation",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Creates a realisation"
            ),
            payloadType = CreateRealisationRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Notification about changed elements",
            servers = {"production", "staging"},
            payloadType = CompositeIdentifiedEntityModifiedNotification.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        CreateRealisationRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), CreateRealisationRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        realisationValidator.validateRequest(request, organisationId);

        List<RealisationWriteDTO> realisations = request.getRealisations();
        List<RealisationEntity> realisationEntities = realisations.stream().map(cur -> modelMapper.map(cur, RealisationEntity.class))
                .collect(Collectors.toList());

        List<CompositeIdentifiedEntityModificationResult> modificationResults = realisationService.createAll(realisationEntities);
        super.notifyNetworkMembers(organisationId, MessageType.REALISATION_CREATED_NOTIFICATION, modificationResults);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Realisations created successfully")));
    }
}
