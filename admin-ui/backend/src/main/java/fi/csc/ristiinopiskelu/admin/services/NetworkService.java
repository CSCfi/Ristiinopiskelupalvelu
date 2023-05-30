package fi.csc.ristiinopiskelu.admin.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.csc.ristiinopiskelu.admin.security.AppUserDetails;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.csc.ristiinopiskelu.admin.exception.MessageSendingFailedException;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.*;
import fi.uta.ristiinopiskelu.messaging.message.current.network.CreateNetworkRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.network.UpdateNetworkRequest;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class NetworkService {
    private static Logger logger = LoggerFactory.getLogger(NetworkService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private ModelMapper modelMapper;

    public List<NetworkReadDTO> findAll(AppUserDetails user) {
        List<NetworkEntity> networks;

        if(user.isSuperUser()) {
            networks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        } else {
            networks = networkRepository.findAllNetworksByOrganisationId(user.getOrganisation(), Pageable.unpaged());
        }

        return networks.stream().map(network -> modelMapper.map(network, NetworkReadDTO.class)).collect(Collectors.toList());
    }

    public Optional<NetworkReadDTO> findById(String networkId, AppUserDetails user) {
        Optional<NetworkReadDTO> network;

        if(user.isSuperUser()) {
            network = networkRepository.findById(networkId)
                .map(networkEntity -> Optional.ofNullable(modelMapper.map(networkEntity, NetworkReadDTO.class))).orElse(null);
        } else {
            network = networkRepository.findNetworkByOrganisationIdAndNetworkId(user.getOrganisation(), networkId)
                .map(networkEntity -> Optional.ofNullable(modelMapper.map(networkEntity, NetworkReadDTO.class))).orElse(null);
        }

        return network;
    }

    public void create(NetworkWriteDTO network, AppUserDetails user) {
        CreateNetworkRequest req = new CreateNetworkRequest();
        req.setNetwork(network);

        Message responseMessage = sendAndReceiveJmsMessage(req, MessageType.CREATE_NETWORK_REQUEST, user.getEppn());
        verifyMessageSuccess(responseMessage, MessageType.CREATE_NETWORK_REQUEST);
    }

    public void update(NetworkWriteDTO network, AppUserDetails user) {
        NetworkEntity original = networkRepository.findById(network.getId()).orElse(null);

        if (original == null) {
            throw new MessageSendingFailedException("Network not found.");
        } else if (!network.isPublished() && original.isPublished()) {
            throw new MessageSendingFailedException("Published networks can not be made unpublished.");
        }

        UpdateNetworkRequest req = new UpdateNetworkRequest();
        req.setNetwork(network);

        Message responseMessage = sendAndReceiveJmsMessage(req, MessageType.UPDATE_NETWORK_REQUEST, user.getEppn());
        verifyMessageSuccess(responseMessage, MessageType.UPDATE_NETWORK_REQUEST);
    }

    public void deleteById(String networkId) {
        NetworkEntity network = networkRepository.findById(networkId).orElse(null);
        if (network.isPublished()) {
            throw new MessageSendingFailedException("Published networks can not be deleted.");
        } else {
            networkRepository.deleteById(networkId);
        }
    }

    public List<NetworkReadDTO> findNetworksByName(String name, String lang, AppUserDetails user) {
        return networkRepository.findAllNetworksByOrganisationIdAndNetworkNameByLanguage(user.getOrganisation(), name, lang, Pageable.unpaged()).stream()
            .map(network -> modelMapper.map(network, NetworkReadDTO.class)).collect(Collectors.toList());
    }

    private Message sendAndReceiveJmsMessage(AbstractRequest request, MessageType messageType, String eppn) {
        String json;

        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing request to JSON", e);
            throw new MessageSendingFailedException("Error while serializing request to JSON", e);
        }

        return jmsTemplate.sendAndReceive("handler", session -> {
            Message message = session.createTextMessage(json);
            message.setStringProperty(MessageHeader.MESSAGE_TYPE, messageType.name());
            message.setStringProperty(MessageHeader.EPPN, eppn);
            return message;
        });
    }

    private void verifyMessageSuccess(Message message, MessageType messageType) {
        if(message == null) {
            throw new MessageSendingFailedException("Did not receive any response message.");
        }

        Object response;

        try {
            response = jmsTemplate.getMessageConverter().fromMessage(message);
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to convert JMS message", e);
        }
        
        if(response instanceof DefaultResponse) {
            if(((DefaultResponse) response).getStatus() != Status.OK) {
                throw new MessageSendingFailedException("Message " + messageType.name() + " handling failed. " +
                        "Error: " + ((DefaultResponse) response).getMessage());
            }
        } else if(response instanceof JsonValidationFailedResponse) {
            throw new MessageSendingFailedException("Message " + messageType.name() + " handling failed. " +
                    "Message data did not pass json validation. Errors: "
                    + String.join(",\n", ((JsonValidationFailedResponse) response).getErrors()));
        } else {
            throw new MessageSendingFailedException("Received unexpected response message. [" + message.getClass() + "]");
        }
    }
}
