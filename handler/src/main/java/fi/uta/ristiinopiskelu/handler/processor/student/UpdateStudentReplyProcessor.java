package fi.uta.ristiinopiskelu.handler.processor.student;

import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentReplyRequest;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class UpdateStudentReplyProcessor implements Processor {

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Replies to a student update request",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Replies to a student update request"
            ),
            payloadType = UpdateStudentReplyRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Forwarded student update reply",
            servers = {"production", "staging"},
            payloadType = ForwardedUpdateStudentReplyRequest.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        throw new UnsupportedOperationException("This operation has been removed");
    }
}
