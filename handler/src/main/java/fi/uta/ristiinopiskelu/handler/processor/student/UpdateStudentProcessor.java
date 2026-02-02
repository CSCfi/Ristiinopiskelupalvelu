package fi.uta.ristiinopiskelu.handler.processor.student;

import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentRequest;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateStudentProcessor extends AbstractStudentProcessor {

    @Autowired
    public UpdateStudentProcessor(JmsMessageForwarder jmsMessageForwarder) {
        super(jmsMessageForwarder);
    }

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Updates a student",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Updates a student"
            ),
            payloadType = UpdateStudentRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Forwarded student update request",
            servers = {"production", "staging"},
            payloadType = ForwardedUpdateStudentRequest.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        throw new UnsupportedOperationException("This operation has been removed");
    }
}
