package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.studymodule.CreateStudyModuleProcessor;
import fi.uta.ristiinopiskelu.handler.processor.studymodule.DeleteStudyModuleProcessor;
import fi.uta.ristiinopiskelu.handler.processor.studymodule.UpdateStudyModuleProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StudyModuleRoute extends AbstractRoute {

    private static final Logger logger = LoggerFactory.getLogger(StudyModuleRoute.class);

    @Value("${general.camel.route.studymodule.create}")
    private String createStudyModuleRoute;

    @Value("${general.camel.route.studymodule.update}")
    private String updateStudyModuleRoute;

    @Value("${general.camel.route.studymodule.delete}")
    private String deleteStudyModuleRoute;

    @Value("${general.camel.route.studymodule.max-threads:1}")
    private int maxThreads;

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    @Autowired
    private CreateStudyModuleProcessor createStudyModuleProcessor;

    @Autowired
    private UpdateStudyModuleProcessor updateStudyModuleProcessor;

    @Autowired
    private DeleteStudyModuleProcessor deleteStudyModuleProcessor;

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
                new RouteConfiguration(createStudyModuleRoute,
                        "createStudyModuleRequest.json",
                        MessageType.CREATE_STUDYMODULE_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        createStudyModuleProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(updateStudyModuleRoute,
                        "updateStudyModuleRequest.json",
                        MessageType.UPDATE_STUDYMODULE_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        updateStudyModuleProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(deleteStudyModuleRoute,
                        "deleteStudyModuleRequest.json",
                        MessageType.DELETE_STUDYMODULE_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        deleteStudyModuleProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()));
    }

    @Override
    protected String getQueueUri() {
        return MessageGroup.STUDYMODULE.getQueueName();
    }
}
