package fi.uta.ristiinopiskelu.handler.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidPersonIdException;
import fi.uta.ristiinopiskelu.handler.validator.person.PersonIdentifierValidationResult;
import fi.uta.ristiinopiskelu.handler.validator.person.PersonIdentifierValidator;
import fi.uta.ristiinopiskelu.handler.validator.person.PersonOidValidationResult;
import fi.uta.ristiinopiskelu.handler.validator.person.PersonSsnValidationResult;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractPersonIdentifiableRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.util.MessageHeaderUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.stream.Stream;

@Component
public class PersonIdValidatorProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(PersonIdValidatorProcessor.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${general.only-test-persons-allowed:false}")
    private boolean onlyTestPersonsAllowed;

    public boolean isOnlyTestPersonsAllowed() {
        return onlyTestPersonsAllowed;
    }

    public void setOnlyTestPersonsAllowed(boolean onlyTestPersonsAllowed) {
        this.onlyTestPersonsAllowed = onlyTestPersonsAllowed;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        MessageType messageType = MessageHeaderUtils.getMessageType(exchange.getIn().getHeaders());
        String messageBody = exchange.getIn().getBody(String.class);

        if(AbstractPersonIdentifiableRequest.class.isAssignableFrom(messageType.getClazz())) {
            AbstractPersonIdentifiableRequest request = (AbstractPersonIdentifiableRequest) objectMapper.readValue(messageBody, messageType.getClazz());

            PersonSsnValidationResult personIdResult = PersonIdentifierValidator.validateSsn(request.getPersonId());
            PersonOidValidationResult personOidResult = PersonIdentifierValidator.validateOid(request.getPersonOid());

            evaluateValidationResults(personIdResult, personOidResult);
        }
    }

    private void evaluateValidationResults(PersonIdentifierValidationResult... validationResults) {
        Assert.notEmpty(validationResults, "validationResults cannot be empty");

        Stream.of(validationResults).forEach(validationResult -> {
            if(validationResult.hasValue()) {
                if(!validationResult.isValid()) {
                    if(validationResult instanceof PersonSsnValidationResult) {
                        throw new InvalidPersonIdException("Person id is invalid");
                    } else {
                        throw new InvalidPersonIdException("Person oid is invalid");
                    }
                }

                if (onlyTestPersonsAllowed && !validationResult.isTestPerson()) {
                    throw new InvalidPersonIdException("Only test persons are allowed in this environment");
                }
            }
        });
    }
}
