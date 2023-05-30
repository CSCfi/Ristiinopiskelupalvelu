package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.handler.processor.studyrecord.CreateStudyRecordProcessor;
import fi.uta.ristiinopiskelu.handler.processor.studyrecord.StudyRecordReplyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StudyRecordRoute extends AbstractRoute {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordRoute.class);

    @Value("${general.camel.route.studyrecord.create}")
    private String createStudyRecordRoute;

    @Value("${general.camel.route.studyrecord.reply}")
    private String studyRecordReplyRoute;

    @Value("${general.camel.route.studyrecord.max-threads:1}")
    private int maxThreads;

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    @Autowired
    private CreateStudyRecordProcessor createStudyRecordProcessor;

    @Autowired
    private StudyRecordReplyProcessor studyRecordReplyProcessor;

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
                new RouteConfiguration(createStudyRecordRoute,
                        "createStudyRecordRequest.json",
                        MessageType.CREATE_STUDYRECORD_REQUEST,
                        MessageType.STUDYRECORD_RESPONSE,
                        createStudyRecordProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(
                        studyRecordReplyRoute,
                        "studyRecordReplyRequest.json",
                        MessageType.STUDYRECORD_REPLY_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                    studyRecordReplyProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads())
        );
    }

    @Override
    protected String getQueueUri() {
        return MessageGroup.STUDYRECORD.getQueueName();
    }
}
