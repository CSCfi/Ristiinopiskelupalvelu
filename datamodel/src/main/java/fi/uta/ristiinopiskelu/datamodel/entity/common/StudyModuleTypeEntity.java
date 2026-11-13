package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyModuleType;

import java.util.Arrays;

/**
 *
 */
public enum StudyModuleTypeEntity {
    BASIC_STUDIES,
    SUBJECT_STUDIES,
    ADVANCED_STUDIES,
    LANGUAGE_STUDIES,
    INTERNSHIP,
    POSTGRADUATE_STUDIES,
    OTHER;

    public static StudyModuleTypeEntity from(StudyModuleType type) {
        if(type == null) {
            return null;
        }

        return Arrays.stream(StudyModuleTypeEntity.values())
            .filter(c -> c.name().equals(type.name()))
            .findFirst()
            .orElse(null);
    }
}
