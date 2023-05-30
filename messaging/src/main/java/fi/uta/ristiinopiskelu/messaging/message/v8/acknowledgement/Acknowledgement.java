package fi.uta.ristiinopiskelu.messaging.message.v8.acknowledgement;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

public class Acknowledgement extends AbstractRequest {
    String receivingOrganisationTkCode;

    MessageType messageType;

    String requestId;

    public Acknowledgement () {}

    public Acknowledgement(String receiverTkCode) {
        receivingOrganisationTkCode = receiverTkCode;
    }

    public String getReceivingOrganisationTkCode() {
        return receivingOrganisationTkCode;
    }

    public void setReceivingOrganisationTkCode(String receivingOrganisationTkCode) {
        this.receivingOrganisationTkCode = receivingOrganisationTkCode;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

}
