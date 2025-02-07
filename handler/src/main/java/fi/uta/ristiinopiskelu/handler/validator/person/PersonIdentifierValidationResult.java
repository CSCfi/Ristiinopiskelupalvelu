package fi.uta.ristiinopiskelu.handler.validator.person;

public interface PersonIdentifierValidationResult {

    boolean isTestPerson();

    boolean hasValue();

    boolean isValid();
}
