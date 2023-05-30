package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RegistrationSelectionItemStatus {
    NOT_ENROLLED(0),
    PENDING(1),
    ACCEPTED(2),
    REJECTED(3),
    ABORTED_BY_STUDENT(4),
    ABORTED_BY_TEACHER(5);

    private final int code;

    RegistrationSelectionItemStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static RegistrationSelectionItemStatus fromValue(int code) {
        for (RegistrationSelectionItemStatus b : RegistrationSelectionItemStatus.values()) {
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
