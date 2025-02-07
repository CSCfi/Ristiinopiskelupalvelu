package fi.uta.ristiinopiskelu.handler.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.handler.processor.AuditLoggingProcessor;
import fi.uta.ristiinopiskelu.handler.processor.MessageSchemaVersionCompatabilityProcessor;
import fi.uta.ristiinopiskelu.handler.processor.MessageSchemaVersionValidatorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.PersonIdValidatorProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.jsonvalidator.JsonValidationException;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ThreadsDefinition;
import org.apache.camel.model.TryDefinition;
import org.apache.commons.text.CaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;

public abstract class AbstractRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRoute.class);

    public static final int DEFAULT_MIN_THREADS = 1;
    public static final int DEFAULT_MAX_THREADS = 1;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLoggingProcessor auditLoggingProcessor;

    @Autowired
    private MessageSchemaVersionValidatorProcessor schemaVersionValidatorProcessor;

    @Autowired
    private MessageSchemaService schemaVersionService;

    @Autowired
    private MessageSchemaVersionCompatabilityProcessor schemaVersionCompatabilityProcessor;

    @Autowired
    private PersonIdValidatorProcessor personIdValidatorProcessor;

    protected abstract List<RouteConfiguration> getConfigs();

    protected abstract String getQueueUri();

    protected int getMinThreads() {
        return DEFAULT_MIN_THREADS;
    }

    protected int getMaxThreads() {
        return DEFAULT_MAX_THREADS;
    }

    protected String getThreadName() {
        return getClass().getSimpleName();
    }

    protected JacksonDataFormat getDataFormat(Class clazz) {
        return new JacksonDataFormat(objectMapper, clazz);
    }

    @Override
    public void configure() throws Exception {
        ThreadsDefinition def = from(getQueueUri()).threads(getMinThreads(), getMaxThreads()).threadName(getThreadName());
        buildChoices(def);
        buildRoutes();
    }

    protected void buildChoices(ThreadsDefinition def) {
        ChoiceDefinition choice = def.choice();

        this.getConfigs().stream().forEach(
                config -> choice.when(header(MessageHeader.MESSAGE_TYPE).isEqualTo(config.getRequestType().name()))
                        .to(config.getFrom()));

        choice.end();
    }

    protected void buildRoutes() {
        if(!CollectionUtils.isEmpty(this.getConfigs())) {
            this.getConfigs().forEach(this::buildRoute);
        }
    }

    protected void buildRoute(RouteConfiguration config) {
        TryDefinition tryDefinition =
            from(config.getFrom())
                .id(config.getRequestType().name())
                .threads(config.getMinThreads(), config.getMaxThreads()).threadName(config.getThreadName())
                .doTry()
                    .process(schemaVersionValidatorProcessor);

        // If route is backwards compatible add schema version compatibility processor to route
        if(config.isRouteBackwardsCompatible()) {
            tryDefinition
                .choice().when(header(MessageHeader.SCHEMA_VERSION).isNotEqualTo(String.valueOf(schemaVersionService.getCurrentSchemaVersion())))
                    .toD("json-validator:" + schemaVersionService.getSchemaVersionPath(
                        MessageType.getMessageGroup(config.getRequestType()), config.getMessageSchemaFilename(), "${header.%s}".formatted(MessageHeader.SCHEMA_VERSION)))
                    .process(schemaVersionCompatabilityProcessor)
                .endChoice();
        }

        tryDefinition
            .process(auditLoggingProcessor)
            .to("micrometer:timer:jsonValidator?action=start")
            .to("json-validator:" + schemaVersionService.getCurrentSchemaVersionPath(
                    MessageType.getMessageGroup(config.getRequestType()), config.getMessageSchemaFilename()))
            .to("micrometer:timer:jsonValidator?action=stop")
            .process(personIdValidatorProcessor)
            .to(String.format("micrometer:timer:%s?action=start", getProcessorClassName(config)))
            .process(config.getRequestProcessor())
            .to(String.format("micrometer:timer:%s?action=stop", getProcessorClassName(config)))
            .marshal(getDataFormat(config.getResponseType().getClazz()))
            .log("Successfully sent '" + config.getRequestType().name() + "' success response message: ${body}")
        .doCatch(JsonValidationException.class)
            .log(LoggingLevel.ERROR, config.getRequestType().name() + " -message handling failed\nEXCEPTION: ${exception.stacktrace}")
            .process(config.getJsonValidationExceptionProcessor())
            .marshal(getDataFormat(JsonValidationFailedResponse.class))
            .log("Successfully sent '"  + config.getRequestType().name() + "' failed response message: ${body}")
        .doCatch(Exception.class)
            .log(LoggingLevel.ERROR, config.getRequestType().name() + " -message handling failed\nEXCEPTION: ${exception.stacktrace}")
            .process(config.getExceptionProcessor())
            .marshal(getDataFormat(config.getResponseType().getClazz()))
            .log("Successfully sent '"  + config.getRequestType().name() + "' failed response message: ${body}")
        .end();
    }

    private String getProcessorClassName(RouteConfiguration config) {
        return CaseUtils.toCamelCase(config.getRequestProcessor().getClass().getSimpleName(), false, ' ');
    }
}
