package fi.uta.ristiinopiskelu.handler.processor.student;

import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentReplyRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class UpdateStudentReplyProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        throw new UnsupportedOperationException("This operation has been removed");
    }
}
