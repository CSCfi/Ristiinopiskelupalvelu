package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationSelectionItemTypeEntity;

import java.util.Arrays;

public enum RegistrationSelectionItemType {

    COURSE_UNIT,
    REALISATION,
    COMPLETION_OPTION,
    ASSESSMENT_ITEM;

    public static RegistrationSelectionItemType from(RegistrationSelectionItemTypeEntity type) {
        if (type == null) {
            return null;
        }

        return Arrays.stream(RegistrationSelectionItemType.values())
            .filter(c -> c.name().equals(type.name()))
            .findFirst()
            .orElse(null);
    }
}
