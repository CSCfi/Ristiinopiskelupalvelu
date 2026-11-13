package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;

import java.util.Arrays;

public enum RegistrationStatusEntity {

    REGISTERED,
    REGISTRATION_REJECTED,
    RECEIVED;

    public static RegistrationStatusEntity from(RegistrationStatus status) {
        if(status == null) {
            return null;
        }

        return Arrays.stream(RegistrationStatusEntity.values())
            .filter(s -> s.name().equals(status.name()))
            .findFirst()
            .orElse(null);
    }
}
