package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.Message;
import org.apache.camel.Exchange;
import org.springframework.util.Assert;

public class StudyRecordMessage extends RistiinopiskeluMessage {

    public StudyRecordMessage(Exchange exchange, MessageType messageType, String correlationId, String studyRecordRequestId,
                              Message messageBody) {
        super(exchange, messageType, correlationId, messageBody);
        Assert.hasText(studyRecordRequestId, "studyRecordRequestId cannot be empty");
        setHeader("studyRecordRequestId", studyRecordRequestId);
    }
}
