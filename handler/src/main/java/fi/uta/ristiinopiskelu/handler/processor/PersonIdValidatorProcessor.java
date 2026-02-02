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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.stream.Stream;

@Slf4j
@Component
public class PersonIdValidatorProcessor implements Processor {

    @Autowired
    private ObjectMapper objectMapper;

    @Getter
    @Setter
    @Value("${general.only-test-persons-allowed:false}")
    private boolean onlyTestPersonsAllowed;

    @Getter
    @Setter
    @Value("${general.oid.validate-checksum:true}")
    private boolean oidValidateChecksum;

    @Override
    public void process(Exchange exchange) throws Exception {
        MessageType messageType = MessageHeaderUtils.getMessageType(exchange.getIn().getHeaders());
        String messageBody = exchange.getIn().getBody(String.class);

        if(AbstractPersonIdentifiableRequest.class.isAssignableFrom(messageType.getClazz())) {
            AbstractPersonIdentifiableRequest request = (AbstractPersonIdentifiableRequest) objectMapper.readValue(
                messageBody, messageType.getClazz());

            PersonSsnValidationResult personIdResult = PersonIdentifierValidator.validateSsn(request.getPersonId());
            PersonOidValidationResult personOidResult = PersonIdentifierValidator.validateOid(request.getPersonOid(),
                oidValidateChecksum);

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
