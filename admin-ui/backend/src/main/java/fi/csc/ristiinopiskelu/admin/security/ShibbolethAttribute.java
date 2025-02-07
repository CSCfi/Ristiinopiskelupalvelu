package fi.csc.ristiinopiskelu.admin.security;

public enum ShibbolethAttribute {

    EPPN("eppn"),
    SURNAME("sn"),
    FIRST_NAMES("funetEduPersonGivenNames"),
    GIVEN_NAME("givenName"),
    EMAIL("mail"),
    ORGANISATION("organisation"),
    FULL_NAME("cn");

    private final String value;

    ShibbolethAttribute(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
