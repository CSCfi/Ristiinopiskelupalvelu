package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.student.UpdateStudentStudyRightProcessor;
import fi.uta.ristiinopiskelu.handler.processor.student.UpdateStudentStudyRightReplyProcessor;
import fi.uta.ristiinopiskelu.handler.processor.student.UpdateStudentProcessor;
import fi.uta.ristiinopiskelu.handler.processor.student.UpdateStudentReplyProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StudentRoute extends AbstractRoute {

    @Value("${general.camel.route.student.update}")
    private String updateStudentRoute;

    @Value("${general.camel.route.student.update_reply}")
    private String updateStudentReplyRoute;

    @Value("${general.camel.route.student.update_studyright}")
    private String updateStudentStudyRightRoute;

    @Value("${general.camel.route.student.update_studyright_reply}")
    private String updateStudentStudyRightReplyRoute;

    @Value("${general.camel.route.student.max-threads:1}")
    private int maxThreads;

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    @Autowired
    private UpdateStudentProcessor updateStudentProcessor;

    @Autowired
    private UpdateStudentReplyProcessor updateStudentReplyProcessor;

    @Autowired
    private UpdateStudentStudyRightProcessor updateStudentStudyRightProcessor;

    @Autowired
    private UpdateStudentStudyRightReplyProcessor updateStudentStudyRightReplyProcessor;

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
                new RouteConfiguration(updateStudentRoute,
                        "updateStudentRequest.json",
                        MessageType.UPDATE_STUDENT_REQUEST,
                        MessageType.STUDENT_RESPONSE,
                        updateStudentProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(updateStudentReplyRoute,
                        "updateStudentReplyRequest.json",
                        MessageType.UPDATE_STUDENT_REPLY_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                    updateStudentReplyProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(updateStudentStudyRightRoute,
                        "updateStudentStudyRightRequest.json",
                        MessageType.UPDATE_STUDENT_STUDYRIGHT_REQUEST,
                        MessageType.STUDENT_RESPONSE,
                        updateStudentStudyRightProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       false,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(updateStudentStudyRightReplyRoute,
                        "updateStudentStudyRightReplyRequest.json",
                        MessageType.UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                    updateStudentStudyRightReplyProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       false,
                        getMinThreads(), getMaxThreads()));
    }

    @Override
    protected String getQueueUri() {
        return MessageGroup.STUDENT.getQueueName();
    }
}
