package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyModuleTypeEntity;

/**
 *
 */
public enum StudyModuleType {
    BASIC_STUDIES,
    SUBJECT_STUDIES,
    ADVANCED_STUDIES,
    LANGUAGE_STUDIES,
    INTERNSHIP,
    POSTGRADUATE_STUDIES,
    OTHER;

    public static StudyModuleType from(StudyModuleTypeEntity source) {
        return source == null ? null : StudyModuleType.valueOf(source.name());
    }
}
