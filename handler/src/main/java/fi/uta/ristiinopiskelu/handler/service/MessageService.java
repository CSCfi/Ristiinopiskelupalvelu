package fi.uta.ristiinopiskelu.handler.service;

public interface MessageService {


    void suspendMessageProcessing();

    void resumeMessageProcessing();

    boolean isMessageProcessingSuspended();
}
