package fi.uta.ristiinopiskelu.handler.validator.person;

import fi.uta.ristiinopiskelu.messaging.util.Oid;
import fi.uta.ristiinopiskelu.messaging.util.OidPersonNode;
import org.springframework.util.StringUtils;

public class PersonOidValidationResult implements PersonIdentifierValidationResult {

    private final String oid;
    private final boolean validateChecksum;

    public PersonOidValidationResult(String oid, boolean validateChecksum) {
        this.oid = oid;
        this.validateChecksum = validateChecksum;
    }

    @Override
    public boolean isTestPerson() {
        return isValid() && Oid.hasNode(oid, OidPersonNode.TEST_PERSON);
    }
    
    @Override
    public boolean hasValue() {
        return StringUtils.hasText(oid);
    }

    @Override
    public boolean isValid() {
        return Oid.isValid(oid, validateChecksum);
    }
}
