package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.Message;
import org.apache.camel.Exchange;
import org.springframework.util.Assert;

public class RegistrationMessage extends RistiinopiskeluMessage {

    public RegistrationMessage(Exchange exchange, MessageType messageType, String correlationId, String registrationRequestId,
                               Message messageBody) {
        super(exchange, messageType, correlationId, messageBody);
        Assert.hasText(registrationRequestId, "registrationRequestId cannot be empty");
        setHeader("registrationRequestId", registrationRequestId);
    }
}
