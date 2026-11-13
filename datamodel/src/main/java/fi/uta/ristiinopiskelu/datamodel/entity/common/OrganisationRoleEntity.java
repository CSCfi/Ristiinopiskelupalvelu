package fi.uta.ristiinopiskelu.datamodel.entity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrganisationRoleEntity {
    ROLE_MAIN_ORGANIZER(1),
    ROLE_OTHER_ORGANIZER(2);

    private final int code;

    OrganisationRoleEntity(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static OrganisationRoleEntity fromValue(int code) {
        for (OrganisationRoleEntity b : OrganisationRoleEntity.values()) {
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
