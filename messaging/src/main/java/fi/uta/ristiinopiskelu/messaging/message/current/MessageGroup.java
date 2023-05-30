package fi.uta.ristiinopiskelu.messaging.message.current;

import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reminder: The Camel routes are built according to the enum values here.
 */
public enum MessageGroup {
    
    COURSEUNIT(
            MessageType.CREATE_COURSEUNIT_REQUEST,
            MessageType.UPDATE_COURSEUNIT_REQUEST,
            MessageType.DELETE_COURSEUNIT_REQUEST
    ),
    STUDYMODULE(
            MessageType.CREATE_STUDYMODULE_REQUEST,
            MessageType.UPDATE_STUDYMODULE_REQUEST,
            MessageType.DELETE_STUDYMODULE_REQUEST
    ),
    REGISTRATION(
            MessageType.CREATE_REGISTRATION_REQUEST,
            MessageType.REGISTRATION_REPLY_REQUEST
    ),
    NETWORK(
            MessageType.CREATE_NETWORK_REQUEST,
            MessageType.UPDATE_NETWORK_REQUEST
    ),
    REALISATION(
            MessageType.CREATE_REALISATION_REQUEST,
            MessageType.UPDATE_REALISATION_REQUEST,
            MessageType.DELETE_REALISATION_REQUEST
    ),
    STUDYRECORD(
            MessageType.CREATE_STUDYRECORD_REQUEST,
            MessageType.STUDYRECORD_REPLY_REQUEST
    ),
    STUDENT(
            MessageType.UPDATE_STUDENT_REQUEST,
            MessageType.UPDATE_STUDENT_REPLY_REQUEST,
            MessageType.UPDATE_STUDENT_STUDYRIGHT_REQUEST,
            MessageType.UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST
    ),
    ACKNOWLEDGEMENT(
            MessageType.ACKNOWLEDGEMENT
    );
    private final MessageType[] messageTypes;

    MessageGroup(MessageType... messageTypes) {
        Assert.notEmpty(messageTypes, "MessageTypes cannot be empty");
        this.messageTypes = messageTypes;
    }

    public List<String> getMessageTypeNames() {
        return Arrays.stream(messageTypes).map(mt -> mt.name()).collect(Collectors.toList());
    }

    public boolean contains(String messageType){
        return this.getMessageTypeNames().stream().anyMatch(name -> (name.equals(messageType)));
    }

    public String getQueueName() {
        return "direct:" + this.name().toLowerCase();
    }
}
