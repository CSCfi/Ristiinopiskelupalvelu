package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.courseunit.CreateCourseUnitProcessor;
import fi.uta.ristiinopiskelu.handler.processor.courseunit.DeleteCourseUnitProcessor;
import fi.uta.ristiinopiskelu.handler.processor.courseunit.UpdateCourseUnitProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CourseUnitRoute extends AbstractRoute {

    private static final Logger logger = LoggerFactory.getLogger(CourseUnitRoute.class);

    @Value("${general.camel.route.courseunit.create}")
    private String createCourseUnitRoute;

    @Value("${general.camel.route.courseunit.update}")
    private String updateCourseUnitRoute;

    @Value("${general.camel.route.courseunit.delete}")
    private String deleteCourseUnitRoute;

    @Value("${general.camel.route.courseunit.max-threads:1}")
    private int maxThreads;

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    @Autowired
    private CreateCourseUnitProcessor createCourseUnitProcessor;

    @Autowired
    private UpdateCourseUnitProcessor updateCourseUnitProcessor;

    @Autowired
    private DeleteCourseUnitProcessor deleteCourseUnitProcessor;

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
                new RouteConfiguration(createCourseUnitRoute,
                        "createCourseUnitRequest.json",
                        MessageType.CREATE_COURSEUNIT_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        createCourseUnitProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                        true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(updateCourseUnitRoute,
                        "updateCourseUnitRequest.json",
                        MessageType.UPDATE_COURSEUNIT_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        updateCourseUnitProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(deleteCourseUnitRoute,
                        "deleteCourseUnitRequest.json",
                        MessageType.DELETE_COURSEUNIT_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        deleteCourseUnitProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()));
    }

    @Override
    protected String getQueueUri() {
        return MessageGroup.COURSEUNIT.getQueueName();
    }

}
