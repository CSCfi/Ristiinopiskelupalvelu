package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrganisationRole {
    ROLE_MAIN_ORGANIZER(1),
    ROLE_OTHER_ORGANIZER(2);

    private final int code;

    OrganisationRole(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static OrganisationRole fromValue(int code) {
        for (OrganisationRole b : OrganisationRole.values()) {
            if (b.code == code) {
                return b;
            }
        }
        return null;
    }

    // Show values correctly in Swagger
    @Override
    public String toString() {
        return String.valueOf(this.code);
    }
}
