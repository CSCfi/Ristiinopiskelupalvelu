package fi.uta.ristiinopiskelu.handler.validator.person;

import fi.uta.ristiinopiskelu.messaging.util.Oid;
import org.springframework.util.StringUtils;

public class PersonOidValidationResult implements PersonIdentifierValidationResult {

    private final String oid;

    public PersonOidValidationResult(String oid) {
        this.oid = oid;
    }

    @Override
    public boolean isTestPerson() {
        return isValid() && Oid.hasNode(oid, Oid.TEST_PERSON_NODE_ID);
    }
    
    @Override
    public boolean hasValue() {
        return StringUtils.hasText(oid);
    }

    @Override
    public boolean isValid() {
        return Oid.isValid(oid);
    }
}
