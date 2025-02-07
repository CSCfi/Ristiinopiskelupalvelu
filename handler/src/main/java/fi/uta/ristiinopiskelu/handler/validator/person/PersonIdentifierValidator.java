package fi.uta.ristiinopiskelu.handler.validator.person;

public class PersonIdentifierValidator {

    public static PersonSsnValidationResult validateSsn(String ssn) {
        return new PersonSsnValidationResult(ssn);
    }

    public static PersonOidValidationResult validateOid(String oid) {
        return new PersonOidValidationResult(oid);
    }
}
