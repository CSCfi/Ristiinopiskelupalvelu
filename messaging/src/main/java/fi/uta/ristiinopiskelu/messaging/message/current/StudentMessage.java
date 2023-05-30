package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.Message;
import org.apache.camel.Exchange;
import org.springframework.util.Assert;

public class StudentMessage extends RistiinopiskeluMessage {

    public StudentMessage(Exchange exchange, MessageType messageType, String correlationId, String studentRequestId,
                          Message messageBody) {
        super(exchange, messageType, correlationId, messageBody);
        Assert.hasText(studentRequestId, "studentRequestId cannot be empty");
        setHeader("studentRequestId", studentRequestId);
    }
}
