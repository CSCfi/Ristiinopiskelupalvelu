package fi.uta.ristiinopiskelu.handler.processor.student;

import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentRequest;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateStudentProcessor extends AbstractStudentProcessor {

    @Autowired
    public UpdateStudentProcessor(JmsMessageForwarder jmsMessageForwarder) {
        super(jmsMessageForwarder);
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        throw new UnsupportedOperationException("This operation has been removed");
    }
}
