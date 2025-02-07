package fi.uta.ristiinopiskelu.handler.validator.person;

import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.messaging.util.Oid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonIdentifierValidatorTest {
    
    @Test
    public void testValidateSsn_withValidRealSsn_shouldSucceed() {
        String testSsn = HetuUtil.generateRandom();

        PersonSsnValidationResult result = PersonIdentifierValidator.validateSsn(testSsn);
        assertFalse(result.isTestPerson());
        assertTrue(result.isValid());
    }

    @Test
    public void testValidateSsn_withTestUserSsn_shouldSucceed() {
        String testSsn = "010199-9999";

        PersonSsnValidationResult result = PersonIdentifierValidator.validateSsn(testSsn);
        assertTrue(result.isTestPerson());
        assertTrue(result.isValid());
    }

    @Test
    public void testValidateSsn_withIncompleteSsn_shouldFail() {
        String testSsn = "01019";

        PersonSsnValidationResult result = PersonIdentifierValidator.validateSsn(testSsn);
        assertFalse(result.isTestPerson());
        assertFalse(result.isValid());

        testSsn = "233333-324";

        result = PersonIdentifierValidator.validateSsn(testSsn);
        assertFalse(result.isTestPerson());
        assertFalse(result.isValid());

        testSsn = null;
        result = PersonIdentifierValidator.validateSsn(testSsn);
        assertFalse(result.isTestPerson());
        assertFalse(result.isValid());
    }

    @Test
    public void testValidateSsn_withNewFormatSsn_shouldSucceed() {
        String testSsn = "010195V9003";

        PersonSsnValidationResult result = PersonIdentifierValidator.validateSsn(testSsn);
        assertTrue(result.isTestPerson());
        assertTrue(result.isValid());
    }

    @Test
    public void testValidateOid_withValidOid_shouldSucceed() {
        String oid = Oid.randomOid(Oid.PERSON_NODE_ID);

        PersonOidValidationResult result = PersonIdentifierValidator.validateOid(oid);
        assertTrue(result.isValid());
        assertFalse(result.isTestPerson());
    }

    @Test
    public void testValidateOid_withInvalidOid_shouldFail() {
        String oid = "1.2.246.562.24.3234";

        PersonOidValidationResult result = PersonIdentifierValidator.validateOid(oid);
        assertFalse(result.isValid());
        assertFalse(result.isTestPerson());
    }

    @Test
    public void testValidateOid_withInvalidTestOid_shouldFail() {
        String oid = "1.2.246.562.98.100033010%s".formatted(Oid.ibmChecksum("100033010"));

        PersonOidValidationResult result = PersonIdentifierValidator.validateOid(oid);
        assertFalse(result.isValid());
        assertFalse(result.isTestPerson());

        oid = "1.2.246.562.98.10003301044444%s".formatted(Oid.ibmChecksum("10003301044444"));

        result = PersonIdentifierValidator.validateOid(oid);
        assertFalse(result.isValid());
        assertFalse(result.isTestPerson());
    }

    @Test
    public void testValidateOid_withIncompleteOid_shouldFail() {
        String testOid = "1.2.246.562";

        PersonOidValidationResult result = PersonIdentifierValidator.validateOid(testOid);
        assertFalse(result.isTestPerson());
        assertFalse(result.isValid());

        testOid = "1.2.246.562.333";

        result = PersonIdentifierValidator.validateOid(testOid);
        assertFalse(result.isTestPerson());
        assertFalse(result.isValid());

        testOid = null;
        result = PersonIdentifierValidator.validateOid(testOid);
        assertFalse(result.isTestPerson());
        assertFalse(result.isValid());
    }
}
