package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.handler.service.MessageService;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private CamelContext camelContext;

    @Override
    public void suspendMessageProcessing() {
        camelContext.suspend();
    }

    @Override
    public void resumeMessageProcessing() {
        camelContext.resume();
    }

    @Override
    public boolean isMessageProcessingSuspended() {
        return camelContext.isSuspended() || camelContext.isSuspending();
    }
}
