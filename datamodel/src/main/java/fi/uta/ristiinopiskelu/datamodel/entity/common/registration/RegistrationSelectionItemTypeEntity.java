package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType;

import java.util.Arrays;

public enum RegistrationSelectionItemTypeEntity {

    COURSE_UNIT,
    REALISATION,
    COMPLETION_OPTION,
    ASSESSMENT_ITEM;

    public static RegistrationSelectionItemTypeEntity from(RegistrationSelectionItemType type) {
        if(type == null) {
            return null;
        }

        return Arrays.stream(RegistrationSelectionItemTypeEntity.values())
            .filter(c -> c.name().equals(type.name()))
            .findFirst()
            .orElse(null);
    }
}
