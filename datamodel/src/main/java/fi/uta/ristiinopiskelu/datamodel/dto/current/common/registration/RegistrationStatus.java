package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationStatusEntity;

public enum RegistrationStatus {

    REGISTERED,
    REGISTRATION_REJECTED,
    RECEIVED;

    public static RegistrationStatus from(RegistrationStatusEntity source) {
        return source == null ? null : RegistrationStatus.valueOf(source.name());
    }
}
