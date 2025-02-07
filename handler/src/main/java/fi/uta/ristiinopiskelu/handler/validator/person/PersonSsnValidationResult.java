package fi.uta.ristiinopiskelu.handler.validator.person;

import com.github.mpolla.HetuUtil;
import org.springframework.util.StringUtils;

public class PersonSsnValidationResult implements PersonIdentifierValidationResult {

    private final String ssn;

    public PersonSsnValidationResult(String ssn) {
        this.ssn = StringUtils.hasText(ssn) ? ssn.toUpperCase() : ssn;
    }

    @Override
    public boolean isTestPerson() {
        return hasValue() && isValidLookingPersonId() && isTestPersonId();
    }

    @Override
    public boolean hasValue() {
        return StringUtils.hasText(ssn);
    }

    @Override
    public boolean isValid() {
        return isTestPerson() || HetuUtil.isValid(ssn);
    }

    private boolean isTestPersonId() {
        return ssn.length() == 11 && ssn.substring(7, 10).startsWith("9");
    }

    private boolean isValidLookingPersonId() {
        return ssn.matches("[0-9]{6}[abcdefyxwvuABCDEFYXWVU+-][0-9]{3}[0-9A-Za-z]");
    }
}
