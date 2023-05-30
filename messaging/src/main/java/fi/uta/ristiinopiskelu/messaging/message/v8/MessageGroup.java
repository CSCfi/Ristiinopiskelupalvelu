package fi.uta.ristiinopiskelu.messaging.message.v8;

import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reminder: The Camel routes are built according to the enum values here.
 */
public enum MessageGroup {

    COURSEUNIT(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_COURSEUNIT_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_COURSEUNIT_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.DELETE_COURSEUNIT_REQUEST
    ),
    STUDYMODULE(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_STUDYMODULE_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_STUDYMODULE_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.DELETE_STUDYMODULE_REQUEST
    ),
    REGISTRATION(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_REGISTRATION_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.REGISTRATION_REPLY_REQUEST
    ),
    NETWORK(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_NETWORK_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_NETWORK_REQUEST
    ),
    REALISATION(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_REALISATION_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_REALISATION_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.DELETE_REALISATION_REQUEST
    ),
    STUDYRECORD(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_STUDYRECORD_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.STUDYRECORD_REPLY_REQUEST
    ),
    STUDENT(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_STUDENT_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_STUDENT_REPLY_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_STUDENT_STUDYRIGHT_REQUEST,
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST
    ),
    ACKNOWLEDGEMENT(
            fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.ACKNOWLEDGEMENT
    );
    private final fi.uta.ristiinopiskelu.messaging.message.v8.MessageType[] messageTypes;

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
