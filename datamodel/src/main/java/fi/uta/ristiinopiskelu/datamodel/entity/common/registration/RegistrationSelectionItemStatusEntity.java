package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import lombok.Getter;

import java.util.Arrays;

public enum RegistrationSelectionItemStatusEntity {
    NOT_ENROLLED(0),
    PENDING(1),
    ACCEPTED(2),
    REJECTED(3),
    ABORTED_BY_STUDENT(4),
    ABORTED_BY_TEACHER(5);

    @Getter
    private final int code;

    RegistrationSelectionItemStatusEntity(int code) {
        this.code = code;
    }

    public static RegistrationSelectionItemStatusEntity fromValue(int code) {
        for (RegistrationSelectionItemStatusEntity b : RegistrationSelectionItemStatusEntity.values()) {
            if (b.code == code) {
                return b;
            }
        }
        return null;
    }

    public static RegistrationSelectionItemStatusEntity from(RegistrationSelectionItemStatus status) {
        if(status == null) {
            return null;
        }

        return Arrays.stream(RegistrationSelectionItemStatusEntity.values())
            .filter(c -> c.name().equals(status.name()))
            .findFirst()
            .orElse(null);
    }
}
